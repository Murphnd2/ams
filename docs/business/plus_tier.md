# ICHRA+ / QSEHRA+ — Product Definition and Data Design

**Status:** Requirements settled; data model designed; not yet built
**Created:** 2026-07-29
**Owner:** Kevin
**Driver:** SWBD / PremiumPath (Forrest Huggins) — ICHRA administration, zizzl displacement
**Related:** `swbd_premiumpath.md`, `healthsherpa.md`, `../analysis/summit_plus_tier_discovery.md`

---

## What the "+" tier is

Two new lines of service — `ICHRA+` and `QSEHRA+` — that bundle HealthSherpa marketplace
services and automated coverage verification on top of standard administration.

Standard `ICHRA` and `QSEHRA` LOS remain unchanged and available to existing agencies. The
"+" tier is additive: a new pre-sale stage (rating-area illustration, contribution design) and
a new post-sale stage (per-participant coverage verification). The middle — Summit
adjudication, disbursement, notice mailing — is untouched.

---

## Decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | LOS granularity is `ICHRA+` / `QSEHRA+` only. Add-ons are enhancements, not LOS. | Avoids combinatorial LOS rows. |
| D2 | **Single bundled PEPM.** Card included; no per-proposal add-on election. | Verification method varies *per participant* and isn't known until after enrollment — months after the proposal is signed. No rate-table mechanism can express it. Employer sees one number. |
| D3 | No per-employee pricing at any stage. | Pricing is aggregate: zone illustration at quote, PEPM on headcount at billing. |
| D4 | Enhancement visibility is controlled by rate-table pricing. | An enhancement with no priced `RateTable` row does not render. No `optional` flag needed. |
| D5 | Billing via existing headcount export to Wave, same as every other LOS. AMS supplies **both** enrolled and eligible counts. | No change to the billing pipeline. Summit's native invoicing is an optional convenience, not a dependency. |
| D6 | Quote inputs: ZIP + headcount mandatory; **age-band counts optional**. | Headcount alone yields a valid range illustration. Age bands upgrade it to a net-cost table. Progressive disclosure — no friction on the default path. |
| D7 | Full census is collected **post-sale at setup**, into a staging table. | Quoting needs no PII. |
| D8 | Census mandatory minimum: SSN, first name, last name. | SSN is the participant correlation key (see D10) and is required for Summit and for 1095 reporting. |
| D9 | Participant verification state lives in its own table, outside the billing pipeline. | The billing pipeline's only correction mechanism is whole-month wipe-and-recreate, which is incompatible with a durable ledger. Isolation is cheaper than extension. |
| D10 | Participant correlation via **HMAC-SHA256 of normalized SSN**, secret key in `ssa.properties`. Store hash + last four; never store raw SSN. | The Summit mailing export carries no participant ID — only name, SSN, DOB. Hashing gives a deterministic join with no SSN at rest. A custom Summit export was priced as an alternative and declined. |
| D11 | Raw SSN lifetime: census intake → Summit creation export → discarded. | Summit is the system of record for SSN. AMS needs it only for the initial handoff. |
| D12 | Employer correlation via `EmployerCustomID`, typed into the Summit setup form as a setup task. | No import automation needed for a once-per-employer value. |
| D13 | PremiumPath Card is **MCC-restricted to insurance codes** (6300, 5960) at issuance. | Every approved transaction is then a premium payment by construction. Merchant-name matching becomes a convenience, not a dependency. |
| D14 | A "+" pricing proposal section, scoped to the "+" LOS, with a flag selecting which HTML variant renders. | Range illustration vs. age-band net-cost table are two renderings of one section, chosen by what the quote snapshot contains. |

---

## Requirements by stage

### Quote (pre-application, no PII)

Inputs: worksite ZIP, eligible headcount, plan year / effective date, entity type, group-plan
status, contribution structure and amounts. Optional: age-band counts, tobacco.

Outputs: eligibility result (which "+" LOS the prospect qualifies for), market illustration,
and — only when age bands are supplied — per-band net cost and an affordability count.

The eligibility result filters the LOS menu. Combined with the existing rate-table gate, an
agency can only quote a "+" LOS that is both priced for it and legal for the prospect.

### Setup (post-application)

Full census into staging: SSN, names, DOB, hire date, home address, division. Employer-
supplied.

Employee-supplied only where the employer cannot answer: marketplace consent, PTC waiver
acknowledgment (ICHRA+), tobacco, dependent detail. **See open item O1 — HealthSherpa's own
EDE consent may cover this, in which case no AMS consent artifact is needed.**

Two-pass Summit export: create participants at setup, then update post-enrollment with
carrier, plan, premium, and effective date.

