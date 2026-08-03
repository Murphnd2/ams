package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.ActivityLandingDao;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.model.ActivityLandingFilter;
import net.superiorstate.ams.model.ActivityLandingRow;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.User;

import java.io.IOException;
import java.util.List;

/**
 * PSP Dashboard — overview of all open activities across the team.
 * Accessible to PSP Admin and PSP User roles.
 * GET → loads all open activities (unfiltered) + PSP staff list → forwards to dashboard JSP.
 */
@WebServlet(name = "PspDashboardHome", value = "/PspDashboardHome")
public class PspDashboardHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // T124 hardening: the PSP admin dashboard, linked only from the isPspAdmin-gated navbar
        // block (navbar25.jsp:227) and nowhere else. Disclosure rather than escalation — it writes
        // nothing — but it aggregates PSP-wide operational data.
        // Same guard, same shape as AgencyAction.doPost's V067 precedent.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || !local.isAuthenticated()) {
            response.sendRedirect("login");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");

        // --- Load all open activities (no ownership filter, all types) ---
        ActivityLandingFilter f = new ActivityLandingFilter();
        f.ownershipFilter = 0;       // All open
        f.includeRenewal = true;
        f.includeSetup = true;
        f.includeTicket = true;
        f.includeOpportunity = true;
        f.viewNeedsContact = false;
        f.viewWaitingOnUs = false;
        f.sortAlphabetically = false;
        f.pageSize = 500;
        f.offset = 0;

        ActivityLandingDao dao = new ActivityLandingDao(emf);
        List<ActivityLandingRow> allActivities = dao.fetchLandingRows(
                local.getCurrentPerson().getId(),
                7,   // daysSinceWarn threshold
                f
        );

        // --- Load PSP staff list ---
        EntityManager em = emf.createEntityManager();
        List<User> staffList;
        try {
            PSP psp = local.getCurrentPerson().getPsp();
            staffList = AuthDAO.getPspStaff(em, psp);
        } finally {
            em.close();
        }

        request.setAttribute("dashboardActivities", allActivities);
        request.setAttribute("dashboardStaff", staffList);

        request.getRequestDispatcher("/WEB-INF/view/a/general/pspDashboard25.jsp")
                .forward(request, response);
    }
}
