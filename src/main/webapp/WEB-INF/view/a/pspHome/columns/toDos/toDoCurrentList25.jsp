<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<style>
  .cl-item { border-left: 4px solid #dee2e6; border-radius: 3px; padding: 0.2rem 0.35rem; margin-bottom: 0.2rem; background: white; }
  .cl-item:hover { background: #f8f9fa; }
  .cl-item.overdue-critical { border-left-color: #dc3545; background: #fff5f5; }
  .cl-item.overdue { border-left-color: #fd7e14; }
  .cl-item.due-today { border-left-color: #0d5681; }
  .cl-item.future { border-left-color: #dee2e6; }
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
  .closed-toggle { font-size: 0.75rem; color: #6c757d; cursor: pointer; text-decoration: none; }
  .closed-toggle:hover { color: #0d5681; }
</style>

<%-- ===== CURRENT CHECKLISTS ===== --%>
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
          <c:set var="tierClass" value="future"/>
        </c:otherwise>
      </c:choose>

      <form method="post" action="ChecklistAction25">
        <div class="cl-item ${tierClass}">
          <div class="d-flex align-items-center">
            <%-- Check or spacer — keeps names aligned --%>
            <c:choose>
              <c:when test="${list.isSingleTasked()}">
                <button type="submit" class="cl-btn check" name="btnCheckList" value="C-${list.getActivity().getId()}" title="Complete">
                  <i class="bi bi-check-circle"></i>
                </button>
              </c:when>
              <c:otherwise>
                <div class="cl-spacer"></div>
              </c:otherwise>
            </c:choose>

            <%-- Name — truncates --%>
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

<%-- ===== UPCOMING (compact dropdown) ===== --%>
<c:if test="${sessionScope.local.getChecklistsFuture().size() > 0}">
  <form method="post" action="ChecklistAction25">
    <div class="d-flex align-items-center mt-3 mb-1 gap-1">
      <button type="submit" class="btn btn-outline-ssa btn-sm" style="font-size: 0.7rem; padding: 0.15rem 0.4rem;">
        <i class="bi bi-arrows-fullscreen"></i>
      </button>
      <select class="form-select form-select-sm" name="btnCheckList" style="font-size: 0.75rem;">
        <c:forEach var="checklist" items="${sessionScope.local.getChecklistsFuture()}">
          <option value="V-${checklist.getActivity().getId()}">${checklist.getName()} (<fmt:formatDate value="${checklist.getDueDate()}" pattern="M/d"/>)</option>
        </c:forEach>
      </select>
    </div>
  </form>
</c:if>

<%-- ===== CLOSED (COMPLETED TODAY) ===== --%>
<c:if test="${sessionScope.local.getChecklistsClosed().size() > 0}">
  <div class="mt-2 mb-1">
    <a class="closed-toggle" data-bs-toggle="collapse" href="#closedChecklists" role="button" aria-expanded="false">
      <i class="bi bi-chevron-right me-1" id="closedChevron"></i>Completed (${sessionScope.local.getChecklistsClosed().size()})
    </a>
  </div>
  <div class="collapse" id="closedChecklists">
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
  <script>
    document.getElementById('closedChecklists')?.addEventListener('show.bs.collapse', function(){
      document.getElementById('closedChevron')?.classList.replace('bi-chevron-right','bi-chevron-down');
    });
    document.getElementById('closedChecklists')?.addEventListener('hide.bs.collapse', function(){
      document.getElementById('closedChevron')?.classList.replace('bi-chevron-down','bi-chevron-right');
    });
  </script>
</c:if>

<script>
  function togglePanel(id) {
    const el = document.getElementById(id);
    if (el) { new bootstrap.Collapse(el, {toggle: true}); }
  }
</script>
