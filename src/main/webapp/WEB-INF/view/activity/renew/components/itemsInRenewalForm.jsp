<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="RemoveItemFromRenewal" class="mt-0 pt-2 mb-2 form-control" >
  <c:forEach var="ri" items="${sessionScope.currentRenewal.getRenewalItemList()}">
    <div class="row mb-1">
      <div class="col-auto me-0 pe-0">
        <c:choose>
          <c:when test="${sessionScope.currentRenewal.getRenewalItemList().size()==1}">
            <button type="button" class="btn btn-outline-secondary border-white border-0 m-0 p-0" data-bs-target="#removeLastRenewalItemModal" data-bs-toggle="modal">
              <i class="bi bi-journal-minus" style="font-size: 1.4rem"></i>
            </button>
          </c:when>
          <c:otherwise>
            <button type="submit" class="btn btn-outline-secondary border-white border-0 m-0 p-0"  id="ri${ri.getId()}" name="btnRemoveItem" value="${ri.getId()}">
                <i class="bi bi-journal-minus" style="font-size: 1.4rem"></i>
            </button>
          </c:otherwise>
        </c:choose>
      </div>
      <div class="col ms-0 ps-0">
        <div class="form-control form-control-sm pe-none border-0">
          <div class="row m-0 mt-1 c-0 g-0">
            <div class="col m-0 p-0 g-0 text-secondary" style="font-size:0.9rem">
                  ${ri.getBenefit().getPlanDescription()}
            </div>
            <div class="col-auto m-0 p-0 g-0 text-muted fst-italic">
              <fmt:formatDate value="${ri.getDateFor()}" pattern="MMM yy"></fmt:formatDate>
            </div>
          </div>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
