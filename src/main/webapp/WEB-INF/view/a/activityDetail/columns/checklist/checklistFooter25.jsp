<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="allDone" value=""/>
<c:if test="${sessionScope.local.getCurrentActivity().getToDoList().size()>0 && sessionScope.local.getCurrentActivity().getToDoList().get(0).isComplete()==false}">
  <c:set var="allDone" value="disabled"/>
</c:if>
<c:if test="${sessionScope.isPspAdmin==true}">
  <c:set var="allDone" value=""/>
</c:if>
<c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
<form method="post" action="CloseActivity25">
  <div class="mt-2 mb-1">
    <c:choose>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Setup\")}">
        <button type="button" class="ssa-action secondary w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal">
          <i class="bi bi-door-open me-1"></i>Close Setup
        </button>
      </c:when>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Renewal\")}">
        <button type="button" class="ssa-action secondary w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal">
          <i class="bi bi-door-open me-1"></i>Close Renewal
        </button>
      </c:when>
      <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Ticket\")}">
        <button type="button" class="ssa-action secondary w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal">
          <i class="bi bi-door-open me-1"></i>Close Ticket
        </button>
      </c:when>
      <c:otherwise>
        <button type="button" class="ssa-action secondary w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal">
          <i class="bi bi-door-open me-1"></i>Close Checklist
        </button>
      </c:otherwise>
    </c:choose>
  </div>
  <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/modals/closeActivityModal.jsp"/>
</form>
</c:if>
<c:if test="${autoShowCloseModal == true}">
<script>
  document.addEventListener('DOMContentLoaded', function() {
    new bootstrap.Modal(document.getElementById('closeActivity')).show();
  });
</script>
</c:if>
<c:import url="/WEB-INF/view/a/checklistDetail/addToDo25.jsp"/>
