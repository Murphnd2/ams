<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="createSimpleChecklist">
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">Title&nbsp;&nbsp;</span>
      <input type="text" class="form-control" name="reminderName" required placeholder="Enter Title Here">
    </div>
  </div>
  <div class="row mb-3">
    <div class="col-lg-6">
      <div class="input-group">
        <span class="input-group-text">When</span>
        <input type="date" class="form-control" name="reminderDate" required>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Step 1</span>
        <input type="text" class="form-control" name="s1name" required  placeholder="Enter Task Here">
        <span class="input-group-text">Step 2</span>
        <input type="text" class="form-control" name="s2name"  placeholder="Enter Task Here">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Step 3</span>
        <input type="text" class="form-control" name="s3name"  placeholder="Enter Task Here">
        <span class="input-group-text">Step 4</span>
        <input type="text" class="form-control" name="s4name"  placeholder="Enter Task Here">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Step 5</span>
        <input type="text" class="form-control" name="s5name"  placeholder="Enter Task Here">
        <span class="input-group-text">Step 6</span>
        <input type="text" class="form-control" name="s6name"  placeholder="Enter Task Here">
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Step 7</span>
        <input type="text" class="form-control" name="s7name"  placeholder="Enter Task Here">
        <span class="input-group-text">Step 8</span>
        <input type="text" class="form-control" name="s8name"  placeholder="Enter Task Here">
      </div>
    </div>
  </div>
  <button type="submit" class="btn btn-success w-100">Create</button>
</form>
