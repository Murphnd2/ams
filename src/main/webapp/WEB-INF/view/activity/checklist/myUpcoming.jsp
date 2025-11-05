<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="InitiateChecklist">
  <c:forEach var="recList" items="${sessionScope.myUpList}">
    <div class="row mb-1">
      <div class="input-group input-group-sm">
        <input class="form-control" readonly value="${recList.getRecurringTaskList().getDescription()}">
        <input class="form-control" readonly value="${recList.getNextDue()}">
        <input class="form-control" readonly value="${recList.getNextBegin()}">
        <button class="btn btn-danger" type="submit" name="btnGenerate" id="${recList.getRecurringTaskList().getId()}" value="${recList.getRecurringTaskList().getId()}" >Add</button>
      </div>
    </div>
  </c:forEach>
</form>
