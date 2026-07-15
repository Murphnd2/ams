# QSEHRA Monthly Attestation & Transaction-Match Claims Engine — Epic

**Date:** July 14, 2026
**Status:** 💡 Backlog — not scoped, not sized, no implementation started
**Priority:** Not yet assigned (compliance-driven; recommend HIGH once scoped, given IRS Notice 2017-67 exposure)

---

## 1. Business Context (the WHY)

QSEHRA reimbursements are tax-excluded under IRC §106(g) only if the participant has minimum essential coverage (MEC) for the month the care is provided. IRS Notice 2017-67 requires (a) initial proof of MEC once per plan year, and (b) an attestation of continued MEC accompanying **each** payment request. The administrator may rely on the attestation absent actual knowledge it is false — the capture and storage of the attestation **is the liability shield**.

**Core design principle: two-key claim release.** A claim pays only when BOTH keys exist for the same coverage month:
1. That month's electronic attestation (the coverage key)
2. A matched premium card transaction (the expense key)

Neither key alone ever triggers payment, in either order. Card transaction data may auto-substantiate the *expense* side (recurring-expense matching against a substantiated baseline claim) but can **never** substitute for the attestation.

---

## 2. Entity Mapping (confirmed against current code, not guessed)

| Concept | Existing AMS entity | Notes / gap |
|---|---|---|
| Employer | `Employer` (`model/summit/archive/Employer.java`) | PK `id`→`organization_id`; `altId` holds Summit's numeric `employer_id`. Use as-is. |
| Employee / participant | `Employee` (`model/summit/archive/Employee.java`) | PK `id`→`employee_id`; FK `employer`. Use as-is. |
| Benefit (the QSEHRA plan itself) | `Benefit` (`model/summit/archive/Benefit.java`) | PK `benefit_id`, natural key `summitId`+`sourceType`, FK `planType`. Use as-is. |
| Benefit type / plan type | `PlanType` (`model/summit/archive/PlanType.java`) | This is the lookup actually attached to real `Benefit` rows (via FK). **QSEHRA has zero footprint here today** — no PlanType/BillingGroup row for it in code or data as verified. It exists only as a `LOS` catalog value (id in the 5–19 sales/proposal range, per `docs/analysis/entity_reference.md`) and as marketing copy in `landing-page.jsp` — the sales-side catalog was never wired to the benefit-administration side. **Adding QSEHRA (and confirming ICHRA / other-HRA `PlanType` rows exist and are correctly coded) is a prerequisite, not an assumption.** |
| Monthly enrollment recognition | **Existing monthly import already carries this data** (per developer, 2026-07-14) — Summit's monthly feed includes enrollment/plan-type information for every employee, but AMS does not currently persist a historical snapshot of it; only current-state fields on `Benefit`/`Employee` are kept. | **Scope decision (confirmed):** do not build a new roster-import pipeline. Instead, extend the existing monthly import to persist a **historical, month-stamped snapshot limited to the HRA family of plan types (QSEHRA, ICHRA, other HRA sub-types)** — not a full historical enrollment log for every benefit. This keeps the new table narrow and avoids duplicating data Summit already re-supplies monthly for all other plan types. |
| Enrollment roster CSV import (if ever needed standalone) | `UniversalImportService` (V048, config-driven) | `target_entity` enum currently supports `EMPLOYER`/`EMPLOYEE`/`BENEFIT`/`PLAN_TYPE` only — no historical-snapshot concept. Given the scope decision above, this path is now secondary — the primary path is extending the existing monthly Summit import, not a new Universal Import config. |
| Monthly scheduled job | `InstallationHealthScheduler` (`service/`, `ScheduledExecutorService`, lifecycle via `EmfListener`) | Only existing precedent is a **fixed-interval** (every 4h), **master-installation-only** poll. A QSEHRA job needs day-of-month logic and must run **per-PSP installation** (each installation processes its own employers) — reusable only as a lifecycle pattern, not as-is. |
| Attestation/reminder email | `EmailTemplate.wrap()` / `wrapBodyOnly()` (`data/util/`), `controller/monthly/` package | No persisted, versioned email-template entity exists anywhere in AMS — bodies are hand-built Java/JSP per use case. FR-2's requirement (centrally versioned legal text, employer may brand but never reword) needs new storage; nothing to reuse there. Per-employer branding can reuse the existing PSP/agency branding constants pattern. |
| Tokenized single-use link | `/q/{guid}` (`QuestionnaireInstance`) and `/apply/{guid}` (`Proposal.applicationGUID`) | Same reusable shape: `UUID` column + `@PrePersist` generation, single-use enforced via a status field transition (not token deletion/invalidation). **Neither existing pattern has an expiry field** — FR-3's month-bound expiring link is new logic layered on this shape. |
| Employer-scoped access control | `AgencyScopeResolver` (`data/resolver/`) | No employer-scoped equivalent exists (`EmployerContextResolver` is unrelated — a chat-NLU employer-name matcher, not an authorization scope). A new `EmployerScopeResolver` would need to be built following the same static-resolver + immutable-scope-object + `canSeeDetail()`/`canSeeRollup()` gate pattern this codebase just standardized on (see recent `AgencyScopeResolver` commits). |
| Reimbursement payment rail | **None found.** | Existing billing (`BillingItem`/`BillingGroup`/`BillingMonth`) models the PSP billing the *employer* for administration fees — the opposite direction of money flow from a QSEHRA reimbursement to an *employee*. `AchEntry`/`AchParseResult` only parse **incoming** ACH return/NOC reports, not outbound transfers. No NACHA file generation, no check-printing integration, no disbursement entity/table exists anywhere in the codebase. This is genuinely greenfield. |

