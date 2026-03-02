<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addSimpleChecklistModal" tabindex="-1" aria-labelledby="addChecklistLabel" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold" id="addChecklistLabel">
          <i class="bi bi-journal-plus me-2"></i>New Checklist
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form method="post" action="CreateChecklist25">
          <%-- Title + Date row --%>
          <div class="row mb-3">
            <div class="col-sm-7 mb-2 mb-sm-0">
              <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">Title</label>
              <input type="text" class="form-control" name="reminderName" required placeholder="e.g. New group onboarding">
            </div>
            <div class="col-sm-5">
              <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">Due Date</label>
              <input type="date" class="form-control" name="reminderDate" required>
            </div>
          </div>

          <%-- Tasks --%>
          <label class="form-label text-ssa fw-semibold" style="font-size: 0.85rem;">Tasks</label>
          <div class="row g-2 mb-2">
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">1</span>
                <input type="text" class="form-control" name="s1name" required placeholder="Required">
              </div>
            </div>
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">2</span>
                <input type="text" class="form-control" name="s2name" placeholder="Optional">
              </div>
            </div>
          </div>
          <div class="row g-2 mb-2">
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">3</span>
                <input type="text" class="form-control" name="s3name" placeholder="Optional">
              </div>
            </div>
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">4</span>
                <input type="text" class="form-control" name="s4name" placeholder="Optional">
              </div>
            </div>
          </div>
          <div class="row g-2 mb-2">
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">5</span>
                <input type="text" class="form-control" name="s5name" placeholder="Optional">
              </div>
            </div>
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">6</span>
                <input type="text" class="form-control" name="s6name" placeholder="Optional">
              </div>
            </div>
          </div>
          <div class="row g-2 mb-3">
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">7</span>
                <input type="text" class="form-control" name="s7name" placeholder="Optional">
              </div>
            </div>
            <div class="col-sm-6">
              <div class="input-group input-group-sm">
                <span class="input-group-text text-ssa fw-semibold" style="font-size: 0.75rem; min-width: 1.8rem; justify-content: center;">8</span>
                <input type="text" class="form-control" name="s8name" placeholder="Optional">
              </div>
            </div>
          </div>

          <button type="submit" class="btn btn-ssa w-100">
            <i class="bi bi-journal-check me-1"></i>Create Checklist
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
