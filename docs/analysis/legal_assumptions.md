# Legal Assumptions Register

**Status:** Active — the standing record of compliance-relevant design decisions made without counsel
**Created:** 2026-07-31
**Owner:** Kevin
**Related:** `docs/analysis/domain_and_compliance_rules.md` · `docs/ichra_strategy.md` · `docs/business/plus_tier.md` · `docs/analysis/plus_tier_build_plan.md` · `docs/analysis/qsehra_attestation_claims_engine.md` · `docs/business/healthsherpa.md` · `docs/business/swbd_premiumpath.md`

---

## ⚠️ This is not legal advice

**No lawyer has reviewed anything in this document.**

SSA has no engaged legal counsel, and is deliberately not buying legal opinions until the ICHRA market
is proven. That is a considered business decision, not an oversight — opinions are expensive, and the
questions worth paying for are not yet the questions that are actually blocking.

Development proceeds anyway. Every entry below is a **product design decision made from research into
primary sources** — statutes, regulations, IRS and DOL guidance. Every entry is an **assumption, not a
conclusion of law**. None of them has been confirmed by anyone qualified to confirm it.

**This document exists so those assumptions are visible and priceable rather than buried in code.**
A design choice that rests on an unconfirmed reading of a regulation is not a problem as long as
somebody can find it later. It becomes a problem when it is indistinguishable from a decision that was
actually checked. Every entry here is the former.

**A second caveat, specific to the citations.** Section numbers, Federal Register cites and Q&A
references below were compiled from research and are recorded to make each entry *findable*, not to
make it *authoritative*. They were not read from primary text in the session that wrote this file
(no network access). **Verify every citation against the actual text before any of this goes to
counsel, into a client-facing document, or into a plan document.** A wrong cite in a register of
assumptions is a navigation error; a wrong cite in a plan document is a different kind of problem.

When counsel is eventually engaged, this document is the scope of work. Nothing here is deleted when
it resolves — entries move to **Confirmed** or **Disproven** with a date, and the reasoning stays
visible so the next reader can see what was believed and why.

---

## The standing principle

> **Prefer the design choice that is cheap to reverse.**

Where an assumption might be wrong, choose the option whose reversal costs a configuration change or a
display edit — never a rebuild, and never something already done to a real participant, a real
employer, or a real dollar.

**Reversal cost is a first-class field in every entry, not a footnote.** It is the field that tells a
developer later which assumptions are worth paying counsel to confirm and which can ride. Two
assumptions can carry identical legal uncertainty and be worlds apart in what being wrong costs: one
is a JSP edit, the other is money already sent to a participant on a substantiation theory that turns
out not to hold. The register ranks by *that*, not by how uncertain the law is.

The corollary is a sequencing rule and it does real work in this project: **an assumption whose
reversal is cheap does not need to be resolved before it is relied on.** LA-04 is the clean example —
the design keeps carrier names off everything, not because the research says it must, but because
either answer costs one display edit, so there is no reason to spend the option early.

---

## The scope line — build now versus defer

The boundary that keeps demo-stage exposure small. It is a scope decision, not a legal conclusion, and
it is the reason most of the entries below carry a **Confirm before** trigger that has not yet fired.

### Build now — agent-facing and analytical

Nothing in this layer **adjudicates a claim, handles funds, enrolls anyone, or is seen by an
employee.** The audience is a licensed agent looking at market math.

- Market cost illustration (rating-area ranges, age-band net cost)
- Affordability analysis computed for the employer
- Subsidy segmentation
- Class design
- Group-to-ICHRA conversion analysis

### Defer — employee-facing and transactional

- Employee-facing plan displays
- Enrollment paths originating in AMS
- Reimbursement release to a real participant

**This is where licensing, ERISA and substantiation actually bite, and none of it is required to
demonstrate the product.** The heavy entries below — LA-01, LA-03, LA-09, LA-10 — all sit on the
defer side of this line or are made survivable by it.

### Why this costs nothing

**The boundary coincides exactly with the existing strategy.** `ichra_strategy.md` §1 puts agent
utility as the wedge and administration revenue as what it earns; §3 rows 1–4 are agent-facing
analytics that "need no HealthSherpa approval, no BAA, no counsel, and no Gate 0." **The low-exposure
path and the high-value path are the same path.** That is a fortunate accident, and it should be
spent deliberately rather than drifted across.

---

## The register

### Index

| # | Assumption | Reversal cost | Confirm before | Status |
|---|---|---|---|---|
| **LA-01** | Signed attestation suffices to release a QSEHRA/ICHRA reimbursement | **Rebuild** / **irreversible** once paid | First real reimbursement | Assumed |
| **LA-02** | Attestation requires an annual coverage baseline on file | Code change | First reimbursement of any plan year | Assumed |
| **LA-03** | An attestation cannot be honored against actual knowledge it is false | Code change (**rising** with each feed ingested) | First policy-status feed ingest | Assumed |
| **LA-04** | Carrier names *may* appear in employee-facing displays — but will not | **Display edit, either direction** | First employee-facing display | Assumed (design defers) |
| **LA-05** | Any plan display AMS can build is definitionally incomplete | Display edit | First employee-facing display | Assumed |
| **LA-06** | No employer-driven ordering in any plan display | Config / code change | First employee-facing display | Assumed |
| **LA-07** | QSEHRA notice runway is 45 days for a non-January effective date | Config change (date arithmetic) | First non-January QSEHRA effective date | Assumed |
| **LA-08** | ICHRA notice rules are **not known** to follow the QSEHRA analysis | Unknown — cannot price | Any ICHRA sale with a short runway | **Open — no basis** |
| **LA-09** | Agent-facing market data with no AMS enrollment path is not producer activity | Code change → **rebuild** if a display goes prospect-facing | Any non-agent audience; multi-state | Assumed — **thin** |
| **LA-10** | Texas TPA licensing (ch. 4151) is unresolved and may already apply | Unknown — potentially **licensure, not code** | Scaling past pilot; each new state | **Open — material** |
| **LA-11** | Design census stays at minimum scope | Config change | Any field added to the design census | Assumed |
| **LA-12** | Affordability is computed for the employer, not presented to the employee | Display edit — but **gated on T44 correctness** | Any employee-facing affordability figure | Assumed |
| **LA-13** | This register is internal work product and is not disclosed to partner agencies through SSA-built tools | ⭐ **Low, one direction only** — restoring disclosure is a data edit; a disclosure already made cannot be withdrawn | Any disclosure of SSA's regulatory status or the review state of its positions to a partner | Assumed |
| **LA-17** | An agent-composed, agent-sent, agency-branded proposal may carry market data to an employer without SSA becoming a producer | ⭐ **Low — display edit — and only because constraint 2 holds.** **Escalates to rebuild** if an agentless front door is ever built | A second state beyond Texas; any agentless front door; any SSA-initiated communication to a prospect | Assumed — **thin** |
| **LA-18** | Staging rate data is treated as fully authoritative, with no distinguishing marker, until production HealthSherpa access is live | Low one direction (flip the constant); **not reversible for proposals already sent under it** | Granting `agency.ichra_enabled` to any agency other than through PSP-admin access, while `ICHRA_RATE_SOURCE_ENV` still reads `STAGING` | Assumed — accepted explicitly |

⚠️ **Index gap, not fixed here.** **LA-14, LA-15 and LA-16 exist in the register below but have never
been added to this table.** Recorded rather than backfilled, because backfilling three entries someone
else wrote is a judgment about their reversal-cost wording, not a transcription. Flagged for Kevin
(S9-A, 2026-08-02).

---

### LA-01 — A signed employee attestation is sufficient substantiation to release a reimbursement

**Assumption.** A signed employee attestation of continued minimum essential coverage is sufficient
substantiation to release a QSEHRA or ICHRA reimbursement. Third-party carrier documentation is not
required.

**Basis.** For **QSEHRA**, IRS Notice 2017-67 — the sole substantive QSEHRA guidance — establishes a
two-part substantiation structure: proof of MEC once per plan year, plus an attestation of continued
coverage accompanying each payment request. The administrator may rely on that attestation **absent
actual knowledge that it is false** (see LA-03, which is the other half of the same rule). For
**ICHRA**, the final regulations (*Health Reimbursement Arrangements and Other Account-Based Group
Health Plans*, 84 Fed. Reg. 28888, June 20, 2019; codified at 26 CFR §54.9802-4) contain a
substantiation requirement — annual, at or before the first reimbursement of the plan year, and again
per reimbursement request — that is expressly satisfiable by **participant attestation**, with the
same actual-knowledge limit.

**This is the strongest-supported entry in the register**, and it is also the one whose being wrong
costs the most. Both authorities point the same way, and the attestation mechanism is not an
inference from silence — it is affirmatively described.

⚠️ **Internal corroboration, and an internal contradiction.**
`docs/analysis/qsehra_attestation_claims_engine.md` §1, written **2026-07-14**, already states the
substance of this assumption in the same terms: *"The administrator may rely on the attestation absent
actual knowledge it is false — the capture and storage of the attestation **is the liability shield**."*
That predates the framing of **O18** as an open counsel question by two weeks. The doc set has been
carrying both an answer and an unanswered question about the same thing. **This entry resolves that
contradiction in favour of the answer** — and records that the answer was always research-based, never
counsel-confirmed, which is exactly why it should have stayed visible rather than being buried in an
epic doc.

**Design choice.** `ATTESTATION` remains the first rung of D17's verification ladder and B3 is built
against it. The attestation record is append-only and immutable once captured, carries a versioned
legal-text ID, and **one attestation unlocks exactly one coverage month** (per the epic's Story 3 and
Story 5 hard rules). No attestation → that month **holds**. Late click is a late payment, never a
compliance event.

**Risk if wrong.** B3's launch configuration is invalid and has **no designed alternative** —
`CARD_TRANSACTION` is gated on the live carrier authorization test (O10) and `HS_POLICY_STATUS` is
carrier-gated with BCBS TX unscheduled and CHRISTUS not listed at all. More seriously, the exposure is
retroactive: reimbursements already released on attestation alone would be unsubstantiated, therefore
taxable to the participant, with the employer carrying the exposure — and SSA is the administrator
that designed the release rule.

**Reversal cost.** **Rebuild** if disproven before launch — the ladder's launch rung has no
replacement. **Irreversible** for any reimbursement already paid. This is the sharpest asymmetry in
the register: building the ledger against attestation is cheap and safe; *releasing a dollar* against
it is the point of no return.

**Confirm before.** **The first real reimbursement release.** Deliberately not "before B3 is built" —
building the ledger costs a phase, and the ledger is worth having under any substantiation rule.
Releasing money is the trigger.

**Status.** Assumed — 2026-07-31. Answers **O18**, which the doc set has recorded as open since
2026-07-29 and simultaneously as answered since 2026-07-14.

---

### LA-02 — Attestation requires an annual coverage baseline on file for the plan year

**Assumption.** The monthly attestation confirms *continuation* of coverage. It does not replace an
initial, plan-year-scoped coverage baseline: carrier name, coverage effective date, covered
individuals.

**Basis.** The two-part structure in LA-01's sources is genuinely two parts. The QSEHRA guidance
separates **initial proof of MEC once per plan year** from the **per-payment attestation**; the ICHRA
regulations similarly distinguish annual substantiation (at or before the first reimbursement of the
plan year) from per-request substantiation. Reading the monthly attestation as sufficient on its own
collapses two requirements into one. **The basis for the separation is solid; the basis for the exact
field list is not** — "carrier name, effective date, covered individuals" is a reasonable reading of
what a baseline must contain to be meaningful, not a quoted field list from any authority.

**Design choice.** A plan-year-scoped baseline record sits **above** the monthly rung, and **the first
reimbursement of each plan year is gated on baseline capture.** This is already the shape of the epic's
Story 6 (Baseline Claim Management) and Story 3 (the attestation page names the participant's baseline
carrier and plan) — so the design consequence is largely already designed, and this entry records *why*
rather than introducing it.

⚠️ **Unresolved: the dependent-data question.** Capturing "covered individuals" pulls **dependent
identity** into the data model. The pre-sale design census was deliberately kept minimal and
dependent-free (LA-11 / D21), and that minimalism is load-bearing — it is what keeps the sales stage
outside the BAA question. The post-sale `plus_participant` design does already contemplate
`dependents`, so this is not net-new to the model overall. **What is unresolved is how much dependent
detail a baseline actually needs.** Names? Count? Relationship? A dependent *count* plus the
participant's own carrier and effective date might satisfy the substantiation purpose at a fraction of
the data cost. Nobody has established the floor, and the design has been quietly assuming the ceiling.
**Resolve this before building the baseline table, not after** — narrowing a captured field later does
not un-capture it.

**Risk if wrong.** If a baseline is required and absent, every reimbursement in that plan year is
unsubstantiated notwithstanding a perfect monthly attestation record — the same failure as LA-01, with
a clean paper trail that documents it. If a baseline is *not* required, SSA has collected participant
carrier and dependent data it did not need, which is a data-minimization problem rather than a
compliance one.

**Reversal cost.** **Code change** — a table, a capture flow, and a gate on the first release of each
plan year. Cheap in isolation; the dependent-field decision inside it is the part that is not cheap
to unwind.

**Confirm before.** The **first reimbursement of any plan year** — which is the same trigger as
LA-01, so these two go to counsel together or not at all.

**Status.** Assumed — 2026-07-31.

---

### LA-03 — An attestation cannot be honored where SSA has actual knowledge it is false

**Assumption.** The reliance permitted in LA-01 is conditional. Where SSA holds information indicating
the attested coverage has lapsed, the attestation does not shield a release.

**This is an override, not a precedence rule.** D17's verification ladder ranks *sources*:
`ATTESTATION` → `CARD_TRANSACTION` → `HS_POLICY_STATUS`, best available wins. That structure is fine
for establishing coverage and **wrong for defeating it.** Any source indicating lapse defeats an
attestation stating otherwise, regardless of where that source sits in the ranking. A
`HS_POLICY_STATUS` of `terminated` beats a signed attestation not because policy status outranks
attestation, but because knowledge of falsity is not a ranked input at all.

**Basis.** The actual-knowledge limit is stated in the same authorities that permit reliance —
Notice 2017-67 for QSEHRA and 26 CFR §54.9802-4's substantiation provisions for ICHRA. **The
existence of the limit is well supported. Its scope is not.**

