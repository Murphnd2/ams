package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbReq;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RequiredSequenceBuilder", value = "/RequiredSequenceBuilder")
public class RequiredSequenceBuilder extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        populateRequiredScreen(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        populateRequiredScreen(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SequenceHome");
        dispatcher.forward(request,response);
    }

    private void populateRequiredScreen(HttpServletRequest request){
        System.out.println(2);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        RequiredTaskList ts = (RequiredTaskList) request.getSession().getAttribute("currentReqList");
        List<Task> availableTasks = dbReq.getTasksNotInSequence(em,ts);
        request.getSession().setAttribute("availableTasks",availableTasks);
        request.getSession().setAttribute("embeddedTaskSequences",dbReq.getTasksForSequence(em,ts));
        em.close();
    }
}