---

## 3. Stories

### Story 1 — Historical HRA-Family Enrollment Snapshot (was FR-1)
As the system, each monthly import run persists a scoped historical record of which employees were enrolled in a QSEHRA/ICHRA/other-HRA benefit for that month, so later attestation eligibility and claim-month matching have a real historical record to query.

**Acceptance Criteria**
- Scope is limited to plan types in the HRA family (QSEHRA, ICHRA, other HRA sub-types) — not a general historical enrollment log for all benefit types.
- Reuses the existing monthly Summit import run; does not introduce a second, parallel roster-file import.
- Unmatched or ambiguous employee/benefit rows go to an exception queue — never silently dropped, never auto-created.
- A monthly job (configurable run day, default 1st) computes the attestation-eligible population from the persisted snapshot for that coverage month.
- **Blocked pending:** confirmation that `PlanType` rows exist/are correctly coded for QSEHRA, ICHRA, and other HRA sub-types (see §2).

### Story 2 — Monthly Attestation Email Generation (FR-2)
As an eligible enrollee, I receive a unique, month-bound email each month prompting me to attest to continued coverage, so the administrator has a fresh compliance record before releasing that month's claim.

**Acceptance Criteria**
- Each link is a unique, single-use, month-bound, unguessable token (UUID-class), invalid after use or expiry (expiry configurable).
- Email templates are configurable per employer (white-label), but the attestation legal text itself is centrally versioned — employers may brand, never reword.
- A configurable mid-month reminder is sent to non-responders.
- No attestation received → that month's claims **HOLD**. This is an explicitly safe failure mode: late click = late payment, never a compliance event.

### Story 3 — Attestation Capture Page (FR-3)
As a participant, I see a plain statement naming my baseline coverage and must take an explicit affirmative action to attest, so there is no ambiguity about consent.

**Acceptance Criteria**
- Page displays: "I attest that I [and my covered dependents, if applicable] continue to be enrolled in minimum essential coverage — [Carrier, Plan] — for [Month Year]," pulling Carrier/Plan from the participant's baseline claim record.
- Requires an explicit affirmative button click. **The box/action is never pre-checked. No confirm-by-silence.**
- Persisted record is **append-only / immutable once captured**: attestation text version ID, participant, benefit, employer, coverage month, timestamp, token ID, IP address, user agent.
- Identity binding is the tokenized link at minimum; an additional gate (DOB/last-4, or portal login) is configurable per employer.
- One attestation unlocks exactly **one** coverage month's premium payment event — never more.

### Story 4 — Card Transaction Feed Intake & Matching (FR-4)
As the system, I ingest the card transaction feed and match transactions to participants and baseline claims, so the expense side of a claim can be substantiated without manual review in the common case.

**Acceptance Criteria**
- Source: DataPath Summit/COMPASS export. **Blocked pending:** exact format, transport (SFTP?), and frequency — external dependency, potentially blocking this entire story.
- Matching chain: transaction → participant/card → baseline claim (merchant match, amount within tolerance, expected cadence) → coverage-month assignment.
- A centrally maintained merchant **exclusion list** (e.g., non-MEC carriers) is checked first: excluded merchants **never** auto-match for reimbursement, regardless of MCC. The list carries a dated change log.
- Unmatched or ambiguous transactions go to a review queue, never auto-approved.
- **Open question:** exact rule for assigning an early/late-transacted premium to its coverage month — derives from baseline due-date logic, to be finalized during design.

