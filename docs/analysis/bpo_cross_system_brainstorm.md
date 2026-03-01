# PSP ↔ BPO Cross-System Communication — Brainstorm & Process Plan

> **Purpose:** Flesh out the full process and communication architecture for how three separate AMS distributions (Super Admin, PSP, BPO) coordinate vendor relationships and task delegation across independent VPS deployments.
>
> **Status:** Brainstorm — not a build plan yet. Establishing the "who does what and when" before the "how."
>
> **Date:** March 1, 2026

---

## 1. The Three Systems

Each of these is its own VPS running its own AMS instance with its own database:

| System | Operator | URL Example | Purpose |
|--------|----------|-------------|---------|
| **Super Admin** | You (Kevin / SSA) | `superiorstate.biz` | Master registry. Manages approved BPO list, PSP fleet health, releases. |
| **PSP** | Client company (e.g., Acme Benefits) | `acmebenefits.com` | Day-to-day benefits admin. Creates activities, checklists, tasks. May outsource tasks to BPOs. |
| **BPO** | Vendor company (e.g., Accelergent) | `accelergent.com` | Receives delegated tasks from one or more PSPs. Completes work, communicates status back. |

**Key principle:** No system has direct database access to another. All cross-system communication is via authenticated REST API calls between the systems.

---

## 2. Process Flow — Who Does What and When

### Phase A: BPO Approval (Super Admin → All PSPs)

This establishes which BPOs exist and are authorized for use.

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| A1 | **Super Admin** | Provisions a BPO VPS (clone master, configure, initialize with BPO deployment key) | BPO system exists and is operational |
| A2 | **Super Admin** | Adds BPO to the master approved-vendor registry (name, URL, status=approved) | Super Admin DB: new row in `approved_vendors` |
| A3 | **Super Admin** | Triggers distribution (or waits for nightly sync) | All PSP systems receive updated vendor list |
| A4 | **Each PSP** | Nightly sync job (or on-demand) pulls the approved vendor list from Super Admin | PSP DB: `bpo_registration` table updated — new BPO rows inserted with `is_approved=true`, `is_requested=false`, `is_accepted=false` |
| A5 | **Each PSP** | PSP Admin sees updated vendor list in their Vendor Management page | Read-only list of available BPOs — can't use them yet |

**Suppression/Removal:** If Super Admin deactivates a BPO (step A2 in reverse), the next sync (A4) marks that BPO as `is_approved=false` on all PSP systems. Any PSP that had an active relationship with that BPO would see a visual warning but existing in-flight tasks aren't yanked mid-stream — they can complete but no new tasks can be assigned.

### Phase B: PSP Requests BPO Relationship (PSP → BPO)

A PSP decides they want to use a specific BPO.

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| B1 | **PSP Admin** | From Vendor Management page, clicks "Request Partnership" on an approved BPO | PSP DB: `bpo_registration` row updated → `is_requested=true` |
| B2 | **PSP System** | Sends API request to the BPO's URL: "PSP XYZ is requesting a vendor relationship" | BPO receives: PSP name, PSP URL, PSP contact info |
| B3 | **BPO System** | Logs the inbound request | BPO DB: new row in `psp_clients` (or equivalent) with `status=PENDING` |
| B4 | **BPO Admin** | Sees pending PSP request in their admin dashboard. Reviews and approves (or rejects). | BPO DB: `psp_clients` row → `status=APPROVED` |
| B5 | **BPO System** | Sends API callback to the PSP's URL: "Your request has been approved" | PSP receives: confirmation + BPO's API credentials/token |
| B6 | **PSP System** | Updates local record | PSP DB: `bpo_registration` row → `is_accepted=true` |
| B7 | **PSP Admin** | BPO now appears in the ManageTask25 vendor sourcing dropdown | Tasks can now be configured to source work to this BPO |

**Rejection path:** If B4 is a rejection, B5 sends a rejection callback, and the PSP row stays at `is_requested=true`, `is_accepted=false`. PSP Admin sees "Request Denied" status. They could re-request later.

**PSP disconnects:** PSP Admin can revoke the relationship at any time (sets `is_accepted=false` locally, notifies BPO). Existing in-flight tasks should complete but no new assignments. The BPO can also initiate disconnect from their side.

### Phase C: Task Configuration (PSP Internal — No Cross-System)

This is the existing ManageTask25 flow, no changes needed to the process:

