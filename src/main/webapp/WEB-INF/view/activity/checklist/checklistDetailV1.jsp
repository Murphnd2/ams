<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row mb-2">
  <div class="col-lg-">
    <div class="input-group">

      <span class="input-group-text">Recurs?</span>
      <c:choose>
        <c:when test="${sessionScope.currentChecklist.getRecurringTaskList()==null || sessionScope.currentChecklist.getRecurringTaskList().isInActive()==true}">
          <div class="form-control text-danger fw-bold">
            No
          </div>
        </c:when>
        <c:otherwise>
          <div class="form-control text-primary">
            Yes
          </div>
        </c:otherwise>
      </c:choose>
      <span class="input-group-text">Due</span>
      <input type="date"  name="newDueDate" class="form-control" value="${sessionScope.currentChecklist.getDueDate()}">
      <button type="submit" class="btn btn-secondary">Change</button>
    </div>
  </div>
</div>
