# Session 28 close-out

Date: 2026-09-08. Branch: `refactor/modernize-architecture`. Baseline at session start: `df3e339`
(`docs: sync claude_memory.md for session 27`). Builds the Summit file 2 row set — one row per plan
the employer elected, replacing the single hardcoded ICHRA row — then **runs it in a browser**. Ends
at this commit.

⭐ **The headline: file 2 emits one row per elected plan, config-driven, and it is runtime-verified.**

⚠️ **The counter-headline, unchanged from session 27 and now carrying more weight: nothing has been
imported into Summit.** Generation is proven; acceptance is not.

---

## 1. Shipped

One code commit.

| Commit | What |
|---|---|
| `a17f32d` | `SummitPlanTemplateResolver` (new), file 2 emits one row per elected plan, keyed on `ServiceItem.id` |

This close-out and its backlog rows ship as `docs: session 28 close-out, backlog and doc corrections`
— the second and final commit of the session.

Six sub-runs produced this. **S28-A** was a read-only Phase A (Opus) that found the load-bearing fact:
AMS has no representation of a Summit CDH plan at all, and the sale is recorded only at `ServiceItem`
granularity. **S28-B** built the row set (Sonnet), keyed on `ServiceItem.code`. **S28-C** was a
read-only investigation (Opus) into why a live walk returned `(none)` — and found the key column is
unreachable from the UI. **S28-D** re-keyed on `ServiceItem.id` (Sonnet). **S28-E** committed and
pushed. **S28-F** is this close-out.

The shape of this session is worth naming: **two of six sub-runs were read-only investigations, and
one of them invalidated the build the previous run had just finished.** That is the process working,
not failing — but it worked because a browser walk was run between the build and the commit.

---

## 2. Runtime verification — the session's headline

> **Evidence class: Kevin's report from a browser walk on local dev, 2026-09-08. Not independently
> verified — no session tooling reached a browser or a database.**

Four behaviours were walked against **Red Creek Solutions** (prospect `136748`, proposal `136753`).

### The legacy path is byte-identical

With `SUMMIT_PLAN_TEMPLATES` unset, file 2 reproduces the `v0.94.00` row exactly:

```
1030|ICHRA 2026|158-136748-ICHRA-2026|ICHRA Plan 2026 for Red Creek Solutions|20260901|158-136748|20260901|20270831
```

This is the protection for a production installation that takes `a17f32d` without touching config. It
was verified live, not reasoned about.

### A mismatch lists the elected services

With the key deliberately mismatched, the 400 page named every elected service as `id=description`:

```
12=FSA, 17=Payment Services, 19=Debit Cards, 123054=HFSA, 123057=DCAP, 123060=PRA
```

⭐ **This error page is the discovery mechanism, not a diagnostic.** Keying on ids only works if an
operator can find an id without SQL, and this project does not ask anyone to run SQL. See §3.

### Two configured entries emit two rows

```
1030|Health FSA 2026|158-136748-FSA-2026|Health FSA Plan 2026 for Red Creek Solutions|20260901|158-136748|20260901|20270831
1030|Dependent Care FSA 2026|158-136748-DCAP-2026|Dependent Care FSA Plan 2026 for Red Creek Solutions|20260901|158-136748|20260901|20270831
```

Two rows, in config order, with **distinct `Import Plan ID` segments** (`FSA` / `DCAP`) and a **shared
employer key and plan year**. That is the whole point of the change, observed rather than inferred.

⚠️ **Template `1030` is deliberately wrong for both rows.** It is the ICHRA template and the only real
id available on this installation. Nothing has been imported, so a wrong template id cost nothing, and
the test was about **row count, ordering and key composition** — not about template correctness.

### ⚠️ What is still not verified

**No file has been imported into Summit.** Unchanged from session 27, and it matters more now: file 2
emits a **multi-row shape that nothing has ever accepted**. Every field requirement recorded in
`docs/business/summit_data_exchange.md` was established by importing a file and reading a results
file, never by reading an element picker — and that method has not been applied to this shape.

---

## 3. Decisions made

**The `ServiceItem → Summit plan` mapping lives in `ssa.properties`, not in a table.** One key,
`SUMMIT_PLAN_TEMPLATES`, format `<serviceItemId>:<templateId>:<keySegment>[:<label>]`, comma
separated, **config order is emit order**. Reasoning: template ids are Summit-assigned and differ per
installation, which is what config is for; a mapping table is schema other features would build on
before anyone knows its right shape; and a property reverses by editing one line and restarting
Tomcat. `SummitPlanTemplateResolver` is the seam a table would move behind, and **no caller would
change** — `configured()` is the only thing the servlet knows about. Filed as **D-90**.

