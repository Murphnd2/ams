<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Application Review</title>
    <style>
        .status-badge { font-size: 0.8rem; }
        .los-badge { font-size: 0.75rem; }
        .table-hover tbody tr { cursor: pointer; }
        .ra-wrap { max-width: 1100px; margin: 0 auto; padding: 0 1rem; }
        @media (max-width: 767.98px) { .ra-wrap { padding: 0 0.5rem; } }
        .filter-pill {
            background: transparent;
            border: 1px solid var(--ssa);
            color: var(--ssa);
            border-radius: 20px;
            padding: 0.2rem 0.85rem;
            font-size: 0.8rem;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        .filter-pill:hover {
            background: rgba(13, 86, 129, 0.08);
        }
        .filter-pill.active {
            background: var(--ssa);
            color: white;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Application Review" scope="request"/>
    <c:set var="pageIcon" value="bi-clipboard-check" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="ra-wrap mt-3">
    <%-- Header Bar --%>
    <div class="hdr-bar d-flex justify-content-between align-items-center mb-3">
        <span><i class="bi bi-clipboard-check me-1"></i>Application Review</span>
        <div class="d-flex gap-2">
            <c:if test="${sessionScope.isPspAdmin || sessionScope.isPspUser || sessionScope.isPspSales}">
                <a href="GenerateProp25" class="btn btn-sm btn-outline-light">
                    <i class="bi bi-building-add me-1"></i>Manual Setup
                </a>
            </c:if>
            <a href="ProposalBuilder" class="btn btn-sm btn-outline-light">
                <i class="bi bi-plus-lg me-1"></i>New Proposal
            </a>
            <c:if test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
                <a href="AgentHome" class="btn btn-sm btn-outline-light">
                    <i class="bi bi-kanban me-1"></i>Pipeline
                </a>
            </c:if>
        </div>
    </div>

    <%-- Flash Messages --%>
    <c:if test="${not empty msg}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="bi bi-check-circle me-1"></i>${msg}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty err}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle me-1"></i>${err}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <%-- Status Filter Pills --%>
    <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <span class="text-muted me-1" style="font-size:0.85rem;">Status:</span>
        <button type="button" class="filter-pill ${isAll ? 'active' : ''}" data-status="ALL" onclick="toggleFilter(this)">All</button>
        <button type="button" class="filter-pill ${fn:contains(selectedStatuses.toString(), 'SUBMITTED') && !isAll ? 'active' : ''}" data-status="SUBMITTED" onclick="toggleFilter(this)">Submitted</button>
        <button type="button" class="filter-pill ${fn:contains(selectedStatuses.toString(), 'UNDER_REVIEW') && !isAll ? 'active' : ''}" data-status="UNDER_REVIEW" onclick="toggleFilter(this)">Under Review</button>
        <button type="button" class="filter-pill ${fn:contains(selectedStatuses.toString(), 'MORE_INFO') && !isAll ? 'active' : ''}" data-status="MORE_INFO" onclick="toggleFilter(this)">More Info</button>
        <button type="button" class="filter-pill ${fn:contains(selectedStatuses.toString(), 'APPROVED') && !isAll ? 'active' : ''}" data-status="APPROVED" onclick="toggleFilter(this)">Approved</button>
        <button type="button" class="filter-pill ${fn:contains(selectedStatuses.toString(), 'DENIED') && !isAll ? 'active' : ''}" data-status="DENIED" onclick="toggleFilter(this)">Denied</button>
    </div>

    <%-- Applications Table --%>
    <div class="card">
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty applications}">
                    <table class="table table-hover table-sm mb-0 align-middle">
                        <thead class="table-light">
                        <tr>
                            <th class="ps-3">Prospect</th>
                            <th>Lines of Service</th>
                            <th>Submitted</th>
                            <th>Status</th>
                            <th class="text-end pe-3">Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="app" items="${applications}">
                            <tr>
                                <td class="ps-3">
                                    <div class="fw-semibold">${app.getProposal().getProspect().getName()}</div>
                                    <small class="text-muted">
                                            ${app.getProposal().getProspect().getContact().getFirstName()}
                                            ${app.getProposal().getProspect().getContact().getLastName()}
                                        <c:if test="${app.getProposal().getProspect().getContact().getEmail() != null}">
                                            &middot; ${app.getProposal().getProspect().getContact().getEmail()}
                                        </c:if>
                                    </small>
                                </td>
                                <td>
                                    <c:forEach var="los" items="${app.getProposal().getLosList()}">
                                        <span class="badge bg-primary los-badge me-1">${los.getShortText()}</span>
                                    </c:forEach>
                                </td>
                                <td>
                                    <c:if test="${app.getDateSubmitted() != null}">
                                        <fmt:formatDate value="${app.getDateSubmitted()}" pattern="MM/dd/yyyy"/>
                                        <br><small class="text-muted"><fmt:formatDate value="${app.getDateSubmitted()}" pattern="h:mm a"/></small>
                                    </c:if>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${app.getStatus() == 'SUBMITTED'}">
                                            <span class="badge bg-warning text-dark status-badge">Submitted</span>
                                        </c:when>
                                        <c:when test="${app.getStatus() == 'UNDER_REVIEW'}">
                                            <span class="badge bg-info status-badge">Under Review</span>
                                        </c:when>
                                        <c:when test="${app.getStatus() == 'MORE_INFO'}">
                                            <span class="badge bg-secondary status-badge">More Info Needed</span>
                                        </c:when>
                                        <c:when test="${app.getStatus() == 'APPROVED'}">
                                            <span class="badge bg-success status-badge">Approved</span>
                                        </c:when>
                                        <c:when test="${app.getStatus() == 'DENIED'}">
                                            <span class="badge bg-danger status-badge">Denied</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-light text-dark status-badge">${app.getStatus()}</span>
                                        </c:otherwise>
                                    </c:choose>
                                    <c:if test="${app.getSetup() != null}">
                                        <br><small class="text-muted"><i class="bi bi-link-45deg"></i> Setup #${app.getSetup().getId()}</small>
                                    </c:if>
                                </td>
                                <td class="text-end pe-3">
                                    <a href="ReviewApplication?id=${app.getProposal().getId()}"
                                       class="btn btn-sm btn-outline-dark">
                                        <i class="bi bi-eye me-1"></i>Review
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <div class="text-center text-muted py-5">
                        <i class="bi bi-inbox" style="font-size: 2rem;"></i>
                        <p class="mt-2 mb-0">No applications found for the selected filter</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Summary Footer --%>
    <c:if test="${not empty applications}">
        <div class="text-muted small mt-2 text-end">
            Showing ${fn:length(applications)} application<c:if test="${fn:length(applications) != 1}">s</c:if>
        </div>
    </c:if>

    </div><%-- /.ra-wrap --%>
</div>

<script>
    // Track active statuses from server
    var activeStatuses = new Set();
    <c:forEach var="s" items="${selectedStatuses}">
    activeStatuses.add('${s}');
    </c:forEach>
    var isAll = ${isAll};

    function toggleFilter(btn) {
        var status = btn.dataset.status;

        if (status === 'ALL') {
            // ALL clicked — select everything
            window.location.href = 'ReviewApplications?status=ALL';
            return;
        }

        // Toggle this status
        if (isAll) {
            // Was showing all — now narrow to just this one
            activeStatuses.clear();
            activeStatuses.add(status);
            isAll = false;
        } else if (activeStatuses.has(status)) {
            activeStatuses.delete(status);
            // If nothing left, default to SUBMITTED
            if (activeStatuses.size === 0) {
                activeStatuses.add('SUBMITTED');
            }
        } else {
            activeStatuses.add(status);
        }

        // Check if all 5 are selected — treat as ALL
        if (activeStatuses.size === 5) {
            window.location.href = 'ReviewApplications?status=ALL';
            return;
        }

        // Navigate with comma-separated statuses
        window.location.href = 'ReviewApplications?status=' + Array.from(activeStatuses).join(',');
    }
</script>
</body>
</html>

