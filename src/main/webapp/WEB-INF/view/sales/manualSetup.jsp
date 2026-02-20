<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Create Manual Setup</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4" style="max-width: 700px;">

  <div class="d-flex justify-content-between align-items-center mb-4">
    <div>
      <h4 class="mb-1"><i class="bi bi-building-add me-2"></i>Create Manual Setup</h4>
      <span class="text-muted">Create a new client setup without a submitted application</span>
    </div>
    <a href="ReviewApplications" class="btn btn-outline-secondary btn-sm">
      <i class="bi bi-arrow-left me-1"></i>Back
    </a>
  </div>

  <form method="post" action="GenerateProp25">
    <input type="hidden" name="app_key" value="<%= java.util.UUID.randomUUID().toString() %>">

    <%-- Company Info --%>
    <div class="card mb-3">
      <div class="card-header bg-white py-2">
        <h6 class="mb-0 fw-semibold">Company Information</h6>
      </div>
      <div class="card-body">
        <div class="mb-3">
          <label for="cname" class="form-label">Company Name <span class="text-danger">*</span></label>
          <input type="text" class="form-control" name="cname" id="cname" required>
        </div>
        <div class="row">
          <div class="col-md-6 mb-3">
            <label for="contact" class="form-label">Contact Name <span class="text-danger">*</span></label>
            <input type="text" class="form-control" name="contact" id="contact" required
                   placeholder="First Last">
          </div>
          <div class="col-md-6 mb-3">
            <label for="email" class="form-label">Contact Email <span class="text-danger">*</span></label>
            <input type="email" class="form-control" name="email" id="email" required>
          </div>
        </div>
      </div>
    </div>

    <%-- Lines of Service --%>
    <div class="card mb-3">
      <div class="card-header bg-white py-2">
        <h6 class="mb-0 fw-semibold">Lines of Service</h6>
        <small class="text-muted">Select the services this client is signing up for</small>
      </div>
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q1" onchange="syncToggle('q1',this)">
              <label class="form-check-label" for="sw_q1">Premium Only Plan (POP)</label>
            </div>
            <input type="hidden" name="q1" id="q1" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q2" onchange="syncToggle('q2',this)">
              <label class="form-check-label" for="sw_q2">Flexible Spending (FSA)</label>
            </div>
            <input type="hidden" name="q2" id="q2" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q3" onchange="syncToggle('q3',this)">
              <label class="form-check-label" for="sw_q3">HRA / MERP</label>
            </div>
            <input type="hidden" name="q3" id="q3" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q4" onchange="syncToggle('q4',this)">
              <label class="form-check-label" for="sw_q4">Health Savings (HSA)</label>
            </div>
            <input type="hidden" name="q4" id="q4" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q5" onchange="syncToggle('q5',this)">
              <label class="form-check-label" for="sw_q5">Transit / Commuter</label>
            </div>
            <input type="hidden" name="q5" id="q5" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q6" onchange="syncToggle('q6',this)">
              <label class="form-check-label" for="sw_q6">COBRA</label>
            </div>
            <input type="hidden" name="q6" id="q6" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q7" onchange="syncToggle('q7',this)">
              <label class="form-check-label" for="sw_q7">Payment Services</label>
            </div>
            <input type="hidden" name="q7" id="q7" value="0">
          </div>
          <div class="col-md-6">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" id="sw_q8" onchange="syncToggle('q8',this)">
              <label class="form-check-label" for="sw_q8">Debit Cards</label>
            </div>
            <input type="hidden" name="q8" id="q8" value="0">
          </div>
        </div>
      </div>
    </div>

    <%-- Submit --%>
    <div class="d-flex gap-2">
      <button type="submit" class="btn btn-primary" id="btnSubmit" onclick="disableOnSubmit(this)">
        <i class="bi bi-building-add me-1"></i>Create Setup
      </button>
      <a href="ReviewApplications" class="btn btn-outline-secondary">Cancel</a>
    </div>
  </form>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
  function syncToggle(hiddenId, checkbox) {
    document.getElementById(hiddenId).value = checkbox.checked ? '1' : '0';
  }
  function disableOnSubmit(btn) {
    setTimeout(function() {
      btn.disabled = true;
      btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Creating...';
    }, 50);
  }
</script>
</body>
</html>

