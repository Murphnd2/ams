# S14-B close-out — T139: `ViewHome25` NPE on the start-renewal path

**Branch:** `refactor/modernize-architecture` (trunk, committed directly)
**Baseline HEAD (read from `git log` at preflight, not carried from the prompt):**
`0a6e29550971393fa3d335c3ee8dde2d01ed5836` — "docs: session S14A close-out (T138, GroupConversion pre-selection banner)"
**Resolved target path:** `src/main/java/net/superiorstate/ams/controller/home/ViewHome25.java`

---

## Shipped

- `240cc4f526aa69fce602646404dbcc957922edf5` — fix: null-safe ToDo persistence loop in
  `ViewHome25.processData` (T139, S14-B). One file, **+10/−1**, no re-indentation of the loop
  body, no logger field, no import change (`List` and `ToDoOut25` were already imported).
- Docs commit (this file, the T139 row, and new rows T140/T141) — its own hash cannot appear in
  its own content; read it with `git log -1`. Reported in the session summary instead.

---

## Root cause — answered, and it contradicts the filing

**`EntityLookup.getActivityById` is never called on the start-renewal path.** It cannot "find no
match," because it is not consulted at all. The filing's mechanism is misattributed.

`intializeActivity` (`AmsDataLocal.java:1185-1234`) has exactly two callers — `AmsDataLocal:651`
(`VIEW_ACTIVITY`) and `:656` (`VIEW_CHECKLIST`). Neither is in the renewal chain. What actually
happens:

1. `UpcomingRenewals25.handleStartRenewal:81` forwards to `AddRenewal25`, setting only
   `currentEmployer` and `benefitsForRenewalList`. It touches no activity state.
2. `AddRenewal25.handleRequest:58` **successfully creates and persists** the renewal, then at
   **`:81-82`** calls `setActivity(renewal)` and `setReFilterOnExit(true)` **directly, bypassing
   `intializeActivity`** — so `setToDoList(...)` (`AmsDataLocal:1202`) never runs.
3. `forwardToView:44` → `ViewHome25.doPost:43` → `processData:145` reads `isReFilterOnExit()`,
   which is `true` **because line 82 just set it** — the path opens its own gate — and enters the
   persistence block, reaching the unguarded loop at `:151`.

`CurrentActivity.toDoList` (`AmsDataLocal.java:1039`) is declared with no initializer, and
`CurrentActivity` is session-scoped. **Precise null condition: no activity detail view has been
opened in that session.** Open one first and the list is non-null but holds the *previous*
activity's to-dos — no NPE, but a stale-slot hazard of the same family as the known multi-tab
`currentActivity` bug.

The filing's *reproduction path* is sound. Its *mechanism* is not. Both statements are recorded in
the T139 row.

### Reach vs. throw — Kevin's explicit question, settled from source

All five bypass servlets **reach** `:151` unconditionally, because the flag they set is the gate.
Whether they **throw** depends on entry point:

| Servlet | Entry point | Throws today? |
|---|---|---|
| `AddRenewal25:81-82` | `addRenewalForm.jsp` via `UpcomingRenewals25` | **Yes** — Upcoming Renewals is reachable directly after login |
| `CreateBlankRenewal25:77-78` | `addActivityModal25.jsp:40` (**PSP home** Add-Activity modal) | **Yes** — no prior activity view required |
| `CreateOpportunity:267-268` | `addActivityModal25.jsp:58`, `agentHome25.jsp:656` (**landing pages**) | **Yes** — no prior activity view required |
| `CloseActivity25:119` | `checklistFooter25.jsp` only, under `a/activityDetail/` | **No** — the activity was necessarily opened first, so the list is populated |
| `GenerateProp25:113` | `generateSetupForm25.jsp` (`a/setup/`), `manualSetup.jsp`, `reviewApplications.jsp` (`sales/`) | **Undetermined** |

**My initial summary's phrase "five successful workflows" was wrong, and is corrected here and in
the T140 row.** Three of the five have been throwing, not succeeding. What would settle
`GenerateProp25`: determining, per entry JSP, whether the user must pass through a
`VIEW_ACTIVITY`/`VIEW_CHECKLIST` dispatch first. Not traced this run.

---

## Verification status

**`code-verified`.** Named explicitly, not upgraded.

**What was actually executed:**

- `.\mvnw.cmd clean package` — **BUILD SUCCESS** (ran fully, including `clean`).
- `.\mvnw.cmd -P local clean package -DskipTests` — first invocation failed on a transient
  `target/` file lock; the retry deleted `target/` and reported **BUILD SUCCESS**. Reported rather
  than silently re-run.
