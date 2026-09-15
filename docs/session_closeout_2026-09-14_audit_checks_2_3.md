# Session close-out — 2026-09-14/15 — Audit checks #2 and #3: funded purses, card declines

Branch `refactor/modernize-architecture`. Work spans **2026-09-14 into 2026-09-15** (past
midnight) — the filename keeps the session's start date; this header states the true range. This
is the authoritative, stand-alone close-out for the whole body of work — it **supersedes**
`docs/session_closeout_2026-09-14_audit_funded_purse_check.md`, which is stale on several points
this file corrects and now carries only a pointer to here. Every repo fact below was re-read from
the tree at close-out time — `git status`, `git log`, `ls docs/migrations`, `AmsDataGlobal.java`
line numbers, and the actual source of the touched files — not restated from any prompt without
checking. **Kept as one file, not split**: the title ("checks #2 and #3") already covers
everything added this pass — employer designation and acknowledgment are both check #3 features,
not a new surface — so a split would fragment one continuous narrative without buying clarity.

Framework: T237 phase 1 (`docs/analysis/audit_framework.md`; framework built at `3ebf5da`, first
check `IchraUncodedParticipantsCheck`). This session built check #2
(`FundedPurseNoDisbursementCheck`) and check #3 (`CardDeclineCheck`), then extended check #3 with
per-employer opt-in designation (V114), participant/employer name resolution, a Summit deep link,
Handled/Ignored finding acknowledgment (V115), and a fix so acknowledging re-runs the check so the
hub and badge stay current. Every piece has been run against real Summit exports or real UI
interaction — none of this is speculative, and none of it is runtime-verified against a live
Tomcat/MySQL beyond the local installation it was built and clicked through on.

## Shipped

**Committed** as `fb1a03c` on `refactor/modernize-architecture`, 22 files, message "T237:
funded-purse and card-decline audit checks, employer designation, finding acknowledgment
(V114/V115)". The branch is **1 commit ahead of `origin/refactor/modernize-architecture`, not yet
pushed** — confirmed by `git status` (`Your branch is ahead of 'origin/...' by 1 commit`) and a
clean working tree.

⚠️ **This corrects the close-out's own prior claim, and a stale instruction in the prompt that
drove part of this pass.** Both read "nothing was committed this session" / "the tree is dirty
throughout" and (separately, in the prompt driving this specific pass) described "a git index
anomaly: six code files and two docs are staged with no `git add` from any run this session" and
instructed a next step of "review `git diff --cached`, then commit." **All of that was true when
written and is not true now** — a later run in this same session staged all nineteen changed paths
explicitly, by name (verified against `git status` before staging, never `git add -A`), and
committed. See "In flight" immediately below for the one thing still genuinely outstanding.

## In flight

**Nothing.** The working tree is clean and every file from this session is committed. The only
outstanding action is `git push` to `origin/refactor/modernize-architecture` — not done in any run
this session (pushing is a git-mutating action outside every run's own scope fence) and is
Kevin's to do. **Nothing here is runtime-verified against a live Tomcat/MySQL deployment** — every
verified run below was against this local installation's own Summit tenant and database, not a
production deploy, and neither V114 nor V115 has been applied anywhere but locally.

## Verified runs — real Summit data

Both checks have read real exports repeatedly across this session, and check #3 has been driven
through its full UI (designation, acknowledgment, un-acknowledgment) against live data.

### Check #2 — `FundedPurseNoDisbursementCheck`, first runs (2026-09-14)

- **August 2026 evaluation** (`MONTH_OFFSET=1`): `ACTION` — **2 purses at 1 employer, 29 rows in
  month, 1 unmapped `TransactionType`** — from
  `ZZ_PARTICIPANT_HISTORY_AUDIT_DEL_Export_20260914165009323.CSV`. Both findings were health-FSA
  purses funded with nothing spent — correct true positives against the predicate as specified.
- **January 2026 evaluation** (`MONTH_OFFSET=8`): **197 purses at 38 employers, 1,091 rows in
  month, 9 unmapped types.**
- **Scope lesson, stated plainly so it isn't relearned:** on health FSA, "funded and nothing spent
  in a given month" is the *normal* state — most FSA elections are annual and many participants
  simply don't submit a claim every month. The predicate is near-population on FSA data and the
  check is **unusably noisy** there. It is only a meaningful signal where sitting on a funded
  balance is abnormal: **ICHRA and Ins125+.** `SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS` should carry only
  `1002` (ICHRA) and `1016` (Ins125+); the FSA ids `1`, `2`, `5` used above were test-only, to prove
  the pipeline against real data, not a production configuration.
- **With the FSA ids removed:** `ERROR` — *"Nothing evaluated: 0 of 13295 rows matched the
  configured plan-type scope."* Correct behaviour, not a regression — see the 2026-09-15 update
  below for what changed once real ICHRA/Ins125+ data appeared.

### Check #3 — `CardDeclineCheck`, first run, pre-designation (2026-09-14)

- **14-day default window, Summit-side scoping only** (V114 not yet built): `ACTION` — **22
  participants declined in the last 14 days (2026-09-01 – 2026-09-14), 41 decline rows at 15
  employers, 7 distinct `Decline Reason` values** — from
  `ZZ_TRANSACTION_HISTORY_AUDIT_Export_20260914184135647.CSV`.
- The rolling window and the participant-level finding unit both worked as designed: 958 total
  decline rows in the file (the full profiling sample, not filtered to the window), 41 fell inside
  the 14-day window, and those 41 rows collapsed to 22 distinct participants — a badge someone can
  actually read, unlike check #2's 197 on FSA data above.

### Check #3 — designated and acknowledged (2026-09-15)

With `audit_decline_employer` populated and the UI driven end to end:

- Window **2026-09-02 – 2026-09-15**: **8 decline rows at 1 of 2 designated employers, 2
  participants**, **722 decline rows at undesignated employers filtered out** — the
  designated-employer filter working exactly as built, on real data, at real scale.
- **Participant 28674 (FRANK RUSSELL)** — 4× `Exceeded Funds Available` at $329.00, MCC 8099,
  most recent 2026-09-15. **Same participant, same amount as the full-year profiling file's 61
  declines at $329** — a live retry cluster, not a coincidence: the exact recurring-charge-failure
  shape the check's decline-count badge and per-participant reason breakdown were built to surface.
- **Participant 3994 (FRANK SILVA)** — 4× `Card blocked decline` at $98.11, MCC 5912. A blocked
  card at a pharmacy — the concrete, named case behind the open DataPath question in "Next" item 5
  (does Card Deactivation block the whole card, including a premium draft on the same card).
- Both participants' names resolved correctly on the detail page — **verified working, not just
  built**: FRANK RUSSELL and FRANK SILVA both came back from `Employee`, confirming the name-join
  logic end to end (see "Name resolution" below).

### Check #2 — `Election`, the real ICHRA/Ins125+ signal (2026-09-15)

- With `1002,1016` (ICHRA, Ins125+) live in `SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS` and real
  PremiumPath/Ins125+ data now in the export: `ERROR` — **1 row matched scope with a parsable
  `SystemDate`, unmapped type `Election`.**
- **This is the naming feature paying for itself on first use** — see "Unmapped `TransactionType`
  values — named" below for why `Election` must **not** be added to the contribution list, and why
  this `ERROR` is correct and will persist until a real contribution transaction exists.

### Check #1 — `IchraUncodedParticipantsCheck` (2026-09-15)

- `OK`, 0 participants. Unaffected by anything in this session; recorded only so the hub's
  three-row state is documented in full alongside checks #2 and #3.

## Decisions made — and what each closed

- **Audit checks #2 and #3, not new surfaces.** The `AuditCheck` contract
  (`key`/`label`/`detailPath`/`evaluate`) fit both exactly; registration is one `List.of(...)`
  element each in `AuditService`. Closed: where each lives and how it is reached.
- **Employer scope is Summit-side for checks #1 and #2 — resolved, not open.** This closes the
  "two employer-scoping conventions" question an earlier pass of this close-out left open.
  `IchraUncodedParticipantsCheck` established the convention first (TA-56, the export template's
  own Employer selector); check #2 briefly carried an AMS-side `SUMMIT_AUDIT_FUNDED_EMPLOYER_IDS`
  key, which was removed because it duplicated the template's selector. **Check #3 diverges from
  this convention deliberately** — see "Employer designation (V114)" below for why.
- **Check #2's plan-type filter is retained as a guard**, not removed alongside the employer key —
  it is the one dimension Summit's export template cannot scope (there is no per-plan-type
  selector), and the FSA-vs-ICHRA scope lesson above shows exactly why it still matters.
