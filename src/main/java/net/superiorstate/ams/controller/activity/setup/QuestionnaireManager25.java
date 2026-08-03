package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.questionnaire.Questionnaire;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Questionnaire Template Manager — standalone admin page for managing questionnaire templates.
 *
 * GET  /QuestionnaireManager25         → loads page with all questionnaires, none selected
 * GET  /QuestionnaireManager25?qId=123 → loads page with questionnaire 123 selected
 */
@WebServlet(name = "QuestionnaireManager25", value = "/QuestionnaireManager25")
public class QuestionnaireManager25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // T124 hardening: the admin Questionnaire Manager, linked only from the isPspAdmin-gated
        // navbar block. ⚠️ Distinct from QuestionnaireInstanceAction, which S9-H deliberately did
        // NOT guard — that one is posted from the activity-detail panel and is legitimately used by
        // agents and PSP users, so an isPspAdmin guard there would be an outage.
        // Same guard, same shape as AgencyAction.doPost's V067 precedent.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        long pspId = local.getCurrentPerson().getPsp().getId();

        try {
            // Load all questionnaires for this PSP (including suppressed, for toggle)
            List<Questionnaire> questionnaireList = getQuestionnaireList(em, pspId);
            request.setAttribute("questionnaireList", questionnaireList);

            // Counts for filter tabs
            long nativeCount = questionnaireList.stream().filter(q -> !q.isExternal()).count();
            long externalCount = questionnaireList.stream().filter(Questionnaire::isExternal).count();
            request.setAttribute("totalCount", questionnaireList.size());
            request.setAttribute("nativeCount", nativeCount);
            request.setAttribute("externalCount", externalCount);

            // If a questionnaire is selected
            String qIdParam = request.getParameter("qId");
            if (qIdParam != null && !qIdParam.isEmpty()) {
                long qId = Long.parseLong(qIdParam);
                Questionnaire selected = getQuestionnaireWithAssociations(em, qId);
                if (selected != null) {
                    request.setAttribute("selectedQuestionnaire", selected);

                    // Load all active ServiceItems (with category) for scoping checkboxes
                    request.setAttribute("allServiceItems", getActiveServiceItems(em, pspId));
                }
            }

        } finally {
            em.close();
        }

        request.setAttribute("adminCurrentPage", "questionnaireManager");
        request.setAttribute("pageTitle", "Questionnaire Manager");
        request.setAttribute("pageIcon", "bi-ui-checks-grid");
        RequestDispatcher dispatcher = request.getRequestDispatcher(
                "/WEB-INF/view/a/general/questionnaireManager25.jsp");
        dispatcher.forward(request, response);
    }

    // ── Query Helpers ───────────────────────────────────────────────────────────

    private List<Questionnaire> getQuestionnaireList(EntityManager em, long pspId) {
        try {
            return em.createQuery(
                    "SELECT q FROM Questionnaire q WHERE q.psp.id = :pspId ORDER BY q.sortOrder",
                    Questionnaire.class)
                    .setParameter("pspId", pspId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private Questionnaire getQuestionnaireWithAssociations(EntityManager em, long qId) {
        try {
            // Use em.find then force-load each collection individually
            // (JOIN FETCH on multiple collections causes Cartesian product issues)
            Questionnaire q = em.find(Questionnaire.class, qId);
            if (q == null) return null;

            // Force-load fields
            List<QuestionnaireField> fields = em.createQuery(
                    "SELECT f FROM QuestionnaireField f WHERE f.questionnaire.id = :qId ORDER BY f.sortOrder",
                    QuestionnaireField.class)
                    .setParameter("qId", qId)
                    .getResultList();
            q.setFieldList(fields);

            // Force-load ServiceItem scoping collection
            q.getServiceItemList().size();

            return q;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ServiceItem> getActiveServiceItems(EntityManager em, long pspId) {
        try {
            return em.createQuery(
                    "SELECT si FROM ServiceItem si LEFT JOIN FETCH si.activityCategory " +
                            "WHERE si.psp.id = :pspId AND si.suppressed = false " +
                            "ORDER BY si.activityCategory.id, si.description",
                    ServiceItem.class)
                    .setParameter("pspId", (int) pspId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }
}
