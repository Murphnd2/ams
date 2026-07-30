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

---

## Notes

No SQL is produced by this discovery process. Any Summit finding implying an AMS schema change
goes through a versioned migration under the normal rules.
