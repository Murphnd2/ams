<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Summit Employer Flags</title>
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
        .map-table td.flag-cell { text-align: center; }
        .map-table tbody tr:hover { background: #f8f9fa; }
        .flag-on { color: #198754; font-weight: 700; }
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
            <i class="bi bi-toggles me-1"></i>Summit Employer Flags
        </h1>
        <a class="ms-auto small" href="${pageContext.request.contextPath}/SummitPlanTemplateAdmin">Summit Plan Templates &rarr;</a>
    </div>

    <div class="rc-body">

        <c:if test="${not empty sessionScope.summitEmployerFlagMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.summitEmployerFlagMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="summitEmployerFlagMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.summitEmployerFlagError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.summitEmployerFlagError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="summitEmployerFlagError" scope="session"/>
        </c:if>

        <div class="status-card">
            Each service item below sets these Summit administration flags when elected on a setup.
            The employer file emits the union across all elected items, as explicit true/false (D46).
            Not yet read by the employer file &mdash; that change waits on SDX-27.
        </div>

        <c:choose>
            <c:when test="${empty mappings}">
                <div class="empty-state">
                    No employer flag mappings yet. Add one below.
                </div>
            </c:when>
            <c:otherwise>
                <table class="map-table">
                    <thead>
                    <tr>
                        <th>Service item</th>
                        <th class="text-center">Enable CDH<br/>Administration</th>
                        <th class="text-center">Enable COBRA<br/>Administration</th>
                        <th class="text-center">Enable Retiree Billing<br/>Administration</th>
                        <th class="text-center">Enable Direct Bill<br/>Administration</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="m" items="${mappings}">
                        <tr>
                            <td>
                                <span class="mono">${m.serviceItemId}</span>
                                <c:if test="${not empty serviceItemDescriptions[m.id]}">
                                    &mdash; <c:out value="${serviceItemDescriptions[m.id]}"/>
                                </c:if>
                            </td>
                            <td class="flag-cell"><c:choose><c:when test="${m.enableCdh}"><span class="flag-on">&#10003;</span></c:when><c:otherwise>&nbsp;</c:otherwise></c:choose></td>
                            <td class="flag-cell"><c:choose><c:when test="${m.enableCobra}"><span class="flag-on">&#10003;</span></c:when><c:otherwise>&nbsp;</c:otherwise></c:choose></td>
                            <td class="flag-cell"><c:choose><c:when test="${m.enableRetireeBilling}"><span class="flag-on">&#10003;</span></c:when><c:otherwise>&nbsp;</c:otherwise></c:choose></td>
                            <td class="flag-cell"><c:choose><c:when test="${m.enableDirectBill}"><span class="flag-on">&#10003;</span></c:when><c:otherwise>&nbsp;</c:otherwise></c:choose></td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-secondary py-0 px-2"
                                   href="${pageContext.request.contextPath}/SummitEmployerFlagAdmin?editId=${m.id}">Edit</a>
                                <form method="post" action="${pageContext.request.contextPath}/SummitEmployerFlagAdmin"
                                      class="d-inline"
                                      onsubmit="return confirm('Remove this flag mapping? Elected setups will no longer contribute these flags to the employer file.');">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${m.id}">
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
             otherwise. Same shape as summitPlanTemplateAdmin25.jsp. --%>
        <div class="form-card">
            <div class="fw-semibold mb-2" style="color: var(--ssa, #0d5681);">
                <c:choose>
                    <c:when test="${not empty editing}">Edit mapping <c:out value="${editing.id}"/></c:when>
                    <c:otherwise>Add a mapping</c:otherwise>
                </c:choose>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/SummitEmployerFlagAdmin">
                <input type="hidden" name="action" value="save">
                <c:if test="${not empty editing}">
                    <input type="hidden" name="id" value="${editing.id}">
                </c:if>

                <div class="row g-2 align-items-end">
                    <div class="col-md-4">
                        <label for="serviceItemId">Service item</label>
                        <%-- The id is shown alongside the description for the same reason
                             summitPlanTemplateAdmin25.jsp shows it: description is a copy of a
                             LOS short text taken at creation (T190) and two items can read
                             identically; the id is the only thing that distinguishes them. --%>
                        <select class="form-select form-select-sm" id="serviceItemId" name="serviceItemId" required>
                            <option value="">— choose —</option>
                            <c:forEach var="si" items="${serviceItems}">
                                <option value="${si.id}"
                                        <c:if test="${not empty editing and editing.serviceItemId eq si.id}">selected</c:if>>
                                    <c:out value="${si.id}"/> — <c:out value="${si.description}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="enableCdh" name="enableCdh"
                                   <c:if test="${not empty editing and editing.enableCdh}">checked</c:if>>
                            <label class="form-check-label" for="enableCdh" style="text-transform:none;">Enable CDH Administration</label>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="enableCobra" name="enableCobra"
                                   <c:if test="${not empty editing and editing.enableCobra}">checked</c:if>>
                            <label class="form-check-label" for="enableCobra" style="text-transform:none;">Enable COBRA Administration</label>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="enableRetireeBilling" name="enableRetireeBilling"
                                   <c:if test="${not empty editing and editing.enableRetireeBilling}">checked</c:if>>
                            <label class="form-check-label" for="enableRetireeBilling" style="text-transform:none;">Enable Retiree Billing Administration</label>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="enableDirectBill" name="enableDirectBill"
                                   <c:if test="${not empty editing and editing.enableDirectBill}">checked</c:if>>
                            <label class="form-check-label" for="enableDirectBill" style="text-transform:none;">Enable Direct Bill Administration</label>
                        </div>
                    </div>
                </div>

                <div class="mt-2">
                    <button type="submit" class="btn btn-sm btn-ssa">
                        <c:choose><c:when test="${not empty editing}">Save</c:when><c:otherwise>Add</c:otherwise></c:choose>
                    </button>
                    <c:if test="${not empty editing}">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${pageContext.request.contextPath}/SummitEmployerFlagAdmin">Cancel</a>
                    </c:if>
                </div>
            </form>
        </div>

    </div>
</div>
</body>
</html>
