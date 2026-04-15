<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <link href="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.snow.css" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.js"></script>
  <title>${applicationScope.global.getPsp().getFullName()} — Email Preview</title>
  <style>
    .recipient-chip {
      display: inline-flex;
      align-items: center;
      background: #e9ecef;
      border-radius: 20px;
      padding: 0.2rem 0.7rem;
      margin: 0.15rem;
      font-size: 0.82rem;
    }
    #bodyQuill { min-height: 320px; background: white; }
    .ql-toolbar.ql-snow { border-radius: 4px 4px 0 0; }
    .ql-container.ql-snow { border-radius: 0 0 4px 4px; }
    .attach-note {
      font-size: 0.78rem;
      color: #6c757d;
      background: #f8f9fa;
      border-left: 3px solid #dee2e6;
      padding: 0.5rem 0.75rem;
      border-radius: 3px;
    }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

  <div class="row mt-3 mb-4">
    <div class="col-xl-9 offset-xl-1 col-lg-10 offset-lg-1">

      <div class="card border-0 shadow-sm">
        <div class="hdr-bar d-flex justify-content-between align-items-center">
          <span><i class="bi bi-lightning-charge-fill me-2" style="color:#fd7e14;"></i>Email Preview — ${fn:escapeXml(sessionScope.a1autoName)}</span>
          <span class="small opacity-75"><i class="bi bi-2-circle me-1"></i>Review &amp; send</span>
        </div>
        <div class="card-body">

          <form method="post" action="SendAutoFinal25" id="autoPreviewForm">
            <input type="hidden" name="csrf" value="${sessionScope.csrfToken}" />
            <input type="hidden" name="sendAutoEmail" value="1" />
            <input type="hidden" name="fromPreview" value="true" />
            <input type="hidden" name="previewBody" id="previewBody" value="" />

            <%-- ===== TO (read-only) ===== --%>
            <div class="mb-3">
              <label class="form-label text-ssa fw-bold" style="font-size:0.85rem;">
                <i class="bi bi-people-fill me-1"></i>To
              </label>
              <div class="d-flex flex-wrap align-items-center border rounded p-2" style="min-height: 42px; background:#f8f9fa;">
                <c:choose>
                  <c:when test="${empty sessionScope.a1recipientList}">
                    <span class="text-muted fst-italic" style="font-size: 0.85rem;">
                      <i class="bi bi-exclamation-triangle me-1 text-warning"></i>No recipients — email cannot be sent without a valid address
                    </span>
                  </c:when>
                  <c:otherwise>
                    <c:forEach var="r" items="${sessionScope.a1recipientList}">
                      <span class="recipient-chip">
                        <i class="bi bi-envelope me-1" style="font-size:0.75rem;"></i>
                        <c:choose>
                          <c:when test="${not empty r.getFirstName() && r.getFirstName() ne 'NEW'}">
                            ${fn:escapeXml(r.getFirstName())} ${fn:escapeXml(r.getLastName())} &lt;${fn:escapeXml(r.getEmail())}&gt;
                          </c:when>
                          <c:otherwise>
                            ${fn:escapeXml(r.getEmail())}
                          </c:otherwise>
                        </c:choose>
                      </span>
                    </c:forEach>
                  </c:otherwise>
                </c:choose>
              </div>
            </div>

            <%-- ===== CC ===== --%>
            <div class="mb-3">
              <label class="form-label text-ssa fw-bold" for="previewCc" style="font-size:0.85rem;">
                <i class="bi bi-person-plus me-1"></i>CC <span class="text-muted fw-normal">(separate multiple addresses with semicolons)</span>
              </label>
              <input type="text" class="form-control" id="previewCc" name="previewCc"
                     placeholder="jane@example.com; john@example.com">
            </div>

            <%-- ===== SUBJECT ===== --%>
            <div class="mb-3">
              <label class="form-label text-ssa fw-bold" for="previewSubject" style="font-size:0.85rem;">
                <i class="bi bi-chat-left-text me-1"></i>Subject
              </label>
              <input type="text" class="form-control" id="previewSubject" name="previewSubject"
                     value="${fn:escapeXml(sessionScope.a1resolvedSubject)}" required>
            </div>

            <%-- ===== BODY (Quill) ===== --%>
            <div class="mb-3">
              <label class="form-label text-ssa fw-bold" style="font-size:0.85rem;">
                <i class="bi bi-body-text me-1"></i>Message
              </label>
              <div id="bodyQuill">${sessionScope.a1resolvedBody}</div>
            </div>

            <%-- ===== ATTACHMENTS (note only) ===== --%>
            <div class="mb-3">
              <div class="attach-note">
                <i class="bi bi-paperclip me-1"></i>Need to add attachments? Use the full email composer on the activity page.
              </div>
            </div>

            <%-- ===== OPTIONS ===== --%>
            <div class="mb-3">
              <div class="form-check">
                <input class="form-check-input" type="checkbox"
                       id="previewAutoClose" name="previewAutoClose" value="true"
                       <c:if test="${sessionScope.a1shouldClose == true}">checked</c:if>>
                <label class="form-check-label" for="previewAutoClose" style="font-size:0.85rem;">
                  Mark this task complete after sending
                </label>
              </div>
            </div>

            <%-- ===== ACTIONS ===== --%>
            <div class="d-flex justify-content-end gap-2 pt-2 border-top">
              <a class="btn btn-outline-ssa mt-3" href="ViewActivity25">
                <i class="bi bi-x-lg me-1"></i>Cancel
              </a>
              <button type="submit" class="btn btn-ssa mt-3" id="btnSendPreview">
                <i class="bi bi-send me-1"></i>Send Email
              </button>
            </div>
          </form>

        </div>
      </div>

    </div>
  </div>
</div>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    var target = document.getElementById('bodyQuill');
    if (!target) return;

    var quill = new Quill(target, {
      theme: 'snow',
      modules: {
        toolbar: [
          ['bold', 'italic', 'underline'],
          [{ 'list': 'ordered' }, { 'list': 'bullet' }],
          ['link'],
          ['clean']
        ]
      }
    });

    var form = document.getElementById('autoPreviewForm');
    var btn = document.getElementById('btnSendPreview');

    form.addEventListener('submit', function() {
      // Sync Quill content to hidden input
      var html = quill.root.innerHTML;
      if (html === '<p><br></p>') html = '';
      document.getElementById('previewBody').value = html;
      // Disable button AFTER the submit event fires to avoid blocking submission
      setTimeout(function() {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>Sending\u2026';
      }, 50);
    });
  });
</script>

</body>
</html>
