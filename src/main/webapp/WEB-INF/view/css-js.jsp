<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" crossorigin="anonymous">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js" integrity="sha384-MrcW6ZMFYlzcLA8Nl+NtUVF0sA7MsXsP1UyJoMp4YLEuNSfAP+JcXn/tWtIaxVXM" crossorigin="anonymous"></script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js" integrity="sha384-0pUGZvbkm6XF6gxjEnlmuGrJXVbNuzT9qBBavbLwCsOGabYfZo0T0to5eqruptLy" crossorigin="anonymous"></script>
--%>
<%--
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.11.0/umd/popper.min.js" integrity="sha384-b/U6ypiBEHpOf/4+1nzFpr53nxSS+GLCkfwBdFNTxtclqqenISfwAzpKaMNFNmj4" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.8/dist/umd/popper.min.js" integrity="sha384-I7E8VVD/ismYTF4hNIPjVp/Zjvgyol6VFvRkX/vR+Vc4jQkC+hVqc2pM8ODewa9r" crossorigin="anonymous"></script>
--%>

<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
<link rel="icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon">

<style>
    .cke_contents {
        min-height: 300px;  /* Set a reasonable minimum height */
        height: auto !important;  /* Allow it to grow as needed */
        max-width: 100% !important;  /* Make sure it doesn’t overflow */
        width: 100% !important;  /* Ensure it fills the container */
    }
    .cke_upgrade {
        display: none !important;
    }
    .btn-ssa {background: #0d5681;border-color: #0d5681;color:white;}
    .btn-ssa:hover {background: #06357a;border-color: #06357a; color:white}
    .text-ssa {color: #0d5681;}
    .bg-ssa{background-color: #0d5681; border-color: #0d5681}
    .border-ssa{border-color:#0d5681;}
    .btn-outline-ssa{background: white;border-color: #0d5681; color:#0d5681;}
    .btn-outline-ssa:hover{background: #0d5681;color:white;}
    .btn-outline-auto{background: white; border-color: white; color:lightblue}
    .btn-outline-auto:hover{border-color: cornflowerblue; color: cornflowerblue}
    .btn-outline-cb{background: white; border-color: white; color: slategray; font-weight: bolder}
    .btn-outline-cb:hover{color:darkslategray}
    .border-auto{border-color:blue}
    .btn-outline-dark-subtle{border-color: #adb5bd; background:white; color:black}
    .btn-outline-dark-subtle:hover{background: #adb5bd;}

    .btn-outline-deleg{background: white;border-color: white; color:#3b71ca;}
    .btn-outline-deleg:hover{color:#06357a;}


    .btn-outline-qm{background: white; border-color:lightgreen; color: lightgreen}
    .btn-outline-qm:hover{border-color:lightseagreen; color:lightseagreen}
    .btn-outline-dg{background: white; border-color:white; color: orangered; font-weight: bold}
    .btn-outline-dg:hover{border-color:white; color:darkred; font-weight: bolder}

    .btn-altSsa {background:#87a948;border-color:#87a948;color:white}
    .btn-altSsa:hover{background: #198754;border-color:#198754}
    .text-altSsa {color:#87a948}
    .bg-altSsa {background-color:#87a948;border-color: #87a948}
    .border-altSsa {border-color: #87a948}
    .btn-outline-altSsa{background: white;border-color: #87a948;color:#87a948}
    .btn-outline-altSsa:hover{background: #87a948;color:white}

    .page-break-after {page-break-after: always}
    .hr-text {border: 0;line-height: 1em;position: relative;text-align: center;height: 1.5em;font-size: 14px;margin: 30px 15px;}
    .hr-text::before {content: "";background: linear-gradient(to right, transparent, white, transparent);position: absolute;left: 0;top: 50%;width: 100%;height: 1px;}
    .hr-text::after {content: attr(data-content);position: relative;padding: 0 7px;line-height: 1.5em;color: white;background-color: #1a1a1a;}

    .hr-lines{
        position: relative;
        max-width: 500px;
        margin: 10px auto;
        text-align: center;
    }

    .hr-lines:before{
        content:" ";
        height: 2px;
        width: auto;
        background: red;
        display: block;
        position: absolute;
        top: 50%;
        left: 0;
    }

    .hr-lines:after{
        content:" ";
        height: 2px;
        width: auto;
        background: red;
        display: block;
        position: absolute;
        top: 50%;
        right: 0;
    }

    .accordion-button-1 {
        background-color: gold;
    }

    .accordion-button-1:not(.collapsed) {
        font-weight: bolder;
        background-color: black;
        color: gold;
    }

</style>


