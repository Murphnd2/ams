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
  <div class="detail-section-card">
    <div class="detail-section-body">
      <a href="${pageContext.request.contextPath}/CensusUpload?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"
         class="btn btn-sm btn-outline-ssa w-100">
        <i class="bi bi-people me-1"></i>Census Upload
      </a>
    </div>
  </div>

  <%-- Summit export (S27-C; fourth link S30-C) — same gate as Census Upload above, same proposalId
       expression. AMS emits all four files of the proven Summit chain. The first three labels carry
       Summit's client-setup file numbers (file 3, Premium Billing, is unproven and has no link here);
       the fourth carries none — that sequence's "file 5" is a different file, enrollment into the
       Premium Billing ICHRA notice plan, and its files 5-8 have no emitter. Plain links: whatever
       SummitExportServlet's Content-Disposition does on click is what happens. --%>
  <div class="detail-section-card">
    <div class="detail-section-body">
      <div class="text-muted small fw-semibold mb-1">Summit export</div>
      <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=employer"
         class="btn btn-sm btn-outline-ssa w-100 mb-1">
        <i class="bi bi-download me-1"></i>Summit file 1 — Employer
      </a>
      <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=cdhplan"
         class="btn btn-sm btn-outline-ssa w-100 mb-1">
        <i class="bi bi-download me-1"></i>Summit file 2 — CDH Plan
      </a>
      <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=demographics"
         class="btn btn-sm btn-outline-ssa w-100">
        <i class="bi bi-download me-1"></i>Summit file 4 — Demographics
      </a>
      <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=enrollment"
         class="btn btn-sm btn-outline-ssa w-100 mt-1">
        <i class="bi bi-download me-1"></i>Summit — HRA Enrollment
      </a>
    </div>
  </div>
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