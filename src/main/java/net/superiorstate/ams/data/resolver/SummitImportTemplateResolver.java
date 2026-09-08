package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * S29-D — the mapping from a Summit export file to the name of the Summit import template it
 * must bind to.
 * <p>
 * <b>Why this exists.</b> Summit binds a retrieved file to an import template <b>by filename
 * prefix</b> — the uploaded file's name must begin with the template's name. Until the emitted
 * filenames carry that prefix, no file AMS produces can be imported at all. Template names live
 * in each TPA's own Summit tenant, so they are per-installation config exactly like
 * {@link SummitPlanTemplateResolver}'s template ids, and a literal in {@code .java} would be the
 * same defect rule 4 exists to prevent, one layer out.
 * <p>
 * <b>Config key: {@code SUMMIT_IMPORT_TEMPLATES}, read from {@code ssa.properties}.</b> Format is
 * comma-separated entries, each of two colon-separated fields:
 * <pre>
 * SUMMIT_IMPORT_TEMPLATES=&lt;file&gt;:&lt;templateName&gt;,...
 * </pre>
 * <ul>
 *   <li><b>{@code file}</b> — the {@code type} request parameter {@code SummitExportServlet}
 *       already uses to tell the three files apart: {@code employer} (file 1, Employer
 *       Demographic), {@code cdhplan} (file 2, Employer CDH Plan), {@code demographics}
 *       (file 4, Demographics). Matched case-insensitively.</li>
 *   <li><b>{@code templateName}</b> — the Summit import template's own name, used verbatim as
 *       the emitted filename's prefix. Restricted to letters, digits, {@code _}, {@code -} and
 *       {@code .}: it travels inside a {@code Content-Disposition} header, so a space, a quote
 *       or a path separator is rejected rather than emitted.</li>
 * </ul>
 * <p>
 * A worked example. <b>The template names are placeholders</b> — they are the vendor's values,
 * read out of the installation's own Summit tenant, and are deliberately not written into source:
 * <pre>
 * SUMMIT_IMPORT_TEMPLATES=employer:ZZ_TEST_ER,cdhplan:ZZ_TEST_CDH,demographics:ZZ_TEST_DEMO
 * </pre>
 * <p>
 * <b>Parsing is tolerant and never throws.</b> This mirrors {@link SummitPlanTemplateResolver}
 * exactly: a malformed entry is skipped with a {@code WARN} naming it and parsing continues, and
 * a duplicate file discriminator keeps the first entry and skips the later one, also with a
 * {@code WARN}. One typo therefore costs one filename, never the whole export — and never the
 * servlet, since nothing here runs in a static initializer.
 * <p>
 * ⚠️ <b>A misspelled discriminator is silent by design.</b> {@code employeer:X} parses fine and
 * simply never matches, so that file keeps its legacy name. This class deliberately does not
 * whitelist the discriminator vocabulary — {@code SummitExportServlet} owns its own request
 * contract and this resolver does not reach into it. The symptom is visible where it matters: the
 * downloaded file does not carry the expected prefix.
 * <p>
 * <b>Prefix collisions are warned about, not refused.</b> Summit matches on prefix, so if one
 * configured name is a prefix of another — {@code SSA_ER} and {@code SSA_ER_FIX} — a file can
 * bind to the wrong template silently. {@link #configured()} logs a {@code WARN} naming both
 * names when it sees this, and still serves. Refusing would turn a vendor-side naming choice
 * into an AMS outage.
 * <p>
 * <b>Absence is a supported state, not an error.</b> When this key is unset, blank, or holds no
 * well-formed entry, {@link #templateNameFor(String)} returns {@link Optional#empty()} for every
 * file and {@code SummitExportServlet} emits the descriptive filenames it emitted before this
 * class existed — byte-for-byte. A production installation taking this commit without touching
 * {@code ssa.properties} sees no change whatsoever. Deciding what to do about an empty answer is
 * the servlet's job, not this class's.
 */
public final class SummitImportTemplateResolver {

    private static final Logger log = LogManager.getLogger(SummitImportTemplateResolver.class);

    /** The {@code ssa.properties} key this resolver reads. Named so callers can cite it in errors. */
    public static final String CONFIG_KEY = "SUMMIT_IMPORT_TEMPLATES";

    /**
     * Characters a template name may carry. It becomes a filename inside a
     * {@code Content-Disposition} header, so whitespace, quotes, pipes and path separators are
     * all excluded — a name that would need escaping is rejected at parse time instead.
     */
    private static final String NAME_PATTERN = "[A-Za-z0-9._-]+";

    private SummitImportTemplateResolver() {}

    /**
     * The configured import template name for one export file.
     *
     * @param file the {@code type} discriminator — {@code employer}, {@code cdhplan} or
     *             {@code demographics}. Matched case-insensitively. Null or blank returns empty.
     * @return the template name to use as the emitted filename's prefix, or
     *         {@link Optional#empty()} when {@link #CONFIG_KEY} is absent, blank, holds no
     *         well-formed entry, or holds no entry for this file. Never null.
     */
    public static Optional<String> templateNameFor(String file) {
        if (file == null || file.isBlank()) return Optional.empty();
        return Optional.ofNullable(configured().get(file.trim().toLowerCase()));
    }

    /**
     * Every configured file-to-template-name pair, in config order.
     *
     * @return an unmodifiable map keyed by the lower-cased file discriminator, in config order.
     *         Empty when {@link #CONFIG_KEY} is absent, blank, or contains no well-formed entry.
     *         Never null.
     */
    public static Map<String, String> configured() {
        String raw = AppConfig.get(CONFIG_KEY);
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> templates = new LinkedHashMap<>();

        for (String entry : raw.split(",")) {
            String trimmedEntry = entry.trim();
            // A blank entry is a stray or trailing comma, not a typo worth warning about --
            // the same silent skip SummitPlanTemplateResolver uses.
            if (trimmedEntry.isEmpty()) continue;

            String[] fields = trimmedEntry.split(":");
            if (fields.length != 2) {
                warnSkip(trimmedEntry, "expected 2 colon-separated fields, found " + fields.length);
                continue;
            }

            String file = fields[0].trim().toLowerCase();
            if (file.isEmpty()) {
                warnSkip(trimmedEntry, "blank file discriminator");
                continue;
            }

            String templateName = fields[1].trim();
            if (templateName.isEmpty()) {
                warnSkip(trimmedEntry, "blank template name");
                continue;
            }
            if (!templateName.matches(NAME_PATTERN)) {
                warnSkip(trimmedEntry, "template name '" + templateName + "' contains a character"
                        + " that is not a letter, digit, underscore, hyphen or dot");
                continue;
            }

            if (templates.containsKey(file)) {
                log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: file '{}' is already configured"
                        + " earlier in the list; the first entry wins",
                        CONFIG_KEY, trimmedEntry, file);
                continue;
            }

            templates.put(file, templateName);
        }

        warnOnPrefixCollisions(templates);

        return Collections.unmodifiableMap(templates);
    }

    /**
     * Summit binds on filename prefix, so one configured name being a prefix of another routes a
     * file to the wrong template with no error anywhere. Warn naming both, and serve anyway —
     * the names are the vendor's, and refusing to export would be a worse failure than a name
     * that needs changing in the Summit tenant.
     */
    private static void warnOnPrefixCollisions(Map<String, String> templates) {
        List<String> files = List.copyOf(templates.keySet());
        for (int i = 0; i < files.size(); i++) {
            for (int j = i + 1; j < files.size(); j++) {
                String a = templates.get(files.get(i));
                String b = templates.get(files.get(j));
                if (a.startsWith(b) || b.startsWith(a)) {
                    log.warn("[SUMMIT-EXPORT] {} prefix collision: '{}' ({}) and '{}' ({}) — Summit"
                            + " binds a retrieved file to an import template by filename prefix, so"
                            + " one of these files can bind to the other's template silently."
                            + " Rename one of the templates in Summit.",
                            CONFIG_KEY, a, files.get(i), b, files.get(j));
                }
            }
        }
    }

    private static void warnSkip(String entry, String reason) {
        log.warn("[SUMMIT-EXPORT] {} entry '{}' skipped: {}", CONFIG_KEY, entry, reason);
    }
}