- **`PTName` is not read at all, in either export.** It is the participant's name
  ("lastname, firstname"), not a plan attribute — the original spec for check #2 was wrong on this.
  Neither export's field set can be filtered in Summit, so the files landing in `ExportFiles`
  always carry participant identity somewhere; each check simply never reads that column. LA-40
  handling sits with the export/landing directory, not with either check.
- **Check #2's month basis is `SystemDate`, unconditionally — not configurable.** `EventDate` was
  briefly a configurable alternative and was removed after the real export showed it blank on
  every disbursement row (87 `Debit Card`, 22 claim rows) — using it as the basis would have turned
  every participant into a false finding.
- **Check #2's total failure is `ERROR`, never `OK` — four conditions, no tolerance knob.** Zero
  rows survive plan-type scope; scoped rows exist but none parses `SystemDate`; date-parsed rows
  exist but none matches either `TransactionType` list; typed rows exist but none falls in the
  evaluation month. Both `ERROR` states recorded this session (the FSA-removed run on 09-14, the
  `Election`-only run on 09-15) are condition 1 and condition 3 respectively, working as designed.
- **Check #3's status semantics deliberately diverge from check #2's.** Zero declines in the window
  is `OK`, not `ERROR` — the window is always "now," so an empty window after a clean parse is the
  good outcome, not a misconfiguration signal. `ERROR` is reserved for the export being unusable:
  not found, stale, zero data rows, or every row's `Transaction Date` unparsable. See "New
  assumptions" for the one place this reasoning is incompletely applied.
- **Check #3's finding unit is the participant, not the row.** 958 decline rows would produce a
  badge nobody reads; `findingCount` is the distinct `Participant System ID` count, with the row
  count, distinct-employer count, and distinct-reason count carried in the summary line instead.
- **Check #3 has no plan-type filter and cannot have one.** `Plan Type Code` is blank on all 958
  profiled declines — the file itself cannot distinguish a premium decline from an FSA one. This is
  the reason employer designation exists at all for check #3 — see below.
- **Check #3's filename matching is exact** (`{prefix}_Export_` literal, then the 17-digit
  timestamp), unlike checks #1/#2's `startsWith(prefix)`. This was specified from the start for
  check #3, precisely because the `startsWith` looseness had already been observed to let
  `ZZ_PARTICIPANT_HISTORY_AUDIT` match a `..._DEL` template on check #2. See "Open follow-ups" item
  3 for carrying the fix back to the other two checks.
- **Check #3's MNQ (Merchant-Not-Qualified) reason match is one configured string, trimmed and
  case-insensitive** — not a list, because only one reason drives the MCC diagnostic table. Every
  other `Decline Reason` value is free text the check never branches on.
- **The Summit participant link on check #3's detail page mirrors `SummitEmployerLinkResolver`'s
  shape exactly** — see "Summit participant deep link" below.
- **Expected-amount matching and partial-payment detection remain out of scope for both checks.**
  Both would need a premium-expectation source AMS does not have, for a weaker signal than "nothing
  paid" / "declined."

## Confirmed export facts

### Identity facts — settled from real data, do not re-derive

- **`Employer SystemID` = `Employer_ID` (Plan History) = Summit `EmployerID` = `Employer.altId`,
  column `employer.employer_id`.** Evidence: the Plan History sample carries `Employer_ID` 1100 and
  `Organization_ID` 1102 as different values; the Transaction export's `Employer SystemID` set
  contains 1100 and not 1102. **Confirmed a second way, in the UI, 2026-09-15**: Taxing Authority
  Consulting Services P.C. shows Summit EmployerID **1100**, AMS id **1102** — the same pair, from
  a live designation, not a file sample.
- **`Participant System ID` = `Participant_ID` (Plan History) = `Employee.id`, column
  `employee.employee_id`.** Evidence: nine participants overlap between the two exports on those
  columns, while `Participant Custom ID` has zero overlap with `Participant_ID` — separate
  namespaces. **`Participant Custom ID` is not the join key** and is never used for name
  resolution or the employer designation join.

### Participant Account History export (check #2's source)

