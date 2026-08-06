# ICHRA proposal section page fixtures

S21-J. Four standalone HTML content blocks, versioned here so the ICHRA proposal section
content is reviewable and reproducible instead of existing only as database rows on one
machine.

**These are authoring fixtures, not application code.** Nothing in AMS reads these files at
runtime. Kevin pastes each one into the `htmlContent` of the corresponding `CUSTOM`
`ProposalSection` row on his local dev box, through the Proposal Settings UI.

Each file is a self-contained HTML fragment — no `<html>`/`<head>`/`<body>` wrapper — matching
the exact structural pattern `ProposalAiBuilder.java`'s system prompt and
`.claude/skills/proposal-content-page/SKILL.md` hold the in-app AI page builder to: a `<style>`
block scoped under a unique class prefix, a `--s` scale factor, and the card-inset pattern
(`page-break-before:always; padding:0.5in 0` on the outer div, a rounded dark card filling the
page). All four use the same default navy palette and table treatment so they read as one
consistent document in sequence.

## Files, and the `ProposalSection` each belongs to

| File | Prefix | Maps to enhancement / section |
|---|---|---|
| `01-ichra-market-overview.html` | `ichram` | The `ICHRA_MARKET` `system_managed` enhancement's `CUSTOM` section (T168) |
| `02-ichra-age-bands-plan-landscape.html` | `ichrab` | Also `ICHRA_MARKET` — the two payload table tokens (`ICHRA_AGE_BAND_TABLE`, `ICHRA_PLAN_LANDSCAPE_TABLE`) had no page of their own before this run; see "Grouping" below |
| `03-ichra-contribution-scenarios.html` | `ichrac` | The `ICHRA_CONTRIBUTION` `system_managed` enhancement's `CUSTOM` section (T171) |
| `04-ichra-group-plan-comparison.html` | `ichracmp` | The `ICHRA_COMPARISON` `system_managed` enhancement's `CUSTOM` section (T172) |

Per the standing configuration rule for every `system_managed` section (S20A spec §1.4a, §8.6):
each is a `scope='SCOPED'` `CUSTOM` section with **one `proposalsectionenhancement` row and
zero `proposalsectionlos` rows**. A LOS association makes `ViewProposal`'s `SCOPED` filter match
unconditionally and the `FlaggedEnhancementResolver` gate never runs.

## Grouping — where it followed Kevin's stated four-page list, and where it filled gaps

Kevin's assumption named four pages: Market Overview; Age Bands & Plan Landscape; Contribution
Scenarios; Group Plan Comparison. Every one of the 20 ICHRA tokens `ViewProposal.buildTokenMap`
and its `putIchra*` methods resolve was placed on exactly one of these four pages — enumerated
fresh from the code for this run, not taken from any document:

- **Page 1** carries the `ICHRA_MARKET` enhancement's scalar tokens: county, county FIPS,
  headcount, plan year (intake); plan count, carrier count, and the three age-21/40/64 premium
  floors, plus the rates-as-of date and the full off-exchange disclosure sentence (market data).
  The three premium-floor tokens weren't named in Kevin's one-line description of this page, but
  they're aggregate county-level market facts exactly like plan/carrier counts, so this is where
  they fit — not a contradiction of the stated grouping, a filled-in detail.
- **Page 2** carries the two table tokens Kevin named (`ICHRA_AGE_BAND_TABLE`,
  `ICHRA_PLAN_LANDSCAPE_TABLE`) plus `ICHRA_PAYLOAD_AS_OF` (the frozen-snapshot disclosure
  sentence) as its natural closing disclosure, since both tables come from that same frozen
  payload.
- **Page 3** carries `ICHRA_CONTRIBUTION_SCENARIO_TABLE` (T171) as named, plus the four older,
  ungated T80 tokens (`ICHRA_CONTRIBUTION_MONTHLY`/`_ANNUAL`/`_TOTAL_MONTHLY`/`_TOTAL_ANNUAL`) as
  a plain-figure summary — they're about contribution and had no other natural home.
- **Page 4** carries only `ICHRA_GROUP_COMPARISON_TABLE` (T172), exactly as named — the single
  token this build shipped.

**Affordability tokens have no home, deliberately.** No token resolves an affordability figure on
any of these four pages, and none will until an `LA-NN` legal_assumptions.md entry is filed and
build 4 is unblocked.

**20 tokens in, 20 tokens placed, none omitted, none duplicated across pages.**

## Compliance

No recommendation, ranking, savings framing, verdict language (visual or textual), affordability
claim, carrier/plan name, or enrollment call-to-action appears in any of the four pages. Every
table token is styled generically (`.PREFIX table`/`th`/`td`) — no per-column styling anywhere,
so nothing singles out one figure or column as preferable. Every page that presents plan or
carrier-level facts (pages 1 and 2) states plainly that the underlying data is off-exchange and
incomplete.

## A known, separately-tracked defect these fixtures use anyway

`{{DATE_CREATED}}` is not used directly in these four pages (none needed a bare proposal date),
but if a future page adds it: it is the correct, real token — there is no `{{PROPOSAL_DATE}}`.
As of this session, `{{DATE_CREATED}}` renders empty on at least one real proposal under a
suspected EclipseLink L2-cache defect (T173's close-out, S21-H); that defect is tracked
separately and does not make the token name wrong.
