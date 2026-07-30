# Summit Discovery — ICHRA+ / QSEHRA+ Tier

**Created:** 2026-07-29
**Owner:** Kevin (manual — Summit UI, config, sample exports)
**Design doc:** `../business/plus_tier.md`

All exports below are schedulable, filterable by date range, filterable by selected employers,
and available as CSV or JSON.

---

## Resolved

### S-1 — Card transaction export ✅

Two reports exist. The primary verification report carries `MCC`, `MerchantName`, `UserID`,
`TransactionAmount`, `Date`, `CardHolderName`, `CardNumber` (last four), `TypeName`,
`PurchaseCount`, `ReturnCount`, `PurchaseAmount`, `ReturnAmount`, `EmployerName`,
`EmployerOrganizationID`. Transaction-level grain, confirmed from a JSON sample
(`PurchaseCount` = 1, `PurchaseAmount` = `TransactionAmount`).

A secondary report carries `DebitCardTransaction_ID`, `ActionCode`, `Iso8583MessageTypeID`,
`PostDate`, `SwipeDate`, `LastFour`, `GroupRefKey`, `DpiAccountNumber`,
`ParticipantDependentCardDetailID` — but no merchant detail and no participant key. Use it
for exception investigation only.

Key findings: `UserID` is the participant's ID even on a dependent's card (dependent rollup is
automatic); `MerchantName` truncates at 16 characters; `CardNumber` is last four only (no PCI
scope). Card will be MCC-restricted at issuance, so merchant matching is a convenience rather
than a dependency.

### S-1a — Card-to-participant join ✅
`UserID` → J2 `User_ID` → `Participant_ID`. No mapping export needed.

### S-2 — Mailing export ✅
Scheduled export by event type. Carries `FirstName`, `LastName`, `SSN`, `DOB`, `ERCustomID`,
`EmployerName`, `Organization_ID`, `EmployerOrganizationID`, `EventTypeID`, `EventName`,
`Mailed`. No participant ID — join on the SSN hash. Doubles as a coverage-event feed.

Export columns cannot be modified; a custom export could be commissioned but would cost money.
Declined in favour of the SSN-hash join, which keeps the correlation strategy under SSA's
control.

### S-5 — Correlation keys ✅
`EmployerCustomID` is typed into the Summit setup form as a setup task. Participant
correlation is the SSN hash. No custom export purchase required.

### S-7 — Export scheduling ✅
All relevant exports are schedulable with configurable date ranges and employer selection.

> Findings S-12 onward come from Summit's AI help section, cross-checked against the Settings,
> Employer and Processing guides and against prior operational understanding. **Reliable pending
> tenant verification.**

### S-12 — Data Exchange / scheduled SFTP ✅

Summit's Data Exchange supports scheduled SFTP in **both directions**: AMS pushes import files and
Summit pulls on a daily/weekly/monthly schedule at a configurable time; Summit pushes exports and AMS
pulls. Network options are **DataPath MOVEit** (folder requires a MOVEit administrator to create) or
an **external network** endpoint. Optional **encryption at rest** on top of SFTP transport —
AES / RSA / Triple DES / Rijndael — with TPA-key and DP-key exchange.

**AMS has no FTP/SFTP client or dependency today**, and no inbound ingest that is not a user upload.
This is net-new infrastructure.

### S-13 — Participant Custom ID ✅ (with one open question)

The census import accepts a **Participant Custom ID**, settable at import and **editable later** —
a partner-supplied participant key, one grain below `EmployerCustomID`. If AMS mints and sets it,
correlation becomes direct and the SSN hash may be unnecessary. **See S-16.**

### S-14 — J7 benefit export ✅

Verbatim header:

```
TPA, EmployerOrganizationID, OrganizationID, EmployerID, Employer, BenefitName, PBBenefitID, Type,
RemitTo, PlanTypeID, PBType, PBTypeID, BenefitID, ImportPlanID, EffectiveDate, Carrier,
CarrierGroupNumber, LastDayofCoverage, Fee, PlanYearID, StartDate, EndDate, TierName, TierID,
TierAge, Gender, Smoker, Amount, DivisionName, DivisionID, DivisionCustomID,
OpenEnrollmentStartDate, OpenEnrollmentEndDate, TerminationDate, StandardAdministrationFeeTypeID,
StandardAdministrationFee
```

Notes:

- **Rate-grain, not plan-grain** — one benefit yields multiple rows. A parser needs a benefit table
  and a tier/rate table, not one flat import.
- `ImportPlanID` is present and keyed to employer, so AMS can **read** plan IDs rather than assert
  them.
