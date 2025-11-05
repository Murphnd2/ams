<form method="post" action="ResetLogin">
  <div class="container bg-light rounded border border-secondary mt-3">
    <div class="row pt-3 mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Enter New Password&nbsp;&nbsp;</span>
          <input type="password" class="form-control" name="newPassword1" required placeholder="Enter Your New Password Here">
        </div>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Re-enter New Password</span>
          <input type="password" class="form-control" name="newPassword2" required>
        </div>
      </div>
    </div>
    <div class="row pb-3">
      <div class="col-12">
        <button type="submit" class="btn btn-success w-100" name="submitButton" value="0">Change Password</button>
      </div>
    </div>
  </div>
  <div class="row">
    <input type="text" ${sessionScope.hiddenText} value="${sessionScope.errorText}" readonly name="tempUserId">
  </div>
</form>
