<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Request a Quote${not empty pspName ? ' - '.concat(pspName) : ''}</title>
    <c:set var="fav" value="${not empty favicon ? favicon : '/images/ssa-favicon.png'}"/>
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}${fav}">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: 'DM Sans', sans-serif;
            margin: 0; padding: 0;
            background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
            min-height: 100vh;
        }
        .quote-header {
            position: fixed; top: 0; left: 0; right: 0; z-index: 1000;
            background: ${applicationScope.global.landingHeaderColor};
            padding: 0.5rem 1.5rem;
            display: flex; justify-content: space-between; align-items: center;
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        .quote-header img { height: 40px; }
        .quote-header a {
            color: ${applicationScope.global.landingHeaderTextColor}; text-decoration: none;
            font-weight: 500; font-size: 0.9rem;
            display: inline-flex; align-items: center; gap: 0.3rem;
            transition: color 0.2s;
        }
        .quote-header a:hover { color: ${applicationScope.global.landingHeaderTextColor}; }
        .quote-brand-wordmark {
            color: ${applicationScope.global.landingHeaderTextColor};
            font-weight: 700; font-size: 1.1rem; letter-spacing: 0.02em;
        }
        .quote-body {
            padding-top: 80px; padding-bottom: 3rem;
            display: flex; justify-content: center; align-items: flex-start;
        }
        .quote-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.10);
            width: 100%; max-width: 560px;
            margin: 0 1rem;
            overflow: hidden;
        }
        .quote-card-header {
            background: ${applicationScope.global.landingHeaderColor};
            padding: 1.5rem 2rem;
            text-align: center;
        }
        .quote-card-header h2 {
            color: white; margin: 0;
            font-size: 1.3rem; font-weight: 700;
        }
        .quote-card-header p {
            color: rgba(255,255,255,0.75); margin: 0.3rem 0 0;
            font-size: 0.85rem;
        }
        .quote-card-body { padding: 2rem; }
        .form-label {
            font-size: 0.8rem; font-weight: 600;
            color: #4a5568; text-transform: uppercase;
            letter-spacing: 0.03em; margin-bottom: 0.25rem;
        }
        .form-control {
            border-radius: 8px; padding: 0.6rem 0.9rem;
            border: 1.5px solid #d1d5db;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        .form-control:focus {
            border-color: ${applicationScope.global.landingHeaderColor};
            box-shadow: 0 0 0 3px rgba(13,86,129,0.12);
        }
        .los-section {
            background: #f8f9fa; border-radius: 8px;
            padding: 1rem 1.25rem; margin-bottom: 1rem;
        }
        .los-section h6 {
            font-size: 0.8rem; font-weight: 600;
            color: #4a5568; text-transform: uppercase;
            letter-spacing: 0.03em; margin-bottom: 0.75rem;
        }
        .form-check { margin-bottom: 0.4rem; }
        .form-check-label { font-size: 0.9rem; color: #374151; }
        .form-check-input:checked {
            background-color: ${applicationScope.global.landingHeaderColor}; border-color: ${applicationScope.global.landingHeaderColor};
        }
        .btn-quote {
            background: ${applicationScope.global.landingHeaderColor}; color: white; border: none;
            border-radius: 8px; padding: 0.65rem;
            font-size: 0.95rem; font-weight: 600;
            width: 100%; transition: background 0.2s, transform 0.1s;
        }
        .btn-quote:hover {
            background: #0a4568; color: white; transform: translateY(-1px);
        }
        .btn-quote:active { transform: translateY(0); }
        .confirm-icon {
            width: 64px; height: 64px; border-radius: 50%;
            background: #d4edda; color: #28a745;
            display: flex; align-items: center; justify-content: center;
            font-size: 2rem; margin: 0 auto 1rem;
        }
        .confirm-body { text-align: center; padding: 2.5rem 2rem; }
        .confirm-body h3 { color: #1a202c; font-weight: 700; margin-bottom: 0.5rem; }
        .confirm-body p { color: #6b7280; font-size: 0.95rem; margin-bottom: 1.5rem; }
        .btn-home {
            background: transparent; color: ${applicationScope.global.landingHeaderColor};
            border: 1.5px solid ${applicationScope.global.landingHeaderColor}; border-radius: 8px;
            padding: 0.5rem 1.5rem; font-weight: 600;
            text-decoration: none; display: inline-block;
            transition: background 0.2s, color 0.2s;
        }
        .btn-home:hover { background: ${applicationScope.global.landingHeaderColor}; color: white; }
        .required-star { color: #dc3545; }
    </style>
</head>
<body>
    <%-- FIXED HEADER --%>
    <div class="quote-header">
        <div>
            <c:choose>
                <%-- Agency host resolved (V068): name-only wordmark, no PSP logo. --%>
                <c:when test="${not empty brandName}">
                    <span class="quote-brand-wordmark">${brandName}</span>
                </c:when>
                <c:otherwise>
                    <c:set var="navLogo" value="${not empty logoNavbar ? logoNavbar : '/images/logoA.png'}"/>
                    <img src="${pageContext.request.contextPath}${navLogo}" alt="Home">
                </c:otherwise>
            </c:choose>
        </div>
        <a href="${pageContext.request.contextPath}/login">
            <i class="bi bi-arrow-left"></i> Back
        </a>
    </div>

    <div class="quote-body">
        <div class="quote-card">

            <c:choose>
                <%-- CONFIRMATION SCREEN --%>
                <c:when test="${submitted}">
                    <div class="confirm-body">
                        <div class="confirm-icon">
                            <i class="bi bi-check-lg"></i>
                        </div>
                        <h3>Thank You!</h3>
                        <p>
                            We've received your quote request for <strong>${companyName}</strong>.
                            A member of our team will be in touch shortly.
                        </p>
                        <a href="${pageContext.request.contextPath}/login" class="btn-home">
                            <i class="bi bi-house me-1"></i>Back to Home
                        </a>
                    </div>
                </c:when>

                <%-- QUOTE FORM --%>
                <c:otherwise>
                    <div class="quote-card-header">
                        <h2><i class="bi bi-envelope-paper me-2"></i>Request a Quote</h2>
                        <p>Tell us about your organization and we'll prepare a customized proposal.</p>
                    </div>
                    <div class="quote-card-body">

                        <c:if test="${not empty error}">
                            <div class="alert alert-danger py-2 mb-3" style="font-size:0.85rem;">
                                <i class="bi bi-exclamation-triangle me-1"></i>${error}
                            </div>
                        </c:if>

                        <form method="post" action="${pageContext.request.contextPath}/RequestQuote">
                            <%-- Honeypot: hidden from humans via CSS, bots fill it --%>
                            <div style="position:absolute;left:-9999px;top:-9999px;" aria-hidden="true" tabindex="-1">
                                <label for="website">Website</label>
                                <input type="text" name="website" id="website" value="" autocomplete="off" tabindex="-1">
                            </div>
                            <input type="hidden" name="formLoadedAt" id="formLoadedAt" value="">
                            <input type="hidden" name="k" id="k" value="${quoteToken}">
                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label class="form-label" for="firstName">First Name <span class="required-star">*</span></label>
                                    <input type="text" class="form-control" name="firstName" id="firstName"
                                           value="${firstName}" placeholder="First name" required>
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="form-label" for="lastName">Last Name <span class="required-star">*</span></label>
                                    <input type="text" class="form-control" name="lastName" id="lastName"
                                           value="${lastName}" placeholder="Last name" required>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Preferred Contact Method <span class="required-star">*</span></label>
                                <div class="d-flex gap-3 mt-1">
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="contactMethod" id="contactEmail"
                                               value="email" ${empty contactMethod || contactMethod == 'email' ? 'checked' : ''}
                                               onchange="toggleContactRequired()">
                                        <label class="form-check-label" for="contactEmail">Email</label>
                                    </div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="contactMethod" id="contactPhone"
                                               value="phone" ${contactMethod == 'phone' ? 'checked' : ''}
                                               onchange="toggleContactRequired()">
                                        <label class="form-check-label" for="contactPhone">Phone</label>
                                    </div>
                                </div>
                            </div>

                            <div class="row">
                                <div class="col-md-6 mb-3">
                                    <label class="form-label" for="email">Email <span class="required-star" id="emailStar">*</span></label>
                                    <input type="email" class="form-control" name="email" id="email"
                                           value="${email}" placeholder="your@email.com">
                                </div>
                                <div class="col-md-6 mb-3">
                                    <label class="form-label" for="phone">Phone <span class="required-star" id="phoneStar" style="display:none">*</span></label>
                                    <input type="tel" class="form-control" name="phone" id="phone"
                                           value="${phone}" placeholder="(555) 123-4567">
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label" for="companyName">Company Name <span class="required-star">*</span></label>
                                <input type="text" class="form-control" name="companyName" id="companyName"
                                       value="${companyName}" placeholder="Company name" required>
                            </div>

                            <div class="mb-3">
                                <label class="form-label" for="employeeCount">Number of Employees</label>
                                <input type="number" class="form-control" name="employeeCount" id="employeeCount"
                                       value="${employeeCount}" placeholder="Approximate headcount" min="1">
                            </div>

                            <c:if test="${not empty losList}">
                                <div class="los-section">
                                    <h6>Services of Interest</h6>
                                    <c:forEach var="los" items="${losList}">
                                        <div class="form-check">
                                            <input class="form-check-input" type="checkbox"
                                                   name="losIds" value="${los.id}" id="los_${los.id}">
                                            <label class="form-check-label" for="los_${los.id}">
                                                ${los.description}
                                            </label>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>

                            <div class="mb-3">
                                <label class="form-label" for="additionalInfo">Any Additional Info?</label>
                                <textarea class="form-control" name="additionalInfo" id="additionalInfo"
                                          rows="3" placeholder="Anything else you'd like us to know...">${additionalInfo}</textarea>
                            </div>

                            <button type="submit" class="btn btn-quote">
                                <i class="bi bi-send me-1"></i>Submit Request
                            </button>
                        </form>
                    </div>
                </c:otherwise>
            </c:choose>

        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function toggleContactRequired() {
            var isEmail = document.getElementById('contactEmail').checked;
            var emailInput = document.getElementById('email');
            var phoneInput = document.getElementById('phone');
            var emailStar = document.getElementById('emailStar');
            var phoneStar = document.getElementById('phoneStar');
            emailInput.required = isEmail;
            phoneInput.required = !isEmail;
            emailStar.style.display = isEmail ? '' : 'none';
            phoneStar.style.display = isEmail ? 'none' : '';
        }
        document.addEventListener('DOMContentLoaded', function() {
            toggleContactRequired();
            document.getElementById('formLoadedAt').value = Date.now().toString();
        });
    </script>
</body>
</html>
