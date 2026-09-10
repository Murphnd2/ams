# Session 42 Addendum — HealthSherpa Escalation — 2026-09-10

**Type:** Vendor correspondence and documentation only. No code, no migration, no release.

**Out of process.** Opened after S42's close-out, directly on a vendor email, without the
session-open read (strategy first, then tactical state). Filed as an addendum so S42's
*"Session 43 opens on T230"* stays true.

---

## Shipped

Verified against `git log`:

- **`a600ab1`** — docs: record 2026-09-10 HealthSherpa escalation, bounced team inboxes, docs recheck — docs only: docs/business/healthsherpa.md, docs/swbd_ichra_build_plan.md.

This close-out's own commit is not listed, because it cannot contain its own hash.

---

## What happened (all 2026-09-10)

1. **Email history reviewed.** HealthSherpa's last written reply was Julian Ferdman on 2026-07-29.
   SSA's sends since then are unanswered: 07-29 (×2), 07-30, and 08-05.
2. **Public docs rechecked:** Supported Carriers (updated 2026-08-24) and the Webhooks API page.
   Findings are in the `healthsherpa.md` 2026-09-10 section.
3. **Escalation sent.** It went To `ichra@healthsherpa.com`, Cc Julian Ferdman, KJ Sherman, and
   Michael Levin. It was framed around ICHRA and asked one thing first, **who is SSA's onboarding
   contact**, then production allow-listing and the BAA.
4. **`ichra@healthsherpa.com` bounced** (550 5.1.1). The bounce names no other recipient.
5. **Michael Levin's auto-reply** said he was at ACA Summit and slow to respond through Friday
   2026-09-11. His signature gives his title as **SVP & General Manager, ICHRA**.
6. **A forward to `ichra_support@healthsherpa.com` also bounced** (550 5.1.1).
7. **Recorded in `a600ab1`**, after a correction run (see Process deviations).

---

## In flight

Nothing. `.idea/artifacts/ams_war_exploded.xml` is pre-existing IntelliJ noise.

---

## Decisions made

Each with its reversal cost:

1. **Ask one question first: who is SSA's onboarding contact.** O12 gates the staging deeplink
   credentials and the webhook form, so this one answer unblocks most of the rest.
   **Reversal: none; this is framing only.**
2. **Lead with ICHRA.** QSEHRA was dropped from the introduction after Kevin said the strategy is
   more ICHRA than QSEHRA. **Reversal: none for the correspondence.** See the open question on the
   strategy doc.
3. **O16 reframed** from *"when in 2026?"* to *"live for 2027-01-01 effective dates?"*
   **Reversal: one line in the next email.** ⚠️ Kevin should confirm this against his launch-year
   decision.
4. **Stop using HealthSherpa's published inboxes.** Contact goes through named individuals only,
   and Michael Levin is the escalation path. **Reversal: none.**
5. **Michael Levin's direct phone number is kept out of the repo.** It is in Kevin's Outlook.
   **Reversal: add a line.**
6. **The 2026-07-31 chase discrepancy is recorded as unverified, not resolved.**
   **Reversal: edit one note.**

---

## New assumptions

- **Julian and KJ received the escalation.** Neither bounce names them, and Michael's auto-reply
  proves his copy delivered. Julian's and KJ's delivery is **inferred, not confirmed.**
- **Part of HealthSherpa's silence this week is ACA Summit.** ⚠️ This basis is thin. At most it
  explains this week, not July and August.
- **The carrier and webhook facts in `healthsherpa.md` (2026-09-10) are docs-stated**, not
  runtime-verified.

---

## Open questions, and what settles each

- **Onboarding contact (O12).** HealthSherpa settles it. Michael Levin is the most likely person to
  do so.
- **Production allow-listing, BAA (O13), webhook contract (O15), BCBS TX for 2027-01-01 (O16), and
  CHRISTUS Policy Status.** HealthSherpa settles these, through the onboarding contact once there
  is one.
- **Whether the 2026-07-31 chase happened.** Kevin settles it with one Sent Items search.
- **The ICHRA-over-QSEHRA emphasis.** Kevin stated it on 2026-09-10. `ichra_strategy.md` was not
  re-read against it this session. Whether the doc needs revising is Kevin's call at the next
  strategy-first session open.

---

## Contradictions found

1. **Build plan §6 records 2026-07-31 chases** that aren't in Kevin's email history. They are
   flagged as unverified in `a600ab1`.
2. **HealthSherpa's own published contact addresses are dead.** `ichra@` comes from its 2025-08-05
   press release, and `ichra_support@` from its agent help center. Public HealthSherpa contact
   information can't be trusted without a delivery test.
3. **The `a600ab1` commit body:** 2 paragraphs after the subject (the descriptive body text, and a
   `Co-Authored-By` trailer). The commit prompt specified two `-m` flags — subject and one body
   paragraph, a two-part message; the compliance statement reported three `-m` flags, the third
   being the `Co-Authored-By` attribution this session's standing instructions require. The body
   text itself matches the commit prompt's wording; the extra paragraph is the trailer, not a
   deviation in content.

---

## Process deviations

1. **No session-open read.** The session went straight to a vendor email.
2. **The record run preceded the delivery results**, so a correction run was needed. **Standing
   rule:** record outbound correspondence once delivery is known, not at send.
3. **Claude.ai supplied both dead addresses from public pages** without flagging their age or
   whether they still worked. **Standing correction:** a vendor address sourced from a public page
   is unverified until something has delivered to it.
4. **The first record prompt assumed `healthsherpa.md` is newest-first.** It is oldest-first. The
   prompt's own branch caught this, and nothing broke.
5. **The correction prompt's Step 4.5 check was stricter than the text Step 2 mandated.** Claude
   Code flagged the gap instead of rewriting the text. This was a prompt defect, handled correctly.

---

## Next

- **Session 43 opens on T230, as S42 recommended.** This addendum changes nothing about it: T230 is
  unblocked and needs a Phase A.
- **Kevin, Wednesday 2026-09-16:** if HealthSherpa hasn't replied, send a two-line note to Michael
  Levin alone asking only who SSA's onboarding contact is.
- **Kevin, carried from S42:** release `v0.97.00`. Current state: no v0.97.00 tag on origin — not
  yet released. D-96 must also be done before the first production push.
- **Strategic note.** S42 and this addendum are both off the path to the Forrest demo. The SWBD
  asks in build plan §6 currently read:
- **O22 book profile** + producing-agent count — ❌ **not sent**
- **"Send me three groups renewing next quarter"** — ❌ **not sent**
- **O24 — the full group book** — deliberately deferred

---

## SQL close-out audit

- **No SQL was produced, run, or recommended this session.**
- **Orphaned `.sql` files:** none.
- **Highest version** (per `migration_tracker.md`): V097.
- **Pending deployment** (per `migration_tracker.md`): V097's row reads "Not applied anywhere as of
  authoring (2026-09-10, S42-B); pending release" and "Production: pending release `v0.97.00`" —
  its Production status column is ⬜ (unapplied). The document's own "Current Highest Version: V096"
  header line is stale and was not corrected this session; it is out of scope here.
- **Schema described but not scripted:** none this session. S42's T230 step-state entity carries
  forward.
