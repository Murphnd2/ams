# ICHRA Setup Checklist — content for the task sequence

**Status:** Ready to enter — build-plan item 10
**Created:** 2026-07-31
**Owner:** Kevin
**Vehicle:** admin UI (Sequence Builder), **not** a migration — see "Why this is a document" below
**Build plan:** `docs/swbd_ichra_build_plan.md` item 10 · demo walkthrough §1 step 8

---

## ⚠️ Audience note — LA-13 does NOT apply to this document

**This document and the checklist it produces are read by SSA staff implementing a case.** That is an
internal audience.

`LA-13` keeps SSA's assumptions register out of **partner-agency-facing** output — the ICHRA design
advisor, whose audience is a licensed agent at SWBD. **It does not apply here, and the distinction
matters.** The PSP staffer working an ICHRA setup needs to know exactly which steps rest on
unreviewed assumptions and which rest on settled rules, because they are the person who will be asked
to defend the file. So `LA-NN` references, "not counsel-confirmed", and open-question status are
stated plainly throughout, and **must not be stripped**.

If any of this content is ever surfaced to an agent or an employer, LA-13 applies to *that* surface
and the register references come out. It is not surfaced today.

---

## Why this is a document and not a migration

**No migration in this repository has ever created a task sequence.** Verified 2026-07-31 by
searching all of `docs/migrations/` (V025–V080) for inserts into, or any mention of, `task`,
`tasksequence`, `tasksequencetable` or `requiredtasklist`: **zero hits**. Every task sequence in AMS
was created through the Sequence Builder admin UI (`SequenceAction25` / `sequenceManager25.jsp`).

Inventing that precedent here would also collide with build rule 5, which names task sequences as
Kevin's reference data. So item 10 delivers **the content, fully specified and ready to type**, and
Kevin enters it when he creates the ICHRA reference rows (item 4).

**Every field the admin UI asks for is filled in below.** Nothing here requires a developer.

---

## The mechanism, as verified 2026-07-31

Read before entering, because two facts change how the content is written.

**How a Setup gets its list — entirely by rows, no code:**

```
Application → ApplicationModule → ServiceItem → RequiredTaskList → TaskSequenceTable → Task → ToDo
```

`CreateSetup25.fillToDoList` calls `ApplicationTaskDAO.getTasksRequiredForApplication`, which resolves
`RequiredTaskList` rows by their `ServiceItem` FK (`purpose_id`). **A new checklist therefore needs no
Java.** Attach a `RequiredTaskList` to the ICHRA `ServiceItem` and every ICHRA Setup inherits it.

### ⭐ There is no due-date or offset field. Anywhere.

This is the single most important fact for this content, and it is a fortunate one.

| Entity | Date-ish fields it has |
|---|---|
| `Task` | **none** — `description`, `psp`, boolean flags, optional links, `task_guid` |
| `TaskSequenceTable` | **none** — only `sort_order` |
| `TaskSequence` | **none** — `description`, `psp`, `is_inactive` |
| `ToDo` (the per-case instance) | `date_completed` only — **no due date** |

**Consequence, and it resolves the LA-08 problem structurally rather than by discipline:** the ICHRA
notice step *cannot* carry a computed, estimated, defaulted or offset due date, because there is no
field capable of holding one. No placeholder is possible, so none can leak. Timing guidance lives in
the task's description text, where it can say "not determined" in words.

### The `description` field is `varchar(200)`

`Task.description` is declared `columnDefinition = "varchar(200)"`. **Every description below is
written to fit inside 200 characters** and has been length-checked. Do not expand them at entry time
without re-checking; the fuller reasoning is in this document, not in the task row.

---

## How to enter it

**Admin ▸ Sequence Builder** (`SequenceAction25`, `newSeqType = "setup"`).

| Field the UI asks for | What to enter |
|---|---|
| `newSeqType` | **setup** |
| `purposeId` | The **ICHRA `ServiceItem`** created in item 4. ⚠️ Do not attach this to the generic HRA ServiceItem — that would change the existing HRA checklist, which must stay untouched. |
| `seqName` | `ICHRA Setup` (the builder defaults the description from the ServiceItem; override it to this) |
| Task rows | The 19 tasks below, in the listed order. The builder assigns `sort_order = order × 10` automatically. |
| `reusable` per task | As marked per task below. |

