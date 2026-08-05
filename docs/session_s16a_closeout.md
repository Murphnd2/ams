# Session S16-A close-out — T142: `/GroupConversion` Count placeholder renders as data

**Run on:** Sonnet, 2026-08-05. Single-file JSP fix, no Phase A (per run brief).

## 1. Shipped

- `af944e91f9bf959f052ce971c1ce2e1ac2d59481` — fix: T142 -- GroupConversion Count field carries a real default, not a placeholder (`groupConversion25.jsp`, +1/−1)
- `087c5ff84af2d6d294d9ec92f0d9f2e9ca074da7` — docs: T142 resolved, session close-out (backlog row + this file)
- `2e555b1ff0b53281d1ffd01a6e917ffd0594fe2f` — docs: T142 close-out -- record the docs commit's own hash (this file only, +2/−2 — filled in the previous commit's hash, which could not be known before that commit existed)

All three hashes read from `git log` in this run, not carried from the prompt. All three pushed together (`git push` → `668afe2..2e555b1`, fast-forward).

## 2. What was anchored on

**Step 1 — the input.** Exactly one match for `placeholder="1"` in the file, inside a single `<c:forEach begin="1" end="6" var="i">` loop (`groupConversion25.jsp:176-194`) that renders all six census rows from one source block — satisfies the "six repeated census rows" condition, hard-stop #1 does not fire.

```
183	                                    <div>
184	                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="count${i}">Count</label>
185	                                        <input type="number" class="form-control form-control-sm" id="count${i}" name="count${i}"
186	                                               min="1" placeholder="1" value="${submittedCounts[i-1]}">
187	                                    </div>
```

Full row markup (age / count / deduction), for record:
```
177	                                <div class="d-flex align-items-end gap-2 mb-2">
178	                                    <div>
179	                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="age${i}">Age</label>
180	                                        <input type="number" class="form-control form-control-sm" id="age${i}" name="age${i}"
181	                                               min="21" max="64" value="${submittedAges[i-1]}">
182	                                    </div>
183	                                    <div>
184	                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="count${i}">Count</label>
185	                                        <input type="number" class="form-control form-control-sm" id="count${i}" name="count${i}"
186	                                               min="1" placeholder="1" value="${submittedCounts[i-1]}">
187	                                    </div>
188	                                    <div>
189	                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="deduction${i}">Deduction</label>
190	                                        <input type="number" step="0.01" class="form-control form-control-sm" id="deduction${i}" name="deduction${i}"
191	                                               min="0" placeholder="Monthly" value="${submittedDeductions[i-1]}">
192	                                    </div>
193	                                </div>
```

**Step 2 — round-trip.** `ROUND-TRIPS: yes — value="${submittedCounts[i-1]}"`. The field already echoes a previously submitted value; an unconditional `value="1"` would have clobbered every submitted count back to 1 on the results re-render. Avoided by using a conditional default instead (see Decisions, below).

**Step 3 — row-inclusion rule.** `GroupConversionServlet.doPost`, the census-row loop at `:220-253`. `:224` — `if (ageRaw == null || ageRaw.isBlank()) { continue; }` — a row is skipped on **blank age**, never on count. `parseBandCount()` (`:614-623`) already returns `1` for a null/blank count (`:616`) before this fix ever existed — so a blank count with a filled age was never silently dropped; it silently became `1` server-side, and the fix's whole point is to make the on-screen value match that behavior. **Inclusion keys on age — safe to proceed, hard-stop #3 does not fire.**

