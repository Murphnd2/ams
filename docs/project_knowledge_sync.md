# claude.ai Project ↔ Repo Sync

How the AMS **claude.ai project** (Opus — planning, decisions, Claude Code prompt authoring) stays aligned with this **repo** (the source of truth, executed by Claude Code / Sonnet).

**Last aligned:** 2026-07-15 · migration V071 · integration branch `refactor/modernize-architecture` → in-flight `feat/agency-scope-resolver`.

---

## 1. The curated knowledge set (what the project should sync)

Eight tracked files — lean, high-signal, and covering orientation + domain + state + planning. Everything else in the repo is reachable by the project's GitHub connector on demand; it does **not** need to live in project knowledge.

| # | Path | Role | Drift |
|---|---|---|---|
| 1 | `CLAUDE.md` | Project instructions + orientation (stack, layout, dev context) | slow |
| 2 | `docs/claude_memory.md` | **Live state brief** — branch, latest migration, active epic, recent sessions, key gotchas | **fast — sync often** |
| 3 | `docs/analysis/migration_tracker.md` | Schema version + per-environment apply state | **fast — sync often** |
| 4 | `.claude/inventory/AMS-DOMAIN-KNOWLEDGE.md` | Business-domain map (PSP/BPO/agency/pipeline) | slow |
| 5 | `.claude/inventory/AMS-TECHNICAL-ARCHITECTURE.md` | Stack + architecture | slow |
| 6 | `docs/analysis/entity_reference.md` | JPA entity / data-model reference | medium |
| 7 | `docs/analysis/project_backlog.md` | Feature priorities (what to plan next) | medium |
| 8 | `docs/ams_to_be_vision.md` | Product/strategy narrative (the "why") | slow |

