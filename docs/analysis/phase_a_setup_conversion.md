# Phase A — The setup conversion surface

Branch `refactor/modernize-architecture` · HEAD `c5d66f6` (as expected) · 2026-09-07 · read-only.
Working tree carried one untracked file at start: `docs/analysis/phase_a_participant_identity.md` (S26-A).

## Answers

### Q1 — The Setup surface

**URL:** `/ViewById?id={activityId}` — `controller/activity/ViewById.java:9`, whose `goToPage` sets
`btnViewActivity` and forwards to the named dispatcher `GoActivityDetail25` (`:27-32`).
`/GoActivityDetail25` (`GoActivityDetail25.java:18`) is the same page reached directly.

**JSP:** `WEB-INF/view/a/activityDetail/activityDetail25.jsp` (468 lines) — three columns: checklist
(`:235-252`), detail (`:266-340`), history (`:351-353`). The Setup-specific panel is selected by class name:
`columns/detail/detailDetail25.jsp:5-21` switches on `getActivity().getClass().getSimpleName()`, and `"Setup"`
imports `columns/detail/detailSetup25.jsp` (`:6-8`). That panel (69 lines) is "Services To Implement" — it lists
`Application.applicationModuleList → ServiceItem.description` (`detailSetup25.jsp:29-35`) with two buttons: expand
(`#setupFullModal`, `:12-15`) and add service module (`#addSetupItem`, `:16-19`).

**Actions available on that screen today.** Checklist column (`columns/checklist/checklistBasic25.jsp`): complete a
ToDo (`CloseToDo25`, `:181`), reopen one (`ReOpenToDo25`, `:229`), manage the task (`ManageTask25`, `:147`), a
per-ToDo dynamic form servlet (`toDo.getFormServlet()`, `:83`), send back to BPO (`SendBackToDo25`, `:426`), and a
hidden autosave post (`PersistChecklist25`, `:445`). Detail column: primary contact
(`detailPrimaryContact25.jsp`), the Setup panel above, questionnaires — six `QuestionnaireInstanceAction` forms
plus a public `/q/{guid}` link (`detailQuestionnaires25.jsp:28,96,139,157,168,179,191`), **NDT Census Testing**
(`activityDetail25.jsp:332`), documents and links via `ShowFileUpload` (`detailDocsLinks25.jsp:39,76`), additional
contacts, and footer modals (`detailFooter25.jsp`, which hosts the `#addSetupItem` modal, `:6`). Right column: add
note (`detailAddNote25.jsp`, gated on the activity being open, `activityDetail25.jsp:348-350`) and history.

**Where a new action attaches.** `activityDetail25.jsp:324-337` — the NDT block is the existing precedent for a
PSP-admin-gated, conditionally-shown action button in the detail column, and it appears exactly once:

```jsp
324              <%-- NDT Census Testing button — shows only when NDT questionnaire is scoped to this activity --%>
325              <c:if test="${sessionScope.local.isPspAdmin()}">
326                <c:set var="hasNdtQuestionnaire" value="false" />
327                <c:forEach var="aq" items="${sessionScope.local.getCurrentActivity().availableQuestionnaires}">
328                  <c:if test="${aq.renderer == 'ndt_125'}"><c:set var="hasNdtQuestionnaire" value="true" /></c:if>
329                </c:forEach>
330                <c:if test="${hasNdtQuestionnaire}">
331                  <div class="detail-section" style="padding: 0.5rem 1rem;">
332                    <a href="${pageContext.request.contextPath}/NdtTestRun" class="btn btn-sm btn-outline-primary w-100">
333                      <i class="bi bi-shield-check me-1"></i>NDT Census Testing
334                    </a>
335                  </div>
336                </c:if>
337              </c:if>
338              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailDocsLinks25.jsp"></c:import>
```
Line 338's `detailDocsLinks25.jsp` import is unique in this file and is a clean single anchor.

### Q2 — How `SummitExportServlet` is reached

**Mapping:** `@WebServlet(name = "SummitExportServlet", value = "/SummitExport")`
(`controller/market/SummitExportServlet.java:38`). `doGet` only.

**Nav entry: none.** `grep -rn "SummitExport" src/main/webapp src/main/java` excluding the servlet's own file →
**no matches.** There is no link, button, or menu item anywhere; the URL must be typed by hand.

