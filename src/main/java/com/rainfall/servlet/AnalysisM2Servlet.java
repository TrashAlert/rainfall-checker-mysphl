package com.rainfall.servlet;

import com.rainfall.dao.RainfallDAO;
import com.rainfall.model.RainfallRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;

/**
 * AnalysisM2Servlet — Module M2: Rainfall Trend and Anomaly Detection
 *
 * URL Mapping : /analysis/m2
 * HTTP Method : GET
 *
 * Query Parameters:
 *   mode       — "batch" or "realtime"
 *   multiplier — anomaly spike multiplier (default 2.0)
 *                A record is anomalous when rfh > rfh_avg * multiplier
 *
 * Purpose:
 *   Analyses rainfall patterns between Malaysia and the Philippines,
 *   detects unusual rainfall spikes, and compares trends across both countries.
 *
 * BATCH MODE:
 *   - Computes per-country stats: avg, max, min, count (Malaysia vs Philippines)
 *   - Counts anomalous spikes per country (rfh > rfh_avg * multiplier)
 *   - Returns a side-by-side comparison JSON
 *
 * REAL-TIME MODE (SSE):
 *   - Streams ALL active records (both countries) one-by-one
 *   - Tracks running moving average per country
 *   - Tracks highest rfh seen so far per country
 *   - Flags sudden changes (rfh deviates from running average by > 50%)
 *   - Frontend updates moving average and highest-so-far panels live
 *
 * All queries filter by is_active = 1 (mandatory).
 */
@WebServlet("/analysis/m2")
public class AnalysisM2Servlet extends HttpServlet {

    private static final double DEFAULT_MULTIPLIER = 2.0;
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

