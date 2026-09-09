# Security Findings — 2026-07-28

**Source:** Phase A investigation into the ICHRA/QSEHRA enrollment portal
(`docs/analysis/phase_a_ichra_enrollment_portal.md`). These findings are **unrelated to that
feature** and stand on their own.

**Status:** 1 of 6 findings remediated — FINDING 1 closed 2026-09-08 (session 36), code-verified but **not yet deployed to production**. FINDINGS 2–6 remain open as of this writing.

---

## FINDING 1 — `/CreateBpoTestUser` is an unauthenticated account-creation endpoint (HIGH)

`controller/.../CreateBpoTestUser.java:149-155` creates a user account with a **hard-coded password**
and **prints that password in the HTTP response**. The endpoint is in `LoginFilter`'s
`ALLOWED_ENDPOINTS` set (lines 19-24), meaning it is **permanently reachable without
authentication**.

Not PHI-related. This is a straightforward unauthenticated privilege-granting endpoint on a live
production system.

**Recommended:** remove the endpoint, or gate it behind an environment check that cannot be true in
production. **Highest priority item in this document.**

**STATUS — ✅ REMEDIATED 2026-09-08 (session 36), commit `16077ab`. Not yet deployed.**

Closed by **removal, not by gating.** A repo-wide search across code, JSPs, docs, scripts and
configuration found **zero callers and zero documented operational dependencies** — every reference
was an inventory entry or this finding itself — so the endpoint was deleted outright rather than
restricted. The `@WebServlet` annotation inside the deleted class was the **sole** declaration of the
`/CreateBpoTestUser` mapping (`web.xml` declares no servlet mappings at all), so deleting the file
removed endpoint and mapping together.

**Path correction (this finding's own text above is left intact as the record):** the elided
`controller/.../CreateBpoTestUser.java` resolved to
`src/main/java/net/superiorstate/ams/controller/authentication/CreateBpoTestUser.java`.

⚠️ **Code-verified only — runtime-verified: nothing.** No request was issued to the endpoint before
or after the change, so the expected 404 is an inference from the mapping's deletion, not an
observation. ⚠️ **Not deployed:** `superiorstate.biz` still runs the previously deployed WAR, so the
endpoint is **open in production right now** and stays open until a build carrying this commit ships.

**Two follow-ups filed in `docs/analysis/project_backlog.md`:** **T220** (LOW) removes the now-inert
`"/CreateBpoTestUser"` string still listed in `LoginFilter.ALLOWED_ENDPOINTS`, which is cleanup
rather than a defect since the path resolves to no servlet; **T221** (HIGH) covers the accounts this
endpoint may already have created — `bpoadmin@test.com` (UserRole 102) and `bpouser@test.com`
(UserRole 103) — because **removing the creator does not revoke what it created**, and both carry a
known hard-coded credential wherever they exist.

---

## FINDING 2 — PHI can reach public GUID URLs through admin configuration alone (HIGH, structural)

`ApplicationField.fieldType` (`ApplicationField.java:24`) is an **unconstrained `varchar(20)`**, and
`ApplicationFieldValue.fieldValue` (`ApplicationFieldValue.java:21`) is **unencrypted `TEXT`**. Both
are reachable through the **unauthenticated** `/apply/{guid}` URL (`ApplyForProposal`) and
`/saveApplication`.

**Any PSP admin can add a field labeled "SSN" through the admin UI and it will flow through a public
URL, stored in plaintext, with zero code changes.** No code change is required to create the
exposure — only a configuration action.

`QuestionnaireField.fieldType` has the identical structure, reachable via `/q/{guid}`
(`FillQuestionnaire`) and `/saveQuestionnaire`.

**Recommended:** a field-type allowlist, an explicit "no PHI in public forms" policy control, or
both. This matters increasingly as SSA represents itself as a PHI-handling administrator under a BAA.

**Design rule:** never build a PHI-bearing form as a third instance of this pattern.

---

## FINDING 3 — `EmployerBillingDetail?uid=` allows month pivoting (MEDIUM, unverified)

Exposes per-employee billing and variance data into the session. A redeemed `uid` appears to allow
pivoting to arbitrary billing months for that employer with **no re-validation**. In `LoginFilter`'s
allow-list.

**Not fully verified** — target entities' full field lists were not read. Needs a follow-up pass.

---

## FINDING 4 — `ShowFileUpload?doc=` serves any WebLink-attached file (MEDIUM, unverified)

Serves any file attached to a `WebLink` — **including email, note, and task attachments**, not only
marketing collateral — via a GUID with **no session check**. In `LoginFilter`'s allow-list.

The implicit security assumption is that `linkPath` values are unguessable. **That assumption was not
independently verified.**

---

## FINDING 5 — `AmsDataLocal` copies installation-wide data into every session (MEDIUM, architectural)

`AmsDataLocal.intializeLocalData` lines 137-141 **unconditionally** copy the entire installation's
open-activity list and renewal-employer list into every session object for any `AppConfig.isPsp()`
install, exposed via public getters with **no scoping**. `AmsDataGlobal` is ServletContext-wide and
unfiltered throughout.

Currently tolerable because every session type (PSP staff, agents, agency admins) is trusted at
roughly PSP/agency level. **This becomes a live data-leak the moment any lower-trust session type is
introduced** — an individual employee being the obvious case.

---

## FINDING 6 — Never-seeded role referenced in authorization code (LOW)

`AuthDAO`'s role switch and `AuthenticateUser.getUsersByRole(em,101)` reference `UserRole` id **101**,
which is **never seeded anywhere**. Roles 6 and 7, documented in
`.claude/inventory/AMS-DOMAIN-KNOWLEDGE.md` and `docs/analysis/entity_reference.md`, are likewise
never seeded. Pre-existing inconsistency; not known to be exploitable.

---

## Suggested priority

1. FINDING 1 — remove or gate `/CreateBpoTestUser`
2. FINDING 2 — constrain public form field types
3. FINDINGS 3 and 4 — complete the verification pass
4. FINDING 5 — address before any lower-trust session type ships
5. FINDING 6 — clean up during unrelated role work
