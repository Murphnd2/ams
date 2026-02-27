<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1">
  <div class="col">
    <div class="input-group input-group-sm mb-3">
      <label for="moduleDetailList" class="input-group-text">Service Item</label>
      <select class="form-select" aria-label="service item drop down" name="moduleDetailList" id="moduleDetailList">
        <c:forEach var="moduleDetail" items="${sessionScope.moduleDetailList}">
          <option class="text-truncate" value="${moduleDetail.getId()}">${moduleDetail.getBulletPoint()}</option>
        </c:forEach>
      </select>
      <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#addNewServiceItem" >Add</button>
    </div>
  </div>
</div>
