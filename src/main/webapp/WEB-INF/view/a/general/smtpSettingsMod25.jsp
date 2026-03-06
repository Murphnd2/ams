<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- PSP Settings Modal — tabbed: Email | Features. Included in navbar25.jsp --%>
<div class="modal fade" id="pspSettingsMod" role="dialog" tabindex="-1" aria-labelledby="pspSettingsLabel" aria-hidden="true">
  <div class="modal-dialog modal-md modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <form method="post" action="UpdatePspSettings">
        <div class="modal-header py-2" style="background: linear-gradient(135deg, #0d5681, #0a4468); color: white;">
          <h6 class="modal-title m-0" id="pspSettingsLabel">
            <i class="bi bi-gear me-1"></i>Settings
          </h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <div id="settingsLoading" class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
            <span class="ms-2 text-muted" style="font-size:0.82rem;">Loading settings...</span>
          </div>
          <div id="settingsError" class="alert alert-danger py-2 mb-0" style="display:none; font-size:0.82rem;">
            <i class="bi bi-exclamation-triangle me-1"></i>Failed to load settings.
          </div>

          <div id="settingsFields" style="display:none;">
            <%-- ═══ TABS ═══ --%>
            <ul class="nav nav-tabs nav-fill mb-3" role="tablist" style="font-size:0.82rem;">
              <li class="nav-item" role="presentation">
                <button class="nav-link active" id="tab-email" data-bs-toggle="tab" data-bs-target="#pane-email"
                        type="button" role="tab" aria-controls="pane-email" aria-selected="true">
                  <i class="bi bi-envelope-gear me-1"></i>Email
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button class="nav-link" id="tab-features" data-bs-toggle="tab" data-bs-target="#pane-features"
                        type="button" role="tab" aria-controls="pane-features" aria-selected="false">
                  <i class="bi bi-toggles me-1"></i>Features
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button class="nav-link" id="tab-tools" data-bs-toggle="tab" data-bs-target="#pane-tools"
                        type="button" role="tab" aria-controls="pane-tools" aria-selected="false">
                  <i class="bi bi-tools me-1"></i>Tools
                </button>
              </li>
            </ul>

            <div class="tab-content">
              <%-- ═══ EMAIL TAB ═══ --%>
              <div class="tab-pane fade show active" id="pane-email" role="tabpanel" aria-labelledby="tab-email">
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
                    <button class="btn btn-outline-secondary" type="button" onclick="toggleSettingsPw()">
                      <i class="bi bi-eye" id="settingsPwIcon"></i>
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

              <%-- ═══ FEATURES TAB ═══ --%>
              <div class="tab-pane fade" id="pane-features" role="tabpanel" aria-labelledby="tab-features">
                <div class="mb-3">
                  <div class="d-flex align-items-center justify-content-between p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <div>
                      <div class="fw-semibold" style="font-size:0.85rem;"><i class="bi bi-clock me-1"></i>Use Timeclock</div>
                      <div class="text-muted" style="font-size:0.75rem;">Show timeclock on home page. When off, a quick Log Ticket form replaces it.</div>
                    </div>
                    <div class="form-check form-switch ms-3">
                      <input class="form-check-input" type="checkbox" role="switch" name="useTimeclock" id="useTimeclock" style="width:2.5em; height:1.25em;">
                    </div>
                  </div>
                </div>
                <div class="mb-3">
                  <div class="d-flex align-items-center justify-content-between p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <div>
                      <div class="fw-semibold" style="font-size:0.85rem;"><i class="bi bi-telephone me-1"></i>Days Until Contact Alert</div>
                      <div class="text-muted" style="font-size:0.75rem;">Activities with no outbound note for this many days are flagged. Set to 99 to disable.</div>
                    </div>
                    <input type="number" name="daysSinceWarning" id="daysSinceWarning" class="form-control form-control-sm ms-3"
                           min="0" max="99" style="width:60px; text-align:center;">
                  </div>
                </div>
                <div class="mb-3">
                  <div class="d-flex align-items-center justify-content-between p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <div>
                      <div class="fw-semibold" style="font-size:0.85rem;"><i class="bi bi-tag me-1"></i>Use Friendly Names</div>
                      <div class="text-muted" style="font-size:0.75rem;">Show descriptive labels in Admin menu. When off, shows formal names.</div>
                    </div>
                    <div class="form-check form-switch ms-3">
                      <input class="form-check-input" type="checkbox" role="switch" name="useFriendlyNames" id="useFriendlyNames" style="width:2.5em; height:1.25em;">
                    </div>
                  </div>
                </div>
              </div>

              <%-- ═══ TOOLS TAB ═══ --%>
              <div class="tab-pane fade" id="pane-tools" role="tabpanel" aria-labelledby="tab-tools">
                <div class="text-muted mb-3" style="font-size:0.78rem;">Quick access to administration tools.</div>

                <a href="BillingAction" class="d-flex align-items-center justify-content-between p-3 rounded mb-2 text-decoration-none"
                   style="background:#f8f9fb; border:1px solid #dee2e6; color:inherit;"
                   onclick="bootstrap.Modal.getInstance(document.getElementById('pspSettingsMod')).hide();">
                  <div>
                    <div class="fw-semibold" style="font-size:0.85rem; color:#0d5681;"><i class="bi bi-currency-dollar me-1"></i>Billing</div>
                    <div class="text-muted" style="font-size:0.75rem;">Monthly billing views and invoicing</div>
                  </div>
                  <i class="bi bi-chevron-right text-muted"></i>
                </a>

                <a href="UploadPspBranding" class="d-flex align-items-center justify-content-between p-3 rounded mb-2 text-decoration-none"
                   style="background:#f8f9fb; border:1px solid #dee2e6; color:inherit;"
                   onclick="bootstrap.Modal.getInstance(document.getElementById('pspSettingsMod')).hide();">
                  <div>
                    <div class="fw-semibold" style="font-size:0.85rem; color:#0d5681;"><i class="bi bi-palette me-1"></i>Branding</div>
                    <div class="text-muted" style="font-size:0.75rem;">Logo and favicon customization</div>
                  </div>
                  <i class="bi bi-chevron-right text-muted"></i>
                </a>

                <a href="SummitImport" class="d-flex align-items-center justify-content-between p-3 rounded mb-2 text-decoration-none"
                   style="background:#f8f9fb; border:1px solid #dee2e6; color:inherit;"
                   onclick="bootstrap.Modal.getInstance(document.getElementById('pspSettingsMod')).hide();">
                  <div>
                    <div class="fw-semibold" style="font-size:0.85rem; color:#0d5681;"><i class="bi bi-cloud-upload me-1"></i>Import Data</div>
                    <div class="text-muted" style="font-size:0.75rem;">Summit data import wizard</div>
                  </div>
                  <i class="bi bi-chevron-right text-muted"></i>
                </a>

                <a href="BenefitAudit" class="d-flex align-items-center justify-content-between p-3 rounded mb-2 text-decoration-none"
                   style="background:#f8f9fb; border:1px solid #dee2e6; color:inherit;"
                   onclick="bootstrap.Modal.getInstance(document.getElementById('pspSettingsMod')).hide();">
                  <div>
                    <div class="fw-semibold" style="font-size:0.85rem; color:#0d5681;"><i class="bi bi-calendar-check me-1"></i>Benefit Audit</div>
                    <div class="text-muted" style="font-size:0.75rem;">Benefit renewal date review</div>
                  </div>
                  <i class="bi bi-chevron-right text-muted"></i>
                </a>

                <a href="ProposalSettings" class="d-flex align-items-center justify-content-between p-3 rounded mb-2 text-decoration-none"
                   style="background:#f8f9fb; border:1px solid #dee2e6; color:inherit;"
                   onclick="bootstrap.Modal.getInstance(document.getElementById('pspSettingsMod')).hide();">
                  <div>
                    <div class="fw-semibold" style="font-size:0.85rem; color:#0d5681;"><i class="bi bi-file-earmark-richtext me-1"></i>Proposal Settings</div>
                    <div class="text-muted" style="font-size:0.75rem;">Proposal template and section editor</div>
                  </div>
                  <i class="bi bi-chevron-right text-muted"></i>
                </a>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-center border-0 py-2">
          <button type="submit" class="ssa-action save" id="settingsSaveBtn" disabled>
            <i class="bi bi-check-lg me-1"></i>Save
          </button>
          <span class="ssa-action-sep">|</span>
          <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
        </div>
      </form>
    </div>
  </div>
