<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="AddModuleToLos" method="post">
    <div class="input-group input-group-sm mb-3">
        <label for="losList" class="input-group-text">Line of Service</label>
        <input type="text" name="losList" id="losList" readonly class="form-control" value="${sessionScope.currentLos.getDescription()}">
    </div>
    <c:import url="/WEB-INF/view/psp/admin/parts/ddServiceModulesRaw.jsp"></c:import>
    <div class="row">
        <div class="col">
            <button class="btn btn-primary form-control" type="submit" id="modAddButton" name="modAddButton" value="1">Link Group to LOS</button>
        </div>
    </div>
</form>
