<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AssignBenefitToRenewal25">
  <label class="form-label text-muted mb-1" style="font-size: 0.82rem;">
    Select a benefit to add to this renewal
  </label>
  <div class="input-group">
    <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/ddBensNotInRenewal25.jsp"></c:import>
    <c:choose>
      <c:when test="${sessionScope.local.getCurrentActivity().getBenefitsNotInRenewal().size()==0}">
        <button type="button" class="btn btn-outline-secondary pe-none">
          <i class="bi bi-plus-square"></i>
        </button>
      </c:when>
      <c:otherwise>
        <button type="submit" class="btn btn-ssa">
          <i class="bi bi-plus-lg me-1"></i>Add
        </button>
      </c:otherwise>
    </c:choose>
  </div>
</form>
