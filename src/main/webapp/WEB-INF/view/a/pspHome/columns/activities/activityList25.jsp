<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<style>
  .act-card {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    padding: 0.45rem 0.6rem;
    margin-bottom: 0.35rem;
    cursor: pointer;
    transition: all 0.15s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .act-card:hover { background: #f0f6fc; border-color: #b0c4d8; }
  .act-card.border-danger-left { border-left: 3px solid #dc3545; }
  .act-card.border-warning-left { border-left: 3px solid #ffc107; }
  .act-card.border-normal { border-left: 3px solid transparent; }

  .act-type-badge {
    font-size: 0.65rem;
    font-weight: 700;
    padding: 0.15rem 0.4rem;
    border-radius: 4px;
    min-width: 2rem;
    text-align: center;
    flex-shrink: 0;
  }
  .act-type-R { background: #e3edfd; color: #1565c0; }
  .act-type-S { background: #ede7f6; color: #5e35b1; }
  .act-type-T { background: #e0f7fa; color: #00838f; }

  .act-name { flex: 1; min-width: 0; font-size: 0.82rem; line-height: 1.25; }
  .act-name-text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

  .act-owner-icon { flex-shrink: 0; font-size: 0.85rem; }

  .act-extra { font-size: 0.7rem; color: #888; font-style: italic; }

  .act-due {
    flex-shrink: 0;
    font-size: 0.7rem;
    font-weight: 600;
    text-align: right;
    min-width: 4.5rem;
  }
  .act-due-ok   { color: #6c757d; }
  .act-due-soon { color: #495057; }
  .act-due-warn { color: #e65100; }
  .act-due-over { color: #c62828; }

  .act-urgency {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 0.15rem;
    width: 2rem;
    font-size: 0.78rem;
  }
  .act-urg-danger  { color: #dc3545; }
  .act-urg-warning { color: #e65100; }
  .act-urg-onus    { color: #dc3545; }
</style>

<c:set var="cp" value="${sessionScope.local.getCurrentPerson().getId()}"/>
<c:set var="daysD" value="${applicationScope.global.getDaysSinceDanger()}"/>
<c:set var="daysW" value="${applicationScope.global.getDaysSinceWarning()}"/>

<div class="overflow-auto" style="max-height: 700px;">
  <form method="post" action="GoActivityDetail25">
    <input type="hidden" name="formSender" value="viewActivity">

    <c:forEach var="activity" items="${requestScope.activityRows}">
      <c:set var="daysS" value="${activity.daysSinceContact}"/>

      <%-- Owner icon --%>
      <c:choose>
        <c:when test="${activity.assignedToId != null && activity.assignedToId == cp}">
          <c:set var="ownerIcon" value="person-fill"/>
          <c:set var="ownerColor" value="color:#0d5681;"/>
        </c:when>
        <c:when test="${activity.delegatedToMe}">
          <c:set var="ownerIcon" value="check-lg"/>
          <c:set var="ownerColor" value="color:#6c757d;"/>
        </c:when>
        <c:otherwise>
          <c:set var="ownerIcon" value="collection"/>
          <c:set var="ownerColor" value="color:#adb5bd;"/>
        </c:otherwise>
      </c:choose>

      <%-- Type badge --%>
      <c:choose>
        <c:when test="${activity.dtype == 'Renewal'}">
          <c:set var="typeLabel" value="R"/>
          <c:set var="typeCls" value="act-type-R"/>
        </c:when>
        <c:when test="${activity.dtype == 'Setup'}">
          <c:set var="typeLabel" value="S"/>
          <c:set var="typeCls" value="act-type-S"/>
        </c:when>
        <c:otherwise>
          <c:set var="typeLabel" value="T"/>
          <c:set var="typeCls" value="act-type-T"/>
        </c:otherwise>
      </c:choose>

      <%-- Employer extra for tickets --%>
      <c:set var="extra" value=""/>
      <c:if test="${activity.dtype == 'Ticket' && activity.ticketEmployerNameLc != null}">
        <c:set var="extra" value="${activity.ticketEmployerNameLc}"/>
      </c:if>

      <%-- Name display: waiting-on-us = UPPER + bold --%>
      <c:choose>
        <c:when test="${activity.waitingOnUs}">
          <c:set var="displayName" value="${fn:toUpperCase(activity.fullName)}"/>
          <c:set var="nameWeight" value="fw-bold"/>
        </c:when>
        <c:otherwise>
          <c:set var="displayName" value="${fn:toLowerCase(activity.fullName)}"/>
          <c:set var="nameWeight" value=""/>
        </c:otherwise>
      </c:choose>

      <%-- Contact urgency: phone icon (color-coded) + on-us icon --%>
      <c:set var="phoneIcon" value=""/>
      <c:set var="phoneCls" value=""/>
      <c:choose>
        <c:when test="${daysS > daysD}">
          <c:set var="phoneIcon" value="telephone-fill"/>
          <c:set var="phoneCls" value="act-urg-danger"/>
        </c:when>
        <c:when test="${daysS > daysW}">
          <c:set var="phoneIcon" value="telephone-fill"/>
          <c:set var="phoneCls" value="act-urg-warning"/>
        </c:when>
      </c:choose>
      <c:set var="onUsIcon" value=""/>
      <c:if test="${activity.waitingOnUs}">
        <c:set var="onUsIcon" value="stack-overflow"/>
      </c:if>

      <%-- Due bucket → card left border + date class --%>
      <c:choose>
        <c:when test="${activity.dueBucket >= 3}">
          <c:set var="borderCls" value="border-danger-left"/>
          <c:set var="dueCls" value="act-due-over"/>
        </c:when>
        <c:when test="${activity.dueBucket == 2}">
          <c:set var="borderCls" value="border-warning-left"/>
          <c:set var="dueCls" value="act-due-warn"/>
        </c:when>
        <c:when test="${activity.dueBucket == 1}">
          <c:set var="borderCls" value="border-normal"/>
          <c:set var="dueCls" value="act-due-soon"/>
        </c:when>
        <c:otherwise>
          <c:set var="borderCls" value="border-normal"/>
          <c:set var="dueCls" value="act-due-ok"/>
        </c:otherwise>
      </c:choose>

      <%-- Card row --%>
      <div class="act-card ${borderCls}" onclick="this.querySelector('button').click();">
        <%-- Owner icon --%>
        <span class="act-owner-icon" style="${ownerColor}">
          <i class="bi bi-${ownerIcon}"></i>
        </span>

        <%-- Type badge --%>
        <span class="act-type-badge ${typeCls}">${typeLabel}</span>

        <%-- Urgency icons: phone (needs contact) + stack (waiting on us) --%>
        <span class="act-urgency">
          <c:if test="${not empty onUsIcon}"><i class="bi bi-${onUsIcon} act-urg-onus"></i></c:if>
          <c:if test="${not empty phoneIcon}"><i class="bi bi-${phoneIcon} ${phoneCls}"></i></c:if>
        </span>

        <%-- Name + extra --%>
        <div class="act-name">
          <span class="act-name-text ${nameWeight}">
            ${displayName}<c:if test="${not empty extra}"> <span class="act-extra">(${extra})</span></c:if>
          </span>
        </div>

        <%-- Due date --%>
        <span class="act-due ${dueCls}">
          <c:choose>
            <c:when test="${activity.dueDate != null}">
              <fmt:formatDate value="${activity.dueDate}" pattern="MMM dd"/>
            </c:when>
            <c:otherwise>—</c:otherwise>
          </c:choose>
        </span>

        <%-- Hidden submit button --%>
        <button type="submit" name="btnViewActivity" value="${activity.activityId}"
                class="d-none" id="act${activity.activityId}"></button>
      </div>

    </c:forEach>

    <c:if test="${empty requestScope.activityRows}">
      <div class="text-center text-muted fst-italic py-4" style="font-size:0.88rem;">
        No matching activities
      </div>
    </c:if>
  </form>
</div>
