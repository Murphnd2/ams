# Questionnaire System Design

**Date:** March 3, 2026
**Status:** Implemented (V039)
**Track:** B2 (per activity_detail_transition_plan.md)

---

## 1. Problem Statement

For many Setups and Renewals, certain LOS, Enhancement, or Benefit items require the PSP to gather specific information from external parties (employers, brokers, vendors) after the application process is complete (for Setups) or where no application process exists (for Renewals). Today this is handled through emails, phone calls, and manual tracking with no structured data collection or status visibility.

The system needs a way to:
- **Preconfigure** questionnaire templates scoped to specific LOS/Enhancement/ServiceItem combinations
- **Attach** questionnaires to activities (Setup, Renewal, Ticket) automatically or manually
- **Collect** structured responses from external parties via GUID-based public links
- **Track** completion status on the activity detail page
- **Integrate** with the automation email system so questionnaire links can be embedded in outbound emails
- **Lock** submitted questionnaires from further editing until a PSP user reopens them

---

## 2. Design Principles

- **Dual-mode: Native + External** — each questionnaire is either **native** (built from QuestionnaireFields, rendered by AMS) or **external** (a pointer to a Jotform or other external form URL). Native mode handles simple-to-moderate data collection. External mode handles complex forms with conditional logic, calculations, tutorials, and multi-page flows that would be impractical to rebuild natively. Both modes share the same container: same scoping, same activity attachment, same status tracking, same automation email tokens.
- **External-first data collection** — almost all questionnaires will be completed by someone outside the PSP (employer contacts, brokers, vendors) via public GUID links or external form URLs.
- **Parallel framework (Option B)** — new entities decoupled from the sales Application pipeline. The Application system is tightly coupled to Proposals, LOS join tables, and the sales lifecycle. Questionnaires need their own domain model that can evolve independently.
- **Shared field rendering** — extract the field-type rendering logic (TEXT, TEXTAREA, NUMBER, DATE, SELECT, RADIO, BOOLEAN, CHECKBOX, JSON) into a reusable JSP include used by both the Application and Questionnaire systems. Only applies to native-mode questionnaires.
- **Preconfigured seed data** — ship starter questionnaire templates via `DatabaseInitializer`, loaded from a JSON seed file (`src/main/resources/questionnaire/questionnaire_seeds.json`). Seeds can be native (with sections and fields) or external (with a Jotform URL). PSP admins customize from there.
- **Separate from Resource Library** — the Resource Library stores static content (documents, videos, links) with a one-to-many-viewers model. Questionnaires are templates instantiated per-activity with per-instance answers — a fundamentally different object.

---

## 3. Lifecycle

### 3.1 Native Mode (built-in fields)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  NOT_STARTED  │────▶│  IN_PROGRESS  │────▶│  SUBMITTED   │────▶│   REVIEWED   │
└──────────────┘     └──────┬───────┘     └──────┬───────┘     └──────────────┘
                            │                     │
                            │                     │  PSP reopens
                            │                     ▼
                            │              ┌──────────────┐
                            └──────────────│   REOPENED    │
                                           └──────────────┘
                                        (returns to editable,
                                         same GUID link works)
```

### 3.2 External Mode (Jotform / external URL)

```
┌──────────────┐                          ┌──────────────┐     ┌──────────────┐
│  NOT_STARTED  │────────────────────────▶│  SUBMITTED   │────▶│   REVIEWED   │
└──────────────┘   PSP manually marks     └──────┬───────┘     └──────────────┘
                   complete after                 │
                   verifying external              │  PSP reopens
                   submission                     ▼
                                           ┌──────────────┐
                                           │   REOPENED    │
                                           └──────────────┘
                                        (PSP marks NOT_STARTED,
                                         notifies respondent to
                                         resubmit externally)
