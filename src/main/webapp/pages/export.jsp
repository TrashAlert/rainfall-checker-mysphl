<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Export Reports - M4</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/" class="brand"> MY &amp; PH Rainfall</a>
        <a href="${pageContext.request.contextPath}/pages/dataset_home.jsp">M3 Import &amp; Data</a>
        <a href="${pageContext.request.contextPath}/pages/analysis.jsp">M1 &amp; M2 Analysis</a>
        <a href="${pageContext.request.contextPath}/export" class="active">M4 Export</a>
    </nav>

    <div class="container">
        <div class="page-title">Module M4 - Report Generation &amp; Export</div>
        <div class="page-subtitle">Generate downloadable reports from M1 (Threshold Alert) and M2 (Trend &amp; Anomaly) results.</div>

        <div class="grid-2">
            <div class="card">
                <div class="card-title"> Generate Report</div>

                <div class="form-group">
                    <label>Analysis to Export</label>
                    <select id="analysisSelect">
                        <option value="both">Both M1 (Alert) + M2 (Trend/Anomaly)</option>
                        <option value="m1">M1 only - Threshold Alert Analysis</option>
                        <option value="m2">M2 only - Trend &amp; Anomaly Detection</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>M1 Alert Threshold (mm)</label>
                    <input type="number" id="thresholdInput" value="50" min="0" step="0.1">
                </div>

                <div class="form-group">
                    <label>M2 Anomaly Multiplier</label>
                    <input type="number" id="multiplierInput" value="2.0" min="0.1" step="0.1">
                    <small style="color:var(--text-muted);">Anomaly when rfh &gt; rfh_avg &times; multiplier</small>
                </div>

                <div style="display:flex; gap:12px; margin-top:8px; flex-wrap:wrap;">
                    <button class="btn btn-primary" onclick="downloadReport('csv')"> Download CSV</button>
                    <button class="btn btn-success" onclick="downloadReport('json')"> Download JSON</button>
                </div>

                <div id="downloadMsg" class="alert alert-success" style="display:none; margin-top:16px;">
                    Download started! Page will refresh shortly to update export history.
                </div>
            </div>

            <div class="card">
                <div class="card-title"> What's Included</div>
                <table style="font-size:13px; width:100%;">
                    <thead><tr><th>Module</th><th>Metric</th></tr></thead>
                    <tbody>
                        <tr><td><span class="badge badge-active">M1</span></td><td>Total alert count, alert % (rfh &gt; threshold)</td></tr>
                        <tr><td><span class="badge badge-active">M2</span></td><td>Malaysia vs Philippines avg, max, min rfh</td></tr>
                        <tr><td><span class="badge badge-active">M2</span></td><td>Anomaly spike count per country</td></tr>
                        <tr><td>Data</td><td>All active records with country tag (MY/PH)</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card">
            <div class="card-title"> Export History</div>
            <c:choose>
                <c:when test="${empty exportLogs}">
                    <div style="color:var(--text-muted);font-size:13px;padding:20px 0;text-align:center;">
                        No exports yet. Generate your first report above.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr><th>#</th><th>Format</th><th>Analysis</th><th>Timestamp</th><th>Records</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="log" items="${exportLogs}" varStatus="st">
                                    <tr>
                                        <td>${st.count}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${log[0] == 'CSV'}"><span class="badge badge-active">CSV</span></c:when>
                                                <c:otherwise><span class="badge badge-ok">JSON</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${log[1]}</td>
                                        <td>${log[2]}</td>
                                        <td>${log[3]}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <a id="downloadAnchor" style="display:none;"></a>
    <% String ctx = request.getContextPath(); %>
    <script>
        var CTX = '<%= ctx %>';

        function downloadReport(format) {
            var analysis   = document.getElementById('analysisSelect').value;
            var threshold  = document.getElementById('thresholdInput').value  || '50';
            var multiplier = document.getElementById('multiplierInput').value || '2.0';

            var url = CTX + '/export?action=download'
                    + '&format='     + format
                    + '&analysis='   + analysis
                    + '&threshold='  + threshold
                    + '&multiplier=' + multiplier;

            var anchor = document.getElementById('downloadAnchor');
            anchor.href = url;
            anchor.click();

            document.getElementById('downloadMsg').style.display = 'block';
            setTimeout(function() { window.location.href = CTX + '/export'; }, 2000);
        }
    </script>
</body>
</html>
