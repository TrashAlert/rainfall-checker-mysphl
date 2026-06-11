package com.rainfall.servlet;

import com.rainfall.dao.RainfallDAO;
import com.rainfall.model.RainfallRecord;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;

/**
 * ImportServlet — Module M3: CSV File Import & Preprocessing
 *
 * URL Mapping : /import
 * HTTP Method : POST (multipart/form-data)
 *
 * Handles CSV upload for both Malaysia and Philippines datasets.
 * Automatically detects country from PCODE prefix (MY = Malaysia, PH = Philippines).
 * Handles both date formats: "1/1/22" (Malaysia) and "2022-01-01" (Philippines).
 *
 * Form fields:
 *   csvFile  — the uploaded CSV file
 *   rowLimit — (optional) maximum valid rows to import
 */
@WebServlet("/import")
public class ImportServlet extends HttpServlet {

    private final RainfallDAO dao = new RainfallDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected a file upload request.");
            return;
        }

        int    totalRows    = 0;
        int    importedRows = 0;
        int    skippedRows  = 0;
        String filename     = "unknown";
        String detectedCountry = "UNKNOWN";
        int    rowLimit     = -1;

        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload   upload  = new ServletFileUpload(factory);
            List<FileItem>      items   = upload.parseRequest(req);

            InputStream csvStream = null;

            for (FileItem item : items) {
                if (item.isFormField() && item.getFieldName().equals("rowLimit")) {
                    String limitStr = item.getString().trim();
                    if (!limitStr.isEmpty()) {
                        try {
                            rowLimit = Integer.parseInt(limitStr);
                            if (rowLimit < 1) {
                                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                               "Row limit must be at least 1.");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                           "Row limit must be a valid whole number.");
                            return;
                        }
                    }
                }
                if (!item.isFormField() && item.getFieldName().equals("csvFile")) {
                    filename  = item.getName();
                    csvStream = item.getInputStream();
                }
            }

            if (csvStream == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No CSV file found in request.");
                return;
            }

            BufferedReader reader    = new BufferedReader(new InputStreamReader(csvStream));
            String         line;
            boolean        isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                if (line.trim().isEmpty()) continue;
                if (rowLimit != -1 && importedRows >= rowLimit) break;

                totalRows++;

                RainfallRecord record = parseAndValidate(line);
                if (record != null) {
                    // Detect country from PCODE prefix on first valid record
                    if (detectedCountry.equals("UNKNOWN") && record.getPcode() != null) {
                        detectedCountry = record.getPcode().startsWith("PH") ? "PH" : "MY";
                    }
                    record.setCountry(detectedCountry);
                    dao.insertRecord(record);
                    importedRows++;
                } else {
                    skippedRows++;
                }
            }

            dao.logImport(filename, detectedCountry, totalRows, importedRows, skippedRows);

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Import failed: " + e.getMessage());
            return;
        }

        String limitMsg = (rowLimit != -1) ? "+Row+limit+of+" + rowLimit + "+applied." : "";
        resp.sendRedirect(req.getContextPath() +
                          "/dataset?msg=Imported+" + importedRows +
                          "+records+(" + detectedCountry + ").+Skipped+" + skippedRows +
                          "+invalid+rows." + limitMsg);
    }

    /**
     * parseAndValidate(line)
     *
     * Parses a single CSV line into a RainfallRecord.
     * Handles two date formats:
     *   Malaysia format  : "1/1/22"
     *   Philippines format: "2022-01-01"
     *
     * Validation:
     *   - date must not be blank
     *   - rfh must be a valid non-negative number
     *   - line must have at least 15 columns
     */
    private RainfallRecord parseAndValidate(String line) {
        String[] cols = line.split(",", -1);
        if (cols.length < 15) return null;

        try {
            String date = cols[0].trim();
            if (date.isEmpty()) return null;

            double rfh = parseDouble(cols[5]);
            if (rfh < 0) return null;

            RainfallRecord r = new RainfallRecord();
            r.setRecordDate(date);
            r.setAdmLevel  (parseInt   (cols[1]));
            r.setAdmId     (parseInt   (cols[2]));
            r.setPcode     (cols[3].trim());
            r.setNPixels   (parseDouble(cols[4]));
            r.setRfh       (rfh);
            r.setRfhAvg    (parseDouble(cols[6]));
            r.setR1h       (parseDouble(cols[7]));
            r.setR1hAvg    (parseDouble(cols[8]));
            r.setR3h       (parseDouble(cols[9]));
            r.setR3hAvg    (parseDouble(cols[10]));
            r.setRfq       (parseDouble(cols[11]));
            r.setR1q       (parseDouble(cols[12]));
            r.setR3q       (parseDouble(cols[13]));
            r.setVersion   (cols[14].trim());
            return r;

        } catch (Exception e) {
            return null;
        }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}
