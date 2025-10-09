<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="jakarta.servlet.jsp.JspWriter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="net.superiorstate.ams.data.Importer.TableMapping" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.Map" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${sessionScope.psp.getFullName()}</title>
    <link href="https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap5.min.css" rel="stylesheet"/>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/navbar.jsp"/>

    <div class="mt-4">
        <h2>📄 Upload Files for Import</h2>

        <form id="uploadForm" action="UploadCsvServlet" method="post" enctype="multipart/form-data">
            <div class="mb-3">
                <label for="fileInput" class="form-label">Select Files</label>
                <input type="file" class="form-control" id="fileInput" name="csvFiles" accept=".csv,.xls,.xlsx" multiple required>
            </div>
            <button type="submit" class="btn btn-primary">Upload</button>
        </form>

        <hr>
        <h4 class="mt-4">😮 Pre-Upload File Analysis</h4>
        <table id="previewTable" class="table table-bordered table-hover">
            <thead class="table-light">
            <tr>
                <th>Filename</th>
                <th>Status</th>
                <th>Matched Prefix</th>
                <th></th>
            </tr>
            </thead>
            <tbody></tbody>
        </table>

        <h4 class="mt-5">📋 Expected Table Mappings</h4>
        <table id="mappingStatusTable" class="table table-sm table-striped">
            <thead>
            <tr>
                <th>Table Prefix</th>
                <th>Status</th>
            </tr>
            </thead>
            <tbody id="mappingStatusBody"></tbody>
        </table>
    </div>

    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/1.13.6/js/dataTables.bootstrap5.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <%
        List<TableMapping> mappings = (List<TableMapping>) request.getAttribute("tableMappings");
        Gson gson = new Gson();
    %>
    <script>
        const tableMappings = {
            <% for (int i = 0; i < mappings.size(); i++) {
                TableMapping mapping = mappings.get(i);
                String prefix = mapping.filePrefix();
                List<String> columns = mapping.columns();
                Map<String, String> headerOverrides = mapping.headerOverrides();
                out.print("\"" + prefix + "\": {");
                out.print("\"columns\": " + gson.toJson(columns) + ",");
                out.print("\"headerOverrides\": " + gson.toJson(headerOverrides));
                out.print("}");
                if (i < mappings.size() - 1) out.print(",\n");
            } %>
        };

        const normalize = str => str.trim().toLowerCase().replace(/[^a-z0-9]/g, '');

        let fileList = [];
        const matchedPrefixes = {};
        const filePrefixMap = {};
        const fileInput = document.getElementById('fileInput');
        const previewTable = $('#previewTable').DataTable({ order: [[0, 'asc']] });

        const updateMappingStatusTable = () => {
            const tbody = document.getElementById('mappingStatusBody');
            tbody.innerHTML = '';

            Object.entries(tableMappings).forEach(([prefix, mapping]) => {
                const row = document.createElement('tr');
                const prefixCell = document.createElement('td');
                prefixCell.textContent = prefix;

                const statusCell = document.createElement('td');
                const normalizedPrefix = normalize(prefix);
                const rawMatches = matchedPrefixes[normalizedPrefix];

                if (rawMatches && rawMatches.length > 1) {
                    statusCell.innerHTML = `<span class="text-warning">⚠ Multiple matches: <code>${rawMatches.join(', ')}</code></span>`;
                } else if (rawMatches && rawMatches.length === 1) {
                    statusCell.innerHTML = `<span class="text-success">✅ Matched by: <code>${rawMatches[0]}</code></span>`;
                } else {
                    statusCell.innerHTML = `<span class="text-danger">❌ No match</span>`;
                }

                row.appendChild(prefixCell);
                row.appendChild(statusCell);
                tbody.appendChild(row);
            });
        };

        const renderTable = () => {
            previewTable.clear().draw();
            fileList.forEach((file) => {
                const fileName = file.name;
                const matchedPrefix = filePrefixMap[fileName];
                const matches = matchedPrefix ? matchedPrefixes[normalize(matchedPrefix)] || [] : [];
                let statusHtml = '<span class="text-danger">No Match</span>';
                let prefixHtml = '-';
                if (matchedPrefix) {
                    prefixHtml = matchedPrefix;
                    statusHtml = matches.length > 1
                        ? '<span class="text-warning">⚠ Multiple Matches</span>'
                        : '<span class="text-success">Matched</span>';
                }
                const escapedFileName = fileName.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
                const removeButton = '<button class="btn btn-sm btn-outline-danger remove-btn" data-filename="' + escapedFileName + '">Remove</button>';
                previewTable.row.add([fileName, statusHtml, prefixHtml, removeButton]);
            });
            previewTable.draw(false);
        };

        $('#previewTable tbody').on('click', 'button.remove-btn', function () {
            const fileName = $(this).data('filename');
            fileList = fileList.filter(f => f.name !== fileName);
            recalculateMappingsAndRender();
        });

        const syncHiddenInputs = () => {
            document.querySelectorAll('input[name="matchedPrefix"]').forEach(input => input.remove());
            fileList.forEach(file => {
                const prefix = filePrefixMap[file.name];
                if (prefix) {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'matchedPrefix';
                    input.value = prefix + '||' + file.name;
                    document.getElementById('uploadForm').appendChild(input);
                }
            });
        };

        const recalculateMappingsAndRender = () => {
            Object.keys(matchedPrefixes).forEach(k => delete matchedPrefixes[k]);
            Object.keys(filePrefixMap).forEach(k => delete filePrefixMap[k]);
            let readCount = 0;
            fileList.forEach(file => {
                const finalizeRender = () => {
                    readCount++;
                    if (readCount === fileList.length) {
                        renderTable();
                        updateMappingStatusTable();
                        refreshHiddenFileInput();
                    }
                };
                if (file.name.toLowerCase().endsWith('.csv')) {
                    const reader = new FileReader();
                    reader.onload = event => {
                        const firstLine = event.target.result.split(/\r?\n/)[0];
                        const headers = firstLine.split(',').map(normalize);
                        tryMatch(file.name, headers);
                        finalizeRender();
                    };
                    reader.readAsText(file, 'UTF-8');
                } else if (file.name.toLowerCase().endsWith('.xlsx')) {
                    const reader = new FileReader();
                    reader.onload = e => {
                        const data = new Uint8Array(e.target.result);
                        const workbook = XLSX.read(data, { type: 'array' });
                        const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
                        const headers = XLSX.utils.sheet_to_json(firstSheet, { header: 1 })[0] || [];
                        const normalizedHeaders = headers.map(normalize);
                        tryMatch(file.name, normalizedHeaders);
                        finalizeRender();
                    };
                    reader.readAsArrayBuffer(file);
                } else {
                    finalizeRender();
                }
            });
            fileInput.value = "";
        };

        const tryMatch = (fileName, headers) => {
            let bestMatch = null;
            let bestScore = 0;
            for (const [prefix, mapping] of Object.entries(tableMappings)) {
                const columns = mapping.columns.map(normalize);
                const overrideMap = Object.entries(mapping.headerOverrides || {}).reduce((acc, [k, v]) => {
                    acc[normalize(k)] = normalize(v);
                    return acc;
                }, {});
                const effective = columns.map(h => overrideMap[h] || h);
                const matchedCount = effective.filter(h => headers.includes(h)).length;
                const allFound = effective.every(h => headers.includes(h));
                if (allFound && matchedCount > bestScore) {
                    bestScore = matchedCount;
                    bestMatch = prefix;
                }
            }
            if (bestMatch) {
                filePrefixMap[fileName] = bestMatch;
                const normalized = normalize(bestMatch);
                matchedPrefixes[normalized] = matchedPrefixes[normalized] || [];
                matchedPrefixes[normalized].push(fileName);
            } else {
                const nameMatch = Object.entries(tableMappings).find(([prefix, mapping]) =>
                    mapping.columns.length === 0 && fileName.toLowerCase().includes(prefix.toLowerCase())
                );
                if (nameMatch) {
                    const [matchedPrefix] = nameMatch;
                    filePrefixMap[fileName] = matchedPrefix;
                    const normalized = normalize(matchedPrefix);
                    matchedPrefixes[normalized] = matchedPrefixes[normalized] || [];
                    matchedPrefixes[normalized].push(fileName);
                }
            }
        };

        const refreshHiddenFileInput = () => {
            const dt = new DataTransfer();
            fileList.forEach(file => dt.items.add(file));
            fileInput.files = dt.files;
        };

        fileInput.addEventListener('change', () => {
            if (fileInput.files.length > 0) {
                fileList = Array.from(fileInput.files);
                recalculateMappingsAndRender();
            }
        });

        document.getElementById('uploadForm').addEventListener('submit', (e) => {
            syncHiddenInputs();
            for (const [prefix, files] of Object.entries(matchedPrefixes)) {
                if (files.length > 1) {
                    e.preventDefault();
                    alert(`Please remove all but one file matching table prefix "${prefix}"`);
                    return;
                }
            }
        });
    </script>
</div>
</body>
</html>







