- `Gender` and `Smoker` are Summit's generic Premium Billing rate columns, built for COBRA.
  **Unused for "+"** — these are employer-determined reimbursement amounts per tier, not market
  premiums. Neither may lawfully drive an ICHRA contribution. `TierAge` may see use, since age-banded
  ICHRA contributions are permitted within limits.
- `DivisionName` / `DivisionID` / `DivisionCustomID` appear at **rate level**, suggesting
  division-scoped contributions — which is what ICHRA classes are. See S-18.
- **No PCOR Reportable field**, so that compliance setting cannot be verified from this export.
- `Carrier` / `CarrierGroupNumber` are present — if letters merge from benefit data, a carrier name
  could surface on a notice. See the endorsement boundary in `domain_and_compliance_rules.md`.

### S-15 — Benefit creation is manual; imports that do exist ✅

**No import template exists for creating Premium Billing benefit plans.** Setup is manual in Employer
Central: Plan Type, Plan Name, Description, Line(s) of Service, **Import Plan ID** (user-defined,
unique within an employer), Effective Date, plan-year details, carrier use types, PCOR Reportable.
Every "+" group therefore needs at least two benefits hand-created — the real ICHRA (CDH) and the
notional notice vehicle (Premium Billing). **This is a floor on onboarding labour that no AMS work
removes.**

Imports that **do** exist for Premium Billing:

- **Coverage assignment** — Employer ID/Custom ID, Participant ID/Custom ID, **Import Plan ID**,
  tier, effective dates.
- **Rate change** — explicitly **bypasses qualifying-event logic** and updates billing directly.
  This is the mechanism behind the notional-benefit notice: change rates, leave participants active,
  remain non-billable.

**Process Approvals:** import-generated events land there with status Auto-Approved / Awaiting
Approval / Manually Approved / Declined, and **Preview displays the document before mailing** —
which may satisfy the preview-and-confirm safety requirement as configuration rather than AMS code.
Summit also natively detects duplicate import files and holds them for accept/reject, giving
file-level idempotency.

**Notice visibility is configuration, not property.** Mailed Letter, DataPath Fulfillment, Portal
Mobile and Push As Alert are **per-notice checkboxes**. Participants not seeing the notional benefit
depends on **Portal Mobile off** and **Push As Alert off**. Record as required configuration —
someone tidying notice settings could switch them on.

---

## Open

| # | Item | Priority | Note |
|---|---|---|---|
| S-3 | `DivisionName` as ICHRA class carrier — assignable per participant, and can contribution vary by division? | Med | Would avoid an AMS class model |
| S-4 | Dependent DOB availability; what `No: of participants` counts | Med | Determines whether the employee must supply dependent DOBs |
| S-6 | Employer funding mechanics — ACH pull vs. prefund, and whether carded participants differ | Med | Shapes invoicing |
| S-8 | Tobacco field on the participant record | Low | Thirty-second check |
| S-9 | Summit native invoicing — arbitrary PEPM per employer, enrolled vs. eligible headcount basis, invoice branding, exportable for revenue share | Low | Optional convenience only; billing goes through Wave regardless |
| S-10 | `EventTypeID` value set | **High** | Blocks the notice-obligation mapping |
| S-11 | Does Summit accept a participant *update* import, not just adds? | Med | Required for the two-pass export |
| S-16 | Does **Participant Custom ID** round-trip in the mailing export and in J2/J3? | High | If yes, it retires the SSN hash entirely and removes the only reason to touch `SummitImportService` for correlation. Observable from existing export files |
| S-17 | Does **J7** cover CDH benefits, or Premium Billing only? Which of its four employer identifiers correlates to what AMS holds? | High | If PB-only, AMS sees the notice vehicle but not the real ICHRA, and a setup-completeness check only half works |
| S-18 | Are **division-scoped rates** true ICHRA class support? | Med | Would answer S-3 affirmatively and move `DivisionName` from low-priority to load-bearing |
| S-19 | Do **Retiree or Direct Bill** sub-LOS plans generate status-change notices with configurable content? | Med | Every awkwardness in the notional-COBRA mechanism stems from the vehicle being COBRA *specifically*. Direct Billing is conceptually closer to paying premiums for individual coverage |
| S-20 | Do **imported** status changes trigger letter generation the same way manual ones do? | High | Everything downstream rests on this. See `summit_notice_automation_discovery.md`, Phase 3 |

---

## Notes

No SQL is produced by this discovery process. Any Summit finding implying an AMS schema change
goes through a versioned migration under the normal rules.
