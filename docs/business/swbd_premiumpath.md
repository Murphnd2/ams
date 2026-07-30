# SWBD / PremiumPath

**Type:** Agency/GA demand + benefits-administration partnership ·
**Status:** **Active** — solution proposal delivered to Forrest **2026-07-15**; pilot-first.

## Summary

Southwestern Benefit Designers (SWBD), "**PremiumPath**" brand — a Texas GA / FMO-style consultancy
(Forrest Huggins). The engagement has grown from "first white-label agency" into a
**benefits-administration partnership**: SSA administers the **PremiumPath Program** for SWBD's
small-employer clients, delivered under SWBD's white-label brand (`premiumpath.net`).

**The PremiumPath Program** (Texas micro-groups, ~10 lives or fewer):

- **Management side** — owners / key employees on a **non-ACA, major-medical-style plan (Presidio)**,
  funded **post-tax** ("bonus up"), premiums paid via the **PremiumPath Card**.
- **Staff side** — a **minimal QSEHRA** ($50/mo, or the employer's chosen level) that (a) triggers a
  **federal Special Enrollment Period**, and (b) **preserves employees' premium tax credits** (de
  minimis — supplements rather than replaces the subsidy).
- **One payroll-deduction experience** to pay premiums; voluntary/ancillary products run post-tax on
  the card; optional **HSA+** (§125 payroll HSA) for bronze-plan employees.