| Step | Actor | Action |
|------|-------|--------|
| C1 | **PSP Admin** | In Sequence Builder, configures a task's vendor sourcing: Internal / Source but Verify / Vendor Only |
| C2 | **PSP Admin** | Selects which approved+accepted BPO handles this task (from dropdown) |
| C3 | **PSP System** | `task.bpo_registration_id` is set, `task.isSourced=true` |

No API call needed here — this is just configuring what will happen when ToDos are generated from this task.

### Phase D: ToDo Assignment (PSP → BPO)

When an activity's checklist is created, ToDos are generated from tasks. Sourced ToDos need to be communicated to the BPO.

**Option 1 — Auto-accept (for trusted BPO-PSP relationships):**

When the BPO has set `auto_accept_tasks=true` for this PSP client:

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| D1 | **PSP System** | Checklist generated → ToDo created from a sourced task | PSP DB: ToDo exists with `todo_guid`, linked to `bpo_registration_id` |
| D2 | **PSP System** | API call to BPO: "New task assignment" | Sends: `todo_guid`, task name, due date, goTo link, info link, activity type, activity name, employer name |
| D3 | **BPO System** | Auto-accepts and stores | BPO DB: new `delegated_todo` row, `status=ACTIVE` |
| D4 | **BPO System** | Sends acknowledgment to PSP | PSP could show "BPO received" status |
| D5 | **BPO Admin** | Sees new unassigned task in dashboard. Assigns to a BPO User. | BPO DB: `delegated_todo.assigned_to` set |

**Option 2 — Request/Accept (for new or less-trusted relationships):**

When the BPO has `auto_accept_tasks=false` for this PSP client:

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| D1 | **PSP System** | Checklist generated → ToDo created from sourced task | PSP DB: ToDo with `todo_guid`, status = `PENDING_BPO_ACCEPTANCE` |
| D2 | **PSP System** | API call to BPO: "Task assignment request" | BPO receives request |
| D3 | **BPO Admin** | Sees inbound task request in pending queue. Can accept or decline. | |
| D4a | **BPO Admin accepts** | BPO sends acceptance callback to PSP | PSP: ToDo status → active. BPO: `delegated_todo` created, `status=ACTIVE` |
| D4b | **BPO Admin declines** | BPO sends decline callback to PSP | PSP: ToDo reverts to internal (unlocked, no BPO assignment) |
| D5 | **BPO Admin** | Assigns accepted task to a specific BPO User | BPO DB: `delegated_todo.assigned_to` set |

**Decision:** Both modes supported. BPO Admin sets `auto_accept_tasks` per PSP client. Defaults to `false` for new relationships.

### Phase E: Task Completion (BPO → PSP)

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| E1 | **BPO User** | Works the task (uses goTo link, follows info link instructions) | External work |
| E2 | **BPO User** | Marks task complete in BPO dashboard (optionally adds a note) | BPO DB: `delegated_todo.completed=true`, note added |
| E3 | **BPO System** | API call to PSP: "ToDo {guid} completed by {user} at {timestamp}" with optional note text | PSP receives completion event |
| E4 | **PSP System** | Updates local ToDo based on task sourcing mode: | |
| | | — **Vendor Only:** `bpo_completed=true` AND `is_complete=true` → Done | Task fully closed |
| | | — **Source but Verify:** `bpo_completed=true`, `is_complete=false` → Unlocked for PSP review | PSP User can now verify |
| E5 | **PSP User** | (Source but Verify only) Reviews BPO's work, checks off or reverts | |

### Phase F: Revert (PSP → BPO)

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| F1 | **PSP User** | Clicks "Revert to BPO" on a task that BPO completed | PSP DB: `bpo_completed=false`, `is_reverted=true` |
| F2 | **PSP System** | API call to BPO: "ToDo {guid} reverted — please redo" with optional note | BPO receives revert event |
| F3 | **BPO System** | Re-opens the delegated_todo, adds revert note | BPO DB: `delegated_todo.completed=false`, note added |
| F4 | **BPO User** | Sees reverted task back in their queue | Back to E1 |

### Phase G: PSP Turns Off Sourcing (PSP → BPO)

