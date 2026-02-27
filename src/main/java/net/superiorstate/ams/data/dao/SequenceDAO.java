package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.model.activity.checklist.sequences.support.DoW;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ActivityCategory;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.WebLink;

import java.util.ArrayList;
import java.util.List;

public abstract class SequenceDAO {

    public static boolean sequenceLoggedForPurpose(EntityManager em, int purposeId){
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.serviceItem.id = :purpose_id");
        q.setParameter("purpose_id",purposeId);
        boolean hasSequence = false;
        try{
            List<TaskSequence> taskSequenceList = (List<TaskSequence>) q.getResultList();
            if(taskSequenceList.size()>0)
                hasSequence = true;
        } catch (NoResultException e){
            e.printStackTrace();
            return false;
        } finally {

            return hasSequence;
        }
    }

    public static List<ServiceItem> getSetupModuleList(EntityManager em){
        Query q = em.createQuery("SELECT tp FROM ServiceItem tp WHERE tp.id >10 AND tp.id < 20 AND tp.id <> 18 order by tp.id");
        return (List<ServiceItem>) q.getResultList();
    }

    public static void flipFilterFlags(HttpServletRequest request, int groupId){
        if(groupId==1)
            request.getSession().setAttribute("fR","checked");
        if (groupId==2)
            request.getSession().setAttribute("fS","checked");
        if (groupId==3)
            request.getSession().setAttribute("fT","checked");
        if (groupId==4)
            request.getSession().setAttribute("fU","checked");
    }
    public static void clearCurrentTask(HttpServletRequest request){
        Task task = new Task();
        task.setId(-1L);
        request.getSession().setAttribute("checkView",2);
        request.getSession().setAttribute("hasCurrentTask",false);
        request.getSession().setAttribute("currentTask", task);
    }


    public static ActivityCategory getActivityCategoryByServiceItemId(EntityManager em, int purposeId){
        if(!sequenceLoggedForPurpose(em,purposeId))
            return new ActivityCategory();
        ServiceItem serviceItem = EntityLookup.getServiceItemById(em,purposeId);
        return serviceItem.getActivityCategory();
    }
    public static List<ActivityCategory> getTemplateGroups(EntityManager em){
        List<ActivityCategory> activityCategoryList = null;
        try{
            Query q = em.createQuery("SELECT tg FROM ActivityCategory tg");
            activityCategoryList = (List<ActivityCategory>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            activityCategoryList = new ArrayList<>();
        }
        finally {
            return activityCategoryList;
        }
    }

    public static LinkType getLinkTypeById(EntityManager em, int id){
        LinkType linkType = null;
        try{
            Query q = em.createQuery("SELECT lt FROM LinkType lt WHERE lt.id = :id");
            q.setParameter("id",id);
            linkType = (LinkType) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            linkType = new LinkType();
        } finally {
            return linkType;
        }
    }
    public static WebLink getWebLinkById(EntityManager em, Long id){
        WebLink webLink = null;
        try{
            Query q = em.createQuery("SELECT wl FROM WebLink wl WHERE wl.id = :id");
            q.setParameter("id",id);
            webLink = (WebLink) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            webLink = new WebLink();
        } finally {
            return webLink;
        }
    }

    public static void createLinkType(EntityManager em, int id, String name){
        LinkType linkType = new LinkType();
        linkType.setId(id);
        linkType.setTypeName(name);
        em.getTransaction().begin();
        em.persist(linkType);
        em.getTransaction().commit();
    }
    public static List<ServiceItem> getServiceItems(EntityManager em){
        List<ServiceItem> serviceItemList = null;
        try{
            Query q = em.createQuery("SELECT tp FROM ServiceItem tp INNER JOIN FETCH tp.activityCategory tg");
            serviceItemList = (List<ServiceItem>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            serviceItemList = new ArrayList<>();
        } finally {
            return serviceItemList;
        }
    }

    public static TaskSequenceTable getTaskSequenceTableById(EntityManager em, Long sequenceId, Long taskId){
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :sequence_id AND tst.task.id = :task_id");
        q.setParameter("sequence_id",sequenceId);
        q.setParameter("task_id",taskId);
        TaskSequenceTable taskSequenceTable = null;
        try{
            taskSequenceTable = (TaskSequenceTable) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            taskSequenceTable = new TaskSequenceTable();
        } finally {
            return taskSequenceTable;
        }
    }



    public static TaskSequence getTaskSequenceById(EntityManager em, Long id){
        TaskSequence taskSequence = null;
        try{
            Query q = em.createQuery("SELECT t FROM TaskSequence t WHERE t.id = :id");
            q.setParameter("id",id);
            taskSequence = (TaskSequence) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
        } finally {
            return taskSequence;
        }
    }

    public static DoW createDoW(EntityManager em, int id, String description, int wdId){
        em.getTransaction().begin();
        DoW doW = new DoW();
        doW.setId(id);
        doW.setName(description);
        doW.setWeekdayId(wdId);
        em.persist(doW);
        em.getTransaction().commit();
        return doW;
    }
}
