# Session 37 Close-Out — 2026-09-09

**Type:** Documentation only. No code, no schema, no SQL, no build, no deploy.

Session 37 reconciled four documentation claims with the state `v0.96.00` actually left on
production. Every correction below was made against a directly observed fact, not an inference.

---

## Shipped

| Commit | What |
|---|---|
| `2f9da02` | docs: reconcile migration tracker, D-95, T221 and session 36 verification claim with deployed state |
| _(this document)_ | docs: session 37 close-out — hash not cited, since a commit cannot carry its own |

**Deploy note — not this session's work, but the fact everything here reconciles to:**
**`v0.96.00` deployed to production from `b683f3b`.** `update.sh` reported
`DONE: Updated ssa_production from v0.94.00 to v0.96.00` at 2026-09-09 12:06:29, and applied both
pending migrations in the same run:

- `MIGRATION APPLIED: V095__summit_plan_template_map.sql`
- `MIGRATION APPLIED: V096__summit_file_export.sql`

---

## Decisions made

**`clean` was permitted for one release build, under an explicit precondition.** The standing rule
on this project is that `.\mvnw.cmd clean` is not run casually, because a clean build against a
running Tomcat can leave the exploded webapp in a broken state. It was permitted for the `v0.96.00`
release build **only because Tomcat was confirmed not running**, and only because a non-clean
`package` had demonstrably failed to remove the orphaned `CreateBpoTestUser.class` (see Contradiction
4).

**This is an exception, not a change to the rule.** The precondition is the whole justification: with
Tomcat running, the same command is still unsafe. A future release build needing `clean` needs the
same precondition established again, explicitly, rather than citing this run as precedent.

---

## New assumptions

**One, and it is registered as unconfirmed rather than asserted.**

The weekly `beta_ssa` refresh pulls from production. Production now carries V095 and V096, so a
refresh should no longer return the local schema to V094 — the refresh-drop warning that both
migrations carried should be obsolete.

**This is an inference from the deploy log, not an observation.** It is written into
`docs/analysis/migration_tracker.md` explicitly flagged as such, and the `beta_ssa` columns were
deliberately left untouched by this run.

**Reversal cost: near zero.** If the next refresh does drop them, nothing was built on the
assumption — the fix is to re-apply both, in order, exactly as before, and to restore the warning
text that was marked superseded rather than deleted.

---

## Open questions raised

**T220 remains open** — the now-inert `LoginFilter.ALLOWED_ENDPOINTS` entry for
`/CreateBpoTestUser`. The endpoint is gone, so the entry allowlists nothing and is not a live
exposure; it is dead configuration that should be removed on its own, not folded into unrelated
work. Untouched by this session by scope fence.

---

## Contradictions found

Four. Each is recorded in place as a dated correction with the original text retained — none was
silently overwritten.

### 1. `docs/analysis/migration_tracker.md` — "Production is current at V094"

**Believed:** Production at V094; V095 applied to `beta_ssa` only; V096 "applied to no environment,
`beta_ssa` included"; both dropped by the weekly refresh.

**True:** Both are applied on production as of 2026-09-09, shipped with `v0.96.00`. Production is at
**V096**.

Production column flipped to ✅ on both rows. The `beta_ssa` columns were left alone — the
refresh-transience convention was not this run's business, and production is now the source those
refreshes come from.

### 2. `docs/deployment_backlog.md` D-95 — two separate errors

**Believed (status):** not applied on production.
**True:** applied 2026-09-09 with the WAR, exactly as the row itself required. D-95 is **done**.

**Believed (central claim):** "⚠️ **Nothing reads the table yet.** … the listing screen and the
same-hash warning are **T212**."
**True:** **T212 shipped** in `373d684`;
`src/main/java/net/superiorstate/ams/controller/market/SummitFileExportAdmin.java:93` calls
`SummitFileExportDAO.findByPspId`.

