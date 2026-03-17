<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Video Link Expired - SSA Training</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        :root { --ssa: #0d5681; }
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
        .video-header h1 {
            font-size: 18px;
            font-weight: 600;
            margin: 0;
        }
        .expired-container {
            max-width: 500px;
            margin: 80px auto;
            text-align: center;
            padding: 0 16px;
        }
        .expired-card {
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            padding: 48px 32px;
        }
        .expired-icon {
            font-size: 48px;
            color: #ccc;
            margin-bottom: 16px;
        }
        .expired-card h2 {
            font-size: 20px;
            color: #333;
            margin: 0 0 12px;
        }
        .expired-card p {
            color: #666;
            margin: 0;
            line-height: 1.5;
        }
    </style>
</head>
<body>

<div class="video-header">
    <i class="bi bi-play-circle-fill" style="font-size: 28px;"></i>
    <h1>SSA Training Video</h1>
</div>

<div class="expired-container">
    <div class="expired-card">
        <div class="expired-icon">
            <i class="bi bi-shield-x"></i>
        </div>
        <h2>Video Link Unavailable</h2>
        <p><c:out value="${errorMessage}" default="This video link has expired or has already been viewed."/></p>
        <p style="margin-top: 16px; font-size: 13px; color: #999;">
            If you need access to this video, please contact your administrator for a new link.
        </p>
    </div>
</div>

</body>
</html>