**Flags a newly created task gets by default** (from `SequenceAction25`): `isSourced=false`,
`hasOwner=false`, `hasAutomation=false`, `hasGoTo=false`, `hasInfo=false`, `allowNonOwner=true`,
`allowEarly=true`, `allowFuture=true`. **These defaults are correct for every task below** — no task
here needs an owner, an automation, a Go-To link or BPO sourcing. Adjust later per case if wanted.

---

# The checklist

Legend for **Basis**: **Settled** = a rule written in `domain_and_compliance_rules.md`,
`ichra_strategy.md` §7, or `ichra_administration_scope.md`. **Assumption LA-NN** = rests on an entry
in `legal_assumptions.md`, not counsel-confirmed. **Undetermined** = SSA has written no rule; the step
exists anyway and says so.

---

## Group A — Plan documents and notices

### 1. Plan document, SPD and adoption agreement

> **Description to type:**
> `ICHRA plan document, SPD and adoption agreement / corporate resolution executed and on file before the effective date.`

- **Reusable:** No (ICHRA-specific)
- **Basis:** **Settled** — `ichra_administration_scope.md` Phase 2
- **Timing:** Before the effective date. Ordinary setup sequencing, no compliance deadline attached.

### 2. ERISA safe-harbor notice and posture

> **Description to type:**
> `ERISA safe-harbor notice issued. Confirm enrollment voluntary, employer endorses no issuer or plan, no employer consideration, employees told the policy is not an ERISA plan.`

- **Reusable:** No
- **Basis:** **Settled** — `ichra_administration_scope.md` Phase 2; `ichra_strategy.md` §7 ("ERISA safe harbor — presentation rules")
- **Timing:** Annual. The four conditions are the substance; all four must hold or the individual policies fall into ERISA.
- **Note for the implementer:** this is the step that constrains any plan-shopping UI — complete list, neutral ordering, employee-controlled sort/filter, no "recommended" badge, no default selection, no curation, no hidden carriers. Where exactly presenting becomes steering is **LA-06**, an assumption.

### 3. ⚠️ ICHRA employee notice — TIMING NOT DETERMINED

> **Description to type:**
> `ICHRA employee notice. TIMING NOT DETERMINED BY SSA (LA-08). Do not compute or assume a date; SSA sets it per case. Do NOT apply the QSEHRA 45/90-day runway - it does not transfer.`

- **Reusable:** No
- **Basis:** **Undetermined — LA-08, status "Open — no basis"**
- **Timing:** ⛔ **None. Deliberately.**

**This is the most delicate step in the checklist and the reason item 10 needed care.**

The ICHRA notice analysis **has not been done**, and it does **not** transfer from QSEHRA. Two things
are specifically unknown: whether the short-first-year drafting lever exists for ICHRA at all, and the
ICHRA notice content requirements, which are understood to be substantially heavier than QSEHRA's.

⚠️ **A trap in SSA's own documents.** `ichra_administration_scope.md` Phase 2 states *"Required 90
days before the plan year. For a newly established ICHRA the notice is due by the date coverage
begins."* **LA-08 identifies that exact assertion as carrying no citation anywhere in the doc set and
explicitly says it should not be relied on.** It appears in `plus_tier_build_plan.md` as O17 and in the
scope doc as if settled. **It is not settled.** Do not enter either figure as this task's timing.

**Who resolves it:** Kevin, via research (not necessarily counsel) — LA-08 records that reading
26 CFR §54.9802-4(c)(6) and the 84 Fed. Reg. 28888 preamble would likely resolve it at zero cost.
**Until then, no ICHRA sale should be quoted on a short runway.**

**Structural protection:** as established above, no `Task`, `TaskSequenceTable` or `ToDo` field can
hold a due date, so this step is incapable of carrying a fabricated one even by accident.

### 4. Per-employee eligibility notices

> **Description to type:**
> `Issue dated per-employee eligibility notices - the SEP proof artifact. A missed SEP window means no coverage for that employee for the plan year.`

- **Reusable:** No
- **Basis:** **Settled** — `ichra_administration_scope.md` Phase 2 and Phase 3
- **Timing:** Per employee, tied to their own eligibility date. Not a single plan-level deadline.

