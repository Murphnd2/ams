# Session 15 close-out — HealthSherpa capability research and documentation reconciliation

**Work performed:** 2026-08-04 · **Session closed:** 2026-08-05
**Branch:** `refactor/modernize-architecture` · **Commits:** `848f1cb`, `4c323ae` (both pushed)
**Code changed:** none · **Migrations:** none · **Releases:** none

> **Read this first if you are picking up the ICHRA workstream.** This session produced no code. It
> produced a body of API research that **falsified six recorded claims**, dissolved a design premise
> that backlog #38 rested on, and escalated a counsel question from "confirm" to "gates whether the
> feature has a compliant form at all." Nothing was built; several things that were believed are now
> known to be false.

---

## 1. What this session was

A capability discussion about HealthSherpa that turned into **O2** — the open item filed 2026-07-29
as *"re-verify the enrollment/status API surface against the ICHRA Partner API; public docs, no
account needed, ~1 hour"* — and then into a full documentation reconciliation.

**The headline finding is procedural, not technical.** O2 sat open for six days, estimated at one
hour. Doing it answered most of what the project had been recording as *vendor-gated*: the enrollment
surface, the poll design, the payment model, the effective-date rule, the carrier matrix, and half of
O16. **"Blocked on HealthSherpa" was, for a large part of the register, blocked on nobody.**

---

## 2. Actions taken, in order

| # | Action | Output |
|---|---|---|
| 1 | Read the ICHRA/HealthSherpa document set — `ichra_strategy.md`, `swbd_ichra_build_plan.md`, `docs/business/*`, sessions 10–14 close-outs | Established current state |
| 2 | Verified the demo walkthrough against **code**, not docs | Found the step-6 gate (below) |
| 3 | Discussed rate-cache economics: storage vs on-demand vs none | Cost model; **T152**, D-83 reframe |
| 4 | Discussed off-exchange rate volatility | Annual-filing finding; **silver loading**; **T151** |
| 5 | Discussed HealthSherpa's data sets, environments and credential model | Staging epistemics; the accountability-gate reframe |
| 6 | **Executed O2** — read 13 of the 25 pages at `docs.ichra.healthsherpa.com` | Six corrections; **T144–T149** |
| 7 | Re-read after under-reading the enrollment side | EnrollConnect, effective dates, webhooks parent, changelog |
| 8 | Updated nine documents | Commit `848f1cb` |
| 9 | Added **Part 11** to the canonical build plan | Commit `4c323ae` |
| 10 | Drafted the HealthSherpa follow-up email | §7 below — **not sent** |

### Documents written

**Commit `848f1cb`** — 9 files, +779/−17:

- **`docs/business/healthsherpa.md`** (+540, 957 → 1497) — two new dated sections: **2026-08-04**
  (O2: the enrollment/status surface) and **2026-08-04 (b)** (rate stability, silver loading, staging
  epistemics, the one-data-set finding)
- **`docs/analysis/project_backlog.md`** — T144–T152 filed; T127 raised LOW → MED
- **`docs/business/README.md`** — HealthSherpa row rewritten; build-plan row corrected
- **`docs/business/ichra_platform_capability_map.md`** — Layer 3 and Layer 4
- **`docs/business/ichra_administration_scope.md`** — effective-date claim confirmed; endpoint path fixed
- **`docs/ichra_strategy.md`** — O2 closed, §4 staleness banner, §10 register sharpened, §3 row 5
- **`docs/deployment_backlog.md`** — **D-83** reframed, **D-84** blocked-prerequisite block
- **`docs/swbd_ichra_build_plan.md`** — §1 step-6 correction
- **`docs/claude_memory.md`** — session entry

**Commit `4c323ae`** — 3 files, +201/−5:

- **`docs/analysis/plus_tier_build_plan.md`** — **Part 11** added and governing; header corrected
- **`docs/ichra_strategy.md`** and **`docs/business/README.md`** — propagated header error fixed

---

## 3. Decisions made this session

**Distinguishing what was decided from what was recommended matters here** — this project's recurring
failure mode is assumptions that read as findings.

### 3a. Decisions Kevin made

| # | Decision | Consequence |
|---|---|---|
| **1** | **Reopen the provenance-gate decision.** A time-boxed, agency-scoped PSP-admin override that treats staging rates as presentable for the Forrest demo | ⚠️ **Reverses session 10's *"settled and not open for relitigation."*** Deliberately reopened, not drifted into — the earlier decision concerned employer-facing figures on an unauthenticated public link; this is an agent-facing control on an authenticated page with three independent gates behind it. Filed as **T150** with a design sketch. **Not specced, not built** |
| **2** | **Do O2 before building anything** | Closed the highest-leverage open item in the workstream |
| **3** | **Update all documents from this session's discussion and research**, not only the O2 findings | Produced commit `848f1cb`'s scope |
| **4** | **Take the `plus_tier_build_plan.md` pass** rather than the email first | Produced Part 11 |
| **5** | **Keep T148 and the T127 severity raise**, despite both originating in the rate-cache discussion rather than O2 | Two November-dated items are now tracked rather than lost |
| **6** | Commit as doc commits on trunk; push | `848f1cb`, `4c323ae` pushed |

