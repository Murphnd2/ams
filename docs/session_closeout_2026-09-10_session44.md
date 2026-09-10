# Session 44 close-out — 2026-09-10

## Session shape

Session 44 did **not** act on S43's "Next" list. **None of S43's five recommendations was
started**, and every S43 carry-forward except the HealthSherpa items is untouched. The session was
a Kevin-directed side project: *where do we stand with HealthSherpa?*

The trigger was Julian Ferdman's 2026-09-10 email. It revealed that HealthSherpa had answered every
open item on **2026-08-18** and that the reply had been missed.

⚠️ **This is the third consecutive session that did not follow the prior session's "Next":**

- S42 → S43 redirected (recorded in S43);
- S43 → S44 redirected (this session).

Both redirections were deliberate. **S43's Next list is re-issued below unchanged, as S45's opening
list.**

## Shipped

- `0fae8d7` — docs: HealthSherpa's 2026-08-18 reply recorded.
  - Build plan: §6 correction block and §5 pointer.
  - `healthsherpa.md`: section "Onboarding reply and AI skill v1.1 (2026-09-10)".
  - LA-39 registered.
- `55cd5d8` — chore: HealthSherpa `ichra-platform` skill v1.1 vendored at
  `.claude/skills/ichra-platform-integration/`.
  - Pinned to upstream `c8d82c79384d6b2badc899fc3eed20f109316f9e`.
  - An SSA precedence block was prepended to `SKILL.md`; everything else is byte-identical to upstream.

Both were produced by s44a rev 2. Rev 1 hard-stopped correctly on an ambiguous anchor before editing
anything.

## Outside the repo

- **Reply sent to Julian 2026-09-10**, after being reviewed against the skill. It contained:
  - API Use Agreement details (entity, address, signer);
  - the NPN problem and the `tpa_slug` question;
  - the remaining webhook questions;
  - the policy-status answer.
- **HealthSherpa agent signup was not completed.** Staging and production signup both require an NPN,
  and Kevin is not licensed. No NPN was entered or authorized.
- **The staging deeplink Basic Auth credential is held by Kevin only.** It is not in the repo, not in
  project knowledge, and not in this file. It was received in the 2026-08-18 email. **Do not add
  that `.eml` to project knowledge.**

## In flight

Nothing uncommitted from this session. `.idea/artifacts/ams_war_exploded.xml` is a pre-existing IDE
change, deliberately not committed.

S43's in-flight admin-UI data items were **not touched this session**, and their status is unknown
to this close-out. Kevin confirms:

- the revised SWBD agent-facing PremiumPath page (the one without "No monthly minimums") — pasted or not;
- the SWBD rate table entry.

## Decisions made

- **Deeplink is AMS's only enrollment route.** This follows from the standing boundary *no enrollment
  path originates in AMS*.
  - A plan with `deeplink_enrollment: false` is not enrollable from AMS.
  - The skill's "prefer EnrollConnect" guidance does not apply.
  - EnrollConnect GET reads are permitted.
  - Recorded in `healthsherpa.md` and the skill's precedence block.
- **The webhook authentication method is `api_key`**, sent by HealthSherpa as an `apiKey` header.
  The form goes back once an endpoint exists.
- **SSA does not hold a HealthSherpa agent account.** Allow-listing is routed through Julian. Three
  possibilities were asked about: a `tpa_slug` for SSA, a test agency, or a partner agency creating
  the account.
- **The HealthSherpa skill is vendored in the repo**, pinned, with SSA precedence prepended, so every
  Claude Code session that loads it sees SSA's boundaries first.
  - Its `payment-and-documents.md` reference is present but out of scope, per the precedence block.
- **Policy status is not make-or-break** (Julian was told so). BCBS TX and CHRISTUS members at a
  2027-01-01 start will need coverage verified another way.

**Closed:**

- O12: Julian Ferdman is the onboarding rep.
- O14: agent-completed, per HealthSherpa's public use-case docs. Not confirmed by Julian.
- O16 and CHRISTUS: neither carrier turns on policy status before OEP.
- Rate limits: quoting 3,000/min, mutations 600/min, reads 1,000/min.
- S43's planned 2026-09-16 note to Michael Levin: no longer needed.
- S43's Sent Items check for the 2026-07-31 chase: no longer needed.

## New assumptions

| Assumption | Reversal cost | Settled by |
|---|---|---|
| **LA-39**: HealthSherpa's API Use Agreement carries the BA terms for the enrollment rail. ⚠️ Thin: a vendor statement, and the paper is unseen. Confirm before the first production deeplink POST or webhook delivery | Low | The agreement arriving |
| **Technical**: production deeplink allow-listing is per partner agency, keyed on `_agent_id`. So `_agent_id` and the agent NPN are per-agency data, never literals | Low | Julian |
| **Technical**: `transaction_id` is a safe webhook idempotency key. Source: skill v1.1, not the official webhooks page | Low until a receiver stores rows | Official docs or Julian |

Both technical assumptions are recorded in `healthsherpa.md`. **No dedicated technical-assumptions
register exists.** They have been living in close-outs and topic docs. Whether one should exist is
Kevin's call.

## Open questions raised this session

All were asked of Julian on 2026-09-10 and are settled by his reply:

- Does SSA get a `tpa_slug`, and does it apply to Deeplink?
- For staging, will HealthSherpa provision a test agency for SSA, or must a partner agency create the
  account? ⚠️ **A partner-agency answer puts SWBD on the staging deeplink path.**
- Webhook automatic retry schedule and limits, and whether delivery comes from a fixed source IP range.
- Which secure channel to use for returning the webhook key.

