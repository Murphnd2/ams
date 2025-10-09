<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<script type="text/javascript">
  function showHide(){
    let reasonList = document.getElementById('ticketSubCategoryList');
    let submitButton = document.getElementById('sb100');
    let reasonEntryArea = document.getElementById('enterReasonRow');
    let reasonInput = document.getElementById('rnT');

    if(reasonList.value ==="S"){
      submitButton.disabled = true;
    } else {
      submitButton.disabled = false;
    }
    if(reasonList.value ==="0"){
      reasonEntryArea.style.display = 'flex';
      reasonInput.required = true;
    } else {
      reasonEntryArea.style.display = 'none';
      reasonInput.required = false;
    }

  }
  function getName(){
    let personInput = document.getElementById('employeeList');


  }
</script>

<form method="post" action="CreateTicket">
  <div class="row align-items-baseline">
    <div class="col-12 col-md-5 col-lg-4 col-xl-3 ">
      <div class="form-label text-info fw-bold w-100">
        <i class="bi bi-envelope"></i> How Contacted
      </div>
    </div>
    <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-0">
        <c:import url="/WEB-INF/view/activity/ticket/components/ddContactMethodNew.jsp"></c:import>
    </div>
    <div class="col-12 col-md-5 col-lg-4 col-xl-3 mt-3">
      <div class="form-label text-info fw-bold w-100">
        <i class="bi bi-person-badge"></i> Select Person
      </div>
    </div>
    <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-3">
      <input class="form-control" list="datalistOptions1" id="employeeList" name="employeeList" autocomplete="off" tabindex="1" required placeholder="Type to search...">
      <datalist id="datalistOptions1">
        <c:forEach var="employee" items="${applicationScope.employeeList}">
          <option class="text-capitalize">
              ${employee.getLastName().toLowerCase()}, ${employee.getFirstName().toLowerCase()} - (${employee.getEr().toLowerCase()}) {[${employee.getId()}]}
          </option>
        </c:forEach>
      </datalist>
    </div>
    <div class="col-12 col-md-5 col-lg-4 col-xl-3 mt-3">
      <div class="form-label text-info fw-bold w-100">
        <i class="bi bi-question-circle"></i> Select Reason
      </div>
    </div>
    <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-3">
      <select class="form-select" oninput="showHide()" aria-label="recurring freq type drop down" name="ticketSubCategoryList" id="ticketSubCategoryList" tabindex="2">
        <option value="S" selected>*** SELECT A REASON FOR TICKET ***</option>
        <option value="0" class="text-secondary fw-light">--- ENTER MY OWN ---</option>
        <c:forEach var="cat" items="${sessionScope.ticketReasonList}">
          <option value="${cat.getId()}">
              ${cat.getNoteCategory().getShortText()} - ${cat.getDescription()}
          </option>
        </c:forEach>
      </select>
    </div>
  </div>
  <div class="row align-items-baseline" id="enterReasonRow" style="display: none ;">
    <div class="col-12 col-md-5 col-lg-4 col-xl-3 mt-3">
      <div class="form-label text-info fw-bold w-100">
        <i class="bi bi-keyboard"></i> Reason for Ticket
      </div>
    </div>
    <div class="col-12 col-md-7 col-lg-8 col-xl-9 mt-1 mt-md-3">
      <input type="text" class="form-control" name="reasonNameTicket" id="rnT" tabindex="3">
    </div>
  </div>
  <div class="row mt-3">
    <div class="col">
      <textarea type="text" required class="form-control" name="ticketDescription" id="ticketDescription" rows="4" placeholder="Describe issue here" tabindex="4"></textarea>
    </div>
  </div>
  <button type="submit" class="btn btn-outline-info w-100 mt-3" id="sb100" disabled tabindex="5">
    <i class="bi bi-ticket-detailed"></i> Create Ticket</button>
</form>
