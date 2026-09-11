# Session 48 close-out — 2026-09-11

## Session shape

- S48 opened strategy-first (the project-knowledge copy of `ichra_strategy.md`, baselined at V076),
  then read the S47 close-out.
- It un-gated T238 part 2 from SDX-27 by registering SDX-27 as an assumption: building the emitter
  is reversible; importing its output for a real employer is not. Kevin then ran SDX-27 in Summit
  the same day, so the assumption was tested and closed.
- Runs: s48a — Phase A (Opus); s48b, s48b2, s48b3 — builds (Sonnet); s48c — commit plus docs; s48z —
  this close-out.
- A parallel thread the same day — Texas TPA licensing, LA-10 — was done in an outside session. It
  is folded in here as part of this session, not as a separate one.

## Shipped

- `7bfa5ff`: docs. LA-10 licensing amendment (outside thread).
- `bcb7aa7`: docs. LA-10 decision, "Option B interim, Option A on trigger" (outside thread;
  superseded the same day — see Decisions).
- `add58f9`: T238 part 2. File 1 emits the legal name, the Employer Plan Name allowance text, and
  the four Summit flags (D46, D47). This is s48b + s48b2 + s48b3.
- `f455ad6`: docs. D47; SDX-24, SDX-27 and SDX-29; the D-97 residual; D-99; the final LA-10
  decision; the checklist rows; T238, T239 and T240.
- This close-out's own commit: its hash goes in the chat report, not in this file.

Runtime-verified locally 2026-09-11 (KEVIN-UI) — seven file 1 walk items:

- a sale with no ICHRA → empty Plan Name;
- the QSEHRA label: `QSEHRA allowance: $500.00/month ($6000.00/year)`;
- a blank legal name refused (the pre-s48b3 build);
- a blank legal name falls back to the prospect name, with a WARN;
- no flag rows → refusal listing `id = description` and marking the unmapped items;
- COBRA true → Employer Plan Name suppressed, with an INFO line;
- an unparseable `hra_annual_ee` (`6,000`) → empty column 7, with a WARN.

Runtime-verified in Summit 2026-09-11 (KEVIN-UI):

- the 11-element Employer Demographic template;
- SDX-24;
- SDX-27, create and update;
- SDX-29.

Code-verified only:

- the WARN when more than one HRA template matches;
- the all-false-rows refusal;
- the no-PSP refusal;
- the ICHRA-positive path (local has no ICHRA template);
- a 47+ character Employer Plan Name in Summit.

## In flight

Nothing uncommitted except the pre-existing `.idea/artifacts/ams_war_exploded.xml`.

## Decisions

- **D47 — file 1 column sources.**
  - (a) Employer Name: the legal name, falling back to the prospect name with a WARN, and refusing
    only if both are blank.
  - (b) Flags: the union over the same election source as file 2, and three refusal messages (no
    PSP in session, no mapped rows, rows exist but all false).
  - (c) Allowance text, driven by `SUMMIT_ALLOWANCE_KEY_SEGMENTS`, with the amount from the shared
    `hra_annual_ee` answer.
  - (d) COBRA suppression, with its conditional reversal.
  - (e) The key segment's three roles: the `Import Plan ID` upsert segment, file 4's ICHRA
    selector, and now also the notice label.
- **N1′ (Kevin):** a blank legal name falls back to the prospect name. This reverses s48b's
  no-fallback rule, and supersedes the S25-C `Prospect.name` rationale.
- **Allowance text is config-driven for every HRA type (Kevin).**
  - One `hra_annual_ee`, from the shared "105 Benefit Allocation" section used by every HRA type:
    ICHRA, QSEHRA, EBHRA, Traditional HRA, MERP and DRiP.
  - `SUMMIT_ALLOWANCE_KEY_SEGMENTS` lives in `ssa.properties`. Its local value is `ICHRA,QSEHRA`,
    and `AppConfig.get` reading it from there was runtime-verified.
