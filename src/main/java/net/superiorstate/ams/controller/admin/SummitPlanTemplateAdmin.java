package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SummitPlanTemplateMapDAO;
import net.superiorstate.ams.data.resolver.SummitPlanDateRuleResolver;
import net.superiorstate.ams.data.resolver.SummitPlanTemplateResolver;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * T202 / S31-F — PSP Admin screen for the Summit plan template mapping (V095).
 * <p>
 * V095 created {@code summit_plan_template_map} but nothing could write to it, so every
 * installation still ran on the {@code SUMMIT_PLAN_TEMPLATES} property fallback. This screen is
 * the writer. It changes no export behaviour on its own — {@code SummitPlanTemplateResolver}
 * already prefers the table and falls back to the property, and neither it nor any export writer
 * is touched here.
 * <p>
 * <b>Gate: PSP admin only.</b> {@link #isAuthorized(HttpSession)} is copied verbatim from
 * {@code RateCacheAdmin} — the nearest admin-screen precedent — and the nav entry sits inside
 * {@code navbar25.jsp}'s existing {@code <c:if test="${sessionScope.isPspAdmin}">} block. The gate
 * is not invented here.
 * <p>
 * <b>Shape follows {@code RateCacheAdmin}</b> (servlet mapping, gate, forward, POST-redirect-GET
 * with session flash attributes) and <b>{@code ProviderSetup}</b> (an {@code action} switch over
 * add / edit / delete of mapping rows, deleting rather than deactivating).
 * <p>
 * ⚠️ <b>The listing includes inactive rows, deliberately.</b>
 * {@code uq_summit_plan_template_map_psp_service_seq} (V103) does not consider {@code is_active},
 * so an inactive row still occupies its (PSP, ServiceItem, seq) slot, and the W5 uniqueness rules
 * on key segment and label count inactive rows too. Hiding them would leave an admin staring at an
 * empty list while an add failed — precisely the trap T202 was filed against.
 * <p>
 * <b>W5 (V103): several rows per service item are the fan-out</b> — one Summit plan per row, each
 * with its own {@code seq}, effective-date rule and offsets, which {@code SummitExportServlet}'s
 * file 2 writer evaluates through {@code SummitPlanDateRuleResolver} (W4). The old "one mapping per
 * service item" pre-check is replaced by three validations in {@link #save}: the rule must be one
 * the resolver supports (V1); the key segment must be unique within the PSP (V2 — it composes
 * Import Plan ID, Summit's upsert key, so a duplicate silently overwrites a plan); and the emitted
 * Plan Name must be unique within the PSP (V3 — file 2's results correlate on it with no row
 * number). Pre-existing violations are never auto-fixed; an edit that does not touch the offending
 * field is allowed through.
 * <p>
 * ⚠️ <b>The {@code ServiceItem} picker shows the id, not just the description.</b>
 * {@code ServiceItem.code} is null on every real installation (T189) and {@code description} is a
 * copy of a LOS's {@code shortText} taken at creation and never updated on rename (T190) — so two
 * items can read identically and the description is a display string, not a key. <b>The id is the
 * only thing that distinguishes them</b>, and it is what the mapping actually stores.
 */
@WebServlet(name = "SummitPlanTemplateAdmin", value = "/SummitPlanTemplateAdmin")
public class SummitPlanTemplateAdmin extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/a/admin/summitPlanTemplateAdmin25.jsp";

    /** Flash attribute names, following {@code RateCacheAdmin}'s {@code rateCacheMessage}/{@code rateCacheError}. */
    private static final String FLASH_MESSAGE = "summitPlanTemplateMessage";
    private static final String FLASH_ERROR = "summitPlanTemplateError";

    /**
     * W5 -- the {@code effective_date_rule} values the form offers and {@link #save} accepts:
     * exactly the set {@link SummitPlanDateRuleResolver} evaluates, referenced not restated, so a
     * rule the emitter would refuse can never be saved here.
     */
    private static final List<String> RULE_OPTIONS = List.of(
            SummitPlanDateRuleResolver.RULE_PLAN_YEAR_START,
            SummitPlanDateRuleResolver.RULE_MOST_RECENT_PAST_MONTHDAY);

    /**
     * Setup-category service items are the ones a sale elects, and the ones the export matches
     * against — {@code ActivityCategory} 2, the same category {@code AmsDataLocal}'s own
     * Setup-module listing filters on. Renewal-category items can never appear in an
     * {@code ApplicationModule}, so offering them would only invite a mapping that never fires.
     */
    private static final int SETUP_ACTIVITY_CATEGORY = 2;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);
            if (pspId == null) {
                session.setAttribute(FLASH_ERROR, "Could not determine your PSP from this session."
                        + " Sign out and back in, then try again.");
                request.setAttribute("mappings", List.of());
                request.setAttribute("serviceItems", List.of());
                forward(request, response);
                return;
            }

            List<SummitPlanTemplateMap> mappings = SummitPlanTemplateMapDAO.findAllByPspId(em, pspId);

            // Which source is actually live. This is the single most useful thing the screen can
            // say: the resolver prefers the table and falls back to the property, so with no ACTIVE
            // row the property is what every export is really using — regardless of how many
            // inactive rows are listed below.
            boolean anyActive = false;
            for (SummitPlanTemplateMap mapping : mappings) {
                if (mapping.isActive()) { anyActive = true; break; }
            }
            String propertyRaw = AppConfig.get(SummitPlanTemplateResolver.CONFIG_KEY);
            request.setAttribute("tableIsLive", anyActive);
            request.setAttribute("propertyKey", SummitPlanTemplateResolver.CONFIG_KEY);
            request.setAttribute("propertyRaw", propertyRaw == null ? "" : propertyRaw);
            request.setAttribute("propertyEntryCount", SummitPlanTemplateResolver.configured().size());

            request.setAttribute("mappings", mappings);
            request.setAttribute("serviceItems", loadSetupServiceItems(em, pspId));
            // W5 -- the rule select is populated from the emitter's own supported set, so the
            // screen can never offer a value the date resolver would refuse.
            request.setAttribute("ruleOptions", RULE_OPTIONS);
            request.setAttribute("defaultRule", SummitPlanDateRuleResolver.RULE_PLAN_YEAR_START);

            // Resolved here rather than in EL. A JSP-side "editId + 0" coercion throws on a
            // non-numeric parameter, and with no JSP precompiler in this build (T199) that would
            // surface as a 500 on a click rather than at build time. An editId that matches
            // nothing simply falls through to the blank add form.
            Long editId = parseLongOrNull(request.getParameter("editId"));
            SummitPlanTemplateMap editing = null;
            if (editId != null) {
                for (SummitPlanTemplateMap mapping : mappings) {
                    if (editId.equals(mapping.getId())) { editing = mapping; break; }
                }
            }
            request.setAttribute("editing", editing);

            forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);
            if (pspId == null) {
                session.setAttribute(FLASH_ERROR, "Could not determine your PSP from this session."
                        + " Nothing was saved.");
                response.sendRedirect(request.getContextPath() + "/SummitPlanTemplateAdmin");
                return;
            }

            String action = request.getParameter("action");
            if ("save".equals(action)) {
                save(request, session, em, pspId);
            } else if ("delete".equals(action)) {
                delete(request, session, em, pspId);
            }
        } catch (RuntimeException e) {
            // Never swallowed. The seq-slot pre-check in save() handles the ordinary duplicate case with a
            // better message, but it cannot close the race between two admins, and a constraint
            // violation must reach the operator rather than a log nobody reads.
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/SummitPlanTemplateAdmin");
    }

    // ── Actions ───────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Long id = parseLongOrNull(request.getParameter("id"));
        Integer serviceItemId = parseIntOrNull(request.getParameter("serviceItemId"));
        Integer templateId = parseIntOrNull(request.getParameter("templateId"));
        String keySegment = trimToEmpty(request.getParameter("keySegment"));
        String label = trimToEmpty(request.getParameter("label"));
        Integer sortOrder = parseIntOrNull(request.getParameter("sortOrder"));
        boolean active = request.getParameter("active") != null;
        // W5 (V103) -- the fan-out discriminator and date rules. seq blank on add = next free
        // ordinal for the service item (see below); the offsets default to 0.
        Integer seq = parseIntOrNull(request.getParameter("seq"));
        String effectiveDateRule = trimToEmpty(request.getParameter("effectiveDateRule")).toUpperCase();
        Integer offsetMonths = parseIntOrNull(request.getParameter("offsetMonths"));
        Integer planYearOffsetYears = parseIntOrNull(request.getParameter("planYearOffsetYears"));

        if (serviceItemId == null || serviceItemId <= 0) {
            session.setAttribute(FLASH_ERROR, "Choose a service item.");
            return;
        }
        if (templateId == null || templateId <= 0) {
            session.setAttribute(FLASH_ERROR, "Plan Template ID must be a positive whole number."
                    + " It is the id Summit assigned to the plan template in your tenant.");
            return;
        }
        String rejection = keySegmentRejection(keySegment);
        if (rejection != null) {
            session.setAttribute(FLASH_ERROR, "Key segment rejected: " + rejection
                    + ". It travels inside Import Plan ID, which is an upsert key in a"
                    + " pipe-delimited file, so it must be letters and digits only — no pipe,"
                    + " whitespace, hyphen or other punctuation.");
            return;
        }
        if (sortOrder == null) sortOrder = 0;
        if (offsetMonths == null) offsetMonths = 0;
        if (planYearOffsetYears == null) planYearOffsetYears = 0;
        // V1 -- the rule must be one the emitter evaluates. The form is a select over the same
        // list, so this fires only on a hand-crafted POST; the emitter's 500 must never be the
        // first place a bad value surfaces.
        if (!RULE_OPTIONS.contains(effectiveDateRule)) {
            session.setAttribute(FLASH_ERROR, "Effective date rule '" + effectiveDateRule
                    + "' is not supported. Supported values are: "
                    + SummitPlanDateRuleResolver.SUPPORTED_RULES + ".");
            return;
        }

        // The service item must belong to this PSP. Without this an admin could map another
        // installation's item by typing its id, and the export would then emit a plan for a
        // service this employer never elected.
        ServiceItem serviceItem = em.find(ServiceItem.class, serviceItemId);
        if (serviceItem == null || serviceItem.getPsp() == null
                || !pspId.equals(serviceItem.getPsp().getId())) {
            session.setAttribute(FLASH_ERROR, "Service item " + serviceItemId
                    + " does not belong to your PSP.");
            return;
        }

        SummitPlanTemplateMap mapping;
        if (id == null) {
            // W5 (V103) -- several rows per service item are now the fan-out, so the "one mapping
            // per service item" pre-check that stood here is gone. Its replacements (V2/V3 and the
            // seq slot check) run below, on add and edit alike.
            mapping = new SummitPlanTemplateMap();
            mapping.setPspId(pspId);
            mapping.setCreatedAt(LocalDateTime.now());
            mapping.setCreatedBy(resolveCurrentUserName(request));
        } else {
            mapping = SummitPlanTemplateMapDAO.findById(em, id);
            if (mapping == null || !pspId.equals(mapping.getPspId())) {
                session.setAttribute(FLASH_ERROR, "That mapping no longer exists, or belongs to"
                        + " another PSP. Nothing was saved.");
                return;
            }
        }

        // Every other row of this PSP -- active AND inactive, since an inactive row still holds
        // its slot and can be reactivated. One list serves the seq slot check, V2 and V3.
        List<SummitPlanTemplateMap> others = new ArrayList<>();
        for (SummitPlanTemplateMap other : SummitPlanTemplateMapDAO.findAllByPspId(em, pspId)) {
            if (id == null || !id.equals(other.getId())) others.add(other);
        }

        // seq: blank on add = next free ordinal for this service item (max + 1, or 0 when it has
        // none) -- the convenience Kevin asked for, applied only when the field is left empty.
        if (seq == null) {
            int max = -1;
            for (SummitPlanTemplateMap other : others) {
                if (serviceItemId.equals(other.getServiceItemId()) && other.getSeq() > max) {
                    max = other.getSeq();
                }
            }
            seq = id == null ? max + 1 : (int) mapping.getSeq();
        }
        if (seq < 0 || seq > Short.MAX_VALUE) {
            session.setAttribute(FLASH_ERROR, "Seq must be between 0 and " + Short.MAX_VALUE + ".");
            return;
        }
        // (psp, service item, seq) is the unique key. Pre-checked so the rejection reads in words
        // rather than as a constraint violation.
        for (SummitPlanTemplateMap other : others) {
            if (serviceItemId.equals(other.getServiceItemId()) && other.getSeq() == seq) {
                session.setAttribute(FLASH_ERROR, "Service item " + serviceItemId + " already has a"
                        + " mapping at seq " + seq + " (row " + other.getId() + ", template "
                        + other.getTemplateId() + ", key segment '" + other.getKeySegment() + "',"
                        + (other.isActive() ? " ACTIVE" : " INACTIVE") + "). Seq is the ordinal"
                        + " within a service item and must be unique there; leave it blank to take"
                        + " the next free one, or edit that row instead.");
                return;
            }
        }

        // V2 -- key segment unique within the PSP, across every service item, inactive rows
        // included. Import Plan ID is sanitize(employerTpaCustomId + keySegment) and is Summit's
        // upsert key: two rows sharing a segment compose the SAME key, so the second plan silently
        // overwrites the first in Summit -- no error, wrong plan funded -- and the ICHRA
        // size() != 1 refusals trip as well. Case-insensitive, the conservative reading of an
        // upsert key whose case handling is unestablished. On edit, checked only when the segment
        // is being changed, so a row already in violation under the pre-V103 key can still be
        // saved for an unrelated edit rather than trapping the operator (no auto-fix, no migration).
        boolean keySegmentChanged = id == null || !keySegment.equals(mapping.getKeySegment());
        if (keySegmentChanged) {
            for (SummitPlanTemplateMap other : others) {
                if (keySegment.equalsIgnoreCase(other.getKeySegment())) {
                    session.setAttribute(FLASH_ERROR, "Key segment '" + keySegment + "' would collide:"
                            + " row " + other.getId() + " (service item " + other.getServiceItemId()
                            + ", seq " + other.getSeq() + ", label '" + effectiveLabel(other) + "',"
                            + (other.isActive() ? " ACTIVE" : " INACTIVE") + ") already holds it."
                            + " Both rows would compose the same Import Plan ID -- Summit's upsert"
                            + " key -- so the second plan would silently overwrite the first in"
                            + " Summit. Key segments must be unique within your PSP, inactive rows"
                            + " included.");
                    return;
                }
            }
        }

        // V3 -- the emitted Plan Name (label, falling back to key segment) unique within the PSP.
        // File 2's results correlate on Plan Name with no row number, so duplicates make result
        // lines unattributable; the emitter refuses at export time and this makes that
        // unreachable. Same edit-only-when-changed rule as V2.
        String newEffectiveLabel = label.isEmpty() ? keySegment : label;
        boolean labelChanged = id == null || !newEffectiveLabel.equals(effectiveLabel(mapping));
        if (labelChanged) {
            for (SummitPlanTemplateMap other : others) {
                if (newEffectiveLabel.equalsIgnoreCase(effectiveLabel(other))) {
                    session.setAttribute(FLASH_ERROR, "Plan Name '" + newEffectiveLabel + "' is"
                            + " already used by row " + other.getId() + " (service item "
                            + other.getServiceItemId() + ", seq " + other.getSeq() + ", key segment '"
                            + other.getKeySegment() + "'," + (other.isActive() ? " ACTIVE" : " INACTIVE")
                            + "). Summit's results file for this template correlates on Plan Name"
                            + " and carries no row number, so two plans sharing one could not be"
                            + " told apart. Labels (or the key segment a blank label falls back to)"
                            + " must be unique within your PSP.");
                    return;
                }
            }
        }

        mapping.setServiceItemId(serviceItemId);
        mapping.setTemplateId(templateId);
        mapping.setKeySegment(keySegment);
        mapping.setLabel(label.isEmpty() ? null : label);
        mapping.setSortOrder(sortOrder);
        mapping.setActive(active);
        mapping.setSeq((short) (int) seq);
        mapping.setEffectiveDateRule(effectiveDateRule);
        mapping.setOffsetMonths(offsetMonths);
        mapping.setPlanYearOffsetYears(planYearOffsetYears);

        if (id == null) {
            SummitPlanTemplateMapDAO.insert(em, mapping);
            session.setAttribute(FLASH_MESSAGE, "Mapping added for service item " + serviceItemId + ".");
        } else {
            SummitPlanTemplateMapDAO.update(em, mapping);
            session.setAttribute(FLASH_MESSAGE, "Mapping " + id + " saved.");
        }
    }

    private void delete(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Long id = parseLongOrNull(request.getParameter("id"));
        if (id == null) {
            session.setAttribute(FLASH_ERROR, "No mapping was identified to remove.");
            return;
        }
        SummitPlanTemplateMap mapping = SummitPlanTemplateMapDAO.findById(em, id);
        if (mapping == null || !pspId.equals(mapping.getPspId())) {
            session.setAttribute(FLASH_ERROR, "That mapping no longer exists, or belongs to another"
                    + " PSP. Nothing was removed.");
            return;
        }
        SummitPlanTemplateMapDAO.delete(em, id);
        session.setAttribute(FLASH_MESSAGE, "Mapping " + id + " removed. Any Summit plan already"
                + " created from it still exists in Summit — this only stops future exports"
                + " emitting it.");
    }

    // ── Reads ─────────────────────────────────────────────────────────

    /**
     * The PSP's Setup-category service items, id-bearing, for the picker.
     * <p>
     * Follows {@code QuestionnaireManager25.getActiveServiceItems}'s PSP-scoped, suppressed-false
     * filter, narrowed to the Setup category because that is the only category an
     * {@code ApplicationModule} can carry.
     */
    private List<ServiceItem> loadSetupServiceItems(EntityManager em, Long pspId) {
        try {
            return em.createQuery(
                            "SELECT si FROM ServiceItem si " +
                            "WHERE si.psp.id = :pspId AND si.suppressed = false " +
                            "AND si.activityCategory.id = :categoryId " +
                            "ORDER BY si.description, si.id", ServiceItem.class)
                    .setParameter("pspId", pspId)
                    .setParameter("categoryId", SETUP_ACTIVITY_CATEGORY)
                    .getResultList();
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code RateCacheAdmin.isAuthorized} — the gate is copied, not invented. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    /**
     * The session's PSP id, or null. Same walk {@code AgencyAction} and {@code PspDashboardHome}
     * use; null is handled by the caller rather than thrown.
     */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Display-only, for {@code created_by}. Null when the session cannot name anyone. */
    private static String resolveCurrentUserName(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        return local.getCurrentPerson().getFullName();
    }

    /**
     * ⚠️ <b>A deliberate duplicate of {@code SummitPlanTemplateResolver.keySegmentRejection}</b>,
     * which is {@code private} and lives in a class this run may not edit. The rule was read from
     * that method rather than recalled, and the two must not drift: a segment this screen accepts
     * but the resolver rejects would be saved and then silently skipped at export time, costing a
     * plan with no message anywhere. Unifying them is filed as <b>T204</b>.
     */
    private static String keySegmentRejection(String keySegment) {
        if (keySegment == null || keySegment.isEmpty()) {
            return "blank key segment";
        }
        if (keySegment.contains("|") || keySegment.chars().anyMatch(Character::isWhitespace)) {
            return "key segment '" + keySegment + "' contains a pipe or whitespace";
        }
        if (!keySegment.matches("[A-Za-z0-9]+")) {
            return "key segment '" + keySegment + "' must be letters and digits only — it composes the "
                    + "Import Plan ID, and the 125 PI Contributions import rejects any other character";
        }
        return null;
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Summit Plan Templates");
        request.setAttribute("pageIcon", "bi-diagram-2");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** The Plan Name a row emits: its label, or its key segment when the label is blank (the resolver's rule). */
    private static String effectiveLabel(SummitPlanTemplateMap row) {
        String label = row.getLabel() == null ? "" : row.getLabel().trim();
        return label.isEmpty() ? (row.getKeySegment() == null ? "" : row.getKeySegment().trim()) : label;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
