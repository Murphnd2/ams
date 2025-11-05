<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addToDoModal" role="dialog" tabindex="-1" aria-labelledby="addToDoModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Add Another To-Do</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <form method="post" action="AddToDo25">
          <div class="row mb-3">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text">To-Do Name</span>
                <input type="text" class="form-control" name="toDoName" required placeholder="Enter Description Here">
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text">Insert Location</span>
                <select class="form-select" name="insertWhere">
                  <c:if test="${sessionScope.local.getCurrentActivity().getToDoList().size()>1}">
                    <option value="-1">AT THE BOTTOM</option>
                  </c:if>
                  <option value="0">AT THE TOP</option>
                  <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
                    <c:if test="${toDo.getTask().getId()!=153}">
                      <option value="${toDo.getToDo().getId()}">AFTER:&nbsp;&nbsp;${toDo.getTask().getDescription().toLowerCase()}</option>
                    </c:if>
                  </c:forEach>
                </select>
                <button class="btn btn-success" type="submit">Add</button>
              </div>
            </div>
          </div>

        </form>
      </div>
    </div>
  </div>
</div>
