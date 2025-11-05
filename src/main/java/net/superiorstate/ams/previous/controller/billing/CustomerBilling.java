package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbBilling;
import net.superiorstate.ams.previous.model.billing.BillingGrid;
import net.superiorstate.ams.previous.model.billing.BillingItem;
import net.superiorstate.ams.previous.model.billing.BillingMonth;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CustomerBilling", value = "/CustomerBilling")
public class CustomerBilling extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response, EntityManager em) throws ServletException, IOException {
        em.close();
        RequestDispatcher dispatcher= request.getRequestDispatcher("/index.jsp");
        dispatcher.forward(request,response);
    }

    private void goToBillingPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/billingHome.jsp");
        dispatcher.forward(request,response);
    }

    private void doThisFirst(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        String billingGuid = request.getParameter("id");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Employer bEmployer = dbBilling.getEmployerByBillingGuid(em,billingGuid);

        if(bEmployer==null)
            goToPage(request,response,em);

        BillingMonth bMonth = dbBilling.getBillingMonthByBillingGuid(em,billingGuid);
        assert bEmployer != null;
        assert bMonth != null;
        List<BillingGrid> billingGridList = dbBilling.getBillingGridForMonth(em,bEmployer,bMonth);
        List<BillingItem> billingItemList = dbBilling.getBillingItemForMonth(em,bEmployer,bMonth);
        request.getSession().setAttribute("billingEmployer",bEmployer);
        request.getSession().setAttribute("billingMonth",bMonth);
        request.getSession().setAttribute("billingGrid",billingGridList);
        request.getSession().setAttribute("billingItems",billingItemList);
        em.close();
        goToBillingPage(request,response);
    }
}
