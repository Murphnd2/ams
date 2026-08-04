# Session 13 close-out — 2026-08-04

**Branch:** `refactor/modernize-architecture` (trunk, committed directly)
**Model:** Sonnet (this run — documentation only). S13-A and S13-B ran on Opus.
**HEAD at preflight:** `44c5826924b13419f430dcf9e44e1a2fd7cdd41d` — exactly this run's stated
expectation (S13-B's close-out hash-recording commit). No discrepancy to report.

Sub-runs this session: **S13-A** (T133), **S13-B** (T137 + the T133 verification correction),
**S13-C** (this close-out). Full detail lives in `docs/session_s13a_closeout.md` and
`docs/session_s13b_closeout.md` — this document summarizes for a session that has not read those,
and cites them rather than repeating their reasoning.

---

## Shipped and released

Three code commits shipped this session. **All three are deployed** — verified by tag ancestry
below, not assumed.

| Commit | Item |
|---|---|
| `e87a8bc` | `SendAuto25` / `AutomationHelper` / `SendAutoFinal25` / `EmailDAO` / `PersonDAO` / `AuthDAO` — To/CC recipients silently dropped when the address belonged to an employee record with no linked Person contact row |
| `eea6482` | **T133** — unmatched `{{TOKEN}}` rendering literally on customer-facing proposal documents |
| `77fa57b` | **T137** — staging-sourced counties unlabeled in `/GroupConversion`'s county dropdown |

**Release `v0.88.03`** — published via the GitHub web UI, tag created at trunk HEAD
(`44c5826`), `ROOT.war` only attached, no migration scripts (none exist for this release — see
SQL audit below). WAR built with `-P server`, 60.5 MB. Packaged persistence verified as JNDI
(`java:comp/env/jdbc/ssa`, `non-jta-data-source`) with no baked credentials — read directly from
the WAR's `META-INF/persistence.xml`, not assumed from the profile name.

### `v0.88.02` — its contents were undetermined going into this run; now determined

Every document in project knowledge — session 12's close-out, and this session's own launch —
names `v0.88.01` as current. `v0.88.02` exists (confirmed by `git fetch --tags` during S13-B)
and was undocumented. Determined this run via `git log v0.88.01..v0.88.02`:

```
809e0f2 fix: resolve contact email from linked Employee in automation email path
7655286 docs: record the session 12 close-out commit hash
def5450 docs: session 12 close-out
effadf3 docs: record the S12-B close-out commit hash
27e3574 docs: S12-B close-out
bcc1417 fix: close Illustration's dropdown labeling gap, amend T48 (S12-B)
cd04745 docs: record the session 11 close-out commit hash
d4d682b docs: session 11 close-out
1b58d79 docs: record the S11-I close-out commit hash
72b13fb docs: S11-I close-out
a48529c docs: correct the proposal-content-page skill's token table (T132)
```

**`v0.88.02` carries exactly one code commit — `809e0f2`, the recipient-email fix (session 13's
first fix, made before S13-A/S13-B) — plus the full session 11/12 documentation trail.**
Tag dates, read from `git log -1 --format=%ai <tag>`:

| Tag | Commit | Date |
|---|---|---|
| `v0.88.00` | `123114a` | 2026-08-03 16:04:51 -0500 |
| `v0.88.01` | `a6a35c2` | 2026-08-03 20:52:45 -0500 |
| `v0.88.02` | `809e0f2` | 2026-08-04 09:07:42 -0500 |
| `v0.88.03` | `44c5826` | 2026-08-04 14:20:27 -0500 |

**`bcc1417` (S12-B's Illustration labeling fix, the T48 amendment) is deployed.** Session 12's
own close-out recorded it as *not yet* in any release (`git merge-base --is-ancestor bcc1417
v0.88.01` → false at the time). Re-checked this run with the same command against the current
tags:

```
git merge-base --is-ancestor bcc1417 v0.88.03   → exit 0 (yes)
git merge-base --is-ancestor bcc1417 HEAD       → exit 0 (yes)
```

It rode into production inside `v0.88.02`, one release after session 12 recorded it as pending.

---

## Verification status

Stated by name, per claim.

- **T133 — `runtime-verified`.** Three tests walked locally 2026-08-04 (full detail:
  `docs/session_s13a_closeout.md`, corrected in `docs/session_s13b_closeout.md`): an existing
  proposal re-rendered with every previously-resolving token intact; that render produced no
  WARN, confirming silence in the normal case; a deliberate `{{NOT_A_REAL_TOKEN}}` in a `CUSTOM`
  section vanished and produced, quoted verbatim from the local log:
  ```
  2026-08-04 14:03:36.923 [http-nio-8080-exec-46] WARN
  net.superiorstate.ams.controller.activity.setup.ViewProposal -
  T133 stripped unmatched proposal token(s) before render: {{NOT_A_REAL_TOKEN}}
  ```
- **SendAuto25 recipient fix — `runtime-verified`.** Walked against the originally-reported case
  before this session's other work began.
- **T137 — `code-verified`, plus one production observation.** After the `v0.88.03` deploy,
  `/GroupConversion` on production was walked and its county dropdown lists **four counties** —
  the regression guard that matters, since the naive fix (filtering) would have emptied it
  entirely. The mixed-provenance and unlabeled-production cases remain **unexercisable**, not
  merely unexercised, until **T136** lands: every currently-warmed county is staging-sourced, so
  there is no data today that could produce either state.

---

## Decisions made

- **T133's fix shape: strip-with-log at render time.** Authoring-time validation was **declined,
  not deferred** — the valid-token set is context-dependent (a token legitimate on an ICHRA
  proposal is absent from the map on a COBRA one), so it cannot be complete, would emit false
  warnings on correct content, and does nothing for rows already in the database.
- **T137's fix shape: per-option labeling, never filtering.** Chosen over a page-level banner for
  two reasons: the banner already exists on the results path (verified at servlet `:523-539`,
  JSP `:218-224`), and the defect only bites in the **mixed** state that does not exist yet.
  Built for a state the data cannot yet produce.
- **The SLF4J logger S13-A introduced stays, as a known one-off — not a precedent.** S13-A
  reported CLAUDE.md's `LoggerFactory` convention as implemented nowhere in the codebase. That
  was true of the grep it ran (SLF4J only) and materially misleading: AMS logs via **Log4j2's
  `LogManager.getLogger` in 31 files**, including `GroupConversionServlet` itself. S13-A's fix
  therefore introduced a **third** logging style into a file/codebase that already had two
  (`System.out.println` and Log4j2 `LogManager`), and the choice to add SLF4J was made on
  evidence framed wrongly. **Decision: leave it.** The runtime walk proved the WARN reaches
  `ams.log` correctly (the SLF4J→Log4j2 bridge works), so the behavioral question is settled by
  evidence rather than by which framework is nominally house style. Reverting would cost a
  release cycle to change nothing observable. Recorded here so a future session does not treat
  SLF4J as the newly-blessed convention and propagate it elsewhere without the same bridge
  guarantee being re-checked.

---

## New backlog row filed

**T139** — confirmed unused (0 occurrences in `project_backlog.md` before assignment; T138 was
the prior high-water mark, filed by S13-B).

> **`ViewHome25` throws `NullPointerException` on the start-renewal path when a to-do list is
> null.** `ViewHome25.java:151` — `for (ToDoOut25 t : local.getCurrentActivity().getToDoList())`
> — iterates the to-do list with no null guard. `AmsDataLocal.CurrentActivity.intializeActivity`
> (`AmsDataLocal.java:1189-1192`) returns early without ever calling `setToDoList(...)` when
> `EntityLookup.getActivityById` finds no matching activity, leaving the field at its default
> `null`. Reachable via `UpcomingRenewals25.handleStartRenewal` → `AddRenewal25.doPost` →
> `AddRenewal25.forwardToView` (`getNamedDispatcher("ViewHome25").forward(...)`, which preserves
> the POST method) → `ViewHome25.doPost:43` → `processData` → the unguarded loop at `:151`. Call
> chain and null-path verified against source this run. **Pre-existing and entirely unrelated to
> this session's shipped work** — noticed while reading logs during T133 verification, per the
> session narrative; not independently re-confirmed against a live log by this documentation-only
> run. Priority as judged; planned.

⚠️ **The specific claim "observed twice in local logs on 2026-08-04" is carried from the session
narrative, not independently verified by this run** — this run read source to confirm the defect
is real and reachable, but did not inspect Tomcat/`ams.log` output itself. See the compliance
note below.

**T138 needs no new row** — filed by S13-B already. Per the session narrative, it was **confirmed
on production** this session: the deployed `/GroupConversion` GET page shows no staging banner at
all, exactly as the row describes (`setProvenanceAttributes` is called only from the
results-computation path, never from `doGet`). This confirmation is not independently re-derivable
from the repo — recorded as narrative, not repo fact.

---

## Lessons — both about prompts, not code

Two hard stops fired this session. **Both were reported before anything was written. Both were
defects in the run prompt, not the codebase.** Treat a hard stop as a successful outcome.

1. **S13-A — the scope fence contradicted itself.** The permitted list said "the file containing
   `replaceTokens`"; the forbidden list named `ViewProposal.java` "for any reason." They are the
   same file — the prompt author had assumed the method lived elsewhere. Resolved by ruling the
   permitted clause authoritative and confining the diff to the method body plus the logger
   field/import it required.
2. **S13-B — a hard stop was written as a proxy for its own risk, not the risk itself.** Stop #2
   said "stop if `loadAvailableCounties` feeds validation." It does (`:176-184`). But the actual
   hazard was a **reshaped return breaking a non-dropdown consumer**, which Step 2's own
   constraint 6 already forbade doing. The stop should have read: *stop if the return shape must
   change **and** there are consumers beyond the dropdown.* **A hard stop must name the condition
   that makes the work dangerous, not a proxy correlated with it** — the proxy and the real
   condition can come apart, as they did here.

Also worth carrying forward as a verification habit: **S13-B tested a claim that could have been
reasoned about instead.** The entire fail-toward-labeling rule for mixed-provenance counties
rested on `MAX('PRODUCTION','STAGING')` returning `'STAGING'`. Rather than reasoning about
lexicographic ordering, S13-B **executed that exact expression against MySQL** and read the
result. Cheap, and it is the same discipline this project has been repeatedly burned by skipping.

---

## Environment findings

- **The local database is behind, and stays behind — this is structural, not a one-time gap.**
  `schema_version` reads **V086** locally; **V087 and V088 are unapplied**, which produces the
  caught-and-logged EclipseLink error (`beta_ssa.proposal_ichra_intake` does not exist) seen on
  proposal views. This is not a "run these migrations" fix-once item: `docs/analysis/
  local_render_verification.md` documents a **weekly** Sunday-03:05 scheduled task that restores
  local `beta_ssa` from a fresh production dump, which erases any locally-applied migration that
  is not also in production. Any future note reading "apply V087/V088 locally" has a shelf life
  of about one week from whenever it is written.
- **Six stale migration scripts (`V066`–`V071`, long since applied to every environment) sit in
  the local, untracked `release\` staging folder.** Attaching that folder's contents wholesale to
  a GitHub release would hand `update.sh` migrations it has already applied. Release attachments
  must be `ROOT.war` only, unless a genuinely new `V0NN__*.sql` exists for that release — true of
  `v0.88.03`, which carries none.

---

## Carried forward, unchanged

⚠️ **Everything in this section is from the session narrative — the conversation and Kevin's own
screen — not independently re-derived from the repository by this run,** except where a specific
repo citation is given. Recording it is the point of this section: session 12 nearly lost S12-A's
findings entirely because they existed only in one conversation.

- **No reply from Forrest as of this session**, per the session narrative. **⚠️ Repo-verification
  note: `docs/analysis/forrest_call_checklist.md`, referenced as "remains prepared," does **not**
  exist anywhere in the working tree, in untracked files, or in `git log --all` history under any
  path.** Either the file was never committed, lives outside this repository, or the reference is
  to a document that has not yet been created. Recorded as a discrepancy rather than silently
  treated as fact. **A reply from Forrest outranks any build item**, per the same narrative.
- **`docs/ichra_strategy.md` remains stale and was NOT edited this run** — it is Kevin's file;
  flagged only. Known discrepancies, carried from the narrative: §4's *"What a user can do today:
  nothing"* is false — the Illustration, `/GroupConversion`, the Market page, and
  `ICHRA_ILLUSTRATION` proposal sections have all shipped since it was written; §2 states the
  Forrest email was unsent as of 2026-07-31 (it has since been sent, per the narrative above);
  §6's endpoint list omits EnrollConnect.
- **Everything blocked on HealthSherpa is unchanged this session** — confirmed against
  `docs/session_closeout_2026-08-03_session12.md:186-191`, which this run read rather than
  re-deriving:

  | Blocker | Last dated | Status (session 12) |
  |---|---|---|
  | Production allow-listing | — | Binding constraint on T130/T137's happy path (T136) |
  | Onboarding representative assignment | 2026-07-29 | *"Asked, not yet answered."* |
  | Staging Basic Auth for the deeplink | — | Routed through the same unassigned representative |
  | BAA with Geozoning, Inc. | 2026-07-29 | *"Unaddressed by anyone so far."* |
  | Webhook authentication methods | — | Same unassigned-contact blocker |
  | BCBS TX policy-status timing | 2026-07-29 | No |
  | CHRISTUS policy-status timing | 2026-07-29 | No |

- **`Proposal.dateCreated` renders blank — the diagnosis narrowed further this session, by
  inference from T133's own verification.** Session 12 had already established the column is
  `insertable = false`, with population depending entirely on the database's own `DEFAULT
  CURRENT_TIMESTAMP`, confirmed present only in the pre-migration baseline dump and never in a
  tracked migration (`docs/session_closeout_2026-08-03_session12.md:164-166`). T133's walk
  (S13-A) re-rendered an existing proposal and confirmed **every token on it matched the map** —
  no WARN fired. That proves this is a **matched-but-null** problem, not a missing-token one,
  consistent with session 12's finding. `SHOW CREATE TABLE proposal` on production remains the
  settling check; not run this session (would require a production connection outside this run's
  scope).
- **The dark-on-dark heading cause remains unexplained** — confirmed against
  `docs/session_closeout_2026-08-03_session12.md:152-156`: the *symptom* is fixed (S11-I's
  defensive rule), and the *print* variant is fully explained (`print-color-adjust`), but the
  original browser-render cause is not. **Do not record "Bootstrap did it" as settled** — S11-I
  verified `--bs-heading-color` defaults to `inherit` in the vendored CSS, which does not explain
  the observed behavior.
- **LA-17 still prohibits a session-based PSP-admin gate on the public proposal path**, by name,
  in writing (`docs/session_closeout_2026-08-03_session12.md:203-204`). Do not re-propose the
  rejected shape; S11-D hard-stopped on exactly this design already.

---

## Next

**Recommend T136 first**, because it is the single item that unblocks verification for the
largest number of already-shipped, code-verified-only features at once: T130 (market-data merge
tokens), T137 (this session — the labeling logic cannot show its own unlabeled case without it),
and the conditional Market page (S11-H) all become independently testable the moment production
HealthSherpa access exists, with **no code change required** on any of them per their own
close-outs. It is external and not something a build session can resolve directly, but every
session that passes without progress on it leaves the same pile of code-verified-only claims
un-upgradeable.

Second: **T138** — the `/GroupConversion` banner gap. Small, well-scoped by S13-B's own close-out,
and the design question it raises (what should a *pre-selection* page even claim about
provenance) is cheap to answer now while the page is simple, and gets more expensive the more the
page grows.

Third: the **agency-branded Market template** remains unstarted and needs Kevin's input on what an
agency template should contain before it can be scoped at all — not a Claude Code task until that
input exists.

All remaining ICHRA capability work continues to wait on HealthSherpa, per the blocker table
above.

---

## SQL close-out audit — mandatory section, empty by design

**Session 13 produced no SQL** — across S13-A, S13-B, and this close-out run. Stated explicitly
rather than the section being omitted.

- **SQL statements produced:** none, in any of the three sub-runs.
- **SQL statements run:** S13-A and S13-B each ran a small number of **read-only `SELECT`s**
  against the **local** `beta_ssa` copy during discovery/verification (documented individually in
  their own close-outs). **No `INSERT`, `UPDATE`, `DELETE`, or DDL** in any sub-run.
- **Orphaned `.sql` files:** none. `git diff --name-only 82c8496..HEAD` — spanning all of this
  session's code and doc commits — contains zero `.sql` paths.
- **Current highest migration version — read from `docs/migrations/` this run, not recalled:**
  **`V088__proposal_ichra_intake_contribution.sql`**. Unchanged by any part of session 13.
- **Pending deployment:** nothing from session 13 requires a migration; `v0.88.03` shipped
  WAR-only, correctly, since none exists to attach.
- **What "pending" means for V087/V088 is a LOCAL environment state only, not a production one.**
  Local `schema_version` reads V086 because the weekly refresh restores production's schema and
  V087/V088 were applied to production (and are present in the repo) but not reapplied locally
  since the last refresh. Production is not behind; the local dev database is, and always will be
  again one week after anyone fixes it by hand. See Environment findings above.
- **Schema described but not scripted:** none identified this session.

---

## Compliance statement

**Scope fence, restated:** permitted to write only this close-out
(`docs/session_closeout_2026-08-04_session13.md`, new) and **one new row** in
`docs/analysis/project_backlog.md` (T139, above). No source file, no JSP, no `.sql` file, and no
existing close-out was opened for editing — each was read only. `docs/ichra_strategy.md` was not
edited. **Confirmed nothing outside this set was written** — see the git diff in the commit below.

**Every hash cited in this document was read from `git log` during this run**, via
`git log -1 --format=%H` / `git log --oneline <range>` / `git merge-base --is-ancestor` — none
carried from the prompt's narrative, none a placeholder. This close-out's own commit hash is
recorded in the follow-up commit per the standing convention, also read from `git log` after the
push, not written in advance.

**Which claims came from the repo versus from the prompt's narrative — stated explicitly for
everything load-bearing:**

| Claim | Source |
|---|---|
| The three commit hashes, their content, and `v0.88.02`/`v0.88.03`'s composition | **Repo** — `git log`, this run |
| `bcc1417` is deployed | **Repo** — `git merge-base --is-ancestor`, this run |
| T133's three-test walk and its exact log line | **`docs/session_s13a_closeout.md`**, itself a repo artifact from a runtime walk |
| T137 code-verified / production-observation split | **`docs/session_s13b_closeout.md`** |
| The SLF4J/Log4j2 correction | **`docs/session_s13b_closeout.md`**, itself citing a 31-file grep run during S13-B |
| The two hard stops | **`docs/session_s13a_closeout.md`** and **`docs/session_s13b_closeout.md`** |
| `ViewHome25:151`'s NPE reachability | **Repo** — read directly this run, both the throw site and the null-producing path |
| "Observed twice in local logs" | **Narrative only** — not independently checked against a log this run |
| T138 confirmed on production | **Narrative only** — not independently re-derivable from the repo |
| No reply from Forrest / the checklist file | **Narrative**, and the checklist file **does not exist in the repo** — flagged as a discrepancy, not silently accepted |
| HealthSherpa blocker table | **Repo** — `docs/session_closeout_2026-08-03_session12.md:186-191`, read this run |
| `dateCreated`, dark-on-dark heading, LA-17 | **Repo** — `docs/session_closeout_2026-08-03_session12.md`, read this run; T133's matched-token inference is new synthesis by this run, not copied from either source |

**No forbidden git operation was run**: no `git add -A`, no `git add .`, no `stash`, `checkout`,
`restore`, `reset`, and no local tag. Staging was by explicit named path, both files listed
individually.

**No hard stop fired in this run.** (Two fired in the sub-runs this close-out documents; none in
S13-C itself — preflight passed, and nothing in the permitted set conflicted with anything
forbidden.)
