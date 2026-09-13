# Enrollment matrix — where the seven flags live (s52e)

**Conclusion:** no existing reference entity has the tab grain. `ServiceItem` is one row per *elected service*, and one elected PremiumPath service item produces four of the tabs (plus the card-issuer seed). The flags need a **new PSP-scoped reference table, one row per enrollable benefit, FK'd to `ServiceItem`** — the gap the migration fills. `ServiceItem` is the runner-up.

## Step 1 — What enumerates the enrollable benefits

- **Election on a sale:** `ApplicationModule` (table `applicationmodule`, `@Entity` with default table name — `ApplicationModule.java:6-7`). Composite PK application × service item: `@Id @ManyToOne @MapsId("applicationId") @JoinColumn(name="application_id") Application application` (`:11-15`) and `@Id @ManyToOne @MapsId("templatePurposeId") @JoinColumn(name="template_purpose_id") ServiceItem serviceItem` (`:17-21`). `Application`'s PK is `proposal_id`.
- **Reference it points at:** `ServiceItem` — table `templatepurpose`, PK `int purpose_id` (`ServiceItem.java:11-17`), `@ManyToOne PSP psp` on `psp_id` (`:35-37`). Cardinality: `ApplicationModule` *N → 1* `ServiceItem`; inverse `@OneToMany(mappedBy="serviceItem") List<ApplicationModule>` (`:23-24`).
- **One path, three writers.** `LOS.serviceItem` (`LOS.java:55-57`, `@ManyToOne`, `service_item_id`) and `Enhancement.serviceItem` (`Enhancement.java:51-53`) are collapsed onto `ServiceItem` at sale time by `ApplyForProposal.java:473,483` and `CreateSetup25.java:150,160`; `AddSetupModule25.java:80` adds one directly. All go through `ActivityDAO.addModule` (`ActivityDAO.java:40-49`). `SummitExportServlet.loadElectedServiceItems` (`:512-527`) reads exactly this: `SELECT am FROM ApplicationModule am WHERE am.application.proposal.id = :pid` → `Map<Integer,String>` of `ServiceItem.id`. Every exporter (`employer`, `cdhplan`, `enrollment`, `cardseed`) starts here. `PlanType → ServiceItem` (`PlanType.java:33-35`) is the *inbound* mirror path (Benefit → PlanType → ServiceItem) and is not used by any exporter.

## Step 2 — Candidates against the constraints

| Candidate | PSP-scoped, Kevin-administered? | Grain |
|---|---|---|
| `ServiceItem` (`templatepurpose`) | Yes — `/ServiceManagerHome` + `/ServiceManagerAction`, `serviceManager25.jsp` | **Too coarse.** `docs/analysis/premiumpath_summit_plan_structure.md:4-5`: "One elected PremiumPath service item produces five Summit CDH plans"; the four tabs (ICHRA, excepted, off-exchange, post-tax) are plans 2–5 of that one item. Four flag-sets cannot sit on one row. Right grain for every other product (FSA, DCA, HSA: one item → one plan → one tab). |
| `summit_plan_template_map` | Yes — `/SummitPlanTemplateAdmin` | **Exactly the tab grain** (rows 2–5 = the four tabs; row 1 `is_card_issuer` = not a tab) — and ruled out by constraint 1. |
| `summit_service_item_flags` | Yes — `/SummitEmployerFlagAdmin` | Same grain as `ServiceItem` (one row per PSP × item, V101) — too coarse for the same reason, and it is Summit file-1 semantics. |
| `PlanType` / `Benefit` | No — inbound Summit mirror ("AMS mirrors plan types, never authors them"); `Ins125+` covers three tabs | Wrong grain and not in AMS before Summit. Out. |
| `LOS` / `Enhancement` / `ServiceModule` | Yes — Service Manager | Pricing/offering grain, N → 1 onto `ServiceItem`; PremiumPath is one LOS. Same coarseness. Out. |

