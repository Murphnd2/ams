<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"/>
  <title>PSP Dashboard</title>
  <style>
    /* ── Dashboard tokens ── */
    :root {
      --dash-renewal:     #0d6efd;
      --dash-setup:       #198754;
      --dash-ticket:      #6f42c1;
      --dash-opportunity: #87a948;
      --dash-overdue:     #dc3545;
      --dash-warning:     #ffc107;
      --dash-future:      var(--ssa);
      --dash-far:         #6c757d;
    }

    /* ── Stat cards ── */
    .dash-stat {
      border: 2px solid var(--ssa);
      border-radius: 10px;
      padding: 12px 16px;
      cursor: pointer;
      transition: all 0.15s ease;
      text-align: center;
      min-width: 0;
      flex: 1 1 0;
    }
    .dash-stat:hover { opacity: 0.85; }
    .dash-stat.active { color: #fff !important; }
    .dash-stat .stat-val { font-size: 1.6rem; font-weight: 700; line-height: 1; }
    .dash-stat .stat-lbl {
      font-size: 0.68rem; font-weight: 600; text-transform: uppercase;
      letter-spacing: 0.5px; margin-top: 3px; opacity: 0.85;
    }
    .dash-stat .stat-sub { font-size: 0.65rem; opacity: 0.7; margin-top: 1px; }

    /* ── Filter pills ── */
    .dash-pill {
      border: 1px solid var(--ssa);
      border-radius: 20px;
      padding: 3px 13px;
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
      background: #fff;
      color: var(--ssa);
      transition: all 0.12s ease;
    }
    .dash-pill:hover { opacity: 0.8; }
    .dash-pill.active { background: var(--ssa); color: #fff; }

    /* ── Activity rows ── */
    .dash-act-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 7px 12px;
      border-bottom: 1px solid #dee2e6;
      font-size: 0.82rem;
      cursor: pointer;
      transition: background 0.1s;
    }
    .dash-act-row:hover { background: #f0f4f8; }
    .dash-act-row:last-child { border-bottom: none; }

    /* ── Type badges ── */
    .dtype-badge {
      font-size: 0.65rem; font-weight: 600; padding: 1px 7px;
      border-radius: 10px; white-space: nowrap;
    }
    .dtype-Renewal     { background: rgba(13,110,253,0.12); color: var(--dash-renewal); }
    .dtype-Setup       { background: rgba(25,135,84,0.12);  color: var(--dash-setup); }
    .dtype-Ticket      { background: rgba(111,66,193,0.12); color: var(--dash-ticket); }
    .dtype-Opportunity { background: rgba(135,169,72,0.15); color: var(--dash-opportunity); }

    .badge-onus   { background: rgba(220,53,69,0.12); color: #dc3545; font-size: 0.65rem; padding: 1px 7px; border-radius: 10px; }
    .badge-quiet  { background: rgba(255,193,7,0.18); color: #856404; font-size: 0.65rem; padding: 1px 7px; border-radius: 10px; }

    /* ── Due bucket headers ── */
    .bucket-hdr {
      padding: 5px 14px;
      font-size: 0.68rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      display: flex;
      justify-content: space-between;
    }
    .bucket-3 { background: rgba(220,53,69,0.08); border-left: 3px solid var(--dash-overdue); color: var(--dash-overdue); }
    .bucket-2 { background: rgba(255,193,7,0.08); border-left: 3px solid var(--dash-warning); color: #856404; }
    .bucket-1 { background: rgba(13,86,129,0.06); border-left: 3px solid var(--dash-future); color: var(--dash-future); }
    .bucket-0 { background: rgba(108,117,125,0.06); border-left: 3px solid var(--dash-far); color: var(--dash-far); }

    /* ── Section headers ── */
    .dash-section-hdr {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 9px 14px;
      background: #f8f9fa;
      border-bottom: 2px solid var(--ssa);
    }
    .dash-section-hdr .hdr-title {
      font-weight: 700; font-size: 0.85rem; color: var(--ssa);
    }
    .dash-section-hdr .hdr-count {
      background: var(--ssa); color: #fff;
      font-size: 0.68rem; font-weight: 700;
      padding: 1px 7px; border-radius: 10px;
      margin-left: 6px;
    }

    /* ── Scrollable panels ── */
    .dash-scroll { overflow-y: auto; }

    /* ── Team workload ── */
    .team-row {
      display: flex; align-items: center; gap: 10px;
      padding: 7px 6px; border-bottom: 1px solid #dee2e6;
      cursor: pointer; border-radius: 4px; transition: background 0.1s;
    }
    .team-row:hover { background: #f0f4f8; }
    .team-row.active { background: rgba(13,86,129,0.07); }
    .team-avatar {
      width: 30px; height: 30px; border-radius: 50%;
      background: var(--ssa); color: #fff;
      display: flex; align-items: center; justify-content: center;
      font-size: 0.7rem; font-weight: 700;
    }
    .team-mini-box {
      width: 18px; height: 18px; border-radius: 3px;
      font-size: 0.6rem; font-weight: 700;
      display: flex; align-items: center; justify-content: center;
    }

    /* ── Prospect grid ── */
    .prospect-box {
      padding: 10px 12px; background: #f8f9fa;
      border-radius: 6px; text-align: center;
    }
    .prospect-box .pval { font-size: 1.2rem; font-weight: 700; color: var(--ssa); }
    .prospect-box .plbl { font-size: 0.62rem; font-weight: 600; text-transform: uppercase; color: #6c757d; }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/css-js.jsp"/>

  <%-- ═══ PAGE HEADER ═══ --%>
  <div class="rounded-3 mb-3 px-4 py-3"
       style="background: linear-gradient(135deg, var(--ssa) 0%, #0a4568 100%); color: #fff;">
    <div class="d-flex align-items-center justify-content-between">
      <div>
        <h5 class="fw-bold mb-0"><i class="bi bi-speedometer2 me-2"></i>PSP Dashboard</h5>
        <small class="opacity-75">Activity overview for all open items</small>
      </div>
      <div class="d-flex align-items-center gap-3">
        <small class="opacity-75">
          <fmt:formatDate value="<%= new java.util.Date() %>" pattern="EEEE, MMMM d, yyyy"/>
        </small>
        <a href="ViewHome25" class="btn btn-sm btn-outline-light" style="opacity:0.7;">
          <i class="bi bi-house me-1"></i>Home
        </a>
      </div>
    </div>
  </div>

  <%-- ═══ SUMMARY STAT CARDS ═══ --%>
  <div class="d-flex gap-2 mb-3 flex-wrap" id="statCards">
    <%-- Populated by JS from the data array --%>
  </div>

  <%-- ═══ FILTER BAR ═══ --%>
  <div class="d-flex align-items-center gap-2 mb-3 flex-wrap" id="filterBar">
    <span class="text-muted fw-semibold" style="font-size:0.72rem;">OWNER:</span>
    <span class="dash-pill active" data-owner="all">All</span>
    <c:forEach var="staff" items="${requestScope.dashboardStaff}">
      <span class="dash-pill" data-owner="${staff.getPerson().getId()}">
        ${staff.getPerson().getFirstName()}
      </span>
    </c:forEach>
    <span style="width:1px;height:20px;background:#dee2e6;margin:0 6px;"></span>
    <span class="text-muted fw-semibold" style="font-size:0.72rem;">STAGE:</span>
    <span class="dash-pill active" data-stage="all">All</span>
    <span class="dash-pill" data-stage="waitingOnUs">Waiting on Us</span>
    <span class="dash-pill" data-stage="needsAttention">Needs Attention (7d+)</span>
    <span class="dash-pill" data-stage="overdue">Overdue</span>
  </div>

  <%-- ═══ TWO-COLUMN LAYOUT ═══ --%>
  <div class="row g-3">
    <%-- LEFT: Activity List --%>
    <div class="col-lg-8">
      <div class="card border rounded-3 overflow-hidden">
        <div class="dash-section-hdr">
          <div>
            <i class="bi bi-list-check me-1"></i>
            <span class="hdr-title">Open Activities</span>
            <span class="hdr-count" id="filteredCount">0</span>
          </div>
          <div id="activeFilterBadges"></div>
        </div>
        <div class="dash-scroll" id="activityListContainer" style="max-height: 600px;">
          <%-- Populated by JS --%>
        </div>
      </div>
    </div>

    <%-- RIGHT SIDEBAR --%>
    <div class="col-lg-4">
      <%-- Team Workload --%>
      <div class="card border rounded-3 overflow-hidden mb-3">
        <div class="dash-section-hdr">
          <div>
            <i class="bi bi-people me-1"></i>
            <span class="hdr-title">Team Workload</span>
          </div>
        </div>
        <div class="dash-scroll p-2" id="teamPanel" style="max-height: 280px;">
          <%-- Populated by JS --%>
        </div>
      </div>

      <%-- Agent Pipeline (placeholder) --%>
      <div class="card border rounded-3 overflow-hidden mb-3">
        <div class="dash-section-hdr">
          <div>
            <i class="bi bi-briefcase me-1"></i>
            <span class="hdr-title">Agent Pipeline</span>
          </div>
        </div>
        <div class="dash-scroll p-3" style="max-height: 200px;">
          <div class="text-muted fst-italic" style="font-size:0.82rem;">
            <i class="bi bi-info-circle me-1"></i>Agent and prospect data coming soon.
          </div>
        </div>
      </div>

      <%-- BPO Vendors (placeholder) --%>
      <div class="card border rounded-3 overflow-hidden mb-3">
        <div class="dash-section-hdr">
          <div>
            <i class="bi bi-link-45deg me-1"></i>
            <span class="hdr-title">BPO Vendors</span>
          </div>
        </div>
        <div class="dash-scroll p-3" style="max-height: 200px;">
          <div class="text-muted fst-italic" style="font-size:0.82rem;">
            <i class="bi bi-info-circle me-1"></i>BPO registration and sync status coming soon.
          </div>
        </div>
      </div>

      <%-- Prospect Overview (placeholder) --%>
      <div class="card border rounded-3 overflow-hidden mb-3">
        <div class="dash-section-hdr">
          <div>
            <i class="bi bi-bullseye me-1"></i>
            <span class="hdr-title">Prospect Overview</span>
          </div>
        </div>
        <div class="p-3">
          <div class="text-muted fst-italic" style="font-size:0.82rem;">
            <i class="bi bi-info-circle me-1"></i>Prospect and application stats coming soon.
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<script>
  // ════════════════════════════════════════════
  //  Hydrate activity data from server
  // ════════════════════════════════════════════
  var activities = [];
  <c:forEach var="a" items="${requestScope.dashboardActivities}">
  activities.push({
    id: <c:out value="${a.getActivityId()}"/>,
    dtype: "<c:out value='${a.getDtype()}'/>",
    name: "<c:out value='${a.getFullName()}' escapeXml='true'/>".replace(/&amp;/g,"&").replace(/&#039;/g,"'").replace(/&quot;/g,'"'),
    assignedToId: <c:choose><c:when test="${a.getAssignedToId() != null}">${a.getAssignedToId()}</c:when><c:otherwise>null</c:otherwise></c:choose>,
    dueDate: "<c:out value='${a.getDueDate()}'/>",
    waitingOnUs: ${a.isWaitingOnUs()},
    daysSince: ${a.getDaysSinceContact()},
    dueBucket: ${a.getDueBucket()},
    oppStage: "<c:out value='${a.getOpportunityStage()}' default=''/>"
  });
  </c:forEach>

  var staff = [];
  <c:forEach var="s" items="${requestScope.dashboardStaff}">
  staff.push({
    personId: ${s.getPerson().getId()},
    name: "<c:out value='${s.getPerson().getFullName()}' escapeXml='true'/>",
    firstName: "<c:out value='${s.getPerson().getFirstName()}' escapeXml='true'/>"
  });
  </c:forEach>

  // ════════════════════════════════════════════
  //  State
  // ════════════════════════════════════════════
  let typeFilter = 'all';
  let ownerFilter = 'all';
  let stageFilter = 'all';

  // ════════════════════════════════════════════
  //  Helpers
  // ════════════════════════════════════════════
  const bucketLabel = {3:'Overdue', 2:'Due This Month', 1:'Coming Up', 0:'Future'};
  const bucketClass = {3:'bucket-3', 2:'bucket-2', 1:'bucket-1', 0:'bucket-0'};
  const dtypeIcon = {Renewal:'bi-arrow-repeat', Setup:'bi-gear', Ticket:'bi-ticket-perforated', Opportunity:'bi-briefcase'};
  const dtypeShort = {Renewal:'R', Setup:'S', Ticket:'T', Opportunity:'O'};
  const statBorder = {
    all:'var(--ssa)', Renewal:'var(--dash-renewal)', Setup:'var(--dash-setup)',
    Ticket:'var(--dash-ticket)', Opportunity:'var(--dash-opportunity)',
    waitingOnUs:'var(--dash-overdue)', overdue:'var(--dash-overdue)'
  };

  function initials(name) {
    return name.split(' ').map(w => w[0]).join('').substring(0,2);
  }

  function formatDate(d) {
    if (!d) return '';
    const dt = new Date(d + 'T00:00:00');
    return dt.toLocaleDateString('en-US', {month:'short', day:'numeric'});
  }

  function filtered() {
    return activities.filter(a => {
      if (typeFilter !== 'all' && a.dtype !== typeFilter) return false;
      if (ownerFilter !== 'all' && a.assignedToId !== parseInt(ownerFilter)) return false;
      if (stageFilter === 'waitingOnUs' && !a.waitingOnUs) return false;
      if (stageFilter === 'needsAttention' && a.daysSince < 7) return false;
      if (stageFilter === 'overdue' && a.dueBucket !== 3) return false;
      return true;
    });
  }

  // ════════════════════════════════════════════
  //  Render: Stat Cards
  // ════════════════════════════════════════════
  function renderStats() {
    const cards = [
      {label:'Total Open', value: activities.length, color:'var(--ssa)', filter:'type', key:'all'},
      {label:'Renewals', value: activities.filter(a=>a.dtype==='Renewal').length, color:'var(--dash-renewal)', filter:'type', key:'Renewal'},
      {label:'Setups', value: activities.filter(a=>a.dtype==='Setup').length, color:'var(--dash-setup)', filter:'type', key:'Setup'},
      {label:'Tickets', value: activities.filter(a=>a.dtype==='Ticket').length, color:'var(--dash-ticket)', filter:'type', key:'Ticket'},
      {label:'Opportunities', value: activities.filter(a=>a.dtype==='Opportunity').length, color:'var(--dash-opportunity)', filter:'type', key:'Opportunity'},
      {label:'Waiting on Us', value: activities.filter(a=>a.waitingOnUs).length, color:'var(--dash-overdue)', filter:'stage', key:'waitingOnUs', sub:'needs our action'},
      {label:'Overdue', value: activities.filter(a=>a.dueBucket===3).length, color:'var(--dash-overdue)', filter:'stage', key:'overdue'},
    ];

    const container = document.getElementById('statCards');
    container.innerHTML = cards.map(function(c) {
      var isActive = (c.filter === 'type' && typeFilter === c.key) ||
                      (c.filter === 'stage' && stageFilter === c.key && typeFilter === 'all');
      return '<div class="dash-stat' + (isActive ? ' active' : '') + '"'
        + ' style="border-color:' + c.color + ';' + (isActive ? 'background:' + c.color : '') + '"'
        + ' data-filter="' + c.filter + '" data-key="' + c.key + '">'
        + '<div class="stat-val">' + c.value + '</div>'
        + '<div class="stat-lbl">' + c.label + '</div>'
        + (c.sub ? '<div class="stat-sub">' + c.sub + '</div>' : '')
        + '</div>';
    }).join('');

    container.querySelectorAll('.dash-stat').forEach(el => {
      el.addEventListener('click', () => {
        const f = el.dataset.filter;
        const k = el.dataset.key;
        if (f === 'type') {
          typeFilter = typeFilter === k ? 'all' : k;
          if (typeFilter !== 'all') stageFilter = 'all';
        } else {
          stageFilter = stageFilter === k ? 'all' : k;
          if (stageFilter !== 'all') typeFilter = 'all';
        }
        renderAll();
      });
    });
  }

  // ════════════════════════════════════════════
  //  Render: Filter pills
  // ════════════════════════════════════════════
  function renderFilters() {
    document.querySelectorAll('[data-owner]').forEach(el => {
      el.classList.toggle('active', ownerFilter === el.dataset.owner);
    });
    document.querySelectorAll('[data-stage]').forEach(el => {
      el.classList.toggle('active', stageFilter === el.dataset.stage);
    });
  }

  // ════════════════════════════════════════════
  //  Render: Activity List
  // ════════════════════════════════════════════
  function renderActivities() {
    const list = filtered();
    document.getElementById('filteredCount').textContent = list.length;

    // Group by bucket (3=overdue first, then 2,1,0)
    const buckets = {3:[], 2:[], 1:[], 0:[]};
    list.forEach(a => {
      const b = a.dueBucket;
      if (buckets[b]) buckets[b].push(a);
      else buckets[0].push(a);
    });

    const container = document.getElementById('activityListContainer');
    if (list.length === 0) {
      container.innerHTML = '<div class="p-4 text-center text-muted" style="font-size:0.85rem;">No activities match the current filters.</div>';
      return;
    }

    let html = '';
    [3,2,1,0].forEach(function(b) {
      var items = buckets[b];
      if (items.length === 0) return;
      html += '<div class="bucket-hdr ' + bucketClass[b] + '">'
        + '<span>' + bucketLabel[b] + '</span><span>' + items.length + '</span>'
        + '</div>';
      items.forEach(function(a) {
        var staffMember = staff.find(function(s) { return s.personId === a.assignedToId; });
        var ownerName = staffMember ? staffMember.firstName : '';
        var dateColor = a.dueBucket===3 ? 'var(--dash-overdue)' : a.dueBucket===2 ? '#856404' : 'var(--ssa)';
        html += '<div class="dash-act-row" onclick="location.href=\'ViewById?id=' + a.id + '\'">'
          + '<i class="bi ' + dtypeIcon[a.dtype] + '" style="font-size:0.9rem;width:20px;text-align:center;"></i>'
          + '<div class="flex-grow-1 text-truncate">'
          + '<div class="fw-semibold text-truncate">' + a.name + '</div>'
          + '</div>'
          + '<span class="dtype-badge dtype-' + a.dtype + '">' + a.dtype + '</span>'
          + (a.waitingOnUs ? '<span class="badge-onus">On Us</span>' : '')
          + (a.daysSince >= 7 ? '<span class="badge-quiet">' + a.daysSince + 'd quiet</span>' : '')
          + '<span style="width:75px;text-align:right;font-size:0.78rem;font-weight:600;color:' + dateColor + '">'
          + formatDate(a.dueDate)
          + '</span>'
          + '<span style="width:80px;font-size:0.78rem;color:#6c757d;text-align:right;">' + ownerName + '</span>'
          + '</div>';
      });
    });
    container.innerHTML = html;
  }

  // ════════════════════════════════════════════
  //  Render: Team Workload
  // ════════════════════════════════════════════
  function renderTeam() {
    const container = document.getElementById('teamPanel');
    let html = '';
    staff.forEach(function(s) {
      var mine = activities.filter(function(a) { return a.assignedToId === s.personId; });
      var onUs = mine.filter(function(a) { return a.waitingOnUs; }).length;
      var overdue = mine.filter(function(a) { return a.dueBucket === 3; }).length;
      var isActive = ownerFilter === String(s.personId);

      // Mini type boxes
      var miniHtml = '';
      ['Renewal','Setup','Ticket','Opportunity'].forEach(function(t) {
        var c = mine.filter(function(a) { return a.dtype === t; }).length;
        if (c > 0) {
          var color = t === 'Renewal' ? 'var(--dash-renewal)' :
                      t === 'Setup' ? 'var(--dash-setup)' :
                      t === 'Ticket' ? 'var(--dash-ticket)' : 'var(--dash-opportunity)';
          miniHtml += '<div class="team-mini-box" style="background:' + color + '20;color:' + color + ';">' + c + '</div>';
        }
      });

      html += '<div class="team-row' + (isActive ? ' active' : '') + '" data-team-id="' + s.personId + '">'
        + '<div class="team-avatar">' + initials(s.name) + '</div>'
        + '<div class="flex-grow-1">'
        + '<div style="font-size:0.82rem;font-weight:600;">' + s.name + '</div>'
        + '<div style="font-size:0.72rem;color:#6c757d;">'
        + mine.length + ' open'
        + (onUs > 0 ? ' &middot; <span style="color:var(--dash-overdue)">' + onUs + ' on us</span>' : '')
        + (overdue > 0 ? ' &middot; <span style="color:var(--dash-overdue)">' + overdue + ' overdue</span>' : '')
        + '</div>'
        + '</div>'
        + '<div class="d-flex gap-1">' + miniHtml + '</div>'
        + '</div>';
    });
    container.innerHTML = html;

    // Click handlers
    container.querySelectorAll('.team-row').forEach(function(el) {
      el.addEventListener('click', function() {
        var id = el.dataset.teamId;
        ownerFilter = ownerFilter === id ? 'all' : id;
        renderAll();
      });
    });
  }

  // ════════════════════════════════════════════
  //  Render all
  // ════════════════════════════════════════════
  function renderAll() {
    renderStats();
    renderFilters();
    renderActivities();
    renderTeam();
  }

  // ════════════════════════════════════════════
  //  Event listeners
  // ════════════════════════════════════════════
  document.addEventListener('DOMContentLoaded', () => {
    // Owner pills
    document.querySelectorAll('[data-owner]').forEach(el => {
      el.addEventListener('click', () => {
        ownerFilter = ownerFilter === el.dataset.owner ? 'all' : el.dataset.owner;
        renderAll();
      });
    });
    // Stage pills
    document.querySelectorAll('[data-stage]').forEach(el => {
      el.addEventListener('click', () => {
        stageFilter = stageFilter === el.dataset.stage ? 'all' : el.dataset.stage;
        renderAll();
      });
    });

    renderAll();
  });
</script>
</body>
</html>
