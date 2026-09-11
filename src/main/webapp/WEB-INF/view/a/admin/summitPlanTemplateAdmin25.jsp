<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Summit Plan Templates</title>
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
            <i class="bi bi-diagram-2 me-1"></i>Summit Plan Templates
        </h1>
        <a class="ms-auto small" href="${pageContext.request.contextPath}/SummitEmployerFlagAdmin">Summit Employer Flags &rarr;</a>
    </div>

    <div class="rc-body">

        <c:if test="${not empty sessionScope.summitPlanTemplateMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.summitPlanTemplateMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="summitPlanTemplateMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.summitPlanTemplateError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.summitPlanTemplateError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="summitPlanTemplateError" scope="session"/>
        </c:if>

        <%-- Which source is actually supplying the mapping. The resolver prefers the table and
             falls back to the property, so with no ACTIVE row the property is what every export
             really uses — however many inactive rows are listed below. --%>
        <div class="status-card">
            <c:choose>
                <c:when test="${tableIsLive}">
                    <div>
                        <span class="badge badge-on"><i class="bi bi-check-lg"></i> Table is live</span>
                        <span class="ms-2">The active mappings below are what Summit file 2 emits.
                            <span class="mono"><c:out value="${propertyKey}"/></span> is not read.</span>
                    </div>
                </c:when>
                <c:otherwise>
                    <div>
                        <span class="badge badge-off"><i class="bi bi-dash-lg"></i> Property is live</span>
                        <span class="ms-2">There are <strong>no active mappings</strong> for your PSP, so
                            Summit file 2 is currently taking its plan list from the
                            <span class="mono"><c:out value="${propertyKey}"/></span> setting in
                            <span class="mono">ssa.properties</span>
                            (<c:out value="${propertyEntryCount}"/> well-formed
                            <c:choose><c:when test="${propertyEntryCount == 1}">entry</c:when><c:otherwise>entries</c:otherwise></c:choose>).
                            Activating a mapping here takes over completely — the property is then not read at all.</span>
                    </div>
                    <c:if test="${not empty propertyRaw}">
                        <div class="text-muted mt-2" style="font-size:0.78rem;">
                            <span class="mono"><c:out value="${propertyKey}"/>=<c:out value="${propertyRaw}"/></span>
                        </div>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- Inactive rows are listed deliberately: the unique constraint ignores is_active, so an
             inactive row still holds its (PSP, service item) slot and blocks a new one. --%>
        <c:choose>
            <c:when test="${empty mappings}">
                <div class="empty-state">
                    No plan template mappings yet. Add one below to take over from
                    <span class="mono"><c:out value="${propertyKey}"/></span>.
                </div>
            </c:when>
            <c:otherwise>
                <table class="map-table">
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Service item</th>
                        <th>Plan Template ID</th>
                        <th>Key segment</th>
                        <th>Label</th>
                        <th>Active</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="m" items="${mappings}">
                        <tr class="${m.active ? '' : 'row-inactive'}">
                            <td><c:out value="${m.sortOrder}"/></td>
                            <td class="mono"><c:out value="${m.serviceItemId}"/></td>
                            <td class="mono"><c:out value="${m.templateId}"/></td>
                            <td class="mono"><c:out value="${m.keySegment}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${empty m.label}">
                                        <span class="text-muted" style="font-size:0.78rem;">
                                            (falls back to <c:out value="${m.keySegment}"/>)</span>
                                    </c:when>
                                    <c:otherwise><c:out value="${m.label}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.active}"><span class="badge badge-on">Active</span></c:when>
                                    <c:otherwise><span class="badge badge-off">Inactive</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-secondary py-0 px-2"
                                   href="${pageContext.request.contextPath}/SummitPlanTemplateAdmin?editId=${m.id}">Edit</a>
                                <form method="post" action="${pageContext.request.contextPath}/SummitPlanTemplateAdmin"
                                      class="d-inline"
                                      onsubmit="return confirm('Remove this mapping? Any Summit plan already created from it stays in Summit; this only stops future exports emitting it.');">
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
             otherwise. The row is matched in the servlet, never by EL arithmetic on a parameter. --%>
        <div class="form-card">
            <div class="fw-semibold mb-2" style="color: var(--ssa, #0d5681);">
                <c:choose>
                    <c:when test="${not empty editing}">Edit mapping <c:out value="${editing.id}"/></c:when>
                    <c:otherwise>Add a mapping</c:otherwise>
                </c:choose>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/SummitPlanTemplateAdmin">
                <input type="hidden" name="action" value="save">
                <c:if test="${not empty editing}">
                    <input type="hidden" name="id" value="${editing.id}">
                </c:if>

                <div class="row g-2">
                    <div class="col-md-4">
                        <label for="serviceItemId">Service item</label>
                        <%-- The id is shown alongside the description because the description is a
                             copy of a LOS short text taken at creation (T190) and two items can read
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
                        <label for="templateId">Plan Template ID</label>
                        <input type="number" min="1" step="1" class="form-control form-control-sm"
                               id="templateId" name="templateId" required
                               value="${not empty editing ? editing.templateId : ''}">
                    </div>
                    <div class="col-md-2">
                        <label for="keySegment">Key segment</label>
                        <input type="text" class="form-control form-control-sm mono"
                               id="keySegment" name="keySegment" required
                               value="<c:out value="${not empty editing ? editing.keySegment : ''}"/>">
                    </div>
                    <div class="col-md-2">
                        <label for="label">Label (optional)</label>
                        <input type="text" class="form-control form-control-sm"
                               id="label" name="label"
                               value="<c:out value="${not empty editing ? editing.label : ''}"/>">
                    </div>
                    <div class="col-md-1">
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
                </div>

                <div class="text-muted mt-2" style="font-size:0.75rem;">
                    <i class="bi bi-info-circle me-1"></i>Key segment travels inside
                    <span class="mono">Import Plan ID</span>, which is an upsert key in a pipe-delimited
                    file — no pipe, no spaces. Label falls back to the key segment when left blank.
                    One mapping per service item: the database rejects a second one even if the first
                    is inactive.
                </div>

                <div class="mt-2">
                    <button type="submit" class="btn btn-sm btn-ssa">
                        <c:choose><c:when test="${not empty editing}">Save</c:when><c:otherwise>Add</c:otherwise></c:choose>
                    </button>
                    <c:if test="${not empty editing}">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${pageContext.request.contextPath}/SummitPlanTemplateAdmin">Cancel</a>
                    </c:if>
                </div>
            </form>
        </div>

    </div>
</div>
</body>
</html>
