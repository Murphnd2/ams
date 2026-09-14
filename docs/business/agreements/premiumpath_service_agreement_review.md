# Service Agreement Review — PremiumPath / ICHRA
**Prepared for:** Kevin Murphy, Superior State Administrators, Inc. dba Superior State Employer Solutions ("SSES")
**Reviewed:** `Service_Agreement_-_RPA_-_BAA.pdf` (Master + Tax-Favored Plan Administration Appendix + BAA) and `Service_Agreement_COBRA_and_BAA.pdf` (Master + COBRA Services Appendix + BAA)
**Date:** 2026-09-14
**Not legal advice.** This is a drafting and risk-allocation review against primary sources and against this project's registered assumptions. Items marked ⚠️ **VERIFY** are ones where I am not confident enough for you to rely on without a second source.

---

## 0. Headline findings

| # | Finding | Severity |
|---|---|---|
| 1 | The two PDFs are each a **complete standalone Master Agreement + BAA**. A client buying both FSA/HRA and COBRA signs two masters and two BAAs with no stated relationship. Which BAA governs a breach is genuinely ambiguous. | **High — structural** |
| 2 | The **BAA's indemnity (§7.2.1) is uncapped, includes a defense-and-fees obligation, and §7.4 lets the client control settlement while SSES still indemnifies.** The Master's liability limits (§2.6) do not reach it, and the BAA is incorporated into the Master. This is the single largest exposure in the document set. | **High** |
| 3 | **§3.4 record retention says "the lesser of eight (8) years … or the termination of this Agreement."** As written, retention ends at termination — the opposite of the intent, and short of HIPAA's 6 years and ERISA §107. | **High — drafting error** |
| 4 | **BAA §6.3.1 ("SSES shall retain no copies of the PHI") flatly contradicts Master §3.4** (retention for 8 years; SSES may retain copies it deems appropriate). | **High — internal conflict** |
| 5 | **Tax-Favored Appendix §B.6 says "SSES is under no obligation to advance funds."** That is correct as a disclaimer but the document nowhere contemplates the arrangement you actually run, where SSES advances its own funds on the card and recoups from the employer. There is **no reimbursement obligation, no ACH authorization, no set-off, and no suspension right.** | **High — unpapered receivable** |
| 6 | **Article V "Services Requested" is an unmarked list, not checkboxes.** An executed PDF does not establish what was purchased or which appendices attach. | **High — AdobeSign** |
| 7 | **No electronic signature / E-SIGN–UETA consent clause** anywhere, and no counterparts clause in the Master (the BAA has one). | Medium |
| 8 | **Nothing in the set addresses ICHRA, QSEHRA, EBHRA, individual-coverage substantiation, the ICHRA notice, the ERISA safe harbor, affordability, or the non-producer posture.** Recommendation below: **a new appendix, not edits to the existing ones.** | **High — scope** |
| 9 | **COBRA Appendix omits the DOL-required Notice of Unavailability and Notice of Early Termination to qualified beneficiaries**, and omits premium grace-period / insignificant-shortfall / NSF / lapse-risk allocation. | **High** |
| 10 | Blanks that must not survive into an AdobeSign template: §1.1 effective date, §4.6 arbitration venue ("State of ."), §5.1 proposal number, Article VI fields, COBRA appendix "Initial: ____". | Medium |

---

## 1. Structural recommendation — modularize before you edit

**Do not maintain two parallel full agreements.** Split into:

```
MSA-2026        Master Services Agreement (Articles I–VI)      — signed once
BAA-2026        Business Associate Agreement                   — signed once, referenced by MSA §3.7
APX-TFP         Tax-Favored Plan Administration Appendix       — §125 / §105 FSA / HRA / MERP / §132
APX-COBRA       COBRA & Continuation Services Appendix
APX-ICHRA       Individual Coverage HRA ("PremiumPath") Appendix    ← new
APX-QSEHRA      Qualified Small Employer HRA Appendix               ← new, separate from ICHRA
APX-CARD        PremiumPath Card Program Appendix                   ← new (see §5, item 5)
SCH-FEE         Fee Schedule (the proposal, by number)
SCH-CONTACT     Designated Contact List (amendable without amending the MSA)
```

Each appendix opens with the same three lines: incorporation, effective date, and **order of precedence**.

**Replace the current precedence clause.** It presently reads: *"If there is a conflict between this Service Appendix and the Agreement, the Agreement controls."* That is backwards — it lets Master §2.5 ("Employer is responsible for payment of claims") override an appendix that describes a different funding mechanic. Use:

> **Order of Precedence.** In the event of conflict, this Appendix controls as to the description, scope, and performance of the services it covers. The Agreement controls as to all other matters, including limitation of liability, indemnification, term, termination, and governing law. The Business Associate Agreement controls as to the use and disclosure of PHI, subject to Article II's limitation of liability, which applies to it.

