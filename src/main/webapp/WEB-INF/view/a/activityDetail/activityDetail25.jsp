<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${sessionScope.psp.getFullName()}</title>
  <style>
    body { background-color: #eef0f4; }

    /* ── Breadcrumb bar ── */
    .detail-crumb-bar {
      background: #f5f7fb;
      border-bottom: 1px solid #e5eaf1;
      padding: 4px 14px;
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.68rem;
      color: #7a8fa0;
    }
    .detail-crumb-bar a { color: var(--ssa); text-decoration: none; font-weight: 500; }
    .detail-crumb-bar a:hover { text-decoration: underline; }
    .crumb-sep { color: #c4ced8; }
    .crumb-overdue { margin-left: auto; color: #dc2626; font-weight: 600; font-size: 0.65rem; }

    /* ── Inline status bar ── */
    .detail-status-bar {
      background: #f8fafc;
      border-bottom: 1px solid #e5eaf1;
      padding: 4px 12px;
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 0.67rem;
      color: #6b7280;
      flex-wrap: wrap;
    }
    .dsb-item { display: flex; align-items: center; gap: 3px; }
    .dsb-icon { font-size: 0.78rem; color: var(--ssa); }
    .dsb-sep { width: 1px; height: 12px; background: #dde3ea; flex-shrink: 0; }
    .dsb-status-pill {
      font-size: 0.62rem; font-weight: 600; padding: 0.15rem 0.5rem;
      border-radius: 8px; background: #fef3c7; color: #92400e;
    }

    /* ── Tonal Zones: three-panel layout ── */

    /* LEFT panel: steel blue-grey = operational/task zone */
    .detail-panel-left { background: #e8eef5; }
    .detail-panel-left-stripe { height: 3px; background: #4a7fa5; }
    .detail-panel-left .hdr-bar {
      background: #3d5a73 !important; color: white !important;
      border-bottom: 1px solid #2e4558 !important;
      position: sticky; top: 0; z-index: 10;
    }
    .detail-panel-left .hdr-bar i { color: rgba(255,255,255,0.8); }
    .detail-panel-left .hdr-bar button,
    .detail-panel-left .hdr-bar .ghost-action { color: rgba(255,255,255,0.7) !important; }
    .detail-panel-left .hdr-bar button:hover,
    .detail-panel-left .hdr-bar .ghost-action:hover { color: white !important; background: rgba(255,255,255,0.12) !important; }
    /* Task rows in left panel */
    .detail-panel-left .td-item { border-bottom-color: #d0dce8; color: #2d4052; }
    .detail-panel-left .td-item:hover { background: #dce8f0; }
    /* BPO/sourced task visual grouping */
    .detail-panel-left .td-item.sourced { background: rgba(74,127,165,0.07); }
    .detail-panel-left .td-item.sourced:hover { background: rgba(74,127,165,0.13); }
    .bpo-badge {
      font-size: 0.58rem; font-weight: 700; padding: 1px 5px; border-radius: 3px;
      background: #dbeafe; color: #1e40af; flex-shrink: 0;
    }
    .bpo-done-badge {
      font-size: 0.58rem; font-weight: 700; padding: 1px 5px; border-radius: 3px;
      background: #d1fae5; color: #065f46; flex-shrink: 0;
    }
    /* Section labels in left panel */
    .detail-panel-left .task-section-label {
      font-size: 0.59rem; font-weight: 700; letter-spacing: 0.07em;
      text-transform: uppercase; padding: 3px 10px; border-bottom: 1px solid #cdd8e4;
      color: #6a8ea8; background: rgba(0,0,0,0.03);
    }

    /* CENTER panel: white = reference/data zone */
    .detail-panel-center { background: white; }
    .detail-panel-center-stripe { height: 3px; background: var(--ssa); }
    /* Center section card headers */
    .detail-section-header {
      background: #f7f9fc; border-bottom: 1px solid #edf0f5;
      padding: 6px 12px; display: flex; align-items: center; gap: 6px;
      font-size: 0.71rem; font-weight: 700; color: #374151;
    }
    .detail-section-header i { color: var(--ssa); }
    .detail-section-header .section-end { margin-left: auto; }
    .detail-section-card { border-bottom: 1px solid #f0f3f7; }
    .primary-contact-row {
      display: flex; align-items: center; gap: 8px; padding: 7px 12px;
      background: #f8fafc; border-left: 3px solid var(--ssa);
    }

    /* RIGHT panel: warm cream = communication zone */
    .detail-panel-right { background: #fffdf7; }
    .detail-panel-right-stripe { height: 3px; background: #d97706; }
    .detail-panel-right .hdr-bar {
      background: #fff8ed !important; color: #92400e !important;
      border-bottom: 1px solid #fde68a !important;
    }
    .detail-panel-right .hdr-bar i { color: #d97706 !important; }
    .detail-panel-right .hdr-bar button,
    .detail-panel-right .hdr-bar .ghost-action { color: #d97706 !important; }
    .detail-panel-right .hdr-bar button:hover,
    .detail-panel-right .hdr-bar .ghost-action:hover { color: #92400e !important; background: #fef3c7 !important; }
    /* Sticky history section header */
    .detail-history-sticky-header {
      position: sticky; top: 0; z-index: 10;
      background: #fff8ed; color: #92400e;
      border-bottom: 1px solid #fde68a; border-top: 1px solid #fde68a;
      padding: 6px 10px; display: flex; align-items: center; gap: 6px;
      font-size: 0.72rem; font-weight: 600;
    }
    .detail-history-sticky-header i { color: #d97706; }
    /* History note rows */
    .history-note-row { padding: 7px 10px; border-bottom: 1px solid #f5ead8; background: white; }
    .history-note-row:hover { background: #fffdf2; }
    /* Note compose area */
    .note-compose-area { padding: 8px 10px; border-bottom: 1px solid #f5e8c8; background: #fffdf7; }

    /* ── Responsive: collapse zones on mobile ── */
    @media (max-width: 1199.98px) {
      .detail-panel-left, .detail-panel-center, .detail-panel-right {
        background: white; border-bottom: 2px solid #eef0f4;
      }
      .detail-panel-left .hdr-bar, .detail-panel-right .hdr-bar {
        background: var(--ssa) !important; color: white !important;
        border-bottom: 1px solid rgba(255,255,255,0.15) !important;
      }
      .detail-panel-left .hdr-bar i, .detail-panel-right .hdr-bar i { color: white !important; }
      .detail-panel-left .hdr-bar button, .detail-panel-right .hdr-bar button { color: rgba(255,255,255,0.8) !important; }
      .detail-panel-left-stripe, .detail-panel-center-stripe, .detail-panel-right-stripe { display: none; }
      .detail-crumb-bar { display: none; }
    }

    .panel-divider {
      width: 9px;
      cursor: col-resize;
      flex-shrink: 0;
      position: relative;
      background: transparent;
    }
    .panel-divider::after {
      content: '';
      position: absolute;
      top: 0;
      bottom: 0;
      left: 3px;
      width: 3px;
      background: #dee2e6;
      transition: background 0.15s;
    }
    .panel-divider:hover::after, .panel-divider.active::after {
      background: var(--ssa);
    }
    .panel-divider::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 3px;
      transform: translateY(-50%);
      width: 3px;
      height: 30px;
      z-index: 1;
      background: repeating-linear-gradient(
              to bottom, transparent, transparent 3px, #adb5bd 3px, #adb5bd 5px
      );
      border-radius: 1px;
    }
    .panel-divider:hover::before, .panel-divider.active::before {
      background: repeating-linear-gradient(
              to bottom, transparent, transparent 3px, white 3px, white 5px
      );
    }

    /* Desktop: side-by-side flex panels */
    @media (min-width: 1200px) {
      #actLayout { display: flex; flex-wrap: nowrap; height: calc(100vh - 80px); overflow: hidden; }
      #panelLeft, #panelCenter, #panelRight { box-sizing: border-box; }
      #panelLeft { display: flex; flex-direction: column; overflow: hidden; min-width: 180px; flex: 0 0 22%; }
      #panelCenter { overflow: hidden auto; min-width: 200px; flex: 1 1 auto; }
      #panelCenter * { box-sizing: border-box; }
      #panelCenter .card, #panelCenter .hdr-bar { max-width: 100%; }
      #panelRight { display: flex; flex-direction: column; overflow: hidden; min-width: 200px; flex: 0 0 30%; }
      #panelRight .history-scroll { flex-grow: 1; overflow: auto; }
      body.checklist-mode #panelRight { flex: 1 1 auto !important; min-width: 300px; }
    }

    /* Tablet/Mobile: stacked, dividers hidden */
    @media (max-width: 1199.98px) {
      #actLayout { display: flex; flex-wrap: wrap; }
      #panelCenter { order: -1; width: 100%; }
      #panelLeft { width: 50%; }
      #panelRight { width: 50%; }
      .panel-divider { display: none; }
      #panelRight .history-scroll { max-height: 400px; overflow-y: auto; }
    }

    @media (max-width: 767.98px) {
      #panelLeft, #panelCenter, #panelRight { width: 100%; }
    }
  </style>
</head>
<body class="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals('CheckList') ? 'checklist-mode' : ''}">
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <c:choose>
    <c:when test="${sessionScope.isPspUser || sessionScope.isPspAdmin || sessionScope.isAgent || sessionScope.isAgencyAdmin || sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser}">

      <%-- ① Breadcrumb bar: back navigation + overdue urgency signal --%>
      <div class="detail-crumb-bar">
        <a href="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName() == 'Opportunity' && !sessionScope.isPspAdmin && !sessionScope.isPspUser ? 'AgentHome' : 'ViewHome25'}"><i class="bi bi-arrow-left me-1"></i>Home</a>
        <span class="crumb-sep">/</span>
        <span>${sessionScope.local.getCurrentActivity().getActivity().getFullName()}</span>
        <c:if test="${not empty daysUntilDue && daysUntilDue < 0}">
          <span class="crumb-overdue">
            <i class="bi bi-exclamation-circle me-1"></i>${-daysUntilDue} days overdue
          </span>
        </c:if>
      </div>

      <div id="actLayout">
        <div id="panelLeft" class="detail-panel-left">
          <div class="detail-panel-left-stripe"></div>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistHeader.jsp"></c:import>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistBasic25.jsp"></c:import>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistFooter25.jsp"></c:import>
          </c:if>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
            <c:import url="/WEB-INF/view/a/checklistDetail/modifyRecurring25.jsp"></c:import>
          </c:if>
        </div>

        <c:choose>
          <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals('CheckList')}">
            <c:choose>
              <c:when test="${sessionScope.local.getCurrentActivity().getActivity().recurringTaskList != null}">
                <%-- Recurring CheckList: show center panel with history --%>
                <div class="panel-divider" id="dividerLeft"></div>
                <div id="panelCenter" class="d-flex flex-column" style="min-height:0;">
                  <c:import url="/WEB-INF/view/a/checklistDetail/checklistHistory25.jsp"/>
                </div>
                <div class="panel-divider" id="dividerRight"></div>
              </c:when>
              <c:otherwise>
                <%-- Non-recurring CheckList: no center panel --%>
                <div class="panel-divider" id="dividerLeft"></div>
              </c:otherwise>
            </c:choose>
          </c:when>
          <c:otherwise>
            <div class="panel-divider" id="dividerLeft"></div>
            <div id="panelCenter" class="detail-panel-center">
              <div class="detail-panel-center-stripe"></div>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailHeader25.jsp"></c:import>
              <%-- ② Inline status bar --%>
              <div class="detail-status-bar">
                <span class="dsb-item">
                  <i class="bi bi-person-fill dsb-icon"></i>
                  <span>${sessionScope.local.getCurrentActivity().getActivity().getLoggedBy().getFirstName()} ${sessionScope.local.getCurrentActivity().getActivity().getLoggedBy().getLastName()}</span>
                </span>
                <div class="dsb-sep"></div>
                <span class="dsb-item">
                  <c:choose>
                    <c:when test="${not empty daysUntilDue && daysUntilDue < 0}">
                      <i class="bi bi-calendar3 dsb-icon" style="color:#dc2626"></i>
                      <span style="color:#dc2626; font-weight:600">
                        <fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDueDate()}" pattern="MMM d"/> &mdash; ${-daysUntilDue}d overdue
                      </span>
                    </c:when>
                    <c:when test="${not empty daysUntilDue && daysUntilDue == 0}">
                      <i class="bi bi-calendar3 dsb-icon" style="color:#d97706"></i>
                      <span style="color:#d97706; font-weight:600">Due today</span>
                    </c:when>
                    <c:when test="${not empty daysUntilDue && daysUntilDue == 1}">
                      <i class="bi bi-calendar3 dsb-icon" style="color:#d97706"></i>
                      <span style="color:#d97706; font-weight:600">Due tomorrow</span>
                    </c:when>
                    <c:when test="${not empty daysUntilDue}">
                      <i class="bi bi-calendar3 dsb-icon"></i>
                      <fmt:formatDate value="${sessionScope.local.getCurrentActivity().getActivity().getDueDate()}" pattern="MMM d"/>
                    </c:when>
                    <c:otherwise>
                      <i class="bi bi-calendar3 dsb-icon"></i>
                      <span class="text-muted">No due date</span>
                    </c:otherwise>
                  </c:choose>
                </span>
                <c:if test="${sessionScope.local.getCurrentActivity().getActivity().getAssignedTo() != null}">
                  <div class="dsb-sep"></div>
                  <span class="dsb-item">
                    <i class="bi bi-person-badge dsb-icon"></i>
                    <span>${sessionScope.local.getCurrentActivity().getActivity().getAssignedTo().getFirstName()} ${sessionScope.local.getCurrentActivity().getActivity().getAssignedTo().getLastName()}</span>
                  </span>
                </c:if>
              </div>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailPrimaryContact25.jsp"></c:import>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailDetail25.jsp"></c:import>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailQuestionnaires25.jsp"></c:import>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailDocsLinks25.jsp"></c:import>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailAdditionalContacts25.jsp"></c:import>
              <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailFooter25.jsp"></c:import>
            </div>
            <div class="panel-divider" id="dividerRight"></div>
          </c:otherwise>
        </c:choose>

        <div id="panelRight" class="detail-panel-right">
          <div class="detail-panel-right-stripe"></div>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailAddNote25.jsp"></c:import>
          </c:if>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/history/historyHeader.jsp"></c:import>
          <div class="history-scroll">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/history/historyDetail25.jsp"></c:import>
          </div>
        </div>
      </div>

    </c:when>
  </c:choose>
</div>

<script>
  let hasUnsaved = false;
  function checkUnsaved() {
    const pending = ${not empty sessionScope.local.pendingCloseIds};
    hasUnsaved = pending;
    return pending;
  }
  document.querySelector('form[action="PersistChecklist25"] button')?.addEventListener('click', () => {
    hasUnsaved = false;
  });
  window.addEventListener('beforeunload', (e) => {
    if (hasUnsaved || checkUnsaved()) {
      document.getElementById('autoSaveForm').submit();
    }
  });
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden' && (hasUnsaved || checkUnsaved())) {
      navigator.sendBeacon('PersistChecklist25', new FormData(document.getElementById('autoSaveForm')));
    }
  });
  document.addEventListener('submit', (e) => {
    if (e.target.action.includes('CloseToDo25')) {
      setTimeout(checkUnsaved, 100);
    }
  });
</script>

<script>
  (function() {
    if (window.innerWidth < 1200) return;
    var container = document.getElementById('actLayout');
    if (!container) return;

    var left = document.getElementById('panelLeft');
    var center = document.getElementById('panelCenter');
    var right = document.getElementById('panelRight');
    var divL = document.getElementById('dividerLeft');
    var divR = document.getElementById('dividerRight');

    var saved = localStorage.getItem('actDetailPanels');
    if (saved && center) {
      try {
        var w = JSON.parse(saved);
        if (left) left.style.flex = '0 0 ' + w.left + 'px';
        if (right) right.style.flex = '0 0 ' + w.right + 'px';
        if (center) center.style.flex = '1 1 auto';
      } catch(e) {}
    }

    function initDrag(divider, sidePanel, isLeft) {
      var startX, startW;
      divider.addEventListener('mousedown', function(e) {
        e.preventDefault();
        startX = e.clientX;
        startW = sidePanel.getBoundingClientRect().width;
        divider.classList.add('active');
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';

        function onMove(e) {
          var delta = e.clientX - startX;
          var newW = isLeft ? startW + delta : startW - delta;
          var minW = parseInt(sidePanel.style.minWidth) || 180;
          var maxW = container.getBoundingClientRect().width * 0.5;
          newW = Math.max(minW, Math.min(maxW, newW));
          sidePanel.style.flex = '0 0 ' + newW + 'px';
        }

        function onUp() {
          divider.classList.remove('active');
          document.body.style.cursor = '';
          document.body.style.userSelect = '';
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
          localStorage.setItem('actDetailPanels', JSON.stringify({
            left: left.getBoundingClientRect().width,
            right: right.getBoundingClientRect().width
          }));
        }

        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      });
    }
    initDrag(divL, left, true);
    if (divR) initDrag(divR, right, false);
  })();
</script>
</body>
</html>