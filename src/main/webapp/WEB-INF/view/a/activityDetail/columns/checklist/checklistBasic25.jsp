<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container-fluid m-0 p-0">
  <div class="row m-0 p-0 overflow-auto" style="max-height:575px">
    <div class="col m-0 p-0">
      <c:set var="isPast" value="pe-none"></c:set>
      <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
        <c:set var="isPast" value=""></c:set>
      </c:if>
      <!-- Add this temporarily -->
      <div style="display:none">
        ToDo IDs:
        <c:forEach var="t" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
          [${t.getToDo().getId()}]
        </c:forEach>
      </div>
      <!-- ToDo Container -->
      <div id="todo-container">
        <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}" varStatus="tds">
          <c:if test="${toDo.getTask().getId()!=153}">
            <div id="todo-${toDo.getToDo().getId()}"
                 class="todo-item"
                 data-sort="${toDo.sortOrder}"
                 data-complete="${toDo.isComplete() ? 'true' : 'false'}"
                 data-canearly="${toDo.allowsEarly() ? 'true' : 'false'}"
                 data-canfuture="${toDo.allowsFuture() ? 'true' : 'false'}">

              <c:set var="isComp" value="${toDo.isComplete()}"></c:set>
              <c:set var="hasAuto" value="${toDo.hasAutomation()}"></c:set>
              <c:set var="canEarly" value="${toDo.allowsEarly()}"></c:set>
              <c:set var="canFuture" value="${toDo.allowsFuture()}"></c:set>
              <c:set var="canNonOwner" value="${toDo.allowsNonOwner()}"></c:set>
              <c:set var="hasOwner" value="${toDo.hasOwner() && toDo.getTaskOwner()!=null}"></c:set>
              <c:set var="isSourced" value="${toDo.isSourced() && toDo.getSourceOwner()!=null}"></c:set>
              <c:set var="hasGoto" value="${toDo.hasGoto() && toDo.getGotoLink()!=null}"></c:set>
              <c:set var="hasInfo" value="${toDo.hasInfo() && toDo.getInfoLink()!=null}"></c:set>
              <c:set var="myId" value="${sessionScope.local.getCurrentPerson().getId()}"></c:set>
              <c:set var="isMyTask" value="${(hasOwner && toDo.getTaskOwner().getId()==myId) || (isSourced && toDo.getSourceOwner().getId()==myId)}"></c:set>
              <c:set var="isDelegated" value="${(hasOwner && !isMyTask) || (isSourced && toDo.getSourceOwner().getId()!=myId)}"></c:set>
              <c:set var="isMyActivity" value="${myId==sessionScope.local.getCurrentActivity().getActivity().getAssignedTo().getId()}"></c:set>

              <c:set var="btnIcon" value="square"></c:set>
              <c:set var="styl" value=""></c:set>
              <c:set var="ital" value=""></c:set>
              <c:set var="formServlet" value="CloseToDo25"></c:set>
              <c:set var="peNone" value=""></c:set>

              <c:choose>
                <c:when test="${!isMyTask && !canNonOwner && (hasOwner || isSourced) && isComp}">
                  <c:set var="btnIcon" value="x-square-fill"></c:set>
                  <c:set var="styl" value="text-decoration:line-through;"></c:set>
                  <c:set var="ital" value="fst-italic fw-lighter"></c:set>
                  <c:set var="formServlet" value="ReOpenToDo25"></c:set>
                  <c:set var="peNone" value="pe-none"></c:set>
                </c:when>
                <c:when test="${isComp}">
                  <c:set var="btnIcon" value="x-square"></c:set>
                  <c:set var="styl" value="text-decoration:line-through;"></c:set>
                  <c:set var="ital" value="fst-italic fw-lighter"></c:set>
                  <c:set var="formServlet" value="ReOpenToDo25"></c:set>
                </c:when>
                <c:when test="${!isMyTask && !canNonOwner && (hasOwner || isSourced)}">
                  <c:set var="btnIcon" value="person-square"></c:set>
                  <c:set var="peNone" value="pe-none"></c:set>
                </c:when>
                <c:when test="${tds.index>0 && (!canEarly || blockFuture)}">
                  <c:set var="btnIcon" value="clock-fill"></c:set>
                  <c:set var="peNone" value="pe-none"></c:set>
                </c:when>
                <c:when test="${isDelegated}">
                  <c:set var="btnIcon" value="box-arrow-up-left"></c:set>
                </c:when>
                <c:when test="${!isMyActivity && !isMyTask}">
                  <c:set var="btnIcon" value="circle"></c:set>
                </c:when>
              </c:choose>

              <c:if test="${sessionScope.isPspAdmin==true}">
                <c:set var="peNone" value=""></c:set>
              </c:if>

              <div class="row m-0 p-0">
                <div class="col m-0 p-0">
                  <div class="input-group input-group-sm p-0 m-0">
                    <button type="button"
                            class="btn btn-outline-cb border-white border-0 m-0 p-0 ${peNone} ${isPast}"
                            onclick="toggleToDo(this, ${toDo.getToDo().getId()}, '${formServlet}')">
                      <i class="bi bi-${btnIcon}" style="font-size: 1.4rem"></i>
                    </button>

                    <c:choose>
                      <c:when test="${!toDo.isComplete() && hasGoto}">
                        <div class="form-control ${ital} border-white border-0">
                          <a href="${toDo.getGotoLink().getLinkPath()}" target="_blank" style="font-size:0.65em">${toDo.getDescription()}</a>
                        </div>
                      </c:when>
                      <c:otherwise>
                        <div class="form-control ${ital} border-white border-0">
                          <span style="font-size: 0.65em; ${styl}">${toDo.getDescription()}</span>
                        </div>
                      </c:otherwise>
                    </c:choose>
                  </div>
                </div>

                <c:if test="${!toDo.isComplete() && hasInfo}">
                  <div class="col-auto m-0 p-0 me-1">
                    <a class="btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1" href="${toDo.getInfoLink().getLinkPath()}" target="_blank">
                      <i class="bi bi-question-lg"></i>
                    </a>
                  </div>
                </c:if>

                <div class="col-auto m-0 p-0">
                  <form method="post" action="ManageTask25" id="fm${toDo.getToDo().getId()}">
                    <input type="text" name="toDoId" value="${toDo.getToDo().getId()}" hidden>
                    <button type="submit" class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 ${peNone} mt-1 ${isPast}">
                      <i class="bi bi-tools"></i>
                    </button>
                  </form>
                </div>
              </div>

              <c:if test="${!canFuture}">
                <c:set var="blockFuture" value="${true}"></c:set>
              </c:if>
            </div>
          </c:if>
        </c:forEach>
      </div>
    </div>
  </div>
