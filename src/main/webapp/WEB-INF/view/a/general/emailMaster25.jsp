<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <script src="https://cdn.ckeditor.com/ckeditor5/36.0.1/classic/ckeditor.js"></script>
  <style>
    .ck-editor__editable_inline {
      max-width: none;
      width: 100%;
      margin: 0 auto;
      box-sizing: border-box;
      height: 300px;
    }
    .recipient-chip {
      display: inline-flex;
      align-items: center;
      background: #e9ecef;
      border-radius: 20px;
      padding: 0.2rem 0.4rem 0.2rem 0.6rem;
      margin: 0.15rem;
      font-size: 0.82rem;
      gap: 0.35rem;
    }
    .recipient-chip .btn-remove {
      border: none;
      background: none;
      color: #dc3545;
      padding: 0 0.25rem;
      font-size: 0.7rem;
      cursor: pointer;
      line-height: 1;
    }
    .recipient-chip .btn-remove:hover { color: #a71d2a; }
    .attach-chip {
      display: inline-flex;
      align-items: center;
      background: #e8f4fd;
      border-radius: 20px;
      padding: 0.2rem 0.4rem 0.2rem 0.6rem;
      margin: 0.15rem;
      font-size: 0.82rem;
      gap: 0.35rem;
    }
    .attach-chip .btn-remove {
      border: none;
      background: none;
      color: #dc3545;
      padding: 0 0.25rem;
      font-size: 0.7rem;
      cursor: pointer;
      line-height: 1;
    }
    .attach-chip .btn-remove:hover { color: #a71d2a; }
    .type-badge {
      display: inline-block;
      padding: 0.15rem 0.6rem;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: 600;
      text-transform: uppercase;
    }
    .type-badge.setup    { background: #6c757d; color: white; }
    .type-badge.renewal  { background: #0d6efd; color: white; }
    .type-badge.ticket   { background: #0dcaf0; color: #000; }
    .type-badge.checklist{ background: #ffc107; color: #000; }
  </style>
  <title>Send Email</title>
  <%-- Auto-show recipient modal if person not found (vanilla JS, no jQuery needed) --%>
  <c:if test="${sessionScope.personNotFound==true}">
    <script>
      document.addEventListener('DOMContentLoaded', function() {
        var modal = new bootstrap.Modal(document.getElementById('addRecipientModal'));
        modal.show();
      });
    </script>
  </c:if>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

  <div class="row mt-3 mb-4">
    <div class="col-xl-10 offset-xl-1">

      <%-- ===== ACTIVITY CONTEXT HEADER ===== --%>
      <c:if test="${sessionScope.local.getCurrentActivity()!=null && sessionScope.local.getCurrentActivity().getActivity()!=null}">
        <c:set var="actType" value="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()}"/>
        <c:choose>
          <c:when test="${actType eq 'Setup'}">
            <c:set var="badgeClass" value="setup"/>
            <c:set var="badgeIcon" value="building"/>
          </c:when>
          <c:when test="${actType eq 'Renewal'}">
            <c:set var="badgeClass" value="renewal"/>
            <c:set var="badgeIcon" value="repeat"/>
          </c:when>
          <c:when test="${actType eq 'Ticket'}">
            <c:set var="badgeClass" value="ticket"/>
            <c:set var="badgeIcon" value="ticket-detailed"/>
          </c:when>
          <c:when test="${actType eq 'CheckList'}">
            <c:set var="badgeClass" value="checklist"/>
            <c:set var="badgeIcon" value="check"/>
          </c:when>
        </c:choose>
        <div class="d-flex align-items-center mb-3 gap-2">
          <span class="type-badge ${badgeClass}"><i class="bi bi-${badgeIcon} me-1"></i>${actType}</span>
          <span class="fw-semibold fs-5">${sessionScope.local.getCurrentActivity().getActivity().getFullName()}</span>
        </div>
      </c:if>

      <%-- ===== EMAIL COMPOSE CARD ===== --%>
      <div class="card border-0 shadow-sm">
        <div class="hdr-bar d-flex justify-content-between align-items-center">
          <span><i class="bi bi-envelope me-2"></i>Compose Email</span>
        </div>
        <div class="card-body">
          <form method="post" id="emailForm25" action="SaveEmailState25" enctype="multipart/form-data">

            <%-- RECIPIENTS --%>
            <div class="row mb-3">
              <div class="col">
                <label class="form-label text-ssa fw-bold"><i class="bi bi-people-fill me-1"></i>Recipients</label>
                <div class="d-flex flex-wrap align-items-center border rounded p-2" style="min-height: 42px;">
                  <c:if test="${sessionScope.local.getCurrentEmail().getRecipientList().size()==0}">
                    <span class="text-muted fst-italic" style="font-size: 0.85rem;">No recipients added</span>
                  </c:if>
                  <c:forEach var="recipient" items="${sessionScope.local.getCurrentEmail().getRecipientList()}">
                    <span class="recipient-chip">
                      ${recipient.getFirstName()} ${recipient.getLastName()} (${recipient.getEmail()})
                      <button type="submit" class="btn-remove" name="action" id="btnDR${recipient.getId()}" value="DR-${recipient.getId()}" title="Remove">
                        <i class="bi bi-x-lg"></i>
                      </button>
                    </span>
                  </c:forEach>
                  <button type="button" class="btn btn-sm btn-outline-ssa ms-auto" data-bs-target="#addRecipientModal" data-bs-toggle="modal">
                    <i class="bi bi-person-plus-fill me-1"></i>Add
                  </button>
                </div>
              </div>
            </div>

            <%-- ATTACHMENTS --%>
            <div class="row mb-3">
              <div class="col">
                <label class="form-label text-ssa fw-bold"><i class="bi bi-paperclip me-1"></i>Attachments</label>
                <div class="d-flex flex-wrap align-items-center border rounded p-2" style="min-height: 42px;">
                  <c:if test="${sessionScope.local.getCurrentEmail().getAttachments().size()==0}">
                    <span class="text-muted fst-italic" style="font-size: 0.85rem;">No attachments</span>
                  </c:if>
                  <c:forEach var="link" items="${sessionScope.local.getCurrentEmail().getAttachments()}">
                    <span class="attach-chip">
                      <i class="bi bi-file-earmark me-1"></i>${link.getPlainText()}
                      <button type="submit" class="btn-remove" name="action" id="btnDA${link.getId()}" value="DA-${link.getId()}" title="Remove">
                        <i class="bi bi-x-lg"></i>
                      </button>
                    </span>
                  </c:forEach>
                  <button type="button" class="btn btn-sm btn-outline-ssa ms-auto" data-bs-target="#addAttachmentModal" data-bs-toggle="modal">
                    <i class="bi bi-plus-lg me-1"></i>Add File
                  </button>
                </div>
              </div>
            </div>

            <%-- SUBJECT --%>
            <div class="row mb-3">
              <div class="col">
                <label class="form-label text-ssa fw-bold" for="eSubject"><i class="bi bi-chat-left-text me-1"></i>Subject</label>
                <input type="text" class="form-control" id="eSubject" name="eSubject" placeholder="Subject of message . . ." value="${sessionScope.local.getCurrentEmail().getSubject()}">
              </div>
            </div>

            <%-- BODY (CKEditor) --%>
            <div class="row mb-3">
              <div class="col">
                <label class="form-label text-ssa fw-bold"><i class="bi bi-body-text me-1"></i>Message</label>
                <textarea id="eBody" name="eBody" class="form-control" rows="6" placeholder="Enter message here...">${sessionScope.local.getCurrentEmail().getBody()}</textarea>
                <script>
                  document.addEventListener('DOMContentLoaded', function () {
                    var clickedAction = null;

                    // Capture which submit button was clicked
                    document.querySelectorAll('#emailForm25 button[name="action"]').forEach(function(btn) {
                      btn.addEventListener('click', function() {
                        clickedAction = this.value;
                      });
                    });

                    ClassicEditor
                            .create(document.querySelector('#eBody'), {
                              toolbar: {
                                items: ['bold', 'italic', 'link', '|','bulletedList','numberedList', '|', 'undo','redo','code'],
                                shouldNotGroupWhenFull: true
                              }
                            })
                            .then(editor => {
                              document.querySelector('#emailForm25').addEventListener('submit', function (event) {
                                event.preventDefault();
                                // Sync CKEditor content to textarea
                                document.querySelector('#eBody').value = editor.getData();
                                // Inject hidden field to preserve the action value
                                // (programmatic .submit() doesn't include the clicked button)
                                if (clickedAction) {
                                  var hidden = document.getElementById('hiddenAction');
                                  if (!hidden) {
                                    hidden = document.createElement('input');
                                    hidden.type = 'hidden';
                                    hidden.name = 'action';
                                    hidden.id = 'hiddenAction';
                                    this.appendChild(hidden);
                                  }
                                  hidden.value = clickedAction;
                                }
                                this.submit();
                              });
                            })
                            .catch(error => {
                              console.error(error);
                            });
                  });
                </script>
              </div>
            </div>

            <%-- ACTION BUTTONS --%>
            <div class="d-flex justify-content-end gap-2">
              <c:choose>
                <c:when test="${sessionScope.local.getCurrentActivity()!=null && sessionScope.local.getCurrentActivity().getActivity()!=null}">
                  <a class="btn btn-outline-ssa" href="ViewActivity25"><i class="bi bi-x-lg me-1"></i>Cancel</a>
                </c:when>
                <c:otherwise>
                  <a class="btn btn-outline-ssa" href="ViewHome25"><i class="bi bi-x-lg me-1"></i>Cancel</a>
                </c:otherwise>
              </c:choose>
              <button type="submit" name="action" value="SE" class="btn btn-ssa">
                <i class="bi bi-send me-1"></i>Send Message
              </button>
            </div>

            <%-- ===== ADD RECIPIENT MODAL (inside main form) ===== --%>
            <c:set var="readOnly" value=""/>
            <c:set var="reqd" value=""/>
            <c:set var="nameVisible" value="d-none"/>
            <c:if test="${sessionScope.personNotFound==true}">
              <c:set var="readOnly" value="readonly"/>
              <c:set var="reqd" value="required"/>
              <c:set var="nameVisible" value=""/>
            </c:if>
            <div class="modal fade" id="addRecipientModal" role="dialog" tabindex="-1" aria-labelledby="addRecipientLabel" aria-hidden="true">
              <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
                <div class="modal-content">
                  <div class="modal-header">
                    <h5 class="modal-title text-ssa fw-bold" id="addRecipientLabel"><i class="bi bi-person-plus me-2"></i>Add Recipient</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                  </div>
                  <div class="modal-body">
                    <div class="mb-3">
                      <label class="form-label text-ssa fw-bold">Email Address</label>
                      <input type="email" class="form-control" ${readOnly} name="emailName" id="emailName" value="${sessionScope.local.getCurrentEmail().getEmailToAdd()}" placeholder="Enter email address">
                    </div>
                    <div class="${nameVisible} mb-3">
                      <label class="form-label text-ssa fw-bold">Name (not found — enter manually)</label>
                      <div class="input-group">
                        <input class="form-control" type="text" name="firstName" ${reqd} placeholder="First Name">
                        <input class="form-control" type="text" name="lastName" ${reqd} placeholder="Last Name">
                      </div>
                    </div>
                    <div class="d-flex justify-content-end gap-2">
                      <button type="button" class="btn btn-outline-ssa" data-bs-dismiss="modal">Close</button>
                      <button type="submit" class="btn btn-ssa" name="action" value="AR">
                        <i class="bi bi-plus-lg me-1"></i>Add Recipient
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <%-- ===== ADD ATTACHMENT MODAL (inside main form) ===== --%>
            <div class="modal fade" id="addAttachmentModal" role="dialog" tabindex="-1" aria-labelledby="addAttachmentLabel" aria-hidden="true">
              <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
                <div class="modal-content">
                  <div class="modal-header">
                    <h5 class="modal-title text-ssa fw-bold" id="addAttachmentLabel"><i class="bi bi-file-earmark-plus me-2"></i>Add Attachment</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                  </div>
                  <div class="modal-body">
                    <div class="mb-3">
                      <label class="form-label text-ssa fw-bold">Select File</label>
                      <input type="file" class="form-control" name="fileUpload" id="fileUpload">
                    </div>
                    <div class="mb-3">
                      <label class="form-label text-ssa fw-bold">Display Name <span class="text-muted fw-normal">(optional)</span></label>
                      <input type="text" class="form-control" name="fileUploadText" id="fileUploadText" placeholder="Custom file name">
                    </div>
                    <div class="d-flex justify-content-end gap-2">
                      <button type="button" class="btn btn-outline-ssa" data-bs-dismiss="modal">Close</button>
                      <button type="submit" class="btn btn-ssa" name="action" value="AA">
                        <i class="bi bi-plus-lg me-1"></i>Add File
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </form>
        </div>
      </div>

    </div>
  </div>
</div>

</body>
</html>
