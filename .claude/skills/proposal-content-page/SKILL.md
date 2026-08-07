---
name: proposal-content-page
description: >
  Generate styled HTML content blocks for the AMS proposal builder's custom pages.
  Use this skill whenever the user wants to create, convert, or adapt content into
  proposal-ready HTML pages — whether from a URL, brochure text, a Gamma presentation,
  marketing copy, or a content description. Also use when the user says things like
  "make a proposal page for...", "convert this to a custom page", "create an HTML block
  for the proposal", "style this as a proposal insert", or references proposal custom
  pages in any way. If the user mentions proposal pages, custom sections, or HTML blocks
  for proposals, use this skill.
---

# Proposal Content Page Generator

Generate self-contained HTML blocks that paste directly into the AMS proposal builder's
custom page sections. Each block is a polished, print-ready page designed to look like a
professionally designed card inset on the proposal's light gray background.

## Where These Pages Live

Custom pages render inside the proposal viewer at this location in the DOM:

```html
<body style="background: #f8f9fa;">
  <div class="proposal-container" style="max-width: 900px; margin: 0 auto;">
    ...
    <div class="proposal-section custom-section">
      <!-- YOUR HTML BLOCK GOES HERE — rendered unescaped -->
    </div>
    ...
  </div>
</body>
```

The host page uses Bootstrap 5 and has its own styles, so everything you write must be
CSS-scoped to avoid collisions.

## Server-Side Sanitization

The `sanitizeHtml()` method on the server will strip:
- `<script>` tags (removed entirely)
- Inline event handlers (`onclick`, `onload`, `onerror`, etc.)
- `javascript:` protocol in links

It **preserves** `<style>` blocks — this is intentional and how we deliver CSS.

Never use `<script>`, event handlers, or `javascript:` links. They will be silently removed
and the page will break.

## HTML Block Structure

Every block follows this exact skeleton:

```html
<style>
  @import url('https://fonts.googleapis.com/css2?family=Source+Sans+3:ital,wght@0,400;0,600;0,700;1,400&display=swap');
  .PREFIX { --s:1; /* scale factor — adjust to fill page */ /* ...colors, font... */ }
  .PREFIX h1, .PREFIX h2, .PREFIX h3 { color: var(--white); }
  .PREFIX h1 { font-size:calc(24pt * var(--s)); margin:0 0 calc(0.5rem * var(--s)) 0; }
  .PREFIX .body { font-size:calc(11pt * var(--s)); }
  /* ... all sizes use calc(Xunit * var(--s)) ... */
  @media print { .PREFIX .card-inset { min-height:auto; } }
  @media (max-width:700px) { /* responsive overrides */ }
</style>
<div class="PREFIX" style="page-break-before:always; padding:0.5in 0;">
  <div class="card-inset">
    <!-- page content here -->
  </div>
</div>
```

Key rules:
- **`--s` scale factor** — defined on the prefix class, default `1`. Every font-size, padding,
  margin, gap, and icon dimension uses `calc(value * var(--s))`. To make everything bigger,
  change to `--s:1.05`. To shrink, use `--s:0.95`. This one number scales the entire page
  proportionally without touching individual rules.
- **What NOT to scale** — `min-height` (tied to physical page), `border-radius` (aesthetic),
  unitless `line-height` multipliers (already relative to scaled font-size), `border-top` on
  cards (accent, stays thin).
- **Fragment only** — no `<html>`, `<head>`, `<body>` wrappers
- **Scoped class prefix** — every CSS selector starts with `.PREFIX` (e.g., `.fsa3`, `.benefits1`, `.cobra2`). Pick a short, unique prefix per block.
- **`@import` inside `<style>`** — external `<link>` tags won't work reliably in this context
- **`page-break-before:always`** on the outer div so each block starts a new printed page
- **`padding:0.5in 0`** on the outer div for comfortable top/bottom margins in print
- **Set heading color explicitly — never rely on inheriting it from `.card-inset`.** The host
  page loads Bootstrap 5, and Bootstrap has its own rule matching heading elements directly
  (`h1, h2, h3, h4, h5, h6 { color: var(--bs-heading-color); }`). A directly-matching rule always
  wins over an inherited value, no matter how low its specificity is — so if anything in the
  cascade ever gives `--bs-heading-color` a concrete value, every `<h1>`/`<h2>`/`<h3>` inside your
  card silently switches to the host's heading color instead of the white you intended, even
  though `.card-inset` itself correctly sets `color: var(--white)`. The skeleton above already
  includes the fix — `.PREFIX h1, .PREFIX h2, .PREFIX h3 { color: var(--white); }` — keep it in
  every block rather than dropping it as redundant.

