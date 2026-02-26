<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- SMTP Settings Modal — included in navbar25.jsp, triggered from Admin dropdown --%>
<div class="modal fade" id="smtpSettingsMod" role="dialog" tabindex="-1" aria-labelledby="smtpSettingsLabel" aria-hidden="true">
  <div class="modal-dialog modal-md modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <form method="post" action="UpdateSmtpSettings">
        <div class="modal-header py-2" style="background: linear-gradient(135deg, #0d5681, #0a4468); color: white;">
          <h6 class="modal-title m-0" id="smtpSettingsLabel">
            <i class="bi bi-envelope-gear me-1"></i>Email Settings
          </h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <div id="smtpLoading" class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
            <span class="ms-2 text-muted" style="font-size:0.82rem;">Loading settings...</span>
          </div>
          <div id="smtpFields" style="display:none;">
            <div class="mb-3">
              <label class="form-label fw-semibold" style="font-size:0.82rem;">SMTP Server</label>
              <input type="text" name="smtpServer" id="smtpServer" class="form-control form-control-sm" required placeholder="mail.smtp2go.com">
            </div>
            <div class="mb-3">
              <label class="form-label fw-semibold" style="font-size:0.82rem;">SMTP Port</label>
              <input type="text" name="smtpPort" id="smtpPort" class="form-control form-control-sm" required placeholder="2525">
            </div>
            <div class="mb-3">
              <label class="form-label fw-semibold" style="font-size:0.82rem;">SMTP User</label>
              <input type="text" name="smtpUser" id="smtpUser" class="form-control form-control-sm" required>
            </div>
            <div class="mb-3">
              <label class="form-label fw-semibold" style="font-size:0.82rem;">SMTP Password</label>
              <div class="input-group input-group-sm">
                <input type="password" name="smtpPassword" id="smtpPassword" class="form-control form-control-sm" required>
                <button class="btn btn-outline-secondary" type="button" onclick="toggleSmtpPw()">
                  <i class="bi bi-eye" id="smtpPwIcon"></i>
                </button>
              </div>
            </div>
            <div class="mb-2">
              <label class="form-label fw-semibold" style="font-size:0.82rem;">From Address <span class="text-muted fw-normal">(optional)</span></label>
              <input type="email" name="smtpFrom" id="smtpFrom" class="form-control form-control-sm" placeholder="noreply@yourdomain.com">
            </div>
            <hr class="my-3">
            <div class="mb-2">
              <label class="form-label fw-semibold" style="font-size:0.82rem;">Email Footer Text</label>
              <input type="text" name="emailFooterText" id="emailFooterText" class="form-control form-control-sm" placeholder="Your Company · Benefits Administration Services">
              <div class="form-text" style="font-size:0.75rem;">Appears at the bottom of all outbound emails.</div>
            </div>
          </div>
          <div id="smtpError" class="alert alert-danger py-2 mb-0" style="display:none; font-size:0.82rem;">
            <i class="bi bi-exclamation-triangle me-1"></i>Failed to load settings.
          </div>
        </div>
        <div class="modal-footer py-2">
          <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-sm btn-ssa" id="smtpSaveBtn" disabled>
            <i class="bi bi-save me-1"></i>Save
          </button>
        </div>
      </form>
    </div>
  </div>
</div>
<script>
  // Load SMTP values when modal opens
  document.getElementById('smtpSettingsMod').addEventListener('show.bs.modal', function () {
    document.getElementById('smtpLoading').style.display = '';
    document.getElementById('smtpFields').style.display = 'none';
    document.getElementById('smtpError').style.display = 'none';
    document.getElementById('smtpSaveBtn').disabled = true;

    fetch('UpdateSmtpSettings')
      .then(function(r) { return r.json(); })
      .then(function(data) {
        document.getElementById('smtpServer').value = data.SMTP_SERVER || '';
        document.getElementById('smtpPort').value = data.SMTP_PORT || '';
        document.getElementById('smtpUser').value = data.SMTP_USER || '';
        document.getElementById('smtpPassword').value = data.SMTP_PASSWORD || '';
        document.getElementById('smtpFrom').value = data.SMTP_FROM || '';
        document.getElementById('emailFooterText').value = data.EMAIL_FOOTER_TEXT || '';
        document.getElementById('smtpLoading').style.display = 'none';
        document.getElementById('smtpFields').style.display = '';
        document.getElementById('smtpSaveBtn').disabled = false;
      })
      .catch(function() {
        document.getElementById('smtpLoading').style.display = 'none';
        document.getElementById('smtpError').style.display = '';
      });
  });

  // Toggle password visibility
  function toggleSmtpPw() {
    var pw = document.getElementById('smtpPassword');
    var icon = document.getElementById('smtpPwIcon');
    if (pw.type === 'password') {
      pw.type = 'text';
      icon.className = 'bi bi-eye-slash';
    } else {
      pw.type = 'password';
      icon.className = 'bi bi-eye';
    }
  }
</script>
