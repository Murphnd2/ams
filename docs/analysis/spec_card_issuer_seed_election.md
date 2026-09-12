# Spec — Card Issuer $1 seed election export (`type=cardseed`)

**Status:** ⭐ **Built and live-verified against Summit, 2026-09-12 (session 53).** Originally a
Phase A read-only spec against trunk at `952f8ea`; the build session closed §8 #1 and #5 (below) and
shipped V104 + the `cardseed` writer + admin checkbox + confirm gate; the filename derivation was
investigated separately and found to need no code change. **Working tree uncommitted as of this
writing** — see `docs/session_closeout_2026-09-12_cardseed.md` for the full session record, the SQL
audit, and what remains before production deploy.

**What it does.** One new `SummitExportServlet` type that emits a `125 PI Elections` file with one row
per census participant, each a `$1.00` annual election on the PremiumPath Card Issuer plan, effective
run-date + 1 (TA-e). Downloadable and pushable, PSP-admin only, behind an operator confirm token and a
prior-push refusal. Nothing about money movement, billing, Summit configuration or PremiumPath Card
mechanics is touched or proposed here.

**Every line reference below is to trunk at `952f8ea`. Where a doc and the code disagreed, the code won
and the disagreement is named.**

---

## 1. Call chain and attachment point

The proven chain (file 1 `employer`, file 2 `cdhplan`), every hop:

| Hop | Where |
|---|---|
| UI action | `src/main/webapp/WEB-INF/view/a/activityDetail/columns/detail/detailSummitSetup25.jsp` — download is a GET link `:38`/`:56` (`/SummitExport?proposalId=…&type=cdhplan`); push is a submit button `:57` targeting the hidden POST form `:154-158` (`summitPush-cdhplan`, hidden `proposalId` + `type`). The panel is gated `:9-11` on `sessionScope.local.isPspAdmin()` and a non-null application/proposal. |
| Servlet | `SummitExportServlet` (`@WebServlet … value = "/SummitExport"`, `:66`). `doPost` `:333-337` sets `PUSH_ATTR` and delegates to `doGet`. `doGet` `:135`: PSP-admin session check `:138-142`; `type` whitelist `:145-151`; `IchraAccessResolver.isAvailable` `:167-170`; proposal→prospect `:172-177`; application answers `:182-188`; `resolveEmployerTpaCustomId` `:195`; `resolveCurrentPspId` `:217`; push-mode checks `:224-275` (`SUMMIT_PUSH_ENABLED` `:227`, enrollment refused `:233`, pushable-type whitelist `:239-240`, `SUMMIT_SFTP_IMPORT_DIR` must end in `ImportFiles` `:248-263`, template must be configured `:265`, `ackDuplicate` `:272-276`); `ExportRecord` built `:288-290`; type dispatch `:291-321`. |
| Writer | `writeEmployerCdhPlan` `:788-1101` → `buildCdhPlanRow` `:1110-1157`. Sibling writers: `writeEmployerDemographic` `:495`, `writeDemographics` `:1261`, `writeHraEnrollment` `:1357`. Each ends by calling `resolveFilename(type, legacyName)` `:1600-1604` then `writeFile(...)` `:1634`. |
| Bytes → response / record | `writeFile` `:1634-1657` is the single sink. Download path: sets headers, prints `content`, then `recordExport` `:1699-1730` inserts a `summit_file_export` row (V096). |
| Push | `writeFile:1647` diverts to `pushFile` `:1764` when `record.pushDir() != null`. Steps: template-named filename `:1776-1777`; filename-collision refusal `:1785-1795` (`SummitFileExportDAO.existsPushedFileName`); SHA-256 `:1797-1798`; content-hash duplicate refusal `:1803-1826` (`findByContentHash`, scoped by file type + PSP; `writeDuplicateWarningHtml` `:1901` offers "Push anyway" with `ackDuplicate`); record first `:1827-1836` (`PUSHING`); upload `:1838-1839` `SummitSftpService.upload(record.pushDir(), pushFilename, bytes)`; mark `PUSHED` `:1859`; success page `:1869-1890`. |
| FTP | `SummitSftpService.upload(String remoteDir, String filename, byte[] content)` `src/main/java/net/superiorstate/ams/data/service/SummitSftpService.java:195`. |
| Template name | `SummitImportTemplateResolver.templateNameFor(type)` `src/main/java/net/superiorstate/ams/data/resolver/SummitImportTemplateResolver.java:98-101` — see §2. |
| Status line | `SummitSetupStatusServlet` `:40-44` maps step→file type and renders the latest delivery attempt via `SummitSetupStepDAO.findLatestDeliveryAttempt` `:166`. |