## The Inset Card

The `.card-inset` is the visual foundation — a rounded rectangle that fills the page and
floats on the light background:

```css
.PREFIX .card-inset {
  background: var(--navy);           /* rich color contrasting #f8f9fa */
  -webkit-print-color-adjust: exact; /* REQUIRED -- see "Printing Backgrounds" below */
  print-color-adjust: exact;         /* REQUIRED -- see "Printing Backgrounds" below */
  border-radius: 12px;              /* aesthetic — don't scale */
  padding: calc(2.5rem * var(--s)) calc(2.75rem * var(--s)) calc(2.25rem * var(--s));
  color: var(--white);
  min-height: calc(11in - 1.5in);   /* physical page — don't scale */
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
```

The flexbox column layout lets you put `flex:1` on one child element (usually a grid or
stats section) to push it down and fill remaining space, so the card doesn't end short.

The `@media print` rule relaxes `min-height` to `auto` so printed pages don't force blank
space. The `@media (max-width:700px)` rule collapses grids to single-column and reduces
padding.

### ⚠️ Printing Backgrounds — `print-color-adjust: exact` is not optional

**There is no PDF-generation engine anywhere in this system.** A customer's "PDF" of a proposal
is produced by their own browser's native Print / Save-as-PDF, printing the exact live page —
same HTML, same CSS, no separate renderer to account for. Every mainstream browser **omits
background colors and images by default when printing**, to save ink, unless the CSS on that
element explicitly opts back in.

Without the opt-in, `.card-inset`'s `background: var(--navy)` silently vanishes on print. The
card's own text colors (`--white`, `--off`, `--muted`) don't change — they just end up sitting
directly on the page's plain white background instead of the navy panel that gave them contrast,
which is exactly what makes `--off`/`--muted` body text (chosen to read cleanly against navy)
look like faint, illegible light grey on white once the panel is gone. **Every element that sets
its own `background` needs both declarations** — `-webkit-print-color-adjust: exact;` and
`print-color-adjust: exact;` — not just `.card-inset`. That includes `.info-box`, `.card`,
`.callout`, and any one-off `background:` you add. The skeleton and every pattern below now
includes them; keep them on anything you add.

## Default Color Palette

Use CSS custom properties defined on the prefix class:

```css
.PREFIX {
  --navy: #1B2A4A;      /* card background */
  --navy-lt: #243B5C;   /* inner cards, info boxes */
  --teal: #2A7F8E;      /* accents, borders, icons */
  --teal-bg: #1E5F6B;   /* callout backgrounds */
  --white: #fff;         /* headings, emphasis */
  --off: #D0D8E4;        /* body text */
  --muted: #A0ADBF;      /* secondary/descriptions */
  font-family: 'Source Sans 3', 'Liberation Sans', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}
```

This dark navy palette is the default. When adapting source material that has its own
color scheme, you can adjust — but always ensure the card background contrasts well with
the `#f8f9fa` proposal background.

## Reusable Layout Patterns

Pick from these building blocks depending on the content:

### Two-Column Info Boxes
Side-by-side boxes on darker navy backgrounds, good for comparing concepts or pairing
related details:
```css
.PREFIX .two-col { display:grid; grid-template-columns:1fr 1fr; gap:1rem; }
.PREFIX .info-box { background:var(--navy-lt); -webkit-print-color-adjust:exact; print-color-adjust:exact;
  border-radius:8px; padding:1.15rem 1.25rem; }
```

### Stat Rings
Circular indicators for key numbers — uses CSS borders for the ring effect:
```css
.PREFIX .stat .ring {
  width:100px; height:100px; border-radius:50%;
  border:5px solid var(--navy-lt); border-top-color:var(--teal);
  display:flex; align-items:center; justify-content:center;
  margin:0 auto 0.5rem;
}
```

### Card Grid (2x2 or 3-col)
Inner cards with teal top borders, each containing an icon, title, body, and bullet list:
```css
.PREFIX .grid { display:grid; grid-template-columns:1fr 1fr; gap:0.85rem; flex:1; }
.PREFIX .card { background:var(--navy-lt); -webkit-print-color-adjust:exact; print-color-adjust:exact;
  border-radius:8px; padding:1rem 1.15rem;
  border-top:3px solid var(--teal); display:flex; flex-direction:column; }
```