- **Real header**, `PTName` present in the unfiltered export (never read):
  `Participant_ID,ParticipantPlan_ID,Employer_ID,Organization_ID,EmployerOrganizationID,DivisionName,EmployerPlan_ID,PlanName,PlanTypeID,StartDate,EndDate,PlanYear,ID,TransactionType,EventDate,SystemDate,ClaimKeyCheckNumber,Transactionamt,User_ID,EmployerPlanDetailForPlanYear_ID,EmploymentStatusID,UserStatusID,ByDivision`.
- **Seven required headers**: `Participant_ID`, `ParticipantPlan_ID`, `PlanName`,
  `PlanTypeID`, `TransactionType`, `SystemDate`, `Transactionamt`. `Employer_ID` is optional —
  read only for the summary line's distinct-employer count, never validated, never filtered on.
- **Filename shape**: `{template}_Export_{17-digit timestamp}_CSV.csv` — a token sits between the
  timestamp and the extension. Relaxed pattern:
  `^.+_(\d{17})(?:_[A-Za-z0-9]+)*\.[A-Za-z0-9]+$`. ⚠️ This is `startsWith`-rooted, not exact — see
  "Open follow-ups" item 3.
- **Date format**: `M/d/yyyy h:mm:ss a` (e.g. `4/1/2026 12:00:00 AM`). Zero unparsable `SystemDate`
  values, zero unparsable amounts, in the 365-row profiling sample.
- **All amounts positive**, every type — no signs, parentheses, or currency symbols in the
  sample. `abs()` is harmless; direction comes only from the configured type lists.
- **The double-count hazard is confirmed, not inferred** (see "New assumptions").
- **`EventDate` is blank on every disbursement row** (87 `Debit Card`, 22 claim rows) — `SystemDate`
  is the only viable month basis; this is not a preference, it is the only column that has data on
  the rows that matter.
- **Working export configuration**: window **Previous Month Start → Previous Month End**,
  destination FTP, **daily**. `SUMMIT_AUDIT_FUNDED_MONTH_OFFSET` must stay at **1** while that
  window shape is in use — a wider or narrower window shape requires re-deriving the offset.

### Transaction export (check #3's source)

Header, verbatim:
```
Employer Custom ID,Employer SystemID,MCC,Participant Custom ID,Participant System ID,Total Transaction Amount,Transaction Date,Claim Denied Reason,Plan Type Code,Card Transaction Status,Amount Paid,Merchant Name,Denied Amount,Decline Reason
```

- ⚠️ **`Decline Reason` must be in the field set, or the export emits no decline rows at all.**
  Without it: 7,638 rows, zero declines. With it: 7,640 rows, **958 declines**. The column does not
  merely add a data field — it changes which rows Summit emits. **This cost two export runs to
  discover** and is the single most important operational fact about this export: a template
  someone "cleans up" by dropping an apparently-unused column silently disables the whole check.
- **`Card Transaction Status` never carries a decline value.** Its observed vocabulary is ten
  post-authorization states: `Auto substantiated`, `Approved`, `Plan Satisfied`,
  `Receipt Requested`, `Returned`, `Authorize`, `Ineligible`, `Expense Denied`, `Receipt Overdue`,
  blank. Do not look for declines there.
- **"Include records with Zero Paid Amount" is not what gates declines.** 1,684 rows had a blank
  `Amount Paid` in the export run that contained *no* declines — that setting and decline
  visibility are independent.
- **Field completeness on a decline row, over 958 declines** — populated on all 958:
  `Employer SystemID`, `MCC`, `Participant System ID`, `Total Transaction Amount`,
  `Transaction Date`, `Decline Reason`. Partially populated: `Employer Custom ID` (930/958),
  `Participant Custom ID` (648/958). **Blank on all 958**: `Plan Type Code`,
  `Card Transaction Status`, `Amount Paid`, `Merchant Name`, `Denied Amount`,
  `Claim Denied Reason` — `CardDeclineCheck` deliberately never reads these six.
- **Two design constraints this creates**: no plan-type scoping is possible on a decline row (see
  "Employer designation (V114)"); MCC is the only merchant signal, since `Merchant Name` is blank.
- **Date format**: `M/d/yyyy h:mm:ss a`, same shape as the Plan History export — the same
  leading-token parse handles it.
- **Working export configuration**: window **Current Calendar Year Start → Current Date** (must
  exceed the check's 14-day rolling window — a narrower export window than the check's window would
  silently truncate what the check can see), destination FTP, **daily or twice daily**, scoped
  Summit-side to the employers being watched.

**Observed `Decline Reason` vocabulary** (free text; no logic is built on any value beyond the
configured MNQ default):

| Reason | Count |
|---|---|
| Card blocked decline | 323 |
| Exceeded Funds Available | 228 |
| Merchant Not Qualified | 199 |
| Invalid transaction | 58 |
| Decline advice | 30 |
| Invalid CVC2 | 26 |
| Card not activated | 24 |
| Card expired | 19 |
| No cards issued | 17 |
| Merchant indicated no eligible items | 9 |
| Withdrawal limit exceeded | 8 |
| Invalid Account Type | 8 |
| Logo record not found | 4 |
| Visanet decline received | 3 |
| IIAS Data Not Found | 1 |
| Invalid Pin | 1 |

**`Merchant Not Qualified` by MCC** — the card-configuration diagnostic the MCC summary table
exists for: 5912 pharmacy **75**, 5734 software 34, 5300 wholesale 16, 5411 grocery 11, 5816
digital goods 8, then a long tail of single digits across 5818, 5699, 5499, 5331, 5946, 7297, 5691,
5817, 5999, 5967, 5968, 5399, 7997, 5712, 7994, 5099, 8999, 7523, 8220, 5941, 5072.

⚠️ **The 5912 (pharmacy) figure is not an MCC gap** — pharmacy is already enabled. The more likely
explanation is non-IIAS pharmacies failing the eligible-items test, supported by the single
`IIAS Data Not Found` row and the nine `Merchant indicated no eligible items` rows.
**Hypothesis, not confirmed** — nobody has traced a specific 5912 decline to a specific merchant's
IIAS status. FRANK SILVA's 4× `Card blocked decline` at MCC 5912 (a pharmacy) is now a concrete,
named instance to trace if this is ever investigated further.

**Repeat-decline clusters** — the shape a failed recurring premium takes: participant 28674
declined **61 times at $329** in the full-year profile (`Exceeded Funds Available`) and, in the
designated/acknowledged run above, **the same participant, same amount, still declining** on
2026-09-15; 27801 eleven times at $175 (`Card blocked decline`); 24440 across three different
amounts, all `Merchant Not Qualified`. This is exactly the pattern `CardDeclineCheck`'s prominent
decline-count badge and per-participant reason breakdown were built to surface.

