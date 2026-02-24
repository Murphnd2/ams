<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList()==null}">
    <div class="mt-2 px-2">
      <button type="button" class="btn btn-outline-ssa btn-sm w-100" data-bs-target="#makeRecurringSequence" data-bs-toggle="modal">
        <i class="bi bi-arrow-repeat me-1"></i>Make Recurring
      </button>
    </div>
  </c:when>
  <c:otherwise>
    <div class="card mt-2" style="border:1px solid #dee2e6;">
      <div class="hdr-bar d-flex align-items-center justify-content-between py-1 px-2" style="font-size:0.85rem; cursor:pointer;"
           data-bs-toggle="collapse" data-bs-target="#collapseRecurring" aria-expanded="false">
        <span><i class="bi bi-arrow-repeat me-1"></i>Repeat Settings</span>
        <i class="bi bi-chevron-down" style="font-size:0.7rem;"></i>
      </div>
      <div id="collapseRecurring" class="collapse">
        <div class="card-body p-2">
          <form method="post" action="ModifyRecurringTask25">
              <%-- Hidden fields the servlet requires --%>
            <input type="hidden" name="sequenceName"
                   value="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDescription()}">
            <input type="hidden" name="startDate"
                   value="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDateStart()}">
            <div class="d-none" id="code_xx">
              <c:set var="cType" value="checked"></c:set>
              <c:set var="cM" value=""></c:set>
              <c:set var="cT" value=""></c:set>
              <c:set var="cW" value=""></c:set>
              <c:set var="cR" value=""></c:set>
              <c:set var="cF" value=""></c:set>
              <c:forEach var="dae" items="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDoWList()}">
                <c:if test="${dae.getWeekdayId()==1}"><c:set var="cM" value="${cType}"></c:set></c:if>
                <c:if test="${dae.getWeekdayId()==2}"><c:set var="cT" value="${cType}"></c:set></c:if>
                <c:if test="${dae.getWeekdayId()==3}"><c:set var="cW" value="${cType}"></c:set></c:if>
                <c:if test="${dae.getWeekdayId()==4}"><c:set var="cR" value="${cType}"></c:set></c:if>
                <c:if test="${dae.getWeekdayId()==5}"><c:set var="cF" value="${cType}"></c:set></c:if>
              </c:forEach>
              <c:set var="word1" value="Update"></c:set>
              <c:set var="ability" value=""></c:set>
              <c:if test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().isInActive()==true}">
                <c:set var="word1" value="Restart"></c:set>
                <c:set var="ability" value="disabled"></c:set>
              </c:if>
            </div>

            <%-- Frequency --%>
            <div class="mb-2">
              <label class="form-label text-ssa fw-semibold mb-1" style="font-size:0.75rem;">
                <i class="bi bi-arrow-repeat me-1"></i>Frequency
              </label>
              <select class="form-select form-select-sm" name="frequencyList" id="frequencyList">
                <c:forEach var="freq" items="${applicationScope.global.getTaskFrequencies()}">
                  <c:choose>
                    <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getTaskFrequency().getId()==freq.getId()}">
                      <option selected value="${freq.getId()}">${freq.getDescription()}</option>
                    </c:when>
                    <c:otherwise>
                      <option value="${freq.getId()}">${freq.getDescription()}</option>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
              </select>
            </div>

            <%-- Days in Advance --%>
            <div class="mb-2">
              <label class="form-label text-ssa fw-semibold mb-1" style="font-size:0.75rem;">
                <i class="bi bi-clock-history me-1"></i>Days Ahead
              </label>
              <select class="form-select form-select-sm" name="daysInAdvance" id="daysInAdvance">
                <c:forEach var="num" begin="0" end="15" step="1">
                  <c:choose>
                    <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDaysInAdvance()==num}">
                      <option selected value="${num}">${num}</option>
                    </c:when>
                    <c:otherwise>
                      <option value="${num}">${num}</option>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
              </select>
            </div>

            <%-- Assigned To --%>
            <div class="mb-2">
              <label class="form-label text-ssa fw-semibold mb-1" style="font-size:0.75rem;">
                <i class="bi bi-person me-1"></i>Assigned To
              </label>
              <select class="form-select form-select-sm" name="userList" id="userList">
                <c:forEach var="user" items="${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser ? applicationScope.global.getBpoUsers() : applicationScope.global.getUsers()}">
                  <c:choose>
                    <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getAssignee().getId()==user.getId()}">
                      <option selected value="${user.getId()}">${user.getFullNameFirstLast()}</option>
                    </c:when>
                    <c:otherwise>
                      <option value="${user.getId()}">${user.getFullNameFirstLast()}</option>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
              </select>
            </div>

            <%-- Day of Week --%>
            <div class="mb-2">
              <label class="form-label text-ssa fw-semibold mb-1" style="font-size:0.75rem;">
                <i class="bi bi-calendar-week me-1"></i>Repeats On <span style="font-weight:400; color:#6c757d;">(weekly)</span>
              </label>
              <div class="d-flex gap-1">
                <div class="flex-fill">
                  <input class="btn-check" ${cM} type="checkbox" id="cbMonday1" name="cbMonday" value="1" autocomplete="off">
                  <label class="btn btn-sm btn-outline-ssa w-100" for="cbMonday1">M</label>
                </div>
                <div class="flex-fill">
                  <input class="btn-check" ${cT} type="checkbox" id="cbTuesday1" name="cbTuesday" value="2" autocomplete="off">
                  <label class="btn btn-sm btn-outline-ssa w-100" for="cbTuesday1">T</label>
                </div>
                <div class="flex-fill">
                  <input class="btn-check" ${cW} type="checkbox" id="cbWednesday1" name="cbWednesday" value="3" autocomplete="off">
                  <label class="btn btn-sm btn-outline-ssa w-100" for="cbWednesday1">W</label>
                </div>
                <div class="flex-fill">
                  <input class="btn-check" ${cR} type="checkbox" id="cbThursday1" name="cbThursday" value="4" autocomplete="off">
                  <label class="btn btn-sm btn-outline-ssa w-100" for="cbThursday1">R</label>
                </div>
                <div class="flex-fill">
                  <input class="btn-check" ${cF} type="checkbox" id="cbFriday1" name="cbFriday" value="5" autocomplete="off">
                  <label class="btn btn-sm btn-outline-ssa w-100" for="cbFriday1">F</label>
                </div>
              </div>
            </div>

            <%-- Ghost-style action buttons --%>
            <div class="d-flex gap-2 mt-3 pt-2" style="border-top:1px solid #eee;">
              <button class="btn btn-sm btn-outline-ssa flex-fill" id="btnStandardRec" name="btnRecurring" value="1" type="submit">
                <i class="bi bi-check-lg me-1"></i>${word1}
              </button>
              <button class="btn btn-sm btn-outline-danger flex-fill" ${ability} id="btnCloseRec" value="0" name="btnRecurring" type="submit">
                <i class="bi bi-x-lg me-1"></i>Stop Repeating
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </c:otherwise>
</c:choose>
