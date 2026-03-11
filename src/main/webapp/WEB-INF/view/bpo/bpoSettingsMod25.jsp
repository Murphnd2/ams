<%@ page contentType="text/html;charset=UTF-8" %>
<%-- BPO Settings Modal — AI Assistant configuration --%>
<div class="modal fade" id="bpoSettingsMod" tabindex="-1" aria-labelledby="bpoSettingsLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header" style="background-color: #0d5681; color: white;">
        <h5 class="modal-title" id="bpoSettingsLabel">
          <i class="bi bi-gear me-2"></i>BPO Settings
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">

        <%-- ═══ AI ASSISTANT ═══ --%>
        <div class="p-3 rounded" style="background:#f8f9fb; border:1px solid #dee2e6;">
          <div class="d-flex align-items-center justify-content-between mb-2">
            <div>
              <div class="fw-semibold" style="font-size:0.85rem;">
                <i class="bi bi-robot me-1"></i>AI Assistant
              </div>
              <div class="text-muted" style="font-size:0.75rem;">
                Enter your Anthropic API key to enable AI-powered features.
                <span id="bpoAiKeySourceBadge"></span>
              </div>
            </div>
            <span id="bpoAiKeyStatus"></span>
          </div>
          <div class="input-group input-group-sm mb-2">
            <input type="password" id="bpoAiApiKeyInput" class="form-control form-control-sm"
                   placeholder="sk-ant-api03-..." autocomplete="off">
            <button class="btn btn-outline-secondary" type="button" onclick="bpoToggleAiKeyVisibility()">
              <i class="bi bi-eye" id="bpoAiKeyPwIcon"></i>
            </button>
          </div>
          <div class="d-flex gap-2">
            <button type="button" class="btn btn-sm btn-outline-primary" id="bpoAiKeySaveBtn" onclick="bpoValidateAndSaveAiKey()">
              <i class="bi bi-check-circle me-1"></i>Validate & Save
            </button>
            <button type="button" class="btn btn-sm btn-outline-danger" id="bpoAiKeyRemoveBtn"
                    style="display:none;" onclick="bpoRemoveAiKey()">
              <i class="bi bi-trash me-1"></i>Remove Key
            </button>
          </div>
          <div id="bpoAiKeyMessage" class="mt-2" style="font-size:0.75rem;"></div>

          <hr class="my-2">
          <div class="form-check form-switch">
            <input class="form-check-input" type="checkbox" id="bpoChatbotAllUsers">
            <label class="form-check-label" for="bpoChatbotAllUsers" style="font-size:0.8rem;">
              Show chatbot to all BPO users
            </label>
            <div class="text-muted" style="font-size:0.72rem;">
              When off, only BPO Admins can see the AI chatbot.
            </div>
          </div>
        </div>

      </div>
      <div class="modal-footer justify-content-center border-0 py-2">
        <button type="button" class="ssa-action save" onclick="bpoSaveSettings()">
          <i class="bi bi-check-lg me-1"></i>Save
        </button>
        <span class="ssa-action-sep">|</span>
        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal">Cancel</button>
      </div>
    </div>
  </div>
</div>

