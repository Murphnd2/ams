<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="jakarta.persistence.*, java.util.*, java.time.*, java.time.format.DateTimeFormatter" %>
<%
    String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzoFCW5OYxBVQN57HEvv4iv257lTIQUMETbsb684bQ3W9pBaoZWDV-OZF8ug_KuuylA/exec";

    String idParam = request.getParameter("id");
    long assigneeId;
    try {
        assigneeId = Long.parseLong(idParam);
    } catch (Exception e) {
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Invalid Link</title>
    <style>
        body{font-family:Arial;text-align:center;margin-top:100px;color:#555;}
        .box{max-width:600px;margin:0 auto;padding:40px;border:2px solid #ddd;border-radius:10px;}
    </style>
</head>
<body>
<div class="box">
    <h1 style="color:#c33">Invalid Link</h1>
    <p>The test ID is not valid.</p>
</div>
</body>
</html>
<%
        return;
    }

    // Build returnUrl pointing to a servlet under this webapp's context path
    String scheme  = request.getScheme();       // http or https
    String server  = request.getServerName();   // host
    int    port    = request.getServerPort();   // 80, 443, etc.
    String context = request.getContextPath();  // "" or "/beta", "/ams", etc.

    String base = scheme + "://" + server +
            ((port == 80 || port == 443) ? "" : (":" + port)) +
            context;

    String returnUrl = base + "/Eligibility125Complete";

    String businessName = "Unknown Employer";
    String dtype = "";
    boolean isSetup = false;
    LocalDate planStart = null;
    LocalDate planEnd = null;
    Set<String> precheckedBenefits = new HashSet<>();

    String planPeriod = "";
    String planYear = "";
    int hceThreshold = 155000;
    String lookbackYear = "2025";

    EntityManager em = null;

    try {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ssaPU");
        em = emf.createEntityManager();

        @SuppressWarnings("unchecked")
        List<Object[]> main = em.createNativeQuery(
                        "SELECT a.full_name, a.DTYPE FROM assignee a " +
                                "WHERE a.id = ? AND a.DTYPE IN ('Renewal','Setup') AND a.is_complete = 0")
                .setParameter(1, assigneeId)
                .getResultList();

        if (main.isEmpty()) {
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Test Not Available</title>
    <style>
        body{font-family:Arial;text-align:center;margin-top:100px;color:#555;}
        .box{max-width:600px;margin:0 auto;padding:40px;border:2px solid #ddd;border-radius:10px;}
    </style>
</head>
<body>
<div class="box">
    <h1 style="color:#c33">Test Not Available</h1>
    <p>This test (ID <%=assigneeId%>) is either completed, not a Renewal/Setup, or no longer active.</p>
</div>
</body>
</html>
<%
        return;
    }

    Object[] row = main.get(0);
    businessName = (String) row[0];
    if (businessName == null || businessName.trim().isEmpty()) businessName = "Unknown Employer";
    dtype = (String) row[1];
    isSetup = "Setup".equals(dtype);

    if (!isSetup) {
        // Get plan start from renewal items
        @SuppressWarnings("unchecked")
        List<java.sql.Date> renewalData = em.createNativeQuery(
                        "SELECT ri.date_for FROM renewalitem ri " +
                                "JOIN benefit b ON ri.benefit_id = b.benefit_id " +
                                "WHERE ri.renewal_id = ? " +
                                "AND b.plan_type_id IN (1,2,4,5,1001,1005,1007) " +
                                "ORDER BY ri.renewal_item_id ASC LIMIT 1")
                .setParameter(1, assigneeId)
                .getResultList();

        if (!renewalData.isEmpty()) {
            java.sql.Date dateObj = renewalData.get(0);
            if (dateObj != null) {
                planStart = dateObj.toLocalDate();
                planEnd = planStart.plusYears(1).minusDays(1);
            }
        }

        // Get plan types for pre-checked benefits
        @SuppressWarnings("unchecked")
        List<Number> planTypeIds = em.createNativeQuery(
                        "SELECT DISTINCT b.plan_type_id FROM renewalitem ri " +
                                "JOIN benefit b ON ri.benefit_id = b.benefit_id " +
                                "WHERE ri.renewal_id = ? " +
                                "AND b.plan_type_id IN (1,2,4,5,1001,1005,1007)")
                .setParameter(1, assigneeId)
                .getResultList();

        Map<Integer,String> benefitMap = new HashMap<>();
        benefitMap.put(1005, "Medical");
        benefitMap.put(1007, "HSA");
        benefitMap.put(2, "HealthFSA");
        benefitMap.put(1, "DepCare");
        benefitMap.put(5, "LPFSA");
        benefitMap.put(4,"HSA");
        benefitMap.put(1001, "PRA");

        for (Number n : planTypeIds) {
            int ptid = n.intValue();
            String type = benefitMap.getOrDefault(ptid, "");
            if (!type.isEmpty()) precheckedBenefits.add(type);
        }
    }

    if (planStart != null) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
        planPeriod = planStart.format(fmt) + " – " + planEnd.format(fmt);
        planYear = String.valueOf(planStart.getYear());
    }

    // HCE threshold based on plan year (if known)
    if ("2027".equals(planYear)) hceThreshold = 160000;
    else if ("2028".equals(planYear)) hceThreshold = 165000;

    // Lookback year: if planYear blank, default as if 2026
    lookbackYear = String.valueOf(Integer.parseInt(planYear.isEmpty() ? "2026" : planYear) - 1);

} catch (Exception e) {
    e.printStackTrace();
    String debugFlag = request.getParameter("debug");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>System Error</title>
</head>
<body style="font-family:Arial;max-width:900px;margin:30px auto;">
<%
    if ("1".equals(debugFlag)) {
%>
<h1 style="color:#c33;">System Error (Debug)</h1>
<p>The following exception occurred:</p>
<pre>
<%
    e.printStackTrace(new java.io.PrintWriter(out));
%>
</pre>
<%
} else {
%>
<h1 style="color:#c33;">System Error</h1>
<p>Please contact support.</p>
<%
    }
%>
</body>
</html>
<%
        return;
    } finally {
        if (em != null) try { em.close(); } catch (Exception ignored) {}
    }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>125 Eligibility Test #<%=assigneeId%></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
          crossorigin="anonymous">
    <style>
        body{font-family:Arial, sans-serif;max-width:960px;margin:30px auto;padding:20px;}
        .section{background:#f9f9f9;padding:20px;margin:20px 0;border-radius:8px;border:1px solid #ddd;}
        .important{font-weight:bold;color:#c33;}
    </style>
</head>
<body>

<h1 class="mb-4 text-primary">§125 Eligibility Test</h1>

<form action="<%=request.getContextPath()%>/Eligibility125Submit"
      method="POST"
      class="card p-4 shadow-sm mb-5"
      onsubmit="return validateForm()">

    <!-- Hidden identifiers -->
    <input type="hidden" name="guid" value="<%=assigneeId%>">
    <input type="hidden" name="originalname" value="<%=businessName%>">
    <input type="hidden" name="namechanged" id="namechanged" value="No">

    <!-- Basic info -->
    <div class="section">
        <!-- Business name with inline Edit button -->
        <div class="mb-3">
            <label class="form-label">Business Name</label>
            <div class="input-group">
                <input type="text"
                       name="businessname"
                       id="bn"
                       class="form-control"
                       value="<%=businessName%>"
                       readonly>
                <button type="button"
                        class="btn btn-outline-secondary"
                        onclick="document.getElementById('bn').removeAttribute('readonly');document.getElementById('namechanged').value='Yes';document.getElementById('bn').focus();">
                    Edit
                </button>
            </div>
            <small class="form-text text-muted">
                This is the employer name we have on file. Click Edit if you need to correct it.
            </small>
        </div>

        <!-- Contact Email -->
        <div class="mb-3">
            <label class="form-label">Your Email</label>
            <input type="email" name="email" class="form-control" required>
        </div>

        <!-- Tax classification -->
        <div class="mb-3">
            <label class="form-label">Tax Classification of the Business</label>
            <select name="taxclass" class="form-select" required onchange="updateOwnershipBlocks()">
                <option value="">— Select —</option>
                <option value="C-Corp">C-Corp</option>
                <option value="S-Corp">S-Corp</option>
                <option value="LLC taxed as Partnership">LLC taxed as Partnership</option>
                <option value="LLC taxed as C-Corp">LLC taxed as C-Corp</option>
                <option value="Non-Profit">Non-Profit</option>
                <option value="Government">Government</option>
                <option value="Partnership">Partnership</option>
                <option value="Sole Proprietor">Sole Proprietor</option>
            </select>
        </div>

        <!-- Plan year -->
        <div class="mb-3">
            <label class="form-label">Plan Year</label>
            <% if (!isSetup && planStart != null) { %>
            <input type="text" class="form-control mb-2" value="<%=planPeriod%>" readonly>
            <input type="hidden" name="planyear" value="<%=planYear%>">
            <input type="hidden" name="planend"
                   value="<%=planEnd != null ? planEnd.toString() : ""%>">
            <small class="form-text text-muted">
                This is the upcoming plan year period for testing.
            </small>
            <% } else { %>
            <label class="form-label">Upcoming Plan Year Start</label>
            <input type="date" class="form-control mb-2" id="planstartInput" name="planstart" required>
            <input type="hidden" name="planend" id="planend">
            <small class="form-text text-muted">
                Select the first day of the new plan year. The plan year end date will be derived automatically.
            </small>
            <% } %>
        </div>
    </div>

    <!-- Controlled-Group Questions -->
    <div class="section">
        <h5>Controlled Group / Affiliated Service Group</h5>
        <%
            String[] cgQuestions = {
                    "Is this business a subsidiary of another business?",
                    "Does the owner(s) of this business own, or have a significant interest in another company?",
                    "Does this business utilize leased employees who have performed services for you on a substantially full-time basis for at least one year?",
                    "Does this business provide services solely to one or just a few customers?",
                    "Do you purchase services from an entity that provides such services solely to your business, or to a very limited number of businesses?"
            };
            for (int i = 1; i <= 5; i++) {
        %>
        <div class="mb-3">
            <label class="form-label"><%= cgQuestions[i-1] %></label>
            <select name="cg<%=i%>" class="form-select" onchange="checkAffiliated()" required>
                <option value="">— Select —</option>
                <option value="Yes">Yes</option>
                <option value="No">No</option>
            </select>
        </div>
        <% } %>
        <input type="hidden" name="affiliatedany" id="affiliatedany" value="No">
    </div>

    <!-- Affiliated Detail (always shown when any CG answer is Yes) -->
    <div id="affiliatedBlock" class="section d-none">
        <h5>Affiliated Entities</h5>

        <div class="mb-3">
            <label class="form-label">
                Are any employees of the affiliated entities covered by THIS Section 125 plan?
            </label>
            <select name="affiliatedcovered" class="form-select">
                <option value="No">No</option>
                <option value="Yes">Yes</option>
            </select>
        </div>

        <div id="affDetail">
            <div class="mb-3">
                <label class="form-label">Total W-2 employees at affiliated entities (not covered):</label>
                <input type="number" name="afftotalw2" class="form-control" min="0" value="0">
                <div class="form-text">
                    Only employees who receive a W-2 paycheck – do NOT include pure K-1 owners.
                </div>
            </div>

            <div class="mb-3">
                <label class="form-label">
                    Of those, how many had <%=lookbackYear%> compensation &gt; $<%=String.format("%,d", hceThreshold)%>?
                </label>
                <input type="number" name="affhighearners" class="form-control" min="0" value="0">
            </div>

            <div class="mb-3">
                <label class="form-label">
                    Of those, how many are &gt;5% owners, officers, or their family
                    (even if paid less than $<%=String.format("%,d", hceThreshold)%>)?
                </label>
                <input type="number" name="affownershiphce" class="form-control" min="0" value="0">
            </div>
        </div>
    </div>

    <!-- Headcount & Eligibility -->
    <div class="section">
        <h5>Headcount & Eligibility</h5>
        <p class="important">
            IMPORTANT – NO DOUBLE-COUNTING<br>
            Answer each question using only employees NOT already counted in a previous question.
        </p>

        <div class="mb-3">
            <label class="form-label">
                Total W-2 employees at the entity(ies) actually covered by this plan:
            </label>
            <input type="number" name="totalw2" id="totalw2"
                   class="form-control" min="1" required
                   onchange="calcRemaining()">
        </div>

        <!-- S-Corp >2% owner block -->
        <div id="partnershipBlock" class="mb-3 d-none">
            <label class="form-label">
                W-2 employees who are &gt;2% S-Corp shareholders
            </label>
            <input type="number" name="partowners"
                   class="form-control mb-2" min="0" value="0"
                   onchange="calcRemaining()">
            <label class="form-label">
                Of those &gt;2% S-Corp shareholders, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
                <small>(Most employers: all of them.)</small>
            </label>
            <input type="number" name="partownersineligible"
                   class="form-control mb-3" min="0" value="0">

            <label class="form-label">
                W-2 employees who are the spouse, parent, or child of a &gt;2% S-Corp shareholder
            </label>
            <input type="number" name="partfamily"
                   class="form-control mb-2" min="0" value="0"
                   onchange="calcRemaining()">
            <label class="form-label">
                Of those family members, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
                <small>(Most employers: all of them.)</small>
            </label>
            <input type="number" name="partfamilyineligible"
                   class="form-control" min="0" value="0">
        </div>

        <!-- C-Corp / LLC (C-Corp) >5% owner block -->
        <div id="corpBlock" class="mb-3 d-none">
            <label class="form-label">
                W-2 employees who own more than 5% of the company
                (C-Corp or LLC taxed as C-Corp)
            </label>
            <input type="number" name="corpofficers"
                   class="form-control mb-2" min="0" value="0"
                   onchange="calcRemaining()">
            <label class="form-label">
                Of those &gt;5% owners, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
                <small>(Most employers: usually 0.)</small>
            </label>
            <input type="number" name="corpofficersineligible"
                   class="form-control mb-3" min="0" value="0">

            <label class="form-label">
                W-2 employees who are the spouse, parent, or child of a &gt;5% owner
            </label>
            <input type="number" name="corpfamily"
                   class="form-control mb-2" min="0" value="0"
                   onchange="calcRemaining()">
            <label class="form-label">
                Of those family members, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
            </label>
            <input type="number" name="corpfamilyineligible"
                   class="form-control" min="0" value="0">
        </div>

        <!-- LLC taxed as Partnership / General Partnership family of >5% partners -->
        <div id="partnerFamilyBlock" class="mb-3 d-none">
            <label class="form-label">
                W-2 employees who are the spouse, parent, or child of a partner
                who owns more than 5% of the business
            </label>
            <input type="number" name="partnerfamily"
                   class="form-control mb-2" min="0" value="0"
                   onchange="calcRemaining()">
            <label class="form-label">
                Of those family members, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
            </label>
            <input type="number" name="partnerfamilyineligible"
                   class="form-control" min="0" value="0">
        </div>

        <div class="mb-3">
            <label class="form-label">
                Employees who make &gt; $<%=String.format("%,d", hceThreshold)%>
                (total including bonuses) per Year<br>
                <small>(do NOT count anyone above):</small>
            </label>
            <input type="number" name="highearners"
                   class="form-control mb-2" min="0" value="0"
                   onchange="calcRemaining()">
            <label class="form-label">
                Of those high earners, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
            </label>
            <input type="number" name="highearnersineligible"
                   class="form-control" min="0" value="0">
        </div>

        <div class="mb-3">
            <label class="form-label">
                Remaining Employees not included above (auto-calculated):
            </label>
            <input type="text" id="remaining" name="remaining"
                   class="form-control mb-2" readonly style="background:#eee;">
            <label class="form-label">
                Of those remaining employees, how many are NOT allowed to participate
                in this Section 125 plan under your rules?
            </label>
            <input type="number" name="remainingineligible"
                   class="form-control" min="0" value="0">
        </div>
    </div>

    <!-- Benefits Offered -->
    <div class="section">
        <h5>Benefits Offered Under the Plan</h5>
        <p>Select any/all pre-tax benefits, or explain changes below.</p>

        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="benefits" value="Medical"
                <%=precheckedBenefits.contains("Medical") ? "checked" : ""%>>
            <label class="form-check-label">Medical Insurance Premiums</label>
        </div>
        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="benefits" value="Dental"
                <%=precheckedBenefits.contains("Dental") ? "checked" : ""%>>
            <label class="form-check-label">Dental Insurance Premiums</label>
        </div>
        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="benefits" value="Vision"
                <%=precheckedBenefits.contains("Vision") ? "checked" : ""%>>
            <label class="form-check-label">Vision Insurance Premiums</label>
        </div>
        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="benefits" value="HSA"
                <%=precheckedBenefits.contains("HSA") ? "checked" : ""%>>
            <label class="form-check-label">Health Savings Account Deposits</label>
        </div>
        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="benefits" value="HealthFSA"
                <%=precheckedBenefits.contains("HealthFSA") ? "checked" : ""%>>
            <label class="form-check-label">Health FSA Contributions</label>
        </div>
        <div class="form-check mb-3">
            <input class="form-check-input" type="checkbox" name="benefits" value="DepCare"
                <%=precheckedBenefits.contains("DepCare") ? "checked" : ""%>>
            <label class="form-check-label">Dependent Care Contributions</label>
        </div>
        <div class="form-check mb-3">
            <input class="form-check-input" type="checkbox" name="benefits" value="LPFSA"
                <%=precheckedBenefits.contains("LPFSA") ? "checked" : ""%>>
            <label class="form-check-label">Limited Purpose FSA Contributions</label>
        </div>
        <div class="form-check mb-3">
            <input class="form-check-input" type="checkbox" name="benefits" value="PRA"
                <%=precheckedBenefits.contains("PRA") ? "checked" : ""%>>
            <label class="form-check-label">Premium Reimbursement Account</label>
        </div>

        <div class="mb-3">
            <label class="form-label">Plan or Benefit Offering Changes since Last Year</label>
            <textarea name="benefitchanges" class="form-control" rows="3"></textarea>
            <div class="form-text">
                If you do not check any benefits above, please describe the plan or expected offerings here.
            </div>
        </div>
    </div>

    <!-- Uniform Benefit Safe-Harbor -->
    <div class="section">
        <h5>Uniform Benefit / Safe Harbor Questions</h5>

        <div class="mb-3">
            <label class="form-label">
                Are employer contributions to the plan IDENTICAL (at each benefit tier) for ALL EMPLOYEES?
            </label>
            <select name="uniform1" class="form-select" onchange="checkUniform()" required>
                <option value="">— Select —</option>
                <option value="Yes">Yes</option>
                <option value="No">No</option>
            </select>
        </div>

        <div class="mb-3">
            <label class="form-label">
                Are benefits UNIFORM regardless of age, years of service, or compensation?
            </label>
            <select name="uniform2" class="form-select" onchange="checkUniform()" required>
                <option value="">— Select —</option>
                <option value="Yes">Yes</option>
                <option value="No">No</option>
            </select>
        </div>

        <div class="mb-3">
            <label class="form-label">
                Are the SAME BENEFITS offered to all eligible employees?
            </label>
            <select name="uniform3" class="form-select" onchange="checkUniform()" required>
                <option value="">— Select —</option>
                <option value="Yes">Yes</option>
                <option value="No">No</option>
            </select>
        </div>

        <div class="mb-3">
            <label class="form-label">
                Are WAITING PERIODS IDENTICAL for all groups of employees?
            </label>
            <select name="uniform4" class="form-select" onchange="checkUniform()" required>
                <option value="">— Select —</option>
                <option value="Yes">Yes</option>
                <option value="No">No</option>
            </select>
        </div>

        <div id="uniformExplain" class="mb-3 d-none">
            <label class="form-label">
                You answered NO to at least one question above. Please describe why benefits vary and exactly how.
            </label>
            <textarea name="uniformexplain" class="form-control" rows="4"></textarea>
        </div>

        <div id="participationBlock" class="mb-3 d-none">
            <label class="form-label">
                Of the officers/key managers and high earners who are eligible, how many will actually participate?
            </label>
            <input type="number" name="officersparticipating" class="form-control" min="0" value="0">
        </div>
    </div>

    <div class="text-center">
        <button type="submit" class="btn btn-primary btn-lg mt-3">
            Submit Eligibility Test
        </button>
    </div>
</form>

<script>
    function checkAffiliated() {
        const anyYes = Array.from(document.querySelectorAll("[name^='cg']")).some(s => s.value === "Yes");
        document.getElementById("affiliatedany").value = anyYes ? "Yes" : "No";
        document.getElementById("affiliatedBlock").classList.toggle("d-none", !anyYes);
    }

    function updatePlanEndFromStart() {
        const startInput = document.getElementById("planstartInput");
        const endInput = document.getElementById("planend");
        if (!startInput || !endInput || !startInput.value) return;

        const parts = startInput.value.split("-");
        if (parts.length !== 3) return;

        const year  = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1; // JS month 0–11
        const day   = parseInt(parts[2], 10);

        const startDate = new Date(year, month, day);
        const endDate = new Date(startDate);
        endDate.setFullYear(endDate.getFullYear() + 1);
        endDate.setDate(endDate.getDate() - 1);

        const yyyy = endDate.getFullYear();
        const mm   = String(endDate.getMonth() + 1).padStart(2, "0");
        const dd   = String(endDate.getDate()).padStart(2, "0");

        endInput.value = `${yyyy}-${mm}-${dd}`;
    }

    function calcRemaining() {
        const totalField = document.getElementById("totalw2");
        const remainingField = document.getElementById("remaining");
        if (!totalField || !remainingField) return;

        const total = parseInt(totalField.value || "0", 10);

        function getInt(selector) {
            const el = document.querySelector(selector);
            if (!el) return 0;
            const v = parseInt(el.value || "0", 10);
            return isNaN(v) ? 0 : v;
        }

        // S-Corp >2% owners & family
        const partOwners  = getInt("[name='partowners']");
        const partFamily  = getInt("[name='partfamily']");

        // C-Corp / LLC (C-Corp) >5% owners & family
        const corpOwners = getInt("[name='corpofficers']");
        const corpFamily = getInt("[name='corpfamily']");

        // LLC taxed as Partnership / Partnership family of >5% partners
        const partnerFamily = getInt("[name='partnerfamily']");

        // High earners
        const highEarners  = getInt("[name='highearners']");

        let rem = total
            - partOwners
            - partFamily
            - corpOwners
            - corpFamily
            - partnerFamily
            - highEarners;

        if (rem < 0) rem = 0;
        remainingField.value = rem;
    }

    function updateOwnershipBlocks() {
        const taxSelect = document.querySelector("[name='taxclass']");
        const corpBlock = document.getElementById("corpBlock");
        const partnershipBlock = document.getElementById("partnershipBlock"); // S-Corp >2% owners
        const partnerFamilyBlock = document.getElementById("partnerFamilyBlock"); // LLC P/Partnership family
        if (!taxSelect || !corpBlock || !partnershipBlock || !partnerFamilyBlock) return;

        const v = taxSelect.value;

        function resetNumericInputs(block) {
            if (!block) return;
            const nums = block.querySelectorAll("input[type='number']");
            nums.forEach(el => { el.value = "0"; });
        }

        // Hide everything and reset
        resetNumericInputs(corpBlock);
        resetNumericInputs(partnershipBlock);
        resetNumericInputs(partnerFamilyBlock);
        corpBlock.classList.add("d-none");
        partnershipBlock.classList.add("d-none");
        partnerFamilyBlock.classList.add("d-none");

        if (!v) {
            calcRemaining();
            return;
        }

        if (v === "S-Corp") {
            // S-Corp → >2% shareholders & family
            partnershipBlock.classList.remove("d-none");
        } else if (v === "C-Corp" || v === "LLC taxed as C-Corp") {
            // C-Corp / LLC taxed as C-Corp → >5% owners & family
            corpBlock.classList.remove("d-none");
        } else if (v === "LLC taxed as Partnership" || v === "Partnership") {
            // LLC taxed as Partnership / General Partnership → family of >5% partners
            partnerFamilyBlock.classList.remove("d-none");
        } else {
            // Sole Proprietor, Non-Profit, Government → no ownership/family blocks
        }

        calcRemaining();
    }

    function checkUniform() {
        const anyNo = ["uniform1","uniform2","uniform3","uniform4"]
            .some(n => document.querySelector("[name='"+n+"']").value === "No");

        document.getElementById("uniformExplain").classList.toggle("d-none", !anyNo);
        document.getElementById("participationBlock").classList.toggle("d-none", !anyNo);
    }

    function validateForm() {
        // Ensure plan year start is selected when applicable
        const planstartInput = document.getElementById("planstartInput");
        if (planstartInput) {
            if (!planstartInput.value) {
                alert("Please select the plan year start date.");
                planstartInput.focus();
                return false;
            }
            updatePlanEndFromStart();
            const planendInput = document.getElementById("planend");
            if (!planendInput || !planendInput.value) {
                alert("There was an issue deriving the plan year end from the start date. Please re-select the plan year start.");
                planstartInput.focus();
                return false;
            }
        }

        function getIntByName(name) {
            const el = document.querySelector("[name='"+name+"']");
            if (!el) return 0;
            const v = parseInt(el.value || "0", 10);
            return isNaN(v) ? 0 : v;
        }

        // --- Affiliated entity validations ---
        const affiliatedBlock = document.getElementById("affiliatedBlock");
        if (affiliatedBlock && !affiliatedBlock.classList.contains("d-none")) {
            const totalAff = getIntByName("afftotalw2");
            const affHigh = getIntByName("affhighearners");
            const affOwner = getIntByName("affownershiphce");

            if (affHigh > totalAff) {
                alert("In the affiliated entity section, the number of employees with compensation above the threshold cannot exceed the total W-2 employees at affiliated entities.");
                const fld = document.querySelector("[name='affhighearners']");
                if (fld) fld.focus();
                return false;
            }

            if (affOwner > (totalAff - affHigh)) {
                alert("In the affiliated entity section, the number of >5% owners/officers/family cannot exceed the remaining employees after those with compensation above the threshold.");
                const fld = document.querySelector("[name='affownershiphce']");
                if (fld) fld.focus();
                return false;
            }
        }

        // --- Headcount & eligibility validations ---
        const totalW2Field = document.getElementById("totalw2");
        if (totalW2Field) {
            const totalW2 = parseInt(totalW2Field.value || "0", 10);
            const highEarners = getIntByName("highearners");
            const highEarnersIn = getIntByName("highearnersineligible");

            const partnershipBlock = document.getElementById("partnershipBlock");
            const corpBlock = document.getElementById("corpBlock");
            const partnerFamilyBlock = document.getElementById("partnerFamilyBlock");

            const partOwners = getIntByName("partowners");
            const partOwnersIn = getIntByName("partownersineligible");
            const partFamily = getIntByName("partfamily");
            const partFamilyIn = getIntByName("partfamilyineligible");

            const corpOwners = getIntByName("corpofficers");
            const corpOwnersIn = getIntByName("corpofficersineligible");
            const corpFamily = getIntByName("corpfamily");
            const corpFamilyIn = getIntByName("corpfamilyineligible");

            const partnerFamily = getIntByName("partnerfamily");
            const partnerFamilyIn = getIntByName("partnerfamilyineligible");

            if (highEarners > totalW2) {
                alert("Employees who make more than the HCE threshold cannot exceed the total W-2 employees covered by this plan.");
                const fld = document.querySelector("[name='highearners']");
                if (fld) fld.focus();
                return false;
            }

            if (highEarnersIn > highEarners) {
                alert("The number of ineligible high earners cannot exceed the number of high earners.");
                const fld = document.querySelector("[name='highearnersineligible']");
                if (fld) fld.focus();
                return false;
            }

            if (partnershipBlock && !partnershipBlock.classList.contains("d-none")) {
                if (partOwnersIn > partOwners) {
                    alert("In the S-Corp section, ineligible >2% shareholders cannot exceed the total >2% shareholders.");
                    const fld = document.querySelector("[name='partownersineligible']");
                    if (fld) fld.focus();
                    return false;
                }
                if (partFamilyIn > partFamily) {
                    alert("In the S-Corp section, ineligible spouses/children of >2% shareholders cannot exceed the total spouses/children of >2% shareholders.");
                    const fld = document.querySelector("[name='partfamilyineligible']");
                    if (fld) fld.focus();
                    return false;
                }
                if ((partOwners + partFamily) > totalW2) {
                    alert("In the S-Corp section, the combined count of >2% shareholders and their spouses/children cannot exceed the total W-2 employees covered by this plan.");
                    const fld = document.querySelector("[name='partowners']");
                    if (fld) fld.focus();
                    return false;
                }
            }

            if (corpBlock && !corpBlock.classList.contains("d-none")) {
                if (corpOwnersIn > corpOwners) {
                    alert("In the >5% owner section, ineligible >5% owners cannot exceed the total >5% owners.");
                    const fld = document.querySelector("[name='corpofficersineligible']");
                    if (fld) fld.focus();
                    return false;
                }
                if (corpFamilyIn > corpFamily) {
                    alert("In the >5% owner section, ineligible spouses/children of >5% owners cannot exceed the total spouses/children of >5% owners.");
                    const fld = document.querySelector("[name='corpfamilyineligible']");
                    if (fld) fld.focus();
                    return false;
                }
                if ((corpOwners + corpFamily) > totalW2) {
                    alert("In the >5% owner section, the combined count of >5% owners and their spouses/children cannot exceed the total W-2 employees covered by this plan.");
                    const fld = document.querySelector("[name='corpofficers']");
                    if (fld) fld.focus();
                    return false;
                }
            }

            if (partnerFamilyBlock && !partnerFamilyBlock.classList.contains("d-none")) {
                if (partnerFamilyIn > partnerFamily) {
                    alert("In the partnership section, ineligible family of >5% partners cannot exceed the total family of >5% partners.");
                    const fld = document.querySelector("[name='partnerfamilyineligible']");
                    if (fld) fld.focus();
                    return false;
                }
                if (partnerFamily > totalW2) {
                    alert("The number of family members of >5% partners cannot exceed the total W-2 employees covered by this plan.");
                    const fld = document.querySelector("[name='partnerfamily']");
                    if (fld) fld.focus();
                    return false;
                }
            }

            let countedOwners = 0;
            if (partnershipBlock && !partnershipBlock.classList.contains("d-none")) {
                countedOwners += partOwners + partFamily;
            }
            if (corpBlock && !corpBlock.classList.contains("d-none")) {
                countedOwners += corpOwners + corpFamily;
            }
            if (partnerFamilyBlock && !partnerFamilyBlock.classList.contains("d-none")) {
                countedOwners += partnerFamily;
            }
            const combinedKeyGroup = countedOwners + highEarners;
            if (combinedKeyGroup > totalW2) {
                alert("The combined count of >2% S-Corp shareholders, their family, >5% owners, their family, family of >5% partners, and high earners cannot exceed the total W-2 employees covered by this plan.");
                totalW2Field.focus();
                return false;
            }

            const remainingField = document.getElementById("remaining");
            let remaining = 0;
            if (remainingField) {
                remaining = parseInt(remainingField.value || "0", 10);
            }
            const remainingIn = getIntByName("remainingineligible");

            if (remainingIn > remaining) {
                alert("The number of ineligible remaining employees cannot exceed the auto-calculated remaining employee count.");
                const fld = document.querySelector("[name='remainingineligible']");
                if (fld) fld.focus();
                return false;
            }
        }

        // Benefits offered: require at least one checkbox OR explanatory text
        const benefitChecks = document.querySelectorAll("input[name='benefits']:checked");
        const benefitNotesEl = document.querySelector("[name='benefitchanges']");
        const benefitNotes = (benefitNotesEl && benefitNotesEl.value) ? benefitNotesEl.value.trim() : "";

        if (benefitChecks.length === 0 && benefitNotes === "") {
            alert("Please either check at least one benefit that will be pre-taxed through this plan, or describe the plan/benefit offering changes in the text box.");
            const firstCheck = document.querySelector("input[name='benefits']");
            if (firstCheck) firstCheck.focus();
            return false;
        }

        return true;
    }

    // Wire up initial behaviors
    const planstartInputInit = document.getElementById("planstartInput");
    if (planstartInputInit) {
        planstartInputInit.addEventListener("change", updatePlanEndFromStart);
    }

    checkAffiliated();
    updateOwnershipBlocks();
</script>

</body>
</html>


