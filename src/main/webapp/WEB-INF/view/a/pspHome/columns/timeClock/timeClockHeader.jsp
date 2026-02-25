<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
  Timeclock Header — Slim .hdr-bar + compact punch row + since line
  Matches the height/style of the Activities and Checklists column headers

  NOTE: The <div class="tc-panel"> is intentionally LEFT OPEN here.
  It is closed by timeClockDetail25.jsp which is imported after this file.
--%>

<c:set var="isIn" value="${sessionScope.local.isUserIsIn()}" />

<style>
  .tc-panel {
    background: #f8f9fb;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    margin-top: 0.5rem;
    margin-bottom: 0.75rem;
  }
  .tc-status-pill {
    font-size: 0.65rem;
    font-weight: 600;
    padding: 0.15rem 0.55rem;
    border-radius: 10px;
    letter-spacing: 0.03em;
    text-transform: uppercase;
  }
  .tc-pill-in  { background: rgba(25,135,84,0.9); color: #fff; }
  .tc-pill-out { background: rgba(255,255,255,0.15); color: rgba(255,255,255,0.7); }
  .tc-punch-area {
    background: #fff;
    padding: 0.65rem 0.85rem 0.55rem;
    border-bottom: 1px solid #e2e6ea;
  }
  .tc-punch-row {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 0.4rem;
  }
  .tc-punch-btn {
    flex: 1;
    border: none;
    border-radius: 8px;
    padding: 0.45rem 0;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    transition: all 0.15s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.3rem;
  }
  .tc-btn-active-out { background: #dc3545; color: #fff; }
  .tc-btn-active-out:hover { background: #bb2d3b; }
  .tc-btn-active-in  { background: #198754; color: #fff; }
  .tc-btn-active-in:hover  { background: #157347; }
  .tc-btn-disabled {
    background: #f0f0f0;
    color: #ccc;
    pointer-events: none;
  }
  .tc-since-line {
    text-align: center;
    font-size: 0.78rem;
    color: #6c757d;
    padding: 0.15rem 0 0.1rem;
  }
  .tc-since-emphasis {
    font-weight: 600;
    color: #495057;
  }
</style>

<div class="tc-panel">

  <%-- ── HEADER BAR ── --%>
  <div class="hdr-bar d-flex align-items-center justify-content-between">
    <span><i class="bi bi-clock me-1"></i>Time Clock</span>
    <c:choose>
      <c:when test="${isIn}">
        <span class="tc-status-pill tc-pill-in">&#9679; Clocked In</span>
      </c:when>
      <c:otherwise>
        <span class="tc-status-pill tc-pill-out">Off Clock</span>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- ── PUNCH AREA ── --%>
  <form method="post" action="TimeClock25">
    <input type="hidden" name="formSender" value="timeClock">
    <div class="tc-punch-area">
      <div class="tc-punch-row">
        <button type="submit" name="btnPunch" value="0"
                class="tc-punch-btn ${isIn ? 'tc-btn-active-out' : 'tc-btn-disabled'}"
                ${isIn ? '' : 'disabled'}>
          <i class="bi bi-moon-stars"></i> Clock Out
        </button>
        <button type="submit" name="btnPunch" value="1"
                class="tc-punch-btn ${isIn ? 'tc-btn-disabled' : 'tc-btn-active-in'}"
                ${isIn ? 'disabled' : ''}>
          <i class="bi bi-sun"></i> Clock In
        </button>
      </div>
      <div class="tc-since-line">
        <c:choose>
          <c:when test="${isIn && not empty sessionScope.local.getMyTimeHistory() && sessionScope.local.getMyTimeHistory().size() > 0}">
            Clocked in since <span class="tc-since-emphasis"><fmt:formatDate value="${sessionScope.local.getMyTimeHistory().get(0).getInTime()}" pattern="h:mm a" /></span>
          </c:when>
          <c:when test="${isIn}">
            Clocked in since <span class="tc-since-emphasis">earlier today</span>
          </c:when>
          <c:when test="${not isIn && not empty sessionScope.local.getMyTimeHistory() && sessionScope.local.getMyTimeHistory().size() > 0}">
            Off clock since <span class="tc-since-emphasis"><fmt:formatDate value="${sessionScope.local.getMyTimeHistory().get(0).getOutTime()}" pattern="h:mm a" /></span>
          </c:when>
          <c:otherwise>
            Off clock since <span class="tc-since-emphasis">yesterday</span>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </form>
