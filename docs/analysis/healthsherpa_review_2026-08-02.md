# HealthSherpa / ICHRA+ — Research Review, 2026-08-02

**Status:** ⚠️ **FOR REVIEW — nothing in this document has been applied to any other doc or to code.**
**Method:** repo docs + live HealthSherpa documentation + three live staging API calls + secondary
regulatory sources.
**Scope:** the ICHRA Partner API (`api.ichra.healthsherpa.com`), not HSOne, not EDE.

**Confidence markers used throughout:**

- ✅ **VERIFIED** — observed directly in a live API response this session
- 📄 **DOCUMENTED** — read from HealthSherpa's live documentation
- 📚 **SECONDARY** — regulatory/industry sources, not primary law. Counsel-gated before client use
- ❓ **UNRESOLVED** — asked and not answered, or absent from the docs

---

## 1. The urgent item

**`rating_area_rate_cache.lcsp_premium` and `.benchmark_silver_premium` are wrong, in the direction
that makes ICHRA offers look affordable when they are not.**

`RateCacheWarmService` quotes with `off_ex: true`. ICHRA affordability requires the **on-Exchange**
LCSP. Measured live this session, Hopkins County TX (75482 / 48223), age 40, self-only, PY2026:

| | Silver plans | LCSP | SLCSP |
|---|---|---|---|
| Off-exchange (what the cache holds) | 27 | **$489.38** | $503.84 |
| On-exchange (what affordability requires) | 14 | **$705.37** | $725.13 |

✅ VERIFIED. **The cache understates LCSP by $215.99/month — 31%.**

**Consequence.** With a $400/mo ICHRA at the 2026 threshold (9.96% 📚):

- cached off-ex LCSP → required contribution $89.38/mo → affordable above ~**$10,800** income
- true on-ex LCSP → required contribution $305.37/mo → affordable above ~**$36,800** income

Every employee between roughly **$10,800 and $36,800 — about 100%–235% FPL — is classified
backwards.** They would be told the offer is affordable and their PTC is lost, when in fact it is
unaffordable and they keep it. That band is the core of the subsidy-eligible population, i.e. the
entire PremiumPath thesis. For an ALE the same error reads as "penalty-safe" when it is not.

**Likely mechanism — and why this is probably not local to Hopkins.** The gap is silver-specific and
one-directional, which is the signature of **silver loading** (insurers inflate on-exchange silver to
fund CSRs; off-exchange silver carries no such load). If so it is structural across FFM markets.
**Not yet confirmed — a second-county test would settle it.**

**Remediation.** Recompute `lcsp_premium` / `benchmark_silver_premium` with `off_ex: false`. Market
illustration figures (low/high premium, carrier count, plan count) are legitimately off-exchange and
should not move. ⚠️ **Regression risk:** see §2.3 — the catastrophic filter must be applied on the
on-exchange path too, or `market_low_premium` breaks again.

**Nothing client-facing should use the current cached LCSP values.**

---

## 2. Live API findings (staging, 2026-08-02)

Three read-only `POST /api/v1/quotes` calls, production key pointed at
`https://api.ichra-staging.healthsherpa.com`. No enrollment, no PHI, no writes.

### 2.1 `meta.result_count` is a PAGE count, not a total ✅

| Call | Plans returned | `meta.result_count` |
|---|---|---|
| default `per_page` | 20 | **20** |
| `per_page: 100` | 65 | **65** |

The OpenAPI schema describes it as *"total number of results."* **That description is wrong.**

There is no cheap truncation check. Completeness can still only be established by paginating to a
short page — the 2026-07-31 finding and the `eba17ad` fix both stand unchanged. `per_page` maxes at
**500** 📄, not 100, so the pagination loop could be widened if worthwhile.

The run also reproduced the 2026-07-31 baseline exactly — 65 plans, BCBS 24 / CHRISTUS 18 / UHC 23,
Silver 27 / Gold 19 / Expanded Bronze 15 / Bronze 4. **Staging is stable and comparable across runs.**

### 2.2 Premium numeric type — settled ✅

Open since 2026-07-28 (*"Confirm from a raw response dump before writing a Java deserializer"*).
`gross_premium` deserializes as a **JSON number** (`371.97` → Double), not a string. Use `BigDecimal`
from a numeric node.

### 2.3 ⭐ The plan set is not age-invariant *the same way* on both exchanges ✅ — NEW

