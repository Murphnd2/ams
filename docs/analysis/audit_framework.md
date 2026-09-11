# Audit Framework

**Design — not built (S46, 2026-09-10)**

**Phase 1 built — `3ebf5da`; runtime-verified 2026-09-11.**

**Decisions made at build:**
- Config sources: the scheduler switch is a DB constant (`AUDIT_SCHEDULER_ENABLED`), following the
  `RATE_CACHE_WARM_ENABLED` precedent; the check settings are in `ssa.properties`.
- The scheduled run uses the installation PSP from `AmsDataGlobal`; requests use the session PSP,
  and a mismatch is a 403.
- The badge reads `applicationScope.auditService` directly, with no servlet or include.
- The export defines the population, so there is no per-PSP employer list.

**Verified at runtime (2026-09-11):**
- `NOT_CONFIGURED` status;
- reload from the DB after a restart;
- an `ACTION` finding (1) with the bell showing;
- the live detail page;
- clearing after coding in Summit;
- newest-file selection;
- all links.

**Not yet exercised:** the scheduled daily run, and the `ERROR` path (no matching export; stale
export).

## Purpose

Recurring, condition-driven findings across all groups, distinct from one-time checklist tasks. A
checklist task fires once, at setup or renewal, for one activity. An audit check runs continuously
against live source data and surfaces a finding for as long as the condition holds, across every
group a PSP administers.

## Check contract

Each check has a key, a label, and `evaluate(psp)`. Evaluation returns:

- a **count** (how many items the check found);
- a **status** — `OK`, `ACTION`, or `ERROR`;
- a **one-line summary**;
- a **detail URL**.

Checks are registered in code and enabled per PSP by config. Nothing installation-specific lives in
code — which employers, which check thresholds, which PSPs see which checks are all configuration,
not literal IDs or names in Java.

## Alarm

A navbar badge, PSP admin only. It shows the total `ACTION` count from the latest **stored** run, and
**never evaluates on page render** — the badge reads a prior run's result, it does not trigger a new
one.

## Hub

One page listing each check: last run, count, status, *Run now*, and detail.

## Storage

A counts-only run table: PSP, check key, run time, count, status, error, and summary. **No personal
data.** Detail pages read their source live and render in-request — the same pattern as the T230
response check (`SummitResponseService`/`SummitResponseServlet`) — rather than persisting the rows a
check found.

## Semantics

Findings are **self-clearing**: no acknowledge or dismiss state in v1. A finding exists only while its
condition holds in the source data. Once the condition clears, the next run's count drops and the
finding is gone — there is nothing to mark resolved.

## Schedule

Daily, plus *Run now*.

## First check — ICHRA participants without an AMS or adopted custom ID

- Reads the newest participant-list export in `ExportFiles`.
- Selects rows for employers on the ICHRA check's configured employer list.
- Keeps rows with `UserStatus` Active and a blank `ParticipantCustomID`.
- The detail page shows name, `Participant_ID`, and the value to set,
  `{prefix}-S-{Participant_ID}`, with the send-then-code sequence stated on the page (send the notice
  On Demand *Just These* first, then code the participant in Summit — coding without sending is the
  unsafe order).

See `docs/business/summit_data_exchange.md`, "Custom-event notices and scheduled exports — tested
2026-09-10", and `docs/analysis/plus_tier_build_plan.md` D43 for the participant-export mechanism this
check reads, and `docs/analysis/legal_assumptions.md` LA-40 for the SSN/PHI handling constraint on
that export.

## Candidate later checks

- Elected plans dropped from file 2 (carryover or unmapped).
- Pushed files with no response after N hours.
- Steps pushed but never marked done.
- Addresses over 50 characters.
- COBRA-flagged groups carrying allowance text in *Employer Plan Name* (the `EmployerPlanName`
  collision recorded in `summit_data_exchange.md`).

## Phase A questions

- Does AMS already have a scheduler or notification mechanism to extend?
- How does `navbar25.jsp` gate PSP-admin content?
- What is the participant-export filename pattern in `ExportFiles`?
- How is the employer list for a check configured, per PSP?