</div>
<script>
  // Load settings when modal opens
  document.getElementById('pspSettingsMod').addEventListener('show.bs.modal', function () {
    document.getElementById('settingsLoading').style.display = '';
    document.getElementById('settingsFields').style.display = 'none';
    document.getElementById('settingsError').style.display = 'none';
    document.getElementById('settingsSaveBtn').disabled = true;

    fetch('UpdatePspSettings')
      .then(function(r) { return r.json(); })
      .then(function(data) {
        document.getElementById('smtpServer').value = data.SMTP_SERVER || '';
        document.getElementById('smtpPort').value = data.SMTP_PORT || '';
        document.getElementById('smtpUser').value = data.SMTP_USER || '';
        document.getElementById('smtpPassword').value = data.SMTP_PASSWORD || '';
        document.getElementById('smtpFrom').value = data.SMTP_FROM || '';
        document.getElementById('emailFooterText').value = data.EMAIL_FOOTER_TEXT || '';
        document.getElementById('useTimeclock').checked = (data.USE_TIMECLOCK !== 'false');
        document.getElementById('useFriendlyNames').checked = (data.USE_FRIENDLY_NAMES !== 'false');
        document.getElementById('daysSinceWarning').value = data.DAYS_SINCE_WARNING || '7';
        document.getElementById('settingsLoading').style.display = 'none';
        document.getElementById('settingsFields').style.display = '';
        document.getElementById('settingsSaveBtn').disabled = false;
      })
      .catch(function() {
        document.getElementById('settingsLoading').style.display = 'none';
        document.getElementById('settingsError').style.display = '';
      });
  });

  // Toggle password visibility
  function toggleSettingsPw() {
    var pw = document.getElementById('smtpPassword');
    var icon = document.getElementById('settingsPwIcon');
    if (pw.type === 'password') {
      pw.type = 'text';
      icon.className = 'bi bi-eye-slash';
    } else {
      pw.type = 'password';
      icon.className = 'bi bi-eye';
    }
  }
</script>
