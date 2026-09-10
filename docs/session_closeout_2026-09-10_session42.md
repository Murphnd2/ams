# Session 42 Close-Out — 2026-09-10

**Type:** Code (T227 + T229, push to DataPath), one migration (V097), documentation. No release.

---

## Shipped

Verified against `git log`, not copied from any prompt:

- **`4d8e7e3`** — feat: T229 push to DataPath, T227 delivery guards, V097 — runtime-verified
  locally.
- **`45d2cf4`** — docs: S42 findings; T227 and T229 closed; SDX-18 resolved for Employer
  Demographic; SDX-19 corrected; new SDX entry; new T and D rows.

This close-out's own commit is not listed, because it cannot contain its own hash.

---

## Runtime verification

Every test ran locally, against the live Summit tenant.

1. The admin listing renders the new Delivery column, which proves V097 and the entity mapping.
2. A push with the flag unset is refused with 403.
3. A download still records its row, with Delivery `download`.
4. The first push, #5 `ZZ_TEST_ER_P140956_20260910094702322.txt`, was recorded as `PUSHED`.
5. A duplicate-content push was refused with 409, and nothing was recorded or sent.
6. Push #8 `ZZ_TEST_ER_20260910111246.txt` (using the S42-D filename shape, with an address
   change) was `PUSHED`. Summit processed it. The response file,
   `Response_ZZ_TEST_ER_20260910111246.txt`, reads
   `Successful|ZZTESTCompany 9102|158E140952|Employer edited successfully`.
7. #5 was held in Summit as a content duplicate of the 2026-09-08 employer file.

Not runtime-verified:

- the "Push anyway" acknowledgement;
- the `PUSH_FAILED` path;
- the pushed-filename collision refusal;
- the `cdhplan` and `demographics` pushes, which use the same code path.

---

## In flight

Nothing. `.idea/artifacts/ams_war_exploded.xml` is pre-existing IntelliJ noise.

---

## Decisions made

Each with its reversal cost:

1. **A push is a POST mode of `SummitExportServlet`'s existing dispatch.** `doPost` sets a
   request attribute and calls `doGet`, so a GET never pushes. **Reversal: remove `doPost` and
   one branch.**
2. **Push mode refuses enrollment outright, before dispatch.** This is stricter than the T201
   token, because anything in `ImportFiles` is processed with no human step. The T201 block
   itself is untouched. **Reversal: one block.**
3. **Push is off unless `ssa.properties` sets `SUMMIT_PUSH_ENABLED=true`.** It is properties only
   and fail-closed, the same pattern as `isIchraDemoStagingAllowed`. **Reversal: config.**
4. **A row is recorded before anything is sent.** No byte reaches `ImportFiles` without a
   `summit_file_export` row, which moves from `PUSHING` to `PUSHED` or `PUSH_FAILED`.
   **Reversal: reorder one method.**
5. **V097 adds four nullable delivery columns.** T230's step state is deliberately not among
   them: Mark done must work when no file exists, so step state is a separate entity. This
   contradicts T230's own row. **Reversal: drop the columns; nothing else reads them.**
6. **The duplicate-content check refuses a push that matches a prior push for the same PSP.** An
   acknowledgement bound to the matched row's id overrides it. A match against a download only
   adds a note. **Reversal: one block.**
7. **Push filenames use the download shape, `{template}_{yyyyMMddHHmmss}.txt`.** Uniqueness
   comes from a refusal whenever a prior pushed row carries the same filename, checked across
   the installation (S42-D). The diagnosis that motivated this was wrong (see Contradictions),
   but the shape is proven, so it stays. **Reversal: one line.**
8. **Claude Code does not lift local credentials.** S42-C hard-stopped correctly, and Kevin
   applied V097 by hand. A Kevin-created `mysql_config_editor` login-path named `ams_beta` is the
   offered mechanism for future local migration runs. It does not exist yet.

---

## New assumptions

- **Schedule Import gates the ~15-minute poll** (the new SDX entry, SDX-22). This is one
  observation. Confirm it on the next push.
- **AMS's duplicate-content check is scoped to one PSP**, while Summit's is probably
  tenant-wide. **Reversal: one filter.**
- **`PUSH_FAILED` rows are treated as never delivered.** A failure mid-stream could leave a
  partial file in `ImportFiles`.
- **IntelliJ pre-stages new files.** This is carried over from S41 and is still unconfirmed.

---

## Open questions, and what settles each

- **Schedule Import as the poll gate.** The next push settles it.
- **Production egress from the VPS to `ftp1.dpath.com:22`.** Kevin, before the first production
  push (the new D-row, D-96).
- **Success tokens for the `cdhplan`, `demographics` and `enrollment` responses.** Each type's
  first push settles its own.
- **Held #5 in Summit.** Kevin approves or rejects it in Process Approvals. This is housekeeping
  only.
- **Whether to tell DataPath (CASE-22040) that its Schedule Import answer was wrong.** Kevin
  decides.

---

## Contradictions found

