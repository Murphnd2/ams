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
import java.util.List;

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
        List<SortedTask> necessaryList = new ArrayList<>();
        for(RequiredTaskList rtl:requiredTaskLists){
            List<TaskSequenceTable> tstList = rtl.getTaskSequenceTableList();
            System.out.println("RTL:"+rtl.getDescription()+"----------------------------");
            System.out.println(" * Task List Size = " + rtl.getTaskSequenceTableList().size());
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
