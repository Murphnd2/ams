<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- S47-F -- PSP review of a client's staged census upload (T231 build 2, D45 e). Shell, card and
     table classes copied from censusUpload25.jsp so this page reads as the same surface. PII here
     (names, addresses) is PSP-admin only, same audience as Census Upload's roster table. --%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Census Review</title>
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
        .roster-table tr.row-issue td { background: #fff8ec; }
        .map-table { width: auto; font-size: 0.82rem; }
        .map-table td { padding: 0.15rem 0.75rem 0.15rem 0; vertical-align: top; }
        .map-table .map-field { color: #495057; font-weight: 600; white-space: nowrap; }
        .map-none { color: #6c757d; font-style: italic; }
        .hint { color: #6c757d; font-size: 0.78rem; }
        .issue-note { color: #7a4a00; font-size: 0.78rem; }
        .diff-col h6 { margin-bottom: 0.3rem; }
        .diff-names { font-size: 0.8rem; max-height: 8rem; overflow-y: auto; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="census-wrap">

    <div class="toolbar">
        <p class="t-title"><i class="bi bi-clipboard-check me-1"></i>Census Review</p>
        <span class="t-employer"><c:out value="${employerName}"/></span>
        <span class="ms-auto hint">Proposal ${proposalId}</span>
    </div>

    <div class="census-body">

        <c:if test="${not empty formError}">
            <div class="alert alert-danger py-2" style="font-size: 0.85rem;">
                <c:out value="${formError}"/>
            </div>
        </c:if>

        <%-- ── Nothing to review ── --%>
        <c:if test="${reviewState == 'NONE'}">
            <div class="status-card">
                <p class="mb-2">No client upload is awaiting review.</p>
                <a href="${pageContext.request.contextPath}/ViewActivity25" class="btn btn-sm btn-outline-ssa">
                    <i class="bi bi-arrow-left me-1"></i>Back to Setup
                </a>
            </div>
        </c:if>

        <%-- ── Unreadable upload: Reject only ── --%>
        <c:if test="${reviewState == 'UNREADABLE'}">
            <div class="status-card">
                <h6>Unreadable upload</h6>
                <p class="mb-2">
                    Received <c:out value="${submittedAtDisplay}"/>
                    <c:if test="${not empty originalFilename}"> &mdash; <code><c:out value="${originalFilename}"/></code></c:if>
                </p>
                <p class="mb-1"><strong>Missing required columns:</strong> <c:out value="${missingLabelsText}"/></p>
                <c:if test="${not empty foundHeadersText}">
                    <p class="mb-0"><strong>Columns found:</strong> <c:out value="${foundHeadersText}"/></p>
                </c:if>
            </div>

            <div class="status-card">
                <h6>Reject this upload</h6>
                <p class="hint mb-2">The client's link stays open and its expiry extends by 30 days so they can upload a corrected file.</p>
                <form method="post" action="${pageContext.request.contextPath}/CensusReview">
                    <input type="hidden" name="proposalId" value="${proposalId}"/>
                    <input type="hidden" name="submissionId" value="${submissionId}"/>
                    <input type="hidden" name="action" value="reject"/>
                    <div class="mb-2">
                        <label class="form-label" style="font-size: 0.8rem;">Note to yourself (optional)</label>
                        <textarea name="note" class="form-control form-control-sm" rows="2"></textarea>
                    </div>
                    <button type="submit" class="btn btn-sm btn-outline-danger">
                        <i class="bi bi-x-circle me-1"></i>Reject
                    </button>
                </form>
            </div>
        </c:if>

        <%-- ── Pending: full review ── --%>
        <c:if test="${reviewState == 'PENDING'}">

            <div class="status-card">
                <h6>Upload summary</h6>
                <p class="mb-0">
                    Received <c:out value="${submittedAtDisplay}"/>
                    <c:if test="${not empty originalFilename}"> &mdash; <code><c:out value="${originalFilename}"/></code></c:if>
                    &mdash; ${rowCount}
                    <c:choose><c:when test="${rowCount == 1}">row</c:when><c:otherwise>rows</c:otherwise></c:choose>,
                    ${issueCount}
                    <c:choose><c:when test="${issueCount == 1}">issue</c:when><c:otherwise>issues</c:otherwise></c:choose>.
                </p>
            </div>

            <%-- Mapping accordion, copied from censusUpload25.jsp:136-195 --%>
            <c:if test="${not empty mappingFields}">
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
                                    <c:forEach var="fm" items="${mappingFields}">
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
                                    <c:when test="${not empty ignoredColumnsText}">
                                        <div class="hint mb-1">These columns were in the file and were not read.</div>
                                        <div><code><c:out value="${ignoredColumnsText}"/></code></div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="hint">Every column in the file was read.</div>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${skippedBlankRows > 0}">
                                    <div class="hint mt-2">
                                        ${skippedBlankRows} blank
                                        <c:choose><c:when test="${skippedBlankRows == 1}">row was</c:when>
                                            <c:otherwise>rows were</c:otherwise></c:choose> skipped.
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>

            <%-- Rows table --%>
            <div class="status-card p-0" style="overflow-x: auto;">
                <table class="roster-table">
                    <thead>
                    <tr>
                        <th>Row #</th>
                        <th>First</th>
                        <th>Last</th>
                        <th>Address 1</th>
                        <th>Address 2</th>
                        <th>City</th>
                        <th>State</th>
                        <th>Zip</th>
                        <th>Email</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="r" items="${rows}">
                        <tr class="${r.hasIssues ? 'row-issue' : ''}">
                            <td>${r.rowNumber}</td>
                            <td><c:out value="${r.firstName}"/></td>
                            <td><c:out value="${r.lastName}"/></td>
                            <td><c:out value="${r.addressLine1}"/></td>
                            <td><c:out value="${r.addressLine2}"/></td>
                            <td><c:out value="${r.city}"/></td>
                            <td><c:out value="${r.state}"/></td>
                            <td><c:out value="${r.postalCode}"/></td>
                            <td><c:out value="${r.email}"/></td>
                        </tr>
                        <c:if test="${r.hasIssues}">
                            <tr class="row-issue">
                                <td></td>
                                <td colspan="8">
                                    <c:forEach var="issue" items="${r.issues}">
                                        <div class="issue-note"><i class="bi bi-exclamation-triangle me-1"></i><c:out value="${issue}"/></div>
                                    </c:forEach>
                                </td>
                            </tr>
                        </c:if>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <%-- Diff against the current roster --%>
            <div class="status-card">
                <h6>Compared with the current roster</h6>
                <c:choose>
                    <c:when test="${rosterEmpty}">
                        <p class="mb-0 hint">No roster loaded yet.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="row g-3">
                            <div class="col-md-4 diff-col">
                                <h6>New (${diffNewCount})</h6>
                                <div class="diff-names"><c:out value="${diffNewText}"/></div>
                            </div>
                            <div class="col-md-4 diff-col">
                                <h6>Matching (${diffMatchingCount})</h6>
                                <div class="diff-names"><c:out value="${diffMatchingText}"/></div>
                            </div>
                            <div class="col-md-4 diff-col">
                                <h6>Missing (${diffMissingCount})</h6>
                                <div class="diff-names"><c:out value="${diffMissingText}"/></div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- Load --%>
            <div class="status-card">
                <h6>Load into the roster</h6>
                <form method="post" action="${pageContext.request.contextPath}/CensusReview">
                    <input type="hidden" name="proposalId" value="${proposalId}"/>
                    <input type="hidden" name="submissionId" value="${submissionId}"/>
                    <input type="hidden" name="action" value="load"/>
                    <div class="row g-3 align-items-end">
                        <div class="col-md-4">
                            <label class="form-label" style="font-size: 0.8rem;">Effective date</label>
                            <input type="date" name="effectiveDate" required class="form-control form-control-sm"/>
                            <div class="hint mt-1">Applied to every participant in the file.</div>
                        </div>
                        <%-- S47-G, Kevin's walk: shown only when replacing could actually happen --
                             a roster exists, the upload has no issues, and Demographics is not
                             already settled. In every other case the Load button below is disabled
                             anyway, so a checkbox here would ask for a confirmation that can't lead
                             anywhere. --%>
                        <c:if test="${not rosterEmpty and not issuesBlockLoad and not demographicsSettled}">
                            <div class="col-md-5">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="replaceConfirmed"
                                           name="replaceConfirmed" value="yes" required/>
                                    <label class="form-check-label" for="replaceConfirmed" style="font-size: 0.82rem;">
                                        Replace the current ${rosterCount}-participant roster
                                    </label>
                                </div>
                            </div>
                        </c:if>
                        <div class="col-md-3">
                            <c:choose>
                                <c:when test="${issuesBlockLoad}">
                                    <button type="button" class="btn btn-sm btn-outline-secondary w-100" disabled
                                            title="Fix or reject: ${issueCount} rows have issues">
                                        <i class="bi bi-upload me-1"></i>Load
                                    </button>
                                    <div class="hint mt-1">Fix or reject: ${issueCount} rows have issues.</div>
                                </c:when>
                                <c:when test="${not rosterEmpty and demographicsSettled}">
                                    <button type="button" class="btn btn-sm btn-outline-secondary w-100" disabled
                                            title="Demographics already pushed or marked done">
                                        <i class="bi bi-upload me-1"></i>Load
                                    </button>
                                    <div class="hint mt-1">Demographics already pushed or marked done.</div>
                                </c:when>
                                <c:otherwise>
                                    <button type="submit" class="btn btn-sm btn-outline-ssa w-100">
                                        <i class="bi bi-upload me-1"></i>Load
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </form>
            </div>

            <%-- Reject --%>
            <div class="status-card">
                <h6>Reject this upload</h6>
                <p class="hint mb-2">The client's link stays open and its expiry extends by 30 days so they can upload a corrected file.</p>
                <form method="post" action="${pageContext.request.contextPath}/CensusReview">
                    <input type="hidden" name="proposalId" value="${proposalId}"/>
                    <input type="hidden" name="submissionId" value="${submissionId}"/>
                    <input type="hidden" name="action" value="reject"/>
                    <div class="mb-2">
                        <label class="form-label" style="font-size: 0.8rem;">Note to yourself (optional)</label>
                        <textarea name="note" class="form-control form-control-sm" rows="2"></textarea>
                    </div>
                    <button type="submit" class="btn btn-sm btn-outline-danger">
                        <i class="bi bi-x-circle me-1"></i>Reject
                    </button>
                </form>
            </div>

        </c:if>

    </div>
</div>
</body>
</html>