That last clause is how you close finding #2 without renegotiating the BAA's substance.

---

## 2. Master Agreement — defects, stale items, and gaps

### 2.1 Regulatory / accuracy

| Cite | Issue | Recommendation |
|---|---|---|
| §3.6 | **"Gramm-Leach-Bllley Act"** — misspelled, and GLBA privacy-notice obligations are for financial institutions. A TPA of a group health plan is governed by HIPAA, not GLBA. The clause also promises SSES will furnish *all* required notices "including any required opt-out notice," which is an obligation you likely do not perform. | Delete the GLBA reference. Replace with: SSES maintains its own privacy and security practices and will furnish a copy on request; participant-facing privacy notices are the Employer's obligation as covered entity. |
| §3.4 | "**lesser of** eight (8) years … or the termination" | → "**greater of** eight (8) years following creation of the record or the period required by applicable law, and in no event less than six (6) years from creation or last effective date (45 C.F.R. §164.316(b)(2))." Add a carve-out permitting SSES to retain records required by its own legal, tax, or insurance obligations notwithstanding BAA §6.3.1. |
| §2.1(b) | Lists the Employer's Plan Administrator duties; ends "unless SSES provides such services under an appendix." Fine, but **silent on the post-2021 CAA obligations** now attached to group health plans. | Add a clause allocating: RxDC prescription-drug reporting, the annual gag-clause prohibition attestation, transparency-in-coverage posting, MHPAEA NQTL comparative analysis, and any successor reporting — **Employer's obligation unless expressly assumed in an appendix for an additional fee.** ⚠️ **VERIFY** whether each applies to an account-based plan (HRA/ICHRA); CMS has issued relief and FAQ guidance narrowing some of these for HRAs. Allocating them generically costs nothing and does not require resolving applicability. |
| §1.2 / §3.1 / §3.8 | Non-discretionary, non-fiduciary status is asserted but never cited. | Add: "SSES is not a fiduciary as defined in ERISA §3(21) with respect to the Program, exercises no discretionary authority or control over Program assets or administration, and is not a named fiduciary, plan administrator (ERISA §3(16)), or claims fiduciary." This matters both for ERISA and for the state TPA-licensing posture already registered in this project. |
| §4.9 | Michigan governing law, but no venue. §4.6 arbitration venue is blank. | Fix both; see §2.2. |
| Article I | **§1.1 is rendered after §1.2 and inside the page footer** in both PDFs — it is in a floating text box. This breaks AdobeSign tag placement and looks like a defect to a client's counsel. | Rebuild Article I in the document body. |

### 2.2 Dispute resolution — pick one path

§4.6 currently provides **non-binding** arbitration as a precondition to suit, becoming binding only if both parties separately agree in writing. That is a mediation clause wearing an arbitration label, it adds cost without finality, and the venue is blank. Choose:

- **Option A (recommended):** Mandatory mediation, then litigation in the state or federal courts sitting in Menominee County / the Western District of Michigan; exclusive jurisdiction; **jury waiver**; each party bears its own fees.
- **Option B:** Binding AAA Commercial arbitration, single arbitrator, seat in Menominee, Michigan.

Add in either case: **a one-year contractual limitations period** for claims arising out of the Agreement, running from the date the claimant knew or should have known of the claim. This is the cheapest single risk reduction available to you.

### 2.3 Fees (§2.2, §5.1)

Missing: the fee basis (PEPM vs. per-plan vs. flat), a stated minimum, an invoice-dispute window, a stated interest rate, and the right to **suspend** services (as distinct from terminating) for non-payment. Add:

- "Fees are stated in the Fee Schedule for proposal #___, which is incorporated by reference. Fee basis, minimums, and setup fees are as stated there."
- Past-due interest at **1.5% per month or the maximum permitted by Michigan law, whichever is less**, plus collection costs.
- Invoices are deemed accepted if not disputed in writing within **30 days**.
- **Suspension right:** SSES may suspend card funding, claims release, and premium remittance — without terminating — on **10 days'** written notice of non-payment, and is not liable for consequences of suspension. This is what protects the advance described in §5 item 5.

### 2.4 Assignment and subcontracting (§4.3)

§4.3 bars assignment without consent and the Master is **silent on subcontracting**, while you in fact use a platform vendor, a card issuer/BIN sponsor, and third-party data sources. Add:

- SSES may **subcontract** any service to qualified vendors, remains responsible for their performance under the Standard of Care, and flows down BAA obligations (BAA §2.8 already requires this).
- SSES may assign to an **affiliate or to a successor** by merger, acquisition, or sale of substantially all assets, without consent.
- Employer consent still required for any other assignment.

### 2.5 Missing clauses (add to Article IV)

