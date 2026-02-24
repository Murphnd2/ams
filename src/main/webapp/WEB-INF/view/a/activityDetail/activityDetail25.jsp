<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${sessionScope.psp.getFullName()}</title>
  <style>
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
      #actLayout { display: flex; height: calc(100vh - 70px); overflow: hidden; }
      #panelLeft { display: flex; flex-direction: column; overflow: hidden; min-width: 180px; flex: 0 0 22%; }
      #panelCenter { overflow: hidden auto; min-width: 200px; flex: 1 1 auto; }
      #panelCenter * { box-sizing: border-box; }
      #panelCenter .card, #panelCenter .hdr-bar { max-width: 100%; }
      #panelRight { display: flex; flex-direction: column; overflow: hidden; min-width: 200px; flex: 0 0 30%; }
      #panelRight .history-scroll { flex-grow: 1; overflow: auto; }
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
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <c:choose>
    <c:when test="${sessionScope.isPspUser || sessionScope.isPspAdmin || sessionScope.isAgent || sessionScope.isAgencyAdmin}">

      <div id="actLayout">
        <div id="panelLeft">
          <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistHeader.jsp"></c:import>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistBasic25.jsp"></c:import>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistFooter25.jsp"></c:import>
          </c:if>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
            <c:import url="/WEB-INF/view/a/checklistDetail/modifyRecurring25.jsp"></c:import>
          </c:if>
        </div>

        <div class="panel-divider" id="dividerLeft"></div>

        <div id="panelCenter">
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailHeader25.jsp"></c:import>
          <c:if test="${!sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailPrimaryContact25.jsp"></c:import>
            <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailAdditionalContacts25.jsp"></c:import>
          </c:if>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailDetail25.jsp"></c:import>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailDocsLinks25.jsp"></c:import>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailFooter25.jsp"></c:import>
        </div>

        <div class="panel-divider" id="dividerRight"></div>

        <div id="panelRight">
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
    if (saved) {
      try {
        var w = JSON.parse(saved);
        left.style.flex = '0 0 ' + w.left + 'px';
        right.style.flex = '0 0 ' + w.right + 'px';
        center.style.flex = '1 1 auto';
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
    initDrag(divR, right, false);
  })();
</script>
</body>
</html>