**Branding / name:** **"PremiumPath" is a working placeholder name** Kevin coined after the 7/14 call
(rolling with Forrest's half-joke about patenting the concept). The agency landing site + custom domain
(`premiumpath.net`) are live, and Forrest's agency, users, and custom proposals are configured to
match. **Name/branding is deferred to Forrest** for final direction — fully rebrandable via config
(Landing Host / Sending Domain toggles per the white-label runbook), no code change required.

## The relationship

- **Forrest Huggins** — SWBD owner; problem-solving consultant/FMO, not a pure producer.
- **Tracy** — SWBD office / operations support.
- **Presidio (Daniel Cruz, CEO/actuary; Bob Hogan, CLO)** — Presidio Healthcare Insurance Company
  (Irving/Dallas TX), product **"FortressPlan"**: a **non-ACA, medically-underwritten major-medical-style**
  plan ($0 deductible / 50% coinsurance to an annual MOOP). TDI-licensed Aug 2025, launched Nov 2025,
  **Texas-only** (expansion targets FL/IN/GA/NC/OH); filed as a Catholic entity (ERD-aligned), with faith
  **and mental-health** exclusions (the MH exclusion is the actuarial tell — this product's identity is
  underwriting, not just doctrine). **Almost certainly NOT MEC** (most likely an excepted-benefit filing).
  **Whether the premium is QSEHRA/§213(d)-reimbursable turns on the filed TDI form — the make-or-break
  unknown (Q1); do not treat "expense-based, not indemnity" as settled.** Presidio–SWBD relationship
  unknown → ask.
- **Annette Bechtold** (Forte Consulting, Atlanta) — made the intro; compliance guidance
  (ICHRA/QSEHRA/§125).

## Status & history

- **2026-07-13 (Mon)** — Intro capabilities call (with Annette). Established SSA can administer QSEHRA
  + individual list/direct billing with consolidated remittances for Forrest's Presidio model.
  Post-tax basis agreed (§125 pre-tax not feasible). Pilot-first.
- **2026-07-14 (Tue)** — Deep-dive (Forrest, Tracy, Kevin). Locked the management-Presidio-post-tax +
  staff-minimal-QSEHRA model and the PremiumPath Card mechanism. Forrest's ask: fees/pricing + a
  revenue share; numbers by Wednesday. (Forrest also *assumed* SSA would have a "setup video" — an
  offhand remark in his closing recap, not a firm request; see transcript.)
- **2026-07-15 (today)** — SSA proposal delivered: *"The SWBD PremiumPath Program — Services & Pricing."*
- **2026-07-15 (send)** — Full package emailed to Forrest: (1) **Services & Pricing** PDF, (2) the
  **live client-facing sample proposal** on the SWBD-branded hub (`premiumpath.net`, with a working
  apply flow — sent as a link, not a static file), and (3) a **PremiumPath Quick-Start** walkthrough
  guide. Framed as "built, not just spec'd": Forrest given his own agent login (`fhuggins@swbdmg.com`,
  scoped to SWBD) to drive the live hub. Awaiting his response, including direction on the program name.
- **2026-07-28 (ICHRA-admin thread)** — Call with Forrest re: quoting. Origin decoded: SWBD had been
  quoting ICHRA for **Sandoval Process Solutions** (3 EEs, Hopkins TX, 9/1/26) through **zizzl health**,
  which gated carriers (turned off Christus; wouldn't put Forrest's carriers on the quote) and charged a
  ~$660/mo minimum admin fee. Forrest's unbundle logic: if he's doing the carrier legwork anyway, better
  to administer elsewhere (SSA). Call also surfaced a **minor §125 opportunity** (details TBD). Forrest is
  focused on integration and raised **HealthSherpa** ("Sherpa") — now under evaluation as the ICHRA
  quote/enroll rail (see `healthsherpa.md`).
- **2026-07-29 (+ tier design session)** — ICHRA+/QSEHRA+ scoped as new LOS bundling HealthSherpa services
  and automated coverage verification; standard ICHRA/QSEHRA unchanged for existing agencies. Summit
  card-transaction and mailing/coverage-event exports both confirmed viable. Single bundled PEPM (card
  included) — verification method varies per participant and can't be a proposal-time election. Billing
  stays on the existing headcount-to-Wave path. Full design and decision log in `plus_tier.md`.

## Opportunity streams (Forrest's framing)

1. **Larger TPA relationships** seeking ICHRA options for **under-25 / under-50 markets**.
   - *Now concrete (2026-07-28):* the **ICHRA-admin / zizzl-displacement** angle. SSA's wedge is lean
     small-group admin economics (vs. zizzl's ~$660/mo minimum) + carrier flexibility; the quoting piece
     is solved by integrating a **CMS-approved EDE (HealthSherpa)** rather than a gated vendor — see
     `healthsherpa.md`.
2. **Small employers (<10 lives)** — per-head admin fees + **revenue share** to SWBD.

Both parties want a **non-VC-backed, flexible, long-term** relationship with room to experiment.

## Revenue / pricing model

- SWBD's margin rides on top of SSA's base **as an administrative fee** (not insurance commission):
  **baked-in SWBD rate card**, **per-quote custom markup**, or **both** (guaranteed baseline +
  additional). One combined employer invoice; monthly remittance to SWBD with a per-group statement.
- Program fees (from the 2026-07-15 proposal): $500 one-time establishment · $50/mo combined minimum ·
  QSEHRA base $350/yr + $5/participant/mo · PremiumPath Card $200/yr + $3.50/participant/mo + $2 card
  issuance · HSA+ add-on $250 doc + $150 NDT + $15/app + $5/mo. (Full detail in the proposal doc.)

## AMS work this drives

**Delivery layer (built):** white-label portal + email (V068–V071), agent markup (V066/V067),
GA→sub-agency hierarchy (V070/V071), open decision #39.

**Program layer (largely existing AMS billing capability, productized as "PremiumPath"):**

- **PremiumPath Card** — a custom **post-tax insurance-payment benefit + debit card**: employee-funded
  via voluntary post-tax payroll deduction (employer holds the funds as a payroll liability — no money
  moves to SSA), card in the employee's own name, **restricted to insurance-carrier merchant codes**,
  spendable only up to the payroll-deposited balance, used as **carrier autopay**; **one consolidated
  settlement draft** to the employer per payment day.
  **Build status (per brief v4):** this is now a **DataPath Summit configuration in progress**, not an
  AMS build — plan template built on the existing Summit/COMPASS rails (post-tax, participant-funded,
  Contribution-Schedule-driven, MCC 6300 loaded). Remaining gates: **live carrier authorization test**
  + **issuing-bank purse classification**. Rename the working template code **"PTC"** before client
  exposure (collides with *premium tax credit*) → PIP/PPA; participant-facing "Insurance Payment Account."
- **Agent-facing value-add (e.g. persistency / lapse alerts) is SWBD's to offer, not SSA's promise.**
  Delivered via **agency-level Summit broker access** — SWBD *staff* run enrollment/status reporting
  across their book (agency-wide login; **not** per-agent logins, **not** agent-filtered). SSA provides
  the broker access at **no charge**; whether SWBD features it to its downline is SWBD's revenue-gated
  call. This lives in the **consolidated SWBD (agent/network-facing) proposal**, never as a
  client/employer promise and never on the SSA service agreement.
- **QSEHRA-Lite admin** — plan docs + adoption paperwork, SEP documentation + **dated per-employee
  eligibility letters** (Marketplace proof), reimbursement by direct deposit/check, W-2 Box 12 Code FF.
  **Administered on DataPath Summit** as well (a separate plan type from the card); the likely **AMS**
  role is **enhancements to gather the monthly attestations** — which ties directly to backlog **#38**
  (QSEHRA attestation / transaction-match engine), whose blocker is exactly the Summit/COMPASS
  transaction feed this card build produces.
- **Individual-level list/direct billing + consolidated remittance** (existing platform: any benefit
  item tied to an individual, online/coupon payments, consolidated billing, remittances).
- **HSA+** — §125 payroll HSA for bronze-plan employees (the one recommended cafeteria plan).
- **Carrier premium quoting / "display all options":** **do not build a shopping/steering layer.** It
  drifts toward broker/steering (endorsement risk under Notice 2017-67 Q&A-55) and competes with SWBD's
  own agents (kills the differentiator). A neutral rate *menu* is generally fine; steering is not; any
  enrollment-for-commission needs a licensed agent or a CMS-approved EDE partner. A neutral quoting
  convenience (e.g. a Sherpa-type API) is a **volume-gated maybe**, not a roadmap commitment.
  **Update (2026-07-28):** HealthSherpa is the concrete "CMS-approved EDE / Sherpa-type API" this bullet
  anticipated — integrating a **free** EDE API is a different calculus than building a rate engine from
  scratch, so this moves from "don't build" to **"integrate, under evaluation"** (see `healthsherpa.md`).

## Compliance guardrails (this model)

- **No §125 for the voluntary lineup.** IRS **Notice 2017-67**: an employer offering *any* group
  health plan — expressly including excepted-benefit-only plans (dental, etc.) — is **disqualified from
  offering a QSEHRA**. Keep voluntary products **post-tax** (on the card). See
  `docs/analysis/domain_and_compliance_rules.md`.
- **Management "bonus up," not employer-paid.** Employer raises post-tax pay → employee funds their own
  card → card autopays the carrier. An **employer-paid** carrier arrangement counts as sponsoring a
  health plan → disqualifies the staff QSEHRA + exposes the employer to the **$100/day/employee** excise
  tax.
- **Post-tax throughout** (§125 pre-tax not feasible here, per Annette/Kevin).
- **Entity eligibility:** no classes/opt-outs; C-corp owner/employees can participate; **sole
  proprietors, partners, and >2% S-corp shareholders cannot**. Flag entity type at intake before quoting.
- **MEC floor (wrapper prerequisite):** a QSEHRA can only reimburse if the participant holds **MEC
  somewhere** — no MEC anywhere = **no reimbursements at all**. FortressPlan is almost certainly not MEC,
  so any wrapper needs cheap MEC (bronze/catastrophic/Medicaid/spouse plan) held elsewhere to open the
  gate. (QSEHRA accepts any-source MEC — spouse/parent/Medicaid/Medicare; ICHRA locks those people out.)

## QSEHRA "stack-wrap" / wrapper play — now the leading hypothesis

Raised by Kevin on the 7/14 call; the SWBD pre-meeting brief (v4) **elevates this from a parked idea to
the leading read** ("Scenario W"). Still **gated on the filed-form answer (Q1) + counsel sign-off** before
it can be relied on:

A **larger QSEHRA** where the employee buys a **bronze ACA** plan that does *not* consume the full
QSEHRA; the **leftover QSEHRA funds** then reimburse the premium of a **non-ACA, expense-based (not
indemnity) underwritten supplemental** policy — effectively **stacking/wrapping** QSEHRA + bronze ACA +
supplemental into near-comprehensive coverage.

- Only works if the supplemental pays **on expenses, not indemnity chunks** (indemnity premium is not
  QSEHRA-reimbursable).
- Different employer segment — those willing to fund toward the **max** end of the QSEHRA (vs. the
  de-minimis $50 model above).
- Needs the supplemental to pay on **expenses, not indemnity** — but for FortressPlan this is **not yet
  known**: §213(d) reimbursability turns on the **filed form** (expense-incurred qualifies; per-period
  cash does not; a per-service fixed schedule is a gray zone). Resolve via Q1 before relying on it.
- Bigger opportunity, more design/compliance work — park until the pilot lands, then raise with Forrest.

## Open items / next

- Deliver pricing to Forrest — **done 2026-07-15** (full package sent; see history). The "setup video" was never a firm ask; the PremiumPath Quick-Start guide serves that purpose.
- Agree a **pilot case**; map the end-to-end workflow (Forrest to provide a flowchart).
- **Producing-agent count** — the opportunity-sizing question.
- Decision on **carrier quoting / Sherpa** (volume-gated).
- Develop the **stack-wrap** opportunity (above) as a later track.
- **Minor §125 opportunity** surfaced on the 2026-07-28 call — details TBD from Kevin.
- **HealthSherpa ICHRA integration** — read-only evaluation (see `healthsherpa.md`, backlog #42).
- ICHRA+/QSEHRA+ build — requirements settled, data model designed. See
  `docs/analysis/plus_tier_build_plan.md` (canonical) and `plus_tier.md` (design intent). The former
  "EDE consent" open item was **malformed** — there is no EDE transaction on the off-exchange ICHRA
  Partner API rail, and the per-employee artifact is required regardless (PTC waiver + MEC
  substantiation). Current highest-value open items are **O34** (does Summit's Participant Custom ID
  round-trip in the mailing export and J2/J3 — it may retire the SSN hash entirely) and **O18**
  (counsel: does a signed attestation suffice as a reimbursement-release record).

## Positioning, structure & liability (from brief v4)

- **Competitive set (bake-off):** Take Command · Thatch · SureCo · Venteur · PeopleKeep. Differentiate on
  **independence** (no competing agency, no carrier ownership), white-label flexibility, **design-time
  QSEHRA sizing** (the hold-harmless screen), the **certification package**, and the card on DataPath rails.
- **White-label structure:** push **Model 1** — SSA contracts with each employer; SWBD brands the
  experience; SSA is the named administrator in legal docs; SWBD's fee rides on the PEPM as a transparent
  **admin-fee split** (one employer invoice; remit SWBD's add-on monthly — not insurance commission).
  Fallback **Model 2** (SWBD contracts, subcontracts SSA) only if he insists, priced higher. Either way,
  **plan docs stay legally accurate about sponsor (employer) / administrator (SSA)** regardless of branding,
  and **no carrier name appears on any SSA-drafted paper**.
- **Advise-and-certify (liability):** three buckets — employer conduct (→ §4980D), agent market-conduct
  (steering), and SSA's narrow controllable lane. Sell the **certification package** (employer
  responsibilities guide + onboarding/annual re-cert + scoped services agreement) as part of what the PEPM
  buys.
- **TPA licensing (parked, revisit pre-scale):** administering plans may implicate a Texas TPA
  certificate of authority (Tex. Ins. Code ch. 4151) and, given SWBD's multi-state footprint, state-by-state
  review. Note: **SSA never holds participant funds**, which removes the money-transmitter / premium-
  collection angle — but the ch. 4151 question attaches to the *act of administering*, not to fund custody.
  Low priority for now per Kevin; counsel read before scaling past a pilot.

## Related

- White-label onboarding: `docs/runbooks/agency_white_label_domain_onboarding.md`
- Compliance rules: `docs/analysis/domain_and_compliance_rules.md`
- Opportunity register: `docs/business/README.md` · DataPath: `docs/business/datapath.md`
