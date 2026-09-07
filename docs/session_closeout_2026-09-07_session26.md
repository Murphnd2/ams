# Session 26 close-out

Date: 2026-09-07. Branch: `refactor/modernize-architecture`. Baseline at session start: `c5d66f6`
(`docs: sync claude_memory.md for session 25`). Builds the AMS-owned participant roster and the
census upload that populates it — the thing sessions 24 and 25 were blocked on. Ends at `e2fc648`.

---

## 1. Shipped

Two commits.

- **`1cfb402`** — `feat(ichra): employer participant roster and census upload at Setup`. V094
  `employer_participant`; `EmployerParticipant`, `EmployerParticipantDAO`, `CensusParseService`,
  `CensusUploadServlet`, `censusUpload25.jsp`; the Census Upload button on `detailSetup25.jsp`;
  LA-33/34/35 in the register; the trailing-optional-column rule in the Summit spec; V094 registered
  in the tracker and `schema_version_migration.sql`.
- **`e2fc648`** — `docs: session 26 phase A findings, participant identity and setup surface`. The
  two read-only Phase A analyses.

Seven sub-runs produced this. **S26-A** and **S26-B** were read-only Phase A — participant identity,
and the Setup conversion surface. **S26-C** was the build; it **hard-stopped once, correctly**, when
its step 6 asserted the NDT block passes an identifier and it passes none, and rev2 replaced the
mechanism rather than mirroring it. **S26-D** applied V094 locally and ran the parser for the first
time. **S26-E** hardened XLSX cell types and duplicate headers, and brought local `beta_ssa` up to
the tree. **S26-F** surfaced the mapping report. **S26-G** fixed the upload-form dead end. Every
sub-run left the working tree dirty by explicit instruction until this one.

---

## 2. In flight

Nothing uncommitted — `e2fc648` is pushed and the tree is clean.

What has **not** run:

- **The multi-row sink.** `SummitExportServlet.writeFile` takes a single `String line` and prints it
  once. **File 2 (Employer CDH Plan) is already affected** — the spec requires one row per plan and
  the servlet can emit one line. This is a **correctness fix on committed code**, not merely a file-4
  prerequisite, and it is the reason file 2 is listed as unproven rather than merely unrun.
- **Files 1 and 2 have still never executed.** Gated on `SUMMIT_TPA_ID_PREFIX` and
  `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` in `ssa.properties`, a Tomcat restart to pick them up, and
  `plan_year_eligibility` being attached to the LOS being sold.
- **File 3's PB notice plan file type and field set remain unproven.** That is a Summit test, not
  code, and it gates file 5.

---

## 3. Decisions made

1. **The roster is a new table, not `employee`.** `ImportIdResolver.allocateInternalId` reallocates
   `MAX(employee_id)+1` on collision, so an `employee` id can change after the fact — and an id a
   later import can change cannot back a Summit upsert key.
2. **`employer_participant` FKs to `prospect`, not `Employer`.** File 4 fires in the same batch as
   file 1, when the Summit-sourced `Employer` row does not yet exist. `Prospect` is the only employer
   identity AMS holds at emit time. (S26-A also established that nothing in the codebase links a
   `Prospect` to an `Employer` at all.)
3. **The participant key is derived at emit time, never stored** —
   `{SUMMIT_TPA_ID_PREFIX}-P-{employer_participant.id}` (**LA-33**), mirroring the employer key.
4. **Replacement refuses after parsing, not before**, so a rejected replacement still reports what
   the submitted file contained before the operator destroys a good roster (**LA-34**).
5. **Email is collected when the employer supplies it; SSN, DOB and compensation never are**
   (**LA-35**) — excluded at the parser rather than at the schema, so the values never enter the
   process at all.
6. **Header matching is a code-level synonym set, not the V048 mapping tables.** Extending V048 means
   contact with the live import path, which is B2 and needs its own Phase A.
7. **The census file is parsed from the `Part` stream and never written to disk** — not to
   `AMS_UPLOAD_DIR`, not to a temp path. Names and home addresses; nothing about the raw file needs
   to survive the request.
