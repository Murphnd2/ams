package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "EmployerRenewalDetailView", value = "/EmployerRenewalDetailView")
public class EmployerRenewalDetailView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewRenewalDetail(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewRenewalDetail(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/renew/upcomingRenewalGenerator.jsp");
        dispatcher.forward(request,response);
    }

    private void viewRenewalDetail(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        populateTheToRenewDetailForSelection(request,em);
        em.close();
    }

    private void populateTheToRenewDetailForSelection(HttpServletRequest request, EntityManager em){
        int employerId = Integer.parseInt(request.getParameter("employerRenewalSelectButton"));
        Employer employer = dM.getEmployerById(em,employerId);
        List<Benefit> benefitList = dR.getBenefitsByEmployerSortedForRenewal(em,employer);
        List<Renewal> pastRenewalsList = dR.getPastRenewalsForEmployer(em,employer);
        request.getSession().setAttribute("currentEmployer",employer);
        request.getSession().setAttribute("currentRenewal", new Renewal());
        request.getSession().setAttribute("currentSetup", new Setup());
        request.getSession().setAttribute("currentTicket", new Ticket());
        request.getSession().setAttribute("benefitsForRenewalList",benefitList);
        request.getSession().setAttribute("pastRenewalList",pastRenewalsList);
        request.getSession().setAttribute("adminView",5);
    }
}
