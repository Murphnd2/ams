# Session 47 close-out — 2026-09-11

## Session shape

- S47 opened strategy-first and was pointed at priority-2 enrollment scoping.
- Kevin redirected it: priority 1 is not done. It covers the whole of Summit setup steps 1–5: the
  census chain (request, client drop, review, PSP upload, Demographics push) plus the employer and
  plan items. That includes the Summit-side work only Kevin can do.
- `docs/analysis/priority1_checklist.md` now defines done, with rows owned by Claude Code and by
  Kevin.
- Four Phase A runs were done (s47a, s47b, s47e) plus the builds (s47c, s47f, s47g, s47i) and the
  commits (s47d, s47h, s47j).

## Shipped

- `653062d`: census build 1. Request, public client drop, staged rows (V100).
- `ff7cb58`: docs. D45, D46, LA-41, SDX-27/28, the priority-1 checklist, and the build 1 walk.
- `ed40cd9`: census build 2. PSP review, Load/Reject, the Clear guard (D45 f), and upload activity
  entries by outcome.
- `9bb4748`: docs. Build 2 walk, D45 build-2 notes and the walk amendment.
- `7f1f8fe`: T238 part 1. Employer-flag config, admin page, resolver (V101).
- `88c08c3`: docs. Flag config walk, the FSA line-of-service model.

Runtime-verified locally 2026-09-11 (KEVIN-UI):

- build 1 (9 items, storage checked by SQL);
- build 2 plus fixes (7 items plus 3 re-checks);
- the flag config (6 items);
- requester notification emails, 3 of 3.

## In flight

Nothing uncommitted except the pre-existing `.idea/artifacts/ams_war_exploded.xml`.

## Decisions

- **Priority 1 redefined (Kevin):** steps 1–5 including the Summit-side rows. The checklist is the
  definition of done.
- **D45:** census intake, a–f. Rows, not files; lenient parse for client uploads; a 30-day token;
  SendProposal-pattern request email; review with a roster comparison; replace until Demographics
  is settled.
  - Build-2 notes:
    - system-seeded status and reason ids as named constants;
    - the Received Email reason for uploads;
    - the diff key is name plus ZIP5;
    - Load into an empty roster is always allowed;
    - Load is retry-safe;
    - Reject keeps the token and extends the expiry.
  - Walk amendment: a clean upload is Waiting on Us; an upload with issues, or unreadable, is
    Waiting on Them.
- **D46:** file 1 emits all four flags as explicit true/false. It amends D44's "true or blank" and
  is pending SDX-27.
  - Design note: column order is the 6 mandatory columns, then Employer Plan Name, then the 4
    flags, which satisfies the trailing-optional rule.
- **Employer flags live in their own table (V101), not on the template map**, because a
  flag-bearing item may have no template.
- **Kevin's model:** a line-of-service item such as FSA maps to nothing; its enhancements (HFSA,
  DCAP) carry the templates and flags.
- **SDX-28**, a COBRA-only plan on its own template row, is preferred over SDX-26 pairing. It
  avoids V095's unique-key change and the new-key-segment duplicate plan. Pending test.
- **Branding:** the census drop keeps `OriginatingAgencyResolver`, matching the request email. Its
  divergence from `/apply/*` is recorded, not changed.

## New assumptions

- **LA-41:** staged whitelisted rows and `summit_file_export.content` are the roster's own data
  class, so D30 is not reversed. Reversal cost: low.
- **Technical, `interpretLenient`:** it duplicates the strict parser's header logic, so a synonym
  change needs both. Reversal: extract a shared method.
- **Technical, status and reason ids:** `ActivityStatus` and `ReasonCreated` ids are fixed by
  `DatabaseInitializer`. Reversal: a lookup by description.
- **Technical, roster diff key:** `lower(first)|lower(last)|zip5`. Reversal: a display edit.
- **Technical, flag union:** the emitter must refuse when no flag is true after the union.
  Guidance only.

## Open questions (who settles)

