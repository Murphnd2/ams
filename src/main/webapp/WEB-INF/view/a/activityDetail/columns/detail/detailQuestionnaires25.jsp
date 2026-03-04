<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="qInstances" value="${sessionScope.local.getCurrentActivity().getQuestionnaireInstances()}" />

<c:if test="${not empty qInstances}">
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: var(--ssa-alt) !important;">
  <div class="card-body py-2 px-3">
    <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa-alt); font-size: 0.85rem;">
        <i class="bi bi-ui-checks-grid me-1"></i>Questionnaires
        <span class="badge rounded-pill text-bg-secondary ms-1" style="font-size: 0.7rem;">${fn:length(qInstances)}</span>
      </span>
    </div>
    <div class="overflow-auto" style="max-height: 160px;">
      <c:forEach var="qi" items="${qInstances}">
        <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
          <%-- Mode icon --%>
          <c:choose>
            <c:when test="${qi.external}">
              <i class="bi bi-box-arrow-up-right me-2" style="color: #7952b3;" title="External Form"></i>
            </c:when>
            <c:otherwise>
              <i class="bi bi-ui-checks me-2" style="color: var(--ssa);" title="Native Form"></i>
            </c:otherwise>
          </c:choose>

          <%-- Questionnaire name + mode badge --%>
          <div class="flex-grow-1 text-truncate">
            <span>${qi.questionnaire.name}</span>
            <c:choose>
              <c:when test="${qi.external}">
                <span class="badge text-bg-purple ms-1" style="font-size: 0.65rem; background-color: #7952b3 !important;">EXTERNAL</span>
              </c:when>
              <c:otherwise>
                <span class="badge text-bg-primary ms-1" style="font-size: 0.65rem;">NATIVE</span>
              </c:otherwise>
            </c:choose>
          </div>

          <%-- Status badge --%>
          <c:choose>
            <c:when test="${qi.status == 'NOT_STARTED'}">
              <span class="badge text-bg-secondary ms-2" style="font-size: 0.65rem;">Not Started</span>
            </c:when>
            <c:when test="${qi.status == 'IN_PROGRESS'}">
              <span class="badge text-bg-warning ms-2" style="font-size: 0.65rem;">In Progress</span>
            </c:when>
            <c:when test="${qi.status == 'SUBMITTED'}">
              <span class="badge text-bg-info ms-2" style="font-size: 0.65rem;">Submitted</span>
            </c:when>
            <c:when test="${qi.status == 'REVIEWED'}">
              <span class="badge text-bg-success ms-2" style="font-size: 0.65rem;">Reviewed</span>
            </c:when>
            <c:otherwise>
              <span class="badge text-bg-secondary ms-2" style="font-size: 0.65rem;">${qi.status}</span>
            </c:otherwise>
          </c:choose>

          <%-- Action link --%>
          <c:choose>
            <c:when test="${qi.external}">
              <a class="btn btn-sm btn-outline-secondary border-0 p-0 px-1 ms-1"
                 href="${qi.questionnaire.resolveExternalUrl(
                         sessionScope.local.getCurrentActivity().getActivity().getFullName(),
                         sessionScope.local.getCurrentActivity().getActivity().getId(),
                         qi.instanceGuid)}"
                 target="_blank" title="Open external form">
                <i class="bi bi-box-arrow-up-right" style="font-size: 0.75rem;"></i>
              </a>
            </c:when>
            <c:otherwise>
              <span class="text-muted ms-1" style="font-size: 0.7rem;" title="Native form (coming soon)">
                <i class="bi bi-eye" style="font-size: 0.75rem;"></i>
              </span>
            </c:otherwise>
          </c:choose>
        </div>
      </c:forEach>
    </div>
  </div>
</div>
</c:if>
