<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="opp" value="${sessionScope.local.getCurrentActivity().getActivity()}"/>
<c:set var="opportunityProposals" value="${sessionScope.local.getCurrentActivity().getOpportunityProposals()}"/>
<c:set var="allProspectProposals" value="${sessionScope.local.getCurrentActivity().getAllProspectProposals()}"/>
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
                  <span>${fn:escapeXml(opp.getProspect().getName())}</span>
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
                          data-previous-value="${opp.getManagedBy() != null ? opp.getManagedBy().getId() : 0}"
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

<%-- Proposals Card with Toggle --%>
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: #0d6efd !important;">
    <div class="card-body py-2 px-3">
        <div class="d-flex align-items-center justify-content-between mb-1">
            <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
                <i class="bi bi-file-earmark-text me-1"></i>Proposals
            </span>
            <div class="d-flex align-items-center">
                <div class="btn-group btn-group-sm me-1" role="group">
                    <button type="button" class="btn btn-outline-secondary active py-0 px-2"
                            id="btnThisOpp" onclick="toggleProposalView('opp')"
                            style="font-size: 0.7rem; line-height: 1.4;">This Opp</button>
                    <button type="button" class="btn btn-outline-secondary py-0 px-2"
                            id="btnAllProposals" onclick="toggleProposalView('all')"
                            style="font-size: 0.7rem; line-height: 1.4;">All</button>
                </div>
                <c:if test="${opp.getProspect() != null && !opp.isComplete()}">
                    <a href="ProposalBuilder?prospectId=${opp.getProspect().getId()}&sourceActivityId=${opp.getId()}"
                       class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 me-1" title="Create Proposal">
                        <i class="bi bi-file-earmark-plus" style="font-size: 0.85rem;"></i>
                    </a>
                </c:if>
                <c:set var="oppHasApp" value="false"/>
                <c:if test="${not empty opportunityProposals}">
                    <c:forEach var="xp" items="${opportunityProposals}">
                        <c:if test="${xp.getApplication() != null}"><c:set var="oppHasApp" value="true"/></c:if>
                    </c:forEach>
                </c:if>
                <c:if test="${oppHasApp}">
                    <a href="ExportApplicationCsv?opportunityId=${opp.getId()}"
                       class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 me-1" title="Export Application CSV">
                        <i class="bi bi-filetype-csv" style="font-size: 0.85rem;"></i>
                    </a>
                </c:if>
                <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none" id="btnExpandProposals"
                        type="button" data-bs-toggle="modal" data-bs-target="#proposalsFullModal" title="View all proposals">
                    <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
                </button>
            </div>
        </div>

        <%-- Proposals linked to THIS opportunity --%>
        <div id="proposalsOpp" class="overflow-auto" style="max-height: 120px;">
                <c:choose>
                    <c:when test="${not empty opportunityProposals}">
                        <c:forEach var="prop" items="${opportunityProposals}">
                            <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                                <a href="ProposalDetail?id=${prop.getId()}" class="text-decoration-none me-2 fw-semibold" style="color: var(--ssa);">#${prop.getId()}</a>
                                <c:choose>
                                    <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary me-1">Created</span></c:when>
                                    <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info me-1">Sent</span></c:when>
                                    <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark me-1">Viewed</span></c:when>
                                    <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary me-1">Applied</span></c:when>
                                    <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success me-1">Approved</span></c:when>
                                    <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger me-1">Denied</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary me-1">${prop.getStatus()}</span></c:otherwise>
                                </c:choose>
                                <c:if test="${prop.getApplication() != null}">
                                    <span class="badge bg-light text-primary border me-1" style="font-size: 0.65rem;"
                                          title="Application: ${prop.getApplication().getStatus()}"><i class="bi bi-file-earmark-check"></i> App</span>
                                </c:if>
                                <c:if test="${prop.getApplication() != null && prop.getApplication().getSetup() != null}">
                                    <span class="badge bg-light text-success border me-1" style="font-size: 0.65rem;"
                                          title="Setup created"><i class="bi bi-gear-fill"></i> Setup</span>
                                </c:if>
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
                    </c:when>
                    <c:otherwise>
                        <p class="text-muted mb-0 py-1" style="font-size: 0.8rem;">
                            <i class="bi bi-info-circle me-1"></i>No proposals linked to this opportunity yet.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- ALL proposals for this prospect --%>
            <div id="proposalsAll" class="overflow-auto" style="max-height: 120px; display: none;">
                <c:choose>
                    <c:when test="${not empty allProspectProposals}">
                        <c:forEach var="prop" items="${allProspectProposals}">
                            <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                                <a href="ProposalDetail?id=${prop.getId()}" class="text-decoration-none me-2 fw-semibold" style="color: var(--ssa);">#${prop.getId()}</a>
                                <c:choose>
                                    <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary me-1">Created</span></c:when>
                                    <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info me-1">Sent</span></c:when>
                                    <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark me-1">Viewed</span></c:when>
                                    <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary me-1">Applied</span></c:when>
                                    <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success me-1">Approved</span></c:when>
                                    <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger me-1">Denied</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary me-1">${prop.getStatus()}</span></c:otherwise>
                                </c:choose>
                                <c:if test="${prop.getApplication() != null}">
                                    <span class="badge bg-light text-primary border me-1" style="font-size: 0.65rem;"
                                          title="Application: ${prop.getApplication().getStatus()}"><i class="bi bi-file-earmark-check"></i> App</span>
                                </c:if>
                                <c:if test="${prop.getApplication() != null && prop.getApplication().getSetup() != null}">
                                    <span class="badge bg-light text-success border me-1" style="font-size: 0.65rem;"
                                          title="Setup created"><i class="bi bi-gear-fill"></i> Setup</span>
                                </c:if>
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
                    </c:when>
                    <c:otherwise>
                        <p class="text-muted mb-0 py-1" style="font-size: 0.8rem;">
                            <i class="bi bi-info-circle me-1"></i>No proposals for this prospect.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
    </div>
