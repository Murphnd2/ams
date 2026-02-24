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

  <%-- Checklist Name --%>
  <div class="mb-3">
    <label class="form-label text-ssa fw-semibold" style="font-size:0.85rem;">
      <i class="bi bi-journal-text me-1"></i>Checklist Name
    </label>
    <input type="text" class="form-control form-control-sm" name="sequenceName" id="sequenceName" required
           placeholder="Name of the Recurring Checklist" value="${sessionScope.local.getCurrentChecklist().getCheckList().getFullName()}">
  </div>

  <div class="row g-3 mb-3">
    <%-- Start Date --%>
    <div class="col-sm-6">
      <label class="form-label text-ssa fw-semibold" style="font-size:0.85rem;">
        <i class="bi bi-calendar-event me-1"></i>Start Date
      </label>
      <input class="form-control form-control-sm" type="date" name="startDate" id="startDate" required
             value="${sessionScope.local.getCurrentChecklist().getCheckList().getDueDate()}">
    </div>

    <%-- Days in Advance --%>
    <div class="col-sm-6">
      <label class="form-label text-ssa fw-semibold" style="font-size:0.85rem;">
        <i class="bi bi-clock-history me-1"></i>Days in Advance
      </label>
      <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddDaysAdvance.jsp"></c:import>
    </div>
  </div>

  <div class="row g-3 mb-3">
    <%-- Assigned To --%>
    <div class="col-sm-6">
      <label class="form-label text-ssa fw-semibold" style="font-size:0.85rem;">
        <i class="bi bi-person me-1"></i>Assigned To
      </label>
      <c:import url="/WEB-INF/view/a/general/ddUserList25.jsp"></c:import>
    </div>

    <%-- Frequency --%>
    <div class="col-sm-6">
      <label class="form-label text-ssa fw-semibold" style="font-size:0.85rem;">
        <i class="bi bi-arrow-repeat me-1"></i>Frequency
      </label>
      <c:import url="/WEB-INF/view/a/checklistDetail/ddFrequency25.jsp"></c:import>
    </div>
  </div>

  <%-- Day of Week (Weekly Only) --%>
  <div class="mb-3">
    <label class="form-label text-ssa fw-semibold" style="font-size:0.85rem;">
      <i class="bi bi-calendar-week me-1"></i>Repeats On <span style="font-weight:400; color:#6c757d;">(weekly only)</span>
    </label>
    <div class="d-flex gap-2">
      <div class="flex-fill">
        <input class="btn-check" ${cM} type="checkbox" id="cbMonday" name="cbMonday" value="1">
        <label class="btn btn-sm btn-outline-ssa w-100" for="cbMonday">M</label>
      </div>
      <div class="flex-fill">
        <input class="btn-check" ${cT} type="checkbox" id="cbTuesday" name="cbTuesday" value="2">
        <label class="btn btn-sm btn-outline-ssa w-100" for="cbTuesday">T</label>
      </div>
      <div class="flex-fill">
        <input class="btn-check" ${cW} type="checkbox" id="cbWednesday" name="cbWednesday" value="3">
        <label class="btn btn-sm btn-outline-ssa w-100" for="cbWednesday">W</label>
      </div>
      <div class="flex-fill">
        <input class="btn-check" ${cR} type="checkbox" id="cbThursday" name="cbThursday" value="4">
        <label class="btn btn-sm btn-outline-ssa w-100" for="cbThursday">R</label>
      </div>
      <div class="flex-fill">
        <input class="btn-check" ${cF} type="checkbox" id="cbFriday" name="cbFriday" value="5">
        <label class="btn btn-sm btn-outline-ssa w-100" for="cbFriday">F</label>
      </div>
    </div>
  </div>

  <%-- Submit --%>
  <button class="btn btn-ssa w-100" id="btnStandardRec" value="1" type="submit">
    <i class="bi bi-arrow-repeat me-1"></i>Generate Recurring Checklist
  </button>
</form>
