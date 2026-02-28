<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="createTicketModal" data-bs-backdrop="static" data-bs-keyboard="false" role="dialog" tabindex="-1" aria-labelledby="createTicketLabel" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title text-ssa fw-bold" id="createTicketLabel">
          <i class="bi bi-ticket-detailed me-2"></i>Create Ticket</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close" tabindex="-1"></button>
      </div>
      <div class="modal-body">

        <%-- ═══ PERSON SEARCH DATA (rendered once as JS array) ═══ --%>
        <script>
          const _ticketEmployees = [
            <c:forEach var="ee" items="${applicationScope.global.getEmployees()}" varStatus="s">
            {id:${ee.getId()},last:"${ee.getLastName()}",first:"${ee.getFirstName()}",er:"${ee.getEmployer()}",email:"${ee.getEmail() != null ? ee.getEmail() : ''}"}<c:if test="${!s.last}">,</c:if>
            </c:forEach>
          ];
        </script>

        <script type="text/javascript">
          function showHide(){
            const reasonList = document.getElementById('ticketSubCategoryList');
            const submitButton = document.getElementById('sb100');
            const reasonEntryArea = document.getElementById('enterReasonRow');
            const reasonInput = document.getElementById('rnT');

            submitButton.disabled = (reasonList.value === 'S');

            if(reasonList.value === '0'){
              reasonEntryArea.style.display = 'flex';
              reasonInput.required = true;
            } else {
              reasonEntryArea.style.display = 'none';
              reasonInput.required = false;
            }
          }

          /* ═══ PERSON TYPEAHEAD ═══ */
          document.addEventListener('DOMContentLoaded', function(){
            const input     = document.getElementById('personSearch');
            const hidden    = document.getElementById('employeeIdField');
            const dropdown  = document.getElementById('personDropdown');
            const badge     = document.getElementById('personBadge');
            const clearBtn  = document.getElementById('personClear');
            const modal     = document.getElementById('createTicketModal');
            const MAX_SHOW  = 10;

            let selectedIndex = -1;

            function renderDropdown(matches){
              dropdown.innerHTML = '';
              selectedIndex = -1;
              if(matches.length === 0 && input.value.trim().length > 0){
                const hint = document.createElement('div');
                hint.className = 'px-3 py-2 text-muted';
                hint.style.fontSize = '0.85rem';
                hint.textContent = 'No match found \u2014 name will be logged as-is';
                dropdown.appendChild(hint);
                dropdown.style.display = 'block';
                return;
              }
              if(matches.length === 0){ dropdown.style.display = 'none'; return; }

              matches.slice(0, MAX_SHOW).forEach(function(ee, idx){
                const item = document.createElement('div');
                item.className = 'tt-item';
                item.setAttribute('data-idx', idx);
                item.innerHTML =
                  '<span class="fw-semibold">' + ee.last.toLowerCase() + ', ' + ee.first.toLowerCase() + '</span>' +
                  '<span class="text-muted ms-2" style="font-size:0.85rem;">' + ee.er + '</span>';
                item.addEventListener('mousedown', function(e){
                  e.preventDefault();
                  pickEmployee(ee);
                });
                dropdown.appendChild(item);
              });

              if(matches.length > MAX_SHOW){
                const more = document.createElement('div');
                more.className = 'px-3 py-1 text-muted';
                more.style.fontSize = '0.8rem';
                more.textContent = '+ ' + (matches.length - MAX_SHOW) + ' more \u2014 keep typing to narrow';
                dropdown.appendChild(more);
              }

              dropdown.style.display = 'block';
            }

            function filterEmployees(query){
              if(!query || query.trim().length < 2) return [];
              const q = query.replace(/,/g, ' ').replace(/\s+/g, ' ').trim().toLowerCase();
              const words = q.split(' ');
              return _ticketEmployees.filter(function(ee){
                const haystack = (ee.last + ' ' + ee.first + ' ' + ee.er + ' ' + ee.email).toLowerCase();
                return words.every(function(w){ return haystack.indexOf(w) >= 0; });
              });
            }

            function pickEmployee(ee){
              hidden.value = ee.id;
              input.value = '';
              input.style.display = 'none';
              input.removeAttribute('required');
              badge.style.display = 'flex';
              badge.querySelector('.badge-name').textContent =
                ee.first.toLowerCase() + ' ' + ee.last.toLowerCase();
              badge.querySelector('.badge-er').textContent = ee.er;
              dropdown.style.display = 'none';
              clearBtn.style.display = 'inline';
            }

            function resetForm(){
              hidden.value = '';
              input.value = '';
              input.style.display = '';
              input.setAttribute('required', '');
              badge.style.display = 'none';
              clearBtn.style.display = 'none';
              dropdown.style.display = 'none';
              dropdown.innerHTML = '';
              selectedIndex = -1;
              document.getElementById('ticketSubCategoryList').value = 'S';
              document.getElementById('sb100').disabled = true;
              document.getElementById('enterReasonRow').style.display = 'none';
              const rnT = document.getElementById('rnT');
              if(rnT) { rnT.value = ''; rnT.required = false; }
            }

            input.addEventListener('input', function(){
              hidden.value = '';
              renderDropdown(filterEmployees(input.value));
            });

            input.addEventListener('focus', function(){
              if(input.value.trim().length >= 2)
                renderDropdown(filterEmployees(input.value));
            });

            input.addEventListener('blur', function(){
              setTimeout(function(){ dropdown.style.display = 'none'; }, 150);
            });

            /* Keyboard navigation */
            input.addEventListener('keydown', function(e){
              const items = dropdown.querySelectorAll('.tt-item');
              if(items.length === 0) return;

              if(e.key === 'ArrowDown'){
                e.preventDefault();
                selectedIndex = Math.min(selectedIndex + 1, items.length - 1);
                items.forEach(function(el, i){ el.classList.toggle('tt-active', i === selectedIndex); });
              } else if(e.key === 'ArrowUp'){
                e.preventDefault();
                selectedIndex = Math.max(selectedIndex - 1, 0);
                items.forEach(function(el, i){ el.classList.toggle('tt-active', i === selectedIndex); });
              } else if(e.key === 'Enter' && selectedIndex >= 0){
                e.preventDefault();
                items[selectedIndex].dispatchEvent(new Event('mousedown'));
              }
            });

            clearBtn.addEventListener('click', function(){
              hidden.value = '';
              input.value = '';
              input.style.display = '';
              input.setAttribute('required', '');
              badge.style.display = 'none';
              clearBtn.style.display = 'none';
              input.focus();
            });

            /* Reset every time modal opens */
            modal.addEventListener('show.bs.modal', resetForm);
            /* Also reset when modal closes */
            modal.addEventListener('hidden.bs.modal', resetForm);

            /* ═══ BUILD GROUPED REASON DROPDOWN ═══ */
            (function buildReasonOptgroups(){
              const select = document.getElementById('ticketSubCategoryList');
              /* Collect all the server-rendered options (skip the first two: placeholder + enter own) */
              const placeholder = select.querySelector('option[value="S"]');
              const enterOwn    = select.querySelector('option[value="0"]');
              const reasonOpts  = [];
              select.querySelectorAll('option').forEach(function(opt){
                if(opt.value !== 'S' && opt.value !== '0') reasonOpts.push(opt);
              });

              /* Group by the category prefix (text before the first " - ") */
              const groups = {};
              const groupOrder = [];
              reasonOpts.forEach(function(opt){
                const text = opt.textContent.trim();
                const dashIdx = text.indexOf(' - ');
                let catName, subName;
                if(dashIdx > 0){
                  catName = text.substring(0, dashIdx).trim();
                  subName = text.substring(dashIdx + 3).trim();
                } else {
                  catName = 'Other';
                  subName = text;
                }
                if(!groups[catName]){
                  groups[catName] = [];
                  groupOrder.push(catName);
                }
                groups[catName].push({ value: opt.value, label: subName });
              });

              /* Rebuild the select: placeholder, enter-own, then optgroups */
              select.innerHTML = '';
              select.appendChild(placeholder);
              select.appendChild(enterOwn);

              groupOrder.forEach(function(cat){
                const og = document.createElement('optgroup');
                og.label = cat;
                groups[cat].forEach(function(item){
                  const o = document.createElement('option');
                  o.value = item.value;
                  o.textContent = item.label;
                  og.appendChild(o);
                });
                select.appendChild(og);
              });
            })();
          });
        </script>

        <style>
          /* Typeahead dropdown */
          .tt-dropdown { position:absolute; z-index:1055; background:white; border:1px solid #dee2e6;
                         border-top:none; border-radius:0 0 6px 6px; max-height:280px; overflow-y:auto;
                         width:100%; display:none; box-shadow:0 4px 12px rgba(0,0,0,.1); }
          .tt-item { padding:0.45rem 0.75rem; cursor:pointer; }
          .tt-item:hover, .tt-item.tt-active { background:#e8f4fd; }
          /* Selected person badge */
          .person-badge { display:none; align-items:center; gap:0.5rem; background:#e8f4fd;
                          border:1px solid #0d5681; border-radius:6px; padding:0.35rem 0.75rem; }
          .person-badge .badge-name { font-weight:600; text-transform:capitalize; color:#0d5681; }
          .person-badge .badge-er { font-size:0.85rem; color:#5a6268; }
        </style>

        <form method="post" action="CreateTicket25" id="thisForm01">
          <input type="hidden" name="employeeId" id="employeeIdField" value="">

          <%-- ROW: Select Person --%>
          <div class="row align-items-baseline">
            <div class="col-12 col-md-5 col-lg-4 col-xl-3">
              <div class="form-label text-ssa fw-bold w-100">
                <i class="bi bi-person-badge"></i> Person
              </div>
            </div>
            <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-0">
              <div style="position:relative;">
                <input class="form-control" type="text" id="personSearch" name="contactName"
                       autocomplete="off" tabindex="1" placeholder="Type a name to search..."
                       required>
                <div class="person-badge" id="personBadge">
                  <span class="badge-name"></span>
                  <span class="badge-er"></span>
                </div>
                <button type="button" class="btn-close" id="personClear"
                        style="display:none; position:absolute; right:10px; top:50%; transform:translateY(-50%);"
                        aria-label="clear" tabindex="-1"></button>
                <div class="tt-dropdown" id="personDropdown"></div>
              </div>
            </div>
          </div>

          <%-- ROW: Select Reason --%>
          <div class="row align-items-baseline mt-3">
            <div class="col-12 col-md-5 col-lg-4 col-xl-3">
              <div class="form-label text-ssa fw-bold w-100">
                <i class="bi bi-question-circle"></i> Reason
              </div>
            </div>
            <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-0">
              <select class="form-select" onchange="showHide()" name="ticketSubCategoryList"
                      id="ticketSubCategoryList" tabindex="2">
                <option value="S" selected>Select a reason...</option>
                <option value="0" class="text-secondary fst-italic">Enter my own reason</option>
                <%-- Server renders flat options; JS regroups into <optgroup> on load --%>

                <c:forEach var="si" items="${applicationScope.global.getTicketServiceItems()}">
                  <option value="${si.getId()}">
                      ${si.getTicketCategory().getShortText()} - ${si.getDescription()}
                  </option>
                </c:forEach>
              </select>
            </div>
          </div>

          <%-- ROW: Custom Reason (hidden until "Enter My Own" selected) --%>
          <div class="row align-items-baseline" id="enterReasonRow" style="display:none;">
            <div class="col-12 col-md-5 col-lg-4 col-xl-3 mt-3">
              <div class="form-label text-ssa fw-bold w-100">
                <i class="bi bi-keyboard"></i> Reason for Ticket
              </div>
            </div>
            <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-3">
              <input type="text" class="form-control" name="reasonNameTicket" id="rnT" tabindex="3">
            </div>
          </div>

          <%-- ROW: Description (CKEditor) --%>
          <div class="row mt-3">
            <div class="col">
              <textarea class="form-control" name="ticketDescription" id="ticketDescription1"
                        rows="4" placeholder="Describe issue here" tabindex="4"></textarea>
              <script>
                document.addEventListener('DOMContentLoaded', function () {
                  ClassicEditor
                          .create(document.querySelector('#ticketDescription1'), {
                            toolbar: {
                              items: ['bold', 'italic', 'link', '|', 'bulletedList', 'numberedList', '|', 'undo', 'redo', 'code'],
                              shouldNotGroupWhenFull: true
                            }
                          })
                          .then(editor => {
                            const editorElement = document.querySelector('.ck-editor__editable');
                            editorElement.setAttribute('tabindex', '4');

                            editor.ui.view.editable.element.addEventListener('keydown', event => {
                              if (event.key === 'Tab' && !event.shiftKey) {
                                event.preventDefault();
                                const el = document.querySelector('[tabindex="5"]');
                                if (el) el.focus();
                              } else if (event.key === 'Tab' && event.shiftKey) {
                                event.preventDefault();
                                const el = document.querySelector('[tabindex="3"]');
                                if (el) el.focus();
                              }
                            });

                            document.querySelector('#thisForm01').addEventListener('submit', function (event) {
                              editor.getData().then(data => {
                                document.querySelector('#ticketDescription1').value = data;
                              }).then(() => {
                                event.currentTarget.submit();
                              }).catch(error => {
                                console.error('Error updating textarea value:', error);
                              });
                              event.preventDefault();
                            });
                          })
                          .catch(error => {
                            console.error(error);
                          });
                });
              </script>
            </div>
          </div>

          <button type="submit" class="btn btn-ssa w-100 mt-3" id="sb100" disabled tabindex="5">
            <i class="bi bi-ticket-detailed"></i> Create Ticket</button>
        </form>
      </div>
    </div>
  </div>
</div>
