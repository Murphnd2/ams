# Session close-out — 2026-09-15 — Card status audit check (#4)

Branch `refactor/modernize-architecture`. Framework: T237 phase 1
(`docs/analysis/audit_framework.md`; framework built at `3ebf5da`). This session built check #4
(`CardStatusCheck`) off the Summit **Debit Card Participants** export, then widened its supersession
rule twice in the same day once real data showed the initial rule wasn't enough. Every repo fact
below was re-read from the tree at close-out time — `git log`, `git status`, `ls docs/migrations`,
`docs/analysis/migration_tracker.md`, `AmsDataGlobal.java` line numbers, `AuditHub.java`,
`CardDeclineCheck.java`/`FundedPurseNoDisbursementCheck.java`, and `CardStatusCheck.java` itself —
not restated from the prompt without checking. Two things in the driving prompt did **not** hold up
against the tree; see "Contradictions found."

## Shipped

**Committed** as `9aef50d` on `refactor/modernize-architecture`, **pushed** to origin
(`67a2008..9aef50d`, confirmed: `git log --oneline origin/refactor/modernize-architecture -5` shows
`9aef50d` at the tip). Five files, `+2518/-1`:

- `CardStatusCheck.java` (new, 1,127 lines)
- `AuditCardStatus.java` (new, 347 lines) — detail servlet
- `auditCardStatus25.jsp` (new, 435 lines) — detail page
- `CardStatusCheckTest.java` (new, 608 lines, 35 tests)
- `AuditService.java` — one line, registering `CardStatusCheck` in `CHECKS`

