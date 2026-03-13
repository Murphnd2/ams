package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class ApplicationTaskDAO {

    public static List<ApplicationModule> getModulesForApplication(EntityManager em, Application a){
        Query q = em.createQuery("SELECT am FROM ApplicationModule am WHERE am.application.proposal.id = :id");
        q.setParameter("id",a.getProposal().getId());
        List<ApplicationModule> applicationModuleList;
        try{
            applicationModuleList = (List<ApplicationModule>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        return applicationModuleList;
    }

    public static List<RequiredTaskList> getTaskListsForApplication(EntityManager em, Application a){
        List<ApplicationModule> applicationModuleList = getModulesForApplication(em,a);
        System.out.println("Application Module List Size: " + applicationModuleList.size());
        List<RequiredTaskList> requiredTaskLists = new ArrayList<>();
        for(ApplicationModule am:applicationModuleList){
            List<RequiredTaskList> list = getTaskListsForModule(em,am);
            System.out.println("Tasks for Application ID-" + am.getApplication().getProposal().getId() + ":TP ID-"+am.getServiceItem().getId() +" = " + list.size());
            for(RequiredTaskList rtl: list){
                requiredTaskLists.add(rtl);
            }
        }
        return requiredTaskLists;
    }

    public static List<SortedTask> getTasksRequiredForApplication(EntityManager em, Application a){
        List<RequiredTaskList> requiredTaskLists = getTaskListsForApplication(em,a);
        System.out.println("Required Task List Size: " + requiredTaskLists.size());

        // Collect unique tasks via legacy dedup
        List<SortedTask> necessaryList = getTasksWithLegacyOrder(em, requiredTaskLists);

        // Check for composite ordering
        if (!requiredTaskLists.isEmpty() && !necessaryList.isEmpty()) {
            int groupId = requiredTaskLists.get(0).getServiceItem().getActivityCategory().getId();
            Long pspId = requiredTaskLists.get(0).getPsp().getId();

            if (CompositeOrderDAO.hasCompositeOrder(em, pspId, groupId)) {
                System.out.println("Using composite ordering for groupId=" + groupId);
                applyCompositeOrder(em, necessaryList, pspId, groupId);
            }
        }

        return necessaryList;
    }

    /**
     * Legacy dedup: iterate RequiredTaskLists, first occurrence wins.
     * Uses direct JPQL query instead of lazy @OneToMany collection to avoid
     * EclipseLink L2 cache staleness (seeded sequences would return empty lists).
     */
    private static List<SortedTask> getTasksWithLegacyOrder(EntityManager em, List<RequiredTaskList> requiredTaskLists) {
        List<SortedTask> necessaryList = new ArrayList<>();
        for(RequiredTaskList rtl:requiredTaskLists){
            List<TaskSequenceTable> tstList = getTaskSequenceItems(em, rtl.getId());
            System.out.println("RTL:"+rtl.getDescription()+"----------------------------");
            System.out.println(" * Task List Size = " + tstList.size());
            for(TaskSequenceTable tst: tstList){
                boolean listHasIt = false;
                for(SortedTask st: necessaryList){
                    if(st.getTask().getId()==tst.getTask().getId()){
                        listHasIt = true;
                        break;
                    }
                }
                if(!listHasIt) {
                    System.out.println("Task: "+tst.getTask().getDescription()+" not in list yet.");
                    necessaryList.add(new SortedTask(tst.getTask(), tst.getSortOrder()));
                } else {
                    System.out.println("Task: "+tst.getTask().getDescription()+" already in list.");
                }
            }
        }
        return necessaryList;
    }

    /**
     * Re-sort the already-deduped task list using composite ordering.
     * Tasks in the composite order get their composite sort values.
     * Tasks NOT in the composite (newly added) get appended at the end.
     */
    private static void applyCompositeOrder(EntityManager em, List<SortedTask> taskList, Long pspId, int groupId) {
        Map<Long, Integer> compositeMap = CompositeOrderDAO.getCompositeOrderMap(em, pspId, groupId);

        // Find max composite sort order for appending unordered tasks
        int maxOrder = 0;
        for (int val : compositeMap.values()) {
            if (val > maxOrder) maxOrder = val;
        }

        // Assign composite sort values
        int appendOrder = maxOrder + 10;
        for (SortedTask st : taskList) {
            Integer compositePos = compositeMap.get(st.getTask().getId());
            if (compositePos != null) {
                st.setSortOrder(compositePos);
            } else {
                // Task not in composite order — append at end
                st.setSortOrder(appendOrder);
                appendOrder += 10;
            }
        }

        // Sort by the newly assigned composite sort values
        taskList.sort(Comparator.comparingInt(SortedTask::getSortOrder));
    }

    /**
     * Direct JPQL query for TaskSequenceTable items — bypasses EclipseLink L2 cache
     * on the lazy @OneToMany collection which can be stale after seeding.
     */
    public static List<TaskSequenceTable> getTaskSequenceItems(EntityManager em, Long sequenceId) {
        Query q = em.createQuery(
                "SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :id ORDER BY tst.sortOrder");
        q.setParameter("id", sequenceId);
        try {
            return (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    public static List<RequiredTaskList> getTaskListsForModule(EntityManager em, ApplicationModule am){
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.serviceItem.id = :id");
        q.setParameter("id",am.getServiceItem().getId());
        List<RequiredTaskList> requiredTaskLists;
        try{
            requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            requiredTaskLists = new ArrayList<>();
        }
        return requiredTaskLists;
    }
}
