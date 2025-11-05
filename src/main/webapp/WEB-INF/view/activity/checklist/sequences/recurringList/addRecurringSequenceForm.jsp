<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddRecurringSequence">
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">List Name</span>
      <input type="text" class="form-control" name="sequenceName" id="sequenceName" required placeholder="Enter description here">
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Effective Date</span>
        <input class="form-control" type="date" name="startDate" id="startDate" required>
      </div>
    </div>
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Days in Advance</span>
        <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddDaysAdvance.jsp"></c:import>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">Assign To</span>
      <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddUsers.jsp"></c:import>
    </div>
  </div>
  <div class="row mb-3">
    <div class="input-group">
      <span class="input-group-text">Frequency</span>
      <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddFrequency.jsp"></c:import>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text w-100">Days It Occurs (Only Used with Weekly)</span>
      </div>
    </div>
    <div class="col">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="cbMonday" name="cbMonday" value="1">
        <label class="form-check-label" for="cbMonday">M</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="cbTuesday" name="cbTuesday"  value="2">
        <label class="form-check-label" for="cbTuesday">T</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="cbWednesday" name="cbWednesday" value="3">
        <label class="form-check-label" for="cbWednesday">W</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="cbThursday" name="cbThursday" value="4">
        <label class="form-check-label" for="cbThursday">R</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="cbFriday" name="cbFriday" value="5">
        <label class="form-check-label" for="cbFriday">F</label>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="input-group">
      <button class="btn btn-primary w-50" id="btnStandardRec" value="1" type="submit">Create</button>
      <button class="btn btn-secondary w-50" id="btnOneTaskList" value="2" type="submit">Create One Task List</button>
    </div>
  </div>
</form>
