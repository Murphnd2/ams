package net.superiorstate.ams.previous.controller.xtra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "FixRenewal", value = "/FixRenewal")
public class FixRenewal extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fixRenewalDate();
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void fixRenewalDate(){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT b FROM Benefit b WHERE b.isActive=true AND b.nextRenewalDue < :date");
        Date dateCheck = Date.valueOf(LocalDate.of(2024,5,1));
        q.setParameter("date",dateCheck);
        List<Benefit> benefitList;
        try{
            benefitList = (List<Benefit>) q.getResultList();
        } catch (NoResultException e){
            return;
        }
        for(Benefit b: benefitList){
            em.getTransaction().begin();
            b.setNextRenewalDue(Date.valueOf(b.getEffectiveDate().toLocalDate().plusYears(1L)));
            em.persist(b);
            em.getTransaction().commit();
        }
        em.close();
    }
}
