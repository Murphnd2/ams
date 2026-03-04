package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.questionnaire.Questionnaire;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireField;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireFieldValue;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Assignee;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Questionnaire auto-attach and query service.
 * Static utility methods — follows the RenewalService pattern.
 */
public abstract class QuestionnaireService {

    /**
     * Finds all active questionnaires matching the given activity type and scoping,
     * then creates a QuestionnaireInstance for each match.
     *
     * @param em           open EntityManager (caller manages transactions)
     * @param activity     the newly created activity (Ticket, Setup, or Renewal)
     * @param activityType "TICKET", "SETUP", or "RENEWAL"
     * @param pspId        the PSP ID to scope questionnaire lookup
     * @return list of newly created QuestionnaireInstance objects
     */
    public static List<QuestionnaireInstance> attachMatchingQuestionnaires(
            EntityManager em, Assignee activity, String activityType, long pspId) {

        List<QuestionnaireInstance> created = new ArrayList<>();

        try {
            System.out.println("[QS] attachMatchingQuestionnaires: type=" + activityType
                    + " pspId=" + pspId + " activityId=" + activity.getId()
                    + " activityClass=" + activity.getClass().getSimpleName());

            // 1. Query all active questionnaires matching activityType (or ALL)
            List<Questionnaire> candidates = em.createQuery(
                            "SELECT q FROM Questionnaire q WHERE q.psp.id = :pspId " +
                                    "AND q.suppressed = false " +
                                    "AND (q.activityType = 'ALL' OR q.activityType = :type) " +
                                    "ORDER BY q.sortOrder",
                            Questionnaire.class)
                    .setParameter("pspId", pspId)
                    .setParameter("type", activityType)
                    .getResultList();

            System.out.println("[QS] candidates found: " + (candidates == null ? 0 : candidates.size()));
            if (candidates == null || candidates.isEmpty()) return created;

            // 2. Gather the activity's scoping context
            Set<Long> activitySiIds = getActivityServiceItemIds(em, activity);
            Set<Long> activityLosIds = getActivityLosIds(em, activity);
            Set<Long> activityEnhIds = getActivityEnhancementIds(em, activity);
            System.out.println("[QS] activity context: siIds=" + activitySiIds
                    + " losIds=" + activityLosIds + " enhIds=" + activityEnhIds);

            // 3. For each candidate, check scope overlap → create instance if match
            for (Questionnaire q : candidates) {
                // Force-load scoping collections
                if (q.getLosList() == null) q.setLosList(new ArrayList<>());
                else q.getLosList().size();
                if (q.getEnhancementList() == null) q.setEnhancementList(new ArrayList<>());
                else q.getEnhancementList().size();
                if (q.getServiceItemList() == null) q.setServiceItemList(new ArrayList<>());
                else q.getServiceItemList().size();

                boolean isGlobal = q.getLosList().isEmpty()
                        && q.getEnhancementList().isEmpty()
                        && q.getServiceItemList().isEmpty();

                boolean scopeMatch = hasScopeOverlap(q, activitySiIds, activityLosIds, activityEnhIds);
                System.out.println("[QS]   checking '" + q.getName() + "' (type=" + q.getActivityType()
                        + " global=" + isGlobal + " scopeMatch=" + scopeMatch
                        + " siScope=" + q.getServiceItemList().stream().map(si -> String.valueOf(si.getId())).toList()
                        + ")");

                if (isGlobal || scopeMatch) {
                    QuestionnaireInstance instance = new QuestionnaireInstance();
                    instance.setQuestionnaire(q);
                    instance.setActivity(activity);
                    // status defaults to NOT_STARTED, guid generated by @PrePersist

                    em.getTransaction().begin();
                    em.persist(instance);
                    em.getTransaction().commit();

                    created.add(instance);
                    System.out.println("[QS]   → ATTACHED instance id=" + instance.getId());
                }
            }
            System.out.println("[QS] total attached: " + created.size());
        } catch (Exception e) {
            System.out.println("[QuestionnaireService] attachMatchingQuestionnaires error: " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        }

        return created;
    }

    /**
     * Loads all QuestionnaireInstance rows for a given activity, with questionnaire eager-loaded.
     */
    public static List<QuestionnaireInstance> getInstancesForActivity(EntityManager em, long activityId) {
        try {
            List<QuestionnaireInstance> instances = em.createQuery(
                            "SELECT qi FROM QuestionnaireInstance qi " +
                                    "JOIN FETCH qi.questionnaire " +
                                    "WHERE qi.activity.id = :actId " +
                                    "ORDER BY qi.questionnaire.sortOrder",
                            QuestionnaireInstance.class)
                    .setParameter("actId", activityId)
                    .getResultList();
            return instances != null ? instances : new ArrayList<>();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    // ── GUID Lookup (for public form access) ──────────────────────────────────

    /**
     * Loads a QuestionnaireInstance by its public GUID, with questionnaire and
     * field list eager-loaded. Returns null if not found.
     */
    public static QuestionnaireInstance getInstanceByGuid(EntityManager em, String guid) {
        try {
            List<QuestionnaireInstance> results = em.createQuery(
                            "SELECT qi FROM QuestionnaireInstance qi " +
                                    "JOIN FETCH qi.questionnaire q " +
                                    "LEFT JOIN FETCH q.fieldList " +
                                    "WHERE qi.instanceGuid = :guid",
                            QuestionnaireInstance.class)
                    .setParameter("guid", guid)
                    .getResultList();
            if (results == null || results.isEmpty()) return null;
            QuestionnaireInstance qi = results.get(0);
            // Re-sort fields (EclipseLink DISTINCT + JOIN FETCH can scramble @OrderBy)
            if (qi.getQuestionnaire().getFieldList() != null) {
                qi.getQuestionnaire().getFieldList().sort(java.util.Comparator.comparingInt(QuestionnaireField::getSortOrder));
            }
            return qi;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Loads saved field values for a questionnaire instance as a Map keyed by fieldKey.
     */
    public static java.util.Map<String, String> getFieldValueMap(EntityManager em, QuestionnaireInstance instance) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        try {
            List<QuestionnaireFieldValue> values = em.createQuery(
                            "SELECT fv FROM QuestionnaireFieldValue fv " +
                                    "JOIN FETCH fv.field " +
                                    "WHERE fv.instance = :instance",
                            QuestionnaireFieldValue.class)
                    .setParameter("instance", instance)
                    .getResultList();
            if (values != null) {
                for (QuestionnaireFieldValue fv : values) {
                    map.put(fv.getField().getFieldKey(), fv.getFieldValue());
                }
            }
        } catch (Exception e) {
            System.out.println("[QuestionnaireService] getFieldValueMap error: " + e.getMessage());
        }
        return map;
    }

    // ── Scope Overlap Check ─────────────────────────────────────────────────

    private static boolean hasScopeOverlap(Questionnaire q,
                                           Set<Long> actSiIds, Set<Long> actLosIds, Set<Long> actEnhIds) {
        // Check ServiceItem overlap
        for (ServiceItem si : q.getServiceItemList()) {
            if (actSiIds.contains((long) si.getId())) return true;
        }
        // Check LOS overlap
        for (LOS los : q.getLosList()) {
            if (actLosIds.contains(los.getId())) return true;
        }
        // Check Enhancement overlap
        for (Enhancement enh : q.getEnhancementList()) {
            if (actEnhIds.contains(enh.getId())) return true;
        }
        return false;
    }

    // ── Activity Context Helpers ─────────────────────────────────────────────

    /**
     * Returns the Set of ServiceItem IDs relevant to the given activity.
     */
    private static Set<Long> getActivityServiceItemIds(EntityManager em, Assignee activity) {
        Set<Long> ids = new HashSet<>();
        try {
            if (activity instanceof Ticket ticket) {
                if (ticket.getTicketServiceItem() != null) {
                    ids.add((long) ticket.getTicketServiceItem().getId());
                }
            } else if (activity instanceof Setup setup) {
                Application app = setup.getApplication();
                if (app != null && app.getApplicationModuleList() != null) {
                    for (ApplicationModule am : app.getApplicationModuleList()) {
                        if (am.getServiceItem() != null) {
                            ids.add((long) am.getServiceItem().getId());
                        }
                    }
                }
            } else if (activity instanceof Renewal renewal) {
                if (renewal.getRenewalItemList() != null) {
                    for (RenewalItem ri : renewal.getRenewalItemList()) {
                        if (ri.getBenefit() != null
                                && ri.getBenefit().getPlanType() != null
                                && ri.getBenefit().getPlanType().getServiceItem() != null) {
                            ids.add((long) ri.getBenefit().getPlanType().getServiceItem().getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[QuestionnaireService] getActivityServiceItemIds error: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Returns the Set of LOS IDs relevant to the given activity.
     * Only Setup activities have direct LOS associations.
     */
    private static Set<Long> getActivityLosIds(EntityManager em, Assignee activity) {
        Set<Long> ids = new HashSet<>();
        try {
            if (activity instanceof Setup setup) {
                Application app = setup.getApplication();
                if (app != null) {
                    Proposal proposal = app.getProposal();
                    if (proposal != null && proposal.getLosList() != null) {
                        for (LOS los : proposal.getLosList()) {
                            ids.add(los.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[QuestionnaireService] getActivityLosIds error: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Returns the Set of Enhancement IDs relevant to the given activity.
     * Setup: derive from ApplicationModules that link to Enhancements via ServiceItem.
     * For now, we check if any LOS's Enhancement has a matching ServiceItem in the application modules.
     */
    private static Set<Long> getActivityEnhancementIds(EntityManager em, Assignee activity) {
        Set<Long> ids = new HashSet<>();
        try {
            if (activity instanceof Setup setup) {
                // Get ServiceItem IDs from application modules
                Set<Long> siIds = new HashSet<>();
                Application app = setup.getApplication();
                if (app != null && app.getApplicationModuleList() != null) {
                    for (ApplicationModule am : app.getApplicationModuleList()) {
                        if (am.getServiceItem() != null) {
                            siIds.add((long) am.getServiceItem().getId());
                        }
                    }
                }
                // Find Enhancements whose ServiceItem is in the module list
                if (!siIds.isEmpty()) {
                    List<Enhancement> enhancements = em.createQuery(
                                    "SELECT e FROM Enhancement e WHERE e.serviceItem.id IN :siIds",
                                    Enhancement.class)
                            .setParameter("siIds", siIds.stream().map(Long::intValue).toList())
                            .getResultList();
                    if (enhancements != null) {
                        for (Enhancement e : enhancements) {
                            ids.add(e.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[QuestionnaireService] getActivityEnhancementIds error: " + e.getMessage());
        }
        return ids;
    }
}
