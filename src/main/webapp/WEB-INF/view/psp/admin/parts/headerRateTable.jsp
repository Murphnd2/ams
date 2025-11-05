<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row mb-3">
  <div class="btn-group" role="group" aria-label="Rate Table Choices">
    <c:choose>
      <c:when test="${sessionScope.rateLock}">
          <a href="UnlockRates" class="btn btn-success">Edit Pricing</a>
      </c:when>
      <c:otherwise>
        <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#buildRateTableModal">Add Rate Item</button>
        <a href="ClearRates" class="btn btn-outline-secondary">Erase All Rates</a>
        <a href="AddStandardRates" class="btn btn-outline-secondary">Reset Rates</a>
        <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#">Copy Rate Table</button>
      </c:otherwise>
    </c:choose>
  </div>
</div>