</div>
<script>
  function toggleToDo(btn, id, servlet) {
    fetch(servlet + '?btnToDo=' + id, { method: 'POST' })
            .then(r => r.json())
            .then(data => {
              if (data.success) {
                const item = btn.closest('.todo-item');
                const isNowComplete = item.dataset.complete !== 'true';
                item.dataset.complete = isNowComplete ? 'true' : 'false';

                const desc = item.querySelector('.form-control span');
                if (isNowComplete) {
                  desc.style.textDecoration = 'line-through';
                  desc.classList.add('fst-italic', 'fw-lighter');
                } else {
                  desc.style.textDecoration = '';
                  desc.classList.remove('fst-italic', 'fw-lighter');
                }

                resortAndRefreshIcons();
              }
            });
  }

  function resortAndRefreshIcons() {
    const container = document.getElementById('todo-container');
    const items = Array.from(container.children);
    let blockFuture = false;

    items.forEach((item, idx) => {
      const isComplete = item.dataset.complete === 'true';
      const canEarly = item.dataset.canearly === 'true';
      const canFuture = item.dataset.canfuture === 'true';

      if (!canFuture && !isComplete) blockFuture = true;

      const isTimeBlocked = idx > 0 && (!canEarly || blockFuture);

      let newIcon = 'square';
      if (isComplete) newIcon = 'x-square';
      else if (isTimeBlocked) newIcon = 'clock-fill';

      const icon = item.querySelector('i.bi');
      const style = icon.getAttribute('style');

      icon.className = 'bi';
      icon.classList.add(`bi-${newIcon}`);
      icon.setAttribute('style', style);
    });

    items.sort((a, b) => {
      const aComp = a.dataset.complete === 'true';
      const bComp = b.dataset.complete === 'true';
      if (aComp && !bComp) return 1;
      if (!aComp && bComp) return -1;
      return parseInt(a.dataset.sort) - parseInt(b.dataset.sort);
    });
    items.forEach(item => container.appendChild(item));
  }
</script>