1. **Electronic Signatures and Records.** E-SIGN (15 U.S.C. §7001) and Michigan UETA consent; electronically executed counterparts are originals; the e-signature platform's audit trail is admissible evidence of execution; electronic delivery of notices and reports is agreed.
2. **Notices by email.** §4.7(a) permits only overnight or first-class mail, while the entire service model is electronic. Add email notice to the Named Contact, effective on transmission absent bounce, for all notices except termination and breach (keep those on mail or mail + email).
3. **Limitation of Liability** (see §3 below) — currently only the embedded consequential-damages waiver in §2.6.
4. **Insurance.** SSES maintains commercial general liability, professional liability (E&O), and cyber/privacy liability in commercially reasonable amounts, and will furnish certificates on request. Do **not** state dollar amounts in the template.
5. **No Third-Party Beneficiaries.** §1.2 covers participants only by implication; make it explicit and extend it to brokers and agents.
6. **De-identified and Aggregate Data.** SSES may create and use de-identified and aggregated data (consistent with 45 C.F.R. §164.514) for benchmarking, product development, and reporting. Without this, BAA §3.5 gives you aggregation rights *for the client*, not for SSES. This is worth more to AMS than anything else on this list.
7. **Survival.** An explicit list: §2.5, §2.6, §3.4, §4.5, §4.6, the liability cap, and the BAA.
8. **Force majeure**, extended. §3.2 has a good list; add cyber incident, third-party platform or carrier outage, and payment-network failure.
9. **Compliance-change fee trigger.** §2.2 has it; extend it to cover changes in *guidance and interpretation*, not just "changes to the Benefit Plans or applicable law."

### 2.6 Broker compensation — a decision you need to make, not a defect

If SSES's invoice ever includes an amount remitted to the Employer's producer (the white-label agent fee), the Master is silent on it, and **CAA 2021 §202 added ERISA §408(b)(2)(B) compensation disclosure for brokerage and consulting services to group health plans** (covered service providers expecting $1,000+ in direct or indirect compensation must disclose to the responsible plan fiduciary).

Two things follow:

1. The **producer** has its own disclosure duty — not yours to perform, but your invoice is the instrument that creates the indirect compensation.
2. Whether **SSES** is itself a covered service provider turns on whether its services are "brokerage" or "consulting." ⚠️ **VERIFY** — a pure recordkeeping/administration TPA is generally treated as outside §408(b)(2)(B), but the plan-document and nondiscrimination-consulting elements of APX-TFP push toward the line.

**This is in direct tension with the standing project boundary that agent markup is invisible to the employer.** A disclosure clause that says "fees may include compensation payable to Employer's broker or agent of record" resolves the contract-silence problem, but §408(b)(2)(B) contemplates an amount or a reasonable estimate, not an acknowledgment. **Flagging as a decision, not resolving it.** Cheapest defensible interim position: include a §408(b)(2) disclosure section that describes SSES's own direct compensation in full, states that SSES receives no compensation from any insurer or carrier, and states that amounts payable to the Employer's producer, if any, are disclosed by the producer. That is accurate under your model and does not require publishing the markup.

---

## 3. The indemnification package

The current §2.6 is a serviceable mutual indemnity built around an undefined-in-place "Standard of Care," and it waives consequential damages. What it is missing is everything that actually allocates the risks your business runs. Replace §2.6 with a full Article containing:

**3.1 Standard of Care — defined separately.** "SSES will perform its obligations with the reasonable care and diligence that a prudent third-party administrator in the same industry would exercise under like circumstances (the 'Standard of Care'). It is not a breach of the Standard of Care for SSES to act in accordance with Employer's written instructions, elections, or certifications, or in reliance on information furnished under §3.2."

**3.2 Reliance — the most valuable clause you can add.**
> SSES may conclusively rely, without independent investigation or verification, on any data, election, instruction, eligibility or classification determination, contribution or withholding amount, plan design decision, certification, or attestation furnished by Employer, Employer's payroll provider, Employer's broker or agent of record, or a Participant, and on the accuracy, completeness, and timeliness of the same. SSES has no duty to audit, reconcile against, or investigate such information.

**3.3 Timeliness.** SSES's performance deadlines — including every notice deadline — run from **receipt of complete and accurate data** from the Employer, not from the underlying event. Late, incomplete, or inaccurate data extends SSES's deadlines accordingly and any resulting penalty is the Employer's. This is the clause that protects you on COBRA.

**3.4 Taxes and penalties.** Employer is solely responsible for excise taxes and penalties arising from the Program, including IRC §4980D, §4980H, §6721/§6722, employment-tax consequences of improper pre-tax treatment, and interest thereon, except to the extent directly and proximately caused by SSES's breach of the Standard of Care.

