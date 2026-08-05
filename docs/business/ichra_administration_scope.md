# ICHRA Administration — End-to-End Service Scope

**Created:** 2026-07-28
**Status:** Scope inventory for pricing and build planning. **Not counsel-reviewed.**

> **Compliance note:** items marked ⚖️ have legal consequence and require counsel sign-off before
> being relied on for a live group. This document is an operational inventory, not legal advice.

## Why this is separate from QSEHRA

`docs/business/swbd_premiumpath.md` describes the QSEHRA (PremiumPath) service list. **ICHRA is not
QSEHRA with different numbers.** Key divergences:

| | QSEHRA | ICHRA |
|---|---|---|
| Employee classes | Forbidden | Permitted, with minimum-class-size rules |
| Contribution cap | Statutory | None |
| Employer size | <50 FTE, no other group plan | Any size |
| Qualifying coverage | **Any-source MEC** — spouse's plan, parent's plan, Medicaid, Medicare | **Individual-market coverage or Medicare only** |
| Affordability calc | N/A | Required — drives PTC eligibility |
| 1095-B filing | No | **Yes** |
| PCORI fee | Generally no | **Yes** — Form 720 |
| W-2 | Box 12 Code FF | **Not** Code FF — do not carry this habit over |

The MEC narrowing is the one most likely to break a small group in practice: **one employee on a
spouse's group plan can invalidate an ICHRA design that would have worked as a QSEHRA.**

## Phase 1 — Design (pre-sale)

- Entity/eligibility screen — sole proprietors, partners, and >2% S-corp shareholders are not
  employees and cannot participate.
- ⚖️ Class design — classes permitted; minimum-class-size rules apply if a class is split.
- Contribution setting — flat, or varied by age and family size; variation must be uniform within a
  class and follow the same curve.
- ⚖️ **Affordability determination** — requires the lowest-cost silver plan for each employee's age
  and rating area. Determines whether the offer is affordable, which determines PTC eligibility.
  **Computable from the HealthSherpa quoting API** (free tier, no approval required) — a direct line
  from an already-available API to a billable deliverable.
- Spousal/dependent premium reimbursement decision.

## Phase 2 — Documents and notice

- ICHRA plan document, SPD, adoption agreement / corporate resolution.
- ⚖️ **90-day employee notice.** ⚠️ **This assertion is uncited and must not be relied on — see LA-08
  in `docs/analysis/legal_assumptions.md`, status "Open — no basis."** Kept here as the record of what
  was once written, not as guidance. Required 90 days before the plan year. For a *newly established*
  ICHRA the notice is due by the date coverage begins — **the exception a short-runway group relies
  on. Confirm with counsel before committing to an effective date.** Getting this wrong is a
  plan-qualification issue. **SSA has NOT determined ICHRA notice timing. It does not transfer from
  QSEHRA, and no date, offset or range above should be treated as SSA's position — the date is set by
  SSA per case.**
- ⚖️ **ERISA safe-harbor notice and posture.** Individual policies stay outside ERISA only if:
  enrollment is voluntary; the employer does not select or endorse any particular issuer or plan; no
  employer consideration changes hands; and employees are told the individual coverage is not an
  ERISA plan.
  **This directly constrains any plan-shopping UI SSA builds**: complete list, neutral ordering,
  employee-controlled sort/filter, **no "recommended" badge, no default selection, no curation, no
  hidden carriers.** SSA builds it on the employer's behalf, so employer endorsement is exactly what
  a curated list would be argued to be.
- Dated per-employee eligibility notices — the SEP proof artifact.

## Phase 3 — Enrollment

- SEP triggering and tracking (`offered_ichra`).
- ⚖️ **Initial substantiation** — proof of individual-market MEC or Medicare before the first
  reimbursement. Narrower than QSEHRA (see table above).
- Effective-date coordination — no API endpoint pre-validates effective dates for a SEP reason; a
  human step. ✅ **CONFIRMED 2026-08-04** against HealthSherpa's own documentation: *"No endpoint to
  query valid dates ahead of time"* — the carrier validates on submission and returns either a list
  of valid dates or a message that selection is unavailable. `desired_effective_date` is optional,
  and omitting it (recommended) lets the carrier derive from SEP type and event date. **Carrier logic
  varies:** *"first of next month from today"* covers Ambetter, the BCBS entities, Cigna, Molina,
  Oscar and UHC; the 15th-of-month cutoff applies to CareSource and MedMutual, **not** the Texas
  carriers. ⚠️ **Operational consequence:** a 9/1/26 effective date on a first-of-next-month Texas
  carrier requires the submission to land in **August**.
- Payment mechanics: employee-pays-then-reimbursed, or direct-to-carrier premium payment. **The
  former is cleaner on the endorsement question.**
- **Enrollment does not end on SSA's system.** Carrier payment is a browser form; until the member
  pays, status is `pending_effectuation` and **they are not covered and cannot be reimbursed.**
  Chasing non-effectuated members is a staffed operational task, and the most likely failure mode in
  the whole flow — a missed SEP window means no coverage for the plan year.

## Phase 4 — Monthly (recurring revenue)

- **Coverage verification before every reimbursement release.** Cannot reimburse a month in which
  coverage was not in force. Backlog #38. Largest ongoing labor item and the primary automation
  target.
- Reimbursement processing and payment rail — **no reimbursement payment rail exists in AMS today.**
- Employer invoicing; SWBD remittance.
- Mid-year new hires (each with their own SEP deadline), terminations, life events.
- ⚖️ Termination — ICHRA loss is itself a SEP. Federal COBRA does not reach small employers; **state
  continuation may.** Worth a one-time counsel read per state, then a standing rule.

