# Session 12 close-out — 2026-08-03

**Branch:** `refactor/modernize-architecture` · **Span:** `cd04745` (session 11 close) → `effadf3` · **Released:** none this session — **`v0.88.01` is still current, and predates all of session 12**
**Theme:** a short continuation of session 11's sitting, not a new phase. Two runs, both about whether the ICHRA market-data path can be trusted, not about adding capability. The finding: it already could be, mostly — and the one place it couldn't, S12-B correctly declined to force a fix that would have broken the page it touched.

---

## Framing

**Session 12 was two runs in the same working sitting session 11 closed** — not a new session in the
usual sense of resuming after a break, and not a build session. Its subject was **provenance
verification**: does the Illustration page leak staging-derived premiums the way the Proposal
Builder's advisory did before S11-G fixed it? S12-A answered that question read-only. S12-B closed
the one gap the answer actually contained. Neither run added a new capability to the product;
both are hardening and correcting the record around capability session 11 already shipped.

---

## Shipped

Three commits, one code-bearing run (S12-B). **S12-A wrote no file and produced no commit** — a
read-only investigation, exactly as its own scope fence specified.

**S12-A — read-only investigation, "Does the Illustration page fail closed on rate provenance?"**
No commits. Findings recovered in full below, since S12-B's own close-out is currently the only
place they survive on disk.

**S12-B — provenance consistency: amend T48, close the reachable blind dropdown.**
`bcc1417` fix: close Illustration's dropdown labeling gap, amend T48 (S12-B)
`27e3574` docs: S12-B close-out
`effadf3` docs: record the S12-B close-out commit hash

### ⚠️ Release status — read this before walking anything

**`bcc1417` is not in any release. Verified directly, not assumed:**
```
git tag --sort=-creatordate | head -3   →  v0.88.01, v0.88.00, v0.87.01  (no newer tag exists)
git merge-base --is-ancestor bcc1417 v0.88.01   →  false
```
`v0.88.01` = `a6a35c2`, S11-H's close-out commit — it predates S12-A and S12-B entirely. **Every
line of code S12-B shipped is on trunk, pushed, and undeployed.** A production walk today would not
exercise the `IllustrationServlet.pricedCountyFips` fix, the meta-line wording change, or anything
else from this session. The next session must know this before treating any part of session 12 as
live.

---

## S12-A's findings — recovered in full

S12-A was read-only and wrote no file, per its own scope fence. Its findings exist only inside
S12-B's close-out (`docs/session_s12b_closeout.md`) and the conversation transcript that produced
them. **Recovering them here is this run's most important job** — without this, the next session
re-investigates a question that is already answered.

1. **Provenance containment was already sound before this session touched anything.** Three
   independent gates prevent staging figures from reaching a customer, and none were built by
   S12-A or S12-B: the **write gate** (`ProposalBuilder.attachRangeSnapshot`/`attachAgeBandSnapshot`,
   `:598`/`:658` — refuses to persist an `ICHRA_ILLUSTRATION` snapshot from non-production rows,
   regardless of how the write is invoked); the **read gate** (`ViewProposal.java:138` — refuses to
   expose a snapshot on the public proposal page unless it's production-sourced, defense-in-depth
   even if a stale non-production row somehow existed); and the **disabled hand-off button**
   (`illustration25.jsp:991-1002`, `:1402-1413` — "Use This in a Proposal" is a disabled button with
   *"Available once production rates are configured"* whenever `sourceEnv != 'PRODUCTION'`). All
   three landed in the same 2026-07-31 batch of commits, well before session 11 began.
2. **Agents are not viewing unmarked staging premiums.** A red banner —
   *"**Test-environment rates.** These figures came from the STAGING environment, not production
   market data. Do not present this to a client."* — accompanies the figures whenever the source
   isn't production, on both `IllustrationServlet` compute modes (RANGE and AGE_BAND, confirmed the
   only two — no third mode exists) and on `/GroupConversion`, which duplicates the identical
   `setProvenanceAttributes` pattern.
3. **`ICHRA_ILLUSTRATION` is a live proposal section type and is already correctly gated, at write
   time and read time both.** This is what turns the provenance question from an internal-accuracy
   concern into a customer-facing one — and the finding is that it was already handled, by the same
   two gates named in item 1.
