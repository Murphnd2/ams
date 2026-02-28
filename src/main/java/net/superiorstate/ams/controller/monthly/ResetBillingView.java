package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.BillingQueryDAO;
import net.superiorstate.ams.model.billing.BillingMonth;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;

@WebServlet(name = "ResetBillingView", value = "/ResetBillingView")
public class ResetBillingView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoBillingHome");
        dispatcher.forward(request, response);
    }

    private void changeView(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            BillingMonth bm = BillingQueryDAO.getCurrentBillingMonth(em);
            if (bm != null)
                request.getSession().setAttribute("billingMonth", bm);
            else
                request.getSession().setAttribute("billingMonth", BillingQueryDAO.getBillingMonths(em).get(0));

            request.getSession().setAttribute("changeOnlyBilling", "N");
            request.getSession().setAttribute("billingView", 999);
            request.getSession().setAttribute("currentBillingEmployer", new Employer());
            request.getSession().setAttribute("currentBillingEmployee", new Employee());
        } finally {
            em.close();
        }
    }
}
