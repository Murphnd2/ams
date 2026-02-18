<%@ page import="net.superiorstate.ams.model.sales.agency.RateTable" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="border border-dark rounded-3 p-2">
<form method="get" action="ModifyRateTable" class="w-100 border-1 rounded-3 border-dark">
    <c:set var="headerName" value=""></c:set>
    <c:forEach var="rateTableList" items="${sessionScope.rateTableList}">
        <c:set var="uniqueIdCode" value="${rateTableList.getRate().getId()}-${rateTableList.getModule().getId()}-${rateTableList.getPriceItem().getId()}"></c:set>
        <c:if test="${rateTableList.getModule().getDescription() != headerName}">
            <c:set var="headerName" value="${rateTableList.getModule().getDescription()}"></c:set>
            <div class="row mb-1">
                <div class="col">
                        ${rateTableList.getModule().getDescription()}
                </div>
            </div>
        </c:if>
        <div class="row mb-1">
            <div class="col offset-lg-1">
                <div class ="input-group input-group-sm">
                    <div class="form-control">
                            ${rateTableList.getPriceItem().getDescription()}
                    </div>
                    <span class="input-group-text">
                       <fmt:formatNumber value="${rateTableList.getPrice()}" type="CURRENCY"></fmt:formatNumber>
                    </span>
                    <button type="submit" <c:if test="${sessionScope.rateLock}">hidden</c:if> class="btn btn-secondary" name="btnRateTable1" id="btn${uniqueIdCode}" value="${uniqueIdCode}">
                        Del
                    </button>
                </div>
            </div>
        </div>
    </c:forEach>
</form>
</div>