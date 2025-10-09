<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ViewRecurringSequence" class="overflow-auto" style="height: 650px">
  <c:forEach var="recList" items="${sessionScope.recTaskList}">
    <div class="row mb-1 mt-0">
      <div class="col">
        <div class="input-group input-group-sm">
          <c:choose>
            <c:when test="${sessionScope.currentRecList.getId()==recList.getId()}">
              <button type="button" class="btn btn-danger" disabled name="recListSelection" id="btnRecList${recList.getId()}" value="${recList.getId()}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </button>
              <c:set var="currentStyle" value="form-control text-danger fw-bold"></c:set>
            </c:when>
            <c:otherwise>
              <button type="submit" class="btn btn-secondary" name="recListSelection" id="btnRecList${recList.getId()}" value="${recList.getId()}">
                View
              </button>
              <c:set var="currentStyle" value="form-control"></c:set>
            </c:otherwise>
          </c:choose>
          <div class="${currentStyle}">
              ${recList.getDescription()}
          </div>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
