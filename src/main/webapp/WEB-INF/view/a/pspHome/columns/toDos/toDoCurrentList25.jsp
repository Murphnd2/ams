<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<style>
  /* ─── ToDo body (flex child of the column) ─── */
  .todo-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    background: #fff;
    border: 1px solid #dde2e7;
    border-top: none;
    border-radius: 0 0 8px 8px;
    overflow: hidden;
  }

  /* ─── Scrollable current items ─── */
  .todo-current {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 4px 0;
  }

  /* ─── Checklist item ─── */
  .cl-item { border-left: 4px solid #dee2e6; padding: 0.2rem 0.35rem; margin-bottom: 0.15rem; background: white; }
  .cl-item:hover { background: #f8f9fa; }
  .cl-item.overdue-critical { border-left-color: #dc3545; background: #fff5f5; }
  .cl-item.overdue { border-left-color: #fd7e14; }
  .cl-item.due-today { border-left-color: #0d5681; }
  .cl-item.tier-future { border-left-color: #dee2e6; }
  .cl-item.closed-item { border-left-color: #198754; background: #f8f9fa; opacity: 0.65; }

  .cl-name { font-size: 0.8rem; font-weight: 600; color: #212529; border: none; background: none; padding: 0; text-align: left; cursor: pointer; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .cl-name:hover { color: #0d5681; }
  .cl-item.overdue-critical .cl-name { color: #dc3545; }
  .cl-item.overdue .cl-name { color: #fd7e14; }

  .cl-date { font-size: 0.7rem; color: #6c757d; white-space: nowrap; }
  .cl-item.overdue-critical .cl-date { color: #dc3545; font-weight: 600; }
  .cl-item.overdue .cl-date { color: #fd7e14; font-weight: 600; }
  .cl-badge-overdue { background: #dc3545; color: white; font-size: 0.6rem; padding: 0.05rem 0.3rem; border-radius: 10px; font-weight: 600; }

  .cl-btn { border: none; background: none; padding: 0; line-height: 1; font-size: 0.85rem; cursor: pointer; width: 1.4rem; text-align: center; flex-shrink: 0; }
  .cl-btn.check { color: #198754; transition: transform 0.15s, color 0.15s; }
  .cl-btn.check:hover { color: #0f5132; transform: scale(1.3); }
  .cl-btn.kebab { color: #6c757d; }
  .cl-btn.kebab:hover { color: #0d5681; }
  .cl-spacer { width: 1.4rem; flex-shrink: 0; }

  .cl-action-panel { background: #f8f9fa; border-radius: 0 0 3px 3px; padding: 0.3rem 0.4rem; margin-top: -0.2rem; margin-bottom: 0.2rem; border-left: 4px solid #0d5681; }

  /* ─── Section toggle bars (Future + Completed) ─── */
  .td-section-toggle {
    display: flex; align-items: center; gap: 6px;
    padding: 7px 12px; font-size: 0.75rem; font-weight: 600;
    color: #6c757d; cursor: pointer; user-select: none;
    border-top: 1px solid #eee; background: #fafbfc;
    flex-shrink: 0; transition: background 0.18s;
    text-decoration: none;
  }
  .td-section-toggle:hover { background: #f0f4f8; color: #0d5681; }
  .td-section-toggle .td-chevron {
    font-size: 0.65rem;
    transition: transform 0.25s ease;
    display: inline-block;
  }
  .td-section-toggle.open .td-chevron { transform: rotate(90deg); }
  .td-section-toggle .td-count {
    background: #e8f0f7; color: #0d5681;
    font-size: 0.65rem; font-weight: 700;
    padding: 1px 7px; border-radius: 10px; margin-left: auto;
  }

  /* ─── Future items: slightly muted ─── */
  .cl-item.future-item { background: #fcfcfd; border-left-color: #e0e4e8; }
  .cl-item.future-item:hover { background: #f4f8fc; }
  .cl-item.future-item .cl-name { color: #555; font-weight: 500; }
</style>

<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<%-- TODO BODY — flex child that fills the remaining column height         --%>
<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<div class="todo-body">

  <%-- ═══ CURRENT / ACTIVE ITEMS (scrollable, flex-grow) ═══ --%>
  <div class="todo-current">
    <c:choose>
      <c:when test="${sessionScope.local.getChecklistsCurrent().size() > 0}">
        <c:forEach var="list" items="${sessionScope.local.getChecklistsCurrent()}">
          <c:choose>
            <c:when test="${list.getDueDate() <= Date.valueOf(LocalDate.now().minusDays(14))}">
              <c:set var="tierClass" value="overdue-critical"/>
            </c:when>
            <c:when test="${list.getDueDate() <= Date.valueOf(LocalDate.now().minusDays(1))}">
              <c:set var="tierClass" value="overdue"/>
            </c:when>
            <c:when test="${list.getDueDate() <= Date.valueOf(LocalDate.now())}">
              <c:set var="tierClass" value="due-today"/>
            </c:when>
            <c:otherwise>
              <c:set var="tierClass" value="tier-future"/>
            </c:otherwise>
          </c:choose>

          <form method="post" action="ChecklistAction25">
            <div class="cl-item ${tierClass}">
              <div class="d-flex align-items-center">
                <%-- Check or spacer --%>
                <c:choose>
                  <c:when test="${list.isSingleTasked()}">
                    <button type="submit" class="cl-btn check" name="btnCheckList" value="C-${list.getActivity().getId()}" title="Complete">
                      <i class="bi bi-check-circle"></i>
                    </button>
                  </c:when>
                  <c:otherwise>
                    <button type="submit" class="cl-btn check" style="color: var(--ssa);" name="btnCheckList" value="V-${list.getActivity().getId()}" title="Open">
                      <i class="bi bi-box-arrow-in-right"></i>
                    </button>
                  </c:otherwise>
                </c:choose>

                <%-- Name --%>
                <button type="submit" class="cl-name flex-grow-1 text-truncate mx-1" name="btnCheckList" value="V-${list.getActivity().getId()}">
                  ${list.getName()}
                </button>

                <%-- Date + overdue badge --%>
                <span class="cl-date me-1">
                  <fmt:formatDate value="${list.getDueDate()}" pattern="M/d"/>
                  <c:if test="${tierClass == 'overdue-critical'}">
                    <span class="cl-badge-overdue ms-1">!</span>
                  </c:if>
                </span>

                <%-- Kebab --%>
                <div class="dropdown">
                  <button type="button" class="cl-btn kebab" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="bi bi-three-dots-vertical"></i>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end" style="font-size: 0.8rem; min-width: 10rem;">
                    <li><button type="submit" class="dropdown-item py-1" name="btnCheckList" value="V-${list.getActivity().getId()}"><i class="bi bi-arrows-fullscreen me-2"></i>Open</button></li>
                    <li><a class="dropdown-item py-1" href="#" onclick="event.preventDefault(); togglePanel('date-${list.getActivity().getId()}');"><i class="bi bi-calendar-event me-2"></i>Change Date</a></li>
                    <li><a class="dropdown-item py-1" href="#" onclick="event.preventDefault(); togglePanel('owner-${list.getActivity().getId()}');"><i class="bi bi-person-gear me-2"></i>Reassign</a></li>
                    <c:if test="${list.isSingleTasked()}">
                      <li><hr class="dropdown-divider my-1"></li>
                      <li><button type="submit" class="dropdown-item py-1 text-success" name="btnCheckList" value="C-${list.getActivity().getId()}"><i class="bi bi-check-circle me-2"></i>Complete</button></li>
                    </c:if>
                  </ul>
                </div>
              </div>
            </div>

            <%-- Date change panel --%>
            <div class="collapse" id="date-${list.getActivity().getId()}">
              <div class="cl-action-panel">
                <div class="input-group input-group-sm">
                  <input type="date" class="form-control" name="newDueDate" value="${list.getDueDate()}">
                  <button type="submit" class="btn btn-sm btn-ssa" name="btnCheckList" value="D-${list.getActivity().getId()}"><i class="bi bi-check-lg"></i></button>
                </div>
              </div>
            </div>

            <%-- Reassign panel --%>
            <div class="collapse" id="owner-${list.getActivity().getId()}">
              <div class="cl-action-panel">
                <div class="input-group input-group-sm">
                  <c:import url="/WEB-INF/view/a/general/ddUserList25.jsp"/>
                  <button type="submit" class="btn btn-sm btn-ssa" name="btnCheckList" value="R-${list.getActivity().getId()}"><i class="bi bi-check-lg"></i></button>
                </div>
              </div>
            </div>
          </form>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <div class="text-center text-muted fst-italic py-3" style="font-size: 0.85rem;">
          <i class="bi bi-check-circle" style="font-size: 1.3rem; display: block; margin-bottom: 0.2rem; color: #c8c8c8;"></i>
          All caught up
        </div>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- ═══ FUTURE TASKS — Collapsible Section ═══ --%>
  <c:if test="${sessionScope.local.getChecklistsFuture().size() > 0}">
    <div class="td-section-toggle" id="futureToggle" onclick="tdToggleSection('future')">
      <i class="bi bi-chevron-right td-chevron" id="futureChevron"></i>
      <i class="bi bi-calendar-event" style="font-size: 0.8rem;"></i>
      Future Tasks
      <span class="td-count">${fn:length(sessionScope.local.getChecklistsFuture())}</span>
    </div>
    <div style="max-height:0; overflow:hidden; transition: max-height 0.3s ease; flex-shrink:0;" id="futureContent">
      <c:forEach var="checklist" items="${sessionScope.local.getChecklistsFuture()}">
        <form method="post" action="ChecklistAction25">
          <div class="cl-item future-item">
            <div class="d-flex align-items-center">
              <div class="cl-spacer"></div>
              <button type="submit" class="cl-name flex-grow-1 text-truncate mx-1" name="btnCheckList" value="V-${checklist.getActivity().getId()}">
                ${checklist.getName()}
              </button>
              <span class="cl-date">
                <fmt:formatDate value="${checklist.getDueDate()}" pattern="M/d"/>
              </span>
            </div>
          </div>
        </form>
      </c:forEach>
    </div>
  </c:if>

  <%-- ═══ COMPLETED — Collapsible Section ═══ --%>
  <c:if test="${sessionScope.local.getChecklistsClosed().size() > 0}">
    <div class="td-section-toggle" id="closedToggle" onclick="tdToggleSection('closed')">
      <i class="bi bi-chevron-right td-chevron" id="closedChevron"></i>
      <i class="bi bi-check-circle" style="font-size: 0.8rem;"></i>
      Completed
      <span class="td-count">${fn:length(sessionScope.local.getChecklistsClosed())}</span>
    </div>
    <div style="max-height:0; overflow:hidden; transition: max-height 0.3s ease; flex-shrink:0;" id="closedContent">
      <c:forEach var="list1" items="${sessionScope.local.getChecklistsClosed()}">
        <form method="post" action="ChecklistAction25">
          <div class="cl-item closed-item">
            <div class="d-flex align-items-center">
              <button type="submit" class="cl-btn check" style="color: #198754;" name="btnCheckList" value="U-${list1.getActivity().getId()}" title="Undo">
                <i class="bi bi-arrow-counterclockwise"></i>
              </button>
              <span class="mx-1 text-truncate" style="font-size: 0.8rem; text-decoration: line-through; color: #6c757d;">
                ${list1.getName()}
              </span>
            </div>
          </div>
        </form>
      </c:forEach>
    </div>
  </c:if>

</div>

<%-- ═══ JavaScript ═══ --%>
<script>
  function tdToggleSection(name) {
    var toggle = document.getElementById(name + 'Toggle');
    var content = document.getElementById(name + 'Content');
    if (!toggle || !content) return;
    var isOpen = toggle.classList.contains('open');
    if (isOpen) {
      content.style.maxHeight = '0';
      toggle.classList.remove('open');
    } else {
      content.style.maxHeight = content.scrollHeight + 'px';
      toggle.classList.add('open');
    }
  }

  function togglePanel(id) {
    var el = document.getElementById(id);
    if (el) { new bootstrap.Collapse(el, {toggle: true}); }
  }
</script>
