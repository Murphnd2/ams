<%--
  T237 -- ICHRA uncoded-participants detail page. Renders IchraUncodedParticipantsCheck.readLive()
  for the current request only -- nothing on this page is stored, and every value here is
  discarded when the response is written, exactly like SummitResponseServlet's response render.

  PII / no persistence -- same note as summitResponse25.jsp: employer, participant name and
  Participant_ID come straight from the export, fetched fresh on this one request. Do not add a
  server-side cache or a hidden field that resubmits this content.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ICHRA Participants Without a Custom ID</title>
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
        .instructions ol { margin: 0.4rem 0 0.6rem 1.2rem; padding: 0; }
        .instructions li { margin-bottom: 0.3rem; }
        .finding-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .finding-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .finding-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; }
        .finding-table tbody tr:hover { background: #f8f9fa; }
        .finding-table code { font-size: 0.8rem; }
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
            <i class="bi bi-bell me-1"></i>ICHRA Participants Without a Custom ID
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
            <c:when test="${!prefixConfigured}">
                <div class="alert alert-danger" style="font-size:0.85rem;">
                    <i class="bi bi-exclamation-triangle me-1"></i>
                    <code>SUMMIT_TPA_ID_PREFIX</code> is not configured on this installation — the
                    coded value cannot be computed. Set it in <code>ssa.properties</code>.
                </div>
            </c:when>
            <c:otherwise>

                <div class="status-card">
                    <dl>
                        <dt>Export file</dt>
                        <dd><code>${exportFileName}</code></dd>
                        <dt>Export timestamp</dt>
                        <dd>${exportTimestampDisplay} (${exportAgeHoursDisplay} old)</dd>
                        <dt>Findings</dt>
                        <dd>${fn:length(displayRows)} participant(s)</dd>
                    </dl>
                </div>

                <div class="status-card instructions">
                    <strong>For each participant, in this order:</strong>
                    <ol>
                        <li><strong>Send the notice</strong> — Summit → Processing → On Demand Processing → Event Notifications → select the employer → select the ICHRA notice event → <strong>Just These</strong> → select this participant → Perform Processing; approve it in Process Approvals.</li>
                        <li><strong>Then code the participant</strong> — set their Participant Custom ID in Summit to the value shown. Coding without sending the notice removes them from this list without a notice.</li>
                    </ol>
                    <p class="mb-0">Do not push a Demographics file for these participants — with no custom ID it would create duplicates.</p>
                </div>

                <c:choose>
                    <c:when test="${empty displayRows}">
                        <div class="empty-state">
                            <i class="bi bi-check-circle fs-3 d-block mb-2"></i>
                            No uncoded Active ICHRA participants found in this export.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table class="finding-table">
                                <thead>
                                <tr>
                                    <th>Employer</th>
                                    <th>Employer ID</th>
                                    <th>Participant</th>
                                    <th>Participant ID</th>
                                    <th>Coded value to set</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="r" items="${displayRows}">
                                    <tr>
                                        <td>${r.employerName}</td>
                                        <td>${r.employerKey}</td>
                                        <td>${r.firstName} ${r.lastName}</td>
                                        <td>${r.participantId}</td>
                                        <td><code>${r.codedValue}</code></td>
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
