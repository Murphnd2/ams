# Activity Detail Page — Transition Plan

**Document Created:** February 23, 2026
**Last Updated:** February 23, 2026
**Status:** Planning — brainstorming complete, ready for phased execution

---

## 1. Executive Summary

This document tracks the modernization and feature expansion of the Activity Detail page (`activityDetail25.jsp`) and its supporting infrastructure. The work is organized into two parallel tracks:

- **Track A — GUI Modernization:** Restyle all existing components to SSA design patterns, improve usability, and lay visual groundwork for upcoming features.
- **Track B — Feature Expansion:** Introduce new backend capabilities (Unified Activity Drivers, Questionnaire System) that extend the page's functionality.

Track A begins immediately. Track B items are built incrementally and integrated as they become ready. The GUI work is designed with Track B in mind so that new features slot in without rework.

---

## 2. Current State — Page Architecture

### 2.1 Entry Points

| Servlet | URL | Purpose |
|---------|-----|---------|
| GoActivityDetail25 | `/GoActivityDetail25` | Primary data loader — resolves activity ID, populates `AmsDataLocal.CurrentActivity` |
| ViewActivity25 | `/ViewActivity25` | Display servlet — checks delegation cache, forwards to JSP |
| ViewById | `/ViewById?id=123` | Direct link entry — chains to GoActivityDetail25 |
| ViewPastActivity25 | `/ViewPastActivity25` | Past activity viewer — sets session flags, chains to GoActivityDetail25 |

### 2.2 JSP Layout — Three-Column Responsive

```
┌─────────────────────────────────────────────────────────────────┐
│  navbar25.jsp                                                   │
├──────────────┬────────────────────────┬─────────────────────────┤
│  LEFT        │  CENTER                │  RIGHT                  │
│  col-xl-3    │  col-xl-5              │  col-xl-4               │
│              │                        │                         │
│  Checklist   │  Detail Header         │  History Header         │
│  Header      │  Primary Contact       │  History Notes          │
│  Automation  │  Type-Specific Detail  │  (scrollable)           │
│  ToDo List   │  Add Note (CKEditor)   │                         │
│  Footer      │  Footer / Actions      │                         │
│  (Recurring) │  (Modals)              │                         │
│              │                        │                         │
│  order-2     │  order-first           │  order-last             │
│  order-xl-1  │  order-xl-2            │                         │
└──────────────┴────────────────────────┴─────────────────────────┘
```

### 2.3 Center Column — Type-Specific Rendering

`detailDetail25.jsp` routes to type-specific sub-JSPs:

| DTYPE | Sub-JSP | Content |
|-------|---------|---------|
| Setup | `detailSetup25.jsp` | Services to implement (checkboxes), Add Module button |
| Renewal | `detailRenewal25.jsp` | Benefit items being renewed |
| Ticket | `detailTicket25.jsp` | Issue subcategory, description, logged-by |
| Opportunity | `detailOpportunity25.jsp` | Stage, prospect info |
| CheckList | *(empty)* | No type-specific content |

### 2.4 Components Needing Modernization

| Component | File(s) | Current Style | Notes |
|-----------|---------|---------------|-------|
| Checklist Header | `checklistHeader.jsp` | `btn-dark fs-3 pe-none` banner | Old style, not SSA |
| History Header | `historyHeader.jsp` | `btn-dark fs-3 pe-none` banner | Old style, not SSA |
| Detail Header | `detailHeader25.jsp` | Functional but inconsistent | Back arrow + colored title |
| Primary Contact | `detailPrimaryContact25.jsp` | Works, edit modal exists | Styling could improve |
| Add Note | `detailAddNote25.jsp` | CKEditor + dropdowns + red Save button | Functional, needs visual pass |
| Detail Footer | `detailFooter25.jsp` | Action buttons row + modal imports | Button styling inconsistent |
| Checklist Body | `checklistBasic25.jsp` | `input-group-sm` rows with icons | Functional but dense |
| Checklist Automation | `checklistAutomation25.jsp` | Conditional import | Needs review |
| Checklist Footer | `checklistFooter25.jsp` | Persist/save actions | Needs review |
| History Body | `historyDetail25.jsp` | `border-top border-dark` rows | Functional, visually dated |
| Type-Specific Panels | `detailSetup25.jsp`, `detailRenewal25.jsp`, `detailTicket25.jsp`, `detailOpportunity25.jsp` | Various styles | Each needs SSA pass |
| Modals | Multiple (close, contacts, past activities, owner, documents, URLs, renewal items, setup modules) | Various older styles | Standardize to SSA modal pattern |

