# Session 11-I close-out — correct the proposal-content-page skill

**Run:** S11-I (Sonnet). Documentation only. No Java, no JSP, no SQL, no behavior change anywhere.

**Branch:** `refactor/modernize-architecture` (trunk).

**One-line summary:** `.claude/skills/proposal-content-page/SKILL.md`'s token table is now regenerated
directly from `ViewProposal.buildTokenMap` — 25 real tokens, grouped, with blank behavior and
all-or-nothing group membership stated for each — plus a phantom-token warning naming all five known
phantoms (a fifth, `{{ICHRA_STATE}}`, found this run), the `print-color-adjust: exact` requirement
S11-C established, and a defensive heading-color baseline rule.

---

## Baseline

- **Hash at preflight (post-pull):** `a6a35c23140f813076dfb17409fc32189a9d4e67`. **Matches the
  prompt's expected start (`a6a35c2`) exactly.** No divergence to report. (Incidental: the pull
  fetched a new tag, `v0.88.01` — noted, not investigated; out of scope for a documentation-only
  run.)
- **Hash at end (feature commit, pushed):** `a48529c7209ae7e9ecbfda36b7bc3c4778dffa44`
- **Hash at end (this close-out commit):** `72b13fb04959b81bd9d986deb973a893b5aa55a9`, read from
  `git log` after push, never carried from the prompt and never written before the push.

Preflight passed all four gates: branch `refactor/modernize-architecture`, `git status --short`
empty, `git pull --ff-only` reported a fast-forward (new tag only, no commits).

## Step 2 — discovery findings, in full

### 1. Token list re-derived fresh from `buildTokenMap` (not copied from any prior close-out)

Read `ViewProposal.java:641-854` directly (`buildTokenMap` and `putIchraMarketTokens`, which it
calls at `:734`). **25 tokens, unchanged from every prior count this session — no difference to
report.** Full list, in the order each is `put()`:

`PROSPECT_NAME`, `AGENT_NAME`, `AGENT_EMAIL`, `AGENCY_NAME`, `PSP_NAME`, `DATE_CREATED`,
`PRIMARY_COLOR`, `ACCENT_COLOR`, `PROPOSAL_ID`, `APPLY_BUTTON`, `ICHRA_COUNTY`,
`ICHRA_COUNTY_FIPS`, `ICHRA_HEADCOUNT`, `ICHRA_PLAN_YEAR`, `ICHRA_CONTRIBUTION_MONTHLY`,
`ICHRA_CONTRIBUTION_ANNUAL`, `ICHRA_CONTRIBUTION_TOTAL_MONTHLY`, `ICHRA_CONTRIBUTION_TOTAL_ANNUAL`,
`ICHRA_PLAN_COUNT`, `ICHRA_CARRIER_COUNT`, `ICHRA_FLOOR_AGE_21`, `ICHRA_FLOOR_AGE_40`,
`ICHRA_FLOOR_AGE_64`, `ICHRA_RATES_AS_OF`, `ICHRA_RATES_SCOPE`.

⚠️ **Worth noting explicitly, since it could be mistaken for a 26th token:** S11-H's Market page
(shipped the prior session) does **not** add to this list. It renders through a completely
separate mechanism — direct JSP request attributes (`marketPlanCount`, `marketFloor21`, etc.) read
by `proposalMarket.jsp` — not through `buildTokenMap`/`replaceTokens` at all. `{{MARKET_*}}` is not
a thing and was never at risk of being documented as one; flagging only so a future reader doesn't
wonder why S11-H isn't reflected in this table.

### 2. Every file carrying each of the five known phantoms

| Phantom | Files (excluding worktrees/target/build artifacts) |
|---|---|
| `AGENT_PHONE` | `SKILL.md:238` (now fixed); `project_backlog.md:192`'s T132 description (correctly states it doesn't exist — historical record, not a false claim) |
| `SECONDARY_COLOR` | `SKILL.md:240` (now fixed); `project_backlog.md:192` (correct record) |
| `CURRENT_DATE` | `SKILL.md:241` (now fixed); `project_backlog.md:192` (correct record) |
| `PROPOSAL_DATE` | `SKILL.md:242` (now fixed); `project_backlog.md:192` (correct record) |
| `ICHRA_STATE` | **`docs/analysis/phase_a_tier1_proposal_section.md:114`** (presented as real — **fixed this run**); `docs/analysis/project_backlog.md:188` (T128's row, correctly recorded as a deviation — historical record); `docs/runs/S10-E_closeout.md` (4 occurrences, all correctly say it does not exist); `docs/runs/S10-G_closeout.md:333` (correctly says it still doesn't exist) |