---

## Group B — Determinations

### 5. Affordability determination

> **Description to type:**
> `Run the affordability determination in AMS (uses on-exchange LCSP, V078). Employer- and agent-facing ONLY - never presented to an employee (LA-12).`

- **Reusable:** No
- **Basis:** **Assumption LA-12** for the audience rule; the computation itself is an AMS feature
- **Timing:** Before the offer is finalised, and again at each renewal against the new LCSP.

**Use the AMS affordability feature — do not hand-calculate.** The on-exchange LCSP columns shipped in
**V078**; before that, `lcsp_premium` was derived from off-exchange silver only, which **understates**
the LCSP and makes an unaffordable offer look affordable (**T44**). A manual calculation from the
older figures reproduces exactly the defect V078 fixed.

⚖️ **LA-12 is an assumption, not settled law:** computing this for a plan sponsor is administration;
putting the same number in front of an employee, whose premium tax credit turns on it, may be advice.
Employer- and agent-facing only.

### 6. Contribution structure recorded

> **Description to type:**
> `Record employer contribution amounts as designed by the agent, per class if classes are used. See checklist doc - SSA has no written rule on permitted ICHRA classes.`

- **Reusable:** No
- **Basis:** ⚠️ **Undetermined in part** — the recording step is ordinary setup; **SSA has written no rule anywhere on permitted ICHRA employee classes or minimum class sizes**
- **Timing:** Before the notice and before enrollment opens.

**Gap flagged:** `domain_and_compliance_rules.md` §5's "no classes and no opt-outs" is a **QSEHRA**
rule and must not be read across to ICHRA. `plus_tier_build_plan.md` **O38** asks whether Summit's
division-scoped rates constitute true class support and is unresolved. **If a case proposes classes,
route the class design itself to Kevin** — the checklist records what was decided, it does not
validate it.

---

## Group C — Enrollment and substantiation

### 7. Initial substantiation — coverage TYPE, not just presence

> **Description to type:**
> `Initial substantiation before the first reimbursement: proof of individual-market MEC or Medicare. Verify coverage TYPE - short-term, fixed indemnity and sharing ministries are NOT MEC.`

- **Reusable:** No
- **Basis:** **Settled** for the type rule — `ichra_strategy.md` §7 ("Substantiation must verify coverage *type*, not just coverage *presence*"); `ichra_administration_scope.md` Phase 3
- **Timing:** Before the first reimbursement of the plan year.

**ICHRA is narrower than QSEHRA here and the direction is easy to get backwards.** ICHRA requires
individual-market coverage or Medicare and **locks out** a participant holding other MEC; QSEHRA
requires MEC from *any* source. Off-exchange ACA individual-market coverage **is** MEC and is not a
downgrade. Reimbursements paid against non-MEC coverage are taxable and the employer carries it.

### 8. Plan-year coverage baseline

> **Description to type:**
> `Record the plan-year coverage baseline: carrier, coverage effective date, covered individuals. Rests on SSA assumption LA-02, not counsel-confirmed - confirm field scope with Kevin.`

- **Reusable:** No
- **Basis:** **Assumption LA-02** — that the per-payment attestation confirms *continuation* only and does not replace an annual plan-year baseline
- **Timing:** At or before the first reimbursement of each plan year.

⚠️ **LA-02 has an unresolved sub-question that bites at exactly this step:** how much **dependent
detail** a baseline actually needs. Names? Count? Relationship? Nobody has established the floor, and
LA-11's data-minimisation is load-bearing — narrowing a captured field later does not un-capture it.
**Confirm the field list with Kevin before the first baseline is collected, not after.**

### 9. Attestation and release posture

> **Description to type:**
> `Confirm the reimbursement release rule for this case with Kevin before the first payment. Rests on SSA assumptions LA-01/LA-02/LA-03 - none counsel-confirmed.`

- **Reusable:** No
- **Basis:** **Assumptions LA-01, LA-02, LA-03**
- **Timing:** Before the first reimbursement release.

**LA-01 is flagged in the register as the assumption whose being wrong costs the most, and as
irreversible once a dollar has moved.** The working assumption is that a signed attestation of
continued MEC suffices to release, without third-party carrier documentation; LA-03 conditions that on
SSA not holding actual knowledge the attestation is false, with any lapse signal overriding it.
**None is counsel-confirmed.** This step exists so the decision is made deliberately per case rather
than inherited silently.