| Step | Actor | Action | System Effect |
|------|-------|--------|---------------|
| G1 | **PSP Admin** | Changes task from Sourced/Vendor Only back to Internal | PSP DB: `task.isSourced=false`, `task.bpo_registration_id=null` |
| G2 | **PSP System** | Any existing in-flight ToDos for that task: option to recall or let complete | Decision point (see below) |
| G3 | **PSP System** | API call to BPO: "Recall ToDo {guid}" (if recalling) or "No new assignments for task {X}" | BPO cleans up |

**In-flight decision:** When a PSP turns off sourcing, what happens to ToDos already sent to BPO?
- **Option A:** Let them finish — BPO completes what they have, but gets no new ones. Cleanest.
- **Option B:** Recall immediately — API tells BPO to drop them, PSP unlocks locally. More disruptive.
- **Recommendation:** Option A (let them finish) as default, with a per-ToDo "recall" button for urgent cases.

---

## 3. Communication Architecture — How the Systems Talk

### API Design Principles

1. **Authenticated:** Every cross-system API call includes an API key or token exchanged during Phase B (partnership approval).
2. **Idempotent:** Every call can be safely retried. Use `todo_guid` as the natural idempotency key.
3. **Asynchronous-friendly:** The sender doesn't block waiting for the receiver to process. Fire and queue.
4. **Minimal payload:** Only send what the receiver needs. BPO never gets full activity details — just task name, due date, links, and notes.

### API Endpoints

Each system exposes a small set of REST endpoints that the others call:

#### Super Admin exposes (called by PSPs):
```
GET  /api/v1/approved-vendors          → Returns list of approved BPOs (name, URL, status)
```

#### PSP exposes (called by BPO and Super Admin):
```
POST /api/v1/vendor/accept             → BPO accepts partnership request
POST /api/v1/vendor/reject             → BPO rejects partnership request
POST /api/v1/vendor/disconnect         → BPO initiates disconnect
POST /api/v1/todo/{guid}/completed     → BPO reports task completion
POST /api/v1/todo/{guid}/accepted      → BPO accepts task assignment (non-auto-accept mode)
POST /api/v1/todo/{guid}/declined      → BPO declines task assignment (non-auto-accept mode)
GET  /api/v1/health                    → Super Admin health check polling
```

#### BPO exposes (called by PSPs):
```
POST /api/v1/client/request            → PSP requests partnership
POST /api/v1/todo/assign               → PSP sends new task assignment
POST /api/v1/todo/{guid}/revert        → PSP reverts a completed task
POST /api/v1/todo/{guid}/recall        → PSP recalls a task (turns off sourcing)
POST /api/v1/todo/{guid}/note          → PSP adds a note (stored at BPO, source_type=PSP)
GET  /api/v1/todo/{guid}/notes         → PSP pulls note history on demand for display
POST /api/v1/todo/{guid}/update        → PSP updates task details (due date change, etc.)
```

### Authentication Between Systems

During Phase B (partnership approval), the two systems exchange API credentials:

- **PSP → BPO:** PSP generates a unique API token for this BPO, stores it in `bpo_registration.api_token_outbound`. Sends it to BPO in the partnership request.
- **BPO → PSP:** BPO generates a unique API token for this PSP, stores it in `psp_clients.api_token_outbound`. Sends it in the acceptance callback.

Every subsequent API call includes the token in an `Authorization: Bearer {token}` header. The receiving system validates the token against its stored credentials.

### Sync Strategy

Two approaches, not mutually exclusive:

#### Event-Driven (Primary)
- When something happens (task created, completed, reverted), the originating system immediately fires an API call to the other.
- Fast, responsive. User sees near-real-time updates.
- Risk: if the other system is down, the event is lost.

#### Interval-Based Reconciliation (Safety Net)
- Every N minutes (configurable, e.g., 5–15 min), each system polls the other for any missed updates.
- Catches anything dropped by event-driven calls (network blip, downtime, etc.).
- Heavier on bandwidth but guarantees eventual consistency.

**Recommendation:** Event-driven primary + interval reconciliation as backup. The interval job can be lighter if it only checks a "last_sync_timestamp" and pulls changes since then.

### Failure Handling

| Failure | Handling |
|---------|----------|
| BPO system is down when PSP sends task | Queue locally, retry on interval. Task shows "Pending Delivery" on PSP side. |
| PSP system is down when BPO completes task | BPO queues completion event, retries. Task stays complete on BPO side. |
| API token invalid/expired | Return 401. Sending system alerts admin. Relationship status → "Auth Error". |
| BPO removed from approved list while tasks in flight | Existing tasks complete normally. No new assignments. PSP sees warning banner. |

