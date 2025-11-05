<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="buttonRule" value="disabled"></c:set>
<c:if test="${sessionScope.availableTasks.size()>0}">
  <c:set var="buttonRule" value=""></c:set>
</c:if>
<form method="post" action="AssignTaskToSequence">
<div class="row mb-3">
  <div class="col">
    <div class="input-group input-group-sm">
      <span class="input-group-text col-2">Select Task</span>
      <c:import url="/WEB-INF/view/checklist/sequence/ddTaskListRaw.jsp"></c:import>
      <button type="submit" class="btn btn-secondary col-2" ${buttonRule}>Assign</button>
    </div>
  </div>
</div>
</form>
