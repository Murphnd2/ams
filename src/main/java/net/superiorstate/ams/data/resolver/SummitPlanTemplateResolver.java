package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.AppConfig;
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
 * SUMMIT_PLAN_TEMPLATES=&lt;ichraServiceItemId&gt;:1030:ICHRA:ICHRA,&lt;s125ExServiceItemId&gt;:1031:S125EX:Section 125 Excepted Benefit
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
            if (keySegment.isEmpty()) {
                warnSkip(trimmedEntry, "blank key segment");
                continue;
            }
            if (keySegment.contains("|") || keySegment.chars().anyMatch(Character::isWhitespace)) {
                warnSkip(trimmedEntry, "key segment '" + keySegment + "' contains a pipe or whitespace");
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

    private static void warnSkip(String entry, String reason) {
        log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: {}", CONFIG_KEY, entry, reason);
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
