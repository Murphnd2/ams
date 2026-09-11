<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- S47-C -- compose-then-send for the census request link (T231 build 1, D45 decision d).
     Layout copied from sales/sendProposal.jsp: standalone page, Bootstrap 5, CKEditor 4 body.
     The {link} placeholder in the body is replaced with the real /census-drop/{token} URL on
     Send; everything else in the body is sent as edited. --%>
<!DOCTYPE html>
<html>
<head>
  <title>Request Census</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
  <script src="https://cdn.ckeditor.com/4.22.1/standard/ckeditor.js"></script>
</head>
<body class="bg-light">
<div class="container py-4" style="max-width: 900px;">

  <%-- Header --%>
  <div class="d-flex justify-content-between align-items-center mb-4">
    <div>
      <h4 class="mb-1">Request census</h4>
      <span class="text-muted"><c:out value="${employerName}"/></span>
    </div>
    <a href="${pageContext.request.contextPath}/ViewActivity25" class="btn btn-outline-secondary btn-sm">
      <i class="bi bi-arrow-left me-1"></i>Back to Setup
    </a>
  </div>

  <%-- Current state --%>
  <div class="card mb-3">
    <div class="card-body py-2">
      <c:choose>
        <c:when test="${not status.hasRequest()}">
          <span class="text-muted small">No census request has been sent for this employer yet.</span>
        </c:when>
        <c:otherwise>
          <div class="d-flex flex-wrap gap-2 align-items-center">
            <span class="text-muted small">Current request:</span>
            <c:choose>
              <c:when test="${status.isActive()}">
                <span class="badge bg-success">Open</span>
                <span class="small">Requested ${requestedAtDisplay} &middot; link expires ${expiresAtDisplay}</span>
              </c:when>
              <c:when test="${status.isExpired()}">
                <span class="badge bg-warning text-dark">Expired</span>
                <span class="small">Link expired ${expiresAtDisplay}</span>
              </c:when>
              <c:when test="${status.requestState == 'LOADED'}">
                <span class="badge bg-primary">Loaded</span>
                <span class="small">Loaded ${closedAtDisplay}</span>
              </c:when>
              <c:otherwise>
                <span class="badge bg-secondary">Revoked</span>
                <span class="small">Revoked ${closedAtDisplay}</span>
              </c:otherwise>
            </c:choose>
            <c:if test="${status.hasSubmission()}">
              <span class="small text-muted">&middot; Upload ${submissionAtDisplay}:
                <c:choose>
                  <c:when test="${status.latestSubmission.state == 'UNREADABLE'}">unreadable</c:when>
                  <c:otherwise>${status.latestSubmission.rowCount} rows, ${status.latestSubmission.issueCount} issues</c:otherwise>
                </c:choose>
              </span>
            </c:if>
          </div>
          <c:if test="${status.isOpen()}">
            <div class="d-flex flex-wrap gap-2 align-items-center mt-2">
              <span class="text-muted small">Link:</span>
              <code class="small user-select-all"><c:out value="${existingLink}"/></code>
              <form method="post" action="${pageContext.request.contextPath}/CensusRequest" class="ms-auto"
                    onsubmit="return confirm('Revoke this census link? The client will no longer be able to upload through it, and any upload awaiting review is discarded.');">
                <input type="hidden" name="proposalId" value="${proposalId}">
                <input type="hidden" name="action" value="revoke">
                <button type="submit" class="btn btn-outline-danger btn-sm"><i class="bi bi-x-circle me-1"></i>Revoke</button>
              </form>
            </div>
          </c:if>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <form method="post" action="${pageContext.request.contextPath}/CensusRequest" id="sendForm">
    <input type="hidden" name="proposalId" value="${proposalId}">
    <input type="hidden" name="action" value="send">

    <%-- Recipients --%>
    <div class="card mb-3">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-semibold">Recipients</h5>
      </div>
      <div class="card-body">
        <div class="mb-3">
          <label for="toEmail" class="form-label">To</label>
          <input type="email" class="form-control" name="toEmail" id="toEmail" value="<c:out value='${toEmail}'/>" required>
        </div>
        <div class="mb-3">
          <label for="ccEmail" class="form-label">CC <span class="text-muted small">(separate multiple with commas)</span></label>
          <input type="text" class="form-control" name="ccEmail" id="ccEmail" placeholder="agent@example.com, other@example.com">
        </div>
        <div class="form-check">
          <input type="checkbox" class="form-check-input" name="copyMe" id="copyMe">
          <label class="form-check-label" for="copyMe">Send me a copy (<c:out value="${senderEmail}"/>)</label>
        </div>
      </div>
    </div>

    <%-- Subject & Body --%>
    <div class="card mb-3">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-semibold">Message</h5>
      </div>
      <div class="card-body">
        <div class="mb-3">
          <label for="subject" class="form-label">Subject</label>
          <input type="text" class="form-control" name="subject" id="subject" value="<c:out value='${defaultSubject}'/>">
        </div>
        <div class="mb-3">
          <label for="body" class="form-label">Body</label>
          <textarea name="body" id="body">${defaultBody}</textarea>
          <div class="form-text">
            <code>{link}</code> is replaced with the secure upload link when you press Send.
            <c:if test="${status.isActive()}">This request is still open, so the existing link is renewed for another 30 days rather than replaced.</c:if>
            Required columns: <c:out value="${requiredLabelsText}"/>.
            Optional: <c:out value="${optionalLabelsText}"/>.
          </div>
        </div>
      </div>
    </div>

    <%-- Actions --%>
    <div class="d-flex gap-2 mb-5">
      <button type="submit" class="btn btn-primary btn-lg" id="btnSend">
        <i class="bi bi-send me-1"></i>Send Request
      </button>
      <a href="${pageContext.request.contextPath}/ViewActivity25" class="btn btn-outline-secondary btn-lg">Cancel</a>
    </div>
  </form>
</div>

<script>
  CKEDITOR.replace('body', {
    height: 340,
    removePlugins: 'elementspath',
    toolbar: [
      { name: 'basicstyles', items: ['Bold', 'Italic', 'Underline'] },
      { name: 'paragraph', items: ['NumberedList', 'BulletedList'] },
      { name: 'links', items: ['Link', 'Unlink'] },
      { name: 'tools', items: ['Source'] }
    ]
  });

  document.getElementById('sendForm').addEventListener('submit', function(e) {
    // Update textarea from CKEditor before submit
    for (var instance in CKEDITOR.instances) {
      CKEDITOR.instances[instance].updateElement();
    }
    var btn = document.getElementById('btnSend');
    btn.disabled = true;
    btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Sending...';
  });
</script>
</body>
</html>
