package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.OriginatingAgencyResolver;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@WebServlet(name = "ViewActivity25", value = "/ViewActivity25")
public class ViewActivity25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    private void processData(HttpServletRequest request) {
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global != null && global.isDelegationDirty()) {
            EntityManager em = getOpenEntityManager(request);
            try {
                global.setActivitiesWithDelegation(global.retrieveActivitiesWithDependencies(em));
                global.clearDelegationDirty();
            } finally {
                em.close();
            }
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Compute daysUntilDue for breadcrumb + status bar urgency coloring
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local != null && local.getCurrentActivity() != null
                && local.getCurrentActivity().getActivity() != null
                && local.getCurrentActivity().getActivity().getDueDate() != null) {
            LocalDate due = local.getCurrentActivity().getActivity().getDueDate().toLocalDate();
            long days = ChronoUnit.DAYS.between(LocalDate.now(), due);
            request.setAttribute("daysUntilDue", (int) days);
        }

        // V061: surface the originating agent in the Setup header (Option C — presentational only)
        if (local != null && local.getCurrentActivity() != null
                && local.getCurrentActivity().getActivity() instanceof Setup setup) {
            Person originAgent = OriginatingAgencyResolver.resolveAgent(setup);
            if (originAgent != null) {
                request.setAttribute("originatingAgent", originAgent);
                Agency originAgency = OriginatingAgencyResolver.resolve(setup);
                if (originAgency != null) request.setAttribute("originatingAgency", originAgency);
            }
        }

        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/activityDetail/activityDetail25.jsp");
        dispatcher.forward(request,response);
    }
    private EntityManager getOpenEntityManager(HttpServletRequest request) {
        EntityManagerFactory emf =
                (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}
