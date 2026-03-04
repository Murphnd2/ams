<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.util.List" %>
<%@ page import="net.superiorstate.ams.controller.data.AchEntry" %>
<%@ page import="net.superiorstate.ams.controller.data.AchParseResult" %>
<c:set var="pageTitle" value="ACH Returns" scope="request"/>
<c:set var="pageIcon" value="bi-bank" scope="request"/>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${applicationScope.global.psp.fullName} — ACH Returns</title>
    <style>
        .ach-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
            overflow: hidden;
        }
        .ach-toolbar {
            display: flex; align-items: center; justify-content: space-between;
            padding: 10px 16px 8px; flex-shrink: 0;
        }
        .ach-scroll {
            flex: 1; overflow-y: auto; padding: 0 16px 16px;
        }
        .ach-content { max-width: 960px; margin: 0 auto; }

        /* Drop zone */
        .drop-zone {
            border: 2px dashed #c8d1da;
            border-radius: 10px;
            padding: 2.5rem;
            text-align: center;
            background: #f8fafb;
            transition: border-color 0.2s, background 0.2s;
            cursor: pointer;
        }
        .drop-zone.drag-over {
            border-color: #0d5681;
            background: #e8f0f6;
        }
        .drop-zone-icon { font-size: 2.5rem; color: #0d5681; margin-bottom: 0.5rem; }
        .drop-zone-text { font-size: 0.9rem; color: #6c757d; }
        .drop-zone-text strong { color: #0d5681; }
        .drop-zone-hint { font-size: 0.75rem; color: #9ca3af; margin-top: 0.35rem; }

        /* Preview table */
        .preview-table th {
            font-size: 0.78rem; white-space: nowrap;
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
        }
        .preview-table td { font-size: 0.84rem; vertical-align: middle; }

        /* Match badges */
        .badge-employee { background: #198754; color: #fff; font-size: 0.7rem; }
        .badge-person { background: #0d6efd; color: #fff; font-size: 0.7rem; }
        .badge-new { background: #ffc107; color: #212529; font-size: 0.7rem; }

        /* Alert styles */
        .ach-alert {
            border-radius: 8px;
            font-size: 0.85rem;
            padding: 0.75rem 1rem;
            margin-bottom: 1rem;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="ach-wrap">
        <div class="ach-scroll">
            <div class="ach-content">

                <%-- Error message --%>
                <c:if test="${not empty error}">
                    <div class="alert alert-danger ach-alert">
                        <i class="bi bi-exclamation-triangle me-2"></i>${error}
                    </div>
                </c:if>

                <%-- Success message --%>
                <c:if test="${not empty successMessage}">
                    <div class="alert alert-success ach-alert">
                        <i class="bi bi-check-circle me-2"></i>${successMessage}
                        <div class="mt-2">
                            <a href="ViewHome25" class="btn btn-sm btn-outline-success">
                                <i class="bi bi-house me-1"></i>Go to Home
                            </a>
                        </div>
                    </div>
                </c:if>

                <%-- ═══ STATE 2: Preview parsed entries ═══ --%>
                <c:if test="${not empty entries}">
                    <%
                        AchParseResult parseResult = (AchParseResult) request.getAttribute("parseResult");
                        List<AchEntry> entries = (List<AchEntry>) request.getAttribute("entries");
                    %>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex align-items-center justify-content-between">
                            <span><i class="bi bi-file-earmark-text me-2"></i>Parsed ACH Report</span>
                            <span class="text-white-50" style="font-size:0.78rem;">
                                <%= parseResult != null ? parseResult.getReportType() : "" %>
                                <% if (parseResult != null && parseResult.getReportDate() != null) { %>
                                    &mdash; <%= parseResult.getReportDate() %>
                                <% } %>
                            </span>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-sm table-hover mb-0 preview-table">
                                    <thead>
                                        <tr>
                                            <th class="ps-3">#</th>
                                            <th>Individual</th>
                                            <th>ID</th>
                                            <th>Reason</th>
                                            <th>Corrected Data</th>
                                            <th>Eff. Date</th>
                                            <th>Matched To</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <% int idx = 0; for (AchEntry e : entries) { idx++; %>
                                        <tr>
                                            <td class="ps-3 text-muted"><%= idx %></td>
                                            <td><strong><%= e.getIndividualName() %></strong></td>
                                            <td class="text-muted"><%= e.getIndividualId() %></td>
                                            <td>
                                                <span class="badge bg-secondary"><%= e.getReasonCode() %></span>
                                                <span class="text-muted" style="font-size:0.78rem;"><%= e.getReasonDescription() != null ? e.getReasonDescription() : "" %></span>
                                            </td>
                                            <td style="font-size:0.82rem;"><%= e.getFormattedCorrectedData() %></td>
                                            <td><%= e.getFormattedEffectiveDate() %></td>
                                            <td>
                                                <% if ("EMPLOYEE".equals(e.getMatchType())) { %>
                                                    <span class="badge badge-employee">Employee</span>
                                                <% } else if ("PERSON".equals(e.getMatchType())) { %>
                                                    <span class="badge badge-person">Person</span>
                                                <% } else { %>
                                                    <span class="badge badge-new">New</span>
                                                <% } %>
                                                <span style="font-size:0.8rem;"><%= e.getMatchedPersonName() != null ? e.getMatchedPersonName() : "" %></span>
                                            </td>
                                        </tr>
                                        <% } %>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                        <div class="card-footer d-flex align-items-center justify-content-between" style="background:#f8fafb;">
                            <span class="text-muted" style="font-size:0.82rem;">
                                <strong><%= entries.size() %></strong> entry/entries will create tickets assigned to you
                            </span>
                            <div>
                                <a href="AchReturnProcessor" class="btn btn-sm btn-outline-secondary me-2">
                                    <i class="bi bi-arrow-left me-1"></i>Cancel
                                </a>
                                <form method="post" action="AchReturnProcessor" style="display:inline;">
                                    <input type="hidden" name="action" value="confirm">
                                    <button type="submit" class="btn btn-sm btn-ssa">
                                        <i class="bi bi-check-lg me-1"></i>Create Tickets
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:if>

                <%-- ═══ STATE 1: Upload form ═══ --%>
                <c:if test="${empty entries && empty successMessage}">
                    <div class="card">
                        <div class="hdr-bar">
                            <i class="bi bi-bank me-2"></i>Upload ACH Return / NOC Report
                        </div>
                        <div class="card-body">
                            <form id="uploadForm" method="post" action="AchReturnProcessor" enctype="multipart/form-data">
                                <input type="hidden" name="action" value="upload">
                                <div class="drop-zone" id="dropZone">
                                    <div class="drop-zone-icon"><i class="bi bi-file-earmark-pdf"></i></div>
                                    <div class="drop-zone-text">
                                        Drag &amp; drop PDF here, or <strong>click to browse</strong>
                                    </div>
                                    <div class="drop-zone-hint">Wells Fargo ACH Return/NOC Report (PDF only)</div>
                                    <input type="file" class="d-none" id="fileInput" name="file" accept=".pdf">
                                </div>

                                <div id="fileInfo" class="mt-3" style="display:none;">
                                    <div class="d-flex align-items-center p-2 border rounded" style="background:#f8fafb;">
                                        <i class="bi bi-file-earmark-pdf text-danger me-2" style="font-size:1.2rem;"></i>
                                        <span id="fileName" class="flex-grow-1" style="font-size:0.85rem; font-weight:500;"></span>
                                        <span id="fileSize" class="text-muted me-3" style="font-size:0.75rem;"></span>
                                        <button type="button" class="btn btn-sm btn-outline-danger" id="removeFile" style="font-size:0.75rem; padding:0.1rem 0.5rem;">
                                            <i class="bi bi-x-lg"></i>
                                        </button>
                                    </div>
                                </div>

                                <div class="text-end mt-3">
                                    <button type="submit" class="btn btn-ssa" id="uploadBtn" disabled>
                                        <i class="bi bi-upload me-1"></i>Parse Report
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </c:if>

            </div><%-- /ach-content --%>
        </div><%-- /ach-scroll --%>
    </div><%-- /ach-wrap --%>
</div><%-- /container-fluid --%>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
(function() {
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileInfo = document.getElementById('fileInfo');
    const uploadBtn = document.getElementById('uploadBtn');

    if (!dropZone || !fileInput) return;

    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drag-over'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) {
            fileInput.files = e.dataTransfer.files;
            showFile(e.dataTransfer.files[0]);
        }
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) showFile(fileInput.files[0]);
    });

    const removeBtn = document.getElementById('removeFile');
    if (removeBtn) {
        removeBtn.addEventListener('click', () => {
            fileInput.value = '';
            fileInfo.style.display = 'none';
            dropZone.style.display = 'block';
            uploadBtn.disabled = true;
        });
    }

    function showFile(file) {
        document.getElementById('fileName').textContent = file.name;
        document.getElementById('fileSize').textContent = formatSize(file.size);
        fileInfo.style.display = 'block';
        dropZone.style.display = 'none';
        uploadBtn.disabled = false;
    }

    function formatSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1048576).toFixed(1) + ' MB';
    }
})();
</script>
</body>
</html>
