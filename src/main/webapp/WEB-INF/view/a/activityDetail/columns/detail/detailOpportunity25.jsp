<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="opp" value="${sessionScope.local.getCurrentActivity().getActivity()}" />
<div class="row m-1">
    <div class="col">
        <%-- Stage & Key Info --%>
        <div class="row mb-2">
            <div class="col-md-6">
                <table class="table table-sm table-borderless mb-0" style="font-size: 0.85rem;">
                    <tr>
                        <td class="text-muted fw-semibold" style="width: 130px;">Stage</td>
                        <td>
                            <c:choose>
                                <c:when test="${opp.getStage() == 'NEW'}"><span class="badge bg-primary">New</span></c:when>
                                <c:when test="${opp.getStage() == 'CONTACTED'}"><span class="badge bg-success">Contacted</span></c:when>
                                <c:when test="${opp.getStage() == 'QUALIFIED'}"><span class="badge bg-warning text-dark">Qualified</span></c:when>
                                <c:when test="${opp.getStage() == 'PROPOSAL_SENT'}"><span class="badge bg-info">Proposal Sent</span></c:when>
                                <c:when test="${opp.getStage() == 'NEGOTIATION'}"><span class="badge bg-danger">Negotiation</span></c:when>
                                <c:when test="${opp.getStage() == 'WON'}"><span class="badge bg-success">Won</span></c:when>
                                <c:when test="${opp.getStage() == 'LOST'}"><span class="badge bg-dark">Lost</span></c:when>
                                <c:when test="${opp.getStage() == 'ON_HOLD'}"><span class="badge bg-secondary">On Hold</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">${opp.getStage()}</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <c:if test="${opp.getProspect() != null}">
                        <tr>
                            <td class="text-muted fw-semibold">Prospect</td>
                            <td>${fn:escapeXml(opp.getProspect().getName())}</td>
                        </tr>
                    </c:if>
                    <c:if test="${opp.getAgency() != null}">
                        <tr>
                            <td class="text-muted fw-semibold">Agency</td>
                            <td>${fn:escapeXml(opp.getAgency().getName())}</td>
                        </tr>
                    </c:if>
                    <c:if test="${opp.getAssignedTo() != null}">
                        <tr>
                            <td class="text-muted fw-semibold">Assigned To</td>
                            <td>${opp.getAssignedTo().getFirstName()} ${opp.getAssignedTo().getLastName()}</td>
                        </tr>
                    </c:if>
                </table>
            </div>
            <div class="col-md-6">
                <table class="table table-sm table-borderless mb-0" style="font-size: 0.85rem;">
                    <c:if test="${opp.getEstimatedEmployees() != null}">
                        <tr>
                            <td class="text-muted fw-semibold" style="width: 130px;">Est. Employees</td>
                            <td>${opp.getEstimatedEmployees()}</td>
                        </tr>
                    </c:if>
                    <c:if test="${opp.getEstimatedValue() != null}">
                        <tr>
                            <td class="text-muted fw-semibold">Est. Value</td>
                            <td>$<fmt:formatNumber value="${opp.getEstimatedValue()}" pattern="#,##0" /></td>
                        </tr>
                    </c:if>
                    <c:if test="${opp.getExpectedCloseDate() != null}">
                        <tr>
                            <td class="text-muted fw-semibold">Expected Close</td>
                            <td><fmt:formatDate value="${opp.getExpectedCloseDate()}" pattern="M/d/yyyy" /></td>
                        </tr>
                    </c:if>
                </table>
            </div>
        </div>

        <%-- Proposals for this Prospect --%>
        <c:if test="${not empty opportunityProposals}">
            <div class="row mt-2">
                <div class="col">
                    <h6 class="fw-bold" style="font-size: 0.85rem;">
                        <i class="bi bi-file-earmark-text me-1"></i>Proposals for ${fn:escapeXml(opp.getProspect().getName())}
                    </h6>
                    <table class="table table-sm table-hover" style="font-size: 0.82rem;">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Status</th>
                                <th>Services</th>
                                <th>Created</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="prop" items="${opportunityProposals}">
                                <tr>
                                    <td>
                                        <a href="ProposalDetail?id=${prop.getId()}" class="text-decoration-none">#${prop.getId()}</a>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${prop.getStatus() == 'CREATED'}"><span class="badge bg-secondary">Created</span></c:when>
                                            <c:when test="${prop.getStatus() == 'SENT'}"><span class="badge bg-info">Sent</span></c:when>
                                            <c:when test="${prop.getStatus() == 'VIEWED'}"><span class="badge bg-warning text-dark">Viewed</span></c:when>
                                            <c:when test="${prop.getStatus() == 'APPLIED'}"><span class="badge bg-primary">Applied</span></c:when>
                                            <c:when test="${prop.getStatus() == 'APPROVED'}"><span class="badge bg-success">Approved</span></c:when>
                                            <c:when test="${prop.getStatus() == 'DENIED'}"><span class="badge bg-danger">Denied</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary">${prop.getStatus()}</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:forEach var="los" items="${prop.getLosList()}">
                                            <span class="badge bg-light text-dark border" style="font-size: 0.7rem;">${los.getShortText()}</span>
                                        </c:forEach>
                                    </td>
                                    <td>
                                        <c:if test="${prop.getDateCreated() != null}">
                                            <fmt:formatDate value="${prop.getDateCreated()}" pattern="M/d/yy" />
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:if>

        <%-- Action: Create Proposal --%>
        <c:if test="${opp.getProspect() != null && !opp.isComplete()}">
            <div class="row mt-2">
                <div class="col">
                    <a href="ProposalBuilder?prospectId=${opp.getProspect().getId()}" class="btn btn-sm btn-outline-primary">
                        <i class="bi bi-file-earmark-plus me-1"></i>Create Proposal
                    </a>
                    <a href="AgentHome" class="btn btn-sm btn-outline-secondary ms-1">
                        <i class="bi bi-arrow-left me-1"></i>Back to Pipeline
                    </a>
                </div>
            </div>
        </c:if>
    </div>
</div>
