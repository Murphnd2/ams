# Session close-out — 2026-09-14 — Demographics verification against the Summit participant export (S62)

Branch `refactor/modernize-architecture`. Six runs on one thread: S62-P1 (Opus, read-only survey),
S62-P2 (Sonnet, build), S62-P3 (commit/push), S62-P4 (Sonnet, verdict ordering), S62-P5 (Sonnet,
export scope + `pspId`), S62-P6 (commit/push), S62-P7 (this close-out). Every repo fact below was
re-read from the tree at close-out time — `git log`, `git show --stat`, the four source files, the
one edited JSP, and the three touched docs — not restated from the prompts that drove each run.

## Shipped

**Three commits**, each re-verified with `git show --stat` at close-out time, plus this close-out's
own fourth:

- **`12fafbc`** — "Demographics verification: read-only compare against Summit participant export
  (S62)". **6 files changed, 850 insertions(+)**, four newly created:
  `src/main/java/net/superiorstate/ams/data/service/SummitExportFetch.java`,
  `src/main/java/net/superiorstate/ams/data/service/SummitDemographicsVerifyService.java`,
  `src/main/java/net/superiorstate/ams/controller/market/SummitDemographicsVerifyServlet.java`,
  `src/main/webapp/WEB-INF/view/market/summitVerifyDemographics25.jsp`; plus one line added to
  `src/main/webapp/WEB-INF/view/a/activityDetail/columns/detail/detailSummitSetup25.jsp` and one
  line to `docs/analysis/technical_assumptions.md` (TA-55).
- **`2a75659`** — "Backlog: file T277, supersede T274, correct D-93 (S62)". **2 files changed, 9
  insertions(+), 6 deletions(-)**: `docs/analysis/project_backlog.md`,
  `docs/deployment_backlog.md`.
- **`f2706fb`** — "Demographics verification: distinguish never-pushed from missing, scope the
  export (S62)". **4 files changed, 309 insertions(+), 15 deletions(-)**: the three
  `SummitDemographicsVerify*` files plus `docs/analysis/technical_assumptions.md` (TA-56).
- **This close-out** — one new file, `docs/session_closeout_2026-09-14_demographics_verification.md`.

Confirmed via `git diff --name-only e05a65c f2706fb`: exactly eight files touched across the whole
session — the three docs and the five source files listed above. No file outside that set was
created, modified, or deleted anywhere in the session.

## In flight

**Nothing.** `git status --porcelain` at close-out start was empty against HEAD `f2706fb`.

## Decisions made — and what each closed

- **Verification is not mirroring.** S62-P1's own Q4 finding — read fresh from the tree, not
  restated from any prompt — is that `employee` and `benefit` must not get a scheduled,
  unconditional mirror the way `employer` (J1, `SummitRefreshService`) does, because a timer cannot
  know which of a hand-typed value or an imported one should win. `T274`'s row, as it now stands
  (`docs/analysis/project_backlog.md:334`), records this verbatim: *"the clobber problem this row
  itself documents (J2/J3 reverting AMS-typed contact names; J4/J5/J7's `is_active`/renewal
  side-effects) is exactly the reason `employee` and `benefit` should not get a scheduled,
  unconditional, table-wide mirror the way `employer` (J1) does... The recommended substitute is a
  per-setup, read-only export compare — the shape `IchraUncodedParticipantsCheck` already proves...
  now factored as `SummitExportFetch` (S62-P2)."* `T274`'s original text is preserved in the same
  row rather than deleted, per this project's standing supersession convention.
- **Demographics first, and why.** It is the only unproven push (of the four named in S62-P1's
  confirmability table — employer, plans, card issuance, enrollments) with an *observed* echo:
  `docs/business/summit_data_exchange.md:1007-1011`'s 2026-09-10 header/value observation that
  `ParticipantCustomID` is populated for every AMS-loaded participant. It also has a single-hop
  correlation key, an export already fetched by a shipped check
  (`IchraUncodedParticipantsCheck`, same `SUMMIT_AUDIT_PARTICIPANT_EXPORT_PREFIX` config family),
  and a `Check response` control that is structurally broken for this exact step — `T277`
  (`docs/analysis/project_backlog.md:404`), filed this session: *"`SummitResponseService.parse`
  classifies on `fields[0]`... on Demographics and HRA Enrollment responses the first field is the
  participant key, so every line renders `UNKNOWN`... 'Check response' is therefore informative
  only for `employer` and `cdhplan` today."*
