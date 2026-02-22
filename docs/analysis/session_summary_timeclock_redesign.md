# Session — February 22, 2026 — Timeclock Redesign + Correction Workflow

## Summary

Redesigned the ViewHome25 timeclock column with daily/weekly summaries, progress bars, live timer, and built a complete time correction request workflow (employee submission + admin review).

## Timeclock UI Redesign

Replaced the flat date/in/out row listing with a modern two-tab layout:

### New Components
- **`timeClockHeader.jsp`** — Color-coded status banner (green=in, gray=out) with today's running total, "since" timestamp, active/off-clock badge, and cleaner punch In/Out buttons
- **`timeClockDetail25.jsp`** — Two-tab layout:
  - **Today tab** — Day selector buttons (Mon–Fri), per-day stretch timeline with progress bar toward 8h, per-stretch durations, pulsing green dot on active stretch, clickable completed stretches for correction requests, correction status badges (Pending/Approved/Denied)
  - **Week tab** — Horizontal bar chart per day with 8h marker line, overtime coloring (orange for 9h+), today highlight (green), footer with avg/day, 40h target, remaining hours

### Supporting Classes
- **`DaySummary.java`** (`model/general/`) — DTO aggregating stretches for a single day with computed totals, formatted strings, overtime flag, progress percentage
- **`TimeStretch.java`** (`model/general/`) — Enhanced with `inLogId`/`outLogId` fields, fixed `getMinutesWorked()` (uses `Duration.between()` instead of broken `compareTo`), added `getMinutesFormatted()` and `isComplete()`
- **`TimeTrackingDAO.java`** (`data/dao/`) — Added `getWeeklySummary()`, `getWeeklyTotalMinutes()`, `formatMinutes()`, `getCorrectionRequestMap()` methods; fixed `getTimeHistoryForRange()` loop to populate `inLogId`/`outLogId`
- **`ViewHome25.java`** (`controller/home/`) — Added `loadTimeclockData()` method loading weekly summary + correction request map as request attributes

## Time Correction Request Workflow

### Entity
- **`TimeCorrectionRequest.java`** (`model/general/`) — JPA entity mapped to `time_correction_request` table. References TimeLog records (in/out) via FK. Snapshots original values. Nullable requested times (null = no change). Status: PENDING → APPROVED or DENIED. Helper methods: `isPending()`, `isApproved()`, `isDenied()`.

### Employee Submission
- **`SubmitTimeCorrection.java`** (`controller/user/`) — POST servlet receiving modal form data. Looks up TimeLog entities, snapshots original values, sets requested times only if checkboxes checked, parses HH:mm from HTML time inputs.
- **`timeCorrectionModal.jsp`** — Bootstrap modal with:
  - Checkboxes for "change in time" / "change out time"
  - Bootstrap icon arrows (`bi-arrow-right`) between original → new time
  - Three validation rules: (1) in < out, (2) new in-time not before previous stretch end, (3) new out-time not after next stretch start
  - Boundary hints showing adjacent stretch times
  - Submit disabled until valid

### Admin Review
- **`ReviewTimeCorrections.java`** (`controller/user/`) — GET loads requests filtered by status (default PENDING), counts for badge. POST processes APPROVE/DENY. On approve: updates actual TimeLog records with requested times. On deny: stamps reviewer only.
- **`reviewTimeCorrections.jsp`** — Filter bar (Pending with count badge, Approved, Denied, All), card layout per request with employee name, status badge, date, original → requested times, employee note, inline approve/deny with comment textarea.

### Navbar Integration
- Added "Time Corrections" link to Admin dropdown in `navbar25.jsp` (PSP Admin only, `bi-clock-history` icon)

## Bug Fixes

1. **Today log not refreshing after clock in/out** — `TimeClock25.goToPage()` changed from forward to redirect (`response.sendRedirect("ViewHome25")`)
2. **Time history not showing on login** — `AuthenticateUser.loadSessionData25()` now initializes `userIsIn` and `myTimeHistory` from DB before storing local to session
3. **Clock state wrong on relogin without clocking out** — Same fix as #2 — `getMyLastPunch()` determines true state on login
4. **AuthenticateUser redirect** — Changed PSP user path from named dispatcher forward to `response.sendRedirect("ViewHome25")` so ViewHome25 gets a clean GET

## Database Migration

Script: `timeclock_correction_migration.sql` (migration #9)
- Creates `time_correction_request` table with FKs to `assignee` and `timelog`
- Indexes on `status`, `requestor_id`, `original_date`

## Files Created/Modified

### New Files
| File | Package/Location |
|------|-----------------|
| `DaySummary.java` | `model/general/` |
| `TimeCorrectionRequest.java` | `model/general/` |
| `SubmitTimeCorrection.java` | `controller/user/` |
| `ReviewTimeCorrections.java` | `controller/user/` |
| `timeCorrectionModal.jsp` | `columns/timeClock/` |
| `reviewTimeCorrections.jsp` | `columns/timeClock/` |
| `timeclock_correction_migration.sql` | `docs/` |

### Modified Files
| File | Changes |
|------|---------|
| `TimeStretch.java` | Added `inLogId`/`outLogId`, fixed `getMinutesWorked()`, added `getMinutesFormatted()`, `isComplete()` |
| `TimeTrackingDAO.java` | Added 4 new methods, fixed loop to populate log IDs |
| `ViewHome25.java` | Added `loadTimeclockData()` method with correction map |
| `AuthenticateUser.java` | Time init on login, forward → redirect fix |
| `TimeClock25.java` | Forward → redirect fix |
| `timeClockHeader.jsp` | Full replacement — status banner + punch buttons |
| `timeClockDetail25.jsp` | Full replacement — tabbed Today/Week with badges |
| `navbar25.jsp` | Added Time Corrections link to Admin dropdown |
| `pspHome25.jsp` | Added `<c:import>` for timeCorrectionModal.jsp |
