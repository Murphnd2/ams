# Superseded — see `docs/session_closeout_2026-09-14_audit_checks_2_3.md`

**This file is stale and has been superseded.** It was written after check #2's initial build and
corrected twice in place, but predates: check #2's employer-config key removal (six keys, not
seven; seven required headers, not eight), the resolution of the two-scoping-conventions question
in favour of Summit-side scoping for all checks, check #3 (`CardDeclineCheck`) being built and
run, the Summit participant-link resolver, and both checks' first runs against real Summit data
(check #2: August/January 2026 evaluations, FSA-noise scope lesson; check #3: 22 participants in a
14-day window).

The consolidated, current close-out is
[`docs/session_closeout_2026-09-14_audit_checks_2_3.md`](session_closeout_2026-09-14_audit_checks_2_3.md).
This file is left in place, unedited below this notice, only so its content is not lost; do not
treat anything below this line as current.

---

# Session close-out — 2026-09-14 — Audit check #2: funded purses with no disbursement

Branch `refactor/modernize-architecture`, HEAD `be9bb2e` throughout. Six runs on one thread:
run 1 (Opus — Part 1 pattern survey, Part 2 build), run 2 (Sonnet — normalization check and the
silent-`OK` fix), run 3 (Sonnet — fourth total-failure condition), run 4 (Sonnet — first draft of
this close-out), run 5 (Sonnet — three defects found by profiling a real 365-row export: `PTName`,
filename pattern, `EventDate`), run 6 (Sonnet — this doc pass, correcting the close-out for run 5
and recording a second export). Every repo fact below was re-read from the tree at close-out time
— `git status`, `git diff --stat`, `ls docs/migrations`, and the four in-flight files — not
restated from the prompts that drove each run.

The check is T237 phase-1 framework check #2 (`docs/analysis/audit_framework.md`; framework built
at `3ebf5da`, first check `IchraUncodedParticipantsCheck`). Purpose: a contribution-funded purse
— PremiumPath (ICHRA allowance, excepted-benefit premium, off-exchange premium, post-tax premium)
or health FSA — that shows money in and nothing out in a month is a payment that never happened.
For PremiumPath that is a coverage-lapse signal invisible to card-decline monitoring, because a
premium that was never attempted produces no declined transaction.

## Shipped

