<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container-fluid m-0 p-0">
  <div class="row m-0 p-0 overflow-auto" style="max-height:575px">
    <div class="col m-0 p-0">
      <c:set var="isPast" value="pe-none"></c:set>
      <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
        <c:set var="isPast" value=""></c:set>
      </c:if>
      <c:set var="blockFuture" value="${false}"></c:set>
      <c:set var="myId" value="${sessionScope.local.getCurrentPerson().getId()}"></c:set>
      <c:set var="isMyActivity" value="${myId==sessionScope.local.getCurrentActivity().getActivity().getAssignedTo().getId()}"></c:set>
      <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}" varStatus="tds">
        <c:if test="${toDo.getTask().getId()!=153}">
          <div id="codeBehind">
              <%-- ***************************************** BASIC ITEMS ********************************************************************* --%>
            <div id="basic_items_cb1">
              <c:set var="isComp" value="${false}"></c:set>
              <c:if test="${toDo.isComplete()==true}">
                <c:set var="isComp" value="${true}"></c:set>
              </c:if>
              <c:set var="hasAuto" value="${false}"></c:set>
              <c:if test="${toDo.hasAutomation()==true}">
                <c:set var="hasAuto" value="${true}"></c:set>
              </c:if>
              <c:set var="canEarly" value="${false}"></c:set>
              <c:if test="${toDo.allowsEarly()==true}">
                <c:set var="canEarly" value="${true}"></c:set>
              </c:if>
              <c:set var="canFuture" value="${false}"></c:set>
              <c:if test="${toDo.allowsFuture()==true}">
                <c:set var="canFuture" value="${true}"></c:set>
              </c:if>
              <c:set var="canNonOwner" value="${false}"></c:set>
              <c:if test="${toDo.allowsNonOwner()==true}">
                <c:set var="canNonOwner" value="${true}"></c:set>
              </c:if>
              <c:set var="hasOwner" value="${false}"></c:set>
              <c:if test="${toDo.hasOwner()==true && toDo.getTaskOwner()!=null}">
                <c:set var="hasOwner" value="${true}"></c:set>
              </c:if>
              <c:set var="isSourced" value="${false}"></c:set>
              <c:if test="${toDo.isSourced()==true && toDo.getSourceOwner()!=null}">
                <c:set var="isSourced" value="${true}"></c:set>
              </c:if>
              <c:set var="hasGoto" value="${false}"></c:set>
              <c:if test="${toDo.hasGoto()==true && toDo.getGotoLink()!=null}">
                <c:set var="hasGoto" value="${true}"></c:set>
              </c:if>
              <c:set var="hasInfo" value="${false}"></c:set>
              <c:if test="${toDo.hasInfo()==true && toDo.getInfoLink()!=null}">
                <c:set var="hasInfo" value="${true}"></c:set>
              </c:if>
            </div>

              <%-- ***************************************** CALCULATED ITEMS ********************************************************************* --%>
            <div id="calculated_items_cb2">
              <c:set var="isMyTask" value="${false}"></c:set>
              <c:if test="${(hasOwner==true && toDo.getTaskOwner().getId()==myId) || (isSourced==true && toDo.getSourceOwner().getId()==myId)}">
                <c:set var="isMyTask" value="${true}"></c:set>
              </c:if>
              <c:set var="isDelegated" value="${false}"></c:set>
              <c:if test="${(hasOwner==true && isMyTask==false) || (isSourced==true && toDo.getSourceOwner().getId()!=myId)}">
                <c:set var="isDelegated" value="${true}"></c:set>
              </c:if>
              <c:set var="isTimeBlocked" value="${false}"></c:set>
              <c:if test="${tds.index>0 && (canEarly==false || blockFuture==true)}">
                <c:set var="isTimeBlocked" value="${true}"></c:set>
              </c:if>
              <c:set var="isWhoBlocked" value="${false}"></c:set>
              <c:if test="${isMyTask==false && canNonOwner==false && (hasOwner==true || isSourced==true)}">
                <c:set var="isWhoBlocked" value="${true}"></c:set>
              </c:if>
            </div>

              <%-- ***************************************** WHETHER AUTOMATION CAN BE ACCESSED ********************************************************************* --%>
            <div id="styling_items_cb3">
              <c:set var="btnIcon" value="square"></c:set>
              <c:set var="styl" value=""></c:set>
              <c:set var="peNone" value=""></c:set>
              <c:set var="ital" value=""></c:set>
              <c:set var="formServlet" value="CloseToDo25"></c:set>
              <c:choose>
                <c:when test="${isWhoBlocked==true && isComp==true}">
                  <c:set var="btnIcon" value="x-square-fill"></c:set>
                  <c:set var="styl" value="text-decoration:line-through;"></c:set>
                  <c:set var="ital" value="fst-italic fw-lighter"></c:set>
                  <c:set var="formServlet" value="ReOpenToDo25"></c:set>
                  <c:set var="peNone" value="pe-none"></c:set>
                </c:when>
                <c:when test="${isComp==true}">
                  <c:set var="btnIcon" value="x-square"></c:set>
                  <c:set var="styl" value="text-decoration:line-through;"></c:set>
                  <c:set var="ital" value="fst-italic fw-lighter"></c:set>
                  <c:set var="formServlet" value="ReOpenToDo25"></c:set>
                </c:when>
                <c:when test="${isWhoBlocked==true}">
                  <c:set var="btnIcon" value="person-square"></c:set>
                  <c:set var="peNone" value="pe-none"></c:set>
                </c:when>
                <c:when test="${isTimeBlocked==true}">
                  <c:set var="btnIcon" value="clock-fill"></c:set>
                  <c:set var="peNone" value="pe-none"></c:set>
                </c:when>
                <c:when test="${isDelegated==true}">
                  <c:set var="btnIcon" value="box-arrow-up-left"></c:set>
                </c:when>
                <c:when test="${isMyActivity==false && isMyTask==false}">
                  <c:set var="btnIcon" value="circle"></c:set>
                </c:when>
                <c:otherwise></c:otherwise>
              </c:choose>
              <c:if test="${sessionScope.isPspAdmin==true}">
                <c:set var="peNone" value=""></c:set>
              </c:if>
            </div>

          </div>
          <div class="row m-0 p-0 ">
            <div class="col m-0 p-0">

              <form method="post" action="${formServlet}">
                <div class="input-group input-group-sm p-0 m-0">
                  <button type="submit" class="btn btn-outline-cb border-white border-0 m-0 p-0 ${peNone} ${isPast}" name="btnToDo" id="btn${toDo.getToDo().getId()}" value="${toDo.getToDo().getId()}">
                    <i class="bi bi-${btnIcon}" style="font-size: 1.4rem"></i>
                  </button>
                  <c:choose>
                    <c:when test="${toDo.isComplete()==false && toDo.hasGoto()==true && toDo.getGotoLink()!=null}">
                      <div class="form-control ${ital} border-white border-0">
                        <a href="${toDo.getGotoLink().getLinkPath()}" target="_blank" style="font-size:0.65em">${toDo.getDescription()}</a>
                      </div>
                    </c:when>
                    <c:otherwise>
                      <div class="form-control ${ital} border-white border-0">
                      <span style="font-size: 0.65em; ${styl}">
                          ${toDo.getDescription()}
                      </span>
                      </div>
                    </c:otherwise>
                  </c:choose>
                </div>
              </form>

            </div>
            <c:if test="${toDo.isComplete()==false && toDo.hasInfo()==true && toDo.getInfoLink()!=null}">
              <div class="col-auto m-0 p-0 me-1">
                <a class="btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1" href="${toDo.getInfoLink().getLinkPath()}" target="_blank">
                  <i class="bi bi-question-lg"></i>
                </a>
              </div>
            </c:if>
            <div class="col-auto m-0 p-0">
              <form method="post" action="ManageTask25" id="fm${toDo.getToDo().getId()}">
                <input type="text" name="toDoId" id="tdId${toDo.getToDo().getId()}" value="${toDo.getToDo().getId()}" hidden>
                <button type="submit" class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 ${peNone} mt-1 ${isPast}">
                  <i class="bi bi-tools"></i>
                </button>
              </form>
            </div>
          </div>
          <c:if test="${canFuture==false}">
            <c:set var="blockFuture" value="${true}"></c:set>
          </c:if>
        </c:if>
      </c:forEach>
    </div>
  </div>
</div>
<!-- Auto-save on page unload -->
<form id="autoSaveForm" method="post" action="PersistChecklist25" style="display:none;">
  <input type="hidden" name="autoSave" value="true">
</form>