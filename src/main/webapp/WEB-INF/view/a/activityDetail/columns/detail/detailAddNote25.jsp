<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<link href="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.snow.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.js"></script>
<style>
  .note-dd select { font-size: 0.72rem; padding: 0.1rem 1.2rem 0.1rem 0.3rem; height: auto; border-color: #e5d5b0; background-color: transparent; color: #92400e; }
  .note-dd select:focus { background-color: white; color: #333; border-color: #d97706; }
  .note-dd select option { color: #333; background: white; }
  #noteEditorWrap .ql-toolbar { padding: 3px 5px; border: none; border-bottom: 1px solid #dee2e6; }
  #noteEditorWrap .ql-toolbar button { width: 22px; height: 22px; padding: 1px; }
  #noteEditorWrap .ql-toolbar .ql-picker-label { font-size: 0.75rem; padding: 0 2px; }
  #noteEditorWrap .ql-container { border: none; font-size: 0.8rem; resize: vertical; overflow: hidden; min-height: 60px; }
  #noteEditorWrap .ql-editor { min-height: 60px; max-height: 300px; overflow-y: auto; padding: 6px 8px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
  #noteEditorWrap .ql-editor.ql-blank::before { font-size: 0.8rem; font-style: italic; color: #adb5bd; }
</style>

<form method="post" action="AddNoteToActivity25" id="addNoteForm" class="mt-2 mb-0 flex-shrink-0">
  <div class="hdr-bar d-flex align-items-center justify-content-between">
    <div class="d-flex align-items-center">
      <span class="d-flex align-items-center">
        <i class="bi bi-journal-plus me-1"></i>
        <span class="fw-semibold">Add Note</span>
      </span>
    </div>
    <div class="d-flex align-items-center gap-2 note-dd">
      <c:import url="/WEB-INF/view/a/general/globalDropDowns/ddReasons25.jsp"></c:import>
      <c:import url="/WEB-INF/view/a/general/globalDropDowns/ddNoteStatus25.jsp"></c:import>
      <%-- V062: Agent visibility override. PSP users pick explicit visibility when the
           default doesn't fit; agents don't see this — their notes are always agent-visible. --%>
      <c:set var="viewerIsAgent" value="${sessionScope.isAgent && !sessionScope.isPspUser && !sessionScope.isPspAdmin && !sessionScope.isPspSales && !sessionScope.isAgencyAdmin}"/>
      <c:if test="${not viewerIsAgent}">
        <select name="agentVisible" class="form-select form-select-sm"
                title="Agent visibility (overrides PSP default)"
                style="font-size:0.72rem; padding:0.1rem 1.2rem 0.1rem 0.3rem; width:auto;">
          <option value="">Default</option>
          <option value="visible">Visible to agent</option>
          <option value="hidden">Hidden from agent</option>
        </select>
      </c:if>
    </div>
  </div>
  <div id="addNoteBody">
    <div class="border border-top-0" style="border-radius: 0 0 6px 6px; border-color: #dee2e6 !important;">
      <div id="noteEditorWrap">
        <div id="noteQuill"></div>
      </div>
      <input type="hidden" name="noteText" id="noteTextHidden">
      <div class="d-flex justify-content-end align-items-center p-1 gap-2">
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

    // Tab goes to save button
    quill.keyboard.bindings[9] = [];
    quill.root.addEventListener('keydown', function(e) {
      if (e.key === 'Tab' && !e.shiftKey) {
        e.preventDefault();
        target.closest('form').querySelector('button[type="submit"]').focus();
      }
    });

    // Form submit — find the correct form (the one containing the visible editor)
    var form = target.closest('form');
    if (form) {
      form.addEventListener('submit', function(e) {
        var html = quill.root.innerHTML;
        if (html === '<p><br></p>') html = '';
        form.querySelector('[name="noteText"]').value = html;
      });
    }

    // (Collapse toggle removed — editor always visible)
  });
</script>