<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addToDoModal" tabindex="-1" aria-labelledby="addToDoLabel" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background: linear-gradient(135deg, #0d5681, #0a4468); color: white;">
        <h6 class="modal-title m-0" id="addToDoLabel">
          <i class="bi bi-plus-circle me-1"></i>Add Task
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form method="post" action="AddToDo25">
          <div class="mb-3">
            <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">Task Name</label>
            <input type="text" class="form-control" name="toDoName" required placeholder="e.g. Follow up with employer">
          </div>
          <div class="mb-3">
            <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">Insert Location</label>
            <select class="form-select" name="insertWhere" style="font-size: 0.85rem;">
              <option value="0">At the top</option>
              <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
                <c:if test="${toDo.getTask().getId()!=153 && toDo.isComplete()==false}">
                  <option value="${toDo.getToDo().getId()}">After: ${toDo.getDescription()}</option>
                </c:if>
              </c:forEach>
              <c:if test="${sessionScope.local.getCurrentActivity().getToDoList().size()>1}">
                <option value="-1" selected>At the bottom</option>
              </c:if>
            </select>
          </div>
          <button type="submit" class="btn btn-ssa w-100">
            <i class="bi bi-plus-circle me-1"></i>Add Task
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