`fb1a03c` (checks #2/#3, V114/V115) **is an ancestor of `9aef50d`** — confirmed with
`git merge-base --is-ancestor fb1a03c 9aef50d` — and was already on `origin` before this session's
work began (the branch was reported up to date with `origin` beforehand, per the driving prompt and
confirmed here by `origin/refactor/modernize-architecture`'s own log matching local exactly). Both
`fb1a03c` and this session's `9aef50d` are now on `origin`.

**`src/test/` is the repository's first committed test tree** — confirmed: `git log --oneline --all
--diff-filter=A -- 'src/test/**'` returns only `9aef50d`, and `git log --oneline -- src/test/` returns
only that same commit. No test file existed in any prior commit on any branch.

Working tree is **clean** (`git status`: "nothing to commit, working tree clean").

## In flight

Nothing. The commit is made and pushed; nothing from this build remains uncommitted.

## Verified runs — real data, 2026-09-15

Reported by Kevin from three runs against the same real export
(`ZZ_CARD_PARTICIPANTS_AUDIT_Export_20260915092635779.CSV`, 823 rows at 2 designated employers, 8,736
undesignated rows ignored at those two rows' scale) — the export itself is not in this repository, so
these counts are recorded as reported, not independently re-derived from the tree:

1. **29 findings**, problem-status only — expired/expiring was 0, matching the check's own
   documented expectation (`Requested` excluded from the expiry gate, and no card-bearing row was
   both expired and un-superseded in this file).
2. **13 findings, 16 superseded**, after the first (usable-replacement-only) supersession build — all
   16 checked row by row against their `Active` replacement.
3. **8 findings, 21 superseded (16 usable, 5 by a newer problem card)**, after widening the rule to
   card-bearing replacements. This matches the widened rule's own prediction exactly (13 → 8, 16 → 21).
   All 8 remaining findings are each holder's newest card, and all are `Hold` — consistent with the
   never-hide invariant: the newest card-bearing row for a holder is never itself superseded.
4. **Participant 3994 (FRANK SILVA), card 7525, `Hold`** — surfaced by this check, and also shown by
   `CardDeclineCheck` as 4× `Card blocked decline` (see `docs/session_closeout_2026-09-14_audit_checks_2_3.md`,
   which named the same participant on the decline side). This is the first cross-check agreement on
   real data between the two card checks the class Javadoc says are complementary.
5. Dependent rows (`UserTypeID = 7`) rendered correctly, with no name — matches the design (`Employee`
   holds participants only).

## Decisions made — and what each closed

- **Reuse of V114 (`audit_decline_employer`) and V115 (`audit_finding_ack`), no new schema.**
  Confirmed by reading `CardStatusCheck.java`: it calls `AuditDeclineEmployerDAO
  .findDesignatedEmployersByAltId` and `AuditFindingAckDAO.findAllByPspAndCheck`/`upsert` — the same
  two DAOs `CardDeclineCheck` uses, and no migration was authored this session (`ls docs/migrations`
  tops out at `V115`, unchanged).
- **Matching key.** `ParticipantCardStatusID` is the matching key; `Status` is display-only; `Name`
  and every `Is*` flag are never read (class Javadoc, `REQUIRED_HEADERS`/never-read comment block).
- **Expiry computed from `ExpirationDate`, never `IsExpired`** — same Javadoc section, same reasoning
  as `CardDeclineCheck`'s own export-defect notes.
- **Finding key: `{UserID}:{LastFour}:{ParticipantCardStatusID}`.** Because the status id is embedded
  in the key, a status change produces a *different* key — the old acknowledgment simply stops
  matching and the new key surfaces unacknowledged, regardless of whether the old one was `HANDLED`
  or `IGNORED`. This was accepted as correct: `Lost/Stolen` after `Hold` is a new condition, not a
  continuation of the old one.
- **Supersession rule, final (widened) form** — confirmed by reading `evaluate()` and
  `cardBearingStatusIds`/`usableStatusIds`:
  - An older flagged row is superseded by any later **card-bearing** row for the same `UserID`.
    Card-bearing = active ∪ problem ids (default `1,2,3,4,5,6,11`), excluding only `Requested` (7)
    and `Requested (Queued)` (13).
  - Card date is `IssuedDate`, falling back to `RequestedDate` only when `IssuedDate` is blank.
    Missing dates fail open on both sides (the flagged row's own missing date skips the check
    entirely; a candidate's missing date just isn't usable evidence).
  - **Never-hide invariant:** a holder's newest card-bearing row is never superseded, because nothing
    in its group is later than it.
  - Supersession is evaluated **before** the acknowledgment split — a superseded row is not a finding
    and is never looked up against `audit_finding_ack` at all.
  - A **usable** replacement is preferred for display over a merely card-bearing one, even when a
    chronologically newer problem-status candidate also qualifies.
- **Accepted display edge case, confirmed by re-reading the replacement-selection code
  (`usableReplacement`/`anyReplacement` tracked in one pass).** With `Lost/Stolen` (oldest), `Active`
  (middle), `Hold` (newest) for one holder: the old `Lost/Stolen` row is superseded and its shown
  replacement is `Active` — the *preferred* usable candidate — not `Hold`, even though `Hold` is
  chronologically the newer of the two qualifying candidates. `Hold` itself still surfaces on its own
  (it is the newest card-bearing row for that holder, so it is never superseded). Net effect: the
  Superseded section can show "by a usable card" for a holder whose *current* card is actually a
  problem — the usable/problem split can overstate recoveries in that specific chain shape. **Not
  present in the real data** (all 3 runs above), but real for the shape it describes. No fix applied;
  recorded as an accepted trade-off of "usable preferred."
- **Scheduler clarification, confirmed against the tree:**
  - "Run now" and detail-page loads read SFTP on demand — confirmed: `evaluate()`/`readLive()` both
    call `loadLatest()` → `SummitSftpService.list`/`read` fresh, no cache.
  - No automatic AMS run exists on this installation — `AUDIT_SCHEDULER_ENABLED` is a DB constant
    (`AuditHub.java:84`, `AppConstantDAO.getConstantValue`), and D-98
    (`docs/deployment_backlog.md`) records its status as **Not started**, unchanged by this session.
  - Summit's own export schedule (7am, per the working configuration Kevin set) is independent of
    AMS's scheduler — AMS only ever reads whatever Summit already delivered.

## Confirmed export facts — Debit Card Participants

Carried forward from the build prompt, now marked confirmed by real runs:

- The header, the `Name` defect, `IsExpired`/`IsCancelled`/`IsReplaced`/`IsMissingEmail` always `0`,
  the status id table, trailing whitespace on `Issued `, `EmployerID` = `Employer.altId`,
  `EmployerOrganizationID` not a fixed offset, `ParticipantID` blank on dependent rows — all as
  documented in `CardStatusCheck.java`'s class Javadoc, unchanged since the first build.
- **New this session:**
  - `LastFour` is the reliable card-exists marker (documented in the class Javadoc, from the
    fulfilment-profiling pass over this same export).
  - Summit keeps a replaced card at its problem status permanently — this is the whole premise of the
    supersession rule, and the 21-superseded-row real run is the confirming evidence for it, not just
    a hypothesis anymore.
  - The template `ZZ_CARD_PARTICIPANTS_AUDIT` delivers daily at 7am Central to FTP, and the real
    filename prefix in production `ssa.properties` is `ZZ_CARD_PARTICIPANTS_AUDIT`, not the build
    prompt's placeholder `ZZ_CARD_STATUS`. **Not independently verifiable from this repository** —
    `ssa.properties` is untracked and does not exist anywhere in this working tree (`find . -iname
    ssa.properties` returns nothing); this is recorded as reported by Kevin, consistent with the
    real export filename named in "Verified runs" above (`ZZ_CARD_PARTICIPANTS_AUDIT_Export_...`),
    which *is* consistent with that prefix under `CardStatusCheck.newestMatchingPattern`'s
    `{prefix}_Export_{17-digit timestamp}` shape.

## New assumptions — with reversal cost

Carried forward from `CardStatusCheck.java`'s own Javadoc, re-confirmed by reading it this session:

- **`ExpirationDate` format tolerance** — `DATE_FORMATS` accepts `M/d/yyyy`, ISO, `M-d-yyyy` on the
  leading token; only the first is confirmed against real data. Reversal: trim the list.
- **`parse`, `Config`, and `summaryOf` are package-private, not `private`** — for the synthetic test
  to drive CSV parsing, construct a config bypassing `AppConfig`, and assert on exact summary text.
  Reversal: none needed unless testability is no longer wanted; behavior is unaffected either way.
- **A `HANDLED` ack is allowed with a `null` `observed_through`** — a problem-status-only row may have
  no meaningful expiration date at all; the servlet's `ack()` accepts a blank/`"—"` `observedThrough`
  without failing the save. Reversal: re-add the hard requirement `CardDeclineCheck` has for both
  fields, at the cost of blocking a legitimate ack on such a row.
- **`cardDateOf` falls back to `RequestedDate` only when `IssuedDate` is blank, not when it's merely
  unparsable** — a deliberately asymmetric fail-open. Reversal: trivial, one condition change.
- **The usable-preferred tie-break is global** — a usable candidate is shown even when a
  chronologically newer problem-status candidate also qualifies. Reversal: small — would need a
  single "newest wins outright, usable only as a tiebreak among ties" comparison instead of the
  current two-tier tracking.
- **`observed_count`/`observed_through` are reused to carry a status id and a date**, not a count and
  a cutoff — documented plainly in the Javadoc so the column names don't mislead a future reader.
- **`EmployerOrganizationID` is treated as never-read** — it appears in neither the build prompt's
  "optional read" nor "never read" list explicitly, but the total column count only reconciles if
  it's never-read; resolved that way and documented in the class Javadoc. See "Contradictions found."

## Open follow-ups

Carried forward from `docs/session_closeout_2026-09-14_audit_checks_2_3.md`, each re-verified against
the tree this session:

1. ⚠️ **Still present, unfixed.** `AmsDataGlobal.java:424-425` and `:430-431` hard-code the Summit
   host (`https://superiorstate.summitwith.us`) and TPA GUID as catch-block fallbacks for
   `SUMMIT_PATH`/`SUMMIT_TPA_GUID`. Re-read this session at those exact lines — unchanged. Still the
   highest-value follow-up: a wrong-answer-that-looks-right defect that only bites a second
   installation.
2. **Still present, unfixed.** `AuditHub.java:84` re-reads `AUDIT_SCHEDULER_ENABLED` per request; the
   badge reflects the setting, not whether a scheduler thread actually exists. Confirmed by reading
   the servlet this session — unchanged from the 09-14 close-out's description.
3. **Still present, unfixed.** `IchraUncodedParticipantsCheck.java:147` and
   `FundedPurseNoDisbursementCheck.java:343` both still use `name.startsWith(prefix)`, unlike checks
   #3 and #4's exact `{prefix}_Export_` match. Confirmed by grep this session.
4. **Still present, unfixed.** `CardDeclineCheck.java`'s `dateParsedRows` is still computed over every
   row in the file, not just decline rows (confirmed at lines 685/698/705 this session) — the same
   exposure `CardStatusCheck` deliberately avoided by scoping its own equivalent count the same way
   (over every row, by design, since `ExpirationDate` matters regardless of designation — a different
   situation from `CardDeclineCheck`'s decline-only relevance).
5. **Still present, and worse than described.** `docs/analysis/audit_framework.md`, re-read in full
   this session: still describes a **three**-method `AuditCheck` contract (`key`, `label`,
   `evaluate(psp)` — `detailPath()` is still missing), still says check #1 "Selects rows for employers
   on the ICHRA check's configured employer list" (Summit-side in reality, TA-56), and **does not
   mention checks #2, #3, or #4 anywhere** — not even in "Candidate later checks." This is the doc a
   future session is most likely to read first, and it is now stale on four of the framework's five
   facts (three original plus check #4's total absence).
6. **Still present, unfixed, now replicated a third time.** `audit_finding_ack` trusts posted
   `observed_count`/`observed_through` rather than re-reading current values at save time —
   `AuditCardStatus.ack()` (this session's build) follows the exact same pattern `AuditCardDeclines`
   established. The exposure (a stale page or racing tab acknowledging against numbers that have
   since moved) now exists in two checks' `doPost` handlers, not one.
7. **Unchanged, cosmetic, not re-verified against a live database this session** (no DB access from
   here). V114's `schema_version` description truncation (MySQL warning 1265) — carried forward as
   previously recorded; V115's own description was already length-checked, and this session's build
   authored no migration at all.

**New this session:**

- **`audit_decline_employer` is now shared by two checks**, confirmed by grep:
  `CardDeclineCheck.java` and `CardStatusCheck.java` both call
  `AuditDeclineEmployerDAO.findDesignatedEmployersByAltId`. The table name is narrower than its use,
  and there is no way to designate an employer for one card check but not the other — a `check_key`
  column would be needed for that, and remains a follow-up, not built.
- **The build prompt's own `ssa.properties` block placeholder was `ZZ_CARD_STATUS`; the real value is
  `ZZ_CARD_PARTICIPANTS_AUDIT`**, per Kevin, set in untracked `ssa.properties` — see "Confirmed export
  facts" above for why this can't be independently re-verified from this repository.
- **IntelliJ auto-stages new files** — observed directly across this session's own commits: `git
  status --porcelain` showed `AM` (added, then further modified) for files this session never staged
  itself. Harmless, but it means "nothing staged" can't be inferred from a bare `git status` glance
  without checking *what's* staged.
- **Line endings: `git add` warned LF→CRLF on all five files** at commit time (from Kevin's own
  pasted terminal output) — recorded only, not a defect; Windows/git `core.autocrlf` behavior.
- **Docs pending, confirmed still missing by reading each file this session:**
  - A config-registry row per key in `docs/business/summit_data_exchange.md` — its "Config registry
    for the audit check" table (line 1057) still lists only check #1's four keys.
  - A `D-NN` for check #4 alongside D-98 — no such row exists; the highest is `D-105`
    (`docs/deployment_backlog.md`), unrelated to card status.
  - A T-number in `docs/analysis/project_backlog.md` (standing rule S16-G) — the highest T-number in
    the file is `T277`; nothing references card status or check #4.
  - A card status section in `docs/analysis/audit_framework.md` — confirmed absent (see follow-up 5).

## Contradictions found

- **The original build prompt's verification case "Hold row with past expiration → appears in both
  sections" still holds, but only when no later card-bearing row exists for that holder** —
  supersession now governs; an isolated `Hold` row (no later candidate in its group) still appears in
  both `problemRows` and `expiryRows` exactly as originally verified.
- **The first widening build's own test, `laterHoldRowDoesNotSupersedeAndSurfacesItselfToo`, was
  reversed by design in the second widening build** — rewritten this session (as
  `laterProblemCardSupersedesAndItselfSurfaces`) once the rule widened from usable-only to
  card-bearing replacements. Confirmed by reading `CardStatusCheckTest.java`.
- **The build prompt's own properties-block placeholder** (`ZZ_CARD_STATUS` vs. the real
  `ZZ_CARD_PARTICIPANTS_AUDIT`) — see "Confirmed export facts."
- **`EmployerOrganizationID` was named in neither the build prompt's "optional read" nor "never read"
  header list** — resolved as never-read by column-count reconciliation, documented in the class
  Javadoc as an assumption.
- ⚠️ **New this close-out: the driving prompt's claim "Pending deployment: V105–V115 on Production"
  does not match `docs/analysis/migration_tracker.md`.** The per-environment table
  (`docs/analysis/migration_tracker.md` lines 247-258) shows the Production column as `⬜` (pending)
  for **V103 and V104 as well** — both rows' own prose states explicitly "Not applied to Production."
  V102 is the last row with Production `✅`. The accurate pending range is **V103–V115**, not
  V105–V115 — the same undercount the 2026-09-14 close-out itself carried forward without
  re-checking against the environment table.
- ⚠️ **New this close-out: `ccprompt_phase2_card_fulfilment.md`, cited in the driving prompt's "Next"
  item 1 as "already specified," does not exist anywhere in this repository.** `find . -iname
  "*card_fulfilment*"` (and `*fulfillment*`) returns nothing. Recorded here so "Next" below does not
  repeat the claim as fact.

## Next

Recommended order, one line of why each:

1. **Fix the `AmsDataGlobal` hard-coded Summit fallback defect** (follow-up 1). It gives a wrong
   answer that looks right on a second installation, and is the highest-value item carried across two
   close-outs now without being picked up.
2. **Locate or (re-)author the card fulfilment monitor spec** before building it — the file the prior
   prompt named as "already specified" isn't in the tree (see "Contradictions found"). The profiling
   facts it would need are already recorded in `CardStatusCheck.java`'s "out of scope" Javadoc note
   and in this session's own build history; a monitor build should read those before writing a new
   spec from scratch.
3. **Deploy V103–V115 to Production and set `AUDIT_SCHEDULER_ENABLED`, then restart.** Nothing in
   this framework has ever run unattended on any installation — every verified run across both card
   checks has been a manual "Run now" or an acknowledgment-driven re-run.

## Deployment prerequisites

Paste-ready `ssa.properties` block for check #4, with the real prefix and the id→label mapping in
comments (the id→label table itself is also in `CardStatusCheck.java`'s class Javadoc). Note: the
Summit template must include every required header (`EmployerID`, `UserID`,
`ParticipantCardStatusID`, `Status`, `ExpirationDate`) or the check fails closed with a named-missing-
header `ERROR`, the same contract `CardDeclineCheck` depends on for its own required column
(`Decline Reason`).

```properties
# --- T237 check #4: card status problems (CardStatusCheck) ---
# Employer scope is data, shared with check #3 -- see audit_decline_employer / AuditDeclineEmployerAdmin.
# Summit template name (filename prefix) of the Debit Card Participants export. Required. Exact match
# on "{this}_Export_..." -- a test template sharing a stem will not silently win.
SUMMIT_AUDIT_CARD_STATUS_EXPORT_PREFIX=ZZ_CARD_PARTICIPANTS_AUDIT
# Comma-separated ParticipantCardStatusID values considered a problem status. Optional, default below.
# 1=Issued, 2=Mailed, 3=Active, 4=Hold, 5=Lost/Stolen, 6=Permanently Inactive, 7=Requested,
# 11=Reissued, 13=Requested (Queued) -- Summit's vocabulary, may gain values.
#SUMMIT_AUDIT_CARD_PROBLEM_STATUS_IDS=5,4,6
# Comma-separated ParticipantCardStatusID values where a card actually exists, gating the expiry
# test. Optional, default below. Also feeds the supersession rule's "usable" tier (active minus
# problem) and, unioned with the problem list above, its "card-bearing" tier.
#SUMMIT_AUDIT_CARD_ACTIVE_STATUS_IDS=3,2,1,4,11
# Expiring-soon horizon in days. Optional, default 60.
#SUMMIT_AUDIT_CARD_EXPIRY_WARN_DAYS=60
# Newest matching export older than this is ERROR. Optional, default 36.
#SUMMIT_AUDIT_CARD_MAX_AGE_HOURS=36
# Byte cap is the existing SUMMIT_AUDIT_EXPORT_MAX_BYTES (default 16777216) -- reused, no new key.
```

## SQL close-out audit

- **No SQL was produced, run, or recommended this session.** No schema change was needed — both
  tables this check uses (`audit_decline_employer`, `audit_finding_ack`) already existed.
- **Highest migration version: V115** — confirmed by `ls docs/migrations`, unchanged by this session.
- **No orphaned `.sql` files** — the numbered sequence `V025`–`V115` is contiguous (checked
  programmatically this close-out; no gaps), plus the pre-existing, non-versioned
  `seed_ndt125_questionnaire.sql`.
- **Pending deployment: V103–V115** on Production, per `docs/analysis/migration_tracker.md`'s
  per-environment table — **corrected from this prompt's own "V105–V115" claim**; see "Contradictions
  found" for the discrepancy and how it was checked.
- **No schema described but not scripted** — confirmed; this session's entire supersession-rule
  widening was pure Java/JSP logic over already-existing tables and columns.
