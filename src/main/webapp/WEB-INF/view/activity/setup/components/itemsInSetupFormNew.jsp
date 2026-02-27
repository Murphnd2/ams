<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="cPop" value=""></c:set>
<c:set var="cFsa" value=""></c:set>
<c:set var="cHra" value=""></c:set>
<c:set var="cHsa" value=""></c:set>
<c:set var="cCob" value=""></c:set>
<c:set var="cTra" value=""></c:set>
<c:set var="cPay" value=""></c:set>
<c:set var="cCrd" value=""></c:set>
<c:forEach var="moduleItem" items="${sessionScope.moduleList}">
  <c:if test="${moduleItem.getServiceItem().getId()==11}">
    <c:set var="cPop" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==12}">
    <c:set var="cFsa" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==13}">
    <c:set var="cHra" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==16}">
    <c:set var="cHsa" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==14}">
    <c:set var="cCob" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==15}">
    <c:set var="cTra" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==17}">
    <c:set var="cPay" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getServiceItem().getId()==19}">
    <c:set var="cCrd" value="checked"></c:set>
  </c:if>
</c:forEach>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col-auto me-0 pe-0">
        <span class="text-secondary fw-bolder fs-5">Services To Implement</span>
      </div>
      <div class="col ms-0 ps-0">
        <button type="button" class="btn btn-sm"  data-bs-target="#addSetupItem" data-bs-toggle="modal">[add]</button>
      </div>
      <div class="col-auto"></div>
    </div>
      <c:if test="${cFsa.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            FSA
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Flexible Spending Accounts
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cHra.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            HRA
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Health Reimbursement Arrangements
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cHsa.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            HSA
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Health Savings Accounts
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cPop.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            POP
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Premium Only Plans
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cPay.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            CHECKS
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Payment Services
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cCrd.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            CARDS
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Summit Debit Cards
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cCob.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            COBRA
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            COBRA Administration
          </span>
          </div>
        </div>
      </c:if>
      <c:if test="${cTra.equals(\"checked\")}">
        <div class="row">
          <div class="col-auto">
            <i class="bi bi-arrow-right"></i>
          </div>
          <div class="col">
          <span class="text-uppercase fw-bolder">
            TRANSIT
          </span>
          </div>
          <div class="col-auto">
          <span class="text-lowercase text-secondary">
            Commuter / Parking Plans
          </span>
          </div>
        </div>
      </c:if>
  </div>
</div>

