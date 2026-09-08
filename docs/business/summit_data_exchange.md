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

## ⚠️ An optional field must never be the last column

**This rule governs files AMS emits to Summit and the custom Summit import templates that consume
them. It has nothing to do with files employers upload into AMS**, where column order is irrelevant
because every column is located by its header (see `CensusParseService`). The two directions are
independent, and conflating them is the misreading this note exists to prevent.

**Established operationally by Kevin** — a known defect in Summit's importer, observed in practice.
Not from vendor documentation, and not verified by any automated test in this repository.

> A column that may be empty cannot be the final mapped column in a template. The importer
> mishandles a trailing empty value. Any nullable element must be ordered **before** a column
> guaranteed to carry data on every row. This governs both the template's Body Format and the emitted
> file, and the two must agree.

The resulting column order for **file 4 (Demographics)** — twelve columns, A–L, as
`SummitExportServlet.writeDemographics` emits them:

```
A Employer TPA Custom ID  | B Participant TPA Custom ID | C First Name
D Last Name               | E Mailing Address Line 1    | F Mailing Address City
G Mailing Address State   | H Mailing Address Zip Code  | I Effective Date
J E-mail Address (opt)    | K Mailing Address Line 2 (opt) | L Branch Code
```

⚠️ **Column order is dictated by the Summit template and mandatory elements cannot be reordered.**
Optional elements are appended after the mandatory block, so `E-mail Address` and
`Mailing Address Line 2` land at J and K regardless of where they belong logically. **A trailing
empty optional field breaks the parse**, so the template ends with a mandatory `Branch Code` that AMS
always populates from `SUMMIT_BRANCH_CODE` (default `AMS`). The value carries no meaning — it exists
to guarantee a non-empty final field.

⚠️ **Read this before adding any column.** Anything appended after `Branch Code` reintroduces the
defect. A new optional field belongs before it, and `Branch Code` stays last.

⭐ **Import-proven 2026-09-08.** A hand-built file in this A–L order with `AMS` in column L was
accepted: five of six rows created, **including all three rows with an empty column K** — the exact
trailing-empty-optional case the sentinel exists to prevent. The sixth failed on a field-length limit
(below), not on order or on the sentinel.

⚠️ **This supersedes the earlier eleven-column layout**, which placed `Mailing Address Line 2` at
position 6 and `Effective Date` last. That order was never accepted by anything; bound positionally
against the template, **City would have landed in a state field**.

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

#### ⚠️ `Import Plan ID` carries NO plan year — corrected S31-J, 2026-09-08

**A Summit benefit plan persists across plan years and accumulates them.** A renewal imports into the
**same** plan and attaches another plan year to it; a plan carries multiple plan years over time, and
elections separate by plan year. That is the fact everything below turns on.

`Import Plan ID` is the **upsert key**. So with a year in it:

- year one emits `158E140952-DCAP-2026` and Summit creates the plan;
- year two emits `158E140952-DCAP-2027`, which Summit reads as a **different** plan and **creates a
  second one** rather than attaching a plan year to the first;
- and so on, **every year, indefinitely** — a growing set of near-duplicate plans with elections split
  across them, indistinguishable in the UI except by a key nobody reads.

**The shipped form is `{employerTpaCustomId}-{keySegment}`.** The plan year travels in
`Plan Year Begin` / `Plan Year End`, which file 2 already emits inline and which is the mechanism
Summit provides for exactly this.

⚠️ **The superseded form was `{employerTpaCustomId}-{keySegment}-{planYear}`.** T185 recorded a
deliberate do-not-touch on it, reasoning that keeping the year was the recoverable error (a spare plan
to delete) and dropping it the destructive one (a renewal overwriting the prior year). **That premise
was wrong**: a renewal does not overwrite, it attaches. T185 is retired.

⚠️ **There are two composition sites, not one** — file 2's `buildCdhPlanRow` and the enrollment
writer's `importPlanId`, which S30-A duplicated rather than extracted. **They must always agree**: if
one carries a year and the other does not, every enrollment row points at a plan id that does not
exist. Both changed in S31-J; anything that touches one must touch the other.

#### `Plan Name` and `Plan Description` are the Label, and nothing else

Both columns now emit the mapping row's **`label`** verbatim. No year — `Plan Year Begin`/`End` already
carry it — and no employer name, which only restates what the row's own `Employer TPA Custom ID` column
says.

⭐ **The label is editable per mapping on the Summit Plan Templates admin screen (T202)**, so **renaming
a plan needs no code change and no config change** — a PSP admin edits the Label field and the next
export carries the new name.

⚠️ **file 2's results template correlates on `Plan Name`.** It carries no row number, so the only way
to tell which result line belongs to which submitted row is the plan name. **Two rows in one file must
therefore not share a label.** Nothing enforces this — the unique constraint on the mapping table is on
(PSP, service item), not on the label — so it is the operator's responsibility when filling in the
Label field.

#### Optional elements — the block that appends after A–H (S31-H, 2026-09-08)

The `Employer CDH Plan` template also offers **optional** elements. Summit's own dialog separates them
from the mandatory block, and AMS appends them **after** column H in whatever order the installation
configures. ⚠️ **The eight mandatory columns are unchanged and remain import-proven** — nothing in this
section alters them.

