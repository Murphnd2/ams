<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ModAgency">
    <div class="row">
        <div class="col-lg-6">
            <div class="row">
                <div class="col">
                    <c:import url="/WEB-INF/view/general/agencyBlockMod.jsp"></c:import><%----%>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <c:import url="/WEB-INF/view/general/addressBlockMod.jsp"></c:import><%----%>
                </div>
            </div>
        </div>
        <div class="col-lg-6">
            <div class="row mb-3">
                <div class="col">
                    <div class="input-group input-group-sm">
                        <label class="input-group-text bg-dark text-light">Primary Contact</label>
                    </div>
                </div>
            </div>
            <c:import url="/WEB-INF/view/general/personBlockMod.jsp"></c:import><%----%>
        </div>
        <c:choose>
            <c:when test="${sessionScope.formDisable}">
            </c:when>
            <c:otherwise>
                <div class="col">
                    <div class="row mb-3">
                        <div class="col-lg-6">
                            <button type="submit"  class="btn btn-primary w-100">Click to Save Changes</button>
                        </div>
                        <div class="col-lg-6">
                            <a href="ToggleState"  class="btn btn-danger w-100">Exit</a>
                        </div>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</form>
