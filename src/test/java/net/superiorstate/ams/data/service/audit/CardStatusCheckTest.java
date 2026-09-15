package net.superiorstate.ams.data.service.audit;

import net.superiorstate.ams.model.market.AuditFindingAck;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic-export test for {@link CardStatusCheck} (T237 fourth audit check). Exercises the
 * package-private static seams directly ({@link CardStatusCheck#evaluate}, {@code
 * splitByAcknowledgment}, {@code toRows}, {@code parse}, {@code dateOf}, {@code
 * newestMatchingPattern}) rather than the public {@code evaluate(EntityManager, Long)}/{@code
 * readLive} entry points, because neither {@code AppConfig} (loaded once, JVM-wide, from a resolved
 * {@code ssa.properties} path, with no reset/injection seam, and on this build's do-not-touch list)
 * nor {@code SummitSftpService} (opens a real JSch session, no DI seam) can be driven from a unit
 * test in this codebase — there is no {@code src/test} precedent for the sibling checks either. A
 * {@code null} {@code EntityManager} is passed to {@code toRows} throughout: {@code
 * resolveParticipantName}'s own catch-all swallows the resulting {@code NullPointerException}
 * exactly as it would swallow a real lookup miss, so every assertion below still exercises the real
 * production code path, not a stub.
 */
class CardStatusCheckTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);
    private static final Set<Integer> DEFAULT_PROBLEM_IDS = Set.of(5, 4, 6);
    private static final Set<Integer> DEFAULT_ACTIVE_IDS = Set.of(3, 2, 1, 4, 11);
    private static final String EMPLOYER = "1100";

    private static CardStatusCheck.Config config(int warnDays) {
        return new CardStatusCheck.Config(null, "ZZ_CARD_STATUS", DEFAULT_PROBLEM_IDS, DEFAULT_ACTIVE_IDS,
                warnDays, TODAY.plusDays(warnDays), 36, 16_777_216L);
    }

    private static CardStatusCheck.CardRow row(String employerId, String userId, String participantId,
                                               String dependentId, String userTypeId, String lastFour,
                                               String status, String statusId, String expirationRaw) {
        return new CardStatusCheck.CardRow(employerId, "Acme Co", userId, participantId, dependentId,
                userTypeId, lastFour, status, statusId, expirationRaw, "", "", "");
    }

    /** Like {@link #row} but with {@code IssuedDate}/{@code MailedDate}/{@code RequestedDate}
     *  exposed — the supersession tests need control over card dates the narrower helper hardcodes
     *  to blank. */
    private static CardStatusCheck.CardRow rowFull(String employerId, String userId, String participantId,
                                                   String dependentId, String userTypeId, String lastFour,
                                                   String status, String statusId, String expirationRaw,
                                                   String issuedDate, String mailedDate, String requestedDate) {
        return new CardStatusCheck.CardRow(employerId, "Acme Co", userId, participantId, dependentId,
                userTypeId, lastFour, status, statusId, expirationRaw, issuedDate, mailedDate, requestedDate);
    }

    private static Set<String> designatedIds() {
        return Set.of(EMPLOYER);
    }

    // ── Condition 1: problem status ──────────────────────────────────

    @Test
    void lostStolenRowAtDesignatedEmployerIsProblemFinding() {
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Lost/Stolen", "5", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertEquals(1, eval.findings().size());
        assertTrue(eval.findings().get(0).problemStatus());
        assertFalse(eval.findings().get(0).expiringOrExpired());
    }

    @Test
    void rowAtUndesignatedEmployerIsExcluded() {
        CardStatusCheck.CardRow r = row("9999", "U1", "P1", "", "3", "1234", "Lost/Stolen", "5", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.undesignatedRows());
        assertEquals(0, eval.matchedRows());
    }

    // ── Condition 2: expired or expiring ──────────────────────────────

    @Test
    void requestedRowWithPastExpirationIsNotAnExpiryFinding() {
        // status 7 = Requested, not in the default active-status list.
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "", "Requested", "7", "1/1/2020");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertTrue(eval.findings().isEmpty());
    }

    @Test
    void activeRowWithPastExpirationIsAnExpiryFinding() {
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Active", "3", "1/1/2020");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertEquals(1, eval.findings().size());
        assertTrue(eval.findings().get(0).expiringOrExpired());
        assertFalse(eval.findings().get(0).problemStatus());
    }

    @Test
    void activeRowExpiringInsideWarnWindowIsAFinding() {
        // TODAY + 60 = 2026-11-14; 10/1/2026 falls inside.
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Active", "3", "10/1/2026");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertEquals(1, eval.findings().size());
        assertTrue(eval.findings().get(0).expiringOrExpired());
    }

    @Test
    void activeRowExpiringOutsideWarnWindowIsNotAFinding() {
        // 3/1/2027 falls after TODAY + 60 (2026-11-14).
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Active", "3", "3/1/2027");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertTrue(eval.findings().isEmpty());
    }

    @Test
    void holdRowWithPastExpirationAppearsInBothSections() {
        // status 4 = Hold: in both the default problem list and the default active list.
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/2020");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertEquals(1, eval.findings().size());
        CardStatusCheck.CardFinding finding = eval.findings().get(0);
        assertTrue(finding.problemStatus());
        assertTrue(finding.expiringOrExpired());

        Map<Integer, String> designated = new LinkedHashMap<>();
        designated.put(Integer.parseInt(EMPLOYER), "Acme Co");
        List<CardStatusCheck.Row> problemRows = CardStatusCheck.toRows(eval.findings(), designated, null, true);
        List<CardStatusCheck.Row> expiryRows = CardStatusCheck.toRows(eval.findings(), designated, null, false);
        assertEquals(1, problemRows.size());
        assertEquals(1, expiryRows.size());
        assertEquals(problemRows.get(0).findingKey(), expiryRows.get(0).findingKey());
    }

    @Test
    void dependentRowIsCountedWithNoNameAndNoError() {
        // UserTypeID = 7 (dependent), ParticipantID blank, DependentID populated.
        CardStatusCheck.CardRow r = row(EMPLOYER, "U777", "", "D555", "7", "9876", "Lost/Stolen", "5", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertEquals(1, eval.findings().size());

        Map<Integer, String> designated = new LinkedHashMap<>();
        designated.put(Integer.parseInt(EMPLOYER), "Acme Co");
        List<CardStatusCheck.Row> rows = CardStatusCheck.toRows(eval.findings(), designated, null, true);
        assertEquals(1, rows.size());
        CardStatusCheck.Row row = rows.get(0);
        assertEquals("", row.participantName());
        assertEquals("D555", row.dependentId());
        assertEquals("U777", row.userId());
    }

    // ── CSV parsing: header contract, trimming, missing headers ──────

    @Test
    void statusTrailingWhitespaceIsTrimmedAndMatchingUsesTheNumericId() throws Exception {
        String csv = "EmployerID,UserID,ParticipantID,ParticipantCardStatusID,Status,ExpirationDate\n"
                + EMPLOYER + ",U1,P1,1,\"Issued \",1/1/2020\n";
        List<CardStatusCheck.CardRow> rows = new CardStatusCheck().parse(csv);
        assertEquals(1, rows.size());
        assertEquals("Issued", rows.get(0).status()); // trimmed by cellOf

        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(rows, config(60), designatedIds());
        assertEquals(1, eval.findings().size());
        assertTrue(eval.findings().get(0).expiringOrExpired()); // status id 1 (Issued) is in the active list
    }

    @Test
    void missingRequiredHeaderThrows() {
        String csv = "EmployerID,UserID,ParticipantCardStatusID,Status\n" + EMPLOYER + ",U1,1,Issued\n";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new CardStatusCheck().parse(csv));
        assertTrue(ex.getMessage().contains("ExpirationDate"));
    }

    @Test
    void headerOnlyExportParsesToZeroDataRows() throws Exception {
        String csv = "EmployerID,UserID,ParticipantCardStatusID,Status,ExpirationDate\n";
        List<CardStatusCheck.CardRow> rows = new CardStatusCheck().parse(csv);
        assertTrue(rows.isEmpty());
    }

    @Test
    void everyUnparsableExpirationDateYieldsZeroDateParsedRows() {
        CardStatusCheck.CardRow r1 = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Active", "3", "not-a-date");
        CardStatusCheck.CardRow r2 = row(EMPLOYER, "U2", "P2", "", "3", "5678", "Hold", "4", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r1, r2), config(60), designatedIds());
        assertEquals(0, eval.dateParsedRows());
        // Still classifies the problem-status row despite the unparsable date — expiry and
        // problem-status are independent tests.
        assertEquals(1, eval.findings().size());
        assertTrue(eval.findings().get(0).problemStatus());
    }

    @Test
    void zeroQualifyingRowsIsZeroFindings() {
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Active", "3", "3/1/2027");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        assertTrue(eval.findings().isEmpty());
    }

    // ── Filename matching ─────────────────────────────────────────────

    @Test
    void exactFilenameMatching() {
        var pattern = CardStatusCheck.newestMatchingPattern("ZZ_CARD_STATUS");
        assertTrue(pattern.matcher("ZZ_CARD_STATUS_Export_20260915030000000.csv").matches());
        assertTrue(pattern.matcher("ZZ_CARD_STATUS_Export_20260915030000000_FOO.CSV").matches());
        assertFalse(pattern.matcher("ZZ_CARD_STATUS_DEL_Export_20260915030000000.csv").matches());
        assertFalse(pattern.matcher("ZZ_CARD_STATUS_Export_2026091503.csv").matches()); // too short a timestamp
    }

    // ── Acknowledgment ─────────────────────────────────────────────────

    private static AuditFindingAck handledAck(String findingKey, int statusId, LocalDate expiry) {
        AuditFindingAck ack = new AuditFindingAck();
        ack.setFindingKey(findingKey);
        ack.setAckState(AuditFindingAck.STATE_HANDLED);
        ack.setObservedCount(statusId);
        ack.setObservedThrough(expiry);
        return ack;
    }

    @Test
    void handledSuppressesUntilExpiryChangesThenResurfaces() {
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/2020");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        CardStatusCheck.CardFinding finding = eval.findings().get(0);

        Map<String, AuditFindingAck> acks = Map.of(finding.findingKey(),
                handledAck(finding.findingKey(), 4, LocalDate.of(2020, 1, 1)));

        CardStatusCheck.AckSplit split1 = CardStatusCheck.splitByAcknowledgment(eval.findings(), acks);
        assertTrue(split1.surfaced().isEmpty());
        assertEquals(1, split1.suppressed().size());

        // Same status, expiry now earlier -> re-surfaces (Objects.equals mismatch).
        CardStatusCheck.CardRow r2 = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/2019");
        CardStatusCheck.Evaluation eval2 = CardStatusCheck.evaluate(List.of(r2), config(60), designatedIds());
        CardStatusCheck.AckSplit split2 = CardStatusCheck.splitByAcknowledgment(eval2.findings(), acks);
        assertEquals(1, split2.surfaced().size());
        assertTrue(split2.suppressed().isEmpty());
    }

    @Test
    void statusChangeResurfacesViaFindingKeyMiss() {
        // Same user/lastFour, status now 5 (Lost/Stolen) instead of the acknowledged 4 (Hold) ->
        // a different findingKey entirely, so the old acknowledgment simply does not match it.
        CardStatusCheck.CardRow acknowledgedRow = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/2020");
        CardStatusCheck.Evaluation ackedEval = CardStatusCheck.evaluate(List.of(acknowledgedRow), config(60), designatedIds());
        String oldKey = ackedEval.findings().get(0).findingKey();
        Map<String, AuditFindingAck> acks = Map.of(oldKey, handledAck(oldKey, 4, LocalDate.of(2020, 1, 1)));

        CardStatusCheck.CardRow changedRow = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Lost/Stolen", "5", "1/1/2020");
        CardStatusCheck.Evaluation changedEval = CardStatusCheck.evaluate(List.of(changedRow), config(60), designatedIds());
        CardStatusCheck.AckSplit split = CardStatusCheck.splitByAcknowledgment(changedEval.findings(), acks);
        assertEquals(1, split.surfaced().size());
        assertTrue(split.suppressed().isEmpty());
    }

    @Test
    void ignoredSuppressesRegardlessOfChange() {
        CardStatusCheck.CardRow r = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/2020");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r), config(60), designatedIds());
        String key = eval.findings().get(0).findingKey();

        AuditFindingAck ignored = new AuditFindingAck();
        ignored.setFindingKey(key);
        ignored.setAckState(AuditFindingAck.STATE_IGNORED);
        // observedCount/observedThrough intentionally left null, matching what the servlet stores
        // for IGNORED.
        Map<String, AuditFindingAck> acks = Map.of(key, ignored);

        // Even with the expiration date now different, IGNORED still suppresses unconditionally.
        CardStatusCheck.CardRow r2 = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/1999");
        CardStatusCheck.Evaluation eval2 = CardStatusCheck.evaluate(List.of(r2), config(60), designatedIds());
        CardStatusCheck.AckSplit split = CardStatusCheck.splitByAcknowledgment(eval2.findings(), acks);
        assertTrue(split.surfaced().isEmpty());
        assertEquals(1, split.suppressed().size());
    }

    @Test
    void acknowledgingEveryFindingLeavesNoneSurfaced() {
        CardStatusCheck.CardRow r1 = row(EMPLOYER, "U1", "P1", "", "3", "1234", "Hold", "4", "1/1/2020");
        CardStatusCheck.CardRow r2 = row(EMPLOYER, "U2", "P2", "", "3", "5678", "Lost/Stolen", "5", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(r1, r2), config(60), designatedIds());
        assertEquals(2, eval.findings().size());

        Map<String, AuditFindingAck> acks = new LinkedHashMap<>();
        for (CardStatusCheck.CardFinding f : eval.findings()) {
            AuditFindingAck ack = new AuditFindingAck();
            ack.setFindingKey(f.findingKey());
            ack.setAckState(AuditFindingAck.STATE_IGNORED);
            acks.put(f.findingKey(), ack);
        }

        CardStatusCheck.AckSplit split = CardStatusCheck.splitByAcknowledgment(eval.findings(), acks);
        assertTrue(split.surfaced().isEmpty()); // -> OK, count 0
        assertEquals(2, split.suppressed().size());
    }

    // ── Date parsing ───────────────────────────────────────────────────

    @Test
    void dateOfHandlesLeadingTokenAndUnparsableInput() {
        assertEquals(LocalDate.of(2026, 9, 15), CardStatusCheck.dateOf("9/15/2026"));
        assertEquals(LocalDate.of(2026, 9, 15), CardStatusCheck.dateOf("9/15/2026 12:00:00 AM"));
        assertNull(CardStatusCheck.dateOf(""));
        assertNull(CardStatusCheck.dateOf(null));
        assertNull(CardStatusCheck.dateOf("garbage"));
    }

    // ── Supersession ───────────────────────────────────────────────────

    @Test
    void supersededByLaterUsableCardIsExcludedAndShownWithReplacementEvidence() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.superseded().size());
        CardStatusCheck.SupersededFinding sf = eval.superseded().get(0);
        assertEquals("2222", sf.replacementLastFour());
        assertEquals("Active", sf.replacementStatus());
        assertEquals(LocalDate.of(2024, 6, 1), sf.replacementCardDate());

        Map<Integer, String> designated = new LinkedHashMap<>();
        designated.put(Integer.parseInt(EMPLOYER), "Acme Co");
        List<CardStatusCheck.SupersededRow> rows = CardStatusCheck.toSupersededRows(eval.superseded(), designated, null);
        assertEquals(1, rows.size());
        assertEquals("2222", rows.get(0).replacementLastFour());
        assertEquals("1111", rows.get(0).lastFour());
    }

    @Test
    void earlierUsableCardDoesNotSupersedeSoRowSurfaces() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "6/1/2024", "", "");
        CardStatusCheck.CardRow earlierActive = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "1/1/2023", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, earlierActive), config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertTrue(eval.superseded().isEmpty());
    }

    // Superseded by the "card status — one finding per card holder" refinement: Hold (4) is not a
    // *usable* replacement (it stays out of usableStatusIds, since it sits in the problem list too),
    // but it IS card-bearing, and the widened rule supersedes on any later card-bearing row, not only
    // a usable one. See laterProblemCardSupersedesAndItselfSurfaces (test case 1 of the follow-up
    // prompt), which replaces this test's old assertions with the new expected behavior.
    @Test
    void laterProblemCardSupersedesAndItselfSurfaces() {
        // Lost/Stolen (older) + Hold (newer), same UserID: the older row is superseded by the
        // problem card (no usable candidate exists), and the Hold row itself surfaces (nothing is
        // later than it, so it can never be superseded — the never-hide invariant).
        CardStatusCheck.CardRow lostStolen = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow hold = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Hold", "4", "",
                "1/1/2026", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(lostStolen, hold), config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertEquals("Hold", eval.findings().get(0).status());

        assertEquals(1, eval.superseded().size());
        CardStatusCheck.SupersededFinding sf = eval.superseded().get(0);
        assertEquals("Lost/Stolen", sf.status());
        assertEquals("Hold", sf.replacementStatus());
        assertFalse(sf.replacementIsUsable());
    }

    @Test
    void fallbackToRequestedDateWhenReplacementsIssuedDateIsBlank() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        // Blank IssuedDate, but a later RequestedDate -- the fallback should still supersede.
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "", "", "6/1/2024");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.superseded().size());
        assertEquals(LocalDate.of(2024, 6, 1), eval.superseded().get(0).replacementCardDate());
    }

    @Test
    void flaggedRowWithNoCardDateAtAllFailsOpenAndSurfaces() {
        // Neither IssuedDate nor RequestedDate is populated on the flagged row itself.
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "", "", "");
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertTrue(eval.superseded().isEmpty());
    }

    @Test
    void expiredActiveRowSupersededOutOfTheExpirySection() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Active", "3", "1/1/2020",
                "1/1/2020", "", "");
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.superseded().size());
        assertTrue(eval.superseded().get(0).expiringOrExpired());
        assertFalse(eval.superseded().get(0).problemStatus());
    }

    @Test
    void dependentRowSupersededByLaterUsableCardOnSameDependentUserId() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U777", "", "D555", "7", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U777", "", "D555", "7", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.superseded().size());
        assertEquals("D555", eval.superseded().get(0).dependentId());
    }

    @Test
    void sameCardDateIsNotStrictlyLaterSoRowSurfaces() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "6/1/2024", "", "");
        CardStatusCheck.CardRow sameDate = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, sameDate), config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertTrue(eval.superseded().isEmpty());
    }

    @Test
    void everyFlaggedRowSupersededYieldsZeroFindingsAndSummaryNamesTheCount() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.superseded().size());

        CardStatusCheck.AckSplit split = CardStatusCheck.splitByAcknowledgment(eval.findings(), Map.of());
        assertTrue(split.surfaced().isEmpty()); // -> OK, count 0

        String summary = CardStatusCheck.summaryOf(eval, 1, split, "ZZ_CARD_STATUS_Export_x.csv");
        assertTrue(summary.startsWith("0 card record(s) flagged"));
        assertTrue(summary.contains("1 superseded by a newer card (1 by a usable card, 0 by a newer problem card)"));
    }

    @Test
    void supersededRowWithExistingAckStaysSupersededAndAckIsUntouched() {
        CardStatusCheck.CardRow flagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow replacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "6/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(flagged, replacement), config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(1, eval.superseded().size());

        // A stale acknowledgment left over from before the row became superseded (e.g. saved on a
        // prior run before the replacement card appeared in the export).
        String staleFindingKey = "U1:1111:5";
        Map<String, AuditFindingAck> acks = Map.of(staleFindingKey, handledAck(staleFindingKey, 5, LocalDate.of(2023, 1, 1)));

        CardStatusCheck.AckSplit split = CardStatusCheck.splitByAcknowledgment(eval.findings(), acks);
        assertTrue(split.surfaced().isEmpty());
        assertTrue(split.suppressed().isEmpty()); // findings is empty -- the ack is never even looked at

        // The ack map passed in is untouched -- nothing in evaluate()/splitByAcknowledgment deletes
        // or mutates it as a side effect of the row being superseded.
        assertEquals(1, acks.size());
        assertTrue(acks.containsKey(staleFindingKey));
    }

    // ── "One finding per card holder" — widened replacement set ──────

    @Test
    void olderRowsSupersededByChainWithUsablePreferredEvenWhenNotNewest() {
        // Lost/Stolen (older), Hold (middle), Active (newest): both older rows are superseded, the
        // replacement shown for each is the Active card (usable preferred over the chronologically
        // newer Hold candidate where both qualify), and nothing is surfaced -- Active itself is
        // never flagged (no expiration date set, not a problem status).
        CardStatusCheck.CardRow lostStolen = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow hold = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Hold", "4", "",
                "1/1/2024", "", "");
        CardStatusCheck.CardRow active = rowFull(EMPLOYER, "U1", "P3", "", "3", "3333", "Active", "3", "",
                "1/1/2025", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(lostStolen, hold, active),
                config(60), designatedIds());

        assertTrue(eval.findings().isEmpty());
        assertEquals(2, eval.superseded().size());
        for (CardStatusCheck.SupersededFinding sf : eval.superseded()) {
            assertEquals("Active", sf.replacementStatus());
            assertTrue(sf.replacementIsUsable());
        }
    }

    @Test
    void olderOfTwoLostStolenRowsSupersededNewerSurfaces() {
        // Two Lost/Stolen rows (older, newer), same user, nothing else -> the older is superseded
        // (by the newer problem card), the newer is surfaced (nothing is later than it).
        CardStatusCheck.CardRow older = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow newer = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Lost/Stolen", "5", "",
                "1/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(older, newer), config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertEquals("2222", eval.findings().get(0).lastFour());

        assertEquals(1, eval.superseded().size());
        assertEquals("1111", eval.superseded().get(0).lastFour());
        assertEquals("Lost/Stolen", eval.superseded().get(0).replacementStatus());
        assertFalse(eval.superseded().get(0).replacementIsUsable());
    }

    @Test
    void newerRequestedRowIsNotCardBearingSoDoesNotSupersede() {
        CardStatusCheck.CardRow lostStolen = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        // Requested (7) is excluded from cardBearingStatusIds by default -- never produced a card.
        CardStatusCheck.CardRow requested = rowFull(EMPLOYER, "U1", "P2", "", "3", "", "Requested", "7", "",
                "1/1/2026", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(lostStolen, requested),
                config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertTrue(eval.superseded().isEmpty());
    }

    @Test
    void olderHoldSupersededByNewerPermanentlyInactiveWhichItselfSurfaces() {
        CardStatusCheck.CardRow hold = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Hold", "4", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow permanentlyInactive = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222",
                "Permanently Inactive", "6", "", "1/1/2024", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(hold, permanentlyInactive),
                config(60), designatedIds());

        assertEquals(1, eval.findings().size());
        assertEquals("Permanently Inactive", eval.findings().get(0).status());

        assertEquals(1, eval.superseded().size());
        assertEquals("Hold", eval.superseded().get(0).status());
        assertEquals("Permanently Inactive", eval.superseded().get(0).replacementStatus());
        assertFalse(eval.superseded().get(0).replacementIsUsable());
    }

    @Test
    void newestCardWithBlankDateFailsOpenSoBothRowsSurface() {
        // The newest card (Hold) has no parseable card date at all; the older Lost/Stolen has one.
        // Fail-open applies from both directions: the Hold candidate is not usable evidence for the
        // Lost/Stolen row (its own date is null), and the Hold row's own supersession check never
        // even starts (its flaggedCardDate is null). Both surface.
        CardStatusCheck.CardRow lostStolen = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow holdNoDate = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Hold", "4", "",
                "", "", "");
        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(List.of(lostStolen, holdNoDate),
                config(60), designatedIds());

        assertEquals(2, eval.findings().size());
        assertTrue(eval.superseded().isEmpty());
    }

    @Test
    void summarySplitCountsAreCorrectForAMixedFile() {
        // U = 1: Lost/Stolen superseded by a later Active (usable).
        CardStatusCheck.CardRow usableFlagged = rowFull(EMPLOYER, "U1", "P1", "", "3", "1111", "Lost/Stolen", "5", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow usableReplacement = rowFull(EMPLOYER, "U1", "P2", "", "3", "2222", "Active", "3", "",
                "1/1/2024", "", "");
        // P = 1: Hold superseded by a later Permanently Inactive (problem-only).
        CardStatusCheck.CardRow problemFlagged = rowFull(EMPLOYER, "U2", "P3", "", "3", "3333", "Hold", "4", "",
                "1/1/2023", "", "");
        CardStatusCheck.CardRow problemReplacement = rowFull(EMPLOYER, "U2", "P4", "", "3", "4444",
                "Permanently Inactive", "6", "", "1/1/2024", "", "");

        CardStatusCheck.Evaluation eval = CardStatusCheck.evaluate(
                List.of(usableFlagged, usableReplacement, problemFlagged, problemReplacement),
                config(60), designatedIds());

        assertEquals(1, eval.findings().size()); // the Permanently Inactive replacement itself surfaces
        assertEquals(2, eval.superseded().size());

        CardStatusCheck.AckSplit split = CardStatusCheck.splitByAcknowledgment(eval.findings(), Map.of());
        String summary = CardStatusCheck.summaryOf(eval, 1, split, "ZZ_CARD_STATUS_Export_x.csv");
        assertTrue(summary.contains("2 superseded by a newer card (1 by a usable card, 1 by a newer problem card)"));
    }
}
