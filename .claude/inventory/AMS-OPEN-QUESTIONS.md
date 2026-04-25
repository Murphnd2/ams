# AMS Inventory — Open Questions & Contradictions

This file collects every contradiction, dangling reference, suspected stale artifact, and decision-point surfaced during Pass 1. Each item names the evidence and is left **unresolved** — this pass documents what exists; reconciliation is for a human.

---

## 1. Java / runtime version mismatch

**Evidence:**
- `pom.xml` `<maven.compiler.source>` and `<maven.compiler.target>` are **Java 17**; CLAUDE.md:7 says "Java 17"
- `.github/workflows/build.yml` configures `actions/setup-java@v4` with `java-version: 21` (temurin)

**Question:** Is the project Java 17 (per pom) or Java 21 (per CI)? CI building against 21 will succeed with `--release 17` source level, but the divergence is unexplained.

---

## 2. Migration version drift

**Evidence:**
- CLAUDE.md:42 — "Check `docs/analysis/migration_tracker.md` for the current highest version (currently **V044**)"
- MEMORY.md current state — "Latest migration: **V061**"
- `AMS-INVENTORY.md` (this pass, walking `docs/migrations/`) — highest version present is **V062**

**Question:** Three different "current" versions are stated. CLAUDE.md is the most stale; MEMORY.md trails the working tree by one. Should CLAUDE.md be regarded as a snapshot rather than a source of truth?

---

## 3. Microsoft Graph / MSAL — declared but unused

**Evidence:**
- `pom.xml` declares `com.microsoft.azure:msal4j:1.21.0` and `com.microsoft.graph:microsoft-graph:5.52.0`
- `Grep` for `com.microsoft.graph` and `com.microsoft.aad.msal4j` against `src/main/java/` returns no imports

**Question:** Are these dependencies pre-staged for an in-progress feature, or leftovers that can be removed? Outlook integration (Sessions 77-78) uses the Outlook **Web Add-in** (taskpane HTML calling AMS APIs), which does not require these libraries on the server. Unresolved.

---

## 4. CLAUDE.md controller list omits subpackages present in the tree

**Evidence:**
- CLAUDE.md:14-23 enumerates `controller/`'s subpackages: activity, authentication, checklist, data, email, monthly, sequence, user
- Live tree (per `Bash ls`) also contains: `admin/`, `api/`, `assistant/`, `home/`, `market/`

**Question:** CLAUDE.md is out of date with respect to the controller tree.

---

## 5. `data/template/` package empty

**Evidence:** Earlier inventory work flagged `src/main/java/net/superiorstate/ams/data/template/` as containing no `.java` files. (See `AMS-INVENTORY.md`.)

**Question:** Is this a placeholder for planned templates, or detritus?

---

## 6. `sales_pipeline_reference.md` claims to replace docs that aren't in the tree

**Evidence:** `AMS-DOCS-INDEX.md` notes that `sales_pipeline_reference.md` claims to be the canonical replacement for three older docs that are no longer present in `docs/`.

**Question:** Were the predecessor docs deleted (good), or moved to a different location (bad reference), or never committed in this branch?

---

## 7. `questionnaire_system_design.md` says "Not Started" but V039 shipped

**Evidence:** `AMS-DOCS-INDEX.md` flags `questionnaire_system_design.md` as labeled "Not Started" though V039 (questionnaires) is in `docs/migrations/` and `model/activity/questionnaire/` is populated.

**Question:** Doc not updated post-implementation.

---

## 8. `serviceitem_unification_design_v2.md` labeled "Draft v2" with feature already shipped

**Evidence:** `AMS-DOCS-INDEX.md` notes the doc is marked draft though service-item unification appears to have shipped (per migration history and codebase entities).

**Question:** Doc lifecycle not maintained.

---

## 9. Two CKEditor distributions coexist

**Evidence:** `webapp/ckeditor/` (CKEditor 4) and `webapp/ckeditor5/` (CKEditor 5) both present (per `AMS-INVENTORY.md`).

**Question:** Is CKEditor 4 still used by any JSP, or is `ckeditor/` orphaned?

---

## 10. `src-directory.txt` UTF-16 file shows old flat layout

**Evidence:** A file `src-directory.txt` at repo root, encoded UTF-16, listing what appears to be a previous (flatter) Java package layout. Single-commit per `git log`.

