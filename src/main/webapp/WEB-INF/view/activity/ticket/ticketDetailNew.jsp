<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col-auto">
        <span class="text-info text-capitalize fw-bolder fs-5">Issue: ${sessionScope.currentTicket.getTicketSubCategory().getDescription().toLowerCase()}</span>
      </div>
      <div class="col-auto"></div>
    </div>
    <div class="row mb-1">
      <div class="col-auto">
        <i class="bi bi-arrow-right"></i>
      </div>
      <div class="col">
          <span class="text-lowercase text-dark overflow-auto">
            ${sessionScope.currentTicket.getDescription().trim()}
          </span>
      </div>
    </div>
    <div class="row">
      <div class="col"></div>
      <div class="col-auto text-lowercase text-secondary fst-italic fw-light" style="font-size: xx-small">
          LOGGED BY ${sessionScope.currentTicket.getLoggedBy().getFullNameFirstLast().toUpperCase()} ON <fmt:formatDate value="${sessionScope.currentTicket.getDateCreated()}" pattern="MMMM dd, yyyy @ hh:mm aa"></fmt:formatDate>
      </div>
    </div>
  </div>
</div>


