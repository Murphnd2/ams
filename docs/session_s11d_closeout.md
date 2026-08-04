# Session 11-D close-out — watermarked staging preview for the market-data page

**Run:** S11-D (Opus). **Outcome: HARD STOP at Step 2. No code was written. No feature was built.**

**Branch:** `refactor/modernize-architecture` (trunk).

**One-line summary:** the run's specified mechanism — a session-based `isPspAdmin` check on the public
proposal render path — is prohibited by name, in writing, in two separate places in this repository,
one of which explicitly predicted its reintroduction. The prohibition was found during Step 2 discovery,
which is exactly what Step 2 exists to do. The finding is this run's deliverable.

---

## Baseline

- **Hash at preflight (post-pull):** `123114ada5fa5a60dd161864c29103dacb1a86b7`
- **Hash at end:** unchanged at `123114ada5fa5a60dd161864c29103dacb1a86b7` for all source and doc files —
  **this close-out is the only commit this run produced.** Its hash is recorded below, read from
  `git log` after push.

Preflight passed all four gates: branch was `refactor/modernize-architecture`, `git status --short` was
empty, and `git pull --ff-only` reported "Already up to date."

**Incidental finding at preflight:** the pull fetched a new tag, **`v0.88.00`** — S11-A's release
(V088 + the T80 half-1 contribution work) has been cut and published by Kevin. Noted because the
standing memory warning is that an un-fetched local `git tag` lies; this run fetched, so this is
current as of 2026-08-03.

## Step 2 — discovery findings, in full

### 2.1 Is `/proposal/{guid}` behind `LoginFilter`, or publicly reachable by GUID?

**Publicly reachable. It is explicitly allow-listed.** `LoginFilter` maps `@WebFilter("/*")`
(`src/main/java/net/superiorstate/ams/LoginFilter.java:16`), so it does see every request to the path —
and then deliberately waves this one through at `:87`:

```java
boolean allowedPath = ALLOWED_ENDPOINTS.contains(path) || path.startsWith("/proposal/")
        || path.startsWith("/apply/") || path.startsWith("/q/") || ... ;
if (loggedIn || allowedPath) { chain.doFilter(req, res); }
```

`/proposal/` is not in the `ALLOWED_ENDPOINTS` set — it is a separate `startsWith` clause on the same
line, alongside `/apply/` and `/q/`. A prospect holding only the GUID reaches the page with no session
and no credential of any kind.

### 2.2 Does the request carry an authenticated session? Can the servlet identify a PSP admin?

**`ViewProposal.java` reads no session state anywhere in the file.** A grep for
`getSession` / `isPspAdmin` / `AmsDataLocal` across the whole servlet returns **zero matches**. It
obtains persistence from the servlet context (`request.getServletContext().getAttribute("emf")`,
`:72`), sets request attributes, and forwards — it never touches `HttpSession`.

**Technically, it could be made to.** `request.getSession(false)` is available on any request, and a
PSP admin who is logged into AMS in the same browser would carry a session with the `isPspAdmin`
attribute set. So the narrow, literal question "could the servlet distinguish an admin from a prospect"
has the answer *yes, the mechanism exists*. **That is not the same question as whether it should**, and
§2's hard stop turns out to rest on the second question, not the first — see "Why the stop fired" below.

### 2.3 Is the same JSP reached by any second, authenticated route?

**No. There is exactly one route.** `viewProposal.jsp` is dispatched from a single place in live source:
`ViewProposal.java:378`. The only other grep hits are stale copies under `.claude/worktrees/` and
`out/artifacts/` — build/worktree artifacts, not live source. `buildTokenMap` likewise has exactly one
caller (`ViewProposal.java:355`).

**Consequence:** there is no existing authenticated surface to attach a preview to. Any authenticated
preview is new construction, not a modification of something already present — which is why the
recommended alternative below is a separate run rather than an in-scope adjustment.

### 2.4 The canonical PSP-admin check used elsewhere

Two established idioms, both in wide use, no new role test needed and no hardcoded role id:

- **Strongest form** — `local != null && local.isAuthenticated() && local.isPspAdmin()`, where
  `local = (AmsDataLocal) request.getSession().getAttribute("local")`. Used at
  `ProposalAiBuilder.java:116` and `AutomationAiBuilder.java:91`.
- **Session-attribute form** — `Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"))`.
  Used at `CreateUser25.java:64`, `ViewHome25.java:219`, `MonthlyBillingLauncher.java:144`,
  `RateCacheAdmin.java:168`, and ~20 other call sites.

Recorded for the follow-on run's benefit. **Neither was used in this run, because no code was written.**

---

## Why the stop fired

The hard stop as written tests a conjunction: *"a prospect can open `/proposal/{guid}` with no session,
**and** the servlet therefore cannot distinguish a PSP admin from a prospect on that request."* The
first half is definitively true (2.1). The second half, read literally, is **arguably false** — the
servlet could read a session (2.2).

