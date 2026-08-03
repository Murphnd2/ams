# Session 9 close-out — 2026-08-02

**Branch:** `refactor/modernize-architecture`. **Runs:** S9-A through S9-H (eight runs; there is no
S9-C in git — it was release prep for `v0.85.11` and an edit to `MEMORY.md`, which is not in this
repository; see §3.7). **This close-out (S9-Z) is documentation only.**

⚠️ **This document's own commit hash is not recorded here** — it cannot be, since the hash does not
exist until after this file is committed. It is the commit immediately following `091ed99`.

---

## 3.1 — Shipped

Every commit from S9-A through S9-H, read from `git log --format="%h %ci %s" 123d6d4^..HEAD`. Grouped
by run, in commit order.

**S9-A** — decisions recorded, no code:
- `123d6d4` — `docs` — LA-17: agent-composed, agent-sent proposals may carry market data
- `aa036b4` — `docs` — D39: ICHRA+ and QSEHRA+ are two separate lines of service
- `983021f` — `docs` — backlog T116–T119 filed

**S9-B** — the entitlement gate, first code of the session:
- `611d7b1` — **SRC** — `IchraAccessResolver.isAvailableForProposal`, session-free entitlement
- `1a41852` — **SRC** — gate ICHRA content on the public proposal view (LA-17 / T116)
- `334f9da` — `docs` — close T116, file T120/T121, confirm T64

*(S9-C ran between S9-B and S9-D: release prep for `v0.85.11`, and a correction to `MEMORY.md`.
Produced no commit in this repository — see §3.7.)*

**S9-D** — runtime-tested the multi-membership branch, probed plus-tier LOS keying:
- `9c46b46` — `docs` — T120 runtime-settled, T122 filed, T64 second observation

**S9-E** — the inert flag:
- `6463b17` — **SRC + MIGRATION** — V086 `los.is_plus_tier`, entity field, admin checkbox
- `a5f103d` — `docs` — file T123, update T122

**S9-F** — access control, first pass:
- `07f722a` — **SRC** — guard `ServiceManagerAction`/`ServiceManagerHome` (T123)
- `dbc77f0` — `docs` — file T124 (systemic sweep), T123 partial

**S9-G** — access control, worst case:
- `c89e878` — **SRC** — guard `RateTableAction`, `PspAdminHome`, `ServiceManagerSort`, `ServiceModuleAction`
- `b78948e` — `docs` — close T123, record the T124 N-patches decision

**S9-H** — access control, remainder; auth reconnaissance:
- `820d027` — **SRC** — guard `PriceItemAction`, `LibraryAction`, `LibraryHome`,
  `QuestionnaireManager25`, `PspDashboardHome`, `PspAgencyHome`
- `091ed99` — `docs` — close T63, record production verification of the S9-F/S9-G guards

**Totals: 15 commits. 6 touch `src/`. 1 carries a migration (V086). 9 are documentation only.**

---

## 3.2 — Released

**`v0.86.00` = `b78948e`.** Contains **V086's migration** (`6463b17`) and **six guards**
(`07f722a`, `c89e878`) — `ServiceManagerAction`, `ServiceManagerHome`, `RateTableAction`,
`PspAdminHome`, `ServiceManagerSort`, `ServiceModuleAction`. Confirmed by `git tag --contains 6463b17`
and `git tag --contains 07f722a` / `c89e878`.

**Deployed by Kevin.** He logged in, opened Service Manager, opened Rate Manager, and edited a price
successfully.

⚠️ **Verification scope, stated precisely — these are different claims:**
- ✅ **Verified:** a PSP admin is **not locked out** of any of the six guarded surfaces.
- ❌ **Not verified, and cannot be from this test:** an **authenticated non-admin** gets `403`.
  Production has no non-admin account available to check with, and nobody would deliberately probe an
  admin URL from a live agent session to find out.

