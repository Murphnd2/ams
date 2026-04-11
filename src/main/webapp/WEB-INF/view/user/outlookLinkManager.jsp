<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Outlook Link Manager</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .audit-wrap {
            display: flex;
            flex-direction: column;
            height: calc(100vh - 64px);
        }
        .audit-toolbar {
            padding: 0.75rem 1rem;
            background: #fff;
            border-bottom: 1px solid #dee2e6;
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 1rem;
            flex-wrap: wrap;
        }
        .audit-body {
            flex: 1;
            overflow: auto;
            padding: 1rem;
        }
        .token-cell {
            font-family: monospace;
            font-size: 0.72rem;
            color: #6c757d;
        }
        .table-outlook th {
            position: sticky;
            top: 0;
            background: #f8f9fa;
            z-index: 1;
            font-size: 0.8rem;
        }
        .table-outlook td { font-size: 0.85rem; }
        .badge-active   { background: #87a948; }
        .badge-inactive { background: #adb5bd; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="audit-wrap">

        <div class="audit-toolbar">
            <div>
                <h5 class="mb-0">
                    <i class="bi bi-envelope-plus me-1"></i> Outlook Link Manager
                </h5>
                <div class="text-muted small mt-1">
                    Link AMS users to their Microsoft 365 email so the "Log to AMS" Outlook add-in can authenticate automatically.
                </div>
            </div>
            <button type="button" class="btn btn-sm ssa-action" data-bs-toggle="modal" data-bs-target="#newLinkModal">
                <i class="bi bi-plus-lg me-1"></i> Link New User
            </button>
        </div>

        <div class="audit-body">

            <%-- Flash messages --%>
            <c:if test="${not empty sessionScope.outlookLinkMessage}">
                <div class="alert alert-success alert-dismissible fade show py-2" role="alert">
                    <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.outlookLinkMessage}"/>
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="outlookLinkMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.outlookLinkError}">
                <div class="alert alert-danger alert-dismissible fade show py-2" role="alert">
                    <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.outlookLinkError}"/>
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="outlookLinkError" scope="session"/>
            </c:if>

            <c:choose>
                <c:when test="${empty links}">
                    <div class="text-muted text-center py-5">
                        <i class="bi bi-envelope-x" style="font-size: 2rem;"></i>
                        <div class="mt-2">No Outlook links configured yet.</div>
                        <div class="small">Click <strong>Link New User</strong> above to get started.</div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card">
                        <div class="card-body p-0">
                            <table class="table table-hover table-outlook mb-0">
                                <thead>
                                    <tr>
                                        <th>AMS User</th>
                                        <th>Microsoft 365 Email</th>
                                        <th>Status</th>
                                        <th>Created</th>
                                        <th class="text-end" style="width: 180px;">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="link" items="${links}">
                                        <tr>
                                            <td>
                                                <div class="fw-semibold"><c:out value="${link.person.fullName}"/></div>
                                                <c:if test="${not empty link.person.email}">
                                                    <div class="text-muted small"><c:out value="${link.person.email}"/></div>
                                                </c:if>
                                            </td>
                                            <td><c:out value="${link.m365Email}"/></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${link.active}">
                                                        <span class="badge badge-active">Active</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge badge-inactive">Inactive</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-muted small">
                                                <c:if test="${not empty link.createdDate}">
                                                    <fmt:formatDate value="${link.createdDate}" pattern="yyyy-MM-dd"/>
                                                </c:if>
                                            </td>
                                            <td class="text-end">
                                                <c:if test="${link.active}">
                                                    <form method="post" action="${pageContext.request.contextPath}/OutlookLinkManager" class="d-inline">
                                                        <input type="hidden" name="action" value="relink"/>
                                                        <input type="hidden" name="linkId" value="${link.id}"/>
                                                        <button type="submit" class="btn btn-sm btn-outline-secondary"
                                                                title="Generate a new token (old token stops working)"
                                                                onclick="return confirm('Regenerate token for ${link.m365Email}? The user will need to re-open the add-in to pick up the new token.');">
                                                            <i class="bi bi-arrow-clockwise"></i> Regenerate
                                                        </button>
                                                    </form>
                                                    <form method="post" action="${pageContext.request.contextPath}/OutlookLinkManager" class="d-inline">
                                                        <input type="hidden" name="action" value="unlink"/>
                                                        <input type="hidden" name="linkId" value="${link.id}"/>
                                                        <button type="submit" class="btn btn-sm btn-outline-danger"
                                                                onclick="return confirm('Deactivate Outlook link for ${link.m365Email}?');">
                                                            <i class="bi bi-x-lg"></i> Unlink
                                                        </button>
                                                    </form>
                                                </c:if>
                                                <c:if test="${!link.active}">
                                                    <form method="post" action="${pageContext.request.contextPath}/OutlookLinkManager" class="d-inline">
                                                        <input type="hidden" name="action" value="relink"/>
                                                        <input type="hidden" name="linkId" value="${link.id}"/>
                                                        <button type="submit" class="btn btn-sm btn-outline-success"
                                                                title="Reactivate and generate a fresh token">
                                                            <i class="bi bi-arrow-clockwise"></i> Reactivate
                                                        </button>
                                                    </form>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- New Link Modal --%>
<div class="modal fade" id="newLinkModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/OutlookLinkManager">
                <input type="hidden" name="action" value="link"/>
                <div class="modal-header">
                    <h5 class="modal-title">Link New User</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label for="personId" class="form-label">AMS User</label>
                        <select class="form-select" id="personId" name="personId" required>
                            <option value="">-- Select a user --</option>
                            <c:forEach var="p" items="${pspUsers}">
                                <option value="${p.id}"><c:out value="${p.fullName}"/>
                                    <c:if test="${not empty p.email}"> &mdash; <c:out value="${p.email}"/></c:if>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label for="m365Email" class="form-label">Microsoft 365 Email</label>
                        <input type="email" class="form-control" id="m365Email" name="m365Email"
                               placeholder="user@superiorstate.biz" required>
                        <div class="form-text">
                            This is the email address the user signs into Outlook/Microsoft 365 with.
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary ssa-action" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn ssa-action" style="background:#0d5681;color:#fff;">
                        <i class="bi bi-link-45deg me-1"></i> Link User
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

</body>
</html>
