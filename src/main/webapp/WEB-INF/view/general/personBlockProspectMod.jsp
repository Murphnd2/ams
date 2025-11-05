<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container">
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="firstName" class="col-3 input-group-text">First Name</label>
                <input type="text" value="${sessionScope.currentProspect.getContact().getFirstName()}" <c:if test="${sessionScope.formDisable3}">disabled</c:if> class="form-control" id="firstName" name="firstName" required/>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="lastName" class="col-3 input-group-text">Last Name</label>
                <input type="text" value="${sessionScope.currentProspect.getContact().getLastName()}" <c:if test="${sessionScope.formDisable3}">disabled</c:if> class="form-control" id="lastName" name="lastName" required/>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="email" class="col-3 input-group-text">Email</label>
                <input type="email" value="${sessionScope.currentProspect.getContact().getEmail()}" <c:if test="${sessionScope.formDisable3}">disabled</c:if> class="form-control" id="email" name="email" required placeholder="name@email.com"/>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="title" class="col-3 input-group-text">Title</label>
                <input type="text" value="${sessionScope.currentProspect.getContact().getTitle()}" <c:if test="${sessionScope.formDisable3}">disabled</c:if> class="form-control" id="title" name="title"/>
            </div>
        </div>
    </div>
    <div class="row mb-3">
        <div class="col">
            <div class="input-group input-group-sm">
                <label for="personPhone" class="col-3 input-group-text">Phone #</label>
                <input type="tel" value="${sessionScope.currentProspect.getContact().getPhone()}" <c:if test="${sessionScope.formDisable3}">disabled</c:if> class="form-control" id="personPhone" name="personPhone" placeholder="###-555-1234" pattern="[0-9]{3}-[0-9]{3}-[0-9]{4}"/>
            </div>
        </div>
    </div>
</div>