⚠️ **Caveat on the price-edit confirmation, worth carrying forward.** `rateManager25.jsp:574` does
`fetch('RateTableAction', …).then(r => { … })` **without checking `r.ok`**, and `fetch` does not
reject on an HTTP error status — only on network failure. So if the guard had rejected the request
with `403`, **the displayed price would still have updated**, because the `.then()` branch runs
either way. **The visual confirmation alone is weak by construction.** What makes the edit
*near-certain* to have actually saved is a different fact: the `doGet` guard on `PspAdminHome` also
passed — Kevin reached the page at all, which requires the identical `isPspAdmin` check the `doPost`
guard uses. A page that loads and a save that appears to work are consistent with the guard passing
cleanly; they are not proof of it on the write side alone.

**Unreleased:** S9-H's six guards, commit `820d027` — `PriceItemAction`, `LibraryAction`,
`LibraryHome`, `QuestionnaireManager25`, `PspDashboardHome`, `PspAgencyHome`. **Next release
`v0.86.01`** — a patch, not a minor bump, since it carries no migration.

---

## 3.3 — In flight

**None.** Every run committed and pushed before ending; `git status --short` was clean at the start of
every subsequent run's preflight, all the way through this close-out's own preflight.

---

## 3.4 — Decisions made

### Recorded in the repo — verify by location, not by trusting this document