**Attachment point — smallest diff.** A fifth `type` discriminator, `cardseed`, attached at exactly
the six places the existing four already touch and nowhere else:

1. `SummitExportServlet:86-92` — add `private static final String TYPE_CARD_SEED = "cardseed";`.
2. `:147-148` — add `|| type.equals(TYPE_CARD_SEED)` to the whitelist and to the error text `:150`.
3. `:239-240` — add `TYPE_CARD_SEED` to the pushable set. (The enrollment refusal `:233` is untouched.)
4. `:291-321` — a new `else if (type.equals(TYPE_CARD_SEED))` branch **before** the final `else`
   (the T201 enrollment branch), calling the new writer. The `else` stays the enrollment fallthrough.
5. New method `writeCardSeedElection(...)` — a sibling of `writeHraEnrollment`, same signature shape
   `(response, em, proposalId, prospect, answers, employerTpaCustomId, pspId, record)`, ending in
   `resolveFilename(TYPE_CARD_SEED, legacy)` + `writeFile(response, filename, lines, record)`.
6. `pushFile`, `writeFile`, `recordExport`, `SummitSftpService` — **no change**; they are type-agnostic.

`summit_file_export.file_type` is `VARCHAR(20)` (`docs/migrations/V096__summit_file_export.sql:72`);
`cardseed` fits. `SUMMIT_IMPORT_TEMPLATES` gets a `cardseed:<template>` entry per installation (§2).

Do **not** generalise `writeHraEnrollment` or `writeDemographics` into a shared helper — the file
already records that duplication over proven code is deliberate (`:1394-1399` comment).

---

## 2. Layout — exact columns, in order

**No writer emits `125 PI Elections` today.** Verified: `grep -rni elections src/main/java` matches
nothing Summit-related; `writeHraEnrollment`'s own javadoc says "the separate `125 PI Elections` file
type, which AMS does not emit" (`SummitExportServlet.java:1349-1350`, and the refusal text `:1455-1457`);
`project_backlog.md:255` (T195) says "**No emitter exists**". The "AMS emits" column in
`docs/analysis/summit_import_templates_reference.md` §4 (`:254-270`) and `summit_import_spec.md` §2
(`:67-79`) is a **contract for a future emitter**, not a description of code. The two docs agree with each
other on column order (A–K). There is no code to disagree with them.

Template settings (spec §1 `:13-28`): delimiter `|`, dates `YYYYMMDD`, no header/footer/body indicator,
every Default Value empty, `Filler` mapped last and Mandatory.

