<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ModifyTaskSequence">

  <c:forEach var="taskSequenceTable" items="${sessionScope.currentSequenceListOfAssignedTasks}">
      <div class="row mb-1 mt-0">
        <div class="col">
          <div class="input-group input-group-sm">
            <button type="submit" class="btn btn-danger col-2" name="modifyTaskSequenceButton" id="btnTaskSeq${taskSequenceTable.getTaskSequenceID()}-1" value="${taskSequenceTable.getCode()}-1">
              Remove
            </button>
            <span class="input-group-text col-7">${taskSequenceTable.getTask().getDescription()}</span>
            <input type="text" name="sortOrder${taskSequenceTable.getCode()}" class="form-control col-2" id="" value="${taskSequenceTable.getSortOrder()}">
            <button type="submit" class="btn btn-outline-secondary col-2" name="modifyTaskSequenceButton" id="btnTaskSeq${taskSequenceTable.getTaskSequenceID()}-0" value="${taskSequenceTable.getCode()}-0">
              Update
            </button>
          </div>
        </div>
      </div>
  </c:forEach>
</form>
