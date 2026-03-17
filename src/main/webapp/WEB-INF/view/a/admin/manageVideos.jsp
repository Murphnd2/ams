<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Training Videos</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .video-row { cursor: pointer; transition: background 0.15s; }
        .video-row:hover { background: #f0f4ff !important; }
        .video-row.selected { background: #e8f0fe !important; border-left: 3px solid var(--ssa); }
        .token-status { font-size: 0.75rem; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
        .token-status.unused { background: #e8f5e9; color: #2e7d32; }
        .token-status.viewed { background: #fff3e0; color: #e65100; }
        .token-status.consumed { background: #fce4ec; color: #c62828; }
        .token-status.expired { background: #f5f5f5; color: #757575; }
        .token-status.grace { background: #fff8e1; color: #f57f17; }
        .copy-btn { cursor: pointer; color: var(--ssa); }
        .copy-btn:hover { color: #06357a; }
        .token-link-box {
            background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 12px 16px; font-family: monospace; font-size: 0.85rem;
            word-break: break-all; margin-bottom: 12px;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="row g-3 mt-1">
        <div class="col-lg-11 col-xl-10 mx-auto">

            <%-- Flash messages --%>
            <c:if test="${not empty sessionScope.videoMessage}">
                <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                    <i class="bi bi-check-circle me-1"></i>${sessionScope.videoMessage}
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="videoMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.videoError}">
                <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                    <i class="bi bi-exclamation-triangle me-1"></i>${sessionScope.videoError}
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="videoError" scope="session"/>
            </c:if>

            <%-- Generated token link banner --%>
            <c:if test="${not empty sessionScope.generatedToken}">
                <div class="alert alert-info alert-dismissible fade show py-2" role="alert">
                    <strong><i class="bi bi-link-45deg me-1"></i>Generated Link:</strong>
                    <div class="token-link-box mt-2" id="generatedLink">${systemUrl}/video?t=${sessionScope.generatedToken}</div>
                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="copyLink()">
                        <i class="bi bi-clipboard me-1"></i>Copy Link
                    </button>
                    <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
                </div>
                <c:remove var="generatedToken" scope="session"/>
            </c:if>

            <div class="row g-3">
                <%-- Left panel: Video list --%>
                <div class="col-md-5">
                    <div class="card">
                        <div class="card-header bg-ssa text-white d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-camera-video me-1"></i> Training Videos</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addVideoModal">
                                <i class="bi bi-plus-lg"></i> Add
                            </button>
                        </div>
                        <div class="card-body p-0">
                            <c:choose>
                                <c:when test="${empty videos}">
                                    <div class="text-muted text-center py-4" style="font-size:0.85rem;">
                                        No videos yet. Upload an MP4 to the server and add it here.
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <table class="table table-sm table-hover mb-0" style="font-size:0.85rem;">
                                        <thead>
                                            <tr class="text-muted" style="font-size:0.78rem;">
                                                <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;">Title</th>
                                                <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;width:70px;" class="text-center">Status</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="v" items="${videos}">
                                                <tr class="video-row ${selectedVideo.id == v.id ? 'selected' : ''}"
                                                    onclick="window.location='${pageContext.request.contextPath}/ManageVideos?videoId=${v.id}'">
                                                    <td>
                                                        <div class="fw-semibold"><c:out value="${v.title}"/></div>
                                                        <div class="text-muted" style="font-size:0.75rem;">
                                                            ${v.filename}
                                                            <c:if test="${not empty v.formattedDuration}"> &middot; ${v.formattedDuration}</c:if>
                                                        </div>
                                                    </td>
                                                    <td class="text-center">
                                                        <c:choose>
                                                            <c:when test="${v.active}">
                                                                <span class="badge bg-success" style="font-size:0.7rem;">Active</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="badge bg-secondary" style="font-size:0.7rem;">Inactive</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <%-- Right panel: Selected video detail + tokens --%>
                <div class="col-md-7">
                    <c:choose>
                        <c:when test="${not empty selectedVideo}">
                            <%-- Video detail header --%>
                            <div class="card mb-3">
                                <div class="card-header d-flex justify-content-between align-items-center">
                                    <span class="fw-semibold"><c:out value="${selectedVideo.title}"/></span>
                                    <form method="post" action="${pageContext.request.contextPath}/ManageVideos" class="d-inline">
                                        <input type="hidden" name="action" value="toggleVideo">
                                        <input type="hidden" name="videoId" value="${selectedVideo.id}">
                                        <button type="submit" class="btn btn-sm ${selectedVideo.active ? 'btn-outline-warning' : 'btn-outline-success'}">
                                            ${selectedVideo.active ? 'Deactivate' : 'Activate'}
                                        </button>
                                    </form>
                                </div>
                                <div class="card-body" style="font-size:0.85rem;">
                                    <div class="row">
                                        <div class="col-6"><strong>Filename:</strong> ${selectedVideo.filename}</div>
                                        <div class="col-6"><strong>Duration:</strong> ${not empty selectedVideo.formattedDuration ? selectedVideo.formattedDuration : 'N/A'}</div>
                                    </div>
                                    <c:if test="${not empty selectedVideo.description}">
                                        <div class="mt-2 text-muted"><c:out value="${selectedVideo.description}"/></div>
                                    </c:if>
                                </div>
                            </div>

                            <%-- Generate token form --%>
                            <div class="card mb-3">
                                <div class="card-header"><i class="bi bi-key me-1"></i> Generate Token Link</div>
                                <div class="card-body">
                                    <form method="post" action="${pageContext.request.contextPath}/ManageVideos">
                                        <input type="hidden" name="action" value="generateToken">
                                        <input type="hidden" name="videoId" value="${selectedVideo.id}">
                                        <div class="row g-2" style="font-size:0.85rem;">
                                            <div class="col-md-6">
                                                <label class="form-label mb-1">Recipient Name</label>
                                                <input type="text" name="recipientName" class="form-control form-control-sm" placeholder="e.g. John Smith">
                                            </div>
                                            <div class="col-md-6">
                                                <label class="form-label mb-1">Recipient Email</label>
                                                <input type="email" name="recipientEmail" class="form-control form-control-sm" placeholder="e.g. john@example.com">
                                            </div>
                                            <div class="col-md-4">
                                                <label class="form-label mb-1">Max Views</label>
                                                <input type="number" name="maxViews" class="form-control form-control-sm" value="1" min="1" max="99">
                                            </div>
                                            <div class="col-md-4">
                                                <label class="form-label mb-1">Expires In (days)</label>
                                                <input type="number" name="expiresIn" class="form-control form-control-sm" placeholder="e.g. 7" min="1">
                                            </div>
                                            <div class="col-md-4 d-flex align-items-end">
                                                <button type="submit" class="btn btn-sm btn-ssa w-100">
                                                    <i class="bi bi-link-45deg me-1"></i>Generate Link
                                                </button>
                                            </div>
                                        </div>
                                    </form>
                                </div>
                            </div>

                            <%-- Token history --%>
                            <div class="card">
                                <div class="card-header"><i class="bi bi-list-ul me-1"></i> Token History</div>
                                <div class="card-body p-0">
                                    <c:choose>
                                        <c:when test="${empty tokens}">
                                            <div class="text-muted text-center py-3" style="font-size:0.85rem;">
                                                No tokens generated yet for this video.
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div style="max-height: 400px; overflow-y: auto;">
                                                <table class="table table-sm mb-0" style="font-size:0.8rem;">
                                                    <thead>
                                                        <tr class="text-muted" style="font-size:0.75rem;">
                                                            <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;">Recipient</th>
                                                            <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;">Created</th>
                                                            <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;" class="text-center">Views</th>
                                                            <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;">Status</th>
                                                            <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;">IP</th>
                                                            <th style="position:sticky;top:0;background:#f8f9fa;z-index:1;"></th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        <c:forEach var="tk" items="${tokens}">
                                                            <tr>
                                                                <td>
                                                                    <c:choose>
                                                                        <c:when test="${not empty tk.recipientName}">
                                                                            <c:out value="${tk.recipientName}"/>
                                                                            <c:if test="${not empty tk.recipientEmail}">
                                                                                <div class="text-muted" style="font-size:0.7rem;"><c:out value="${tk.recipientEmail}"/></div>
                                                                            </c:if>
                                                                        </c:when>
                                                                        <c:when test="${not empty tk.recipientEmail}">
                                                                            <c:out value="${tk.recipientEmail}"/>
                                                                        </c:when>
                                                                        <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                                                    </c:choose>
                                                                </td>
                                                                <td><fmt:formatDate value="${tk.createdAt}" pattern="MM/dd/yy h:mm a"/></td>
                                                                <td class="text-center">${tk.viewCount}/${tk.maxViews}</td>
                                                                <td>
                                                                    <c:set var="st" value="${tk.status}"/>
                                                                    <c:choose>
                                                                        <c:when test="${st == 'Unused'}"><span class="token-status unused">Unused</span></c:when>
                                                                        <c:when test="${st == 'Viewed'}"><span class="token-status viewed">Viewed</span></c:when>
                                                                        <c:when test="${st == 'Viewed (grace)'}"><span class="token-status grace">Grace</span></c:when>
                                                                        <c:when test="${st == 'Consumed'}"><span class="token-status consumed">Used</span></c:when>
                                                                        <c:when test="${st == 'Expired'}"><span class="token-status expired">Expired</span></c:when>
                                                                    </c:choose>
                                                                </td>
                                                                <td><c:out value="${tk.ipAddress}" default="-"/></td>
                                                                <td class="text-end text-nowrap">
                                                                    <span class="copy-btn" title="Copy link"
                                                                          onclick="navigator.clipboard.writeText('${systemUrl}/video?t=${tk.token}');this.innerHTML='<i class=\'bi bi-check\'></i>';setTimeout(()=>this.innerHTML='<i class=\'bi bi-clipboard\'></i>',1500)">
                                                                        <i class="bi bi-clipboard"></i>
                                                                    </span>
                                                                    <c:if test="${st == 'Unused' || st == 'Viewed' || st == 'Viewed (grace)'}">
                                                                        <form method="post" action="${pageContext.request.contextPath}/ManageVideos"
                                                                              class="d-inline" onsubmit="return confirm('Revoke this token?')">
                                                                            <input type="hidden" name="action" value="revokeToken">
                                                                            <input type="hidden" name="tokenId" value="${tk.id}">
                                                                            <input type="hidden" name="videoId" value="${selectedVideo.id}">
                                                                            <button type="submit" class="btn btn-link btn-sm text-danger p-0 ms-2" title="Revoke">
                                                                                <i class="bi bi-x-circle"></i>
                                                                            </button>
                                                                        </form>
                                                                    </c:if>
                                                                </td>
                                                            </tr>
                                                        </c:forEach>
                                                    </tbody>
                                                </table>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="card">
                                <div class="card-body text-center text-muted py-5">
                                    <i class="bi bi-camera-video" style="font-size:2rem;"></i>
                                    <div class="mt-2">Select a video from the list to manage its tokens.</div>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Add Video Modal --%>
<div class="modal fade" id="addVideoModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/ManageVideos">
                <input type="hidden" name="action" value="addVideo">
                <div class="modal-header">
                    <h6 class="modal-title"><i class="bi bi-plus-circle me-1"></i> Add Training Video</h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body" style="font-size:0.85rem;">
                    <div class="mb-3">
                        <label class="form-label">Filename <span class="text-danger">*</span></label>
                        <input type="text" name="filename" class="form-control form-control-sm" required
                               placeholder="e.g. getting-started.mp4">
                        <div class="form-text">Must already be uploaded to the server's video directory via SCP.</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Title <span class="text-danger">*</span></label>
                        <input type="text" name="title" class="form-control form-control-sm" required
                               placeholder="e.g. Getting Started with AMS">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Description</label>
                        <textarea name="description" class="form-control form-control-sm" rows="2"
                                  placeholder="Brief description of the video content"></textarea>
                    </div>
                    <div class="mb-0">
                        <label class="form-label">Duration (seconds)</label>
                        <input type="number" name="durationSeconds" class="form-control form-control-sm"
                               placeholder="e.g. 180 for 3 minutes" min="0">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-sm btn-ssa">Add Video</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function copyLink() {
    const el = document.getElementById('generatedLink');
    navigator.clipboard.writeText(el.textContent.trim()).then(() => {
        const btn = event.target.closest('button');
        const orig = btn.innerHTML;
        btn.innerHTML = '<i class="bi bi-check me-1"></i>Copied!';
        setTimeout(() => btn.innerHTML = orig, 1500);
    });
}
</script>
</body>
</html>
