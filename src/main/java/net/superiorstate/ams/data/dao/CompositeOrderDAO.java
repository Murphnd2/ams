package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.sequences.support.CompositeTaskOrder;
import net.superiorstate.ams.model.activity.checklist.sequences.support.CompositeTaskView;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.general.PSP;

import java.util.*;

public abstract class CompositeOrderDAO {

    /**
     * Get the composite order for a given PSP and activity category, sorted by sortOrder.
     */
    public static List<CompositeTaskOrder> getCompositeOrder(EntityManager em, Long pspId, int groupId) {
        Query q = em.createQuery(
                "SELECT cto FROM CompositeTaskOrder cto " +
                "WHERE cto.psp.id = :pspId AND cto.activityCategory.id = :groupId " +
                "ORDER BY cto.sortOrder");
        q.setParameter("pspId", pspId);
        q.setParameter("groupId", groupId);
        try {
            return (List<CompositeTaskOrder>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Check if a composite order exists for the given PSP + activity category.
     */
    public static boolean hasCompositeOrder(EntityManager em, Long pspId, int groupId) {
        Query q = em.createQuery(
                "SELECT COUNT(cto) FROM CompositeTaskOrder cto " +
                "WHERE cto.psp.id = :pspId AND cto.activityCategory.id = :groupId");
        q.setParameter("pspId", pspId);
        q.setParameter("groupId", groupId);
        try {
            return ((Long) q.getSingleResult()) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Collect all unique tasks across all sequences of a given activity category for a PSP.
     * Returns CompositeTaskView DTOs with task, composite position (or -1 if unordered),
     * and the list of sequence names the task belongs to.
     */
    public static List<CompositeTaskView> getAllTasksForCategory(EntityManager em, Long pspId, int groupId) {
        // 1. Get all active RequiredTaskLists for this activity category
        Query qSeq = em.createQuery(
                "SELECT r FROM RequiredTaskList r " +
                "WHERE r.serviceItem.activityCategory.id = :groupId AND r.inActive = false " +
                "ORDER BY r.description");
        qSeq.setParameter("groupId", groupId);
        List<RequiredTaskList> sequences;
        try {
            sequences = (List<RequiredTaskList>) qSeq.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }

        // 2. Load existing composite order as a map: taskId → sortOrder
        Map<Long, Integer> compositeMap = new HashMap<>();
        List<CompositeTaskOrder> existing = getCompositeOrder(em, pspId, groupId);
        for (CompositeTaskOrder cto : existing) {
            compositeMap.put(cto.getTask().getId(), cto.getSortOrder());
        }

        // 3. Merge all tasks across sequences, deduplicating by task ID
        // Track which sequences each task belongs to
        Map<Long, CompositeTaskView> taskMap = new LinkedHashMap<>();

        for (RequiredTaskList rtl : sequences) {
            String seqName = rtl.getDescription();
            // Remove type prefix like "(Setup) " for cleaner display
            if (seqName.startsWith("(Setup) ") || seqName.startsWith("(Renewal) ")) {
                seqName = seqName.substring(seqName.indexOf(") ") + 2);
            } else if (seqName.startsWith("(Ticket)")) {
                seqName = seqName.substring(seqName.indexOf(")") + 1).trim();
            }

            List<TaskSequenceTable> tstList = rtl.getTaskSequenceTableList();
            for (TaskSequenceTable tst : tstList) {
                Task task = tst.getTask();
                Long taskId = task.getId();
                // Force eager access of fields needed in JSP
                task.isReUsable();
                task.hasOwner();
                task.hasGoTo();
                task.isSourced();

                CompositeTaskView view = taskMap.get(taskId);
                if (view == null) {
                    int compositePos = compositeMap.getOrDefault(taskId, -1);
                    view = new CompositeTaskView(task, compositePos, task.isReUsable());
                    taskMap.put(taskId, view);
                }
                view.addSequenceName(seqName);
            }
        }

        // 4. Sort: ordered tasks first (by composite position), then unordered at bottom
        List<CompositeTaskView> result = new ArrayList<>(taskMap.values());
        result.sort((a, b) -> {
            if (a.isOrdered() && b.isOrdered()) return Integer.compare(a.getCompositeSortOrder(), b.getCompositeSortOrder());
            if (a.isOrdered() && !b.isOrdered()) return -1;
            if (!a.isOrdered() && b.isOrdered()) return 1;
            return 0; // both unordered — keep insertion order
        });

        return result;
    }

    /**
     * Save composite order: delete-and-rebuild pattern.
     * Only persists entries for the given task list — any orphaned entries are naturally pruned.
     */
    public static void saveCompositeOrder(EntityManager em, Long pspId, int groupId, List<long[]> entries) {
        em.getTransaction().begin();
        try {
            // Delete existing composite order for this PSP + group
            Query qDel = em.createQuery(
                    "DELETE FROM CompositeTaskOrder cto " +
                    "WHERE cto.psp.id = :pspId AND cto.activityCategory.id = :groupId");
            qDel.setParameter("pspId", pspId);
            qDel.setParameter("groupId", groupId);
            qDel.executeUpdate();

            // Insert new entries
            PSP psp = em.find(PSP.class, pspId);
            ActivityCategory category = em.find(ActivityCategory.class, groupId);

            for (long[] entry : entries) {
                long taskId = entry[0];
                int sortOrder = (int) entry[1];
                Task task = em.find(Task.class, taskId);
                if (task != null) {
                    CompositeTaskOrder cto = new CompositeTaskOrder(psp, category, task, sortOrder);
                    em.persist(cto);
                }
            }

            em.getTransaction().commit();
            System.out.println("✅ CompositeOrderDAO: Saved composite order for groupId=" + groupId + " with " + entries.size() + " tasks");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("❌ CompositeOrderDAO: Failed to save composite order — " + e.getMessage());
            throw e;
        }
    }

    /**
     * Build a map of taskId → compositeSortOrder for use in population logic.
     * Returns empty map if no composite order exists.
     */
    public static Map<Long, Integer> getCompositeOrderMap(EntityManager em, Long pspId, int groupId) {
        Map<Long, Integer> map = new HashMap<>();
        List<CompositeTaskOrder> orders = getCompositeOrder(em, pspId, groupId);
        for (CompositeTaskOrder cto : orders) {
            map.put(cto.getTask().getId(), cto.getSortOrder());
        }
        return map;
    }
}
