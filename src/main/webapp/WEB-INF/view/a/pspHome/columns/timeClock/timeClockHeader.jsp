<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
  Timeclock Header — Status banner + punch buttons
  Replaces the old dark "My Time" bar + flat In/Out buttons
--%>

<%-- Determine clocked-in state and CSS classes --%>
<c:set var="isIn" value="${sessionScope.local.isUserIsIn()}" />
<c:set var="bannerClass" value="${isIn ? 'tc-banner-in' : 'tc-banner-out'}" />

<c:set var="oClass" value="tc-punch-btn tc-btn-out tc-btn-disabled" />
<c:set var="iClass" value="tc-punch-btn tc-btn-in" />
<c:if test="${isIn}">
  <c:set var="oClass" value="tc-punch-btn tc-btn-out" />
  <c:set var="iClass" value="tc-punch-btn tc-btn-in tc-btn-disabled" />
</c:if>

<style>
  /* ─── TIMECLOCK PANEL ─── */
  .tc-panel {
    background: #f8f9fb;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    margin-top: 0.5rem;
    margin-bottom: 0.75rem;
  }

  /* ─── STATUS BANNER ─── */
  .tc-banner { padding: 1.1rem 1.15rem 0.9rem; color: #fff; position: relative; }
  .tc-banner-in  { background: linear-gradient(135deg, #157347, #198754); }
  .tc-banner-out { background: linear-gradient(135deg, #495057, #6c757d); }
  .tc-status-label {
    font-size: 0.68rem; font-weight: 600; letter-spacing: 0.07em;
    text-transform: uppercase; opacity: 0.85;
  }
  .tc-status-time { font-size: 1.5rem; font-weight: 700; line-height: 1.2; }
  .tc-status-since { font-size: 0.8rem; opacity: 0.8; margin-top: 0.1rem; }
  .tc-elapsed-badge {
    position: absolute; top: 0.9rem; right: 1rem;
    background: rgba(255,255,255,0.2); border-radius: 20px;
    padding: 0.25rem 0.65rem; font-size: 0.75rem; font-weight: 600;
  }

  /* ─── PUNCH BUTTONS ─── */
  .tc-punch-row {
    display: flex; gap: 0.5rem;
    padding: 0.65rem 1.15rem;
    background: #fff;
    border-bottom: 1px solid #e2e6ea;
  }
  .tc-punch-btn {
    flex: 1; border: 2px solid; border-radius: 8px;
    padding: 0.5rem 0; font-weight: 600; font-size: 0.88rem;
    cursor: pointer; transition: all 0.15s ease;
    display: flex; align-items: center; justify-content: center; gap: 0.35rem;
    text-decoration: none;
  }
  .tc-btn-out { border-color: #dc3545; color: #dc3545; background: transparent; }
  .tc-btn-out:hover { background: #dc3545; color: #fff; }
  .tc-btn-in  { border-color: #198754; color: #fff; background: #198754; }
  .tc-btn-in:hover { background: #157347; }
  .tc-btn-disabled {
    border-color: #ddd !important; color: #bbb !important;
    background: #f5f5f5 !important; pointer-events: none;
  }
</style>

<div class="tc-panel">

  <%-- ── STATUS BANNER ── --%>
  <div class="tc-banner ${bannerClass}">
    <div class="tc-status-label">
      <c:choose>
        <c:when test="${isIn}">Currently Clocked In</c:when>
        <c:otherwise>Currently Clocked Out</c:otherwise>
      </c:choose>
    </div>
    <div class="tc-status-time" id="tcRunningTime">
      <c:choose>
        <c:when test="${isIn}">
          <c:if test="${not empty todaySummary}">
            ${todaySummary.getTotalFormatted()}
          </c:if>
          <c:if test="${empty todaySummary}">0h 00m</c:if>
        </c:when>
        <c:otherwise>&mdash;</c:otherwise>
      </c:choose>
    </div>
    <div class="tc-status-since">
      <c:choose>
        <c:when test="${isIn && not empty sessionScope.local.getMyTimeHistory() && sessionScope.local.getMyTimeHistory().size() > 0}">
          since <fmt:formatDate value="${sessionScope.local.getMyTimeHistory().get(0).getInTime()}" pattern="h:mm a" />
        </c:when>
        <c:when test="${isIn}">since earlier</c:when>
        <c:when test="${not isIn && not empty sessionScope.local.getMyTimeHistory() && sessionScope.local.getMyTimeHistory().size() > 0}">
          since <fmt:formatDate value="${sessionScope.local.getMyTimeHistory().get(0).getOutTime()}" pattern="h:mm a" />
        </c:when>
        <c:otherwise>since yesterday</c:otherwise>
      </c:choose>
    </div>
    <div class="tc-elapsed-badge">
      <c:choose>
        <c:when test="${isIn}"><i class="bi bi-clock"></i>&nbsp; Active</c:when>
        <c:otherwise><i class="bi bi-moon-stars"></i>&nbsp; Off Clock</c:otherwise>
      </c:choose>
    </div>
  </div>

  <%-- ── PUNCH BUTTONS ── --%>
  <form method="post" action="TimeClock25">
    <input type="hidden" name="formSender" value="timeClock">
    <div class="tc-punch-row">
      <button type="submit" class="${oClass}" name="btnPunch" value="0">
        <i class="bi bi-moon-stars"></i> Clock Out
      </button>
      <button type="submit" class="${iClass}" name="btnPunch" value="1">
        <i class="bi bi-sun"></i> Clock In
      </button>
    </div>
  </form>