```

External mode skips `IN_PROGRESS` — AMS has no visibility into partial saves on the external form. The PSP user manually transitions `NOT_STARTED` → `SUBMITTED` after confirming the external form was completed.

### State Definitions

| Status | Who Acts | Native Form State | External Form State | Transitions To |
|--------|----------|-------------------|---------------------|----------------|
| `NOT_STARTED` | — | Editable (no data saved yet) | Link available, awaiting submission | `IN_PROGRESS` (native: first save), `SUBMITTED` (external: PSP marks complete) |
| `IN_PROGRESS` | External person | Editable, auto-save active | *Not used in external mode* | `SUBMITTED` (explicit submit) |
| `SUBMITTED` | External person (native) or PSP user (external) | **Locked — read-only** | Marked complete by PSP | `REVIEWED` (PSP marks reviewed), `REOPENED` (PSP unlocks) |
| `REVIEWED` | PSP user | Locked — read-only | Informational | `REOPENED` (PSP unlocks) |
| `REOPENED` | PSP user triggers | Editable again, same GUID | Reverted to NOT_STARTED for re-collection | `SUBMITTED` (re-submit) |

### Key Behaviors — Native Mode

- **Save Progress:** AJAX auto-save (same pattern as `SaveApplicationProgress`). Each save updates field values and sets status to `IN_PROGRESS` if currently `NOT_STARTED`.
- **Submit:** Explicit submit button. Sets status to `SUBMITTED`, captures `date_submitted`, `submitted_by_name`, `submitted_by_email`. Form becomes read-only.
- **Lock on Submit:** The public form servlet checks status on every GET. If `SUBMITTED` or `REVIEWED`, renders all fields as read-only with a "This questionnaire has been submitted" banner.
- **Reopen:** PSP user action from the activity detail page. Sets status back to `IN_PROGRESS`, records `reopened_by` and `date_reopened`. The external person can then edit via the same GUID link.
- **Review:** Optional PSP action after submission. Marks the questionnaire as reviewed with timestamp and reviewer. Informational — does not change the external person's ability to see the form.

### Key Behaviors — External Mode

- **Link generation:** The automation email token or "Copy Link" button provides the external URL (with optional merge tokens resolved). The instance GUID is appended as a query parameter (`?ref={instance_guid}`) for submission correlation.
- **No auto-save or submit tracking:** AMS does not control the external form. The PSP user is responsible for verifying submission and manually updating status.
- **Mark Complete:** PSP user clicks "Mark Complete" on the activity detail page, which sets status to `SUBMITTED` and captures the timestamp. Optionally captures submitter name/email if the PSP enters them.
- **Reopen:** PSP user reverts status to `NOT_STARTED`. The external person would need to be notified separately to resubmit (no automatic notification in v1).

---

## 4. Entity Model

### 4.1 Questionnaire (Template Definition)

Mirrors `ApplicationSection`. Defines a reusable questionnaire template at the PSP level.

**Table: `questionnaire`**

| Column | Type | Notes |
|--------|------|-------|
| `questionnaire_id` | BIGINT, PK, AUTO_INCREMENT | |
| `psp_id` | INT, FK → `assignee(id)` | PSP-scoped |
| `name` | VARCHAR(100), NOT NULL | Display name: "FSA Compliance Testing" |
| `description` | VARCHAR(500) | Optional guidance text shown at top of form |
| `activity_type` | VARCHAR(20), DEFAULT 'ALL' | `SETUP`, `RENEWAL`, `TICKET`, or `ALL` |
| `external_url` | VARCHAR(500), NULLABLE | If populated → external mode (Jotform, etc.). If NULL → native mode (uses QuestionnaireFields). |
| `sort_order` | INT | Ordering in admin lists |
| `suppressed` | TINYINT, DEFAULT 0 | Soft delete |
| `template_key` | VARCHAR(50), NULLABLE | For preconfigured seed questionnaires (duplicate detection) |

**Mode determination:** `external_url IS NULL` → native mode. `external_url IS NOT NULL` → external mode.

**External URL merge tokens:** The `external_url` can contain merge tokens that are resolved at link-generation time when an instance is created or a link is copied:
- `{erName}` — employer name from the activity's primary contact
- `{activityId}` — the AMS activity ID
- `{instanceGuid}` — the questionnaire instance GUID (for submission correlation)

Example: `https://form.jotform.com/92684899657181?testId={activityId}&erName={erName}&ref={instanceGuid}`

**Unique index:** `(template_key, psp_id)` — same pattern as ApplicationSection V034.

**JPA Entity: `Questionnaire`**
- Package: `net.superiorstate.ams.model.activity.questionnaire`
- Key fields: `name`, `description`, `activityType`, `externalUrl`, `sortOrder`, `suppressed`, `templateKey`
- Method: `isExternal()` → returns `externalUrl != null && !externalUrl.isBlank()`
- Method: `resolveExternalUrl(String erName, Long activityId, String instanceGuid)` → replaces merge tokens in `externalUrl`
- Relationships:
  - `psp` → M:1 PSP
  - `fieldList` → 1:M QuestionnaireField (ordered by sortOrder) — empty for external-mode questionnaires
  - `losList` → M:N LOS (join: `questionnaire_los`)
  - `enhancementList` → M:N Enhancement (join: `questionnaire_enhancement`)
  - `serviceItemList` → M:N ServiceItem (join: `questionnaire_serviceitem`)

