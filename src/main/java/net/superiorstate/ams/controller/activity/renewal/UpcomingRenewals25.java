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
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
        if ("selectEmployer".equals(action)) {
            handleSelectEmployer(request, response);
        } else if ("startRenewal".equals(action)) {
            handleStartRenewal(request, response);
        } else {
            response.sendRedirect("UpcomingRenewals");
        }
    }

    private void handleSelectEmployer(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            int employerId = Integer.parseInt(request.getParameter("employerId"));
            Employer employer = em.find(Employer.class, employerId);
            if (employer == null) {
                response.sendRedirect("UpcomingRenewals");
                return;
            }

            List<Benefit> benefitList = RenewalService.getBenefitsByEmployerSortedForRenewal(em, employer);
            request.getSession().setAttribute("currentEmployer", employer);
            request.getSession().setAttribute("benefitsForRenewalList", benefitList);

            LinkedHashMap<String, List<RenewalEmployer>> renewalsByMonth = buildRenewalsByMonth(em);
            request.setAttribute("renewalsByMonth", renewalsByMonth);
            request.setAttribute("expandedEmployerId", employer.getId());
            request.setAttribute("benefitsForDisplay", benefitList);
            request.setAttribute("pageTitle", "Upcoming Renewals");
            request.setAttribute("pageIcon", "bi-calendar-check");
            request.getRequestDispatcher("/WEB-INF/view/a/renew/upcomingRenewals25.jsp").forward(request, response);
        } finally {
            em.close();
        }
    }

    private void handleStartRenewal(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("AddRenewal25");
        dispatcher.forward(request, response);
    }

    /**
     * Builds a LinkedHashMap grouping RenewalEmployer DTOs by month label.
     * Order: OVERDUE first, then chronological months.
     */
    private LinkedHashMap<String, List<RenewalEmployer>> buildRenewalsByMonth(EntityManager em) {
        List<RenewalEmployer> allRenewals = RenewalQueryDAO.getEmployerRenewals(em);
        if (allRenewals == null || allRenewals.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // Bulk query: get earliest upcoming benefit date per employer
        LocalDate cutoffLd = LocalDate.now().withDayOfMonth(1).plusMonths(3);
        Date cutoff = Date.valueOf(cutoffLd);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT b.employer.id, MIN(b.nextRenewalDue) FROM Benefit b " +
                "WHERE b.isActive = true AND b.nextRenewalDue IS NOT NULL AND b.nextRenewalDue < :cutoff " +
                "GROUP BY b.employer.id")
                .setParameter("cutoff", cutoff)
                .getResultList();

        Map<Integer, LocalDate> earliestByEmployer = new HashMap<>();
        for (Object[] row : rows) {
            Integer empId = (Integer) row[0];
            Date earliest = (Date) row[1];
            if (earliest != null) {
                earliestByEmployer.put(empId, earliest.toLocalDate());
            }
        }

        // Build grouped map: OVERDUE first, then by YearMonth
        LinkedHashMap<String, List<RenewalEmployer>> result = new LinkedHashMap<>();
        TreeMap<YearMonth, List<RenewalEmployer>> monthGroups = new TreeMap<>();

        for (RenewalEmployer re : allRenewals) {
            if (re.getStage() == 0) {
                result.computeIfAbsent("OVERDUE", k -> new ArrayList<>()).add(re);
            } else {
                int empId = re.getEmployer().getId();
                LocalDate earliest = earliestByEmployer.get(empId);
                YearMonth ym;
                if (earliest != null) {
                    ym = YearMonth.from(earliest);
                } else {
                    // Fallback: use current month + stage offset
                    ym = YearMonth.now().plusMonths(re.getStage() - 1);
                }
                monthGroups.computeIfAbsent(ym, k -> new ArrayList<>()).add(re);
            }
        }

        // Append month groups in chronological order
        for (Map.Entry<YearMonth, List<RenewalEmployer>> entry : monthGroups.entrySet()) {
            String label = entry.getKey().format(MONTH_FORMATTER);
            result.put(label, entry.getValue());
        }

        return result;
    }
}
