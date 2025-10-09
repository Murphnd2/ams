<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="AddSetupModule25">
    <c:set var="disThis" value=""></c:set>
    <div class="row">
        <div class="col m-0 p-0 ms-1 me-1">
            <select class="form-select" aria-label="recurring freq type drop down" name="remainingModList" id="remainingModList">
                <c:choose>
                    <c:when test="${sessionScope.local.getCurrentActivity().getModsNotInSetup().size()>0}">
                        <c:forEach var="modItem" items="${sessionScope.local.getCurrentActivity().getModsNotInSetup()}">
                            <option value="${modItem.getId()}">${modItem.getDescription()}</option>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <option>No Remaining Modules</option>
                        <c:set var="disThis" value="disabled"></c:set>
                    </c:otherwise>
                </c:choose>
            </select>
        </div>
        <div class="col-auto m-0 p-0 me-1">
            <button type="submit" class="btn btn-outline-secondary" ${disThis}>
                <i class="bi bi-bookmark-plus"></i> Add
            </button>
        </div>
    </div>
</form>
