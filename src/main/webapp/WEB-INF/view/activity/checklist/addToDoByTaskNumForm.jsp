<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddTaskToChecklist">
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">To-Do Name</span>
      <input type="number" class="form-control" name="taskID" required placeholder="Enter # Here">
      <button class="btn btn-success" type="submit">Add</button>
    </div>
  </div>
</form>
