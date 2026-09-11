<%--
  S45-B -- T230 phase 1. Summit response check for one setup step (employer, cdhplan, schedules,
  demographics). Reached from detailSummitSetup25.jsp's "Check response" links.

  ⚠️ PII / no persistence. Everything under "Found" below comes straight from SummitResponseService,
  fetched fresh on this one request and never stored -- a response line can carry a participant
  name (Demographics) or an echoed street address (SummitFileExport's own PII note). Do not add a
  server-side cache or a hidden field that resubmits raw response content; if this page is
  refreshed, it re-fetches.

  Mark done button logic (mirrors Phase A's design point 5 -- "Mark done ... when the response is
  ambiguous"): allOk binds the export and needs no confirm; any other state where a pushed row
  exists still binds that export ("Mark done anyway", confirmed); only when there is truly no
  pushed row (or the step is schedules, which has none by design) does Mark done go fully manual
  with no exportId.
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
    <title>Summit Response Check</title>
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
        .resp-table tr.row-failed { background: #fdecea; }
        .badge-ok { background: #198754; color: #fff; }
        .badge-failed { background: #dc3545; color: #fff; }
        .badge-unknown { background: #6c757d; color: #fff; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .num { text-align: right; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-arrow-repeat me-1"></i><c:out value="${stepLabel}"/>
        </h1>
        <span class="text-muted" style="font-size: 0.8rem;">Proposal <c:out value="${proposalId}"/></span>
        <%-- S45d -- only when the session's current activity is really this proposal's Setup;
             ViewActivity25 renders whatever activity is current with no parameters (S45e: not
             GoActivityDetail25, which throws NumberFormatException on a bare GET, runtime-verified
             2026-09-10), so never show this link when that could open a different activity. --%>
        <c:if test="${backToSetup}">
            <a href="${pageContext.request.contextPath}/ViewActivity25" class="btn btn-outline-ssa btn-sm">
                <i class="bi bi-arrow-left"></i> Back to setup
            </a>
        </c:if>
    </div>

    <div class="rc-body">

        <div class="status-card">
            <div class="fw-semibold mb-1">Step state</div>
            <c:choose>
                <c:when test="${not empty stepState and stepState.state == 'DONE'}">
                    <div>
                        <i class="bi bi-check2-circle text-success"></i>
                        Done · <c:out value="${stepUpdatedDisplay}"/> · <c:out value="${stepState.basis}"/>
                        <c:if test="${not empty stepState.updatedBy}"> · <c:out value="${stepState.updatedBy}"/></c:if>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/SummitResponse" class="mt-2">
                        <input type="hidden" name="proposalId" value="${proposalId}"/>
                        <input type="hidden" name="step" value="${step}"/>
                        <input type="hidden" name="action" value="reopen"/>
                        <button type="submit" class="btn btn-sm btn-outline-secondary">
                            <i class="bi bi-arrow-counterclockwise"></i> Reopen
                        </button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div><i class="bi bi-circle text-muted"></i> Open</div>
                </c:otherwise>
            </c:choose>
        </div>

        <c:if test="${step != 'schedules'}">

            <div class="status-card">
                <div class="fw-semibold mb-1">Latest pushed</div>
                <c:choose>
                    <c:when test="${not empty latestPushed}">
                        <div><c:out value="${latestPushed.fileName}"/></div>
                        <div class="text-muted" style="font-size: 0.78rem;">
                            Delivered <c:out value="${deliveredDisplay}"/> ·
                            <c:out value="${latestPushed.rowCount}"/> row(s)
                        </div>
                        <c:if test="${not empty latestAttempt and latestAttempt.id != latestPushed.id}">
                            <div class="mt-2 text-warning" style="font-size: 0.78rem;">
                                <i class="bi bi-exclamation-triangle"></i>
                                A later attempt: <c:out value="${latestAttempt.deliveryStatus}"/> ·
                                <c:out value="${latestAttemptDisplay}"/>
                                <c:if test="${not empty latestAttempt.deliveryError}">
                                    — <c:out value="${latestAttempt.deliveryError}"/>
                                </c:if>
                            </div>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <div class="text-muted">
                            Nothing has been pushed for this step. Mark done is available as a
                            manual override for a group already set up in Summit.
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${not empty latestPushed}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Response</div>
                    <div class="text-muted mb-2" style="font-size: 0.78rem;">
                        <c:out value="${checkResponseName}"/> in <c:out value="${checkResponseDir}"/>
                    </div>

                    <c:choose>
                        <c:when test="${not empty rcSentRows}">
                            <div class="mb-2">
                                Sent <c:out value="${rcSentRows}"/> row(s), received
                                <c:out value="${rcLineCount}"/> line(s) —
                                <span class="badge badge-ok"><c:out value="${rcOkCount}"/> OK</span>
                                <span class="badge badge-failed"><c:out value="${rcFailedCount}"/> Failed</span>
                                <span class="badge badge-unknown"><c:out value="${rcUnknownCount}"/> Unknown</span>
                            </div>
                            <c:if test="${rcCountMismatch}">
                                <div class="text-danger mb-2" style="font-size: 0.8rem;">
                                    <i class="bi bi-exclamation-triangle"></i>
                                    Line count differs from rows sent. Position-based correlation is
                                    unreliable; review manually.
                                </div>
                            </c:if>
                            <c:if test="${rcUnknownCount > 0}">
                                <div class="text-warning mb-2" style="font-size: 0.8rem;">
                                    <i class="bi bi-question-circle"></i>
                                    Unrecognized status tokens. Tokens are configured by
                                    SUMMIT_RESPONSE_OK_TOKENS / SUMMIT_RESPONSE_FAIL_TOKENS.
                                </div>
                            </c:if>
                            <table class="resp-table">
                                <thead>
                                <tr>
                                    <th class="num">#</th>
                                    <th>Status</th>
                                    <th>Fields</th>
                                    <th>Comment</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="row" items="${rcRows}">
                                    <tr class="${row.classification == 'FAILED' ? 'row-failed' : ''}">
                                        <td class="num mono"><c:out value="${row.lineNo}"/></td>
                                        <td>
                                            <span class="badge ${row.classification == 'OK' ? 'badge-ok' : (row.classification == 'FAILED' ? 'badge-failed' : 'badge-unknown')}">
                                                <c:out value="${row.classification}"/>
                                            </span>
                                            <c:out value="${row.status}"/>
                                        </td>
                                        <td class="mono"><c:out value="${row.middle}"/></td>
                                        <td><c:out value="${row.comment}"/></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </c:when>
                        <c:when test="${checkFound and not empty checkError}">
                            <div class="text-danger"><c:out value="${checkError}"/></div>
                        </c:when>
                        <c:when test="${not checkFound and empty checkError}">
                            <div class="text-muted">
                                No response file yet.
                                <ul class="mt-1 mb-0">
                                    <li>Summit polls ImportFiles roughly every 15 minutes.</li>
                                    <li>If nothing appears, check Summit's File History — a Held
                                        duplicate, or a filename that matches no import template,
                                        produces no response file.</li>
                                    <li>Confirm the Schedule Import checkbox is enabled on the
                                        Imports configuration (SDX-22).</li>
                                </ul>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="text-danger"><c:out value="${checkError}"/></div>
                        </c:otherwise>
                    </c:choose>

                    <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${proposalId}&step=${step}"
                       class="btn btn-sm btn-outline-ssa mt-2">
                        <i class="bi bi-arrow-repeat"></i> Check again
                    </a>
                </div>
            </c:if>
        </c:if>

        <div class="status-card">
            <c:choose>
                <c:when test="${rcAllOk}">
                    <form method="post" action="${pageContext.request.contextPath}/SummitResponse">
                        <input type="hidden" name="proposalId" value="${proposalId}"/>
                        <input type="hidden" name="step" value="${step}"/>
                        <input type="hidden" name="action" value="markdone"/>
                        <input type="hidden" name="exportId" value="${latestPushed.id}"/>
                        <button type="submit" class="btn btn-outline-ssa">
                            <i class="bi bi-check2-circle"></i> Mark done (all rows Successful)
                        </button>
                    </form>
                </c:when>
                <c:when test="${not empty latestPushed}">
                    <form method="post" action="${pageContext.request.contextPath}/SummitResponse">
                        <input type="hidden" name="proposalId" value="${proposalId}"/>
                        <input type="hidden" name="step" value="${step}"/>
                        <input type="hidden" name="action" value="markdone"/>
                        <input type="hidden" name="exportId" value="${latestPushed.id}"/>
                        <button type="submit" class="btn btn-outline-ssa"
                                onclick="return confirm('Mark this step done? The response was not confirmed all-Successful. Review it above before confirming.');">
                            <i class="bi bi-check2-circle"></i> Mark done anyway
                        </button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${pageContext.request.contextPath}/SummitResponse">
                        <input type="hidden" name="proposalId" value="${proposalId}"/>
                        <input type="hidden" name="step" value="${step}"/>
                        <input type="hidden" name="action" value="markdone"/>
                        <button type="submit" class="btn btn-outline-ssa"
                                onclick="return confirm('Mark this step done without a response review? Use this for a group already set up in Summit or entered by hand.');">
                            <i class="bi bi-check2-circle"></i> Mark done (manual)
                        </button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</div>
</body>
</html>
