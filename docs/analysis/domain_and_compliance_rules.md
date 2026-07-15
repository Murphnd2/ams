# AMS Domain & Compliance Rules

Cross-cutting rules that shape feature design and must not be violated. Short by design; linked from
`CLAUDE.md`. Update here when a rule changes.

**Last reviewed:** 2026-07-15 (rev. b — SWBD brief v4 sync: §213(d) filed-form test, MEC floor, endorsement boundary).

---

## 1. SSA plan taxonomy (internal terms — not the federal categories)

SSA uses internal product names that do **not** map 1:1 to federal categories. Get these right in
feature logic, plan-type mapping, and UI:

- **HRA** — employer-funded §105 arrangement **with rollover** of unused funds.
- **MERP** (Medical Expense Reimbursement Plan) — same as HRA but **without rollover**.
- **DRiP** — a **MERP subtype** that reimburses **deductibles only**.

These are distinct from the **federal** arrangements (ICHRA, EBHRA, QSEHRA, …). Do not conflate
SSA's HRA/MERP/DRiP with federal HRA types.

## 2. Compliance data-source constraint

- **`CoverageStatus` is NOT a valid compliance data source.** It is billing-driven, has no backfill,
  and no run log — so it cannot be trusted for regulatory filing.
- All compliance reporting must source from **Summit exports**, not `CoverageStatus`. This constrains
  the planned **V072** ICHRA reporting (1094/1095-B + PCORI) and the QSEHRA attestation work
  (backlog #38).

## 3. HIPAA / BAA — PHI routing

- Any feature that touches **PHI** must route AI calls through **AWS Bedrock** (covered by a BAA),
  **not** the standard Anthropic API key.
- Non-PHI AI features (chatbot, email/proposal assistants) may use the standard Anthropic key.

## 4. Agent-markup security boundary

- **Base price and markup must never appear in public proposal page HTML.** The public GUID proposal
  page (`proposalPricing.jsp`) emits **sell price only**.
- The `base | markup | sell` breakdown is **internal-only** (`proposalDetail.jsp`). Guard this whenever
  touching proposal-pricing rendering — leaking base/markup into public page source is a
  confidentiality break. (V066/V067.)

## 5. QSEHRA design constraints (IRS Notice 2017-67)

Relevant to QSEHRA-based products (e.g. the SWBD/PremiumPath program — `docs/business/swbd_premiumpath.md`):

- **An employer offering a QSEHRA may NOT sponsor any group health plan** — *including* plans of only
  "excepted benefits" (dental, vision, accident, hospital-indemnity, cancer). Do **not** wrap those in a
  §125 cafeteria plan: doing so makes them employer-sponsored plans → disqualifies the QSEHRA and risks a
  **$100/day/employee** excise tax. Keep voluntary/ancillary products **post-tax**.
- **Compliant employer funding is "bonus up," not employer-paid premiums.** Employer raising post-tax pay
  is fine; an employer paying/collecting a carrier bill directly counts as sponsoring a plan (same
  disqualification + excise exposure).
- **One §125 is safe and recommended:** an **individual HSA** payroll pre-tax cafeteria plan pairs
  cleanly with a *premium-only* QSEHRA for HSA-qualified bronze plans.
- **Entity eligibility:** no classes/opt-outs; C-corp owner/employees can participate; **sole proprietors,
  partners, and >2% S-corp shareholders cannot**.
- **Reimbursement scope:** a QSEHRA can reimburse premiums and §213(d) expenses. A non-ACA supplemental
  premium is reimbursable **only if the filed policy form pays on expenses, not indemnity**: expense-
  incurred (% of actual charges) qualifies; per-period cash ($X/day) does **not**; a per-service fixed
  schedule is a **gray zone**. Allocation trap (Treas. Reg. §1.213-1(e)(4)): a bundled premium reimburses
  **only the separately-stated medical charge**. Get the filed form + counsel sign-off before the first
  claim. (Relevant to the SWBD "stack-wrap" — `docs/business/swbd_premiumpath.md`.)
- **MEC floor:** a QSEHRA reimburses nothing unless the participant holds **MEC somewhere** (any source —
  spouse/parent/Medicaid/Medicare). ICHRA, by contrast, locks out participants who have other MEC.
- **Endorsement boundary:** keep **carrier names off all SSA-drafted paper** (plan docs, notices, card,
  proposals). Program-because-of-one-carrier conduct risks reclassification as an employer-sponsored group
  health plan (→ QSEHRA disqualified, §4980D). Parallel to the agent-markup confidentiality boundary (§4).
