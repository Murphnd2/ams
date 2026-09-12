# Summit import file specification

**Status:** established empirically 2026-09-11/12 against demo employer `ZZSDX27A` (System ID 1394).
Behavioural evidence is in `summit_import_contracts.md`; this file is the emitter contract.
Per-template column layouts, template-level settings and the 2026-09-12 picker reads are in
`summit_import_templates_reference.md` — the working reference for all six templates.

⚠️ Everything here is observed unless explicitly marked untested. Anything marked untested must not be
relied on without a test.

---

## 1. Template configuration (all file types)

Set identically on every AMS-facing template:

| Setting | Value |
|---|---|
| File Format | Delimited |
| Delimiter | `|` (pipe) |
| Date Format | `YYYYMMDD` |
| Extraneous Data | No |
| Include Header | unchecked |
| Include Footer | unchecked |
| Include Body Record Indicator | unchecked |
| Produce Results File | checked, **not** Errors Only |
| Default Value (every element) | **empty** |

**Rules that follow from the platform, not from preference:**

- ⚠️ **A `Filler` element must be mapped last and set Mandatory on `125 PI Elections` and
  `HRA Enrollment`.** Optional elements cannot safely be the final column *on those templates*, and
  neither has a mandatory element available to serve as a trailing sentinel. AMS populates it with a
  constant (`X`). ⚠️ Narrowed 2026-09-12: this is **not** a platform-wide rule — `ZZ_TEST_CDH` ends on
  eight optional columns and imports with all of them empty, and `ZZ_TEST_ER` tolerates a blank
  trailing column. Keep the sentinel where it is proven necessary; test any new template rather than
  assume (`summit_import_templates_reference.md`, rules).
- ⚠️ **Leave every `Default Value` empty.** Whether a template default fires on a blank file field is
  **untested**; a default would be invisible in the file and would make blank fields uninterpretable.
- ⚠️ **Column count is validated whole-file, before processing.** One malformed row rejects the entire
  file, 0 records processed. There is no partial-success safety net on these types. The emitter must
  guarantee field count structurally.
- ⚠️ **De-duplication is on content hash, not filename.** A byte-identical re-send is silently held.
  The emitter must guarantee content variance, not just a unique filename.

**Results file format:** pipe-delimited, no header. Success tokens: `Successful|Success` for Elections
and HRA Enrollment; `Successful|Contribution Import completed successfully` for Contributions.
⚠️ **The line shape is not constant across templates** (corrected 2026-09-12, session 51 — this
line previously read "all types: `Participant TPA Custom ID|Status|Message`"). Observed 2026-09-12:
**three variants** —
(1) Employer Demographic: `Status|Employer Name|Employer TPA Custom ID|Message`, **Status first**,
4 fields (re-observed on `ZZ_TEST_ER` 2026-09-12; Employer CDH Plan is also status-first per
`docs/business/summit_data_exchange.md` SDX-18); (2) Demographics:
`Participant TPA Custom ID|Status|Message|Employer TPA Custom ID`, 4 fields, status **second**;
(3) HRA Enrollment: `Participant TPA Custom ID|Status|Message`, 3 fields, status second. **The
status field is not in a fixed position; a fixed-position parser is unsafe.** ⚠️ **Implication for code, not changed here:**
`SummitResponseService.check` classifies every line on `fields[0]`; on any template whose first
field is the participant key rather than `Status`, every line classifies `UNKNOWN`. Per-template
detail: `summit_import_templates_reference.md`, "Rules that apply to every file".

---

## 2. `125 PI Elections`

Creates a participant's election on a §125 plan. Used at initial enrollment and again after renewal.

### Column layout AMS emits

