<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container">
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="agencyName" class="col-3 input-group-text">Agency Name</label>
            <input type="text" class="form-control" id="agencyName" name = "agencyName" required/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="taxId" class="col-3 input-group-text">Tax ID</label>
        <input type="text" class="form-control" id="taxId" name="taxId" pattern="\d{2}-\d{7}" placeholder="##-#######" title="Please match the requested format of ##-#######"/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="agencyPhone" class="col-3 input-group-text">Phone #</label>
        <input type="tel" class="form-control" id="agencyPhone" name="agencyPhone"  placeholder="###-555-1234" pattern="[0-9]{3}-[0-9]{3}-[0-9]{4}" required/>
      </div>
    </div>
  </div>
</div>
