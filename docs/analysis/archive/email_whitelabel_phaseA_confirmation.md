# Phase A — Read-Only Confirmation: Per-Agency White-Label Email Sending

**Status:** READ-ONLY confirmation pass complete. No code/migration/DDL/git mutation. **Awaiting approval before Phase B.**
**Branch:** `refactor/modernize-architecture` · Date: 2026-07-09
**Builds on:** `docs/analysis/email_identity_current_state.md`

**Headline:** every anchor from the prior investigation still holds; `V069` is free; the deliverability test has **not** been run, so per the locked rule Phase B **removes the `mail.smtp.from` override** (corroborated by the infra docs — SMTP2GO owns the return-path via its `em102001` CNAME VERP). Exact change list at the end. **Then STOP.**

---

## Step 1 — Choke point + signatures confirmed

`data/dao/EmailDAO.java` (unchanged since the investigation; V068 did not touch it):
- **Overloads:** `sendEmail(from,to,subj,body,em)` (46–51) → `sendEmail(from,to,subj,body,em)` list (53–56) → **fully-specified** `sendEmail(fromWho, to, cc, bcc, subject, htmlBody, em)` (59–138); plus the `Email`-model `sendEmail(Email, em)` (142–166).
- **From logic (74–89):** `smtpFrom = SMTP_FROM`; if blank → `fromWho` (valid email) else `SMTP_USER`; validates or throws; `msg.setFrom(new InternetAddress(smtpFrom))` — **no display name** (single-arg ctor).
- **Reply-To (92–94):** `if (isValidEmail(fromWho)) msg.setReplyTo(fromWho)` — hardcoded to `fromWho`, not a parameter.
- **Envelope override (127):** `session.getProperties().put("mail.smtp.from", smtpFrom)` — sets return-path = header From. **This is the line Phase B removes** (see Step 5).
- **Session (170–205):** all SMTP_* via `AppConstantDAO.getConstantValue(em, ...)`; STARTTLS required; explicit `Transport`.

These are the exact hook points for the `EmailIdentity`-aware overload.

## Step 2 — 8 live call sites + dead path confirmed

| # | Path (class:line) | `fromWho` source |
|---|---|---|
| 1 | `SendProposal.doPost:122` | `sender.getEmail()` (`local.getCurrentPerson()`) |
| 2 | `ProposalDetail:108` (`sendToProspect`, fromEmail @:90) | `sender.getEmail()` |
| 3 | `SendEmail25:114` (`EmailDAO.sendEmail(Email,em)`) | `Email.getCreatedBy().getEmail()` |
| 4 | `SendAutoFinal25:192, :362` | `Email.getCreatedBy().getEmail()` |
| 5 | `SendInvitation:164` | `local.getCurrentPerson().getEmail()` |
| 6 | `SendEmployerBillingDetail:52` | `Email.getCreatedBy().getEmail()` |
| 7 | `CreateUser25:330` | literal `"noreply@superiorstate.net"` |
| 8 | `HelpUserLogin:89` | literal `"noreply@superiorstate.net"` |

**Dead path:** `AmsDataLocal.sendEmail(EntityManager, AmsDataGlobal)` at **line 1685** — own `Transport.send`, `From = getSender().getEmail()`, no Reply-To. **Zero callers** (re-grepped `\.sendEmail\(` and bare `sendEmail(` — only the definition; no invocation). Safe to delete in Phase B.

**Non-senders (unchanged):** `EmailBillingToEmployer` (builds link → forwards to JSP), `AcceptInvite` (no send).

## Step 3 — Identity context + resolver confirmed

