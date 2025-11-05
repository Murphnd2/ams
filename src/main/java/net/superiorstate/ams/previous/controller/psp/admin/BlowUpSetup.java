package net.superiorstate.ams.previous.controller.psp.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;
import net.superiorstate.ams.previous.model.sales.agency.Prospect;
import net.superiorstate.ams.previous.model.sales.application.Application;
import net.superiorstate.ams.previous.model.sales.application.ApplicationModule;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "BlowUpSetup", value = "/BlowUpSetup")
public class BlowUpSetup extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = request.getParameter("guid");
        blowUpSetup(request,guid);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Got This Far");
        String guid = request.getParameter("guid");
        blowUpSetup(request,guid);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetAdminView");
        dispatcher.forward(request,response);
    }

    private void blowUpSetup(HttpServletRequest request, String guid){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        CheckList c = getChecklistByGuid(em,guid);
        clearToDos(em,c);
        System.out.println("Removed ToDos");

        Setup s = getSetupByGuid(em,guid);
        reassignChecklistToSetup(em,s);
        System.out.println("Swapped assigned checklist");

        em.getTransaction().begin();
        CheckList c1 = dM.getCheckListById(em,c.getId());
        em.remove(c1);
        em.getTransaction().commit();
        System.out.println("Delete Checklist");

        em.getTransaction().begin();
        Setup setup = dM.getSetupById(em,s.getId());
        em.remove(setup);
        em.getTransaction().commit();
        System.out.println("Delete Setup");

        Application application = getApplicationByGuid(em,guid);
        clearApplicationModules(em,application);
        System.out.println("Delete Modules");

        em.getTransaction().begin();
        Application a = getApplicationByGuid(em,guid);
        em.remove(a);
        em.getTransaction().commit();
        System.out.println("Delete Application");

        Proposal proposal = getProposalByGuid(em,guid);
        clearProposalItems(em,proposal);
        System.out.println("Delete Proposal Items");

        Prospect prospect = proposal.getProspect();
        em.getTransaction().begin();
        Proposal p = getProposalByGuid(em,guid);
        em.remove(p);
        em.getTransaction().commit();
        System.out.println("Delete Proposal");

        Person contact = prospect.getContact();
        em.getTransaction().begin();
        Prospect pr = dM.getProspectById(em,prospect.getId());
        em.remove(pr);
        em.getTransaction().commit();
        System.out.println("Delete Prospect");

        em.getTransaction().begin();
        Person person = dM.getPersonById(em,contact.getId());
        em.remove(person);
        em.getTransaction().commit();
        System.out.println("Delete Contact");


    }


    private void clearToDos(EntityManager em, CheckList c){
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId");
        q.setParameter("cId",c.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        if(toDoList.size()==0)
            return;
        for(ToDo t: toDoList){
            em.getTransaction().begin();
            em.remove(t);
            em.getTransaction().commit();
        }
    }

    private void clearProposalItems(EntityManager em, Proposal p){
        if(p.getLosList() !=null && p.getLosList().size()>0){
            for(int i = 0; i < p.getLosList().size(); i++){
                em.getTransaction().begin();
                LOS los = dM.getLosById(p.getLosList().get(i).getId());
                Proposal proposal = dM.getProposalById(em,p.getId());
                proposal.getLosList().remove(los);
                los.getListOfProposalsThatIncludeThisLOS().remove(proposal);
                em.persist(proposal);
                em.persist(los);
                em.getTransaction().commit();
            }
        }
    }
    private void clearApplicationModules(EntityManager em, Application a){
        Query q = em.createQuery("SELECT am FROM ApplicationModule am WHERE am.application.proposal.id = :pId");
        q.setParameter("pId",a.getProposal().getId());
        List<ApplicationModule> applicationModuleList;
        try{
            applicationModuleList = (List<ApplicationModule>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        for(ApplicationModule am:applicationModuleList){
            em.getTransaction().begin();
            em.remove(am);
            em.getTransaction().commit();
        }
        return;
    }

    private Proposal getProposalByGuid(EntityManager em, String guid){
        Query q = em.createQuery("SELECT p FROM Proposal p WHERE p.applicationGUID = :guid");
        q.setParameter("guid",guid);
        Proposal p;
        try{
            p = (Proposal) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return p;
    }

    private Application getApplicationByGuid(EntityManager em, String guid){
        Query q = em.createQuery("SELECT a FROM Application a WHERE a.proposal.id = :pId");
        Proposal p = getProposalByGuid(em,guid);
        q.setParameter("pId",p.getId());
        Application a;
        try{
            a = (Application) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return a;
    }
    private Setup getSetupByGuid(EntityManager em, String guid){
        Query q = em.createQuery("SELECT s FROM Setup s WHERE s.application.proposal.id = :pId");
        Proposal p = getProposalByGuid(em,guid);
        q.setParameter("pId", p.getId());
        Setup s;
        try{
            s = (Setup) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return s;
    }

    private CheckList getChecklistByGuid(EntityManager em,String guid){
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.assignedTo.id = :sId");
        Setup s = getSetupByGuid(em,guid);
        q.setParameter("sId",s.getId());
        CheckList c;
        try{
            c = (CheckList) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return c;
    }

    private void reassignChecklistToSetup(EntityManager em, Setup s){
        CheckList checkList = s.getCheckList();
        em.getTransaction().begin();
        CheckList c = dM.getCheckListById(em,checkList.getId());
        Person p = dM.getPersonById(em,2L);
        assert c != null;
        c.setAssignedTo(p);
        em.persist(c);
        em.getTransaction().commit();

        em.getTransaction().begin();
        Setup setup = dM.getSetupById(em,s.getId());
        CheckList tempChecklist = dM.getCheckListById(em,128L);
        assert setup != null;
        setup.setCheckList(tempChecklist);
        em.persist(setup);
        em.getTransaction().commit();

    }
}
