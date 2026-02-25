<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<c:set var="pageTitle" value="Branding" scope="request"/>
<c:set var="pageIcon" value="bi-palette" scope="request"/>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${applicationScope.global.psp.fullName} — Branding</title>
    <style>
        .brand-card {
            background: white;
            border: 1.5px solid #e5e7eb;
            border-radius: 10px;
            padding: 1.25rem;
            margin-bottom: 1rem;
            transition: border-color 0.2s;
        }
        .brand-card:hover {
            border-color: #0d5681;
        }
        .brand-card h6 {
            font-size: 0.85rem;
            font-weight: 700;
            color: #0d5681;
            margin-bottom: 0.15rem;
        }
        .brand-card .specs {
            font-size: 0.72rem;
            color: #9ca3af;
            margin-bottom: 0.75rem;
        }
        .preview-box {
            background: #f8f9fa;
            border: 1px dashed #d1d5db;
            border-radius: 8px;
            padding: 1rem;
            text-align: center;
            min-height: 70px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 0.75rem;
        }
        .preview-box.dark-bg {
            background: #0d5681;
        }
        .preview-box img {
            max-height: 60px;
            max-width: 100%;
        }
        .preview-box.login-preview img {
            max-height: 120px;
        }
        .preview-box.favicon-preview img {
            max-height: 32px;
            image-rendering: pixelated;
        }
        .file-input-wrapper {
            position: relative;
        }
        .file-input-wrapper input[type="file"] {
            font-size: 0.82rem;
        }
        .current-label {
            font-size: 0.72rem;
            color: #6b7280;
            text-align: center;
            margin-top: 0.25rem;
        }
        .upload-hint {
            font-size: 0.75rem;
            color: #9ca3af;
            margin-top: 0.25rem;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="row mt-3">
        <div class="col"></div>
        <div class="col-lg-6 col-md-8 col-sm-12">

            <%-- Success alert --%>
            <c:if test="${param.success == 'true'}">
                <div class="alert alert-success py-2 mb-3" style="font-size:0.85rem;">
                    <i class="bi bi-check-circle me-1"></i>Branding updated successfully. Changes are live.
                </div>
            </c:if>

            <%-- Error alert --%>
            <c:if test="${not empty param.error}">
                <div class="alert alert-danger py-2 mb-3" style="font-size:0.85rem;">
                    <i class="bi bi-exclamation-triangle me-1"></i>${param.error}
                </div>
            </c:if>

            <form method="post" action="UploadPspBranding" enctype="multipart/form-data">

                <%-- NAVBAR LOGO --%>
                <div class="brand-card">
                    <h6><i class="bi bi-image me-1"></i>Navbar Logo</h6>
                    <div class="specs">PNG &bull; Max 300 × 80 px &bull; Displays at 36px height in the navigation bar</div>
                    <div class="row align-items-center">
                        <div class="col-4">
                            <div class="preview-box dark-bg" id="navbarPreviewBox">
                                <img id="navbarPreview" src="${pageContext.request.contextPath}${requestScope.currentNavbarLogo}" alt="Current">
                            </div>
                            <div class="current-label">Current</div>
                        </div>
                        <div class="col-8">
                            <div class="file-input-wrapper">
                                <input type="file" class="form-control form-control-sm" name="navbarLogo" accept="image/png"
                                       onchange="previewFile(this, 'navbarPreview')">
                            </div>
                            <div class="upload-hint">Leave blank to keep current logo</div>
                        </div>
                    </div>
                </div>

                <%-- LOGIN LOGO --%>
                <div class="brand-card">
                    <h6><i class="bi bi-window me-1"></i>Login Page Logo</h6>
                    <div class="specs">PNG &bull; Max 800 × 400 px &bull; Featured on the sign-in page</div>
                    <div class="row align-items-center">
                        <div class="col-4">
                            <div class="preview-box login-preview dark-bg" id="loginPreviewBox">
                                <img id="loginPreview" src="${pageContext.request.contextPath}${requestScope.currentLoginLogo}" alt="Current">
                            </div>
                            <div class="current-label">Current</div>
                        </div>
                        <div class="col-8">
                            <div class="file-input-wrapper">
                                <input type="file" class="form-control form-control-sm" name="loginLogo" accept="image/png"
                                       onchange="previewFile(this, 'loginPreview')">
                            </div>
                            <div class="upload-hint">Leave blank to keep current logo</div>
                        </div>
                    </div>
                </div>

                <%-- FAVICON --%>
                <div class="brand-card">
                    <h6><i class="bi bi-bookmark-star me-1"></i>Favicon</h6>
                    <div class="specs">ICO (under 100KB) or PNG (max 32 × 32 px) &bull; Browser tab icon</div>
                    <div class="row align-items-center">
                        <div class="col-4">
                            <div class="preview-box favicon-preview" id="faviconPreviewBox">
                                <img id="faviconPreview" src="${pageContext.request.contextPath}${requestScope.currentFavicon}" alt="Current">
                            </div>
                            <div class="current-label">Current</div>
                        </div>
                        <div class="col-8">
                            <div class="file-input-wrapper">
                                <input type="file" class="form-control form-control-sm" name="favicon" accept=".ico,image/png,image/x-icon"
                                       onchange="previewFile(this, 'faviconPreview')">
                            </div>
                            <div class="upload-hint">Leave blank to keep current favicon</div>
                        </div>
                    </div>
                </div>

                <%-- SUBMIT --%>
                <div class="d-flex gap-2 mt-3 mb-4">
                    <button type="submit" class="btn btn-ssa">
                        <i class="bi bi-cloud-arrow-up me-1"></i>Upload &amp; Apply
                    </button>
                    <a href="ViewHome25" class="btn btn-outline-ssa">Cancel</a>
                </div>

            </form>
        </div>
        <div class="col"></div>
    </div>
</div>

<script>
    function previewFile(input, previewId) {
        const preview = document.getElementById(previewId);
        if (input.files && input.files[0]) {
            const reader = new FileReader();
            reader.onload = function(e) {
                preview.src = e.target.result;
            };
            reader.readAsDataURL(input.files[0]);
        }
    }
</script>
</body>
</html>
