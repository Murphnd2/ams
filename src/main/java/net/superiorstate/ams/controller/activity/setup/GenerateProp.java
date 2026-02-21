package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.ActivityDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.dao.ApplicationTaskDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.ActivityViewHelper;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "GenerateProp", value = "/GenerateProp")
public class GenerateProp extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    private void doThis(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        String agencyName = request.getParameter("agency");
        Agency agency = getAgency(em,agencyName);

        String contactName = request.getParameter("contact");
        String contactEmail = request.getParameter("email");
        Person contact = createPerson(em,contactName,contactEmail,agency);

        String customerName = request.getParameter("cname");
        Prospect prospect = createProspect(em,customerName,contact,agency);

        Proposal proposal = createProposal(request,em,prospect);
        fillProposal(request,em, proposal);

        Application application = createApplication(em,proposal);
        fillApplication(request,application,em);

        CheckList checkList = createChecklist(request,em,customerName);
        Setup setup = createSetup(request,em,prospect,application,checkList);
        fillToDoList(em,setup);

        ActivityViewHelper.setActivityView(request,em,setup.getId());

        em.close();

        goAdminHomePage(request,response);
    }
    private void goAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);

    }
    private Agency getAgency(EntityManager em, String agency){
        Agency a;
        if(agency=="OneDigital"){
            a = SalesDAO.getAgencyFull(em,1L);
        } else if (agency=="Hummel"){
            a = SalesDAO.getAgencyFull(em,2L);
        } else {
            a = SalesDAO.getAgencyFull(em,14L);
        }
        System.out.println("Agency: " + a.getName());
        return a;
    }
    private Person createPerson(EntityManager em,String contact, String email, Agency a){
        em.getTransaction().begin();
        Person p = new Person();
        p.setFullName(contact);
        p.setFirstName(contact.substring(0,contact.indexOf(' ')));
        p.setLastName(contact.substring(contact.indexOf(' ')+1));
        p.setEmail(email);
        p.setPsp(EntityLookup.getPspById(em,4L));
        p.setAddress(a.getAddress());
        em.persist(p);
        em.getTransaction().commit();
        System.out.println("Person: " + p.getFirstName()+" "+p.getLastName());
        return p;
    }
    private Prospect createProspect(EntityManager em, String customerName, Person contact, Agency a){
        em.getTransaction().begin();
        Prospect p = new Prospect();
        p.setName(customerName);
        p.setContact(contact);
        p.setAddress(contact.getAddress());
        Person agent = getAgent(em,a);
        p.setAgent(agent);
        em.persist(p);
        em.getTransaction().commit();
        System.out.println("Prospect: "+ p.getName());
        return p;
    }

    private Person getAgent(EntityManager em, Agency agency){
        if(agency.getAgentList()!=null && agency.getAgentList().size()>0)
            return agency.getAgentList().get(0);
        Person p = EntityLookup.getPersonById(em,104L);
        em.getTransaction().begin();
        agency.addAgent(p);
        em.persist(agency);
        em.getTransaction().commit();
        return p;
    }
    private Proposal createProposal(HttpServletRequest request,EntityManager em,Prospect p){
        String propKey = request.getParameter("app_key");
        Rate rate = EntityLookup.getRateById(em,52L);
        em.getTransaction().begin();
        Proposal proposal = new Proposal();
        proposal.setRate(rate);
        proposal.setApplicationGUID(propKey);
        proposal.setProspect(p);
        em.persist(proposal);
        em.getTransaction().commit();
        System.out.println("Proposal: "+ proposal.getApplicationGUID());
        return proposal;
    }
    private void fillProposal(HttpServletRequest request,EntityManager em, Proposal p){
        String wPop = request.getParameter("q1");
        String wFsa = request.getParameter("q2");
        String wHra = request.getParameter("q3");
        String wHsa = request.getParameter("q4");
        String wTran = request.getParameter("q5");
        String wCob = request.getParameter("q6");
        if(wPop.equals("1"))
            addItem(em,p,5);
        if(wFsa.equals("1"))
            addItem(em,p,6);
        if(wHra.equals("1"))
            addItem(em,p,7);
        if(wHsa.equals("1"))
            addItem(em,p,9);
        if(wTran.equals("1"))
            addItem(em,p,10);
        if(wCob.equals("1"))
            addItem(em,p,8);
    }
    private void addItem(EntityManager em, Proposal p, int losID){
        em.getTransaction().begin();
        Proposal proposal = EntityLookup.getProposalById(em,p.getId());
        LOS los = SalesDAO.getLosFull(em,losID);
        proposal.getLosList().add(los);
        los.getListOfProposalsThatIncludeThisLOS().add(proposal);
        em.persist(proposal);
        em.persist(los);
        em.getTransaction().commit();
        System.out.println("Fill Proposal: " + losID);
    }
    private Application createApplication(EntityManager em, Proposal proposal){
        em.getTransaction().begin();
        Application a = new Application();
        a.setProposal(proposal);
        em.persist(a);
        em.getTransaction().commit();
        System.out.println("Application Created for Proposal: " + a.getProposal().getId());
        return a;
    }
    private void fillApplication(HttpServletRequest request, Application a, EntityManager em){
        String wPop = request.getParameter("q1");
        String wFsa = request.getParameter("q2");
        String wHra = request.getParameter("q3");
        String wHsa = request.getParameter("q4");
        String wTran = request.getParameter("q5");
        String wCob = request.getParameter("q6");
        String wPay = request.getParameter("q7");
        String wCrd = request.getParameter("q8");
        if(wPop.equals("1"))
            addModule(em,a,11);
        if(wFsa.equals("1"))
            addModule(em,a,12);
        if(wHra.equals("1"))
            addModule(em,a,13);
        if(wHsa.equals("1"))
            addModule(em,a,16);
        if(wTran.equals("1"))
            addModule(em,a,15);
        if(wCob.equals("1"))
            addModule(em,a,14);
        if(wPay.equals("1"))
            addModule(em,a,17);
        if(wCrd.equals("1"))
            addModule(em,a,19);
    }

    private void addModule(EntityManager em, Application a, int templatePurposeId){
        TemplatePurpose tp = EntityLookup.getTemplatePurposeById(em,templatePurposeId);


        ActivityDAO.addModule(em,a,tp);

    }
    private CheckList createChecklist(HttpServletRequest request, EntityManager em, String erName){
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setFullName(erName + " Checklist");
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setComplete(false);
        c.setLoggedBy(currentPerson);
        em.persist(c);
        em.getTransaction().commit();

        Task t = EntityLookup.getTaskById(em,153L);
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
        toDo.setCompletedBy(currentPerson);
        toDo.setComplete(true);
        toDo.setTask(t);
        toDo.setCheckList(c);
        toDo.setSortOrder(0);
        em.persist(toDo);
        em.getTransaction().commit();

        System.out.println("Checklist Created: " + c.getFullName() + " ID: " + c.getId());
        return c;
    }
    private Setup createSetup(HttpServletRequest request, EntityManager em, Prospect prospect,Application application, CheckList checkList){
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        em.getTransaction().begin();
        Setup setup = new Setup();
        setup.setApplication(application);
        setup.setPrimaryContactSetup(prospect.getContact());
        setup.setComplete(false);
        setup.setFullName(prospect.getName());
        setup.setLoggedBy(currentPerson);
        setup.setAssignedTo(currentPerson);
        setup.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2)));
        setup.setCheckList(checkList);
        em.persist(setup);
        em.getTransaction().commit();
        System.out.println("Setup Created for Proposal Guid:" + application.getProposal().getApplicationGUID());

        em.getTransaction().begin();
        CheckList c = EntityLookup.getCheckListById(em,checkList.getId());
        assert c != null;
        c.setAssignedTo(setup);
        c.setSetup(setup);
        em.persist(c);
        em.getTransaction().commit();
        System.out.println("Checklist assigned to Setup");

        return setup;
    }
    private void fillToDoList(EntityManager em, Setup setup){
        Application a = setup.getApplication();
        CheckList c = setup.getCheckList();
        List<SortedTask> sortedTaskList = ApplicationTaskDAO.getTasksRequiredForApplication(em,a);
        if(sortedTaskList.size()==0) {
            sortedTaskList.add(new SortedTask(EntityLookup.getTaskById(em, 153L), 1000));
        } else {
            System.out.println("Sorted Task List Size = " + sortedTaskList.size());
        }
        for(SortedTask st: sortedTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(false);
            if(st.getTask().getId()==153L)
                toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            assert c != null;
            CheckList checkList = EntityLookup.getCheckListById(em,c.getId());
            assert checkList != null;
            checkList.getToDoList().add(toDo);
            em.persist(checkList);
            em.getTransaction().commit();
            System.out.println("Todo created for Task: "+ st.getTask().getDescription());
        }
    }


}