⚠️ **AMS conforms to the template, never the reverse.** Which optional elements exist, and in what
order, is a property of the Summit-side template; `SUMMIT_CDH_OPTIONAL_ELEMENTS` is how an operator
states what the template was told. **Unset means AMS emits nothing extra**, which is the default and
leaves file 2 byte-identical to what it emitted before S31-H.

⚠️ **`ZZ_TEST_CDH` currently maps NO optional elements.** Until they are added on the Summit side, any
file carrying a non-empty optional block **will not import**. Configuring the AMS side first is
harmless but achieves nothing on its own.

**The three tiers, settled 2026-09-08:**

| Tier | Elements | Source in AMS |
|---|---|---|
| Fixed, every CDH plan | `Run-out Enabled` true · `Run-out Calculation (by date)` false · `Run-out # Days` 90 · `Terminated Run-out Type` 1 · `Terminated Run-out # Days` 90 | Config, each defaulting to the value shown |
| Per sale | `Grace Period Enabled` · `Grace Period Calculation (by date)` **true** · `Grace Period Date` | Application answers, mapped per plan; the date is computed (see below) |
| **Not importable** | FSA **carryover** and its amount; HRA / MERP / DRiP reimbursement logic | Nothing — the plan is skipped and logged |

**Supported tokens:** `GRACE_ENABLED`, `GRACE_BY_DATE`, `GRACE_DATE`, `GRACE_DAYS`, `RUNOUT_ENABLED`,
`RUNOUT_BY_DATE`, `RUNOUT_DAYS`, `TERM_RUNOUT_TYPE`, `TERM_RUNOUT_DAYS`, `OPEN_ENROLL_START`,
`OPEN_ENROLL_END`. Config order is emit order.

⚠️ **An unrecognised token refuses the export rather than being skipped**, breaking deliberately with
the tolerant parsing every sibling resolver uses. Summit binds optional elements **positionally**, so
silently dropping one shifts every element after it and loads each value into the wrong field — which
Summit accepts without complaint. A skipped token there costs a plan row; a skipped token here costs
column alignment.

⚠️ **`OPEN_ENROLL_START` and `OPEN_ENROLL_END` always emit empty.** S31-H searched every application
package: **AMS collects no open-enrollment dates anywhere.** The tokens exist so a template that maps
those columns still binds positionally.

##### Grace, per plan

`SUMMIT_CDH_GRACE_FIELDS` maps a plan mapping's `key_segment` to the application field holding that
plan's end-of-year answer — e.g. `FSA:hfsa_roll_or_grace,DCAP:dcap_grace`.

| Case | Emitted |
|---|---|
| Key segment not listed | All three grace elements **empty**. ICHRA's path, and correct: one layout for every row, blank where the concept does not apply. |
| Answer = `2-1/2 Month Grace Period` | Enabled true, **by-date true**, `GRACE_DATE` computed from the plan year end (see below). `GRACE_DAYS` empty. |
| Answer = `None` | Enabled false, the other two empty |
| Answer = `Carryover` | **Plan omitted from the file** — see below |
| Answer absent or unrecognised | ⚠️ **The export refuses**, naming the field, the plan and the accepted values |

⚠️ **The unanswered case is refused, never defaulted.** An unanswered grace question emitted as "no
grace" is a wrong plan setting that Summit imports cleanly and nobody notices.

⚠️ **The accepted answers are the application's own option labels, stored verbatim.** S31-H established
by tracing render → request parameter → persist that a `RADIO` stores the option token itself:
`applyForProposal.jsp` emits `value="${opt}"`, and both `ApplyForProposal` and
`SaveApplicationProgress` store `paramValue.trim()`. The options are
`None|Carryover|2-1/2 Month Grace Period` (`hfsa_roll_or_grace`) and `None|2-1/2 Month Grace Period`
(`dcap_grace`). **So the stored value is a display label, and a display label is editable in the
Service Manager** — reword an option and later answers stop matching. That fails to a **refusal**, not
to a wrong setting, which is the safe direction and is deliberate.

##### ⚠️ The grace period is a DATE, and it must be — a day count cannot express the rule

**Treas. Reg. §1.125-1(e)** caps a grace period at **the fifteenth day of the third calendar month
after the end of the plan year**. That is a calendar rule, and the length it implies **changes with the
plan year's end month**:

| Plan year ends | Grace period ends | Which is |
|---|---|---|
| 31 December | **15 March** | 74 days |
| 30 June | **15 September** | 77 days |
| 31 January | **15 April** | 74 days |

⚠️ **So no fixed day count can express it**, and the obvious count is wrong in the dangerous direction:
**75 days after 31 December is 16 March — one day beyond the statutory maximum**, every non-leap plan
year. S31-H shipped exactly that and S31-I corrected it before anything was committed. The three rows
above are computed by the emitter itself, not by hand.

AMS therefore emits `Grace Period Calculation (by date)` **true** and supplies `Grace Period Date`,
which Summit supports directly. The date is derived from the plan year end the export already parses —
add three months, set the day to 15 — using month arithmetic, never day arithmetic. **`GRACE_DAYS`
emits empty**: with a date supplied, a day count is redundant and a second source of truth for the same
fact.

⚠️ **No grace length is collected on the application, and none is needed.** The length lives inside the
option text ("2-1/2 Month Grace Period") and is never stored as a number — but the statutory date is
computable from the plan year end alone, so nothing has to be collected. **There is deliberately no
config key for a grace day count**; one existed briefly in S31-H and was removed, because leaving it
available invites someone to set a wrong value that Summit would accept without complaint.

