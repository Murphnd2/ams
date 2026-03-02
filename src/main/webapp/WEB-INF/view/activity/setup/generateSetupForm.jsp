<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="GenerateProp" class="mb-1">
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Company Name</span>
        <input type="text" class="form-control" name="cname" id="cname" required>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Company Contact</span>
        <input type="text" class="form-control" name="contact" id="contact" required>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Email</span>
        <input type="email" class="form-control" name="email" id="email" required>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">POP</span>
        <select class="form-select" aria-label="recurring freq type drop down" name="q1" id="q1">
          <option value="1" >YES</option>
          <option value="0" selected >NO</option>
        </select>
      </div>
    </div>
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">FSA</span><select class="form-select" aria-label="recurring freq type drop down" name="q2" id="q2">
        <option value="1" >YES</option>
        <option value="0" selected >NO</option>
      </select>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">HRA</span>
        <select class="form-select" aria-label="recurring freq type drop down" name="q3" id="q3">
          <option value="1" >YES</option>
          <option value="0" selected >NO</option>
        </select>
      </div>
    </div>
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">HSA</span><select class="form-select" aria-label="recurring freq type drop down" name="q4" id="q4">
        <option value="1" >YES</option>
        <option value="0" selected>NO</option>
      </select>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Transit</span>
        <select class="form-select" aria-label="recurring freq type drop down" name="q5" id="q5">
          <option value="1" >YES</option>
          <option value="0" selected>NO</option>
        </select>
      </div>
    </div>
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">COBRA</span><select class="form-select" aria-label="recurring freq type drop down" name="q6" id="q6">
        <option value="1" >YES</option>
        <option value="0" selected>NO</option>
      </select>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Payments</span>
        <select class="form-select" aria-label="recurring freq type drop down" name="q7" id="q7">
          <option value="1" >YES</option>
          <option value="0" selected>NO</option>
        </select>
      </div>
    </div>
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Cards</span><select class="form-select" aria-label="recurring freq type drop down" name="q8" id="q8">
        <option value="1" >YES</option>
        <option value="0" selected>NO</option>
      </select>
      </div>
    </div>
  </div>
  <c:set var="app_key" value="${UUID.randomUUID()}"></c:set>
  <div class="row">
    <div class="col"></div>
    <div class="col-auto">
        <input type="hidden" value="AGENCY" name="agency" id="agency">
        <input type="hidden" value="${app_key}" name="app_key" id="app_key">
        <button type="submit" class="ssa-action save">
          <i class="bi bi-building me-1"></i>Add New Customer
        </button>
    </div>
  </div>

</form>
