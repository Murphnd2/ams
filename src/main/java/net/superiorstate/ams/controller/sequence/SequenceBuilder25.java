package net.superiorstate.ams.controller.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.ReqTaskListTix;
import net.superiorstate.ams.model.activity.checklist.sequences.GenSeq;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.activity.ticket.TicketSubCategory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolidated Sequence Template Manager — replaces GoTicketTemplate25 and SequenceHome.
 *
 * GET  /SequenceBuilder25           → loads page with all sequences, no sequence selected
 * GET  /SequenceBuilder25?load=123  → loads page with sequence 123 pre-selected in builder
 */
@WebServlet(name = "SequenceBuilder25", value = "/SequenceBuilder25")
public class SequenceBuilder25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            loadAllSequences(request, em);
            handleLoadRequest(request, em);
            loadSupportData(request, em);
        } finally {
            if (em.isOpen()) em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/general/sequenceBuilder/sequenceManager25.jsp");
        dispatcher.forward(request, response);
    }

    /**
     * Loads all RequiredTaskList sequences grouped by type, plus ticket display wrappers.
     * Eagerly resolves task counts so JSP doesn't need lazy-loaded collections.
     */
    private void loadAllSequences(HttpServletRequest request, EntityManager em) {
        // All active sequences by group
        List<RequiredTaskList> renewalList = getRequiredTaskLists(em, 1);
        List<RequiredTaskList> setupList = getRequiredTaskLists(em, 2);
        List<RequiredTaskList> ticketList = getRequiredTaskLists(em, 3);

        request.getSession().setAttribute("seqRenewalList", renewalList);
        request.getSession().setAttribute("seqSetupList", setupList);
        request.getSession().setAttribute("seqTicketList", ticketList);

        // Eagerly build task count map: sequenceId → count
        // This avoids lazy-loading taskSequenceTableList in the JSP after em is closed
        Map<Long, Integer> taskCountMap = new HashMap<>();
        for (RequiredTaskList rtl : renewalList) {
            taskCountMap.put(rtl.getId(), getTaskCount(em, rtl.getId()));
        }
        for (RequiredTaskList rtl : setupList) {
            taskCountMap.put(rtl.getId(), getTaskCount(em, rtl.getId()));
        }
        for (RequiredTaskList rtl : ticketList) {
            taskCountMap.put(rtl.getId(), getTaskCount(em, rtl.getId()));
        }
        request.setAttribute("taskCountMap", taskCountMap);

        // Ticket sequences need the TicketSubCategory wrapper for display (category - name)
        List<ReqTaskListTix> ticketDisplay = new ArrayList<>();
        for (RequiredTaskList rtl : ticketList) {
            try {
                ReqTaskListTix rtlt = new ReqTaskListTix(em, rtl.getId());
                ticketDisplay.add(rtlt);
            } catch (Exception e) {
                System.out.println("⚠️ SequenceBuilder25: Skipped ticket RTL id=" + rtl.getId() + " — " + e.getMessage());
            }
        }
        ticketDisplay.sort(Comparator.comparing(ReqTaskListTix::getDescription));
        request.getSession().setAttribute("seqTicketDisplay", ticketDisplay);

        // Total count for badges
        int total = renewalList.size() + setupList.size() + ticketList.size();
        request.setAttribute("seqTotalCount", total);
        request.setAttribute("seqRenewalCount", renewalList.size());
        request.setAttribute("seqSetupCount", setupList.size());
        request.setAttribute("seqTicketCount", ticketList.size());
    }

    /**
     * If ?load=ID is present, pre-loads that sequence into the builder (GenSeq list).
     */
    private void handleLoadRequest(HttpServletRequest request, EntityManager em) {
        String loadParam = request.getParameter("load");
        if (loadParam == null || loadParam.isEmpty()) {
            // No sequence selected — clear builder state
            request.getSession().setAttribute("sbListBuilder", new ArrayList<GenSeq>());
            request.getSession().setAttribute("sbSelectedId", -1L);
            request.getSession().setAttribute("sbSelectedName", "");
            request.getSession().setAttribute("sbSelectedGroupId", -1);
            request.getSession().setAttribute("sbIsSuppressed", false);
            return;
        }

        try {
            long seqId = Long.parseLong(loadParam);
            RequiredTaskList rtl = EntityLookup.getReqListById(em, seqId);
            if (rtl == null) return;

            // Load tasks in order
            Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id ORDER BY tst.sortOrder");
            q.setParameter("id", rtl.getId());
            List<TaskSequenceTable> tstList;
            try {
                tstList = (List<TaskSequenceTable>) q.getResultList();
            } catch (NoResultException e) {
                tstList = new ArrayList<>();
            }

            // Convert to GenSeq for session editing — eagerly copy all Task fields we need
            List<GenSeq> builder = new ArrayList<>();
            for (int i = 0; i < tstList.size(); i++) {
                TaskSequenceTable tst = tstList.get(i);
                Task task = tst.getTask();
                GenSeq gs = new GenSeq();
                gs.setDescription(task.getDescription());
                gs.setTask(task);
                gs.setPublicTask(task.isReUsable());
                gs.setSequenceNumber(i * 1000);
                // Force eager access of Task boolean fields we display in JSP
                // so they're loaded into the persistence context before em closes
                task.hasOwner();
                task.hasGoTo();
                task.isSourced();
                builder.add(gs);
            }
            if (builder.isEmpty()) {
                GenSeq g = new GenSeq();
                g.setDescription("");
                builder.add(g);
            }

            request.getSession().setAttribute("sbListBuilder", builder);
            request.getSession().setAttribute("sbSelectedId", rtl.getId());
            request.getSession().setAttribute("sbSelectedName", rtl.getDescription());
            request.getSession().setAttribute("sbSelectedGroupId", rtl.getTemplatePurpose().getTemplateGroup().getId());

            // Check if this is a ticket sequence and whether it's suppressed
            boolean isSuppressed = false;
            if (rtl.getTemplatePurpose().getTemplateGroup().getId() == 3) {
                // It's a ticket sequence — check the linked TicketSubCategory
                try {
                    TicketSubCategory tsc = (TicketSubCategory) em.createQuery(
                                    "SELECT tsc FROM TicketSubCategory tsc WHERE tsc.templatePurpose.id = :pid")
                            .setParameter("pid", rtl.getTemplatePurpose().getId())
                            .getSingleResult();
                    isSuppressed = !tsc.isActive();
                } catch (Exception ignored) {}
            }
            request.getSession().setAttribute("sbIsSuppressed", isSuppressed);
        } catch (NumberFormatException ignored) {
        }
    }

    /**
     * Loads support data for the "New Sequence" form and the add-task picker.
     */
    private void loadSupportData(HttpServletRequest request, EntityManager em) {
        // Reusable tasks for the "pick existing" dropdown
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.reUsable = true ORDER BY t.description");
        List<Task> reusableTasks;
        try {
            reusableTasks = (List<Task>) q.getResultList();
        } catch (NoResultException e) {
            reusableTasks = new ArrayList<>();
        }
        request.getSession().setAttribute("sbReusableTasks", reusableTasks);

        // Unassigned template purposes for new Renewal/Setup sequences
        request.getSession().setAttribute("sbUnassignedRenewal", getUnassignedPurposes(em, 1));
        request.getSession().setAttribute("sbUnassignedSetup", getUnassignedPurposes(em, 2));

        // Ticket categories for new Ticket sequences
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null) {
            request.getSession().setAttribute("sbTicketCategories", global.getTicketCategories());
        } else {
            Query qCat = em.createQuery("SELECT t FROM TicketCategory t WHERE t.active = true ORDER BY t.description");
            request.getSession().setAttribute("sbTicketCategories", qCat.getResultList());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private List<RequiredTaskList> getRequiredTaskLists(EntityManager em, int groupId) {
        Query q = em.createQuery("SELECT r FROM RequiredTaskList r WHERE r.templatePurpose.templateGroup.id = :id AND r.inActive = false ORDER BY r.description");
        q.setParameter("id", groupId);
        try {
            return (List<RequiredTaskList>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<TemplatePurpose> getUnassignedPurposes(EntityManager em, int groupId) {
        List<RequiredTaskList> assigned = getRequiredTaskLists(em, groupId);
        Query q = em.createQuery("SELECT t FROM TemplatePurpose t WHERE t.templateGroup.id = :id ORDER BY t.description");
        q.setParameter("id", groupId);
        List<TemplatePurpose> allPurposes;
        try {
            allPurposes = new ArrayList<>((List<TemplatePurpose>) q.getResultList());
        } catch (NoResultException e) {
            return new ArrayList<>();
        }

        for (RequiredTaskList rtl : assigned) {
            allPurposes.remove(rtl.getTemplatePurpose());
        }
        return allPurposes;
    }

    /**
     * Gets task count for a sequence via COUNT query — avoids lazy-loading the collection.
     */
    private int getTaskCount(EntityManager em, Long sequenceId) {
        Query q = em.createQuery("SELECT COUNT(tst) FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id");
        q.setParameter("id", sequenceId);
        try {
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}