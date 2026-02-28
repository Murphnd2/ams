package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.BillingQueryDAO;
import net.superiorstate.ams.model.billing.BillingGrid;
import net.superiorstate.ams.model.billing.BillingMonth;
import net.superiorstate.ams.model.billing.EmployeeVariance;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "EmployerBillingDetail", value = "/EmployerBillingDetail")
public class EmployerBillingDetail extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = request.getParameter("uid");
        if (guid == null || guid.equals(""))
            return;
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        BillingMonth bm;
        Employer er;
        try {
            bm = BillingQueryDAO.getBillingMonthByBillingGuid(em, guid);
            er = BillingQueryDAO.getEmployerByBillingGuid(em, guid);
        } catch (Exception e) {
            bm = null;
            er = null;
        } finally {
            em.close();
        }
        if (bm != null && er != null) {
            request.getSession().setAttribute("validEmployer", er);
            request.getSession().setAttribute("erBillingMonth", bm);
            getBillingDetail(request, bm);
            goToPage(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long billingMonthId = Long.parseLong(request.getParameter("selectedMonth"));
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery("SELECT bm FROM BillingMonth bm WHERE bm.monthId = :id");
            q.setParameter("id", billingMonthId);
            BillingMonth bm;
            try {
                bm = (BillingMonth) q.getSingleResult();
                request.getSession().setAttribute("erBillingMonth", bm);
            } catch (NoResultException e) {
                return;
            }
            getBillingDetail(request, bm);
            goToPage(request, response);
        } finally {
            em.close();
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/billing/erBilling25.jsp");
        dispatcher.forward(request, response);
    }

    private void getBillingDetail(HttpServletRequest request, BillingMonth bm) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<BillingMonth> billingMonthList = BillingQueryDAO.getBillingMonths(em);
            request.getSession().setAttribute("erBillingMonthList", billingMonthList);
            Employer er = (Employer) request.getSession().getAttribute("validEmployer");
            List<BillingGrid> billingGridList = BillingQueryDAO.getEmployerMonthlyDetail(em, er, bm);
            List<EmployeeVariance> employeeSummary = BillingQueryDAO.getEeMonthlyVariance(em, bm, er);
            request.getSession().setAttribute("erEmployeeSummary", employeeSummary);
            request.getSession().setAttribute("employerBillingDetail", billingGridList);
            request.setAttribute("billingGridItems", employeeSummary);
            request.setAttribute("billingGridMode", "external");
        } finally {
            em.close();
        }
    }
}
