package net.superiorstate.ams.controller.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplateGroup;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.model.activity.ticket.TicketSubCategory;
import net.superiorstate.ams.model.general.PSP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles SAVE, CREATE, DELETE, and SUPPRESS actions from the Sequence Template Manager.
 * Always redirects back to SequenceBuilder25 after completion.
 *
 * POST params:
 *   action       = "SAVE" | "CREATE" | "DELETE" | "SUPPRESS"
 *
 * SAVE params:
 *   sequenceId   = existing RequiredTaskList id
 *   taskOrder    = JSON array: [{order, taskId, desc, reusable}, ...]
 *
 * CREATE params:
 *   newSeqType       = "ticket" | "renewal" | "setup"
 *   seqName          = sequence name
 *   ticketCategoryId = (ticket only) TicketCategory id, or -1 for new category
 *   newCategoryName  = (ticket only, when ticketCategoryId=-1) new category description
 *   newCategoryShort = (ticket only, when ticketCategoryId=-1) new category short code
 *   purposeId        = (renewal/setup only) TemplatePurpose id
 *
 * DELETE params:
 *   sequenceId   = RequiredTaskList id to deactivate
 *
 * SUPPRESS params:
 *   sequenceId   = RequiredTaskList id whose TicketSubCategory.isActive to toggle
 */