**Keyed on `ServiceItem.id`, not `ServiceItem.code`.** See **T189**. The alternative was adding a Code
field to a shared admin surface and then hand-entering a free-text key on every service on every
installation, where a typo silently costs a plan row and the only feedback is a 400. The id is
non-null, immutable and already unique; the config is *already* installation-specific because template
ids are, so keying on another installation-specific value costs nothing new.

**Rule 4 is satisfied by config, not by avoidance.** No `ServiceItem` id literal appears in any
`.java`, `.jsp`, `.sql` or doc — **including javadoc examples**, which use `<ichraServiceItemId>`-style
placeholders. ⚠️ **Note for a future rule-4 grep:** `SummitPlanTemplateResolver`'s javadoc does carry
Summit template ids `1030`/`1031` in its worked example. Those are **external vendor values, not
PSP-scoped AMS reference rows**, and they are deliberate. A grep that flags them is finding
documentation, not a violation.

**The 400 error page is the discovery mechanism.** This is a design decision, not a nicety: it is the
only way an operator learns a `ServiceItem` id. It lists the elected services as `id=description` and
closes by naming the key, the file and the restart requirement.

**T185 stands: the plan year stays inside `Import Plan ID`.** Nothing has been imported, so reversal
cost is zero today and non-zero the moment a file lands. Keeping the year risks a **spare plan** at
renewal; dropping it risks a **renewal overwriting the prior year's plan**. The recoverable error was
chosen over the destructive one. ⚠️ **Confirm before the first production import** — that import
settles it either way.

**T186 unchanged, but no longer pre-committed.** The plan name still carries the year, but now derives
from the configured `label` rather than a string literal, so it **follows whatever T185 settles**
rather than deciding it in advance.

---

## 4. New assumptions

Registered as technical assumptions with reversal cost, in the manner of the LA-series:

**`ServiceItem.id` is stable for the life of a service.** It is a database primary key and nothing in
AMS renumbers it — but the config now depends on that, which it did not before. **Reversal cost: low**
— re-key the property. ⚠️ **Confirm before any process that recreates catalogue rows**: a re-seed, or
a migration that rebuilds `templatepurpose`, would silently break every configured mapping while
leaving the export apparently working (it would emit the legacy single row, or a 400).

**A per-installation config key is acceptable operational cost.** `SUMMIT_PLAN_TEMPLATES` must be
written by hand per installation, from ids discovered through the error page, and applied with a
Tomcat restart. **Reversal cost: low today, rising with each installation configured** — the migration
away from a property is mechanical while one installation carries it and a coordination problem once
several do.

---

## 5. Contradictions found

⭐ **The session 27 close-out was wrong that the multi-row sink had no consumer.** §7 of that document
said "the multi-row sink exists (S27-A) and has no consumer." It had one: `writeEmployerCdhPlan`
already called the `List<String>` overload via `Collections.singletonList`, and `writeDemographics`
called it with a real multi-element roster. **The plumbing was done; the missing piece was always the
upstream mapping.** This materially changed the size of the build — from "wire up an orphaned sink" to
"invent the elected-plan mapping that does not exist," which is a different and larger problem.

**S28-A described `ServiceItem.code` as "a free PSP-entered string."** The "PSP-entered" half is false
— nothing in the product has ever offered a field for it. **S28-B inherited the phrase verbatim into a
javadoc** before S28-C caught it, where it sat in the working tree describing the exact column the
build was broken on. ⭐ **This is the session's clearest instance of a claim surviving a code read and
dying on a browser walk.** Two runs read the entity, the write paths and the callers; neither noticed
that no *writer* existed for the column, because reading a getter tells you nothing about who calls
the setter.

**S28-A's claim that `DatabaseInitializer` seeds one LOS and one Enhancement is wrong — it seeds
none.** The block is commented out (lines 476–510). A fresh non-demo install has **no Setup
`ServiceItem` at all**; the catalogue is entirely Service-Manager-authored, or `DemoDataSeeder`-authored
on demo installs. This strengthens T189 rather than softening it.

**S28-C reasoned from four elected services; there are six.** The chip panel is scroll-clipped at
120px and `DCAP` and `PRA` were below the fold. ⭐ **A visible UI list was treated as a complete list,
and it was not.** Filed as **T188**. Worth noting that the error page built in the same session is
what exposed the discrepancy — the fix for the investigation's blind spot arrived before anyone knew
there was one.

