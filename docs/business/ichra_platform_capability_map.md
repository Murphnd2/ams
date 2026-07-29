# ICHRA/QSEHRA Platform Capability Map — What SSA Can Build for GAs and Agents

**Created:** 2026-07-29
**Status:** Product thinking. **Nothing here is approved for build.**

## Thesis

The individual-policy market's structural failure — named by HealthSherpa in its own materials — is
that **the work scales with employee count**, unlike group coverage. A general agency with 40 ICHRA
groups averaging 15 employees is tracking 600 individual policies: 600 effectuations, 600 payment
statuses, 600 renewal dates, with no book-level view of any of it.

**SSA's opportunity: group-like visibility and control over a book of individual policies.** Every
layer below is a facet of that.

## Layer 1 — Sales and modeling tools (ungated, highest leverage)

**Buildable today.** Quoting and APTC estimation are free, self-serve, and already proven at rate
parity. No approval, no BAA, no PHI.

- **Group-to-ICHRA conversion analysis.** Census plus current group premium in; employer cost,
  per-employee impact, and net position out. The agent already knows the group premium; SSA supplies
  the individual-market side.
- **Class optimization.** ICHRA permits classes (QSEHRA forbids them). Which class structure minimizes
  employer cost while keeping employees whole is a genuine optimization problem, and HealthSherpa
  names it as an intended use case for these endpoints.
- **Affordability threshold modeling.** Per employee, the contribution level at which the offer flips
  from unaffordable (PTC preserved) to affordable (PTC lost). A curve per person, and the most
  misunderstood mechanic in ICHRA design. Presenting it clearly is a differentiator by itself.
- **Employee-level illustration** — what this person pays, and what they can actually buy in their
  county.

**These are sales assets, not administration.** A GA cares more about winning cases than about who
does the paperwork. **This layer is what makes SSA interesting to an agency rather than merely
useful** — and it is the layer with the fewest dependencies.

## Layer 2 — Proposal and sold case (mostly exists)

Built: white-label proposals, per-agency domains, agent markup, GA/sub-agency hierarchy (V066–V071).

**Missing:** ICHRA/QSEHRA reference data and checklists. Phase A confirmed no `LOS` row, no dedicated
`ServiceItem`, and no ICHRA-specific task sequence — **a sold ICHRA case today would inherit the
generic HRA checklist with zero ICHRA compliance steps.** See
`docs/analysis/phase_a_ichra_enrollment_portal.md` Q4.

Then plan documents, adoption agreement, and the ⚖️ 90-day notice with its tracking — deliverables
that exist for QSEHRA and need ICHRA variants.

## Layer 3 — Enrollment

Shopping UI, census-gated access, deeplink handoff. Available for every carrier in the support matrix
including BCBS TX and CHRISTUS.

**Open question shaping this layer:** whether the deeplink supports employee self-service or assumes
an agent drives it. **The answer determines whether this is an employee portal or an agent
workstation.** See `docs/business/healthsherpa.md`.

**Design constraint (⚖️, non-negotiable):** neutral, complete plan presentation. No curation, no
"recommended" badge, no default selection, no hidden carriers. Two independent reasons — the ERISA
safe harbor requires the employer not endorse any particular issuer or plan, and SSA holds no
licensure to advise on plan selection. Route plan-choice questions to the licensed agent.

## Layer 4 — Administration (recurring revenue)

Coverage verification by webhook where the carrier supports it, **manual fallback where it does not —
build the fallback as a first-class path, not an afterthought.** Reimbursement processing, which has
**no rail in AMS today** (backlog #38). Employer billing and agency remittance, which exist. Then
⚖️ 1095-B and ⚖️ PCORI.

## Layer 5 — The GA console (the actual differentiator)

**This is where AMS's existing bones matter most.** AMS is already a book-of-business and
activity-tracking system — `Activity`, `Setup`, `Renewal`, `Ticket`, employer lists, billing grids.
Pointed at individual policies, that yields:

- Book view across all groups and all employees, with enrollment and coverage state
- Downline visibility — which sub-agency, which agent, which cases
- **Attribution reporting keyed on the NPN** HealthSherpa returns per policy
- **Exception alerts** — SEP window closing, application submitted but never effectuated, policy
  terminated mid-year, renewal notice due in 90 days

**The alert engine is the real operational value.** It is the difference between a book that runs
itself and one that generates surprises, and it is precisely what a GA cannot get from a carrier
portal or a spreadsheet.

## What the agent gets

1. **Commission on the individual policies, preserved** — AOR travels per application by NPN, so SSA
   is structurally not competing for the policy.
2. **Admin margin** — via the agent markup mechanism already built (V066/V067).
3. **A tool that helps them win cases** they would otherwise lose to a group renewal.

## Constraints to design around, not discover

- ⚖️ **ICHRA and QSEHRA are mutually exclusive at the employer level.** QSEHRA requires the employer
  offer no other group health plan, and an ICHRA **is** a group health plan. Not a per-class menu.
- ⚖️ **No plan advice, no curation** (Layers 1 and 3). ERISA safe harbor plus absence of licensure.
- **Subsidy-eligible participants need on-exchange**, which is unresolved. See the segmentation
  section in `ichra_administration_scope.md`.
- **Off-exchange coverage status is carrier-gated.** Metro Texas is covered; rural Texas is not.
- **Substantiation must verify coverage *type*, not just presence** — non-MEC off-exchange products
  exist and reimbursements against them are taxable.

## Assessment

**Layers 1 and 5 are the defensible ones.** Layers 2 through 4 are table stakes that several vendors
do adequately.

## Strategic question this raises

**Is SSA becoming SWBD's administrator, or building a platform where SWBD is the first agency?**

The existing infrastructure already answers it — per-agency domains, agent markup, GA hierarchy are a
multi-tenant platform, not a single-client build. **Which makes SWBD a design partner and reference
account rather than the destination**, and should shape what gets conceded: no exclusivity, no bespoke
build, and pricing that works at twenty agencies rather than one.