| Col | Element | Type | Req | AMS emits |
|---|---|---|---|---|
| A | Employer TPA Custom ID | AlphaNumeric | Mandatory | always |
| B | Participant TPA Custom ID | AlphaNumeric | Mandatory | always |
| C | Import Plan ID | AlphaNumeric | Mandatory | always |
| D | Effective Date | Numeric | optional | **always** — routes to plan year, controls back-posting |
| E | Plan Start Date | Numeric | optional | **never** — see concerns |
| F | Coverage End Date | Numeric | optional | on termination only |
| G | Participant Annual Election Amount | Numeric | optional | **always** |
| H | Participant Per Contribution Amount | Numeric | optional | **never** — see concerns |
| I | Participant Contribution Schedule | AlphaNumeric | optional | when the election is funded by schedule |
| J | Employer Contribution Schedule | AlphaNumeric | optional | **never on an Ins125+ plan** |
| K | Filler | AlphaNumeric | **set Mandatory** | constant `X` |

Offered but unmapped: `Plan Status` (Numeric — code set unknown; see open questions).

### Field semantics

**C — Import Plan ID.** Identifies the benefit. ⚠️ After a renewal it identifies a *family* of plan
years, not one plan record; the effective date selects which. Must be strictly alphanumeric (§5).

**D — Effective Date.** Sets the participant's plan effective date, independent of the plan's own.
Selects the plan year. **Controls back-posting** — see the amount model below.

**E — Plan Start Date.** ⚠️ **Not a selector — a cross-check.** It must agree with D; disagreement
returns `Plan Not Found`. It is not needed to target a plan year even after renewal. Supplying it can
only turn a working row into a failure. **Omit.**

**G / H — the amount model.** Summit divides the annual election across the schedule's run dates falling
on or after the effective date, then immediately posts every such date already elapsed.

- Annual supplied alone → per-contribution derived. Observed: 1200 over 9 remaining dates → 133.33.
- Per-contribution supplied alone → **annual derived**. Observed: 50.00 × 9 → $450.00 annual.
- ⚠️ **Both supplied → H is stored and displayed but ignored for posting; G governs.** Produces a record
  showing two different numbers for the same thing (grid 50.00, actual post 133.33).
  **Emit G only, never both.**

**I — Participant Contribution Schedule.** Must be a name in the **plan's allowed set**
(`Participant Contribution Schedule` multi-select on the benefit) or the row fails
`Participant Contribution Schedule 'X' not found`.

- Global schedules resolve from the file once they are in the allowed set.
- The allowed set carries across plan years — no re-selection after renewal.
- ⚠️ A renewal election **does not inherit** the prior year's schedule. Emit it explicitly every time.
- Omitting the column is valid and produces an **expectation-only election**: annual recorded, $0.00
  posted, $0.00 disbursable, Last/Next Date and Number of Contributions N/A.

**J — Employer Contribution Schedule.** ⚠️ Structurally meaningless on `Ins125+`. The template's
`Funding source(s)` is Participant-only, so no employer schedule field renders on the plan and
employer-only schedules are filtered out of the plan's dropdown. **Never emit on a 125 row.**

### Concerns

- ⚠️ **Not idempotent.** A second election for an enrolled participant fails `Plan Already Enrolled`.
  A re-sent mixed file partially succeeds. This contradicts the general "AMS emits full current state,
  re-sending is safe" principle that holds for Employer Demographic, CDH Plan and Demographics.
  (⚠️ Qualified 2026-09-12: on Employer Demographic a re-send is *accepted* but **an empty cell
  clears the stored value** — F3, `summit_import_templates_reference.md` §1 — so "safe" there means
  no duplication, not no data loss.)
  **The emitter needs an operator gate** — same shape as the existing `confirm=ENROLL-ALL-P{id}` token.
- ⚠️ **A backdated effective date creates immediately spendable money.** Observed: effective 1/1/2026 on
  a 52-date schedule posted **$853.88 disbursable** at enrollment. On a card-funded plan that is live
  funding for premium never withheld from anyone's payroll.
- ⚠️ **Manual Processing does not prevent this.** Processing Type governs the recurring run, not the
  enrollment-time catch-up. A current or future effective date is the only reliable control.
- ⚠️ **Schedule vintage drives the money.** Identical elections produced $133.33, $150.00 and $853.88
  purely from when each schedule was created. Prefer long-standing globals; do not treat the derived
  per-contribution figure as a stable contract.
