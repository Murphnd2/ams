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
        @media (min-width: 992px) {
            .proposal-content {
                width: fit-content;
                min-width: 700px;
                max-width: 100%;
                margin: 0 auto;
            }
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Proposal #${proposal.getId()}" scope="request"/>
    <c:set var="pageIcon" value="bi-file-earmark-text" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
    <div class="proposal-content mt-2">

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
                <input type="text" class="form-control guid-link" id="guidLink" size="70"
                       value="${proposalLink}" readonly>
                <button class="btn btn-outline-secondary" type="button" onclick="copyLink()">
                    <i class="bi bi-clipboard me-1"></i>Copy
                </button>
                <a class="btn btn-outline-secondary" href="${proposalLink}" target="_blank" rel="noopener">
                    <i class="bi bi-box-arrow-up-right me-1"></i>View
                </a>
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
        <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
            <h5 class="mb-0 fw-semibold">Pricing Summary</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <%-- Editable breakdown: every line is shown, including $0.00 "Included" lines, so a
                     markup can be added to any of them. Gated the same way the save action is gated
                     server-side (ProposalDetail.doPost, saveMarkup): agent, agency admin, or PSP admin. --%>
                <c:when test="${canEditMarkup}">
                    <form method="post" action="ProposalDetail">
                        <input type="hidden" name="id" value="${proposal.getId()}">
                        <input type="hidden" name="action" value="saveMarkup">
                        <c:set var="currentModule" value=""/>
                        <table class="table table-sm mb-0">
                            <thead>
                                <tr class="pricing-header">
                                    <th class="ps-5">Item</th>
                                    <th class="text-end">Standard</th>
                                    <th class="text-end text-nowrap" style="width:130px;">Markup<i class="bi bi-question-circle text-muted ms-1" style="cursor:help;" data-bs-toggle="tooltip" data-bs-placement="top" title="Agent markup is a flat dollar amount added to the standard price. Markup cannot be negative."></i></th>
                                    <th class="text-end pe-3">Sell</th>
                                </tr>
                            </thead>
                            <c:forEach var="line" items="${pricing}">
                                <c:if test="${line.getModule().getId() != currentModule}">
                                    <c:set var="currentModule" value="${line.getModule().getId()}"/>
                                    <tr class="pricing-header">
                                        <td colspan="4" class="fw-semibold py-2 px-3">
                                            <c:choose>
                                                <c:when test="${not empty line.getModule().getLos()}">${line.getModule().getLos().getDescription()}</c:when>
                                                <c:when test="${not empty line.getModule().getEnhancement()}">${line.getModule().getEnhancement().getDescription()}</c:when>
                                                <c:otherwise>${line.getModule().getDescription()}</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:if>
                                <tr>
                                    <td class="ps-5">${line.getPriceItem().getDescription()}</td>
                                    <td class="text-end">
                                        <c:choose>
                                            <c:when test="${line.getBasePrice() < 0.02}">Included</c:when>
                                            <c:otherwise><fmt:formatNumber value="${line.getBasePrice()}" type="currency"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end">
                                        <input type="hidden" name="moduleId" value="${line.getModule().getId()}">
                                        <input type="hidden" name="priceItemId" value="${line.getPriceItem().getId()}">
                                        <input type="number" class="form-control form-control-sm text-end d-inline-block"
                                               name="markup" min="0" step="0.01" style="max-width:110px;"
                                               value="<fmt:formatNumber value='${line.getMarkup()}' pattern='0.00'/>">
                                    </td>
                                    <td class="text-end pe-3 fw-semibold">
                                        <c:choose>
                                            <c:when test="${line.getSellPrice() < 0.02}">Included</c:when>
                                            <c:otherwise><fmt:formatNumber value="${line.getSellPrice()}" type="currency"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty pricing}">
                                <tr><td class="text-muted p-3" colspan="4">No pricing available for this rate/LOS combination</td></tr>
                            </c:if>
                        </table>
                        <c:if test="${not empty pricing}">
                            <div class="p-3">
                                <button type="submit" class="btn btn-outline-primary btn-sm">
                                    <i class="bi bi-check2 me-1"></i>Save Markup
                                </button>
                            </div>
                        </c:if>
                    </form>
                </c:when>
                <%-- Read-only breakdown for viewers who can't edit markup — $0.00 sell rows are hidden,
                     same visibility rule as the public proposalPricing.jsp. --%>
                <c:otherwise>
                    <c:set var="currentModule" value=""/>
                    <c:set var="hasVisibleRows" value="false"/>
                    <table class="table table-sm mb-0">
                        <thead>
                            <tr class="pricing-header">
                                <th class="ps-5">Item</th>
                                <th class="text-end">Standard</th>
                                <th class="text-end text-nowrap">Markup</th>
                                <th class="text-end pe-3">Sell</th>
                            </tr>
                        </thead>
                        <c:forEach var="line" items="${pricing}">
                            <c:if test="${line.getSellPrice() > 0.001}">
                                <c:if test="${line.getModule().getId() != currentModule}">
                                    <c:set var="currentModule" value="${line.getModule().getId()}"/>
                                    <tr class="pricing-header">
                                        <td colspan="4" class="fw-semibold py-2 px-3">
                                            <c:choose>
                                                <c:when test="${not empty line.getModule().getLos()}">${line.getModule().getLos().getDescription()}</c:when>
                                                <c:when test="${not empty line.getModule().getEnhancement()}">${line.getModule().getEnhancement().getDescription()}</c:when>
                                                <c:otherwise>${line.getModule().getDescription()}</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:if>
                                <tr>
                                    <td class="ps-5">${line.getPriceItem().getDescription()}</td>
                                    <td class="text-end">
                                        <c:choose>
                                            <c:when test="${line.getBasePrice() < 0.02}">Included</c:when>
                                            <c:otherwise><fmt:formatNumber value="${line.getBasePrice()}" type="currency"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end">
                                        <c:choose>
                                            <c:when test="${line.getMarkup() < 0.02}">&mdash;</c:when>
                                            <c:otherwise><fmt:formatNumber value="${line.getMarkup()}" type="currency"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end pe-3 fw-semibold">
                                        <c:choose>
                                            <c:when test="${line.getSellPrice() < 0.02}">Included</c:when>
                                            <c:otherwise><fmt:formatNumber value="${line.getSellPrice()}" type="currency"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <c:set var="hasVisibleRows" value="true"/>
                            </c:if>
                        </c:forEach>
                        <c:if test="${empty pricing || hasVisibleRows == 'false'}">
                            <tr><td class="text-muted p-3" colspan="4">No pricing available for this rate/LOS combination</td></tr>
                        </c:if>
                    </table>
                </c:otherwise>
            </c:choose>
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

    </div><%-- /proposal-content --%>
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

    document.addEventListener('DOMContentLoaded', function() {
        var tips = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tips.forEach(function(el) { new bootstrap.Tooltip(el); });
    });
</script>
</body>
</html>
