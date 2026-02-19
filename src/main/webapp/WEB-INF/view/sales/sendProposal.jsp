<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
  <title>Send Proposal</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
  <script src="https://cdn.ckeditor.com/4.22.1/standard/ckeditor.js"></script>
</head>
<body class="bg-light">
<div class="container py-4" style="max-width: 900px;">

  <%-- Header --%>
  <div class="d-flex justify-content-between align-items-center mb-4">
    <div>
      <h4 class="mb-1">Send Proposal #${proposal.getId()}</h4>
      <span class="text-muted">${proposal.getProspect().getName()} &middot; ${proposal.getRate().getDescription()}</span>
    </div>
    <a href="ProposalDetail?id=${proposal.getId()}" class="btn btn-outline-secondary btn-sm">
      <i class="bi bi-arrow-left me-1"></i>Back to Detail
    </a>
  </div>

  <%-- Proposal Summary --%>
  <div class="card mb-3">
    <div class="card-body py-2">
      <div class="d-flex flex-wrap gap-2 align-items-center">
        <span class="text-muted small">Lines of Service:</span>
        <c:forEach var="los" items="${proposal.getLosList()}">
          <span class="badge bg-primary">${los.getDescription()}</span>
        </c:forEach>
        <span class="text-muted small ms-3">Link:</span>
        <code class="small">${proposalLink}</code>
      </div>
    </div>
  </div>

  <form method="post" action="SendProposal" id="sendForm">
    <input type="hidden" name="id" value="${proposal.getId()}">

    <%-- Recipients --%>
    <div class="card mb-3">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0 fw-semibold">Recipients</h5>
      </div>
      <div class="card-body">
        <div class="mb-3">
          <label for="toEmail" class="form-label">To</label>
          <input type="email" class="form-control" name="toEmail" id="toEmail" value="${toEmail}" required>
        </div>
        <div class="mb-3">
          <label for="ccEmail" class="form-label">CC <span class="text-muted small">(separate multiple with commas)</span></label>
          <input type="text" class="form-control" name="ccEmail" id="ccEmail" placeholder="agent@example.com, other@example.com">
        </div>
        <div class="form-check">
          <input type="checkbox" class="form-check-input" name="copyMe" id="copyMe">
          <label class="form-check-label" for="copyMe">Send me a copy (${senderEmail})</label>
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
          <input type="text" class="form-control" name="subject" id="subject"
                 value="Your Benefits Proposal — ${proposal.getProspect().getName()}">
        </div>
        <div class="mb-3">
          <label for="body" class="form-label">Body</label>
          <textarea name="body" id="body">${defaultBody}</textarea>
        </div>
      </div>
    </div>

    <%-- Actions --%>
    <div class="d-flex gap-2 mb-5">
      <button type="submit" class="btn btn-primary btn-lg" id="btnSend">
        <i class="bi bi-send me-1"></i>Send Proposal
      </button>
      <a href="ProposalDetail?id=${proposal.getId()}" class="btn btn-outline-secondary btn-lg">Cancel</a>
    </div>
  </form>
</div>

<script>
  CKEDITOR.replace('body', {
    height: 300,
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
