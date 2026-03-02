<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Vendor Management</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .vendor-card {
            border: 1px solid #dee2e6;
            border-left: 4px solid #adb5bd;
            border-radius: 6px;
            padding: 1rem 1.25rem;
            margin-bottom: 0.75rem;
            background: white;
        }
        .vendor-card.approved { border-left-color: #198754; }
        .vendor-card.pending { border-left-color: #fd7e14; }
        .vendor-card.disconnected { border-left-color: #adb5bd; opacity: 0.6; }
        .status-dot {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            margin-right: 0.35rem;
        }
        .status-dot.approved { background: #198754; }
        .status-dot.pending { background: #fd7e14; }
        .status-dot.disconnected { background: #adb5bd; }
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
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="row g-3 mt-1">
        <div class="col-lg-10 col-xl-8 mx-auto">

            <%-- Flash messages --%>
            <c:if test="${not empty sessionScope.vendorMessage}">
                <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                    <i class="bi bi-check-circle me-1"></i>${sessionScope.vendorMessage}
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="vendorMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.vendorError}">
                <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                    <i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.vendorError}
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="vendorError" scope="session"/>
            </c:if>

            <%-- Request New Connection --%>
            <div class="card mb-4">
                <div class="hdr-bar d-flex align-items-center">
                    <i class="bi bi-plus-circle me-2"></i>Request New Connection
                </div>
                <div class="card-body">
                    <form method="post" action="VendorManager" class="row g-3 align-items-end">
                        <input type="hidden" name="action" value="requestConnection">
                        <div class="col-md-4">
                            <label class="form-label" style="font-size:0.8rem; font-weight:600; color:#495057;">BPO Name</label>
                            <input type="text" class="form-control form-control-sm" name="bpoName"
                                   placeholder="e.g. Accelergent BPO Services" required>
                        </div>
                        <div class="col-md-5">
                            <label class="form-label" style="font-size:0.8rem; font-weight:600; color:#495057;">BPO URL</label>
                            <input type="url" class="form-control form-control-sm" name="bpoUrl"
                                   placeholder="https://bpo.example.com" required pattern="https://.*">
                        </div>
                        <div class="col-md-3">
                            <button type="submit" class="btn btn-ssa btn-sm w-100">
                                <i class="bi bi-send me-1"></i>Request Connection
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <%-- Registered Vendors --%>
            <div class="card">
                <div class="hdr-bar d-flex align-items-center">
                    <i class="bi bi-diagram-3 me-2"></i>Registered Vendors
                </div>
                <div class="card-body">
                    <c:choose>
                        <c:when test="${not empty vendors}">
                            <c:forEach var="v" items="${vendors}">
                                <c:set var="statusClass" value="${v.approved && v.accepted ? 'approved' : v.active ? 'pending' : 'disconnected'}"/>
                                <div class="vendor-card ${statusClass}">
                                    <div class="d-flex justify-content-between align-items-start">
                                        <div>
                                            <div style="font-size:1rem; font-weight:600; color:#212529;">
                                                ${v.bpoName}
                                            </div>
                                            <div style="font-size:0.8rem; color:#6c757d; margin-top:0.15rem;">
                                                <i class="bi bi-link-45deg me-1"></i>${v.partnerUrl != null ? v.partnerUrl : v.bpoUrl}
                                            </div>
                                            <div style="font-size:0.8rem; margin-top:0.4rem;">
                                                <span class="status-dot ${statusClass}"></span>
                                                <c:choose>
                                                    <c:when test="${v.approved && v.accepted}">
                                                        <span style="color:#198754; font-weight:500;">APPROVED</span>
                                                        <c:if test="${v.dateApproved != null}">
                                                            <span class="text-muted ms-2" style="font-size:0.75rem;">
                                                                Since: <fmt:formatDate value="${v.dateApproved}" pattern="yyyy-MM-dd"/>
                                                            </span>
                                                        </c:if>
                                                    </c:when>
                                                    <c:when test="${v.active && v.requested}">
                                                        <span style="color:#fd7e14; font-weight:500;">PENDING</span>
                                                        <c:if test="${v.dateRequested != null}">
                                                            <span class="text-muted ms-2" style="font-size:0.75rem;">
                                                                Requested: <fmt:formatDate value="${v.dateRequested}" pattern="yyyy-MM-dd"/>
                                                            </span>
                                                        </c:if>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span style="color:#adb5bd; font-weight:500;">DISCONNECTED</span>
                                                        <c:if test="${v.dateDisconnected != null}">
                                                            <span class="text-muted ms-2" style="font-size:0.75rem;">
                                                                Since: <fmt:formatDate value="${v.dateDisconnected}" pattern="yyyy-MM-dd"/>
                                                            </span>
                                                        </c:if>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                        <c:if test="${v.active}">
                                            <form method="post" action="VendorManager"
                                                  onsubmit="return confirm('Disconnect ${v.bpoName}? This will revoke API access.');">
                                                <input type="hidden" name="action" value="disconnect">
                                                <input type="hidden" name="vendorId" value="${v.id}">
                                                <button type="submit" class="btn-disconnect">
                                                    <i class="bi bi-x-circle me-1"></i>
                                                    <c:choose>
                                                        <c:when test="${v.approved && v.accepted}">Disconnect</c:when>
                                                        <c:otherwise>Cancel Request</c:otherwise>
                                                    </c:choose>
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-state">
                                <i class="bi bi-diagram-3"></i>
                                <div style="font-size:0.85rem; margin-top:0.5rem;">No vendors registered yet.</div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

        </div>
    </div>
</div>
</body>
</html>