4. **The Illustration's premium computation is itself ungated — the surviving true clause of T48.**
   `handleRangeMode`/`handleAgeBandMode` compute and render figures from whatever rows exist,
   regardless of provenance. Only the *label* (the banner, and now the meta-line — S12-B) and the
   *downstream hand-off* (the button, the write gate) are gated. This is deliberate, not an
   oversight: gating the figures themselves would make an agent-facing tool unable to show test
   data to the agent at all, which is a different and worse problem than showing it with a warning.
5. **The T48 dating finding.** Using `git log -S` (read-only history inspection): T48 was filed in
   commit `42caf4e`. Two **later commits the same day** — `b0e524b` (adding the write/read gates in
   item 1) and `0b4711b` (adding the banner/meta-line/hand-off-gate mechanism in item 2) — both
   postdate it. **T48 was accurate the instant it was written and was invalidated by same-day work
   that never circled back to correct it.** Worth recording as its own lesson: this is a different
   failure mode from ordinary staleness-over-time drift — the gap between "true" and "known false"
   was hours, not weeks, and nothing caught it because nothing re-read the item after the commits
   that invalidated it landed.
6. **Reach: neither `/Illustration` nor `/GroupConversion` is reachable without an authenticated
   session.** `IllustrationServlet.isAuthorized` delegates to the session-based
   `IchraAccessResolver.isAvailable`, and neither path appears in `LoginFilter`'s allow-list —
   confirmed independently at the filter level, not only at the servlet's own check. Not reachable
   by a prospect under any path.

---

## Decisions made

