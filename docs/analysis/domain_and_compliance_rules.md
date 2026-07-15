# AMS Domain & Compliance Rules

Cross-cutting rules that shape feature design and must not be violated. Short by design; linked from
`CLAUDE.md`. Update here when a rule changes.

**Last reviewed:** 2026-07-15.

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
