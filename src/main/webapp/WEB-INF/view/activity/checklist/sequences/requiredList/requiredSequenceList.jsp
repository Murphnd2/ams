<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ViewRequiredSequence" class="overflow-auto" style="height:650px">
  <c:forEach var="reqList" items="${sessionScope.reqTaskList}">
    <div class="row mb-1 mt-0">
      <div class="col">
        <div class="input-group input-group-sm">
          <c:choose>
            <c:when test="${sessionScope.currentReqList.getId()==reqList.getId()}">
              <button type="button" class="btn btn-danger" disabled name="reqListSelection" id="btnReqList${reqList.getId()}" value="${reqList.getId()}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </button>
              <c:set var="currentStyle" value="form-control text-danger fw-bold"></c:set>
            </c:when>
            <c:otherwise>
              <button type="submit" class="btn btn-secondary" name="reqListSelection" id="recListSelection${reqList.getId()}" value="${reqList.getId()}">
                View
              </button>
              <c:set var="currentStyle" value="form-control"></c:set>
            </c:otherwise>
          </c:choose>
          <div class="${currentStyle}">
              ${reqList.getDescription()}
          </div>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
