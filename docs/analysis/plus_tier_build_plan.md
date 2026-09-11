# ICHRA+ / QSEHRA+ — Reconciliation, Open Items, and Phased Build Plan

**Prepared for:** Kevin Murphy, Superior State Administrators
**Date:** 29 July 2026
**Status:** Planning only. No code, no schema, no SQL produced.
**Baseline:** Migration **V073**, to be re-verified against `ls docs/migrations/` before any script is written.
**Revision 10** — D41, the card's two sub-accounts fund on different rules by benefit type.
**Part 13 governs.** It files the funding asymmetry between the excepted-benefit and the
off-exchange major-medical sub-account as a decision. It changes nothing in Parts 1–12.

⚠️ **Header note, 2026-09-08.** **Part 12 shipped without a header bump** — exactly as Parts 9 and 10
did, and exactly what the 2026-08-04 correction below exists to prevent. The block read *"Revision 9
— Part 11 governs"* while Part 12 (D40) already existed. No revision number is assigned to Part 12
retrospectively; **Revision 10 absorbs it.** The precedence line, not the revision number, is the
authority on what governs.

*Revision 9* — O2 resolved from public documentation; four HSOne-era premises dissolved; O20
escalated to load-bearing.
*Part 11* resolves **O2** (with one residual promoted to O41), partially answers O14,
sharpens O15, half-answers O16, escalates **O20**, upgrades **D20** from a decision to a requirement,
strengthens D16 and D38, makes D17's `HS_POLICY_STATUS` rung concrete per carrier, and adds O41–O42.

⚠️ **Header correction, 2026-08-04.** This block previously read *"Revision 6 — Part 8 governs"* while
**Parts 9 and 10 already existed** in the file (D38, the LCSP data-source split, 2 August; and D39,
the two-LOS decision, 2 August). Under this document's own precedence rule — later Parts govern — the
header had been understating the current revision by two Parts and pointing readers at a superseded
one. **`docs/business/README.md` had copied the same wrong claim** and was corrected the same day.
Two revision numbers were never assigned in the header (Parts 9 and 10 shipped without bumping it);
this block jumps to **Revision 9** so the number and the Part count agree from here.

*Revision 8* — **Part 10**: D39, `ICHRA+` and `QSEHRA+` are two separate LOS rows (structural — the
quotable attached-product bundle differs by statute).

