package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.TimeTrackingDAO;
import net.superiorstate.ams.model.general.TimeLog;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

@WebServlet(name = "TimeClock25", value = "/TimeClock25")
public class TimeClock25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setTimeClockItems(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setTimeClockItems(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }

    private void setTimeClockItems(HttpServletRequest request){
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        TimeLog lastPunch;
        Query q = em.createQuery("SELECT t FROM TimeLog t WHERE t.person.id = :id AND t.id = (SELECT MAX (tl.id) FROM TimeLog tl WHERE tl.person.id = :id)");
        q.setParameter("id",local.getCurrentPerson().getId());
        try{
            lastPunch = (TimeLog) q.getSingleResult();
        } catch (Exception e){
            lastPunch = new TimeLog();
            lastPunch.setId(0L);
        }
        TimeLog thisPunch = new TimeLog();
        thisPunch.setPunchTime(Time.valueOf(LocalTime.now()));
        thisPunch.setPunchDate(Date.valueOf(LocalDate.now()));
        thisPunch.setPerson(local.getCurrentPerson());
        boolean isPunchedIn = !lastPunch.getId().equals(0L) && lastPunch.isIn();
        thisPunch.setIn(!isPunchedIn);
        em.getTransaction().begin();
        em.persist(thisPunch);
        em.getTransaction().commit();
        local.setUserIsIn(isPunchedIn);
        refreshTime(em,local);
        em.close();
        request.getSession().setAttribute("local",local);
    }

    private void refreshTime(EntityManager em, AmsDataLocal local){
        local.setUserIsIn(TimeTrackingDAO.getMyLastPunch(em,local.getCurrentUser()).isIn());
        local.setMyTimeHistory(TimeTrackingDAO.getTodaysTimeHistory(em,local.getCurrentPerson()));
    }

}
