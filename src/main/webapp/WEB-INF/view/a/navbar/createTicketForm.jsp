<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="createTicketModal" data-bs-backdrop="static" data-bs-keyboard="false" role="dialog" tabindex="-1" aria-labelledby="createTicketModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title text-info fw-bold" id="loginLabel">
          <i class="bi bi-ticket-detailed"></i> Create Ticket</h3>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close" tabindex="-1"></button>
      </div>
      <div class="modal-body">
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

        <form method="post" action="createTicket3" id="thisForm01">
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
              <textarea type="text" class="form-control" name="ticketDescription" id="ticketDescription1" rows="4" placeholder="Describe issue here" tabindex="4"></textarea>
              <script>
                document.addEventListener('DOMContentLoaded', function () {
                  ClassicEditor
                          .create(document.querySelector('#ticketDescription1'), {
                            toolbar: {
                              items: ['bold', 'italic', 'link', '|','bulletedList','numberedList', '|', 'undo','redo','code'],
                              shouldNotGroupWhenFull: true  // Prevent grouping when full
                            }
                          })
                          .then(editor => {
                            // When the form is submitted
                            document.querySelector('#thisForm01').addEventListener('submit', function (event) {
                              // Update the textarea's value with the editor's data
                              editor.getData().then(data => {
                                document.querySelector('#ticketDescription1').value = data;
                              }).then(() => {
                                // Allow form submission
                                event.currentTarget.submit(); // Submit the form
                              }).catch(error => {
                                console.error('Error updating textarea value:', error);
                              });

                              // Prevent the default submission until the CKEditor data is set
                              event.preventDefault();
                            });
                          })
                          .catch(error => {
                            console.error(error);
                          });
                });
              </script>
            </div>
          </div>
          <button type="submit" class="btn btn-outline-info w-100 mt-3" id="sb100" disabled tabindex="5">
            <i class="bi bi-ticket-detailed"></i> Create Ticket</button>
        </form>
      </div>
    </div>
  </div>
</div>
