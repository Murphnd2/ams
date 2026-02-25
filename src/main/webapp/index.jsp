<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>
        <c:choose>
            <c:when test="${sessionScope.local.isAuthenticated()==true}">
                ${sessionScope.psp.getFullName()}
            </c:when>
            <c:otherwise>Sign In</c:otherwise>
        </c:choose>
    </title>
    <style>
        body {
            background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .login-wrapper {
            width: 100%;
            max-width: 420px;
            padding: 0 1rem;
        }
        .login-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.10);
            overflow: hidden;
        }
        .login-header {
            background: #0d5681;
            padding: 1.5rem 2rem;
            text-align: center;
        }
        .login-header img {
            max-height: 60px;
            margin-bottom: 0.5rem;
        }
        .login-header h1 {
            color: white;
            font-size: 1.1rem;
            font-weight: 600;
            margin: 0;
            letter-spacing: 0.02em;
        }
        .login-body {
            padding: 2rem;
        }
        .login-body .form-label {
            font-size: 0.8rem;
            font-weight: 600;
            color: #4a5568;
            text-transform: uppercase;
            letter-spacing: 0.03em;
            margin-bottom: 0.25rem;
        }
        .login-body .form-control {
            border-radius: 8px;
            padding: 0.6rem 0.9rem;
            border: 1.5px solid #d1d5db;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        .login-body .form-control:focus {
            border-color: #0d5681;
            box-shadow: 0 0 0 3px rgba(13,86,129,0.12);
        }
        .btn-login {
            background: #0d5681;
            color: white;
            border: none;
            border-radius: 8px;
            padding: 0.65rem;
            font-size: 0.95rem;
            font-weight: 600;
            width: 100%;
            transition: background 0.2s, transform 0.1s;
        }
        .btn-login:hover {
            background: #0a4568;
            color: white;
            transform: translateY(-1px);
        }
        .btn-login:active {
            transform: translateY(0);
        }
        .help-link {
            display: block;
            text-align: center;
            margin-top: 1rem;
            font-size: 0.85rem;
            color: #0d5681;
            text-decoration: none;
        }
        .help-link:hover {
            text-decoration: underline;
            color: #0a4568;
        }
        .login-footer {
            text-align: center;
            padding: 0 2rem 1.5rem;
            font-size: 0.75rem;
            color: #9ca3af;
        }
    </style>
</head>
<body>
    <div class="login-wrapper">
        <div class="login-card">
            <div class="login-header">
                <c:set var="loginLogo" value="${not empty applicationScope.global.logoLogin ? applicationScope.global.logoLogin : '/images/logoD.png'}"/>
                <img src="${pageContext.request.contextPath}${loginLogo}" class="img-fluid" alt="Logo" style="max-height: 400px;">
            </div>
            <div class="login-body">

                <%-- Success message (e.g. after password reset) --%>
                <c:if test="${param.reset == 'success'}">
                    <div class="alert alert-success py-2 mb-3" style="font-size:0.85rem;">
                        <i class="bi bi-check-circle me-1"></i>Password updated successfully. Please sign in.
                    </div>
                </c:if>

                <%-- Error message from failed login --%>
                <c:if test="${not empty sessionScope.loginError}">
                    <div class="alert alert-danger py-2 mb-3" style="font-size:0.85rem;">
                        <i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.loginError}
                    </div>
                    <c:remove var="loginError" scope="session"/>
                </c:if>

                <form method="post" action="AuthenticateUser">
                    <div class="mb-3">
                        <label class="form-label" for="userName">Username or Email</label>
                        <input type="text" class="form-control" name="userName" id="userName"
                               placeholder="Enter your username or email" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="userPassword">Password</label>
                        <input type="password" class="form-control" name="userPassword" id="userPassword"
                               placeholder="Enter your password" required>
                    </div>
                    <button type="submit" class="btn btn-login" name="submitButton" value="0">
                        <i class="bi bi-box-arrow-in-right me-1"></i>Sign In
                    </button>
                </form>

                <a href="NeedsHelp" class="help-link">
                    <i class="bi bi-question-circle me-1"></i>Need help logging in?
                </a>
            </div>
            <div class="login-footer">
                Powered by AMS
            </div>
        </div>
    </div>
</body>
</html>
