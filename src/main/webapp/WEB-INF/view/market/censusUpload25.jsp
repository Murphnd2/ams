<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Census ingestion at Setup (S26-C, V094). Loads an employer's participant roster; emits no
     Summit file — files 4 and 5 read this roster and are a separate build item.

     Deliberately absent: any download, export, print or "send to Summit" affordance. This page
     loads and displays a roster of named people with home addresses; the emit surface is
     SummitExportServlet and stays there.

     The uploaded file is parsed from the request stream and never written to disk. --%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Census Upload</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .census-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
        }
        .toolbar {
            padding: 0.65rem 1rem;
            background: #fff; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.75rem;
        }
        .toolbar .t-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0;
        }
        .toolbar .t-employer {
            font-size: 0.85rem; color: #495057;
        }
        .census-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .status-card h6 {
            font-weight: 700; color: var(--ssa, #0d5681);
            font-size: 0.85rem; margin: 0 0 0.6rem 0;
        }
        .roster-table { font-size: 0.82rem; width: 100%; }
        .roster-table th {
            font-weight: 600; color: #495057; background: #f8f9fa;
            border-bottom: 2px solid #dee2e6; padding: 0.35rem 0.5rem;
            position: sticky; top: 0;
        }
        .roster-table td {
            padding: 0.3rem 0.5rem; border-bottom: 1px solid #eceff1;
        }
        .map-table { width: auto; font-size: 0.82rem; }
        .map-table td { padding: 0.15rem 0.75rem 0.15rem 0; vertical-align: top; }
        .map-table .map-field { color: #495057; font-weight: 600; white-space: nowrap; }
        .map-none { color: #6c757d; font-style: italic; }
        .err-list { margin: 0; padding-left: 1.1rem; }
        .err-list li { margin-bottom: 0.2rem; }
        .err-row { font-weight: 600; color: #842029; }
        .hint { color: #6c757d; font-size: 0.78rem; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="census-wrap">

    <div class="toolbar">
        <p class="t-title"><i class="bi bi-people me-1"></i>Census Upload</p>
        <span class="t-employer"><c:out value="${employerName}"/></span>
        <span class="ms-auto hint">Proposal ${proposalId}</span>
    </div>

    <div class="census-body">

        <c:if test="${not empty notice}">
            <div class="alert alert-success py-2" style="font-size: 0.85rem;">
                <c:out value="${notice}"/>
            </div>
        </c:if>

        <%-- Replacement refusal. Same position and markup as the success banner above, styled
             as a warning: the file was fine, the insert was refused. Mutually exclusive with the
             parse-error banner by construction — the servlet only sets these attributes after a
             clean parse — and the `empty errors` guard keeps the two from ever stacking. --%>
        <c:if test="${not empty refusedExisting and empty errors}">
            <div class="alert alert-warning py-2" style="font-size: 0.85rem;">
                <strong>Not loaded</strong> &mdash; a census is already loaded for this employer
                (${refusedExisting}
                <c:choose><c:when test="${refusedExisting == 1}">participant</c:when>
                    <c:otherwise>participants</c:otherwise></c:choose>).
                The file you submitted contained ${refusedSubmitted}
                <c:choose><c:when test="${refusedSubmitted == 1}">participant</c:when>
                    <c:otherwise>participants</c:otherwise></c:choose>
                and was read successfully. Clear the current roster to replace it.
            </div>
        </c:if>

        <c:if test="${not empty errors}">
            <div class="alert alert-danger py-2" style="font-size: 0.85rem;">
                <div class="fw-semibold mb-2">
                    Nothing was inserted. ${fn:length(errors)}
                    <c:choose><c:when test="${fn:length(errors) == 1}">problem</c:when>
                        <c:otherwise>problems</c:otherwise></c:choose> found:
                </div>
                <ul class="err-list">
                    <c:forEach var="e" items="${errors}">
                        <li>
                            <c:if test="${e.rowNumber > 0}">
                                <span class="err-row">Row ${e.rowNumber}</span>
                                <c:if test="${not empty e.column}"> &middot; <code><c:out value="${e.column}"/></code></c:if>
                                &mdash;
                            </c:if>
                            <c:out value="${e.reason}"/>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>

        <%-- What was read from the file. Collapsed by default; present on success AND on a
             parse failure, because a missing-header failure is exactly when the operator needs
             to see what did match. Accordion markup follows checklistHistory25.jsp:90-101.

             Header NAMES of ignored columns are shown; no VALUE from an ignored column is ever
             rendered here. If the employer sent an SSN column its name may appear, its contents
             must not. --%>
        <c:if test="${not empty mapping and not empty mapping.fields}">
            <div class="accordion mb-3" id="mappingAccordion">
                <div class="accordion-item">
                    <h2 class="accordion-header">
                        <button class="accordion-button collapsed py-2 px-3" type="button"
                                data-bs-toggle="collapse" data-bs-target="#mappingBody"
                                style="font-size: 0.82rem; background: #f8f9fa;">
                            <i class="bi bi-list-columns me-2 text-muted"></i>
                            <span>What was read from the file</span>
                        </button>
                    </h2>
                    <div id="mappingBody" class="accordion-collapse collapse">
                        <div class="accordion-body" style="font-size: 0.82rem;">

                            <table class="map-table mb-3">
                                <tbody>
                                <c:forEach var="fm" items="${mapping.fields}">
                                    <tr>
                                        <td class="map-field"><c:out value="${fm.label}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${fm.matched}">
                                                    <code><c:out value="${fm.header}"/></code>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="map-none">
                                                        no column found<c:if test="${fm.required}"> &mdash; required</c:if>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>

                            <c:choose>
                                <c:when test="${not empty mapping.ignoredColumns}">
                                    <div class="hint mb-1">
                                        These columns were in the file and were not read.
                                    </div>
                                    <div>
                                        <c:forEach var="col" items="${mapping.ignoredColumns}" varStatus="s">
                                            <code><c:out value="${col}"/></code><c:if test="${not s.last}">, </c:if>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="hint">Every column in the file was read.</div>
                                </c:otherwise>
                            </c:choose>

                            <c:if test="${mapping.skippedBlankRows > 0}">
                                <div class="hint mt-2">
                                    ${mapping.skippedBlankRows} blank
                                    <c:choose><c:when test="${mapping.skippedBlankRows == 1}">row was</c:when>
                                        <c:otherwise>rows were</c:otherwise></c:choose> skipped.
                                </div>
                            </c:if>

                        </div>
                    </div>
                </div>
            </div>
        </c:if>

        <%-- Upload form. Always rendered, whether or not a census is loaded. Hiding it when a
             roster existed made the replacement refusal unreachable — there was no form to
             submit, so the only way forward was to clear a good roster before learning whether
             the replacement file was even parseable. --%>
        <div class="status-card">
                <h6>Upload the employer's census</h6>
                <form method="post" enctype="multipart/form-data"
                      action="${pageContext.request.contextPath}/CensusUpload">
                    <input type="hidden" name="proposalId" value="${proposalId}"/>
                    <div class="row g-3">
                        <div class="col-md-5">
                            <label class="form-label" style="font-size: 0.8rem;">Census file</label>
                            <input type="file" name="censusFile" required
                                   accept=".csv,.txt,.xlsx,.xls"
                                   class="form-control form-control-sm"/>
                            <div class="hint mt-1">
                                .csv, .txt, .xlsx or .xls. Up to 5,000 employees.
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" style="font-size: 0.8rem;">Effective date</label>
                            <input type="date" name="effectiveDate" required
                                   class="form-control form-control-sm"/>
                            <div class="hint mt-1">Applied to every participant in the file.</div>
                        </div>
                        <div class="col-md-3 d-flex align-items-end">
                            <button type="submit" class="btn btn-sm btn-outline-ssa w-100">
                                <i class="bi bi-upload me-1"></i>Upload census
                            </button>
                        </div>
                    </div>
                </form>
            </div>

            <div class="status-card">
                <h6>What the file needs</h6>
                <p class="mb-2" style="font-size: 0.82rem;">
                    <strong>Column order does not matter.</strong> Every column is found by its
                    heading, and columns we do not recognise are ignored &mdash; payroll,
                    department and salary columns can stay in the file.
                </p>
                <p class="mb-2" style="font-size: 0.82rem;">
                    <strong>Required headings:</strong> first name, last name, address, city,
                    state, zip. Common variations are accepted (for example
                    <code>First Name</code>, <code>fname</code> or <code>Given Name</code>).
                </p>
                <p class="mb-0" style="font-size: 0.82rem;">
                    <strong>Optional:</strong> a second address line, and email.
                    <span class="hint">
                        Social security numbers, dates of birth and compensation are never read
                        or stored, even when the file contains them.
                    </span>
                </p>
            </div>

        <%-- Loaded roster --%>
        <c:if test="${not empty participants}">
            <div class="status-card">
                <h6>
                    Loaded roster &mdash; ${fn:length(participants)}
                    <c:choose><c:when test="${fn:length(participants) == 1}">participant</c:when>
                        <c:otherwise>participants</c:otherwise></c:choose>
                </h6>
                <p class="hint mb-0">
                    A census is already loaded for this employer. To replace it, clear it first.
                </p>
            </div>

            <div class="status-card p-0" style="overflow-x: auto;">
                <table class="roster-table">
                    <thead>
                    <tr>
                        <th>Last name</th>
                        <th>First name</th>
                        <th>Address</th>
                        <th>City</th>
                        <th>State</th>
                        <th>Zip</th>
                        <th>Email</th>
                        <th>Effective</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${participants}">
                        <tr>
                            <td><c:out value="${p.lastName}"/></td>
                            <td><c:out value="${p.firstName}"/></td>
                            <td>
                                <c:out value="${p.addressLine1}"/>
                                <c:if test="${not empty p.addressLine2}">
                                    <span class="hint"><c:out value="${p.addressLine2}"/></span>
                                </c:if>
                            </td>
                            <td><c:out value="${p.city}"/></td>
                            <td><c:out value="${p.state}"/></td>
                            <td><c:out value="${p.postalCode}"/></td>
                            <td><c:out value="${p.email}"/></td>
                            <td><c:out value="${p.effectiveDate}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <div class="status-card">
                <h6>Clear this roster</h6>
                <div class="alert alert-warning py-2 mb-2" style="font-size: 0.82rem;">
                    If these participants have already been sent to Summit, clearing and
                    re-uploading will assign new participant IDs and orphan the Summit records.
                </div>
                <form method="post" action="${pageContext.request.contextPath}/CensusUpload"
                      onsubmit="return confirm('Clear all ${fn:length(participants)} participants for this employer? If they have already been sent to Summit, re-uploading will assign new participant IDs and orphan the Summit records.');">
                    <input type="hidden" name="proposalId" value="${proposalId}"/>
                    <input type="hidden" name="action" value="clear"/>
                    <div class="form-check mb-2">
                        <input class="form-check-input" type="checkbox" id="confirmClear"
                               name="confirmClear" value="yes" required/>
                        <label class="form-check-label" for="confirmClear" style="font-size: 0.82rem;">
                            I understand this cannot be undone.
                        </label>
                    </div>
                    <button type="submit" class="btn btn-sm btn-outline-danger">
                        <i class="bi bi-trash me-1"></i>Clear roster
                    </button>
                </form>
            </div>
        </c:if>

    </div>
</div>
</body>
</html>