### 4.2 QuestionnaireField (Individual Question)

Mirrors `ApplicationField`. Same field-type system.

**Table: `questionnaire_field`**

| Column | Type | Notes |
|--------|------|-------|
| `field_id` | BIGINT, PK, AUTO_INCREMENT | |
| `questionnaire_id` | BIGINT, FK → `questionnaire` | |
| `field_key` | VARCHAR(100), NOT NULL | Internal identifier (lowercase, underscores) |
| `label` | VARCHAR(200) | Display label |
| `field_type` | VARCHAR(20), DEFAULT 'TEXT' | TEXT, TEXTAREA, NUMBER, DATE, SELECT, RADIO, BOOLEAN, CHECKBOX, JSON |
| `select_options` | VARCHAR(500) | Pipe-delimited options for SELECT/RADIO/CHECKBOX |
| `help_text` | VARCHAR(500) | Tooltip or guidance |
| `is_required` | TINYINT, DEFAULT 0 | |
| `sort_order` | INT | |
| `suppressed` | TINYINT, DEFAULT 0 | |

**Unique index:** `(questionnaire_id, field_key)` — field keys unique within a questionnaire.

**JPA Entity: `QuestionnaireField`**
- Package: `net.superiorstate.ams.model.activity.questionnaire`
- Relationships:
  - `questionnaire` → M:1 Questionnaire

### 4.3 QuestionnaireInstance (One Filling, Tied to an Activity)

Mirrors `Application`. Represents one instance of a questionnaire attached to a specific activity.

**Table: `questionnaire_instance`**

| Column | Type | Notes |
|--------|------|-------|
| `instance_id` | BIGINT, PK, AUTO_INCREMENT | |
| `questionnaire_id` | BIGINT, FK → `questionnaire` | Which template |
| `activity_id` | BIGINT, FK → `assignee(id)` | The Setup, Renewal, or Ticket |
| `todo_id` | BIGINT, FK → `todo(id)`, NULLABLE | Optional: specific task gating |
| `instance_guid` | VARCHAR(36), NOT NULL, UNIQUE | UUID for public access links |
| `status` | VARCHAR(20), DEFAULT 'NOT_STARTED' | NOT_STARTED, IN_PROGRESS, SUBMITTED, REVIEWED, REOPENED |
| `submitted_by_name` | VARCHAR(100), NULLABLE | Who filled it out (external) |
| `submitted_by_email` | VARCHAR(200), NULLABLE | |
| `date_created` | TIMESTAMP, DEFAULT CURRENT_TIMESTAMP | |
| `date_submitted` | TIMESTAMP, NULLABLE | |
| `date_reviewed` | TIMESTAMP, NULLABLE | |
| `reviewed_by_id` | BIGINT, FK → `assignee(id)`, NULLABLE | PSP user who reviewed |
| `date_reopened` | TIMESTAMP, NULLABLE | |
| `reopened_by_id` | BIGINT, FK → `assignee(id)`, NULLABLE | PSP user who reopened |

**JPA Entity: `QuestionnaireInstance`**
- Package: `net.superiorstate.ams.model.activity.questionnaire`
- Relationships:
  - `questionnaire` → M:1 Questionnaire
  - `activity` → M:1 Assignee (the Setup/Renewal/Ticket)
  - `todo` → M:1 ToDo (optional gating)
  - `reviewedBy` → M:1 Person
  - `reopenedBy` → M:1 Person
  - `fieldValues` → 1:M QuestionnaireFieldValue

### 4.4 QuestionnaireFieldValue (The Answers)

Mirrors `ApplicationFieldValue`. Stores one answer per field per instance.

**Table: `questionnaire_field_value`**

| Column | Type | Notes |
|--------|------|-------|
| `field_value_id` | BIGINT, PK, AUTO_INCREMENT | |
| `instance_id` | BIGINT, FK → `questionnaire_instance` | |
| `field_id` | BIGINT, FK → `questionnaire_field` | |
| `field_value` | TEXT | The answer |

**Unique index:** `(instance_id, field_id)` — one answer per field per instance.

**JPA Entity: `QuestionnaireFieldValue`**
- Package: `net.superiorstate.ams.model.activity.questionnaire`
- Relationships:
  - `instance` → M:1 QuestionnaireInstance
  - `field` → M:1 QuestionnaireField

### 4.5 Scoping Join Tables