### Story 5 — Two-Key Claim Release (FR-5)
As the system, I release a claim for payment only when both compliance keys are present for the same coverage month, so no reimbursement is ever paid without both a coverage attestation and a substantiated expense.

**Acceptance Criteria**
- Auto-release fires **only** when ALL are true: a valid attestation exists for coverage month M; a matched transaction exists for month M; the baseline claim is active; amount ≤ available QSEHRA allowance/balance.
- **Hard rule:** payment goes out only on an existing reimbursement rail (ACH/check to the employee). A card account is **never** a reimbursement destination — this option must not be built, exposed in the UI, or reachable via API.
- **Blocked pending:** no reimbursement payment rail exists in AMS today (see §2) — this story cannot be implemented until that gap is designed and resourced separately.
- Partial-payment handling when premium exceeds remaining allowance: release the allowable portion per plan rules — **open question**, exact rule TBD.

### Story 6 — Baseline Claim Management (FR-6)
As an administrator, I establish and maintain each participant's recurring baseline claim, so downstream automated matching has a stable reference to check transactions against.

**Acceptance Criteria**
- First claim per policy uses traditional substantiation: uploaded premium statement (policyholder, coverage period, amount) plus the initial annual MEC proof.
- This establishes the recurring baseline (merchant, amount, cadence) used by Story 4's matching engine.
- An amount change (expected annually at open enrollment, or mid-year for a qualifying SEP change) triggers a re-baseline event.

### Story 7 — Audit & Reporting (FR-7)
As a compliance/finance user, I can see, for any payment, exactly which attestation and transaction authorized it, and can run reports on program health, so the liability shield is demonstrable on demand.

**Acceptance Criteria**
- Every payment's audit record ties together: attestation ID + transaction ID + baseline ID + release decision + acting party (system or user).
- Reports available: attestation response rate, holds by reason, unmatched transactions, exclusion-list hits, per-employer monthly summary.

---

## 4. Non-Functional Constraints

- Existing AMS stack only (Java/JSP, Maven, Tomcat 10, MySQL/EclipseLink) — no new frameworks without explicit approval.
- Use the existing `ScheduledExecutorService`/`EmfListener` lifecycle pattern for the monthly job; extend it for calendar-based (day-of-month), per-installation scheduling rather than inventing a new job framework.
- Employer-scoped access control throughout — new `EmployerScopeResolver`, modeled on `AgencyScopeResolver`'s static-resolver + `canSeeX()` gate pattern.
- TLS-only links; PII handling per existing AMS standards; attestation store append-only (no update/delete path).

---

## 5. Open Questions

1. **Summit/COMPASS transaction export** — format, transport (SFTP?), frequency. External dependency; **blocks Story 4**.
2. **`PlanType` coverage for the HRA family** — confirm QSEHRA, ICHRA, and other HRA sub-type rows exist and are correctly coded; QSEHRA currently has no footprint in the admin-side schema at all. **Blocks Story 1.**
3. **Historical snapshot schema** — exact shape of the new HRA-family enrollment-snapshot table/entity (fields, retention, relationship to `Benefit`/`Employee`) needs design; not guessed here per instruction.
4. Run-out window and late-attestation policy per plan.
5. Coverage-month vs. transaction-date assignment rule (Story 4).
6. Attestation page location: standalone tokenized page, inside a participant portal, or both.
7. Partial-payment plan rules (Story 5).
8. **No reimbursement payment rail exists in AMS today** — Story 5's payout mechanism is genuinely greenfield (no ACH origination, no check printing, no disbursement entity). Needs its own design/vendor decision, likely a separate epic in its own right.
9. `UniversalImportService.target_entity` has no historical/enrollment-snapshot value — moot if Story 1's "extend the existing monthly import" approach is confirmed, since that bypasses Universal Import entirely; revisit only if a standalone roster-file path is later needed.

---

## 6. Explicitly Out of Scope for This Epic (per hard rules)

- Any reimbursement destination that is a card account (Story 5 hard rule).
- Pre-checked or silence-implies-consent attestation UI (Story 3 hard rule).
- Auto-matching transactions from excluded merchants under any MCC (Story 4 hard rule).
- Mutating or deleting a captured attestation record once written (Story 3 hard rule).
