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
 * {@code uq_summit_plan_template_map_psp_service} does not consider {@code is_active}, so an
 * inactive row still occupies its (PSP, ServiceItem) pair and blocks a new one. Hiding inactive
 * rows would leave an admin staring at an empty list while every add failed — precisely the trap
 * T202 was filed against.
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
            // Never swallowed. The pre-check in save() handles the ordinary duplicate case with a
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
                    + " pipe-delimited file, so it may not contain a pipe or any whitespace.");
            return;
        }
        if (sortOrder == null) sortOrder = 0;

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
            // ⚠️ Pre-check the unique constraint so the rejection can be explained in words. The
            // constraint ignores is_active, so an INACTIVE row blocks a new one just as firmly --
            // which is exactly the case an admin cannot guess at. Nothing is deleted to make room.
            SummitPlanTemplateMap existing =
                    SummitPlanTemplateMapDAO.findByPspAndServiceItem(em, pspId, serviceItemId);
            if (existing != null) {
                session.setAttribute(FLASH_ERROR, "Service item " + serviceItemId
                        + " is already mapped by row " + existing.getId() + " (template "
                        + existing.getTemplateId() + ", key segment '" + existing.getKeySegment()
                        + "', currently " + (existing.isActive() ? "ACTIVE" : "INACTIVE")
                        + "). One Summit plan per service item is enforced by the database and does"
                        + " not consider the active flag, so an inactive row still holds the slot."
                        + " Edit that row rather than adding a second one.");
                return;
            }
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

        mapping.setServiceItemId(serviceItemId);
        mapping.setTemplateId(templateId);
        mapping.setKeySegment(keySegment);
        mapping.setLabel(label.isEmpty() ? null : label);
        mapping.setSortOrder(sortOrder);
        mapping.setActive(active);

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
        return null;
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Summit Plan Templates");
        request.setAttribute("pageIcon", "bi-diagram-2");
        request.getRequestDispatcher(VIEW).forward(request, response);
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
