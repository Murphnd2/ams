# Phase 2 · Phase A — Read-Only Confirmation Report
## Host-Header Custom Agency Landing Pages

**Status:** READ-ONLY confirmation pass complete. No code, migration, DDL, or git mutation written. The sanitizer was prototyped and run in a scratch directory only. **Awaiting developer approval before Phase B.**
**Branch:** `refactor/modernize-architecture` · Date: 2026-07-09

**Headline result:** every locked assumption is confirmed against the live code, and the sanitizer de-risk **passed** — `LandingSafe` (Jsoup) faithfully preserves the current landing HTML. **Recommendation: apply `LandingSafe` to BOTH the new agency landing and the existing PSP landing (replacing the weak regex), per decision #6.** The fallback path is not needed.

---

## Step 1 — Login seam confirmed

`controller/authentication/login.java` · `routeLogin(...)` structure verified:
- **Lines 25–48:** authenticated-redirect branch (`local.isAuthenticated()` → role redirects `BpoHome`/`ViewHome25`/`AgentHome`), inside a `try/catch(Exception ignored)`.
- **Line 50:** comment `// Unauthenticated — show custom landing or default login`.
- **Lines 51–60:** the existing PSP block — `global.isUseCustomLanding()` → forward `customLanding25.jsp`.
- **Line 61:** default `forward("/index.jsp")`.

