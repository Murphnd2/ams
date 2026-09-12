# Summit import templates — working reference

Six templates, in dependency order. Everything here is either import-proven or read off the Summit
element picker. Anything unverified is marked. As of 2026-09-12.

**Test employer used throughout:** `ZZTEST SDX27 Employer` / Employer TPA Custom ID `ZZSDX27A` /
System ID 1394. Older examples in the source docs use `ZZTEST001` and `158E140952`.

---

## Rules that apply to every file

- **Delimiter `|`. No header row.** Body Record Indicator unchecked, Row Indicator blank.
- **Dates are `YYYYMMDD`, numeric, no separators.**
- **Amounts** two decimals, no currency symbol, no thousands separator. A thousands separator is
  refused rather than stripped — deliberately, since `7,200` and `7.200,00` read differently and
  Summit accepts a wrong amount silently.
- ⚠️ **A trailing empty optional field breaks the parse — on some templates, not all.** Corrected
  2026-09-12 (later the same day): this was written as a platform-wide rule and is **template-specific**.
  `ZZ_TEST_CDH` (§2) ends on eight optional columns and imports with all of them empty;
  `ZZ_TEST_ER` (§1) tolerates a blank trailing column N. Demographics, Elections and HRA Enrollment
  still need their mandatory sentinel (`Branch Code`, `Filler`, `Filler`) — proven on those templates
  and to be kept. What decides it is unknown; until it is, **test each template rather than assume
  either way**, and on the sentinel templates **nothing may be appended after the sentinel** — a new
  optional column goes before it.
- **Filename = template prefix + timestamp**, e.g. `ZZ_TEST_DEMO_20260912142336.txt`. Summit matches
  the file to its template **by filename prefix**.
- ⚠️ **A repeated filename comes back Held, 0 records.** Always a fresh timestamp.
- **The response is `Response_` + your exact source filename.** You can predict it and poll for it —
  no run ID or correlation table. **Check the byte size differs from what you uploaded**; a
  same-size response is the echo symptom seen on 2026-09-11.
- ⚠️ **Results line shape is not constant across templates — three variants observed.** Employer
  Demographic: `Status|Employer Name|Employer TPA Custom ID|Message` (**Status first**, 4 fields).
  Demographics: `Participant TPA Custom ID|Status|Message|Employer TPA Custom ID` (4 fields, status
  second). HRA Enrollment: `Participant TPA Custom ID|Status|Message` (3 fields, status second).
  **A fixed-position parser is unsafe** — `SummitResponseService` classifies on `fields[0]`, which is
  right for Employer Demographic and wrong for the two participant-keyed shapes (code not changed;
  implication recorded). Success token is `Successful`.
- ⚠️ **Empty-cell semantics differ by template.** Employer Demographic **clears** a stored value when
  the cell is empty (§1, F3); Demographics leaves it alone and clears only on a literal `n/a`. Nothing
  in the picker says which applies.
- **Elections and enrollments do not upsert.** Re-running against a consumed participant does not
  overwrite. Use fresh participants.

**Order matters:** employer → plan → participant → enrollment. Files 3 and 4 both fail without 1 and 2.

---

## 1. ZZ_TEST_ER — Employer Demographic

Creates or updates the employer. **Column layout read off the picker 2026-09-12 — fourteen columns,
all Optional.** ⚠️ This section was first written earlier on 2026-09-12 as six columns from prose,
not from the picker; that layout was wrong and is replaced here, not appended to.

| | Column | Type | |
|---|---|---|---|
| A | Employer Name | AlphaNumeric | Optional |
| B | Employer TPA Custom ID | AlphaNumeric | Optional |
| C | Mailing Address | AlphaNumeric | Optional |
| D | Mailing City | AlphaNumeric | Optional |
| E | Mailing State | **Alpha** | Optional |
| F | Mailing Zip | **Numeric** | Optional |
| G | Employer Plan Name | AlphaNumeric | Optional |
| H | Enable CDH Administration | Boolean | Optional |
| I | Enable COBRA Administration | Boolean | Optional |
| J | Enable Retiree Billing Administration | Boolean | Optional |
| K | Enable Direct Bill Administration | Boolean | Optional |
| L | Primary Contact Title | AlphaNumeric | Optional |
| M | Primary Contact Name | AlphaNumeric | Optional |
| N | Primary Contact Email | AlphaNumeric | Optional |

