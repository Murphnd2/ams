<%--
  T237 -- card-status detail page. Renders CardStatusCheck.readLive() for the current request only
  -- nothing on this page is stored, and every value here is discarded when the response is written,
  exactly like auditCardDeclines25.jsp.

  PII / no persistence -- same posture as auditCardDeclines25.jsp. UserID, ParticipantID/DependentID,
  card status and expiration come straight from the Debit Card Participants export, fetched fresh on
  this one request. The employer and participant NAMES on this page are resolved in-request from AMS
  rows already in hand (the designated-employer join, and employee by ParticipantID) and discarded
  with the response; nothing here is persisted, and audit_run holds counts only (LA-40, LA-43). No
  SSN is read from anywhere. Do not add a server-side cache, a hidden field that resubmits this
  content, or any further join for identity.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Card Status</title>
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
        .status-badge {
            display: inline-block; padding: 0.15rem 0.5rem;
            border-radius: 999px; font-weight: 700; font-size: 0.78rem;
            background: #f8d7da; color: #842029;
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
            <i class="bi bi-credit-card me-1"></i>Card Status
        </h1>
        <a class="ms-auto small" href="${pageContext.request.contextPath}/AuditDeclineEmployerAdmin">Designated employers &rarr;</a>
        <a href="AuditHub" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-arrow-left me-1"></i>Audit Hub
        </a>
    </div>

    <div class="ai-body">

        <c:if test="${not empty sessionScope.auditCardStatusAckMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.auditCardStatusAckMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditCardStatusAckMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.auditCardStatusAckError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.auditCardStatusAckError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditCardStatusAckError" scope="session"/>
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
                        <dt>Expiring-soon horizon</dt>
                        <dd>${warnDays} day(s)</dd>
                        <dt>Designated employers</dt>
                        <dd>${designatedEmployerCount} <span class="text-muted">(only card records at these employers are evaluated — shared with Card Declines)</span></dd>
                        <dt>Export file</dt>
                        <dd><code>${exportFileName}</code></dd>
                        <dt>Export timestamp</dt>
                        <dd>${exportTimestampDisplay} (${exportAgeHoursDisplay} old)</dd>
                        <dt>Problem-status findings</dt>
                        <dd>${fn:length(problemRows)}</dd>
                        <dt>Expired/expiring findings</dt>
                        <dd>${fn:length(expiryRows)}</dd>
                        <dt>Acknowledged</dt>
                        <dd>${suppressedCount}</dd>
                        <dt>Superseded</dt>
                        <dd>${supersededCount} <span class="text-muted">(${supersededByUsable} by a usable card, ${supersededByProblem} by a newer problem card — see below)</span></dd>
                        <dt>Card records at designated employers</dt>
                        <dd>${matchedRows} <span class="text-muted">(${distinctEmployerCount} distinct employer(s); ${undesignatedRows} row(s) at undesignated employers ignored)</span></dd>
                    </dl>
                </div>

                <div class="status-card">
                    A card is listed when its status prevents use, or when it is expired or expiring
                    soon. On a premium-bearing card either condition means the next premium payment
                    will fail — reissue the card or confirm coverage with the carrier before the
                    grace period ends. A card on hold is the same condition the Card Declines audit
                    reports as a blocked-card decline. Requested cards are excluded: they were never
                    issued.
                </div>

                <div class="section-title">Problem status</div>
                <c:choose>
                    <c:when test="${empty problemRows}">
                        <div class="empty-state">
                            <i class="bi bi-check-circle fs-3 d-block mb-2"></i>
                            No cards with a problem status.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>Participant</th>
                                    <th>ParticipantID</th>
                                    <th>DependentID</th>
                                    <th>UserID</th>
                                    <th>Employer</th>
                                    <th>Last four</th>
                                    <th>Status</th>
                                    <th>Expiration</th>
                                    <th>Issued</th>
                                    <th>Mailed</th>
                                    <th>Acknowledge</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${problemRows}">
                                    <tr>
                                        <td><c:out value="${r.participantName()}"/></td>
                                        <td>
                                            <c:set var="participantUrl" value="${participantSummitUrls[r.participantId()]}"/>
                                            <c:choose>
                                                <c:when test="${not empty participantUrl}">
                                                    <a href="${participantUrl}" target="_blank" rel="noopener">${r.participantId()} <i class="bi bi-box-arrow-up-right" style="font-size: 0.7em;"></i></a>
                                                </c:when>
                                                <c:otherwise>${r.participantId()}</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${r.dependentId()}</td>
                                        <td>${r.userId()}</td>
                                        <td><c:out value="${r.employerName()}"/></td>
                                        <td>${r.lastFour()}</td>
                                        <td><span class="status-badge">${r.status()}</span></td>
                                        <td>${r.expirationDateDisplay()}</td>
                                        <td>${r.issuedDate()}</td>
                                        <td>${r.mailedDate()}</td>
                                        <td class="text-end">
                                            <form method="post" action="${pageContext.request.contextPath}/AuditCardStatus"
                                                  class="d-flex align-items-center gap-1 justify-content-end flex-wrap">
                                                <input type="hidden" name="action" value="ack">
                                                <input type="hidden" name="findingKey" value="${r.findingKey()}">
                                                <input type="hidden" name="observedStatusId" value="${r.participantCardStatusId()}">
                                                <input type="hidden" name="observedThrough" value="${r.expirationDateDisplay()}">
                                                <input type="text" name="note" class="form-control form-control-sm"
                                                       style="width:110px; display:inline-block;" placeholder="note (optional)">
                                                <button type="submit" name="state" value="HANDLED"
                                                        class="btn btn-sm btn-outline-success py-0 px-2"
                                                        onclick="return confirm('Mark this card handled? Suppressed unless status or expiration changes.');">Handled</button>
                                                <button type="submit" name="state" value="IGNORED"
                                                        class="btn btn-sm btn-outline-secondary py-0 px-2"
                                                        onclick="return confirm('Ignore this card? Suppressed until removed, regardless of new activity.');">Ignore</button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="section-title">Expired or expiring</div>
                <c:choose>
                    <c:when test="${empty expiryRows}">
                        <div class="empty-state">
                            <i class="bi bi-check-circle fs-3 d-block mb-2"></i>
                            No cards expired or expiring within the window.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>Participant</th>
                                    <th>ParticipantID</th>
                                    <th>DependentID</th>
                                    <th>UserID</th>
                                    <th>Employer</th>
                                    <th>Last four</th>
                                    <th>Status</th>
                                    <th>Expiration</th>
                                    <th>Issued</th>
                                    <th>Mailed</th>
                                    <th>Acknowledge</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${expiryRows}">
                                    <tr>
                                        <td><c:out value="${r.participantName()}"/></td>
                                        <td>
                                            <c:set var="participantUrl2" value="${participantSummitUrls[r.participantId()]}"/>
                                            <c:choose>
                                                <c:when test="${not empty participantUrl2}">
                                                    <a href="${participantUrl2}" target="_blank" rel="noopener">${r.participantId()} <i class="bi bi-box-arrow-up-right" style="font-size: 0.7em;"></i></a>
                                                </c:when>
                                                <c:otherwise>${r.participantId()}</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${r.dependentId()}</td>
                                        <td>${r.userId()}</td>
                                        <td><c:out value="${r.employerName()}"/></td>
                                        <td>${r.lastFour()}</td>
                                        <td>${r.status()}</td>
                                        <td><span class="status-badge">${r.expirationDateDisplay()}</span></td>
                                        <td>${r.issuedDate()}</td>
                                        <td>${r.mailedDate()}</td>
                                        <td class="text-end">
                                            <form method="post" action="${pageContext.request.contextPath}/AuditCardStatus"
                                                  class="d-flex align-items-center gap-1 justify-content-end flex-wrap">
                                                <input type="hidden" name="action" value="ack">
                                                <input type="hidden" name="findingKey" value="${r.findingKey()}">
                                                <input type="hidden" name="observedStatusId" value="${r.participantCardStatusId()}">
                                                <input type="hidden" name="observedThrough" value="${r.expirationDateDisplay()}">
                                                <input type="text" name="note" class="form-control form-control-sm"
                                                       style="width:110px; display:inline-block;" placeholder="note (optional)">
                                                <button type="submit" name="state" value="HANDLED"
                                                        class="btn btn-sm btn-outline-success py-0 px-2"
                                                        onclick="return confirm('Mark this card handled? Suppressed unless status or expiration changes.');">Handled</button>
                                                <button type="submit" name="state" value="IGNORED"
                                                        class="btn btn-sm btn-outline-secondary py-0 px-2"
                                                        onclick="return confirm('Ignore this card? Suppressed until removed, regardless of new activity.');">Ignore</button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

                <details class="superseded-details">
                    <summary class="section-title" style="display:list-item; cursor:pointer; margin-top:1rem;">Superseded by a newer card (${supersededCount})</summary>
                    <div class="status-card" style="margin-top:0.5rem;">
                        A card here would otherwise be flagged above, but the same UserID holds a newer
                        card, so this older record needs no separate action. Either condition can be why:
                        a newer usable card (replaced), or a newer card that is itself in a problem
                        status — that newer card is listed above (in Problem status or Expired/expiring),
                        so the older record would only be a duplicate of the same problem. Nothing here
                        needs action; it is shown so the exclusion can be verified, not just trusted.
                    </div>
                    <c:choose>
                        <c:when test="${empty supersededRows}">
                            <div class="empty-state">
                                <i class="bi bi-dash-circle fs-3 d-block mb-2"></i>
                                No superseded cards.
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div style="overflow-x:auto;">
                                <table class="finding-table">
                                    <thead>
                                    <tr>
                                        <th>Participant</th>
                                        <th>ParticipantID</th>
                                        <th>DependentID</th>
                                        <th>UserID</th>
                                        <th>Employer</th>
                                        <th>Last four</th>
                                        <th>Status</th>
                                        <th>Expiration</th>
                                        <th>Issued</th>
                                        <th>Mailed</th>
                                        <th>Replacement card</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="r" items="${supersededRows}">
                                        <tr>
                                            <td><c:out value="${r.participantName()}"/></td>
                                            <td>
                                                <c:set var="participantUrl3" value="${participantSummitUrls[r.participantId()]}"/>
                                                <c:choose>
                                                    <c:when test="${not empty participantUrl3}">
                                                        <a href="${participantUrl3}" target="_blank" rel="noopener">${r.participantId()} <i class="bi bi-box-arrow-up-right" style="font-size: 0.7em;"></i></a>
                                                    </c:when>
                                                    <c:otherwise>${r.participantId()}</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>${r.dependentId()}</td>
                                            <td>${r.userId()}</td>
                                            <td><c:out value="${r.employerName()}"/></td>
                                            <td>${r.lastFour()}</td>
                                            <td>${r.status()}</td>
                                            <td>${r.expirationDateDisplay()}</td>
                                            <td>${r.issuedDate()}</td>
                                            <td>${r.mailedDate()}</td>
                                            <td>${r.replacementLastFour()} / ${r.replacementStatus()} / ${r.replacementCardDateDisplay()}</td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </details>

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
                                    <th>Participant</th>
                                    <th>UserID</th>
                                    <th>Condition(s)</th>
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
                                        <td><c:out value="${a.participantName()}"/></td>
                                        <td>${a.userId()}</td>
                                        <td>${a.conditions()}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.ackState() == 'HANDLED'}"><span class="badge bg-success">Handled</span></c:when>
                                                <c:otherwise><span class="badge bg-secondary">Ignored</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.ackState() == 'HANDLED'}">status ${a.observedStatusDisplay()}, expiring ${a.observedThroughDisplay()}</c:when>
                                                <c:otherwise>&mdash;</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>status ${a.currentStatusDisplay()}, expiring ${a.currentExpirationDisplay()}</td>
                                        <td><c:out value="${a.note()}"/></td>
                                        <td><c:out value="${a.ackedBy()}"/></td>
                                        <td>${a.ackedAtDisplay()}</td>
                                        <td class="text-end">
                                            <form method="post" action="${pageContext.request.contextPath}/AuditCardStatus"
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
