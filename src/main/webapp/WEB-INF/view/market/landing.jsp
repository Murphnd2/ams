<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String ctx = request.getContextPath(); // "" when deployed at web root
    String logoPath = ctx + "/images/logo1.png"; // you said: webapp/images/logo1.png
    int year = java.time.Year.now().getValue();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Superior State Employer Solutions | Employee Benefits & HR Services</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=DM+Sans:wght@400;500;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --teal: #005F73;
            --olive: #7A9B3C;
            --navy: #003049;
            --cream: #FAF7F2;
            --sand: #E8DFD0;
            --accent: #E07A5F;
            --slate: #475569;
            --white: #FFFFFF;
            --light-gray: #F5F5F5;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: 'DM Sans', sans-serif;
            color: var(--navy);
            background: var(--cream);
            line-height: 1.6;
            overflow-x: hidden;
        }

        /* Navigation */
        nav {
            position: fixed;
            top: 0; left: 0; right: 0;
            background: rgba(255, 255, 255, 0.98);
            backdrop-filter: blur(10px);
            z-index: 1000;
            border-bottom: 1px solid rgba(0, 95, 115, 0.1);
            animation: slideDown 0.6s ease-out;
        }

        @keyframes slideDown {
            from { transform: translateY(-100%); opacity: 0; }
            to   { transform: translateY(0); opacity: 1; }
        }

        .nav-container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 1rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .logo img {
            height: 55px;
            display: block;
            transition: transform 0.3s ease;
        }

        .logo:hover img { transform: scale(1.05); }

        .nav-links {
            display: flex;
            gap: 2.5rem;
            align-items: center;
        }

        .nav-links a {
            color: var(--slate);
            text-decoration: none;
            font-size: 0.95rem;
            font-weight: 500;
            transition: color 0.3s ease;
            position: relative;
        }

        .nav-links a:hover { color: var(--teal); }

        .nav-links a::after {
            content: '';
            position: absolute;
            bottom: -4px;
            left: 0;
            width: 0;
            height: 2px;
            background: var(--olive);
            transition: width 0.3s ease;
        }

        .nav-links a:hover::after { width: 100%; }

        .phone-link {
            color: var(--teal) !important;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .cta-btn {
            background: var(--teal);
            color: white !important;
            padding: 0.75rem 1.5rem;
            border-radius: 6px;
            font-weight: 600;
            transition: all 0.3s ease;
        }

        .cta-btn:hover {
            background: var(--olive);
            transform: translateY(-2px);
        }

        .cta-btn::after { display: none; }

        /* Hero Section */
        .hero {
            margin-top: 80px;
            min-height: 90vh;
            display: flex;
            align-items: center;
            position: relative;
            overflow: hidden;
            background: linear-gradient(135deg, var(--cream) 0%, var(--sand) 100%);
        }

        .hero::before {
            content: '';
            position: absolute;
            top: -50%;
            right: -20%;
            width: 100%;
            height: 150%;
            background: radial-gradient(circle, rgba(122, 155, 60, 0.08) 0%, transparent 70%);
            animation: pulse 8s ease-in-out infinite;
        }

        @keyframes pulse {
            0%, 100% { transform: scale(1) rotate(0deg); }
            50%      { transform: scale(1.1) rotate(5deg); }
        }

        .hero-container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 4rem 2rem;
            display: grid;
            grid-template-columns: 1.2fr 1fr;
            gap: 4rem;
            align-items: center;
            position: relative;
            z-index: 1;
        }

        .hero-content { animation: fadeInUp 0.8s ease-out 0.2s both; }

        @keyframes fadeInUp {
            from { opacity: 0; transform: translateY(30px); }
            to   { opacity: 1; transform: translateY(0); }
        }

        .eyebrow {
            font-size: 0.9rem;
            letter-spacing: 0.15em;
            text-transform: uppercase;
            color: var(--teal);
            font-weight: 700;
            margin-bottom: 1.5rem;
            display: inline-block;
        }

        h1 {
            font-family: 'Instrument Serif', serif;
            font-size: 4.5rem;
            line-height: 1.1;
            color: var(--navy);
            margin-bottom: 1.5rem;
            letter-spacing: -0.02em;
            font-weight: 400;
        }

        .highlight {
            color: var(--olive);
            font-style: italic;
            position: relative;
            display: inline-block;
        }

        .hero-subtitle {
            font-size: 1.25rem;
            color: var(--slate);
            margin-bottom: 2.5rem;
            line-height: 1.7;
            max-width: 600px;
        }

        .hero-ctas {
            display: flex;
            gap: 1.5rem;
            align-items: center;
        }

        .primary-btn {
            background: var(--teal);
            color: white;
            padding: 1rem 2.5rem;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 600;
            font-size: 1.05rem;
            transition: all 0.3s ease;
            display: inline-block;
        }

        .primary-btn:hover {
            background: var(--olive);
            transform: translateY(-3px);
            box-shadow: 0 10px 30px rgba(0, 95, 115, 0.2);
        }

        .secondary-btn {
            color: var(--navy);
            padding: 1rem 2rem;
            text-decoration: none;
            font-weight: 600;
            font-size: 1.05rem;
            position: relative;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            transition: gap 0.3s ease;
        }

        .secondary-btn:hover { gap: 1rem; color: var(--teal); }

        .secondary-btn::after { content: '→'; transition: transform 0.3s ease; }

        .hero-visual {
            position: relative;
            animation: fadeInUp 0.8s ease-out 0.4s both;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 2rem;
            background: white;
            padding: 2.5rem;
            border-radius: 16px;
            box-shadow: 0 20px 60px rgba(0, 95, 115, 0.12);
        }

        .stat-item {
            text-align: center;
            padding: 1.5rem;
            border-radius: 12px;
            transition: all 0.3s ease;
        }

        .stat-item:hover { background: var(--cream); transform: translateY(-5px); }

        .stat-number {
            font-family: 'Instrument Serif', serif;
            font-size: 3rem;
            color: var(--teal);
            display: block;
            margin-bottom: 0.5rem;
        }

        .stat-label {
            font-size: 0.95rem;
            color: var(--slate);
            font-weight: 500;
        }

        /* Trust Bar */
        .trust-bar {
            background: white;
            padding: 3rem 2rem;
            border-top: 1px solid rgba(0, 95, 115, 0.1);
            border-bottom: 1px solid rgba(0, 95, 115, 0.1);
        }

        .trust-container {
            max-width: 1400px;
            margin: 0 auto;
            text-align: center;
        }

        .trust-text {
            font-size: 0.9rem;
            color: var(--slate);
            margin-bottom: 2rem;
            letter-spacing: 0.05em;
            text-transform: uppercase;
        }

        .trust-items {
            display: flex;
            justify-content: center;
            gap: 4rem;
            flex-wrap: wrap;
        }

        .trust-item { font-size: 1.1rem; color: var(--navy); font-weight: 600; }

        /* Services Section */
        .services {
            padding: 8rem 2rem;
            background: var(--white);
        }

        .services-container { max-width: 1400px; margin: 0 auto; }

        .section-header { text-align: center; margin-bottom: 5rem; }

        .section-eyebrow {
            font-size: 0.9rem;
            letter-spacing: 0.15em;
            text-transform: uppercase;
            color: var(--teal);
            font-weight: 700;
            margin-bottom: 1rem;
        }

        .section-title {
            font-family: 'Instrument Serif', serif;
            font-size: 3.5rem;
            color: var(--navy);
            margin-bottom: 1.5rem;
            font-weight: 400;
        }

        .section-subtitle {
            font-size: 1.2rem;
            color: var(--slate);
            max-width: 700px;
            margin: 0 auto;
        }

        .services-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
            gap: 2rem;
        }

        .service-card {
            background: var(--cream);
            padding: 2.5rem;
            border-radius: 16px;
            transition: all 0.4s ease;
            position: relative;
            overflow: hidden;
            border: 1px solid transparent;
        }

        .service-card::before {
            content: '';
            position: absolute;
            top: 0; left: 0; right: 0;
            height: 4px;
            background: linear-gradient(90deg, var(--teal), var(--olive));
            transform: scaleX(0);
            transform-origin: left;
            transition: transform 0.4s ease;
        }

        .service-card:hover::before { transform: scaleX(1); }

        .service-card:hover {
            transform: translateY(-8px);
            box-shadow: 0 20px 60px rgba(0, 95, 115, 0.15);
            border-color: rgba(0, 95, 115, 0.2);
        }

        .service-icon {
            width: 60px;
            height: 60px;
            background: var(--teal);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 1.5rem;
            font-size: 1.8rem;
            transition: all 0.4s ease;
        }

        .service-card:hover .service-icon {
            background: var(--olive);
            transform: rotate(5deg) scale(1.1);
        }

        .service-title {
            font-family: 'Instrument Serif', serif;
            font-size: 1.8rem;
            color: var(--navy);
            margin-bottom: 1rem;
            font-weight: 400;
        }

        .service-description {
            color: var(--slate);
            margin-bottom: 1.5rem;
            line-height: 1.7;
        }

        .service-link {
            color: var(--teal);
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            transition: gap 0.3s ease;
        }

        .service-link:hover { gap: 1rem; color: var(--olive); }

        .service-link::after { content: '→'; }

        /* CTA Section */
        .cta-section {
            padding: 8rem 2rem;
            background: var(--navy);
            position: relative;
            overflow: hidden;
        }

        .cta-section::before {
            content: '';
            position: absolute;
            top: -50%;
            left: -50%;
            width: 200%;
            height: 200%;
            background: radial-gradient(circle, rgba(122, 155, 60, 0.1) 0%, transparent 70%);
            animation: rotate 20s linear infinite;
        }

        @keyframes rotate {
            from { transform: rotate(0deg); }
            to   { transform: rotate(360deg); }
        }

        .cta-container {
            max-width: 1000px;
            margin: 0 auto;
            text-align: center;
            position: relative;
            z-index: 1;
        }

        .cta-container h2 {
            font-family: 'Instrument Serif', serif;
            font-size: 3.5rem;
            color: white;
            margin-bottom: 1.5rem;
            font-weight: 400;
        }

        .cta-container p {
            font-size: 1.3rem;
            color: rgba(255, 255, 255, 0.8);
            margin-bottom: 3rem;
        }

        .cta-buttons {
            display: flex;
            gap: 1.5rem;
            justify-content: center;
            flex-wrap: wrap;
        }

        .white-btn { background: white; color: var(--navy); }
        .white-btn:hover { background: var(--cream); color: var(--teal); }

        .outline-btn {
            background: transparent;
            color: white;
            border: 2px solid white;
        }

        .outline-btn:hover { background: rgba(255, 255, 255, 0.1); }

        /* Footer */
        footer {
            background: var(--navy);
            color: white;
            padding: 4rem 2rem 2rem;
        }

        .footer-container {
            max-width: 1400px;
            margin: 0 auto;
            display: grid;
            grid-template-columns: 2fr 1fr 1fr 1fr;
            gap: 4rem;
            margin-bottom: 3rem;
        }

        .footer-logo { margin-bottom: 1.5rem; }

        .footer-logo img {
            height: 50px;
            filter: brightness(0) invert(1);
        }

        .footer-description {
            color: rgba(255, 255, 255, 0.7);
            line-height: 1.7;
            margin-bottom: 1.5rem;
        }

        .footer-title {
            font-weight: 700;
            margin-bottom: 1.5rem;
            font-size: 1.1rem;
        }

        .footer-links { list-style: none; }

        .footer-links li { margin-bottom: 0.75rem; }

        .footer-links a {
            color: rgba(255, 255, 255, 0.7);
            text-decoration: none;
            transition: color 0.3s ease;
        }

        .footer-links a:hover { color: var(--olive); }

        .footer-bottom {
            border-top: 1px solid rgba(255, 255, 255, 0.1);
            padding-top: 2rem;
            text-align: center;
            color: rgba(255, 255, 255, 0.5);
        }

        /* Mobile Responsive */
        @media (max-width: 768px) {
            h1 { font-size: 2.5rem; }

            .hero-container {
                grid-template-columns: 1fr;
                gap: 3rem;
            }

            .nav-links { display: none; }

            .services-grid { grid-template-columns: 1fr; }

            .footer-container {
                grid-template-columns: 1fr;
                gap: 2rem;
            }

            .section-title { font-size: 2.5rem; }
            .cta-container h2 { font-size: 2.5rem; }
            .stats-grid { grid-template-columns: 1fr; }

            .trust-items {
                flex-direction: column;
                gap: 1.5rem;
            }
        }

        /* Scroll animations */
        .fade-in {
            opacity: 0;
            transform: translateY(30px);
            transition: opacity 0.6s ease, transform 0.6s ease;
        }

        .fade-in.visible {
            opacity: 1;
            transform: translateY(0);
        }
    </style>
