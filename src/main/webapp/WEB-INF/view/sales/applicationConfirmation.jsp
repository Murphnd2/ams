<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Application Submitted — ${pspName}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <style>
        :root { --psp-primary: ${primaryColor}; --psp-accent: ${accentColor}; }
        body { background: #f8f9fa; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        .app-header { background: var(--psp-primary); color: white; padding: 2rem 0; }
        .app-header h1 { font-size: 1.6rem; font-weight: 600; margin: 0; }
        .accent-bar { height: 4px; background: var(--psp-accent); }
        .confirm-container { max-width: 600px; margin: 0 auto; padding: 3rem 1rem; text-align: center; }
        .check-circle { font-size: 4rem; color: var(--psp-accent); margin-bottom: 1.5rem; }
        .app-footer { text-align: center; color: #999; font-size: 0.85rem; padding: 2rem 0; border-top: 1px solid #e9ecef; max-width: 600px; margin: 0 auto; }
    </style>
</head>
<body>

<div class="app-header">
    <div style="max-width:600px;margin:0 auto;padding:0 1rem;">
        <h1>${pspName}</h1>
    </div>
</div>
<div class="accent-bar"></div>

<div class="confirm-container">
    <c:choose>
        <c:when test="${applicationStatus == 'APPROVED'}">
            <div class="check-circle"><i class="bi bi-check-circle-fill"></i></div>
        </c:when>
        <c:when test="${applicationStatus == 'DENIED'}">
            <div class="check-circle" style="color: #6c757d;"><i class="bi bi-info-circle-fill"></i></div>
        </c:when>
        <c:otherwise>
            <div class="check-circle"><i class="bi bi-check-circle-fill"></i></div>
        </c:otherwise>
    </c:choose>
    <c:choose>
        <c:when test="${not empty statusMessage}">
            <h2 style="color: var(--psp-primary); margin-bottom: 1rem;">
                <c:choose>
                    <c:when test="${applicationStatus == 'APPROVED'}">Application Approved</c:when>
                    <c:when test="${applicationStatus == 'DENIED'}">Application Reviewed</c:when>
                    <c:otherwise>Application Submitted</c:otherwise>
                </c:choose>
            </h2>
            <p class="mb-3">${statusMessage}</p>
        </c:when>
        <c:otherwise>
            <h2 style="color: var(--psp-primary); margin-bottom: 1rem;">Application Submitted</h2>
            <p class="mb-3">Thank you! Your application for <strong>${prospectName}</strong> has been received.</p>
            <p class="text-muted">Our team will review your application and follow up with next steps. You can expect to hear from us within two business days.</p>
        </c:otherwise>
    </c:choose>
</div>

<div class="app-footer">
    <p class="mb-0">&copy; ${pspName} &middot; Benefits Administration Services</p>
</div>

</body>
</html>