- ⚠️ **There is no mandatory field on this template at all — including the upsert key.** A row with
  column B blank is not rejected by the template; what Summit then does with it is untested.
- L, M and N were **added to the template on 2026-09-12** for the W2 work item. Boolean token `true`
  accepted in column H.
- **A blank trailing column N is tolerated** — this template needs no sentinel.
- Elements available but unmapped: Employer System ID, Primary Contact Phone, Status, Mailing
  Address 2, Corp Address 1/2, Corp City/State/Zip, Website, Shipping Contact / email / phone, Total
  Employee Count, Tax ID, SIC Code. `Primary Contact Phone` was deliberately **not** added — the
  application does not collect it.
- `Employer TPA Custom ID` is the upsert key and AMS owns it — `{SUMMIT_TPA_ID_PREFIX}E{Prospect.id}`.
- `Enable COBRA Administration` is the flag that turns on **Premium Billing**, which is what platform
  ICHRA mailings ride on. Set only when a COBRA ServiceItem is elected (D43/D44/D46) — ICHRA
  employers are CDH-only.
- **Results line:** `Status|Employer Name|Employer TPA Custom ID|Message` — **Status first** (the
  third observed shape; see the rules above).

### ⚠️ F3 — an empty cell CLEARS the stored value (data-loss behaviour)

**Proven 2026-09-12.** Two rows uploaded against the same employer: row 1 with `kevin@example.com` in
N, row 2 identical but with N blank. Result: the employer's Email is **empty**. Column G was blank on
both rows and `Employer Plan Name` — previously `ICHRA allowance: $500.00/mon` — was **also cleared**.

**This is the opposite of Demographics**, where an empty cell leaves the stored value alone and only a
literal `n/a` clears it (recorded against `Employer.customId`, V102). Same platform, same file
family, opposite behaviour, and nothing in the picker reveals which applies.

**Emitter consequence — open design decision, not yet made:** whatever file 1 sends is the whole
truth. Omitting a value deletes it, so **re-sending file 1 against an existing employer silently
wipes any field a human set in Summit that AMS does not populate** — every unmapped element above,
and every mapped one AMS leaves blank. Whether file 1 should ever be re-sent on an existing
employer, or emitted only on create, is **undecided** (backlog T248). The "AMS emits full current
state, re-sending is safe" principle recorded for this file type is true only in the sense that
Summit accepts the re-send; it is not safe for data Summit holds that AMS does not.

## 2. ZZ_TEST_CDH — Employer CDH Plan

Creates the benefit plan for that employer. **Column layout read off the picker 2026-09-12 — sixteen
columns.** ⚠️ This section was first written earlier on 2026-09-12 as eight columns from prose, not
from the picker; an 8-field file is rejected outright — *"The validated file contains 8 columns. The
file template defines 16 columns."* That layout was wrong and is replaced here, not appended to.

| | Column | Type | |
|---|---|---|---|
| A | Plan Template ID | Numeric | Mandatory |
| B | Plan Name | AlphaNumeric | Mandatory |
| C | Import Plan ID | AlphaNumeric | Mandatory |
| D | Plan Description | AlphaNumeric | Mandatory |
| E | Effective Date | **AlphaNumeric** | Mandatory |
| F | Employer TPA Custom ID | AlphaNumeric | Mandatory |
| G | Plan Year Begin | AlphaNumeric | Optional |
| H | Plan Year End | AlphaNumeric | Optional |
| I | Grace Period Enabled | Boolean | Optional |
| J | Grace Period Calculation (by date) | Boolean | Optional |
| K | Grace Period Date | AlphaNumeric | Optional |
| L | Run-out Enabled | Boolean | Optional |
| M | Run-out Calculation (by date) | Boolean | Optional |
| N | Run-out # Days | Numeric | Optional |
| O | Terminated Run-out Type | Numeric | Optional |
| P | Terminated Run-out # Days | Numeric | Optional |