⚠️ **The open question: does ingesting a policy-status feed create a duty to act on it?**

"Actual knowledge" is a lower bar than "should have known" — but a system that *receives* daily policy
status and *does not look at it* is a poor fit for either characterization. The uncomfortable
possibility is that **receiving the data makes SSA responsible for catching lapses it would otherwise
have had no way to know about**, converting a passive shield into an active monitoring obligation.

**Nobody has researched this. It is not a matter of reading the substantiation regulation more
carefully — it is a question about what an administrator's knowledge means when the administrator is a
system.**

**This makes the HealthSherpa policy-status integration both more valuable and more binding.** More
valuable, because automated lapse detection is exactly the differentiator the capability map names.
More binding, because once the feed exists, the option of not knowing is gone — and it cannot be
un-ingested. Note the sequencing consequence: **B7 (Policy Status) is not a purely additive automation
win.** It changes SSA's knowledge posture. That should be a deliberate decision, not a side effect of
a carrier finally becoming available.

**Design choice.** The verification ladder needs an **explicit override path**, not just source
ranking: a lapse signal from any source blocks release for the affected months and routes to an
exception queue, independent of what the ladder would otherwise select. This is a design change to
D17 as currently written, and it is cheap **now** and expensive **after** B3 ships.

**Risk if wrong.** In the safe direction (an override exists where none was required): unnecessary
holds, participant friction, manual review load. In the unsafe direction (no override where one was
required): reimbursements released against known-lapsed coverage, which is the worst version of the
LA-01 failure because the record shows SSA held the contradicting data.

**Reversal cost.** **Code change** today — the ladder is designed, not built. **Rising with every feed
ingested**: each new data source that could indicate lapse widens the surface an override must cover,
and each is a decision that cannot be walked back.

**Confirm before.** **The first policy-status feed ingest** — B7, or any card-transaction feed that
could evidence non-payment. Not before B3, because attestation-only operation does not raise the
question.

**Status.** Assumed — 2026-07-31. The duty-to-act sub-question is **open with no basis.**

---

### LA-04 — Carrier names may appear in an employee-facing plan display

**Assumption.** Nothing in the ICHRA regulations or the ERISA safe harbor requires carrier anonymity.
The prohibition is on **selecting or endorsing**, not on **identifying**.

**Basis.** The ICHRA-specific ERISA safe harbor (29 CFR §2510.3-1(l), added by the 2019 final rules)
conditions non-ERISA status of the individual policies on the purchase being voluntary, the employer
not **selecting or endorsing** any particular issuer or coverage, the employer receiving no
consideration, and an annual notice. **The operative verbs are selection and endorsement.** A
complete, neutrally ordered list that names every carrier in the market does the opposite of selecting
one. Read the other way, the safe harbor becomes self-defeating: it requires a complete list, and a
complete list of insurance plans necessarily identifies who issues them.

This resolves the tension recorded as **O25** — where `domain_and_compliance_rules.md` §5's
endorsement boundary ("keep carrier names off all SSA-drafted paper") and the safe harbor's
completeness requirement were recorded as pulling in opposite directions. **They are not actually in
tension, because they govern different artifacts.** The endorsement boundary is about *SSA-drafted
paper* — plan documents, notices, the card, program marketing — where naming a carrier is a statement
by SSA. A market display is not a statement by SSA about which carrier to choose; it is a rendering of
what the market contains.

**Design choice — and it goes the other way.** **Carrier names stay off everything until a
demonstrable need exists.**

This is deliberate, and it is the standing principle doing its job. The shipped illustration works on
**counts and tiers alone** — `illustration25.jsp` renders `carrierCount` and never an issuer name
(verified in the JSP, 2026-07-31). Nothing currently built needs carrier identity, so there is no
reason to spend the option. If a later feature genuinely needs names — a provider check that must
report *which* plan covers a doctor, an employee-facing complete list — the research above says it is
available, and the entry gets revisited then.

**Risk if wrong.** Low in both directions, which is the point. If the assumption is wrong and names
were shown, the exposure is endorsement-flavoured and the fix is immediate. If the assumption is right
and names were withheld, the cost is a slightly less useful display.

**Reversal cost.** **Display edit — either direction.** Adding names to a display that computes over
plan objects already carrying `issuer` is trivial; removing them is equally trivial. **The symmetry
is why this entry does not need to be resolved before shipping anything.**

**Confirm before.** The first **employee-facing** display. Agent-facing displays are covered by
LA-09's narrower lane, and the current one names no carriers anyway.

**Status.** Assumed — 2026-07-31. Design deliberately declines to rely on it. Answers the substance of
**O25**.

---

### LA-05 — Any plan display AMS can build is definitionally incomplete

**Assumption.** The "not exhaustive" disclosure on any AMS plan display is **forced by the data**, not
chosen as a precaution.

**Basis — this one is verifiable in the repo, not researched.** D16 fixes launch at **off-exchange
only**, and `RateCacheWarmService.warmCounty` calls `quoteSingleApplicant(..., offExchange = true)`,
which sets `off_ex: true` on the request (verified 2026-07-31 in
`RateCacheWarmService.java:264-265` and `HealthSherpaService.java:208`). **On-exchange plans therefore
never appear in anything AMS shows.** In the reference county that is 45 on-exchange plans absent
against 65 off-exchange present. This is not a gap to be closed by pagination or a better query — it
is what the configuration means.

Two further sources of incompleteness, both structural: pagination silently truncates and `meta`
carries no total, so a short response is indistinguishable from a small market until you paginate to
exhaustion; and the `metal_levels` request filter omits "Expanded Bronze," a real returned value
covering 15 of 65 plans in the reference county.

**Design choice.** Every plan display carries a **completeness disclosure**, worded as a statement of
what the display *is* rather than a hedge about what it might miss. This is not a legal precaution
bolted onto an otherwise complete display — the display is genuinely partial and the disclosure is
accurate.

⚠️ **For QSEHRA the disclosure must go further.** A QSEHRA reimburses against **MEC from any source** —
a spouse's group plan, COBRA, Medicare, Medicaid, CHIP, TRICARE. **None of those appear in an
individual-market display, and none of them can.** A QSEHRA participant looking at an individual-market
plan list is seeing one of several qualifying paths, and the display gives no hint that the others
exist. So the QSEHRA disclosure must **name the qualifying coverage types**, not merely say the list is
partial. Saying "this list is not exhaustive" to someone whose spouse's plan already qualifies is
technically true and practically misleading.

Note the interaction with the ICHRA/QSEHRA asymmetry already recorded in `ichra_strategy.md` §7:
ICHRA *locks out* participants with other MEC, QSEHRA *requires* MEC from somewhere. The same display
therefore needs different disclosure text per product — this is not one paragraph reused.

**Risk if wrong.** There is no "wrong" direction on the completeness fact itself; it is observed
behaviour. The risk is in **omitting** the disclosure: a display that reads as the market and is not
is a misrepresentation, and for QSEHRA an employee could conclude no qualifying option exists when one
already does.

**Reversal cost.** **Display edit.** If launch later goes on-exchange (a D16 reversal, gated on
licensure SSA does not hold), the disclosure narrows rather than disappears.

**Confirm before.** The first **employee-facing** display. Agent-facing displays should carry it
anyway, because an agent repeating an incomplete list to a client inherits the problem.

**Status.** Assumed — 2026-07-31. The completeness *fact* is **Confirmed** against the code; only the
disclosure adequacy is assumed.

⚠️ **One thing this entry does not yet apply to: there is no plan display today.** The illustration
renders aggregates — counts, LCSP, benchmark silver, lowest bronze — not a plan list, and the cache is
empty on every installation. This entry is **prospective**, and it is recorded now so the disclosure
is designed in rather than retrofitted onto the first display that ships.

---

### LA-06 — No employer-driven ordering in any plan display

**Assumption.** Any ordering, promotion or default that traces back to the employer risks being
characterized as selection or endorsement, defeating the ERISA safe harbor.

**Basis.** The same safe-harbor conditions as LA-04 (29 CFR §2510.3-1(l)) — with the emphasis on the
other verb. LA-04 establishes that *naming* is not endorsing. This entry is the boundary on the far
side: **ordering can be endorsing.** A "recommended" badge, a featured position, a pre-selected
default or a filter narrowed on entry all communicate a choice, and the safe harbor's protection turns
on the employer not making one. The reasoning is straightforward; what is assumed is where exactly the
line sits between *presenting* and *steering*.

**Design choice — the concrete prohibitions.**

- No featured plans, no promoted listings.
- No "recommended" anything.
- No default carrier.
- No pre-selected narrowing filters — a display opens showing everything, and the employee narrows it.
- Employee-**initiated** sorting on objective criteria (premium, deductible, metal level) is
  acceptable, because the choice of ordering is the employee's.

**And the one that is easy to get wrong: agent-side preference must never propagate into an
employee-facing default view.** An agent legitimately has preferences — carriers they know, plans they
have placed before — and agent-facing tools may legitimately reflect them. The moment any of that
becomes the default ordering an employee sees, an agent preference has become, in effect, an
employer-side selection. **This is a data-flow rule, not a UI rule**, and it is the kind of thing that
arrives by accident: a shared component, a saved view, a "sensible default" inherited from the agent
context. Guard it at the boundary, the same way base/markup is guarded out of public proposal HTML
(`domain_and_compliance_rules.md` §4).

**Risk if wrong.** In the conservative direction (no ordering where some was permitted): a less useful
display. In the other direction: the individual policies lose safe-harbor protection and become ERISA
plans, which is a plan-qualification problem for the employer and a design problem SSA authored.

**Reversal cost.** **Config change to code change**, depending on where the rule is enforced. If
ordering neutrality is a property of one display component it is cheap; if agent preference has been
allowed to flow through shared plumbing, unpicking it is a code change with real surface area. **This
is an argument for enforcing it at a single boundary from the start** — cheap now, not cheap later.

**Confirm before.** The first **employee-facing** display. Agent-facing tools may order freely, which
is precisely why the propagation boundary has to exist before there is anything to propagate.

**Status.** Assumed — 2026-07-31. Formalizes the presentation rules already recorded as settled in
`ichra_strategy.md` §7 and `domain_and_compliance_rules.md` §5.

---

### LA-07 — QSEHRA notice runway is 45 days for a non-January effective date

**Assumption.** A QSEHRA can be stood up on roughly a **45-day** practical runway for a non-January
effective date, because the statutory notice deadline for a short first plan year lands on or before
the effective date rather than 90 days prior.

**Basis.** The QSEHRA notice requirement (IRC §9831(d)(4)) requires written notice to each eligible
employee **not later than 90 days before the beginning of the year** — with the deadline for an
employee who was not eligible at the beginning of the year running to **the date that employee becomes
eligible**. **The legal floor therefore depends on plan-document drafting, not on the calendar:**

- **A calendar-year plan with a short first year.** The plan year begins January 1. No employee was
  eligible on January 1 — the arrangement did not exist. The notice deadline for each employee is
  therefore the date they become eligible, i.e. **on or before the effective date.**
- **A plan year beginning on the effective date.** The plan year begins when coverage begins, and
  the 90-day-before-the-beginning-of-the-year rule bites directly. **90 days.**

**Year two onward is always 90 days** under either drafting, so this lever fires exactly once per
employer.

⚠️ **The basis for the 90-day/eligibility-date structure is solid; the basis for "45 days" is not
legal at all.** 45 days is the *practical* floor, and **the binding constraint is getting individual
coverage in force** — application, underwriting-free but not instant, effective-date rules, first
premium paid — not the notice rule. The notice analysis removes notice as the binding constraint; it
does not make 45 days achievable by itself. Do not present 45 days as a legal conclusion. It is an
operational estimate that the notice rule happens not to contradict.

**Design choice.** **Notice due date becomes computable** — from effective date, plan-year structure,
and per-employee eligibility timing. That is a bigger consequence than it sounds: it upgrades the
notice obligation register (B4a) from a **log** of notices that were sent into a **worklist** of
notices that are owed and when. B4a is already recorded as gating on nothing and as *"the shortest
path to fixing a problem that exists today, for existing clients"* — a computed due date is what makes
its output actionable rather than retrospective.

⚠️ **Two hard rules, both of which exist because the tempting shortcut is obvious.**

1. **Never backdate an effective date to cure a late notice.** A late notice is a
   $50-per-employee-per-failure problem (IRC §6652(o), capped annually). A backdated effective date
   on a plan document is a different category of problem entirely, and it is the kind that is
   discovered later by someone who is not on your side. Take the penalty.
2. **January 1 is structurally the hardest first-year date, not the easiest.** It reads as the natural
   start and it is the one date where the short-first-year drafting lever cannot fire — a calendar-year
   plan effective January 1 has no short first year, so the full 90 days applies. **An October
   conversation about a January 1 start is already late.** This inverts the intuition every sales
   conversation brings to it, which is exactly why it belongs in a register rather than in someone's
   head.

**Risk if wrong.** A missed notice is a per-employee penalty and, more importantly, an argument that
the arrangement was not properly established. If the short-first-year reading is wrong, every
non-January QSEHRA sold on a 45-day runway has a defective notice.

**Reversal cost.** **Config change** — the due-date computation is date arithmetic over plan-year
structure and eligibility date. Being wrong about the rule means recomputing due dates and, for
anything already sold, a late-notice remediation. Cheap in code, not free in the field.

**Confirm before.** The **first non-January QSEHRA effective date sold on a short runway.** This is
the entry most likely to be needed soonest, because it is the one a sales conversation can reach
without any of the deferred build landing.

**Status.** Assumed — 2026-07-31. Answers the QSEHRA half of **O17**. See LA-08 for the ICHRA half.

---

### LA-08 — ICHRA notice requirements are NOT known to follow the QSEHRA analysis

**This is a gap, not an assumption. It is numbered so it cannot be skipped.**

**Assumption.** None. **The research that produced LA-07 covered QSEHRA only.**

**ICHRA is the product.** LA-07 is a well-worked answer to a question about the adjacent arrangement,
and the temptation to carry it across is strong precisely because the two look similar and the
90-day figure appears in both. **They are different regimes with different notice provisions.**

**What is specifically unknown:**

