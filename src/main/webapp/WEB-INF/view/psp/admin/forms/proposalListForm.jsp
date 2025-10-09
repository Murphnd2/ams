<%@ page import="jakarta.persistence.EntityManagerFactory" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ProposalDetail">
  <div class="row">
    <div class="col">
      <c:forEach var="proposal" items="${sessionScope.proposalList}">
        <div class="row mb-1">
          <div class="col">
            <div class="input-group input-group-sm">
              <c:choose>
                <c:when test="${proposal.getId()==sessionScope.currentProposal.getId()}">
                  <button type="button" class="btn btn-danger" disabled name="proposalSelectButton" id="btnProposal${proposal.getId()}" value="${proposal.getId()}">
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                  </button>
                  <div class="form-control text-danger fw-bold">
                      ${proposal.getId()}
                  </div>
                </c:when>
                <c:otherwise>
                  <span class="input-group-text">
                    #${proposal.getId()}
                  </span>
                  <div class="form-control">
                    <div class="row">
                      <c:forEach var="losItem" items="${proposal.getLosList()}">
                        <div class="col-auto me-2">${losItem.getDescription()}</div>
                      </c:forEach>
                    </div>
                  </div>
                  <c:url var="proposalPath" value="/serviceProposal"></c:url>
                  <a href="${proposalPath}?guid=${proposal.getApplicationGUID()}" target="_blank"><button type="button" class="btn btn-secondary h-100">View</button></a>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
  </div>
</form>
