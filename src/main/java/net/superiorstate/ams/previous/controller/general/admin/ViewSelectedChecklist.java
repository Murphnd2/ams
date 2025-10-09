package net.superiorstate.ams.previous.controller.general.admin;

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
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ViewSelectedChecklist", value = "/ViewSelectedChecklist")
public class ViewSelectedChecklist extends HttpServlet {
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

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        String buttonValue = request.getParameter("btnCheckList");
        if(buttonValue.contains("R-")){
            Long id = Long.parseLong(buttonValue.substring(2));
            Long personId = Long.parseLong(request.getParameter("userList"));
            em.getTransaction().begin();
            CheckList c = dM.getCheckListById(em,id);
            Person p = dM.getPersonById(em,personId);
            c.setAssignedTo(p);
            em.persist(c);
            em.getTransaction().commit();
            request.getSession().setAttribute("currentChecklist", new CheckList());
            request.getSession().setAttribute("currentToDoList",new ArrayList<>());
            request.getSession().setAttribute("adminView",0);
            request.getSession().setAttribute("currentActivity",c );
        }else{
            Long id = Long.parseLong(request.getParameter("btnCheckList"));
            CheckList c = dM.getCheckListById(em,id);
            request.getSession().setAttribute("currentPrimaryContact",dM.getPersonById(em,c.getAssignedTo().getId()));
            System.out.println(c.getDateCreated());
            request.getSession().setAttribute("currentChecklist", dM.getCheckListById(em,id));
            request.getSession().setAttribute("currentToDoList",getToDoListByChecklistId(em,id));
            request.getSession().setAttribute("adminView",4);
            request.getSession().setAttribute("currentActivity", dM.getCheckListById(em,id) );
        }
        request.getSession().setAttribute("currentSetup", new Setup());
        request.getSession().setAttribute("currentTicket", new Ticket());
        request.getSession().setAttribute("currentRenewal",new Renewal());
        em.close();
    }

    public static List<ToDo> getToDoListByChecklistId(EntityManager em, Long id){
        Query q = em.createQuery("SELECT t FROM ToDo t JOIN FETCH t.task task WHERE t.checkList.id = :id ORDER BY t.isComplete, t.sortOrder");
        q.setParameter("id",id);
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            toDoList = new ArrayList<>();
        }
        return toDoList;
    }
}
