# SWBD / PremiumPath

**Type:** Agency/GA demand + benefits-administration partnership ·
**Status:** **Active** — solution proposal delivered to Forrest **2026-07-15**; pilot-first.

## Summary

Southwestern Benefit Designers (SWBD), "**PremiumPath**" brand — a Texas GA / FMO-style consultancy
(Forrest Huggins). The engagement has grown from "first white-label agency" into a
**benefits-administration partnership**: SSA administers the **PremiumPath Program** for SWBD's
small-employer clients, delivered under SWBD's white-label brand (`premiumpath.net`).

**The PremiumPath Program** (Texas micro-groups, ~10 lives or fewer):

- **Management side** — owners / key employees on a **non-ACA, major-medical-style plan (Presidio)**,
  funded **post-tax** ("bonus up"), premiums paid via the **PremiumPath Card**.
- **Staff side** — a **minimal QSEHRA** ($50/mo, or the employer's chosen level) that (a) triggers a
  **federal Special Enrollment Period**, and (b) **preserves employees' premium tax credits** (de
  minimis — supplements rather than replaces the subsidy).
- **One payroll-deduction experience** to pay premiums; voluntary/ancillary products run post-tax on
  the card; optional **HSA+** (§125 payroll HSA) for bronze-plan employees.

## The relationship

- **Forrest Huggins** — SWBD owner; problem-solving consultant/FMO, not a pure producer.
- **Tracy** — SWBD office / operations support.
- **Presidio (Daniel Cruz)** — the non-ACA carrier/product (accident + critical-illness "two-tranche"
  plan built to behave like major medical — **expense-based, not indemnity**).
- **Annette Bechtold** (Forte Consulting, Atlanta) — made the intro; compliance guidance
  (ICHRA/QSEHRA/§125).

## Status & history

- **2026-07-13 (Mon)** — Intro capabilities call (with Annette). Established SSA can administer QSEHRA
  + individual list/direct billing with consolidated remittances for Forrest's Presidio model.
  Post-tax basis agreed (§125 pre-tax not feasible). Pilot-first.
- **2026-07-14 (Tue)** — Deep-dive (Forrest, Tracy, Kevin). Locked the management-Presidio-post-tax +
  staff-minimal-QSEHRA model and the PremiumPath Card mechanism. Forrest's ask: fees/pricing + a setup
  video + a revenue share; numbers by Wednesday.
- **2026-07-15 (today)** — SSA proposal delivered: *"The SWBD PremiumPath Program — Services & Pricing."*

## Opportunity streams (Forrest's framing)

1. **Larger TPA relationships** seeking ICHRA options for **under-25 / under-50 markets**.
2. **Small employers (<10 lives)** — per-head admin fees + **revenue share** to SWBD.

Both parties want a **non-VC-backed, flexible, long-term** relationship with room to experiment.

## Revenue / pricing model

- SWBD's margin rides on top of SSA's base **as an administrative fee** (not insurance commission):
  **baked-in SWBD rate card**, **per-quote custom markup**, or **both** (guaranteed baseline +
  additional). One combined employer invoice; monthly remittance to SWBD with a per-group statement.
- Program fees (from the 2026-07-15 proposal): $500 one-time establishment · $50/mo combined minimum ·
  QSEHRA base $350/yr + $5/participant/mo · PremiumPath Card $200/yr + $3.50/participant/mo + $2 card
  issuance · HSA+ add-on $250 doc + $150 NDT + $15/app + $5/mo. (Full detail in the proposal doc.)

## AMS work this drives

**Delivery layer (built):** white-label portal + email (V068–V071), agent markup (V066/V067),
GA→sub-agency hierarchy (V070/V071), open decision #39.

**Program layer (largely existing AMS billing capability, productized as "PremiumPath"):**

- **PremiumPath Card** — a custom **post-tax insurance-payment benefit + debit card**: employee-funded
  via voluntary post-tax payroll deduction (employer holds the funds as a payroll liability — no money
  moves to SSA), card in the employee's own name, **restricted to insurance-carrier merchant codes**,
  spendable only up to the payroll-deposited balance, used as **carrier autopay**; **one consolidated
  settlement draft** to the employer per payment day.
- **QSEHRA-Lite admin** — plan docs + adoption paperwork, SEP documentation + **dated per-employee
  eligibility letters** (Marketplace proof), reimbursement by direct deposit/check, W-2 Box 12 Code FF.
- **Individual-level list/direct billing + consolidated remittance** (existing platform: any benefit
  item tied to an individual, online/coupon payments, consolidated billing, remittances).
- **HSA+** — §125 payroll HSA for bronze-plan employees (the one recommended cafeteria plan).
- **Future enhancement:** carrier **premium quoting** for ICHRAs (SSA doesn't shop carriers today) —
  potentially via a **Sherpa**-type API — only if volume justifies it.

## Compliance guardrails (this model)

- **No §125 for the voluntary lineup.** IRS **Notice 2017-67**: an employer offering *any* group
  health plan — expressly including excepted-benefit-only plans (dental, etc.) — is **disqualified from
  offering a QSEHRA**. Keep voluntary products **post-tax** (on the card). See
  `docs/analysis/domain_and_compliance_rules.md`.
- **Management "bonus up," not employer-paid.** Employer raises post-tax pay → employee funds their own
  card → card autopays the carrier. An **employer-paid** carrier arrangement counts as sponsoring a
  health plan → disqualifies the staff QSEHRA + exposes the employer to the **$100/day/employee** excise
  tax.
- **Post-tax throughout** (§125 pre-tax not feasible here, per Annette/Kevin).
- **Entity eligibility:** no classes/opt-outs; C-corp owner/employees can participate; **sole
  proprietors, partners, and >2% S-corp shareholders cannot**. Flag entity type at intake before quoting.

## Future / parked — QSEHRA "stack-wrap" opportunity (don't lose)

Raised by Kevin on the 7/14 call; **not today's task**, but worth developing with Forrest later:

A **larger QSEHRA** where the employee buys a **bronze ACA** plan that does *not* consume the full
QSEHRA; the **leftover QSEHRA funds** then reimburse the premium of a **non-ACA, expense-based (not
indemnity) underwritten supplemental** policy — effectively **stacking/wrapping** QSEHRA + bronze ACA +
supplemental into near-comprehensive coverage.

- Only works if the supplemental pays **on expenses, not indemnity chunks** (indemnity premium is not
  QSEHRA-reimbursable).
- Different employer segment — those willing to fund toward the **max** end of the QSEHRA (vs. the
  de-minimis $50 model above).
- Needs a **carrier that can build an expense-based product** (Presidio's current product includes
  indemnity, so wouldn't qualify as-is).
- Bigger opportunity, more design/compliance work — park until the pilot lands, then raise with Forrest.

## Open items / next

- Deliver pricing + a **setup video** to Forrest (pricing done in the proposal).
- Agree a **pilot case**; map the end-to-end workflow (Forrest to provide a flowchart).
- **Producing-agent count** — the opportunity-sizing question.
- Decision on **carrier quoting / Sherpa** (volume-gated).
- Develop the **stack-wrap** opportunity (above) as a later track.

## Related

- White-label onboarding: `docs/runbooks/agency_white_label_domain_onboarding.md`
- Compliance rules: `docs/analysis/domain_and_compliance_rules.md`
- Opportunity register: `docs/business/README.md` · DataPath: `docs/business/datapath.md`
