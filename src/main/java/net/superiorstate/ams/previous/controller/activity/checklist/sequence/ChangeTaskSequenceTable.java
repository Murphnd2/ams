package net.superiorstate.ams.previous.controller.activity.checklist.sequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbReq;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;

import java.io.IOException;

@WebServlet(name = "ChangeTaskSequenceTable", value = "/ChangeTaskSequenceTable")
public class ChangeTaskSequenceTable extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        determineAndTakeAction(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        determineAndTakeAction(request, response);
    }
    private void goToPage1(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RequiredSequenceBuilder");
        dispatcher.forward(request,response);
    }

    private void goToPage2(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RecurringSequenceBuilder");
        dispatcher.forward(request,response);
    }
    private void determineAndTakeAction(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        TaskSequence ts;
        int viewId = (Integer) request.getSession().getAttribute("sequenceView");
        RequiredTaskList ts1 = (RequiredTaskList) request.getSession().getAttribute("currentReqList");
        RecurringTaskList ts2 = (RecurringTaskList) request.getSession().getAttribute("currentRecList");
        if(viewId ==1)
            ts = ts1;
        else if(viewId==2)
            ts = ts2;
        else
            ts = ts1;
        String buttonValue = request.getParameter("btnTaskChange");
        String btnFunction = buttonValue.substring(0,2);
        Long taskId = Long.parseLong(buttonValue.substring(3));
        switch (btnFunction){
            case "UP":
                dbReq.moveTaskUp(em,ts, dM.getTaskById(em,taskId));
                break;
            case "DN":
                dbReq.moveTaskDown(em,ts, dM.getTaskById(em,taskId));
                break;
            case "RM":
                dbReq.removeTaskFromSequence(em,dbReq.getTaskSequenceTableByIds(em,ts, dM.getTaskById(em,taskId)));
                break;
        }
        if(viewId==1)
            goToPage1(request,response);
        if(viewId==2)
            goToPage2(request,response);
    }
}