**Distinction that matters:** most `ICHRA_STATE` hits are *correct* historical records already
saying the token doesn't exist — those were left alone. Only
`phase_a_tier1_proposal_section.md:114` presented it as if it resolved (a pre-build spec table with
`With an intake row → TX`), which is the actual phantom instance, and is the one file named in the
fence's "other docs" clause and corrected.

### 3. Broader `{{...}}` sweep — anything unnamed?

Grepped the whole repo for `\{\{[A-Za-z_]+\}\}` with no phantom-specific filter. Result: every hit
outside the five known phantoms matches one of the 25 real tokens, or is generic placeholder prose
(`{{TOKEN}}`, `{{TOKEN_NAME}}` used to explain the mechanism itself, not as a literal reference).
**No new, previously-unnamed phantom was found.**

Explicitly checked `docs/proposal_html/*.html` (the authored artifacts the fence forbids editing)
and every `.html` file in the repo more broadly for any `{{...}}` pattern at all: **zero matches.**
Session 10's fix to the live page-2 copy already removed the two phantoms that had reached it —
nothing is currently sitting broken in authored HTML.

### 4. `SKILL.md`'s token table and card-inset section — printed, read in full before editing

Printed both in full during discovery (the 8-row phantom-laden table at `:233-242`, and the
card-inset CSS block at `:91-101`) before any edit was made. Confirmed the file's own
"Server-Side Sanitization" section already correctly states `<style>` is preserved — that part
needed no correction.

## Shipped

- `a48529c7209ae7e9ecbfda36b7bc3c4778dffa44` — `docs: correct the proposal-content-page skill's
  token table (T132)`. 3 files, +154/−19: `SKILL.md`, `project_backlog.md`,
  `phase_a_tier1_proposal_section.md`. Pushed to `origin/refactor/modernize-architecture`.
- `72b13fb04959b81bd9d986deb973a893b5aa55a9` — `docs: S11-I close-out` (this document, as a second
  commit). Pushed.

## Decisions made

- **Grouped the table by the five categories the prompt specified** (proposal/agent, brand colors,
  ICHRA intake, ICHRA contribution, ICHRA market) rather than one flat 25-row table, so an author
  looking for "how do I show the county" doesn't have to scan past market-data tokens they don't
  need yet.
- **Named `ICHRA_STATE` as a sixth entry in the phantom warning**, distinct from the original four,
  because it's a different failure mode: the other four are pure invention, but
  `ProposalIchraIntake.state` is a genuinely real database column and Java field — the entity field
  existing is *why* the phantom token assumption keeps recurring. Gave it its own explanatory row
  rather than folding it into the same four-row table, per the prompt's explicit instruction that
  it "deserves its own line."
- **Did not document `{{ICHRA_MARKET_BLOCK}}`.** Checked `buildTokenMap` directly (finding 1) and
  confirmed no such key exists. Added one explicit sentence stating it does not exist, so a reader
  who has seen it referenced elsewhere isn't left wondering whether the omission was an oversight.
- **Corrected `phase_a_tier1_proposal_section.md` by striking the phantom row rather than deleting
  it**, with a dated note explaining what changed and why (S10-E's build prompt deliberately
  narrowed the token set). Matches the project's own established convention for this exact
  situation — `docs/analysis/migration_tracker.md` strikes rather than deletes superseded claims for
  the same reason: the row itself is evidence of why the confusion kept recurring.
- **Did not touch any of the correctly-worded `ICHRA_STATE` mentions** in `project_backlog.md` or
  the `docs/runs/S10-*_closeout.md` files. Those already say the token doesn't exist; editing them
  would be revising historical run records for no benefit, and none of them presents the phantom as
  real.
- **Flagged, did not fix, the stale "Reference Examples" paths** at `SKILL.md:383-392`
  (`docs/proposal-fsa-page3.html`/`page4.html` don't exist; the real files are
  `docs/proposal_html/proposal_fsa_one.html`/`proposal_fsa_two.html`). Found during discovery, real,
  and cheap to fix — but outside the explicit 3a–3e edit list this run was scoped to, so it's
  recorded in `project_backlog.md`'s T132 row and here rather than silently expanding scope.
- **Added `print-color-adjust: exact` to every background-setting pattern in the skill**, not only
  `.card-inset` — `.info-box`, `.card`, `.callout`, and the icon-circle's `rgba()` background — since
  the prompt's rationale (browsers drop backgrounds on print) applies to all of them equally, and
  documenting it on only one pattern would leave the same defect class open on the others.
