<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<style>
  /* === ToDo item rows === */
  .td-item { border-left: 4px solid #dee2e6; border-radius: 3px; padding: 0.2rem 0.35rem; margin-bottom: 0.2rem; background: white; }
  .td-item:hover { background: #f8f9fa; }
  .td-item.td-blocked { opacity: 0.55; }
  .td-item.td-delegated { border-left-color: #6c757d; }
  .td-item.td-closed { border-left-color: #198754; background: #f8f9fa; opacity: 0.65; }
  .td-item.bpo-awaiting-verify { border-left-color: #ffc107; background: #fffbea; }

  /* State icon button */
  .td-btn { border: none; background: none; padding: 0; line-height: 1; font-size: 0.95rem; cursor: pointer; width: 1.4rem; text-align: center; flex-shrink: 0; }
  .td-btn.check { color: #198754; transition: transform 0.15s, color 0.15s; }
  .td-btn.check:hover { color: #0f5132; transform: scale(1.3); }
  .td-btn.state-icon { color: #6c757d; }
  .td-btn.undo { color: #198754; transition: transform 0.15s; }
  .td-btn.undo:hover { transform: scale(1.3); }

  /* Description text */
  .td-name { font-size: 0.78rem; font-weight: 500; color: #212529; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .td-name a { color: #0d5681; text-decoration: none; }
  .td-name a:hover { text-decoration: underline; }
  .td-info { color: #6c757d; font-size: 0.65rem; margin-left: 0.2rem; text-decoration: none !important; }
  .td-info:hover { color: #0d5681; }

  /* Kebab menu */
  .td-kebab { border: none; background: none; padding: 0; line-height: 1; font-size: 0.85rem; cursor: pointer; width: 1.2rem; text-align: center; flex-shrink: 0; color: #6c757d; }
  .td-kebab:hover { color: #0d5681; }

  /* Automation lightning icon */
  .td-auto { border: none; background: none; padding: 0; line-height: 1; font-size: 0.85rem; cursor: pointer; width: 1.2rem; text-align: center; flex-shrink: 0; color: #fd7e14; transition: transform 0.15s, color 0.15s; }
  .td-auto:hover { color: #e8590c; transform: scale(1.25); }

  /* Completed section toggle */
  .td-closed-toggle { font-size: 0.75rem; color: #6c757d; cursor: pointer; text-decoration: none; }
  .td-closed-toggle:hover { color: #0d5681; }

  /* Completed item name */
  .td-closed-name { font-size: 0.78rem; text-decoration: line-through; color: #6c757d; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>

<div class="d-flex flex-column" style="flex: 1 1 auto; min-height: 0;">

  <%-- Scrollable open items --%>
  <div class="overflow-auto flex-grow-1" style="min-height: 0;">
    <div class="m-0 p-0">

      <%-- Activity-closed gate --%>
      <c:set var="isPast" value="pe-none"/>
      <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
        <c:set var="isPast" value=""/>
      </c:if>

      <%-- ===== OPEN ITEMS ===== --%>
      <c:set var="isFirstOpen" value="true"/>
      <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}" varStatus="tds">
        <c:if test="${toDo.getTask().getId() != 153 && toDo.isComplete() == false}">

          <%-- Determine row class --%>
          <c:set var="rowClass" value="td-item"/>
          <c:if test="${toDo.isTimeBlocked() || toDo.isWhoBlocked()}">
            <c:set var="rowClass" value="td-item td-blocked"/>
          </c:if>
          <c:if test="${toDo.isDelegated()}">
            <c:set var="rowClass" value="td-item td-delegated"/>
          </c:if>
          <c:if test="${toDo.isBpoCompleted() && !toDo.isComplete()}">
            <c:set var="rowClass" value="td-item bpo-awaiting-verify"/>
          </c:if>

          <div class="${rowClass}">
            <div class="d-flex align-items-center">

              <%-- State icon / check button --%>
              <form method="post" action="${toDo.getFormServlet()}" class="m-0 p-0" style="display:inline;">
                <c:choose>
                  <c:when test="${toDo.getBtnIcon() == 'square'}">
                    <%-- Actionable: green check --%>
                    <button type="submit" class="td-btn check ${toDo.getPointerEvents()} ${isPast}" name="btnToDo" value="${toDo.getToDo().getId()}" title="Complete">
                      <i class="bi bi-check-circle"></i>
                    </button>
                  </c:when>
                  <c:otherwise>
                    <%-- Blocked/delegated: show state icon --%>
                    <button type="submit" class="td-btn state-icon ${toDo.getPointerEvents()} ${isPast}" name="btnToDo" value="${toDo.getToDo().getId()}" title="${toDo.getBtnIcon()}">
                      <i class="bi bi-${toDo.getBtnIcon()}" style="font-size: 0.9rem;"></i>
                    </button>
                  </c:otherwise>
                </c:choose>
              </form>

              <%-- Description --%>
              <div class="td-name flex-grow-1 mx-1">
                <c:choose>
                  <c:when test="${toDo.isComplete()==false && toDo.hasGoto()==true && toDo.getGotoLink()!=null}">
                    <a href="${toDo.getGotoLink().getLinkPath()}" target="_blank">${toDo.getDescription()}</a>
                  </c:when>
                  <c:otherwise>
                    ${toDo.getDescription()}
                  </c:otherwise>
                </c:choose>
                <c:if test="${toDo.hasInfo()==true && toDo.getInfoLink()!=null}">
                  <a href="${toDo.getInfoLink().getLinkPath()}" target="_blank" class="td-info" title="Info"><i class="bi bi-question-circle-fill"></i></a>
                </c:if>
                <c:if test="${toDo.isBpoCompleted() && !toDo.isComplete()}">
                  <span class="badge bg-warning text-dark" style="font-size: 0.6rem; margin-left: 0.3rem;">BPO Done - Verify</span>
                </c:if>
                <c:if test="${toDo.isSourced() && toDo.getBpoRegistration() != null}">
                  <span class="bpo-note-indicator" data-todo-id="${toDo.getToDo().getId()}"
                        style="display:none; cursor:pointer; margin-left:0.3rem;"
                        title="View BPO Notes"
                        onclick="openBpoNotesModal(${toDo.getToDo().getId()})">
                    <i class="bi bi-chat-left-text" style="font-size:0.75rem; color:#0d5681;"></i>
                    <span class="bpo-note-count" style="font-size:0.65rem; color:#0d5681;"></span>
                  </span>
                </c:if>
              </div>

              <%-- Automation icon (first open item only) --%>
              <c:if test="${isFirstOpen && toDo.hasAutomation() && toDo.getAutomation() != null && empty isPast}">
                <button type="button" class="td-auto" data-bs-toggle="modal" data-bs-target="#autoModal" title="${toDo.getAutomationText()}">
                  <i class="bi bi-lightning-charge-fill"></i>
                </button>
              </c:if>

              <%-- Kebab menu --%>
              <c:if test="${empty isPast}">
                <div class="dropdown">
                  <button type="button" class="td-kebab" data-bs-toggle="dropdown" data-bs-strategy="fixed" aria-expanded="false">
                    <i class="bi bi-three-dots-vertical"></i>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end" style="font-size: 0.78rem; min-width: 9rem;">
                    <li>
                      <form method="post" action="ManageTask25" class="m-0">
                        <input type="hidden" name="toDoId" value="${toDo.getToDo().getId()}">
                        <button type="submit" class="dropdown-item py-1"><i class="bi bi-gear me-2"></i>Open</button>
                      </form>
                    </li>
                    <c:if test="${toDo.hasInfo()==true && toDo.getInfoLink()!=null}">
                      <li>
                        <a class="dropdown-item py-1" href="${toDo.getInfoLink().getLinkPath()}" target="_blank"><i class="bi bi-question-circle me-2"></i>Info</a>
                      </li>
                    </c:if>
                    <c:if test="${toDo.hasGoto()==true && toDo.getGotoLink()!=null}">
                      <li>
                        <a class="dropdown-item py-1" href="${toDo.getGotoLink().getLinkPath()}" target="_blank"><i class="bi bi-box-arrow-up-right me-2"></i>Go To</a>
                      </li>
                    </c:if>
                    <c:if test="${toDo.isSourced() && toDo.getBpoRegistration() != null}">
                      <li>
                        <a class="dropdown-item py-1" href="javascript:void(0)" onclick="openBpoNotesModal(${toDo.getToDo().getId()})">
                          <i class="bi bi-chat-left-text me-2"></i>Notes
                        </a>
                      </li>
                    </c:if>
                    <c:if test="${toDo.getBtnIcon() == 'square'}">
                      <li><hr class="dropdown-divider my-1"></li>
                      <li>
                        <form method="post" action="${toDo.getFormServlet()}" class="m-0">
                          <button type="submit" class="dropdown-item py-1 text-success" name="btnToDo" value="${toDo.getToDo().getId()}"><i class="bi bi-check-circle me-2"></i>Complete</button>
                        </form>
                      </li>
                    </c:if>
                  </ul>
                </div>
              </c:if>

            </div>
          </div>

          <c:set var="isFirstOpen" value="false"/>

        </c:if>
      </c:forEach>

    </div>
  </div>

  <%-- ===== COMPLETED ITEMS (pinned below scroll) ===== --%>
  <c:set var="closedCount" value="0"/>
      <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
        <c:if test="${toDo.getTask().getId() != 153 && toDo.isComplete() == true}">
          <c:set var="closedCount" value="${closedCount + 1}"/>
        </c:if>
      </c:forEach>

      <c:if test="${closedCount > 0}">
        <div class="mt-2 mb-1">
          <a class="td-closed-toggle" data-bs-toggle="collapse" href="#tdClosedItems" role="button" aria-expanded="false">
            <i class="bi bi-chevron-right me-1" id="tdClosedChevron"></i>Completed (${closedCount})
          </a>
        </div>
        <div class="collapse" id="tdClosedItems">
          <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
            <c:if test="${toDo.getTask().getId() != 153 && toDo.isComplete() == true}">
              <div class="td-item td-closed">
                <div class="d-flex align-items-center">

                  <%-- Undo button --%>
                  <form method="post" action="ReOpenToDo25" class="m-0 p-0" style="display:inline;">
                    <button type="submit" class="td-btn undo ${isPast}" name="btnToDo" value="${toDo.getToDo().getId()}" title="Undo">
                      <i class="bi bi-arrow-counterclockwise"></i>
                    </button>
                  </form>

                  <%-- Name (strike-through) --%>
                  <span class="td-closed-name flex-grow-1 mx-1">${toDo.getDescription()}</span>

                </div>
              </div>
            </c:if>
          </c:forEach>
        </div>

        <script>
          document.getElementById('tdClosedItems')?.addEventListener('show.bs.collapse', function(){
            document.getElementById('tdClosedChevron')?.classList.replace('bi-chevron-right','bi-chevron-down');
          });
          document.getElementById('tdClosedItems')?.addEventListener('hide.bs.collapse', function(){
            document.getElementById('tdClosedChevron')?.classList.replace('bi-chevron-down','bi-chevron-right');
          });
        </script>
      </c:if>

</div>

<%-- ===== AUTOMATION MODAL ===== --%>
<c:set var="firstOpenToDo" value="${null}"/>
<c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
  <c:if test="${firstOpenToDo == null && toDo.getTask().getId() != 153 && toDo.isComplete() == false}">
    <c:set var="firstOpenToDo" value="${toDo}"/>
  </c:if>
</c:forEach>

<c:if test="${firstOpenToDo != null && firstOpenToDo.hasAutomation() && firstOpenToDo.getAutomation() != null}">
  <div class="modal fade" id="autoModal" tabindex="-1" aria-labelledby="autoModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
      <div class="modal-content">
        <div class="modal-header py-2" style="background: linear-gradient(135deg, #0d5681, #0a4468); color: white;">
          <h6 class="modal-title m-0" id="autoModalLabel">
            <i class="bi bi-lightning-charge-fill me-1" style="color: #fd7e14;"></i>${firstOpenToDo.getAutomationText()}
          </h6>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <div class="text-center text-muted py-3">
            <i class="bi bi-envelope-paper" style="font-size: 2rem; color: #0d5681;"></i>
            <p class="mt-2 mb-1 fw-semibold" style="font-size: 0.9rem;">${firstOpenToDo.getAutomation().getAutomationName()}</p>
            <p class="mb-0" style="font-size: 0.78rem;">Automation email ready to send for this task.</p>
          </div>
        </div>
        <div class="modal-footer py-2">
          <c:set var="autoId" value="${firstOpenToDo.getAutomation().getId()}"/>
          <a href="PreviewAutomation?aeId=${autoId}" class="btn btn-sm btn-outline-secondary" target="_blank">
            <i class="bi bi-eye me-1"></i>Preview
          </a>
          <a href="${firstOpenToDo.getServletName()}" class="btn btn-sm btn-ssa">
            <i class="bi bi-send me-1"></i>Send
          </a>
        </div>
      </div>
    </div>
  </div>
</c:if>

<%-- ===== BPO NOTES MODAL ===== --%>
<div class="modal fade" id="bpoNotesModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header" style="background:var(--ssa); color:white; padding:0.5rem 1rem;">
        <h6 class="modal-title mb-0"><i class="bi bi-chat-left-text me-2"></i>Task Notes</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body" style="padding:0.75rem 1rem;">
        <div id="pspNotesList" style="max-height:300px; overflow-y:auto; margin-bottom:0.75rem;">
          <div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>
        </div>
        <hr style="margin:0.5rem 0;">
        <div class="mb-2">
          <div class="input-group input-group-sm">
            <input type="text" class="form-control" id="pspNoteInput" placeholder="Add a note...">
            <button type="button" class="btn btn-sm btn-outline-ssa" onclick="pspAddNoteAjax()">
              <i class="bi bi-chat-dots me-1"></i>Add
            </button>
          </div>
          <div class="mt-1">
            <label class="form-label mb-0" style="font-size:0.75rem; color:#6c757d; cursor:pointer;">
              <i class="bi bi-paperclip"></i> Attach file
              <input type="file" id="pspNoteFile" style="display:none;" onchange="pspUpdateFileLabel(this)">
            </label>
            <span id="pspFileLabel" style="font-size:0.75rem; color:#0d5681;"></span>
            <span id="pspFileClear" style="display:none; font-size:0.75rem; color:#dc3545; cursor:pointer; margin-left:0.3rem;" onclick="pspClearFileInput()">&times;</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<script>
  /* === WS2: Note indicator AJAX on page load === */
  document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.bpo-note-indicator').forEach(function(el) {
      const todoId = el.dataset.todoId;
      fetch('BpoGetNotes?todoId=' + todoId)
        .then(function(r) { return r.json(); })
        .then(function(notes) {
          if (notes.length > 0) {
            el.style.display = 'inline';
            el.querySelector('.bpo-note-count').textContent = ' (' + notes.length + ')';
          }
        })
        .catch(function() {}); // silent fail
    });
  });

  /* === WS3: Notes modal functions === */
  let currentPspNoteToDoId = null;

  function openBpoNotesModal(todoId) {
    currentPspNoteToDoId = todoId;
    const notesList = document.getElementById('pspNotesList');
    notesList.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>';
    document.getElementById('pspNoteInput').value = '';
    pspClearFileInput();

    fetch('BpoGetNotes?todoId=' + todoId)
      .then(function(r) { return r.json(); })
      .then(function(notes) { pspRenderNotes(notes); })
      .catch(function() {
        notesList.innerHTML = '<div class="text-center text-muted py-2" style="font-size:0.8rem;">Could not load notes</div>';
      });

    new bootstrap.Modal(document.getElementById('bpoNotesModal')).show();
  }

  function pspRenderNotes(notes) {
    const notesDiv = document.getElementById('pspNotesList');
    if (notes.length === 0) {
      notesDiv.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">No notes yet</div>';
      return;
    }
    let html = '';
    notes.forEach(function(n) {
      const badgeStyle = n.source === 'BPO'
        ? 'background:#e8f4f8; color:#0d5681;'
        : 'background:#fff3cd; color:#856404;';
      html += '<div style="padding:0.35rem 0; border-bottom:1px solid #f0f0f0;">';
      html += '  <div class="d-flex align-items-center gap-2">';
      html += '    <span style="font-size:0.65rem; padding:0.1rem 0.4rem; border-radius:8px; ' + badgeStyle + '">' + n.source + '</span>';
      html += '    <span style="font-size:0.8rem; font-weight:500;">' + n.author + '</span>';
      html += '    <span style="font-size:0.7rem; color:#999;">' + n.date + '</span>';
      html += '  </div>';
      html += '  <div style="font-size:0.85rem; margin-top:0.15rem; padding-left:0.2rem;">' + n.text + '</div>';
      if (n.attachments && n.attachments.length > 0) {
        html += '<div style="margin-top:0.25rem; padding-left:0.2rem;">';
        n.attachments.forEach(function(att) {
          html += '<a href="' + att.url + '" target="_blank" '
               + 'style="display:inline-block; font-size:0.75rem; padding:0.15rem 0.5rem; '
               + 'background:#e8f4f8; color:#0d5681; border-radius:12px; text-decoration:none; '
               + 'margin-right:0.3rem; margin-bottom:0.2rem;">'
               + '<i class="bi bi-paperclip"></i> ' + att.name + '</a>';
        });
        html += '</div>';
      }
      html += '</div>';
    });
    notesDiv.innerHTML = html;
  }

  function pspAddNoteAjax() {
    const input = document.getElementById('pspNoteInput');
    const noteText = input.value.trim();
    if (!noteText) return;

    const fileInput = document.getElementById('pspNoteFile');
    input.disabled = true;

    const formData = new FormData();
    formData.append('todoId', currentPspNoteToDoId);
    formData.append('noteText', noteText);
    if (fileInput.files.length > 0) {
      formData.append('noteFile', fileInput.files[0]);
    }

    fetch('AddNoteToToDo25', {
      method: 'POST',
      body: formData
    })
    .then(function() {
      input.value = '';
      input.disabled = false;
      pspClearFileInput();
      input.focus();
      return fetch('BpoGetNotes?todoId=' + currentPspNoteToDoId);
    })
    .then(function(r) { return r.json(); })
    .then(function(notes) { pspRenderNotes(notes); })
    .catch(function() { input.disabled = false; });
  }

  function pspUpdateFileLabel(input) {
    const label = document.getElementById('pspFileLabel');
    const clear = document.getElementById('pspFileClear');
    if (input.files.length > 0) {
      label.textContent = input.files[0].name;
      clear.style.display = 'inline';
    } else {
      label.textContent = '';
      clear.style.display = 'none';
    }
  }

  function pspClearFileInput() {
    document.getElementById('pspNoteFile').value = '';
    document.getElementById('pspFileLabel').textContent = '';
    document.getElementById('pspFileClear').style.display = 'none';
  }
</script>

<!-- Auto-save on page unload -->
<form id="autoSaveForm" method="post" action="PersistChecklist25" style="display:none;">
  <input type="hidden" name="autoSave" value="true">
</form>