@WebServlet(name = "SequenceAction25", value = "/SequenceAction25")
public class SequenceAction25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "";

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long redirectId = -1;

        try {
            switch (action) {
                case "SAVE" -> redirectId = handleSave(request, em);
                case "CREATE" -> redirectId = handleCreate(request, em);
                case "DELETE" -> handleDelete(request, em);
                case "SUPPRESS" -> redirectId = handleSuppress(request, em);
            }
        } catch (Exception e) {
            System.out.println("❌ SequenceAction25 error (" + action + "): " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            if (em.isOpen()) em.close();
        }

        // Build redirect with view state params
        String f = request.getParameter("f");
        String ss = request.getParameter("ss");
        StringBuilder redir = new StringBuilder("SequenceBuilder25");

        // SUPPRESS special case: if item was just suppressed and view is "hide suppressed",
        // deselect (don't load it) so it disappears cleanly from the list.
        // If item was just restored, or view is "show suppressed", stay on it.
        boolean justSuppressed = Boolean.TRUE.equals(request.getAttribute("justSuppressed"));
        boolean viewingHidden = "1".equals(ss);

        if ("SUPPRESS".equals(action) && justSuppressed && !viewingHidden) {
            // Item just got hidden and view hides suppressed → deselect, keep same view state
            redir.append("?");
        } else if ("SUPPRESS".equals(action) && justSuppressed && viewingHidden) {
            // Item just got hidden but view shows suppressed → stay on it
            redir.append("?load=").append(redirectId);
        } else if (redirectId > 0) {
            redir.append("?load=").append(redirectId);
        } else {
            redir.append("?");
        }
        if (f != null && !f.isEmpty()) redir.append("&f=").append(f);
        if (ss != null && !ss.isEmpty()) redir.append("&ss=").append(ss);

        response.sendRedirect(redir.toString());
    }

    // ── SAVE: Update task list for an existing sequence ──────────────────────────

    private long handleSave(HttpServletRequest request, EntityManager em) {
        long seqId = Long.parseLong(request.getParameter("sequenceId"));
        String taskOrderJson = request.getParameter("taskOrder");
        if (taskOrderJson == null || taskOrderJson.isEmpty()) return seqId;

        RequiredTaskList rtl = EntityLookup.getReqListById(em, seqId);
        if (rtl == null) return -1;

        PSP psp = EntityLookup.getPspById(em, 4L);

        // Parse the JSON task array manually (no external library needed)
        List<TaskEntry> entries = parseTaskEntries(taskOrderJson);

        // Delete all existing TaskSequenceTable rows for this sequence
        em.getTransaction().begin();
        Query delQ = em.createQuery("DELETE FROM TaskSequenceTable t WHERE t.taskSequence.id = :id");
        delQ.setParameter("id", rtl.getId());
        delQ.executeUpdate();
        em.getTransaction().commit();

        // Rebuild from the submitted order
        List<TaskSequenceTable> newTstList = new ArrayList<>();
        for (TaskEntry entry : entries) {
            Task task;
            if (entry.taskId > 0) {
                // Existing task
                task = EntityLookup.getTaskById(em, entry.taskId);
            } else {
                // New task — create it
                em.getTransaction().begin();
                task = new Task();
                task.setDescription(entry.desc);
                task.setPsp(psp);
                task.setReUsable(entry.reusable);
                task.setSourced(false);
                task.setHasOwner(false);
                task.setHasAutomation(false);
                task.setHasGoTo(false);
                task.setHasInfo(false);
                task.setAllowNonOwner(true);
                task.setAllowEarly(true);
                task.setAllowFuture(true);
                em.persist(task);
                em.getTransaction().commit();
            }

            if (task != null) {
                int sortOrder = entry.order * 10;
                em.getTransaction().begin();
                TaskSequenceTable tst = new TaskSequenceTable();
                tst.setTaskSequence(rtl);
                tst.setTask(task);
                tst.setSortOrder(sortOrder);
                em.persist(tst);
                em.getTransaction().commit();
                newTstList.add(tst);
            }
        }

        // Update the sequence's list reference
        em.getTransaction().begin();
        rtl.setTaskSequenceTableList(newTstList);
        em.persist(rtl);
        em.getTransaction().commit();

        System.out.println("✅ SequenceAction25 SAVE: sequence " + seqId + " updated with " + newTstList.size() + " tasks");
        return seqId;
    }

    // ── CREATE: New sequence ─────────────────────────────────────────────────────

    private long handleCreate(HttpServletRequest request, EntityManager em) {
        String type = request.getParameter("newSeqType");
        String name = request.getParameter("seqName");
        if (type == null || name == null || name.trim().isEmpty()) return -1;

        PSP psp = EntityLookup.getPspById(em, 4L);
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        TemplatePurpose tp;

        if ("ticket".equals(type)) {
            // Tickets: create TicketSubCategory + TemplatePurpose + RequiredTaskList
            long catId = Long.parseLong(request.getParameter("ticketCategoryId"));
            TicketCategory tc;

            if (catId == -1) {
                // ── Create new TicketCategory on-the-fly ──
                String newCatName = request.getParameter("newCategoryName");
                String newCatShort = request.getParameter("newCategoryShort");
                if (newCatName == null || newCatName.trim().isEmpty()) return -1;
                if (newCatShort == null || newCatShort.trim().isEmpty()) {
                    newCatShort = newCatName.trim().length() > 20
                            ? newCatName.trim().substring(0, 20)
                            : newCatName.trim();
                }

                em.getTransaction().begin();
                tc = new TicketCategory();
                tc.setDescription(newCatName.trim());
                tc.setShortText(newCatShort.trim());
                tc.setActive(true);
                em.persist(tc);
                em.getTransaction().commit();

                // Update global cache with new category
                if (global != null) {
                    List<TicketCategory> updatedCats = new ArrayList<>(global.getTicketCategories());
                    updatedCats.add(tc);
                    updatedCats.sort((a, b) -> a.getDescription().compareToIgnoreCase(b.getDescription()));
                    global.setTicketCategories(updatedCats);
                }

                System.out.println("✅ SequenceAction25 CREATE: new TicketCategory '" + tc.getDescription()
                        + "' id=" + tc.getId());
            } else {
                tc = EntityLookup.getTicketCategoryById(em, catId);
            }

            TemplateGroup tg = EntityLookup.getTemplateGroupById(em, 3);

            // Create TicketSubCategory
            em.getTransaction().begin();
            TicketSubCategory tsc = new TicketSubCategory();
            tsc.setActive(true);
            tsc.setDescription(name.trim());
            tsc.setTicketCategory(tc);
            em.persist(tsc);
            em.getTransaction().commit();

            // Create TemplatePurpose
            em.getTransaction().begin();
            tp = new TemplatePurpose();
            tp.setSortOrder(100);
            tp.setDescription(name.trim());
            tp.setTemplateGroup(tg);
            em.persist(tp);
            em.getTransaction().commit();

            // Link them
            em.getTransaction().begin();
            tsc.setTemplatePurpose(tp);
            em.persist(tsc);
            em.getTransaction().commit();

            // Update global subcategory cache
            if (global != null) {
                List<TicketSubCategory> updated = new ArrayList<>(global.getTicketSubCategories());
                updated.add(tsc);
                global.setTicketSubCategories(updated);
                getServletContext().setAttribute("global", global);
            }
        } else {
            // Renewal or Setup: use existing unassigned TemplatePurpose
            int purposeId = Integer.parseInt(request.getParameter("purposeId"));
            tp = EntityLookup.getTemplatePurposeById(em, purposeId);
            if (tp == null) return -1;
        }

        // Create the RequiredTaskList
        String prefix = "ticket".equals(type) ? "" : ("renewal".equals(type) ? "(Renewal) " : "(Setup) ");
        em.getTransaction().begin();
        RequiredTaskList rtl = new RequiredTaskList();
        rtl.setInActive(false);
        rtl.setPsp(psp);
        rtl.setTemplatePurpose(tp);
        rtl.setDescription(prefix + tp.getDescription());
        em.persist(rtl);
        em.getTransaction().commit();

        System.out.println("✅ SequenceAction25 CREATE: new " + type + " sequence '" + rtl.getDescription() + "' id=" + rtl.getId());
        return rtl.getId();
    }

    // ── DELETE: Soft-delete (deactivate) a sequence ──────────────────────────────

    private void handleDelete(HttpServletRequest request, EntityManager em) {
        long seqId = Long.parseLong(request.getParameter("sequenceId"));
        RequiredTaskList rtl = EntityLookup.getReqListById(em, seqId);
        if (rtl == null) return;

        em.getTransaction().begin();
        rtl.setInActive(true);
        em.persist(rtl);
        em.getTransaction().commit();

        System.out.println("✅ SequenceAction25 DELETE: sequence " + seqId + " deactivated");
    }

    /**
     * Toggles the isActive flag on the TicketSubCategory linked to a ticket sequence.
     * When suppressed (isActive=false), the reason won't appear in the Create Ticket dropdown.
     * The sequence itself remains intact and editable.
     */
    private long handleSuppress(HttpServletRequest request, EntityManager em) {
        String seqIdParam = request.getParameter("sequenceId");
        if (seqIdParam == null || seqIdParam.isEmpty()) return -1;

        long seqId = Long.parseLong(seqIdParam);
        RequiredTaskList rtl = EntityLookup.getReqListById(em, seqId);
        if (rtl == null || rtl.getTemplatePurpose() == null) return -1;

        // Find the TicketSubCategory linked to this sequence's TemplatePurpose
        TicketSubCategory tsc = findSubCategoryByPurpose(em, rtl.getTemplatePurpose().getId());
        if (tsc == null) return seqId;

        // Toggle isActive
        em.getTransaction().begin();
        tsc.setActive(!tsc.isActive());
        em.persist(tsc);
        em.getTransaction().commit();

        // Flag whether the item is now suppressed (for redirect logic)
        request.setAttribute("justSuppressed", !tsc.isActive());

        // Refresh the global ticket subcategory cache
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null) {
            List<TicketSubCategory> refreshed = getActiveTicketSubCategories(em);
            global.setTicketSubCategories(refreshed);
            getServletContext().setAttribute("global", global);
        }

        System.out.println("✅ SequenceAction25 SUPPRESS: subcategory " + tsc.getId()
                + " (" + tsc.getDescription() + ") → isActive=" + tsc.isActive());

        return seqId;
    }

    private TicketSubCategory findSubCategoryByPurpose(EntityManager em, int purposeId) {
        try {
            return (TicketSubCategory) em.createQuery(
                            "SELECT tsc FROM TicketSubCategory tsc WHERE tsc.templatePurpose.id = :pid")
                    .setParameter("pid", purposeId)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private List<TicketSubCategory> getActiveTicketSubCategories(EntityManager em) {
        return em.createQuery(
                        "SELECT t FROM TicketSubCategory t WHERE t.isActive = true ORDER BY t.ticketCategory.shortText, t.description",
                        TicketSubCategory.class)
                .getResultList();
    }

    // ── JSON parsing helper (no external library) ────────────────────────────────

    /**
     * Parses the taskOrder JSON array manually.
     * Expected format: [{"order":0,"taskId":123,"desc":"...","reusable":true}, ...]
     */
    private List<TaskEntry> parseTaskEntries(String json) {
        List<TaskEntry> entries = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.equals("[]")) return entries;

        // Remove outer brackets
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);

        // Split by },{ pattern
        String[] objects = inner.split("\\},\\s*\\{");
        for (String obj : objects) {
            // Clean up braces
            obj = obj.replace("{", "").replace("}", "").trim();
            if (obj.isEmpty()) continue;

            TaskEntry entry = new TaskEntry();
            String[] fields = obj.split(",(?=\\s*\")"); // split on comma before quote
            for (String field : fields) {
                field = field.trim();
                if (field.startsWith("\"order\"")) {
                    entry.order = extractInt(field);
                } else if (field.startsWith("\"taskId\"")) {
                    entry.taskId = extractLong(field);
                } else if (field.startsWith("\"desc\"")) {
                    entry.desc = extractString(field);
                } else if (field.startsWith("\"reusable\"")) {
                    entry.reusable = extractBoolean(field);
                }
            }
            entries.add(entry);
        }
        return entries;
    }

    private int extractInt(String field) {
        try { return Integer.parseInt(field.split(":")[1].trim().replace("\"", "")); }
        catch (Exception e) { return 0; }
    }
    private long extractLong(String field) {
        try { return Long.parseLong(field.split(":")[1].trim().replace("\"", "")); }
        catch (Exception e) { return -1; }
    }
    private String extractString(String field) {
        try {
            int colonIdx = field.indexOf(":");
            String val = field.substring(colonIdx + 1).trim();
            if (val.startsWith("\"")) val = val.substring(1);
            if (val.endsWith("\"")) val = val.substring(0, val.length() - 1);
            return val;
        } catch (Exception e) { return ""; }
    }
    private boolean extractBoolean(String field) {
        try { return field.split(":")[1].trim().replace("\"", "").equals("true"); }
        catch (Exception e) { return false; }
    }

    /** Simple DTO for parsed task entries */
    private static class TaskEntry {
        int order = 0;
        long taskId = -1;
        String desc = "";
        boolean reusable = false;
    }
}
