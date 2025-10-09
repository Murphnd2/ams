<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ChangeActivityOwner" class="mb-1">
    <div class="accordion" id="changeDueDateForm">
        <div class="accordion-item">
            <div class="accordion-header" id="changeOwnerHeader">
                <div class="input-group">
                    <div class="form-control text-warning fw-bolder bg-dark">
                        ASSIGNED TO ${sessionScope.currentActivity.getAssignedTo().getFullName().toUpperCase()}
                    </div>
                    <button type="button" data-bs-toggle="collapse" data-bs-target="#changeOwnerBody" class="btn btn-outline-warning bg-dark text-warning">
                        <i class="bi bi-caret-down-square-fill"></i>
                    </button>
                </div>
            </div>
            <div class="accordion-collapse collapse" id="changeOwnerBody">
                <div class="accordion-body p-0 m-0 mt-1 mb-3">
                    <div class="row">
                        <div class="col-auto m-0 mt-1 pe-0">
                            <div class="btn btn-sm btn-dark rounded-end-0 me-0 pe-none">
                                Change Owner To
                            </div>
                        </div>
                        <div class="col m-0 mt-1 ps-0 pe-0">
                            <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddUsers.jsp"></c:import>
                        </div>
                        <div class="col-auto m-0 mt-1 ps-0">
                            <button type="submit" class="btn btn-sm btn-outline-dark rounded-start-0">
                                <i class="bi bi-nintendo-switch"></i> <span class="d-none d-md-inline">Change</span>
                            </button>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    </div>
</form>

