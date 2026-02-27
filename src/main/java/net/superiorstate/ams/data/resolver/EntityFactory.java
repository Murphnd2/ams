package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.RequiredTaskDAO;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.WebLink;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public abstract class EntityFactory {

    public static CheckList createChecklist(EntityManager em, String name, List<String> toDoList, Date dueDate, User user){
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setDueDate(dueDate);
        c.setFullName(name);
        c.setAssignedTo(user.getPerson());
        c.setComplete(false);
        c.setLoggedBy(user.getPerson());
        em.persist(c);
        em.getTransaction().commit();
        int sortOrder = 10;
        for(String td: toDoList){
            Task task = createTaskOneTime(em,td,user.getPerson().getPsp());
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setComplete(false);
            toDo.setCheckList(c);
            toDo.setTask(task);
            toDo.setSortOrder(sortOrder);
            em.persist(toDo);
            em.getTransaction().commit();
            sortOrder += 10;
        }
        return c;
    }

    public static CheckList createReminder(EntityManager em, String reminder, User user, Date reminderDate){
        List<String> toDoList = new ArrayList<>();
        toDoList.add(reminder);
        return createChecklist(em,reminder,toDoList,reminderDate,user);
    }

    public static Task createTaskOneTime(EntityManager em, String taskName, PSP psp){
        em.getTransaction().begin();
        Task task = new Task();
        task.setReUsable(false);
        task.setDescription(taskName);
        task.setHasOwner(false);
        task.setSourced(false);
        task.setAllowNonOwner(true);
        task.setHasAutomation(false);
        task.setHasGoTo(false);
        task.setHasInfo(false);
        task.setAllowEarly(true);
        task.setAllowFuture(true);
        task.setPsp(psp);
        em.persist(task);
        em.getTransaction().commit();
        return task;
    }

    public static RequiredTaskList createReqList(EntityManager em, String name, PSP psp, ServiceItem tp){
        if(RequiredTaskDAO.listExistsForPurpose(em,tp))
            return RequiredTaskDAO.getRtlForPurpose(em,tp);
        em.getTransaction().begin();
        RequiredTaskList r = new RequiredTaskList();
        r.setDescription(name);
        r.setServiceItem(tp);
        r.setPsp(psp);
        r.setInActive(false);
        em.persist(r);
        em.getTransaction().commit();
        return r;
    }

    public static WebLink createWebLink(EntityManager em, String name, String path, LinkType linkType ){
        em.getTransaction().begin();
        WebLink webLink = new WebLink();
        webLink.setLinkType(linkType);
        webLink.setLinkPath(path);
        webLink.setPlainText(name);
        em.persist(webLink);
        return webLink;
    }
    public static WebLink createWebLink(EntityManager em, String name, String path, int linkTypeId){
        LinkType linkType = EntityLookup.getLinkTypeById(em,linkTypeId);
        return createWebLink(em, name, path, linkType);
    }
}
