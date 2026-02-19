<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Proposal Detail</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        .status-badge { font-size: 0.85rem; }
        .guid-link { font-family: monospace; font-size: 0.9rem; }
        .pricing-header { background-color: #f8f9fa; }
    </style>
</head>
<body class="bg-light">
<div class="container py-4" style="max-width: 900px;">

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
                    <c:if test="${rt.getModule().getDescription() != currentModule}">
                        <c:set var="currentModule" value="${rt.getModule().getDescription()}"/>
                        <tr class="pricing-header">
                            <td colspan="2" class="fw-semibold py-2 px-3">${currentModule}</td>
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
        <div class="card-body d-flex gap-2">
            <button class="btn btn-primary" disabled>
                <i class="bi bi-send me-1"></i>Send to Prospect
            </button>
            <a href="ProposalBuilder" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i>Back to Builder
            </a>
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
