package net.superiorstate.ams.controller.activity.questionnaire;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import net.superiorstate.ams.data.service.QuestionnaireService;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireField;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireFieldValue;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireInstance;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;

/**
 * AJAX auto-save for native questionnaire progress.
 * Pattern follows SaveApplicationProgress.
 */
@WebServlet(name = "SaveQuestionnaireProgress", value = "/saveQuestionnaire")
@MultipartConfig
public class SaveQuestionnaireProgress extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String guid = request.getParameter("guid");
        if (guid == null || guid.isEmpty()) {
            response.setStatus(400);
            out.print("{\"error\":\"Missing GUID\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            QuestionnaireInstance qi = QuestionnaireService.getInstanceByGuid(em, guid);
            if (qi == null) {
                response.setStatus(404);
                out.print("{\"error\":\"Questionnaire not found\"}");
                return;
            }

            // Guard: already submitted
            if ("SUBMITTED".equals(qi.getStatus()) || "REVIEWED".equals(qi.getStatus())) {
                response.setStatus(409);
                out.print("{\"error\":\"Already submitted\"}");
                return;
            }

            // Load non-suppressed fields via direct query
            List<QuestionnaireField> fields = QuestionnaireService.getFieldsForQuestionnaire(
                    em, qi.getQuestionnaire().getId());

            // Debug: log what we received
            System.out.println("[SaveQ] Fields count: " + fields.size());
            System.out.println("[SaveQ] All params: " + java.util.Collections.list(request.getParameterNames()));
            int savedCount = 0;

            // Save values (single transaction)
            em.getTransaction().begin();
            for (QuestionnaireField field : fields) {
                String paramValue = request.getParameter(field.getFieldKey());

                if ("CHECKBOX".equals(field.getFieldType())) {
                    String[] values = request.getParameterValues(field.getFieldKey());
                    paramValue = (values != null) ? String.join("|", values) : null;
                }

                // Find existing value
                Query existQ = em.createQuery(
                        "SELECT v FROM QuestionnaireFieldValue v " +
                                "WHERE v.instance = :instance AND v.field = :field");
                existQ.setParameter("instance", qi);
                existQ.setParameter("field", field);
                List<QuestionnaireFieldValue> existing = existQ.getResultList();

                if (paramValue != null && !paramValue.trim().isEmpty()) {
                    if (!existing.isEmpty()) {
                        existing.get(0).setFieldValue(paramValue.trim());
                        em.merge(existing.get(0));
                    } else {
                        QuestionnaireFieldValue fv = new QuestionnaireFieldValue();
                        fv.setInstance(qi);
                        fv.setField(field);
                        fv.setFieldValue(paramValue.trim());
                        em.persist(fv);
                    }
                    savedCount++;
                } else if (!existing.isEmpty()) {
                    // Clear previously saved value if now empty
                    em.remove(existing.get(0));
                }
            }

            // Save submitter info if provided (captured during auto-save)
            String submitterName = request.getParameter("_submitter_name");
            String submitterEmail = request.getParameter("_submitter_email");
            if (submitterName != null && !submitterName.isBlank()) qi.setSubmittedByName(submitterName.trim());
            if (submitterEmail != null && !submitterEmail.isBlank()) qi.setSubmittedByEmail(submitterEmail.trim());

            // Transition status
            if ("NOT_STARTED".equals(qi.getStatus()) || "REOPENED".equals(qi.getStatus())) {
                qi.setStatus("IN_PROGRESS");
            }
            em.persist(qi);
            em.getTransaction().commit();

            System.out.println("[SaveQ] Saved " + savedCount + " field values");
            out.print("{\"status\":\"saved\",\"timestamp\":\"" + Instant.now().toString() + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            response.setStatus(500);
            out.print("{\"error\":\"Save failed\"}");
        } finally {
            em.close();
        }
    }
}