### Ongoing

Monthly coverage verification, notice obligation tracking and reconciliation, premium-change
detection, and an agent/employer-facing verification utility.

---

## Data design

No SQL is produced by this document. Each item below becomes a versioned migration under the
normal rules when the build starts.

### New tables

| Table | Grain | Purpose |
|---|---|---|
| `rating_area_rate_cache` | plan year × county FIPS × age band × tobacco | LCSP self-only, benchmark silver, lowest bronze, carrier count, plan count, `fetched_at`. Populated once per county per plan year from HealthSherpa; reused across all prospects. |
| `plus_quote` | one per proposal | Snapshot of quote inputs, resolved county/rating area, `illustration_mode` (range vs. age-band), and the illustration as shown. Rates move; the illustration must be reproducible months later. |
| `plus_census_stage` | one per census row | Employer-supplied census landing table. Raw SSN transits here and is cleared after the Summit creation export. |
| `plus_participant` | one per eligible employee | `ssn_hash`, `ssn_last_four`, home county FIPS + rating area, DOB, tobacco, dependents, LCSP self-only, affordability result, HealthSherpa identifiers, carrier of record, expected premium. Links to `Employee` (Summit `Participant_ID`). |
| `participant_coverage_month` | participant × month | Verification ledger: verified yes/no, `verification_source`, verified timestamp, premium seen, variance from expected. Operational and audit artifact only — **not** a billing driver (D5, D9). |
| `notice_obligation` | participant × notice type × effective date | `PENDING → EXPORTED → MAILED → RECONCILED`, with the Summit event reference once known. Supports the completeness assertion. |
| `notice_event_map` | Summit `EventTypeID` → notice type | Which coverage events create which notice obligations. **Blocked on open item O2.** |

`verification_source` values: `HS_POLICY_STATUS`, `CARD_TRANSACTION`, `ATTESTATION`,
`DOC_UPLOAD`, `UNVERIFIED`. It carries no billing consequence (D2, D5) — it exists so the
automated-vs-manual mix is reportable and so exceptions are findable.

### Existing-model additions

- County FIPS and rating area: no such concept exists anywhere in the model today.
- Tobacco: no person-level field exists.
- Promotion of already-staged J3 fields (`DOB`, `HireDate`, `EffectiveDate`,
  `TerminationDate`, `DivisionName`) from staging to `Employee` — these are parsed today and
  discarded, so this is promotion code plus entity fields, not new parsing.
- Per-employee questionnaire attachment: add a nullable `employee_id` to
  `questionnaire_instance` alongside the existing not-null `activity_id`, and widen the unique
  index to `(questionnaire_id, activity_id, employee_id)`. The campaign hangs off the
  employer's Setup activity; each instance additionally points at one employee. This
  dissolves the constraint that currently blocks one-template-to-many-recipients.
- `ProposalSection` LOS scoping: does not exist today (`ApplicationSection` has it via
  `applicationsectionlos`; `ProposalSection` does not). Required for D14.

---

## Summit feeds

Both are scheduled, employer-filterable, date-range-filterable, and available as CSV or JSON.

### Card transaction feed — verification

Two reports exist and serve different jobs:

- **Primary (verification):** carries `MCC`, `MerchantName`, `UserID`, `TransactionAmount`,
  `Date`, `CardHolderName`, `CardNumber` (last four only). Transaction-level grain.
- **Secondary (exceptions/forensics):** carries `DebitCardTransaction_ID`, `ActionCode`,
  `Iso8583MessageTypeID`, `PostDate` vs. `SwipeDate`, `LastFour` — but no merchant detail and
  no participant key. Pull on demand when a participant lands in the exception queue.

Notes that matter for ingest:
- `UserID` is the **participant's** ID even on a dependent's card, so dependent transactions
  roll up automatically. No dependent-to-participant mapping is needed.
- Join path is `UserID` → J2 `User_ID` → `Participant_ID`.
- `MerchantName` is truncated to 16 characters (card-network descriptor truncation). Any
  carrier matching must work on truncated prefixes.
- Natural idempotency key: `(UserID, Date, TransactionAmount, MerchantName)` — `Date` carries
  sub-second precision.
- `CardNumber` is last four only. Discard at the parse boundary regardless; it has no role.
- `Date` appears to be a post date from an overnight batch, not a swipe time. Month-attribution
  rule should be set deliberately. See open item O3.

Verification chain: expected premium captured at setup → monthly card activity on an
MCC-restricted card → amount matched within tolerance → participant-month verified. Premium
variance routes to an exception queue and may trigger a rate-change notice obligation rather
than failing verification.

### Mailing / coverage-event feed — notices

