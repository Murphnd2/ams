package net.superiorstate.ams.previous.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.misc.dbS;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.application.Application;
import net.superiorstate.ams.previous.model.sales.application.ApplicationModule;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "StartSetup", value = "/StartSetup")
public class StartSetup extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String appKey = request.getParameter("app_key");

        if(!isValidAppKey(appKey)) {
            goHomePage(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Proposal p = getProposalFromGuid(appKey);
        if(!applicationStarted(p))
            createApplication(request,appKey);

        Application a = getApplicationFromProposal(p);
        createModules(request,a,em);

        if(!setupStarted(a))
            createSetup(request,em,a);

        Setup s = getSetupFromApplication(a);

        fillToDoList(request,appKey, em);
        em.close();

        goAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void fillToDoList(HttpServletRequest request,String appKey, EntityManager em){
        Proposal p = getProposalFromGuid(appKey);
        Application a = getApplicationFromProposal(p);
        Setup s = dM.getSetupById(em,a.getProposal().getId());
        assert s != null;
        CheckList c = dM.getCheckListById(em,s.getCheckList().getId());
        List<SortedTask> sortedTaskList = dbS.getTasksRequiredForApplication(em,dM.getApplicationById(em, p.getId()));
        if(sortedTaskList.size()==0)
            sortedTaskList.add(new SortedTask(dM.getTaskById(em,129L),1000));
        for(SortedTask st: sortedTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
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
        }
        ViewSelectedActivity.setActivityView(request,em,s);
    }

    private void createModules(HttpServletRequest request, Application a, EntityManager em){
        String wPop = request.getParameter("q1");
        System.out.println("POP: " + wPop);
        String wFsa = request.getParameter("q2");
        System.out.println("FSA: " + wFsa);
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

    private Person createPrimaryContact(HttpServletRequest request, EntityManager em){
        String contactName = request.getParameter("q10");
        String contactEmail = request.getParameter("q11");
        em.getTransaction().begin();
        Person person = new Person();
        person.setFullName(contactName);
        person.setEmail(contactEmail);
        if(contactName.indexOf(",")>0){
            person.setLastName(contactName.substring(0,contactName.indexOf(",")));
            String fName = contactName.substring(contactName.indexOf(",")+1,contactName.length());
            person.setFirstName(fName.trim());
        } else {
            String fName = contactName.substring(0,contactName.indexOf(" "));
            String lName = contactName.substring(contactName.indexOf(" ")+1,contactName.length());
            person.setLastName(lName);
            person.setFirstName(fName);
        }
        em.persist(person);
        em.getTransaction().commit();
        return person;
    }

    private Setup createSetup(HttpServletRequest request, EntityManager em, Application a){
        Person person = createPrimaryContact(request,em);
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        String erName = request.getParameter("q9");

        CheckList c = createCheckList(request,em);

        em.getTransaction().begin();
        Setup setup = new Setup();
        setup.setApplication(a);
        setup.setPrimaryContactSetup(person);
        setup.setComplete(false);
        setup.setFullName(erName);
        setup.setLoggedBy(currentPerson);
        setup.setAssignedTo(currentPerson);
        setup.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2)));
        setup.setCheckList(c);
        em.persist(setup);
        em.getTransaction().commit();

        em.getTransaction().begin();
        CheckList checkList = dM.getCheckListById(em,c.getId());
        assert checkList != null;
        checkList.setAssignedTo(setup);
        checkList.setSetup(setup);
        em.persist(checkList);
        em.getTransaction().commit();

        return setup;
    }

    private CheckList createCheckList(HttpServletRequest request, EntityManager em){
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        String erName = request.getParameter("q9");
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setFullName(erName + " Checklist");
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setComplete(false);
        c.setLoggedBy(currentPerson);
        em.persist(c);
        em.getTransaction().commit();
        return c;
    }

    private void createApplication(HttpServletRequest request, String appKey){
        Proposal p = getProposalFromGuid(appKey);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Application a = new Application();
        a.setProposal(p);
        em.persist(a);
        em.getTransaction().commit();
        em.close();
    }

    private void addModule(EntityManager em, Application a, int templatePurposeId){
        TemplatePurpose tp = dM.getTemplatePurposeById(em,templatePurposeId);
        if(moduleExists(em,a,tp))
            return;

        em.getTransaction().begin();
        ApplicationModule am = new ApplicationModule();
        am.setTemplatePurpose(tp);
        am.setApplication(a);
        em.persist(am);
        em.getTransaction().commit();

        Application application = dM.getApplicationById(em,a.getProposal().getId());

        em.getTransaction().begin();
        application.getApplicationModuleList().add(am);
        em.persist(application);
        em.getTransaction().commit();
    }

    private boolean moduleExists(EntityManager em, Application a, TemplatePurpose tp){
        Query q = em.createQuery("SELECT am FROM ApplicationModule am WHERE am.application.proposal.id = :aId AND am.templatePurpose.id = :tpId");
        q.setParameter("aId",a.getProposal().getId());
        q.setParameter("tpId",tp.getId());
        ApplicationModule applicationModule;
        try{
            applicationModule = (ApplicationModule) q.getSingleResult();
        }catch (NoResultException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }



    private void goAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);

    }
    private void goHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

    private Application getApplicationFromProposal(Proposal p){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT a FROM Application a WHERE a.proposal.id = :propId");
        q.setParameter("propId",p.getId());
        Application a;
        try{
            a = (Application) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            a = null;
        }
        em.close();
        return a;
    }

    private boolean setupStarted(Application a){
        Setup s = getSetupFromApplication(a);
        if(s==null)
            return false;
        return true;
    }

    private boolean applicationStarted(Proposal p){
        Application a = getApplicationFromProposal(p);
        if(a==null)
            return false;
        return true;
    }

    private Setup getSetupFromApplication(Application a){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT s FROM Setup s WHERE s.application.proposal.id = :id");
        q.setParameter("id",a.getProposal().getId());
        Setup s;
        try{
            s = (Setup) q.getSingleResult();
        }catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        return s;
    }

    private Proposal getProposalFromGuid(String appKey){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT p FROM Proposal p WHERE p.applicationGUID = :guid");
        q.setParameter("guid",appKey);
        Proposal p;
        try{
            p = (Proposal) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            p = null;
        }
        em.close();
        return p;
    }

    private boolean isValidAppKey(String appKey){
        Proposal p = getProposalFromGuid(appKey);
        if(p==null)
            return false;
        return true;
    }
}
