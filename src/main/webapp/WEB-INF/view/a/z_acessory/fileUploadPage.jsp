<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="jakarta.servlet.jsp.JspWriter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="net.superiorstate.ams.data.service.Importer.TableMapping" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.Map" %>
<c:set var="pageTitle" value="Data Import" scope="request"/>
<c:set var="pageIcon" value="bi-cloud-upload" scope="request"/>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${applicationScope.global.psp.fullName} — Data Import</title>
    <style>
        .upload-wrapper { max-width: 960px; margin: 1rem auto; }

        /* Upload drop zone */
        .drop-zone {
            border: 2px dashed #c8d1da;
            border-radius: 10px;
            padding: 2rem;
            text-align: center;
            background: #f8fafb;
            transition: border-color 0.2s, background 0.2s;
            cursor: pointer;
            position: relative;
        }
        .drop-zone.drag-over {
            border-color: #0d5681;
            background: #e8f0f6;
        }
        .drop-zone-icon { font-size: 2.5rem; color: #0d5681; margin-bottom: 0.5rem; }
        .drop-zone-text { font-size: 0.9rem; color: #6c757d; }
        .drop-zone-text strong { color: #0d5681; }
        .drop-zone-hint { font-size: 0.75rem; color: #9ca3af; margin-top: 0.35rem; }

        /* File list card */
        .file-card {
            background: white;
            border: 1.5px solid #e5e7eb;
            border-radius: 10px;
            overflow: hidden;
            margin-top: 1rem;
        }

        /* File rows */
        .file-row {
            display: flex;
            align-items: center;
            padding: 0.55rem 0.85rem;
            border-bottom: 1px solid #f0f0f0;
            font-size: 0.82rem;
            transition: background 0.1s;
        }
        .file-row:last-child { border-bottom: none; }
        .file-row:hover { background: #f8fafb; }
        .file-row .file-icon { color: #0d5681; font-size: 1rem; margin-right: 0.6rem; flex-shrink: 0; }
        .file-row .file-name { flex: 1; font-weight: 500; color: #1a202c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .file-row .file-prefix {
            font-size: 0.72rem;
            font-weight: 600;
            padding: 0.1rem 0.5rem;
            border-radius: 10px;
            margin: 0 0.5rem;
            white-space: nowrap;
        }
        .prefix-matched { background: #d4edda; color: #155724; }
        .prefix-warning { background: #fff3cd; color: #856404; }
        .prefix-none { background: #f8d7da; color: #842029; }
        .file-row .file-remove {
            color: #adb5bd;
            cursor: pointer;
            font-size: 0.9rem;
            padding: 0.15rem 0.35rem;
            border-radius: 4px;
            transition: color 0.15s, background 0.15s;
        }
        .file-row .file-remove:hover { color: #dc3545; background: #fef2f2; }

        /* Mapping status */
        .mapping-row {
            display: flex;
            align-items: center;
            padding: 0.4rem 0.85rem;
            border-bottom: 1px solid #f0f0f0;
            font-size: 0.8rem;
        }
        .mapping-row:last-child { border-bottom: none; }
        .mapping-prefix { font-weight: 600; color: #1a202c; flex: 0 0 180px; }
        .mapping-status { flex: 1; }
        .mapping-dot {
            display: inline-block;
            width: 8px; height: 8px;
            border-radius: 50%;
            margin-right: 0.4rem;
            vertical-align: middle;
        }
        .dot-ok { background: #87a948; }
        .dot-warn { background: #fd7e14; }
        .dot-miss { background: #dc3545; }

        /* Summary bar */
        .upload-summary {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0.6rem 0.85rem;
            background: #f8fafb;
            border-top: 1.5px solid #e5e7eb;
            font-size: 0.8rem;
            color: #6c757d;
        }
        .upload-summary .count-badge {
            background: #0d5681;
            color: white;
            padding: 0.1rem 0.55rem;
            border-radius: 10px;
            font-size: 0.72rem;
            font-weight: 600;
            margin-right: 0.35rem;
        }

        /* Empty state */
        .upload-empty {
            text-align: center;
            padding: 2rem;
            color: #adb5bd;
        }
        .upload-empty i { font-size: 2rem; display: block; margin-bottom: 0.5rem; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="upload-wrapper">

        <%-- ═══ UPLOAD SECTION ═══ --%>
        <div class="card file-card">
            <div class="hdr-bar d-flex align-items-center justify-content-between">
                <span><i class="bi bi-cloud-upload me-2"></i>Upload Files for Import</span>
                <button type="button" class="btn btn-sm btn-outline-light" id="uploadBtn"
                        style="font-size:0.75rem; padding:0.15rem 0.6rem;" disabled>
                    <i class="bi bi-send me-1"></i>Upload
                </button>
            </div>

            <form id="uploadForm" action="UploadCsvServlet" method="post" enctype="multipart/form-data">
                <%-- Drop zone --%>
                <div class="drop-zone" id="dropZone">
                    <div class="drop-zone-icon"><i class="bi bi-file-earmark-arrow-up"></i></div>
                    <div class="drop-zone-text">
                        Drag &amp; drop files here, or <strong>click to browse</strong>
                    </div>
                    <div class="drop-zone-hint">CSV, XLS, XLSX &bull; Files are matched automatically by header analysis</div>
                    <input type="file" class="d-none" id="fileInput" name="csvFiles" accept=".csv,.xls,.xlsx" multiple>
                </div>

                <%-- File list --%>
                <div id="fileListContainer" style="display:none;">
                    <div id="fileListBody"></div>
                    <div class="upload-summary" id="uploadSummary">
                        <div>
                            <span class="count-badge" id="fileCount">0</span> files selected
                            &middot;
                            <span id="matchedCount">0</span> matched
                        </div>
                        <div id="summaryWarnings" style="color: #fd7e14;"></div>
                    </div>
                </div>
            </form>
        </div>

        <%-- ═══ MAPPING STATUS ═══ --%>
        <div class="card file-card mt-3" id="mappingCard" style="display:none;">
            <div class="hdr-bar" style="font-size:0.85rem;">
                <i class="bi bi-diagram-3 me-2"></i>Expected Table Mappings
            </div>
            <div id="mappingStatusBody"></div>
        </div>

    </div><%-- /upload-wrapper --%>
</div><%-- /container-fluid --%>

<script src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

<%
    List<TableMapping> mappings = (List<TableMapping>) request.getAttribute("tableMappings");
    Gson gson = new Gson();
%>
<script>
    // ── Table mappings from server ──
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
    const dropZone = document.getElementById('dropZone');
    const fileListContainer = document.getElementById('fileListContainer');
    const fileListBody = document.getElementById('fileListBody');
    const uploadBtn = document.getElementById('uploadBtn');
    const mappingCard = document.getElementById('mappingCard');

    // ── Drop zone interactions ──
    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drag-over'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) {
            fileList = [...fileList, ...Array.from(e.dataTransfer.files)];
            recalculateMappingsAndRender();
        }
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) {
            fileList = [...fileList, ...Array.from(fileInput.files)];
            recalculateMappingsAndRender();
        }
    });

    // ── Upload button ──
    uploadBtn.addEventListener('click', () => {
        syncHiddenInputs();
        for (const [prefix, files] of Object.entries(matchedPrefixes)) {
            if (files.length > 1) {
                alert('Please remove all but one file matching table prefix "' + prefix + '"');
                return;
            }
        }
        document.getElementById('uploadForm').submit();
    });

    // ── Matching logic ──
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

    // ── Render file list ──
    const renderFileList = () => {
        fileListBody.innerHTML = '';
        if (fileList.length === 0) {
            fileListContainer.style.display = 'none';
            mappingCard.style.display = 'none';
            uploadBtn.disabled = true;
            return;
        }
        fileListContainer.style.display = 'block';
        mappingCard.style.display = 'block';
        uploadBtn.disabled = false;

        let matchedTotal = 0;
        let warnings = 0;

        fileList.forEach((file) => {
            const fileName = file.name;
            const matchedPrefix = filePrefixMap[fileName];
            const matches = matchedPrefix ? matchedPrefixes[normalize(matchedPrefix)] || [] : [];

            let prefixHtml, iconClass;
            if (matchedPrefix && matches.length > 1) {
                prefixHtml = '<span class="file-prefix prefix-warning">' + matchedPrefix + ' (duplicate)</span>';
                iconClass = 'bi-exclamation-triangle text-warning';
                warnings++;
                matchedTotal++;
            } else if (matchedPrefix) {
                prefixHtml = '<span class="file-prefix prefix-matched">' + matchedPrefix + '</span>';
                iconClass = 'bi-check-circle text-success';
                matchedTotal++;
            } else {
                prefixHtml = '<span class="file-prefix prefix-none">No match</span>';
                iconClass = 'bi-x-circle text-danger';
            }

            const row = document.createElement('div');
            row.className = 'file-row';
            row.innerHTML =
                '<i class="bi ' + iconClass + ' file-icon"></i>' +
                '<span class="file-name" title="' + fileName.replace(/"/g, '&quot;') + '">' + fileName + '</span>' +
                prefixHtml +
                '<span class="file-remove" data-filename="' + fileName.replace(/"/g, '&quot;') + '" title="Remove"><i class="bi bi-x-lg"></i></span>';
            fileListBody.appendChild(row);
        });

        // Summary
        document.getElementById('fileCount').textContent = fileList.length;
        document.getElementById('matchedCount').textContent = matchedTotal;
        document.getElementById('summaryWarnings').textContent = warnings > 0 ? (warnings + ' duplicate warning' + (warnings > 1 ? 's' : '')) : '';

        // Remove handlers
        fileListBody.querySelectorAll('.file-remove').forEach(btn => {
            btn.addEventListener('click', function() {
                const fn = this.getAttribute('data-filename');
                fileList = fileList.filter(f => f.name !== fn);
                recalculateMappingsAndRender();
            });
        });
    };

    // ── Render mapping status ──
    const updateMappingStatus = () => {
        const tbody = document.getElementById('mappingStatusBody');
        tbody.innerHTML = '';

        Object.entries(tableMappings).forEach(([prefix, mapping]) => {
            const normalizedPrefix = normalize(prefix);
            const rawMatches = matchedPrefixes[normalizedPrefix];

            let dotClass, statusText;
            if (rawMatches && rawMatches.length > 1) {
                dotClass = 'dot-warn';
                statusText = 'Multiple matches: ' + rawMatches.join(', ');
            } else if (rawMatches && rawMatches.length === 1) {
                dotClass = 'dot-ok';
                statusText = rawMatches[0];
            } else {
                dotClass = 'dot-miss';
                statusText = 'No file matched';
            }

            const row = document.createElement('div');
            row.className = 'mapping-row';
            row.innerHTML =
                '<span class="mapping-prefix">' + prefix + '</span>' +
                '<span class="mapping-status"><span class="mapping-dot ' + dotClass + '"></span>' + statusText + '</span>';
            tbody.appendChild(row);
        });
    };

    // ── Sync hidden inputs for form submission ──
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
        // Refresh the actual file input
        const dt = new DataTransfer();
        fileList.forEach(file => dt.items.add(file));
        fileInput.files = dt.files;
    };

    // ── Recalculate all matches and re-render ──
    const recalculateMappingsAndRender = () => {
        Object.keys(matchedPrefixes).forEach(k => delete matchedPrefixes[k]);
        Object.keys(filePrefixMap).forEach(k => delete filePrefixMap[k]);
        let readCount = 0;

        if (fileList.length === 0) {
            renderFileList();
            return;
        }

        fileList.forEach(file => {
            const finalizeRender = () => {
                readCount++;
                if (readCount === fileList.length) {
                    renderFileList();
                    updateMappingStatus();
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
            } else if (file.name.toLowerCase().endsWith('.xlsx') || file.name.toLowerCase().endsWith('.xls')) {
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
    };
</script>
</body>
</html>