| Decision | Location |
|---|---|
| LA-17 (agent-composed, agent-sent proposals may carry market data) | `docs/analysis/legal_assumptions.md:998` |
| The entitlement-gating subsection (LA-17's enforcement mechanism) | Inside the LA-17 entry, same file |
| D39 (ICHRA+/QSEHRA+ are two lines of service) | `docs/analysis/plus_tier_build_plan.md:2016` |
| T116's multi-membership rule (strictly the originating agency) | `docs/analysis/project_backlog.md`, T116 row |
| T124-as-N-patches | `docs/analysis/project_backlog.md`, T124 row, attributed to Kevin, 2026-08-02 |

### Conversation-level — no repo record. Recorded here so they are not lost.

**1. The proposal is tiered by input fidelity, like the analysis.** Tier 1 (name, ZIP, headcount) is a
*consultation-invite* proposal: how the mechanism works, what it costs the employer, an invitation to
talk. Tier 2/3 carries premium ranges, plan and carrier counts, the contribution slider, and
affordability. **Tier 1 is not a degraded artifact** — for the QSEHRA/PremiumPath no-group-plan motion
it is the complete product, because that motion does not need a plan-level number to be true.

**2. Product determination is an output of the design, not an input.** The agent is not asked "ICHRA
or QSEHRA?" at first contact. It falls out of contribution size against the QSEHRA cap, headcount,
whether classes are needed, whether any group plan is retained, and how subsidy-heavy the workforce
is. **The tier-1 proposal should name no product.** Conversions are not always ICHRA — a sub-50
employer dropping its group plan is QSEHRA-eligible, and a lean-contribution conversion with a
subsidy-heavy workforce is a real QSEHRA case, not an edge case. *(Filed structurally as T119, which
records the design-note half of this; the product-naming rule itself is conversation-only.)*

**3. ⭐ Kevin's layered gating model — this supersedes the "mark the section vs. mark the LOS" debate
from S9-D/S9-E entirely.** Three layers, each doing a job the others cannot:

- **Rate package → LOS selectability.** An agency can only select an LOS its rate package carries.
  **This is already built** and is the real protection for the four active `CUSTOM` sections T122
  flagged (`PremiumPath`, `QSEHRA Lite`, `HSA Plus`, `BenefitBridge`) — they are `SCOPED` to LOS
  `135534`, and an unentitled agency's rate package simply does not offer that LOS. **T122's HIGH
  severity was filed before this was understood** and has not yet been revisited against it (see
  below).
- **`los.is_plus_tier` (V086) → a workflow trigger, not a visibility marker.** When a plus-tier LOS is
  selected in the Proposal Builder and the agent is ICHRA-entitled, the builder interjects with ZIP +
  headcount before Create Proposal.
- **Data presence → the render gate.** No intake, no snapshot, nothing renders. This is what T116/S9-B
  already built.
- **T116's entitlement gate is the backstop** for the case where all three of the above are
  misconfigured — belt-and-suspenders, not the primary mechanism.

**Why this beats a single marker on `LOS` or on `proposal_section`:** `proposalsectionlos` is
many-to-many, and sections `HRA 1`/`HRA 2` link **both** an ordinary active `HRA/MERP` LOS and a
suppressed `ICHRA_` LOS on the same section row. Any render-time rule that infers gating purely from
LOS association either withholds ordinary HRA pages or leaks ICHRA content — there is no single
threshold that gets both right. **Asking the LOS at selection time, in the builder, is unambiguous**
because it never has to disambiguate a section carrying two LOS types at once.

**The failure mode this closes:** an unentitled agent with a plus-tier LOS available (because rate
scoping is misconfigured) sees no interjection → no data is collected → no plus-tier sections have
anything to render → the proposal is administration-only. **Fails closed without hiding anything from
the agent** — nothing looks broken, it just doesn't offer what it can't back up.

⚠️ **T122 has NOT been amended to reflect this decision.** Checked directly against the current
backlog while writing this close-out: T122 still reads **HIGH, "decision needed, not code,"** with no
reference to rate-package scoping. **The next session should either re-file T122's severity in light
of this, or explain why the existing protection is not sufficient** — that re-evaluation did not
happen this session and should not be assumed to have happened.

**4. ⭐ The builder interjection and T74's ZIP intake are the same intake.** The sale-motion walk
stalled at stage 1 on *"the agent has name, ZIP and headcount, and no surface serves it."* Putting
that intake inside the Proposal Builder answers it on the existing pipeline rather than through a new
front door. **First contact happens in the builder.** *(T74 itself, `/Illustration`'s ZIP handling, is
already `✅ Done` per the backlog — this decision is about reusing that same intake shape inside a
different servlet, not about T74's own status.)*

**5. `QuestionnaireInstanceAction` must not be guarded on `isPspAdmin` (S9-H).** It needs an
activity-scoped authorisation model instead. It is the counter-example to the filter/base-class
convention idea floated across S9-F–S9-H — a path-list filter keyed on "admin surfaces" would have
guarded this one wrongly by default, since agents and PSP Users legitimately post to it from the
activity-detail panel. **This is the one open decision remaining in T124.**

---

## 3.5 — New assumptions

**LA-17**, full text at `docs/analysis/legal_assumptions.md:998`. Reversal cost is **LOW**, and
specifically *because* no agentless front door exists — V071's per-agency public quote token has not
become a prospect-facing entry point. **It escalates to rebuild the moment one is.**

**Technical assumptions carried this session, not legal ones:**
- **The multi-membership rule** (T116): entitlement follows strictly the originating agency, the same
  resolver call `ViewProposal` already uses for branding — runtime-verified both directions in S9-D.
- **The layered model's reliance on rate-table scoping** (§3.4.3 above): the claim that an agency's
  rate package already restricts which LOS it can select is asserted by Kevin this session and has
  **not been independently verified against code or a runtime walk**. Treat it as an assumption, not a
  confirmed fact, until a run checks it.

---

## 3.6 — Open questions, and who settles them

### Kevin owns

- **The local password for `kevin@superiorstate.net`** — this is T111's real content. **The account
  exists, is active, and has a credential; no account needs creating.** Five runs (S7-F, S9-B, S9-E,
  S9-F, S9-G) reported this as blocked on a missing test account; S9-H found the account was never
  missing — see §3.7.
- **`QuestionnaireInstanceAction`'s authorisation model** (§3.4.5) — activity-scoped, not `isPspAdmin`.
- **`ServiceModuleAction`** — mapped, mutating, called by nothing anywhere in the tree. Delete it, or
  keep it guarded-but-orphaned?
- **`BillingAction` / `GoBillingHome`** — excluded from every T124 run by standing instruction. Still
  unguarded. Kevin's billing domain; his call on whether and when.
- **Cutting `v0.86.01`** — S9-H's six guards, unreleased.
- **T122's severity** in light of the rate-package-scoping decision (§3.4.3) — not re-evaluated this
  session.
- ⚠️ **Three SWBD emails to Forrest, still unsent — verified true, not assumed.** Checked against the
  repo while writing this close-out: `docs/ichra_strategy.md:312-313` still shows *"send me three
  groups renewing next quarter"* as **❌ Not sent**, and the workflow flowchart Forrest offered to
  draw as open. `docs/claude_memory.md:30` corroborates the same two items as of session 5. **Neither
  file has moved since.**
- ⚠️ **`docs/ichra_strategy.md` is stale in the place this session checked, confirmed, not assumed.**
  Lines 84, 267 and 389 still describe T44 (on-exchange LCSP) as open/unfixed — *"2 calls to fix
  T44," "T44's empirical half"* — while `project_backlog.md`'s T44 row has read **✅ Done** since
  2026-08-01. **Flagged, not edited — it is Kevin's file**, per this run's scope fence and the
  standing rule that nobody but Kevin edits it.

### A run settles

- **The authenticated-non-admin 403 check.** Twelve guards now rest on `isPspAdmin` being read
  correctly; production can only ever show the admin side. **22 agent accounts exist locally
  (§3.7)** — the check is reachable the moment any one of their passwords is known. This is the
  highest-value unrun verification in the project right now.
- **The builder interjection** — the actual consumer of V086's flag. It needs its own Phase A, and
  that Phase A **lands directly on a known live defect**: `GenerateProp25` hardcodes LOS ids 5–10 and
  never reads `Proposal.getLosList()` (**T9**), so a naive interjection would silently miss any
  plus-tier LOS outside that hardcoded range.

---

## 3.7 — Contradictions found

Each entry: what was believed, what corrected it.

1. **`MEMORY.md`'s deployment state was stale** (claimed `v0.85.06` undeployed, K3 defect live) —
   corrected in S9-C. **And `MEMORY.md` is not in this git repository at all** — it is Claude Code's
   user-level memory store, unversioned, edited directly with no commit possible.
2. **`AMS_TEST_USER` is read by no code.** One hit repo-wide — a shell variable inside a `curl` recipe
   in `docs/analysis/local_render_verification.md:104`. **Five runs reported its absence from
   `ssa.properties` as the T111 blocker. The absence was real; the inference drawn from it was
   wrong** — no application code was ever going to read a key from that file under that name.
3. **`LOS.suppressed` is a dedicated toggle action, not a checkbox.** Discovered assembling S9-E's
   admin-checkbox pattern: there are **two** house patterns for a boolean flag on this codebase, not
   one — toggle-action for show/hide, checkbox-in-form for capability flags like `ichra_enabled`.
4. **`UserManager` does not create users or set passwords.** Its actions are role/lifecycle management
   only (`addAgentRole`, `deactivate`, etc.). Account creation is `CreateUser25`, and even that only
   **emails an invite** — it never sets a password directly. No admin UI in this codebase sets a
   password.
5. **`viewProposal.jsp` already wraps the entire ICHRA `<div>` in
   `<c:if test="${not empty ichraSnapshot}">`.** S9-B's gate "half (b)" was specified on the premise
   that half (a) alone would leave header chrome rendering — a runtime walk showed that premise false.
   Half (b) was kept anyway, for a different, correct reason (defence in depth, independent of that
   JSP detail), and the code comment was corrected rather than left asserting the wrong justification.
6. **"Latent because SCOPED" is a default, not an invariant.** `ProposalSettings` offers a Display
   Scope radio button that sets `scope='ALL'` on an `ICHRA_ILLUSTRATION` section — one click removes
   the LOS-scoping that S9-A/S9-B's threat model leaned on.
7. **`IchraAccessResolver`'s own javadoc forbids any LOS or `constant` reference**, in absolute terms.
   The build plan's rule 4 describes a growth path toward exactly that. **The code and the written
   plan disagree; the code is the one that governs**, and S9-D's probe (T122) had to route the
   plus-tier re-key to a different class for this reason.
8. ⚠️ **A pushed tag is invisible to a local clone until `git pull` fetches it — observed three
   separate times this session** (`v0.85.11` mid-S9-D, `v0.86.00` mid-S9-H, and again confirmed at
   this close-out's own preflight). **Always pull before listing tags; never infer "not yet released"
   from an un-pulled tag list.**

---

## 3.8 — ⚠️ A working note for the next session

**Six claims asserted by `claude.ai` this session were falsified by a run**, all listed above under
§3.7: the `<c:otherwise>`/chrome justification for S9-B's half (b), "latent because SCOPED," `MEMORY.md`
being in the repo, `IchraAccessResolver`'s javadoc describing a growth path, T122's severity basis, and
the section-flag proposal that predated the layered-gating decision.

**None of the six cost a rebuild** — every one was caught before it shipped wrong, by a run that
verified against the actual repository rather than trusting the prompt's framing. **The pattern
underneath all six is the same:** a fact about the repository was asserted from a project-knowledge
copy rather than read live. The standing rule already covers this — **never assert a repo fact from
project knowledge; read it** — and this session is the concrete evidence for why the rule exists, not
a new rule. Carry the discipline forward; nothing new needs to be adopted.

---

## 3.9 — An honest accounting of where the session went

**Seven of eight runs (S9-B through S9-H, minus S9-C) were access control, not ICHRA.** The pivot was
justified in the moment it happened — `RateTableAction` was live, unauthenticated-adjacent privilege
escalation over **pricing**, discovered mid-sweep, and leaving it unfixed while continuing the ICHRA
build would have been the wrong call. But said plainly: **this is not progress toward the SWBD demo.**

**The ICHRA build itself advanced exactly two steps this session:** T116's entitlement gate (S9-B,
shipped and runtime-verified) and V086's plus-tier flag (S9-E, shipped, deliberately inert). **The
flag's actual consumer — the Proposal Builder interjection — is unbuilt**, and the sale-motion walk
that was supposed to validate the whole design **stalled at stage 1** on the missing intake surface
that the interjection is meant to answer. Session 9 produced the *design* for closing that stall
(§3.4.4) but not the code.

---

## 3.10 — Next

**Two independent threads, in order of leverage per unit of effort:**

1. **Settle the non-admin 403 check first.** It is cheap relative to its payoff — twelve access-control
   guards across two releases rest on an assumption that has only ever been tested from the admin
   side. One of 22 existing local agent accounts, if a password becomes known, closes the gap
   immediately; nothing needs to be built.
2. **Then build the builder interjection**, Phase A first given the known `GenerateProp25`/T9 defect
   sitting directly in its path. This is the item that actually resumes ICHRA progress after a session
   that was mostly security work — and per §3.4.3, re-check T122's severity against the rate-package
   scoping decision before treating the interjection's gating as the *only* protection layer.

`BillingAction` / `GoBillingHome` and `ServiceModuleAction`'s fate stay with Kevin; neither blocks
either thread above.

---

## 3.11 — SQL close-out audit, whole session

| Check | Result |
|---|---|
| **Statements produced** | One migration: `docs/migrations/V086__los_plus_tier.sql` (S9-E). Nothing else produced across S9-A–S9-H. |
| **Statements run** | The V086 migration, against local `beta_ssa` only (S9-E). Every other SQL statement across the session was a read-only `SELECT` for verification or fixture setup/teardown — every fixture write was reverted in the same run that made it, with a printed verification query, per each run's own close-out. **No write was left in place in any local database beyond V086 itself, and no deployed environment was ever touched.** |
| **In a versioned migration** | ✅ Yes — V086 is the session's only migration. |
| ⭐ **Released?** | **Yes.** `git tag --contains 6463b17` includes `v0.86.00` — confirmed again at this close-out. **This is no longer pending.** |
| ⚠️ **Per-environment apply status** | **Not checked this session and not inferable from the tag.** A released tag means the migration *shipped in the WAR*; whether `update.sh` has actually *run* it against Production, Demo, BPO or Master is a separate fact, tracked in `docs/analysis/migration_tracker.md`. **That file was not updated this session and should be, once each environment's apply status is confirmed.** |
| **Orphaned `.sql` files** | None found. |
| **Current highest version** | **V086.** |
| **Schema described but not scripted** | None outstanding — V086 was both described (S9-D's probe) and scripted (S9-E) within the session. |

---

*Prior close-outs: `docs/session_closeout_2026-07-31_session{2,3,4}.md`,
`docs/session_closeout_2026-08-01_session{5,6}.md`,
`docs/session_closeout_2026-08-02_session{7,8}.md`.*