---

## 3. Track B — Feature Expansion Roadmap

### 3.1 Unified Activity Driver (Phase: Design → Build → Migrate)

**Concept:** All activity types share a common pattern — a named, suppressible entity that optionally drives a checklist via a linked task sequence. Today this is implemented differently per type. Unifying it enables new activity types to be configuration, not development.

**Current Driver Implementations:**

| Activity | Current Driver | Owner | Frequency | Sequence Required? |
|----------|---------------|-------|-----------|-------------------|
| Ticket | TicketSubcategory | PSP | One-time | Optional |
| Setup | TemplatePurpose / ApplicationModule | PSP | One-time | Should be optional |
| Renewal | Benefit (DataPath import only) | PSP | Annual only | Optional |
| Opportunity | *(none — to be built)* | Agency | One-time | Optional |

**Target State:**

| Activity | Driver | Owner | Frequency | Sequence Required? |
|----------|--------|-------|-----------|-------------------|
| Ticket | Unified Driver (PSP-scoped) | PSP | One-time | Optional |
| Setup | Unified Driver (PSP-scoped) | PSP | One-time | Optional |
| Renewal | Unified Driver (PSP-scoped, imported + custom) | PSP | Configurable | Optional |
| Opportunity | Unified Driver (Agency-scoped) | Agency | One-time | Optional |

**Key Design Decisions:**