Proven row (2026-09-12):
```
1035|W1 Out Of Year|ZZSDX27AW1OOPY|W1 Out Of Year|20251001|ZZSDX27A|20260101|20261231||||||||
```

- ⚠️ **`Effective Date` is AlphaNumeric here but Numeric on HRA Enrollment.**
- **No trailing sentinel; the template ends on an optional column, and a row with I–P empty imports
  fine.** The trailing-empty-optional rule is template-specific (see the rules above).
- ⚠️ **The shipped emitter's column count is configuration-dependent and unverified against any
  production template.** `buildCdhPlanRow` emits the eight mandatory-block columns A–H **plus one
  column per element listed in `SUMMIT_CDH_OPTIONAL_ELEMENTS`**, in configured order (tokens
  `GRACE_ENABLED, GRACE_BY_DATE, GRACE_DATE, GRACE_DAYS, RUNOUT_ENABLED, RUNOUT_BY_DATE, RUNOUT_DAYS,
  TERM_RUNOUT_TYPE, TERM_RUNOUT_DAYS`). With nothing configured it emits **exactly 8** and would be
  rejected by this 16-column template; with the eight I–P elements configured in this order it emits
  16. Summit validates column count whole-file, so **the configured element list must match the
  target template's column count exactly**, and nobody has verified what production's template
  defines (backlog T247). Do not change code on this note.
- ⭐ **An effective date outside the plan year is accepted and stored verbatim** (F5). The proven row
  above — effective `20251001` against plan year 2026 — was created with the date **intact, not
  coerced** to the plan-year start, confirmed on the Edit Benefit Plan screen. This is what makes the
  PremiumPath generic-card-plan date rule (implementation-minus-three-months, prior plan year;
  `summit_import_spec.md` §7 step 1) viable.
- **The plan template, not the row, sets the plan's nature.** From the same screen: template 1035 is
  `ICHRA Notice`, plan type `CARD_NOTICE`, Plan Structure `Annual Renewal`; **Funding tax treatment,
  Funding source and Participant funding method come from the template.** The five PremiumPath plans
  therefore differ **by Plan Template ID**, not by anything the CDH row carries. `+ Assign Plan Year`
  on that screen is the manual renewal step.

⚠️ **`Import Plan ID` must be alphanumeric only — no special characters.** Known Summit behaviour on
**some** import types but not all, so a key that works on one template can fail on another. No other
field carries this restriction. See §5.

⚠️ **`Import Plan ID` carries no plan year, and this is the single most load-bearing rule in the set.**
It's the upsert key. A Summit plan **persists across plan years and accumulates them** — a renewal
attaches another plan year to the same plan. Put a year in the key and year two creates a *second*
plan instead, elections split across both, indistinguishable in the UI. Shipped form is
`sanitize(employerTpaCustomId + keySegment)` — straight concatenation, **no separator**; the year
travels in `Plan Year Begin`/`Plan Year End`. Source docs write this as
`{employerTpaCustomId}-{keySegment}`, where the hyphen is prose notation, not a character in the key.

⚠️ **Two code sites compose this key** — file 2's `buildCdhPlanRow` and the enrollment writer's
`importPlanId`. If they ever disagree, every enrollment row points at a plan that doesn't exist.

⚠️ **This template's results correlate on `Plan Name`** — no row number. **Two rows in one file must
not share a label.** Nothing enforces it.

`Plan Name` and `Plan Description` both emit the mapping row's `label` verbatim; a PSP admin can
rename a plan by editing the Label field, no code or config change.

