<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1">
  <div class="col">
    <div class="input-group input-group-sm mb-3">
      <label for="serviceItemList" class="input-group-text">Service Item</label>
      <select class="form-select" aria-label="service item drop down" name="serviceItemList" id="serviceItemList">
        <c:forEach var="serviceItem" items="${sessionScope.serviceItemList}">
          <option class="text-truncate" value="${serviceItem.getId()}">${serviceItem.getBulletPoint()}</option>
        </c:forEach>
      </select>
      <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#addNewServiceItem" >Add</button>
    </div>
  </div>
</div>
