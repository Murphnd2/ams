<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="net.superiorstate.ams.previous.model.activity.checklist.CheckList" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
    <c:when test="${sessionScope.myCurrentItems.size()>0}">
        <c:forEach var="list" items="${sessionScope.myCurrentItems}">
            <div class="d-none" id="codeBehind3">
                <c:choose>
                    <c:when test="${list.dueDate <= Date.valueOf(LocalDate.now().minusDays(14))}">
                        <c:set var="format" value="btn btn-warning fw-bolder text-uppercase"></c:set>
                        <c:set var="butt" value="btn-outline-warning text-dark fw-bold"></c:set>
                    </c:when>
                    <c:when test="${list.dueDate <= Date.valueOf(LocalDate.now().minusDays(1))}">
                        <c:set var="format" value="btn btn-outline-dark text-danger fw-bold"></c:set>
                        <c:set var="butt" value=""></c:set>
                    </c:when>
                    <c:when test="${list.dueDate <= Date.valueOf(LocalDate.now())}">
                        <c:set var="format" value="btn btn-outline-dark"></c:set>
                        <c:set var="butt" value="btn btn-outline-dark"></c:set>
                    </c:when>
                    <c:otherwise>
                        <c:set var="format" value="btn btn-outline-dark text-secondary fw-lighter text-lowercase"></c:set>
                        <c:set var="butt" value=""></c:set>
                    </c:otherwise>
                </c:choose>
            </div>
            <c:choose>
                <c:when test="${list.getToDoCount()==1}">
                        <form method="post" action="CloseSingleItemChecklist" class="mb-1">
                            <div class="accordion" id="a${list.getId()}">
                                <div class="accordion-item">
                                    <div class="accordion-header" id="h${list.getId()}">
                                        <div class="input-group input-group-sm">
                                            <c:choose>
                                                <c:when test="${sessionScope.currentChecklist.getId()==list.getId()}">
                                                    <button type="submit" class="btn btn-dark text-warning pe-none" name="btnCheckList" id="btn2${list.getId()}" value="V-${list.getId()}">
                                                        <i class="bi bi-arrow-left"></i>
                                                    </button>
                                                    <div class="form-control ${format} btn btn-dark text-warning pe-none">
                                                            ${list.getName()}
                                                    </div>
                                                    <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-outline-warning">
                                                        <i class="bi bi-chevron-down"></i>
                                                    </button>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="submit" class="btn btn-outline-dark ${butt}" name="btnCheckList" id="btn${list.getId()}-todo" value="${list.getId()}">
                                                        <i class="bi bi-square"></i>
                                                    </button>
                                                    <button type="submit" class="form-control ${format}" name="btnCheckList" id="btn2${list.getId()}" value="V-${list.getId()}">
                                                            ${list.getName()}
                                                    </button>
                                                    <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-outline-dark">
                                                        <i class="bi bi-chevron-down"></i>
                                                    </button>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                    <div id="collapse${list.getId()}" class="accordion-collapse collapse">
                                        <div class="accordion-body p-0 m-0 mt-1">
                                            <div class="input-group input-group-sm w-100 mb-1">
                                                <c:import url="/WEB-INF/view/general/ddUserList.jsp"></c:import>
                                                <button type="submit" class="btn btn-outline-success" name="btnCheckList" value="R-${list.getId()}">
                                                    <i class="bi bi-box-arrow-in-right"></i>
                                                </button>
                                            </div>
                                            <div class="input-group input-group-sm w-100">
                                                <div class="input-group-text">Due Date</div>
                                                <input type="date" class="form-control" name="newDueDate" value="${list.getDueDate()}">
                                                <button type="submit" class="btn btn-outline-success" name="btnCheckList" value="D-${list.getId()}"><i class="bi bi-calendar-check-fill"></i></button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </form>
                </c:when>
                <c:otherwise>
                    <form method="post" action="ViewSelectedChecklist" class="mb-1">
                        <div class="accordion" id="a${list.getId()}">
                            <div class="accordion-item">
                                <div class="accordion-header" id="h${list.getId()}">
                                    <div class="input-group input-group-sm">
                                        <c:choose>
                                            <c:when test="${sessionScope.currentChecklist.getId()==list.getId()}">
                                                <button type="submit" class="btn btn-dark pe-none text-warning" name="btnCheckList" id="btn${list.getId()}" value="${list.getId()}">
                                                    <i class="bi bi-arrows-fullscreen"></i>
                                                </button>
                                                <div class="form-control ${format} btn btn-dark text-warning pe-none">
                                                        ${list.getName()}
                                                </div>
                                                <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-warning">
                                                    <i class="bi bi-chevron-down"></i>
                                                </button>
                                            </c:when>
                                            <c:when test="${list.hasDelegate()}">

                                                <button type="submit" class="form-control btn btn-outline-primary" name="btnCheckList" id="btn2${list.getId()}" value="${list.getId()}">
                                                    <div class="row m-0 p-0">
                                                        <div class="col-auto m-0 p-0">
                                                            <i class="bi bi-arrows-fullscreen"></i>
                                                        </div>
                                                        <div class="col m-0 p-0">
                                                                ${list.getName()}
                                                        </div>
                                                    </div>
                                                </button>

                                            </c:when>
                                            <c:otherwise>
                                                <button type="submit" class="form-control ${format}" name="btnCheckList" id="btn2${list.getId()}" value="${list.getId()}">
                                                    <div class="row m-0 p-0">
                                                        <div class="col-auto m-0 p-0">
                                                            <i class="bi bi-arrows-fullscreen"></i>
                                                        </div>
                                                        <div class="col m-0 p-0">
                                                                ${list.getName()}
                                                        </div>
                                                    </div>
                                                </button>
                                                <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-outline-dark">
                                                    <i class="bi bi-chevron-down"></i>
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                                <div id="collapse${list.getId()}" class="accordion-collapse collapse">
                                    <div class="accordion-body">
                                        <div class="input-group input-group-sm">
                                            <c:import url="/WEB-INF/view/general/ddUserList.jsp"></c:import>
                                            <button type="submit" class="btn btn-outline-success" name="btnCheckList" value="R-${list.getId()}">
                                                <i class="bi bi-box-arrow-in-right"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </form>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </c:when>
    <c:otherwise>
        <div class="row">
            <div class="col">
                <div class="form-control text-primary fw-bold text-uppercase">
                    Bubkis!
                </div>
            </div>
        </div>
    </c:otherwise>
</c:choose>
<div class="row">
    <div class="col">
        <c:forEach var="list1" items="${sessionScope.myClosedLists}">
            <form method="post" action="ReOpenChecklist" class="mb-1">
                <div class="input-group input-group-sm">
                    <button type="submit" class="btn btn-outline-success"  name="btnCheckList" id="btn2${list1.getId()}" value="${list1.getId()}">
                        <i class="bi bi-x-square"></i>
                    </button>
                    <div class="form-control text-success fw-lighter" style="text-decoration: line-through;font-style: italic">
                            ${list1.getFullName()}
                    </div>
                </div>
            </form>
        </c:forEach>
    </div>
</div>