- **`SummitExportFetch` created as a new file; the two existing duplicates left untouched.**
  `SummitExportFetch.java` (170 lines) extracts the fetch/select/read/BOM-strip sequence as a
  reusable, stateless, read-only helper. `IchraUncodedParticipantsCheck.java` and
  `SummitRefreshService.java` were never opened for edit in any of the six runs (confirmed: neither
  file appears in any of the three commits' file lists above) — each keeps its own independent copy
  of the same logic. This is a deliberate extraction-target-first, migrate-later choice, recorded in
  `SummitExportFetch`'s own class Javadoc: *"This is the extraction target for duplicated fetch
  logic, not yet a shared one... Neither has been migrated to this class."*
- **`Verdict.EXPORT_PREDATES_PUSH` declared but deliberately unreachable.** Read fresh from
  `SummitDemographicsVerifyService.java:366-378` at HEAD: comparing the export filename's timestamp
  (Summit's own clock, unknown zone) against `SummitFileExport.getDeliveredAt()`
  (`LocalDateTime.now()` on the AMS JVM, `SummitExportServlet.java:2301`) risks a wrong
  "predates"/"postdates" verdict, because **D-104** (`docs/deployment_backlog.md`, read verbatim at
  close-out) already documents an unresolved clock interaction: *"The filename timestamp is
  Summit's local time (CST); if the server runs UTC the computed age reads 5–6 hours too old...
  inside the 26-hour default, but by only a ~2-hour margin."* The code's own comment states the
  judgment plainly: *"per this build's own instruction a wrong staleness verdict is worse than
  none -- so this step is skipped entirely."* The enum value and its JSP banner are still written
  (S62-P4's own instruction was to declare the full spec even where one branch is unreachable), so a
  future sound comparison would render correctly without a second build.
- **`pspId` moved to the session, removing an unproven invariant rather than documenting it.**
  S62-P4 first resolved `pspId` via `proposal.getRate().getPsp()` (an entity-graph path, since the
  service has no `HttpServletRequest`) and documented it as *"a disclosed, unverified assumption,
  not a proven invariant."* S62-P5 replaced this: `SummitDemographicsVerifyServlet` now resolves
  `pspId` from the session via a `resolveCurrentPspId` method copied verbatim from
  `SummitSetupStatusServlet.java:123-129` (confirmed session-based, not proposal-derived, at
  close-out — `local.getCurrentPerson().getPsp()`), and passes it into `verify(...)` as a parameter.
  `SummitDemographicsVerifyService.java` no longer imports `Proposal` or `PSP` at all (confirmed:
  grep for both at HEAD returns only Javadoc/comment text, no live code reference) and the
  unproven-invariant paragraph is gone from the class Javadoc, not merely superseded.
- **`T252` was not extended, and this is consistent with the row's own existing scope.** Read
  `T252` fresh (`docs/analysis/project_backlog.md:312`): its list of five fragment servlets needing
  to change together for a PSP-user access widening is `SummitSetupStatusServlet`,
  `SummitEmployerLinkServlet`, `CensusRequestStatusServlet`, `CardIssuerAvailabilityServlet`,
  `CensusLifecycleServlet`. `SummitResponseServlet` — a standalone linked page reached the same way
  `SummitDemographicsVerifyServlet` is, not a fragment embedded via `c:import`/`jsp:include` — is
  **not** on that list either. The new servlet's absence from `T252` was a deliberate S62-P1
  judgment call (recorded in that run's own report), and re-reading the row now confirms it is
  consistent with the row's established scope, not an oversight.

## New assumptions

Two entries in `docs/analysis/technical_assumptions.md`, quoted verbatim from the tree at HEAD:

- **TA-55** (`:77`): *"the participant-list export's `ParticipantCustomID` echoes AMS's composed
  `{prefix}-P-{employer_participant.id}` verbatim, and a blank value marks a Summit-UI-added
  participant."* **Basis:** a single observed export header and values on 2026-09-10
  (`docs/business/summit_data_exchange.md:1007-1011`). **Risk if wrong:** false `MISSING` for a
  participant whose key Summit stored differently, or false `FOUND` on a coincidental collision.
  **Reversal cost: none** — the service persists nothing (LA-40). **Updated 2026-09-14 (S62-P4):**
  the first live attempt (proposal 141504) returned zero matching rows *only* because no
  demographics push had been recorded for that proposal, not because the key failed to echo.
  **Plainly: TA-55 has never been runtime-exercised.** No participant has been confirmed `FOUND`
  by this compare against a real export at any point in this session.
- **TA-56** (`:78`): *"the export reached via `SUMMIT_AUDIT_PARTICIPANT_EXPORT_PREFIX` is filtered
  Summit-side to employers on PremiumPath or a regular ICHRA."* The filter is employer-level, not
  participant-level — within an in-scope employer every participant appears, so the per-participant
  compare stays sound; an out-of-scope employer produces zero rows regardless of what was pushed.
  **Basis:** Kevin read the Summit-side export template, 2026-09-14 — a Summit configuration fact,
  reported here and not derivable from anything in this repo. **Risk if wrong:** if the filter were
  participant-level rather than employer-level, a `MISSING` result would mean "not in this filtered
  subset" rather than "not in Summit," and the per-participant compare itself would be invalid for
  an in-scope employer, not merely scope-limited. **Reversal cost: low** — pointing the prefix at a
  different, unfiltered export is one `ssa.properties` edit; nothing is persisted.

## Open questions raised — and who or what settles them

- **TA-55 remains unexercised.** Needs an in-scope employer (PremiumPath or regular ICHRA) with a
  recorded demographics push, so the compare has a real `FOUND` case to test against. Kevin's
  setup/testing, not a build item.
- **A stale export with a recorded push lands on `EMPLOYER_KEY_ABSENT`** (or
  `PARTICIPANTS_UNCONFIRMED`), and the banner names the possibility ("this export's age relative to
  the push cannot be established") but cannot rule it out. Settled only by a sound, zone-qualified
  timestamp basis for both sides of the comparison — which D-104 currently denies. Not a build item
  until D-104's clock question is independently resolved.
- **`pspId` asymmetry.** `SummitSetupStepDAO.findLatestDeliveryAttempt` filters by `pspId`;
  `findByProposalAndStep` deliberately does not (its own class Javadoc: *"Deliberately not filtered
  by `pspId`"*). A viewer whose session PSP differs from the PSP recorded on a past push would see
  `NO_PUSH_RECORDED` for a proposal that genuinely was pushed. Theoretical at an installation with
  one PSP (this one, per every prior session's own framing); would need a second PSP or a
  cross-PSP-admin scenario to actually surface.
- **Two copy-paste debts in one feature area, both deferred.** The export-fetch fetch/select/read
  sequence now exists in three independent copies (`IchraUncodedParticipantsCheck`,
  `SummitRefreshService`, `SummitExportFetch`) rather than one shared implementation.
  `resolveCurrentPspId` is now a third independent copy (`SummitSetupStatusServlet`,
  `SummitResponseServlet`, `SummitDemographicsVerifyServlet`) under the same T254 precedent this
  project has already accepted for that specific helper. Neither is fixed here; both were explicitly
  out of scope for every run in this session.
- **Carried forward from S62-P1 §7, unsettled, and gating everything past Demographics.** Two
  questions with nothing in the tree to answer them: whether one real J4 export (of an actual
  AMS-pushed plan) has been pulled by hand — plans verification cannot be designed against a
  hypothesis about what Summit echoes back; and whether a J6-shaped enrollment export exists on the
  tenant at all, and what its bare `CustomID` column denotes — card-issuance and enrollment
  verification both wait on this. Neither question was investigated or answered by any of the six
  runs in this session; both remain exactly as S62-P1 left them.
- **T276** (`/SummitRefresh`'s status page still claims a 17-digit-only timestamp, stale since
  `b6f1864` widened acceptance to 14–17) remains open, ships alone, untouched by this session.

## Contradictions found

1. **"The tree is CRLF"** — asserted in the S61 close-out and carried verbatim into S62-P2's own
   prompt as a load-bearing warning ("the single most likely way this run fails"). **Re-verified at
   this close-out, independently, three ways, and confirmed false**: `od -c` on the head of
   `SummitDemographicsVerifyService.java` shows bare `\n` bytes with no `\r`; `tr -cd '\r' | wc -c`
   returns `0` on that file, `SummitDemographicsVerifyServlet.java`, and
   `summitVerifyDemographics25.jsp`; and `git show HEAD:<path> | tr -cd '\r' | wc -c` on the
   committed blob itself also returns `0`. `git config core.autocrlf` is `true` locally, which
   explains the `git add` "LF will be replaced by CRLF" warnings seen in every commit this session,
   but not the actual on-disk or in-blob bytes, which are LF throughout. **No effect on any result**
   — every edit this session used exact-content matching (the `Edit` tool), never a line-ending
   assumption, so the false claim cost nothing beyond the verification step itself.
2. **S62-P2's build spec would have produced a silently blank page.** It specified forwarding the
   `SummitDemographicsVerifyService.Result` record to the JSP directly. Records generate accessors
   with no `get` prefix (`outcome()`, not `getOutcome()`); JSP EL property resolution is
   JavaBean-getter based. Followed literally, every `${result.outcome}`-style expression would have
   resolved to nothing and the page would have rendered as an empty shell — no exception, no error,
   just silence. Caught before compiling, against `SummitResponseServlet`'s own already-committed
   precedent (`SummitResponseServlet.java:174-177`, read and cited at build time), which documents
   exactly this EL limitation and flattens its own records into scalar request attributes for the
   same reason. Same defect class as item 1: a spec detail asserted as settled where a five-minute
   check would have caught it.
3. **S62-P1 found two claims in its own prompt wrong, and corrected both without waiting for a
   later run.** First: the prompt characterized D-93 as needing one missing `SUMMIT_IMPORT_TEMPLATES`
   entry; the S56 close-out (`docs/session_closeout_2026-09-13_two_file_export.md:52`) had already
   declared two (`enrollment:` and `elections:`), and D-93's own row had never been updated to say
   so — corrected in this session's second commit (`2a75659`). Second: the prompt's own framing
   leaned on `IchraUncodedParticipantsCheck` as proof that "verification needs no writes"; on
   inspection, that class proves the read-only *fetch* shape but joins to no AMS record at all (it
   is a table-wide rule filter, not a per-setup correlation), so it narrows — without reversing —
   the evidence for the eventual per-setup design S62-P2 through P5 actually built.
4. **No further contradictions found** between this prompt and the tree as re-read at close-out
   time. Every commit hash, file list, insertion/deletion count, TA/T-number text, and D-93 status
   named in this prompt matched what `git show --stat` and the docs themselves carry at HEAD
   `f2706fb`.

## Next

**Arm TA-55's own test.** Everything else this session built is inert until it is exercised against
a real `FOUND` case: an in-scope employer (PremiumPath or regular ICHRA, per TA-56) with a demographics
push already recorded. That is the cheapest next step with the highest information value — it either
confirms `ParticipantCustomID`'s echo (upgrading TA-55 from "observed once" to "runtime-verified," the
same upgrade TA-51 got in S61), or it surfaces a defect this compare was built specifically to catch.
Everything else recorded above as "open" (the `pspId` asymmetry, the two copy-paste debts, plans/card/
enrollment verification) is lower-value or blocked on an external fact nobody has gone and read yet.

## SQL close-out audit

**This session produced no SQL, no migration, and no schema change whatsoever, across all six runs.**
Verified, not asserted:

- `git diff --name-only e05a65c f2706fb -- '*.sql'` returns nothing — no `.sql` file was created,
  modified, or referenced by any commit in this session.
- `git diff --name-only e05a65c f2706fb -- 'docs/migrations/'` returns nothing — the migrations
  directory is untouched.
- **No `constant` row was inserted and no `ssa.properties` key was added.** The only occurrence of
  the string `ssa.properties` anywhere in this session's diff is inside TA-56's own text, describing
  a *hypothetical future* reversal ("pointing `SUMMIT_AUDIT_PARTICIPANT_EXPORT_PREFIX` at a
  different, unfiltered export is one `ssa.properties` edit") — not an edit this session made.
- **Current highest migration in the tree: V113** (`ls docs/migrations | sort -V | tail -1` →
  `V113__enrollment_matrix_participant_agent_note.sql`), unchanged by this session. A stray
  `seed_ndt125_questionnaire.sql` also sits in that directory (not V-numbered, pre-existing,
  untouched, not investigated further here).
- **What is pending deployment:** unchanged by this session in every respect. This session shipped
  no migration, so there is nothing new to apply anywhere, no version to bump, and no release to cut.
  Whatever V105–V113 pending-deployment state existed before this session (per prior close-outs)
  remains exactly as it was; this session neither resolves nor depends on it.
- **Schema described but not scripted:** none. Every build item this session completed (three new
  Java files, one new JSP, one control added to an existing JSP, three doc edits, one appended TA
  line) required no schema change, and none was described as future work either — the compare
  itself persists nothing (LA-40), by design, in both its first build (S62-P2) and its refinement
  (S62-P4/P5).
- **Nothing from this session is deployed.** No migration means no version bump and no GitHub
  release. `ROOT.war` was never built in any of the six runs — every `mvnw` invocation this session
  was `compile`, never `package`, and none touched a database or a remote host beyond the plain
  `git push` at the end of S62-P3 and S62-P6.

**No SQL was produced, executed, or proposed by any run in this session.**
