# Outlook Add-in — Session 78 Handoff Prompt

Copy everything below the line into a new Claude Code session on your other workstation.

---

## Context: Outlook Web Add-in "Log to AMS" — Session 78 Continuation

### What was built in Session 77

We built a complete Outlook Web Add-in ("Log to AMS") that adds a button to the Outlook reading pane. When clicked, a taskpane opens and lets the user:

1. **Auto-authenticate** via their Microsoft 365 email (mapped to an AMS Person via `outlook_user_link` table, V060 migration)
2. **Search open activities** by employer name (typeahead, 300ms debounce, hits `GET /api/v1/outlook/activities?q=...`)
3. **Log the email as a Note** on the selected activity, with attachments uploaded to Wasabi S3 and linked via WebLink records (`POST /api/v1/outlook/log-email`)

### Current deployment status

- **V060 migration** applied to local dev and production (`beta_ssa`)
- **WAR** deployed to production (`superiorstate.biz`)
- **Add-in** deployed org-wide via M365 Admin Center → Integrated Apps
- **Verified working:** The "Log to AMS" button appears in Outlook reading pane, authentication succeeds, activity search returns results
- **Not yet tested:** Full end-to-end log-email flow with attachments (was waiting for M365 propagation at end of Session 77)
- **V060 FK bug found and fixed:** Original migration referenced `person(person_id)` but Person uses SINGLE_TABLE inheritance in the `assignee` table. Fixed to `assignee(id)`. Both DBs were patched by hand, corrected migration committed as `6f03291`.

### Files created in Session 77

**API endpoints** (`src/main/java/net/superiorstate/ams/controller/api/outlook/`):
- `OutlookApiHelper.java` — shared token validation + JSON helpers
- `OutlookAuthApi.java` — `POST /api/v1/outlook/authenticate`
- `OutlookActivitiesApi.java` — `GET /api/v1/outlook/activities`
- `OutlookLogEmailApi.java` — `POST /api/v1/outlook/log-email`

**Admin page:**
- `OutlookLinkManager.java` (`controller/user/`) — PSP Admin CRUD for linking M365 emails
- `outlookLinkManager.jsp` (`WEB-INF/view/user/`)

**Entity + model changes:**
- `OutlookUserLink.java` (`model/general/`) — JPA entity for `outlook_user_link`
- `Note.java` — added `@OneToMany(mappedBy="note") List<WebLink> webLinkList`
- `WebLink.java` — added `@ManyToOne @JoinColumn(name="note_id") Note note`

**Static add-in files** (`src/main/webapp/outlook/`):
- `manifest.xml` — Office Add-in manifest (Mailbox 1.5+, MessageReadCommandSurface)
- `taskpane.html` — single-page UI (Office.js + Bootstrap 5)
- `icon-16.png`, `icon-32.png`, `icon-80.png` — brand-matched navy tiles
- `README.md` — icon specs + sideload instructions

**Filter changes:**
- `ApiTokenFilter.java` — bypass for `/api/v1/outlook/`
- `LoginFilter.java` (root package `net.superiorstate.ams`) — `/outlook/` added to allowed paths

**Migration:**
- `docs/migrations/V060__outlook_user_link.sql` — `outlook_user_link` table + `weblink.note_id` FK

### Key technical details

- **Auth model:** per-user `api_token` (64 hex chars) on `outlook_user_link`, sent as `Authorization: Bearer {token}`. No session cookies. Token stored in browser `localStorage`.
- **ActivityStatus IDs:** 1=Waiting on Them, 2=No Change, 3=Waiting on Us (corrected from earlier memory)
- **ReasonCreated IDs:** 4="Received Email" (used by OutlookLogEmailApi)
- **Person inheritance:** Person extends Assignee (SINGLE_TABLE). All Person rows live in `assignee` table with PK column `id`. FKs must reference `assignee(id)`, not `person(person_id)`.
- **Activity25 view:** `a25_activity_list_open`, fields: activity (FK), dType, name (employer), assignedTo, dueDate, waitingOnUs, daysSinceContact. No service_item column.

---

## Session 78 Task: Add "Create Ticket from Email" to the Outlook Add-in

### Objective

Extend the existing Outlook taskpane so the user can, in addition to logging an email to an existing activity, **create a brand-new Ticket** directly from the email. This is the natural next step — users often receive emails that need a new support ticket, not just a note on an existing one.

### Requirements

**Phase 1: Read existing Ticket creation patterns**

Read these files (do NOT modify) to understand how Tickets are created in AMS:

1. `src/main/java/net/superiorstate/ams/model/activity/ticket/Ticket.java` — Ticket entity fields
2. `src/main/java/net/superiorstate/ams/model/activity/ticket/TicketCategory.java` — categories for the ticket
3. Any existing servlet that creates Tickets (search for `new Ticket()` or `em.persist` with Ticket) — understand what fields are required vs optional
4. `src/main/java/net/superiorstate/ams/model/general/Assignee.java` — the `fullName` field that Activities inherit
5. Review the existing `OutlookLogEmailApi.java` to understand the current note-creation flow

Report findings before making any changes. Key questions to answer:
- What fields are required on a new Ticket? (category, contact, employer, etc.)
- What TicketCategory values exist in the seed data?
- How is `fullName` (from Assignee) composed for a Ticket?
- What is the minimum viable Ticket that can be created from just an email's metadata?

**Phase 2: New API endpoint**

Create `OutlookCreateTicketApi.java` at `POST /api/v1/outlook/create-ticket`:
- Accepts multipart form data (like log-email): subject, body, sender info, attachments
- Also accepts: ticketCategoryId (required), optional employerName or employerId
- Creates a Ticket with the email content as the first Note
- Uploads any attachments to Wasabi, links via WebLink → Note
- Returns `{ "success": true, "activityId": 123 }`

**Phase 3: Update taskpane UI**

Add a tab or toggle to `taskpane.html`:
- **Tab 1: "Log to Activity"** — current flow (search existing activity, log note)
- **Tab 2: "Create Ticket"** — new flow:
  - Ticket Category dropdown (fetched from a new API endpoint or hardcoded from seed data)
  - Optional: employer/contact lookup
  - Subject auto-filled from email subject
  - "Create Ticket" button
  - Same attachment handling as existing flow

**Phase 4: Supporting API (if needed)**

If the ticket category list needs to come from the database, create:
- `GET /api/v1/outlook/ticket-categories` — returns `[{id, name}]`

### Developer preferences (from CLAUDE.md)

- **One step at a time** — don't list multiple steps; confirm completion before the next
- **Produce full downloadable files** — not partial snippets. Include full file path.
- **Check existing files first** before asking the developer to paste code
- End every response with: **Next action: ...**
