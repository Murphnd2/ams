<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addRenewalModal" role="dialog" tabindex="-1" aria-labelledby="addRenewalModal" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="loginLabel">Select an Employer to Renew</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
            </div>
            <div class="modal-body">
                <c:import url="/WEB-INF/view/activity/renew/renewalListV01.jsp"></c:import>
            </div>
        </div>
    </div>
</div>
