<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${sessionScope.currentChecklist.getRecurringTaskList()==null}">
    <div class="row mt-2">
      <div class="col">
        <button type="button" class="btn btn-outline-warning w-100" data-bs-target="#makeRecurringSequence" data-bs-toggle="modal">Make Recurring</button>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <div class="accordion mt-2" id="recurItem">
      <div class="accordion-item">
        <h2 class="accordion-header">
          <button class="accordion-button accordion-button-1 collapsed p-2" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
            View Repeat Settings
          </button>
        </h2>
        <div id="collapseOne" class="accordion-collapse collapse" data-bs-parent="#recurItem">
          <div class="accordion-body m-0 p-0">
            <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/modifyRecurringSequenceFromChecklistV1.jsp"></c:import>
          </div>
        </div>
      </div>
    </div>
  </c:otherwise>
</c:choose>
