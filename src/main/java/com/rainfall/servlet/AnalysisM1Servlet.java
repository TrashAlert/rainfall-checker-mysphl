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
 * AnalysisM1Servlet — Module M1: Rainfall Threshold Alert Analysis
 *
 * URL Mapping : /analysis/m1
 * HTTP Method : GET
 *
 * Query Parameters:
 *   mode      — "batch" or "realtime"
 *   threshold — alert threshold in mm (default 50.0)
 *   lowThresh — lower severity boundary (default 30.0)
 *   highThresh— upper severity boundary (default 50.0)
 *
 * Purpose:
 *   Simulates an early warning rainfall monitoring system that identifies
 *   potential flood-risk situations by detecting records exceeding thresholds.
 *
 * BATCH MODE:
 *   - Counts total records exceeding threshold (all active, both countries)
 *   - Returns top 10 locations with most alerts (PCODE, country, count, max rfh)
 *   - Returns severity summary: Normal / Moderate / Heavy counts
 *
 * REAL-TIME MODE (SSE):
 *   - Streams active records one-by-one
 *   - Flags each record as alert or normal
 *   - Frontend updates live alert counter and warning panel
 *
 * All queries filter by is_active = 1 (mandatory).
 */
@WebServlet("/analysis/m1")
public class AnalysisM1Servlet extends HttpServlet {

    private static final double DEFAULT_THRESHOLD  = 50.0;
    private static final double DEFAULT_LOW_THRESH = 30.0;
    private static final double DEFAULT_HIGH_THRESH= 50.0;

    private final RainfallDAO dao = new RainfallDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String mode = req.getParameter("mode");
        if (mode == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Missing 'mode' parameter. Use ?mode=batch or ?mode=realtime");
            return;
        }

        double threshold  = parseParam(req, "threshold",  DEFAULT_THRESHOLD);
        double lowThresh  = parseParam(req, "lowThresh",  DEFAULT_LOW_THRESH);
        double highThresh = parseParam(req, "highThresh", DEFAULT_HIGH_THRESH);

        if (threshold < 0 || lowThresh < 0 || highThresh < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Threshold values cannot be negative.");
            return;
        }

        switch (mode) {
            case "batch":    handleBatch(resp, threshold, lowThresh, highThresh); break;
            case "realtime": handleRealtime(resp, threshold); break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid mode. Use 'batch' or 'realtime'.");
        }
    }

    /**
     * handleBatch()
     *
     * BATCH ANALYSIS — Threshold Alert Summary
     *
     * Steps:
     *   1. Count total alerts (rfh > threshold, is_active = 1)
     *   2. Get top 10 alert locations with alert count and max rfh
     *   3. Get severity summary (Normal / Moderate / Heavy)
     *   4. Return combined JSON response
     *
     * Response JSON:
     *   {
     *     alertCount, totalRecords, alertPercentage, threshold,
     *     topLocations: [{pcode, country, count, maxRfh}, ...],
     *     severity: {Normal:N, Moderate:N, Heavy:N}
     *   }
     */
    private void handleBatch(HttpServletResponse resp,
                              double threshold, double lowThresh, double highThresh)
            throws IOException {
        try {
            int alertCount   = dao.getAlertCount(threshold);
            int totalRecords = dao.getActiveRecords().size();
            double alertPct  = totalRecords > 0 ? (double) alertCount / totalRecords * 100 : 0;

            List<String[]>     topLocations = dao.getTopAlertLocations(threshold, 10);
            Map<String, Integer> severity   = dao.getSeveritySummary(lowThresh, highThresh);

            // Build JSON manually — clean and simple
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"alertCount\":").append(alertCount).append(",");
            json.append("\"totalRecords\":").append(totalRecords).append(",");
            json.append(String.format("\"alertPercentage\":%.2f,", alertPct));
            json.append("\"threshold\":").append(threshold).append(",");
            json.append("\"lowThresh\":").append(lowThresh).append(",");
            json.append("\"highThresh\":").append(highThresh).append(",");

            // Top locations array
            json.append("\"topLocations\":[");
            for (int i = 0; i < topLocations.size(); i++) {
                String[] loc = topLocations.get(i);
                json.append("{");
                json.append("\"pcode\":\"").append(loc[0]).append("\",");
                json.append("\"country\":\"").append(loc[1]).append("\",");
                json.append("\"count\":").append(loc[2]).append(",");
                json.append("\"maxRfh\":").append(loc[3]);
                json.append("}");
                if (i < topLocations.size() - 1) json.append(",");
            }
            json.append("],");

            // Severity summary object
            json.append("\"severity\":{");
            json.append("\"Normal\":").append(severity.getOrDefault("Normal", 0)).append(",");
            json.append("\"Moderate\":").append(severity.getOrDefault("Moderate", 0)).append(",");
            json.append("\"Heavy\":").append(severity.getOrDefault("Heavy", 0));
            json.append("}");

            json.append("}");

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "M1 Batch failed: " + e.getMessage());
        }
    }

    /**
     * handleRealtime()
     *
     * REAL-TIME — Live Alert Detection via SSE
     *
     * Streams active records one-by-one.
     * For each record:
     *   - Checks if rfh > threshold → isAlert flag
     *   - Sends running alert count
     *   - Frontend highlights alert records in red and increments counter
     *
     * SSE event format:
     *   data: {"seq":1,"date":"2022-01-01","pcode":"PH01","country":"PH",
     *          "rfh":3.31,"isAlert":false,"alertCount":0,"threshold":50.0}
     */
    private void handleRealtime(HttpServletResponse resp, double threshold)
            throws IOException {

        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection",    "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = resp.getWriter();

        try {
            List<RainfallRecord> records = dao.getActiveRecords();
            int alertCount = 0;
            int seq        = 0;

            for (RainfallRecord r : records) {
                seq++;
                boolean isAlert = r.getRfh() > threshold;
                if (isAlert) alertCount++;

                String event = String.format(
                    "data: {\"seq\":%d,\"date\":\"%s\",\"pcode\":\"%s\",\"country\":\"%s\"," +
                    "\"rfh\":%.4f,\"isAlert\":%b,\"alertCount\":%d,\"threshold\":%.1f}\n\n",
                    seq, r.getRecordDate(), r.getPcode(), r.getCountry(),
                    r.getRfh(), isAlert, alertCount, threshold
                );

                writer.write(event);
                writer.flush();
                Thread.sleep(50);
            }

            writer.write(String.format(
                "data: {\"done\":true,\"total\":%d,\"alertCount\":%d}\n\n",
                seq, alertCount
            ));
            writer.flush();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
            writer.flush();
        }
    }

    /** Safely parses a double query parameter, returns defaultVal if missing/invalid */
    private double parseParam(HttpServletRequest req, String name, double defaultVal) {
        String val = req.getParameter(name);
        if (val == null || val.isEmpty()) return defaultVal;
        try { return Double.parseDouble(val); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
