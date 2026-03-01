<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="opp" value="${sessionScope.local.getCurrentActivity().getActivity()}"/>
<c:set var="canEditStage" value="${sessionScope.isPspAdmin
    || sessionScope.isAgent || sessionScope.isAgencyAdmin
    || (opp.getAssignedTo() != null && opp.getAssignedTo().getId() == sessionScope.local.getCurrentPerson().getId())}"/>

<%-- Key Info Card --%>
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: #0d6efd !important;">
    <div class="card-body py-2 px-3">
        <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
        <i class="bi bi-bullseye me-1"></i>Opportunity Details
      </span>
            <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none" id="btnExpandOpp"
                    type="button" data-bs-toggle="modal" data-bs-target="#oppFullModal" title="View full details">
                <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
            </button>
        </div>
        <div id="oppContent" class="overflow-auto" style="max-height: 160px; font-size: 0.85rem;">
            <%-- Row A: Prospect | Stage --%>
            <div class="row g-0 py-1 border-bottom">
              <div class="col-12 col-md-6 d-flex align-items-center">
                <span class="text-muted fw-semibold" style="min-width:80px;">Prospect</span>
                <c:if test="${opp.getProspect() != null}">
                  <span class="flex-grow-1">${fn:escapeXml(opp.getProspect().getName())}</span>
                  <c:if test="${!opp.isComplete()}">
                    <a href="ProposalBuilder?prospectId=${opp.getProspect().getId()}"
                       class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ms-1" title="Create Proposal">
                      <i class="bi bi-file-earmark-plus" style="font-size:0.85rem;"></i>
                    </a>
                  </c:if>
                </c:if>
              </div>
              <div class="col-12 col-md-6 d-flex align-items-center">
                <span class="text-muted fw-semibold" style="min-width:80px;">Stage</span>
                <c:choose>
                  <c:when test="${canEditStage}">
                    <select class="form-select form-select-sm py-0" id="oppStageSelect"
                            style="font-size:0.8rem; width:auto; min-width:120px; height:26px;"
                            onchange="updateOppStage(this.value)">
                      <option value="NEW"           ${opp.getStage()=='NEW'?'selected':''}>New</option>
                      <option value="CONTACTED"      ${opp.getStage()=='CONTACTED'?'selected':''}>Contacted</option>
                      <option value="QUALIFIED"      ${opp.getStage()=='QUALIFIED'?'selected':''}>Qualified</option>
                      <option value="PROPOSAL_SENT"  ${opp.getStage()=='PROPOSAL_SENT'?'selected':''}>Proposal Sent</option>
                      <option value="NEGOTIATION"    ${opp.getStage()=='NEGOTIATION'?'selected':''}>Negotiation</option>
                      <option value="ON_HOLD"        ${opp.getStage()=='ON_HOLD'?'selected':''}>On Hold</option>
                      <option value="WON"            ${opp.getStage()=='WON'?'selected':''}>Won</option>
                      <option value="LOST"           ${opp.getStage()=='LOST'?'selected':''}>Lost</option>
                    </select>
                    <span id="oppStageSaved" class="text-success ms-2" style="font-size:0.75rem; display:none;">
                      <i class="bi bi-check-circle"></i>
                    </span>
                  </c:when>
                  <c:otherwise>
                    <c:choose>
                      <c:when test="${opp.getStage() == 'NEW'}"><span class="badge bg-primary">New</span></c:when>
                      <c:when test="${opp.getStage() == 'CONTACTED'}"><span class="badge bg-success">Contacted</span></c:when>
                      <c:when test="${opp.getStage() == 'QUALIFIED'}"><span class="badge bg-warning text-dark">Qualified</span></c:when>
                      <c:when test="${opp.getStage() == 'PROPOSAL_SENT'}"><span class="badge bg-info">Proposal Sent</span></c:when>
                      <c:when test="${opp.getStage() == 'NEGOTIATION'}"><span class="badge bg-danger">Negotiation</span></c:when>
                      <c:when test="${opp.getStage() == 'WON'}"><span class="badge bg-success">Won</span></c:when>
                      <c:when test="${opp.getStage() == 'LOST'}"><span class="badge bg-dark">Lost</span></c:when>
                      <c:when test="${opp.getStage() == 'ON_HOLD'}"><span class="badge bg-secondary">On Hold</span></c:when>
                      <c:otherwise><span class="badge bg-secondary">${opp.getStage()}</span></c:otherwise>
                    </c:choose>
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
            <%-- Row B: Agent | Agency --%>
            <div class="row g-0 py-1 border-bottom">
              <div class="col-12 col-md-6 d-flex align-items-center">
                <span class="text-muted fw-semibold" style="min-width:80px;">Agent</span>
                <c:if test="${opp.getAssignedTo() != null}">
                  <span>${opp.getAssignedTo().getFirstName()} ${opp.getAssignedTo().getLastName()}</span>
                </c:if>
              </div>
              <div class="col-12 col-md-6 d-flex align-items-center">
                <span class="text-muted fw-semibold" style="min-width:80px;">Agency</span>
                <c:if test="${opp.getAgency() != null}">
                  <span>${fn:escapeXml(opp.getAgency().getName())}</span>
                </c:if>
              </div>
            </div>
            <%-- Row C: Managed By --%>
            <div class="d-flex align-items-center py-1 border-bottom">
              <span class="text-muted fw-semibold" style="min-width:80px;">Managed By</span>
              <c:choose>
                <c:when test="${sessionScope.isPspAdmin}">
                  <select class="form-select form-select-sm py-0" id="oppManagedBySelect"
                          style="font-size:0.8rem; width:auto; min-width:130px; height:26px;"
                          onchange="updateOppManagedBy(this.value)">
                    <option value="0" ${opp.getManagedBy() == null ? 'selected' : ''}>None</option>
                    <c:forEach var="u" items="${applicationScope.global.getOpportunityManagers()}">
                      <option value="${u.getId()}" ${opp.getManagedBy() != null && opp.getManagedBy().getId() == u.getId() ? 'selected' : ''}>${u.getFirstName()} ${u.getLastName()}</option>
                    </c:forEach>
                  </select>
                  <span id="oppManagedBySaved" class="text-success ms-2" style="font-size:0.75rem; display:none;">
                    <i class="bi bi-check-circle"></i>
                  </span>
                </c:when>
                <c:otherwise>
                  <span>${opp.getManagedBy() != null ? opp.getManagedBy().getFirstName().concat(' ').concat(opp.getManagedBy().getLastName()) : 'None'}</span>
                </c:otherwise>
              </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- Proposals Card --%>
