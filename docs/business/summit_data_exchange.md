# Summit Data Exchange

**Created:** 2026-09-07 · **Status:** **Active** · **Owner:** Kevin

> **Every field requirement in this document was established by importing a file and reading the
> results file.** Vendor AI ("Atlas") answered several of these questions incorrectly, or as "not
> documented." **Where this document and vendor documentation disagree, this document is correct.**

## What this is

A live test sequence against DataPath Summit's Data Exchange proved the complete file-based path from
AMS to a funded ICHRA benefit: employer, plan, participant, enrollment. This document records what
that sequence found — transport shape, template mechanics, the exact file layouts that worked, and the
identifier-ownership rules that determine whether AMS can regenerate and resend safely. **None of this
existed anywhere in the repo before this document.**

## Transport

- Host `ftp1.dpath.com`, port 443. Protocol (FTPS vs SFTP) is **not established** — it matters for
  client library selection and is unresolved (see SDX-01).
- Three separate folder configurations, each with its own credentials: **Imports**, **Results**,
  **Exports**. All are currently Folder Location = `Datapath Network`, togglable to
  `External Network`.
- **No credentials in this repo, ever.**
- Current posture: Encrypted Files off, Header Password Required off, Restrict IP Addresses off.
- Direction is undecided. `Datapath Network` means AMS pushes out, with no inbound exposure on the
  VPS. `External Network` means Summit pulls from a server SSA runs, which means an inbound file
  service to secure and patch. This is reversible by dropdown; **file generation and file delivery
  are separable, and phase one is generating a correct file for manual upload.**

## How templates work

- A template is a **user-defined mapping**, not a fixed vendor layout. File Type, File Format,
  Delimiter, Date Format, Extraneous Data are all per template. **We choose the element set and
  order** — the file format is a design output, not a constraint.
- **Body Format** is the actual mapped columns in the file.
- **Unmapped Fields** are elements Summit needs that are **not** in the file; a Default Value is set
  in the template and applied to **every record**. Such elements **must not appear in the file**.
  This means AMS emits only what varies per row.
- **Header Format** is file self-identification: `DP System Entity ID`, `Template Name`, `Date`,
  `Password`. ⚠️ **Never map the `Password` element in any template AMS generates** — it would write
  a credential into a file on disk on every run. Keep Header Password Required off.
- **Footer Format** is optional, carrying Total Record Count and Process on Error Count. Useful as a
  validation control once emission is automated.
- **Include Body Record Indicator** puts the configured Row Indicator value in **column A**, shifting
  mapped data to start at B. All templates in this spec leave it **off**, so data starts at A.
- **Produce Results File** writes a results file to the configured Results FTP folder. **Errors Only**
  limits it to exception rows.

## ⚠️ "Optional" does not mean optional

**The Mandatory/Optional column in the element picker describes the template, not Summit's business
rules.** Elements labelled Optional are frequently required, and the import fails without them.
Requirements are discovered by importing, never by reading the picker. Two proven examples: the
employer mailing address block, and plan year information on an annual-renewal plan — both labelled
Optional, both hard requirements.

## The proven chain

Four files in strict order. Each depends on the one before.

> **Employer Demographic → Employer CDH Plan → Demographics → HRA Enrollment**

All four templates share the same settings: Delimited, `|`, `YYYYMMDD`, Extraneous Data No, no header,
no footer, no body record indicator.

### 1. Employer Demographic — creates or updates the employer

Columns: `Employer Name`, `Employer TPA Custom ID`, `Mailing Address`, `Mailing City`,
`Mailing State`, `Mailing Zip`

```
SSA ICHRA Test Employer A|ZZTEST001|100 Main Street|Marinette|WI|54143
```

