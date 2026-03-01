<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<style>
  /* ─── Filter Surface ─── */
  .af-surface {
    background: #fff;
    border: 1px solid #dde2e7;
    border-top: none;
    overflow: hidden;
    transition: max-height 0.3s ease, padding 0.3s ease, opacity 0.25s ease;
  }
  .af-surface.collapsed {
    max-height: 0 !important;
    padding-top: 0 !important;
    padding-bottom: 0 !important;
    border-bottom-color: transparent;
    opacity: 0;
  }
  .af-surface.expanded {
    max-height: 500px;
    padding: 10px 12px 6px;
    opacity: 1;
    border-radius: 0 0 8px 8px;
  }

  /* Header bar radius adapts */
  .hdr-bar.af-panel-closed { border-radius: 8px; }
  .hdr-bar.af-panel-open   { border-radius: 8px 8px 0 0; }

  /* ─── Preset pills in header ─── */
  .af-hdr-sep {
    width: 1px; height: 20px;
    background: rgba(255,255,255,0.25);
    flex-shrink: 0;
  }
  .af-presets {
    display: flex; align-items: center;
    gap: 5px; flex: 1; overflow-x: auto;
  }
  .af-preset-btn {
    display: inline-flex; align-items: center; gap: 3px;
    padding: 3px 9px; font-size: 0.7rem; font-weight: 600;
    border: 1.5px solid rgba(255,255,255,0.4); border-radius: 14px;
    background: transparent; color: rgba(255,255,255,0.85);
    cursor: pointer; transition: all 0.18s; white-space: nowrap;
    text-decoration: none;
  }
  .af-preset-btn:hover {
    background: rgba(255,255,255,0.12);
    border-color: rgba(255,255,255,0.6);
    color: #fff; text-decoration: none;
  }
  .af-preset-btn.active {
    background: rgba(255,255,255,0.2);
    border-color: rgba(255,255,255,0.8);
    color: #fff;
  }
  .af-preset-btn i { font-size: 0.75rem; }
  .af-preset-edit {
    background: none;
    border: 1px dashed rgba(255,255,255,0.3);
    color: rgba(255,255,255,0.5);
    border-radius: 14px; padding: 3px 7px;
    font-size: 0.65rem; cursor: pointer;
    transition: all 0.18s; white-space: nowrap;
  }
  .af-preset-edit:hover {
    border-color: rgba(255,255,255,0.7);
    color: rgba(255,255,255,0.9);
  }
  .af-hdr-right {
    display: flex; align-items: center;
    gap: 8px; margin-left: auto; flex-shrink: 0;
  }
  .af-hdr-count {
    font-size: 0.72rem; font-weight: 400;
    opacity: 0.8; white-space: nowrap;
  }
  .af-hdr-toggle {
    background: none;
    border: 1.5px solid rgba(255,255,255,0.4);
    color: rgba(255,255,255,0.85);
    border-radius: 5px; padding: 2px 7px;
    font-size: 0.85rem; cursor: pointer;
    transition: all 0.18s;
    display: flex; align-items: center; gap: 3px;
  }
  .af-hdr-toggle:hover {
    background: rgba(255,255,255,0.12);
    border-color: rgba(255,255,255,0.6);
  }
  .af-hdr-toggle.open {
    background: rgba(255,255,255,0.18);
    border-color: rgba(255,255,255,0.7);
  }
  .af-hdr-toggle .af-arrow {
    font-size: 0.65rem;
    transition: transform 0.25s ease;
    display: inline-block;
  }
  .af-hdr-toggle.open .af-arrow { transform: rotate(180deg); }

  /* ─── Filter Grid ─── */
  .af-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr auto;
    gap: 0;
  }
  .af-col {
    padding: 0 10px;
    border-right: 1px solid #dde2e7;
  }
  .af-col:first-child { padding-left: 2px; }
  .af-col:last-child  { border-right: none; padding-right: 2px; }
  .af-col-label {
    font-size: 0.65rem; font-weight: 700;
    text-transform: uppercase; letter-spacing: 0.06em;
    color: #6c757d; margin-bottom: 5px;
  }

  /* ─── Clickable chip rows ─── */
  .af-chip {
    display: flex; align-items: center; gap: 6px;
    padding: 4px 8px; border-radius: 6px;
    font-size: 0.78rem; font-weight: 500;
    cursor: pointer; transition: all 0.18s;
    user-select: none; border: 1.5px solid transparent;
    margin-bottom: 3px;
  }
  .af-chip-icon {
    width: 20px; height: 20px; border-radius: 4px;
    display: flex; align-items: center; justify-content: center;
    font-size: 0.7rem; flex-shrink: 0;
  }
  .af-chip-check {
    margin-left: auto; font-size: 0.75rem;
    opacity: 0; transition: opacity 0.18s;
  }
  .af-chip.on  { background: #f0f4f8; border-color: #c8d6e0; }
  .af-chip.on .af-chip-check { opacity: 1; }
  .af-chip.off { opacity: 0.4; }

  .af-chip[data-type="renewal"] .af-chip-icon    { background: #dbeafe; color: #1d4ed8; }
  .af-chip[data-type="setup"] .af-chip-icon      { background: #e5e7eb; color: #374151; }
  .af-chip[data-type="ticket"] .af-chip-icon     { background: #e0f2fe; color: #0284c7; }
  .af-chip[data-type="opportunity"] .af-chip-icon { background: #eef3e4; color: #4a7a1a; }

  /* ─── Radio rows (Owner + Attention) ─── */
  .af-radio {
    display: flex; align-items: center; gap: 6px;
    padding: 4px 8px; border-radius: 6px;
    font-size: 0.78rem; font-weight: 500;
    cursor: pointer; transition: all 0.18s;
    user-select: none; border: 1.5px solid transparent;
    margin-bottom: 3px;
  }
  .af-radio-dot {
    width: 14px; height: 14px; border-radius: 50%;
    border: 2px solid #bbb; flex-shrink: 0;
    transition: all 0.18s; position: relative;
  }
  .af-radio-dot::after {
    content: ''; position: absolute;
    top: 2px; left: 2px; right: 2px; bottom: 2px;
    border-radius: 50%; transform: scale(0);
    transition: transform 0.18s;
  }
  .af-radio.selected { background: #e8f0f7; border-color: #b0ccdf; }
  .af-radio.selected .af-radio-dot { border-color: #0d5681; }
  .af-radio.selected .af-radio-dot::after { background: #0d5681; transform: scale(1); }
  .af-radio:hover:not(.selected) { background: #f6f8fa; }
  .af-radio-icon { font-size: 0.85rem; width: 18px; text-align: center; }

  /* Attention-specific selected colors */
  .af-radio[data-attn="1"].selected { background: #fce8e6; border-color: #f0bbb5; }
  .af-radio[data-attn="1"].selected .af-radio-dot { border-color: #c0392b; }
  .af-radio[data-attn="1"].selected .af-radio-dot::after { background: #c0392b; }
  .af-radio[data-attn="2"].selected { background: #fef5ec; border-color: #f5cfa0; }
  .af-radio[data-attn="2"].selected .af-radio-dot { border-color: #e67e22; }
  .af-radio[data-attn="2"].selected .af-radio-dot::after { background: #e67e22; }
  .af-radio[data-attn="3"].selected { background: #fef3f2; border-color: #f0bbb5; }
  .af-radio[data-attn="3"].selected .af-radio-dot { border-color: #c0392b; }
  .af-radio[data-attn="3"].selected .af-radio-dot::after { background: #c0392b; }

  /* ─── Sort buttons ─── */
  .af-sort-col {
    display: flex; flex-direction: column;
    align-items: center; min-width: 68px;
    padding: 0 6px !important;
  }
  .af-sort-btn {
    display: flex; align-items: center; justify-content: center;
    gap: 4px; padding: 5px 6px; border-radius: 6px;
    font-size: 0.72rem; font-weight: 600;
    cursor: pointer; transition: all 0.18s;
    user-select: none; border: 1.5px solid transparent;
    background: transparent; color: #6c757d;
    margin-bottom: 3px; width: 100%;
  }
  .af-sort-btn.active {
    background: #e8f0f7; border-color: #b0ccdf; color: #0d5681;
  }
  .af-sort-btn:hover:not(.active) { background: #f6f8fa; }
  .af-sort-btn i { font-size: 0.85rem; }

  /* ─── Summary bar ─── */
  .af-summary {
    display: flex; align-items: center; gap: 6px;
    font-size: 0.7rem; color: #6c757d;
    padding-top: 7px; margin-top: 7px;
    border-top: 1px solid #dde2e7;
  }
  .af-summary .af-count {
    font-weight: 700; color: #0d5681; font-size: 0.8rem;
  }
  .af-summary .af-sep { color: #ccc; }
</style>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- Activity Filter State                                                 --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<c:set var="af" value="${sessionScope.local.getActivityFilter()}"/>
<c:set var="presets" value="${sessionScope.local.getFilterPresets()}"/>
<c:set var="rowCount" value="${fn:length(requestScope.activityRows)}"/>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- HEADER BAR                                                            --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<div class="mt-2">
  <div class="hdr-bar d-flex align-items-center gap-2 af-panel-closed" id="afHdrBar">

    <%-- Title --%>
    <span style="white-space:nowrap; font-weight:600;">
      <i class="bi bi-activity"></i> Activities
    </span>

    <span class="af-hdr-sep"></span>

    <%-- Preset buttons --%>
    <div class="af-presets">
      <c:forEach var="preset" items="${presets}">
        <a href="FilterActivities25?preset=${preset.getSlotNumber()}"
           class="af-preset-btn"
           data-slot="${preset.getSlotNumber()}">
          <c:choose>
            <c:when test="${preset.getSlotNumber()==1}"><i class="bi bi-lightning-charge"></i></c:when>
            <c:when test="${preset.getSlotNumber()==2}"><i class="bi bi-collection"></i></c:when>
            <c:otherwise><i class="bi bi-bookmark"></i></c:otherwise>
          </c:choose>
          <c:out value="${preset.getLabel()}"/>
        </a>
      </c:forEach>
      <button class="af-preset-edit" type="button" data-bs-toggle="modal" data-bs-target="#presetModal">
        <i class="bi bi-gear"></i>
      </button>
    </div>

    <%-- Right: add + count + toggle --%>
    <div class="af-hdr-right">
      <button class="btn btn-sm btn-outline-light" type="button"
              data-bs-toggle="modal" data-bs-target="#addActivityModal"
              title="New Activity" style="font-size:0.7rem; padding:0.15rem 0.45rem;">
        <i class="bi bi-plus-lg"></i>
      </button>
      <span class="af-hdr-count">${rowCount} showing</span>
      <button class="af-hdr-toggle" id="afToggle" type="button" onclick="afTogglePanel()">
        <i class="bi bi-sliders2-vertical"></i>
        <span class="af-arrow"><i class="bi bi-chevron-up"></i></span>
      </button>
    </div>
  </div>

  <%-- ══════════════════════════════════════════════════════════════════ --%>
  <%-- COLLAPSIBLE FILTER BODY                                           --%>
  <%-- ══════════════════════════════════════════════════════════════════ --%>
  <div class="af-surface collapsed" id="afSurface">
    <form action="FilterActivities25" method="post" id="afForm">
      <input type="hidden" name="attentionFilter" id="afAttn" value="${af.getAttentionFilter()}">
      <input type="hidden" name="whoFilter" id="afWho" value="${af.getOwnershipFilter()}">
      <input type="hidden" name="fAlpha" id="afAlpha" value="${af.isSortAlphabetically() ? '1' : '0'}">

      <div class="af-grid">

        <%-- ── COL 1: Type (checkboxes) ── --%>
        <div class="af-col">
          <div class="af-col-label">Type</div>
          <div class="af-chip ${af.isViewRenewal() ? 'on' : 'off'}" data-type="renewal" onclick="afToggleType(this)">
            <span class="af-chip-icon"><i class="bi bi-repeat"></i></span>
            Renewals
            <i class="bi bi-check-lg af-chip-check"></i>
            <input type="checkbox" name="vRenew" value="1" class="d-none" ${af.isViewRenewal() ? 'checked' : ''}>
          </div>
          <div class="af-chip ${af.isViewSetup() ? 'on' : 'off'}" data-type="setup" onclick="afToggleType(this)">
            <span class="af-chip-icon"><i class="bi bi-buildings"></i></span>
            Setups
            <i class="bi bi-check-lg af-chip-check"></i>
            <input type="checkbox" name="vSetup" value="3" class="d-none" ${af.isViewSetup() ? 'checked' : ''}>
          </div>
          <div class="af-chip ${af.isViewTicket() ? 'on' : 'off'}" data-type="ticket" onclick="afToggleType(this)">
            <span class="af-chip-icon"><i class="bi bi-ticket-detailed"></i></span>
            Tickets
            <i class="bi bi-check-lg af-chip-check"></i>
            <input type="checkbox" name="vTicket" value="5" class="d-none" ${af.isViewTicket() ? 'checked' : ''}>
          </div>
          <c:if test="${sessionScope.isPspSales || sessionScope.isPspAdmin}">
            <div class="af-chip ${af.isViewOpportunity() ? 'on' : 'off'}" data-type="opportunity" onclick="afToggleType(this)">
              <span class="af-chip-icon"><i class="bi bi-graph-up-arrow"></i></span>
              Opportunities
              <i class="bi bi-check-lg af-chip-check"></i>
              <input type="checkbox" name="vOpp" value="7" class="d-none" ${af.isViewOpportunity() ? 'checked' : ''}>
            </div>
          </c:if>
        </div>

        <%-- ── COL 2: Showing / Owner (radio) ── --%>
        <div class="af-col">
          <div class="af-col-label">Showing</div>
          <div class="af-radio ${af.getOwnershipFilter()==0 ? 'selected' : ''}" data-val="0" onclick="afSelectOwner(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-people af-radio-icon"></i> All Open
          </div>
          <div class="af-radio ${af.getOwnershipFilter()==1 ? 'selected' : ''}" data-val="1" onclick="afSelectOwner(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-person-check af-radio-icon"></i> My World
          </div>
          <div class="af-radio ${af.getOwnershipFilter()==2 ? 'selected' : ''}" data-val="2" onclick="afSelectOwner(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-person af-radio-icon"></i> I Own
          </div>
          <div class="af-radio ${af.getOwnershipFilter()==3 ? 'selected' : ''}" data-val="3" onclick="afSelectOwner(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-check-lg af-radio-icon"></i> Helping On
          </div>
        </div>

        <%-- ── COL 3: Attention (radio) ── --%>
        <div class="af-col">
          <div class="af-col-label">Attention</div>
          <div class="af-radio ${af.getAttentionFilter()==0 ? 'selected' : ''}" data-attn="0" onclick="afSelectAttn(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-app af-radio-icon"></i> Show All
          </div>
          <c:if test="${!applicationScope.global.isContactTrackingDisabled()}">
          <div class="af-radio ${af.getAttentionFilter()==1 ? 'selected' : ''}" data-attn="1" onclick="afSelectAttn(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-exclamation-triangle af-radio-icon" style="color:#c0392b;"></i> Needs Attention
          </div>
          </c:if>
          <div class="af-radio ${af.getAttentionFilter()==2 ? 'selected' : ''}" data-attn="2" onclick="afSelectAttn(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-hourglass-split af-radio-icon" style="color:#e67e22;"></i> Waiting on Us
          </div>
          <c:if test="${!applicationScope.global.isContactTrackingDisabled()}">
          <div class="af-radio ${af.getAttentionFilter()==3 ? 'selected' : ''}" data-attn="3" onclick="afSelectAttn(this)">
            <span class="af-radio-dot"></span>
            <i class="bi bi-telephone af-radio-icon" style="color:#c0392b;"></i> Needs Contact
          </div>
          </c:if>
        </div>

        <%-- ── COL 4: Sort ── --%>
        <div class="af-col af-sort-col">
          <div class="af-col-label">Sort</div>
          <div class="af-sort-btn ${!af.isSortAlphabetically() ? 'active' : ''}" data-sort="0" onclick="afSelectSort(this)">
            <i class="bi bi-calendar2"></i> Due
          </div>
          <div class="af-sort-btn ${af.isSortAlphabetically() ? 'active' : ''}" data-sort="1" onclick="afSelectSort(this)">
            <i class="bi bi-sort-alpha-down"></i> A&ndash;Z
          </div>
        </div>

      </div>
    </form>

    <%-- Summary bar --%>
    <div class="af-summary">
      <span class="af-count">${rowCount}</span> activities
      <span class="af-sep">&middot;</span>
      <span id="afSummaryText"></span>
    </div>
  </div>
</div>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- PRESET MANAGEMENT MODAL                                               --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<div class="modal fade" id="presetModal" tabindex="-1">
  <div class="modal-dialog modal-sm">
    <div class="modal-content">
      <div class="modal-header" style="background:#0d5681; color:#fff; padding:10px 16px;">
        <h6 class="modal-title mb-0"><i class="bi bi-bookmark-star me-1"></i> My Filter Presets</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <p style="font-size:0.75rem; color:#6c757d; margin-bottom:12px;">
          Click <strong>Save Current</strong> to overwrite a preset with whatever filters are active now.
        </p>
        <c:forEach var="preset" items="${presets}">
          <form action="SaveFilterPreset" method="post" class="d-flex align-items-center gap-2 mb-2 p-2 border rounded"
                style="background:#f8f9fb;">
            <input type="hidden" name="slotNumber" value="${preset.getSlotNumber()}">
            <span style="font-size:0.85rem; font-weight:700; width:18px; text-align:center; color:#0d5681;">
              ${preset.getSlotNumber()}
            </span>
            <input type="text" name="label" value="${fn:escapeXml(preset.getLabel())}"
                   maxlength="16" class="form-control form-control-sm" style="font-weight:600; font-size:0.8rem;">
            <button type="submit" class="btn btn-sm btn-ssa" style="white-space:nowrap; font-size:0.72rem;">
              <i class="bi bi-check-lg"></i> Save Current
            </button>
          </form>
        </c:forEach>
      </div>
    </div>
  </div>
</div>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- JAVASCRIPT                                                            --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<script>
  // ─── Collapse / Expand with localStorage ───
  const afSurface = document.getElementById('afSurface');
  const afHdrBar  = document.getElementById('afHdrBar');
  const afTogBtn  = document.getElementById('afToggle');

  function afSetPanel(open) {
    if (open) {
      afSurface.classList.remove('collapsed');
      afSurface.classList.add('expanded');
      afHdrBar.classList.remove('af-panel-closed');
      afHdrBar.classList.add('af-panel-open');
      afTogBtn.classList.add('open');
    } else {
      afSurface.classList.remove('expanded');
      afSurface.classList.add('collapsed');
      afHdrBar.classList.remove('af-panel-open');
      afHdrBar.classList.add('af-panel-closed');
      afTogBtn.classList.remove('open');
    }
    localStorage.setItem('ssa-af-open', open ? '1' : '0');
  }

  function afTogglePanel() {
    afSetPanel(afSurface.classList.contains('collapsed'));
  }

  // Restore saved state — default to CLOSED
  var savedAf = localStorage.getItem('ssa-af-open');
  if (savedAf === '1') afSetPanel(true);

  // ─── Type chip toggle → auto-submit ───
  function afToggleType(el) {
    const cb = el.querySelector('input[type=checkbox]');
    cb.checked = !cb.checked;
    el.classList.toggle('on', cb.checked);
    el.classList.toggle('off', !cb.checked);
    afSubmit();
  }

  // ─── Owner radio → auto-submit ───
  function afSelectOwner(el) {
    el.closest('.af-col').querySelectorAll('.af-radio').forEach(r => r.classList.remove('selected'));
    el.classList.add('selected');
    document.getElementById('afWho').value = el.dataset.val;
    afSubmit();
  }

  // ─── Attention radio → auto-submit ───
  function afSelectAttn(el) {
    el.closest('.af-col').querySelectorAll('.af-radio').forEach(r => r.classList.remove('selected'));
    el.classList.add('selected');
    document.getElementById('afAttn').value = el.dataset.attn;
    afSubmit();
  }

  // ─── Sort toggle → auto-submit ───
  function afSelectSort(el) {
    el.closest('.af-sort-col').querySelectorAll('.af-sort-btn').forEach(s => s.classList.remove('active'));
    el.classList.add('active');
    document.getElementById('afAlpha').value = el.dataset.sort;
    afSubmit();
  }

  // ─── Form submit ───
  function afSubmit() {
    document.getElementById('afForm').submit();
  }

  // ─── Summary text ───
  (function buildSummary() {
    const types = [];
    document.querySelectorAll('.af-chip.on').forEach(c => types.push(c.dataset.type));
    const typeText = types.length >= 4 ? 'All types' : types.length === 0 ? 'No types' :
      types.map(t => t.charAt(0).toUpperCase() + t.slice(1)).join(', ');

    const ownerEl = document.querySelector('.af-col:nth-child(2) .af-radio.selected');
    const ownerText = ownerEl ? ownerEl.textContent.trim() : '';

    const attnEl = document.querySelector('.af-col:nth-child(3) .af-radio.selected');
    const attnText = attnEl ? attnEl.textContent.trim() : '';

    const sortEl = document.querySelector('.af-sort-btn.active');
    const sortText = sortEl && sortEl.dataset.sort === '1' ? 'A-Z' : 'Due date';

    const el = document.getElementById('afSummaryText');
    if (el) el.textContent = ownerText + ' \u00b7 ' + typeText + ' \u00b7 ' + attnText + ' \u00b7 ' + sortText;
  })();
</script>
