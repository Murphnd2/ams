<%--
  T237 -- card-decline detail page. Renders CardDeclineCheck.readLive() for the current request
  only -- nothing on this page is stored, and every value here is discarded when the response is
  written, exactly like auditFundedPurse25.jsp.

  PII / no persistence -- same posture as auditIchraUncoded25.jsp. Participant System ID, Employer
  SystemID, decline reasons, dates and amounts come straight from the Transaction export, fetched
  fresh on this one request. The employer and participant NAMES on this page are resolved
  in-request from AMS rows already in hand (the designated-employer join, and employee by
  Participant System ID) and discarded with the response; nothing here is persisted, and audit_run
  holds counts only (LA-40, LA-43). No SSN is read from anywhere. Do not add a server-side cache,
  a hidden field that resubmits this content, or any further join for identity.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Card Declines</title>
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
        .status-card dl { display: grid; grid-template-columns: 200px 1fr; row-gap: 0.4rem; margin: 0; }
        .status-card dt { font-weight: 600; color: #495057; }
        .status-card dd { margin: 0; }
        .section-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.85rem;
            margin: 1rem 0 0.5rem;
        }
        .finding-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .finding-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .finding-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: top; }
        .finding-table td.num { text-align: right; font-variant-numeric: tabular-nums; }
        .finding-table th.num { text-align: right; }
        .finding-table tbody tr:hover { background: #f8f9fa; }
        .decline-count-badge {
            display: inline-block; min-width: 1.8rem; padding: 0.15rem 0.5rem;
            border-radius: 999px; font-weight: 700; font-size: 0.85rem;
            background: #f8d7da; color: #842029; text-align: center;
        }
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
            <i class="bi bi-bell me-1"></i>Card Declines
        </h1>
        <a class="ms-auto small" href="${pageContext.request.contextPath}/AuditDeclineEmployerAdmin">Designated employers &rarr;</a>
        <a href="AuditHub" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Audit Hub
        </a>
    </div>

    <div class="ai-body">

        <c:if test="${not empty sessionScope.auditCardDeclineAckMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.auditCardDeclineAckMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditCardDeclineAckMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.auditCardDeclineAckError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.auditCardDeclineAckError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditCardDeclineAckError" scope="session"/>
        </c:if>

        <c:choose>
            <c:when test="${not empty snapshotError}">
                <div class="alert alert-danger" style="font-size:0.85rem;">
                    <i class="bi bi-exclamation-triangle me-1"></i>${snapshotError}
                </div>
            </c:when>
            <c:otherwise>

                <div class="status-card">
                    <dl>
                        <dt>Window</dt>
                        <dd>Last ${windowDays} day(s) — ${windowRangeDisplay}</dd>
                        <dt>Designated employers</dt>
                        <dd>${designatedEmployerCount} <span class="text-muted">(only declines at these employers are evaluated)</span></dd>
                        <dt>Export file</dt>
                        <dd><code>${exportFileName}</code></dd>
                        <dt>Export timestamp</dt>
                        <dd>${exportTimestampDisplay} (${exportAgeHoursDisplay} old)</dd>
                        <dt>Participants with declines</dt>
                        <dd>${fn:length(findings)} <span class="text-muted">(${suppressedCount} acknowledged — see below)</span></dd>
                        <dt>Total decline rows</dt>
                        <dd>${totalDeclineRows}</dd>
                        <dt>Distinct employers</dt>
                        <dd>${distinctEmployerCount}</dd>
                        <dt>Distinct decline reasons</dt>
                        <dd>${distinctReasonCount}</dd>
                    </dl>
                </div>

                <div class="status-card">
                    A participant is listed when a card transaction was declined in the window. A
                    blocked card or a funds shortfall on a premium-bearing card means a premium
                    payment failed — confirm coverage with the carrier before the grace period ends.
                    Repeated declines of the same amount indicate a recurring charge retrying.
                    Unqualified-merchant declines may indicate an MCC that is not enabled, or a
                    merchant that is enabled but cannot supply eligible-item data.
                </div>

                <div class="section-title">Participants</div>
                <c:choose>
                    <c:when test="${empty findings}">
                        <div class="empty-state">
                            <i class="bi bi-check-circle fs-3 d-block mb-2"></i>
                            No card declines in the window.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>Participant System ID</th>
                                    <th>Participant</th>
                                    <th>Participant Custom ID</th>
                                    <th>Employer SystemID</th>
                                    <th>Employer</th>
                                    <th class="num">Declines</th>
                                    <th>Decline reasons</th>
                                    <th>Most recent decline</th>
                                    <th class="num">Largest amount</th>
                                    <th>MCCs</th>
                                    <th>Acknowledge</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${findings}">
                                    <tr>
                                        <td>
                                            <c:set var="participantUrl" value="${participantSummitUrls[r.participantSystemId()]}"/>
                                            <c:choose>
                                                <c:when test="${not empty participantUrl}">
                                                    <a href="${participantUrl}" target="_blank" rel="noopener">${r.participantSystemId()} <i class="bi bi-box-arrow-up-right" style="font-size: 0.7em;"></i></a>
                                                </c:when>
                                                <c:otherwise>${r.participantSystemId()}</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <%-- Name resolved in-request from employee by Participant System ID;
                                             blank until the next J2 refresh has seen this participant. --%>
                                        <td><c:out value="${r.participantName()}"/></td>
                                        <td>${r.participantCustomId()}</td>
                                        <td>${r.employerSystemId()}</td>
                                        <td><c:out value="${r.employerName()}"/></td>
                                        <td class="num"><span class="decline-count-badge">${r.declineCount()}</span></td>
                                        <td>${r.reasonBreakdown()}</td>
                                        <td>${r.mostRecentDeclineDate()}</td>
                                        <td class="num">${r.maxAmount()}</td>
                                        <td>${r.mccList()}</td>
                                        <td class="text-end">
                                            <%-- Both buttons post the row's CURRENT decline count and most-recent-decline
                                                 date -- what a Handled acknowledgment is scoped to. The server nulls both
                                                 out for Ignore regardless of what is posted here (V115: neither is stored
                                                 for that state). --%>
                                            <form method="post" action="${pageContext.request.contextPath}/AuditCardDeclines"
                                                  class="d-flex align-items-center gap-1 justify-content-end flex-wrap">
                                                <input type="hidden" name="action" value="ack">
                                                <input type="hidden" name="participantId" value="${r.participantSystemId()}">
                                                <input type="hidden" name="observedCount" value="${r.declineCount()}">
                                                <input type="hidden" name="observedThrough" value="${r.mostRecentDeclineDate()}">
                                                <input type="text" name="note" class="form-control form-control-sm"
                                                       style="width:110px; display:inline-block;" placeholder="note (optional)">
                                                <button type="submit" name="state" value="HANDLED"
                                                        class="btn btn-sm btn-outline-success py-0 px-2"
                                                        onclick="return confirm('Mark participant ${r.participantSystemId()} handled? Suppressed unless a new decline exceeds ${r.declineCount()} or a date after ${r.mostRecentDeclineDate()}.');">Handled</button>
                                                <button type="submit" name="state" value="IGNORED"
                                                        class="btn btn-sm btn-outline-secondary py-0 px-2"
                                                        onclick="return confirm('Ignore participant ${r.participantSystemId()}? Suppressed until removed, regardless of new activity.');">Ignore</button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="section-title">Acknowledged</div>
                <c:choose>
                    <c:when test="${empty acknowledged}">
                        <div class="empty-state">
                            <i class="bi bi-dash-circle fs-3 d-block mb-2"></i>
                            No acknowledged findings.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>Participant System ID</th>
                                    <th>Participant</th>
                                    <th>State</th>
                                    <th>Observed at acknowledgment</th>
                                    <th>Current activity</th>
                                    <th>Note</th>
                                    <th>Acknowledged by</th>
                                    <th>Acknowledged at</th>
                                    <th></th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="a" items="${acknowledged}">
                                    <tr>
                                        <td>${a.participantSystemId()}</td>
                                        <td><c:out value="${a.participantName()}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.ackState() == 'HANDLED'}"><span class="badge bg-success">Handled</span></c:when>
                                                <c:otherwise><span class="badge bg-secondary">Ignored</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <%-- Handled is state-scoped: this is what the finding looked like when
                                                     acknowledged, not a permanent suppression. Ignored carries neither. --%>
                                                <c:when test="${a.ackState() == 'HANDLED'}">${a.observedCountDisplay()} decline(s) through ${a.observedThroughDisplay()}</c:when>
                                                <c:otherwise>&mdash;</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${a.currentDeclineCount()} decline(s), most recent ${a.currentMostRecentDeclineDisplay()}</td>
                                        <td><c:out value="${a.note()}"/></td>
                                        <td><c:out value="${a.ackedBy()}"/></td>
                                        <td>${a.ackedAtDisplay()}</td>
                                        <td class="text-end">
                                            <form method="post" action="${pageContext.request.contextPath}/AuditCardDeclines"
                                                  class="d-inline"
                                                  onsubmit="return confirm('Un-acknowledge this finding? It will surface again if still active.');">
                                                <input type="hidden" name="action" value="unack">
                                                <input type="hidden" name="id" value="${a.ackId()}">
                                                <button type="submit" class="btn btn-sm btn-outline-danger py-0 px-2">Un-acknowledge</button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="section-title">Unqualified-merchant declines by MCC</div>
                <c:choose>
                    <c:when test="${empty mccSummary}">
                        <div class="empty-state">
                            <i class="bi bi-dash-circle fs-3 d-block mb-2"></i>
                            No unqualified-merchant declines in the window.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>MCC</th>
                                    <th class="num">Decline count</th>
                                    <th class="num">Distinct participants</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="m" items="${mccSummary}">
                                    <tr>
                                        <td>${m.mcc()}</td>
                                        <td class="num">${m.declineCount()}</td>
                                        <td class="num">${m.participantCount()}</td>
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
