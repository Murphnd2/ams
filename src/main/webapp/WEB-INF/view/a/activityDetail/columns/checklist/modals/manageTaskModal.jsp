<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:url var="jsPath" value="/WEB-INF/css/toDoListScripts.js"></c:url>
<script type="text/javascript" src="${jsPath}"></script>
<c:set var="modId" value="mod${toDo.getToDo().getId()}"></c:set>
<c:set var="modId2" value="mod2${toDo.getToDo().getId()}"></c:set>
<div class="modal fade" id="${modId}" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
        <div class="modal-content">
            <form method="post" action="UpdateAutomation">
                <div class="modal-header">
                    <div class="container-fluid">
                        <div class="row">
                            <h3 class="col">
                                MANAGE <span class="text-primary fst-italic">${toDo.getDescription()} <i class="bi bi-check-square"></i></span>
                            </h3>
                            <div class="col-auto">
                                <button type="button" class="btn-close" data-bs-toggle="modal" data-bs-target="#cExitRow${toDo.getToDo().getId()}"></button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-body">

                    <!-- ***** ONLY DO ON TOP? ********************************************** -->
                    <div class="row mt-1">
                        <div class="col-auto fw-bolder text-primary">WHEN TASK CAN BE PERFORMED</div>
                        <div class="col"><hr></div>
                    </div>
                    <div id="Code_Behind_for_row_1" class="d-none">
                        <c:set var="aEarly" value=""></c:set>
                        <c:set var="bEarly" value="checked"></c:set>
                        <c:set var="earlyText" value="Task can be done <b><i><span style=\"color:red\">only at the top</span></i></b> of your ToDo list."></c:set>
                        <c:set var="cb1" value="text-secondary fw-lighter"></c:set>
                        <c:if test="${toDo.allowsEarly()}">
                            <c:set var="aEarly" value="checked"></c:set>
                            <c:set var="bEarly" value=""></c:set>
                            <c:set var="earlyText" value="Task can be done at <b><u>anytime</u></b>."></c:set>
                            <c:set var="cb1" value=""></c:set>
                        </c:if>
                    </div>
                    <div class="row">
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb1a${toDo.getToDo().getId()}" name="allowEarly" value="1" ${aEarly}  autocomplete="off">
                            <label class="btn btn-outline-success w-100" for="cb1a${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">At </span>Any Time
                            </label>
                        </div>
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb1b${toDo.getToDo().getId()}" name="allowEarly" value="0" ${bEarly}  autocomplete="off">
                            <label class="btn btn-outline-danger w-100" for="cb1b${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Only At Top of List</span>
                                <span class="d-lg-none">Top Only</span>
                            </label>
                        </div>
                    </div>

                    <!-- ***** AFFECT FUTURE TASKS? ********************************************** -->
                    <div class="row mt-3">
                        <div class="col-auto fw-bolder text-primary">AFFECT ON SUBSEQUENT TASKS</div>
                        <div class="col"><hr></div>
                    </div>
                    <div id="Code_Behind_for_row_2" class="d-none">
                        <c:set var="aFuture" value=""></c:set>
                        <c:set var="bFuture" value="checked"></c:set>
                        <c:set var="futureText" value="Task <span style=\"color:red\"><b><i>prevents completion of subsequent tasks</i></b></span> until it is completed."></c:set>
                        <c:if test="${toDo.allowsFuture()}">
                            <c:set var="aFuture" value="checked"></c:set>
                            <c:set var="bFuture" value=""></c:set>
                            <c:set var="futureText" value="Task <b><u>does not affect</u></b> subsequent tasks."></c:set>
                        </c:if>
                    </div>
                    <div class="row">
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb2a${toDo.getToDo().getId()}" name="allowFuture" value="1" ${aFuture}  autocomplete="off">
                            <label class="btn btn-outline-success w-100" for="cb2a${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Does Not Affect Them</span>
                                <span class="d-lg-none">No Affect</span>
                            </label>
                        </div>
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb2b${toDo.getToDo().getId()}" name="allowFuture" value="0" ${bFuture} autocomplete="off">
                            <label class="btn btn-outline-danger w-100" for="cb2b${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Prevents Early Completion of Them</span>
                                <span class="d-lg-none">Stop Completion</span>
                            </label>
                        </div>
                    </div>


                    <!-- ***** DESIGNATED EMPLOYEE ROW ********************************************** -->
                    <div class="row mt-3">
                        <div class="col-auto fw-bolder text-primary">DESIGNATE TASK TO A SPECIFIC EMPLOYEE</div>
                        <div class="col"><hr></div>
                    </div>
                    <div id="Code_Behind_For_DESIGNATED_EMPLOYEE" class="d-none">
                        <c:set var="bcb1" value="checked"></c:set>
                        <c:set var="bcb2" value=""></c:set>
                        <c:set var="bcb3" value=""></c:set>
                        <c:set var="showGoTo" value="col d-none"></c:set>
                        <c:choose>
                            <c:when test="${toDo.hasOwner() && !toDo.allowsNonOwner()}">
                                <c:set var="bcb1" value=""></c:set>
                                <c:set var="bcb2" value=""></c:set>
                                <c:set var="bcb3" value="checked"></c:set>
                                <c:set var="showGoTo" value="col"></c:set>
                            </c:when>
                            <c:when test="${toDo.hasOwner()}">
                                <c:set var="bcb1" value=""></c:set>
                                <c:set var="bcb2" value="checked"></c:set>
                                <c:set var="bcb3" value=""></c:set>
                                <c:set var="showGoTo" value="col"></c:set>
                            </c:when>
                            <c:otherwise></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="row">
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb3a${toDo.getToDo().getId()}" name="whoOwns" value="0" ${bcb1}  autocomplete="off" onchange="setEmployee(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-success w-100" for="cb3a${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">No, Any EE Can Do</span>
                                <span class="d-lg-none">No</span>
                            </label>
                        </div>
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb3b${toDo.getToDo().getId()}" name="whoOwns" value="1" ${bcb2} autocomplete="off" onchange="setEmployee(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-warning w-100 text-dark" for="cb3b${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Yes, Inform EE, Any Can Do</span>
                                <span class="d-lg-none">Yes, Inform</span>
                            </label>
                        </div>
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb3c${toDo.getToDo().getId()}" name="whoOwns" value="2" ${bcb3}  autocomplete="off" onchange="setEmployee(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-danger w-100" for="cb3c${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Yes, Only This EE Can Do</span>
                                <span class="d-lg-none">Yes, Require</span></label>
                        </div>
                    </div>
                    <div class="row mt-2" >
                        <div class="${showGoTo}" id="eeDropDown${toDo.getToDo().getId()}">
                            <div class="input-group">
                                <label class="input-group-text d-none d-lg-inline" for="ownerId${toDo.getToDo().getId()}">Employee Assignment</label>
                                <select class="form-select" name="ownerId" id="ownerId${toDo.getToDo().getId()}">
                                    <c:forEach var="user" items="${sessionScope.sVar.getStaffList()}">
                                        <c:set var="uSelect" value=""></c:set>
                                        <c:if test="${toDo.hasOwner() && toDo.getTaskOwner().getId()!=null && toDo.getTaskOwner().getId()==user.getId()}">
                                            <c:set var="uSelect" value="selected"></c:set>
                                        </c:if>
                                        <option value="${user.getId()}" ${uSelect} class="form-control">${user.getLastName().toUpperCase()}, ${user.getFirstName().toUpperCase()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- ***** OUTSOURCED ITEM ROW ********************************************** -->
                    <div class="row mt-3">
                        <div class="col-auto fw-bolder text-primary">DESIGNATE TASK TO OUTSIDE VENDOR</div>
                        <div class="col"><hr></div>
                    </div>
                    <div id="Code_Behind_For_OUTSOURCED_TASK" class="d-none">
                        <c:set var="acb1" value="checked"></c:set>
                        <c:set var="acb2" value=""></c:set>
                        <c:set var="acb3" value=""></c:set>
                        <c:set var="showSource" value="col d-none"></c:set>
                        <c:choose>
                            <c:when test="${toDo.isSourced() && !toDo.allowsNonOwner()}">
                                <c:set var="acb1" value=""></c:set>
                                <c:set var="acb2" value=""></c:set>
                                <c:set var="acb3" value="checked"></c:set>
                                <c:set var="showSource" value="col"></c:set>
                            </c:when>
                            <c:when test="${toDo.isSourced()}">
                                <c:set var="acb1" value=""></c:set>
                                <c:set var="acb2" value="checked"></c:set>
                                <c:set var="acb3" value=""></c:set>
                                <c:set var="showSource" value="col"></c:set>
                            </c:when>
                            <c:otherwise></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="row">
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb4a${toDo.getToDo().getId()}" name="isSourced" value="0" ${acb1}  autocomplete="off" onchange="setSource(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-success w-100" for="cb4a${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">No, Is An Internal Task</span>
                                <span class="d-lg-none">No</span>
                            </label>
                        </div>
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb4b${toDo.getToDo().getId()}" name="isSourced" value="1" ${acb2}  autocomplete="off" onchange="setSource(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-warning w-100 text-dark" for="cb4b${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Source, But We Can Do Too</span>
                                <span class="d-lg-none">Inform Source</span>
                            </label>
                        </div>
                        <div class="col">
                            <input type="radio" class="btn-check" id="cb4c${toDo.getToDo().getId()}" name="isSourced" value="2" ${acb3}  autocomplete="off" onchange="setSource(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-danger w-100" for="cb4c${toDo.getToDo().getId()}">
                                <span class="d-none d-lg-inline">Yes, Only BPO Can Do</span>
                                <span class="d-lg-none">Yes</span></label>
                        </div>
                    </div>
                    <div class="row mt-2" >
                        <div class="${showSource}" id="bpoDropDown${toDo.getToDo().getId()}">
                            <div class="input-group">
                                <label class="input-group-text d-none d-lg-inline" for="ownerId${toDo.getToDo().getId()}">Outside Source</label>
                                <select class="form-select" id="sourceId${toDo.getToDo().getId()}" name="sourceId">
                                    <c:forEach var="bpo" items="${sessionScope.bpoUserList}">
                                        <c:set var="bSelect" value=""></c:set>
                                        <c:if test="${toDo.isSourced() && toDo.getTaskOwner().getId()!=null && toDo.getTaskOwner().getId()==bpo.getId()}">
                                            <c:set var="bSelect" value="selected"></c:set>
                                        </c:if>
                                        <option value="${bpo.getId()}" ${bSelect} class="form-control">${bpo.getFirstName().toUpperCase()} ${bpo.getLastName().toUpperCase()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </div>


                    <!-- ***** WHERE TO DO THE WORK ********************************************** -->
                    <div class="row mt-3">
                        <div class="col-auto fw-bolder text-primary"><span class="d-none d-lg-inline">IS THERE A </span>WEBSITE WHERE YOU GO TO PERFORM
                            <span class="d-none d-lg-inline"> THE</span> TASK?</div>
                        <div class="col"><hr></div>
                    </div>
                    <div id="Code_Behind_For_Goto" class="d-none">
                        <c:set var="gtChecked" value=""></c:set>
                        <c:set var="notGtChecked" value="checked"></c:set>
                        <c:set var="gtStyle" value="d-none"></c:set>
                        <c:if test="${toDo.hasGoTo() && toDo.getGoToLink()!=null}">
                            <c:set var="gtChecked" value="checked"></c:set>
                            <c:set var="notGtChecked" value=""></c:set>
                            <c:set var="gtStyle" value=""></c:set>
                        </c:if>
                    </div>
                    <div class="row">
                        <div class="col-6 col-lg-2">
                            <input type="radio" class="btn-check" id="cb5a${toDo.getToDo().getId()}" name="hasGoTo" value="1" ${gtChecked}  autocomplete="off" onchange="showGoTo(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-success w-100" for="cb5a${toDo.getToDo().getId()}">YES</label>
                        </div>
                        <div class="col-6 col-lg-2">
                            <input type="radio" class="btn-check" id="cb5b${toDo.getToDo().getId()}" name="hasGoTo" value="0" ${notGtChecked} autocomplete="off" onchange="showGoTo(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-danger w-100" for="cb5b${toDo.getToDo().getId()}">No</label>
                        </div>
                        <div class="col-12 col-lg-6 mt-1 mt-lg-0">
                            <input type="url" class="form-control ${gtStyle}" name="goToPath" id="goToPath${toDo.getToDo().getId()}" value="${toDo.getGoToLink().getLinkPath()}" placeholder="Enter path to site here">
                        </div>
                    </div>

                    <!-- ***** WHERE TO DO GO FOR HELP ********************************************** -->
                    <div class="row mt-3">
                        <div class="col-auto fw-bolder text-primary">
                            <span class="d-none d-lg-inline">IS THERE A </span>
                            LINK TO INSTRUCTIONS OR HOW-TO GUIDE?</div>
                        <div class="col"><hr></div>
                    </div>
                    <div id="Code_Behind_For_Info" class="d-none">
                        <c:set var="infoChecked" value=""></c:set>
                        <c:set var="notInfoChecked" value="checked"></c:set>
                        <c:set var="infoStyle" value="d-none"></c:set>
                        <c:if test="${toDo.hasInfo() && toDo.getInfoLink()!=null}">
                            <c:set var="infoChecked" value="checked"></c:set>
                            <c:set var="notInfoChecked" value=""></c:set>
                            <c:set var="infoStyle" value=""></c:set>
                        </c:if>
                    </div>
                    <div class="row mt-1">
                        <div class="col-6 col-lg-2">
                            <input type="radio" class="btn-check" id="cb6a${toDo.getToDo().getId()}" name="hasInfo" value="1" ${infoChecked}  autocomplete="off" onchange="showPath(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-success w-100" for="cb6a${toDo.getToDo().getId()}">YES</label>
                        </div>
                        <div class="col-6 col-lg-2">
                            <input type="radio" class="btn-check" id="cb6b${toDo.getToDo().getId()}" name="hasInfo" value="0" ${notInfoChecked}  autocomplete="off" onchange="showPath(${toDo.getToDo().getId()})">
                            <label class="btn btn-outline-danger w-100" for="cb6b${toDo.getToDo().getId()}">No</label>
                        </div>
                        <div class="col-12 col-lg-6 mt-1 mt-lg-0">
                            <input type="url" class="form-control ${infoStyle}" name="infoPath" id="infoPath${toDo.getToDo().getId()}" value="${toDo.getInfoLink().getLinkPath()}" placeholder="Enter path to reference site here.">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <div class="container-fluid">
                        <div class="row m-0 p-0">
                            <div class="col-12 col-lg-6">
                                <button type="button" class="btn btn-warning w-100"  data-bs-toggle="modal" data-bs-target="#${modId2}">
                                    <i class="bi bi-robot"></i> Build Standard Email
                                </button>
                            </div>
                            <div class="col-12 col-lg-6 mt-2 mt-lg-auto">
                                <button type="submit" name="btnAuto1" value="${toDo.getToDo().getId()}" class="btn btn-primary w-100">
                                    <i class="bi bi-save"></i>&nbsp;&nbsp;
                                    Save Changes
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>