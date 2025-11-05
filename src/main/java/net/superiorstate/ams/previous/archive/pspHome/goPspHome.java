package net.superiorstate.ams.previous.archive.pspHome;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.model.activity.ActivityOut;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.TimeLog;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "goPspHome", value = "/goPspHome")
public class goPspHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setSessionVariables(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setSessionVariables(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome.jsp");
        dispatcher.forward(request,response);
    }

    private void setSessionVariables(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        SessionVar sVar = new SessionVar();
        if(request.getSession().getAttribute("sVar")!=null)
            sVar = (SessionVar) request.getSession().getAttribute("sVar");
        else {
            sVar = initializeData(em,request);
            request.getSession().setAttribute("sVar",sVar);
            return;
        }

        if(sVar.isNeedsActivityRefresh()){
            Query q = em.createQuery("SELECT a FROM ActivityOut a");
            List<ActivityOut> activityOutList = new ArrayList<>();
            try{
                activityOutList = (List<ActivityOut>) q.getResultList();
            } catch (Exception ignored){}
            if(activityOutList.size()>0){
                for(ActivityOut ao: activityOutList)
                    if(ao.getActivity().getId().equals(sVar.getActivityOutNeedingRefresh().getActivity().getId())) {
                        em.refresh(ao);
                        break;
                    }
            }
            sVar.setOpenActivityList(activityOutList);
            sVar.refreshFilteredActivityList(em);
            sVar.setActivityOutNeedingRefresh(null);
            sVar.setNeedsActivityRefresh(false);
        }
        String sender;
        try{
            sender = request.getParameter("formSender");
        }catch (Exception e){
            sender = "";
        }
        if(sender!= null){
            if(sender.equals("filterButton")){
                setFilterItems(request,sVar);
                sVar.reFilterActivityList();

            } else if(sender.equals("timeClock")){
                setTimeClockItems(em,sVar);
                refreshTime(em,sVar);
            }
        }
        sVar.setCurrentActivity(null);
        request.getSession().setAttribute("sVar",sVar);
        em.close();
    }

    private void setTimeClockItems(EntityManager em, SessionVar sVar){
        TimeLog lastPunch;
        Query q = em.createQuery("SELECT t FROM TimeLog t WHERE t.person.id = :id AND t.id = (SELECT MAX (tl.id) FROM TimeLog tl WHERE tl.person.id = :id)");
        q.setParameter("id",sVar.getCurrentPerson().getId());
        try{
            lastPunch = (TimeLog) q.getSingleResult();
        } catch (Exception e){
            lastPunch = new TimeLog();
            lastPunch.setId(0L);
        }
        TimeLog thisPunch = new TimeLog();
        thisPunch.setPunchTime(Time.valueOf(LocalTime.now()));
        thisPunch.setPunchDate(Date.valueOf(LocalDate.now()));
        thisPunch.setPerson(sVar.getCurrentPerson());
        boolean isPunchedIn = !lastPunch.getId().equals(0L) && lastPunch.isIn();
        thisPunch.setIn(!isPunchedIn);
        em.getTransaction().begin();
        em.persist(thisPunch);
        em.getTransaction().commit();
        sVar.setUserIsIn(isPunchedIn);
    }

    private void setFilterItems(HttpServletRequest request, SessionVar sVar){
        String fOnUs = request.getParameter("fOnUs");
        sVar.setFilterWaitingOnUs(fOnUs != null);

        String fCall = request.getParameter("fCall");
        sVar.setFilterNeedsContact(fCall != null);

        String fAlpha = request.getParameter("fAlpha");
        sVar.setFilterAlphabetical(fAlpha != null && fAlpha.equals("1"));

        String whose = request.getParameter("whoFilter");
        int whoFilter;
        try {
            whoFilter = Integer.parseInt(whose);
        } catch (Exception e){
            whoFilter = 1;
        }
        sVar.setFilterWhoseActivities(whoFilter);

        String rn = request.getParameter("vRenew");
        sVar.setFilterRenewal(true);
        if(rn==null)
            sVar.setFilterRenewal(false);

        String st = request.getParameter("vSetup");
        sVar.setFilterSetup(true);
        if(st==null)
            sVar.setFilterSetup(false);

        String tk = request.getParameter("vTicket");
        sVar.setFilterTicket(true);
        if(tk==null)
            sVar.setFilterTicket(false);
    }

    private SessionVar initializeData(EntityManager em, HttpServletRequest request){
        SessionVar sVar = new SessionVar();
        //Master Lists
        setUserInformation(request, em, sVar);
        sVar.refreshStaffList(em);
        sVar.refreshTicketReasonList(em);
        sVar.refreshPspEmployerList(em);


        refreshTime(em, sVar);
        refreshActivityListing(em, sVar, true, true, true, true, true, false, 1);
        sVar.refreshOpenChecklists(em);
        sVar.refreshClosedCheckLists(em);
        sVar.setNeedsActivityRefresh(false);
        return sVar;
    }

    private void setUserInformation(HttpServletRequest request, EntityManager em, SessionVar sVar){
        User user = (User) request.getSession().getAttribute("currentUser");
        Person person = (Person) request.getSession().getAttribute("currentPerson");
        sVar.setCurrentUser(user);
        sVar.setCurrentPerson(person);
    }

    private void refreshTime(EntityManager em, SessionVar sVar){
        sVar.refreshUserIsIn(em);
        sVar.refreshMyTimeHistory(em);
    }

    private void refreshActivityListing(EntityManager em, SessionVar sVar, boolean R, boolean S, boolean T, boolean onUs, boolean needsContact, boolean alphabetize, int whose){
        sVar.refreshOpenActivityList(em);
        reFilterActivityListing(em, sVar, R, S, T, onUs, needsContact, alphabetize, whose);
    }

    private void reFilterActivityListing(EntityManager em, SessionVar sVar, boolean R, boolean S, boolean T, boolean onUs, boolean needsContact, boolean alphabetize, int whose){
        sVar.setFilterRenewal(R);
        sVar.setFilterSetup(S);
        sVar.setFilterTicket(T);
        sVar.setFilterWaitingOnUs(onUs);
        sVar.setFilterNeedsContact(needsContact);
        sVar.setFilterAlphabetical(alphabetize);
        sVar.setFilterWhoseActivities(whose);
        sVar.reFilterActivityList();
    }

}
