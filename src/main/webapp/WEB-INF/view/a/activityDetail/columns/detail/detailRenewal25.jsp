<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="isPast" value="pe-none"/>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete() == false}">
  <c:set var="isPast" value=""/>
</c:if>
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: #0d6efd !important;">
  <div class="card-body py-2 px-3">
    <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
        <i class="bi bi-shield-check me-1"></i>Benefits In Renewal
      </span>
      <div class="d-flex align-items-center">
        <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none me-1" id="btnExpandRenewal"
                type="button" data-bs-toggle="modal" data-bs-target="#renewalFullModal" title="View all benefits">
          <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
        </button>
        <button type="button" class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ${isPast}"
                data-bs-target="#addRenewalItem" data-bs-toggle="modal" title="Add benefit">
          <i class="bi bi-plus-circle" style="font-size: 0.85rem;"></i>
        </button>
      </div>
    </div>
    <c:choose>
      <c:when test="${empty sessionScope.local.getCurrentActivity().getActivity().getRenewalItemList()}">
        <div class="text-muted fst-italic" style="font-size: 0.82rem;">No benefits added yet.</div>
      </c:when>
      <c:otherwise>
        <div id="renewalContent" class="overflow-auto" style="max-height: 120px;">
          <c:forEach var="ri" items="${sessionScope.local.getCurrentActivity().getActivity().getRenewalItemList()}">
            <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.88rem;">
              <span class="fw-semibold text-dark flex-grow-1">${ri.getBenefit().getPlanDescription()}</span>
              <span class="text-muted me-2" style="font-size: 0.78rem;">
                <fmt:formatDate value="${ri.getDateFor()}" pattern="MMM yy"/>
              </span>
              <form method="post" action="RemoveItemFromRenewal25" class="m-0 p-0">
                <button type="submit" class="btn btn-sm text-danger p-0 ${isPast}"
                        id="ri${ri.getId()}" name="btnRemoveItem" value="${ri.getId()}" title="Remove">
                  <i class="bi bi-x-circle" style="font-size: 0.78rem;"></i>
                </button>
              </form>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%-- Full list modal --%>
<div class="modal fade" id="renewalFullModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-shield-check me-2"></i>Benefits In Renewal</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:forEach var="ri" items="${sessionScope.local.getCurrentActivity().getActivity().getRenewalItemList()}">
          <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.88rem;">
            <span class="fw-semibold text-dark flex-grow-1">${ri.getBenefit().getPlanDescription()}</span>
            <span class="text-muted" style="font-size: 0.78rem;">
              <fmt:formatDate value="${ri.getDateFor()}" pattern="MMM yy"/>
            </span>
          </div>
        </c:forEach>
      </div>
    </div>
  </div>
</div>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    var el = document.getElementById('renewalContent');
    if (el && el.scrollHeight > el.clientHeight) {
      document.getElementById('btnExpandRenewal').classList.remove('d-none');
    }
  });
</script>