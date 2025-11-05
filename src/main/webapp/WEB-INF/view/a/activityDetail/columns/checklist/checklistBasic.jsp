<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container-fluid m-0 p-0">
  <div class="row m-0 p-0 overflow-auto" style="max-height:575px">
    <div class="col m-0 p-0">
      <c:forEach var="toDo" items="${sessionScope.sVar.getCurrentActivityToDos()}">
        <div class="row m-0 p-0 ">
          <div class="col m-0 p-0">
            <form method="post" action="${toDo.getFormServlet()}">
              <div class="input-group input-group-sm p-0 m-0">
                <%-- INSERT BUTTON HTML REFERENCE--%>
                ${toDo.getToDoButton(sessionScope.sVar.getCurrentPerson(),sessionScope.sVar.getCurrentActivity().getAssignedTo())}
                <div class="form-control border-white border-0">
                    ${toDo.getDescriptionLink(sessionScope.sVar.getCurrentPerson(),"0.65em")}
                </div>
              </div>
            </form>
          </div>
          <div class="col-auto m-0 p-0 me-1">
            ${toDo.getHelpButton(sessionScope.sVar.getCurrentPerson())}
          </div>
          <div class="col-auto m-0 p-0">
            ${toDo.getManageButton(sessionScope.sVar.getCurrentPerson(),sessionScope.isPspAdmin)}
          </div>
        </div>
      </c:forEach>
    </div>
  </div>
</div>