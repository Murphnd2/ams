package net.superiorstate.ams.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.service.RenewalService;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.summit.archive.Benefit;

import java.io.IOException;

@WebServlet(name = "AssignBenefitToRenewal25", value = "/AssignBenefitToRenewal25")
public class AssignBenefitToRenewal25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        assignBenefit(request);
        forwardToHome(request, response);
    }

    private void assignBenefit(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || local.getCurrentActivity() == null || local.getCurrentActivity().getActivity() == null) {
            return;
        }

        Renewal renewal = (Renewal) local.getCurrentActivity().getActivity();
        int benefitId;

        try {
            benefitId = Integer.parseInt(request.getParameter("addBenefitList"));
        } catch (NumberFormatException e) {
            return; // Invalid or missing benefitId
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Benefit benefitToAdd = EntityLookup.getBenefitById(em, benefitId);
            RenewalService.addBenefitToRenewal(request, em, benefitToAdd, renewal);
            request.getSession().setAttribute("vp","1");
            request.getSession().setAttribute("pastActivityId",local.getCurrentActivity().getActivity().getId().toString());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private void forwardToHome(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoActivityDetail25");
        dispatcher.forward(request, response);
    }
}


