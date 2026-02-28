<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- billingGrid25.jsp — shared billing variance table fragment
     Expects request attributes:
       billingGridItems  = List<EmployerVariance> or List<EmployeeVariance>
       billingGridMode   = "employer" | "employee" | "external"
--%>
<c:if test="${empty billingGridItems}">
    <div class="text-center text-muted py-4">
        <i class="bi bi-inbox" style="font-size: 2rem;"></i>
        <p class="mt-2 mb-0">No billing data for this month.</p>
    </div>
</c:if>
<c:if test="${not empty billingGridItems}">
<div class="table-responsive">
    <table class="table table-sm table-hover mb-0" id="billingTable" style="font-size: 0.82rem;">
        <thead class="table-light">
            <tr>
                <th class="ps-2" style="min-width: 180px;">Name</th>
                <th class="text-center" style="width: 62px;">COBRA</th>
                <th class="text-center" style="width: 62px;">FSA</th>
                <th class="text-center" style="width: 62px;">HRA</th>
                <th class="text-center" style="width: 62px;">Dual</th>
                <th class="text-center" style="width: 62px;">Transit</th>
                <th class="text-center" style="width: 62px;">HSA</th>
                <th class="text-center" style="width: 62px;">Direct</th>
                <th class="text-center" style="width: 62px;">Retiree</th>
                <th class="text-center" style="width: 62px;">LSA</th>
            </tr>
        </thead>
        <tbody id="billingTableBody">
            <c:forEach var="item" items="${billingGridItems}" varStatus="loop">
                <%-- Build data-name for search filtering --%>
                <c:choose>
                    <c:when test="${billingGridMode == 'employer'}">
                        <c:set var="rowName" value="${item.getEmployer().getEmployerName()}" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="rowName" value="${item.getEmployee().getLastName()}, ${item.getEmployee().getFirstName()}" />
                    </c:otherwise>
                </c:choose>

                <%-- Detect any change across all 9 benefit types --%>
                <c:set var="hasChange" value="${item.getCobraNet()!=0 || item.getFsaNet()!=0 || item.getHraNet()!=0
                    || item.getDualNet()!=0 || item.getTransitNet()!=0 || item.getHsaNet()!=0
                    || item.getDirectNet()!=0 || item.getRetireeNet()!=0 || item.getLsaNet()!=0}" />

                <tr data-name="${rowName}">
                    <%-- NAME COLUMN --%>
                    <td class="ps-2 position-relative">
                        <c:choose>
                            <c:when test="${billingGridMode == 'employer'}">
                                <form method="post" action="BillingAction" class="d-inline mb-0">
                                    <input type="hidden" name="action" value="drillDown">
                                    <button type="submit" name="employerId" value="${item.getEmployer().getId()}"
                                            class="btn btn-link btn-sm text-start p-0 text-uppercase text-decoration-none fw-semibold text-ssa"
                                            style="font-size: 0.82rem;">
                                        ${item.getEmployer().getEmployerName()}
                                    </button>
                                </form>
                            </c:when>
                            <c:when test="${billingGridMode == 'employee'}">
                                <span class="text-uppercase fw-semibold">
                                    ${item.getEmployee().getLastName()}, ${item.getEmployee().getFirstName()}
                                </span>
                                <c:if test="${item.getEmployee().getCobraStatusId()==2}">
                                    <span class="badge bg-warning text-dark ms-1" style="font-size: 0.65rem;">QB</span>
                                </c:if>
                                <c:if test="${item.getEmployee().getCobraStatusId()==3}">
                                    <span class="badge bg-info ms-1" style="font-size: 0.65rem;">COBRA</span>
                                </c:if>
                            </c:when>
                            <c:otherwise>
                                <span class="text-uppercase">
                                    ${item.getEmployee().getLastName()}, ${item.getEmployee().getFirstName()}
                                </span>
                                <c:if test="${item.getEmployee().getCobraStatusId()==2}">
                                    <span class="badge bg-warning text-dark ms-1" style="font-size: 0.65rem;">QB</span>
                                </c:if>
                                <c:if test="${item.getEmployee().getCobraStatusId()==3}">
                                    <span class="badge bg-info ms-1" style="font-size: 0.65rem;">COBRA</span>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                        <c:if test="${hasChange}">
                            <span class="position-absolute bg-danger rounded-circle"
                                  style="width: 8px; height: 8px; top: 50%; right: 4px; transform: translateY(-50%);"></span>
                        </c:if>
                    </td>

                    <%-- COBRA --%>
                    <td class="text-center position-relative">
                        ${item.getCobraCurrent()}
                        <c:if test="${item.getCobraNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getCobraNet()}</span>
                        </c:if>
                    </td>

                    <%-- FSA --%>
                    <td class="text-center position-relative">
                        ${item.getFsaCurrent()}
                        <c:if test="${item.getFsaNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getFsaNet()}</span>
                        </c:if>
                    </td>

                    <%-- HRA --%>
                    <td class="text-center position-relative">
                        ${item.getHraCurrent()}
                        <c:if test="${item.getHraNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getHraNet()}</span>
                        </c:if>
                    </td>

                    <%-- Dual --%>
                    <td class="text-center position-relative">
                        ${item.getDualCurrent()}
                        <c:if test="${item.getDualNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getDualNet()}</span>
                        </c:if>
                    </td>

                    <%-- Transit --%>
                    <td class="text-center position-relative">
                        ${item.getTranCurrent()}
                        <c:if test="${item.getTransitNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getTransitNet()}</span>
                        </c:if>
                    </td>

                    <%-- HSA --%>
                    <td class="text-center position-relative">
                        ${item.getHsaCurrent()}
                        <c:if test="${item.getHsaNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getHsaNet()}</span>
                        </c:if>
                    </td>

                    <%-- Direct --%>
                    <td class="text-center position-relative">
                        ${item.getDirectCurrent()}
                        <c:if test="${item.getDirectNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getDirectNet()}</span>
                        </c:if>
                    </td>

                    <%-- Retiree --%>
                    <td class="text-center position-relative">
                        ${item.getRetireeCurrent()}
                        <c:if test="${item.getRetireeNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getRetireeNet()}</span>
                        </c:if>
                    </td>

                    <%-- LSA --%>
                    <td class="text-center position-relative">
                        ${item.getLsaCurrent()}
                        <c:if test="${item.getLsaNet()!=0}">
                            <span class="badge rounded-pill bg-danger position-absolute" style="font-size: 0.6rem; top: 1px; right: 1px;">${item.getLsaNet()}</span>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
</c:if>