**Scale, whole-file counts from the 958 profiled declines**: 59 participants card-blocked across
28 employers; 63 with insufficient funds; 57 hitting unqualified merchants.

### Card transaction export — ruled out as a decline source

`ZZ_AUDIT_CARD_02` / *Debit Card Transaction*. Header:
```
OrganizationID,EmployerOrganizationID,DebitCardTransactionID,GroupRefKey,DpiAccountNumber,TransactionAmount,SalesAmount,PurchaseType,ApprovalCode,PostDate,SwipeDate,Iso8583MessageTypeID,ActionCode,ParticipantDependentCardDetailID,LastFour,IsByDivision,DivisionID,DivisionName
```

Recorded here so this dead end is not walked again: no MCC, no merchant name, **no participant key
of any kind** (only `DpiAccountNumber`, `ParticipantDependentCardDetailID`, `LastFour` — none of
which join to `Participant_ID`/`Participant System ID`), and `TransactionAmount` is `0` on every
row while `SalesAmount` carries the actual value. `Iso8583MessageTypeID`/`ActionCode` are genuine
ISO 8583 fields that *could* represent declines (`1210` authorization response / `1220` financial
advice; `ActionCode` `0` approved, `900` advice-acknowledged) but the sample — 116 rows spanning
2026-01-07 to 2026-09-11 — contained none, despite covering a known decline for that employer on
08/18. **Do not return to this export for declines.**

### Claim Search report — dead end, not exportable

The Summit UI report that first surfaced declines is **not exportable**. It is fully superseded by
the Transaction export with `Decline Reason` selected. Recorded only so a future session does not
rediscover it and try again.

## Employer designation (V114)

Card-decline monitoring is opt-in per employer, since check #3 has no other narrowing available
(`Plan Type Code` is blank on every decline row — see above).

- **Table `audit_decline_employer`** — PSP-scoped opt-in list. Row presence is the designation:
  no `is_active` column, exactly V101's reasoning (a designation creates nothing in Summit and is
  not an upsert identity for anything, so removing the row is the only "off" state). No seed rows.
  `UNIQUE (psp_id, employer_id)` — one designation per employer per PSP; re-designating updates
  nothing since there is nothing to update, the row's presence *is* the fact. `employer_id INT` FKs
  to `employer(organization_id)` — the **AMS** employer primary key, deliberately **not** the
  Summit `EmployerID` the check matches on. Storing the AMS FK means the employer name resolves
  for free through the join and referential integrity holds (an employer can't be deleted out from
  under a live designation); `Employer.altId` (the Summit id) is read off the joined row only when
  the check needs to match it against the export.
- **Admin page `/AuditDeclineEmployerAdmin`**, following `SummitEmployerFlagAdmin`'s shape exactly
  (gate, PSP resolution, `action=save|delete`, flash messages, redirect-to-self). Reached from a
  right-aligned toolbar link on the card-decline detail page — **no navbar entry**, matching every
  other admin screen this framework has added.
- **`CardDeclineCheck` filters decline rows to designated employers' `altId`.** Zero designated
  employers → `NOT_CONFIGURED`, not `OK` — an opt-in feature with nothing opted in has not been
  configured, and reporting `OK` would claim all-clear on an unmonitored book. **Verified at real
  scale, 2026-09-15**: 722 decline rows at undesignated employers filtered out, 8 retained at 1 of
  2 designated employers — the filter doing real, visible work on a 7,640-row file.
- **Save-side ownership gap, recorded not fixed.** `Employer` has no PSP column (the platform is
  one PSP per installation today), so the save path can only verify "the employer exists," not
  "the employer belongs to this PSP." PSP ownership *is* enforced on delete, against the
  designation row itself, which does carry `psp_id`. Harmless on a one-PSP install; would need real
  scoping if the platform ever goes genuinely multi-PSP on one installation.
- **`altId = 0` designations are allowed, not refused, and flagged in the UI.** T265 records that
  the demo J1 fixture produces `altId = 0` while a real J1 file populates it correctly — refusing a
  zero-`altId` designation outright would permanently block an employer whose id simply hasn't
  refreshed yet, rather than one that never will.

## Name resolution

Employer and participant names resolve on the card-decline detail page, in-request, from AMS rows
already in hand — nothing is persisted, and `audit_run` stays counts-only.

- **Employer name** from `Employer.employerName`, via the designated-employer join already
  established above — no separate lookup needed.
- **Participant name** from `Employee.firstName`/`lastName`, keyed `Employee.id` = `Participant
  System ID` (the identity fact confirmed above).
- **Degrades to the bare id when no name resolves** — a refresh-lag gap, not the steady state.
  `Employee` rows are populated only by the manual `SummitImportWizard` J2/J3 path today; a daily
  J2 refresh service is planned, mirroring `SummitRefreshService`'s J1 handling, after which
  coverage should be near-complete. **Verified working, 2026-09-15**: FRANK RUSSELL and FRANK
  SILVA both resolved correctly on the designated/acknowledged run above — the join logic is
  proven, not just built.
