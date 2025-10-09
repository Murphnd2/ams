package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.misc.dbS;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.PersonV;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.application.Application;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GenerateProp25", value = "/GenerateProp25")
public class GenerateProp25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    private void doThis(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Agency agency = getAgency(em);

        String contactName = request.getParameter("contact");
        String contactEmail = request.getParameter("email");
        Person contact = createPerson(em,contactName,contactEmail,agency,local);

        String customerName = request.getParameter("cname");
        Prospect prospect = createProspect(em,customerName,contact,agency);

        Proposal proposal = createProposal(request,em,prospect);
        fillProposal(request,em, proposal);

        Application application = createApplication(em,proposal);
        fillApplication(request,application,em);

        CheckList checkList = createChecklist(request,em,customerName);
        Setup setup = createSetup(request,em,prospect,application,checkList);
        fillToDoList(em,setup);

        List<Activity25u> listToModify = new ArrayList<>(global.getActivitiesAllOpen());
        Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
        q.setParameter("id",setup.getId());
        Activity25 a25 = (Activity25) q.getSingleResult();
        Activity25u au = new Activity25u(a25);
        listToModify.add(au);
        global.setActivitiesAllOpen(listToModify);
        local.setActivitiesAllOpen(global.getActivitiesAllOpen());
        local.getCurrentActivity().setReFilterOnExit(true);

        request.getSession().setAttribute("local",local);
        request.getServletContext().setAttribute("global",global);

        em.close();
        goAdminHomePage(request,response);
    }
    private void goAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);

    }
    private Agency getAgency(EntityManager em){
        Query q = em.createQuery("SELECT a FROM Agency a order by a.id");
        List<Agency> agencies;
        try{
            agencies = (List<Agency>) q.getResultList();
        } catch (NoResultException e){
            return new Agency();
        }
        return agencies.get(0);
    }
    private Person createPerson(EntityManager em,String contact, String email, Agency a, AmsDataLocal local){
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.email = :email");
        q.setParameter("email",email.toLowerCase().trim());
        List<PersonV> personVList;
        try{
            personVList = (List<PersonV>) q.getResultList();
        } catch (NoResultException e){
            personVList = null;
        }

        if(personVList!=null && personVList.size()>0){
            return dM.getPersonById(em,personVList.get(0).getId());
        }
        int index = contact.indexOf(' ');
        em.getTransaction().begin();
        Person p = new Person();
        p.setFullName(contact);
        if(index>0){
            p.setFirstName(contact.substring(0,index));
            p.setLastName(contact.substring(index+1));
        } else {
            p.setFirstName(contact);
            p.setLastName(contact);
        }

        p.setEmail(email);
        p.setPsp(local.getCurrentPerson().getPsp());
        p.setAddress(a.getAddress());
        em.persist(p);
        em.getTransaction().commit();
        System.out.println("Person: " + p.getFirstName()+" "+p.getLastName());
        return p;
    }
    private Prospect createProspect(EntityManager em, String customerName, Person contact, Agency a){
        Person agent = getAgent(em,a);
        em.getTransaction().begin();
        Prospect p = new Prospect();
        p.setName(customerName);
        p.setContact(contact);
        p.setAddress(contact.getAddress());
        p.setAgent(agent);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private Person getAgent(EntityManager em, Agency agency){
        if(agency.getAgentList()!=null && agency.getAgentList().size()>0)
            return agency.getAgentList().get(0);
        Person p = dM.getPersonById(em,104L);
        em.getTransaction().begin();
        agency.addAgent(p);
        em.persist(agency);
        em.getTransaction().commit();
        return p;
    }
    private Proposal createProposal(HttpServletRequest request,EntityManager em,Prospect p){
        String propKey = request.getParameter("app_key");
        Rate rate = dM.getRateById(em,52L);
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
        Proposal proposal = dM.getProposalById(em,p.getId());
        LOS los = dG.getLosFull(em,losID);
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
        TemplatePurpose tp = dM.getTemplatePurposeById(em,templatePurposeId);


        dActivity.addModule(em,a,tp);

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

        Task t = dM.getTaskById(em,153L);
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
        CheckList c = dM.getCheckListById(em,checkList.getId());
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
        List<SortedTask> sortedTaskList = dbS.getTasksRequiredForApplication(em,a);
        if(sortedTaskList.size()==0) {
            sortedTaskList.add(new SortedTask(dM.getTaskById(em, 153L), 1000));
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
            CheckList checkList = dM.getCheckListById(em,c.getId());
            assert checkList != null;
            checkList.getToDoList().add(toDo);
            em.persist(checkList);
            em.getTransaction().commit();
            System.out.println("Todo created for Task: "+ st.getTask().getDescription());
        }
    }
}
