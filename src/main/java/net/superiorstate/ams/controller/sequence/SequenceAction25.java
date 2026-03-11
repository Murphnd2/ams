package net.superiorstate.ams.controller.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.CompositeOrderDAO;
import net.superiorstate.ams.data.dao.TicketQueryDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.ticket.TicketCategory;
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
 *   purposeId        = (renewal/setup only) ServiceItem id
 *
 * DELETE params:
 *   sequenceId   = RequiredTaskList id to deactivate
 *
 * SUPPRESS params:
 *   sequenceId   = RequiredTaskList id whose ServiceItem.isSuppressed to toggle
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
                case "SAVE_COMPOSITE" -> {
                    handleSaveComposite(request, em);
                    request.setAttribute("compositeRedirect", true);
                }
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

        // SAVE_COMPOSITE: redirect back to the composite view
        if (Boolean.TRUE.equals(request.getAttribute("compositeRedirect"))) {
            String compositeGroupId = request.getParameter("compositeGroupId");
            redir.append("?composite=").append(compositeGroupId);
            if (f != null && !f.isEmpty()) redir.append("&f=").append(f);
            if (ss != null && !ss.isEmpty()) redir.append("&ss=").append(ss);
            response.sendRedirect(redir.toString());
            return;
        }

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
        if (type == null) return -1;
        if ("ticket".equals(type) && (name == null || name.trim().isEmpty())) return -1;

        PSP psp = EntityLookup.getPspById(em, 4L);
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        ServiceItem tp;

        if ("ticket".equals(type)) {
            // Tickets: create ServiceItem + RequiredTaskList
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

            ActivityCategory tg = EntityLookup.getTemplateGroupById(em, 3);

            // Create ServiceItem with V020 fields
            em.getTransaction().begin();
            tp = new ServiceItem();
            tp.setSortOrder(100);
            tp.setDescription(name.trim());
            tp.setActivityCategory(tg);
            tp.setPsp(psp);
            tp.setSourceType("MANUAL");
            tp.setTicketCategory(tc);
            em.persist(tp);
            em.getTransaction().commit();

            // Update global ServiceItem cache
            if (global != null) {
                List<ServiceItem> updated = new ArrayList<>(global.getTicketServiceItems());
                updated.add(tp);
                global.setTicketServiceItems(updated);
                getServletContext().setAttribute("global", global);
            }
        } else {
            // Renewal or Setup: use existing unassigned TemplatePurpose
            int purposeId = Integer.parseInt(request.getParameter("purposeId"));
            tp = EntityLookup.getServiceItemById(em, purposeId);
            if (tp == null) return -1;
        }

        // Create the RequiredTaskList
        String prefix = "ticket".equals(type) ? "" : ("renewal".equals(type) ? "(Renewal) " : "(Setup) ");
        em.getTransaction().begin();
        RequiredTaskList rtl = new RequiredTaskList();
        rtl.setInActive(false);
        rtl.setPsp(psp);
        rtl.setServiceItem(tp);
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
     * Toggles the isSuppressed flag on the ServiceItem linked to a ticket sequence.
     * When suppressed, the reason won't appear in the Create Ticket dropdown.
     * The sequence itself remains intact and editable.
     */
    private long handleSuppress(HttpServletRequest request, EntityManager em) {
        String seqIdParam = request.getParameter("sequenceId");
        if (seqIdParam == null || seqIdParam.isEmpty()) return -1;

        long seqId = Long.parseLong(seqIdParam);
        RequiredTaskList rtl = EntityLookup.getReqListById(em, seqId);
        if (rtl == null || rtl.getServiceItem() == null) return -1;

        ServiceItem si = rtl.getServiceItem();

        // Toggle suppressed
        em.getTransaction().begin();
        si.setSuppressed(!si.isSuppressed());
        em.persist(si);
        em.getTransaction().commit();

        // Flag whether the item is now suppressed (for redirect logic)
        request.setAttribute("justSuppressed", si.isSuppressed());


        // Refresh the global ticket subcategory cache
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null) {
            List<ServiceItem> refreshed = TicketQueryDAO.getActiveTicketServiceItems(em);
            global.setTicketServiceItems(refreshed);
            getServletContext().setAttribute("global", global);
        }

        System.out.println("✅ SequenceAction25 SUPPRESS: ServiceItem " + si.getId()
                + " (" + si.getDescription() + ") → suppressed=" + si.isSuppressed());

        return seqId;
    }


    // ── SAVE_COMPOSITE: Save cross-sequence composite task ordering ────────────

    private void handleSaveComposite(HttpServletRequest request, EntityManager em) {
        String compositeOrderJson = request.getParameter("compositeOrder");
        int groupId = Integer.parseInt(request.getParameter("compositeGroupId"));

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentPerson() == null || local.getCurrentPerson().getPsp() == null) return;
        Long pspId = local.getCurrentPerson().getPsp().getId();

        if (compositeOrderJson == null || compositeOrderJson.isEmpty() || compositeOrderJson.equals("[]")) {
            // Empty order means clear all composite entries
            CompositeOrderDAO.saveCompositeOrder(em, pspId, groupId, new ArrayList<>());
            return;
        }

        // Parse JSON array: [{"taskId":123,"order":0}, ...]
        List<long[]> entries = parseCompositeEntries(compositeOrderJson);
        CompositeOrderDAO.saveCompositeOrder(em, pspId, groupId, entries);
    }

    /**
     * Parses composite order JSON: [{"taskId":123,"order":0}, ...]
     * Returns list of long[]{taskId, sortOrder}
     */
    private List<long[]> parseCompositeEntries(String json) {
        List<long[]> entries = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.equals("[]")) return entries;

        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);

        String[] objects = inner.split("\\},\\s*\\{");
        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "").trim();
            if (obj.isEmpty()) continue;

            long taskId = -1;
            int order = 0;
            String[] fields = obj.split(",");
            for (String field : fields) {
                field = field.trim();
                if (field.contains("\"taskId\"")) {
                    taskId = extractLong(field);
                } else if (field.contains("\"order\"")) {
                    order = extractInt(field);
                }
            }
            if (taskId > 0) {
                entries.add(new long[]{taskId, order});
            }
        }
        return entries;
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