</head>
<body>
<!-- Navigation -->
<nav>
    <div class="nav-container">
        <a href="<%= ctx %>/" class="logo">
            <img src="<%= logoPath %>" alt="Superior State Employer Solutions">
        </a>
        <div class="nav-links">
            <a href="#services">Services</a>
            <a href="#about">About</a>
            <a href="#resources">Resources</a>
            <a href="tel:8008797752" class="phone-link">📞 800-879-7752</a>
            <a href="#quote" class="cta-btn">Get a Quote</a>
        </div>
    </div>
</nav>

<!-- Hero Section -->
<section class="hero">
    <div class="hero-container">
        <div class="hero-content">
            <span class="eyebrow">Employee Benefits Simplified</span>
            <h1>
                <span class="highlight">Smarter</span> benefits.<br>
                Healthier bottom line.
            </h1>
            <p class="hero-subtitle">
                We help employers reduce healthcare costs by 25-35% while delivering exceptional benefits that employees actually value.
            </p>
            <div class="hero-ctas">
                <a href="#quote" class="primary-btn">Request Your Quote</a>
                <a href="#services" class="secondary-btn">Explore Services</a>
            </div>
        </div>

        <div class="hero-visual">
            <div class="stats-grid">
                <div class="stat-item">
                    <span class="stat-number">35%</span>
                    <span class="stat-label">Average Tax Savings</span>
                </div>
                <div class="stat-item">
                    <span class="stat-number">30+</span>
                    <span class="stat-label">Years in Business</span>
                </div>
                <div class="stat-item">
                    <span class="stat-number">1000+</span>
                    <span class="stat-label">Employers Served</span>
                </div>
                <div class="stat-item">
                    <span class="stat-number">100%</span>
                    <span class="stat-label">Compliance Focus</span>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- Trust Bar -->
