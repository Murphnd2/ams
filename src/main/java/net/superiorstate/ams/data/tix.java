package net.superiorstate.ams.data;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class tix {

    public static Person resolveContactEmail(EntityManager em, Person p, AmsDataLocal local, AmsDataGlobal global){
        if(p.getEmployee()==null)
            return p;
        Employee ee = p.getEmployee();
        String pEmail = "";
        if(p.getEmail()!=null)
            pEmail = p.getEmail().trim().toLowerCase();
        String eEmail = "";
        if(ee.getEmail()!=null)
            eEmail = ee.getEmail().trim().toLowerCase();
        String hEmail = "";
        if(ee.getHrEmail()!=null)
            hEmail = ee.getHrEmail().trim().toLowerCase();
        String pFirst = "";
        if(p.getFirstName()!=null)
            pFirst = p.getFirstName().trim().toUpperCase();

        String eFirst = "";
        if(ee.getFirstName()!=null)
            eFirst = ee.getFirstName().trim().toUpperCase();
        String pLast = "";
        if(p.getLastName()!=null)
            pLast = p.getLastName().trim().toUpperCase();
        String eLast = "";
        if(ee.getLastName()!=null)
            eLast = p.getEmployee().getLastName().trim().toUpperCase();
        String trueEmail = null;
        String trueFirst = null;
        String trueLast = null;
        boolean updatePerson = false;
        boolean updateEmployee = false;
        if(Validator.isValidEmail(hEmail) && !hEmail.equalsIgnoreCase(pEmail) && !hEmail.equalsIgnoreCase(eEmail)){
            updateEmployee = true;
            updatePerson = true;
            trueEmail = hEmail;
        } else if(Validator.isValidEmail(hEmail) && !hEmail.equalsIgnoreCase(pEmail)){
            updatePerson = true;
            trueEmail = hEmail;
        } else if(Validator.isValidEmail(eEmail) && !eEmail.equalsIgnoreCase(pEmail)){
            updateEmployee = true;
            updatePerson = true;
            trueEmail = eEmail;
        } else if(Validator.isValidEmail(pEmail)){
            updateEmployee = true;
            trueEmail = pEmail;
        }
        if(!eFirst.equalsIgnoreCase(pFirst) && !eFirst.equals("")){
            updatePerson = true;
            trueFirst = eFirst;
        } else if(!eFirst.equalsIgnoreCase(pFirst) && !pFirst.equals("")){
            updateEmployee = true;
            trueFirst = pFirst;
        }
        if(!eLast.equalsIgnoreCase(pLast) && !eLast.equals("")){
            updatePerson = true;
            trueLast = eLast;
        } else if(!eLast.equalsIgnoreCase(pLast) && !pLast.equals("")){
            updateEmployee = true;
            trueLast = pLast;
        }
        if(updateEmployee)
            ee = updateEmployee(em,ee,trueFirst,trueLast,trueEmail);
        if(updatePerson)
            p = updatePerson(em,p,trueFirst,trueLast,trueEmail,ee);
        if(updateEmployee)
            createTicket(em,p,eEmail,eFirst,eLast,local,global);
        return p;
    }
    public static void createTicket(EntityManager em, Person p, String oldEmail, String oldFirst, String oldLast, AmsDataLocal local, AmsDataGlobal global){
        CheckList c = createCheckListForSummitEmployeeChange(em,local);

        String desc = "Update "+ oldFirst + " " + oldLast + " in Summit";
        Task t1 = tix.createTask(em,desc,local);
        WebLink w = getWeblinkForTaskOne(em,p,global,desc);
        em.getTransaction().begin();
        t1.setHasGoTo(true);
        t1.setGoToLink(w);
        em.persist(t1);
        em.getTransaction().commit();

        ToDo td1 = tix.createToDo(em,t1,10,c);
        List<ToDo> toDoList = new ArrayList<>();
        toDoList.add(td1);

        if(!oldFirst.equalsIgnoreCase(local.getCurrentActivity().getPrimaryContact().getFirstName())){
            desc = "FIRST: " +oldFirst.toLowerCase()+" -> "+local.getCurrentActivity().getPrimaryContact().getFirstName().toUpperCase();
            ToDo td2 = tix.createToDo(em,desc,20,local,c);
            toDoList.add(td2);
        }
        if(!oldLast.equalsIgnoreCase(local.getCurrentActivity().getPrimaryContact().getLastName())){
            desc = "LAST: " + oldLast.toLowerCase() + " -> " + local.getCurrentActivity().getPrimaryContact().getLastName().toUpperCase();
            ToDo td3 = tix.createToDo(em,desc,30,local,c);
            toDoList.add(td3);
        }
        if(!oldEmail.equalsIgnoreCase(local.getCurrentActivity().getPrimaryContact().getEmail())){
            desc = "EMAIL: " + local.getCurrentActivity().getPrimaryContact().getEmail().toLowerCase();
            ToDo td4 = tix.createToDo(em, desc,40,local, c);
            toDoList.add(td4);
        }

        em.getTransaction().begin();
        c.setToDoList(toDoList);
        em.persist(c);
        em.getTransaction().commit();
        em.refresh(c);

        local.respondToActivityUpdate(em,"CHECK_REMINDER", c);
    }
    public static WebLink getWeblinkForTaskOne(EntityManager em, Person p, AmsDataGlobal global, String desc){
        Employer er = p.getEmployee().getEmployer();
        String path = global.getSummitPath() + "/Area/Participant/ParticipantList?employerId=" + er.getAltId();
        em.getTransaction().begin();
        WebLink w = new WebLink();
        w.setLinkType(EntityLookup.getLinkTypeById(em,2));
        w.setPlainText(desc);
        w.setLinkPath(path);
        w.setActive(true);
        em.persist(w);
        em.getTransaction().commit();
        return w;
    }
    public static CheckList createCheckListForSummitEmployeeChange(EntityManager em,AmsDataLocal local){
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setLoggedBy(local.getCurrentPerson());
        c.setFullName("UPDATE SUMMIT EMPLOYEE DATA");
        c.setAssignedTo(local.getCurrentPerson());
        c.setPrimaryContact(local.getCurrentActivity().getPrimaryContact());
        c.setComplete(false);
        c.setDueDate(Date.valueOf(LocalDate.now()));
        em.persist(c);
        em.getTransaction().commit();
        return c;
    }
    public static Task createTask(EntityManager em, String description, AmsDataLocal local){
        em.getTransaction().begin();
        Task t1 = new Task();
        t1.setHasAutomation(false);
        t1.setSourced(false);
        t1.setDescription(description);
        t1.setAllowNonOwner(true);
        t1.setPsp(local.getCurrentPerson().getPsp());
        t1.setHasAutomation(false);
        t1.setAllowEarly(true);
        t1.setAllowFuture(true);
        t1.setSourced(false);
        t1.setHasOwner(false);
        t1.setReUsable(false);
        em.persist(t1);
        em.getTransaction().commit();
        return t1;
    }
    public static ToDo createToDo(EntityManager em, String description, int sortOrder, AmsDataLocal local, CheckList c){
        Task t1 = createTask(em,description,local);
        return createToDo(em,t1,sortOrder,c);
    }
    public static ToDo createToDo(EntityManager em, Task t, int sortOrder, CheckList c){
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setCheckList(c);
        toDo.setTask(t);
        toDo.setSortOrder(sortOrder);
        toDo.setComplete(false);
        em.persist(toDo);
        em.getTransaction().commit();
        return toDo;
    }
    private static Person updatePerson(EntityManager em, Person p, String first, String last, String email, Employee ee){
        Person x = EntityLookup.getPersonById(em,p.getId());
        em.getTransaction().begin();
        x.setEmail(email);
        x.setFirstName(first);
        x.setLastName(last);
        x.setEmployee(ee);
        em.persist(x);
        em.getTransaction().commit();
        em.refresh(x);
        return x;
    }
    private static Employee updateEmployee(EntityManager em, Employee ee, String first, String last, String email){
        Employee e = EntityLookup.getEmployeeById(em,ee.getId());
        em.getTransaction().begin();
        e.setEmail(email);
        e.setHrEmail(email);
        e.setFirstName(first);
        e.setLastName(last);
        em.persist(e);
        em.getTransaction().commit();
        em.refresh(e);
        return e;
    }
}