| Table | Columns | Purpose |
|-------|---------|---------|
| `questionnaire_los` | `questionnaire_id`, `los_id` | M:N → LOS |
| `questionnaire_enhancement` | `questionnaire_id`, `enhancement_id` | M:N → Enhancement |
| `questionnaire_serviceitem` | `questionnaire_id`, `purpose_id` | M:N → ServiceItem (templatepurpose) |

These follow the exact same pattern as `applicationsectionlos` and `applicationsectionenhancement`.

---

## 5. Scoping Rules

When an activity is created, the system determines which questionnaires to auto-attach based on:

1. **Activity type match** — questionnaire's `activity_type` must be `ALL` or match the activity type (SETUP, RENEWAL, TICKET)
2. **LOS/Enhancement/ServiceItem overlap** — at least one of the activity's LOS, Enhancement, or ServiceItem must appear in the questionnaire's scoping join tables (OR logic across all three)
3. **Not suppressed** — `suppressed = 0`

The auto-attach logic runs in `TaskBuilder25` (for Setups/Tickets) and `RenewalService` (for Renewals). For each matching questionnaire, a `QuestionnaireInstance` is created with status `NOT_STARTED` and a fresh UUID.

PSP users can also manually attach additional questionnaires from the activity detail page.

---

## 6. Servlet Plan

### 6.1 Public-Facing (External Users — Native Mode Only)

| Servlet | URL | Purpose |
|---------|-----|---------|
| `FillQuestionnaire` | `/q/{guid}` | GET: renders native form (editable or read-only based on status). For external-mode questionnaires, redirects to the resolved `external_url`. POST: submits the native questionnaire. |
| `SaveQuestionnaireProgress` | `/SaveQuestionnaireProgress` | AJAX POST: auto-saves field values, sets status to IN_PROGRESS. Native mode only. |

**`FillQuestionnaire` GET behavior:**
- Looks up `QuestionnaireInstance` by GUID
- **If external mode** (`questionnaire.isExternal()` is true):
  - Resolves merge tokens in the external URL using activity data (`{erName}`, `{activityId}`, `{instanceGuid}`)
  - Redirects (HTTP 302) to the resolved external URL
  - No form rendering — the external service handles everything
- **If native mode:**
  - If `NOT_STARTED`, `IN_PROGRESS`, or `REOPENED` → render editable form with save/submit buttons
  - If `SUBMITTED` or `REVIEWED` → render read-only form with "Submitted on {date}" banner
  - Loads questionnaire template fields, pre-fills with any saved values
- No login required — GUID-based access (same as `ApplyForProposal`)

**`FillQuestionnaire` POST behavior (native mode only):**
- Validates required fields
- Saves all field values (upsert pattern from `ApplyForProposal`)
- Sets status to `SUBMITTED`, captures `submitted_by_name`, `submitted_by_email`, `date_submitted`
- Sends email notification to activity assignee
- Renders confirmation page

**`SaveQuestionnaireProgress` behavior (native mode only):**
- Same AJAX pattern as `SaveApplicationProgress`
- Accepts GUID + field key/value pairs
- Upserts into `questionnaire_field_value`
- If status is `NOT_STARTED`, sets to `IN_PROGRESS`
- Returns JSON `{"status":"saved","timestamp":"..."}`

### 6.2 PSP-Facing (Internal Users)

| Servlet | URL | Purpose |
|---------|-----|---------|
| `ViewQuestionnaireResponse` | `/ViewQuestionnaireResponse` | View submitted answers for native-mode questionnaires (read-only detail page) |
| `QuestionnaireAction` | `/QuestionnaireAction` | Admin actions: reopen, review, attach, detach, mark complete (external mode) |

**`QuestionnaireAction` cases:**
- `reopen` — sets status to `IN_PROGRESS` (native) or `NOT_STARTED` (external), records `reopened_by_id` and `date_reopened`
- `review` — sets status to `REVIEWED`, records `reviewed_by_id` and `date_reviewed`
- `attach` — manually creates a `QuestionnaireInstance` for a selected questionnaire template on an activity
- `detach` — removes a `QuestionnaireInstance` (only if `NOT_STARTED`)
- `markComplete` — **external mode only.** Sets status to `SUBMITTED`, captures `date_submitted`. Optionally accepts `submitted_by_name` and `submitted_by_email` entered by the PSP user.
- `markIncomplete` — **external mode only.** Reverts status to `NOT_STARTED`.

### 6.3 Admin (Questionnaire Template Management)

Option A: **New tab in ServiceManager25** — keeps all service configuration in one place.
Option B: **Separate QuestionnaireManager page** — if ServiceManager gets too crowded.