- ⚠️ **The plan's allowed set is a mandatory hand step** — not importable via `Employer CDH Plan`, and
  invisible to AMS. Forgetting it is the most likely production failure; it happened once during
  testing. ⚠️ Summit's error names the schedule, which reads as *the schedule doesn't exist* when the
  truth is *it isn't attached to the plan*. **Any AMS operator prompt must say "confirm the schedule is
  selected on the plan," not "confirm the schedule exists."**
- ⚠️ After renewal, an effective-date bug **posts to the wrong plan year rather than failing.**

---

## 3. `125 PI Contributions`

Posts money to an existing election. Distinct from Elections; does not create enrollment.

### Column layout AMS emits

| Col | Element | Type | Req | AMS emits |
|---|---|---|---|---|
| A | Employer TPA Custom ID | AlphaNumeric | Mandatory | always |
| B | Participant TPA Custom ID | AlphaNumeric | Mandatory | always |
| C | Import Plan ID | AlphaNumeric | Mandatory | always |
| D | Participant Contribution Amount | Numeric | Mandatory | always |
| E | Plan Effective Date | Numeric | optional | not currently |
| F | Update Participant Annual Election | Boolean | optional | see below |
| G | Filler | AlphaNumeric | **set Mandatory** | constant `X` |

~~Offered but unmapped: `Employer Contribution Amount` (Numeric — the employer-money path, untested).~~
**Superseded 2026-09-12 (session 51):** the picker read on `ZZ_TEST_125_CONTRIB` on 2026-09-12 found
**no `Employer Contribution Amount` element on this template** — it is participant-money only (§9 #9
closed; `summit_import_templates_reference.md` §5). The earlier "offered but unmapped" reading is
retained struck through as the record of what session 50 believed.

⚠️ **Column position of the flag (F).** The additive test recorded the flag one column earlier; the
2026-09-12 picker read puts `Plan Effective Date` at E and `Update Participant Annual Election` at
F, as this table shows. **Build against F and re-check the picker before any emitter run** — a file
built to the old position writes `Y` into a Numeric date field.

### Field semantics

**C — Import Plan ID.** ⚠️⚠️ **This field rejects any non-alphanumeric character**, returning
`Invalid data for Import Plan ID`. Proven by `Validate Import Format`:

| Value | Result |
|---|---|
| `ZZSDX27A-INS125A` | **fail** |
| `ZZSDX27A_INS125A` | **fail** |
| `ZZSDX27AINS125A` | pass |
| `INS125A` | pass |
| `1394` | pass |
| `ABC123` | pass — **and resolves to nothing** |

`ABC123` passing proves this is a **character check, not a lookup**. See §5.

⚠️ **The restriction is column C only.** `Participant TPA Custom ID` (column B) as `158-P-S27-01`
was accepted by this same file type on 2026-09-12, returning
`Successful|Contribution Import completed successfully`. The character check does not extend to
column B — it is specific to `Import Plan ID` on `125 PI Contributions`.

**D — Participant Contribution Amount.** Posts as of processing; there is no posting-date element.
⚠️ Contributions **stack** on scheduled postings ($150 back-post + $100 file = $250) and increment
`Number of Contributions`. Nothing observed caps the total at the annual election.

**F — Update Participant Annual Election.** Token **`Y` is valid** (`true` untested).
⚠️ **It increments, it does not replace** — $1,200 annual + $75 contribution → **$1,275 annual**.
This suits the $1-seed flow (the election grows as real premium arrives), but the $1 seed remains in the
total and must be accounted for.

### Concerns

- ⭐ **A contribution posts to an election with no funding schedule.** This is the mechanism the whole
  implementation sequence depends on and it is proven.
- ⚠️ `Import for Process Approval` exists on this file type only. **It is a template-level checkbox,
  not a column** (confirmed on the 2026-09-12 picker read; currently unchecked on
  `ZZ_TEST_125_CONTRIB`) — so it never appears in the file and cannot be set per row. Purpose
  unverified by test; left unchecked in testing. It presumably routes the import to Process Approvals
  rather than posting on arrival — **this needs a deliberate production decision**, since it is the
  difference between money posting automatically and money waiting for a human.
