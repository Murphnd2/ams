# Session close-out — 2026-09-14 — Audit checks #2 and #3: funded purses, card declines

Branch `refactor/modernize-architecture`, HEAD `be9bb2e` throughout. This is the authoritative,
stand-alone close-out for the day's audit-framework work — it **supersedes**
`docs/session_closeout_2026-09-14_audit_funded_purse_check.md`, which is stale on several points
this file corrects and now carries only a pointer to here. Every repo fact below was re-read from
the tree at close-out time — `git status`, `git diff --stat`, `ls docs/migrations`, and the actual
source of `FundedPurseNoDisbursementCheck.java` (config-key count and required-header list
verified directly, not taken on the closing prompt's word) — not restated from any prompt.

Framework: T237 phase 1 (`docs/analysis/audit_framework.md`; framework built at `3ebf5da`, first
check `IchraUncodedParticipantsCheck`). Eight runs on one thread built and hardened check #2
(`FundedPurseNoDisbursementCheck`), built check #3 (`CardDeclineCheck`) fresh against the pattern
check #2 established, added a Summit participant link to check #3's detail page, and — twice —
corrected the running close-out against real Summit data. Both new checks were **run against real
Summit exports today** (see "Verified runs" below) — this is no longer speculative work.

## Shipped

**Nothing was committed this session.** The tree is dirty throughout. `git log` still ends at
`be9bb2e` ("Summit deep links: add on-demand processing and three receipt management page keys"),
which predates this session.

## In flight

Nine paths, all awaiting Kevin's review and commit. **Only `AuditService.java` is a modified
tracked file** (registration of two checks, `+4/−1`); everything else is new and untracked.

| Path | State | What it is |
|---|---|---|
| `src/main/java/net/superiorstate/ams/data/service/audit/FundedPurseNoDisbursementCheck.java` | untracked, new | Check #2. Key `funded_purse_no_disbursement`, detail path `/AuditFundedPurse`. **Runtime-verified** (see below). |
| `src/main/java/net/superiorstate/ams/controller/admin/AuditFundedPurse.java` | untracked, new | Check #2's detail servlet. |
| `src/main/webapp/WEB-INF/view/a/admin/auditFundedPurse25.jsp` | untracked, new | Check #2's detail page — six columns (`Participant_ID`, `ParticipantPlan_ID`, `PlanName`, contribution total, disbursement total, month). |
| `src/main/java/net/superiorstate/ams/data/service/audit/CardDeclineCheck.java` | untracked, new | Check #3. Key `card_declines`, detail path `/AuditCardDeclines`. **Runtime-verified on its first run** (see below). |
| `src/main/java/net/superiorstate/ams/controller/admin/AuditCardDeclines.java` | untracked, new | Check #3's detail servlet; also builds the per-participant Summit link map (see "Participant link" below). |
| `src/main/webapp/WEB-INF/view/a/admin/auditCardDeclines25.jsp` | untracked, new | Check #3's detail page — header block, participants table (decline count as a badge, Participant System ID linked to Summit when configured), MCC/unqualified-merchant summary table. |
| `src/main/java/net/superiorstate/ams/data/resolver/SummitParticipantLinkResolver.java` | untracked, new | Builds the Summit "Edit Participant" URL from the existing `SUMMIT_PATH`/`SUMMIT_TPA_GUID` constants, mirroring `SummitEmployerLinkResolver`'s style exactly. No new config key. |
| `src/main/java/net/superiorstate/ams/data/service/audit/AuditService.java` | **modified**, +4/−1 | Registration only: `CHECKS = List.of(new IchraUncodedParticipantsCheck(), new FundedPurseNoDisbursementCheck(), new CardDeclineCheck())`. |
| `docs/session_closeout_2026-09-14_audit_funded_purse_check.md` | untracked, superseded | Left in place with a pointer to this file (see "Files touched" in the compliance statement). |

No migration, no entity, no DAO change, no touch to `navbar25.jsp` — the badge already sums
`ACTION` counts across the registry, and the hub lists every registered check with a Details link.
**Nothing here is runtime-verified on production** — both real runs described below were against
whatever Summit tenant/export path this installation's `ssa.properties` currently points at, not a
production deploy.

## Verified runs — real Summit data, 2026-09-14

Both checks have now read a real export at least once. This is the load-bearing update to the
prior close-out, which described everything as synthetic-only.

### Check #2 — `FundedPurseNoDisbursementCheck`

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
- **Current state with the FSA ids removed:** `ERROR` — *"Nothing evaluated: 0 of 13295 rows
  matched the configured plan-type scope."* **This is correct behaviour, not a regression** — the
  scope-matching row count and the total-failure guard both work exactly as designed. It will
  persist until this installation has a real PremiumPath (ICHRA) or Ins125+ group. See "Next" item
  1.

### Check #3 — `CardDeclineCheck`

- **First run**, 14-day default window: `ACTION` — **22 participants declined in the last 14 days
  (2026-09-01 – 2026-09-14), 41 decline rows at 15 employers, 7 distinct `Decline Reason`
  values** — from `ZZ_TRANSACTION_HISTORY_AUDIT_Export_20260914184135647.CSV`.
- The rolling window and the participant-level finding unit both worked as designed: 958 total
  decline rows in the file (the full profiling sample, not filtered to the window), 41 fell inside
  the 14-day window, and those 41 rows collapsed to 22 distinct participants — a badge someone can
  actually read, unlike check #2's 197 on FSA data above.

## Decisions made — and what each closed

- **Audit checks #2 and #3, not new surfaces.** The `AuditCheck` contract
  (`key`/`label`/`detailPath`/`evaluate`) fit both exactly; registration is one `List.of(...)`
  element each in `AuditService`. Closed: where each lives and how it is reached.
- **Employer scope is Summit-side for all three checks now — resolved, not open.** This closes the
  "two employer-scoping conventions" question the prior close-out left open. `IchraUncodedParticipantsCheck`
  established the convention first (TA-56, the export template's own Employer selector); check #2
  briefly carried an AMS-side `SUMMIT_AUDIT_FUNDED_EMPLOYER_IDS` key, which was removed because it
  duplicated the template's selector — a second place to maintain and a second way for a group to
  be silently excluded. Check #3 never had an AMS-side employer key at all, both because the
  convention was already settled by then and because its source data leaves no other option (see
  below). One convention, three checks.
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
  evaluation month. The plan-type-scope `ERROR` above (condition 1) is exactly this working as
  intended once the FSA ids were removed.
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
  why employer scope being Summit-side is not optional for this check the way it was a choice for
  check #2: there is nothing else to scope on.
- **Check #3's filename matching is exact** (`{prefix}_Export_` literal, then the 17-digit
  timestamp), unlike the other two checks' `startsWith(prefix)`. This was specified from the start
  for check #3, precisely because the `startsWith` looseness had already been observed to let
  `ZZ_PARTICIPANT_HISTORY_AUDIT` match a `..._DEL` template on check #2. See "Open follow-ups" item
  4 for carrying the fix back to the other two checks.
- **Check #3's MNQ (Merchant-Not-Qualified) reason match is one configured string, trimmed and
  case-insensitive** — not a list, because only one reason drives the MCC diagnostic table. Every
  other `Decline Reason` value is free text the check never branches on.
- **The Summit participant link on check #3's detail page mirrors `SummitEmployerLinkResolver`'s
  shape exactly**: a small static resolver reading `SUMMIT_PATH`/`SUMMIT_TPA_GUID` via the
  `ServletContext` → `AmsDataGlobal` route, returning `null` (never a broken link, never an
  exception) when either constant is unset. No new config key — both constants already exist on
  any installation that has the employer link working. Unlike the employer link, there is no
  AMS-side id-resolution step first: `Participant System ID` is already the Summit-native id on the
  export row.
- **Expected-amount matching and partial-payment detection remain out of scope for both checks.**
  Both would need a premium-expectation source AMS does not have, for a weaker signal than "nothing
  paid" / "declined."

## Confirmed export facts

### Participant Account History export (check #2's source)

- **Real header**, `PTName` present in the unfiltered export (never read):
  `Participant_ID,ParticipantPlan_ID,Employer_ID,Organization_ID,EmployerOrganizationID,DivisionName,EmployerPlan_ID,PlanName,PlanTypeID,StartDate,EndDate,PlanYear,ID,TransactionType,EventDate,SystemDate,ClaimKeyCheckNumber,Transactionamt,User_ID,EmployerPlanDetailForPlanYear_ID,EmploymentStatusID,UserStatusID,ByDivision`.
- **Seven required headers** (verified directly in `FundedPurseNoDisbursementCheck.java` at
  close-out time, not merely asserted): `Participant_ID`, `ParticipantPlan_ID`, `PlanName`,
  `PlanTypeID`, `TransactionType`, `SystemDate`, `Transactionamt`. `Employer_ID` is optional —
  read only for the summary line's distinct-employer count, never validated, never filtered on.
- **Filename shape**: `{template}_Export_{17-digit timestamp}_CSV.csv` — a token sits between the
  timestamp and the extension. Relaxed pattern:
  `^.+_(\d{17})(?:_[A-Za-z0-9]+)*\.[A-Za-z0-9]+$`. ⚠️ This is `startsWith`-rooted, not exact — see
  "Open follow-ups" item 4.
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

### Transaction export (check #3's source) — new this session

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
- **Two design constraints this creates**: no plan-type scoping is possible on a decline row, so
  employer scope must be (and is) Summit-side; and MCC is the only merchant signal, since
  `Merchant Name` is blank.
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
IIAS status.

**Repeat-decline clusters** — the shape a failed recurring premium takes: participant 28674
declined **61 times at $329** (`Exceeded Funds Available`); 27801 eleven times at $175
(`Card blocked decline`); 24440 across three different amounts, all `Merchant Not Qualified`. This
is exactly the pattern `CardDeclineCheck`'s prominent decline-count badge and per-participant
reason breakdown were built to surface.

**Scale, live on FSA data today**: 59 participants card-blocked across 28 employers; 63 with
insufficient funds; 57 hitting unqualified merchants. (These are whole-file counts from the 958
profiled declines, not the 22-participant, 14-day-window finding above.)

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

## New assumptions — carried forward, with reversal cost

- **`Claim -Participant Portal/Mobile` is a claim record, not a payment — confirmed this session,
  not merely inferred.** All 21 `ClaimsPayment` rows (16 ACH, 5 Check) match a claim row on
  `ClaimKeyCheckNumber` with an identical amount (key `97687`, $312.50 on both), and zero of the 87
  `Debit Card` keys appear as a payment row. Excluded from check #2's disbursement list;
  including it would double-count every portal reimbursement. Reversal: config only, no code
  change.
- **Check #3's `ERROR` conditions are computed over every row, not just decline rows.** A file
  where non-decline rows carry garbled dates but decline rows parse fine would still report
  `ERROR` and surface nothing — the check would go silent on a file that is actually fine for its
  purpose. Given 6,682 of 7,640 rows in the profiled export were non-declines, **this is a live
  exposure, not a theoretical one.** Reversal: filter to decline rows (`declineReason` non-blank)
  before the date-parse funnel in `CardDeclineCheck.evaluate` — a few lines.
- **Check #3's `participantId` is appended to the Summit URL unencoded**, matching the employer
  resolver's treatment of an `int` `employerId`. `Participant System ID` is a `String` read from a
  CSV cell, so nothing at the type level guarantees it stays numeric the way an `int` does.
  Reversal: one `URLEncoder.encode` call in `SummitParticipantLinkResolver`.
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

## Open follow-ups

1. ⚠️ **`AmsDataGlobal.java:425` and `:431` hard-code the Summit host and TPA GUID as catch-block
   fallbacks** for `SUMMIT_PATH`/`SUMMIT_TPA_GUID` when the DB constant lookup throws. Any
   installation whose lookup fails silently composes Summit links — including the new participant
   link this session added — into **SSA's own Summit tenant**, not that installation's. Pre-existing,
   same class of defect as T243, out of every recent run's scope fence (`AmsDataGlobal` is
   explicitly restricted in all three check-building runs and the link-resolver run). **Highest-value
   item in this list** — it is a wrong-answer-that-looks-right defect, not a missing feature.
