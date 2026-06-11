package com.rainfall.model;

/**
 * RainfallRecord — Data Model
 *
 * Represents one row from the rainfall_data table.
 * Supports both Malaysia (MY) and Philippines (PH) records.
 * The country field distinguishes the dataset origin.
 */
public class RainfallRecord {

    private int    id;
    private String country;      // "MY" or "PH"
    private String recordDate;
    private int    admLevel;
    private int    admId;
    private String pcode;
    private double nPixels;
    private double rfh;          // Rainfall height mm — primary analysis field
    private double rfhAvg;
    private double r1h;
    private double r1hAvg;
    private double r3h;
    private double r3hAvg;
    private double rfq;
    private double r1q;
    private double r3q;
    private String version;
    private int    isActive;

    public RainfallRecord() {}

    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getCountry()              { return country; }
    public void   setCountry(String c)      { this.country = c; }

    public String getRecordDate()           { return recordDate; }
    public void   setRecordDate(String d)   { this.recordDate = d; }

    public int    getAdmLevel()             { return admLevel; }
    public void   setAdmLevel(int a)        { this.admLevel = a; }

    public int    getAdmId()                { return admId; }
    public void   setAdmId(int a)           { this.admId = a; }

    public String getPcode()                { return pcode; }
    public void   setPcode(String p)        { this.pcode = p; }

    public double getNPixels()              { return nPixels; }
    public void   setNPixels(double n)      { this.nPixels = n; }

    public double getRfh()                  { return rfh; }
    public void   setRfh(double r)          { this.rfh = r; }

    public double getRfhAvg()               { return rfhAvg; }
    public void   setRfhAvg(double r)       { this.rfhAvg = r; }

    public double getR1h()                  { return r1h; }
    public void   setR1h(double r)          { this.r1h = r; }

    public double getR1hAvg()               { return r1hAvg; }
    public void   setR1hAvg(double r)       { this.r1hAvg = r; }

    public double getR3h()                  { return r3h; }
    public void   setR3h(double r)          { this.r3h = r; }

    public double getR3hAvg()               { return r3hAvg; }
    public void   setR3hAvg(double r)       { this.r3hAvg = r; }

    public double getRfq()                  { return rfq; }
    public void   setRfq(double r)          { this.rfq = r; }

    public double getR1q()                  { return r1q; }
    public void   setR1q(double r)          { this.r1q = r; }

    public double getR3q()                  { return r3q; }
    public void   setR3q(double r)          { this.r3q = r; }

    public String getVersion()              { return version; }
    public void   setVersion(String v)      { this.version = v; }

    public int    getIsActive()             { return isActive; }
    public void   setIsActive(int a)        { this.isActive = a; }

    @Override
    public String toString() {
        return "RainfallRecord{id=" + id + ", country=" + country +
               ", date=" + recordDate + ", pcode=" + pcode +
               ", rfh=" + rfh + ", isActive=" + isActive + "}";
    }
}
