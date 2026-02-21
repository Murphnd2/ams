<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Accept Invitation</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
  <style>
    :root { --ssa: #0d5681; }
    body { background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%); min-height: 100vh; }
    .reg-card { max-width: 700px; margin: 2rem auto; border: none; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,0.1); }
    .reg-header { background: var(--ssa); color: white; border-radius: 12px 12px 0 0; padding: 1.5rem 2rem; }
    .reg-header h4 { margin: 0; }
    .reg-header small { opacity: 0.8; }
    .btn-ssa { background: var(--ssa); border-color: var(--ssa); color: white; }
    .btn-ssa:hover { background: #06357a; color: white; }
    .section-label { color: #6c757d; font-size: 0.9rem; font-weight: 600; border-bottom: 1px solid #dee2e6; padding-bottom: 0.3rem; margin-bottom: 0.75rem; margin-top: 1rem; }
  </style>
</head>
<body>
<div class="container py-4">

  <%-- Error State --%>
  <c:if test="${not empty inviteError}">
    <div class="reg-card card">
      <div class="reg-header">
        <h4><i class="bi bi-envelope-x me-2"></i>Invitation Problem</h4>
      </div>
      <div class="card-body text-center py-5">
        <i class="bi bi-exclamation-triangle" style="font-size: 3rem; color: #dc3545; display: block; margin-bottom: 1rem;"></i>
        <p class="fs-5 mb-3">${inviteError}</p>
        <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-secondary">Go to Login</a>
      </div>
    </div>
  </c:if>

  <%-- Registration Form --%>
  <c:if test="${not empty invitation}">
    <div class="reg-card card">
      <div class="reg-header">
        <h4><i class="bi bi-person-plus me-2"></i>Complete Your Registration</h4>
        <small>You've been invited to join <strong>${agency.getName()}</strong> as
          <c:choose>
            <c:when test="${isManager}">an <strong>Agency Manager</strong></c:when>
            <c:otherwise>an <strong>Agent</strong></c:otherwise>
          </c:choose>
        </small>
      </div>
      <div class="card-body px-4 py-3">

        <c:if test="${not empty formError}">
          <div class="alert alert-danger py-2" role="alert">
            <i class="bi bi-exclamation-circle me-1"></i>${formError}
          </div>
        </c:if>

        <form method="post" action="AcceptInvite">
          <input type="hidden" name="guid" value="${invitation.getGuid()}"/>

          <%-- Your Info --%>
          <div class="section-label"><i class="bi bi-person me-1"></i>Your Information</div>
          <div class="row mb-2">
            <div class="col-md-6">
              <label class="form-label fw-semibold mb-0">First Name</label>
              <input type="text" name="firstName" class="form-control" required
                     value="${not empty param.firstName ? param.firstName : invitation.getFirstName()}">
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold mb-0">Last Name</label>
              <input type="text" name="lastName" class="form-control" required
                     value="${not empty param.lastName ? param.lastName : invitation.getLastName()}">
            </div>
          </div>
          <div class="mb-2">
            <label class="form-label fw-semibold mb-0">Email</label>
            <input type="email" name="email" class="form-control" required
                   value="${not empty param.email ? param.email : invitation.getEmail()}">
          </div>

          <%-- Agency Manager: Agency details --%>
          <c:if test="${isManager}">
            <div class="section-label"><i class="bi bi-briefcase me-1"></i>Agency Details</div>
            <div class="mb-2">
              <label class="form-label fw-semibold mb-0">Agency Name</label>
              <input type="text" name="agencyName" class="form-control" required
                     value="${not empty param.agencyName ? param.agencyName : agency.getName()}">
            </div>
            <div class="mb-2">
              <label class="form-label fw-semibold mb-0">Tax ID <small class="text-muted fw-normal">(required for 1099)</small></label>
              <input type="text" name="taxId" class="form-control" required placeholder="XX-XXXXXXX"
                     value="${not empty param.taxId ? param.taxId : agency.getTaxId()}">
            </div>

            <div class="section-label"><i class="bi bi-geo-alt me-1"></i>Mailing Address</div>
            <div class="mb-2">
              <label class="form-label fw-semibold mb-0">Address 1</label>
              <input type="text" name="address1" class="form-control" required
                     value="${not empty param.address1 ? param.address1 : (agency.getAddress() != null ? agency.getAddress().getAddress1() : '')}">
            </div>
            <div class="mb-2">
              <label class="form-label fw-semibold mb-0">Address 2</label>
              <input type="text" name="address2" class="form-control"
                     value="${not empty param.address2 ? param.address2 : (agency.getAddress() != null ? agency.getAddress().getAddress2() : '')}">
            </div>
            <div class="row mb-2">
              <div class="col-md-6">
                <label class="form-label fw-semibold mb-0">City</label>
                <input type="text" name="city" class="form-control" required
                       value="${not empty param.city ? param.city : (agency.getAddress() != null ? agency.getAddress().getCity() : '')}">
              </div>
              <div class="col-md-3">
                <label class="form-label fw-semibold mb-0">State</label>
                <input type="text" name="state" class="form-control" required maxlength="2"
                       value="${not empty param.state ? param.state : (agency.getAddress() != null ? agency.getAddress().getState() : '')}">
              </div>
              <div class="col-md-3">
                <label class="form-label fw-semibold mb-0">Zip Code</label>
                <input type="text" name="zipCode" class="form-control" required
                       value="${not empty param.zipCode ? param.zipCode : (agency.getAddress() != null ? agency.getAddress().getZipCode() : '')}">
              </div>
            </div>
          </c:if>

          <%-- Agent: Optional address --%>
          <c:if test="${!isManager}">
            <div class="section-label"><i class="bi bi-briefcase me-1"></i>Agency</div>
            <div class="mb-2">
              <label class="form-label fw-semibold mb-0">Agency</label>
              <input type="text" class="form-control" disabled value="${agency.getName()}">
            </div>

            <div class="section-label"><i class="bi bi-geo-alt me-1"></i>Address <small class="text-muted fw-normal">(optional)</small></div>
            <div class="mb-2">
              <label class="form-label fw-semibold mb-0">Address 1</label>
              <input type="text" name="address1" class="form-control" value="${param.address1}">
            </div>
            <div class="row mb-2">
              <div class="col-md-6">
                <label class="form-label fw-semibold mb-0">City</label>
                <input type="text" name="city" class="form-control" value="${param.city}">
              </div>
              <div class="col-md-3">
                <label class="form-label fw-semibold mb-0">State</label>
                <input type="text" name="state" class="form-control" maxlength="2" value="${param.state}">
              </div>
              <div class="col-md-3">
                <label class="form-label fw-semibold mb-0">Zip</label>
                <input type="text" name="zipCode" class="form-control" value="${param.zipCode}">
              </div>
            </div>
          </c:if>

          <%-- Password --%>
          <div class="section-label"><i class="bi bi-lock me-1"></i>Set Your Password</div>
          <div class="row mb-2">
            <div class="col-md-6">
              <label class="form-label fw-semibold mb-0">Password</label>
              <input type="password" name="password" class="form-control" required minlength="8"
                     placeholder="Minimum 8 characters">
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold mb-0">Confirm Password</label>
              <input type="password" name="confirmPassword" class="form-control" required minlength="8">
            </div>
          </div>

          <div class="mt-3 mb-2">
            <button type="submit" class="btn btn-ssa btn-lg w-100">
              <i class="bi bi-check-circle me-2"></i>Complete Registration
            </button>
          </div>
        </form>
      </div>
    </div>
  </c:if>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
