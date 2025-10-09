<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
    <head>
        <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
        <title>Rate Manager</title>
    </head>
    <body>
        <div class="container-fluid">
            <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
            <div class="row">
            <%-- LEFT COLUMN ********************************************************************************* --%>
                <div class="col-lg-3">
                    <%-- ALL RATE LISTING --%>
                    <div class="btn btn-primary w-100 fw-bold fs-3 pe-none mt-2 mb-2">
                        <div class="row">
                            <div class="col">
                                <i class="bi bi-clock-history"></i> All Rates
                            </div>
                            <div class="col-auto"><button type="button" class="btn btn-primary pe-auto" data-bs-toggle="modal" data-bs-target="#addNewRateModal">
                                <i class="bi bi-plus-square"></i>
                            </button>
                            </div>
                        </div>
                    </div>
                    <c:import url="/WEB-INF/view/psp/admin/forms/pspRateListForm.jsp"></c:import>
                    <c:if test="${sessionScope.hasCurrentRate==true&&sessionScope.pspAdminHomeSender==1}">
                    <%-- ASSIGNED AGENCY LISTING --%>
                        <h5>Agencies Assigned</h5>
                        <c:import url="/WEB-INF/view/psp/admin/forms/assignedAgenciesForm.jsp"></c:import>
                    <%-- ACTION TO ASSIGN AGENCY TO SELECTED RATE --%>
                        <h5>Assign Agency</h5>
                        <button type="button" class="btn btn-danger btn-sm w-100 mb-2" data-bs-toggle="modal" data-bs-target="#addAgencyModal">
                           <i class="bi bi-plus-circle-dotted"></i> Add Agency
                        </button>
                        <c:import url="/WEB-INF/view/psp/admin/forms/assignAgencyToRateForm.jsp"></c:import>
                    </c:if>
                </div>
            <%-- ************************************ CENTER COLUMN ****************************************** --%>
                <div class="col">
                    <c:choose>
                        <c:when test="${sessionScope.pspAdminHomeSender==1}">
                            <div class="btn btn-outline-primary w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                                <i class="bi bi-clock-history"></i> ${sessionScope.currentRate.getDescription()}
                            </div>
                            <c:import url="/WEB-INF/view/psp/admin/parts/headerRateTable.jsp"></c:import>
                            <c:import url="/WEB-INF/view/psp/admin/forms/priceListForRateForm.jsp"></c:import>
                        </c:when>
                        <c:when test="${sessionScope.pspAdminHomeSender==2}">
                            <div class="btn btn-outline-primary w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                                ${sessionScope.currentLos.getDescription()} Mods
                            </div>
                            <c:import url="/WEB-INF/view/psp/admin/forms/serviceListForLosForm.jsp"></c:import>
                            <button type="button" class="btn btn-outline-dark w-100" data-bs-toggle="modal" data-bs-target="#linkModalToLos">
                                Add Module to ${sessionScope.currentLos.getDescription()}
                            </button>
                            <c:if test="${sessionScope.hasCurrentModule==true}">
                                <div class="btn btn-outline-primary w-100 mt-2 mb-2 fw-bold fs-5 pe-none">
                                        ${sessionScope.currentModule.getDescription()} Service Items
                                </div>
                                <c:import url="/WEB-INF/view/psp/admin/forms/itemListForServiceForm.jsp"></c:import>
                                <button type="button" class="btn btn-outline-secondary w-100" data-bs-toggle="modal" data-bs-target="#linkServiceItemToModule">
                                     Add Item to ${sessionScope.currentModule.getDescription()}
                                </button>
                            </c:if>
                        </c:when>
                        <c:otherwise>
                            Home
                        </c:otherwise>
                    </c:choose>
                </div>
            <%-- ******************************************************************************** RIGHT COLUMN  --%>
                <div class="col-lg-3">
                    <div class="btn btn-outline-primary w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                        Lines of Service
                    </div>
                    <c:import url="/WEB-INF/view/psp/admin/forms/pspLosListForm.jsp"></c:import>
                </div>
            </div>
        </div>
        <%-- * * * * * * O F F C A N V A S   I T E M S * * * * * *--%>
        <c:import url="/WEB-INF/view/psp/admin/modals/addNewRateModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/buildRateTableModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/addNewServiceModuleModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/addNewPriceItemModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/addNewServiceItemModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/pspAgencyListModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/linkModuleToLosModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/linkServiceItemToModuleModal.jsp"></c:import>
        <c:import url="/WEB-INF/view/psp/admin/modals/addAgencyModal.jsp"></c:import>
    </body>
</html>
