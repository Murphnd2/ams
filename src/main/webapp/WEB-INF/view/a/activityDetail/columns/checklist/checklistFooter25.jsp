<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="allDone" value=""></c:set>
<c:if test="${sessionScope.local.getCurrentActivity().getToDoList().size()>0 && sessionScope.local.getCurrentActivity().getToDoList().get(0).isComplete()==false}">
    <c:set var="allDone" value="disabled"></c:set>
</c:if>
<%--
<c:if test="${sessionScope.isPspAdmin==true}">
    <c:set var="allDone" value=""></c:set>
</c:if>--%>
<form method="post" action="CloseActivity25">
    <div class="row mt-2">
        <div class="col-6">
            <button class="btn btn-sm btn-success w-100" type="button" data-bs-toggle="modal" data-bs-target="#addToDoModal">
                <i class="bi bi-card-checklist"></i> Add Task
            </button>
        </div>
        <div class="col-6">
            <c:choose>
                <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Setup\")}">
                    <button type="button" class="btn btn-sm btn-secondary w-100" ${allDone} name="btnCheckList" data-bs-target="#closeActivity" data-bs-toggle="modal" ${allDone}>
                        <i class="bi bi-door-open"></i> Close Setup
                    </button>
                </c:when>
                <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Renewal\")}">
                    <button type="button" class="btn btn-sm btn-primary w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal" name="btnCheckList" ${allDone}>
                        <i class="bi bi-door-open"></i> Close Renewal
                    </button>
                </c:when>
                <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Ticket\")}">
                    <button type="button" class="btn btn-sm btn-info w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal" name="btnCheckList" ${allDone}>
                        <i class="bi bi-door-open"></i> Close Ticket
                    </button>
                </c:when>
                <c:otherwise>
                    <button type="button" class="btn btn-sm btn-warning w-100" ${allDone} data-bs-target="#closeActivity" data-bs-toggle="modal" name="btnCheckList" ${allDone}>
                        <i class="bi bi-door-open"></i> Close Checklist
                    </button>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/modals/closeActivityModal.jsp"></c:import>
</form>
<c:import url="/WEB-INF/view/a/checklistDetail/addToDo25.jsp"></c:import>