Recommend **Option A** (new "Questionnaire" tab) to start. The admin UI would provide:
- Create/edit/suppress questionnaire templates
- **Mode toggle:** "Native Form" vs "External URL" — choosing external shows a URL input field; choosing native shows the field builder
- Add/edit/reorder/suppress fields within a questionnaire (native mode only)
- Assign LOS/Enhancement/ServiceItem scoping (checkboxes, same pattern as ApplicationSection)
- Set activity type scope (dropdown: Setup, Renewal, Ticket, All)
- Preview the form (native) or test the URL (external)

---

## 7. Automation Email Integration

### New Token: `<q>questionnaire_name</q>`

When `SendAutoFinal25` processes an automation template, it looks for `<q>` tokens and replaces them with a clickable link to the questionnaire.

**How it works:**
1. The automation template contains: `<q>Section 125 Eligibility Testing</q>`
2. `SendAutoFinal25` finds the `QuestionnaireInstance` attached to the current activity whose questionnaire name matches
3. **Native mode:** generates the AMS public URL: `{webPath}/q/{instance_guid}`
4. **External mode:** resolves merge tokens in the `external_url` using activity data (`{erName}`, `{activityId}`, `{instanceGuid}`)
5. Replaces the token with an HTML anchor: `<a href="{resolved_url}">Section 125 Eligibility Testing</a>`

**Edge cases:**
- If no matching instance is found, the token is replaced with the questionnaire name as plain text (no link) and a warning is logged
- If multiple instances match (same questionnaire name, same activity), uses the most recently created one

This follows the established pattern of `<rf>` for reference links and `<l>` for link prompts.

---

## 8. Activity Detail Integration

The activity detail page gets a new **Questionnaires panel** showing all attached questionnaire instances:

| Column | Content |
|--------|---------|
| Name | Questionnaire template name (linked to response viewer for native, or external URL for external) |
| Mode | Badge: NATIVE (blue) or EXTERNAL (purple) |
| Status | Badge: NOT_STARTED (gray), IN_PROGRESS (blue), SUBMITTED (green), REVIEWED (dark green), REOPENED (orange) |
| Submitted | Date + submitter name (if submitted) |
| Actions — Native | View (if submitted) · Reopen (if submitted/reviewed) · Copy Link (GUID URL) · Detach (if not started) |
| Actions — External | Open Form (opens external URL in new tab) · Mark Complete (if not started) · Mark Incomplete (if submitted) · Reopen (if submitted/reviewed) · Copy Link (resolved external URL) · Detach (if not started) |

This panel sits alongside the existing documents/links section — similar visual weight, same card-based layout.

**Completion gating (optional, Phase 7):** A task's "Complete" button can be disabled if a linked questionnaire instance (via `todo_id`) is not yet in `SUBMITTED` or `REVIEWED` status. This applies to both native and external mode. This is opt-in per task configuration, not a global enforcement.

---

## 9. Shared Field Renderer

Extract from `applyForProposal.jsp` into a reusable JSP include:

**`/WEB-INF/view/components/fieldRenderer.jsp`**

Accepts via `<jsp:attribute>` or page-scoped variables:
- `field` — the field object (QuestionnaireField or ApplicationField — both have the same getter signatures)
- `fieldValue` — the current value (String, nullable)
- `readOnly` — boolean (true for submitted/reviewed questionnaires and application reviews)

Renders the appropriate HTML input based on `field.getFieldType()`:
- TEXT → `<input type="text">`
- TEXTAREA → `<textarea>`
- NUMBER → `<input type="number">`
- DATE → `<input type="date">`
- SELECT → `<select>` with options from `field.getSelectOptionsList()`
- RADIO → radio buttons
- BOOLEAN → Yes/No toggle
- CHECKBOX → multi-select checkboxes
- JSON → specialized rendering (plan builder, etc.)

Both `ApplicationField` and `QuestionnaireField` expose identical method signatures (`getFieldKey()`, `getLabel()`, `getFieldType()`, `getSelectOptionsList()`, `isRequired()`, `getHelpText()`), so the include works with either entity type without an interface.

---

## 10. Preconfigured Seed Questionnaires

Loaded by `DatabaseInitializer` from `src/main/resources/questionnaire/questionnaire_seeds.json` using the `template_key` pattern (same as ApplicationSection starter packages). Each has a `template_key` for duplicate detection.

### Starter Templates