That second error also invalidated the row's own severity line, `Priority: MED — nothing breaks
without it`. Once T212 shipped, a PSP admin opening `/SummitFileExportAdmin` against a V096-less
database would have got a 500. The row understated its own priority for as long as its reader
existed.

### 3. `docs/analysis/project_backlog.md` T221 — accounts assumed to possibly exist

**Believed:** two BPO test accounts may already have been created and would need disabling per
environment; nothing in session 36 could determine whether they existed anywhere.

**True:** production's `user` table was queried for both accounts by user name and by email and
returned **zero rows**. The endpoint never ran on production. Nothing was created, nothing needs
revoking. T221 is **done**.

Local caveat recorded in one line: local `beta_ssa` refreshes weekly from production, so any copy
that ever existed locally is transient and reimports clean from a source now confirmed empty. No
further action on any environment.

### 4. `docs/session_closeout_2026-09-08_session36.md` — a verification that never ran

**This is the most serious of the four.** That document asserted the removed class was "confirmed
absent from both `target/classes` and the freshly built WAR — checked explicitly."

**Only the `target/classes` half ran.** The WAR check was `&&`-chained behind an `ls` that failed by
design; the shell short-circuited, the real check never executed, and the command's `||` branch
printed a pass. The output read as a clean two-part verification of a command that had not run.

**The subsequent build proved the claim false: the WAR did contain the class.** `package` without
`clean` had left the orphan in the exploded webapp directory — precisely the risk the sentence names
and then failed to test for. A clean rebuild resolved it.

The original sentence was left unedited and a dated correction appended beneath it. That document's
`v0.96.00` tag caveat — "the version number is not a claim about schema state" — was corrected the
same way: both migrations shipped with the release and applied on deploy, so production genuinely is
at V096 and the caveat no longer holds.

---

## Verification status

**Runtime-verified — genuinely, for the first time in this chain:**

- **`/CreateBpoTestUser` returns `404` on `superiorstate.biz`.** Observed with
  `curl -o /dev/null -w "%{http_code}"`. Every prior claim in this chain was an inference from the
  `@WebServlet` mapping's deletion; this is the first actual request issued to the endpoint.
- **`CreateBpoTestUser.class` is absent from the production filesystem.**
  `sudo find / -name "CreateBpoTestUser.class"` returned no paths — which also confirms the clean
  rebuild resolved the orphan that the non-clean build had carried.
- **Neither BPO test account exists in production's `user` table.** Queried by user name and by
  email; zero rows.
- **V095 and V096 are applied on production.** Observed in the `update.sh` deploy log, not inferred
  from the release tag.

**T29 is closed and runtime-verified.** The 404 settles it.

**Code-verified only — named explicitly so it is not mistaken for the above:**

- **T212's listing screen has not been exercised against the now-present table.**
  `SummitFileExportAdmin.java:93` was read, not run. The screen was never opened on production
  before or after the deploy, so the DAO call is compile-verified and its behaviour against real
  `summit_file_export` rows is unobserved.
- **The enrollment emitter's zero-row refusal remains unexecuted** (T196/T209, carried unchanged
  from session 35).
- **The refresh-drop inference** in New Assumptions above — not observed.

**Not verified and not claimed:** no database was queried by this session, nothing was deployed, no
build was run. Every production fact above was established before this session began and is recorded
here, not produced here.

---

## Process lessons

Three, written to be reused rather than read once.

### 1. A verification command must be issued standalone

`&&`-chaining a check behind a command that can fail produces a **silent false pass**: the check
never runs, and whatever the `||` branch prints is indistinguishable from a real result. Session 36's
WAR check failed exactly this way.

**Rule:** issue a verification as its own command, and read its own exit status. **Prove the check
ran** with a positive control — run it against something you know is present and confirm it reports
present. A check that cannot fail on a negative case is not evidence.

### 2. `package` without `clean` cannot remove an orphan

Maven's `package` does not clear the exploded webapp directory. A deleted `.class` file **survives
into every subsequent WAR** until a `clean` removes it. This is not an edge case — it is the default
behaviour on every delete-a-class change.

**Rule:** a release artifact that removes a class needs either a clean build or an explicit orphan
check against the WAR itself. "It compiled" says nothing about what the WAR still contains.

### 3. A doc's severity assessment goes stale when the code it describes changes

D-95 was written when nothing read `summit_file_export`, and its `Priority: MED — nothing breaks
without it` was correct at that moment. **T212 then shipped the reader, and nobody revisited the
row.** From that commit until this session, the backlog understated the consequence of not applying
V096 — a 500 for any PSP admin opening the screen.

**Rule:** shipping the consumer of a deferred dependency means re-reading the dependency's own
backlog row. A severity claim has an implicit "as of" that nothing in the document enforces. This is
the same class of failure the tracker's own maintenance note (2026-07-30) already names for
migration status — it applies to priority and blast radius too, not just to ✅/⬜.

---

## Next

**T220** — remove the inert `LoginFilter.ALLOWED_ENDPOINTS` entry for `/CreateBpoTestUser`.

**Why this one:** it is the last remaining fragment of the T29 chain, it ships alone, and it is a
small, self-contained code change. That last point matters more than its size. Sessions 33 and 34
were both docs-only, session 35 shipped a narrow code change, session 36 shipped code, and **this
session is docs-only again**. This project's deliverable is build prompts and working code, not
filings — and the corrections above are exactly the kind of work that expands to fill a session.
T220 puts the next run back on code.

**Worth doing at the same time, cheaply:** open `/SummitFileExportAdmin` once on production. The
table now exists and the screen has never been run against it; that converts T212 from
code-verified to runtime-verified for the cost of a single page load.

---

## SQL close-out audit

**No `.sql` file was created, modified, or recommended by this session.**

- **Current highest version in tree: V096** (`docs/migrations/V096__summit_file_export.sql`).
- **Nothing is pending on production.** V095 and V096 were both applied 2026-09-09, observed in the
  `update.sh` deploy log for release `v0.96.00`. Production is at V096 and is not behind on any
  migration in this tree.
- Demo / BPO / Master remain **N/A** for the V095–V096 pair — no Summit tenant. This is a scoping
  statement, not a backlog.
- The `beta_ssa` columns in `docs/analysis/migration_tracker.md` were **deliberately not touched** by
  this run. See New Assumptions.
