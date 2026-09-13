package net.superiorstate.ams.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.dao.PayrollFrequencyDAO;
import net.superiorstate.ams.model.market.PayrollFrequency;

import java.io.IOException;
import java.util.List;

/**
 * s53c — PSP Admin screen for the payroll-frequency reference registry (V107).
 * <p>
 * V107 created {@code payroll_frequency} — the curated, employer-independent list the
 * enrollment matrix dropdown reads (once the matrix is wired to it, a change out of scope
 * for this run). This screen is the writer. It changes no existing behaviour: nothing yet
 * reads this table, and neither {@code EnrollmentMatrixServlet} nor
 * {@code enrollmentMatrix25.jsp} is touched here.
 * <p>
 * <b>Gate: PSP admin only.</b> {@link #isAuthorized(HttpSession)} is copied verbatim from
 * {@code SummitPlanTemplateAdmin.isAuthorized}, itself copied verbatim from
 * {@code RateCacheAdmin} — the gate is not invented here.
 * <p>
 * <b>URL-only.</b> No nav entry, menu item, or link from any existing page points at
 * {@code /PayrollFrequencyAdmin}. A non-PSP-admin who reaches it is redirected before
 * learning anything about the page.
 * <p>
 * <b>Not PSP-scoped.</b> {@code payroll_frequency} is employer-independent and
 * installation-wide (s53b), so unlike {@code SummitPlanTemplateAdmin} this screen carries
 * no {@code pspId} filter — every PSP admin on the installation sees and edits the same
 * rows.
 * <p>
 * <b>{@code OTHER_CUSTOM} and {@code OTHER_NOT_IMPORTABLE} are rejected as a {@code code}</b>
 * on both create and edit — those are servlet-level sentinels the matrix appends itself
 * ({@link PayrollFrequency#OTHER_CUSTOM}, {@link PayrollFrequency#OTHER_NOT_IMPORTABLE}),
 * never rows in this table.
 * <p>
 * <b>Shape follows {@code SummitPlanTemplateAdmin}</b> (servlet mapping, gate, forward,
 * POST-redirect-GET with session flash attributes, an {@code action} switch over
 * save / delete).
 */
@WebServlet(name = "PayrollFrequencyAdmin", value = "/PayrollFrequencyAdmin")
public class PayrollFrequencyAdmin extends HttpServlet {

    private static final String VIEW = "/WEB-INF/view/a/admin/payrollFrequencyAdmin25.jsp";

    /** Flash attribute names, following {@code SummitPlanTemplateAdmin}'s naming convention. */
    private static final String FLASH_MESSAGE = "payrollFrequencyMessage";
    private static final String FLASH_ERROR = "payrollFrequencyError";

    /**
     * The two servlet-level sentinel codes a row here may never claim. Referenced from
     * {@link PayrollFrequency}, not restated, so the two lists cannot drift.
     */
    private static final List<String> RESERVED_CODES =
            List.of(PayrollFrequency.OTHER_CUSTOM, PayrollFrequency.OTHER_NOT_IMPORTABLE);

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
            List<PayrollFrequency> rows = PayrollFrequencyDAO.findAll(em);
            request.setAttribute("rows", rows);
            request.setAttribute("reservedCodes", RESERVED_CODES);

            // Same not-in-EL-arithmetic reasoning as SummitPlanTemplateAdmin: resolved here
            // rather than coerced in the JSP, so a non-numeric editId falls through to the
            // blank add form instead of surfacing a 500.
            Integer editId = parseIntOrNull(request.getParameter("editId"));
            PayrollFrequency editing = editId == null ? null : PayrollFrequencyDAO.findById(em, editId);
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
            String action = request.getParameter("action");
            if ("save".equals(action)) {
                save(request, session, em);
            } else if ("delete".equals(action)) {
                delete(request, session, em);
            }
        } catch (RuntimeException e) {
            // Never swallowed — a constraint violation (duplicate code, racing admins) must
            // reach the operator rather than a log nobody reads.
            session.setAttribute(FLASH_ERROR, "The change was not saved: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect(request.getContextPath() + "/PayrollFrequencyAdmin");
    }

    // ── Actions ───────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpSession session, EntityManager em) {
        Integer id = parseIntOrNull(request.getParameter("id"));
        String code = trimToEmpty(request.getParameter("code"));
        String label = trimToEmpty(request.getParameter("label"));
        Integer periodsPerYear = parseIntOrNull(request.getParameter("periodsPerYear"));
        String summitScheduleName = trimToEmpty(request.getParameter("summitScheduleName"));
        String applicationValue = trimToEmpty(request.getParameter("applicationValue"));
        boolean enrollmentApproved = request.getParameter("enrollmentApproved") != null;
        boolean active = request.getParameter("active") != null;
        Integer sortOrder = parseIntOrNull(request.getParameter("sortOrder"));

        if (code.isEmpty()) {
            session.setAttribute(FLASH_ERROR, "Code is required.");
            return;
        }
        if (RESERVED_CODES.stream().anyMatch(reserved -> reserved.equalsIgnoreCase(code))) {
            session.setAttribute(FLASH_ERROR, "'" + code + "' is a reserved sentinel the matrix"
                    + " appends itself (" + RESERVED_CODES + ") — it cannot be used as a code here.");
            return;
        }
        if (label.isEmpty()) {
            session.setAttribute(FLASH_ERROR, "Label is required.");
            return;
        }

        PayrollFrequency existingByCode = PayrollFrequencyDAO.findByCode(em, code);
        if (existingByCode != null && (id == null || !existingByCode.getId().equals(id))) {
            session.setAttribute(FLASH_ERROR, "Code '" + code + "' is already used by row "
                    + existingByCode.getId() + " ('" + existingByCode.getLabel() + "'). Codes must"
                    + " be unique.");
            return;
        }

        PayrollFrequency row;
        if (id == null) {
            row = new PayrollFrequency();
        } else {
            row = PayrollFrequencyDAO.findById(em, id);
            if (row == null) {
                session.setAttribute(FLASH_ERROR, "That row no longer exists. Nothing was saved.");
                return;
            }
        }

        row.setCode(code);
        row.setLabel(label);
        row.setPeriodsPerYear(periodsPerYear);
        row.setSummitScheduleName(summitScheduleName.isEmpty() ? null : summitScheduleName);
        row.setApplicationValue(applicationValue.isEmpty() ? null : applicationValue);
        row.setEnrollmentApproved(enrollmentApproved);
        row.setActive(active);
        row.setSortOrder(sortOrder == null ? 0 : sortOrder);

        PayrollFrequencyDAO.save(em, row);
        session.setAttribute(FLASH_MESSAGE, id == null
                ? "Payroll frequency '" + code + "' added."
                : "Payroll frequency " + id + " saved.");
    }

    private void delete(HttpServletRequest request, HttpSession session, EntityManager em) {
        Integer id = parseIntOrNull(request.getParameter("id"));
        if (id == null) {
            session.setAttribute(FLASH_ERROR, "No row was identified to remove.");
            return;
        }
        PayrollFrequency row = PayrollFrequencyDAO.findById(em, id);
        if (row == null) {
            session.setAttribute(FLASH_ERROR, "That row no longer exists. Nothing was removed.");
            return;
        }
        PayrollFrequencyDAO.delete(em, id);
        session.setAttribute(FLASH_MESSAGE, "Payroll frequency " + id + " ('" + row.getCode() + "') removed.");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code SummitPlanTemplateAdmin.isAuthorized} — the gate is copied, not invented. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Payroll Frequencies");
        request.setAttribute("pageIcon", "bi-calendar-week");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
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
