package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AddToDoToChecklist", value = "/AddToDoToChecklist")
public class AddToDoToChecklist extends HttpServlet {
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
        User user = (User) request.getSession().getAttribute("currentUser");
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        String toDoName = request.getParameter("toDoName");
        CheckList checkList = (CheckList) request.getSession().getAttribute("currentChecklist");
        List<ToDo> toDoList = checkList.getToDoList();
        int maxSort = 0;
        for(ToDo t: toDoList){
            if(t.getSortOrder()>maxSort)
                maxSort = t.getSortOrder();
        }
        em.getTransaction().begin();
        Task task = new Task();
        task.setPsp(psp);
        task.setDescription(toDoName);
        task.setReUsable(false);
        task.setAllowEarly(true);
        task.setAllowFuture(true);
        task.setAllowNonOwner(true);
        em.persist(task);
        em.getTransaction().commit();
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setComplete(false);
        toDo.setTask(task);
        toDo.setCheckList(checkList);
        toDo.setSortOrder(maxSort + 1);
        em.persist(toDo);
        em.getTransaction().commit();
        List<ToDo> checkListToDoList = ViewSelectedChecklist.getToDoListByChecklistId(em, checkList.getId());
        request.getSession().setAttribute("currentToDoList",checkListToDoList);
        em.close();
    }

}