2. **The Audit Hub badge reports the setting, not the state.** `AuditHub` re-reads
   `AUDIT_SCHEDULER_ENABLED` from the DB on every request; `EmfListener` reads it once, at Tomcat
   startup, to decide whether to call `AuditService.start()`. Setting the constant to `true` on a
   running Tomcat flips the badge to "Enabled — daily" immediately, while no scheduler thread
   exists. Only a restart makes the two agree. See "Scheduler enablement" below.
3. **Check #2 counts unmapped `TransactionType` values but does not name them.** The names already
   exist in memory (`Evaluation.ignoredTypes`, a `Set<String>`); `Snapshot` carries only the
   `size()`. Naming them on the detail page is a three-file change (widen `Snapshot`, one servlet
   attribute, one `<c:forEach>` in the JSP), no persistence change. **January's real run reported 9
   unmapped types** — any one of them could be an unrecognized contribution type, which would make
   those purses silently invisible to the check rather than merely uncounted.
4. **Check #2's (and `IchraUncodedParticipantsCheck`'s) filename matching is `startsWith`, not
   exact.** `ZZ_PARTICIPANT_HISTORY_AUDIT` matched a `..._DEL` test template in the real August run
   above — confirmed, not hypothetical, since that's the exact file the August finding came from.
   Check #3 already matches `{prefix}_Export_` exactly (built that way from the start, specifically
   because this looseness had already been observed). Carrying the fix back is one line in
   `FundedPurseNoDisbursementCheck` and the same one line in `IchraUncodedParticipantsCheck`.