### 10. Effectuation confirmed

> **Description to type:**
> `Confirm each member effectuated with the carrier. Until premium is paid status is pending_effectuation - not covered and cannot be reimbursed. Human step; no API pre-validates this.`

- **Reusable:** No
- **Basis:** **Settled** — `ichra_administration_scope.md` Phase 3
- **Timing:** Ongoing until every member is effectuated.

**Named in the scope doc as the most likely failure mode in the whole flow.** Enrollment does not end
on SSA's system — carrier payment is a browser form. Chasing non-effectuated members is a staffed
task.

---

## Group D — Summit configuration

Sourced from `plus_tier_build_plan.md` Part 7, "The ICHRA setup checklist now has real content".

### 11. Create the ICHRA benefit with PCOR Reportable checked

> **Description to type:**
> `Create the ICHRA benefit (CDH) in Summit with PCOR Reportable CHECKED. PCORI applies to ICHRA and the flag is otherwise missed silently at year end.`

- **Reusable:** No
- **Basis:** **Settled** — `plus_tier_build_plan.md` Part 7 item 1
- **Timing:** At setup, before the first import.

**This is the step that makes task 16 (PCORI) possible.** Miss the checkbox here and the PCORI
obligation surfaces nowhere.

### 12. Create the notice benefit

> **Description to type:**
> `Create the notice benefit (Premium Billing LOS). Set the Import Plan ID per convention and verify it matches before the first import runs.`

- **Reusable:** No
- **Basis:** **Settled** — `plus_tier_build_plan.md` Part 7 items 2 and 5

### 13. Verify plan-year alignment

> **Description to type:**
> `Verify plan-year alignment: the start date cannot fall after billing effective dates, and gaps between plan years are not permitted. A mid-year date needs deliberate setup.`

- **Reusable:** Yes (applies to any plan-year setup)
- **Basis:** **Settled** — `plus_tier_build_plan.md` Part 7 item 3

### 14. Verify notice settings per benefit

> **Description to type:**
> `Verify notice settings per benefit: Mailed Letter ON, DataPath Fulfillment ON, Portal Mobile OFF, Push As Alert OFF. Required configuration - these are not safe defaults.`

- **Reusable:** No
- **Basis:** **Settled** — `plus_tier_build_plan.md` Part 7 item 4
- **Why it matters more than it looks:** participants not seeing the notional benefit is **configured, not inherent**. Portal Mobile displays the communication on the Participant Portal and Push As Alert notifies at next login. Anyone tidying notice settings later could switch them on.

---

## Group E — Annual compliance

### 15. 1094-B / 1095-B filing

> **Description to type:**
> `1094-B / 1095-B filing - ICHRA is MEC and the plan sponsor reports. Source the data from Summit exports, never from CoverageStatus.`

- **Reusable:** No
- **Basis:** **Settled** — `ichra_administration_scope.md` Phase 5; data-source rule from `domain_and_compliance_rules.md` §2
- **Timing:** Annual, per IRS filing deadlines. ⚠️ SSA has written no rule recording those deadlines — see "What is missing" below.

⚠️ **`CoverageStatus` is not a valid compliance data source** — billing-driven, no backfill, no run
log. This is a settled rule and it applies directly here.

### 16. PCORI fee

> **Description to type:**
> `PCORI fee on Form 720, annually by July 31. Small, recurring and penalty-bearing. Depends on PCOR Reportable being set when the benefit was created.`

- **Reusable:** No
- **Basis:** **Settled** — `ichra_administration_scope.md` Phase 5 (the July 31 date is stated there)
- **Timing:** **July 31 annually.** The one hard date in this checklist that the repo actually records.

### 17. §105(h) nondiscrimination testing

> **Description to type:**
> `Section 105(h) nondiscrimination testing. SSA has not written a rule for its scope or timing - confirm the approach with Kevin before the first test.`

- **Reusable:** No
- **Basis:** ⚠️ **Undetermined** — `ichra_administration_scope.md` Phase 5 names the obligation but records no scope, method or deadline
- **Timing:** Not determined by SSA. Named, not dated.

