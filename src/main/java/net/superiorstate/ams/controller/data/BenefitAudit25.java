package net.superiorstate.ams.controller.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.model.summit.archive.Benefit;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Benefit Renewal Audit — lists all benefits with plan year data,
 * flags year-to-year end date changes (short plan years),
 * and allows inline editing of nextRenewalDue and renewalMonths.
 */
@WebServlet(name = "BenefitAudit25", value = "/BenefitAudit")
public class BenefitAudit25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            List<Benefit> benefits = em.createQuery(
                    "SELECT b FROM Benefit b WHERE b.isActive = true ORDER BY b.employer.employerName, b.planName",
                    Benefit.class).getResultList();
            request.setAttribute("benefits", benefits);

            // Filter mode
            String filter = request.getParameter("filter");
            request.setAttribute("filter", filter != null ? filter : "all");

        } finally {
            em.close();
        }

        request.getRequestDispatcher("/WEB-INF/view/a/general/benefitAudit25.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            if ("save".equals(action)) {
                // Single benefit update
                int benefitId = Integer.parseInt(request.getParameter("benefitId"));
                String nextRenewalStr = request.getParameter("nextRenewalDue");
                int renewalMonths = Integer.parseInt(request.getParameter("renewalMonths"));

                em.getTransaction().begin();
                Benefit b = em.find(Benefit.class, benefitId);
                if (b != null) {
                    LocalDate parsed = LocalDate.parse(nextRenewalStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                    b.setNextRenewalDue(Date.valueOf(parsed));
                    b.setRenewalMonths(renewalMonths);
                    em.merge(b);
                }
                em.getTransaction().commit();

            } else if ("acceptAll".equals(action)) {
                // Bulk: accept detected renewal dates for all benefits with plan year data
                List<Benefit> benefits = em.createQuery(
                        "SELECT b FROM Benefit b WHERE b.isActive = true AND b.planYearEnd IS NOT NULL",
                        Benefit.class).getResultList();

                em.getTransaction().begin();
                for (Benefit b : benefits) {
                    LocalDate detectedRenewal = b.getDetectedRenewalDate();
                    if (detectedRenewal != null) {
                        int renewalMonths = b.getRenewalMonths();
                        LocalDate nextDue = detectedRenewal;
                        while (nextDue.isBefore(LocalDate.now())) {
                            nextDue = nextDue.plusMonths(renewalMonths);
                        }
                        b.setNextRenewalDue(Date.valueOf(nextDue));
                        em.merge(b);
                    }
                }
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            request.setAttribute("error", "Save failed: " + e.getMessage());
        } finally {
            em.close();
        }

        response.sendRedirect("BenefitAudit" + (request.getParameter("filter") != null
                ? "?filter=" + request.getParameter("filter") : ""));
    }

    private boolean isAdmin(HttpServletRequest request) {
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return isPspAdmin != null && isPspAdmin;
    }
}