##### ⚠️ Carryover cannot be imported at all

**Carryover has no element anywhere in the `Employer CDH Plan` template** — verified against the live
element list 2026-09-08. A carryover FSA therefore **cannot be imported by any file AMS can produce**
and must be built by hand in Summit.

AMS's response is to **omit that plan from file 2**, log a `WARN` naming the plan, its `ServiceItem`
and its template, and **emit every other plan normally**. The export does not fail. This is the same
shape as the unmapped-elected-item skip: behaviour correct, visibility added.

⚠️ **The WARN cannot name a carryover amount, because AMS collects none.** S31-H searched the complete
`s125_fsa` field set — `hfsa_maximum_amount` is the annual election limit, not a carryover amount, and
the `hra` package's `hra_roll_limit_*` fields belong to a different plan type. Whoever builds the plan
by hand must get the amount from the employer.

⚠️ **Both skips are log-only today.** Nothing on screen tells an operator that a plan was dropped —
they see a file with fewer rows than they expected and no explanation. Surfacing them is filed as a
backlog item.

##### Boolean representation is unproven

⚠️ **Nothing establishes how Summit wants a Boolean in a delimited file.** The element list says only
"Boolean" — `true`/`false`, `1`/`0` and `Y`/`N` are all plausible and none has been imported. Rather
than guess, both sides are config: `SUMMIT_CDH_BOOL_TRUE` and `SUMMIT_CDH_BOOL_FALSE`, defaulting to
`true` and `false`. **One import settles it**, the way every other Summit question in this document was
settled, and the fix is then a properties edit rather than a build.

### 3. Demographics — creates the participant

Columns, A–L: `Employer TPA Custom ID`, `Participant TPA Custom ID`, `First Name`, `Last Name`,
`Mailing Address Line 1`, `Mailing Address City`, `Mailing Address State`, `Mailing Address Zip Code`,
`Effective Date`, `E-mail Address` *(optional)*, `Mailing Address Line 2` *(optional)*, `Branch Code`

```
158E140952|158-P-9001|Alice|Testcase|100 Main Street|Marinette|WI|54143|20270101|alice@example.com|Apt 4B|AMS
158E140952|158-P-9002|Frank|Testcase|1529 Ogden Street|Marinette|WI|54143|20270101|||AMS
```

The second row shows the case the layout exists for: **both optional columns empty, and the file still
parses** because mandatory `Branch Code` follows them. Corrected 2026-09-08 (S29-I) — this section
previously showed a **nine-column** example omitting `Mailing Address Line 2`, `E-mail Address` and
`Branch Code`, and described it as the test template's element set. `ZZ_TEST_DEMO` maps twelve
elements, so that description was wrong as well as short, and two disagreeing layouts in one file is
how the wrong one gets implemented.

⚠️ **`Mailing Address Line 1` has a 50-character maximum.** Established by import 2026-09-08: a
55-character address was rejected per-row with
`'…' exceeds the maximum column size of 50. Length of data exceeded for Mailing Address Line 1.`
while the other five rows in the same file were created. **Field lengths are not documented in the
element picker and no other field has been tested** — name, city, email and the employer-side
address fields are all unknown. Assume any of them may have a limit, and expect to discover it the
same way.

The same address at **48 characters** (`10455 North Shore Industrial Pkwy Bldg C Ste 200`) was
accepted, so the limit is a plain field-length check rather than something subtler.

The AMS census parser accepts addresses longer than 50 characters, so a row that uploads cleanly can
still fail at Summit. Filed as a backlog item.

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

⭐ **AMS emits this file as of S30-A (2026-09-08)** — `SummitExportServlet` at
`/SummitExport?proposalId={id}&type=enrollment`, a fourth `type` alongside `employer`, `cdhplan` and
`demographics`. ⚠️ **The layout is import-proven; the emitter is not.** A hand-built file in this
order was accepted 2026-09-08, but the emitter itself is **compile-verified only — never run, never
imported** (T196). ⚠️ **It also has no UI link** — the three existing files are linked from the Setup
detail screen and this one is not, so the URL must be typed by hand until that is added (T196).

One row per participant, from the **identical** roster query and ordering file 4 uses
(`EmployerParticipantDAO.findByProspectId`, ordered by last name, first name, id). An empty roster
emits a zero-row file rather than refusing, matching file 4.

| Column | Source in AMS |
|---|---|
| `Employer TPA Custom ID` | `resolveEmployerTpaCustomId` — `{SUMMIT_TPA_ID_PREFIX}E{Prospect.id}`, the same value files 1, 2 and 4 emit |
| `Participant TPA Custom ID` | `{SUMMIT_TPA_ID_PREFIX}-P-{employer_participant.id}`, the same composition file 4 emits |
| `Import Plan ID` | file 2's own composition for the **ICHRA** row: `{employerTpaCustomId}-{keySegment}` — **no plan year** (S31-J) |
| `Effective Date` | the `plan_year_start` application answer — the same value file 2 emits as its `Effective Date` and `Plan Year Begin`, **not** `employer_participant.effective_date` (T198) |
| `Participant Annual Election Amount` | the `hra_annual_ee` application answer ("Annual Amount per Employee", HRA package, `hra_benefit_allocation` section), two decimal places, no currency symbol, no thousands separator |

