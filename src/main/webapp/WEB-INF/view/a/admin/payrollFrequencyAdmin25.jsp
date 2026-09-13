<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Payroll Frequencies</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
        }
        .toolbar {
            padding: 0.65rem 1rem;
            background: #fff; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.75rem;
        }
        .toolbar .t-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0;
        }
        .rc-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .map-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .map-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .map-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: middle; }
        .map-table tbody tr:hover { background: #f8f9fa; }
        .row-inactive td { color: #adb5bd; }
        .badge-on { background: #198754; color: #fff; }
        .badge-off { background: #adb5bd; color: #fff; }
        .badge-approved { background: #0d5681; color: #fff; }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
        .form-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-top: 0.9rem; font-size: 0.85rem;
        }
        .form-card label { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em;
            color: #6c757d; font-weight: 600; margin-bottom: 0.15rem; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-calendar-week me-1"></i>Payroll Frequencies
        </h1>
    </div>

    <div class="rc-body">

        <c:if test="${not empty sessionScope.payrollFrequencyMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.payrollFrequencyMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="payrollFrequencyMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.payrollFrequencyError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.payrollFrequencyError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="payrollFrequencyError" scope="session"/>
        </c:if>

        <%-- This is the curated, employer-independent registry the enrollment matrix dropdown
             reads (once wired to it -- a separate build). enrollment_approved is the flag that
             decides what the matrix offers; a new row defaults to not-approved. --%>
        <div class="status-card">
            Rows marked <strong>Enrollment Approved</strong> below are the curated set the
            enrollment matrix dropdown offers. The two sentinel values
            <c:forEach var="rc" items="${reservedCodes}" varStatus="rcs">
                <span class="mono"><c:out value="${rc}"/></span><c:if test="${!rcs.last}">, </c:if>
            </c:forEach>
            are appended by the matrix itself and are not rows here — they cannot be entered as a
            code below.
        </div>

        <c:choose>
            <c:when test="${empty rows}">
                <div class="empty-state">
                    No payroll frequencies yet. Add one below.
                </div>
            </c:when>
            <c:otherwise>
                <table class="map-table">
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Code</th>
                        <th>Label</th>
                        <th>Periods / Year</th>
                        <th>Summit Schedule Name</th>
                        <th>Application Value</th>
                        <th>Enrollment Approved</th>
                        <th>Active</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="r" items="${rows}">
                        <tr class="${r.active ? '' : 'row-inactive'}">
                            <td><c:out value="${r.sortOrder}"/></td>
                            <td class="mono"><c:out value="${r.code}"/></td>
                            <td><c:out value="${r.label}"/></td>
                            <td class="mono"><c:out value="${r.periodsPerYear}"/></td>
                            <td class="mono"><c:out value="${r.summitScheduleName}"/></td>
                            <td class="mono"><c:out value="${r.applicationValue}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${r.enrollmentApproved}"><span class="badge badge-approved">Approved</span></c:when>
                                    <c:otherwise><span class="badge badge-off">Not approved</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${r.active}"><span class="badge badge-on">Active</span></c:when>
                                    <c:otherwise><span class="badge badge-off">Inactive</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-secondary py-0 px-2"
                                   href="${pageContext.request.contextPath}/PayrollFrequencyAdmin?editId=${r.id}">Edit</a>
                                <form method="post" action="${pageContext.request.contextPath}/PayrollFrequencyAdmin"
                                      class="d-inline"
                                      onsubmit="return confirm('Remove this payroll frequency?');">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${r.id}">
                                    <button type="submit" class="btn btn-sm btn-outline-danger py-0 px-2">Remove</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>

        <%-- Add / edit. One form: pre-filled when the servlet resolved an "editing" row, blank
             otherwise. --%>
        <div class="form-card">
            <div class="fw-semibold mb-2" style="color: var(--ssa, #0d5681);">
                <c:choose>
                    <c:when test="${not empty editing}">Edit payroll frequency <c:out value="${editing.id}"/></c:when>
                    <c:otherwise>Add a payroll frequency</c:otherwise>
                </c:choose>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/PayrollFrequencyAdmin">
                <input type="hidden" name="action" value="save">
                <c:if test="${not empty editing}">
                    <input type="hidden" name="id" value="${editing.id}">
                </c:if>

                <div class="row g-2">
                    <div class="col-md-2">
                        <label for="code">Code</label>
                        <input type="text" class="form-control form-control-sm mono"
                               id="code" name="code" required
                               value="<c:out value="${not empty editing ? editing.code : ''}"/>">
                    </div>
                    <div class="col-md-3">
                        <label for="label">Label</label>
                        <input type="text" class="form-control form-control-sm"
                               id="label" name="label" required
                               value="<c:out value="${not empty editing ? editing.label : ''}"/>">
                    </div>
                    <div class="col-md-2">
                        <label for="periodsPerYear">Periods / Year</label>
                        <input type="number" min="1" step="1" class="form-control form-control-sm"
                               id="periodsPerYear" name="periodsPerYear"
                               value="${not empty editing ? editing.periodsPerYear : ''}">
                    </div>
                    <div class="col-md-2">
                        <label for="sortOrder">Order</label>
                        <input type="number" step="1" class="form-control form-control-sm"
                               id="sortOrder" name="sortOrder"
                               value="${not empty editing ? editing.sortOrder : 0}">
                    </div>
                    <div class="col-md-1 d-flex align-items-end">
                        <div class="form-check mb-1">
                            <input class="form-check-input" type="checkbox" id="active" name="active"
                                   <c:if test="${empty editing or editing.active}">checked</c:if>>
                            <label class="form-check-label" for="active" style="text-transform:none;">Active</label>
                        </div>
                    </div>
                    <div class="col-md-2 d-flex align-items-end">
                        <div class="form-check mb-1">
                            <input class="form-check-input" type="checkbox" id="enrollmentApproved" name="enrollmentApproved"
                                   <c:if test="${not empty editing and editing.enrollmentApproved}">checked</c:if>>
                            <label class="form-check-label" for="enrollmentApproved" style="text-transform:none;">Enrollment Approved</label>
                        </div>
                    </div>
                </div>
                <div class="row g-2 mt-1">
                    <div class="col-md-4">
                        <label for="summitScheduleName">Summit Schedule Name</label>
                        <input type="text" class="form-control form-control-sm"
                               id="summitScheduleName" name="summitScheduleName"
                               value="<c:out value="${not empty editing ? editing.summitScheduleName : ''}"/>">
                    </div>
                    <div class="col-md-4">
                        <label for="applicationValue">Application Value</label>
                        <input type="text" class="form-control form-control-sm"
                               id="applicationValue" name="applicationValue"
                               value="<c:out value="${not empty editing ? editing.applicationValue : ''}"/>">
                    </div>
                    <div class="col-md-4 d-flex align-items-end">
                        <button type="submit" class="btn btn-sm btn-primary">
                            <c:choose>
                                <c:when test="${not empty editing}">Save Changes</c:when>
                                <c:otherwise>Add Payroll Frequency</c:otherwise>
                            </c:choose>
                        </button>
                        <c:if test="${not empty editing}">
                            <a class="btn btn-sm btn-outline-secondary ms-2"
                               href="${pageContext.request.contextPath}/PayrollFrequencyAdmin">Cancel</a>
                        </c:if>
                    </div>
                </div>
            </form>
        </div>

    </div>
</div>

</body>
</html>
