<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="jakarta.persistence.*, java.util.*, java.time.*, java.time.format.DateTimeFormatter" %>
<%
    String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzoFCW5OYxBVQN57HEvv4iv257lTIQUMETbsb684bQ3W9pBaoZWDV-OZF8ug_KuuylA/exec";

    String idParam = request.getParameter("id");
    long assigneeId;
    try { assigneeId = Long.parseLong(idParam); }
    catch (Exception e) {
%>
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Invalid Link</title>
<style>body{font-family:Arial;text-align:center;margin-top:100px;color:#555;}
.box{max-width:600px;margin:0 auto;padding:40px;border:2px solid #ddd;border-radius:10px;}</style>
</head><body><div class="box"><h1 style="color:#c33">Invalid Link</h1>
<p>The test ID is not valid.</p></div></body></html>
<%
        return;
    }

    EntityManager em = null;
    String businessName = "Unknown Employer";
    String dtype = "";
    boolean isSetup = false;
    LocalDate planStart = null;
    LocalDate planEnd = null;
    Set<String> precheckedBenefits = new HashSet<>();

    try {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ssaPU");
        em = emf.createEntityManager();

        @SuppressWarnings("unchecked")
        List<Object[]> main = em.createNativeQuery(
                        "SELECT a.full_name, a.DTYPE FROM assignee a WHERE a.id = ? AND a.DTYPE IN ('Renewal','Setup') AND a.is_complete = 0")
                .setParameter(1, assigneeId)
                .getResultList();

        if (main.isEmpty()) {
%>
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Test Not Available</title>
<style>body{font-family:Arial;text-align:center;margin-top:100px;color:#555;}
.box{max-width:600px;margin:0 auto;padding:40px;border:2px solid #ddd;border-radius:10px;}</style>
</head><body><div class="box"><h1 style="color:#c33">Test Not Available</h1>
<p>This test (ID <%=assigneeId%>) is either completed, not a Renewal/Setup, or no longer active.</p></div></body></html>
<%
        return;
    }

    Object[] row = main.get(0);
    businessName = (String) row[0];
    if (businessName == null || businessName.trim().isEmpty()) businessName = "Unknown Employer";
    dtype = (String) row[1];
    isSetup = "Setup".equals(dtype);

    if (!isSetup) {
        @SuppressWarnings("unchecked")
        List<Object[]> renewalData = em.createNativeQuery(
                        "SELECT ri.date_for FROM renewalitem ri JOIN benefit b ON ri.benefit_id = b.benefit_id WHERE ri.renewal_id = ? AND b.plan_type_id IN (1,2,4,5,1001,1005,1007) ORDER BY ri.renewal_item_id ASC LIMIT 1")
                .setParameter(1, assigneeId)
                .getResultList();

        if (!renewalData.isEmpty()) {
            planStart = ((java.sql.Date) renewalData.get(0)[0]).toLocalDate();
            planEnd = planStart.plusYears(1).minusDays(1);
        }

        @SuppressWarnings("unchecked")
        List<Integer> planTypeIds = em.createNativeQuery(
                        "SELECT DISTINCT b.plan_type_id FROM renewalitem ri JOIN benefit b ON ri.benefit_id = b.benefit_id WHERE ri.renewal_id = ? AND b.plan_type_id IN (1,2,4,5,1001,1005,1007)")
                .setParameter(1, assigneeId)
                .getResultList();

        Map<Integer,String> benefitMap = new HashMap<>() {{
            put(1, "Medical"); put(2, "Dental"); put(4, "Vision");
            put(5, "HSA"); put(1001, "HealthFSA"); put(1005, "DepCare"); put(1007, "Other");
        }};

        for (Integer ptid : planTypeIds) {
            String type = benefitMap.getOrDefault(ptid, "");
            if (!type.isEmpty()) precheckedBenefits.add(type);
        }
    }

} catch (Exception e) {
    e.printStackTrace();
%>
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>System Error</title></head>
<body style="text-align:center;margin-top:100px"><h1 style="color:#c33">System Error</h1>
<p>Please contact support.</p></body></html>
<%
        return;
    } finally {
        if (em != null) try { em.close(); } catch (Exception ignored) {}
    }

    String planPeriod = "";
    if (planStart != null) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
        planPeriod = planStart.format(fmt) + " – " + planEnd.format(fmt);
    }
    String planYear = planStart != null ? String.valueOf(planStart.getYear()) : "";

    int hceThreshold = 155000;
    if ("2027".equals(planYear)) hceThreshold = 160000;
    else if ("2028".equals(planYear)) hceThreshold = 165000;

    String lookbackYear = String.valueOf(Integer.parseInt(planYear.isEmpty()?"2026":planYear)-1);
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>125 Eligibility Test #<%=assigneeId%></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body{font-family:Arial;max-width:900px;margin:30px auto;padding:20px;line-height:1.5;}
        h1,h2{color:#0066cc;}
        .section{background:#f9f9f9;padding:20px;margin:20px 0;border-radius:8px;border:1px solid #ddd;}
        label{display:block;margin:15px 0 5px;font-weight:bold;}
        input,select,textarea{width:100%;padding:8px;box-sizing:border-box;}
        .hidden{display:none;}
        button{background:#0066cc;color:white;padding:15px 30px;font-size:18px;border:none;border-radius:5px;cursor:pointer;}
        button:hover{background:#0052a3;}
        .important{font-weight:bold;color:#c33;}
    </style>
</head>
<body>

<h1>125 Eligibility Test</h1>

<form action="<%=GOOGLE_SCRIPT_URL%>" method="POST">
    <input type="hidden" name="assignee_id" value="<%=assigneeId%>">
    <input type="hidden" name="originalname" value="<%=businessName%>">
    <input type="hidden" name="namechanged" id="namechanged" value="No">

    <div class="section">
        <label>Business Name <button type="button" onclick="document.getElementById('bn').removeAttribute('readonly');document.getElementById('namechanged').value='Yes'">Edit</button></label>
        <input type="text" name="businessname" id="bn" value="<%=businessName%>" readonly>

        <label>Your Email</label>
        <input type="email" name="email" required>

        <% if (isSetup) { %>
        <label>For the Upcoming Plan Year Ending</label>
        <input type="date" name="planend" required>
        <% } else { %>
        <label>For the Upcoming Plan Year Ending</label>
        <input type="text" value="<%=planPeriod%>" readonly>
        <input type="hidden" name="planyear" value="<%=planYear%>">
        <% } %>
    </div>

    <!-- ALL LOCKED-IN SECTIONS BELOW -->

    <!-- 5 Controlled-Group Questions -->
    <div class="section">
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
        <label><%= cgQuestions[i-1] %></label>
        <select name="cg<%=i%>" onchange="checkAffiliated()">
            <option value="">— Select —</option>
            <option value="Yes">Yes</option>
            <option value="No">No</option>
        </select>
        <% } %>
        <input type="hidden" name="affiliatedany" id="affiliatedany" value="No">
    </div>

    <!-- Affiliated Detail Below -->
    <div id="affiliatedBlock" class="section hidden">
        <label>You indicated one or more affiliations above.<br>Are any employees of the affiliated entities covered by THIS Section 125 plan?</label>
        <select name="affiliatedcovered" onchange="toggleAffDetail()">
            <option value="No">No</option>
            <option value="Yes">Yes</option>
        </select>

        <div id="affDetail" class="hidden">
            <label>Total W-2 employees at affiliated entities (not covered):</label>
            <input type="number" name="afftotalw2" min="0" value="0">
            <p class="note">(Only employees who receive a W-2 paycheck – do NOT include pure K-1 owners)</p>

            <label>Of those, how many had <%=lookbackYear%> compensation > $<%=String.format("%,d", hceThreshold)%>?</label>
            <input type="number" name="affhighearners" min="0" value="0">

            <label>Of those, how many are >5% owners, officers, or their family (even if paid less than $<%=String.format("%,d", hceThreshold)%>)?</label>
            <input type="number" name="affownershiphce" min="0" value="0">
        </div>
    </div>

    <!-- Headcount & Eligibility -->
    <div class="section">
        <p class="important">IMPORTANT – NO DOUBLE-COUNTING<br>
            Answer each question using only employees NOT already counted in a previous question.</p>

        <label>Total W-2 employees at the entity(ies) actually covered by this plan:</label>
        <input type="number" name="totalw2" id="totalw2" min="1" required onchange="calcRemaining()">

        <div id="partnershipBlock" class="hidden">
            <label>Employees who are >2% Owners</label>
            <input type="number" name="partowners" min="0" value="0" onchange="calcRemaining()">
            <label>(of those any INELIGIBLE?)</label>
            <input type="number" name="partownersineligible" min="0" value="0">

            <label>Employees who are Spouses or Children of >2% Owners</label>
            <input type="number" name="partfamily" min="0" value="0" onchange="calcRemaining()">
            <label>(of those any INELIGIBLE?)</label>
            <input type="number" name="partfamilyineligible" min="0" value="0">
        </div>

        <div id="corpBlock" class="hidden">
            <label>Employees who are Officers of the Company</label>
            <input type="number" name="corpofficers" min="0" value="0" onchange="calcRemaining()">
            <label>(of those any INELIGIBLE?)</label>
            <input type="number" name="corpofficersineligible" min="0" value="0">

            <label>Employees who are Spouses or Children of Officers of the Company</label>
            <input type="number" name="corpfamily" min="0" value="0" onchange="calcRemaining()">
            <label>(of those any INELIGIBLE?)</label>
            <input type="number" name="corpfamilyineligible" min="0" value="0">
        </div>

        <label>Employees who make > $<%=String.format("%,d", hceThreshold)%> (total including bonuses) per Year<br>(do NOT count anyone above):</label>
        <input type="number" name="highearners" min="0" value="0" onchange="calcRemaining()">
        <label>(of those any INELIGIBLE?)</label>
        <input type="number" name="highearnersineligible" min="0" value="0">

        <label>Remaining Employees not included above (auto-calculated):</label>
        <input type="text" id="remaining" readonly style="background:#eee;">
        <label>(of those any INELIGIBLE?)</label>
        <input type="number" name="remainingineligible" min="0" value="0">
    </div>

    <!-- Benefits Offered -->
    <div class="section">
        <label>Check ANY/ALL Benefits that are/will be pre-taxed through this plan.</label>
        <label><input type="checkbox" name="benefits" value="Medical" <%=precheckedBenefits.contains("Medical")?"checked":""%>> Medical Insurance Premiums</label>
        <label><input type="checkbox" name="benefits" value="Dental" <%=precheckedBenefits.contains("Dental")?"checked":""%>> Dental Insurance Premiums</label>
        <label><input type="checkbox" name="benefits" value="Vision" <%=precheckedBenefits.contains("Vision")?"checked":""%>> Vision Insurance Premiums</label>
        <label><input type="checkbox" name="benefits" value="HSA" <%=precheckedBenefits.contains("HSA")?"checked":""%>> Health Savings Account Deposits</label>
        <label><input type="checkbox" name="benefits" value="HealthFSA" <%=precheckedBenefits.contains("HealthFSA")?"checked":""%>> Health FSA Contributions</label>
        <label><input type="checkbox" name="benefits" value="DepCare" <%=precheckedBenefits.contains("DepCare")?"checked":""%>> Dependent Care Contributions</label>

        <label>Plan or Benefit Offering Changes since Last Year</label>
        <textarea name="benefitchanges" rows="3"></textarea>
    </div>

    <!-- Uniform Benefit Safe-Harbor -->
    <div class="section">
        <label>Are employer contributions to the plan IDENTICAL (at each benefit tier) for ALL EMPLOYEES?</label>
        <select name="uniform1" onchange="checkUniform()"><option>Yes</option><option>No</option></select>

        <label>Are benefits UNIFORM regardless of age, years of service, or compensation?</label>
        <select name="uniform2" onchange="checkUniform()"><option>Yes</option><option>No</option></select>

        <label>Are the SAME BENEFITS offered to all eligible employees?</label>
        <select name="uniform3" onchange="checkUniform()"><option>Yes</option><option>No</option></select>

        <label>Are WAITING PERIODS IDENTICAL for all groups of employees?</label>
        <select name="uniform4" onchange="checkUniform()"><option>Yes</option><option>No</option></select>

        <div id="uniformExplain" class="hidden">
            <label>You answered NO to at least one question above. Please describe why benefits vary in this way and exactly how they vary in the space below.</label>
            <textarea name="uniformexplain" rows="4"></textarea>
        </div>

        <div id="participationBlock" class="hidden">
            <label>Of the officers/key managers and high earners who are eligible, how many will actually participate?</label>
            <input type="number" name="officersparticipating" min="0">
        </div>
    </div>

    <button type="submit">Submit Eligibility Test</button>
</form>

<script>
    function checkAffiliated() {
        const anyYes = Array.from(document.querySelectorAll("[name^='cg']")).some(s => s.value === "Yes");
        document.getElementById("affiliatedany").value = anyYes ? "Yes" : "No";
        document.getElementById("affiliatedBlock").classList.toggle("hidden", !anyYes);
    }
    function toggleAffDetail() {
        const cov = document.querySelector("[name='affiliatedcovered']").value;
        document.getElementById("affDetail").classList.toggle("hidden", cov === "Yes");
    }
    function calcRemaining() {
        const total = parseInt(document.getElementById("totalw2").value) || 0;
        const part = parseInt(document.querySelector("[name='partowners']")?.value || 0);
        const fam = parseInt(document.querySelector("[name='partfamily']")?.value || 0);
        const corp = parseInt(document.querySelector("[name='corpofficers']")?.value || 0);
        const high = parseInt(document.querySelector("[name='highearners']")?.value || 0);
        const rem = total - part - fam - corp - high;
        document.getElementById("remaining").value = rem < 0 ? 0 : rem;
    }
    function checkUniform() {
        const anyNo = ["uniform1","uniform2","uniform3","uniform4"].some(n => document.querySelector("[name='"+n+"']").value === "No");
        document.getElementById("uniformExplain").classList.toggle("hidden", !anyNo);
        document.getElementById("participationBlock").classList.toggle("hidden", !anyNo);
    }
    checkAffiliated();
</script>

</body>
</html>