### Callout Box
Teal-background box for important notes or summaries:
```css
.PREFIX .callout { background:var(--teal-bg); -webkit-print-color-adjust:exact; print-color-adjust:exact;
  border-radius:6px; padding:0.75rem 1rem; }
```

### Icon Circles
Teal-tinted circles for card icons (use Unicode emoji or HTML entities):
```css
.PREFIX .card-icon span {
  display:inline-flex; align-items:center; justify-content:center;
  width:36px; height:36px; border-radius:50%;
  background:rgba(42,127,142,0.2); -webkit-print-color-adjust:exact; print-color-adjust:exact;
  font-size:16px; color:var(--teal);
}
```

## Filling the Page — Size Budget

The card-inset target is **912px (9.5in)** at 96dpi — that's an 11in page minus 1.5in for
outer padding and card padding. Content must fill this space without overflowing.

The key technique: put `flex:1` on one expandable section (a grid, stats row, or spacer)
so it stretches to consume remaining space. Then use `align-content:center` on grid layouts
so the content within that flex section centers vertically in the available area.

**Page budget math:**
- Total card height: ~912px
- Card padding top+bottom: ~76px → ~836px usable
- Title + subtitle: ~60-80px
- Body paragraph: ~40-60px (depends on length and font size)
- Remaining: ~700px for info boxes, grids, stats, callouts

If content is text-heavy (like a 2x2 card grid with bullet lists), use smaller font sizes
(9-10pt body, 8.5pt bullets). If content is sparse (title + paragraph + stat rings), use
larger fonts (11-12pt body, 19-20pt subtitle) to fill space.

## Font Sizing Guide

Calibrated ranges for the 900px container. Pick sizes based on content density — use the
higher end for sparse pages and lower end for dense pages:

- Page title: `22-26pt`, weight 700
- Subtitle: `19-20pt`, weight 700, off-white
- Section heading: `12-13pt`, weight 700
- Body text: `10-12pt`, line-height `1.5-1.7`
- Info box text: `10-10.5pt`, line-height `1.55`
- Card body: `9-10pt`, line-height `1.4-1.5`
- Bullet items: `8.5-9.5pt`, line-height `1.35-1.45`
- Callout text: `9.5-10.5pt`
- Badges / tags: `7-8pt`

## Validating Page Fit

After generating HTML blocks, use the preview server (`docs-preview` in `.claude/launch.json`)
and `docs/preview-pages.html` to measure actual rendered height.

To get the natural content height (without the min-height floor), temporarily set
`card.style.minHeight = '0'` via JS and read `scrollHeight`. The content should be
**at or slightly under 912px** — the `flex:1` section absorbs the difference.

- If natural height > 912px: content overflows, reduce font sizes or spacing
- If natural height < 750px: too much flex stretch, increase font sizes or spacing
- Sweet spot: 750-900px natural height, with 12-162px of comfortable flex stretch

## Merge Tokens

The proposal system resolves these tokens server-side, in `ViewProposal.buildTokenMap` and the
`putIchra*` methods it calls (`src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java`),
before rendering. **This table is generated from that method directly — every token below is
real, and these 30 are the complete set.** Do not add a token to your HTML unless it appears here.

### ⚠️ An unmatched token is not stripped and not blanked — it renders literally

`replaceTokens` only ever looks at the token map's own keys; a `{{TOKEN}}` in your HTML whose name
isn't one of the 30 below is never touched by anything. It passes straight through to the
customer's page as the literal text `{{TOKEN}}` — braces and all. This has already happened on a
live, generated proposal PDF.

**Five names have been used in this project's own docs or drafts as if they were real tokens.
None of them exist. Do not use any of these:**

