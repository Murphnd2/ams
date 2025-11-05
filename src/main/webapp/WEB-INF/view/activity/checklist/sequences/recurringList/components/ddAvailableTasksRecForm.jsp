<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="toDisable" value="disabled"></c:set>
<c:if test="${sessionScope.availableTasksRec.size()>0}">
  <c:set var="toDisable" value=""></c:set>
</c:if>
<form method="post" action="AddAvailableTaskRec">
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <span class="input-group-text">Task To Add</span>
        <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddAvailableTasksRec.jsp"></c:import>
        <input type="text" class="form-control" ${toDisable} id="floatingSort" required placeholder="Sort Order" name="tbSortOrder">
        <button class="btn btn-primary" type="submit" ${toDisable}>Add</button>
      </div>
    </div>
  </div>
</form>
