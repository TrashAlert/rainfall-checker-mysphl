<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Import Dataset - M3</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/" class="brand"> MY &amp; PH Rainfall</a>
        <a href="${pageContext.request.contextPath}/pages/dataset_home.jsp" class="active">M3 Import &amp; Data</a>
        <a href="${pageContext.request.contextPath}/pages/analysis.jsp">M1 &amp; M2 Analysis</a>
        <a href="${pageContext.request.contextPath}/export">M4 Export</a>
    </nav>

    <div class="container">
        <div class="page-title">Module M3 — Data Management</div>
        <div class="page-subtitle">Import Malaysia or Philippines rainfall CSV datasets.</div>

        <div class="grid-2">
            <div class="card">
                <div class="card-title"> Import CSV Dataset</div>
                <p style="color:var(--text-muted); font-size:13px; margin-bottom:20px;">
                    Upload a Malaysia (MY) or Philippines (PH) CSV file.
                    Country is automatically detected from the PCODE column.
                </p>

                <form action="${pageContext.request.contextPath}/import"
                      method="POST"
                      enctype="multipart/form-data">

                    <div class="form-group">
                        <label>CSV File</label>
                        <input type="file" name="csvFile" accept=".csv" required id="fileInput">
                    </div>

                    <div class="form-group">
                        <label>Row Import Limit</label>
                        <input type="number" name="rowLimit" id="rowLimit" min="1"
                               placeholder="e.g. 500 — leave blank to import all"
                               style="width:100%;">
                        <small style="color:var(--text-muted);">
                            Leave blank to import the entire file.
                        </small>
                    </div>

                    <div id="fileInfo" style="color:var(--text-muted);font-size:12px;margin-bottom:16px;display:none;">
                        Selected: <span id="fileName"></span>
                    </div>

                    <button type="submit" class="btn btn-primary" id="importBtn">
                        Import Dataset
                    </button>
                </form>

                <div class="alert alert-info" style="margin-top:20px;">
                    <strong>Malaysia CSV</strong> — PCODE starts with MY (e.g. MY01)<br>
                    <strong>Philippines CSV</strong> — PCODE starts with PH (e.g. PH01)<br>
                    Country is detected automatically from the PCODE column.
                </div>
            </div>

            <div class="card">
                <div class="card-title"> Preprocessing &amp; Validation Rules</div>
                <table style="font-size:13px;">
                    <thead>
                        <tr><th>Rule</th><th>Condition</th><th>Action</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>Date check</td><td>date not blank</td><td><span class="badge badge-active">Accept</span></td></tr>
                        <tr><td>rfh range</td><td>rfh &ge; 0</td><td><span class="badge badge-active">Accept</span></td></tr>
                        <tr><td>Negative rfh</td><td>rfh &lt; 0</td><td><span class="badge badge-deleted">Skip row</span></td></tr>
                        <tr><td>Column count</td><td>&lt; 15 columns</td><td><span class="badge badge-deleted">Skip row</span></td></tr>
                        <tr><td>Country (MY)</td><td>PCODE starts with MY</td><td><span class="badge badge-active">Tag as MY</span></td></tr>
                        <tr><td>Country (PH)</td><td>PCODE starts with PH</td><td><span class="badge badge-active">Tag as PH</span></td></tr>
                        <tr><td>is_active</td><td>All imported records</td><td><span class="badge badge-active">Set to 1</span></td></tr>
                    </tbody>
                </table>

                <div style="margin-top:20px;">
                    <a href="${pageContext.request.contextPath}/dataset" class="btn btn-ghost">
                         Browse Existing Records &rarr;
                    </a>
                </div>
            </div>
        </div>
    </div>

    <script>
        document.getElementById('fileInput').addEventListener('change', function() {
            if (this.files.length > 0) {
                document.getElementById('fileName').textContent = this.files[0].name;
                document.getElementById('fileInfo').style.display = 'block';
            }
        });
        document.querySelector('form').addEventListener('submit', function() {
            document.getElementById('importBtn').textContent = 'Importing...';
            document.getElementById('importBtn').disabled = true;
        });
    </script>
</body>
</html>
