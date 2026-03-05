<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Applications</title>
    <style>
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px); overflow: hidden;
        }
        .audit-body { flex: 1 1 auto; overflow-y: auto; padding: 1rem 0; }
        .app-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.03em; color: #6c757d;
        }
        .app-table td { font-size: 0.9rem; vertical-align: middle; }
        .btn-outline-ssa {
            color: var(--ssa); border-color: var(--ssa);
        }
        .btn-outline-ssa:hover {
            background: var(--ssa); color: #fff;
        }
        .btn-ssa {
            background: var(--ssa); color: #fff; border: none;
        }
        .btn-ssa:hover {
            background: #0a4566; color: #fff;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Applications" scope="request"/>
    <c:set var="pageIcon" value="bi-file-earmark-check" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="audit-wrap">
        <%-- Toolbar --%>
        <div class="d-flex align-items-center justify-content-between py-2">
            <div>
                <span class="fw-semibold" style="font-size:1.1rem;">
                    <i class="bi bi-file-earmark-check me-1"></i>Applications
                </span>
                <span class="text-muted ms-2" style="font-size:0.85rem;">All in-flight and submitted applications across your PSP</span>
            </div>
        </div>

        <%-- Scrollable body --%>
        <div class="audit-body">

            <%-- Section 1: In Progress --%>
            <div class="card mb-4">
                <div class="card-header hdr-bar">
                    <i class="bi bi-pencil-square me-2"></i>In Progress
                </div>
                <div class="card-body p-0">
                    <table class="table table-hover mb-0 app-table">
                        <thead>
                            <tr>
                                <th>Prospect</th>
                                <th>Agency</th>
                                <th>Agent</th>
                                <th>Started</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty inProgressList}">
                                    <tr><td colspan="5" class="text-center text-muted py-3">
                                        <i class="bi bi-inbox" style="font-size:1.5rem; display:block; margin-bottom:0.3rem;"></i>
                                        No in-progress applications
                                    </td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="p" items="${inProgressList}">
                                        <tr>
                                            <td class="fw-semibold">${p.getProspect().getName()}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${agencyNameMap[p.getId()] != null}">
                                                        ${agencyNameMap[p.getId()]}
                                                    </c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${p.getProspect().getAgent() != null}">
                                                        ${p.getProspect().getAgent().getFirstName()} ${p.getProspect().getAgent().getLastName()}
                                                    </c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:if test="${p.getApplication() != null && p.getApplication().getDateStarted() != null}">
                                                    <fmt:formatDate value="${p.getApplication().getDateStarted()}" pattern="MM/dd/yyyy"/>
                                                </c:if>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${isPspAdmin && takeOverEligible.contains(p.getId())}">
                                                        <form method="post" action="ApplicationsHome" style="display:inline;">
                                                            <input type="hidden" name="action" value="takeOver">
                                                            <input type="hidden" name="proposalId" value="${p.getId()}">
                                                            <button type="submit" class="btn btn-outline-ssa btn-sm"
                                                                    onclick="return confirm('Take over this opportunity?')">
                                                                <i class="bi bi-person-fill-check me-1"></i>Take Over
                                                            </button>
                                                        </form>
                                                    </c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

            <%-- Section 2: Awaiting Review --%>
            <div class="card">
                <div class="card-header hdr-bar">
                    <i class="bi bi-clipboard-check me-2"></i>Awaiting Review
                </div>
                <div class="card-body p-0">
                    <table class="table table-hover mb-0 app-table">
                        <thead>
                            <tr>
                                <th>Prospect</th>
                                <th>Agency</th>
                                <th>Agent</th>
                                <th>Submitted</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty pendingReviewList}">
                                    <tr><td colspan="5" class="text-center text-muted py-3">
                                        <i class="bi bi-inbox" style="font-size:1.5rem; display:block; margin-bottom:0.3rem;"></i>
                                        No applications awaiting review
                                    </td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="p" items="${pendingReviewList}">
                                        <tr>
                                            <td>
                                                <a href="ReviewApplication?id=${p.getId()}" class="fw-semibold text-decoration-none">
                                                    ${p.getProspect().getName()}
                                                </a>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${agencyNameMap[p.getId()] != null}">
                                                        ${agencyNameMap[p.getId()]}
                                                    </c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${p.getProspect().getAgent() != null}">
                                                        ${p.getProspect().getAgent().getFirstName()} ${p.getProspect().getAgent().getLastName()}
                                                    </c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:if test="${p.getApplication() != null && p.getApplication().getDateSubmitted() != null}">
                                                    <fmt:formatDate value="${p.getApplication().getDateSubmitted()}" pattern="MM/dd/yyyy"/>
                                                </c:if>
                                            </td>
                                            <td>
                                                <a href="ReviewApplication?id=${p.getId()}" class="btn btn-ssa btn-sm">
                                                    <i class="bi bi-clipboard-check me-1"></i>Review
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

        </div><%-- /audit-body --%>
    </div><%-- /audit-wrap --%>
</div>
</body>
</html>
