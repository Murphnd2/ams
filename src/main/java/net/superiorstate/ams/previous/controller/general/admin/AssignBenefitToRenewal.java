package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;

import java.io.IOException;

@WebServlet(name = "AssignBenefitToRenewal", value = "/AssignBenefitToRenewal")
public class AssignBenefitToRenewal extends HttpServlet {
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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Renewal r = (Renewal) request.getSession().getAttribute("currentActivity");
        int benefitId = Integer.parseInt(request.getParameter("addBenefitList"));
        Benefit benefitToAdd = dM.getBenefitById(em,benefitId);
        dR.addBenefitToRenewal(request,em,benefitToAdd,r);
        ViewSelectedActivity.setActivityView(request,em,r);
        em.close();
    }
}