<div class="trust-bar">
    <div class="trust-container">
        <p class="trust-text">Trusted By Businesses Since 1996</p>
        <div class="trust-items">
            <div class="trust-item">✓ IRS Compliance</div>
            <div class="trust-item">✓ COBRA Assurance</div>
            <div class="trust-item">✓ HIPAA Compliance</div>
            <div class="trust-item">✓ Trusted Expertise</div>
        </div>
    </div>
</div>

<!-- Services Section -->
<section class="services" id="services">
    <div class="services-container">
        <div class="section-header fade-in">
            <p class="section-eyebrow">Our Solutions</p>
            <h2 class="section-title">Comprehensive Benefits Administration</h2>
            <p class="section-subtitle">
                From tax-advantaged accounts to full benefit administration outsourcing, we handle the complexity so you can focus on your business.
            </p>
        </div>

        <div class="services-grid">
            <div class="service-card fade-in">
                <div class="service-icon">💰</div>
                <h3 class="service-title">Flexible Spending Accounts</h3>
                <p class="service-description">
                    Save 25-35% on healthcare and dependent care expenses through pre-tax benefits. Simple setup, immediate savings for employers and employees.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>

            <div class="service-card fade-in">
                <div class="service-icon">🏥</div>
                <h3 class="service-title">Health Reimbursement Accounts</h3>
                <p class="service-description">
                    Design custom tax-free health benefits that match your needs. Supplement high-deductible plans or cover dental and vision costs.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>

            <div class="service-card fade-in">
                <div class="service-icon">📊</div>
                <h3 class="service-title">Medical Expense Reimbursement</h3>
                <p class="service-description">
                    Take control of healthcare costs without the hassle of self-insurance. Reduce premiums while maintaining excellent coverage.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>

            <div class="service-card fade-in">
                <div class="service-icon">⚖️</div>
                <h3 class="service-title">COBRA Administration</h3>
                <p class="service-description">
                    Don't risk costly compliance mistakes. We handle all legal notifications, communication, and documentation requirements.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>

            <div class="service-card fade-in">
                <div class="service-icon">🏦</div>
                <h3 class="service-title">Health Savings Accounts</h3>
                <p class="service-description">
                    Partner with healthcare experts, not just banks. Ensure your employees' HSA compliance with qualified high-deductible plans.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>

            <div class="service-card fade-in">
                <div class="service-icon">🚌</div>
                <h3 class="service-title">Transit & Parking Accounts</h3>
                <p class="service-description">
                    Provide tax-sheltered commuter benefits for parking and transit expenses. Help employees save on daily work travel costs.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>

            <div class="service-card fade-in">
                <div class="service-icon">💼</div>
                <h3 class="service-title">Billing Services</h3>
                <p class="service-description">
                    Streamline administrative functions for billing and collection services. Lower costs, reduce risk, and improve service delivery.
                </p>
                <a href="#" class="service-link">Learn more</a>
            </div>
        </div>
    </div>