## 3. ZZ_TEST_DEMO — Demographics

Creates the participant. **Import-proven 2026-09-08 and again 2026-09-12** (five of five created).

| | Column | |
|---|---|---|
| A | Employer TPA Custom ID | mandatory |
| B | Participant TPA Custom ID | mandatory |
| C | First Name | mandatory |
| D | Last Name | mandatory |
| E | Mailing Address Line 1 | mandatory, **50 char max** |
| F | Mailing Address City | mandatory |
| G | Mailing Address State | mandatory |
| H | Mailing Address Zip Code | mandatory |
| I | Effective Date | mandatory |
| J | E-mail Address | optional |
| K | Mailing Address Line 2 | optional |
| L | Branch Code | mandatory sentinel — `AMS` |

```
ZZSDX27A|158-P-S27-15|R15|Testcase|100 Main Street|Marinette|WI|54143|20260101|||AMS
```

- Both optional columns empty still parses — that's what `Branch Code` exists for. Proven twice.
- `Mailing Address Line 1` at 55 chars is rejected per-row; 48 is accepted. **No other field's length
  has been tested** — assume any of them may have a limit.
- **Neither SSN nor DOB is required.** The no-SSN boundary holds from proposal through enrollment.
- Optional elements land at J and K regardless of where they belong logically; the picker appends
  optionals after the mandatory block.

## 4. ZZ_TEST_125_ELECTIONS — 125 PI Elections

The §125 leg. Proven 2026-09-11/12.

| | Column | | AMS emits |
|---|---|---|---|
| A | Employer TPA Custom ID | mandatory | always |
| B | Participant TPA Custom ID | mandatory | always |
| C | Import Plan ID | mandatory | always |
| D | Effective Date | optional | always |
| E | Plan Start Date | optional | **never** — cross-check only |
| F | Coverage End Date | optional | termination only |
| G | Annual Election Amount | optional | always |
| H | Per Contribution Amount | optional | **never** |
| I | Participant Contribution Schedule | optional | when funded by schedule |
| J | Employer Contribution Schedule | optional | never on Ins125+ |
| K | Filler | mandatory sentinel | constant `X` |

- **Required set is A, B, C only.** Effective Date is optional *on this template*.
- ⚠️ **If both G and H are supplied, H is ignored.** Emit G, never H.
- **Column D drives the money.** A backdated effective date posts every elapsed contribution run date
  immediately ($853.88 observed). Current or future posts $0.00. Manual Processing does **not**
  suppress the catch-up.
- **E is a cross-check, not a selector.** A wrong Plan Start Date returns `Plan Not Found`.
- **I must be one of the plan's allowed schedules** — proven three times.

## 5. ZZ_TEST_125_CONTRIB — 125 PI Contributions

Posts a contribution against an existing election. **Column order read off the picker 2026-09-12.**

| | Column | | Type |
|---|---|---|---|
| A | Employer TPA Custom ID | Mandatory | AlphaNumeric |
| B | Participant TPA Custom ID | Mandatory | AlphaNumeric |
| C | Import Plan ID | Mandatory | AlphaNumeric |
| D | Participant Contribution Amount | Mandatory | Numeric |
| E | Plan Effective Date | Optional | Numeric |
| F | Update Participant Annual Election | Optional | **Boolean** |
| G | Filler | Mandatory sentinel | AlphaNumeric |

⚠️ **Column F is the one to understand, and it is ADDITIVE, not a replacement.** With the flag set,
the contribution amount is **added to** the existing annual election. Proven 2026-09: participant
`158-P-S27-09` on `PremiumPath Excepted` held an annual election of $1,200.00 with $0.00 posted; a
Contributions row of $75.00 with the flag set produced an annual election of **$1,275.00**, with
Contributions To Date and Available/Disbursable Balance all $75.00. Not $75 replacing $1,200 — $1,200
plus $75.

