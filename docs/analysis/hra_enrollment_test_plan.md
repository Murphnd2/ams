# HRA Enrollment — remaining hand-upload test plan

**Status:** checklist, 2026-09-12. Kevin runs these by hand in Summit; nothing here is code, SQL or
a migration. Each test is one upload plus one look in the UI. Written from
`summit_import_spec.md` (§4, §7, §9), `summit_import_contracts.md` and
`docs/business/summit_data_exchange.md`.

⚠️ **The live objects below come from Kevin's environment, not the repo — unverified here.**
Employer `ZZTEST SDX27 Employer` / `ZZSDX27A` / System ID 1394. Plans `ZZSDX27AINS125A`
(Ins125+, Participant, Contribution Schedule, PY 2026+2027), `ZZSDX27AICHRA` (Employer,
Contribution Schedule, PY 2027, tiers `EE0` 6000 / `FAM` 12000), `ZZSDX27AICHRASF` (Employer,
Single Fund, Pro-Rate None, PY 2026, tier `EEONLY` 6000). Schedules `S27 EE Only` (Ptcp,
bi-weekly), `S27 ER Only` (Empr), `S27 Default` (both, default), `S27 EE Manual` (Ptcp, Manual
Approval). Templates `ZZ_TEST_125_ELECTIONS` (A–K), `ZZ_TEST_125_CONTRIB` (A–G),
`ZZ_TEST_HRA_ENROLL` (A–H), `ZZ_TEST_DEMO`.

## Layout check before the first upload

Spec §4 maps **eight** columns on `HRA Enrollment`:

| A | B | C | D | E | F | G | H |
|---|---|---|---|---|---|---|---|
| Employer TPA Custom ID | Participant TPA Custom ID | Import Plan ID | Effective Date | Tier ID | Employer Contribution Schedule | Participant Contribution Schedule | Filler (`X`, set Mandatory) |

`ZZ_TEST_HRA_ENROLL` is described as A–H — **eight columns, so the count matches.** The repo does
not record the template's element *order*; eyeball the picker against the table above before
upload, because column count is validated whole-file and one wrong row rejects everything.

⚠️ A **different, five-column** HRA Enrollment layout is recorded in `summit_data_exchange.md` §4
(`Employer TPA Custom ID|Participant TPA Custom ID|Import Plan ID|Effective Date|Participant Annual
Election Amount`) and is what `writeHraEnrollment` emits. That is a different template mapping
(the 2026-09-08 ICHRA+ chain, template 1030), not this one. **Do not upload a five-column file to
`ZZ_TEST_HRA_ENROLL`.** The two docs also disagree on what the amount column does on an HRA plan —
see the terminal report; not resolved here.

## Rules that apply to every row

- Pipe-delimited, dates `YYYYMMDD`, no header/footer, **exactly 8 fields = 7 pipes**, `X` last.
- **Fresh participant per row** — HRA Enrollment does not upsert (the contracts record
  `Plan Already Enrolled` on a second election for the same participant and plan). Rows below use
  `158-P-S27-15` onward as placeholders. *Setup note: create these via Demographics / the admin UI
  as each test needs them.* `-08` and `-09` reportedly sit at future effective dates with $0
  balances — **re-check before reusing**, and only where the test's plan is one they are not
  already enrolled in.
- **Content variance is automatic** when the participant is fresh; a byte-identical re-send is held
  on content hash and produces nothing.
- Results line: `Participant TPA Custom ID|Status|Message`; success is `Successful|Success`.
  **If what comes back looks like the input file echoed, it is not a results file** — check File
  History for Held / duplicate / no-template-match before drawing any conclusion.
- Recorded failure strings and what each means: `Plan Not Found` (plan/date mismatch — plan year
  is the binding constraint); `No tier identified` (no Tier ID supplied and nothing to fall back
  on); `No matching Tier` (Tier ID supplied, exact-match failed — check `0`/`O`, `1`/`l`);
  `Not a tier funded plan` (Tier ID supplied on a Single Fund plan). The employer-side schedule
  failure string is **NOT FOUND** in the repo (only the participant-side
  `Participant Contribution Schedule 'X' not found` is recorded).

---

