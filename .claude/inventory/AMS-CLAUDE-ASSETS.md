# AMS `.claude/` Asset Inventory — Pass 1

> **⚠️ STALE SNAPSHOT.** Generated 2026-04-25 (Pass 1), same vintage as `AMS-INVENTORY.md` and
> `AMS-DOMAIN-KNOWLEDGE.md` (both confirmed stale 2026-07-28). This file makes no migration-version
> claim of its own, but its `.claude/` asset inventory (file lists, mtimes, staleness guesses) has not
> been re-verified since generation — treat every claim here as a snapshot, not current state.
>
> See also the accuracy warning at the top of `docs/analysis/entity_reference.md` — several
> "what data exists" claims in these inventory files trace to the same dead seeder
> (`ReferenceDataSeeder.java`, unreachable via the commented-out `Main.java:35`).

`.claude/` is excluded from version control by `.gitignore` (.gitignore:5). All dates below come from filesystem `mtime` rather than git. There is no commit history for these files.

---

## 1. `.claude/launch.json`

- **Path:** `.claude/launch.json`
- **Type:** config (Claude Code launch config)
- **Last-modified (mtime):** 2026-03-06 16:35:22
- **Touch count:** N/A — not git-tracked
- **Staleness:** **possibly stale** — touched once over a month ago; cannot verify whether the docs preview is still used.
- **AMS-specific:** AMS-specific (the `--directory docs` argument points at this repo's docs).
- **Summary:** Defines a single launch configuration named `docs-preview` that runs `python -m http.server 8123 --directory docs` (.claude/launch.json:1-12). Exposes the `docs/` folder over HTTP on port 8123 for previewing markdown/HTML.

---

## 2. `.claude/settings.local.json`

- **Path:** `.claude/settings.local.json`
- **Type:** config (Claude Code permission allow-list)
- **Last-modified (mtime):** 2026-04-24 07:29:32
- **Touch count:** N/A — not git-tracked
- **Staleness:** current (touched within the last week).
- **AMS-specific:** AMS-specific — many entries reference absolute Windows paths under `C:\Users\kevinmurphy\IdeaProjects\ams\` and AMS package paths.
- **Summary:** A 56,876-byte JSON file whose `permissions.allow` array enumerates Bash command patterns the user has approved during prior Claude Code sessions. Examples observed at the top of the file include `mvn compile -P local -q`, IntelliJ-bundled mvn wrapper invocations, `git worktree remove`, `git rm --cached -r .claude/`, `git add` calls naming specific AMS source files, MySQL/Python interpreter paths, and `xargs grep` filters. The file appears to accumulate as the user grants `Bash(...)` permissions.

---

## 3. `.claude/skills/proposal-content-page/SKILL.md`

- **Path:** `.claude/skills/proposal-content-page/SKILL.md`
- **Type:** skill
- **Last-modified (mtime):** 2026-03-06 16:49:36
- **Touch count:** N/A — not git-tracked
- **Staleness:** possibly stale (mtime ~7 weeks old; the SKILL system message in this conversation lists the skill as both `proposal-content-page` and `anthropic-skills:proposal-content-page` — see docs/analysis/archive/AMS-OPEN-QUESTIONS.md).
- **AMS-specific:** AMS-specific. Skill body explicitly references the AMS proposal builder, the `sanitizeHtml()` server method (described in MEMORY.md), and the SSA proposal viewer DOM structure (`.proposal-container`, `.proposal-section.custom-section`).
- **Size:** 11,612 bytes / 275 lines.
- **Summary (read from SKILL.md, not filename):**
  - Frontmatter `name: proposal-content-page`, `description:` triggers on requests like "make a proposal page for…", "convert this to a custom page", "create an HTML block for the proposal" (.claude/skills/proposal-content-page/SKILL.md:1-12).
  - Generates self-contained HTML "blocks" that paste into the AMS proposal builder's custom page sections. Output renders inside `<body style="background:#f8f9fa;"><div class="proposal-container">…</div>` (.claude/skills/proposal-content-page/SKILL.md:18-32).
  - Encodes a strict skeleton: scoped `<style>` + a `.PREFIX .card-inset` div with a `--s` CSS scale factor that proportionally resizes every dimension expressed as `calc(X * var(--s))` (.claude/skills/proposal-content-page/SKILL.md:53-85).
  - Documents server-side `sanitizeHtml()` rules: strips `<script>`, inline event handlers, `javascript:` URLs; preserves `<style>` blocks (.claude/skills/proposal-content-page/SKILL.md:36-46).
  - Provides a default dark-navy color palette via CSS custom properties (`--navy`, `--navy-lt`, `--teal`, etc.; .claude/skills/proposal-content-page/SKILL.md:111-127) plus reusable layout patterns: two-column info boxes, stat rings, card grid, callout box, icon circles (.claude/skills/proposal-content-page/SKILL.md:135-178).
  - Imposes constraints: fragment only (no `<html>`/`<head>`/`<body>`), `@import` font CSS inside `<style>`, `page-break-before:always` on the outer block (.claude/skills/proposal-content-page/SKILL.md:80-85).

---

## 4. `.claude/worktrees/vigilant-jennings/`

- **Path:** `.claude/worktrees/vigilant-jennings/`
- **Type:** other (empty directory; intended worktree mount point)
- **Last-modified:** 2026-02-28 07:14
- **Touch count:** N/A
- **Staleness:** possibly stale — directory is empty; the user's `settings.local.json` allow-list contains `git worktree remove --force .claude/worktrees/vigilant-jennings` and `git branch -D claude/vigilant-jennings`, suggesting this worktree was previously created and removed.
- **AMS-specific:** AMS-specific (worktree name appears in AMS settings allow-list).
- **Summary:** Empty directory. Contents of the original git worktree are no longer present.

---

## 5. `.claude/inventory/`

- **Path:** `.claude/inventory/`
- **Type:** other (this pass's output directory)
- **Last-modified:** 2026-04-25 (created during this session)
- **Summary:** Populated by this inventory task with `AMS-INVENTORY.md`, `AMS-CLAUDE-ASSETS.md`, `AMS-DOCS-INDEX.md`, `AMS-TECHNICAL-ARCHITECTURE.md`, `AMS-DOMAIN-KNOWLEDGE.md`, `docs/analysis/archive/AMS-OPEN-QUESTIONS.md`.

---

## Summary

| Asset | Type | mtime | Notes |
|---|---|---|---|
| `launch.json` | config | 2026-03-06 | Docs preview server. |
| `settings.local.json` | config | 2026-04-24 | Permission allow-list (active). |
| `skills/proposal-content-page/SKILL.md` | skill | 2026-03-06 | AMS proposal HTML block generator. |
| `worktrees/vigilant-jennings/` | other | 2026-02-28 | Empty. |
| `inventory/` | other | 2026-04-25 | This pass. |

**Notes for the human reviewer:**
- The directory `.claude/skills/` contains exactly one skill (`proposal-content-page`).
- Three AMS-related skills appear in the active conversation's available-skills list — `proposal-content-page`, `anthropic-skills:proposal-content-page`, and several `anthropic-skills:*` skills (consolidate-memory, setup-cowork, pdf, xlsx, docx, schedule, pptx, skill-creator). Of those, only `proposal-content-page` is materialized in this repo's `.claude/skills/`. The others appear to come from a parent/global Claude Code install. (See docs/analysis/archive/AMS-OPEN-QUESTIONS.md.)
- No prompts, agents, hooks, or other skill files exist under `.claude/`.
