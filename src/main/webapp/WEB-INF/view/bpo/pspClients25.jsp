<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>PSP Clients</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .hdr-bar-pending { background-color: #e67e22; color: white; padding: 0.5rem 0.75rem; font-weight: 600; font-size: 1rem; border-radius: 6px 6px 0 0; }
        .client-card {
            border: 1px solid #dee2e6;
            border-left: 4px solid #198754;
            border-radius: 6px;
            padding: 1rem 1.25rem;
            margin-bottom: 0.75rem;
            background: white;
        }
        .request-card {
            border: 1px solid #ffc107;
            border-left: 4px solid #fd7e14;
            border-radius: 6px;
            padding: 1rem 1.25rem;
            margin-bottom: 0.75rem;
            background: #fffdf5;
        }
        .btn-approve {
            background: #0d5681;
            border-color: #0d5681;
            color: white;
            font-size: 0.8rem;
            font-weight: 500;
            padding: 0.3rem 0.75rem;
            border-radius: 4px;
            cursor: pointer;
            transition: background 0.15s;
        }
        .btn-approve:hover { background: #06357a; color: white; }
        .btn-reject {
            border: none;
            background: none;
            color: #dc3545;
            font-size: 0.8rem;
            font-weight: 500;
            padding: 0.3rem 0.75rem;
            border-radius: 4px;
            cursor: pointer;
            transition: background 0.15s;
        }
        .btn-reject:hover { background: rgba(220,53,69,0.08); }
        .btn-disconnect {
            border: none;
            background: none;
            color: #dc3545;
            font-size: 0.8rem;
            font-weight: 500;
            padding: 0.25rem 0.5rem;
            border-radius: 4px;
            cursor: pointer;
            transition: background 0.15s;
        }
        .btn-disconnect:hover { background: rgba(220,53,69,0.08); }
        .auto-accept-badge {
            display: inline-block;
            font-size: 0.7rem;
            padding: 0.15rem 0.5rem;
            border-radius: 10px;
            font-weight: 500;
        }
        .auto-accept-on { background: #d4edda; color: #155724; }
        .auto-accept-off { background: #f8f9fa; color: #6c757d; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="row g-3 mt-1">
        <div class="col-lg-10 col-xl-8 mx-auto">

            <%-- Flash messages --%>
            <c:if test="${not empty sessionScope.clientMessage}">
                <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                    <i class="bi bi-check-circle me-1"></i>${sessionScope.clientMessage}
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="clientMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.clientError}">
                <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                    <i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.clientError}
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="clientError" scope="session"/>
            </c:if>

            <%-- Pending Requests --%>
            <c:set var="hasPending" value="false"/>
            <c:forEach var="c" items="${clients}">
                <c:if test="${c.status == 'PENDING'}"><c:set var="hasPending" value="true"/></c:if>
            </c:forEach>

            <c:if test="${hasPending}">
                <div class="card mb-4">
                    <div class="hdr-bar-pending d-flex align-items-center">
                        <i class="bi bi-bell me-2"></i>Pending Requests
                    </div>
                    <div class="card-body">
                        <c:forEach var="c" items="${clients}">
                            <c:if test="${c.status == 'PENDING'}">
                                <div class="request-card">
                                    <div class="d-flex justify-content-between align-items-start">
                                        <div>
                                            <div style="font-size:1rem; font-weight:600; color:#212529;">
                                                ${c.pspName}
                                            </div>
                                            <div style="font-size:0.8rem; color:#6c757d; margin-top:0.15rem;">
                                                <i class="bi bi-link-45deg me-1"></i>${c.pspUrl}
                                            </div>
                                            <c:if test="${c.dateRequested != null}">
                                                <div style="font-size:0.75rem; color:#999; margin-top:0.25rem;">
                                                    Requested: <fmt:formatDate value="${c.dateRequested}" pattern="yyyy-MM-dd"/>
                                                </div>
                                            </c:if>
                                        </div>
                                        <div class="d-flex gap-2">
                                            <form method="post" action="BpoPspClients">
                                                <input type="hidden" name="action" value="approve">
                                                <input type="hidden" name="clientId" value="${c.id}">
                                                <button type="submit" class="btn btn-approve">
                                                    <i class="bi bi-check-lg me-1"></i>Approve
                                                </button>
                                            </form>
                                            <form method="post" action="BpoPspClients"
                                                  onsubmit="return confirm('Reject request from ${c.pspName}?');">
                                                <input type="hidden" name="action" value="reject">
                                                <input type="hidden" name="clientId" value="${c.id}">
                                                <button type="submit" class="btn-reject">
                                                    <i class="bi bi-x-lg me-1"></i>Reject
                                                </button>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>
            </c:if>

            <%-- Active Clients --%>
            <div class="card">
                <div class="hdr-bar d-flex align-items-center">
                    <i class="bi bi-building me-2"></i>Active Clients
                </div>
                <div class="card-body">
                    <c:set var="hasActive" value="false"/>
                    <c:forEach var="c" items="${clients}">
                        <c:if test="${c.status == 'APPROVED'}">
                            <c:set var="hasActive" value="true"/>
                            <div class="client-card">
                                <div class="d-flex justify-content-between align-items-start">
                                    <div>
                                        <div style="font-size:1rem; font-weight:600; color:#212529;">
                                            ${c.pspName}
                                        </div>
                                        <div style="font-size:0.8rem; color:#6c757d; margin-top:0.15rem;">
                                            <i class="bi bi-link-45deg me-1"></i>${c.pspUrl}
                                        </div>
                                        <div style="font-size:0.8rem; margin-top:0.4rem;">
                                            <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#198754; margin-right:0.35rem;"></span>
                                            <span style="color:#198754; font-weight:500;">APPROVED</span>
                                            <c:if test="${c.dateApproved != null}">
                                                <span class="text-muted ms-2" style="font-size:0.75rem;">
                                                    Since: <fmt:formatDate value="${c.dateApproved}" pattern="yyyy-MM-dd"/>
                                                </span>
                                            </c:if>
                                            <span class="ms-3">
                                                <span class="auto-accept-badge ${c.autoAcceptTasks ? 'auto-accept-on' : 'auto-accept-off'}">
                                                    Auto-accept: ${c.autoAcceptTasks ? 'ON' : 'OFF'}
                                                </span>
                                                <form method="post" action="BpoPspClients" class="d-inline ms-1">
                                                    <input type="hidden" name="action" value="toggleAutoAccept">
                                                    <input type="hidden" name="clientId" value="${c.id}">
                                                    <button type="submit" class="btn btn-sm btn-outline-secondary"
                                                            style="font-size:0.68rem; padding:0.1rem 0.4rem;">Toggle</button>
                                                </form>
                                            </span>
                                        </div>
                                    </div>
                                    <form method="post" action="BpoPspClients"
                                          onsubmit="return confirm('Disconnect ${c.pspName}? This will revoke API access.');">
                                        <input type="hidden" name="action" value="disconnect">
                                        <input type="hidden" name="clientId" value="${c.id}">
                                        <button type="submit" class="btn-disconnect">
                                            <i class="bi bi-x-circle me-1"></i>Disconnect
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </c:if>
                    </c:forEach>
                    <c:if test="${!hasActive}">
                        <div class="empty-state">
                            <i class="bi bi-building"></i>
                            <div style="font-size:0.85rem; margin-top:0.5rem;">No PSP clients connected yet.</div>
                        </div>
                    </c:if>
                </div>
            </div>

        </div>
    </div>
</div>
</body>
</html>