**3.5 Mutual indemnity** — keep the existing symmetry, but add to the Employer's side: inaccurate or late data; plan design; eligibility and classification determinations; failure to execute or distribute documents; broker or producer conduct; and any determination reserved to the Employer under an appendix (affordability, class structure, COBRA premium rates).

**3.6 Indemnity procedure** — currently absent from the Master and one-sided in the BAA. Prompt written notice; indemnitor controls defense with counsel of its choice; indemnitee cooperates; **no settlement admitting liability or imposing non-monetary obligations on the indemnitor without consent**; failure to give prompt notice reduces the obligation to the extent of resulting prejudice.

**3.7 Limitation of liability** — new.
> Except for (a) a party's indemnity obligations for third-party claims, (b) SSES's gross negligence or willful misconduct, and (c) Employer's payment obligations, **each party's aggregate liability arising out of or relating to this Agreement, all Appendices, and the Business Associate Agreement will not exceed the total fees paid by Employer to SSES during the twelve (12) months preceding the event giving rise to the claim.** Neither party is liable for indirect, incidental, consequential, special, exemplary, or punitive damages, or for lost profits, revenue, or business opportunity, regardless of the theory of liability.

Note the deliberate inclusion of the BAA in the cap's scope — that is finding #2's fix. Expect a client's counsel to push for a carve-out for SSES's own HIPAA breach; a defensible fallback is a **super-cap** at the greater of 12 months' fees or a stated multiple, tied to your cyber policy limit, applicable solely to breach-notification and regulatory-response costs.

**3.8 No guarantee of outcome.** SSES does not guarantee the tax treatment, ERISA status, or regulatory qualification of the Program, and provides no tax or legal advice (§2.4 says this; move it here and broaden it to cover computational outputs such as affordability and nondiscrimination test results, which are based on Employer-furnished data).

---

## 4. BAA — required fixes

| Cite | Issue | Fix |
|---|---|---|
| §7.2.1, §7.3.1, §7.4 | Uncapped indemnity + duty to fund the client's chosen counsel + client controls settlement while SSES indemnifies. Commercially unusual and unbounded. | Make the indemnity mutual and reciprocal in mechanics; delete §7.4's unilateral settlement control or make it subject to SSES's consent where SSES bears the cost; subject the whole Article to Master §3.7's cap per the precedence clause. |
| §7.1 | Garbled: "In the event that **SSES** receives a subpoena … **CLIENT** shall promptly forward a copy of such subpoena **CLIENT**." Nonsensical as written. | Rewrite: the **receiving** party promptly notifies the other, to the extent legally permitted, and reasonably cooperates; the party whose information is sought may seek protective relief at its own cost. |
| §6.3.1 | "SSES shall retain no copies of the PHI" — conflicts with Master §3.4 and with your own retention obligations. | Add: "except that SSES may retain PHI to the extent required by applicable law or by its legal, tax, audit, or insurance obligations, and will extend the protections of this Agreement to any PHI so retained for as long as it is retained." |
| §2.6 | Monthly reporting, by the 10th, of the **aggregate number of unsuccessful unauthorized access attempts "including pings."** You almost certainly do not produce this report. An unperformed contractual obligation is a stipulated breach. | Delete, or replace with: "SSES will provide a summary of unsuccessful security incidents upon Client's reasonable written request, no more than annually. The parties agree that routine unsuccessful attempts (pings, port scans, failed logins) do not require individual reporting." This is consistent with OCR's own guidance on trivial incidents. |
| §2.5 / §2.7 | 30 calendar days to report both non-permitted uses and potential breaches. Since the **covered entity's** individual-notification clock under 45 C.F.R. §164.404 is 60 days from discovery, a 30-day BA window consumes half of it. Counsel for any sophisticated employer will object. | Tighten to what you can actually perform: non-permitted use — **without unreasonable delay and no later than 10 business days**; potential breach — **no later than 15 calendar days**, with the §164.410 content set. Do not shorten below what your operations can meet. |
| §2.11 | "SSES agrees not to receive … remuneration in exchange for any PHI" — correct, and worth keeping. Make sure the de-identified-data clause in §2.5(6) above is drafted so it does not read as a sale of PHI: de-identified data is not PHI. | Add the cross-reference expressly. |
| Term | BAA term is defined by PHI destruction, not by the Master. | Tie: "co-terminous with the Agreement, surviving until PHI is returned, destroyed, or protected as provided in §6.3." |
| Breach costs | Unallocated. | Add: the breaching party bears the direct costs of required notification, call center, and credit monitoring, subject to the Master's cap and any super-cap. |
| State law | §8.6 handles more-stringent state privacy law generically. You will have Texas participants. | Sufficient as drafted; no change needed, but note Texas Bus. & Com. Code ch. 521 imposes its own notification duties on any person conducting business in Texas. |

