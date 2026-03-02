<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="removeLastRenewalItemModal" role="dialog" tabindex="-1" aria-labelledby="removeLastRenewalItemModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Please Confirm</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <div class="row">
          <div class="col">
            <div class="form-label">You are about to remove the only remaining renewal item from this renewal.
              This will close this renewal from your active list.  The item will be available to regenerate into a new renewal.</div>
          </div>
        </div>
        <div class="row">
          <div class="col">
            <a href="DeleteSingleItemRenewal" class="ssa-action danger w-100" style="text-decoration:none;">
              <i class="bi bi-trash me-1"></i>Remove Final Renewal Item
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
