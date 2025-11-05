<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="createTicketNew">
  <div class="row mb-2">
    <div class="col">
      <div class="input-group input-group-sm">
        <c:import url="/WEB-INF/view/activity/ticket/components/ddContactMethod.jsp"></c:import>
      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col">
      <div class="input-group input-group-sm fw-bold">
        <span class="input-group-text" style="width:20%">
          <i class="bi bi-person-badge"></i>&nbsp;&nbsp;Select Employee
        </span>
        <c:import url="/WEB-INF/view/activity/ticket/components/ddEmployeeList.jsp"></c:import>
      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col">
      <div class="input-group input-group-sm">
        <span class="input-group-text" style="width:20%">
          <i class="bi bi-question-lg"></i>&nbsp;&nbsp;Select Reason
        </span>
        <c:import url="/WEB-INF/view/activity/ticket/components/ddTicketTypes.jsp"></c:import>
      </div>
    </div>
  </div>
  <div class="row mb-2">
    <div class="col">
        <textarea type="text" class="form-control" name="ticketDescription" id="ticketDescription" rows="4" placeholder="Describe issue here"></textarea>
    </div>
  </div>
  <button type="submit" class="btn btn-outline-info w-100">
    <i class="bi bi-ticket-detailed"></i> Create Ticket</button>
</form>
