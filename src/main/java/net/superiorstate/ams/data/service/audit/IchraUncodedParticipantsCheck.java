package net.superiorstate.ams.data.service.audit;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.service.SummitSftpService;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T237 first check: ICHRA participants Active in Summit's participant-list export with a blank
 * {@code ParticipantCustomID} — the D43 flat-amount notice mechanism's ongoing housekeeping list
 * ({@code docs/analysis/plus_tier_build_plan.md} D43, {@code docs/analysis/audit_framework.md}).
 * <p>
 * Reads the newest matching file in the {@code ExportFiles} directory sibling to
 * {@code SUMMIT_SFTP_IMPORT_DIR}, over the same {@code SummitSftpService} transport
 * {@code SummitResponseService} uses for {@code ResponseFiles}. ⚠️ <b>Counts only persist.</b>
 * {@link #evaluate} never returns row content; {@link #readLive} exposes the actual rows for the
 * detail page's one in-request render, the same pattern {@code SummitResponseService}'s parsed
 * content is rendered and discarded — see LA-40 in {@code docs/analysis/legal_assumptions.md}.
 */
public class IchraUncodedParticipantsCheck implements AuditCheck {

    public static final String KEY = "ichra_uncoded_participants";

    private static final String CFG_PREFIX = "SUMMIT_AUDIT_PARTICIPANT_EXPORT_PREFIX";
    private static final String CFG_MAX_AGE_HOURS = "SUMMIT_AUDIT_EXPORT_MAX_AGE_HOURS";
    private static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";

    private static final long DEFAULT_MAX_AGE_HOURS = 36;
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;

    /** {@code {anything}_{17-digit yyyyMMddHHmmssSSS}.{extension}} — the ExportFiles naming
     *  pattern recorded in {@code docs/business/summit_data_exchange.md}. */
    private static final Pattern TIMESTAMPED_NAME = Pattern.compile("^.+_(\\d{17})\\.[A-Za-z0-9]+$");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final String H_EMPLOYER_ID = "Employer_ID";
    private static final String H_EMPLOYER_NAME = "EmployerName";
    private static final String H_EMPLOYER_CUSTOM_ID = "EmployerCustomID"; // optional
    private static final String H_PARTICIPANT_ID = "Participant_ID";
    private static final String H_FIRST_NAME = "FirstName";
    private static final String H_LAST_NAME = "LastName";
    private static final String H_PARTICIPANT_CUSTOM_ID = "ParticipantCustomID";
    private static final String H_USER_STATUS = "UserStatus";

    private static final List<String> REQUIRED_HEADERS = List.of(
            H_EMPLOYER_ID, H_EMPLOYER_NAME, H_PARTICIPANT_ID, H_FIRST_NAME, H_LAST_NAME,
            H_PARTICIPANT_CUSTOM_ID, H_USER_STATUS);

    private static final String STATUS_ACTIVE = "active";

    @Override
    public String key() { return KEY; }

    @Override
    public String label() { return "ICHRA participants without a Summit custom ID"; }

    @Override
    public String detailPath() { return "/AuditIchraUncoded"; }

    @Override
    public AuditResult evaluate(EntityManager em, Long pspId) {
        try {
            Loaded loaded = loadLatest();
            if (loaded.errorResult != null) return loaded.errorResult;

            List<ParticipantRow> findings = findingsOf(loaded.rows);
            String summary = summaryOf(findings, loaded.fileName);
            return findings.isEmpty() ? AuditResult.ok(summary) : AuditResult.action(findings.size(), summary);
        } catch (Exception e) {
            return AuditResult.error(scrub(e));
        }
    }

    /**
     * For the detail page — the same read path as {@link #evaluate}, never stored. The
     * returned rows are exactly the fields the detail page renders; nothing else survives the
     * parse (see {@link #toParticipantRow}).
     */
    public Snapshot readLive() {
        try {
            Loaded loaded = loadLatest();
            if (loaded.errorResult != null) {
                return new Snapshot(null, null, List.of(), loaded.errorResult.error());
            }
            List<ParticipantRow> findings = findingsOf(loaded.rows);
            List<Row> rows = new ArrayList<>(findings.size());
            for (ParticipantRow p : findings) {
                String employerKey = !p.employerCustomId.isBlank() ? p.employerCustomId : p.employerId;
                rows.add(new Row(p.employerName, employerKey, p.firstName, p.lastName, p.participantId));
            }
            return new Snapshot(loaded.fileName, loaded.fileTimestamp, rows, null);
        } catch (Exception e) {
            return new Snapshot(null, null, List.of(), scrub(e));
        }
    }

    // ── Load ──────────────────────────────────────────────────────────

    private Loaded loadLatest() {
        String prefix = AppConfig.get(CFG_PREFIX);
        if (prefix == null || prefix.isBlank()) {
            return Loaded.failed(AuditResult.notConfigured(
                    "Config key " + CFG_PREFIX + " is not set."));
        }

        String importDir = AppConfig.get("SUMMIT_SFTP_IMPORT_DIR");
        String exportDir = exportDirFor(importDir);
        if (exportDir == null) {
            return Loaded.failed(AuditResult.error(
                    "Cannot derive an ExportFiles directory from SUMMIT_SFTP_IMPORT_DIR ('"
                            + importDir + "')."));
        }

        long maxAgeHours = parseLongOrDefault(AppConfig.get(CFG_MAX_AGE_HOURS), DEFAULT_MAX_AGE_HOURS);
        long maxBytes = parseLongOrDefault(AppConfig.get(CFG_MAX_BYTES), DEFAULT_MAX_BYTES);

        SummitSftpService sftp = new SummitSftpService();

        List<SummitSftpService.SftpEntry> entries;
        try {
            entries = sftp.list(exportDir);
        } catch (SummitSftpService.SftpTransportException e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        String newestName = null;
        long newestTimestamp = -1;
        for (SummitSftpService.SftpEntry entry : entries) {
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name == null || !name.startsWith(prefix)) continue;
            Matcher m = TIMESTAMPED_NAME.matcher(name);
            if (!m.matches()) continue;
            long ts;
            try {
                ts = Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (ts > newestTimestamp) {
                newestTimestamp = ts;
                newestName = name;
            }
        }

        if (newestName == null) {
            return Loaded.failed(AuditResult.error(
                    "No export matching prefix '" + prefix + "' found in " + exportDir + "."));
        }

        LocalDateTime fileTimestamp;
        try {
            fileTimestamp = LocalDateTime.parse(String.valueOf(newestTimestamp), TIMESTAMP_FMT);
        } catch (Exception e) {
            return Loaded.failed(AuditResult.error(
                    "Export filename '" + newestName + "' carries an unparsable timestamp."));
        }

        long ageHours = Duration.between(fileTimestamp, LocalDateTime.now()).toHours();
        if (ageHours > maxAgeHours) {
            return Loaded.failed(AuditResult.error(
                    "Export is " + ageHours + " hours old (max " + maxAgeHours + ")."));
        }

        byte[] bytes;
        try {
            bytes = sftp.read(exportDir, newestName, (int) Math.min(maxBytes, Integer.MAX_VALUE));
        } catch (SummitSftpService.SftpTransportException e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        String content = new String(bytes, StandardCharsets.UTF_8);
        // Same BOM-strip pattern as SummitImportService.
        if (content.startsWith("﻿")) {
            content = content.substring(1);
        }

        List<ParticipantRow> rows;
        try {
            rows = parse(content);
        } catch (MissingHeaderException e) {
            return Loaded.failed(AuditResult.error(e.getMessage()));
        } catch (Exception e) {
            return Loaded.failed(AuditResult.error(scrub(e)));
        }

        return Loaded.ok(newestName, fileTimestamp, rows);
    }

    /** Sibling {@code ExportFiles} directory to {@code importDir}, guarded the same way
     *  {@code SummitResponseService.responseDirFor} guards {@code ResponseFiles}: the final
     *  segment must be exactly {@code ImportFiles}, otherwise {@code null}. */
    private static String exportDirFor(String importDir) {
        if (importDir == null) return null;
        String trimmed = importDir.trim();
        if (trimmed.isEmpty()) return null;
        String normalized = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        int lastSlash = normalized.lastIndexOf('/');
        String finalSegment = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (!"ImportFiles".equals(finalSegment)) return null;
        String parent = lastSlash >= 0 ? normalized.substring(0, lastSlash + 1) : "";
        return parent + "ExportFiles";
    }

    private static long parseLongOrDefault(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── Parse ─────────────────────────────────────────────────────────

    private static final class MissingHeaderException extends RuntimeException {
        MissingHeaderException(String message) { super(message); }
    }

    /** Parses delimited content the same way {@code CensusParseService.readDelimited} does
     *  (opencsv {@code CSVReaderBuilder}), then maps each row immediately into a
     *  {@link ParticipantRow} carrying only the columns this check needs — nothing else survives
     *  the parse. */
    private List<ParticipantRow> parse(String content) throws Exception {
        List<String[]> lines = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8)).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return List.of();
        }

        String[] header = lines.get(0);
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.put(header[i] == null ? "" : header[i].trim(), i);
        }

        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_HEADERS) {
            if (!index.containsKey(required)) missing.add(required);
        }
        if (!missing.isEmpty()) {
            throw new MissingHeaderException("Export is missing required header(s): " + String.join(", ", missing));
        }

        List<ParticipantRow> rows = new ArrayList<>();
        for (int r = 1; r < lines.size(); r++) {
            rows.add(toParticipantRow(lines.get(r), index));
        }
        return rows;
    }

    /** Maps one raw row into a record holding only {@link #REQUIRED_HEADERS} plus the optional
     *  {@code EmployerCustomID} — every other column in the source row is discarded here and
     *  never referenced again. */
    private static ParticipantRow toParticipantRow(String[] raw, Map<String, Integer> index) {
        return new ParticipantRow(
                cellOf(raw, index, H_EMPLOYER_ID),
                cellOf(raw, index, H_EMPLOYER_NAME),
                index.containsKey(H_EMPLOYER_CUSTOM_ID) ? cellOf(raw, index, H_EMPLOYER_CUSTOM_ID) : "",
                cellOf(raw, index, H_PARTICIPANT_ID),
                cellOf(raw, index, H_FIRST_NAME),
                cellOf(raw, index, H_LAST_NAME),
                cellOf(raw, index, H_PARTICIPANT_CUSTOM_ID),
                cellOf(raw, index, H_USER_STATUS));
    }

    private static String cellOf(String[] raw, Map<String, Integer> index, String header) {
        Integer i = index.get(header);
        if (i == null || i >= raw.length || raw[i] == null) return "";
        return raw[i].trim();
    }

    // ── Filter / summarize ───────────────────────────────────────────

    private static List<ParticipantRow> findingsOf(List<ParticipantRow> rows) {
        List<ParticipantRow> findings = new ArrayList<>();
        for (ParticipantRow row : rows) {
            if (STATUS_ACTIVE.equals(row.userStatus.toLowerCase(java.util.Locale.ROOT))
                    && row.participantCustomId.isBlank()) {
                findings.add(row);
            }
        }
        return findings;
    }

    private static String summaryOf(List<ParticipantRow> findings, String fileName) {
        Set<String> employers = new LinkedHashSet<>();
        for (ParticipantRow row : findings) {
            employers.add(row.employerId);
        }
        return findings.size() + " participant(s) at " + employers.size() + " employer(s) — export " + fileName;
    }

    private static String scrub(Exception e) {
        String message = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
        return message.length() > 400 ? message.substring(0, 400) : message;
    }

    // ── Value types ──────────────────────────────────────────────────

    /** Internal, full row as parsed — never returned outside this class. {@link #readLive}
     *  narrows this to {@link Row} before it reaches a caller. */
    private record ParticipantRow(String employerId, String employerName, String employerCustomId,
                                   String participantId, String firstName, String lastName,
                                   String participantCustomId, String userStatus) {
    }

    private record Loaded(AuditResult errorResult, String fileName, LocalDateTime fileTimestamp,
                           List<ParticipantRow> rows) {
        static Loaded failed(AuditResult errorResult) {
            return new Loaded(errorResult, null, null, List.of());
        }

        static Loaded ok(String fileName, LocalDateTime fileTimestamp, List<ParticipantRow> rows) {
            return new Loaded(null, fileName, fileTimestamp, rows);
        }
    }

    /** One finding, narrowed to exactly what the detail page renders. */
    public record Row(String employerName, String employerKey, String firstName, String lastName,
                       String participantId) {
    }

    /** The detail page's whole view model. {@code error} is set (with empty {@code findings})
     *  when the export could not be loaded at all — the detail page shows it instead of a table. */
    public record Snapshot(String fileName, LocalDateTime fileTimestamp, List<Row> findings, String error) {
    }
}
