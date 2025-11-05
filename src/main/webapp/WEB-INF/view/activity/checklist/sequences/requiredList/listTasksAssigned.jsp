<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ChangeTaskSequenceTable">
  <c:forEach var="taskSequence" items="${sessionScope.embeddedTaskSequences}">
    <div class="row mb-1">
      <div class="input-group input-group-sm">
        <button class="btn btn-outline-dark" type="submit" name="btnTaskChange" id="UP-${taskSequence.getTask().getId()}"  value="UP-${taskSequence.getTask().getId()}">
          <i class="bi bi-arrow-up"></i>
        </button>
        <button class="btn btn-outline-dark" type="submit" name="btnTaskChange" id="DN-${taskSequence.getTask().getId()}"  value="DN-${taskSequence.getTask().getId()}">
          <i class="bi bi-arrow-down"></i>
        </button>
        <button class="btn btn-outline-dark pe-none">
            ${taskSequence.getSortOrder()}
        </button>
        <input class="form-control" readonly value="${taskSequence.getTask().getDescription()}">
        <button class="btn btn-outline-danger" type="submit" name="btnTaskChange" id="RM-${taskSequence.getTask().getId()}" value="RM-${taskSequence.getTask().getId()}" >
          <i class="bi bi-trash"></i>
        </button>
      </div>
    </div>
  </c:forEach>
</form>