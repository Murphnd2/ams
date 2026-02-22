package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.TimeCorrectionRequest;
import net.superiorstate.ams.model.general.TimeLog;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ReviewTimeCorrections", value = "/ReviewTimeCorrections")
public class ReviewTimeCorrections extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadRequests(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processAction(request);
        loadRequests(request);
        goToPage(request, response);
    }

    private void loadRequests(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Determine filter — default to PENDING
            String statusFilter = request.getParameter("statusFilter");
            if (statusFilter == null || statusFilter.isEmpty()) {
                statusFilter = "PENDING";
            }

            List<TimeCorrectionRequest> requests;

            if ("ALL".equals(statusFilter)) {
                Query q = em.createQuery(
                    "SELECT t FROM TimeCorrectionRequest t " +
                    "ORDER BY CASE t.status WHEN 'PENDING' THEN 0 WHEN 'APPROVED' THEN 1 ELSE 2 END, " +
                    "t.dateRequested DESC");
                requests = q.getResultList();
            } else {
                Query q = em.createQuery(
                    "SELECT t FROM TimeCorrectionRequest t WHERE t.status = :status " +
                    "ORDER BY t.dateRequested DESC");
                q.setParameter("status", statusFilter);
                requests = q.getResultList();
            }

            // Force eager load of requestor and reviewer names
            for (TimeCorrectionRequest tcr : requests) {
                if (tcr.getRequestor() != null) tcr.getRequestor().getFirstName();
                if (tcr.getReviewer() != null) tcr.getReviewer().getFirstName();
            }

            request.setAttribute("correctionRequests", requests);
            request.setAttribute("statusFilter", statusFilter);

            // Counts for the filter badges
            Query countPending = em.createQuery(
                "SELECT COUNT(t) FROM TimeCorrectionRequest t WHERE t.status = 'PENDING'");
            request.setAttribute("pendingCount", countPending.getSingleResult());

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("correctionRequests", new ArrayList<>());
        } finally {
            em.close();
        }
    }

    private void processAction(HttpServletRequest request) {
        String action = request.getParameter("action");
        String requestIdStr = request.getParameter("requestId");
        String comment = request.getParameter("reviewComment");

        if (action == null || requestIdStr == null) return;

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Long requestId = Long.parseLong(requestIdStr);
            TimeCorrectionRequest tcr = em.find(TimeCorrectionRequest.class, requestId);
            if (tcr == null || !tcr.isPending()) return;

            em.getTransaction().begin();

            if ("APPROVE".equals(action)) {
                tcr.setStatus("APPROVED");
                tcr.setReviewer(local.getCurrentPerson());
                tcr.setReviewComment(comment != null ? comment.trim() : null);
                tcr.setDateReviewed(new Timestamp(System.currentTimeMillis()));

                // Apply the corrections to the actual TimeLog records
                if (tcr.isInTimeChanged() && tcr.getInLog() != null) {
                    TimeLog inLog = em.find(TimeLog.class, tcr.getInLog().getId());
                    if (inLog != null) {
                        inLog.setPunchTime(tcr.getRequestedInTime());
                        em.persist(inLog);
                    }
                }
                if (tcr.isOutTimeChanged() && tcr.getOutLog() != null) {
                    TimeLog outLog = em.find(TimeLog.class, tcr.getOutLog().getId());
                    if (outLog != null) {
                        outLog.setPunchTime(tcr.getRequestedOutTime());
                        em.persist(outLog);
                    }
                }

            } else if ("DENY".equals(action)) {
                tcr.setStatus("DENIED");
                tcr.setReviewer(local.getCurrentPerson());
                tcr.setReviewComment(comment != null ? comment.trim() : null);
                tcr.setDateReviewed(new Timestamp(System.currentTimeMillis()));
            }

            em.persist(tcr);
            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher =
            request.getRequestDispatcher("/WEB-INF/view/a/pspHome/columns/timeClock/reviewTimeCorrections.jsp");
        dispatcher.forward(request, response);
    }
}
