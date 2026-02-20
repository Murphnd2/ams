# Activity Management System — To-Be Vision Document

**Created:** February 18, 2026
**Target Milestone:** Datapath Client Conference — Philadelphia, PA — April 20–22, 2026
**Product Name:** Activity Management System (AMS)
**Domain Strategy:** PSP subdomain model (e.g., `superiorstate.activitymanagement.com`)

---

## Executive Summary

The Activity Management System is a companion platform for Datapath clients (Plan Service Providers / PSPs) that fills operational gaps in Datapath's software. It provides activity tracking, checklist-driven workflows, email communication, billing support, and sales tools that PSPs need daily but cannot efficiently perform in Datapath alone.

**Near-term positioning:** Complementary tool working alongside Datapath via export/import data exchange.
**Future positioning:** Deeper integration via direct API connections to Datapath data sets.

**Target buyers:** Benefits administration firms (PSPs) already on or adopting Datapath's platform. These firms share similar workflows, pain points, and limitations imposed by Datapath, making the platform highly transferable with minimal customization.

**Conference goal:** Live demo-ready. Core features should flow smoothly end-to-end and tell a compelling story about what AMS does for a PSP. Features should demonstrate capacity and direction, not perfection.

---

## Priority Tiers

| Tier | Description | Deadline |
|------|-------------|----------|
| **Conference** | Must be demo-ready by April 20, 2026 | April 2026 |
| **Personal/Business** | Useful to Superior State specifically; lower priority | Ongoing |
| **Post-Conference** | Growth and marketing efforts after the conference | Post-April 2026 |

---

## Conference-Tier Projects

### 1. Users and Roles Enhancement

**Current state:** Basic user/admin authentication with minimal role differentiation.

**Near-term roles (needed now):**

- **User** — General PSP staff member. Works activities, manages clients, performs daily tasks.
- **Admin** — Full access, system configuration, user management.
- **Accountant** — Scoped view primarily into billing and financial data. Mainly about what they can see, with potential future restrictions on what they can do.

**Future roles (built as companion features require them):**

- **Sales Agent** — Tied to an agency; accesses sales portal, creates proposals at agency-assigned rates.
- **Sales Agency Manager** — Oversight of agents within their agency.
- **Client Contact** — Very limited portal access (possible future). Could submit tickets/service requests. Not a near-term priority.
- **3rd Party Vendor User** — External vendor staff who access sourced tasks via their own AMS install.
- **3rd Party Vendor Admin** — Manages vendor-side configuration and PSP connections.

**Single Sign-On (SSO):**

- Implement as optional alongside existing username/password credentials ("Sign in with Microsoft" button on login page).
- First priority: Office 365 / Microsoft.
- Secondary: Google (depending on ease of development).
- Long-term: Active Directory, Datapath software sign-on.

**Design principle:** Architect the role structure for expansion but only build what is needed for current features. Don't over-engineer RBAC before the features that require it exist.

---

### 2. Sales Portal

**Overview:** A complete sales quoting and proposal system, migrated and enhanced from the superiorstate.net platform.

**Core Object Model:**

- **Line of Service (LOS)** — A core product sold by the PSP (e.g., FSA, HRA, HSA, COBRA, Direct Bill, Transit, MERP).
- **Module / Feature Add-on** — Optional extras with their own pricing, globally linked to eligible LOSs by the PSP (e.g., Payment Services, Debit Cards). Not all LOSs support all modules.
- **Rate** — A pricing tier. Agencies are assigned one or more rates in advance by the PSP. Rates can vary by group size, relationship, or other factors.
- **Rate Table** — Per Rate/LOS (or Rate/Module) combination. Contains one or more fees. If a LOS is not listed in a rate table, it is not selectable/saleable under that rate.
- **Fee Type** — A reusable catalog item describing how a fee is applied (e.g., Setup Fee, Annual Fee, Per Participant Per Month). Same name may have different amounts across rate/LOS combinations.
- **Features List** — Descriptive content per LOS or Module. Not pricing-related. Lists what the PSP brings to the table for a given service. Multiple per LOS. Used to populate proposal content. Could tie into the sales/marketing library.
- **Agency** — Assigned one or more rates by the PSP.
- **Sales Agent** — Belongs to an agency. Available rates are determined by agency assignment.
- **Prospect** — A potential client associated with an agent.
- **Proposal** — Generated for a prospect. Combines selected LOSs (constrained by rate table) with applicable modules and their pricing.

