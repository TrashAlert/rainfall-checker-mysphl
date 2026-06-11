<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Analysis - M1 &amp; M2</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
    <style>
        .tab-bar { display:flex; gap:4px; border-bottom:1px solid var(--border); margin-bottom:28px; }
        .tab-btn { padding:10px 20px; background:none; border:none; color:var(--text-muted); font-size:13px; cursor:pointer; border-bottom:2px solid transparent; margin-bottom:-1px; transition:color 0.2s; font-family:var(--font-body); }
        .tab-btn.active { color:var(--accent); border-bottom-color:var(--accent); }
        .tab-content { display:none; }
        .tab-content.active { display:block; }
        .mode-row { display:flex; gap:12px; margin-bottom:20px; flex-wrap:wrap; }
        .progress-info { font-size:12px; color:var(--text-muted); font-family:var(--font-mono); margin-top:8px; }
        .comparison-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; margin-bottom:20px; }
        .country-card { background:var(--surface2); border:1px solid var(--border); border-radius:var(--radius); padding:16px; }
        .country-card h4 { font-size:13px; font-weight:600; margin-bottom:12px; }
        .country-card .stat-row { display:flex; justify-content:space-between; font-size:12px; padding:4px 0; border-bottom:1px solid var(--border); }
        .country-card .stat-row:last-child { border-bottom:none; }
        .my-color { color:#58a6ff; }
        .ph-color { color:#3fb950; }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="<%=request.getContextPath()%>/" class="brand"> MY &amp; PH Rainfall</a>
        <a href="<%=request.getContextPath()%>/pages/dataset_home.jsp">M3 Import &amp; Data</a>
        <a href="<%=request.getContextPath()%>/pages/analysis.jsp" class="active">M1 &amp; M2 Analysis</a>
        <a href="<%=request.getContextPath()%>/export">M4 Export</a>
    </nav>

    <div class="container">
        <div class="page-title">Analysis Control Panel</div>
        <div class="page-subtitle">All analysis runs on active records only (is_active = 1). Soft-deleted records are excluded automatically.</div>

        <div class="tab-bar">
            <button class="tab-btn active" onclick="switchTab('m1', this)">M1 - Rainfall Threshold Alert</button>
            <button class="tab-btn" onclick="switchTab('m2', this)">M2 - Trend &amp; Anomaly Detection</button>
        </div>

        <!-- ═══ M1 TAB ══════════════════════════════════════════════════ -->
        <div id="tab-m1" class="tab-content active">

            <div class="card" style="margin-bottom:20px; padding:16px 24px;">
                <div style="display:flex; align-items:center; gap:24px; flex-wrap:wrap;">
                    <div>
                        <label style="margin:0 8px 0 0; white-space:nowrap;">Alert Threshold (mm):</label>
                        <input type="number" id="m1Threshold" value="50" min="0" step="0.1" style="width:100px;">
                    </div>
                    <div>
                        <label style="margin:0 8px 0 0; white-space:nowrap;">Low Severity (mm):</label>
                        <input type="number" id="m1LowThresh" value="30" min="0" step="0.1" style="width:100px;">
                    </div>
                    <div>
                        <label style="margin:0 8px 0 0; white-space:nowrap;">High Severity (mm):</label>
                        <input type="number" id="m1HighThresh" value="50" min="0" step="0.1" style="width:100px;">
                    </div>
                </div>
                <small style="color:var(--text-muted); margin-top:8px; display:block;">
                    Alert Threshold: records with rfh above this value are flagged.
                    Low/High Severity: define Normal / Moderate / Heavy rainfall bands.
                </small>
            </div>

            <div class="mode-row">
                <button class="btn btn-primary" onclick="runM1Batch()"> Run Batch Analysis</button>
                <button class="btn btn-success" id="m1RtBtn" onclick="toggleM1Realtime()"> Start Real-Time Stream</button>
            </div>

            <!-- M1 Batch Results -->
            <div id="m1BatchResult" style="display:none;">
                <div class="stat-grid">
                    <div class="stat-card">
                        <div class="stat-value" style="color:var(--danger);" id="m1AlertCount">-</div>
                        <div class="stat-label">Alert Records</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value" id="m1TotalCount">-</div>
                        <div class="stat-label">Active Records</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value" style="color:var(--warning);" id="m1AlertPct">-</div>
                        <div class="stat-label">% Alerts</div>
                    </div>
                </div>
                <div id="m1ThresholdUsed" class="alert alert-info" style="display:none;"></div>

                <!-- Severity Summary -->
                <div class="grid-2" style="margin-top:16px;">
                    <div class="card">
                        <div class="card-title"> Severity Summary</div>
                        <table style="font-size:13px; width:100%;">
                            <thead><tr><th>Level</th><th>Records</th></tr></thead>
                            <tbody>
                                <tr><td>Normal</td><td id="sevNormal">-</td></tr>
                                <tr><td style="color:var(--warning);">Moderate</td><td id="sevModerate">-</td></tr>
                                <tr><td style="color:var(--danger);">Heavy</td><td id="sevHeavy">-</td></tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="card">
                        <div class="card-title"> Top Alert Locations</div>
                        <div id="topLocations" style="font-size:12px; font-family:var(--font-mono);">
                            Run batch analysis to see top locations.
                        </div>
                    </div>
                </div>
            </div>

            <div class="grid-2" style="margin-top:20px;">
                <div class="card">
                    <div class="card-title"> Live Alert Counter</div>
                    <div class="violation-counter" id="m1LiveCount">0</div>
                    <div class="progress-info" id="m1Progress">Press Start Real-Time Stream to begin</div>
                    <div class="progress-bar-wrap">
                        <div class="progress-bar-fill" id="m1ProgressBar" style="width:0%; background:var(--danger);"></div>
                    </div>
                </div>
                <div class="card">
                    <div class="card-title"> Alert Stream Log</div>
                    <div class="stream-log" id="m1Log">
                        <div style="color:var(--text-muted);">Waiting for stream...</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ═══ M2 TAB ══════════════════════════════════════════════════ -->
        <div id="tab-m2" class="tab-content">

            <div class="card" style="margin-bottom:20px; padding:16px 24px;">
                <div style="display:flex; align-items:center; gap:16px; flex-wrap:wrap;">
                    <label style="margin:0; white-space:nowrap;">Anomaly Multiplier:</label>
                    <input type="number" id="m2Multiplier" value="2.0" min="0.1" step="0.1" style="width:100px;">
                    <small style="color:var(--text-muted);">
                        A record is anomalous when rfh &gt; (rfh_avg &times; multiplier).
                        Higher values = stricter anomaly detection.
                    </small>
                </div>
            </div>

            <div class="mode-row">
                <button class="btn btn-primary" onclick="runM2Batch()"> Run Batch Analysis</button>
                <button class="btn btn-success" id="m2RtBtn" onclick="toggleM2Realtime()"> Start Real-Time Stream</button>
            </div>

            <!-- M2 Batch Results -->
            <div id="m2BatchResult" style="display:none;">
                <div class="comparison-grid">
                    <div class="country-card">
                        <h4 class="my-color"> Malaysia (MY)</h4>
                        <div class="stat-row"><span>Average rfh</span><span id="myAvg">-</span></div>
                        <div class="stat-row"><span>Max rfh</span><span id="myMax">-</span></div>
                        <div class="stat-row"><span>Min rfh</span><span id="myMin">-</span></div>
                        <div class="stat-row"><span>Total Records</span><span id="myCount">-</span></div>
                        <div class="stat-row"><span style="color:var(--danger);">Anomaly Spikes</span><span id="myAnomalies" style="color:var(--danger);">-</span></div>
                    </div>
                    <div class="country-card">
                        <h4 class="ph-color"> Philippines (PH)</h4>
                        <div class="stat-row"><span>Average rfh</span><span id="phAvg">-</span></div>
                        <div class="stat-row"><span>Max rfh</span><span id="phMax">-</span></div>
                        <div class="stat-row"><span>Min rfh</span><span id="phMin">-</span></div>
                        <div class="stat-row"><span>Total Records</span><span id="phCount">-</span></div>
                        <div class="stat-row"><span style="color:var(--danger);">Anomaly Spikes</span><span id="phAnomalies" style="color:var(--danger);">-</span></div>
                    </div>
                </div>
                <div id="m2ComparisonMsg" class="alert alert-info" style="display:none;"></div>
            </div>

            <!-- M2 Real-Time Panels -->
            <div class="grid-2" style="margin-top:20px;">
                <div class="card">
                    <div class="card-title"> Live Moving Average &amp; Highest-So-Far</div>
                    <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:12px;">
                        <div style="text-align:center;">
                            <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px;">MY Moving Avg</div>
                            <div style="font-size:22px; font-weight:700; color:#58a6ff; font-family:var(--font-mono);" id="myMovingAvg">-</div>
                        </div>
                        <div style="text-align:center;">
                            <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px;">MY Highest So Far</div>
                            <div style="font-size:22px; font-weight:700; color:#58a6ff; font-family:var(--font-mono);" id="myHighest">-</div>
                        </div>
                        <div style="text-align:center;">
                            <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px;">PH Moving Avg</div>
                            <div style="font-size:22px; font-weight:700; color:#3fb950; font-family:var(--font-mono);" id="phMovingAvg">-</div>
                        </div>
                        <div style="text-align:center;">
                            <div style="font-size:11px; color:var(--text-muted); margin-bottom:4px;">PH Highest So Far</div>
                            <div style="font-size:22px; font-weight:700; color:#3fb950; font-family:var(--font-mono);" id="phHighest">-</div>
                        </div>
                    </div>
                    <div class="progress-info" id="m2Progress">Press Start Real-Time Stream to begin</div>
                </div>
                <div class="card">
                    <div class="card-title"> Trend &amp; Anomaly Stream Log</div>
                    <div class="stream-log" id="m2Log">
                        <div style="color:var(--text-muted);">Waiting for stream...</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <% String ctx = request.getContextPath(); %>
    <script>
        var CTX = '<%= ctx %>';

        function switchTab(tabId, btnEl) {
            document.querySelectorAll('.tab-content').forEach(function(t) { t.classList.remove('active'); });
            document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
            document.getElementById('tab-' + tabId).classList.add('active');
            btnEl.classList.add('active');
        }

        var urlTab = new URLSearchParams(window.location.search).get('tab');
        if (urlTab === 'm2') { document.querySelectorAll('.tab-btn')[1].click(); }

        /* ═══════════════════════════════════════════════════════════
         * M1 — Rainfall Threshold Alert Analysis
         * ═══════════════════════════════════════════════════════════
         *
         * runM1Batch()
         * Fetches GET /analysis/m1?mode=batch with threshold + severity params.
         * Displays alert count, severity summary, and top alert locations.
         */
        function runM1Batch() {
            var threshold  = parseFloat(document.getElementById('m1Threshold').value);
            var lowThresh  = parseFloat(document.getElementById('m1LowThresh').value);
            var highThresh = parseFloat(document.getElementById('m1HighThresh').value);

            if (isNaN(threshold) || threshold < 0) { alert('Enter a valid threshold value.'); return; }

            var btn = document.querySelector('[onclick="runM1Batch()"]');
            btn.textContent = 'Running...';
            btn.disabled = true;

            fetch(CTX + '/analysis/m1?mode=batch&threshold=' + threshold +
                  '&lowThresh=' + lowThresh + '&highThresh=' + highThresh)
                .then(function(r) { if (!r.ok) throw new Error('Server error: ' + r.status); return r.json(); })
                .then(function(d) {
                    document.getElementById('m1AlertCount').textContent = d.alertCount.toLocaleString();
                    document.getElementById('m1TotalCount').textContent = d.totalRecords.toLocaleString();
                    document.getElementById('m1AlertPct').textContent   = d.alertPercentage.toFixed(2) + '%';
                    document.getElementById('m1BatchResult').style.display = 'block';

                    var used = document.getElementById('m1ThresholdUsed');
                    used.textContent   = 'Results for alert threshold: rfh > ' + d.threshold + ' mm';
                    used.style.display = 'block';

                    /* Severity summary */
                    document.getElementById('sevNormal').textContent   = d.severity.Normal.toLocaleString();
                    document.getElementById('sevModerate').textContent = d.severity.Moderate.toLocaleString();
                    document.getElementById('sevHeavy').textContent    = d.severity.Heavy.toLocaleString();

                    /* Top locations table */
                    var html = '<table style="width:100%;font-size:12px;">';
                    html += '<tr><th style="text-align:left;">PCODE</th><th>Country</th><th>Alerts</th><th>Max rfh</th></tr>';
                    d.topLocations.forEach(function(loc) {
                        html += '<tr><td>' + loc.pcode + '</td><td>' + loc.country +
                                '</td><td>' + loc.count + '</td><td>' + loc.maxRfh + ' mm</td></tr>';
                    });
                    html += '</table>';
                    document.getElementById('topLocations').innerHTML = html;
                })
                .catch(function(e) { alert('M1 Batch failed: ' + e.message); })
                .finally(function() { btn.textContent = 'Run Batch Analysis'; btn.disabled = false; });
        }

        var m1Source = null, m1Running = false;

        function toggleM1Realtime() { if (m1Running) { stopM1(); } else { startM1(); } }

        /*
         * startM1()
         * Opens SSE to /analysis/m1?mode=realtime&threshold=X.
         * Each event contains isAlert flag and running alert count.
         * Frontend highlights alert records in red.
         */
        function startM1() {
            m1Running = true;
            document.getElementById('m1RtBtn').textContent = 'Stop Stream';
            document.getElementById('m1RtBtn').className   = 'btn btn-danger';
            document.getElementById('m1Log').innerHTML     = '';
            document.getElementById('m1LiveCount').textContent = '0';

            var threshold = parseFloat(document.getElementById('m1Threshold').value);
            if (isNaN(threshold) || threshold < 0) { alert('Enter a valid threshold.'); stopM1(); return; }

            appendLog('m1Log', 'Starting stream - alert threshold: rfh > ' + threshold + ' mm', 'log-done');

            m1Source = new EventSource(CTX + '/analysis/m1?mode=realtime&threshold=' + threshold);

            m1Source.onmessage = function(e) {
                try {
                    var d = JSON.parse(e.data);
                    if (d.done) {
                        appendLog('m1Log', 'Done. ' + d.total + ' records checked. ' + d.alertCount + ' alerts detected.', 'log-done');
                        document.getElementById('m1Progress').textContent = 'Complete - ' + d.alertCount + ' alerts found';
                        document.getElementById('m1ProgressBar').style.width = '100%';
                        stopM1(); return;
                    }
                    if (d.error) { appendLog('m1Log', 'Error: ' + d.error, 'log-violation'); stopM1(); return; }

                    document.getElementById('m1LiveCount').textContent = d.alertCount;
                    document.getElementById('m1Progress').textContent  =
                        'Record ' + d.seq + ' | ' + d.country + ' | ' + d.pcode + ' | rfh: ' + d.rfh.toFixed(2) + ' mm';

                    if (d.seq <= 200) {
                        if (d.isAlert) {
                            appendLog('m1Log',
                                '[' + d.seq + '] ' + d.country + ' | ' + d.pcode + ' | ' + d.date +
                                ' | rfh=' + d.rfh.toFixed(2) + ' > ' + d.threshold + ' ALERT',
                                'log-violation');
                        } else if (d.seq % 15 === 0) {
                            appendLog('m1Log',
                                '[' + d.seq + '] ' + d.country + ' | ' + d.pcode +
                                ' | rfh=' + d.rfh.toFixed(2) + ' ok', 'log-normal');
                        }
                    }
                } catch(ex) {}
            };

            m1Source.onerror = function() { appendLog('m1Log', 'Connection error.', 'log-violation'); stopM1(); };
        }

        function stopM1() {
            m1Running = false;
            if (m1Source) { m1Source.close(); m1Source = null; }
            document.getElementById('m1RtBtn').textContent = 'Start Real-Time Stream';
            document.getElementById('m1RtBtn').className   = 'btn btn-success';
        }

        /* ═══════════════════════════════════════════════════════════
         * M2 — Rainfall Trend and Anomaly Detection
         * ═══════════════════════════════════════════════════════════
         *
         * runM2Batch()
         * Fetches GET /analysis/m2?mode=batch&multiplier=X.
         * Displays side-by-side Malaysia vs Philippines comparison.
         */
        function runM2Batch() {
            var multiplier = parseFloat(document.getElementById('m2Multiplier').value);
            if (isNaN(multiplier) || multiplier <= 0) { alert('Multiplier must be greater than 0.'); return; }

            var btn = document.querySelector('[onclick="runM2Batch()"]');
            btn.textContent = 'Running...';
            btn.disabled = true;

            fetch(CTX + '/analysis/m2?mode=batch&multiplier=' + multiplier)
                .then(function(r) { if (!r.ok) throw new Error('Server error: ' + r.status); return r.json(); })
                .then(function(d) {
                    /* Malaysia stats */
                    document.getElementById('myAvg').textContent       = d.malaysia.avg.toFixed(4) + ' mm';
                    document.getElementById('myMax').textContent       = d.malaysia.max.toFixed(4) + ' mm';
                    document.getElementById('myMin').textContent       = d.malaysia.min.toFixed(4) + ' mm';
                    document.getElementById('myCount').textContent     = d.malaysia.count.toLocaleString();
                    document.getElementById('myAnomalies').textContent = d.malaysia.anomalyCount.toLocaleString();

                    /* Philippines stats */
                    document.getElementById('phAvg').textContent       = d.philippines.avg.toFixed(4) + ' mm';
                    document.getElementById('phMax').textContent       = d.philippines.max.toFixed(4) + ' mm';
                    document.getElementById('phMin').textContent       = d.philippines.min.toFixed(4) + ' mm';
                    document.getElementById('phCount').textContent     = d.philippines.count.toLocaleString();
                    document.getElementById('phAnomalies').textContent = d.philippines.anomalyCount.toLocaleString();

                    document.getElementById('m2BatchResult').style.display = 'block';

                    /* Comparison message */
                    var msg = document.getElementById('m2ComparisonMsg');
                    msg.textContent = d.comparison.higherAvgCountry + ' has a higher average rainfall. ' +
                                      d.comparison.higherMaxCountry + ' recorded the highest single rfh value. ' +
                                      'Total anomaly spikes detected: ' + d.comparison.totalAnomaly +
                                      ' (multiplier: ' + d.multiplier + 'x)';
                    msg.style.display = 'block';
                })
                .catch(function(e) { alert('M2 Batch failed: ' + e.message); })
                .finally(function() { btn.textContent = 'Run Batch Analysis'; btn.disabled = false; });
        }

        var m2Source = null, m2Running = false;

        function toggleM2Realtime() { if (m2Running) { stopM2(); } else { startM2(); } }

        /*
         * startM2()
         * Opens SSE to /analysis/m2?mode=realtime&multiplier=X.
         * Each event contains per-country moving average, highest-so-far,
         * and a suddenChange flag for spike detection.
         */
        function startM2() {
            m2Running = true;
            document.getElementById('m2RtBtn').textContent = 'Stop Stream';
            document.getElementById('m2RtBtn').className   = 'btn btn-danger';
            document.getElementById('m2Log').innerHTML     = '';

            var multiplier = parseFloat(document.getElementById('m2Multiplier').value);
            if (isNaN(multiplier) || multiplier <= 0) { alert('Enter a valid multiplier.'); stopM2(); return; }

            appendLog('m2Log', 'Starting stream - anomaly multiplier: ' + multiplier + 'x', 'log-done');

            m2Source = new EventSource(CTX + '/analysis/m2?mode=realtime&multiplier=' + multiplier);

            m2Source.onmessage = function(e) {
                try {
                    var d = JSON.parse(e.data);

                    if (d.done) {
                        appendLog('m2Log',
                            'Done. ' + d.total + ' records processed. ' +
                            'MY final avg: ' + d.myFinalAvg.toFixed(2) + ' mm | ' +
                            'PH final avg: ' + d.phFinalAvg.toFixed(2) + ' mm', 'log-done');
                        document.getElementById('m2Progress').textContent = 'Stream complete';
                        stopM2(); return;
                    }
                    if (d.error) { appendLog('m2Log', 'Error: ' + d.error, 'log-violation'); stopM2(); return; }

                    /* Update live moving averages and highest-so-far */
                    document.getElementById('myMovingAvg').textContent = d.myMovingAvg > 0 ? d.myMovingAvg.toFixed(4) + ' mm' : '-';
                    document.getElementById('myHighest').textContent   = d.myHighest   > 0 ? d.myHighest.toFixed(4)   + ' mm' : '-';
                    document.getElementById('phMovingAvg').textContent = d.phMovingAvg > 0 ? d.phMovingAvg.toFixed(4) + ' mm' : '-';
                    document.getElementById('phHighest').textContent   = d.phHighest   > 0 ? d.phHighest.toFixed(4)   + ' mm' : '-';

                    document.getElementById('m2Progress').textContent =
                        'Record ' + d.seq + ' | ' + d.country + ' | ' + d.pcode + ' | rfh: ' + d.rfh.toFixed(2) + ' mm';

                    /* Log sudden changes prominently, every 15th normal record quietly */
                    if (d.seq <= 300) {
                        if (d.suddenChange) {
                            appendLog('m2Log',
                                '[' + d.seq + '] ' + d.country + ' | ' + d.pcode +
                                ' | rfh=' + d.rfh.toFixed(2) + ' SUDDEN CHANGE DETECTED',
                                'log-violation');
                        } else if (d.seq % 15 === 0) {
                            appendLog('m2Log',
                                '[' + d.seq + '] ' + d.country + ' | ' + d.pcode +
                                ' | rfh=' + d.rfh.toFixed(2) + ' mm', 'log-normal');
                        }
                    }
                } catch(ex) {}
            };

            m2Source.onerror = function() { appendLog('m2Log', 'Connection error.', 'log-violation'); stopM2(); };
        }

        function stopM2() {
            m2Running = false;
            if (m2Source) { m2Source.close(); m2Source = null; }
            document.getElementById('m2RtBtn').textContent = 'Start Real-Time Stream';
            document.getElementById('m2RtBtn').className   = 'btn btn-success';
        }

        /*
         * appendLog(logId, message, cssClass)
         * Adds a line to a stream-log div and auto-scrolls to bottom.
         */
        function appendLog(logId, message, cssClass) {
            var log = document.getElementById(logId);
            var row = document.createElement('div');
            row.className   = 'log-row ' + (cssClass || '');
            row.textContent = message;
            log.appendChild(row);
            log.scrollTop   = log.scrollHeight;
        }
    </script>
</body>
</html>
