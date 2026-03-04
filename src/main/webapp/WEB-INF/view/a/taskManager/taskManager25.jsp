<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${applicationScope.global.getPsp().getFullName()}</title>
  <style>
    .tm-label { font-size: 0.8rem; font-weight: 600; color: var(--ssa, #0d5681); margin-bottom: 0.25rem; }
    .tm-hint { font-size: 0.72rem; color: #6c757d; margin-top: 0.15rem; }
    .tm-card { border: 1px solid #dee2e6; border-radius: 6px; background: white; padding: 0.75rem; }
    .tm-section { border-left: 3px solid var(--ssa, #0d5681); padding-left: 0.6rem; margin-bottom: 0.75rem; }
    .tm-toggle-row { display: flex; gap: 0.35rem; }
    .tm-toggle-row .btn { font-size: 0.78rem; padding: 0.25rem 0.5rem; flex: 1; }
    .tm-select { font-size: 0.82rem; }
    .tm-url { font-size: 0.82rem; }
    .xml-tag { font-family: monospace; font-size: 0.75rem; background: #e9ecef; padding: 0.15rem 0.35rem; border-radius: 3px; color: #0d5681; white-space: nowrap; }
    .xml-desc { font-size: 0.75rem; color: #495057; }

    /* Full-height layout on desktop */
    @media (min-width: 992px) {
      .tm-form { display: flex; flex-direction: column; height: calc(100vh - 110px); overflow: hidden; }
      .tm-columns { flex: 1; min-height: 0; }
      .tm-col-left { display: flex; flex-direction: column; }
      .tm-col-left .tm-card { flex: 1; overflow-y: auto; }
      .tm-col-right { display: flex; flex-direction: column; }
      .tm-col-right .tm-card { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
      .tm-col-right .tm-card .tm-auto-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
      .tm-col-right .tm-card .tm-auto-body textarea { flex: 1; resize: none; }
    }

    /* Tag reference overlay — positioned over left column, below header */
    .tm-tag-overlay {
      display: none;
      position: fixed;
      z-index: 10;
      overflow-y: auto;
    }
    .tm-tag-overlay.show { display: flex; flex-direction: column; }
    @media (max-width: 991.98px) {
      .tm-tag-overlay { top: 60px; left: 0.5rem; right: 0.5rem; bottom: 0.5rem; border-radius: 6px; }
    }

    /* Ghost footer buttons — now using global .ssa-action from css-js.jsp */
  </style>
</head>
<body>
<%-- === Code-behind: compute all state variables === --%>
<div class="d-none">
  <c:set var="toDo" value="${sessionScope.local.getCurrentToDo()}"/>
  <c:set var="pspAdm1" value="disabled"/>
  <c:if test="${sessionScope.isPspAdmin==true}"><c:set var="pspAdm1" value=""/></c:if>
  <c:set var="ownerLock1" value=""/>
  <c:if test="${toDo.getTask().hasOwner() && toDo.getTask().getOwner()!=null && toDo.getTask().getOwner().getId()!=sessionScope.local.getCurrentPerson().getId() && sessionScope.isPspAdmin==false}">
    <c:set var="ownerLock1" value="disabled"/>
  </c:if>

  <%-- allowEarly state --%>
  <c:set var="aEarly" value=""/><c:set var="bEarly" value="checked"/>
  <c:if test="${toDo.getTask().allowEarly()}"><c:set var="aEarly" value="checked"/><c:set var="bEarly" value=""/></c:if>

  <%-- allowFuture state --%>
  <c:set var="aFuture" value=""/><c:set var="bFuture" value="checked"/>
  <c:if test="${toDo.getTask().allowFuture()}"><c:set var="aFuture" value="checked"/><c:set var="bFuture" value=""/></c:if>

  <%-- whoOwns state --%>
  <c:set var="bcb1" value="checked"/><c:set var="bcb2" value=""/><c:set var="bcb3" value=""/>
  <c:set var="showEE" value="d-none"/>
  <c:choose>
    <c:when test="${toDo.getTask().hasOwner() && !toDo.getTask().allowNonOwner()}">
      <c:set var="bcb1" value=""/><c:set var="bcb3" value="checked"/><c:set var="showEE" value=""/>
    </c:when>
    <c:when test="${toDo.getTask().hasOwner()}">
      <c:set var="bcb1" value=""/><c:set var="bcb2" value="checked"/><c:set var="showEE" value=""/>
    </c:when>
  </c:choose>

  <%-- isSourced state --%>
  <c:set var="acb1" value="checked"/><c:set var="acb2" value=""/><c:set var="acb3" value=""/>
  <c:set var="showBPO" value="d-none"/>
  <c:choose>
    <c:when test="${toDo.getTask().isSourced() && !toDo.getTask().allowNonOwner()}">
      <c:set var="acb1" value=""/><c:set var="acb3" value="checked"/><c:set var="showBPO" value=""/>
    </c:when>
    <c:when test="${toDo.getTask().isSourced()}">
      <c:set var="acb1" value=""/><c:set var="acb2" value="checked"/><c:set var="showBPO" value=""/>
    </c:when>
  </c:choose>

  <%-- goTo state --%>
  <c:set var="gtChecked" value=""/><c:set var="notGtChecked" value="checked"/><c:set var="gtStyle" value="d-none"/>
  <c:if test="${toDo.getTask().hasGoTo() && toDo.getTask().getGoToLink()!=null}">
    <c:set var="gtChecked" value="checked"/><c:set var="notGtChecked" value=""/><c:set var="gtStyle" value=""/>
  </c:if>

  <%-- info state --%>
  <c:set var="infoChecked" value=""/><c:set var="notInfoChecked" value="checked"/><c:set var="infoStyle" value="d-none"/>
  <c:if test="${toDo.getTask().hasInfo() && toDo.getTask().getInfoLink()!=null}">
    <c:set var="infoChecked" value="checked"/><c:set var="notInfoChecked" value=""/><c:set var="infoStyle" value=""/>
  </c:if>
</div>

<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  <form action="UpdateTask25" method="post" class="tm-form">

    <%-- === HEADER === --%>
    <div class="hdr-bar mt-2 mb-2 d-flex align-items-center flex-shrink-0">
      <i class="bi bi-gear me-2"></i>Manage Task: <span class="fw-normal fst-italic ms-1">${sessionScope.local.getCurrentToDoOut().getDescription()}</span>
    </div>

    <div class="row g-3 tm-columns">

      <%-- ============================================================ --%>
      <%-- LEFT COLUMN: Task Settings                                   --%>
      <%-- ============================================================ --%>
        <div class="col-12 ${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser ? 'col-lg-12' : 'col-lg-4'} tm-col-left">
        <div class="tm-card tm-left">

          <%-- ORDERING --%>
          <div class="tm-section">
            <div class="tm-label"><i class="bi bi-sort-down me-1"></i>Ordering</div>
            <div class="mb-2">
              <div class="tm-hint mb-1">When can this task be performed?</div>
              <div class="tm-toggle-row">
                <input type="radio" class="btn-check" id="cb1a" name="allowEarly" value="1" ${aEarly} autocomplete="off">
                <label class="btn btn-outline-success" for="cb1a">Any Time</label>
                <input type="radio" class="btn-check" id="cb1b" name="allowEarly" value="0" ${bEarly} autocomplete="off">
                <label class="btn btn-outline-danger" for="cb1b">Only When At Top</label>
              </div>
            </div>
            <div>
              <div class="tm-hint mb-1">Does this task block tasks below it?</div>
              <div class="tm-toggle-row">
                <input type="radio" class="btn-check" id="cb2a" name="allowFuture" value="1" ${aFuture} autocomplete="off">
                <label class="btn btn-outline-success" for="cb2a">No, Independent</label>
                <input type="radio" class="btn-check" id="cb2b" name="allowFuture" value="0" ${bFuture} autocomplete="off">
                <label class="btn btn-outline-danger" for="cb2b">Yes, Blocks Below</label>
              </div>
            </div>
          </div>

          <%-- ASSIGNMENT --%>
          <div class="tm-section">
            <div class="tm-label"><i class="bi bi-person-badge me-1"></i>Employee Assignment</div>
            <div class="tm-toggle-row mb-2">
              <input type="radio" class="btn-check" id="cb3a" name="whoOwns" value="0" ${bcb1} autocomplete="off" onchange="toggleEE()">
              <label class="btn btn-outline-success" for="cb3a">Anyone</label>
              <input type="radio" class="btn-check" id="cb3b" name="whoOwns" value="1" ${bcb2} autocomplete="off" onchange="toggleEE()">
              <label class="btn btn-outline-warning text-dark" for="cb3b">Assigned</label>
              <input type="radio" class="btn-check" id="cb3c" name="whoOwns" value="2" ${bcb3} autocomplete="off" onchange="toggleEE()">
              <label class="btn btn-outline-danger" for="cb3c">Exclusive</label>
            </div>
            <div id="eeDropDown" class="${showEE}">
              <select class="form-select form-select-sm tm-select" name="ownerId" id="ownerId">
                <c:forEach var="user" items="${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser ? applicationScope.global.getBpoUsers() : applicationScope.global.getUsers()}">
                  <c:set var="uSelect" value=""/>
                  <c:if test="${toDo.getTask().hasOwner() && toDo.getTask().getOwner()!=null && toDo.getTask().getOwner().getId()==user.getId()}">
                    <c:set var="uSelect" value="selected"/>
                  </c:if>
                  <option value="${user.getId()}" ${uSelect}>${user.getLastName()}, ${user.getFirstName()}</option>
                </c:forEach>
              </select>
              <div class="tm-hint">Assigned = visible to them, anyone can complete. Exclusive = only they can complete.</div>
            </div>
          </div>

            <%-- VENDOR SOURCING (PSP only, hidden when no approved vendors) --%>
            <c:if test="${!sessionScope.isBpo && !sessionScope.isBpoAdmin && !sessionScope.isBpoUser && !empty applicationScope.global.getActiveBpoRegistrations()}">
              <div class="tm-section">
                <div class="tm-label"><i class="bi bi-building me-1"></i>Vendor Sourcing</div>
                <div class="tm-toggle-row mb-2">
                  <input type="radio" class="btn-check" id="cb4a" name="isSourced" value="0" ${acb1} autocomplete="off" onchange="toggleBPO()">
                  <label class="btn btn-outline-success" for="cb4a">Internal</label>
                  <input type="radio" class="btn-check" id="cb4b" name="isSourced" value="1" ${acb2} autocomplete="off" onchange="toggleBPO()">
                  <label class="btn btn-outline-warning text-dark" for="cb4b">Sourced</label>
                  <input type="radio" class="btn-check" id="cb4c" name="isSourced" value="2" ${acb3} autocomplete="off" onchange="toggleBPO()">
                  <label class="btn btn-outline-danger" for="cb4c">Vendor Only</label>
                </div>
                <div id="bpoDropDown" class="${showBPO}">
                  <select class="form-select form-select-sm tm-select" name="sourceId" id="sourceId">
                    <c:forEach var="bpo" items="${applicationScope.global.getActiveBpoRegistrations()}">
                      <c:set var="bSelect" value=""/>
                      <c:if test="${toDo.getTask().isSourced() && toDo.getTask().getBpoRegistration()!=null && toDo.getTask().getBpoRegistration().getId()==bpo.getId()}">
                        <c:set var="bSelect" value="selected"/>
                      </c:if>
                      <option value="${bpo.getId()}" ${bSelect}>${bpo.getBpoName()}</option>
                    </c:forEach>
                  </select>
                </div>
              </div>
            </c:if>

          <%-- LINKS --%>
          <div class="tm-section">
            <div class="tm-label"><i class="bi bi-link-45deg me-1"></i>Links</div>

            <%-- GoTo link --%>
            <div class="mb-2">
              <div class="d-flex align-items-center gap-2 mb-1">
                <div class="tm-toggle-row" style="width: 120px; flex: 0 0 120px;">
                  <input type="radio" class="btn-check" id="cb5a" name="hasGoTo" value="1" ${gtChecked} autocomplete="off" onchange="toggleGoTo()">
                  <label class="btn btn-outline-success" for="cb5a" style="font-size:0.72rem; padding:0.15rem 0;">GoTo</label>
                  <input type="radio" class="btn-check" id="cb5b" name="hasGoTo" value="0" ${notGtChecked} autocomplete="off" onchange="toggleGoTo()">
                  <label class="btn btn-outline-secondary" for="cb5b" style="font-size:0.72rem; padding:0.15rem 0;">None</label>
                </div>
                <div class="tm-hint m-0">Website where the task is performed</div>
              </div>
              <input type="url" class="form-control form-control-sm tm-url ${gtStyle}" name="goToPath" id="goToPath" value="<c:if test='${toDo.getTask().getGoToLink()!=null}'>${toDo.getTask().getGoToLink().getLinkPath()}</c:if>" placeholder="https://...">
            </div>

            <%-- Info link --%>
            <div>
              <div class="d-flex align-items-center gap-2 mb-1">
                <div class="tm-toggle-row" style="width: 120px; flex: 0 0 120px;">
                  <input type="radio" class="btn-check" id="cb6a" name="hasInfo" value="1" ${infoChecked} autocomplete="off" onchange="toggleInfo()">
                  <label class="btn btn-outline-success" for="cb6a" style="font-size:0.72rem; padding:0.15rem 0;">Info</label>
                  <input type="radio" class="btn-check" id="cb6b" name="hasInfo" value="0" ${notInfoChecked} autocomplete="off" onchange="toggleInfo()">
                  <label class="btn btn-outline-secondary" for="cb6b" style="font-size:0.72rem; padding:0.15rem 0;">None</label>
                </div>
                <div class="tm-hint m-0">How-to guide or instructions</div>
              </div>
              <input type="url" class="form-control form-control-sm tm-url ${infoStyle}" name="infoPath" id="infoPath" value="<c:if test='${toDo.getTask().getInfoLink()!=null}'>${toDo.getTask().getInfoLink().getLinkPath()}</c:if>" placeholder="https://...">
            </div>
          </div>

          <%-- Buttons at bottom of left card --%>
          <div class="text-center mt-auto pt-2 border-top">
            <button type="submit" ${ownerLock1} name="btnAuto1" value="${toDo.getId()}" class="ssa-action save">
              <i class="bi bi-check-lg me-1"></i>Save
            </button>
            <span class="ssa-action-sep">|</span>
            <c:choose>
              <c:when test="${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser}">
                <a href="BpoHome" class="ssa-action cancel" style="text-decoration:none;">
                  <i class="bi bi-x-lg me-1"></i>Cancel
                </a>
              </c:when>
              <c:when test="${sessionScope.local.getCurrentActivity() != null && sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
                <a href="ViewChecklist25" class="ssa-action cancel" style="text-decoration:none;">
                  <i class="bi bi-x-lg me-1"></i>Cancel
                </a>
              </c:when>
              <c:otherwise>
                <a href="ViewActivity25" class="ssa-action cancel" style="text-decoration:none;">
                  <i class="bi bi-x-lg me-1"></i>Cancel
                </a>
              </c:otherwise>
            </c:choose>
          </div>

        </div>
      </div>

      <%-- ============================================================ --%>
      <%-- RIGHT COLUMN: Automation Email                               --%>
      <%-- ============================================================ --%>
      <c:if test="${!sessionScope.isBpo && !sessionScope.isBpoAdmin && !sessionScope.isBpoUser}">
          <div class="col-12 col-lg-8 tm-col-right">
        <div class="tm-card">
          <c:choose>
            <c:when test="${toDo.getTask().isAutomated()==true && toDo.getTask().getAutomation()==null}">
              <div class="tm-section">
                <div class="tm-label"><i class="bi bi-lightning-charge me-1"></i>Automation</div>
                <div class="tm-hint">This task is automated externally: <b>${toDo.getTask().getServletName()}</b></div>
                <c:if test="${sessionScope.isPspAdmin==true}">
                  <a href="BlowUpAuto?taskId=${toDo.getTask().getId()}" class="btn btn-sm btn-outline-danger mt-1" style="font-size:0.75rem;">
                    <i class="bi bi-trash me-1"></i>Remove External Automation
                  </a>
                </c:if>
              </div>
            </c:when>
            <c:otherwise>
              <div class="tm-section tm-auto-body">
                <div class="tm-label"><i class="bi bi-lightning-charge me-1"></i>Email Automation</div>
                <div class="tm-hint mb-2">Attach a template email that can be sent when this task is active.</div>

                <div class="mb-2 flex-shrink-0">
                  <label class="form-label tm-hint fw-semibold mb-0">Display Statement</label>
                  <input type="text" class="form-control form-control-sm" name="autoName" value="${toDo.getTask().getAutomationText()}" placeholder="e.g. Send enrollment confirmation to employer">
                </div>

                <div class="mb-0 flex-grow-1 d-flex flex-column" style="min-height:0;">
                  <div class="d-flex justify-content-between align-items-center mb-1 flex-shrink-0">
                    <label class="form-label tm-hint fw-semibold mb-0">Email Template</label>
                    <button class="btn btn-sm btn-outline-secondary" type="button" onclick="document.getElementById('tagOverlay').classList.toggle('show')" style="font-size:0.68rem; padding:0.1rem 0.35rem;">
                      <i class="bi bi-question-circle me-1"></i>Tag Reference
                    </button>
                  </div>
                  <textarea class="form-control flex-grow-1" name="autoText" rows="6" style="font-family: monospace; font-size: 0.78rem; min-height: 150px;"><c:if test="${toDo.getTask().getAutomation()!=null}">${toDo.getTask().getAutomation().getHtmlContent()}</c:if></textarea>
                </div>
              </div>

              <input type="hidden" name="taskId" value="${toDo.getTask().getId()}">
            </c:otherwise>
          </c:choose>
        </div>
      </div>
      </c:if>

    </div>

  </form>
</div>

<%-- Tag Reference Overlay (positioned over left column by JS) --%>
<div class="tm-tag-overlay tm-card" id="tagOverlay" style="background:white; border:1px solid var(--ssa,#0d5681); box-shadow: 0 4px 16px rgba(0,0,0,0.12); padding: 1rem;">
  <div class="d-flex justify-content-between align-items-center mb-2 flex-shrink-0">
    <div class="tm-label m-0"><i class="bi bi-code-slash me-1"></i>Template Tags</div>
    <button type="button" class="btn-close" onclick="document.getElementById('tagOverlay').classList.remove('show')" aria-label="Close"></button>
  </div>
  <table class="table table-sm table-hover mb-0" style="font-size: 0.78rem;">
    <thead style="position:sticky; top:0; background:white;">
      <tr><th style="font-size:0.7rem; color:#6c757d;">Tag</th><th style="font-size:0.7rem; color:#6c757d;">Description</th></tr>
    </thead>
    <tbody>
      <tr><td class="xml-tag">&lt;sbj&gt; &lt;/sbj&gt;</td><td class="xml-desc">Subject line of the email</td></tr>
      <tr><td class="xml-tag">&lt;ii&gt; &lt;/ii&gt;</td><td class="xml-desc">User input prompt — label between tags</td></tr>
      <tr><td class="xml-tag">&lt;ii&gt;&lt;l&gt; &lt;/ii&gt;</td><td class="xml-desc">Link input prompt — name after &lt;l&gt;</td></tr>
      <tr><td class="xml-tag">&lt;ii&gt;&lt;cc&gt; &lt;/ii&gt;</td><td class="xml-desc">CC recipient prompt</td></tr>
      <tr><td class="xml-tag">&lt;&lt;#erName&gt;&gt;</td><td class="xml-desc">Inserts the employer name</td></tr>
      <tr><td class="xml-tag">&lt;&lt;#activityType&gt;&gt;</td><td class="xml-desc">Inserts the activity type</td></tr>
      <tr><td class="xml-tag">&lt;&lt;sig&gt;&gt;</td><td class="xml-desc">Inserts sender's signature block</td></tr>
      <tr><td class="xml-tag">&lt;&lt;close&gt;&gt;</td><td class="xml-desc">Auto-closes this task when sent</td></tr>
      <tr><td class="xml-tag">&lt;br/&gt;</td><td class="xml-desc">Line break (HTML)</td></tr>
      <tr><td class="xml-tag">&lt;nl&gt;</td><td class="xml-desc">New line (plain text)</td></tr>
      <tr><td class="xml-tag">&lt;rf&gt; &lt;/rf&gt;</td><td class="xml-desc">Clickable reference link</td></tr>
      <tr><td class="xml-tag">&lt;&lt;webLinkTask&gt;&gt;</td><td class="xml-desc">Inserts this task's GoTo URL</td></tr>
    </tbody>
  </table>
</div>

<script>
  // Position tag overlay over left card
  function positionOverlay(){
    const card = document.querySelector('.tm-col-left .tm-card');
    const overlay = document.getElementById('tagOverlay');
    if(!card || !overlay || window.innerWidth < 992) return;
    const r = card.getBoundingClientRect();
    overlay.style.top = r.top + 'px';
    overlay.style.left = r.left + 'px';
    overlay.style.width = r.width + 'px';
    overlay.style.bottom = (window.innerHeight - r.bottom) + 'px';
    overlay.style.borderRadius = '6px';
  }
  // Reposition on toggle and resize
  const origToggle = document.getElementById('tagOverlay');
  new MutationObserver(()=>{ if(origToggle.classList.contains('show')) positionOverlay(); }).observe(origToggle, {attributes:true, attributeFilter:['class']});
  window.addEventListener('resize', positionOverlay);

  function toggleEE(){
    const none = document.getElementById('cb3a').checked;
    document.getElementById('eeDropDown').className = none ? 'd-none' : '';
    // If setting employee, reset source to internal (mutual exclusion)
    if(!none){
      const cb4a = document.getElementById('cb4a');
      if (cb4a) {
        cb4a.checked = true;
        document.getElementById('bpoDropDown').className = 'd-none';
      }
    }
  }
  function toggleBPO(){
    const none = document.getElementById('cb4a').checked;
    document.getElementById('bpoDropDown').className = none ? 'd-none' : '';
    // If setting source, reset employee to anyone
    if(!none){
      document.getElementById('cb3a').checked = true;
      document.getElementById('eeDropDown').className = 'd-none';
    }
  }
  function toggleGoTo(){
    const on = document.getElementById('cb5a').checked;
    const el = document.getElementById('goToPath');
    el.className = on ? 'form-control form-control-sm tm-url' : 'form-control form-control-sm tm-url d-none';
    el.required = on;
    if(!on) el.value = '';
  }
  function toggleInfo(){
    const on = document.getElementById('cb6a').checked;
    const el = document.getElementById('infoPath');
    el.className = on ? 'form-control form-control-sm tm-url' : 'form-control form-control-sm tm-url d-none';
    el.required = on;
    if(!on) el.value = '';
  }
</script>
</body>
</html>