---

## 5. Tax-Favored Plan Administration Appendix (APX-TFP)

1. **Nondiscrimination testing list (§A.5) needs correcting and completing.** Currently: §125(g)(3) eligibility, §125 key employee concentration, "§105(h)(2)(A) eligibility," §129 55% average benefits, and "25% Shareholder Concentration Test required under Code Section 129." Issues:
   - Missing the **§125 contributions-and-benefits test**.
   - **§105(h)** has *two* tests — the eligibility test (§105(h)(3)) and the **benefits test (§105(h)(4))**. Citing "§105(h)(2)(A)" names the general requirement, not a test, and omits benefits testing entirely.
   - Missing the **§129(d)(2)/(d)(3) eligibility** test.
   - The "25% Shareholder Concentration Test" is the **§129(d)(4)** limit — not more than 25% of amounts paid may go to more-than-5% owners. Relabel it accurately.
   - **§105(h) applies to HRAs, MERPs, and ICHRAs**, and ICHRA class structures create real testing exposure. Say so, and say that SSES tests only the plans it administers, only from Employer-certified data, and that results are informational, with correction the Employer's responsibility.

2. **SBC clause (§B.10) is likely wrong.** It asserts that HRAs, MERPs, **and excepted FSAs** must furnish an SBC, citing a re-enrollment date of September 23, 2012. Excepted benefits are generally **excluded** from the SBC requirement, and the 2012 date is stale template language. ⚠️ **VERIFY** before rewriting; my read is that an **excepted-benefit health FSA is not subject to SBC**, a non-excepted HRA/MERP **is**, and an **ICHRA is** (it is a non-excepted group health plan). Rewrite generically: "Group health plans subject to the SBC requirement must furnish an SBC. Employer is responsible for determining applicability and for furnishing SBCs. No assistance is provided by SSES except under a separate appendix for an additional fee."

3. **Missing obligations to allocate.** Add to §B: **PCORI fee** (Form 720) for HRAs, MERPs, and ICHRAs — Employer files, SSES supplies counts; **§6055 reporting** (Form 1095-B, or 1095-C Part III for an ALE) for self-insured MEC including an ICHRA; **§6051 W-2** reporting (including QSEHRA Box 12 code FF); Form 5500 where applicable. The existing §A.7 covers only 5500 data.

4. **Missing plan-mechanics terms.** Run-out period and claims-submission deadline; uniform coverage rule for the health FSA; carryover vs. grace period election; forfeiture and experience-gain disposition; HSA expressly out of scope unless elected; mid-year election change authority (present) plus the **prospective-only** rule.

5. **Funding (§B.6) does not match how PremiumPath actually operates.** The appendix offers only check-writing authority over an employer account or claims-paid-directly-by-employer, then states SSES has no obligation to advance funds, then refers to "the electronic payment card agreement incorporated into and made a part of this Agreement." Two problems: **(a)** that card agreement is referenced but is not in the document set — a reference to a nonexistent document fails; **(b)** your card model advances SSES's funds and recoups from the employer, and the contract contains no reimbursement obligation, no schedule, no ACH authorization, no set-off, and no security.

   **This is a contract gap, not a mechanism question — I am proposing no change to how money moves.** Create **APX-CARD** containing: SSES *may but is not obligated to* advance; Employer's unconditional obligation to reimburse advanced amounts within a stated number of business days of the funding notice; **ACH debit authorization** with the account designated on a schedule; interest on late reimbursement; **right of set-off** against any Employer funds held; the suspension right from §2.3 above; express statement that a missed or unfunded payroll results in no card load and **never in participant debt**; card deactivation on termination; and disposition of unspent balances. Add an optional **personal guaranty** page for closely held employers — worth having in the template even if you rarely deploy it.

6. **§A.3 gives SSES initial claim determination and "up to the first level of appeal" (§B.2).** That is a discretionary act and it is the exact fact pattern that drives the state TPA-licensing question already registered in this project. Keep the allocation, but make the disclaimer explicit: initial determinations are ministerial applications of the Employer's written plan terms and substantiation rules, the Employer holds final authority on appeal, and SSES exercises no discretion.

---

## 6. COBRA & Continuation Services Appendix (APX-COBRA)

1. **Name of the statute is wrong** — "Consolidated Omnibus **Reconciliation** Act of 1985" should be "Consolidated Omnibus **Budget** Reconciliation Act of 1985." It appears in the opening recital of the appendix. HIPAA is also folded into "the Acts" and then never given an obligation; drop it or give it one.

2. **Missing notices required of the plan administrator.** The appendix covers initial/general notice, qualifying-event notice, update notices, and a termination notice **to the Employer**. It omits the two DOL-required notices **to qualified beneficiaries**: the **Notice of Unavailability of Continuation Coverage** and the **Notice of Early Termination of Continuation Coverage**. Also missing: second-qualifying-event and disability-extension handling, Medicare-entitlement interaction, and open-enrollment/rate-change notices.

