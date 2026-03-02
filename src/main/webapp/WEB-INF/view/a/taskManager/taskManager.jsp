<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>${sessionScope.psp.getFullName()}</title>
</head>
<body>
<div id="massCodeBehindNew" class="d-none">
    <c:set var="toDo" value="${sessionScope.sVar.getCurrentToDo()}"></c:set>
    <c:set var="pspAdm1" value="disabled"></c:set>
    <c:if test="${sessionScope.isPspAdmin==true}">
        <c:set var="pspAdm1" value=""></c:set>
    </c:if>
    <c:set var="modId" value="mod${toDo.getId()}"></c:set>
    <c:set var="modId2" value="mod2${toDo.getId()}"></c:set>
    <c:set var="isComplete1" value="${toDo.isComplete()}"></c:set>
    <c:set var="hasOwner2" value="${toDo.getTask().hasOwner() && toDo.getTask().getOwner()!=null}"></c:set>
    <c:set var="isTheOwner1" value="${toDo.getTask().getOwner()!=null && toDo.getTask().getOwner().getId()==sessionScope.sVar.getCurrentPerson().getId()}"></c:set>
    <c:set var="requiresOwnerOnly1" value="${!toDo.getTask().allowNonOwner()}"></c:set>
    <c:set var="isLocked1a" value="${toDo.getTask().hasOwner() && !isTheOwner1 && requiresOwnerOnly1}"></c:set>
    <c:set var="isLocked1b" value="${!tdId.first && !toDo.getTask().allowEarly()}"></c:set>
    <c:set var="isTrue" value="${true}"></c:set>
    <c:set var="isLocked1" value="${isLocked1a || isLocked1b}"></c:set>
    <c:set var="isDelegated1" value="${toDo.getTask().hasOwner()}"></c:set>
    <c:set var="isActivityOwner1" value="${sessionScope.sVar.getCurrentActivity().getAssignedTo().getId()==sessionScope.sVar.getCurrentPerson().getId()}"></c:set>
    <c:set var="ownerLock1" value=""></c:set>
    <c:if test="${hasOwner2==true && isTheOwner1==false && sessionScope.isPspAdmin==false}">
        <c:set var="ownerLock1" value="disabled"></c:set>
    </c:if>