The 2026-07-31 finding — catastrophic plans restricted to under-30, so age 40 returns none — was
measured **off-exchange**. On-exchange, **age 40 returned 2 Catastrophic plans.**

`RateCacheWarmService`'s catastrophic filter for ages 30+ was calibrated against off-exchange
behavior. **The moment `off_ex` flips to false for the LCSP fix, catastrophic plans reappear at age
40 and `market_low_premium` is exposed again unless the filter is applied on that path.** This is a
live regression risk inside the §1 remediation, not a theoretical one.

### 2.4 On-exchange plan count is 47, not 45 ✅

The 45 figure recorded 2026-07-28 was HSOne's. On-exchange PY2026 Hopkins: 47 plans — BCBS 19 /
CHRISTUS 14 / UHC 14; Silver 14 / Gold 16 / Expanded Bronze 12 / Bronze 3 / Catastrophic 2.

### 2.5 Spot-check needing follow-up ❓

`ehb_premium` returned `0` on one on-exchange plan. Since `ehb_premium` is flagged in
`healthsherpa.md` as relevant to HRA substantiation, this needs a proper look. **One row is not a
finding** — recorded so it is not lost.

---

## 3. Product-shape resolutions

### 3.1 There are two API products, not three 📄

The working mental model of "on-exchange API / off-exchange API / ICHRA API" cuts on the wrong axis.

| | HealthSherpa One (HSOne) | ICHRA Partner API |
|---|---|---|
| Base | `api.one.healthsherpa.com` | `api.ichra.healthsherpa.com` |
| Access | self-serve, instant key | keys **issued** |
| Role | simplified subset | **the target product** |

**EDE is not a third API.** It is a CMS regulatory status plus HealthSherpa's own marketplace
platform. There is nothing to integrate against.

**On-exchange vs off-exchange is a request parameter, not a product.** `off_ex` is a boolean on
`POST /api/v1/quotes`, default `false`. Same endpoint, same key. 📄 *"Quoting supports on-ex and
off-ex."*

### 3.2 ✅ RESOLVED — the Julian contradiction

`healthsherpa.md` (2026-07-29, open item 8) records: Julian said these APIs are *"entirely
off-exchange"* while the docs describe on-exchange quoting **and** enrollment. Marked **"Ask; do not
infer."**

**Resolved without needing to ask.** Both statements are right about different halves:

- **QuoteConnect** — 📄 *"for on- and off-exchange markets"*, plus APTC estimation
- **EnrollConnect / Deeplink** — 📄 *"off-exchange applications"*

Quoting is both exchanges. **Enrollment is off-exchange only.** Julian was describing the enrollment
rail. No permission gate is involved, and **D16 (off-exchange only at launch) is unaffected** — it
was already correct, for reasons that still hold.

### 3.3 ✅ RESOLVED — O14, deeplink self-service vs agent-completed

Open since 2026-07-29, *"Asked, not yet answered."* The Use Cases page states 📄:

> "The agent **or** employee completes enrollment in HealthSherpa's UI."

**Both are supported.** HealthSherpa additionally documents a **"Full Experience"** use case matching
the SWBD scenario exactly: *the agent defines strategy, the employee selects and enrolls.*

**Consequence for B5:** the employee-portal-vs-agent-workstation question dissolves. The same deeplink
serves both; who receives it becomes per-group configuration, not architecture. **This removes a gate
from B5.**

### 3.4 Enrollment path selection is per-plan, not a platform election 📄

`healthsherpa.md` frames Deeplink vs EnrollConnect as a strategic choice by SSA with opposite PHI
consequences. That framing is still useful, but HealthSherpa's documented routing is mechanical:

1. `api_enrollment: true` → EnrollConnect
2. else `deeplink_enrollment: true` → Deeplink
3. both false → quote-only

📄 *"Both accept the same canonical request schema. Build your payload once, then route it."*

Deeplink is ✅ live for **every** carrier in the matrix, so SSA can still elect deeplink-everywhere and
keep PHI out. HealthSherpa steers toward EnrollConnect; the flags describe capability, not obligation.
**The PHI argument for deeplink survives — it just isn't the vendor's default recommendation.**

---

## 4. Live-doc drift vs. repo docs

Carrier matrix in repo is dated **2026-05-19**; live page reads **2026-07-30**. Changelog has entries
through **2026-07-29**.