### 18. W-2 reporting

> **Description to type:**
> `W-2 reporting for ICHRA. NOT the QSEHRA Box 12 Code FF item. SSA has not determined the ICHRA W-2 treatment - confirm with Kevin before year end.`

- **Reusable:** No
- **Basis:** ⚠️ **Undetermined** — Phase 5 states only what it is *not*
- **Timing:** Not determined by SSA.

### 19. Termination and state continuation

> **Description to type:**
> `On termination: ICHRA loss is itself a SEP. Federal COBRA does not reach small employers; state continuation may. Needs a one-time read per state, then a standing rule.`

- **Reusable:** No
- **Basis:** ⚠️ **Partly undetermined** — `ichra_administration_scope.md` Phase 4 records the SEP fact as settled and the state-continuation question as needing a per-state read that has not been done
- **Timing:** On the termination event.

---

## Renewal

Renewal is not a setup task — it recurs. When a renewal sequence is built, it must carry:
recompute affordability against next year's LCSP (task 5), reset contributions (task 6), and reissue
the notice **on timing SSA has not yet determined** (task 3, LA-08). Source:
`ichra_administration_scope.md` Phase 5.

---

## What is missing — compliance content the repo has no rule for

**A genuine output of this run.** These are named obligations with no SSA-written rule behind them.
Each is a task above carrying an explicit "not determined" marker rather than a fabricated answer.

| # | Gap | Consequence | Who resolves |
|---|---|---|---|
| 1 | **ICHRA notice timing** (task 3) | The headline gap. LA-08, "Open — no basis". Blocks quoting any short-runway ICHRA sale. | Kevin — research, likely zero cost |
| 2 | **Permitted ICHRA classes / minimum class sizes** (task 6) | No rule anywhere. QSEHRA's "no classes" must not be read across. O38 open on whether Summit supports classes natively. | Kevin |
| 3 | **§105(h) scope, method and deadline** (task 17) | Obligation named, never specified. | Kevin |
| 4 | **ICHRA W-2 treatment** (task 18) | Repo records only what it is not. | Kevin |
| 5 | **1094-B/1095-B filing deadlines** (task 15) | The obligation is recorded; the dates are not written anywhere in the repo. | Kevin |
| 6 | **State continuation on ICHRA loss** (task 19) | Needs a one-time read per state; none done. Plural already, given SWBD's footprint. | Kevin / counsel |
| 7 | **Reimbursement payment rail** | Not a checklist gap but a hard blocker downstream — `ichra_administration_scope.md` Phase 4 and backlog #38 both record that **no reimbursement payment rail exists in AMS today.** Tasks 7–9 substantiate a payment the platform cannot yet make. | Kevin — architecture |

**Gap 7 is worth reading twice.** The checklist can be entered and worked end-to-end today; the
release step at the end of it has no rail behind it.

---

## What this deliberately does not do

- **Does not touch the generic HRA checklist or any existing sequence.** Additive only — a new
  `RequiredTaskList` on a new `ServiceItem`.
- **Does not compute, default, or offset any date.** Structurally impossible; see the mechanism note.
- **Does not carry LA-07's QSEHRA figures.** Those are QSEHRA answers. If a QSEHRA sequence is built
  later it gets its own content, with all three LA-07 qualifications stated: the 45-day figure is
  operational not legal, **January 1 is the hardest first-year date and not the easiest**, and an
  effective date is **never** backdated to cure a late notice.
- **Does not depend on item 4 having been done.** The content is entered when the ICHRA `ServiceItem`
  exists; nothing here blocks on it, and nothing here breaks if it never appears.

---

## Related

- **Build plan:** `docs/swbd_ichra_build_plan.md` item 10
- **Service scope, Phases 2–5:** `docs/business/ichra_administration_scope.md`
- **Settled rules:** `docs/analysis/domain_and_compliance_rules.md` · `docs/ichra_strategy.md` §7
- **Assumptions LA-01…LA-13:** `docs/analysis/legal_assumptions.md`
- **The zero-ICHRA-steps finding:** `docs/analysis/phase_a_ichra_enrollment_portal.md` Q4
- **Summit configuration detail:** `docs/analysis/plus_tier_build_plan.md` Part 7