**Nothing was committed this session.** The tree is dirty — one tracked file modified, three files
untracked, plus this close-out. `git log` still ends at `be9bb2e` ("Summit deep links: add
on-demand processing and three receipt management page keys"), which predates this session.

## In flight

Four code paths plus this close-out, all awaiting Kevin's review and commit. **None of it is
runtime-verified against the live Summit tenant** — the check has been exercised only against
synthetic Participant Plan History CSVs built on the real header row (reflective test, run 5:
filename-pattern sort, header without `PTName`, blank-`EventDate` disbursements, the four
total-failure conditions, the happy path — all passing). `./mvnw compile` is clean.

| Path | State | What it is |
|---|---|---|
| `src/main/java/net/superiorstate/ams/data/service/audit/FundedPurseNoDisbursementCheck.java` | untracked, new | The check. Key `funded_purse_no_disbursement`, label "Funded purses with no disbursement", detail path `/AuditFundedPurse`. Reads the newest Participant Plan History export from the `ExportFiles` sibling of `SUMMIT_SFTP_IMPORT_DIR` over `SummitSftpService`, parses by header name with opencsv (eight required headers — see "Confirmed against real data"), scopes on `Employer_ID` + `PlanTypeID`, month from `SystemDate` only, groups by `(Participant_ID, ParticipantPlan_ID)`, sums `abs(Transactionamt)` per configured type list, findings = contribution > 0 and disbursement == 0. Four total-failure conditions → `ERROR`. `readLive()` for the detail page. |
| `src/main/java/net/superiorstate/ams/controller/admin/AuditFundedPurse.java` | untracked, new | Detail servlet at `/AuditFundedPurse`. Gate: PSP admin + session PSP equals `auditService.getPspId()` (the hub's gate). Calls `check.readLive()`, forwards; persists nothing. |
| `src/main/webapp/WEB-INF/view/a/admin/auditFundedPurse25.jsp` | untracked, new | Detail page. Header block (evaluation month, source filename, timestamp/age, finding count, unmapped-type count); **six** columns: `Participant_ID`, `ParticipantPlan_ID`, `PlanName`, contribution total, disbursement total, month. No names, no SSN. |
| `src/main/java/net/superiorstate/ams/data/service/audit/AuditService.java` | modified, +3/−1 | Registration only: `CHECKS = List.of(new IchraUncodedParticipantsCheck(), new FundedPurseNoDisbursementCheck())`. |
| `docs/session_closeout_2026-09-14_audit_funded_purse_check.md` | untracked, new | This file. |

No migration, no entity, no DAO, no change to `navbar25.jsp` — the badge already sums `ACTION`
counts across the registry, and the hub lists every registered check with a Details link.

## Decisions made — and what each closed

- **Audit check #2, not a new surface.** The `AuditCheck` contract (`key`/`label`/`detailPath`/
  `evaluate`) fits exactly; registration is one element in `AuditService.CHECKS`; the hub, the
  badge, and the `audit_run` table need nothing. Closed: where this lives and how it is reached.
- **One-file design off Participant Plan History.** No Balance export, no stored prior value, no
  second file to correlate. The month's transaction rows alone answer "money in, nothing out".
  Closed: the data source; also closed any need for state between runs (findings stay
  self-clearing, counts-only in `audit_run`, LA-40).
- **Direction comes only from the configured `TransactionType` lists, applied to
  `abs(Transactionamt)`.** The export's sign convention is irrelevant and is never inferred.
  Closed: the sign question, before it was ever asked of a real file.
- **Scoping keys on `PlanTypeID` and `Employer_ID`, not `PlanName`/employer name.** Ids survive a
  rename in Summit; names do not. Closed: which columns the two scope keys match.
- **`PTName` is not read at all.** The original spec described `PTName` as the plan type name and
  made it a required header and a detail-page column. **That was wrong — `PTName` is the
  participant's name** ("lastname, firstname"); `PlanName` is the plan label. Run 5 removed it from
  the required headers, the parsed row record, and the detail page (now six columns). The export's
  field set cannot be filtered in Summit, so the file landing in `ExportFiles` always carries
  participant names in that column even though the check ignores it — an LA-40 handling
  consideration for the export on the FTP landing directory, not for the check. Recorded in the
  class Javadoc so nobody re-adds the column thinking it is a plan attribute. Closed: the column
  set, and where the name-handling obligation actually sits.
- **Month basis is `SystemDate`, always — not configurable.** Run 1 made `EventDate` a
  configurable alternative (`SUMMIT_AUDIT_FUNDED_MONTH_BASIS`); run 5 removed the key and its
  handling after the real export showed `EventDate` blank on **every** disbursement row:

  | `TransactionType` | rows | blank `EventDate` | blank `SystemDate` |
  |---|---|---|---|
  | `Debit Card` | 87 | **87** | 0 |
  | `Claim -Participant Portal/Mobile ` | 22 | **22** | 0 |
  | `Participant Scheduled Contribution` | 200 | 0 | 0 |
  | `Participant Portal/Mobile ClaimsPayment ACH` | 16 | 0 | 0 |
  | `Participant Portal/Mobile ClaimsPayment Check` | 5 | 0 | 0 |
  | `Election` | 22 | 0 | 0 |
  | `Participant Single Fund Amount or Annual Election Amount` | 12 | 0 | 0 |
  | `Credit` | 1 | 0 | 0 |

  With the basis set to `EventDate`, every card and claim row fails date parsing while every
  contribution parses — contribution totals with zero disbursements, turning **every participant
  into a false finding**, the worst possible output for this check. The table is in the class
  Javadoc. Closed: the basis question, permanently; seven config keys, not eight.
- **The framework-level (hub) gate on the detail page — not `IchraUncodedParticipantsCheck`'s
  extra `IchraAccessResolver` gate.** That gate is documented in `AuditIchraUncoded` as specific to
  the first check's ICHRA-only content. This check covers health FSA as well as PremiumPath; the
  ICHRA gate would hide every FSA finding. Closed: detail-page authorization.
- **Total failure is `ERROR`, never `OK`.** Four conditions, checked in order, each absolute, no
  tolerance knob, no new config key: (1) zero rows survive the employer + plan-type scope; (2)
  scoped rows exist but none parses a `SystemDate`; (3) date-parsed rows exist but none matches
  either type list — judged over the **whole file**, not just the evaluation month; (4) typed rows
  exist but zero fall in the evaluation month. Each message names the observed counts and the
  likely cause (wrong prefix / wrong ids / unrecognized date format / type lists not matching the
  file's vocabulary / export date window or `SUMMIT_AUDIT_FUNDED_MONTH_OFFSET` misaligned).
  Partial failures stay in the summary line and never change the status. Closed: a result that
  reads "all clear" after evaluating nothing — a false negative that would be trusted — can no
  longer occur. Condition 4's rationale is in the `totalFailureOf` Javadoc: a month-to-date export
  against a prior-closed-month evaluation would otherwise report `OK` every run, permanently.
- **`NOT_CONFIGURED` for any of the five required keys absent or blank**, and for a value present
  in both type lists. No fallback defaults for the employer, plan-type, or type lists. (The
  month-basis validation that existed in runs 1–4 went with the key in run 5.) Closed:
  config-error behaviour, matching the first check.
- **Cell normalization is trim-at-parse plus case-fold on both sides.** `cellOf` trims every cell
  (so `Employer_ID`, `PlanTypeID`, `TransactionType` all reach the filter trimmed); `csvSet` trims
  the configured lists and lower-cases the type lists; the cell's type is lower-cased at the
  comparison. Confirmed correct in run 2 against the real trailing-space value
  `Claim -Participant Portal/Mobile `. Closed: whitespace and case in the export vocabulary.
- **Filename pattern relaxed for this check only.** The real filename
  `Participant_Account_History_with_Division_Option_Export_20260914145448667_CSV.csv` carries a
  `_CSV` token between the 17-digit timestamp and the extension, so the pattern copied from the
  first check (`^.+_(\d{17})\.[A-Za-z0-9]+$`) matched nothing — the check would have reported
  `ERROR` on every run regardless of configuration. Run 5 replaced it with
  `^.+_(\d{17})(?:_[A-Za-z0-9]+)*\.[A-Za-z0-9]+$` (zero or more underscore-delimited tokens
  allowed after the timestamp; the captured group is still the newest-file sort key). The constant
  is private to this check; `IchraUncodedParticipantsCheck`'s pattern is unchanged, and nothing is
  shared between them. Closed: file discovery for this export.
- **Expected-amount matching and partial-payment detection are deliberately out of scope for
  v1.** Both need a premium-expectation source AMS does not have, for a weaker signal than
  "nothing paid". Closed: v1 scope.

Synthetic coverage as of run 5, all on the real header row: the real filename shape matching and
out-sorting an older one, and the first check's tokenless shape still matching; a header without
`PTName` passing validation, with the parsed row carrying exactly eight fields; disbursement rows
with blank `EventDate` and populated `SystemDate` counting correctly (a card-paid purse is not a
finding; a purse whose only outflow is the claim record is); scope matching nothing → `ERROR`;
all-unparsable `SystemDate` → `ERROR`; types in neither list → `ERROR`; typed rows only outside the
evaluation month → `ERROR`; the run-1 happy path (negative-signed contribution, parenthesised
disbursement, out-of-scope employer/plan-type/month rows, two unmapped types, one unparsable
amount) still producing exactly one finding. Earlier runs additionally covered trailing-space
`TransactionType`/`Employer_ID`/`PlanTypeID` cells matching once trimmed, and a partial failure
leaving the status unaffected.

## Confirmed against real data (2026-09-14)

Settled by profiling one real Participant Plan History export —
`Participant_Account_History_with_Division_Option_Export_20260914145448667_CSV.csv`, 365 rows, one
employer, three plan types. These were assumptions in runs 1–4; they are facts now, so the next
session does not re-derive them. Each is also recorded in the check's class Javadoc under
"Confirmed by the same export, do not change".

- **Header row** (the contract; `PTName` present in the unfiltered export, removed from the
  profiled sample deliberately):
  `Participant_ID,ParticipantPlan_ID,Employer_ID,Organization_ID,EmployerOrganizationID,DivisionName,EmployerPlan_ID,PlanName,PlanTypeID,StartDate,EndDate,PlanYear,ID,TransactionType,EventDate,SystemDate,ClaimKeyCheckNumber,Transactionamt,User_ID,EmployerPlanDetailForPlanYear_ID,EmploymentStatusID,UserStatusID,ByDivision`.
  The check requires exactly eight of these: `Participant_ID`, `ParticipantPlan_ID`,
  `Employer_ID`, `PlanName`, `PlanTypeID`, `TransactionType`, `SystemDate`, `Transactionamt`.
- **Filename shape** is `{template}_Export_{17-digit yyyyMMddHHmmssSSS}_CSV.csv` — a `_CSV` token
  between the timestamp and the extension. The original pattern (copied from the first check)
  matched nothing; the relaxed pattern `^.+_(\d{17})(?:_[A-Za-z0-9]+)*\.[A-Za-z0-9]+$` is private
  to this check. `IchraUncodedParticipantsCheck`'s pattern is unchanged.
- **Date format** is `M/d/yyyy h:mm:ss AM` (e.g. `4/1/2026 12:00:00 AM`). The leading-token
  `M/d/yyyy` parse handles it. **Zero** unparsable `SystemDate` values, **zero** unparsable
  amounts in the sample.
- **All amounts are positive, every type** — no signs, no parentheses, no currency symbols.
  `abs()` is harmless and direction correctly comes only from the configured type lists.
- **The double-count hazard is confirmed, not inferred.** All 21 `ClaimsPayment` rows (16 ACH,
  5 Check) match a `Claim -Participant Portal/Mobile` row on `ClaimKeyCheckNumber` with an
  identical amount (key `97687`, $312.50 on both). **Zero** of the 87 `Debit Card` keys appear as
  a payment row. So counting `{Debit Card, ClaimsPayment ACH, ClaimsPayment Check}` counts each
  disbursement exactly once, and adding `Claim -Participant Portal/Mobile` would double-count
  every portal reimbursement. The exclusion is config-only; the warning is inline in the
  properties block below.
- **`ClaimKeyCheckNumber` on a Check payment** takes the form `120544/ 20005` — claim key, slash,
  check number. Parse the leading token if ever joining on it. Nothing is built on this.
- **Observed reference values — do not hardcode:** `Employer_ID` `1100`; `PlanTypeID` `1` Dep
  Care FSA, `2` Health FSA, `5` LP FSA. Config values for that installation, nothing more.

### Known limitation — cross-month portal claims

A portal claim filed in month N and paid by ACH or check in month N+1 produces a finding for
month N, because the claim record is (correctly) not counted as a disbursement and the payment
row lands in the next month's `SystemDate`. Confirmed in the data: a Check payment carries
`SystemDate` 8/5 against a claim filed in July.

This is FSA-only noise. It cannot occur for PremiumPath, where premiums are card-paid and the
`Debit Card` row itself is the disbursement, dated when it happens. **Deliberately not fixed** —
pairing claim rows to payment rows across month boundaries (on `ClaimKeyCheckNumber`, with the
`/`-suffixed Check form) is real complexity for the benefit type this check matters least for. The
detail page shows the zero disbursement, so a finding of this kind is explainable on inspection.

## New assumptions

Technical, each with reversal cost. Those that the real export settled have moved to "Confirmed
against real data" above.

- **Plan-type list matches `PlanTypeID`, not `PlanName`.** No prior employer-list convention
  existed to copy (see "Contradictions found"); id-keying was chosen for stability under rename.
  Reversal: one line in the filter (`H_PLAN_TYPE_ID` → `H_PLAN_NAME`, with `csvSet(..., true)` for
  case-fold).
- **Date-format list.** `DATE_FORMATS` still accepts `M/d/yyyy`, `yyyy-MM-dd`, `M-d-yyyy` on the
  leading token. The first is confirmed; the other two are untested tolerance and cost nothing.
  Reversal: trim the list.
- **Amount parsing** strips `$` and `,` and treats `(x)` as negative before `abs()`. None of that
  occurred in the sample; harmless tolerance. Reversal: trivial, one method.
- **The relaxed filename regex allows any number of `_token` segments after the timestamp**, not
  just one `_CSV`. A file such as `…_Export_<ts>_CSV_backup.csv` would also match and could win
  the sort if newer. Reversal: `*` → `?` in the group, one character.
- **`YearMonth.now()` is server-local.** A UTC server evaluates the same month as CST except for a
  few hours on the 1st. Low impact at daily cadence. Reversal: pin a zone in one call.
- **Whole-file basis for condition 3.** Type classification runs before the month filter so "type
  lists match nothing" is judged over every scoped, date-parsed row in the file, distinguishing it
  from "this month has no rows" (condition 4). Side effect: the unmapped-type count in the summary
  covers the whole file, not just the month. Reversal: move the month test above type
  classification — two lines.
- **The five required keys are per-PSP by construction**, because one installation is one PSP
  (`AuditService` is built with a single `pspId` in `EmfListener`). No per-PSP keying inside
  `ssa.properties`. Reversal: none needed unless the framework goes multi-PSP.

## Observed `TransactionType` vocabulary

From a live FSA-group Participant Plan History export, so the next session does not rediscover it:

| Value | Classification |
|---|---|
| `Participant Scheduled Contribution` | contribution |
| `Debit Card` | disbursement |
| `Participant Portal/Mobile ClaimsPayment ACH` | disbursement |
| `Participant Portal/Mobile ClaimsPayment Check` | disbursement |
| `Election` | excluded — the election record's annual amount, not money moved |
| `Claim -Participant Portal/Mobile ` | excluded — claim record, **confirmed** to pair 1:1 with a `ClaimsPayment` row on `ClaimKeyCheckNumber`; see "Confirmed against real data" |
| `Participant Single Fund Amount or Annual Election Amount` | unresolved — one label covering two different things |
| `Credit` | unresolved — single occurrence, own ID sequence |

Notes:

- Values carry trailing whitespace (`Claim -Participant Portal/Mobile ` verified). `cellOf` trims
  every cell at parse time and the comparison is case-insensitive, so config values need neither
  the trailing space nor exact case.
- **Every value above names the participant as the funding source.** ICHRA allowance is
  employer-funded, so PremiumPath groups will carry contribution types absent from this list. They
  surface as *unmapped* (counted in the summary; condition 3 fires if nothing maps at all), never
  as mis-counted — but until a PremiumPath export has been read, the contribution list is
  incomplete for the check's primary purpose. The balance snapshot export (below) carries an
  explicit `EmployerYTDContribution` column and is now the intended source for the PremiumPath
  signal — see "Next".
- The two unresolved values are excluded from both lists by default. Excluding a contribution type
  can only produce a missed finding, never a false one; excluding a disbursement type can only
  produce a false finding, which the detail page exposes.

## Second export discovered — participant balance snapshot

Found while profiling; **this changes the plan for check #3** (see "Next"). Nothing reads it yet.

- **Filename shape:** `JJ_TEST_PBA_Export_20260914153619517_CSV.csv` — the same
  `{template}_Export_{17-digit timestamp}_CSV.csv` shape as Plan History, so the relaxed pattern
  would match it. 6,740 rows in the sample.
- **What it is:** a point-in-time snapshot, one row per participant per plan per plan year,
  schedulable daily.
- **Header — 27 columns, entirely different from Participant Plan History:**

  ```
  Participant_ID,EmployerOrganizationID,Organization_ID,DivisionOrganizationID,Employer_ID,DivisionName,EmployerPlan_ID,PlanName,PlanDescription,PlanType_ID,PlanType,ElectionAmount,ParticipantYTDContribution,EmployerYTDContribution,YTDClaim,YTDPayments,AccountBalance,DisbursableBalance,StartDate,EndDate,PlanYear,IsGracePeriodEnabled,IsCarryOverEnable,CarryoverAmount,PendindCardTransaction,AvailableBalance,CoverageEndDate
  ```

Properties that matter:

- **No participant name column at all.** Inherently name-free, which makes it materially safer to
  land on FTP than Participant Plan History (whose `PTName` cannot be filtered out Summit-side).
- **`EmployerYTDContribution` is separate from `ParticipantYTDContribution`.** This is the column
  that matters for ICHRA — allowance is employer money, and every `TransactionType` in Plan
  History named the participant as the funding source. The snapshot answers the PremiumPath
  question directly, without a contribution-type vocabulary.
- **`PendindCardTransaction`** — note Summit's misspelling; **code must match the header
  exactly.** A nonzero value is unsubstantiated card activity against the account, which is the
  condition that leads to card deactivation and, on a premium-bearing card, a blocked premium
  draft.
- **`PlanType_ID` (underscore) and a `PlanType` code column** (`FSA`, `LFSA`) — both differ from
  Plan History's `PlanTypeID`, which has no plan-type name beside it. A check reading both exports
  cannot share a header constant for plan type.
- **YTD figures, not monthly.** A monthly amount would require differencing two snapshots and
  therefore persisting prior values — exactly the stored-state design check #2 deliberately
  avoids. Any check on this export must be a single-row predicate on the YTD columns.
- **Rows span multiple plan years per participant** (2022 and 2023 both present in the sample),
  so any check reading it must filter on `PlanYear` or the `StartDate`/`EndDate` bounds, or it
  will evaluate closed plan years as if current.

## Open questions raised — and who or what settles them

- **Neither export is yet confirmed schedulable to the FTP `ExportFiles` destination.** Both
  filenames carry the scheduled-export shape, and the first check already reads scheduled exports
  from that directory, so it is likely fine — but it is unverified, and if wrong it blocks both
  check #2 and the planned check #3. Settled by Kevin scheduling each export to FTP once and
  seeing it land.
- **`ClaimKeyCheckNumber` as a transaction-level join to the card-transaction export** —
  unverified. Nothing in the codebase handles either export. `docs/analysis/summit_plus_tier_discovery.md`
  S-1 records the card export's primary report with no claim-key column at all and the secondary
  with `DebitCardTransaction_ID`/`GroupRefKey` but no participant key; the only documented viable
  join is participant-level via `UserID` → `User_ID` → `Participant_ID` (the Plan History header
  also carries `User_ID`). Settled by inspecting a real card-transaction export alongside a Plan
  History export. Nothing was built on it.
- **Whether card declines appear at all in a Summit Transaction export, and whether "Include
  records with Zero Paid Amount" is required for them to.** Settled by Kevin running that export
  against a date range containing a known decline. This is the prerequisite for the companion
  decline-monitor check, which is **not built**.
- **Whether Summit's Card Deactivation suspends the whole card or only the account that failed
  substantiation.** Settled by DataPath support. Bears on premium-lapse risk: if card-level, an
  unsubstantiated FSA swipe can block a premium draft — which this check would then catch a month
  late, as a purse with contributions and no disbursement.
- **Two employer-scoping conventions now coexist in the framework.** `IchraUncodedParticipantsCheck`
  scopes Summit-side in the export template (TA-56); this check scopes in AMS config
  (`SUMMIT_AUDIT_FUNDED_EMPLOYER_IDS`). One convention should be chosen before check #3. Settled by
  Kevin; the design doc's Phase A question "How is the employer list for a check configured, per
  PSP?" is still open in `docs/analysis/audit_framework.md`.

## Contradictions found

- **The audit design doc says the first check "filters to employers on a configured list."** It
  does not — `IchraUncodedParticipantsCheck.findingsOf` filters on `UserStatus` = active and blank
  `ParticipantCustomID` only; employer scope is Summit-side in the export template (TA-56). There
  was therefore no existing employer-list config style to mirror, and this check's `Employer_ID`
  list is the first of its kind in the framework.
- **The `AuditCheck` contract has four methods; the design doc describes three.** `detailPath()`
  is omitted from the doc.
- **`ssa.properties` is not a tracked repository file.** It lives at
  `{catalina.base}/conf/ssa.properties` per server (`AppConfig.resolvePath`). The first check's
  keys are documented in `docs/business/summit_data_exchange.md` (config-registry table) and
  `docs/deployment_backlog.md` D-98, not in a properties file. The run-1 prompt's "may modify
  `ssa.properties`" therefore had no target; the keys are documented in the check's class Javadoc
  and in "Deployment prerequisites" below.
- **Run 2's prompt stated it was "not established" that cell values are trimmed.** They were —
  `cellOf` trims every cell at parse time in the file as written in run 1. No change was made for
  that defect.
- **The run-1 spec described `PTName` as the plan type name.** It is the participant's name.
  Runs 1–4 built and documented it as a plan attribute (required header, detail column); run 5
  removed it. The spec's header row was also given in a different column order from the real
  file — harmless, since parsing is by name, and a useful accidental proof of that.

## Deployment prerequisites

Not work items — things that must exist before the check can be demonstrated:

1. **The seven config keys populated in `ssa.properties` on the target server**, then a Tomcat
   restart (`AppConfig` reads once at startup). Paste-ready block, current as of run 5
   (`SUMMIT_AUDIT_FUNDED_MONTH_BASIS` no longer exists); starting values for the two type lists
   are drawn from the vocabulary table above and are **FSA-only until a PremiumPath export has been
   read** (see the vocabulary note):

   ```properties
   # --- T237 check #2: funded purses with no disbursement (FundedPurseNoDisbursementCheck) ---
   # Summit template name (filename prefix) of the Participant Plan History export. Required.
   SUMMIT_AUDIT_PLAN_HISTORY_EXPORT_PREFIX=
   # Comma-separated Employer_ID values to evaluate. Required.
   SUMMIT_AUDIT_FUNDED_EMPLOYER_IDS=
   # Comma-separated PlanTypeID values (numeric id, not PlanName). Required.
   SUMMIT_AUDIT_FUNDED_PLAN_TYPE_IDS=
   # Comma-separated TransactionType values meaning money in / money out. Both required.
   # Trimmed and case-insensitive; a value in both lists is NOT_CONFIGURED.
   # Do NOT add "Claim -Participant Portal/Mobile" to the disbursement list - it is the claim
   # record and would double-count every portal reimbursement (confirmed on ClaimKeyCheckNumber).
   SUMMIT_AUDIT_FUNDED_CONTRIBUTION_TYPES=Participant Scheduled Contribution
   SUMMIT_AUDIT_FUNDED_DISBURSEMENT_TYPES=Debit Card,Participant Portal/Mobile ClaimsPayment ACH,Participant Portal/Mobile ClaimsPayment Check
   # Month basis is SystemDate, always (EventDate is blank on every card/claim row) - not configurable.
   # Closed months back to evaluate. Default 1 (most recent fully-closed month).
   #SUMMIT_AUDIT_FUNDED_MONTH_OFFSET=1
   # Newest matching export older than this is ERROR. Default 36.
   # Separate from SUMMIT_AUDIT_EXPORT_MAX_AGE_HOURS (the participant-list check's key).
   #SUMMIT_AUDIT_PLAN_HISTORY_MAX_AGE_HOURS=36
   # Byte cap is the existing SUMMIT_AUDIT_EXPORT_MAX_BYTES (default 16777216) - reused, no new key.
   ```

   Existing keys reused, not duplicated: `SUMMIT_SFTP_IMPORT_DIR` (its `ExportFiles` sibling is the
   landing directory), `SUMMIT_AUDIT_EXPORT_MAX_BYTES`. Scheduler switch is the existing DB
   constant `AUDIT_SCHEDULER_ENABLED` (D-98); *Run now* on the hub works without it.

2. **A Participant Plan History scheduled export in Summit**, destination FTP, with a template name
   matching `SUMMIT_AUDIT_PLAN_HISTORY_EXPORT_PREFIX` and **a date window covering the evaluation
   month** — a month-to-date window with the default offset of 1 triggers the fourth `ERROR`
   condition on every run. A window of "prior month through today" satisfies both the default
   offset and any later switch to offset 0.

3. **Docs — pending, not authorised this session:** a row per key in the
   `docs/business/summit_data_exchange.md` config registry ("Runtime results — 2026-09-11 (T237
   phase 1)" table), a `D-NN` item in `docs/deployment_backlog.md` alongside D-98, and a T-number in
   `docs/analysis/project_backlog.md` (standing rule S16-G: filing a T-number means writing the row
   in the same run — so both should land in the run that files it). None of these files was
   touched this session.

## Next

1. **Populate the seven keys and run check #2 against a real export.** Restart Tomcat, *Run now*
   on the hub, confirm the summary line's row counts against the file by eye. Five assumptions
   (date-format tolerance, amount tolerance, regex breadth, server-local month, whole-file basis
   for condition 3) collapse to confirmed-or-wrong on one run. **Nothing else should be built on
   this pattern until it has read real data once.** Commit the four in-flight paths together with
   the doc rows and the T-number once that has been seen to work.

2. **Then check #3 off the balance snapshot, as the PremiumPath-facing one.** Per participant-plan
   row: flag where `EmployerYTDContribution` is nonzero and `YTDPayments` is zero or a small
   fraction of it; separately surface `PendindCardTransaction > 0`. Both are single-row
   predicates — no stored state, no `TransactionType` vocabulary, no month logic, no double-count
   hazard. A substantially shorter build than check #2, on the same framework contract.
   - **Design caveat to carry into that build:** ICHRA allowance legitimately exceeds premium in
     some designs, so a contribution-vs-payments gap alone is not proof of a missed payment. A gap
     approaching the full contribution is.
   - Must filter on `PlanYear` / `StartDate`–`EndDate` (multiple plan years per participant in
     the sample) and match the header `PendindCardTransaction` exactly, misspelling included.

3. **Check #2 is not reworked.** Plan History stays the right source for monthly FSA cycle
   detection; the snapshot is the right source for the ICHRA premium signal. Two checks, two
   sources.

Do **not** start the decline-monitor companion check until the "do declines appear in the
Transaction export" question is settled — it is the prerequisite, and the answer decides whether
that check reads a Summit export at all.

## SQL close-out audit

- **No SQL statement was produced, run, or recommended this session.** Not in any run, not in this
  close-out.
- No migration was written; nothing to mark versioned or unversioned.
- **No orphaned `.sql` files.** `docs/migrations/` contains `V025`–`V113` plus the pre-existing
  `seed_ndt125_questionnaire.sql`, unchanged.
- **Current highest migration version: V113** (`V113__enrollment_matrix_participant_agent_note.sql`).
- **Pending deployment (unchanged by this session):** V105–V113 on Production per
  `docs/analysis/migration_tracker.md`; V112/V113 not yet applied anywhere. The four in-flight
  code paths need no migration — `audit_run` (V099) holds the new check via a new `check_key`
  value (`funded_purse_no_disbursement`, 28 chars, column VARCHAR(50)).
- **No schema described but not scripted.** The check persists nothing beyond the existing
  counts-only `audit_run` row that `AuditService` writes for every registered check.
