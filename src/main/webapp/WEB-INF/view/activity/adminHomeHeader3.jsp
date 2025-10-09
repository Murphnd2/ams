<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<script>
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    })
</script>
<div class="p-2 pt-1 border border-dark rounded w-100 mt-2 mb-2 text-light bg-dark fw-bold fs-3 align-items-center text-center">
    <form action="filterActivities" method="post">
        <div class="d-none">
            <c:set var="alpha" value=""></c:set>
            <c:set var="calen" value="checked"></c:set>
            <c:if test="${sessionScope.qAlpha}">
                <c:set var="alpha" value="checked"></c:set>
                <c:set var="calen" value=""></c:set>
            </c:if>

            <c:choose>
                <c:when test="${sessionScope.qWhoFilter==3}">
                    <c:set var="wAll" value=""></c:set>
                    <c:set var="wMePlus" value=""></c:set>
                    <c:set var="wMe" value=""></c:set>
                    <c:set var="wPlus" value="checked"></c:set>
                </c:when>
                <c:when test="${sessionScope.qWhoFilter==0}">
                    <c:set var="wAll" value="checked"></c:set>
                    <c:set var="wMePlus" value=""></c:set>
                    <c:set var="wMe" value=""></c:set>
                    <c:set var="wPlus" value=""></c:set>
                </c:when>
                <c:when test="${sessionScope.qWhoFilter==2}">
                    <c:set var="wAll" value=""></c:set>
                    <c:set var="wMePlus" value=""></c:set>
                    <c:set var="wMe" value="checked"></c:set>
                    <c:set var="wPlus" value=""></c:set>
                </c:when>
                <c:otherwise>
                    <c:set var="wAll" value=""></c:set>
                    <c:set var="wMePlus" value="checked"></c:set>
                    <c:set var="wMe" value=""></c:set>
                    <c:set var="wPlus" value=""></c:set>
                </c:otherwise>
            </c:choose>
            <c:set var="vr" value=""></c:set>
            <c:if test="${sessionScope.vRn.equals(\"1\")}">
                <c:set var="vr" value="checked"></c:set>
            </c:if>
            <c:set var="vs" value=""></c:set>
            <c:if test="${sessionScope.vSt.equals(\"3\")}">
                <c:set var="vs" value="checked"></c:set>
            </c:if>
            <c:set var="vt" value=""></c:set>
            <c:if test="${sessionScope.vTk.equals(\"5\")}">
                <c:set var="vt" value="checked"></c:set>
            </c:if>
            <c:choose>
                <c:when test="${sessionScope.qNwf.equals(\"0\")}">
                    <c:set var="qou" value=""></c:set>
                    <c:set var="qnc" value=""></c:set>
                </c:when>
                <c:when test="${sessionScope.qNwf.equals(\"2\")}">
                    <c:set var="qou" value=""></c:set>
                    <c:set var="qnc" value="checked"></c:set>
                </c:when>
                <c:when test="${sessionScope.qNwf.equals(\"3\")}">
                    <c:set var="qou" value="checked"></c:set>
                    <c:set var="qnc" value=""></c:set>
                </c:when>
                <c:otherwise>
                    <c:set var="qou" value="checked"></c:set>
                    <c:set var="qnc" value="checked"></c:set>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="row align-items-center">
            <div class="col-auto m-0 me-1">
                <div class="btn-group" role="group">
                    <button type="submit" class="btn  btn-outline-light">
                        <i class="bi bi-filter"></i> FILTER
                    </button>
                    <button type="button" class="btn  btn-outline-light" data-bs-target="#extraFilter" data-bs-toggle="collapse" >
                       <i class="bi bi-caret-down"></i>
                    </button>
                </div>
            </div>
            <div class="col fs-3 text-start text-md-center fw-bold text-white ps-0 ms-0">
                <span class="d-none d-md-inline">
                    <i class="bi bi-activity"></i>&nbsp;
                </span>
                Activities
            </div>
            <div class="col-auto">
                <%--<form action="ViewThisActivity" method="post">
                    <div class="input-group input-group-sm">
                        <select class="form-select border-warning border-end-0 rounded-end-0" name="activitySelected" style="max-width: 200px">
                            <option value="0">Pick Activity</option>
                        </select>
                        <button type="submit" class="btn btn-outline-warning">GO</button>
                    </div>
                </form>--%>
            </div>
        </div>
        <div class="row collapse" id="extraFilter">
            <div class="col">
                <div class="row m-0 p-0">
                    <div class="col-auto m-0 p-0 text-left">
                        <div class="btn-group" role="group">
                            <input type="checkbox" class="btn-check" name="vRenew" id="vRenew" autocomplete="off" value="1" ${vr}>
                            <label class="btn btn-sm btn-outline-primary" for="vRenew">
                                <i class="bi bi-repeat"></i> R
                            </label>
                            <input type="checkbox" class="btn-check" name="vSetup" id="vSetup" autocomplete="off" value="3" ${vs}>
                            <label class="btn btn-sm btn-outline-secondary" for="vSetup">
                                <i class="bi bi-buildings"></i> S
                            </label>
                            <input type="checkbox" class="btn-check" name="vTicket" id="vTicket" autocomplete="off" value="5" ${vt}>
                            <label class="btn btn-sm btn-outline-info" for="vTicket">
                                <i class="bi bi-ticket-detailed"></i> T
                            </label>
                            <button type="button" class="btn btn-sm btn-outline-info pe-none">Activity Type</button>
                        </div>
                    </div>
                    <div class="col"></div>
                    <div class="col-auto m-0 p-0 text-center">
                        <div class="btn-group m-0" role="group">
                            <button type="button" class="btn btn-sm btn-outline-danger pe-none">Attention</button>
                            <input type="checkbox" class="btn-check" name="fOnUs" id="fOnUs" autocomplete="off" value="1" ${qou} >
                            <label class="btn btn-sm btn-outline-danger" for="fOnUs" data-bs-toggle="tooltip" title="Show Activities Waiting On Me!">
                                <i class="bi bi-stack-overflow"></i>
                            </label>
                            <input type="checkbox" class="btn-check" name="fCall" id="fCall" autocomplete="off" value="1" ${qnc}>
                            <label class="btn btn-sm btn-outline-danger" for="fCall" data-bs-toggle="tooltip" title="Show Activities Needing Contact!">
                                <i class="bi bi-telephone"></i>
                            </label>
                        </div>
                    </div>
                </div>
                <div class="row m-0 p-0">
                    <div class="col-auto m-0 p-0 text-left">
                        <div class="btn-group" role="group">
                            <input type="radio" class="btn-check" name="whoFilter" id="whoAll" autocomplete="off" value="0" ${wAll}>
                            <label class="btn btn-sm btn-outline-warning" for="whoAll" data-bs-toggle="tooltip" title="Show ALL Open Activities">
                                &nbsp;<i class="bi bi-people"></i>
                            </label>
                            <input type="radio" class="btn-check" name="whoFilter" id="whoAllMe" autocomplete="off" value="1" ${wMePlus}>
                            <label class="btn btn-sm btn-outline-warning" for="whoAllMe"  data-bs-toggle="tooltip" title="Show all MY Open Activities">
                                <i class="bi bi-person-plus"></i>
                            </label>
                            <input type="radio" class="btn-check" name="whoFilter" id="whoOnlyMe" autocomplete="off" value="2" ${wMe}>
                            <label class="btn btn-sm btn-outline-warning" for="whoOnlyMe" data-bs-toggle="tooltip" title="Only Activities Assigned to Me">
                                <i class="bi bi-person"></i>
                            </label>
                            <input type="radio" class="btn-check" name="whoFilter" id="whoHelp" autocomplete="off" value="3" ${wPlus}>
                            <label class="btn btn-sm btn-outline-warning" for="whoHelp" data-bs-toggle="tooltip" title="Only Activities with Delegated Tasks">
                                <i class="bi bi-person-check"></i>
                            </label>
                            <button type="button" class="btn btn-sm btn-outline-warning pe-none">&nbsp;Ownership&nbsp;&nbsp;</button>
                        </div>
                    </div>
                    <div class="col"></div>
                    <div class="col-auto m-0 p-0 text-center">
                        <div class="btn-group m-0" role="group">
                            <button type="button" class="btn btn-sm btn-outline-success pe-none">&nbsp;&nbsp;Sort By&nbsp;&nbsp;</button>
                            <input type="radio" class="btn-check" name="fAlpha" id="fCal" autocomplete="off" value="0" ${calen}>
                            <label class="btn btn-sm btn-outline-success" for="fCal" data-bs-toggle="tooltip" title="Filter By Due Date">
                                <i class="bi bi-calendar2"></i>
                            </label>
                            <input type="radio" class="btn-check" name="fAlpha" id="fAlpha" autocomplete="off" value="1" ${alpha} >
                            <label class="btn btn-sm btn-outline-success" for="fAlpha"  data-bs-toggle="tooltip" title="Filter Alphabetically">
                                <i class="bi bi-alphabet-uppercase"></i>
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </form>
</div>
<div class="row overflow-auto" style="max-height: 700px">
    <div class="col">
        <c:import url="/WEB-INF/view/activity/activityListV1.jsp"></c:import>
    </div>
</div>