3. **Statutory clock not stated, and not protected.** Add the framework (employer → plan administrator within 30 days; plan administrator → QB within 14 days; 44 days combined where the employer is the administrator; QB election period 60 days) **and** the §3.3 clause above: SSES's 14-day obligation runs from receipt of complete and accurate data, not from the qualifying event. Your appendix asks the Employer for notice within 14 days of knowledge — keep that, but make the consequence of lateness explicit rather than implied.

4. **Premium handling is under-papered.** Add: initial payment 45-day window and subsequent 30-day grace period; the **insignificant-shortfall** rule and how SSES applies it; NSF/returned-payment fee; the remittance schedule to the Employer; that SSES **collects premiums as agent of the Employer** and does not hold them in trust or bear the risk of non-payment; and — critically — that **SSES does not advance premium and the Employer bears the risk of coverage lapse** where the Employer fails to effect timely carrier reinstatement or termination. The current appendix already puts carrier notifications on the Employer; the lapse-risk consequence should be stated in the same breath.

5. **Fee mechanics.** "SSES will retain any administrative fees added to premiums charged by the insurer" — say that this is the **statutory 2% (or 150% during a disability extension)** or such lesser amount as the Employer directs, that it is SSES's compensation, and **add a basis for continuation of a self-insured arrangement (an HRA or ICHRA), where there is no insurer premium to load.**

6. **Add ICHRA continuation expressly — this answers your COBRA question directly.** An ICHRA is a group health plan and is subject to COBRA (unlike the individual policies it reimburses, which are not). The appendix currently assumes insured group coverage throughout. Add:
   - The **ICHRA itself** is the continuable coverage; the qualified beneficiary's individual policy is not, and continuation of the ICHRA does **not** continue or guarantee individual coverage.
   - The **Employer determines the COBRA premium** for the ICHRA on an actuarially reasonable basis and certifies it to SSES; SSES bills what the Employer certifies and makes no actuarial determination.
   - A continuing QB must **remain enrolled in individual health insurance coverage** and satisfy the same substantiation requirements as an active participant; failure to substantiate holds reimbursement rather than terminating continuation.
   - Loss of ICHRA eligibility is an individual-market **special enrollment period** trigger; SSES provides the notice, the Employer and the producer handle the coverage conversation. ⚠️ **VERIFY** the SEP mechanics and timing against the current Marketplace rules before this language goes to a client.
   - ⚠️ **VERIFY** whether a **QSEHRA** is subject to COBRA before drafting the QSEHRA appendix — the Cures Act excludes a QSEHRA from the definition of a group health plan for certain purposes, and I am not confident which. Do not carry the ICHRA answer across.

7. **Regulatory relief clause.** The 2020–2023 outbreak-period tolling is over, but it happened, and it materially changed election and payment deadlines mid-stream at no notice. Add: where a government action extends or tolls COBRA deadlines, SSES will implement the relief and may revise fees under §2.2; the Employer bears the cost of extended administration.

8. **State continuation.** Expressly out of scope unless elected in an addendum. Do not leave this to inference in a multi-state agent network.

9. Housekeeping: "Initial: ______" needs to become a real AdobeSign initial field or be deleted; the appendix refers to "Superior State Administrators, Inc." throughout while the Master uses "SSES" — normalize to one defined term.

---

## 7. New — Individual Coverage HRA Appendix (APX-ICHRA / PremiumPath)

**Recommendation: a new appendix. Do not amend APX-TFP to cover ICHRA.** Three reasons: the §125 integration is conditional in a way no other benefit in APX-TFP is; the non-producer and no-steering posture needs its own contractual home; and the ICHRA notice and substantiation obligations have no analogue in FSA/HRA administration. APX-TFP does need **one** amendment, in §7.3 below.

### 7.1 SSES responsibilities

- ICHRA plan document, SPD, and adoption resolution text; §125 plan document or POP amendment where the cafeteria plan rail is elected.
- Preparation of the **ICHRA notice** to eligible participants, and delivery through SSES's platform or by mail. **Timing is not determined by SSES** — state that the Employer sets the offer and effective dates and that notice timing is a function of those dates. ⚠️ The newly-established-plan notice deadline is an **open item in this project's register (LA-08)** and is not settled; the appendix must not recite a 90-day figure or any computed date as a contractual commitment.
- Capture and retention of **opt-out and waiver** elections, once per plan year, with effective dates.
- **Substantiation program:** annual proof or attestation of individual coverage at or before the first reimbursement of the plan year, plus an attestation accompanying each payment request; reliance on attestation absent actual knowledge of falsity; append-only immutable attestation records with a versioned legal-text identifier.
- Reimbursement and premium-payment processing, through the card or by reimbursement.
- §105(h) nondiscrimination testing.
- Affordability **computation output** for the Employer (see 7.2).
- Participant enrollment, decline, and premium-information records.
- Renewal administration.