- **Verified the Bootstrap heading-color claim (3d) against the actual vendored CSS** before writing
  the baseline rule, rather than taking the prompt's premise on faith — `bootstrap.css:218-224`
  confirms `h1, h2, h3, h4, h5, h6, .h1...` all carry `color: var(--bs-heading-color)` as a
  directly-matching rule. `--bs-heading-color` defaults to `inherit`, so the defect is latent, not
  currently manifesting — but the mechanism is real (any future redefinition of that custom property
  anywhere in the cascade would silently override every heading inside every card-inset), which is
  exactly why a defensive baseline earns its keep now rather than after it breaks.

## New assumptions

None introduced. This run's job was to remove incorrect documentation, not add new claims requiring
future reversal.

## Open questions raised

- **Should `SKILL.md`'s stale "Reference Examples" paths be fixed?** (Decisions, above.) A one-line
  fix, deliberately left for a future run or for Kevin to decide is worth doing.
- **Should the token table be auto-generated from `buildTokenMap` by tooling, rather than hand-
  maintained and periodically re-derived by a run like this one?** T132's original description
  already suggested this as the durable fix. Not attempted here — would require a build step or
  script, which is code, and this run was documentation-only by its own scope fence.

## Contradictions found

None against the run prompt. One internal correction within the docs set itself:
`phase_a_tier1_proposal_section.md`'s `{{ICHRA_STATE}}` row contradicted the S10-E build decision
that superseded it — addressed above, not a contradiction in this run's own instructions.

## Additional finding — phantom tokens in authored HTML

**None found, and none currently exist.** Explicitly checked per the close-out's required callout:
`docs/proposal_html/*.html` and every other `.html` file in the repository were grepped for any
`{{...}}` pattern. Zero matches anywhere. The two phantoms that reached a live, generated proposal
PDF (session 10, `{{AGENT_PHONE}}` and `{{PROPOSAL_DATE}}`) were already corrected directly in the
pasted HTML at that time, per `project_backlog.md`'s T132 row and
`docs/session_closeout_2026-08-03_session10.md:94`. There is no live defect of this kind waiting in
authored HTML as of this run — worth stating plainly since the run's own "Why" section could be read
as implying one still exists.

## Verification

- **Every token in the table cross-checked against `buildTokenMap`, one at a time** — see Step 2
  finding 1 for the full 25-token list, re-derived fresh this run rather than copied from S11-B's or
  any other prior output. Confirmed one-to-one in both directions: every token the method `put()`s
  appears in the table, and every token in the table exists in the method.
- **No code file was touched.** `git status --short` at both the pre-edit and pre-commit points
  showed only `.claude/skills/proposal-content-page/SKILL.md`,
  `docs/analysis/project_backlog.md`, and `docs/analysis/phase_a_tier1_proposal_section.md` — all
  three `.md` files, none Java/JSP/SQL.

## SQL close-out audit

**This run produces no SQL.** No migration was written, run, or recommended; no `.sql` file was
created, modified, or orphaned — expected for a documentation-only run, and confirmed by the
`git status` result above. **Current highest migration version:** **V088** —
`ls docs/migrations/*.sql | sort | tail -2` returns `V088__proposal_ichra_intake_contribution.sql`
(plus the long-standing non-versioned `seed_ndt125_questionnaire.sql`), unchanged from every prior
session this run.

## Next

- Consider fixing `SKILL.md`'s stale reference-example paths (flagged, not fixed, above) — a
  one-line correction whenever someone is next in the file for another reason.
- Consider whether the token table should be tooling-generated rather than periodically
  re-derived by hand, if this class of drift recurs a third time.

## Compliance statement

Scope fence was writable: `.claude/skills/proposal-content-page/SKILL.md`,
`docs/analysis/project_backlog.md`, `docs/session_s11i_closeout.md`, plus
`docs/analysis/phase_a_tier1_proposal_section.md` — named individually here, per the fence's
instruction, as the one file Step 2 found carrying a live phantom-token instance outside the skill
itself, and edited only to correct that token reference (struck the row, added a dated note — no
other content in that file was touched).

Forbidden list, confirmed: **no Java, JSP, or SQL file was modified anywhere in this run** — every
commit this run produced touches `.md` files only. No file under `docs/proposal_html/` or any other
`.html` file was edited; the sweep in "Additional finding" above was read-only. No forbidden git
operation was run: no `git add -A`, no `git add .`, no `stash`, `checkout`, `restore`, `reset`, and
no local tag. Staging was by explicit named path, each file listed individually.
