# Design — Monthly Billing Launcher (one-click import + billing pipeline)
**Status:** For approval. No build prompt written yet.
**Scope:** SSA-internal only. Local FTP drop + legacy `Importer` (2A) path. Distributed per-PSP FTP is explicitly **out of scope** (backlog item — see §14).
**Author of record:** Kevin (decisions) / Claude (design). All code facts cite the three Phase A read-only reports run this session.
---
## 1. What we're building (one paragraph)
A single **launcher page** replaces the six separate checklist "Go To" clicks that run the monthly legacy import + billing pipeline. The operator drops the Summit exports on the FTP folder (already happening), uploads the HSA file through the page, and the page shows a **pre-flight checklist**: every required file present, plus a verified verdict on whether the (usually-absent) Plan Type file is actually needed this month. When the gates pass, one button launches a **background worker** that runs wipe → import → promote → clear-billing → create-billing to completion, recording per-step status to a new `billing_run` table. Because the billing step outlives any HTTP request, the page **polls** that table for live progress and a final verification summary — the timeout that plagues `CreateBilling25` today stops mattering, because we're reading status, not holding a connection.
---
## 2. Locked decisions (recap)
| # | Decision | Choice |
|---|----------|--------|
| Scope | Which path to automate | **B** — legacy `Importer` (billing feed) only; SSA-internal FTP |
| Shape | Automation model | Single launcher page; 2 preconditions; one button; background run with verification |
| Status | How run status is tracked | **A** — persisted `billing_run` table (new migration) |
| Renewals | Session-scoped `refreshRenewals` | **A** + flag — worker skips it; `renewals_refreshed` flag drives an **exactly-once, demand-driven** refresh on the renewals page |
| Renewals check | Which run the flag check reads | **(i)** latest run, gated `status = COMPLETED AND renewals_refreshed = FALSE` |
| Gate | File-presence verification | **A** — required-vs-optional checklist, **enforced**; every export required except Plan Type; Plan Type optional but guarded by a **pre-launch plan-type gap check** on the CSVs |
---
## 3. Architecture overview
```
  Summit  ──(SFTP push)──►  FTP inbox on prod          Operator
                                   │                       │
                                   │                 uploads HSA (+ PlanType if new)
                                   ▼                       ▼
                         ┌───────────────────────────────────────┐
                         │        LAUNCHER PAGE (GET)             │
                         │  • lists files, matches TABLE_MAPPINGS │
                         │  • required-file checklist (enforced)  │
                         │  • plan-type gap check (SummitImport   │
                         │    Service.parseCsv → compare PlanType)│
                         │  • HSA upload control                  │
                         │  • "Run" button (armed only if gates   │
                         │    pass) + live progress (AJAX poll)   │
                         └───────────────┬───────────────────────┘
                                         │ POST /LaunchMonthlyBilling
                                         ▼
                         ┌───────────────────────────────────────┐
                         │   BillingPipelineRunner (background)   │  writes each step ►  ┌──────────────┐
                         │   single-thread executor, 1 run max    │ ───────────────────► │ billing_run  │
                         │   step 1 WIPE      Cleaner             │                       │ (+ _step)    │
                         │   step 2 IMPORT    Importer            │  ◄─── AJAX poll ───   │  status,     │
                         │   step 3 PROMOTE   Updater (17 calls)  │      /BillingRunStatus│  current_step│
                         │   step 4 CLEAR     (extracted)         │                       │  counts,     │
                         │   step 5 CREATE    MonthlyBiller step_*│                       │  error_text, │
                         │        (14 sub-steps, live progress)   │                       │  renewals_   │
                         └───────────────────────────────────────┘                       │  refreshed   │
                                                                                          └──────────────┘
                                                                                                 ▲
   Renewals navbar page (GET) ── if latest COMPLETED run has renewals_refreshed=FALSE ── refresh once, flip flag
```
**Reuse of existing patterns:** the background executor mirrors `InstallationHealthScheduler`'s `ScheduledExecutorService` (started/stopped in `EmfListener`); each worker step obtains its own `EntityManager` from the `emf` `ServletContext` attribute exactly as the four pipeline servlets do today; every step calls the **same service classes** the servlets call — nothing about the billing logic changes.
---
## 4. Folder model
Two-folder model, for clean re-runs and a stable pre-launch view:
- **FTP inbox** (new, e.g. `<data>/ftp-inbox`): Summit's SFTP push target; the HSA/PlanType page uploads also land here. This is what the launcher's pre-launch check reads. Originals stay here until a fully successful run archives them.
- **Processing dir** (existing `AMS_UPLOAD_DIR`, default `<catalina.base>/work/ams-uploads`): what `Importer.importAllMatchingFilesInMappingOrder(em, uploadDir, processedDir)` scans. The worker **copies** inbox → processing dir at the start of the run, so the inbox retains originals if a later step fails.
Rationale: `Importer` *moves* matched files to `processedDir` during import, emptying the scan dir. Keeping the inbox as the source of truth means a failed run (most likely at the slow billing step) never destroys the month's source files. It also cleanly separates "files have arrived and are verifiable" (inbox) from "files are mid-processing" (processing dir).
> **Alternative (simpler, less robust):** single folder — FTP + uploads land directly in `AMS_UPLOAD_DIR`; re-run-from-scratch would require re-dropping files. Given the real failure mode is billing (which runs *after* import and needs no files — see §11), the two-folder model's main payoff is modest. Flagged as a swap point if you'd rather keep one folder.
**Prerequisite (infra, not AMS code):** an SFTP endpoint on the prod box for Summit to authenticate into, writing to the inbox — with a restricted user, key, and the FTP-server password-complexity rules shown in Summit's FTP Details tab. This is sysadmin work parallel to the AMS build; no credentials belong in the repo or in this doc.
---
## 5. Data model — `billing_run` (+ `billing_run_step`)
New migration, **number TBD** — confirm the next free `V0NN` at build. Note memory earmarks **V072 for ICHRA compliance reporting**, so this is likely **V072 or later** depending on ordering; the migration must self-register, update `migration_tracker.md` and `schema_version_migration.sql` per project rules (produced in Phase B, not here).
**Parent — `billing_run`** (one row per launch):
| column | type | notes |
|--------|------|-------|
| `id` | BIGINT PK AUTO_INCREMENT | |
| `started_at` | DATETIME NOT NULL | launch time |
| `completed_at` | DATETIME NULL | set when RUNNING → COMPLETED/FAILED |
| `status` | VARCHAR(16) NOT NULL | `RUNNING` / `COMPLETED` / `FAILED` |
| `current_step` | VARCHAR(40) NULL | live step label (incl. billing sub-step) |
| `mode` | VARCHAR(16) NOT NULL | `FULL` or `BILLING_ONLY` (re-run — §11) |
| `plan_type_supplied` | BOOLEAN NOT NULL | whether a Plan Type file was included |
| `renewals_refreshed` | BOOLEAN NOT NULL DEFAULT FALSE | drives §10 |
| `error_text` | TEXT NULL | failure detail |
| `launched_by` | BIGINT NULL | assignee id of operator (history) |
**Child — `billing_run_step`** (one row per pipeline step, for the progress display + history):
| column | type | notes |
|--------|------|-------|
| `id` | BIGINT PK AUTO_INCREMENT | |
| `run_id` | BIGINT NOT NULL FK → `billing_run.id` | |
| `step_name` | VARCHAR(40) NOT NULL | `WIPE`,`IMPORT`,`PROMOTE`,`CLEAR_BILLING`,`CREATE_BILLING` |
| `status` | VARCHAR(16) NOT NULL | `PENDING`/`RUNNING`/`COMPLETED`/`FAILED`/`SKIPPED` |
| `started_at` | DATETIME NULL | |
| `completed_at` | DATETIME NULL | |
| `detail` | VARCHAR(255) NULL | e.g. row counts, files matched, benefits skipped |
| `error_text` | TEXT NULL | |
> **Lighter alternative:** collapse the child table into a single `steps_json TEXT` column on `billing_run`. Cleaner migration, but JSON-in-MySQL + JPA is more awkward than a plain child entity and loses queryable step history. Recommendation: keep the child table (in-keeping with the codebase's clean-entity style). Easy to swap if you prefer lean.
Two new JPA entities (`BillingRun`, `BillingRunStep`) in `model/` (package TBD — likely `model/billing/` or `model/general/`).
---
## 6. Pre-launch verification (launcher GET)
Runs every time the page loads, reads the **FTP inbox**:
**6a. Required-file checklist (enforced).** For each of I1–IB, match inbox files against `Importer.TABLE_MAPPINGS` using the same normalized header-superset rule the real import uses. Show a row per expected type: `type | file found | present/missing`. HSA is required and satisfied by the page upload (§7). Any required type missing → button not armed. (This is also what catches a **failed FTP export** — the file simply isn't there.)
**6b. Plan Type gap check (the clever part).** Plan Type is optional, but "absent" is verified, not assumed:
1. If a Plan Type file **is** present in the inbox → green ("Plan Type file provided"); no gap check needed.
2. If **absent** → parse the CDH (`import4benefitcdh`) and PB (`import7benefitpb`) files with **`SummitImportService.parseCsv(File) → List<Map<String,String>>`** (quote-aware, header-keyed), collect the distinct **`row.get("plantypeid")`** values, and compare to the live set `SELECT p.planTypeId FROM PlanType p` — the *exact* field the real promotion (`Updater.processNewBenefitI4Fast/I7Fast`) matches against.
   - **No unknown ids** → green ("No new plan types detected — Plan Type file not needed"). Absence is safe.
   - **Unknown ids present** → red, **blocks launch**: "N new plan type(s) referenced (ids: …). Run the Plan Type export in Summit and drop it before launching." This is a real correctness guard: today those benefits are **silently dropped** to a failures CSV nobody sees (confirmed in Phase A #3), never reaching the live `Benefit` table.
**Normalization note:** the plan-type column is `PlanTypeID` → normalizes to `plantypeid` identically under both `Importer`'s and `SummitImportService`'s rules, so parsing via `SummitImportService.parseCsv` is safe for this specific column.
**Button-arm condition:** all required files present **AND** (Plan Type present **OR** no new plan types detected) **AND** no billing run currently `RUNNING`.
---
## 7. The launcher page (UI)
New servlet + JSP (e.g. `MonthlyBillingLauncher` → `monthlyBillingLauncher.jsp`). Elements:
- **Pre-flight checklist** (§6) — required files green/missing; Plan Type verdict green/red with the gap-check result.
- **HSA upload control** — operator uploads the HSA CSV; saved into the inbox with a **controlled filename** that satisfies the live HSA match rule (Phase A #3 found three inconsistent HSA rules; because we own the saved name here, we name it to satisfy whichever path `ImportCsvFiles25` actually exercises — resolved at build). HSA is required; its checklist row goes green once uploaded.
- **Run button** — armed only per §6; POSTs to `LaunchMonthlyBilling`.
- **Live progress panel** — after launch, AJAX-polls `BillingRunStatus` (§8) every few seconds: shows current step, the 5 top-level steps with status, and for CREATE_BILLING the sub-step (e.g. "Filling billing grid — 11/14"). On completion shows the verification summary (counts, any benefits skipped, errors). On failure shows `error_text` and offers the billing-only re-run (§11).
- **Access control** — restrict to PSP admin (§12).
---
## 8. Background worker
**`BillingPipelineRunner`** (new service). Launched by `LaunchMonthlyBilling` (new servlet):
1. Servlet re-validates the gates server-side (never trust the armed button alone), inserts a `billing_run` row (`RUNNING`) + five `billing_run_step` rows (`PENDING`), captures the app-scoped `AmsDataGlobal` reference and the `emf`, then **submits the runner to a single-thread executor and returns immediately** with the new `run_id`.
- **Executor:** one app-owned `ExecutorService` (single thread), created in `EmfListener.contextInitialized()` and shut down in `contextDestroyed()`, mirroring `InstallationHealthScheduler`. Single-thread + a pre-insert check ("is any run already `RUNNING`?") enforces **one billing run at a time**.
- **Status endpoint:** `BillingRunStatus` (new servlet) returns the `billing_run` + its steps as JSON for the page to poll. Read-only.
**Runner loop:** for each step, set step `RUNNING` + `billing_run.current_step`, obtain a fresh `EntityManager` from `emf`, call the service, set step `COMPLETED` (+`detail` counts) or on exception set `FAILED` + `error_text`, mark the run `FAILED`, and stop. On all steps done, mark run `COMPLETED`, `completed_at = now`, leave `renewals_refreshed = FALSE`.
**No `HttpSession` on the worker thread** — the only session-scoped call in the whole pipeline (`local.refreshRenewals`) is deliberately excluded and handled via §10. `AmsDataGlobal.miniUpdate(em)` (app-scoped) is still called by the worker using the captured global reference.
---
## 9. Pipeline steps → existing calls, and the extractions required
| Step | What it calls today (in the servlet) | Worker calls | Extraction needed? |
|------|--------------------------------------|--------------|--------------------|
| WIPE | `Cleaner.wipeTables(WipeTables25.TABLES)` | same | **Move the `TABLES` list** out of the servlet into a shared constant (e.g. `Cleaner.MONTHLY_STAGING_TABLES`) so servlet + worker share one source of truth |
| IMPORT | `Importer.importAllMatchingFilesInMappingOrder(em, uploadDir, processedDir)` | same (already headless) | none |
| PROMOTE | 17 inline `Updater.*` calls in `UpdateTables25.process()` + `global.miniUpdate` + `local.refreshRenewals` | **Extract the 17-call ordered sequence** into one method (e.g. `Updater.runMonthlyPromotion(em)`), **excluding** the two cache/session tail calls | **Yes** — extract sequence; `UpdateTables25` then delegates to it (keeps working, no behavior change). `global.miniUpdate` called by worker separately (app-scoped); `local.refreshRenewals` handled by §10 |
| CLEAR_BILLING | inline JPQL deletes in `ClearBilling25.doPost()` | extracted method | **Yes** — extract the inline clear-current-month logic into a callable method (e.g. `MonthlyBiller.clearCurrentMonth(em)` or a small `BillingCleaner`), so the worker (and the servlet) both call it |
| CREATE_BILLING | `new MonthlyBiller(em).run()` | drive the **14 `step_*()` methods individually** (they already exist, currently unused) | none new — use existing `step_*` scaffolding to update `current_step` per sub-step for live progress |
These extractions are **solicited and minimal** — each moves an existing block into a shared callable seam so the worker and the original servlet run identical logic. The servlets keep functioning (they delegate to the extracted method); no reformatting or behavior change beyond the extraction itself.

> **⚠️ CLEAR_BILLING extraction source — naming collision warning.** `Biller.java` already has a method named `clearMonthlyBilling()`. It is **not** the logic to extract — it's a separate, incomplete, currently-unused method that only deletes `BillingGrid` + `CoverageStatus` by month, and nothing in the codebase calls it today. The real clear step lives in `ClearBilling25.clearMonthBilling()` (private, inline in the servlet) and does **seven** deletes in FK-safe order: `BillingLink`, `BillingGrid`, `BillingItem`, `CoverageStatus` (by month), full-table `Coverage`, full-table `Enrollment2`, then the `BillingMonth` row itself. The extraction in this row must be sourced from `ClearBilling25.clearMonthBilling()`, not `Biller.clearMonthlyBilling()` — reaching for the latter by name-similarity alone would silently drop five of the seven deletes and leave orphaned/duplicate billing data on a re-run. See also §15.

**PROMOTE tail handling:** after the 17 promotion calls, the worker calls `global.miniUpdate(em)` (app-scoped cache refresh — safe off-request). It does **not** call `refreshRenewals` (session-scoped) — that's §10.
---
## 10. `renewals_refreshed` — exactly-once, demand-driven refresh
- The worker completes with `billing_run.renewals_refreshed = FALSE`, signaling "live tables changed; the session renewal view is stale."
- The **renewals navbar page** (servlet name TBD — a build-time lookup) on GET checks: the latest `billing_run` ordered by `started_at DESC`; if its `status = COMPLETED` and `renewals_refreshed = FALSE`, it runs the renewal refresh **once**, flips the flag to `TRUE`, then renders. Otherwise it renders normally with no refresh cost.
- The refresh itself: this servlet **has** a live `HttpSession`, so it can invoke the existing session-scoped refresh (`local.refreshRenewals(em)`) exactly as `UpdateTables25` does today — the constraint was only ever on the *background* thread. Net effect: the expensive refresh happens once, on first view after a completed run, for whoever opens renewals — and never repeats.
- Gate is explicitly `status = COMPLETED` so a failed/running run never triggers a refresh against half-updated tables.
---
## 11. Re-run / recovery
The realistic failure mode is CREATE_BILLING (slow, N+1-heavy). Because billing runs *after* import/promote and needs **no source files**, recovery doesn't require re-importing:
- The launcher offers, for a `FAILED` (or completed) run, a **"Re-run billing only"** action → `mode = BILLING_ONLY`: runs only CLEAR_BILLING + CREATE_BILLING (the extracted clear + the 14 billing sub-steps), skipping WIPE/IMPORT/PROMOTE (their steps recorded `SKIPPED`). This directly serves "billing timed out / errored" without touching the month's staging data.
- A full re-run from scratch remains possible: because WIPE leads and the inbox retains originals (§4), the worker re-copies inbox → processing dir and re-runs cleanly. (This is the existing idempotency guarantee — WIPE-first makes the no-dedupe raw INSERT safe.)
---
## 12. Access control (closing a gap found in Phase A)
Phase A #8 found `WipeTables25`/`UpdateTables25`/`ClearBilling25`/`CreateBilling25` have **no role check** — any authenticated user who knows the URL can trigger them. The launcher is the natural place to gate this: the launcher, `LaunchMonthlyBilling`, and `BillingRunStatus` servlets check the `isPspAdmin` session flag (403 otherwise), matching the import wizards' pattern. Whether to *also* add checks to the four legacy servlets is a small extra decision — flagged, not assumed (they'd remain reachable directly otherwise).
---
## 13. Code-change summary
**New:**
- Migration `V0NN` (`billing_run` + `billing_run_step`) — self-registering, tracker + `schema_version_migration.sql` updated (Phase B).
- Entities: `BillingRun`, `BillingRunStep`.
- Service: `BillingPipelineRunner` (+ small executor wiring in `EmfListener`).
- Servlets: `MonthlyBillingLauncher` (GET page + HSA upload), `LaunchMonthlyBilling` (POST), `BillingRunStatus` (poll JSON).
- JSP: `monthlyBillingLauncher.jsp` (checklist + progress).
**Extracted (solicited, behavior-preserving):**
- `WipeTables25.TABLES` → shared constant on `Cleaner`.
- 17-call promotion sequence → `Updater.runMonthlyPromotion(em)` (servlet delegates).
- `ClearBilling25` inline clear → callable method (servlet delegates) — sourced from `ClearBilling25.clearMonthBilling()`, **not** `Biller.clearMonthlyBilling()` (see §9 warning).
**Reused as-is:** `Importer.importAllMatchingFilesInMappingOrder`, `SummitImportService.parseCsv`, `MonthlyBiller.step_*()`, `AmsDataGlobal.miniUpdate`, `AmsDataLocal.refreshRenewals` (session-side only).
**Infra prerequisite (sysadmin, parallel):** SFTP endpoint + inbox folder on prod for Summit push.
---
## 14. Out of scope (backlog)
- **Distributed per-PSP FTP automation** — DataPath-customer AMS instances configuring their own drop-folders through the 2B/2C Summit path. To be captured as a `project_backlog.md` item during session close-out.
- Broader renewal-refresh redesign (moving it fully to page-load unconditionally) — not needed; §10 handles it.
- Deduplicating `Importer`'s three header-reading implementations / quote-blind header split (Phase A #4) — noted, not in scope.
---
## 15. Build-time details to confirm (not blocking approval)
1. **Next free migration number** (V072 vs later, given ICHRA compliance earmark).
2. **Renewals navbar servlet identity** — which servlet renders the renewals view (for §10 wiring). Small Phase A lookup.
3. **Exact signatures** of `Cleaner.wipeTables(...)` and the `ClearBilling25` clear block to extract (captured in Phase A; reconfirm at build).
4. **Live HSA filename rule** — which of the three HSA match paths `ImportCsvFiles25` actually exercises in production, so the page saves the HSA upload under a name that matches.
5. **Executor placement** — confirm `EmfListener` is where the single-thread executor is created/torn down (consistent with `InstallationHealthScheduler`).
6. **Entity package** for `BillingRun`/`BillingRunStep` (`model/billing/` vs `model/general/`).
7. **CLEAR_BILLING extraction source — do not confuse with `Biller.clearMonthlyBilling()`.** Extract from `ClearBilling25.clearMonthBilling()` (7 deletes: `BillingLink`, `BillingGrid`, `BillingItem`, `CoverageStatus`, full-table `Coverage`, full-table `Enrollment2`, `BillingMonth`). `Biller.clearMonthlyBilling()` is a separate, incomplete, currently-unused method (only `BillingGrid` + `CoverageStatus`) that happens to have a near-identical name — reusing it by mistake would silently under-clear on every re-run. See §9.
---
## 16. As-built / operational notes (post-launch, 2026-07-16/17)
Built as designed, in the increment order §13/§15 anticipated. Four things surfaced during and after the build that the original design didn't (couldn't) anticipate — documented here rather than rewriting the sections above, so the design/build history stays legible.

**§15 items resolved:** migration numbers landed as **V072** (`billing_run`+`billing_run_step`) and **V073** (a same-epic follow-up, not the original design — see below); `BillingRun`/`BillingRunStep` went in `model/billing/`; the `EmfListener` executor sits alongside `InstallationHealthScheduler`'s block as designed, without its `AppConfig.isMaster()` gate; the CLEAR_BILLING extraction correctly sourced from `ClearBilling25.clearMonthBilling()`, not `Biller.clearMonthlyBilling()`, per the §9/§15 warning.

**V073 — `current_step` column too narrow (schema bug).** `billing_run.current_step VARCHAR(40)` (§5's original spec) was sized for a short label but the worker's actual CREATE_BILLING sub-step labels (`"CREATE_BILLING: Fill billing coverage table (7/14)"`) run well past 40 characters. This threw MySQL error 1406 and failed a live run mid-CREATE_BILLING. V073 widens the column to `VARCHAR(255)`, schema-only, no code change (the app was already writing the longer labels correctly — the column was the bug).

**Pre-flight file-matching bug (fixed in `MonthlyBillingPreflight`, not `Importer`).** The pre-flight's original implementation called `Importer.findMatchingTableMapping`, which turned out to be override-blind and weakly normalized (`trim().toLowerCase()` only, never applies `headerOverrides`). This falsely reported `I1_Employer`/`I3_Employee_Alt`/`I8_Alt2_QB` as "Missing" even when the files were correctly present, because those three mappings' `headerOverrides` exist specifically to bridge real Summit header differences (an `EmployerOrganizationID` rename, space/colon-bearing headers, and two genuine Summit-side misspellings) that the method never consulted. Fixed by adding a private `matchMapping()` helper to `MonthlyBillingPreflight` that mirrors `Importer.importAllMatchingFilesInMappingOrder`'s matching predicate exactly (override-aware, strongly normalized). `Importer.java` was **not** touched — `findMatchingTableMapping` has a separate, unrelated, dormant caller (`importMissingPlanTypes`) and was left alone on purpose.

**Live OOM incident + launcher-side L2 cache eviction fix.** A test run froze in a GC death-spiral at CREATE_BILLING sub-step 13/14 (`fillBillingLinks`) on a 1GB-heap box that was already swapping (1.8GB total RAM). Root cause: none of the persistence configs declare `<shared-cache-mode>` and no entity is `@Cacheable`, so EclipseLink's opt-out default applies — every persisted `Coverage`/`Enrollment2`/`CoverageStatus`/`BillingGrid`/`BillingLink`/`BillingItem` accumulates in the shared L2 cache regardless of `em.clear()` (which only clears the first-level, per-EM cache), and a prior FAILED run's persisted entities stayed resident into the next run, compounding. Fixed entirely on the launcher side — `BillingPipelineRunner.evictBillingCaches()` (per-class `emf.getCache().evict(Class)`, never `evictAll()`) called at `run()` start, after each of the 14 CREATE_BILLING sub-steps, in a `finally` around CREATE_BILLING, and after PROMOTE. `MonthlyBiller`/`Biller`/`CreateBilling25` (the legacy path) were not touched — the fix is scoped entirely to the worker.

**Stale-run reaper (`BillingRunService.reapStaleRuns`).** A worker that dies hard (OOM, JVM restart) can't run its own `failRun` safety net from a dead thread, so its `billing_run` row stays `RUNNING` forever and the single-run guard (`isRunActive`) 409s every future launch attempt — this happened live and required a hand-edited `UPDATE` to recover. Added a bulk-JPQL reaper (`reapStaleRuns(emf, 30)`, 30-minute threshold — billing runs complete in minutes even when slow, so this can't plausibly reap a legitimately-running job) called in `LaunchMonthlyBilling.doPost` right before the `isRunActive` check.

**Mode-aware Run button (fixed client-side in `launcher.jsp`).** §6/§7's original design gated the Run button purely on `canLaunch` (required-file presence). That's correct for `FULL` mode but wrong for `BILLING_ONLY` (§11's re-run path): a billing-only re-run doesn't touch the upload folder at all, but after a prior run has consumed and moved the files to `processed/`, the folder is empty and `canLaunch=false` — disabling the button exactly when a billing-only re-run is needed (the operator had to bypass via the browser console once). Fixed with a `data-can-launch` attribute + client-side `updateButtonState()`: `FULL` still requires `canLaunch`; `BILLING_ONLY` is always enabled; either mode is disabled while a run is active.

**New backlog item, not a design gap — `T27` in `project_backlog.md`.** While investigating the OOM, `MonthlyBiller.fillBillingLinks` was found to read **all** historical `BillingMonth` rows (its outer query has no current-month filter) and do an N+1 `COUNT(BillingLink)` per grid row. It doesn't corrupt data — the `linkCount==0` guard means it only inserts genuinely-missing links, and historical months never have any — but it needlessly materializes every historical month's `BillingGrid` table into memory on every single run, growing worse every month the app runs. This affects the legacy path too (shared method, not launcher-specific) and wasn't something Phase A's original investigation surfaced. Logged as `T27`, not fixed in this epic — the L2 eviction above is the immediate mitigation; scoping the query to the current month is the root-cause cleanup.

**Not yet done as of this writing:** V072/V073 not applied to any production database; WAR containing this feature not deployed to production; no live end-to-end production billing run has completed via the launcher yet (the OOM incident occurred during testing, which is exactly what surfaced the fixes documented above).

---
*End of design. On approval, this becomes a sequence of Phase A (targeted lookups in §15) + Phase B build prompts — schema/migration first, then extractions, then worker + servlets + page, in reviewable increments.*
