
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<div class="container-fluid m-0 p-3">

  <!-- Header / Activity Info -->
  <div class="row mb-3">
    <div class="col">
      <h5 class="mb-0">${sessionScope.local.getCurrentActivity().getActivity().getName()}</h5>
      <small class="text-muted">Assigned to: ${sessionScope.local.getCurrentActivity().getActivity().getAssignedTo().getFullName()}</small>
    </div>
  </div>

  <!-- ToDo Container -->
  <div id="todo-container" class="overflow-auto border rounded bg-white p-2" style="max-height:575px">
    <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}" varStatus="tds">
      <c:if test="${toDo.getTask().getId() != 153}">
        <div id="todo-${toDo.getToDo().getId()}" class="todo-item row m-0 p-1 border-bottom"
             data-complete="${toDo.isComplete()}"
             data-canearly="${toDo.allowsEarly()}"
             data-canfuture="${toDo.allowsFuture()}">

          <!-- Toggle Button -->
          <div class="col-auto m-0 p-0">
            <c:set var="iconName" value="${toDo.isComplete() ? 'x-square' : 'square'}" />
            <c:set var="peNone" value="${sessionScope.isPspAdmin ? '' : (toDo.isComplete() && !toDo.allowsNonOwner() && !sessionScope.local.getCurrentPerson().getId().equals(toDo.getTaskOwner()?.getId())) ? 'pe-none' : ''}" />
            <c:set var="isPast" value="${sessionScope.local.getCurrentActivity().getActivity().isComplete() ? 'pe-none' : ''}" />
            <button type="button"
                    class="btn btn-outline-cb border-white border-0 p-0 ${peNone} ${isPast}"
                    onclick="toggleToDo(this, ${toDo.getToDo().getId()})">
              <i class="bi bi-${iconName}" style="font-size:1.4rem"></i>
            </button>
          </div>

          <!-- Description -->
          <div class="col m-0 p-0">
            <span class="form-control border-white border-0"
                  style="font-size:0.65em;${toDo.isComplete() ? 'text-decoration:line-through;' : ''}">
                ${toDo.getDescription()}
            </span>
          </div>

          <!-- Info Link -->
          <c:if test="${!toDo.isComplete() && toDo.hasInfo() && toDo.getInfoLink() != null}">
            <div class="col-auto m-0 p-0 me-1">
              <a class="btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1"
                 href="${toDo.getInfoLink().getLinkPath()}" target="_blank">
                <i class="bi bi-question-lg"></i>
              </a>
            </div>
          </c:if>

          <!-- Manage Task -->
          <div class="col-auto m-0 p-0">
            <form method="post" action="ManageTask25" id="fm${toDo.getToDo().getId()}">
              <input type="hidden" name="toDoId" value="${toDo.getToDo().getId()}">
              <button type="submit" class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 ${peNone} mt-1 ${isPast}">
                <i class="bi bi-tools"></i>
              </button>
            </form>
          </div>
        </div>
      </c:if>
    </c:forEach>
  </div>

  <!-- Optional: Closed ToDos (collapsed) -->
  <div class="mt-3">
    <button class="btn btn-sm btn-outline-secondary" data-bs-toggle="collapse" data-bs-target="#closed-todos">
      Show Completed (<c:out value="${sessionScope.local.getCurrentActivity().getToDoList().stream().filter(t -> t.isComplete()).count()}" />)
    </button>
    <div id="closed-todos" class="collapse mt-2">
      <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}">
        <c:if test="${toDo.isComplete() && toDo.getTask().getId() != 153}">
          <div class="text-muted small">
            <i class="bi bi-check-square"></i> ${toDo.getDescription()}
          </div>
        </c:if>
      </c:forEach>
    </div>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
  function toggleToDo(btn, id) {
    const form = btn.closest('form');
    if (form) form.addEventListener('submit', e => e.preventDefault(), { once: true });

    fetch('CloseToDo25?btnToDo=' + id, {
      method: 'POST',
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
            .then(r => {
              if (!r.ok) throw new Error('HTTP ' + r.status);
              return r.json();
            })
            .then(data => {
              if (!data.success) return;

              const item = btn.closest('.todo-item');
              const isComplete = data.complete;
              item.dataset.complete = isComplete;

              const span = item.querySelector('span');
              if (isComplete) {
                span.style.textDecoration = 'line-through';
                span.classList.add('fst-italic', 'fw-lighter');
              } else {
                span.style.textDecoration = '';
                span.classList.remove('fst-italic', 'fw-lighter');
              }

              resortAndRefreshIcons();
            })
            .catch(err => console.error('Toggle failed:', err));
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
      if (icon) {
        const style = icon.getAttribute('style') || '';
        icon.className = 'bi';
        icon.classList.add(`bi-${newIcon}`);
        icon.setAttribute('style', style);
      }
    });

    items.sort((a, b) => {
      const aComp = a.dataset.complete === 'true';
      const bComp = b.dataset.complete === 'true';
      if (aComp && !bComp) return 1;
      if (!aComp && bComp) return -1;
      return parseInt(a.dataset.sort || '0') - parseInt(b.dataset.sort || '0');
    });

    items.forEach(i => container.appendChild(i));
  }
</script>