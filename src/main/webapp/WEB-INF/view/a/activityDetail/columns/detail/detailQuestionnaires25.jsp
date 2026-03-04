<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="qInstances" value="${sessionScope.local.getCurrentActivity().getQuestionnaireInstances()}" />
<c:set var="qAvailable" value="${sessionScope.local.getCurrentActivity().getAvailableQuestionnaires()}" />

<c:if test="${not empty qInstances || not empty qAvailable}">
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: var(--ssa-alt) !important;">
  <div class="card-body py-2 px-3">
    <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa-alt); font-size: 0.85rem;">
        <i class="bi bi-ui-checks-grid me-1"></i>Questionnaires
        <c:if test="${not empty qInstances}">
          <span class="badge rounded-pill text-bg-secondary ms-1" style="font-size: 0.7rem;">${fn:length(qInstances)}</span>
        </c:if>
      </span>
      <%-- Attach dropdown --%>
      <c:if test="${not empty qAvailable}">
        <div class="dropdown">
          <button class="btn btn-sm btn-outline-secondary border-0 p-0 px-1" type="button"
                  data-bs-toggle="dropdown" aria-expanded="false" title="Attach questionnaire"
                  id="qAttachBtn">
            <i class="bi bi-plus-circle" style="font-size: 0.85rem;"></i>
          </button>
          <ul class="dropdown-menu dropdown-menu-end shadow" style="font-size: 0.82rem; max-height: 250px; overflow-y: auto;">
            <li><span class="dropdown-header">Attach Questionnaire</span></li>
            <c:forEach var="qa" items="${qAvailable}">
              <li>
                <form method="post" action="${pageContext.request.contextPath}/QuestionnaireInstanceAction" class="d-inline">
                  <input type="hidden" name="action" value="attach">
                  <input type="hidden" name="questionnaireId" value="${qa.id}">
                  <button type="submit" class="dropdown-item">
                    <c:choose>
                      <c:when test="${qa.external}">
                        <i class="bi bi-box-arrow-up-right me-1" style="color: #7952b3; font-size: 0.75rem;"></i>
                      </c:when>
                      <c:otherwise>
                        <i class="bi bi-ui-checks me-1" style="color: var(--ssa); font-size: 0.75rem;"></i>
                      </c:otherwise>
                    </c:choose>
                    ${qa.name}
                  </button>
                </form>
              </li>
            </c:forEach>
          </ul>
        </div>
      </c:if>
    </div>
    <c:if test="${not empty qInstances}">
    <div class="overflow-auto" style="max-height: 200px;">
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

          <%-- Questionnaire name --%>
          <div class="flex-grow-1 text-truncate">
            <span>${qi.questionnaire.name}</span>
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
            <c:when test="${qi.status == 'REOPENED'}">
              <span class="badge text-bg-warning ms-2" style="font-size: 0.65rem;">Reopened</span>
            </c:when>
            <c:otherwise>
              <span class="badge text-bg-secondary ms-2" style="font-size: 0.65rem;">${qi.status}</span>
            </c:otherwise>
          </c:choose>

          <%-- Action buttons --%>
          <div class="d-flex align-items-center ms-1 gap-0">

            <%-- Email with questionnaire link --%>
            <form method="post" action="${pageContext.request.contextPath}/QuestionnaireInstanceAction" class="d-inline">
              <input type="hidden" name="instanceId" value="${qi.id}">
              <input type="hidden" name="action" value="emailQuestionnaire">
              <button type="submit" class="btn btn-sm btn-outline-secondary border-0 p-0 px-1" title="Email questionnaire link">
                <i class="bi bi-envelope" style="font-size: 0.75rem;"></i>
              </button>
            </form>

            <%-- Copy Link (always available) --%>
            <c:choose>
              <c:when test="${qi.external}">
                <button class="btn btn-sm btn-outline-secondary border-0 p-0 px-1" type="button"
                        onclick="qCopyLink('${qi.questionnaire.resolveExternalUrl(
                                sessionScope.local.getCurrentActivity().getActivity().getFullName(),
                                sessionScope.local.getCurrentActivity().getActivity().getId(),
                                qi.instanceGuid)}')"
                        title="Copy external link">
                  <i class="bi bi-clipboard" style="font-size: 0.75rem;"></i>
                </button>
              </c:when>
              <c:otherwise>
                <button class="btn btn-sm btn-outline-secondary border-0 p-0 px-1" type="button"
                        onclick="qCopyNativeLink('${qi.instanceGuid}')"
                        title="Copy form link">
                  <i class="bi bi-clipboard" style="font-size: 0.75rem;"></i>
                </button>
              </c:otherwise>
            </c:choose>

            <%-- Open/View link --%>
            <c:choose>
              <c:when test="${qi.external}">
                <a class="btn btn-sm btn-outline-secondary border-0 p-0 px-1"
                   href="${qi.questionnaire.resolveExternalUrl(
                           sessionScope.local.getCurrentActivity().getActivity().getFullName(),
                           sessionScope.local.getCurrentActivity().getActivity().getId(),
                           qi.instanceGuid)}"
                   target="_blank" title="Open external form">
                  <i class="bi bi-box-arrow-up-right" style="font-size: 0.75rem;"></i>
                </a>
              </c:when>
              <c:otherwise>
                <a class="btn btn-sm btn-outline-secondary border-0 p-0 px-1"
                   href="${pageContext.request.contextPath}/q/${qi.instanceGuid}"
                   target="_blank" title="${qi.status == 'SUBMITTED' || qi.status == 'REVIEWED' ? 'View response' : 'Open form'}">
                  <i class="bi ${qi.status == 'SUBMITTED' || qi.status == 'REVIEWED' ? 'bi-eye' : 'bi-pencil-square'}" style="font-size: 0.75rem;"></i>
                </a>
              </c:otherwise>
            </c:choose>

            <%-- Action dropdown (kebab menu) --%>
            <div class="dropdown">
              <button class="btn btn-sm btn-outline-secondary border-0 p-0 px-1 q-kebab" type="button"
                      data-bs-toggle="dropdown" aria-expanded="false" title="Actions">
                <i class="bi bi-three-dots-vertical" style="font-size: 0.75rem;"></i>
              </button>
              <ul class="dropdown-menu dropdown-menu-end shadow" style="font-size: 0.82rem;">

                <%-- Review (when SUBMITTED) --%>
                <c:if test="${qi.status == 'SUBMITTED'}">
                  <li>
                    <form method="post" action="${pageContext.request.contextPath}/QuestionnaireInstanceAction" class="d-inline">
                      <input type="hidden" name="instanceId" value="${qi.id}">
                      <input type="hidden" name="action" value="review">
                      <button type="submit" class="dropdown-item"><i class="bi bi-check2-circle me-2 text-success"></i>Mark Reviewed</button>
                    </form>
                  </li>
                </c:if>

                <%-- Reopen (when SUBMITTED or REVIEWED) --%>
                <c:if test="${qi.status == 'SUBMITTED' || qi.status == 'REVIEWED'}">
                  <li>
                    <form method="post" action="${pageContext.request.contextPath}/QuestionnaireInstanceAction" class="d-inline">
                      <input type="hidden" name="instanceId" value="${qi.id}">
                      <input type="hidden" name="action" value="reopen">
                      <button type="submit" class="dropdown-item"><i class="bi bi-arrow-counterclockwise me-2 text-warning"></i>Reopen</button>
                    </form>
                  </li>
                </c:if>

                <%-- Mark Complete — external only, when NOT_STARTED or REOPENED --%>
                <c:if test="${qi.external && (qi.status == 'NOT_STARTED' || qi.status == 'REOPENED')}">
                  <li>
                    <form method="post" action="${pageContext.request.contextPath}/QuestionnaireInstanceAction" class="d-inline">
                      <input type="hidden" name="instanceId" value="${qi.id}">
                      <input type="hidden" name="action" value="markComplete">
                      <button type="submit" class="dropdown-item"><i class="bi bi-check-lg me-2 text-info"></i>Mark Complete</button>
                    </form>
                  </li>
                </c:if>

                <%-- Detach (only if NOT_STARTED) --%>
                <c:if test="${qi.status == 'NOT_STARTED'}">
                  <li><hr class="dropdown-divider"></li>
                  <li>
                    <form method="post" action="${pageContext.request.contextPath}/QuestionnaireInstanceAction" class="d-inline">
                      <input type="hidden" name="instanceId" value="${qi.id}">
                      <input type="hidden" name="action" value="detach">
                      <button type="submit" class="dropdown-item text-danger"><i class="bi bi-x-circle me-2"></i>Detach</button>
                    </form>
                  </li>
                </c:if>
              </ul>
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
    </c:if>
  </div>
