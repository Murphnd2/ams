<form method="post" action="ApplySequenceFilter">
  <div class="row">
    <div class="col">
      <div class="form-check form-check-inline form-switch">
        <input class="form-check-input" type="checkbox" name="switchSetups" id="switchSetups" ${sessionScope.fS} value="${sessionScope.fS}">
        <label class="form-check-label" for="switchSetups">Setup</label>
      </div>
      <div class="form-check form-check-inline form-switch">
        <input class="form-check-input" type="checkbox" name="switchRenewals" id="switchRenewals" ${sessionScope.fR} value="${sessionScope.fR}">
        <label class="form-check-label" for="switchRenewals">Renewal</label>
      </div>
      <div class="form-check form-check-inline form-switch">
        <input class="form-check-input" type="checkbox" name="switchTickets" id="switchTickets" ${sessionScope.fT} value="${sessionScope.fT}">
        <label class="form-check-label" for="switchTickets">Ticket</label>
      </div>
      <div class="form-check form-check-inline form-switch">
        <input class="form-check-input" type="checkbox"name="switchUsers"  id="switchUsers" ${sessionScope.fU} value="${sessionScope.fU}">
        <label class="form-check-label" for="switchUsers">User</label>
      </div>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <button type="submit" class="btn btn-sm btn-primary w-100">Filter</button>
    </div>
  </div>
</form>