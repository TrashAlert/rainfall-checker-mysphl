package com.rainfall.dao;

import com.rainfall.model.RainfallRecord;
import com.rainfall.util.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * RainfallDAO — Data Access Object
 *
 * Handles ALL database operations for the rainfall_data table.
 * Supports both Malaysia (MY) and Philippines (PH) records.
 *
 * M1 methods: getAlertSummary(), getTopAlertLocations(), getActiveRecords()
 * M2 methods: getTrendSummary(), getActiveRecordsByCountry()
 * M3 methods: insertRecord(), getAllRecords(), searchRecords(),
 *             getRecordById(), updateRecord(), softDelete(),
 *             reinstateRecord(), deleteAll(), getRecordCount()
 * M4 methods: logExport(), getExportLogs(), logImport()
 */
public class RainfallDAO {

    // ── INSERT ────────────────────────────────────────────────────────────────

    /**
     * insertRecord(record)
     * Saves one RainfallRecord to the database.
     * Called by M3 ImportServlet for every valid CSV row.
     */
    public void insertRecord(RainfallRecord record) throws SQLException {
        String sql = "INSERT INTO rainfall_data " +
                     "(country, record_date, adm_level, adm_id, pcode, n_pixels, rfh, rfh_avg, " +
                     "r1h, r1h_avg, r3h, r3h_avg, rfq, r1q, r3q, version, is_active) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  record.getCountry());
            ps.setString(2,  record.getRecordDate());
            ps.setInt   (3,  record.getAdmLevel());
            ps.setInt   (4,  record.getAdmId());
            ps.setString(5,  record.getPcode());
            ps.setDouble(6,  record.getNPixels());
            ps.setDouble(7,  record.getRfh());
            ps.setDouble(8,  record.getRfhAvg());
            ps.setDouble(9,  record.getR1h());
            ps.setDouble(10, record.getR1hAvg());
            ps.setDouble(11, record.getR3h());
            ps.setDouble(12, record.getR3hAvg());
            ps.setDouble(13, record.getRfq());
            ps.setDouble(14, record.getR1q());
            ps.setDouble(15, record.getR3q());
            ps.setString(16, record.getVersion());
            ps.executeUpdate();
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * getAllRecords(offset, limit)
     * Returns a paginated list of ALL records (active + deleted).
     * Used by M3 dataset browser.
     */
    public List<RainfallRecord> getAllRecords(int offset, int limit) throws SQLException {
        String sql = "SELECT * FROM rainfall_data ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            return mapResultSet(ps.executeQuery());
        }
    }

