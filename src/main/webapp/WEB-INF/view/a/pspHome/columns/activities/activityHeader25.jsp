<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<script>
  var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })
</script>
<%-- ── Activity Filter State (hidden — drives checked/selected states) ── --%>
<c:set var="af" value="${sessionScope.local.getActivityFilter()}"/>
<div class="d-none" id="actFilterState">
  <c:set var="alpha" value=""/>
  <c:set var="calen" value="checked"/>
  <c:if test="${af.isSortAlphabetically()==true}">
    <c:set var="alpha" value="checked"/>
    <c:set var="calen" value=""/>
  </c:if>
  <c:choose>
    <c:when test="${af.getOwnershipFilter()==3}">
      <c:set var="wAll" value=""/><c:set var="wMePlus" value=""/><c:set var="wMe" value=""/><c:set var="wPlus" value="checked"/>
    </c:when>
    <c:when test="${af.getOwnershipFilter()==0}">
      <c:set var="wAll" value="checked"/><c:set var="wMePlus" value=""/><c:set var="wMe" value=""/><c:set var="wPlus" value=""/>
    </c:when>
    <c:when test="${af.getOwnershipFilter()==2}">
      <c:set var="wAll" value=""/><c:set var="wMePlus" value=""/><c:set var="wMe" value="checked"/><c:set var="wPlus" value=""/>
    </c:when>
    <c:otherwise>
      <c:set var="wAll" value=""/><c:set var="wMePlus" value="checked"/><c:set var="wMe" value=""/><c:set var="wPlus" value=""/>
    </c:otherwise>
  </c:choose>
  <c:set var="vr" value=""/><c:if test="${af.isViewRenewal()==true}"><c:set var="vr" value="checked"/></c:if>
  <c:set var="vs" value=""/><c:if test="${af.isViewSetup()==true}"><c:set var="vs" value="checked"/></c:if>
  <c:set var="vt" value=""/><c:if test="${af.isViewTicket()==true}"><c:set var="vt" value="checked"/></c:if>
  <c:choose>
    <c:when test="${af.isViewWaitingOnUs()==false && af.isViewNeedsContact()==false}">
      <c:set var="qou" value=""/><c:set var="qnc" value=""/>
    </c:when>
    <c:when test="${af.isViewWaitingOnUs()==false}">
      <c:set var="qou" value=""/><c:set var="qnc" value="checked"/>
    </c:when>
    <c:when test="${af.isViewNeedsContact()==false}">
      <c:set var="qou" value="checked"/><c:set var="qnc" value=""/>
    </c:when>
    <c:otherwise>
      <c:set var="qou" value="checked"/><c:set var="qnc" value="checked"/>
    </c:otherwise>
  </c:choose>
</div>

