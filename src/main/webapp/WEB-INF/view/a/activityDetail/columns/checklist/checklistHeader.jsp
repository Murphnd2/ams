<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="hdr-bar mt-2 mb-0 d-flex align-items-center justify-content-between">
  <span><i class="bi bi-check2-square me-2"></i>Checklist</span>
  <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
    <button class="btn btn-sm btn-outline-light" style="font-size: 0.7rem; padding: 0.15rem 0.45rem;" type="button" data-bs-toggle="modal" data-bs-target="#addToDoModal" title="Add Task">
      <i class="bi bi-plus-lg"></i>
    </button>
  </c:if>
</div>
