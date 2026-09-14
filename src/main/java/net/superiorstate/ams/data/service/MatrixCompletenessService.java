package net.superiorstate.ams.data.service;

import net.superiorstate.ams.model.market.EmployerParticipant;
import net.superiorstate.ams.model.market.EnrollmentMatrixEntry;
import net.superiorstate.ams.model.market.EnrollmentMatrixParticipant;
import net.superiorstate.ams.model.market.PayrollFrequency;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S58-P7 — for one enrollment matrix, which (participant, leg) cells are incomplete and why, and
 * a per-participant status the agent view and the PSP view both render.
 * <p>
 * ⚠️ <b>The cell rule is {@code SummitExportServlet}'s completeness gate, copied exactly, not a
 * second definition of done</b> (that file is fenced; TA-24 registers that the rule now lives in
 * two places and the exporter should later delegate here):
 * <ul>
 *   <li>a <b>declined</b> entry is satisfied (the waiver);</li>
 *   <li>otherwise the leg's {@code enrollment_amount_mode} names the required value —
 *       {@code TIER} → {@code tier_name} non-blank; {@code ANNUAL_ELECTION} or
 *       {@code MONTHLY_PREMIUM} → {@code amount} non-null; any other mode → never satisfied
 *       ({@link #isModeValuePopulated}).</li>
 * </ul>
 * Two places where this view is <i>stricter</i> than the exporter, both deliberate and both in
 * the direction "complete here ⇒ accepted there": a participant with no header row at all, and a
 * leg with no entry row, are invisible to the exporter (silently absent from the file) but are
 * reported here as NOT_STARTED / a gap, because an agent must see them as work to do.
 * <p>
 * <b>Needs PSP confirmation is derived, never stored</b>: the participant's
 * {@code payroll_frequency} is {@link PayrollFrequency#OTHER_CUSTOM} or
 * {@link PayrollFrequency#OTHER_NOT_IMPORTABLE}. PSP resolving it means selecting a real
 * schedule, which clears the condition by itself. A participant in that state is not
 * {@code COMPLETE}, however full their cells are. The boolean is also exposed independently of
 * status so the PSP page can mark a PARTIAL participant that carries a sentinel.
 * <p>
 * Status precedence: {@code LOCKED} (PSP has frozen the row; the agent cannot act) →
 * {@code NOT_STARTED} (no header row) → {@code PARTIAL} (any cell gap) → {@code DECLINED} (every
 * leg declined) → {@code NEEDS_PSP_CONFIRMATION} → {@code COMPLETE}. A matrix with no legs has
 * no cells to fill, so a saved participant is COMPLETE (or NEEDS_PSP_CONFIRMATION) — the same
 * matrix the exporter would refuse on zero rows, which is a plan-configuration problem, not an
 * entry problem.
 * <p>
 * Plain classes with bean getters (not records) because the JSPs read them through EL.
 */
public final class MatrixCompletenessService {

    private MatrixCompletenessService() {}

    public enum ParticipantStatus {
        NOT_STARTED("Not started"),
        PARTIAL("Partial"),
        COMPLETE("Complete"),
        NEEDS_PSP_CONFIRMATION("Administrator to finish"),   // S58-P8: agent-facing label; the PSP page uses its own literal text
        /** S58-P9 — stored payroll schedule is not enrollment-approved (derived from the row, never stored). Same family as NEEDS_PSP_CONFIRMATION: the administrator acts, the agent did nothing wrong. */
        SCHEDULE_NOT_APPROVED("Administrator to update schedule"),
        DECLINED("Declined"),
        LOCKED("Locked");

        private final String label;
        ParticipantStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** One incomplete (participant, leg) cell and the field it is missing. */
    public static final class Gap {
        private final Long participantId;
        private final Long legId;
        private final String legLabel;
        private final String field;     // "amount" | "tier"
        private final String message;   // human sentence for the page

        Gap(Long participantId, Long legId, String legLabel, String field, String message) {
            this.participantId = participantId;
            this.legId = legId;
            this.legLabel = legLabel;
            this.field = field;
            this.message = message;
        }
        public Long getParticipantId() { return participantId; }
        public Long getLegId() { return legId; }
        public String getLegLabel() { return legLabel; }
        public String getField() { return field; }
        public String getMessage() { return message; }
    }

    public static final class ParticipantAssessment {
        private final Long participantId;
        private final ParticipantStatus status;
        private final boolean needsPspConfirmation;
        private final boolean scheduleNotApproved;
        private final boolean locked;
        private final List<Gap> gaps;

        ParticipantAssessment(Long participantId, ParticipantStatus status, boolean needsPspConfirmation,
                              boolean scheduleNotApproved, boolean locked, List<Gap> gaps) {
            this.participantId = participantId;
            this.status = status;
            this.needsPspConfirmation = needsPspConfirmation;
            this.scheduleNotApproved = scheduleNotApproved;
            this.locked = locked;
            this.gaps = Collections.unmodifiableList(gaps);
        }
        public Long getParticipantId() { return participantId; }
        public ParticipantStatus getStatus() { return status; }
        /** The enum name, for EL comparisons that should not depend on enum coercion. */
        public String getStatusCode() { return status.name(); }
        public String getStatusLabel() { return status.getLabel(); }
        public boolean isNeedsPspConfirmation() { return needsPspConfirmation; }
        /** S58-P9 — the stored payroll code resolves to no enrollment-approved row (or to none at all). */
        public boolean isScheduleNotApproved() { return scheduleNotApproved; }
        public boolean isLocked() { return locked; }
        public List<Gap> getGaps() { return gaps; }
        /** Nothing left for the agent to do: COMPLETE, DECLINED or LOCKED. */
        public boolean isDone() {
            return status == ParticipantStatus.COMPLETE
                    || status == ParticipantStatus.DECLINED
                    || status == ParticipantStatus.LOCKED;
        }
    }

    public static final class MatrixAssessment {
        private final Map<Long, ParticipantAssessment> byParticipantId;
        private final int totalCount;
        private final int completeCount;
        private final int doneCount;
        private final int needsPspConfirmationCount;
        private final int scheduleNotApprovedCount;

        MatrixAssessment(Map<Long, ParticipantAssessment> byParticipantId) {
            this.byParticipantId = Collections.unmodifiableMap(byParticipantId);
            int total = 0, complete = 0, done = 0, needs = 0, notApproved = 0;
            for (ParticipantAssessment pa : byParticipantId.values()) {
                total++;
                if (pa.getStatus() == ParticipantStatus.COMPLETE) complete++;
                if (pa.isDone()) done++;
                if (pa.isNeedsPspConfirmation()) needs++;
                if (pa.isScheduleNotApproved()) notApproved++;
            }
            this.totalCount = total;
            this.completeCount = complete;
            this.doneCount = done;
            this.needsPspConfirmationCount = needs;
            this.scheduleNotApprovedCount = notApproved;
        }
        public Map<Long, ParticipantAssessment> getByParticipantId() { return byParticipantId; }
        public int getTotalCount() { return totalCount; }
        /** Strictly COMPLETE participants. */
        public int getCompleteCount() { return completeCount; }
        /** COMPLETE + DECLINED + LOCKED — what the agent's progress bar counts. */
        public int getDoneCount() { return doneCount; }
        public int getNeedsPspConfirmationCount() { return needsPspConfirmationCount; }
        public int getScheduleNotApprovedCount() { return scheduleNotApprovedCount; }
    }

    /**
     * Verbatim from {@code SummitExportServlet.isModeValuePopulated} (S56-C). Keep identical.
     */
    public static boolean isModeValuePopulated(SummitPlanTemplateMap leg, EnrollmentMatrixEntry entry) {
        if (leg == null || entry == null) return false;
        String mode = leg.getEnrollmentAmountMode();
        if ("TIER".equals(mode)) return trimToNull(entry.getTierName()) != null;
        if ("ANNUAL_ELECTION".equals(mode) || "MONTHLY_PREMIUM".equals(mode)) return entry.getAmount() != null;
        return false;
    }

    /**
     * S58-P9 — a stored payroll code the agent could not have chosen fresh: non-null, not a
     * sentinel, and absent from the enrollment-approved set (a row that was un-approved,
     * deactivated, or hand-created and never approved — {@code BIWEEKLY24} is the live case).
     */
    public static boolean isStoredScheduleNotApproved(String code, Set<String> approvedPayrollCodes) {
        if (code == null || code.isBlank()) return false;
        if (isSentinelPayrollFrequency(code)) return false;
        return approvedPayrollCodes == null || !approvedPayrollCodes.contains(code);
    }

    public static boolean isSentinelPayrollFrequency(String code) {
        return PayrollFrequency.OTHER_CUSTOM.equals(code) || PayrollFrequency.OTHER_NOT_IMPORTABLE.equals(code);
    }

    /**
     * Assesses the matrix as the page loaded it — the same four collections
     * {@code EnrollmentMatrixServlet.renderMatrix} already builds, so no second query pass.
     *
     * @param roster                   participants in page order
     * @param legs                     the setup's legs in page order
     * @param headersByParticipant     {@code participant.id → header row} (absent = never saved)
     * @param entriesByParticipantAndLeg {@code "<headerId>_<legId>" → entry row}
     */
    public static MatrixAssessment assess(List<EmployerParticipant> roster,
                                          List<SummitPlanTemplateMap> legs,
                                          Map<Long, EnrollmentMatrixParticipant> headersByParticipant,
                                          Map<String, EnrollmentMatrixEntry> entriesByParticipantAndLeg,
                                          Set<String> approvedPayrollCodes) {
        Map<Long, ParticipantAssessment> out = new LinkedHashMap<>();
        Set<String> approved = approvedPayrollCodes == null ? Set.of() : approvedPayrollCodes;
        if (roster == null) return new MatrixAssessment(out);
        List<SummitPlanTemplateMap> safeLegs = legs == null ? List.of() : legs;

        for (EmployerParticipant participant : roster) {
            Long pid = participant.getId();
            EnrollmentMatrixParticipant header = headersByParticipant == null ? null : headersByParticipant.get(pid);
            boolean locked = header != null && header.isEntryLocked();
            boolean needsPsp = header != null && isSentinelPayrollFrequency(header.getPayrollFrequency());
            // S58-P9 -- derived: a stored, non-sentinel code that is not enrollment-approved (or
            // resolves to no row at all). Never stored; selecting an approved schedule clears it.
            boolean notApproved = header != null && isStoredScheduleNotApproved(header.getPayrollFrequency(), approved);

            if (locked) {
                out.put(pid, new ParticipantAssessment(pid, ParticipantStatus.LOCKED, needsPsp, notApproved, true, new ArrayList<>()));
                continue;
            }
            if (header == null) {
                out.put(pid, new ParticipantAssessment(pid, ParticipantStatus.NOT_STARTED, false, false, false,
                        gapsForUnstarted(pid, safeLegs)));
                continue;
            }

            List<Gap> gaps = new ArrayList<>();
            int declinedLegs = 0;
            for (SummitPlanTemplateMap leg : safeLegs) {
                EnrollmentMatrixEntry entry = entriesByParticipantAndLeg == null ? null
                        : entriesByParticipantAndLeg.get(header.getId() + "_" + leg.getId());
                if (entry != null && entry.isDeclined()) { declinedLegs++; continue; }
                if (isModeValuePopulated(leg, entry)) continue;
                gaps.add(gapFor(pid, leg));
            }

            ParticipantStatus status;
            if (!gaps.isEmpty()) {
                status = ParticipantStatus.PARTIAL;
            } else if (!safeLegs.isEmpty() && declinedLegs == safeLegs.size()) {
                status = ParticipantStatus.DECLINED;
            } else if (needsPsp) {
                status = ParticipantStatus.NEEDS_PSP_CONFIRMATION;
            } else if (notApproved) {
                status = ParticipantStatus.SCHEDULE_NOT_APPROVED;
            } else {
                status = ParticipantStatus.COMPLETE;
            }
            out.put(pid, new ParticipantAssessment(pid, status, needsPsp, notApproved, false, gaps));
        }
        return new MatrixAssessment(out);
    }

    private static List<Gap> gapsForUnstarted(Long pid, List<SummitPlanTemplateMap> legs) {
        List<Gap> gaps = new ArrayList<>();
        for (SummitPlanTemplateMap leg : legs) gaps.add(gapFor(pid, leg));
        return gaps;
    }

    private static Gap gapFor(Long pid, SummitPlanTemplateMap leg) {
        String label = trimToNull(leg.getLabel());
        if (label == null) label = leg.getKeySegment();
        String mode = leg.getEnrollmentAmountMode();
        if ("TIER".equals(mode)) {
            return new Gap(pid, leg.getId(), label, "tier", label + ": choose a coverage level or mark declined");
        }
        if ("MONTHLY_PREMIUM".equals(mode)) {
            return new Gap(pid, leg.getId(), label, "amount", label + ": enter the monthly premium or mark declined");
        }
        if ("ANNUAL_ELECTION".equals(mode)) {
            return new Gap(pid, leg.getId(), label, "amount", label + ": enter the annual election or mark declined");
        }
        // S58-P8: agent-facing wording -- the mode string is internal and is not shown.
        return new Gap(pid, leg.getId(), label, "amount", label
                + ": this plan can't be completed here — ask your plan administrator");
    }

    private static String trimToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