<c:if test="${not empty opportunityProposals}">
    <div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: #0d6efd !important;">
        <div class="card-body py-2 px-3">
            <div class="d-flex align-items-center justify-content-between mb-1">
        <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
          <i class="bi bi-file-earmark-text me-1"></i>Proposals for ${fn:escapeXml(opp.getProspect().getName())}
        </span>
                <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none" id="btnExpandProposals"
                        type="button" data-bs-toggle="modal" data-bs-target="#proposalsFullModal" title="View all proposals">
                    <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
                </button>
            </div>
            <div id="proposalsContent" class="overflow-auto" style="max-height: 120px;">
                <c:forEach var="prop" items="${opportunityProposals}">
                    <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                        <a href="ProposalDetail?id=${prop.getId()}" class="text-decoration-none me-2 fw-semibold" style="color: var(--ssa);">#${prop.getId()}</a>
                        <c:choose>
                            <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary me-2">Created</span></c:when>
                            <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info me-2">Sent</span></c:when>
                            <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark me-2">Viewed</span></c:when>
                            <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary me-2">Applied</span></c:when>
                            <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success me-2">Approved</span></c:when>
                            <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger me-2">Denied</span></c:when>
                            <c:otherwise><span class="badge bg-secondary me-2">${prop.getStatus()}</span></c:otherwise>
                        </c:choose>
                        <span class="flex-grow-1">
              <c:forEach var="los" items="${prop.getLosList()}">
                  <span class="badge bg-light text-dark border" style="font-size: 0.68rem;">${los.getShortText()}</span>
              </c:forEach>
            </span>
                        <c:if test="${prop.getDateCreated() != null}">
              <span class="text-muted" style="font-size: 0.75rem;">
                <fmt:formatDate value="${prop.getDateCreated()}" pattern="M/d/yy"/>
              </span>
                        </c:if>
                    </div>
                </c:forEach>
            </div>
        </div>
    </div>
</c:if>

