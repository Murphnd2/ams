<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row mb-3 mt-2">
    <div class="col border rounded border-dark pt-2 pb-2 ms-2 me-2" style="background: black;min-height: 100px">
        <div class="row">
            <div class="col">
                <h5 class="fw-bold overflow-hidden text-white">
                    ${sessionScope.currentTicket.getTicketSubCategory().getDescription().toUpperCase()}
                </h5>
            </div>
        </div>
        <div class="row">
            <div class="col">
                <label class="form-label fst-italic text-light">
                    ${sessionScope.currentTicket.getDescription().trim()}
                </label>
            </div>
        </div>
    </div>
</div>

<div class="row mb-0">
  <div class="col">
      <h5 class="text-success fw-bold">
          LOGGED BY ${sessionScope.currentTicket.getLoggedBy().getFullNameFirstLast().toUpperCase()}
      </h5>
  </div>
</div>
<div class="row">
  <div class="col text-success">
    <fmt:formatDate value="${sessionScope.currentTicket.getDateCreated()}" pattern="MMMM dd, yyyy @ hh:mm aa"></fmt:formatDate>
  </div>
</div>
