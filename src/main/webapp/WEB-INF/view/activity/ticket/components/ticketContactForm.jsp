<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="AddNoteToActivity">
  <c:if test="${sessionScope.currentActivity.getClass().getSimpleName().equals(\"Ticket\")}">
    <c:if test="${sessionScope.currentTicket.getContact().getEmployee().getId()!=null && sessionScope.currentTicket.getContact().getEmployee().getId()>0}">
      <div class="row mb-1">
        <div class="col d-none d-md-block">
          <div class="input-group">
            <button type="button" class="btn btn-primary pe-none">
              <i class="bi bi-building"></i>
              Employer</button>
            <input type="text" class="form-control align-items-center border-primary" name="ticketEmployer" value="${sessionScope.currentTicket.getContact().getEmployee().getEmployer().getEmployerName()}  [${sessionScope.currentErId}][${sessionScope.currentErAltId}]">

          </div>
        </div>
        <div class="col d-grid d-md-none">
          <div class="input-group input-group-sm">
            <button type="button" class="btn btn-primary pe-none">
              <i class="bi bi-building"></i>
              Employer&nbsp;</button>
            <input type="text" class="form-control align-items-center border-primary" name="ticketEmployer" value="${sessionScope.currentTicket.getContact().getEmployee().getEmployer().getEmployerName()}">
          </div>
        </div>
      </div>
    </c:if>
  <div class="row mb-1">
    <div class="col d-none d-md-block">
      <div class="input-group">
        <button type="button" class="btn btn-primary pe-none">
          <i class="bi bi-send"></i>
          Email&nbsp;</button>
        <input type="email" class="form-control align-items-center border-primary" name="ticketEmail" value="${sessionScope.currentTicket.getContact().getEmail()}">
        <button type="submit" name="btnAddNote1" value="UpdateEmail" class="btn btn-outline-primary">Update</button>
      </div>
    </div>
    <div class="col d-grid d-md-none">
      <div class="input-group input-group-sm">
        <button type="button" class="btn btn-primary pe-none">
          <i class="bi bi-send"></i>
          Email&nbsp;</button>
        <input type="email" class="form-control align-items-center border-primary" name="ticketEmail" value="${sessionScope.currentTicket.getContact().getEmail()}">
        <button type="submit" name="btnAddNote1" value="UpdateEmail" class="btn btn-outline-primary">Update</button>
      </div>
    </div>
  </div>
    <div class="row mb-1">
      <div class="col d-none d-md-block">
        <div class="input-group">
          <button type="button" class="btn btn-secondary pe-none">
            <i class="bi bi-phone"></i>
            Phone</button>
          <input type="tel" class="col btn btn-outline-secondary pe-none text-dark" placeholder="No Phone Number Data" name="ticketPhone" value="${sessionScope.currentTicket.getContact().getPhone()}">
        </div>
      </div>
      <div class="col d-grid d-md-none">
        <div class="input-group input-group-sm">
          <button type="button" class="btn btn-secondary pe-none">
            <i class="bi bi-phone"></i>
            Phone</button>
          <input type="tel" class="col btn btn-outline-secondary pe-none text-dark" placeholder="No Phone Number Data" name="ticketPhone" value="${sessionScope.currentTicket.getContact().getPhone()}">
        </div>
      </div>
    </div>
  </c:if>
</form>
