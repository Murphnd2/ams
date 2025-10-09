<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:import url="/WEB-INF/view/activity/automationForm.jsp"></c:import>
<c:set var="allDone" value=""></c:set>
<div class="container-fluid m-0 p-0">
    <div class="row m-0 p-0">
        <div class="col m-0 p-0">
            <c:set var="blockRemainder" value="N"></c:set>
            <c:forEach var="toDo" items="${sessionScope.currentToDoList}" varStatus="tdId">
                <div id="massCodeBehind" class="d-none">
                    <c:set var="box" value="clock"></c:set>

                    <c:set var="onlyOwner" value="Y"></c:set>
                    <c:if test="${toDo.getTask().allowNonOwner() || (toDo.getTask().getOwner()!=null && toDo.getTask().getOwner().getId()==sessionScope.currentPerson.getId())}">
                        <c:set var="onlyOwner" value="N"></c:set>
                    </c:if>

                    <c:set var="hOwner" value="N"></c:set>
                    <c:if test="${toDo.getTask().hasOwner() && toDo.getTask().allowNonOwner() && (toDo.getTask().getOwner().getId()!=sessionScope.currentPerson.getId())}">
                        <c:set var="hOwner" value="Y"></c:set>
                    </c:if>

                    <c:set var="hSource" value="N"></c:set>
                    <c:if test="${toDo.getTask().isSourced()}">
                        <c:set var="hSource" value="Y"></c:set>
                    </c:if>

                    <c:if test="${onlyOwner.equals(\"Y\")}">
                        <c:set var="box" value="lock"></c:set>
                    </c:if>

                    <c:set var="onlyAtTop" value="Y"></c:set>
                    <c:if test="${tdId.first || toDo.getTask().allowEarly()}">
                        <c:set var="onlyAtTop" value="N"></c:set>
                    </c:if>

                    <c:set var="futureBlocked" value="N"></c:set>
                    <c:if test="${blockRemainder.equals(\"Y\")}">
                        <c:set var="futureBlocked" value="Y"></c:set>
                    </c:if>

                    <c:if test="${toDo.getTask().allowFuture()==false && !toDo.isComplete() && !toDo.getTask().getDescription().equals(\"Default\")}">
                        <c:set var="blockRemainder" value="Y"></c:set>
                    </c:if>

                    <c:set var="enableIt" value="Y"></c:set>
                    <c:if test="${onlyOwner.equals(\"Y\") || onlyAtTop.equals(\"Y\") || futureBlocked.equals(\"Y\")}">
                        <c:set var="enableIt" value="N"></c:set>
                    </c:if>

                    <c:set var="ref" value=""></c:set>
                    <c:if test="${toDo.isComplete() || !toDo.getTask().hasInfo() || toDo.getTask().getInfoLink()==null || (!toDo.getTask().allowNonOwner() && toDo.getTask().getOwner().getId()!=sessionScope.currentPerson.getId())}">
                        <c:set var="ref" value="style=\"visibility: hidden\""></c:set>
                    </c:if>


                    <c:set var="ital" value=""></c:set>
                    <c:set var="styl" value=""></c:set>
                    <c:if test="${toDo.isComplete()}">
                        <c:set var="ital" value="fst-italic fw-lighter"></c:set>
                        <c:set var="styl" value="style=\"text-decoration:line-through\""></c:set>
                    </c:if>
                    <c:set var="io" value="0"></c:set>
                    <c:if test="${toDo.getTask().getOwner()!=null && toDo.getTask().getOwner().getId()==sessionScope.currentPerson.getId()}">
                        <c:set var="io" value="1"></c:set>
                    </c:if>
                </div>

                <c:if test="${!toDo.getTask().getDescription().equals(\"Default\")}">
                    <div class="row m-0 p-0">
                        <div class="col m-0 p-0">
                            <form method="post" action="${toDo.getFormServlet()}" class="m-0 d-none d-sm-grid"  >
                                <div class="input-group input-group-sm p-0 m-0">
                                        ${toDo.getToDoButton(enableIt,toDo.getId(),box,hOwner,hSource)}
                                    <div class="form-control ${ital} border-white border-0" ${styl}>${toDo.getWebDescription(onlyOwner,"0.65rem",io)}
                                    </div>
                                </div>
                            </form>
                        </div>
                        <div class="col-auto m-0 p-0 me-1" ${ref}>
                            <a class="btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1" href="${toDo.getTask().getInfoLink().getLinkPath()}" target="_blank">
                                <i class="bi bi-question-lg"></i>
                            </a>
                        </div>
                        <div class="col-auto m-0 p-0">
                            <c:set var="modId" value="mod${toDo.getId()}"></c:set>
                            <c:set var="modId2" value="mod2${toDo.getId()}"></c:set>
                            <!-- Button trigger modal -->
                            <button type="button" class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 mt-1" data-bs-toggle="modal" data-bs-target="#${modId}">
                                <i class="bi bi-tools"></i>
                            </button>

                            <!-- Modal -->
                            <div class="modal fade" id="${modId}" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">

                                <div class="modal-dialog modal-lg">
                                    <div class="modal-content">
                                        <form method="post" action="UpdateAutomation">
                                            <div class="modal-header">
                                                <h5 class="modal-title" id="exampleModalLabel">Control ${toDo.getTask().getDescription()}</h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <div class="row mt-1">
                                                    <div class="col-1"><hr></div>
                                                    <div class="col-auto fw-bolder text-info">WHEN TASK CAN BE PERFORMED</div>
                                                    <div class="col"><hr></div>
                                                </div>
                                                <div id="Code_Behind_for_row_1" class="d-none">
                                                    <c:set var="aEarly" value=""></c:set>
                                                    <c:set var="earlyText" value="Task can be done <b><i><span style=\"color:red\">only at the top</span></i></b> of your ToDo list."></c:set>
                                                    <c:set var="cb1" value="text-secondary fw-lighter"></c:set>
                                                    <c:if test="${toDo.getTask().allowEarly()}">
                                                        <c:set var="aEarly" value="checked"></c:set>
                                                        <c:set var="earlyText" value="Task can be done at <b><u>anytime</u></b>."></c:set>
                                                        <c:set var="cb1" value=""></c:set>
                                                    </c:if>
                                                </div>
                                                <div class="row">
                                                    <div class="col">
                                                        <div class="form-check" >
                                                            <input class="form-check-input" type="checkbox" value="1"  id="cb1${toDo.getId()}" name="allowEarly" ${aEarly} onchange="updateEarly(${toDo.getId()})">
                                                            <label class="form-check-label ${cb1}" for="cb1${toDo.getId()}">
                                                                Check if Task can be done at any time.
                                                            </label>
                                                        </div>
                                                    </div>
                                                    <div class="col m-0 p-0">

                                                        <div class="row">
                                                            <div class="col" id="item1${toDo.getId()}">
                                                                    ${earlyText}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>

                                                <div class="row mt-3">
                                                    <div class="col-1"><hr></div>
                                                    <div class="col-auto fw-bolder text-success">IF TASK AFFECTS SUBSEQUENT TASKS</div>
                                                    <div class="col"><hr></div>
                                                </div>
                                                <div id="Code_Behind_for_row_2" class="d-none">
                                                    <c:set var="aFuture" value=""></c:set>
                                                    <c:set var="futureText" value="Task <span style=\"color:red\"><b><i>prevents completion of subsequent tasks</i></b></span> until it is completed."></c:set>
                                                    <c:if test="${toDo.getTask().allowFuture()}">
                                                        <c:set var="aFuture" value="checked"></c:set>
                                                        <c:set var="futureText" value="Task <b><u>does not affect</u></b> subsequent tasks."></c:set>
                                                    </c:if>
                                                </div>
                                                <div class="row">
                                                    <div class="col">
                                                        <div class="form-check">
                                                            <input class="form-check-input" type="checkbox" value="1" id="cb2${toDo.getId()}" name="allowFuture" ${aFuture} onchange="updateFuture(${toDo.getId()})">
                                                            <label class="form-check-label" for="cb2${toDo.getId()}">
                                                                Check if this task's status does not affect subsequent tasks.
                                                            </label>
                                                        </div>
                                                    </div>
                                                    <div class="col m-0 p-0">

                                                        <div class="row">
                                                            <div class="col" id="item2${toDo.getId()}">
                                                                    ${futureText}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>

                                                <!-- ***** DESIGNATED EMPLOYEE ROW ********************************************** -->
                                                <div class="row mt-3">
                                                    <div class="col-1"><hr></div>
                                                    <div class="col-auto fw-bolder text-info">DESIGNATE TASK TO A SPECIFIC EMPLOYEE</div>
                                                    <div class="col"><hr></div>
                                                </div>
                                                <div id="Code_Behind_For_DESIGNATED_EMPLOYEE" class="d-none">
                                                    <c:set var="aOwner" value=""></c:set>
                                                    <c:set var="eOnly" value=""></c:set>
                                                    <c:set var="aStyle" value="d-none"></c:set>
                                                    <c:set var="eStyle" value="d-none"></c:set>
                                                    <c:set var="ownerText" value="Task can be done by <b><u>anyone</u></b>"></c:set>
                                                    <c:choose>
                                                        <c:when test="${toDo.getTask().hasOwner() && !toDo.getTask().allowNonOwner()}">
                                                            <c:set var="ownerText" value="Task <b>is assigned</b> and <span style=\"color:red\"><b><i>must be done</i></b></span> by that employee."></c:set>
                                                            <c:set var="eOnly" value="checked"></c:set>
                                                            <c:set var="aOwner" value="checked"></c:set>
                                                            <c:set var="aStyle" value="mt-1"></c:set>
                                                            <c:set var="eStyle" value="mt-1"></c:set>
                                                        </c:when>
                                                        <c:when test="${toDo.getTask().hasOwner()}">
                                                            <c:set var="ownerText" value="Task <b>is assigned</b> but still can be <b><i>done by anyone</i></b>"></c:set>
                                                            <c:set var="aOwner" value="checked"></c:set>
                                                            <c:set var="aStyle" value="mt-1"></c:set>
                                                            <c:set var="eStyle" value="mt-1"></c:set>
                                                        </c:when>
                                                        <c:otherwise></c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <div class="row" id="ownerRowA${toDo.getId()}" style="visibility: visible">
                                                    <div class="col me-2">
                                                        <div class="row m-0 p-0">
                                                            <div class="col m-0 p-0">
                                                                <div class="form-check">
                                                                    <input class="form-check-input" ${aOwner} type="checkbox" value="1"  id="hasOwner${toDo.getId()}" name="hasOwner" onchange="showHideOwners(${toDo.getId()})" >
                                                                    <label class="form-check-label" for="hasOwner${toDo.getId()}">
                                                                        Check to assign task to an employee.
                                                                    </label>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="row ${aStyle}" id="ownerRowB${toDo.getId()}" >
                                                            <div class="col">
                                                                <div class="form-check">
                                                                    <input class="form-check-input" type="checkbox" value="" disabled hidden >
                                                                    <select class="form-select form-select-sm" name="ownerId" id="ownerId${toDo.getId()}">
                                                                        <c:forEach var="user" items="${sessionScope.pspUserList}">
                                                                            <c:set var="uSelect" value=""></c:set>
                                                                            <c:if test="${toDo.getTask().hasOwner() && toDo.getTask().getOwner().getId()!=null && toDo.getTask().getOwner().getId()==user.getId()}">
                                                                                <c:set var="uSelect" value="selected"></c:set>
                                                                            </c:if>
                                                                            <option value="${user.getId()}" ${uSelect} class="form-control">${user.getLastName().toUpperCase()}, ${user.getFirstName().toUpperCase()}</option>
                                                                        </c:forEach>
                                                                    </select>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="row ${eStyle}" id="onlyOwnerRow${toDo.getId()}">
                                                            <div class="col">
                                                                <div class="form-check">
                                                                    <input class="form-check-input" ${eOnly} type="checkbox" value="1" id="onlyOwner${toDo.getId()}" name="onlyOwner" onchange="showHideOwners(${toDo.getId()})">
                                                                    <label class="form-check-label text-primary" for="onlyOwner${toDo.getId()}">
                                                                        Check if <b><i>only</i></b> this employee can complete task.
                                                                    </label>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="col m-0 p-0">

                                                        <div class="row">
                                                            <div class="col" id="item3${toDo.getId()}">
                                                                ${ownerText}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>

                                                <!-- ***** OUTSOURCED ITEM ROW ********************************************** -->
                                                <div class="row mt-3">
                                                    <div class="col-1"><hr></div>
                                                    <div class="col-auto fw-bolder text-success">DESIGNATE TASK TO OUTSIDE VENDOR</div>
                                                    <div class="col"><hr></div>
                                                </div>
                                                <div id="Code_Behind_For_OUTSOURCED_TASK" class="d-none">
                                                    <c:set var="aOut" value=""></c:set>
                                                    <c:set var="eOnly2" value=""></c:set>
                                                    <c:set var="oStyle" value="d-none"></c:set>
                                                    <c:set var="sourceText" value="The task is <u>not sourced</u> to an outside vendor."></c:set>
                                                    <c:choose>
                                                        <c:when test="${toDo.getTask().isSourced() && !toDo.getTask().allowNonOwner()}">
                                                            <c:set var="aOut" value="checked"></c:set>
                                                            <c:set var="oStyle" value="mt-1"></c:set>
                                                            <c:set var="sourceText" value="<span style=\"color:red\">This task can <b>ONLY BE COMPLETED BY THE VENDOR</b>.</span>"></c:set>
                                                            <c:set var="eOnly2" value="checked"></c:set>
                                                        </c:when>
                                                        <c:when test="${toDo.getTask().isSourced()}">
                                                            <c:set var="aOut" value="checked"></c:set>
                                                            <c:set var="oStyle" value="mt-1"></c:set>
                                                            <c:set var="sourceText" value="The task will be <b>shown to the outside vendor</b> and they will be permitted to flag it completed if necessary."></c:set>
                                                        </c:when>
                                                        <c:otherwise></c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <div class="row">
                                                    <div class="col">
                                                        <div class="row m-0 p-0">
                                                            <div class="col m-0 p-0">
                                                                <div class="form-check">
                                                                    <input class="form-check-input" ${aOut} type="checkbox" ${aOut} id="isSourced${toDo.getId()}" value="1" name="isSourced" onchange="showHideSourcing(${toDo.getId()})" >
                                                                    <label class="form-check-label" for="isSourced${toDo.getId()}">
                                                                        Check if this task is to be sourced to an <b>OUTSIDE VENDOR</b>.
                                                                    </label>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="row ${oStyle}" id="sourceRowB${toDo.getId()}" >
                                                            <div class="col">
                                                                <div class="form-check">
                                                                    <input class="form-check-input" type="checkbox" value="" disabled hidden >
                                                                    <select class="form-select form-select-sm" id="sourceId${toDo.getId()}" name="sourceId">
                                                                        <c:forEach var="bpo" items="${sessionScope.bpoUserList}">
                                                                            <c:set var="bSelect" value=""></c:set>
                                                                            <c:if test="${toDo.getTask().isSourced() && toDo.getTask().getOwner().getId()!=null && toDo.getTask().getOwner().getId()==bpo.getId()}">
                                                                                <c:set var="bSelect" value="selected"></c:set>
                                                                            </c:if>
                                                                            <option value="${bpo.getId()}" ${bSelect} class="form-control">${bpo.getFirstName().toUpperCase()} ${bpo.getLastName().toUpperCase()}</option>
                                                                        </c:forEach>
                                                                    </select>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div class="row ${oStyle}" id="onlySourceRow${toDo.getId()}" >
                                                            <div class="col">
                                                                <div class="form-check">
                                                                    <input class="form-check-input" type="checkbox" ${eOnly2} value="1" id="onlySource${toDo.getId()}" name="onlyOwner2" onchange="showHideSourcing(${toDo.getId()})">
                                                                    <label class="form-check-label text-primary" for="onlySource${toDo.getId()}">
                                                                        Check if <b>ONLY THIS VENDOR</b> can complete this task.
                                                                    </label>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div class="col m-0 p-0">
                                                        <div class="row">
                                                            <div class="col fw-bold text-decoration-underline">
                                                                STATUS
                                                            </div>
                                                        </div>
                                                        <div class="row">
                                                            <div class="col" id="item4${toDo.getId()}">
                                                                    ${sourceText}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>

                                                <!-- ***** WHERE TO DO THE WORK ********************************************** -->
                                                <div class="row mt-3">
                                                    <div class="col"><hr></div>
                                                    <div class="col-auto fw-bolder text-info">WEBSITE WHERE YOU PERFORM THIS TASK</div>
                                                    <div class="col"><hr></div>
                                                </div>
                                                <div id="Code_Behind_For_Goto" class="d-none">
                                                    <c:set var="gtChecked" value=""></c:set>
                                                    <c:set var="gtStyle" value="d-none"></c:set>
                                                    <c:if test="${toDo.getTask().hasGoTo() && toDo.getTask().getGoToLink()!=null}">
                                                        <c:set var="gtChecked" value="checked"></c:set>
                                                        <c:set var="gtStyle" value="mt-1"></c:set>
                                                    </c:if>
                                                </div>
                                                <div class="row mt-3">
                                                    <div class="col">
                                                        <div class="form-check">
                                                            <input class="form-check-input" type="checkbox" ${gtChecked} value="1" id="hasGoTo${toDo.getId()}" name="hasGoTo" onchange="showHideGoTo(${toDo.getId()})"  >
                                                            <label class="form-check-label" for="hasGoTo${toDo.getId()}">
                                                                Is there a <b>website</b> where the task should be performed?
                                                            </label>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="row ${gtStyle}" id="goToRowB${toDo.getId()}">
                                                    <div class="col">
                                                        <div class="form-check">
                                                            <input class="form-check-input" type="checkbox" value="" disabled hidden >
                                                            <input type="url" class="form-control form-control-sm" name="goToPath" id="goToPath${toDo.getId()}" value="${toDo.getTask().getGoToLink().getLinkPath()}">
                                                        </div>
                                                    </div>
                                                </div>

                                                <!-- ***** WHERE TO DO GO FOR HELP ********************************************** -->
                                                <div class="row mt-3">

                                                    <div class="col"><hr></div>
                                                    <div class="col-auto fw-bolder text-success">HELP INSTRUCTIONS</div>
                                                    <div class="col"><hr></div>
                                                </div>
                                                <div id="Code_Behind_For_Info" class="d-none">
                                                    <c:set var="infoChecked" value=""></c:set>
                                                    <c:set var="infoStyle" value="d-none"></c:set>
                                                    <c:if test="${toDo.getTask().hasInfo() && toDo.getTask().getInfoLink()!=null}">
                                                        <c:set var="infoChecked" value="checked"></c:set>
                                                        <c:set var="infoStyle" value="mt-1"></c:set>
                                                    </c:if>
                                                </div>
                                                <div class="row mt-3">
                                                    <div class="col">
                                                        <div class="form-check">
                                                            <input class="form-check-input" type="checkbox" value="1" id="hasInfo${toDo.getId()}" name="hasInfo" onchange="showHideInfo(${toDo.getId()})" ${infoChecked} >
                                                            <label class="form-check-label" for="hasInfo${toDo.getId()}">
                                                                Have you created a <b>reference and/or instructions</b> for this task?
                                                            </label>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="row ${infoStyle}" id="infoRowB${toDo.getId()}">
                                                    <div class="col">
                                                        <div class="form-check">
                                                            <input class="form-check-input" type="checkbox" value="" disabled hidden >
                                                            <input type="url" class="form-control form-control-sm" name="infoPath" id="infoPath${toDo.getId()}" value="${toDo.getTask().getInfoLink().getLinkPath()}">
                                                        </div>
                                                    </div>
                                                </div>


                                                <div class="row mt-1">
                                                    <div class="col w-100">
                                                        <button type="button" class="btn btn-outline-ssa w-100" data-bs-toggle="modal" data-bs-target="#${modId2}">
                                                            <i class="bi bi-robot"></i> Manage Automated Email Content
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-outline-altSsa" data-bs-dismiss="modal">
                                                    <i class="bi bi-x"></i>
                                                    Close
                                                </button>
                                                <button type="submit" name="btnAuto1" value="${toDo.getId()}" class="btn btn-ssa">
                                                    <i class="bi bi-save"></i>
                                                    Save Changes
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>



                            <!-- Modal -->
                            <div class="modal fade" id="${modId2}" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="staticBackdropLabel" aria-hidden="true">
                                <div class="modal-dialog modal-xl">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <h5 class="modal-title" id="staticBackdropLabel">Build / Edit Automation</h5>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <div class="modal-body">
                                            <div class="container-fluid">
                                                <div class="row">
                                                    <div class="col">
                                                        <input type="text" class="form-control" id="autoName${toDo.getId()}" name="autoName" value="${toDo.getTask().getAutomationText()}">
                                                        <label for="autoName${toDo.getId()}" class="form-label">Automation Name</label>
                                                    </div>
                                                </div>
                                                <div class="row">
                                                    <div class="col">
                                                        <textarea class="form-control" id="autoText${toDo.getId()}" name="autoText" rows="15">
                                                                ${toDo.getTask().getAutomation().getHtmlContent().trim()}
                                                        </textarea>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="modal-footer">
                                            <button type="button" class="btn btn-secondary" data-bs-toggle="modal" data-bs-target="#${modId}">Back</button>
                                            <button type="button" class="btn btn-primary">Save/Update</button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
            <script>
                function showHideInfo(toDoId){
                    let controlId = "hasInfo" + toDoId;
                    let inputId = "infoRowB" + toDoId;
                    let inputId2 = "infoPath" + toDoId;
                    let hasGoTo = document.getElementById(controlId);
                    let inputRow = document.getElementById(inputId);
                    let inputBox = document.getElementById(inputId2);
                    if(hasGoTo.checked){
                        inputRow.className="row mt-1";
                        inputBox.required=true;
                    } else{
                        inputRow.className="row d-none";
                        inputBox.required=false;
                    }
                }

                function showHideGoTo(toDoId){
                    let controlId = "hasGoTo" + toDoId;
                    let inputId = "goToRowB" + toDoId;
                    let inputId2 = "goToPath" + toDoId;
                    let hasGoTo = document.getElementById(controlId);
                    let inputRow = document.getElementById(inputId);
                    let inputBox = document.getElementById(inputId2);
                    if(hasGoTo.checked){
                        inputRow.className="row mt-1";
                        inputBox.required=true;
                    } else{
                        inputRow.className="row d-none";
                        inputBox.required=false;
                    }

                }

                function updateFuture(toDoId){
                    let status = "item2" + toDoId;
                    let checkBoxId = "cb2" + toDoId;
                    let statusRow = document.getElementById(status);
                    let checkBox = document.getElementById(checkBoxId);
                    if(checkBox.checked){
                        statusRow.innerHTML = "Task <b><u>does not affect</u></b> subsequent tasks.";
                    } else {
                        statusRow.innerHTML = "Task <span style=\"color:red\"><b><i>prevents completion of subsequent tasks</i></b></span> until it is completed.";
                    }
                }

                function updateEarly(toDoId){
                    let status = "item1" + toDoId;
                    let checkBoxId = "cb1" + toDoId;
                    let statusRow = document.getElementById(status);
                    let checkBox = document.getElementById(checkBoxId);
                    if(checkBox.checked){
                        statusRow.innerHTML = "Task can be done at <b><u>anytime</u></b>.";
                    } else {
                        statusRow.innerHTML = "Task can be done <b><i><span style=\"color:red\">only at the top</span></i></b> of your ToDo list.";
                    }

                }

                function showHideOwners(toDoId){
                    let controlId = "hasOwner" + toDoId;
                    let dropDownId = "ownerRowB" + toDoId;
                    let onlyOwnerId = "onlyOwner" + toDoId;
                    let onlyOwnerRowId = "onlyOwnerRow" + toDoId;
                    let status = "item3" + toDoId;
                    let status2 = "item4" + toDoId;
                    let controlId2 = "isSourced" + toDoId;
                    let sourceRow2 = "sourceRowB" + toDoId;
                    let onlySourceRowId = "onlySourceRow" + toDoId;
                    let onlySource = "onlySource" + toDoId;

                    let onlyOwnerCheck = document.getElementById(onlyOwnerId);
                    let onlyOwnerRow = document.getElementById(onlyOwnerRowId);
                    let hasOwner = document.getElementById(controlId);
                    let ownerDropDown = document.getElementById(dropDownId);
                    let statusRow = document.getElementById(status);
                    let ownerCheckBox = document.getElementById(controlId2);
                    let sourceDropDown = document.getElementById(sourceRow2);
                    let onlySourceRow = document.getElementById(onlySourceRowId);
                    let statusRow2 = document.getElementById(status2);
                    let onlySourceCheck = document.getElementById(onlySource);

                    if(hasOwner.checked===true){
                       ownerDropDown.className = "row mt-2";
                       onlyOwnerRow.className= "row mt-1";
                       ownerCheckBox.checked = false;
                       sourceDropDown.className = "row d-none";
                       onlySourceRow.className = "row d-none";
                       onlySourceCheck.checked = false;

                    } else {
                        ownerDropDown.className= "row d-none";
                        onlyOwnerRow.className = "row d-none";
                        onlyOwnerCheck.checked = false;

                    }
                    if(hasOwner.checked===true && onlyOwnerCheck.checked===true){
                        statusRow.innerHTML = "Task <b>is assigned</b> and <span style=\"color:red\"><b><i>must be done</i></b></span> by that employee.";
                        statusRow2.innerHTML = "This task is not sourced to an outside vendor";
                    } else if(hasOwner.checked===true){
                        statusRow.innerHTML = "Task <b>is assigned</b> but still can be <b><i>done by anyone</i></b>";
                        statusRow2.innerHTML = "This task is not sourced to an outside vendor";
                    } else {
                        statusRow.innerHTML = "Task can be done by <b><u>anyone</u></b>";
                    }

                }

                function showHideSourcing(toDoId){
                    let controlId = "isSourced" + toDoId;
                    let controlId2 = "hasOwner" + toDoId;
                    let dropDownId = "sourceRowB" + toDoId;
                    let checkBoxId = "ownerRowA" + toDoId;
                    let dropDownId2 = "ownerRowB" + toDoId;
                    let onlyOwnerId = "onlyOwner" + toDoId;
                    let onlyOwnerRowId = "onlyOwnerRow" + toDoId;
                    let sourceTextId = "item4" + toDoId;
                    let onlySourceRowId = "onlySourceRow" + toDoId;
                    let onlySource = "onlySource" + toDoId;

                    let status = "item3" + toDoId;

                    let onlyOwnerCheck = document.getElementById(onlyOwnerId);
                    let onlyOwnerRow = document.getElementById(onlyOwnerRowId);
                    let isSourced = document.getElementById(controlId);
                    let sourceDropDown = document.getElementById(dropDownId);
                    let ownerCheckBox = document.getElementById(controlId2);
                    let ownerCheckBoxRow = document.getElementById(checkBoxId);
                    let ownerDropDownRow = document.getElementById(dropDownId2);
                    let onlySourceCheck = document.getElementById(onlySource);
                    let onlySourceRow = document.getElementById(onlySourceRowId);
                    let sourceText = document.getElementById(sourceTextId);
                    let statusRow = document.getElementById(status);

                    if(isSourced.checked===true){
                        sourceDropDown.className="row mt-1";
                        ownerCheckBox.checked = false;
                        ownerDropDownRow.className="row d-none";
                        onlyOwnerRow.className = "row d-none";
                        onlyOwnerCheck.checked = false;
                        onlySourceCheck.style.visibility = 'visible';
                        onlySourceRow.className="row mt-1";

                    } else {
                        sourceDropDown.className="row d-none";
                        ownerCheckBoxRow.style.visibility = 'visible';
                        onlyOwnerCheck.checked = false;
                        onlySourceCheck.style.visibility = 'hidden';
                        onlySourceRow.className="row d-none";
                        onlySourceCheck.checked=false;
                    }
                    if(isSourced.checked===true && onlySourceCheck.checked===true){
                        sourceText.innerHTML = "<span style=\"color:red\">This task can <b>ONLY BE COMPLETED BY THE VENDOR</b>.";
                        statusRow.innerHTML = "This task <b><i>is sourced</i></b> and <span style=\"color:red\"><b>can't be done by employees.</b></span>";
                    } else if(isSourced.checked===true){
                        sourceText.innerHTML = "The task will be <b>shown to the outside vendor</b> and they will be permitted to flag it completed if necessary.";
                        statusRow.innerHTML = "This task <b><i>is sourced</i></b> but can be completed by <b><u>anyone</u></b>.";
                    } else {
                        sourceText.innerHTML = "The task is not sourced to an outside vendor."
                        statusRow.innerHTML = "Task can be done by <b><u>anyone</u></b>";
                    }
                }
            </script>
        </div>
    </div>
    <form method="post" action="CloseSingleItemChecklist">
        <div class="row mt-1">
            <div class="col-6">
                <button class="btn btn-sm btn-success w-100" type="button" data-bs-toggle="modal" data-bs-target="#addToDoModal">
                    <i class="bi bi-card-checklist"></i> Add Task
                </button>
            </div>
            <div class="col-6">
                <c:choose>
                    <c:when test="${sessionScope.adminView==1}">
                        <button type="button" class="btn btn-sm btn-secondary w-100" name="btnCheckList" data-bs-target="#closeActivity" data-bs-toggle="modal" ${allDone}>
                            <i class="bi bi-door-open"></i> Close Setup
                        </button>
                    </c:when>
                    <c:when test="${sessionScope.adminView==2}">
                        <button type="button" class="btn btn-sm btn-primary w-100" data-bs-target="#closeActivity" data-bs-toggle="modal" name="btnCheckList" ${allDone}>
                            <i class="bi bi-door-open"></i> Close Renewal
                        </button>
                    </c:when>
                    <c:when test="${sessionScope.adminView==3}">
                        <button type="button" class="btn btn-sm btn-info w-100" data-bs-target="#closeActivity" data-bs-toggle="modal" ${allDone} value="${sessionScope.currentToDoList.get(0).getCheckList().getId()}">
                            <i class="bi bi-door-open"></i> Close Ticket
                        </button>
                    </c:when>
                    <c:otherwise>
                        <button type="button" class="btn btn-sm btn-warning w-100" data-bs-target="#closeActivity" data-bs-toggle="modal"  ${allDone} value="${sessionScope.currentToDoList.get(0).getCheckList().getId()}">
                            <i class="bi bi-door-open"></i> Close Checklist
                        </button>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <c:import url="/WEB-INF/view/activity/closeActivityModal.jsp"></c:import>
    </form>
    <c:import url="/WEB-INF/view/activity/checklist/addToDoToChecklistModal.jsp"></c:import>
</div>