1. **Whether the short-first-year drafting lever exists for ICHRA at all.** LA-07's mechanism depends
   on the QSEHRA statute's eligibility-date fallback. Whether the ICHRA regulations contain an
   analogous provision — and whether it is triggered by the same plan-document drafting — has not been
   researched.
2. **Notice *content*.** The ICHRA notice content requirements are understood to be **substantially
   heavier** than QSEHRA's — the ICHRA notice must address opt-out and waiver rights, the effect on
   premium tax credit eligibility, and the ERISA safe-harbor statement, among others. A heavier notice
   is a longer lead time in practice even where the legal deadline is identical.

⚠️ **One repo finding that bears directly on this.** `plus_tier_build_plan.md`'s **O17** already
asserts an ICHRA answer: *"For a new ICHRA the notice is due by the date coverage begins rather than
90 days prior."* **That assertion carries no citation anywhere in the doc set**, and it is the only
place the ICHRA newly-established-plan rule appears. If it is right, it partly answers item 1 above.
**It has not been verified and should not be relied on** — it is recorded here so that whoever does
the ICHRA research knows there is an existing claim to check rather than starting from zero.

**Design choice.** **None yet, and that is the point.** No ICHRA sale should be quoted on a short
runway until this is researched. The QSEHRA runway analysis does not transfer, and treating it as
though it does is the single most likely way this register causes harm rather than preventing it.

**Risk if wrong.** Unknown, and unknowable until researched — which is worse than a known risk, not
better.

**Reversal cost.** **Cannot be priced.** If the lever does not exist, ICHRA effective dates carry a
90-day floor permanently, which is a **sales-process** constraint rather than a code one and cannot be
reversed by any change to AMS.

**Confirm before.** **Any ICHRA sale with a short runway.** This is research, not necessarily counsel
— reading 26 CFR §54.9802-4(c)(6) and the preamble discussion at 84 Fed. Reg. 28888 would likely
resolve item 1 at zero cost. **It should be done before the counsel package is priced**, so that
counsel is asked the residual question rather than the whole one.

**Status.** **Open — no basis.** 2026-07-31.

---

### LA-09 — Agent-facing market reference data is not producer activity

**Assumption.** SSA displaying market reference data to a **licensed agent**, with **no enrollment
path originating in AMS**, does not constitute acting as an insurance producer.

**Basis.** SSA receives an **administration fee**, not commission; makes **no recommendation**; and
any transaction occurs at a **CMS-approved EDE** that is the licensed party. The AOR finding supports
the separation structurally: agent of record travels **per application, keyed on NPN**, so SSA is not
in the compensation chain for the policy and is not competing with the agency's agents for it.

⚠️ **This is a genuine assumption on thin evidence, and it should not be read as comfortably as the
others.** Three specific reasons:

1. **ERISA does not preempt state producer licensing.** ERISA §514(b)(2)(A) preserves state regulation
   of insurance, so no amount of federal-plan framing answers a state licensing question. This is a
   **per-state** question by construction.
2. **Displaying named plans with premiums, alongside an enrollment pathway, can constitute soliciting
   insurance** under state producer statutes. Several states define "solicit" broadly enough that
   presenting plan-specific pricing to a prospective purchaser is captured, regardless of who is paid.
3. **The "no recommendation" defense is thinner than it feels.** It rests on SSA's own
   characterization of its output. An affordability threshold that identifies which employees come out
   ahead is analytically neutral and practically decisive.

**Design choice.** **No enrollment path originates in AMS. Agent-facing only.** The illustration is
authenticated and agent-facing (D22), and the enrollment rail — when it exists — hands off to
HealthSherpa, which holds the licensure. This is not an incidental architecture; it is the entire
basis for the assumption holding.

⚠️ **A live pressure on this entry that is already in the plan.** D22 records that the existing
per-agency public quote token (V071) *"becomes an optional prospect-facing front door in a later
increment."* **A prospect is an employer, not a licensed agent.** The moment market data renders for a
prospect, LA-09's central fact — the audience is a licensed agent — stops being true, and the entry
does not cover it. That increment should be treated as **crossing the scope line**, not as widening an
existing feature.

> **Narrowed by LA-17 (2026-08-02) — one case only, and not this one.** LA-17 carves out the
> **agent-composed, agent-sent, agency-branded proposal**: market data reaches an employer, but a
> licensed agent chose the recipient, chose the content, and pressed send. **The agentless front door
> described immediately above is expressly *not* narrowed** — LA-17's constraint 2 is that V071's
> public quote token must not become a prospect-facing entry point, and LA-17 collapses if it does.
> Nothing in LA-09 is superseded or withdrawn.

⚠️ **Open sub-question, unresearched: does operating as the licensed agency's tool, under the agency's
license, change the analysis?** If AMS is white-labelled to SWBD and an SWBD-licensed agent is the
operator, one reading is that the licensed party is doing the displaying and SSA is supplying
software. Another is that SSA is doing it and the branding is cosmetic. **This is potentially the
cheapest way to make the whole question go away**, and nobody has looked at it. It is worth an hour of
research before it is worth an opinion.

**Risk if wrong.** Unlicensed producer activity — state-by-state, with penalties and, more damagingly,
a characterization problem that follows the business. This is one of two entries (with LA-10) whose
downside is a **licensure** problem rather than a code problem.

**Reversal cost.** **Code change** to withdraw a display or re-gate an audience — genuinely cheap
while agent-facing-only holds. **Escalates to rebuild** the moment a prospect-facing or
employee-facing surface exists, because the reversal is then a removal of shipped functionality that
someone was sold on.

**Confirm before.** **Any non-agent audience** — prospect-facing, employer-facing or employee-facing —
and **before multi-state operation.** Texas-only agent-facing is the narrowest posture available and
the current one.

**Status.** Assumed — **thin** — 2026-07-31.

---

### LA-10 — Texas TPA licensing (ch. 4151) is unresolved and may already apply to the existing book

**Assumption.** None favourable. **This entry records exposure, not a resolution.**

**Basis — statutory text, read partially.** Tex. Ins. Code **§4151.001(1)** defines an "administrator"
**disjunctively**. A person is an administrator who, in connection with coverage, either:

- **collects premiums or contributions *from* residents of this state**, **or**
- **adjusts or settles claims *for* residents of this state.**

**SSA never holds participant funds, which defeats the first prong only.** The doc set has been
carrying that fact as if it were the answer — `swbd_premiumpath.md` notes correctly that no fund
custody *"removes the money-transmitter / premium-collection angle"* while also noting that the ch. 4151
question *"attaches to the act of administering, not to fund custody."* **Both are in the same
paragraph, and only the reassuring half has been operationally load-bearing.**

**On the second prong, the posture is weak.** SSA performs **initial adjudication** with the employer
as final decision-maker. That is the standard TPA arrangement — it is what nearly every TPA does — and
**it is unlikely to defeat a statute written around adjusting or settling claims.** "The employer
signs off" is a common structure, not an exemption.

⚠️ **§4151.0021 (Processing Agents) and §4151.0022 (Nonapplicability) have not been read.** These are
the **most likely relevant exemptions** and the highest-value hour of unpaid work available on this
entry. It is entirely possible that one of them disposes of the question. Nobody has looked.

⚠️ **Critically: the statute keys on *participant residence*, not on SSA's location.** Two
consequences the doc set has not absorbed:

1. **This is a per-state question wherever SSA has participants** — not a Texas question that becomes
   a multi-state question later. Given SWBD's multi-state footprint it is already plural.
2. **It applies to the existing FSA / HRA / HSA / COBRA book, not just to ICHRA.** The doc set files
   this under the ICHRA counsel docket (O21, *"parked, pre-scale"*), which frames it as a future
   problem attached to a future product. **On the statute's own terms it attaches to what SSA does
   today.** ICHRA does not create this exposure; it only increases the participant count and the state
   count.

**Design choice.** None available — **this is not a design question.** No architecture makes it go
away, which is precisely why it is easy to leave parked: there is nothing to build, so it never
surfaces as a blocker in a build plan.

**Risk if wrong.** Operating as an unlicensed administrator, per state, across the existing book.
Remediation is a certificate-of-authority application, not a code change.

**Reversal cost.** **Not code at all — potentially licensure.** The cost of being wrong does not scale
with what has been built; it scales with **how many participants in how many states have been
administered in the meantime.** That makes delay itself the expensive variable, which inverts the
usual "park it until it blocks something" instinct.

**Note: a TDI determination letter may be substantially cheaper than a legal opinion.** Asking the
regulator whether a described arrangement requires a certificate is a recognized path and is worth
pricing before the counsel engagement is scoped. It also produces something more useful than an
opinion — a regulator's own position.

**Confirm before.** **Scaling past a pilot**, and **before entering each new state**. The parked
disposition is defensible for demo-stage work and indefensible past it.

**Status.** **Open — material — not blocking demo-stage work.** 2026-07-31. Escalates
`swbd_premiumpath.md`'s *"parked, revisit pre-scale"* and O21's *"parked, pre-scale"* by noting that
the residence key means the clock is already running on the existing book.

---

### LA-11 — Design census stays at minimum scope

**Assumption.** A pre-sale design census limited to **age, ZIP, family tier and income band** carries
no individually identifiable health information and therefore sits **entirely outside the BAA
question**.

**Basis.** D21 establishes the pre-sale design census as a distinct object from the post-sale
administrative census, with **no SSN**. The reasoning is that none of the four fields identifies an
individual, and none is health information — they are rating inputs. **The confidence here comes from
the fields being obviously insufficient to identify anyone, not from a de-identification analysis
having been performed.** Nobody has run the fields against a de-identification standard, and small-group
re-identification is a real phenomenon: age plus ZIP plus family tier in a fourteen-person employer is
not as anonymous as it looks in the abstract.

**Design choice.** The design census carries **age, ZIP, family tier, income band. No SSN, no names,
no dependents.**

⚠️ **Note that "no names, no dependents" is stricter than what D21 literally says.** D21 specifies the
four fields and *"no SSN"*; the exclusion of names and dependent detail is this register's reading of
minimum scope, applied consistently with LA-02's unresolved dependent-data question. **It is recorded
as a design choice rather than as a restatement of D21**, so that a later reader does not find the
tighter rule in D21 and fail to find it.

**This is why A3 can ship years before enrollment-stage PHI questions resolve.** The BAA with
HealthSherpa (O13) has no recorded response and has been open since 2026-07-28. Nothing in the
build-now scope depends on it, and that is not luck — it is D21 working. **The corollary is that the
minimalism is load-bearing and every field added to the design census spends it.** There is no such
thing as adding "just DOB."

⚠️ **Unresolved: whose data is it?** When an agent uploads information about a **third party's**
employees — the agent's client's staff, not the agent's own — SSA holds data about people who have no
relationship with SSA, did not consent, and mostly do not know AMS exists. The authority chain runs
agent → employer → employee and **nobody has established that it is intact.** Data-minimization makes
this survivable rather than resolved: it is much easier to defend holding an age and a ZIP under an
unclear authority than a name and an SSN. **The question is not answered; it is made cheap.** That is
the standing principle applied to a question nobody has asked yet, and it is the best argument for
keeping the census minimal even if the BAA lands tomorrow.

**Risk if wrong.** Low at the current scope by construction. Rises non-linearly with each field
added — the first identifying field converts a rating-input file into a personal-data file, and the
first health field converts it again.

**Reversal cost.** **Config change** to narrow the census. **Effectively irreversible for data already
collected** — narrowing the schema does not unsee what was uploaded, and the deletion path for
already-received census files is not designed.

**Confirm before.** **Any field added to the design census.** Not "before A3 ships" — A3 at the
current scope is the safe case. The trigger is the change, and it should be a deliberate decision with
this entry in front of the person making it.

**Status.** Assumed — 2026-07-31.

---

### LA-12 — Affordability is computed for the employer, not presented to the employee

**Assumption.** Computing an affordability determination **for a plan sponsor** is administration — a
TPA function, squarely in SSA's lane. Presenting that determination **to an employee**, whose premium
tax credit eligibility turns on it, may be advice.

**Basis.** The distinction is one of audience and reliance rather than of computation — the arithmetic
is identical either way. An employer needs the affordability result to structure a compliant offer;
that is what an administrator is for. An employee reading the same number is reading a statement about
**their own** subsidy eligibility, and acting on it means declining or accepting a credit. The line
between administration and advice is drawn here by reasoning rather than by any authority anyone has
read. **The reasoning is sound and the citation is absent.** It is consistent with the settled rule
already in the doc set — plan-selection guidance routes to the licensed agent — but nothing states the
affordability case specifically.

**Design choice.** Affordability output is **employer- and agent-facing**. Employee-facing
affordability figures are on the defer side of the scope line.

⚠️ **T44 is a correctness dependency, and it matters more than the audience question.**

`rating_area_rate_cache` currently derives `lcsp_premium` and `benchmark_silver_premium` from
**off-exchange** silver plans, because the warm job quotes with `off_ex: true` (LA-05, verified in
code). **ICHRA affordability keys on the lowest-cost silver plan offered *on the Exchange* for the
employee's rating area** (26 CFR §1.36B-2(c)(5) and IRC §36B(c)(4)(C) — cite unverified, see the
citation caveat). The two plan sets differ materially: 45 on-exchange against 65 off-exchange in the
reference county.

**The dangerous direction is understating the LCSP.** An off-exchange-only silver plan that undercuts
the true on-exchange LCSP produces a lower affordability threshold, which makes an **unaffordable
offer look affordable**. The employer then makes an offer believing it clears; the employee loses
subsidy eligibility they were entitled to; and the determination that told everyone it was fine came
from SSA.

**An assumption about *who sees* a determination does not help if the determination is wrong.** LA-12
is about audience; T44 is about correctness; **correctness is the one that hurts.** T44's own entry
splits into an empirical half (two staging calls differing only in `off_ex` — blocked today because no
installation has a key) and a compliance half (whether an indicative illustration figure is held to the
affordability standard at all), and correctly files the compliance half alongside the counsel package.

**Risk if wrong.** On the audience assumption: unlicensed advice to an employee about their own tax
credit. On the T44 dependency: systematically wrong affordability determinations, delivered
confidently, to every employer using the feature.

