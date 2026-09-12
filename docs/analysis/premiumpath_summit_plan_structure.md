# PremiumPath — the five-plan Summit structure

**Status:** built and verified against live Summit 2026-09-12 (W3 V103 · W4 `5982c26` · W5 `a889263`).
One elected PremiumPath service item produces **five** Summit CDH plans from five
`summit_plan_template_map` rows; a QSEHRA sale adds a sixth from its own service item. Every other
product still maps one row → one plan. Onboarding rationale: `summit_import_spec.md` §7; per-column
mechanics: `summit_import_templates_reference.md` §2; legal basis for the prior-year legs: LA-36.

---

## The five plans and why each exists

| # | Row label | Summit template | Plan type (from the template) | Rule | `offset_months` | `plan_year_offset_years` | Why it exists |
|---|---|---|---|---|---|---|---|
| 1 | PremiumPath Card Issuer | 1037 `125+Setup` | Ins125+ (Post-Tax, Participant, Annual Election) | `MOST_RECENT_PAST_MONTHDAY` | −3 | −1 | **Cards precede information.** A card-enabled benefit the whole census can enroll in at $1 before anyone knows their plan or premium, so card production starts early; its implementation-minus-three-months effective date also drives the AMS renewal prompt one month ahead (§7 step 1). |
| 2 | PremiumPath Excepted | 1031 `Ins125+ Excepted Benefit` | Ins125+ | `PLAN_YEAR_START` | 0 | −1 | The Presidio excepted-benefit premium leg, funded by participant salary reduction. |
| 3 | PremiumPath Off Exchange | 1032 | Ins125+ | `PLAN_YEAR_START` | 0 | −1 | The off-exchange individual-premium leg (LA-19: pre-tax treatment needs ICHRA coverage and off-exchange purchase). |
| 4 | PremiumPath Post Tax | 1036 | Ins125+ (post-tax) | `PLAN_YEAR_START` | 0 | −1 | The post-tax residual — anything that cannot be pre-tax (LA-23, LA-38's post-tax default). |
| 5 | PremiumPath ICHRA | 1030 `ICHRA+` | ICHRA (employer-funded) | `PLAN_YEAR_START` | 0 | 0 | The employer allowance. **Implementation-year plan year only** — employer money, no salary-reduction timing to solve, and funds must not be available before the implementation date. |
| — | QSEHRA | 1034 | HRA | `PLAN_YEAR_START` | 0 | 0 | Separate service item; listed because it is in the verified six-row output. |

**Plan type, funding source, funding method and tax treatment come from the Summit plan template,
never from the CDH row** (proven on the Edit Benefit Plan screen, 2026-09-12). The five rows differ
by `Plan Template ID`; the CDH row carries only identity, dates and the optional grace/run-out block.

---

## Verified output — employer `158E141452`, D = 2027-01-01, run 2026-09-12

| Label | Template | Effective (E) | Plan year (G – H) |
|---|---|---|---|
| PremiumPath Card Issuer | 1037 | **2025-10-01** | 2026-01-01 – 2026-12-31 |
| PremiumPath Excepted | 1031 | 2026-01-01 | 2026-01-01 – 2026-12-31 |
| PremiumPath Off Exchange | 1032 | 2026-01-01 | 2026-01-01 – 2026-12-31 |
| PremiumPath Post Tax | 1036 | 2026-01-01 | 2026-01-01 – 2026-12-31 |
| PremiumPath ICHRA | 1030 | 2027-01-01 | 2027-01-01 – 2027-12-31 |
| QSEHRA | 1034 | 2027-01-01 | 2027-01-01 – 2027-12-31 |

All six imported, all six created in Summit on these dates. The single-row export for a non-PremiumPath
product was byte-identical to its pre-W4 output.

---

## The date rules, as built (`SummitPlanDateRuleResolver`)

Inputs per row: **D** = the sale's `plan_year_start` answer, the `plan_year_end` answer, and the row's
three V103 columns. `today` is taken **once per export**, so every row in one file sees the same date.

- **Plan year begin (G)** = D + `plan_year_offset_years`.
- **Plan year end (H)** = the **`plan_year_end` answer** + the same offset. ⚠️ Shifted, not recomputed
  as begin + 1 year − 1 day: for a calendar year they agree (LA-30), but a short first plan year
  (D 2026-07-01, answer end 2026-12-31) must stay 2026-12-31 — a recompute would produce 2027-06-30
  and silently extend the plan year by six months. (F8: H has always come from the answer.)
- **Effective date (E)** by `effective_date_rule`:
  - `PLAN_YEAR_START` → this row's own G. **`offset_months` is not applied.**
  - `MOST_RECENT_PAST_MONTHDAY` → take the month and day of (D + `offset_months`), then the most
    recent occurrence of that month/day **strictly before** `today`. D 2027-01-01, −3 → 10-01:
    today 2026-09-21 → **2025-10-01**; today 2026-10-01 → still 2025-10-01; today 2026-10-03 →
    2026-10-01.
  - Feb 29 clamps to Feb 28 in a non-leap year — never fails, never skips a year.
- ⭐ **An effective date outside its own plan year is valid and intended.** Plan 1 above is effective
  2025-10-01 in plan year 2026. Summit stores it verbatim (proven 2026-09-12: `10/01/2025` against
  `1/1/2026–12/31/2026`, uncoerced). **Do not add a guard that "corrects" it** — the card-issuer plan
  depends on it.
- An unrecognised rule refuses the whole export (500) naming the row; the admin screen only offers
  the two supported values, so that refusal is reachable only from a hand-crafted POST.

---

## ⚠️ Why plans 2–4 sit in the PRIOR plan year — do not "correct" this

For a 1/1/2027 implementation, **December 2026 payroll deducts January 2027 premium**. That
salary reduction has to post into a cafeteria plan year that exists in **2026**, so every §125 leg
(plans 2, 3, 4) is created with `plan_year_offset_years = −1` — plan year 1/1/2026–12/31/2026 — even
though the coverage it funds is 2027. **This is LA-36 in structural form**: the last month of a plan
year funding the first month of the next is the month-ahead cadence LA-36 rests on (Federal Register
preamble to the 2007 proposed cafeteria-plan regulations; IRB 2007-39), bounded by the cross-year
purchase prohibition (Prop. Treas. Reg. §1.125-1 Q&A-7). A future reader who sees four plans in 2026
and one in 2027 and "fixes" the four to match the ICHRA year will break the December deduction and
the whole onboarding sequence. Leave them.

The ICHRA (plan 5) is deliberately **excluded** from the prior-year pattern: employer money, no
salary-reduction timing, and funds should not be available before the implementation date. It gets
the implementation-year plan year only (offset 0).

---

## Renewal — the manual `+ Assign Plan Year` step

A Summit plan persists across plan years and accumulates them; the `Import Plan ID` upsert key
carries no year (S31-J, LA-42). At renewal, **plans 2–5 each get a new plan year attached by hand**
in Summit — the `+ Assign Plan Year` action on the Edit Benefit Plan screen (proven additive: the
prior year's elections and balances survive as a second Active record, §7 step 5). AMS does not
automate this; a re-sent file 2 would upsert the same plan, not add a year.

**Plan 1 gets no renewal and expires deliberately.** Its purpose is card issuance at onboarding;
cards stay active while *any* card-enabled benefit holds funds, so once the real plans carry money the
card-issuer plan can lapse without affecting the participant's card.

---

## Entering the rows (W5, `/SummitPlanTemplateAdmin`)

Five rows against the PremiumPath service item, `seq` 0–4 (leave blank on add to take the next
free ordinal), each with a **distinct key segment** and a **distinct label** — the screen refuses
otherwise, because a shared key segment composes the same `Import Plan ID` (Summit's upsert key) and
the second plan would silently overwrite the first, and a shared label makes file 2's results
unattributable. `sort_order` is the emit order across all rows; `seq` is only the ordinal within the
service item. Inactive rows still hold their slot and still count for both uniqueness rules.

Template ids are **per tenant** and are never written into source or seeded — the numbers above are
the dev tenant's, read off its Summit configuration on 2026-09-12.

---

## What is still open

- Card issuance timing — assumed next-day-prospective (TA-e), untestable until SSA issues cards (T245).
  Funds are known to be live at record creation; the card is the unproven part.
- How AMS learns a plan's **employer funding method** and emits `Tier ID` on HRA Enrollment now that
  Contribution Schedule is the ICHRA standard (spec §9 #11 consequence; the shipped enrollment
  emitter still targets the 5-column template-1030 layout).
- Production template column counts vs production config (T247 closed on dev only; V103 not on
  dev_ssa or production).
- File 1 re-send semantics given empty-clears (T248).
