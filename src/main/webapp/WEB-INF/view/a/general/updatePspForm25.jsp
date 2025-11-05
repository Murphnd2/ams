<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="UpdatePsp25">
  <div class="row mb-1">
    <div class="col">
      <input type="text" class="form-control" name="companyName" required placeholder="Enter Company Name">
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="text" class="form-control" name="address1" required placeholder="Address 1">
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="text" class="form-control" name="address2" placeholder="Address 2 (optional)">
    </div>
  </div>
  <div class="row mb-1">
    <div class="col-7">
      <input type="text" class="form-control" name="city" required placeholder="City">
    </div>
    <div class="col-2">
      <input type="text" class="form-control" name="state" required maxlength="2" placeholder="2 Character State Code">
    </div>
    <div class="col-3">
      <input type="text" class="form-control" name="zip" required maxlength="5" placeholder="5 Digit Zip Code">
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="tel" class="form-control" name="phone" placeholder="Phone (optional) in XXX-XXX-XXXX format">
    </div>
  </div>
  <div class="row mt-2 mb-1">
    <div class="col">
      <div class="w-100 btn btn-altSsa pe-none ">
        Primary Contact Info
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="text" class="form-control" name="lastNameP" required placeholder="Admin Last Name">
    </div>
    <div class="col">
      <input type="text" class="form-control" name="firstNameP" required placeholder="Admin First Name">
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="email" class="form-control" name="emailPrimary" required placeholder="Admin Email">
    </div>
  </div>
  <div class="row mt-2 mb-1">
    <div class="col">
      <div class="w-100 btn btn-altSsa pe-none ">
        First User Info
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="text" class="form-control" name="lastNameS" required placeholder="User Last Name">
    </div>
    <div class="col">
      <input type="text" class="form-control" name="firstNameS" required placeholder="User First Name">
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <input type="email" class="form-control" name="emailSecondary" required placeholder="User Email">
    </div>
  </div>
  <div class="row mt-3 mb-1">
    <div class="col">
      <input type="password" class="form-control" name="accessCode" required placeholder="Authorization Code">
    </div>
    <div class="col">
      <button type="submit" class="btn btn-ssa w-100">
        <i class="bi bi-plus"></i> Update PSP
      </button>
    </div>
  </div>
</form>
