<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="createReminder">
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">Name</span>
      <input type="text" class="form-control" name="reminderName" required placeholder="Enter Reminder Here">
      <span class="input-group-text">When</span>
      <input type="date" class="form-control" name="reminderDate" required>
      <button class="btn btn-primary" type="submit">Create</button>
    </div>
  </div>
</form>
