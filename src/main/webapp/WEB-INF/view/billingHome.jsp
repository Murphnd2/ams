<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Billing</title>
</head>
<body>
<div class="container-fluid">
    <div class="row">
        <div class="col offset-lg-1">
            <h3>${sessionScope.billingEmployer.getEmployerName()}</h3>
            <div class="row mb-1 fw-bolder">
                <div class="col-3">Name
                </div>
                <div class="col">COBRA
                </div>
                <div class="col">FSA
                </div>
                <div class="col">HRA
                </div>
                <div class="col">HSA
                </div>
                <div class="col">TRANSIT
                </div>
                <div class="col">DUAL
                </div>
            </div>
            <c:forEach var="grid" items="${sessionScope.billingGrid}">
                <div class="row mb-1">
                    <div class="col-3">
                        ${grid.getEmployee().getFirstName()} ${grid.getEmployee().getLastName()}
                    </div>
                    <div class="col">
                        ${grid.isCobra()}
                    </div>
                    <div class="col">
                            ${grid.isFlexSpend()}
                    </div>
                    <div class="col">
                            ${grid.isHealthReimb()}
                    </div>
                    <div class="col">
                            ${grid.isHsa()}
                    </div>
                    <div class="col">
                            ${grid.isTransit()}
                    </div>
                    <div class="col">
                            ${grid.isDualPlan()}
                    </div>
                </div>
            </c:forEach>
        </div>
        <div class="col-lg-1"></div>
    </div>
</div>
</body>
</html>
