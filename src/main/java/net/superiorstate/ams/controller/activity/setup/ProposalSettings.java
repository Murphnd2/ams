package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.offering.ProposalSection;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Pattern;

@WebServlet(name = "ProposalSettings", value = "/ProposalSettings")
public class ProposalSettings extends HttpServlet {

    private static final String DEFAULT_TITLE_HTML =
            "<div style=\"text-align: center; padding: 3rem 1rem;\">\n" +
            "  <h1 style=\"font-size: 2rem; font-weight: 700; color: {{PRIMARY_COLOR}};\">Service Proposal</h1>\n" +
            "  <h2 style=\"font-size: 1.4rem; color: #333; margin-top: 1rem;\">Prepared for {{PROSPECT_NAME}}</h2>\n" +
            "  <p style=\"font-size: 1.1rem; color: #666; margin-top: 2rem;\">Prepared by {{AGENT_NAME}}</p>\n" +
            "  <p style=\"color: #666;\">{{AGENCY_NAME}}</p>\n" +
            "  <p style=\"color: #999; margin-top: 3rem;\">{{DATE_CREATED}}</p>\n" +
            "</div>";

    private static final String DEFAULT_CLOSING_HTML =
            "<div style=\"text-align: center; padding: 2rem 1rem;\">\n" +
            "  <h3 style=\"color: {{PRIMARY_COLOR}};\">Ready to Get Started?</h3>\n" +
            "  <p style=\"color: #666; margin-bottom: 1.5rem;\">Click below to begin your application.</p>\n" +
            "  {{APPLY_BUTTON}}\n" +
            "  <p style=\"color: #999; margin-top: 2rem; font-size: 0.85rem;\">Questions? Contact {{AGENT_NAME}} at {{AGENT_EMAIL}}</p>\n" +
            "</div>";

    // Patterns for HTML sanitization
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("\\s+on\\w+\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_HANDLER_SINGLE_PATTERN = Pattern.compile("\\s+on\\w+\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_PROTOCOL_PATTERN = Pattern.compile("(href|src)\\s*=\\s*\"\\s*javascript:[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_PROTOCOL_SINGLE_PATTERN = Pattern.compile("(href|src)\\s*=\\s*'\\s*javascript:[^']*'", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        try {
            List<ProposalSection> sections = loadSections(em, psp);

            // Auto-initialize defaults if none exist
            if (sections.isEmpty()) {
                initializeDefaults(em, psp);
                sections = loadSections(em, psp);
            }

            request.setAttribute("sections", sections);
            request.setAttribute("pageTitle", "Proposal Settings");
            request.setAttribute("pageIcon", "bi-file-earmark-text");

        } finally {
            em.close();
        }

