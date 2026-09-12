# Summit Import Contracts — Empirical Test Results (2026-09-11/12)

**Status:** Findings only. No SQL, no code, no migration was produced or is proposed by this
document. Every statement below is an observed test result against demo employer `ZZSDX27A`
(System ID 1394) in Summit, not a design decision and not an assumption.

**Overlap note:** `docs/business/summit_data_exchange.md` already exists as the Summit
data-exchange spec/reference doc. This file does **not** merge into it — it stands alone as the
raw empirical record from the 2026-09-11/12 test session. Reconciling the two (folding proven
facts into the spec, flagging where the spec's existing text is contradicted) is follow-up work,
not done here.

**Emitter specification:** `docs/analysis/summit_import_spec.md` is the field-level emitter contract
built from these findings — this file remains the record of how the behaviour was established.

**Related:** `docs/business/summit_data_exchange.md` · `docs/analysis/legal_assumptions.md`
(LA-22, LA-38) · `docs/business/swbd_premiumpath.md`

---

## Cross-cutting (all import types)

- Results file body format: `Participant TPA Custom ID|Status|Message`, pipe-delimited, no header.
- Success tokens observed: `Successful|Success` (125 PI Elections, HRA Enrollment);
  `Successful|Contribution Import completed successfully` (125 PI Contributions).
- Column-count validation is **whole-file, pre-processing**. One malformed row rejects the entire
  file with `Failed: the validated file contains N columns. The file template defines M columns.`
  and 0 records processed. **No partial-success safety net** — unlike Demographics, where bad rows
  failed individually.
- **De-duplication is on content hash, not filename.** A byte-identical file under a new name is
  held and not reprocessed. AMS must guarantee content variance on any re-send, not just a unique
  filename.
- `Validate Import Format` (Import Setup) validates a file without processing it. Fast loop for
  format questions; reports per row.
- Optional elements must never be the last mapped column. `125 PI Elections` and `HRA Enrollment`
  have no mandatory element available as a trailing sentinel, so **map a `Filler` last and set it
  Mandatory**.
- Template-level `Default Value` exists per element. Whether it fires on a blank file field is
  **untested** — assume it does; leave all Default Values empty.

## `125 PI Elections`

- Mandatory: `Employer TPA Custom ID`, `Participant TPA Custom ID`, `Import Plan ID`. Everything
  else optional, including `Effective Date`.
- `Plan Status` is **Numeric** — a code, valid set unknown. Left unmapped. (Open question.)
- **Contribution schedules must be in the plan's allowed set** (`Participant Contribution
  Schedule` multi-select on the benefit) or the row fails `Participant Contribution Schedule 'X'
  not found`. Proven three times, including once accidentally. The message points at the schedule,
  not at the plan — it reads like the schedule doesn't exist when in fact it isn't attached to the
  plan.
- **The plan-level allowed set is not importable.** `Employer CDH Plan` has no element for it. It
  is a mandatory hand step per plan, after plan import and before any enrollment file.
- Once in the allowed set, **global schedules resolve from the file** — this is what makes a
  TPA-wide global schedule list viable instead of per-employer schedule creation.
- The allowed set **carries across plan years**; it does not need re-selecting after renewal.
- Schedule dropdown at plan level is **filtered by funding source** — an employer-only schedule is
  not selectable on a participant-funded plan. A both-funded schedule is selectable on either, so
  one both-funded global per frequency covers 125 and ICHRA alike.
- Schedule names are unique within **{global list} ∪ {that one employer's list}**. Two different
  employers may each hold the same name; an employer schedule may **not** take a global's name.
  → AMS may use a fixed naming convention, but it must never collide with a global name.
- **Elections do not upsert.** A second election for an enrolled participant fails
  `Plan Already Enrolled`. A re-sent mixed file partially succeeds. This file is **not idempotent**,
  contrary to the general "AMS emits full current state" principle that holds for Employer
  Demographic, CDH Plan and Demographics.
- **Amount behaviour.** Summit divides the annual election across the schedule's run dates falling
  on or after the effective date, then immediately posts every such date already elapsed.
  - Observed: annual 1200 on a schedule with 9 remaining dates → 133.33/period, 1 elapsed →
    $133.33 posted and **disbursable**.
  - Same election on a schedule with 52 dates, 37 elapsed → **$853.88 posted**.
  - Per-contribution supplied alone (no annual) → annual is **derived**: 50.00 × 9 = $450.00.
  - **Both supplied → the supplied per-contribution is stored and displayed but ignored for
    posting**; the annual governs. Produces a visibly inconsistent record (grid showed 50.00,
    actual post 133.33). **AMS must emit annual (column G) only, never both.**
- **Manual Processing does NOT suppress the enrollment-time back-post.** Processing Type governs
  the recurring run, not the catch-up.
- **A current or future effective date eliminates back-posting entirely** ($0.00 posted). This is
  the only reliable control, and it is a field AMS owns. Trade-off: the per-contribution figure
  inflates because the annual is spread over fewer remaining periods.
- **Schedule vintage drives the money.** Identical elections produced $133.33 / $150.00 / $853.88
  purely from when each schedule was created. Not a stable contract; prefer long-standing globals.
- An election with **no schedule at all** is accepted: annual recorded, $0.00 posted, $0.00
  disbursable, Last/Next Date and Number of Contributions all N/A. **This is the expectation-only
  election.**
- `Plan Start Date` (column E) is a **cross-check, not a selector**. It must agree with the
  effective date; disagreement returns `Plan Not Found`. It is not needed to target a plan year
  even after renewal. **AMS omits it** — it can only turn a working row into a failure.
- **Renewal is additive.** Assigning a new plan year creates a second independent Active plan
  record; the prior year's election and balances survive intact. The effective date alone routes a
  row to the right year. ⚠️ After renewal, `Import Plan ID` identifies a *family* of plan years —
  an effective-date bug posts to the wrong year rather than failing.
- A renewal election **does not inherit the prior year's schedule** — it carries whatever the file
  says. Ongoing-enrollment files must emit schedules explicitly for everyone.

## `125 PI Contributions`

- Mandatory: `Employer TPA Custom ID`, `Participant TPA Custom ID`, `Import Plan ID`,
  `Participant Contribution Amount`. No schedule element exists on this file type.
- ⚠️ **`Import Plan ID` on this file type rejects any non-alphanumeric character** with
  `Invalid data for Import Plan ID`. Proven by `Validate Import Format`: `ZZSDX27A-INS125A` and
  `ZZSDX27A_INS125A` failed; `ZZSDX27AINS125A`, `INS125A`, `1394` and `ABC123` all passed —
  including `ABC123`, which resolves to nothing. **It is a character check, not a lookup.**
  - **Consequence: the `{employerCustomId}-{key}` Import Plan ID convention is unusable.** AMS's
    plan-ID generator must produce strictly alphanumeric IDs.
  - This is **forward-only** — no existing SSA plan currently receives imports. Remediation path is
    proven: `Import Plan ID` is editable on an active plan.
  - Elections and HRA Enrollment accept hyphens in the same field. The restriction is specific to
    this file type.
- **A contribution posts to an election with no funding schedule.** Proven. This is what makes the
  expectation-only election fundable and is the mechanism the implementation sequence depends on.
- Contributions **stack** on top of scheduled postings ($150 back-post + $100 file = $250) and
  increment `Number of Contributions`. Nothing observed caps the total at the annual election.
- `Update Participant Annual Election` (Boolean): token **`Y` is valid**. It **increments** the
  annual election — $1,200 + $75 contribution → **$1,275 annual**. It does not replace.
  ⚠️ Useful for the $1-seed flow (the election grows as premium arrives) but the seed must be
  accounted for. `true` untested.
- `Plan Effective Date` optional; supplying it changed nothing.
- `Import for Process Approval` checkbox exists on this file type only (not on Elections). Purpose
  unverified; left unchecked for testing.
- Plan history distinguishes `Method`: `Plan Import` vs `Contribution Import`.

## `HRA Enrollment`

- Mandatory: `Employer TPA Custom ID`, `Participant TPA Custom ID`, `Import Plan ID`,
  **`Effective Date`** (mandatory here, unlike Elections).
- No employer-amount element exists. Employer money comes from plan configuration, never the file.
- `Tier ID` is free text, **exact match**. ⚠️ A `0`/`O` transposition produced `No matching Tier`.
  **AMS should generate Tier IDs and avoid digit/letter-lookalike characters.**
- Two distinct failure messages: `No matching Tier` (value supplied, no match) vs
  `No tier identified` (nothing supplied, nothing to fall back on).
- **The `Tier ID` requirement inverts by `Employer funding method`:**
  - **Contribution Schedule** (tier-funded): `Tier ID` is **mandatory** in practice on a multi-tier
    plan with no default tier set, despite the element being listed Optional.
  - **Single Fund**: `Tier ID` must be **omitted** — supplying it fails `Not a tier funded plan`.
    The tier is derived from plan configuration; the manual enrollment screen offers no tier
    selection and resolves it automatically.
  - ⚠️ **AMS cannot see which funding method a plan uses.** This must be an operator input or
    per-plan config, and it changes the emitted row shape.
- Parentheses and spaces in schedule names resolve fine (`Excepted Benefit Monthy (1st)`). The
  alphanumeric restriction is specific to `Import Plan ID` on `125 PI Contributions`.
- **The ICHRA allowance surfaces as `Employer Funding`, not `Annual Election`.** Annual Election
  stays $0.00 on an employer-funded plan; that field is for participant elections.
- Also available as elements, unused: `Plan ID` (Summit system-generated key — an alternative to
  `Import Plan ID`), `Plan Dependent Link`, `Record Processing Status`, `Record Comment`,
  `Tier Name`.
- A separate `Enrollment` file type exists that looks identical. Purpose unknown.

## Plan configuration facts that govern the above

- The template's `Funding source(s)` controls which schedule fields render on the plan.
  `Ins125+ Excepted Benefit` (template 1031) is Participant-only, so **`Employer Contribution
  Schedule` has nowhere to land on an Ins125+ plan** — AMS never emits it on a 125 row.
- `Participant funding method` is **not editable on an active benefit**. Options:
  `Contribution Schedule` (balance = accumulated postings) or `Annual` (balance = annual election
  regardless of payroll).
- `Employer funding method`: `Contribution Schedule` or `Single Fund`.
  - Under **Single Fund**, `Employer Contribution Schedule` and `Employer Funding By` **disappear
    entirely**; the tier column is `Employer Annual Amount` and a `Pro-Rate Employer Amount`
    dropdown appears (`None` vs prorate on effective date).
  - Observed: Single Fund + Pro-Rate `None` + tier 6000.00, effective 1/1/2026 → **$6,000 Employer
    Funding, $6,000 disbursable immediately**, no schedule on either side.
  - Under **Contribution Schedule** with a 2027 plan year, the same $6,000 tier produced **$0.00**
    — correct, since no run date has arrived, but it means there is no readable expected-annual
    until money posts.
  - ⭐ **Single Fund is the stronger fit for PremiumPath ICHRA**: the tier is the source of truth,
    schedule vintage drops out of the calculation, and frequency stops being a tier dimension
    (no tier-per-payroll-frequency combinatorics).
- Under Contribution Schedule + `Employer Funding By: Contribution Schedule`, the tier box is a
  per-contribution figure multiplied by the schedule's period count; under `Annual Amount` it is
  the year's total.
- `Tier ID` and `Default` columns only render in the tier grid once a second tier exists.
- Plan `Effective Date` is a floor on the earliest coverage date; the **plan year** is the binding
  constraint.
- `Import Plan ID` is editable on an active plan.

## Kevin's implementation sequence — validation status

For any implementation date, §125 benefits need a plan year opening at least one payroll cycle
earlier, so the pre-tax deduction for the implementation month posts in the prior month. ICHRA gets
no prior-year leg. A "generic" card-enabled benefit at implementation-minus-three-months drives the
AMS renewal prompt (AMS reads renewal timing off effective date, one month ahead) so the ICHRA
notice cycle has a hook.

| Step | Status |
|---|---|
| 1 — generic benefit, prior plan year | Supported |
| 2 — census enrolled at $1, future effective date | **Proven** — future effective date posts $0.00 |
| 3 — real benefit enrolled with **no schedule** | **Proven** — expectation-only election accepted |
| 4 — `125 PI Contributions` posts the deduction | **Proven** — posts to a no-schedule election |
| 5 — manual renewal to next plan year | **Proven** — additive, prior year intact |
| 6 — ongoing `125 PI Elections` | **Proven** — renewal unlocks re-enrollment |

⚠️ Card production timing was never tested — whether Summit initiates a card at record creation or
at the effective date is **unknown**, and the early-enrollment rationale depends on it.

## Open questions

- `Plan Status` numeric code set on `125 PI Elections`.
- Does a template-level `Default Value` fire on a blank file field?
- Card production trigger: record creation or effective date?
- How a multi-tier Single Fund plan resolves a tier when the file supplies none (Default flag?).
- What the separate `Enrollment` file type is for.
- What `Import for Process Approval` does.

## Contradictions with existing doc-set framing

- **`125 PI Elections` is not idempotent** (`Plan Already Enrolled`), contradicting the general
  "AMS emits full current state, re-sending is safe" principle recorded elsewhere for other file
  types (Employer Demographic, CDH Plan, Demographics). Both are true for their own file types;
  no doc should state the principle unqualified across all Summit file types.
- Content-hash de-duplication means a byte-identical re-send is silently swallowed at the transport
  layer, regardless of what the import type itself would have done with it.