| Template Key | Name | Mode | Activity Type | Scope | Notes |
|---|---|---|---|---|---|
| `q_cobra_renewal` | COBRA Renewal Questionnaire | **Native** | RENEWAL | Enhancement: COBRA | 8 sections, 50+ fields. Open enrollment, up to 6 plan blocks, attestation. |
| `q_125_eligibility` | Section 125 Eligibility Testing | **External** | RENEWAL | LOS: POP/125 | Jotform URL: `https://form.jotform.com/92684899657181`. Heavy conditional logic, tutorial pages, business-structure-dependent calculations. 2,140 submissions in Jotform. |
| `q_105_participation` | Section 105 Participation Test | **External** | RENEWAL | LOS: HRA, MERP | Jotform URL: `https://form.jotform.com/240454860219153`. Conditional logic for affiliate business counts, HCE calculations. 157 submissions in Jotform. |
| `q_fsa_testing` | FSA Compliance Testing | Native | SETUP | LOS: FSA | Testing vendor, date, pass/fail, results summary. |
| `q_hsa_custodian` | HSA Custodian Setup | Native | SETUP | LOS: HSA | Custodian name, account/routing numbers, funding method. |
| `q_debit_card` | Debit Card Order Form | Native | SETUP | Enhancement: Debit Cards | Quantity, shipping address, rush order flag. |
| `q_bank_info` | Employer Bank Information | Native | SETUP | ALL | Bank name, routing/account numbers, account type. |
| `q_renewal_confirm` | Renewal Confirmation | Native | RENEWAL | ALL | Plan year dates, changes requested, authorized by. |
| `q_cobra_renewal_jf` | COBRA Renewal (Jotform) | **External** | RENEWAL | Enhancement: COBRA | Jotform URL: `https://form.jotform.com/241552616097055`. Alternative to native version — PSP can choose which to use and suppress the other. 122 submissions in Jotform. |

**Note:** The COBRA Renewal is provided in both native and external (Jotform) versions. PSPs can suppress whichever they don't want. The native version is a direct field-for-field port from the Jotform; the external version preserves the original Jotform with its conditional show/hide logic for the repeating plan blocks.

**External URL merge tokens in seeds:**
```
https://form.jotform.com/92684899657181?erName={erName}&ref={instanceGuid}
https://form.jotform.com/240454860219153?erName={erName}&ref={instanceGuid}
https://form.jotform.com/241552616097055?erName={erName}&ref={instanceGuid}
```

These are examples — the exact set would be refined based on real operational needs. PSP admins can suppress any they don't use, create new ones, and switch between native and external modes.

---

## 11. Build Phases

### Phase 1: Schema + Entities (V038)
- Migration script: 4 tables (with `external_url` on questionnaire) + 3 join tables + indexes
- JPA entities: `Questionnaire` (with `isExternal()` and `resolveExternalUrl()` methods), `QuestionnaireField`, `QuestionnaireInstance`, `QuestionnaireFieldValue`
- Package: `net.superiorstate.ams.model.activity.questionnaire`
- Seed JSON: `src/main/resources/questionnaire/questionnaire_seeds.json` (3 Jotform-derived templates + generic starters)
- Seed loader in `DatabaseInitializer` reads JSON, checks `template_key` for duplicates, creates native or external questionnaires
- **Deliverable:** Migration script, 4 entity classes, seed JSON, DatabaseInitializer update

### Phase 2: Admin UI
- New "Questionnaire" tab in ServiceManager25
- Questionnaire CRUD (create, edit, suppress) with **mode toggle** (Native Form / External URL)
- External URL input with merge token documentation (`{erName}`, `{activityId}`, `{instanceGuid}`)
- Field CRUD within a questionnaire (create, edit, reorder, suppress) — only shown for native-mode questionnaires
- LOS/Enhancement/ServiceItem scoping checkboxes
- Activity type dropdown
- **Deliverable:** ServiceManagerHome update, serviceManager25.jsp tab, ServiceManagerAction cases

### Phase 3: Public Form Servlet
- `FillQuestionnaire` servlet — GET renders native form OR redirects to external URL; POST submits native form
- `SaveQuestionnaireProgress` servlet (AJAX auto-save, native mode only)
- `fillQuestionnaire.jsp` — public form page using shared field renderer (native mode)
- `questionnaireConfirmation.jsp` — submission confirmation (native mode)
- Extract `fieldRenderer.jsp` from `applyForProposal.jsp`
- Retrofit `applyForProposal.jsp` and `reviewApplication.jsp` to use shared renderer
- **Deliverable:** 2 servlets, 3 JSPs, 1 shared include, 2 JSP refactors

### Phase 4: Auto-Attach on Activity Creation
- Update `TaskBuilder25` to check scoping rules and create instances for Setups
- Update checklist creation for Renewals (wherever renewals are generated)
- Update ticket creation path if ticket-scoped questionnaires are desired
- **Deliverable:** Servlet updates with auto-attach logic

