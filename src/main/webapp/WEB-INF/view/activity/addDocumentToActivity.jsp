<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="AddDocumentToActivity" class="mb-1" enctype="multipart/form-data">
  <div class="row mb-2">
    <div class="col">
      <div class="input-group">
        <input type="file" class="form-control"  name="fileUpload" accept=".doc,.docx,.pdf,.xls,.csv,.txt"  required/>
      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Name</span>
        <input type="text" class="form-control" name="fileName" id="fileName" placeholder="*Optional - Describe File Here">

      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col"></div>
    <div class="col-auto">
      <button type="submit" class="btn btn-outline-dark">
        <i class="bi bi-cloud-plus"></i>&nbsp; Add File
      </button>
    </div>
  </div>
</form>
