# Session 50 Close-out — 2026-09-12

## Scope

Documentation-only run against a scope fence: `docs/analysis/legal_assumptions.md`,
`docs/analysis/summit_import_contracts.md` (new), `docs/analysis/migration_tracker.md` (read only),
and this close-out. No source, migration, or JSP files were in scope, and none were touched. No git
mutation ran (no `add`/`commit`/`stash`/`checkout`/`restore`, no tags) — the working tree is left
dirty for Kevin.

## Decisions made

- **LA-38** moves from *awaiting Presidio's position* to *awaiting an approved amended form.*
  Design unchanged — post-tax remains the default Presidio bucket, no SSA-originated ACH in any
  bucket, pre-tax still gated on both LA-37 and a written §V.H resolution.
- **Import Plan ID naming convention becomes strictly alphanumeric**, forced by
  `125 PI Contributions` rejecting any non-alphanumeric character in that field. The
  `{employerCustomId}-{key}` convention is unusable as-is; AMS's plan-ID generator must change.
  This is forward-only — no existing SSA plan currently receives imports, and `Import Plan ID` is
  editable on an active plan as a remediation path.
- **Single Fund is the presumptive ICHRA funding method for PremiumPath**, pending Kevin's
  confirmation — the tier is the source of truth, schedule vintage drops out of the calculation,
  and per-payroll-frequency tier combinatorics disappear.

## New assumptions / register changes

- **LA-38** — Status and Confirm before updated (see verbatim before/after in the compliance
  statement below). Reversal cost unchanged.
- **LA-22** — specified-disease breadth recorded as a forward reclassification risk (a future
  tri-agency rule narrowing "specified disease" excepted-benefit treatment would fall hardest on a
  schedule this broad), alongside corroboration that the expense-incurred reading was presented to
  and accepted by Presidio's chief legal officer on the second call, and that Presidio's own prior
  internal read of the product was fixed indemnity. Reversal cost: none — this is a risk record,
  not a design change.
- **LA-37** — frequency note added: the SWBD setup-fee tiers agreed on the second call make
  ≤5-life groups the expected volume case, raising the practical frequency of the more-than-2%
  exclusion. Marked explicitly as an inference from the fee-tier structure, not anything said on
  the call about ownership composition.
- **No new LA entry was warranted from the call.** Everything the call produced attaches to an
  existing entry (LA-22, LA-37, LA-38); none of it stands as an independent assumption.

## Open questions raised — and who settles them

- Presidio's amended §V.H form: settled by TDI approval, not by Presidio's word. No date given.
- LA-36 option B (PTO sale) — the elective-PTO basis rests on secondary commentary only; a
  primary-text read is no longer deferrable. Kevin or counsel.
- How per-employee premium amounts reach SSA, and in what batches. Kevin named this on the call as
  the one thing whose shape he does not know. Unowned.
- `Plan Status` numeric code set on `125 PI Elections`. Unowned.
- Does a template-level `Default Value` fire on a blank file field? Untested, assume yes.
- Card production trigger: record creation or effective date? Unowned — the early-enrollment
  rationale in the implementation sequence depends on this.
- How a multi-tier Single Fund plan resolves a tier when the file supplies none (a Default flag?).
- What the separate `Enrollment` file type (distinct from `HRA Enrollment`) is for.
- What `Import for Process Approval` (on `125 PI Contributions` only) does.

## Contradictions found

- **`125 PI Elections` is not idempotent** (`Plan Already Enrolled` on a second election for an
  already-enrolled participant), contradicting the general "AMS emits full current state,
  re-sending is safe" principle recorded elsewhere for Employer Demographic, CDH Plan and
  Demographics. Both are true for their own file types — the doc set must not state the principle
  unqualified across all Summit file types. Flagged in `summit_import_contracts.md`'s
  contradictions section for reconciliation against `docs/business/summit_data_exchange.md`.
- Content-hash de-duplication means a byte-identical re-send is silently swallowed at the transport
  layer, regardless of what the import type itself would have done with it — a second, independent
  mechanism from the idempotency question above, and worth keeping distinct.
- Presidio's prior internal read of their own product as fixed indemnity contradicts nothing SSA
  has filed; recorded as corroboration on LA-22, not as a conflict to resolve.

## SQL close-out audit

**No SQL was produced, run, or recommended by any of this session.** Task 1, 2, 3, 4 and 5 are
documentation-only; Task 5 in particular records import-behavior findings, not schema changes.

Current state:
- **Highest migration version: V102** (`employer.custom_id`, T241, authored 2026-09-11) — one
  version ahead of this project's `MEMORY.md`, which still read V101 as of its last update;
  `ls docs/migrations/` confirms `V102__employer_custom_id.sql` is the latest file on disk.
