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

## The Inset Card

The `.card-inset` is the visual foundation — a rounded rectangle that fills the page and
floats on the light background:

```css
.PREFIX .card-inset {
  background: var(--navy);           /* rich color contrasting #f8f9fa */
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
.PREFIX .info-box { background:var(--navy-lt); border-radius:8px; padding:1.15rem 1.25rem; }
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
.PREFIX .card { background:var(--navy-lt); border-radius:8px; padding:1rem 1.15rem;
  border-top:3px solid var(--teal); display:flex; flex-direction:column; }
```

### Callout Box
Teal-background box for important notes or summaries:
```css
.PREFIX .callout { background:var(--teal-bg); border-radius:6px; padding:0.75rem 1rem; }
```

### Icon Circles
Teal-tinted circles for card icons (use Unicode emoji or HTML entities):
```css
.PREFIX .card-icon span {
  display:inline-flex; align-items:center; justify-content:center;
  width:36px; height:36px; border-radius:50%;
  background:rgba(42,127,142,0.2); font-size:16px; color:var(--teal);
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

The proposal system resolves these tokens server-side before rendering. Use them in the
HTML content wherever personalization makes sense:

| Token | Resolves To |
|-------|-------------|
| `{{PROSPECT_NAME}}` | Company/prospect name |
| `{{AGENT_NAME}}` | Sales agent's full name |
| `{{AGENT_EMAIL}}` | Sales agent's email |
| `{{AGENT_PHONE}}` | Sales agent's phone |
| `{{PRIMARY_COLOR}}` | PSP primary brand color |
| `{{SECONDARY_COLOR}}` | PSP secondary brand color |
| `{{CURRENT_DATE}}` | Today's date |
| `{{PROPOSAL_DATE}}` | Proposal creation date |

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
