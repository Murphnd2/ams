<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="ChangeActivityDueDate" class="mb-1">
  <div class="accordion" id="changeDueDateForm">
    <div class="accordion-item">
      <div class="accordion-header" id="changeDueDateHeader">
        <div class="input-group">
          <div class="form-control text-danger fw-bolder">
            Due on <fmt:formatDate value="${sessionScope.currentActivity.getDueDate()}" pattern="MMMM dd, yyyy"></fmt:formatDate>
          </div>
          <button type="button" data-bs-toggle="collapse" data-bs-target="#changeDueDateBody" class="btn btn-outline-danger">
            <i class="bi bi-caret-down-square-fill"></i>
          </button>
        </div>
      </div>
      <div class="accordion-collapse collapse" id="changeDueDateBody">
        <div class="accordion-body p-0 m-0 mt-1">
          <div class="input-group w-100 mb-1">
            <span class="input-group-text bg-danger text-white">Choose New Due Date</span>
            <input type="date" name="newDueDate" class="form-control text-danger" value="${sessionScope.currentActivity.getDueDate()}">
            <button type="submit" class="btn btn-outline-danger">
              <i class="bi bi-nintendo-switch"></i> <span class="d-none d-md-inline">Change</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</form>
