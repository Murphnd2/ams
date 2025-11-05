package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "ReOpenChecklist", value = "/ReOpenChecklist")
public class ReOpenChecklist extends HttpServlet {

    private String whereToGo = "GoAdminHome";

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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(whereToGo);
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("adminView",0);
        Long id = Long.parseLong(request.getParameter("btnCheckList"));

        em.getTransaction().begin();
        CheckList c = dM.getCheckListById(em,id);
        c.setDateCompleted(null);
        c.setCompletedBy(null);
        c.setComplete(false);
        em.persist(c);
        em.getTransaction().commit();
        checkIfRecurring(request, em, c);
        request.getSession().setAttribute("currentChecklist", new CheckList());
        if(c.getToDoList().size()>1){
            request.getSession().setAttribute("currentChecklist",c);
            whereToGo = "ViewSelectedChecklist";
        }else{
            request.getSession().setAttribute("currentChecklist",new CheckList());
            request.getSession().setAttribute("adminView",0);
        }
        em.close();
    }
    public void checkIfRecurring(HttpServletRequest request,EntityManager em, CheckList checkList){
        if(checkList.getRecurringTaskList() == null)
            return;
        if(checkList.getRecurringTaskList().getId()<=0)
            return;
        RecurringTaskList rtl = dbRec.getRecurringListById(em,checkList.getRecurringTaskList().getId());
        if(rtl == null)
            return;
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.recurringTaskList.id = :id AND c.isComplete=false AND c.id > :cid order by c.id DESC");
        q.setParameter("id",rtl.getId());
        q.setParameter("cid",checkList.getId());
        List<CheckList> listOfFutureChecklists;
        try{
            listOfFutureChecklists = (List<CheckList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            listOfFutureChecklists = null;
        }
        if(listOfFutureChecklists == null)
            return;
        for(CheckList c:listOfFutureChecklists){
            em.getTransaction().begin();
            CheckList cUpdate = dM.getCheckListById(em,c.getId());
            cUpdate.setRecurringTaskList(null);
            cUpdate.setComplete(true);
            cUpdate.setFullName(c.getFullName() + "(*ERASED*)");
            cUpdate.setDateCompleted(Date.valueOf(LocalDate.now().minusDays(1)));
            cUpdate.setCompletedBy((Person) c.getAssignedTo());
            em.persist(cUpdate);
            em.getTransaction().commit();
        }
    }

}