### 7.2 Employer responsibilities — the determinations that stay with the client

- Establishes **classes** of employees, contribution amounts, and whether amounts vary by age or family size; determines **minimum class size** applicability; ensures the **same-terms** requirement is met. SSES implements as directed and makes no class determination.
- Determines its own **ALE status** and makes any **§4980H(b) affordability determination**. SSES may furnish a computation based on third-party data; **that computation is informational, is not a determination, and is not furnished to employees.** (This is the contractual expression of a registered project assumption and it should be in the agreement, not only in the code.)
- **§6055 reporting** (1095-B / 1095-C Part III), **PCORI**, Form 5500 where applicable, and W-2 reporting.
- Delivers the **ERISA safe-harbor annual notice** stating that the individual coverage is not subject to ERISA, and observes the safe-harbor conditions: **no endorsement** of any particular issuer or policy, no cash-or-coverage alternative outside the ICHRA, no consideration received in connection with an employee's individual coverage, and annual notice to participants. SSES's services are designed to support these conditions but **the Employer's conduct determines whether they are met.**
- Certifies payroll withholding amounts, funds the ICHRA, and reimburses card advances per APX-CARD.
- Determines the ICHRA COBRA premium (cross-reference APX-COBRA §6 above).

### 7.3 The §125 rail — off-Exchange only

This is the clause with the most legal weight in the appendix and it should be drafted conservatively, because the underlying question is registered as **open** in this project (LA-19: whether the cafeteria-plan permission is conditioned on the employee being *covered by* rather than merely *offered* an ICHRA; the more restrictive "covered by" reading is what the design assumes).

Draft it to say:

- Salary reduction under the cafeteria plan is available **only** for individual health insurance coverage **purchased outside an Exchange**, and only for participants **covered by** the ICHRA. §125(f)(3) bars a qualified health plan offered through an Exchange from being a qualified benefit.
- The participant **attests to the purchase channel** as well as to enrollment. Substantiation has a channel element.
- A participant who obtains Exchange coverage, or who waives the ICHRA, is **out of the cafeteria plan** for individual major medical for the remainder of the plan year; the election terminates prospectively rather than being warned.
- Neither SSES nor the Employer facilitates, assists, or directs Exchange enrollment.
- **Excepted-benefit** accident and specified-disease premiums are a separate qualified-benefit rail and do not depend on ICHRA coverage. Keep them in a separate paragraph so the two are not read together.
- Employer acknowledges that improper pre-tax treatment produces W-2 and employment-tax corrections and that those consequences are the Employer's under §3.4.

**Amend APX-TFP** to add "individual health insurance premium salary reduction (off-Exchange coverage only)" and "excepted-benefit insurance premiums" to the list of benefits the cafeteria plan may include, cross-referencing APX-ICHRA §7.3 for the conditions.

### 7.4 Non-producer / no-steering clause

> SSES is not a licensed insurance producer, agency, or broker with respect to individual health insurance coverage; receives no commission or other compensation from any issuer; does not solicit, negotiate, sell, or place individual coverage; and does not recommend, rank, curate, endorse, or select any issuer, plan, or policy, or establish any default. Any market, plan, or rate information furnished by SSES is indicative reference information furnished to Employer or to Employer's licensed producer, is sourced from third parties, is limited to off-Exchange coverage and therefore incomplete by design, and is not a quotation, an offer, or advice. No enrollment in individual coverage originates with SSES. Employer's producer or agent of record is separately engaged and is not SSES's agent or subcontractor; SSES is not responsible for the producer's conduct, licensure, suitability determinations, or communications.

Add a matching **third-party data disclaimer**: no warranty of accuracy, completeness, currency, or fitness; rates and plan availability are subject to change by issuers; SSES is not liable for reliance on such data.

### 7.5 Other clauses for this appendix

- **Owners and ineligible individuals.** More-than-2% S-corporation shareholders, partners, sole proprietors, and certain family members cannot receive these benefits tax-free. **Employer determines status**; SSES applies post-tax treatment as directed and relies on the Employer's determination.
- **Regulatory volatility acknowledgment.** The ICHRA rules are comparatively recent, have been the subject of litigation and proposed amendment, and are subject to change; services are provided against current guidance, and §2.2's fee-revision right applies.
- **Termination.** Run-out, final substantiation, card deactivation, no vesting in unspent ICHRA amounts, and transfer of records per §3.4.
- **Do not issue APX-QSEHRA to a Texas employer** under the current interim licensing posture registered in this project. That is a sales control, not contract language — but the appendix should not be in the AdobeSign library as a default-available document.

