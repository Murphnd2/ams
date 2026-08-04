package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.ProposalIchraSnapshot;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;
import net.superiorstate.ams.model.sales.offering.ProposalSection;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
    // Note: <style> blocks are intentionally preserved — PSP admins are trusted users
    // and <style> is required for rich cover-page HTML.
    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("<script[\\s\\S]*?>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_HANDLER_PATTERN =
            Pattern.compile("(?i)\\s*on[a-z]+\\s*=\\s*\"[^\"]*\"");
    private static final Pattern EVENT_HANDLER_SINGLE_PATTERN =
            Pattern.compile("(?i)\\s*on[a-z]+\\s*=\\s*'[^']*'");
    private static final Pattern JS_PROTOCOL_PATTERN =
            Pattern.compile("(?i)(href|src)\\s*=\\s*\"\\s*javascript:[^\"]*\"");
    private static final Pattern JS_PROTOCOL_SINGLE_PATTERN =
            Pattern.compile("(?i)(href|src)\\s*=\\s*'\\s*javascript:[^']*'");

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

            // Force-init M:N collections and agency while EM is open
            for (ProposalSection s : sections) {
                if (s.getLosList() != null) s.getLosList().size();
                if (s.getEnhancementList() != null) s.getEnhancementList().size();
                if (s.getAgency() != null) s.getAgency().getId();
            }

            // Load LOS and Enhancement lists for scope checkboxes
            List<LOS> allLos = em.createQuery(
                    "SELECT l FROM LOS l WHERE l.psp.id = :pspId AND l.suppressed = false ORDER BY l.sortOrder", LOS.class)
                    .setParameter("pspId", psp.getId().longValue()).getResultList();
            List<Enhancement> allEnhancements = em.createQuery(
                    "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId AND e.suppressed = false ORDER BY e.sortOrder", Enhancement.class)
                    .setParameter("pspId", psp.getId().longValue()).getResultList();

            // Load agencies for agency override dropdowns
            List<Agency> agencies = em.createQuery(
                    "SELECT a FROM Agency a WHERE a.psp.id = :pspId ORDER BY a.name", Agency.class)
                    .setParameter("pspId", psp.getId())
                    .getResultList();

            request.setAttribute("sections", sections);
            request.setAttribute("allLos", allLos);
            request.setAttribute("allEnhancements", allEnhancements);
            request.setAttribute("agencyList", agencies);
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
        String redirectSectionId = request.getParameter("sectionId"); // preserve focus after redirect

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
                        redirectSectionId = String.valueOf(custom.getId());
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
                        redirectSectionId = null; // deleted — fall back to first section
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
                        // Allow toggle for: CUSTOM, FEATURES, PRICING, or agency-scoped TITLE/CLOSING
                        boolean isDefaultTitleClosing = ("TITLE".equals(type) || "CLOSING".equals(type)) && section.getAgency() == null;
                        if (!isDefaultTitleClosing) {
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

                case "createAgencySection" -> {
                    String type = request.getParameter("sectionType"); // "TITLE" or "CLOSING"
                    long agencyId = Long.parseLong(request.getParameter("agencyId"));

                    if ("TITLE".equals(type) || "CLOSING".equals(type)) {
                        Agency agency = em.find(Agency.class, agencyId);
                        if (agency != null) {
                            // Check if one already exists for this agency + type
                            List<ProposalSection> existing = em.createQuery(
                                    "SELECT s FROM ProposalSection s WHERE s.psp.id = :pspId AND s.agency.id = :agencyId AND s.sectionType = :type",
                                    ProposalSection.class)
                                    .setParameter("pspId", psp.getId())
                                    .setParameter("agencyId", agencyId)
                                    .setParameter("type", type)
                                    .getResultList();

                            if (existing.isEmpty()) {
                                // Find the default section of this type to copy content from
                                List<ProposalSection> defaults = em.createQuery(
                                        "SELECT s FROM ProposalSection s WHERE s.psp.id = :pspId AND s.agency IS NULL AND s.sectionType = :type",
                                        ProposalSection.class)
                                        .setParameter("pspId", psp.getId())
                                        .setParameter("type", type)
                                        .getResultList();

                                String defaultHtml = "";
                                int sortOrder = "TITLE".equals(type) ? 1 : 999;
                                if (!defaults.isEmpty()) {
                                    defaultHtml = defaults.get(0).getHtmlContent() != null ? defaults.get(0).getHtmlContent() : "";
                                    sortOrder = defaults.get(0).getSortOrder();
                                }

                                em.getTransaction().begin();
                                ProposalSection section = new ProposalSection();
                                section.setPsp(psp);
                                section.setAgency(agency);
                                section.setSectionType(type);
                                section.setTitle(type.substring(0, 1) + type.substring(1).toLowerCase() + " \u2014 " + agency.getName());
                                section.setHtmlContent(defaultHtml);
                                section.setSortOrder(sortOrder);
                                section.setActive(true);
                                section.setScope("ALL");
                                em.persist(section);
                                em.getTransaction().commit();
                                request.getSession().setAttribute("flashMessage", "Agency override created.");
                            }
                        }
                    }
                }

                case "createIchraSection" -> {
                    // Build-plan item 6. Modeled on createAgencySection above.
                    // ⚠️ Not extending initializeDefaults — that only fires when a PSP
                    // has zero sections at all, and every real PSP already has the four
                    // defaults, so it would never run for this.
                    // LOS-scoped from the moment it exists (S5): a scope='ALL' section
                    // is never created, not even transiently — if no LOS resolves, the
                    // action refuses outright rather than creating an unscoped section
                    // to be fixed up later. Singleton per PSP (unlike CUSTOM, which
                    // allows many): a second ICHRA_ILLUSTRATION section would render
                    // twice on any proposal carrying a snapshot.
                    String losIdParam = request.getParameter("losId");
                    LOS ichraLos = null;
                    if (losIdParam != null && !losIdParam.isBlank()) {
                        try {
                            long losId = Long.parseLong(losIdParam.trim());
                            LOS candidate = em.find(LOS.class, losId);
                            if (candidate != null && candidate.getPsp() != null && candidate.getPsp().getId().equals(psp.getId())) {
                                ichraLos = candidate;
                            }
                        } catch (NumberFormatException ignored) {}
                    }

                    if (ichraLos == null) {
                        request.getSession().setAttribute("flashMessage",
                                "Could not create the ICHRA Illustration section — select a line of service to scope it to.");
                    } else {
                        List<ProposalSection> existingIchra = em.createQuery(
                                        "SELECT s FROM ProposalSection s WHERE s.psp.id = :pspId AND s.sectionType = :type",
                                        ProposalSection.class)
                                .setParameter("pspId", psp.getId())
                                .setParameter("type", ProposalIchraSnapshot.SECTION_TYPE)
                                .getResultList();

                        if (!existingIchra.isEmpty()) {
                            request.getSession().setAttribute("flashMessage", "An ICHRA Illustration section already exists for this PSP.");
                        } else {
                            List<ProposalSection> sections = loadSections(em, psp);
                            int maxOrder = sections.stream()
                                    .filter(s -> !"CLOSING".equals(s.getSectionType()))
                                    .mapToInt(ProposalSection::getSortOrder)
                                    .max().orElse(0);

                            em.getTransaction().begin();
                            ProposalSection ichraSection = new ProposalSection();
                            ichraSection.setPsp(psp);
                            ichraSection.setSectionType(ProposalIchraSnapshot.SECTION_TYPE);
                            ichraSection.setTitle("ICHRA Illustration");
                            ichraSection.setSortOrder(maxOrder + 1);
                            ichraSection.setActive(true);
                            ichraSection.setScope("SCOPED");
                            ichraSection.getLosList().add(ichraLos);
                            em.persist(ichraSection);

                            // Ensure CLOSING is after this new section
                            for (ProposalSection s : sections) {
                                if ("CLOSING".equals(s.getSectionType()) && s.getSortOrder() <= ichraSection.getSortOrder()) {
                                    s.setSortOrder(ichraSection.getSortOrder() + 1);
                                    em.merge(s);
                                }
                            }
                            em.getTransaction().commit();
                            redirectSectionId = String.valueOf(ichraSection.getId());
                            request.getSession().setAttribute("flashMessage", "ICHRA Illustration section created, scoped to " + ichraLos.getDescription() + ".");
                        }
                    }
                }

                case "createMarketSection" -> {
                    // S11-H. Modeled on createIchraSection above, with two deliberate differences.
                    // ⚠️ Not extending initializeDefaults, for the same reason that one isn't:
                    // it only fires when a PSP has zero sections at all, and every real PSP
                    // already has the four defaults, so it would never run for this.
                    //
                    // (1) scope stays 'ALL' and no LOS is attached. Unlike ICHRA_ILLUSTRATION,
                    //     this page's visibility is resolved entirely server-side per proposal by
                    //     ViewProposal.resolveMarketPage — which already requires the proposal to
                    //     quote a plus-tier LOS — so section-level LOS scoping would be a second,
                    //     independently-maintained copy of the same condition.
                    // (2) Agency-agnostic by design (S11-F): the agency-override block in
                    //     ViewProposal handles TITLE/CLOSING only, and a MARKET row carrying an
                    //     agency_id would fall straight through it and render for every agency.
                    //     So no agency is ever set here.
                    //
                    // Singleton per PSP, like ICHRA_ILLUSTRATION: a second MARKET section would
                    // render the page twice on any qualifying proposal.
                    List<ProposalSection> existingMarket = em.createQuery(
                                    "SELECT s FROM ProposalSection s WHERE s.psp.id = :pspId AND s.sectionType = :type",
                                    ProposalSection.class)
                            .setParameter("pspId", psp.getId())
                            .setParameter("type", ViewProposal.MARKET_SECTION_TYPE)
                            .getResultList();

                    if (!existingMarket.isEmpty()) {
                        request.getSession().setAttribute("flashMessage", "A Market section already exists for this PSP.");
                    } else {
                        List<ProposalSection> sections = loadSections(em, psp);
                        int maxOrder = sections.stream()
                                .filter(s -> !"CLOSING".equals(s.getSectionType()))
                                .mapToInt(ProposalSection::getSortOrder)
                                .max().orElse(0);

                        em.getTransaction().begin();
                        ProposalSection marketSection = new ProposalSection();
                        marketSection.setPsp(psp);
                        marketSection.setSectionType(ViewProposal.MARKET_SECTION_TYPE);
                        marketSection.setTitle("Individual Market Overview");
                        marketSection.setSortOrder(maxOrder + 1);
                        marketSection.setActive(true);
                        marketSection.setScope("ALL");
                        em.persist(marketSection);

                        // Ensure CLOSING is after this new section
                        for (ProposalSection s : sections) {
                            if ("CLOSING".equals(s.getSectionType()) && s.getSortOrder() <= marketSection.getSortOrder()) {
                                s.setSortOrder(marketSection.getSortOrder() + 1);
                                em.merge(s);
                            }
                        }
                        em.getTransaction().commit();
                        redirectSectionId = String.valueOf(marketSection.getId());
                        request.getSession().setAttribute("flashMessage",
                                "Market section created. It appears on a proposal only when the agency is ICHRA-enabled, "
                                        + "the proposal quotes a plus-tier line of service, and production rate data exists for the county.");
                    }
                }

                case "deleteAgencySection" -> {
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    ProposalSection section = em.find(ProposalSection.class, sectionId);
                    if (section != null && section.getPsp().getId().equals(psp.getId())
                            && section.getAgency() != null
                            && ("TITLE".equals(section.getSectionType()) || "CLOSING".equals(section.getSectionType()))) {
                        em.getTransaction().begin();
                        em.remove(section);
                        em.getTransaction().commit();
                        request.getSession().setAttribute("flashMessage", "Agency override deleted.");
                    }
                }

                case "updateScope" -> {
                    long sectionId = Long.parseLong(request.getParameter("sectionId"));
                    String scope = request.getParameter("scope");
                    ProposalSection section = em.find(ProposalSection.class, sectionId);

                    if (section != null
                            && ("CUSTOM".equals(section.getSectionType()) || ProposalIchraSnapshot.SECTION_TYPE.equals(section.getSectionType()))
                            && section.getPsp().getId().equals(psp.getId())) {

                        // Initialize collections if needed
                        if (section.getLosList() == null) section.setLosList(new ArrayList<>());
                        if (section.getEnhancementList() == null) section.setEnhancementList(new ArrayList<>());

                        em.getTransaction().begin();
                        section.setScope("SCOPED".equals(scope) ? "SCOPED" : "ALL");

                        // Clear existing associations
                        section.getLosList().clear();
                        section.getEnhancementList().clear();

                        // If SCOPED, add selected LOS and Enhancement associations
                        if ("SCOPED".equals(scope)) {
                            String[] losIds = request.getParameterValues("losIds");
                            if (losIds != null) {
                                for (String id : losIds) {
                                    LOS los = em.find(LOS.class, Long.parseLong(id));
                                    if (los != null) section.getLosList().add(los);
                                }
                            }
                            String[] enhIds = request.getParameterValues("enhIds");
                            if (enhIds != null) {
                                for (String id : enhIds) {
                                    Enhancement enh = em.find(Enhancement.class, Long.parseLong(id));
                                    if (enh != null) section.getEnhancementList().add(enh);
                                }
                            }
                        }

                        em.merge(section);
                        em.getTransaction().commit();
                        request.getSession().setAttribute("flashMessage", "Scope updated.");
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

        String redirectUrl = redirectSectionId != null
                ? "ProposalSettings?sectionId=" + redirectSectionId
                : "ProposalSettings";
        response.sendRedirect(redirectUrl);
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
        title.setScope("ALL");
        em.persist(title);

        ProposalSection features = new ProposalSection();
        features.setPsp(psp);
        features.setSectionType("FEATURES");
        features.setTitle("Features");
        features.setSortOrder(2);
        features.setActive(true);
        features.setScope("ALL");
        em.persist(features);

        ProposalSection pricing = new ProposalSection();
        pricing.setPsp(psp);
        pricing.setSectionType("PRICING");
        pricing.setTitle("Pricing");
        pricing.setSortOrder(3);
        pricing.setActive(true);
        pricing.setScope("ALL");
        em.persist(pricing);

        ProposalSection closing = new ProposalSection();
        closing.setPsp(psp);
        closing.setSectionType("CLOSING");
        closing.setTitle("Closing Page");
        closing.setHtmlContent(DEFAULT_CLOSING_HTML);
        closing.setSortOrder(4);
        closing.setActive(true);
        closing.setScope("ALL");
        em.persist(closing);

        em.getTransaction().commit();
    }

    /**
     * HTML sanitization for trusted PSP admin users.
     * Strips: <script> tags, on* event handlers, javascript: protocols.
     * Preserves: <style> blocks, inline styles, CSS variables, merge tokens.
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