**Step 4 — Illustration's actual fix shape.** `illustration25.jsp:470-471`, inside the W15 comment block (`:452-469`):
```
451	                                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="count${i}">Count</label>
...
470	                                                        <input type="number" class="form-control form-control-sm age-band-count" id="count${i}" name="count${i}"
471	                                                               min="1" value="${empty submittedCounts[i-1] ? 1 : submittedCounts[i-1]}">
```
No `placeholder` attribute at all — removed, not restyled. A plain conditional `value` EL expression, evaluated server-side at render, not a JS-applied default on the primary render path. (A separate JS default at `illustration25.jsp:~1699-1702` exists only inside the dynamic add-row clone handler for that page's repeater — GroupConversion has no such repeater by deliberate design per the S14-C comment at `:163-174`, so that JS path is not relevant here.) **Materially the same shape as this prompt assumed — hard-stop #4 does not fire.**

## 3. Verification status

- **Code-verified**: the JSP diff, the round-trip preservation, the row-inclusion safety argument, and the build.
- **`.\mvnw.cmd clean package` → `BUILD SUCCESS`** (no hard-stop #6).
- **Local render verification: blocked, but not by T111 specifically.** `jps -l` showed no relevant Java process and no listener was found on port 8089 — no local Tomcat instance was running or reachable in this session at all, and this session has no mechanism to start one (no scripted `tomcat:run`, no IntelliJ run-config access). Verification could not reach even T111's usual point of failure (an authenticated wall behind a reachable login page); it never got a response from a server. Functionally the same outcome five prior sessions hit under T111 — local automated iteration is gated — but the specific cause this run was "no server," not "no credential."
- **What T111 does not gate:** production verification by a human operator. Five prior sessions were blocked locally while production walks by Kevin ran fine and runtime-verified other work in the same window (T138, T139 in session 14). This claim is **code-verified only** — it rests on reading prior close-outs, not on observing a production walk this run.

## 4. Decisions made

- **Placeholder removed, not kept** — mirrors Illustration exactly, which removed it rather than restyling or retaining it alongside a value default. A `placeholder` on a field that always carries a real `value` never displays anyway (placeholders only show when the value is empty), so keeping it would have been inert, not incorrect — but matching Illustration's actual markup shape exactly, per the run brief's own instruction, meant removing it.
- **Conditional default (`${empty ... ? 1 : ...}`), not unconditional `value="1"`** — required by step 2's round-trip finding; an unconditional default would have overwritten every submitted count on the results re-render, a regression the run brief specifically warned about.
- **No servlet touched** — `parseBandCount()`'s existing blank-defaults-to-1 behavior was left exactly as-is, matching Illustration's own W15 comment ("The servlet's blank-defaults-to-1 parse is untouched"). This build only makes the JSP stop lying about what that behavior already was.

## 5. New assumptions

None. Every claim in the fix rests on code read this run (the servlet's row-inclusion logic, Illustration's exact current markup), not on inference or the prompt's own description.

## 6. Open questions raised

None. Steps 2, 3, and 4 each resolved to the non-blocking branch cleanly; no ambiguity was left for Kevin to settle on this defect.

## 7. Contradictions found

None against the backlog, the build plan, or `illustration25.jsp` as read. T142's row text (`docs/analysis/project_backlog.md`, pre-edit) described the fix accurately — "a real `value="1"` default, matching Illustration exactly" turned out to match what Illustration's file actually contains, including the placeholder removal, once read directly.

## 8. Anything noticed and deliberately not fixed

Nothing new. T143 (results render below a full-height static form) remains open and untouched, as scoped. No new defect was observed in the census row markup, the servlet's row-inclusion logic, or Illustration's Count field during this run's reads.

`grep -rn "T15[3-9]"` was not run because nothing surfaced that needed a new number — recorded here rather than silently omitted, per the run brief's instruction to state explicitly when there is nothing to file.

## 9. Code-verified-only disclosure

Every claim in this close-out rests on reading code and build output, not on observing the running application, except the git/build mechanics themselves (which were executed, not just read). Specifically code-verified-only:
- The Count field will render a black `1` instead of grey on a fresh load.
- A submitted count survives the results re-render unchanged.
- The row-inclusion safety argument (blank age skips, blank count already defaulted to 1) — read from `GroupConversionServlet.java`, not exercised.
- The claim that T111 has never blocked a production walk — read from prior close-outs (T138, T139 in the memory index), not observed this run.

## 10. SQL close-out audit

**This run produced no SQL.** No `.sql` file was created, edited, run, or recommended. Current highest migration, read from `docs/migrations/` this run (not recalled): **V088** (`V088__proposal_ichra_intake_contribution.sql`). No orphaned `.sql` files were introduced. Nothing is pending deployment as a result of this run beyond the WAR itself. No schema was described but left unscripted. Migrations are confirmed unchanged at V088 — this run touched no migration file and no schema.

## 11. Compliance statement

- **Wrote to:** `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp` (the Count input default), `docs/analysis/project_backlog.md` (T142's row only), `docs/session_s16a_closeout.md` (this file, new — including a follow-up edit to itself, see below).
- **Wrote to nothing else.** Confirmed by `git diff --name-only` on each commit: `af944e91` touches exactly `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp`; `087c5ff8` touches exactly `docs/analysis/project_backlog.md` and `docs/session_s16a_closeout.md` (new file); `2e555b1f` touches exactly `docs/session_s16a_closeout.md`.
- **Read-only, as scoped:** `GroupConversionServlet.java`, `illustration25.jsp` — both read, neither edited.
- **No forbidden git operation was run.** Only `git rev-parse`, `git log`, `git status`, `git diff`, `git ls-files`, `git merge-base`, `git pull --ff-only`, `git add <named path>`, `git commit`, and `git push` were used. No branch was created, switched, or deleted; no `add -A`/`add .`/`add -u`; no `stash`/`checkout`/`restore`/`reset`/`rebase`/`tag`/`branch`; no force-push. `git push` completed as a clean fast-forward: `668afe2..2e555b1`.
- **Hard stops:** none fired. Every hard-stop condition (#1 input shape, #3 row-inclusion, #4 Illustration's fix shape, #6 build failure) was checked and resolved to its non-blocking branch.
- **Every hash cited above was read from `git log` in this run.** No hash was carried from the prompt and no placeholder was left in the final version of this file.
- **Three commits, not two.** Code and docs were split as the run brief allows, but a third small commit was needed to record the docs commit's own hash inside this close-out — a commit cannot cite its own hash before it exists. That third commit touches only this file and contains no code or backlog change.

## 12. Next

**T143** — `/GroupConversion`'s results render below a full-height static input form — is the recommended next step, and the backlog row itself says so ("Ship after T142 — correctness before layout"). Still agreed after reading the file this run: it is layout-only, no servlet change, and T142 (the correctness defect) is now resolved, clearing the stated precondition.
