package net.superiorstate.ams.data.resolver;

import net.superiorstate.ams.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * W2 (session 52) — the <b>optional</b> element block Summit file 1 (Employer Demographic) appends
 * after its eleven fixed columns. A sibling of {@link SummitCdhElementResolver}, deliberately the
 * same shape: same config mechanism, same defaulting, same refusal on a bad token.
 * <p>
 * <b>AMS conforms to the Summit template, never the reverse.</b> The template decides which optional
 * elements it maps and in what order, and the file's column count must match the template's exactly
 * or the whole file is rejected (observed 2026-09-12: "The validated file contains 8 columns. The file
 * template defines 16 columns."). So these columns are never emitted unconditionally.
 * <p>
 * ⚠️ <b>Unset or blank means emit nothing extra, and that is the default.</b> With {@link #CONFIG_KEY}
 * absent, {@link #configured()} returns an empty list and file 1's bytes are identical to what it
 * emitted before this class existed. Kevin enables the tokens per installation once that
 * installation's template has the columns.
 * <p>
 * <b>Config key: {@code SUMMIT_EMPLOYER_OPTIONAL_ELEMENTS}.</b> An ordered, comma-separated list of
 * the tokens in {@link Element}, in the template's column order:
 * <pre>
 * SUMMIT_EMPLOYER_OPTIONAL_ELEMENTS=CONTACT_TITLE,CONTACT_NAME,CONTACT_EMAIL
 * </pre>
 * <b>Config order is emit order</b> — the operator's token order is their template's column order.
 * <p>
 * ⚠️ <b>An unrecognised token is a REFUSAL, not a skip</b>, for the reason
 * {@link SummitCdhElementResolver} gives: Summit binds optional elements positionally, so a dropped
 * token shifts every element after it into the wrong field, and Summit accepts a wrong value without
 * complaint. Parsing never throws; a rejection is returned as data on {@link Parsed}.
 * <p>
 * ⚠️ <b>An empty cell CLEARS the stored value on this template</b> (proven 2026-09-12: a blank
 * {@code Primary Contact Email} wiped an employer's Email; a blank {@code Employer Plan Name} wiped
 * that field). This is the opposite of Demographics. {@link #values} therefore logs a {@code WARN}
 * naming each configured element whose value resolves to empty, so a re-send that will blank a
 * field in Summit is visible in the log. Log only — whether file 1 should re-send at all is T248.
 * <p>
 * <b>Sources.</b> {@code Primary Contact Title} ← {@code contact_title}; {@code Primary Contact Email}
 * ← {@code contact_email}; {@code Primary Contact Name} ← {@code contact_first_name} and
 * {@code contact_last_name} joined by one space, each part trimmed, a blank part omitted with no
 * stray space, both blank → empty. All four keys live in the {@code general} package's
 * {@code primary_contact} section. ⚠️ A malformed name imports successfully and stays wrong in
 * Summit — there is no downstream check. {@code Primary Contact Phone} is deliberately not an
 * element: the application does not collect it.
 */
public final class SummitEmployerElementResolver {

    private static final Logger log = LogManager.getLogger(SummitEmployerElementResolver.class);

    /** The config key this resolver reads. Named so callers can cite it in errors. */
    public static final String CONFIG_KEY = "SUMMIT_EMPLOYER_OPTIONAL_ELEMENTS";

    // Application answer keys (packages/general.json, section primary_contact).
    static final String FIELD_CONTACT_FIRST_NAME = "contact_first_name";
    static final String FIELD_CONTACT_LAST_NAME = "contact_last_name";
    static final String FIELD_CONTACT_TITLE = "contact_title";
    static final String FIELD_CONTACT_EMAIL = "contact_email";

    private SummitEmployerElementResolver() {}

    /** One optional element the {@code Employer Demographic} template may map. */
    public enum Element {
        CONTACT_TITLE, CONTACT_NAME, CONTACT_EMAIL
    }

    /**
     * The parsed element list, or the reason it could not be parsed. Never both, never neither.
     */
    public static final class Parsed {
        private final List<Element> elements;
        private final String rejection;

        private Parsed(List<Element> elements, String rejection) {
            this.elements = elements;
            this.rejection = rejection;
        }

        /** The elements in emit order. Empty when the key is unset — the default, and not an error. */
        public List<Element> getElements() {
            return elements;
        }

        /** Null when the list parsed. Otherwise a message naming the bad token, fit for an error page. */
        public String getRejection() {
            return rejection;
        }

        public boolean isRejected() {
            return rejection != null;
        }
    }

    /**
     * The configured optional elements, in emit order.
     *
     * @return a {@link Parsed} carrying an unmodifiable list — empty when {@link #CONFIG_KEY} is
     *         absent or blank — or a rejection naming the first unrecognised token. Never null.
     */
    public static Parsed configured() {
        String raw = AppConfig.get(CONFIG_KEY);
        if (raw == null || raw.isBlank()) {
            return new Parsed(Collections.emptyList(), null);
        }

        List<Element> elements = new ArrayList<>();
        for (String entry : raw.split(",")) {
            String token = entry.trim();
            // A stray or trailing comma contributes no element and cannot shift a column.
            if (token.isEmpty()) continue;

            Element element;
            try {
                element = Element.valueOf(token.toUpperCase());
            } catch (IllegalArgumentException e) {
                StringBuilder supported = new StringBuilder();
                for (Element known : Element.values()) {
                    if (supported.length() > 0) supported.append(", ");
                    supported.append(known.name());
                }
                return new Parsed(Collections.emptyList(),
                        CONFIG_KEY + " contains an unrecognised element token '" + token + "'."
                                + " Supported tokens are: " + supported + ". The export refuses rather"
                                + " than skipping the token, because Summit binds optional elements by"
                                + " position: dropping one would shift every element after it and load"
                                + " each value into the wrong field, which Summit accepts without"
                                + " complaint. Fix the token, then restart Tomcat.");
            }
            if (elements.contains(element)) {
                return new Parsed(Collections.emptyList(),
                        CONFIG_KEY + " lists element '" + element.name() + "' more than once. Each"
                                + " element maps to exactly one column in the Summit template, so a"
                                + " repeat is a configuration error rather than two columns. Remove"
                                + " the duplicate, then restart Tomcat.");
            }
            elements.add(element);
        }

        return new Parsed(Collections.unmodifiableList(elements), null);
    }

    /**
     * The value of each configured element, resolved from the application answers. Every configured
     * element is present in the returned map (possibly as {@code ""}) so the caller can append
     * positionally; nothing is ever omitted. Logs a {@code WARN} for each configured element whose
     * value is empty, because on this template an empty cell clears what Summit already holds.
     *
     * @param configured the elements in emit order, from {@link #configured()}
     * @param answers    the application answer map keyed by field key; may be null
     * @param prospectId named in the warning so the log line identifies the employer
     * @return an {@link EnumMap} with exactly the configured elements as keys, unmodifiable
     */
    public static Map<Element, String> values(List<Element> configured, Map<String, String> answers,
                                              long prospectId) {
        Map<Element, String> values = new EnumMap<>(Element.class);
        for (Element element : configured) {
            String value;
            switch (element) {
                case CONTACT_TITLE:
                    value = trimmed(answers, FIELD_CONTACT_TITLE);
                    break;
                case CONTACT_NAME:
                    value = contactName(trimmed(answers, FIELD_CONTACT_FIRST_NAME),
                                        trimmed(answers, FIELD_CONTACT_LAST_NAME));
                    break;
                case CONTACT_EMAIL:
                    value = trimmed(answers, FIELD_CONTACT_EMAIL);
                    break;
                default:
                    value = "";
            }
            if (value.isEmpty()) {
                log.warn("[SUMMIT-EXPORT] Employer Demographic for prospect {}: configured element {}"
                                + " resolves to an empty value. On this template an empty cell CLEARS"
                                + " the value Summit already holds for that field; a re-send will blank"
                                + " it.",
                        prospectId, element.name());
            }
            values.put(element, value);
        }
        return Collections.unmodifiableMap(values);
    }

    /**
     * {@code Primary Contact Name} from the two AMS answers. Both present → {@code first + " " + last};
     * one blank → the other alone, no leading or trailing space; both blank → {@code ""}. Parts are
     * expected already trimmed.
     */
    static String contactName(String first, String last) {
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return first + " " + last;
    }

    private static String trimmed(Map<String, String> answers, String key) {
        String value = answers == null ? null : answers.get(key);
        return value == null ? "" : value.trim();
    }
}
