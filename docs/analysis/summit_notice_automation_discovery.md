# Summit Discovery — Import-Driven Notice Automation

**Created:** 2026-07-30
**Owner:** Kevin (manual — Summit UI, imports, scheduled exports)
**Design docs:** `../business/plus_tier.md`, `./summit_plus_tier_discovery.md`
**Status:** Not yet run.

---

## Why this test exists

The ICHRA/QSEHRA notice mechanism in production today is a workaround. Summit has no ICHRA or
QSEHRA notice event type, so a **notional COBRA-type benefit** is created alongside the real
ICHRA benefit; a status change against that notional benefit fires Summit's tracked-letter
vehicle, which carries the correct content and produces a mailing record.

The workaround is proven manually through the UI. **What is not proven is whether it can be
driven by import.** If it can, AMS generates the files from the new-hire report and the manual
double-entry collapses into a reviewed load. If it cannot, AMS's contribution is limited to a
worklist and the double-entry stays manual.

Everything downstream of that one answer changes shape, so it is tested first.

### Secondary outcome — this closes O5 for the "+" tier

Open item **O5** (`EventTypeID` value set, S-10 in the Summit discovery doc) has been carried as
**High** priority and treated as a question for DataPath. It is not — at least not for this
build. AMS does not need the full enumeration; it needs the IDs for **the events this workaround
actually generates**, and those are observable by doing the thing and reading the export.

Same method that worked for merge-token discovery: perform the action, observe what the system
emits. The complete enumeration still matters for notice types not yet built, but it stops
gating B4b.

---

## Established before this test (do not re-derive)

| Fact | Source |
|---|---|
| Custom COBRA-type benefits can be created | Kevin, 2026-07-30 |
| COBRA-type enrollments flag **eligibility** counts and do **not** cause double counts | Kevin, 2026-07-30 |
| DataPath bills COBRA participants only once status changes to **qualified beneficiary**. Status changes used here alter rates while leaving participants as **active employees** — **not billable.** Cost is notice mailing only | Kevin, 2026-07-30 |
| Participants in the ICHRA see nothing alarming from the notional item | Kevin, 2026-07-30 |
| Mailing export carries `EventTypeID`, `EventName`, `Mailed`, `SSN`, `DOB`, `ERCustomID` — no participant ID | `summit_plus_tier_discovery.md` S-2 |
| Mailing export is scheduled **by event type** | `summit_plus_tier_discovery.md` S-2 |
| Different letter contexts expose different merge-token sets | Merge-token discovery, prior session |

---

## Safety constraints

**Letters generate from imports the same way they do from manual entry. A malformed import
therefore mails real letters to real participants** — postage spent, and a compliance artifact
created that cannot be recalled.

1. **Do not run any phase against a live "+" employer with real participants** until Phases 1–4
   are complete and understood.
2. Use a **test employer**, or a participant whose mailing address you control and can verify.
3. Record the **exact file** used for every import attempt. A failed test is only useful if the
   input is reproducible.
4. If a phase produces unexpected mail, stop and record before continuing.

**Design rule this establishes for AMS:** any AMS-generated Summit import that can trigger
letters must have a **preview-and-confirm step** — render the rows, show which letter each will
trigger, require an explicit action. **Never a blind load.** Cheap to build; the failure it
prevents is expensive and irreversible.

---

## Phase 0 — Baseline and configuration capture

Record before touching anything.

| # | Capture | Notes |
|---|---|---|
| N-0a | Exact configuration of the notional COBRA-type benefit — name as displayed, benefit type, custom vs. standard, plan structure, rate structure | AMS may need to reference this by name in generated files |
| N-0b | Exact configuration of the real ICHRA benefit alongside it | |
| N-0c | Which exports are currently scheduled for this employer, and their event-type filters | The new-hire report is one of these |
| N-0d | Current mailing-export output for this employer — a "before" sample | Everything in later phases is a diff against this |
| N-0e | How the notional benefit is **named** in employer-facing and participant-facing views | See N-7 |

---

## Phase 1 — UI-driven letter (the control)

Run the existing manual process end to end on the test participant. **This is the control every
later phase is compared against** — without it, an import-generated letter cannot be judged
correct or incorrect.

| # | Observation | Record |
|---|---|---|
| N-1a | Coverage added to the notional benefit via UI — steps and required fields | |
| N-1b | Status change performed via UI — which status change, what values, what the form required | |
| N-1c | **Was a letter generated?** Which template | |
| N-1d | **Letter content** — capture the full rendered letter. Every merge field that resolved, and every one that rendered blank or literal | The comparison artifact for Phase 3 |
| N-1e | `EventTypeID` and `EventName` in the mailing export | **This is the O5 payload** |
| N-1f | `Mailed` value and its meaning — queued, printed, or posted | |
| N-1g | **Lag** between status change and appearance in the export | Sets `notice_obligation` reconciliation aging in B4a |

---

## Phase 2 — Import-driven coverage add

| # | Observation | Record |
|---|---|---|
| N-2a | **Does a coverage-add import exist for COBRA-type benefits?** Where in Summit | If no, stop — Phase 3 cannot proceed and the workaround stays manual |
| N-2b | File format — CSV/Excel, column names, required vs. optional | AMS's generator writes to this spec |
| N-2c | How the participant is identified — `Participant_ID`, SSN, custom ID, name+DOB | Determines what AMS must carry to generate the file |
| N-2d | How the **benefit** is identified — name, plan ID, code | |
| N-2e | Run it for a second test participant. Did coverage attach correctly? | |
| N-2f | **Did the add alone generate anything?** | Expected: no. Confirm rather than assume |
| N-2g | Error behaviour on a deliberately malformed row — rejected, skipped silently, or partially applied | Partial application is the dangerous one |