### 3b. Decisions recorded in the documents (status changes, not new choices)

| # | Change | Where |
|---|---|---|
| **D20** | ⬆️ **Upgraded from a decision to a requirement.** `external_id` is unique per platform and both create and submit are non-idempotent, so AMS-side dedup is structural, not hygiene. **Stop describing it as a choice** | Part 11 |
| **O20** | ⬆️ **Escalated from a confirmation item to load-bearing** — see §5 | Part 11 |
| **D17** | `HS_POLICY_STATUS` rung made concrete per carrier | Part 11 |
| **D16** | Strengthened — off-exchange webhook payload is a superset of on-exchange | Part 11 |
| **D38** | Independently reinforced by the silver-loading mechanism | Part 11 |
| **Precedence** | **Part 11 governs**; header moved to Revision 9 | `plus_tier_build_plan.md` |
| **T127** | LOW → MED | `project_backlog.md` |

### 3c. Recommendations made, NOT decided — these are still open for Kevin

1. **Warm all 254 Texas counties and move refresh to plan-year cadence** plus a drift check (**T152**;
   D-83 reframed). Recommended, not adopted.
2. **Enable the step-6 hand-off on staging data**, since its gate is redundant with three downstream
   gates. Recommended; **T150** is the vehicle and it is unspecced.
3. **Whether the public proposal keeps a demo-data marker.** Explicitly left to Kevin.
4. **Send O20 to counsel with O18 and O17**, not after them.
5. **Michael Levin's place on the email thread.**

---

## 4. Open items in `healthsherpa.md` clarified this session

| Item | Prior state | Now |
|---|---|---|
| **O2** | Open since 2026-07-29, est. 1 hour | ✅ **RESOLVED.** Three of its four sub-questions **dissolved** (they were HSOne concepts); the fourth promoted to **O41** rather than closed silently |
| **Open question #4 — UHC** | *"Quotable but not API-enrollable in TX"* | ❌ **FALSIFIED.** UHC has been API-enrollable in TX since **2026-06-09**, seven weeks before the claim was written. **Wrong when written, not superseded** |
| **`employer_external_id` + `updated_since` polling** | Recorded as backlog #38's *"concrete unblock"* | ❌ **Does not exist on this product.** Replaced by push webhooks carrying `paid_through_date`, correlated on AMS's own `external_id`. **Stronger, but a different shape** — any pull-by-employer design needs rewriting. Affects Part 1 **C4** |
| **O14** — deeplink self-service or agent-driven | Open | ⚠️ **Partially answered, leaning agent workstation.** EnrollConnect carries four `agent_*` attestations plus `broker_signature_attestation` and `consumer_working_with_agent`. **Not closed** |
| **O15** — webhook auth methods | Open | ⚠️ **Sharpened.** The *setup process* is public. The **delivery semantics** are not — retry, guarantees, ordering, idempotency, signature verification, IP allow-listing. Those are the half that changes AMS's code |
| **O16** — BCBS TX / CHRISTUS policy status | Open | ⚠️ **Half-answered from public material.** BCBS TX published as *"coming in 2026"* — **year confirmed, month not.** **CHRISTUS is absent from the matrix entirely**, not marked "coming" |
| **O20** — ERISA posture on an AMS-built display | "Confirm with counsel" | ⬆️ **LOAD-BEARING.** See §5 |
| **O13** — BAA | Open | **Scope changed, not status.** The deeplink **accepts `ssn`**, so PHI minimisation is a design choice AMS makes, not a property of the rail. EnrollConnect's PHI surface is materially larger than recorded |
| **Effective-date pre-validation** | Recorded as *"a human step"* | ✅ **CONFIRMED** — *"No endpoint to query valid dates ahead of time."* The existing claim was right |
| **`tpa_slug` rejected if caller-supplied** | Recorded as a hard constraint | ❌ **HSOne's constraint.** Accepted on this product, inside the HRA object |
| **`pending_effectuation` as a Policy Status value** | Recorded | ❌ **Wrong webhook.** It is a Submission Confirmation value |
| **"Enrollment cannot be fully headless"** | Recorded as universal | ⚠️ **Carrier-dependent.** ACH can be set server-side |
| **Deeplink path `/ichra/off_ex`** | Recorded | ❌ **`/public/ichra/off_ex`** — third path-segment miss in this document set |
| **New** | — | **O41** (employer filtering on `GET /api/v1/applications`), **O42** (`_agent_id` required or not) — **both settle from the OpenAPI YAML, no credential; neither belongs on the vendor register** |

---

## 5. The three findings that change what gets built

**1. `plan_hios_id` is required on every enrollment route** — deeplink, EnrollConnect, and Deeplinks
V2, which states *"no shopping/browse experience is supported."* **The plan display cannot be handed
to HealthSherpa.** So O20 stops being a confirmation item and becomes a gate on **B5 existing at
all**, and the counsel question changes shape: not *"is a curated list an endorsement problem?"* —
nobody proposes curation — but **"does SSA building a complete, neutral, uncurated display on the
employer's behalf itself constitute employer endorsement?"** If yes, **B5 has no compliant form on
this rail.**

