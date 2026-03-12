<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${sessionScope.local.getCurrentPerson().getPsp().getFullName()}</title>
  <script src="https://cdn.ckeditor.com/ckeditor5/36.0.1/classic/ckeditor.js"></script>
  <style>
    .ck-editor__editable_inline {
      max-width: 800px;
      width: 100%;
      margin: 0 auto;
      box-sizing: border-box;
      height:300px;
    }
    .ck.ck-toolbar {
      width: 100%;
      max-width: 800px;
      margin: 0;
      display: flex;
      flex-wrap: wrap;
      box-sizing: border-box;
    }

    /* ── Tonal Zone Layout ── */
    body { background-color: #eef0f4; }

    .home-zones-row {
        display: flex;
        align-items: stretch;
        height: calc(100vh - 62px);
        overflow: hidden;
    }
    .home-zones-row > [class*="col-"] {
        display: flex;
        flex-direction: column;
        min-height: 0;
    }

    /* Left column — warm amber */
    .home-col-left {
        background: #fffbf0;
        border-right: 1px solid #fde68a;
        overflow-y: auto;
    }
    .home-col-left .zone-stripe { height: 3px; background: #f59e0b; margin-bottom: 0; }
    .home-col-left .hdr-bar, .home-col-left .card-header {
        background: #fffbf0 !important;
        color: #92400e !important;
        border-bottom: 1px solid #fde68a !important;
    }
    .home-col-left .hdr-bar i, .home-col-left .card-header i { color: #d97706; }
    .home-col-left .ghost-action, .home-col-left .hdr-bar button {
        color: #d97706 !important;
    }
    .home-col-left .ghost-action:hover { color: #92400e !important; }
    .home-col-left .hdr-bar .btn-outline-light {
        border-color: #fde68a !important;
        color: #d97706 !important;
    }
    .home-col-left .hdr-bar .btn-outline-light:hover {
        background: rgba(245,158,11,0.08) !important;
    }
    .home-col-left .tc-status-pill.tc-pill-out {
        background: rgba(146,64,14,0.1);
        color: #92400e;
    }

    /* Center column — white (SSA blue header unchanged) */
    .home-col-center {
        background: white;
        border-left: 1px solid #e5eaf1;
        border-right: 1px solid #e5eaf1;
    }
    .home-col-center .zone-stripe { height: 3px; background: var(--ssa); margin-bottom: 0; }

    /* Right column — cool teal */
    .home-col-right {
        background: #f0fdfc;
        border-left: 1px solid #a7f3d0;
    }
    .home-col-right .zone-stripe { height: 3px; background: #0d9488; margin-bottom: 0; }
    .home-col-right .hdr-bar, .home-col-right .card-header {
        background: #f0fdfc !important;
        color: #065f46 !important;
        border-bottom: 1px solid #ccfbf1 !important;
    }
    .home-col-right .hdr-bar i, .home-col-right .card-header i { color: #0d9488; }
    .home-col-right .ghost-action, .home-col-right .hdr-bar button {
        color: #0d9488 !important;
    }
    .home-col-right .ghost-action:hover { color: #065f46 !important; }
    .home-col-right .hdr-bar .btn-outline-light {
        border-color: #a7f3d0 !important;
        color: #0d9488 !important;
    }
    .home-col-right .hdr-bar .btn-outline-light:hover {
        background: rgba(13,148,136,0.08) !important;
    }
    .home-col-right .todo-body {
        background: #f0fdfc;
        border-color: #ccfbf1;
    }

    /* Section labels inside tonal columns */
    .home-col-left .section-label,
    .home-col-right .section-label {
        background: rgba(0,0,0,0.025);
        color: #9ca3af;
        border-bottom: 1px solid rgba(0,0,0,0.04);
    }
    .home-col-right .td-section-toggle {
        background: rgba(0,0,0,0.025);
        color: #9ca3af;
        border-top-color: rgba(0,0,0,0.04);
    }
    .home-col-right .td-section-toggle:hover { color: #065f46; }
    .home-col-right .td-count {
        background: #ccfbf1;
        color: #065f46;
    }

    /* Quick-ticket create button in left column */
    .home-col-left .btn-ssa,
    .home-col-left .btn-quick-ticket {
        background: #f59e0b !important;
        border-color: #f59e0b !important;
        color: white !important;
    }
    .home-col-left .btn-ssa:hover,
    .home-col-left .btn-quick-ticket:hover {
        background: #d97706 !important;
        border-color: #d97706 !important;
    }

    /* Active count badges in right column header */
    .home-col-right .hdr-count-badge {
        background: #ccfbf1;
        color: #065f46;
        font-size: 0.65rem;
        padding: 1px 6px;
        border-radius: 8px;
        font-weight: 600;
    }

    /* Responsive: stack on small screens, reset tonal backgrounds */
    @media (max-width: 991px) {
        .home-zones-row { flex-direction: column; height: auto; overflow: visible; }
        .home-col-left, .home-col-center, .home-col-right {
            background: white;
            border: none;
            border-bottom: 2px solid #eef0f4;
        }
        .home-col-left .zone-stripe, .home-col-center .zone-stripe, .home-col-right .zone-stripe {
            display: none;
        }
        .home-col-left .hdr-bar, .home-col-left .card-header,
        .home-col-right .hdr-bar, .home-col-right .card-header {
            background: var(--ssa) !important;
            color: white !important;
            border-bottom: none !important;
        }
        .home-col-left .hdr-bar i, .home-col-left .card-header i,
        .home-col-right .hdr-bar i, .home-col-right .card-header i { color: white; }
        .home-col-left .ghost-action, .home-col-left .hdr-bar button,
        .home-col-right .ghost-action, .home-col-right .hdr-bar button {
            color: rgba(255,255,255,0.8) !important;
        }
        .home-col-left .hdr-bar .btn-outline-light,
        .home-col-right .hdr-bar .btn-outline-light {
            border-color: rgba(255,255,255,0.4) !important;
            color: rgba(255,255,255,0.85) !important;
        }
        .home-col-left .btn-ssa, .home-col-left .btn-quick-ticket {
            background: var(--ssa) !important;
            border-color: var(--ssa) !important;
        }
        .home-col-right .todo-body {
            background: #fff;
            border-color: #dde2e7;
        }
        .home-col-right .td-section-toggle {
            background: #fafbfc;
            color: #6c757d;
            border-top-color: #eee;
        }
        .home-col-right .td-count {
            background: #e8f0f7;
            color: #0d5681;
        }
    }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <div style="height: 6px; background: #eef0f4;"></div>
  <div class="row home-zones-row">
    <c:if test="${applicationScope.global.useTimeclock}">
    <%-- ************************ T I M E C L O C K   C O L U M N ********************************************************* --%>
    <div class="col-12 col-lg-7 col-xl-3 order-last home-col-left">
      <div class="zone-stripe"></div>
      <c:import url="/WEB-INF/view/a/pspHome/columns/timeClock/timeClockHeader.jsp"></c:import>
      <c:import url="/WEB-INF/view/a/pspHome/columns/timeClock/timeClockDetail25.jsp"></c:import>
      <c:import url="/WEB-INF/view/a/pspHome/columns/timeClock/timeCorrectionModal.jsp"/>
    </div>
    </c:if>
    <c:if test="${!applicationScope.global.useTimeclock}">
    <%-- ************************ Q U I C K   T I C K E T   C O L U M N ************************************************* --%>
    <div class="col-12 col-lg-7 col-xl-3 order-last home-col-left">
      <div class="zone-stripe"></div>
      <c:import url="/WEB-INF/view/a/pspHome/columns/quickTicket25.jsp"/>
    </div>
    </c:if>
      <%-- ************************ A C T I V I T Y   C O L U M N ********************************************************* --%>
      <div class="col-12 col-lg-7 col-xl-6 order-first order-xl-2 d-flex flex-column home-col-center" style="min-height: 400px;">
        <div class="zone-stripe"></div>
        <c:import url="/WEB-INF/view/a/pspHome/columns/activities/activityHeader25.jsp"></c:import>
        <c:import url="/WEB-INF/view/a/pspHome/columns/activities/activityList25.jsp"></c:import>
      </div>
      <%-- ************************ T O D O   C O L U M N ********************************************************* --%>
      <div class="col-12 col-lg-5 col-xl-3 order-2 order-xl-first d-flex flex-column home-col-right" style="min-height: 400px;">
        <div class="zone-stripe"></div>
        <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoHeader.jsp"></c:import>
        <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoCurrentList25.jsp"></c:import>
      </div>
  </div>
</div>
</body>
</html>