Optional adds: `docs/analysis/sales_pipeline_reference.md` (if you do sales-pipeline work often) and `docs/analysis/CONTEXT_DataPath_Partnership_Strategy.md` (the DataPath licensing/partnership track — your project's second major thread).

## 2. What to EXCLUDE from the sync

These bloat the knowledge store and add noise (they remain in the repo, reachable on demand):

- `docs/analysis/archive/**` — historical/one-off session notes and handoffs
- `docs/migrations/**` and `docs/importscript/**` — raw SQL + multi-MB baselines
- `docs/mockups/**`, `docs/proposal_html/**`, `docs/updates/**` — samples/bundles
- `docs/analysis/session_history_archive.md` — large historical log (reference on demand)
- `src/**`, `demo/**`, `target/**`, build output — code, reachable via connector when needed

## 3. GitHub connector configuration

**If your connector supports file/path selection (preferred):** point it at exactly the 8 paths in §1. Add the optional 9th if desired.

**If your connector only supports whole-folder or whole-repo sync:** it will pull far more than needed and refill your storage. Two fixes, in order of preference:
1. Select the individual paths if the UI allows checking items in the repo tree.
2. If it's folder-only, ask me to add a `docs/knowledge/` consolidation folder as a follow-up (Phase 5) — I'll relocate the state docs there and update references so a single folder is the clean sync target.

After changing the selection, trigger a re-index and delete any previously-synced stale files the connector no longer manages.

## 4. Per-session sync ritual (prevents re-drift)

At the end of any Claude Code session that lands a migration or a notable feature:

1. **Update `docs/claude_memory.md`** — Current State (branch, latest migration), and add a Recent Sessions bullet.
2. **Update `docs/analysis/migration_tracker.md`** — new version row + per-env status (already required by the migration discipline in `CLAUDE.md`).
3. **Commit** those docs with the feature.
4. The GitHub connector re-syncs files 2 & 3 automatically — the project is current with two small edits, not a re-upload.
5. (Optional) In the project, paste a one-line delta: "Landed VNNN <feature>; latest migration now VNNN." so the project's memory updates immediately without waiting on a re-index.

**Rule of thumb:** the project's Claude should treat `claude_memory.md` + `migration_tracker.md` as the state source, and when a fast-drift fact matters (exact latest migration, current branch), confirm against live git rather than trusting older memory.

---

## Appendix A — Corrections to MERGE into your existing project instructions

Your current project instructions are good and largely current — **keep them.** Apply these targeted fixes; the rest (two-phase Phase-A/Phase-B workflow, session close-out SQL audit, migration rules, `<userPreferences>`) stays as-is.

**1. REPLACE the release/deploy description** (the old "tag-first: commit → git tag → push tag → select existing tag" is wrong and causes the tag errors you hit) with:

> Releases are created by hand in the GitHub Releases web UI (github.com/Murphnd2/ams/releases): commit AND push everything to trunk first, then Draft a new release and **type** the new tag `v0.NN.PP` (it attaches to trunk HEAD). **Never create or push tags from local git** — it conflicts with the web-created tag. `update.sh` applies migrations before swapping ROOT.war.

**2. REPLACE the branch description** with:

> Trunk = `refactor/modernize-architecture` (the GitHub **default** branch). Most work is committed **directly** to trunk; feature branches are situational (a safe fallback point, e.g. before a demo), then folded back. `main`/`beta`/`dev`/`feature/automation-email-preview` were deleted 2026-07-15. Repo is `Murphnd2/ams`.

**3. ADD a state-source rule:**

> Treat `docs/claude_memory.md` + `docs/analysis/migration_tracker.md` as the current-state source (latest migration, branch, active epic). For fast-drift facts, note they may have advanced and confirm from `ls docs/migrations/` / `git branch -a`. All 30 Pass-1 inventory open questions are resolved (`.claude/inventory/AMS-OPEN-QUESTIONS.md`) — don't re-raise them.

## Appendix B — One-time "reconcile your memory" message (paste into a fresh project chat once, after re-syncing)

```
I've cleaned up the repo and re-synced this project. Update your understanding of AMS to this current state and rely on it over any older memory:

CORRECTIONS TO YOUR MEMORY (these were stale):
- Latest migration is V071, and production IS at V071 (release tags run through v0.71.06). Your memory's "prod V067, V070/V071 pending" is stale.
- The repo docs you last saw were badly out of date (CLAUDE.md V044, claude_memory V039, tracker V057) — that was a stale connector cache, not the real branch. They're refreshed to V071 now.
- White-label runbook: docs/runbooks/agency_white_label_domain_onboarding.md now EXISTS (created 2026-07-15) — previously only in your memory + a project-only upload. It has 5 items flagged to verify against AGENCY_DNS_SETUP_ONEPAGER.md.
- Cloudflare SSL: superiorstate.net = Full (NOT strict) for SaaS custom hostnames (avoids error 526); superiorstate.biz = Full (strict). The repo doc that said "both Full strict" is corrected.
- Origin firewall: verified 2026-07-15 that the origin (66.179.248.171) does NOT accept direct 443 — it's Cloudflare-only ingress (your memory was right; the repo's "D-73 not started" is corrected).

WORKFLOW (corrected):
- Trunk = refactor/modernize-architecture (GitHub default). Direct-commit; feature branches situational; main/beta/dev/feature-automation-email-preview deleted. Repo is Murphnd2/ams.
- Releases: GitHub web UI — commit+push to trunk, then TYPE the new tag v0.NN.PP in Draft a Release. NEVER local git tag (it errors).

RECENT EPIC (since Session 88): V068 host landing → V069 white-label email (EmailIdentityResolver) → white-label wrapper → V070 GA→sub-agency parent → V071 quote tokens → feat/agency-scope-resolver (AgencyScopeResolver + IDOR closure). Open decision logged: GA→sub-agency rate-assignment model (live constraint check vs. copy-at-creation) — backlog #39.

DOC REORG: 9 one-off docs → docs/analysis/archive/ (exclude from sync). The curated sync set + this whole procedure live in docs/project_knowledge_sync.md.

RESOLVED (don't re-raise): Java 17 across pom + CI; log4j2.xml exists; MSAL/Graph removed (Outlook is a Web Add-in); /tpo intentional legacy; Summit + Universal imports converge.

Going forward, treat docs/claude_memory.md + docs/analysis/migration_tracker.md as the state source, and confirm fast-drift facts against live git when they matter.
```