⚠️ **The ICHRA plan is identified by its `keySegment` being `ICHRA`.** HRA Enrollment is for HRA plans
only, and `SummitPlanTemplateResolver.PlanTemplate` carries no plan-kind marker, so the emitter selects
the one configured-and-elected template whose key segment is `ICHRA` and **refuses on zero matches or
on more than one** rather than picking. An enrollment naming the wrong plan imports successfully and
funds the wrong benefit; there is no import-time safety net for it.

⚠️ **The amount is flat per employee, and that is a structural limit, not a simplification.**
`hra_annual_ee` has tiered siblings — `hra_annual_ee_plus_one`, `hra_annual_ee_plus_children`,
`hra_annual_family` — which are **unreachable**: `employer_participant` carries no coverage tier, so
nothing can select among them. Every row in an emitted file therefore carries the same amount. See
T197. ⚠️ This also **corrects T178**, which concluded no per-participant annual amount existed anywhere
in the model: T178 surveyed `proposal_ichra_intake`, V093's additions and `Benefit`, but **not the
application answer set**, where an annual per-employee figure has been a required field all along.
The uniformity concern T178 raises still stands; the "no source exists" premise does not.

⚠️ **A non-numeric amount answer is refused, not normalised.** `hra_annual_ee` is a `TEXT` field.
The emitter accepts an optional leading `$`, digits, and at most two decimal places. **A thousands
separator is refused rather than stripped** — stripping commas reads `7,200` correctly and `7.200,00`
as seven-point-two, and Summit accepts a wrong amount silently, so there is no later stage at which
such a misread would surface. A refusal costs one corrected application answer.

⚠️ **No `Branch Code` column.** That sentinel is Demographics-only — it exists there because column K
is an optional field left blank on most rosters. This layout's last column is mandatory and always
populated, so it needs none, and nothing may be appended after it either.

## ⚠️ ID ownership and uniqueness

The single most important section. Four identifiers, three owned by AMS.

| Identifier | Assigned by | Uniqueness | Notes |
|---|---|---|---|
| `Employer TPA Custom ID` | **AMS** | Per installation | **Upsert key.** Must be stable for the life of the employer — changing it orphans the old record and creates a new one. Derive from something immutable, never from a name or tax ID. ⚠️ **Must be alphanumeric** — no hyphen, no underscore (import-established 2026-09-08; see below). AMS composes `{prefix}E{prospectId}`. |
| `Participant TPA Custom ID` | **AMS** | ⚠️ **GLOBALLY UNIQUE across all employers** | See the warning below. **Hyphens are accepted** — `158-P-9001` imported *and enrolled* successfully 2026-09-08. AMS composes `{prefix}-P-{participantId}`, unchanged. |
| `Import Plan ID` | **AMS** | Per-employer accepted — **treat as suspect** | Two employers took `ICHRA2027` and enrollment resolved correctly. But this is the same evidence pattern that misled on participants. Namespace by employer unless a test proves otherwise. **Hyphens are accepted** — `158140952-PROBEA-2026` imported successfully 2026-09-08. ⚠️ **The shape CHANGED in S31-J: it is now `{employerKey}-{keySegment}` with NO plan year**, because a Summit plan persists across plan years and a year in the upsert key would create a duplicate plan every renewal. T185 is retired. |
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

⚠️ **`Employer TPA Custom ID` must be alphanumeric — and only that field.** Established by import
2026-09-08. `158-140952` and `158_140952` were both rejected with
`Invalid data for Employer TPA Custom ID.` before field binding; `158140952` and `ZZTEST001` were
accepted. AMS therefore composes `{prefix}E{prospectId}`.

**The constraint does not generalise.** The same import round tested the other two AMS-assigned
identifiers and both accept hyphens: `Import Plan ID` as `158140952-PROBEA-2026`, and
`Participant TPA Custom ID` as `158-P-9001`. The participant value was then **enrolled
successfully** — clearing the stage at which the known duplicate-ID failure surfaces, so this is not
another accept-now-fail-later case. Three identifiers, three different validations. Do not infer one
field's rules from another's.

⭐ The full chain — Employer Demographic → Employer CDH Plan → Demographics → HRA Enrollment — was
proven end to end in this round against plan template `1030` (`ICHRA+`).

## Re-import behaviour

Re-importing with the same `Employer TPA Custom ID` **updates in place**. The results comment changes
from `Employer created successfully` to `Employer edited successfully`. Nothing duplicates. A
byte-identical record is rewritten rather than skipped. **No `Record Process Indicator` value is
needed for create or update.**

Consequence: **AMS emits full current state, not deltas.** No sent-state tracking, no create-vs-update
branch, no reconciliation table. Regenerating and re-sending is safe.

⭐ **Demographics upserts on `Participant TPA Custom ID`** the same way Employer Demographic upserts on
`Employer TPA Custom ID`. Observed 2026-09-08: a re-import of five existing participants returned
`Participant Successfully Edited`, while a sixth — previously rejected on a field length — returned
`Participant Successfully Created` in the same file. **The full-state, no-deltas rule therefore extends
to participants, not only to employers**: no sent-state tracking and no create-vs-update branch is
needed on either.