| Col | Element | Emit | Source |
|---|---|---|---|
| A | Employer TPA Custom ID | always | `employerTpaCustomId` from `resolveEmployerTpaCustomId(prospect)` `:373-380` — same value every other file emits |
| B | Participant TPA Custom ID | always | `summitTpaIdPrefix() + "-P-" + participant.getId()` — byte-identical to Demographics `:1276` and HRA Enrollment `:1482`. Hyphenated form is import-proven on `125 PI Elections` (`158-P-S27-09`, reference §5 `:296`) |
| C | Import Plan ID | always | `sanitize(employerTpaCustomId + cardIssuer.getKeySegment())` — the S31-J/S50 composition at `:1142` and `:1468`, strictly alphanumeric, no plan year. `cardIssuer` is the row §3 resolves |
| D | Effective Date | always | **`today.plusDays(1)`**, `SUMMIT_DATE` format, where `today = LocalDate.now()` taken once per export (the W4 convention `:924`). This is TA-e's "next day" rule, recorded as the design choice for card-issuance enrollments in `docs/business/summit_data_exchange.md:1693-1698`. Guarded: must lie within the Card Issuer row's own resolved plan year (§3 step 4); otherwise refuse |
| E | Plan Start Date | **never** | empty — spec rule 4 (`:439`); a wrong value returns `Plan Not Found` |
| F | Coverage End Date | never | empty (termination only) |
| G | Participant Annual Election Amount | always | constant `"1.00"` — a business constant, not an installation id. Two decimals, no symbol |
| H | Per Contribution Amount | **never** | empty — spec rule 3: if both G and H are present H is ignored and the record shows two numbers |
| I | Participant Contribution Schedule | **never in this build** | empty. Omitting I produces the proven **expectation-only election**: annual recorded, `$0.00` posted, `$0.00` disbursable (`summit_import_contracts.md:83-85`). This is what makes a `$1` placeholder safe regardless of date; it also sidesteps rule 10/11 (schedule registry + "confirm the schedule is selected on the plan" ack), neither of which exists in AMS |
| J | Employer Contribution Schedule | **never** | empty — structurally meaningless on `Ins125+` (spec rule 5) |
| K | Filler | always | constant `"X"` — mandatory trailing sentinel (spec `:31-35`). **Nothing may follow it** |

Row shape: `A|B|C|D||||1.00||||X` — 11 fields, exactly. Column count is validated whole-file before
processing (spec `:36-38`), so the writer must build the row from an 11-element `String.join("|", …)`,
never by conditional appends.

Roster: `EmployerParticipantDAO.findByProspectId(em, prospect.getId())` — **exactly** the set and order
Demographics and HRA Enrollment emit (`:1265`, `:1475`); no filter. Zero rows → refuse (T209 shape,
`:1498-1507` wording adapted). Summit's dependency order is employer → plan → participant → election,
so this file is useless before files 1, 2 and 4 have been processed; the writer does not (and cannot)
verify that — the confirm dialog says it.

**Template name resolution.** `SummitImportTemplateResolver.configured()` `:110-160` parses
`SUMMIT_IMPORT_TEMPLATES` (`ssa.properties`, `AppConfig.get`, **properties-only** — `AppConfig.java:64-73`
reads no DB constant) as `file:templateName,…`, lower-casing the discriminator; `templateNameFor(type)`
`:98-101` returns `Optional.empty()` for an absent key. **Absent key behaviour:** download falls back to
the legacy descriptive filename (`resolveFilename` `:1600-1604`) that matches no Summit template; push is
refused at `:265-269` before anything is generated ("no Summit import template is configured for this
type"). T250 records that the dev property today names `employer/cdhplan/demographics` only. So this
build needs a per-installation config line `cardseed:<the tenant's 125 PI Elections template name>` —
D-row, not source (rule 4). Note `warnOnPrefixCollisions` `:168-183` WARNs when two configured names
share a prefix, and `a.startsWith(b)` is true for **identical** names — when a real `elections:` emitter
later shares this template, expect a harmless WARN per request; not this build's problem.

---

## 3. Card Issuer resolver

**Nothing existing carries the meaning.** Checked:

- `summit_plan_template_map` columns (`SummitPlanTemplateMap.java:54-107`, V095 `:51-69`, V103
  `:144-174`): `psp_id, service_item_id, seq, effective_date_rule, offset_months,
  plan_year_offset_years, template_id, key_segment, label, sort_order, is_active`. The Card Issuer is
  one of five rows on the **same** `service_item_id` (`premiumpath_summit_plan_structure.md:13-19`), so
  anything keyed per service item cannot pick it: `summit_service_item_flags` (V101) is per item, not
  per row. `template_id` is per tenant (rule 4). `effective_date_rule = MOST_RECENT_PAST_MONTHDAY` is
  unique to that row **today** but is a date rule, not a meaning — a second plan using the same rule
  would break it. `label` is free text the admin may rename. `seq` is an ordinal, "not an identifier"
  (`:61-66`).
- `PlanTemplate` carries "no plan-kind marker" — code's own words (`SummitExportServlet.java:1350-1351`;
  `summit_data_exchange.md:771`). T195 (`project_backlog.md:255`) lists "no plan-family marker on
  `summit_plan_template_map`" as an open gap from the session-51 audit.
