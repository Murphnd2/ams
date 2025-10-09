<form method="post" action="UpdateTaskName" >
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Task Name&nbsp;&nbsp;</span>
          <input type="text" class="form-control" name="task_name" required value="${currentTask.getDescription()}">
          <button type="submit" class="btn btn-outline-success" name="submitButton" value="0">Update Name</button>
        </div>
      </div>
    </div>
</form>
