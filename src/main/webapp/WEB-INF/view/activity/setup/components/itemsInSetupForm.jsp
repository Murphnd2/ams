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
  <c:if test="${moduleItem.getTemplatePurpose().getId()==11}">
    <c:set var="cPop" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==12}">
    <c:set var="cFsa" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==13}">
    <c:set var="cHra" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==16}">
    <c:set var="cHsa" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==14}">
    <c:set var="cCob" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==15}">
    <c:set var="cTra" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==17}">
    <c:set var="cPay" value="checked"></c:set>
  </c:if>
  <c:if test="${moduleItem.getTemplatePurpose().getId()==19}">
    <c:set var="cCrd" value="checked"></c:set>
  </c:if>
</c:forEach>
<div class="container-fluid form-control mb-2">
  <div class="row w-100">
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="fsaCheck" ${cFsa}>
        <label class="form-check-label" for="fsaCheck">FSA</label>
      </div>
    </div>
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="hraCheck" ${cHra}>
        <label class="form-check-label" for="hraCheck">HRA</label>
      </div>
    </div>
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="hsaCheck" ${cHsa}>
        <label class="form-check-label" for="hsaCheck">HSA</label>
      </div>
    </div>
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="popCheck" ${cPop}>
        <label class="form-check-label" for="popCheck">POP</label>
      </div>
    </div>
  </div>
  <div class="row w-100">
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="payCheck" ${cPay}>
        <label class="form-check-label" for="payCheck">CHECK<span class="d-none d-md-inline">S</span></label>
      </div>
    </div>
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="crdCheck" ${cCrd}>
        <label class="form-check-label" for="crdCheck">CARD<span class="d-none d-md-inline">S</span></label>
      </div>
    </div>
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="cobCheck" ${cCob}>
        <label class="form-check-label" for="cobCheck">COB<span class="d-none d-md-inline">RA</span></label>
      </div>
    </div>
    <div class="col-3">
      <div class="form-check form-check-inline">
        <input class="form-check-input" type="checkbox" id="trnCheck" ${cTra}>
        <label class="form-check-label" for="trnCheck">TRAN<span class="d-none d-md-inline">SIT</span></label>
      </div>
    </div>
  </div>
</div>