- No `constant` row and no `SUMMIT_*` property names a card-issuer plan (full key list grepped from
  `src/main/java`: none of the 31 keys).

**Two rule-4-compliant precedents exist for attaching a meaning to a mapping row**, both matching on
`key_segment`: the hardcoded `LEGACY_ICHRA_SEGMENT` convention `writeHraEnrollment:1436-1441`, and the
config-driven `SUMMIT_ALLOWANCE_KEY_SEGMENTS` property `allowanceSegment:665-720`. Both are
key-segment conventions; the ICHRA one is called "the only such convention that exists" (`:1352`).

**Recommended: the smallest new flag, on the row Kevin already administers.**

- **Table/column:** `summit_plan_template_map.is_card_issuer TINYINT(1) NOT NULL DEFAULT 0` (V104, §7).
- **Entity:** `SummitPlanTemplateMap` gains `@Column(name = "is_card_issuer", nullable = false) private boolean cardIssuer;`.
- **Resolver:** `SummitPlanTemplateResolver.PlanTemplate` gains a `boolean cardIssuer` (8-arg
  constructor `:132` → 9-arg; the 4-arg legacy constructor `:127` passes `false`; the table reader
  `:428-430` passes `row.isCardIssuer()`; the property path `:280` passes `false` — the property has no
  slot for it and never will).
- **Admin:** `/SummitPlanTemplateAdmin` `save()` `:194-207` reads one more checkbox
  (`cardIssuer`, same idiom as `active` `:201`); `summitPlanTemplateAdmin25.jsp` renders it on the
  add/edit form and as a column in the listing.
- **Selection in the writer** (mirrors `writeHraEnrollment:1401-1461` exactly):
  1. `configured = SummitPlanTemplateResolver.configuredOrThrow(em, pspId)` — refuse (500) on
     `KeySegmentRejectedException`, same text as `:1402-1409`.
  2. If `configured.isEmpty()` (legacy property path): **refuse** — the synthetic ICHRA fallback has no
     Card Issuer and the property carries no flag. Message: configure the Summit Plan Templates screen.
  3. `candidates` = configured rows whose `serviceItemId` is in `loadElectedServiceItems(em, proposalId)`
     `:460`; `matches` = candidates with `isCardIssuer()`.
  4. `matches.size() != 1` → refuse (400) naming the count and the candidate list (`keySegment=templateId`),
     as `:1442-1460` does. Zero or more than one is never a pick.
  5. Resolve the row's dates with `SummitPlanDateRuleResolver.resolve(planYearStart, planYearEnd,
     rule, offsetMonths, planYearOffsetYears, today, rowRef)` (`:108-110`) — same call file 2 makes
     `:946-950` — and require `planYearBegin ≤ today+1 ≤ planYearEnd`; otherwise refuse (400) naming
     the three dates. Basis: the plan year is binding for an election date (`Plan Not Found`,
     spec §7 concerns `:407-413`, proven on ICHRA 2026-09-12).

**Zero-SQL alternative, not recommended:** `SUMMIT_CARD_ISSUER_KEY_SEGMENT` in `ssa.properties`,
matched against `key_segment` the way `SUMMIT_ALLOWANCE_KEY_SEGMENTS` is. Costs a server file edit and a
Tomcat restart per installation — exactly what V095 exists to remove from the deployment path — and is
installation-wide rather than per PSP. Take it only if Kevin vetoes a migration.

---

## 4. Gate

Reuse, verbatim, the three layers every Summit export already has — no new role, no new resolver:

1. JSP: the panel's own `<c:if test="${sessionScope.local.isPspAdmin() and …}">`
   (`detailSummitSetup25.jsp:9-11`) — the new row sits inside it.
2. Servlet: `Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"))` `:138-142` then
   `IchraAccessResolver.isAvailable(em, request)` `:167-170` (`IchraAccessResolver.java:44-55` —
   PSP admin short-circuits true). Both run before the type dispatch, so the new type inherits them
   with no code.