⚠️ **Summit rejects a byte-identical re-send as a duplicate file — on content, not filename.** Observed
2026-09-08: an AMS-generated Demographics file was refused as a duplicate of a hand-built probe with
identical content under a different name. **Changing a single byte was enough to make it process.**

This is a trap in exactly one case. Re-sending unchanged state that already applied is a no-op anyway,
so the dedupe is normally harmless. But when an import fails **for a Summit-side reason** — an unmapped
element, a wrong template, a missing employer — the operator fixes the template and re-sends the same
file, and **nothing happens**. The file is unchanged; the outcome would not have been. ⚠️ **Expect a
silent no-op at the moment a retry is most expected.**

**Consequence for automated transport:** a retry mechanism cannot rely on re-sending the same bytes.
Not yet designed — transport is still manual upload.

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

**ICHRA is a selectable Plan Type in Summit.** Vendor AI said this was not documented and likely used
the generic HRA code; that was wrong.

> ⚠️ **Correction, session 27, 2026-09-07.** An earlier revision of this section listed **ICHRA**,
> **Ins125** and **MERP** among the *native* types. That is wrong, and it is the kind of error that
> would silently break a second installation. **Summit plan types are user-creatable, and several in
> use here are SSA's own** — `ICHRA`, `Ins125` and `MERP` were created by Kevin. They appeared in the
> picker because they already existed in this tenant, not because Summit ships them. **A fresh Summit
> tenant does not carry them.** `FSA`, `DCA` and `HRA` are native.

Types relevant to the bundle, as they appear in **this** tenant's picker: **ICHRA**, **EBHRA**,
**HRA**, **MERP**, **FSA**, **LFSA**, **DCA**, **HSA**, **Ins125**, **Ins125_w_HSA**, plus TRN, PRK,
PRA, DRiP and custom codes. Of these, `FSA`, `DCA` and `HRA` are confirmed native; `ICHRA`, `Ins125`
and `MERP` are SSA-created; the rest are **unverified either way** and should not be assumed native.
`LFSA` existing natively would matter — the limited-purpose FSA fork required by an HSA pairing would
need no workaround — but that is now an assumption to test, not an established fact.

**The plan-type dimension is claims eligibility.** A plan type determines which expense categories
adjudicate — `DRiP` excludes copay and coinsurance, `MERP` allows deductible and coinsurance but not
copay, `HRA` allows all three. This is encoded in the type rather than in per-plan benefit orders.
**AMS mirrors plan types; it does not author them** — they arrive via the monthly billing import,
which refreshes the full list from Summit. No AMS code branches on a plan-type code string (a
repo-wide search for `getCode().equals(...)` returns nothing), so the adjudication meaning lives
entirely in Summit.

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

### Claim processing rules on `Ins125+` templates — all off

> **Evidence class: Summit vendor AI plus reasoning, 2026-09-07. Not test-verified.** Cheap to
> reverse — these are checkboxes.

On the `Ins125+` templates (1031, 1032), **all four Claim Processing Rules and Spenddown are off**:
`None-Contributions Only`, `Enable off-set manual transactions`, `Allow off-set of transactions of
other plans`, `Allow withdrawals`, `Allow on-hold claims`, `Spenddown`.

- **The reason is cross-plan offset.** A manual claim on one plan can clear a denied debit-card
  transaction on another. With the ICHRA and the 125 rail **on the same card**, that is precisely the
  blurring of funding streams the separate plans exist to prevent.
- ⚠️ **`Enable off-set manual transactions` auto-checks `Allow off-set of transactions of other
  plans`.** The two are **not independent** — the narrower-sounding one silently enables the broader
  one. Anyone re-enabling the first should expect the second.
- ⚠️ **Both ends must be off.** An offset needs two plans, so leaving it off on `Ins125+` while it is
  on for `ICHRA+` **may still open the path**. Checking one template is not sufficient verification.
- **Withdrawals off** — cash-out on a premium rail is a **pre-tax exclusion problem**, not merely an
  unusual setting.
- ⚠️ **One vendor claim to treat as thin:** that most claim toggles have no practical effect unless
  the card is enabled for the plan. **The card is enabled here**, so that conditional does not apply,
  and "premium-only plans do not adjudicate" **should not be leaned on as a general safety argument**.

## Summit objects created for the ICHRA+ bundle — 2026-09-07

> **Evidence class: reported by Kevin from the Summit UI, 2026-09-07. Not test-verified.** Nothing
> below has been exercised by an import. No repo evidence exists for any of it and none is possible —
> these are Summit-side objects. Recorded as reported.

### Three new plan types, all **TPA Custom**

| Code | Name | Line of Service |
|---|---|---|
| `Ins125+` | Section 125 Premium — Card Funded | CDH |
| `I_NOTICE` | ICHRA notice plan | COBRA |
| `Q_NOTICE` | QSEHRA notice plan | COBRA |

### Four new plan templates

| Template ID | Name | Plan type | Line of Service |
|---|---|---|---|
| 1030 | `ICHRA+` | `ICHRA` (pre-existing type) | CDH |
| 1031 | `Ins125+ Excepted Benefit` | `Ins125+` | CDH |
| 1032 | `Ins125+ Off-Exchange` | `Ins125+` | CDH |
| 1033 | `ICHRA+ Notice` | `I_NOTICE` | COBRA |

### Notes on what exists and what does not

