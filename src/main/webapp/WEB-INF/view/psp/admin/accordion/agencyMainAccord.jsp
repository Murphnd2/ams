<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="accordion" id="accordionExample">
  <div class="accordion-item">
    <h2 class="accordion-header" id="headingOne">
      <button class="accordion-button ${sessionScope.pOneA}" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
        ${sessionScope.currentAgency.getName()} (Agency)
      </button>
    </h2>
    <div id="collapseOne" class="accordion-collapse collapse ${sessionScope.pOneB}" aria-labelledby="headingOne" data-bs-parent="#accordionExample">
      <div class="accordion-body">
        <c:import url="/WEB-INF/view/psp/admin/forms/modAgencyForm.jsp"></c:import>
        <c:if test="${sessionScope.formDisable}">
          <div class="row">
            <div class="col">
              <form action="ToggleState" method="post">
                <input name="formSender" hidden value="ModifyAgencyForm" id="formSender">
                <button type="submit" class="btn btn-outline-danger w-100">Unlock and Modify</button>
              </form>
            </div>
          </div>
        </c:if>
      </div>
    </div>
  </div>
  <c:if test="${sessionScope.hasCurrentAgent}">
    <div class="accordion-item">
      <h2 class="accordion-header" id="headingTwo">
        <button class="accordion-button ${sessionScope.pTwoA}" type="button" data-bs-toggle="collapse" data-bs-target="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">
            ${sessionScope.currentAgent.getFirstName()}&nbsp;${sessionScope.currentAgent.getLastName()} (Agent)
        </button>
      </h2>
      <div id="collapseTwo" class="accordion-collapse collapse ${sessionScope.pTwoB}" aria-labelledby="headingTwo" data-bs-parent="#accordionExample">
        <div class="accordion-body">
          <c:import url="/WEB-INF/view/psp/admin/forms/modAgentForm.jsp"></c:import>
        </div>
      </div>
    </div>
  </c:if>
  <div class="accordion-item">
    <h2 class="accordion-header" id="headingThree">
      <button class="accordion-button ${sessionScope.pThreeA}" type="button" data-bs-toggle="collapse" data-bs-target="#collapseThree" aria-expanded="false" aria-controls="collapseThree">
        <c:choose>
          <c:when test="${sessionScope.hasCurrentAgent}">
            ${sessionScope.currentAgent.getFirstName()}'s Prospects
          </c:when>
          <c:otherwise>
            All Agency Prospects
          </c:otherwise>
        </c:choose>
      </button>
    </h2>
    <div id="collapseThree" class="accordion-collapse collapse ${sessionScope.pThreeB}" aria-labelledby="headingThree" data-bs-parent="#accordionExample">
      <div class="accordion-body">
        <div class="row">
          <div class="col-lg-6">
            <c:import url="/WEB-INF/view/psp/admin/forms/prospectListForm.jsp"></c:import>
            <div class="row">
              <div class="col">
                <button class="btn btn-outline-secondary w-100" name="btnAddProspect" id="btnAddProspect" data-bs-toggle="modal" data-bs-target="#addProspectModal">Add Prospect</button>
              </div>
            </div>
          </div>
          <div class="col-lg-6">
            <c:if test="${sessionScope.hasCurrentProspect}">
              <c:import url="/WEB-INF/view/general/personBlockProspectMod.jsp"></c:import>
            </c:if>
          </div>
        </div>
      </div>
    </div>
  </div>
  <c:if test="${sessionScope.hasCurrentProspect}">
    <div class="accordion-item">
      <h2 class="accordion-header" id="headingFour">
        <button class="accordion-button ${sessionScope.pFourA}" type="button" data-bs-toggle="collapse" data-bs-target="#collapseFour" aria-expanded="false" aria-controls="collapseFour">
            Proposals for ${sessionScope.currentProspect.getName()}
        </button>
      </h2>
      <div id="collapseFour" class="accordion-collapse collapse ${sessionScope.pFourB}" aria-labelledby="headingFour" data-bs-parent="#accordionExample">
        <div class="accordion-body">
          <c:import url="/WEB-INF/view/psp/admin/forms/proposalListForm.jsp"></c:import>
          <div class="row">
            <div class="col-lg-6">
              <button class="btn btn-outline-danger w-100" name="btnAddProposal" id="btnAddProposal" data-bs-toggle="modal" data-bs-target="#addProposalModal">Add Proposal</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </c:if>
</div>
