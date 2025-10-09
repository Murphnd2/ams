<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row mb-1">
    <div class="col">
        <div class="input-group">
            <span class="input-group-text">Created By</span>
            <div class="form-control">${sessionScope.currentActivity.getLoggedBy().getFullName()}</div>
        </div>
    </div>
    <div class="col">
        <div class="input-group">
            <span class="input-group-text">Date Created</span>
            <div class="form-control">${sessionScope.currentActivity.getDateCreated()}</div>
        </div>
    </div>
</div>
<div class="row mb-1">
    <div class="col">
        <div class="input-group">
            <span class="input-group-text">Assigned To</span>
            <div class="form-control">${sessionScope.currentActivity.getAssignedTo().getFullName()}</div>
        </div>
    </div>
    <div class="col">
        <div class="input-group">
            <span class="input-group-text">Date Due</span>
            <div class="form-control">${sessionScope.currentActivity.getDueDate()}</div>
        </div>
    </div>
</div>
