<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ProposalView">
        <div class="row">
            <div class="col">
                <c:forEach var="prospect" items="${sessionScope.prospectList}">
                    <div class="row mb-1">
                        <div class="col">
                            <div class="input-group input-group-sm">
                                <c:choose>
                                    <c:when test="${prospect.getId()==sessionScope.currentProspect.getId()}">
                                        <button type="button" class="btn btn-danger" disabled name="prospectSelectButton" id="btnProspect${prospect.getId()}" value="${prospect.getId()}">
                                            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                        </button>
                                        <div class="form-control text-danger fw-bold">
                                                ${prospect.getName()}
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="submit" class="btn btn-secondary" name="prospectSelectButton" id="btnProspect${prospect.getId()}" value="${prospect.getId()}">
                                            View
                                        </button>
                                        <div class="form-control">
                                                ${prospect.getName()}
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
</form>
