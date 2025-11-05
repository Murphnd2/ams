<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row">
    <div class="col">
        <input type="file" class="form-control" name="fileUpload" id="fileUpload">
    </div>
</div>
<div class="row">
    <div class="col">
        <input type="text" class="form-control" name="fileUploadText" id="fileUploadText" placeholder="*Optional - add easy to understand file name">
    </div>
</div>
<div class="row">
    <div class="col"></div>
    <div class="col-auto">
        <button type="submit" class="btn btn-primary" name="btnSubmit" value="Upload">
            <i class="bi bi-plus"></i> Add File
        </button>
    </div>
</div>