*Revision 7* — **Part 9**: D38, the LCSP data-source split (CMS for compliance, HealthSherpa for
illustration; V078's columns retained as a cross-check).

*Revision 6* — Custom ID mailing-export correlation resolved negatively, division-scoped ICHRA
classes, the two-report notice model, and the HealthSherpa provider-check resolution.
**Part 8** resolves O34 (negatively), O35, O36, and O38, narrows B4a's reconciliation
problem, adds O39–O40, and resolves O23 favorably.

*Revision 5* — Summit Data Exchange capabilities, correlation keys, and the J7 benefit export.
**Part 7** adds three decisions (D35–D37), revises D34, may retire D10/D11/D32, adds a
new platform phase, and carries the **consolidated build list and question list**.

*Revision 4* — notice automation, census intake and the notional COBRA mechanism settled. **Part 6**
resolves D18, D29, D31 and O30, reframes O5, repositions B4a, splits A4.

*Revision 3* — the census and participant-loading architecture was settled after Part 4 was written.
**Part 5 deletes phase B0** and resolves several items Part 4 opened.

*Revision 2* — `summit_plus_tier_discovery.md` and `phase_a_ichra_enrollment_portal.md` were synced
after Parts 1–3 were written. **Part 4 supersedes Parts 1–3 wherever they conflict.**

Earlier parts are left as written — the provenance is worth keeping, and this design has already
reversed more than once.

---

## How to read this document

**Part 1** reconciles seven conflicts (C1–C7) between `docs/business/plus_tier.md` — written without
`healthsherpa.md` in context — and the synced documentation set. It ends with the doc-correction list
and the Gate 0 query.

**Part 2** replaces open items O1–O9 with a renumbered O1–O21 series plus six new decisions
(D15–D20), sorted by dependency rather than topic.

**Part 3** is the phased build plan, **sequenced for earliest evidence that SSA makes SWBD's network
win more cases** — not for earliest live administration. It adds four open items (O22–O25) and four
decisions (D21–D24) that the partnership reframe surfaced.

**Part 4** revises the plan following the sync of two documents that were unavailable when Parts 1–3
were written: `docs/analysis/summit_plus_tier_discovery.md` and
`docs/analysis/phase_a_ichra_enrollment_portal.md`. It adds four open items (O26–O29), four decisions
(D25–D28), and one new phase (B0).

**Part 5** settles the census and participant-loading architecture. It **deletes phase B0**, resolves
D25 and D26, and adds three decisions (D29–D31) and one open item (O30).

**Part 6** settles notice automation, census intake, and the notional COBRA mechanism. It resolves
D18, D29, D31 and O30, reframes O5, repositions B4a earlier, splits A4, adds three decisions
(D32–D34) and three open items (O31–O33), and **corrects a T35 claim in Part 5**.

**Part 7** covers Summit Data Exchange (scheduled SFTP both directions), the Participant Custom ID
and J7 benefit-export correlation keys, and the manual-benefit-setup constraint. It adds D35–D37 and
O34–O38, revises D34, and ends with the **consolidated build list and prioritised question list** —
the two working artifacts.

**Part 8** resolves the Participant Custom ID mailing-export question negatively (O34) — the SSN hash
correlation mechanism (D10/D11/D32) survives — narrows O35 and O36 with the J2/J7 employer-identifier
bridge, finds strong evidence for division-scoped ICHRA classes (O38), and establishes that the two
notice reports are complementary rather than competing, narrowing B4a's reconciliation problem. It
adds O39–O40 and resolves O23 favorably via `docs/business/healthsherpa.md`'s 2026-07-30 section.

**Part 9** splits the LCSP data source (**D38**): CMS's ICHRA Employer LCSP Look-up Table for
compliance-facing affordability, HealthSherpa for illustration figures. V078's `onex_*` columns are
retained as a cross-check, not removed.

**Part 10** establishes (**D39**) that `ICHRA+` and `QSEHRA+` are **two separate `LOS` rows** — the
quotable attached-product bundle differs by statute, not by preference.

**Part 11** resolves **O2** from public documentation — the enrollment/status surface, the poll
design, and the correlation model — and dissolves four HSOne-era premises, including the
employer-scoped `GET /v1/enrollments` polling design that backlog **#38**'s "concrete unblock" rested
on. It escalates **O20** to load-bearing (`plan_hios_id` is required on every enrollment route, so the
plan display cannot be handed off), upgrades **D20** to a requirement, and adds **O41–O42**.

**Part 12** (7 September 2026) adds **D40** — the ICHRA+ bundle gets its own Summit plan types — and
**O43**, whether Premium Billing plans mirror into AMS as `Benefit` rows.

**Precedence: Part 13 > Part 12 > Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**

### Provenance warning governing everything below

`healthsherpa.md`'s correction section disclaims part of its own earlier content: the `api_enrollable`
semantics, the 42-of-65 Hopkins enrollability count, the `POST /v1/enrollments` payload shape, and the
`employer_external_id` + `updated_since` polling design **all describe HSOne, not the ICHRA Partner
API**. Several `plus_tier.md` assumptions descend from that text. They are not wrong; they are
**unverified against the target product**, and each is marked.

Anything sourced from `docs.ichra.healthsherpa.com/integration-guide/supported-carriers` (the carrier
matrix) is correct-product and treated as authoritative.

---
---

# Part 1 — Reconciliation

## C1 — HealthSherpa is not EDE; O1 is malformed

**Resolved:** O1 is malformed as written. EDE is the on-exchange FFM pathway; the target rail is
off-exchange, where no EDE transaction occurs and "EDE applicant consent" does not exist as an object.

**Probably resolved (→ gate):** access appears partner-scoped, not per-participant-consent-gated.
Correct-product evidence:

- The Policy Status webhook is subscribed **at the partner account level**, with exchange scope chosen
  at setup. It pushes to SSA's endpoint. No per-applicant gate appears anywhere in the setup flow.
- The payload carries `external_id` — described as SSA's own correlation key.
- On the deeplink path, **HealthSherpa collects SSN, immigration status, incarceration status,
  attestations and signatures** inside its own flow. Applicant consent is captured there, by them.

Not settled: the `GET /v1/enrollments` filter list including `employer_external_id` is HSOne-era.
→ **Gate O2.**

### The schema change survives

C1 assumed the `employee_id` addition existed only to carry marketplace consent. Four independent
loads sit on a per-employee artifact, three unrelated to HealthSherpa:

1. **ICHRA PTC opt-out / waiver.** ICHRA rules require the employee be permitted to opt out and waive
   future reimbursements, annually. A per-employee, per-plan-year signed record held by the plan
   administrator. HealthSherpa is not a party to it.
2. **Initial MEC substantiation.** Proof of individual-market or Medicare coverage before the first
   reimbursement. Where no carrier status feed exists — the launch market, per C2 — this is a
   per-employee artifact by necessity.
3. **Dependent detail**, unless Summit supplies it (O8).
4. **Tobacco**, if actually needed — see O19.

**Also:** `plus_consent` was never a table in the design. The new-tables list is
`rating_area_rate_cache`, `plus_quote`, `plus_census_stage`, `plus_participant`,
`participant_coverage_month`, `notice_obligation`, `notice_event_map`. Nothing structural to delete;
the correction is textual.

**Verdict:** keep the nullable `employee_id` on `questionnaire_instance` and the widened unique index.
Restate its rationale as **substantiation + PTC waiver**, not marketplace consent, and rename the
artifact accordingly. **No branch needed.**

## C2 — Policy Status is carrier-gated

Confirmed from the correct-product matrix. Hopkins County TX:

| | BCBS TX | CHRISTUS | UHC |
|---|---|---|---|
| Deeplink enrollment | live | live | live |
| **Policy Status** | **2026 (planned)** | **not listed** | **live** |

HCSC (the Blue Cross entity covering TX) payment webhook is *In Progress*. CHRISTUS Health Plan is
live on-exchange only.

**Correction to the brief:** the brief lists the market as Christus / Ambetter / BCBS.
`healthsherpa.md`'s eval found **no Ambetter in Hopkins on either exchange**, and warns explicitly
that the statewide issuer list is not county-level availability. The list may be SWBD's *appointments*
rather than county *availability*. → **Gate O3.**

**Inversion accepted, with a sharper conclusion.** The one Hopkins carrier with live Policy Status is
not the one an employee is likely to land on, and **SSA cannot influence which one they land on** —
the ERISA safe harbor forbids curation, ordering, defaults and endorsement. So verification method is
neither knowable at proposal time nor steerable afterward.

That is precisely the reasoning behind **D2 (single bundled PEPM)**. C2 does not undermine D2; it
independently confirms it.

**Recommendation — off-exchange only at launch (D16).** Three reasons stack: on-exchange
`/v1/policy-status/*` is agent-scoped and alpha; the rail needs agent licensure SSA does not hold; and
it needs a Marketplace agent-account link SSA deliberately declined to make.

### The asymmetry finding — ICHRA+ and QSEHRA+ are not the same product

Not in C1–C7, and the most consequential item in this reconciliation.

`plus_tier.md` treats `ICHRA+` and `QSEHRA+` as two labels on one build. The off-exchange rail breaks
that:

- Off-exchange coverage carries **no APTC** — net premium equals gross.
- Per `healthsherpa.md`, the off-exchange rail serves the non-subsidy-eligible population fully and
  the subsidy-eligible population not at all.
- PremiumPath's staff-side QSEHRA is **designed to preserve the premium tax credit** — de minimis
  $50/mo, supplementing rather than replacing the subsidy. Subsidy-eligible by construction.

**Therefore: subsidy-eligible QSEHRA+ participants cannot be enrolled through the rail SSA is
building.** QSEHRA is supported off-exchange as a `type` value — which is what Julian confirmed — but
that serves a QSEHRA population that has forgone the subsidy, which is not PremiumPath's population.

**Consequence (D15):** ICHRA+ gets an SSA-mediated enrollment leg. **QSEHRA+ does not.** Its "+"
content is the quoting/illustration layer plus verification and administration; the employee enrolls
on-exchange through their licensed agent. Still saleable, and honest. The quote-stage router therefore
routes not only on legal eligibility but on **which rail can serve the resulting population**.

## C3 — Webhooks exist

Confirmed: two independently subscribable webhooks (Submission Confirmation, Policy Status), sharing a
schema; `paid_through_date` and `grace_period_start_date` in the payload. `plus_tier.md`'s polling
implication is stale.

**`paid_through_date` is the right primitive** — "coverage was in force through this date" is a
defensible basis for releasing a month's reimbursement; "is this person currently active" is not.

**Recommendation: push primary, incremental poll as reconciliation, attestation as the floor.**
Sequenced against three blockers:

1. Webhooks are **not on by default**. Setup routes through an account manager or implementation
   specialist who provides a form. **No onboarding representative is assigned.** Nothing is testable
   until one is.
2. Inbound Policy Status carries member and policy data — **PHI**. The **BAA is unaddressed**
   (Geozoning, Inc. DBA HealthSherpa). Production enablement is BAA-gated.
3. The poll fallback is HSOne-era and unverified (O2).

**Architecture:** `/api/*` behind `ApiTokenFilter` is the natural home. Which authentication methods
HealthSherpa's form accepts is not in the synced docs (→ O15) — `ApiTokenFilter` is a static-token
mechanism; HMAC would need new code.

**Design consequence:** the ledger's ingest must be **source-agnostic behind one interface**, with
sources landed in dependency order — `ATTESTATION` first (no external access needed), then a webhook
receiver against staging, then poll reconciliation, then production post-BAA.

## C4 — `employer.external_id` as a third correlation key

The keys sit at different grains, so a flat list of three would mislead.

**Employer level**

| Key | Owner | Direction | Status |
|---|---|---|---|
| `Employer.id` (= Summit Organization ID) | AMS | internal | exists |
| Summit `EmployerCustomID` / `ERCustomID` | Summit | AMS ↔ Summit | D12, typed in at setup |
| HealthSherpa `employer.external_id` | AMS-supplied | AMS → HS | **HSOne-era → O2** |

**Participant level**

| Key | Owner | Direction | Status |
|---|---|---|---|
| `Employee.id` (= Summit `Participant_ID`) | Summit | AMS ↔ Summit | exists |
| `ssn_hash` (HMAC-SHA256) | AMS | **internal only** | D10 — sole purpose is the mailing-export join |
| HealthSherpa per-policy `external_id` | AMS-supplied | AMS ↔ HS | correct-product confirmed |

Two design calls this forces:

- **The employer-level and policy-level HealthSherpa external ids are different fields at different
  grains.** Do not collapse them.
- **Do not use `ssn_hash` as the HealthSherpa `external_id`** (→ **D20**). It is the obvious shortcut
  and it is wrong: it puts a pseudonymous SSN derivative on the wire to a third party and pins the
  hash space. Mint a separate opaque participant UUID for outbound use. `ssn_hash` never leaves AMS.

## C5 — Backlog #16 overlaps #43

**Recommendation: #16 becomes a phase of #43** — not absorbed silently, not left separate.

**Why it shrinks to phase size.** #16 was scoped as an authenticated, PHI-bearing employee portal —
the hardest problem its Phase A identified. The deeplink finding largely dissolves it: HealthSherpa
collects the SSN, immigration status, incarceration status, attestations and signatures; AMS transmits
**prefill demographics only**. What remains is a census-gated handoff that prefills, attaches
`_agent_id`, and redirects.

**Why it cannot be independent.** It is strictly downstream of census staging and `plus_participant` —
the handoff needs a participant row and the selling agent's NPN. Left separate, it will be scoped
independently and grow a duplicate participant model.

**Mechanics:** keep #16 as a pointer row (`→ #43 Phase N`) so the Phase A doc stays discoverable;
update #12's supersession note to follow. **Rename the deliverable** from "Employee Enrollment Portal"
to "enrollment handoff" — the old name keeps re-summoning the PHI-portal framing.

**One gate stays on that phase only (O14):** whether the deeplink supports employee self-service or
assumes an agent completes it. The Use Cases text says *redirect agents to complete each employee's
enrollment*. Two constraints ride with it: the ERISA presentation rules, and the **effective-date
trap** — `desired_effective_date` returns 422 if invalid for the SEP reason and event date, with **no
pre-validation endpoint**.

## C6 — Do the standard LOS rows exist? (Gate 0)

Unresolved, and correctly so — `entity_reference.md`'s LOS value list derives from
`ReferenceDataSeeder.java`, which is dead code, and the live `DatabaseInitializer` LOS block is
commented out (476–510). The file carries an accuracy warning for exactly this.

**One extension the brief does not include.** An `LOS` row alone does not make a quotable line of
service. An LOS prices only through an attached `ServiceModule` → `RateTable`, and **agency quoting
eligibility is implied, not explicit** — it requires an `agencyrates` assignment *plus* a
non-zero-priced `RateTable` row. An unpriced enhancement simply does not render. So Gate 0 must probe
**three** things: reference rows, a priced module path, and a task sequence. An `LOS` row with no
priced `RateTable` is invisible, which is indistinguishable from absent for planning purposes.

**Branch A — a priced, sequenced catalog exists.** Additive per D1: add `ICHRA+` / `QSEHRA+` `LOS`
rows, `ServiceModule`s, priced `RateTable` rows, enhancement links, and an ICHRA task sequence. Days.

**Branch B — any leg is missing (including "exists but unpriced").** The catalog is created, split
three ways (→ **D18**): schema via migration; reference rows for live environments via migration
`INSERT` or `D-NN` deployment-backlog items; future PSPs via the commented `DatabaseInitializer` LOS
block. Branch B also inherits the compliance checklist — today a sold ICHRA case *would inherit the
generic HRA checklist with zero ICHRA compliance steps*. That means real content: 90-day notice with
its newly-established-plan exception, ERISA safe-harbor notice, affordability determination, initial
substantiation, 1095-B, PCORI, §105(h). Weeks, not days.

## C7 — The card is partly built

**Accepted:** D13 is in flight, not pending. Summit configuration in progress on existing
Summit/COMPASS rails — post-tax, participant-funded, Contribution-Schedule-driven, MCC 6300 loaded.
Remaining gates: **live carrier authorization test** + **issuing-bank purse classification**.

**O9 merges into the authorization test (→ O10).** Same event. With one added requirement: **capture
the actual MCC the test transaction posts under**, not merely whether it approved.

**The discrepancy (→ O9).** `plus_tier.md` D13 says MCC-restricted to **6300 and 5960**;
`swbd_premiumpath.md` says **6300 loaded**. If only 6300 is loaded and an acquirer posts under 5960
(direct marketing — insurance services), the transaction **declines** — a failed premium payment on an
individual policy, which is a lapse path. Settle before the live test, not after.

**"PTC" rename.** Confirmed: collides with *premium tax credit*; rename to PIP/PPA before client
exposure; participant-facing "Insurance Payment Account." **AMS-side dependency:** if AMS ever stores
or maps the Summit plan template code — most plausibly as a `plantype` row — the rename must land
**before** that mapping is written, or "PTC" gets baked into AMS reference data.

### The convergence — the plan-shaping conclusion

C1, C2 and C7 land on the same point:

- **C2:** Policy Status is unavailable for the likely carriers, and SSA cannot steer carrier choice.
- **C7:** the card's ability to pay a premium and post under a restricted MCC is **untested**. Until
  O10 passes, `CARD_TRANSACTION` is not known to be a source at all.
- **C1:** a per-employee substantiation artifact is required anyway, for unrelated reasons.

So making card verification primary is **one step too far**. At launch:

| Rank | Source | Status |
|---|---|---|
| **Primary** | `ATTESTATION` | no external dependency — buildable today |
| Promoted on O10 passing | `CARD_TRANSACTION` | gated on O9 + the live test |
| Arrives per carrier | `HS_POLICY_STATUS` | gated on matrix + rep + BAA |

`verification_source` already accommodates all three — no data-model change. **`ATTESTATION` is the
only source with no external gate**, which makes the per-employee attestation path the earliest
revenue-supporting phase — and it consumes the `questionnaire_instance.employee_id` change C1 was
expected to delete.

## Doc corrections needed

**`docs/business/plus_tier.md`** — nine, plus a provenance header:

1. Rewrite **O1**; strike "EDE applicant consent" and the Setup-stage note about EDE consent.
2. Rename "marketplace consent" to substantiation + PTC waiver; restate the `employee_id` rationale;
   note no `plus_consent` table existed.
3. **New D15** — ICHRA+ / QSEHRA+ asymmetry on the enrollment rail.
4. Invert verification primacy; add the carrier-gating table and the un-steerability point; note that
   C2 independently confirms D2.
5. Record **D16** (off-exchange only at launch) with its three reasons.
6. Replace polling-implied language with push-primary / poll-reconcile / attestation-floor; name the
   account-manager and BAA gates.
7. Add the two-level correlation-key map; state `ssn_hash` never leaves AMS (**D20**).
8. Mark **D1**'s "additive" as conditional on Gate 0 Branch A.
9. Restate **D13** as in-flight with named gates; fold in O9; flag the 6300-vs-5960 discrepancy; add
   the "PTC" rename dependency.

**`docs/business/README.md`** — the documented entry point, so staleness is expensive. Its HealthSherpa
row still reads *"Webhooks are **unconfirmed** … do not plan around them"* and frames the integration
as EDE; the SWBD row says *"carrier quoting via HealthSherpa EDE integration."* Both contradict the
correction section.

**`docs/business/swbd_premiumpath.md`** — open items still carry the malformed O1.

**`docs/analysis/project_backlog.md`** — #43's "Blocked-ish on HealthSherpa consent question (O1)" is
now wrong; keep the `employee_id` requirement with the new rationale; #16 → pointer row; #12's
supersession note follows; #42 carries the stale webhook/EDE framing.

**`docs/analysis/domain_and_compliance_rules.md`** — see **O25** in Part 3.

**Connector** — add `summit_plus_tier_discovery.md` and `phase_a_ichra_enrollment_portal.md`.

## The Gate 0 query

Read-only. No DDL, no DML, no `constant` writes. **Not a migration** — it must not be filed under
`docs/migrations/`; that is exactly the orphaned-`.sql` pattern T38 exists to clean up. If it earns a
home in the repo it belongs in `docs/analysis/`.

Run **once per environment** (production, master, demo, BPO) and label each result set —
`migration_tracker.md` shows the environments badly diverged.

```sql
-- ============================================================
-- Gate 0 — ICHRA/QSEHRA reference-data probe
-- READ-ONLY. No DDL. No DML. Safe on production.
-- Purpose: decide Branch A (rows exist, "+" is additive)
--          vs Branch B (catalog must be created).
-- Run per environment; label output with the environment name.
-- ============================================================

SELECT DATABASE() AS schema_name, @@hostname AS host, NOW() AS run_at;

-- --- 0. Settle the tracker discrepancy while we're here -----
-- claude_memory flags V072/V073 as unapplied everywhere despite
-- the launcher being live in production. This answers it.
SELECT * FROM schema_version ORDER BY 1;

-- --- 1. Which of the expected tables exist here -------------
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('los','enhancement','enhancement_los','losmodules',
                     'servicemodule','templatepurpose','plantype',
                     'rate','ratetable','priceitem','agencyrates',
                     'requiredtasklist','tasksequencetable','task')
ORDER BY table_name;

-- --- 2. Column names, so nothing below has to be guessed ----
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('los','enhancement','servicemodule','templatepurpose',
                     'plantype','ratetable','agencyrates','requiredtasklist')
ORDER BY table_name, ordinal_position;

-- --- 3. The catalogs, in full (all small reference tables) ---
SELECT * FROM los             ORDER BY 1;
SELECT * FROM templatepurpose ORDER BY 1;   -- = ServiceItem
SELECT * FROM plantype        ORDER BY 1;
SELECT * FROM enhancement     ORDER BY 1;
SELECT * FROM servicemodule   ORDER BY 1;   -- the pricing bridge

-- --- 4. Join tables: is anything actually wired? ------------
SELECT * FROM losmodules      ORDER BY 1;
SELECT * FROM enhancement_los ORDER BY 1;

-- --- 5. Is any module PRICED? ------------------------------
-- An LOS with no non-zero RateTable row does not render, which is
-- indistinguishable from absent. Substitute the real price column
-- name from step 2 before running.
SELECT module_id,
       COUNT(*) AS rate_rows,
       SUM(CASE WHEN <price_col> > 0 THEN 1 ELSE 0 END) AS priced_rows
FROM ratetable
GROUP BY module_id
ORDER BY module_id;

-- --- 6. Agency quoting eligibility, the other half ---------
SELECT * FROM agencyrates ORDER BY 1;

-- --- 7. Task sequences (confirm table names from steps 1-2) -
SELECT * FROM requiredtasklist ORDER BY 1;
```

**Invocation on production:**

```bash
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu \
mysql --socket=/var/run/mysqld/mysqld.sock -t beta_ssa \
  < gate0_probe.sql > gate0_production_$(date +%F).txt
```

**Decision rule**

- **Branch A** — an `LOS` row for ICHRA and/or QSEHRA exists, *and* reaches a `ServiceModule` with at
  least one non-zero `RateTable` row, *and* has a task sequence attached.
- **Branch B** — any of those three is missing.
- **Branch A-minus** — rows exist but unpriced or sequence-less. Plan as Branch B; the remediation is
  smaller but the phase still exists.

---
---

# Part 2 — Revised open items

## Disposition of O1–O9

| Old | Item | Disposition |
|---|---|---|
| **O1** | EDE consent → TPA Policy Status access | **Dissolved.** Malformed. Schema consequence settled → **D17**. Replaced by **O2** and **O19**. |
| **O2** | Summit `EventTypeID` value set | **→ O5, rescoped.** Gates notice *reconciliation* only, not obligation tracking. |
| **O3** | Card feed `Date` — post or swipe | **→ O6, downgraded**, plus **D19** (the policy choice). |
| **O4** | Policy Status coverage off-exchange | **RESOLVED, unfavorably** — exists but carrier-gated. Remainder → **O16**. |
| **O5** | `DivisionName` as class carrier | **→ O4 + O7, downgraded.** ICHRA-only; irrelevant at small headcounts. |
| **O6** | Dependent DOB availability | **→ O8, downgraded.** HealthSherpa collects household detail on the deeplink path. |
| **O7** | Employer funding mechanics | **→ O11, unchanged.** |
| **O8** | Counsel (4 items) | **→ O17–O21.** Three were individually plan-shaping and buried by aggregation. |
| **O9** | Carrier card acceptance / MCC | **Merged into D13's gate → O10.** AMS-side consequence → **D17**. |

## Replacement list

### Self-answerable now — no external dependency

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O1** | **Gate 0 probe.** Do live DBs hold ICHRA/QSEHRA `LOS`, `ServiceItem`, `PlanType`, a priced `ServiceModule`→`RateTable` path, and a task sequence? Per environment. | The Part 1 query | Branch A vs B — the size of B1 |
| **O2** | **Re-verify the enrollment/status API surface** against the ICHRA Partner API: is the enrollment list employer-scoped; do `employer_external_id`, `updated_since`, and `employer.external_id` exist. All four HSOne-era. | Public docs, `/llms.txt`, `GET <page>.md?ask=`. **No account needed.** ~1 hour | The correlation map, poll design, and every HSOne-inherited assumption |
| **O3** | **Which carriers are actually in Hopkins off-exchange**, 2026 + 2027. Settles the Ambetter discrepancy. | Free quoting API | Any illustration shown to Forrest |
| **O4** | **Does the AMS import promote `DivisionName`?** Staged and discarded today. | Code read (Phase A) | ICHRA class model only. Not launch-blocking |

### Summit / DataPath

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O5** | **`EventTypeID` enumeration**, and which values are letter-generating. | Summit / DataPath; **check the unsynced Summit doc first** | `notice_event_map` and notice **reconciliation**. **Not** obligation tracking — AMS can create obligations from its own events with no knowledge of Summit's IDs. This split de-risks the notice phase considerably |
| **O6** | **Card feed `Date` semantics.** Resolution path: pull the forensics report, which carries both `PostDate` and `SwipeDate`, for the same transactions and compare. | Summit report comparison | Nothing until O10 passes |
| **O7** | **Can Summit vary contribution by division/class?** | DataPath | ICHRA class support only |
| **O8** | **Dependent DOB availability.** | Summit export inspection | Family-tier amounts and Summit dependent setup — not enrollment |
| **O9** | **Which MCCs are actually loaded** — 6300 only, or 6300 + 5960. | Summit template inspection | **Risk item.** A 5960 posting against a 6300-only template declines, which on an individual policy is a lapse path |
| **O10** | **Live carrier authorization test** — does a carrier accept the card, and **under which MCC does it post**. | One real transaction | Whether `CARD_TRANSACTION` becomes a source at all |
| **O11** | **Employer funding mechanics** — ACH pull vs prefund; do carded participants differ. | Kevin / Forrest / DataPath | Whether invoicing needs a funding line. **Scope guard:** AMS billing produces counts, not dollars (D5/D9). Do not let this pull dollars back into the billing model |

### HealthSherpa — vendor-gated

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O12** | **Onboarding representative assignment.** None assigned. Routes staging credentials *and* the webhook form. | HealthSherpa | **The tightest bottleneck on the HealthSherpa track.** Chase independently of the technical questions |
| **O13** | **BAA execution.** Geozoning, Inc. DBA HealthSherpa. Unaddressed by anyone. | Both parties + counsel | Any production PHI flow. Staging (synthetic) is not gated |
| **O14** | **Deeplink: self-service or agent-completed?** | HealthSherpa | Employee page vs agent workstation |
| **O15** | **Webhook auth methods supported** — static token, HMAC, mTLS. | Docs, then the form (partly O12-gated) | Whether `ApiTokenFilter` suffices |
| **O16** | **Policy Status timing for the launch market** — BCBS TX *when* in 2026; CHRISTUS at all; HCSC payment webhook. | HealthSherpa (sent to Julian) | When `HS_POLICY_STATUS` becomes real. The BCBS date *largely determines whether SSA builds for a 2026 or 2027 launch* |

### Counsel

| # | Item | Why separated | Blocks |
|---|---|---|---|
| **O17** | **90-day notice — the newly-established-plan exception.** For a new ICHRA the notice is due by the date coverage begins rather than 90 days prior. | Governs whether any short-runway effective date is achievable at all. Getting it wrong is a plan-qualification issue, not paperwork | Every short-runway case, permanently — not just one client |
| **O18** | **MEC substantiation** — initial and ongoing. Specifically: **does a signed employee attestation suffice where no carrier status feed exists?** | **Validates or invalidates the launch verification design.** If attestation alone is insufficient as a reimbursement-release record, D17 fails and needs replacing before it is built | B3's launch configuration |
| **O19** | **Is the ICHRA affordability LCSP premium tobacco-loaded?** | Direct schema consequence | Whether `rating_area_rate_cache` needs its tobacco dimension, and whether a person-level tobacco field is needed at all |
| **O20** | **ERISA safe-harbor posture on an AMS-built plan display.** Constraints are known; what needs confirming is whether SSA building it *on the employer's behalf* is itself an endorsement problem. | The scope doc flags that a curated list would be argued to be employer endorsement | B5's UI |
| **O21** | **Standing counsel docket** — QSEHRA SEP window · §213(d) / Presidio filed form · MEC-floor design · PCORI · 1095-B/1094-B · §105(h) NDT · state continuation on ICHRA loss · W-2 (**not** Box 12 Code FF) · Tex. Ins. Code ch. 4151 TPA certificate (parked, pre-scale). | Nine sub-items, one owner, one delivery | Annual-compliance phases, mostly downstream |

## New decisions (D15–D20)

Choices for you, not external unknowns. Four currently read as blockers and are not.

| # | Decision | Recommendation | Depends on |
|---|---|---|---|
| **D15** | ICHRA+ and QSEHRA+ are **not symmetric** on the enrollment rail. ICHRA+ gets an SSA-mediated enrollment leg; QSEHRA+ does not. | Adopt. The alternative is selling a capability that cannot exist on the available rail | — |
| **D16** | **Off-exchange only at launch.** | Adopt | — |
| **D17** | **Verification-source ladder** — `ATTESTATION` primary; `CARD_TRANSACTION` on O10; `HS_POLICY_STATUS` per carrier. No data-model change. | Adopt, subject to **O18** | O18 |
| **D18** | **Reference-row delivery** — migration `INSERT` vs `D-NN` items. Rule 7 forbids `INSERT INTO constant` explicitly; silent on these tables. | Your call — it materially changes B1 under Branch B | — |
| **D19** | **Month-attribution rule** for card verification. Does a 2 September post date verify September or August? Carriers commonly draft in the prior month. | Decide deliberately rather than inheriting whatever the parse code does first | O6 |
| **D20** | **Outbound correlation key is a separate opaque UUID.** `ssn_hash` never leaves AMS. | Adopt, and state it — the shortcut is obvious and wrong | — |

## Crosswalk

| Old | New |
|---|---|
| O1 | dissolved → O2, O19, D17 |
| O2 | O5 (rescoped) |
| O3 | O6 + D19 |
| O4 | resolved → O16 |
| O5 | O4 + O7 |
| O6 | O8 |
| O7 | O11 |
| O8 | O17, O18, O19, O20, O21 |
| O9 | O10 + D17 |
| — | new: O1, O2, O3, O9, O12–O15 · D15–D20 |

**Cross-reference discipline:** `plus_tier.md` should carry only items gating a "+"-tier build
decision, and **reference** `healthsherpa.md`'s vendor list rather than copying it. Duplicating vendor
questions into a second list is how docs accumulate three same-purpose sections and start triggering
Claude Code anchor-ambiguity hard-stops. O12–O16 are summaries with pointers.

---
---

# Part 3 — Phased plan (partnership criterion)

## The criterion, and what changed

The earlier draft sequenced for **earliest value against a live effective date**. The right criterion
is **earliest evidence that SSA makes SWBD's network win more cases**. Those produce different plans.

Two consequences fall out immediately:

**Gate 0 stops being the critical path.** The catalog-and-proposal work is about *selling
administration services*. Winning a partnership is about demonstrating capability. The illustration
tool needs no `LOS` row — so the Branch A/B risk that dominated the earlier draft moves off the front
and lands in the middle.

**Layer 5 arrives far earlier.** The GA console was placed last because a book view needs live
policies. But the **pipeline** version needs only prospects, which SWBD has today. `Opportunity
extends Activity` already carries stage, prospect, agency, `estimatedEmployees`, `estimatedValue`,
`expectedCloseDate`. With V070's hierarchy and V071's per-agency tokens, downline pipeline visibility
is mostly views and read models — no new schema, and buildable before a single ICHRA case exists.

## Structure

Three tracks. **Track A runs first and alone.** Track B's gates are long-lead, so chase them during
A. Track C is what makes leaving expensive.

**Nothing in Track A depends on Gate 0, HealthSherpa approval, a BAA, or counsel sign-off.** That is
deliberate: the entire front of the plan is unblocked today.

**Migration numbers below are placeholders from a V073 baseline.** If tracks run in parallel, allocate
numbers in **commit order, not plan order**. The Gate 0 probe also settles the V072/V073 tracker
discrepancy, which must be resolved first — if the tracker is wrong, the baseline is wrong.

---

## Track A — Partnership evidence

### A0 — Unblock (days)

**Scope.** Run the **O23** network/formulary data test (free, load-bearing for A2). Send Forrest the
book-profile ask (**O22**) and the data-sharing ask (**O24**). Open **O12** (rep) and **O13** (BAA)
now — both are long-lead and gate Track B. **T29** security fix. **T35** ships on its own — do not
bundle it. **T38** orphan registration. Part 1 doc corrections. Cut **`v0.73.PP`**. Run the Gate 0
probe when at a workstation; its result is not needed until B1.

**Gate.** None. Everything is startable today.
**Schema.** **V074** — T35 column drops. **V0NN** — T38 renumbering.
**Dependencies.** None.
**Demoable outcome.** Nothing. Days, and it removes three ways later phases could be built wrong.
**Deferred.** All "+" features.

### A1 — Rating illustration, agent-facing ⭐

**Scope.** ZIP → county + FIPS → cached market rates → premium range for the market. Optional
age-band counts upgrade it to a net-cost table with an affordability threshold per band. **Authenticated
agent access** (**D22**) so every illustration is attributed to a named agent and sub-agency. No
prospect PII, no employer record required.

**Why first.** Fewest dependencies, highest leverage. Quoting is free, self-serve, needs no approval,
no BAA, and touches no PHI — already proven at rate parity. It needs **no `LOS` row**, so it is immune
to Gate 0. Per the capability map, sales-and-modeling tools are *the layer that makes SSA interesting
to an agency rather than merely useful*.

**Gate.** **O3** + **O22** — which markets to warm the cache for. **O19** decides whether the tobacco
dimension is *used*, not whether the column exists.
**Schema.** **V075** — `rating_area_rate_cache` (plan year × county FIPS × age band × tobacco; LCSP
self-only, benchmark silver, lowest bronze, carrier count, plan count, `fetched_at`; tobacco column
nullable and unused pending O19, so one column does not cost a second migration). **V076** —
`illustration_log` (agency, agent, ZIP, FIPS, headcount, plan year, mode, timestamp, result summary —
**no PII**).
**Dependencies.** HealthSherpa quoting key (held). Agent logins via the existing `Invitation` system.
**Demoable outcome.** **The partnership demo.** Forrest and any downline agent run live cases
immediately. Nothing to install, nothing to sign.
**Deferred.** Prospect-facing public token front door (D22, increment two); proposal embedding (B1);
class optimization (A3).

### A2 — "Will I lose my doctor?" — conditional

**Scope.** Employee- or agent-driven provider check: enter physicians, see which plans in the county
include them. Permitted under the ERISA constraints precisely because employee-controlled sort/filter
is allowed; what is forbidden is curation, badges, defaults and hidden carriers.

**Why it matters.** This is the first question every ICHRA employee asks and a common reason employers
decline. It also de-risks the case for the employer, which is what actually closes it.

**Gate.** **O23.** `healthsherpa.md` flags as its highest-value untested item whether plan objects
carry provider networks, drug formularies and benefit summaries — warning that if they do not, *the
shopping UI is a price list with phone calls behind it*. **If O23 fails, this phase does not happen**,
and several later assumptions need revisiting. Free to test with the key already held.
**Schema.** Likely none — `GET /v1/reference/providers` is free and ungated. Optional provider cache
deferred.
**Dependencies.** A1's county resolution.
**Demoable outcome.** The objection that kills cases, answered on screen. The highest-emotion demo in
the plan.
**Deferred.** Formulary check; benefit-summary comparison.

### A3 — Design deliverables (billable pre-sale)

**Scope.** Three analyses from one census:

1. **Subsidy segmentation** — split the population into subsidy-eligible (better off with PTC
   preserved → QSEHRA-lite, or a deliberately unaffordable ICHRA) and non-subsidy-eligible (ICHRA
   works fully). This is the PremiumPath thesis made computable, and it answers *which product to
   sell* before anyone commits.
2. **Affordability threshold per employee** — the contribution level at which the offer flips from
   unaffordable (PTC preserved) to affordable (PTC lost). A curve per person, and the most
   misunderstood mechanic in ICHRA design.
3. **Class optimization** — ICHRA permits classes, QSEHRA forbids them, and HealthSherpa names class
   optimization as an intended use case for these endpoints.

**D21 — the design census is a separate object from the administrative census.** Age, ZIP, family
tier, household income band. **No SSN, no PII beyond that.** This keeps quoting PII-free per D7 and
keeps the entire sales stage outside the BAA question. The original design treated census as one
thing; splitting it is what lets A3 ship years before O13 resolves.

**Gate.** **O19** for exactness. The analyses are useful before it resolves.
**Schema.** **V077** — `design_census` + `design_scenario` (modeled output, reproducible months
later).
**Dependencies.** A1's rate cache.
**Demoable outcome.** A priced ICHRA design with per-employee impact and a defensible class structure
— **and a design fee.** Turns pre-sale work from a cost into revenue.
**Deferred.** Group-to-ICHRA conversion analysis (A4 — needs the current group premium).

### A4 — Renewal-defense engine ⭐ the flagship

**Scope.** Import SWBD's **group** book (employer, renewal date, headcount, ZIP, carrier, current
premium). A scheduled radar surfaces every renewal 90–120 days out, auto-runs the conversion analysis,
and delivers a queue of pre-analyzed opportunities to the responsible agent — *before* the renewal
conversation rather than during it.

