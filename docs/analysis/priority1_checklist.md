# Priority 1 — Summit setup steps 1–5: checklist

**Created:** 2026-09-11 (s47c)
**Status:** Active — a running state document
**⚠️ Not synced to project knowledge.** This file changes every session and describes what is
*not yet done*; syncing it would freeze a snapshot that is wrong within a day. Read it from the repo.

**The rule:** Priority 1 is done when every row is checked; each session close-out updates this file.

Sources: `docs/analysis/plus_tier_build_plan.md` Part 14 (D43–D46), `docs/business/summit_data_exchange.md`
(SDX-NN), `docs/analysis/project_backlog.md` (T231, T238), `docs/deployment_backlog.md` (D-96–D-98),
s47a/s47b Phase A (2026-09-11).

| Step | Row | Owner | Status | Source |
|---|---|---|---|---|
| **1 Employer** | SDX-24 — *Employer Plan Name* is an available import element | Kevin | ✅ element present (KEVIN-UI 2026-09-11); a value-import test is pending | SDE SDX-24 |
| 1 Employer | SDX-27 (new) — force all four flags (CDH, COBRA, Direct Bill, Retiree) as explicit `true`/`false`. Is `false` honored on create? What does `false` do on update of a flag already on? Test on a throwaway employer. | Kevin | ⬜ | SDE SDX-27; D46 |
| 1 Employer | Add the four flag elements and *Employer Plan Name* to the Employer Demographic import template | Kevin | ⬜ | D46; D-97 |
| 1 Employer | Config: a PSP-scoped ServiceItem → administration-type mapping (CDH, COBRA, Direct Bill, Retiree), with an admin screen. None exists today — `ServiceItem.code` is null on Setup items (s47b Q10). Copy the V095 `summit_plan_template_map` + `SummitPlanTemplateAdmin` pattern. Can be built before SDX-27: it is a gated admin page nothing reads until the emitter ships. Entering its rows is a data-entry note for Kevin. | Claude Code | ⬜ | s47b Q10/D; D46 |
| 1 Employer | Emitter: the four flags as the union across elected ServiceItems (D46), plus *Employer Plan Name* from `hra_annual_ee` when the elected templates include the `ICHRA` key segment | Claude Code | ⬜ blocked on SDX-27 | D46; s47b Q6/Q11 |
| 1 Employer | Decision: the *Employer Plan Name* COBRA-collision rule — is the name omitted when the COBRA flag is true? | Kevin | ⬜ at the emitter build | SDE "`EmployerPlanName` collision"; D43 limits |
| 1 Employer | D-97: confirm D46 makes the template's flag defaults moot | Kevin | ⬜ after SDX-27 | `deployment_backlog.md` D-97 |
| **2 Plans** | SDX-26 — dual CDH + COBRA template | Kevin | ⬜ | SDE SDX-26; D44 |
| 2 Plans | SDX-28 (new) — does the CDH Plan import load a **COBRA-only** plan from a COBRA template id when the employer is COBRA-flagged first? | Kevin | ⬜ | SDE SDX-28 |
| 2 Plans | Create Summit plan templates per the SDX-26/28 results. Entering AMS mapping rows is a data-entry note, not a work item. | Kevin | ⬜ | — |
| 2 Plans | Conditional template mapping (a schema change). **Needed only if SDX-26 is chosen over SDX-28.** Under V095's `UNIQUE (psp_id, service_item_id)`, a paired row needs a key change or a companion column (s47b E3). If the paired row carries a different key segment, a re-push after a COBRA election change creates a second plan in Summit (s47b Q14). SDX-28 avoids both, because COBRA would get its own mapping row and its own key segment. | Claude Code | ⬜ conditional | s47b Q12/Q14/E3; T238 |
| 2 Plans | Data-entry note: the test setup's HFSA (ServiceItem `123054`) has no mapping row, so its plan has never been pushed (s47b E8) | Kevin | ⬜ | S45 close-out :31-33 |
| **3 Request** | Build 1: request, email, token, public drop, staging | Claude Code | ✅ built s47c 2026-09-11 (V100, compile-verified; not runtime-verified) | T231; D45 a–d |
| 3 Request | Runtime walk of build 1 | Kevin | ✅ 2026-09-11, KEVIN-UI, local. **Passed:** panel before any request; the compose page; Send (returns to the setup, status line updates, email arrives with a working link, logged on the activity); the branded upload page; the clean upload (10 rows); the issues upload (3 flagged, no values echoed); the missing-column upload (unreadable, City named); Revoke (the old link shows inactive); re-request and re-upload on a new token. **Storage verified by SQL:** earlier submissions are `SUPERSEDED` with `rows_json` NULL, and SSN, DOB, pay and department strings are absent from `rows_json`. **Not confirmed:** the "Census upload received" requester notification emails — re-checked in the build 2 walk. **Not walked separately:** a garbled token; `resolveActive` returns the same empty result for unknown and closed tokens (CODE). | S47 walk |
| **4 Census** | Build 2: review page, Load and Reject, the Clear guard (decision f) | Claude Code | ⬜ | T231; D45 e–f |
| 4 Census | Build 2: a client upload logs an inbound entry on the setup activity and sets its status to Waiting on Us, mirroring Send's Waiting on Them. Send's existing status change is correct as is. | Claude Code | ⬜ | Kevin 2026-09-11 / S47 walk |
| 4 Census | Build 2: the upload page shows field labels in its issue lines ("Last name"), not field names (`last_name`). | Claude Code | ⬜ | Kevin 2026-09-11 / S47 walk |
| 4 Census | Build 2, cosmetic: the inactive page renders an empty PSP-name slot, showing `Census upload —` in the tab title and `© · Benefits Administration Services` in the footer. Drop the separator when the name is empty. | Claude Code | ⬜ | Kevin 2026-09-11 / S47 walk |
| 4 Census | Runtime walk: client upload → review → load | Kevin | ⬜ | — |
| **5 Demographics** | Runtime walk of the AMS Demographics push over SFTP, the response read by AMS, Mark done REVIEWED, Push anyway, `PUSH_FAILED`, and the collision refusal | Kevin | ⬜ | S42/S45/S46 carry-forward; s47a Q17 |
| 5 Demographics | Confirm the Demographics import and results templates are current, with `Record Comment` mapped | Kevin | ⬜ | SDE "Response check (T230)"; D-89 |
| **All pushes** | Confirm production `ImportFiles` retrieval (SDX-15 used manual retrieval only) | Kevin | ⬜ | SDE SDX-15/SDX-19 |
| **Release** | D-96, D-97, D-98, and a release carrying V097–V100 | Kevin | ⬜ | `deployment_backlog.md` |
| **Open questions** | Do the upload page and request email carry agency white-label branding (SWBD / premiumpath.net), or PSP branding only? Both copy `/apply/*`'s pattern. Settle this before the Forrest demo. | Claude Code (one read) | ⬜ | S47 walk item 4 |
