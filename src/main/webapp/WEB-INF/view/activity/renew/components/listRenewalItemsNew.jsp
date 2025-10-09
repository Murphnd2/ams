<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col-auto me-0 pe-0">
        <span class="text-primary fw-bolder fs-5">Benefits In Renewal</span>
      </div>
      <div class="col ms-0 ps-0">
        <button type="button" class="btn btn-sm"   data-bs-target="#addRenewalItem" data-bs-toggle="modal">[add]</button>
      </div>
      <div class="col-auto"></div>
    </div>
    <c:forEach var="ri" items="${sessionScope.currentRenewal.getRenewalItemList()}">
      <div class="row">
        <div class="col-auto">
          <i class="bi bi-arrow-right"></i>
        </div>
        <div class="col">
          <span class="text-uppercase fw-bolder">
              ${ri.getBenefit().getPlanDescription()}
          </span>
        </div>
        <div class="col-auto">
          <span class="text-lowercase text-secondary">
            <fmt:formatDate value="${ri.getDateFor()}" pattern="MMM yy"></fmt:formatDate>
          </span>
        </div>
        <div class="col-auto">
          <form method="post" action="RemoveItemFromRenewal">
            <button type="submit" class="btn btn-sm text-danger p-0"  id="ri${ri.getId()}" name="btnRemoveItem" value="${ri.getId()}">
              <i class="bi bi-trash"></i>
            </button>
          </form>
        </div>
      </div>
    </c:forEach>
  </div>
</div>
