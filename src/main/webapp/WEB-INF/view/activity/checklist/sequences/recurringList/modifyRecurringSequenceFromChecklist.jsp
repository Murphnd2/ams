<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="ModifyRecurringTaskFromChecklist">
  <div class="row mb-2">
    <div class="input-group">
      <span class="input-group-text">Recurring List Name</span>
      <input type="text" class="form-control" name="sequenceName" id="sequenceName" required
             placeholder="Enter description here" value="${sessionScope.currentChecklist.getRecurringTaskList().getDescription()}">
      <label class="input-group-text" style="width:60px">${sessionScope.currentChecklist.getRecurringTaskList().getId()}</label>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Start Date</span>
        <input class="form-control" type="date" style="width:100px" name="startDate" id="startDate" required value="${sessionScope.currentChecklist.getRecurringTaskList().getDateStart()}">

        <span class="input-group-text">Days in Advance</span>
        <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddDaysAdvance.jsp"></c:import>
      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="input-group">
      <span class="input-group-text">Assigned To</span>
      <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddUsers.jsp"></c:import>
    </div>
  </div>
  <div class="row mb-2">
    <div class="input-group">
      <span class="input-group-text">Frequency</span>
      <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddFrequency.jsp"></c:import>
    </div>
  </div>
  <c:set var="cType" value="checked"></c:set>
  <c:set var="cM" value=""></c:set>
  <c:set var="cT" value=""></c:set>
  <c:set var="cW" value=""></c:set>
  <c:set var="cR" value=""></c:set>
  <c:set var="cF" value=""></c:set>
  <c:forEach var="dae" items="${sessionScope.currentChecklist.getRecurringTaskList().getDoWList()}">
    <c:if test="${dae.getWeekdayId()==1}">
      <c:set var="cM" value="${cType}"></c:set>
    </c:if>
    <c:if test="${dae.getWeekdayId()==2}">
      <c:set var="cT" value="${cType}"></c:set>
    </c:if>
    <c:if test="${dae.getWeekdayId()==3}">
      <c:set var="cW" value="${cType}"></c:set>
    </c:if>
    <c:if test="${dae.getWeekdayId()==4}">
      <c:set var="cR" value="${cType}"></c:set>
    </c:if>
    <c:if test="${dae.getWeekdayId()==5}">
      <c:set var="cF" value="${cType}"></c:set>
    </c:if>
  </c:forEach>
  <div class="row mb-2">
    <div class="col-auto">
      <div class="input-group">
        <span class="input-group-text">Repeats (Weekly Only)</span>
      </div>
    </div>
    <div class="col">
      <div class="form-check form-check-inline">
        <input class="form-check-input" ${cM} type="checkbox" id="cbMonday" name="cbMonday" value="1">
        <label class="form-check-label" for="cbMonday">M</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" ${cT} type="checkbox" id="cbTuesday" name="cbTuesday"  value="2">
        <label class="form-check-label" for="cbTuesday">T</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" ${cW} type="checkbox" id="cbWednesday" name="cbWednesday" value="3">
        <label class="form-check-label" for="cbWednesday">W</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" ${cR} type="checkbox" id="cbThursday" name="cbThursday" value="4">
        <label class="form-check-label" for="cbThursday">R</label>
      </div>
      <div class="form-check form-check-inline">
        <input class="form-check-input" ${cF} type="checkbox" id="cbFriday" name="cbFriday" value="5">
        <label class="form-check-label" for="cbFriday">F</label>
      </div>
    </div>
  </div><%----%>
  <c:set var="word1" value="Update"></c:set>
  <c:set var="ability" value=""></c:set>
  <c:if test="${sessionScope.currentChecklist.getRecurringTaskList().isInActive()==true}">
    <c:set var="word1" value="Restart"></c:set>
    <c:set var="ability" value="disabled"></c:set>
  </c:if>
  <div class="row mb-2">
    <div class="input-group">
      <button class="btn btn-success w-50" id="btnStandardRec"  name="btnRecurring"  value="1" type="submit">
        <i class="bi bi-play-btn"></i>
        ${word1}</button>
      <button class="btn btn-danger w-50" ${ability} id="btnCloseRec" value = "0" name="btnRecurring" type="submit">
        <i class="bi bi-stop-btn"></i>
        Stop Repeating</button>
    </div>
  </div>
</form>