- **The mapping table is live locally.** `/SummitPlanTemplateAdmin` now has three rows: HFSA
  123054 → `FSA` (template 1000); DCAP 123057 → `DCAP` (1001); QSEHRA 135541 → `QSEHRA` (1034). Two
  consequences: `SUMMIT_PLAN_TEMPLATES` is no longer read locally; item 12 (the FSA line of
  service) now maps to nothing, which settles S47's item-12-versus-HFSA question.
- **LA-10, final (Kevin, S48).** Licensing is approached when sales volume warrants. No service is
  excluded or withheld from Texas employers in the interim. No design safeguard is enforced or
  planned. This supersedes both `bcb7aa7`'s Option B sales constraint and its proposal-stage
  trigger.
- **D-97 → residual only.** Two items remain: review the production flag rows before the first
  real push; keep the template defaults unchecked.
- **The COBRA-collision question from S47 is closed by N5 (D47(d)).** Kevin intends to reword the
  COBRA general notice paragraph so it needs no plan name, which is what would let N5 be reversed.

## New assumptions

- **Technical:** a 47+ character Employer Plan Name is untested in Summit. Only 46 characters has
  been shown to store. Reversal: shorten the format string.
- **Technical:** a 6-column production file against the 11-column template. How Summit handles it
  is untested. It is avoided by release order (D-99 caution). Reversal: none needed if the order
  holds.
- **Technical:** SDX-27 on create. An honored `false` can't be told apart from the unchecked
  default. It is harmless while the defaults stay unchecked, which is the D-97 residual.
- **Technical:** the key segment now also serves as the notice label (D47(e)). Reversal: a label
  source separate from the segment.
- **Legal — LA-10, amended in place (outside thread):**
  - reliance on *NGS American* for the ERISA slice. This is thin: it predates the 2003
    recodification as ch. 4151. Reversal cost: licensure, possibly retroactive;
  - that holding a certificate does not waive *NGS* is unverified;
  - advance-and-recoup vs prong 1 is thin; rely on it in neither direction;
  - the S48 final decision sets the status to accepted risk.

## Open questions (who settles)

- **Kevin, in the Summit UI:**
  - SDX-28, then SDX-26;
  - the COBRA general notice reword;
  - which other letters merge `EmployerPlanName`. This and the reword together gate D47(d)'s
    reversal.
- **Kevin, before production:**
  - the D-99 data: `SUMMIT_ALLOWANCE_KEY_SEGMENTS`, the mapping rows added all at once, and the
    flag rows;
  - the D-97 residual;
  - the release-order caution;
  - D-96 and D-98.
- **Kevin, priority 1 remainder:**
  - the Demographics push walk (step 5);
  - production retrieval;
  - the release.
- **Kevin, LA-10:**
  - are there non-ERISA Texas participants in the existing book?
  - does SSA collect Presidio premiums from Texas residents?
- **Counsel or a TDI letter, when volume warrants:** does *NGS American* survive the
  recodification? And the multi-state question, per LA-10.
- **Carried from S47:**
  - `landing_host` for client links, before the Forrest demo;
  - a successful-Load walk on a setup whose Demographics isn't settled;
  - the file 2 "no template" warning noise for line-of-service items;
  - priority-2 enrollment scoping (T201, SDX-12);
  - O22 (still unsent);
  - the status of the Sandoval demo case;
  - the `ichra_strategy.md` §4 rewrite;
  - the production server's timezone;
  - T236;
  - the LA-08 notice document.

## Contradictions found

- **s48a:**
  - It said the test Employer Plan Name value was 45 characters. It is 44.
  - It cited `docs/analysis/summit_data_exchange.md`; the file is at `docs/business/`.
- **claude.ai (s48), two claims corrected against runtime:**
  - "local AMS already has template rows" was wrong: file 2 was reading the
    `SUMMIT_PLAN_TEMPLATES` property;
  - the QSEHRA service item is 135541, not the 135540 inferred from the enhancement URL.
