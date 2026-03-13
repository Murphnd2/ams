<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="detail-section-card">
  <div class="detail-section-header">
    <i class="bi bi-chat-left-text"></i>
    Description
    <span class="section-end">
      <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none" id="btnExpandDesc"
              type="button" data-bs-toggle="modal" data-bs-target="#descriptionModal" title="View full description">
        <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
      </button>
    </span>
  </div>
  <div class="detail-section-body">
    <div id="descContent" class="text-dark overflow-auto" style="font-size: 0.9rem; max-height: 120px;">
      ${sessionScope.local.getCurrentActivity().getActivity().getDescription().trim()}
    </div>
    <div class="text-muted fst-italic mt-2" style="font-size: 0.72rem;">
      Logged by ${sessionScope.local.getCurrentActivity().getActivity().getLoggedBy().getFullNameFirstLast()}
      on <fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDateCreated()}" pattern="MMM dd, yyyy @ hh:mm aa"/>
    </div>
  </div>
</div>

<%-- Full description modal --%>
<div class="modal fade" id="descriptionModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-chat-left-text me-2"></i>Full Description</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body" style="font-size: 0.9rem;">
        ${sessionScope.local.getCurrentActivity().getActivity().getDescription().trim()}
      </div>
    </div>
  </div>
</div>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    var el = document.getElementById('descContent');
    if (el.scrollHeight > el.clientHeight) {
      document.getElementById('btnExpandDesc').classList.remove('d-none');
    }
  });
</script>