**I stopped anyway, on a stronger finding that Step 2 surfaced.** Stating this plainly because it
matters for how the next run treats this document: the stop did not fire on the prompt's literal test.
It fired because discovery found that the specified mechanism is already prohibited, by name, in code
and in the legal register — and because the scope fence forbade touching either place where that
prohibition lives.

### The prohibition, quoted

**1. `IchraAccessResolver.java:113-118`** — javadoc on `isAvailableForProposal`, the resolver
`ViewProposal` already calls on this exact page:

> **There is deliberately no `isPspAdmin` bypass, and none may be added.**
> `isAvailable(EntityManager, HttpServletRequest)` short-circuits on the session's PSP-admin attribute,
> which is correct on an authenticated agent surface and **wrong here**: a PSP admin who happens to be
> logged in and opens a public proposal link must not thereby cause employer-facing market data to
> render in a document sent to a prospect. **The audience of this page is the employer, never the
> viewer's own session. This method reads no session state of any kind.**

**2. `docs/analysis/legal_assumptions.md:1069-1070`** — LA-17, the entry that governs market data on
employer-facing proposals:

> **The PSP-admin bypass must not be carried across**: on a public page there is no admin, and
> **a bypass that cannot fire is a bypass waiting to be reintroduced by someone who does not know why
> it was absent.**

That sentence was written 2026-08-02. **This run is the reintroduction it predicts.** Recording that
explicitly, because the register earned the point and the next run should not have to rediscover it.

### The substantive reason, not merely the documentary one

A session check answers *whose browser issued the request*. It does not answer *who is reading the
document*. On this page those two diverge, and the divergence is the documented sales motion rather
than an edge case:

- **The PDF is the admin's own browser print.** S11-C established there is no server-side PDF renderer
  anywhere in this project — the customer-facing PDF is produced by whoever prints the live page. A PSP
  admin who previews and saves-as-PDF bakes staging figures into a file that then travels. The run
  prompt concedes this directly: *"a watermarked PDF still travels, and a screenshot can crop the
  watermark off."*
- **The in-the-room case is real and on the backlog.** T78 records agents working *"across a desk from
  an employer, on a phone."* An admin-authenticated device showing staging dollar figures to an
  employer in the room satisfies the session gate perfectly while defeating its entire purpose.

By the run prompt's own standard — ***"a gate that a prospect can trip is not a gate"*** — an employer
reading the admin's screen, or the admin's printed PDF, trips it. The gate constrains who *loads* the
page; the compliance risk is about who *sees* the figures.

### The scope-fence problem this created

The fence forbade modifying `IchraAccessResolver` and did not include the ability to revise LA-17's
prohibition. Building the feature would therefore have shipped code **directly contradicting an
unmodifiable, explicitly-worded compliance control located in its own call path**, with no way to
update that control to reflect a new decision. That is not a defect I can write my way out of inside
this fence — it is a decision to reverse, and reversing a recorded compliance control is Kevin's call.

---

## Shipped

**No code. No feature. No migration.** This close-out document is the only artifact this run produced;
its commit hash is recorded at the top and in "Decisions made" below.

## In flight

Nothing uncommitted. `git status --short` was empty at preflight, remained empty through all of Step 2
(which is read-only by construction), and contains only this close-out at commit time.

## Decisions made

- **Stopped rather than built**, and escalated to Kevin rather than choosing unilaterally. Reversing a
  compliance control that a prior session deliberately recorded — with reasoning, and with an explicit
  "none may be added" — is not a judgment this run was scoped to make.
- **Wrote no `LA-NN` entry**, despite `legal_assumptions.md` being in the writable set. §4c's assumption
  registration describes an assumption *made by a shipped design*. Nothing shipped, so there is no
  assumption to register. Adding an LA entry for an unbuilt feature would corrupt the register's meaning
  — it records what the running system assumes, not what a run considered.
- **Wrote no backlog update**, though `project_backlog.md` was writable. Kevin's instruction on stopping
  was specific: write this close-out, commit, push, stop. T136's status is unchanged by this run in any
  case — the production allow-listing dependency is exactly where it was.
- **Confirmed the direction for the follow-on run** (Kevin, this session): the authenticated-route
  alternative below is the direction, but **it is not to be built until a new prompt scopes it.**

## New assumptions

**None.** Nothing was built, so nothing new is assumed. The run's *existing* assumptions were tested
against source and one failed — see "Contradictions found".

## Open questions raised

- **Does Kevin wish to supersede LA-17's no-`isPspAdmin`-bypass rule for the staging-preview case?**
  Settled only by Kevin. If yes, it needs a run with `IchraAccessResolver` **and** LA-17 in scope, so
  the javadoc and the register are updated to match the new decision rather than silently contradicted.
  As of this session's answer: **no — the authenticated-route alternative is preferred instead.**
