<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
  <title>Agency Manager</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
  <style>
    :root { --ssa: #0d5681; --ssa-alt: #87a948; }
    .btn-ssa { background: var(--ssa); border-color: var(--ssa); color: white; }
    .btn-ssa:hover { background: #06357a; color: white; }
    .btn-outline-ssa { background: white; border-color: var(--ssa); color: var(--ssa); }
    .btn-outline-ssa:hover { background: var(--ssa); color: white; }
    .agency-card { cursor: pointer; transition: all 0.15s; }
    .agency-card:hover { background-color: #f0f4f8; }
    .agency-card.active { border-left: 4px solid #2B5F8A; background-color: #e8eef4; }
    .agent-row { border-bottom: 1px solid #eee; padding: 0.5rem 0; }
    .agent-row:last-child { border-bottom: none; }
    .rate-check { padding: 0.4rem 0.75rem; border-bottom: 1px solid #eee; }
    .rate-check:last-child { border-bottom: none; }
    .detail-label { color: #6c757d; font-size: 0.85rem; margin-bottom: 2px; }
    .empty-state { text-align: center; color: #6c757d; padding: 3rem 1rem; }
    .empty-state i { font-size: 2.5rem; margin-bottom: 0.5rem; display: block; }
  </style>
</head>
<body class="bg-light">
<div class="container-fluid py-3" style="max-width: 1400px;">

  <%-- Header --%>
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div>
      <h4 class="mb-0"><i class="bi bi-people-fill me-2"></i>Agency Manager</h4>
      <small class="text-muted">Manage agencies, rate assignments, and agents</small>
    </div>
    <div>
      <c:set var="adminCurrentPage" value="agencyManager" scope="request"/>
      <c:import url="/WEB-INF/view/sales/adminNav.jsp"/>
    </div>
  </div>

  <div class="row g-3">

    <%-- ======================== LEFT COLUMN: Agency List ======================== --%>
    <div class="col-lg-3">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center" style="background-color: #2B5F8A; color: white;">
          <span class="fw-bold"><i class="bi bi-building me-1"></i>Agencies</span>
          <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addAgencyModal">
            <i class="bi bi-plus-lg"></i>
          </button>
        </div>
        <div class="card-body p-0">
          <c:choose>
            <c:when test="${not empty agencyList}">
              <c:forEach var="agency" items="${agencyList}">
                <a href="PspAgencyHome?agencyId=${agency.getId()}" class="d-block text-decoration-none text-dark">
                  <div class="agency-card p-2 ps-3 ${selectedAgency != null && selectedAgency.getId() == agency.getId() ? 'active' : ''}">
                    <div class="fw-semibold">${agency.getName()}</div>
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
                <i class="bi bi-building"></i>
                <p class="mb-0">No agencies created yet</p>
              </div>
            </c:otherwise>
          </c:choose>
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
                  <h5 class="mb-0">${selectedAgency.getName()}</h5>
                  <small class="text-muted">Agency ID: ${selectedAgency.getId()}</small>
                </div>
                <button type="button" class="btn btn-outline-primary btn-sm" data-bs-toggle="modal" data-bs-target="#editAgencyModal">
                  <i class="bi bi-pencil me-1"></i>Edit
                </button>
              </div>
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
                    <div class="agent-row d-flex justify-content-between align-items-center px-3">
                      <div>
                        <span class="fw-semibold">${agent.getFirstName()} ${agent.getLastName()}</span>
                        <c:if test="${agent.getEmail() != null}">
                          <br><small class="text-muted">${agent.getEmail()}</small>
                        </c:if>
                      </div>
                      <form method="post" action="AgencyAction" class="d-inline">
                        <input type="hidden" name="action" value="removeAgent"/>
                        <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                        <input type="hidden" name="agentId" value="${agent.getId()}"/>
                        <button type="submit" class="btn btn-sm btn-outline-danger" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                onclick="return confirm('Remove ${agent.getFirstName()} ${agent.getLastName()} from this agency?');">
                          <i class="bi bi-x-lg"></i>
                        </button>
                      </form>
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

    <%-- ======================== RIGHT COLUMN: Rate Assignments ======================== --%>
    <div class="col-lg-4">
      <c:if test="${not empty selectedAgency}">
        <div class="card">
          <div class="card-header fw-bold" style="background-color: #5a8a6a; color: white;">
            <i class="bi bi-tags me-1"></i>Rate Assignments
          </div>
          <div class="card-body p-0">
            <c:choose>
              <c:when test="${not empty allRates}">
                <form method="post" action="AgencyAction">
                  <input type="hidden" name="action" value="updateRates"/>
                  <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
                  <c:forEach var="rate" items="${allRates}">
                    <div class="rate-check">
                      <div class="form-check">
                          <%-- Check if this rate is in the agency's rate list --%>
                        <c:set var="isAssigned" value="false"/>
                        <c:forEach var="agencyRate" items="${selectedAgency.getAgencyRateList()}">
                          <c:if test="${agencyRate.getId() == rate.getId()}">
                            <c:set var="isAssigned" value="true"/>
                          </c:if>
                        </c:forEach>
                        <input class="form-check-input" type="checkbox" name="rateIds" value="${rate.getId()}"
                               id="rate_${rate.getId()}" <c:if test="${isAssigned == 'true'}">checked</c:if>>
                        <label class="form-check-label" for="rate_${rate.getId()}">
                          <span class="fw-semibold">${rate.getDescription()}</span>
                          <br><small class="text-muted">ID: ${rate.getId()}</small>
                        </label>
                      </div>
                    </div>
                  </c:forEach>
                  <div class="p-2">
                    <button type="submit" class="btn btn-primary btn-sm w-100">
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
      </c:if>
    </div>

  </div><%-- end row --%>
</div>

<%-- ======================== MODALS ======================== --%>

<%-- Add New Agency Modal --%>
<div class="modal fade" id="addAgencyModal" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <form method="post" action="AgencyAction">
        <input type="hidden" name="action" value="createAgency"/>
        <div class="modal-header">
          <h5 class="modal-title"><i class="bi bi-building me-2"></i>New Agency</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label fw-semibold">Agency Name</label>
            <input type="text" name="agencyName" class="form-control" required placeholder="e.g. Midwest Benefits Group">
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold">Phone</label>
            <input type="text" name="phone" class="form-control" placeholder="555-555-5555">
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold">Tax ID</label>
            <input type="text" name="taxId" class="form-control" placeholder="XX-XXXXXXX">
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-primary">Create Agency</button>
        </div>
      </form>
    </div>
  </div>
</div>

<%-- Edit Agency Modal --%>
<c:if test="${not empty selectedAgency}">
  <div class="modal fade" id="editAgencyModal" tabindex="-1">
    <div class="modal-dialog">
      <div class="modal-content">
        <form method="post" action="AgencyAction">
          <input type="hidden" name="action" value="editAgency"/>
          <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
          <div class="modal-header">
            <h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Agency</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-semibold">Agency Name</label>
              <input type="text" name="agencyName" class="form-control" required value="${selectedAgency.getName()}">
            </div>
            <div class="mb-3">
              <label class="form-label fw-semibold">Phone</label>
              <input type="text" name="phone" class="form-control" value="${selectedAgency.getPhone()}">
            </div>
            <div class="mb-3">
              <label class="form-label fw-semibold">Tax ID</label>
              <input type="text" name="taxId" class="form-control" value="${selectedAgency.getTaxId()}">
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary">Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <%-- Assign Agent Modal --%>
  <div class="modal fade" id="assignAgentModal" tabindex="-1">
    <div class="modal-dialog">
      <div class="modal-content">
        <form method="post" action="AgencyAction">
          <input type="hidden" name="action" value="assignAgent"/>
          <input type="hidden" name="agencyId" value="${selectedAgency.getId()}"/>
          <div class="modal-header">
            <h5 class="modal-title"><i class="bi bi-person-plus me-2"></i>Assign Agent</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
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
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <c:if test="${not empty pendingAgents}">
              <button type="submit" class="btn btn-primary">Assign</button>
            </c:if>
          </div>
        </form>
      </div>
    </div>
  </div>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
