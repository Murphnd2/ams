<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>§125 Eligibility Test – Submitted</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
          crossorigin="anonymous">
</head>
<body class="bg-light">

<div class="container" style="max-width: 720px; margin-top: 60px;">
    <div class="card shadow-sm">
        <div class="card-body p-4">
            <h2 class="card-title text-success mb-3">Thank You!</h2>
            <p class="card-text">
                Your §125 Eligibility Test has been submitted successfully.
            </p>
            <p class="card-text">
                If you have any questions or need to provide corrections, please contact our office.
            </p>
            <div class="mt-4">
                <a href="<%=request.getContextPath()%>/" class="btn btn-primary">
                    Return to Home
                </a>
            </div>
        </div>
    </div>
</div>

</body>
</html>

