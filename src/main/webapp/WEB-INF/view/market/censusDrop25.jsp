<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- S47-C -- public census drop page, /census-drop/{token} (T231 build 1, D29 option 2 / D45).
     Head and branding copied from sales/applyForProposal.jsp: no session, no navbar, PSP colours
     from EMAIL_COLOR_PRIMARY/ACCENT, white-label band when an originating agency exists.
     ⚠️ Never renders a cell value from the upload -- issues are row number and reason only. --%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Census upload — ${not empty agencyName ? agencyName : pspName}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <style>
        :root {
            --psp-primary: ${primaryColor};
            --psp-accent: ${accentColor};
        }
        body { background: #f8f9fa; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        .app-header { background: var(--psp-primary); color: white; padding: 2rem 0; }
        .app-header.wl { background: #1f2937; }
        .app-header h1 { font-size: 1.6rem; font-weight: 600; margin: 0; }
        .app-header .subtitle { opacity: 0.85; font-size: 0.95rem; }
        .accent-bar { height: 4px; background: var(--psp-accent); }
        .app-container { max-width: 800px; margin: 0 auto; padding: 2rem 1rem 4rem; }
        .section-card { background: white; border-radius: 8px; margin-bottom: 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.08); overflow: hidden; }
        .section-header { background: var(--psp-primary); color: white; padding: 0.75rem 1.25rem; font-weight: 600; font-size: 1.05rem; }
        .section-body { padding: 1.25rem; }
        .btn-submit { background: var(--psp-accent); border-color: var(--psp-accent); color: white; font-size: 1.05rem; font-weight: 600; padding: 0.6rem 2.5rem; border-radius: 6px; }
        .btn-submit:hover { background: var(--psp-primary); border-color: var(--psp-primary); color: white; }
        .issue-list { max-height: 22rem; overflow-y: auto; font-size: 0.9rem; }
        .app-footer { text-align: center; color: #999; font-size: 0.85rem; padding: 2rem 0; border-top: 1px solid #e9ecef; }
    </style>
</head>
<body>

<%-- Header --%>
<div class="app-header ${not empty agencyName ? 'wl' : ''}">
    <div class="app-container" style="padding-top:0;padding-bottom:0;">
        <c:choose>
            <c:when test="${not empty agencyName}">
                <h1><c:out value="${agencyName}"/></h1>
                <div class="subtitle">Census upload</div>
            </c:when>
            <c:otherwise>
                <h1><c:out value="${pspName}"/></h1>
                <div class="subtitle">Benefits Administration — Census upload</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
<c:if test="${empty agencyName}"><div class="accent-bar"></div></c:if>

<div class="app-container">

<c:choose>
    <%-- ── Inactive: one state for unknown, expired, revoked and loaded tokens ── --%>
    <c:when test="${inactive}">
        <div class="section-card">
            <div class="section-body text-center py-5">
                <i class="bi bi-link-45deg" style="font-size: 2rem; color: #6c757d;"></i>
                <p class="mb-0 mt-2" style="font-size: 1.05rem;">This link is no longer active. Please contact your administrator.</p>
            </div>
        </div>
    </c:when>

    <%-- ── Active ── --%>
    <c:otherwise>
        <h2 class="h4 mb-3">Census upload for <c:out value="${employerName}"/></h2>

        <%-- Result of the upload just made --%>
        <c:if test="${not empty resultState}">
            <c:choose>
                <c:when test="${resultState == 'UNREADABLE'}">
                    <div class="alert alert-warning">
                        <c:choose>
                            <c:when test="${not empty fileError}">
                                <p class="mb-1"><strong>We couldn't read that file.</strong> <c:out value="${fileError}"/></p>
                            </c:when>
                            <c:otherwise>
                                <p class="mb-1"><strong>We couldn't find these required columns:</strong> <c:out value="${missingLabelsText}"/></p>
                            </c:otherwise>
                        </c:choose>
                        <c:if test="${not empty foundHeadersText}">
                            <p class="mb-1">Columns we found: <c:out value="${foundHeadersText}"/></p>
                        </c:if>
                        <p class="mb-0">Please fix and upload again.</p>
                    </div>
                </c:when>
                <c:when test="${resultIssueCount == 0}">
                    <div class="alert alert-success">
                        <i class="bi bi-check-circle me-1"></i>Received ${resultRowCount} rows. Your administrator will review them.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-info">
                        <p class="mb-2"><i class="bi bi-info-circle me-1"></i>Received ${resultRowCount} rows; ${resultIssueCount} need attention:</p>
                        <ul class="issue-list mb-2">
                            <c:forEach var="line" items="${issueLines}">
                                <li><c:out value="${line}"/></li>
                            </c:forEach>
                        </ul>
                        <p class="mb-0">You can upload a corrected file; the newest upload replaces earlier ones.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:if>

        <c:if test="${not empty formError}">
            <div class="alert alert-danger"><c:out value="${formError}"/></div>
        </c:if>

        <div class="section-card">
            <div class="section-header"><i class="bi bi-list-check me-2"></i>What to include</div>
            <div class="section-body">
                <p class="mb-1"><strong>Required columns:</strong> <c:out value="${requiredLabelsText}"/></p>
                <p class="mb-2"><strong>Optional columns:</strong> <c:out value="${optionalLabelsText}"/></p>
                <p class="mb-1">Please leave out Social Security numbers, dates of birth, and pay — we don't need them.</p>
                <p class="mb-0 text-muted" style="font-size: 0.9rem;">Column order doesn't matter — each column is found by its heading. Files up to 5 MB, .csv, .txt, .xlsx or .xls.</p>
            </div>
        </div>

        <div class="section-card">
            <div class="section-header"><i class="bi bi-upload me-2"></i>Upload your census</div>
            <div class="section-body">
                <form method="post" enctype="multipart/form-data"
                      action="${pageContext.request.contextPath}/census-drop/${token}">
                    <div class="mb-3">
                        <input type="file" name="censusFile" required accept=".csv,.txt,.xlsx,.xls" class="form-control"/>
                    </div>
                    <button type="submit" class="btn btn-submit"><i class="bi bi-cloud-upload me-1"></i>Upload</button>
                </form>
                <c:if test="${not empty lastUploadDisplay}">
                    <p class="mt-3 mb-0 text-muted" style="font-size: 0.9rem;">Last upload received ${lastUploadDisplay}. A new upload replaces it.</p>
                </c:if>
            </div>
        </div>
    </c:otherwise>
</c:choose>

    <div class="app-footer">
        <c:choose>
            <c:when test="${not empty agencyName}">
                <p class="mb-0">&copy; <c:out value="${agencyName}"/></p>
            </c:when>
            <c:otherwise>
                <p class="mb-0">&copy; <c:out value="${pspName}"/> &middot; Benefits Administration Services</p>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
