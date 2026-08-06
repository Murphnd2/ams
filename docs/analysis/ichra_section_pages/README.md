# ICHRA proposal section page fixtures

S21-J. Three standalone HTML content blocks, one per `enhancement.system_section_key`,
versioned here so the ICHRA proposal section content is reviewable and reproducible instead of
existing only as database rows on one machine.

**These are authoring fixtures, not application code.** Nothing in AMS reads these files at
runtime. Kevin pastes each one into the `htmlContent` of the corresponding `CUSTOM`
`ProposalSection` row on his local dev box, through the Proposal Settings UI.

⚠️ **This replaces an earlier four-page draft of this same fixture set**, committed and then
superseded within this same session. The earlier draft split plan-landscape and age-band content
onto a page of their own — wrong, because tokens are not gated individually. The *section* is
gated, by `system_section_key`, and exactly three keys are actually live (see below). A fourth
page would need a fourth enhancement, a fourth key, and matching completeness logic in
`buildIchraPayload` — application code, out of scope for this run.

Each file is a self-contained HTML fragment — no `<html>`/`<head>`/`<body>` wrapper — following
the structural pattern `ProposalAiBuilder.java`'s system prompt and
`.claude/skills/proposal-content-page/SKILL.md` hold the in-app AI page builder to: a `<style>`
block scoped under a unique class prefix, a `--s` scale factor, and the card-inset pattern
(`page-break-before:always; padding:0.5in 0` on the outer div, a rounded dark card filling the
page). All three use the same default navy palette and table treatment so they read as one
consistent document in sequence.

## Section keys, and why there are three

`ServiceManager`'s `system_section_key` dropdown (`serviceManager25.jsp:958-964`) offers four
literal values plus blank: `ICHRA_MARKET`, `ICHRA_CONTRIBUTION`, `ICHRA_COMPARISON`, and
`ICHRA_AFFORDABILITY`. The payload's own `sections` block (`ProposalBuilder.buildSectionsBlock`)
likewise always carries all four keys. But `ICHRA_AFFORDABILITY.selected` is hardcoded `false`
server-side — no control anywhere lets an agent select it — so `FlaggedEnhancementResolver`'s
`selected && complete` gate can never both hold for that key. It is configurable but permanently
inert, deliberately, pending an `LA-NN` legal_assumptions.md entry. **Three keys can actually gate
a rendered section: `ICHRA_MARKET`, `ICHRA_CONTRIBUTION`, `ICHRA_COMPARISON`** — one file per key,
named for it.

## Files, and the `ProposalSection` each belongs to

| File | Prefix | `system_section_key` | Build |
|---|---|---|---|
| `ICHRA_MARKET.html` | `ichram` | `ICHRA_MARKET` | T168 |
| `ICHRA_CONTRIBUTION.html` | `ichrac` | `ICHRA_CONTRIBUTION` | T171 |
| `ICHRA_COMPARISON.html` | `ichracmp` | `ICHRA_COMPARISON` | T172 |

Per the standing configuration rule for every `system_managed` section (S20A spec §1.4a, §8.6):
each is a `scope='SCOPED'` `CUSTOM` section with **one `proposalsectionenhancement` row and
zero `proposalsectionlos` rows**. A LOS association makes `ViewProposal`'s `SCOPED` filter match
unconditionally and the `FlaggedEnhancementResolver` gate never runs.

## Token-to-page mapping — all 20 ICHRA tokens, enumerated fresh from `ViewProposal.java`

| Page | Tokens (20 total, each placed exactly once) |
|---|---|
| `ICHRA_MARKET.html` | `ICHRA_COUNTY`, `_COUNTY_FIPS`, `_HEADCOUNT`, `_PLAN_YEAR` (intake); `_PLAN_COUNT`, `_CARRIER_COUNT`, `_FLOOR_AGE_21/40/64`, `_RATES_AS_OF`, `_RATES_SCOPE` (market); `_AGE_BAND_TABLE`, `_PLAN_LANDSCAPE_TABLE`, `_PAYLOAD_AS_OF` (14) |
| `ICHRA_CONTRIBUTION.html` | `_CONTRIBUTION_MONTHLY`, `_ANNUAL`, `_TOTAL_MONTHLY`, `_TOTAL_ANNUAL` (T80); `_CONTRIBUTION_SCENARIO_TABLE` (5) |
| `ICHRA_COMPARISON.html` | `_GROUP_COMPARISON_TABLE` (1) |

`_PLAN_LANDSCAPE_TABLE` and `_PAYLOAD_AS_OF` sit on the market page, not a page of their own —
they are market data, gated by the market section, same as everything else there.

**Affordability tokens have no home, deliberately.** No token resolves an affordability figure on
any of these three pages, and none will until an `LA-NN` entry is filed and build 4 is unblocked.

## `ICHRA_AGE_BAND_TABLE` — placed on the market page, not duplicated onto the contribution page

`ICHRA_AGE_BAND_TABLE` (`ViewProposal.java:899-916`) emits a three-column `<table>`: **Age,
Lives, Premium** — the frozen census/premium picture, gated by `ICHRA_MARKET` alone (it renders
whenever age bands exist in the payload, with or without a contribution).

T171's `ICHRA_CONTRIBUTION_SCENARIO_TABLE`, in its AGE_BAND branch, emits **Age, Lives, Premium,
Contribution, Net** — a strict superset of the same three columns, but only when
`ICHRA_CONTRIBUTION` is *also* selected and complete (`snapshot.getContribution() != null`).

These two tables are not identical, and not always both present: on a proposal where only
`ICHRA_MARKET` is selected, `ICHRA_AGE_BAND_TABLE` is the *only* census table shown, and it needs
a home. Placed on the contribution page instead, that market-only case would lose its census
table entirely. Placed on the market page, the (common) case where both sections are selected
does show three overlapping columns twice, once bare and once with the contribution and net
columns appended — an acceptable "context, then payoff" progression, not true duplication of the
same figure. Genuinely redundant would mean identical content with nothing gained by showing it
twice; this is a superset relationship the reader can follow across two pages. Flagged here so
Kevin can override the call if the printed sequence reads as repetitive in practice.

## Compliance

No recommendation, ranking, savings framing, verdict language (visual or textual), affordability
claim, carrier/plan name, or enrollment call-to-action appears in any of the three pages. Every
table token is styled generically (`.PREFIX table`/`th`/`td`) — no per-column styling anywhere,
so nothing singles out one figure or column as preferable. The market page states plainly, twice
(once for plan/carrier counts, once for the plan landscape table), that the underlying data is
off-exchange and incomplete.

## Degradation

The market page needs the most care — plan/carrier counts and premium floors depend on cached,
production-sourced rate data existing for that county, which frequently is not the case (as of
this writing, true for every live proposal). Its stat tiles degrade to a label with no number
rather than a sentence with a gap, and its age-band/plan-landscape sections are introduced with
copy that reads correctly whether or not the table beneath it renders anything.

## A known, separately-tracked defect these fixtures use anyway

`{{DATE_CREATED}}` is not used directly in these three pages (none needed a bare proposal date),
but if a future page adds it: it is the correct, real token — there is no `{{PROPOSAL_DATE}}`.
As of this session, `{{DATE_CREATED}}` renders empty on at least one real proposal under a
suspected EclipseLink L2-cache defect (T173's close-out, S21-H); that defect is tracked
separately and does not make the token name wrong.