1. **Schedule Import.** S40 recorded it as non-functional, per DataPath's CASE-22040 answer.
   Kevin found it gates processing: nothing was processed from about 15:54 on 2026-09-09 until
   he enabled it on 2026-09-10, and processing resumed at 11:54. SDX-19 has been corrected.
2. **The S42 filename diagnosis was wrong.** `ZZ_TEST_ER_P140956_20260910094702322.txt` did match
   `ZZ_TEST_ER`. Once processing resumed, it was held as a content duplicate. The resolver's
   "binds by filename prefix" comment stands. The non-pickup was the stalled poller. S42-D's
   filename change is kept, but it was not needed. S42-D's code comment claiming the `_P` shape
   never matched was corrected in C1, before commit.
3. **The hypothesis that a Held file blocks the queue is disproven.** #8 was processed while #5
   was held.
4. **S42-D's compliance statement said V097 was never applied locally.** Kevin had applied it via
   Workbench at 09:38, before tests 1–5.
5. **V097's original description was 245 characters, against `schema_version.description
   VARCHAR(200)`.** `INSERT IGNORE` truncated it with warning 1265 on local. It was fixed to 42
   characters before release. The local row holds the truncated text until the weekly refresh.

---

## Next

- **Session 43 opens on T230: response check and Mark done.**
  - SDX-18 is now answered for Employer Demographic.
  - Step state is a separate entity, and it is a think-first schema item.
  - A Phase A is required, because it touches `SummitExportServlet` and the panel.
  - It is unblocked.
- **Kevin, when ready:**
  - Release `v0.97.00` (`ROOT.war` plus V097) through the GitHub web UI.
  - A production push additionally needs the new D-row (D-96).
- **Strategic note.** This is admin-layer work; it does not advance the Forrest demo, and that
  trade is known.

---

## SQL close-out audit

- **Authored:** `docs/migrations/V097__summit_file_export_delivery.sql` (S42-B). Its description
  literal was corrected in S42-D. It is versioned and registered in `migration_tracker.md` and
  `docs/schema_version_migration.sql`.
- **Executed:** Kevin applied V097 to local `beta_ssa` via Workbench, using the pre-fix text.
  Claude Code executed no SQL this session.
- **Orphaned `.sql` files:** checked via `git status --short` — none.
- **Highest version:** V097. **Pending deployment:** V097; production is at V096.
- **Schema described but not scripted:** T230's step-state entity (described only, not designed).

---

## Compliance statement

1. **Every file modified or created this session, by full path:**
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\src\main\java\net\superiorstate\ams\controller\market\SummitExportServlet.java`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\src\main\java\net\superiorstate\ams\model\market\SummitFileExport.java`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\src\main\java\net\superiorstate\ams\data\dao\SummitFileExportDAO.java`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\src\main\webapp\WEB-INF\view\a\activityDetail\columns\detail\detailSummitSetup25.jsp`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\src\main\webapp\WEB-INF\view\market\summitFileExportAdmin25.jsp`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\migrations\V097__summit_file_export_delivery.sql` (new)
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\analysis\migration_tracker.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\schema_version_migration.sql`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\analysis\project_backlog.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\business\summit_data_exchange.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\deployment_backlog.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\claude_memory.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\session_closeout_2026-09-10_session42.md` (new, this file)
2. **Every git command run this run, all read-only except the permitted staging/commit/push:**
   `git branch --show-current`, `git log --oneline -1`, `git status -sb`, `git status --short`
   (repeated), `git diff --cached --name-only` (repeated), `.\mvnw.cmd -q -DskipTests compile`,
   `git add` (one file per invocation, twelve total across C1/C2/C3), `git commit` ×3,
   `git show --stat HEAD` ×3, `git push origin refactor/modernize-architecture` ×2,
   `git log --oneline -4` and `-5`. No `add -A`, `add .`, `add -u`, `commit -a`, `stash`,
   `checkout`, `restore`, `reset`, `rebase`, `merge`, `pull`, `tag`, or force-push.
3. **Hashes:**
   - **C1 — `4d8e7e3`** — feat: T229 push to DataPath from Summit setup panel, T227 delivery
     guards, V097 — runtime-verified.
   - **C2 — `45d2cf4`** — docs: record S42 push findings — Schedule Import gates the poller,
     SDX-18 success token, T227 T229 closed.
   - **C3 — the commit this file ships in** — docs: close session 42 — push to DataPath live
     locally, V097 pending release. Cannot contain its own hash; read it from `git log` after
     this run, not from this document.
4. **No SQL executed** — see the SQL close-out audit above. Kevin applied V097 to local
   `beta_ssa` by hand, outside this run.
5. **No `.properties` file was opened.**
6. **`docs/analysis/archive/AMS-OPEN-QUESTIONS.md` was not opened** — the new T234 row references
   it by name only, per S42-E's own instruction.
7. **Hard stops: none.** Every preflight check in every S42 sub-run passed, or (S42-C) stopped
   correctly per its own scope fence and is recorded as such rather than worked around.
8. **Final `git status --short`:** only `.idea/artifacts/ams_war_exploded.xml` should remain,
   pre-existing IntelliJ noise, never staged this run.
