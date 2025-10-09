<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Create Checklist</title>
    <script>
        function selectActivity(){
            let rr = document.getElementById("renewalRow");
            let sr = document.getElementById("setupRow");
            let tr = document.getElementById("ticketRow")
            let ele = document.getElementsByName('activityType');
            let tb = document.getElementById("taskBuilder");
            let ont = document.getElementsByName("oldNewTicket");
            let onr = document.getElementsByName("oldNewRenewal");
            let ons = document.getElementsByName("oldNewSetups");
            let tb1 = document.getElementById("ticketDescription1");
            tb1.required=false;
            if(ele[0].checked){
                ont[0].checked=true;
                showTicket();
                rr.classList.add("d-none");
                sr.classList.add("d-none");
                tr.classList.remove("d-none");
            } else if(ele[1].checked){
                onr[0].checked = true;
                showRenewal();
                tr.classList.add("d-none");
                sr.classList.add("d-none");
                rr.classList.remove("d-none");
            } else {
                ons[0].checked=true;
                showSetup();
                rr.classList.add("d-none");
                tr.classList.add("d-none");
                sr.classList.remove("d-none");
            }
            tb.classList.add("d-none");
        }
        function showTicket(){
            let ele = document.getElementsByName('oldNewTicket');
            let theSelect = document.getElementById('ticketSequenceList');
            let otItems = document.getElementById("oldTickets");
            let ntItems = document.getElementById("newTickets");
            let tb2 = document.getElementById("ticketDescription1");
            if(ele[1].checked){
                otItems.classList.add("d-none");
                ntItems.classList.remove("d-none");
                tb2.required=true;
            } else {
                ntItems.classList.add("d-none");
                otItems.classList.remove("d-none");
                tb2.required=false;
            }
            theSelect.value = "-1";
            document.getElementById("loadTicket").disabled = true;
        }
        function enableTicketLoad(){
            let theSelect = document.getElementById('ticketSequenceList');
            document.getElementById("loadTicket").disabled = theSelect.value === "-1";
        }
        function showRenewal(){
            let ele = document.getElementsByName('oldNewRenewal');
            let otItems = document.getElementById("oldRenewals");
            let ntItems = document.getElementById("newRenewals");
            let theSelect = document.getElementById('renewalSequenceList');
            let tb21 = document.getElementById("ticketDescription1");
            tb21.required = false;
            if(ele[1].checked){
                otItems.classList.add("d-none");
                ntItems.classList.remove("d-none");
            } else {
                ntItems.classList.add("d-none");
                otItems.classList.remove("d-none");
            }
            theSelect.value="-1";
            document.getElementById("loadRenewal").disabled=true;
        }
        function enableRenLoad(){
            let ts = document.getElementById("renewalSequenceList");
            let bt = document.getElementById("loadRenewal");
            bt.disabled = ts.value === "-1";
        }
        function showSetup(){
            let ele = document.getElementsByName('oldNewSetups');
            let otItems = document.getElementById("oldSetups");
            let ntItems = document.getElementById("newSetups");
            let theSelect = document.getElementById('setupSequenceList');
            let tb22 = document.getElementById("ticketDescription1");
            tb22.required = false;
            if(ele[1].checked){
                otItems.classList.add("d-none");
                ntItems.classList.remove("d-none");
            } else {
                ntItems.classList.add("d-none");
                otItems.classList.remove("d-none");
            }
            theSelect.value = "-1";
            document.getElementById('loadSetup').disabled=true;
        }
        function enableSetupBut(){
            let theSelect = document.getElementById('setupSequenceList');
            document.getElementById('loadSetup').disabled = theSelect.value === "-1";
        }
        function enableAdd(){
            let ele = document.getElementsByName('whichRadio');
            let taskSel = document.getElementById("selTask-I");
            let btAdd = document.getElementById('addTask-I');

            if(ele[0].checked){
                btAdd.disabled=false;
                btAdd.type="submit";
            } else if(taskSel.value ==="-1"){
                btAdd.disabled=true;
                taskSel.classList.add("text-danger");
            } else {
                btAdd.disabled = false;
                btAdd.type="submit";
                taskSel.classList.remove("text-danger");
            }

        }
        function showHideX(){
            let descBox = document.getElementById("taskDesc-I");
            let taskSel = document.getElementById("selTask-I");
            let ele = document.getElementsByName('whichRadio');
            let saveBut = document.getElementById('saveTask-I');
            let saveBut1 = document.getElementById('saveTask1-I');
            if(ele[1].checked){
                descBox.classList.add("d-none");
                taskSel.classList.remove("d-none");
                descBox.required=false;
                saveBut.classList.add("d-none");
                saveBut1.classList.add("d-none");
            } else {
                taskSel.classList.add("d-none");
                descBox.classList.remove("d-none");
                saveBut.classList.remove("d-none");
                saveBut1.classList.remove("d-none");
                descBox.required=true;
            }
            taskSel.value="-1";
            taskSel.classList.add("text-danger");
            enableAdd();
        }
        function showHideY(intId){
            let radioName = "whichRadio"+intId;
            let rb = document.getElementsByName(radioName);
            let tbName = "taskDesc-"+intId;
            let tb = document.getElementById(tbName);
            let ddName = "selTask-"+intId;
            let dd = document.getElementById(ddName);
            let btName = "saveTask1-"+intId;
            let bt = document.getElementById(btName);
            if(rb[0].checked){
                dd.classList.add("d-none");
                tb.classList.remove("d-none");
                tb.required = true;
                bt.classList.remove("d-none");
            } else {
                dd.classList.remove("d-none");
                tb.classList.add("d-none");
                tb.required = false;
                bt.classList.add("d-none");
            }
        }
        function enableAddY(intId){
            let eString = "whichRadio"+intId;
            let tString = "selTask-"+intId;
            let bString = "addTask-"+intId;
            let ele = document.getElementsByName(eString);
            let taskSel = document.getElementById(tString);
            let btAdd = document.getElementById(bString);

            if(ele[0].checked){
                btAdd.disabled=false;
                btAdd.type="submit";
            } else if(taskSel.value ==="-1"){
                btAdd.disabled=true;
                taskSel.classList.add("text-danger");
            } else {
                btAdd.disabled = false;
                btAdd.type="submit";
                taskSel.classList.remove("text-danger");
            }
        }
        function deleteLine(intId){
            let textBoxName = "taskDesc-" + intId;
            let textOne = document.getElementById(textBoxName);
            textOne.required=false;
            let theForm = document.getElementById("mainForm");
            theForm.elements["btnTb"].value = "DE-" + intId;
            theForm.submit();
        }
        function endReq(intId){
            let boxName = "taskDesc-"+intId;
            let theBox = document.getElementById(boxName);
            theBox.required=false;
        }
        function startReq(intId){
            let boxName = "taskDesc-"+intId;
            let theBox = document.getElementById(boxName);
            theBox.required=true;
        }
    </script>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