    /**
     * searchRecords(keyword, offset, limit)
     * Filters records by PCODE, date, or country keyword.
     */
    public List<RainfallRecord> searchRecords(String keyword, int offset, int limit)
            throws SQLException {
        String sql = "SELECT * FROM rainfall_data " +
                     "WHERE (pcode LIKE ? OR record_date LIKE ? OR country LIKE ?) " +
                     "ORDER BY id DESC LIMIT ? OFFSET ?";
        String like = "%" + keyword + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt   (4, limit);
            ps.setInt   (5, offset);
            return mapResultSet(ps.executeQuery());
        }
    }

    /**
     * getRecordById(id)
     * Fetches a single record by primary key. Used by M3 EditServlet.
     */
    public RainfallRecord getRecordById(int id) throws SQLException {
        String sql = "SELECT * FROM rainfall_data WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            List<RainfallRecord> list = mapResultSet(ps.executeQuery());
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * getActiveRecords()
     * Returns ALL active records (is_active = 1) ordered by id.
     * Used by M1 real-time SSE and M4 export.
     */
    public List<RainfallRecord> getActiveRecords() throws SQLException {
        String sql = "SELECT * FROM rainfall_data WHERE is_active = 1 ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return mapResultSet(ps.executeQuery());
        }
    }

    /**
     * getActiveRecordsByCountry(country)
     * Returns active records filtered by country (MY or PH).
     * Used by M2 for comparing Malaysia vs Philippines trends.
     */
    public List<RainfallRecord> getActiveRecordsByCountry(String country) throws SQLException {
        String sql = "SELECT * FROM rainfall_data WHERE is_active = 1 AND country = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, country);
            return mapResultSet(ps.executeQuery());
        }
    }

    // ── M1: THRESHOLD ALERT ANALYSIS ─────────────────────────────────────────

    /**
     * getAlertCount(threshold)
     * Returns the total number of active records where rfh > threshold.
     * Used by M1 batch analysis.
     */
    public int getAlertCount(double threshold) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rainfall_data WHERE is_active = 1 AND rfh > ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, threshold);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * getTopAlertLocations(threshold, limit)
     * Returns the top locations (PCODE) with the most records exceeding
     * the threshold, ordered by occurrence count descending.
     * Used by M1 batch to show highest heavy rainfall locations.
     *
     * Returns list of String[]{pcode, country, count, maxRfh}
     */
    public List<String[]> getTopAlertLocations(double threshold, int limit) throws SQLException {
        String sql = "SELECT pcode, country, COUNT(*) as cnt, MAX(rfh) as maxRfh " +
                     "FROM rainfall_data " +
                     "WHERE is_active = 1 AND rfh > ? " +
                     "GROUP BY pcode, country " +
                     "ORDER BY cnt DESC LIMIT ?";
        List<String[]> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, threshold);
            ps.setInt   (2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new String[]{
                    rs.getString("pcode"),
                    rs.getString("country"),
                    String.valueOf(rs.getInt("cnt")),
                    String.format("%.2f", rs.getDouble("maxRfh"))
                });
            }
        }
        return result;
    }

    /**
     * getSeveritySummary(thresholds)
     * Groups active records into severity bands based on rfh ranges.
     * Returns count per severity level.
     *
     * Severity levels:
     *   Normal  : rfh <= low
     *   Moderate: low < rfh <= high
     *   Heavy   : rfh > high
     */
    public Map<String, Integer> getSeveritySummary(double low, double high) throws SQLException {
        String sql = "SELECT " +
                     "SUM(CASE WHEN rfh <= ? THEN 1 ELSE 0 END) AS normal, " +
                     "SUM(CASE WHEN rfh > ? AND rfh <= ? THEN 1 ELSE 0 END) AS moderate, " +
                     "SUM(CASE WHEN rfh > ? THEN 1 ELSE 0 END) AS heavy " +
                     "FROM rainfall_data WHERE is_active = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, low);
            ps.setDouble(2, low);
            ps.setDouble(3, high);
            ps.setDouble(4, high);
            ResultSet rs = ps.executeQuery();
            Map<String, Integer> map = new LinkedHashMap<>();
            if (rs.next()) {
                map.put("Normal",   rs.getInt("normal"));
                map.put("Moderate", rs.getInt("moderate"));
                map.put("Heavy",    rs.getInt("heavy"));
            }
            return map;
        }
    }

    // ── M2: TREND AND ANOMALY ANALYSIS ───────────────────────────────────────

    /**
     * getCountryStats(country)
     * Returns aggregate statistics for a country's active records:
     * average rfh, max rfh, min rfh, total records.
     * Used by M2 batch for Malaysia vs Philippines comparison.
     *
     * Returns double[]{avg, max, min, count}
     */
    public double[] getCountryStats(String country) throws SQLException {
        String sql = "SELECT AVG(rfh), MAX(rfh), MIN(rfh), COUNT(*) " +
                     "FROM rainfall_data WHERE is_active = 1 AND country = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, country);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new double[]{
                    rs.getDouble(1), rs.getDouble(2),
                    rs.getDouble(3), rs.getDouble(4)
                };
            }
            return new double[]{0, 0, 0, 0};
        }
    }

    /**
     * getAnomalyCount(country, multiplier)
     * Counts records where rfh > (rfh_avg * multiplier).
     * A spike is detected when the actual rainfall greatly exceeds
     * the long-term average for that location and period.
     * Used by M2 batch anomaly detection.
     */
    public int getAnomalyCount(String country, double multiplier) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rainfall_data " +
                     "WHERE is_active = 1 AND country = ? AND rfh_avg > 0 AND rfh > (rfh_avg * ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, country);
            ps.setDouble(2, multiplier);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * updateRecord(id, newRfh, newDate, newPcode)
     * Corrects rfh, date, and pcode for a specific record.
     */
    public void updateRecord(int id, double newRfh, String newDate, String newPcode)
            throws SQLException {
        String sql = "UPDATE rainfall_data SET rfh=?, record_date=?, pcode=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newRfh);
            ps.setString(2, newDate);
            ps.setString(3, newPcode);
            ps.setInt   (4, id);
            ps.executeUpdate();
        }
    }

    // ── SOFT DELETE & REINSTATE ───────────────────────────────────────────────

    /**
     * softDelete(id)
     * Sets is_active = 0. Record is excluded from all analysis queries.
     */
    public void softDelete(int id) throws SQLException { setActiveFlag(id, 0); }

    /**
     * reinstateRecord(id)
     * Sets is_active = 1. Record is re-included in analysis.
     */
    public void reinstateRecord(int id) throws SQLException { setActiveFlag(id, 1); }

    private void setActiveFlag(int id, int flag) throws SQLException {
        String sql = "UPDATE rainfall_data SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flag);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * deleteAll()
     * Hard-deletes ALL rows from rainfall_data and clears import_log.
     * Reserved for removing an entire imported dataset batch.
     */
    public int deleteAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            int deleted = st.executeUpdate("DELETE FROM rainfall_data");
            st.executeUpdate("DELETE FROM import_log");
            return deleted;
        }
    }

    // ── COUNT ─────────────────────────────────────────────────────────────────

    /**
     * getRecordCount(keyword)
     * Returns total record count matching the search keyword.
     * Empty keyword = count all records.
     */
    public int getRecordCount(String keyword) throws SQLException {
        String sql = keyword.isEmpty()
            ? "SELECT COUNT(*) FROM rainfall_data"
            : "SELECT COUNT(*) FROM rainfall_data WHERE pcode LIKE ? OR record_date LIKE ? OR country LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── EXPORT LOG ────────────────────────────────────────────────────────────

    /** logExport — records every M4 download to export_log */
    public void logExport(String exportType, String analysis, int recordCount)
            throws SQLException {
        String sql = "INSERT INTO export_log (export_type, analysis, record_count) VALUES (?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, exportType);
            ps.setString(2, analysis);
            ps.setInt   (3, recordCount);
            ps.executeUpdate();
        }
    }

    /** getExportLogs — returns all export history rows, newest first */
    public List<String[]> getExportLogs() throws SQLException {
        String sql = "SELECT export_type, analysis, exported_at, record_count " +
                     "FROM export_log ORDER BY exported_at DESC";
        List<String[]> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new String[]{
                    rs.getString("export_type"),
                    rs.getString("analysis"),
                    rs.getString("exported_at"),
                    String.valueOf(rs.getInt("record_count"))
                });
            }
        }
        return logs;
    }

    // ── IMPORT LOG ────────────────────────────────────────────────────────────

    /** logImport — records each CSV import summary to import_log */
    public void logImport(String filename, String country, int total, int imported, int skipped)
            throws SQLException {
        String sql = "INSERT INTO import_log (filename, country, total_rows, imported_rows, skipped_rows) " +
                     "VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filename);
            ps.setString(2, country);
            ps.setInt   (3, total);
            ps.setInt   (4, imported);
            ps.setInt   (5, skipped);
            ps.executeUpdate();
        }
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * mapResultSet(rs)
     * Converts every row in a ResultSet into a RainfallRecord object.
     */
    private List<RainfallRecord> mapResultSet(ResultSet rs) throws SQLException {
        List<RainfallRecord> list = new ArrayList<>();
        while (rs.next()) {
            RainfallRecord r = new RainfallRecord();
            r.setId        (rs.getInt   ("id"));
            r.setCountry   (rs.getString("country"));
            r.setRecordDate(rs.getString("record_date"));
            r.setAdmLevel  (rs.getInt   ("adm_level"));
            r.setAdmId     (rs.getInt   ("adm_id"));
            r.setPcode     (rs.getString("pcode"));
            r.setNPixels   (rs.getDouble("n_pixels"));
            r.setRfh       (rs.getDouble("rfh"));
            r.setRfhAvg    (rs.getDouble("rfh_avg"));
            r.setR1h       (rs.getDouble("r1h"));
            r.setR1hAvg    (rs.getDouble("r1h_avg"));
            r.setR3h       (rs.getDouble("r3h"));
            r.setR3hAvg    (rs.getDouble("r3h_avg"));
            r.setRfq       (rs.getDouble("rfq"));
            r.setR1q       (rs.getDouble("r1q"));
            r.setR3q       (rs.getDouble("r3q"));
            r.setVersion   (rs.getString("version"));
            r.setIsActive  (rs.getInt   ("is_active"));
            list.add(r);
        }
        return list;
    }
}
