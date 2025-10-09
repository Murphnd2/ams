<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddToDoToChecklist">
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">To-Do Name</span>
      <input type="text" class="form-control" name="toDoName" required placeholder="Enter Description Here">
      <button class="btn btn-success" type="submit">Add</button>
    </div>
  </div>
</form>
