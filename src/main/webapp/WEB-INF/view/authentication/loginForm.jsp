
<form method="post" action="AuthenticateUser">
  <div class="container">
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Username</span>
          <input type="text" class="form-control" name="userName" placeholder="Enter Your Username Here">
        </div>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Password&nbsp;&nbsp;&nbsp;</span>
          <input type="password" class="form-control" name="userPassword">
        </div>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <button type="submit" class="btn btn-success w-100" name="submitButton" value="0">
          <i class="bi bi-lock"></i> Login
        </button>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col"></div>
      <div class="col-auto">
        <a href="NeedsHelp">Need Help Logging In?</a>
      </div>
    </div>
  </div>
</form>