**Host-dispatch insertion point → between line 50 and line 51** (immediately after the authenticated branch's `catch`, before the `AmsDataGlobal global = ...` fetch). The new block will normalize `request.getServerName()`, and only for a non-PSP host with a resolved agency + non-blank `landing_html` will it set `landingHtml` + a `whiteLabel`/`hideChrome` request attribute, set the two security headers, and forward to `customLanding25.jsp`. Any miss falls through untouched to the existing lines 51–61. `LoginFilter` is **not** modified (it already whitelists `""`/`/login`/`/AuthenticateUser` host-agnostically).

---

## Step 2 — Cache + agency plumbing confirmed

`data/AmsDataGlobal.java`:
- **Line 115:** `private List<Agency> agencies;` (the cached list).
- **Line 180** (in `initializeGlobalData`): `setAgencies(SalesDAO.getAgencyList(em, getPsp().getId().intValue()));` — populated here.
- **Line 946** (in `refreshSalesData`): `setAgencies(...)` — reloaded here.
- **Accessor `getAgencies()`** at line 712.
- **`getConstantValue`/`AppConfig`** available for the `PSP_HOSTS` read.

`model/sales/agency/Agency.java`:
- **Suppressed accessor confirmed** (V057): `isSuppressed()` at lines 90–92; column `suppressed` at lines 24–25. The `hostToAgencyId` builder will **skip suppressed agencies** as specified.
- `getId()` returns `Long`; `markup_enabled` (V067) present at lines 27–28, 98–104 — the new `landing_host`/`landing_html` fields sit alongside these.

`controller/activity/setup/AgencyAction.java`:
- **Lines 31–35:** `isPspAdmin` server-side guard already present (V067) — returns `SC_FORBIDDEN` for non-admins. The landing save inherits this gate.
- **Lines 300–309:** after every action, `global.refreshSalesData(em2)` runs and re-sets the `global` context attribute → **cache invalidation is free**; the new `hostToAgencyId` map rebuilds inside `refreshSalesData`.
- **`editAgency` case, lines 118–177**, reads `markupEnabled` at line 127 — the `landing_host` read/validate/store attaches here.

**Plan:** add `Map<String,Long> hostToAgencyId` + a `rebuildHostMap()` called from `initializeGlobalData` (after line 180) and `refreshSalesData` (after line 946); add `getAgencyIdForHost(String)`, a landing-HTML accessor, `isPspHost(String)`, and a `PSP_HOSTS` reader.

---

## Step 3 — Edit-UI structure confirmed

**Mirror source** — `WEB-INF/view/a/general/smtpSettingsMod25.jsp` (the Admin → Settings → Features "Custom Landing Page" panel):
- **Lines 170–188:** the panel — a raw `<textarea id="landingHtmlEditor" rows="12">`, a **Preview** button (`toggleLandingPreview()`), and a **Save HTML** button (`saveLandingHtml()`), plus a preview `<iframe>` and a status `<div>`.
- **Lines 391–405:** `toggleLandingPreview()` swaps the textarea for an `<iframe srcdoc=...>`.
- **Lines 408–429:** `saveLandingHtml()` — `fetch('UpdatePspSettings', {method:'POST', body:'action=saveLandingHtml&landingHtml='+encodeURIComponent(html)})`, expects `{status:'ok'}` JSON.
- Server handler = `UpdatePspSettings.saveLandingHtml` (lines 224–262): sanitize → upsert `CUSTOM_LANDING_HTML` constant `text_value` → `global.initializeGlobalData(em)` → JSON.

**Attach target** — `WEB-INF/view/sales/agencyManager25.jsp` `#editAgencyModal`:
- **Lines 590–595:** `<form method="post" action="AgencyAction">` with hidden `action=editAgency` + `agencyId=${selectedAgency.getId()}`.
- **Lines 617–623:** the V067 `markupEnabled` checkbox in the left "Agency Info" column — the **host input** attaches immediately adjacent here (part of the `editAgency` form).
- The **landing-HTML panel** (textarea + Preview + Save HTML) attaches as a full-width row inside the modal body, wired to a **new AJAX action**.

**New servlet action name → `saveAgencyLandingHtml`**, dispatched early in `AgencyAction.doPost` (after the isPspAdmin guard, before the main `switch`), mirroring how `UpdatePspSettings.doPost` branches on `action` before the main save. **AJAX endpoint → `POST AgencyAction` with body `action=saveAgencyLandingHtml&agencyId=<id>&landingHtml=<encoded>`**, returning `{status:'ok'|'error'}`. Rationale for the split (matching the PSP pattern): the host + flags ride the normal `editAgency` form save; the large HTML blob saves separately via AJAX so it doesn't bloat the form-redirect POST.

---

## Step 4 — Sanitizer de-risk (the critical step) — **PASSED**

### Method
- **Corpus:** `docs/sample-landing-content.html` (22,712 chars) — a realistic full landing page representative of what agencies will paste. It contains: a font `<link rel="stylesheet" href="https://fonts.googleapis.com/…">`, one large `<style>` block (`:root` CSS variables, `@keyframes`, `@media`, `linear-gradient`/`radial-gradient`, `rgba()`, `content:'\2192'`, pseudo-elements), `<section>/<div>/<span>/<h1–h4>/<p>/<ul>/<li>/<a>/<br>/<strong>`, heavy `href="#fragment"` anchor nav, a `tel:` link, an external `http://kb.superiorstate.net` link with `target=_blank rel=noopener`, numeric HTML entities + emoji, and a trailing `<script>` block.
- **Prototype:** a real `LandingSafe` built on **Jsoup 1.17.2** (the project's pinned version, from `~/.m2`), compiled and run against the corpus + an injected-attack fixture. Safelist = `Safelist.relaxed()` + `style, link, section, header, footer, nav, main, article, aside, figure, figcaption, hr, button, span, div`; `class/id/style/title/role` on `:all`; `link[rel,href,type,media]` (href https-only); `a[href]` protocols `#,http,https,mailto,tel`; `img[src]` protocols `http,https,data`. Plus a CSS-value hardening pass over `<style>` contents and inline `style=`.

### Results (26 preservation checks, all effectively PASS)
Structural tag counts, input → output:

| Node | in → out | |
|---|---|---|
| `<div` | 47 → 47 | ✅ |
| `<a ` | 25 → 25 | ✅ |
| `<h3` | 10 → 10 | ✅ |
| `<style` | 1 → 1 | ✅ (full CSS body preserved — the key Jsoup risk) |
| `<link` | 1 → 1 | ✅ (survived body-fragment parse) |
| `<ul` / `<li` | 2 → 2 / 12 → 12 | ✅ |
| `service-icon` emoji divs | 10 → 10 | ✅ (emoji intact as literal chars) |
| `<script` | 1 → **0** | ✅ stripped |

CSS specifics preserved verbatim: `:root`, `--teal: #005F73`, `@keyframes pulse`, `@media (max-width: 768px)`, `linear-gradient(135deg`, `radial-gradient(circle`, `rgba(122, 155, 60, 0.08)`, `content: '\2192'`. Anchor nav preserved: `href="#services"`, `href="#quote"`, `tel:8008797752`, `kb.superiorstate.net`, `target="_blank"`, `rel="noopener"`.

### The only diffs (all benign)
```
1) &display=swap  →  &amp;display=swap   (Jsoup escapes bare & in href; browser decodes identically — URL still valid)
2) <!-- … -->     →  (removed)           (HTML comments dropped by the Cleaner — no value lost)
3) &#128176; etc. →  💰 (literal UTF-8)   (numeric entities decoded; harmless under customLanding25.jsp charset=UTF-8)
4) <script>…</script> → (removed)        (intended)
```
Size 22,712 → 21,557 chars (~5%), entirely from the removed script/comments + whitespace normalization. **No structural, CSS, class, link, or content loss.**

### Attack fixture (all PASS)
`onclick`, `onerror` handlers → stripped; `<iframe>`, `<form>` → stripped; `javascript:` href → stripped; `url(javascript:…)` in both inline `style` and `<style>` → neutralized; non-https `@import url('http://evil…')` → stripped; benign `color:red` inline style and `.a{color:blue}` rule → **kept**.

### Two minor Phase-B refinements (non-blocking, noted for implementation)
1. The CSS `url(javascript:…)` filter leaves a cosmetic stray `)` (`background:url())`) — tighten the regex to balance parens. **Not a security issue** (the `javascript:` payload is gone).
2. Jsoup drops HTML comments — fine for landing pages; just documenting the behavior.

### Verdict
`LandingSafe` preserves the current landing HTML with zero meaningful loss and blocks every tested vector. **Apply it to both landings** (agency + PSP), replacing `UpdatePspSettings.sanitizeHtml`'s regex. Fallback path (reuse regex for agency) is **not** required.

---

## Step 5 — Migration version confirmed

- `docs/migrations/` highest on disk = **V067** (`V067__agency_markup_enabled.sql`).
- `docs/schema_version_migration.sql` registers through **V067** (line 90).
- **V068 is free** → the migration is `V068__agency_landing_host.sql`.
- **Tracker staleness (to fix in Phase B):** `docs/analysis/migration_tracker.md` header says *"Current Highest Version: V065"* and its status table stops at V065, though its prose notes describe V066/V067. Phase B bumps the header to V068 and adds table rows for V066, V067, V068.

---

## Step 6 — `AppConfig` config-read pattern confirmed

`AppConfig.get(String key, String defaultValue)` (line 62) reads from the `ssa.properties` `Properties` loaded at startup (`load()`, lines 37–57; resolves `-Dssa.config` or `{catalina.base}/conf/ssa.properties`). This is exactly how `SAVE_PATH`, `BRANDING_PATH`, `VIDEO_PATH` are read in `AmsDataGlobal.setConstants` (lines 293–295). **`PSP_HOSTS` follows the same path:** `AppConfig.get("PSP_HOSTS", "superiorstate.net,superiorstate.biz")`, split on comma, trimmed + lowercased into an exact-match set. (Note: `WEB_PATH` is a DB constant, not a property — `PSP_HOSTS` intentionally uses the `ssa.properties`/`SAVE_PATH` pattern instead, since it's an infra/DNS value that differs per installation.)

---

## Final exact change list for Phase B (no edits made yet)

**Migration + docs**
1. `docs/migrations/V068__agency_landing_host.sql` *(new)* — `ALTER TABLE agency ADD COLUMN landing_host VARCHAR(255) NULL`, `ADD COLUMN landing_html MEDIUMTEXT NULL`, `ADD UNIQUE INDEX uq_agency_landing_host (landing_host)`; header note re prerequisites V057/V067; self-register `INSERT IGNORE … 'V068'`. Match V067 style.
2. `docs/schema_version_migration.sql` — append `('V068', …)` row.
3. `docs/analysis/migration_tracker.md` — header → V068; add rows for V066/V067/V068 + a V068 prose note.
4. `docs/deployment_backlog.md` — new D-item: set `PSP_HOSTS` in `ssa.properties` on each environment (default `superiorstate.net,superiorstate.biz`).

**Java**
5. `model/sales/agency/Agency.java` — add `landingHost` (`@Column(name="landing_host")`) + `landingHtml` (`@Column(name="landing_html", columnDefinition="MEDIUMTEXT")`) fields + accessors. (Eager load retained — acceptable at this scale.)
6. `data/AmsDataGlobal.java` — add `Map<String,Long> hostToAgencyId`; `rebuildHostMap()` (skips suppressed, lowercases host, non-blank only) called from `initializeGlobalData` (after line 180) + `refreshSalesData` (after line 946); `getAgencyIdForHost(String)`, landing-HTML accessor, `PSP_HOSTS` reader + `isPspHost(String)` (exact match).
7. `data/util/LandingSafe.java` *(new)* — the proven Jsoup sanitizer above (with the paren-balance refinement).
8. `controller/authentication/login.java` — host-dispatch block inserted between lines 50–51.
9. `controller/activity/setup/AgencyAction.java` — early `saveAgencyLandingHtml` AJAX branch (sanitize via `LandingSafe`, upsert `landing_html` by `agencyId`, JSON response); in `editAgency` case, read/validate/store `landing_host` (format check, lowercase+trim, duplicate-host pre-check → friendly error, reject any `PSP_HOSTS` value).
10. `controller/user/UpdatePspSettings.java` — `saveLandingHtml` switches from `sanitizeHtml` (regex) to `LandingSafe.clean` (decision #6). *(Regex method may be retained/deprecated; no behavior change beyond stronger sanitization.)*

**JSP**
11. `WEB-INF/view/authentication/customLanding25.jsp` — gate the fixed `.landing-header` chrome (lines 44–53) on `${requestScope.whiteLabel}`/`hideChrome`; keep login modal + scroll scripts; PSP path unchanged.
12. `WEB-INF/view/sales/agencyManager25.jsp` — add host input (adjacent to markup toggle, lines 617–623) + landing-HTML panel (textarea + Preview + Save HTML AJAX → `AgencyAction action=saveAgencyLandingHtml`) to `#editAgencyModal`.

**Response headers** (login servlet, agency-landing branch only): `Content-Security-Policy: frame-ancestors 'none'` + `X-Content-Type-Options: nosniff`.

---

## Guardrail compliance (Phase A)
- Read-only: no repo files edited, no migration/DDL created, no git mutation. The sanitizer prototype + this report live in scratch/`docs/analysis` only.
- No decisions re-litigated — this pass confirmed the locked design against live code and de-risked the sanitizer.
- Out-of-scope items (proposal/application flow, `LoginFilter`, `AgentHome`, multi-host, full CSP, DNS/TLS) were not touched.

**HARD STOP — awaiting approval to begin Phase B.**
