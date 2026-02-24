# Activity Detail Page — Transition Plan

**Document Created:** February 23, 2026
**Last Updated:** February 25, 2026
**Status:** Track A complete — A1–A14 + S4 done. S5 (mobile polish) remaining.

---

## 1. Executive Summary

This document tracks the modernization and feature expansion of the Activity Detail page (`activityDetail25.jsp`) and its supporting infrastructure. The work is organized into two parallel tracks:

- **Track A — GUI Modernization:** Restyle all existing components to SSA design patterns, improve usability, and lay visual groundwork for upcoming features.
- **Track B — Feature Expansion:** Introduce new backend capabilities (Unified Activity Drivers, Questionnaire System) that extend the page's functionality.

Track A is complete (except S5 mobile polish). Track B items are built incrementally and integrated as they become ready. The GUI work is designed with Track B in mind so that new features slot in without rework.

---

## 2. Current State — Page Architecture

### 2.1 Entry Points

| Servlet | URL | Purpose |
|---------|-----|---------|
| GoActivityDetail25 | `/GoActivityDetail25` | Primary data loader — resolves activity ID, populates `AmsDataLocal.CurrentActivity` |
| ViewActivity25 | `/ViewActivity25` | Display servlet — checks delegation cache, forwards to JSP |
| ViewById | `/ViewById?id=123` | Direct link entry — chains to GoActivityDetail25 |
| ViewPastActivity25 | `/ViewPastActivity25` | Past activity viewer — sets session flags, chains to GoActivityDetail25 |

### 2.2 JSP Layout — Resizable Three-Panel

