<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Import Summit Data</title>
    <style>
        .upload-card {
            border: 2px dashed #dee2e6;
            border-radius: 8px;
            padding: 16px 20px;
            margin-bottom: 12px;
            transition: border-color 0.2s;
        }
        .upload-card:hover { border-color: var(--ssa); }
        .upload-card .badge { font-size: 0.7rem; }
        .upload-label { font-weight: 600; color: var(--ssa); margin-bottom: 4px; }
        .upload-hint { font-size: 0.82rem; color: #6c757d; margin-bottom: 8px; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 800px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-cloud-upload"></i> Import Summit Data</h4>
    <p class="text-muted mb-3">Upload your Summit export files. Plan Types must be imported first (reference data for benefits).</p>
    <hr>

    <form method="POST" action="SummitImport" enctype="multipart/form-data">
        <input type="hidden" name="action" value="upload">

        <%-- Plan Types --%>
        <div class="upload-card">
            <div class="upload-label">
                <i class="bi bi-file-earmark-spreadsheet"></i> Plan Types
                <span class="badge bg-secondary">Excel</span>
            </div>
            <div class="upload-hint">Pre-defined Summit export — categorizes benefits (FSA, HRA, COBRA, etc.)</div>
            <input type="file" class="form-control form-control-sm" name="planTypeFile"
                   accept=".xlsx,.xls" style="max-width: 450px;">
        </div>

        <%-- Employers --%>
        <div class="upload-card">
            <div class="upload-label">
                <i class="bi bi-building"></i> Employers
                <span class="badge bg-secondary">CSV</span>
            </div>
            <div class="upload-hint">J1 — Employer Listing</div>
            <input type="file" class="form-control form-control-sm" name="employerFile"
                   accept=".csv,.txt" style="max-width: 450px;">
        </div>

        <%-- Employees Contact (J2) --%>
        <div class="upload-card">
            <div class="upload-label">
                <i class="bi bi-people"></i> Employees — Contact Info
                <span class="badge bg-secondary">CSV</span>
            </div>
            <div class="upload-hint">J2 — Participant Listing Simple (names, email, addresses)</div>
            <input type="file" class="form-control form-control-sm" name="employeeJ2File"
                   accept=".csv,.txt" style="max-width: 450px;">
        </div>

        <%-- Employees Status (J3) --%>
        <div class="upload-card">
            <div class="upload-label">
                <i class="bi bi-person-check"></i> Employees — Status & Dates
                <span class="badge bg-secondary">CSV</span>
            </div>
            <div class="upload-hint">J3 — Participant Listing Report with Division Option (status IDs, hire/term dates)</div>
            <input type="file" class="form-control form-control-sm" name="employeeJ3File"
                   accept=".csv,.txt" style="max-width: 450px;">
        </div>

        <%-- Benefits --%>
        <div class="upload-card">
            <div class="upload-label">
                <i class="bi bi-heart-pulse"></i> Benefits
                <span class="badge bg-secondary">CSV</span>
            </div>
            <div class="upload-hint">J4 — Employer Benefit Plans (drives renewal pipeline)</div>
            <input type="file" class="form-control form-control-sm" name="benefitFile"
                   accept=".csv,.txt" style="max-width: 450px;">
        </div>

        <hr>
        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary"><i class="bi bi-arrow-right"></i> Next: Review & Configure</button>
            <a href="ViewHome25" class="btn btn-outline-secondary">Cancel</a>
        </div>
    </form>
</div>
</body>
</html>