- Deployed the local-profile WAR to an isolated `CATALINA_BASE` on port 8089 per
  `docs/analysis/local_render_verification.md`; never touched the IntelliJ run config (8082) or the
  system Tomcat (8080).
- Clean start: **zero `SEVERE`** entries and **zero `NullPointerException`** lines across the
  instance's logs.
- `GET /ams/ViewHome25` → `302` → `/ams/login`; `GET /ams/UpcomingRenewals` → `302` → `/ams/login`;
  `POST /ams/AddRenewal25` → `302` → `/ams/login`. Auth gates intact, no server error introduced.
- `GET /ams/login` → `200`, **33,776 bytes** — byte-identical to the established S7-F baseline.
- Instance stopped, scratch `CATALINA_BASE` deleted (outside the repo, in `$HOME`).

**What was NOT executed — and why the above does not touch the fix.** ⚠️ All three protected
endpoints redirect at `LoginFilter` **before reaching `processData`**, so **the guarded line never
ran in any of these requests.** The three 302s prove the app deploys and routes; they prove nothing
about the guard. Exercising it requires an authenticated session, and **T111 (no local PSP Admin
credential) remains open** — the same wall S13-B and S14-A hit. I did not attempt to create an
account: that is a database write outside this run's fence, and `local_render_verification.md`
states account creation is Kevin's, done by hand.

**Specifically not exercised:**