- **`Q_NOTICE` has no template yet.** The type exists; nothing is configured under it.
- ⚠️ **The `ICHRA` plan type now carries two active templates** — `ICHRA` at **1009** and `ICHRA+` at
  **1030**. AMS carries exactly one `SUMMIT_ICHRA_PLAN_TEMPLATE_ID`, so **every ICHRA sale AMS emits
  points at 1030**, whether or not the sale is a facilitated one. 1009's status is unexamined.
  **Recorded as a known limitation, not a defect** — a single-template config is what the emitter was
  built for, and nothing has yet needed the other.
- **`LFSA` (1002) and `HSA` (1021) templates already exist.** The limited-purpose FSA fork required by
  an HSA pairing has its Summit-side objects ready whenever that decision unblocks — no Summit work
  is on that critical path.
- ⭐ **`ICHRA+ Notice` (1033) is Line of Service COBRA, which confirms** that the notice plan lives on
  **Premium Billing, not CDH**, and therefore **cannot be a row in file 2**. The spec previously
  stated this as design intent; it is now confirmed against a real object. See file 3 in the client
  setup sequence.

## How AMS task checklists key off plan types

Established by reading source in session 27 (S27-D, S27-E), 2026-09-07. Recorded here rather than in a
build plan because it is a durable statement about how the two systems relate, and because the
plan-type decision it justifies ([D40](../analysis/plus_tier_build_plan.md)) depends on it entirely.

⚠️ **Setup and renewal key on different things. They are separate mechanisms and are easy to
conflate.**

| | Setup | Renewal |
|---|---|---|
| Keyed on | `ServiceItem` via `ApplicationModule` | `ServiceItem` via `PlanType` |
| Reached from | `LOS.serviceItem`, `Enhancement.serviceItem`, or a PSP user's manual `AddSetupModule25` | `Benefit.planType.serviceItem` only |
| `ActivityCategory` | 2 (Setup) | 1 (Renewal) |
| Sees the plan type? | **No** — no setup path reads `PlanType` at all | Yes — it is the only input |
| Sees the LOS / sale? | Yes | **No** |

**Renewal keys only on `Benefit → PlanType → ServiceItem (ActivityCategory 1) → RequiredTaskList`.**
`RenewalService` is the only class in the codebase that builds a renewal checklist, and
`PlanType.serviceItem` is its only `ServiceItem` source across all four of its task-building methods
(`getTasksRequiredForRenewal2`, `getTasksRequiredForRenewal`, `getTasksRequiredForBenefit`,
`getTasksRequiredForRenewalItem`).

**`Benefit` is an inbound Summit mirror carrying no trace of the LOS, Enhancement, proposal or
application the sale came through.** There is nothing to traverse back toward the sale even in
principle. **Two employers holding the same plan types resolve byte-identical renewal task sets,
however differently they were sold.** On renewal, the plan type is the only lever the code offers.

**The checklist is the union over the benefits an operator ticks, deduplicated by task id.** One
`RenewalItem` is created per `Benefit` selected; each contributes its plan type's sequence; overlapping
tasks collapse to one. ⚠️ **There is no subtraction and no substitution on either the setup or the
renewal path — a task attached to a plan type fires for every group holding that plan type.** This is
the constraint that drives D40: a task cannot be added for one group's benefit without adding it for
every group holding the same type.

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
11. **SDX-11** — **Is `Schedule Name` unique TPA-wide or per-employer?** Reported as unique within the
    TPA, while schedules are managed **under an employer**. ⚠️ **This is the same shape as the
    participant-key trap** — a per-employer object living in a global namespace, where the create
    succeeds and the collision surfaces somewhere else later. **If TPA-wide, per-employer schedule
    names must be derived from something immutable, not typed.** Testable cheaply: two employers, same
    schedule name.
12. **SDX-12** — **What is `125 PI Elections`' true required field set?** Never imported. Three columns
    show Mandatory in the picker; the rest show Optional. Discovered by importing and reading the
    results file, never by reading the picker — this document's standing rule that **"Optional" does
    not mean optional**.
13. **SDX-13** — **Is `Employer Contribution Schedule` meaningful on a plan whose funding source is
    Participant only?** Both schedule elements are mappable on `125 PI Elections`; only one obviously
    applies to a participant-funded premium plan.

## Test artifacts

Test records `ZZTEST001`, `ZZTEST002`, participants `ZZP001`–`ZZP003`, plan template `1029` and
templates prefixed `ZZ_TEST_` exist in the live Summit environment and should be cleaned up.

**From the 2026-09-08 alphanumeric round**, all in the live tenant and all disposable:

- Employer `158140952`, plus the rejected attempts `158-140952` and `158_140952` (those two created
  nothing — they failed before field binding — but the results files remain)
- Plans `158140952-PROBEA-2026` and `158140952PROBEB2026`

⚠️ **Every plan in the live tenant whose `Import Plan ID` ends in a year carries the SUPERSEDED shape**
(S31-J dropped the plan year from the key). A re-export **will not update them** — it emits
`{employerKey}-{keySegment}`, which Summit reads as a new plan, so the year-suffixed plans will be
**joined by** a new plan rather than replaced. They are test artifacts and disposable, but they must be
deleted rather than left to look like current records, and **no real employer should be left holding a
year-suffixed plan** — if one exists, it needs deleting in Summit before the corrected export runs for
that employer.
- Participants `158-P-9001` and `158P9002`, and their HRA Enrollment records

