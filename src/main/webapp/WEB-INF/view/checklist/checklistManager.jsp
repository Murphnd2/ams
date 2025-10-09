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
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <div class="row">
    <%-- ******* L E F T    C O L U M N   **************** --%>
    <div class="col-lg-3">
      <h3>Task List</h3>
      <c:import url="/WEB-INF/view/checklist/task/taskList.jsp"></c:import>
      <button type="button" class="btn btn-danger w-100" data-bs-toggle="modal" data-bs-target="#addTaskModal">Add Task</button>

    </div>
    <%-- ******* C E N T E R    C O L U M N   **************** --%>
    <div class="col">
      <c:choose>
        <c:when test="${sessionScope.checkView == 1}">
          <h3>Task Builder View</h3>
          <c:import url="/WEB-INF/view/checklist/task/taskView.jsp"></c:import>
          <div class="row mb-3">
            <div class="col">&nbsp;</div>
            <div class="col-lg-3">
              <button type="button" class="btn btn-primary w-100" data-bs-toggle="modal" data-bs-target="#addFileModal">Attach a File</button>
            </div>
            <div class="col-lg-3">
              <button type="button" class="btn btn-warning w-100" data-bs-toggle="modal" data-bs-target="#addWeblinkModal">Add a WebLink</button>
            </div>
          </div>
          <c:import url="/WEB-INF/view/checklist/task/taskLinkView.jsp"></c:import>
        </c:when>
        <c:when test="${sessionScope.checkView == 2}">
          <h3>Sequence Builder View</h3>
          <c:import url="/WEB-INF/view/checklist/sequence/sequenceView.jsp"></c:import>
          <c:import url="/WEB-INF/view/checklist/sequence/assignTaskForm.jsp"></c:import>
          <c:import url="/WEB-INF/view/checklist/task/taskListForSequence.jsp"></c:import>
        </c:when>
        <c:otherwise>
          <h3>Default View</h3>
        </c:otherwise>
      </c:choose>

    </div>
    <%-- ******* R I G H T    C O L U M N   **************** --%>
    <div class="col-lg-3">
      <h3>Sequence List</h3>

      <c:import url="/WEB-INF/view/checklist/sequence/sequenceGroupFilter.jsp"></c:import>
      <c:import url="/WEB-INF/view/checklist/sequence/sequenceList.jsp"></c:import>
      <button type="button" class="btn btn-danger w-100" data-bs-toggle="modal" data-bs-target="#addSequenceModal">Add Sequence</button>
    </div>
  </div>
</div>
<%-- ******* M O D A L S  A R E   B E L O W   **************** --%>
<c:import url="/WEB-INF/view/activity/checklist/task/components/addTaskModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/checklist/task/components/addWebLinkToTaskModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/checklist/task/components/addFileToTaskModal.jsp"></c:import>
<c:import url="/WEB-INF/view/checklist/sequence/addSequenceModal.jsp"></c:import>
</body>
</html>