Everything else in the Employer Demographic element list is genuinely optional. Note that
`Enable COBRA Administration` is the flag that enables **Premium Billing** — the platform ICHRA
mailings ride on. Premium Billing covers COBRA, Retiree Billing and Direct Bill; ICHRA needs the COBRA
leg. AMS will set this true on employers with no COBRA, so it is not later read as a defect.

### 2. Employer CDH Plan — creates the benefit plan for that employer

Columns: `Plan Template ID`, `Plan Name`, `Import Plan ID`, `Plan Description`, `Effective Date`,
`Employer TPA Custom ID`, `Plan Year Begin`, `Plan Year End`

```
1029|ICHRA 2027|ICHRA2027|ICHRA Plan 2027|20270101|ZZTEST001|20270101|20271231
```

Plan year is mandatory because the plan template's funding structure is Annual renewal. Two routes
exist — an existing global `Plan Year ID`, or `Plan Year Begin`/`Plan Year End` to define one inline.
**Inline dates are preferred**: AMS already knows the plan year from the application, and it avoids
carrying a second Summit-assigned reference that changes annually.

### 3. Demographics — creates the participant

Columns: `Employer TPA Custom ID`, `Participant TPA Custom ID`, `First Name`, `Last Name`,
`Mailing Address Line 1`, `Mailing Address City`, `Mailing Address State`, `Mailing Address Zip Code`,
`Effective Date`

```
ZZTEST001|ZZP001|Alice|Testcase|100 Main Street|Marinette|WI|54143|20270101
```

**Neither SSN nor DOB is required.** The setup export carries no SSN, so the project's no-SSN boundary
extends from proposal through enrollment without an exception.

### 4. HRA Enrollment — enrolls the participant and sets the amount

Columns: `Employer TPA Custom ID`, `Participant TPA Custom ID`, `Import Plan ID`, `Effective Date`,
`Participant Annual Election Amount`

```
ZZTEST001|ZZP001|ICHRA2027|20270101|600.00
```

Amounts accept two decimal places. `Participant Annual Election Amount` carries the benefit amount
even though an ICHRA is employer-funded; funding source is set on the plan template, not per record.

## ⚠️ ID ownership and uniqueness

The single most important section. Four identifiers, three owned by AMS.

| Identifier | Assigned by | Uniqueness | Notes |
|---|---|---|---|
| `Employer TPA Custom ID` | **AMS** | Per installation | **Upsert key.** Must be stable for the life of the employer — changing it orphans the old record and creates a new one. Derive from something immutable, never from a name or tax ID. |
| `Participant TPA Custom ID` | **AMS** | ⚠️ **GLOBALLY UNIQUE across all employers** | See the warning below. |
| `Import Plan ID` | **AMS** | Per-employer accepted — **treat as suspect** | Two employers took `ICHRA2027` and enrollment resolved correctly. But this is the same evidence pattern that misled on participants. Namespace by employer unless a test proves otherwise. |
| `Plan Template ID` | **Summit** | — | The **only** Summit-assigned foreign reference the emitter needs. Config, resolved at runtime, **never hardcoded** — it differs per installation, same reasoning as the project's reference-row rule. |

**Write this warning in full, it cost a wrong conclusion:**

> `Participant TPA Custom ID` must be globally unique across every employer. The Demographics import
> **accepts** a duplicate ID under a different employer and reports success. The failure surfaces one
> step later, at HRA Enrollment, as `Employer ID Conflict` — even though `Employer TPA Custom ID` is
> supplied in the enrollment row. Summit cannot disambiguate the participant.
>
> This is the worst shape a constraint can take: the create succeeds and the failure appears in a
> different file, days or months later, looking like anything but a naming scheme. **Derive
> participant IDs from a globally unique key such as the AMS employee record's own primary key —
> never a per-employer sequence.**

## Re-import behaviour

Re-importing with the same `Employer TPA Custom ID` **updates in place**. The results comment changes
from `Employer created successfully` to `Employer edited successfully`. Nothing duplicates. A
byte-identical record is rewritten rather than skipped. **No `Record Process Indicator` value is
needed for create or update.**

