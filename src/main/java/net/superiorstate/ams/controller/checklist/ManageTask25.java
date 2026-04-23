package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoOut;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.sales.agency.Agency;

import java.io.IOException;

@WebServlet(name = "ManageTask25", value = "/ManageTask25")
public class ManageTask25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewTaskManager(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewTaskManager(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/taskManager/taskManager25.jsp");
        dispatcher.forward(request,response);
    }

    private void viewTaskManager(HttpServletRequest request){
        String toDoIdString;
        try{
            toDoIdString = request.getParameter("toDoId").toString();
        } catch (Exception e){
            return;
        }
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long toDoId = Long.parseLong(toDoIdString);
        System.out.println("ToDoId: "+ toDoId);
        Query q = em.createQuery("SELECT t FROM ToDoOut t WHERE t.toDo.id = :id");
        q.setParameter("id",toDoId);
        ToDoOut toDoOut;
        try{
            toDoOut = (ToDoOut) q.getSingleResult();
        } catch (Exception e){
            em.close();
            return;
        }
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        local.setCurrentToDoOut(toDoOut);
        ToDo currentToDo = EntityLookup.getToDoById(em, toDoOut.getToDo().getId());
        local.setCurrentToDo(currentToDo);
        request.getSession().setAttribute("local",local);

        // V061: resolve originating agency for agent-delegation override row.
        // Only populated when this ToDo belongs to a Setup with a resolvable agency.
        resolveOriginatingAgency(request, currentToDo);

        em.close();
    }

    /**
     * Sets `originatingAgency` as a request attribute (null if not resolvable).
     * When non-null, taskManager25.jsp renders the per-Setup override sub-row
     * with PSP users + that agency's agents in the dropdown.
     */
    private void resolveOriginatingAgency(HttpServletRequest request, ToDo toDo) {
        request.setAttribute("originatingAgency", null);
        if (toDo == null) return;
        CheckList cl = toDo.getCheckList();
        if (cl == null) return;
        Setup setup = cl.getSetup();
        if (setup == null) return;
        Agency agency = OriginatingAgencyResolver.resolve(setup);
        if (agency == null) return;
        request.setAttribute("originatingAgency", agency);
        request.setAttribute("originatingAgents", agency.getAgentList());
    }
}