- **LA-43 filed** (`docs/analysis/legal_assumptions.md`, not touched this pass, already current):
  joining participant names to decline reasons and MCCs makes the page a record of who attempted
  what kind of purchase and why it was refused — closer to PHI than anything the audit surfaced
  before it. Posture: PSP-admin gated (the framework's own gate, no wider), in-request, nothing
  persisted, no SSN read from anywhere.
- **J2 export header, recorded for the refresh build** (not yet built — this is reference for
  whoever builds it):
  ```
  Employer_ID,SetupCompletionDate,EmployerCustomID,Organization_ID,EmployerName,EmployerOrganizationID,Participant_ID,User_ID,FirstName,LastName,ParticipantCustomID,UserStatus,Email,Address1,Address2,City,State,ZipCode,MobilePhone,HomePhone,WorkPhone,IsRegisterdToPortal,FailedLoginCount,LastLoginDate
  ```
  Fields are not selectable. ⚠️ **A daily J2 therefore lands email, full address and three phone
  numbers for the whole book in `ExportFiles`** — no SSN, but a meaningful recurring PII load worth
  an LA-40 note when the refresh service is actually built, not deferred to then. **`IsRegisterdToPortal`
  is misspelled in Summit's own header** — code must match it exactly, the same class of gotcha as
  `PendindCardTransaction` on the balance-snapshot export (see "Next" item 4). **Open question for
  that build**: `SummitImportService.importEmployees` is the existing J2/J3 handler — whether a
  J2-only feed satisfies it, or whether J3 columns are required too, is unverified.

## Summit participant deep link

`SummitParticipantLinkResolver`, mirroring `SummitEmployerLinkResolver`'s shape exactly: composes
`{SUMMIT_PATH}/ParticipantModule/EditParticipant.aspx?isGlobalSearch=true&tpaGuid={SUMMIT_TPA_GUID}&participantId={Participant System ID}`.
Both constants are pre-existing (already required for the employer link) — no new config key.
Degrades to plain text on the detail page when either is absent, never a broken link. Unlike the
employer link, there is no AMS-side id-resolution step first — `Participant System ID` is already
the Summit-native id on the export row. `participantId` is appended **unencoded**, matching the
employer resolver's own treatment of its (numeric, `int`) id; `Participant System ID` is a
`String` read from a CSV cell, so nothing at the type level guarantees it stays numeric the way an
`int` does. Reversal: one `URLEncoder.encode` call.

## Finding acknowledgment (V115)

- **Table `audit_finding_ack`** — generic on `check_key` + `finding_key`, so other checks can be
  added without a second migration; **wired to `card_declines` only** this session. `UNIQUE
  (psp_id, check_key, finding_key)` — one acknowledgment per finding; changing state updates the
  row rather than inserting a second.
- **Two states, and the distinction is the entire point of the feature.** `IGNORED` suppresses
  unconditionally until the row is removed — a known ongoing condition not worth seeing again.
  `HANDLED` suppresses only while the finding's **current** decline count `<= observed_count` and
  its current most-recent-decline date `<= observed_through` — both captured at acknowledgment
  time. Any new activity past either boundary re-surfaces the finding. A permanent Handled would
  swallow a fresh decline on a premium-bearing card, which is precisely the silent-failure shape
  this check exists to prevent. Either observed value missing → surfaced, never suppressed on
  incomplete data — a fail-open guard, not a convenience default.
- **`findingCount` excludes suppressed findings**, so the hub count and the navbar badge both drop
  when something is acknowledged. The summary line reports surfaced count plus a handled/ignored
  breakdown of what's suppressed, so the hub row stays honest about why a number moved.
- **The MCC summary is computed over all decline rows, acknowledged or not.** It is a
  card-configuration diagnostic (which MCC to consider enabling), not a per-participant finding —
  suppressing a participant must not distort what the diagnostic reports about an MCC. Mechanically
  guaranteed, not just documented: the MCC accumulation happens per-row inside the check's private
  `evaluate`, before the acknowledgment split (which operates on already-aggregated
  per-participant findings) even runs.
- ⚠️ **Acknowledging a check-level `ERROR` or `NOT_CONFIGURED` is deliberately out of scope.**
  Those mean the check could not evaluate at all — a missing export, an unmapped vocabulary, no
  designated employers. Suppressing them would hide a broken configuration rather than a resolved
  finding. That case wants a snooze with an expiry, a genuinely different feature, not yet
  designed and not to be backed into by extending this one.
- **Known exposure, recorded not fixed:** the server trusts the posted `observed_count`/
  `observed_through` rather than re-reading them at save time. A stale page left open, or two
  admin tabs racing, could acknowledge against numbers that have since moved, silently swallowing
  one decline until the count grows past what was actually observed. Low probability, non-zero.
  Closing it means a server-side re-lookup of current count/date at save time, which changes the
  design's stated intent (the admin's *observed* values, not a server recomputation) — a real
  trade-off, not a bug to just fix. See "Open follow-ups" item 6.

## Stale hub and badge after acknowledgment — fixed

- **Cause.** The Audit Hub table and `AuditService.getActionCount()` (which feeds the navbar
  badge) read stored `audit_run` rows, written only when a check actually runs. Acknowledging a
  finding writes to `audit_finding_ack` and re-ran nothing, so no page reload — F5, hard refresh,
  neither — could change a stored number; only "Run now" corrected it. The detail page was always
  right, because `readLive()` recomputes per request — producing exactly the visible
  inconsistency Kevin hit: the participants table showing zero surfaced while the hub above it
  still said 2.
- **Fix.** `AuditService.runOneCheck(checkKey)` — runs exactly one registered check, synchronously,
  and persists its result the same way `runAll` does, sharing the same `running` `AtomicBoolean`
  guard as the manual and scheduled paths so mutual exclusion is automatic; a re-run here can never
  race "Run now" or the daily schedule in either direction. The card-decline detail servlet calls
  it after a successful `ack`/`unack`, before the redirect. **Only `CardDeclineCheck` is re-run** —
  structurally guaranteed: the method finds one check by key and every downstream call
  (`evaluate`/insert) threads only that local variable, never the full registry again.
- **New trigger value `ACK_RERUN`** — 9 characters in `audit_run.run_trigger VARCHAR(10)` (V099),
  fits with one to spare, no schema change — alongside the existing `SCHEDULED` and `MANUAL`, so
  the run history stays honest about *why* a count changed outside the hub's own "Run now" or the
  daily schedule.
- **All three failure paths preserve the acknowledgment.** A run already in progress: the
  acknowledgment is saved, the re-run is skipped (not retried, not lost), and the flash message
  says a run is already in progress so the hub will catch up on its own. A re-run that throws
  (e.g. the export is missing): caught at the servlet, the acknowledgment stands, the flash message
  says the re-run failed and the hub count will update on the next run. An `EntityManagerFactory`
  failure inside the re-run itself: same outcome, same message — a stale count that self-corrects
  on the next run beats a button that silently did nothing, or worse, discarded the
  acknowledgment.
- **Accepted cost.** The acknowledgment POST now pays one SFTP fetch and a full file parse before
  redirecting. Fine at 7,640 rows today; will degrade quietly, not suddenly, as the export grows —
  worth watching, not yet worth engineering around.
- **Runtime-verified, 2026-09-15**: the hub and badge now update immediately on acknowledgment or
  un-acknowledgment, without a manual "Run now" click — confirmed by observation, not just by the
  reflective test suite that exercised the guard/exception paths.

## Unmapped `TransactionType` values — named

- **`Evaluation.ignoredTypes` widened `Set<String>` → `Map<String,String>`**: the case-folded
  value is the matching/dedupe key, the first-seen **original-case** text is what's displayed — so
  a value can be pasted straight into `ssa.properties` without re-typing or re-casing it by hand.
