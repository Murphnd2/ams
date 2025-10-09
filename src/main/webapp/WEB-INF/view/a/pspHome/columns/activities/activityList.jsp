<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row overflow-auto" style="max-height: 700px">
    <div class="col">
        <form method="post" action="goActivityDetail">
            <input type="text" name="formSender" value="viewActivity" hidden>
            <c:forEach var="activity" items="${sessionScope.local.getFilteredActivityList()}">

                <c:set var="icCol1" value="outline-dark"> </c:set>
                <c:if test="${activity.getOwnershipLevel(sessionScope.sVar.getCurrentPerson().getId())==1}">
                    <c:set var="icCol1" value="dark"> </c:set>
                </c:if>
                <c:if test="${activity.getOwnershipLevel(sessionScope.sVar.getCurrentPerson().getId())==2}">
                    <c:set var="icCol1" value="secondary"> </c:set>
                </c:if>
                <div class="row mb-1">
                    <div class="col">
                        <div class="input-group input-group-sm">
                            <button type="button" class="btn btn-${icCol1} pe-none d-none d-md-inline">
                                <i class="bi bi-${activity.getPicture(sessionScope.sVar.getCurrentPerson().getId())}"></i>
                            </button>
                                ${activity.getButtonHtml()}
                            <div class="form-control pe-none text-truncate">
                                    ${activity.getFullNameFormatted()}
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
