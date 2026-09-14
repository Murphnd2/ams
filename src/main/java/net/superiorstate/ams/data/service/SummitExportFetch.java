package net.superiorstate.ams.data.service;

import net.superiorstate.ams.AppConfig;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S62-P2 -- the read-only fetch-and-select half of the export-compare pattern: derive the
 * {@code ExportFiles} directory, select the newest file under a caller-supplied prefix, and read
 * it under the configured byte cap.
 * <p>
 * ⚠️ <b>This is the extraction target for duplicated fetch logic, not yet a shared one.</b> The
 * same fetch shape is independently implemented in {@code IchraUncodedParticipantsCheck} (the
 * 17-digit-only timestamp pattern, unchanged since its own comment records that fixed width) and
 * in {@code SummitRefreshService} (the 14-to-17-digit widened pattern, TA-54). <b>Neither has been
 * migrated to this class.</b> This run adds a third, independent copy of the same fetch logic
 * rather than touching either existing one -- migrating those two is a later, separately-fenced
 * item. This class exists so the fetch shape has exactly one place to be extracted to when that
 * migration happens, and so no fourth copy is written from scratch.
 * <p>
 * <b>Reads only.</b> No {@code EntityManager} parameter, no write, no persistence, no
 * {@code audit_run} row. Every caller is responsible for its own record-keeping, exactly as
 * {@code IchraUncodedParticipantsCheck} keeps none and {@code SummitRefreshService} keeps its own.
 * <p>
 * <b>Timestamp width: 14 to 17 digits</b> ({@link #TIMESTAMPED_NAME}), matching
 * {@code SummitRefreshService}'s widened pattern (TA-54) rather than
 * {@code IchraUncodedParticipantsCheck}'s 17-digit-only one -- a real Summit export has been
 * observed at 16 digits (S61-P12), so a new fetch path should not repeat the narrower assumption.
 * Selection compares the leading 14 digits (fixed width, so string compare is numeric compare);
 * a tie on that breaks on the right-padded fractional group, the same rule
 * {@code SummitRefreshService.rightPadFraction} documents.
 */
public final class SummitExportFetch {

    /** {@code {anything}_{14-to-17-digit timestamp}.{extension}} -- see the class Javadoc on why
     *  this is wider than {@code IchraUncodedParticipantsCheck}'s 17-digit-only pattern. Group 1 is
     *  the leading 14 digits (seconds precision); group 2 is the trailing 0-to-3 digit fraction. */
    private static final Pattern TIMESTAMPED_NAME = Pattern.compile("^.+_(\\d{14})(\\d{0,3})\\.[A-Za-z0-9]+$");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private SummitExportFetch() {}

    /** Whether {@link #fetch} found and read a file, or found nothing matching the prefix. */
    public enum Outcome { FOUND, NOT_FOUND }

    /**
     * One fetch attempt's result. {@code content} and {@code fileTimestamp} are null when
     * {@code outcome} is {@link Outcome#NOT_FOUND}.
     */
    public record Result(Outcome outcome, String fileName, LocalDateTime fileTimestamp, String content) {

        public static Result notFound() {
            return new Result(Outcome.NOT_FOUND, null, null, null);
        }

        public static Result found(String fileName, LocalDateTime fileTimestamp, String content) {
            return new Result(Outcome.FOUND, fileName, fileTimestamp, content);
        }

        public boolean found() { return outcome == Outcome.FOUND; }
    }

    /**
     * Derives {@code ExportFiles} from the {@code SUMMIT_SFTP_IMPORT_DIR} config value, exactly as
     * {@code IchraUncodedParticipantsCheck.exportDirFor} does: the final path segment of the
     * configured import directory must be exactly {@code ImportFiles}, and {@code ExportFiles} is
     * its sibling. Returns null when {@code importDir} is null, blank, or does not end in
     * {@code ImportFiles}.
     */
    public static String exportDirFor(String importDir) {
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

    /**
     * Lists {@code exportDir} over {@code sftp}, selects the newest entry whose name starts with
     * {@code prefix} and whose trailing timestamp matches {@link #TIMESTAMPED_NAME}, reads it under
     * {@code maxBytes}, and BOM-strips the content.
     * <p>
     * A directory holding no file matching {@code prefix} at all, or holding only files whose
     * timestamp does not parse, both return {@link Outcome#NOT_FOUND} -- this method does not
     * distinguish the two reasons; a caller that needs to (as {@code SummitRefreshService} does,
     * to name a Summit-side naming defect precisely) must inspect the listing itself.
     *
     * @param sftp      the transport to use for this one fetch -- the caller owns its lifecycle,
     *                  exactly as every other caller of {@code SummitSftpService} does.
     * @param exportDir the {@code ExportFiles} directory to list, e.g. from {@link #exportDirFor}.
     * @param prefix    the filename prefix to match. Never null/blank -- callers configure this.
     * @param maxBytes  the byte cap under which the selected file is read.
     * @return {@link Result#notFound()} when no matching, parseable file exists; otherwise
     *         {@link Result#found}. Never null. Throws {@link SummitSftpService.SftpTransportException}
     *         on a transport failure -- this method does not translate that into a {@code Result},
     *         since callers render transport failures differently from "nothing found."
     */
    public static Result fetch(SummitSftpService sftp, String exportDir, String prefix, long maxBytes)
            throws SummitSftpService.SftpTransportException {
        List<SummitSftpService.SftpEntry> entries = sftp.list(exportDir);

        String newestName = null;
        String newestLeading14 = null;
        String newestFraction = null;
        for (SummitSftpService.SftpEntry entry : entries) {
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name == null || !name.startsWith(prefix)) continue;
            Matcher m = TIMESTAMPED_NAME.matcher(name);
            if (!m.matches()) continue;
            String leading14 = m.group(1);
            String fraction = rightPadFraction(m.group(2));
            boolean isNewer = newestLeading14 == null
                    || leading14.compareTo(newestLeading14) > 0
                    || (leading14.equals(newestLeading14) && fraction.compareTo(newestFraction) > 0);
            if (isNewer) {
                newestLeading14 = leading14;
                newestFraction = fraction;
                newestName = name;
            }
        }

        if (newestName == null) {
            return Result.notFound();
        }

        LocalDateTime fileTimestamp;
        try {
            fileTimestamp = LocalDateTime.parse(newestLeading14, TIMESTAMP_FMT);
        } catch (Exception e) {
            return Result.notFound();
        }

        byte[] bytes = sftp.read(exportDir, newestName, (int) Math.min(maxBytes, Integer.MAX_VALUE));
        String content = new String(bytes, StandardCharsets.UTF_8);
        // Same BOM-strip pattern as SummitImportService and IchraUncodedParticipantsCheck.
        if (content.startsWith("﻿")) {
            content = content.substring(1);
        }

        return Result.found(newestName, fileTimestamp, content);
    }

    /** Reads a config value through {@link AppConfig#get}, the same access point every caller of
     *  this class already uses for its own keys. Exposed so a caller's Javadoc can name a single
     *  source for "how is this config value read" without duplicating the call. */
    public static String config(String key) {
        return AppConfig.get(key);
    }

    /**
     * Right-pads a 0-to-3 digit fractional group to exactly 3 digits with trailing zeros, so
     * unequal widths (e.g. {@code "21"} vs {@code "210"}) still compare correctly as the same
     * hundredths value. Matches {@code SummitRefreshService.rightPadFraction} exactly -- right,
     * not left, because a Summit timestamp's trailing digits are read as a fraction of a second
     * (hundredths/milliseconds), not a left-anchored integer.
     */
    private static String rightPadFraction(String fraction) {
        return (fraction + "000").substring(0, 3);
    }
}
