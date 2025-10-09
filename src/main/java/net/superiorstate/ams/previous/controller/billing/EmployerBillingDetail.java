package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbBilling;
import net.superiorstate.ams.previous.data.summit.bill;
import net.superiorstate.ams.previous.model.billing.BillingGrid;
import net.superiorstate.ams.previous.model.billing.BillingMonth;
import net.superiorstate.ams.previous.model.billing.EmployeeVariance;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "EmployerBillingDetail", value = "/EmployerBillingDetail")
public class EmployerBillingDetail extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = request.getParameter("uid");
        if(guid==null || guid.equals(""))
            return;
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        BillingMonth bm;
        Employer er;
        try{
            bm = bill.getBillingMonthByGuid(em,guid);
            er = bill.getEmployerByGuid(em,guid);
        } catch (Exception e){
            bm = null;
            er = null;
        }
        em.close();
        if(bm!=null && er!=null){
            request.getSession().setAttribute("validEmployer", er);
            request.getSession().setAttribute("erBillingMonth",bm);
            getBillingDetail(request,bm);
            goToPage(request,response);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long billingMonthId = Long.parseLong(request.getParameter("selectedMonth"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.monthId = :id");
        q.setParameter("id",billingMonthId);
        BillingMonth bm;
        try{
            bm = (BillingMonth) q.getSingleResult();
            request.getSession().setAttribute("erBillingMonth",bm);
        } catch (NoResultException e){
            return;
        }
        getBillingDetail(request,bm);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/erBilling.jsp");
        dispatcher.forward(request,response);
    }
    private void getBillingDetail(HttpServletRequest request, BillingMonth bm){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        List<BillingMonth> billingMonthList = bill.getAllBillingMonths(em);
        request.getSession().setAttribute("erBillingMonthList",billingMonthList);
        Employer er = (Employer) request.getSession().getAttribute("validEmployer");
        List<BillingGrid> billingGridList = bill.getEmployerMonthlyBilling(em,er,bm);
        List<EmployeeVariance> employeeSummary = dbBilling.getEeMonthlyVariance(em,bm,er);
        request.getSession().setAttribute("erEmployeeSummary",employeeSummary);
        request.getSession().setAttribute("employerBillingDetail", billingGridList);
        em.close();


    }
}
