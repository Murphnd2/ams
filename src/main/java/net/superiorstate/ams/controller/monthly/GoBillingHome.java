package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.BillingQueryDAO;
import net.superiorstate.ams.model.billing.BillingMonth;
import net.superiorstate.ams.model.billing.EmployeeVariance;
import net.superiorstate.ams.model.billing.EmployerVariance;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "GoBillingHome", value = "/GoBillingHome")
public class GoBillingHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillBillingData(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillBillingData(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/billing/billingHome25.jsp");
        dispatcher.forward(request, response);
    }

    private void fillBillingData(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            BillingMonth bm = (BillingMonth) request.getSession().getAttribute("billingMonth");
            Object bvObj = request.getSession().getAttribute("billingView");
            int billingView = (bvObj != null) ? Integer.parseInt(bvObj.toString()) : 0;
            Employer er = (Employer) request.getSession().getAttribute("currentBillingEmployer");
            Employee ee = (Employee) request.getSession().getAttribute("currentBillingEmployee");
            List<BillingMonth> billingMonthList = BillingQueryDAO.getBillingMonths(em);
            request.getSession().setAttribute("billingMonths", billingMonthList);
            boolean changeOnlyBilling;
            try {
                String cob = request.getSession().getAttribute("changeOnlyBilling").toString();
                changeOnlyBilling = cob.equals("Y");
            } catch (Exception e) {
                changeOnlyBilling = false;
            }

            switch (billingView) {
                case 1:
                    List<EmployeeVariance> employeeSummary = BillingQueryDAO.getEeMonthlyVariance(em, bm, er);
                    request.getSession().setAttribute("employeeSummary", employeeSummary);
                    request.setAttribute("billingGridItems", employeeSummary);
                    request.setAttribute("billingGridMode", "employee");
                    break;
                case 2:
                    break;
                case 3:
                    break;
                default:
                    List<EmployerVariance> employerVarianceList;
                    if (changeOnlyBilling) {
                        employerVarianceList = BillingQueryDAO.getOnlyChanges(em, bm);
                    } else
                        employerVarianceList = BillingQueryDAO.getMonthlyVariance(em, bm);
                    request.getSession().setAttribute("monthlySummary", employerVarianceList);
                    request.setAttribute("billingGridItems", employerVarianceList);
                    request.setAttribute("billingGridMode", "employer");
                    break;
            }
        } finally {
            em.close();
        }
    }
}
