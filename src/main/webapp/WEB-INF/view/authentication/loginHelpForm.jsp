<form method="post" action="HelpUserLogin">
  <div class="container">
    <div class="row mb-3 mt-5">
      <div class="col">
          <label for="userName" class="form-label">Please Enter Your Username or Email Address</label>
          <input type="text" class="form-control" name="userName" id="userName" placeholder="Enter Your Username Here">
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <button type="submit" class="btn btn-danger w-100" name="submitButton" id="btn2" value="1">Reset My Password</button>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <button type="submit" class="btn btn-outline-primary w-100" name="submitButton" id="btn1" value="0">Send One-Time Login Link</button>
      </div>
    </div>
  </div>
</form>
