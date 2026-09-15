<%--
  T237 check #3 -- card-decline employer designation admin (V114). Lists the employers this PSP
  has opted in to card-decline monitoring, with add and remove. Same layout, gate and flash shape
  as summitEmployerFlagAdmin25.jsp. Row presence is the designation: there is no edit state.
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
    <title>Card-Decline Employers</title>
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
        .map-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .map-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .map-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: middle; }
        .map-table tbody tr:hover { background: #f8f9fa; }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
        .form-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-top: 0.9rem; font-size: 0.85rem;
        }
        .form-card label { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em;
            color: #6c757d; font-weight: 600; margin-bottom: 0.15rem; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .unmatchable { color: #b02a37; font-size: 0.75rem; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-credit-card-2-front me-1"></i>Card-Decline Employers
        </h1>
        <a class="ms-auto small" href="${pageContext.request.contextPath}/AuditCardDeclines">Card Declines &rarr;</a>
    </div>

    <div class="rc-body">

        <c:if test="${not empty sessionScope.auditDeclineEmployerMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.auditDeclineEmployerMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditDeclineEmployerMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.auditDeclineEmployerError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.auditDeclineEmployerError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="auditDeclineEmployerError" scope="session"/>
        </c:if>

        <div class="status-card">
            Card-decline monitoring is opt-in per employer. The Card Declines audit evaluates only the
            employers listed here, matched on their Summit EmployerID. With no employers designated the
            check reports <em>Not configured</em>, never <em>OK</em>. Add an employer here as part of its
            plan setup; remove it to stop monitoring.
        </div>

        <c:choose>
            <c:when test="${empty employerRows}">
                <div class="empty-state">
                    No employers are designated for card-decline monitoring. Add one below.
                </div>
            </c:when>
            <c:otherwise>
                <table class="map-table">
                    <thead>
                    <tr>
                        <th>Employer</th>
                        <th>Summit EmployerID</th>
                        <th>AMS id</th>
                        <th>Designated by</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="r" items="${employerRows}">
                        <tr>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty r.employerName}"><c:out value="${r.employerName}"/></c:when>
                                    <c:otherwise><span class="text-muted">(employer ${r.employerId} not found)</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <span class="mono">${r.altId}</span>
                                <c:if test="${r.unmatchable}">
                                    <div class="unmatchable">No Summit EmployerID in AMS &mdash; will not match any decline row until a J1 refresh supplies one.</div>
                                </c:if>
                            </td>
                            <td><span class="mono">${r.employerId}</span></td>
                            <td><c:out value="${r.createdBy}"/></td>
                            <td class="text-end">
                                <form method="post" action="${pageContext.request.contextPath}/AuditDeclineEmployerAdmin"
                                      class="d-inline"
                                      onsubmit="return confirm('Stop card-decline monitoring for this employer?');">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${r.id}">
                                    <button type="submit" class="btn btn-sm btn-outline-danger py-0 px-2">Remove</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>

        <div class="form-card">
            <div class="fw-semibold mb-2" style="color: var(--ssa, #0d5681);">Designate an employer</div>

            <form method="post" action="${pageContext.request.contextPath}/AuditDeclineEmployerAdmin">
                <input type="hidden" name="action" value="save">
                <div class="row g-2 align-items-end">
                    <div class="col-md-6">
                        <label for="employerId">Employer</label>
                        <%-- The Summit EmployerID is shown beside the name because it is what the
                             check matches on; an employer showing 0 has not been refreshed from a
                             J1 export that carries EmployerID and cannot match a decline row yet. --%>
                        <select class="form-select form-select-sm" id="employerId" name="employerId" required>
                            <option value="">— choose —</option>
                            <c:forEach var="e" items="${employers}">
                                <option value="${e.id}">
                                    <c:out value="${e.employerName}"/> — Summit EmployerID <c:out value="${e.altId}"/><c:if test="${e.altId le 0}"> (none)</c:if>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="mt-2">
                    <button type="submit" class="btn btn-sm btn-ssa">Add</button>
                </div>
            </form>
        </div>

    </div>
</div>
</body>
</html>
