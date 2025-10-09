package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbBilling;
import net.superiorstate.ams.previous.model.billing.BillingMonth;
import net.superiorstate.ams.previous.model.billing.EmployeeVariance;
import net.superiorstate.ams.previous.model.billing.EmployerVariance;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "GoBillingHome", value = "/GoBillingHome")
public class GoBillingHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillBillingData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillBillingData(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/billing/billingHome.jsp");
        dispatcher.forward(request,response);
    }

    private void fillBillingData(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        BillingMonth bm = (BillingMonth) request.getSession().getAttribute("billingMonth");
        int billingView = Integer.parseInt(request.getSession().getAttribute("billingView").toString());
        Employer er = (Employer) request.getSession().getAttribute("currentBillingEmployer");
        Employee ee = (Employee) request.getSession().getAttribute("currentBillingEmployee");
        List<BillingMonth> billingMonthList = dbBilling.getBillingMonths(em);
        request.getSession().setAttribute("billingMonths",billingMonthList);
        boolean changeOnlyBilling;
        try {
            String cob = request.getSession().getAttribute("changeOnlyBilling").toString();
            if(cob.equals("Y"))
                changeOnlyBilling = true;
            else
                changeOnlyBilling = false;
        } catch (Exception e){
            changeOnlyBilling = false;
        }

        switch (billingView){
            case 1:
                List<EmployeeVariance> employeeSummary = dbBilling.getEeMonthlyVariance(em,bm,er);
                request.getSession().setAttribute("employeeSummary",employeeSummary);
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                List<EmployerVariance> employerVarianceList;
                if(changeOnlyBilling) {
                    employerVarianceList = dbBilling.getOnlyChanges(em,bm);
                } else
                    employerVarianceList = dbBilling.getMonthlyVariance(em,bm);
                request.getSession().setAttribute("monthlySummary",employerVarianceList);
                break;

        }
    }
}
