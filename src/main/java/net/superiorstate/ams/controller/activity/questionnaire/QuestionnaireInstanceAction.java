package net.superiorstate.ams.controller.activity.questionnaire;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.activity.questionnaire.Questionnaire;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;
import net.superiorstate.ams.model.general.Assignee;
import net.superiorstate.ams.model.general.LinkType;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;
import net.superiorstate.ams.data.resolver.EntityLookup;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * PSP-side actions on questionnaire instances from the activity detail page.
 * Actions: review, reopen, detach, markComplete, attach, emailQuestionnaire.
 *
 * After action, refreshes questionnaire instances in session and forwards
 * back to ViewActivity25 (or CreateEmail25 for emailQuestionnaire).
 */
@WebServlet(name = "QuestionnaireInstanceAction", value = "/QuestionnaireInstanceAction")
public class QuestionnaireInstanceAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if (action == null) {
            forwardToView(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentPerson = local.getCurrentPerson();
            long activityId = local.getCurrentActivity().getActivity().getId();
            long pspId = currentPerson.getPsp().getId();

            if ("emailQuestionnaire".equals(action)) {
                // Pre-populate email compose with questionnaire link attachment
                String instanceIdStr = request.getParameter("instanceId");
                if (instanceIdStr != null) {
                    long instanceId = Long.parseLong(instanceIdStr);
                    QuestionnaireInstance qi = em.find(QuestionnaireInstance.class, instanceId);
                    if (qi != null) {
                        Questionnaire q = qi.getQuestionnaire();
                        String activityName = local.getCurrentActivity().getActivity().getFullName();

                        // Resolve the questionnaire URL
                        String url;
                        if (qi.isExternal()) {
                            url = q.resolveExternalUrl(activityName, activityId, qi.getInstanceGuid());
                        } else {
                            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
                            url = global.getWebPath() + "/q/" + qi.getInstanceGuid();
                        }

                        // Create in-memory WebLink (linkType=2 = external URL)
                        LinkType externalLinkType = EntityLookup.getLinkTypeById(em, 2);
                        WebLink attachment = new WebLink();
                        attachment.setPlainText(q.getName());
                        attachment.setLinkPath(url);
                        attachment.setLinkType(externalLinkType);
                        attachment.setActive(true);

                        // Set subject and add attachment — keep existing recipients
                        local.getCurrentEmail().setSubject("Questionnaire: " + q.getName());
                        local.getCurrentEmail().getAttachments().clear();
                        local.getCurrentEmail().getAttachments().add(attachment);
                        request.getSession().setAttribute("local", local);
                    }
                }
                em.close();
                RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
                dispatcher.forward(request, response);
                return;
            }

            if ("attach".equals(action)) {
                // Manual attach: create new instance for selected questionnaire
                String qIdStr = request.getParameter("questionnaireId");
                if (qIdStr != null) {
                    long questionnaireId = Long.parseLong(qIdStr);
                    Questionnaire q = em.find(Questionnaire.class, questionnaireId);
                    if (q != null) {
                        Assignee activity = em.find(Assignee.class, activityId);
                        QuestionnaireInstance instance = new QuestionnaireInstance();
                        instance.setQuestionnaire(q);
                        instance.setActivity(activity);
                        em.getTransaction().begin();
                        em.persist(instance);
                        em.getTransaction().commit();
                    }
                }
            } else {
                // Instance-based actions: review, reopen, detach, markComplete
                String instanceIdStr = request.getParameter("instanceId");
                if (instanceIdStr == null) {
                    forwardToView(request, response);
                    return;
                }
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
                        qi.setStatus(qi.isExternal() ? "NOT_STARTED" : "IN_PROGRESS");
                        qi.setDateReopened(Timestamp.from(Instant.now()));
                        qi.setReopenedBy(currentPerson);
                        em.persist(qi);
                        em.getTransaction().commit();
                        break;

                    case "detach":
                        if ("NOT_STARTED".equals(qi.getStatus())) {
                            em.getTransaction().begin();
                            em.remove(qi);
                            em.getTransaction().commit();
                        }
                        break;

                    case "markComplete":
                        if (qi.isExternal()) {
                            em.getTransaction().begin();
                            qi.setStatus("SUBMITTED");
                            qi.setDateSubmitted(Timestamp.from(Instant.now()));
                            String submitterName = request.getParameter("submitterName");
                            String submitterEmail = request.getParameter("submitterEmail");
                            if (submitterName != null && !submitterName.isBlank()) qi.setSubmittedByName(submitterName.trim());
                            if (submitterEmail != null && !submitterEmail.isBlank()) qi.setSubmittedByEmail(submitterEmail.trim());
                            em.persist(qi);
                            em.getTransaction().commit();
                        }
                        break;
                }
            }

            // Refresh questionnaire instances + available list in session
            local.getCurrentActivity().setQuestionnaireInstances(
                    QuestionnaireService.getInstancesForActivity(em, activityId));
            local.getCurrentActivity().setAvailableQuestionnaires(
                    QuestionnaireService.getAvailableQuestionnaires(em, pspId, activityId));
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
