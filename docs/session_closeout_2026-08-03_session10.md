# Session 10 close-out — 2026-08-03

**Branch:** `refactor/modernize-architecture` · **Span:** `a517141` (session 9 close) → `9f4bf6e` · **Released:** `v0.87.01`
**Theme:** the sale-motion walk that stalled at stage 1 in session 9 now runs end to end, on production, for the tier-1 artifact — and stalls at a data-access wall for the tier-2/3 one.

---

## Shipped

Eleven commits, six runs (S10-A, B, D, E, F, G — **S10-C was never run**; see [Open questions](#open-questions-raised)).

**S10-A — Phase A spec, the Proposal Builder ICHRA interjection.**
`4560f6d` docs: Phase A spec for the Proposal Builder ICHRA interjection (S10-A)
`7e98597` docs: record the S10-A close-out commit hash

**S10-B — T125 build, the interjection itself + V087.**
`1d4c284` feat: ICHRA intake interjection in the Proposal Builder (T125, V087)
`3e233c0` docs: record the S10-B close-out commit hash

**S10-D — Phase A spec, the tier-1 proposal section.**
`b732ee5` docs: Phase A spec for the tier-1 proposal section (S10-D)
`ddad05d` docs: record the S10-D close-out commit hash

**S10-E — T128 build, `{{ICHRA_*}}` intake tokens for `CUSTOM` sections.**
`3b04198` feat: ICHRA intake tokens for CUSTOM proposal sections (T128)
`434f445` docs: record the S10-E close-out commit hash

**S10-F — T129 build, the entitlement gate closed before market data exists.**
`b3915d4` fix: gate plus-tier CUSTOM proposal sections behind ICHRA entitlement (T129)
`091ca0f` docs: record the S10-F close-out commit hash and stat

**S10-G — T130 build, market-data tokens reusing the illustration's cache-only read path.**
`0a1a783` feat: market-data merge tokens for plus-tier proposal sections
`9f4bf6e` docs: record the S10-G close-out commit hash and stat

**Pattern across all five build runs:** every diff touched exactly one file under `src/` (`ProposalBuilder.java` once, `ViewProposal.java` four times) plus its own backlog rows and close-out. No run widened its own scope fence.

---

## Released

`git tag --sort=-creatordate | head -5` (after `git pull`, per the standing rule that an un-fetched tag is invisible):

```
v0.87.01
v0.87.00
v0.86.00
v0.85.11
v0.85.10
```

- **`v0.87.00`** = `3e233c0` (S10-B) — V087 + T125 only.
- **`v0.87.01`** = `9f4bf6e` — **everything through S10-G**, confirmed by `git merge-base --is-ancestor` against every S10 commit. It also carries `820d027`, session 9's six admin-hardening guards, which had been sitting unreleased since 2026-08-02 as "`v0.86.01` pending." **That item is now released and can stop being carried as pending.**
- **Nothing from this session is unreleased.** The working tree was clean at every run's preflight and remains clean now.

---

## In flight

**Nothing.** `git status --short` at this run's preflight was empty and remains empty — every S10 change is committed, pushed, and tagged.

---

## Decisions made

1. **Discriminate the entitlement gate on the section's LOS association, not on `section_type`** (T129/S10-F). Adding `CUSTOM` to `ICHRA_GATED_SECTION_TYPES` was the trap — it would have stripped four live non-ICHRA `CUSTOM` sections from every line of service using them. Settled: gate on whether any associated LOS carries `is_plus_tier`.
2. **Entitlement gates the market-data *values*, independently of the section gate** (T130/S10-G). Even though T129 should already omit the section, `putIchraMarketTokens` re-checks `ichraEntitled` itself — deliberate defence in depth, justified directly by T131 having shown the section-level gate is configuration-sensitive.
3. **Cache reads only, structurally — never a HealthSherpa call from the public proposal path** (T130/S10-G). Established as a hard constraint before any code was written, and proved three independent static ways (reference search, import audit, HTTP-primitive grep) rather than merely assumed.
4. ⚠️ **The provenance gate is not to be relaxed to make the market-data demo work — settled and not open for relitigation.** Kevin's own production query found Hopkins County fully warmed but `source_env='STAGING'`; S10-G's `allMatch`-PRODUCTION gate correctly suppressed every market-data token, including the disclosure line. The fix is upstream (production HealthSherpa access, T136), not a loosened check. **The gate behaved exactly as designed** — this is recorded as a decision, not a defect.
5. **The market-data `CUSTOM` section has been unconfigured on the live proposal**, pending T136, because blank figure cards read to a viewer as broken software rather than as honestly incomplete data. The tier-1 administration section stays configured; only the market-data page was pulled.
6. **T131's confirm-before trigger fired and was resolved, but the underlying discriminator question was deliberately left open** rather than fixed reactively. Narrowing the gate (LOS association + a market-data marker) is real work with its own scope; setting `ichra_enabled` on the affected agency was the correct immediate response, not a substitute for deciding the narrower design.

---

## New assumptions

- **T131 (technical, not `LA-NN`).** S10-F's safety argument — no pre-existing LOS carries `is_plus_tier`, so no pre-existing `CUSTOM` section can be caught by T129's gate — held for less than a day. The flag was set on SWBD PremiumPath Program, which already carries six `CUSTOM` sections; all six are now gated for any agency without `ichra_enabled`. **Reversal cost: narrow the discriminator** (add a market-data marker alongside the LOS association) — cheap today, grows with every section added to a plus-tier LOS. **Confirm-before trigger has now fired once** (Kevin's own home agency) and been resolved by setting the flag; the trigger remains live for the next agency that hits it.
- **No new `LA-NN` was filed this session.** S10-D found tier-1 content already covered by LA-17 constraint 3's administration-only carve-out; S10-G found the market-data tokens already named explicitly by LA-17's own text ("premium ranges, plan and carrier counts"). Both are content review findings recorded in their run close-outs, not new legal positions.
- **Technical: the eleven `{{ICHRA_*}}` tokens are all-or-nothing per group, by design.** The four intake tokens (county/FIPS/headcount/plan-year) render for any agency, entitled or not, because they're the employer's own inputs. The seven market-data tokens require entitlement, a warm cache, and unanimous `PRODUCTION` provenance across every row for that county/year — one `STAGING` row blanks the whole group. Reversal cost of loosening either boundary: one line each, not recommended.

---

## Open questions raised

- **S10-C's status.** It was scoped to answer whether `isAvailableForProposal` returning `false` actually causes `ViewProposal` to omit gated content in production — exactly what happened, unplanned, when Kevin quoted from his own unentitled home agency. **Recommendation: close S10-C as answered-by-observation, or re-scope it narrowly to the one thing observation still can't confirm** — that the omission holds for a *prospect-facing* send (an agent-composed, agent-sent proposal to an actual employer), not just an agent's own preview. That distinction matters to LA-17 specifically.
- **T131's discriminator question.** Narrow the gate now, or continue accepting the current all-or-nothing behavior per plus-tier LOS? Nobody's decided; the trigger firing once raises the cost of leaving it undecided, but doesn't answer it.
- **T133's fix shape.** Strip unmatched `{{...}}` globally in `replaceTokens` (blast-radius unknown, touches every existing token) versus validate at authoring time in `ProposalSettings` (safer, but doesn't protect content pasted directly into the database or a future authoring path). Not decided.
- **T44/T76's staging-vs-production distinction, now sharper.** Session 10 confirms staging HealthSherpa access exists and has been used to warm real cache data; only production allow-listing is missing. This slightly changes the shape of T76 (cache warm-on-miss) too, since the "no key exists anywhere" premise those items were filed under during July is now half-resolved.

---

## Contradictions found

- **T132 — the `proposal-content-page` skill's token table.** Four of its eight documented tokens don't match `buildTokenMap`: `AGENT_PHONE` and `CURRENT_DATE` don't exist at all; `SECONDARY_COLOR` should be `ACCENT_COLOR`; `PROPOSAL_DATE` should be `DATE_CREATED`. **This is not a theoretical gap — two of the wrong ones were pasted into live HTML and rendered as literal braces on a generated customer-facing proposal PDF this session.** Corrected in the pasted HTML directly; the skill file itself is filed as T132, not fixed here (out of this run's scope fence).
- **T133 — `replaceTokens`'s unmatched-token behavior is a general exposure, not an ICHRA-specific one.** S10-E (§3) documented the mechanism precisely — an unmapped `{{TOKEN}}` renders as literal text — and T128 worked around it *only* for the eleven ICHRA keys by always populating them. Every other token in the system, including the four the skill gets wrong, remains exposed to exactly the defect that just reached a real proposal.
- ⚠️ **`docs/ichra_strategy.md` is now substantially stale, flagged and not edited (it is Kevin's file, out of every S10 run's scope fence and this one's).** §4's "What is actually built, and what a user can do with it right now" is dated 2026-07-31 and states plainly: *"What a user can do today: **nothing**."* That was accurate on 2026-07-31. As of session 10 it is not — the Proposal Builder interjection, ZIP resolution, plan-year derivation, tier-1 section rendering, and the entitlement gate are all runtime-verified in production. §9 ("the sequence") and §12 ("open items by owner") likely carry the same staleness by extension, though this run did not audit them line by line. **Recommend Kevin update §4 before it misleads anyone reading the strategy doc cold.**

---

## Next

**Immediate, Kevin's:** obtain production HealthSherpa allow-listing (T136). This is the one item standing between "page 3 renders with no code change" and the current blank state — S10-G's provenance gate already reads `source_env` per row and needs nothing further once production-sourced rows exist.

**Immediate, cheap, either of us:** T132 (fix the skill's token table) and T134 (Create Proposal button disabled-state CSS) are both small, low-risk, and prevent the next person from repeating this session's PDF defect or being confused by a button that looks clickable when it isn't.

**Not immediate, and Kevin's alone — see the honest accounting below.**

---

## SQL close-out audit — whole session (S10-A through S10-Z)

**One migration, applied.** `V087__proposal_ichra_intake.sql` — created S10-A (spec, text only, no file), written S10-B, and **applied to production by Kevin on 2026-08-03**, released in `v0.87.00`. No other run this session produced, ran, or recommended any SQL — S10-D, S10-E, S10-F and S10-G all worked from tables that already existed (`proposal_section`/`proposalsectionlos` from V036/V037, `proposal_ichra_intake` from V087, `rating_area_rate_cache` from V074/V078).

**Current highest version:** `ls docs/migrations/*.sql` → 64 files, highest `V087__proposal_ichra_intake.sql`. Unchanged since S10-B; no run after it added a migration.

**Orphaned `.sql` files:** none created this session. Pre-existing, unrelated: `docs/migrations/seed_ndt125_questionnaire.sql` (no version prefix), `docs/schema_version_migration.sql`, `docs/updates/update_V039_to_V057.sql`, `docs/importscript/*`.

**Pending deployment:** nothing SQL-related. All code from this session is in `v0.87.01`.

**⚠️ `docs/analysis/migration_tracker.md` still shows V087's Production cell as ⬜ — this is drift, not a fresh finding, and this run cannot fix it.** The per-environment table (line ~131) has not been flipped since S10-A/B correctly recorded it unapplied at authoring time; Kevin's 2026-08-03 production apply postdates that. `docs/analysis/migration_tracker.md` is **not** in this run's scope fence (only `project_backlog.md` and this close-out are), so the flip itself must happen in a run that includes that file. **Flagging it explicitly rather than letting it repeat the exact drift CLAUDE.md's "Keeping state docs current" section already warns about** — the V072/V073 case that let the tracker's Production column drift 49 versions before a 2026-07-30 reconciliation. This is the same failure mode, caught same-day instead of months later.

**Schema described but not scripted:** none this session.

---

## ⚠️ The honest accounting

Session 9's close-out stated plainly that seven of its eight runs went to access-control hardening rather than ICHRA. Session 10 is the inverse — **every run was ICHRA** — so the honest accounting here is a different question: not "did we spend the session on the right thing," but **did Forrest get closer to seeing something?**

**Yes, concretely, for the tier-1 artifact.** Page 1 and page 2 — the consultation-invite pages, SSA's administration voice, the employer's own county and headcount echoed back — run end to end today, on production, from an agent selecting a plus-tier LOS in the builder through a rendered proposal PDF. This was runtime-verified this session, not merely code-verified: the intake panel appeared, the ZIP resolved, the proposal was created without regression, the tokens rendered on a real PDF, and — unplanned but decisive — the entitlement gate was observed doing exactly its job when Kevin's own unentitled home agency lost the sections and got them back on flipping the flag. **This is demonstrable now.**

**No, for the market-data artifact, and the reason is not code.** Page 3 — plan counts, carrier counts, premium floors — does not populate, and Kevin's own production query pinpointed exactly why: the rate cache is fully warmed for the reference county but every row is staging-sourced, and the entitlement/provenance gate correctly refuses to show staging figures with a dollar sign on an unauthenticated employer-facing link. **The gate is not the blocker; production HealthSherpa access is.** That is now the single named item (T136) standing between the current state and a complete tier-1-through-tier-3 demo, and it requires nothing from this codebase to resolve.

⚠️ **Three SWBD emails to Forrest remain unsent — carried forward from session 9, and from every session before it.** They gate the top-ranked sale-motion items, and `/GroupConversion` has still never been fed real data. **This is now the longest-standing open item in the project, and it is Kevin's alone** — no build run, however careful, moves it. The tier-1 artifact this session made demonstrable is worth nothing to Forrest until someone sends it to him.

---

## Compliance statement

**Scope fence, restated.** Writable: `docs/session_closeout_2026-08-03_session10.md` (new) and `docs/analysis/project_backlog.md` (rows this run files, closes, or updates with session-level facts the repository could not otherwise record — T125/T128/T129/T130/T131's status cells, and new rows T132–T136). **Nothing outside that set was written.**

**No code was written and no SQL was run.** This entire run is documentation: two files, zero lines under `src/`, zero lines under `docs/migrations/`, zero database access (this container has none — see every prior S10 close-out).

**`docs/ichra_strategy.md` was read and its staleness is recorded above — it was not edited.** It is Kevin's file, outside every S10 run's scope fence including this one.

**No git operation other than the preflight reads and the close-out `add`/`commit`/`push`/`log` was run.** No `git add -A`, no tag, no stash, checkout, restore, or reset.
