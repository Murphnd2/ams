# Session 27 close-out

Date: 2026-09-08 (work spanning 2026-09-07/08). Branch: `refactor/modernize-architecture`. Baseline at
session start: `caf2f53` (`docs: sync claude_memory.md for session 26`). Completes the Summit setup
export — multi-row sink, file 4, and a UI entry point — then **runs it for the first time**. Ends at
this commit.

⭐ **The headline: `SummitExportServlet` had never executed. All three emitters are now
runtime-verified.**

---

## 1. Shipped

Three commits.

| Commit | What |
|---|---|
| `8bcde0e` | Multi-row `writeFile` overload, file 4 Demographics, three PSP-admin-gated export links |
| `ceb8d7e` | S27-D findings: setup keys on `ServiceItem`; D40; O43; T179–T184 |
| `5e470c6` | Summit plan types and templates, `125 PI Elections`, contribution schedules, claim rules; SDX-11–13; D-89 |

Eight sub-runs produced this. **S27-A** added the multi-row sink as an overload, leaving both existing
call paths untouched. **S27-B** **hard-stopped correctly on its first attempt** — the Summit spec
carried two different Demographics column lists and the run refused to pick one; rev2 built to the
eleven-column layout after Kevin settled it by decision. **S27-C** added the entry point. **S27-D** and
**S27-E** were read-only investigations into what actually keys a task sequence. **S27-F** committed the
code and filed six defects. **S27-G** recorded the Summit-side configuration. **S27-H** is this
close-out.

**Production released at `v0.94.00`**, applying **V092, V093 and V094**. ⭐ **That clears the pending
state all three had been in since they were authored — production is no longer behind on any migration
in this tree.** See §6.

---

## 2. Runtime verification — the session's headline

> **Evidence class: Kevin's report from a browser walk on production (`v0.94.00`) and local,
> 2026-09-08. Not independently verified — no session tooling reached a browser or a database.**

`SummitExportServlet` had never executed in its entire existence. It needed three things that had
never been in place at once: `SUMMIT_TPA_ID_PREFIX` and `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` in
`ssa.properties`, a Tomcat restart, and `plan_year_eligibility` attached to the LOS being sold. All
three emitters now have a real file behind them.

### File 1 — Employer Demographic

```
Red Creek Solutions|158-136748|3852 Idlebrook Dr.|Frisco|TX|75034
```

Six fields, UTF-8, Unix LF. ⭐ **`158-136748` confirms `SUMMIT_TPA_ID_PREFIX` loaded** — the prefix is
being read from config at request time, not defaulted or hardcoded.

### File 2 — Employer CDH Plan

```
1030|ICHRA 2026|158-136748-ICHRA-2026|ICHRA Plan 2026 for Red Creek Solutions|20260901|158-136748|20260901|20270831
```

Eight fields. ⭐ **`1030` confirms `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` loaded**, and **field 6 matches file
1's employer key exactly** — the join between the two files holds, which was the specific thing S25-C
built `resolveEmployerTpaCustomId` to guarantee.

**Two defects fell out of this one row** — see T185 (the plan year inside an upsert key) and T186 (the
static year in the plan name). Neither is a regression; both are design questions the row made visible.

### File 4 — Demographics

**38 rows**, participant ids `158-P-39` through `158-P-76`, **contiguous**. Rows ordered by last name
while the ids run in insert order, which is `EmployerParticipantDAO.findByProspectId`'s
`ORDER BY p.lastName, p.firstName, p.id` behaving as written. Eleven fields per row.

Four things the walk proved that code review could not:

- ⭐ **Empty address-2 is present and delimited, not dropped.** This was the entire reason the
  eleven-column layout puts both nullables ahead of `Effective Date`.
- ⭐ **Email populated throughout** — which retroactively **vindicates the eleven-column choice over
  the tested nine**. Had the column been omitted, real data would have been silently discarded.
