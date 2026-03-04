package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.questionnaire.Questionnaire;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireField;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles CRUD actions for the Questionnaire Manager.
 * Always redirects back to QuestionnaireManager25 after completion.
 */
@WebServlet(name = "QuestionnaireAction25", value = "/QuestionnaireAction25")
public class QuestionnaireAction25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String action = request.getParameter("action");
        String qIdParam = request.getParameter("qId");

        try {
            switch (action) {

                // ── Questionnaire CRUD ──────────────────────────────────────

                case "createQuestionnaire" -> {
                    Questionnaire q = new Questionnaire();
                    q.setName(request.getParameter("name").trim());
                    q.setDescription(trimOrNull(request.getParameter("description")));
                    q.setActivityType(request.getParameter("activityType"));
                    q.setExternalUrl(trimOrNull(request.getParameter("externalUrl")));
                    q.setSortOrder(9999);
                    q.setSuppressed(false);
                    q.setPsp(psp);

                    em.getTransaction().begin();
                    em.persist(q);
                    em.getTransaction().commit();

                    qIdParam = q.getId().toString();
                }

                case "editQuestionnaire" -> {
                    long qId = Long.parseLong(qIdParam);
                    Questionnaire q = em.find(Questionnaire.class, qId);
                    if (q != null && q.getPsp().getId().equals(psp.getId())) {
                        em.getTransaction().begin();
                        q.setName(request.getParameter("name").trim());
                        q.setDescription(trimOrNull(request.getParameter("description")));
                        q.setActivityType(request.getParameter("activityType"));
                        q.setExternalUrl(trimOrNull(request.getParameter("externalUrl")));
                        em.merge(q);
                        em.getTransaction().commit();
                    }
                }

                case "suppressQuestionnaire" -> {
                    long qId = Long.parseLong(qIdParam);
                    Questionnaire q = em.find(Questionnaire.class, qId);
                    if (q != null && q.getPsp().getId().equals(psp.getId())) {
                        em.getTransaction().begin();
                        q.setSuppressed(!q.isSuppressed());
                        em.merge(q);
                        em.getTransaction().commit();
                    }
                }

                // ── Field CRUD (native mode only) ───────────────────────────

                case "createField" -> {
                    long qId = Long.parseLong(qIdParam);
                    Questionnaire q = em.find(Questionnaire.class, qId);
                    if (q != null && q.getPsp().getId().equals(psp.getId()) && !q.isExternal()) {
                        QuestionnaireField f = new QuestionnaireField();
                        f.setQuestionnaire(q);
                        f.setFieldKey(request.getParameter("fieldKey").trim().toLowerCase().replaceAll("\\s+", "_"));
                        f.setLabel(trimOrNull(request.getParameter("label")));
                        f.setFieldType(request.getParameter("fieldType"));
                        f.setSelectOptions(trimOrNull(request.getParameter("selectOptions")));
                        f.setHelpText(trimOrNull(request.getParameter("helpText")));
                        f.setSectionName(trimOrNull(request.getParameter("sectionName")));
                        f.setRequired("on".equals(request.getParameter("isRequired")));
                        f.setSortOrder(9999);
                        f.setSuppressed(false);

                        em.getTransaction().begin();
                        em.persist(f);
                        em.getTransaction().commit();
                    }
                }

                case "editField" -> {
                    long fieldId = Long.parseLong(request.getParameter("fieldId"));
                    QuestionnaireField f = em.find(QuestionnaireField.class, fieldId);
                    if (f != null && f.getQuestionnaire().getPsp().getId().equals(psp.getId())) {
                        em.getTransaction().begin();
                        f.setFieldKey(request.getParameter("fieldKey").trim().toLowerCase().replaceAll("\\s+", "_"));
                        f.setLabel(trimOrNull(request.getParameter("label")));
                        f.setFieldType(request.getParameter("fieldType"));
                        f.setSelectOptions(trimOrNull(request.getParameter("selectOptions")));
                        f.setHelpText(trimOrNull(request.getParameter("helpText")));
                        f.setSectionName(trimOrNull(request.getParameter("sectionName")));
                        f.setRequired("on".equals(request.getParameter("isRequired")));
                        em.merge(f);
                        em.getTransaction().commit();

                        qIdParam = f.getQuestionnaire().getId().toString();
                    }
                }

                case "suppressField" -> {
                    long fieldId = Long.parseLong(request.getParameter("fieldId"));
                    QuestionnaireField f = em.find(QuestionnaireField.class, fieldId);
                    if (f != null && f.getQuestionnaire().getPsp().getId().equals(psp.getId())) {
                        em.getTransaction().begin();
                        f.setSuppressed(!f.isSuppressed());
                        em.merge(f);
                        em.getTransaction().commit();

                        qIdParam = f.getQuestionnaire().getId().toString();
                    }
                }

                // ── Scoping (clear-all-then-readd pattern) ──────────────────

                case "updateScope" -> {
                    long qId = Long.parseLong(qIdParam);
                    Questionnaire q = em.find(Questionnaire.class, qId);
                    if (q != null && q.getPsp().getId().equals(psp.getId())) {
                        // Force-init collections
                        if (q.getLosList() == null) q.setLosList(new ArrayList<>());
                        if (q.getEnhancementList() == null) q.setEnhancementList(new ArrayList<>());
                        if (q.getServiceItemList() == null) q.setServiceItemList(new ArrayList<>());

                        em.getTransaction().begin();

                        // Clear existing
                        q.getLosList().clear();
                        q.getEnhancementList().clear();
                        q.getServiceItemList().clear();

                        // Re-add selected LOS
                        String[] losIds = request.getParameterValues("losIds");
                        if (losIds != null) {
                            for (String id : losIds) {
                                LOS los = em.find(LOS.class, Long.parseLong(id));
                                if (los != null) q.getLosList().add(los);
                            }
                        }

                        // Re-add selected Enhancements
                        String[] enhIds = request.getParameterValues("enhIds");
                        if (enhIds != null) {
                            for (String id : enhIds) {
                                Enhancement enh = em.find(Enhancement.class, Long.parseLong(id));
                                if (enh != null) q.getEnhancementList().add(enh);
                            }
                        }

                        // Re-add selected ServiceItems
                        String[] siIds = request.getParameterValues("serviceItemIds");
                        if (siIds != null) {
                            for (String id : siIds) {
                                ServiceItem si = em.find(ServiceItem.class, Integer.parseInt(id));
                                if (si != null) q.getServiceItemList().add(si);
                            }
                        }

                        em.merge(q);
                        em.getTransaction().commit();
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("QuestionnaireAction25 error (" + action + "): " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            if (em.isOpen()) em.close();
        }

        // Redirect back to manager with selected questionnaire
        String redirect = "QuestionnaireManager25";
        if (qIdParam != null && !qIdParam.isEmpty()) {
            redirect += "?qId=" + qIdParam;
        }
        response.sendRedirect(redirect);
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
