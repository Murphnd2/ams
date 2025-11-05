package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "GenerateRen", value = "/GenerateRen")
public class GenerateRen extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        String erKeyString = request.getParameter("erKey");
        int erKey = Integer.parseInt(erKeyString);
        Employer employer = getEmployer(em,erKey);
        if(employer==null) {
            System.out.println("Employer ID NOT FOUND: " + erKeyString);
            return;
        } else {
            System.out.println("Employer Found: " + employer.getEmployerName());
        }
        List<Employee> contactList = employer.getContactList();
        if(contactList == null || contactList.size()==0){
            String contactEmail = request.getParameter("email");
            Employee employee = dbEmail.getEmployeeByEmail(em,contactEmail);
            if(employee == null || employee.getId()==0){
                System.out.println("Email not found in employee list: " + contactEmail);
                employee = createNewEmployee(em,employer,contactEmail,currentPerson);
            } else {
                System.out.println("Employee ID FOUND : " + employee.getId());
            }
            assignEmployeeAsContact(em,employer,employee);
        } else{
            System.out.println("List Exists: 1st Contact is - " + contactList.get(0).getFirstName() + " " + contactList.get(0).getLastName() );
        }
        String dateForString = request.getParameter("dueDate");
        System.out.println("Due Date = " + dateForString);
        String renType = request.getParameter("renType");
        System.out.println("Renewal Type = " + renType);

        Renewal r = createRenewal(em,employer,currentPerson);
        CheckList c = createChecklist(em,r,currentPerson);

        createRenewalItems(em,r,dateForString,renType);
        createToDoList(em,r,c);

        ViewSelectedActivity.setActivityView(request,em,r);

        em.close();
        goAdminHomePage(request,response);

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    public static Employee createNewEmployee(EntityManager em, Employer er, String email, Person creator){
        return createNewEmployee(em,er,email,creator,"Employer","Contact");
    }

    public static Employee createNewEmployee(EntityManager em, Employer er, String email, Person creator, String first, String last){
        em.getTransaction().begin();
        Employee ee = new Employee();
        ee.setId(getNextTempId(em));
        ee.setEmail(email);
        ee.setFirstName(first);
        ee.setLastName(last);
        ee.setAddress1(creator.getAddress().getAddress1());
        ee.setAddress2(creator.getAddress().getAddress2());
        ee.setCity(creator.getAddress().getCity());
        ee.setState(creator.getAddress().getState());
        ee.setZipCode(creator.getAddress().getZipCode());
        ee.setEmployer(er);
        em.persist(ee);
        em.getTransaction().commit();
        return ee;
    }

    public static void assignEmployeeAsContact(EntityManager em, Employer er, Employee ee){
        em.getTransaction().begin();
        Employer employer = getEmployerByOrgId(em, er);
        er.getContactList().add(ee);
        em.persist(employer);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Employee employee = dM.getEmployeeById(em,ee.getId());
        ee.getEmployerList().add(employer);
        em.persist(employee);
        em.getTransaction().commit();
    }

    private static Employer getEmployerByOrgId(EntityManager em, Employer er){
        Query q = em.createQuery("SELECT e FROM Employer e where e.id = :id");
        q.setParameter("id",er.getId());
        return (Employer) q.getSingleResult();
    }
    private static int getNextTempId(EntityManager em){
        Query q = em.createQuery("SELECT MIN(e.id) as minEe FROM Employee e");
        int currentMin = (int) q.getSingleResult();
        if(currentMin>0)
            return -1;
        return currentMin-1;
    }
    private void goAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);

    }
    private void createToDoList(EntityManager em, Renewal r, CheckList c){
        List<SortedTask> sortedTaskList = dR.getTasksRequiredForRenewal(em,r);
        if(sortedTaskList.size()==0){
            sortedTaskList.add(new SortedTask(dM.getTaskById(em,129L),1000));
            System.out.println("Added Tod ID 129 because no tasks required");
        } else {
            System.out.println("Sorted Task List Size = " + sortedTaskList.size());
        }
        for(SortedTask st: sortedTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            if(st.getTask().getId()==129L)
                toDo.setComplete(true);
            else
                toDo.setComplete(false);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            assert c != null;
            CheckList checkList = dM.getCheckListById(em,c.getId());
            assert checkList != null;
            checkList.getToDoList().add(toDo);
            em.persist(checkList);
            em.getTransaction().commit();
            System.out.println("Todo created for Task: "+ st.getTask().getDescription());
        }
    }

    private void createRenewalItems(EntityManager em, Renewal r, String dateFor, String type){
        int renewalType = Integer.parseInt(type);
        em.getTransaction().begin();
        RenewalItem ri = new RenewalItem();
        int monthFor = Integer.parseInt(dateFor.substring(0,2));
        int dayFor = Integer.parseInt(dateFor.substring(3,5));
        int yearFor = Integer.parseInt(dateFor.substring(7));
        LocalDate localDate = LocalDate.of(yearFor,monthFor,dayFor);
        Date theDate = Date.valueOf(localDate);
        ri.setDateFor(theDate);
        ri.setRenewal(r);
        int benefitId;
        switch(renewalType){
            case 1: //FSA
                benefitId = 2;
                break;
            case 2: //HRA
                benefitId = 5;
                break;
            case 3: //COB MED
                benefitId = -8;
                break;
            case 4: //LP FSA
                benefitId = 4;
                break;
            case 5: //POP w HSA
                benefitId = -7;
                break;
            default: //POP
                benefitId = 1;
                break;
        }
        Benefit benefit = getBenefitById(em,benefitId);
        ri.setBenefit(benefit);
        em.persist(ri);
        em.getTransaction().commit();
        em.getTransaction().begin();
        Renewal rnw = dM.getRenewalById(em,r.getId());
        rnw.getRenewalItemList().add(ri);
        em.persist(rnw);
        em.getTransaction().commit();
        System.out.println("Renewal Item Created for Case " + renewalType);
    }

    private Benefit getBenefitById(EntityManager em, int benId){
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.id = :id");
        q.setParameter("id",benId);
        Benefit b;
        try{
            b = (Benefit) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return b;
    }

    private CheckList createChecklist(EntityManager em, Renewal r, Person creator){
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setLoggedBy(creator);
        c.setFullName(r.getFullName() + " Checklist");
        c.setRenewal(r);
        c.setAssignedTo(r);
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setComplete(false);
        em.persist(c);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Renewal renewal = dM.getRenewalById(em,r.getId());
        renewal.setCheckList(c);
        em.persist(renewal);
        em.getTransaction().commit();
        System.out.println("Checklist Created: ID: " + c.getId());
        return c;
    }
    private Renewal createRenewal(EntityManager em, Employer employer, Person creator){
        em.getTransaction().begin();
        Renewal r = new Renewal();
        r.setFullName(employer.getEmployerName());
        r.setEmployer(employer);
        r.setAssignedTo(creator);
        r.setLoggedBy(creator);
        r.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        em.persist(r);
        em.getTransaction().commit();
        System.out.println("Renewal Created - ID: " + r.getId());
        return r;
    }

    private static Employer getEmployer(EntityManager em, int erKey){
        Query q = em.createQuery("SELECT er FROM Employer er WHERE er.erKey = :erKey");
        q.setParameter("erKey",erKey);
        Employer employer;
        try{
            employer = (Employer) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return employer;
    }
}
