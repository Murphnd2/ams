# Phase 1 Design Plan — Host-Header Custom Agency Landing Pages

**Status:** READ-ONLY investigation. No code, no migration, no DDL written. This is the design plan for developer review before any Phase-2 build.
**Branch:** `refactor/modernize-architecture`
**Author:** Claude (Opus) · Date: 2026-07-09

---

## 0. Executive summary

The per-agency branded front door is a **direct extension of the existing V043 PSP custom-landing feature**, not a greenfield build. Everything V043 does for one PSP-wide landing — cache HTML in `AmsDataGlobal`, decide in the `login` servlet's unauthenticated branch, sanitize on save, reload the cache after save — has a per-agency analogue.

**Recommended shape:**
- **Hook point:** extend the existing unauthenticated branch inside the `login` servlet (Option A). Do **not** add a second filter — annotation-only `@WebFilter` ordering is undefined and the root path already funnels cleanly through `login`.
- **Storage:** add two columns to `agency` — `landing_host VARCHAR(255)` (unique, indexed, case-folded) and `landing_html MEDIUMTEXT`. One host per agency to start.
- **Lookup/cache:** build a `Map<String,Long> hostToAgencyId` + reuse the already-cached agency list in `AmsDataGlobal`, invalidated by the existing `refreshSalesData(em)` call that `AgencyAction` already fires.
- **Sanitization:** this is a **public, pre-login** page — a real XSS surface. Use a **hardened Jsoup safelist** purpose-built for a full landing page, *not* the V043 regex `sanitizeHtml` (regex is a weak XSS guarantee) and *not* `AutoSafe` (too restrictive). Add a CSP header as defense-in-depth (Phase 2 stretch).
- **Reserved migration version:** **V068** (see §3.3 — tracker lags, verified against live sources).

---

## 1. Current entry-flow map (verified against code)

### 1.1 The request funnel for `/` and the default landing

```
Browser → GET https://<host>/
   │
   ▼
LoginFilter  (@WebFilter("/*"), net.superiorstate.ams.LoginFilter)
   │  path = URI minus contextPath, trailing slashes stripped  → "" for root
   │  • isStaticResource? no
   │  • startsWith("/api/")? no
   │  • uninitialized? (session attr "uninitialized"==0) → redirect /initialize.jsp
   │  • loggedIn (session "local".isAuthenticated())? OR allowedPath?
   │       ALLOWED_ENDPOINTS contains ""  →  allowedPath = true
   │  → chain.doFilter (request proceeds)
   ▼
login servlet  (@WebServlet urlPatterns = {"/login", ""})   ← uniquely owns root
   │  routeLogin():
   │  1. If session "local" != null && isAuthenticated():
   │        redirect by role → BpoHome / ViewHome25 / AgentHome
   │        return
   │  2. Unauthenticated:
   │        global = ctx.getAttribute("global")           ← AmsDataGlobal singleton
   │        if global.isUseCustomLanding() && html non-blank:
   │            request.setAttribute("landingHtml", html)
   │            forward → /WEB-INF/view/authentication/customLanding25.jsp
   │            return
   │        forward → /index.jsp    (default login page)
```

### 1.2 The exact decision point

**`login.routeLogin()` lines 50–61** — specifically the block:

```java
AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
if (global != null && global.isUseCustomLanding()) {
    String html = global.getCustomLandingHtml();
    if (html != null && !html.isBlank()) {
        request.setAttribute("landingHtml", html);
        request.getRequestDispatcher("/WEB-INF/view/authentication/customLanding25.jsp").forward(...);
        return;
    }
}
request.getRequestDispatcher("/index.jsp").forward(request, response);
```

This is the single seam where an unauthenticated request "decides" which landing to show. **The per-agency dispatch inserts here**, *before* the PSP-wide `isUseCustomLanding()` check.

### 1.3 Supporting facts confirmed

