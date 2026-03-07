<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Site Updated — ${not empty applicationScope.global.psp.fullName ? applicationScope.global.psp.fullName : 'Superior State Administration'}</title>
    <c:set var="fav" value="${not empty applicationScope.global.favicon ? applicationScope.global.favicon : '/images/ssa-favicon.png'}"/>
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}${fav}">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <c:set var="headerBg" value="${not empty applicationScope.global.landingHeaderColor ? applicationScope.global.landingHeaderColor : '#0d5681'}"/>
    <style>
        * { box-sizing: border-box; }
        body { font-family: 'DM Sans', sans-serif; margin: 0; padding: 0; background: #f5f7fa; }
        .tpo-header {
            background: ${headerBg};
            padding: 0.5rem 1.5rem;
            display: flex; justify-content: space-between; align-items: center;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        .tpo-header img { height: 40px; }
        .tpo-wrap {
            min-height: calc(100vh - 56px);
            display: flex; align-items: center; justify-content: center;
            padding: 2rem 1rem;
        }
        .tpo-card {
            background: #fff;
            border-radius: 16px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.08);
            max-width: 580px;
            width: 100%;
            padding: 2.5rem 2rem;
            text-align: center;
        }
        .tpo-icon {
            font-size: 3rem;
            color: ${headerBg};
            margin-bottom: 1rem;
        }
        .tpo-card h1 {
            font-size: 1.5rem;
            font-weight: 700;
            color: #1a1a1a;
            margin-bottom: 0.75rem;
        }
        .tpo-card p {
            color: #555;
            font-size: 1rem;
            line-height: 1.6;
            margin-bottom: 1.25rem;
        }
        .tpo-contact {
            background: #f0f4f8;
            border-radius: 10px;
            padding: 1.25rem;
            margin-top: 1.5rem;
            text-align: left;
        }
        .tpo-contact h2 {
            font-size: 0.95rem;
            font-weight: 600;
            color: #333;
            margin-bottom: 0.5rem;
        }
        .tpo-contact p {
            font-size: 0.9rem;
            color: #555;
            margin-bottom: 0.35rem;
        }
        .tpo-contact a {
            color: ${headerBg};
            text-decoration: none;
            font-weight: 500;
        }
        .tpo-contact a:hover { text-decoration: underline; }
        .tpo-home-btn {
            display: inline-block;
            margin-top: 1.5rem;
            background: ${headerBg};
            color: #fff;
            padding: 0.6rem 1.5rem;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 600;
            font-size: 0.95rem;
            transition: opacity 0.2s;
        }
        .tpo-home-btn:hover { opacity: 0.9; color: #fff; }
    </style>
</head>
<body>
    <div class="tpo-header">
        <div>
            <c:set var="navLogo" value="${not empty applicationScope.global.logoNavbar ? applicationScope.global.logoNavbar : '/images/logoA.png'}"/>
            <img src="${pageContext.request.contextPath}${navLogo}" alt="Home">
        </div>
    </div>

    <div class="tpo-wrap">
        <div class="tpo-card">
            <div class="tpo-icon"><i class="bi bi-info-circle"></i></div>
            <h1>Our Site Has Been Updated</h1>
            <p>
                The page you were trying to reach is no longer available at this address.
                We've upgraded our systems to serve you better.
            </p>
            <p>
                If you need an updated proposal or have questions about your benefits,
                please contact your agent or reach out to us directly.
            </p>

            <div class="tpo-contact">
                <h2><i class="bi bi-telephone me-1"></i> Contact Us</h2>
                <p>
                    <strong>${not empty applicationScope.global.psp.fullName ? applicationScope.global.psp.fullName : 'Superior State Administration'}</strong>
                </p>
            </div>

            <a href="${pageContext.request.contextPath}/login" class="tpo-home-btn">
                <i class="bi bi-house-door me-1"></i> Go to Home Page
            </a>
        </div>
    </div>
</body>
</html>