⚠️ **Employer `158140952` predates the `E` and does not match the shipped scheme.** AMS now composes
`{prefix}E{prospectId}`, so a re-export of that same prospect emits `158E140952` and Summit creates a
**second** employer rather than updating this one. That is accepted — it is a cleanup record, not a
precedent, and no real employer has ever been imported.

## Client setup sequence

⚠️ **Scope statement.** This sequence covers **new client setup only**. Renewal is explicitly out of
scope — nothing below describes, implies, or should be read as a renewal path.

`Benefit` is **not** part of this sequence in either direction. It is an inbound mirror of plans
Summit already created (`summit_id NOT NULL`), so it cannot precede an export — it is downstream of
setup, not a source for it.

The sequence has two phases with different triggers and different failure modes: a **deterministic
core** derivable entirely from the employer's application, and a **partner-dependent tail** that waits
on data from a third party.

### Core (files 1–4)

Everything here is derivable from the employer's application and fires at implementation as one
sequence.

**File 1 — Employer Demographic.** Creates the employer. `Employer TPA Custom ID` =
`{SUMMIT_TPA_ID_PREFIX}E{Prospect.id}` ([LA-29](../analysis/legal_assumptions.md)) — ⚠️ **a
configured installation prefix plus the prospect id, not a bare `Prospect.id`.** `SummitExportServlet.resolveEmployerTpaCustomId` builds it and **refuses to emit** rather than
fall back to a bare id when the prefix is missing or malformed; a local walk on 2026-09-08 observed
`158E140952`. ⚠️ **The separator became `E` on 2026-09-08 (S29-G2)** — Summit rejects a
non-alphanumeric `Employer TPA Custom ID`, so the earlier `{SUMMIT_TPA_ID_PREFIX}-{Prospect.id}` form
is superseded. The session 27 production walk observed `158-136748` under that superseded form, so a
value of that shape in an older close-out is a pre-S29-G2 record, not a current one.
Corrected 2026-09-08 (session 28) — this sentence previously read
`Prospect.id`, which understates the shape of an **upsert key** whose prefix D-89 records as
effectively irreversible once real records land, and a doc that understates it is how a duplicate
employer gets created under a second key. What is unchanged is the point that follows: the custom ID
ties the Summit record back to the AMS application permanently — recurring monthly employer exports
carry the same custom ID, so the linkage established here is what makes every later reconciliation
possible. **`Enable COBRA Administration`
must be set true here** — it is the flag that enables Premium Billing, which file 4 depends on. AMS
sets this true on employers with no COBRA of their own, so it should not later be read as a defect.

**File 2 — Employer CDH Plan.** ⚠️ **One file, multiple rows** — one row per plan, each with its own
`Plan Template ID` and `Import Plan ID`, all sharing the employer key. This is not one import per
plan. Rows, in the order the application elects them:

| Plan | Summit plan type | Carries |
|---|---|---|
| PremiumPath card plan for Presidio premiums | `Ins125` | Employee pre-tax salary reduction |
| PremiumPath card plan for off-exchange premiums | `Ins125` | Employee pre-tax salary reduction |
| ICHRA | `ICHRA` | Employer contribution |
| Health FSA (optional) | `FSA` | Employee pre-tax election |
| Dependent Care FSA (optional) | `DCA` | Employee pre-tax election |

⚠️ **Why the first three are separate plans, not one:** the employee's pre-tax salary reduction and
the employer's ICHRA contribution are distinct funding streams that both happen to land on the same
card. Keeping them as separate plans is what preserves the Section 125 premium rail as a thing
distinct from the ICHRA. Collapsing them would blur the two funding streams together.

**HSA is deliberately deferred** — setup is more involved and it interacts with the limited-purpose
FSA fork. Out of scope for now, not forgotten.

**File 3 — Premium Billing ICHRA notice plan.** The plan that generates ICHRA notices lives on the
**Premium Billing platform, not CDH**, so it is a different file type from file 2 and cannot be a row
in it. Depends on `Enable COBRA Administration` from file 1. ⚠️ **The exact PB file type and its field
requirements are unproven** — the tested chain (above) covered CDH only. Recorded here as unproven,
not as known.

**File 4 — Census (Demographics).** Creates participants. Requires a `Participant TPA Custom ID`
convention. ⭐ **Import-proven as of 2026-09-08**, keying on
`{SUMMIT_TPA_ID_PREFIX}-P-{employer_participant.id}`. This passage previously read that File 4 was
**blocked** because AMS had no AMS-generated employee key to derive one from for a client not yet
imported from Summit; **V094's `employer_participant` roster resolved that**, and the file has since
imported successfully twice into the live tenant.

**File 5 — Enrollment into the PB ICHRA notice plan.** Everyone offered the ICHRA needs the notice,
including employees who will opt out, because the opt-out only exists relative to an offer. This
enrolls the full census, not a subset.

### ⚠️ `125 PI Elections` — the file type the tested chain never touched

> **Evidence class: Summit vendor AI, 2026-09-07. Not test-verified — this file type has never been
> imported.** Recorded as unproven, on the same footing as file 3.

**A `125 PI Elections` import file type exists.** Per Summit's vendor AI it is **the correct file type
for enrolling participants into an `Ins125` plan with per-participant premium amounts**, and **HRA
Enrollment is for HRA plans and is not correct for `Ins125` enrollments**.