- `login` uniquely owns the `""` (root) pattern; **no welcome-file and no competing servlet** (grep confirmed).
- `web.xml` contains **no `<filter-mapping>`** — `LoginFilter` is annotation-only; there is no declared filter ordering.
- `customLanding25.jsp` renders the stored HTML raw via `${requestScope.landingHtml}` inside a fixed header + a Bootstrap login modal (`loginFormModal.jsp`). The comment on line 55 explicitly says *"pre-sanitized on save"* — i.e. the JSP trusts the store; **all sanitization happens at write time.** This is the invariant the per-agency path must preserve.
- The landing header pulls `global.landingHeaderColor` / `logoNavbar` / `favicon` — all PSP-scoped today.

---

## 2. Recommended hook point — servlet vs. filter

### Recommendation: **Option A — extend the `login` servlet's unauthenticated branch.**

Insert host resolution at the top of the unauthenticated block (after the "already authenticated" redirect, before the PSP `isUseCustomLanding()` check):

```
// pseudocode — Phase 2
if not authenticated:
    String host = normalizeHost(request.getServerName());
    if !isPspHost(host):
        Long agencyId = global.getAgencyIdForHost(host);   // null-safe, case-insensitive
        if agencyId != null:
            String agencyHtml = global.getAgencyLandingHtml(agencyId);
            if agencyHtml != null && !agencyHtml.isBlank():
                request.setAttribute("landingHtml", agencyHtml);
                // optionally set agency-scoped logo/colors as request attrs
                forward → customLanding25.jsp
                return
        // non-PSP host, no agency match OR matched-but-empty → fall through
    // existing PSP isUseCustomLanding() logic unchanged
```

### Why A over B (new filter)

| Factor | A: extend `login` servlet | B: new `@WebFilter` |
|---|---|---|
| **Filter ordering** | N/A | ⚠️ Annotation `@WebFilter` order is **undefined by spec**; Tomcat falls back to filter-name alpha order. Getting it reliably before/after `LoginFilter` needs `web.xml` `<filter-mapping>` ordering — new config surface, easy to get wrong. |
| **Scope of concern** | Only the landing decision, exactly where it already lives | Would re-implement auth/allowed-path logic or risk intercepting `/AuthenticateUser`, `/apply/*`, `/proposal/*`, static assets |
| **Blast radius** | Touches one method already responsible for this decision | `/*` filter touches every request; must carefully pass through everything but root |
| **Consistency with V043** | Mirrors the established pattern exactly | Diverges from the existing design |
| **Reuse of `global`** | `global` already fetched here | Filter would refetch context attr |

**Interaction with `LoginFilter`:** unchanged. `LoginFilter` already whitelists `""`, `/login`, `/AuthenticateUser`, `/index.jsp`, static resources, `/apply/*`, `/proposal/*`, etc. Because dispatch happens *inside* `login` (which `LoginFilter` already lets through for any host), **no `LoginFilter` change is required.** The host allow-list is a *landing-selection* concern, not an *access-control* concern — keep it out of `LoginFilter`.

**Option C considered and rejected:** a `ServletContextListener`-level or valve-level dispatch. Overkill; the servlet seam is cleaner and testable.

---

## 3. Agency schema additions

### 3.1 Host column

- **Column:** `landing_host VARCHAR(255) NULL` on `agency`.
- **Uniqueness:** `UNIQUE INDEX uq_agency_landing_host (landing_host)`. MySQL treats multiple `NULL`s as distinct, so agencies with no host don't collide — exactly the desired behavior (only agencies that opt in have a host).
- **Case handling:** hostnames are case-insensitive. **Store normalized lowercase** and normalize the incoming `getServerName()` to lowercase before lookup. (Belt-and-suspenders: even with a case-insensitive collation, normalize in Java so the cache key matches.)
- **One host per agency to start** (see §3.4 for multi-host).

### 3.2 HTML storage — two options evaluated

**Option 1 (RECOMMENDED): `landing_html MEDIUMTEXT NULL` column directly on `agency`.**

