<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:import url="/WEB-INF/view/activity/automationForm.jsp"></c:import>
<c:set var="allDone" value=""></c:set>
<div class="container-fluid m-0 p-0">
  <c:choose>
    <c:when test="${sessionScope.currentPerson.getId()==104}">
      <div class="row overflow-auto m-0 p-0" style="max-height: 575px">
    </c:when>
    <c:otherwise>
      <div class="row overflow-auto m-0 p-0" style="max-height: 625px">
    </c:otherwise>
  </c:choose>
    <div class="col m-0 p-0">
      <c:forEach var="toDo" items="${sessionScope.currentToDoList}">
        <c:if test="${!toDo.isComplete()}">
          <c:set var="allDone" value="disabled"></c:set>
        </c:if>
        <c:set var="taskId" value=""></c:set>
        <c:if test="${sessionScope.currentPerson.getId()==104}">
          <c:set var="taskId2" value="${toDo.getTask().getId()}&nbsp;"></c:set>
        </c:if>
        <c:set var="autColor" value=""></c:set>
        <c:if test="${toDo.getTask().isAutomated() && sessionScope.currentPerson.getId()==105}">
          <c:set var="autColor" value="fw-bold text-danger"></c:set>
        </c:if>
        <c:choose>
          <c:when test="${toDo.getTask().getDescription().equals(\"Default\")}">
          </c:when>
          <c:when test="${toDo.isComplete()}">
            <form method="post" action="ReOpenToDo" class="m-0 d-none d-sm-grid">
              <div class="input-group input-group-sm p-0 m-0">
                <button type="submit" class="btn btn-outline-success border-white border-0 m-0 p-0" name="btnToDo" id="btn${toDo.getId()}-${toDo.getId()}" value="${toDo.getId()}">
                  <i class="bi bi-x-square" style="font-size: 1.4rem"></i>
                </button>
                <div class="form-control text-success fw-lighter p-0 pt-2 ps-2 border-white border-0"  style="text-decoration:line-through;font-style: italic;font-size:0.65rem">
                    ${toDo.getTask().getDescription()}
                </div>
              </div>
            </form>
          </c:when>
          <c:otherwise>
            <form method="post" action="CloseToDo" class="m-0">
              <div class="input-group input-group-sm p-0 m-0">
                <c:choose>
                  <c:when test="${toDo.getTask().getId()==38590}">
                    <button class="btn btn-outline-success border-white border-0 m-0 p-0" type="button" data-bs-toggle="modal" data-bs-target="#sendProposalModal">
                      <i class="bi bi-send" style="font-size: 1.4rem"></i>
                    </button><div class="modal fade" id="sendProposalModal" role="dialog" tabindex="-1" aria-labelledby="sendProposalLabel" aria-hidden="true">
                    <div class="modal-dialog modal-xl modal-fullscreen-sm-down" role="document">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="sendProposalLabel">Send Intro</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                        </div>
                        <div class="modal-body">
                          <div class="input-group mb-2">
                            <span class="input-group-text">Agent Name / Email</span>
                            <input class="form-control" id="agentName" name="agentName">
                            <input type="email" class="form-control" id="agentEmail" name="agentEmail">
                          </div>
                          <div class="input-group mb-2">
                            <span class="input-group-text">Contact Name / Email</span>
                            <input class="form-control" id="prosContactName" name="prosContactName">
                            <input type="email" class="form-control" id="prosContactEmail" name="prosContactEmail">
                          </div>
                          <div class="input-group mb-2">
                            <span class="input-group-text">Company Name</span>
                            <input class="form-control" id="prosCompany" name="prosCompany">
                          </div>
                          <div class="input-group mb-2">
                            <span class="input-group-text">Proposal</span>
                            <input class="form-control" id="prosProposal" name="prosProposal">
                          </div>
                          <div class="form-check form-check-inline">
                            <input class="form-check-input" type="checkbox" id="prosFSA" value="FSA">
                            <label class="form-check-label" for="prosFSA">FSA</label>
                          </div>
                          <div class="form-check form-check-inline">
                            <input class="form-check-input" type="checkbox" id="prosHRA" value="HRA">
                            <label class="form-check-label" for="prosHRA">HRA</label>
                          </div>
                          <div class="form-check form-check-inline">
                            <input class="form-check-input" type="checkbox" id="prosCOB" value="COBRA">
                            <label class="form-check-label" for="prosCOB">COB</label>
                          </div>
                          <button type="submit" class="btn btn-primary" name="btnToDo" id="btnToDo-${toDo.getId()}" value="B-${toDo.getId()}">Send to Agent Only</button>
                          <button type="submit" class="btn btn-primary" name="btnToDo" id="btnToDo-${toDo.getId()}" value="P-${toDo.getId()}">Send to Prospect</button>
                          <button type="submit" class="btn btn-outline-success border-white border-0 m-0 p-0" name="btnToDo" id="btn${toDo.getId()}-${toDo.getId()}-1" value="${toDo.getId()}">
                            <i class="bi bi-square" style="font-size: 1.4rem"></i>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                  </c:when>

                  <c:when test="${toDo.getTask().getId()==4976}">
                    <button class="btn btn-outline-success border-white border-0 m-0 p-0" type="button" data-bs-toggle="modal" data-bs-target="#sendSummitModal">
                      <i class="bi bi-send" style="font-size: 1.4rem"></i>
                    </button><div class="modal fade" id="sendSummitModal" role="dialog" tabindex="-1" aria-labelledby="sendSummitLabel" aria-hidden="true">
                    <div class="modal-dialog modal-xl modal-fullscreen-sm-down" role="document">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="sendSummitLabel">Send Intro</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                        </div>
                        <div class="modal-body">
                          <div class="input-group mb-2">
                            <span class="input-group-text">Contact Username</span>
                            <input class="form-control" id="contactUserName" name="contactUserName">
                          </div>
                          <div class="input-group mb-2">
                            <span class="input-group-text">Contact Password</span>
                            <input class="form-control" id="contactPassword" name="contactPassword">
                          </div>
                          <button type="submit" class="btn btn-primary" name="btnToDo" id="btnToDo-${toDo.getId()}" value="S-${toDo.getId()}">Send</button>
                          <button type="submit" class="btn btn-outline-success border-white border-0 m-0 p-0" name="btnToDo" id="btn${toDo.getId()}-${toDo.getId()}-1" value="${toDo.getId()}">
                            <i class="bi bi-square" style="font-size: 1.4rem"></i>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                  </c:when>
                  <c:otherwise>
                    <button type="submit" class="btn btn-outline-success border-white border-0 m-0 p-0" name="btnToDo" id="btn${toDo.getId()}-${toDo.getId()}" value="${toDo.getId()}">
                      <i class="bi bi-square" style="font-size: 1.4rem"></i>
                    </button>
                  </c:otherwise>
                </c:choose>
                <div class="form-control border-white p-0 pt-2 ps-2 border-0 ${autColor}" style="font-size: 0.65rem;">
                  <c:choose>
                    <c:when test="${toDo.getTask().getWebLinkList().size()==0}"><%-- **************************************** NO WEBLINKS TIED TO TASK --%>
                      ${taskId}${toDo.getTask().getDescription()}
                    </c:when>
                    <c:when test="${toDo.getTask().getWebLinkList().size()>=1}"><%-- **************************************** ONLY 1 LINK TIED TO TASK --%>
                      <c:choose>
                        <c:when test="${toDo.getTask().getWebLinkList().get(0).getLinkType().getId()==2}"><%-- --------link ia a hyperlink ------------- --%>
                          <a href="${toDo.getTask().getWebLinkList().get(0).getLinkPath()}" target="_blank">${taskId}${toDo.getTask().getDescription()} (Link)</a>
                        </c:when>
                        <c:otherwise><%-- -------------------------------------------------------------------link is a file upload ------------ --%>
                          <a href="ViewFileUpload?doc=${toDo.getTask().getWebLinkList().get(0).getLinkPath()}" target="_blank">${taskId}${toDo.getTask().getDescription()} (File)</a>
                        </c:otherwise>
                      </c:choose>
                    </c:when>
                    <c:otherwise><%-- ******************************************************************************** MULITPLE LINKS TIED TO TASK --%>
                      <a href="#" target="_blank">${taskId}{toDo.getTask().getDescription()} (2+)</a>
                    </c:otherwise>
                  </c:choose>
                </div>

                <c:if test="${!toDo.getTask().isAutomated() && sessionScope.currentPerson.getId()==104}">
                  <button type="button" class="btn btn-outline-success border-white border-0 m-0 p-0" data-bs-target="#addNewAutoModal-${toDo.getId()}" data-bs-toggle="modal">
                    <i class="bi bi-arrow-up-circle"></i>
                  </button>
                  <div class="modal fade" id="addNewAutoModal-${toDo.getId()}" role="dialog" tabindex="-1" aria-labelledby="addNewAutoModal" aria-hidden="true">
                    <div class="modal-dialog modal-fullscreen" role="document">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="newAutoLabel">Create Automation for Task</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                        </div>
                        <div class="modal-body">
                          <div class="row mb-1">
                              <div class="col-5">
                                <div class="input-group">
                                  <span class="input-group-text">
                                    Servlet Name
                                  </span>
                                  <input class="form-control" name="servName" value="SendAutoEmail?aeId=${toDo.getTask().getId()}" readonly>
                                </div>
                              </div>
                              <div class="col-7">
                                <div class="input-group">
                                  <span class="input-group-text">
                                    Automation Text
                                  </span>
                                  <input class="form-control" name="autoText" >
                                </div>
                              </div>
                          </div>
                          <div class="row mb-1">
                            <div class="col"></div>
                            <div class="col-auto">
                              <div class="form-label">((#erName))((#activityType))(sbj)(/sbj)(ii)(l)(cc)(/ii)(br/)(nl)(rf)(/rf)((close))((webLinkTask))((sig))</div>
                            </div>
                            <div class="col"></div>
                            <div class="col-3"></div>
                          </div>
                          <div class="row mb-1">
                            <div class="col-9">
                              <textarea rows="20" class="form-control" name="content"></textarea>
                            </div>
                            <div class="col-3 fs fs-6 overflow-auto" style="height:500px">
                              <c:forEach var="iLink" items="${sessionScope.insertLinkList}">
                                <div class="row fs-6">
                                  <div class="col-auto fs-6">
                                    <label style="font-size: x-small">${iLink.getId()}</label>
                                  </div>
                                  <div class="col fs-6">
                                    <label style="font-size: x-small">${iLink.getPlainText()}</label>
                                  </div>
                                </div>
                              </c:forEach>
                            </div>
                          </div>
                          <div class="row mb-1">
                              <div class="col">
                                <button type="submit" class="btn btn-success w-100" name="btnToDo" id="btnToDo-${toDo.getId()}" value="Z-${toDo.getId()}">Automate</button>
                              </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </c:if>
                <c:if test="${toDo.getTask().getWebLinkList().size()==0}">
                  <button type="button" class="btn btn-outline-success border-white border-0 m-0 p-0" data-bs-target="#addLinkModal-${toDo.getId()}" data-bs-toggle="modal">
                    <i class="bi bi-paperclip"></i>
                  </button>
                  <div class="modal fade" id="addLinkModal-${toDo.getId()}" role="dialog" tabindex="-1" aria-labelledby="addLinkModal" aria-hidden="true">
                    <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="loginLabel">Attach Link to Task</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
                        </div>
                        <div class="modal-body">
                          <div class="row">
                            <div class="row mb-3">
                              <div class="col">
                                <div class="input-group">
                                  <span class="input-group-text text-muted">URL:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
                                  <input type="url" class="form-control" name="linkPath" id="linkPath" placeholder="https://example.com" pattern="https://.*" >
                                </div>
                              </div>
                            </div>
                            <div class="row mb-3">
                              <div class="col">
                                <div class="input-group">
                                  <span class="input-group-text text-muted">Link Name</span>
                                  <input type="text" class="form-control" name="linkName" id="linkName" placeholder="Enter a name for your link" >
                                </div>
                              </div>
                            </div>
                            <div class="row">
                              <div class="col">
                                <button type="submit" class="btn btn-success w-100" name="btnToDo" id="btnToDo-${toDo.getId()}" value="L-${toDo.getId()}">Add</button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </c:if>
              </div>
            </form>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </div>
  </div>
</div>
<form method="post" action="CloseSingleItemChecklist">
  <div class="row mt-1">
    <div class="col-6">
      <button class="ssa-action primary w-100" type="button" data-bs-toggle="modal" data-bs-target="#addToDoModal">
        <i class="bi bi-card-checklist me-1"></i>Add Task
      </button>
    </div>
    <div class="col-6">
      <c:choose>
        <c:when test="${sessionScope.adminView==1}">
          <button type="button" class="ssa-action secondary w-100" name="btnCheckList" data-bs-target="#closeActivity" data-bs-toggle="modal" ${allDone}>
            <i class="bi bi-door-open me-1"></i>Close Setup
          </button>
        </c:when>
        <c:when test="${sessionScope.adminView==2}">
          <button type="button" class="ssa-action secondary w-100" data-bs-target="#closeActivity" data-bs-toggle="modal" name="btnCheckList" ${allDone}>
            <i class="bi bi-door-open me-1"></i>Close Renewal
          </button>
        </c:when>
        <c:when test="${sessionScope.adminView==3}">
          <button type="button" class="ssa-action secondary w-100" data-bs-target="#closeActivity" data-bs-toggle="modal" ${allDone} value="${sessionScope.currentToDoList.get(0).getCheckList().getId()}">
            <i class="bi bi-door-open me-1"></i>Close Ticket
          </button>
        </c:when>
        <c:otherwise>
          <button type="button" class="ssa-action secondary w-100" data-bs-target="#closeActivity" data-bs-toggle="modal"  ${allDone} value="${sessionScope.currentToDoList.get(0).getCheckList().getId()}">
            <i class="bi bi-door-open me-1"></i>Close Checklist
          </button>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <%--  **************************************** USE 104 BELOW IF WANT TO SHOW ********************************************************* --%>
  <c:if test="${sessionScope.currentPerson.getId()==103}">
    <button type="button" class="btn btn-warning w-100" data-bs-target="#AssignToSequence" data-bs-toggle="modal">Assign To Sequence ${sessionScope.currentPerson.getId()}</button>
    <div class="row mt-1">
      <div class="col">
        <c:if test="${sessionScope.currentPerson.getId()==104}">
          <button class="btn btn-sm btn-outline-success w-100" type="button" data-bs-toggle="modal" data-bs-target="#addSpecificTask">
            Add Task #
          </button>
        </c:if>
      </div>
    </div>
  </c:if>
  <c:import url="/WEB-INF/view/activity/closeActivityModal.jsp"></c:import>
</form>
<c:import url="/WEB-INF/view/activity/checklist/addToDoToChecklistModal.jsp"></c:import>

<c:import url="/WEB-INF/view/activity/checklist/addToDoByTaskNumModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/checklist/sequences/requiredList/requiredFromChecklistModal.jsp"></c:import>


