<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="EmployerRenewalDetailView">
    <c:forEach var="employer" items="${sessionScope.employerRenewalList}">
        <div class="row mb-1 mt-0">
            <div class="col">
                <div class="input-group input-group-sm">
                    <c:choose>
                        <c:when test="${sessionScope.currentEmployer.getId()==employer.getId()}">
                            <button type="button" class="btn btn-danger" disabled name="employerRenewalSelectButton" id="btnEmployerRenewal${employer.getId()}" value="${employer.getId()}">
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                            </button>
                            <c:set var="currentStyle" value="form-control text-danger fw-bold"></c:set>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" class="btn btn-secondary" name="employerRenewalSelectButton" id="btnEmployerRenewal${employer.getId()}" value="${employer.getId()}">
                                View
                            </button>
                            <c:set var="currentStyle" value="form-control"></c:set>
                        </c:otherwise>
                    </c:choose>
                    <div class="${currentStyle}">
                            ${employer.getEmployerName()}
                    </div>
                </div>
            </div>
        </div>
    </c:forEach>
</form>
