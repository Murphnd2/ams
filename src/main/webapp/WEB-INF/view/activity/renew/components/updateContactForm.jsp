<form method="post" action="UpdateRenewalContact">
  <div class="input-group">
    <span class="input-group-text">First Name</span>
    <input type="text" class="form-control" name="newFirst" value="${sessionScope.contactList.get(0).getFirstName()}">
    <span class="input-group-text">Last Name</span>
    <input type="text" class="form-control" name="newLast" value="${sessionScope.contactList.get(0).getLastName()}">
  </div>
  <div class="input-group">
    <span class="input-group-text">Email&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
    <input type="email" class="form-control" name="newEmail" value="${sessionScope.contactList.get(0).getEmail()}">
    <button type="submit" class="btn btn-primary">
      <i class="bi bi-upload"></i>&nbsp; Update
    </button>
  </div>
</form>
