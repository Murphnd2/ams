<%--@elvariable id="toDo" type=""--%>
<div class="row">
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text text-muted">URL:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
        <input type="url" class="form-control" name="linkPath" id="linkPath" placeholder="https://example.com" pattern="https://.*" >
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text text-muted">Link Name</span>
        <input type="text" class="form-control" name="linkName" id="linkName" placeholder="Enter a name for your link" >
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col">
      <button type="submit" class="btn btn-success w-100" name="btnToDo" value="L-${toDo.getId()}">Add</button>
    </div>
  </div>
</div>