**Reversal cost.** **Display edit** for the audience question — withdrawing an employee-facing figure
is trivial. **Code change** for T44, plus a cache re-warm. **But every determination already issued on
a wrong LCSP is irreversible** in the sense that matters: the offer was made, the employee acted, the
plan year ran.

**Confirm before.** Any **employee-facing** affordability figure. **And, independently of counsel, fix
T44 before any affordability output is shown to anyone at all** — including the agent. The audience
gate and the correctness fix are separate obligations and the correctness one comes first.

**Status.** Assumed — 2026-07-31. T44's compliance half belongs in the counsel package with LA-01,
LA-04 and LA-07.

---

### LA-13 — This register is internal work product and is not disclosed to partner agencies

**Assumption.** SSA's internal record of unresolved compliance questions — this document — is work
product for SSA and its eventual counsel. It is **not disclosed to partner agencies through SSA-built
tools.** An agent asking a question SSA has not settled gets *"SSA has not finalized a position on
this. I can't give you an answer; take it to SSA directly."* They do not get the register entry, they
do not get its status, they do not get an LA number, and they do not get any characterization of SSA's
own legal or regulatory exposure.

**Basis — and this one is a judgment call, not a researched legal conclusion.** No statute or
regulation was read for this entry and none is being relied on. The reasoning is about audience. The
ICHRA design advisor's users are licensed agents at a **partner agency** — SWBD — not SSA staff. As
first built (V080, item 12), the advisor would have told that audience, by default and unprompted,
that whether Tex. Ins. Code ch. 4151 requires a certificate of authority for what SSA does today is
unresolved and may already apply, that SSA's substantiation approach is unreviewed, and that one of
its positions is irreversible once a dollar has moved. **Telling a partner agency that SSA may be
operating without a required certificate of authority is a business and legal disclosure.** It may
eventually be the right thing to say — but it is a decision for Kevin to make deliberately, in a
channel he chooses, not one made by default inside a chatbot's system prompt.

⚠️ **The prompt as built also contradicted itself**, which is what surfaced this. Boundary 5 ordered
*name the LA number when you rely on one* and, in the same paragraph, *never cite the assumptions
register as authority to anyone outside SSA*. The audience is outside SSA. Both instructions could not
hold, and the failure mode of that contradiction is disclosure, not silence.

**Design choice.** Register identifiers and exposure characterizations are stripped from **all
agent-facing output** — the skill's `system_prompt` and every `knowledge_chunk` title, section and
body. What is deliberately **retained in full**:

- **The settled-versus-unfinalized distinction.** Deleting it would make the advisor present
  unreviewed positions as settled law, which is worse than the disclosure it fixes. The advisor still
  separates *"this is SSA's rule"* from *"SSA has not finalized this"* — it just stops naming the
  register while doing so.
- **Every refusal boundary, at full force** — plan selection, licensure, ICHRA notice timing,
  per-person affordability, and the "no written rule → say so and stop" fallback.
- **The LA-07 QSEHRA runway qualifications** — operational-not-legal, January 1 is the hardest first
  year, never backdate. Those are operational cautions the agent needs in order to sell competently,
  not disclosures about SSA, so they are stated as SSA's current operating guidance rather than as an
  unconfirmed assumption. The ICHRA scope fence on that content is unchanged.
- **Licensing questions route to SSA** with *no* characterization in either direction — not "resolved",
  not "open", and no reassurance.

`source_citation` on each chunk still carries the LA references. That field is internal: it is never
copied onto the in-memory chunk (`KnowledgeSearchService.toChunks`) and never emitted into a prompt
(`buildContext`), so it reaches only the Knowledge Manager admin UI. It is the provenance trail for
Kevin and for counsel, and it stays.

**Risk if wrong.** An agent receives less context about how firm a position is than full candour would
give, and could over-rely on an answer. **Mitigated structurally:** where a position is unfinalized the
advisor does not answer at all — it declines and routes to SSA. The agent never receives a soft answer
they might mistake for a firm one; they receive no answer and a person to ask. The residual risk is an
agent who does not follow up.

**Reversal cost.** ⭐ **Low, and asymmetric — this is the entry the standing principle most obviously
governs.** Restoring disclosure is a data edit to V080's prompt and chunks. **A disclosure already made
to a partner agency cannot be withdrawn** — it has been read, and it may have been repeated. When one
direction is a text edit and the other is unrecoverable, the default is to withhold and decide later.

**Confirm before.** Any decision to disclose SSA's regulatory status, or the review state of its
compliance positions, to a partner agency **in any channel** — not only this tool. The trigger is the
disclosure, not the feature.

**Status.** Assumed — 2026-07-31. Applied to V080 the same day, before it was committed or deployed.

---

### LA-14 — `FPL_ANNUAL_2026` holds the employer safe-harbor poverty line, not the PTC one

**Assumption.** The federal poverty guideline stored under `FPL_ANNUAL_2026` is the figure the
**employer-side ICHRA affordability safe harbor** uses — the **2026** table, $15,960 for a one-person
household in the 48 contiguous states and DC. It is **not** the figure the premium-tax-credit
computation uses for 2026 coverage, which is the **2025** table, $15,650.

**Basis — unusually strong for this register, and the weakness is elsewhere.** Two things are cited,
not reasoned: the applicable percentage of **9.96%** for plan years beginning in 2026 (IRS Rev. Proc.
2025-25, 2025-07-18 — up from 9.02% in 2025, the highest it has been), and the **2026 HHS poverty
guidelines** effective January 2026. The safe harbor permits an employer to use the guideline in effect
**within six months before the first day of the plan year**, which for the reference case (Sandoval,
9/1/26 effective) makes the 2026 table available and correct. The divergent figure comes from
26 CFR §1.36B-1(h), which fixes the poverty line for PTC purposes at the one in effect on the first day
of the open enrollment period preceding the taxable year — the 2025 table for 2026 coverage.

**So this entry is not about sourcing. It is about naming.** Both figures are published, both are
correct for their own computation, and the constant's name — `FPL_ANNUAL_2026` — records the *year* and
not the *computation*. A future reader adding a PTC feature would find a plausibly-named constant
holding the wrong number for their purpose.

**Design choice.** Seeded as the safe-harbor figure because the safe harbor is the only thing that reads
it: `IllustrationServlet.computeAffordability`'s `"FPL"` branch passes it as the reference income into
`AffordabilityCalculator.flipContribution`, which answers *"at what employer contribution does this
offer become affordable"* — an employer-side question. **AMS computes no premium-tax-credit dollar
figure anywhere, deliberately** (LA-12, and the item-9 boundary that PTC eligibility is shown only as
kept-or-lost, never as an amount). The $15,650 figure has no reader in this codebase and was not seeded.

⚠️ **A second naming assumption, recorded and deliberately not fixed.** Putting the plan year inside the
constant's *name* means 2027 requires a new constant **and a new reader** — `IllustrationServlet` builds
the lookup key by string concatenation on the selected plan year, so a 2027 illustration silently finds
nothing and reports itself unconfigured. That is the fail-closed direction and it is not a defect this
run is fixing; it is a design question for whoever owns the constants convention.

**Risk if wrong.** If the two figures were transposed, every FPL-basis flip contribution would be
computed from an income $310/yr too low — shifting each threshold by about **$2.57/month** in the
direction that makes an offer look *less* affordable than it is. That is the **safe** direction (LA-12's
dangerous direction is understatement of the threshold), and the magnitude is inside the "estimate, not
an exact figure" caveat the page already carries. The real risk is the naming one: a later feature
reading this constant for a PTC purpose and getting a silently wrong answer.

**Reversal cost.** ⭐ **Trivial — a one-row `UPDATE`, or a two-line edit to
`DatabaseInitializer.addIchraAffordabilityConstants`.** Nothing derived from these values is persisted:
no affordability figure is written to `illustration_log`, to `proposal_ichra_snapshot` (which is
structurally incapable of carrying one), or to any other row. Correcting the constant corrects every
future computation immediately and there is no back-catalogue to restate. **This is among the cheapest
entries in the register to be wrong about** — which is precisely why it should be checked before the
values get copied somewhere that does persist.

**Confirm before.** (1) Any feature that computes a **premium tax credit amount** — that reader needs
the 2025 table and must not reuse this constant. (2) Adding plan year **2027**, which needs both a new
constant pair and a check that the applicable percentage and guideline have been re-published. (3) Any
employer whose plan year begins such that the six-month lookback makes a different guideline table the
correct one.

**Status.** Assumed — 2026-08-01. Seeded in `DatabaseInitializer` the same day. ⚠️ **The seed does not
reach an already-initialized installation**, so production remains unverified until the click-script's
step 10 is run (T65).

---

### LA-15 — A subsidy-preserving contribution ceiling is a different object from an affordability threshold

**Assumption.** Reporting *"at $450 this employee's coverage becomes affordable and they lose PTC
eligibility"* is analysis. Presenting *"stay under $412 to keep them eligible"* is **a recommended
contribution**, and recommending a contribution is closer to advice than reporting a threshold — even
though the arithmetic is identical and the second number is just the first one restated.

**Basis — reasoning, not authority, and the reasoning is about framing rather than computation.**
Surfaced 2026-08-01 from Kevin's account of how a prospect case actually runs: an agent may pick a
contribution *because* he infers many of the employees are subsidy-eligible, and in that framing the
affordability threshold **inverts into a budget ceiling.** The same `AffordabilityCalculator.flipContribution`
output serves both. What changes is the verb: today the page *warns* about crossing a line; the ceiling
framing *tells the employer where to land*. That crosses toward the no-steering boundary the doc set
draws everywhere else — no curation, no "recommended", no default selection (`ichra_administration_scope.md`
ERISA safe-harbor posture, LA-04, and D24's agent-only rule). **No authority was read for this entry.**

⚠️ **It also inherits LA-12's dangerous direction.** A ceiling presented as a target is acted on more
directly than a threshold presented as a warning — so if the underlying LCSP is understated (**T44**,
still unresolved), a ceiling is wrong in a way that immediately shapes an employer's offer.

**Design choice — and this run deliberately makes none.** Current wording is unchanged and stays a
threshold report. The tension is recorded so that whoever builds the prospect-case framing sees it
before writing the label, rather than discovering it in review. **Nothing in the code moves on this
entry today.**

**Risk if wrong.** In the permissive direction: SSA effectively recommends a contribution level to an
employer, which is a design recommendation with tax consequences for employees, made by an entity
holding no licensure. In the restrictive direction: SSA withholds the single most useful number in an
ICHRA design from the person who needs it, and a competitor supplies it.

**Reversal cost.** ⭐ **Display edit, both directions, and nothing is persisted** — no affordability
figure is written to `illustration_log`, and `proposal_ichra_snapshot` (V079) is structurally incapable
of carrying one. There is no back-catalogue to restate. The asymmetry is reputational rather than
technical: a ceiling once presented to an employer as a target has been acted on.

**Confirm before.** Any label, tooltip, headline or proposal section that presents a contribution figure
as a **target, ceiling, recommendation, or "optimal"** rather than as a threshold being crossed. The
trigger is the framing, not the feature.

**Status.** Assumed — 2026-08-01. Source: Kevin's walkthrough, not a document. No code changed.

---

### LA-16 — Employer-entered data arriving through an unauthenticated proposal link

**Assumption.** An employer improving a census, correcting an age, or entering their current group
premium through a **public proposal link** is *data collected from a real person arriving by a side
door* — and it is subject to the same obligations as data collected through the front door, despite the
link's convenience framing.

**Basis.** No authority read. The reasoning is structural: `/proposal/*` is public and unauthenticated
(the constraint LA-12 already leans on to keep affordability off that surface), so anything the page
accepts is accepted from an unauthenticated party with no identity assertion and no agreement to any
terms. The existing public surfaces — `/q/*`, `/proposal/`, `/apply/` — either collect nothing or sit
behind an application the person deliberately started. A proposal that quietly gains input fields is a
different thing from a proposal that is read, and T30/T31's lessons about public form surfaces (rate
limiting, abuse) attach the moment it does.

**Design choice — deferred in practice, and that is the point of registering it now.** The
2026-08-01 decision is that **T81's interactive employer proposal ships as a sandbox: the employer
corrects estimates, enters current rates and an expected increase, figures update live, and nothing is
written.** No row, no PII, no authentication needed, because nothing is collected. That is build rule
3's worked example — render everything, store nothing — and it defers this entire question. **This entry
exists because T81's obvious next phase, returning those corrections to the agent, walks straight into
it**, and the value of that phase is exactly what makes it tempting to add without deciding.

**Risk if wrong.** Collecting employee ages and employer financials from an unauthenticated party
without notice, retention rules, or a lawful basis — and, if the census is ever improved to real
identities rather than age bands, doing so on a surface that has none of the protections the
authenticated census path (**D7**, **D21**) was designed around. D21 exists specifically to keep the
sales stage outside the BAA question; a public write path is how that boundary gets crossed by accident.

**Reversal cost.** ⭐ **While the sandbox holds: zero — there is nothing to reverse, because nothing is
stored.** Once a write path exists it inverts sharply: collected data must be located, retained per some
rule nobody has written, and deleted on request, and any of it already forwarded to an agent cannot be
recalled. **The cheap moment to decide is before the first row is written, which is now.**

**Confirm before.** Any field on a public proposal page whose value is **sent to the server** — as
opposed to consumed by client-side script and discarded. That is the exact line the sandbox decision
draws, and it is the only line that needs watching.

**Status.** Assumed — 2026-08-01. Deferred by the sandbox decision; registered against T81's next phase.

---

### LA-17 — An agent-composed, agent-sent, agency-branded proposal may carry market data to an employer

**This entry narrows LA-09. It does not replace it.** LA-09 holds everywhere else, including — and
especially — the agentless public front door it already names as crossing the scope line.

**Assumption.** A proposal that is **composed by a licensed agent**, **sent by that agent**, and
**branded to that agent's agency** may carry market data to an employer — premium ranges, plan and
carrier counts, affordability output, a contribution slider — without SSA thereby engaging in producer
activity.

**Basis.** **The distinguishing fact is who initiated the delivery and who chose the content, not what
the document contains.** The agent selects the recipient, composes the document, and presses send. SSA
supplies document-production software to a licensed producer. On that reading, the market data in the
document is the agent's statement to their own client, rendered by a tool, and SSA's role is the
tool's.

