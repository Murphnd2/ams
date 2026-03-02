<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>
        <c:choose>
            <c:when test="${sessionScope.uninitialized != 1}">Initialize AMS</c:when>
            <c:otherwise>${applicationScope.global.psp.fullName}</c:otherwise>
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
        .init-wrapper {
            width: 100%;
            max-width: 560px;
            padding: 1rem;
        }
        .init-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.10);
            overflow: hidden;
        }
        .init-header {
            background: #0d5681;
            padding: 1.25rem 2rem;
            text-align: center;
        }
        .init-header img {
            height: 48px;
            margin-bottom: 0.5rem;
        }
        .init-header h1 {
            color: white;
            font-size: 1.15rem;
            font-weight: 600;
            margin: 0;
        }
        .init-header p {
            color: rgba(255,255,255,0.7);
            font-size: 0.8rem;
            margin: 0.3rem 0 0;
        }
        .init-body {
            padding: 1.5rem 2rem 2rem;
        }
        .section-label {
            font-size: 0.72rem;
            font-weight: 700;
            color: #0d5681;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            margin: 1.25rem 0 0.5rem;
            padding-bottom: 0.25rem;
            border-bottom: 2px solid #e8eef4;
        }
        .section-label:first-child {
            margin-top: 0;
        }
        .init-body .form-label {
            font-size: 0.78rem;
            font-weight: 600;
            color: #4a5568;
            margin-bottom: 0.15rem;
        }
        .init-body .form-control, .init-body .form-select {
            border-radius: 8px;
            padding: 0.5rem 0.75rem;
            border: 1.5px solid #d1d5db;
            font-size: 0.85rem;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        .init-body .form-control:focus, .init-body .form-select:focus {
            border-color: #0d5681;
            box-shadow: 0 0 0 3px rgba(13,86,129,0.12);
        }
        .init-body .form-control::placeholder {
            color: #b0b8c4;
            font-size: 0.8rem;
        }
        .btn-initialize {
            background: #0d5681;
            color: white;
            border: none;
            border-radius: 8px;
            padding: 0.65rem;
            font-size: 0.9rem;
            font-weight: 600;
            width: 100%;
            transition: background 0.2s, transform 0.1s;
        }
        .btn-initialize:hover {
            background: #0a4568;
            color: white;
            transform: translateY(-1px);
        }
        .btn-initialize:active { transform: translateY(0); }
        .init-footer {
            text-align: center;
            padding: 0 2rem 1.25rem;
            font-size: 0.75rem;
            color: #9ca3af;
        }
        /* Post-init state */
        .success-wrapper {
            text-align: center;
            padding: 3rem 2rem;
        }
        .success-wrapper .checkmark {
            font-size: 3rem;
            color: #87a948;
            margin-bottom: 1rem;
        }
        .success-wrapper h2 {
            font-size: 1.1rem;
            font-weight: 600;
            color: #1a202c;
            margin-bottom: 0.5rem;
        }
        .success-wrapper p {
            font-size: 0.85rem;
            color: #6b7280;
            margin-bottom: 1.5rem;
        }
    </style>
</head>
<body>
    <div class="init-wrapper">
        <div class="init-card">
            <div class="init-header">
                <img src="${pageContext.request.contextPath}/images/logoA.png" alt="AMS">
                <c:choose>
                    <c:when test="${sessionScope.uninitialized != 1}">
                        <h1><i class="bi bi-gear me-2"></i>Initialize Database</h1>
                        <p>Set up your AMS instance</p>
                    </c:when>
                    <c:otherwise>
                        <h1>${applicationScope.global.psp.fullName}</h1>
                        <p>System is initialized</p>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:choose>
                <%-- UNINITIALIZED: Show setup form --%>
                <c:when test="${sessionScope.uninitialized != 1}">
                    <div class="init-body">

                        <%-- Error message --%>
                        <c:if test="${not empty requestScope.initError}">
                            <div class="alert alert-danger py-2 mb-3" style="font-size:0.85rem;">
                                <i class="bi bi-exclamation-triangle me-1"></i>${requestScope.initError}
                            </div>
                        </c:if>

                        <form method="post" action="InitializeDataBase">

                            <%-- DEPLOYMENT KEY --%>
                            <div class="section-label"><i class="bi bi-shield-lock me-1"></i>Authorization</div>
                            <div class="mb-2">
                                <label class="form-label" for="deploymentKey">Deployment Key</label>
                                <input type="password" class="form-control" name="deploymentKey" id="deploymentKey" required>
                                <small class="text-muted">Format: PSP-yourkey or BPO-yourkey (optional demo tag: PSP-yourkey-DEMO)</small>
                            </div>

                            <%-- COMPANY --%>
                            <div class="section-label"><i class="bi bi-building me-1"></i>Company</div>
                            <div class="mb-2">
                                <label class="form-label" for="pspName">Company Name</label>
                                <input type="text" class="form-control" name="pspName" id="pspName" maxlength="200" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label" for="phone">Phone</label>
                                <input type="tel" class="form-control" name="phone" id="phone" maxlength="12" placeholder="###-###-####" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label" for="taxId">Tax ID</label>
                                <input type="text" class="form-control" name="taxId" id="taxId" maxlength="10" placeholder="##-#######" required>
                            </div>

                            <%-- ADDRESS --%>
                            <div class="section-label"><i class="bi bi-geo-alt me-1"></i>Address</div>
                            <div class="mb-2">
                                <label class="form-label" for="address">Street Address</label>
                                <input type="text" class="form-control" name="address" id="address" maxlength="100" required>
                            </div>
                            <div class="row mb-2">
                                <div class="col-6">
                                    <label class="form-label" for="city">City</label>
                                    <input type="text" class="form-control" name="city" id="city" maxlength="50" required>
                                </div>
                                <div class="col-2">
                                    <label class="form-label" for="state">State</label>
                                    <input type="text" class="form-control" name="state" id="state" maxlength="2" placeholder="MI" required>
                                </div>
                                <div class="col-4">
                                    <label class="form-label" for="zipCode">Zip</label>
                                    <input type="text" class="form-control" name="zipCode" id="zipCode" maxlength="5" placeholder="49858" required>
                                </div>
                            </div>

                            <%-- PRIMARY CONTACT --%>
                            <div class="section-label"><i class="bi bi-person me-1"></i>Primary Contact</div>
                            <div class="row mb-2">
                                <div class="col-6">
                                    <label class="form-label" for="firstName">First Name</label>
                                    <input type="text" class="form-control" name="firstName" id="firstName" maxlength="50" required>
                                </div>
                                <div class="col-6">
                                    <label class="form-label" for="lastName">Last Name</label>
                                    <input type="text" class="form-control" name="lastName" id="lastName" maxlength="80" required>
                                </div>
                            </div>
                            <div class="mb-2">
                                <label class="form-label" for="email">Email <span style="font-weight:400; color:#9ca3af;">(also your username)</span></label>
                                <input type="email" class="form-control" name="email" id="email" maxlength="100" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label" for="password">Password</label>
                                <input type="password" class="form-control" name="password" id="password" minlength="8" required>
                            </div>

                            <%-- DOMAIN & SERVICES --%>
                            <div class="section-label"><i class="bi bi-globe me-1"></i>Domain &amp; Services</div>
                            <div class="mb-2">
                                <label class="form-label" for="domain">Domain Name</label>
                                <input type="url" class="form-control" name="domain" id="domain" placeholder="https://yourdomain.com" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label" for="summit">Summit Admin Path</label>
                                <input type="url" class="form-control" name="summit" id="summit" placeholder="yourdomain.summitwith.us" required>
                            </div>

                            <%-- SMTP --%>
                            <div class="section-label"><i class="bi bi-envelope me-1"></i>Email (SMTP)</div>
                            <div class="row mb-2">
                                <div class="col-8">
                                    <label class="form-label" for="smtpServer">SMTP Server</label>
                                    <input type="text" class="form-control" name="smtpServer" id="smtpServer" placeholder="smtp.example.com" required>
                                </div>
                                <div class="col-4">
                                    <label class="form-label" for="smtpPort">Port</label>
                                    <input type="text" class="form-control" name="smtpPort" id="smtpPort" placeholder="2525" required>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-6">
                                    <label class="form-label" for="smtpUser">SMTP Username</label>
                                    <input type="text" class="form-control" name="smtpUser" id="smtpUser" required>
                                </div>
                                <div class="col-6">
                                    <label class="form-label" for="smtpPwd">SMTP Password</label>
                                    <input type="password" class="form-control" name="smtpPwd" id="smtpPwd" required>
                                </div>
                            </div>

                            <%-- SUBMIT --%>
                            <button type="submit" class="btn btn-initialize">
                                <i class="bi bi-rocket-takeoff me-1"></i>Initialize Database
                            </button>
                        </form>
                    </div>
                </c:when>

                <%-- ALREADY INITIALIZED: Show success state --%>
                <c:otherwise>
                    <div class="success-wrapper">
                        <div class="checkmark"><i class="bi bi-check-circle-fill"></i></div>
                        <h2>System Ready</h2>
                        <p>This instance has already been initialized.</p>
                        <a href="${pageContext.request.contextPath}/login" class="btn btn-initialize" style="max-width:240px; display:inline-block;">
                            <i class="bi bi-box-arrow-in-right me-1"></i>Go to Login
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>

            <div class="init-footer">
                Powered by AMS
            </div>
        </div>
    </div>
</body>
</html>
