<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-1">
  <div class="col">
    <div class="input-group input-group-sm mb-3">
      <label class="input-group-text" for="priceItemList">Item&nbsp;&nbsp;</label>
      <select class="form-select" aria-label="price item drop down" name="priceItemList" id="priceItemList">
        <c:forEach var="priceItemList" items="${sessionScope.priceItemList}">
          <c:choose>
            <c:when test="${sessionScope.currentPriceItem.getId()==priceItemList.getId()}">
              <option class="text-truncate" value="${priceItemList.getId()}" selected>(${priceItemList.getSortOrder()}) ${priceItemList.getDescription()}</option>
            </c:when>
            <c:otherwise>
              <option class="text-truncate" value="${priceItemList.getId()}">(${priceItemList.getSortOrder()}) ${priceItemList.getDescription()}</option>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </select>
      <button class="btn btn-outline-secondary" type="button" data-bs-toggle="modal" data-bs-target="#addNewPriceItem" id="btnAddPriceItem">Add</button>
    </div>
  </div>
</div>