- **Named in two places**: the detail page's header block (first 12 values, with a remainder count
  above that), and total-failure condition 3's message (first 5, keeping `audit_run.summary`
  comfortably inside its truncation cap regardless of how many distinct values a file carries).
- **It paid for itself on first use.** Check #2's single ICHRA/Ins125+ row, once real PremiumPath
  data reached the export (2026-09-15), carries `TransactionType = Election`. ⚠️ **Do not add
  `Election` to the contribution list** — it is the annual election figure ($100–$7,500 observed
  in the FSA sample), not money that moved. Adding it would post a full year's figure as one
  month's contribution with no disbursement against it, flagging **every** new ICHRA enrollee at
  plan start as a false finding — the exact kind of noise the FSA-plan-type lesson already warned
  against, in a new column.
- **Check #2's current state is therefore correct and will persist**: a plan exists with an
  election recorded and no contribution posted against it yet. `ERROR` (condition 3 — nothing
  matched either configured type list) until a real contribution transaction appears in the
  export. Without the naming feature, "1 unmapped value" would have looked like a missing config
  entry, and the obvious fix — add whatever the value was to a list — would have been the wrong
  one, silently, the first time this check ever saw real ICHRA data.
- **Worth designing later, not now:** "a plan exists with no funding activity yet" and "your
  config is wrong" currently share one status, `ERROR`. A check that stays red for weeks trains
  people to stop reading the hub. Condition 3 is firing correctly here — this is not a bug — but a
  distinct status, or a scoped suppression for exactly this case, may be worth designing once real
  PremiumPath groups are live in volume and this stops being a single row.

## New assumptions — carried forward, with reversal cost

- **`Claim -Participant Portal/Mobile` is a claim record, not a payment — confirmed, not merely
  inferred.** All 21 `ClaimsPayment` rows (16 ACH, 5 Check) match a claim row on
  `ClaimKeyCheckNumber` with an identical amount (key `97687`, $312.50 on both), and zero of the 87
  `Debit Card` keys appear as a payment row. Excluded from check #2's disbursement list;
  including it would double-count every portal reimbursement. Reversal: config only, no code
  change.
- **Check #3's `ERROR` conditions are computed over every row, not just decline rows.** A file
  where non-decline rows carry garbled dates but decline rows parse fine would still report
  `ERROR` and surface nothing — the check would go silent on a file that is actually fine for its
  purpose. Given 6,682 of 7,640 rows in the profiled export were non-declines, **this is a live
  exposure, not a theoretical one.** Reversal: filter to decline rows (`declineReason` non-blank)
  before the date-parse funnel in `CardDeclineCheck.evaluate` — a few lines. See "Open follow-ups"
  item 4.
- **Check #3's `participantId` is appended to the Summit URL unencoded** — see "Summit participant
  deep link" above.
- **Zero declines in the window is `OK`, not `ERROR`** — a deliberate divergence from check #2's
  fourth total-failure condition, since an empty window is the good outcome for this check, not a
  misconfiguration signal. Recorded explicitly so a future reader does not "fix" this into
  consistency with check #2 and turn a healthy card program into a daily false alarm.
