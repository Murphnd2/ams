<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Quick Ticket — Inline card shown on the home page when the timeclock is disabled.
  Mirrors the Create Ticket modal form (createTicket25.jsp) but renders as a
  static card that lives in the same column slot as the timeclock.
  All element IDs are prefixed with qt_ to avoid collisions with the navbar modal.
--%>

<style>
  /* Typeahead dropdown (scoped to quick-ticket card) */
  .qt-dropdown { position:absolute; z-index:1055; background:white; border:1px solid #dee2e6;
                   border-top:none; border-radius:0 0 6px 6px; max-height:260px; overflow-y:auto;
                   width:100%; display:none; box-shadow:0 4px 12px rgba(0,0,0,.1); }
  .qt-dropdown .qt-item { padding:0.4rem 0.75rem; cursor:pointer; font-size:0.85rem; }
  .qt-dropdown .qt-item:hover, .qt-dropdown .qt-item.qt-active { background:#e8f4fd; }
  /* Selected person badge */
  .qt-badge { display:none; align-items:center; gap:0.5rem; background:#e8f4fd;
              border:1px solid #0d5681; border-radius:6px; padding:0.3rem 0.7rem; }
  .qt-badge .qt-badge-name { font-weight:600; text-transform:capitalize; color:#0d5681; font-size:0.85rem; }
  .qt-badge .qt-badge-er   { font-size:0.8rem; color:#5a6268; }
</style>

<div class="tc-panel" style="margin-top:0;">
  <%-- ── HEADER BAR ── --%>
  <div class="hdr-bar d-flex align-items-center justify-content-between">
    <span><i class="bi bi-ticket-detailed me-1"></i>Log Ticket</span>
  </div>

  <%-- ── FORM BODY ── --%>
  <div style="background:#fff; padding:0.75rem 0.85rem 0.85rem;">
    <form method="post" action="CreateTicket25" id="qt_form">
      <input type="hidden" name="employeeId" id="qt_employeeId" value="">

      <%-- Person search --%>
      <label class="form-label fw-semibold mb-1" style="font-size:0.82rem;">
        <i class="bi bi-person-badge me-1 text-ssa"></i>Person
      </label>
      <div style="position:relative;" class="mb-2">
        <input class="form-control form-control-sm" type="text" id="qt_personSearch" name="contactName"
               autocomplete="off" placeholder="Type a name to search..." required>
        <div class="qt-badge" id="qt_personBadge">
          <span class="qt-badge-name"></span>
          <span class="qt-badge-er"></span>
        </div>
        <button type="button" class="btn-close" id="qt_personClear"
                style="display:none; position:absolute; right:8px; top:50%; transform:translateY(-50%); font-size:0.6rem;"
                aria-label="clear"></button>
        <div class="qt-dropdown" id="qt_personDropdown"></div>
      </div>

      <%-- Reason dropdown --%>
      <label class="form-label fw-semibold mb-1" style="font-size:0.82rem;">
        <i class="bi bi-question-circle me-1 text-ssa"></i>Reason
      </label>
      <select class="form-select form-select-sm mb-2" onchange="qt_showHide()" name="serviceItemList" id="qt_serviceItemList">
        <option value="S" selected>Select a reason...</option>
        <option value="0" class="text-secondary fst-italic">Enter my own reason</option>
        <c:forEach var="si" items="${applicationScope.global.getTicketServiceItems()}">
          <option value="${si.getId()}">${si.getTicketCategory().getShortText()} - ${si.getDescription()}</option>
        </c:forEach>
      </select>

      <%-- Custom reason (hidden until "Enter my own" selected) --%>
      <div id="qt_enterReasonRow" style="display:none;" class="mb-2">
        <label class="form-label fw-semibold mb-1" style="font-size:0.82rem;">
          <i class="bi bi-keyboard me-1 text-ssa"></i>Reason for Ticket
        </label>
        <input type="text" class="form-control form-control-sm" name="reasonNameTicket" id="qt_reasonName">
      </div>

      <%-- Description --%>
      <label class="form-label fw-semibold mb-1" style="font-size:0.82rem;">
        <i class="bi bi-chat-text me-1 text-ssa"></i>Description
      </label>
      <textarea class="form-control form-control-sm mb-2" name="ticketDescription" id="qt_description"
                rows="4" placeholder="Describe issue here" style="font-size:0.85rem;"></textarea>

      <button type="submit" class="btn btn-ssa btn-sm w-100" id="qt_submitBtn" disabled>
        <i class="bi bi-ticket-detailed me-1"></i>Create Ticket
      </button>
    </form>
  </div>
</div>

<script>
  /* ═══ QUICK-TICKET FORM LOGIC ═══ */
  (function(){
    /* ── Reason show/hide ── */
    window.qt_showHide = function(){
      var sel   = document.getElementById('qt_serviceItemList');
      var btn   = document.getElementById('qt_submitBtn');
      var row   = document.getElementById('qt_enterReasonRow');
      var input = document.getElementById('qt_reasonName');
      btn.disabled = (sel.value === 'S');
      if(sel.value === '0'){ row.style.display = ''; input.required = true; }
      else { row.style.display = 'none'; input.required = false; }
    };

    /* ── Person typeahead (reuses _ticketEmployees from createTicket25.jsp) ── */
    var input    = document.getElementById('qt_personSearch');
    var hidden   = document.getElementById('qt_employeeId');
    var dropdown = document.getElementById('qt_personDropdown');
    var badge    = document.getElementById('qt_personBadge');
    var clearBtn = document.getElementById('qt_personClear');
    var MAX_SHOW = 8;
    var selIdx   = -1;

    function filter(query){
      if(!query || query.trim().length < 2) return [];
      var q = query.replace(/,/g,' ').replace(/\s+/g,' ').trim().toLowerCase();
      var words = q.split(' ');
      return _ticketEmployees.filter(function(ee){
        var h = (ee.last+' '+ee.first+' '+ee.er+' '+ee.email).toLowerCase();
        return words.every(function(w){ return h.indexOf(w)>=0; });
      });
    }

    function render(matches){
      dropdown.innerHTML = ''; selIdx = -1;
      if(matches.length === 0 && input.value.trim().length > 0){
        var hint = document.createElement('div');
        hint.className = 'px-3 py-2 text-muted';
        hint.style.fontSize = '0.8rem';
        hint.textContent = 'No match found \u2014 name will be logged as-is';
        dropdown.appendChild(hint);
        dropdown.style.display = 'block';
        return;
      }
      if(matches.length === 0){ dropdown.style.display='none'; return; }

      matches.slice(0, MAX_SHOW).forEach(function(ee, idx){
        var item = document.createElement('div');
        item.className = 'qt-item';
        item.setAttribute('data-idx', idx);
        item.innerHTML =
          '<span class="fw-semibold">'+ee.last.toLowerCase()+', '+ee.first.toLowerCase()+'</span>'+
          '<span class="text-muted ms-2" style="font-size:0.8rem;">'+ee.er+'</span>';
        item.addEventListener('mousedown', function(e){ e.preventDefault(); pick(ee); });
        dropdown.appendChild(item);
      });
      if(matches.length > MAX_SHOW){
        var more = document.createElement('div');
        more.className = 'px-3 py-1 text-muted';
        more.style.fontSize = '0.75rem';
        more.textContent = '+ '+(matches.length - MAX_SHOW)+' more \u2014 keep typing';
        dropdown.appendChild(more);
      }
      dropdown.style.display = 'block';
    }

    function pick(ee){
      hidden.value = ee.id;
      input.value = '';
      input.style.display = 'none';
      input.removeAttribute('required');
      badge.style.display = 'flex';
      badge.querySelector('.qt-badge-name').textContent = ee.first.toLowerCase()+' '+ee.last.toLowerCase();
      badge.querySelector('.qt-badge-er').textContent = ee.er;
      dropdown.style.display = 'none';
      clearBtn.style.display = 'inline';
    }

    input.addEventListener('input', function(){ hidden.value=''; render(filter(input.value)); });
    input.addEventListener('focus', function(){ if(input.value.trim().length>=2) render(filter(input.value)); });
    input.addEventListener('blur', function(){ setTimeout(function(){ dropdown.style.display='none'; },150); });

    input.addEventListener('keydown', function(e){
      var items = dropdown.querySelectorAll('.qt-item');
      if(items.length===0) return;
      if(e.key==='ArrowDown'){ e.preventDefault(); selIdx=Math.min(selIdx+1,items.length-1);
        items.forEach(function(el,i){ el.classList.toggle('qt-active',i===selIdx); }); }
      else if(e.key==='ArrowUp'){ e.preventDefault(); selIdx=Math.max(selIdx-1,0);
        items.forEach(function(el,i){ el.classList.toggle('qt-active',i===selIdx); }); }
      else if(e.key==='Enter'&&selIdx>=0){ e.preventDefault(); items[selIdx].dispatchEvent(new Event('mousedown')); }
    });

    clearBtn.addEventListener('click', function(){
      hidden.value=''; input.value=''; input.style.display='';
      input.setAttribute('required',''); badge.style.display='none';
      clearBtn.style.display='none'; input.focus();
    });

    /* ── Build grouped optgroups for reason dropdown ── */
    (function(){
      var select = document.getElementById('qt_serviceItemList');
      var placeholder = select.querySelector('option[value="S"]');
      var enterOwn    = select.querySelector('option[value="0"]');
      var opts = [];
      select.querySelectorAll('option').forEach(function(opt){
        if(opt.value!=='S' && opt.value!=='0') opts.push(opt);
      });
      var groups = {}, order = [];
      opts.forEach(function(opt){
        var text = opt.textContent.trim();
        var dash = text.indexOf(' - ');
        var cat, sub;
        if(dash>0){ cat=text.substring(0,dash).trim(); sub=text.substring(dash+3).trim(); }
        else { cat='Other'; sub=text; }
        if(!groups[cat]){ groups[cat]=[]; order.push(cat); }
        groups[cat].push({value:opt.value, label:sub});
      });
      select.innerHTML = '';
      select.appendChild(placeholder);
      select.appendChild(enterOwn);
      order.forEach(function(cat){
        var og = document.createElement('optgroup');
        og.label = cat;
        groups[cat].forEach(function(item){
          var o = document.createElement('option');
          o.value = item.value;
          o.textContent = item.label;
          og.appendChild(o);
        });
        select.appendChild(og);
      });
    })();
  })();
</script>
