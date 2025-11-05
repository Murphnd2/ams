<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="AddUrlToActivity" class="mb-1">
  <div class="row mb-2">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">URL Path</span>
        <input type="url" class="form-control"  name="urlUpload" required/>
      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Name</span>
        <input type="text" class="form-control" name="urlName" id="urlName" placeholder="*Optional - Describe Link Here">

      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col"></div>
    <div class="col-auto">
      <button type="submit" class="btn btn-outline-dark">
        <i class="bi bi-cloud-plus"></i>&nbsp; Add URL
      </button>
    </div>
  </div>
</form>
