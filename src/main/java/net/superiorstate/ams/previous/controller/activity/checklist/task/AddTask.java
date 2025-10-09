package net.superiorstate.ams.previous.controller.activity.checklist.task;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;

import java.io.IOException;

@WebServlet(name = "AddTask", value = "/AddTask")
public class AddTask extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addTask(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("TaskDetailView");
        dispatcher.forward(request,response);
    }
    private void addTask(HttpServletRequest request) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        String taskDescription = request.getParameter("task_name");
        Task task = new Task();
        task.setDescription(taskDescription);
        task.setPsp((PSP) request.getSession().getAttribute("psp"));
        task.setReUsable(true);
        task.setAllowNonOwner(true);
        task.setAllowFuture(true);
        task.setAllowEarly(true);
        em.getTransaction().begin();
        em.persist(task);
        em.getTransaction().commit();

        String path = request.getParameter("linkPath");
        String description = request.getParameter("linkName");
        WebLink webLink = null;
        if((path != null && !path.equals("")) && (description !=null && !description.equals(""))) {
            webLink = getWebLink(em, path, description);
            em.getTransaction().begin();
            task.addWebLink(webLink);
            em.persist(task);
            em.persist(webLink);
            em.getTransaction().commit();
        }
        em.close();
        request.getSession().setAttribute("currentTask",task);
        request.getSession().setAttribute("hasCurrentTask",true);
        request.getSession().setAttribute("checkView",1);
    }


    private WebLink getWebLink(EntityManager em, String path, String description){
        WebLink webLink = new WebLink();
        LinkType linkType = ddC.getLinkTypeById(em,2);
        webLink.setLinkType(linkType);
        webLink.setLinkPath(path);
        webLink.setPlainText(description);
        em.getTransaction().begin();
        em.persist(webLink);
        em.getTransaction().commit();
        return webLink;
    }
}
