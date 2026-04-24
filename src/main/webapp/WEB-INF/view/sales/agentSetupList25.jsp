<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>My Setups</title>
    <style>
        :root { --ssa: #0d5681; --ssa-accent: #87a948; }
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
            font-weight: 700; color: var(--ssa); font-size: 0.95rem; margin: 0;
        }
        .toolbar .t-count {
            font-size: 0.75rem; color: #6c757d;
        }
        .setup-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .setup-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.7rem 0.9rem; margin-bottom: 0.55rem;
            display: grid; grid-template-columns: 1.4fr 1fr 110px 110px 120px 120px; gap: 0.75rem;
            align-items: center;
            font-size: 0.82rem;
        }
        .setup-card:hover { box-shadow: 0 2px 10px rgba(13,86,129,0.12); border-color: #b7d4e8; }
        .setup-card a:hover { text-decoration: underline !important; }
        .setup-card .s-name {
            font-weight: 700; color: var(--ssa);
            display: flex; align-items: center; gap: 0.4rem;
        }
        .setup-card .s-sub {
            font-size: 0.72rem; color: #6c757d; font-weight: 400;
            display: block; margin-top: 2px;
        }
        .setup-card .col-head {
            font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.04em;
            color: #6c757d; font-weight: 600;
        }
        .setup-card .progress {
            height: 6px; border-radius: 3px; background: #e9ecef; overflow: hidden;
        }
        .setup-card .progress-bar {
            height: 100%; background: var(--ssa-accent);
        }
        .setup-card .badge-my {
            display: inline-block; padding: 2px 8px; border-radius: 10px;
            font-size: 0.72rem; font-weight: 700;
        }
        .my-has { background: #fff3cd; color: #856404; }
        .my-none { background: #e9ecef; color: #6c757d; }
        .selling-me { background: #d1ecf1; color: #0c5460; padding: 1px 6px; border-radius: 8px; font-size: 0.65rem; font-weight: 700; margin-left: 4px; }
        .header-row {
            display: grid; grid-template-columns: 1.4fr 1fr 110px 110px 120px 120px; gap: 0.75rem;
            padding: 0.4rem 0.9rem;
            font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.04em;
            color: #6c757d; font-weight: 600;
            position: sticky; top: 0; background: #eef1f5; z-index: 1;
        }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
        .btn-back {
            background: transparent; border: 1px solid #dee2e6; color: var(--ssa);
            border-radius: 4px; font-size: 0.78rem; padding: 3px 10px;
            text-decoration: none;
        }
        .btn-back:hover { background: #fff; border-color: var(--ssa); }
    </style>
</head>
<body>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="audit-wrap">
        <div class="toolbar">
            <a href="AgentHome" class="btn-back"><i class="bi bi-arrow-left"></i> Pipeline</a>
            <h1 class="t-title m-0"><i class="bi bi-clipboard-check me-1"></i>My Setups</h1>
            <span class="t-count">
                <c:choose>
                    <c:when test="${isAgencyAdmin}">Agency-wide view &middot; ${fn:length(setupRows)} setup${fn:length(setupRows) == 1 ? '' : 's'}</c:when>
                    <c:otherwise>${fn:length(setupRows)} setup${fn:length(setupRows) == 1 ? '' : 's'}</c:otherwise>
                </c:choose>
            </span>
        </div>

        <div class="setup-body">
            <c:choose>
                <c:when test="${empty setupRows}">
                    <div class="empty-state">
                        <i class="bi bi-clipboard2" style="font-size: 2rem; display: block; margin-bottom: 0.5rem;"></i>
                        No active Setups are visible to you yet. Setups appear here when you are the selling agent, or when a PSP delegates a Setup task directly to you.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="header-row">
                        <div>Employer / Prospect</div>
                        <div>Selling Agent</div>
                        <div>Due</div>
                        <div>Progress</div>
                        <div>My Open ToDos</div>
                        <div>&nbsp;</div>
                    </div>
                    <c:forEach var="row" items="${setupRows}">
                        <div class="setup-card">
                            <div>
                                <div class="s-name">
                                    <i class="bi bi-clipboard-check"></i>
                                    <a href="ViewById?id=${row.id}"
                                       style="color:#0d5681; text-decoration:underline; font-weight:700;">
                                        <c:out value="${row.name}"/>
                                    </a>
                                    <span style="color:#6c757d; font-weight:400; font-size:0.7rem; margin-left:4px;">
                                        #${row.id}
                                    </span>
                                </div>
                                <span class="s-sub"><c:out value="${row.prospectName}"/></span>
                            </div>
                            <div>
                                <c:out value="${row.sellingAgentName}"/>
                                <c:if test="${row.sellingAgentIsMe}"><span class="selling-me">ME</span></c:if>
                            </div>
                            <div>
                                <c:choose>
                                    <c:when test="${row.dueDate != null}">
                                        <fmt:formatDate value="${row.dueDate}" pattern="MMM d, yyyy"/>
                                    </c:when>
                                    <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                </c:choose>
                            </div>
                            <div>
                                <div class="progress" title="${row.doneToDos} of ${row.totalToDos} complete">
                                    <div class="progress-bar" style="width: ${row.progressPct}%"></div>
                                </div>
                                <div class="col-head" style="margin-top:2px;">${row.progressPct}% (${row.doneToDos}/${row.totalToDos})</div>
                            </div>
                            <div>
                                <c:choose>
                                    <c:when test="${row.myOpenToDos > 0}">
                                        <span class="badge-my my-has">${row.myOpenToDos} open</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge-my my-none">none</span>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${isAgencyAdmin && row.scopedOpenToDos > row.myOpenToDos}">
                                    <div class="col-head" style="margin-top:2px;">${row.scopedOpenToDos} agency</div>
                                </c:if>
                            </div>
                            <div style="text-align:right;">
                                <a href="ViewById?id=${row.id}"
                                   class="btn btn-primary"
                                   style="font-size:0.78rem; padding: 3px 12px; background:#0d5681; border-color:#0d5681; color:white;">
                                    Open <i class="bi bi-chevron-right"></i>
                                </a>
                            </div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>
