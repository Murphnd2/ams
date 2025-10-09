package net.superiorstate.ams.previous.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.misc.dbS;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.sales.application.Application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddSetupModule", value = "/AddSetupModule")
public class AddSetupModule extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request);
        goAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request);
        goAdminHomePage(request,response);

    }
    private void goAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RefreshCurrentActivity");
        dispatcher.forward(request,response);

    }
    private void doThis(HttpServletRequest request){
        Activity a;
        Setup s;
        Application app;
        int tpId;
        try{
            a = (Activity) request.getSession().getAttribute("currentActivity");

            tpId = Integer.parseInt(request.getParameter("remainingModList"));
        } catch (Exception e){
            return;
        }
        s = (Setup) a;
        app = s.getApplication();
        if(app==null)
            return;

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        TemplatePurpose tp = dM.getTemplatePurposeById(em,tpId);

        dActivity.addModule(em,app,tp);
        List<SortedTask> sortedTaskList;
        try{
            sortedTaskList = dbS.getTasksRequiredForApplication(em,app);
        } catch (Exception e){
            return;
        }
        if(sortedTaskList.size()==0)
            return;

        CheckList checkList = s.getCheckList();
        CheckList c = dM.getCheckListById(em,checkList.getId());
        assert c != null;
        List<ToDo> toDoList = c.getToDoList();
        List<Task> taskList = new ArrayList<>();
        for(ToDo t:toDoList)
            taskList.add(t.getTask());
        for(SortedTask st:sortedTaskList){
            if(taskList.contains(st.getTask()))
               continue;
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(false);
            if(st.getTask().getId()==153L)
                toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            c.getToDoList().add(toDo);
            em.persist(c);
            em.getTransaction().commit();
        }
        em.close();
    }
}
