<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="SequenceDetailView">
  <c:set var="lastId" value="0"></c:set>
  <c:forEach var="sequence" items="${sessionScope.PspTaskSequenceList}">
    <c:if test="${sequence.getId()!=lastId}">
      <div class="row mb-1 mt-0">
        <div class="col">
          <div class="input-group input-group-sm">
            <c:choose>
              <c:when test="${sessionScope.currentTaskSequence.getId()==sequence.getId()}">
                <button type="button" class="btn btn-danger" disabled name="sequenceSelectButton" id="btnSequence${sequence.getId()}" value="${sequence.getId()}">
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                </button>
                <c:set var="currentStyle" value="form-control text-danger fw-bold"></c:set>
              </c:when>
              <c:otherwise>
                <button type="submit" class="btn btn-secondary" name="sequenceSelectButton" id="btnSequence${sequence.getId()}" value="${sequence.getId()}">
                  View
                </button>
                <c:set var="currentStyle" value="form-control"></c:set>
              </c:otherwise>
            </c:choose>
            <div class="${currentStyle}">
              ${sequence.getDescription()}
            </div>
          </div>
        </div>
      </div>
    </c:if>
    <c:set var="lastId" value="${sequence.getId()}"></c:set>
  </c:forEach>
</form>