3. Push: `SUMMIT_PUSH_ENABLED` `:227`, properties-only.

Plus the operator gate spec rule 9 requires for any election file (`summit_import_spec.md:445`),
in the T201 shape: the request must carry `confirm=CARDSEED-P{proposalId}` (GET and POST alike;
`:304-317` pattern). The JSP link and hidden push form carry it, and the push button's `onclick`
confirm text says: *"Push the $1 card-issuer seed election for every participant on the roster? Files
1, 2 and 4 must already be processed in Summit. Summit rejects a participant already enrolled in this
plan. There is no undo."*

---

## 5. Idempotency

**The Elections layout is neither additive nor a replacement — it is rejected per row.** Settled from
the repo, three independent records: `summit_import_contracts.md:62-64` ("Elections do not upsert. A
second election for an enrolled participant fails `Plan Already Enrolled`. A re-sent mixed file
partially succeeds"), `summit_import_spec.md:122-123`, `summit_import_templates_reference.md:42-43`.
Column F on Contributions (additive, `reference:290-298`) is a different template and does not apply.
Nothing else needs settling.

**AMS records nothing per participant.** The only record is `summit_file_export` (file-level: type,
proposal, prospect, SHA-256, delivery status — `recordExport:1699-1730`). Consequences:

- A second run **re-emits every roster row**. Summit fails the already-enrolled ones
  (`Plan Already Enrolled`) and enrolls any participant added since — the roster is replaceable until
  Demographics is pushed (`CensusUploadServlet:307-310`), so "added since" is rare but possible.
- The existing content-hash refusal (`pushFile:1803-1826`) catches a re-push **only on the same day**:
  column D is `today+1`, so the bytes change daily.
- **Add a proposal-level guard** in the new writer, before generation: if
  `SummitSetupStepDAO.findLatestPushed(em, pspId, proposalId, TYPE_CARD_SEED)` (`:149-159`) is non-null,
  refuse (409) with the prior push's id/filename/time and a "Push anyway" form carrying
  `ackPrior=<id>` — the same shape as `writeDuplicateWarningHtml:1901-1927`. Only that exact id
  unlocks it. Download (GET) gets the same refusal as plain text without the form.
- Do **not** write a `summit_setup_step` row or parse the response: the Elections results line is
  participant-keyed (`Participant TPA Custom ID|Status|Message`), and `SummitResponseService.check`
  classifies on `fields[0]` (`:97-106`), so every line would classify `UNKNOWN` — recorded, unchanged
  (`summit_import_spec.md:52-58`). Out of scope here.

---

## 6. Files to touch

| Path | New/Mod | Why |
|---|---|---|
| `docs/migrations/V104__plan_template_map_card_issuer.sql` | **new** | `is_card_issuer` column (§7) |
| `src/main/java/net/superiorstate/ams/model/market/SummitPlanTemplateMap.java` | mod | `cardIssuer` field + accessors |
| `src/main/java/net/superiorstate/ams/data/resolver/SummitPlanTemplateResolver.java` | mod | `PlanTemplate.cardIssuer`; table reader passes `row.isCardIssuer()`; property/legacy paths pass `false` |
| `src/main/java/net/superiorstate/ams/controller/admin/SummitPlanTemplateAdmin.java` | mod | `save()` reads the `cardIssuer` checkbox; no other validation change |
| `src/main/webapp/WEB-INF/view/a/admin/summitPlanTemplateAdmin25.jsp` | mod | checkbox on the form; column in the listing |
| `src/main/java/net/superiorstate/ams/controller/market/SummitExportServlet.java` | mod | `TYPE_CARD_SEED`; whitelist `:147-150`; pushable set `:239-240`; dispatch branch before the enrollment `else`; new `writeCardSeedElection` (§2 layout, §3 selection, §4 confirm token, §5 prior-push guard) |
| `src/main/java/net/superiorstate/ams/controller/market/SummitSetupStatusServlet.java` | mod | one `STEP_FILE_TYPES` entry `"cardseed", "cardseed"` so the row shows `PUSHED · time` |
| `src/main/webapp/WEB-INF/view/a/activityDetail/columns/detail/detailSummitSetup25.jsp` | mod | new row under step 6 above the `125 PI Elections` placeholder (`:120-131`): status include, download link with `confirm`, push button; hidden `summitPush-cardseed` form (`:159-163` pattern) with hidden `confirm` |
| `docs/analysis/migration_tracker.md` · `docs/schema_version_migration.sql` · `docs/claude_memory.md` | mod | migration discipline (CLAUDE.md) |
| `docs/analysis/project_backlog.md` | mod | file the T-row in the same run (S16-G); update T195/T232/T245 cross-refs |
| `ssa.properties` on each installation (**not a repo file**) | config | `SUMMIT_IMPORT_TEMPLATES` += `cardseed:<125 PI Elections template name>`; needs a D-row in `docs/deployment_backlog.md` |

Untouched, deliberately: `pushFile`, `writeFile`, `recordExport`, `SummitSftpService`,
`SummitImportTemplateResolver`, `SummitFileExportDAO`, `SummitResponseServlet`, `writeHraEnrollment`,
`writeEmployerCdhPlan`, `DatabaseInitializer` (T203 precedent — migrations do not touch it).

---

## 7. SQL

`docs/migrations/V104__plan_template_map_card_issuer.sql`, in V103's information_schema +
PREPARE/EXECUTE idempotent shape (`V103:144-150`):

```sql
SET @db = DATABASE();
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'is_card_issuer');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN is_card_issuer TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Marks the one mapping row per sale that is the card-issuer placeholder plan the $1 seed election (type=cardseed) enrols into. Exactly one active flagged row among a sale''''s elected service items, enforced by the emitter, not the database.'' AFTER plan_year_offset_years',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS SELECT 'V104' AS version, '<date>' AS updated;
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V104', 'summit_plan_template_map.is_card_issuer: marks the card-issuer placeholder plan for the $1 seed election export (type=cardseed)', 'V104__plan_template_map_card_issuer.sql', NOW());
```

No rows inserted (rule 5); no unique index — "at most one flagged row per PSP" is wrong (several
service items, several sales) and "per service item" would need `is_active` semantics V095 rejected;
the emitter's `size() != 1` refusal is the enforcement. Reversal: `DROP COLUMN is_card_issuer`.
Backfill: none — Kevin ticks the box on the `PremiumPath Card Issuer` row once V104 is applied.

If the zero-SQL alternative in §3 is chosen instead: **no SQL required.**

---

## 8. Open questions

| # | Question | Settles it |
|---|---|---|
| 1 | ~~Effective date: next day (TA-e) is a recorded design choice, but spec §9 #12 still lists effective-date policy as Kevin's.~~ ⭐ **Closed 2026-09-12 (session 53, build).** Kevin: default the effective date to run date + 1 (TA-e), but render it as an **operator-editable date input** on the confirmation screen rather than fixing it in code — reversible without a deploy, since card issuance timing is untestable until T245. Validated server-side: must parse and must not be in the past, and must fall inside the Card Issuer row's own resolved plan year (§3). Built as `writeCardSeedConfirmPage` in `SummitExportServlet`. The clock sub-question (CST vs the VPS clock for the *default* value) is now low-stakes — a wrong default is a one-click edit on the confirmation screen, not a silent wrong date — and is left unresolved. | **Closed** |
| 2 | Whether Summit issues a card off an **expectation-only** (no-schedule, `$0` posted) `$1` election, and whether at record creation or effective date. The whole capability rests on it. | T245 — untestable until SSA issues cards. Not blocking the build; blocking the first real use |
| 3 | Whether an election date **inside the plan year but before the plan's own effective date** is accepted. | Moot as built (session 53): the effective date is now operator-editable (see #1), so nothing in code assumes it is always after any particular reference date — the plan-year bound in §3 is the only guard, and it is enforced regardless of how the date was chosen |
| 4 | Summit-side prerequisites this spec must not propose and cannot check: the `125+Setup` template is card-enabled; its `Participant funding method` is `Annual` (spec §7 concerns `:417-419`, "likely correct" — not editable once active). | Kevin / DataPath, outside this build |
| 5 | ~~Migration vs property for the flag (§3).~~ ⭐ **Closed 2026-09-12 (session 53, build).** Kevin: column, not property — a config property naming which row is the Card Issuer would store a PSP-scoped reference row identifier in configuration, exactly what build rule 4 forbids. Shipped as V104, `summit_plan_template_map.is_card_issuer TINYINT(1) NOT NULL DEFAULT 0`. Selection at export time mirrors `writeHraEnrollment`'s exactly-one-match refusal precisely: zero or more than one flagged row among a sale's elected services is refused, never picked, and never falls back to `templateId`, `label`, `keySegment` or `seq`. | **Closed** |
| 6 | Whether the new row should be labelled as a step-6 sub-row or its own numbered step. Spec puts it under step 6, above the `125 PI Elections` placeholder, with no file number (T196's precedent). | Cosmetic; builder's call — built as specced |

---

## 9. Live verification, 2026-09-12 (session 53)

⭐ **Import-proven, not just compiled.** Kevin's account, not independently re-verified by this
session — no database or Summit access was used to confirm it, consistent with the read-only fence
every run in this chain has carried. A fresh six-person fake census was loaded to a prospect (the
prior test roster was reused from another demo employer and was replaced first, to avoid collisions).
File 4 (Demographics) was pushed, then the `cardseed` file was pushed.

**Observed in Summit** on participant Marcus Coldwater:

| Field | Value |
|---|---|
| Plan Type | `Ins125+` |
| Plan Name | PremiumPath Card Issuer |
| Import Plan ID | `158E141452SETUP` |
| Plan effective date | `10/1/2025` |
| Participant plan effective date | `09/13/2026` |
| Participant Plan Status | Active |
| Annual Election | $1.00 |
| Disbursable Balance | $1.00 |
| Both funding schedules | None |

**What this settles, by import rather than by document:**

- **The `is_card_issuer` resolver selects the right row.** `158E141452SETUP` composes as
  `employerTpaCustomId` (`158E141452`) + the flagged row's `keySegment` (`SETUP`) — the exact §3
  selection (exactly-one-match among elected services) picked the PremiumPath Card Issuer row and
  no other PremiumPath row on the same service item.
- **The plan effective date (`10/1/2025`) matches `premiumpath_summit_plan_structure.md`'s own
  verified output** for the same employer (`MOST_RECENT_PAST_MONTHDAY`, −3/−1) — corroborating
  W4's date-rule resolver independently of this build.
- **The participant effective date (`09/13/2026`) is the operator-editable default working as
  designed** — run 2026-09-12, TA-e's run-date + 1 = 2026-09-13, accepted because it falls inside
  the plan's own 2026-01-01–2026-12-31 plan year (§3 step 5's guard).
- **An election with no contribution schedule yields a disbursable balance** — the layout's column
  I (Participant Contribution Schedule) was correctly left empty, and Summit still posted the full
  $1.00 as disbursable rather than refusing or zeroing it (both funding schedules read None).
- **The 11-column A–K layout imports clean** — no column-count rejection, no `Plan Not Found`, no
  `Plan Already Enrolled`.

**Not settled by this run:** whether Summit actually issues a physical card from this election (T245
— untestable until SSA issues cards); production behaviour (this ran against Kevin's local dev
Summit tenant only, after he added `cardseed:ZZ_TEST_125_ELECTIONS` to his local `ssa.properties` —
see D-101 for the production config gap).
