<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="readOnly" value=""></c:set>
<c:set var="reqd" value=""></c:set>
<c:set var="visible" value="invisible"></c:set>
<c:if test="${sessionScope.personNotFound==true}">
    <c:set var="readOnly" value="readonly"></c:set>
    <c:set var="reqd" value="required"></c:set>
    <c:set var="visible" value=""></c:set>
</c:if>
<c:if test="${sessionScope.emailNotFound}==true">
    <c:set var="readOnly" value=""></c:set>
    <c:set var="reqd" value=""></c:set>
    <c:set var="visible" value="invisible"></c:set>
</c:if>
<div class="row mb-2">
    <div class="col">
        <input type="email" class="form-control" ${readOnly} name="emailName" id="emailName" value="${sessionScope.currentEmailString}">
    </div>
</div>
<div class="row ${visible} mb-2">
    <div class="col">
        <div class="input-group">
            <span class="input-group-text">Enter Name</span>
            <input class="form-control" type="text" name="firstName" ${reqd} placeholder="First">
            <input class="form-control" type="text" name="lastName" ${reqd} placeholder="Last">
        </div>
    </div>
</div>
<div class="row mb-2">
    <div class="col"></div>
    <div class="col-auto">
        <a class="btn btn-outline-secondary" href="EscapeEmail">Cancel</a>
    </div>
    <div class="col-auto">
        <button type="submit" class="btn btn-primary" name="btnSubmit" value="AT">
            <i class="bi bi-plus"></i> Add Recipient
        </button>
    </div>
</div>
