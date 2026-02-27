<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="UpdateSequenceName" >
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <input type="text" class="form-control text-primary fw-bold" name="sequenceName" required value="${currentTaskSequence.getDescription()}">
        <select class="form-select" aria-label="recurring freq type drop down" name="serviceItemList" id="serviceItemList">
          <c:forEach var="serviceItem" items="${sessionScope.serviceItemList}">
            <c:choose>
              <c:when test="${serviceItem.getId()==currentTaskSequence.getServiceItem().getId()}">
                <option value="${serviceItem.getId()}" selected>(${serviceItem.getActivityCategory().getDescription()}) ${serviceItem.getDescription()}</option>
              </c:when>
              <c:otherwise>
                <option value="${serviceItem.getId()}">(${serviceItem.getActivityCategory().getDescription()}) ${serviceItem.getDescription()}</option>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </select>
        <button type="submit" class="btn btn-primary col-2" name="submitButton" value="0">Update</button>
      </div>
    </div>
  </div>

</form>
