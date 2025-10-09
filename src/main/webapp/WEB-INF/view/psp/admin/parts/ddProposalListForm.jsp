<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="row">
  <div class="btn-group justify-content-start">
    <button type="button" class="btn btn-outline-success dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
      Proposals:
      <span class="fst-italic">
        <c:choose>
          <c:when test="${sessionScope.currentProspect.getName().length()>20}">
            ${sessionScope.currentProspect.getName().substring(0,19)}...
          </c:when>
          <c:otherwise>
            ${sessionScope.currentProspect.getName()}
          </c:otherwise>
        </c:choose>
      </span>
    </button>
    <c:url var="proposalPath" value="/serviceProposal"></c:url>
    <ul class="dropdown-menu">
      <c:forEach var="proposal" items="${sessionScope.proposalList}">
        <li>
          <a href="${proposalPath}?guid=${proposal.getApplicationGUID()}" target="_blank" class="dropdown-item">
            <div class="input-group input-group-sm">
              <button type="button" class="btn btn-secondary">View</button>
              <span class="input-group-text">#${proposal.getId()}</span>
              <span class="form-control text-wrap pe-auto">
                <c:forEach var="losItem" items="${proposal.getLosList()}">
                  &nbsp;${losItem.getShortText()}
                </c:forEach>
              </span>
            </div>
          </a>
        </li>
      </c:forEach>
      <li><hr class="dropdown-divider"></li>
      <li>
        <button class="dropdown-item" name="btnAddProposal1" id="btnAddProposal1" data-bs-toggle="modal" data-bs-target="#addProposalModal">Add Proposal</button>
      </li>
    </ul>
  </div>
</div>