- **Hyphenated surname (`Pipestem-Ott`) intact**, no delimiter collision.
- ⭐ **Red Creek's duplicated address survived untouched** — `Lori|King|8593 Kosmal Lane|8593 Kosmal
  Lane`. That is the emit-as-is decision working exactly as specified: the roster holds what the
  employer's file held, and the exporter does not compare, de-duplicate, refuse or flag it.

**Empty-roster behaviour confirmed** on production, where no roster exists: file 4 emitted a **valid
empty file without throwing**, as S27-B specified. This also corroborates that V094 reached production
— the query could not have succeeded against a missing table.

### ⚠️ What is still not verified

**No file has been imported into Summit. Generation is proven; acceptance is not.** Every field
requirement in `summit_data_exchange.md` was established by importing a file and reading the results
file, and none of these three files has been through that. The Summit-side Demographics template also
still carries nine columns, not eleven (D-89).

---

## 3. Decisions made

- **D40 amended, not replaced.** The ICHRA half is reversed: the **standard `ICHRA` plan type is
  reused**, with the card difference carried by the **`ICHRA+` template (1030)**. A plan type drives
  only the renewal checklist, and an ICHRA renews identically with or without a card — card-on/off is
  a template setting, and heavier funding or higher opt-out rates are **data differences, not task
  differences**. **The `Ins125+` split stands and carries the load**, because the existing `Ins125`
  serves testing-only groups and the task union is additive with no subtraction. **`I_NOTICE` and
  `Q_NOTICE` added** as separate types.
- **`SUMMIT_TPA_ID_PREFIX` = `158`**, from the DataPath-assigned TPA ID. Chosen for **uniqueness, not
  routing** — FTP credentials handle routing. It closes a scope that cannot be tested: the participant
  global-uniqueness finding used two employers **under one TPA**, so whether Summit's participant
  namespace is per-TPA or instance-wide remains unproven, and a TPA-derived prefix is correct under
  either answer. ⚠️ **Effectively irreversible now that a real employer key has been emitted** — it is
  the leading segment of both upsert keys.
- **Eleven-column Demographics layout** over the tested nine, both nullables ahead of `Effective Date`.
  **Vindicated by the walk** (§2).
- **File 4's `Effective Date` sources `EmployerParticipant.effectiveDate`**, not file 2's plan-year
  derivation — it is per-participant and **survives mid-year hires**, which a plan-year-wide date
  would not.

---

## 4. New assumptions and open questions

Carried forward from this session:

| Ref | Question |
|---|---|
| **SDX-11** | Is `Schedule Name` unique TPA-wide or per-employer? Same shape as the participant-key trap — a per-employer object in a global namespace |
| **SDX-12** | What is `125 PI Elections`' true required field set? Never imported |
| **SDX-13** | Is `Employer Contribution Schedule` meaningful on a participant-funded plan? |
| **O43** | Do Premium Billing plans mirror into AMS as `Benefit` rows? If not, neither notice type can drive a renewal checklist |
| **T185–T187** | The three walk defects — see §5 |
| **T179–T184** | The six defects filed in `ceb8d7e` from S27-D/S27-E |

### ⚠️ New, and larger than it looks: AMS cannot know what Summit already holds

**Red Creek's census already exists in Summit under keys from the pre-existing process, not
`158-P-{id}`.** AMS derives its participant key from **its own AUTO_INCREMENT sequence** and has no way
to discover what Summit already holds for that employer. **Importing file 4 for Red Creek would
therefore create duplicate participants rather than update existing ones.**

**This is a general problem for any employer already in Summit, not a Red Creek quirk** — and every
existing SSA client is in that category. It does not affect a genuinely new employer, which is the
case the setup sequence was designed around. **Recorded, not solved here.** It interacts with LA-33
(the key is derived, never stored) and with the "AMS emits full current state, not deltas" position:
both are correct for a new employer and neither addresses an employer with a pre-existing Summit
identity.

---

## 5. Defects found in the walk

Three, all filed in `project_backlog.md`, **none fixed**:

- **T185 — `Import Plan ID` embeds the plan year, and it is an upsert key.** `158-136748-ICHRA-2026`
  becomes `...-ICHRA-2027` at renewal, which Summit reads as a **different plan** — renewal would
  create a second plan rather than renew. Whether the year belongs in the key depends on whether Summit
  models one plan carrying successive years or a plan per year, **which is unestablished**. Compounds
  the existing recorded suspicion that per-employer `Import Plan ID` uniqueness was accepted on thin
  evidence. **Belongs to the file 2 row-set build.**
- **T186 — the CDH plan name is static and carries a year the plan-year fields already hold.**
  `ICHRA Plan 2026 for Red Creek Solutions` would still read 2026 in 2027. ⚠️ **Sequence after T185** —
  the name should follow whatever T185 settles about the key, not pre-commit it.
- **T187 — mojibake in the three export link labels.** `Summit file 1 â<box><box> Employer`: a UTF-8 em
  dash decoded as Latin-1. **Only the new markup is affected** — the adjacent Census Upload button and
  the checklist render clean, so it is those bytes, not a page-wide charset problem. Simplest fix is a
  plain hyphen. Cosmetic, PSP-admin-only, independent of the other two.

---

## 6. Contradictions found

- **"Fully per-LOS driven" was incomplete in three documents** (`plus_tier_build_plan.md`,
  `swbd_ichra_build_plan.md`, `section125_los_phase_a.md`). The setup axis is **`ServiceItem`**, reached
  from `LOS`, `Enhancement` **and** `AddSetupModule25`. The narrow claim holds; the broad reading does
  not. Corrected in `ceb8d7e`, originals retained.
- **`ICHRA`, `Ins125` and `MERP` were recorded as native Summit plan types.** They are **TPA Custom** —
  Kevin created them, and they appeared in the picker because they already existed in this tenant.
  Corrected in `ceb8d7e`, **confirmed visually by the Level column** in the Summit plan-type list.
- **Files 6 and 7 of the setup sequence were written against HRA Enrollment**, which is HRA-only.
  **`125 PI Elections` is the correct type.** Corrected in `5e470c6`.
- **S27-D reached a correct conclusion for an incomplete reason.** It read only the XLSX importer
  branch and generalised. The dead CSV branch assigns a **category-2** ServiceItem directly to a
  PlanType (**T182**) — the conclusion survives only because that branch is disabled.
- ⚠️ **Still uncorrected, flagged by S27-G and repeated here because it is now on the critical path:**
  the **file 2 plan table** in the setup sequence names plan type **`Ins125`** for the two card plans;
  under amended D40 they are **`Ins125+`** (templates 1031/1032). **This is the table someone reads when
  building file 2's row set — which is the next code item.**
- **Also stale:** file 4's "**This is blocked**" note in the setup sequence, which says AMS has no
  AMS-generated employee key. Session 26 (V094) and S27-B resolved that, and this session emitted 38
  rows through it.
- ⚠️ **New, found while updating the tracker:** `migration_tracker.md` shows **`beta_ssa` (work) as ⬜
  for V092–V094**, while project memory records all three as **applied locally in session 26 (S26-E)**.
  **One of the two is stale and this session did not settle it** — no database query was run. Left
  unflipped deliberately; flipping a cell on memory alone is the exact failure the tracker's own
  maintenance note exists to prevent.

---

## 7. Next

**File 2's row set.** It now carries **three** reasons to be next:

1. It **emits one row where the spec requires one per plan** — the multi-row sink exists (S27-A) and
   has no consumer.
2. **T185 and T186 both live there**, and T185 gates T186.
3. The **stale `Ins125` table row would mislead whoever builds it** (§6).

**The config shape question arrives with it.** Templates **1031** and **1032** have no consumer yet, and
**one property per template id does not scale past a handful** — `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` is
already carrying a known limitation, since the `ICHRA` type now has two active templates (1009 and
1030) and every sale points at 1030.

Also open and cheap: **T187** (one-line label fix), and **D-89's remaining Summit-side work** — the
Demographics template must be mapped to eleven elements in order before an emitted file will import.

---

## 8. SQL close-out audit

**Session 27 produced no SQL.** No migration was written, no `.sql` file created, no schema change
described, and **no session run connected to a database** — S27-D and S27-E were explicitly forbidden
from doing so and complied.

- **Highest migration in the tree: V094.** Unchanged all session.
- ⭐ **V092, V093 and V094 are now applied to production** via the `v0.94.00` release. All three had
  been pending since they were authored. **`migration_tracker.md` updated accordingly** — this is the
  backfill its maintenance note demands be done as part of landing the deploy, not as a follow-up.
- **Environments other than production:** Demo PSP, BPO and Master are **N/A** for all three — none runs
  the ICHRA/HSA/roster code these serve, so this is a scoping statement rather than a backlog. **The
  local `beta_ssa` cells remain ⬜ and disputed** — see §6.
