<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addReminderModal" tabindex="-1" aria-labelledby="addReminderLabel" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold" id="addReminderLabel">
          <i class="bi bi-bell me-2"></i>New Reminder
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form method="post" action="CreateReminder25">
          <div class="mb-3">
            <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">What do you need to remember?</label>
            <input type="text" class="form-control" name="reminderName" required placeholder="e.g. Follow up with broker on rates">
          </div>
          <div class="mb-3">
            <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">Due Date</label>
            <input type="date" class="form-control" name="reminderDate" required>
          </div>
          <button type="submit" class="btn btn-ssa w-100">
            <i class="bi bi-bell-fill me-1"></i>Create Reminder
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
