<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1">
  <div class="col">
    <div class="input-group input-group-sm mb-3">
      <label for="losList" class="input-group-text">Line of Service</label>
      <select class="form-select" aria-label="los drop down" name="losList" id="losList">
        <c:forEach var="losList" items="${sessionScope.losList}">
          <c:choose>
            <c:when test="${sessionScope.currentLOS.getId()==losList.getId()}">
              <option value="${losList.getId()}" selected>${losList.getDescription()}</option>
            </c:when>
            <c:otherwise>
              <option value="${losList.getId()}">${losList.getDescription()}</option>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </select>
    </div>
  </div>
</div>
