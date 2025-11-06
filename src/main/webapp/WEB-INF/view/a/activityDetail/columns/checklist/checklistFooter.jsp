<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="allDone" value=""></c:set>
<c:if test="${sessionScope.sVar.getCurrentActivityToDos().size()>0 && sessionScope.sVar.getCurrentActivityToDos().get(0).isComplete()==false}">
  <c:set var="allDone" value="disabled"></c:set>
</c:if>
<c:if test="${sessionScope.isPspAdmin==true}">
  <c:set var="allDone" value=""></c:set>
</c:if>
<form method="post" action="closeActivity">
  <div class="row mt-2">
    <!-- Add Task Button -->
    <div class="col-6 col-md-4">
      <button class="btn btn-sm btn-success w-100" type="button"
              data-bs-toggle="modal" data-bs-target="#addToDoModal">
        <i class="bi bi-card-checklist"></i> Add Task
      </button>
    </div>

    <!-- Conditional Save Button -->
    <c:if test="${not empty sessionScope.local.pendingCloseIds}">
      <div class="col-6 col-md-4">
        <form method="post" action="PersistChecklist25" class="d-inline w-100">
          <button type="submit" class="btn btn-sm btn-warning w-100">
            <i class="bi bi-save"></i> Save Changes
          </button>
        </form>
      </div>
    </c:if>

    <!-- Close Activity Button (2 or 3 col layout) -->
    <div class="${not empty sessionScope.local.pendingCloseIds ? 'col-6 col-md-4' : 'col-6'}">
      <c:choose>
        <c:when test="${sessionScope.sVar.getClassName().equals('Setup')}">
          <button type="button" class="btn btn-sm btn-secondary w-100" ${allDone}
                  data-bs-toggle="modal" data-bs-target="#closeActivity">
            <i class="bi bi-door-open"></i> Close Setup
          </button>
        </c:when>
        <c:when test="${sessionScope.sVar.getClassName().equals('Renewal')}">
          <button type="button" class="btn btn-sm btn-primary w-100" ${allDone}
                  data-bs-toggle="modal" data-bs-target="#closeActivity">
            <i class="bi bi-door-open"></i> Close Renewal
          </button>
        </c:when>
        <c:when test="${sessionScope.sVar.getClassName().equals('Ticket')}">
          <button type="button" class="btn btn-sm btn-info w-100" ${allDone}
                  data-bs-toggle="modal" data-bs-target="#closeActivity">
            <i class="bi bi-door-open"></i> Close Ticket
          </button>
        </c:when>
        <c:otherwise>
          <button type="button" class="btn btn-sm btn-warning w-100" ${allDone}
                  data-bs-toggle="modal" data-bs-target="#closeActivity">
            <i class="bi bi-door-open"></i> Close Checklist
          </button>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
  <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/modals/closeActivityModal.jsp"></c:import>
</form>
<c:import url="/WEB-INF/view/a/checklistDetail/addToDoModal.jsp"></c:import>