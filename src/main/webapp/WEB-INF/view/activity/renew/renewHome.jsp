<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Home</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
    <div class="row mb-2">
        <%-- ******* L E F T    C O L U M N   **************** --%>
        <div class="col-lg-3">
            <c:choose>
                <c:when test="${sessionScope.renewalView == 1}">

                </c:when>
                <c:when test="${sessionScope.renewalView == 2}">
                    <h3>Open Tasks</h3>
                </c:when>
                <c:otherwise>
                    <c:import url="/WEB-INF/view/authentication/timeclock/punchClock.jsp"></c:import>
                    <c:import url="/WEB-INF/view/activity/myActivityQuickLink.jsp"></c:import>
                </c:otherwise>
            </c:choose>
        </div>
        <%-- ******* C E N T E R    C O L U M N   **************** --%>
        <div class="col">
            <c:choose>
                <c:when test="${sessionScope.renewalView == 1}">
                    View 1
                    <c:import url="/WEB-INF/view/activity/renew/addRenewalForm.jsp"></c:import>
                </c:when>
                <c:when test="${sessionScope.renewalView == 2}">
                    <div class="row mb-2">
                        <div class="col-8">
                            <h3>View Renewal View</h3>
                        </div>
                        <div class="col-4">
                            <c:import url="/WEB-INF/view/activity/renew/renewalButtonStrip.jsp"></c:import>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="row mb-2">
                        <div class="col-8">
                            <h3>My Renewals</h3>
                        </div>
                        <div class="col-4">
                            <c:import url="/WEB-INF/view/activity/renew/renewalButtonStrip.jsp"></c:import>
                        </div>
                    </div>
                    <c:import url="/WEB-INF/view/activity/renew/activeRenewals.jsp"></c:import>
                </c:otherwise>
            </c:choose>

        </div>
        <%-- ******* R I G H T    C O L U M N   **************** --%>
        <div class="col-lg-3">
            <c:choose>
                <c:when test="${sessionScope.renewalView == 1}">

                </c:when>
                <c:when test="${sessionScope.renewalView == 2}">
                    <h3>History</h3>
                </c:when>
                <c:otherwise>
                    <h3>My Task List</h3>
                    Place Task List Here
                    <button type="button" class="btn btn-warning w-100" data-bs-toggle="modal" data-bs-target="#addRenewalModal">Do Something</button>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
<%-- ******* M O D A L S  A R E   B E L O W   **************** --%>
<c:import url="/WEB-INF/view/activity/renew/upcomingRenewalsModal.jsp"></c:import>
</body>
</html>