- **Plan-type list (check #2) matches `PlanTypeID`, not `PlanName`.** Ids survive a rename in
  Summit; names do not. Reversal: one line in the filter, plus `csvSet(..., true)` for case-fold if
  switching to name matching.
- **Date-format tolerance list** in both checks still accepts `M/d/yyyy`, `yyyy-MM-dd`, `M-d-yyyy`
  on the leading token; only the first is confirmed against real data in either export. Reversal:
  trim the list to the confirmed shape.
- **Amount parsing** (check #2 only — check #3 does not need signed/parenthetical amounts, since
  `Total Transaction Amount` was positive in every profiled row) strips `$`/`,` and treats `(x)` as
  negative before `abs()`. None of that occurred in the sample; harmless tolerance. Reversal:
  trivial.
- **`YearMonth.now()` (check #2) / `LocalDate.now()` (check #3) are server-local.** A UTC server
  evaluates a slightly different "now" than CST for a few hours around midnight/month boundaries.
  Low impact at daily cadence. Reversal: pin a zone in one call each.
- **Check #2's whole-file basis for total-failure condition 3.** Type classification runs before
  the month filter so "type lists match nothing" is judged over every scoped, date-parsed row in
  the file, distinguishing it from "this month has no rows" (condition 4). Side effect: the
  unmapped-type count in the summary covers the whole file, not just the month. Reversal: move the
  month test above type classification — two lines.
- **Both checks' required config keys are per-PSP by construction**, because one installation is
  one PSP (`AuditService` is built with a single `pspId` in `EmfListener`). No per-PSP keying
  inside `ssa.properties`. Reversal: none needed unless the framework goes multi-PSP.
- **`audit_decline_employer.employer_id` has no `is_active` guard against a deactivated AMS
  employer** — a designation survives the employer going inactive in AMS, and continues matching
  if the employer's `altId` still appears in the export. Consistent with the "row presence is the
  designation" rule; no reversal needed unless deactivation should imply un-designation.
- **`audit_finding_ack` trusts posted `observed_count`/`observed_through`** — see "Finding
  acknowledgment (V115)" and "Open follow-ups" item 6.

## Open follow-ups

Replaces the prior list in full — renumbered, one item added, one reworded for emphasis.

1. ⚠️ **`AmsDataGlobal.java:425` and `:431` hard-code the Summit host and TPA GUID as catch-block
   fallbacks** for `SUMMIT_PATH`/`SUMMIT_TPA_GUID` when the DB constant lookup throws. Any
   installation whose lookup fails silently composes Summit links — including the new participant
   link this session added — into **SSA's own Summit tenant**, not that installation's.
   Pre-existing, same class of defect as T243, out of every run's scope fence this whole session.
   **Highest value of these follow-ups** — it is a wrong-answer-that-looks-right defect, and it
   only bites on a second installation, which is the whole platform ambition.
2. **The Audit Hub badge reports the scheduler *setting*, not its *state*.** `AuditHub` re-reads
   `AUDIT_SCHEDULER_ENABLED` per request; `EmfListener` reads it once, at Tomcat startup, to decide
   whether to call `AuditService.start()`. Setting the constant to `true` on a running Tomcat flips
   the badge to "Enabled — daily" immediately, while no scheduler thread exists. Only a restart
   makes the two agree. See "Deployment state" below.
3. **Check #2's (and `IchraUncodedParticipantsCheck`'s) filename matching is `startsWith`, not
   exact.** `ZZ_PARTICIPANT_HISTORY_AUDIT` matched a `..._DEL` test template in the real August
   run — confirmed, not hypothetical. Check #3 already matches `{prefix}_Export_` exactly (built
   that way from the start, specifically because this looseness had already been observed).
   Carrying the fix back is one line in `FundedPurseNoDisbursementCheck` and the same one line in
   `IchraUncodedParticipantsCheck`.
4. **Check #3's date-parse `ERROR` conditions are computed over all rows, not just decline rows.**
   A file where non-decline rows have garbled dates but decline rows parse fine reports `ERROR`
   and surfaces nothing. 6,682 of 7,640 rows were non-declines, so this is a live exposure, not a
   theoretical one.
5. **`docs/analysis/audit_framework.md` still claims check #1 filters on a configured employer
   list, and still describes a three-method `AuditCheck` contract.** Both false — check #1's
   employer scope is Summit-side (TA-56) with no AMS-side list ever built, and the real contract
   has four methods (`detailPath()` is missing from the doc's description). This is the doc a
   future session is most likely to read first, so its staleness compounds.
6. **Acknowledgment trusts posted `observed_count`/`observed_through` rather than re-reading them
   at save time** — see "Finding acknowledgment (V115)" for the exposure and why it isn't a quick
   fix.
7. **V114's `schema_version` description truncated** (MySQL warning 1265, `description
   VARCHAR(200)`, the source string ran 276 characters). Cosmetic — the version, script name and
   applied-on timestamp all landed correctly, and V114's own `schema_info` view carries the full
   version regardless. V115's description was length-checked at 112 characters before writing it,
   specifically to not repeat this.

## Scheduler enablement

- `AUDIT_SCHEDULER_ENABLED` — a DB `constant` row, value the literal string `true`
  (`equalsIgnoreCase`, so case-insensitive; **not** `1`, `Y`, or anything else; **not** trimmed, so
  a trailing space fails). **No row exists on this installation; nothing seeds it** —
  `DatabaseInitializer` does not create it, and constant rows are never migrations by project rule.
  This is **D-98 step 3**, status Not started.
- `SUMMIT_REFRESH_ENABLED` — identical shape, gates `SummitRefreshService`. Also off, also
  unseeded.
- Two routes to set either, neither requiring hand-written SQL as the only option: a plain
  `INSERT ... ON DUPLICATE KEY UPDATE` on the target MySQL (the D-82 precedent shape), or the
  Master console's `/ManageInstallation?id={id}` constant push (gated on `AppConfig.isMaster()` +
  session `isPspAdmin`, PUTs to `/api/v1/system/constants` on the target, no SQL involved).
  **Either way, Tomcat must be restarted afterward** — see follow-up 2 above for why the badge
  cannot be trusted as a substitute for that restart.
- `AuditService.start()` calls `scheduleAtFixedRate(10 min initial delay, 24 h interval)` — **there
  is no time-of-day.** It fires 10 minutes after Tomcat starts, then every 24 hours from that
  moment; every restart re-phases it, and the phase is not configurable
  (`RateCacheWarmService` is the same shape with a 5-minute initial delay instead of 10).
  **The scheduled path itself has never run on any installation** — every real run recorded in
  this close-out was a manual "Run now" or an acknowledgment-driven `ACK_RERUN`.

## Deployment state

- **Highest migration: V115.** Pending on Production: **V105–V115, all unapplied.** V114 and V115
  are applied **locally only** — neither has touched Production, Demo, BPO, or Master.
  `docs/analysis/migration_tracker.md` is the authoritative per-environment source; both V114 and
  V115 are recorded there as authored-not-applied.
- **Scheduler still disabled** on this installation — see "Scheduler enablement" above; this is
  unchanged by anything shipped this session.
- **Export schedules, all Summit-side, all confirmed working:**
  - **Participant Account History** (check #2) — window Previous Month Start → Previous Month End,
    **daily**.
  - **Transaction** (check #3) — window Current Calendar Year Start → Current Date, **daily or
    twice daily**, scoped Summit-side to the employers being watched.
  - **Employer J1** — **daily**; Summit offers 7am/9am/11am/1pm delivery slots. Recorded here
    because the planned J2 refresh service (see "Name resolution") will need its own schedule
    decision, and J1's existing cadence is the nearest precedent.
- **Git**: everything from this session is committed (`fb1a03c`), branch is one commit ahead of
  `origin`, not pushed. See "Shipped" above — this line exists so "Deployment state" alone answers
  "is this on the server anywhere," and the answer is no: nothing has left this workstation.

## Next

1. **Push `fb1a03c` to `origin/refactor/modernize-architecture`.** The commit step from an earlier
   revision of this close-out's own "Next" list is done; pushing is what remains, and it is a
   git-mutating action no run in this session's scope fence was authorised to perform.
2. **Follow-ups 1 and 2** — both wrong-answer-that-looks-right defects, the same class this session
   removed three times from inside the checks themselves (a total-failure guard that would
   otherwise report `OK` on nothing evaluated, twice, plus the zero-designated-employers guard).
   A control surface that looks correct while quietly being wrong is the recurring shape worth
   hunting for next, not just these two instances of it.
3. **Deploy V105–V115 to Production and set the two scheduler constants** (`AUDIT_SCHEDULER_ENABLED`,
   `SUMMIT_REFRESH_ENABLED`) — see "Scheduler enablement." Nothing in this session's work has run
   unattended anywhere, ever.
4. **Build check #4 off the participant balance snapshot** (`JJ_TEST_PBA`, 27 columns, entirely
   name-free, `EmployerYTDContribution` kept separate from the participant's own YTD contribution,
   `PendindCardTransaction` — Summit's own misspelling, must be matched exactly, the same class of
   gotcha as `IsRegisterdToPortal` on the J2 export above). Two single-row predicates, no stored
   state, no `TransactionType`-style vocabulary config, no month logic: employer contribution
   nonzero with `YTDPayments` at or near zero, and `PendindCardTransaction > 0`. **Design caveat to
   carry into that build:** ICHRA allowance legitimately exceeds premium in some plan designs, so a
   contribution/payments gap alone is not proof of a missed payment — a gap approaching the full
   contribution is the actual signal.
5. **Two DataPath questions, pre-launch not curiosity:**
   - Does Summit's Card Deactivation suspend the whole card, or only the specific account that
     failed substantiation? `Card blocked decline` was 323 of 958 profiled declines — the single
     largest reason — and **FRANK SILVA is now a live, named instance** (4× blocked at a pharmacy,
     2026-09-15) to ask DataPath about directly rather than hypothetically. If deactivation is
     card-level, an unsubstantiated FSA swipe can block that same card's premium draft and lapse
     ICHRA coverage — the exact failure mode this whole check exists to catch, caused by an
     unrelated FSA claim.
   - Can `Plan Type Code` be populated on decline rows at all? That one change would make the
     decline check plan-scopable the way check #2 is, rather than employer-scoped only — it would
     not replace employer designation (Summit-side scoping still has value) but would let the
     check narrow to ICHRA/Ins125+ declines specifically, the same signal-vs-noise improvement
     check #2 got from its plan-type filter.

## Context worth carrying forward

Kevin's stated framing this session: **SWBD is sold.** The constraint from here is readiness at
first enrollment, not proving capability to a prospect. These audit checks are the path to that
readiness, not a detour from it — an ICHRA participant whose premium silently fails is what loses a
client in month two, quietly, long after the sale closed. `docs/ams_to_be_vision.md` (or wherever
the project's current framing doc lives) still reads "speed to a demonstrable product for Forrest"
and should be updated to reflect that the sale is done and the goal has shifted to operational
readiness.

## Deployment prerequisites

Not work items — things that must exist before either check can be demonstrated on a fresh
installation.

**Check #2 — six keys** (`SUMMIT_AUDIT_FUNDED_MONTH_BASIS` and `SUMMIT_AUDIT_FUNDED_EMPLOYER_IDS`
both removed in earlier runs; do not re-add either):

```properties
# --- T237 check #2: funded purses with no disbursement (FundedPurseNoDisbursementCheck) ---
# Employer scope is Summit-side: the export template's required Employer selector. No AMS key.
# Summit template name (filename prefix) of the Participant Plan History export. Required.
SUMMIT_AUDIT_PLAN_HISTORY_EXPORT_PREFIX=
# Comma-separated PlanTypeID values (numeric id, not PlanName). Required.
# Use 1002 (ICHRA) and 1016 (Ins125+) only -- FSA plan types make this check unusably noisy
# (health FSA "funded, nothing spent this month" is the normal state, not a signal).
SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS=1002,1016
# Comma-separated TransactionType values meaning money in / money out. Both required.
# Trimmed and case-insensitive; a value in both lists is NOT_CONFIGURED.
# Do NOT add "Claim -Participant Portal/Mobile" to the disbursement list - it is the claim
# record and would double-count every portal reimbursement (confirmed on ClaimKeyCheckNumber).
# Do NOT add "Election" to the contribution list - it is the annual election figure, not money
# moved; adding it flags every new enrollee at plan start (confirmed 2026-09-15).
SUMMIT_AUDIT_FUNDED_CONTRIBUTION_TYPES=Participant Scheduled Contribution
SUMMIT_AUDIT_FUNDED_DISBURSEMENT_TYPES=Debit Card,Participant Portal/Mobile ClaimsPayment ACH,Participant Portal/Mobile ClaimsPayment Check
# Month basis is SystemDate, always (EventDate is blank on every card/claim row) - not configurable.
# Closed months back to evaluate. Default 1 (most recent fully-closed month). Must match the
# export's own window shape -- 1 pairs with "Previous Month Start -> Previous Month End".
#SUMMIT_AUDIT_FUNDED_MONTH_OFFSET=1
# Newest matching export older than this is ERROR. Default 36.
#SUMMIT_AUDIT_PLAN_HISTORY_MAX_AGE_HOURS=36
# Byte cap is the existing SUMMIT_AUDIT_EXPORT_MAX_BYTES (default 16777216) - reused, no new key.
```

**Check #3 — four keys, no employer or plan-type key possible or needed** (employer scope is now
data, in `audit_decline_employer`, maintained through `/AuditDeclineEmployerAdmin` — not a key
here):

```properties
# --- T237 check #3: card declines (CardDeclineCheck) ---
# Employer scope is data, not config -- see audit_decline_employer / AuditDeclineEmployerAdmin.
# Plan Type Code is blank on every decline row, so no plan-type scoping is possible either.
# Summit template name (filename prefix) of the Transaction export. Required. Exact match on
# "{this}_Export_..." -- a test template sharing a stem will not silently win.
SUMMIT_AUDIT_DECLINE_EXPORT_PREFIX=
# Rolling window in days, inclusive of today. Optional, default 14.
#SUMMIT_AUDIT_DECLINE_WINDOW_DAYS=14
# The Decline Reason value driving the MCC summary table. Optional, default below.
# Compared trimmed, case-insensitive. Do not build logic on any other reason string -- Summit's
# vocabulary is free text (see the observed-vocabulary table in this close-out).
#SUMMIT_AUDIT_DECLINE_MNQ_REASON=Merchant Not Qualified
# Newest matching export older than this is ERROR. Optional, default 36.
#SUMMIT_AUDIT_DECLINE_MAX_AGE_HOURS=36
# Byte cap is the existing SUMMIT_AUDIT_EXPORT_MAX_BYTES (default 16777216) - reused, no new key.
```

`SUMMIT_PATH` and `SUMMIT_TPA_GUID` are pre-existing constants (already required for the employer
link) — the participant link needs neither a new key nor any change to either block above.

Both checks' Summit-side export templates and windows are recorded under "Confirmed export facts."

Docs pending, not authorised in any run this session: a row per key in
`docs/business/summit_data_exchange.md`'s config registry, a `D-NN` item in
`docs/deployment_backlog.md` for each check alongside D-98, and T-numbers in
`docs/analysis/project_backlog.md` for both checks (standing rule S16-G: file the T-number in the
same run that closes the work — none of that has happened yet for either check, nor for V114/V115).

## SQL close-out audit

**No SQL was run against any database by any run this session.** Two migrations were produced —
**V114** (`audit_decline_employer`) and **V115** (`audit_finding_ack`) — both versioned, both
registered in `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql`,
neither containing `INSERT INTO constant`, both now **committed** (`fb1a03c`) rather than merely
authored in a dirty working tree, as an earlier revision of this close-out described them. **No
orphaned `.sql` files** — `docs/migrations/` holds `V025`–`V115` plus the pre-existing
`seed_ndt125_questionnaire.sql`. **Highest migration version: V115.** **Pending deployment:
V105–V115** on Production; V114/V115 applied locally only. **No schema described but not
scripted.** The only SQL *stated*, never run or scripted, is the `AUDIT_SCHEDULER_ENABLED`
constant insert under "Scheduler enablement" — a D-98 deployment item, never a migration by
project rule.
