# Read-Only Investigation — Outbound Email Identity (From / Reply-To / Envelope) & White-Label Sending

**Status:** READ-ONLY mapping pass. No code/schema/migration changes. This is the current-state map that grounds the per-agency white-label *sending* design (a separate future task).
**Branch:** `refactor/modernize-architecture` · Date: 2026-07-09

---

## 0. Headline findings (the ones that change the design)

1. **`SMTP_FROM` is blank in the live `beta_ssa` DB** (`SMTP_FROM = ''`, row present but empty). This flips the whole model: with `SMTP_FROM` unset, `EmailDAO` sets the **`From:` header to the sender's own email** (`fromWho`), and the SMTP **envelope-from to the same address**. So today, mail goes out **as the human sender's real address**, not a fixed Superior State address.
2. **`EmailDAO` is the single *live* SMTP choke point.** One class does all real sending; every user-facing send path funnels through its `sendEmail(...)` overloads. There is a **second, dead** SMTP path in `AmsDataLocal` (its own `Transport.send`) with **no callers** — legacy, but a latent second From-injection point.
3. **Deliverability posture today is "follows the sender," which is aligned only for the PSP domain.** PSP-staff senders (`@superiorstate.net`) are verified in the SMTP2GO account and deliver fine. **External-agency agents (e.g. `user@swbd.com`) already send `From:` their own unverified domain** → SPF/DKIM unaligned in the relay → reject-or-spam. This is precisely the gap the white-label design must close, and it exists *now*, not hypothetically.
4. **`Reply-To` and a friendly `From` display name are not first-class in the API.** `EmailDAO` hardcodes `Reply-To = fromWho`; it accepts **no** explicit From-header, Reply-To, or display-name parameter. The signatures must be extended to carry a per-message identity decision.
5. **The identity context needed for a per-agency decision is *not* delivered to the choke point.** `EmailDAO.sendEmail` receives only a `String fromWho` + `EntityManager` — not the sender `Person` and not the `Agency`. Per-message From/Reply-To routing needs new plumbing to pass sender+agency (or a resolved email-config) into `EmailDAO`.

---

## 1. Current-state map — every outbound path

### 1.1 The SMTP layer — `EmailDAO` (`data/dao/EmailDAO.java`)

The fully-specified `sendEmail(fromWho, to, cc, bcc, subject, htmlBody, em)` (lines 59–138) is the real send. How each deliverability field is built:

| Field | How it's set | Line |
|---|---|---|
| **`From:` header** | `smtpFrom` = constant **`SMTP_FROM`**; **if blank →** `fromWho` when it's a valid email, **else** `SMTP_USER`; if still not a valid email, **throws**. `msg.setFrom(new InternetAddress(smtpFrom))` — **no display name**. | 75–89 |
| **`Reply-To:`** | `if (isValidEmail(fromWho)) msg.setReplyTo(fromWho)` — **always the caller's `fromWho`**, never parameterized. | 92–94 |
| **Envelope sender / return-path** | `session.getProperties().put("mail.smtp.from", smtpFrom)` — **equals the `From:` header** (same `smtpFrom`). | 127 |
| **Display name** | none — `new InternetAddress(addr)` with no personal name, anywhere. | 89 |
| **Transport** | explicit `session.getTransport("smtp")` → `connect()` (Authenticator supplies creds) → `sendMessage`. Multipart/alternative (text+HTML). | 130–137 |

