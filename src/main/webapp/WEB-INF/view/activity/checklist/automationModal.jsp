<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="modId" value="mod${toDo.getId()}"></c:set>
<!-- Button trigger modal -->
<button type="button" class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 mt-1" data-bs-toggle="modal" data-bs-target="#${modId}">
  <i class="bi bi-tools"></i>
</button>

<!-- Modal -->
<div class="modal fade" id="${modId}" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="exampleModalLabel">Control ${toDo.getTask().getDescription()}</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form method="post" action="UpdateAutomation">
          <c:set var="aEarly" value=""></c:set>
          <%--@elvariable id="toDo" type=""--%>
          <c:if test="${toDo.getTask().allowEarly()}">
            <c:set var="aEarly" value="checked"></c:set>
          </c:if>
          <div class="row">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="1" id="allowEarly" name="allowEarly" ${aEarly}>
                <label class="form-check-label" for="allowEarly">
                  Task Can Be Done Early?
                </label>
              </div>
            </div>
          </div>
          <c:set var="aFuture" value=""></c:set>
          <%--@elvariable id="toDo" type=""--%>
          <c:if test="${toDo.getTask().allowFuture()}">
            <c:set var="aFuture" value="checked"></c:set>
          </c:if>
          <div class="row mt-3">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="1" id="allowFuture" name="allowFuture" ${aFuture}>
                <label class="form-check-label" for="allowFuture">
                  Allow Items in List Beyond This Task To Be Done?
                </label>
              </div>
            </div>
          </div>
          <div class="row mt-3" id="ownerRowA${toDo.getId()}" style="visibility: visible">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="${toDo.getId()}" id="hasOwner${toDo.getId()}" name="hasOwner" onchange="showHideOwners(${toDo.getId()})" >
                <label class="form-check-label" for="hasOwner${toDo.getId()}">
                  Does This Task Have an Owner?
                </label>
              </div>
            </div>
          </div>
          <div class="row mt-1" id="ownerRowB${toDo.getId()}" style="visibility: hidden">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" disabled hidden >
                <select class="form-select form-select-sm" name="ownerId" id="ownerId${toDo.getId()}">
                  <c:forEach var="user" items="${sessionScope.pspUserList}">
                    <c:set var="uSelect" value=""></c:set>
                    <%--@elvariable id="toDo" type=""--%>
                    <jsp:useBean id="toDo" scope="request" type=""/>
                    <%--@elvariable id="toDo" type=""--%>
                    <c:if test="${toDo.getTask().hasOwner() && toDo.getTask().getOwner().getId()!=null && toDo.getTask().getOwner().getId()==user.getId()}">
                      <c:set var="uSelect" value="selected"></c:set>
                    </c:if>
                    <option value="${user.getId()}" ${uSelect} class="form-control">${user.getLastName().toUpperCase()}, ${user.getFirstName().toUpperCase()}</option>
                  </c:forEach>
                </select>
              </div>
            </div>
          </div>
          <div class="row mt-3">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" id="isSourced${toDo.getId()}" name="isSourced" onchange="showHideSourcing(${toDo.getId()})" >
                <label class="form-check-label" for="isSourced${toDo.getId()}">
                  Will This Task Be Outsourced?
                </label>
              </div>
            </div>
          </div>
          <div class="row mt-1" id="sourceRowB${toDo.getId()}" style="visibility: hidden;">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" disabled hidden >
                <select class="form-select form-select-sm" name="sourceId${toDo.getId()}">
                  <c:forEach var="bpo" items="${sessionScope.bpoUserList}">
                    <c:set var="bSelect" value=""></c:set>
                    <%--@elvariable id="toDo" type=""--%>
                    <jsp:useBean id="toDo" scope="request" type=""/>
                    <jsp:useBean id="toDo" scope="request" type=""/>
                    <%--@elvariable id="toDo" type=""--%>
                    <jsp:useBean id="toDo" scope="request" type=""/>
                    <jsp:useBean id="toDo" scope="request" type=""/>
                    <c:if test="${toDo.getTask().isSourced() && toDo.getTask().getOwner().getId()!=null && toDo.getTask().getOwner().getId()==bpo.getId()}">
                      <c:set var="bSelect" value="selected"></c:set>
                    </c:if>
                    <option value="${bpo.getId()}" ${bSelect} class="form-control">${bpo.getFirstName().toUpperCase()} ${bpo.getLastName().toUpperCase()}</option>
                  </c:forEach>
                </select>
              </div>
            </div>
          </div>
          <div class="row mt-3" id="onlyOwnerRow${toDo.getId()}" style="visibility: hidden;">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" id="onlyOwner${toDo.getId()}" name="onlyOwner">
                <label class="form-check-label" for="onlyOwner${toDo.getId()}">
                  Can Only the Task Owner Complete the Task?
                </label>
              </div>
            </div>
          </div>
          <c:set var="gtChecked" value=""></c:set>
          <c:set var="gtStyle" value="style=\"visibility: hidden\""></c:set>
          <%--@elvariable id="toDo" type=""--%>
          <jsp:useBean id="toDo" scope="request" type=""/>
          <%--@elvariable id="toDo" type=""--%>
          <jsp:useBean id="toDo" scope="request" type=""/>
          <c:if test="${toDo.getTask().hasGoTo() && toDo.getTask().getGoToLink()!=null}">
            <c:set var="gtChecked" value="checked"></c:set>
            <c:set var="gtStyle" value=""></c:set>
          </c:if>
          <div class="row mt-3">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" ${gtChecked} value="${toDo.getTask().getGoToLink().getLinkPath()}" id="hasGoTo${toDo.getId()}" name="hasGoTo" onchange="showHideGoTo(${toDo.getId()})"  >
                <label class="form-check-label" for="hasGoTo${toDo.getId()}">
                  Provide a URL to Website for Task?
                </label>
              </div>
            </div>
          </div>

          <div class="row mt-1" id="goToRowB${toDo.getId()}" ${gtStyle}>
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" disabled hidden >
                <input type="url" class="form-control form-control-sm" name="goToPath" id="goToPath${toDo.getId()}" value="${toDo.getTask().getGoToLink().getLinkPath()}">
              </div>
            </div>
          </div>

          <c:set var="infoChecked" value=""></c:set>
          <c:set var="infoStyle" value="style=\"visibility: hidden\""></c:set>
          <%--@elvariable id="toDo" type=""--%>
          <jsp:useBean id="toDo" scope="request" type=""/>
          <%--@elvariable id="toDo" type=""--%>
          <jsp:useBean id="toDo" scope="request" type=""/>
          <c:if test="${toDo.getTask().hasInfo() && toDo.getTask().getInfoLink()!=null}">
            <c:set var="infoChecked" value="checked"></c:set>
            <c:set var="infoStyle" value=""></c:set>
          </c:if>
          <div class="row mt-3">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" id="hasInfo${toDo.getId()}" name="hasInfo" onchange="showHideInfo(${toDo.getId()})" ${infoChecked} >
                <label class="form-check-label" for="hasInfo${toDo.getId()}">
                  Is there a Help Website/Document?
                </label>
              </div>
            </div>
          </div>
          <div class="row mt-1" id="infoRowB${toDo.getId()}" ${infoStyle}>
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" disabled hidden >
                <input class="form-control form-control-sm" name="infoPath" id="infoPath${toDo.getId()}">
              </div>
            </div>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
        <button type="button" class="btn btn-primary">Save changes</button>
      </div>
    </div>
  </div>
</div>