- Plan history records `Method` as `Contribution Import` vs `Plan Import`, so the two are
  distinguishable in the audit trail.

---

## 4. `HRA Enrollment`

Creates a participant's enrollment on an employer-funded plan (ICHRA).

### Column layout AMS emits

| Col | Element | Type | Req | AMS emits |
|---|---|---|---|---|
| A | Employer TPA Custom ID | AlphaNumeric | Mandatory | always |
| B | Participant TPA Custom ID | AlphaNumeric | Mandatory | always |
| C | Import Plan ID | AlphaNumeric | Mandatory | always |
| D | Effective Date | Numeric | **Mandatory** | always |
| E | Tier ID | AlphaNumeric | optional | **depends on funding method — see below** |
| F | Employer Contribution Schedule | AlphaNumeric | optional | Contribution Schedule plans only |
| G | Participant Contribution Schedule | AlphaNumeric | optional | never |
| H | Filler | AlphaNumeric | **set Mandatory** | constant `X` |

Offered but unmapped: `Plan Year Id`, `Plan ID` (Summit system key — an alternative to `Import Plan ID`),
`Employer System ID`, `Participant System ID`, `Plan Type`, `Participant Annual Election Amount`,
`Plan Start Date`, `Plan End Date`, `Coverage End Date`, `Participant Per Contribution Amount`,
`Plan Status`, `Plan Dependent Link`, `Record Processing Status`, `Record Comment`, `Tier Name`.

⚠️ **There is no employer-amount element.** Employer money comes from plan configuration, never the file.

### Field semantics

**D — Effective Date.** Mandatory here, unlike Elections.

**E — Tier ID.** Free text, **exact match**, AlphaNumeric.

⚠️⚠️ **The requirement inverts by the plan's `Employer funding method`:**

| Funding method | `Tier ID` | Failure if wrong |
|---|---|---|
| **Contribution Schedule** (tier-funded) | **mandatory** in practice on a multi-tier plan with no default tier, despite being listed Optional | `No tier identified` |
| **Single Fund** | **must be omitted** — the tier is derived from plan configuration | `Not a tier funded plan` |

⚠️ **AMS cannot see which funding method a plan uses.** This must be an operator input or per-plan
config, and it changes the emitted row shape.

Two distinct failure messages exist and mean different things: `No matching Tier` (a value was supplied
and did not match) vs `No tier identified` (nothing supplied, nothing to fall back on).

⚠️ **A `0`/`O` transposition produced `No matching Tier` during testing.** AMS should **generate** Tier
IDs rather than accept typed ones, and avoid digit/letter lookalikes (`0`/`O`, `1`/`l`).

**F — Employer Contribution Schedule.** Must be in the plan's employer-side allowed set. ⚠️ Under
Single Fund the field does not exist on the plan at all — omit.

### Concerns

- ⚠️ **Under Contribution Schedule funding, there is no readable expected-annual.** The allowance
  materializes only as money posts. Observed: a $6,000 tier on a 2027 plan year showed $0.00 across
  Annual Election, Employer Funding and balances. Reconciliation must be **tier-based, not
  amount-based** on that configuration.
- ⭐ **Under Single Fund the allowance is readable** — it surfaces as **`Employer Funding`**, not
  `Annual Election` (which stays $0.00 on an employer-funded plan). Observed: tier 6000.00, Pro-Rate
  `None`, effective 1/1/2026 → **$6,000 Employer Funding, $6,000 disbursable immediately**, no schedule
  on either side.
- Parentheses and spaces in schedule names resolve fine (`Excepted Benefit Monthy (1st)`).
- A separate `Enrollment` file type exists that looks identical. Purpose unknown — noted in case a
  roadblock turns out to be a wrong-file-type problem.

---

## 5. The Import Plan ID naming standard

⚠️ **Import Plan IDs must be strictly alphanumeric — no hyphens, no underscores, no punctuation.**

