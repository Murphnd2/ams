<%--
  S62-P2 -- T277's working replacement for the Demographics "Check response" signal
  (SummitResponseService.parse classifies on fields[0], which on the Demographics response is the
  participant key, so every line renders UNKNOWN there). Reached from
  detailSummitSetup25.jsp's Census row "Verify against Summit export" control.

  ⚠️ PII / no persistence (LA-40). Every row under "Participants" below comes straight from
  SummitDemographicsVerifyService, fetched fresh on this one request and never stored -- the
  participant-list export can carry names, addresses and phone numbers (summit_data_exchange.md's
  observed header). Do not add a server-side cache or a hidden field that resubmits raw export
  content; if this page is refreshed, it re-fetches.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Summit Demographics Verify</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .audit-wrap {
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
        .rc-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .resp-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .resp-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .resp-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: middle; }
        .resp-table tr.row-missing { background: #fdecea; }
        .resp-table tr.row-wrong { background: #fff8e1; }
        .badge-found { background: #198754; color: #fff; }
        .badge-wrong { background: #fd7e14; color: #fff; }
        .badge-missing { background: #dc3545; color: #fff; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .num { text-align: right; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-clipboard-check me-1"></i>Demographics — verify against Summit export
        </h1>
        <span class="text-muted" style="font-size: 0.8rem;">Proposal <c:out value="${proposalId}"/></span>
        <%-- Same rule as summitResponse25.jsp:76-84 -- only when the session's current activity is
             really this proposal's Setup; ViewActivity25 renders whatever activity is current with
             no parameters, so never show this link when that could open a different activity. --%>
        <c:if test="${backToSetup}">
            <a href="${pageContext.request.contextPath}/ViewActivity25" class="btn btn-outline-ssa btn-sm">
                <i class="bi bi-arrow-left"></i> Back to setup
            </a>
        </c:if>
    </div>

    <div class="rc-body">

        <c:choose>
            <c:when test="${outcome == 'OK'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Source export</div>
                    <div><c:out value="${fileName}"/></div>
                    <div class="text-muted" style="font-size: 0.78rem;">
                        <c:out value="${fileTimestampDisplay}"/>
                    </div>
                </div>

                <div class="status-card">
                    <div class="fw-semibold mb-2">Counts</div>
                    <div class="mb-2">
                        <span class="badge badge-found"><c:out value="${foundCount}"/> Found</span>
                        <span class="badge badge-wrong"><c:out value="${wrongEmployerCount}"/> Wrong employer</span>
                        <span class="badge badge-missing"><c:out value="${missingCount}"/> Missing</span>
                        <span class="badge bg-secondary"><c:out value="${unkeyedCount}"/> Unkeyed (Summit-added)</span>
                    </div>
                    <div class="text-muted" style="font-size: 0.78rem;">
                        <c:out value="${expectedCount}"/> participant(s) expected from AMS's own roster.
                        "Unkeyed" rows are export rows under this employer with a blank
                        Participant Custom ID — per the 2026-09-10 observation, a participant added
                        directly in the Summit UI rather than by an AMS push. Not counted as an error.
                    </div>
                </div>

                <c:if test="${wrongEmployerCount > 0 or missingCount > 0}">
                    <div class="text-danger mb-2" style="font-size: 0.85rem;">
                        <i class="bi bi-exclamation-triangle"></i>
                        One or more participants did not confirm cleanly. Review the table below.
                    </div>
                </c:if>

                <table class="resp-table">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Expected key</th>
                        <th>State</th>
                        <th>Summit UserStatus</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${participants}">
                        <tr class="${p.state == 'MISSING' ? 'row-missing' : (p.state == 'FOUND_WRONG_EMPLOYER' ? 'row-wrong' : '')}">
                            <td><c:out value="${p.firstName}"/> <c:out value="${p.lastName}"/></td>
                            <td class="mono"><c:out value="${p.expectedKey}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${p.state == 'FOUND'}">
                                        <span class="badge badge-found">Found</span>
                                    </c:when>
                                    <c:when test="${p.state == 'FOUND_WRONG_EMPLOYER'}">
                                        <span class="badge badge-wrong">Wrong employer</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-missing">Missing</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${p.userStatus}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:when>

            <c:when test="${outcome == 'NO_EXPORT_FOUND'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">No matching export file found</div>
                    <div class="text-muted">
                        No file matching the configured participant-export prefix was found in
                        Summit's export directory, or none had a parseable timestamp. Nothing was
                        compared.
                    </div>
                </div>
            </c:when>

            <c:when test="${outcome == 'MISSING_HEADERS'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Required header(s) absent</div>
                    <div class="text-muted mb-2">
                        <c:if test="${not empty fileName}">
                            <c:out value="${fileName}"/> — <c:out value="${fileTimestampDisplay}"/><br/>
                        </c:if>
                        The export is missing one or more columns this compare requires:
                    </div>
                    <ul>
                        <c:forEach var="h" items="${missingHeaderNames}">
                            <li><c:out value="${h}"/></li>
                        </c:forEach>
                    </ul>
                </div>
            </c:when>

            <c:when test="${outcome == 'PREFIX_NOT_CONFIGURED'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Summit TPA prefix not configured</div>
                    <div class="text-muted">
                        SUMMIT_TPA_ID_PREFIX is absent, blank, or contains a non-alphanumeric
                        character on this installation. No employer or participant key can be
                        composed, so nothing was compared.
                    </div>
                </div>
            </c:when>

            <c:when test="${outcome == 'EXPORT_DIR_NOT_CONFIGURED'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Export directory not configured</div>
                    <div class="text-muted">
                        SUMMIT_SFTP_IMPORT_DIR does not resolve to an ImportFiles directory, so no
                        sibling ExportFiles directory could be derived.
                    </div>
                </div>
            </c:when>

            <c:when test="${outcome == 'PROSPECT_NOT_FOUND'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Proposal has no prospect</div>
                    <div class="text-muted">
                        This proposal does not resolve to a prospect record. Nothing was compared.
                    </div>
                </div>
            </c:when>

            <c:otherwise>
                <div class="status-card">
                    <div class="fw-semibold mb-1">Could not reach the Summit export</div>
                    <div class="text-danger">
                        <c:out value="${errorMessage}"/>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>

    </div>
</div>
</body>
</html>
