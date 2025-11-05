<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1">
  <div class="col">
    <div class="input-group input-group-sm mb-3">
      <label class="input-group-text" for="serviceModuleList">Module</label>
      <select class="form-select" aria-label="serviceModuleList type drop down" name="serviceModuleList" id="serviceModuleList">
        <c:forEach var="serviceModuleList" items="${sessionScope.serviceModuleList}">
          <c:choose>
            <c:when test="${sessionScope.currentModule.getId()==serviceModuleList.getId()}">
              <option class="text-truncate" value="${serviceModuleList.getId()}" selected>(${serviceModuleList.getSortOrder()}) ${serviceModuleList.getDescription()}</option>
            </c:when>
            <c:otherwise>
              <option class="text-truncate" value="${serviceModuleList.getId()}">(${serviceModuleList.getSortOrder()}) ${serviceModuleList.getDescription()}</option>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </select>
      <button class="btn btn-outline-secondary" type="button" data-bs-toggle="modal" data-bs-target="#addNewServiceModule" name="btnAddServiceModule">Add</button>
    </div>
  </div>
</div>
