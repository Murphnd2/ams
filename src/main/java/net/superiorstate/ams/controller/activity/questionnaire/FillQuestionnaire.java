package net.superiorstate.ams.controller.activity.questionnaire;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireField;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireFieldValue;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;
import net.superiorstate.ams.model.general.Assignee;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Public-facing questionnaire form servlet.
 * Accessed via /q/{instance_guid} — no login required.
 *
 * GET: renders native form (or redirects external questionnaires).
 * POST: submits the native questionnaire.
 *
 * Pattern follows ApplyForProposal (/apply/{guid}).
 */
@WebServlet(name = "FillQuestionnaire", value = "/q/*")
public class FillQuestionnaire extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = extractGuid(request, response);
        if (guid == null) return;

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            QuestionnaireInstance qi = QuestionnaireService.getInstanceByGuid(em, guid);
            if (qi == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // External mode: redirect to resolved external URL
            if (qi.isExternal()) {
                Assignee activity = qi.getActivity();
                String erName = activity.getFullName();
                Long activityId = (long) activity.getId();
                String resolvedUrl = qi.getQuestionnaire().resolveExternalUrl(erName, activityId, qi.getInstanceGuid());
                if (resolvedUrl != null) {
                    response.sendRedirect(resolvedUrl);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }

            // Native mode: render form
            List<QuestionnaireField> fields = qi.getQuestionnaire().getFieldList();
            if (fields == null) {
                fields = List.of();
            }
            // Filter suppressed fields
            fields = fields.stream().filter(f -> !f.isSuppressed()).toList();

            // Load saved values
            Map<String, String> defaults = QuestionnaireService.getFieldValueMap(em, qi);

            // Pre-fill submitter info if previously saved (auto-save may have captured it)
            if (qi.getSubmittedByName() != null) defaults.putIfAbsent("_submitter_name", qi.getSubmittedByName());
            if (qi.getSubmittedByEmail() != null) defaults.putIfAbsent("_submitter_email", qi.getSubmittedByEmail());

            // Determine read-only state
            boolean readOnly = "SUBMITTED".equals(qi.getStatus()) || "REVIEWED".equals(qi.getStatus());

            // PSP branding
            String primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
            String accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
            if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
            if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";

            // PSP name
            String pspName = "";
            if (qi.getQuestionnaire().getPsp() != null) {
                pspName = qi.getQuestionnaire().getPsp().getFullName();
            }

            // Activity name
            String activityName = qi.getActivity().getFullName();

            request.setAttribute("instance", qi);
            request.setAttribute("questionnaire", qi.getQuestionnaire());
            request.setAttribute("fields", fields);
            request.setAttribute("defaults", defaults);
            request.setAttribute("readOnly", readOnly);
            request.setAttribute("primaryColor", primaryColor);
            request.setAttribute("accentColor", accentColor);
            request.setAttribute("pspName", pspName);
            request.setAttribute("activityName", activityName);

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/questionnaire/fillQuestionnaire.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = extractGuid(request, response);
        if (guid == null) return;

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            QuestionnaireInstance qi = QuestionnaireService.getInstanceByGuid(em, guid);
            if (qi == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Guard: already submitted
            if ("SUBMITTED".equals(qi.getStatus()) || "REVIEWED".equals(qi.getStatus())) {
                setupConfirmationAttributes(em, qi, request);
                request.setAttribute("alreadySubmitted", true);
                RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/questionnaire/questionnaireConfirmation.jsp");
                dispatcher.forward(request, response);
                return;
            }

            // Load non-suppressed fields
            List<QuestionnaireField> fields = qi.getQuestionnaire().getFieldList();
            if (fields == null) fields = List.of();
            fields = fields.stream().filter(f -> !f.isSuppressed()).toList();

            // Save field values (single transaction)
            em.getTransaction().begin();
            for (QuestionnaireField field : fields) {
                String paramValue = request.getParameter(field.getFieldKey());

                // Handle CHECKBOX — multiple values
                if ("CHECKBOX".equals(field.getFieldType())) {
                    String[] values = request.getParameterValues(field.getFieldKey());
                    paramValue = (values != null) ? String.join("|", values) : null;
                }

                // Upsert field value
                if (paramValue != null && !paramValue.trim().isEmpty()) {
                    Query existQ = em.createQuery(
                            "SELECT v FROM QuestionnaireFieldValue v " +
                                    "WHERE v.instance = :instance AND v.field = :field");
                    existQ.setParameter("instance", qi);
                    existQ.setParameter("field", field);
                    List<QuestionnaireFieldValue> existing = existQ.getResultList();

                    if (!existing.isEmpty()) {
                        existing.get(0).setFieldValue(paramValue.trim());
                        em.persist(existing.get(0));
                    } else {
                        QuestionnaireFieldValue fv = new QuestionnaireFieldValue();
                        fv.setInstance(qi);
                        fv.setField(field);
                        fv.setFieldValue(paramValue.trim());
                        em.persist(fv);
                    }
                }
            }

            // Capture submitter info
            String submitterName = request.getParameter("_submitter_name");
            String submitterEmail = request.getParameter("_submitter_email");
            if (submitterName != null && !submitterName.isBlank()) qi.setSubmittedByName(submitterName.trim());
            if (submitterEmail != null && !submitterEmail.isBlank()) qi.setSubmittedByEmail(submitterEmail.trim());

            // Update status
            qi.setStatus("SUBMITTED");
            qi.setDateSubmitted(Timestamp.from(Instant.now()));
            em.persist(qi);
            em.getTransaction().commit();

            // Set up confirmation page
            setupConfirmationAttributes(em, qi, request);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[FillQuestionnaire] POST error: " + e.getMessage());
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/questionnaire/questionnaireConfirmation.jsp");
        dispatcher.forward(request, response);
    }

    private String extractGuid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return pathInfo.substring(1);
    }

    private void setupConfirmationAttributes(EntityManager em, QuestionnaireInstance qi, HttpServletRequest request) {
        String primaryColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_PRIMARY");
        String accentColor = AppConstantDAO.getConstantValue(em, "EMAIL_COLOR_ACCENT");
        if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#2B5F8A";
        if (accentColor == null || accentColor.isEmpty()) accentColor = "#7AB648";

        String pspName = "";
        if (qi.getQuestionnaire().getPsp() != null) {
            pspName = qi.getQuestionnaire().getPsp().getFullName();
        }

        request.setAttribute("primaryColor", primaryColor);
        request.setAttribute("accentColor", accentColor);
        request.setAttribute("pspName", pspName);
        request.setAttribute("questionnaireName", qi.getQuestionnaire().getName());
    }
}