**Gating — two checks, both hard 403.** First, before any DB work (`:63-67`):
```java
boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
if (!isPspAdmin) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
```
Then, after the `EntityManager` opens (`:89-93`):
```java
if (!IchraAccessResolver.isAvailable(em, request)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
```
So an arbitrary logged-in agent is stopped twice: they are not a PSP admin, and separately the installation must
carry ICHRA entitlement. Input validation follows (`:70-76`): `proposalId` must be present and numeric and `type`
must be exactly `employer` or `cdhplan`, else 400.

### Q3 — File upload

**Yes — extensively.** 22 servlets carry `@MultipartConfig`; 12 JSPs post `enctype="multipart`. There is **no
`commons-fileupload` in `pom.xml`** (`grep -n -i fileupload pom.xml` → no matches) — everything uses the Jakarta
`jakarta.servlet.http.Part` API directly, which is the supported path on this Tomcat 10 / Jakarta Servlet 5.0 stack
and is already proven in production here.

**Clearest end-to-end example: `controller/monthly/UploadCsvServlet.java`** (`/UploadCsvServlet`, `:23`), whose
`@MultipartConfig` is **bare** (`:24`) — container defaults, no declared size limits.
- **Validation:** extension allowlist `{csv, txt, xlsx, xls}` (`:27-28`, checked by `allowed()` `:41-44`);
  `safeSubmittedName()` strips any path from the submitted name and falls back to parsing `content-disposition`
  (`:46-63`); `safeChild()` normalises and rejects traversal outside the upload dir (`:81-88`). A file failing the
  extension check is collected into `unmatchedFiles` and skipped, not fatal (`:131-137`).
- **Where the bytes land: the filesystem**, not memory and not a blob column. The directory is resolved by
  `PathUtil.resolveAndEnsureDir(ctx, "AMS_UPLOAD_DIR", "AMS_UPLOAD_DIR", "ams.upload.dir", "work/ams-uploads")`
  (`:96-104`; `data/util/PathUtil.java:43-97`) — context-param, then env var, then `-D`, then
  `catalina.base/work/ams-uploads`. Each part streams to a `.{name}.part` temp then `Files.move(..., REPLACE_EXISTING)`
  atomically into place (`:166-171`), with `part.delete()` releasing the container temp (`:173`). Prior files for
  the same prefix are deleted first — hard-replace mode (`:65-78`, `:157`).
- **Declared size limits elsewhere:** `SummitImportWizard` 20 MB/file and 50 MB/request (`:36-39`), `ProviderSetup`
  10/20 MB (`:38`), `ChatAssistant` 10 MB (`:40`), `ImportTransitionManager` 5 MB (`:25`).

### Q4 — Parsing

`pom.xml` carries exactly two relevant dependencies:

| Dependency | Version | pom lines |
|---|---|---|
| `org.apache.poi:poi` | 5.2.3 | `:74-76` |
| `org.apache.poi:poi-ooxml` | 5.2.3 | `:79-81` |
| `com.opencsv:opencsv` | 5.9 | `:86-88` |

**Absent:** `commons-csv`, `super-csv`, `jackson-dataformat-csv` — no matches in `pom.xml`.

Existing parsing in Java, all real library use rather than crude splitting:
`data/service/Importer.java` uses both — opencsv `CSVReaderBuilder`/`CSVParserBuilder`/`CSVWriter` (imports `:4-8`;
readers at `:231`, `:376`, `:401`, the last with `withSkipLines(1)`) and POI (`:21-23`), branching on extension:
`new XSSFWorkbook(fis)` vs `HSSFWorkbook` at `:540`, `:752`, `:996`. `data/service/SummitImportService.java` uses
POI (`:13-15`, `:94`, `:1057`, `:1080`); `data/service/UniversalImportService.java` uses POI (`:14-16`, `:112`).

**Everything a census parser needs is genuinely available — CSV and both XLS/XLSX — and nothing would need adding.**

### Q5 — `SummitExportServlet`'s extension shape

