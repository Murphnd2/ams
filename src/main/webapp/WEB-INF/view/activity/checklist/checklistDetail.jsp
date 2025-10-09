<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row mb-2">
  <div class="col-lg-">
    <div class="input-group">
      <span class="input-group-text">Created</span>
      <div class="form-control">
        <fmt:formatDate value="${sessionScope.currentChecklist.getDateCreated()}" pattern="MMMM dd, yyyy @ hh:mm aa"></fmt:formatDate>
      </div>
    </div>
  </div>
</div>
<div class="row mb-2">
  <div class="col">
    <form method="post" action="ChangeChecklistDueDate">
      <div class="input-group">
        <span class="input-group-text">Due Date</span>
        <input type="date"  name="newDueDate" class="form-control" value="${sessionScope.currentChecklist.getDueDate()}">
        <button type="submit" class="btn btn-secondary">Change</button>
      </div>
    </form>
  </div>
</div>
<div class="row mb-2">
  <div class="col">
    <div class="input-group">
      <span class="input-group-text">Is Recurring?</span>
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

    </div>
  </div>
</div>
<c:import url="/WEB-INF/view/activity/checklist/modifyRecurringItem.jsp"></c:import>