</section>

<!-- CTA Section -->
<section class="cta-section" id="quote">
    <div class="cta-container">
        <h2>Ready to transform your benefits program?</h2>
        <p>Let's show you how much you can save while improving employee satisfaction.</p>
        <div class="cta-buttons">
            <a href="#quote" class="primary-btn white-btn">Get Your Free Quote</a>
            <a href="tel:8008797752" class="primary-btn outline-btn">Call 800-879-7752</a>
        </div>
    </div>
</section>

<!-- Footer -->
<footer>
    <div class="footer-container">
        <div>
            <div class="footer-logo">
                <img src="<%= logoPath %>" alt="Superior State Employer Solutions">
            </div>
            <p class="footer-description">
                Providing comprehensive employee benefits administration and HR solutions to Michigan businesses since 1996.
            </p>
            <p class="footer-description">
                <strong>PO Box 577<br>
                    Menominee, MI 49858</strong>
            </p>
        </div>

        <div>
            <h4 class="footer-title">Services</h4>
            <ul class="footer-links">
                <li><a href="#">FSA Plans</a></li>
                <li><a href="#">HRA Plans</a></li>
                <li><a href="#">HSA Plans</a></li>
                <li><a href="#">COBRA Admin</a></li>
                <li><a href="#">Transit Accounts</a></li>
                <li><a href="#">Billing Services</a></li>
            </ul>
        </div>

        <div id="resources">
            <h4 class="footer-title">Resources</h4>
            <ul class="footer-links">
                <li><a href="http://kb.superiorstate.net">Knowledge Base</a></li>
                <li><a href="/tpo/forms.aspx">Forms Library</a></li>
                <li><a href="/tpo/brochures.aspx">Brochures</a></li>
                <li><a href="/account/register.aspx">Agent Portal</a></li>
            </ul>
        </div>

        <div id="about">
            <h4 class="footer-title">Company</h4>
            <ul class="footer-links">
                <li><a href="#about">About Us</a></li>
                <li><a href="/tpo/contact_us.aspx">Contact</a></li>
                <li><a href="#">Careers</a></li>
                <li><a href="#">Privacy Policy</a></li>
            </ul>
        </div>
    </div>

    <div class="footer-bottom">
        <p>&copy; <%= year %> Superior State Employer Solutions. All rights reserved.</p>
    </div>
</footer>

<script>
    // Scroll animation observer
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, observerOptions);

    document.querySelectorAll('.fade-in').forEach(el => observer.observe(el));

    // Smooth scrolling
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
</script>
</body>
</html>
