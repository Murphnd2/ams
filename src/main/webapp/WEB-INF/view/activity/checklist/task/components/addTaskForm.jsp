<form method="post" action="AddTask" >
  <div class="container">
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Task Name&nbsp;&nbsp;</span>
          <input type="text" class="form-control" name="task_name" placeholder="Enter Task description here" required>
        </div>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text text-muted">* URL:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
          <input type="url" class="form-control" name="linkPath" id="linkPath" placeholder="**OPTIONAL** enter as https://example.com" pattern="https://.*" >
        </div>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text text-muted">* Link Name</span>
          <input type="text" class="form-control" name="linkName" id="linkName" placeholder="**OPTIONAL** enter a name for your link" >
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <button type="submit" class="btn btn-success w-100" name="submitButton" value="0">Submit</button>
      </div>
    </div>
  </div>
</form>