**Two stale claims in `docs/business/summit_data_exchange.md`** — see §6. One corrected, one found not
to exist.

---

## 6. Doc corrections

**Corrected: the employer key is prefixed.** The client-setup-sequence passage describing File 1 read
`Employer TPA Custom ID` = `Prospect.id`. The shipped code emits
`{SUMMIT_TPA_ID_PREFIX}-{Prospect.id}`, and the session 27 production walk observed `158-136748`. This
is an **upsert key** whose prefix D-89 records as effectively irreversible once real records land, so
a doc that understates its shape is how a duplicate employer gets created under a second key.
Corrected in place; the LA-29 reference and the surrounding reconciliation argument are unchanged.

**⚠️ Not corrected, because it is not there: `plan_year_eligibility`.** The correction was to have
fixed any passage implying it is a table, entity or migration — it is a `templateKey` on an
`ApplicationSection` row, seeded from `src/main/resources/packages/s125_fsa.json` by `PackageLoader`
and attached to a LOS through the `applicationsectionlos` join table, with **no migration defining
it**. A grep of `summit_data_exchange.md` for `plan_year_eligibility`, `plan_year_start` and
`plan_year_end` returns **nothing**: the concept does not appear in that file in any form. The
misconception was carried in the **S28-A run prompt**, which asked for "the migration and entity," not
in any committed document. `deployment_backlog.md`'s D-89 mentions it and describes it correctly
("must be attached to the LOS being sold"). **No passage was invented to correct.** The fact itself is
recorded here and in `claude_memory.md` so it does not have to be rediscovered.

---

## 7. Defects filed

Four items, all filed into their backlogs **in this run** — not merely reported here.

| # | What | Where |
|---|---|---|
| T188 | The "Services To Implement" panel hides elected services below a scroll fold | `project_backlog.md` |
| T189 | `ServiceItem.code` is an unreachable column — no UI can set it, null on every non-demo installation | `project_backlog.md` |
| T190 | Renaming a LOS leaves its linked `ServiceItem.description` stale | `project_backlog.md` |
| D-90 | `ssa.properties` needs `SUMMIT_PLAN_TEMPLATES`, plus a Tomcat restart | `deployment_backlog.md` |

**T189 is the reusable one.** It is not really a defect report — it is the finding that a column
*looking* like a key is not evidence that anything can set it, and that the check is "who calls the
setter," not "does the field exist."

---

## 8. Open questions

**Does Summit accept these rows?** Nothing has been imported. **Settled by an import**, and by nothing
else — the standing rule of `summit_data_exchange.md` applies in full: "Optional" does not mean
optional, and requirements are discovered by importing a file and reading the results file.

**Does Summit model one plan across successive plan years, or one plan per year?** This settles T185,
and T186 behind it. **Settled by an import, or by DataPath.**

**What does an ICHRA sale's elected `ServiceItem` set actually look like?** ⚠️ Red Creek Solutions is
an **FSA/DCAP group with no ICHRA service in its elected set**, so the ICHRA row has never been emitted
from a real election — only from the legacy fallback. **Not a build item:** it needs an ICHRA LOS and
service in the catalogue, which is Kevin's data entry when testing requires it. Until then, the
configured path has been proven with FSA and DCAP standing in for the real plan set.

---

## 9. Next

**Import file 1 and file 2 into Summit.** Generation is proven and acceptance is not; every field
requirement in `summit_data_exchange.md` was established by importing and reading a results file; and
an import **settles T185 as a side effect**. D-89's remaining Summit-side work gates the Demographics
half — the Summit template still carries **nine** columns against an emitted **eleven**.

Cheap and open alongside it: **T187** (the mojibake in the three export link labels — a one-line fix,
still present as of this session's walks) and **T188**.

---

## 10. SQL close-out audit

- **Session 28 produced no SQL.** No migration written, no `.sql` file created, no schema change
  described, and **no run connected to a database**. Every run was fenced against it and every run
  complied — including the two read-only investigations, where going to look at the rows was the
  tempting move and was declined in both.
- **Highest migration in the tree: V094.** Unchanged all session.
- **Nothing pending deployment from this session** on the schema side. D-90 is an `ssa.properties`
  item and carries no SQL.
- **`migration_tracker.md` is untouched, deliberately**, because nothing in it changed. That includes
  the **disputed local `beta_ssa` cells for V092–V094** carried over from session 27, which this
  session **did not settle either** — no database having been queried. Flipping a cell on memory alone
  is the exact failure that file's maintenance note exists to prevent.