</div>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/altNavbar.jsp"></c:import>
    <form action="UpdateTask25" method="post">
    <div class="container">
        <div class="row">
            <h3 class="col">
                MANAGE <span class="text-primary fst-italic">${sessionScope.sVar.getCurrentToDoOut().getDescription()} <i class="bi bi-check-square"></i></span>
            </h3>
        </div>

        <div class="accordion" id="accordionExample">
            <div class="accordion-item">
                <h2 class="accordion-header" id="headingOne">
                    <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
                        <b>Construct Task Completion Restrictions and Assignments</b>
                    </button>
                </h2>
                <div id="collapseOne" class="accordion-collapse collapse show" aria-labelledby="headingOne" data-bs-parent="#accordionExample">
                    <div class="accordion-body">
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
                            <c:if test="${toDo.getTask().allowEarly()}">
                                <c:set var="aEarly" value="checked"></c:set>
                                <c:set var="bEarly" value=""></c:set>
                                <c:set var="earlyText" value="Task can be done at <b><u>anytime</u></b>."></c:set>
                                <c:set var="cb1" value=""></c:set>
                            </c:if>
                        </div>
                        <div class="row">
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb1a${toDo.getId()}" name="allowEarly" value="1" ${aEarly}  autocomplete="off">
                                <label class="btn btn-outline-success w-100" for="cb1a${toDo.getId()}">
                                    <span class="d-none d-lg-inline">At </span>Any Time
                                </label>
                            </div>
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb1b${toDo.getId()}" name="allowEarly" value="0" ${bEarly}  autocomplete="off">
                                <label class="btn btn-outline-danger w-100" for="cb1b${toDo.getId()}">
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
                            <c:if test="${toDo.getTask().allowFuture()}">
                                <c:set var="aFuture" value="checked"></c:set>
                                <c:set var="bFuture" value=""></c:set>
                                <c:set var="futureText" value="Task <b><u>does not affect</u></b> subsequent tasks."></c:set>
                            </c:if>
                        </div>
                        <div class="row">
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb2a${toDo.getId()}" name="allowFuture" value="1" ${aFuture}  autocomplete="off">
                                <label class="btn btn-outline-success w-100" for="cb2a${toDo.getId()}">
                                    <span class="d-none d-lg-inline">Does Not Affect Them</span>
                                    <span class="d-lg-none">No Affect</span>
                                </label>
                            </div>
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb2b${toDo.getId()}" name="allowFuture" value="0" ${bFuture} autocomplete="off">
                                <label class="btn btn-outline-danger w-100" for="cb2b${toDo.getId()}">
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
                                <c:when test="${toDo.getTask().hasOwner() && !toDo.getTask().allowNonOwner()}">
                                    <c:set var="bcb1" value=""></c:set>
                                    <c:set var="bcb2" value=""></c:set>
                                    <c:set var="bcb3" value="checked"></c:set>
                                    <c:set var="showGoTo" value="col"></c:set>
                                </c:when>
                                <c:when test="${toDo.getTask().hasOwner()}">
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
                                <input type="radio" class="btn-check" id="cb3a${toDo.getId()}" name="whoOwns" value="0" ${bcb1}  autocomplete="off" onchange="setEmployee(${toDo.getId()})">
                                <label class="btn btn-outline-success w-100" for="cb3a${toDo.getId()}">
                                    <span class="d-none d-lg-inline">No, Any EE Can Do</span>
                                    <span class="d-lg-none">No</span>
                                </label>
                            </div>
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb3b${toDo.getId()}" name="whoOwns" value="1" ${bcb2} autocomplete="off" onchange="setEmployee(${toDo.getId()})">
                                <label class="btn btn-outline-warning w-100 text-dark" for="cb3b${toDo.getId()}">
                                    <span class="d-none d-lg-inline">Yes, Inform EE, Any Can Do</span>
                                    <span class="d-lg-none">Yes, Inform</span>
                                </label>
                            </div>
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb3c${toDo.getId()}" name="whoOwns" value="2" ${bcb3}  autocomplete="off" onchange="setEmployee(${toDo.getId()})">
                                <label class="btn btn-outline-danger w-100" for="cb3c${toDo.getId()}">
                                    <span class="d-none d-lg-inline">Yes, Only This EE Can Do</span>
                                    <span class="d-lg-none">Yes, Require</span></label>
                            </div>
                        </div>
                        <div class="row mt-2" >
                            <div class="${showGoTo}" id="eeDropDown${toDo.getId()}">
                                <div class="input-group">
                                    <label class="input-group-text d-none d-lg-inline" for="ownerId${toDo.getId()}">Employee Assignment</label>
                                    <select class="form-select" name="ownerId" id="ownerId${toDo.getId()}">
                                        <%-- --%>
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
                                <c:when test="${toDo.getTask().isSourced() && !toDo.getTask().allowNonOwner()}">
                                    <c:set var="acb1" value=""></c:set>
                                    <c:set var="acb2" value=""></c:set>
                                    <c:set var="acb3" value="checked"></c:set>
                                    <c:set var="showSource" value="col"></c:set>
                                </c:when>
                                <c:when test="${toDo.getTask().isSourced()}">
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
                                <input type="radio" class="btn-check" id="cb4a${toDo.getId()}" name="isSourced" value="0" ${acb1}  autocomplete="off" onchange="setSource(${toDo.getId()})">
                                <label class="btn btn-outline-success w-100" for="cb4a${toDo.getId()}">
                                    <span class="d-none d-lg-inline">No, Is An Internal Task</span>
                                    <span class="d-lg-none">No</span>
                                </label>
                            </div>
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb4b${toDo.getId()}" name="isSourced" value="1" ${acb2}  autocomplete="off" onchange="setSource(${toDo.getId()})">
                                <label class="btn btn-outline-warning w-100 text-dark" for="cb4b${toDo.getId()}">
                                    <span class="d-none d-lg-inline">Source, But We Can Do Too</span>
                                    <span class="d-lg-none">Inform Source</span>
                                </label>
                            </div>
                            <div class="col">
                                <input type="radio" class="btn-check" id="cb4c${toDo.getId()}" name="isSourced" value="2" ${acb3}  autocomplete="off" onchange="setSource(${toDo.getId()})">
                                <label class="btn btn-outline-danger w-100" for="cb4c${toDo.getId()}">
                                    <span class="d-none d-lg-inline">Yes, Only BPO Can Do</span>
                                    <span class="d-lg-none">Yes</span></label>
                            </div>
                        </div>
                        <div class="row mt-2" >
                            <div class="${showSource}" id="bpoDropDown${toDo.getId()}">
                                <div class="input-group">
                                    <label class="input-group-text d-none d-lg-inline" for="ownerId${toDo.getId()}">Outside Source</label>
                                    <select class="form-select" id="sourceId${toDo.getId()}" name="sourceId">
                                        <%-- --%>
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
                            <c:if test="${toDo.getTask().hasGoTo() && toDo.getTask().getGoToLink()!=null}">
                                <c:set var="gtChecked" value="checked"></c:set>
                                <c:set var="notGtChecked" value=""></c:set>
                                <c:set var="gtStyle" value=""></c:set>
                            </c:if>
                        </div>
                        <div class="row">
                            <div class="col-6 col-lg-2">
                                <input type="radio" class="btn-check" id="cb5a${toDo.getId()}" name="hasGoTo" value="1" ${gtChecked}  autocomplete="off" onchange="showGoTo(${toDo.getId()})">
                                <label class="btn btn-outline-success w-100" for="cb5a${toDo.getId()}">YES</label>
                            </div>
                            <div class="col-6 col-lg-2">
                                <input type="radio" class="btn-check" id="cb5b${toDo.getId()}" name="hasGoTo" value="0" ${notGtChecked} autocomplete="off" onchange="showGoTo(${toDo.getId()})">
                                <label class="btn btn-outline-danger w-100" for="cb5b${toDo.getId()}">No</label>
                            </div>
                            <div class="col-12 col-lg-6 mt-1 mt-lg-0">
                                <input type="url" class="form-control ${gtStyle}" name="goToPath" id="goToPath${toDo.getId()}" value="${toDo.getTask().getGoToLink().getLinkPath()}" placeholder="Enter path to site here">
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
                            <c:if test="${toDo.getTask().hasInfo() && toDo.getTask().getInfoLink()!=null}">
                                <c:set var="infoChecked" value="checked"></c:set>
                                <c:set var="notInfoChecked" value=""></c:set>
                                <c:set var="infoStyle" value=""></c:set>
                            </c:if>
                        </div>
                        <div class="row mt-1">
                            <div class="col-6 col-lg-2">
                                <input type="radio" class="btn-check" id="cb6a${toDo.getId()}" name="hasInfo" value="1" ${infoChecked}  autocomplete="off" onchange="showPath(${toDo.getId()})">
                                <label class="btn btn-outline-success w-100" for="cb6a${toDo.getId()}">YES</label>
                            </div>
                            <div class="col-6 col-lg-2">
                                <input type="radio" class="btn-check" id="cb6b${toDo.getId()}" name="hasInfo" value="0" ${notInfoChecked}  autocomplete="off" onchange="showPath(${toDo.getId()})">
                                <label class="btn btn-outline-danger w-100" for="cb6b${toDo.getId()}">No</label>
                            </div>
                            <div class="col-12 col-lg-6 mt-1 mt-lg-0">
                                <input type="url" class="form-control ${infoStyle}" name="infoPath" id="infoPath${toDo.getId()}" value="${toDo.getTask().getInfoLink().getLinkPath()}" placeholder="Enter path to reference site here.">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <!-- ***** AUTOMATE EMAIL FOR TASK ********************************************** -->
            <c:choose>
                <c:when test="${toDo.getTask().isAutomated()==true && toDo.getTask().getAutomation()==null}">
                    &nbsp;
                </c:when>
                <c:otherwise>
                    <div class="accordion-item">
                        <h2 class="accordion-header" id="headingTwo">
                            <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">
                                <b>Construct a Standard Email Message to Be Sent With This Task</b>
                            </button>
                        </h2>
                        <div id="collapseTwo" class="accordion-collapse collapse" aria-labelledby="headingTwo" data-bs-parent="#accordionExample">
                            <div class="accordion-body">
                                <div class="container-fluid">
                                    <div class="row d-none">
                                        <input type="text" name="taskId" value="${toDo.getTask().getId()}">
                                    </div>
                                    <div class="row">
                                        <div class="col">
                                            <div class="input-group">
                                                <label for="autoName${toDo.getId()}" class="input-group-text d-none d-lg-inline">Statement To Display For Email</label>
                                                <input type="text" class="form-control" id="autoName${toDo.getId()}" name="autoName" value="${toDo.getTask().getAutomationText()}" placeholder="Enter statement to display for the email automation">
                                                <button class="btn btn-primary" type="button" data-bs-toggle="collapse" data-bs-target="#XmlHelp" aria-expanded="false" aria-controls="XmlHelp">
                                                    <i class="bi bi-question-circle"></i> Xml Help
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                    <!-- ****** XML HELP CARDS ****************************** -->
                                    <div class="row row-cols-2 row-cols-md-3 row-cols-xl-4 collapse mt-1" id="XmlHelp">
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;sbj&gt;&nbsp&lt;/sbj&gt;</h6>
                                                <div class="card card-body">
                                                    <p class="card-text">Enter the subject line of the email between the elements.</p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;ii&gt;&nbsp&lt;/ii&gt;</h6>
                                                <div class="card card-body">
                                                    <p class="card-text">Creates a prompt for user entry before sending.  Enter the name of the prompt between elements.</p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info ">
                                                <h6 class="card-header text-bg-info">&lt;ii&gt;&lt;l&gt;&nbsp&lt;/ii&gt;</h6>

                                                <div class="card card-body">
                                                    <p class="card-text">Creates a prompt for a LINK entry before sending.  Enter the name of the link after the &lt;l&gt;</p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;&lt;#erName&gt;&gt;</h6>

                                                <div class="card card-body">
                                                    <p class="card-text">Inserts the name of the employer into the message. <b><i>Will only work for renewals.</i></b></p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;rf&gt;&nbsp&lt;/rf&gt;</h6>

                                                <div class="card card-body">
                                                    <p class="card-text">Enter the code of the reference link to insert between the elements.</p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;&lt;#activityType&gt;&gt;</h6>

                                                <div class="card card-body">
                                                    <p class="card-text">Inserts the word Renewal, Setup or Ticket as appropriate.</p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;&lt;close&gt;&gt;</h6>

                                                <div class="card card-body">
                                                    <p class="card-text">Closes the task after the email is sent.</p>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col mb-1">
                                            <div class="card h-100 border-info">
                                                <h6 class="card-header text-bg-info">&lt;&lt;sig&gt;&gt;</h6>

                                                <div class="card card-body">
                                                    <p class="card-text">Inserts a signature at the end of the email.</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="row mt-2">
                                        <div class="col-12 col-lg-9">
                                            <div class="row">
                                                <div class="col text-center text-primary fw-bold fs-4">
                                                    HTML / XML <span class="d-none d-lg-inline">Standard Email Template Entry</span>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="col">
                                                    <c:set var="text1" value="${toDo.getTask().getAutomation().getHtmlContent().trim()}"></c:set>
                                                    <c:set var="ind" value="${text1.indexOf(\"<pre>\")}"></c:set>
                                                    <c:set var="text2" value="${text1.substring(ind)}"></c:set>
                                                    <c:choose>
                                                        <c:when test="${toDo.getTask().getAutomation()!=null}">
                                                            <textarea class="form-control" id="autoText${toDo.getId()}" name="autoText" rows="15">${text2}</textarea>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <textarea class="form-control" id="autoText${toDo.getId()}" name="autoText" rows="15"></textarea>
                                                        </c:otherwise>
                                                    </c:choose>

                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-12 col-lg-3 d-none d-lg-inline">
                                            <div class="row">
                                                <div class="col">
                                                    <div class="row m-0 p-0 align-items-center">
                                                        <div class="col text-secondary fw-bolder fs-5">
                                                            Code List
                                                        </div>
                                                        <div class="col-auto">
                                                            <button type="button" class="btn btn-sm text-secondary-emphasis border-0" data-bs-toggle="modal" data-bs-target="#addInsertLinkModal">
                                                                [add code]
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="col">
                                                    <c:forEach var="refLink" items="${sessionScope.rfCodes}">
                                                        <div class="row m-0 p-0 mt-1">
                                                            <div class="col m-0 p-0">${refLink.getId()}</div>
                                                            <div class="col m-0 p-0">${refLink.getPlainText()}</div>
                                                        </div>
                                                    </c:forEach>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>


        <!-- ***** FOOTER ********************************************** -->
        <div class="row mt-1 ">
            <div class="col-12 col-lg-6 mt-2 mt-lg-auto">
                <button type="submit" ${ownerLock1} name="btnAuto1" value="${toDo.getId()}" class="btn btn-primary w-100">
                    <i class="bi bi-save"></i>&nbsp;&nbsp;
                    Save Changes
                </button>
            </div>
            <div class="col-12 col-lg-6 mt-2 mt-lg-auto">
                <c:choose>
                    <c:when test="${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName().equals(\"CheckList\")}">
                        <a href="ViewChecklist25" class="btn btn-outline-danger w-100">
                            <i class="bi bi-chevron-left"></i>&nbsp;
                            Cancel Changes
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a href="ViewActivity25" class="btn btn-outline-danger w-100">
                            <i class="bi bi-chevron-left"></i>&nbsp;
                            Cancel Changes
                        </a>
                    </c:otherwise>
                </c:choose>

            </div>
        </div>
    </div>
    <script>
            function setEmployee(toDoId){
                let cb1 = "cb3a"  + toDoId;
                let cbA = document.getElementById(cb1);
                let cb2 = "cb4a" + toDoId;
                let cbB = document.getElementById(cb2);
                let row1 = "eeDropDown" + toDoId;
                let rowA = document.getElementById(row1);
                let row2 = "bpoDropDown" + toDoId;
                let rowB = document.getElementById(row2);

                if(cbA.checked)
                    rowA.className = "col d-none";
                else {
                    rowA.className = "col";
                    cbB.checked=true;
                    rowB.className = "col d-none";
                }
            }
            function setSource(toDoId){
                let cb1 = "cb3a"  + toDoId;
                let cbA = document.getElementById(cb1);
                let cb2 = "cb4a" + toDoId;
                let cbB = document.getElementById(cb2);
                let row1 = "eeDropDown" + toDoId;
                let rowA = document.getElementById(row1);
                let row2 = "bpoDropDown" + toDoId;
                let rowB = document.getElementById(row2);

                if(cbB.checked)
                    rowB.className = "col d-none";
                else {
                    rowB.className = "col";
                    cbA.checked=true;
                    rowA.className = "col d-none";
                }
            }
            function showPath(toDoId){
                let i1= "infoPath" + toDoId;
                let input1 = document.getElementById(i1);
                let cb1 = "cb6a" + toDoId;
                let cbA = document.getElementById(cb1);

                if(cbA.checked){
                    input1.required=true;
                    input1.className="form-control";
                } else {
                    input1.required=false;
                    input1.className="form-control d-none";
                    input1.value='';
                }
            }
            function showGoTo(toDoId){
                let i1= "goToPath" + toDoId;
                let input1 = document.getElementById(i1);
                let cb1 = "cb5a" + toDoId;
                let cbA = document.getElementById(cb1);

                if(cbA.checked){
                    input1.required=true;
                    input1.className="form-control";
                } else {
                    input1.required=false;
                    input1.className="form-control d-none";
                    input1.value='';
                }
            }

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
    </form>
</div>
</body>
</html>