- **Without the flag, the annual election is left alone.** `-01` and `-02` both kept $1,200.00.
- **`Y` is the confirmed token.** `true` was sent in an earlier run but that file failed for an
  unrelated reason, so `true` remains untested despite the Boolean data type.
- F's Default Value checkbox is present and **unchecked**, so a blank F takes no default.
- **There is no Employer Contribution Amount element on this template** — that open question is
  answered. Only a participant amount exists.
- **`Import for Process Approval` is a template-level checkbox, not a column** — also answered. It is
  currently **unchecked** on this template.
- No employer-side amount and no tier: this template is participant-money only.

⚠️ **The flag's column position has moved.** The session that ran the additive test recorded the flag
at **E**. The picker read on 2026-09-12 puts it at **F**, with `Plan Effective Date` now at E —
consistent with `Plan Effective Date` having been added to the template after that test. **Build
against F**, and re-check the picker before any emitter run, because a file built to the old position
writes `Y` into a Numeric date field. The additive behaviour itself is unaffected by the move.

⚠️ **Import Plan ID must be ALPHANUMERIC — no special characters, ever.** This is a known Summit
behaviour affecting **some import types but not all**, which makes it worse than a uniform rule: a
key with a hyphen can import cleanly on one template and fail on another, so a single successful
test proves nothing about the others. No other field carries this restriction.

**The code is already correct.** `SummitExportServlet` composes the key as
`sanitize(employerTpaCustomId + keySegment)` — straight concatenation, no separator — and
`keySegment` is regex-validated `[A-Za-z0-9]+` at both admin sites. That's why `ZZSDX27A` + `ICHRA`
yields `ZZSDX27AICHRA` and imports clean.

⚠️ **The docs are what's wrong.** The composition is written throughout the source docs as
`{employerTpaCustomId}-{keySegment}`. That hyphen is prose notation separating two placeholders, but
it reads as a literal, and anyone implementing from the written form rather than the code will
produce a key that fails on some templates and not others. **Correct the prose wherever it appears.**

**Relevance to the $1 card-seed flow.** Additive behaviour is what lets a placeholder election grow
into the real figure as premium arrives, with no correction pass. ⚠️ But the $1 seed stays in the
total, so the final annual election reads $1 high unless it is backed out.

## 6. ZZ_TEST_HRA_ENROLL — HRA Enrollment

The HRA/ICHRA leg. **Column order read off the picker 2026-09-12.**

| | Column | |
|---|---|---|
| A | Employer TPA Custom ID | Mandatory |
| B | Participant TPA Custom ID | Mandatory |
| C | Import Plan ID | Mandatory |
| D | Effective Date | Mandatory — **numeric** |
| E | Tier ID | Optional |
| F | **Employer** Contribution Schedule | Optional |
| G | **Participant** Contribution Schedule | Optional |
| H | Filler | Mandatory sentinel — `X` |

```
ZZSDX27A|158-P-S27-19|ZZSDX27AICHRA|20270101|EE0|||X
```

⚠️ **F and G are Employer-then-Participant here — the reverse of Elections' I/J.** Easy to transpose.

⚠️ **Effective Date is Mandatory on this template** though optional on Elections.

**Tier ID (E) is conditional on the plan's funding method:**

| Plan funding | Tier ID | Failure if wrong |
|---|---|---|
| Contribution Schedule | required | `No tier identified` |
| Single Fund | **omit** | `Not a tier funded plan` |

- ⚠️ **The file cannot tell you which applies.** Same template, opposite rules, and AMS has no column
  or config that records a plan's funding method. This is an open decision, not a gap in this doc.
- **Proven 2026-09-12:** Single Fund with E omitted → `Successful`. Contribution Schedule with
  `EE0` in E and **both** F and G blank → `Successful`. So the schedule columns aren't required —
  though `S27 Default` is flagged Default Schedule on this employer, so what's proven is "not
  required where a default exists."