```
┌─────────────────────────────────────────────────────────────────┐
│  navbar25.jsp                                                   │
├──────────────┬────────────────────────┬─────────────────────────┤
│  LEFT        │  CENTER                │  RIGHT                  │
│  panelLeft   │  panelCenter           │  panelRight             │
│  (resizable) ║  (flex fill)           ║  (resizable)            │
│              │                        │                         │
│  Checklist   │  Detail Header         │  Add Note (Quill)       │
│  Header      │  + owner/date/archive  │  (collapsible,          │
│  ToDo List   │  Primary Contact       │   resizable editor)     │
│  Footer      │  Additional Contacts   │  History Header         │
│  (Close +    │  Type-Specific Detail  │  History Notes          │
│   modals)    │  Documents & Links     │  (flex-grow scroll)     │
│              │  Modal Imports          │                         │
├──────────────┴────────────────────────┴─────────────────────────┤
│  ║ = draggable panel dividers (widths saved to localStorage)    │
│  Responsive: ≥1200px side-by-side, 768-1199 wrap, <768 stack   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Center Column — Type-Specific Rendering

`detailDetail25.jsp` routes to type-specific sub-JSPs:

| DTYPE | Sub-JSP | Content |
|-------|---------|---------|
| Setup | `detailSetup25.jsp` | Data-driven module list (SSA badges, scroll/expand) |
| Renewal | `detailRenewal25.jsp` | Benefits in renewal (card with add/remove, scroll/expand) |
| Ticket | `detailTicket25.jsp` | Description card with expandable modal |
| Opportunity | `detailOpportunity25.jsp` | Two cards: details + proposals |
| CheckList | *(empty)* | No type-specific content |

---

## 3. Track A — GUI Modernization Status

### 3.1 Design Principles

- **`.hdr-bar`** headers (blue gradient, white text, icon)
- **`.btn-ssa` / `.btn-outline-ssa`** button classes
- **Card-based layouts** with subtle borders and left-border color coding
- **Chip/badge components** for status indicators
- **Color-coded urgency** (red = overdue, orange = warning, SSA blue = current, gray = inactive)
- **`modal-sm`** with SSA blue headers for all modals
- **Quill** rich text editor (replaces CKEditor)
- **Resizable panels** with drag dividers and localStorage persistence

### 3.2 GUI Work Items

| ID | Component | Status | Notes |
|----|-----------|--------|-------|
| A1 | Checklist Header | ✅ Done | `.hdr-bar` pattern, "+" button for add task |
| A2 | History Header | ✅ Done | `.hdr-bar` pattern |
| A3+S1+S2 | Detail Header + type badge + driver subtitle | ✅ Done | + owner/date/archive action icons |
| A4 | Primary Contact Section | ✅ Done | SSA card, pencil edit |
| A5 | Type-Specific Panels (all 4) | ✅ Done | Ticket, Renewal, Setup, Opportunity |
| A6 | Add Note Section | ✅ Done | Quill editor, collapsible, resizable |
| A7 | Footer Decomposition | ✅ Done | Contacts card, Docs card, header icons |
| A8 | Checklist Body (ToDo list) | ✅ Done | Card-based rows, left-border urgency, open scroll + completed pinned |
| A9 | Checklist Automation | ✅ Done | Lightning bolt on automated tasks, modal preview/send, info icons |
| A10 | Checklist Footer/Layout | ✅ Done | Simplified to Close + modals. Panel CSS flex column. |
| A11 | History Body | ✅ Done | SSA styling, fixed duplicate date bug |
| A12 | Modals Standardization | ✅ Done | All modals SSA `modal-sm` pattern |
| A13 | Closed Activity Banner | ✅ Done | Muted gray archived feel, "by [name]" when available |
| A14 | Auto-Save UX | ✅ Done | Amber "Unsaved" dot next to Save, Quill text-change listener |
| S3 | Questionnaire Placeholder | Not Started | Hidden section for Track B |
| S4 | Standardize `pe-none` gating | ✅ Done | Audit confirmed all panels already gated. No changes needed. |
| S5 | Mobile stacking polish | Not Started | Verify responsive layout at all breakpoints |

### 3.3 Infrastructure Completed

| Item | Description |
|---|---|
| Resizable panels | Drag dividers, localStorage persistence, CSS responsive |
| Single-layout architecture | Eliminated dual mobile/desktop blocks (fixed modal duplication) |
| Quill editor | Replaced CKEditor, resizable, tab-to-save, localStorage height |
| `AddDocumentToActivity25` | New Wasabi upload servlet for activity documents |
| Download link fix | `ShowFileUpload?doc=` replaces dead `DownloadActivityDoc` |
| Add Task modal | SSA styling, stacked layout, "At the top"/"At the bottom" positioning |
| Task Manager page | `taskManager25.jsp` fully rewritten with SSA patterns (two-column, flex layout) |

---

## 4. Track B — Feature Expansion Roadmap

### 4.1 Unified Activity Driver

| Phase | Scope | Risk | Status |
|-------|-------|------|--------|
| B1.1 | Design unified driver entity/schema | Low | Not Started |
| B1.2 | Build Opportunity sales types (agency-scoped) | Medium | Not Started |
| B1.3 | Custom PSP renewal drivers | Medium | Not Started |
| B1.4 | Configurable renewal frequency | Medium | Not Started |
| B1.5 | Migrate ticket subcategories | Low | Not Started |
| B1.6 | Migrate setup modules (TemplatePurpose) | High | Not Started |

### 4.2 Questionnaire System

| Phase | Scope | Risk | Status |
|-------|-------|------|--------|
| B2.1 | Design schema | Low | Not Started |
| B2.2 | Template admin UI | Low–Medium | Not Started |
| B2.3 | Public form servlet (GUID) | Medium | Not Started |
| B2.4 | Task Manager integration | Medium | Not Started |
| B2.5 | SendAutoFinal25 integration | Medium | Not Started |
| B2.6 | Activity Detail integration | Medium | Not Started |
| B2.7 | Auto-completion on submission | Low | Not Started |

---

## 5. Execution Order

### Phase 1 — GUI Foundation (Track A) — COMPLETE
1. ~~A1 + A2~~ ✅
2. ~~A3 + S1 + S2~~ ✅
3. ~~A4~~ ✅
4. ~~A5~~ ✅
5. ~~A6~~ ✅
6. ~~A7~~ ✅
7. ~~A8~~ ✅ — Checklist body
8. ~~A9 + A10~~ ✅ — Checklist automation + footer/layout
9. ~~A11~~ ✅
10. ~~A12~~ ✅
11. ~~A13 + A14 + S4~~ ✅ — Polish items complete
12. S5 — Mobile stacking polish ← **NEXT**

### Phase 2–5 — unchanged from original plan

---

## 6. Change Log

| Date | Change |
|------|--------|
| 2026-02-23 | Document created. Track A and Track B outlined. |
| 2026-02-24 | A1–A7, A11, A12 completed. Resizable panel layout. Quill editor. Wasabi document upload. Single-layout architecture. |
| 2026-02-25 | A8, A9, A10 completed. Checklist panel fully modernized (body, automation, footer/layout). Add Task modal modernized. Task Manager page rewritten with SSA patterns. |
| 2026-02-25 | Document cleanup: consolidated duplicate execution order sections, updated §3.2 status table, removed stale append instructions. |
| 2026-02-25 | A13, A14, S4 completed. Navbar restyled (ghost buttons, bottom radius, unauthenticated transparent bar). Email view screens modernized (ViewEmail, ViewEmailHistory — new servlets + SSA JSPs). Track A effectively complete except S5 mobile polish. |
