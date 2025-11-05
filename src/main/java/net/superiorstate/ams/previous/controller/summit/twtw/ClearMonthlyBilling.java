package net.superiorstate.ams.previous.controller.summit.twtw;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.Helper;
import net.superiorstate.ams.previous.model.billing.BillingMonth;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "ClearMonthlyBilling", value = "/ClearMonthlyBilling")
public class ClearMonthlyBilling extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        clearMonthlyBilling();
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        clearMonthlyBilling();
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }
    private void clearMonthlyBilling(){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        clearBillingGrid(em);
        clearCoverageStatus(em);
        em.close();
    }

    private void clearCoverageStatus(EntityManager em){
        Query q = em.createQuery("DELETE FROM CoverageStatus cs WHERE cs.monthFor = :mf");
        q.setParameter("mf", Helper.getMonthFor());
        em.getTransaction().begin();
        q.executeUpdate();
        em.getTransaction().commit();
    }

    private void clearBillingGrid(EntityManager em){
        Query q = em.createQuery("DELETE FROM BillingGrid bg WHERE bg.billingMonth.monthId = :mId");
        int monthId = getMonthId(em);
        if(monthId==-1)
            return;
        q.setParameter("mId",monthId);
        em.getTransaction().begin();
        q.executeUpdate();
        em.getTransaction().commit();
    }

    private int getMonthId(EntityManager em){
        Date monthFor = Helper.getMonthFor();
        Query q = em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.fullDate = :fullDate");
        q.setParameter("fullDate",monthFor);
        BillingMonth bm;
        try{
            bm = (BillingMonth) q.getSingleResult();
        } catch (NoResultException e){
            return -1;
        }
        return bm.getMonthId();
    }

}