**Recommendation.** A new PSP-scoped reference table — working name **`enrollment_benefit`** — one row per enrollable benefit, `service_item_id` FK to `templatepurpose`, several rows per service item allowed, following the scalar-FK convention of `SummitPlanTemplateMap`/`SummitServiceItemFlags` (`psp_id`, `service_item_id`, `label`, `sort_order`, `is_active`, audit columns) plus the seven flags. It is reached by resolver from the sale's elected `ServiceItem`s (build rule 4), renders with zero Summit rows (constraint 1), and lets FSA/HSA carry one row while PremiumPath carries four (constraint 4). **Runner-up: `ServiceItem` itself.** The single fact that flips it: if PremiumPath's four legs become four *elected service items* (reversing W3–W5's one-item→five-plans fan-out and quadrupling its setup-checklist keying), then one tab = one `ServiceItem`, the seven columns go on `templatepurpose`, and no new table is needed.

## Step 3 — The join

- **Tabs:** `Application(proposal_id)` → `ApplicationModule.application_id` → `ApplicationModule.template_purpose_id` → `ServiceItem.purpose_id` → **[GAP: `enrollment_benefit.service_item_id`, filtered `psp_id` + `is_active`, ordered `sort_order`]** → tabs. Entry key is the sale's `proposalId`, exactly as `loadElectedServiceItems` takes it (an employer pre-Summit is `Prospect` → `Proposal` → `Application`).
- **Detail row:** `EmployerParticipant(prospect_id)` × tab → **[GAP: no per-participant-per-benefit election entity exists.]** `writeHraEnrollment` takes the amount from intake answers (`SummitExportServlet.java:1409+`), `cardseed` writes a constant $1 — nothing today stores a participant's election against a benefit. That row is §5's subject, not this spec's.
- **Second gap, exporter-side only:** tab → Summit plan. Nothing links an `enrollment_benefit` row to its `summit_plan_template_map` row; the `125 PI Elections`/HRA writers will need that hop to route a tab's elections to a `template_id`. Named, not designed here.

## Step 4 — The seven columns (on `enrollment_benefit`)

| Column | Type | Default | Read by |
|---|---|---|---|
| `show_monthly_premium` | `TINYINT(1) NOT NULL` | `0` | matrix UI |
| `show_annual_election` | `TINYINT(1) NOT NULL` | `0` | matrix UI |
| `show_tier` | `TINYINT(1) NOT NULL` | `0` | matrix UI |
| `show_opt_out` | `TINYINT(1) NOT NULL` | `0` | matrix UI |
| `affects_payroll` | `TINYINT(1) NOT NULL` | `0` | payroll deduction report |
| `tax_treatment` | `VARCHAR(8) NOT NULL` | `'POST'` | payroll deduction report (grouping) |
| `is_importable` | `TINYINT(1) NOT NULL` | `0` | manual-entry report (`0` rows); matrix UI (lock/unlock cells) |

- All booleans default `0`: a new row asserts nothing until Kevin sets it, matching `summit_service_item_flags`' all-false defaults. `is_importable = 0` means manual — the safe default since importability needs a feed that exists.
- `tax_treatment` is a **code-validated string** (`PRE` | `POST`), not a boolean and not a MySQL `ENUM`, on the `effective_date_rule` precedent (`SummitPlanTemplateMap.java:71-76`). A boolean forecloses a third value the ICHRA tab already suggests (employer-funded, neither); an `ENUM` makes adding it an `ALTER` on a live table; a string makes it a constant. Default `POST` per LA-38's post-tax default.
- **The exporter reads none of the seven.** Tax treatment, funding source and method come from the Summit plan template, not the row (`premiumpath_summit_plan_structure.md:22-24`); the exporter only needs the tab→template hop named in Step 3.
- **Collisions:** none on a new table. If the runner-up wins, `templatepurpose` already has `sort_order`, `is_suppressed`, `source_type`, `code`, `description` — none of the seven collide, but `source_type` (`'CDH'`/`'COBRA'`) sits next to `is_importable` and must not be overloaded to mean it.
