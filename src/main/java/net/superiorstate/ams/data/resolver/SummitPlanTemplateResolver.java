package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.dao.SummitPlanTemplateMapDAO;
import net.superiorstate.ams.model.market.SummitPlanTemplateMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * S28-B — the mapping from an elected AMS {@code ServiceItem} to the Summit CDH plan it
 * should create in file 2 (Employer CDH Plan).
 * <p>
 * ⚠️ <b>S31-D — this property is now the FALLBACK, not the only source.</b>
 * {@link #configured(EntityManager, Long)} reads the PSP-scoped {@code summit_plan_template_map}
 * table (V095) first and reads this property only when that table holds no active row for the
 * PSP. Everything below still describes the property exactly, and the property path is
 * unchanged — but it is reached second. Until T202 ships the admin screen there is no way to
 * populate the table, so in practice the property is still what every installation runs on.
 * <p>
 * <b>Config key: {@code SUMMIT_PLAN_TEMPLATES}, read from {@code ssa.properties}.</b> Format
 * is comma-separated entries, each of three or four colon-separated fields:
 * <pre>
 * SUMMIT_PLAN_TEMPLATES=&lt;serviceItemId&gt;:&lt;templateId&gt;:&lt;keySegment&gt;[:&lt;label&gt;],...
 * </pre>
 * <ul>
 *   <li><b>{@code serviceItemId}</b> — the {@code ServiceItem}'s own primary key, matched
 *       against the modules the employer actually elected. Positive integer.</li>
 *   <li><b>{@code templateId}</b> — the Summit-assigned Plan Template ID. Positive integer.</li>
 *   <li><b>{@code keySegment}</b> — the segment placed inside {@code Import Plan ID}. May not
 *       contain a pipe or any whitespace, because {@code Import Plan ID} is an upsert key that
 *       travels through a pipe-delimited file.</li>
 *   <li><b>{@code label}</b> — optional, human-facing, used in {@code Plan Name} and
 *       {@code Plan Description}. <b>Defaults to {@code keySegment}</b> when absent or blank.
 *       May contain spaces. It cannot contain a colon (that would split into a fifth field and
 *       be rejected), and a stray pipe is stripped at emit by the servlet's own
 *       {@code sanitize()}, so it cannot corrupt the file.</li>
 * </ul>
 * <p>
 * ⚠️ <b>S28-D — why the key is an id and not a friendly code. Do not "fix" this back.</b> This
 * originally keyed on {@code ServiceItem.code}, which looked like the readable choice. S28-C
 * proved that column is <b>unreachable from the Service Manager UI</b> — {@code
 * ServiceManagerAction} creates every Setup-category {@code ServiceItem} without ever calling
 * {@code setCode}, the Service Manager JSP has no such field, and the {@code DatabaseInitializer}
 * baseline that would have seeded coded items sits inside a commented-out block. On a real
 * installation every {@code ServiceItem.code} is therefore null, and a live walk against a Setup
 * showing four service chips matched nothing at all. <b>The id is the only stable, non-null,
 * AMS-owned handle on an elected service</b> that does not first require adding an admin surface
 * and hand-entering a free-text key on every service on every installation.
 * <p>
 * A worked example — the shape this installation is expected to carry. <b>The service item ids
 * are placeholders</b>; they are installation-specific and are deliberately not written into
 * source (rule 4):
 * <pre>
 * SUMMIT_PLAN_TEMPLATES=&lt;ichraServiceItemId&gt;:&lt;ichraTemplateId&gt;:ICHRA:ICHRA,&lt;s125ExServiceItemId&gt;:&lt;s125ExTemplateId&gt;:S125EX:Section 125 Excepted Benefit
 * </pre>
 * <b>To discover an installation's ids</b>: generate Summit file 2 with this key unset or
 * deliberately mismatched — the resulting error page lists every service elected on that
 * application as {@code id = description}, which is exactly what this key needs.
 * <p>
 * <b>Entry order is meaningful — it is the order the rows are emitted in.</b> This follows the
 * convention {@code AmsDataGlobal.getPspHosts()} / {@code getPrimaryPspHost()} already
 * establishes for multi-value properties on this installation: config order carries operator
 * intent, and the reader preserves it rather than sorting or bucketing.
 * <p>
 * <b>Parsing is tolerant and never throws.</b> A malformed entry is skipped with a
 * {@code WARN} naming it, and parsing continues with the rest — the same tolerance
 * {@code RATE_CACHE_PLAN_YEARS} is read with in {@code ProposalBuilder} and
 * {@code IllustrationServlet}. A duplicate service item id keeps the first entry and skips the
 * later one, also with a {@code WARN}. One typo therefore costs one plan row, never the whole
 * export.
 * <p>
 * <b>Absence is a supported state, not an error.</b> When this key is unset, blank, or holds no
 * well-formed entry, {@link #configured()} returns an empty list and
 * {@code SummitExportServlet} falls back to its legacy behaviour: a single ICHRA row built from
 * {@code SUMMIT_ICHRA_PLAN_TEMPLATE_ID}, byte-identical to what production emitted at
 * {@code v0.94.00}. Deciding what to do about an empty list is the servlet's job, not this
 * class's.
 * <p>
 * <b>Why a property and not a table.</b> Template ids are Summit-assigned and differ per
 * installation, which is what config is for; a mapping table would be schema other features
 * build on before anyone knows its right shape; and a property is reversible by editing one
 * line and restarting Tomcat. <b>If it outgrows a property, this class is the seam a
 * table-backed mapping moves behind</b> — {@link #configured()} is the only thing any caller
 * knows about, so the storage can change without touching a single call site.
 */
public final class SummitPlanTemplateResolver {

    private static final Logger log = LogManager.getLogger(SummitPlanTemplateResolver.class);

    /** The {@code ssa.properties} key this resolver reads. Named so callers can cite it in errors. */
    public static final String CONFIG_KEY = "SUMMIT_PLAN_TEMPLATES";

    /** V095. Named here so log lines can cite the source that answered without a literal. */
    private static final String TABLE_NAME = "summit_plan_template_map";

    private SummitPlanTemplateResolver() {}

    /**
     * One configured Summit plan template — an immutable value type. Constructed by
     * {@link #configured()} from a validated config entry, or directly by a caller that needs
     * a synthetic entry (the servlet's legacy single-ICHRA fallback).
     */
    public static final class PlanTemplate {

        private final int serviceItemId;
        private final int templateId;
        private final String keySegment;
        private final String label;

        public PlanTemplate(int serviceItemId, int templateId, String keySegment, String label) {
            this.serviceItemId = serviceItemId;
            this.templateId = templateId;
            this.keySegment = keySegment;
            this.label = label;
        }

        /**
         * The {@code ServiceItem} primary key this template is elected by. Always positive on a
         * configured entry; a caller may synthesise a non-matching sentinel.
         */
        public int getServiceItemId() {
            return serviceItemId;
        }

        /** The Summit-assigned Plan Template ID. Always positive. */
        public int getTemplateId() {
            return templateId;
        }

        /** The segment placed inside {@code Import Plan ID}. Carries no pipe and no whitespace. */
        public String getKeySegment() {
            return keySegment;
        }

        /** Human-facing name used in {@code Plan Name} and {@code Plan Description}. Never blank. */
        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return serviceItemId + ":" + templateId + ":" + keySegment + ":" + label;
        }
    }

    /**
     * The configured plan templates, in config order.
     *
     * @return an unmodifiable list in config order — which is emit order. Empty when
     *         {@link #CONFIG_KEY} is absent, blank, or contains no well-formed entry.
     *         Never null.
     */
    public static List<PlanTemplate> configured() {
        String raw = AppConfig.get(CONFIG_KEY);
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        List<PlanTemplate> templates = new ArrayList<>();
        Set<Integer> seenServiceItemIds = new HashSet<>();

        for (String entry : raw.split(",")) {
            String trimmedEntry = entry.trim();
            // A blank entry is a stray or trailing comma, not a typo worth warning about --
            // same silent skip RATE_CACHE_PLAN_YEARS parsing uses.
            if (trimmedEntry.isEmpty()) continue;

            String[] fields = trimmedEntry.split(":");
            if (fields.length < 3 || fields.length > 4) {
                warnSkip(trimmedEntry, "expected 3 or 4 colon-separated fields, found " + fields.length);
                continue;
            }

            Integer serviceItemId = parsePositiveInt(fields[0]);
            if (serviceItemId == null) {
                warnSkip(trimmedEntry, "service item id '" + fields[0].trim() + "' is not a positive integer");
                continue;
            }

            Integer templateId = parsePositiveInt(fields[1]);
            if (templateId == null) {
                warnSkip(trimmedEntry, "template id '" + fields[1].trim() + "' is not a positive integer");
                continue;
            }

            String keySegment = fields[2].trim();
            // S31-D -- the rule moved to keySegmentRejection so the table reader applies exactly
            // the same one. The skips and their wording are unchanged.
            String keySegmentRejection = keySegmentRejection(keySegment);
            if (keySegmentRejection != null) {
                warnSkip(trimmedEntry, keySegmentRejection);
                continue;
            }

            // An absent OR blank fourth field means "no label" -- both fall back to the key
            // segment, so a trailing colon is never a way to configure a blank plan name.
            String label = keySegment;
            if (fields.length == 4 && !fields[3].trim().isEmpty()) {
                label = fields[3].trim();
            }

            if (!seenServiceItemIds.add(serviceItemId)) {
                log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: service item id {} is already"
                        + " configured earlier in the list; the first entry wins",
                        CONFIG_KEY, trimmedEntry, serviceItemId);
                continue;
            }

            templates.add(new PlanTemplate(serviceItemId, templateId, keySegment, label));
        }

        return Collections.unmodifiableList(templates);
    }

    /**
     * S31-D — the configured plan templates for one PSP, <b>table first, property as fallback</b>.
     * This is the overload {@code SummitExportServlet} calls; the no-arg {@link #configured()} is
     * the property-only path it falls back to.
     * <p>
     * <b>Resolution order.</b> {@link SummitPlanTemplateMapDAO#findActiveByPspId} first; if it
     * returns one or more rows they are used and the property is not read at all. If it returns
     * none — an installation that has taken V095 but entered no mapping rows, which is every
     * installation until T202 ships the admin screen — the property is read exactly as before and
     * an {@code INFO} records that the fallback was used.
     * <p>
     * ⚠️ <b>Callers cannot tell which source answered, and must not learn.</b> The return type,
     * ordering contract and every field's meaning are identical either way, so the emitted file is
     * byte-identical for a mapping expressed either way. That is the property this whole change
     * rests on.
     * <p>
     * ⚠️ <b>Both sources are validated by the same rules</b> — {@link #keySegmentRejection} and the
     * duplicate-{@code serviceItemId} first-wins rejection are shared, not reimplemented. A table
     * row carrying a pipe or whitespace in {@code key_segment} is skipped with a {@code WARN}
     * exactly as a malformed property entry is, because a bad row must cost one plan rather than
     * corrupting a pipe-delimited file.
     * <p>
     * <b>A database failure is not allowed to break the export.</b> Any exception from the read is
     * logged and treated as "no rows", which falls through to the property — the same tolerance the
     * parser applies to a malformed entry, one layer out.
     *
     * @param em     an open {@code EntityManager}. Null skips the table and goes straight to the
     *               property.
     * @param pspId  the PSP whose mapping to read. Null skips the table — a caller with no resolved
     *               PSP gets the property path rather than an error.
     * @return an unmodifiable list in emit order. Empty when neither source holds a well-formed
     *         entry. Never null.
     */
    public static List<PlanTemplate> configured(EntityManager em, Long pspId) {
        List<PlanTemplate> fromTable = fromDatabase(em, pspId);
        if (!fromTable.isEmpty()) {
            return fromTable;
        }
        List<PlanTemplate> fromProperty = configured();
        log.info("[SUMMIT-EXPORT] no active {} rows for PSP {}; falling back to the {} property"
                        + " ({} entries)",
                TABLE_NAME, pspId, CONFIG_KEY, fromProperty.size());
        return fromProperty;
    }

    /**
     * The V095 table's contribution, or an empty list. Never throws: a read failure is logged and
     * reported as empty so the caller falls back to the property.
     */
    private static List<PlanTemplate> fromDatabase(EntityManager em, Long pspId) {
        if (em == null || pspId == null) return Collections.emptyList();

        List<SummitPlanTemplateMap> rows;
        try {
            rows = SummitPlanTemplateMapDAO.findActiveByPspId(em, pspId);
        } catch (RuntimeException e) {
            log.warn("[SUMMIT-EXPORT] could not read {} for PSP {} ({}); falling back to the {}"
                            + " property", TABLE_NAME, pspId, e.toString(), CONFIG_KEY);
            return Collections.emptyList();
        }
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<PlanTemplate> templates = new ArrayList<>();
        Set<Integer> seenServiceItemIds = new HashSet<>();

        for (SummitPlanTemplateMap row : rows) {
            String rowRef = "id " + row.getId();

            Integer serviceItemId = row.getServiceItemId();
            if (serviceItemId == null || serviceItemId <= 0) {
                warnSkip(TABLE_NAME, rowRef, "service item id '" + serviceItemId + "' is not a positive integer");
                continue;
            }

            Integer templateId = row.getTemplateId();
            if (templateId == null || templateId <= 0) {
                warnSkip(TABLE_NAME, rowRef, "template id '" + templateId + "' is not a positive integer");
                continue;
            }

            String keySegment = row.getKeySegment() == null ? "" : row.getKeySegment().trim();
            String rejection = keySegmentRejection(keySegment);
            if (rejection != null) {
                warnSkip(TABLE_NAME, rowRef, rejection);
                continue;
            }

            // Identical rule to the property's absent-or-blank fourth field: both fall back to the
            // key segment, so a blank label is never a way to configure a blank plan name.
            String label = keySegment;
            if (row.getLabel() != null && !row.getLabel().trim().isEmpty()) {
                label = row.getLabel().trim();
            }

            // The unique constraint makes this unreachable through the admin screen; it is kept so
            // the two sources are validated identically and so a hand-edited row cannot behave
            // differently from a hand-edited property entry.
            if (!seenServiceItemIds.add(serviceItemId)) {
                log.warn("[SUMMIT-EXPORT] {} skipped: service item id {} is already mapped earlier"
                        + " for this PSP; the first row wins", rowRef, serviceItemId);
                continue;
            }

            templates.add(new PlanTemplate(serviceItemId, templateId, keySegment, label));
        }

        if (templates.isEmpty()) {
            log.warn("[SUMMIT-EXPORT] {} held {} row(s) for PSP {} but none were well-formed;"
                            + " falling back to the {} property",
                    TABLE_NAME, rows.size(), pspId, CONFIG_KEY);
        } else {
            log.info("[SUMMIT-EXPORT] {} supplied {} plan template(s) for PSP {}; the {} property"
                    + " was not read", TABLE_NAME, templates.size(), pspId, CONFIG_KEY);
        }
        return Collections.unmodifiableList(templates);
    }

    /**
     * The one implementation of the {@code keySegment} rule, shared by the property parser and the
     * table reader so the two can never diverge.
     *
     * @return null when the segment is acceptable, otherwise the skip reason, worded exactly as the
     *         property parser worded it before this method existed.
     */
    private static String keySegmentRejection(String keySegment) {
        if (keySegment == null || keySegment.isEmpty()) {
            return "blank key segment";
        }
        if (keySegment.contains("|") || keySegment.chars().anyMatch(Character::isWhitespace)) {
            return "key segment '" + keySegment + "' contains a pipe or whitespace";
        }
        return null;
    }

    /** The property path. Wording unchanged from before S31-D. */
    private static void warnSkip(String entry, String reason) {
        warnSkip(CONFIG_KEY, entry, reason);
    }

    /**
     * S31-D — the same skip notice, naming the source that actually held the bad entry. Without
     * this overload a malformed {@code summit_plan_template_map} row would be logged as a
     * {@code SUMMIT_PLAN_TEMPLATES} entry, sending whoever reads it to edit the wrong thing.
     */
    private static void warnSkip(String source, String entry, String reason) {
        log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: {}", source, entry, reason);
    }

    /**
     * Mirrors {@code SummitExportServlet.parsePositiveInt} -- null for absent, blank,
     * non-numeric or non-positive, never an exception.
     */
    private static Integer parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
