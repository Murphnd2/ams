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
                <%-- Custom Landing Page toggle --%>
                <div class="mb-3">
                  <div class="d-flex align-items-center justify-content-between p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <div>
                      <div class="fw-semibold" style="font-size:0.85rem;"><i class="bi bi-house-door me-1"></i>Custom Landing Page</div>
                      <div class="text-muted" style="font-size:0.75rem;">Show a branded landing page instead of the plain login screen.</div>
                    </div>
                    <div class="form-check form-switch ms-3">
                      <input class="form-check-input" type="checkbox" role="switch" name="useCustomLanding" id="useCustomLanding" style="width:2.5em; height:1.25em;"
                             onchange="document.getElementById('landingEditorPanel').style.display = this.checked ? '' : 'none';">
                    </div>
                  </div>
                </div>
                <%-- Landing page settings (collapsed when toggle is off) --%>
                <div id="landingEditorPanel" style="display:none;" class="mb-3">
                  <%-- Header color pickers --%>
                  <div class="p-3 rounded mb-2" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <label class="form-label fw-semibold m-0 mb-2" style="font-size:0.82rem;"><i class="bi bi-palette me-1"></i>Header Bar Colors</label>
                    <div class="row g-2">
                      <div class="col-6">
                        <label class="form-label text-muted m-0" style="font-size:0.75rem;">Background</label>
                        <div class="d-flex align-items-center gap-2">
                          <input type="color" name="landingHeaderColor" id="landingHeaderColor" value="#0d5681"
                                 class="form-control form-control-sm p-0 border-0" style="width:36px; height:36px; cursor:pointer;">
                          <input type="text" id="landingHeaderColorHex" class="form-control form-control-sm" style="width:90px; font-size:0.78rem; font-family:monospace;"
                                 value="#0d5681" maxlength="7"
                                 oninput="var v=this.value; if(/^#[0-9a-fA-F]{6}$/.test(v)) document.getElementById('landingHeaderColor').value=v;"
                                 onchange="var v=this.value; if(/^#[0-9a-fA-F]{6}$/.test(v)) document.getElementById('landingHeaderColor').value=v;">
                        </div>
                      </div>
                      <div class="col-6">
                        <label class="form-label text-muted m-0" style="font-size:0.75rem;">Text / Button</label>
                        <div class="d-flex align-items-center gap-2">
                          <input type="color" name="landingHeaderTextColor" id="landingHeaderTextColor" value="#ffffff"
                                 class="form-control form-control-sm p-0 border-0" style="width:36px; height:36px; cursor:pointer;">
                          <input type="text" id="landingHeaderTextColorHex" class="form-control form-control-sm" style="width:90px; font-size:0.78rem; font-family:monospace;"
                                 value="#ffffff" maxlength="7"
                                 oninput="var v=this.value; if(/^#[0-9a-fA-F]{6}$/.test(v)) document.getElementById('landingHeaderTextColor').value=v;"
                                 onchange="var v=this.value; if(/^#[0-9a-fA-F]{6}$/.test(v)) document.getElementById('landingHeaderTextColor').value=v;">
                        </div>
                      </div>
                    </div>
                  </div>
                  <%-- Landing page HTML editor --%>
                  <div class="p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                      <label class="form-label fw-semibold m-0" style="font-size:0.82rem;">Landing Page HTML</label>
                      <div>
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="toggleLandingPreview()">
                          <i class="bi bi-eye me-1"></i><span id="landingPreviewLabel">Preview</span>
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-primary ms-1" onclick="saveLandingHtml()">
                          <i class="bi bi-floppy me-1"></i>Save HTML
                        </button>
                      </div>
                    </div>
                    <textarea id="landingHtmlEditor" class="form-control" rows="12"
                              style="font-family: 'Courier New', monospace; font-size:0.78rem; display:block;"
                              placeholder="Paste your landing page HTML here..."></textarea>
                    <iframe id="landingHtmlPreview" style="width:100%; height:300px; border:1px solid #dee2e6; border-radius:4px; display:none; background:#fff;"></iframe>
                    <div id="landingHtmlStatus" class="mt-1" style="font-size:0.75rem;"></div>
                  </div>
                </div>
                <%-- ═══ AI ASSISTANT ═══ --%>
                <hr class="my-3">
                <div class="mb-3">
                  <div class="p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                      <div>
                        <div class="fw-semibold" style="font-size:0.85rem;">
                          <i class="bi bi-robot me-1"></i>AI Assistant
                        </div>
                        <div class="text-muted" style="font-size:0.75rem;">
                          Enter your Anthropic API key to enable AI-powered features.
                          <span id="aiKeySourceBadge"></span>
                        </div>
                      </div>
                      <span id="aiKeyStatus"></span>
                    </div>
                    <div class="input-group input-group-sm mb-2">
                      <input type="password" id="aiApiKeyInput" class="form-control form-control-sm"
                             placeholder="sk-ant-api03-..." autocomplete="off">
                      <button class="btn btn-outline-secondary" type="button" onclick="toggleAiKeyVisibility()">
                        <i class="bi bi-eye" id="aiKeyPwIcon"></i>
                      </button>
                    </div>
                    <div class="d-flex gap-2">
                      <button type="button" class="btn btn-sm btn-outline-primary" id="aiKeySaveBtn" onclick="validateAndSaveAiKey()">
                        <i class="bi bi-check-circle me-1"></i>Validate & Save
                      </button>
                      <button type="button" class="btn btn-sm btn-outline-danger" id="aiKeyRemoveBtn"
                              style="display:none;" onclick="removeAiKey()">
                        <i class="bi bi-trash me-1"></i>Remove Key
                      </button>
                    </div>
                    <div id="aiKeyMessage" class="mt-2" style="font-size:0.75rem;"></div>
                    <hr class="my-2">
                    <div class="form-check form-switch">
                      <input class="form-check-input" type="checkbox" id="chatbotAllUsers" name="chatbotAllUsers">
                      <label class="form-check-label" for="chatbotAllUsers" style="font-size:0.8rem;">
                        Show chatbot to all users
                      </label>
                      <div class="text-muted" style="font-size:0.72rem;">
                        When off, only PSP Admins can see the AI chatbot. Other AI features (proposal builder, automation) are always admin-only.
                      </div>
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
        document.getElementById('useCustomLanding').checked = (data.USE_CUSTOM_LANDING === 'true');
        document.getElementById('landingHtmlEditor').value = data.CUSTOM_LANDING_HTML || '';
        document.getElementById('landingEditorPanel').style.display = (data.USE_CUSTOM_LANDING === 'true') ? '' : 'none';
        var hc = data.LANDING_HEADER_COLOR || '#0d5681';
        var htc = data.LANDING_HEADER_TEXT_COLOR || '#ffffff';
        document.getElementById('landingHeaderColor').value = hc;
        document.getElementById('landingHeaderColorHex').value = hc;
        document.getElementById('landingHeaderTextColor').value = htc;
        document.getElementById('landingHeaderTextColorHex').value = htc;
        document.getElementById('daysSinceWarning').value = data.DAYS_SINCE_WARNING || '7';
        // AI key status
        var aiSource = data.AI_KEY_SOURCE || 'none';
        var aiHint = data.AI_KEY_HINT || '';
        var statusEl = document.getElementById('aiKeyStatus');
        var sourceBadge = document.getElementById('aiKeySourceBadge');
        var removeBtn = document.getElementById('aiKeyRemoveBtn');
        document.getElementById('aiApiKeyInput').value = '';
        document.getElementById('aiApiKeyInput').placeholder =
            aiSource !== 'none' ? 'Current key: ' + aiHint : 'sk-ant-api03-...';
        if (aiSource === 'database') {
            statusEl.innerHTML = '<span class="badge bg-success" style="font-size:0.7rem;">Active</span>';
            sourceBadge.innerHTML = '';
            removeBtn.style.display = '';
        } else if (aiSource === 'properties') {
            statusEl.innerHTML = '<span class="badge bg-info" style="font-size:0.7rem;">Active (config file)</span>';
            sourceBadge.innerHTML = '<br><span style="font-size:0.7rem;" class="text-info">Using server config file.</span>';
            removeBtn.style.display = 'none';
        } else {
            statusEl.innerHTML = '<span class="badge bg-secondary" style="font-size:0.7rem;">Not Configured</span>';
            sourceBadge.innerHTML = '';
            removeBtn.style.display = 'none';
        }
        document.getElementById('aiKeyMessage').innerHTML = '';
        document.getElementById('chatbotAllUsers').checked = (data.CHATBOT_ALL_USERS === 'true');
        document.getElementById('settingsLoading').style.display = 'none';
        document.getElementById('settingsFields').style.display = '';
        document.getElementById('settingsSaveBtn').disabled = false;
      })
      .catch(function() {
        document.getElementById('settingsLoading').style.display = 'none';
        document.getElementById('settingsError').style.display = '';
      });
  });

  // Sync color picker → hex text input
  document.getElementById('landingHeaderColor').addEventListener('input', function() {
    document.getElementById('landingHeaderColorHex').value = this.value;
  });
  document.getElementById('landingHeaderTextColor').addEventListener('input', function() {
    document.getElementById('landingHeaderTextColorHex').value = this.value;
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

  // Landing page HTML preview toggle
  function toggleLandingPreview() {
    var editor = document.getElementById('landingHtmlEditor');
    var preview = document.getElementById('landingHtmlPreview');
    var label = document.getElementById('landingPreviewLabel');
    if (editor.style.display !== 'none') {
      preview.srcdoc = editor.value;
      editor.style.display = 'none';
      preview.style.display = 'block';
      label.textContent = 'Edit HTML';
    } else {
      editor.style.display = 'block';
      preview.style.display = 'none';
      label.textContent = 'Preview';
    }
  }

  // Save landing HTML via AJAX (separate from main form save)
  function saveLandingHtml() {
    var html = document.getElementById('landingHtmlEditor').value;
    var status = document.getElementById('landingHtmlStatus');
    status.innerHTML = '<span class="text-muted"><i class="bi bi-arrow-repeat"></i> Saving...</span>';
    fetch('UpdatePspSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=saveLandingHtml&landingHtml=' + encodeURIComponent(html)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.status === 'ok') {
        status.innerHTML = '<span class="text-success"><i class="bi bi-check-circle"></i> Saved.</span>';
      } else {
        status.innerHTML = '<span class="text-danger"><i class="bi bi-x-circle"></i> Save failed.</span>';
      }
      setTimeout(function() { status.innerHTML = ''; }, 3000);
    })
    .catch(function() {
      status.innerHTML = '<span class="text-danger"><i class="bi bi-x-circle"></i> Save failed.</span>';
    });
  }

  // ── AI Key Management ──

  function toggleAiKeyVisibility() {
    var inp = document.getElementById('aiApiKeyInput');
    var icon = document.getElementById('aiKeyPwIcon');
    if (inp.type === 'password') { inp.type = 'text'; icon.className = 'bi bi-eye-slash'; }
    else { inp.type = 'password'; icon.className = 'bi bi-eye'; }
  }

  function validateAndSaveAiKey() {
    var key = document.getElementById('aiApiKeyInput').value.trim();
    if (!key) { showAiMsg('warning', 'Please enter an API key.'); return; }
    showAiMsg('muted', '<span class="ai-spin-icon"></span> Validating...');
    document.getElementById('aiKeySaveBtn').disabled = true;
    fetch('UpdatePspSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=saveApiKey&apiKey=' + encodeURIComponent(key)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      document.getElementById('aiKeySaveBtn').disabled = false;
      if (data.status === 'ok') {
        showAiMsg('success', '<i class="bi bi-check-circle"></i> Key validated and saved. AI features are now active.');
        document.getElementById('aiKeyStatus').innerHTML =
            '<span class="badge bg-success" style="font-size:0.7rem;">Active</span>';
        document.getElementById('aiApiKeyInput').value = '';
        document.getElementById('aiApiKeyInput').placeholder = 'Current key: ' + data.hint;
        document.getElementById('aiKeyRemoveBtn').style.display = '';
        document.getElementById('aiKeySourceBadge').innerHTML = '';
      } else {
        showAiMsg('danger', '<i class="bi bi-x-circle"></i> ' + (data.message || 'Validation failed.'));
      }
    })
    .catch(function() {
      document.getElementById('aiKeySaveBtn').disabled = false;
      showAiMsg('danger', '<i class="bi bi-x-circle"></i> Request failed.');
    });
  }

  function removeAiKey() {
    if (!confirm('Remove the AI API key? AI features will be disabled.')) return;
    showAiMsg('muted', '<span class="ai-spin-icon"></span> Removing...');
    fetch('UpdatePspSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=removeApiKey'
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.status === 'ok') {
        showAiMsg('success', '<i class="bi bi-check-circle"></i> Key removed. AI features disabled.');
        document.getElementById('aiKeyStatus').innerHTML =
            '<span class="badge bg-secondary" style="font-size:0.7rem;">Not Configured</span>';
        document.getElementById('aiApiKeyInput').placeholder = 'sk-ant-api03-...';
        document.getElementById('aiKeyRemoveBtn').style.display = 'none';
      } else {
        showAiMsg('danger', '<i class="bi bi-x-circle"></i> ' + (data.message || 'Remove failed.'));
      }
    })
    .catch(function() {
      showAiMsg('danger', '<i class="bi bi-x-circle"></i> Request failed.');
    });
  }

  function showAiMsg(cls, html) {
    var el = document.getElementById('aiKeyMessage');
    el.innerHTML = '<span class="text-' + cls + '">' + html + '</span>';
    if (cls === 'success') setTimeout(function() { el.innerHTML = ''; }, 5000);
  }
</script>
<style>
  .ai-spin-icon { display: inline-block; width: 12px; height: 12px; border: 2px solid #6c757d;
    border-top-color: transparent; border-radius: 50%; animation: aiSpin 0.8s linear infinite; }
  @keyframes aiSpin { 100% { transform: rotate(360deg); } }
</style>
