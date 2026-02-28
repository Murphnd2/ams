<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${sessionScope.validEmployer.getEmployerName()} - Billing Detail</title>
    <style>
        body { background-color: #f5f7fa; }
        .er-brand {
            background: linear-gradient(135deg, #0d5681 0%, #0a4469 100%);
            color: white;
            padding: 1.25rem 1.5rem;
            border-radius: 0 0 8px 8px;
        }
        .er-brand h3 { margin-bottom: 0.25rem; }
        .er-brand .subtitle { opacity: 0.8; font-size: 0.85rem; }
        #billingSearch {
            max-width: 260px;
            font-size: 0.8rem;
        }
    </style>
</head>
<body>
<div class="container-fluid" style="max-width: 1200px;">

    <%-- Branded header --%>
    <div class="er-brand mb-3">
        <h3 class="fw-bold">${sessionScope.validEmployer.getEmployerName()}</h3>
        <span class="subtitle">Monthly Billing Detail</span>
    </div>

    <%-- Month selector + search --%>
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
        <div class="d-flex align-items-center gap-2">
            <span class="badge bg-altSsa text-white text-uppercase" style="font-size: 0.9rem; padding: 0.4rem 1rem;">
                <fmt:formatDate value="${sessionScope.erBillingMonth.getFullDate()}" pattern="MMMM yyyy"/>
            </span>
            <form method="post" action="EmployerBillingDetail" class="d-print-none mb-0">
                <div class="input-group input-group-sm">
                    <span class="input-group-text" style="font-size: 0.8rem;">Month</span>
                    <select class="form-select form-select-sm" name="selectedMonth" style="font-size: 0.8rem; min-width: 160px;">
                        <c:forEach var="month" items="${sessionScope.erBillingMonthList}">
                            <option value="${month.getMonthId()}"
                                ${sessionScope.erBillingMonth.getMonthId()==month.getMonthId() ? 'selected' : ''}>
                                <fmt:formatDate value="${month.getFullDate()}" pattern="MMMM yyyy"/>
                            </option>
                        </c:forEach>
                    </select>
                    <button type="submit" class="btn btn-sm btn-altSsa">Go</button>
                </div>
            </form>
        </div>
        <div class="d-print-none">
            <input type="text" id="billingSearch" class="form-control form-control-sm"
                   placeholder="Search employees..." oninput="filterBillingTable()">
        </div>
    </div>

    <%-- Billing grid card --%>
    <div class="card border-0 shadow-sm">
        <div class="hdr-bar d-flex align-items-center gap-2" style="background-color: #0d5681;">
            <i class="bi bi-people"></i>
            <span class="fw-bold" style="font-size: 0.9rem;">Employee Billing Detail</span>
        </div>

        <%-- Shared billing grid --%>
        <c:import url="/WEB-INF/view/billing/billingGrid25.jsp"/>
    </div>

    <%-- Footer --%>
    <div class="text-center text-muted mt-4 mb-3" style="font-size: 0.75rem;">
        Superior State Administration
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
