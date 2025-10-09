package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;

import java.io.IOException;

@WebServlet(name = "RemoveLinkFromTask", value = "/RemoveLinkFromTask")
public class RemoveLinkFromTask extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeLink(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("TaskDetailView");
        dispatcher.forward(request,response);
    }

    private void removeLink(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Long selectedLinkId = Long.parseLong(request.getParameter("linkSelectButton"));
        WebLink webLink = ddC.getWebLinkById(em,selectedLinkId);
        Task currentTask = (Task) request.getSession().getAttribute("currentTask");
        em.getTransaction().begin();
        Task task = dM.getTaskById(em,currentTask.getId());
        if(task.getWebLinkList().contains(webLink)){
            task.removeWebLink(webLink);
            em.persist(task);
            em.persist(webLink);
        }
        em.getTransaction().commit();
        em.close();
    }
}