**Consequence of the blank `SMTP_FROM`:** `From:` = `Reply-To:` = envelope-from = **`fromWho`** (the human sender's address). The "From = brand, Reply-To = human" split the code was written for **only activates when `SMTP_FROM` is configured** — which it is not.

The `Email`-model overload `sendEmail(Email, em)` (142–166): `fromWho = email.getCreatedBy().getEmail()` → same rules; From/Reply-To become the note's creator.

**SMTP session config** (`getSession`, 170–205), all from DB constants (values not printed here): `SMTP_SERVER` (`mail.smtp2go.com`), `SMTP_PORT` (`2525`), `SMTP_USER` (a non-email SMTP2GO account token, len 13 — **not** an address), `SMTP_PASSWORD`. STARTTLS required; per-domain cert trust. Optional `SMTP_DEBUG`.

### 1.2 Send-path inventory (all callers of `EmailDAO`)

| # | Path (class:line) | `fromWho` passed → today's `From:`/`Reply-To:` | Body wrap | Agency used? |
|---|---|---|---|---|
| 1 | **Proposal compose+send** — `SendProposal.doPost:122` | `sender.getEmail()` (logged-in agent) | `EmailTemplate.wrapBodyOnly` (signature is in the composed body) | Agency name in **body signature only** via `resolveSenderAgencyName` (:151) — *not* From |
| 2 | **Proposal quick-send** — `ProposalDetail:108` (`action=sendToProspect`) | `sender.getEmail()` | `EmailTemplate.wrap` — **signs the PSP name** (`sender.getPsp().getFullName()`) | No — signs PSP, not agency |
| 3 | **General email (Compose)** — `SendEmail25:114` (`EmailTemplate.wrap` at :47) | `Email.getCreatedBy().getEmail()` | `EmailTemplate.wrap` (signature: name/email/PSP) | No |
| 4 | **Automation final send** — `SendAutoFinal25:192, :362` | `Email.getCreatedBy().getEmail()` | pre-built `Email.detail` | No |
| 5 | **Agent invitation** — `SendInvitation:164` | `local.getCurrentPerson().getEmail()` | inline HTML, signs `global.getPsp().getFullName()` | Agency **name** in body copy; not From |
| 6 | **Employer billing** — `SendEmployerBillingDetail:52` | `Email.getCreatedBy().getEmail()` | — | No |
| 7 | **New-user welcome/invite** — `CreateUser25:330` | **literal `"noreply@superiorstate.net"`** | `EmailTemplate.wrapBodyOnly` | No |
| 8 | **Password/help login** — `HelpUserLogin:89` | **literal `"noreply@superiorstate.net"`** | `EmailTemplate.wrapBodyOnly` | No |

**Not senders (checked):** `EmailBillingToEmployer` only builds a billing link and forwards to `sendBillingForm.jsp` (the actual send is #6). `AcceptInvite` is the invite-acceptance landing — no send.

**Dead path:** `AmsDataLocal.sendEmail(EntityManager, AmsDataGlobal)` (line 1685) — its own `getMessage()`/`Transport.send`, **`From = getSender().getEmail()`, no `Reply-To`, no envelope-from override, HTML-only (no multipart)**. **Zero callers** (grep-verified). Legacy; note it so a future refactor doesn't accidentally revive an un-branded path.

### 1.3 `EmailTemplate` (`data/util/EmailTemplate.java`)
Builds the HTML wrapper + signature. `wrap(body, sender, attachments, em)` prints a signature block of **sender name / sender email / PSP full name** (52–101). `wrapBodyOnly(body, pspName, em)` — no signature. Neither touches headers; **body-level identity only**. Footer text = `EMAIL_FOOTER_TEXT` (`"Superior State Administrators, Inc. | solutions, simplified"`).

---

## 2. Which deliverability scenario are we in today?

**"From follows the human sender," single-relay (SMTP2GO), aligned only for the PSP domain.**

- **PSP staff** (`@superiorstate.net`): `From:`/envelope-from = their `@superiorstate.net` address → domain verified in the SMTP2GO account → SPF/DKIM aligned → **delivers**. This is why email "works" today — virtually all real senders are PSP staff.
- **External-agency agents** (`user@swbd.com`): `From:`/envelope-from = `user@swbd.com` → **not** a verified sender domain in the relay → **reject or DMARC-fail/spam**. Any agent who sends a proposal today is already in this failure mode.
- **System mail** (#7, #8): literal `noreply@superiorstate.net` → aligned → delivers.
- `Reply-To` currently **duplicates** `From` (both `fromWho`) because `SMTP_FROM` is blank — the routing distinction the code intends is inert.
- **No path sets a friendly display name.** No path currently produces the *intended* white-label shape (`From: user@admin.swbd.com`, `Reply-To: user@swbd.com`).

**Net:** we are *not* in a "safe, single Superior State From" state. We are in a "From = whoever's logged in" state that happens to be safe only because senders are mostly PSP-domain. The moment agents send, From is their own unverified domain — so the deliverability problem is live.

---

## 3. From-injection points (where a per-agency decision would hook)

- **Primary (centralized): `EmailDAO.sendEmail(...)`, lines 75–94** — the *one* place `From`, `Reply-To`, and envelope-from are decided. A per-agency identity decision belongs here (or in a helper it calls). This is a genuine choke point — **good news for the design**: one method governs headers for 8 of 8 live paths.
- **Secondary (dead): `AmsDataLocal` line 1685/1693** — sets `From` independently, no Reply-To. Uncalled; must be either deleted or routed through `EmailDAO` before any identity model is trusted, so it can't diverge.
- **Body-signature points (not headers, but identity-relevant):** `EmailTemplate.wrap` (PSP name) and the inline signatures in `SendProposal`/`SendInvitation` (agency vs PSP name) — inconsistent today (quick-send signs PSP, compose signs agency). Cosmetic, but worth aligning with the header identity.

**Centralized?** Yes for headers (EmailDAO). **Scattered?** Only in what each caller *passes in* (`fromWho`) and in the body signatures.

---

## 4. Identity context available at the injection point vs. new plumbing needed

| Needed for a per-message From/Reply-To decision | Available today? | Where |
|---|---|---|
| **Sender's real email** | ✅ Yes at every live call site | `local.getCurrentPerson().getEmail()` (interactive) / `Email.getCreatedBy().getEmail()` (model) |
| **Sender `Person` object** | ✅ At call sites — **but ❌ not inside `EmailDAO`** | `EmailDAO.sendEmail` receives only `String fromWho` + `em`; it never sees the `Person` |
| **Originating agency** | ⚠️ Resolvable, not delivered | Proposal paths: `OriginatingAgencyResolver.resolve(Proposal)` (source-agent → prospect.agent → createdBy). Sender-based: `SendProposal.resolveSenderAgencyName` re-implements "first agency of this agent" inline; `OriginatingAgencyResolver.agencyOf(Person)` does the same but is **private**. No public `resolve(Person)` helper. |
| **Agency email config (sending domain / verified flag)** | ❌ Does not exist | — (see §5) |

**Plumbing gap:** to decide From/Reply-To per message *inside* the choke point, `EmailDAO` needs the **sender `Person` and/or resolved `Agency`** (or a small resolved "email identity" value) passed in — none of which it receives today. Options the design will weigh: (a) extend `EmailDAO.sendEmail` to accept the sender `Person`/`Agency`; (b) resolve identity at the call sites and pass explicit `fromHeader`/`replyTo`/`displayName`; (c) a new public `OriginatingAgencyResolver.resolve(Person)` (promote the private `agencyOf`) so any path can resolve an agency from a sender.

---

## 5. Mail-send API gaps

`EmailDAO.sendEmail` overloads accept `fromWho, to[, cc, bcc], subject, htmlBody, em` (and the `Email` model variant). **Missing to express the target model:**
- **No explicit `From:` header parameter** — From is derived (SMTP_FROM → fromWho → SMTP_USER). Can't say "send From `user@admin.swbd.com`" without changing this logic.
- **No `Reply-To` parameter** — hardcoded to `fromWho`. Can't say "From = agency subdomain, Reply-To = agent's real address."
- **No display-name parameter** — `new InternetAddress(addr)` only; JavaMail supports `new InternetAddress(addr, personal)` but it's not wired.
- **No envelope-from parameter** — always equals the header From. For SPF, the design may want envelope-from on a Superior-State/relay bounce domain independent of the header From.

So the send API **must be extended** (new params or a small `EmailIdentity`/`MailFrom` struct: header-from, display-name, reply-to, optional envelope-from) to carry a per-agency decision. `EmailTemplate` needs no change for headers (it's body-only).

---

## 6. Agency schema surface for a future email config (informational — no design here)

`Agency` (`model/sales/agency/Agency.java`) fields today: `id`, `name`, `taxId`, `phone`, `suppressed` (V057), `markupEnabled` (V067), **`landingHost`** + **`landingHtml`** (V068), `psp`, `address`, `primaryContact`, `manager`, `agencyRateList`, `agentList`.

A per-agency **email** config would naturally add (conceptually — *not* designed/reserved here): a **sending domain/subdomain** (e.g. `admin.swbd.com`) and a **verified-in-relay flag**. Note the intended sending subdomain is the **same `admin.swbd.com` already captured as `landing_host` (V068)** — the design should decide whether email reuses `landing_host` or adds a dedicated `email_domain` + `email_verified` (likely dedicated, since web-host presence ≠ relay verification). Natural home: on `Agency`, beside `landing_host`. **Current highest migration: V068** (next free = V069) — stated for reference only; no migration reserved.

---

## 7. Open questions for the design phase

1. **Why is `SMTP_FROM` blank?** Intentional (so From follows the sender) or an un-set default? The answer determines whether the fix is "start setting SMTP_FROM per-agency" vs. "add explicit From plumbing." (The PSP Settings UI can set it — `UpdatePspSettings` — but it's empty.)
2. **What does SMTP2GO actually do today** with `From: user@swbd.com` from an account where only `superiorstate.net` is verified — silent relay (spam-bound), or 550 reject? Determines whether agent sends currently *fail loudly* or *land in spam*.
3. **Envelope-from strategy:** keep envelope-from on a Superior-State/relay bounce domain (SPF aligned there) while header-From is the agency subdomain (DKIM-aligned via a per-agency DKIM key in the relay)? This is the standard "aligned white-label" shape and drives what the API must carry.
4. **Per-agency verification source of truth:** is "verified" a manual admin flag, or read back from the relay's domain-verification API? Governs the `email_verified` semantics.
5. **Reuse `landing_host` or a separate `email_domain`?** Web front door and mail domain are conceptually the same subdomain but verified through different channels (DNS A/CNAME vs. relay DKIM/SPF/verification).
6. **Fallback identity:** when an agency isn't verified, the target model sends `From:` a Superior State address, `Reply-To:` the agent. Confirm the exact fallback From (`SMTP_FROM`? a fixed `noreply@superiorstate.net`? per-PSP?) — today none is guaranteed because `SMTP_FROM` is blank.
7. **System mail (#7/#8) and automation (#4):** should these stay on `noreply@superiorstate.net` regardless of agency, or also white-label? An identity model must state the rule for *every* path or some keep sending as the wrong identity.
8. **Dead `AmsDataLocal.sendEmail`:** delete or fold into `EmailDAO` as part of the work, so there's exactly one header-setting path.

---

## Guardrails honored
Read-only: no files edited, no migration/DDL, no git mutation. No secret values printed (SMTP password / API keys were never read; `SMTP_FROM`/`SMTP_SERVER`/`SMTP_PORT` are non-secret config and `SMTP_USER` reported only by shape). Mapping only — no schema or code designed.
