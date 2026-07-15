# docs/analysis/archive/

Historical, point-in-time, and one-off documents moved out of the active `docs/` set on **2026-07-15** during the knowledge cleanup.

These are kept for provenance and git history but are **not** part of the current-state reference set and should be **excluded from the claude.ai project-knowledge sync**. For live project state, use the curated set (see `CLAUDE.md` → "Where deeper context lives" and `docs/claude_memory.md`).

| File | Why archived | Superseded by |
|---|---|---|
| `session_86_notes.md` | Per-session build notes (Session 86, V062) | `session_history_archive.md` |
| `bpo_feature_session_history.md` | Session-by-session BPO build log (Feb 2026) | `session_history_archive.md` |
| `outlook-addin-handoff.md` | One-shot Session 78 handoff prompt | Outlook add-in shipped (V060) |
| `email_assistant_handoff.md` | One-shot Email Draft Assistant handoff prompt | Shipped (V065) |
| `host_agency_landing_phase2A_confirmation.md` | Phase-confirmation note for V068 | Shipped (V068); `migration_tracker.md` |
| `email_whitelabel_phaseA_confirmation.md` | Phase-confirmation note for V069 | Shipped (V069); `migration_tracker.md` |
| `proxy_readiness_audit_prompt.md` | The prompt that generated the audit | `proxy_readiness_audit.md` (kept as current) |
| `dead_jsp_cleanup_summary.md` | Result report from `cleanup/dead-jsp-removal` (Mar 2026) | Completed one-off |
| `dead_jsp_investigation.md` | Companion investigation report | Completed one-off |
| `PHASE1_NOTES.md` | Dev-scratch notes — Phase 1 `AgencyScopeResolver` (was at repo root) | Merged to trunk `e0a62d1`; see `AgencyScopeResolver.java` |
| `PHASE2_NOTES.md` | Dev-scratch notes — Phase 2 IDOR-gap closure (was at repo root) | Merged to trunk `e0a62d1` |
| `PHASE2B_NOTES.md` | Dev-scratch notes — Phase 2b plain-agent scope + residual IDOR gaps (was at repo root) | Merged to trunk `e0a62d1` |
