<form action="AddServiceModule" method="post">
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="serviceModuleName" class="input-group-text">Module Name</label>
        <input type="text" class="form-control" id="serviceModuleName" name = "serviceModuleName" required/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm ">
        <label for="smShortText" class="input-group-text">Short Name</label>
        <input type="text" class="form-control" id="smShortText" name = "smShortText" maxlength="6" required/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group input-group-sm">
        <label for="smSortOrder" class="input-group-text">Sort Order #</label>
        <input type="number" class="form-control" id="smSortOrder" name = "smSortOrder" min="0" required/>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <button class="btn btn-secondary form-control" type="submit">Add</button>
    </div>
  </div>
</form>
