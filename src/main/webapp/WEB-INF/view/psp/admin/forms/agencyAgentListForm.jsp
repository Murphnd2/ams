<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="SelectAgent">
    <c:forEach var="agent" items="${sessionScope.agencyAgentList}">
        <div class="row mb-1">
            <div class="col">
                <div class="input-group input-group-sm">
                    <c:choose>
                        <c:when test="${sessionScope.currentAgent.getId()==agent.getId()}">
                            <button type="button" class="btn btn-danger" disabled name="btnAgentSelect" id="btnAgent${agent.getId()}" value="${agent.getId()}">
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                            </button>
                            <div class="form-control">
                                    ${agent.toString()}
                            </div>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" class="btn btn-secondary" name="btnAgentSelect" id="btnAgent${agent.getId()}" value="${agent.getId()}">
                                View
                            </button>
                            <div class="form-control">
                                    ${agent.toString()}
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </c:forEach>
</form>