8. **Both gates stay on the census upload** — session `isPspAdmin`, then `IchraAccessResolver`.

---

## 4. New assumptions

- **LA-33 — the participant key is derived, not stored.** Reversal cost: cheap before the first real
  import, **effectively irreversible after** — changing an upsert key orphans every record keyed on
  the old value.
- **LA-34 — the roster refuses replacement rather than merging.** Reversal cost: **cheap.** A merge
  path can be added later with no schema change; the table has a surrogate key and no name or address
  uniqueness constraint.
- **LA-35 — email collected, SSN/DOB/compensation never.** Reversal cost: **asymmetric, and that is
  the point.** Dropping a nullable column is cheap; collecting personal data is the direction that
  does not reverse.

---

## 5. Open questions raised

1. ⚠️ **Employer-facing secure upload — the next session's lead item.** The employer uploads a file
   attached to the activity, not rows to a table; a PSP user still runs the census upload. Triggered
   by a task in the sequence with an automated email; Kevin gets notified when the file lands. The
   link must carry an **opaque token resolving to the activity server-side — never an activity id**,
   which is sequential and guessable. **First question for its Phase A: is `/q/{guid}` genuinely
   unauthenticated?** Six questionnaire actions and a `/q/{guid}` route already sit on the Setup
   screen. If that route is unauthenticated, the token pattern, expiry, link generation and
   guid-to-activity resolution already exist in production, and this is reuse rather than invention.
   **Do not design the token before that grep.**
2. **The duplicated address line.** Red Creek's file repeats address line 1 in the address 2 column
   for at least one participant. Employer data, not a parser fault, and it will reach Summit that
   way. Flag or refuse — decide in the file 4 prompt.
3. **The submit button reads "Upload census" when a roster is loaded**, and pressing it will always
   be refused.
4. **`.xls` legacy HSSF has never been exercised.** The allowlist accepts it and the parser branches
   on it.
5. **`schema_info` reflects apply order, not the highest version applied.** Locally V093 ran after
   V094 and won the `CREATE OR REPLACE VIEW`. Production applies in version order via `update.sh`, so
   the exposure is to out-of-order or partial applies only.
6. **`AmsDataLocal.currentActivity` is a single session slot** — every action on the activity detail
   screen that resolves through it is exposed to a multi-tab rebind. Found in session 26, owned by
   nothing, deliberately not fixed.
7. **Carried forward unresolved:** S24-A, eleven legal assumptions still unfiled; `CLAUDE.md` says
   V073 and `MEMORY.md` says V089 while the tree is at V094; ZZTEST/ZZP/ZZ_TEST_* artifacts in live
   Summit still need cleanup.

---

## 6. Contradictions found

1. **"AMS has no AMS-generated employee key" was wrong.** Two allocators exist — a negative-id
   namespace (`min(id)-1`, used by PSP staff-user creation, the seeder and the bootstrap) and
   `ImportIdResolver`'s `MAX(employee_id)+1`.
2. **`employee.custom_id` does not round-trip Summit's ParticipantCustomID.** It is **write-only** —
   `getCustomId` has zero callers — and carries no unique constraint. This was load-bearing in the
   session-open brief.
3. **"`Employee.@Id` holds Summit's ID" is partly wrong** — it is a mixed namespace. AMS-created rows
   use negatives, and both import services reallocate on collision.
4. **The census form suppression was a JSP `c:if`, not a servlet decision.** Claude.ai asserted it was
   servlet-side; step 1 of S26-G disproved it before any change was made — `doGet` already called
   `renderForm` unconditionally.
5. **`DataFormatter` was already in place before S26-E claimed to add it.** The stated `54143.0`
   defect never existed; only the leading-zero loss was real, and only the pad rule was new work.
6. **`detailSetup25.jsp` had no PSP-admin gate of any kind** before this session.

---

## 7. Next

**The multi-row sink**, because file 2 is already wrong on committed code and file 4 needs it.

Then **file 4 (Demographics)** against the roster this session built, in the column order recorded in
the Summit spec — both nullable columns ahead of `Effective Date`.

