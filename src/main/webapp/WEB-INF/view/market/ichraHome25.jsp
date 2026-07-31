<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ICHRA</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .ichra-wrap {
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
        .ichra-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .hub-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
            gap: 1rem;
        }
        .hub-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 1.1rem; text-decoration: none; color: inherit;
            display: block;
        }
        .hub-card:hover {
            border-color: var(--ssa, #0d5681);
            box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            text-decoration: none; color: inherit;
        }
        .hub-card .hub-icon {
            font-size: 1.5rem; color: var(--ssa, #0d5681); margin-bottom: 0.5rem;
        }
        .hub-card .hub-title {
            font-weight: 700; font-size: 0.9rem; margin-bottom: 0.25rem;
        }
        .hub-card .hub-desc {
            font-size: 0.8rem; color: #6c757d;
        }
        .hub-card.disabled {
            opacity: 0.55; pointer-events: none; cursor: default;
        }
        .hub-card .hub-badge {
            font-size: 0.65rem; text-transform: uppercase; letter-spacing: 0.04em;
            font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 4px;
            display: inline-block; margin-bottom: 0.5rem;
        }
        .hub-badge.live { background: #d1e7dd; color: #0f5132; }
        .hub-badge.coming { background: #e9ecef; color: #6c757d; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="ichra-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0"><i class="bi bi-heart-pulse me-1"></i>ICHRA</h1>
    </div>

    <div class="ichra-body">
        <div class="hub-grid">

            <a class="hub-card" href="Illustration">
                <span class="hub-badge live">Live</span>
                <div class="hub-icon"><i class="bi bi-calculator"></i></div>
                <div class="hub-title">Rating-Area Illustration</div>
                <div class="hub-desc">Indicative market premium range by county and headcount.</div>
            </a>

            <a class="hub-card" href="RateCacheAdmin">
                <span class="hub-badge live">Live</span>
                <div class="hub-icon"><i class="bi bi-graph-up"></i></div>
                <div class="hub-title">Rate Cache Admin</div>
                <div class="hub-desc">Cache status and manual refresh for the rating-area rate cache.</div>
            </a>

            <div class="hub-card disabled">
                <span class="hub-badge coming">Coming</span>
                <div class="hub-icon"><i class="bi bi-bar-chart-steps"></i></div>
                <div class="hub-title">Age-Band Net Cost</div>
                <div class="hub-desc">Per-employee net cost after contribution, by age band.</div>
            </div>

            <div class="hub-card disabled">
                <span class="hub-badge coming">Coming</span>
                <div class="hub-icon"><i class="bi bi-shield-check"></i></div>
                <div class="hub-title">Affordability Threshold</div>
                <div class="hub-desc">Employer- and agent-facing affordability check against the benchmark plan.</div>
            </div>

            <div class="hub-card disabled">
                <span class="hub-badge coming">Coming</span>
                <div class="hub-icon"><i class="bi bi-arrow-left-right"></i></div>
                <div class="hub-title">Group-to-ICHRA Conversion</div>
                <div class="hub-desc">Side-by-side comparison against an existing group plan.</div>
            </div>

            <div class="hub-card disabled">
                <span class="hub-badge coming">Coming</span>
                <div class="hub-icon"><i class="bi bi-signpost-split"></i></div>
                <div class="hub-title">Design Advisor</div>
                <div class="hub-desc">Guided contribution-strategy recommendations.</div>
            </div>

        </div>
    </div>
</div>
</body>
</html>
