<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="isPast" value="pe-none"/>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete() == false}">
  <c:set var="isPast" value=""/>
</c:if>
<div class="detail-section-card">
  <div class="detail-section-header">
    <i class="bi bi-gear-wide-connected"></i>
    Services To Implement
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
        <div id="setupContent" class="overflow-auto" style="max-height: 120px;">
          <c:forEach var="moduleItem" items="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getApplicationModuleList()}">
            <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.88rem;">
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