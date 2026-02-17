package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.Checklist25u;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "ChangeOwner25", value = "/ChangeOwner25")
public class ChangeOwner25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeOwnership(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeOwnership(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void changeOwnership(HttpServletRequest request){
        String userIdString = request.getParameter("userList");
        Long userId = Long.parseLong(userIdString);

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        Person newOwner = EntityLookup.getPersonById(em,userId);
        if(newOwner == null){
            em.close();
            return;
        }
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");

        Activity a = EntityLookup.getActivityById(em,local.getCurrentActivity().getActivity().getId());
        if(a==null) {
            em.close();
            return;
        }
        em.getTransaction().begin();
        a.setAssignedTo(newOwner);
        em.persist(a);
        em.getTransaction().commit();
        em.refresh(a);

        if(a.getClass().getSimpleName().equals("CheckList")){
            CheckList c = (CheckList) a;
            Checklist25u cu = local.retrieveChecklist25uFromList(local.getChecklistsAll(),c.getId());
            local.getChecklistsAll().remove(cu);
            local.splitChecklists();
        } else {
            global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
            local.setActivitiesWithDependencies(global.getActivitiesWithDelegation());
            Optional<Activity25u> matchingActivity = global.getActivitiesAllOpen().stream().filter(au->au.getActivity().getId().equals(a.getId())).findFirst();
            if(matchingActivity.isPresent()){
                Activity25u au = matchingActivity.get();
                au.setAssignedTo(newOwner);
                au.getActivity().setAssignedTo(newOwner);
                for(int i = 0; i < global.getActivitiesAllOpen().size(); i++){
                    if(global.getActivitiesAllOpen().get(i).getActivity().getId().equals(au.getActivity().getId())){
                        global.getActivitiesAllOpen().set(i,au);
                        local.setActivitiesAllOpen(global.getActivitiesAllOpen());
                        break;
                    }
                }
            }
            local.getCurrentActivity().setReFilterOnExit(true);
            local.setNextView("activityDetail");
        }

        local.respondToActivityUpdate(em,"OWNER",newOwner);

        request.getSession().setAttribute("local",local);
        request.getServletContext().setAttribute("global",global);

        em.close();
    }
}