- ✅ Dead-simple: one row per agency already loaded in `AmsDataGlobal.getAgencies()`; the HTML rides along.
- ✅ Mirrors V043 exactly (V043 put the PSP HTML in `constant.text_value` `TEXT`; here the natural home is the agency row).
- ✅ `MEDIUMTEXT` (16 MB) comfortably exceeds `TEXT` (64 KB) — landing pages with inline base64 images or large `<style>` blocks won't truncate. (V043 used `TEXT`; prefer `MEDIUMTEXT` here to avoid a future truncation surprise.)
- ⚠️ Loads the blob into the cached `Agency` entity. Acceptable — agencies are few (tens, not thousands) and already fully cached. If blob size becomes a concern, split to a side table later; not needed now.

**Option 2: reuse `proposal_section` (the V044 mechanism).**

V044 already added `agency_id BIGINT` + `fk_ps_agency` + `UNIQUE INDEX uq_ps_psp_agency_type (psp_id, agency_id, section_type)` to `proposal_section`. A new `section_type = 'LANDING'` could store per-agency landing HTML with zero new columns.

- ✅ No schema change to `agency`; reuses an agency-scoped, uniquely-indexed store.
- ✅ Consistent with the proposal-content model the team already knows.
- ❌ **Semantic mismatch:** `proposal_section` is scoped to the *proposal builder* (TITLE/PRICING/FEATURES/CLOSING/CUSTOM), keyed by `psp_id` + `section_type`, and its content is a *proposal fragment*, not a full pre-login page. Overloading it couples the public front door to the sales-proposal schema.
- ❌ The **host** column still has to live somewhere. `proposal_section` has no host concept, so you'd *still* add `landing_host` to `agency` (or a new table). So Option 2 doesn't avoid an `agency` change — it just splits the feature across two tables.
- ❌ Lookup path is more awkward: host→agency (from `agency`) then agency→section (from `proposal_section` filtered by `section_type='LANDING'`). Two joins for a hot pre-login path vs. one cached row.

**Verdict:** Option 1. Keep host + HTML together on `agency`; the feature is self-contained, one cached row answers the whole question, and it reads as a clean extension of V043. Note the tradeoff for the developer: Option 2 wins only if you expect landing pages to become multi-section/versioned like proposals — flag that as an open question (§10).

### 3.3 Reserved migration version — **V068**

Verified against the three live sources (per the "tracker may lag" warning):

| Source | Highest version reported |
|---|---|
| `docs/migrations/` (actual files on disk) | **V067** (`V067__agency_markup_enabled.sql`) |
| `docs/schema_version_migration.sql` | **V067** (registers V001–V067) |
| `docs/analysis/migration_tracker.md` | ⚠️ **Header says "Current Highest Version: V065"**, table rows stop at V065, but the prose *notes* section describes V066 and V067 |

**→ Next reserved version: `V068`.**

**Discrepancy to report:** `migration_tracker.md` is stale — its "Current Highest Version" header and status table stop at V065 while V066/V067 exist on disk, are registered in `schema_version_migration.sql`, and are noted in the tracker's own prose. This matches the MEMORY.md note that the tracker lags. **Phase 2 must update `migration_tracker.md` (header + table) and `schema_version_migration.sql` when V068 lands**, and ideally also fix the V066/V067 table rows while there.

### 3.4 Multi-host — flagged, not built

