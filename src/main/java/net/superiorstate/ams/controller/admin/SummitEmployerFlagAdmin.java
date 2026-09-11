package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.SummitServiceItemFlagsDAO;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.SummitServiceItemFlags;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * T238 part 1 / D46 — PSP Admin screen for the Summit employer administration flag mapping (V101):
 * which of the four Employer Demographic Boolean elements (CDH, COBRA, Retiree Billing, Direct
 * Bill) an elected {@code ServiceItem} turns on. Follows {@code SummitPlanTemplateAdmin} exactly —
 * same gate, same session-PSP resolution, same {@code ServiceItem} ownership check, same
 * add/edit/delete shape, same page layout — because s47b Q10 established there is no other working
 * kind-marker for a Setup {@code ServiceItem}, and V095/{@code SummitPlanTemplateAdmin} already
 * solved the same problem once.
 * <p>
 * <b>Gate: PSP admin only.</b> {@link #isAuthorized(HttpSession)} is copied verbatim from
 * {@code SummitPlanTemplateAdmin}, itself copied verbatim from {@code RateCacheAdmin}. No nav entry
 * is added here — {@code navbar25.jsp} is untouched by this build.
 * <p>
 * <b>Changes no export behaviour.</b> {@code SummitEmployerFlagResolver} reads this table, and
 * nothing calls the resolver yet — the file 1 emitter change is a separate, later build, blocked
 * on SDX-27.
 * <p>
 * ⚠️ <b>The {@code ServiceItem} picker shows the id, not just the description</b> — same reason
 * {@code SummitPlanTemplateAdmin} shows it: {@code ServiceItem.code} is null on every real
 * installation (T189) and {@code description} is a copy of a LOS's {@code shortText} taken at
 * creation and never updated on rename (T190), so two items can read identically and the id is the
 * only thing that distinguishes them.
 */
@WebServlet(name = "SummitEmployerFlagAdmin", value = "/SummitEmployerFlagAdmin")
public class SummitEmployerFlagAdmin extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/a/admin/summitEmployerFlagAdmin25.jsp";

    /** Flash attribute names, following {@code SummitPlanTemplateAdmin}'s naming convention. */
    private static final String FLASH_MESSAGE = "summitEmployerFlagMessage";
    private static final String FLASH_ERROR = "summitEmployerFlagError";

    /** Same category restriction as {@code SummitPlanTemplateAdmin} — only Setup-category items are ever elected. */
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

            List<SummitServiceItemFlags> mappings = SummitServiceItemFlagsDAO.findAllByPspId(em, pspId);
            request.setAttribute("mappings", mappings);
            request.setAttribute("serviceItems", loadSetupServiceItems(em, pspId));
            request.setAttribute("serviceItemDescriptions", loadServiceItemDescriptions(em, mappings));

            // Resolved here rather than in EL, same reasoning SummitPlanTemplateAdmin gives (T199,
            // no JSP precompiler): a non-numeric editId would 500 on click rather than at build time.
            Long editId = parseLongOrNull(request.getParameter("editId"));
            SummitServiceItemFlags editing = null;
            if (editId != null) {
                for (SummitServiceItemFlags mapping : mappings) {
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
                response.sendRedirect(request.getContextPath() + "/SummitEmployerFlagAdmin");
                return;
            }

            String action = request.getParameter("action");
            if ("save".equals(action)) {
                save(request, session, em, pspId);
            } else if ("delete".equals(action)) {
                delete(request, session, em, pspId);
            }
        } catch (RuntimeException e) {
            // Never swallowed, same contract SummitPlanTemplateAdmin documents: the pre-check in
            // save() handles the ordinary duplicate case, but it cannot close the race between two
            // admins, and a constraint violation must reach the operator.
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/SummitEmployerFlagAdmin");
    }

    // ── Actions ───────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Long id = parseLongOrNull(request.getParameter("id"));
        Integer serviceItemId = parseIntOrNull(request.getParameter("serviceItemId"));
        boolean enableCdh = request.getParameter("enableCdh") != null;
        boolean enableCobra = request.getParameter("enableCobra") != null;
        boolean enableRetireeBilling = request.getParameter("enableRetireeBilling") != null;
        boolean enableDirectBill = request.getParameter("enableDirectBill") != null;

        if (serviceItemId == null || serviceItemId <= 0) {
            session.setAttribute(FLASH_ERROR, "Choose a service item.");
            return;
        }

        // The service item must belong to this PSP — same reasoning SummitPlanTemplateAdmin gives:
        // without this an admin could map another installation's item by typing its id.
        ServiceItem serviceItem = em.find(ServiceItem.class, serviceItemId);
        if (serviceItem == null || serviceItem.getPsp() == null
                || !pspId.equals(serviceItem.getPsp().getId())) {
            session.setAttribute(FLASH_ERROR, "Service item " + serviceItemId
                    + " does not belong to your PSP.");
            return;
        }

        SummitServiceItemFlags flags;
        if (id == null) {
            SummitServiceItemFlags existing =
                    SummitServiceItemFlagsDAO.findByPspAndServiceItem(em, pspId, serviceItemId);
            if (existing != null) {
                session.setAttribute(FLASH_ERROR, "This service item already has a flag mapping"
                        + " — edit it instead.");
                return;
            }
            flags = new SummitServiceItemFlags();
            flags.setPspId(pspId);
            flags.setServiceItemId(serviceItemId);
            flags.setCreatedAt(LocalDateTime.now());
            flags.setCreatedBy(resolveCurrentUserName(request));
        } else {
            flags = SummitServiceItemFlagsDAO.findById(em, id);
            if (flags == null || !pspId.equals(flags.getPspId())) {
                session.setAttribute(FLASH_ERROR, "That mapping no longer exists, or belongs to"
                        + " another PSP. Nothing was saved.");
                return;
            }
            flags.setUpdatedAt(LocalDateTime.now());
            flags.setUpdatedBy(resolveCurrentUserName(request));
        }

        flags.setEnableCdh(enableCdh);
        flags.setEnableCobra(enableCobra);
        flags.setEnableRetireeBilling(enableRetireeBilling);
        flags.setEnableDirectBill(enableDirectBill);

        if (id == null) {
            SummitServiceItemFlagsDAO.insert(em, flags);
            session.setAttribute(FLASH_MESSAGE, "Flag mapping added for service item " + serviceItemId + ".");
        } else {
            SummitServiceItemFlagsDAO.update(em, flags);
            session.setAttribute(FLASH_MESSAGE, "Flag mapping " + id + " saved.");
        }
    }

    private void delete(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Long id = parseLongOrNull(request.getParameter("id"));
        if (id == null) {
            session.setAttribute(FLASH_ERROR, "No mapping was identified to remove.");
            return;
        }
        SummitServiceItemFlags flags = SummitServiceItemFlagsDAO.findById(em, id);
        if (flags == null || !pspId.equals(flags.getPspId())) {
            session.setAttribute(FLASH_ERROR, "That mapping no longer exists, or belongs to another"
                    + " PSP. Nothing was removed.");
            return;
        }
        SummitServiceItemFlagsDAO.delete(em, id);
        session.setAttribute(FLASH_MESSAGE, "Flag mapping " + id + " removed.");
    }

    // ── Reads ─────────────────────────────────────────────────────────

    /** Same PSP-scoped, suppressed-false, Setup-category filter {@code SummitPlanTemplateAdmin} uses. */
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

    /**
     * Description text for each mapped row's service item, keyed by mapping id, for the listing
     * table — {@code SummitServiceItemFlags} carries only the scalar id, and the table's "Service
     * item" column needs the description alongside it, the same way
     * {@code summitPlanTemplateAdmin25.jsp} shows the id alone but this screen shows both.
     */
    private java.util.Map<Long, String> loadServiceItemDescriptions(EntityManager em, List<SummitServiceItemFlags> mappings) {
        java.util.Map<Long, String> descriptions = new java.util.LinkedHashMap<>();
        for (SummitServiceItemFlags mapping : mappings) {
            String description = null;
            try {
                ServiceItem si = em.find(ServiceItem.class, mapping.getServiceItemId());
                if (si != null) description = si.getDescription();
            } catch (RuntimeException ignore) { /* leave null, JSP falls back to the id alone */ }
            descriptions.put(mapping.getId(), description);
        }
        return descriptions;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code SummitPlanTemplateAdmin.isAuthorized}, itself verbatim from {@code RateCacheAdmin}. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    /** Same walk {@code SummitPlanTemplateAdmin.resolveCurrentPspId} uses. */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    /** Display-only, for {@code created_by}/{@code updated_by}. Null when the session cannot name anyone. */
    private static String resolveCurrentUserName(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        return local.getCurrentPerson().getFullName();
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Summit Employer Flags");
        request.setAttribute("pageIcon", "bi-toggles");
        request.getRequestDispatcher(VIEW).forward(request, response);
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
