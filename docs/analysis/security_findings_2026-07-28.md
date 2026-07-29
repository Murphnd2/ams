# Security Findings — 2026-07-28

**Source:** Phase A investigation into the ICHRA/QSEHRA enrollment portal
(`docs/analysis/phase_a_ichra_enrollment_portal.md`). These findings are **unrelated to that
feature** and stand on their own.

**Status:** none remediated as of this writing.

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
