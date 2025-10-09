<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addReminderModal" role="dialog" tabindex="-1" aria-labelledby="addReminderModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Create a Reminder</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <form method="post" action="CreateReminder25">
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
      </div>
    </div>
  </div>
</div>
