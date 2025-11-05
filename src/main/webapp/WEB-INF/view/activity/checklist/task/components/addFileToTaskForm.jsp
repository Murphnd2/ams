<form method="post" action="AddFileToTask" enctype="multipart/form-data">
  <div class="row mb-3">
    <div class="col">
      <input type="file" name="file" class="form-control"/>
    </div>
    <div class="col-lg-3">
    <input type="submit" value="Attach" class="btn btn-primary w-100">
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Name (Optional)</span>
        <input type="text" class="form-control" name="optionalText" id="optionalText">
      </div>
    </div>
  </div>
</form>
