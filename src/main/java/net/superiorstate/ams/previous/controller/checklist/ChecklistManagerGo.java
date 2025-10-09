package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ChecklistManagerGo", value = "/ChecklistManagerGo")
public class ChecklistManagerGo extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillPspTaskList(request);
        //fillPspSequenceList(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillPspTaskList(request);
        //fillPspSequenceList(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/activity/checklist/checklistHome.jsp");
        dispatcher.forward(request,response);
    }

    private void fillPspTaskList(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        Query q = em.createQuery("SELECT t FROM Task t LEFT JOIN FETCH t.webLinkList wll WHERE t.psp.id = :psp_id and t.reUsable=true ORDER BY t.description");
        q.setParameter("psp_id",psp.getId());
        List<Task> taskList = null;
        try {
            taskList = (List<Task>) q.getResultList();
        } catch (NoResultException e) {
            taskList = new ArrayList<>();
        } finally {
            request.getSession().setAttribute("PspTaskList",taskList);
        }
        em.close();
    }

    private void fillPspSequenceList(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        Query q = em.createQuery("SELECT ts FROM TaskSequence ts  WHERE ts.psp.id = :psp_id ORDER BY ts.description");
        q.setParameter("psp_id",psp.getId());
        List<TaskSequence> taskSequenceList = getAppropriateTaskSequences(em,request);
        request.getSession().setAttribute("PspTaskSequenceList",taskSequenceList);
        em.close();
    }

    private List<TaskSequence> getAppropriateTaskSequences(EntityManager em, HttpServletRequest request){

        String fS = request.getSession().getAttribute("fS").toString();
        String fR = request.getSession().getAttribute("fR").toString();
        String fT = request.getSession().getAttribute("fT").toString();
        String fU = request.getSession().getAttribute("fU").toString();
        List<Integer> myIncList = new ArrayList<>();
        if(fS=="checked")
            myIncList.add(2);
        if(fR=="checked")
            myIncList.add(1);
        if(fT== "checked")
            myIncList.add(3);
        if(fU=="checked")
            myIncList.add(4);
        Query q = em.createQuery("SELECT ts FROM TaskSequence ts WHERE ts.templatePurpose.templateGroup.id IN :myList");
        q.setParameter("myList",myIncList);
        List<TaskSequence> taskSequenceList = null;
        try{
            taskSequenceList = (List<TaskSequence>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskSequenceList = new ArrayList<>();
        } finally {
            return taskSequenceList;
        }

    }
}
