<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${sessionScope.validEmployer.getEmployerName()}</title>
</head>
<body>
<div class="container-fluid mt-5">
  <div class="row mb-3">
    <div class="col">
      <h3 class="text-ssa fw-bold">
        ${sessionScope.validEmployer.getEmployerName()}
      </h3>
    </div>
  </div>
  <div class="row mb-3">
    <div class="col">
      <h3 class="form-control text-white fw-bold text-uppercase bg-altSsa" >
        <fmt:formatDate value="${sessionScope.erBillingMonth.getFullDate()}" pattern="MMMM yyyy"></fmt:formatDate>
      </h3>
    </div>
    <div class="col">
      <form method="post" action="EmployerBillingDetail">
        <div class="input-group d-print-none">
          <span class="input-group-text">Select Month To View</span>
          <select class="form-select m-0" aria-label="recurring freq type drop down" name="selectedMonth" id="selectedMonth">
            <c:forEach var="month" items="${sessionScope.erBillingMonthList}">
              <c:choose>
                <c:when test="${sessionScope.erBillingMonth.getMonthId()==month.getMonthId()}">
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
          <button type="submit" class="btn btn-success btn-altSsa">
            Go
          </button>
        </div>
      </form>
    </div>
  </div>
  <c:set var="hColor" value="primary btn-ssa"></c:set>
  <div class="row mb-1">
    <div class="col-5">
      <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none text-uppercase" style="background: #0d5681">
        EMPLOYEE LISTING
      </button>
    </div>
    <div class="col-7">
      <div class="row">
        <div class="col">
          <button type="button" class="btn btn-sm btn-${hColor} btn-ssa w-100 pe-none">
            Cobra
          </button>
        </div>
        <div class="col">
          <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none" >
            FSA
          </button>
        </div>
        <div class="col">
          <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none" >
            HRA
          </button>
        </div>
        <div class="col">
          <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none" >
            DUAL
          </button>
        </div>
        <div class="col">
          <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none" >
            Transit
          </button>
        </div>
        <div class="col">
          <button type="button" class="btn btn-${hColor} btn-sm w-100 pe-none" >
            HSA
          </button>
        </div>
      </div>
    </div>
  </div>
  <c:forEach var="employee" items="${sessionScope.erEmployeeSummary}" varStatus="loop">
    <c:choose>
      <c:when test="${loop.index % 2 ==0}">
        <c:set var="outlineColor" value="primary btn-outline-ssa"></c:set>
        <c:set var="bgColor" value="bg-light"></c:set>
        <c:set var="bStyle" value=""></c:set>
      </c:when>
      <c:otherwise>
        <c:set var="outlineColor" value="secondary btn-outline-altSsa"></c:set>
        <c:set var="bgColor" value=""></c:set>

        <c:set var="bStyle" value=""></c:set>
      </c:otherwise>
    </c:choose>
    <div class="row mb-1">
      <div class="col-5">
        <button type="submit" class="btn btn-outline-${outlineColor} btn-sm w-100 text-uppercase position-relative pe-none" id="btn${employee.getEmployee().getId()}" name="btnEmployee" value="${employee.getEmployee().getId()}"  ${bStyle}>
            ${employee.getEmployee().getLastName()}, ${employee.getEmployee().getFirstName()}
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

</div>
</body>
</html>
