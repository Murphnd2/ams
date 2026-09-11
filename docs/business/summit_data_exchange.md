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

- Host `ftp1.dpath.com`, **port 22, SSH-based SFTP** — confirmed by a TCP banner probe on
  2026-09-09 returning `SSH-2.0-MOVEit Transfer SFTP` on port 22 (ports 21, 443, 990, and 2222 all
  closed/timeout). Not FTPS, not implicit TLS. This corrects the port-443/protocol-unresolved text
  this section previously carried; see SDX-01.
- **Host-key algorithm profile** (evidence: `ssh -vv` KEXINIT read, 2026-09-09): the server offers
  `ssh-dss` as its **sole** host-key algorithm; KEX, ciphers and MACs are all current
  (`curve25519-sha256`, AES-CTR/GCM, ChaCha20-Poly1305, HMAC-SHA2). AMS re-enables `ssh-dss` per
  session in `SummitSftpService`, never globally. ⭐ Because 1024-bit DSA is the weakest element in
  an otherwise modern stack, pinning `SUMMIT_SFTP_HOST_KEY` is **recommended, not merely optional**
  — the pinned fingerprint carries the server-identity assurance the host-key algorithm no longer
  provides on its own. Whether DataPath intends to offer a stronger host-key algorithm is
  [SDX-14](#open-questions), for DataPath support, not an AMS work item.
- Three separate folder configurations, each with its own credentials: **Imports**, **Results**,
  **Exports**. All are currently Folder Location = `Datapath Network`, togglable to
  `External Network`. Per DataPath support (2026-09-09), each TPA can create its own sub-folders
  under these three with its own login — no MOVEit administrator is required.
- **No credentials in this repo, ever.**
- Current posture: Encrypted Files off, Header Password Required off, Restrict IP Addresses off.
  Per DataPath support (2026-09-09), the encrypted-files checkbox is not widely used and DataPath's
  own support has no configuration guidance for it, so it stays off.
- Direction is undecided. `Datapath Network` means AMS pushes out, with no inbound exposure on the
  VPS. `External Network` means Summit pulls from a server SSA runs, which means an inbound file
  service to secure and patch. This is reversible by dropdown; **file generation and file delivery
  are separable, and phase one is generating a correct file for manual upload.**
- **Config keys (`ssa.properties`, untracked, lives at `{catalina.base}/conf/ssa.properties` —
  never in this repo):** `SUMMIT_SFTP_HOST`, `SUMMIT_SFTP_PORT` (defaults to 22), `SUMMIT_SFTP_USER`,
  `SUMMIT_SFTP_PASSWORD`, `SUMMIT_SFTP_HOST_KEY` (optional pinned fingerprint), and
  `SUMMIT_SFTP_REMOTE_DIR` (defaults to `/`). Read inline via `AppConfig.get(...)` at their call
  sites in `SummitSftpService` — there is no constants class for these keys, matching the existing
  `SUMMIT_TPA_ID_PREFIX` convention. This note is the only written registry of these key names.
- **Runtime-verified 2026-09-09.** Connect, authenticate and list all succeeded against
  `ftp1.dpath.com:22` from a PSP-admin request to `/SummitSftpTest`. Server identification
  `SSH-2.0-MOVEit Transfer SFTP`. Host-key pinning is verified working; the SHA-256 fingerprint
  observed on 2026-09-09 is
  `b7:35:25:a2:08:d3:20:d2:68:12:d5:da:ae:3b:f4:81:c6:15:bb:91:54:94:33:22:93:5e:2c:56:35:3a:ff:e1`.
  A host-key fingerprint is public by design and belongs in this document — it is not a credential
  and must not be treated as one. It changes if DataPath rekeys the server, in which case the
  connection fails closed and the pin is updated from a fresh unpinned run. ⚠️ The account directory
  name contains spaces — any remote path handling must tolerate them.
- **Observed directory structure, complete (2026-09-09, via the `?dir=` browse override on
  `/SummitSftpTest`):**
  ```
  /DataExchange/Superior State Administrators Inc/
      ImportFiles      empty
      ResponseFiles    ~40 files
      ExportFiles      2 files
  ```
  ⚠️ **The directory names differ from this document's own labels.** The Summit UI calls the three
  folder configurations **Imports**, **Results**, **Exports**; the actual directories are
  **`ImportFiles`**, **`ResponseFiles`**, **`ExportFiles`**. Results maps to `ResponseFiles`, which
  is not guessable from either name.
- **Direction of each folder, and the evidence for it.** `ExportFiles` holds two files Summit itself
  wrote (naming pattern below) — nothing in AMS has ever written there — which is what fixes the
  polarity: `ExportFiles` is Summit-to-AMS. By the same Imports/Results/Exports correspondence,
  `ImportFiles` is AMS-to-Summit (dropping a file there is what starts processing) and
  `ResponseFiles` is Summit-to-AMS (one result file per processed import).
- ⭐ **The `Response_` + source-filename convention is a load-bearing finding.** A source file named
  `ZZ_TEST_CDH_20260908101219.txt` produced a response named
  `Response_ZZ_TEST_CDH_20260908101219.txt` — Summit's response filename is always `Response_`
  prefixed to the exact source filename. **AMS can predict the exact name of its own result file and
  poll for it** — no run ID, correlation table, or separate lookup is needed. Summit itself tolerates
  spaces and punctuation in filenames; AMS should not imitate that.
- `ResponseFiles` is readable over SFTP regardless of how the import arrived. The ~40 files observed
  there (several stamped `20260908`) all came from imports run through the Summit web UI, not SFTP,
  and are visible over SFTP anyway — **the results channel is usable today, independently of AMS
  ever uploading anything.**
- ⚠️ **Corrected 2026-09-09 — the sweep claim below was wrong.** This document previously stated
  `ImportFiles` was empty and **swept, not merely unused** — that `ResponseFiles` holding responses
  to imports no longer present in `ImportFiles` meant a delivered file is consumed after
  processing. That was session 39's inference from an empty folder, not an observation of a sweep,
  and testing (session 40, see below) disproves it: both `AMS_SFTP_IMPORT_PROBE_20260909134434.txt`
  and `ZZ_TEST_DEMO_20260909141133.txt` remained in `ImportFiles` after Summit retrieved and
  processed them. **`ImportFiles` was empty on 2026-09-09 for some other, now-unestablished reason**
  — a manual clear-out, a retention job, or something else. Retrieval reads without removing;
  whatever emptied the folder before, it was not "processing sweeps the folder."
- `ExportFiles` naming pattern: `{prefix}_{Type}_Export_{yyyyMMddHHmmssSSS}.{ext}` — a 17-digit
  timestamp including milliseconds. The two files observed are dated 2025-04-23 and 2023-02-08 (one
  `.Email`, one `.CSV`), under two distinct prefixes.
- ⭐ **SDX-15 RESOLVED (2026-09-09).** An SFTP-delivered file is retrieved and processed the same
  way a Summit web-UI upload is. Evidence: `ZZ_TEST_DEMO_20260909141133.txt` (436 bytes) was
  uploaded to `ImportFiles` over SFTP only — nothing was uploaded through the web UI for this test.
  Before Summit's retrieval ran, `ResponseFiles` held 36 files; after retrieval it held 37, the new
  one being `Response_ZZ_TEST_DEMO_20260909141133.txt` (246 bytes). ⚠️ **This finding stands
  unchanged; how retrieval actually works does not.** The sentence that stood here — "retrieval is
  Summit-side and pull-initiated, not automatic on upload" — implied a manual or scheduled trigger
  caused this. **It did not.** See the misattribution correction below.
- ⭐ **SDX-16 RESOLVED (2026-09-09).** The SFTP account has write permission on `ImportFiles`.
  Proven directly by both drops landing there: `AMS_SFTP_IMPORT_PROBE_20260909134434.txt`
  (259 bytes) and `ZZ_TEST_DEMO_20260909141133.txt` (436 bytes), both confirmed present in a
  post-upload listing.
- ⚠️ **CORRECTED 2026-09-09 (DataPath Support, case CASE-22040, Derrick Norton, FCS Support
  Services) — the retrieval model recorded here until today was wrong.** It read: *"Dropping a
  file into `ImportFiles` over SFTP is necessary but not sufficient for Summit to act on it —
  retrieval is a separate, Summit-side, pull-initiated step, run either on demand ('Initiate File
  Retrieval') or on a schedule configured on that same screen. For this session's tests, no
  schedule was configured; retrieval was run manually."* **That is not how it works.** DataPath's
  direct answer, quoted verbatim: *"Current system design is that the file process goes out and
  looks for new files every 15 minutes."* Retrieval is **fully automatic** and has nothing to do
  with either button on the Imports/Responses screen. ⚠️ **This is a vendor statement, dated
  2026-09-09, not a runtime observation** — DataPath support has already been wrong twice on this
  project (port 21 vs. 22; the sub-folder permission) — but it is consistent with every
  processed-file observation to date, since each delivered file was in fact processed within a
  plausible 15-minute window.
- ⚠️ **Misattribution correction.** Two files were genuinely delivered over SFTP and genuinely
  processed; those response files exist and their content is exactly as recorded elsewhere in this
  section. **The observation is real. What caused it was misattributed.** Every "clicked Initiate
  File Retrieval, then the response appeared" reading recorded today — including this section's own
  prior "retrieval was run manually" framing and the "today's result is not an artifact of clicking
  rather than scheduling" reasoning that stood below — described a coincidence of timing with the
  15-minute poller, not a causal sequence. Per DataPath Support: *"To the knowledge of Support that
  button does not have a completed feature function and will not process any files if pressed."*
  **Any latency figure inferred today from the gap between a click and a response is void** — it
  measured the distance to a poll that was going to happen regardless of the click. **The real,
  DataPath-stated bound on delivery-to-pickup latency is the 15-minute poll interval**, not a
  click-driven figure.
- **Schedule Import — corrected 2026-09-09 (DataPath Support, CASE-22040).** The vendor guide
  (further below) describes it as a working feature scoped to External Network. **DataPath Support
  says it does not work at all**: the option has no completed back end, does not function under any
  network configuration, and the product team intends to remove it as an undesired feature since no
  other client has requested completion. Its documented External-Network scoping was therefore
  never a real constraint on anything — the feature it scoped doesn't function either way.
- **Initiate File Retrieval — corrected 2026-09-09 (DataPath Support, CASE-22040).** Same
  correction: **the button does not work.** The guide (further below) describes it as a manual
  trigger following the same logic as a scheduled run; DataPath Support instead says, to their
  knowledge, it has no completed feature function and will not process any file if pressed. Whether
  it is template-scoped or folder-wide is therefore **moot** — see SDX-20.
- ⭐ **SDX-19 RESOLVED (2026-09-09, DataPath Support) — the automation fork does not exist.**
  Retrieval is already fully automatic, every 15 minutes, under this tenant's current **DataPath
  Network** configuration. No switch to External Network is required and no architecture decision
  is pending — the fork this document raised assumed Schedule Import was the only path to
  automation and that it required External Network; DataPath's answer removes both premises at
  once. Vendor statement, not independently tested by AMS; consistent with every observation to
  date.
- ⭐ **SDX-20 RESOLVED AS MOOT (2026-09-09, DataPath Support).** Initiate File Retrieval does not
  function, so whether it would have been template-scoped or folder-wide does not arise. The
  15-minute poller covers the whole folder — every file in `ImportFiles` is picked up on its own
  schedule, not a template's.
- ⭐ **SDX-17 RESOLVED (2026-09-09) — duplicate handling, DataPath-stated and
  tenant-corroborated.** DataPath Support describes three safeguards: files are retained per IT
  storage policy; **content** identical to an already-processed file is flagged as a duplicate and
  **held pending TPA approval** rather than processed; a **repeated filename** is rejected as a
  duplicate outright; and further checks run against identification records created on import.
  Reference: `https://summitguide.dpath.net/processing-process-approvals/#htoc-duplicate-checking`.
  **The tenant corroborates the content-duplicate behaviour directly**:
  `ZZ_TEST_DEMO_20260909153935.txt` — a new filename carrying content byte-identical to an
  already-processed file — appeared in Summit's File History with status **Held**, 0 records,
  sitting in Currently Processing. That is exactly DataPath's described behaviour, **observed**,
  not merely stated. This is why S40-H made the probe payload's participant IDs vary by
  timestamp — a repeat of the earlier fixed rows would have been held rather than processed on any
  later run, regardless of filename.
- ⚠️ **SDX-21 (new, 2026-09-09) — DataPath's folder-creation answer is internally inconsistent and
  contradicts the tenant.** Asked about `mkdir`'s "Permission denied," DataPath states **both**
  that sub-folders can be created **and** that the main directory structure cannot be changed and
  was automated at setup in a way DataPath believes cannot be modified. Those two statements do not
  reconcile with each other, and neither reconciles with the tenant's own observed behaviour
  (`mkdir` against a new sub-folder denied outright, 2026-09-09). **Recorded as unresolved, not
  guessed at** — and **not currently blocking anything**, since every AMS delivery path targets the
  existing `ImportFiles` folder and none requires creating a new one.
- **Operational consequence 1 — why the `writetest` `ImportFiles` refusal is permanent.** Because
  retrieval is automatic within 15 minutes with no human checkpoint of any kind, anything written
  to `ImportFiles` will be picked up and processed regardless of whether anyone is watching. This
  is why `SummitSftpTestServlet.handleWriteTest`'s refusal on any target resolving under
  `ImportFiles` is a hard code check with no config override — there is no "safe to test carefully"
  mode for that folder; a write there is a live delivery the moment it lands, not before someone
  reviews it.
- **Operational consequence 2 — every delivery must vary both filename and content.** Per
  DataPath's duplicate handling (SDX-17 above): a delivery whose **content** repeats an
  already-processed file is held pending manual approval rather than processed or cleanly rejected;
  a delivery whose **filename** repeats an already-used one is rejected as a duplicate outright.
  **A real delivery mechanism built on this transport must generate a unique filename and unique
  content on every send** — reusing either risks a delivery that neither succeeds nor fails
  cleanly, but instead sits in a queue awaiting a human. This is a design requirement for whatever
  eventually wires `SummitExportServlet` to this transport, not merely a test-servlet concern.
- **Filename-to-template matching (2026-09-09).** Summit matches an inbound file to an import
  template by filename, checked against the Imports/Responses screen's list of recognized import
  templates (46 rows observed; `01_CENSUS` is Demographics). Two files were dropped in the same
  session: `AMS_SFTP_IMPORT_PROBE_20260909134434.txt`, whose name matches no template, produced no
  response and was ignored; `ZZ_TEST_DEMO_20260909141133.txt`, whose `ZZ_TEST_` prefix matches the
  naming convention prior UI test uploads used, matched Demographics and produced a response.
  Content was not the differentiator — both files' content is nonsense to a real import; only the
  probe's had a name Summit didn't recognize at all.
- ⚠️ **Both files are still present in `ImportFiles` after retrieval and processing.** Confirmed by
  a listing taken after the response appeared. Retrieval reads without removing — see the sweep
  correction above. **Until they are removed (Summit UI only — see Test artifacts), a future
  retrieval may reprocess `ZZ_TEST_DEMO_20260909141133.txt` and generate another failure response.**
  A note to Kevin, not a work item. Whether a second retrieval actually reprocesses a file still
  present is not observed — see [SDX-17](#open-questions).
- **Response file format for Demographics, read verbatim from Summit (2026-09-09), the response
  to `ZZ_TEST_DEMO_20260909141133.txt`:**
  ```
  ZZZ-P-901|Failed|Invalid data for Employer TPA Custom ID.
  |ZZZ-NO-SUCH-EMPLOYER
  ZZZ-P-902|Failed|Invalid data for Employer TPA Custom ID.
  |ZZZ-NO-SUCH-EMPLOYER
  ZZZ-P-903|Failed|Invalid data for Employer TPA Custom ID.
  |ZZZ-NO-SUCH-EMPLOYER
  ```
  Pipe-delimited, no header row, no trailer. Field 1 is the **Participant TPA Custom ID** —
  the correlation key back to the source row — field 2 a per-row status, field 3 a message. ⚠️
  **Two things this is NOT established to mean:** whether the offending value (here
  `ZZZ-NO-SUCH-EMPLOYER`) genuinely occupies its own line as a fourth field, or whether that is a
  rendering artifact of how the file was viewed — the record shape is undetermined. And only the
  `Failed` status token has been observed; the spelling of a success status is unknown — see
  [SDX-18](#open-questions).
- ⚠️ **Narrowed 2026-09-09 — this format is established for Demographics only, from a single
  file, and should not be read as the general response shape.** DataPath's
  `260607_SummitGuide_Processing` guide (vendor documentation, content dated 2020-01-03) lists
  Premium Billing import error messages in a different, row-number form
  (`Failed to import data at line: ...`), and none of them matches the
  `Invalid data for Employer TPA Custom ID.` message observed above. **Response file content is
  likely per-import-type, not one general shape** — the block above describes what a Demographics
  response looks like, not what every Summit import template's response looks like. Treat any
  extrapolation to another template's response format as unverified until tested against that
  template directly.
- ⭐ **Write permission confirmed on an existing folder, refused on directory creation — three
  browser tests, 2026-09-09, live production tenant, PSP admin, from Kevin's workstation.**
  (1) `?action=writetest` targeting a new sub-folder `AmsWriteTest` under the account directory
  failed: `Write test FAILED: Permission denied.` (2) A `?dir=` listing of the account directory
  taken immediately after showed `ExportFiles`, `ImportFiles`, `ResponseFiles` and no
  `AmsWriteTest` — isolating the failure to directory creation, not file upload. (3)
  `?action=writetest` retargeted at the existing `ExportFiles` succeeded:
  `AMS_SFTP_WRITE_TEST_20260909133051.txt`, 108 bytes, confirmed present in the post-upload
  listing. **The account can write files into an existing directory. It cannot create
  directories.** Both halves observed, not inferred.
- ⚠️ **Contradicts DataPath's 2026-09-09 statement above that a TPA may create its own
  sub-folders without a MOVEit administrator.** The SFTP account's observed `mkdir` permission
  says otherwise. Most plausibly that statement describes folder creation through the Summit web
  UI — a different permission surface than the SFTP account — but that is **not confirmed**, and
  this document does not assert it. Recorded as a contradiction, not a resolution. **DataPath's own
  follow-up answer on this same day made it worse, not better — see [SDX-21](#open-questions):**
  their answer to a direct follow-up both reaffirms sub-folder creation is possible and says the
  directory structure cannot be changed, which do not reconcile with each other or with the
  tenant.
- **Superseded 2026-09-09:** this bullet previously said upload into `ImportFiles` was untried
  because the test servlet's guard refused to connect. Session 40 added two separately-gated
  actions (`?action=importdrop`, `?action=importdropdemo`) built specifically to make that
  attempt deliberately, and both succeeded — see SDX-15/SDX-16 above.
- **Still not established, by any test to date:** anything about production egress from
  `superiorstate.biz` — every SFTP test across sessions 39 and 40 ran from Kevin's workstation, not
  the VPS. Also still not established: whether `mkdir`'s "Permission denied" reflects the SFTP
  account's own permissions or a MOVEit-side configuration choice — the two were never
  distinguished by any test.
- ⭐ **Corroborated by DataPath's own documentation — `260607_SummitGuide_Processing` (vendor
  guide, content dated 2020-01-03, supplied by Kevin 2026-09-09).** This is six-year-old vendor
  documentation, not a runtime source; where it and the live tenant could ever disagree, the
  tenant wins — DataPath's support desk has already been wrong twice on this project (port 21 vs.
  22; the sub-folder permission). Recorded here because it corroborates the tenant where the two
  overlap. The guide states the FTP folder must be created **by the MOVEit administrator** —
  independently corroborating the observed `mkdir` denial above, and contradicting DataPath
  support's 2026-09-09 sub-folder statement for a **second time**, now from DataPath's own
  documentation rather than a second support interaction. ⚠️ **This does not settle whether the
  denial is the SFTP account's own permission or a MOVEit-side configuration choice** — that
  question is left exactly as open as it was above; the guide says who is supposed to create the
  folder, not why this account specifically cannot.
- **Network configuration, as documented in the same guide.** Imports are configured as one of two
  modes: **DataPath Network** (the TPA pushes files to DataPath's own MOVEit server) or **External
  Network** (DataPath pulls files from an FTP server the TPA hosts). AMS pushes to
  `ftp1.dpath.com` — DataPath's server — so this tenant is configured **DataPath Network**, the
  vendor's name for the direction already recorded above ("Direction is undecided... `Datapath
  Network` means AMS pushes out"). See [SDX-19](#open-questions).
- ⚠️ **Schedule Import, as documented (superseded by DataPath Support's direct answer above).**
  The guide describes "Schedule Import" as available **only** when imports are configured for
  **External Network**, offering Daily, Weekly or Monthly at a set time — no intraday option is
  described anywhere in the guide. The Schedule Import checkbox was visible and unchecked on this
  DataPath-Network tenant on 2026-09-09; the guide's scoping made that visibility puzzling at the
  time. **It no longer is** — DataPath Support's 2026-09-09 answer says the feature has no
  completed back end and does not function under any network configuration, so a checkbox that
  shouldn't (per the guide) even appear, and does nothing when checked (per Support), are the same
  fact stated two ways.
- ⚠️ **Initiate File Retrieval, as documented (superseded by DataPath Support's direct answer
  above).** The guide describes it as a manual trigger for the selected import template(s),
  positioned as a fallback for when a scheduled retrieval fails or an off-schedule import is
  needed, and states it follows the **same logic** as a scheduled run and executes immediately.
  **DataPath Support's 2026-09-09 answer supersedes this description**: to Support's knowledge the
  button has no completed feature function and will not process any file if pressed. The reasoning
  that once stood here — "today's result is not an artifact of clicking rather than scheduling,
  because manual and scheduled retrieval are the same underlying path" — reached a conclusion that
  happens to be correct (the click did not cause today's result) by an incorrect route (it assumed
  the click did something). See the misattribution correction above for the corrected account, and
  [SDX-20](#open-questions) for why the template-scope question this bullet used to raise is now
  moot.
- ⭐ **S42 (2026-09-10) — the push path is live and runtime-verified against the live tenant.**
  1. AMS now pushes to `ImportFiles` from the Summit setup panel (T229, `4d8e7e3`).
  2. Two filename shapes matched the `ZZ_TEST_ER` template:
     `ZZ_TEST_ER_20260910111246.txt` was processed;
     `ZZ_TEST_ER_P140956_20260910094702322.txt` was held as a content duplicate. This
     corroborates matching by template-name prefix.
  3. The content-duplicate Held behaviour (SDX-17) reproduced on the push path.
  4. A Held file does not block the queue — #8 processed while #5 was held.
  5. A Held file appears only in File History and Currently Processing, with no response file.

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
harmless but achieves nothing on its own. ⭐ **SUPERSEDED (2026-09-10).** Kevin confirmed the
template maps the eight optional elements in the emitted order, and a 16-column file imported
successfully.

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

##### Boolean representation is proven

⭐ **RESOLVED (2026-09-10, S45).** `true`/`false` imports correctly **in both directions**, verified
in the Summit UI against plan `158E140952-DCAP` (an AMS-pushed `ZZ_TEST_CDH` file):

- grace enabled = Grace;
- grace by-date `true` → Grace Period = Date, and Grace Date = 12/15/2027;
- run-out enabled, with run-out by-date `false` → Days, 90;
- terminated run-out → "Number of days after termination", 90.

`SUMMIT_CDH_BOOL_TRUE`/`SUMMIT_CDH_BOOL_FALSE` stand at their defaults, `true` and `false`.

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
imported** (T196). ⚠️ The UI link exists. It was added in S30-C (`detailSetup25.jsp`, fourth in the
Setup screen's Summit export block, labelled Summit — HRA Enrollment). The earlier statement here
that it had none was stale (corrected S41, 2026-09-10).

⚠️ The emitter refuses by default — T201 guard, S41, 2026-09-10 (`721864b`). AMS stores no election
state, and this file enrolls every roster participant into the funded plan. So `type=enrollment`
returns a plain-text 400, stating the roster count, unless the request carries
`confirm=ENROLL-ALL-P{proposalId}`. The Setup-screen link sends no token and always refuses.
Runtime-verified locally. With the correct token, the request reached `writeHraEnrollment`, which
refused on a blank `hra_annual_ee`. That is the emitter's first recorded runtime execution. No file
has yet been produced.

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

## Response check (T230)

Shipped 2026-09-10 (S45, phase 1: `employer`, `cdhplan`, `schedules`, `demographics` — `enrollment`
is priority 2 and is not part of this). The Summit setup panel's "Check response" and "Mark done"
controls, backed by `SummitResponseService`, `SummitResponseServlet` (`/SummitResponse`) and
`SummitSetupStatusServlet` (`/SummitSetupStatus`, an include-only panel fragment).

**Design:**

- **Predict, don't correlate.** The response for a pushed file is `Response_` + that row's exact
  pushed filename, in the `ResponseFiles` directory sibling to that row's exact `delivery_dir`
  (`ImportFiles`). Both are stored verbatim on the `summit_file_export` row, so nothing here ever
  reconstructs or guesses either.
- **Fetch on demand.** The response is listed and read over SFTP fresh on every check-page request
  (a new `SummitSftpService.read`, size-capped at 1 MiB) — never cached, never pre-fetched.
- **Classify on the first field.** Success/failure tokens are matched against the trimmed first
  field only, case-insensitively — never by an echoed key, since the response line shape differs
  by file type (see Results files above, and SDX-18). A status matching neither token list is
  `UNKNOWN`, never counted as success.
- **Persist nothing from a response.** A response line can carry personal data — Demographics
  echoes participant names, and a rejection comment has echoed a full street address (Results
  files above). The response is fetched, parsed and rendered in one request, then discarded.
- **Step state lives in `summit_setup_step` (V098)**, a separate entity from `summit_file_export`,
  not an extension of it — Mark done must work with no pushed file at all (the manual override for
  a group already set up in Summit, or entered by hand). One row per `(proposal_id, step_key)`:
  `state` (DONE/OPEN), `basis` (REVIEWED, from the check page with an export id, or MANUAL, from
  the panel), `export_id` (set only for REVIEWED). No auto-completion — every DONE is an explicit
  PSP-admin action.

**Config keys — this section is their only written registry:**

- `SUMMIT_RESPONSE_OK_TOKENS` — comma-separated success tokens, default `Successful`.
- `SUMMIT_RESPONSE_FAIL_TOKENS` — comma-separated failure tokens, default `Failed`.

Both are read inline via `AppConfig.get` in `SummitResponseService`, matching the existing
`SUMMIT_SFTP_*`/`SUMMIT_CDH_*` convention of no dedicated constants class for these keys.

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

1. **SDX-01** — ⭐ **RESOLVED (2026-09-09).** Port 22, SSH-based SFTP — a TCP banner probe from
   Kevin's workstation returned `SSH-2.0-MOVEit Transfer SFTP` on port 22, with ports 21, 443, 990,
   and 2222 all closed/timeout. Both vendor answers were wrong: DataPath support (case CASE-22040,
   2026-09-09) said port 21, and this document previously displayed port 443 — neither reconciles
   with the other, and the live banner is the evidence this document trusts per its own standing
   rule that live results beat vendor statements.
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
14. **SDX-14** — Does DataPath intend to offer a stronger SFTP host-key algorithm than `ssh-dss`
    (currently the server's only option — see Transport)? For DataPath support, not an AMS work item.
15. **SDX-15** — ⭐ **RESOLVED (2026-09-09).** Is a file delivered to `ImportFiles` over SFTP
    processed the same way a file uploaded through the Summit web UI is? **Yes.**
    `ZZ_TEST_DEMO_20260909141133.txt` was delivered to `ImportFiles` over SFTP only, with no web-UI
    upload anywhere in the test; Summit's manually-initiated retrieval picked it up, matched it to
    the Demographics template by filename, and produced `Response_ZZ_TEST_DEMO_20260909141133.txt`
    in `ResponseFiles` (36 files before, 37 after) with a per-row `Failed` status against the
    deliberately nonexistent employer ID. Kevin authorized the deliberate `ImportFiles` write this
    question required after the sub-folder staging path (recorded in the prior text of this entry)
    turned out not to exist. See Transport for the full evidence and the retrieval-model,
    filename-matching, and response-format detail this resolution surfaced.
16. **SDX-16** — ⭐ **RESOLVED (2026-09-09).** Does the SFTP account have write permission on
    `ImportFiles` at all, independent of what processing a write there would trigger? **Yes** —
    proven directly by both session-40 drops landing there (`AMS_SFTP_IMPORT_PROBE_*` and
    `ZZ_TEST_DEMO_*`, see Transport).
17. **SDX-17** — ⭐ **RESOLVED (2026-09-09).** Does a second retrieval reprocess a file that is
    still present in `ImportFiles`? **Not by reprocessing it as new — Summit holds it as a
    duplicate pending manual approval, per DataPath Support (CASE-22040) and corroborated directly
    by the tenant** (`ZZ_TEST_DEMO_20260909153935.txt`, Held, 0 records). A repeated **filename**
    is rejected outright rather than held. See Transport.
18. **SDX-18** — ⭐ **RESOLVED for Employer Demographic only (2026-09-10, S42).** What is the
    per-row success status token in a Summit response file? For Employer Demographic, the success
    token is `Successful`, observed on `Response_ZZ_TEST_ER_20260910111246.txt`:
    `Successful|ZZTESTCompany 9102|158E140952|Employer edited successfully` (72 bytes,
    2026-09-10). The line shape is `Status|Employer Name|Employer TPA Custom ID|Comment` —
    different from Demographics' `Participant TPA Custom ID|Status|Message` shape recorded in
    Transport below, confirming the per-import-type warning already recorded there. **Success
    tokens for `cdhplan`, `demographics` and `enrollment` remain unobserved** — each type's first
    successful push settles its own. ⭐ **2026-09-10 (S45) — resolved for Employer CDH Plan.** The
    success token is `Successful`. The response line shape is
    `Status|Employer TPA Custom ID|Plan Name|Comment`, observed as
    `Successful|158E140952|Dependent Care FSA|Employer Plan created successfully`, from a file
    pushed by AMS over SFTP. Demographics and HRA Enrollment remain unobserved as AMS-pushed
    responses.
19. **SDX-19** — ⭐ **RESOLVED (2026-09-09, DataPath Support, CASE-22040) — the automation fork
    does not exist.** Retrieval is fully automatic every 15 minutes under the tenant's existing
    **DataPath Network** configuration; no switch to External Network, and no architecture
    decision, is needed. The fork assumed Schedule Import was the only path to automation and that
    it required External Network — DataPath's answer that Schedule Import doesn't function at all,
    and that automatic polling runs regardless, removes both premises. Vendor statement, not
    independently tested by AMS; consistent with every observation to date. See Transport.
    ⚠️ **Corrected 2026-09-10 (S42).** DataPath (CASE-22040) described Schedule Import as
    non-functional. Kevin observed that it gates processing: nothing in `ImportFiles` was
    processed from about 15:54 on 2026-09-09 — including `ZZ_TEST_DEMO_20260909160108.txt`, which
    has a correct name — until Kevin enabled the Schedule Import checkbox on the Imports/Responses
    tab (settings shown: Daily, 12:00 PM). Processing then resumed at 11:54, and every waiting
    file was handled. Automatic retrieval therefore depends on that checkbox, contrary to
    DataPath's statement above. See [SDX-22](#open-questions).
20. **SDX-20** — ⭐ **RESOLVED AS MOOT (2026-09-09, DataPath Support, CASE-22040).** Is "Initiate
    File Retrieval" template-scoped or folder-wide? **The question does not arise — the button has
    no completed feature function and does not process files at all**, per DataPath Support. The
    15-minute automatic poller covers the whole folder. See Transport.
21. **SDX-21** — ⚠️ **DataPath's folder-creation answer contradicts itself and the tenant.** Asked
    about the observed `mkdir` denial, DataPath Support states both that sub-folders can be
    created and that the main directory structure cannot be changed and was automated at setup in
    a way DataPath believes cannot be modified — two statements that do not reconcile with each
    other, and neither reconciles with the tenant's observed `Permission denied`. Unresolved.
    **Not currently blocking anything** — every AMS delivery path targets the existing
    `ImportFiles` folder, none requires creating one.
22. **SDX-22** — ⚠️ **Open (2026-09-10, S42).** Does Summit's Schedule Import checkbox gate the
    ~15-minute `ImportFiles` poll? Observed once (2026-09-10): yes — see the SDX-19 correction
    above. Confirm on the next push that processing happens within about 15 minutes, not at the
    configured daily time. **Operational consequence: if the box is unchecked, every AMS push sits
    unprocessed, silently** — no error, no response file, nothing in File History until the
    checkbox is enabled or someone notices the silence. **2026-09-10 (S45):** an AMS-pushed
    `ZZ_TEST_CDH` file received its response within roughly 8 minutes (pushed 20:05:45, marked
    reviewed 20:13). Whether the Schedule Import checkbox is scoped per template is still
    unproven.

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

**From the 2026-09-09 SFTP write test:** `AMS_SFTP_WRITE_TEST_20260909133051.txt` (108 bytes) is in
`ExportFiles` on the live tenant. Removable only through the Summit UI — `SummitSftpService` has no
delete method, by design. `ExportFiles` is never read by AMS, so the file is inert. A note, not a
work item.

**From the 2026-09-09 `ImportFiles` drop tests (SDX-15/SDX-16):**
`AMS_SFTP_IMPORT_PROBE_20260909134434.txt` (259 bytes, ignored — matched no template) and
`ZZ_TEST_DEMO_20260909141133.txt` (436 bytes, matched Demographics, produced an all-`Failed`
response) both remain in `ImportFiles` on the live tenant — retrieval reads without removing (see
Transport's sweep correction). Removable only through the Summit UI; `SummitSftpService` has no
delete method, by design. ⚠️ **Until they are removed, a future retrieval may reprocess
`ZZ_TEST_DEMO_20260909141133.txt` and generate another failure response** — see
[SDX-17](#open-questions). A note to Kevin, not a work item.

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
