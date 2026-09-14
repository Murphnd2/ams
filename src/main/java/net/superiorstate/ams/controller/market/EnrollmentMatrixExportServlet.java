package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.EmployerParticipantDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixEntryDAO;
import net.superiorstate.ams.data.dao.EnrollmentMatrixParticipantDAO;
import net.superiorstate.ams.data.dao.SummitPlanTemplateMapDAO;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.market.EnrollmentMatrix;
import net.superiorstate.ams.model.market.EnrollmentMatrixEntry;
import net.superiorstate.ams.model.market.EnrollmentMatrixParticipant;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationModule;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * S58-P1 — {@code GET /EnrollmentMatrixExport?setupId=N}: the enrollment matrix screen as an
 * {@code .xlsx}, one row per rendered (participant, leg) pair, in the order the page renders
 * them. Export only; nothing here is an import and nothing is pushed anywhere.
 * <p>
 * <b>Gate: PSP admin only</b> — {@link #isAuthorized(HttpSession)} is copied verbatim from
 * {@code EnrollmentMatrixServlet}, which itself copied it from {@code SummitPlanTemplateAdmin}
 * / {@code RateCacheAdmin}. That method is private to its servlet, so it is replicated rather
 * than extracted (no refactoring of that file this run).
 * <p>
 * <b>Read-only, deliberately.</b> {@code EnrollmentMatrixServlet.doGet} lazily INSERTs the
 * {@code enrollment_matrix} row on first open ({@code findOrCreateMatrix}); this servlet calls
 * {@link EnrollmentMatrixDAO#findBySetupId} instead and treats a null matrix as "nothing saved
 * yet" — every participant × leg row still emits, with the key columns populated and the
 * editable cells empty. That blank sheet is the primary use case.
 * <p>
 * <b>Rows and legs</b> come from the same chain the page uses: {@code Setup → Application →
 * Proposal}; roster via {@link EmployerParticipantDAO#findByProspectId} (lastName, firstName,
 * id); legs via {@link SummitPlanTemplateMapDAO#findActiveByPspId} filtered to the elected
 * service items and {@code enrollmentAmountMode != "NONE"} — {@link #loadElectedServiceItemIds}
 * duplicates the JPQL the page (and {@code SummitExportServlet}) already carry for that, since
 * both copies are private to files this run may not touch.
 * <p>
 * <b>Columns</b> (see TA-18): four leading keys, then the page's read-only display columns,
 * then the editable ones. Editable {@code <select>} cells carry the value the page <i>posts</i>
 * (the payroll-frequency code, the tier's {@code summitTierId}), not the display label.
 * {@code Leg mode} is the leg's stored {@code enrollmentAmountMode} — the attribute the page
 * branches on to decide whether to render "Monthly premium", "Annual election" or "Tier"; it is
 * emitted so a blank row says which of those three cells applies.
 * <p>
 * <b>Filename</b> {@code enrollment-matrix_<setupId>_<yyyyMMdd-HHmm>.xlsx} — a different
 * extension and a different stamp shape from the {@code {template}_{yyyyMMddHHmmss}.txt} Summit
 * push files, and it never passes through {@code SummitImportTemplateResolver}.
 */
@WebServlet(name = "EnrollmentMatrixExportServlet", value = "/EnrollmentMatrixExport")
public class EnrollmentMatrixExportServlet extends HttpServlet {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Header row, in emit order. Leading keys, then read-only display, then editable. */
    private static final String[] HEADERS = {
            "entry_id",
            "participant_id",
            "leg",
            "participant_tpa_custom_id",
            "Participant",
            "Leg",
            "Leg mode",
            "Locked",
            "Payroll frequency",
            "Custom schedule name",
            "Monthly premium",
            "Annual election",
            "Tier",
            "Declined"
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        Long setupId = parseLongOrNull(request.getParameter("setupId"));
        if (setupId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No setup id was given.");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long pspId = resolveCurrentPspId(request);
            if (pspId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Could not determine your PSP from this session.");
                return;
            }

            Setup setup = em.find(Setup.class, setupId);
            if (setup == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No setup found for id " + setupId + ".");
                return;
            }
            Application application = setup.getApplication();
            if (application == null || application.getProposal() == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Setup " + setupId + " has no linked application.");
                return;
            }
            long proposalId = application.getProposal().getId();
            Prospect prospect = application.getProposal().getProspect();

            // Find only -- never create. A setup whose matrix has never been opened or saved
            // has no row here, and that is the blank-sheet case this export exists for.
            EnrollmentMatrix matrix = EnrollmentMatrixDAO.findBySetupId(em, setupId);

            List<EmployerParticipant> roster = prospect == null
                    ? List.of() : EmployerParticipantDAO.findByProspectId(em, prospect.getId());

            Set<Integer> electedServiceItemIds = loadElectedServiceItemIds(em, proposalId);
            List<SummitPlanTemplateMap> legs = new ArrayList<>();
            for (SummitPlanTemplateMap candidate : SummitPlanTemplateMapDAO.findActiveByPspId(em, pspId)) {
                if (electedServiceItemIds.contains(candidate.getServiceItemId())
                        && !"NONE".equals(candidate.getEnrollmentAmountMode())) {
                    legs.add(candidate);
                }
            }

            String tpaPrefix = resolveTpaPrefix();

            byte[] bytes;
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Enrollment Matrix");

                Font bold = workbook.createFont();
                bold.setBold(true);
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFont(bold);

                Row headerRow = sheet.createRow(0);
                for (int c = 0; c < HEADERS.length; c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(HEADERS[c]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 1;
                for (EmployerParticipant participant : roster) {
                    // Header row lookup mirrors the page: only reachable when a matrix exists.
                    EnrollmentMatrixParticipant header = matrix == null ? null
                            : EnrollmentMatrixParticipantDAO.findByMatrixAndParticipant(
                                    em, matrix.getId(), participant.getId());

                    String participantName = nullToEmpty(participant.getLastName())
                            + ", " + nullToEmpty(participant.getFirstName());
                    String participantTpaCustomId = tpaPrefix == null
                            ? "" : tpaPrefix + "-P-" + participant.getId();
                    boolean locked = header != null && header.isEntryLocked();

                    for (SummitPlanTemplateMap leg : legs) {
                        EnrollmentMatrixEntry entry = header == null ? null
                                : EnrollmentMatrixEntryDAO.findByParticipantAndLeg(em, header.getId(), leg.getId());

                        String mode = nullToEmpty(leg.getEnrollmentAmountMode());
                        String legLabel = isBlank(leg.getLabel()) ? nullToEmpty(leg.getKeySegment()) : leg.getLabel();

                        Row row = sheet.createRow(rowIndex++);
                        int c = 0;
                        // -- leading keys
                        setNumericOrBlank(row.createCell(c++), entry == null ? null : entry.getId());
                        setNumericOrBlank(row.createCell(c++), participant.getId());
                        setNumericOrBlank(row.createCell(c++), leg.getId());
                        row.createCell(c++).setCellValue(participantTpaCustomId);
                        // -- read-only display
                        row.createCell(c++).setCellValue(participantName);
                        row.createCell(c++).setCellValue(legLabel);
                        row.createCell(c++).setCellValue(mode);
                        row.createCell(c++).setCellValue(locked ? "Yes" : "");
                        // -- editable, header-level (repeats on every leg row of the participant)
                        row.createCell(c++).setCellValue(header == null ? "" : nullToEmpty(header.getPayrollFrequency()));
                        row.createCell(c++).setCellValue(header == null ? "" : nullToEmpty(header.getCustomScheduleName()));
                        // -- editable, per leg. The amount lands under the label the page
                        //    renders for this leg's mode; the other amount column stays blank.
                        BigDecimal amount = entry == null ? null : entry.getAmount();
                        setDecimalOrBlank(row.createCell(c++), "MONTHLY_PREMIUM".equals(mode) ? amount : null);
                        setDecimalOrBlank(row.createCell(c++), "ANNUAL_ELECTION".equals(mode) ? amount : null);
                        row.createCell(c++).setCellValue(
                                entry == null || !"TIER".equals(mode) ? "" : nullToEmpty(entry.getTierName()));
                        row.createCell(c++).setCellValue(entry != null && entry.isDeclined() ? "Yes" : "");
                    }
                }

                for (int c = 0; c < HEADERS.length; c++) {
                    sheet.autoSizeColumn(c);
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                workbook.write(buffer);
                bytes = buffer.toByteArray();
            }

            String filename = "enrollment-matrix_" + setupId + "_"
                    + LocalDateTime.now().format(FILE_STAMP) + ".xlsx";

            response.setContentType(CONTENT_TYPE_XLSX);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(bytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    // ── Reads ─────────────────────────────────────────────────────────

    /**
     * Duplicates {@code EnrollmentMatrixServlet.loadElectedServiceItemIds} (itself a duplicate of
     * {@code SummitExportServlet.loadElectedServiceItems}) — both private to files this run may
     * not touch. The legs this sheet carries must be exactly the legs the page renders.
     */
    private Set<Integer> loadElectedServiceItemIds(EntityManager em, long proposalId) {
        Query q = em.createQuery(
                "SELECT am FROM ApplicationModule am " +
                "WHERE am.application.proposal.id = :pid");
        q.setParameter("pid", proposalId);
        @SuppressWarnings("unchecked")
        List<ApplicationModule> modules = (List<ApplicationModule>) q.getResultList();
        Set<Integer> ids = new LinkedHashSet<>();
        for (ApplicationModule module : modules) {
            ServiceItem serviceItem = module.getServiceItem();
            if (serviceItem != null) ids.add(serviceItem.getId());
        }
        return ids;
    }

    /**
     * The same trimmed {@code SUMMIT_TPA_ID_PREFIX} {@code SummitExportServlet} composes the
     * Participant TPA Custom ID from ({@code prefix + "-P-" + participant.getId()}). Null when
     * the installation has not configured one — the column then emits empty rather than a
     * half-built id.
     */
    private static String resolveTpaPrefix() {
        String raw = AppConfig.get("SUMMIT_TPA_ID_PREFIX");
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Verbatim from {@code EnrollmentMatrixServlet.isAuthorized}. */
    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    /** Same walk {@code EnrollmentMatrixServlet.resolveCurrentPspId} uses. */
    private static Long resolveCurrentPspId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("local");
        if (!(attribute instanceof AmsDataLocal local)) return null;
        if (local.getCurrentPerson() == null) return null;
        PSP psp = local.getCurrentPerson().getPsp();
        return psp == null ? null : psp.getId();
    }

    private static void setNumericOrBlank(Cell cell, Long value) {
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    private static void setDecimalOrBlank(Cell cell, BigDecimal value) {
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
