<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="MakeRecurringFromChecklist25">
  <div class="d-none" id="code_ys">
    <c:set var="cType" value="checked"></c:set>
    <c:set var="cM" value=""></c:set>
    <c:set var="cT" value=""></c:set>
    <c:set var="cW" value=""></c:set>
    <c:set var="cR" value=""></c:set>
    <c:set var="cF" value=""></c:set>
    <c:forEach var="dae" items="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getDoWList()}">
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
  </div>


  <div class="row mt-1">
    <div class="col-12 col-md-5 mt-1">
      <button type="button" class="btn btn-sm btn-secondary w-100 pe-none">Checklist Name</button>
    </div>
    <div class="col-12 col-md-7 mt-1">
      <input type="text" class="form-control form-control-sm" name="sequenceName" id="sequenceName" required
             placeholder="Name of the Recurring Checklist Goes Here" value="${sessionScope.local.getCurrentChecklist().getCheckList().getFullName()}">
    </div>
  </div>
  <div class="row mt-1">
    <div class="col-12 col-md-5 mt-1">
      <button type="button" class="btn btn-sm btn-secondary w-100 pe-none">Start Date</button>
    </div>
    <div class="col-12 col-md-7 mt-1">
      <input class="form-control form-control-sm" type="date" name="startDate" id="startDate" required value="${sessionScope.local.getCurrentChecklist().getCheckList().getDueDate()}">
    </div>
  </div>
  <div class="row mt-1">
    <div class="col-12 col-md-5 mt-1">
      <button type="button" class="btn btn-sm btn-secondary w-100 pe-none">Days in Advance</button>
    </div>
    <div class="col-12 col-md-7 mt-1">
      <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddDaysAdvance.jsp"></c:import>
    </div>
  </div>
  <div class="row mt-1">
    <div class="col-12 col-md-5 mt-1">
      <button type="button" class="btn btn-sm btn-secondary w-100 pe-none">Assigned To</button>
    </div>
    <div class="col-12 col-md-7 mt-1">
      <c:import url="/WEB-INF/view/a/general/ddUserList25.jsp"></c:import>
    </div>
  </div>
  <div class="row mt-1">
    <div class="col-12 col-md-5 mt-1">
      <button type="button" class="btn btn-sm btn-secondary w-100 pe-none">Frequency</button>
    </div>
    <div class="col-12 col-md-7 mt-1">
      <c:import url="/WEB-INF/view/a/checklistDetail/ddFrequency25.jsp"></c:import>
    </div>
  </div>
  <div class="row mt-1">
    <div class="col-12 col-md-5 mt-1">
      <button type="button" class="btn btn-sm btn-secondary w-100 pe-none">Repeats (Weekly Only)</button>
    </div>

    <div class="col-12 col-md-7 mt-1">
      <div class="row m-0 p-0">
        <div class="col ms-0">
          <input class="btn-check" ${cM} type="checkbox" id="cbMonday" name="cbMonday" value="1">
          <label class="btn btn-sm btn-outline-secondary w-100" for="cbMonday">M</label>
        </div>
        <div class="col">
          <input class="btn-check" ${cT} type="checkbox" id="cbTuesday" name="cbTuesday"  value="2">
          <label class="btn btn-sm btn-outline-secondary w-100" for="cbTuesday">T</label>
        </div>
        <div class="col">
          <input class="btn-check" ${cW} type="checkbox" id="cbWednesday" name="cbWednesday" value="3">
          <label class="btn btn-sm btn-outline-secondary w-100" for="cbWednesday">W</label>
        </div>
        <div class="col">
          <input class="btn-check" ${cR} type="checkbox" id="cbThursday" name="cbThursday" value="4">
          <label class="btn btn-sm btn-outline-secondary w-100" for="cbThursday">R</label>
        </div>
        <div class="col me-0">
          <input class="btn-check" ${cF} type="checkbox" id="cbFriday" name="cbFriday" value="5">
          <label class="btn btn-sm btn-outline-secondary w-100" for="cbFriday">F</label>
        </div>
      </div>
    </div>
  </div>



  <div class="row mt-2">
    <div class="col-md-5">

    </div>
    <div class="col-12 col-md-7">
      <button class="btn btn-success w-100" id="btnStandardRec" value="1" type="submit">Generate</button>
    </div>

  </div>
</form>
