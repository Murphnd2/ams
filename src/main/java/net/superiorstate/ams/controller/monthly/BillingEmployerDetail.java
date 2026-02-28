package net.superiorstate.ams.controller.monthly;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;

@WebServlet(name = "BillingEmployerDetail", value = "/BillingEmployerDetail")
public class BillingEmployerDetail extends HttpServlet {
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
            request.getSession().setAttribute("billingView", 1);
            String employerIdString = request.getParameter("btnEmployer");
            String sessionErIdString = "";
            try {
                sessionErIdString = request.getSession().getAttribute("sessionErIdString").toString();
            } catch (Exception e) {
                e.printStackTrace();
            }

            int erId;
            if (employerIdString == null || employerIdString.equals(""))
                erId = Integer.parseInt(sessionErIdString);
            else
                erId = Integer.parseInt(employerIdString);
            Employer employer = EntityLookup.getEmployerById(em, erId);
            request.getSession().setAttribute("currentBillingEmployer", employer);
            request.getSession().setAttribute("currentBillingEmployee", new Employee());
            request.getSession().setAttribute("sessionErIdString", erId);
        } finally {
            em.close();
        }
    }
}
