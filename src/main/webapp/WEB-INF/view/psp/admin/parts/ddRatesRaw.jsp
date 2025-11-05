<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1">
    <div class="col-auto">
        <h4>Rate</h4>
    </div>
    <div class="col-auto">
        <button type="button" class="btn btn-outline-primary btn-sm" data-bs-toggle="offcanvas" data-bs-target="#addRateOC" >Add</button>
    </div>
    <div class="col">&nbsp;</div>
</div>
<div class="row mb-1">
    <div class="col">
        <select class="form-select mb-3" aria-label="rateList type drop down" name="rateList" id="rateList">
            <c:if test="${requestScope.rateList.size()<1}">
                <option value="0">MUST CREATE NEW RATE</option>
            </c:if>
            <c:forEach var="rateList" items="${requestScope.rateList}">
                <c:url value="ActivateRate" var="rateUrl">
                    <c:param name="rateId" value="${rateList.getId()}"></c:param>
                </c:url>
                <c:choose>
                    <c:when test="${rateList.getId() == sessionScope.currentRate.getId()}">
                        <option value="${rateList.getId()}" selected href="${rateUrl}">
                                ${rateList.getDescription()}</option>
                    </c:when>
                    <c:otherwise>
                        <option value="${rateList.getId()}" href="${rateUrl}">
                                ${rateList.getDescription()}</option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </select>
    </div>
</div>

