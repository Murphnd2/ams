package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.WebLink;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "TaskDetailView", value = "/TaskDetailView")
public class TaskDetailView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            getTaskDetail(request);
            goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getTaskDetail(request);
        goToPage(request,response);
    }

    private void getTaskDetail(HttpServletRequest request){
        request.getSession().setAttribute("checkView",1);
        Long selectedTaskId = null;
        try{
            selectedTaskId = Long.parseLong(request.getParameter("taskSelectButton"));
        } catch (Exception e){
            e.printStackTrace();
            selectedTaskId = -1L;
        }
        Task task = null;
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        if(selectedTaskId > 0L){
            task = dM.getTaskById(em,selectedTaskId);
            request.getSession().setAttribute("hasCurrentTask",true);
            request.getSession().setAttribute("currentTask",task);
        } else{
            task = (Task) request.getSession().getAttribute("currentTask");
        }
        Query q = em.createQuery("SELECT wl FROM WebLink wl INNER JOIN FETCH wl.listOfTasksWithThisWebLink tl WHERE tl.id = :task_id");
        q.setParameter("task_id",task.getId());
        List<WebLink> webLinkList = null;
        try{
            webLinkList = (List<WebLink>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            webLinkList = new ArrayList<>();
        } finally {
            request.getSession().setAttribute("linkList",webLinkList);
            em.close();
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getSession().setAttribute("hasCurrentTaskSequence",false);
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ChecklistManagerGo");
        dispatcher.forward(request,response);
    }
}