- **Are there in fact zero `PRODUCTION`-sourced rows in `rating_area_rate_cache` today?** The run
  premise asserts it; this container has no database and cannot check. Immaterial to the stop (the
  prohibition holds either way), but it is the premise behind "the page reads as broken."

## Contradictions found

- ⚠️ **The run prompt's core design contradicts `IchraAccessResolver.java:113-118` and
  `legal_assumptions.md:1069-1070`**, both quoted above. This is the finding that stopped the run. The
  prompt's §4a specifies a `stagingPreviewOk` branch conditioned on "the viewer is a PSP admin by the
  canonical check from Step 2.4" — that is precisely the bypass both documents forbid by name.
- **The prompt's §2.2 phrasing anticipates the wrong failure mode.** It frames the risk as "the servlet
  cannot identify the viewer." The actual risk is that the servlet *can* identify the viewer, and that
  identifying the viewer is the wrong basis for deciding what an employer-facing document contains.
  Worth correcting in any successor prompt so the next run does not re-derive it.

## ⚠️ Code-verified-only disclosure

**Everything above is code-verified only, and less than usual even by that standard** — no build was
run (correctly, since nothing changed), no Tomcat, no database. Every finding comes from reading source
files, `pom.xml`, and the tracked legal register. Specifically **not** verified at runtime: that a PSP
admin's session actually reaches `ViewProposal` intact on the `/proposal/{guid}` path, and that zero
`PRODUCTION` rows exist. Neither affects the stop.

## Next

**Recommended, and confirmed as the direction by Kevin this session — build the preview on a new
authenticated route, in a new run.**

Shape: a PSP-admin-gated servlet (e.g. `/ProposalPreview?id=N`) that sits **behind** `LoginFilter`
rather than inside the public allow-list, renders the same JSP, and passes staging figures plus the
watermark. Why this preserves what the current design protects:

- **The public document stops varying by viewer entirely.** `/proposal/{guid}` keeps its session-free
  render path, and LA-17's invariant — *"the audience of this page is the employer, never the viewer's
  own session"* — survives untouched and literally true.
- **A prospect cannot reach the preview at all**, rather than reaching it and being turned away by a
  check inside a public page. `LoginFilter` redirects an unauthenticated caller to `/login` before any
  servlet runs. That is a gate a prospect cannot trip, which is the standard the run prompt itself set.
- **`IchraAccessResolver` needs no change**, so the prohibition stays intact and correctly worded.

Still true, and unchanged by any of this: **the watermark must carry `print-color-adjust: exact` and
its `-webkit-` variant inline**, per S11-C's finding that browsers drop backgrounds when printing and
that nothing in this codebase opts proposal content back in. And the residual risk the run prompt named
honestly still applies to the preview route — a printed PDF travels, and a screenshot can crop a
watermark. That risk is worth registering as an `LA-NN` entry **when the feature actually ships**, not
before.

⚠️ **T136 is unchanged.** Production HealthSherpa allow-listing remains the real blocker for the
production path; nothing in this run moved it.

## SQL close-out audit

- **This run produced no SQL. Stating that explicitly, as required.** No migration was written, none was
  run, and none is recommended. No `.sql` file was created, modified, or orphaned.
- **Current highest migration version:** **V088** — `ls docs/migrations/*.sql | sort | tail -2` returns
  `V088__proposal_ichra_intake_contribution.sql` (plus the long-standing non-versioned
  `seed_ndt125_questionnaire.sql`).
- **Pending deployment:** V088 was pending at S11-A's close; the `v0.88.00` tag fetched during this
  run's preflight indicates it has since been released. Per-environment status remains
  `docs/analysis/migration_tracker.md`'s to record — **this run did not update it**, having verified
  nothing about production state from inside a container with no database.
- **Schema described but not scripted:** none.

## Compliance statement

Scope fence for this run was writable: the file containing `buildTokenMap` (`ViewProposal.java`,
market-token block only), `docs/analysis/legal_assumptions.md`, `docs/analysis/project_backlog.md`, and
`docs/session_s11d_closeout.md`. **Of those four, exactly one was written — this close-out.**
`ViewProposal.java`, `legal_assumptions.md` and `project_backlog.md` are byte-identical to their state
at the baseline hash.

Forbidden list, confirmed: **the production path's behavior is untouched by construction** — no source
file changed, so with every row `PRODUCTION` the output is not merely equivalent but identical, being
the same bytes. `replaceTokens` and unmatched-token handling (T133): not modified. The intake tokens,
contribution tokens, entitlement resolver and `IchraAccessResolver`: read only, not modified. Any
rate-fetch, cache-write, or `source_env` write path: not touched — this run did not even read
`sourceEnv` at runtime, only inspected the code that does. No migration, no SQL.

No forbidden git operation was run: no `git add -A`, no `git add .`, no `stash`, `checkout`, `restore`,
`reset`, and no local tag. Staging was by explicit named path. No build was run.