Two more, not settled by Julian:

- Do AMS's configured HealthSherpa hosts match the skill? The skill gives
  `api.ichra-staging.healthsherpa.com` / `api.ichra.healthsherpa.com` for quoting and EnrollConnect,
  and `staging.healthsherpa.com` / `www.healthsherpa.com` for Deeplink. **Unverified against AMS.**
  Claude Code checks when `HealthSherpaService` is next touched.
- Does a technical-assumptions register get created? Kevin.

## Contradictions found

- **Build plan §1, §5 and §6, and S43's carry-forward, said HealthSherpa had not replied since
  2026-07-29.** It replied on 2026-08-18. This is corrected by the §6 block in `0fae8d7`.
  - The stale lines (§1 line 101, §5 line 772, §6 lines 789–812, and line 844) were deliberately left
    unedited.
- **s44a rev 1 asserted that `healthsherpa.md` is newest-first.** It is chronological, oldest first.
  The build plan's "read backwards" line was misread in claude.ai. Rev 2 corrected the anchor.
- **The new `healthsherpa.md` section sits above an undated section** ("Quoting — parameters this
  document had not recorded", line 1206) that may be newer. Strict date order may be off by one
  section. Left as is; a one-line move fixes it if it matters.
- **S43's "Next" was not followed.** Deliberate, as recorded above.

No Step 1 check contradicted this close-out.

## Carried forward from S43 (untouched this session unless marked)

- T230, response check and Mark done. Phase A required, touching `SummitExportServlet` and the panel.
  The step-state entity is a think-first schema item. Unblocked.
- Release `v0.97.00` (`ROOT.war` plus V097).
  - Tag check this session: No `v0.97.*` tag exists; the latest tag is `v0.96.01`.
  - It carries C1, the fix for the production-visible apply-page defect, which raises its priority.
- D-96, production egress from the VPS to `ftp1.dpath.com:22`, before the first production push.
- SDX-22, Schedule Import as the poll gate. Confirm on the next push.
- Success tokens for the `cdhplan`, `demographics` and `enrollment` responses.
- Held file #5 in Summit Process Approvals. Kevin approves or rejects it.
- CASE-22040: whether to tell DataPath its Schedule Import answer was wrong. Kevin decides.
- S42 paths not runtime-verified:
  - the "Push anyway" acknowledgement;
  - `PUSH_FAILED`;
  - the pushed-filename collision refusal;
  - the `cdhplan` and `demographics` pushes.
- C1 paths not runtime-verified:
  - production;
  - the final-submit guard;
  - the crafted-POST drop.
- `mysql_config_editor` login-path `ams_beta` for local migration runs. Not yet created.
- **HealthSherpa (updated this session):**
  - Waiting on Julian for the API Use Agreement and the four open questions above.
  - When the agreement arrives, check it for BA terms (LA-39).
  - The webhook form goes back once an endpoint exists.
- SWBD asks (build plan §6):
  - O22 book profile and producing-agent count: not sent;
  - "three groups renewing next quarter": not sent.
- The `ichra_strategy.md` ICHRA-over-QSEHRA re-read. It still does not reflect the ICHRA + §125
  baseline.
- S43's open questions, all with Kevin:
  - two rails under §125;
  - downstream agent margin through SWBD's rate card, with Forrest;
  - enhancement-drop logging.
- `migration_tracker.md`'s "Current Highest Version" header. Check this session: line 19 reads
  `## Current Highest Version: V096` — stale against the current highest migration V097. Not edited
  (scope fence).

## Next (recommendation; Kevin chooses) — S43's list, re-issued unchanged

1. **T235**, the `selectedLos` sanitizer. Small, the same file and pattern as s43b, and it ships
   alone. Doing it before the release puts both apply-page fixes in one deploy.
2. **Kevin: release `v0.97.00`.** It carries V097, the S42 push, and the apply-page fixes. D-96 is
   still required before any production push.
3. **Employer-facing proposal custom pages** under the messaging register. On the demo path; unblocked.
4. **T230.** Unblocked, but off the demo path. Queued, not dropped.
5. **Blocked:** any pre-tax supplemental copy, pending the carrier's resolution.

HealthSherpa adds no item ahead of these.

- A webhook receiver skeleton is buildable now against sandbox: authenticate, deduplicate by
  `transaction_id`, and persist **no PHI**. It is not ahead of items 1–4.
- The deeplink hand-off can't be tested until an allowlisted `_agent_id` exists, which waits on Julian.

**Project-knowledge hygiene:**

- This file carries forward everything open from S43, so **the S43 close-out can be pruned** from
  project knowledge.
- Keep `sherpaSKILL.md`: claude.ai has no repo access, so the vendored copy doesn't replace it.

## SQL close-out audit

- **No SQL was produced, run, or recommended this session.** That covers both runs (s44a rev 1,
  s44a rev 2) and this close-out.
- Orphaned `.sql` files: none — no untracked `.sql` files and no modified `.sql` files.
- Highest migration version in tree: V097 (`V097__summit_file_export_delivery.sql`).
- Migrations pending production deployment: V097 only — every migration V001 through V096 is ✅ in
  the Production column of `migration_tracker.md`.
- `migration_tracker.md` "Current Highest Version" header: line 19, `## Current Highest Version:
  V096`. Stale — the highest migration in tree is V097. Not edited; scope fence.
- Schema described but not scripted: S42's T230 step-state entity carries forward. **Nothing new this
  session.** The webhook receiver's storage is deliberately undescribed until its PHI question is
  settled.
- This session's commits need no migration.