## Phase 5 — Annual compliance

- ⚖️ **1094-B / 1095-B filing** — ICHRA is MEC; the plan sponsor reports. Anticipated as planned V072.
- ⚖️ **PCORI fee** — Form 720, annually by July 31. Recurring, small, easy to forget, penalty-bearing.
- ⚖️ §105(h) nondiscrimination testing.
- W-2 — **not** the QSEHRA Box 12 Code FF item.
- Renewal — recompute affordability against next year's LCSP, reset contributions, reissue the notice
  on time.

## Pricing implication

ICHRA admin is materially heavier than QSEHRA-Lite. Affordability computation, narrower MEC
substantiation, 1095-B, and PCORI are all net-new. **Three of those are things a low-cost competitor
quietly does not do well** — which is where the case against zizzl's ~$660/mo minimum is made.

## Boundary — what SSA does NOT do

- Not the ERISA **Plan Administrator** — that is the employer as plan sponsor. SSA provides
  ministerial administrative services. This distinction belongs in the BAA, the plan documents, and
  all client-facing material.
- No commission, no carrier appointment, no participant funds held.
- **Not plan-selection advice.** Guidance on which plan to choose is agent territory. Route those
  questions to the licensed agent. Educational content about *how ICHRA works* is SSA's to write.
- No carrier servicing after effectuation — claims, disputes, ID cards, network questions belong to
  the member and carrier, with the agent as AOR.

---

## MEC, subsidies, and off-exchange — population segmentation

*Added 2026-07-29.*

> ⚖️ **The enrollment-right question in this section is the load-bearing legal fact under the whole
> off-exchange model and requires counsel sign-off. Do not build against it as settled.**

### Off-exchange ACA coverage is MEC

Minimum essential coverage includes coverage under a health plan offered in the individual market
within a State. **Nothing in that definition turns on whether the policy was purchased through an
exchange.** Off-exchange ACA individual-market coverage therefore satisfies:

- QSEHRA's MEC requirement, and
- ICHRA's narrower individual-health-insurance-coverage requirement

equally well. "Off-exchange" is not a compliance downgrade.

### Subsidies only matter to the subsidy-eligible

For an employee whose household income places them outside premium-tax-credit range, **the exchange
offers nothing the off-exchange market does not.** They need two things: the enrollment right, and the
tax-free reimbursement. Off-exchange delivers both — arguably more cleanly, with no APTC
reconciliation at tax time and no income attestation to the exchange.

### ⚖️ The linchpin is the enrollment right, not MEC

SEPs are an exchange construct. ACA-compliant issuers in the individual market **outside** the
exchange are subject to guaranteed-availability rules requiring limited open enrollment periods for
the same triggering events — which is why an off-exchange enrollment rail exists at all and why an
ICHRA/QSEHRA offer functions as an enrollment reason there.

**However:** the off-exchange triggering-event list is not a perfect mirror of the exchange SEP list,
and carrier practice varies. **Counsel question, stated precisely: does the QSEHRA/ICHRA triggering
event reliably compel off-exchange issuers, and on what window?** This sits alongside the existing
open item on the exact SEP window (60-day placeholder).

### The segmentation, and how to determine it

**PTC eligibility is not eyeballable.** It is household income measured against the federal poverty
level, adjusted for household size, and upper-threshold behavior has changed with legislation more
than once. **Verify current-plan-year thresholds rather than relying on a remembered number.**

This is a calculation, and the tooling exists: HealthSherpa's `POST /api/v1/aptc_estimates` endpoint
(required: `zip_code`, `fip_code`, `household_income`, `applicants[]`; returns `estimated_aptc` and
`csr_level`), and quoting with `household_income`, which returns subsidy information inline.
✅ **Both confirmed present 2026-08-04** — the endpoint path is corrected here from `/aptc_estimate`,
and `household_income` is verified as a documented parameter on `POST /api/v1/quotes` itself, so the
segmentation output needs **one optional field added to a call AMS already makes**, not a second
integration (backlog **T147**). ⚠️ **"Ungated" means no separate approval, not no credential** — both
are live calls and sit behind the same staging/production access gate as all quoting.

**So intake includes a routing step:**

| Segment | Path | Status |
|---|---|---|
| **Not subsidy-eligible** | Off-exchange. Fully servable on the rail available today. | ✅ Available |
| **Subsidy-eligible** | Requires on-exchange to actually claim the credit. | ❌ Unresolved — see the on-exchange section in `healthsherpa.md` |

**Effect on PremiumPath:** the off-exchange-only constraint does not invalidate the design — **it
splits the population.** PremiumPath's non-subsidy-eligible participants are servable now. Its
subsidy-eligible participants are not. This is a materially better position than "PremiumPath does not
fit," and it lowers the urgency of the on-exchange question from a gate to a roadmap item.

### ⚠️ Guardrail — "off-exchange" is not a synonym for ACA-compliant

Short-term limited duration insurance, fixed indemnity plans, and health care sharing ministries are
all sold off-exchange and **none of them are MEC.** For ICHRA the requirement is narrower still —
individual health insurance coverage or Medicare.

Anything enrolled through HealthSherpa's off-exchange rail is ACA individual market and therefore
fine. **The risk is employee-sourced coverage.** If a participant presents coverage they obtained
independently, substantiation must confirm it is ACA-compliant individual-market coverage — **not
merely that a policy exists.**

**This is a real failure mode with a real consequence: reimbursements paid against non-MEC coverage
are taxable, and the employer carries the exposure.** Substantiation logic must check coverage *type*,
not just coverage *presence*.