⚠️ **This corrects the sequence below.** **The proven chain's HRA Enrollment file (file 4 of that
chain) covers the ICHRA only.** Files 6 and 7 of this setup sequence were both written against HRA
Enrollment and both describe enrolling into `Ins125` plans — see the dated corrections on each.

Mapped columns observed in the template picker, all **Mandatory**: `Employer TPA Custom ID`,
`Participant TPA Custom ID`, `Import Plan ID`.

Available **optional** elements: `Participant Annual Election Amount`, `Effective Date`, `Plan Start
Date`, `Coverage End Date`, `Participant Per Contribution Amount`, `Plan Status`, `Participant
Contribution Schedule`, `Employer Contribution Schedule`, and a run of `Filler` elements.

⚠️ **The true required field set is unproven.** The standing rule of this document applies in full:
**"Optional" does not mean optional** — requirements are discovered by importing a file and reading
the results file, never by reading the element picker. See [SDX-12](#open-questions).

### ⚠️ Contribution schedules are a setup prerequisite — before any election file

> **Evidence class: Summit vendor AI plus reasoning, 2026-09-07. Not test-verified.**

**Contribution schedules must already exist in Summit before an election file will import.**
**Supplying a schedule name in a file does not create one.** This sits **between file 2 and any
election file** in the dependency order.

- ⭐ **The `Participant Contribution Schedule` and `Employer Contribution Schedule` elements take the
  Schedule *Name*** — not a code, and not a Summit-assigned id. **So this adds no fifth Summit-owned
  identifier: the ID-ownership table above stays at four.** AMS supplies a string it controls.
- Schedules are managed **per employer**: Employer → Employer Central → an employer → Schedules tab.
  Funding source is a checkbox pair, **both checked by default**.
- **Two schedules are needed per group:**
  1. a **monthly post on the 1st** for excepted-benefit premiums — uniform across employers, since
     the premium is monthly regardless of payroll;
  2. one matching **the employer's actual payroll calendar** for off-exchange funding — **per-employer
     hand configuration whenever that calendar does not match a default**.
- ⚠️ **This raises the setup-labour floor**, and it compounds with the existing note that Summit
  provides **no import template for creating Premium Billing benefit plans**, so every group already
  needs at least two benefits hand-created. **The pitch is that ongoing administration is automated —
  never that setup is cheap.**
- See [SDX-11](#open-questions) on whether `Schedule Name` is unique TPA-wide or per-employer. That
  question is load-bearing for how these names are generated.

### Tail (files 6–8)

Each of these waits on data from a third party. Event-driven on arrival, not part of the
implementation batch — some may never arrive, in which case the data is entered by hand.

**File 6 — Off-exchange enrollments and amounts**, sourced from HealthSherpa where available. Enrolls
into the off-exchange `Ins125` plan and the `ICHRA` plan with dollar amounts. Where no file is
available, entered by hand.

> ⚠️ **Correction, 2026-09-07.** This was written against **HRA Enrollment**, which is **wrong for the
> `Ins125` leg**. File 6 splits across **two** file types: the off-exchange `Ins125` enrollment goes
> through **`125 PI Elections`**, and only the `ICHRA` leg goes through **HRA Enrollment**. See the
> `125 PI Elections` section above. Its required field set is unproven.

**File 7 — Presidio enrollments**, sourced from Presidio where available. Enrolls into the Presidio
`Ins125` plan based on elections. Underwriting means the enrolled set is not the applied-for set.

> ⚠️ **Correction, 2026-09-07.** This was written against **HRA Enrollment**. File 7 enrolls into an
> `Ins125` plan only, so it goes through **`125 PI Elections`** in full — HRA Enrollment does not
> apply to it at all. See the `125 PI Elections` section above. Its required field set is unproven.

**File 8 — FSA and DCA elections**, where the employer supplies them in a usable form.

### Ingestion — reuse the existing pattern

Partner files arrive in whatever shape the partner sends, with varying column order and naming. AMS
should map them to a canonical form and emit Summit files from that, rather than parsing each partner
format ad hoc.

**AMS already has this pattern.** The V048 configuration family is PSP-scoped, config-driven, and does
exactly this mapping:

- `import_provider` — `provider_id`, `provider_code`, `psp_id NOT NULL`
- `import_file_type` — `file_label`, `target_entity`, `file_format`, `sort_order`
- `import_field_mapping` — `source_column` → `canonical_field`, `is_required`, `is_key`,
  `transform_rule`
- `import_plan_type_mapping` — `source_plan_code` → `target_plan_type_id`, nullable provider = system
  default
- `import_run_log` — per-entity inserted/updated/skipped counters

It is inbound-only today. **Extending it is preferable to inventing a second mapping layer.** Recorded
here as the recommended direction, not as a decision — it has not been designed.

### Ordering

Summit enforces a real dependency order: **employer → plans → participants → enrollments**. A file
referencing an employer or plan that does not exist yet will fail. Within a single enrollment file,
row order does not matter.

⚠️ **Distinguish this from partner-file column ordering**, which is a different problem solved by the
mapping layer above. The two are easy to conflate.

### Transport

Three inbound sources with three different mechanisms — employer upload, Presidio (SFTP likely),
HealthSherpa (existing process). **None is needed to prove the chain.** Manual upload works for all
three initially. Building all three transports on spec is explicitly not the plan.