**Cascading selection logic:**

Rate selection → drives LOS availability → drives module availability → drives fee display → drives feature content display.

Everything flows down from the rate table choice. If a rate table doesn't include a LOS, the agent cannot select it. If a selected LOS has a linked module and the rate table has pricing for that module, the module pricing appears on the proposal. Features display for any LOS or module shown on the proposal.

**Fee structure:**

Each LOS or module can have one or more fees. A fee combines a fee type (reusable name/description) with a rate (dollar amount) specific to that rate table/LOS combination. Example: Standard FSA pricing has three fees — Setup Fee (one-time), Annual Fee (annual), PPPM Fee (monthly per participant).

**Proposal lifecycle:**

1. **Created** — Agent selects rate, chooses LOSs, system generates proposal with GUID.
2. **Sent** — Email sent to prospect containing a link to the proposal landing page.
3. **Viewed** — Prospect views pricing and feature details via the GUID URL (public-facing page).
4. **Apply** — Prospect clicks through to application page, scoped only to the proposed LOSs.
5. **Application submitted** — Prospect completes application for some or all proposed LOSs.
6. **Approved** — PSP reviews and approves the application.
7. **Setup created** — Application flows into a Setup activity in AMS with the appropriate checklist/template.

**Current bridge:** Application approval currently triggers an email to the PSP, who manually creates the setup via a servlet call with parameters. The to-be vision is a smoother automated handoff from application approval directly into setup activity creation.

---

### 3. Sales / Marketing Library

**Overview:** A centralized, organized repository of marketing and reference materials that can be used across email communications and proposals.

**Material types:**

- **Web links** — URLs to useful info, blogs, IRS sites, vendor resources.
- **PDFs** — Brochures, guides, compliance documents.
- **Videos** — Explainer videos, training content, promotional material.

**Organization:**

- Tagged to one or more LOSs (many-to-many). Some materials cross multiple LOSs (e.g., debit card info applies to FSA, HRA, HSA, Transit, MERP).
- Simple audience/purpose tag to distinguish sales-oriented (prospect-facing) from client-facing (how-to, educational) materials.
- No complex taxonomy needed initially — keep the structure right and let content grow organically over time.

**Storage:**

- Cloud object storage (Wasabi, S3-compatible) with GUID-based keys per document.
- PSP-agnostic: Every PSP's materials stay isolated by GUID even in a shared bucket. No collision risk regardless of how many PSPs are using the system.
- No dependency on individual PSP cloud storage capabilities.

**Usage points:**

- Referenced when composing manual emails.
- Inserted into automated email templates.
- Linked from proposal feature content.

**Philosophy:** Build the container now, fill it over time. The structure is the deliverable; content is an ongoing effort.

---

### 4. Recurring Tasks / Sequence Template Overhaul

**Overview:** Improve the GUI and usability of the sequence/template builder. Core concept is sound — execution needs to be more user-friendly and intuitive.

**Core concept (unchanged):**

Three activity types each have reusable task templates:

- **Setup templates** — Per LOS/module combination. Selling an FSA might require ~20 steps; adding debit cards adds ~10 more. Templates are additive based on what was sold.
- **Renewal templates** — Per benefit type. Annual process, ~20 steps each to ensure proper renewal handling for each benefit.
- **Ticket templates** — Per common service scenario (new services inquiry, login help, plan questions, etc.). Standardizes response procedures.

Templates are blueprints — when an activity is created, the relevant template's tasks are copied into a live checklist for that specific activity.

**Current state issues:**

- Two overlapping entry points (`SequenceHome` and `GoTicketTemplate25` → `sequenceBuilderForm.jsp`) that should be consolidated.
- `GoTicketTemplate25` is currently broken in production.
- Builder uses session-heavy state management and dropdown-based task selection — functional but clunky.
- Some legacy references (`ChecklistManagerGo`) still present in sequence JSPs.

