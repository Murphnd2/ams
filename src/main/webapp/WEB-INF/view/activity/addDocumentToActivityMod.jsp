<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addDocAct" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-file-earmark-arrow-up me-2"></i>Upload Document</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body py-3">
        <c:if test="${not empty uploadError}">
          <div class="alert alert-danger alert-dismissible fade show py-2" role="alert" style="font-size: 0.82rem;">
            <i class="bi bi-exclamation-triangle me-1"></i>${uploadError}
            <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert" aria-label="Close" style="font-size: 0.6rem;"></button>
          </div>
        </c:if>
        <form method="post" action="AddDocumentToActivity25" enctype="multipart/form-data">
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">File</label>
          <input type="file" class="form-control form-control-sm mb-2" name="fileUpload" required>
          <label class="form-label fw-semibold" style="font-size: 0.82rem; color: var(--ssa);">Display Name <span class="fw-normal text-muted">(optional)</span></label>
          <input type="text" class="form-control form-control-sm mb-2" name="fileName" placeholder="Describe this file">
          <button type="submit" class="btn btn-sm btn-ssa w-100 mt-1">
            <i class="bi bi-cloud-upload me-1"></i>Upload
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
