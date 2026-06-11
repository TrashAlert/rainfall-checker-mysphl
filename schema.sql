-- ============================================================
-- Database Schema for Malaysia & Philippines Rainfall Analysis
-- Course: BITS 3515 TCP/IP Programming
-- ============================================================

CREATE DATABASE IF NOT EXISTS rainfall_db2;
USE rainfall_db2;

-- ============================================================
-- MAIN TABLE: rainfall_data
-- Stores records from both Malaysia and Philippines datasets.
-- country column distinguishes between MY and PH records.
-- is_active = 1 means included in analysis, 0 = soft-deleted.
-- ============================================================
CREATE TABLE IF NOT EXISTS rainfall_data (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    country     VARCHAR(5)     NOT NULL,              -- "MY" or "PH"
    record_date VARCHAR(20)    NOT NULL,
    adm_level   INT,
    adm_id      INT,
    pcode       VARCHAR(10),
    n_pixels    DOUBLE,
    rfh         DOUBLE,                               -- Primary analysis field
    rfh_avg     DOUBLE,
    r1h         DOUBLE,
    r1h_avg     DOUBLE,
    r3h         DOUBLE,
    r3h_avg     DOUBLE,
    rfq         DOUBLE,
    r1q         DOUBLE,
    r3q         DOUBLE,
    version     VARCHAR(20),
    is_active   TINYINT(1) DEFAULT 1,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- IMPORT LOG TABLE: import_log
-- ============================================================
CREATE TABLE IF NOT EXISTS import_log (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    filename      VARCHAR(255),
    country       VARCHAR(5),                         -- "MY" or "PH"
    total_rows    INT,
    imported_rows INT,
    skipped_rows  INT,
    imported_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- EXPORT LOG TABLE: export_log
-- ============================================================
CREATE TABLE IF NOT EXISTS export_log (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    export_type  VARCHAR(10) NOT NULL,
    analysis     VARCHAR(50),
    exported_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    record_count INT
);