Carries `FirstName`, `LastName`, `SSN`, `DOB`, `ERCustomID`, `EmployerName`, `Organization_ID`,
`EmployerOrganizationID`, `EventTypeID`, `EventName`, `Mailed`.

- No participant ID. Join on `ssn_hash` (D10), with DOB plus surname as a secondary check for
  hash misses caused by census typos.
- This is a **coverage-event feed as well as a mailing log** — it reports status changes that
  originated in Summit rather than in AMS. Anything ingesting it will need explicit EclipseLink
  L2 cache eviction after write.
- Summit retains proof of mailing. AMS's contribution is the **completeness assertion**: every
  notice obligation has a corresponding mailed row. AMS does not attempt to replicate proof of
  mailing.
- No per-letter document reference or tracking number, so an obligation ties to "a notice of
  type X was mailed to this person in this period," not to a specific physical letter. That is
  sufficient for completeness.

---

## Open items

| # | Item | Consequence |
|---|---|---|
| O1 | Does HealthSherpa's EDE applicant consent cover Policy Status access by the administering TPA? | If yes, the AMS consent artifact and the per-employee questionnaire requirement both disappear. This is the only open item that could **delete** a table rather than fill one in. |
| O2 | Summit `EventTypeID` value set | Blocks `notice_event_map`, which is the core of the notice engine. |
| O3 | Card feed `Date` — post date or swipe date? | Determines which month a premium payment verifies. |
| O4 | HealthSherpa Policy Status coverage of **off-exchange** enrollments | Off-exchange participants fall back to card verification. Since the card is MCC-restricted (D13), this is survivable — but the automated/manual mix drives ops cost and should be known before pricing. |
| O5 | Summit `DivisionName` as ICHRA class carrier | If it can carry class and vary contribution, no AMS class model is needed. |
| O6 | Summit dependent DOB availability | If absent, dependent DOBs must be collected from the employee. |
| O7 | Employer funding mechanics (ACH pull vs. prefund), and whether carded participants differ | Shapes whether invoicing needs a funding line. |
| O8 | Counsel: QSEHRA SEP window, §213(d) filed-form question, MEC-floor design, PCORI applicability | Unchanged from prior sessions. |
| O9 | Whether a carrier accepts card payment for individual premiums at all, and posts as 6300/5960 | Untested. One real carded premium transaction settles it. Worth engineering before the tier is priced. |

---

## Phase A findings that shaped this design

A read-only repo investigation (2026-07-29) established:

- **No dollar amount exists anywhere in the billing model.** `billing_summary` is a view
  summing boolean coverage flags; `BillingItem` has the right shape but is dead, unpopulated
  scaffolding. The only correction mechanism is whole-month wipe-and-recreate. → D5, D9.
- **Enhancement has no required/optional flag**; every linked enhancement is pulled into every
  proposal automatically. Visibility is controlled by rate-table pricing instead. → D4.
- **`Employee` exists** as a production entity keyed on Summit `Participant_ID`, but is **not**
  in the `Assignee` hierarchy, and `questionnaire_instance.activity_id` is FK'd to
  `assignee(id)`. A missing FK, not a missing entity.
- **New LOS creation and pricing is fully additive** — no existing `RateTable`, `losmodules`,
  or `agencyrates` rows need to change.
- **`ProposalSection.sectionType` is a free `VARCHAR(20)`** with JSP `<c:choose>` dispatch and
  no default branch — an unmatched type renders silently. New section types need no schema
  change, but do need a JSP branch. `ProposalSection` has no LOS scoping.
- **J2/J3 parsing is column-name-driven** and tolerant of unknown and reordered columns, but
  each persisted field still needs explicit extraction code.
- **No county/FIPS/rating-area or tobacco concept exists** anywhere in the model.
- **No HTTP response cache, no retry/backoff convention, no shared third-party HTTP client**,
  and three inconsistent credential-storage patterns. The HealthSherpa client is a real build.
- **No FTP/SFTP client or dependency**, and no inbound ingest that isn't a user upload.
  `InstallationHealthScheduler` is a usable template for a scheduled job.
- **No per-employee scoping in Wasabi keys**, and most upload paths never delete the S3 object
  when the referencing record is removed.

---

## Not doing

- **Carrier list billing** — inserting SSA into the individual-market premium chain risks
  group-health-plan reclassification. Counsel question before it is ever a feature question.
- **A shopping or steering layer.** Neutral market illustration only; no carrier named, no
  plan recommended. Carrier names stay off all SSA-drafted paper.
- **AMS-owned notice mailing.** Summit's proof of mailing is the reason notices stay there.
- **Replicating Summit adjudication, custody, or disbursement.**