- **Sender `Person` + real email:** available at every live call site — `local.getCurrentPerson()` (#1,2,5) or `Email.getCreatedBy()` (#3,4,6). `Person` accessors verified: `getFirstName()` (:58), `getEmail()` (:84), `getPsp()` (:116), `getListOfAgenciesWithThisAgent()` (:132).
- **Agency resolution:** `OriginatingAgencyResolver.resolve(Proposal)` is **public** (:67); `resolveAgent(Proposal)` public (:77); **`agencyOf(Person)` is private** (:116) — Phase B **promotes it to a public `resolve(Person)`**. `SendProposal.resolveSenderAgencyName` (:151) **duplicates** the "first agency of this agent" logic → replaced by the public resolver.
- **Plumbing gap (as expected):** `EmailDAO.sendEmail` receives only `String fromWho` + `em` — never the `Person`/`Agency`. Phase B closes this by resolving an `EmailIdentity` **at each call site** and passing it into the new overload (keeps `EmailDAO` free of session/context coupling).

## Step 4 — Agency entity + migration version confirmed

- `Agency` (`model/sales/agency/Agency.java`) current columns: `agency_id`, `agency_name`, `tax_id`, `phone`, `suppressed` (V057), `markup_enabled` (V067), **`landing_host`, `landing_html`** (V068), `psp_id`, `address_id`, `contact_id`, `manager_id`, + `agencyrates`/`agents` joins. **No email fields yet.**
- **Migration version:** highest on disk = **V068** (`docs/migrations/`); `schema_version_migration.sql` registers through V068; `V069` is **FREE**. `migration_tracker.md` header already reads **V068** (fixed in the V068 commit) — **no lingering V066/V067 lag**; Phase B just bumps to V069 + adds one row.

## Step 5 — Envelope decision (SETTLED)

**No deliverability test result exists in the repo** — searched all of `docs/infrastructure/`, `docs/analysis/`, `docs/deployment_backlog.md`: no mail-tester score, no Gmail "Show Original" `spf=/dkim=/dmarc=` capture. The only relevant evidence is the DNS setup: SMTP2GO sending-domain verification uses **CNAME `em102001`** (return-path/VERP) + **DKIM TXT `s102001._domainkey`** (`production_architecture.md:74–78`, `cloudflare_setup.md:71`).

**Decision (per the locked default): Phase B REMOVES the `mail.smtp.from` override (EmailDAO:127).** Rationale: SMTP2GO's own `em102001` return-path is how per-domain SPF alignment is achieved; the explicit override forces envelope-from = header From, which *fights* that VERP for a verified agency domain. Removing it lets SMTP2GO own the return-path (aligned for verified domains, harmless for PSP domains). Implementation keeps a nullable `envelopeFrom` on `EmailIdentity` (resolver returns `null` for all four tiers in v1 → override not set); the field is a future hook, not used now.

**Flag:** Phase B proceeds on the "remove override" default. If the developer later runs the test and finds SMTP2GO requires an explicit envelope, the nullable `envelopeFrom` hook makes that a one-line resolver change.

## Step 6 — Config surface + UI anchors confirmed

- **`VERIFIED_PSP_DOMAINS`** → **`AppConfig.get("VERIFIED_PSP_DOMAINS", "superiorstate.net,superiorstate.biz")`**, comma-split + lowercased — same `ssa.properties`/`AppConfig` pattern as V068's `PSP_HOSTS` (infra-shaped, per-installation). Distinct key from `PSP_HOSTS`.
- **`FALLBACK_FROM`** → **DB constant via `AppConstantDAO.getConstantValue(em, "FALLBACK_FROM")`**, default `notifications@superiorstate.net` — matches how `EmailDAO` already reads `SMTP_FROM`/`SMTP_USER` (PSP-editable through the settings surface, no redeploy). Read inside the resolver/`EmailDAO` where an `em` is in hand.
- **Agency Manager UI:** `WEB-INF/view/sales/agencyManager25.jsp` `#editAgencyModal` — the V068 **Landing Host** input sits in the left "Agency Info" column (added beside the `markupEnabled` checkbox). The new **Sending Domain** (`email_domain`, JS pre-fill from the landing-host field) + **Verified** toggle (`email_verified`) attach right there.
- **Servlet:** `AgencyAction.editAgency` case at **line 128**; the V068 `landingHost` validate/store block is **131–152** (`agency.setLandingHost(...)` at 152). The `email_domain` read/validate/store + `email_verified` toggle hook **immediately after line 152**, reusing the existing `validateLandingHost` pattern (`HOST_PATTERN` + duplicate pre-check) for the domain-format + duplicate-`email_domain` checks. `AgencyAction.doPost` is already `isPspAdmin`-gated (V067) and fires `refreshSalesData` after edits.

---

## Exact change list for Phase B (no edits made)

**Migration + docs**
1. `docs/migrations/V069__agency_email_sending.sql` *(new)* — `ADD COLUMN email_domain VARCHAR(255) NULL`, `ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0`, `ADD UNIQUE INDEX uq_agency_email_domain (email_domain)`; prereq note V057/V067/V068; self-register `'V069'`. V068 style.
2. `docs/schema_version_migration.sql` — append `('V069', …)`.
3. `docs/analysis/migration_tracker.md` — header → V069, add V069 row + prose note.
4. Email onboarding doc — **no `AGENCY_CUSTOM_DOMAIN_ONBOARDING.md` exists**; Phase B adds an **Email Sending** section to `docs/infrastructure/cloudflare_setup.md` (the existing SMTP2GO-DNS home) covering: verify `email_domain` in SMTP2GO (Sender Domains → `em102001` SPF return-path CNAME + `s102001._domainkey` DKIM at the subdomain), then PSP-admin flips `email_verified`; **strict gate: never flip `email_verified` before SMTP2GO shows Verified.**

**Java**
5. `model/sales/agency/Agency.java` — add `emailDomain` (`@Column(name="email_domain")`) + `emailVerified` (`@Column(name="email_verified")` boolean) + accessors.
6. `data/util/EmailIdentity.java` *(new)* — immutable value object: `fromAddress`, `displayName`, `replyTo`, `envelopeFrom` (nullable).
7. `data/util/EmailIdentityResolver.java` *(new)* — `resolve(Person sender, Agency agencyOrNull, PSP psp, EntityManager em) → EmailIdentity` implementing Tiers 0/1/2 (Tier-0 domain ∈ `VERIFIED_PSP_DOMAINS`; Tier-1 localpart graft onto `email_domain` when `email_verified`, malformed-localpart → Tier-2; Tier-2 `FALLBACK_FROM` + sender full-name display; all human tiers `replyTo` = sender real email; `envelopeFrom` = null) + `systemIdentity()` → fixed `noreply@superiorstate.net`, no reply-to.
8. `data/dao/EmailDAO.java` — new `sendEmail(EmailIdentity identity, to, cc, bcc, subject, htmlBody, em)` overload: `new InternetAddress(addr, personal)` for display name, `setReplyTo` from identity, set `mail.smtp.from` **only if** `identity.envelopeFrom != null` (i.e. **remove the unconditional override at :127**). `identity == null` → preserve today's behavior (rollout safety).
9. `data/resolver/OriginatingAgencyResolver.java` — promote `agencyOf(Person)` to **public `resolve(Person)`**; keep `resolve(Proposal)`.
10. **Call-site wiring** — #1–#6 resolve an `EmailIdentity` (sender = current person / `Email.createdBy`; agency = `resolve(Proposal)` for #1/#2, `resolve(Person)` for #5/#6, `resolve(Person)` on `createdBy` for #3/#4) and pass it in; replace `SendProposal.resolveSenderAgencyName`. #7/#8 use `EmailIdentityResolver.systemIdentity()`.
11. **Delete** dead `AmsDataLocal.sendEmail(EntityManager, AmsDataGlobal)` (~1685) + its private `getMessage(AmsDataGlobal)` helper if unused elsewhere (confirm on delete).
12. `data/util/EmailTemplate.java` — **signature-source fix**: drive the body signature from the resolved identity (agency name for Tier 1; consistent PSP/agency for others), resolving the quick-send-signs-PSP vs. compose-signs-agency contradiction. Body-only; wrapper structure untouched.
13. Config readers: `VERIFIED_PSP_DOMAINS` (AppConfig) + `FALLBACK_FROM` (DB constant), defaults as above.

**JSP**
14. `WEB-INF/view/sales/agencyManager25.jsp` — `#editAgencyModal`: **Sending Domain** input (`email_domain`, JS pre-fill from the landing-host field) + **Verified** toggle (`email_verified`, PSP-admin only), wired to `AgencyAction editAgency` with domain-format + duplicate-domain validation (friendly redirect like `landingError`).

---

## Guardrails honored (Phase A)
Read-only — no files edited, no migration/DDL, no git mutation. No secret values read/printed (`SMTP_PASSWORD`/API keys never queried). Design not re-litigated; the one test-dependent line (envelope) settled against the evidence (test absent → remove override, with a nullable hook to reverse cheaply).

**HARD STOP — awaiting approval to begin Phase B.**
