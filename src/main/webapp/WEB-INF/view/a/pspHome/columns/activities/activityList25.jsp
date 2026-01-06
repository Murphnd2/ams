<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>

<div class="row overflow-auto" style="max-height: 700px">
    <div class="col">

        <!-- Use getter-style EL to match your existing app objects -->
        <c:set var="cp"
               value="${sessionScope.local.getCurrentPerson().getId()}"></c:set>

        <c:set var="daysD"
               value="${applicationScope.global.getDaysSinceDanger()}"></c:set>

        <c:set var="daysW"
               value="${applicationScope.global.getDaysSinceWarning()}"></c:set>



        <form method="post" action="GoActivityDetail25">
            <input type="text" name="formSender" value="viewActivity" hidden>

            <c:forEach var="activity" items="${requestScope.activityRows}">

                <c:set var="daysS" value="${activity.daysSinceContact}"></c:set>

                <!-- Icon: assigned-to-me first, else delegated-to-me -->
                <c:choose>
                    <c:when test="${activity.assignedToId != null && activity.assignedToId == cp}">
                        <c:set var="icCol1" value="dark"></c:set>
                        <c:set var="picture" value="person"></c:set>
                    </c:when>
                    <c:when test="${activity.delegatedToMe}">
                        <c:set var="icCol1" value="secondary"></c:set>
                        <c:set var="picture" value="check-lg"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="icCol1" value="outline-dark"></c:set>
                        <c:set var="picture" value="collection"></c:set>
                    </c:otherwise>
                </c:choose>

                <!-- Type color/symbol -->
                <c:choose>
                    <c:when test="${activity.dtype == 'Renewal'}">
                        <c:set var="aColor" value="primary"></c:set>
                        <c:set var="aSymbol" value="repeat"></c:set>
                    </c:when>
                    <c:when test="${activity.dtype == 'Setup'}">
                        <c:set var="aColor" value="secondary"></c:set>
                        <c:set var="aSymbol" value="buildings"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="aColor" value="info"></c:set>
                        <c:set var="aSymbol" value="ticket-detailed"></c:set>
                    </c:otherwise>
                </c:choose>

                <!-- Ticket extra (already lowercased in SQL) -->
                <c:set var="extra" value=""></c:set>
                <c:if test="${activity.dtype == 'Ticket' && activity.ticketEmployerNameLc != null}">
                    <c:set var="extra" value="(${activity.ticketEmployerNameLc})"></c:set>
                </c:if>

                <!-- Waiting-on-us controls name casing + outline -->
                <c:choose>
                    <c:when test="${activity.waitingOnUs}">
                        <c:set var="outline" value=""></c:set>
                        <c:set var="name" value="${fn:toUpperCase(activity.fullName)}"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="outline" value="outline-"></c:set>
                        <c:set var="name" value="${fn:toLowerCase(activity.fullName)}"></c:set>
                    </c:otherwise>
                </c:choose>

                <!-- Due bucket -> CSS class (Option 1) -->
                <c:choose>
                    <c:when test="${activity.dueBucket == 0}">
                        <c:set var="dueClass"
                               value="btn btn-outline-secondary text-lowercase fw-lighter pe-none"></c:set>
                    </c:when>
                    <c:when test="${activity.dueBucket == 1}">
                        <c:set var="dueClass"
                               value="btn btn-outline-dark pe-none"></c:set>
                    </c:when>
                    <c:when test="${activity.dueBucket == 2}">
                        <c:set var="dueClass"
                               value="btn btn-warning text-dark pe-none"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="dueClass"
                               value="btn btn-danger fw-bold pe-none"></c:set>
                    </c:otherwise>
                </c:choose>

                <div class="row mb-1">
                    <div class="col">
                        <div class="input-group input-group-sm">

                            <button type="button"
                                    class="btn btn-${icCol1} pe-none d-none d-md-inline">
                                <i class="bi bi-${picture}"></i>
                            </button>

                            <button type="submit"
                                    class="btn btn-${outline}${aColor}"
                                    name="btnViewActivity"
                                    style="width:21%"
                                    id="${activity.activityId}"
                                    value="${activity.activityId}">
                                <i class="bi bi-${aSymbol}">
                                    <span class="d-none d-lg-inline">${activity.dtype}</span>
                                </i>
                            </button>

                            <div class="form-control pe-none text-truncate">
                                <c:choose>
                                    <c:when test="${daysS > daysD}">
                                        <span class="fw-bold text-danger">${name}</span> <i>${extra}</i>
                                    </c:when>
                                    <c:when test="${daysS > daysW}">
                                        <span class="fw-bold">${name}</span> <i>${extra}</i>
                                    </c:when>
                                    <c:otherwise>
                                        ${name} <i>${extra}</i>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <div class="${dueClass} d-none d-xl-grid text-truncate"
                                 type="button" style="width:20%">
                                <c:choose>
                                    <c:when test="${activity.dueDate != null}">
                                        <fmt:formatDate value="${activity.dueDate}" pattern="MMM dd yyyy"></fmt:formatDate>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted">no due date</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                        </div>
                    </div>
                </div>

            </c:forEach>
        </form>

    </div>
</div>
