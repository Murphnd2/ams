package net.superiorstate.ams.previous.archive.activityDetail.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.ActivityOut;
import net.superiorstate.ams.previous.model.activity.checklist.CheckListShell;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@WebServlet(name = "closeActivity", value = "/closeActivity")
public class closeActivity extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processActivityClosure(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processActivityClosure(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome.jsp");
        dispatcher.forward(request,response);
    }

    private void processActivityClosure(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        SessionVar sVar;
        try{
            sVar = (SessionVar) request.getSession().getAttribute("sVar");
        } catch (Exception e){
            return;
        }

        Activity a = dM.getActivityById(em, sVar.getCurrentActivity().getId());
        if(a==null)
            return;

        //Close Activity in Database
        em.getTransaction().begin();
        a.setComplete(true);
        a.setDateCompleted(Date.valueOf(LocalDate.now()));
        a.setCompletedBy(sVar.getCurrentPerson());
        em.persist(a);
        em.getTransaction().commit();

        if(a.getClass().getSimpleName().equals("CheckList")){
            List<CheckListShell> masterOpenChecklist = sVar.getMyCurrentCheckLists();
            List<CheckListShell> newList = masterOpenChecklist.stream().filter(cs -> !Objects.equals(cs.getId(),a.getId())).toList();
            List<CheckListShell> mutableList = new ArrayList<>(newList);
            sVar.setMyCurrentCheckLists(mutableList);
            sVar.refreshOpenChecklists(em);
            sVar.refreshClosedCheckLists(em);
        } else {
            //Remove Activity from Session Listing
            List<ActivityOut> masterOpenList = sVar.getOpenActivityList();
            List<ActivityOut> newList = masterOpenList.stream().filter(ao -> !Objects.equals(ao.getActivity().getId(), a.getId())).toList();
            List<ActivityOut> mutableList = new ArrayList<>(newList);
            sVar.setOpenActivityList(mutableList);
            sVar.reFilterActivityList();
        }

        //Remove any "current" flags
        sVar.setCurrentActivity(null);
        sVar.setCurrentCheckList(null);
        sVar.setCurrentOpenToDos(null);
        sVar.setCurrentClosedToDos(null);
        sVar.setCurrentActivityNotes(null);

        request.getSession().setAttribute("sVar",sVar);

        em.close();
    }
}
