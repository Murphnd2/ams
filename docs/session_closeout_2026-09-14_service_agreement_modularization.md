# Session close-out — 2026-09-14 · Service agreement modularization

**Save into project knowledge. Read at session open, after the strategy docs.**

⚠️ **This was a document-assistance side session.** No repo changes, no build prompts for AMS features, no SQL. Everything below is contract drafting and one product decision about e-signature delivery.

⚠️ **Never assert a repo fact from this file.** It records what was drafted and decided in conversation. The repo is authoritative for anything about AMS.

---

## 1. What this session did

Converted SSA's two standalone service agreements — the §125/§105 "Tax-Favored" set and the COBRA set, each carrying a full duplicate master agreement and BAA — into a **modular set of twelve documents**, and drafted v1 of all of them.

**Status: all twelve at draft v1. Nothing finalized. Nothing reviewed by counsel. Nothing issued.**

Kevin's stated process: answer the open items → update documents → finalize with counsel → then delivery and retention.

---

## 2. Documents produced

All as downloadable `.md` files. **Document activity is parked as of this session's end.**

| Reference | Covers | Status |
|---|---|---|
| `premiumpath_service_agreement_review.md` | The original review that started the session — ten headline findings, defects, stale items, indemnification package, AdobeSign field map | Complete |
| `open_items_register_v1.md` | **Consolidated open items across all twelve documents** — 68 items in six categories, eleven blocking | Complete |
| `MSA-2026_draft_v1.md` | Master Services Agreement, Articles I–VIII | draft v1 |
| `BAA-2026_draft_v1.md` | Business Associate Agreement | draft v1 |
| `APX-RA_draft_v1.md` | Reimbursement accounts — Health FSA, DCAP, HRA, EBHRA, MERP | draft v1 |
| `RIDER-ICHRA_draft_v1.md` | Individual coverage HRA — notice, substantiation, classes, ERISA safe harbor, §125 rail, non-producer | draft v1 |
| `RIDER-QSEHRA_draft_v1.md` | QSEHRA | draft v1 |
| `APX-POP_draft_v1.md` | §125 premium only — documents and testing only | draft v1 |
| `APX-TRANSIT_draft_v1.md` | §132(f) parking and commuter | draft v1 |
| `APX-COBRA_draft_v1.md` | COBRA and continuation, including premium custody and ICHRA continuation | draft v1 |
| `APX-PAYMENT_draft_v1.md` | Check issuance and participant direct deposit | draft v1 |
| `APX-CARD_draft_v1.md` | Electronic payment card | draft v1 |
| `SCH-BANKING_draft_v1.md` | Banking authorizations — the executed settlement form | draft v1 |
| `SCH-CONTACT_draft_v1.md` | Designated contact list | draft v1 |
| `SCH-FEE` | The proposal, by number | Not drafted — nothing to draft |

---

## 3. Decisions made

| Decision | What it closed |
|---|---|
| **Modular set**, not edits to the two standalone agreements | Each PDF was a complete master + BAA; a client buying both signed two of each with no stated relationship |
| **Corporate name** — Superior State Administrators, Inc. ("SSA") throughout; SSES retained only as a "formerly doing business as" recital in the MSA | Kevin reverted to the corporate name in nearly all communication |
| **Liability cap reaches the BAA**, via MSA §5.8 and BAA §7.5 plus an order-of-precedence clause | The largest exposure in the old set: an uncapped BAA indemnity with client-controlled settlement |
| **Funds representation scoped, not flat** (MSA §1.6) | COBRA premiums pass through SSA; a flat "holds no funds" clause would have been false where it matters most |
| **Consolidate FSA and HRA into APX-RA** with a plan-type section; ICHRA and QSEHRA as riders on top | Everything but the plan-type section came out identical; duplicate operative text is how a set drifts |
| **Transit and POP get their own thin appendices** | A §132(f) program is not a cafeteria plan, not a group health plan, not ERISA. A POP client buys documents and testing |
| **No notice deadline in any document** | LA-08 is open with no basis; a contract term converts a working assumption into a commitment |
| **§125 rail drafted to the restrictive "covered by" reading** (RIDER-ICHRA E.1) | LA-19. Cheap-to-reverse direction: too conservative costs an amendment, too permissive costs per-employee W-2 corrections |
| **No product names in operative terms** | Keeps the set usable for any employer and any partner agency |
| **E-sign: short term, prefilled PDF uploaded to Adobe manually; migrate to an API vendor when ready** | Adobe API access is enterprise/developer tier only and Kevin's Acrobat Pro plan has no API menu |
| **SignWell is the intended API vendor** when migration happens; BoldSign as the upgrade path | Volume currently under 25/month, so SignWell's 25 free API documents cover it; $0.75/doc beyond is acceptable |
| **The send step is an adapter**; AMS-side work is vendor-independent | Prefill generation can be built without settling the vendor |

---

## 4. Contradictions found — reported, not fixed

These disagree with the project's own registered facts and need action in `legal_assumptions.md`.

