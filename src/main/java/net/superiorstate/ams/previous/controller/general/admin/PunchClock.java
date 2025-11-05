package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.TimeLog;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

@WebServlet(name = "PunchClock", value = "/PunchClock")
public class PunchClock extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        punchTheClock(request);
        goToPage(request,response);
    }

    private void punchTheClock(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person user = (Person) request.getSession().getAttribute("currentPerson");
        TimeLog lastPunch = getLastPunch(em,user);
        TimeLog thisPunch = new TimeLog();
        thisPunch.setPunchDate(Date.valueOf(LocalDate.now()));
        thisPunch.setPunchTime(Time.valueOf(LocalTime.now()));
        thisPunch.setPerson(user);
        boolean isPunchedIn = !lastPunch.getId().equals(0L) && lastPunch.isIn();
        thisPunch.setIn(!isPunchedIn);
        em.getTransaction().begin();
        em.persist(thisPunch);
        em.getTransaction().commit();
        request.getSession().setAttribute("userStatus", !isPunchedIn);
        em.close();
    }

    private TimeLog getLastPunch(EntityManager em, Person user){
        TimeLog lastPunch;
        Query q = em.createQuery("SELECT t FROM TimeLog t WHERE t.person.id = :id AND t.id = (SELECT MAX (tl.id) FROM TimeLog tl WHERE tl.person.id = :id)");
        q.setParameter("id",user.getId());
        try{
            lastPunch = (TimeLog) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            lastPunch = new TimeLog();
            lastPunch.setId(0L);
        }
        return lastPunch;
    }


    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
        Renewal renewal = new Renewal();
    }

}
