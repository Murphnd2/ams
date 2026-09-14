<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="isPast" value="pe-none"/>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete() == false}">
  <c:set var="isPast" value=""/>
</c:if>
<%-- T188: expose the count so a clipped list is visibly partial. Same collection the list below iterates. --%>
<c:set var="setupModules" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getApplicationModuleList()}"/>
<div class="detail-section-card">
  <div class="detail-section-header">
    <i class="bi bi-gear-wide-connected"></i>
    Services To Implement<c:if test="${not empty setupModules}"> (${setupModules.size()})</c:if>
    <span class="section-end">
      <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none me-1" id="btnExpandSetup"
              type="button" data-bs-toggle="modal" data-bs-target="#setupFullModal" title="View all services">
        <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
      </button>
      <button type="button" class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ${isPast}"
              data-bs-target="#addSetupItem" data-bs-toggle="modal" title="Add service module">
        <i class="bi bi-plus-circle" style="font-size: 0.85rem;"></i>
      </button>
    </span>
  </div>
  <div class="detail-section-body">
    <c:choose>
      <c:when test="${empty sessionScope.local.getCurrentActivity().getActivity().getApplication().getApplicationModuleList()}">
        <div class="text-muted fst-italic" style="font-size: 0.82rem;">No service modules assigned yet.</div>
      </c:when>
      <c:otherwise>
        <div id="setupContent" class="overflow-auto d-flex flex-wrap gap-1" style="max-height: 320px;">
          <c:forEach var="moduleItem" items="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getApplicationModuleList()}">
            <div style="font-size: 0.88rem;">
              <span class="badge me-2" style="background-color: var(--ssa); font-size: 0.7rem;">
                  ${moduleItem.getServiceItem().getDescription()}
              </span>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%-- Census Upload (V094) — this page renders only for a Setup (detailDetail25.jsp:5-21 switches
     on the activity's class name), so the button cannot appear on a non-Setup activity by
     construction. proposalId travels as a request parameter rather than through the session,
     matching SummitExportServlet on the sibling surface of this flow: AmsDataLocal.currentActivity
     is a single slot that a second tab rebinds, which on a roster insert would load named people
     against the wrong employer. Hidden when the Setup has no application or proposal to key on. --%>
<c:if test="${sessionScope.local.isPspAdmin()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal()}">
  <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailSummitSetup25.jsp"></c:import>
</c:if>

<%-- s52m -- link to the enrollment matrix, URL-only screen (no nav entry). Same PSP-admin gate
     and same Application/Proposal preconditions as the Census Upload block above, copied
     verbatim: EnrollmentMatrixServlet needs a proposal to resolve the sale's elected legs, the
     same way that flow does, and a non-PSP-admin must not see a link to a page they cannot open. --%>
<c:if test="${sessionScope.local.isPspAdmin()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal()}">
  <div class="mt-2">
    <a class="btn btn-sm btn-outline-ssa"
       href="${pageContext.request.contextPath}/EnrollmentMatrix?setupId=${sessionScope.local.getCurrentActivity().getActivity().getId()}">
      <i class="bi bi-grid-3x3-gap me-1"></i>Enrollment Matrix
    </a>
  </div>
</c:if>

<%-- S58-P3/P4 -- agent-view link controls, any PSP user or admin (matching MatrixAccessResolver's
     PSP-scoped widening; the server gate is EnrollmentMatrixServlet.doPost's isPspStaff on
     action=issueLink). Both POST action=issueLink, which lazily issues
     enrollment_matrix.access_guid and answers {"url"}. "Copy matrix link" puts the URL on the
     clipboard (prompt fallback); "Open agent view" opens it in a new tab -- the tab is opened
     synchronously on click and pointed at the URL once it arrives, so popup blockers don't eat
     it. The page it opens is the standalone read-only /matrix/{guid} view, authenticated and
     authorised per request. Same Application/Proposal preconditions as the blocks above. --%>
<c:if test="${(sessionScope.isPspAdmin or sessionScope.isPspUser)
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal()}">
  <div class="mt-2">
    <button type="button" class="btn btn-sm btn-outline-ssa" id="btnCopyMatrixLink"
            onclick="ammCopyMatrixLink(${sessionScope.local.getCurrentActivity().getActivity().getId()})"
            title="Copy a link an agent can open to view this matrix">
      <i class="bi bi-link-45deg me-1"></i>Copy matrix link
    </button>
    <button type="button" class="btn btn-sm btn-outline-ssa ms-1" id="btnOpenAgentView"
            onclick="ammOpenAgentView(${sessionScope.local.getCurrentActivity().getActivity().getId()})"
            title="Open the read-only agent view of this matrix in a new tab">
      <i class="bi bi-box-arrow-up-right me-1"></i>Open agent view
    </button>
  </div>
  <script>
    function ammIssueMatrixLink(setupId) {
      return fetch('${pageContext.request.contextPath}/EnrollmentMatrix', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'action=issueLink&setupId=' + encodeURIComponent(setupId)
      }).then(function (r) { return r.json(); }).then(function (data) {
        if (!data || !data.url) { throw new Error(data && data.error ? data.error : 'The link could not be issued.'); }
        return data.url;
      });
    }
    function ammCopyMatrixLink(setupId) {
      var btn = document.getElementById('btnCopyMatrixLink');
      if (btn) btn.disabled = true;
      ammIssueMatrixLink(setupId).then(function (url) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(url).then(function () {
            alert('Matrix link copied:\n' + url);
          }, function () { window.prompt('Copy this matrix link:', url); });
        } else {
          window.prompt('Copy this matrix link:', url);
        }
      }).catch(function (e) {
        alert(e && e.message ? e.message : 'The link could not be issued.');
      }).finally(function () { if (btn) btn.disabled = false; });
    }
    function ammOpenAgentView(setupId) {
      var btn = document.getElementById('btnOpenAgentView');
      if (btn) btn.disabled = true;
      var tab = window.open('', '_blank');
      ammIssueMatrixLink(setupId).then(function (url) {
        if (tab) { tab.location = url; } else { window.location = url; }
      }).catch(function (e) {
        if (tab) tab.close();
        alert(e && e.message ? e.message : 'The link could not be issued.');
      }).finally(function () { if (btn) btn.disabled = false; });
    }
  </script>
</c:if>

<%-- Full list modal --%>
<div class="modal fade" id="setupFullModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-gear-wide-connected me-2"></i>Services To Implement</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:forEach var="moduleItem" items="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getApplicationModuleList()}">
          <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.88rem;">
            <span class="badge me-2" style="background-color: var(--ssa); font-size: 0.7rem;">
                ${moduleItem.getServiceItem().getDescription()}
            </span>
          </div>
        </c:forEach>
      </div>
    </div>
  </div>
</div>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    var el = document.getElementById('setupContent');
    if (el && el.scrollHeight > el.clientHeight) {
      document.getElementById('btnExpandSetup').classList.remove('d-none');
    }
  });
</script>