Consequence: **AMS emits full current state, not deltas.** No sent-state tracking, no create-vs-update
branch, no reconciliation table. Regenerating and re-sending is safe.

`Record Process Indicator` presumably governs termination or deletion; its valid values are unknown.

## Results files

Format follows the results template. Observed shape includes status, echoed key fields, a comment, and
a **row number** — the row number gives reliable positional correlation.

Key echo **depends on how far validation got**, not on pass or fail: a record rejected before field
binding returns empty key fields, while one rejected after binding echoes them. **AMS should correlate
on row number, not on the echoed key.**

Always map `Record Comment` into the results template. `Record Processing Status` alone yields a bare
"Failed" with no reason.

## Plan types and the ICHRA template

**ICHRA is a native Plan Type in Summit.** Vendor AI said this was not documented and likely used the
generic HRA code; that was wrong.

Native types relevant to the bundle: **ICHRA**, **EBHRA**, **HRA**, **MERP**, **FSA**, **LFSA**,
**DCA**, **HSA**, **Ins125**, **Ins125_w_HSA**, plus TRN, PRK, PRA, DRiP and custom codes. `LFSA`
existing natively matters — the limited-purpose FSA fork required by an HSA pairing needs no
workaround.

Plan template configuration as tested:

- Plan Type `ICHRA`, Line of Service `CDH`
- **Funding structure: Annual renewal** — an ICHRA is a plan-year benefit whose amount resets
  annually
- **Funding source: Employer only**, participant-initiated contributions off. ⚠️ The employee's
  pre-tax salary reduction is a **separate `Ins125` plan**, never employee funding on the ICHRA.
  Merging them would blur the ICHRA and the Section 125 rail together.
- **Funding tax treatment: Pre-tax** — see [LA-27](../analysis/legal_assumptions.md); none of the six
  available options actually describes employer-provided excludable money.
- Test template ID `1029` (`ZZ_TEST_ICHRA`). A pre-existing ICHRA template at ID `1009` was not
  examined.
- `Enable debit card` will be needed for premium payment on the card; it was off in the test template.
- `PCOR Reportable` was off in the test template — see [LA-28](../analysis/legal_assumptions.md).

## Open questions

Numbered `SDX-NN`, a series local to this document — distinct from the project's global `O-NN`
open-question registry (`O1`–`O52+`, tracked in `docs/swbd_ichra_build_plan.md` /
`docs/ichra_strategy.md` / `plus_tier_build_plan.md`). Do not confuse the two.

1. **SDX-01** — FTPS or SFTP on port 443 — determines the client library when transport is
   automated.
2. **SDX-02** — Do two employers supplying identical plan year dates create one global plan year or
   duplicates?
3. **SDX-03** — Valid `Record Process Indicator` values.
4. **SDX-04** — Termination handling — `System Status`, `Termination Date`, `Coverage End Date`, and
   which file carries it.
5. **SDX-05** — Mid-year election change — new enrollment record, adjustment, or something else.
6. **SDX-06** — Is `Import Plan ID` globally unique in practice?
7. **SDX-07** — Is load order enforced, and what happens when a file references a missing employer or
   plan?
8. **SDX-08** — Does `Funding tax treatment = Pre-tax` drive payroll or W-2 reporting differently from
   employer-provided money?
9. **SDX-09** — Does enabling COBRA Administration for Premium Billing generate COBRA artifacts or
   notices? **Not safely testable — the failure mode is a notice reaching a real person. Route to
   Summit support.**
10. **SDX-10** — Does `PCOR Reportable` drive PCORI reporting data capture?

## Test artifacts

Test records `ZZTEST001`, `ZZTEST002`, participants `ZZP001`–`ZZP003`, plan template `1029` and
templates prefixed `ZZ_TEST_` exist in the live Summit environment and should be cleaned up.