        double multiplier = DEFAULT_MULTIPLIER;
        String mStr = req.getParameter("multiplier");
        if (mStr != null && !mStr.isEmpty()) {
            try {
                multiplier = Double.parseDouble(mStr);
                if (multiplier <= 0) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                   "Multiplier must be greater than 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid multiplier value.");
                return;
            }
        }

        switch (mode) {
            case "batch":    handleBatch(resp, multiplier);    break;
            case "realtime": handleRealtime(resp, multiplier); break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid mode. Use 'batch' or 'realtime'.");
        }
    }

    /**
     * handleBatch()
     *
     * BATCH ANALYSIS — Malaysia vs Philippines Trend Comparison
     *
     * Steps:
     *   1. Get stats for Malaysia: avg, max, min, count
     *   2. Get stats for Philippines: avg, max, min, count
     *   3. Count anomalies (spikes) per country
     *   4. Return combined JSON with side-by-side comparison
     *
     * Response JSON:
     *   {
     *     malaysia:    {avg, max, min, count, anomalyCount},
     *     philippines: {avg, max, min, count, anomalyCount},
     *     multiplier,
     *     comparison:  {higherAvgCountry, higherMaxCountry, totalAnomaly}
     *   }
     */
    private void handleBatch(HttpServletResponse resp, double multiplier)
            throws IOException {
        try {
            double[] myStats  = dao.getCountryStats("MY");
            double[] phStats  = dao.getCountryStats("PH");
            int myAnomalies   = dao.getAnomalyCount("MY", multiplier);
            int phAnomalies   = dao.getAnomalyCount("PH", multiplier);

            String higherAvg = myStats[0] >= phStats[0] ? "Malaysia" : "Philippines";
            String higherMax = myStats[1] >= phStats[1] ? "Malaysia" : "Philippines";
            int totalAnomaly = myAnomalies + phAnomalies;

            StringBuilder json = new StringBuilder();
            json.append("{");

            // Malaysia stats
            json.append("\"malaysia\":{");
            json.append(String.format("\"avg\":%.4f,", myStats[0]));
            json.append(String.format("\"max\":%.4f,", myStats[1]));
            json.append(String.format("\"min\":%.4f,", myStats[2]));
            json.append(String.format("\"count\":%d,", (int) myStats[3]));
            json.append("\"anomalyCount\":").append(myAnomalies);
            json.append("},");

            // Philippines stats
            json.append("\"philippines\":{");
            json.append(String.format("\"avg\":%.4f,", phStats[0]));
            json.append(String.format("\"max\":%.4f,", phStats[1]));
            json.append(String.format("\"min\":%.4f,", phStats[2]));
            json.append(String.format("\"count\":%d,", (int) phStats[3]));
            json.append("\"anomalyCount\":").append(phAnomalies);
            json.append("},");

            // Comparison summary
            json.append("\"multiplier\":").append(multiplier).append(",");
            json.append("\"comparison\":{");
            json.append("\"higherAvgCountry\":\"").append(higherAvg).append("\",");
            json.append("\"higherMaxCountry\":\"").append(higherMax).append("\",");
            json.append("\"totalAnomaly\":").append(totalAnomaly);
            json.append("}");

            json.append("}");

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "M2 Batch failed: " + e.getMessage());
        }
    }

    /**
     * handleRealtime()
     *
     * REAL-TIME — Trend Tracking via SSE
     *
     * Streams ALL active records (both countries interleaved).
     * Per record, tracks independently for each country:
     *   - Moving average (running average of rfh seen so far)
     *   - Highest rfh seen so far
     *   - Sudden change flag: rfh deviates from moving avg by > 50%
     *
     * SSE event format:
     *   data: {"seq":1,"date":"2022-01-01","pcode":"MY01","country":"MY",
     *          "rfh":144.5,
     *          "myMovingAvg":144.5,"myHighest":144.5,
     *          "phMovingAvg":0,"phHighest":0,
     *          "suddenChange":false,"multiplier":2.0}
     */
    private void handleRealtime(HttpServletResponse resp, double multiplier)
            throws IOException {

        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection",    "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = resp.getWriter();

        try {
            List<RainfallRecord> records = dao.getActiveRecords();

            // Per-country tracking variables
            double mySumRfh   = 0; int myCount   = 0; double myHighest   = 0;
            double phSumRfh   = 0; int phCount   = 0; double phHighest   = 0;
            int    seq        = 0;

            for (RainfallRecord r : records) {
                seq++;
                boolean isMY = "MY".equals(r.getCountry());
                double  rfh  = r.getRfh();

                // Update running totals for the correct country
                double myMovingAvg, phMovingAvg;
                boolean suddenChange = false;

                if (isMY) {
                    mySumRfh += rfh; myCount++;
                    if (rfh > myHighest) myHighest = rfh;
                    myMovingAvg = mySumRfh / myCount;
                    // Sudden change: rfh deviates from moving avg by > 50%
                    if (myCount > 1 && myMovingAvg > 0) {
                        suddenChange = Math.abs(rfh - myMovingAvg) / myMovingAvg > 0.5;
                    }
                } else {
                    phSumRfh += rfh; phCount++;
                    if (rfh > phHighest) phHighest = rfh;
                    phMovingAvg = phSumRfh / phCount;
                    if (phCount > 1 && phMovingAvg > 0) {
                        suddenChange = Math.abs(rfh - phMovingAvg) / phMovingAvg > 0.5;
                    }
                }

                myMovingAvg = myCount > 0 ? mySumRfh / myCount : 0;
                phMovingAvg = phCount > 0 ? phSumRfh / phCount : 0;

                String event = String.format(
                    "data: {\"seq\":%d,\"date\":\"%s\",\"pcode\":\"%s\",\"country\":\"%s\"," +
                    "\"rfh\":%.4f," +
                    "\"myMovingAvg\":%.4f,\"myHighest\":%.4f," +
                    "\"phMovingAvg\":%.4f,\"phHighest\":%.4f," +
                    "\"suddenChange\":%b,\"multiplier\":%.1f}\n\n",
                    seq, r.getRecordDate(), r.getPcode(), r.getCountry(),
                    rfh, myMovingAvg, myHighest, phMovingAvg, phHighest,
                    suddenChange, multiplier
                );

                writer.write(event);
                writer.flush();
                Thread.sleep(50);
            }

            writer.write(String.format(
                "data: {\"done\":true,\"total\":%d," +
                "\"myFinalAvg\":%.4f,\"myHighest\":%.4f," +
                "\"phFinalAvg\":%.4f,\"phHighest\":%.4f}\n\n",
                seq, myCount > 0 ? mySumRfh/myCount : 0, myHighest,
                phCount > 0 ? phSumRfh/phCount : 0, phHighest
            ));
            writer.flush();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
            writer.flush();
        }
    }
}