- **Both Production and the dev box are at V102 — nothing is pending release.**
- Nothing in this session adds to, or changes, that state.

⚠️ **Disagreement with `docs/analysis/migration_tracker.md`, not corrected there (out of scope for
this edit):** the tracker's Version History table still shows Production at V096 (`v0.96.00`,
2026-09-09) with V097 through V102 all reading unapplied (⬜) for `beta_ssa` (work), `beta_ssa`
(home), `dev_ssa` and Production alike. That figure is stale as of this correction and needs an
update pass of its own.

## Next

Two AMS-facing items from the Presidio call, recorded here as raised on the call — not scheduled,
not built:

- **Tracy provisioned as an agent-portal user** — quote generation, application link, employer
  prefill, census exchange. All of this maps onto the existing Opportunity → Proposal →
  Application → Setup pipeline with no new surface implied. The census exchange piece is on the
  critical path for card fulfillment, not a convenience — it is how a participant's data reaches
  Summit in the first place.
- **Exception reporting on unpaid premium**, so the agent hears before a cancellation rather than
  after.

---

## Compliance statement

### Verbatim before-text of every line edited

**LA-38 — Confirm before (before):**
> Any pre-tax salary reduction for a Presidio premium; any ACH origination to Presidio.

**LA-38 — Status (before):**
> Assumed on the form text; awaiting Presidio's position on §V.H.

**LA-22 — Status (before, two blocks; nothing between "Reversal cost" and "Confirm before" was
touched, and neither was edited — this section only had text inserted before it, not replaced):**
> **Status.** Assumed. Arrears loading is available as a no-cost hedge, since it is being built for
> the ACA bucket regardless. The asymmetry is deliberate and is the reason the same card mechanism
> runs differently by benefit type: excepted benefits sit outside the market reforms and carry a
> §106 shelter on unrecovered premium, and individual major medical has neither. See LA-36 for the
> funding paths available on the major medical side.
>
> **2026-09-09 status update.** No longer a bare assumption. Supported by primary documents (state
> form approval + form text), with residual exposure to (a) future tri-agency rulemaking of the
> 2023 kind and (b) a substance-over-form challenge to the breadth of the condition schedule. The
> forms read are marked Revised 10/17/2025, post-dating the 2025-08-20 SERFF submissions, and the
> issued condition schedule is bracketed/variable — so both the approved text and the issued subset
> remain unconfirmed with the carrier. See LA-38 for the separate question of who may pay Presidio
> premium.

**LA-22 — before the "Reversal cost" line (nothing existed here to quote as "before"; two new
addendum paragraphs were inserted, nothing removed):** N/A — insertion only.

**LA-37 — before the insertion (nothing existed after the Status line to quote; one new paragraph
was appended, nothing removed):** N/A — insertion only.

### Task-by-task disposition

1. **Completed.** LA-38 Confirm before and Status both updated exactly as scoped; before-text
   printed above; design fields (assumption, basis, design choice, risk-if-wrong, what-this-does-
   not-affect, reversal cost) untouched.
2. **Completed.** LA-22 read in full before editing; forward reclassification risk and CLO
   corroboration added as two dated addenda ahead of "Reversal cost," in the same addendum style
   the entry already uses. No restructuring — nothing removed or reordered.
3. **Completed — record does not exist.** `presidio_call_record.md` was not found anywhere in the
   repository (recursive search, no match). Per the task's own branch for this case, nothing was
   created; the record lives in project knowledge only.
4. **Completed.** LA-37 exists and has a natural place for the note (its Status line). The
   entity-type/who's-buying qualifying-question language the task describes was already present in
   LA-37's Design choice from a prior session — only the SWBD-volume frequency inference was new,
   and it is added as its own paragraph, explicitly marked as an inference from the fee-tier
   structure rather than anything said on the call.
5. **Completed.** `docs/analysis/summit_import_contracts.md` created new. A Summit data-exchange
   doc already exists at `docs/business/summit_data_exchange.md` — per instructions this file was
   NOT merged into it; the overlap is noted both in the new file's header and in this close-out's
   Contradictions section above, as a reconciliation item for later, not done here.
6. **Completed.** This document.

### Confirmations

- **No git mutation ran.** No `add`, `commit`, `stash`, `checkout`, `restore`, or tag command was
  executed at any point in this session.
- **No file outside the scope fence was touched.** Files written or edited this session:
  `docs/analysis/legal_assumptions.md` (edited), `docs/analysis/summit_import_contracts.md`
  (created), `docs/analysis/migration_tracker.md` (read only — not edited), and this file. Task 3's
  "one new file, only if positive" branch did not fire, since the check came back negative.