**Dispatch is a request parameter**, not a path or a button. Validated at `:70-76`
(`type == null || !(type.equals("employer") || type.equals("cdhplan"))` → 400), then dispatched at `:131-135`:
```java
if (type.equals("employer")) { writeEmployerDemographic(response, prospect, answers, employerTpaCustomId); }
else                         { writeEmployerCdhPlan(response, prospect, answers, employerTpaCustomId); }
```
Per-file writers: `writeEmployerDemographic` (`:190-222`) and `writeEmployerCdhPlan` (`:235-283`), each validating
its own inputs, then building **one** record via `String.join("|", …)` and handing it to the shared sink:
```java
:299  private void writeFile(HttpServletResponse response, String filename, String line) throws IOException {
:300      response.setContentType("text/plain");
:301      response.setCharacterEncoding("UTF-8");
:302      response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
:303      PrintWriter out = response.getWriter();
:304      out.print(line);
:305      out.print("\n");
:306      out.flush();
:307  }
```
Five lines on what changes:
1. `writeFile` takes a single `String line` (`:299`) and prints it once — that is the only thing blocking a
   multi-row file; nothing else in the servlet assumes one record.
2. Changing its signature to `List<String>` would touch both existing writers, since both pass one `String`.
3. Adding an **overload** instead leaves both existing paths byte-identical.
4. A new file type is otherwise purely additive: one more accepted token in the `:73` validation, one more branch
   at `:131`, one new `write<File>` method.
5. **So yes — a second file type can be added without altering the existing two paths, provided the sink is
   overloaded rather than modified.** The shared helpers `sanitize` (`:319-322`) and `sanitizeFilename` (`:325`)
   are already per-value and need no change.

### Q6 — Task sequences

`TaskSequence` is abstract with `@Id @GeneratedValue @Column(name="sequence_id")`, a `description`, a PSP FK and an
`inActive` flag (`model/activity/checklist/sequences/TaskSequence.java:9-27`); its subclasses are `RequiredTaskList`
— which is what ties a sequence to a service, via `@JoinColumn(name="purpose_id") private ServiceItem serviceItem`
(`RequiredTaskList.java:8-13`) — and `RecurringTaskList.java`. Tasks join sequences through `TaskSequenceTable`, a
composite-key entity (`TaskSequenceID`) over `task_id` + `sequence_id` carrying `sort_order`
(`support/TaskSequenceTable.java:7-25`), with `CompositeTaskOrder` (V047, `support/CompositeTaskOrder.java`) holding
cross-sequence ordering per activity type per PSP. The LOS link is indirect: `ServiceModule.los`
(`ServiceModule.java:35`) and `ServiceModule.listOfLosWithThisModule` (`:47`) reach `ModuleDetail` → `ServiceItem`
(`:123`), and `ApplicationModule` joins an `Application` to a `ServiceItem` — which is what `detailSetup25.jsp:29`
renders. Instances are `ToDo` (`@Id @GeneratedValue todo_id`, `task_id`, `checklist_id`, `sort_order`, `is_complete`,
**`date_completed`**, `completed_by_id`, `todo_guid`, plus BPO completion columns) — `model/.../ToDo.java:10-55`.
On artifacts: the **template** can reference one — `Task.goToLink` and `Task.infoLink` are both `WebLink` FKs with
`has_goto`/`has_info` flags (`Task.java:28-46`) — and the **instance** can carry attachments through
`ToDoNote.webLinkList` (`ToDoNote.java:43`, the V033 `todo_note_attachments` join). A completion timestamp is native
(`ToDo.date_completed`); no entity stores a generated file itself, only a `WebLink` reference to one.

## What this run could not establish

- **Where a census upload should land.** Every existing upload uses one global `AMS_UPLOAD_DIR`
  (`PathUtil.java:79-97`); there is no per-Setup or per-employer upload directory precedent anywhere in the tree.
  A Kevin decision, not a fact.
- **Whether the container's default multipart limits suffice for a real census.** `UploadCsvServlet:24` declares
  none, and the tree contains no sample census and no row counts. A sample file from an employer would settle it.
- **Whether `SummitExportServlet`'s ICHRA gate (`:89-93`) is correct for a non-ICHRA Setup.** As written it refuses
  every Setup on an installation without ICHRA entitlement, including one being converted for CDH only. Whether
  that is intended is a Kevin decision; a runtime walk would confirm the observed behaviour.
- **Whether the NDT block (`activityDetail25.jsp:324-337`) is the intended home for a Summit action** or merely the
  nearest structural precedent. Design decision, explicitly out of scope for this run.
- **Whether files 1 and 2 actually produce Summit-acceptable output.** Nothing was executed here and they have
  still never been run; only a real Summit import test can establish that.
