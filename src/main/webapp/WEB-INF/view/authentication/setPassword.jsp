<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Set Your Password</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
  <style>
    body { background-color: #f4f5f7; font-family: Arial, Helvetica, sans-serif; }
    .brand-bar { background: #0d5681; padding: 1rem 0; }
    .brand-bar img { height: 36px; }
    .card-container { max-width: 460px; margin: 60px auto; }
    .btn-ssa { background-color: #0d5681; border-color: #0d5681; color: white; }
    .btn-ssa:hover { background-color: #094568; border-color: #094568; color: white; }
  </style>
</head>
<body>

  <%-- Simple brand bar — no navbar, no session needed --%>
  <div class="brand-bar text-center">
    <img src="${pageContext.request.contextPath}/images/logo1.png" alt="Logo"
         onerror="this.style.display='none'">
  </div>

  <div class="card-container">

    <%-- ═══ ERROR STATE — link expired, used, or invalid ═══ --%>
    <c:if test="${not empty linkError}">
      <div class="card shadow-sm border-0">
        <div class="card-body text-center py-5">
          <i class="bi bi-exclamation-triangle text-warning" style="font-size: 3rem;"></i>
          <h5 class="mt-3 mb-2">Unable to Continue</h5>
          <p class="text-muted">${linkError}</p>
          <a href="${pageContext.request.contextPath}/NeedsHelp" class="btn btn-ssa mt-2">
            <i class="bi bi-envelope me-1"></i>Request a New Link
          </a>
          <div class="mt-3">
            <a href="${pageContext.request.contextPath}/index.jsp" class="text-muted" style="font-size: 0.85rem;">
              Back to Login
            </a>
          </div>
        </div>
      </div>
    </c:if>

    <%-- ═══ SET PASSWORD FORM — tempUser exists in session ═══ --%>
    <c:if test="${empty linkError && not empty sessionScope.tempUser}">
      <div class="card shadow-sm border-0">
        <div class="card-header text-center py-3" style="background: #0d5681; color: white;">
          <h5 class="m-0"><i class="bi bi-shield-lock me-2"></i>Set Your Password</h5>
        </div>
        <div class="card-body p-4">

          <c:if test="${not empty formError}">
            <div class="alert alert-danger py-2" style="font-size: 0.85rem;">
              <i class="bi bi-exclamation-triangle me-1"></i>${formError}
            </div>
          </c:if>

          <p class="text-muted mb-3" style="font-size: 0.85rem;">
            Welcome! Please choose a password for your account.
          </p>

          <form method="post" action="ResetLogin">
            <div class="mb-3">
              <label for="newPassword1" class="form-label fw-semibold" style="font-size: 0.85rem;">
                New Password
              </label>
              <input type="password" class="form-control" id="newPassword1" name="newPassword1"
                     required minlength="8" placeholder="At least 8 characters">
            </div>
            <div class="mb-3">
              <label for="newPassword2" class="form-label fw-semibold" style="font-size: 0.85rem;">
                Confirm Password
              </label>
              <input type="password" class="form-control" id="newPassword2" name="newPassword2"
                     required minlength="8" placeholder="Re-enter your password">
            </div>
            <button type="submit" class="btn btn-ssa w-100">
              <i class="bi bi-check-circle me-1"></i>Set Password & Continue
            </button>
          </form>

          <div class="mt-3 text-center">
            <a href="${pageContext.request.contextPath}/index.jsp" class="text-muted" style="font-size: 0.85rem;">
              Back to Login
            </a>
          </div>
        </div>
      </div>
    </c:if>

    <%-- ═══ FALLBACK — no error and no tempUser (direct navigation) ═══ --%>
    <c:if test="${empty linkError && empty sessionScope.tempUser}">
      <div class="card shadow-sm border-0">
        <div class="card-body text-center py-5">
          <i class="bi bi-link-45deg text-muted" style="font-size: 3rem;"></i>
          <h5 class="mt-3 mb-2">No Active Session</h5>
          <p class="text-muted">Please use the link from your email to set your password.</p>
          <a href="${pageContext.request.contextPath}/index.jsp" class="btn btn-ssa mt-2">
            Back to Login
          </a>
        </div>
      </div>
    </c:if>

  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
