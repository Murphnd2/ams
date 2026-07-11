<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"/>
  <title>Agency Manager</title>
  <style>
    .agency-card { cursor: pointer; transition: all 0.15s; }
    .agency-card:hover { background-color: #f0f4f8; }
    .agency-card.active { border-left: 4px solid #2B5F8A; background-color: #e8eef4; }
    .agent-row { border-bottom: 1px solid #eee; padding: 0.5rem 0; }
    .agent-row:last-child { border-bottom: none; }
    .rate-check { padding: 0.4rem 0.75rem; border-bottom: 1px solid #eee; transition: opacity 0.15s; }
    .rate-check:last-child { border-bottom: none; }
    .detail-label { color: #6c757d; font-size: 0.85rem; margin-bottom: 2px; }
    .prospect-row { cursor: pointer; transition: all 0.15s; padding: 0.4rem 0.75rem; border-bottom: 1px solid #eee; }
    .prospect-row:last-child { border-bottom: none; }
    .prospect-row:hover { background-color: #f0f4f8; }
    .prospect-row.active { border-left: 3px solid var(--ssa); background-color: #e8eef4; }
    .accordion-panel { max-height: 300px; overflow-y: auto; }
    .accordion-panel.expanded { max-height: 400px; }
    .los-tag { display: inline-block; background: #e9ecef; color: #495057; font-size: 0.7rem; padding: 1px 5px; border-radius: 3px; margin-right: 2px; }
    .rate-popover { max-width: 500px; }
    .rate-popover .popover-body table td, .rate-popover .popover-body table th { padding: 0.15rem 0.4rem; }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:set var="pageTitle" value="Agency Manager" scope="request"/>
  <c:set var="pageIcon" value="bi-people-fill" scope="request"/>
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <c:if test="${not empty param.agentError}">
    <c:set var="agentErrorMsg" value="Unable to complete that agent action."/>
    <c:if test="${param.agentError == 'nosuccessor'}">
      <c:set var="agentErrorMsg" value="Cannot remove the agency manager: no other agent in this agency has an active user account."/>
    </c:if>
    <c:if test="${param.agentError == 'needmanager'}">
      <c:set var="agentErrorMsg" value="Cannot remove the agency manager without selecting a replacement."/>
    </c:if>
    <c:if test="${param.agentError == 'badmanager'}">
      <c:set var="agentErrorMsg" value="That person cannot be made agency manager (not an active agent in this agency)."/>
    </c:if>
    <div class="alert alert-danger alert-dismissible fade show mt-2" role="alert">
      ${fn:escapeXml(agentErrorMsg)}
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
  </c:if>

  <div class="row" style="margin-top:0.5rem">
    <%-- ======================== LEFT COLUMN: Agency List ======================== --%>
    <div class="col-lg-3">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center" style="background-color: #2B5F8A; color: white;">
          <span class="fw-bold"><i class="bi bi-briefcase me-1"></i>Agencies</span>
          <div>
            <button type="button" class="btn btn-sm btn-outline-light me-1" data-bs-toggle="modal" data-bs-target="#inviteModal" title="Send Invitation">
              <i class="bi bi-envelope-plus"></i>
            </button>
            <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addAgencyModal" title="Create Agency">
              <i class="bi bi-plus-lg"></i>
            </button>
          </div>
        </div>
        <div class="card-body p-0">
          <c:choose>
            <c:when test="${not empty agencyList}">
              <c:forEach var="agency" items="${agencyList}">
                <a href="PspAgencyHome?agencyId=${agency.getId()}${showSuppressed ? '&showSuppressed=true' : ''}" class="d-block text-decoration-none ${agency.isSuppressed() ? 'text-muted' : 'text-dark'}">
                  <div class="agency-card p-2 ps-3 ${selectedAgency != null && selectedAgency.getId() == agency.getId() ? 'active' : ''} ${agency.isSuppressed() ? 'opacity-50' : ''}">
                    <div class="fw-semibold${agency.isSuppressed() ? ' fst-italic' : ''}">
                      ${agency.getName()}
                      <c:if test="${agency.isSuppressed()}">
                        <span class="badge bg-secondary ms-1" style="font-size:0.65rem">Suppressed</span>
                      </c:if>
                    </div>
                    <small class="text-muted">
                      <c:if test="${agency.getPhone() != null && !agency.getPhone().isEmpty()}">
                        <i class="bi bi-telephone me-1"></i>${agency.getPhone()}
                      </c:if>
                    </small>
                  </div>
                </a>
              </c:forEach>
            </c:when>
            <c:otherwise>
              <div class="empty-state py-4">
                <i class="bi bi-briefcase"></i>
                <p class="mb-0">No agencies created yet</p>
              </div>
            </c:otherwise>
          </c:choose>
          <div class="text-center py-2 border-top">
            <c:choose>
              <c:when test="${showSuppressed}">
                <a href="PspAgencyHome${not empty selectedAgency ? '?agencyId='.concat(selectedAgency.getId()) : ''}" class="small text-muted text-decoration-none">
                  <i class="bi bi-eye-slash me-1"></i>Hide suppressed
                </a>
              </c:when>
              <c:otherwise>
                <a href="PspAgencyHome?showSuppressed=true${not empty selectedAgency ? '&agencyId='.concat(selectedAgency.getId()) : ''}" class="small text-muted text-decoration-none">
                  <i class="bi bi-eye me-1"></i>Show suppressed
                </a>
              </c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>
    </div>

    <%-- ======================== CENTER COLUMN: Agency Detail ======================== --%>
    <div class="col-lg-5">
      <c:choose>
        <c:when test="${not empty selectedAgency}">

          <%-- Agency Info Card --%>
          <div class="card mb-3">
            <div class="card-body py-2">
              <div class="d-flex justify-content-between align-items-center">
                <div>
                  <h5 class="mb-0">
                    ${selectedAgency.getName()}
                    <c:if test="${selectedAgency.isSuppressed()}">
                      <span class="badge bg-secondary ms-1" style="font-size:0.7rem">Suppressed</span>
                    </c:if>
                  </h5>
                  <small class="text-muted">Agency ID: ${selectedAgency.getId()}</small>
                </div>
                <div>
                  <c:choose>
                    <c:when test="${selectedAgency.isSuppressed()}">
                      <form method="post" action="AgencyAction" class="d-inline">
                        <input type="hidden" name="action" value="unsuppressAgency"/>
                        <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                        <button type="submit" class="btn btn-outline-success btn-sm me-1" title="Reactivate this agency">
                          <i class="bi bi-arrow-counterclockwise me-1"></i>Reactivate
                        </button>
                      </form>
                    </c:when>
                    <c:otherwise>
                      <button type="button" class="btn btn-outline-danger btn-sm me-1" onclick="confirmSuppress()" title="Suppress this agency">
                        <i class="bi bi-x-circle me-1"></i>Suppress
                      </button>
                      <form id="suppressForm" method="post" action="AgencyAction" class="d-none">
                        <input type="hidden" name="action" value="suppressAgency"/>
                        <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                      </form>
                    </c:otherwise>
                  </c:choose>
                  <button type="button" class="btn btn-outline-primary btn-sm" data-bs-toggle="modal" data-bs-target="#editAgencyModal">
                    <i class="bi bi-pencil me-1"></i>Edit
                  </button>
                  <form method="post" action="AgencyAction" class="d-inline">
                    <input type="hidden" name="action" value="mintQuoteToken"/>
                    <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                    <button type="submit" class="btn btn-outline-secondary btn-sm">
                      <i class="bi bi-link-45deg me-1"></i>${empty selectedAgency.getQuoteToken() ? 'Generate Quote Link' : 'Regenerate Link'}
                    </button>
                  </form>
                </div>
              </div>
              <c:if test="${not empty selectedAgency.getQuoteToken()}">
                <div class="mt-2 pt-2 border-top">
                  <label class="form-label mb-1 small text-muted">Public quote-request link</label>
                  <div class="input-group input-group-sm">
                    <input type="text" class="form-control" id="quoteLinkField" readonly
                           value="https://${quoteLinkBase}/RequestQuote?k=${selectedAgency.getQuoteToken()}">
                    <button type="button" class="btn btn-outline-secondary" onclick="copyQuoteLink()">
                      <i class="bi bi-clipboard"></i>
                    </button>
                  </div>
                  <small class="text-muted">Leads from this link are attributed to ${selectedAgency.getName()}.</small>
                </div>
              </c:if>
            </div>
          </div>

          <%-- Agency Details --%>
          <div class="card mb-3">
            <div class="card-header fw-bold">
              <i class="bi bi-info-circle me-1"></i>Details
            </div>
            <div class="card-body">
              <div class="row">
                <div class="col-md-6">
                  <div class="detail-label">Phone</div>
                  <div class="mb-2">${not empty selectedAgency.getPhone() ? selectedAgency.getPhone() : '<span class="text-muted fst-italic">Not set</span>'}</div>
                </div>
                <div class="col-md-6">
                  <div class="detail-label">Tax ID</div>
                  <div class="mb-2">${not empty selectedAgency.getTaxId() ? selectedAgency.getTaxId() : '<span class="text-muted fst-italic">Not set</span>'}</div>
                </div>
              </div>
              <c:if test="${selectedAgency.getPrimaryContact() != null}">
                <div class="detail-label">Primary Contact</div>
                <div class="mb-2">
                    ${selectedAgency.getPrimaryContact().getFirstName()} ${selectedAgency.getPrimaryContact().getLastName()}
                  <c:if test="${selectedAgency.getPrimaryContact().getEmail() != null}">
                    <br><small class="text-muted">${selectedAgency.getPrimaryContact().getEmail()}</small>
                  </c:if>
                </div>
              </c:if>
              <c:if test="${selectedAgency.getAddress() != null}">
                <div class="detail-label">Address</div>
                <div>
                  <c:if test="${selectedAgency.getAddress().getAddress1() != null}">${selectedAgency.getAddress().getAddress1()}<br></c:if>
                  <c:if test="${selectedAgency.getAddress().getAddress2() != null && !selectedAgency.getAddress().getAddress2().isEmpty()}">${selectedAgency.getAddress().getAddress2()}<br></c:if>
                  <c:if test="${selectedAgency.getAddress().getCity() != null}">${selectedAgency.getAddress().getCity()}, </c:if>
                  <c:if test="${selectedAgency.getAddress().getState() != null}">${selectedAgency.getAddress().getState()} </c:if>
                  <c:if test="${selectedAgency.getAddress().getZipCode() != null}">${selectedAgency.getAddress().getZipCode()}</c:if>
                </div>
              </c:if>
            </div>
          </div>

          <%-- Agents Card --%>
          <c:set var="mgrId" value="${selectedAgency.getManager() != null ? selectedAgency.getManager().getId() : 0}"/>
          <c:set var="eligibleCount" value="0"/>
          <c:forEach var="ea" items="${agentList}">
            <c:if test="${ea.getId() != mgrId && activeAgentIds.contains(ea.getId())}">
              <c:set var="eligibleCount" value="${eligibleCount + 1}"/>
            </c:if>
          </c:forEach>

          <div class="card">
            <div class="card-header d-flex justify-content-between align-items-center fw-bold">
              <span><i class="bi bi-person-badge me-1"></i>Agents</span>
              <button type="button" class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#assignAgentModal">
                <i class="bi bi-plus-lg me-1"></i>Assign Agent
              </button>
            </div>
            <div class="card-body p-2">
              <c:choose>
                <c:when test="${not empty agentList}">
                  <c:forEach var="agent" items="${agentList}">
                    <c:set var="isMgr" value="${agent.getId() == mgrId}"/>
                    <c:set var="isActiveAgent" value="${activeAgentIds.contains(agent.getId())}"/>
                    <div class="agent-row d-flex justify-content-between align-items-center px-3">
                      <div>
                        <span class="fw-semibold">${agent.getFirstName()} ${agent.getLastName()}</span>
                        <c:if test="${isMgr}">
                          <span class="badge bg-primary ms-1" style="font-size: 0.65rem;">Manager</span>
                        </c:if>
                        <c:if test="${!isActiveAgent}">
                          <span class="badge bg-secondary ms-1" style="font-size: 0.65rem;">Inactive</span>
                        </c:if>
                        <c:if test="${agent.getEmail() != null}">
                          <br><small class="text-muted">${agent.getEmail()}</small>
                        </c:if>
                      </div>
                      <div class="d-flex align-items-center gap-1">

                        <%-- Make Manager (non-manager, active agents only) --%>
                        <c:if test="${!isMgr && isActiveAgent}">
                          <form method="post" action="AgencyAction" class="d-inline">
                            <input type="hidden" name="action" value="setManager"/>
                            <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                            <input type="hidden" name="agentId" value="${agent.getId()}"/>
                            <button type="submit" class="btn btn-sm btn-outline-primary" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                    title="Make agency manager"
                                    onclick="return confirm('Make ${fn:escapeXml(agent.getFirstName())} ${fn:escapeXml(agent.getLastName())} the agency manager?');">
                              <i class="bi bi-person-check"></i>
                            </button>
                          </form>
                        </c:if>

                        <%-- Remove --%>
                        <c:choose>
                          <%-- Manager, successors available: must reassign --%>
                          <c:when test="${isMgr && eligibleCount > 0}">
                            <button type="button" class="btn btn-sm btn-outline-danger" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                    title="Reassign manager, then remove"
                                    data-bs-toggle="modal" data-bs-target="#reassignManagerModal">
                              <i class="bi bi-x-lg"></i>
                            </button>
                          </c:when>
                          <%-- Manager and the only agent: allowed, warn --%>
                          <c:when test="${isMgr && fn:length(agentList) <= 1}">
                            <form method="post" action="AgencyAction" class="d-inline">
                              <input type="hidden" name="action" value="removeAgent"/>
                              <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                              <input type="hidden" name="agentId" value="${agent.getId()}"/>
                              <button type="submit" class="btn btn-sm btn-outline-danger" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                      onclick="return confirm('WARNING: ${fn:escapeXml(agent.getFirstName())} ${fn:escapeXml(agent.getLastName())} is the agency manager and the only agent. Removing them leaves this agency with no agents and no manager. Continue?');">
                                <i class="bi bi-x-lg"></i>
                              </button>
                            </form>
                          </c:when>
                          <%-- Manager, other agents exist but none active: blocked --%>
                          <c:when test="${isMgr}">
                            <button type="button" class="btn btn-sm btn-outline-danger" disabled
                                    style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                    title="This agent is the agency manager. No other active agent is available to take over.">
                              <i class="bi bi-x-lg"></i>
                            </button>
                          </c:when>
                          <%-- Normal agent --%>
                          <c:otherwise>
                            <form method="post" action="AgencyAction" class="d-inline">
                              <input type="hidden" name="action" value="removeAgent"/>
                              <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                              <input type="hidden" name="agentId" value="${agent.getId()}"/>
                              <button type="submit" class="btn btn-sm btn-outline-danger" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                      onclick="return confirm('Remove ${fn:escapeXml(agent.getFirstName())} ${fn:escapeXml(agent.getLastName())} from this agency?');">
                                <i class="bi bi-x-lg"></i>
                              </button>
                            </form>
                          </c:otherwise>
                        </c:choose>
                      </div>
                    </div>
                  </c:forEach>
                </c:when>
                <c:otherwise>
                  <div class="empty-state py-3">
                    <i class="bi bi-person-badge" style="font-size: 1.5rem;"></i>
                    <p class="mb-0"><small>No agents assigned</small></p>
                  </div>
                </c:otherwise>
              </c:choose>
            </div>
          </div>

        </c:when>
        <c:otherwise>
          <div class="card">
            <div class="card-body empty-state">
              <i class="bi bi-arrow-left-circle"></i>
              <p class="mb-0 fs-5">Select an agency to view details</p>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </div>

    <%-- ======================== RIGHT COLUMN: Rate Assignments + Prospects/Proposals ======================== --%>
    <div class="col-lg-4">
      <c:if test="${not empty selectedAgency}">

        <%-- Rate Assignments Card --%>
        <div class="card mb-3">
          <div class="card-header fw-bold" style="background-color: #5a8a6a; color: white;">
            <i class="bi bi-tags me-1"></i>Rate Assignments
          </div>
          <div class="card-body p-0">
            <c:choose>
              <c:when test="${not empty allRates}">
                <form method="post" action="AgencyAction" id="rateForm">
                  <input type="hidden" name="action" value="updateRates"/>
                  <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                  <c:forEach var="rate" items="${allRates}">
                    <div class="rate-check" id="rateRow_${rate.getId()}">
                      <div class="form-check">
                        <c:set var="isAssigned" value="false"/>
                        <c:forEach var="agencyRate" items="${selectedAgency.getAgencyRateList()}">
                          <c:if test="${agencyRate.getId() == rate.getId()}">
                            <c:set var="isAssigned" value="true"/>
                          </c:if>
                        </c:forEach>
                        <input class="form-check-input rate-cb" type="checkbox" name="rateIds" value="${rate.getId()}"
                               id="rate_${rate.getId()}" data-original="${isAssigned}" onchange="updateRateState()"
                               <c:if test="${isAssigned == 'true'}">checked</c:if>>
                        <label class="form-check-label" for="rate_${rate.getId()}">
                          <span class="fw-semibold rate-hover" data-rate-id="${rate.getId()}" style="cursor: help; border-bottom: 1px dotted #999;">${rate.getDescription()}</span>
                          <br><small class="text-muted">ID: ${rate.getId()}</small>
                        </label>
                      </div>
                    </div>
                  </c:forEach>
                  <div class="p-2">
                    <button type="submit" class="btn btn-sm w-100" id="saveRatesBtn" disabled
                            style="background: #ccc; color: #666; border: none; transition: all 0.2s;">
                      <i class="bi bi-check-lg me-1"></i>Save Rate Assignments
                    </button>
                  </div>
                </form>
              </c:when>
              <c:otherwise>
                <div class="text-center text-muted py-3">
                  <small>No rates available. <a href="PspAdminHome">Create one first.</a></small>
                </div>
              </c:otherwise>
            </c:choose>
          </div>
        </div>

        <%-- ======================== Prospects Accordion ======================== --%>
        <div class="card mb-3">
          <div class="card-header fw-bold d-flex justify-content-between align-items-center" style="background-color: var(--ssa); color: white; cursor: pointer;" onclick="toggleProspectPanel()">
            <span><i class="bi bi-briefcase me-1"></i>Prospects <span id="prospectCount" class="badge bg-light text-dark ms-1" style="font-size: 0.7rem;">${fn:length(prospectSummaryList)}</span></span>
            <div class="d-flex align-items-center gap-2" onclick="event.stopPropagation();">
              <select id="agentFilter" class="form-select form-select-sm" style="width: auto; font-size: 0.7rem; padding: 0.1rem 1.4rem 0.1rem 0.3rem;" onchange="filterProspects()">
                <option value="ALL">All Agents</option>
                <c:forEach var="agent" items="${agentList}">
                  <option value="${agent.getId()}">${agent.getFirstName()} ${agent.getLastName()}</option>
                </c:forEach>
              </select>
              <a href="#" class="text-white text-decoration-none" onclick="toggleProspectSort(); return false;" style="font-size: 0.7rem;">
                <i class="bi bi-arrow-down-up me-1"></i><span id="prospectSortLabel">A-Z</span>
              </a>
              <i class="bi bi-chevron-up text-white" id="prospectChevron" style="transition: transform 0.2s;"></i>
            </div>
          </div>
          <div id="prospectPanel" class="accordion-panel">
            <c:choose>
              <c:when test="${not empty prospectSummaryList}">
                <c:forEach var="ps" items="${prospectSummaryList}">
                  <div class="prospect-row" data-prospect-id="${ps.prospectId}" data-prospect-name="${fn:escapeXml(ps.prospectName)}"
                       data-agent-name="${fn:escapeXml(ps.agentName)}" data-agent-id="${ps.agentId}"
                       onclick="selectProspect(this)">
                    <div class="d-flex justify-content-between align-items-center" style="font-size: 0.8rem;">
                      <div style="min-width: 0; flex: 1;">
                        <div class="fw-semibold text-truncate" title="${fn:escapeXml(ps.prospectName)}">${fn:escapeXml(ps.prospectName)}</div>
                        <small class="text-muted text-truncate d-block" title="${fn:escapeXml(ps.agentName)}">${fn:escapeXml(ps.agentName)}</small>
                      </div>
                      <div class="ms-2 text-nowrap">
                        <c:choose>
                          <c:when test="${ps.furthestStatus == 'CREATED'}"><span class="badge bg-secondary" style="font-size: 0.65rem;">Created</span></c:when>
                          <c:when test="${ps.furthestStatus == 'SENT'}"><span class="badge bg-info" style="font-size: 0.65rem;">Sent</span></c:when>
                          <c:when test="${ps.furthestStatus == 'VIEWED'}"><span class="badge bg-warning text-dark" style="font-size: 0.65rem;">Viewed</span></c:when>
                          <c:when test="${ps.furthestStatus == 'APPLIED'}"><span class="badge bg-primary" style="font-size: 0.65rem;">Applied</span></c:when>
                          <c:when test="${ps.furthestStatus == 'APPROVED'}"><span class="badge bg-success" style="font-size: 0.65rem;">Approved</span></c:when>
                          <c:when test="${ps.furthestStatus == 'DENIED'}"><span class="badge bg-danger" style="font-size: 0.65rem;">Denied</span></c:when>
                          <c:otherwise><span class="badge bg-secondary" style="font-size: 0.65rem;">${ps.furthestStatus}</span></c:otherwise>
                        </c:choose>
                      </div>
                    </div>
                  </div>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <div class="text-center text-muted py-3" style="font-size: 0.85rem;">
                  <i class="bi bi-briefcase" style="font-size: 1.5rem; display: block; margin-bottom: 0.3rem;"></i>
                  No prospects for this agency
                </div>
              </c:otherwise>
            </c:choose>
          </div>
        </div>

        <%-- ======================== Proposals Accordion ======================== --%>
        <div class="card" id="proposalCard" style="display: none;">
          <div class="card-header fw-bold d-flex justify-content-between align-items-center" style="background-color: #5a4a8a; color: white; cursor: pointer;" onclick="toggleProposalPanel()">
            <span><i class="bi bi-file-earmark-text me-1"></i>Proposals for <span id="selectedProspectName">&#8212;</span></span>
            <div class="d-flex align-items-center gap-2" onclick="event.stopPropagation();">
              <select id="propStatusFilter" class="form-select form-select-sm" style="width: auto; font-size: 0.7rem; padding: 0.1rem 1.4rem 0.1rem 0.3rem;" onchange="filterProposals()">
                <option value="ALL">All</option>
                <option value="CREATED">Created</option>
                <option value="SENT">Sent</option>
                <option value="VIEWED">Viewed</option>
                <option value="APPLIED">Applied</option>
                <option value="APPROVED">Approved</option>
                <option value="DENIED">Denied</option>
              </select>
              <a href="#" class="text-white text-decoration-none" onclick="toggleProposalSort(); return false;" style="font-size: 0.7rem;">
                <i class="bi bi-arrow-down-up me-1"></i><span id="propSortLabel">A-Z</span>
              </a>
              <i class="bi bi-chevron-up text-white" id="proposalChevron" style="transition: transform 0.2s;"></i>
            </div>
          </div>
          <div id="proposalPanel" class="accordion-panel expanded">
            <table class="table table-sm table-hover mb-0" id="proposalTable" style="font-size: 0.78rem;">
              <thead class="table-light">
                <tr>
                  <th class="ps-3">#</th>
                  <th>Status</th>
                  <th>Services</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="prop" items="${proposalList}">
                  <tr class="proposal-row" data-prospect-id="${prop.getProspect().getId()}"
                      data-status="${prop.getStatus()}"
                      data-created="${prop.getDateCreated() != null ? prop.getDateCreated().getTime() : '0'}"
                      style="display: none;">
                    <td class="ps-3">
                      <a href="ProposalDetail?id=${prop.getId()}" target="_blank" class="text-decoration-none">${prop.getId()}</a>
                    </td>
                    <td>
                      <c:choose>
                        <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary">Created</span></c:when>
                        <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info">Sent</span></c:when>
                        <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark">Viewed</span></c:when>
                        <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary">Applied</span></c:when>
                        <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success">Approved</span></c:when>
                        <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger">Denied</span></c:when>
                        <c:otherwise><span class="badge bg-secondary">${prop.getStatus()}</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <c:forEach var="los" items="${prop.getLosList()}">
                        <span class="los-tag">${los.getShortText()}</span>
                      </c:forEach>
                    </td>
                    <td class="text-nowrap">
                      <c:if test="${prop.getDateCreated() != null}">
                        <fmt:formatDate value="${prop.getDateCreated()}" pattern="M/yy"/>
                      </c:if>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
            <div id="noProposalsMsg" class="text-center text-muted py-3" style="font-size: 0.85rem; display: none;">
              No proposals match the current filter
            </div>
          </div>
        </div>

      </c:if>
    </div>

  </div><%-- end row --%>
</div>

<%-- ======================== MODALS (always rendered) ======================== --%>

<%-- Add New Agency Modal --%>
<div class="modal fade" id="addAgencyModal" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <form method="post" action="AgencyAction">
        <input type="hidden" name="action" value="createAgency"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
          <h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Agency</h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
        </div>
        <div class="modal-body">
          <div class="row">
            <%-- Left column: Agency Info --%>
            <div class="col-md-6">
              <h6 class="text-muted mb-2"><i class="bi bi-briefcase me-1"></i>Agency Info</h6>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Agency Name</label>
                <input type="text" name="agencyName" class="form-control form-control-sm" required placeholder="e.g. Midwest Benefits Group">
              </div>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Phone</label>
                <input type="text" name="phone" class="form-control form-control-sm" placeholder="555-555-5555">
              </div>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Tax ID</label>
                <input type="text" name="taxId" class="form-control form-control-sm" placeholder="XX-XXXXXXX">
              </div>
            </div>
            <%-- Right column: Primary Contact / Agency Manager --%>
            <div class="col-md-6">
              <h6 class="text-muted mb-2"><i class="bi bi-person me-1"></i>Primary Contact / Agency Manager</h6>
              <div class="row mb-2">
                <div class="col-6">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">First Name</label>
                  <input type="text" name="contactFirst" class="form-control form-control-sm">
                </div>
                <div class="col-6">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Last Name</label>
                  <input type="text" name="contactLast" class="form-control form-control-sm">
                </div>
              </div>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Email</label>
                <input type="email" name="contactEmail" class="form-control form-control-sm">
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-center border-0">
          <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create Agency</button>
          <span class="ssa-action-sep">|</span>
          <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
        </div>
      </form>
    </div>
  </div>
</div>

<%-- Invite Agency/Agent Modal --%>
<div class="modal fade" id="inviteModal" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <form method="post" action="SendInvitation">
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
          <h6 class="modal-title fw-semibold"><i class="bi bi-envelope-plus me-2"></i>Send Invitation</h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
        </div>
        <div class="modal-body">
          <div class="row">
            <%-- Left column: Agency + Role --%>
            <div class="col-md-6">
              <h6 class="text-muted mb-2"><i class="bi bi-briefcase me-1"></i>Agency</h6>
              <div class="mb-2">
                <div class="form-check form-check-inline">
                  <input class="form-check-input" type="radio" name="inviteMode" id="modeNew" value="new" checked onchange="toggleInviteMode()">
                  <label class="form-check-label" for="modeNew">New Agency</label>
                </div>
                <div class="form-check form-check-inline">
                  <input class="form-check-input" type="radio" name="inviteMode" id="modeExisting" value="existing" onchange="toggleInviteMode()">
                  <label class="form-check-label" for="modeExisting">Existing Agency</label>
                </div>
              </div>
              <div id="newAgencyFields">
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Agency Name</label>
                  <input type="text" name="agencyName" id="invAgencyName" class="form-control form-control-sm" required>
                </div>
              </div>
              <div id="existingAgencyFields" style="display: none;">
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Select Agency</label>
                  <select name="existingAgencyId" id="invExistingAgency" class="form-select form-select-sm">
                    <option value="">-- Select --</option>
                    <c:forEach var="ag" items="${agencyList}">
                      <option value="${ag.getId()}">${ag.getName()}</option>
                    </c:forEach>
                  </select>
                </div>
              </div>
              <hr class="my-2">
              <h6 class="text-muted mb-2"><i class="bi bi-person-check me-1"></i>Role</h6>
              <div class="mb-2">
                <div class="form-check">
                  <input class="form-check-input" type="radio" name="role" id="roleManager" value="AGENCY_MANAGER" checked>
                  <label class="form-check-label" for="roleManager">
                    <strong>Agency Manager</strong>
                    <br><small class="text-muted">Can create proposals, invite agents, manage agency</small>
                  </label>
                </div>
                <div class="form-check mt-2">
                  <input class="form-check-input" type="radio" name="role" id="roleAgent" value="AGENT">
                  <label class="form-check-label" for="roleAgent">
                    <strong>Agent</strong>
                    <br><small class="text-muted">Can create proposals for assigned prospects</small>
                  </label>
                </div>
              </div>
            </div>
            <%-- Right column: Contact + Rates --%>
            <div class="col-md-6">
              <h6 class="text-muted mb-2"><i class="bi bi-person me-1"></i>Contact Info</h6>
              <div class="row mb-2">
                <div class="col-6">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">First Name</label>
                  <input type="text" name="firstName" class="form-control form-control-sm" required>
                </div>
                <div class="col-6">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Last Name</label>
                  <input type="text" name="lastName" class="form-control form-control-sm" required>
                </div>
              </div>
              <div class="mb-2">
                <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Email</label>
                <input type="email" name="email" class="form-control form-control-sm" required>
              </div>
              <hr class="my-2">
              <h6 class="text-muted mb-2"><i class="bi bi-tags me-1"></i>Pre-assign Rates</h6>
              <div style="max-height: 150px; overflow-y: auto;">
                <c:choose>
                  <c:when test="${not empty allRates}">
                    <c:forEach var="rate" items="${allRates}">
                      <div class="form-check">
                        <input class="form-check-input" type="checkbox" name="rateIds" value="${rate.getId()}" id="invRate_${rate.getId()}">
                        <label class="form-check-label" for="invRate_${rate.getId()}" style="font-size: 0.85rem;">${rate.getDescription()}</label>
                      </div>
                    </c:forEach>
                  </c:when>
                  <c:otherwise>
                    <small class="text-muted">No rates available. <a href="PspAdminHome">Create one first.</a></small>
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-center border-0">
          <button type="submit" class="ssa-action save"><i class="bi bi-send me-1"></i>Send Invitation</button>
          <span class="ssa-action-sep">|</span>
          <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
        </div>
      </form>
    </div>
  </div>
</div>

<%-- Edit Agency Modal + Assign Agent Modal (require selectedAgency) --%>
<c:if test="${not empty selectedAgency}">

  <%-- Edit Agency Modal --%>
  <div class="modal fade" id="editAgencyModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <form method="post" action="AgencyAction">
          <input type="hidden" name="action" value="editAgency"/>
          <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
          <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
            <h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Agency</h6>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
          </div>
          <div class="modal-body">
            <div class="row">
              <%-- Left column: Agency info + Primary Contact --%>
              <div class="col-md-6">
                <h6 class="text-muted mb-2"><i class="bi bi-briefcase me-1"></i>Agency Info</h6>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Agency Name</label>
                  <input type="text" name="agencyName" class="form-control form-control-sm" required value="${selectedAgency.getName()}">
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Phone</label>
                  <input type="text" name="phone" class="form-control form-control-sm" value="${selectedAgency.getPhone()}">
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Tax ID</label>
                  <input type="text" name="taxId" class="form-control form-control-sm" value="${selectedAgency.getTaxId()}">
                </div>
                <div class="mb-2 form-check">
                  <input type="checkbox" class="form-check-input" id="markupEnabledCheck" name="markupEnabled"
                         ${selectedAgency.isMarkupEnabled() ? 'checked' : ''}>
                  <label class="form-check-label" for="markupEnabledCheck" style="font-size: 0.85rem;">
                    Enable agent markup
                  </label>
                </div>
                <%-- V068: branded landing host (leave blank to disable the agency front door) --%>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Landing Host</label>
                  <input type="text" name="landingHost" class="form-control form-control-sm"
                         placeholder="e.g. swbd.superiorstate.net"
                         value="<c:out value='${selectedAgency.getLandingHost()}'/>">
                  <div class="form-text" style="font-size: 0.72rem;">Bare hostname only (no https://, no path). Leave blank to disable.</div>
                  <div id="agencyLandingHostError" class="alert alert-danger py-1 px-2 mt-1" style="display:none; font-size:0.78rem;"></div>
                </div>
                <%-- V069: white-label email sending domain (pre-fills from landing host; verified independently) --%>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Sending Domain</label>
                  <input type="text" name="emailDomain" id="agencyEmailDomain" class="form-control form-control-sm"
                         placeholder="e.g. admin.swbd.com"
                         value="<c:out value='${selectedAgency.getEmailDomain()}'/>">
                  <div class="form-text" style="font-size: 0.72rem;">
                    Bare domain only. Agents send <code>From: name@&lt;domain&gt;</code> once Verified.
                    Leave blank to send from the Superior State fallback.
                  </div>
                  <div class="form-check mt-1">
                    <input type="checkbox" class="form-check-input" id="emailVerifiedCheck" name="emailVerified"
                           ${selectedAgency.isEmailVerified() ? 'checked' : ''}>
                    <label class="form-check-label" for="emailVerifiedCheck" style="font-size: 0.85rem;">
                      Verified in SMTP2GO
                    </label>
                    <div class="form-text" style="font-size: 0.7rem; color:#b45309;">
                      Only check this after the domain shows <strong>Verified</strong> in SMTP2GO (SPF return-path + DKIM).
                      Enabling early sends unaligned mail (spam/DMARC failure).
                    </div>
                  </div>
                  <div id="agencyEmailDomainError" class="alert alert-danger py-1 px-2 mt-1" style="display:none; font-size:0.78rem;"></div>
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Parent (General Agency)</label>
                  <select name="parentAgencyId" class="form-select form-select-sm"
                          ${selectedAgencyHasChildren ? 'disabled' : ''}>
                    <option value="0">None (top-level agency)</option>
                    <c:forEach var="ga" items="${eligibleParents}">
                      <option value="${ga.getId()}"
                        ${selectedAgency.getParentAgency() != null && selectedAgency.getParentAgency().getId() == ga.getId() ? 'selected' : ''}>
                        ${ga.getName()}
                      </option>
                    </c:forEach>
                  </select>
                  <c:if test="${selectedAgencyHasChildren}">
                    <small class="text-muted">This agency is a general agency with sub-agencies, so it can't be made a sub-agency itself.</small>
                  </c:if>
                  <div id="agencyParentError" class="alert alert-danger py-1 px-2 mt-1" style="display:none; font-size:0.78rem;"></div>
                </div>
                <hr class="my-2">
                <h6 class="text-muted mb-2"><i class="bi bi-person me-1"></i>Primary Contact</h6>
                <div class="row mb-2">
                  <div class="col-6">
                    <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">First Name</label>
                    <input type="text" name="contactFirst" class="form-control form-control-sm"
                           value="${selectedAgency.getPrimaryContact() != null ? selectedAgency.getPrimaryContact().getFirstName() : ''}">
                  </div>
                  <div class="col-6">
                    <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Last Name</label>
                    <input type="text" name="contactLast" class="form-control form-control-sm"
                           value="${selectedAgency.getPrimaryContact() != null ? selectedAgency.getPrimaryContact().getLastName() : ''}">
                  </div>
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Email</label>
                  <input type="email" name="contactEmail" class="form-control form-control-sm"
                         value="${selectedAgency.getPrimaryContact() != null ? selectedAgency.getPrimaryContact().getEmail() : ''}">
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Contact Phone</label>
                  <input type="text" name="contactPhone" class="form-control form-control-sm"
                         value="${selectedAgency.getPrimaryContact() != null ? selectedAgency.getPrimaryContact().getPhone() : ''}">
                </div>
              </div>
              <%-- Right column: Address --%>
              <div class="col-md-6">
                <h6 class="text-muted mb-2"><i class="bi bi-geo-alt me-1"></i>Address</h6>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Address 1</label>
                  <input type="text" name="address1" class="form-control form-control-sm"
                         value="${selectedAgency.getAddress() != null ? selectedAgency.getAddress().getAddress1() : ''}">
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Address 2</label>
                  <input type="text" name="address2" class="form-control form-control-sm"
                         value="${selectedAgency.getAddress() != null ? selectedAgency.getAddress().getAddress2() : ''}">
                </div>
                <div class="mb-2">
                  <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">City</label>
                  <input type="text" name="city" class="form-control form-control-sm"
                         value="${selectedAgency.getAddress() != null ? selectedAgency.getAddress().getCity() : ''}">
                </div>
                <div class="row mb-2">
                  <div class="col-6">
                    <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">State</label>
                    <input type="text" name="state" class="form-control form-control-sm" maxlength="2"
                           value="${selectedAgency.getAddress() != null ? selectedAgency.getAddress().getState() : ''}">
                  </div>
                  <div class="col-6">
                    <label class="form-label fw-semibold mb-0" style="font-size: 0.85rem;">Zip Code</label>
                    <input type="text" name="zipCode" class="form-control form-control-sm"
                           value="${selectedAgency.getAddress() != null ? selectedAgency.getAddress().getZipCode() : ''}">
                  </div>
                </div>
              </div>
            </div>
            <%-- V068: per-agency custom landing page (saved separately via AJAX, sanitized on save) --%>
            <hr class="my-2">
            <div class="p-2 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <label class="form-label fw-semibold m-0" style="font-size:0.82rem;"><i class="bi bi-window me-1"></i>Landing Page HTML</label>
                <div>
                  <button type="button" class="btn btn-sm btn-outline-secondary" onclick="toggleAgencyLandingPreview()">
                    <i class="bi bi-eye me-1"></i><span id="agencyLandingPreviewLabel">Preview</span>
                  </button>
                  <button type="button" class="btn btn-sm btn-outline-primary ms-1" onclick="saveAgencyLanding()">
                    <i class="bi bi-floppy me-1"></i>Save HTML
                  </button>
                </div>
              </div>
              <input type="hidden" id="agencyLandingId" value="${selectedAgency.getId()}">
              <textarea id="agencyLandingHtmlEditor" class="form-control" rows="10"
                        style="font-family: 'Courier New', monospace; font-size:0.78rem; display:block;"
                        placeholder="Paste this agency's landing page HTML here..."><c:out value="${selectedAgency.getLandingHtml()}"/></textarea>
              <iframe id="agencyLandingHtmlPreview" style="width:100%; height:300px; border:1px solid #dee2e6; border-radius:4px; display:none; background:#fff;"></iframe>
              <div id="agencyLandingHtmlStatus" class="mt-1" style="font-size:0.75rem;"></div>
              <div class="form-text" style="font-size:0.72rem;">The white-label front door is active when this HTML is non-blank and the Landing Host resolves. "Save Changes" saves the host; "Save HTML" saves this content.</div>
            </div>
          </div>
          <div class="modal-footer justify-content-center border-0">
            <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save Changes</button>
            <span class="ssa-action-sep">|</span>
            <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <%-- Assign Agent Modal --%>
  <%-- Reassign Manager + Remove --%>
  <c:if test="${not empty selectedAgency && selectedAgency.getManager() != null && eligibleCount > 0}">
  <div class="modal fade" id="reassignManagerModal" tabindex="-1">
    <div class="modal-dialog">
      <div class="modal-content">
        <form method="post" action="AgencyAction">
          <input type="hidden" name="action" value="removeAgent"/>
          <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
          <input type="hidden" name="agentId" value="${mgrId}"/>
          <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
            <h6 class="modal-title fw-semibold"><i class="bi bi-person-gear me-2"></i>Reassign Agency Manager</h6>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
          </div>
          <div class="modal-body">
            <p class="mb-3">
              <strong>${fn:escapeXml(selectedAgency.getManager().getFirstName())} ${fn:escapeXml(selectedAgency.getManager().getLastName())}</strong>
              is the designated agency manager. Select the agent who will take over as manager.
              They will be promoted and the current manager removed from this agency.
            </p>
            <label class="form-label fw-semibold">New Agency Manager</label>
            <select name="newManagerId" class="form-select" required>
              <c:if test="${eligibleCount > 1}">
                <option value="">-- Select --</option>
              </c:if>
              <c:forEach var="cand" items="${agentList}">
                <c:if test="${cand.getId() != mgrId && activeAgentIds.contains(cand.getId())}">
                  <option value="${cand.getId()}">${fn:escapeXml(cand.getFirstName())} ${fn:escapeXml(cand.getLastName())}
                    <c:if test="${cand.getEmail() != null}"> (${fn:escapeXml(cand.getEmail())})</c:if>
                  </option>
                </c:if>
              </c:forEach>
            </select>
            <small class="text-muted mt-1 d-block">Only agents with an active user account can be manager.</small>
          </div>
          <div class="modal-footer justify-content-center border-0">
            <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Promote &amp; Remove</button>
            <span class="ssa-action-sep">|</span>
            <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </div>
  </c:if>

  <div class="modal fade" id="assignAgentModal" tabindex="-1">
    <div class="modal-dialog">
      <div class="modal-content">
        <form method="post" action="AgencyAction">
          <input type="hidden" name="action" value="assignAgent"/>
          <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
          <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
            <h6 class="modal-title fw-semibold"><i class="bi bi-person-plus me-2"></i>Assign Agent</h6>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
          </div>
          <div class="modal-body">
            <c:choose>
              <c:when test="${not empty pendingAgents}">
                <label class="form-label fw-semibold">Select Agent</label>
                <select name="agentId" class="form-select" required>
                  <option value="">-- Select --</option>
                  <c:forEach var="pa" items="${pendingAgents}">
                    <option value="${pa.getId()}">${pa.getFirstName()} ${pa.getLastName()}
                      <c:if test="${pa.getEmail() != null}"> (${pa.getEmail()})</c:if>
                    </option>
                  </c:forEach>
                </select>
                <small class="text-muted mt-1 d-block">Showing users with the Agent role</small>
              </c:when>
              <c:otherwise>
                <div class="text-center text-muted py-3">
                  <i class="bi bi-person-x" style="font-size: 2rem; display: block;"></i>
                  <p class="mb-0">No pending agents available</p>
                  <small>Create a user with the Agent role first</small>
                </div>
              </c:otherwise>
            </c:choose>
          </div>
          <div class="modal-footer justify-content-center border-0">
            <c:if test="${not empty pendingAgents}">
              <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Assign</button>
              <span class="ssa-action-sep">|</span>
            </c:if>
            <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </div>

</c:if>

<script>
  // ── Agency Suppression ──────────────────────────────────────
  function confirmSuppress() {
    if (confirm('Suppress this agency? This will remove all assigned rates and hide the agency from active workflows.')) {
      document.getElementById('suppressForm').submit();
    }
  }

  // ── V068: Per-agency Landing Page ───────────────────────────
  function toggleAgencyLandingPreview() {
    var editor = document.getElementById('agencyLandingHtmlEditor');
    var preview = document.getElementById('agencyLandingHtmlPreview');
    var label = document.getElementById('agencyLandingPreviewLabel');
    if (!editor || !preview) return;
    if (editor.style.display !== 'none') {
      preview.srcdoc = editor.value;
      editor.style.display = 'none';
      preview.style.display = 'block';
      label.textContent = 'Edit HTML';
    } else {
      editor.style.display = 'block';
      preview.style.display = 'none';
      label.textContent = 'Preview';
    }
  }

  function saveAgencyLanding() {
    var editor = document.getElementById('agencyLandingHtmlEditor');
    var idEl = document.getElementById('agencyLandingId');
    var status = document.getElementById('agencyLandingHtmlStatus');
    if (!editor || !idEl) return;
    status.innerHTML = '<span class="text-muted"><i class="bi bi-arrow-repeat"></i> Saving...</span>';
    fetch('AgencyAction', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=saveAgencyLandingHtml&agencyId=' + encodeURIComponent(idEl.value)
          + '&landingHtml=' + encodeURIComponent(editor.value)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.status === 'ok') {
        status.innerHTML = '<span class="text-success"><i class="bi bi-check-circle"></i> Saved.</span>';
      } else {
        status.innerHTML = '<span class="text-danger"><i class="bi bi-x-circle"></i> Save failed.</span>';
      }
      setTimeout(function() { status.innerHTML = ''; }, 3000);
    })
    .catch(function() {
      status.innerHTML = '<span class="text-danger"><i class="bi bi-x-circle"></i> Save failed.</span>';
    });
  }

  // Increment 3: surface a friendly parent-agency validation error (from the editAgency redirect) and reopen the modal.
  (function() {
    var pe = new URLSearchParams(window.location.search).get('parentError');
    if (!pe) return;
    var msg = pe === 'self' ? "An agency can't be its own general agency."
            : pe === 'haschildren' ? "This agency has sub-agencies, so it can't become a sub-agency itself."
            : pe === 'notoplevel' ? 'The selected agency is already a sub-agency; only top-level agencies can be a parent.'
            : pe === 'notfound' ? 'Selected parent agency not found.'
            : 'Invalid parent selection.';
    var el = document.getElementById('agencyParentError');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
    var modalEl = document.getElementById('editAgencyModal');
    if (modalEl && window.bootstrap) { try { new bootstrap.Modal(modalEl).show(); } catch (e) {} }
  })();

  // Surface a friendly landing-host validation error (from the editAgency redirect) and reopen the modal.
  (function() {
    var le = new URLSearchParams(window.location.search).get('landingError');
    if (!le) return;
    var msg = le === 'psp' ? 'That host is reserved as a PSP host and cannot be assigned to an agency.'
            : le === 'duplicate' ? 'That landing host is already used by another agency.'
            : 'Invalid landing host. Use a bare hostname like agency.example.com (no https://, no path).';
    var el = document.getElementById('agencyLandingHostError');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
    var modalEl = document.getElementById('editAgencyModal');
    if (modalEl && window.bootstrap) { try { new bootstrap.Modal(modalEl).show(); } catch (e) {} }
  })();

  // V069: surface a friendly email-domain validation error (from the editAgency redirect).
  (function() {
    var ee = new URLSearchParams(window.location.search).get('emailError');
    if (!ee) return;
    var msg = ee === 'duplicate' ? 'That sending domain is already used by another agency.'
            : 'Invalid sending domain. Use a bare domain like admin.example.com (no https://, no path).';
    var el = document.getElementById('agencyEmailDomainError');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
    var modalEl = document.getElementById('editAgencyModal');
    if (modalEl && window.bootstrap) { try { new bootstrap.Modal(modalEl).show(); } catch (e) {} }
  })();

  // V069: pre-fill the Sending Domain from the Landing Host when the domain is still blank.
  (function() {
    var hostEl = document.querySelector('#editAgencyModal input[name="landingHost"]');
    var domEl = document.getElementById('agencyEmailDomain');
    if (!hostEl || !domEl) return;
    function prefill() {
      if (!domEl.value.trim() && hostEl.value.trim()) domEl.value = hostEl.value.trim();
    }
    hostEl.addEventListener('blur', prefill);
    hostEl.addEventListener('change', prefill);
  })();

  // ── Rate Assignment State Tracking ──────────────────────────

  function updateRateState() {
    var checkboxes = document.querySelectorAll('.rate-cb');
    var isDirty = false;
    checkboxes.forEach(function(cb) {
      var original = cb.dataset.original === 'true';
      var row = document.getElementById('rateRow_' + cb.value);
      if (row) {
        row.style.opacity = cb.checked ? '1' : '0.4';
      }
      if (cb.checked !== original) {
        isDirty = true;
      }
    });
    var btn = document.getElementById('saveRatesBtn');
    if (btn) {
      if (isDirty) {
        btn.disabled = false;
        btn.style.background = '#0d6efd';
        btn.style.color = 'white';
      } else {
        btn.disabled = true;
        btn.style.background = '#ccc';
        btn.style.color = '#666';
      }
    }
  }

  updateRateState();

  // ── Invite Modal Toggle ──────────────────────────────────

  function toggleInviteMode() {
    var isNew = document.getElementById('modeNew').checked;
    document.getElementById('newAgencyFields').style.display = isNew ? '' : 'none';
    document.getElementById('existingAgencyFields').style.display = isNew ? 'none' : '';
    var nameInput = document.getElementById('invAgencyName');
    var selectInput = document.getElementById('invExistingAgency');
    if (isNew) {
      nameInput.required = true;
      selectInput.required = false;
    } else {
      nameInput.required = false;
      selectInput.required = true;
    }
  }

  // ── Rate Pricing Popover ──────────────────────────────────

  var rateData = {};
  <c:if test="${not empty rateTableMap}">
    <c:forEach var="entry" items="${rateTableMap}">
      rateData[${entry.key}] = [
        <c:forEach var="rt" items="${entry.value}" varStatus="s">
          {mod:"${fn:escapeXml(rt.getModule().getShortText())}", fee:"${fn:escapeXml(rt.getPriceItem().getDescription())}", price:${rt.getPrice()}}<c:if test="${!s.last}">,</c:if>
        </c:forEach>
      ];
    </c:forEach>
  </c:if>

  function buildRateGrid(rateId) {
    var rows = rateData[rateId];
    if (!rows || rows.length === 0) return '<em class="text-muted">No pricing set</em>';

    var modules = [];
    var moduleSet = {};
    var feeSet = {};
    var fees = [];
    var priceMap = {};

    rows.forEach(function(r) {
      if (!moduleSet[r.mod]) { moduleSet[r.mod] = true; modules.push(r.mod); }
      if (!feeSet[r.fee]) { feeSet[r.fee] = true; fees.push(r.fee); }
      priceMap[r.fee + '|' + r.mod] = r.price;
    });

    var html = '<table class="table table-sm table-bordered mb-0" style="font-size:0.7rem;min-width:200px;">';
    html += '<thead><tr><th></th>';
    modules.forEach(function(m) { html += '<th class="text-center text-nowrap">' + m + '</th>'; });
    html += '</tr></thead><tbody>';
    fees.forEach(function(f) {
      html += '<tr><td class="text-nowrap fw-semibold">' + f + '</td>';
      modules.forEach(function(m) {
        var p = priceMap[f + '|' + m];
        html += '<td class="text-center">' + (p !== undefined ? '$' + p.toFixed(2) : '') + '</td>';
      });
      html += '</tr>';
    });
    html += '</tbody></table>';
    return html;
  }

  document.querySelectorAll('.rate-hover').forEach(function(el) {
    var rateId = el.dataset.rateId;
    new bootstrap.Popover(el, {
      trigger: 'hover',
      placement: 'left',
      html: true,
      sanitize: false,
      title: 'Pricing',
      content: function() { return buildRateGrid(rateId); },
      container: 'body',
      customClass: 'rate-popover'
    });
  });

  // ── Prospect/Proposal Accordion ──────────────────────────

  var selectedProspectId = null;
  var prospectPanelOpen = true;
  var proposalPanelOpen = true;

  function selectProspect(el) {
    var prospectId = el.dataset.prospectId;
    var prospectName = el.dataset.prospectName;

    if (selectedProspectId === prospectId) {
      selectedProspectId = null;
      document.querySelectorAll('.prospect-row').forEach(function(r) { r.classList.remove('active'); });
      document.getElementById('proposalCard').style.display = 'none';
      var pp = document.getElementById('prospectPanel');
      pp.style.display = '';
      document.getElementById('prospectChevron').style.transform = '';
      prospectPanelOpen = true;
      return;
    }

    selectedProspectId = prospectId;

    document.querySelectorAll('.prospect-row').forEach(function(r) { r.classList.remove('active'); });
    el.classList.add('active');

    var pp = document.getElementById('prospectPanel');
    pp.style.display = 'none';
    document.getElementById('prospectChevron').style.transform = 'rotate(180deg)';
    prospectPanelOpen = false;

    document.getElementById('proposalCard').style.display = '';
    document.getElementById('selectedProspectName').textContent = prospectName;
    document.getElementById('proposalPanel').style.display = '';
    document.getElementById('proposalChevron').style.transform = '';
    proposalPanelOpen = true;

    document.getElementById('propStatusFilter').value = 'ALL';
    document.getElementById('propSortLabel').textContent = 'A-Z';

    filterProposals();
  }

  function toggleProspectPanel() {
    var panel = document.getElementById('prospectPanel');
    var chevron = document.getElementById('prospectChevron');
    if (prospectPanelOpen) {
      panel.style.display = 'none';
      chevron.style.transform = 'rotate(180deg)';
    } else {
      panel.style.display = '';
      chevron.style.transform = '';
    }
    prospectPanelOpen = !prospectPanelOpen;
  }

  function toggleProposalPanel() {
    var panel = document.getElementById('proposalPanel');
    var chevron = document.getElementById('proposalChevron');
    if (proposalPanelOpen) {
      panel.style.display = 'none';
      chevron.style.transform = 'rotate(180deg)';
    } else {
      panel.style.display = '';
      chevron.style.transform = '';
    }
    proposalPanelOpen = !proposalPanelOpen;
  }

  function filterProspects() {
    var agentId = document.getElementById('agentFilter').value;
    var rows = document.querySelectorAll('.prospect-row');
    var visibleCount = 0;
    rows.forEach(function(r) {
      var show = (agentId === 'ALL' || r.dataset.agentId === agentId);
      r.style.display = show ? '' : 'none';
      if (show) visibleCount++;
    });
    document.getElementById('prospectCount').textContent = visibleCount;
  }

  function toggleProspectSort() {
    var panel = document.getElementById('prospectPanel');
    var rows = Array.from(panel.querySelectorAll('.prospect-row'));
    var label = document.getElementById('prospectSortLabel');
    var isAZ = label.textContent === 'A-Z';

    rows.sort(function(a, b) {
      if (isAZ) {
        return a.dataset.agentName.localeCompare(b.dataset.agentName);
      } else {
        return a.dataset.prospectName.localeCompare(b.dataset.prospectName);
      }
    });

    rows.forEach(function(r) { panel.appendChild(r); });
    label.textContent = isAZ ? 'Agent' : 'A-Z';
  }

  function filterProposals() {
    if (!selectedProspectId) return;
    var status = document.getElementById('propStatusFilter').value;
    var rows = document.querySelectorAll('.proposal-row');
    var visibleCount = 0;
    rows.forEach(function(r) {
      var matchProspect = r.dataset.prospectId === selectedProspectId;
      var matchStatus = (status === 'ALL' || r.dataset.status === status);
      var show = matchProspect && matchStatus;
      r.style.display = show ? '' : 'none';
      if (show) visibleCount++;
    });
    document.getElementById('noProposalsMsg').style.display = visibleCount === 0 ? '' : 'none';
  }

  function toggleProposalSort() {
    var tbody = document.querySelector('#proposalTable tbody');
    if (!tbody) return;
    var rows = Array.from(tbody.querySelectorAll('.proposal-row'));
    var label = document.getElementById('propSortLabel');
    var isAZ = label.textContent === 'A-Z';

    rows.sort(function(a, b) {
      if (isAZ) {
        return parseInt(b.dataset.created) - parseInt(a.dataset.created);
      } else {
        return parseInt(a.dataset.created) - parseInt(b.dataset.created);
      }
    });

    rows.forEach(function(r) { tbody.appendChild(r); });
    label.textContent = isAZ ? 'Newest' : 'A-Z';
  }
</script>
<script>
function copyQuoteLink() {
    var f = document.getElementById('quoteLinkField');
    if (!f) return;
    f.select();
    f.setSelectionRange(0, 99999);
    navigator.clipboard.writeText(f.value);
}
</script>
</body>
</html>
