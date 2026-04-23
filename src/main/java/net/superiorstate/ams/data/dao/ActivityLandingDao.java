package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.ActivityLandingFilter;
import net.superiorstate.ams.model.ActivityLandingRow;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ActivityLandingDao {

    private final EntityManagerFactory emf;

    public ActivityLandingDao(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public List<ActivityLandingRow> fetchLandingRows(
            long mePersonId,
            int daysSinceWarn,
            ActivityLandingFilter f
    ) {
        EntityManager em = emf.createEntityManager();
        try {
            String sql = buildSql();

            Query q = em.createNativeQuery(sql);

            // params CTE (1..10)
            q.setParameter(1, mePersonId);
            q.setParameter(2, daysSinceWarn);

            q.setParameter(3, f.ownershipFilter);
            q.setParameter(4, f.includeRenewal ? 1 : 0);
            q.setParameter(5, f.includeSetup ? 1 : 0);
            q.setParameter(6, f.includeTicket ? 1 : 0);

            q.setParameter(7, f.viewNeedsContact ? 1 : 0);
            q.setParameter(8, f.viewWaitingOnUs ? 1 : 0);

            q.setParameter(9, f.sortAlphabetically ? 1 : 0);

            q.setParameter(10, f.includeOpportunity ? 1 : 0);

            // LIMIT/OFFSET (11..12)
            q.setParameter(11, f.pageSize);
            q.setParameter(12, f.offset);

            @SuppressWarnings("unchecked")
            List<Object[]> rows = q.getResultList();

            List<ActivityLandingRow> out = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                long activityId = ((Number) r[0]).longValue();
                String dtype = (String) r[1];
                String fullName = (String) r[2];

                Long assignedToId =
                        (r[3] == null) ? null : ((Number) r[3]).longValue();

                Date dueDate = toSqlDate(r[4]);

                boolean waitingOnUs =
                        (r[5] != null) && ((Number) r[5]).intValue() == 1;

                int daysSinceContact =
                        (r[6] == null) ? 9999 : ((Number) r[6]).intValue();

                boolean delegatedToMe =
                        (r[7] != null) && ((Number) r[7]).intValue() == 1;

                int dueBucket =
                        (r[8] == null) ? 0 : ((Number) r[8]).intValue();

                String ticketEmployerNameLc = (String) r[9];

                String opportunityStage = (String) r[10];

                Long managedById = (r[11] == null) ? null : ((Number) r[11]).longValue();

                out.add(new ActivityLandingRow(
                        activityId,
                        dtype,
                        fullName,
                        assignedToId,
                        dueDate,
                        waitingOnUs,
                        daysSinceContact,
                        delegatedToMe,
                        dueBucket,
                        ticketEmployerNameLc,
                        opportunityStage,
                        managedById
                ));
            }

            return out;
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private Date toSqlDate(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d;
        if (v instanceof Timestamp ts) return new Date(ts.getTime());
        if (v instanceof java.util.Date ud) return new Date(ud.getTime());
        throw new IllegalArgumentException("Unsupported date type: " + v.getClass());
    }

    private String buildSql() {
        return """
WITH
params AS (
  SELECT
    ? AS me,
    ? AS daysSinceWarn,
    ? AS ownFilter,
    ? AS incRenewal,
    ? AS incSetup,
    ? AS incTicket,
    ? AS viewNeedsContact,
    ? AS viewWaitingOnUs,
    ? AS sortAlpha,
    ? AS incOpportunity
),

open_act AS (
  SELECT
    a.id,
    a.DTYPE,
    a.full_name,
    a.assigned_to_id,
    a.due_date,
    a.checklist_id,
    a.primary_contact,
    a.managed_by_id,
    a.opportunity_stage
  FROM assignee a
  WHERE a.is_complete = 0
    AND (
      (a.DTYPE IN ('Renewal', 'Setup', 'Ticket'))
      OR (a.DTYPE = 'Opportunity'
          AND (a.opportunity_stage IS NULL
               OR a.opportunity_stage NOT IN ('WON', 'LOST')))
    )
),

last_outbound AS (
  SELECT
    n.activity_id,
    MAX(COALESCE(n.date_generated, DATE(n.date_created))) AS last_contact_date
  FROM note n
  JOIN reasoncreated rc
    ON rc.use_id = n.reason_id
   AND rc.outbound = 1
  GROUP BY n.activity_id
),

last_status_note AS (
  SELECT
    n.activity_id,
    MAX(n.note_id) AS max_note_id
  FROM note n
  WHERE n.status_id IN (1, 3)
  GROUP BY n.activity_id
),

last_status AS (
  SELECT
    n.activity_id,
    n.status_id
  FROM note n
  JOIN last_status_note lsn
    ON lsn.max_note_id = n.note_id
),

ticket_extra AS (
  SELECT
    t.id AS ticket_id,
    LOWER(er.employer_name) AS ticket_employer_name_lc
  FROM open_act t
  JOIN assignee p
    ON p.id = t.primary_contact
   AND p.DTYPE = 'Person'
  JOIN employee e
    ON e.employee_id = p.employee_id
  JOIN employer er
    ON er.organization_id = e.employer_id
  WHERE t.DTYPE = 'Ticket'
),

base AS (
  SELECT
    oa.id AS activity_id,
    oa.DTYPE AS dtype,
    oa.full_name AS full_name,
    oa.assigned_to_id AS assigned_to_id,
    oa.due_date AS due_date,
    oa.managed_by_id AS managed_by_id,
    oa.opportunity_stage AS opportunity_stage,

    CASE
      WHEN ls.status_id = 1 THEN 0
      WHEN ls.status_id = 3 THEN 1
      ELSE 1
    END AS waiting_on_us,

    CASE
      WHEN lo.last_contact_date IS NULL THEN 9999
      ELSE DATEDIFF(CURDATE(), lo.last_contact_date)
    END AS days_since_contact,

    CASE
      WHEN oa.assigned_to_id = p.me THEN 0
      WHEN EXISTS (
        SELECT 1
        FROM todo td
        LEFT JOIN task tsk
          ON tsk.task_id = td.task_id
        WHERE td.checklist_id = oa.checklist_id
          AND td.is_complete = 0
          AND (
            /* V061: ToDo-level override wins when enabled */
            (td.override_ownership = 1 AND td.has_owner = 1 AND td.owner_id = p.me)
            OR
            /* Task-level fallback (only when ToDo override is off) */
            (COALESCE(td.override_ownership, 0) = 0
              AND tsk.has_owner = 1 AND tsk.owner_id = p.me)
          )
        LIMIT 1
      ) THEN 1
      ELSE 0
    END AS delegated_to_me,

    CASE
      WHEN oa.due_date IS NULL THEN 0
      WHEN DATE_SUB(oa.due_date, INTERVAL DAYOFMONTH(oa.due_date)-1 DAY)
           > DATE_ADD(DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE())-1 DAY),
                      INTERVAL 1 MONTH)
        THEN 0
      WHEN DATE_SUB(oa.due_date, INTERVAL DAYOFMONTH(oa.due_date)-1 DAY)
           > DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE())-1 DAY)
        THEN 1
      WHEN DATE_SUB(oa.due_date, INTERVAL DAYOFMONTH(oa.due_date)-1 DAY)
           > DATE_SUB(DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE())-1 DAY),
                      INTERVAL 1 MONTH)
        THEN 2
      ELSE 3
    END AS due_bucket,

    te.ticket_employer_name_lc AS ticket_employer_name_lc,

    CASE
      WHEN lo.last_contact_date IS NULL THEN 1
      WHEN DATEDIFF(CURDATE(), lo.last_contact_date) > p.daysSinceWarn THEN 1
      ELSE 0
    END AS needs_contact

  FROM open_act oa
  CROSS JOIN params p
  LEFT JOIN last_outbound lo
    ON lo.activity_id = oa.id
  LEFT JOIN last_status ls
    ON ls.activity_id = oa.id
  LEFT JOIN ticket_extra te
    ON te.ticket_id = oa.id
)

SELECT
  b.activity_id,
  b.dtype,
  b.full_name,
  b.assigned_to_id,
  b.due_date,
  b.waiting_on_us,
  b.days_since_contact,
  b.delegated_to_me,
  b.due_bucket,
  b.ticket_employer_name_lc,
  b.opportunity_stage,
  b.managed_by_id
FROM base b
CROSS JOIN params p
WHERE 1 = 1

  /* ── Ownership filter ── */
  AND (
    /* 0 = All Open: no ownership restriction */
    p.ownFilter = 0

    /* 1 = My World: I own it, or I manage it, or I have a delegated task */
    OR (p.ownFilter = 1 AND (
         b.assigned_to_id = p.me
         OR (b.dtype = 'Opportunity' AND b.managed_by_id = p.me)
         OR b.delegated_to_me = 1
       ))

    /* 2 = I Own: assigned to me or I manage it */
    OR (p.ownFilter = 2 AND (
         b.assigned_to_id = p.me
         OR (b.dtype = 'Opportunity' AND b.managed_by_id = p.me)
       ))

    /* 3 = Helping On: delegated to me AND I don't own it */
    OR (p.ownFilter = 3 AND (
         b.delegated_to_me = 1
         AND b.assigned_to_id != p.me
       ))
  )

  /* ── Type filter ── */
  AND (
    (p.incRenewal = 1 AND b.dtype = 'Renewal')
    OR (p.incSetup = 1 AND b.dtype = 'Setup')
    OR (p.incTicket = 1 AND b.dtype = 'Ticket')
    OR (p.incOpportunity = 1 AND b.dtype = 'Opportunity'
        AND (b.assigned_to_id = p.me OR b.managed_by_id = p.me))
  )

  /* ── Attention filter ── */
  AND (
    (p.viewNeedsContact = 0 AND p.viewWaitingOnUs = 0)
    OR (p.viewNeedsContact = 1 AND p.viewWaitingOnUs = 1
        AND (b.needs_contact = 1 OR b.waiting_on_us = 1))
    OR (p.viewNeedsContact = 1 AND p.viewWaitingOnUs = 0
        AND b.needs_contact = 1)
    OR (p.viewNeedsContact = 0 AND p.viewWaitingOnUs = 1
        AND b.waiting_on_us = 1)
  )

ORDER BY
  CASE WHEN p.sortAlpha = 1 THEN b.full_name END ASC,
  CASE WHEN p.sortAlpha = 0 THEN (b.due_date IS NULL) END ASC,
  CASE WHEN p.sortAlpha = 0 THEN b.due_date END ASC,

  b.full_name ASC,
  b.activity_id DESC

LIMIT ? OFFSET ?
""";
    }
}