</div>

<%-- Copy link toast --%>
<div id="qCopyToast" class="position-fixed bottom-0 end-0 p-3" style="z-index: 9999; display: none;">
  <div class="toast align-items-center text-bg-success border-0 show" role="alert">
    <div class="d-flex">
      <div class="toast-body"><i class="bi bi-clipboard-check me-1"></i>Link copied to clipboard</div>
    </div>
  </div>
</div>

<script>
function qCopyNativeLink(guid) {
    var url = window.location.origin + '${pageContext.request.contextPath}/q/' + guid;
    navigator.clipboard.writeText(url).then(function() { qShowCopyToast(); });
}
function qCopyLink(url) {
    navigator.clipboard.writeText(url).then(function() { qShowCopyToast(); });
}
function qShowCopyToast() {
    var toast = document.getElementById('qCopyToast');
    toast.style.display = 'block';
    setTimeout(function() { toast.style.display = 'none'; }, 2000);
}
// Pre-init kebab dropdowns with fixed strategy to escape overflow container
document.querySelectorAll('.q-kebab').forEach(function(el) {
    new bootstrap.Dropdown(el, { popperConfig: { strategy: 'fixed' } });
});
// Pre-init attach dropdown with fixed strategy too
var attachBtn = document.getElementById('qAttachBtn');
if (attachBtn) {
    new bootstrap.Dropdown(attachBtn, { popperConfig: { strategy: 'fixed' } });
}
</script>
</c:if>