5. **Check #3's date-parse funnel scoping** — see "New assumptions" above; the live-exposure note
   there is the actionable version of this item.
6. **`docs/analysis/audit_framework.md` still claims check #1 filters on a configured employer
   list, and still describes a three-method `AuditCheck` contract.** Both are false — check #1's
   employer scope is Summit-side (TA-56) with no AMS-side list ever built, and the real contract has
   four methods (`detailPath()` is missing from the doc's description). This is the doc a future
   session is most likely to read first, so its staleness compounds.

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
  **The scheduled path itself has never run on any installation** — both real runs recorded above
  were manual "Run now" triggers.

## Next

1. **Remove the FSA plan-type ids from `SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS`**, leaving `1002,1016`.
   Check #2 will stay `ERROR` ("0 of N rows matched the configured plan-type scope") until this
   installation has a real PremiumPath or Ins125+ group — that is correct, not broken.
2. **Commit the session's work**, then take follow-up 1 (the `AmsDataGlobal` hardcoded fallbacks)
   and follow-up 2 (the badge reporting the setting, not the state) — both are the same class of
   defect this session removed twice from inside the checks themselves (a total-failure guard that
   would otherwise report `OK` on nothing evaluated): a control surface that looks correct while
   quietly being wrong.
3. **Build check #4 off the participant balance snapshot** (`JJ_TEST_PBA`, 27 columns, entirely
   name-free, `EmployerYTDContribution` kept separate from the participant's own YTD contribution,
   `PendindCardTransaction` — note Summit's own misspelling, must be matched exactly). Two
   single-row predicates, no stored state, no `TransactionType`-style vocabulary config, no month
   logic: employer contribution nonzero with `YTDPayments` at or near zero, and
   `PendindCardTransaction > 0`. **Design caveat to carry into that build:** ICHRA allowance
   legitimately exceeds premium in some plan designs, so a contribution/payments gap alone is not
   proof of a missed payment — a gap approaching the full contribution is the actual signal.
4. **Open DataPath question, pre-launch not curiosity:** does Summit's Card Deactivation suspend
   the whole card, or only the specific account that failed substantiation? `Card blocked decline`
   was 323 of 958 profiled declines — the single largest reason. If deactivation is card-level, an
   unsubstantiated FSA swipe can block that same card's premium draft and lapse ICHRA coverage —
   the exact failure mode this whole check exists to catch, caused by an unrelated FSA claim.

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

**Check #2 — six keys, current as of this session** (`SUMMIT_AUDIT_FUNDED_MONTH_BASIS` and
`SUMMIT_AUDIT_FUNDED_EMPLOYER_IDS` both removed in earlier runs; do not re-add either):

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