1. ⚠️ **REG-01 — the card does not advance SSA's funds.** `legal_assumptions.md` fact #2 states that "for the debit card it advances its own funds and recoups them from the employer." **Wrong instrument.** Cards are issued by Armstrong Bank and the program provider drafts the employer's designated account the next business day. **The advance is on the ACH participant-payment rail** — where SSA settles a participant credit and the employer's funding pull can fail. Kevin's practice there: invoice and collect; absorb and chase has never occurred. ⚠️ **LA-10's reading of the Texas advance-and-collect exemption leans on the wrong characterization.**
2. **REG-02 — COBRA premium pass-through confirmed.** Fact #3 stands. MSA §1.6 is scoped accordingly.
3. **REG-03 — a recommendation was made, withdrawn, and re-established elsewhere.** The original review recommended a card advance-and-recoup clause; it was withdrawn when the bank-funded settlement emerged, then re-established on the payment rail as APX-PAYMENT Section D. Recorded so the reversal is visible.

---

## 5. New findings with live exposure

⚠️ **The Summit card-setup popup warrants contract language that does not exist.** When SSA enables the card for an employer in Summit, it acknowledges — on behalf of SSA — that SSA "has language in its agreement with Employer" covering three points: the employer is responsible to DataPath for settlement payments; a $25 charge per ACH return for insufficient funds; and DataPath may terminate system access for the employer's plan participants for non-payment. **None of the three appears in either current agreement.** The same acknowledgment carries an SSA indemnity to DataPath covering the employer's failure to pay.

This is live on every card employer today. APX-CARD B.2–B.4 and SCH-BANKING Part 5 carry the language, with DataPath named an intended third-party beneficiary — but nothing is executed yet.

**Related:** the old DataPath settlement form and Settlement Account Agreement are © 2015/2016 and describe the retired pre-funding model. The current employer-facing form Kevin uses ("Employer Authorization Form") covers payment services only and contains no reimbursement obligation for a failed pull.

---

## 6. Open questions and who settles them

**`open_items_register_v1.md` is the authoritative list.** Eleven items are marked blocking. In the order recommended there:

1. **EXT-01, EXT-02, EXT-05** — one conversation with DataPath: is there a current settlement form and Settlement Account Agreement superseding 2015/2016; does an SSA-provided form satisfy the acknowledgment; does the licensing agreement carry flow-down requirements beyond what the popup recites. **Only items with live exposure today.**
2. **DEC-11, DEC-12, DEC-13** — where collected COBRA premiums actually sit, remittance cadence, and whether SSA retains the interest. Operational answers first; drafting follows. ⚠️ DEC-11 has weight beyond the drafting — state TPA statutes impose fiduciary-account rules on funds a TPA receives.
3. **PRA-01, PRA-02, PRA-03** — three places the drafts assert something about SSA's practice that a client could rely on: breach-response timing SSA can actually meet; whether claim determinations are genuinely ministerial; whether the platform collects the withholding certification wording in APX-CARD C.2.
4. **DEC-19** — ICHRA affordability computation in scope at launch or out. **Recommendation: out.**
5. **VER-09, VER-11** — whether an eligible employee can decline a QSEHRA, and the nondiscrimination test citations.

Plus, from the original review, six items needing verification against primary sources: SBC and excepted-benefit FSAs; CAA-era reporting applicability to account-based plans; §6055 form for an ICHRA; QSEHRA and COBRA; ICHRA SEP mechanics; and ERISA §408(b)(2)(B) covered-service-provider status.

---

## 7. What the next session needs to know

- **Document work is parked.** Do not resume drafting unless Kevin says to.
- **Start from `open_items_register_v1.md`**, not from the individual drafts. It is the index into all twelve.
- **Nothing here is final.** Every draft carries its own "Notes to Kevin" section with the decisions and flags specific to it — those are not duplicated in the register, which carries the cross-document view.
- **The AMS-side build that this session identified** and did not start: generate the assembled, prefilled document set from application data, keyed to the proposal number, with the send step behind a swappable adapter. Short-term output is a prefilled PDF Kevin uploads to Adobe by hand.
- **Retention was raised and not designed.** The shape discussed: capture three artifacts on completion (executed PDF, audit trail/certificate of completion, structured field data), hash at ingest, files on disk with metadata in MySQL inside the existing Wasabi backup path, vendor retention set as short as allowed. ⚠️ Executed SCH-BANKING contains full routing and account numbers — decide whether it belongs in its own envelope with its own retention, and keep ACH fields out of AMS data tables.

---

## 8. SQL close-out audit

**No SQL was produced, run, or recommended in this session.**

- No migration was written.
- No schema was described.
- No orphaned `.sql` files were created.
- No statement was recommended for Kevin to run.
- Nothing is pending deployment from this session.

The retention design in §7 would require schema when it is built. **It was discussed, not specified, and no table or column was named.**

---

## 9. Next

**Recommended next step:** the DataPath conversation (EXT-01, EXT-02, EXT-05). It is the only open item with exposure that exists today rather than exposure that arrives when documents are issued, and its answers change the drafting of two documents.

Everything else waits on Kevin working the register.