## Test 1 — Single Fund enrollment **by import** (Tier ID omitted)

**Proves:** that spec §4's Single Fund row ("`Tier ID` must be omitted — the tier is derived from
plan configuration") holds for a *file*, not just the manual screen. Changes spec §4 E-row and
rule 6 of §8 if it fails.
**What the spec claims / evidence:** contracts §HRA Enrollment records `Not a tier funded plan`
when a Tier ID *was* supplied on SF, and the $6,000 Employer Funding observation — but does not
record how that enrollment was created. Kevin's account: manual entry only. The
`…singlefund2` attempt returned an echo, not a results file → **unproven by import.**

Template `ZZ_TEST_HRA_ENROLL`, file `ZZ_TEST_HRA_ENROLL_sf3.txt`:
```
ZZSDX27A|158-P-S27-15|ZZSDX27AICHRASF|20260101||||X
```
Participant: fresh (`-15`).

| Result | Establishes |
|---|---|
| `Successful|Success` | SF import proven. Then open the participant's plan record: expect **Employer Funding $6,000, $6,000 disbursable**, tier `EEONLY`, history Method `Plan Import`. Spec §4 stands as written. |
| `No tier identified` | SF still wants a tier when the file omits it → the manual screen's auto-resolution does **not** apply to import; spec §4 E-row and rule 6 must be rewritten (Tier ID emitted under both methods). |
| `Not a tier funded plan` | Should be impossible with E blank; if seen, Summit treats an empty field as supplied — record it, then re-check the template's `Default Value` on Tier ID (see Test 6). |
| `Plan Not Found` | Date/plan-year mismatch — confirm the SF plan's PY is 2026 and its effective date ≤ 20260101. |
| Echo again / nothing | Not a results file. File History: Held (content duplicate of `singlefund2`?), or no template match on filename. |

---

## Test 2 — Card production trigger: record creation or effective date? (§9 #1)

**Proves:** whether Summit initiates a card when the enrollment record is created or when its
effective date arrives. §7 step 2's entire rationale (early $1 enrollment "initiates card
production") and step 1's "generic card-enabled benefit" depend on it; if production keys off the
effective date, the generic benefit's value collapses to the renewal hook alone (§7 concerns).
**What the spec claims / evidence:** nothing — "never tested", spec §7 and contracts both. Start
this one early: it needs a calendar wait.

*Setup note:* a **card-enabled** plan. Which of the three test plans has Card Enabled / `Enable
debit card` on is **NOT FOUND** in the repo (the only record is that it was *off* on template
`ZZ_TEST_ICHRA` 1029). Confirm on the plan first; whether it can be switched on an active plan is
not recorded either. Pick an effective date a few days out but inside the plan year, so the wait
is short. Row below assumes the SF plan (PY 2026) is the card-enabled one:

Template `ZZ_TEST_HRA_ENROLL`, file `ZZ_TEST_HRA_ENROLL_card1.txt`:
```
ZZSDX27A|158-P-S27-16|ZZSDX27AICHRASF|20260916||||X
```
Participant: fresh (`-16`), or `-08`/`-09` **only after re-check** that they are not already on
this plan and that their plan is the card-enabled one.

**The results file does not answer this test.** `Successful|Success` only means the record
exists. Then observe, twice:
1. **Same day, before the effective date** — the participant's card area in Summit (the repo
   records no menu path; **NOT FOUND**). A card record / card request present now ⇒ **record
   creation** triggers production ⇒ §7 steps 1–2 stand.
2. **On or after the effective date** — if the card appears only now ⇒ **effective date** triggers
   production ⇒ §7 step 2 must move its effective date to the card lead-time, and step 1's
   generic benefit is justified by the renewal prompt alone. Rewrite §7 concerns accordingly.
3. Neither ⇒ card production is not driven by enrollment at all on this tenant (or needs a
   card-vendor run); record it as a DataPath question, not a design fact.

Side observation to record: under SF / Pro-Rate None with a *future* effective date, is Employer
Funding already $6,000 disbursable? Relevant to §7's "funds should not be available before the
implementation date" for ICHRA.

---

## Test 3 — ICHRA enrollment dated **before** its plan year opens (§9 #2)