- Agency-scoped drivers (Opportunity sales types) are fully agency-owned — tasks, sequences, and types. No PSP involvement, no cross-agency visibility.
- PSP can create custom renewal drivers alongside imported DataPath benefits.
- Renewal frequency is configurable (monthly, quarterly, semi-annual, annual, custom).
- The unified driver coexists with current structures initially — new Opportunities use it natively, existing types migrate incrementally.
- TemplatePurpose entanglement (it's the linchpin of the sequence system) is the highest-risk migration and should be last.

**Phased Approach:**

| Phase | Scope | Risk |
|-------|-------|------|
| B1.1 | Design unified driver entity/schema | Low |
| B1.2 | Build Opportunity sales type using unified driver (agency-scoped) | Medium — first agency-scoped feature |
| B1.3 | Add custom PSP-defined renewal drivers alongside imported benefits | Medium |
| B1.4 | Add configurable frequency to renewal drivers | Medium — business logic change |
| B1.5 | Migrate Ticket subcategories to unified driver | Low |
| B1.6 | Migrate Setup modules to unified driver | High — TemplatePurpose entanglement |

### 3.2 Questionnaire System (Phase: Design → Build → Integrate)

**Concept:** Replace external Jotform dependency with self-hosted questionnaire objects. PSP admin defines questionnaire templates (sets of typed questions). Templates can be linked to automation emails in the task manager. When the automation fires, a questionnaire instance is created (tied to activity + recipient), a GUID link is generated and embedded in the email. The recipient fills out the form via a public-facing page. Responses flow back into the system and attach to the activity.

**Common Questionnaire Use Cases:**

- New benefit rates (COBRA, billing)
- Discrimination testing data (multiple test types)
- Enrollment / census data collection
- General information requests

**System Components:**

| Component | Description |
|-----------|-------------|
| **QuestionnaireTemplate** | PSP admin-defined. Named, suppressible. Contains ordered list of questions with types (text, number, date, yes/no, dropdown, file upload), labels, required flag. |
| **QuestionnaireInstance** | Created at automation send-time. Tied to activity + assignee (Person or Employer) + template. Has GUID, status (SENT / VIEWED / COMPLETED), timestamps. |
| **QuestionnaireResponse** | Individual answers. Ties to instance + question. Holds response value. |
| **Public form servlet** | GUID-based public access (like ViewProposal). GET renders form, POST saves responses. Optional AJAX auto-save. |
| **Task Manager integration** | Automation email builder gets a dropdown to insert a questionnaire template. Produces a token (e.g., `<<questionnaire:TEMPLATE_ID>>`) in the email content. |
| **Send-time processing** | `SendAutoFinal25` recognizes questionnaire tokens, creates instance, generates GUID, replaces token with clickable link. |
| **Activity Detail integration** | Activity detail page shows questionnaire status (sent, viewed, completed) and allows viewing completed responses. ToDo item can auto-update on completion. |

**Phased Approach:**

| Phase | Scope | Risk |
|-------|-------|------|
| B2.1 | Design schema (template, instance, response tables) | Low |
| B2.2 | Build QuestionnaireTemplate admin UI (PSP admin) | Low–Medium |
| B2.3 | Build public form servlet (GUID-based, renders form, saves responses) | Medium |
| B2.4 | Integrate with Task Manager automation email builder (insert token) | Medium |
| B2.5 | Integrate with SendAutoFinal25 (token → instance → GUID link) | Medium |
| B2.6 | Integrate with Activity Detail page (status display, response viewing) | Medium |
| B2.7 | Auto-completion of ToDo items on questionnaire submission | Low |

---

## 4. Track A — GUI Modernization Plan

### 4.1 Design Principles

Apply consistent SSA patterns established in prior modernization work:

- **`.hdr-bar`** headers (blue gradient, white text, icon) replace old `btn-dark fs-3 pe-none` banners
- **`.btn-ssa` / `.btn-outline-ssa`** button classes
- **Card-based layouts** with subtle borders and left-border color coding
- **Chip/badge components** for status indicators
- **Color-coded urgency** (red = overdue/danger, orange = warning, SSA blue = current, gray = future/inactive)
- **Hover effects** on interactive elements for clear clickability
- **Consistent spacing and typography** across all panels

### 4.2 GUI Work Items

Each item is a discrete, deliverable unit of work.

| ID | Component | Scope | Future-Proofing Notes | Status |
|----|-----------|-------|----------------------|--------|
| A1 | Checklist Header | Replace `btn-dark` banner with `.hdr-bar` pattern | Leave room for future "Questionnaire Status" indicators | Not Started |
| A2 | History Header | Replace `btn-dark` banner with `.hdr-bar` pattern | — | Not Started |
| A3 | Detail Header | Restyle with SSA colors, improve back-nav, type icon/badge | Ensure driver info can be displayed here when unified drivers are built | Not Started |
| A4 | Primary Contact Section | SSA card styling, improve edit interaction | — | Not Started |
| A5 | Type-Specific Detail Panels | SSA pass on all 4 type panels (Ticket, Renewal, Setup, Opportunity) | Renewal panel: leave visual space for custom benefits and frequency display. Setup panel: prepare for driver-based module display. | Not Started |
| A6 | Add Note Section | SSA button styling, CKEditor container cleanup | — | Not Started |
| A7 | Detail Footer / Action Buttons | Standardize to SSA button patterns, clean up layout | Add placeholder position for future "Questionnaires" action button | Not Started |
| A8 | Checklist Body (ToDo list) | Card-based rows with left-border urgency, kebab menus (match pspHome pattern) | Automation items should visually indicate questionnaire attachment when B2 lands | Not Started |
| A9 | Checklist Automation Section | Review and modernize | Will be enhanced when questionnaire integration lands (B2.4) | Not Started |
| A10 | Checklist Footer | SSA button styling | — | Not Started |
| A11 | History Body (Notes list) | Card-based note entries, improve email link styling, metadata layout | — | Not Started |
| A12 | Modals (all) | Standardize all modals to SSA modal pattern (header color, button classes, form styling) | Contact modal should accommodate questionnaire-assignee selection later | Not Started |
| A13 | Closed Activity Banner | Restyle the yellow completed banner | — | Not Started |
| A14 | Auto-Save UX | Add visual indicator for pending/saved state | — | Not Started |

### 4.3 Simple Improvements (Align with Transition Plan)

Items that can be done during GUI work that benefit the future state:

| ID | Improvement | Rationale |
|----|-------------|-----------|
| S1 | Add activity type badge/chip to detail header | Visual consistency; prepares for unified driver display |
| S2 | Show driver/category info prominently in header area | Currently buried in type-specific panels; surfacing it prepares for unified driver |
| S3 | Add "Questionnaires" placeholder section (hidden/empty initially) in the activity detail center column | When B2 lands, the section is ready to populate |
| S4 | Standardize the closed-activity `pe-none` gating pattern across all panels | Currently inconsistent; some panels gate, some don't |
| S5 | Improve mobile stacking — ensure all three columns render cleanly on phone | Current breakpoints work but spacing is tight |

---

## 5. Execution Order

**Recommended sequence** (one item at a time):

### Phase 1 — GUI Foundation (Track A)
1. **A1 + A2** — Headers (quick wins, establish pattern)
2. **A3 + S1 + S2** — Detail header with type badge and driver info
3. **A4** — Primary contact section
4. **A5** — Type-specific panels (one at a time: Ticket → Renewal → Setup → Opportunity)
5. **A6** — Add Note section
6. **A7 + S3** — Footer actions + questionnaire placeholder
7. **A8** — Checklist body (biggest single piece)
8. **A9 + A10** — Checklist automation + footer
9. **A11** — History body
10. **A12** — Modals standardization pass
11. **A13 + A14 + S4 + S5** — Polish items

### Phase 2 — Unified Driver Foundation (Track B1)
12. **B1.1** — Design unified driver entity/schema
13. **B1.2** — Build Opportunity sales types (agency-scoped)

### Phase 3 — Questionnaire Foundation (Track B2)
14. **B2.1** — Design questionnaire schema
15. **B2.2** — Build template admin UI
16. **B2.3** — Build public form servlet

### Phase 4 — Integration
17. **B2.4 + B2.5** — Task manager + send-time integration
18. **B2.6 + B2.7** — Activity detail integration + auto-completion
19. **B1.3** — Custom PSP renewal drivers
20. **B1.4** — Configurable renewal frequency

### Phase 5 — Legacy Migration
21. **B1.5** — Migrate ticket subcategories
22. **B1.6** — Migrate setup modules (highest risk)

---

## 6. Dependencies & Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| TemplatePurpose entanglement (B1.6) | High — touches sequence builder, task builder, checklist generation, setup modules | Defer to last; coexist with unified driver until ready |
| Agency-scoped data (B1.2) | Medium — first feature with agency ownership; every query needs scope filtering | Design scope column from the start; add to unified driver entity |
| DataPath import bridge (B1.3) | Medium — imported benefits must coexist with custom drivers | Unified driver gets nullable FK to imported benefit; import process creates/syncs driver records |
| Questionnaire public form security (B2.3) | Medium — external-facing form needs protection | Reuse existing GUID pattern from ViewProposal; add rate limiting and validation |
| GUI rework during Track B | Low — if Track A is done with future features in mind, Track B integration is additive | Placeholder sections and flexible layouts in Track A |

---

## 7. Database Migration Planning

All schema changes will follow the established versioning scheme (`V{NNN}__{description}.sql`). Major migrations anticipated:

| Feature | Estimated Tables | Notes |
|---------|-----------------|-------|
| Unified Activity Driver | 1–2 new tables (driver, driver_sequence_link) + ALTER on activity | Coexist with current driver columns initially |
| Agency-Scoped Sequences | Possible new table or scope column on existing sequence tables | Agency owns tasks and sequences independently |
| Questionnaire System | 3 new tables (template, instance, response) + question definition table | Clean new tables, no migration of existing data |
| Renewal Frequency | ALTER on driver table (frequency column) + business logic | Schema simple, logic change is the real work |

Specific migration scripts will be produced at build time per established rules.

---

## 8. Change Log

| Date | Change |
|------|--------|
| 2026-02-23 | Document created. Brainstorming complete. Track A and Track B outlined. Execution order established. |
