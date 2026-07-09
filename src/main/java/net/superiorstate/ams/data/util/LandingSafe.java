package net.superiorstate.ams.data.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

/**
 * Parser-based (Jsoup) sanitizer for PUBLIC, pre-login landing-page HTML.
 * <p>
 * This is a hardened, full-page-capable safelist used for content that is
 * authored by PSP/agency admins but rendered to <b>anonymous</b> visitors before
 * authentication — a real stored-XSS surface. Unlike the weak regex sanitizer it
 * replaces, this parses the HTML and copies only safelisted nodes, so anything
 * not explicitly allowed (script tags, {@code on*} handlers, unknown tags) is
 * dropped structurally rather than pattern-matched.
 * <p>
 * It is deliberately permissive enough to preserve a rich branded landing page:
 * {@code <style>} blocks, font {@code <link rel="stylesheet">}, layout tags,
 * {@code class}/{@code id}, inline {@code style}, {@code #}-anchor navigation,
 * {@code tel:}/{@code mailto:} links, and inline {@code data:image} images. It
 * blocks {@code <script>/<iframe>/<object>/<embed>/<form>}, all event handlers,
 * and dangerous URL/CSS vectors (a CSS-value pass strips {@code expression()},
 * {@code javascript:}/{@code vbscript:} URLs, non-https {@code @import},
 * {@code behavior:}, and {@code -moz-binding}).
 * <p>
 * Sanitize on <b>save</b> (write time), never on render — callers store the
 * cleaned output so the public JSP keeps its "pre-sanitized on save" invariant.
 */
public final class LandingSafe {

    private LandingSafe() {}

    private static final Safelist SAFELIST = buildSafelist();

    /**
     * Sentinel base URI used only to let Jsoup's protocol check accept safe
     * <em>relative</em> links (e.g. {@code href="RequestQuote"},
     * {@code src="/branding/logo.png"}). Combined with
     * {@link Safelist#preserveRelativeLinks(boolean) preserveRelativeLinks(true)},
     * the stored attribute keeps its original relative value while the protocol
     * check passes against the resolved https URL. The sentinel is never emitted
     * into stored output. A relative URL cannot carry a {@code javascript:} scheme,
     * so this does not weaken the XSS guarantee.
     */
    private static final String BASE_URI = "https://ssa-landing.invalid/";

    private static Safelist buildSafelist() {
        return Safelist.relaxed()
                // structural / layout tags a full landing page needs
                .addTags("style", "link", "section", "header", "footer", "nav",
                        "main", "article", "aside", "figure", "figcaption", "hr",
                        "br", "button", "span", "div")
                // layout hooks + inline style on every element
                .addAttributes(":all", "class", "id", "style", "title", "role")
                // font <link rel="stylesheet" href="https://...">
                .addAttributes("link", "rel", "href", "type", "media", "crossorigin")
                .addProtocols("link", "href", "https")
                // anchors: allow #fragment nav, tel:, mailto:, http(s), and relative links
                .addAttributes("a", "href", "target", "rel", "name")
                .addProtocols("a", "href", "#", "http", "https", "mailto", "tel")
                // images incl. inline data:image and relative paths
                .addAttributes("img", "src", "alt", "width", "height", "loading")
                .addProtocols("img", "src", "http", "https", "data")
                // keep relative hrefs/src as authored (safe: relative URLs have no scheme)
                .preserveRelativeLinks(true);
        // on* handlers, <script>/<iframe>/<object>/<embed>/<form> are never listed,
        // so Jsoup drops them (and every unlisted attribute) during the copy.
    }

    // ── CSS-value hardening (applied to <style> block contents and inline style="") ──

    /** Fast pre-check: does this CSS contain any construct we neutralize? */
    private static final Pattern CSS_DANGER = Pattern.compile(
            "(?i)(expression\\s*\\(|javascript\\s*:|vbscript\\s*:|behavior\\s*:|-moz-binding"
                    + "|@import\\s+url\\(\\s*['\"]?\\s*(?!https)|url\\(\\s*['\"]?\\s*(?:javascript|vbscript)\\s*:)");

    // Balanced-paren url(...) matcher for a dangerous scheme — consumes the WHOLE
    // url(...) including its closing paren so no stray ')' is left in saved CSS.
    // The inner alternation tolerates one level of nested parens (e.g. the "(2)"
    // inside url(javascript:alert(2))) so the entire construct is excised cleanly.
    private static final Pattern CSS_URL_SCHEME = Pattern.compile(
            "(?i)url\\(\\s*['\"]?\\s*(?:javascript|vbscript)\\s*:(?:[^()]|\\([^()]*\\))*\\)");
    private static final Pattern CSS_EXPRESSION = Pattern.compile(
            "(?i)expression\\s*\\([^)]*\\)");
    private static final Pattern CSS_BAD_IMPORT = Pattern.compile(
            "(?i)@import\\s+url\\(\\s*['\"]?\\s*(?!https)[^)]*\\)\\s*;?");
    private static final Pattern CSS_BEHAVIOR = Pattern.compile(
            "(?i)behavior\\s*:[^;}]*;?");
    private static final Pattern CSS_MOZ_BINDING = Pattern.compile(
            "(?i)-moz-binding\\s*:[^;}]*;?");

    private static boolean cssIsDangerous(String css) {
        return css != null && !css.isEmpty() && CSS_DANGER.matcher(css).find();
    }

    /**
     * Remove dangerous CSS constructs, leaving balanced/valid CSS. A neutralized
     * {@code url(javascript:...)} is replaced with an empty {@code url()} (paren
     * balanced — no dangling ')'); dangerous at-rules/declarations are dropped whole.
     */
    private static String filterCss(String css) {
        if (css == null || css.isEmpty()) return css;
        String out = css;
        out = CSS_EXPRESSION.matcher(out).replaceAll("");
        out = CSS_URL_SCHEME.matcher(out).replaceAll("url()");
        out = CSS_BAD_IMPORT.matcher(out).replaceAll("");
        out = CSS_BEHAVIOR.matcher(out).replaceAll("");
        out = CSS_MOZ_BINDING.matcher(out).replaceAll("");
        return out;
    }

    /**
     * Sanitize agency/PSP landing HTML for safe public rendering.
     *
     * @param html raw author-supplied HTML (may be null/blank)
     * @return sanitized HTML fragment safe to store and render unescaped, or "" if input is blank
     */
    public static String clean(String html) {
        if (html == null || html.isBlank()) return "";

        Document dirty = Jsoup.parseBodyFragment(html, BASE_URI);
        Document clean = new Cleaner(SAFELIST).clean(dirty);
        clean.outputSettings()
                .prettyPrint(false)   // preserve author formatting / CSS whitespace
                .charset("UTF-8");

        // Post-pass 1: harden <style> block contents
        for (Element style : clean.select("style")) {
            StringBuilder sb = new StringBuilder();
            for (DataNode dn : style.dataNodes()) sb.append(dn.getWholeData());
            String css = sb.toString();
            if (cssIsDangerous(css)) {
                style.html("");                       // clear existing data children
                style.appendChild(new DataNode(filterCss(css)));
            }
        }

        // Post-pass 2: harden inline style="" attributes
        for (Element el : clean.select("[style]")) {
            String css = el.attr("style");
            if (cssIsDangerous(css)) el.attr("style", filterCss(css));
        }

        // Return the body-fragment inner HTML — the stored form that
        // customLanding25.jsp renders unescaped inside .landing-body.
        return clean.body().html();
    }
}
