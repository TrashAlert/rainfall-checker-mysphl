<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Malaysia &amp; Philippines Rainfall Analysis</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .hero { text-align:center; padding:60px 0 40px; }
        .hero h1 { font-family:var(--font-mono); font-size:32px; font-weight:700; color:var(--accent); margin-bottom:10px; }
        .hero p { color:var(--text-muted); font-size:15px; }
        .module-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(250px,1fr)); gap:20px; margin-top:40px; }
        .module-card { background:var(--surface); border:1px solid var(--border); border-radius:var(--radius); padding:28px 24px; text-decoration:none; color:var(--text); transition:border-color 0.2s,transform 0.15s; display:block; }
        .module-card:hover { border-color:var(--accent); transform:translateY(-3px); }
        .module-tag { font-family:var(--font-mono); font-size:11px; font-weight:700; color:var(--accent); text-transform:uppercase; letter-spacing:0.1em; margin-bottom:10px; }
        .module-title { font-size:16px; font-weight:600; margin-bottom:8px; }
        .module-desc { font-size:12px; color:var(--text-muted); line-height:1.6; }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/" class="brand">&#127783; MY &amp; PH Rainfall</a>
        <a href="${pageContext.request.contextPath}/pages/dataset_home.jsp">M3 Import &amp; Data</a>
        <a href="${pageContext.request.contextPath}/pages/analysis.jsp">M1 &amp; M2 Analysis</a>
        <a href="${pageContext.request.contextPath}/export">M4 Export</a>
    </nav>

    <div class="container">
        <div class="hero">
            <h1>Malaysia &amp; Philippines Rainfall Analysis</h1>
            <p>BITS 3515 TCP/IP Programming &mdash; Mini Project 2</p>
            <p style="margin-top:8px; font-size:12px; color:var(--text-muted);">
                Datasets: Malaysia Sub-National Rainfall &nbsp;|&nbsp; Philippines Sub-National Rainfall
                &nbsp;|&nbsp; Analysis field: rfh (mm)
            </p>
        </div>

        <div class="module-grid">
            <a href="${pageContext.request.contextPath}/pages/analysis.jsp?tab=m1" class="module-card">
                <div class="module-tag">M1 &mdash; Analysis A</div>
                <div class="module-title">Rainfall Threshold Alert Analysis</div>
                <div class="module-desc">
                    Detects records exceeding dangerous rainfall thresholds.
                    Shows top alert locations, severity summary, and live alert counter via SSE.
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/pages/analysis.jsp?tab=m2" class="module-card">
                <div class="module-tag">M2 &mdash; Analysis B</div>
                <div class="module-title">Trend &amp; Anomaly Detection</div>
                <div class="module-desc">
                    Compares Malaysia vs Philippines rainfall trends.
                    Detects anomaly spikes and tracks moving average and highest-so-far live via SSE.
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/pages/dataset_home.jsp" class="module-card">
                <div class="module-tag">M3 &mdash; Data Management</div>
                <div class="module-title">Import, Browse &amp; Manage</div>
                <div class="module-desc">
                    Upload Malaysia or Philippines CSV. Auto-detects country from PCODE.
                    Browse, edit, soft-delete, reinstate, or clear all records.
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/export" class="module-card">
                <div class="module-tag">M4 &mdash; Export &amp; Reports</div>
                <div class="module-title">Report Generation</div>
                <div class="module-desc">
                    Export M1 alert and M2 trend/anomaly results as CSV or JSON.
                    Maintains full export history with timestamps.
                </div>
            </a>
        </div>
    </div>
</body>
</html>
