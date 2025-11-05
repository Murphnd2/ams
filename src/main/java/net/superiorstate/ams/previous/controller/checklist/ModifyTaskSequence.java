package net.superiorstate.ams.previous.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;

import java.io.IOException;

@WebServlet(name = "ModifyTaskSequence", value = "/ModifyTaskSequence")
public class ModifyTaskSequence extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        onClickDo(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        onClickDo(request);
        goToPage(request,response);
    }

    private void onClickDo(HttpServletRequest request){
        String buttonIdString = request.getParameter("modifyTaskSequenceButton");

        int indexOfFirst = buttonIdString.indexOf("-");
        int indexOfSecond = buttonIdString.indexOf("-",indexOfFirst+1);
        int stringLength = buttonIdString.length();
        String sequenceIdString = buttonIdString.substring(0,indexOfFirst);
        String taskIdString = buttonIdString.substring(indexOfFirst+1,indexOfSecond);
        String whatToDoString = buttonIdString.substring(indexOfSecond+1,stringLength);
        Long sequenceId = Long.parseLong(sequenceIdString);
        Long taskId = Long.parseLong(taskIdString);
        int whatToDo = Integer.parseInt(whatToDoString);

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        if(whatToDo==1){
            em.getTransaction().begin();
            TaskSequenceTable taskSequenceTable = ddC.getTaskSequenceTableById(em,sequenceId,taskId);
            em.remove(taskSequenceTable);
            em.getTransaction().commit();
        } else if(whatToDo==0){
            Integer newSortOrder = Integer.parseInt(request.getParameter("sortOrder"+sequenceIdString+"-"+taskIdString));
            em.getTransaction().begin();
            TaskSequenceTable taskSequenceTable = ddC.getTaskSequenceTableById(em,sequenceId,taskId);
            taskSequenceTable.setSortOrder(newSortOrder);
            em.persist(taskSequenceTable);
            em.getTransaction().commit();
        }
        em.close();
    }


    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("SequenceDetailView");
        dispatcher.forward(request,response);
    }
}
