<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="addToDoToList">
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
          <option value="-1">AT THE BOTTOM</option>
          <option value="0">AT THE TOP</option>
          <c:forEach var="toDo" items="${sessionScope.sVar.getCurrentActivityToDos()}">
            <option value="${toDo.getToDo().getId()}">AFTER:&nbsp;&nbsp;${toDo.getTask().getDescription().toLowerCase()}</option>
          </c:forEach>
        </select>
        <button class="btn btn-success" type="submit">Add</button>
      </div>
    </div>
  </div>

</form>
