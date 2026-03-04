package net.superiorstate.ams.controller.activity.questionnaire;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * PSP-side actions on questionnaire instances from the activity detail page.
 * Actions: review, reopen, detach, markComplete.
 *
 * After action, refreshes questionnaire instances in session and forwards
 * back to ViewActivity25.
 */
@WebServlet(name = "QuestionnaireInstanceAction", value = "/QuestionnaireInstanceAction")
public class QuestionnaireInstanceAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String instanceIdStr = request.getParameter("instanceId");

        if (action == null || instanceIdStr == null) {
            forwardToView(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentPerson = local.getCurrentPerson();
            long instanceId = Long.parseLong(instanceIdStr);

            QuestionnaireInstance qi = em.find(QuestionnaireInstance.class, instanceId);
            if (qi == null) {
                forwardToView(request, response);
                return;
            }

            switch (action) {
                case "review":
                    em.getTransaction().begin();
                    qi.setStatus("REVIEWED");
                    qi.setDateReviewed(Timestamp.from(Instant.now()));
                    qi.setReviewedBy(currentPerson);
                    em.persist(qi);
                    em.getTransaction().commit();
                    break;

                case "reopen":
                    em.getTransaction().begin();
                    // Native → IN_PROGRESS, External → NOT_STARTED
                    qi.setStatus(qi.isExternal() ? "NOT_STARTED" : "IN_PROGRESS");
                    qi.setDateReopened(Timestamp.from(Instant.now()));
                    qi.setReopenedBy(currentPerson);
                    em.persist(qi);
                    em.getTransaction().commit();
                    break;

                case "detach":
                    // Only allow detach if NOT_STARTED (no data saved)
                    if ("NOT_STARTED".equals(qi.getStatus())) {
                        em.getTransaction().begin();
                        em.remove(qi);
                        em.getTransaction().commit();
                    }
                    break;

                case "markComplete":
                    // External mode only: mark as submitted
                    if (qi.isExternal()) {
                        em.getTransaction().begin();
                        qi.setStatus("SUBMITTED");
                        qi.setDateSubmitted(Timestamp.from(Instant.now()));
                        // Optionally capture submitter info from form
                        String submitterName = request.getParameter("submitterName");
                        String submitterEmail = request.getParameter("submitterEmail");
                        if (submitterName != null && !submitterName.isBlank()) qi.setSubmittedByName(submitterName.trim());
                        if (submitterEmail != null && !submitterEmail.isBlank()) qi.setSubmittedByEmail(submitterEmail.trim());
                        em.persist(qi);
                        em.getTransaction().commit();
                    }
                    break;
            }

            // Refresh questionnaire instances in session
            long activityId = local.getCurrentActivity().getActivity().getId();
            local.getCurrentActivity().setQuestionnaireInstances(
                    QuestionnaireService.getInstancesForActivity(em, activityId));
            request.getSession().setAttribute("local", local);

        } catch (Exception e) {
            System.out.println("[QuestionnaireInstanceAction] Error: " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }

        forwardToView(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        forwardToView(request, response);
    }

    private void forwardToView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }
}
