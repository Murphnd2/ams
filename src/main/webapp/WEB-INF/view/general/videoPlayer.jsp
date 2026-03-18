<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${videoTitle} - SSA Training</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        :root { --ssa: #0d5681; --ssa-alt: #87a948; }
        body {
            font-family: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #f0f2f5;
            margin: 0;
            padding: 0;
        }
        .video-header {
            background: var(--ssa);
            color: white;
            padding: 16px 24px;
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .video-header img {
            height: 36px;
        }
        .video-header h1 {
            font-size: 18px;
            font-weight: 600;
            margin: 0;
        }
        .video-container {
            max-width: 960px;
            margin: 32px auto;
            padding: 0 16px;
        }
        .video-card {
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .video-card video {
            width: 100%;
            display: block;
            background: #000;
        }
        .video-info {
            padding: 20px 24px;
        }
        .video-info h2 {
            font-size: 20px;
            font-weight: 600;
            color: #333;
            margin: 0 0 8px;
        }
        .video-info p {
            color: #666;
            margin: 0;
            line-height: 1.5;
        }
        .video-notice {
            text-align: center;
            margin-top: 16px;
            color: #999;
            font-size: 13px;
        }
        .video-notice i {
            margin-right: 4px;
        }
    </style>
</head>
<body>

<div class="video-header">
    <i class="bi bi-play-circle-fill" style="font-size: 28px;"></i>
    <h1>SSA Training Video</h1>
</div>

<div class="video-container">
    <div class="video-card">
        <video controls preload="metadata" controlslist="nodownload"
               oncontextmenu="return false;">
            <source src="${pageContext.request.contextPath}/video?t=${tokenParam}&stream=1" type="video/mp4">
            Your browser does not support HTML5 video.
        </video>
        <div class="video-info">
            <h2><c:out value="${videoTitle}"/></h2>
            <c:if test="${not empty videoDescription}">
                <p><c:out value="${videoDescription}"/></p>
            </c:if>
        </div>
    </div>
    <div class="video-notice">
        <i class="bi bi-shield-lock"></i>
        This is a single-use video link. It will expire after viewing.
    </div>
</div>

<script>
(function() {
    var recorded = false;
    var video = document.querySelector('video');
    if (video) {
        video.addEventListener('play', function() {
            if (recorded) return;
            recorded = true;
            fetch('${pageContext.request.contextPath}/video?t=${tokenParam}&action=recordView', {
                method: 'POST'
            });
        });
    }
})();
</script>

</body>
</html>