---

## 8. AdobeSign structure

**One envelope, one executed PDF, ordered:** MSA → BAA → elected appendices → Fee Schedule (proposal) → Designated Contact List. Single signature block at the MSA signature page signing all, plus a separate signature block on the BAA (it is a separate agreement with its own parties clause), plus one initial field per appendix.

**Make Article V checkboxes, and let them drive assembly.** Suggested field set:

| Field | Type | Notes |
|---|---|---|
| `AgreementEffectiveDate` | Date, required | Fills §1.1 |
| `ProposalNumber` | Text, required, validated | §5.1. **Make this the AMS proposal id** — it is the key that lets executed-agreement data come back into the Setup activity later. |
| `ArbitrationState` / `VenueCounty` | Text, pre-filled read-only | Never leave blank |
| `Svc_125Docs` … `Svc_COBRA`, `Svc_ICHRA`, `Svc_QSEHRA`, `Svc_EBHRA`, `Svc_Card` | Checkbox group, ≥1 required | Drives which appendices are included |
| `CobraInitialNoticeAllCurrent` | Checkbox | The COBRA appendix's "and if so selected in Article V" election has **no corresponding field in Article V today** — a live defect |
| Company, Address, City/State/ZIP, Form of business, State of organization | Text, required | |
| `TaxID` | Text, required, masked | Mark as restricted data. For a sole proprietor this may be an SSN — consider collecting EIN only and rejecting SSN, consistent with the project's minimum-census posture |
| Named Contact name / email / phone | Text, required; email validated | Email drives §4.7 notice |
| `DesignatedContact1..3` name + email | Text, 1 required | Move to an amendable schedule |
| `AuthorizedOfficer` name / title | Text, required | Currently no title field |
| Signature, Date | Signature | |
| `Initial_APX_TFP`, `Initial_APX_COBRA`, `Initial_APX_ICHRA`, `Initial_APX_CARD` | Initials, conditional on the Article V checkbox | |
| `ACH_Bank`, `ACH_Routing`, `ACH_Account`, `ACH_Auth` | Conditional, APX-CARD only | ⚠️ Treat as restricted; do not export into AMS |

Three mechanical notes:
1. **Rebuild the documents so no content sits in floating text boxes** (Article I today). Text tags and anchors in floating frames place unpredictably.
2. Use **conditional field visibility** keyed to the Article V checkboxes rather than maintaining separate templates per service combination — one template, N appendices, is what makes this maintainable across a white-label agent network.
3. Downloading the field data gives you a clean structured record keyed on `ProposalNumber`. **Noted as an opportunity, not a work item** — nothing in this review depends on it.

---

## 9. Items I did not verify

Listed so nothing here is mistaken for a settled conclusion.

1. Whether SBC requirements reach excepted-benefit FSAs (§5 item 2) — my read is no, but the current clause says yes.
2. Whether RxDC reporting, the gag-clause attestation, and MHPAEA NQTL analysis apply to HRAs and ICHRAs (§2.1). The recommendation avoids needing the answer.
3. Whether an ICHRA sponsor's §6055 reporting is on Form 1095-B or 1095-C Part III in every case (§5 item 3, §7.2).
4. Whether a **QSEHRA** is subject to COBRA (§6 item 6).
5. The individual-market SEP mechanics on loss of ICHRA eligibility (§6 item 6).
6. Whether SSES is a "covered service provider" for ERISA §408(b)(2)(B) disclosure (§2.6).
7. The ICHRA notice deadline for a newly established plan — **open in this project's own register and deliberately not recited as a contract term** (§7.1).
8. Whether the §125 permission for off-Exchange individual coverage is conditioned on being covered by, or merely offered, an ICHRA — **open in the register; the appendix is drafted to the restrictive reading** (§7.3).

Items 7 and 8 are load-bearing for APX-ICHRA. The appendix as outlined is drafted so that being wrong about either costs a document revision rather than a correction of employee tax treatment already taken.

---

## 10. Suggested build order

1. **Decide the structure** — modular set vs. editing the two standalone documents in place. Everything else depends on it.
2. **MSA-2026** — Articles I–VI with the new Article on liability and indemnification (§3), the e-signature clause, precedence, subcontracting, and the de-identified data clause.
3. **BAA-2026** — the six fixes in §4.
4. **APX-TFP** and **APX-COBRA** — corrections in §5 and §6.
5. **APX-ICHRA** + the APX-TFP §125 amendment — §7.
6. **APX-CARD** — §5 item 5.
7. **APX-QSEHRA** — last, and gated on the COBRA question in §9 item 4.
8. **AdobeSign template** — §8, built once against the finished set.
