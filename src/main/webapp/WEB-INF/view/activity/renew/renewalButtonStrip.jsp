<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="dropdown">
  <button class="btn btn-secondary dropdown-toggle" type="button" id="ddRenewalActions" data-bs-toggle="dropdown" aria-expanded="false">
    Renewal Actions
  </button>
  <ul class="dropdown-menu" aria-labelledby="ddRenewalActions">
    <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#addRenewalTaskModal">Add Task</button></li>
    <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#addRenewalNoteModal">Add Note</button></li>
    <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#manageRenewalContactsModal">Manage Contacts</button></li>
    <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#viewRenewalClosedTasksModal">View Completed Tasks</button></li>
    <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#addRenewalModal">View Upcoming</button></li>
  </ul>
</div>

