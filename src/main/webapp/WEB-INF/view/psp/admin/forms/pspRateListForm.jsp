<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="cRateId" value="${sessionScope.currentRate.getId()}"></c:set>
<form method="post" action="RateView">
    <c:forEach var="rate" items="${requestScope.rateList}">
        <div class="row mb-1 mt-0">
            <div class="col">
                <div class="input-group input-group-sm">
                    <c:choose>
                        <c:when test="${rate.getId()==sessionScope.currentRate.getId()&&sessionScope.pspAdminHomeSender==1}">
                            <button type="button" class="btn btn-danger" disabled name="rateSelectButton" id="btnRate${rate.getId()}" value="${rate.getId()}">
                            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                            </button>
                            <div class="form-control text-danger fw-bold">
                                    ${rate.getDescription()}
                            </div>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" class="btn btn-secondary" name="rateSelectButton" id="btnRate${rate.getId()}" value="${rate.getId()}">
                                View
                            </button>
                            <div class="form-control">
                                    ${rate.getDescription()}
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </c:forEach>
</form>