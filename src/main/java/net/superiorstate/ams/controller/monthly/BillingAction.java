package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.BillingQueryDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.billing.BillingMonth;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;

@WebServlet(name = "BillingAction", value = "/BillingAction")
public class BillingAction extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processAction(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processAction(request, response);
    }

    private void processAction(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.isBlank()) action = "reset";

        switch (action) {
            case "changeMonth" -> doChangeMonth(request);
            case "drillDown"   -> doDrillDown(request);
            case "back"        -> doBack(request);
            case "changesOnly" -> doChangesOnly(request);
            default            -> doReset(request);
        }

        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoBillingHome");
        dispatcher.forward(request, response);
    }

    private void doReset(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            BillingMonth bm = BillingQueryDAO.getCurrentBillingMonth(em);
            if (bm != null)
                request.getSession().setAttribute("billingMonth", bm);
            else {
                var months = BillingQueryDAO.getBillingMonths(em);
                if (!months.isEmpty())
                    request.getSession().setAttribute("billingMonth", months.get(0));
            }
            request.getSession().setAttribute("changeOnlyBilling", "N");
            request.getSession().setAttribute("billingView", 999);
            request.getSession().setAttribute("currentBillingEmployer", new Employer());
            request.getSession().setAttribute("currentBillingEmployee", new Employee());
        } finally {
            em.close();
        }
    }

    private void doChangeMonth(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            request.getSession().setAttribute("billingView", 999);
            String monthIdString = request.getParameter("monthList");
            int monthId = Integer.parseInt(monthIdString);
            BillingMonth billingMonth = BillingQueryDAO.getBillingMonthById(em, monthId);
            request.getSession().setAttribute("billingMonth", billingMonth);
        } finally {
            em.close();
        }
    }

    private void doDrillDown(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            request.getSession().setAttribute("billingView", 1);
            String employerIdString = request.getParameter("employerId");
            String sessionErIdString = "";
            try {
                Object sessionVal = request.getSession().getAttribute("sessionErIdString");
                if (sessionVal != null) sessionErIdString = sessionVal.toString();
            } catch (Exception ignored) {}

            int erId;
            if (employerIdString == null || employerIdString.isBlank())
                erId = Integer.parseInt(sessionErIdString);
            else
                erId = Integer.parseInt(employerIdString);

            Employer employer = EntityLookup.getEmployerById(em, erId);
            request.getSession().setAttribute("currentBillingEmployer", employer);
            request.getSession().setAttribute("currentBillingEmployee", new Employee());
            request.getSession().setAttribute("sessionErIdString", String.valueOf(erId));
        } finally {
            em.close();
        }
    }

    private void doBack(HttpServletRequest request) {
        request.getSession().setAttribute("billingView", 999);
        request.getSession().setAttribute("currentBillingEmployer", new Employer());
        request.getSession().setAttribute("currentBillingEmployee", new Employee());
    }

    private void doChangesOnly(HttpServletRequest request) {
        request.getSession().setAttribute("billingView", 999);
        request.getSession().setAttribute("changeOnlyBilling", "Y");
        request.getSession().setAttribute("currentBillingEmployer", new Employer());
        request.getSession().setAttribute("currentBillingEmployee", new Employee());
    }
}
