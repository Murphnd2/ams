<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddRequiredSequence">
    <div class="row mb-3">
        <div class="input-group">
            <span class="input-group-text">For&nbsp;&nbsp;</span>
            <c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/components/ddTemplatePurposes.jsp"></c:import>
        </div>
    </div>
    <div class="row mb-3">
        <div class="input-group">
            <button class="btn btn-primary w-100" type="submit">Create</button>
        </div>
    </div>
</form>