One host per agency now. If multi-host is later wanted (agency has both `swbd.superiorstate.net` and `admin.swbd.com`):
- Move host off `agency` into a child table `agency_landing_host (id, agency_id FK, host UNIQUE)`.
- Lookup map becomes `host → agencyId` many-to-one (already the map shape below, so the cache code wouldn't change — only the source query and the edit UI would).
- The HTML could stay one-per-agency (all hosts show the same page) or become per-host (bigger change).
Design the Phase-2 lookup around a `Map<String,Long>` so the single-host→multi-host migration is a data/UI change, not a lookup-code rewrite.

---

## 4. Host → agency lookup + caching

### 4.1 Reading the host

- Use **`request.getServerName()`** — already the established pattern in this codebase (`SendProposal`, `ProposalDetail`, `SendInvitation`, `EmailBillingToEmployer`, `VendorManager25`, `BpoPspClients` all use `request.getScheme() + "://" + request.getServerName()`).
- **Proxy consideration:** production runs behind **nginx → Tomcat** (and Cloudflare in front). `getServerName()` reflects the `Host` header Tomcat receives, which is whatever nginx forwards. The standard nginx config `proxy_set_header Host $host;` passes the original vanity host through, so `getServerName()` returns e.g. `swbd.superiorstate.net`. **The app does NOT currently trust `X-Forwarded-Host` anywhere** (grep confirmed — no reference in `src/main/java`). Recommendation: **rely on `getServerName()` only; do not introduce `X-Forwarded-Host` trust** (it's spoofable unless the proxy strips it, and it's unnecessary if nginx forwards `Host`). Phase-2 prerequisite: confirm nginx forwards `Host` unmodified for the vanity subdomains (it must anyway for TLS SNI/routing).

### 4.2 Cache in `AmsDataGlobal`

`AmsDataGlobal` already caches the full agency list (`agencies`, loaded via `SalesDAO.getAgencyList` in `initializeGlobalData` and refreshed in `refreshSalesData`). Hosts change rarely → **cache a derived map alongside it.**

Add to `AmsDataGlobal` (Phase 2):
```java
private Map<String, Long>  hostToAgencyId;    // normalized-lowercase host → agency_id
private Map<Long, String>  agencyLandingHtml; // agency_id → sanitized landing HTML (or store on Agency)
```
Build both in `initializeGlobalData` and `refreshSalesData` from the already-loaded agency list (no extra query if `landing_host`/`landing_html` are columns on `Agency` — they arrive with the entity). Provide null-safe accessors:
```java
public Long   getAgencyIdForHost(String host)  // returns null if host blank/unmatched
public String getAgencyLandingHtml(Long id)
```

### 4.3 Cache invalidation trigger

`AgencyAction.doPost` **already** does, at the end of every action:
```java
global.refreshSalesData(em2);
getServletContext().setAttribute("global", global);
```
`refreshSalesData` already reloads `agencies`. **So editing an agency's host/HTML through `AgencyAction` invalidates the map for free** — Phase 2 just needs `refreshSalesData` to also rebuild `hostToAgencyId` / `agencyLandingHtml` from the freshly loaded list. This mirrors V043's `global.initializeGlobalData(em)` reload-after-save exactly. No new invalidation plumbing.

### 4.4 Case / null / blank handling

- Normalize host: `host == null ? "" : host.trim().toLowerCase(Locale.ROOT)`.
- Empty/blank host → no lookup, fall through to PSP behavior.
- Map lookup miss → fall through (see §6).
- Guard against an agency with a host but blank HTML → treated as "matched-but-empty" (see §6).

---

## 5. Sanitization on a PUBLIC pre-login page (XSS surface)

**Threat model:** the landing HTML is authored by agency/PSP admins but rendered **to the public, before authentication**, raw via `${requestScope.landingHtml}`. A stored-XSS payload here executes in every anonymous visitor's browser on the agency's front door. This is a materially higher-stakes surface than the proposal builder (authenticated, internal).

### 5.1 The two existing sanitizers — analysis

**`UpdatePspSettings.sanitizeHtml` (V043 regex)** — strips `<script>`, `on*=` handlers (double- and single-quoted), and `javascript:` in `href`/`src`. Preserves `<style>` and inline styles.
- ❌ **Regex HTML sanitization is a known-weak XSS guarantee.** Concrete bypasses it misses:
  - Event handlers with **no quotes**: `<img src=x onerror=alert(1)>` — the pattern requires `on...="..."` or `on...='...'`; unquoted attributes slip through.
  - Event handlers split by whitespace/newlines or with weird casing beyond the simple pattern.
  - `javascript:` in contexts other than `href`/`src` (e.g. inside `<style>`: `background:url(javascript:...)` on old engines; CSS `expression()`).
  - **`<style>` is preserved** → CSS-based data exfiltration / UI-redressing / `@import` to attacker CSS; and `<style>` can contain content that, combined with injected markup, enables clickjacking over the login modal.
  - Malformed/mutation-XSS: browsers "fix up" broken HTML into script-executing DOM that the regex never saw as a match.
- **Conclusion:** acceptable-ish for an internal, PSP-admin-only feature; **not** the standard I'd want on an unauthenticated public page.

**`AutoSafe.clean` (Jsoup `Safelist.relaxed()` + a few tags)** — real parser-based safelist.
- ✅ Strong XSS guarantee (parses, drops anything not explicitly allowed).
- ❌ **Too restrictive for a full landing page:** `Safelist.relaxed()` allows basic formatting/links/images/tables but **strips `<style>`, `id`/`class` on most elements, and inline `style` attributes** — a rich branded landing page needs layout CSS. Using it as-is would gut the design.

### 5.2 Recommendation: a **new hardened Jsoup safelist** purpose-built for the public landing page

Build a dedicated safelist (Phase 2, e.g. `LandingSafe.clean()`), parser-based like `AutoSafe` but permissive enough for layout:
- **Allow** structural/layout tags: `div, section, span, p, h1–h6, ul/ol/li, a, img, table/…, header, footer, nav, figure, br, hr, strong, em, b, i, button` (button as a styled anchor, no JS).
- **Allow** `class` and `id` (needed for the JSP's `.ss-fade`/anchor-scroll hooks and for layout).
- **Allow** a **curated inline `style`** — Jsoup can keep `style` but you should constrain it. Jsoup's safelist doesn't validate CSS *values*, so pair `style` allow-listing with a CSS sanitizer or a value regex that blocks `expression(`, `javascript:`, `url(` with non-`https:`/`data:image` targets, and `@import`.
- **`<style>` blocks:** the current design *wants* them (the header CSS + content animations reference classes). Two sub-options:
  - **(preferred, safest)** Disallow author `<style>` blocks; require styling via allowed inline `style` + a fixed set of framework classes the JSP already ships (Bootstrap + `.ss-fade`). Smaller attack surface, no CSS parser needed.
  - **(if `<style>` is required for parity with V043)** Allow `<style>` but run its contents through a CSS allowlist/sanitizer (strip `expression`, `@import`, `url(javascript:…)`, `behavior:`). This is the V043 parity path but with a real CSS filter instead of trusting regex.
- **Protocols:** restrict `a[href]` and `img[src]` to `http, https, mailto, #`-anchors and `data:image/*` for `img` only.
- **Absolutely block:** `<script>`, `<iframe>`, `<object>`, `<embed>`, `<form>` (a form on the landing could phish credentials), `on*` handlers (Jsoup drops all attributes not explicitly allowed, so these die automatically — the key strength over regex), `javascript:`/`vbscript:`/`data:text/html` URLs.

**Residual risk of the chosen approach:** with a parser-based safelist, the dominant residual risks are (a) **CSS-only attacks** if `<style>`/inline `style` are allowed without a CSS-value filter (data exfil via `url()`, clickjacking via positioning over the login modal), and (b) **allowlist over-permissiveness** (e.g. allowing `target`/`rel` without `rel="noopener"`, or allowing `data:` too broadly). Mitigate CSS-positioning/clickjacking by (i) filtering CSS values and (ii) rendering the login modal/header in a container the sanitized content cannot overlay (e.g. the fixed header already sits at `z-index:1000`; ensure sanitized content can't set higher `z-index`/`position:fixed`). **No raw unsanitized HTML may reach the page** — sanitize on **save** (like V043) so the JSP keeps its "pre-sanitized on save" invariant, and consider sanitizing again on render as defense-in-depth for legacy rows.

### 5.3 Response headers (defense-in-depth)

- A **Content-Security-Policy** on the landing response — e.g. `default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; img-src 'self' data: https:; frame-ancestors 'none'` — would neutralize most injected-script and clickjacking vectors even if sanitization is bypassed. **Note:** the current `customLanding25.jsp` loads Bootstrap/Google Fonts from CDNs and uses inline `<style>`/`<script>`, so a strict CSP needs `'unsafe-inline'` for style or a nonce for the page's own scripts — tune carefully.
- `X-Content-Type-Options: nosniff` and `frame-ancestors 'none'` (anti-clickjacking) are cheap wins.
- **Scope call:** CSP is a **Phase-2 stretch / defense-in-depth**, not a blocker for the core feature. Recommend including at least `frame-ancestors 'none'` + `X-Content-Type-Options` in Phase 2; full CSP tuning can be a fast-follow.

---

## 6. Fallbacks & passthrough

| Scenario | Behavior |
|---|---|
| **PSP host** (`superiorstate.net` / `superiorstate.biz`, or any host in the allow-list) | Host dispatch is skipped entirely → today's flow unchanged (PSP `isUseCustomLanding()` → `customLanding25.jsp`, else `/index.jsp`). |
| **Non-PSP host, agency matched, HTML present** | Serve that agency's landing HTML via `customLanding25.jsp`. |
| **Non-PSP host, no agency match** | **Fall through to the default landing** (`/index.jsp` or PSP custom landing). **Recommended over 404** — an unmatched vanity host that still resolves to this server (misconfigured DNS, staging, a removed agency) should show a working login, not a dead end. |
| **Non-PSP host, agency matched but HTML blank** | Same fall-through to default landing. (Agency opted into a host but hasn't authored a page yet.) |

**Recommendation: fall through, never 404.** Rationale: the login and the rest of the app must keep working under any host; a 404 on the front door would strand legitimate users mid-DNS-cutover.

**Post-landing routing under an agency host — confirmed safe:** host dispatch replaces **only the landing render**. Once the visitor clicks Login:
- The modal posts to `/AuthenticateUser` (in `LoginFilter.ALLOWED_ENDPOINTS`, host-agnostic) → normal auth → role redirect to `ViewHome25`/`AgentHome`/`BpoHome`.
- All authenticated servlets, the login POST, and public whitelist paths (`/apply/*`, `/proposal/*`, `/q/*`, `/outlook/*`, static resources) are matched by **path only** in `LoginFilter` and are **completely independent of host**. Nothing about them changes under an agency host.
- An **already-authenticated** user hitting `/` on an agency host still gets the role redirect (the `login` servlet's first branch runs before host dispatch) — unchanged.

So the agency host is a cosmetic front-door swap; the app behaves identically past the landing.

---

## 7. Where agencies set it (PSP-admin UI)

**Exact touchpoints (all already the home of the V067 `markup_enabled` toggle):**

- **JSP:** `src/main/webapp/WEB-INF/view/sales/agencyManager25.jsp`, `#editAgencyModal` (lines 590–~680). The form posts `method="post" action="AgencyAction"` with hidden `action=editAgency` + `agencyId`. The `markupEnabled` checkbox lives at **lines 617–623** — add the host field and HTML editor **immediately adjacent**, in the left "Agency Info" column.
  - **Host field:** `<input name="landingHost">` — with client-side pattern validation (hostname format) and a helper note ("Leave blank to disable the branded landing").
  - **HTML editor:** two viable patterns:
    1. **Textarea in the same form** (simplest) — a `<textarea name="landingHtml">` submitted with the rest of `editAgency`. Sanitize server-side in the `editAgency` branch. Downside: a large HTML blob rides the normal form-redirect POST.
    2. **Separate AJAX save** mirroring V043's `saveLandingHtml` action — a dedicated `AgencyAction action=saveAgencyLanding` (or a new small servlet) with a CKEditor/textarea, matching how the PSP landing is edited today (`UpdatePspSettings` AJAX + CKEditor). **Preferred for parity** and to avoid coupling a big blob to the agency-info save, but more work. Recommend Option 2 if a rich editor (CKEditor, already in the webapp) is wanted; Option 1 if a plain textarea suffices for v1.
- **Servlet:** `src/main/java/net/superiorstate/ams/controller/activity/setup/AgencyAction.java`, the **`case "editAgency"`** branch (lines 118–177). Add:
  - `agency.setLandingHost(normalize(request.getParameter("landingHost")));`
  - `agency.setLandingHtml(LandingSafe.clean(request.getParameter("landingHtml")));`  ← **sanitize-on-save hooks here** (the analogue of `UpdatePspSettings.saveLandingHtml`).
  - `AgencyAction` already enforces `isPspAdmin` (added in V067, lines 27–35) and already fires `global.refreshSalesData(em2)` at the end → cache invalidation is free.
- **Entity:** `Agency.java` — add `landingHost` / `landingHtml` fields + getters/setters (columns `landing_host`, `landing_html`).

**Validation the host field needs:**
- **Format:** valid hostname (letters/digits/hyphens/dots, no scheme, no path). Reject `http://…`, spaces, `*`.
- **Normalization:** lowercase + trim before store (matches lookup normalization).
- **Duplicate-host rejection:** the `UNIQUE INDEX uq_agency_landing_host` enforces it at the DB level; the servlet should **pre-check** (query for an existing agency with that host ≠ this one) and return a friendly error rather than letting the unique-constraint violation bubble up as a 500. Also reject any host that is itself a **PSP host** (an agency must not be able to claim `superiorstate.biz`).

---

## 8. DNS / TLS / Cloudflare — FLAGGED, not solved

A vanity host only reaches the server if DNS + TLS terminate for it. **This is a prerequisite track parallel to the code — not part of the Phase-2 build.** Two tiers:

- **Tier 1 — `*.superiorstate.net` subdomains** (e.g. `swbd.superiorstate.net`): **feasible now.** Add a DNS record (or wildcard `*.superiorstate.net`) in Cloudflare pointing at the same origin, and a TLS cert covering the subdomain. With Cloudflare in front, the edge cert can cover `*.superiorstate.net`; origin can use the existing Let's Encrypt setup (wildcard via DNS-01 using a Cloudflare API token, per the nginx/Let's Encrypt migration noted in CLAUDE.md). nginx must route the subdomain to the same Tomcat and forward `Host` unmodified. **No new infra beyond a DNS entry + wildcard/DNS-01 cert coverage.**
- **Tier 2 — customer-owned domains** (e.g. `admin.swbd.com`): **needs infra not present today.** Requires (a) the agency to `CNAME admin.swbd.com → us`, and (b) a cert that **covers that third-party hostname**. Options: **Cloudflare for SaaS / Custom Hostnames** (Cloudflare issues+manages per-customer certs; cleanest for a multi-tenant SaaS front door) or **per-host ACME** (on-demand Let's Encrypt issuance keyed to the incoming SNI). Either is a real project: onboarding flow, cert automation, validation, renewal, and origin routing.

**Recommendation for the developer:** ship the code + Tier-1 hosts first (immediately demoable on `*.superiorstate.net`, e.g. the SWBD engagement's `swbd.superiorstate.net`); treat Tier-2 custom domains as a separate infra epic gated on Cloudflare-for-SaaS (or ACME-on-demand) being stood up. The **application code is identical for both tiers** — it only ever reads `getServerName()`; the difference is purely DNS/TLS/onboarding.

---

## 9. Files that would change in Phase 2 (list only — no edits made)

**Migration (new):**
- `docs/migrations/V068__agency_landing_host.sql` — `ALTER TABLE agency ADD COLUMN landing_host VARCHAR(255) NULL`, `ADD COLUMN landing_html MEDIUMTEXT NULL`, `ADD UNIQUE INDEX uq_agency_landing_host (landing_host)`, self-register in `schema_version`.
- `docs/schema_version_migration.sql` — append the `('V068', …)` row.
- `docs/analysis/migration_tracker.md` — bump header to V068, add rows for V066/V067/V068 (fix the lag).

**Java:**
- `model/sales/agency/Agency.java` — add `landingHost`, `landingHtml` fields + accessors.
- `data/AmsDataGlobal.java` — add `hostToAgencyId` / `agencyLandingHtml` maps, build in `initializeGlobalData` + `refreshSalesData`, add null-safe accessors + a `isPspHost(host)` / host-allow-list reader.
- `controller/authentication/login.java` — insert host-dispatch block in the unauthenticated branch.
- `controller/activity/setup/AgencyAction.java` — read/sanitize/write `landingHost` + `landingHtml` in `case "editAgency"` (and/or a new `saveAgencyLanding` AJAX action); pre-check duplicate/PSP host.
- `data/util/LandingSafe.java` (**new**) — hardened Jsoup safelist for full-page public HTML (+ optional CSS-value filter).
- (optional) a small helper to read the PSP-host allow-list from `ssa.properties` via `AppConfig` (e.g. `PSP_HOSTS=superiorstate.net,superiorstate.biz`).

**JSP:**
- `WEB-INF/view/sales/agencyManager25.jsp` — add host input + HTML editor to `#editAgencyModal` (adjacent to `markupEnabled`).
- (maybe) `WEB-INF/view/authentication/customLanding25.jsp` — optionally parametrize header logo/colors per-agency (Phase-2 nice-to-have; today they're PSP-scoped). Add security headers here or in the servlet.

**Config (ops, not code):**
- `ssa.properties` on each host — add `PSP_HOSTS` (see §2/§10). nginx vhost + DNS + TLS for Tier-1 subdomains (ops track, §8).

---

## 10. Options with tradeoffs & open questions for the developer

**Decisions with a clear recommendation (confirm or override):**
1. **Hook point** → *Option A, extend `login` servlet.* (vs. new filter — rejected on ordering/blast-radius.)
2. **HTML storage** → *Option 1, `agency.landing_html MEDIUMTEXT`.* (vs. reuse `proposal_section` — rejected on semantic mismatch; still needs a host column anyway.)
3. **Sanitizer** → *new hardened Jsoup `LandingSafe`.* (vs. V043 regex — too weak for public; vs. `AutoSafe` — too restrictive.)
4. **Unmatched/empty host** → *fall through to default landing, never 404.*
5. **Migration version** → **V068.**

**Open questions requiring a developer decision:**
- **A. PSP-host allow-list location.** Recommended: **`ssa.properties` via `AppConfig`** — e.g. `PSP_HOSTS=superiorstate.net,superiorstate.biz` — read once at startup, **not hardcoded**. It's infra-shaped (differs per installation: production/demo/bpo/master may have different PSP hosts), matches how `WEB_PATH`, `SAVE_PATH`, etc. are handled, and needs no DB round-trip on the hot path. Alternative: a DB `Constant` (editable without a redeploy, cached in `AmsDataGlobal` like the others) — pick DB-constant if PSP admins should edit it in-app; pick `ssa.properties` if it's an ops-set infra value. **Recommend `ssa.properties`** given it's an infra/DNS concern. Should subdomain matching be exact, or should any subdomain of a PSP apex (`*.superiorstate.biz`) also count as a PSP host? (Recommend: treat exact PSP apex + `www` as PSP; everything else non-PSP so agency subdomains work.)
- **B. `<style>` blocks in agency HTML** — allow (with a CSS-value filter, V043 parity) or disallow (inline `style` + shipped framework classes only, smaller attack surface)? Affects the `LandingSafe` design.
- **C. HTML editor UX** — plain `<textarea>` in the `editAgency` form (simple) vs. a dedicated CKEditor + AJAX save mirroring the PSP landing (`UpdatePspSettings.saveLandingHtml`)? CKEditor is already in the webapp.
- **D. Per-agency header chrome** — should the fixed header logo/colors on `customLanding25.jsp` also become agency-scoped, or keep the PSP chrome for v1? (Recommend PSP chrome v1; agency logo/colors as fast-follow — the agency's own HTML body already carries most branding.)
- **E. Multi-host** — confirm one-host-per-agency for v1 (recommended). If multi-host is near-term, move host to a child table now (§3.4) to avoid a second migration.
- **F. CSP scope** — include full CSP in Phase 2, or ship `frame-ancestors 'none'` + `nosniff` now and tune CSP as a fast-follow? (Recommend the latter.)
- **G. Should an agency landing be gated by a per-agency enable flag** (like `markup_enabled`), or is "has a non-blank host + HTML" sufficient to be considered enabled? (Recommend: presence of host+HTML = enabled; no extra flag needed, keeps the model simple.)

---

## Guardrail compliance
- Read-only: no files edited, no migration created, no DDL run, no git mutation. This document is the sole output.
- PSP-host and unmatched-host behavior is explicitly preserved unchanged (§6).
- Public HTML treated as an XSS surface; no raw unsanitized HTML reaches the page (§5).
- Phase-2 ships via a versioned migration (V068) + `update.sh` release — out of scope here.
