<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="otherContactsModal" role="dialog" tabindex="-1" aria-labelledby="otherContactsModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header mb-0 pb-0">
        <h5 class="modal-title text-ssa fw-bold fs-5" id="loginLabel">
          <i class="bi bi-people"></i> Other Contact List</h5>
        <button class="btn btn-sm" name="showModConForm1" id="btnShowModConForm12" type="button" data-bs-target="#addContactToActivity" data-bs-toggle="modal">
          [add]
        </button>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body mt-0 pt-0">
        <c:import url="/WEB-INF/view/a/activityDetail/columns/modals/contactManager25.jsp"></c:import>

      </div>
    </div>
  </div>
</div>
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/addContactToActivityMod.jsp"></c:import>
