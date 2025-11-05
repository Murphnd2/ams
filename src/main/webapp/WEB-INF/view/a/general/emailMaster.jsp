<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:set var="reqd1" value="${sessionScope.getRequired}"> </c:set>
  <c:import url="/WEB-INF/view/css-js.jsp"> </c:import>
  <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
  <script src="https://cdn.ckeditor.com/ckeditor5/36.0.1/classic/ckeditor.js"></script>
  <style>
    .ck-editor__editable_inline {
      max-width: none;
      width: 100%;
      margin: 0 auto;
      box-sizing: border-box;
      height:300px;
    }

  </style>
  <title>Send Email</title>
  <c:choose>
    <c:when test="${sessionScope.personNotFound==true}">
      <script>
        $(document).ready(function(){
          $("#addRecipientModal").modal('show');
        });
      </script>
    </c:when>
    <c:otherwise>
    </c:otherwise>
  </c:choose>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <div class="row mt-3">
    <div class="col offset-xl-1">
      <form method="post" id="emForm" action="emailActionsNew" enctype="multipart/form-data">
        <c:if test="${sessionScope.sVar.getCurrentActivity()!=null}">
          <c:choose>
            <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"Setup\")}">
              <c:set var="bCol" value="bg-secondary text-white"></c:set>
              <c:set var="bClass" value="building"></c:set>
            </c:when>
            <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"Renewal\")}">
              <c:set var="bCol" value="bg-primary text-white"></c:set>
              <c:set var="bClass" value="repeat"></c:set>
            </c:when>
            <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"Ticket\")}">
              <c:set var="bCol" value="bg-info"></c:set>
              <c:set var="bClass" value="ticket-detailed"></c:set>
            </c:when>
            <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"CheckList\")}">
              <c:set var="bCol" value="bg-warning"></c:set>
              <c:set var="bClass" value="check"></c:set>
            </c:when>
          </c:choose>
          <div class="row mb-2">
            <div class="input-group w-100">
              <div class="form-control fs-3 fw-bold ${bCol}">
                <i class="bi bi-${bClass}"></i>
                  ${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName()} for ${sessionScope.sVar.getCurrentActivity().getFullName()}
              </div>
            </div>
          </div>
        </c:if>
        <%-- ************************ TO WHO LIST ********************************* --%>
        <div class="row mb-2">
          <div class="input-group">
            <span class="input-group-text me-0"><i class="bi bi-people-fill"></i>&nbsp;&nbsp;Recipient(s)&nbsp;&nbsp;&nbsp;</span>
            <div class="form-control m-0 p-0">
              <div class="d-flex row m-1 p-0">
                <c:import url="/WEB-INF/view/general/email/lists/toWhoList.jsp"></c:import>
              </div>
            </div>
            <button type="button" name="btnAddRecipient" data-bs-target="#addRecipientModal" data-bs-toggle="modal" class="btn btn-primary btn-sm">
              <i class="bi bi-person-plus-fill"></i> Add Recipient&nbsp;&nbsp;&nbsp;&nbsp;
            </button>
          </div>
        </div>
        <%-- ************************ SUBJECT LINE ********************************* --%>
        <div class="row mb-2">
          <div class="input-group">
            <span class="input-group-text me-0"><i class="bi bi-lightbulb"></i>&nbsp;&nbsp;Subject&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
            <input type="text" class="form-control form-control" name="eSubject" placeholder="Subject of message . . ." value="${sessionScope.currentEmailSubject}">
          </div>
        </div>
        <%-- ************************ ATTACHMENT LIST ********************************* --%>
        <div class="row mb-2">
          <div class="input-group">
            <span class="input-group-text me-0"><i class="bi bi-files"></i>&nbsp;&nbsp;Attachment(s)</span>
            <div class="form-control m-0 p-0">
              <div class="d-flex row m-1 p-0">
                <c:import url="/WEB-INF/view/general/email/lists/attachmentList.jsp"></c:import>
              </div>
            </div>
            <button type="button" name="btnAddAttachment" data-bs-target="#addAttachmentModal" data-bs-toggle="modal" class="btn btn-secondary btn-sm">
              <i class="bi bi-paperclip"></i> Add Attachment
            </button>
          </div>
        </div>
        <%-- ************************ MESSAGE BODY ********************************* --%>
        <div class="row mb-2">
          <div class="col">
            <textarea name="messageBody" id="mesBod" class="form-control" rows="6" placeholder="Enter message here......">${sessionScope.messageBody}</textarea>
            <script>
              document.addEventListener('DOMContentLoaded', function () {
                ClassicEditor
                        .create(document.querySelector('#mesBod'), {
                          toolbar: {
                            shouldNotGroupWhenFull: true  // Prevent grouping when full
                          }
                        })
                        .then(editor => {
                          // When the form is submitted
                          document.querySelector('#emForm').addEventListener('submit', function (event) {
                            // Update the textarea's value with the editor's data
                            editor.getData().then(data => {
                              document.querySelector('#mesBod').value = data;
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
        <%-- ************************ SEND BUTTON ********************************* --%>
        <div class="row">
          <div class="col"></div>
          <div class="col-auto">
            <c:choose>
              <c:when test="${sessionScope.sVar.getCurrentActivity()==null}">
                <a class="btn btn-outline-secondary" href="goPspHome">Cancel</a>
              </c:when>
              <c:otherwise>
                <a class="btn btn-outline-secondary" href="goActivityDetail">Cancel</a>
              </c:otherwise>
            </c:choose>
          </div>
          <div class="col-auto">
            <button type="submit" name="btnSubmit" value="SE" class="btn btn-success">
              <i class="bi bi-send"></i> Send Message
            </button>
          </div>
        </div>
        <c:import url="/WEB-INF/view/general/email/modals/addRecipientModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/general/email/modals/addAttachmentModal.jsp"></c:import>
      </form>
    </div>
    <div class="col-xl-1"></div>
  </div>
</div>
