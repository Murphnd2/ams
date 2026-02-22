<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
  Timeclock Detail — Today tab (stretches) + Week tab (bar chart)
  Replaces the old flat date/in/out row listing
--%>

<style>
  /* ─── TABS ─── */
  .tc-tabs {
    display: flex; background: #fff;
    border-bottom: 1px solid #e2e6ea;
  }
  .tc-tab {
    flex: 1; text-align: center; padding: 0.55rem 0;
    font-size: 0.76rem; font-weight: 600; color: #6c757d;
    cursor: pointer; border-bottom: 2px solid transparent;
    transition: all 0.15s ease; text-transform: uppercase; letter-spacing: 0.04em;
  }
  .tc-tab:hover { color: #0d5681; }
  .tc-tab.tc-active { color: #0d5681; border-bottom-color: #0d5681; }

  .tc-tab-panel { display: none; }
  .tc-tab-panel.tc-visible { display: block; }

  /* ─── TODAY TAB ─── */
  .tc-today { padding: 0.85rem 1.15rem 0.65rem; background: #fff; }
  .tc-today-header {
    display: flex; align-items: baseline; justify-content: space-between;
    margin-bottom: 0.5rem;
  }
  .tc-day-label { font-weight: 600; font-size: 0.92rem; color: #212529; }
  .tc-hours-total { font-weight: 700; font-size: 1rem; }
  .tc-hours-good { color: #198754; }
  .tc-hours-warn { color: #fd7e14; }

  .tc-progress-track {
    height: 5px; background: #e9ecef; border-radius: 3px;
    overflow: hidden; margin-bottom: 0.65rem;
  }
  .tc-progress-fill {
    height: 100%; border-radius: 3px; transition: width 0.4s ease;
  }

  .tc-stretch {
    display: flex; align-items: center; padding: 0.4rem 0;
    border-bottom: 1px solid #f0f1f3; font-size: 0.82rem;
  }
  .tc-stretch:last-child { border-bottom: none; }
  .tc-dot {
    width: 7px; height: 7px; border-radius: 50%;
    background: #0d5681; margin-right: 0.55rem; flex-shrink: 0;
  }
  .tc-dot-active {
    background: #198754;
    box-shadow: 0 0 0 3px rgba(25,135,84,0.2);
    animation: tcPulse 2s infinite;
  }
  @keyframes tcPulse {
    0%, 100% { box-shadow: 0 0 0 3px rgba(25,135,84,0.2); }
    50% { box-shadow: 0 0 0 6px rgba(25,135,84,0.08); }
  }
  .tc-stretch-times { flex: 1; color: #212529; }
  .tc-stretch-dur { font-weight: 600; color: #6c757d; font-size: 0.76rem; }
  .tc-no-data { color: #adb5bd; font-size: 0.84rem; padding: 1rem 0; text-align: center; }

  /* ─── WEEK TAB ─── */
  .tc-week { padding: 0.75rem 1.15rem 0.85rem; }
  .tc-week-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 0.65rem;
  }
  .tc-week-label { font-weight: 600; font-size: 0.83rem; color: #212529; }
  .tc-week-total { font-weight: 700; font-size: 0.92rem; color: #0d5681; }

  .tc-bar-row {
    display: flex; align-items: center; margin-bottom: 0.35rem;
    font-size: 0.76rem; height: 24px;
  }
  .tc-bar-label { width: 32px; font-weight: 600; color: #6c757d; flex-shrink: 0; }
  .tc-bar-track {
    flex: 1; height: 16px; background: #e9ecef; border-radius: 4px;
    overflow: hidden; margin: 0 0.5rem; position: relative;
  }
  /* 8-hour marker at 72.7% of the 11h max scale */
  .tc-bar-track::after {
    content: ''; position: absolute;
    left: 72.7%; top: 0; bottom: 0; width: 1px;
    background: rgba(0,0,0,0.12);
  }
  .tc-bar-fill {
    height: 100%; border-radius: 4px; transition: width 0.5s ease;
  }
  .tc-fill-regular { background: #0d5681; }
  .tc-fill-overtime { background: #fd7e14; }
  .tc-fill-today { background: #198754; }
  .tc-bar-hours { width: 48px; text-align: right; font-weight: 600; color: #212529; flex-shrink: 0; }
  .tc-bar-empty { opacity: 0.35; }

  .tc-week-footer {
    display: flex; justify-content: space-between;
    margin-top: 0.65rem; padding-top: 0.55rem;
    border-top: 1px solid #e2e6ea; font-size: 0.76rem;
  }
  .tc-stat-label { color: #6c757d; }
  .tc-stat-value { font-weight: 700; color: #212529; }

  /* ─── CLOSE PANEL ─── */
  .tc-panel-bottom { border-radius: 0 0 10px 10px; overflow: hidden; }
</style>

  <%-- ── SECTION TABS ── --%>
  <div class="tc-tabs">
    <div class="tc-tab tc-active" data-tctab="today" onclick="tcSwitchTab('today')">
      <i class="bi bi-calendar-day"></i>&nbsp; Today
    </div>
    <div class="tc-tab" data-tctab="week" onclick="tcSwitchTab('week')">
      <i class="bi bi-bar-chart-line"></i>&nbsp; This Week
    </div>
  </div>

<%-- ════════════════════════════════════════ --%>
<%--              TODAY TAB                   --%>
<%-- ════════════════════════════════════════ --%>
<div class="tc-tab-panel tc-visible tc-panel-bottom" id="tcPanelToday">
  <div class="tc-today">

    <%-- ── Day selector (this week's days) ── --%>
    <c:if test="${not empty weekSummary}">
      <div class="d-flex gap-1 mb-2" id="tcDaySelector">
        <c:forEach var="day" items="${weekSummary}">
          <c:if test="${day.getTotalMinutes() > 0 || day.isToday()}">
            <button type="button"
                    class="btn btn-sm ${day.isToday() ? 'btn-primary' : 'btn-outline-secondary'} tc-day-sel-btn"
                    data-dayindex="${day.getDayLabel()}"
                    onclick="tcSelectDay('${day.getDayLabel()}', this)"
                    style="font-size:0.72rem; padding:0.2rem 0.5rem; font-weight:600;">
                ${day.getDayLabel()}
            </button>
          </c:if>
        </c:forEach>
      </div>
    </c:if>

    <%-- ── Day panels (one per day with data) ── --%>
    <c:if test="${not empty weekSummary}">
      <c:forEach var="day" items="${weekSummary}">
        <c:if test="${day.getTotalMinutes() > 0 || day.isToday()}">
          <div class="tc-day-panel ${day.isToday() ? 'tc-visible' : ''}"
               id="tcDay_${day.getDayLabel()}" style="${day.isToday() ? '' : 'display:none;'}">

            <div class="tc-today-header">
                <span class="tc-day-label">
                  <fmt:formatDate value="${day.getDate()}" pattern="EEEE, MMM d" />
                </span>
              <span class="tc-hours-total ${day.isOvertime() ? 'tc-hours-warn' : 'tc-hours-good'}">
                  ${day.getTotalFormatted()}
              </span>
            </div>

              <%-- Progress bar toward 8h --%>
            <c:set var="pct" value="${day.getPercentOfDay()}" />
            <div class="tc-progress-track">
              <div class="tc-progress-fill" style="width:${pct > 100 ? 100 : pct}%; background:${day.isOvertime() ? '#fd7e14' : (day.isToday() ? '#198754' : '#0d5681')};"></div>
            </div>

              <%-- Stretch list --%>
            <c:choose>
              <c:when test="${not empty day.getStretches() && day.getStretches().size() > 0}">
                <c:forEach var="ts" items="${day.getStretches()}" varStatus="loop">
                  <c:choose>
                    <%-- Completed stretch — clickable for correction --%>
                    <c:when test="${ts.isComplete()}">
                      <div class="tc-stretch tc-stretch-clickable"
                           onclick="tcOpenCorrection(
                                   '${ts.getInLogId()}',
                                   '${ts.getOutLogId()}',
                                   '<fmt:formatDate value="${ts.getInDate()}" pattern="yyyy-MM-dd" />',
                                   '<fmt:formatDate value="${ts.getInDate()}" pattern="EEEE, MMM d" />',
                                   '<fmt:formatDate value="${ts.getInTime()}" pattern="HH:mm" />',
                                   '<fmt:formatDate value="${ts.getOutTime()}" pattern="HH:mm" />',
                                   '<fmt:formatDate value="${ts.getInTime()}" pattern="h:mm a" />',
                                   '<fmt:formatDate value="${ts.getOutTime()}" pattern="h:mm a" />'
                                   )"
                           title="Click to request a correction">
                        <span class="tc-dot"></span>
                        <span class="tc-stretch-times">
                            <fmt:formatDate value="${ts.getInTime()}" pattern="h:mm a" />
                            &mdash;
                            <fmt:formatDate value="${ts.getOutTime()}" pattern="h:mm a" />
                          </span>
                        <span class="tc-stretch-dur">${ts.getMinutesFormatted()}</span>
                        <i class="bi bi-pencil-square" style="color:#adb5bd; font-size:0.72rem; margin-left:0.3rem;"></i>
                      </div>
                    </c:when>
                    <%-- Active stretch (still clocked in) — not clickable --%>
                    <c:otherwise>
                      <div class="tc-stretch">
                        <span class="tc-dot ${(loop.first && sessionScope.local.isUserIsIn() && ts.getOutTime() == null) ? 'tc-dot-active' : ''}"></span>
                        <span class="tc-stretch-times">
                            <fmt:formatDate value="${ts.getInTime()}" pattern="h:mm a" />
                            &mdash;
                            <c:choose>
                              <c:when test="${ts.getOutTime() != null}">
                                <fmt:formatDate value="${ts.getOutTime()}" pattern="h:mm a" />
                              </c:when>
                              <c:otherwise>
                                <em style="color:#198754;">now</em>
                              </c:otherwise>
                            </c:choose>
                          </span>
                        <span class="tc-stretch-dur">${ts.getMinutesFormatted()}</span>
                      </div>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <div class="tc-no-data">No time logged</div>
              </c:otherwise>
            </c:choose>

          </div>
        </c:if>
      </c:forEach>
    </c:if>

    <c:if test="${empty weekSummary}">
      <div class="tc-no-data">No time data available</div>
    </c:if>

  </div>
</div>

<%-- ── Day selector script ── --%>
<script>
  function tcSelectDay(dayLabel, btn) {
    // Hide all day panels
    document.querySelectorAll('.tc-day-panel').forEach(function(p) { p.style.display = 'none'; });
    // Show selected
    var panel = document.getElementById('tcDay_' + dayLabel);
    if (panel) panel.style.display = 'block';
    // Update button styles
    document.querySelectorAll('.tc-day-sel-btn').forEach(function(b) {
      b.classList.remove('btn-primary');
      b.classList.add('btn-outline-secondary');
    });
    btn.classList.remove('btn-outline-secondary');
    btn.classList.add('btn-primary');
  }
</script>



<%-- ════════════════════════════════════════ --%>
  <%--              WEEK TAB                   --%>
  <%-- ════════════════════════════════════════ --%>
  <div class="tc-tab-panel tc-panel-bottom" id="tcPanelWeek">
    <div class="tc-week">
      <div class="tc-week-header">
        <span class="tc-week-label">${weekLabel}</span>
        <span class="tc-week-total">${weekTotalFormatted}</span>
      </div>

      <c:if test="${not empty weekSummary}">
        <c:forEach var="day" items="${weekSummary}">
          <c:set var="mins" value="${day.getTotalMinutes()}" />
          <c:set var="barPct" value="${day.getPercentOfDay()}" />
          <c:choose>
            <c:when test="${mins == 0}">
              <div class="tc-bar-row tc-bar-empty">
                <span class="tc-bar-label">${day.getDayLabel()}</span>
                <div class="tc-bar-track"><div class="tc-bar-fill" style="width:0%;"></div></div>
                <span class="tc-bar-hours">&mdash;</span>
              </div>
            </c:when>
            <c:otherwise>
              <c:set var="fillClass" value="tc-fill-regular" />
              <c:if test="${day.isToday()}"><c:set var="fillClass" value="tc-fill-today" /></c:if>
              <c:if test="${day.isOvertime()}"><c:set var="fillClass" value="tc-fill-overtime" /></c:if>
              <div class="tc-bar-row">
                <span class="tc-bar-label">${day.getDayLabel()}</span>
                <div class="tc-bar-track">
                  <div class="tc-bar-fill ${fillClass}" style="width:${barPct}%;"></div>
                </div>
                <span class="tc-bar-hours" ${day.isToday() ? 'style="color:#198754;"' : ''}>
                  ${day.getTotalFormatted()}
                </span>
              </div>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </c:if>

      <div class="tc-week-footer">
        <div>
          <span class="tc-stat-label">Avg/Day</span><br>
          <span class="tc-stat-value">${avgPerDayFormatted}</span>
        </div>
        <div style="text-align:center;">
          <span class="tc-stat-label">Target</span><br>
          <span class="tc-stat-value">40h 00m</span>
        </div>
        <div style="text-align:right;">
          <span class="tc-stat-label">Remaining</span><br>
          <span class="tc-stat-value">${remainingFormatted}</span>
        </div>
      </div>
    </div>
  </div>

</div> <%-- closes .tc-panel from timeClockHeader.jsp --%>

<%-- ── Tab switching + live timer JS ── --%>
<script>
  function tcSwitchTab(tab) {
    document.querySelectorAll('.tc-tab').forEach(function(t) { t.classList.remove('tc-active'); });
    document.querySelectorAll('.tc-tab-panel').forEach(function(p) { p.classList.remove('tc-visible'); });
    document.querySelector('[data-tctab="' + tab + '"]').classList.add('tc-active');
    document.getElementById(tab === 'today' ? 'tcPanelToday' : 'tcPanelWeek').classList.add('tc-visible');
  }

  <%-- Live running timer (only when clocked in) --%>
  <c:if test="${sessionScope.local.isUserIsIn()}">
  (function() {
    var el = document.getElementById('tcRunningTime');
    if (!el) return;
    // Parse the initial "Xh Ym" to seconds
    var text = el.textContent.trim();
    var match = text.match(/(\d+)h\s*(\d+)m/);
    var totalSec = 0;
    if (match) {
      totalSec = parseInt(match[1]) * 3600 + parseInt(match[2]) * 60;
    }
    setInterval(function() {
      totalSec++;
      var h = Math.floor(totalSec / 3600);
      var m = Math.floor((totalSec % 3600) / 60);
      var s = totalSec % 60;
      el.textContent = h + 'h ' + (m < 10 ? '0' : '') + m + 'm ' + (s < 10 ? '0' : '') + s + 's';
    }, 1000);
  })();
  </c:if>
</script>