**2. Silver loading probably explains V078's 44% on-vs-off LCSP gap.** Off-exchange-only silver
*mirror* plans without the CSR load — **different plans, not different prices for one plan.** Makes
T44/V078 **systematic infrastructure, not belt-and-braces**, and independently reinforces Part 9's
D38. ⚠️ **[inference], untested** — the discriminating probe is two staging calls (**T151**).

**3. The demo breaks at step 6.** `illustration25.jsp:991-1003` renders *"Use This in a Proposal"* as
a **disabled button** unless `sourceEnv == 'PRODUCTION'` — shipped that way in item 7's own commit
(`e5b2009`), **the same day** `swbd_ichra_build_plan.md` §1 declared *"the demo has no external
gate."* Its gate is **redundant**: `ViewProposal` refuses staging data at three further points
(`:142`, `:467`, `:796`), so enabling it puts no staging figure in front of an employer.

---

## 6. Two errors of mine, corrected in place

Recorded because this session's whole subject was other people's unverified claims.

1. **T148's first draft said a missing age curve produces "no error."** False —
   `RateCacheWarmService.java:252-256` logs at **ERROR** per skipped year, and **D-84 already said so
   correctly.** Caught by reading the code before editing D-84. The row carries the correction inline.
   The HIGH severity was based on the 39 unverified curve factors, not the silence, so it stands.
2. **I "fixed" `README.md`'s build-plan row to "Revision 6 / Part 8 governs"** — copying the build
   plan's own header, which was stale because Parts 9 and 10 had shipped without a header bump.
   `ichra_strategy.md` §15 had the same error; mine was the third copy. All four now read **Revision 9
   / Part 11 governs**. ⭐ **Rule now written into §15: read a document's last section heading, not its
   self-description.**

---

## 7. The HealthSherpa follow-up email — drafted, NOT sent

Kevin's mailbox was not touched. Full draft is in the session transcript; the ask list is:

1. **Onboarding representative** (O12) — gates most of the rest
2. **Production allow-listing** (T136) — production 403s today
3. **Staging deeplink Basic Auth**
4. **Webhook delivery semantics** — auth method, retry policy, delivery guarantees, ordering,
   `transaction_id` idempotency, signature verification, fixed IP range
5. **BAA** — Geozoning, Inc. DBA HealthSherpa
6. **BCBS TX policy status — which month?** and **CHRISTUS — planned at all?**
7. **Quoting rate limits**
8. Plus a documentation-inconsistency note on `_agent_id` (goodwill; also O42)

**Deliberately dropped from the 29 July list:** deeplink self-service vs agent-completed, and the
deeplink-vs-EnrollConnect steer — both now largely answerable from their own docs. **Re-asking would
signal we hadn't read them.**

**Contacts:** Julian Ferdman (`julian.ferdman@healthsherpa.com`), KJ Sherman
(`kj.sherman@healthsherpa.com`). Michael Levin was CC'd 2026-07-29 without introduction — **open
question whether he stays on the thread.**

---

## 8. What remains open

**Kevin's, external:**

- **Send the HealthSherpa email** (drafted, above)
- **3 SWBD emails to Forrest** — longest-standing open item in the project, unchanged this session
- **T136** — production HealthSherpa allow-listing
- **Reference-row configuration** — two `LOS` rows now (D39), priced `ServiceModule` → `RateTable`

**Dated, with a November deadline:**

- **T148** — `AgeCurve` has no 2027 curve; **D-84's plan-year seeding is a no-op without a code
  change**, and only 5 of the 2026 curve's 44 factors were ever verified
- **T127** — once two plan years are live, **sort order in a constant silently decides the plan year**

**Cheap and unblocked, no credential:**

- **T145** — diff the recorded contract against the OpenAPI YAML. **Would settle O41 and O42 as a side
  effect.** Nothing from this session is safe to code against until this runs
- **T151** — the on/off exchange overlap and same-plan price probe (two staging calls)
- **O1** — the Gate 0 probe, still never run

**Needs a decision or a spec:**

- **T150** — the demo override: requirement and design sketch only
- **T152 / D-83** — cadence and full-state warm: recommended, not adopted

**Not read, and it is the largest remaining documentation gap:** 12 of 25 HealthSherpa doc pages,
highest-value being **Carrier-Specific Info**, **Integration Scenarios**, **Use Cases** and **FAQs**.

---

## 9. Standing caveat

Every HealthSherpa finding in this session came from **fetched-and-summarised documentation pages,
not from parsing the OpenAPI YAML** each API reference page has linked since 2026-07-09. This
document set has now been bitten **four times** by exactly what prose drops —
`fips_code`/`fip_code`, `uses_tobacco`/`smoker`, `/v1/quotes`/`/api/v1/quotes`, and
`/ichra/off_ex`/`/public/ichra/off_ex`.

**Treat nothing from this session as safe to code against until T145 runs.**