**Proves:** whether §7 step 3 works for the ICHRA leg as written (enroll 12/1/2026 into a plan
whose year opens 1/1/2027). Changes §7 step 3 and the benefits table if it fails.
**What the spec claims / evidence:** "Untested; `Plan Not Found` is the plausible outcome" (§7
concerns). Contracts record a CS/2027-PY enrollment showing $0.00 but not the row shape used.

*Setup note:* confirm `S27 ER Only` (or `S27 Default`) **is selected on `ZZSDX27AICHRA`'s
employer-side allowed set** — "confirm the schedule is selected on the plan", not that it exists.
Whether column F is *required* under Contribution Schedule funding is not recorded; it is
included so the row matches a plausible ongoing-enrollment shape. Tier `EE0` is written as given
(E-E-**zero**) — verify against the tier grid; a `0`/`O` transposition already produced
`No matching Tier` once.

Template `ZZ_TEST_HRA_ENROLL`, file `ZZ_TEST_HRA_ENROLL_prepy1.txt` — two rows, the second is
the control:
```
ZZSDX27A|158-P-S27-17|ZZSDX27AICHRA|20261201|EE0|S27 ER Only||X
ZZSDX27A|158-P-S27-18|ZZSDX27AICHRA|20270101|EE0|S27 ER Only||X
```
Participants: fresh (`-17`, `-18`).

| Row 1 (12/1/26) | Row 2 (1/1/27) | Establishes |
|---|---|---|
| `Successful|Success` | `Successful|Success` | Step 3 works for ICHRA as written. Open row 1's record: was the effective date **stored as 20261201 or floored to 20270101**? Record which. |
| `Plan Not Found` | `Successful|Success` | **The plan year is binding.** §7 step 3 needs an ICHRA-specific date (1/1/2027) — the "ICHRA does not fit step 3" concern becomes a rule. |
| `Plan Not Found` | `Plan Not Found` | Not the date — plan id, PY or effective-date floor on the plan itself. Fix and re-run with two fresh participants. |
| `No tier identified` / `No matching Tier` | same | Tier binding failed independent of date — fix tier first; the date question is unanswered. |
| schedule-not-found (string not recorded) | same | `S27 ER Only` is not in the plan's allowed set — a setup step, not the date. |

---

## Test 4 — Multi-tier Single Fund, file supplies no Tier ID (§9 #3)

**Proves:** how a Single Fund plan with more than one tier picks a tier when the row is blank —
Default flag, first tier, or refusal. Changes spec §4 E-row (SF "must be omitted" may need
"…and a Default tier set") and §6's tier-grid row. Run after Test 1 so the single-tier SF
baseline is known.
**What the spec claims / evidence:** open question only; "`Tier ID` and `Default` columns only
render once a second tier exists" (§6).

*Setup note:* add a second tier to `ZZSDX27AICHRASF` (e.g. `FAMONLY` 12000) with **no** Default
set. Whether a tier can be added to an active plan is not recorded; if not, a second SF plan with
two tiers (any alphanumeric Import Plan ID) and substitute it in column C.

**4a** — template `ZZ_TEST_HRA_ENROLL`, file `ZZ_TEST_HRA_ENROLL_sfmt1.txt`:
```
ZZSDX27A|158-P-S27-19|ZZSDX27AICHRASF|20260101||||X
```
| Result | Establishes |
|---|---|
| `Successful|Success` | A tier resolved without a default. Open the record: Employer Funding **6000 ⇒ first/lowest tier**, **12000 ⇒ last/highest** — record the rule. |
| `No tier identified` | Multi-tier SF needs a Default tier. Go to 4b. |
| `Not a tier funded plan` | Contradicts Test 1 — the second tier changed the plan's funding behaviour; record and stop. |

**4b** (only if 4a refused) — set Default on `EEONLY`, then file `ZZ_TEST_HRA_ENROLL_sfmt2.txt`:
```
ZZSDX27A|158-P-S27-20|ZZSDX27AICHRASF|20260101||||X
```
`Successful|Success` with Employer Funding 6000 ⇒ **the Default flag is the resolver**; AMS's SF
rule becomes "omit Tier ID *and* require a Default tier on any multi-tier SF plan". Anything else
⇒ the resolver is unknown; DataPath question.

