package net.superiorstate.ams.previous.archive.activityDetail.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDoOut;

import java.io.IOException;

@WebServlet(name = "reOpenToDoOut", value = "/reOpenToDoOut")
public class reOpenToDoOut extends HttpServlet {

    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processToDoClosure(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processToDoClosure(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/activityDetail/activityDetail.jsp");
        if(getActivity().getClass().getSimpleName().equals("CheckList"))
            dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/checklistDetail/checklistDetail.jsp");
        dispatcher.forward(request,response);
    }

    private void processToDoClosure(HttpServletRequest request){
        String toDoIdString;
        try{
            toDoIdString = request.getParameter("btnToDo").toString();
        } catch (Exception e){
            return;
        }
        long toDoId = Long.parseLong(toDoIdString);
        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        // Close the ToDo
        ToDo toDo = dM.getToDoById(em,toDoId);
        if(toDo==null){
            em.close();
            return;
        }
        em.getTransaction().begin();
        toDo.setComplete(false);
        toDo.setDateCompleted(null);
        toDo.setCompletedBy(null);
        em.persist(toDo);
        em.getTransaction().commit();
        em.refresh(toDo);

        try{
            Query q = em.createQuery("SELECT t FROM ToDoOut t WHERE t.toDo.id = :id");
            q.setParameter("id",toDoId);
            ToDoOut toDoOut = (ToDoOut) q.getSingleResult();
            em.refresh(toDoOut);
        } catch (Exception ignored){}


        sVar.refreshToDoOutList(em);
        setActivity(sVar.getCurrentActivity());
        em.close();

        request.getSession().setAttribute("sVar",sVar);
    }
}