### Phase 5: Activity Detail Integration
- Questionnaire status panel on activity detail page (both native and external, with mode badge)
- `ViewQuestionnaireResponse` servlet for PSP-side answer review (native mode)
- `QuestionnaireAction` servlet (reopen, review, attach, detach, markComplete, markIncomplete)
- "Copy Link" button — copies AMS GUID URL (native) or resolved external URL (external)
- "Open Form" button for external-mode questionnaires (opens in new tab)
- "Mark Complete" / "Mark Incomplete" for external-mode manual status management
- **Deliverable:** Activity detail JSP update, 2 servlets, response viewer JSP

### Phase 6: Automation Email Token
- `SendAutoFinal25` processes `<q>questionnaire_name</q>` tokens
- Replaces with public URL to matching instance
- Add to XML Help reference cards in task manager
- **Deliverable:** SendAutoFinal25 update, taskManager25.jsp help card update

### Phase 7: Completion Gating (Optional)
- Optional `todo_id` linkage on `QuestionnaireInstance`
- Task completion check: if linked questionnaire not SUBMITTED/REVIEWED, warn or block
- Configuration flag on the questionnaire template or the task
- **Deliverable:** ToDo completion logic update, UI warning/block

---

## 12. Migration Script Outline (V038)

```sql
-- V038__questionnaire_system.sql
-- Prerequisite: V037

-- 1. questionnaire (template)
CREATE TABLE questionnaire (
    questionnaire_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    activity_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    external_url VARCHAR(500),
    sort_order INT DEFAULT 0,
    suppressed TINYINT DEFAULT 0,
    template_key VARCHAR(50),
    CONSTRAINT fk_questionnaire_psp FOREIGN KEY (psp_id) REFERENCES assignee(id),
    UNIQUE INDEX uq_questionnaire_template (template_key, psp_id)
);

-- 2. questionnaire_field
CREATE TABLE questionnaire_field (
    field_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    questionnaire_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    label VARCHAR(200),
    field_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    select_options VARCHAR(500),
    help_text VARCHAR(500),
    is_required TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    suppressed TINYINT DEFAULT 0,
    CONSTRAINT fk_qfield_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire(questionnaire_id),
    UNIQUE INDEX uq_qfield_key (questionnaire_id, field_key)
);

-- 3. questionnaire_instance
CREATE TABLE questionnaire_instance (
    instance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    questionnaire_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    todo_id BIGINT,
    instance_guid VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    submitted_by_name VARCHAR(100),
    submitted_by_email VARCHAR(200),
    date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_submitted TIMESTAMP NULL,
    date_reviewed TIMESTAMP NULL,
    reviewed_by_id BIGINT,
    date_reopened TIMESTAMP NULL,
    reopened_by_id BIGINT,
    CONSTRAINT fk_qinst_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire(questionnaire_id),
    CONSTRAINT fk_qinst_activity FOREIGN KEY (activity_id) REFERENCES assignee(id) ON DELETE CASCADE,
    CONSTRAINT fk_qinst_todo FOREIGN KEY (todo_id) REFERENCES todo(id) ON DELETE SET NULL,
    CONSTRAINT fk_qinst_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES assignee(id),
    CONSTRAINT fk_qinst_reopener FOREIGN KEY (reopened_by_id) REFERENCES assignee(id),
    UNIQUE INDEX uq_qinst_guid (instance_guid),
    UNIQUE INDEX uq_qinst_activity_questionnaire (questionnaire_id, activity_id)
);

-- 4. questionnaire_field_value
CREATE TABLE questionnaire_field_value (
    field_value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    field_value TEXT,
    CONSTRAINT fk_qfv_instance FOREIGN KEY (instance_id) REFERENCES questionnaire_instance(instance_id) ON DELETE CASCADE,
    CONSTRAINT fk_qfv_field FOREIGN KEY (field_id) REFERENCES questionnaire_field(field_id),
    UNIQUE INDEX uq_qfv_instance_field (instance_id, field_id)
);

-- 5. Scoping join tables
CREATE TABLE questionnaire_los (
    questionnaire_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (questionnaire_id, los_id),
    CONSTRAINT fk_qlos_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire(questionnaire_id),
    CONSTRAINT fk_qlos_los FOREIGN KEY (los_id) REFERENCES los(los_id)
);

CREATE TABLE questionnaire_enhancement (
    questionnaire_id BIGINT NOT NULL,
    enhancement_id BIGINT NOT NULL,
    PRIMARY KEY (questionnaire_id, enhancement_id),
    CONSTRAINT fk_qenh_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire(questionnaire_id),
    CONSTRAINT fk_qenh_enhancement FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id)
);

CREATE TABLE questionnaire_serviceitem (
    questionnaire_id BIGINT NOT NULL,
    purpose_id INT NOT NULL,
    PRIMARY KEY (questionnaire_id, purpose_id),
    CONSTRAINT fk_qsi_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES questionnaire(questionnaire_id),
    CONSTRAINT fk_qsi_serviceitem FOREIGN KEY (purpose_id) REFERENCES templatepurpose(purpose_id)
);

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V038', 'Questionnaire system: templates, fields, instances, values, scoping', 'V038__questionnaire_system.sql', NOW());
```

