<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="container-fluid m-0 p-0">
  <div class="row m-0 p-0 overflow-auto" style="max-height:575px">
    <div class="col m-0 p-0">

      <c:set var="isPast"   value="${sessionScope.local.currentActivity.activity.complete ? 'pe-none' : ''}" />
      <c:set var="myId"     value="${sessionScope.local.currentPerson.id}" />
      <c:set var="isAdmin"  value="${sessionScope.isPspAdmin}" />

      <div id="todo-container">
        <c:forEach var="toDo" items="${sessionScope.local.currentActivity.toDoList}" varStatus="tds">
          <c:if test="${toDo.task.id != 153}">

            <c:set var="complete"      value="${toDo.complete}" />
            <c:set var="canEarly"      value="${toDo.allowsEarly()}" />
            <c:set var="canFuture"     value="${toDo.allowsFuture()}" />
            <c:set var="allowsNonOwn"  value="${toDo.allowsNonOwner()}" />
            <c:set var="hasOwner"      value="${toDo.hasOwner() && toDo.taskOwner != null}" />
            <c:set var="hasSource"     value="${toDo.isSourced() && toDo.sourceOwner != null}" />
            <c:set var="myTask"        value="${(hasOwner && toDo.taskOwner.id == myId) || (hasSource && toDo.sourceOwner.id == myId)}" />
            <c:set var="whoBlocked"    value="${!myTask && !allowsNonOwn && (hasOwner || hasSource)}" />
            <c:set var="delegated"     value="${(hasOwner && !myTask) || (hasSource && toDo.sourceOwner.id != myId)}" />
            <c:set var="notMyActivity" value="${!sessionScope.local.currentActivity.activity.assignedTo.id == myId && !myTask}" />

            <c:set var="baseIcon">
              <c:choose>
                <c:when test="${whoBlocked}">person-square</c:when>
                <c:when test="${delegated}">box-arrow-up-left</c:when>
                <c:when test="${notMyActivity}">circle</c:when>
                <c:otherwise>square</c:otherwise>
              </c:choose>
            </c:set>

            <c:set var="peNone" value="${!isAdmin && whoBlocked ? 'pe-none' : ''}" />

            <div class="todo-item row m-0 p-0"
                 data-id="${toDo.toDo.id}"
                 data-complete="${complete}"
                 data-baseicon="${baseIcon}"
                 data-penone="${peNone}"
                 data-canearly="${canEarly}"
                 data-canfuture="${canFuture}"
                 data-sort="${toDo.sortOrder}"
                 data-isadmin="${isAdmin}"
                 data-activitycomplete="${sessionScope.local.currentActivity.activity.complete}">

              <div class="col-auto m-0 p-0">
                <button type="button"
                        class="btn btn-outline-cb border-white border-0 m-0 p-0 ${peNone} ${isPast}"
                        onclick="window.todoToggle(this, ${toDo.toDo.id})">
                  <i class="bi bi-${complete ? (whoBlocked ? 'x-square-fill' : 'x-square') : baseIcon}"
                     style="font-size:1.4rem"></i>
                </button>
              </div>

              <div class="col m-0 p-0">
                <c:choose>
                  <c:when test="${!complete && toDo.hasGoto() && toDo.gotoLink != null}">
                    <div class="form-control border-white border-0">
                      <a href="${toDo.gotoLink.linkPath}" target="_blank" style="font-size:0.65em">
                          ${toDo.description}
                      </a>
                    </div>
                  </c:when>
                  <c:otherwise>
                    <div class="form-control border-white border-0">
                      <span style="font-size:0.65em;${complete ? 'text-decoration:line-through;' : ''}"
                            class="${complete ? 'fst-italic fw-lighter' : ''}">
                          ${toDo.description}
                      </span>
                    </div>
                  </c:otherwise>
                </c:choose>
              </div>

              <c:if test="${!complete && toDo.hasInfo() && toDo.infoLink != null}">
                <div class="col-auto m-0 p-0 me-1">
                  <a class="btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1"
                     href="${toDo.infoLink.linkPath}" target="_blank">
                    <i class="bi bi-question-lg"></i>
                  </a>
                </div>
              </c:if>

              <div class="col-auto m-0 p-0">
                <form method="post" action="ManageTask25">
                  <input type="hidden" name="toDoId" value="${toDo.toDo.id}">
                  <button type="submit"
                          class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 ${peNone} mt-1 ${isPast}">
                    <i class="bi bi-tools"></i>
                  </button>
                </form>
              </div>
            </div>

            <c:if test="${!canFuture && !complete}">
              <c:set var="blockFuture" value="true" scope="request"/>
            </c:if>
          </c:if>
        </c:forEach>
      </div>

    </div>
  </div>
