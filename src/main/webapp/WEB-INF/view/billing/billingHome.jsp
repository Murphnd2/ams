<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Billing Home</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
    <div class="row mb-3">
        <div class="col">
            <div class="form-control border-danger bg-danger text-white fw-bold text-uppercase">
                <fmt:formatDate value="${sessionScope.billingMonth.getFullDate()}" pattern="MMMM yyyy"></fmt:formatDate>
            </div>
        </div>
        <div class="col">
            <form method="post" action="ChangeBillingMonth">
                <div class="input-group d-print-none">
                    <span class="input-group-text">Select Month To View</span>
                    <select class="form-select m-0" aria-label="recurring freq type drop down" name="monthList" id="monthList">
                        <c:forEach var="month" items="${sessionScope.billingMonths}">
                            <c:choose>
                                <c:when test="${sessionScope.billingMonth.getMonthId()==month.getMonthId()}">
                                    <option selected value="${month.getMonthId()}">
                                        <fmt:formatDate value="${month.getFullDate()}" pattern="MMMM yyyy"></fmt:formatDate>
                                    </option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${month.getMonthId()}">
                                        <fmt:formatDate value="${month.getFullDate()}" pattern="MMMM yyyy"></fmt:formatDate>
                                    </option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                    <button type="submit" class="btn btn-success">
                        Go
                    </button>
                </div>
            </form>
        </div>
    </div>
    <c:choose>
        <c:when test="${sessionScope.billingView==1}">
            <c:set var="hColor" value="primary"></c:set>
            <c:set var="hName" value="${sessionScope.currentBillingEmployer.getEmployerName()}'s Employees"></c:set>
        </c:when>
        <c:otherwise>
            <c:set var="hColor" value="dark"></c:set>
            <c:set var="hName" value="Employer Name"></c:set>
        </c:otherwise>
    </c:choose>
    <div class="row mb-1">
        <div class="col-5">
            <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none text-uppercase">
                ${hName}
                <c:if test="${sessionScope.billingView==1}">                    &nbsp;
                    <a href="BillBack" class="text-white pe-auto d-print-none">(Back)</a>
                    <a href="EmailBillingToEmployer" class="text-white pe-auto d-print-none">{Email}</a>
                </c:if>
                <c:if test="${sessionScope.changeOnlyBilling.equals(\"Y\")}">
                    <a href="ResetBillingView" class="text-white pe-auto d-print-none">[Reset]</a>
                </c:if>
                    <c:if test="${sessionScope.changeOnlyBilling.equals(\"N\")}">
                        <a href="BillingChanges" class="text-white pe-auto d-print-none">[Filter]</a>
                    </c:if>
            </button>
        </div>
        <div class="col-7">
            <div class="row">
                <div class="col">
                    <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none">
                        Cobra
                    </button>
                </div>
                <div class="col">
                    <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none">
                        FSA
                    </button>
                </div>
                <div class="col">
                    <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none">
                        HRA
                    </button>
                </div>
                <div class="col">
                    <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none">
                        DUAL
                    </button>
                </div>
                <div class="col">
                    <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none">
                        Transit
                    </button>
                </div>
                <div class="col">
                    <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none">
                        HSA
                    </button>
                </div>
            </div>
        </div>
    </div>

    <c:choose>
        <c:when test="${sessionScope.billingView==1}">
            <form method="post" action="BillingEmployeeDetail">
                <c:forEach var="employee" items="${sessionScope.employeeSummary}" varStatus="loop">
                    <c:choose>
                        <c:when test="${loop.index % 2 ==0}">
                            <c:set var="outlineColor" value="primary"></c:set>
                            <c:set var="bgColor" value="bg-light"></c:set>
                        </c:when>
                        <c:otherwise>
                            <c:set var="outlineColor" value="secondary"></c:set>
                            <c:set var="bgColor" value=""></c:set>
                        </c:otherwise>
                    </c:choose>
                    <div class="row mb-1">
                        <div class="col-5">
                            <button type="submit" class="btn btn-outline-${outlineColor} btn-sm w-100 text-uppercase position-relative" id="btn${employee.getEmployee().getId()}" name="btnEmployee" value="${employee.getEmployee().getId()}">
                                    ${employee.getEmployee().getLastName()}, ${employee.getEmployee().getFirstName()}
                                        <c:if test="${employee.getEmployee().getCobraStatusId()==2}">
                                            &nbsp;(QB)
                                        </c:if>
                                        <c:if test="${employee.getEmployee().getCobraStatusId()==3}">
                                            &nbsp;(COBRA)
                                        </c:if>
                                        <c:if test="${employee.getCobraNet()!=0 || employee.getFsaNet()!=0 || employee.getHraNet()!=0 || employee.getTransitNet()!=0 || employee.getHsaNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle p-2 bg-danger border border-light rounded-circle">
                                    <span class="visually-hidden">New alerts</span>
                                </span>
                                </c:if>
                            </button>
                        </div>
                        <div class="col-7">
                            <div class="row">
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employee.getCobraCurrent()}
                                        <c:if test="${employee.getCobraNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employee.getCobraNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employee.getFsaCurrent()}
                                        <c:if test="${employee.getFsaNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employee.getFsaNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employee.getHraCurrent()}
                                        <c:if test="${employee.getHraNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employee.getHraNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employee.getDualCurrent()}
                                        <c:if test="${employee.getDualNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employee.getDualNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employee.getTranCurrent()}
                                        <c:if test="${employee.getTransitNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employee.getTransitNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employee.getHsaCurrent()}
                                        <c:if test="${employee.getHsaNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employee.getHsaNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </form>
        </c:when>
        <c:otherwise>
            <form method="post" action="BillingEmployerDetail">
                <c:forEach var="employer" items="${sessionScope.monthlySummary}" varStatus="loop">
                    <c:choose>
                        <c:when test="${loop.index % 2 ==0}">
                            <c:set var="outlineColor" value="primary"></c:set>
                            <c:set var="bgColor" value="bg-light"></c:set>
                        </c:when>
                        <c:otherwise>
                            <c:set var="outlineColor" value="secondary"></c:set>
                            <c:set var="bgColor" value=""></c:set>
                        </c:otherwise>
                    </c:choose>
                    <div class="row mb-1">
                        <div class="col-5">
                            <button type="submit" class="btn btn-outline-${outlineColor} btn-sm w-100 text-uppercase position-relative" id="btn${employer.getEmployer().getId()}" name="btnEmployer" value="${employer.getEmployer().getId()}">
                                    ${employer.getEmployer().getEmployerName()}
                                <c:if test="${employer.getCobraNet()!=0 || employer.getFsaNet()!=0 || employer.getHraNet()!=0 || employer.getTransitNet()!=0 || employer.getHsaNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle p-2 bg-danger border border-light rounded-circle">
                                    <span class="visually-hidden">New alerts</span>
                                </span>
                                </c:if>
                            </button>
                        </div>
                        <div class="col-7">
                            <div class="row">
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employer.getCobraCurrent()}
                                        <c:if test="${employer.getCobraNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employer.getCobraNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employer.getFsaCurrent()}
                                        <c:if test="${employer.getFsaNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employer.getFsaNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employer.getHraCurrent()}
                                        <c:if test="${employer.getHraNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employer.getHraNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employer.getDualCurrent()}
                                        <c:if test="${employer.getDualNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employer.getDualNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employer.getTranCurrent()}
                                        <c:if test="${employer.getTransitNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employer.getTransitNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                                <div class="col">
                                    <button type="button" class="btn btn-outline-${outlineColor} btn-sm ${bgColor} position-relative pe-none w-100">
                                            ${employer.getHsaCurrent()}
                                        <c:if test="${employer.getHsaNet()!=0}">
                                <span class="position-absolute top-50 start-100 translate-middle badge rounded-pill bg-danger">
                                        ${employer.getHsaNet()}
                                </span>
                                        </c:if>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </form>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
