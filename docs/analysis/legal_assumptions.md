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
