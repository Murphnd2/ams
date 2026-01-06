<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row overflow-auto" style="max-height: 700px">
    <div class="col">
        <c:set var="cp" value="${sessionScope.local.getCurrentPerson().getId()}"></c:set>
        <form method="post" action="GoActivityDetail25">
            <input type="text" name="formSender" value="viewActivity" hidden>
            <c:set var="daysD" value="${applicationScope.global.getDaysSinceDanger()}"></c:set>
            <c:set var="daysW" value="${applicationScope.global.getDaysSinceWarning()}"></c:set>
            <c:forEach var="activity" items="${sessionScope.local.getFilteredActivityList()}">
                <c:set var="daysS" value="${activity.getDaysSinceContact()}"></c:set>
                <c:choose>
                    <c:when test="${activity.getAssignedTo()!=null && activity.getAssignedTo().getId()==cp}">
                        <c:set var="icCol1" value="dark"></c:set>
                        <c:set var="picture" value="person"></c:set>
                    </c:when>
                    <c:when test="${activity.isDelegated()}">
                        <c:set var="icCol1" value="secondary"></c:set>
                        <c:set var="picture" value="check-lg"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="icCol1" value="outline-dark"></c:set>
                        <c:set var="picture" value="collection"></c:set>
                    </c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${activity.getActivity().getClass().getSimpleName().equals(\"Renewal\")}">
                        <c:set var="aColor" value="primary"></c:set>
                        <c:set var="aSymbol" value="repeat"></c:set>
                    </c:when>
                    <c:when test="${activity.getActivity().getClass().getSimpleName().equals(\"Setup\")}">
                        <c:set var="aColor" value="secondary"></c:set>
                        <c:set var="aSymbol" value="buildings"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="aColor" value="info"></c:set>
                        <c:set var="aSymbol" value="ticket-detailed"></c:set>
                    </c:otherwise>
                </c:choose>

                <c:set var="extra" value=""></c:set>
                <c:if test="${activity.getActivity().getClass().getSimpleName().equals(\"Ticket\") && activity.getActivity().getPrimaryContact()!=null && activity.getActivity().getPrimaryContact().getEmployee()!=null}">
                    <c:set var="extra" value="(${activity.getActivity().getPrimaryContact().getEmployee().getEmployer().getEmployerName().toLowerCase()})"></c:set>
                </c:if>
                <c:choose>
                    <c:when test="${activity.isWaitingOnUs()}">
                        <c:set var="outline" value=""></c:set>
                        <c:set var="name" value="${activity.getActivity().getFullName().toUpperCase()}"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="outline" value="outline-"></c:set>
                        <c:set var="name" value="${activity.getActivity().getFullName().toLowerCase()}"></c:set>
                    </c:otherwise>
                </c:choose>
                <div class="row mb-1">
                    <div class="col">
                        <div class="input-group input-group-sm">
                            <button type="button" class="btn btn-${icCol1} pe-none d-none d-md-inline">
                                <i class="bi bi-${picture}"></i>
                            </button>
                            <button type="submit" class="btn btn-${outline}${aColor}" name="btnViewActivity" style="width:21%" id="${activity.getActivity().getId()}"
                                    value="${activity.getActivity().getId()}">
                                <i class="bi bi-${aSymbol}">
                                    <span class="d-none d-lg-inline">${activity.getActivity().getClass().getSimpleName()}</span>
                                </i>
                            </button>
                            <div class="form-control pe-none text-truncate">
                                <c:choose>
                                    <c:when test="${daysS>daysD}">
                                        <span class="fw-bold text-danger">${name}</span> <i>${extra}</i>
                                    </c:when>
                                    <c:when test="${daysS>daysW}">
                                        <span class="fw-bold">
                                                ${name}
                                        </span> <i>${extra}</i>
                                    </c:when>
                                    <c:otherwise>
                                        ${name} <i>${extra}</i>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div class="${activity.getDateHtml()} d-none d-xl-grid text-truncate" type="button" style="width:20%">
                                <fmt:formatDate value="${activity.getDueDate()}" pattern="MMM dd yyyy"></fmt:formatDate>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </form>
    </div>
</div>
