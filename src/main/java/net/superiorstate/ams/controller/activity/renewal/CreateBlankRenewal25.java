package net.superiorstate.ams.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.service.RenewalService;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateBlankRenewal25", value = "/CreateBlankRenewal25")
public class CreateBlankRenewal25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToHome(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToHome(request, response);
    }

    private void handleRequest(HttpServletRequest request) {
        int employerId = parseEmployerId(request.getParameter("employerId"));
        if (employerId == 0) return;

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

            Employer employer = EntityLookup.getEmployerById(em, employerId);
            Person currentPerson = local.getCurrentPerson();

            Renewal renewal = RenewalService.createRenewal(em, employer, currentPerson);
            RenewalService.createCheckListForRenewal(em, renewal, currentPerson);
            AddRenewal25.assignPrimaryContact(em, renewal);

            updateSessionAndGlobalState(request, em, local, global, renewal);
        } finally {
            em.close();
        }
    }

    private int parseEmployerId(String idParam) {
        try {
            return Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateSessionAndGlobalState(HttpServletRequest request, EntityManager em,
                                             AmsDataLocal local, AmsDataGlobal global, Renewal renewal) {
        Activity25u newActivity = local.getActivity25u(em, renewal);
        List<Activity25u> allActivities = new ArrayList<>(global.getActivitiesAllOpen());
        allActivities.add(newActivity);

        global.setActivitiesAllOpen(allActivities);
        local.setActivitiesAllOpen(allActivities);

        local.getCurrentActivity().setActivity(renewal);
        local.getCurrentActivity().setReFilterOnExit(true);

        request.getSession().setAttribute("local", local);
        request.getServletContext().setAttribute("global", global);
    }

    private void forwardToHome(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }
}