Then the **employer-facing secure upload**, opening with the `/q/{guid}` Phase A.

---

## 8. SQL close-out audit

- **V094 is the only migration produced this session**: one `CREATE TABLE employer_participant`, plus
  the `CREATE OR REPLACE VIEW schema_info` and `INSERT IGNORE INTO schema_version` blocks copied from
  V093. **No `INSERT INTO constant`.**
- One registration line added to `docs/schema_version_migration.sql`.
- **No orphaned `.sql` files.**
- **Highest version in the tree: V094.**
- **Pending deployment: V092, V093, V094** — all three unapplied in production, demo, BPO and master.
  V092 and V093 were applied to local `beta_ssa` this session (S26-E) and **both applied cleanly with
  no errors**, which is useful information for the production deployment.

---

## 9. Verification class

**Runtime-verified**, against a real 38-employee employer file in a browser and in a harness: the
parser end to end — header-synonym matching, order independence, dropping of unrecognised columns
(SSN, DOB, salary, department), per-row validation, the blank-row skip, the 5,000-row cap and its
boundary, CSV/XLSX parity, XLSX numeric-cell handling and leading-zero ZIP recovery, and
duplicate-header refusal. Also: **both gates**, the `proposalId → Proposal → Prospect` resolution,
`insertAll`, the mapping report on success and on failure, the post-parse replacement refusal with
correct counts, and `deleteByProspectId` with its cache eviction. **V094 applies to a real MySQL 8
schema.**

**Code-verified only:** `.xls` legacy HSSF, and everything in section 2.

---

## Close-out of this run (S26-H)

**Scope fence compliance.** Thirteen paths staged, **each by explicit named path, one `git add` per
file — no `git add -A`, no `git add .`, no wildcard**. Commit 1 (11 paths):
`docs/migrations/V094__employer_participant.sql`, `EmployerParticipant.java`,
`EmployerParticipantDAO.java`, `CensusParseService.java`, `CensusUploadServlet.java`,
`censusUpload25.jsp`, `migration_tracker.md`, `schema_version_migration.sql`,
`legal_assumptions.md`, `summit_data_exchange.md`, `detailSetup25.jsp`. Commit 2 (2 paths):
`phase_a_participant_identity.md`, `phase_a_setup_conversion.md`. Nothing outside the fence was
staged or committed. **No tag was created**; no `stash`, `checkout`, `restore`, `rm` or `mv` was run.
Git commands used: `rev-parse --abbrev-ref`, `rev-parse --short`, `status --porcelain`, `status -sb`,
`add` (×13), `diff --cached --name-only`, `commit -F -` (×2), `push`, `log --oneline`.

⚠️ **One fence path was wrong and was corrected.** The fence named
`src/main/webapp/WEB-INF/view/a/activityDetail/detailSetup25.jsp`; that path does not exist. `find`
returned exactly one `detailSetup25.jsp` in the tree, at
`src/main/webapp/WEB-INF/view/a/activityDetail/columns/detail/detailSetup25.jsp` — the file S26-C
modified and S26-G read. The fence path was missing `columns/detail/`. Since the intent was
unambiguous and the file resolved uniquely, the real path was staged rather than hard-stopping.

**Commits, read from `git log --oneline -3`:**

- `1cfb402` — `feat(ichra): employer participant roster and census upload at Setup` (11 files, 1743
  insertions, 2 deletions)
- `e2fc648` — `docs: session 26 phase A findings, participant identity and setup surface` (2 files,
  467 insertions)

**Both pushed** — `c5d66f6..e2fc648 refactor/modernize-architecture -> refactor/modernize-architecture`,
exit 0. `git status -sb` shows the branch level with `origin/refactor/modernize-architecture`.

**Verification class of this run.** Documentation and git only. **Nothing was built, executed or
deployed; no database was connected; no source file's content was changed.**

**SQL audit for this run.** **This run produced no SQL.** Highest version in the tree remains
**V094**; pending deployment remains **V092, V093, V094**.

**Working tree.** Clean apart from this close-out file, which is created after the push by design so
that every hash in it was read from `git log` rather than predicted.