**Why this is the flagship.** AMS already does this shape of work for its own clients:
`Benefit.nextRenewalDue`, the `RENEWAL_DAYS_OUT` lookahead, `fillRenewalEmployers()` scanning for what
is coming due. The capability map already names *a tool that helps them win cases they would otherwise
lose to a group renewal* as what the agent gets. This is the mechanism, assembled almost entirely from
parts that exist.

**D24, non-negotiable.** Output goes to the **agent**, never to the employer directly. Per the
three-bucket liability framing — employer conduct (§4980D), agent market-conduct (steering,
replacement), and SSA's narrow lane — supplying analysis to a licensed agent is squarely in SSA's
lane. Contacting an employer to recommend replacing coverage is not.

**Gate.** **O24** — will SWBD share its book, and in what format. **This is the partnership test as
much as a technical gate.** If he shares the book, the relationship is real. If he will not, that is
itself the answer, and it arrives cheaply.
**Schema.** **V078** — `agency_book_group`, agency-scoped, with an optional FK to `Prospect` once an
entry converts (**D23**).
**Dependencies.** A1 + A3. Scheduler pattern: `InstallationHealthScheduler` (JDK
`ScheduledExecutorService`, master-only, 4-hourly) is the template — the only scheduled-job precedent
in the codebase.
**Demoable outcome.** *"Here are the eleven groups in your book renewing in the next 120 days where
ICHRA beats the renewal, with the numbers already run."* That is a different conversation from a
feature demo.
**Deferred.** Carrier-direct renewal data; automated employer outreach (forbidden by D24).

### A5 — Pipeline console (Layer 5, early)

**Scope.** Downline visibility — which sub-agency, which agent, which stage, what value, what
activity. Built from `Opportunity` + V070 hierarchy + V071 tokens + A1's illustration log + A4's book.

**Gate.** None.
**Schema.** Likely views and read models only; **V079** if a view chain is needed.
**Dependencies.** A1, A4.
**Demoable outcome.** Forrest sees his network working — precisely what a GA cannot get from a carrier
portal or a spreadsheet. Layer 5 arriving years before the book of policies exists.
**Deferred.** Book view over live policies and the alert engine (C2).

### A6 — Design advisor

**Scope.** A `chatbot_skill` answering agents' ICHRA/QSEHRA design questions — S-corp shareholder
eligibility, whether the group's dental plan disqualifies a QSEHRA, MEC narrowing, class minimums —
from rules already written in `domain_and_compliance_rules.md` and `ichra_administration_scope.md`,
plus the `federal_rules` KB seeded by V063. HealthSherpa's docs are themselves AI-queryable via
`?ask=`, so live product facts are reachable too.

**Hard boundary.** Education with citations. **Never plan selection**, never anything requiring
licensure — those route to the licensed agent.

**Gate.** None.
**Schema.** **V080** — `chatbot_skill` row + KB content. Precedent: V065 seeded
`EMAIL_DRAFT_ASSISTANT` the same way.
**Dependencies.** None technically; content quality depends on A0's doc corrections.
**Demoable outcome.** Nearly free — schema, service and content all exist. High perceived value per
unit of work.
**Deferred.** Any PHI-touching use, which would route through Bedrock per the HIPAA tiering.

---

## Track B — Administration (recurring revenue)

Starts once the relationship is real. Long-lead gates (**O12**, **O13**, **O18**) should already be
moving from A0.

### B1 — "+" catalog and proposal integration — the Gate 0 branch lives here, and only here

**Scope (both branches).** Make `ICHRA+` / `QSEHRA+` quotable and get the illustration onto a
white-label proposal. `ProposalSection` LOS scoping (absent today — `ApplicationSection` has it via
`applicationsectionlos`; `ProposalSection` does not), the "+" pricing section per D14 with its two
render variants, `plus_quote` for reproducibility, and the quote-stage router — entity type,
group-plan status, MEC narrowing, and **rail routing per D15**.

**Branch A.** Additive per D1. Days.
**Branch B.** Catalog created, split three ways per **D18**, and it inherits the ICHRA compliance
checklist. Weeks.

**Gate.** **O1** resolved; **D18** decided; **D15**, **D16** adopted.
**Schema.** **V081** — `proposalsectionlos` + LOS scoping. **V082** — `plus_quote`. Branch B adds
reference-row delivery.
**Dependencies.** A1 (the illustration to embed).
**Demoable outcome.** A complete white-label "+" proposal on `premiumpath.net` — illustration
embedded, priced, agent markup applied and invisible to the employer, apply flow already live.
**Deferred.** Class optimization; agency-default markup; optional `ReviewApplication` pricing display.

### B2 — Census, participant model, Summit handoff

**Scope.** `plus_census_stage` (raw SSN transits and is cleared after export, per D11);
`plus_participant` with `ssn_hash` + last four (D10) and the separate opaque outbound UUID (D20); the
two-pass Summit export (create at setup, update post-enrollment with carrier, plan, premium, effective
date); employer correlation via `EmployerCustomID` as a setup task (D12); promotion of the
already-staged J3 fields (`DOB`, `HireDate`, `EffectiveDate`, `TerminationDate`, `DivisionName`) —
parsed and discarded today, so this is promotion code plus entity fields, not new parsing.

**Gate.** None external. Needs a sold "+" case.
**Schema.** **V083** — `plus_census_stage` + `plus_participant`. **V084** — `Employee` field additions.
**Dependencies.** B1. **Watch T36** — three code paths write `ParticipantStatusId` /
`EmploymentStatusID` to two different `Employee` columns depending on pipeline and insert-vs-update
branch, and static analysis cannot say which is authoritative. The "+" tier inherits that latent bug,
and status changes drive notice obligations. Resolve here or accept knowingly.
**Demoable outcome.** Sold case → census upload → Summit participants created.
**Deferred.** Dependent detail (O8); class model (O4 / O7).

### B3 — Verification ledger, attestation source

