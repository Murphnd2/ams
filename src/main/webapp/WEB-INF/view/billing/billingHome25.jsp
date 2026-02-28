<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Billing Home</title>
    <style>
        .billing-month-badge {
            font-size: 0.95rem;
            padding: 0.4rem 1rem;
            letter-spacing: 0.03em;
        }
        #billingSearch {
            max-width: 220px;
            font-size: 0.8rem;
        }
        .toolbar-link {
            color: white;
            text-decoration: none;
            font-size: 0.8rem;
            opacity: 0.85;
        }
        .toolbar-link:hover {
            color: white;
            opacity: 1;
            text-decoration: underline;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <%-- Month selector bar --%>
    <div class="d-flex align-items-center gap-3 mt-2 mb-2">
        <span class="badge bg-danger billing-month-badge text-uppercase">
            <fmt:formatDate value="${sessionScope.billingMonth.getFullDate()}" pattern="MMMM yyyy"/>
        </span>
        <form method="post" action="BillingAction" class="d-print-none mb-0">
            <input type="hidden" name="action" value="changeMonth">
            <div class="input-group input-group-sm">
                <span class="input-group-text" style="font-size: 0.8rem;">Month</span>
                <select class="form-select form-select-sm" name="monthList" style="font-size: 0.8rem; min-width: 160px;">
                    <c:forEach var="month" items="${sessionScope.billingMonths}">
                        <option value="${month.getMonthId()}"
                            ${sessionScope.billingMonth.getMonthId()==month.getMonthId() ? 'selected' : ''}>
                            <fmt:formatDate value="${month.getFullDate()}" pattern="MMMM yyyy"/>
                        </option>
                    </c:forEach>
                </select>
                <button type="submit" class="btn btn-sm btn-altSsa">Go</button>
            </div>
        </form>
    </div>

    <%-- Main card --%>
    <div class="card border-0 shadow-sm">

        <%-- Toolbar header --%>
        <div class="hdr-bar d-flex align-items-center justify-content-between flex-wrap gap-2">
            <div class="d-flex align-items-center gap-2">
                <c:choose>
                    <c:when test="${sessionScope.billingView == 1}">
                        <i class="bi bi-building"></i>
                        <span class="text-uppercase fw-bold" style="font-size: 0.9rem;">
                            ${sessionScope.currentBillingEmployer.getEmployerName()}
                        </span>
                        <a href="BillingAction?action=back" class="toolbar-link d-print-none">(Back)</a>
                        <a href="EmailBillingToEmployer" class="toolbar-link d-print-none">
                            <i class="bi bi-envelope"></i> Email
                        </a>
                    </c:when>
                    <c:otherwise>
                        <i class="bi bi-currency-dollar"></i>
                        <span class="fw-bold" style="font-size: 0.9rem;">Employer Summary</span>
                        <c:if test="${sessionScope.changeOnlyBilling == 'Y'}">
                            <span class="badge bg-warning text-dark ms-1" style="font-size: 0.7rem;">Changes Only</span>
                            <a href="BillingAction?action=reset" class="toolbar-link d-print-none">[Reset]</a>
                        </c:if>
                        <c:if test="${sessionScope.changeOnlyBilling != 'Y'}">
                            <a href="BillingAction?action=changesOnly" class="toolbar-link d-print-none">[Filter Changes]</a>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="d-print-none">
                <input type="text" id="billingSearch" class="form-control form-control-sm"
                       placeholder="Search..." oninput="filterBillingTable()">
            </div>
        </div>

        <%-- Shared billing grid --%>
        <c:import url="/WEB-INF/view/billing/billingGrid25.jsp"/>

    </div>
</div>

<script>
    function filterBillingTable() {
        var query = document.getElementById('billingSearch').value.toLowerCase();
        var rows = document.querySelectorAll('#billingTableBody tr');
        for (var i = 0; i < rows.length; i++) {
            var name = (rows[i].getAttribute('data-name') || '').toLowerCase();
            rows[i].style.display = name.indexOf(query) !== -1 ? '' : 'none';
        }
    }
</script>
</body>
</html>