<div class="container-fluid">
    <div class="row m-0 p-0">
        <div class="col-lg-1 col-xl-2"></div>
        <div class="col-12 col-lg-10 col-xl-9 m-0 p-0">
            <form method="post" action="TaskBuilder25" id="updatedForm">
                <!-- **************************   TAGLIB CODE BEHIND ************************************************************** -->
                <div class="d-none" id="massCodeBehind169">
                    <div id="onLoad">
                        <c:set var="selLock" value=""> </c:set>
                        <c:set var="selLock2" value="d-none"> </c:set>
                        <c:set var="selLock3" value=""> </c:set>
                        <c:if test="${sessionScope.lockTemplateSelector==1}">
                            <c:set var="selLock" value="pe-none"> </c:set>
                            <c:set var="selLock2" value="" > </c:set>
                            <c:set var="selLock3" value="d-none"> </c:set>
                        </c:if>
                    </div>
                    <div id="rowOneF">
                        <c:set var="r1Tik" value="checked"> </c:set>
                        <c:set var="r1Ren" value=""> </c:set>
                        <c:set var="r1Set" value=""> </c:set>
                        <c:set var="r1Tik2" value=""> </c:set>
                        <c:set var="r1Ren2" value="d-none"> </c:set>
                        <c:set var="r1Set2" value="d-none"> </c:set>
                        <c:choose>
                            <c:when test="${sessionScope.r1sel.equals(\"1\")}">
                                <c:set var="r1Tik" value=""> </c:set>
                                <c:set var="r1Ren" value="checked"> </c:set>
                                <c:set var="r1Set" value=""> </c:set>
                                <c:set var="r1Tik2" value="d-none"> </c:set>
                                <c:set var="r1Ren2" value=""> </c:set>
                                <c:set var="r1Set2" value="d-none"> </c:set>
                            </c:when>
                            <c:when test="${sessionScope.r1sel.equals(\"2\")}">
                                <c:set var="r1Tik" value=""> </c:set>
                                <c:set var="r1Ren" value=""> </c:set>
                                <c:set var="r1Set" value="checked"> </c:set>
                                <c:set var="r1Tik2" value="d-none"> </c:set>
                                <c:set var="r1Ren2" value="d-none"> </c:set>
                                <c:set var="r1Set2" value=""> </c:set>
                            </c:when>
                        </c:choose>
                    </div>
                    <div id="rowTikH">
                        <!-- T I C K E T   if N E W is selected -->
                        <c:set var="tSelExist" value=""> </c:set>
                        <c:set var="tSelNew" value="checked"> </c:set>
                        <c:set var="tSelExist2" value="selected"> </c:set>
                        <c:set var="tViewNew" value=""> </c:set>
                        <c:set var="tViewExist" value="d-none"> </c:set>
                        <c:set var="tSelExist3" value="disabled"> </c:set>
                        <!-- T I C K E T   if E X I S T I N G is selected -->
                        <c:if test="${sessionScope.tSel.equals(\"0\")}">
                            <c:set var="tSelExist" value="checked"> </c:set>
                            <c:set var="tSelNew" value=""> </c:set>
                            <c:set var="tSelExist2" value=""> </c:set>
                            <c:set var="tViewNew" value="d-none"> </c:set>
                            <c:set var="tViewExist" value=""> </c:set>
                            <c:set var="tSelExist3" value=""> </c:set>
                        </c:if>
                    </div>
                    <div id="rowRenH">
                        <!-- R E N E W A L  if  N E W is selected -->
                        <c:set var="rSelExist" value=""> </c:set>
                        <c:set var="rSelNew" value="checked"> </c:set>
                        <c:set var="rSelExist2" value="selected"> </c:set>
                        <c:set var="rViewNew" value=""> </c:set>
                        <c:set var="rViewExist" value="d-none"> </c:set>
                        <c:set var="rSelExist3" value="disabled"> </c:set>
                        <!-- R E N E W A L  if E X I S T I N G is selected -->
                        <c:if test="${sessionScope.rSel.equals(\"0\")}">
                            <c:set var="rSelExist" value="checked"> </c:set>
                            <c:set var="rSelNew" value=""> </c:set>
                            <c:set var="rSelExist2" value=""> </c:set>
                            <c:set var="rViewNew" value="d-none"> </c:set>
                            <c:set var="rViewExist" value=""> </c:set>
                            <c:set var="rSelExist3" value=""> </c:set>
                        </c:if>
                    </div>
                    <div id="rowSetH">
                        <!-- S E T U P  if  N E W is selected -->
                        <c:set var="sSelExist" value=""> </c:set>
                        <c:set var="sSelNew" value="checked"> </c:set>
                        <c:set var="sSelExist2" value="selected"> </c:set>
                        <c:set var="sViewNew" value=""> </c:set>
                        <c:set var="sViewExist" value="d-none"> </c:set>
                        <c:set var="sSelExist3" value="disabled"> </c:set>
                        <!-- S E T U P if E X I S T I N G is selected -->
                        <c:if test="${sessionScope.sSel.equals(\"0\")}">
                            <c:set var="sSelExist" value="checked"> </c:set>
                            <c:set var="sSelNew" value=""> </c:set>
                            <c:set var="sSelExist2" value=""> </c:set>
                            <c:set var="sViewNew" value="d-none"> </c:set>
                            <c:set var="sViewExist" value=""> </c:set>
                            <c:set var="sSelExist3" value=""> </c:set>
                        </c:if>
                    </div>
                    <c:set var="showTb" value="d-none"> </c:set>
                    <c:if test="${sessionScope.showTaskBuilder.equals(\"Y\")}">
                        <c:set var="showTb" value=""> </c:set>
                    </c:if>
                </div>
                <!-- **************************   HEADER ROWS ************************************************************** -->
                <div class="row">
                    <div class="col text-center">
                        <hr>
                        <h4>Sequence Template Manager</h4>
                        <hr>
                    </div>
                </div>
                <!-- **************************   ACTIVITY SELECTION ROWS ************************************************************** -->
                <div class="row ${selLock3}">
                    <div class="col-12">
                        <div class="btn-group w-100">
                            <input type="radio" name="activityType" id="typeTicket" value="0" class="btn-check" autocomplete="off" ${r1Tik} onclick="selectActivity()">
                            <label class="btn btn-outline-info border-dark-subtle fw-bold  ${selLock}" for="typeTicket">
                                <i class="bi bi-ticket-detailed"></i> Ticket<span class="d-none d-lg-inline">&nbsp;Sequences</span>
                            </label>
                            <input type="radio" name="activityType" id="typeRenewal" value="1" class="btn-check" autocomplete="off" ${r1Ren} onclick="selectActivity()" >
                            <label class="btn btn-outline-primary border-dark-subtle fw-bold border-start-0  ${selLock}" for="typeRenewal">
                                <i class="bi bi-repeat"></i> Renewal<span class="d-none d-lg-inline">&nbsp;Sequences</span>
                            </label>
                            <input type="radio" name="activityType" id="typeSetup" value="2" class="btn-check" autocomplete="off" ${r1Set} onclick="selectActivity()" >
                            <label class="btn btn-outline-secondary border-dark-subtle fw-bold border-start-0 ${selLock}" for="typeSetup">
                                <i class="bi bi-buildings"></i> Setup<span class="d-none d-lg-inline">&nbsp;Sequences</span>
                            </label>
                        </div>
                    </div>
                </div>
                <div class="row ${selLock2}">
                    <div class="col-auto ">
                    </div>
                    <div class="col" id="SequenceName">
                        <div class="text-primary fs-4 fw-bold text-center overflow-hidden ${selLock2}">
                            <i class="bi bi-lock"></i>
                            &nbsp;${sessionScope.namePlate}
                            &nbsp;<a href="GoTicketTemplate25" class="text-danger fs-6 ${selLock2} ">[cancel]</a>
                        </div>
                    </div>
                    <div class="col-auto">
                    </div>
                </div>
                <!-- **************************   SUB ACTIVITY SELECTION ROWS ************************************************************** -->
                <div class="row m-0 p-0 ${selLock3}" id="allSelectRows">
                    <div class="col m-0 p-0">
                        <!-- **************************   TICKET SELECTION ROWS ************************************************************** -->
                        <div class="row mt-1 ${r1Tik2}" id="ticketRow">
                            <div class="col-12 m-0 mt-1 p-0">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="btn-group w-100">
                                            <input type="radio" name="oldNewTicket" id="ontOld" value="0" class="btn-check" autocomplete="off" ${tSelExist} onclick="showTicket()">
                                            <label class="btn btn-outline-info border-dark-subtle text-dark ${selLock}" for="ontOld">
                                                <i class="bi bi-journal-text"></i> Modify Existing
                                            </label>
                                            <input type="radio" name="oldNewTicket" id="ontNew" value="1" class="btn-check" autocomplete="off" ${tSelNew} onclick="showTicket()" >
                                            <label class="btn btn-outline-info border-dark-subtle text-dark  border-start-0 ${selLock}" for="ontNew">
                                                <i class="bi bi-journal-plus"></i> Create New
                                            </label>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>
                            <div class="col-12 m-0 mt-2 p-0 ${tViewExist}" id="oldTickets">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="input-group w-100">
                                            <label for="ticketSequenceList" class="input-group-text d-none d-xl-inline">
                                                Existing Sequences
                                            </label>
                                            <select class="form-select ${selLock}" id="ticketSequenceList" name="ticketSequenceList" onchange="enableTicketLoad()">
                                                <option value="-1" ${tSelExist2}>Select A Sequence</option>
                                                <c:forEach var="tSeq" items="${sessionScope.rtlTicketList}">
                                                    <c:choose>
                                                        <c:when test="${sessionScope.selectedSequenceId==tSeq.getId()}">
                                                            <option value="${tSeq.getId()}" selected>${tSeq.getDescription()}</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="${tSeq.getId()}">${tSeq.getDescription()}</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </select>
                                            <button type="submit" value="LT-" name="btnTb" id="loadTicket" ${tSelExist3} class="btn btn-outline-dark ${selLock}">Load</button>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>
                            <div class="col m-0 mt-2 p-0 ${tViewNew}" id="newTickets">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="input-group w-100">
                                            <label for="ticketDescription1" class="input-group-text d-none d-xl-inline ">Category</label>
                                            <select class="form-select ${selLock}" id="selectCatForm" name="ticketCategory">
                                                <c:forEach var="cat11" items="${applicationScope.global.getTicketCategories()}">
                                                    <c:choose>
                                                        <c:when test="${cat11.getId()==sessionScope.tikCatId}">
                                                            <option value="${cat11.getId()}" selected>${cat11.getDescription()}</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="${cat11.getId()}">${cat11.getDescription()}</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </select>
                                            <input type="text" id="ticketDescription1" class="form-control ${selLock}" name="ticketDescription" value="${sessionScope.tikDescription}"  placeholder="Give Name of Task List" >
                                            <button type="submit" value="NT-" id="newTicket" name="btnTb" class="btn btn-outline-dark ${selLock}">Start</button>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>
                        </div>
                        <!-- **************************   RENEWAL SELECTION ROWS ************************************************************** -->
                        <div class="row mt-1 ${r1Ren2}" id="renewalRow">
                            <div class="col-12 m-0 mt-1 p-0">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="btn-group w-100">
                                            <input type="radio" name="oldNewRenewal" id="onrOld" value="0" class="btn-check" autocomplete="off" ${rSelExist} onclick="showRenewal()">
                                            <label class="btn btn-outline-primary border-dark-subtle ${selLock}" for="onrOld">
                                                <i class="bi bi-journal-text"></i>&nbsp;Modify Existing
                                            </label>
                                            <input type="radio" name="oldNewRenewal" id="onrNew" value="1" class="btn-check" autocomplete="off" ${rSelNew} onclick="showRenewal()" >
                                            <label class="btn btn-outline-primary border-start-0 ${selLock}" for="onrNew">
                                                <i class="bi bi-journal-plus"></i>&nbsp;Create New
                                            </label>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>

                            <div class="col-12 m-0 mt-2 p-0 ${rViewExist}" id="oldRenewals">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="input-group w-100">
                                            <label for="renewalSequenceList" class="input-group-text d-none d-xl-inline">
                                                Existing Sequences
                                            </label>
                                            <select class="form-select ${selLock}" id="renewalSequenceList" name="renewalSequenceList" onchange="enableRenLoad()">
                                                <option value="-1" ${rSelExist2}>Select A Sequence</option>
                                                <c:forEach var="rSeq" items="${sessionScope.reqRenewalList}">
                                                    <c:choose>
                                                        <c:when test="${sessionScope.selectedSequenceId==rSeq.getId()}">
                                                            <option value="${rSeq.getId()}" selected>${rSeq.getDescription()}</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="${rSeq.getId()}">${rSeq.getDescription()}</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </select>
                                            <button type="submit" name="btnTb" id="loadRenewal" value="LR-" class="btn btn-outline-dark ${selLock}" ${rSelExist3} >Load</button>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>
                            <div class="col-12 m-0 mt-2 p-0 ${rViewNew}" id="newRenewals">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="input-group w-100">
                                            <label for="renewalDescription" class="input-group-text d-none d-xl-inline">Name</label>
                                            <select class="form-select ${selLock}" id="renewalDescription" name="renewalDescription">
                                                <c:forEach var="rSeq2" items="${sessionScope.notRenewalList}">
                                                    <c:choose>
                                                        <c:when test="${rSeq2.getId()==sessionScope.rSequenceSelectedId}">
                                                            <option value="${rSeq2.getId()}" selected>(Renewal) ${rSeq2.getDescription()}</option></c:when>
                                                        <c:otherwise>
                                                            <option value="${rSeq2.getId()}">(Renewal) ${rSeq2.getDescription()}</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </select>
                                            <button type="submit" name="btnTb" value="NR-" class="btn btn-outline-dark ${selLock}">
                                                Start
                                            </button>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>
                        </div>
                        <!-- **************************   SETUP SELECTION ROWS ************************************************************** -->
                        <div class="row mt-1 ${r1Set2}" id="setupRow">
                            <div class="col-12 m-0 mt-1 p-0">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="btn-group w-100">
                                            <input type="radio" name="oldNewSetups" id="onsOld" value="0" class="btn-check" autocomplete="off" ${sSelExist} onclick="showSetup()">
                                            <label class="btn btn-outline-secondary border-dark-subtle ${selLock}" for="onsOld">
                                                <i class="bi bi-journal-text"></i>&nbsp;Modify Existing
                                            </label>
                                            <input type="radio" name="oldNewSetups" id="onsNew" value="1" class="btn-check" autocomplete="off" ${sSelNew} onclick="showSetup()" >
                                            <label class="btn btn-outline-secondary border-dark-subtle border-start-0 ${selLock}" for="onsNew">
                                                <i class="bi bi-journal-plus"></i>&nbsp;Create New
                                            </label>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>
                            </div>
                            <div class="col-12 m-0 mt-2 p-0 ${sViewExist}" id="oldSetups">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="input-group w-100">
                                            <label for="setupSequenceList" class="input-group-text d-none d-xl-inline">
                                                Current Sequences
                                            </label>
                                            <select class="form-select ${selLock}" id="setupSequenceList" name="setupSequenceList" onchange="enableSetupBut()">
                                                <option value="-1" ${sSelExist2}>Select A Sequence</option>
                                                <c:forEach var="sSeq" items="${sessionScope.reqSetupList}">
                                                    <c:choose>
                                                        <c:when test="${sessionScope.SequenceSelectedId==sSeq.getId()}">
                                                            <option value="${sSeq.getId()}" selected>${sSeq.getDescription()}</option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <option value="${sSeq.getId()}">${sSeq.getDescription()}</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </select>
                                            <button type="submit" name="btnTb" value="LS-" id="loadSetup" ${sSelExist3} class="btn btn-outline-dark ${selLock}">Load</button>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>

                            </div>
                            <div class="col-12 m-0 mt-2 p-0 ${sViewNew}" id="newSetups">
                                <div class="row m-0 p-0">
                                    <div class="col-md-1"></div>
                                    <div class="col-12 col-md-10">
                                        <div class="input-group w-100">
                                            <label for="setupDescription" class="input-group-text d-none d-xl-inline">Name</label>
                                            <select class="form-select ${selLock}" id="setupDescription" name="setupDescription">
                                                <c:forEach var="rSeq3" items="${sessionScope.notSetupList}">
                                                    <c:choose>
                                                        <c:when test="${rSeq3.getId()==sessionScope.sSequenceSelected}">
                                                            <option value="${rSeq3.getId()}" selected>(Setup) ${rSeq3.getDescription()}</option></c:when>
                                                        <c:otherwise>
                                                            <option value="${rSeq3.getId()}">(Setup) ${rSeq3.getDescription()}</option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </select>
                                            <button type="submit" name="btnTb" value="NS-" class="btn btn-outline-dark ${selLock}">
                                                Start
                                            </button>
                                        </div>
                                    </div>
                                    <div class="col-md-1"></div>
                                </div>

                            </div>
                        </div>
                    </div>
                </div>
                <!-- **************************   TASK BUILDER ROWS ************************************************************** -->
                <div class="row m-0 p-0 ${showTb}" id="taskBuilder">
                    <div class="col m-0 p-0">
                        <hr>
                        <div class="row mt-1">
                            <div class="col-auto"></div>
                            <div class="col text-center text-secondary fs-5">
                                <i class="bi bi-hammer"></i> Build Sequence Below
                                <a href="ClearGrid25" class="text-danger fs-6">
                                    [clear list]
                                </a>
                            </div>
                            <div class="col-auto">

                            </div>
                        </div>
                        <c:choose>
                            <c:when test="${sessionScope.listBuilder!=null && sessionScope.listBuilder.size()>0}">
                                <c:set var="listSize" value="${sessionScope.lisBuilder.size()}"> </c:set>
                                <c:forEach var="task" items="${sessionScope.listBuilder}" varStatus="tId">
                                    <div class="d-none" id="codeBehind531">
                                        <c:set var="hasTaskShow" value="d-none"> </c:set>
                                        <c:set var="hasTaskHide" value=""> </c:set>
                                        <c:set var="hasTaskLock" value=""> </c:set>
                                        <c:set var="hasTaskColor" value="bg-secondary text-light"> </c:set>
                                        <c:set var="firstOne" value=""> </c:set>
                                        <c:set var="lastOne" value=""> </c:set>
                                        <c:if test="${tId.first}">
                                            <c:set var="firstOne" value="disabled"> </c:set>
                                        </c:if>
                                        <c:if test="${tId.last}">
                                            <c:set var="lastOne" value="disabled"> </c:set>
                                        </c:if>
                                        <c:if test="${task.getTask()!=null && task.getTask().getId()>0}">
                                            <c:set var="hasTaskShow" value=""> </c:set>
                                            <c:set var="hasTaskHide" value="d-none"> </c:set>
                                            <c:set var="hasTaskLock" value="pe-none"> </c:set>
                                            <c:set var="hasTaskColor" value=""> </c:set>
                                        </c:if>
                                        <c:set var="noTasks" value=""> </c:set>
                                        <c:if test="${sessionScope.availableTasks==null || sessionScope.availableTasks.size()==0}">
                                            <c:set var="noTasks" value="d-none"> </c:set>
                                        </c:if>
                                    </div>
                                    <div class="row mt-1 align-items-center">
                                        <div class="col">
                                            <div class="btn-group w-100">
                                                <div class="input-group-text border-dark-subtle rounded-end-0 border-end-0 ${hasTaskColor} ">
                                                    <span class="d-none d-md-inline">Step&nbsp;</span>${tId.count}
                                                </div>
                                                <div class="btn-group-vertical">
                                                    <button type="submit" name="btnTb" value="UP-${tId.index}" ${firstOne} class="btn btn-sm btn-outline-dark border-dark-subtle p-0 ps-1 pe-1 rounded-top-0">
                                                        <i class="bi bi-chevron-compact-up"></i>
                                                    </button>
                                                    <button type="submit" name="btnTb" value="DN-${tId.index}" ${lastOne} class="btn btn-sm btn-outline-dark border-dark-subtle p-0 ps-1 pe-1 rounded-bottom-0">
                                                        <i class="bi bi-chevron-compact-down"></i>
                                                    </button>
                                                </div>
                                                <div class="btn-group-vertical ${hasTaskHide}">
                                                    <input type="radio" value="0" id="tType0-${tId.index}" name="whichRadio${tId.index}" checked class="btn-check" onclick="showHideY(${tId.index})">
                                                    <label for="tType0-${tId.index}" class="btn btn-sm btn-outline-secondary border-dark-subtle rounded-top-0 p-0 ps-2 pe-2 border-start-0">
                                                        enter&nbsp;new
                                                    </label>
                                                    <input type="radio" value="1" id="tType1-${tId.index}" name="whichRadio${tId.index}" class="btn-check" onclick="showHideY(${tId.index})" >
                                                    <label for="tType1-${tId.index}" class="btn btn-sm ${noTasks} btn-outline-secondary border-dark-subtle rounded-bottom-0 p-0 ps-2 pe-2 border-start-0">
                                                        pick&nbsp;from&nbsp;list
                                                    </label>
                                                </div>

                                                <input class="form-control border-dark-subtle rounded-end-0 rounded-start-0 border-start-0 ${hasTaskLock}" id="taskDesc-${tId.index}" name="taskDesc${tId.index}" placeholder="Enter description here" required value="${task.getDescription()}">
                                                <input type="checkbox" class="btn-check" id="saveTask-${tId.index}" name="saveTheTask${tId.index}" value="1">
                                                <label for="saveTask-${tId.index}" id="saveTask1-${tId.index}" class="btn btn-outline-danger border-dark-subtle border-start-0 border-end-0 pt-2 ${hasTaskHide}">
                                                    <i class="bi bi-floppy2-fill"></i>
                                                </label>
                                                <select class="form-select border-dark-subtle border-start-0 rounded-start-0 rounded-end-0 d-none ${hasTaskHide}" id="selTask-${tId.index}" name="selTask${tId.index}" >
                                                    <c:forEach var="task14" items="${sessionScope.availableTasks}">
                                                        <c:choose>
                                                            <c:when test="${task.getTask()!=null && task14.getId()==task.getTask().getId()}">
                                                                <option value="${task14.getId()}" selected>${task14.getDescription()}</option>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <option value="${task14.getId()}">${task14.getDescription()}</option>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </c:forEach>
                                                </select>
                                                <button type="submit" id="addTask-${tId.index}" value="AD-${tId.index}" name="btnTb" class="btn btn-outline-dark border-dark-subtle ps-1 ps-sm-2 ps-lg-3 pe-1 pe-sm-2 pe-lg-3">
                                                    <i class="bi bi-plus-lg"></i>
                                                </button>
                                                <button type="submit" id="delTask-${tId.index}" value="DE-${tId.index}" name="btnTb" class="btn btn-outline-danger border-start-0 ps-1 ps-sm-2 ps-lg-3 pe-1 pe-sm-2 pe-lg-3">
                                                    <i class="bi bi-trash"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="row align-items-center">
                                    <div class="col-2 pe-0 align-middle">
                                        <div class="btn-group w-100" role="group">
                                            <input type="radio" value="0" id="tType0-I" name="whichRadio" class="btn-check" checked onclick="showHideX()">
                                            <label for="tType0-I" class="btn btn-outline-success ">
                                                <span class="d-none d-xl-inline">&nbsp;&nbsp;</span>N<span class="d-none d-xl-inline">ew&nbsp;&nbsp;</span>
                                            </label>
                                            <input type="radio" value="1" id="tType1-I" name="whichRadio" class="btn-check" onclick="showHideX()" >
                                            <label for="tType1-I" class="btn btn-outline-success">
                                                E<span class="d-none d-xl-inline">xisting</span>
                                            </label>
                                        </div>
                                    </div>
                                    <div class="col ps-1">
                                        <div class="input-group h-100">
                                            <label for="taskDesc-I" class="input-group-text">
                                                <span class="d-none d-md-inline">Task&nbsp;</span>#1
                                            </label>
                                            <input class="form-control" id="taskDesc-I" name="taskDescI" placeholder="Enter description here">
                                            <select class="d-none form-select text-danger" id="selTask-I" name="selTaskI" onchange="enableAdd()">
                                                <option value="-1" selected>--Select Task To Include--</option>
                                                <c:forEach var="task" items="${sessionScope.availableTasks}">
                                                    <option value="${task.getId()}">${task.getDescription()}</option>
                                                </c:forEach>
                                            </select>
                                            <input type="checkbox" class="btn-check" id="saveTask-I" name="saveTask1" value="1">
                                            <label for="saveTask-I" id="saveTask1-I" class="btn btn-outline-danger ps-1 pe-1">
                                                <i class="bi bi-floppy"></i>
                                            </label>
                                        </div>
                                    </div>
                                    <div class="col-auto m-0 m-sm-1 p-1 p-md-2">
                                        <div class="row m-0 p-0">
                                            <div class="col m-0 p-0">
                                                <button type="submit" disabled class="btn btn-sm btn-outline-success mt-0 mb-0 pt-0 pb-0">
                                                    <i class="bi bi-chevron-compact-up"></i>
                                                </button>
                                            </div>
                                        </div>
                                        <div class="row m-0 p-0">
                                            <div class="col m-0 p-0">
                                                <button type="submit" disabled class="btn btn-sm btn-outline-success mt-0 mb-0 pt-0 pb-0">
                                                    <i class="bi bi-chevron-compact-down"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-auto m-0 m-sm-1 p-1 p-md-2">
                                        <button type="submit" id="addTask-I" value="ST-I" name="btnTb" class="btn btn-primary h-100 ps-1 ps-sm-2 ps-lg-3 pe-1 pe-sm-2 pe-lg-3 ">
                                            <i class="bi bi-plus"></i>
                                        </button>
                                    </div>
                                    <div class="col-auto m-0 m-sm-1 p-1 p-md-2">
                                        <button type="submit" id="delTask-I" value="DE-I" name="btnTb" class="btn btn-outline-danger h-100 ps-1 ps-sm-2 ps-lg-3 pe-1 pe-sm-2 pe-lg-3" disabled>
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </div>
                                </div>
                            </c:otherwise>
                        </c:choose>
                        <div class="row mt-1 align-items-center">
                            <div class="col-md"></div>
                            <div class="col-12 col-md-auto">
                                <script>
                                    function releaseTextBox() {
                                        let ele1 = document.getElementsByName('activityType');
                                        let ele2 = document.getElementsByName('oldNewTicket');
                                        let tb33 = document.getElementById('ticketDescription1');
                                        if (ele1[0].checked && ele2[1].checked && tb33.value === "") {
                                            tb33.required = true;
                                            tb33.classList.remove("pe-none");
                                        }

                                    }
                                </script>
                                <button type="submit" class="btn btn-dark w-100" name="btnTb" value="CH-" onmouseover="releaseTextBox()">
                                    <i class="bi bi-ui-checks"></i>&nbsp;Add/Modify Sequence
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
        <div class="col-lg-1 col-xl-2"></div>
    </div>
</div>
</body>
</html>
