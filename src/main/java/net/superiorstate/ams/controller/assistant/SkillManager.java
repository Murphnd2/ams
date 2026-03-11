package net.superiorstate.ams.controller.assistant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.ChatbotSkillDAO;
import net.superiorstate.ams.model.general.ChatbotSkill;
import net.superiorstate.ams.model.general.PSP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Admin CRUD servlet for managing chatbot skills.
 * PSP Admin only.
 *
 * URL: /SkillManager
 */
@WebServlet(name = "SkillManager", value = "/SkillManager")
public class SkillManager extends HttpServlet {

    private static final Logger log = LogManager.getLogger(SkillManager.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        boolean isBpoAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isBpoAdmin"));
        if (local == null || (!local.isPspAdmin() && !isBpoAdmin)) {
            response.sendRedirect(isBpoAdmin ? "BpoHome" : "ViewHome25");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = local.getCurrentPerson().getPsp().getId();
            List<ChatbotSkill> skills = ChatbotSkillDAO.getAllSkills(em, pspId);
            request.setAttribute("skills", skills);
        } finally {
            em.close();
        }

        request.getRequestDispatcher("/WEB-INF/view/a/assistant/skillManager25.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        boolean isBpoAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isBpoAdmin"));
        if (local == null || (!local.isPspAdmin() && !isBpoAdmin)) {
            response.sendRedirect(isBpoAdmin ? "BpoHome" : "ViewHome25");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect("SkillManager");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Long pspId = local.getCurrentPerson().getPsp().getId();

            switch (action) {
                case "create" -> {
                    em.getTransaction().begin();
                    ChatbotSkill skill = new ChatbotSkill();
                    skill.setPsp(em.find(PSP.class, pspId));
                    populateFromRequest(skill, request);
                    em.persist(skill);
                    em.getTransaction().commit();
                    log.info("Created chatbot skill: {}", skill.getSkillName());
                }
                case "update" -> {
                    Long skillId = Long.parseLong(request.getParameter("skillId"));
                    em.getTransaction().begin();
                    ChatbotSkill skill = em.find(ChatbotSkill.class, skillId);
                    if (skill != null && skill.getPsp().getId().equals(pspId)) {
                        populateFromRequest(skill, request);
                        em.merge(skill);
                    }
                    em.getTransaction().commit();
                    log.info("Updated chatbot skill: {}", skill != null ? skill.getSkillName() : skillId);
                }
                case "delete" -> {
                    Long skillId = Long.parseLong(request.getParameter("skillId"));
                    em.getTransaction().begin();
                    ChatbotSkill skill = em.find(ChatbotSkill.class, skillId);
                    if (skill != null && skill.getPsp().getId().equals(pspId)) {
                        em.remove(skill);
                    }
                    em.getTransaction().commit();
                    log.info("Deleted chatbot skill ID: {}", skillId);
                }
                case "toggle" -> {
                    Long skillId = Long.parseLong(request.getParameter("skillId"));
                    em.getTransaction().begin();
                    ChatbotSkill skill = em.find(ChatbotSkill.class, skillId);
                    if (skill != null && skill.getPsp().getId().equals(pspId)) {
                        skill.setActive(!skill.isActive());
                        em.merge(skill);
                    }
                    em.getTransaction().commit();
                }
            }
        } catch (Exception e) {
            log.error("Error in SkillManager action: {}", action, e);
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }

        response.sendRedirect("SkillManager");
    }

    private void populateFromRequest(ChatbotSkill skill, HttpServletRequest request) {
        skill.setSkillName(request.getParameter("skillName"));
        skill.setDescription(request.getParameter("description"));
        skill.setSystemPrompt(request.getParameter("systemPrompt"));
        skill.setTriggerKeywords(request.getParameter("triggerKeywords"));
        skill.setAcceptsFileUpload("true".equals(request.getParameter("acceptsFileUpload")));
        skill.setAcceptedMimeTypes(request.getParameter("acceptedMimeTypes"));
        skill.setModel(request.getParameter("model") != null && !request.getParameter("model").isBlank()
                ? request.getParameter("model") : "claude-haiku-4-5-20251001");
        skill.setMaxTokens(parseIntOrDefault(request.getParameter("maxTokens"), 1024));
        skill.setAdminOnly("true".equals(request.getParameter("adminOnly")));
        skill.setSortOrder(parseIntOrDefault(request.getParameter("sortOrder"), 100));
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value != null && !value.isBlank() ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
