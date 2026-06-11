package com.rainfall.servlet;

import com.rainfall.dao.RainfallDAO;
import com.rainfall.model.RainfallRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * ExportServlet — Module M4: Report Generation and Export
 *
 * URL Mapping : /export
 * HTTP Methods: GET
 *
 * Query Parameters:
 *   action     — "page" (show UI) or "download" (generate file)
 *   format     — "csv" or "json"
 *   analysis   — "m1", "m2", or "both"
 *   threshold  — M1 alert threshold (default 50.0)
 *   multiplier — M2 anomaly multiplier (default 2.0)
 *
 * Purpose:
 *   Assembles M1 (threshold alert) and M2 (trend/anomaly) results
 *   and streams downloadable CSV or JSON reports.
 *   Logs every export to the export_log table.
 */
@WebServlet("/export")
public class ExportServlet extends HttpServlet {

    private static final double DEFAULT_THRESHOLD  = 50.0;
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private final RainfallDAO dao = new RainfallDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");
        if ("download".equals(action)) {
            handleDownload(req, resp);
        } else {
            showExportPage(req, resp);
        }
    }

    /**
     * showExportPage()
     * Loads export history and forwards to export.jsp.
     */
    private void showExportPage(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("exportLogs", dao.getExportLogs());
            req.getRequestDispatcher("/pages/export.jsp").forward(req, resp);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Error loading export page: " + e.getMessage());
        }
    }

    /**
     * handleDownload()
     *
     * Builds the report file content and streams it to the browser.
     * Sets Content-Disposition: attachment so the browser downloads it.
     * Logs the export to export_log after streaming.
     */
    private void handleDownload(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String format   = req.getParameter("format");
        String analysis = req.getParameter("analysis");

        if (format == null || (!format.equals("csv") && !format.equals("json"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid format. Use 'csv' or 'json'.");
            return;
        }
        if (analysis == null || (!analysis.equals("m1") && !analysis.equals("m2")
                                  && !analysis.equals("both"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Invalid analysis. Use 'm1', 'm2', or 'both'.");
            return;
        }

        double threshold  = DEFAULT_THRESHOLD;
        double multiplier = DEFAULT_MULTIPLIER;
        try {
            if (req.getParameter("threshold")  != null) threshold  = Double.parseDouble(req.getParameter("threshold"));
            if (req.getParameter("multiplier") != null) multiplier = Double.parseDouble(req.getParameter("multiplier"));
        } catch (NumberFormatException e) { /* use defaults */ }

        try {
            List<RainfallRecord> records = dao.getActiveRecords();
            int count = records.size();

            // Compute M1 results
            int    alertCount   = computeAlerts(records, threshold);
            double alertPct     = count > 0 ? (double) alertCount / count * 100 : 0;

            // Compute M2 results
            double[] myStats    = dao.getCountryStats("MY");
            double[] phStats    = dao.getCountryStats("PH");
            int myAnomalies     = dao.getAnomalyCount("MY", multiplier);
            int phAnomalies     = dao.getAnomalyCount("PH", multiplier);

            String content;
            String filename;

            if ("csv".equals(format)) {
                content  = buildCSV(records, analysis, threshold, multiplier,
                                    alertCount, alertPct, myStats, phStats,
                                    myAnomalies, phAnomalies);
                filename = "rainfall_report_" + analysis + ".csv";
                resp.setContentType("text/csv");
            } else {
                content  = buildJSON(records, analysis, threshold, multiplier,
                                     alertCount, alertPct, myStats, phStats,
                                     myAnomalies, phAnomalies);
                filename = "rainfall_report_" + analysis + ".json";
                resp.setContentType("application/json");
            }

            resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(content);

            dao.logExport(format.toUpperCase(), analysis.toUpperCase(), count);

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Export failed: " + e.getMessage());
        }
    }

    // ── Report Builders ───────────────────────────────────────────────────────

    /**
     * buildCSV()
     * Constructs the CSV file content.
     * Includes a summary header block then a full data table.
     */
    private String buildCSV(List<RainfallRecord> records, String analysis,
                             double threshold, double multiplier,
                             int alertCount, double alertPct,
                             double[] myStats, double[] phStats,
                             int myAnomalies, int phAnomalies) {

        StringBuilder sb = new StringBuilder();
        sb.append("# Malaysia & Philippines Rainfall Analysis Report\n");
        sb.append("# Total Active Records,").append(records.size()).append("\n");

        if (!analysis.equals("m2")) {
            sb.append("# M1 - Alert Threshold (mm),").append(threshold).append("\n");
            sb.append("# M1 - Total Alerts,").append(alertCount).append("\n");
            sb.append(String.format("# M1 - Alert Percentage,%.2f%%\n", alertPct));
        }
        if (!analysis.equals("m1")) {
            sb.append("# M2 - Anomaly Multiplier,").append(multiplier).append("\n");
            sb.append(String.format("# M2 - Malaysia Avg rfh,%.4f mm\n", myStats[0]));
            sb.append(String.format("# M2 - Malaysia Anomalies,%d\n", myAnomalies));
            sb.append(String.format("# M2 - Philippines Avg rfh,%.4f mm\n", phStats[0]));
            sb.append(String.format("# M2 - Philippines Anomalies,%d\n", phAnomalies));
        }
        sb.append("#\n");
        sb.append("id,country,date,pcode,rfh,rfh_avg,r1h,r3h,version,is_active\n");

        for (RainfallRecord r : records) {
            sb.append(r.getId()).append(",")
              .append(r.getCountry()).append(",")
              .append(r.getRecordDate()).append(",")
              .append(r.getPcode()).append(",")
              .append(String.format("%.4f", r.getRfh())).append(",")
              .append(String.format("%.4f", r.getRfhAvg())).append(",")
              .append(String.format("%.4f", r.getR1h())).append(",")
              .append(String.format("%.4f", r.getR3h())).append(",")
              .append(r.getVersion()).append(",")
              .append(r.getIsActive()).append("\n");
        }
        return sb.toString();
    }

    /**
     * buildJSON()
     * Constructs the JSON file content.
     * Includes a summary object then a records array.
     */
    private String buildJSON(List<RainfallRecord> records, String analysis,
                              double threshold, double multiplier,
                              int alertCount, double alertPct,
                              double[] myStats, double[] phStats,
                              int myAnomalies, int phAnomalies) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"report\": \"Malaysia & Philippines Rainfall Analysis\",\n");
        sb.append("  \"totalActiveRecords\": ").append(records.size()).append(",\n");
        sb.append("  \"summary\": {\n");

        if (!analysis.equals("m2")) {
            sb.append("    \"m1_threshold\": ").append(threshold).append(",\n");
            sb.append("    \"m1_alertCount\": ").append(alertCount).append(",\n");
            sb.append(String.format("    \"m1_alertPercentage\": %.2f,\n", alertPct));
        }
        if (!analysis.equals("m1")) {
            sb.append("    \"m2_multiplier\": ").append(multiplier).append(",\n");
            sb.append(String.format("    \"m2_malaysia_avgRfh\": %.4f,\n", myStats[0]));
            sb.append(String.format("    \"m2_malaysia_maxRfh\": %.4f,\n", myStats[1]));
            sb.append("    \"m2_malaysia_anomalies\": ").append(myAnomalies).append(",\n");
            sb.append(String.format("    \"m2_philippines_avgRfh\": %.4f,\n", phStats[0]));
            sb.append(String.format("    \"m2_philippines_maxRfh\": %.4f,\n", phStats[1]));
            sb.append("    \"m2_philippines_anomalies\": ").append(phAnomalies).append("\n");
        } else {
            // Remove trailing comma from last m1 line
            int idx = sb.lastIndexOf(",");
            if (idx != -1) sb.deleteCharAt(idx);
            sb.append("\n");
        }

        sb.append("  },\n");
        sb.append("  \"records\": [\n");

        for (int i = 0; i < records.size(); i++) {
            RainfallRecord r = records.get(i);
            sb.append("    {")
              .append("\"id\":").append(r.getId()).append(",")
              .append("\"country\":\"").append(r.getCountry()).append("\",")
              .append("\"date\":\"").append(r.getRecordDate()).append("\",")
              .append("\"pcode\":\"").append(r.getPcode()).append("\",")
              .append("\"rfh\":").append(String.format("%.4f", r.getRfh())).append(",")
              .append("\"rfhAvg\":").append(String.format("%.4f", r.getRfhAvg())).append(",")
              .append("\"r1h\":").append(String.format("%.4f", r.getR1h())).append(",")
              .append("\"r3h\":").append(String.format("%.4f", r.getR3h())).append(",")
              .append("\"version\":\"").append(r.getVersion()).append("\",")
              .append("\"isActive\":").append(r.getIsActive())
              .append("}");
            if (i < records.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n}\n");
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Counts records where rfh > threshold in the provided list */
    private int computeAlerts(List<RainfallRecord> records, double threshold) {
        int count = 0;
        for (RainfallRecord r : records) if (r.getRfh() > threshold) count++;
        return count;
    }
}
