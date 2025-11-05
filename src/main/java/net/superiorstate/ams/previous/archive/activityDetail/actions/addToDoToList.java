package net.superiorstate.ams.previous.archive.activityDetail.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDoOut;

import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;

@WebServlet(name = "addToDoToList", value = "/addToDoToList")
public class addToDoToList extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addToDo(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addToDo(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goActivityDetail");
        dispatcher.forward(request,response);
    }

    private void addToDo(HttpServletRequest request){
        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        if(sVar==null)
            return;
        Activity a = sVar.getCurrentActivity();
        if(a==null)
            return;
        List<ToDoOut> tdl = sVar.getCurrentActivityToDos();
        if(tdl==null)
            return;
        CheckList c = null;
        if(tdl.size()>0)
            c = tdl.get(0).getCheckList();
        else c = sVar.getCurrentCheckList();
        if(c==null)
            return;
        String toDoDescription;
        long positionId;
        try{
            toDoDescription = request.getParameter("toDoName").toString();
            positionId = Long.parseLong(request.getParameter("insertWhere").toString());
        } catch (Exception e){return;}
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        //Create the Task
        em.getTransaction().begin();
        Task task = new Task();
        task.setPsp(a.getLoggedBy().getPsp());
        task.setDescription(toDoDescription);
        task.setReUsable(false);
        task.setAllowEarly(true);
        task.setAllowFuture(true);
        task.setAllowNonOwner(true);
        em.persist(task);
        em.getTransaction().commit();

        //Find the insert location
        int sortOrder=-1;
        OptionalInt maxSortOrder;
        if(positionId == -1){
            maxSortOrder = tdl.stream().mapToInt(ToDoOut::getSortOrder).max();
            if(maxSortOrder.isPresent())
                sortOrder = maxSortOrder.getAsInt() + 1;
        } else if(positionId!=0) {
            ToDo t = dM.getToDoById(em,positionId);
            if(t!=null)
                sortOrder = t.getSortOrder();
        } else{
            maxSortOrder = tdl.stream().mapToInt(ToDoOut::getSortOrder).min();
            if(maxSortOrder.isPresent())
                sortOrder = maxSortOrder.getAsInt() -1;
        }

        //Create the ToDo
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setComplete(false);
        toDo.setTask(task);
        toDo.setCheckList(c);
        toDo.setSortOrder(sortOrder);
        em.persist(toDo);
        em.getTransaction().commit();
        em.refresh(toDo);

        sVar.refreshToDoOutList(em);
        em.close();
        request.getSession().setAttribute("sVar",sVar);
    }
}
