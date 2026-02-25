<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Login Help</title>
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
        .help-wrapper {
            width: 100%;
            max-width: 420px;
            padding: 0 1rem;
        }
        .help-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.10);
            overflow: hidden;
        }
        .help-header {
            background: #0d5681;
            padding: 1.25rem 2rem;
            text-align: center;
        }
        .help-header h1 {
            color: white;
            font-size: 1.1rem;
            font-weight: 600;
            margin: 0;
        }
        .help-header p {
            color: rgba(255,255,255,0.7);
            font-size: 0.8rem;
            margin: 0.3rem 0 0;
        }
        .help-body {
            padding: 2rem;
        }
        .help-body .form-label {
            font-size: 0.8rem;
            font-weight: 600;
            color: #4a5568;
            text-transform: uppercase;
            letter-spacing: 0.03em;
            margin-bottom: 0.25rem;
        }
        .help-body .form-control {
            border-radius: 8px;
            padding: 0.6rem 0.9rem;
            border: 1.5px solid #d1d5db;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        .help-body .form-control:focus {
            border-color: #0d5681;
            box-shadow: 0 0 0 3px rgba(13,86,129,0.12);
        }
        .btn-reset {
            background: #0d5681;
            color: white;
            border: none;
            border-radius: 8px;
            padding: 0.6rem;
            font-size: 0.9rem;
            font-weight: 600;
            width: 100%;
            transition: background 0.2s, transform 0.1s;
        }
        .btn-reset:hover {
            background: #0a4568;
            color: white;
            transform: translateY(-1px);
        }
        .btn-reset:active { transform: translateY(0); }
        .btn-onetime {
            background: white;
            color: #0d5681;
            border: 2px solid #0d5681;
            border-radius: 8px;
            padding: 0.6rem;
            font-size: 0.9rem;
            font-weight: 600;
            width: 100%;
            transition: background 0.2s, transform 0.1s;
        }
        .btn-onetime:hover {
            background: #f0f7fc;
            color: #0a4568;
            transform: translateY(-1px);
        }
        .btn-onetime:active { transform: translateY(0); }
        .back-link {
            display: block;
            text-align: center;
            margin-top: 1rem;
            font-size: 0.85rem;
            color: #0d5681;
            text-decoration: none;
        }
        .back-link:hover {
            text-decoration: underline;
            color: #0a4568;
        }
        .help-footer {
            text-align: center;
            padding: 0 2rem 1.5rem;
            font-size: 0.75rem;
            color: #9ca3af;
        }
        .option-divider {
            display: flex;
            align-items: center;
            margin: 1rem 0;
            color: #9ca3af;
            font-size: 0.8rem;
        }
        .option-divider::before, .option-divider::after {
            content: '';
            flex: 1;
            border-bottom: 1px solid #e5e7eb;
        }
        .option-divider span {
            padding: 0 0.75rem;
        }
    </style>
</head>
<body>
    <div class="help-wrapper">
        <div class="help-card">
            <div class="help-header">
                <h1><i class="bi bi-question-circle me-2"></i>Login Help</h1>
                <p>We'll help you get back into your account</p>
            </div>
            <div class="help-body">

                <%-- Success message --%>
                <c:if test="${param.status == 'sent'}">
                    <div class="alert alert-success py-2 mb-3" style="font-size:0.85rem;">
                        <i class="bi bi-check-circle me-1"></i>Check your email — we've sent instructions to help you log in.
                    </div>
                </c:if>

                <%-- Error: user not found --%>
                <c:if test="${param.status == 'notfound'}">
                    <div class="alert alert-danger py-2 mb-3" style="font-size:0.85rem;">
                        <i class="bi bi-exclamation-triangle me-1"></i>We couldn't find an account with that username or email. Please try again.
                    </div>
                </c:if>

                <form method="post" action="HelpUserLogin">
                    <div class="mb-3">
                        <label class="form-label" for="userName">Username or Email</label>
                        <input type="text" class="form-control" name="userName" id="userName"
                               placeholder="Enter your username or email" required autofocus>
                    </div>

                    <button type="submit" class="btn btn-reset mb-2" name="submitButton" value="1">
                        <i class="bi bi-key me-1"></i>Reset My Password
                    </button>

                    <div class="option-divider"><span>or</span></div>

                    <button type="submit" class="btn btn-onetime" name="submitButton" value="0">
                        <i class="bi bi-link-45deg me-1"></i>Send One-Time Login Link
                    </button>
                </form>

                <a href="${pageContext.request.contextPath}/login" class="back-link">
                    <i class="bi bi-arrow-left me-1"></i>Back to Sign In
                </a>
            </div>
            <div class="help-footer">
                Powered by AMS
            </div>
        </div>
    </div>
</body>
</html>
