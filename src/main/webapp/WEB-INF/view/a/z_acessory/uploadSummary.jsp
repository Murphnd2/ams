<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<c:set var="pageTitle" value="Upload Summary" scope="request"/>
<c:set var="pageIcon" value="bi-cloud-check" scope="request"/>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${applicationScope.global.psp.fullName} — Upload Summary</title>
    <style>
        .summary-wrapper { max-width: 800px; margin: 1rem auto; }

        .summary-card {
            background: white;
            border: 1.5px solid #e5e7eb;
            border-radius: 10px;
            overflow: hidden;
            margin-bottom: 1rem;
        }

        .result-row {
            display: flex;
            align-items: flex-start;
            padding: 0.6rem 1rem;
            border-bottom: 1px solid #f0f0f0;
            font-size: 0.84rem;
            gap: 0.6rem;
        }
        .result-row:last-child { border-bottom: none; }

        .result-icon {
            flex-shrink: 0;
            width: 22px;
            text-align: center;
            font-size: 0.9rem;
            margin-top: 1px;
        }
        .result-icon.success { color: #198754; }
        .result-icon.fail { color: #dc3545; }

        .result-text { flex: 1; color: #333; }

        .count-pill {
            display: inline-block;
            background: var(--ssa, #0d5681);
            color: white;
            padding: 0.15rem 0.65rem;
            border-radius: 12px;
            font-size: 0.78rem;
            font-weight: 600;
        }

        .empty-state {
            text-align: center;
            padding: 1.5rem;
            color: #adb5bd;
            font-size: 0.85rem;
        }
        .empty-state i { font-size: 1.5rem; display: block; margin-bottom: 0.4rem; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="summary-wrapper">

        <%-- ═══ UPLOAD COUNT BANNER ═══ --%>
        <div class="summary-card">
            <div class="hdr-bar d-flex align-items-center justify-content-between">
                <span><i class="bi bi-cloud-check me-2"></i>Upload Complete</span>
                <span class="count-pill">${uploadCount} file<c:if test="${uploadCount != 1}">s</c:if> uploaded</span>
            </div>
        </div>

        <%-- ═══ MATCHED FILES ═══ --%>
        <c:if test="${not empty matchedFiles}">
            <div class="summary-card">
                <div class="hdr-bar" style="background: #198754;">
                    <i class="bi bi-check-circle me-2"></i>Matched &amp; Renamed Files
                </div>
                <c:forEach var="item" items="${matchedFiles}">
                    <div class="result-row">
                        <div class="result-icon success"><i class="bi bi-check-lg"></i></div>
                        <div class="result-text">${item}</div>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <%-- ═══ UNMATCHED / REJECTED FILES ═══ --%>
        <c:if test="${not empty unmatchedFiles}">
            <div class="summary-card">
                <div class="hdr-bar" style="background: #dc3545;">
                    <i class="bi bi-x-circle me-2"></i>Unmatched or Rejected Files
                </div>
                <c:forEach var="item" items="${unmatchedFiles}">
                    <div class="result-row">
                        <div class="result-icon fail"><i class="bi bi-x-lg"></i></div>
                        <div class="result-text">${item}</div>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <%-- ═══ EMPTY STATE ═══ --%>
        <c:if test="${empty matchedFiles && empty unmatchedFiles}">
            <div class="summary-card">
                <div class="empty-state">
                    <i class="bi bi-inbox"></i>
                    No files were processed in this upload.
                </div>
            </div>
        </c:if>

        <%-- ═══ ACTION BUTTONS ═══ --%>
        <div class="d-flex gap-2 mt-3">
            <a href="ShowUploadPage" class="btn btn-ssa">
                <i class="bi bi-arrow-left me-1"></i>Back to Upload
            </a>
            <a href="PreviewUploadedFiles" class="btn btn-outline-ssa">
                <i class="bi bi-eye me-1"></i>Preview Files
            </a>
            <a href="ViewHome25" class="btn btn-outline-secondary">
                <i class="bi bi-house me-1"></i>Home
            </a>
        </div>

    </div>
</div>
</body>
</html>