**Question:** Snapshot artifact from a previous reorganization. Safe to delete? (Pass 1 doesn't propose changes.)

---

## 11. `ams-src.7z` — 9.6 MB single-commit binary

**Evidence:** A 9.6 MB `.7z` archive of source at repo root, single-commit history, never modified since import.

**Question:** Why is a binary source archive committed? Likely import artifact.

---

## 12. `proposal-content-page` skill listed twice in available-skills

**Evidence:** The Claude Code system reminder for this conversation lists both `proposal-content-page` and `anthropic-skills:proposal-content-page` as available. Only one matches a file in the repo (`.claude/skills/proposal-content-page/SKILL.md`).

**Question:** Is the second one inherited from a global Claude Code skill registry, and if so, do its contents differ from this repo's copy? Cannot answer from the working tree alone.

---

## 13. `.claude/worktrees/vigilant-jennings/` — empty directory

**Evidence:** Directory exists but is empty. `settings.local.json` allow-list contains `git worktree remove --force .claude/worktrees/vigilant-jennings` and `git branch -D claude/vigilant-jennings`, suggesting the worktree was created and removed; the empty mount point lingers.

**Question:** Safe to remove? (Out of scope for Pass 1.)

---

## 14. `web.xml` hardcoded Windows path

**Evidence:** `src/main/webapp/WEB-INF/web.xml` `<context-param>` for file-upload location is the absolute path `c:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\km_web_100\...` — a developer-machine path.

**Question:** Production tomcat10 deployment must be overriding this somewhere (otherwise uploads break). Where? Not investigated in Pass 1.

---

## 15. `persistence-local.xml` hardcoded credentials

**Evidence:** `src/main/resources/META-INF/persistence-local.xml` includes JDBC URL `jdbc:mysql://127.0.0.1:3306/beta_ssa` with literal username `root` and password `Passw0rd!`.

**Question:** This file is intentional for local development (Maven copies persistence-server.xml in the `server` profile). However, the credentials are in a tracked file. Out of scope for Pass 1 to fix; flagged.

---

## 16. EclipseLink behaviors documented as gotchas

**Evidence:** MEMORY.md lists three EclipseLink quirks the team has hit:

- L2 cache eviction required after entity mutations
- Nested `JOIN FETCH` silently dropped
- `EntityManager` must stay open during JSP forward — move `forward()` inside try block

**Question:** Not a contradiction — just noted as architectural pain that may motivate future ORM migration. Logged for completeness.

---

## 17. `LoginFilter` allow-list — `/tpo` undocumented

**Evidence:** `LoginFilter.java` allows requests to `/api/`, `/proposal/`, `/apply/`, `/q/`, `/tpo`, `/video`, `/outlook/` without a session.

**Question:** What is `/tpo`? "Third-party outsourcing" is the obvious guess, but no servlet, JSP, or doc was located in this pass.

---

## 18. Two flavors of "imports" — partial overlap

**Evidence:**
- `data/service/SummitImportService.java` + `SummitSync.java` + `model/summit/imports/` — provider-specific Summit/Datapath imports
- `data/service/UniversalImportService.java` + `InteractiveImportSession.java` + `ImportResolutionService.java` + `ImportCommitService.java` + `model/imports/` — generic interactive imports

**Question:** Is the generic Universal/Interactive importer intended to eventually subsume Summit imports, or are they parallel-by-design? No design doc resolves this in `docs/`.

---

## 19. CLAUDE.md mentions "Active feature: `feature/proposal-customization`"

**Evidence:** CLAUDE.md:51-52 — "Active work: `feature/proposal-customization` (branched from `refactor/modernize-architecture`)". Current branch in this conversation is `inventory/pass-1-ams` (created for this pass) cut from `refactor/modernize-architecture`. The `feature/proposal-customization` branch may or may not still exist.

**Question:** Branch state not verified in Pass 1.

---

## 20. No `log4j2.xml` configuration file

**Evidence:** `pom.xml` declares Log4j 2.20.0 + SLF4J→Log4j bridge, but no `src/main/resources/log4j2.xml` (or equivalent) was located. Code uses `System.out.println` extensively (per `Grep`).

**Question:** Logging is effectively going to `catalina.out` via console with default-config Log4j? Intentional? (Pass 1: noted, not investigated.)

---

## 21. EmfListener probes `Constant SSL_PORT='443'` to detect "DB initialized"

**Evidence:** `EmfListener.java` queries the `Constant` entity for `key=SSL_PORT` with value `443` to decide whether the DB is initialized; only then constructs `AmsDataGlobal`.

**Question:** Why does SSL port serve as the readiness flag? Convention from earlier dev. Noted; not a contradiction.

---

## 22. `Activity25p.java`, `Activity25u.java`, `Checklist25u.java` — naming convention not documented

**Evidence:** At `model/` root, alongside `Activity25.java` and `Checklist25.java`, sit `Activity25p.java`, `Activity25u.java`, `Checklist25u.java`. CLAUDE.md and MEMORY.md only document the "25" convention, not the "25p" / "25u" suffixes.

**Question:** Likely "PSP" and "User" view variants. Unverified.

---

## 23. `summit/imports/sEmployee2.java` / `sEmployer2.java`

**Evidence:** Each Summit-import staging table has both an unsuffixed and a `2`-suffixed sibling.

**Question:** Versioned schema? Two providers? No doc consulted in Pass 1.

---

## 24. `webapp/WEB-INF/web.xml` is nearly empty

**Evidence:** `web.xml` declares `<servlet-spec>` 5.0 with only the file-upload context-param and TldScanner.jarsToSkip context-param. Servlet mappings live entirely on `@WebServlet` annotations.

**Question:** Not a contradiction. Just noted that there is essentially no XML wiring; everything is annotation-driven.

---

## 25. `src/main/java/net/superiorstate/ams/EmfListener.java` and `LoginFilter.java` live at the package root

**Evidence:** Both files are at `net.superiorstate.ams` (no subpackage), unlike every other servlet/filter which is nested under `controller/` or `filter/`.

**Question:** Convention drift; not a defect.

---

## 26. `.claude/` is in `.gitignore` (line 5) but this pass writes to it

**Evidence:** `.gitignore:5` ignores `.claude/`. The user-supplied task asks for output under `.claude/inventory/` and a commit. To commit, `git add -f` is required.

**Question:** The user is presumably aware (the directory was their choice). This pass uses `git add -f .claude/inventory/` so only the inventory output is forced in, not other `.claude/` artifacts.

---

## 27. `MEMORY.md` says `EntityManager must stay open during JSP forward`

**Evidence:** Listed as a gotcha but not actionable as a question; flagged here only because if any servlet forwards outside a try-with-resources EM block, that's latent risk. Not investigated in Pass 1.

---

## 28. `docs/migrations/` has 38 versioned scripts (V025-V062), but the lowest is V025

**Evidence:** No V001-V024 in `docs/migrations/`. CLAUDE.md does not document where the baseline schema lives.

**Question:** V025 is the start of versioned migrations; is there a `schema_baseline.sql` or equivalent? `docs/schema_version_migration.sql` (per CLAUDE.md:73) is the registration table — not the baseline DDL.

---

## 29. CLAUDE.md `Reference Documentation` section refers to docs that may have moved

**Evidence:** CLAUDE.md:71-78 lists eight reference docs. `AMS-DOCS-INDEX.md` (this pass) walks `docs/` and notes which of those are present and current; some are tagged "current," others may not exist at the cited path. (See AMS-DOCS-INDEX.md for per-file resolution.)

**Question:** Each cited path should be cross-checked against `AMS-DOCS-INDEX.md`.

---

## 30. The `proposal-content-page` SKILL is the only repo-local skill

**Evidence:** `.claude/skills/` contains exactly one skill. The active conversation's available-skills list shows additional `anthropic-skills:*` skills (consolidate-memory, setup-cowork, pdf, xlsx, docx, schedule, pptx, skill-creator).

**Question:** Are those installed at the user/global level (i.e., `~/.claude/skills/`)? Not visible in this repo. Documented in `AMS-CLAUDE-ASSETS.md`.

---

## Summary

**Total open questions raised:** 30

**Suggested triage priority for human reviewer:**

1. **High** — Items 2 (migration drift), 4 (CLAUDE.md outdated), 14 (hardcoded path), 15 (hardcoded credentials), 28 (no baseline schema doc)
2. **Medium** — Items 1, 3, 9, 10, 11, 13, 17, 18, 26
3. **Low / informational** — All others