---

## Phase 3 — Import-driven status change ⭐ the primary question

| # | Observation | Record |
|---|---|---|
| N-3a | **Does a status-change import exist for COBRA-type benefits?** | If no, the automation path closes here |
| N-3b | File format, required and optional columns | |
| N-3c | Which fields drive **letter selection** — event type, coverage tier, effective date, rate | If the import accepts fewer fields than the UI, content may diverge |
| N-3d | **DID THE LETTER GENERATE?** | **The question this whole protocol exists to answer** |
| N-3e | **Content compared to N-1d, field by field** | Identical, thinner, or different template |
| N-3f | `EventTypeID` / `EventName` — **same as N-1e, or different?** | If import-driven events carry a different ID, `notice_event_map` needs both |
| N-3g | Lag, compared to N-1g | |
| N-3h | Did the participant remain an **active employee**, not flipped to qualified beneficiary? | **Confirms the no-billing property under the import path.** Do not assume it carries over from the UI path |

---

## Phase 4 — Sequencing, dependency, idempotency

| # | Observation | Record |
|---|---|---|
| N-4a | Can add and status change go in **one file**, or must they be two in sequence? | |
| N-4b | Status change imported for coverage that does not yet exist — hard error, silent skip, or created implicitly? | Silent skip is the dangerous one: no coverage, no letter, no error |
| N-4c | **Same import run twice** — duplicate coverage, duplicate letter, or no-op? | A duplicate letter is a mailed artifact. Determines whether AMS needs its own idempotency guard |
| N-4d | Partial-failure behaviour on a 10-row file with one bad row | All-or-nothing, or 9 applied and 1 rejected |
| N-4e | Is there any import **preview or dry-run** mode in Summit? | If so, AMS's preview step can defer to it |

---

## Phase 5 — Event-type mapping for the "+" tier

Enumerate only the events this tier actually needs. For each, perform the status change and read
the resulting `EventTypeID` from the mailing export.

| Notice | Trigger | `EventTypeID` | `EventName` | Letter template | Content verified |
|---|---|---|---|---|---|
| Mid-year new hire / SEP notice | Status change on hire | | | | |
| Annual renewal / rate change | Rate-change status change | | | | |
| Termination | | | | | |
| *(other, as discovered)* | | | | | |

**Output:** this table is the source data for `notice_event_map` and closes **O5** for "+"-tier
purposes.

---

## Phase 6 — Decliner path

Per **O30**, all eligibles are loaded, not only enrollees. Since COBRA-type enrollments flag the
eligibility count, **the notional benefit is the eligibility marker** — so decliners need one
too, and their handling is not obvious.

| # | Observation | Record |
|---|---|---|
| N-6a | Does a decliner get a notional record with **no** status change, or a different one? | |
| N-6b | Does any letter generate for a decliner? Should one? | An eligible non-electing employee may still be owed a notice |
| N-6c | Does the decliner appear in the **eligible** count without appearing in **enrolled**? | Confirms D5's two counts separate cleanly at the source |

---

## Phase 7 — Blast radius and naming

| # | Observation | Record |
|---|---|---|
| N-7a | Does the notional benefit generate **anything else on a schedule** — election-deadline reminders, premium coupons or invoices, expiration-of-rights notices? | The one to check hardest: silent, mailed, and goes to the employee |
| N-7b | Does it appear in COBRA-specific **reporting** in a way that misstates the employer's COBRA position? | |
| N-7c | Can the benefit be **named** something self-evidently administrative — e.g. "ICHRA Notice Administration" rather than anything COBRA-shaped? | Naming solves employer confusion once; training solves it every time. Matters at fifty groups, not at three |
| N-7d | Can benefit-level visibility be **suppressed** in the employer or participant view? | |
| N-7e | Mailing cost per notice | The only cost this mechanism carries. Input to D2's bundled PEPM |

---

## Decision branches

**If N-3d is YES — letters generate from imports.**
- AMS generates both files from the new-hire report; the manual double-entry becomes a reviewed
  load.
- **O26 partially resolves** — the coverage-add and status-change imports exist and work.
- AMS's generator requires the preview-and-confirm step (design rule above), plus its own
  idempotency guard if N-4c shows duplicates are possible.
- B4a's worklist becomes "review and load" rather than "go do this manually in Summit."

**If N-3d is NO — letters require UI entry.**
- The double-entry stays manual. AMS's contribution is the obligation register and worklist
  only: *these three new hires need the notice sequence*, then reconcile against the mailing
  export.
- Still worth building — it closes the tracking gap, which is the actual current pain — but the
  labour per participant does not fall.
- **This becomes a concrete product ask for DataPath**, and a strong one: *you have no
  ICHRA/QSEHRA notice event type, so administrators construct notional COBRA benefits to borrow
  your status-change letter vehicle, and it cannot be automated.* A specific gap, a proven
  workaround, from someone administering real cases.

**If N-3e shows content diverges between UI and import** — the import path may be usable for
coverage adds but not for notice-generating status changes. A hybrid: bulk-load coverage,
hand-enter status changes. Record which fields are missing; that list is the product ask.

---

## Notes

- **No SQL is produced by this discovery process.** Any finding implying an AMS schema change
  goes through a versioned migration under the normal rules.
- Findings feed: `notice_event_map` (Phase 5), B4a obligation aging (N-1g / N-3g), the AMS import
  generator spec (Phases 2–4), and D2's bundled PEPM (N-7e).
- Phases 1–3 are the minimum useful run. Phases 4–7 can follow, but **Phase 7 should not wait
  until fifty groups exist** — N-7a in particular is silent and mailed.
