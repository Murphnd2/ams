package net.superiorstate.ams.data.service;

import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.model.market.SummitFileExport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * S45-B -- T230 (phase 1: employer, cdhplan, demographics). Predicts a pushed file's response
 * filename and directory, fetches it over SFTP on demand, and classifies its rows against
 * configured success/failure tokens. Pure logic plus one SFTP-orchestrating method; no servlet
 * types here.
 * <p>
 * ⚠️ <b>Nothing this class returns is ever persisted.</b> A response line can echo personal data --
 * Demographics echoes participant names, and {@code SummitFileExport}'s own class note records a
 * rejection comment that once echoed a full street address. {@link CheckResult} and
 * {@link ResponseCheck} exist to be rendered in one request and discarded; only the caller's
 * decision to Mark done (basis and export id) belongs in {@code summit_setup_step}.
 * <p>
 * Predict, don't correlate: the response for a pushed row is {@code Response_} + that row's exact
 * {@code file_name}, in the {@code ResponseFiles} directory sibling to that row's exact
 * {@code delivery_dir}. Both are stored verbatim on the {@link SummitFileExport} row
 * ({@code SummitExportServlet.pushFile}), so this class never has to reconstruct or guess either.
 * <p>
 * Classification is by line position (row number), never by an echoed key -- Demographics'
 * response shape is {@code Participant TPA Custom ID|Status|Message} and the observed Employer
 * Demographic shape is {@code Status|Employer Name|Employer TPA Custom ID|Comment} (SDX-18): the
 * status field is not even in the same column across file types, so only the first field is ever
 * trusted, and only as this row's own status -- never matched back against what was sent.
 */
public abstract class SummitResponseService {

    /** 1 MiB -- the display cap for a fetched response file, matching {@link #check}'s size gate. */
    private static final int MAX_RESPONSE_BYTES = 1_048_576;

    /**
     * Derives the {@code ResponseFiles} directory sibling to a stored {@code delivery_dir}
     * ({@code ImportFiles}). Mirrors {@code SummitSftpTestServlet}'s derivation exactly, plus a
     * guard that {@code deliveryDir} really does resolve to {@code ImportFiles} first (Phase A
     * confirmed every push is refused unless {@code SUMMIT_SFTP_IMPORT_DIR}'s final segment is
     * exactly {@code ImportFiles}, so a stored {@code delivery_dir} should always satisfy this --
     * this method still checks rather than trusting the column).
     *
     * @return the derived {@code .../ResponseFiles} path, or empty if {@code deliveryDir} is
     *         null/blank or its final path segment is not exactly {@code ImportFiles}.
     */
    public static Optional<String> responseDirFor(String deliveryDir) {
        if (deliveryDir == null) return Optional.empty();
        String trimmed = deliveryDir.trim();
        if (trimmed.isEmpty()) return Optional.empty();
        String normalized = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        int lastSlash = normalized.lastIndexOf('/');
        String finalSegment = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (!"ImportFiles".equals(finalSegment)) return Optional.empty();
        String parent = lastSlash >= 0 ? normalized.substring(0, lastSlash + 1) : "";
        return Optional.of(parent + "ResponseFiles");
    }

    /** {@code Response_} + the exact pushed filename. */
    public static String responseNameFor(String pushedFileName) {
        return "Response_" + pushedFileName;
    }

    /**
     * Parses response content already fetched, against the row count actually sent. Never throws
     * on malformed input -- an unparsable or empty line contributes nothing, a line with fewer
     * fields than expected simply carries an empty comment.
     * <p>
     * Success and failure tokens are configured, per the class-wide "no constants class for these
     * keys" convention this codebase already uses for {@code SUMMIT_SFTP_*} ({@code AppConfig.get}
     * inline, read once per call here rather than per line): {@code SUMMIT_RESPONSE_OK_TOKENS}
     * (default {@code Successful}) and {@code SUMMIT_RESPONSE_FAIL_TOKENS} (default {@code Failed}),
     * each a comma-separated list matched against the trimmed first field, case-insensitively. A
     * status matching neither is {@code UNKNOWN} -- never counted as success.
     */
    public static ResponseCheck parse(String content, int sentRows) {
        String rawOk = AppConfig.get("SUMMIT_RESPONSE_OK_TOKENS");
        String rawFail = AppConfig.get("SUMMIT_RESPONSE_FAIL_TOKENS");
        Set<String> okTokens = splitTokens((rawOk == null || rawOk.isBlank()) ? "Successful" : rawOk);
        Set<String> failTokens = splitTokens((rawFail == null || rawFail.isBlank()) ? "Failed" : rawFail);

        List<ResponseLine> lines = new ArrayList<>();
        int okCount = 0;
        int failedCount = 0;
        int unknownCount = 0;
        if (content != null) {
            for (String rawLine : content.split("\r?\n")) {
                if (rawLine.isBlank()) continue;
                String[] fields = rawLine.split(Pattern.quote("|"), -1);
                String status = fields.length > 0 ? fields[0].trim() : "";
                String classification;
                if (containsIgnoreCase(okTokens, status)) {
                    classification = "OK";
                    okCount++;
                } else if (containsIgnoreCase(failTokens, status)) {
                    classification = "FAILED";
                    failedCount++;
                } else {
                    classification = "UNKNOWN";
                    unknownCount++;
                }
                String comment = fields.length > 0 ? fields[fields.length - 1] : "";
                lines.add(new ResponseLine(lines.size() + 1, status, classification, List.of(fields), comment));
            }
        }
        int lineCount = lines.size();
        boolean countMismatch = lineCount != sentRows;
        boolean allOk = !countMismatch && okCount == sentRows && failedCount == 0 && unknownCount == 0;
        return new ResponseCheck(sentRows, lineCount, okCount, failedCount, unknownCount, lines, countMismatch, allOk);
    }

