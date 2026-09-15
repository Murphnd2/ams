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
import net.superiorstate.ams.data.dao.AuditDeclineEmployerDAO;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.AuditDeclineEmployer;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T237 check #3 — PSP Admin screen for the card-decline employer designation list (V114): which
 * AMS employers {@code CardDeclineCheck} evaluates. Follows {@code SummitEmployerFlagAdmin}
 * exactly — same gate, same session-PSP resolution, same {@code action=save|delete} shape, same
 * ownership check on the target row, same flash messages, same page layout — because that screen
 * already solved "a PSP-scoped list maintained through the UI" once (V101) and there is no reason
 * for a second shape.
 * <p>
 * <b>Gate: PSP admin only.</b> {@link #isAuthorized(HttpSession)} is copied verbatim from
 * {@code SummitEmployerFlagAdmin}, itself from {@code SummitPlanTemplateAdmin} and
 * {@code RateCacheAdmin}. No nav entry — {@code navbar25.jsp} is untouched; the page is reached
 * from a toolbar link on {@code auditCardDeclines25.jsp}, because the designation list is a
 * property of that check, not of the Audit Hub.
 * <p>
 * <b>Add and delete only — there is nothing to edit.</b> A row's presence is the designation
 * (V114); it carries no attributes an admin would change, so this screen has no edit state.
 * <p>
 * <b>Ownership check differs from the precedent in one way, by necessity.</b>
 * {@code SummitEmployerFlagAdmin} verifies the chosen {@code ServiceItem} belongs to the session
 * PSP; {@code Employer} carries no PSP column (it is installation-wide — one PSP per
 * installation), so on <i>save</i> the check is only that the employer exists, and PSP ownership
 * is enforced on the designation row itself on <i>delete</i>, exactly as the precedent does for
 * its own rows.
 * <p>
 * ⚠️ <b>The picker shows the Summit {@code EmployerID} ({@code altId}) alongside the name</b> and
 * flags an employer whose {@code altId} is {@code 0}: such an employer (imported from a J1 file
 * without the {@code EmployerID} column, T265) can be designated but will never match a decline
 * row, and the admin should know that before choosing it.
 */
@WebServlet(name = "AuditDeclineEmployerAdmin", value = "/AuditDeclineEmployerAdmin")
public class AuditDeclineEmployerAdmin extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/a/admin/auditDeclineEmployerAdmin25.jsp";

    /** Flash attribute names, following {@code SummitEmployerFlagAdmin}'s naming convention. */
    private static final String FLASH_MESSAGE = "auditDeclineEmployerMessage";
    private static final String FLASH_ERROR = "auditDeclineEmployerError";

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
                request.setAttribute("designations", List.of());
                request.setAttribute("employerRows", List.of());
                request.setAttribute("employers", List.of());
                forward(request, response);
                return;
            }

            List<AuditDeclineEmployer> designations = AuditDeclineEmployerDAO.findAllByPspId(em, pspId);
            request.setAttribute("designations", designations);
            request.setAttribute("employerRows", loadEmployerRows(em, designations));
            request.setAttribute("employers", loadActiveEmployers(em));

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
                response.sendRedirect(request.getContextPath() + "/AuditDeclineEmployerAdmin");
                return;
            }

            String action = request.getParameter("action");
            if ("save".equals(action)) {
                save(request, session, em, pspId);
            } else if ("delete".equals(action)) {
                delete(request, session, em, pspId);
            }
        } catch (RuntimeException e) {
            // Never swallowed, same contract SummitEmployerFlagAdmin documents: the pre-check in
            // save() handles the ordinary duplicate case, but it cannot close the race between two
            // admins, and a constraint violation must reach the operator.
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/AuditDeclineEmployerAdmin");
    }

    // ── Actions ───────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Integer employerId = parseIntOrNull(request.getParameter("employerId"));
        if (employerId == null || employerId <= 0) {
            session.setAttribute(FLASH_ERROR, "Choose an employer.");
            return;
        }

        // Employer carries no PSP column (installation-wide), so the only thing to verify here is
        // that the id names a real employer — see the class note on the ownership check.
        Employer employer = em.find(Employer.class, employerId);
        if (employer == null) {
            session.setAttribute(FLASH_ERROR, "Employer " + employerId + " does not exist.");
            return;
        }

        AuditDeclineEmployer existing = AuditDeclineEmployerDAO.findByPspAndEmployer(em, pspId, employerId);
        if (existing != null) {
            session.setAttribute(FLASH_ERROR, employer.getEmployerName() + " is already designated.");
            return;
        }

        AuditDeclineEmployer designation = new AuditDeclineEmployer();
        designation.setPspId(pspId);
        designation.setEmployerId(employerId);
        designation.setCreatedAt(LocalDateTime.now());
        designation.setCreatedBy(resolveCurrentUserName(request));

        AuditDeclineEmployerDAO.insert(em, designation);

        String note = employer.getAltId() > 0
                ? ""
                : " ⚠ This employer has no Summit EmployerID in AMS (altId 0) and will not match any decline row until a J1 refresh supplies one.";
        session.setAttribute(FLASH_MESSAGE, employer.getEmployerName() + " designated for card-decline monitoring." + note);
    }

    private void delete(HttpServletRequest request, HttpSession session, EntityManager em, Long pspId) {
        Long id = parseLongOrNull(request.getParameter("id"));
        if (id == null) {
            session.setAttribute(FLASH_ERROR, "No designation was identified to remove.");
            return;
        }
        AuditDeclineEmployer designation = AuditDeclineEmployerDAO.findById(em, id);
        if (designation == null || !pspId.equals(designation.getPspId())) {
            session.setAttribute(FLASH_ERROR, "That designation no longer exists, or belongs to another"
                    + " PSP. Nothing was removed.");
            return;
        }
        AuditDeclineEmployerDAO.delete(em, id);
        session.setAttribute(FLASH_MESSAGE, "Designation " + id + " removed.");
    }

    // ── Reads ─────────────────────────────────────────────────────────

    /** One row of the listing table — the designation paired with its employer's name and Summit id. */
    public static final class EmployerRow {
        private final AuditDeclineEmployer designation;
        private final String employerName;
        private final int altId;

        EmployerRow(AuditDeclineEmployer designation, String employerName, int altId) {
            this.designation = designation;
            this.employerName = employerName;
            this.altId = altId;
        }

        public Long getId() { return designation.getId(); }
        public Integer getEmployerId() { return designation.getEmployerId(); }
        public String getEmployerName() { return employerName; }
        public int getAltId() { return altId; }
        public boolean isUnmatchable() { return altId <= 0; }
        public String getCreatedBy() { return designation.getCreatedBy(); }
    }

    /**
     * Name and Summit id for each designated row, keyed in the designations' own (name) order —
     * {@code AuditDeclineEmployer} carries only the scalar AMS id, and the table needs the name and
     * {@code altId} beside it, the same way {@code SummitEmployerFlagAdmin.loadServiceItemDescriptions}
     * decorates its own rows.
     */
    private List<EmployerRow> loadEmployerRows(EntityManager em, List<AuditDeclineEmployer> designations) {
        List<EmployerRow> rows = new ArrayList<>(designations.size());
        for (AuditDeclineEmployer designation : designations) {
            String name = null;
            int altId = 0;
            try {
                Employer employer = em.find(Employer.class, designation.getEmployerId());
                if (employer != null) {
                    name = employer.getEmployerName();
                    altId = employer.getAltId();
                }
            } catch (RuntimeException ignore) { /* leave null; JSP falls back to the AMS id alone */ }
            rows.add(new EmployerRow(designation, name, altId));
        }
        return rows;
    }

    /** Active AMS employers for the picker, by name. {@code Employer} has no PSP column. */
    private List<Employer> loadActiveEmployers(EntityManager em) {
        try {
            return em.createQuery(
                            "SELECT e FROM Employer e WHERE e.isActive = true " +
                            "ORDER BY e.employerName, e.id", Employer.class)
                    .getResultList();
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code SummitEmployerFlagAdmin.isAuthorized}, itself verbatim from {@code RateCacheAdmin}. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    /** Same walk {@code SummitEmployerFlagAdmin.resolveCurrentPspId} uses. */
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

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Card-Decline Employers");
        request.setAttribute("pageIcon", "bi-credit-card-2-front");
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