| Written as | Why it looks real | What to use instead |
|---|---|---|
| `{{AGENT_PHONE}}` | Seems like the obvious sibling of `{{AGENT_EMAIL}}` | No phone token exists at all |
| `{{SECONDARY_COLOR}}` | Sounds like the counterpart to `{{PRIMARY_COLOR}}` | `{{ACCENT_COLOR}}` |
| `{{CURRENT_DATE}}` | "Today's date" is a common thing to want | `{{DATE_CREATED}}` (the proposal's creation date — there is no "today" token) |
| `{{PROPOSAL_DATE}}` | Reads naturally as "the proposal's date" | `{{DATE_CREATED}}` |
| `{{ICHRA_STATE}}` | ⚠️ **The trap here is different — `ProposalIchraIntake.state` genuinely exists as a database column and a Java field.** It was deliberately never exposed as a merge token (a documented decision, not an oversight), so the entity field existing is exactly why people keep assuming the token does. | Not available. If a draft needs the state abbreviation next to the county name, say so before writing the prose — there is no token for it today. |

### Proposal and agent

| Token | Resolves to | When blank |
|---|---|---|
| `{{PROSPECT_NAME}}` | The prospect/employer's name | Empty string |
| `{{AGENT_NAME}}` | Selling agent's first + last name | Empty string, if no agent resolves for this proposal |
| `{{AGENT_EMAIL}}` | Selling agent's email | Empty string |
| `{{AGENCY_NAME}}` | Resolved agency name | Falls back to the PSP's name if no agency resolves — essentially never blank |
| `{{PSP_NAME}}` | The PSP's full name | Empty string |
| `{{DATE_CREATED}}` | Proposal creation date, formatted like `August 3, 2026` | Empty string, if the proposal has no creation date on record |
| `{{PROPOSAL_ID}}` | The proposal's numeric id | Empty string (not observed in practice) |
| `{{APPLY_BUTTON}}` | A complete, styled `<a>` "Apply Now" button — insert where you want the call to action | Never blank — always renders the button |

`{{AGENT_NAME}}`/`{{AGENT_EMAIL}}` blank together only when no agent resolves for the proposal at
all; otherwise each is independent.

### Brand colors

| Token | Resolves to | When blank |
|---|---|---|
| `{{PRIMARY_COLOR}}` | PSP's primary brand color (hex) | Never blank — falls back to a hardcoded default if the PSP hasn't set one |
| `{{ACCENT_COLOR}}` | PSP's accent/secondary brand color (hex) | Never blank, same fallback behavior |

### ICHRA intake — county, headcount, plan year

Populated from the ZIP/county/headcount an agent enters in the Proposal Builder for a plus-tier
line of service (T125). **Not entitlement-gated and not provenance-gated** — these are the
agent's own input, resolved the same way regardless of who is viewing the page.

| Token | Resolves to | When blank |
|---|---|---|
| `{{ICHRA_COUNTY}}` | County name, e.g. `Hopkins County` | Empty string |
| `{{ICHRA_COUNTY_FIPS}}` | 5-digit county FIPS code | Empty string |
| `{{ICHRA_HEADCOUNT}}` | Eligible employee count the agent entered | Empty string |
| `{{ICHRA_PLAN_YEAR}}` | Plan year the county/rates were interpreted against | Empty string |

All four blank together whenever the proposal has no intake row at all (every line of service that
isn't plus-tier, and any proposal created before this existed) — there is no case where some of
the four are populated and others aren't.

### ICHRA contribution — the employer's own stated contribution (V088)

The employer's monthly-per-employee ICHRA contribution, as entered by the agent — **an intake
token like the four above, not a market-data one.** Not entitlement-gated, not provenance-gated.

| Token | Resolves to | When blank |
|---|---|---|
| `{{ICHRA_CONTRIBUTION_MONTHLY}}` | Contribution per employee per month, formatted currency | Empty string |
| `{{ICHRA_CONTRIBUTION_ANNUAL}}` | Contribution per employee per year | Empty string |
| `{{ICHRA_CONTRIBUTION_TOTAL_MONTHLY}}` | Contribution × headcount, per month | Empty string |
| `{{ICHRA_CONTRIBUTION_TOTAL_ANNUAL}}` | Contribution × headcount, per year | Empty string |

⚠️ **Strict all-or-nothing group of these four.** The contribution field is optional at intake —
if the agent left it blank, or headcount is missing or zero, **all four** resolve to empty string
together, never a mix of some populated and some blank, and never `$0.00`.

| Token | Resolves to | When blank |
|---|---|---|
| `{{ICHRA_CONTRIBUTION_SCENARIO_TABLE}}` | A complete `<table>` element (T171) — insert on its own, never wrapped in another `<table>` — showing per-age-band premium/contribution/net rows when age-specific data was captured at build time, or a single group-level low/high range row when it was not | Empty string |

Unlike the four scalar tokens above, this one is not a live intake read — it comes from the
proposal's frozen snapshot (same source as "ICHRA payload" below), captured once at build time
and never recomputed at render.

### ICHRA market — plan/carrier counts and premium floors

Resolved from the cached market rate data for the intake row's county and plan year. **Behind a
provenance gate** — see below — because this is real market data, not the agent's own input.

| Token | Resolves to | When blank |
|---|---|---|
| `{{ICHRA_PLAN_COUNT}}` | Number of plans available in the county (age-40 row) | Empty string |
| `{{ICHRA_CARRIER_COUNT}}` | Number of carriers | Empty string |
| `{{ICHRA_FLOOR_AGE_21}}` | Lowest available monthly premium at age 21, formatted currency | Empty string |
| `{{ICHRA_FLOOR_AGE_40}}` | Same, at age 40 | Empty string |
| `{{ICHRA_FLOOR_AGE_64}}` | Same, at age 64 | Empty string |
| `{{ICHRA_RATES_AS_OF}}` | Date the cached rates were fetched, formatted like `{{DATE_CREATED}}` | Empty string |
| `{{ICHRA_RATES_SCOPE}}` | A full disclosure sentence — off-exchange only, not a quote, not complete | Empty string |

⚠️ **All seven blank together** unless every one of these holds: the selling agency is
ICHRA-entitled, an intake row exists with a county and plan year, cached rate rows exist for that
county/year, and **every** one of those rows is production-sourced (not staging). As of this
writing, production HealthSherpa access does not exist, so **these seven tokens are empty on every
live proposal today** — do not build page content that assumes they will resolve. Even once the
gate passes, an individual age's floor can still be empty on its own if that specific age has no
cached row — the group-level gate and a single figure's own presence are two different things.

**⚠️ Not documented here on purpose: an `{{ICHRA_MARKET_BLOCK}}` token does not exist.** If you've
seen it referenced anywhere, that reference is stale — `buildTokenMap` was checked directly for
this skill update (2026-08-03) and no such key is ever put into the token map.

### ICHRA payload — frozen snapshot data (T165/V090, T172)

Resolved from `ProposalIchraSnapshot.payloadJson` — a frozen, point-in-time snapshot captured at
proposal **build** time, never re-fetched or recomputed at render. **This is a different source
and a different gate than "ICHRA market" above**, which queries the live rate cache on every
render — a proposal can show real age-band premiums here while every live market token above is
empty, or the reverse. That is not a bug.

| Token | Resolves to | When blank |
|---|---|---|
| `{{ICHRA_AGE_BAND_TABLE}}` | A complete `<table>` element — insert on its own, never wrapped in another `<table>` — of age/lives/premium rows, frozen at build time | Empty string, if the proposal has no age-band data captured |
| `{{ICHRA_PLAN_LANDSCAPE_TABLE}}` | A complete `<table>` element — insert on its own, never wrapped in another `<table>` — of metal level/premium/HSA-eligible/ICHRA-only rows | **Always empty on every proposal today.** The HealthSherpa plan-fetch that would populate this (build item T166) has never been built — do not build page content that assumes this will resolve |
| `{{ICHRA_PAYLOAD_AS_OF}}` | A complete disclosure sentence — **not** a table — stating when the snapshot was captured | Empty string, if no snapshot exists |
| `{{ICHRA_GROUP_COMPARISON_TABLE}}` | A complete `<table>` element (T172) — insert on its own, never wrapped in another `<table>` — comparing the group's current plan cost against the planned ICHRA contribution | Empty string, if the employer's current-coverage figures or the contribution were never entered |

## Workflow

When the user provides source material (URL, text, description):

1. **Read the source** — fetch the URL or read the provided content
2. **Identify structure** — break the content into logical pages (one card-inset per page)
3. **Generate HTML blocks** — one file per page using the skeleton above
4. **Apply the card-inset pattern** — dark card, rounded corners, fills the page
5. **Adapt styling** — match the source's visual feel where possible, but always using the
   card-inset foundation and scoped CSS
6. **Save files** — write each block to `docs/` with a descriptive filename like
   `proposal-{topic}-page{N}.html`

## Reference Examples

Two working examples live in the project:

- `docs/proposal-fsa-page3.html` — overview page with title, subtitle, body paragraph,
  two-column info boxes, and three stat rings
- `docs/proposal-fsa-page4.html` — detail page with title, body, 2x2 card grid with
  icons and bullet lists, and a callout box

Read these files when you need to see the exact patterns in action.

## Things to Avoid

- No `<script>` tags, `onclick`/`onload`/etc., or `javascript:` links (stripped by sanitizer)
- No `<link>` stylesheet tags (use `@import` inside `<style>`)
- No `<html>`/`<head>`/`<body>` wrappers
- No company branding, logos, or taglines unless the user specifically requests them
- No unstyled content — everything should be inside the card-inset
- Don't use Bootstrap classes — they might collide with the host page. Write all CSS from scratch under the scoped prefix.