- **Kevin, in the Summit UI:**
  - SDX-27, explicit `false` on create and on update, using a throwaway employer;
  - the SDX-24 value import;
  - SDX-26 and SDX-28;
  - adding the flag elements and Employer Plan Name to the import template;
  - D-97.
- **Kevin, at the emitter build:** the Employer Plan Name COBRA-collision rule, and the "no
  template" warning noise for line-of-service items.
- **Kevin, before the Forrest demo:** should client links use the agency's `landing_host` rather
  than the admin's host? This affects proposal links too.
- **Kevin, data:**
  - the template-map service item 12 versus HFSA `123054`;
  - a successful-Load walk on a setup whose Demographics isn't settled.
- **Carried:**
  - priority-2 enrollment scoping (T201, SDX-12);
  - the SWBD asks (O22) are still unsent per S46;
  - the demo case, Sandoval with a 9/1/26 effective date, is past its date and its status is
    unknown;
  - the `ichra_strategy.md` §4 rewrite is owed;
  - the production server's timezone;
  - T236;
  - the LA-08 notice document.

## Contradictions found

- **s47a (census):**
  - Census Upload refuses rather than replacing or appending.
  - LA-34's wording was corrected.
  - D30 conflicts with `summit_file_export.content`; resolved by LA-41.
  - The S27 empty-file behavior was superseded by T209.
  - D29's Option 1 was retained.
  - SDX-18 was stale; noted.
- **s47b (Summit):**
  - `summit_data_exchange.md` said ICHRA needs the COBRA flag; supersession notes were added.
  - D44's "extend V095" is impossible under its unique key.
  - The ICHRA plan is identified by the literal key-segment string `ICHRA`.
  - The `SummitPlanTemplateResolver` and `writeEmployerCdhPlan` javadocs are stale; left alone.
  - D-97 conflicts with blank-flag semantics.
  - HFSA has never been pushed.
  - T238's undefined "Decision B" was fixed.
- **s47e (build 2):**
  - Two different agency resolvers exist (`/apply/*` versus the census drop).
  - Pre-existing literal ids 1 and 7 in SendProposal.
  - No client-upload reason is seeded.
  - `Activity.getOnUs()` reads a cached note list.
  - LA-34's Assumption line is unchanged.
- **Pre-existing, left alone:**
  - the Census Upload refusal banner says "Nothing was inserted" when Clear is refused;
  - Census Upload's Clear section still shows when the guard will refuse it.
- The project-knowledge copies of `ichra_strategy.md` and `swbd_ichra_build_plan.md` are baselined
  at V076.

## Next (recommendation; Kevin chooses)

1. **Kevin, in the Summit UI:** SDX-27, then the SDX-24 value import. SDX-27 unblocks the file 1
   emitter (T238 part 2), the only remaining priority-1 Claude Code build on the employer side.
2. **Kevin:** SDX-28, then SDX-26. These decide whether plan pairing needs any build at all.
3. **Kevin:** the template-map item 12 check, and the successful-Load walk.
4. **Kevin:** the Demographics push walk (step 5), production retrieval, then the release.

## SQL close-out audit

- **Produced:** `V100__census_intake.sql` and `V101__summit_service_item_flags.sql`. Both are
  versioned migrations, registered in `migration_tracker.md` and `schema_version_migration.sql`
  (verified: both rows present in each file; tracker header reads "Current Highest Version: V101").
- **Run:** Kevin applied V100 and V101 locally. Claude Code ran none. Kevin also ran one read-only
  diagnostic `SELECT` on `census_submission`. It is not schema and needs no migration.
- **Orphaned `.sql` files:** `docs/migrations/seed_ndt125_questionnaire.sql`, pre-existing and
  logged as T38. Not touched.
- **Highest migration:** V101 (verified: `ls docs/migrations` lists no version past V101).
- **Pending production:** V097, V098, V099, V100, V101 (verified: each row's Production column
  reads ⬜ in `migration_tracker.md`). The next release is `v0.101.00`, carrying them plus C1, after
  D-96, D-97 and D-98.
- **Constant rows:** none new. D-98 (`AUDIT_SCHEDULER_ENABLED`) is carried.
- **Schema described but not scripted:** none.
