package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.AuditRunDAO;
import net.superiorstate.ams.model.audit.AuditRun;
import net.superiorstate.ams.model.imports.ImportProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S61-P6 -- scheduled Summit J1 employer refresh. Fetches the newest J1 employer export from the
 * tenant's SFTP {@code ExportFiles} directory and feeds it, unchanged, to
 * {@link SummitImportService#importEmployers}, so Summit employer data lands in {@code employer}
 * without a human running the {@code /SummitImport} wizard.
 * <p>
 * <b>Lifecycle</b> mirrors {@code AuditService}: single-thread scheduled executor, daemon thread,
 * {@code scheduleAtFixedRate}, {@code shutdownNow()} then {@code awaitTermination(10s)}, an
 * {@code AtomicBoolean running} guard shared between the scheduled tick and the manual trigger so
 * at most one refresh executes at a time. A tick that finds itself overlapping an in-flight run
 * skips and records that it skipped; it never queues.
 * <p>
 * <b>Fetch</b> mirrors {@code IchraUncodedParticipantsCheck}: the {@code ExportFiles} directory is
 * derived from {@code SUMMIT_SFTP_IMPORT_DIR}; the newest file whose name starts with the
 * configured prefix is selected. A file older than the max-age guard, no matching file at all, or a
 * file that matched the prefix but whose trailing timestamp didn't parse are each a <b>normal
 * no-op</b> recorded as {@code OK}, not an error, with a summary naming which of the three it was.
 * Bytes are read under the same byte cap that check uses, BOM-stripped, written to a temp file under
 * {@code java.io.tmpdir} (never the repo, never {@code AMS_UPLOAD_DIR}), imported, and the temp file
 * deleted in a {@code finally}.
 * <p>
 * <b>S61-P12 -- Summit's export timestamp is not a fixed width.</b> A real {@code ZZ_J1_Employer}
 * export landed with a 16-digit timestamp where {@code IchraUncodedParticipantsCheck}'s own
 * participant export (and every other export observed to that point) carried 17 -- see
 * {@link #TIMESTAMPED_NAME}. This service's copy of the pattern now accepts 14 to 17 digits; that
 * check's copy is untouched and must stay 17-only, since its export has always been that width.
 * <p>
 * <b>S61-P9 -- skip-if-already-imported.</b> Kevin runs the Summit-side J1 export four times a day
 * (7/9/11/1); between the 1pm run and the next morning's 7am run the newest file is legitimately
 * many hours old, so the guard that actually prevents redundant re-imports is file identity, not
 * age. Before fetching bytes, the newest filename is compared against {@link #lastImportedFilename}
 * -- an exact-match skip, recorded as {@code SKIPPED}, with no fetch, no import, no cache refresh.
 * That field is set only on a successful import (never by a skip/error/no-op record, so it survives
 * a run of skips unchanged) and is seeded at construction from the last persisted {@code audit_run}
 * row <i>only if that row is itself a successful, parseable import</i> -- absent, wrong status, or
 * unparseable all fall through to "proceed with the import" rather than a wrong skip. It is lost on
 * restart, which costs at most one redundant idempotent re-import, never a missed one. The manual
 * trigger has a {@code force} overload that bypasses this skip (never the age guard); the scheduled
 * tick never forces. With the skip guard doing this work, the max-age guard's remaining job is
 * narrow: catch an export that has stopped running entirely.
 * <p>
 * <b>J1 only.</b> No J2/J3 (reverts AMS-typed contact names), no J4/J5/J7 (renewal and
 * {@code is_active} side-effects), no plan types, no employer inactivation propagation --
 * {@code SummitImportService.importEmployers} skips Inactive rows and that is left exactly as is.
 * <p>
 * <b>Run records</b> reuse {@code audit_run} through {@code AuditRunDAO.insert}, under the single
 * stable {@code check_key} {@link #CHECK_KEY}. {@code finding_count} is always 0: the J1 importer
 * re-merges rows whose email/phone/contact are blank on every run, so its "updated" count is not
 * evidence of change and is deliberately not surfaced as a finding. Counts and the filename go in
 * {@code summary}; nothing from the file's rows is ever logged or persisted here.
 * <p>
 * <b>Configuration.</b> The scheduled start is gated in {@code EmfListener} on the DB constant
 * {@code SUMMIT_REFRESH_ENABLED} (same mechanism as {@code AUDIT_SCHEDULER_ENABLED}); absent means
 * off, and "Run now" from {@code /SummitRefresh} works regardless. Everything about the fetch is
 * {@code ssa.properties}, read through {@code AppConfig.get} exactly as
 * {@code IchraUncodedParticipantsCheck} reads its own keys:
 * <ul>
 *   <li>{@code SUMMIT_SFTP_IMPORT_DIR} -- existing key, reused; {@code ExportFiles} is its sibling.</li>
 *   <li>{@code SUMMIT_AUDIT_EXPORT_MAX_BYTES} -- existing byte cap, reused (default 16 MiB).</li>
 *   <li>{@code SUMMIT_REFRESH_J1_PREFIX} -- filename prefix of the J1 export (default
 *       {@code ZZ_J1_Employer}).</li>
 *   <li>{@code SUMMIT_REFRESH_MAX_AGE_HOURS} -- ignore a file older than this (default 26, wide
 *       enough to span the overnight gap between a 1pm export and the next 7am one).</li>
 *   <li>{@code SUMMIT_REFRESH_INTERVAL_MINUTES} -- tick interval (default 60).</li>
 * </ul>
 */
public class SummitRefreshService {

    private static final Logger log = LogManager.getLogger(SummitRefreshService.class);

    /** {@code audit_run.check_key} for every row this service writes. */
    public static final String CHECK_KEY = "SUMMIT_REFRESH_J1";

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";

    public static final String CFG_PREFIX = "SUMMIT_REFRESH_J1_PREFIX";
    public static final String CFG_MAX_AGE_HOURS = "SUMMIT_REFRESH_MAX_AGE_HOURS";
    public static final String CFG_INTERVAL_MINUTES = "SUMMIT_REFRESH_INTERVAL_MINUTES";
    /** Existing keys, reused rather than duplicated. */
    public static final String CFG_IMPORT_DIR = "SUMMIT_SFTP_IMPORT_DIR";
    public static final String CFG_MAX_BYTES = "SUMMIT_AUDIT_EXPORT_MAX_BYTES";

    public static final String DEFAULT_PREFIX = "ZZ_J1_Employer";
    /** S61-P9 -- widened from 2 to 26h: four daily Summit exports (7/9/11/1) leave an ~18h
     *  overnight gap before the skip-if-already-imported guard (not this one) starts doing the
     *  work of keeping a redundant re-import from happening. */
    public static final long DEFAULT_MAX_AGE_HOURS = 26;
    public static final long DEFAULT_INTERVAL_MINUTES = 60;
    private static final long DEFAULT_MAX_BYTES = 16_777_216L;
    private static final long INITIAL_DELAY_MINUTES = 10;

    /**
     * S61-P12 -- widened from {@code IchraUncodedParticipantsCheck}'s fixed
     * {@code {anything}_{17-digit yyyyMMddHHmmssSSS}.{extension}} shape after a real
     * {@code ZZ_J1_Employer} export landed with a 16-digit timestamp and was silently excluded (the
     * prefix matched; this pattern didn't). Group 1 is always exactly the leading 14 digits --
     * {@code yyyyMMddHHmmss}, second precision -- and group 2 is whatever's left, 0 to 3 digits.
     * <b>What that fractional group means is not known.</b> Summit may be emitting a
     * zero-stripped millisecond value (so a 2-digit fragment would want a leading zero restored) or
     * a hundredths-of-a-second value (so it would want a trailing zero appended) -- there is no way
     * to tell from one observed filename, and guessing a padding direction into the code would assert
     * something not established. Ordering therefore never treats the fragment as a number of
     * milliseconds or hundredths; see {@link #rightPadFraction} for the one place it's used at all.
     */
    private static final Pattern TIMESTAMPED_NAME = Pattern.compile("^.+_(\\d{14})(\\d{0,3})\\.[A-Za-z0-9]+$");
    /** Parses group 1 only -- second precision, deliberately ignoring the fractional group 2. */
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** S61-P9 -- leading marker on a successful-import {@code summary}, so
     *  {@link #parseImportedFilename} can tell a real import apart from a no-op/skip/error summary
     *  without guessing. Owned entirely by this class -- no other writer of {@code check_key}
     *  {@link #CHECK_KEY} rows exists. */
    private static final String IMPORTED_MARKER = "Imported ";

    private final ScheduledExecutorService executor;
    private final EntityManagerFactory emf;
    private final AmsDataGlobal global;
    private final Long pspId;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean scheduled = false;
    private volatile AuditRun latestRun;

    /** S61-P9 -- filename of the last successfully imported J1 export, or {@code null} if there
     *  hasn't been one this JVM knows of. Updated only on a genuine successful import (step 7 of
     *  {@link #runOnce}), never by a skip/error/no-op record, so it is stable across any number of
     *  {@code SKIPPED} ticks in a row. Seeded at construction from the persisted {@code audit_run}
     *  row, but that seed is used only when the row is itself a parseable success -- see
     *  {@link #parseImportedFilename}. Lost on restart if the persisted row at that moment isn't a
     *  success (e.g. the last row was a {@code SKIPPED}); the cost is one redundant, idempotent
     *  re-import, never a missed one. */
    private volatile String lastImportedFilename;

    /**
     * Loads the last stored run for {@link #CHECK_KEY}. Never throws -- a database problem at
     * startup must not abort {@code contextInitialized}; it logs and starts with no last run.
     */
    public SummitRefreshService(EntityManagerFactory emf, AmsDataGlobal global, Long pspId) {
        this.emf = emf;
        this.global = global;
        this.pspId = pspId;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "summit-refresh");
            t.setDaemon(true);
            return t;
        });

        try {
            EntityManager em = emf.createEntityManager();
            try {
                latestRun = AuditRunDAO.findLatestPerCheck(em, pspId).get(CHECK_KEY);
                lastImportedFilename = parseImportedFilename(latestRun);
            } finally {
                if (em.isOpen()) em.close();
            }
        } catch (Exception e) {
            log.warn("[SUMMIT-REFRESH] Could not load prior run — starting with no last run", e);
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    /** Starts the periodic refresh. Only {@code EmfListener} calls this, and only when the
     *  {@code SUMMIT_REFRESH_ENABLED} constant is {@code true}. */
    public void start() {
        long interval = getIntervalMinutes();
        executor.scheduleAtFixedRate(this::scheduledTick, INITIAL_DELAY_MINUTES, interval, TimeUnit.MINUTES);
        scheduled = true;
        log.info("[SUMMIT-REFRESH] Started — refreshing every {}m (first run in {}m)", interval, INITIAL_DELAY_MINUTES);
    }

    /** Same shutdown shape as {@code AuditService.stop()}: {@code shutdownNow} then a 10s wait so an
     *  in-flight import doesn't outlive the {@code EntityManagerFactory}. */
    public void stop() {
        executor.shutdownNow();
        try {
            if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("[SUMMIT-REFRESH] Stopped");
            } else {
                log.warn("[SUMMIT-REFRESH] Stopped — task still running after 10s shutdown timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[SUMMIT-REFRESH] Stop interrupted while awaiting termination");
        }
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public boolean isRunInProgress() {
        return running.get();
    }

    public AuditRun getLatestRun() {
        return latestRun;
    }

    /** S61-P9 -- filename the skip guard will compare the next newest export against, or
     *  {@code null} if nothing has been successfully imported yet (this JVM's lifetime, or a
     *  parseable persisted row at construction). */
    public String getLastImportedFilename() {
        return lastImportedFilename;
    }

    public Long getPspId() {
        return pspId;
    }

    public enum TriggerResult { STARTED, ALREADY_RUNNING }

    /** Runs one refresh on this service's own executor and returns immediately. Shares the
     *  {@code running} guard with the scheduled tick, so two rapid clicks cannot overlap.
     *  Equivalent to {@code triggerManual(false)}. */
    public TriggerResult triggerManual() {
        return triggerManual(false);
    }

    /**
     * S61-P9 -- same as {@link #triggerManual()}, but with {@code force} true the
     * skip-if-already-imported check in {@link #runOnce} is bypassed for this one run (the max-age
     * guard is not; a genuinely stale export still no-ops). For a PSP admin who corrected the same
     * file on the Summit side and needs {@code /SummitRefresh}'s Run now to actually re-fetch it.
     * The scheduled tick never calls this overload -- {@link #scheduledTick} always passes
     * {@code false}.
     */
    public TriggerResult triggerManual(boolean force) {
        if (!running.compareAndSet(false, true)) {
            return TriggerResult.ALREADY_RUNNING;
        }
        executor.execute(() -> runAcquired(TRIGGER_MANUAL, force));
        return TriggerResult.STARTED;
    }

    private void scheduledTick() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[SUMMIT-REFRESH] Scheduled tick skipped — a run is already in progress");
            record(TRIGGER_SCHEDULED, STATUS_SKIPPED, "Tick skipped — a refresh was already in progress.", null, 0);
            return;
        }
        runAcquired(TRIGGER_SCHEDULED, false); // scheduled tick never forces
    }

    /** Precondition: caller has acquired {@code running}. Nothing thrown here may escape --
     *  {@code scheduleAtFixedRate} silently cancels all future ticks after one uncaught throw. */
    private void runAcquired(String trigger, boolean force) {
        long start = System.currentTimeMillis();
        try {
            runOnce(trigger, start, force);
        } catch (Throwable t) {
            log.error("[SUMMIT-REFRESH] Run failed", t);
            record(trigger, STATUS_ERROR, null, scrub(t), elapsed(start));
        } finally {
            running.set(false);
        }
    }

    // ── One tick ─────────────────────────────────────────────────────

    private void runOnce(String trigger, long start, boolean force) {
        // 1. Config.
        String prefix = getPrefix();
        String exportDir = getExportDir();
        if (exportDir == null) {
            record(trigger, STATUS_NOT_CONFIGURED, null,
                    "Cannot derive an ExportFiles directory from " + CFG_IMPORT_DIR + " ('"
                            + AppConfig.get(CFG_IMPORT_DIR) + "').", elapsed(start));
            return;
        }
        long maxAgeHours = getMaxAgeHours();
        long maxBytes = getMaxBytes();

        // 2. List and select the newest timestamped file under the prefix.
        SummitSftpService sftp = new SummitSftpService();
        List<SummitSftpService.SftpEntry> entries;
        try {
            entries = sftp.list(exportDir);
        } catch (SummitSftpService.SftpTransportException e) {
            record(trigger, STATUS_ERROR, null, scrub(e), elapsed(start));
            return;
        }

        // S61-P12 -- primary key is the leading 14 digits (fixed-width, so String.compareTo is
        // numeric compare); a tie on that breaks on the fractional group, right-padded so unequal
        // widths (e.g. "21" vs "210") still compare -- see rightPadFraction for why right, not left.
        String newestName = null;
        String newestLeading14 = null;
        String newestFraction = null;
        int prefixMatchCount = 0;
        String unparseableExample = null;
        for (SummitSftpService.SftpEntry entry : entries) {
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name == null || !name.startsWith(prefix)) continue;
            prefixMatchCount++;
            Matcher m = TIMESTAMPED_NAME.matcher(name);
            if (!m.matches()) {
                if (unparseableExample == null) unparseableExample = name;
                continue;
            }
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

        // 3. No file matching the prefix at all is a normal no-op.
        if (prefixMatchCount == 0) {
            record(trigger, STATUS_OK, "No-op — no export matching prefix '" + prefix + "' in " + exportDir + ".",
                    null, elapsed(start));
            return;
        }

        // 3b. S61-P12 -- at least one file matched the prefix, but none had a parseable timestamp.
        // This is the failure that cost real time on 2026-09-14: a 16-digit Summit timestamp was
        // silently excluded by what was then a 17-digit-only pattern. Distinguished from "nothing
        // matched the prefix" above so it can't happen silently again.
        if (newestName == null) {
            record(trigger, STATUS_OK, "No-op — " + prefixMatchCount + " file(s) matched prefix '" + prefix
                    + "' but none parsed as {prefix}_..._{14-17 digit timestamp}.{ext}; e.g. '"
                    + unparseableExample + "'.", null, elapsed(start));
            return;
        }

        // 3c. S61-P9 -- skip when this exact file was already imported successfully. force
        // (manual trigger only) bypasses this check; it never bypasses the age guard below.
        if (!force && newestName.equals(lastImportedFilename)) {
            record(trigger, STATUS_SKIPPED, "Skipped — " + newestName + " was already imported.", null, elapsed(start));
            return;
        }

        // 4. A stale file (genuinely abandoned export) is also a normal no-op. Second precision only
        // -- the fractional group played no part in selecting this file and plays none here either.
        LocalDateTime fileTimestamp;
        try {
            fileTimestamp = LocalDateTime.parse(newestLeading14, TIMESTAMP_FMT);
        } catch (Exception e) {
            record(trigger, STATUS_ERROR, null,
                    "Export filename '" + newestName + "' carries an unparsable timestamp.", elapsed(start));
            return;
        }

        long ageHours = Duration.between(fileTimestamp, LocalDateTime.now()).toHours();
        if (ageHours > maxAgeHours) {
            record(trigger, STATUS_OK, "No-op — newest export " + newestName + " is " + ageHours
                    + "h old (max " + maxAgeHours + "h).", null, elapsed(start));
            return;
        }

        // 4. Read under the byte cap, BOM-strip, spool to a temp file outside the repo and upload dir.
        byte[] bytes;
        try {
            bytes = sftp.read(exportDir, newestName, (int) Math.min(maxBytes, Integer.MAX_VALUE));
        } catch (SummitSftpService.SftpTransportException e) {
            record(trigger, STATUS_ERROR, null, scrub(e), elapsed(start));
            return;
        }

        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.startsWith("﻿")) {
            content = content.substring(1);
        }

        Path temp = null;
        try {
            temp = Files.createTempFile("summit-j1-", ".csv");
            Files.writeString(temp, content, StandardCharsets.UTF_8);

            // 5. Import on a fresh EntityManager with the SUMMIT provider, resolved the way the wizard does.
            SummitImportService.ImportResult result;
            EntityManager em = emf.createEntityManager();
            try {
                ImportProvider summitProvider = resolveSummitProvider(em);
                result = SummitImportService.importEmployers(em, temp.toFile(), summitProvider);
            } catch (Exception e) {
                // importEmployers declares throws Exception; the file is still removed in the outer finally.
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                record(trigger, STATUS_ERROR, null, scrub(e), elapsed(start));
                return;
            } finally {
                if (em.isOpen()) em.close();
            }

            // 6. Refresh the global employer cache exactly as SummitImportWizard does after an import.
            //    (resetEmployeeCache is J2/J3-only there and is deliberately not called: J1 only.)
            if (global != null) {
                EntityManager refreshEm = emf.createEntityManager();
                try {
                    global.initializeGlobalData(refreshEm);
                } finally {
                    if (refreshEm.isOpen()) refreshEm.close();
                }
            }

            // 7. Record. Warning text is never persisted or logged -- count only. The IMPORTED_MARKER
            // prefix is what parseImportedFilename looks for on a restart; lastImportedFilename
            // itself is set directly (no parsing needed) since newestName is already in hand.
            String summary = IMPORTED_MARKER + newestName + " (" + fileTimestamp + ") — " + result.summary()
                    + (result.getWarnings().isEmpty() ? "" : ", " + result.getWarnings().size() + " warning(s)");
            lastImportedFilename = newestName;
            record(trigger, STATUS_OK, summary, null, elapsed(start));
            log.info("[SUMMIT-REFRESH] {} — {}", newestName, result.summary());
        } catch (IOException e) {
            record(trigger, STATUS_ERROR, null, scrub(e), elapsed(start));
        } finally {
            // 8. Always remove the spooled copy.
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    log.warn("[SUMMIT-REFRESH] Could not delete temp file {}", temp, e);
                }
            }
        }
    }

    /** Same lookup {@code SummitImportWizard} performs; {@code null} when the SUMMIT provider row
     *  is absent, which {@code importEmployers} tolerates (it then records no id mappings). */
    private static ImportProvider resolveSummitProvider(EntityManager em) {
        try {
            return em.createQuery(
                    "SELECT p FROM ImportProvider p WHERE p.providerCode = 'SUMMIT'", ImportProvider.class)
                    .setMaxResults(1).getSingleResult();
        } catch (Exception ignored) {
            return null;
        }
    }

    // ── Run records ──────────────────────────────────────────────────

    private void record(String trigger, String status, String summary, String error, int durationMs) {
        AuditRun run = new AuditRun();
        run.setPspId(pspId);
        run.setCheckKey(CHECK_KEY);
        run.setRunAt(LocalDateTime.now());
        run.setRunTrigger(trigger);
        run.setStatus(status);
        run.setFindingCount(0);
        run.setSummary(truncate(summary, 500));
        run.setError(truncate(error, 500));
        run.setDurationMs(durationMs);

        EntityManager em = emf.createEntityManager();
        try {
            AuditRunDAO.insert(em, run);
            latestRun = run;
        } catch (Exception e) {
            log.error("[SUMMIT-REFRESH] Could not record run", e);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    // ── Config accessors (also read by /SummitRefresh for display) ──

    public String getPrefix() {
        String raw = AppConfig.get(CFG_PREFIX);
        return (raw == null || raw.isBlank()) ? DEFAULT_PREFIX : raw.trim();
    }

    public long getMaxAgeHours() {
        return parseLongOrDefault(AppConfig.get(CFG_MAX_AGE_HOURS), DEFAULT_MAX_AGE_HOURS);
    }

    public long getMaxBytes() {
        return parseLongOrDefault(AppConfig.get(CFG_MAX_BYTES), DEFAULT_MAX_BYTES);
    }

    public long getIntervalMinutes() {
        long v = parseLongOrDefault(AppConfig.get(CFG_INTERVAL_MINUTES), DEFAULT_INTERVAL_MINUTES);
        return v > 0 ? v : DEFAULT_INTERVAL_MINUTES;
    }

    /** Sibling {@code ExportFiles} directory to {@code SUMMIT_SFTP_IMPORT_DIR}, guarded the same way
     *  {@code IchraUncodedParticipantsCheck.exportDirFor} guards it: the final segment must be
     *  exactly {@code ImportFiles}, otherwise {@code null}. */
    public String getExportDir() {
        String importDir = AppConfig.get(CFG_IMPORT_DIR);
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

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * S61-P12 -- pads a 0-to-3-digit fractional timestamp fragment to a fixed 3 characters by
     * appending {@code '0'} on the right, so fragments of different lengths (a bare {@code "21"}
     * against a full {@code "210"}) remain comparable as same-width strings for tie-breaking only.
     * This is <b>not</b> a claim about what the fragment means -- whether it is hundredths of a
     * second or a millisecond value with a leading zero stripped is unknown (see
     * {@link #TIMESTAMPED_NAME}'s Javadoc), and left-padding instead would assert the latter.
     * Right-padding needs no such assertion: it only orders same-second files consistently, and two
     * exports inside one second (harmless either way, since the import is idempotent) is the only
     * thing that depends on it.
     */
    private static String rightPadFraction(String fraction) {
        return (fraction + "000").substring(0, 3);
    }

    private static long parseLongOrDefault(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int elapsed(long start) {
        return (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start);
    }

    private static String scrub(Throwable t) {
        String message = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
        return message.length() > 400 ? message.substring(0, 400) : message;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    /**
     * S61-P9 -- recovers the filename of a successful import from a persisted {@code audit_run}
     * summary, written in the {@link #IMPORTED_MARKER}{@code + filename + " (" + ...} shape step 7
     * of {@link #runOnce} writes. Returns {@code null} for anything else -- absent row, non-{@code
     * OK} status, a no-op/skip/error summary, or a shape that doesn't parse -- so a doubtful case
     * always falls through to "proceed with the import" rather than a wrong skip. Used only at
     * construction, to seed {@link #lastImportedFilename} across a restart; a live import sets that
     * field directly, with the filename already in hand, needing no parsing at all.
     */
    private static String parseImportedFilename(AuditRun run) {
        if (run == null || !STATUS_OK.equals(run.getStatus())) return null;
        String summary = run.getSummary();
        if (summary == null || !summary.startsWith(IMPORTED_MARKER)) return null;
        int parenIdx = summary.indexOf(" (", IMPORTED_MARKER.length());
        if (parenIdx < 0) return null;
        String filename = summary.substring(IMPORTED_MARKER.length(), parenIdx);
        return filename.isBlank() ? null : filename;
    }

    /** Exposed for the status page only -- what a matching filename must look like. */
    public static String expectedFilenameExample(String prefix) {
        return prefix + "_Export_20260914081530123.CSV";
    }
}
