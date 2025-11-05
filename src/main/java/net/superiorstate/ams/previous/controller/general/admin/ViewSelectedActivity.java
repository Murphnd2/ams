package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.ticket.CreateTicket;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.activity.vA;
import net.superiorstate.ams.previous.data.misc.dbA;
import net.superiorstate.ams.previous.data.misc.dbRenew;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.general.UserRole;
import net.superiorstate.ams.previous.model.sales.application.ApplicationModule;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@WebServlet(name = "ViewSelectedActivity", value = "/ViewSelectedActivity")
public class ViewSelectedActivity extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        //vA.changeActivityView(request,em);


        request.getSession().setAttribute("lastTab",2);
        Long id = Long.parseLong(request.getParameter("btnViewActivity"));
        Activity selectedActivity = dM.getActivityById(em,id);
        assert selectedActivity != null;
        setActivityView(request,em,selectedActivity);


        em.close();
    }

    private static void fillRenewalSpecificLists(HttpServletRequest request, EntityManager em, Renewal r){
        List<Employee> contactList = dbRenew.getEmployeesAssignedToRenewal(em,r);
        request.getSession().setAttribute("contactList", dbTicket.getTicketEmployeeList(em,contactList));
        List<Employee> employeeList = dbRenew.getContactsNotAssigned(em,r);
        request.getSession().setAttribute("remainingEmployees", dbTicket.getTicketEmployeeList(em,employeeList));
        List<Benefit> benefitsNotInRenewal = dbRenew.getBenefitsNotInRenewal(em,r);
        request.getSession().setAttribute("benefitsNotInRenewal",benefitsNotInRenewal);

    }
    private static void fillSetupSpecificLists(HttpServletRequest request, EntityManager em, Setup s){
        List<Person> setupContactList = s.getContactList();
        List<ApplicationModule> moduleList = s.getApplication().getApplicationModuleList();
        Person agent = s.getApplication().getProposal().getProspect().getAgent();
        Person setupContact = new Person();
        if(s.getPrimaryContact()==null && s.getPrimaryContactSetup()!=null)
            setupContact = s.getPrimaryContactSetup();
        else if(s.getPrimaryContact()!=null)
            setupContact = s.getPrimaryContact();
        request.getSession().setAttribute("setupContactList",setupContactList);
        request.getSession().setAttribute("setupContact",setupContact);
        request.getSession().setAttribute("moduleList",moduleList);
        request.getSession().setAttribute("setupAgent",agent);
        String propLink = request.getContextPath() + "/serviceProposal?guid=" + s.getApplication().getProposal().getApplicationGUID();
        String appLink = Q.ONLINE_APPLICATION_DATA + "&entry=" + s.getApplication().getProposal().getApplicationGUID();
        request.getSession().setAttribute("proposalLink",propLink);
        request.getSession().setAttribute("appLink",appLink);
        List<TemplatePurpose> remainingModules = remainingModules(request,em,moduleList);
        request.getSession().setAttribute("remainingMods",remainingModules);
    }

    public static List<TemplatePurpose> remainingModules(HttpServletRequest request,EntityManager em, List<ApplicationModule> currentMods){
        List<TemplatePurpose> allModulesList = (List<TemplatePurpose>) request.getSession().getAttribute("setupModules");
        List<TemplatePurpose> remainingModules = new ArrayList<>();
        for(TemplatePurpose mod: allModulesList){
            boolean inList = false;
            for(ApplicationModule am: currentMods){
                if(am.getTemplatePurpose().getId()==mod.getId()){
                    inList = true;
                    break;
                }
            }
            if(!inList)
                remainingModules.add(mod);
        }
        return remainingModules;
    }

    public static List<Activity> getPastActivities(HttpServletRequest request, EntityManager em, Activity a){
        if(!a.getClass().getSimpleName().equals("Renewal") && !a.getClass().getSimpleName().equals("Ticket"))
            return new ArrayList<>();
        List<Activity> pastActivities;
        if(a.getClass().getSimpleName().equals("Renewal")){
            Renewal r = (Renewal) a;
            Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.employer.id = :id order by r.id desc");
            q.setParameter("id",r.getEmployer().getId());
            try{
                pastActivities = (List<Activity>) q.getResultList();
            } catch (NoResultException e){
                pastActivities = new ArrayList<>();
            }
        } else {
            Ticket t = (Ticket) a;
            long cId;
            if(t.getPrimaryContact()==null)
                cId = t.getContact().getId();
            else
                cId = t.getPrimaryContact().getId();
            Query q = em.createQuery("SELECT t FROM Ticket t WHERE t.primaryContact.id = :id OR t.contact.id = :id order by t.id desc ");
            q.setParameter("id",cId);
            try{
                pastActivities = (List<Activity>) q.getResultList();
            } catch (NoResultException e){
                pastActivities = new ArrayList<>();
            }
        }
        return pastActivities;

    }
    public static void setActivityView(HttpServletRequest request, EntityManager em, Activity a){
        //vA.changeActivityView(request,em);
        setActivityViewOld(request,em,a);
        request.getSession().setAttribute("pspUserList",getPspUsers(em));
        request.getSession().setAttribute("bpoUserList",getBpoUsers(em));
    }

    private static List<Person> getBpoUsers(EntityManager em){
        return getUsersByRole(em,101);
    }

    private static List<Person> getUsersByRole(EntityManager em, int roleId){
        Query q = em.createQuery("SELECT ur FROM UserRole ur WHERE ur.id = :id");
        q.setParameter("id",roleId);
        UserRole ur = (UserRole) q.getSingleResult();
        List<User> users = ur.getUserList();
        List<Person> personList = new ArrayList<>();
        if(users==null || users.size()==0)
            return personList;
        for(User u:users){
            if(!personList.contains(u.getPerson()))
                personList.add(u.getPerson());
        }
        Collections.sort(personList);
        return personList;    }
    private static List<Person> getPspUsers(EntityManager em){
        return getUsersByRole(em,1);
    }

    private static void setActivityViewOld(HttpServletRequest request, EntityManager em, Activity a){
        Long id = a.getId();
        Activity selectedActivity = dM.getActivityById(em,id);
        assert selectedActivity != null;
        Person primaryContact = dActivity.getPrimaryContact(em,selectedActivity);
        request.getSession().setAttribute("currentPrimaryContact",primaryContact);
        request.getSession().setAttribute("otherContactList",selectedActivity.getAssigneeContactList());
        request.getSession().setAttribute("activityWebLinkList",selectedActivity.getWebLinkList());
        request.getSession().setAttribute("currentActivityId",id);
        request.getSession().setAttribute("currentActivity",selectedActivity);
        request.getSession().setAttribute("pastActivities",getPastActivities(request,em,selectedActivity));
        request.getSession().setAttribute("rfCodes",vA.getAutomationInsertLinks(em));
        CheckList c = getCheckListForActivity(em,selectedActivity);
        request.getSession().setAttribute("currentChecklist",c);
        String classType = selectedActivity.getClass().getSimpleName();
        switch (classType){
            case "Renewal":
                request.getSession().setAttribute("adminView",2);
                request.getSession().setAttribute("currentRenewal", dM.getRenewalById(em,id));
                request.getSession().setAttribute("currentSetup", new Setup());
                request.getSession().setAttribute("currentTicket", new Ticket());
                request.getSession().setAttribute("currentActivityEmployees", dActivity.getEmployeeList(em,selectedActivity));
                fillRenewalSpecificLists(request,em,dM.getRenewalById(em,id));
                break;
            case "Setup":
                request.getSession().setAttribute("adminView",1);
                request.getSession().setAttribute("currentRenewal", new Renewal());
                request.getSession().setAttribute("currentSetup", dM.getSetupById(em,id));
                request.getSession().setAttribute("currentTicket", new Ticket());
                fillSetupSpecificLists(request,em, Objects.requireNonNull(dM.getSetupById(em, id)));
                break;
            case "Ticket":
                request.getSession().setAttribute("adminView",3);
                request.getSession().setAttribute("currentRenewal", new Renewal());
                request.getSession().setAttribute("currentSetup", new Setup());
                Ticket t = dM.getTicketById(em,id);
                request.getSession().setAttribute("currentTicket", t);
                Employee e = getTicketEmployee(em,t);
                request.getSession().setAttribute("currentEeId","");
                request.getSession().setAttribute("currentEeAltId","");
                request.getSession().setAttribute("currentErId","");
                request.getSession().setAttribute("currentErAltId","");
                request.getSession().setAttribute("tIsEmployee",0);
                request.getSession().setAttribute("currentEmployer3","");
                request.getSession().setAttribute("currentActivityEmployees",new ArrayList<>());
                if(e!=null){
                    request.getSession().setAttribute("tIsEmployee",1);
                    request.getSession().setAttribute("currentActivityEmployees",dActivity.getEmployeeList(em,e.getEmployer()));
                    request.getSession().setAttribute("currentEeId",e.getId());
                    request.getSession().setAttribute("currentEeAltId",e.getMmKey());
                    request.getSession().setAttribute("currentErId",e.getEmployer().getId());
                    request.getSession().setAttribute("currentErAltId",e.getEmployer().getErKey());
                    request.getSession().setAttribute("currentEmployer3",e.getEmployer().getEmployerName());
                }
                break;
            default:
                request.getSession().setAttribute("adminView",999);
                request.getSession().setAttribute("currentRenewal", new Renewal());
                request.getSession().setAttribute("currentSetup", new Setup());
                request.getSession().setAttribute("currentTicket", new Ticket());
                break;
        }
        try{
            List<ToDo> toDoList = ViewSelectedChecklist.getToDoListByChecklistId(em,c.getId());
            request.getSession().setAttribute("currentToDoList",toDoList);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Employee getTicketEmployee(EntityManager em, Ticket ticket){
        if(!isPersonAnEmployee(em,ticket.getContact()))
            return null;
        Query q = em.createQuery("SELECT e FROM Employee e WHERE e.id = :id");
        q.setParameter("id",ticket.getContact().getEmployee().getId());
        List<Employee> employeeList;
        try{
            employeeList = (List<Employee>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return employeeList.get(0);
    }

    private static boolean isPersonAnEmployee(EntityManager em, Person p){
        Query q = em.createQuery("SELECT p FROM Person p INNER JOIN Employee e ON e.id = p.employee.id");
        List<Person> personList;
        try{
            personList = (List<Person>) q.getResultList();
        } catch (NoResultException e){
            return false;
        }
        if(personList.contains(p))
            return true;
        return false;
    }


    private static void assureChecklistAssignedToActivity(EntityManager em, CheckList c){
        String classType = c.getAssignedTo().getClass().getSimpleName();
        switch (classType) {
            case "Renewal":
                em.getTransaction().begin();
                Renewal r = dM.getRenewalById(em, c.getAssignedTo().getId());
                r.setCheckList(c);
                em.persist(r);
                em.getTransaction().commit();
                break;
            case "Setup":
                em.getTransaction().begin();
                Setup s = (Setup) dM.getActivityById(em, c.getAssignedTo().getId());
                s.setCheckList(c);
                em.persist(s);
                em.getTransaction().commit();
                break;
            case "Ticket":
                em.getTransaction().begin();;
                Ticket t = (Ticket) dM.getActivityById(em,c.getAssignedTo().getId());
                t.setCheckList(c);
                em.persist(t);
                em.getTransaction().commit();
            default:
                break;
        }
    }



    public static CheckList getCheckListForActivity(EntityManager em, Activity a){
        CheckList c;
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :id");
        q.setParameter("id",a.getId());
        try{
            c = (CheckList) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            c = createCheckList(em,a);
        }
        assureChecklistAssignedToActivity(em,c);
        return c;
    }



    private static void fillRenewalItems(EntityManager em, Renewal r){
        for(ToDo t:r.getCheckList().getToDoList()){
            if(t.isComplete() && t.getDateCompleted()==dbA.getFalseCloseDate(em)){
                em.getTransaction().begin();
                ToDo toDo = dM.getToDoById(em,t.getId());
                toDo.setComplete(false);
                toDo.setDateCompleted(null);
                em.persist(toDo);
                em.getTransaction().commit();
            }
        }

        List<SortedTask> tasksToAdd = getTasksToAdd(em,r);
        for(SortedTask st: tasksToAdd){
            em.getTransaction().begin();
            ToDo t = new ToDo();
            t.setTask(st.getTask());
            t.setSortOrder(st.getSortOrder());
            t.setComplete(false);
            t.setCheckList(r.getCheckList());
            em.persist(t);
            em.getTransaction().commit();
            em.getTransaction().begin();
            CheckList c = dM.getCheckListById(em,r.getCheckList().getId());
            c.getToDoList().add(t);
            em.persist(c);
            em.getTransaction().commit();
        }

        List<Task> tasksNotNeeded = getTasksNotNeeded(em,r);
        for(Task t:tasksNotNeeded){
            if(t.getId() == 1463L) continue;
            Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.task.id = :taskId AND t.checkList.id = :checkId");
            q.setParameter("taskId",t.getId());
            q.setParameter("checkId",r.getCheckList().getId());
            try{
                em.getTransaction().begin();
                ToDo toDo = (ToDo) q.getSingleResult();
                if(!toDo.isComplete()){
                    toDo.setComplete(true);
                    toDo.setDateCompleted(dbA.getFalseCloseDate(em));
                    em.persist(toDo);
                }
                em.getTransaction().commit();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    private static List<Task> getTasksNotNeeded(EntityManager em, Renewal r){
        List<Task> tasksNotNeeded = new ArrayList<>();
        for(Task existingTask: getTasksAlreadyInToDoList(em,r)){
            boolean isNecessary = false;
            for(SortedTask requiredTask: getTasksRequiredList(em,r)){
                if(existingTask.getId().equals(requiredTask.getTask().getId())){
                    isNecessary = true;
                    break;
                }
            }
            if(!isNecessary)
                tasksNotNeeded.add(existingTask);
        }
        return tasksNotNeeded;
    }



    private static List<SortedTask> getTasksToAdd(EntityManager em, Renewal r){
        List<SortedTask> tasksToAdd = new ArrayList<>();
        for(SortedTask requiredTask:getTasksRequiredList(em,r)){
            boolean alreadyPresent = false;
            for(Task existingTask: getTasksAlreadyInToDoList(em,r)){
                if(existingTask.getId().equals(requiredTask.getTask().getId())){
                    alreadyPresent = true;
                    break;
                }
            }
            if(!alreadyPresent)
                tasksToAdd.add(requiredTask);
        }
        return tasksToAdd;
    }

    private static List<Task> getTasksAlreadyInToDoList(EntityManager em, Renewal r){
        List<Task> taskList = new ArrayList<>();
        List<ToDo> toDoList = r.getCheckList().getToDoList();
        for(ToDo toDo:toDoList)
            taskList.add(toDo.getTask());
        return taskList;
    }

    private static List<SortedTask> getTasksRequiredList(EntityManager em, Renewal r){
        List<Task> tasksRequired = new ArrayList<>();
        List<SortedTask> sortedTaskList = new ArrayList<>();
        for(RenewalItem renewalItem: r.getRenewalItemList()){
            TemplatePurpose itemsPurpose = renewalItem.getBenefit().getPlanType().getTemplatePurpose();
            List<RequiredTaskList> requiredTaskLists;
            Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl WHERE rtl.templatePurpose.id = :id");
            q.setParameter("id",itemsPurpose.getId());
            try{
                requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
            } catch (NoResultException e){
                e.printStackTrace();
                continue;
            }
            for(RequiredTaskList rtl:requiredTaskLists){
                System.out.println("RTL NAME: " + rtl.getDescription());
                for(TaskSequenceTable tst:rtl.getTaskSequenceTableList()){
                    Task taskToCheck = tst.getTask();
                    boolean notInList = true;
                    for(Task t: tasksRequired){
                        if(t.getId().equals(taskToCheck.getId())){
                            notInList = false;
                            break;
                        }
                    }
                    if(notInList) {
                        tasksRequired.add(taskToCheck);
                        SortedTask st = new SortedTask(tst.getTask(),tst.getSortOrder());
                        sortedTaskList.add(st);
                        System.out.println("Sorted Task Added: " + st.getTask().getDescription());
                    }
                }
            }
        }
        return sortedTaskList;
    }

    private static CheckList createCheckList(EntityManager em, Activity a){
        String classType = a.getClass().getSimpleName();
        CheckList c = new CheckList();
        em.getTransaction().begin();
        c.setAssignedTo(a);
        c.setComplete(false);
        c.setFullName(a.getFullName()+" Checklist");
        c.setDueDate(a.getDueDate());
        c.setLoggedBy((Person) a.getAssignedTo());
        em.persist(c);
        em.getTransaction().commit();
        if(classType.equals("Ticket")){
            CreateTicket.createToDoList(em,(Ticket) a,c);
        } else {
            Task t = dC.createTaskOneTime(em,"Default",((Person) a.getAssignedTo()).getPsp());
            ToDo td = new ToDo();
            em.getTransaction().begin();
            td.setCheckList(c);
            td.setTask(t);
            td.setSortOrder(99999);
            td.setComplete(true);
            em.persist(td);
            em.getTransaction().commit();
        }

        return c;
    }
}