    /**
     * Full check for one pushed export: derives the response directory and name, lists
     * {@code ResponseFiles} looking for an exact-name match, and -- if found and under the display
     * cap -- fetches and parses it. Every failure path returns a populated {@link CheckResult}
     * rather than throwing; nothing here ever reaches the caller as an exception.
     *
     * @param sftp   the transport to use for this one check -- callers own its lifecycle exactly as
     *               {@code SummitExportServlet.pushFile} owns its own {@code new SummitSftpService()}.
     * @param pushed a row whose {@code deliveryStatus} is {@code PUSHED} -- callers are expected to
     *               have already selected such a row (e.g. via
     *               {@code SummitSetupStepDAO.findLatestPushed}); this method does not re-check the
     *               status itself, it only reads {@code deliveryDir}, {@code fileName} and
     *               {@code rowCount} off whatever row is passed.
     */
    public static CheckResult check(SummitSftpService sftp, SummitFileExport pushed) {
        Optional<String> dirOpt = responseDirFor(pushed.getDeliveryDir());
        if (dirOpt.isEmpty()) {
            return new CheckResult(null, null, false, null, null,
                    "Cannot derive a ResponseFiles directory from the stored delivery directory '"
                            + pushed.getDeliveryDir() + "'.");
        }
        String responseDir = dirOpt.get();
        String responseName = responseNameFor(pushed.getFileName());

        List<SummitSftpService.SftpEntry> entries;
        try {
            entries = sftp.list(responseDir);
        } catch (SummitSftpService.SftpTransportException e) {
            return new CheckResult(responseDir, responseName, false, null, null, e.getMessage());
        }

        SummitSftpService.SftpEntry match = null;
        for (SummitSftpService.SftpEntry entry : entries) {
            if (responseName.equals(entry.getName())) {
                match = entry;
                break;
            }
        }
        if (match == null) {
            return new CheckResult(responseDir, responseName, false, null, null, null);
        }
        if (match.getSize() > MAX_RESPONSE_BYTES) {
            return new CheckResult(responseDir, responseName, true, match.getSize(), null,
                    "Response file is " + match.getSize() + " bytes, over the "
                            + MAX_RESPONSE_BYTES + "-byte display cap. Not read.");
        }

        byte[] bytes;
        try {
            bytes = sftp.read(responseDir, responseName, MAX_RESPONSE_BYTES);
        } catch (SummitSftpService.SftpTransportException e) {
            return new CheckResult(responseDir, responseName, true, match.getSize(), null, e.getMessage());
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        ResponseCheck parsed = parse(content, pushed.getRowCount());
        return new CheckResult(responseDir, responseName, true, match.getSize(), parsed, null);
    }

    private static Set<String> splitTokens(String raw) {
        Set<String> tokens = new HashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) tokens.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static boolean containsIgnoreCase(Set<String> tokens, String status) {
        return tokens.contains(status.trim().toLowerCase(Locale.ROOT));
    }

    /** One parsed response line. {@code fields} holds every pipe-delimited field, in order. */
    public record ResponseLine(int lineNo, String status, String classification,
                                List<String> fields, String comment) {
    }

    /** The parsed outcome of one response file, checked against the row count actually sent. */
    public record ResponseCheck(int sentRows, int lineCount, int okCount, int failedCount,
                                 int unknownCount, List<ResponseLine> lines,
                                 boolean countMismatch, boolean allOk) {
    }

    /** The outcome of one {@link #check} call -- found/not-found, size, parsed result, or error. */
    public record CheckResult(String responseDir, String responseName, boolean found,
                               Long sizeBytes, ResponseCheck responseCheck, String error) {
    }
}
