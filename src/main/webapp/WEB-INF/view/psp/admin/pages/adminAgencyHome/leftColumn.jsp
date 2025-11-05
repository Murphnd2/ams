<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <div class="row mb-3">
        <%-- *********** L I S T   O F   A L L   P S P   A G E N C I E S ******************** --%>
        <c:import url="/WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/agencyList.jsp"></c:import>
    </div>
    <hr/>
<c:if test="${sessionScope.hasCurrentAgency==true}">
    <div class="row mb-3">
        <%-- *********** L I S T   O F   A L L   A G E N C Y   A G E N T S ******************* --%>
        <c:import url="/WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/agentList.jsp"></c:import>
    </div>
    <hr/>
</c:if>
<c:if test="${sessionScope.hasCurrentAgent==true}">
    <div class="row mb-1">
        <%-- *********** L I S T   O F   A L L   A G E N T   P R O S P E C T S ******************* --%>
        <c:import url="/WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/prospectList.jsp"></c:import>
    </div>
    <hr/>
</c:if>
<c:if test="${sessionScope.hasCurrentProspect==true}">
    <div class="row mb-2">
        <%-- *********** L I S T   O F   A L L   P R O S P E C T   P R O P O S A L S ******************* --%>
        <c:import url="/WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/proposalList.jsp"></c:import>
    </div>
</c:if>