        request.getRequestDispatcher("/WEB-INF/view/sales/proposalSettings.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        PSP psp = local.getCurrentPerson().getPsp();

        String action = request.getParameter("action");

        try {
            switch (action) {

                case "saveContent" -> {
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    String htmlContent = request.getParameter("htmlContent");
                    ProposalSection section = em.find(ProposalSection.class, sectionId);
                    if (section != null && section.getPsp().getId().equals(psp.getId())) {
                        String type = section.getSectionType();
                        if ("TITLE".equals(type) || "CLOSING".equals(type) || "CUSTOM".equals(type)) {
                            em.getTransaction().begin();
                            section.setHtmlContent(sanitizeHtml(htmlContent));
                            em.merge(section);
                            em.getTransaction().commit();
                        }
                    }
                    request.getSession().setAttribute("flashMessage", "Content saved.");
                }

                case "createCustom" -> {
                    String title = request.getParameter("title");
                    if (title != null && !title.trim().isEmpty()) {
                        // Find max sort_order (before CLOSING)
                        List<ProposalSection> sections = loadSections(em, psp);
                        int maxOrder = sections.stream()
                                .filter(s -> !"CLOSING".equals(s.getSectionType()))
                                .mapToInt(ProposalSection::getSortOrder)
                                .max().orElse(0);

                        ProposalSection custom = new ProposalSection();
                        custom.setPsp(psp);
                        custom.setSectionType("CUSTOM");
                        custom.setTitle(title.trim());
                        custom.setHtmlContent("");
                        custom.setSortOrder(maxOrder + 1);
                        custom.setActive(true);

                        em.getTransaction().begin();
                        em.persist(custom);

                        // Ensure CLOSING is after this new section
                        for (ProposalSection s : sections) {
                            if ("CLOSING".equals(s.getSectionType()) && s.getSortOrder() <= custom.getSortOrder()) {
                                s.setSortOrder(custom.getSortOrder() + 1);
                                em.merge(s);
                            }
                        }
                        em.getTransaction().commit();
                        request.getSession().setAttribute("flashMessage", "Custom page created.");
                    }
                }

                case "deleteCustom" -> {
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    ProposalSection section = em.find(ProposalSection.class, sectionId);
                    if (section != null && "CUSTOM".equals(section.getSectionType()) && section.getPsp().getId().equals(psp.getId())) {
                        em.getTransaction().begin();
                        em.remove(section);
                        em.getTransaction().commit();
                        request.getSession().setAttribute("flashMessage", "Custom page deleted.");
                    }
                }

                case "reorder" -> {
                    // AJAX endpoint — read JSON body
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = request.getReader().readLine()) != null) {
                        sb.append(line);
                    }
                    String json = sb.toString().trim();
                    // Parse simple JSON array of IDs: [1,2,3,4]
                    json = json.replace("[", "").replace("]", "").replace(" ", "");
                    String[] idStrings = json.split(",");

                    List<ProposalSection> sections = loadSections(em, psp);

                    // Validate: first must be TITLE, last must be CLOSING
                    if (idStrings.length > 0) {
                        long firstId = Long.parseLong(idStrings[0]);
                        long lastId = Long.parseLong(idStrings[idStrings.length - 1]);
                        ProposalSection first = sections.stream().filter(s -> s.getId().equals(firstId)).findFirst().orElse(null);
                        ProposalSection last = sections.stream().filter(s -> s.getId().equals(lastId)).findFirst().orElse(null);

                        if (first != null && "TITLE".equals(first.getSectionType()) &&
                                last != null && "CLOSING".equals(last.getSectionType())) {
                            em.getTransaction().begin();
                            for (int i = 0; i < idStrings.length; i++) {
                                long id = Long.parseLong(idStrings[i].trim());
                                for (ProposalSection s : sections) {
                                    if (s.getId().equals(id)) {
                                        s.setSortOrder(i + 1);
                                        em.merge(s);
                                        break;
                                    }
                                }
                            }
                            em.getTransaction().commit();
                        }
                    }

                    // Respond with 200 OK for AJAX
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.print("{\"status\":\"ok\"}");
                    out.flush();
                    em.close();
                    return; // Don't redirect for AJAX
                }

                case "toggleActive" -> {
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    ProposalSection section = em.find(ProposalSection.class, sectionId);
                    if (section != null && section.getPsp().getId().equals(psp.getId())) {
                        String type = section.getSectionType();
                        // Don't allow deactivating TITLE or CLOSING
                        if (!"TITLE".equals(type) && !"CLOSING".equals(type)) {
                            em.getTransaction().begin();
                            section.setActive(!section.isActive());
                            em.merge(section);
                            em.getTransaction().commit();
                        }
                    }
                }

                case "renameSection" -> {
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    String title = request.getParameter("title");
                    ProposalSection section = em.find(ProposalSection.class, sectionId);
                    if (section != null && "CUSTOM".equals(section.getSectionType())
                            && section.getPsp().getId().equals(psp.getId())
                            && title != null && !title.trim().isEmpty()) {
                        em.getTransaction().begin();
                        section.setTitle(title.trim());
                        em.merge(section);
                        em.getTransaction().commit();
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("ProposalSettings error (" + action + "): " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect("ProposalSettings");
    }

    private List<ProposalSection> loadSections(EntityManager em, PSP psp) {
        return em.createQuery(
                        "SELECT s FROM ProposalSection s WHERE s.psp.id = :pspId ORDER BY s.sortOrder",
                        ProposalSection.class)
                .setParameter("pspId", psp.getId())
                .getResultList();
    }

    private void initializeDefaults(EntityManager em, PSP psp) {
        em.getTransaction().begin();

        ProposalSection title = new ProposalSection();
        title.setPsp(psp);
        title.setSectionType("TITLE");
        title.setTitle("Title Page");
        title.setHtmlContent(DEFAULT_TITLE_HTML);
        title.setSortOrder(1);
        title.setActive(true);
        em.persist(title);

        ProposalSection features = new ProposalSection();
        features.setPsp(psp);
        features.setSectionType("FEATURES");
        features.setTitle("Features");
        features.setSortOrder(2);
        features.setActive(true);
        em.persist(features);

        ProposalSection pricing = new ProposalSection();
        pricing.setPsp(psp);
        pricing.setSectionType("PRICING");
        pricing.setTitle("Pricing");
        pricing.setSortOrder(3);
        pricing.setActive(true);
        em.persist(pricing);

        ProposalSection closing = new ProposalSection();
        closing.setPsp(psp);
        closing.setSectionType("CLOSING");
        closing.setTitle("Closing Page");
        closing.setHtmlContent(DEFAULT_CLOSING_HTML);
        closing.setSortOrder(4);
        closing.setActive(true);
        em.persist(closing);

        em.getTransaction().commit();
    }

    /**
     * Basic HTML sanitization — strips script tags, event handlers, and javascript: protocols.
     * This is a safety net for trusted PSP admin users, not a security boundary.
     */
    static String sanitizeHtml(String html) {
        if (html == null) return "";
        String result = html;
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll("");
        result = EVENT_HANDLER_SINGLE_PATTERN.matcher(result).replaceAll("");
        result = JS_PROTOCOL_PATTERN.matcher(result).replaceAll("$1=\"\"");
        result = JS_PROTOCOL_SINGLE_PATTERN.matcher(result).replaceAll("$1=''");
        return result;
    }
}
