<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col">
        <span class="text-danger fw-bolder fs-5">Add Notes To ${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()}</span>
      </div>
      <div class="col-auto"></div>
    </div>
    <form method="post" action="AddNoteToActivity25" id="myForm" >
      <div class="row mb-0 pb-0">
        <div class="col mb-0 pb-0">
          <textarea class="form-control rounded-0 rounded-top" name="noteText" id="editor" rows="6" type="text" placeholder="Add text here for the note"></textarea>
          <script>
            document.addEventListener('DOMContentLoaded', function () {
              ClassicEditor
                      .create(document.querySelector('#editor'), {
                        toolbar: {
                          items: ['bold', 'italic', 'link', '|','bulletedList','numberedList', '|', 'undo','redo','code'],
                          shouldNotGroupWhenFull: true  // Prevent grouping when full
                        }
                      })
                      .then(editor => {
                        // When the form is submitted
                        document.querySelector('#myForm').addEventListener('submit', function (event) {
                          // Update the textarea's value with the editor's data
                          editor.getData().then(data => {
                            document.querySelector('#editor').value = data;
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
      <div class="row mt-1">
        <div class="col mt-0 pt-0">
          <div class="input-group input-group-sm rounded-0 d rounded-bottom">
            <span class="input-group-text d-none d-sm-inline d-md-none d-lg-inline">Reason</span>
            <c:import url="/WEB-INF/view/a/general/globalDropDowns/ddReasons25.jsp"> </c:import>
            <span class="input-group-text d-none d-sm-inline d-md-none d-lg-inline">Status</span>
            <c:import url="/WEB-INF/view/a/general/globalDropDowns/ddNoteStatus25.jsp"> </c:import>
            <button type="submit" name="btnAddNote1" value="Save" class="btn btn-danger">
              <i class="bi bi-journal-plus"></i> Save
            </button>
          </div>
        </div>
      </div>
    </form>

  </div>
</div>