</div>

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
                <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Est. Employees</span><span><c:choose><c:when test="${opp.getEstimatedEmployees() != null}">${opp.getEstimatedEmployees()}</c:when><c:otherwise>&mdash;</c:otherwise></c:choose></span></div>
                <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Est. Value</span><span><c:choose><c:when test="${opp.getEstimatedValue() != null}">$<fmt:formatNumber value="${opp.getEstimatedValue()}" pattern="#,##0"/></c:when><c:otherwise>&mdash;</c:otherwise></c:choose></span></div>
                <div class="d-flex py-1 border-bottom"><span class="text-muted fw-semibold" style="min-width:100px;">Expected Close</span><span><c:choose><c:when test="${opp.getExpectedCloseDate() != null}"><fmt:formatDate value="${opp.getExpectedCloseDate()}" pattern="M/d/yyyy"/></c:when><c:otherwise>&mdash;</c:otherwise></c:choose></span></div>
            </div>
        </div>
    </div>
</div>

<%-- Full proposals modal (shows all prospect proposals) --%>
<c:if test="${not empty allProspectProposals}">
    <div class="modal fade" id="proposalsFullModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
            <div class="modal-content">
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-file-earmark-text me-2"></i>All Proposals for ${fn:escapeXml(opp.getProspect().getName())}</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <c:forEach var="prop" items="${allProspectProposals}">
                        <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                            <a href="ProposalDetail?id=${prop.getId()}" class="text-decoration-none me-2 fw-semibold" style="color: var(--ssa);">#${prop.getId()}</a>
                            <c:choose>
                                <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary me-1">Created</span></c:when>
                                <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info me-1">Sent</span></c:when>
                                <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark me-1">Viewed</span></c:when>
                                <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary me-1">Applied</span></c:when>
                                <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success me-1">Approved</span></c:when>
                                <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger me-1">Denied</span></c:when>
                                <c:otherwise><span class="badge bg-secondary me-1">${prop.getStatus()}</span></c:otherwise>
                            </c:choose>
                            <c:if test="${prop.getApplication() != null}">
                                <span class="badge bg-light text-primary border me-1" style="font-size: 0.65rem;"
                                      title="Application: ${prop.getApplication().getStatus()}"><i class="bi bi-file-earmark-check"></i> App</span>
                            </c:if>
                            <c:if test="${prop.getApplication() != null && prop.getApplication().getSetup() != null}">
                                <span class="badge bg-light text-success border me-1" style="font-size: 0.65rem;"
                                      title="Setup created"><i class="bi bi-gear-fill"></i> Setup</span>
                            </c:if>
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
        var el2 = document.getElementById('proposalsOpp');
        var el3 = document.getElementById('proposalsAll');
        if ((el2 && el2.scrollHeight > el2.clientHeight) || (el3 && el3.scrollHeight > el3.clientHeight)) {
            var btn = document.getElementById('btnExpandProposals');
            if (btn) btn.classList.remove('d-none');
        }
    });

    /* ═══ Proposal view toggle ═══ */
    function toggleProposalView(mode) {
        var oppDiv = document.getElementById('proposalsOpp');
        var allDiv = document.getElementById('proposalsAll');
        var btnOpp = document.getElementById('btnThisOpp');
        var btnAll = document.getElementById('btnAllProposals');
        if (mode === 'all') {
            oppDiv.style.display = 'none';
            allDiv.style.display = '';
            btnOpp.classList.remove('active');
            btnAll.classList.add('active');
        } else {
            oppDiv.style.display = '';
            allDiv.style.display = 'none';
            btnOpp.classList.add('active');
            btnAll.classList.remove('active');
        }
    }

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

        // Warn if removing managed-by on an outside-agency opportunity
        if (managedById === '0' || managedById === '') {
            var assigneeId = '${opp.getAssignedTo() != null ? opp.getAssignedTo().getId() : 0}';
            var currentUserId = '${sessionScope.local.getCurrentPerson().getId()}';
            if (assigneeId !== currentUserId) {
                var ok = confirm(
                    'Warning: Removing Managed By will remove this opportunity from your activity list.\n\n' +
                    'This opportunity is assigned to an outside agent. Without a Managed By value, ' +
                    'it will only be visible in the agent\'s pipeline.\n\n' +
                    'Are you sure you want to proceed?'
                );
                if (!ok) {
                    // Revert to previous value
                    sel.value = sel.dataset.previousValue || '${opp.getManagedBy() != null ? opp.getManagedBy().getId() : 0}';
                    return;
                }
            }
        }

        // Track current value for potential revert
        sel.dataset.previousValue = managedById;

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