- **D46 "D-97's defaults no longer apply" vs D-97's S48 residual** (flagged in s48c). This is only
  apparent: D46 holds for every tested case, and the residual covers the one untested corner. No
  doc change.
- **The D-97 checklist row stays open although SDX-27 closed.** That is correct: the flag-row
  review is new residual work.
- **Outside thread:**
  - LA-10's basis, "SSA never holds participant funds," was false: SSA collects COBRA premiums and
    remits them. Corrected in `7bfa5ff`.
  - The Advanced Research report is superseded wherever it conflicts with LA-10. Do not add it to
    project knowledge as authoritative.
- **`bcb7aa7`'s Option B QSEHRA+ constraint conflicted with the 2026-07-14 Forrest model** (the
  staff-QSEHRA leg). It was withdrawn by Kevin's final decision.
- **Stale notes filed as T240:** T205's status, and `SummitCdhElementResolver.bool`'s "UNPROVEN"
  Javadoc.
- **Project-knowledge copies are stale:**
  - `ichra_strategy.md` and `swbd_ichra_build_plan.md` (baselined at V076);
  - `legal_assumptions.md`, which predates `7bfa5ff`, `bcb7aa7` and `f455ad6`, and still carries
    the false "never holds participant funds" basis.

## Next (recommendation; Kevin chose)

1. **S49 opens with the T239 Phase A (Opus):** the setup/activity company name, from the legal
   name, falling back to `Prospect.name`. Kevin chose this. It is the only open Claude Code item,
   and it touches the existing setup pipeline for every LOS. The Phase A must establish: whether
   the name is copied at setup creation or read live; every surface that shows it; the smallest
   safe diff. Ship it alone.
2. **Kevin:** SDX-28, then SDX-26.
3. **Kevin:** the Demographics push walk (step 5), the D-99 production data, then `v0.101.00`.
4. **Kevin, project knowledge:** replace `legal_assumptions.md` with the repo copy; save this
   close-out.

## SQL close-out audit

- **Produced:** none. No migration, no schema, no `.sql` file from any s48 run (verified: highest
  migration in tree is still V101, unchanged since session 47).
- **Run:** Claude Code ran none. Kevin ran one read-only diagnostic
  `SELECT * FROM beta_ssa.applicationfieldvalue ORDER BY application_id DESC` (local, 11:13). It is
  not schema and needs no migration.
- **Data changed without SQL, all local, all via the admin UI or properties:**
  - `summit_plan_template_map`: three rows;
  - `summit_service_item_flags`: rows edited and restored during the walk;
  - `ssa.properties`: `SUMMIT_ALLOWANCE_KEY_SEGMENTS=ICHRA,QSEHRA`;
  - proposal 141303's application answers.
- **Summit test data (not AMS):**
  - `ZZSDX27A` created (system id 1394, inert);
  - ZZTESTCompany 9102's Employer Plan Name cleared.
- **Orphaned `.sql` files:** `docs/migrations/seed_ndt125_questionnaire.sql`, pre-existing and
  logged as T38 (verified: the only file under `docs/migrations` not matching `V###__*.sql`).
- **Highest migration:** V101 (verified: `ls docs/migrations` lists no version past V101).
- **Pending production:** V097, V098, V099, V100, V101 (verified: each row's Production column
  reads ⬜ in `migration_tracker.md`). The next release is `v0.101.00`, carrying V097–V101, C1 and
  `add58f9`, after D-96, D-97 (residual), D-98 and D-99.
- **Constant rows:** none new. D-98 (`AUDIT_SCHEDULER_ENABLED`) is carried.
  `SUMMIT_ALLOWANCE_KEY_SEGMENTS` is an `ssa.properties` entry, not a constant row, and is tracked
  as D-99(a).
- **Schema described but not scripted:** none.