The `{employerCustomId}-{key}` convention is **unusable**: a plan named that way can never receive a
contribution import, which means it can never be funded by file. Elections and HRA Enrollment accept the
hyphen, so the failure appears only later, at the point where money is supposed to move.

- AMS's plan-ID generator must enforce alphanumeric-only.
- **Forward-only.** No existing SSA plan currently receives imports, so historical IDs need no campaign.
- **Remediation is proven** where it is ever needed: `Import Plan ID` is editable on an active plan.
  ⚠️ But it is a foreign key from AMS's perspective — changing one silently breaks any file AMS emits
  with the old value.

---

## 6. Plan configuration that governs emitter behaviour

AMS does not control these, but what it must emit depends on them.

| Setting | Effect |
|---|---|
| Template `Funding source(s)` | Controls which schedule fields render on the plan. `Ins125+ Excepted Benefit` (1031) is Participant-only → no employer schedule field exists. |
| `Participant funding method` | `Contribution Schedule` (balance = accumulated postings) or `Annual` (balance = annual election regardless of payroll). ⚠️ **Not editable on an active benefit.** |
| `Employer funding method` | `Contribution Schedule` or `Single Fund`. Determines whether `Tier ID` is required or forbidden. |
| `Employer Funding By` | Under Contribution Schedule only. `Contribution Schedule` → tier box is per-contribution × period count; `Annual Amount` → tier box is the year's total. Absent entirely under Single Fund. |
| `Pro-Rate Employer Amount` | Single Fund only. `None` (full allowance regardless of entry date, observed) or prorate on effective date (untested). |
| Plan-level schedule allowed sets | Multi-select, filtered by funding source. **Not importable.** Hand step per plan. |
| Tier grid | `Tier ID` and `Default` columns only render once a second tier exists. |
| Plan `Effective Date` vs plan year | Effective date is a floor on the earliest coverage date; **the plan year is the binding constraint.** ⭐ An effective date *before* the plan year is accepted by import and stored verbatim (F5, 2026-09-12) — not coerced. |
| Plan template vs CDH row | **Funding tax treatment, Funding source and Participant funding method are set by the plan template, not the import row** (Edit Benefit Plan screen, 2026-09-12). The five PremiumPath plans differ by `Plan Template ID`, not by anything the CDH row carries. `+ Assign Plan Year` on that screen is the manual renewal step (§7 step 5). |