**To-be goals:**

- Single, consolidated, intuitive builder interface.
- Clean support for all three activity types with their sub-categorization (ticket categories, LOS types, benefit types).
- Tasks within templates can be pre-configured with outsourcing flags, links (goTo and info), automation, and ownership.
- Some ticket templates may eventually be partially handled by a chatbot, but human workflow templates remain necessary.

---

### 5. Third-Party Vendor Task Outsourcing

**Overview:** Enable PSPs to outsource specific checklist tasks to third-party service vendors (such as Datapath's India-based service company) through a federated, system-to-system sync architecture.

**Architecture: Federated model — not shared multi-tenancy.**

Each party (PSP and vendor) runs their own AMS install. The systems communicate via API to sync task assignments and status.

**PSP-side configuration:**

- Register third-party vendors the PSP will use (vendor organization name + URL to their AMS install).
- Registered vendors appear in the sourcing dropdown when configuring tasks.
- Existing `Task` entity fields support this: `isSourced`, `sourceOwner`, `allowNonOwner`.

**Vendor-side configuration:**

- Register PSP clients to "look back" at (PSP name + URL to their AMS install).
- Consolidated dashboard shows sourced tasks across all connected PSP clients.
- View is similar to how internal non-owner staff see delegated tasks.

**Sync model:**

- Bidirectional, interval-based. Each system maintains its own copy of task data.
- PSP → Vendor: New sourced task assigned, task reopened, task details changed, activity closed/cancelled.
- Vendor → PSP: Task marked complete, timestamps, possibly vendor notes.
- Not real-time, but frequent enough to feel responsive.

**Access scoping:**

- Vendor sees task information but is restricted from other activity details.
- Task links are critical: the **goTo link** (website to perform the task) and **info link** (instructions/how-to guide) are how each PSP communicates their specific process for a generically-named task. The same task "by name" may be performed differently for each PSP.

**Existing foundation:**

- `Task` entity already has `isSourced` (boolean), `sourceOwner` (Person), `allowNonOwner` (boolean).
- UI in `taskManager25.jsp` supports three sourcing states: not sourced, sourced (anyone can complete), sourced (vendor only).
- `UpdateTask25` persists these flags.
- `TaskDAO` has queries to find tasks by source owner.
- Missing: Vendor organization concept, vendor login/portal, API sync layer, cross-system communication.

---

### 6. Email System Standardization

**Current state:** Two separate sending mechanisms:

- **Manual email (SendEmail25):** Uses Microsoft Graph API with Azure AD OAuth2.
- **Automation email (SendAutoFinal25):** Uses SMTP via EmailDAO, pulling SMTP2GO credentials from database constants.

**To-be approach — tiered email capability:**

- **Baseline (all PSPs):** SMTP as the standard sending method. Already configurable per PSP (credentials stored in DB constants table). Provider-agnostic — works with SMTP2GO, Gmail, or any SMTP service. Consolidate `SendEmail25` to also use the SMTP path.
- **Enhanced (optional):** Microsoft Graph API for PSPs who connect O365 via SSO. Enables inbox sync, reply tracking, read receipts, sending-as-user.
- **Future consideration:** If/when email replies need to be pulled back into the system (attaching a client's response to an activity), Graph becomes the natural path.

**Email settings** are already partially handled in `initialize.jsp` (collects SMTP server, port, user, password during PSP setup).

---

### 7. Datapath API Readiness

**This is a design principle, not a discrete project.**

When building or modifying data import/sync logic, architect it so the data source is abstracted. Today it's file import (CSV/Excel upload → staging tables → processing). Tomorrow it could be an API call to Datapath.

**Guideline:** Keep transformation/business logic separate from the data retrieval method. Swapping from CSV import to API fetch should not require rewriting downstream processing.

This applies to all data exchange points with Datapath — employee data, employer data, billing counts, benefit configurations, etc.

---

### 8. Deployment Readiness

**Overview:** Make the system installable and runnable for new PSP clients.

**Beta deployment model (first ~5 PSPs):**

1. Create a Linux VM on IONOS (or similar hosting).
2. Install MySQL, Java 17, Tomcat 10.
3. Deploy blank `beta_ssa` database schema.
4. Upload WAR file.
5. Run the initialize function to seed required data.
6. PSP configures their info (name, address, domain, SMTP settings, etc.) via `initialize.jsp`.

**Audit needed:** Analyze what the current initialize function actually covers versus what a cold-start PSP install truly requires. Gap analysis should include: seed constants, default roles, default fee types, template categories, user role definitions, and any other reference data.

**Future consideration:** If demand grows beyond 5 beta clients, evaluate whether per-VM deployment is sustainable or if containerized/multi-tenant deployment is warranted.

---

### 9. Mobile-Friendly Design

**Overview:** Ensure the platform works well on mobile devices, starting with the sales portal.

**Approach:** Responsive web design, not a native app. No app store overhead, works on any device.

**Current state:** Bootstrap responsive classes are in use throughout, but touch targets, font sizes, and button sizing often fall short on mobile. Partially responsive but not mobile-optimized.

**To-be goals:**

- Sales portal designed mobile-first (agents quoting on the go).
- Across the entire application, improve tap targets, text readability, and button sizing for mobile use.
- Standing rule for all new development: design mobile-friendly from the start rather than retrofitting.

---

### 10. Front Page / Landing Page

**For Superior State (superiorstate.net):**

- Migrate the application from `superiorstate.biz` to `superiorstate.net`.
- `landing-page.jsp` serves as the public marketing front page and gateway into the user portal (login modal already built in).

**For distributed PSP installs:**

- Default landing page is `index.jsp` — the simple, clean login page that displays after logout. Functional and sufficient out of the box.
- Future nice-to-have: Configurable branding (PSP name, logo, colors from database) but not a requirement for beta launch.

---

## Personal / Business Tier Projects

These projects are valuable to Superior State specifically but are not priorities for conference demo or PSP distribution.

### 11. Outlook Email Import Add-in

**Overview:** An Outlook add-in (plugin) that adds a button to the email toolbar in Outlook's web interface. When clicked, imports the email content into the note history of a selected activity.

**Workflow:**

1. User views an email in Outlook web.
2. Clicks the AMS import button.
3. A panel opens listing open activities (initially all open activities; filtered by current user if SSO/Microsoft is connected).
4. User selects an activity.
5. Email content (subject, body, sender, date, attachments) is imported as a note in that activity's history.

**Ties to:** O365 SSO work, Microsoft Graph API.

---

### 12. Time Clock Enhancement

**Overview:** Improve the existing time clock feature on the landing page.

**Enhancements:**

- **Employee view:** Better summaries of logged time, visibility into their own history.
- **Correction workflow:** Employee requests time corrections → admin/accountant reviews and approves or denies.
- **Payroll approval:** Accountant/admin approves finalized time data for a pay period.
- **Payroll export:** Push approved time to Patriot Tax (direct API if available; intermediary like Finch if not). Stays Patriot-specific — no need to generalize for other payroll vendors.
- **Reporting:** Admin/accountant views with summaries, totals by employee, by pay period, etc.

---

### 13. Invoicing System

**Overview:** Auto-generate and manage client invoices based on known sales pricing, replacing manual Wave invoice management.

**Key aspects:**

- **Pricing source:** Starts from proposal/rate table data, but supports per-client overrides that persist independently of the master rate table. (Example: postage increases may trigger a fee change for existing clients without altering the original rate.)
- **Three fee cadences:** One-time (ad hoc, usually during new client setup or special requests), monthly (12 invoices/year), annual (billed as the 13th invoice per year).
- **Variable counts:** Participant counts update monthly from billing data already calculated on the platform. Eliminates the bookkeeper's manual count updates in Wave.
- **Client visibility:** Clients could potentially view their invoices and count detail in better granularity (ties to the client access role discussion).
- **Payment integration:** Replace Wave's invoicing/payment collection with something cheaper (Square, Stripe, or similar) that still handles ACH and card payments.

**Note:** Wave remains as the general ledger / accounting system even if invoicing moves to AMS.

---

### 14. Payroll-to-Accounting Automation

**Overview:** Automate the transfer of payroll data from Patriot Tax into Wave as multi-line journal entries.

**Current state:** Bookkeeper manually re-keys payroll data from Patriot into Wave each pay period.

**To-be:** Pull payroll data from Patriot API → format as multi-line journal entries → push into Wave via API.

**Dependencies:** Requires both Patriot and Wave API access.

---

### 15. AI Employee Knowledge Assistant

**Overview:** An AI-powered chatbox embedded in AMS that answers employee questions using indexed knowledge bases and Claude's API. Uses retrieval-augmented generation (RAG) — the system finds relevant chunks from pre-indexed JSON files and sends them as context with each question to the Anthropic Messages API.

**Full specification:** `docs/analysis/ai_chatbot_feature_spec.md`

**Knowledge bases (stored in `src/main/resources/knowledge/`):**

- **DataPath Summit Guide** — 502 searchable chunks covering HSA, FSA, HRA, COBRA, premium billing, enrollment, debit cards, claims. Sourced from Summit user guides, release notes, and training documentation.
- **Summit Training Videos** — 30 training video titles mapped to Wistia embed URLs. Included in responses when a relevant video exists.
- **Wave Accounting Help** — 449 chunks covering accounting, invoicing, payments, receipts, bank connections, and reports.
- **Business Continuity** — Systems, networks, infrastructure, vendor access, and disaster recovery documentation.
- **Backup & Recovery Procedures** — Windows Server Backup, rclone/Wasabi S3, MySQL dumps, and restore runbooks.

**Access control:** Role-based, using the existing UserRole system. Standard PSP users see only Summit-related knowledge bases. Admins see all five knowledge bases including Wave, Business Continuity, and Backup/Recovery. The `knowledge-config.json` registry maps each KB to a required access role.

**Architecture:** Chatbox UI sends AJAX requests to a `ChatAssistant` servlet. The servlet checks the user's role, searches eligible knowledge bases for relevant chunks, builds a context-augmented prompt, calls the Anthropic API, and returns the response with source citations and video links.

**Model:** Claude Haiku 4.5 for cost efficiency. Configurable to Sonnet if higher quality is needed.

**Knowledge base updates:** Re-crawl sources and regenerate JSON files. No code changes required — replace files and restart. New KBs can be added by dropping a JSON file and registering it in `knowledge-config.json`.

**Future tie-ins:** Ticket escalation (create a support ticket from a chatbot conversation that can't be resolved). Client-facing version (limited KB access for the future Client Contact role). Embeddings upgrade if keyword search proves insufficient.

**Ties to:** Ticket templates (chatbot handles simple scenarios, escalates to human workflow for complex ones). Marketing library (potential future KB source).

---


## Post-Conference Tier

### 16. Social Media Marketing Automation

**Overview:** Post-conference push to grow Superior State's business through automated, AI-powered social media content.

**Focus areas:** Lifestyle Spending Accounts, ICHRAs, and HSAs — the three highest-growth, most interesting services.

**Target audience:** Primarily agents and agencies (the sales channel). Goal is to generate enough interest that they contact Superior State. Secondary audience is direct prospects.

**Approach:** AI-driven content creation for speed and uniqueness. Automate scheduling and distribution across platforms. Maximize volume while maintaining quality and relevance.

**Ties to:** Sales/marketing library (content created here feeds the library and vice versa).

---

## Standing Architectural Principles

These apply across all projects and should be considered in every development decision:

1. **Data source abstraction** — Keep business logic separate from data retrieval methods. Today's file import should be swappable with tomorrow's API call without rewriting downstream code.

2. **Mobile-first for new features** — All new UI work should be designed for mobile usability from the start, not retrofitted.

3. **PSP-agnostic configuration** — Features that vary by PSP (SMTP settings, branding, pricing, vendor connections) should be database-driven, not hardcoded.

4. **Role expansion readiness** — The role/permission system should be architecturally ready to add new roles without major refactoring, even if only User/Admin/Accountant are built now.

5. **Conference demo quality** — For the April 2026 milestone, features should demonstrate capability and direction. Polished enough for a live demo, not necessarily production-hardened for edge cases.

6. **Content containers first** — For features with growing content (marketing library, chatbot knowledge, templates), build the structure right and let content accumulate over time.