---

## Test 5 — `Pro-Rate Employer Amount` = prorate on effective date (§9 #8)

**Proves:** what a mid-year SF enrollment funds when proration is on. Changes spec §6's Pro-Rate
row ("prorate on effective date (untested)") and, if the figure is predictable, gives
reconciliation a formula.
**What the spec claims / evidence:** `None` observed (full $6,000 regardless of entry date);
prorate untested.

*Setup note:* an SF plan with Pro-Rate set to prorate. Whether `ZZSDX27AICHRASF`'s Pro-Rate is
editable on an active plan is not recorded; if not, a new SF plan (placeholder id
`ZZSDX27AICHRASFP` below, single tier 6000, PY 2026).

Template `ZZ_TEST_HRA_ENROLL`, file `ZZ_TEST_HRA_ENROLL_prorate1.txt`:
```
ZZSDX27A|158-P-S27-21|ZZSDX27AICHRASFP|20260701||||X
```
| Result | Establishes |
|---|---|
| `Successful|Success`, Employer Funding **< 6000** | Proration applies by import. Record the figure and derive the basis (6/12 ⇒ 3000 monthly; 184/365 ⇒ ~3024.66 daily). No formula is recorded anywhere — do not assume one. |
| `Successful|Success`, Employer Funding **= 6000** | Pro-Rate does not act on an imported effective date (or acts on something else). §6 row rewritten; §4 concern that "the allowance is readable" still holds. |
| `Plan Not Found` | Effective date outside the plan year / before the plan's own effective date. |

---

## Test 6 — Does a template `Default Value` fire on a blank file field? (§9 #5)

**Proves:** whether spec §1's "leave every Default Value empty" is a necessary rule or a
precaution. Changes §1 and the "untested" flag at §1 line "Whether a template default fires…".
Run last — it edits the template, and it needs Test 3's tier-binding shape settled.
**What the spec claims / evidence:** untested; contracts say "assume it does".

*Setup note:* on `ZZ_TEST_HRA_ENROLL`, set **Default Value = `FAM`** on the `Tier ID` element only.
**Clear it again immediately after this test**, whatever the result.

Template `ZZ_TEST_HRA_ENROLL`, file `ZZ_TEST_HRA_ENROLL_default1.txt` (E deliberately blank, on
the CS plan where a blank tier is otherwise a known failure):
```
ZZSDX27A|158-P-S27-22|ZZSDX27AICHRA|20270101||S27 ER Only||X
```
| Result | Establishes |
|---|---|
| `Successful|Success`, record shows tier `FAM` | **Defaults fire on blank fields.** §1 rule is load-bearing: a stray default silently rewrites blank columns. Keep "every Default Value empty" as a hard rule and add "audit template defaults before first production upload". |
| `No tier identified` | **Defaults do not fire on blank fields** (same string Test 3 would give with no default). §1 can downgrade the rule to a precaution. |
| `No matching Tier` | The default fired but `FAM` did not match — check the tier grid; the fire/no-fire question is still answered *yes*. |

---

## What this does not cover

Decision from Kevin, not a test (§9):
- **#11** — Is Single Fund the ICHRA standard? (Tests 1, 4, 5 inform it; none decides it.)
- **#12** — Effective-date policy for elections (future/current vs backdated).
- **#13** — §125 plan-year-boundary deduction as an `LA-NN` entry. Note **LA-36** in
  `legal_assumptions.md` already covers the month-ahead cadence (last month of a plan year funding
  the first month of the next); the spec does not cite it — Kevin/counsel decide whether it suffices.

Test-settled but **outside HRA Enrollment**, so not in this plan:
- **#4** `Plan Status` numeric code set — an Elections column; no candidate codes are recorded
  anywhere, so a test would be guessing. DataPath first.
- **#6** `Import for Process Approval` — a `125 PI Contributions` checkbox.
- **#9** `Employer Contribution Amount` on the Contributions file — the employer-money path.
- **#10** Scope of `Undo Last Change` — an Elections UI action.

Vendor question, not a test: **#7** — what the separate `Enrollment` file type is for.
