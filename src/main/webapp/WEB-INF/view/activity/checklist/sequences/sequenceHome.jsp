<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Home</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
    <div class="row">
        <%-- ******* L E F T    C O L U M N   **************** --%>
        <div class="col-lg-3">
            <h3>Required Sequences</h3>
            <c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/requiredSequenceList.jsp"></c:import>
            <button type="button" class="btn btn-danger w-100" data-bs-toggle="modal" data-bs-target="#AddReqSequence">Add Required</button>

        </div>
        <%-- ******* C E N T E R    C O L U M N   **************** --%>
        <div class="col">
            <c:choose>
                <c:when test="${sessionScope.sequenceView == 1}">
                    <h3>Required Task List Builder</h3>
                    <h5>${sessionScope.currentReqList.getDescription()}</h5>
                    <c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/components/ddAvailableTasksForm.jsp"></c:import>
                    <c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/listTasksAssigned.jsp"></c:import>
                </c:when>
                <c:when test="${sessionScope.sequenceView == 2}">
                    <h3>Recurring Task List Builder</h3>
                    <h5>${sessionScope.currentRecList.getDescription()}</h5>
                    <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/components/ddAvailableTasksRecForm.jsp"></c:import>
                    <c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/listTasksAssigned.jsp"></c:import>
                </c:when>
                <c:otherwise>
                    <h3>Sequence Manager</h3><button type="button" class="btn btn-outline-success" data-bs-toggle="modal" data-bs-target="#createSubCatModal">Add Tick</button>
                    <c:import url="/WEB-INF/view/activity/ticket/addAutoCategoryMod.jsp"></c:import>
                    <c:import url="/WEB-INF/view/activity/checklist/myUpcoming.jsp"></c:import>
                </c:otherwise>
            </c:choose>

        </div>
        <%-- ******* R I G H T    C O L U M N   **************** --%>
        <div class="col-lg-3">
            <h3>Recurring Sequences</h3>
            <c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/recurringSequenceList.jsp"></c:import>
            <button type="button" class="btn btn-danger w-100" data-bs-toggle="modal" data-bs-target="#AddRecSequence">Add Recurring</button>
        </div>
    </div>
</div>
<%-- ******* M O D A L S  A R E   B E L O W   **************** --%>
<c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/addRequiredSequenceModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/checklist/sequences/recurringList/addRecurringSequenceModal.jsp"></c:import>
</body>
</html>