**Check #3 — four keys, no employer or plan-type key possible or needed:**

```properties
# --- T237 check #3: card declines (CardDeclineCheck) ---
# Employer scope is Summit-side. Plan Type Code is blank on every decline row, so no plan-type
# scoping is possible either -- there is nothing else to filter this export on.
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
link) — the new participant link needs neither a new key nor any change to either check's block
above.

Both checks' Summit-side export templates and windows are recorded under "Confirmed export facts."

Docs pending, not authorised in any run this session: a row per key in
`docs/business/summit_data_exchange.md`'s config registry, a `D-NN` item in
`docs/deployment_backlog.md` for each check alongside D-98, and T-numbers in
`docs/analysis/project_backlog.md` for both checks (standing rule S16-G: file the T-number in the
same run that closes the work — none of that has happened yet for either check).

## SQL close-out audit

**No SQL was produced, run, or recommended in any run this session**, across all eight code/build
runs and the two documentation passes. No migration was written. `audit_run` (V099) holds both new
checks via new `check_key` values (`funded_purse_no_disbursement`, `card_declines`) — no schema
change was needed or made. **Current highest migration version: V113**
(`V113__enrollment_matrix_participant_agent_note.sql`). **Pending deployment:** V105–V113 on
Production per `docs/analysis/migration_tracker.md`, unchanged by this session. **No schema
described but not scripted.** The only SQL *stated* anywhere this session was the
`AUDIT_SCHEDULER_ENABLED` constant insert under "Scheduler enablement" above — a D-98 deployment
item, never a migration, and it was not run.
