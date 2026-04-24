<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<link href="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.snow.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.js"></script>
<style>
  /* Quill shell */
  #noteEditorWrap .ql-toolbar { padding: 3px 5px; border: none; border-bottom: 1px solid #dee2e6; }
  #noteEditorWrap .ql-toolbar button { width: 22px; height: 22px; padding: 1px; }
  #noteEditorWrap .ql-toolbar .ql-picker-label { font-size: 0.75rem; padding: 0 2px; }
  #noteEditorWrap .ql-container { border: none; font-size: 0.8rem; resize: vertical; overflow: hidden; min-height: 60px; }
  #noteEditorWrap .ql-editor { min-height: 60px; max-height: 300px; overflow-y: auto; padding: 6px 8px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
  #noteEditorWrap .ql-editor.ql-blank::before { font-size: 0.8rem; font-style: italic; color: #adb5bd; }

  /* V062: agent-visibility tri-state pill in the header bar.
     Out of tab order (tabindex="-1"); cycles Default → Visible → Hidden on click. */
  .agent-vis-pill {
    font-size: 0.68rem; font-weight: 600;
    padding: 2px 8px; border-radius: 12px;
    border: 1px solid rgba(255,255,255,0.35);
    cursor: pointer;
    background: rgba(255,255,255,0.08); color: #ffffff;
    display: inline-flex; align-items: center; gap: 4px;
    line-height: 1.4;
  }
  .agent-vis-pill:hover  { background: rgba(255,255,255,0.18); }
  .agent-vis-pill.default { background: rgba(255,255,255,0.08); color: #ced4da; border-color: rgba(255,255,255,0.25); }
  .agent-vis-pill.visible { background: #d4edda; color: #155724; border-color: #c3e6cb; }
  .agent-vis-pill.hidden  { background: #fff3cd; color: #856404; border-color: #ffeeba; }

  /* Inline footer strip — Reason / Status / Save. Tab order: Quill → Reason → Status → Save. */
  .note-footer-bar {
    display: flex; align-items: center; gap: 6px;
    padding: 6px 8px; border-top: 1px solid #f1f3f5; background: #fbfcfd;
  }
  .note-footer-bar .form-select {
    font-size: 0.72rem; padding: 2px 22px 2px 6px; height: auto;
    border: 1px solid #ced4da; background-color: white; color: #212529;
  }
  /* Sized to fit the longest typical option text without truncation. */
  .note-footer-bar select[name="reasonList"] { min-width: 170px; }
  .note-footer-bar select[name="noteStatus"] { min-width: 140px; }
  .note-footer-bar .form-select:focus { border-color: var(--ssa, #0d5681); box-shadow: 0 0 0 0.15rem rgba(13,86,129,.2); }
  .note-footer-bar .spacer { flex: 1; }
</style>

<c:set var="viewerIsAgent" value="${sessionScope.isAgent && !sessionScope.isPspUser && !sessionScope.isPspAdmin && !sessionScope.isPspSales && !sessionScope.isAgencyAdmin}"/>

<form method="post" action="AddNoteToActivity25" id="addNoteForm" class="mt-2 mb-0 flex-shrink-0">
  <%-- Header: title + agent-visibility tri-state pill (PSP roles only, out of tab order) --%>
  <div class="hdr-bar d-flex align-items-center justify-content-between">
    <div class="d-flex align-items-center">
      <span class="d-flex align-items-center">
        <i class="bi bi-journal-plus me-1"></i>
        <span class="fw-semibold">Add Note</span>
      </span>
    </div>
    <c:if test="${not viewerIsAgent}">
      <%-- Tri-state toggle. Default → Visible → Hidden → Default on click.
           The hidden input holds the value submitted with the form. --%>
      <button type="button" id="agentVisPill" class="agent-vis-pill default" tabindex="-1"
              onclick="cycleAgentVis()" title="Click to cycle agent visibility for this note">
        <i class="bi bi-dash-circle" id="agentVisIcon"></i>
        <span id="agentVisLabel">Agent: default</span>
      </button>
      <input type="hidden" name="agentVisible" id="agentVisInput" value=""/>
    </c:if>
  </div>

  <%-- Body: Quill editor + inline footer strip --%>
  <div id="addNoteBody">
    <div class="border border-top-0" style="border-radius: 0 0 6px 6px; border-color: #dee2e6 !important;">
      <div id="noteEditorWrap">
        <div id="noteQuill"></div>
      </div>
      <input type="hidden" name="noteText" id="noteTextHidden">

      <div class="note-footer-bar">
        <c:import url="/WEB-INF/view/a/general/globalDropDowns/ddReasons25.jsp"></c:import>
        <c:import url="/WEB-INF/view/a/general/globalDropDowns/ddNoteStatus25.jsp"></c:import>
        <span class="spacer"></span>
        <span id="noteUnsavedIndicator" class="d-none" style="font-size: 0.72rem; color: #b08000; transition: opacity 0.3s;">
          <i class="bi bi-circle-fill me-1" style="font-size: 0.35rem; vertical-align: middle;"></i>Unsaved
        </span>
        <button type="submit" name="btnAddNote1" value="Save" class="btn btn-sm btn-ssa px-2 py-0"
                title="Save Note" style="font-size: 0.8rem;">
          <i class="bi bi-floppy"></i>
        </button>
      </div>
    </div>
  </div>
</form>

<script>
  // V062: tri-state cycle: "" (default) → "visible" → "hidden" → ""
  // Keeps the hidden input, the pill's css class, its icon, and its label in sync.
  (function() {
    var STATES = [
      { val: '',        cls: 'default', icon: 'bi-dash-circle',     label: 'Agent: default' },
      { val: 'visible', cls: 'visible', icon: 'bi-eye-fill',        label: 'Agent: visible' },
      { val: 'hidden',  cls: 'hidden',  icon: 'bi-eye-slash-fill',  label: 'Agent: hidden'  }
    ];
    window.cycleAgentVis = function() {
      var input = document.getElementById('agentVisInput');
      var pill  = document.getElementById('agentVisPill');
      var icon  = document.getElementById('agentVisIcon');
      var lbl   = document.getElementById('agentVisLabel');
      if (!input || !pill) return;
      var idx = STATES.findIndex(function(s) { return s.val === input.value; });
      if (idx < 0) idx = 0;
      var next = STATES[(idx + 1) % STATES.length];
      input.value = next.val;
      pill.classList.remove('default','visible','hidden');
      pill.classList.add(next.cls);
      if (icon) icon.className = 'bi ' + next.icon;
      if (lbl) lbl.textContent = next.label;
    };
  })();

  document.addEventListener('DOMContentLoaded', function() {
    // Find the visible noteQuill element (handles dual layout)
    var targets = document.querySelectorAll('#noteQuill');
    var target = null;
    for (var i = 0; i < targets.length; i++) {
      if (targets[i].offsetParent !== null) { target = targets[i]; break; }
    }
    if (!target && targets.length > 0) target = targets[targets.length - 1];
    if (!target) return;

    // Init Quill on the visible instance
    var quill = new Quill(target, {
      theme: 'snow',
      placeholder: 'Add a note...',
      modules: {
        toolbar: [['bold', 'italic'], [{ 'list': 'ordered'}, { 'list': 'bullet' }], ['link']]
      }
    });
    window._noteEditor = quill;

    // Unsaved content indicator
    quill.on('text-change', function() {
      var indicator = document.getElementById('noteUnsavedIndicator');
      if (!indicator) return;
      var hasContent = quill.getText().trim().length > 0;
      indicator.classList.toggle('d-none', !hasContent);
    });

    // Restore saved editor height
    var savedHeight = localStorage.getItem('noteEditorHeight');
    var container = target.closest('#noteEditorWrap').querySelector('.ql-container');
    if (savedHeight && container) {
      container.style.height = savedHeight;
    }

    // Watch for resize and persist
    if (container) {
      var resizeObserver = new ResizeObserver(function(entries) {
        for (var entry of entries) {
          localStorage.setItem('noteEditorHeight', entry.target.style.height || entry.contentRect.height + 'px');
        }
      });
      resizeObserver.observe(container);
    }

    // Tab from Quill: jump to the first footer control (Reason select), falling
    // back to Save if the selects aren't present.
    var form = target.closest('form');
    quill.keyboard.bindings[9] = [];
    quill.root.addEventListener('keydown', function(e) {
      if (e.key === 'Tab' && !e.shiftKey) {
        e.preventDefault();
        var next = form.querySelector('select[name="reasonList"]')
                || form.querySelector('select')
                || form.querySelector('button[type="submit"]');
        if (next) next.focus();
      }
    });

    // Form submit — serialize Quill HTML into the hidden noteText field
    if (form) {
      form.addEventListener('submit', function(e) {
        var html = quill.root.innerHTML;
        if (html === '<p><br></p>') html = '';
        form.querySelector('[name="noteText"]').value = html;
      });
    }
  });
</script>