---

## 4. Data Each System Stores

### Super Admin Database
```
approved_vendors
├── vendor_id (PK)
├── name
├── url
├── contact_email
├── is_active
├── date_added
└── date_deactivated
```
This is the **source of truth** for which BPOs are authorized to exist in the ecosystem.

### PSP Database (existing + additions)
```
bpo_registration (already exists — V016, V027)
├── bpo_reg_id (PK)
├── psp_id (FK)
├── bpo_name
├── bpo_url
├── is_active
├── is_approved          ← synced from Super Admin
├── is_requested         ← PSP initiated request
├── is_accepted          ← BPO approved the request
├── api_token_outbound   ← NEW: token PSP sends to BPO
├── api_token_inbound    ← NEW: token BPO sends to PSP
├── auto_accept_tasks    ← NEW: if true, skip D4 acceptance step
├── date_registered
├── date_requested       ← NEW
├── date_accepted        ← NEW
└── date_disconnected    ← NEW
```

ToDos already have `todo_guid` and `bpo_completed` fields (V011). No structural changes needed for the task sync — just the API layer.

### BPO Database
```
psp_clients (new table)
├── client_id (PK)
├── psp_name
├── psp_url
├── api_token_outbound    ← token BPO sends to PSP
├── api_token_inbound     ← token PSP sends to BPO
├── status                ← PENDING / APPROVED / REJECTED / DISCONNECTED
├── auto_accept_tasks     ← BPO admin configurable per PSP
├── date_requested
├── date_approved
└── date_disconnected

delegated_todo (new table)
├── delegated_id (PK)
├── todo_guid (unique — matches PSP's todo_guid)
├── psp_client_id (FK → psp_clients)
├── task_name
├── task_description
├── due_date
├── goto_link
├── info_link
├── activity_type         ← Renewal / Setup / Ticket / CheckList
├── activity_name
├── employer_name         ← included per Q2 decision
├── assigned_to_id (FK → person, nullable)  ← which BPO user
├── is_completed
├── completed_by_id (FK → person, nullable)
├── completed_date
├── is_reverted
├── last_sync_timestamp
└── status                ← PENDING_ACCEPTANCE / ACTIVE / COMPLETED / REVERTED / RECALLED

todo_note (already exists from V011 — authoritative for delegated tasks)
├── note_id (PK)
├── todo_id (FK → delegated_todo.delegated_id for cross-system; FK → todo for local)
├── todo_guid             ← NEW: correlation key for cross-system note lookups
├── created_by_id (FK → person)
├── created_date
├── note_text
└── source_type           ← 'PSP' or 'BPO'
```

**Note on `todo_note` for cross-system:** The BPO's `todo_note` table is the single source of truth for all notes on delegated tasks. PSP-originated notes (e.g., revert reasons) are pushed to BPO via API and stored with `source_type=PSP`. PSP pulls the full note history on demand via `GET /api/v1/todo/{guid}/notes`. The PSP's own `todo_note` table is only used for notes on non-delegated (internal) tasks.

---

## 5. Design Decisions (Finalized)

### Q1: Auto-accept vs. Request/Accept for task assignments?
**Decision:** Hybrid — BPO Admin can set `auto_accept_tasks` per PSP client. When enabled, new task assignments are automatically accepted and appear in the BPO dashboard immediately. When disabled, each inbound assignment requires BPO Admin approval before it's visible to BPO Users. This gives established BPO-PSP relationships zero-friction flow while letting the BPO maintain control over new or less-trusted clients.

