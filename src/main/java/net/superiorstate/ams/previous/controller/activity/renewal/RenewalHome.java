package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbTime;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.TimeStretch;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "RenewalHome", value = "/RenewalHome")
public class RenewalHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillRenewalsToDo(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillRenewalsToDo(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/activity/renew/renewHome.jsp");
        dispatcher.forward(request,response);
    }

    private void fillRenewalsToDo(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        setEmployerRenewalList(request,em);
        setActiveRenewalView(request,em);
        setMyRenewalList(request,em);
        setMyTimeHistoryForToday(request,em);
        em.close();
    }
    private void setEmployerRenewalList(HttpServletRequest request, EntityManager em){
        List<Employer> employerRenewalList = dR.getEmployersNeedingRenewal(em);
        request.getSession().setAttribute("employerRenewList",employerRenewalList);
    }

    private void setActiveRenewalView(HttpServletRequest request, EntityManager em){
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        List<Renewal> renewalList = new ArrayList<>();
        int renewalListView = (Integer) request.getSession().getAttribute("renewalListView");
        switch (renewalListView){
            case 1: renewalList = dR.getOpenRenewals(em); // GET ALL OPEN RENEWALS
                break;
            case 2: renewalList = dR.getClosedRenewals(em); // GET ALL CLOSED RENEWALS
                break;
            default: renewalList = dR.getMyRenewals(em,user); // GET MY RENEWALS
                break;
        }
        request.getSession().setAttribute("renewalList",renewalList);
    }

    private void setMyRenewalList(HttpServletRequest request, EntityManager em){
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        List<Renewal> renewalList;
        Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.isComplete = false AND r.assignedTo.id = :id");
        q.setParameter("id", user.getId());
        try{
            renewalList = (List<Renewal>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            renewalList = new ArrayList<>();
        }
        request.getSession().setAttribute("myRenewalList",renewalList);
    }

    private void setMyTimeHistoryForToday(HttpServletRequest request, EntityManager em){
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        List<TimeStretch> myStretches = dbTime.getTodaysTimeHistory(em,user);
        request.getSession().setAttribute("myTimeList",myStretches);
    }


}