<%-- Full details modal --%>
<div class="modal fade" id="oppFullModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
        <div class="modal-content">
            <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                <h6 class="modal-title fw-semibold"><i class="bi bi-bullseye me-2"></i>Opportunity Details</h6>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
            </div>
            <div class="modal-body" style="font-size: 0.85rem;">
                <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Stage</span>
                    <c:choose>
                        <c:when test="${opp.getStage() == 'NEW'}"><span class="badge bg-primary">New</span></c:when>
                        <c:when test="${opp.getStage() == 'CONTACTED'}"><span class="badge bg-success">Contacted</span></c:when>
                        <c:when test="${opp.getStage() == 'QUALIFIED'}"><span class="badge bg-warning text-dark">Qualified</span></c:when>
                        <c:when test="${opp.getStage() == 'PROPOSAL_SENT'}"><span class="badge bg-info">Proposal Sent</span></c:when>
                        <c:when test="${opp.getStage() == 'NEGOTIATION'}"><span class="badge bg-danger">Negotiation</span></c:when>
                        <c:when test="${opp.getStage() == 'WON'}"><span class="badge bg-success">Won</span></c:when>
                        <c:when test="${opp.getStage() == 'LOST'}"><span class="badge bg-dark">Lost</span></c:when>
                        <c:when test="${opp.getStage() == 'ON_HOLD'}"><span class="badge bg-secondary">On Hold</span></c:when>
                        <c:otherwise><span class="badge bg-secondary">${opp.getStage()}</span></c:otherwise>
                    </c:choose>
                </div>
                <c:if test="${opp.getProspect() != null}">
                    <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Prospect</span><span>${fn:escapeXml(opp.getProspect().getName())}</span></div>
                </c:if>
                <c:if test="${opp.getAgency() != null}">
                    <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Agency</span><span>${fn:escapeXml(opp.getAgency().getName())}</span></div>
                </c:if>
                <c:if test="${opp.getAssignedTo() != null}">
                    <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Agent</span><span>${opp.getAssignedTo().getFirstName()} ${opp.getAssignedTo().getLastName()}</span></div>
                </c:if>
                <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Managed By</span><span>${opp.getManagedBy() != null ? opp.getManagedBy().getFirstName().concat(' ').concat(opp.getManagedBy().getLastName()) : 'None'}</span></div>
                <c:if test="${opp.getEstimatedEmployees() != null}">
                    <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Est. Employees</span><span>${opp.getEstimatedEmployees()}</span></div>
                </c:if>
                <c:if test="${opp.getEstimatedValue() != null}">
                    <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Est. Value</span><span>$<fmt:formatNumber value="${opp.getEstimatedValue()}" pattern="#,##0"/></span></div>
                </c:if>
                <c:if test="${opp.getExpectedCloseDate() != null}">
                    <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Expected Close</span><span><fmt:formatDate value="${opp.getExpectedCloseDate()}" pattern="M/d/yyyy"/></span></div>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%-- Full proposals modal --%>
<c:if test="${not empty opportunityProposals}">
    <div class="modal fade" id="proposalsFullModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
            <div class="modal-content">
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-file-earmark-text me-2"></i>Proposals for ${fn:escapeXml(opp.getProspect().getName())}</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <c:forEach var="prop" items="${opportunityProposals}">
                        <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                            <a href="ProposalDetail?id=${prop.getId()}" class="text-decoration-none me-2 fw-semibold" style="color: var(--ssa);">#${prop.getId()}</a>
                            <c:choose>
                                <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary me-2">Created</span></c:when>
                                <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info me-2">Sent</span></c:when>
                                <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark me-2">Viewed</span></c:when>
                                <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary me-2">Applied</span></c:when>
                                <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success me-2">Approved</span></c:when>
                                <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger me-2">Denied</span></c:when>
                                <c:otherwise><span class="badge bg-secondary me-2">${prop.getStatus()}</span></c:otherwise>
                            </c:choose>
                            <span class="flex-grow-1">
                <c:forEach var="los" items="${prop.getLosList()}">
                    <span class="badge bg-light text-dark border" style="font-size: 0.68rem;">${los.getShortText()}</span>
                </c:forEach>
              </span>
                            <c:if test="${prop.getDateCreated() != null}">
                                <span class="text-muted" style="font-size: 0.75rem;"><fmt:formatDate value="${prop.getDateCreated()}" pattern="M/d/yy"/></span>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </div>
</c:if>

<script>
    document.addEventListener('DOMContentLoaded', function() {
        var el1 = document.getElementById('oppContent');
        if (el1 && el1.scrollHeight > el1.clientHeight) {
            document.getElementById('btnExpandOpp').classList.remove('d-none');
        }
        var el2 = document.getElementById('proposalsContent');
        if (el2 && el2.scrollHeight > el2.clientHeight) {
            document.getElementById('btnExpandProposals').classList.remove('d-none');
        }
    });

    /* ═══ AJAX stage update ═══ */
    function updateOppStage(newStage) {
        var sel = document.getElementById('oppStageSelect');
        var saved = document.getElementById('oppStageSaved');
        sel.disabled = true;

        var params = new URLSearchParams();
        params.append('oppId', '${opp.getId()}');
        params.append('stage', newStage);
        params.append('ajax', 'true');

        fetch('UpdateOpportunityStage', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.ok) {
                saved.style.display = 'inline';
                setTimeout(function() { saved.style.display = 'none'; }, 2000);
            }
        })
        .catch(function() {
            alert('Failed to update stage.');
        })
        .finally(function() {
            sel.disabled = false;
        });
    }

    /* ═══ AJAX managed-by update ═══ */
    function updateOppManagedBy(managedById) {
        var sel = document.getElementById('oppManagedBySelect');
        var saved = document.getElementById('oppManagedBySaved');
        sel.disabled = true;

        var params = new URLSearchParams();
        params.append('oppId', '${opp.getId()}');
        params.append('managedById', managedById);
        params.append('ajax', 'true');

        fetch('UpdateOpportunityStage', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.ok) {
                saved.style.display = 'inline';
                setTimeout(function() { saved.style.display = 'none'; }, 2000);
            }
        })
        .catch(function() {
            alert('Failed to update managed by.');
        })
        .finally(function() {
            sel.disabled = false;
        });
    }
</script>