**Schedule naming.** Names are unique within {global list} ∪ {that one employer's list}. Two employers
may each hold the same name; an employer schedule may **not** take a global's name. A both-funded
schedule is selectable on both participant- and employer-funded plans, so **one both-funded global per
payroll frequency covers 125 and ICHRA alike** — the global list does not need doubling.

⚠️ Any AMS naming convention for schedules must never collide with a global name, or the save fails on
every employer. ⚠️ Global names are installation-specific — AMS needs a config-mapped
payroll-frequency → schedule-name registry, never literal strings in code.

~~⭐ **Single Fund is the stronger fit for PremiumPath ICHRA**, pending Kevin's confirmation:~~ the tier
becomes the source of truth, schedule vintage drops out of the calculation, and payroll frequency stops
being a tier dimension — avoiding tier-per-frequency combinatorics
(`EEOBIWEEKLY` / `EEOWEEKLY` / `FAMBIWEEKLY` / `FAMWEEKLY`) on any group with mixed payroll.

⭐ **Decided 2026-09-12 (Kevin, session 51) — Single Fund is NOT the ICHRA standard.** ICHRA and
QSEHRA will be **Contribution Schedule** funded with monthly stipends in almost all cases. The
Single Fund case above is retained as the analysis that was considered; it is not the design.
Consequences: AMS **does** emit `Tier ID` on HRA Enrollment rows; the tier-per-frequency question
is live again; and the open question of how AMS learns a plan's employer funding method (§4) stays
open rather than evaporating. Recorded as decided — do not relitigate.

---

## 7. Implementation sequence and rationale

Kevin's onboarding sequence, with the reason each step exists and its validation status.

### The problem being solved

Two timing problems, one sequence.

1. **Payroll precedes coverage.** Premium for the implementation month must be withheld pre-tax in the
   month *before* implementation. For a 1/1/27 group, December 2026 payroll deducts January premium —
   and that deduction has to post somewhere that exists in the **2026** plan year. So every §125 benefit
   needs a plan year opening at least one payroll cycle before the implementation date, even though the
   coverage is 2027.
2. **Cards precede information.** Cards take time to print and mail, and card production keys off
   enrollment in a card-enabled plan. Enrollment has to exist long before anyone knows which plan a
   person belongs in or what their premium is. So enrollment and funding are decoupled, and enrollment
   happens first against a placeholder.

ICHRA is deliberately excluded from the prior-year pattern: employer money, no salary-reduction timing
to solve, and funds should not be available before the implementation date. It gets an
implementation-year plan year only.

### The sequence (1/1/27 example)

| # | Step | Why | Status |
|---|---|---|---|
| 1 | **Generic** card-enabled benefit, effective implementation-minus-three-months, plan year 2026 | Something the whole census can enroll in immediately. ⭐ The −3 month effective date also drives the renewal prompt: **AMS reads renewal timing off effective date and prompts one month ahead**, so a 10/1 effective date fires the prompt on 9/1, giving the ICHRA notice cycle a mechanism to act on before 1/1. | **Proven 2026-09-12 (F5)** — a CDH plan imported effective `20251001` against plan year 1/1/2026–12/31/2026 was created with the date **stored verbatim, not coerced** to the plan-year start (`ZZSDX27AW1OOPY`, template 1035 `ICHRA Notice` / `CARD_NOTICE`). The out-of-year effective date the generic card plan needs is accepted |
| 2 | Enroll the entire census at **$1 annual**, effective 12/1/2026, on an auto-processing schedule | Initiates card production without needing plan or premium detail. Future effective date means **nothing back-posts** — no spendable money on a placeholder. | **Proven** — future effective date posts $0.00 |
| 3 | As each person's real coverage and premium arrive, enroll them in the correct benefit, effective 12/1/2026 for the §125 benefits, **with no schedule**. ⚠️ **The ICHRA leg enrolls on or after its plan-year start (1/1/2027), never 12/1/2026** — corrected 2026-09-12, see concerns | The election exists as an expectation; nothing posts. Decouples *when we learn* a premium from *when it is deducted*. | **Proven** (§125) — no-schedule election accepted, $0.00 disbursable. **ICHRA at 12/1/2026: proven to FAIL** `Plan Not Found` (2026-09-12); same row at 1/1/2027 succeeded |
| 4 | `125 PI Contributions` posts the correct month's deduction | Puts the right money on the right election at the right time, regardless of when the information arrived. | **Proven** — contributions post to a no-schedule election |
| 5 | Manually **renew** each 2026-plan-year benefit into a 2027 plan year | Moves the benefits into the implementation year. | **Proven** — renewal is additive; prior year's election and balances survive intact as a second Active record |
| 6 | `125 PI Elections` with real per-person schedule assignments | Becomes the ongoing enrollment. | **Proven** — the new plan year unlocks re-enrollment that otherwise fails `Plan Already Enrolled` |

**Benefits created for a 1/1/27 implementation:**

| Benefit | Effective | Plan year |
|---|---|---|
| Generic (card-enabled) | 10/1/26 | 1/1/26 – 12/31/26 |
| 125+Excepted | 1/1/26 | 1/1/26 – 12/31/26 |
| 125+OffExchange | 1/1/26 | 1/1/26 – 12/31/26 |
| PostTaxPlan | 1/1/26 | 1/1/26 – 12/31/26 |
| ICHRA | 1/1/27 | 1/1/27 – 12/31/27 |

⭐ **Built and verified 2026-09-12 (W3 V103 · W4 `5982c26` · W5 `a889263`).** The table above is now
produced by AMS: one `summit_plan_template_map` row per plan, each with `effective_date_rule` /
`offset_months` / `plan_year_offset_years`, evaluated per row by `SummitPlanDateRuleResolver` when
file 2 is generated. Verified output for employer `158E141452`, D = 2027-01-01, run 2026-09-12 — six
rows imported, six plans created, plan type coming from the Summit template not the file:

| Label (row) | Template | Rule / offsets | Effective | Plan year |
|---|---|---|---|---|
| PremiumPath Card Issuer | 1037 (`125+Setup`) | `MOST_RECENT_PAST_MONTHDAY`, −3, −1 | **2025-10-01** | 2026-01-01 – 2026-12-31 |
| PremiumPath Excepted | 1031 | `PLAN_YEAR_START`, 0, −1 | 2026-01-01 | 2026-01-01 – 2026-12-31 |
| PremiumPath Off Exchange | 1032 | `PLAN_YEAR_START`, 0, −1 | 2026-01-01 | 2026-01-01 – 2026-12-31 |
| PremiumPath Post Tax | 1036 | `PLAN_YEAR_START`, 0, −1 | 2026-01-01 | 2026-01-01 – 2026-12-31 |
| PremiumPath ICHRA | 1030 | `PLAN_YEAR_START`, 0, 0 | 2027-01-01 | 2027-01-01 – 2027-12-31 |
| QSEHRA (separate service item) | 1034 | `PLAN_YEAR_START`, 0, 0 | 2027-01-01 | 2027-01-01 – 2027-12-31 |

⚠️ The generic plan's effective date is **run-date dependent**: the table's 10/1/26 is what the
rule yields once the export runs on or after 2026-10-02; run on 2026-09-12 it yielded 2025-10-01
(the most recent 10-01 strictly in the past). Both are valid — Summit stores an effective date
outside the plan year verbatim (F5). Rules in full, and the reasons each plan exists:
`docs/analysis/premiumpath_summit_plan_structure.md`.

### Concerns with the sequence

- ⚠️ **Card production timing was never tested.** Whether Summit initiates a card at record creation or
  at the effective date is unknown, and step 2's entire rationale depends on it. If production keys off
  the effective date, the generic benefit's value collapses to the renewal hook alone.
- ⚠️ **The ICHRA does not fit step 3 as written** — its plan year starts 1/1/27, but step 3 enrolls at
  12/1/2026, before the plan year opens. ~~Untested; `Plan Not Found` is the plausible outcome.~~
  ⭐ **Proven 2026-09-12 (session 51): it fails.** An `HRA Enrollment` row on `ZZSDX27AICHRA` (PY
  2027) effective 20261201 returned `Plan Not Found`; the same row effective 20270101, in the same
  file, returned `Successful`. **The plan year is binding: the ICHRA leg enrolls on or after
  plan-year start.** Step 3 is corrected above. ⚠️ `Plan Not Found` is **also** what a wrong
  `Import Plan ID` returns — the two causes are indistinguishable from the results file; check the
  plan id before blaming the date, and vice versa.
- ⚠️ **Every benefit needs its schedule allowed set populated by hand before any election file touches
  it**, including the generic one.
- ⚠️ **The generic benefit's funding method must be right at creation** — `Participant funding method`
  is not editable once active. For a $1 placeholder that will never see a payroll schedule, **`Annual`
  is likely correct**, not `Contribution Schedule`.
- ⚠️ **Deducting January 2027 premium under a 2026 cafeteria plan year runs near the §125 deferred-
  compensation prohibition.** Advance premium payment is ordinary and this is probably fine, but the
  design carries real weight on it. ~~Cheap to register as an `LA-NN` entry now; expensive to unwind
  after plan documents are drafted around it.~~ ⭐ **Already registered — `LA-36` in
  `legal_assumptions.md`** ("First-month premium funding comes from an employee-sourced on-ramp; the
  month-ahead cadence is expressly permitted"): the last month of a plan year funding the first month
  of the next is the carve-out LA-36 rests on (Federal Register preamble to the 2007 proposed
  cafeteria-plan regulations; IRB 2007-39), and the cross-year purchase prohibition (Prop. Treas. Reg.
  §1.125-1 Q&A-7) is the boundary it records. Status there: *Assumed*, operative regulation section
  unread. This sequence — step 1's 2026 plan year and step 4's December deduction of January premium —
  depends on LA-36 holding; cross-referenced 2026-09-12 (session 51).

---

## 8. Emitter rules — consolidated

1. Import Plan IDs: **strictly alphanumeric**.
2. Tier IDs: **generated, not typed**; avoid `0`/`O` and `1`/`l`.
3. Elections: emit **annual only** (G), never annual + per-contribution.
4. Elections: **omit `Plan Start Date`** (E).
5. Elections: never emit `Employer Contribution Schedule` (J) on an `Ins125+` plan.
6. HRA Enrollment: emit `Tier ID` under Contribution Schedule funding; **omit it under Single Fund**.
   Funding method must come from operator input or per-plan config.
7. Every emitted file: **content variance guaranteed**, not just filename variance.
8. Every emitted file: trailing `Filler` populated, field count structurally guaranteed.
9. Election files are **not idempotent** — operator gate required before emitting.
10. Operator acknowledgement required that the plan's schedule allowed set is configured in Summit,
    worded as *"confirm the schedule is selected on the plan."*
11. Schedule names come from a **config-mapped payroll-frequency registry**, never literals.
12. Effective-date policy is a **product decision, not a default** — future/current prevents
    back-posting but inflates the per-contribution figure; backdated creates immediately spendable money.

---

## 9. Open questions

| # | Question | Settles it |
|---|---|---|
| 1 | Card production trigger — record creation or effective date? **Untestable until SSA issues cards.** Funds are live at record creation (`-15`: $6,000 disbursable on 9/12 against a 10/1/2026 effective date); card issuance is assumed separate — see TA-e in `docs/business/summit_data_exchange.md` and the parked backlog item | Test when issuance is live; highest value, step 2 depends on it |
| 2 | ~~Does an ICHRA enrollment dated before its plan year opens fail?~~ ⭐ **Answered 2026-09-12: yes, `Plan Not Found`** — §7 step 3 corrected | ~~Test~~ Done |
| 3 | Multi-tier Single Fund — how is a tier resolved when the file supplies none? Default flag? **Parked** — needs a second tier on the SF plan (backlog) | Test |
| 4 | `Plan Status` numeric code set on Elections | Test / DataPath |
| 5 | Does a template `Default Value` fire on a blank file field? **Parked** — needs a Default Value set then cleared (backlog) | Test |
| 6 | What `Import for Process Approval` does. ⭐ **Partly answered 2026-09-12:** it is a template-level checkbox, not a column (§3); its effect when checked is still untested | Test / DataPath |
| 7 | What the separate `Enrollment` file type is for | DataPath |
| 8 | `Pro-Rate Employer Amount` = prorate on effective date, mid-year behaviour. **Parked** — needs an SF plan with Pro-Rate=prorate (backlog); the proration formula is unrecorded | Test |
| 9 | ~~`Employer Contribution Amount` on the Contributions file~~ ⭐ **Closed 2026-09-12: no such element exists on the template** (§3) | ~~Test~~ Done |
| 10 | Scope of `Undo Last Change` on an election | Test |
| 11 | ~~Is Single Fund confirmed as the ICHRA standard?~~ ⭐ **Decided 2026-09-12: no.** ICHRA and QSEHRA will be Contribution Schedule funded with monthly stipends in almost all cases. Consequences: AMS **does** emit `Tier ID`; how AMS learns a plan's employer funding method stays a live question (§4). Do not relitigate | **Decided** |
| 12 | Effective-date policy for elections | **Kevin** |
| 13 | ~~§125 plan-year-boundary deduction — register as `LA-NN`?~~ ⭐ **Closed 2026-09-12: `LA-36` already covers it** (§7 concerns, cross-referenced). Whether LA-36's *Assumed* status suffices before plan documents are drafted is the residual question | ~~Kevin or counsel~~ Counsel, on LA-36's own "Confirm before" terms |
