package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbBilling;
import net.superiorstate.ams.previous.model.billing.BillingMonth;

import java.io.IOException;

@WebServlet(name = "ChangeBillingMonth", value = "/ChangeBillingMonth")
public class ChangeBillingMonth extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoBillingHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("billingView",999);
        String monthIdString = request.getParameter("monthList");
        int monthId = Integer.parseInt(monthIdString);
        BillingMonth billingMonth = dbBilling.getBillingMonthById(em,monthId);
        request.getSession().setAttribute("billingMonth",billingMonth);
        em.close();
    }
}