1. That a start-renewal in a fresh session now completes instead of throwing — **not walked.**
2. That the `pspHome25.jsp` render is unaffected — reasoned from code (the page renders from
   `loadLandingRows`'s fresh DAO query at `:210`, not from `toDoList`), **not walked.**
3. That the non-null case still persists to-do completions unchanged — the guard is inert when the
   list is non-null (the ternary yields the original list), **not walked.**

---

## Decisions made

1. **Guarded silently, no log line — Kevin's ruling after two hard stops fired.** See Hard stops
   below. My recommendation and his decision agreed: null on these paths means "nothing was ever
   loaded to change," not an error, so there is nothing to warn about and nothing dropped.
2. **Logging style: not applicable — no logger was added.** The brief specified Log4j2
   `LogManager.getLogger` if a logger were needed, and explicitly barred SLF4J (S13-A's instance
   being a one-off, not a precedent). Since the guard is silent, **no logger field, no import, and
   no logging-style question arose.** Recording this explicitly because the brief asked for the
   decision: the style question was made moot by the silence ruling, not answered.
   For the record, `ViewHome25` currently has **no logger at all** — it uses `System.out.println`
   (`:110`, `:126`) and `e.printStackTrace()` (`:166`). Untouched; out of fence.
3. **Ternary over an `if`-wrapped block, to keep the diff minimal.**
   `for (ToDoOut25 t : pendingToDos == null ? List.<ToDoOut25>of() : pendingToDos)` changes two
   lines and leaves the nine-line loop body untouched at its original indentation; an `if` wrapper
   would have re-indented all of it and obscured the diff. The brief asked for the smallest
   possible diff at the loop.
4. **Left the transaction structure alone.** When the list is null the block still opens and
   commits an empty transaction. Harmless, and restructuring it would violate "change nothing else
   in `processData`."
5. **Added an explanatory comment.** A comment is not a log line; given how counter-intuitive the
   real mechanism is (and that the filed one was wrong), a future reader deleting the guard as
   "defensive noise" is a real risk. It names T140 so the cause is one hop away.
6. **Filed two rows, not one.** Kevin directed that the `:124`/`:145` finding ship separately as
   its own row rather than being folded into the guard or the root-cause row.

---

## New assumptions

**S14B-1 — an empty iteration is always the correct behaviour when `toDoList` is null.** This rests
on null meaning "never populated" rather than "populated then cleared." Verified: within
`CurrentActivity` (`AmsDataLocal:1036-1505`), `setToDoList` is called only at `:1202`
(`getToDosForCurrentActivity`) and from the outer class at `:745`/`:757` (both with a constructed
`newList`) — **no call site ever passes null**, so null is unambiguously the never-initialised
state. **Reversal cost: low.** If a future caller ever sets it null to mean "invalidate," the guard
would silently skip a legitimate persistence pass. A `setToDoList` that rejected null would make
that impossible, but it lives in `AmsDataLocal` — out of fence.

**S14B-2 — nothing else in the request depends on `toDoList` being iterated.** `getToDoList()`
appears **exactly once** in `ViewHome25` (`:151`, confirmed by grep), and the page renders from
`loadLandingRows`, so skipping the loop cannot change what is displayed. **Reversal cost: trivial**
— it is a single-method, single-call-site fact, re-checkable with one grep.

No `LA-NN` filed: this is a null guard on an internal persistence loop, not a market, legal, or
customer-facing claim.

---

## Open questions raised

1. **Should the five bypass servlets call `intializeActivity`, or should `CurrentActivity`
   initialise `toDoList` to an empty list?** The first gives consistent state at the cost of extra
   queries on a path that just built the entity; the second makes the partial-population pattern
   explicit but leaves the stale-list case untouched. *Settled by:* Kevin, when T140 is scheduled —
   either answer requires editing `AmsDataLocal.java`.
2. **Does `GenerateProp25` reach `:151` with a null list?** Undetermined; see the Root cause table.
   *Settled by:* tracing its three entry JSPs for a preceding `VIEW_ACTIVITY`/`VIEW_CHECKLIST`
   dispatch.
3. **Can `CurrentActivity` itself ever be null?** T141 cannot be resolved without this, and it is an
   `AmsDataLocal` question. *Settled by:* whoever takes T141.
4. **T111 now blocks a third consecutive session's runtime verification** (S13-B, S14-A, S14-B).
   *Settled by:* Kevin, per the existing T111 row.

---

## New backlog rows filed

Grep confirming both numbers unused **before** assignment:
`grep -rn "T140" docs/` → **0 results**; `grep -rn "T141" docs/` → **0 results**.
(`grep -o "T1[0-9][0-9]" docs/analysis/project_backlog.md | sort -u -V | tail` confirmed T139 was
the highest in use.)

- **T140 — Five servlets populate `CurrentActivity` partially (`setActivity()` without
  `intializeActivity()`), leaving `toDoList` unset.** MED. The real root cause behind T139, with the
  full reach-vs-throw table above, the stale-list second failure mode, and both candidate fixes.
  Notes that it requires touching `AmsDataLocal.java`.
- **T141 — `ViewHome25.processData` guards `getCurrentActivity()` for null at `:129`/`:133` and
  dereferences it unguarded at `:124`/`:145`.** LOW. A *different* null from T139's. The unguarded
  `:124` runs first, so either the later guards are dead code or the earlier dereferences are
  unsafe — it cannot be both. No observed failure attached; filed as an internal contradiction.

---

## Contradictions found

1. **⚠️ The T139 filing's stated mechanism does not hold.** `EntityLookup.getActivityById` is never
   called on the start-renewal path. Full detail under Root cause; corrected in the T139 row rather
   than silently. The filing's *reproduction path* is correct — only the cause is wrong. This is the
   third consecutive session to find a defect in the brief's narrative rather than in the code.
2. **⚠️ My own first summary of this run said "five successful workflows," and that was wrong.**
   Three of the five have been throwing. Caught by Kevin's follow-up question, not by me, and
   corrected in both the T140 row and the table above. Recording it because the error was the same
   shape as the one this project keeps hitting: a plausible summary asserted a step ahead of the
   evidence.
3. **All three line numbers cited in this prompt held up** — a change from S14-A, which found two
   stale citations. `ViewHome25.java:151` is exactly the loop; `ViewHome25.doPost:43` is exactly the
   `processData(request)` call; `AmsDataLocal.java:1189-1192` is exactly the early-return block
   (its enclosing method is `:1185-1234`, which the prompt did not claim). Nothing to correct.
4. **The prompt's framing that the guard site is an "activity-not-found signal"** is inseparable
   from contradiction #1 — it follows from the misattributed mechanism, and is why the brief
   specified guard-and-warn. The evidence inverted that, which is what fired stop #3.
5. **"Observed twice in local logs on 2026-08-04" remains unconfirmed**, exactly as the prompt
   warned. This run did not attempt to confirm it and found no local log evidence either way — the
   isolated instance was fresh. The call chain *is* now verified against source (with a corrected
   mechanism); the observation is still session narrative.

---

## Hard stops

| # | Condition | Result |
|---|---|---|
| 1 | Null reachable where an empty list is not a correct render | **Cleared.** The loop persists ToDo completions; it is not the render source (`pspHome25.jsp` renders from `loadLandingRows:210`). Skipping drops nothing, because nothing pending exists on these paths. |
| 2 | Guard cannot be confined to the loop / same null flows elsewhere | **Cleared.** `getToDoList()` appears exactly once in `ViewHome25` (`:151`). The `:124`/`:145` unguarded dereferences are a **different** null (a null `CurrentActivity`, not a null list) — filed as T141, not folded in. |
| 3 | Null is the ordinary state, not an activity-not-found signal | **⛔ FIRED.** Reported before writing anything; Kevin ruled guard-silently. |
| 4 | Throw site shared with non-renewal paths that would log WARNs at volume | **⛔ FIRED.** Five servlets share the gate; reported with the caller list. |

Both firing stops pointed the same way, and the ruling was Kevin's, not mine.

---

## Next

**Take T140 next, and expect it to need `AmsDataLocal.java`.** T139's guard stops the NPE but leaves
the cause in place: three landing-page-reachable workflows still arrive at `ViewHome25` with a
half-populated `CurrentActivity`, and the stale-list variant — where the list is non-null but
belongs to a *different* activity — is untouched by a null guard and is the more interesting half.
It is currently masked only by the `isComplete() != wasComplete()` test at `:152`.

**Before that, T111 is worth an hour of Kevin's time.** It has now blocked runtime verification in
three consecutive sessions, and T140 is exactly the kind of session-state defect that a code read
will not settle — `code-verified` will keep being the ceiling until a local credential exists.

---

## Code-verified-only disclosure

Every claim about the *runtime behaviour of the fix* rests on reading code, not on a walk:

- That a fresh-session start-renewal now completes instead of throwing (Verification #1)
- That the `pspHome25.jsp` render is unaffected (Verification #2)
- That the non-null path still persists completions unchanged (Verification #3)
- Both New assumptions (S14B-1, S14B-2)
- The entire Root cause section, including the reach-vs-throw table — derived from source and
  entry-point greps, **not** from observing any of the five paths execute
- The claim that three of the five servlets "have been throwing" — inferred from entry-point
  reachability plus the null condition, **not** from a captured stack trace. No production or local
  log was inspected this run.

What **was** runtime-verified: both Maven profiles build; the WAR deploys and starts with zero
`SEVERE` and zero `NullPointerException`; three protected endpoints correctly 302 to `/login`; the
login page renders at its established byte count. **None of that reaches the guarded line**, which
sits behind an authentication this environment cannot currently provide.

---

## SQL close-out audit

- **SQL statements produced:** none.
- **SQL statements run:** none. No `SELECT` or any other statement was run against local
  `beta_ssa` — this build needed only source reading, greps, build output, and Tomcat logs.
- **Orphaned `.sql` files:** zero. `git diff --name-only 0a6e29550971393fa3d335c3ee8dde2d01ed5836..HEAD`
  returns only `src/main/java/net/superiorstate/ams/controller/home/ViewHome25.java` plus the two
  doc paths — **no `.sql` path**. Run this session, not recalled.
- **Current highest migration version:** `V088__proposal_ichra_intake_contribution.sql`, read from
  `ls docs/migrations/` this run. **Unchanged by this build** — no migration created or needed.
- **Pending deployment:** this fix's WAR, alongside the still-unreleased T133/T137/T138.
- **Schema described but not scripted:** none.

**This build produced no SQL, which is the expected outcome — stated explicitly rather than omitted.**

---

## Compliance statement

- **Scope fence restated:** permitted were `ViewHome25.java` (null-guard site only),
  `docs/analysis/project_backlog.md` (the T139 row, plus the two new rows Kevin authorised
  mid-run), and this close-out. **Nothing outside that set was written** — `git status` after all
  edits shows exactly those three paths.
- **⚠️ `AmsDataLocal.java` was read and NOT modified.** Confirmed: it does not appear in
  `git status`, in either commit's file list, or in `git diff --name-only <baseline>..HEAD`. It was
  read at `:640-664`, `:1180-1239`, and via grep — read-only throughout, as §2 requires. The same
  holds for `UpcomingRenewals25`, `AddRenewal25`, `CreateBlankRenewal25`, `CreateOpportunity`,
  `GenerateProp25`, `CloseActivity25`, `EntityLookup`, and every JSP consulted for entry points.
- **Every hash was read from `git log`/`git rev-parse` this run** — the code commit
  (`240cc4f526aa69fce602646404dbcc957922edf5`) and the baseline
  (`0a6e29550971393fa3d335c3ee8dde2d01ed5836`). The prompt deliberately stated no baseline hash;
  none was carried from it.
- **Repo versus prompt narrative:** every line number, call site, and entry point above was
  re-verified against the live files this run. Two of the prompt's substantive claims did not
  survive that check — the causal mechanism (contradiction #1) and, downstream of it, the
  guard-and-warn instruction (stops #3/#4). The "observed twice in local logs" claim was neither
  confirmed nor refuted, and is still labelled narrative.
- **Scope extension, stated explicitly:** the brief permitted only the existing T139 row and
  forbade new T-numbers. **Kevin authorised two new rows (T140, T141) mid-run**, with grep-confirmed
  numbers. No other row was added or renumbered.
- **No forbidden git operation ran.** Only `git log`, `git status`, `git diff`, `git pull --ff-only`,
  `git rev-parse`, `git add <named path>`, `git commit`, `git push`. Staging was by explicit named
  path, one `git add` per file.
- **Hard stops: two fired (#3 and #4).** Reported before anything was written; the resulting
  decision was Kevin's.
