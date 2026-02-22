
---

## February 22, 2026 — Timeclock Redesign + Correction Workflow

**Full reference:** `session_summary_timeclock_redesign.md`

Redesigned the ViewHome25 timeclock column and built a time correction request workflow:

- **UI Redesign:** Replaced flat date/in/out rows with two-tab layout: Today (day selector, stretch timeline with progress bars, per-stretch durations, pulsing active dot) and Week (horizontal bar chart per day with 8h marker, overtime coloring, avg/day + remaining stats)
- **DaySummary DTO** aggregating stretches per day with computed totals, overtime flag, progress percentage
- **TimeStretch enhanced** with `inLogId`/`outLogId`, fixed `getMinutesWorked()` bug, added `getMinutesFormatted()` and `isComplete()`
- **TimeCorrectionRequest entity** — PENDING/APPROVED/DENIED workflow for employee time correction requests
- **SubmitTimeCorrection servlet** — employee modal form submission
- **timeCorrectionModal.jsp** — Bootstrap modal with time input validation (in < out, no overlap with adjacent stretches), boundary hints
- **ReviewTimeCorrections servlet + JSP** — admin review page with filter tabs, approve/deny with comment, auto-updates TimeLog on approve
- **Correction status badges** on stretch timeline (Pending=orange, Approved=green, Denied=red) via `correctionMap` loaded in ViewHome25
- **Navbar:** Added "Time Corrections" link to Admin dropdown
- **Bug fixes:** TimeClock25 forward→redirect, AuthenticateUser time init on login + forward→redirect, clock state correct on relogin
- **Migration:** `timeclock_correction_migration.sql` (#9) — `time_correction_request` table