</div>

<script>
  (function() {
    'use strict';

    // Safe guard
    if (typeof window.todoToggle !== 'undefined') return;

    function toggle(btn, id) {
      fetch('CloseToDo25?btnToDo=' + id, {
        method: 'POST',
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      })
              .then(r => r.json())
              .then(data => {
                if (!data.success) return;

                const item = btn.closest('.todo-item');
                const nowComplete = data.complete;
                item.dataset.complete = nowComplete;

                const i = btn.querySelector('i.bi');
                const base = item.dataset.baseicon;
                const isAdmin = item.dataset.isadmin === 'true';
                const activityComplete = item.dataset.activitycomplete === 'true';
                const whoBlocked = item.dataset.penone === 'pe-none' && !isAdmin;

                // Icon
                const newIcon = nowComplete
                        ? (whoBlocked ? 'x-square-fill' : 'x-square')
                        : base;
                i.className = 'bi';
                i.classList.add('bi-' + newIcon);
                i.setAttribute('style', 'font-size:1.4rem');

                // Button class
                btn.className = 'btn btn-outline-cb border-white border-0 m-0 p-0 ' +
                        (nowComplete && item.dataset.penone === 'pe-none' ? 'pe-none ' : '') +
                        (activityComplete ? 'pe-none' : '');

                // Description
                const span = item.querySelector('span');
                if (span) {
                  span.style.textDecoration = nowComplete ? 'line-through' : '';
                  if (nowComplete) {
                    span.classList.add('fst-italic', 'fw-lighter');
                  } else {
                    span.classList.remove('fst-italic', 'fw-lighter');
                  }
                }

                sortAndBlock();
              })
              .catch(err => console.error('Toggle failed:', err));
    }

    function sortAndBlock() {
      const container = document.getElementById('todo-container');
      if (!container) return;
      const items = Array.from(container.children);

      items.sort((a, b) => {
        const aComp = a.dataset.complete === 'true';
        const bComp = b.dataset.complete === 'true';
        if (!aComp && bComp) return -1;
        if (aComp && !bComp) return 1;
        return (parseInt(a.dataset.sort) || 0) - (parseInt(b.dataset.sort) || 0);
      });
      items.forEach(el => container.appendChild(el));

      let blockFuture = false;
      items.forEach(el => {
        const complete   = el.dataset.complete === 'true';
        const canEarly   = el.dataset.canearly === 'true';
        const canFuture  = el.dataset.canfuture === 'true';
        const btn        = el.querySelector('button');
        if (!btn) return;
        const i          = btn.querySelector('i.bi');
        if (!i) return;

        if (!complete && !canFuture) blockFuture = true;
        const timeBlocked = !complete && blockFuture && !canEarly;

        if (timeBlocked) {
          i.className = 'bi bi-clock-fill';
          btn.classList.add('pe-none');
        } else if (!complete) {
          const base = el.dataset.baseicon;
          i.className = 'bi';
          i.classList.add('bi-' + base);
          if (el.dataset.penone !== 'pe-none') btn.classList.remove('pe-none');
        }
      });
    }

    // Init
    document.addEventListener('DOMContentLoaded', () => {
      sortAndBlock();
      window.todoToggle = toggle;
    });
  })();
</script>