### Q2: What exactly does the BPO see?
**Decision:** The current BPO (Datapath's service company) already sees all this information in Summit today, so the baseline should include employer name, activity name, and task context. However, this should be configurable by BPO "type" — a future column on the BPO registration or a BPO-level setting that controls payload scope. For now, the standard payload includes: task name, activity type, activity name (e.g., "Acme Corp 2026 Renewal"), employer name, due date, goTo link, info link. Employee-level PII is excluded by default but could be an opt-in field for BPO types that need it.

### Q3: Who generates the todo_guid?
**Decision:** PSP generates it on ToDo creation (`@PrePersist` UUID). This is already implemented. The PSP is the source of truth for the ToDo. The BPO stores the same GUID in `delegated_todo.todo_guid` and uses it as the correlation key for all communication.

### Q4: Note synchronization — who stores notes?
**Decision:** BPO owns and stores all notes. PSP pulls on demand via API.

**Rationale:** The BPO generates far more notes than the PSP during daily task work — internal progress updates, partial completion notes, coordination between BPO users. Storing these at the PSP would mean every BPO note triggers an API call. Instead:

- **BPO stores all notes** in their `todo_note` table (which already has `source_type` = PSP or BPO).
- **When PSP user views a delegated ToDo's notes**, the UI makes a live API call to the BPO: `GET /api/v1/todo/{guid}/notes` → returns JSON array of notes.
- **When PSP adds a note** (e.g., on revert), it pushes to BPO via API: `POST /api/v1/todo/{guid}/note` → BPO stores it with `source_type=PSP`.
- **BPO has the complete history locally** — fast for their daily workflow, no dependency on PSP being up.
- **PSP sees notes on-demand** — slight latency on first load but acceptable. If BPO is temporarily down, PSP sees a "notes unavailable" message rather than stale data.

This model means the BPO's `todo_note` table is the single source of truth for all communication about a delegated task. The PSP's `todo_note` table is only used for notes on non-delegated (internal) tasks.

### Q5: Timing of Super Admin → PSP vendor list sync?
**Decision:** Nightly sync as default, piggybacking on the existing `update.sh` cron infrastructure. The vendor list changes infrequently (new BPO onboarded, BPO deactivated), so 24-hour latency is acceptable. If a same-day need arises in practice, we can add an on-demand pull later, but start simple.

### Q6: What happens to the existing single-instance demo model?
**Decision:** Continues unchanged. The single-instance model (PSP and BPO users on same system) is the development/demo mode indefinitely. `bpo_registration` rows point to the system's own URL. API calls become local HTTP calls to itself. No special-casing needed — the API layer works identically whether the target is localhost or a remote VPS.

### Q7: API framework?
**Decision:** Jakarta Servlets. New `@WebServlet` endpoints under `/api/v1/` path prefix, returning JSON. Consistent with the entire existing codebase. The API surface is small (< 15 endpoints total across all three systems) — no need for JAX-RS or any additional framework dependency.

### Q8: Security hardening for API endpoints?
**Decision:** Token auth + HTTPS. API endpoints excluded from `LoginFilter` (authenticate via `Authorization: Bearer` header, not session). HTTPS is already enforced on all VPSes. Rate limiting and IP allowlisting deferred — not necessary between known systems initially, can add later if warranted.

---

## 6. Implementation Phases (Suggested)

This is a rough sequencing, not a detailed build plan:

### Phase 1: Super Admin Vendor Registry + PSP Sync
- Build `approved_vendors` table and admin UI on Super Admin system
- Build the sync endpoint (`GET /api/v1/approved-vendors`)
- PSP update script (or new sync job) pulls and updates `bpo_registration.is_approved`
- PSP Vendor Management page shows approved vendors

### Phase 2: PSP ↔ BPO Partnership Flow
- PSP "Request Partnership" button → API call to BPO
- BPO "Pending Requests" admin page → approve/reject → callback to PSP
- Token exchange during approval
- PSP shows approved BPOs in ManageTask25 dropdown (already works for `is_accepted=true` rows)

### Phase 3: Task Assignment Sync (PSP → BPO)
- When sourced ToDo is created, PSP fires API to BPO
- BPO `delegated_todo` table receives and stores
- BPO dashboard shows cross-system tasks alongside local tasks
- BPO Admin can assign to BPO Users

### Phase 4: Task Completion Sync (BPO → PSP)
- BPO marks complete → API to PSP
- PSP updates `bpo_completed` (and `is_complete` for Vendor Only)
- Note sync — BPO sends notes via API, PSP stores

### Phase 5: Revert + Recall + Disconnect
- PSP revert → API to BPO
- PSP recall (turn off sourcing) → API to BPO
- PSP or BPO disconnect partnership → mutual notification
- Interval reconciliation job for missed events

---

## 7. What Already Exists (Foundation)

| Component | Status | Notes |
|-----------|--------|-------|
| `bpo_registration` table | ✅ V016 + V027 | Has `is_approved`, `is_requested`, `is_accepted`. Needs `api_token_*`, dates. |
| `todo_guid` on ToDo | ✅ V011 | UUID generated on creation. Ready for cross-system correlation. |
| `bpo_completed` / `bpo_completed_by` on ToDo | ✅ V011 | Completion tracking from BPO side. |
| `todo_note` table | ✅ V011 | Notes with `source_type` (PSP/BPO). |
| BPO auth routing (roles 101-103) | ✅ | BPO users land on BpoHome. |
| BPO dashboard (`BpoHome`, `bpoHome25.jsp`) | ✅ | Shows delegated tasks, notes, completion. |
| Task sourcing in ManageTask25 | ✅ V027 | `bpo_registration_id` on task, dropdown from active registrations. |
| Vendor sourcing 3-state toggle | ✅ | Internal / Source but Verify / Vendor Only. |
| ToDo completion workflow | ✅ | BPO complete, PSP verify, PSP revert. |
| Database views for BPO delegation | ✅ V027 | Full view chain carries `bpo_registration_id`. |
| Nightly update script on all VPSes | ✅ | Existing cron infrastructure for adding sync jobs. |
| `ssa.properties` config system | ✅ | Can store API tokens, system type (PSP/BPO/ADMIN), etc. |

**What's missing:** The API layer, token exchange, `psp_clients` table on BPO side, `approved_vendors` table on Super Admin side, sync jobs, and the cross-system partnership flow UI.

---

## 8. Enhancement: Structured BPO Task Responses

Two complementary ideas for enriching how BPOs report back on completed tasks, beyond plain-text notes.

---

### 8A: Note Attachments (File Uploads on Notes)

**The problem:** Today the BPO (e.g., Datapath's service company) emails a daily claims-entry report to the PSP as a spreadsheet attachment. This happens outside the system entirely. Many outsourced tasks will have a standard deliverable — a report, a completed form, a screenshot, a reconciliation file.

**The opportunity:** Let `todo_note` carry file attachments, so the BPO can attach the claims report directly to the completion note, and the PSP sees it inline when they review the task.

**What already exists:**
- `WebLink` entity stores attachment metadata (`linkPath` = UUID.extension, `plainText` = display name, `linkType` = 1 for uploaded file)
- `StorageDAO` handles Wasabi S3 upload + pre-signed URL generation
- `AddAttachment25` handles multipart file upload, creates `WebLink`, stores in Wasabi
- `Email extends Note` already has `webLinkList` (1:M WebLink) — so the concept of "note with attachments" is proven in the codebase
- `EmailTemplate` renders attachment pills with pre-signed download URLs

**What to build:**
- Add a `webLinkList` (1:M WebLink) relationship to `ToDoNote` entity (same pattern as `Email`)
- BPO dashboard's "Add Note" form gets a file upload option (reuse `AddAttachment25` pattern)
- Notes displayed on both BPO and PSP side render attachment pills (reuse attachment rendering pattern)
- For cross-system: when PSP pulls notes via `GET /api/v1/todo/{guid}/notes`, the response includes attachment metadata. Attachments are served via pre-signed URLs from the BPO's Wasabi bucket — PSP renders download links that point to the BPO's storage.

**API payload for notes with attachments:**
```json
{
  "notes": [
    {
      "noteId": 42,
      "todoGuid": "abc-123",
      "sourceType": "BPO",
      "createdBy": "Priya Sharma",
      "createdDate": "2026-03-01T14:30:00Z",
      "noteText": "Claims entry completed for March batch. 47 claims processed, 2 flagged for review.",
      "attachments": [
        {
          "displayName": "Acme_Claims_March_2026.xlsx",
          "downloadUrl": "https://s3.wasabisys.com/ams-file-storage/accelergent/abc123.xlsx?X-Amz-...",
          "expiresAt": "2026-03-08T14:30:00Z"
        }
      ]
    }
  ]
}
```

**Implementation effort:** Low — the attachment plumbing already exists. It's mostly wiring `ToDoNote` to `WebLink` the same way `Email` is, and adding a file input to the note form.

---

### 8B: Task Response Questionnaires (Structured Data Collection)

**The problem:** Some outsourced tasks need more than a file — they need structured answers. "How many claims were processed?" "Were any flagged?" "What was the total dollar amount?" The PSP and BPO need to agree on what information is expected back, and the BPO needs a form to fill out, not just a free-text note.

**The opportunity:** Reuse the ApplicationSection/ApplicationField dynamic form framework as a general-purpose "Questionnaire" system. A PSP defines a response template for a task (what fields do I need back?), and the BPO fills it in as part of completing the task.

**What already exists:**
- `ApplicationSection` → ordered set of `ApplicationField` entries (TEXT, TEXTAREA, NUMBER, DATE, SELECT, RADIO, BOOLEAN, CHECKBOX, JSON field types)
- `ApplicationFieldValue` stores the filled-in answers keyed to a specific application instance
- `applyForProposal.jsp` dynamically renders sections and fields with conditional logic
- `ServiceManagerAction` provides admin UI for creating/editing sections and fields
- The full pattern: define a template (sections + fields) → attach to a context (application) → collect answers (field values) → review answers

**Conceptual mapping to BPO tasks:**

| Sales Pipeline Concept | BPO Task Response Concept |
|------------------------|--------------------------|
| ApplicationSection | ResponseTemplate (set of fields expected back for a task type) |
| ApplicationField | ResponseField (individual data point — "claims processed", "total amount", etc.) |
| ApplicationFieldValue | ResponseValue (BPO's actual answers for a specific ToDo) |
| Application (context) | The ToDo itself (one set of answers per delegated task) |
| applyForProposal.jsp | BPO task completion form |
| ReviewApplication | PSP review of BPO's structured response |

**How it would work:**

1. **PSP Admin** configures a Response Template for a task type (e.g., "Claims Entry Response" with fields: Claims Processed (NUMBER), Claims Flagged (NUMBER), Total Dollar Amount (NUMBER), Notes (TEXTAREA), Report File (file attachment)).

2. **Template is linked to a Task** — when PSP sets up vendor sourcing in ManageTask25, they can optionally attach a response template. This tells the BPO "when you complete this task, fill out this form."

3. **Template is sent to BPO** along with the task assignment (Phase D). BPO stores the template definition locally.

4. **BPO User completes the task** — the "Mark Complete" flow presents the response form (rendered from the template fields). BPO fills in the structured data + optionally attaches files.

5. **Structured response is sent back to PSP** with the completion callback (Phase E). PSP stores the response values and displays them in the task review view.

6. **PSP User reviews** — for "Source but Verify" tasks, the PSP sees both the structured response data and any attachments, then decides to verify or revert.

**Generalization potential:** This isn't BPO-specific. The same Questionnaire/ResponseTemplate pattern could be used for:
- Client onboarding checklists (PSP asks employer to fill out a form as part of a setup task)
- Internal task responses (PSP user fills out a structured form when completing certain task types)
- Compliance documentation (attach structured responses to renewal tasks)

**Implementation approach — two options:**

**Option A: Reuse ApplicationSection/Field directly.** Add a `scope` value (e.g., `TASK_RESPONSE`) to ApplicationSection. Link sections to Tasks via a new join table. Reuse the rendering JSP pattern. Pros: zero new entities. Cons: ApplicationSection is currently tightly coupled to the sales pipeline (PSP-scoped, LOS-linked).

**Option B: Create a parallel Questionnaire framework.** New entities: `Questionnaire`, `QuestionnaireField`, `QuestionnaireResponse`, `QuestionnaireFieldValue`. Same field-type system but decoupled from sales. Pros: clean separation, can evolve independently. Cons: code duplication of the field rendering pattern.

**Recommendation:** Option B for clean architecture, but extract the shared field-type rendering into a reusable JSP tag or include that both the sales application and the questionnaire system use. This avoids duplication while keeping the domain models separate.

---

### 8A vs 8B — When to Use Each

| Scenario | Use |
|----------|-----|
| BPO needs to upload a report/file as deliverable | **8A** — Attach file to completion note |
| PSP needs specific data points back from BPO | **8B** — Response questionnaire |
| Both — BPO fills out a form AND attaches a file | **8A + 8B** — Questionnaire with a file-upload field type |
| Simple task, no structured response needed | Neither — plain completion note is sufficient |

Most tasks will start with just 8A (note + attachment). 8B is for the tasks where the PSP wants to standardize and quantify what comes back. Both can coexist on the same task.

---

### Implementation Priority

**8A (Note Attachments)** should come first — it's low effort (plumbing exists), immediately useful (replaces the emailed daily report), and works within the existing note infrastructure.

**8B (Task Response Questionnaires)** is a larger architectural piece that can be designed in parallel but built after the core cross-system sync is working. It's more valuable once multiple PSPs are using the system and want to standardize their BPO interactions.