**Scope.** `participant_coverage_month` as a durable ledger deliberately outside the billing pipeline
(D9 — the pipeline's only correction mechanism is whole-month wipe-and-recreate). The per-employee
questionnaire attachment: nullable `employee_id` on `questionnaire_instance` alongside the not-null
`activity_id`, unique index widened to `(questionnaire_id, activity_id, employee_id)` — the campaign
hangs off the employer's Setup activity, each instance pointing at one employee. Attestation campaign,
monthly per-employer verification view, exception queue, and a **source-agnostic ingest interface** so
B6 and B7 plug in without rework.

**Gate.** **O18** — counsel confirming a signed attestation suffices as a reimbursement-release record
where no carrier feed exists. **If it does not, this design fails and needs replacing before it is
built.**
**Schema.** **V085** — `questionnaire_instance.employee_id` + widened index. **V086** —
`participant_coverage_month`.
**Dependencies.** B2. **No billing changes** — AMS produces counts, dollars happen in Wave (D5). O11
must not pull dollars back in here.
**Demoable outcome.** A monthly coverage-verification report per employer with an exception queue.
**The recurring-revenue proof**, and the answer to "how do you do this without zizzl's $660 minimum."
**Deferred.** Automated sources (B6, B7). Reimbursement payment rail — **no such rail exists in AMS
today** (backlog #38), and it is not in this plan.

### B4 — Notice obligations

**4a — obligation tracking.** `notice_obligation` (participant × notice type × effective date;
`PENDING → EXPORTED → MAILED → RECONCILED`), created from **AMS's own events** — new hire, renewal,
rate change, termination. **No dependency on O5**: AMS needs no knowledge of Summit's event IDs to
know it owes a notice.

**4b — reconciliation.** Ingest the Summit mailing/coverage-event feed (`EventTypeID`, `EventName`,
`Mailed`, joined on `ssn_hash` with DOB + surname as a secondary check for census-typo hash misses),
plus `notice_event_map`. Delivers the **completeness assertion**: every obligation has a corresponding
mailed row. AMS does not replicate proof of mailing — Summit retains that. Requires explicit
EclipseLink **L2** cache eviction after write, since this feed reports changes originating outside
AMS.

**Gate.** 4a: none. 4b: **O5**.
**Schema.** **V087** — `notice_obligation`. **V088** — `notice_event_map` + mailing-feed staging.
**Dependencies.** B2 for the participant model.
**Demoable outcome.** 4a: an obligation register with aging. 4b: a defensible answer to "prove every
required notice went out."
**Deferred.** Per-letter tracking — the feed carries no document reference, so an obligation ties to
"a notice of type X was mailed to this person in this period." Sufficient for completeness.

### B5 — Enrollment handoff (absorbs backlog #16)

**Scope.** ICHRA+ only, per D15. Census-gated handoff: prefill demographics, attach `_agent_id`
(required on the deeplink — and the reason AOR travels per application by NPN, so SSA never competes
with an agency's agents for the policy), redirect. **HealthSherpa collects the SSN, immigration
status, incarceration status, attestations and signatures.** That is what shrinks #16 from an epic to
a phase, and it must not drift toward EnrollConnect, which reinstates the entire PHI-collection
problem.

**Gate.** **O12**, **O13** (outbound prefill is PHI), **O14**, **O20**, **O2**.
**Schema.** Likely none beyond `plus_participant` fields for HealthSherpa identifiers (B2).
**Dependencies.** B1–B2. **Non-negotiable design constraints:** complete list, neutral ordering,
employee-controlled sort/filter, no "recommended" badge, no default selection, no curation, no hidden
carriers. And the **effective-date trap** — 422 on an invalid `desired_effective_date` with no
pre-validation endpoint. Build the retry UX in from the start.
**Demoable outcome.** Employee or agent goes from AMS to a completed off-exchange application without
SSA touching an SSN.
**Deferred.** QSEHRA+ enrollment (D15 — not available on this rail). On-exchange (D16). EnrollConnect.

### B6 — Card transaction ingest — conditional

**Scope.** Ingest the Summit card feed. Idempotency on `(UserID, Date, TransactionAmount,
MerchantName)`; join `UserID` → J2 `User_ID` → `Participant_ID`, which rolls dependent-card
transactions up automatically; carrier matching tolerant of `MerchantName`'s 16-character truncation;
premium variance routed to the exception queue with a possible rate-change notice obligation rather
than a verification failure. Discard `CardNumber` at the parse boundary.

**Gate — four, one terminal.** **O9**, **O10**, **O6** + **D19**. **If O10 fails, this phase does not
happen** and `ATTESTATION` remains primary indefinitely.
**Schema.** **V089** — card transaction staging.
**Dependencies.** B3's ingest interface. DataPath card configuration complete. **"PTC" renamed before
any AMS-side plan-type mapping is written.**
**Demoable outcome.** Automated verification for carded participants; the manual/automated mix becomes
reportable.
**Deferred.** The forensics report — pull on demand for exceptions only.

### B7 — Policy Status ingest

**Scope.** Webhook receiver on `/api/*` behind `ApiTokenFilter`, consuming Submission Confirmation and
Policy Status. `paid_through_date` is the operative field. Add incremental poll reconciliation for
missed webhooks.

**Gate.** **O12**, **O13**, **O15**, **O16**, **O2**.
**Schema.** **V090** — webhook event log + `HS_POLICY_STATUS` wiring.
**Dependencies.** B3's ingest interface. Build against staging first — staging is free and ungated by
contract.
**Demoable outcome.** Automated verification arriving per carrier as the matrix fills in; the manual
share of the book shrinks visibly.
**Deferred.** On-exchange `/v1/policy-status/*` — agent-scoped and alpha.

---

## Track C — Durability

### C1 — Annual re-certification automation

**Scope.** The SWBD brief already names the certification package — employer responsibilities guide,
onboarding and annual re-cert, scoped services agreement — as part of what the PEPM buys. AMS has the
machinery to *run* it automatically: the questionnaire system with per-instance public GUIDs,
recurring checklists, and notice tracking.

**Gate.** None beyond B4a for obligation tracking.
**Schema.** Likely none — reuses questionnaire + recurring checklist infrastructure.
**Demoable outcome.** Recurring revenue at near-zero marginal labour, sold as liability reduction
rather than as a feature.

### C2 — Full GA console and alert engine

**Scope.** Book view across all groups and employees with enrollment and coverage state; downline
visibility; **attribution reporting keyed on the NPN** HealthSherpa returns per policy; and the
**alert engine** — SEP window closing, application submitted but never effectuated, policy terminated
mid-year, renewal notice due in 90 days.

Per the capability map this is *the actual differentiator* and *the real operational value*. A5
delivers the pipeline half early; C2 is the book half, which needs live policies to point at.

**Gate.** Enough live cases to be worth looking at.
**Schema.** Mostly views and read models; **V091** if alert state or dismissal needs persisting.
**Demoable outcome.** The thing that makes SWBD's whole downline want the platform.

---

## What the pitch sounds like at each stage

| After | What you can say to Forrest |
|---|---|
| **A1** | "Your agents can quote ICHRA in any county today, on your brand, and I can see who's quoting what." |
| **A2** | "And we can answer 'will I lose my doctor' before the employee panics." |
| **A3** | "Give me a census and I'll tell you which of your employees are better off with the subsidy and which with ICHRA — and what the optimal class structure is. That's a design fee, not free work." |
| **A4** | "Send me your group book and I'll tell you which renewals you're about to lose and where ICHRA wins." |
| **A5** | "Here's your whole network's pipeline, by sub-agency and agent." |
| **B1–B3** | "And when you win them, I administer them — including the small ones nobody else will touch." |
| **C1–C2** | "And the compliance record and the exception alerts live here, so the book runs itself." |

---

## On durability — where the switching cost actually lives

A pitch built purely on sales tools has a failure mode: SSA becomes a free tool vendor to a GA, easily
replaced when someone ships a nicer calculator. The switching cost lives in four places, and only one
is a feature:

1. **SSA holds the book state and the compliance record.** Once notice obligations, substantiation and
   verification history live in AMS, leaving means abandoning the audit trail.
2. **The white label means SWBD's brand equity accrues to a system SSA runs.** `premiumpath.net`,
   per-agency sending identity, agency-scoped proposal overrides — Forrest's clients experience SWBD
   and SSA is invisible. A real asset for him and a real lock for you.
3. **AOR travels per application by NPN.** SSA is structurally not competing for the policy. This is
   the trust asset that makes a GA willing to route its downline through you at all — and it should be
   said out loud in the pitch rather than left implicit.
4. **The economics at the small end.** zizzl's $660/mo minimum on three lives is the wound. If
   automated verification makes sub-10-life groups profitable, "we'll take your small cases" opens
   Forrest's second opportunity stream — **his own framing**, not one you are inventing.

Which is the argument for not abandoning the administration layer: **sales tools win the large cases,
automation wins the small ones, and Forrest's book has both.** Lead with the sales layer to win the
partnership; let the admin layer land underneath while the relationship forms.

---

## New open items from the reframe (O22–O25)

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O22** | **SWBD book profile** — counties, group-size distribution, carriers, renewal-date distribution, and **producing-agent count** (already an open item in `swbd_premiumpath.md` as *the opportunity-sizing question*). | Forrest | Which markets to warm the rate cache for (A1); whether A4 is worth building; how to size the opportunity |
| **O23** | **Do plan objects carry provider networks, drug formularies and benefit summaries?** `healthsherpa.md`'s own highest-value untested item. | Free quoting API, key already held | **A2 entirely.** If it fails, A2 does not happen and several later assumptions need revisiting |
| **O24** | **Will SWBD share its group book**, and in what format? | Forrest | **A4 entirely** — and it is the partnership test as much as a technical gate |
| **O25** | **Carrier names: the two rules pull opposite ways.** `domain_and_compliance_rules.md` says keep carrier names off all SSA-drafted paper (endorsement / §4980D reclassification risk). The ERISA safe harbor *requires* a complete, neutral plan list, which necessarily names carriers. Proposed reconciliation: **carrier names may appear in neutral, complete, employee-facing market displays; they must not appear in SSA-drafted plan documents, notices, or program marketing.** Needs counsel confirmation and a clarification in that doc. | Counsel | A1, A2, A3, B5 — every display SSA builds |

## New decisions from the reframe (D21–D24)

| # | Decision | Recommendation | Depends on |
|---|---|---|---|
| **D21** | The **pre-sale design census** (age, ZIP, family tier, income band — no SSN) is a separate object from the **post-sale administrative census** (SSN, for the Summit handoff). | Adopt. Keeps quoting PII-free per D7 and keeps the entire sales stage outside the BAA question — which is what lets A3 ship years before O13 resolves | — |
| **D22** | Illustration access is **authenticated agent-facing first**; the existing per-agency public quote token (V071) becomes an optional prospect-facing front door in a later increment. | Adopt. Authenticated access gives **per-agent** attribution for free and adds no new unauthenticated surface — the public token is per-agency only. Note that the current whitelist is `/q/*`, `/proposal/`, `/apply/`; adding a public path means rate limiting and the T30/T31 lessons about public form surfaces. Reversible either way | — |
| **D23** | SWBD's group book lives in a **new `agency_book_group` table**, agency-scoped, with an optional FK to `Prospect` once an entry converts — rather than extending `Prospect` directly. | Adopt. These are pre-prospect book entries with a different lifecycle; conflating them muddles the sales pipeline | O24 |
| **D24** | Renewal-defense output goes to the **agent only**, never to the employer directly. | Adopt, non-negotiable. Agent market-conduct (steering, replacement) is the agent's bucket and licensure; SSA supplying analysis to a licensed agent is squarely in SSA's lane | — |

---

## Sequencing summary

> **Revised in Part 4.** Track A is unchanged; Track B gains a prerequisite phase (B0) and three new
> gates. See "Revised sequencing" at the end of Part 4.

| Phase | Gate | Gate 0-sensitive | Evidence delivered to SWBD |
|---|---|---|---|
| A0 — Unblock | none | no | — |
| **A1 — Rating illustration** | O3, O22 | **no** | **Agents quote live cases today** |
| A2 — Provider check | **O23** (terminal) | no | The objection answered |
| A3 — Design deliverables | O19 (soft) | no | Billable design work |
| **A4 — Renewal defense** | **O24** (partnership test) | no | **Cases he was about to lose** |
| A5 — Pipeline console | none | no | Network visibility |
| A6 — Design advisor | none | no | Agent self-service on rules |
| B1 — "+" catalog + proposal | O1, D18 | **yes** | Complete sales story |
| B2 — Census + Summit | none | no | Sold case runs |
| B3 — Verification ledger | **O18** | no | Recurring-revenue proof |
| B4a — Notice obligations | none | no | Obligation register |
| B4b — Notice reconciliation | O5 | no | Completeness assertion |
| B5 — Enrollment handoff | O12, O13, O14, O20, O2 | no | End-to-end enrollment |
| B6 — Card ingest | O9, O10, O6, D19 | no | Conditional |
| B7 — Policy Status | O12, O13, O15, O16, O2 | no | Automation grows per carrier |
| C1 — Re-certification | B4a | no | Recurring, near-zero labour |
| C2 — GA console + alerts | scale | no | The differentiator |

**Parallel and independent:** T35 (HIGH, ships alone), T29 (security), T38 (orphan registration), T36
(resolve at B2), the O21 counsel docket.

**Critical path.** **O22** + **O23** → A1/A2, and both are answerable this week. **O24** → A4, which is
the flagship. **O1** → B1 and everything customer-facing in Track B. **O18** → B3. **O12** → B5/B7.

Nothing on Track A's critical path depends on HealthSherpa granting anything.

---

---
---

# Part 4 — Revision following the Summit and Phase A document sync

Both previously-missing documents are now available. **Track A is unaffected — which strengthens the
partnership-first sequencing rather than weakening it.** Every material change lands in Track B, and
Track B got heavier.

## From `summit_plus_tier_discovery.md`

**S-11 — does Summit accept a participant *update* import, or adds only?** Open, Med priority, and the
doc itself notes it is *required for the two-pass export*. **This gates B2's central mechanism.** The
two-pass design — create participants at setup, update post-enrollment with carrier, plan, premium and
effective date — has no fallback in the current plan. If Summit accepts adds only, the second pass
becomes manual data entry per participant, which changes B2's labour profile and possibly its pricing.
→ **O26**.

**Card reversals are visible and unhandled.** The primary card report carries `PurchaseCount`,
`ReturnCount`, `PurchaseAmount`, `ReturnAmount` and `TypeName` alongside `TransactionAmount` —
confirmed from a JSON sample where `PurchaseCount` = 1 and `PurchaseAmount` = `TransactionAmount`. The
secondary report carries `ActionCode` and `Iso8583MessageTypeID`, which distinguish authorization from
settlement from reversal. The plan's verification chain — *amount matched within tolerance →
participant-month verified* — has **no reversal path**. A premium paid and later refunded would verify
a month in which coverage lapsed. → **D27**.

**O6's resolution path is confirmed.** The secondary report carries **both** `PostDate` and
`SwipeDate`, so post-vs-swipe is answerable by pulling the same transactions from both reports and
comparing — exactly the method proposed in Part 2. No new work to determine it.

**The mailing export is scheduled *by event type*.** So the `EventTypeID` set (S-10 = **O5**) gates not
only `notice_event_map` but the **export configuration itself**. B4a still stands free of it — AMS
creates obligations from its own events — but B4b cannot even be configured until O5 resolves. A
slightly earlier dependency than recorded.

**Two new low-priority items.** Summit may carry a **tobacco field on the participant record** (S-8, a
thirty-second check) → **O27**. And **Summit native invoicing** supports arbitrary PEPM per employer,
an enrolled-vs-eligible headcount basis, invoice branding, and export for revenue share (S-9) —
explicitly optional since billing goes through Wave, but it maps onto SWBD's remittance-with-per-group-
statements ask and is worth knowing before building anything bespoke → **O28**, deferred.

## From `phase_a_ichra_enrollment_portal.md`

This changes the plan more than expected. Four findings matter.

### 1. D5's "no change to the billing pipeline" is conditional — and the condition was never stated

`MonthlyBiller.logCoverageStatusForThisMonthCDH` (lines 373–406) — **the path covering HRA / MERP /
ICHRA / EBHRA / QSEHRA** — runs entirely off `Enrollment2.importEmployee → ImportEmployee`, a
**Summit-staging-only entity wiped monthly**. Separately, Premium Billing explicitly excludes negative
IDs: `MonthlyBiller.java:329-334`, `WHERE ee.employer.id = :id AND ee.id > 0`, live in both the legacy
pipeline and the Monthly Billing Launcher.

So an AMS-native employee — which is what census intake produces — gets **no `CoverageStatus`, no
`BillingGrid` row, and no bill.**

D5 therefore holds **only if** a "+" participant completes a full round trip: census → Summit creation
export → Summit administers the plan → the monthly Summit CDH enrollment import returns the
participant as a positive-ID employee with enrollment rows. That is consistent with the design's
premise that Summit adjudication is untouched, but it has consequences nobody wrote down:

- **The first "+" invoice cannot precede the first monthly Summit import cycle after setup.** There is
  a structural billing lag of up to one cycle.
- **The Summit handoff is load-bearing for revenue**, not merely for administration. D12 and the
  two-pass export sit on the billing critical path.
- **The "+" plan must be a Summit CDH benefit** the enrollment import recognises, or the CDH path
  never sees it.

→ **D25** and **O29**.

### 2. The designed happy path runs straight into a silent data-loss bug

`Updater.mergeNegativeToPositiveEmployees` (lines 622–730) calls `em.remove()` on a negative-ID
employee and reparents its links onto a positive-ID employee when name or email match at the same
employer.

That is *precisely* the round trip in (1): census creates a native employee; the Summit import later
returns the same person with a positive ID; the sweep deletes the negative row. The Phase A doc names
the consequence directly — for an ICHRA enrollee who later appears in a Summit import, this **silently
destroys a record carrying enrollment and attestation history.**

`plus_participant` is designed to link to `Employee`. `participant_coverage_month` hangs off
`plus_participant`. **The verification ledger's anchor is deletable by a routine monthly sweep, on the
happy path, with no error.**

**T33 is therefore not backlog-MED for this work — it is a hard prerequisite.** Same for T32 (three ad
hoc `MIN(id)-1` allocators, no shared helper, race risk; the one correct implementation,
`HsaBillingHelper.getNextEeId`, has zero call sites). → new phase **B0**, and **D26**.

### 3. Gate 0's realistic best case is Branch A-minus, not Branch A

Phase A Q4 establishes more than "no seeder creates the rows":

- Even the dead-code ICHRA/EBHRA `PlanType`s point at the **generic HRA `ServiceItem` (id 5)**. A
  Setup born from an ICHRA sale today gets the plain HRA checklist with **zero ICHRA compliance
  steps**.
- **No QSEHRA `PlanType` exists at all.** ICHRA/EBHRA exist in dead code and need wiring — a small
  fix. **QSEHRA needs new code.**
- No `RequiredTaskList` / `TaskSequenceTable` / `Task` is ICHRA/EBHRA/QSEHRA-specific.
- **T9 would silently drop a "+" selection in the internal Manual Setup path.** `GenerateProp25`
  hardcodes `q1`–`q8` against LOS ids 5–10 and ServiceItem ids 11–19 with literal `.equals("1")`
  checks and never reads `Proposal.getLosList()`. Scoped to `GenerateProp25` only.

Gate 0 can only discover manually-entered rows. **Even a favourable result leaves the ServiceItem,
PlanType, task-sequence and checklist gaps intact**, because hand-entering an `LOS` row would not have
created any of them. **Plan for A-minus/B; treat Branch A as unlikely.** And either fix T9 or forbid
Manual Setup for "+" cases — an inline note in B1, not a separate project.

### 4. Employee-facing surfaces are harder than assumed, and A2 should sidestep it

Phase A Q2: `AmsDataLocal.intializeLocalData` **unconditionally** copies the entire installation's
open-activity and renewal-employer lists into every session, all reachable via public getters with no
scoping; `AmsDataGlobal` is ServletContext-wide and unfiltered. The doc's framing: *an individual
employee would be the first genuinely low-trust session in AMS.*

Phase A Q5: `Employer` has **no public-safe identifier of any kind.** `altId` is Summit's own employer
ID, used to build internal authenticated deep links, never validated as public-facing — and the
`1494286` guard fix was a null-safety UX fix, **not** security hardening. An employer-scoped public
surface needs a newly minted public-safe identifier plus a migration, `AmsDataGlobal` cache logic and a
`login.java` branch — **sized like the original V068 effort.**

Also: **no dedicated census/roster upload servlet exists.** The Interactive Import Wizard is
PSP-Admin/BPO-Admin only and allocates positive MAX+1 IDs.

**Consequences.** A1 is unaffected — agents are an already-trusted session type at agency granularity,
and the illustration creates no `Employee` rows. But **A2 should be stateless**: physician names plus
ZIP, looked up live, **nothing persisted**. That clears both the low-trust-session problem and FINDING
2's rule against PHI-bearing public forms at once. → **D28**. And B5's employer-scoped surface is
V068-sized, not a config addition.

### One finding that reduces scope

`ApplyForProposal` and `CreateSetup25` are confirmed **fully dynamic** and per-LOS driven
(`ApplyForProposal.java:444-448` via `ActivityDAO.addModule()`). The customer-facing sales-to-setup
path needs no work to carry a new LOS — only the internal Manual Setup tool does.

> ⚠️ **Correction, session 27 (S27-D), 2026-09-07.** The setup checklist keys on `ServiceItem`, not
> LOS. `CreateSetup25` populates `ApplicationModule` from `LOS.serviceItem` **and**
> `Enhancement.serviceItem` (`ApplyForProposal.java:451-457` is an Enhancement loop immediately below
> the LOS loop this claim cites), and `AddSetupModule25` is a third door where a PSP user attaches a
> `ServiceItem` to a live Setup by hand. Tasks resolve via
> `RequiredTaskList WHERE serviceItem.id = :id`. A new LOS does flow through with no code change; LOS
> is one input to a ServiceItem-keyed mechanism, not the mechanism.

## New phase B0 — participant-model prerequisites

> **DELETED BY PART 5.** Every "+" employee now arrives with a positive Summit `Participant_ID`, so
> the negative-ID convention — and this phase's entire reason for existing — is off the route. Left
> here for provenance. T33/T32/T34 revert to independent tech debt.

**Scope.** T33 merge-sweep exemption or avoidance; T32 allocator consolidation onto the one correct
implementation; the "+" participant ID convention (**D26**); T34 (`PersonDAO.assignToGenericEmployer`
persists without `setId()`). Optionally T36 here rather than in B2.

**Gate.** None — code-only and read-analysable.
**Schema.** Possibly none; a guard column or exemption flag if the chosen approach needs one.
**Dependencies.** None. **Must precede B2.**
**Demoable outcome.** None. It prevents silent destruction of the audit trail the whole tier is sold
on.
**Deferred.** Nothing.

## New open items

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O26** | **Does Summit accept a participant *update* import, or adds only?** (S-11) | Summit / DataPath | **B2's two-pass export.** No fallback designed; adds-only makes the second pass manual per participant |
| **O27** | **Tobacco on the Summit participant record** (S-8, thirty-second check) — pairs with O19, which asks whether affordability needs it at all | Summit UI | Whether tobacco has a source once O19 says it is needed |
| **O28** | **Summit native invoicing** — arbitrary PEPM, enrolled vs eligible basis, branding, export for revenue share (S-9) | Summit | Nothing. Deferred — billing goes through Wave. Relevant only to the SWBD remittance ask |
| **O29** | **Does the "+" plan appear in Summit's monthly CDH enrollment export** as a benefit the import path recognises? | Summit config + one import cycle | **Whether "+" participants can be billed at all** (see D25) |

Plus the four questions Phase A raised: the billing-rail question (now **D25**), role 4 versus a fresh
role id, the employer public identifier, and appetite to remediate the PHI-adjacent surfaces
independently (T30 / T31).

## New decisions

| # | Decision | Recommendation | Depends on |
|---|---|---|---|
| **D25** | **"+" billing basis.** D5 holds only via a full Summit round trip; the first "+" invoice can lag setup by up to one import cycle. | Adopt the round trip rather than building a parallel headcount source — it preserves D5 and adds no billing-pipeline change. Price and communicate the one-cycle lag deliberately | O29 |
| **D26** | **"+" participant ID convention**, and exemption from the merge sweep. | Prefer avoiding the negative-ID convention for "+" participants entirely, so `mergeNegativeToPositiveEmployees` never sees them — narrower than adding an exemption branch to a 100-line sweep. Decide in B0 | — |
| **D27** | **Card reversals un-verify.** A `ReturnCount` / `ReturnAmount` row, or a reversal `Iso8583MessageTypeID`, must reverse or block the month's verification. | Adopt. Net-of-returns matching, with reversals routed to the exception queue rather than silently flipping a verified month | — |
| **D28** | **A2 is stateless** — physician names plus ZIP, live lookup, nothing persisted. | Adopt. Sidesteps the first-low-trust-session problem and FINDING 2's public-form rule together | — |

## Revised sequencing

| Phase | Change |
|---|---|
| **A1–A6** | **Unaffected.** A2 adds **D28** (stateless) |
| **B0** *(new)* | T33 + T32 + T34 + **D26**. Code-only, no gate. **Must precede B2** |
| **B1** | Larger — plan for **A-minus/B**. Adds ServiceItem, PlanType (QSEHRA net-new), task sequence, ICHRA checklist. Plus **T9** or a Manual-Setup prohibition for "+" |
| **B2** | Gains **O26** (two-pass gate), a net-new census upload servlet, and B0 as a prerequisite |
| **B3** | Gains **D25** / **O29** — the ledger's `Employee` anchor and the billing round trip |
| **B4** | 4b's *export configuration* also waits on O5, not only `notice_event_map` |
| **B5** | Employer-scoped public surface is **V068-sized**; needs a new public-safe `Employer` identifier |
| **B6** | Gains **D27** (reversals) |
| **B7** | **Needs its own Phase A** — the Phase A doc states no investigation has been done on webhook ingestion |
| **C1–C2** | Unaffected |

**The headline: nothing on Track A moved.** Track B gained a prerequisite phase and three new gates,
which makes leading with the sales layer more clearly right, not less.

Migration placeholders shift by whatever B0 needs; still allocate in **commit order**.

---
---

---
---

# Part 5 — Census and participant-loading architecture

## The settled model

```
Employer census
      ↓
AMS intake utility          ← client-facing; raw SSN transits here; ssn_hash minted here
      ↓
plus_census_stage           ← cleared after export (D11)
      ↓
generated Summit import file
      ↓
Summit creates participants ← every participant gets a positive Participant_ID
      ↓
J2 / J3 export
      ↓
AMS Employee rows + plus_participant population
```

**AMS is the system of record for intake. Summit is the system of record for participants.** No
"+" employee is ever created natively in AMS.

## What this resolves

**Phase B0 is deleted.** Its entire purpose was T33, T32 and D26. With every "+" employee arriving on
a positive Summit `Participant_ID`:

- `mergeNegativeToPositiveEmployees` never sees a "+" participant. The silent data-loss path is not on
  the route. **T33 reverts to independent MED tech debt** — it still threatens HSA-only employees and
  `ensurePrimaryContactEmployee`, but not this work.
- **T32's allocator race is irrelevant here** — no allocation happens; `Employee.id` *is* Summit's ID.
- Premium Billing's `ee.id > 0` exclusion stops being an obstacle.

**D26 resolves by construction** — the negative-ID convention is avoided entirely, which is what the
Part 4 recommendation asked for, achieved without touching the sweep.

**D25 is reframed, not merely resolved.** Census → Summit → J2/J3 is not a workaround for the billing
pipeline; it is AMS's existing employee-loading pattern. And `SummitImportWizard` is on-demand and
file-driven, so the J2/J3 import runs immediately after the Summit load — **employee creation does not
wait for the monthly cycle.** The lag concern narrows strictly to enrollment appearing in the CDH path
(**O29**).

**D7, D10 and D11 stand as written.** Raw SSN transits `plus_census_stage` at intake and is cleared
after export; the `ssn_hash` is minted there.

**T35 is fully decoupled again.** Because the hash is minted at intake, J3's SSN is redundant, so T35
can **drop** the columns rather than null-at-end-of-PROMOTE. It ships alone, as originally logged.
*(The reasoning here is superseded by Part 6 — T35 never had a "+"-tier dependency at all. The
conclusion is unchanged.)*

**J3 field promotion becomes the central mechanism**, not an optimization. `DOB`, `HireDate`,
`EffectiveDate`, `TerminationDate` and `DivisionName` are how `plus_participant` gets populated at all.

## New decisions

### D29 — census intake surface

A client-facing census upload is **the highest-PHI surface in the tier**, and both existing
client-facing rails are what the security findings warn against: FINDING 2's rule against PHI-bearing
forms on the `ApplicationField` / `QuestionnaireField` GUID pattern, and FINDING 3's month-pivoting on
`EmployerBillingDetail?uid=`. Phase A adds: no employer-scoped role, no public-safe `Employer`
identifier, `AmsDataLocal` leaking installation-wide.

| Option | Shape | Cost | Verdict |
|---|---|---|---|
| **1 — PSP-staff intake** | Employer sends the census; SSA staff upload it inside AMS | No new public surface, no employer-identifier work, works with today's roles, BAA-clean | **Recommended for launch.** Manual, and entirely adequate at small headcounts |
| **2 — one-time-GUID upload** | `User.tempGuid` / `guidExpiration` / `guidUsed`, the mechanism Phase A named as the closer analogue. Single-use, short expiry | Modest | **Increment two.** Meaningfully different from FINDING 2 — a file upload, not a config-driven field form, so the "any admin can add a field labeled SSN" mode does not exist |
| **3 — authenticated employer portal** | `EmployerScope` / `EmployerScopeResolver`, a wired participant role, a public-safe `Employer` identifier, a session class that is not `AmsDataLocal` | **V068-sized** | Only if employer self-service becomes a real requirement |

### D30 — the census file never goes through the standard upload path

Wasabi keys are `{slugified-PSP-name}/{uuid.ext}` — **PSP-scoped only**, no employer or activity
scoping. Retrieval is by presigned GET URL. **Most upload paths never delete the S3 object when the
referencing record is removed.** An SSN-bearing census file uploaded through `StorageDAO` would sit in
object storage indefinitely, retrievable by presigned URL, unscoped.

**Rule: parse in memory → stage to `plus_census_stage` → never persist the raw file.** The *generated*
Summit import file follows the same rule — stream it to the browser, do not store it.

### D31 — the intake utility is permanent, and needs a single-participant mode

If J3's SSN columns are dropped (T35) and a mid-year new hire is added **directly in Summit**, that
participant arrives via J2/J3 with **no AMS-side `ssn_hash`** — and silently falls out of the
mailing-export join, which is the notice-reconciliation mechanism.

**Every participant add must route through AMS's utility.** That means single-participant mode, not
bulk-only, and it makes the census tool the ongoing participant-maintenance tool rather than a setup
step. That aligns with the mid-year-new-hire SEP tracking the scope doc already calls for. **This is a
process rule, not an assumption** — it needs stating in the runbook, because the failure is silent.

## Open items

**O26 narrows.** Pass one is now a file AMS generates, not an API call. The question becomes: **does
Summit need the post-enrollment carrier, plan, premium and effective date at all?** If it needs them
only for reimbursement adjudication against expected premium — and AMS holds them for verification
regardless — the second pass may not exist, and S-11 stops mattering. If Summit does need them and
accepts adds only, there is no alternative path, because AMS is not the system of record for
participants.

**O29 unchanged.** J2/J3 create the `Employee`; they do not create enrollment. Billability still turns
on the "+" benefit appearing in Summit's CDH enrollment export.

**O30 (new) — does the census carry all eligibles, or only electing participants?** D5 requires both
enrolled and eligible counts, and only Summit participants get J2/J3 rows. If only electing
participants go to Summit, eligibles have no `Participant_ID`, no J2/J3 row, and nothing to hold them —
which reintroduces exactly the native-employee problem this architecture removes.
**Recommendation: load all eligibles into Summit as participants regardless of election**, since a
Summit participant record does not require an active benefit. Needs confirming.

## Revised B2 — census, participant model, Summit handoff

**Scope.** The AMS intake utility (D29 Option 1 at launch, with single-participant mode per D31);
`plus_census_stage` with in-memory parsing per D30; `ssn_hash` minted at intake; the Summit import file
generator; **J2/J3 promotion of the staged fields**; `plus_participant` population.

**Gate.** **O26** (whether a second pass exists at all), **O30** (eligibles vs electing).
**Schema.** `plus_census_stage` + `plus_participant`; `Employee` field additions for the promoted J3
fields.
**Dependencies.** B1. **No B0.**

**Risk worth naming.** Extending the J2/J3 import to promote staged fields means changing
`SummitImportService` / `SummitImportWizard` — **live production code on the active import path**, and
the same code **T36**'s divergent `ParticipantStatusId` / `EmploymentStatusID` mapping bug lives in.
That is materially higher-risk than adding net-new tables, and it **wants its own Phase A** before any
implementation prompt is written.

## Revised sequencing

| Phase | Status after Part 5 |
|---|---|
| A0–A6 | Unchanged |
| ~~B0~~ | **Deleted** |
| B1 | Unchanged from Part 4 (plan for A-minus/B; T9 or a Manual-Setup prohibition) |
| **B2** | Rewritten above. Gains the intake utility and D29–D31; loses the negative-ID problem; **needs its own Phase A** for the import-path changes |
| B3 | **D25 / D26 resolved.** Ledger anchors to a positive-ID Summit `Employee` |
| B4–B7 | Unchanged from Part 4 |
| C1–C2 | Unchanged |

**Independent, unblocked:** T35 (drop, ships alone), T29, T38, T33/T32/T34 (reverted to ordinary tech
debt), the O21 counsel docket.

---
---

---
---

# Part 6 — Notice automation, census intake, and the notional COBRA mechanism

## Corrections to earlier parts

**T35 never had a "+"-tier dependency.** It concerns the **legacy** staging pipeline —
`ImportEmployeeAlt.ssn` landing in `import3employeealt` via `Importer` / `Updater`. Dropping those
columns changes what AMS *stores*; it does not change what the J3 **file** contains, and the active
load path is `SummitImportWizard`, a different code path entirely. Part 5's drop-versus-null tradeoff
table is **void**. **T35 drops the columns and ships alone**, as originally logged.

**O5 is not DataPath-gated.** It has been carried at **High** priority, blocked on DataPath supplying
the `EventTypeID` enumeration. AMS does not need the enumeration — it needs the IDs for **the events
its own workaround generates**, and those are observable: perform the status change, read the mailing
export. Same method that worked for merge-token discovery. **O5 becomes a byproduct of the
notice-automation test** and stops gating B4b. The full enumeration still matters for notice types not
yet built, but it is no longer on the critical path.

## D31 — settled: Summit-first for ongoing adds

**All current administrative clients work in Summit.** Employers enter new hires there, and that is
not expected to change.

AMS-first for a mid-year hire would mean employer → AMS → generated file → someone loads it into
Summit → export → import back: **three added steps that remove zero Summit steps**, because the
notice vehicle is in Summit and the coverage entry has to happen there regardless. Intake cannot move
the vehicle.

**Settled:**
- **Ongoing adds — Summit-first.** No AMS pre-entry, no process mandate.
- **Setup census — AMS-first**, where D29's column-mapping validation pays for itself against a full
  roster and a rejected file is much cheaper than a bad Summit load.

**The exception-queue backstop proposed in Part 5 is not required.** Its purpose was to catch a
silently missed SEP window on a Summit-first add. That risk does not exist here: an employer entering
a new hire triggers a COBRA-initial-notice event, which is captured by a **scheduled export that is
already running** — effectively a new-hire report. The trigger exists and is reliable.

**What is missing is not awareness but tracking.** Nothing records that a notice was owed, that the
Summit entry was completed, or that the letter actually mailed. That is the real gap, and it is
B4a's job rather than an intake-order question.

## D32 — SSN is sourced from Summit feeds, not from the census

SSN has **no tactical purpose** in AMS today. It is carried through import into Summit for accuracy;
no functionality depends on it, and no customer requirement is known to need it in Summit.

D10's hash exists solely because the mailing export carries no `Participant_ID`. But **the mailing
export carries SSN itself**, and so does J3. So the hash can be computed at the parse boundary of
either Summit feed — HMAC the value as the row is read, store the hash, discard the raw value. No new
SSN residency anywhere.

**Consequences:**
- A Summit-first participant acquires a hash automatically on the next J2/J3 import. Nothing needs to
  route through AMS to make correlation work.
- **D11 may become moot.** If the census carries no SSN, no raw SSN transits `plus_census_stage` and
  there is nothing to clear.
- **D30 softens.** The census becomes name, DOB, ZIP, tier and hire date — no longer the highest-PHI
  surface in the tier. D30's rule (parse in memory, never persist the raw file) still stands as good
  practice, but it stops being load-bearing.
- **D29's GUID option becomes easier to justify** as the file's sensitivity drops.

**Gated on O31** — if Summit's participant import *requires* SSN, the setup census must still carry it
for pass-through. That is a weaker requirement than correlation, with shorter residency, but it keeps
D11 and D30 live.

## D33 — the notional COBRA benefit is a documented mechanism, not a workaround

Summit has no ICHRA/QSEHRA notice event type. The production mechanism is a **custom COBRA-type
benefit** created alongside the real ICHRA benefit; a status change against it fires Summit's
tracked-letter vehicle with the correct content and produces a mailing record.

Established properties:

| Property | Consequence |
|---|---|
| Custom COBRA-type benefits can be created | The mechanism is configurable per employer |
| COBRA-type enrollments **flag eligibility counts** | **The notional benefit is the eligibility marker**, not merely a letter vehicle |
| They do **not** cause double counts | AMS's headcount to Wave is unaffected |
| DataPath bills only on status change to **qualified beneficiary**; these status changes alter rates while leaving participants **active employees** | **The mechanism is free.** Cost is notice mailing only — an input to D2's bundled PEPM |
| ICHRA participants see nothing alarming from the notional item | No participant-experience problem |

**Because it is the eligibility marker, every eligible employee needs one — including decliners.**
Only enrollees additionally receive the real ICHRA benefit. The decliner path needs its own handling:
a notional record, with a different status change or none.

**Two things to record now rather than discover later.** First, the eligible count's source is this
mechanism — so if DataPath ever ships a real ICHRA notice event type, retiring the notional benefit
would also change where the eligible count comes from. Second, **naming**: if custom COBRA-type
benefits allow arbitrary names, name it something self-evidently administrative ("ICHRA Notice
Administration") rather than anything COBRA-shaped. Naming solves employer confusion once; training
solves it every time — which matters at fifty groups across SWBD's downline, not at three.

**Strategic note.** This is a concrete product ask to carry into the DataPath conversation:
*Summit has no ICHRA/QSEHRA notice event type, so administrators construct notional COBRA benefits to
borrow the status-change letter vehicle.* A specific gap, a proven workaround, from someone
administering real cases — a stronger opening than an abstract partnership pitch, and something
Charles can take internally.

## D34 — any letter-triggering generated import requires preview-and-confirm

Letters generate from imports the same way they do from manual entry. A malformed AMS-generated file
would therefore **mail a batch of wrong letters to real participants** — postage spent, and a
compliance artifact that cannot be recalled.

**Rule:** any AMS-generated Summit import capable of triggering letters must render the rows, show
which letter each will trigger, and require an explicit confirmation. **Never a blind load.** Cheap to
build; the failure it prevents is expensive and irreversible.

## D18 — deferred until Gate 0 runs

D18 asks how reference **rows** reach environments — not schema, which is always a versioned
migration. Three mechanisms exist: migration `INSERT` (all environments, automatic, idempotent),
`DatabaseInitializer` (new PSPs only), and `D-NN` items (manual, per environment).

Two reasons the `constant` prohibition probably extends to `los` / `templatepurpose` / `plantype`:
**PSP scoping** (`LOS` carries `psp_id`; which PSP does a migration target on master/demo/BPO?) and
**ID determinism** — T9's `GenerateProp25` hardcodes LOS ids 5–10 and ServiceItem ids 11–19, so
AUTO_INCREMENT inserts land on different IDs per environment and break silently and differently,
while explicit IDs risk colliding with hand-entered rows.

**D18 is therefore partly gated on O1.** A uniformly empty Gate 0 result makes a migration with
explicit IDs viable and cleaner; an uneven result — production holding a hand-entered ICHRA LOS,
demo holding nothing — cannot be expressed in a single migration and forces D-NN per environment.

**Decision: defer until Gate 0 runs**, and plan for the hybrid Rule 7 prescribes —
`DatabaseInitializer` for future PSPs (rewriting the commented 476–510 block), `D-NN` items for
existing live environments. That shape survives an uneven result.

## D29 — settled

**Options 1 and 2 both; the one-time GUID preferred.** Option 3 (authenticated employer portal) is
parked, not ruled out — but no long-term need for it is currently foreseen.

**Added requirement:** AMS presents the uploaded file to the **PSP** for column-header mapping against
expected inputs, and **declines any import not meeting the minimum field set.**

This has a structural benefit worth stating: **the public surface stays dumb.** The employer's GUID
upload is an opaque file drop with no intelligence in it; all mapping, validation and rejection logic
sits behind authentication. That is why FINDING 2's rule against PHI-bearing public forms does not
bite — this is a file handoff, not a config-driven field form.

**Precedent exists but needs a deliberate choice.** `SummitImportWizard`, `UniversalImport` and
`InteractiveImport` are all column-name-driven and tolerant of reordered or unknown columns — but the
four import paths carry divergent field-mapping logic for the same source fields, and no single path
is authoritative. **Reuse the pattern; decide explicitly which implementation to extend.** A Phase A
question, not an assumption.

**The minimum field set is dictated by Summit, not by AMS** — the generated file must satisfy Summit's
participant import. See **O31**.

## O30 — settled: all eligibles

All eligible employees are loaded into Summit as participants regardless of election.

This separates D5's two counts at the source rather than by inference: **eligible** comes from the
participant roster via the notional benefit (D33); **enrolled** comes from the real ICHRA benefit and
the CDH enrollment export. Different actions, different feeds, no derivation. It is also how the
process already works in production.

## A4 splits — the book ask has to be earned

"Send me your book" is a large request to make of a partner nothing has yet been proven to. **"Send me
three groups renewing next quarter"** is an easy yes, and it self-selects for cases where the
analysis matters.

**A4a — sample conversion analysis.** Three to five renewing groups, entered by hand. Current group
premium in, ICHRA comparison out. **Gate: a handful of groups from Forrest** — a much smaller ask than
O24. A thin increment over A3, which already produces subsidy segmentation, affordability thresholds
and class structure from a census; A4a adds current group cost as an input. Little new machinery,
likely no new schema.

**A4b — book import and renewal radar.** `agency_book_group`, the scheduled radar, the pre-analyzed
opportunity queue. **Gate: O24**, now warranted by A4a having proven the analysis is worth pointing at
the whole book.

The partnership-test framing moves to A4b, where the ask is earned rather than cold. **D24** — output
to the agent, never the employer — applies to both.

## B4a moves earlier

The notice obligation register is now **the shortest path to fixing a problem that exists today**,
rather than one that appears after a sale — and **both its input feeds already exist and are already
scheduled.**

**Flow:** new-hire event lands → AMS creates a `notice_obligation` (`PENDING`) → worklist shows *three
new hires need the ICHRA notice sequence* → the Summit entry is done → the mailing export returns the
mailed row → obligation reconciles to `MAILED`.

**What B4a-early needs:** ingest the notice/mailing export, correlate the employer (already in AMS via
normal Summit import), identify the participant (hash join per D32), a `notice_obligation` table, and
a worklist.

**What it does not need:** census intake, `plus_census_stage`, the Summit file generator, or the
J2/J3 promotion changes — which is the risky part of B2, since it touches live production import
code. The only soft dependency is knowing which employers are "+" employers; a flag or a manual list
covers that without waiting for B1.

If the notice-automation test returns **yes** on import-driven letters, the worklist becomes *review
and load* rather than *go do this manually*, and the labour per participant falls. If **no**, B4a
still closes the tracking gap — the labour does not fall, but the silent-miss risk does.

## New open items

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O31** | **What does Summit's participant import require, and what does it accept optionally?** | Summit / DataPath | D29's minimum-field validation rules, **and D32** — if SSN is required, the census must carry it for pass-through and D11/D30 stay live |
| **O32** | **Does the active J2/J3 path (`SummitImportService`) parse J3's SSN at read, or skip it?** `summit_import_design.md` says the field is "billing-only and ignored by sync" — ignored-at-read and read-then-discarded are different, and only one makes hashing free | Code read (B2 Phase A) | Whether D32's hash-at-parse is free or new code |
| **O33** | **Does the notional benefit generate anything else on a schedule** — election-deadline reminders, premium coupons or invoices, expiration-of-rights notices? | Summit config inspection + one cycle | Nothing structurally, but it is **silent, mailed, and goes to the employee**. Check before fifty groups exist, not after |

**O26 extends into three questions for DataPath**, worth asking together: does the participant import
accept **updates**; do **coverage-add and status-change imports** exist for custom COBRA-type
benefits; and — the one everything rests on — **do imported status changes trigger letter
generation**?

## Discovery protocol

`docs/analysis/summit_notice_automation_discovery.md` (created 2026-07-30, not yet run) is the test
plan for the above. Seven phases, N-numbered findings. Phases 1–3 are the minimum useful run: a
UI-driven control letter, an import-driven coverage add, and an import-driven status change. Its
Phase 5 output is the `notice_event_map` source data and closes **O5** for "+"-tier purposes.

## Revised sequencing

| Phase | Status after Part 6 |
|---|---|
| A0–A3 | Unchanged |
| **A4a** *(new)* | Sample conversion analysis. Gate: a few renewing groups from Forrest |
| **A4b** | Book import + renewal radar. Gate: **O24**, now earned by A4a |
| A5–A6 | Unchanged |
| **B4a** | **Moved earlier.** Both feeds exist; no dependency on B2's risky import changes. The shortest path to fixing current pain |
| B1 | **D18 deferred to Gate 0.** Otherwise unchanged |
| B2 | **D29 settled** (GUID + PSP column mapping); **D31 settled** (setup census only, no ongoing intake); **D32** may remove SSN from the census entirely. Still needs its own Phase A |
| B3 | Unchanged |
| B4b | **O5 no longer DataPath-gated** — closed by the discovery protocol |
| B5–B7 | Unchanged |
| C1–C2 | Unchanged |

**Independent and unblocked:** T35 (drop, ships alone), T29, T38, T33/T32/T34 (ordinary tech debt),
the O21 counsel docket, and the notice-automation discovery protocol.

---
---

---
---

# Part 7 — Summit Data Exchange, correlation keys, and the consolidated build list

## Source note

Findings below come from Summit's own AI help section, cross-checked against the Settings, Employer
and Processing guides and against prior understanding. Treated as **reliable pending
tenant-verification** — the behaviour described is consistent, but instance-specific configuration
still needs confirming in the SSA tenant before anything is built against it.

## Corrections to Part 6-era assumptions

**"+" benefit rates are tier-grain, not rate-cell grain.** The J7 export carries `TierAge`, `Gender`,
`Smoker` and `Amount`, which is Summit's generic Premium Billing rate structure — built for COBRA,
where rates genuinely are banded that way. For a "+" benefit these are **employer-determined
reimbursement amounts per tier**, not market premiums: a handful of rows, not a matrix.

**`Smoker` is therefore not a tobacco source.** O19 and O27 are unaffected and tobacco still has no
source from this direction. ⚖️ Neither gender nor tobacco can lawfully drive an ICHRA contribution
amount, so those columns stay unused. **`TierAge` may see use** — age-banded ICHRA contributions are
permitted within limits, so the column is available rather than needing a workaround.

**The renewal notice therefore carries a contribution change, not a premium change.** Still the right
content — the reimbursement amount is what the employer changed and what the employee needs told.

## D35 — Participant Custom ID may retire the SSN hash entirely

The census import accepts a **Participant Custom ID**, settable at import and editable later. That is
a partner-supplied participant key — the same role `EmployerCustomID` plays at employer level (D12),
one grain down.

D10's hash exists for exactly one reason: the mailing export carries no participant ID. If AMS mints
and sets a Participant Custom ID at census import, correlation becomes **direct**.

**If it round-trips, this collapses:** D10 (the hash), D11 (raw-SSN clearing in `plus_census_stage`),
D32 (hash-at-parse from Summit feeds), O32 (does `SummitImportService` parse J3's SSN), and the
hash-vs-name-match decision flagged as the highest-leverage item in the Part 6 question list. No HMAC,
no key management, no parse-boundary hashing, and **no reason to touch `SummitImportService` for
correlation**.

Second-order benefit: AMS gains a stable participant key **it controls**, rather than depending on
Summit's internal `Participant_ID` as sole anchor.

**Gated on O34** — does Participant Custom ID appear in the **mailing export** and in J2/J3? If it is
write-only, the hash returns. Observable, not a DataPath question.

## D36 — read benefit plan IDs from J7; do not assert them

`ImportPlanID` is **user-defined and unique within an employer only**, so a convention could be fixed
across all "+" employers. But the J7 benefit export carries `ImportPlanID` keyed to employer — so AMS
can **read reality instead of asserting it**.

Confirmed J7 header:

```
TPA, EmployerOrganizationID, OrganizationID, EmployerID, Employer, BenefitName,
PBBenefitID, Type, RemitTo, PlanTypeID, PBType, PBTypeID, BenefitID, ImportPlanID,
EffectiveDate, Carrier, CarrierGroupNumber, LastDayofCoverage, Fee, PlanYearID,
StartDate, EndDate, TierName, TierID, TierAge, Gender, Smoker, Amount,
DivisionName, DivisionID, DivisionCustomID, OpenEnrollmentStartDate,
OpenEnrollmentEndDate, TerminationDate, StandardAdministrationFeeTypeID,
StandardAdministrationFee
```

**Decision: use both.** A naming convention so setup is predictable, **and** J7 discovery so AMS knows
the truth regardless. Where they disagree, raise a flag rather than fail silently. Discovery beats
convention alone because manual setup is exactly where discipline fails — a typo produces an import
that targets nothing, or the wrong plan.

**Two properties this buys:** it is **self-correcting** (a plan renamed or re-IDed in Summit is picked
up on the next benefit import — no drift, no stale mapping), and it enables a **setup-completeness
check** — AMS can assert that a "+" employer has both required benefits before generating any import
file. Nobody has that today.

**Parsing note:** J7 is rate-grain, so one benefit yields several rows. AMS needs a benefit table and
a tier/rate table, not one flat import.

**Bears on classes:** `DivisionName` / `DivisionID` / `DivisionCustomID` appear at rate level,
suggesting rates can be **division-scoped**. Division-level contribution amounts are precisely what
ICHRA classes are — which would answer **O7** affirmatively and move `DivisionName` from low-priority
to load-bearing. → **O38**.

**Compliance watch:** `Carrier` and `CarrierGroupNumber` sit in the benefit data. If letters merge from
Summit benefit data, a carrier name could surface on a notice — which runs into the standing rule
about carrier names on SSA-drafted paper (**O25**). Worth checking what the letter templates actually
pull; this is the kind of thing that appears without anyone deciding it should.

**Gap:** no PCOR Reportable field, so that compliance setting stays a manual checklist item AMS cannot
verify.

## D37 — Summit Data Exchange: scheduled SFTP, both directions

Data Exchange supports scheduled SFTP in both directions — AMS pushes import files and Summit pulls on
a daily / weekly / monthly schedule at a configurable time; Summit pushes exports and AMS pulls.

**This converts every "generate a file, someone loads it" step in the plan into an unattended loop**,
and changes AMS's contribution from a worklist into an actual pipeline.

**The catch:** AMS has **no FTP/SFTP client or dependency**, and no inbound ingest that is not a user
upload. This is net-new infrastructure — client library, credential storage, a scheduled job on the
`InstallationHealthScheduler` pattern, file generators and parsers. It was parked in the backlog as
*automated SFTP delivery of Summit exports*; it is now load-bearing rather than a convenience.

**Two sub-decisions:**

| | Options | Recommendation |
|---|---|---|
| **Network** | DataPath MOVEit, or external network (SSA hosts an endpoint DataPath connects into) | **MOVEit** — SSA pushes and pulls, no inbound exposure on SSA infrastructure. Note the folder requires a MOVEit administrator to set up: a lead-time item |
| **Encryption at rest** | AES / RSA / Triple DES / Rijndael, with TPA-key and DP-key exchange, on top of SFTP transport | **Take it**, given PHI in both directions. Adds key management, and credentials land in the same unstandardised place as everything else — `ssa.properties` vs `constant` table vs DB-first-with-fallback, with no documented standard for new integrations |

**Build this early and independently.** It benefits **every existing administrative client
immediately**, not only the "+" tier — the same argument that moved B4a forward. Designated **P1**
below.

## D34 revised — Process Approvals may supply preview-and-confirm natively

Part 6's D34 required AMS to build a preview-and-confirm step before any letter-triggering import.
Summit appears to provide this already: import-generated events land in **Process Approvals** with
status Auto-Approved / Awaiting Approval / Manually Approved / Declined, and **clicking Preview
displays the associated document** before anything mails. Custom events behave identically. Approve,
decline, or postpone.

**So D34 becomes a Summit configuration choice** — whether these events require manual approval —
rather than AMS code. Better placed than an AMS-side preview, because it sits at the last point before
mailing.

**Related:** Summit compares imports against previously-imported files and holds suspected duplicates
for accept/reject. **File-level idempotency is native**, so the discovery protocol's N-4c narrows to
row-level behaviour within an accepted file.

## O26 and O31 — largely resolved from documentation

| Question | Status |
|---|---|
| Coverage assignment by import | **Exists** for Premium Billing. Needs Employer ID/Custom ID, Participant ID/Custom ID, **Import Plan ID**, tier, effective dates |
| Rate-change import | **Exists, and explicitly bypasses qualifying-event logic** — updates billing directly. Precisely the mechanism: change rates, leave participants active, stay non-billable |
| Participant import requirements (O31) | Employer Custom ID or System ID, Participant Custom ID if used, demographics. **SSN not listed as required** — supports D32/D35's no-SSN census, though absence from a prose summary is not proof of absence from the template |
| **Do imported status changes trigger letters?** | **Still open.** The docs say post-import billing changes generate events in Process Approvals and *related notices will reflect updated amounts* — ambiguous between "the import generates a notice" and "notices that would otherwise generate show the new figures." **Phase 3 of the discovery protocol must still run.** The docs move the prior; they do not close it |

## Benefit setup is manual — and that is a floor

**Summit provides no import template for creating Premium Billing benefit plans.** Setup is manual in
Employer Central: Plan Type, Plan Name, Description, Line(s) of Service, **Import Plan ID**, Effective
Date, plan-year details, carrier use types, PCOR Reportable.

Every "+" group therefore needs at least two benefits hand-created — the real ICHRA (CDH) and the
notional notice vehicle (Premium Billing). **No amount of AMS work removes this.**

Worth naming honestly, because the *we'll take your small cases* argument depends on per-group cost
being low. At three lives, two manual benefit setups plus plan-year configuration is a meaningful
fraction of total effort. It does not break the economics — automation still wins on the **recurring
monthly** work, which is where zizzl's $660 floor actually bites — but the pitch should say *ongoing
administration is automated*, not *setup is cheap*.

It also keeps SSA in the loop per group, which is a durability asset as much as a cost: not a
self-serve product an agency could run without you.

## The ICHRA setup checklist now has real content

This was the vaguest item in the build list. Concrete steps:

1. Create the **ICHRA benefit** (CDH) — **PCOR Reportable checked**, since ⚖️ PCORI applies to ICHRA
   and the checkbox is available for HRA / Other / Custom plan types. A compliance step otherwise
   missed silently.
2. Create the **notice benefit** (Premium Billing LOS), Import Plan ID per convention.
3. **Verify plan-year alignment** — the start date cannot fall after billing effective dates, and gaps
   between plan years are not permitted. A mid-year effective date needs deliberate setup, not
   defaults.
4. **Verify notice settings per benefit:** Mailed Letter **on**, DataPath Fulfillment **on**, Portal
   Mobile **off**, Push As Alert **off**.
5. Verify the Import Plan ID matches convention before the first import.

**Point 4 matters more than it looks.** Participants not seeing the notional benefit is **configured,
not inherent** — Portal Mobile displays the communication on the Participant Portal, and Push As Alert
notifies them at next login. Both are per-notice checkboxes that someone tidying notice settings could
switch on later. Record as required configuration, not as an observed property.

**DataPath Fulfillment** is the flag routing notices to DataPath's mailing service — presumably what
produces the tracked mailing and its per-notice cost.

## New open items

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O34** | **Does Participant Custom ID appear in the mailing export and J2/J3?** If write-only, the hash returns | 🔍 export inspection | **D35 — whether an entire correlation subsystem is needed.** Cheapest high-value question on the list |
| **O35** | **Does J7 cover CDH benefits, or Premium Billing only?** The header is PB-heavy (`PBBenefitID`, `PBType`, `PBTypeID`) alongside generic `BenefitID` / `PlanTypeID` | 🔍 export inspection | If PB-only, AMS sees the notice vehicle but **not the real ICHRA**, and the setup-completeness check only half works — a second export would be needed |
| **O36** | **Which of J7's four employer identifiers** (`TPA`, `EmployerOrganizationID`, `OrganizationID`, `EmployerID`) correlates to what AMS holds? None is obviously the `ERCustomID` the mailing export uses | 🔍 inspection | Joining benefit rows to employers reliably |
| **O37** | **Do Retiree or Direct Bill sub-LOS plans generate status-change notices with configurable content?** | 🟠 Summit help / test | Every awkwardness in D33 comes from the vehicle being **COBRA specifically** — employer confusion, COBRA reporting, participant visibility, the renaming workaround. **Direct Billing is conceptually much closer** to paying premiums for individual coverage. If either works, the artifact problem dissolves. Ask before the mechanism is standardised across the downline |
| **O38** | **Are division-scoped rates true class support?** (`DivisionName` at rate level) | 🟠 Summit / test | Whether ICHRA classes are supportable natively or need a workaround. Would answer **O7** affirmatively |

## Revised build list

**Legend:** 🔵 Kevin · 🟠 DataPath/Summit · 🟢 HealthSherpa · 🟣 SWBD · ⚖️ Counsel · 🔍 Self-serve

### P1 — Summit Data Exchange *(new; independent; benefits all current clients)*

| # | Type | Item | Purpose / user |
|---|---|---|---|
| 1 | Infra | SFTP client + credential storage | Net-new — no FTP/SFTP dependency exists today |
| 2 | Infra | Scheduled push/pull job | `InstallationHealthScheduler` pattern |
| 3 | DB | Exchange run/file log | What was sent or fetched, when, with what result — the audit and retry surface |
| 4 | UI | **Data Exchange monitor** | *PSP staff.* Run history, failures, manual trigger |

### A1–A3 — Illustration, provider check, design deliverables

Unchanged from Part 6. `rating_area_rate_cache`, `illustration_log`, illustration page, cache admin,
provider-check page (🔍 O23, terminal), `design_census`, `design_scenario`, intake and output pages.

### A4a / A4b — Conversion analysis, then book radar

Unchanged. A4a gated on 🟣 a few renewing groups; A4b on 🟣 O24.

### B4a — Notice obligation register *(independent; fixes current pain)*

| # | Type | Item | Purpose / user |
|---|---|---|---|
| 5 | DB | Participant correlation — **Custom ID per D35**, hash only if O34 fails | Joins a mailed row back to a person |
| 6 | DB | Mailing-feed staging | Ingests the scheduled notice export |
| 7 | DB | `notice_obligation` | `PENDING → EXPORTED → MAILED → RECONCILED`. **Nothing tracks this today** |
| 8 | DB | `notice_event_map` | Summit `EventTypeID` → AMS notice type. From protocol Phase 5 |
| 9 | UI | **Notice worklist** | *PSP staff.* The actionable queue |
| 10 | UI | **Obligation register / reconciliation** | *PSP staff, later employer-facing evidence.* Aging, completeness, exceptions |

🔵 How does AMS identify "+" employers before B1 exists — flag, manual list, or J7-derived?

### B-J7 — Benefit reference import *(new, small, enables the rest)*

| # | Type | Item | Purpose / user |
|---|---|---|---|
| 11 | DB | `summit_benefit` + `summit_benefit_rate` | J7 at benefit and tier grain. Supplies `ImportPlanID` for every generated import file |
| 12 | UI | **Benefit reference / setup-completeness view** | *PSP staff.* Does this "+" employer have both required benefits, correctly configured? |

### A5, A6 — Pipeline console, design advisor

Unchanged.

### B1 — "+" catalog and proposal

Unchanged: `LOS`, `ServiceItem`, `PlanType` (QSEHRA net-new), `ServiceModule` + `RateTable`, **the
ICHRA task sequence now populated from the setup checklist above**, `proposalsectionlos`, `plus_quote`,
the "+" proposal section. 🔵 O1 Gate 0 · 🔵 D18 · 🔵 T9 or a Manual-Setup prohibition.

### B2 — Census intake, participant model, Summit handoff

| # | Type | Item | Purpose / user |
|---|---|---|---|
| 13 | DB | `plus_census_stage` | Setup census. **May carry no SSN** (D32/D35) |
| 14 | DB | `plus_participant` | Anchors to `Employee`; holds the Custom ID and verification state |
| 15 | DB | `Employee` field additions | Promotes J3's `DOB`, `HireDate`, `EffectiveDate`, `TerminationDate`, `DivisionName` |
| 16 | UI | **Census upload — PSP-facing** | *PSP staff.* Column mapping, minimum-field rejection |
| 17 | UI | **Census upload — one-time GUID** | *Employer contact.* Opaque single-use drop |
| 18 | UI | **Import file generator + preview** | *PSP staff.* Streams to SFTP (P1) or download. Preview per D34 — or defer to Process Approvals |

⚠️ Touches `SummitImportService` / `SummitImportWizard` — live production code, where T36's bug lives.
**Needs its own Phase A.**

### B3 → C2

Unchanged: verification ledger (`questionnaire_instance.employee_id`, `participant_coverage_month`,
attestation campaign and form, monthly verification queue), then enrollment handoff, card ingest,
Policy Status, GA console.

## Revised question list

### Tier 0 — cheapest, highest leverage, answerable today

| # | Question | Owner | Blocks |
|---|---|---|---|
| 1 | **Does Participant Custom ID round-trip in the mailing export and J2/J3?** | 🔍 | **D35 — an entire correlation subsystem** |
| 2 | **Does J7 cover CDH benefits or PB only?** | 🔍 | The setup-completeness check |
| 3 | **Which J7 employer identifier correlates to AMS?** | 🔍 | Joining benefits to employers |
| 4 | **Do Retiree/Direct Bill generate status-change notices?** | 🟠 | Whether the COBRA artifact problem dissolves |
| 5 | **Do HealthSherpa plan objects carry networks/formularies?** | 🔍 | **A2 entirely — terminal** |

### Tier 1 — Track A

Hopkins carrier availability 🔍 · book profile 🟣 O22 · tobacco-loaded LCSP ⚖️ O19 · carrier names on
display ⚖️ O25 · branded design artifact 🔵 · 3–5 renewing groups 🟣.

### Tier 2 — B4a and P1

`EventTypeID` values 🔍 (protocol Phase 5) · **do imported status changes trigger letters** 🟠 (protocol
Phase 3 — everything downstream rests on it) · notional-benefit scheduled communications 🟠 O33 ·
MOVEit folder setup 🟠 (lead-time) · encryption key exchange 🟠 · "+"-employer identification 🔵.

### Tier 3 — B1/B2

Gate 0 🔵 O1 · D18 🔵 · participant-import template field list 🟠 O31 (prose summary is not the template)
· does the participant import accept **updates** 🟠 · CDH enrollment export carries the "+" plan 🟠 O29
· division-scoped rates as class support 🟠 O38 · T9 🔵.

### Tier 4 — B3 and beyond

Attestation sufficiency ⚖️ **O18** · HealthSherpa rep 🟢 O12 · BAA 🟢 O13 · deeplink self-service 🟢 O14 ·
ERISA display posture ⚖️ O20 · API surface re-verification 🔍 O2 · Policy Status timing 🟢 O16 · webhook
auth 🟢 O15 · MCCs loaded 🟠 O9 · live authorization test 🟠 O10.

### Tier 5

Card date semantics · full book share 🟣 O24 · employer funding 🟠 O11 · dependent DOB 🟠 O8 ·
`DivisionName` promotion 🔍 O4 · counsel docket ⚖️ O21 · Summit tobacco field 🟠 O27 · Summit native
invoicing 🟠 O28.

---
---

## Session close-out — SQL audit

*(Corrected 2026-07-30 — the migration figures below are stale. A live `schema_version` probe
confirmed 2026-07-30 that every migration V001–V073 is applied to production; V074
`rating_area_rate_cache` and V075 `illustration_log` were authored the same day and are correctly
unapplied on every environment. Current release is v0.73.02. Dated record preserved as written below.)*

**No SQL was produced.** Parts 1–7 are planning only.

- **Migration scripts produced or updated:** none.
- **Current highest version:** **V073**, to be re-verified against `ls docs/migrations/` and against
  the Gate 0 probe's `schema_version` read before any script is written.
- **Pending production deployment:** V072/V073 show unapplied in `migration_tracker.md` on every
  environment, yet the Monthly Billing Launcher is live in production and depends on `billing_run` /
  `billing_run_step`. Either the tracker is stale or `v0.73.PP` is not WAR-only. **Resolve before
  cutting it** — the Gate 0 probe answers this.
- **Read-only artifact:** the Gate 0 probe contains no DDL or DML and creates no migration obligation.
  If filed, it belongs in `docs/analysis/`, **not** `docs/migrations/`.
- **Schema changes described but not scripted:** **V074**–**V091** as mapped above, all placeholders
  pending renumbering, to be allocated in **commit order** if tracks run in parallel. Plus Branch B's
  reference-row delivery, whose mechanism is undecided (**D18**).
- **Orphaned SQL flagged:** `docs/migrations/seed_ndt125_questionnaire.sql` — unversioned, outside the
  numbered sequence (T38). Needs renaming with a self-registering `INSERT IGNORE INTO schema_version`,
  plus registration in `migration_tracker.md` and `schema_version_migration.sql`.

---
---

# Part 8 — Correlation keys resolved: Custom ID, division-scoped classes, and the two-report notice model

**Date:** 30 July 2026

## O34 — resolved NEGATIVELY

`ParticipantCustomID` appears in J2 but **not** in the mailing export. D35 does not fire. D10 (the SSN
hash), D11, D32, and O32 all survive — the hash remains the correlation mechanism for notice
reconciliation. Minting the Custom ID is still worthwhile for the AMS-controlled participant key, but
it is **not** the mailing-export join.

## O35 — resolved

J7 is Premium Billing only. But J4 covers CDH, and AMS already imports both, so AMS is not blind to
the real ICHRA benefit. The setup-completeness check works; it reads two exports rather than one.
`ImportPlanID` is present in both J4 and J7, so D36 holds on both sides.

## O36 — narrowed

`EmployerCustomID` appears only in J2, never in J7. J7 must join on a system identifier, and J2 is the
bridge — it carries `EmployerCustomID` alongside `Employer_ID`, `Organization_ID`, and
`EmployerOrganizationID`.

## O38 — strong affirmative evidence

Division fields appear at rate grain in J7 and at participant grain in the PB Initial Notice Event
Report. Together that indicates participants are division-assignable and contributions can vary by
division — **ICHRA classes may be natively supportable, with no AMS class model needed.** Confirmation
is a Summit UI check.

## New — the two notice reports are complementary, not competing

The **PB Initial Notice Event Report** is participant-keyed (`ParticipantSystemID`,
`ParticipantCustomID`), carries a real `MailedDate`, no SSN or DOB, and identifies new hires needing
the notice sequence — **it creates the obligation.** The **mailing report** is the only artifact
proving a notice was sent, and carries no participant ID. They map onto the two ends of
`PENDING → EXPORTED → MAILED → RECONCILED`. B4a needs both.

**Consequence for B4a:** because obligations are created from a participant-keyed report,
reconciliation matches a mailing row against a small set of open obligations already scoped to one
employer — a much narrower problem than D10 was designed for. Worth revisiting whether
`(employer + surname + DOB)` suffices before building the hash into B4a.

## New open items

- **O39** — which export file feeds `import7premiumenrollment` (the PB enrollment feed)?
- **O40** — is `ImportPlanID` persisted anywhere in AMS today? It is absent from `Benefit`'s field
  list, so D36 likely needs a new column.

## O23 — resolved favorably

See the 2026-07-30 section of `docs/business/healthsherpa.md`. **A2 stays in the plan.**

---
---

# Part 9 — LCSP data source split

**Date:** 2 August 2026 (S8-D)

## D38 — LCSP data source split: CMS for compliance, HealthSherpa for illustration

The CMS **ICHRA Employer LCSP Premium Look-up Table** is the source for any compliance-facing
affordability figure, where it covers the geography. Published by CMS/CCIIO, free, no API key, by
geography and age, covering FFE and SBE-FP states — Texas qualifies. It is **on-exchange by
construction**, which is what makes it the defensible source: the class of defect T44/V078 existed to
close is structurally impossible against it.

HealthSherpa remains the **illustration** source — market low/high premium, plan count, carrier count,
lowest bronze, age-band net cost. These are legitimately off-exchange market figures and do not move.

**SBM states fall back to HealthSherpa on-exchange quoting.** CMS does not publish them. The two
sources are complementary, not competing.

`onex_lcsp_premium` and `onex_benchmark_silver_premium` (V078, T44) are **retained**. They stop being
primary and become a **cross-check**: a material divergence between CMS and HealthSherpa for the same
county and age is a signal worth logging. T44's work is not superseded and must not be removed.

**Consequence worth stating.** The compliance-facing number stops depending on a vendor preview
endpoint with no SLA, and the affordability half of the build stops being gated on D-78/D-79 or on
production allow-listing.

**Reversal cost: low.** Purely additive — a new reference table and a resolver, nothing removed. If
the CMS file proves unusable, the current HealthSherpa-sourced path stands unchanged.

⚠️ **Hard prerequisite — Kevin's, not a work item.** `cms.gov` returns 403 to automated fetching, so
the PY2026 table must be downloaded through a browser. **No migration may be written against this
table until its actual columns and granularity have been inspected** — `healthsherpa_review_2026-08-02.md`
§5.8 is secondary-sourced and describes the file rather than shows it. This is a note, not a numbered
work item and not a dependency on any build item.

⚠️ **Evidence grade.** The CMS table's existence and coverage are 📚 secondary-sourced from
`healthsherpa_review_2026-08-02.md` §5.8 and have **not been verified against the file itself.**

---
---

# Part 10 — ICHRA+ and QSEHRA+ are two lines of service

**Date:** 2 August 2026 (S9-A)
**Source:** Kevin's end-to-end walk of the sales motion, session 9. Not a document.

## D39 — `ICHRA+` and `QSEHRA+` are two separate lines of service, not one LOS with a design attribute

**Decision.** `ICHRA+` and `QSEHRA+` are **two separate `LOS` rows**. They are not one line of service
carrying a design attribute, a product flag, or a variant column that selects between them.

**The deciding reason is structural, not operational — the quotable attached-product bundle differs by
statute.**

- **A health FSA is a group health plan.** A **QSEHRA requires that the employer offer no group health
  plan.** Therefore **QSEHRA + FSA disqualifies the QSEHRA.** The two cannot appear on the same quote,
  ever, and not as a matter of preference.
- **An ICHRA *is* a group health plan**, which is what makes the ICHRA itself **COBRA-continuable**.
  A **QSEHRA is excepted from that definition** and has nothing to continue.

**So: ICHRA can be quoted alongside COBRA and FSA. QSEHRA can be quoted alongside neither.** That is a
difference in what may legally sit in the same proposal — which is exactly what an LOS row governs in
this codebase, since `Proposal.losList` is what drives the priced bundle. **A design attribute on one
shared LOS cannot express "these two attached products are unavailable," only "these two are
unselected."** The distinction matters the first time someone tries to sell the disallowed combination.

**Supersedes the weaker earlier reasoning** that the two might merge because administration cost is
similar. **Administration cost may well be similar. That is not what decides it.** This also sharpens
**D15** (Part 2) and the Part 1 asymmetry finding, which established that ICHRA+ and QSEHRA+ diverge on
the *enrollment rail*; D39 establishes that they also diverge on the *quotable bundle*, which is a
second and independent reason for two rows.

**Consequence for the build sequence.** **The reference rows are two `LOS` rows, not one** — with their
own `ServiceItem`, `PlanType` and priced `ServiceModule` → `RateTable` rows, and their own
`agencyrates` assignment.

⚠️ **This lands on a task that is Kevin's, not a build item.** `swbd_ichra_build_plan.md` §3 item 4 was
**deleted as a numbered build item on 2026-07-31** — the reference rows are created through the admin
UI when Kevin is ready, and nothing is sequenced around them. **That file is outside this run's scope
fence and was not edited.** ⚠️ **Flagged for a later run: item 4's note in
`docs/swbd_ichra_build_plan.md` still reads as a single undifferentiated row-creation task and should
say two LOS rows.**

⚠️ **Caveat, and it materially limits how often the COBRA half fires.** **Most of SWBD's book is under
20 lives, where federal COBRA does not attach at all** — Texas state continuation applies instead. The
COBRA attachment above is real, but **it quotes mainly at the larger end of the book**, not across it.
Do not build the ICHRA+ bundle presentation as though COBRA were a standard attachment for this
population. `state continuation on ICHRA loss` is already carried in the **O21** standing docket
(Part 2), and this is the second decision to lean on it.

---
---

# Part 11 — O2 resolved: the enrollment surface is documented, and four HSOne-era premises dissolve

**Date:** 4 August 2026
**Source:** `docs/business/healthsherpa.md` sections **2026-08-04** and **2026-08-04 (b)**, produced by
reading 13 of the 25 pages at `docs.ichra.healthsherpa.com`. No credential, no representative, no BAA.
**Part 11 governs.**

## O1–O21 status changes

| # | Was | Now |
|---|---|---|
| **O2** | Open since 2026-07-29, *"~1 hour, no account needed"* | ✅ **RESOLVED** — with one residual, promoted to **O41** below. See "What O2 actually answered" |
| **O14** | Deeplink self-service or agent-completed? Open | ⚠️ **Partially answered, leaning agent workstation.** Not closed |
| **O15** | Webhook auth methods supported | ⚠️ **Sharpened, still vendor-gated.** The *setup process* is public; the **delivery semantics** are not, and they are the part that changes AMS's code |
| **O16** | BCBS TX policy status *when* in 2026; CHRISTUS at all | ⚠️ **Half-answered from public material.** BCBS TX is published as *"coming in 2026"* — **year confirmed, month not.** CHRISTUS is **absent from the matrix entirely**, not marked "coming" |
| **O20** | ERISA posture on an AMS-built plan display | ⬆️ **ESCALATED from "confirm" to load-bearing.** No longer avoidable — see below |
| **O13** | BAA execution | **Scope changed, not status.** The deeplink accepts `ssn`, so PHI minimisation is a **design choice AMS makes**, not a property of the rail. EnrollConnect's PHI surface is materially larger than Part 1 recorded |

## What O2 actually answered — and the one thing it did not

O2 was written as four HSOne-era sub-questions: *is the enrollment list employer-scoped; do
`employer_external_id`, `updated_since`, and `employer.external_id` exist.*

**Three of the four dissolve.** There is no `GET /v1/enrollments` employer-scoped polling endpoint on
this product. `employer_external_id` and `updated_since` do not appear anywhere in the ICHRA Partner
API's documented surface — they are HSOne's, exactly as the provenance warning in this document's
header anticipated. **The correlation model is different in kind, not in spelling:**

- **`external_id` is set by the platform, per application**, and is **enforced unique per platform**
  (a duplicate returns `422 duplicate_external_id`).
- It is echoed in **both** webhook payloads, alongside `application_id` and `transaction_id`.
- Employer identity travels in the **`hra` object** (employer `name`, `phone`, address, **`fein`**,
  plus `tpa_slug`) — as application content, not as a queryable scope key.

⚠️ **The fourth sub-question is NOT answered, and is promoted rather than buried.** `GET
/api/v1/applications` exists and is paginated, but **its query parameters were not captured in this
read.** Whether it can be filtered by employer — or whether per-employer grouping is entirely AMS-side
off `external_id` — is unestablished. **See O41.**

⭐ **Consequence for C4 (Part 1) and for backlog #38.** C4 treated `employer.external_id` as a third
correlation key, and `healthsherpa.md`'s 2026-07-28 section framed #38's *"concrete unblock"* as
employer-scoped `GET /v1/enrollments` polling with `updated_since`. **That unblock, as described, does
not exist on this product.** What replaces it is stronger, not weaker: **push webhooks carrying
`paid_through_date`** (Policy Status), which is a better substantiation artifact than a polled list —
*"coverage was in force through this date"* rather than *"is this person active now."* But it is
**push, correlated on AMS's own key**, and any design assuming a pull-by-employer needs rewriting.

## The poll design — settled, and it belongs to EnrollConnect not to a status endpoint

O2 was said to block *"the correlation map, the poll design, and every HSOne-inherited assumption."*
All three are now unblocked. Documented polling guidance:

- **Minimum 1 hour** after submission before the first poll; then every **4–8 hours**.
- **Never more than once per minute per application** (`429`, carrying `Retry-After`).
- `5xx` retryable, exponential backoff, **max 3 attempts**.
- `include_events=false` for a lightweight poll.
- **Webhooks are the documented primary; polling is the fallback** — which inverts the emphasis Part 1
  C3 carried.

Most carriers report effectuation within **1–3 business days**.

## D-series consequences

| # | Change |
|---|---|
| **D20** | ⬆️ **Upgraded from a decision to a requirement.** *"Outbound correlation key is a separate opaque UUID"* was adopted as good hygiene. `external_id` being **unique per platform**, with **create and submit both non-idempotent**, makes it structurally necessary — AMS must own dedup and cannot retry blindly. **D20 is no longer optional and should stop being described as a choice** |
| **D16** | ✅ **Strengthened.** The **off-exchange webhook payload is a superset of on-exchange** — off-ex adds `application_id`, `issuer_hios_id`, `members[]` and `policies[]`; on-exchange carries materially less. The data coverage verification needs **exists only on the off-exchange rail**. Off-exchange-first is now a data-availability argument, not only a licensing one |
| **D17** | **The `HS_POLICY_STATUS` rung becomes concrete per carrier**, and the launch market is mixed: **UHC live today** (23 of Hopkins' 65 plans), **BCBS TX coming in 2026** (24 plans), **CHRISTUS absent from the matrix** (18 plans). ⭐ **The "rural Texas has no automated verification" premise is wrong** — roughly a third of that market is automatable now. The `ATTESTATION` rung is still primary and **O18 is still terminal for B3**, but D17's ladder is no longer hypothetical at its top rung |
| **D15** | Unchanged and reinforced. The rail asymmetry stands; `sep_reason` carries **both** `offered_ichra` and `offered_qsehra` as first-class values, so QSEHRA's *SEP trigger* is supported off-exchange even though D15's *enrollment leg* asymmetry is unaffected |
| **D38** (Part 9) | ✅ **Reinforced by an independent mechanism.** D38 chose CMS for compliance-facing LCSP because it is *"on-exchange by construction,"* making T44/V078's defect class *"structurally impossible."* **Silver loading explains why that defect class is systematic rather than incidental** — issuers file off-exchange-only silver *mirror* plans without the CSR load, so the off-exchange LCSP understates the on-exchange one **in every silver-loaded market, always in the dangerous direction**. V078's Hopkins numbers (44% gap, identical 1.441 ratio at both ages) fit this exactly. **D38's reasoning was right and is now better supported than when it was written.** ⚠️ Still **[inference]** — the discriminating test is two staging calls, filed as backlog **T151** |

## ⭐ O20 is now load-bearing — the enrollment API forces the plan display

**`plan_hios_id` is a required field on every enrollment route** — the current Application Deeplink,
EnrollConnect, and Deeplinks V2, whose specification states *"A specific plan HIOS ID remains
required; no shopping/browse experience is supported."*

**There is no integration shape in which SSA hands off to a HealthSherpa shopping experience and the
presentation burden travels with it.** AMS must present plans and pass one.

**So O20 stops being a confirmation item and becomes a gate on B5 existing at all.** The ERISA
safe-harbour constraints already recorded — complete list, neutral ordering, employee-controlled
sort/filter, no "recommended" badge, no default selection, no hidden carriers — are now the **only
compliant route to the single input the enrollment API requires**. They are a mechanism, not a
preference.

⚖️ **Sharpen the counsel question accordingly.** It is no longer *"is a curated list an endorsement
problem?"* — nobody is proposing curation. It is: **does SSA building a complete, neutral, uncurated
plan display on the employer's behalf itself constitute employer endorsement?** If the answer is yes,
**B5 has no compliant form on this rail** and the enrollment leg needs rethinking rather than
redesigning. That is a materially bigger question than O20 was carrying, and it should go to counsel
with **O18 and O17** rather than after them.

## B5 / B7 — the architecture is now fully specified

Not a scope change; a removal of unknowns. The route is documented end to end:

1. Quote (built) → every plan carries `deeplink_enrollment` and `api_enrollment`.
2. Present neutrally (O20) → obtain `plan_hios_id`.
3. Route on the documented tree: `api_enrollment` → **EnrollConnect**; else `deeplink_enrollment` →
   **Deeplink**; else quote-only (`422 "Plan is not available for enrollment"`).
4. Optional pre-validate: `GET /api/v1/plans/{hios_id}?include=enrollment_requirements`.
5. Submit. ⭐ **Both routes accept the same canonical request schema** — one payload builder, two
   routes, which is simpler than Part 3 assumed.
6. Payment: `payment_required_with_submission` → ACH server-side via `PUT /payment_method`; else
   submit then `payment_redirect` / pay-by-phone / carrier-external.
7. Reconcile on webhooks; poll as fallback.

⚠️ **Correction to a claim repeated throughout this document set:** *"enrollment cannot be fully
headless"* is **carrier-dependent, not universal.** It holds for the `payment_redirect` path; ACH can
be set server-side, and Anthem/Wellpoint gained in-flow ACH on 2026-07-29.

⚠️ **Texas-specific, and it lands on the demo county:** `disclosure_statement_accepted` is a
**required attestation for Texas HMO plans** — and Hopkins' reference plan is *Blue Advantage Silver
HMO 306*. EnrollConnect also requires supplemental signatures for **CO, UT, NJ**, and a typed full
legal name plus signature date.

## New open items

| # | Item | Settled by | Blocks |
|---|---|---|---|
| **O41** | **Can `GET /api/v1/applications` be filtered by employer**, or is per-employer grouping entirely AMS-side off `external_id`? The endpoint exists and is paginated; its query parameters were not captured. **This is O2's one unanswered sub-question, promoted rather than closed silently** | The linked **OpenAPI YAML** — public, no credential | The B4a/#38 correlation map, and whether AMS needs its own employer↔application index |
| **O42** | **`_agent_id` — required or not?** The **2026-04-09 changelog** says *"no longer required"*; the **current Application Deeplink page** lists it as required. Both cannot be true of the same endpoint. **`_agent_id` is the AOR mechanism** and SWBD's downline attribution rests on it — if it is optional, attribution may silently fall back to an account default, re-creating the single-AOR failure mode the 2026-07-28 evaluation flagged as potentially fatal | The OpenAPI YAML for both endpoints — public | The AOR design; backlog twin is **T144** |

**O15 is sharpened, not replaced.** The webhook *setup process* is documented (supply HTTPS endpoint,
auth preference, exchange scope on/off/both, which APIs, environment; they register; verify in
staging; go live). **What remains vendor-only is the operational contract:** authentication methods
(*"a variety"*, unspecified), **retry policy, delivery guarantees, ordering, idempotency /
`transaction_id` handling, signature verification, IP allow-listing.** Those five decide whether AMS's
receiver needs dedup and reordering logic — i.e. whether `ApiTokenFilter` suffices is now the *smaller*
half of O15. **Ask for them by name.**

## ⚠️ Standing caveat on everything in this Part

These findings come from **fetched-and-summarised documentation pages, not from parsing the OpenAPI
YAML** that each API reference page has linked since 2026-07-09. This document set has now been bitten
**four times** by precisely what prose drops — `fips_code`/**`fip_code`**, `uses_tobacco`/**`smoker`**,
`/v1/quotes`/**`/api/v1/quotes`**, and now `/ichra/off_ex`/**`/public/ichra/off_ex`**.

**Nothing in this Part is safe to code against without a YAML diff first.** Filed as backlog **T145**,
and it would settle **O41** and **O42** as a side effect.

## What this Part does not touch

**The Gate 0 / B1 track, the Summit correlation work (O5–O11, O26–O40), the card items, and the
notice model are all unaffected by O2.** O2 was scoped to the HealthSherpa enrollment/status surface
and nothing in it bears on Summit, billing, or the catalog. Parts 5–8 stand unchanged.

**Precedence: Part 13 > Part 12 > Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**

---
---

# Part 12 — The ICHRA+ bundle gets its own Summit plan types

**Date:** 7 September 2026 (S27-D, S27-E, S27-F)
**Source:** Kevin's decision, taken on the source-reading findings of S27-D and S27-E. The mechanism
those runs established is recorded in
[`docs/business/summit_data_exchange.md`](../business/summit_data_exchange.md) under "How AMS task
checklists key off plan types" — read that first; this Part is the decision, not the evidence.

## D40 — the ICHRA+ bundle gets its own Summit plan types

> ⚠️ **Amended 2026-09-07 (S27-G). The ICHRA half of D40 is reversed — read this before the original
> text below, which is retained for provenance.**
>
> **The standard `ICHRA` plan type is reused**, with the card difference carried by the **`ICHRA+`
> plan template (1030)** rather than by a distinct type. **Rationale:** a plan type in AMS drives only
> the renewal checklist, and **the ICHRA renews identically whether or not a card is attached**.
> Card-on/card-off is a *template* setting. Heavier funding and higher opt-out rates on the
> facilitated model are **data differences, not task differences** — and the renewal mechanism keys on
> tasks, not amounts.
>
> **The `Ins125+` split stands and carries the load.** The existing `Ins125` type serves groups SSA
> performs annual testing for — **no cards, no per-cardholder premium loading**. Because tasks attach
> to a plan type and fire for every group holding it, and **the union is additive with no subtraction
> on either the setup or the renewal path**, card-loading tasks on the existing `Ins125` would fire on
> **every testing-only group**. That is the differentiating recurring work, and it lives on **the card
> plans, not on the ICHRA** — which is exactly why the ICHRA can stay standard.
>
> **Two notice plan types added:** **`I_NOTICE`** and **`Q_NOTICE`**, kept separate because notice
> content and renewal process are expected to differ between ICHRA and QSEHRA.
>
> **`FSA`, `DCA` and `HSA` remain unchanged** — nothing about this sale varies for them.
>
> **Reversal cost:** creating a plan type is cheap; **merging two later is not**, because `Benefit` is
> an inbound Summit mirror and existing rows keep the type they were created with. That asymmetry is
> unchanged by this amendment and still argues for splitting where a task set genuinely differs.
>
> ⚠️ **Open, and it should be checked before renewal sequences are built:** whether the existing
> `ICHRA` and `Ins125` types **already carry renewal sequences built for direct sales**. The union is
> additive, so a facilitated group **inherits whatever the shared type carries**. `Ins125+` starts
> **empty** — which is **correct** if facilitated groups need no non-discrimination testing, and a
> **gap** if they do, since those tasks would have to be **rebuilt on the new type rather than
> inherited**.
>
> The Summit objects that now exist under this amended decision are recorded in
> [`docs/business/summit_data_exchange.md`](../business/summit_data_exchange.md), "Summit objects
> created for the ICHRA+ bundle — 2026-09-07".

**Decision.** The ICHRA+ bundle gets **its own Summit plan types**, rather than reusing the existing
ones.

**Why.** The existing `Ins125` type serves groups SSA does annual testing for — **no cards, no
per-cardholder premium loading**. Tasks attach to a plan type and, as S27-E established, **fire for
every group holding that type**; there is no subtraction and no substitution on either the setup or
the renewal path. So card-loading tasks hung on the existing `Ins125` would fire on **every
testing-only group**. Separating the types is the only mechanism the model offers to keep those task
sets apart.

**The new types:**

| # | Type | Platform | Notes |
|---|---|---|---|
| 1 | ~~**ICHRA+**~~ | CDH | ⚠️ **REVERSED by the 2026-09-07 amendment above** — the standard `ICHRA` type is reused; the card difference is carried by plan template **1030** |
| 2 | **Ins125+** | CDH | **Stands.** One type, two plan templates — templates **1031** (Excepted Benefit) and **1032** (Off-Exchange) |
| 3 | **ICHRA+ notice plan** | Premium Billing | **Stands, and now split in two** — types `I_NOTICE` and `Q_NOTICE`. `I_NOTICE` has template **1033**; `Q_NOTICE` has none yet. Still gated on **O43** below |

⚠️ **Why `Ins125+` is one type and not two.** Both templates are premium salary reduction landing on
the card, so they carry the **same eligibility** and therefore the same type. A group holding both
generates **two `RenewalItem`s that dedupe to one task set** — the correct outcome, not a defect.
What differs between them is the **inbound data source** (Presidio vs off-exchange), and that is
**ingestion, not adjudication**. Splitting the type to model a data source would push an ingestion
concern into the claims-eligibility dimension, which is what the plan type actually means.

**Unchanged: `FSA`, `DCA` and `HSA`.** Nothing about this sale varies for them, so they keep their
existing types and existing sequences.

### Reversal cost — asymmetric, and that asymmetry is the argument

**Creating a type is cheap. Merging two types later is not.** `Benefit` is an inbound Summit mirror
(`summit_id NOT NULL`), so **existing rows keep the type they were created with**. Unwinding a split
would mean either reassigning historical benefit rows — rewriting mirrored data AMS does not own — or
carrying both types indefinitely. Splitting later is cheap; merging later is not.

## O43 — do Premium Billing plans mirror into AMS as `Benefit` rows?

**Open question.** Do Premium Billing plans appear in the **Summit plan-type export**, and do they
**mirror into AMS as `Benefit` rows carrying a plan type**?

**Why it matters.** The tested chain (`docs/business/summit_data_exchange.md`, "The proven chain") was
**CDH-only**. Renewal checklists resolve exclusively through `Benefit → PlanType → ServiceItem`, so a
plan type that never arrives as a `Benefit` row **cannot drive a renewal checklist at all**. If PB
plans do not mirror, the ICHRA+ notice plan type (item 3 of D40) cannot carry its own renewal tasks
and **that work must hang off a CDH type instead**.

**Settled by:** a **Summit test** — export the plan types, run an import, look for the PB plan in
`plantype` and for a mirrored `Benefit`. **Not settleable by reading AMS code**, which can only show
what AMS does with rows it receives, never which rows Summit sends.

**Precedence: Part 13 > Part 12 > Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**

---
---

# Part 13 — The card's two sub-accounts fund on different rules

**Date:** 8 September 2026 (S34-A)
**Source:** Kevin's proposal on the Presidio call, accepted by Daniel Cruz. The reasoning on both
sides was already in [`legal_assumptions.md`](legal_assumptions.md) — LA-20, LA-21, LA-22 and LA-36.
What had never been filed was the row recording the consequence for **card configuration**.

⚠️ **Evidence grade.** No call record in `docs/` carries this exchange. The origin is Kevin's
recollection of the call, not a document, and Daniel's acceptance is recorded nowhere in the repo.

## D41 — one card, two sub-accounts, two funding rules that differ by benefit type

**Decision.** A single card carries **two separately-ledgered sub-accounts whose funding rules differ
by benefit type.**

| Sub-account | Employer advance | Funding rule |
|---|---|---|
| **Excepted-benefit premium** | **Permitted** | The employer may pay at the start of a coverage period and collect the employee's share by salary reduction across that period |
| **Off-exchange individual major medical** | **Never** | Funded only from salary reduction **already withheld**. The balance must never run ahead of withholding |

**What it closes.** Whether the card's funding behaviour is uniform across benefit types. **It is not,
and cannot be.** The card itself cannot distinguish them — **the sub-account configuration is what
carries the distinction.** That is what makes separate ledgering a compliance control here rather
than plumbing.

**Why — the asymmetry is not a risk preference.** Excepted benefits sit **outside the market
reforms** (excepted from PHSA 2711 and 2713, so there is no market reform to violate) and carry a
**§106 shelter** on unrecovered premium, so an unrecovered balance at a mid-year termination is
employer-paid coverage rather than forgiven debt. **Individual major medical has neither.** Employer
money reaching an individual premium outside the ICHRA allowance is the arrangement the market
reforms were written against, and a purse running ahead of withholding is the mechanism that
produces it. See **LA-20** for the shared-card structure and its separate-ledgering requirement,
**LA-22** for the excepted-benefit side, and **LA-21** and **LA-36** for the major medical side —
LA-36 records the five employee-sourced on-ramps for month one and the **closed decision** against
employer advance there.

**Open within the decision — the confirmation mechanism on the payroll-match side.** Whether
confirmation of amounts actually withheld arrives as a **payroll file feed** or, in Daniel's
phrasing, a **regular email** was raised on the call and **not settled**. Both were live; neither was
chosen. It is a mechanism question rather than a compliance one — the funding rule stands either way
and only the evidence of withholding varies — so it is recorded here as open rather than promoted to
a numbered O-item.

**Reversal cost: low**, as the register records it on all three underlying assumptions (LA-20, LA-21,
LA-22): card configuration and funding schedule. No schema depends on which way it runs.

**Precedence: Part 13 > Part 12 > Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**

## D42 — the card carries three sub-accounts, not two: a Presidio post-tax bucket joins D41's pair

**Decision.** The premium card carries **three** benefit types, not two.

| Bucket | Tax character | Who | Funding control |
|---|---|---|---|
| Post-tax Presidio | After-tax wages, employer forwards under the DOL voluntary-plan safe harbor | **Default** for all Presidio buyers; **required** where the buyer is a more-than-2% S-corp shareholder, an S-corp shareholder's family by §318 attribution, a partner, or a sole proprietor | Loose — employer fronting is a non-issue for excepted benefits |
| Pre-tax Presidio | §125 salary reduction, excepted-benefit premium | §125-eligible W-2 buyers only, in a group that adopts §125 for its whole eligible population, **and** only after Presidio has addressed §V.H in writing | Loose |
| Pre-tax off-exchange ACA | §125 salary reduction net of the ICHRA allowance | ICHRA holders buying off-exchange individual major medical | Arrears loading; exact payroll match |

**Payment instrument.** All three buckets pay by a **card in the member's name**. No SSA-originated
ACH to Presidio in any bucket — a TPA is not on Presidio's accepted-payer list, and an unauthorized
payment is refunded and not credited, which lapses coverage.

**What it closes.** The open question carried since the Daniel call — whether the card needs one
loose benefit type and one exact-payroll-match type. It needs three. The third bucket exists because
Presidio's own approved forms (§V.H) contemplate post-tax payroll facilitation, and because a large
share of the target census cannot participate in a §125 plan at all.

**Relationship to D41.** D41 established two sub-accounts split by benefit type — excepted-benefit
premium (employer advance permitted) versus off-exchange individual major medical (employer advance
never permitted). This decision does not revise that split; it divides D41's excepted-benefit
sub-account in two by **tax character** (post-tax default, pre-tax only where both gates below are
passed), because a card that is only ever pre-tax cannot serve the share of the Presidio buyer
population who cannot hold pre-tax status at all. D41's major-medical sub-account is unchanged and
is the third row above.

**Cross-references.** **LA-37** (§125 participant eligibility of owner-buyers — more-than-2% S-corp
shareholders, §318-attributed family, partners, and sole proprietors cannot participate in a
cafeteria plan); **LA-38** (Presidio's §V.H payer restriction, which is why post-tax is the default
and every bucket requires a member-name card); **LA-21** (arrears loading, carried forward unchanged
for the off-exchange ACA bucket); and **D40, D41** (the two prior decisions in this family).

**Reversal cost: low** — card configuration and funding-bucket assignment. No schema depends on
which tax character a given buyer lands in; getting the qualification question wrong at sale is the
expensive direction (see LA-37).

**Precedence: Part 13 > Part 12 > Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**

---
---

# Part 14 — Custom-event ICHRA/QSEHRA notices, and Summit flags derived from elected items

**Date:** 10 September 2026 (S46)
**Source:** Kevin's Summit UI testing 2026-09-10, recorded in `docs/business/summit_data_exchange.md`,
"Custom-event notices and scheduled exports — tested 2026-09-10", and a QSEHRA test recovered from a
prior chat, recorded in `summit_notice_automation_discovery.md`.

## D43 — ICHRA/QSEHRA notices use a Summit custom event, not a notional COBRA benefit (flat-amount groups)

| Stage | What happens |
|---|---|
| **Setup** | The employer is CDH-only. The allowance text goes in *Employer Plan Name* (entered by hand until the SDX question on importing it is answered). The employer is added to the participant-list audit export. On Demand *All* sends the initial notices. |
| **Ongoing** | A scheduled participant export goes to `ExportFiles`. AMS lists Active rows with a blank `ParticipantCustomID` for ICHRA employers. The operator sends On Demand *Just These*, **then** codes the person `{prefix}-S-{Participant_ID}` in Summit, so they drop off the list. Coding without sending is the unsafe failure. **Never push Demographics for these people** — with no custom ID it would create duplicates. |
| **Renewal** | Update *Employer Plan Name* if the amount changes, then On Demand *All* before the notice deadline. |
| **Proof of mailing** | PB Mailing - Detail Report under DataPath fulfillment. Whether it includes custom events is an open SDX question (SDX-23). |

**Rationale:** proven on a CDH-only employer (`ZZ CDH Only Test`, Employer_ID 1393). It eliminates:
- the COBRA flag, which cannot be undone;
- the COBRA initial-notice trap;
- the notice benefit and its tiers;
- both unproven Premium Billing file types (setup-sequence files 3 and 5).

**Limits:**
- the `EmployerPlanName` collision on groups with real COBRA administration (the default COBRA
  general notice also reads `EmployerPlanName` as "the Plan" name);
- one amount per group;
- no hire date in the export, so timeliness depends on how often the export runs (daily chosen).

**Reversal cost:** low. It is Summit configuration plus one AMS review surface.

**Superseded, not deleted.** The Premium Billing notice path (setup-sequence files 3 and 5,
`I_NOTICE`, plan template 1033 — see `docs/business/summit_data_exchange.md`, "Summit objects created
for the ICHRA+ bundle") is superseded for flat-amount groups by this decision. It remains the fallback
for amounts that vary by participant, per the recovered QSEHRA test's tier-name mechanism (see
`summit_notice_automation_discovery.md`, "2026-09-10 — prior QSEHRA test recovered"). Tasks 12 and 14
of `docs/business/ichra_setup_checklist.md` are annotated accordingly, not removed.

## D44 — Summit administration flags and paired plan templates, derived from elected items (design; gated)

- **Each elected ServiceItem maps to a set of Summit administration types.** For example, HFSA → CDH;
  COBRA → COBRA; HSA → none. The employer's flags are the **union** across everything elected.
- **The employer file emits `true` or blank, never `false`**, because Premium Billing flags cannot be
  turned off and a re-export must never disable a setting administered outside AMS.
- **Plan templates prefer a paired mapping.** When a paired companion is also elected, the paired
  mapping (e.g. HFSA + COBRA → an HFSA dual template) replaces the single mapping. COBRA alone emits
  no CDH row.
- **Keyed on ServiceItem id**, extending V095 `summit_plan_template_map`, with no literal IDs.
- **Gated on two SDX tests:** SDX-25 (that a blank flag leaves the setting unchanged) and SDX-26 (that
  the CDH Plan import accepts dual templates).
- **With the custom-event notice design (D43), ICHRA maps to CDH only.**

## D45 — Census intake (Kevin, 2026-09-11)

Settles the census chain's stages 1–4 (s47a Phase A). Supersedes D29's "increment two" framing —
the one-time GUID drop is built now (build 1, s47c), with D29 option 1 (PSP-staff Census Upload)
kept as the fallback. Decisions a–f verbatim:

- **a. Rows, not files.** The client's file is parsed in memory; only whitelisted columns are staged
  and the raw file is never stored. If the client's headers miss a required field, the upload is kept
  as *unreadable* with header names only. The fallback remains Census Upload (D29 Option 1 stays).
- **b. Lenient parse for client uploads only.** Incomplete rows are staged with their issues rather
  than rejected wholesale. Census Upload keeps all-or-nothing.
- **c. Token.** One request row with a UUID and a 30-day expiry. Unlimited uploads, the newest
  superseding the rest. It closes on load or revoke.
- **d. Request email.** SendProposal's compose pattern, sent to the primary contact. The default body
  carries the link, the required columns, and "leave out SSN, DOB and pay." It is logged to the
  activity. Step 3 shows its status, and the requester is emailed on each upload.
- **e. Review.** Shows the mapping, the rows and each row's issues, plus a diff against the current
  roster (new, matching, missing). **Load** requires zero issues. **Reject** reopens the link.
- **f. Replace until pushed.** The roster can be replaced until Demographics is pushed or marked
  done, then it's refused, and the same guard applies to the existing Clear action. Changes after
  that are ongoing administration, not setup, so there is no stale flag.

**D30 is not reversed** by (a): no raw file is kept, and the staged rows are the same data class the
roster already holds — see LA-41. Build 1 (a–d) shipped s47c as V100, `CensusRequestServlet`,
`CensusDropServlet`, `CensusRequestStatusServlet`, `CensusIntakeService` and
`CensusParseService.parseLenient`; build 2 (e–f) shipped s47f as `CensusReviewServlet`, additions to
`CensusIntakeService`/`CensusSubmissionDAO`, and a guard block in `CensusUploadServlet.handleClear`.

**Build 2 design notes (2026-09-11).**

- **Three calls made for the build, recorded as design notes rather than asked of Kevin.**
  (1) `ActivityStatus`/`ReasonCreated` ids are system rows seeded identically by
  `DatabaseInitializer` on every installation (:276-278, :322-329) — not PSP-scoped reference rows
  an admin creates — so rule 4's cross-installation risk does not apply; `SendProposal` and
  `AddNoteToActivity25` already depend on the same ids, and the new code names them as constants in
  one place (`CensusIntakeService`) rather than scattering literals. (2) The inbound entry uses
  `ReasonCreated` 4 ("Received Email"), the nearest inbound type that already exists; its detail
  text says plainly that it was a census upload. (3) Branding stays as build 1 built it — the
  upload page resolves its agency the same way the request email does (`OriginatingAgencyResolver`),
  which differs from `/apply/*`'s rule; that divergence is recorded (checklist Open questions), not
  changed. The link host (the admin's, never `agency.landing_host`) is pre-existing behavior shared
  with `SendProposal`, also left on the checklist for Kevin.
- **Review diff key.** `lower(trim(first)) | lower(trim(last)) | first five digits of postal code`,
  computed over the staged upload and the current `employer_participant` roster, to bucket rows as
  New / Matching / Missing. No uniqueness exists on the roster table (s47e Q9); this key is a
  review aid only, not a database constraint.
- **Load into an empty roster is always allowed.** The demographics-settled guard and the
  replace-confirmation checkbox apply only when a roster already exists — a first load has nothing
  to replace and nothing to orphan.
- **Load is retry-safe after a failed insert.** If a replacement's `deleteByProspectId` succeeds but
  `insertAll` then fails, the submission is left `PENDING` with its rows intact rather than being
  closed — the operator can run Load again without another client upload. The clear that already
  happened is not undone.
- **Reject keeps the token and extends the expiry.** The request stays `OPEN` on the same token;
  `expires_at` resets to now + `EXPIRY_DAYS` so the client can upload a corrected file at the same
  link rather than needing a new one.

**Amended by walk, 2026-09-11 (s47g):** the upload entry's status follows the outcome. A clean
upload is Waiting on Us; an upload with issues, or an unreadable one, is Waiting on Them. All three
log a Received Email entry and offer Review.

## D46 — Employer flags: all four explicit (Kevin, 2026-09-11)

*Amends D44's "true or blank only."* File 1 emits CDH, COBRA, Direct Bill and Retiree as explicit
`true`/`false`, derived from elected ServiceItems. Kevin's view is that this settles the other
employer questions (SDX-25 becomes moot, and D-97's defaults no longer apply). **Pending test
SDX-27** — that `false` is honored on create, and what `false` does on update of a flag already on.

**Design note (s47b, 2026-09-11).** File 1 has no optional columns today, and the rule that an
optional element must never be last applies to it (`summit_data_exchange.md`, trailing-column
rule). Column order is therefore the six mandatory columns, then *Employer Plan Name* (optional),
then the four flags. The flags are never blank under D46, so the file always ends on a populated
column. The flag derivation needs a new ServiceItem → administration-type config, because no kind
marker exists on Setup ServiceItems (`ServiceItem.code` is null there — s47b Q10; copy the V095
`summit_plan_template_map` + `SummitPlanTemplateAdmin` pattern). **With D43, ICHRA maps to CDH
only** (D44's line stands).

## D47 — File 1 (Employer Demographic) column sources (s48, 2026-09-11; code `add58f9131f7397259304d780f75ec0adf23ab1d`)

File 1 now carries 11 columns, in this order:

- the 6 mandatory columns;
- Employer Plan Name;
- four flags: CDH, COBRA, Retiree Billing, Direct Bill. Each is an explicit `true`/`false` (D46),
  through `SummitCdhElementResolver.bool`.

- **(a) Employer Name.**
  - Source: the `company_legal_name` answer. It is in `packages/general.json`, `required`, and
    attached to every LOS and enhancement.
  - Blank → `Prospect.name`, with a WARN. Both blank → refuse.
  - The legal name is preferred per Kevin: the agent usually enters `Prospect.name`.
  - Supersedes the S25-C Javadoc rationale for `Prospect.name`.
  - The filename stays on `Prospect.name`, because Summit names result files `Response_` + the
    source filename.
- **(b) Flags.**
  - Source: the `SummitEmployerFlagResolver` union over `loadElectedServiceItems`, the same
    election source file 2 uses.
  - Refuse when no flag is true. There are three distinct messages: no PSP in session (500 +
    `log.error`); no mapped rows (400); rows exist but all are false (400).
  - Unmapped elected items are logged at INFO only. Kevin's model: a line-of-service item such as
    FSA maps to nothing, and its enhancements carry the templates and flags.
- **(c) Employer Plan Name.**
  - Text: `{keySegment} allowance: $M/month ($A/year)`.
  - Emitted only when exactly one elected template's key segment is in
    `SUMMIT_ALLOWANCE_KEY_SEGMENTS`. That setting is in `ssa.properties`, comma-separated,
    case-insensitive; absent means `ICHRA`.
  - Zero matches → empty. More than one → empty + WARN.
  - Legacy path (no template config) → `ICHRA`, if listed.
  - Amount: `hra_annual_ee`, from the shared "105 Benefit Allocation" section used by every HRA
    type. Absent or unparseable → empty + WARN.
  - Monthly is annual ÷ 12, `HALF_UP`, 2 dp, no thousands separators.
  - This supersedes s48b's ICHRA-only rule.
- **(d) COBRA suppression.** When the COBRA flag is true, Employer Plan Name is empty (INFO),
  because the default COBRA general notice merges it as "the Plan."
  - Conditional reversal: delete the condition once both of these are true:
    - the COBRA general notice no longer merges `EmployerPlanName`. Kevin intends to reword that
      paragraph to drop the plan name;
    - other letters that use the field have been checked.
  - Cost while it stays: a group with both COBRA and an HRA gets no allowance text in its custom
    notice.
- **(e) The key segment now has three roles.** It is:
  1. the `Import Plan ID` upsert segment, immutable after the first push;
  2. file 4's ICHRA selector, the literal `ICHRA`;
  3. the notice label.

  Role 3 is new coupling. Reversal: a label source separate from the segment.

**Reversal cost:** every rule above is a one-condition or one-string edit in `SummitExportServlet`.

**Verification:**

- Runtime-verified locally 2026-09-11: ICHRA-less sale → empty Plan Name; QSEHRA label;
  legal-name fallback; no-rows refusal; COBRA suppression; unparseable amount.
- Code-verified only: the WARN when more than one template matches; the all-false-rows refusal;
  the no-PSP refusal; the ICHRA-positive path, since local has no ICHRA template.

**Precedence: Part 14 > Part 13 > Part 12 > Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**