This is also **the observable operating model of existing broker platforms** — Zywave, Employee
Navigator, Ease — which render carrier and rate data into employer-facing, broker-produced proposals
and are not themselves licensed producers. ⚠️ **State this as an industry-pattern observation, not as
legal authority.** That an entire software category operates this way without apparent enforcement is
evidence about market practice and about what regulators have not pursued. It is not a holding, it is
not a safe harbor, and nobody has checked whether those platforms hold licenses for reasons unrelated
to this question.

**Design choice — three constraints, all load-bearing.** The assumption is not that the document is
harmless. It is that these three facts are true of it, and **the entry fails if any one of them stops
being true.**

1. **Every send is an agent action, logged with the agent's identity.** **SSA never initiates a
   communication to a prospect**, and no SSA-initiated channel to a prospect exists. This is the fact
   the whole entry rests on — if a proposal can leave the system without an agent having pressed send,
   the "who initiated the delivery" basis is gone.
2. **No agentless public front door.** V071's per-agency public quote token **must not become a
   prospect-facing entry point.** This is the exact increment LA-09 names as crossing the scope line,
   and **it remains uncrossed** — confirmed by Kevin, 2026-08-02. A prospect who arrives at market data
   without an agent having sent it to them is not covered by this entry under any reading.
3. **Two voices, structurally separate in the document.** **The agent's section carries market data.
   SSA's supplemental section describes administration services only** — no plans, no carriers, no
   selection guidance. Keep them **visually and structurally distinct**, so that the document itself
   shows who said what. This is not cosmetic: the basis above turns on the market content being the
   agent's statement, and a document that blends the two voices is evidence against the very fact it
   depends on.

#### The enforcement mechanism — proposal-scoped entitlement gating

**Kevin's design rule, confirmed 2026-08-02.** ICHRA/QSEHRA-specific plus-tier proposal content
appears **only when the proposal is tied to an agent of an agency carrying the ICHRA entitlement flag**
(`agency.ichra_enabled`, V077). **An employer coming to SSA directly, with no entitled agency behind
them, receives administration content only — no plus-tier items.**

**Recorded here rather than as its own LA number, because it is not an independent assumption.** It
reads no statute and it makes no claim about the law. It is *how constraints 1–3 above are actually
enforced in code*: an entitled agency behind the proposal is the machine-checkable proxy for "a
licensed agent composed and sent this." Under the register's own admission rule — a decision earns an
LA number when it both rests on a reading of authority **and** would be expensive to reverse, *both,
not either* — this fails the first test.

⚠️ **The consequence is that the availability resolver becomes the enforcement point for a legal
boundary, not only a visibility preference.** That materially raises the cost of getting it wrong.
A resolver bug that over-reports entitlement does not merely show a section to the wrong audience; it
renders market data into an employer-facing document in a case this entry does not cover. **The
resolver should be treated as a compliance control and reviewed as one.**

⚠️ **And it cannot reuse the resolver that exists.** `IchraAccessResolver.isAvailable` takes an
`HttpServletRequest`, reads `HttpSession`, and short-circuits on the `isPspAdmin` session attribute
(`IchraAccessResolver.java:43-96`). **The public proposal view has no session at all** — `ViewProposal`
(`/proposal/*`) is exempted from `LoginFilter` and reads no session anywhere in its render path. A
proposal-scoped answer needs a **separate, session-free overload** resolving from the `Proposal`
instance. The chain exists and is already used on that page
(`OriginatingAgencyResolver.resolve(Proposal)` → `Agency.isIchraEnabled()`), so this is a small
addition rather than new plumbing — see the S9-A probe conclusion and the backlog item it produced.
**The PSP-admin bypass must not be carried across**: on a public page there is no admin, and a bypass
that cannot fire is a bypass waiting to be reintroduced by someone who does not know why it was absent.

**Risk if wrong.** **Same class as LA-09** — state producer licensing, per state. **A characterisation
problem rather than a code problem**: the exposure is that a regulator reads the arrangement
differently than SSA does, and no architecture inside AMS answers that. Per-state by construction, for
the same reason LA-09 is (ERISA §514(b)(2)(A) preserves state regulation of insurance).

**Reversal cost.** ⭐ **LOW — a display edit — and only because of constraint 2.** The snapshot stops
rendering in the section; the proposal is otherwise unchanged. **Record explicitly: if the agentless
front door is ever built, this entry's reversal cost escalates to rebuild**, matching LA-09's own.
At that point the reversal is not a display edit but the removal of shipped functionality somebody was
sold on — which is precisely the escalation LA-09 already describes, arriving through this entry
instead of directly.

**Confirm before.** **A second state beyond Texas**; or **any agentless front door**; or **any
SSA-initiated communication to a prospect**. Any one of the three, not all three.

**Status.** Assumed — **thin** — 2026-08-02. **Thin, and specifically why:** this is structural
reasoning plus an industry-pattern observation, and nothing else. **Nobody has read Tex. Ins. Code's
solicitation definitions** — LA-09 already flags that several states define "solicit" broadly enough to
capture presenting plan-specific pricing to a prospective purchaser, and that warning is not answered
here, only narrowed around. **Texas is the entire book, so Texas is the one jurisdiction where the hour
would pay.** It belongs with LA-09 in Group 4 of the counsel list, and the free research is the same
research.

⚠️ **This entry does not resolve O25.** O25 is carrier names on **SSA-drafted paper**, and LA-04
disposes of the safe-harbor half of it while the design declines to rely on the answer. **Carrier names
in a proposal remain separately open** — LA-17 says an agent may send market data, not that SSA may
print a carrier name. Constraint 3 is what keeps the two questions apart: the agent's section and
SSA's are different paper.

---

### LA-18 — Staging rate data is treated as fully authoritative pending production HealthSherpa access

**This is a technical/operational assumption, not a reading of statute** — recorded here because
`legal_assumptions.md` is this project's only assumption register (confirmed by direct search, S21-L;
no separate technical register exists) and because its risk, once an agency is entitled, is the same
class of exposure LA-17 already governs: market data reaching an employer on a document neither SSA
nor the agent can vouch for as accurate.

**Assumption.** Cached rate data sourced from AMS's staging HealthSherpa environment is presented to
entitled ICHRA users — including on the public, unauthenticated proposal page — as though it were
authoritative market fact, with no distinguishing marker, disclaimer, or staging banner anywhere in
the rendered document. A single reference constant, `ICHRA_RATE_SOURCE_ENV` (read via
`RateSourceEnvResolver.authoritativeSourceEnv`, S21-L), names which `RatingAreaRateCache.sourceEnv`
value — `STAGING` or `PRODUCTION` — is authoritative for this installation. **Today it reads
`STAGING`.**

**Basis.** Kevin's explicit, on-the-record decision (2026-08-06, S21-L): the product must be
demonstrable end to end before production HealthSherpa access exists, and gating every ICHRA surface
behind "no data is production yet" would make that impossible during the demonstration window. **The
control is entitlement, not data provenance** — `IchraAccessResolver` / `agency.ichra_enabled` decides
who can see ICHRA content at all, and **nobody outside PSP admin is being granted that entitlement
while the constant reads `STAGING`.** Kevin is not treating the figures as real himself; he is choosing
to let entitled users see them presented as though they were, for demonstration purposes, ahead of
granting entitlement to any real agency. Staging remains meaningful afterward — dev and test
installations continue to point at it — so this is a configurable constant with a considered default,
not a temporary switch awaiting deletion.

**Design choice.** One resolver, one question ("which source env is authoritative"), backed by a
`constant` row seeded to `STAGING` (`DatabaseInitializer`). Every read-time provenance check that
previously hardcoded `SOURCE_ENV_PRODUCTION` — `RateCacheDAO.check`, `ViewProposal.putIchraMarketTokens`,
`ProposalBuilder`'s two snapshot-attach checks — now asks the resolver instead, uncached across
requests, so flipping the constant takes effect on the next call, no restart. What is written to a
cache row or stamped onto a snapshot at build time is unchanged and stays honest regardless of what
this resolver currently favors.

**Risk if wrong.** If an agency is entitled while the constant still reads `STAGING`, that agency's
agents — and any employer who receives one of their proposal links, since `/proposal/*` is public and
unauthenticated — see plan counts, carrier counts, and premium floors sourced from a staging
environment, presented as plain fact, with nothing in the document marking them as non-production. If
staging figures diverge materially from the real market — a live possibility, not a hypothetical: a
different S21-K-adjacent probe this session found an off-exchange LCSP figure understated by roughly
44% at age 40 against a comparable on-exchange baseline — an employer could make a real coverage
decision, or an agent could make a real representation to a client, based on a number that does not
reflect the actual market. This is a materially larger exposure than a code bug: it is data presented
as fact that is not fact, on a customer-facing document, by deliberate design rather than by accident.