- ⚠️ **Enrollment dated before the plan year opens fails with `Plan Not Found`.** 12/1/2026 against a
  2027 plan year failed; 1/1/2027 succeeded, same file. **A wrong Import Plan ID returns the identical
  message** — you cannot distinguish the two from the results file.
- **Funding comes from the plan tier, not from the file.** `-15` on Single Fund tier `EEONLY` shows
  Annual Election `$0.00` and Employer Funding `$6,000.00` with nothing in the file supplying an
  amount.
- ⚠️ **Money is live at record creation, not at the effective date.** `-15` showed $6,000 available
  and disbursable on 9/12 against a 10/1/2026 effective date. Card issuance is assumed to behave
  differently — that's the open TA.
- **This layout is not what AMS currently emits.** `writeHraEnrollment` emits a **5-column** layout
  ending in `Participant Annual Election Amount`, no Tier ID, no Filler — built against a different
  template (the 1030 chain, 2026-09-08). Per the finding above that column is inert on a tier-funded
  plan. **The shipped emitter targets a layout this reference does not describe.**

---

## Template-level settings — check these before blaming a row

Every template carries settings above the column list. They are per-template, they are invisible in
the file, and a wrong one produces a failure that looks like a data problem.

Observed on `ZZ_TEST_125_CONTRIB`, 2026-09-12, and worth confirming on each of the other five:

| Setting | Value | Why it matters |
|---|---|---|
| File Format | Delimited | — |
| Delimiter | `\|` | — |
| Date Format | `YYYYMMDD` | Set per template. A template set to a different format silently reads your dates wrong. |
| Extraneous Data | **No** | Extra columns are not tolerated. |
| Import for Process Approval | unchecked | When checked, imports land pending approval rather than posting. |
| Include Header / Include Footer | both unchecked | — |
| **Produce Results File** | **checked** | ⚠️ If this is off, **no results file is generated at all**. |
| Errors Only | unchecked | When on, successful rows are omitted from the results file. |

⚠️ **`Produce Results File` is the first thing to check against the 2026-09-11 echo.** The
`ZZ_TEST_HRA_ENROLL_singlefund2` upload that appeared to return the input file rather than a results
file is exactly what an unset results file would look like from the outside. Test 1 produced a proper
results file on 2026-09-12, so the setting is on now — but if the symptom returns, look here before
looking at the row.

## What is still not known

- **The Boolean token for Contributions F** — `Y` confirmed; `true` untested.
- **Whether the flag sits at E or F** — recorded as a template change: E at test time, F on the
  2026-09-12 picker. Re-check before emitting.
- **Template-level settings on the other five templates** — only `ZZ_TEST_125_CONTRIB` has been read.
- **Field length limits** on everything except `Mailing Address Line 1`.
- **`Plan Status` code set** on Elections — importable, but no candidate codes exist anywhere.
- **Whether F/G schedules are required** where no default schedule exists on the employer.
- **Multi-tier Single Fund** resolution with Tier ID omitted — untested (was Test 4).
- **Pro-Rate** behaviour on an effective date — untested (Test 5), and the proration formula is
  unrecorded.
- **Template `Default Value`** firing on a blank field — untested (Test 6).
- **Card issuance timing** — assumed next-day-prospective, backlog item, cannot test until issuance.
- **Whether Card Enabled, Pro-Rate, or tiers are editable on an active plan.**
- **Production `Employer CDH Plan` template column count** vs what `buildCdhPlanRow` emits under
  production's `SUMMIT_CDH_OPTIONAL_ELEMENTS` — unverified (T247).
- **File 1 re-send semantics** — whether Employer Demographic is ever re-sent on an existing employer,
  given that an empty cell clears the stored value (T248).
- **Which templates tolerate a trailing empty optional** and what decides it — `ZZ_TEST_CDH` and
  `ZZ_TEST_ER` do; Demographics, Elections and HRA Enrollment need a sentinel.
- **What Employer Demographic does with a blank upsert key** (column B), now that no column on it is
  mandatory.
