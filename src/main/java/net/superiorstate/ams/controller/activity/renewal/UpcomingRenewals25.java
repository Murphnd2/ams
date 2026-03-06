package net.superiorstate.ams.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.RenewalQueryDAO;
import net.superiorstate.ams.data.service.RenewalService;
import net.superiorstate.ams.model.activity.renewal.RenewalEmployer;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WebServlet(name = "UpcomingRenewals25", value = "/UpcomingRenewals")
public class UpcomingRenewals25 extends HttpServlet {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || !local.isAuthenticated()) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            LinkedHashMap<String, List<RenewalEmployer>> renewalsByMonth = buildRenewalsByMonth(em);
            request.setAttribute("renewalsByMonth", renewalsByMonth);
            request.setAttribute("pageTitle", "Upcoming Renewals");
            request.setAttribute("pageIcon", "bi-calendar-check");
            request.getRequestDispatcher("/WEB-INF/view/a/renew/upcomingRenewals25.jsp").forward(request, response);
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || !local.isAuthenticated()) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        if ("startRenewal".equals(action)) {
            handleStartRenewal(request, response);
        } else {
            response.sendRedirect("UpcomingRenewals");
        }
    }

    private void handleStartRenewal(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            int employerId = Integer.parseInt(request.getParameter("employerId"));
            Employer employer = em.find(Employer.class, employerId);
            if (employer == null) {
                response.sendRedirect("UpcomingRenewals");
                return;
            }

            // Set session state required by AddRenewal25
            List<Benefit> benefitList = RenewalService.getBenefitsByEmployerSortedForRenewal(em, employer);
            request.getSession().setAttribute("currentEmployer", employer);
            request.getSession().setAttribute("benefitsForRenewalList", benefitList);

            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("AddRenewal25");
            dispatcher.forward(request, response);
        } finally {
            em.close();
        }
    }

    /**
     * Builds a LinkedHashMap grouping RenewalEmployer DTOs (with benefits) by month label.
     * Order: OVERDUE first, then chronological months. Uses 2 DB queries total.
     */
    private LinkedHashMap<String, List<RenewalEmployer>> buildRenewalsByMonth(EntityManager em) {
        List<RenewalEmployer> allRenewals = RenewalQueryDAO.getEmployerRenewalsWithBenefits(em);
        if (allRenewals == null || allRenewals.isEmpty()) {
            return new LinkedHashMap<>();
        }

        LinkedHashMap<String, List<RenewalEmployer>> result = new LinkedHashMap<>();
        TreeMap<YearMonth, List<RenewalEmployer>> monthGroups = new TreeMap<>();

        for (RenewalEmployer re : allRenewals) {
            if (re.getStage() == 0) {
                result.computeIfAbsent("OVERDUE", k -> new ArrayList<>()).add(re);
            } else {
                // Month from earliest benefit (first in the sorted list)
                YearMonth ym;
                if (!re.getBenefits().isEmpty()) {
                    ym = YearMonth.from(re.getBenefits().get(0).getNextRenewalDue().toLocalDate());
                } else {
                    ym = YearMonth.now().plusMonths(re.getStage() - 1);
                }
                monthGroups.computeIfAbsent(ym, k -> new ArrayList<>()).add(re);
            }
        }

        for (Map.Entry<YearMonth, List<RenewalEmployer>> entry : monthGroups.entrySet()) {
            String label = entry.getKey().format(MONTH_FORMATTER);
            result.put(label, entry.getValue());
        }

        return result;
    }
}
