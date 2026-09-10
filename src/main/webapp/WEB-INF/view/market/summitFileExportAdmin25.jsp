<%--
  S32-G — Retained Summit export files (T212).

  Lists every file SummitExportServlet actually generated for this PSP, newest first, and lets one
  be downloaded again as the exact bytes that were sent.

  ⚠️ PII. summit_file_export.content holds participant names, street addresses and email addresses
  (the Demographics file's rows). This page therefore NEVER previews, excerpts or renders content —
  the list is metadata only, and the bytes leave only through the download action, which re-checks
  the row's PSP against the session's. Do not add a preview column.

  ⚠️ T194. Summit dedupes a re-sent file on content, so re-sending identical bytes does nothing at
  all — no error, no results row. Rows sharing a content hash are badged below for that reason.

  ⚠️ No timezone arithmetic. T216 established (page view, 2026-09-08) that the JPA round trip is
  symmetric, so generatedAt reads back as local time. Display it as-is; converting would reintroduce
  the five-hour skew T216 was filed about.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Retained Summit Exports</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
        }
        .toolbar {
            padding: 0.65rem 1rem;
            background: #fff; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.75rem;
        }
        .toolbar .t-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0;
        }
        .rc-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .map-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .map-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .map-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: middle; }
        .map-table tbody tr:hover { background: #f8f9fa; }
        .badge-dup { background: #fd7e14; color: #fff; }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .num { text-align: right; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-archive me-1"></i>Retained Summit Exports
        </h1>
    </div>

    <div class="rc-body">

        <c:if test="${not pspResolved}">
            <div class="alert alert-warning py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i>Could not determine your PSP from this
                session, so nothing is listed. Sign out and back in, then try again.
            </div>
        </c:if>

        <div class="status-card">
            <div>
                Every file the Summit export screen has generated for your PSP is kept here with the
                <strong>exact bytes that were sent</strong>. Downloading a row below returns those stored
                bytes — it does <strong>not</strong> regenerate the file, which matters because plan
                template mappings, the TPA prefix, the branch code and the participant roster can all
                have changed since an export ran.
            </div>
            <div class="mt-2">
                <span class="badge badge-dup"><i class="bi bi-files"></i> Same content</span>
                <span class="ms-2"><strong>Summit does not process a file twice.</strong> Rows
                    carrying this badge share their bytes with another row listed here. Summit holds
                    a file whose content matches an already-processed file pending TPA approval in
                    File History, and rejects a repeated filename outright.</span>
            </div>
        </div>

        <c:choose>
            <c:when test="${empty exports}">
                <div class="empty-state">
                    No exports have been retained yet.
                </div>
            </c:when>
            <c:otherwise>
                <table class="map-table">
                    <thead>
                    <tr>
                        <th>Generated</th>
                        <th>Type</th>
                        <th>File name</th>
                        <th>Delivery</th>
                        <th class="num">Rows</th>
                        <th class="num">Bytes</th>
                        <th class="num">Prospect</th>
                        <th class="num">Proposal</th>
                        <th>Generated by</th>
                        <th>Content hash</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="e" items="${exports}">
                        <tr>
                            <td class="mono">${e.generatedAt}</td>
                            <td><c:out value="${e.fileType}"/></td>
                            <td><c:out value="${e.fileName}"/></td>
                            <td>
                                <c:out value="${empty e.deliveryStatus ? 'download' : e.deliveryStatus}"/>
                                <c:if test="${not empty e.deliveryError}">
                                    <br/><small class="text-danger"><c:out value="${e.deliveryError}"/></small>
                                </c:if>
                            </td>
                            <td class="num">${e.rowCount}</td>
                            <td class="num">${e.byteCount}</td>
                            <td class="num"><c:out value="${e.prospectId}"/></td>
                            <td class="num"><c:out value="${e.proposalId}"/></td>
                            <td><c:out value="${e.generatedBy}"/></td>
                            <td class="mono">
                                <c:out value="${fn:substring(e.contentSha256, 0, 12)}"/>
                                <c:if test="${duplicateHash[e.contentSha256]}">
                                    <span class="badge badge-dup ms-1"
                                          title="Another retained export has identical bytes. Summit would dedupe a re-send of this file.">
                                        <i class="bi bi-files"></i> Same content
                                    </span>
                                </c:if>
                            </td>
                            <td>
                                <a class="btn btn-sm btn-outline-secondary py-0"
                                   style="font-size:0.72rem;"
                                   href="SummitFileExportAdmin?action=download&id=${e.id}">
                                    <i class="bi bi-download me-1"></i>Download
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>

    </div>
</div>
</body>
</html>
