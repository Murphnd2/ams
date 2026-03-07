<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${not empty applicationScope.global.psp.fullName ? applicationScope.global.psp.fullName : 'Welcome'}</title>
    <c:set var="fav" value="${not empty applicationScope.global.favicon ? applicationScope.global.favicon : '/images/ssa-favicon.png'}"/>
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}${fav}">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; }
        body { font-family: 'DM Sans', sans-serif; margin: 0; padding: 0; }
        .landing-header {
            position: fixed; top: 0; left: 0; right: 0; z-index: 1000;
            background: ${applicationScope.global.landingHeaderColor};
            padding: 0.5rem 1.5rem;
            display: flex; justify-content: space-between; align-items: center;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        .landing-header img { height: 40px; }
        .landing-login-btn {
            background: rgba(255,255,255,0.12);
            color: ${applicationScope.global.landingHeaderTextColor};
            border: 1.5px solid rgba(255,255,255,0.3);
            padding: 0.4rem 1.2rem; border-radius: 6px;
            font-weight: 600; font-size: 0.9rem;
            cursor: pointer; transition: all 0.2s;
            text-decoration: none; display: inline-flex; align-items: center; gap: 0.35rem;
        }
        .landing-login-btn:hover {
            background: rgba(255,255,255,0.22);
            border-color: rgba(255,255,255,0.5);
            color: ${applicationScope.global.landingHeaderTextColor};
        }
        .landing-body { padding-top: 56px; }
    </style>
</head>
<body>
    <%-- FIXED HEADER: logo left, login right --%>
    <div class="landing-header">
        <div>
            <c:set var="navLogo" value="${not empty applicationScope.global.logoNavbar ? applicationScope.global.logoNavbar : '/images/logoA.png'}"/>
            <img src="${pageContext.request.contextPath}${navLogo}" alt="Home">
        </div>
        <button class="landing-login-btn" data-bs-toggle="modal" data-bs-target="#loginModal">
            <i class="bi bi-box-arrow-in-right"></i>Login
        </button>
    </div>

    <%-- CUSTOM HTML CONTENT (pre-sanitized on save) --%>
    <div class="landing-body">
        ${requestScope.landingHtml}
    </div>

    <%-- LOGIN MODAL --%>
    <c:import url="/WEB-INF/view/authentication/loginFormModal.jsp"/>

    <%-- Auto-open modal on login error --%>
    <c:if test="${not empty sessionScope.loginError}">
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Inject error alert into modal body
            var modalBody = document.querySelector('#loginModal .modal-body');
            if (modalBody) {
                var alert = document.createElement('div');
                alert.className = 'alert alert-danger py-2 mb-3';
                alert.style.fontSize = '0.85rem';
                alert.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.loginError}';
                modalBody.insertBefore(alert, modalBody.firstChild);
            }
            new bootstrap.Modal(document.getElementById('loginModal')).show();
        });
    </script>
    <c:remove var="loginError" scope="session"/>
    </c:if>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <%-- Scroll-triggered fade-in animations for landing page content.
         Content marks elements with class "ss-fade" (or "fade-in"); this observer
         adds "ss-visible" / "visible" when they enter the viewport. --%>
    <script>
        (function() {
            var obs = new IntersectionObserver(function(entries) {
                entries.forEach(function(e) {
                    if (e.isIntersecting) {
                        e.target.classList.add('ss-visible');
                        e.target.classList.add('visible');
                    }
                });
            }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });
            document.querySelectorAll('.ss-fade, .fade-in').forEach(function(el) { obs.observe(el); });

            // Smooth scroll for anchor links within the landing content
            document.querySelectorAll('.landing-body a[href^="#"]').forEach(function(anchor) {
                anchor.addEventListener('click', function(e) {
                    var href = this.getAttribute('href');
                    if (href !== '#') {
                        var target = document.querySelector(href);
                        if (target) {
                            e.preventDefault();
                            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        }
                    }
                });
            });
        })();
    </script>
</body>
</html>