<%-- ── Header Bar ── --%>
<div class="mt-2">
  <div class="hdr-bar d-flex align-items-center justify-content-between">
    <%-- Left: quick-view buttons --%>
    <div class="d-flex align-items-center gap-1">
      <a href="FilterActivities25?viewAllActivities=ALL" class="btn btn-sm btn-outline-light" data-bs-toggle="tooltip" title="All Open">
        <i class="bi bi-journal-text"></i>
      </a>
      <a href="FilterActivities25?viewAllActivities=MY" class="btn btn-sm btn-outline-light" data-bs-toggle="tooltip" title="My Actionable">
        <i class="bi bi-journal-check"></i>
      </a>
      <a href="FilterActivities25?viewAllActivities=REN" class="btn btn-sm btn-outline-light" data-bs-toggle="tooltip" title="All Renewals">
        <i class="bi bi-journal-medical"></i>
      </a>
    </div>

    <%-- Center: title --%>
    <span>
      <i class="bi bi-activity me-1"></i>Activities
    </span>

    <%-- Right: filter toggle --%>
    <button class="btn btn-sm btn-outline-light" type="button" data-bs-toggle="collapse" data-bs-target="#actFilterPanel">
      <i class="bi bi-filter"></i>
    </button>
  </div>

  <%-- ── Collapsible Filter Panel ── --%>
  <div class="collapse border border-top-0 rounded-bottom" id="actFilterPanel" style="background:#f8f9fb;">
    <form action="FilterActivities25" method="post" class="p-2">
      <input type="hidden" value="filterButton" name="formSender">

      <%-- Row 1: Activity Type + Attention --%>
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
        <%-- Activity type toggles --%>
        <div class="d-flex align-items-center gap-1">
          <span class="text-ssa fw-bold" style="font-size:0.75rem;">TYPE</span>
          <div class="btn-group btn-group-sm" role="group">
            <input type="checkbox" class="btn-check" name="vRenew" id="vRenew" autocomplete="off" value="1" ${vr}>
            <label class="btn btn-outline-primary" for="vRenew" data-bs-toggle="tooltip" title="Renewals">
              <i class="bi bi-repeat"></i> R
            </label>
            <input type="checkbox" class="btn-check" name="vSetup" id="vSetup" autocomplete="off" value="3" ${vs}>
            <label class="btn btn-outline-secondary" for="vSetup" data-bs-toggle="tooltip" title="Setups">
              <i class="bi bi-buildings"></i> S
            </label>
            <input type="checkbox" class="btn-check" name="vTicket" id="vTicket" autocomplete="off" value="5" ${vt}>
            <label class="btn btn-outline-info" for="vTicket" data-bs-toggle="tooltip" title="Tickets">
              <i class="bi bi-ticket-detailed"></i> T
            </label>
          </div>
        </div>

        <%-- Attention toggles --%>
        <div class="d-flex align-items-center gap-1">
          <span class="text-danger fw-bold" style="font-size:0.75rem;">ATTENTION</span>
          <div class="btn-group btn-group-sm" role="group">
            <input type="checkbox" class="btn-check" name="fOnUs" id="fOnUs" autocomplete="off" value="1" ${qou}>
            <label class="btn btn-outline-danger" for="fOnUs" data-bs-toggle="tooltip" title="Waiting On Us">
              <i class="bi bi-stack-overflow"></i>
            </label>
            <input type="checkbox" class="btn-check" name="fCall" id="fCall" autocomplete="off" value="1" ${qnc}>
            <label class="btn btn-outline-danger" for="fCall" data-bs-toggle="tooltip" title="Needs Contact">
              <i class="bi bi-telephone"></i>
            </label>
          </div>
        </div>
      </div>

      <%-- Row 2: Ownership + Sort + Apply --%>
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
        <%-- Ownership radios --%>
        <div class="d-flex align-items-center gap-1">
          <span class="text-ssa fw-bold" style="font-size:0.75rem;">OWNER</span>
          <div class="btn-group btn-group-sm" role="group">
            <input type="radio" class="btn-check" name="whoFilter" id="whoAll" autocomplete="off" value="0" ${wAll}>
            <label class="btn btn-outline-dark" for="whoAll" data-bs-toggle="tooltip" title="All Open">
              <i class="bi bi-people"></i>
            </label>
            <input type="radio" class="btn-check" name="whoFilter" id="whoAllMe" autocomplete="off" value="1" ${wMePlus}>
            <label class="btn btn-outline-dark" for="whoAllMe" data-bs-toggle="tooltip" title="All My Activities">
              <i class="bi bi-person-plus"></i>
            </label>
            <input type="radio" class="btn-check" name="whoFilter" id="whoOnlyMe" autocomplete="off" value="2" ${wMe}>
            <label class="btn btn-outline-dark" for="whoOnlyMe" data-bs-toggle="tooltip" title="Assigned to Me Only">
              <i class="bi bi-person"></i>
            </label>
            <input type="radio" class="btn-check" name="whoFilter" id="whoHelp" autocomplete="off" value="3" ${wPlus}>
            <label class="btn btn-outline-dark" for="whoHelp" data-bs-toggle="tooltip" title="Delegated Tasks Only">
              <i class="bi bi-person-check"></i>
            </label>
          </div>
        </div>

        <%-- Sort + Apply --%>
        <div class="d-flex align-items-center gap-2">
          <div class="d-flex align-items-center gap-1">
            <span class="text-ssa fw-bold" style="font-size:0.75rem;">SORT</span>
            <div class="btn-group btn-group-sm" role="group">
              <input type="radio" class="btn-check" name="fAlpha" id="fCal" autocomplete="off" value="0" ${calen}>
              <label class="btn btn-outline-dark" for="fCal" data-bs-toggle="tooltip" title="Sort by Due Date">
                <i class="bi bi-calendar2"></i>
              </label>
              <input type="radio" class="btn-check" name="fAlpha" id="fAlpha" autocomplete="off" value="1" ${alpha}>
              <label class="btn btn-outline-dark" for="fAlpha" data-bs-toggle="tooltip" title="Sort Alphabetically">
                <i class="bi bi-alphabet-uppercase"></i>
              </label>
            </div>
          </div>
          <button type="submit" class="btn btn-sm btn-ssa">
            <i class="bi bi-check-lg"></i> Apply
          </button>
        </div>
      </div>
    </form>
  </div>
</div>
