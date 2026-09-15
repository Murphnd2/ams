<%--
  T237 -- funded-purse / no-disbursement detail page. Renders
  FundedPurseNoDisbursementCheck.readLive() for the current request only -- nothing on this page
  is stored, and every value here is discarded when the response is written, exactly like
  auditIchraUncoded25.jsp.

  No persistence -- Participant_ID / ParticipantPlan_ID and the two totals come straight from the
  Participant Plan History export, fetched fresh on this one request. The export carries no
  participant name or SSN and this page joins to nothing to obtain them. Do not add a server-side
  cache or a hidden field that resubmits this content.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Funded Purses With No Disbursement</title>
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
        .ai-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .status-card dl { display: grid; grid-template-columns: 180px 1fr; row-gap: 0.4rem; margin: 0; }
        .status-card dt { font-weight: 600; color: #495057; }
        .status-card dd { margin: 0; }
        .finding-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .finding-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .finding-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; }
        .finding-table td.num { text-align: right; font-variant-numeric: tabular-nums; }
        .finding-table th.num { text-align: right; }
        .finding-table tbody tr:hover { background: #f8f9fa; }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-bell me-1"></i>Funded Purses With No Disbursement
        </h1>
        <a href="AuditHub" class="btn btn-sm btn-outline-secondary ms-auto">
            <i class="bi bi-arrow-left me-1"></i>Audit Hub
        </a>
    </div>

    <div class="ai-body">

        <c:choose>
            <c:when test="${not empty snapshotError}">
                <div class="alert alert-danger" style="font-size:0.85rem;">
                    <i class="bi bi-exclamation-triangle me-1"></i>${snapshotError}
                </div>
            </c:when>
            <c:otherwise>

                <div class="status-card">
                    <dl>
                        <dt>Evaluation month</dt>
                        <dd>${evaluationMonthDisplay}</dd>
                        <dt>Export file</dt>
                        <dd><code>${exportFileName}</code></dd>
                        <dt>Export timestamp</dt>
                        <dd>${exportTimestampDisplay} (${exportAgeHoursDisplay} old)</dd>
                        <dt>Findings</dt>
                        <dd>${fn:length(findings)} purse(s)</dd>
                        <dt>Unmapped types</dt>
                        <dd>
                            ${ignoredTypeCount} distinct <code>TransactionType</code> value(s) in neither configured list — ignored
                            <c:if test="${not empty ignoredTypeValues}">
                                <br/>
                                <%-- Original file casing, pasteable straight into ssa.properties. First dozen only; the
                                     rest are counted, not dropped silently. --%>
                                <c:forEach var="v" items="${ignoredTypeValues}" varStatus="loop" end="11">
                                    <code>${v}</code><c:if test="${!loop.last && loop.index lt 11}">, </c:if>
                                </c:forEach>
                                <c:if test="${fn:length(ignoredTypeValues) gt 12}">
                                    , and ${fn:length(ignoredTypeValues) - 12} more
                                </c:if>
                            </c:if>
                        </dd>
                    </dl>
                </div>

                <div class="status-card">
                    A purse is listed when it received a contribution in the evaluation month and paid
                    nothing out. For a PremiumPath purse that means the premium was never attempted —
                    confirm the participant's coverage with the carrier before the grace period ends.
                    Expected-amount and partial-payment comparison are not performed here.
                </div>

                <c:choose>
                    <c:when test="${empty findings}">
                        <div class="empty-state">
                            <i class="bi bi-check-circle fs-3 d-block mb-2"></i>
                            Every funded purse in ${evaluationMonthDisplay} shows a disbursement.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>Participant ID</th>
                                    <th>Participant Plan ID</th>
                                    <th>Plan</th>
                                    <th class="num">Contributions</th>
                                    <th class="num">Disbursements</th>
                                    <th>Month</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${findings}">
                                    <tr>
                                        <td>${r.participantId()}</td>
                                        <td>${r.participantPlanId()}</td>
                                        <td>${r.planName()}</td>
                                        <td class="num">${r.contributionTotal()}</td>
                                        <td class="num">${r.disbursementTotal()}</td>
                                        <td>${evaluationMonthDisplay}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

            </c:otherwise>
        </c:choose>

        <form method="POST" action="AuditHub" class="mt-3">
            <input type="hidden" name="action" value="runnow">
            <button type="submit" class="btn btn-sm btn-outline-primary">
                <i class="bi bi-play-fill me-1"></i>Run audit now
            </button>
        </form>

    </div>
</div>

</body>
</html>