- **T48 amended, not closed.** Its two false clauses ("nothing in `src/` ever branches on the
  value," "`IllustrationServlet` neither filters nor displays it") are struck with the dating
  finding above attached. Its surviving true clause — the premium figures themselves remain
  ungated — is preserved explicitly, and is the stated reason the item stays open rather than being
  marked resolved.
- **The Illustration meta-line now names staging explicitly, following the red banner already on
  that page, rather than the Proposal Builder's staging-silent rule.** S11-G deliberately kept the
  word "staging" out of the Proposal Builder's advisory, reasoning that agents have no concept of
  environment provenance and shouldn't acquire one through that surface. The Illustration page
  already contradicts that premise for itself — it names "STAGING" loudly, in a red banner, on the
  same screen. S12-B judged that being coy in the meta-line two inches below a banner that already
  says the word would be a worse inconsistency (*within* the page) than differing from a *different*
  page's more cautious rule. **Recording this so a future reader who finds both rules in the
  codebase does not assume one of them is a mistake — they are two deliberate answers to two
  different design questions.**
- **`GroupConversionServlet` was declined, not deferred.** S12-B's own run prompt specified the
  identical fix for `GroupConversionServlet.loadAvailableCounties` as for
  `IllustrationServlet.pricedCountyFips`. Discovery found they are not the same shape of thing: the
  Illustration's method only labels a chooser separate from the main dropdown; `GroupConversionServlet`'s
  method **is** the only dropdown. Applying the same fix there would have emptied it, since every
  warmed county is currently staging-sourced. `GroupConversionServlet.java` was never opened for
  editing. Filed as **T137** (below) rather than silently skipped.

---

## ⚠️ New open item — T137

Added to `docs/analysis/project_backlog.md`, next free number (`T136` was the prior highest;
`T137` was confirmed unused before assignment):

> **T137** — `/GroupConversion`'s county dropdown is provenance-blind, and the fix that works
> elsewhere would break it. `GroupConversionServlet.loadAvailableCounties` is that page's only
> county-list source — a filter, not a label, unlike the Illustration's analogous method. It needs
> a different design (a per-option label, or a page-level banner mirroring the Illustration's), not
> a copy of S11-G's or S12-B's fix. MED priority, planned, needs a design pass rather than a guess.

Full detail lives in the T137 row itself and in `docs/session_s12b_closeout.md`, not duplicated
here.

---

## Open items carried forward from session 11

Restated, not re-derived. **None of these were changed by session 12** except where noted.

1. **The agency-branded Market template is unstarted, not deferred.** Kevin's design called for an
   agency-scoped HTML template with tokens marking where market content inserts. What shipped
   (S11-H) is a fixed, SSA-styled fragment (`proposalMarket.jsp`) rendering through direct JSP
   request attributes, not tokens — a different, simpler shape that would need substantial rework,
   not extension, to become agency-templated.
2. **`{{ICHRA_MARKET_BLOCK}}` was never built.** S11-E was written for it; S11-F superseded that
   design. The Market page renders through direct request attributes, confirmed against
   `buildTokenMap` directly (S11-I) — no such token exists, and none should be documented.
3. **The heading-colour cause is unexplained.** S11-I verified `--bs-heading-color` defaults to
   `inherit` in the vendored Bootstrap CSS, so Bootstrap's own default configuration does not
   explain a dark-on-dark heading rendered in a browser. The *print* symptom is fully explained by
   `print-color-adjust` (browsers drop backgrounds by default); the original *browser-render*
   symptom that triggered the investigation is not. **The symptom is fixed** by S11-I's defensive
   baseline (`color: var(--white)` set explicitly, beating inheritance regardless of cause). **The
   cause remains unknown — do not record "Bootstrap did it" as settled in any future doc.**
4. **Figure extraction is duplicated between `proposalMarket.jsp`'s path
   (`ViewProposal.resolveMarketPage`) and `putIchraMarketTokens`.** Two implementations of the same
   rules (tobacco filter, `byAge` map, age-40-row count rule, newest-`fetchedAt` scan), deliberately
   duplicated in S11-H since `putIchraMarketTokens` was forbidden to touch in that run. Can drift
   silently; comments name the origin of each rule, which limits but does not prevent it.
5. **`Proposal.dateCreated` renders blank.** The column is `insertable = false`; population depends
   entirely on the database's own `DEFAULT CURRENT_TIMESTAMP`, confirmed present only in the
   pre-migration baseline dump, never in a tracked migration. **One `SHOW CREATE TABLE proposal` on
   production settles whether the live schema still carries that default.**
6. **T133 — unmatched tokens render literally.** Still `📋 Planned` in the backlog, unchanged by
   session 12. Session 10 scoped two candidate fix shapes (strip globally in `replaceTokens`, versus
   validate at authoring time) and left the choice unmade; it is still unmade.
7. **`SKILL.md`'s Reference Examples paths are stale.** Points at `docs/proposal-fsa-page3.html`/
   `page4.html`, neither of which exists; the real files are
   `docs/proposal_html/proposal_fsa_one.html`/`proposal_fsa_two.html`. Flagged by S11-I, not fixed,
   correctly out of that run's scope.

---

## Blocked on HealthSherpa

Unchanged from session 11 — no session-12 run touched HealthSherpa directly. Restated for
continuity, not re-derived:

| Item | Date first asked | Reply? |
|---|---|---|
| Production allow-listing | Not separately dated — a follow-up step after staging access per the documented onboarding flow. Staging key issued 2026-07-30; production has 403'd on every check since. | **No.** |
| Onboarding representative assignment | 2026-07-29 | **No.** *"Asked, not yet answered."* |
| Staging Basic Auth for the deeplink | Not independently dated — routed through the same unassigned representative. | **No** — same blocker. |
| BAA with Geozoning, Inc. | 2026-07-29 | **No.** *"Unaddressed by anyone so far."* |
| Webhook authentication methods | Not independently dated — same unassigned-contact blocker. | **No.** |
| BCBS TX policy-status timing | 2026-07-29 | **No.** |
| CHRISTUS policy-status timing | 2026-07-29 | **No.** |

**Consequence, restated:** the Market page is built (S11-H), gated on the correct condition
(S11-G's `RateCacheDAO.check()`), and verified — by code inspection — to hold closed today. **It
appears with no further build the day production access lands.** Nothing in session 12 changed
this; if anything, S12-A/B's findings reinforce that the gating mechanism is trustworthy, which
makes the wait purely external.

---

## ⚠️ Do not re-litigate

**LA-17 and `IchraAccessResolver`'s javadoc prohibit a session-based PSP-admin gate on the public
proposal path, by name, in writing.** S11-D hard-stopped on exactly this design and built nothing —
the correct outcome, reached by discovery rather than by refusing to look. Any revival of the
watermarked staging preview needs a genuinely new authenticated route (a PSP-admin servlet sitting
*behind* `LoginFilter`), not a check living inside a public page. A future session that has
forgotten this and re-proposes the session-based check will be re-deriving a conclusion this
project has already reached twice.

---

## Lesson worth recording

**Two hard stops this session caught prompt defects, not implementation defects.** S11-D was asked
to build a mechanism LA-17 already prohibited by name — the stop fired on discovery, not on a coding
mistake. S12-B was asked to apply the identical fix to two methods whose consumers turned out to
differ — one a label, one the dropdown's only data source — and the stop (partial, in that case)
fired on the same kind of verification. **In both cases the fence existed to catch exactly this,
and did.** The scope fences and hard-stop conditions in these run prompts are earning their keep on
the instructions handed to a run as much as on the code a run might have written — worth carrying
forward as a reason to keep writing prompts with explicit discovery-before-build steps and named
hard-stop conditions, rather than trusting a prompt's own framing of "these two things are the same
shape."

---

## Next

Four candidates were on the table for session 11; session 12 answered one (the Illustration
provenance question) and added one (T137). Re-weighing with what's now known:

- **The `/GroupConversion` design pass (T137)** is real, filed, and needs a product/design decision
  (per-option label vs. page-level banner) before any build run could scope it — similar shape to
  the agency-template requirement below, in that it needs a decision first, not code first.
- **The agency-branded Market template** remains unstarted and still needs Kevin's input on what an
  agency's own template should actually contain.
- **T133** (unmatched tokens render literally) is still the one item that's a pure engineering
  decision with no design input needed, and two sessions running have now left its fix-shape choice
  unmade while the underlying defect class has caused one live customer-facing incident.
- **Most remaining build items still wait on HealthSherpa**, unchanged.

**Recommendation unchanged from session 11, with T133 given more weight this time:** decide T133's
fix shape. It is the one item on every list so far that costs nothing to decide (the two candidate
shapes were scoped by session 10), doesn't need a design conversation with Kevin, doesn't wait on an
external party, and has already caused a real defect once. The `/GroupConversion` design pass and
the agency-template requirement are better suited to a short design conversation before a build run
is written for either.

---

## SQL close-out audit — session 12

**Session 12 produced no SQL.** Stating this explicitly, as required: no migration was written, run,
or recommended by either S12-A (read-only, forbidden from any database access) or S12-B (whose own
scope fence forbade migrations). **Current highest migration version:** **V088** —
`ls docs/migrations/*.sql | sort | tail -2` returns `V088__proposal_ichra_intake_contribution.sql`
(plus the long-standing non-versioned `seed_ndt125_questionnaire.sql`), unchanged since S11-A.
**Confirmed: V088 shipped in `v0.88.00`** — `git log -1 --format="%H %s" v0.88.00` returns `123114a`,
S11-A's own close-out hash-recording commit, per session 11's own verified release-ancestry finding.

---

## Compliance statement

**Scope fence, restated.** Writable: `docs/session_closeout_2026-08-03_session12.md` (new) and
`docs/analysis/project_backlog.md` (the new T137 row only). **Nothing outside that set was written.**

**No Java, JSP, HTML, or SQL file was touched.** This entire run is two files, both documentation.

**Every existing close-out was read, not revised** — `session_s12b_closeout.md` and
`session_closeout_2026-08-03_session11.md`, both read in full this run (the latter authored earlier
in this same working sitting and re-verified against source rather than trusted from memory —
release-tag ancestry re-confirmed directly via `git merge-base --is-ancestor`). Neither was edited.

**`docs/ichra_strategy.md` was not read or edited this run** — no claim in this close-out required
checking it, unlike session 11's close-out which did check specific claims against it.

**Every hash cited in this document was read from `git log` during this run** —
`git log --oneline cd04745..effadf3` for the session-12 commit list, `git tag --sort=-creatordate`
and `git merge-base --is-ancestor bcc1417 v0.88.01` for the release-status finding, and
`git log -1 --format="%H %s" v0.88.00` for the V088 release confirmation. None was carried from the
prompt, and none is a placeholder.

**No forbidden git operation was run:** no `git add -A`, no `git add .`, no `stash`, `checkout`,
`restore`, `reset`, and no local tag. Staging was by explicit named path.
