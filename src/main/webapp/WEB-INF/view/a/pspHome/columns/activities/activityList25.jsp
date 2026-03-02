<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<style>
  .act-card {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    padding: 0.45rem 0.6rem;
    margin-bottom: 0.35rem;
    cursor: pointer;
    transition: all 0.15s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .act-card:hover { background: #f0f6fc; border-color: #b0c4d8; }
  .act-card.border-danger-left { border-left: 3px solid #dc3545; }
  .act-card.border-warning-left { border-left: 3px solid #ffc107; }
  .act-card.border-normal { border-left: 3px solid transparent; }

  .act-type-badge {
    font-size: 0.65rem;
    font-weight: 700;
    padding: 0.15rem 0.4rem;
    border-radius: 4px;
    min-width: 2rem;
    text-align: center;
    flex-shrink: 0;
  }
  .act-type-R { background: #e3edfd; color: #1565c0; }
  .act-type-S { background: #ede7f6; color: #5e35b1; }
  .act-type-T { background: #e0f7fa; color: #00838f; }
  .act-type-O { background: #e8f5e9; color: #2e7d32; }

  .act-stage {
    flex-shrink: 0;
    font-size: 0.6rem;
    font-weight: 600;
    padding: 0.1rem 0.35rem;
    border-radius: 3px;
    text-transform: uppercase;
    white-space: nowrap;
  }
  .act-stage-NEW          { background: #e3f2fd; color: #1565c0; }
  .act-stage-CONTACTED    { background: #e8f5e9; color: #2e7d32; }
  .act-stage-QUALIFIED    { background: #fff3e0; color: #e65100; }
  .act-stage-PROPOSAL_SENT { background: #f3e5f5; color: #6a1b9a; }
  .act-stage-NEGOTIATION  { background: #fce4ec; color: #b71c1c; }
  .act-stage-ON_HOLD      { background: #f5f5f5; color: #616161; }

  .act-name { flex: 1; min-width: 0; font-size: 0.82rem; line-height: 1.25; }
  .act-name-text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

  .act-owner-icon { flex-shrink: 0; font-size: 0.85rem; }

  .act-extra { font-size: 0.7rem; color: #888; font-style: italic; }

  .act-due {
    flex-shrink: 0;
    font-size: 0.7rem;
    font-weight: 600;
    text-align: right;
    min-width: 4.5rem;
  }
  .act-due-ok   { color: #6c757d; }
  .act-due-soon { color: #495057; }
  .act-due-warn { color: #e65100; }
  .act-due-over { color: #c62828; }

  .act-urgency {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 0.15rem;
    width: 2rem;
    font-size: 0.78rem;
  }
  .act-urg-danger  { color: #dc3545; }
  .act-urg-warning { color: #e65100; }
  .act-urg-onus    { color: #dc3545; }
</style>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- DATA SERIALIZATION                                                    --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<script>
  const ALL_ACTIVITIES = [
    <c:forEach var="row" items="${requestScope.activityRows}" varStatus="s">
    {
      id: ${row.activityId},
      dtype: '${row.dtype}',
      fullName: '${fn:replace(fn:replace(row.fullName, "\\", "\\\\"), "'", "\\'")}',
      assignedToId: <c:choose><c:when test="${row.assignedToId != null}">${row.assignedToId}</c:when><c:otherwise>null</c:otherwise></c:choose>,
      dueDate: <c:choose><c:when test="${row.dueDate != null}">'<fmt:formatDate value="${row.dueDate}" pattern="yyyy-MM-dd"/>'</c:when><c:otherwise>null</c:otherwise></c:choose>,
      waitingOnUs: ${row.waitingOnUs},
      daysSince: ${row.daysSinceContact},
      delegatedToMe: ${row.delegatedToMe},
      dueBucket: ${row.dueBucket},
      ticketEmployer: <c:choose><c:when test="${not empty row.ticketEmployerNameLc}">'${fn:replace(row.ticketEmployerNameLc, "'", "\\'")}'</c:when><c:otherwise>null</c:otherwise></c:choose>,
      oppStage: <c:choose><c:when test="${not empty row.opportunityStage}">'${row.opportunityStage}'</c:when><c:otherwise>null</c:otherwise></c:choose>
    }<c:if test="${!s.last}">,</c:if>
    </c:forEach>
  ];
  const ME_PERSON_ID = ${requestScope.mePersonId};
  const DAYS_WARN = ${requestScope.daysSinceWarning};
  const CAN_SEE_OPPS = ${requestScope.canSeeOpportunities};
</script>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- SCROLL CONTAINER (JS will render into this)                           --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<div class="overflow-auto flex-grow-1" style="min-height: 0;" id="actListContainer">
</div>

<%-- ══════════════════════════════════════════════════════════════════════ --%>
<%-- CLIENT-SIDE FILTER + RENDER                                           --%>
<%-- ══════════════════════════════════════════════════════════════════════ --%>
<script>
  const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

  function formatDueDate(isoStr) {
    if (!isoStr) return '\u2014';
    const parts = isoStr.split('-');
    const m = parseInt(parts[1], 10) - 1;
    const d = parts[2];
    return MONTHS[m] + ' ' + d;
  }

  function escHtml(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  function readFilterState() {
    // Type chips
    const vRenewal = !!document.querySelector('.af-chip[data-type="renewal"].on');
    const vSetup   = !!document.querySelector('.af-chip[data-type="setup"].on');
    const vTicket  = !!document.querySelector('.af-chip[data-type="ticket"].on');
    const oppChip  = document.querySelector('.af-chip[data-type="opportunity"]');
    const vOpp     = oppChip ? oppChip.classList.contains('on') : false;

    // Ownership
    const ownerEl = document.querySelector('.af-col:nth-child(2) .af-radio.selected');
    const ownership = ownerEl ? parseInt(ownerEl.dataset.val) : 0;

    // Attention
    const attnEl = document.querySelector('.af-col:nth-child(3) .af-radio.selected');
    const attention = attnEl ? parseInt(attnEl.dataset.attn) : 0;

    // Sort
    const sortEl = document.querySelector('.af-sort-btn.active');
    const sortAlpha = sortEl ? sortEl.dataset.sort === '1' : false;

    return { vRenewal, vSetup, vTicket, vOpp, ownership, attention, sortAlpha };
  }

  function filterAndRender() {
    const f = readFilterState();
    const daysW = CONTACT_TRACKING_DISABLED ? 99999 : DAYS_WARN;

    // Filter
    let filtered = ALL_ACTIVITIES.filter(row => {
      // Type filter
      const typeOk =
        (row.dtype === 'Renewal' && f.vRenewal) ||
        (row.dtype === 'Setup' && f.vSetup) ||
        (row.dtype === 'Ticket' && f.vTicket) ||
        (row.dtype === 'Opportunity' && f.vOpp && CAN_SEE_OPPS);
      if (!typeOk) return false;

      // Ownership filter
      if (f.ownership === 1) {
        if (!(row.assignedToId === ME_PERSON_ID || row.delegatedToMe)) return false;
      } else if (f.ownership === 2) {
        if (row.assignedToId !== ME_PERSON_ID) return false;
      } else if (f.ownership === 3) {
        if (!(row.delegatedToMe && row.assignedToId !== ME_PERSON_ID)) return false;
      }

      // Attention filter
      const needsContact = daysW < 99 && row.daysSince > daysW;
      if (f.attention === 1) {
        if (!(row.waitingOnUs || needsContact)) return false;
      } else if (f.attention === 2) {
        if (!row.waitingOnUs) return false;
      } else if (f.attention === 3) {
        if (!needsContact) return false;
      }

      return true;
    });

    // Sort
    if (f.sortAlpha) {
      filtered.sort((a, b) => {
        const cmp = a.fullName.localeCompare(b.fullName, undefined, {sensitivity: 'base'});
        return cmp !== 0 ? cmp : b.id - a.id;
      });
    } else {
      filtered.sort((a, b) => {
        // Nulls last
        if (!a.dueDate && !b.dueDate) {
          const cmp = a.fullName.localeCompare(b.fullName, undefined, {sensitivity: 'base'});
          return cmp !== 0 ? cmp : b.id - a.id;
        }
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        const cmp = a.dueDate.localeCompare(b.dueDate);
        if (cmp !== 0) return cmp;
        const nameCmp = a.fullName.localeCompare(b.fullName, undefined, {sensitivity: 'base'});
        return nameCmp !== 0 ? nameCmp : b.id - a.id;
      });
    }

    // Render
    const container = document.getElementById('actListContainer');
    if (filtered.length === 0) {
      container.innerHTML = '<div class="text-center text-muted fst-italic py-4" style="font-size:0.88rem;">No matching activities</div>';
    } else {
      let html = '';
      for (const row of filtered) {
        html += renderRow(row, daysW);
      }
      container.innerHTML = html;
    }

    // Update counts
    updateRowCount(filtered.length);
    if (typeof buildSummary === 'function') buildSummary();
  }

  function renderRow(row, daysW) {
    // Owner icon
    let ownerIcon, ownerColor;
    if (row.assignedToId !== null && row.assignedToId === ME_PERSON_ID) {
      ownerIcon = 'person-fill'; ownerColor = 'color:#0d5681;';
    } else if (row.delegatedToMe) {
      ownerIcon = 'check-lg'; ownerColor = 'color:#6c757d;';
    } else {
      ownerIcon = 'collection'; ownerColor = 'color:#adb5bd;';
    }

    // Type badge
    let typeIcon, typeCls;
    switch (row.dtype) {
      case 'Renewal':     typeIcon = 'repeat';         typeCls = 'act-type-R'; break;
      case 'Setup':       typeIcon = 'buildings';      typeCls = 'act-type-S'; break;
      case 'Opportunity': typeIcon = 'graph-up-arrow'; typeCls = 'act-type-O'; break;
      default:            typeIcon = 'ticket-detailed'; typeCls = 'act-type-T'; break;
    }

    // Needs contact
    const needsContact = daysW < 99 && row.daysSince > daysW;

    // Attention icon + name weight
    let attentionHtml = '', nameWeight;
    if (row.waitingOnUs && needsContact) {
      attentionHtml = '<i class="bi bi-exclamation-triangle act-urg-danger"></i>';
      nameWeight = 'fw-bold';
    } else if (row.waitingOnUs) {
      attentionHtml = '<i class="bi bi-hourglass-split act-urg-onus"></i>';
      nameWeight = '';
    } else if (needsContact) {
      attentionHtml = '<i class="bi bi-telephone act-urg-warning"></i>';
      nameWeight = '';
    } else {
      nameWeight = 'text-muted';
    }

    // Due bucket
    let borderCls, dueCls;
    if (row.dueBucket >= 3) {
      borderCls = 'border-danger-left'; dueCls = 'act-due-over';
    } else if (row.dueBucket === 2) {
      borderCls = 'border-warning-left'; dueCls = 'act-due-warn';
    } else if (row.dueBucket === 1) {
      borderCls = 'border-normal'; dueCls = 'act-due-soon';
    } else {
      borderCls = 'border-normal'; dueCls = 'act-due-ok';
    }

    // Ticket employer extra
    const extra = (row.dtype === 'Ticket' && row.ticketEmployer)
      ? ' <span class="act-extra">(' + escHtml(row.ticketEmployer) + ')</span>'
      : '';

    // Opportunity stage badge
    let stageHtml = '';
    if (row.dtype === 'Opportunity' && row.oppStage) {
      stageHtml = '<span class="act-stage act-stage-' + escHtml(row.oppStage) + '">'
        + escHtml(row.oppStage.replace(/_/g, ' '))
        + '</span>';
    }

    // Due date formatted
    const dueDateText = formatDueDate(row.dueDate);

    // Full name lowercase + capitalize via CSS
    const nameText = escHtml(row.fullName.toLowerCase());

    return '<div class="act-card ' + borderCls + '" onclick="goActivity(' + row.id + ')">'
      + '<span class="act-owner-icon" style="' + ownerColor + '"><i class="bi bi-' + ownerIcon + '"></i></span>'
      + '<span class="act-type-badge ' + typeCls + '"><i class="bi bi-' + typeIcon + '"></i></span>'
      + '<span class="act-urgency">' + attentionHtml + '</span>'
      + '<div class="act-name"><span class="act-name-text ' + nameWeight + '" style="text-transform: capitalize;">'
      + nameText + extra
      + '</span></div>'
      + stageHtml
      + '<span class="act-due ' + dueCls + '">' + dueDateText + '</span>'
      + '</div>';
  }

  function goActivity(activityId) {
    // Use a form POST to match existing GoActivityDetail25 behavior
    const form = document.createElement('form');
    form.method = 'post';
    form.action = 'GoActivityDetail25';
    form.style.display = 'none';

    const sender = document.createElement('input');
    sender.type = 'hidden';
    sender.name = 'formSender';
    sender.value = 'viewActivity';
    form.appendChild(sender);

    const btn = document.createElement('input');
    btn.type = 'hidden';
    btn.name = 'btnViewActivity';
    btn.value = activityId;
    form.appendChild(btn);

    document.body.appendChild(form);
    form.submit();
  }

  // Initial render on page load using current filter state from UI controls
  filterAndRender();
</script>