<script>
  // Load current settings when modal opens
  document.getElementById('bpoSettingsMod')?.addEventListener('shown.bs.modal', function() {
    fetch('UpdateBpoSettings')
      .then(function(r) { return r.json(); })
      .then(function(data) {
        // AI key status
        var statusEl = document.getElementById('bpoAiKeyStatus');
        var removeBtn = document.getElementById('bpoAiKeyRemoveBtn');
        var input = document.getElementById('bpoAiApiKeyInput');
        var sourceBadge = document.getElementById('bpoAiKeySourceBadge');

        if (data.AI_KEY_SOURCE === 'database') {
          statusEl.innerHTML = '<span class="badge bg-success" style="font-size:0.7rem;">Active</span>';
          input.placeholder = 'Current key: ' + data.AI_KEY_HINT;
          removeBtn.style.display = '';
          sourceBadge.innerHTML = '';
        } else if (data.AI_KEY_SOURCE === 'properties') {
          statusEl.innerHTML = '<span class="badge bg-info" style="font-size:0.7rem;">Server Config</span>';
          input.placeholder = 'Server key: ' + data.AI_KEY_HINT;
          removeBtn.style.display = 'none';
          sourceBadge.innerHTML = '<span class="badge bg-info ms-1" style="font-size:0.6rem;">via ssa.properties</span>';
        } else {
          statusEl.innerHTML = '<span class="badge bg-secondary" style="font-size:0.7rem;">Not Configured</span>';
          input.placeholder = 'sk-ant-api03-...';
          removeBtn.style.display = 'none';
          sourceBadge.innerHTML = '';
        }

        // Chatbot toggle
        document.getElementById('bpoChatbotAllUsers').checked = data.CHATBOT_ALL_BPO_USERS === true;
      })
      .catch(function() {
        document.getElementById('bpoAiKeyStatus').innerHTML =
          '<span class="badge bg-warning" style="font-size:0.7rem;">Error loading</span>';
      });
  });

  function bpoToggleAiKeyVisibility() {
    var inp = document.getElementById('bpoAiApiKeyInput');
    var icon = document.getElementById('bpoAiKeyPwIcon');
    if (inp.type === 'password') { inp.type = 'text'; icon.className = 'bi bi-eye-slash'; }
    else { inp.type = 'password'; icon.className = 'bi bi-eye'; }
  }

  function bpoValidateAndSaveAiKey() {
    var key = document.getElementById('bpoAiApiKeyInput').value.trim();
    if (!key) { bpoShowAiMsg('warning', 'Please enter an API key.'); return; }
    bpoShowAiMsg('muted', '<i class="bi bi-arrow-repeat bpo-spin"></i> Validating...');
    document.getElementById('bpoAiKeySaveBtn').disabled = true;
    fetch('UpdateBpoSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=saveApiKey&apiKey=' + encodeURIComponent(key)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      document.getElementById('bpoAiKeySaveBtn').disabled = false;
      if (data.status === 'ok') {
        bpoShowAiMsg('success', '<i class="bi bi-check-circle"></i> Key validated and saved. AI features are now active.');
        document.getElementById('bpoAiKeyStatus').innerHTML =
            '<span class="badge bg-success" style="font-size:0.7rem;">Active</span>';
        document.getElementById('bpoAiApiKeyInput').value = '';
        document.getElementById('bpoAiApiKeyInput').placeholder = 'Current key: ' + data.hint;
        document.getElementById('bpoAiKeyRemoveBtn').style.display = '';
        document.getElementById('bpoAiKeySourceBadge').innerHTML = '';
      } else {
        bpoShowAiMsg('danger', '<i class="bi bi-x-circle"></i> ' + (data.message || 'Validation failed.'));
      }
    })
    .catch(function() {
      document.getElementById('bpoAiKeySaveBtn').disabled = false;
      bpoShowAiMsg('danger', '<i class="bi bi-x-circle"></i> Request failed.');
    });
  }

  function bpoRemoveAiKey() {
    if (!confirm('Remove the AI API key? AI features will be disabled.')) return;
    bpoShowAiMsg('muted', '<i class="bi bi-arrow-repeat bpo-spin"></i> Removing...');
    fetch('UpdateBpoSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=removeApiKey'
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.status === 'ok') {
        bpoShowAiMsg('success', '<i class="bi bi-check-circle"></i> Key removed. AI features disabled.');
        document.getElementById('bpoAiKeyStatus').innerHTML =
            '<span class="badge bg-secondary" style="font-size:0.7rem;">Not Configured</span>';
        document.getElementById('bpoAiApiKeyInput').placeholder = 'sk-ant-api03-...';
        document.getElementById('bpoAiKeyRemoveBtn').style.display = 'none';
      } else {
        bpoShowAiMsg('danger', '<i class="bi bi-x-circle"></i> ' + (data.message || 'Remove failed.'));
      }
    })
    .catch(function() {
      bpoShowAiMsg('danger', '<i class="bi bi-x-circle"></i> Request failed.');
    });
  }

  function bpoSaveSettings() {
    var chatbotAll = document.getElementById('bpoChatbotAllUsers').checked;
    fetch('UpdateBpoSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=saveSettings&chatbotAllBpoUsers=' + (chatbotAll ? 'true' : 'false')
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
      if (data.status === 'ok') {
        bootstrap.Modal.getInstance(document.getElementById('bpoSettingsMod')).hide();
      } else {
        bpoShowAiMsg('danger', '<i class="bi bi-x-circle"></i> ' + (data.message || 'Save failed.'));
      }
    })
    .catch(function() {
      bpoShowAiMsg('danger', '<i class="bi bi-x-circle"></i> Save failed.');
    });
  }

  function bpoShowAiMsg(cls, html) {
    var el = document.getElementById('bpoAiKeyMessage');
    el.innerHTML = '<span class="text-' + cls + '">' + html + '</span>';
    if (cls === 'success') setTimeout(function() { el.innerHTML = ''; }, 5000);
  }
</script>
<style>
  @keyframes bpoSpinAnim { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
  .bpo-spin { display: inline-block; animation: bpoSpinAnim 1s linear infinite; }
</style>