| Item | Repo says | Live docs say |
|---|---|---|
| BCBS TX EnrollConnect | ☑️ coming 2026 | ✅ **live** (HCSC IL/MT/NM/OK/TX, 2026-07-16) |
| UHC off-exchange enrollment | "genuine carrier-level gap" | ✅ **live across the board** (that was HSOne's `api_enrollable`) |
| `_agent_id` on deeplink | required | **required** — see §7.1, my correction was the error |
| `per_page` | paginate at 100 | max **500** |
| Error handling | two envelopes, branch on body shape | **422 for all validation errors** (2026-07-06) — two-envelope model was HSOne's |
| EnrollConnect exclusions | NJ, NY | NJ, NY + 📄 *"all carriers and all states expected prior to OEP PY2027"* |
| On-exchange plan count | 45 | 47 ✅ |

**Absent from repo docs entirely:**

- **Deeplinks V2** (targeted May 2026) — full canonical prefill incl. attestations, signatures, agent
  of record, employer info; backward compatible
- **In-flow ACH payment** — Anthem/Wellpoint, 2026-07-29, `payment_type: "initial" | "both"`
- **Post-enrollment changes** for submitted applications (2026-05-18)
- **Application Events Timeline** — `GET /applications/:id` audit trail, field-level diffs, automatic
  PII redaction (2026-05-18)
- **AI Agent Skill** published for Claude Code (2026-06-10)
- **Carrier Effective Date Logic** page — see §4.1

### 4.1 The effective-date trap is shallower than recorded

Repo records: *"`desired_effective_date` returns 422 if invalid… **There is no endpoint to query valid
dates ahead of time.**"* Still true about the endpoint — but HealthSherpa now publishes 📄 **seven
distinct effective-date patterns by carrier and state**:

- most: 1st of month following application
- CareSource (GA/IN/NC/OH/WV) and MedMutual (OH): 1st–15th → next month; 16th+ → month after next
- Anthem, Wellpoint, BCBS variants, Antidote, Sanford: QLE-dependent
- CareSource WI: past vs future QLE; Highmark: later of today/QLE, then 1st of month

**Implementable client-side.** Goes from "unpredictable 422s" to "encode seven rules." Still build the
retry UX. **Narrows the B5 constraint materially.**

### 4.2 What did NOT change — do not overclaim

**Policy Status for the launch market is unchanged.** BCBS TX still ☑️ 2026; **CHRISTUS still blank**.
The Hopkins County conclusion holds exactly as written: enrollment works, automated coverage
verification does not, `ATTESTATION` stays primary, **D2 and D17 stand.**

What moved is the **enrollment** column, not the **verification** column. These must not be conflated.

Two additions:
- New matrix legend value 🟨 = *"Live Policy Status, no Payment Info"* (Cigna, Oscar). The D17 ladder
  does not distinguish this state.
- The PY2027 language above bears directly on the 2026-vs-2027 launch question (**O16**).

---

## 5. Regulatory findings 📚

**All of §5 is secondary-source synthesis. None of it is counsel-cleared. It belongs on the O21 docket
before it drives a client-facing number.**

### 5.1 The 400% FPL cliff is back for 2026 — and it is a sales argument

Enhanced subsidies (ARPA, extended by IRA) eliminated the cliff **2021–2025** and expired after 2025.
For PY2026 the hard cliff returns, no subsidy above 400% FPL, expected-contribution rising to as much
as **9.96%**.

Households 400–500% FPL — 3% of 2025 signups — accounted for **27% of the 2025→2026 signup drop**;
that group's enrollment fell **44%**. **Those people just became strong ICHRA candidates, this plan
year.**

⚠️ **Congress could restore the enhancements.** The cliff and the percentage must be **configurable
parameters**, never constants in a calculator.

### 5.2 Two different affordability questions — the trap

> IRS: *"The employer shared responsibility affordability safe harbors do not affect whether an
> employee's coverage is affordable for purposes of determining the employee's eligibility for the
> premium tax credit."*

| | Basis | Safe harbors? |
|---|---|---|
| **Employer** (§4980H penalty) | W-2 / rate of pay / FPL | ✅ yes |
| **Employee** (PTC eligibility) | **actual household income** | ❌ no |

**§4980H applies only to ALEs (50+ FTE).** SWBD's book is small groups with no employer-mandate
exposure, so the safe harbors protect against a penalty that does not exist for them. For those
groups the entire question is employee-side, which the safe harbors cannot answer.

**FPL points the wrong way for PremiumPath.** An offer unaffordable at FPL can still be *affordable*
for a higher earner (bigger income → bigger 9.96% threshold). Using FPL to assert PTC preservation
would be wrong for exactly the better-paid employees.

### 5.3 LCSP vs SLCSP — a schema-relevant distinction

| Test | Benchmark |
|---|---|
| **ICHRA** affordability | **LCSP** — lowest-cost silver, self-only |
| **QSEHRA** affordability | **SLCSP** — *second*-lowest-cost silver, self-only |
| APTC benchmark | SLCSP |

`rating_area_rate_cache` stores both. They are **not interchangeable**. Verify the illustration code
does not treat `benchmark_silver_premium` as a rename of `lcsp_premium`.

### 5.4 ICHRA is either/or; QSEHRA stacks — the segmentation thesis in one line

| Scenario | PTC | HRA | Net |
|---|---|---|---|
| QSEHRA, unaffordable | PTC − permitted benefit | keeps it | ≈ full PTC from two sources |
| QSEHRA, **affordable** | **none** | keeps it | HRA only |
| ICHRA, enrolled | none | keeps it | HRA only |
| ICHRA, unaffordable + opt out | full PTC | **none** | PTC only |
| **ICHRA, affordable + opt out** | **none** | **none** | **nothing** |

Two corrections to common framings:

- **"Unaffordable ICHRA → opt out" is wrong as advice.** Unaffordable means the employee gets a
  *choice*. A generous-but-unaffordable ICHRA can beat a small PTC. The output is a **dollar
  comparison**, not an instruction.
- **"QSEHRA has no consequence regardless" is wrong.** True only while unaffordable. PremiumPath's
  $50/mo de minimis never approaches the line, but the rule is not general — if the benefit ever
  scales, PTC is lost silently.

**Last row is the design hazard.** An employee who opts out of an *affordable* ICHRA gets neither. The
waiver artifact (C1 item 1) should carry that warning computed for that employee, not just a
signature block.

### 5.5 Four employee groups — only one needs analysis

| Group | PTC? | ICHRA verdict |
|---|---|---|
| Under 100% FPL in TX (non-expansion → coverage gap) | none | pure upside |
| **100–400% FPL** | **yes** | **the analysis band** |
| Over 400% FPL (cliff back) | none | pure upside |
| **Medicare-eligible** | **none, regardless of affordability or opt-out** | pure upside |

The Medicare row is a **required ICHRA notice element**. ⚠️ HealthSherpa does not quote Medicare, so
that group is unpriced by the illustration even though its answer is trivial.

**This is a better employer conversation than "it depends"** — for most of a workforce it is simple.

### 5.6 ICHRA classes are a closed list — constrains A3 #3

Permitted: full-time/part-time · salaried/hourly · seasonal · temporary staffing-firm · unionized ·
**same insurance rating area** · waiting period · non-resident aliens with no US-source income · and
combinations.

**Income and subsidy eligibility are not on the list.** You cannot build classes that sort employees
by whether they would keep a credit — the most natural way to implement a contribution strategy, and
unavailable. **Rating area is the one geographic lever.** This must be compiled into A3 #3, not
checked afterward.

### 5.7 Affordability is household-to-household — a product boundary

- **Premium side is individual:** LCSP self-only, that employee's age and location
- **Income side is household:** MAGI of employee + spouse + filing dependents; household size sets the
  FPL band

Neither is knowable by the employer. **AMS can never *determine* affordability — only model it.** Even
the employee cannot: PTC uses *projected* MAGI, reconciled on Form 8962 at tax time.

**The honest output is a breakeven, not a verdict:** *"above roughly $X of household income for a
family of Y, this offer becomes affordable and you lose the credit."* One curve, parameterized by
household size, from age and location.

That is **A3 #2 almost verbatim** — the existing design already has the right shape. It also stays
clear of individualized tax advice.

### 5.8 ⭐ CMS publishes an authoritative LCSP table — a second data source

CMS/CCIIO publishes the **ICHRA Employer LCSP Premium Look-up Table**: free, no API key, by geography
and age, for **FFE and SBE-FP states** (Texas qualifies). A PY2026 table was published Dec 2025.

| | CMS LCSP table | HealthSherpa quote |
|---|---|---|
| Job | the **compliance** number | the **illustration** |
| Authority | what an auditor checks | vendor rate data |
| Cost | free, static file | needs a configured key |
| Exchange | **on-exchange by construction** | whatever `off_ex` says |
| Coverage | FFE + SBE-FP only | all 50 states incl. SBMs |

⭐ **It is on-exchange by construction, so it would have made the §1 defect impossible.** That elevates
it from "a free alternative" to "the more defensible source for anything compliance-facing."

Loading it is the same shape of work as the `zip_county` crosswalk (V085 precedent), and it would let
the affordability half of A1/A3 ship **without** D-78/D-79 or any HealthSherpa key.

**SBM states (CA, NY, CO, WA, MA…) are not in the CMS table** — HealthSherpa on-exchange quoting fills
that gap. The two sources are complementary.

### 5.9 healthcare.gov already does the determination — do not rebuild it

CMS publishes an **HRA affordability tool** taking household size, income and HRA amount. Authoritative
and free.

**So the determination is commodity.** The differentiated product is what CMS does *not* do:

1. **The comparison** — CMS says affordable/not; it never says whether the HRA beats the forgone PTC
2. **The inverse** — the employer asks "what should I contribute?", not "is $400 affordable?"
3. **Timing** — CMS runs at application; the decision happens months earlier at design
4. **Aggregate** — no census view, no agent visibility

**Pointing employees at CMS's own tool for the determination is a credibility move and lowers
exposure.**

---

## 6. Enrollment leg — capability and constraints

### 6.1 The agent's economics are the reason they stay engaged

When a group drops its plan, group commission disappears; individual-policy commission follows **agent
of record**. AOR travels **per application, by NPN** (webhook: `policy_aor_npn`, `submitter_npn`,
`npn_used_at_submission`, plus a per-policy `agent_of_record` object). **SSA never competes with the
agency's agents for the policy** — structurally important to the partnership.

### 6.2 Two identifiers, not one 📄

| | Job |
|---|---|
| **`_agent_id`** | HealthSherpa slug; routes the application to that agent's **HealthSherpa dashboard**. **Required on Deeplink**, optional/nullable on EnrollConnect |
| **NPN** | drives **AOR and commission** |
| `agent_of_record_tin` | Anthem/Wellpoint commission tracking (added 2025-11-06) |

📄 FAQ: `_agent_id` is *"separate from commission-related agent fields."* Passing it yields visibility,
not necessarily commission. **Both will likely be needed, set separately.**

### 6.3 ⚠️ AMS has no NPN field — verified gap

Grepped `src/`: **zero matches** for `NPN`, producer number, or license number. Every `agentId` hit is
an internal `Person` id. There is also no field for a HealthSherpa `_agent_id`.

**The enrollment leg needs an agent-identity addition that does not exist and is not in the current
schema plan** — NPN, `_agent_id`, probably TIN — on the agent record, resolved through the existing
`AgencyScopeResolver` / originating-agency chain so a per-employee deeplink picks up the right agent.
Small migration; a hard prerequisite for **B5**.

### 6.4 Allowlisting is lighter than assumed 📄

Passing a valid API key *"will automatically allowlist any valid `_agent_id`… allowing agents not
associated with an agency to proceed."* **No per-agent HealthSherpa approval queue.** The friction is
account creation, not approval.

### 6.5 ❓ Appointment filtering — unresolved, and my read is "no"

**Question:** does the deeplink filter plans by the agent's carrier appointments?

**The docs do not cover it.** Confirmed by querying HealthSherpa's own `?ask=` documentation endpoint:
*"If you need carrier-appointment-specific behavior, it isn't described in the docs I can access."*

**Evidence pointing away from filtering:**

- ✅ The quote API takes **no agent parameter at all** — the full 65/47 plan sets came back today with
  none supplied. The shopping layer is definitively unfiltered.
- 📄 `deeplink_enrollment` is a **plan-level** flag *"determined by plan-level flags returned from
  QuoteConnect"* — plan capability, not agent credential.
- 📄 The only documented restriction gates **the agent proceeding**, not which plans appear.
- Filtering a consumer's choices by their agent's appointments would be significant, visible behavior
  to leave undocumented.

**Likelier mechanism: just-in-time appointment**, common in individual ACA business. ⚠️ HealthSherpa's
doc tool suggested "how to test just-in-time appointment flow" — but that echoes wording from the
query itself, so **it is not evidence.**

**Design conclusion regardless: do not build appointment logic.** SSA has no appointment data and no
business owning it. If carriers gate, HealthSherpa or the carrier enforces and SSA surfaces the error.

**Residual risk is relational, not technical:** an unappointed agent enrolls successfully, no
commission attaches, and it reaches Forrest as *"your platform didn't pay me."*

### 6.6 Where SSA's value actually sits

**HealthSherpa does the enrollment.** On the deeplink path they collect SSN, immigration status,
incarceration status, attestations and signatures. SSA transmits prefill demographics and redirects.
**Framing the integration as "we enroll people" builds the wrong thing and takes on avoidable PHI.**

**Only SSA knows the denominator:**

- the agent knows who they talked to
- HealthSherpa knows who applied
- the carrier knows who paid
- **only SSA knows who was *eligible*** — it holds the census and contribution schedule

So only SSA can say *"3 of your 12 eligible employees haven't enrolled and the SEP closes in 9 days."*
That chase list is the labor forcing competitors into per-group minimums, and it reuses the
verification-ledger machinery.

**Pitch:** not "we enroll your clients' employees" but **"your agents will never lose an employee in
the gap between the sale and the effective date."**

### 6.7 Gating — mostly not code

| | |
|---|---|
| Production key + enrollment access | account manager |
| **Each agent needs a HealthSherpa account** | per-agent onboarding — **Forrest's work, and the long pole** |
| **Individual-market carrier appointments** | ⚠️ group appointment ≠ individual appointment. **Ask Forrest** |
| `_agent_id` production allowlisting | auto, if the API key is passed (§6.4) |
| **BAA** | deeplink shrinks the PHI surface to prefill + webhook data; **does not eliminate it** |
| Webhook config form + public authenticated endpoint | `/api/*` behind `ApiTokenFilter` |

---

## 7. Access status

### 7.1 Production is not enabled

Production returns **403**; the key works against staging only. Separate keys are issued per
environment 📄.

**Mitigating:** staging returns **real rate data** (confirmed 2026-07-30 and again today). Fine for
building and for demoing to Forrest. **Not** fine for anything a paying client relies on — no SLA, and
open item #6 already flags client-facing output off a preview product as a different risk posture.

### 7.2 Two separate asks — currently conflated in the docs

| Ask | Requires |
|---|---|
| **Production quoting key** | ask the account manager after staging validation. No BAA, no enrollment approval, no PHI |
| **Enrollment** | account manager + `_agent_id` allowlisting + webhook form + **BAA** |

**The entire sales side — A1, A2, A3 — needs only the first.** Current docs treat "HealthSherpa
access" as one thing gated on O12/O13. It is not.

### 7.3 No record of a production request

The repo shows:

- **2026-07-28** — off-exchange enrollment request submitted **via the HSOne portal** (different
  product, kept as fallback)
- **2026-07-29** — seven questions emailed to Julian
- **2026-07-30** — ICHRA Partner API key issued; staging works, production 403. The doc's note that
  this is *"consistent with the documented onboarding flow"* is an **inference about the process, not
  a record of a request.**

**Nothing after that records a production request.** Absence from the docs is not proof none was made
— only that it was not logged.

### 7.4 ⚠️ A stale blocker in our own docs

Both `healthsherpa.md` and `claude_memory.md` still read *"No onboarding representative or account
manager has been assigned… Nothing is testable until one is."* Written **2026-07-29**. A staging key
arrived **2026-07-30**.

So either a rep was assigned and never recorded, or KJ/Julian issued it directly. **O12 is described
as "the tightest bottleneck on the HealthSherpa track" and may have quietly cleared four days ago.**

### 7.5 New contact 📄

`ichrasupport@healthsherpa.com` — the documented intake address. Not in our contact list, which has
only Julian, KJ, and Michael Levin.

---

## 8. Planned doc updates — NOT YET APPLIED

### `docs/business/healthsherpa.md`

New dated section **2026-08-02**, following the existing supersession convention (retain superseded
text, do not delete):

1. **The LCSP defect** (§1) with the measured table and the misclassification band
2. **`result_count` is a page count** — schema description wrong; 07-31 finding and `eba17ad` stand
3. **Catastrophic differs by exchange** (§2.3) — amends the 07-31 age-invariance finding again
4. **Premium type settled** — JSON number → `BigDecimal`
5. **Julian contradiction RESOLVED** (§3.2) — mark open item 8 resolved
6. **O14 RESOLVED** (§3.3) — agent *or* employee
7. **Live-doc drift table** (§4) — and update the carrier matrix to the 2026-07-30 version
8. **Effective-date trap narrowed** (§4.1) — seven published patterns
9. **Deeplinks V2 / in-flow ACH / post-enrollment changes / events timeline / AI skill** — new
   capabilities absent from the file
10. **§4.2 explicitly** — Policy Status for BCBS TX / CHRISTUS is UNCHANGED. Guard against
    misreading the enrollment-column improvement
11. **Access section** (§7) — two asks not one; no production request on record; §7.4 stale blocker;
    `ichrasupport@` added to contacts
12. **`_agent_id` is required on Deeplink, optional on EnrollConnect** (§6.2)

### `docs/business/plus_tier.md`

1. **D17 unaffected but sharpen** — add the 🟨 "policy status, no payment info" state to the ladder
2. **New decision (D38?)** — CMS LCSP table as the compliance source; HealthSherpa as the illustration
   source; SBM states fall back to HealthSherpa
3. **New decision (D39?)** — affordability output is a **breakeven curve, not a verdict**; point
   employees at CMS's own tool for determination
4. **LCSP vs SLCSP** (§5.3) into the data-design section against `rating_area_rate_cache`
5. **The four-bucket segmentation** (§5.5) into the Quote-stage requirements
6. **Agent identity fields** (§6.3) into "Existing-model additions" — NPN, `_agent_id`, TIN
7. **D15 note** — the ICHRA-either/or vs QSEHRA-stacks table (§5.4) is its clearest statement

### `docs/analysis/plus_tier_build_plan.md`

1. **O14 → RESOLVED**; remove the gate from B5
2. **O16** — add the 📄 *"all carriers and all states prior to OEP PY2027"* signal
3. **A1 scope** — the LCSP fix and its catastrophic-filter regression risk
4. **A3 #3** — compile the closed class list (§5.6) into scope as a constraint
5. **B5** — agent-identity prerequisite (§6.3); effective-date logic now implementable (§4.1)
6. **New open items** — §9 below

### `docs/deployment_backlog.md`

- **D-78 / D-79** — narrow the "unapplied everywhere" claim: production holds a key pointed at
  staging. Determine and record what demo/master/BPO actually hold.
- **D-82** — unchanged and still correct.

### `docs/claude_memory.md`

- Current State: the LCSP defect as an active known issue
- Correct the §7.4 stale onboarding-rep blocker
- ICHRA section: O14 resolved, Julian contradiction resolved

### Code — separate change, not a doc update

- `RateCacheWarmService` — `off_ex: false` for LCSP/SLCSP; catastrophic filter on that path;
  re-warm all cached rows
- `HealthSherpaService` — `BigDecimal` from numeric node
- ⚠️ **Existing cached rows must be invalidated, not merely overwritten going forward**

---

## 9. New open items surfaced

Numbering picks up after **O40** (build plan Part 8). ⚠️ **Verify against the live file before
filing.** T-numbers to be assigned from the live backlog.

### Empirical — self-answerable with the staging key

| # | Item | Why it matters |
|---|---|---|
| **O41** | **Is the LCSP on/off-exchange gap silver loading?** Re-run §1 in a second county (ideally metro TX + a non-TX FFM state). | Determines whether the §1 defect is systemic or Hopkins-local. Changes the size of the fix. |
| **O42** | **`ehb_premium` returning 0** (§2.5) — real, or staging artifact? | `ehb_premium` is flagged for HRA substantiation. |
| **O43** | **Does the catastrophic-plan set differ by exchange at other ages?** (§2.3) | The age-curve findings have now been amended twice; establish the boundary properly. |

### Vendor — ask HealthSherpa

| # | Item | Why it matters |
|---|---|---|
| **O44** | **Does submission require prior carrier appointment, is it just-in-time, and what is the failure mode for an unappointed NPN?** (§6.5) | Determines whether an agent can enroll and go unpaid. Relational risk with Forrest. |
| **O45** | **`_agent_id` vs NPN vs `agent_of_record_tin`** — which fields must be set, on which path, for AOR to attach? (§6.2) | The commission chain. Currently inferred from three doc fragments. |
| **O46** | **Production quoting key** — is a request on record, and what is outstanding? (§7.3) | Gates every client-facing use of Track A. |
| **O47** | **Is an onboarding rep/account manager assigned?** (§7.4) | O12 may have cleared 2026-07-30 and gone unrecorded. |
| **O48** | **Deeplinks V2 status** — shipped? Does full-canonical prefill change the B5 build? (§4) | Targeted May 2026, absent from our docs. |

### Counsel — add to the O21 docket

| # | Item | Why it matters |
|---|---|---|
| **O49** | **ALE vs non-ALE affordability split.** Safe harbors protect ALEs from a penalty; they do not answer the employee's PTC question, and SWBD's book is non-ALE. (§5.2) | Not stated in either doc today. Determines whether one tool or two. |
| **O50** | **Confirm LCSP (ICHRA) vs SLCSP (QSEHRA)** benchmarks. (§5.3) | Direct schema consequence. |
| **O51** | **Location safe harbor** — our quote input is worksite ZIP (D6) while the base rule keys on residence. Are we relying on the safe harbor, and is that intended? (§5.7) | Load-bearing for **every** illustration produced. |
| **O52** | **Advice boundary for a TPA.** Licensure + tax advice + the PEPM conflict of interest in advising participants on opt-out. Distinct from O20, which covers plan *display*. (§5.9) | Determines whether the tool can be employee-facing at all. |
| **O53** | **2022 family-glitch regulations** and ICHRA offers to family members — a separate test from the employee's own. | Unverified in this session. |
| **O54** | **Verify the 2026 affordability percentage (9.96%) and FPL figures** against primary sources before any client-facing calculation. | Secondary sources drift; indexed annually. |

### Partnership — ask Forrest

| # | Item | Why it matters |
|---|---|---|
| **O55** | **Do SWBD's agents hold *individual-market* appointments with BCBS TX, CHRISTUS and UHC — or only group?** (§6.5, §6.7) | Group ≠ individual. Could change which carriers are practical for his downline. Independent of what HealthSherpa does. |
| **O56** | **Per-agent HealthSherpa account onboarding** across the downline — who owns it, what is the lead time? (§6.7) | The long pole on the enrollment leg, and it is not SSA's work. |

---

## 10. Corrections I made during this session

Recorded because this file will be read as a source, and three of my own statements were wrong.

1. **"No AMS installation has ever authenticated to this API."** Repeated from the 2026-07-31 note,
   which checked local dev only. **Production holds a key**, pointed at staging. The doc's claim
   needs narrowing, not deleting.
2. **"`_agent_id` is optional — your doc saying 'required' is contradicted."** **Wrong.** It is
   **required on Deeplink**, optional/nullable on EnrollConnect ("nullable for API usage" = the API
   path). The original 2026-07-29 note was correct; my correction was the error.
3. **"The LCSP question's capability half is resolved — just flip `off_ex`."** Understated. Flipping
   it is **mandatory**, the cached values are wrong by 31%, and the fix carries a catastrophic-filter
   regression risk. It moved from an open item to a live data defect.

---

## 11. What is verified vs. what is not

**Verified live this session (✅):** the LCSP gap · `result_count` semantics · premium numeric type ·
on-exchange catastrophic at age 40 · 47 on-exchange plans · the 65-plan baseline reproduced · AMS has
no NPN field.

**Read from live vendor docs (📄):** the two-product structure · quoting both exchanges / enrollment
off-exchange only · O14 · enrollment-path routing · carrier matrix at 2026-07-30 · effective-date
patterns · `per_page` 500 · 422-for-all-validation · `_agent_id` required on Deeplink · auto-allowlist
· `ichrasupport@`.

**Secondary sources, counsel-gated (📚):** everything in §5. The 9.96% figure, FPL amounts, safe-harbor
mechanics, class list, and QSEHRA/ICHRA PTC interactions are all synthesis from law firm, industry and
IRS-summary sources — **not primary law.**

**Unresolved (❓):** appointment filtering (O44) · silver-loading generality (O41) · `ehb_premium` zero
(O42) · whether a production request exists (O46) · whether an onboarding rep is assigned (O47).

---

## Related

- `docs/business/healthsherpa.md` — the vendor file this amends
- `docs/business/plus_tier.md` — D1–D17
- `docs/analysis/plus_tier_build_plan.md` — canonical for D18–D37, O1–O40, the phased build
- `docs/deployment_backlog.md` — D-78, D-79, D-82
- Live docs: `https://docs.ichra.healthsherpa.com/` · index at `/llms.txt` · per-page markdown by
  appending `.md` · natural-language query via `GET <page>.md?ask=`
- CMS ICHRA Employer LCSP Premium Look-up Table — `cms.gov/marketplace/employer-initiatives`
  (⚠️ cms.gov returns 403 to automated fetching; use a browser)