**Reversal cost.** Low in one direction, not reversible in the other. Flipping `ICHRA_RATE_SOURCE_ENV`
to `PRODUCTION` once real data is live is a one-row change with no code impact — every read-time check
follows immediately. **But any proposal already sent while the constant read `STAGING` cannot be
un-sent or silently corrected** — its figures are frozen into that proposal's `payload_json` at build
time (this session's own point-in-time design, T162), and an employer who has already read them has
already read them. The reversal cost that matters is not the constant's; it is the accumulated set of
documents already built under it by the time it flips.

**Confirm before.** Granting `agency.ichra_enabled` to any agency other than through PSP-admin access,
while `ICHRA_RATE_SOURCE_ENV` still reads `STAGING`. That is the one trigger — not a code change, not a
deploy, a single-row data change in a different table (`agency`) than the one this entry is about
(`constant`).

**Status.** Assumed — **accepted explicitly**, 2026-08-06 (S21-L). Not thin in the sense of "nobody
has considered it" — this is a deliberate, informed trade Kevin made with the risk stated plainly and
on the record, per this run's own instructions not to soften it. It is unresolved only in the sense
that production data readiness, not further review, is what closes it.

---

### LA-19 — §125 pre-tax treatment of individual premiums requires ICHRA coverage and off-exchange purchase

**Assumption.** A cafeteria plan may treat individual health insurance premiums as a qualified
benefit only where the employee is covered by an ICHRA and the coverage was not purchased through
an Exchange. IRC §125(f)(3) independently and permanently bars any QHP offered through an Exchange
from being a qualified benefit. The employee population therefore splits three ways:
excepted-benefit buyers, who run on the ordinary §125 voluntary-benefit track and need no ICHRA —
and arguably cannot validly hold one, since ICHRA eligibility requires enrollment in individual
health insurance coverage and excepted benefits are not that; off-exchange individual major medical
buyers, who keep the ICHRA and pre-tax the residual; and on-exchange buyers, who waive the ICHRA to
preserve PTC eligibility and pay post-tax only.

**Basis.** IRC §125(f)(3). Notice 2013-54, under which an employer arrangement paying or
reimbursing individual market premiums — including through §125 salary reduction — is an employer
payment plan that fails PHSA 2711 and 2713. The 2019 ICHRA final rule's amendment to the cafeteria
plan regulations creating the off-exchange exception. 26 CFR 54.9802-4's individual-coverage
requirement. Thin point, stated plainly: whether the amended regulation conditions the permission
on the employee being covered by an ICHRA or merely offered one has not been read against primary
text. All reasoning here assumes "covered by," which is the more restrictive reading.

**Design choice.** Model the three buckets explicitly. Treat ICHRA waiver state as determinative of
§125 eligibility for individual major medical. Substantiation gains a channel element: the
attestation must establish not only enrollment in individual coverage but that it was not purchased
through an Exchange. An employee who moves to on-exchange coverage mid-year has their election
terminated rather than warned.

**Risk if wrong.** If the condition is "offered," the design is merely more conservative than
required and nothing breaks. If it is "covered by" and the opposite had been assumed, employees who
waived would have taken impermissible pre-tax reductions — W-2 and 941 corrections plus a §125
operational failure.

**Reversal cost.** Low before any employee election is collected. High afterward: corrections run
per-employee, per-pay-period.

**Confirm before.** Before any plan document is issued, and before any employee salary reduction
election is accepted for individual major medical.

**Status.** OPEN — assumed, not verified against primary text. The most load-bearing unverified
item in the structure.

---

### LA-20 — ICHRA and §125 funds may share one card with separate sub-accounts

**Assumption.** An ICHRA may pay individual premiums directly to the issuer, including through a
card in the employee's name, and may share a single card with a §125 salary-reduction sub-account
provided the sub-accounts are separately ledgered and the §125 election is set net of the ICHRA
allowance.

**Basis.** An ICHRA is a group health plan integrated with individual coverage; paying or
reimbursing individual premiums is its authorized purpose, so the employer-payment-plan concern
governing unintegrated arrangements does not reach it. §105(b) excludes reimbursement only of
expenses not otherwise compensated, so pre-taxing the full premium while the ICHRA also pays part
of it would exclude the same dollars twice. Existing FSA/HRA stacked-card practice with an ordering
rule is the operational precedent. The PremiumPath constraint requiring clearly-employee dollars was
developed in a QSEHRA context, where no integration exists, and is not assumed to govern here.

**Design choice.** Two sub-accounts, one purse. ICHRA drawn first, so employer dollars do not sit
unspent while the employee over-reduces. §125 election capped at premium minus the ICHRA allowance
and re-set when the premium changes. Separate ledgering is a merit rather than plumbing: it proves
the ICHRA paid exactly its allowance and no more.

**Risk if wrong.** If separate accounts or separate settlement were required, the card
configuration is wrong but no tax position is.

**Reversal cost.** Low — card configuration and funding schedule.

**Confirm before.** First mixed-funded card transaction.

**Status.** Assumed; design chosen for cheap reversal.

---

### LA-21 — Arrears loading is the control preventing employer-payment-plan characterization

**Assumption.** A funding schedule that releases salary-reduction dollars only after the pay dates
funding them cannot produce a purse that systematically runs ahead of withholding, and therefore
cannot constitute an arrangement under which the employer pays individual premiums beyond the ICHRA
allowance. Isolated variances from payroll error remain failures correctable within the §4980D
reasonable-cause window rather than features of the design.

**Basis.** §4980D imposes $100 per day per employee for market reform failures, with relief where
the failure is due to reasonable cause rather than willful neglect and is corrected within 30 days
of when the employer knew or should have known. A schedule designed to post ahead of withholding is
not a failure being corrected — it is the arrangement operating as intended, and reasonable cause is
hard to argue for a mechanism that was specified, documented and configured. A §125 election must
also be prospective; reductions recovering money already spent are repaying an advance.

**Design choice.** Arrears loading for the off-exchange ACA bucket, on a monthly cadence so the
full premium is available on the first and no carrier draft fails. The resulting one-cycle gap at
plan year start is absorbed by setting the coverage effective date one payroll cycle after salary
reduction begins. Annual true-up explicitly rejected as the control point: twelve months of
accumulated advance is a standing arrangement. If arrears loading is ever relaxed, 30-day variance
detection substitutes for it.

**Risk if wrong.** Overstated in the conservative direction. If a modest systematic advance were
acceptable, the design costs one onboarding cycle and nothing else. The asymmetry justifying it: an
under-funded purse is a customer-service problem, while a declined premium draft can lapse an
off-exchange policy, which in turn breaks the ICHRA's individual-coverage requirement for that month
and converts a payment glitch into a substantiation failure.

**Reversal cost.** Low — funding schedule.

**Confirm before.** First mid-year termination with an outstanding purse balance. Any proposal to
load ahead of withholding.

**Status.** Assumed; conservative by choice. Arrears loading is one of five permitted on-ramps
rather than the only control; see LA-36, which records the full set and the expressly-permitted
month-ahead cadence that makes the concession unnecessary where an employee-sourced on-ramp funds
month one.

---

### LA-22 — Employer advance of excepted-benefit premiums is not an employer payment plan

**Assumption.** An employer may pay excepted-benefit premiums — accident-only, specified disease —
at the start of a coverage period and collect the employee's share by salary reduction across that
period, without creating an employer payment plan.

**Basis.** Excepted benefits are excepted from PHSA 2711 and 2713, so there is no market reform to
violate. Employer-provided accident and health coverage is excludable under §106, so unrecovered
premium is employer-paid coverage rather than taxable income or discharged debt. Economically
identical to conventional group list-bill practice, where the employer remits on the first and
collects employee contributions during the month.

**2026-09-09 addendum — the excepted-benefit characterization is no longer assumed.** It is now
supported by primary documents: (i) TDI/SERFF approval of separate specified-disease and
accident-only forms for Presidio HealthCare Insurance Company (NAIC 17821) under TOI H07I.002
(Dread Disease) and H02I.000 (Health – Accident Only) — trackings PHCI-134639174,
PHCI-134654486, PHCI-134639079, PHCI-134654162, all closed/approved 2025-08-20; (ii) an
enumerated, individually defined 23-category condition schedule (Policy Schedule §I.B.4 of
`TX-SERRA-SD-Freedom-2025`) with an insuring definition — *"'Sickness / Condition' means each of
the conditions set forth in the Policy Schedule"* — limited to that schedule, rather than an open
"any sickness" grant; (iii) a face-page recital that benefits are *"independent from and not
coordinated with any other insurance coverage"* and paid *"on an expense incurred basis,"*
satisfying the non-coordination conditions at 26 CFR 54.9831-1(c)(4)(ii)(B)–(C) / 45 CFR
148.220(b)(3); and (iv) face-page notices disclaiming MEC and major-medical status (*"IS NOT
MINIMUM ESSENTIAL COVERAGE UNDER FEDERAL LAW"*; *"SUPPLEMENTAL COVERAGE ISSUED ONLY TO SUPPLEMENT
INSURANCE ALREADY IN FORCE"*).

**Design choice.** Frame in the plan document as employer premium payment with employee
contribution collected over the coverage period — not as an advance the employer recoups. The
framing determines the tax result on a mid-year termination: forgiven debt is taxable, unrecovered
§106 coverage is not. Wage deduction authorization included in the salary reduction agreement; Texas
Labor Code §61.018 requires written authorization and final-paycheck limits still cap recovery.
Reported salary reduction must equal what was actually withheld, not what was scheduled.

**Risk if wrong.** The characterization rests entirely on the policies being genuine excepted
benefits. If that fails, the employer has been advancing money against unintegrated individual
major medical. Fronting converts a carrier's regulatory problem into the employer's and the TPA's.

**2026-09-09 addendum — residual exposure, now that the base characterization is supported.** The
residual is a substance-over-form argument: that 23 categories approximating the whole of medicine,
marketed as an alternative to comprehensive coverage, is not "coverage *only* for a specified
disease or illness." That argument is aggravated by Presidio's public marketing, which describes
the product as health insurance and prices it against ACA bronze. It is not a defect on the face of
the form.

**Reversal cost.** Low — funding schedule. But retroactive if the excepted-benefit characterization
is ever challenged.

**Confirm before.** Any challenge to the excepted-benefit characterization of the underlying
policies.

**2026-09-09 addendum.** Also confirm before relying further: which product generation and which
bracketed condition subset the pilot actually issues, and whether the 10/17/2025 form revision
(the text read) is the approved text — both unconfirmed with the carrier.

**Status.** Assumed. Arrears loading is available as a no-cost hedge, since it is being built for
the ACA bucket regardless. The asymmetry is deliberate and is the reason the same card mechanism
runs differently by benefit type: excepted benefits sit outside the market reforms and carry a §106
shelter on unrecovered premium, and individual major medical has neither. See LA-36 for the funding
paths available on the major medical side.

**2026-09-09 status update.** No longer a bare assumption. **Supported by primary documents (state
form approval + form text), with residual exposure to (a) future tri-agency rulemaking of the 2023
kind and (b) a substance-over-form challenge to the breadth of the condition schedule.** The forms
read are marked *Revised 10/17/2025*, post-dating the 2025-08-20 SERFF submissions, and the issued
condition schedule is bracketed/variable — so both the approved text and the issued subset remain
unconfirmed with the carrier. See LA-38 for the separate question of who may pay Presidio premium.

---

### LA-23 — An ICHRA may fund on-exchange premiums by member-name card; residual is post-tax only

**Assumption.** Nothing in the ICHRA rules restricts reimbursable coverage to off-exchange. A full
ICHRA may fund an on-exchange individual policy through a card in the employee's name. The
employee's residual is post-tax only, since §125(f)(3) bars Exchange QHPs from a cafeteria plan
categorically.

**Basis.** 26 CFR 54.9802-4 does not condition on purchase channel; the off-exchange condition
attaches to the §125 permission alone. §125(f)(3) is statutory. A card presenting as the member
also sidesteps carrier policies on accepting premium from third parties.

**Design choice.** Support as a fourth funding profile: ICHRA sub-account on schedule, post-tax
employee sub-account in arrears. Declining APTC at enrollment is an operational requirement of the
arrangement rather than a notice footnote — an employee covered by an ICHRA is PTC-ineligible for
those months, and with the reconciliation repayment caps removed there is no ceiling on what an
unchecked box costs at reconciliation. Arrears still applies to the residual: a purse running ahead
of employee contribution is employer money paying an on-exchange premium above the allowance, and
there is no §106 to catch a forgiven balance on termination.

**Risk if wrong.** The ICHRA-side assumption is low risk. The residual-must-be-post-tax point is
statutory and high confidence. Unknown: whether any carrier or marketplace-side rule treats
HRA-funded card payment differently for on-exchange business. That is carrier policy, not
regulation, and varies.

**Reversal cost.** Low.

**Confirm before.** First on-exchange card configuration for a live client.

**Status.** Assumed. Governs existing conventional ICHRA clients, not only the prospective
structure.

---

### LA-24 — The structure is designed for non-ALEs and inverts above 50 FTEs

**Assumption.** A deliberately unaffordable micro-ICHRA is an offer of coverage that avoids the
4980H(a) penalty but not 4980H(b). An applicable large employer therefore incurs a (b) penalty for
every employee who receives a subsidy, and the economics invert above 50 full-time equivalents.

**Basis.** 4980H(a) and (b) mechanics. An ICHRA offer counts as an offer of coverage. Affordability
is measured against the on-exchange lowest cost silver plan for the rating area, minus the ICHRA
amount — a CSR-loaded figure, which makes a small allowance comfortably unaffordable and the (b)
exposure correspondingly certain.

**Design choice.** Non-ALE status is a qualifying question in the sales motion, asked before design
work begins — not a footnote in the plan document.

**Risk if wrong.** Selling into an ALE creates penalty exposure at the employer, designed by the
TPA.

**Reversal cost.** None if caught at qualification. The engagement if not.

**Confirm before.** Any prospect at or approaching 50 full-time equivalents.

**Status.** Assumed, high confidence.

---

### LA-25 — Participant identifiers must be globally unique

**Assumption.** `Participant TPA Custom ID` must be unique across every employer in the Summit
installation, not merely within one employer.

**Basis.** **Test-verified.** A duplicated per-employer participant ID was accepted by the
Demographics import and reported success; enrollment on that same ID under HRA Enrollment returned
`Employer ID Conflict`, even though `Employer TPA Custom ID` was supplied in the enrollment row.
Summit could not disambiguate the participant. See `docs/business/summit_data_exchange.md`.

**Design choice.** Derive participant IDs from a globally unique AMS key, such as the employee
record's own primary key — never a per-employer sequence.

**Risk if wrong.** None in this direction; the reverse — treating the ID as per-employer-unique —
is what failed the test.

**Reversal cost.** **Rising sharply.** Cheap before any participant is created in Summit; requires
re-keying live records afterward.

**Confirm before.** N/A — already confirmed by test.

**Status.** Confirmed by test, 2026-09-07.

---

### LA-26 — Summit upserts on `Employer TPA Custom ID`, so AMS emits full state

**Assumption.** Re-importing an Employer Demographic file with an existing `Employer TPA Custom ID`
updates that employer in place rather than creating a duplicate, so AMS can safely regenerate and
resend full current state on every run with no delta tracking.

**Basis.** **Test-verified.** Re-import with the same ID produced `Employer edited successfully`
in place of the original `Employer created successfully`; a byte-identical record was rewritten
rather than skipped. See `docs/business/summit_data_exchange.md`.

**Design choice.** No sent-state store, no create-vs-update branch, no reconciliation table. AMS
emits full current state each run.

**Risk if wrong.** Silent duplicate employers.

**Reversal cost.** Moderate — adds a sent-state store.

**Confirm before.** N/A — already confirmed by test.

**Status.** Confirmed by test, 2026-09-07.

---

### LA-27 — `Funding tax treatment = Pre-tax` correctly represents employer ICHRA contributions

**Assumption.** Setting the ICHRA plan template's Funding tax treatment element to `Pre-tax`
correctly represents an employer-funded ICHRA contribution.

**Basis.** ⚠️ **Reasoning from option names only, not verified against Summit behaviour.** Employer
ICHRA money is excluded under §105/§106 and is not a salary reduction, which is what "pre-tax"
normally denotes; no option among the six available actually describes employer-provided excludable
money. Every alternative taxes money that should not be taxed.

**Design choice.** Configure the test template as `Pre-tax` pending confirmation.

**Risk if wrong.** Incorrect payroll or W-2 treatment on real money.

**Reversal cost.** **Cheap as a template setting, rising once contributions are processed.**

**Confirm before.** First live ICHRA funding.

**Status.** Assumed — thin basis, 2026-09-07.

---

### LA-28 — `PCOR Reportable` should be enabled on the ICHRA plan template

**Assumption.** The ICHRA plan template should have `PCOR Reportable` enabled.

**Basis.** ⚠️ **General PCORI treatment of HRAs, not read against primary text.** An ICHRA is a
self-insured group health plan, and the employer generally owes PCORI fees counted on covered
employees. The flag was off in the test template.

**Design choice.** Flag pending confirmation; not yet changed from the test template's off setting.

**Risk if wrong.** If Summit uses the flag to drive reporting data capture, the data is absent when
an annual filing comes due — a gap that surfaces long after setup.

**Reversal cost.** **Cheap now, expensive to reconstruct retroactively.**

**Confirm before.** First live ICHRA plan is created.

**Status.** Assumed — thin basis, pairs with open question O-10 in
`docs/business/summit_data_exchange.md`, 2026-09-07.

---

### LA-29 — `Prospect.id` is the employer identity Summit keys on

**Assumption.** `Employer TPA Custom ID` in the Employer Demographic and Employer CDH Plan files
should be sourced from AMS's own `Prospect.id`, not from any employer-record or Summit-facing field.

**Basis.** **Code-verified.** `Prospect.id` is `@GeneratedValue`, immutable, and exists before the
employer is ever sent to Summit. The alternatives considered — `Employer.organization_id`, an
`er_key` — are Summit-owned and circular for the create case (Summit has not assigned one until
after the first successful import), and a tax ID is mutable and unsuitable for an upsert key per
LA-26.

**Design choice.** `SummitExportServlet` renders `Prospect.id` as a plain string into the
`Employer TPA Custom ID` column of both files (`SummitExportServlet.java`).

**Risk if wrong.** Employers keyed wrongly in Summit — either a new employer created per file
(never converging) or two AMS employers upserted onto the same Summit record.

**Reversal cost.** Cheap before the first file is imported; rising sharply after, since LA-26
establishes this is an upsert key and changing it orphans the old record and creates a duplicate.

**Confirm before.** The first real (non-test) employer file is imported into Summit.

**Status.** Assumed, 2026-09-07. **Updated 2026-09-07 (S25-C):** the employer key is now emitted as
a configured installation prefix (`SUMMIT_TPA_ID_PREFIX`) plus `Prospect.id`, raising uniqueness
from per-installation to cross-installation — `Prospect.id` alone is only unique within one AMS
database, and the platform's multi-installation ambition means two installations could someday feed
the same Summit TPA account. The prefix must never change once any employer has been imported under
it, for the same reversal-cost reason `Prospect.id` itself must not.

---

### LA-30 — ICHRA plan years are calendar years

**Assumption.** An ICHRA plan year always runs January 1 through December 31 of
`proposal_ichra_intake.plan_year`.

**Basis.** `proposal_ichra_intake.plan_year` stores a single year integer with no accompanying
begin/end date pair, so Jan 1 – Dec 31 of that year is the only interpretation the current schema
can express. `SummitExportServlet` derives `Effective Date`, `Plan Year Begin`, and `Plan Year End`
this way.

**Risk if wrong.** A non-calendar plan year cannot be exported at all today, and would be silently
exported as a calendar year instead of erroring — the export has no way to know the assumption
does not hold for a given employer.

**Reversal cost.** Moderate — requires capturing explicit begin/end dates on the intake (or plan),
a schema change, plus a servlet update to prefer the explicit dates when present.

**Confirm before.** The first non-calendar ICHRA plan year is sold.

**Status.** Assumed, 2026-09-07. **Narrowed 2026-09-07 (S25-B):** no longer load-bearing for the
Employer CDH Plan export — `SummitExportServlet` now reads `plan_year_start`/`plan_year_end` as real
dates from application answers, so a non-calendar plan year is expressible there. Still applies
anywhere a bare year integer is the only available source.

---

### LA-31 — Employer identity details for the Summit export come from application answers, not `Prospect` or `proposal_ichra_intake`

**Assumption.** The employer's mailing address and plan year emitted in the Employer Demographic and
Employer CDH Plan files are sourced from the proposal's application field values
(`address_street1`/`address_city`/`address_state`/`address_zip` and
`plan_year_start`/`plan_year_end`), not from `Prospect.address` or `proposal_ichra_intake.plan_year`.

**Basis.** **Code-verified.** `Prospect.address` is unset at five of the seven `new Prospect()`
creation sites (`CreateOpportunity`, `CreateProspect`, `CreateSetup25`, `RequestQuote`, and the demo
seeder is the sixth, address-set) and, at the remaining two (`GenerateProp`, `GenerateProp25`), is
assigned from the contact `Person`'s address — which was itself set from the selling `Agency`'s
address, not the employer's. `Prospect.address` therefore never holds the employer's own address. The
applicant asserts the real address and plan year during the application process; those answers persist
in `applicationfieldvalue`, keyed by `applicationfield.field_key`.

**Design choice.** `SummitExportServlet` reads the answer map by literal `fieldKey`, following the
established house pattern (`ApplyForProposal:243-247`, `ReviewApplication`, `bill_benefit_plans`
throughout the application surfaces). It refuses to emit, with a message naming the missing field key,
when the proposal has no application, no saved answers, or a required key is absent, blank, or
(for the two plan-year fields) unparseable as a date. No fallback chain to `Prospect`/`Agency` data is
implemented, deliberately — a silent fallback is the failure pattern this assumption exists to remove.

**Risk if wrong.** An export that refuses more often than strictly necessary — e.g. on an installation
that has not loaded the `s125_fsa` package's `plan_year_eligibility` section, `plan_year_start`/
`plan_year_end` will never be present regardless of whether the applicant supplied a plan year some
other way. That is the safe direction: it fails closed, not open.

**Reversal cost.** Cheap — a source repoint confined to `SummitExportServlet`, no schema.

**Confirm before.** The first real (non-test) Employer Demographic or Employer CDH Plan file is
generated for an installation, to confirm the expected `ApplicationField` rows are actually present.

**Status.** Confirmed by code inspection, 2026-09-07.

---

### LA-32 — The Summit TPA prefix is configuration, not a code literal

**Assumption.** The installation-specific prefix combined with `Prospect.id` to form `Employer TPA
Custom ID` (and, by extension, `Import Plan ID`) must be read from config (`SUMMIT_TPA_ID_PREFIX`)
at request time, never hardcoded in `SummitExportServlet`.

**Basis.** **Code-verified.** `Employer TPA Custom ID` is Summit's upsert key (LA-26); `Prospect.id`
is `@GeneratedValue` and only unique within one AMS database (LA-29). A literal prefix baked into the
servlet would be identical across every installation running that code, defeating the purpose the
moment a second installation exists — the platform's stated ambition.

**Design choice.** `resolveEmployerTpaCustomId` reads `AppConfig.get("SUMMIT_TPA_ID_PREFIX")` at
request time and validates it (non-blank after trim, no pipe, no whitespace) before use. No config
file for this key is tracked in the repo — same as `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` — so it is an
operational note for Kevin to set in `ssa.properties`, not a code or migration change.

**Risk if wrong.** None in the refusing direction — an absent or invalid prefix causes both file
types to refuse rather than emit. The risk is entirely in the alternative not taken: a silent
bare-`Prospect.id` fallback would let two installations both emit `42` for different employers,
and Summit would upsert one onto the other.

**Reversal cost.** Cheap before the first real import — the config value can be corrected freely.
Effectively irreversible after — changing an upsert key orphans every record keyed on the old value,
the same failure mode LA-29 already establishes for `Prospect.id` itself.

**Confirm before.** The first real (non-test) employer file is imported into Summit — same
confirmation point as LA-29, since the two assumptions are verified together in practice.

**Status.** Assumed, 2026-09-07.

---

### LA-33 — The participant key is derived, not stored

**Assumption.** The Summit `Participant TPA Custom ID` is built at emit time as
`{SUMMIT_TPA_ID_PREFIX}-P-{employer_participant.id}` and is never persisted. `employer_participant`
carries no column for it, deliberately.

**Basis.** **Code and operational experience, not counsel.** This mirrors the employer key already
established in LA-29 and LA-32: an installation-scoped prefix read from config at request time,
combined with an AMS-generated surrogate id that is only unique within one AMS database. The `-P-`
infix keeps the participant namespace disjoint from the employer namespace, so a prospect id and a
participant id can never collide into the same Summit key.

**Design choice.** `employer_participant.id` is `BIGINT AUTO_INCREMENT` and is the opaque immutable
key. A stored copy of the derived value would be a second source of truth that can drift from the
derivation — the emit path would then have two answers and no rule about which wins.

**Risk if wrong.** If Summit turns out to require a participant key that is stable across a change of
prefix, or one that carries employer-scoped rather than installation-scoped uniqueness, every
participant already imported is keyed on a value AMS can no longer reproduce. The refusing direction
is safe: an absent or invalid prefix causes the emit to refuse rather than fall back to a bare id.

**Reversal cost.** Cheap before the first real import — nothing is keyed on it yet. **Effectively
irreversible after** — changing an upsert key orphans every record keyed on the old value, the same
failure mode LA-29 and LA-32 already establish for the employer key.

**Confirm before.** The first real (non-test) Demographics file is imported into Summit — the same
confirmation point as LA-29 and LA-32, since all three are verified together in one import.

**Status.** Assumed, 2026-09-07.

---

### LA-34 — The roster refuses replacement rather than merging

**Assumption.** Once a census is loaded for an employer, a second upload is refused. The operator
must explicitly clear the roster first. AMS does not merge, diff, or upsert an incoming census against
a loaded one.

**Basis.** **Code and operational experience, not counsel.** Participant ids are AMS-generated and are
not reissued: clearing and re-uploading assigns new ids to the same people. Under LA-33 the Summit key
is derived from that id, so a silent re-key orphans every Summit participant record created from the
previous load, and the orphaning is invisible until Summit is next reconciled. A refusal is
recoverable — the operator sees it immediately and decides. A silent re-key is not.

**Design choice.** `CensusUploadServlet` calls `EmployerParticipantDAO.countByProspectId` before
parsing and refuses by name when it is non-zero. The clear action is POST-only, PSP-admin gated, and
requires an explicit confirmation parameter; the confirmation text states the orphaning consequence
in the operator's own terms rather than in schema terms.

**Risk if wrong.** The cost is operator friction on a legitimate mid-year census correction — an
employer adds three employees and the whole roster must be cleared and reloaded, reassigning ids for
everyone including the unchanged majority. That is a real cost, and it is the reason a merge path is
worth building once the reconciliation behaviour is understood.

**Reversal cost.** Cheap. A merge or incremental-add path can be added later with no schema change —
the table already has a surrogate key and no name or address uniqueness constraint, so appending
participants to an existing roster is a pure code change.

**Confirm before.** The first employer needs a mid-year census correction after an emission. That is
the point at which the friction becomes real and the merge semantics have to be decided.

**Status.** Assumed, 2026-09-07.

---

### LA-35 — Employee email is collected; SSN, date of birth and compensation never are

**Assumption.** `employer_participant` collects email when the employer's file supplies it, and never
collects social security number, date of birth, or compensation — even when the employer's census
contains all three, as employer censuses routinely do.

**Basis.** **Code and operational experience, not counsel.** Summit's Demographics import requires
none of the four. Email is routinely present in employer files, is not a sensitive identifier in the
way the other three are, and is the identifier a future employee-facing portal or notice-delivery
surface would need. The other three would be collected only because the employer's spreadsheet
happened to carry them, which is not a reason.

**Design choice.** The exclusion lives at the **parser**, not only at the schema. `CensusParseService`
recognises columns by header against a fixed synonym set and drops every unrecognised column silently;
SSN, DOB and compensation headers match nothing, so those values are never mapped, never held in a
parsed row, and never reach the persistence layer. A schema-only exclusion would still pull the values
into process memory and into any future logging or error message that echoed a row.

**Risk if wrong.** If Summit or a downstream product later requires date of birth — for age-banded
enrollment, say — the roster cannot supply it and every employer must be asked for a second file. That
is a real re-work cost, and it is the deliberate trade: the cost falls on the direction that can be
paid later, not on the direction that cannot be undone.

**Reversal cost.** Asymmetric, which is the whole point. **Dropping a nullable column is cheap;
collecting personal data is the direction that does not reverse** — once SSNs have been written to a
production table, backups, and any log that echoed them, "we stopped collecting it" does not undo the
collection. That asymmetry is why the three are excluded at the parser rather than merely left out of
the schema.

**Confirm before.** Any change that would add SSN, date of birth, or compensation to this roster —
which is a new assumption requiring its own entry and a fresh look at what the data protection posture
then has to be, not an edit to this one.

**Status.** Assumed, 2026-09-07.

---

### LA-36 — First-month premium funding comes from an employee-sourced on-ramp; the month-ahead cadence is expressly permitted

**Assumption.** A cafeteria plan may fund off-exchange individual major medical premium on a
month-ahead cadence — each month's salary reductions funding the following month's premium,
including the last month of a plan year funding the first month of the next — without deferring
compensation. A twelve-month first plan year must therefore fund thirteen months of premium, and
that one-month gap at inception is closed from employee-sourced funds. It is never closed by
employer advance.

**Basis.** The cadence carve-out — salary reduction contributions in the last month of a plan
year used to pay accident and health insurance premiums for the first month of the following
plan year, named in a list of practices that do not defer compensation — was read in the
preamble to the 2007 proposed cafeteria plan regulations as published in the Federal Register,
and restated in Internal Revenue Bulletin 2007-39. Both are agency text describing what the
regulations provide. The operative regulation section itself was not read, so this is one tier
short of primary text and is recorded that way deliberately. The boundary on the other side —
that contributions for one plan year may not purchase a benefit provided in a subsequent plan
year — appears in Prop. Treas. Reg. § 1.125-1, Q&A-7 as quoted in IRS Notice 2005-42, and
forecloses stretching a prior-year reduction across the second and later months of the following
year. Separately and at a lower tier: the elective PTO rules relied on by option B — no
carryover between plan years, an ordering rule requiring nonelective PTO to be used first, and
cash-out or forfeiture of unused elective PTO at plan year end — rest on consistent practitioner
commentary from several independent sources, not on primary text. Agency text was read only to
the extent of confirming that paid time off is a permitted taxable benefit and that carryover
is barred generally as deferral.

**Design choice.** Five on-ramp options for month one, all employee-sourced, selected per employer
and in two cases per employee:

- **A — Bonus or stipend at the start of the plan year.** Prospectively elected, reduced to fund
  month one. Must be paid in the first payroll of the plan year, not the last payroll of the prior
  year: a January-paid bonus can fund months one through three, while a December-paid one reaches
  only January before the cross-year purchase prohibition binds.
- **B — PTO sale.** Irrevocable election made in the prior tax year, no independent cash-out right
  in the underlying policy. Available, not recommended — it is the most complex of the five and the
  sizing is demanding at high family premiums.
- **C — One heavier month.** The employee's first month of reductions carries two months' premium.
- **D — Employee pays month one directly, post-tax, from their own funds.** Simplest available;
  costs one month of pre-tax treatment.
- **E — Coverage effective one payroll cycle after reductions begin.** Free; the LA-21 default, and
  the right answer for any mid-year effective date.

C and D are offered to the employee as a choice, since neither carries a compliance dimension.

Employer advance with payroll recoupment is closed by decision and is not offered, negotiated per
case, or built. Cross-year restoration of sold PTO days is likewise closed: it is deferred
compensation under the prohibition cited above, and it restores nothing economically regardless.

**Risk if wrong.** Low in the direction that matters. If the carve-out were read more narrowly than
assumed, the fallback is option E, which is already the LA-21 default and costs one onboarding
cycle. No employee tax position depends on the carve-out being available — it governs only whether
a December reduction may fund January, and where it may not, coverage starts a cycle later. The
material risk sits on option A rather than the cadence: a stipend conditioned on purchasing
coverage is an employer payment plan regardless of payroll coding, and it also enlarges the
employer's effective contribution, which threatens the deliberate unaffordability the waiver path
depends on. See LA-24.

**Reversal cost.** Low. Funding schedule and per-employee on-ramp selection. No schema depends on
the choice; the sub-accounts, ledgering and monthly cadence are identical across all five options.

**Confirm before.** Any proposal to fund month one from employer funds. Any stipend whose payroll
description, plan document language or employee communication ties it to purchasing coverage. First
January-effective group where option A is the selected on-ramp.

**Status.** Assumed. Verification is deliberately tiered rather than uniform. The month-ahead
cadence rests on agency text — Federal Register preamble and IRB 2007-39 — with the operative
regulation section unread; the cross-year purchase prohibition rests on a proposed regulation
quoted in an IRS notice; the elective PTO rules underpinning option B rest on secondary
commentary only and must be read against primary text before any PTO on-ramp is used in a live
plan. That gap is contained: options A, C, D and E do not depend on the PTO rules, and option B
is already recorded as available rather than recommended. Two further items remain unverified
and are not load-bearing here — the precise conditions under which increased taxable
compensation escapes employer-payment-plan characterization, and the §4980D minimum-penalty and
cap figures.

---

### LA-37 — More-than-2% S-corp shareholders, partners and sole proprietors cannot participate in a cafeteria plan; §318 attribution reaches family

**Assumption.** More-than-2% S-corporation shareholders, partners in a partnership (including LLC
members taxed as partners), and sole proprietors cannot participate in a cafeteria plan. For
S-corporations, §318 attribution extends the exclusion to the shareholder's spouse, children,
parents, and grandparents. Where owner-employees *can* participate (a C-corporation), §125(b)
nondiscrimination applies, including the 25% key-employee concentration test — which a
management-only pre-tax offering fails almost by construction.

**Basis.** IRC §1372 (2% S-corp shareholders treated as partners for fringe-benefit purposes);
§318 attribution; Prop. Reg. §1.125-1(g)(2); IRC §125(b)(2). Long-settled and non-controversial;
the §318 family consequence is the piece worth confirming with whoever drafts the plan document.

**Design choice.** Entity type and *who is buying* become qualifying questions in the sales motion
alongside non-ALE status (LA-24). The pre-tax Presidio path is offered only where the Presidio
buyers are §125-eligible W-2 employees and the group adopts §125 for its whole eligible
population. Where the buyers are owners or owner-family, the post-tax path is the design, not a
fallback.

**Risk if wrong.** Overstated in the conservative direction is unlikely here; understated means
excluded individuals took impermissible pre-tax reductions — W-2 and 941 corrections per person,
per pay period.

**Reversal cost.** None if caught at qualification. Per-employee corrections if not.

**Confirm before.** Any §125 plan document naming Presidio premiums as a qualified benefit for an
owner-employee.

**Status.** Assumed, high confidence. Cross-reference LA-24 (non-ALE status) as a companion
qualifying question asked at the same stage of the sales motion.

---

### LA-38 — Presidio's §V.H payer restriction requires post-tax-by-default funding and a member-name card

**Assumption.** Presidio's approved policy forms restrict who may pay premium. An employer may
facilitate payment by payroll deduction **only where the payments do not create an ERISA group
health plan**. A §125 pre-tax salary-reduction arrangement very likely does create one, because
pre-tax salary reduction is treated as an employer contribution and the §125 plan document naming
the coverage is endorsement — either of which defeats the DOL voluntary-plan safe harbor. A TPA is
not on the accepted-payer list at all. Therefore: (i) post-tax payroll facilitation is the funding
model Presidio's own forms contemplate, and (ii) premium for every Presidio bucket must be paid by
a **card in the member's name**, which presents as the insured, rather than by an SSA-originated
ACH, which presents as an unauthorized third party.

**Basis.** §V.H (Third Party Payments) of `TX-SERRA-SD-Freedom-2025`, `TX-SERRA-ACC-Freedom-2025`
and `TX-SERRA-ACC-2025` — identical text in all three: *"Except as provided below, We do not
accept Premium or cost-sharing payments from any third party. Unauthorized Premium and
cost-sharing payments will not be credited to Your account and will be refunded to the
unauthorized payer. Premium and cost-sharing payments will only be accepted from: 1. You or a
member of Your family; 2. A Trust, Power of Attorney or Legal Guardian making payments on behalf
of an Insured; or 3. An employer facilitating payment collection through payroll deduction or
similar method for the employee (provided such payments do not create an Employee Retirement
Income Security Act group health plan)."* DOL voluntary-plan safe harbor at 29 CFR
2510.3-1(j); DOL Adv. Op. 1994-23A on the safe harbor's prongs; the carve-out in DOL Technical
Release 2013-03 / IRS Notice 2013-54 for after-tax employee premiums forwarded by an employer,
which is evidently what the clause was drafted to preserve. **State plainly:** that pre-tax §125
salary reduction defeats the safe harbor is uniform practitioner treatment and follows
structurally from salary-reduction dollars being employer contributions for §106 purposes, but
there is no regulation or ruling stating it in those words.

**Design choice.** Post-tax Presidio is the **default** card bucket. Pre-tax Presidio is offered
only where two gates are both passed: §125-eligible W-2 buyers (LA-37), **and** Presidio has
addressed §V.H in writing — preferably by an endorsement or amended form filed with TDI rather
than a side letter, since a letter accepting payments the approved form refuses is an unfiled
policy term. No SSA-originated ACH to Presidio in any bucket.

**Risk if wrong.** This is a **contract** risk, not a tax risk. The remedy written into §V.H is
non-crediting and refund of the premium — which runs the grace period and terminates coverage
prospectively for non-payment, rather than rescinding to issue. Claims incurred while premium was
credited stand. The real cost is reinstatement: re-underwriting plus a reset of the 12-month
pre-existing-condition clock, most likely discovered at a large claim.

**What this does NOT affect.** Excepted-benefit status is a property of the coverage, not of who
pays or how; ERISA-plan status merely shifts the test from 45 CFR 148.220(b)(3) to 26 CFR
54.9831-1(c)(4), which imposes the same conditions the form meets (see LA-22). Consequently the
§4980D $100/day/employee exposure does not attach to the Presidio bucket whether or not the
clause is waived or breached — the excise tax presupposes coverage that is *not* excepted. A
waiver by Presidio therefore does not reclassify the product; it is contractual and state-filing
housekeeping.

**Reversal cost.** Low prospectively — funding character and payment instrument. High for any
individual whose coverage lapses.

**Confirm before.** Any pre-tax salary reduction for a Presidio premium; any ACH origination to
Presidio.

**Status.** Assumed on the form text; awaiting Presidio's position on §V.H.

---

## Candidates considered and not adopted

Recorded so the next reader knows they were seen and declined, rather than missed. **None of these
carries an LA number** — they are not part of the register.

- **AI design advisor as education rather than advice (A6).** A chatbot answering an agent's ICHRA
  design questions is a compliance-relevant surface, and the build plan already sets a hard boundary:
  *"Education with citations. Never plan selection, never anything requiring licensure."* **Declined
  as a register entry because that boundary is a settled rule, not an assumption** — it restates the
  no-plan-selection rule already in `domain_and_compliance_rules.md`. It becomes an entry the moment
  the advisor is pointed at an employer or employee audience rather than an agent, at which point it
  is really LA-09 in a different costume.
- **Staging-sourced rate data displayed without provenance.** T48 establishes that `source_env` is
  advisory only — excluded from the delete scope and the unique key — and `IllustrationServlet`
  neither filters nor displays it, so an agent cannot tell which environment produced a number. The
  numbers are real (staging returns real rates), so this is not a data-quality issue. **Declined
  because it is a disclosure-and-engineering defect already tracked as T48 and §5.2 of the strategy
  doc, not a legal assumption** — no reading of any authority is being relied on. It would become an
  entry if a *stale* cache were ever shown as current, which is a different question nobody has asked.
- **Renewal-defense output and agent market conduct (replacement/twisting).** Presenting an agent with
  a case for replacing a group plan touches agent market-conduct rules. **Declined because D24 already
  disposes of it non-negotiably** — output goes to the agent, never to the employer — and the conduct
  is then the licensed agent's, in their own regulated lane. Worth re-opening only if D24 is ever
  softened.

---

## What would change these

The short list of findings that force **redesign** rather than adjustment. Everything else in the
register is an adjustment.

| Finding | Consequence |
|---|---|
| **Attestation is not sufficient substantiation (LA-01 disproven)** | **B3 fails and needs replacing before it is built.** D17's launch rung has no alternative — `CARD_TRANSACTION` is gated on O10 and `HS_POLICY_STATUS` is carrier-gated. This is the single most expensive finding available, and it invalidates a phase rather than a feature. |
| **Ingesting a policy-status feed creates a duty to act on it (LA-03's open sub-question answered yes)** | **B7 stops being a pure automation win and becomes a monitoring obligation.** Every feed ingested widens SSA's knowledge and cannot be un-ingested. Changes whether B7 is *wanted*, not just when it is built. |
| **Producer licensing captures agent-facing market display (LA-09 disproven)** | **The entire build-now scope is misplaced.** Rows 1–4 of the capability list are the wedge; if displaying them is producer activity, the wedge requires licensure SSA does not hold and the strategy's sequencing inverts. |
| **ch. 4151 applies to the existing book (LA-10 confirmed adversely)** | **A licensure problem, not a product problem** — and retroactive across the existing FSA/HRA/HSA/COBRA book, in every state where a participant resides. No code change addresses it. |
| **ICHRA has no short-first-year notice lever (LA-08 resolved unfavourably)** | **A permanent 90-day sales-cycle floor on ICHRA.** Not reversible by anything AMS does; it changes what can be sold and when, and the January-1 trap in LA-07 becomes the general case. |
| **Employer-driven ordering defeats the safe harbor more broadly than assumed (LA-06 narrowed)** | Individual policies become ERISA plans. Employer-side plan-qualification exposure, authored by SSA's display design. |

Deliberately **not** on this list: LA-04 (either answer is a display edit), LA-05 (the completeness
fact is verified, only the disclosure wording is assumed), LA-11 (narrowing is cheap at current
scope), and LA-12's audience half (a display edit). **That those four are absent is the standing
principle working** — they were designed to be cheap to be wrong about, and they are.

---

## What to price when counsel is engaged

Grouped by the event that triggers the need, **ordered within each group by cost of being wrong**, so
the engagement can be scoped rather than open-ended.

### Group 1 — Before scaling past a pilot *(the only group with a clock already running)*

| Entry | Question | Why it leads |
|---|---|---|
| **LA-10** | Does Tex. Ins. Code ch. 4151 require a certificate of authority for what SSA does **today**, and in which states? | The exposure is **retroactive across the existing book** and grows with participant-months, not with what is built. Delay is the expensive variable. **Read §4151.0021 and §4151.0022 first — it may not need counsel at all.** A TDI determination letter may be cheaper and more useful than an opinion. |

### Group 2 — Before the first real reimbursement *(the LA-01 package — one engagement, three questions)*

| Entry | Question | Why it leads |
|---|---|---|
| **LA-01** | Does a signed attestation suffice as a reimbursement-release record? | Terminal for B3, irreversible once a dollar moves. |
| **LA-03** | Does ingesting a policy-status feed create a duty to act on it? | Changes whether an automation is wanted, and the answer arrives too late once the feed is live. |
| **LA-02** | What must an annual baseline contain — specifically, how much dependent detail? | Cheapest of the three, and it constrains a schema that is about to be written. |

### Group 3 — Before any employee-facing display

| Entry | Question | Why it leads |
|---|---|---|
| **LA-06** | Where is the line between presenting and steering? | Safe-harbor failure is the employer's plan-qualification problem, authored by SSA. |
| **LA-12** | Is an affordability determination shown to an employee advice? | Turns on their own subsidy eligibility. **Note T44 must be fixed regardless of the answer.** |
| **LA-05** | Is the completeness disclosure adequate, and does the QSEHRA version name enough coverage types? | Misrepresentation risk; cheap to fix, easy to under-write. |
| **LA-04** | May carrier names appear? | **Ask last.** Either answer is a display edit and the design currently needs neither. |

### Group 4 — Before multi-state or non-agent audiences

| Entry | Question | Why it leads |
|---|---|---|
| **LA-09** | Is agent-facing market display producer activity — and does operating under the agency's license change it? | Per-state by construction. **Research the agency-license sub-question before paying for the main one; it may dispose of it.** |

### Not counsel — research first, and free

- **LA-08** — read 26 CFR §54.9802-4(c)(6) and the 84 Fed. Reg. 28888 preamble. May resolve the ICHRA
  notice-timing half at zero cost, and would let the O17 question go to counsel as a residual rather
  than as a whole.
- **LA-10** — read §4151.0021 and §4151.0022 before anything else in Group 1.
- **LA-09** — the agency-license sub-question.
- **Every citation in this document** — verify against primary text before any of it is quoted to
  counsel or into a plan document.

**The shape of the eventual engagement:** Group 2 as one package (it is one subject), Group 1 possibly
as a regulator letter rather than an opinion, Groups 3 and 4 deferred until the scope line is actually
crossed. That is a materially smaller engagement than "review our ICHRA product," which is the
question that gets asked when no register exists.

---

## How to use this register

**New compliance-relevant design decisions get an LA number at the time they are made, not
retroactively.** The value of this document is that the assumption and the decision are recorded
together, by the person who made both, while the reasoning is still available. A register assembled
after the fact records what someone later reconstructed, which is a different and much weaker
artifact — LA-01's two-week gap between an answer in one doc and an open question in another is
exactly what that failure looks like.

**A decision needs an LA number when it (a) rests on a reading of a statute, regulation or agency
guidance, and (b) would be expensive or embarrassing to reverse.** Both, not either. Reading a
regulation to pick a field length is not a legal assumption. Choosing an audience for an affordability
figure is.

**Entries move to Confirmed or Disproven with a date when counsel eventually reviews them. Nothing is
deleted.** A Disproven entry is more valuable than a Confirmed one — it explains why the code looks
the way it does, and it stops the next reader from re-deriving the original wrong answer from the same
sources.

**Where an entry conflicts with `domain_and_compliance_rules.md`, that file governs for settled
rules and this file governs for decisions under uncertainty.** The two are not competing: settled
rules are things nobody is guessing about, and everything here is a guess with its reasoning attached.
When an entry here becomes Confirmed, the rule it implies belongs in that file, and this entry stays
as provenance.

**Do not cite this document as authority.** Not to a client, not to a partner, not in a plan document,
not in a proposal. It is an internal record of what SSA is assuming, written by people who are not
lawyers, from sources they have not verified against primary text.

---

## Related

- **The settled rules:** `docs/analysis/domain_and_compliance_rules.md`
- **Strategy and the scope line:** `docs/ichra_strategy.md` (§7 compliance boundaries, §8 out of scope)
- **Product decisions D1–D17:** `docs/business/plus_tier.md`
- **Build mechanics, D18–D37, O1–O40:** `docs/analysis/plus_tier_build_plan.md`
- **The attestation design LA-01/02/03 govern:** `docs/analysis/qsehra_attestation_claims_engine.md`
- **The data surface LA-05 and LA-12 rest on:** `docs/business/healthsherpa.md`
- **The relationship and the TPA parking:** `docs/business/swbd_premiumpath.md`
- **Correctness dependencies:** `docs/analysis/project_backlog.md` (T44, T47, T48)
