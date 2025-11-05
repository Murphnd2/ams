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
  <title>Send Email</title>

</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <div class="row mt-3">
    <div class="col offset-xl-1">
      <%-- ACTIVITY HEADER (IF APPLICABLE) --%>
        <c:if test="${sessionScope.local.getCurrentActivity()!=null && sessionScope.local.getCurrentActivity().getActivity()!=null}">
          <c:choose>
            <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Setup\")}">
              <c:set var="bCol" value="bg-secondary text-white"></c:set>
              <c:set var="bClass" value="building"></c:set>
            </c:when>
            <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Renewal\")}">
              <c:set var="bCol" value="bg-primary text-white"></c:set>
              <c:set var="bClass" value="repeat"></c:set>
            </c:when>
            <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"Ticket\")}">
              <c:set var="bCol" value="bg-info"></c:set>
              <c:set var="bClass" value="ticket-detailed"></c:set>
            </c:when>
            <c:when test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
              <c:set var="bCol" value="bg-warning"></c:set>
              <c:set var="bClass" value="check"></c:set>
            </c:when>
          </c:choose>
          <div class="row mb-2">
            <div class="input-group w-100">
              <div class="form-control fs-3 fw-bold ${bCol}">
                <i class="bi bi-${bClass}"></i>
                  ${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()} for ${sessionScope.local.getCurrentActivity().getActivity().getFullName()}
              </div>
            </div>
          </div>
        </c:if>
        <form method="post" id="emailForm25" action="SaveEmailState25" enctype="multipart/form-data">
          <div class="row mb-2">
            <div class="col-10 m-0 pe-0 pt-0 pb-0">
              <div class="input-group">
                <span class="input-group-text me-0"><i class="bi bi-people-fill"></i>&nbsp;&nbsp;Recipient(s)&nbsp;&nbsp;&nbsp;</span>
                <div class="form-control m-0 p-0 rounded-end-0">
                  <div class="d-flex row m-1 p-0">
                    <c:if test="${sessionScope.local.getCurrentEmail().getRecipientList().size()==0}">
                      <div class="col-auto">
                        <div class="form-control form-control-sm pe-none">
                          EMPTY
                        </div>
                      </div>
                    </c:if>
                    <c:forEach var="recipient" items="${sessionScope.local.getCurrentEmail().getRecipientList()}">
                      <div class="col-auto">
                        <div class="input-group input-group-sm">
                          <span class="input-group-text">
                            ${recipient.getFirstName()} ${recipient.getLastName()} (${recipient.getEmail()})
                          </span>
                          <button type="submit" class="btn btn-danger" name="action" id="btnDR${recipient.getId()}" value="DR-${recipient.getId()}">X</button>
                        </div>
                      </div>
                    </c:forEach>
                    <div class="col"></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-2 m-0 ps-0 pt-0 pb-0">
              <div class="input-group h-100">
                <button type="button"  name="btnAddRecipient" data-bs-target="#addRecipientModal" data-bs-toggle="modal" class="btn btn-primary w-100 rounded-start-0">
                  <i class="bi bi-person-plus-fill"></i> Add Recipient&nbsp;&nbsp;&nbsp;&nbsp;
                </button>
              </div>
              <c:import url="/WEB-INF/view/a/general/email/addRecipientModal25.jsp"></c:import>
            </div>
          </div>
          <div class="row mb-2">
            <div class="col-10 m-0 pe-0 pt-0 pb-0">
              <div class="input-group">
                <span class="input-group-text me-0"><i class="bi bi-files"></i>&nbsp;&nbsp;Attachment(s)</span>
                <div class="form-control m-0 p-0 rounded-end-0">
                  <div class="d-flex row m-1 p-0">
                    <c:if test="${sessionScope.local.getCurrentEmail().getAttachments().size()==0}">
                      <div class="col-auto">
                        <div class="form-control form-control-sm pe-none">
                          EMPTY
                        </div>
                      </div>
                    </c:if>
                    <c:forEach var="link" items="${sessionScope.local.getCurrentEmail().getAttachments()}">
                      <div class="col-auto">
                        <div class="input-group input-group-sm">
                          <span class="input-group-text">
                            <c:choose>
                              <c:when test="${link.getPlainText()!=null && !link.getPlainText().equals(\"\")}">
                                ${link.getPlainText()}
                              </c:when>
                              <c:otherwise>
                                ${link.getLinkPath()}
                              </c:otherwise>
                            </c:choose>
                          </span>
                          <button type="submit" class="btn btn-danger" name="action" id="btnDA${link.getId()}" value="DA-${link.getId()}">X</button>
                        </div>
                      </div>
                    </c:forEach>
                    <div class="col"></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-2 m-0 ps-0 pt-0 pb-0">
              <div class="input-group h-100">
                <c:choose>
                  <c:when test="${sessionScope.isPspAdmin==true}">
                    <button type="button" name="btnAddAttachment" data-bs-target="#addAttachmentModal" data-bs-toggle="modal" class="btn btn-secondary w-100 rounded-start-0 ">
                      <i class="bi bi-paperclip"></i> Add Attachment
                    </button>
                  </c:when>
                  <c:otherwise>
                    <button type="button" name="btnAddAttachment" disabled data-bs-target="#addAttachmentModal" data-bs-toggle="modal" class="btn btn-secondary w-100 rounded-start-0 ">
                      <i class="bi bi-paperclip"></i> Add Attachment
                    </button>
                  </c:otherwise>
                </c:choose>

              </div>
              <div class="modal fade" id="addAttachmentModal" role="dialog" tabindex="-1" aria-labelledby="addAttachmentModal" aria-hidden="true">
                <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
                  <div class="modal-content">
                    <div class="modal-header">
                      <h5 class="modal-title" id="loginLabel">Add File Attachment</h5>
                      <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body">
                      <div class="row">
                        <div class="col">
                          <input type="file" class="form-control" name="fileUpload" id="fileUpload">
                        </div>
                      </div>
                      <div class="row mt-1">
                        <div class="col">
                          <div class="input-group">
                            <span class="input-group-text">File Name&nbsp;</span>
                            <input type="text" class="form-control" name="fileUploadText" id="fileUploadText" placeholder="*Optional">
                          </div>
                        </div>
                      </div>
                      <div class="row">
                        <div class="col"></div>
                        <div class="col-auto">
                          <button type="submit" class="btn btn-primary" name="action" value="AA">
                            <i class="bi bi-plus"></i> Add File
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="row mb-2">
            <div class="input-group">
              <span class="input-group-text me-0"><i class="bi bi-lightbulb"></i>&nbsp;&nbsp;Subject&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
              <input type="text" class="form-control" id="eSubject" name="eSubject" placeholder="Subject of message . . ." value="${sessionScope.local.getCurrentEmail().getSubject()}">
            </div>
          </div>
          <div class="row mb-2">
            <div class="col">
              <textarea id="eBody" name="eBody" class="form-control" rows="6" placeholder="Enter message here......">${sessionScope.local.getCurrentEmail().getBody()}</textarea>
              <script>
                document.addEventListener('DOMContentLoaded', function () {
                  ClassicEditor
                          .create(document.querySelector('#eBody'), {
                            toolbar: {
                              items: ['bold', 'italic', 'link', '|','bulletedList','numberedList', '|', 'undo','redo','code'],
                              shouldNotGroupWhenFull: true  // Prevent grouping when full
                            }
                          })
                          .then(editor => {
                            // When the form is submitted
                            document.querySelector('#emailForm25').addEventListener('submit', function (event) {
                              // Update the textarea's value with the editor's data
                              editor.getData().then(data => {
                                document.querySelector('#eBody').value = data;
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
          <div class="row">
            <div class="col"></div>
            <div class="col-auto">
              <c:choose>
                <c:when test="${sessionScope.local.getCurrentActivity()!=null && sessionScope.local.getCurrentActivity().getActivity()!=null}">
                  <a class="btn btn-outline-secondary" href="ViewActivity25">Cancel</a>
                </c:when>
                <c:otherwise>
                  <a class="btn btn-outline-secondary" href="ViewHome25">Cancel</a>
                </c:otherwise>
              </c:choose>
            </div>
            <div class="col-auto">
              <button type="submit" name="action" value="SE" class="btn btn-success">
                <i class="bi bi-send"></i> Send Message
              </button>
            </div>
          </div>
        </form>
    </div>
    <div class="col-xl-1"></div>
  </div>
</div>
