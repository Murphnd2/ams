package net.superiorstate.ams.previous.controller.activity.checklist.task;

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

@WebServlet(name = "AddWeblinkToTask", value = "/AddWeblinkToTask")
public class AddWeblinkToTask extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addLink(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("TaskDetailView");
        dispatcher.forward(request,response);
    }

    private void addLink(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        WebLink webLink = new WebLink();
        webLink.setLinkPath(request.getParameter("linkPath"));
        webLink.setLinkType(ddC.getLinkTypeById(em,2));
        webLink.setPlainText(request.getParameter("linkName"));
        em.persist(webLink);
        em.getTransaction().commit();

        Task currentTask = (Task) request.getSession().getAttribute("currentTask");
        em.getTransaction().begin();
        Task task = dM.getTaskById(em,currentTask.getId());
        assert task != null;
        task.addWebLink(webLink);
        em.persist(task);
        em.persist(webLink);
        em.getTransaction().commit();
        em.close();
    }
}