---

## 13. Entity Relationship Summary

```
PSP (1) ──────────────────── (M) Questionnaire
                                    │
                                    ├── external_url? ──▶ [External Form: Jotform, etc.]
                                    │                     (no fields/values — status tracked manually)
                                    │
                                    ├── (if native) ──▶ (1:M) QuestionnaireField
                                    │
                      ┌─────────────┼─────────────┐
                      │             │             │
                  (M:N) LOS    (M:N) Enh    (M:N) ServiceItem
                                    │
                                    └── (1:M) QuestionnaireInstance ──── (M:1) Activity (Setup/Renewal/Ticket)
                                                  │                  ──── (M:1) ToDo (optional gating)
                                                  │
                                                  └── (if native) (1:M) QuestionnaireFieldValue ──── (M:1) QuestionnaireField
```

---

## 14. Design Decisions (Resolved)

1. **Cascade delete — YES.** Questionnaire instances delete with the activity. If the Setup/Renewal is removed, the questionnaire responses lose their context. Implemented via `ON DELETE CASCADE` on `questionnaire_instance.activity_id` FK, which cascades through to `questionnaire_field_value` via `ON DELETE CASCADE` on `instance_id` FK.
2. **Email notification on submit — YES.** When an external person submits a questionnaire, the activity's assigned PSP user receives an email notification via existing `EmailDAO.sendEmail()` pattern. Uses `EmailTemplate.wrapBodyOnly()` for branded notification without auto-signature. Notification includes: questionnaire name, activity name, submitter name/email, and a link to the response viewer.
3. **No duplicate instances — ENFORCED.** Unique constraint on `(questionnaire_id, activity_id)` prevents multiple instances of the same questionnaire on one activity. PSP users must reopen the existing instance rather than creating a new one.
4. **BPO cross-system — YES, DESIRED.** The `instance_guid` and public servlet are system-agnostic by design. Cross-system sync of questionnaire templates and responses will be layered on via the existing REST API pattern (same approach as task delegation and note sync). This is a future phase after the core questionnaire system is operational.
5. **Dual-mode: Native + External — YES.** Each questionnaire template has an optional `external_url` field. If NULL, the questionnaire uses native AMS fields (QuestionnaireField/QuestionnaireFieldValue). If populated, the questionnaire is a managed pointer to an external form (Jotform, etc.) with manual status tracking by the PSP user. Both modes share the same scoping, activity attachment, automation email tokens, and status lifecycle. This lets complex forms with conditional logic, calculations, and multi-page tutorials stay in Jotform while simple data collection is handled natively.
6. **Conditional logic — FUTURE ITEM.** Native-mode questionnaires do not support conditional field visibility (show/hide based on other field values) in v1. Forms requiring conditional logic should use external mode (Jotform) until native conditional logic is implemented. This is the primary driver for the dual-mode design — the 125 Eligibility and 105 Participation forms have heavy conditional logic that would be impractical to rebuild natively without a conditional rendering engine.

---

## 15. References

- `docs/analysis/bpo_cross_system_brainstorm.md` §8B — original BPO questionnaire concept
- `docs/analysis/activity_detail_transition_plan.md` §4.2 — Track B2 phase list
- `src/main/java/net/superiorstate/ams/model/sales/application/` — Application entity pattern
- `src/main/java/net/superiorstate/ams/controller/activity/setup/ApplyForProposal.java` — public form servlet pattern
- `src/main/java/net/superiorstate/ams/controller/activity/setup/SaveApplicationProgress.java` — AJAX auto-save pattern
- `src/main/java/net/superiorstate/ams/controller/activity/setup/ServiceManagerAction.java` — admin CRUD pattern
- `src/main/webapp/WEB-INF/view/sales/applyForProposal.jsp` — field rendering to extract
