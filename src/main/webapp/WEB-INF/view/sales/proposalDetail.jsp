<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Proposal Detail</title>
    <style>
        .status-badge { font-size: 0.85rem; }
        .guid-link { font-family: monospace; font-size: 0.9rem; }
        .pricing-header { background-color: #f8f9fa; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Proposal Detail" scope="request"/>
    <c:set var="pageIcon" value="bi-file-earmark-text" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
    <div class="row align-items-center py-2">
        <div class="col">
            <h5 class="mb-0 d-inline">Proposal #${proposal.getId()}</h5>
            <span class="badge bg-secondary status-badge ms-2">${proposal.getStatus()}</span>
        </div>
    </div>

    <%-- Header --%>
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h4 class="mb-1">Proposal #${proposal.getId()}</h4>
            <span class="badge bg-secondary status-badge">${proposal.getStatus()}</span>
        </div>
        <div>
            <a href="ProposalBuilder" class="btn btn-outline-secondary btn-sm me-2">
                <i class="bi bi-plus-lg me-1"></i>New Proposal
            </a>
            <c:choose>
                <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
                    <a href="AgentHome" class="btn btn-outline-primary btn-sm">
                        <i class="bi bi-kanban me-1"></i>Pipeline
                    </a>
                </c:when>
                <c:otherwise>
                    <a href="ViewHome25" class="btn btn-outline-primary btn-sm">
                        <i class="bi bi-house me-1"></i>Home
                    </a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Prospect & Rate Info --%>
    <div class="card mb-3">
        <div class="card-body">
            <div class="row">
                <div class="col-md-6">
                    <h6 class="text-muted mb-1">Prospect</h6>
                    <div class="fw-semibold">${proposal.getProspect().getName()}</div>
                    <c:if test="${proposal.getProspect().getContact() != null}">
                        <small class="text-muted">
                                ${proposal.getProspect().getContact().getFirstName()} ${proposal.getProspect().getContact().getLastName()}
                            <c:if test="${proposal.getProspect().getContact().getEmail() != null}">
                                &middot; ${proposal.getProspect().getContact().getEmail()}
                            </c:if>
                        </small>
                    </c:if>
                </div>
                <div class="col-md-3">
                    <h6 class="text-muted mb-1">Rate Package</h6>
                    <div class="fw-semibold">${proposal.getRate().getDescription()}</div>
                </div>
                <div class="col-md-3">
                    <h6 class="text-muted mb-1">Created</h6>
                    <div class="fw-semibold">
                        <fmt:formatDate value="${proposal.getDateCreated()}" pattern="MM/dd/yyyy"/>
                    </div>
                    <small class="text-muted">by ${proposal.getCreatedBy().getFirstName()} ${proposal.getCreatedBy().getLastName()}</small>
                </div>
            </div>
        </div>
    </div>

    <%-- GUID Link --%>
    <div class="card mb-3">
        <div class="card-body">
            <h6 class="text-muted mb-1">Proposal Link</h6>
            <div class="input-group">
                <input type="text" class="form-control guid-link" id="guidLink"
                       value="https://superiorstate.biz/proposal/${proposal.getApplicationGUID()}" readonly>
                <button class="btn btn-outline-secondary" type="button" onclick="copyLink()">
                    <i class="bi bi-clipboard me-1"></i>Copy
                </button>
            </div>
            <small class="text-muted mt-1 d-block">Share this link with the prospect to view the proposal.</small>
        </div>
    </div>

    <%-- Lines of Service --%>
    <div class="card mb-3">
        <div class="card-header bg-white py-3">
            <h5 class="mb-0 fw-semibold">Lines of Service</h5>
        </div>
        <div class="card-body">
            <c:forEach var="los" items="${proposal.getLosList()}">
                <span class="badge bg-primary me-1 mb-1" style="font-size: 0.85rem;">${los.getDescription()}</span>
            </c:forEach>
            <c:if test="${empty proposal.getLosList()}">
                <span class="text-muted">No lines of service selected</span>
            </c:if>
        </div>
    </div>

    <%-- Pricing --%>
    <div class="card mb-3">
        <div class="card-header bg-white py-3">
            <h5 class="mb-0 fw-semibold">Pricing Summary</h5>
        </div>
        <div class="card-body p-0">
            <c:set var="currentModule" value=""/>
            <table class="table table-sm mb-0">
                <c:forEach var="rt" items="${pricing}">
                    <c:if test="${rt.getModule().getId() != currentModule}">
                        <c:set var="currentModule" value="${rt.getModule().getId()}"/>
                        <tr class="pricing-header">
                            <td colspan="2" class="fw-semibold py-2 px-3">
                                <c:choose>
                                    <c:when test="${not empty rt.getModule().getLos()}">${rt.getModule().getLos().getDescription()}</c:when>
                                    <c:when test="${not empty rt.getModule().getEnhancement()}">${rt.getModule().getEnhancement().getDescription()}</c:when>
                                    <c:otherwise>${rt.getModule().getDescription()}</c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:if>
                    <tr>
                        <td class="ps-5">${rt.getPriceItem().getDescription()}</td>
                        <td class="text-end pe-3">
                            <fmt:formatNumber value="${rt.getPrice()}" type="currency"/>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty pricing}">
                    <tr><td class="text-muted p-3">No pricing available for this rate/LOS combination</td></tr>
                </c:if>
            </table>
        </div>
    </div>

        <%-- Actions --%>
        <div class="card mb-5">
            <div class="card-body d-flex flex-wrap gap-2">
                <a href="SendProposal?id=${proposal.getId()}" class="btn btn-primary">
                    <i class="bi bi-send me-1"></i>${proposal.getStatus() == 'SENT' ? 'Send Again' : 'Send Proposal'}
                </a>
                <c:if test="${proposal.getApplication() != null && (proposal.getApplication().getStatus() == 'SUBMITTED' || proposal.getApplication().getStatus() == 'UNDER_REVIEW' || proposal.getApplication().getStatus() == 'MORE_INFO')}">
                    <a href="ReviewApplication?id=${proposal.getId()}" class="btn btn-warning">
                        <i class="bi bi-clipboard-check me-1"></i>Review Application
                    </a>
                </c:if>
                <c:if test="${proposal.getApplication() != null && proposal.getApplication().getStatus() == 'APPROVED'}">
                    <a href="ReviewApplication?id=${proposal.getId()}" class="btn btn-success">
                        <i class="bi bi-check-circle me-1"></i>View Approved Application
                    </a>
                </c:if>
                <c:if test="${proposal.getApplication() != null && proposal.getApplication().getStatus() == 'DENIED'}">
                    <a href="ReviewApplication?id=${proposal.getId()}" class="btn btn-outline-danger">
                        <i class="bi bi-x-circle me-1"></i>View Denied Application
                    </a>
                </c:if>
                <a href="ProposalBuilder" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i>Back to Builder
                </a>
                <c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
                    <a href="ReviewApplications" class="btn btn-outline-dark">
                        <i class="bi bi-list-check me-1"></i>All Applications
                    </a>
                </c:if>
                <c:choose>
                    <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
                        <a href="AgentHome" class="btn btn-outline-primary">
                            <i class="bi bi-kanban me-1"></i>Pipeline
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a href="ViewHome25" class="btn btn-outline-primary">
                            <i class="bi bi-house me-1"></i>Home
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

</div>

<script>
    function copyLink() {
        var linkInput = document.getElementById('guidLink');
        linkInput.select();
        document.execCommand('copy');
        var btn = linkInput.nextElementSibling;
        btn.innerHTML = '<i class="bi bi-check me-1"></i>Copied';
        setTimeout(function() {
            btn.innerHTML = '<i class="bi bi-clipboard me-1"></i>Copy';
        }, 2000);
    }
</script>
</body>
</html>
