package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedChecklist;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RefreshToDoAutomation", value = "/RefreshToDoAutomation")
public class RefreshToDoAutomation extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        refreshToDoList(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        refreshToDoList(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void refreshToDoList(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        refreshToDoDetail(request,em);
        em.close();
    }

    private void refreshToDoDetail(HttpServletRequest request, EntityManager em){
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        CheckList c = (CheckList) request.getSession().getAttribute("currentChecklist");
        List<ToDo> toDoList = ViewSelectedChecklist.getToDoListByChecklistId(em,c.getId());
        request.getSession().setAttribute("currentToDoList",toDoList);
    }




}
