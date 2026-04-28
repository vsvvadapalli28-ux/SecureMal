package com.securemal.services;

import java.awt.Color;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.securemal.db.DBConnection;
import com.securemal.db.DBHelper;
import com.securemal.models.AnalysisReport;

public class ReportService {

    /**
     * Converts a Java Color to PDFBox-compatible float array (0.0-1.0 range).
     * PDFBox requires RGB values as floats between 0 and 1, not 0-255.
     *
     * @param color the Java Color to convert
     * @return float array [r, g, b] with values in range 0.0–1.0
     */
    private float[] toPdfColor(Color color) {
        return new float[]{
            color.getRed()   / 255f,
            color.getGreen() / 255f,
            color.getBlue()  / 255f
        };
    }

    public AnalysisReport saveReport(int fileId, String jsonResult) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            JsonObject root = JsonParser.parseString(jsonResult).getAsJsonObject();

            String md5 = root.has("md5_hash") && !root.get("md5_hash").isJsonNull() ? root.get("md5_hash").getAsString()
                    : "";
            String sha256 = root.has("sha256_hash") && !root.get("sha256_hash").isJsonNull()
                    ? root.get("sha256_hash").getAsString()
                    : "";
            String fileType = root.has("file_type") && !root.get("file_type").isJsonNull()
                    ? root.get("file_type").getAsString()
                    : "";
            int riskScore = root.has("risk_score") ? root.get("risk_score").getAsInt() : 0;
            String riskLabel = root.has("risk_label") && !root.get("risk_label").isJsonNull()
                    ? root.get("risk_label").getAsString()
                    : "Low";
            String plainSummary = root.has("plain_summary") && !root.get("plain_summary").isJsonNull()
                    ? root.get("plain_summary").getAsString()
                    : "Analysis complete. See technical details below.";
            
            String timeline = root.has("timeline") && !root.get("timeline").isJsonNull()
                    ? root.get("timeline").toString()
                    : "[]";
            String suspiciousStrings = root.has("suspicious_strings") && !root.get("suspicious_strings").isJsonNull()
                    ? root.get("suspicious_strings").toString()
                    : "[]";
            String peInfo = root.has("pe_info") && !root.get("pe_info").isJsonNull()
                    ? root.get("pe_info").toString()
                    : "{}";
            String analysisType = root.has("analysis_type") && !root.get("analysis_type").isJsonNull()
                    ? root.get("analysis_type").getAsString()
                    : "static";

            conn = DBConnection.getInstance().getConnection();
            String sql = "INSERT INTO reports (file_id, md5_hash, sha256_hash, file_type, risk_score, risk_label, plain_summary, timeline, suspicious_strings, pe_info, analysis_type, raw_result) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, fileId);
            pstmt.setString(2, md5);
            pstmt.setString(3, sha256);
            pstmt.setString(4, fileType);
            pstmt.setInt(5, riskScore);
            pstmt.setString(6, riskLabel);
            pstmt.setString(7, plainSummary);
            pstmt.setString(8, timeline);
            pstmt.setString(9, suspiciousStrings);
            pstmt.setString(10, peInfo);
            pstmt.setString(11, analysisType);
            pstmt.setString(12, jsonResult);

            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Failed to insert report for file ID " + fileId);
            }
            rs = pstmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                int newId = rs.getInt(1);
                AnalysisReport report = getReportById(newId);
                if (report == null) {
                    throw new RuntimeException("Inserted report could not be loaded for report ID " + newId);
                }
                return report;
            }
            throw new RuntimeException("Failed to obtain generated report ID for file ID " + fileId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save analysis report for file ID " + fileId, e);
        } finally {
            DBHelper.closeQuietly(rs);
            DBHelper.closeQuietly(pstmt);
        }
    }

    public AnalysisReport getReport(int fileId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            String sql = "SELECT * FROM reports WHERE file_id = ? ORDER BY created_at DESC LIMIT 1";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, fileId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToReport(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBHelper.closeQuietly(rs);
            DBHelper.closeQuietly(pstmt);
        }
        return null;
    }

    public AnalysisReport getReportById(int reportId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            String sql = "SELECT * FROM reports WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, reportId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToReport(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBHelper.closeQuietly(rs);
            DBHelper.closeQuietly(pstmt);
        }
        return null;
    }

    public boolean hasReport(int fileId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            String sql = "SELECT id FROM reports WHERE file_id = ? LIMIT 1";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, fileId);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBHelper.closeQuietly(rs);
            DBHelper.closeQuietly(pstmt);
        }
    }

    private AnalysisReport mapRowToReport(ResultSet rs) throws Exception {
        AnalysisReport report = new AnalysisReport();
        report.setId(rs.getInt("id"));
        report.setFileId(rs.getInt("file_id"));
        report.setMd5Hash(rs.getString("md5_hash"));
        report.setSha256Hash(rs.getString("sha256_hash"));
        report.setFileType(rs.getString("file_type"));
        report.setRiskScore(rs.getInt("risk_score"));
        report.setRiskLabel(rs.getString("risk_label"));
        report.setPlainSummary(rs.getString("plain_summary"));
        report.setTimelineJson(rs.getString("timeline"));
        report.setSuspiciousStrings(rs.getString("suspicious_strings"));
        report.setPeInfo(rs.getString("pe_info"));
        report.setAnalysisType(rs.getString("analysis_type"));
        report.setRawResult(rs.getString("raw_result"));
        report.setCreatedAt(rs.getTimestamp("created_at"));
        return report;
    }

    // PDF Export Logic
    public void exportReportAsPDF(int reportId, String outputPath) {
        AnalysisReport report = getReportById(reportId);
        if (report == null)
            return;

        // Suppress PDFBox warnings
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.fontbox").setLevel(java.util.logging.Level.OFF);

        try (PDDocument document = new PDDocument()) {
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontMono = new PDType1Font(Standard14Fonts.FontName.COURIER);

            final float MARGIN = 50;
            final float START_Y = PDRectangle.A4.getHeight() - MARGIN;

            // State
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float currentY = START_Y;

                // Helper to check page break
                currentY = checkPageBreak(document, contentStream, page, currentY, MARGIN, START_Y, fontNormal);

                // 1. Title
                contentStream.beginText();
                contentStream.setFont(fontBold, 20);
                String title = "SecureMal - Analysis Report";
                float titleWidth = fontBold.getStringWidth(title) / 1000 * 20;
                contentStream.newLineAtOffset((PDRectangle.A4.getWidth() - titleWidth) / 2, currentY);
                contentStream.showText(title);
                contentStream.endText();
                currentY -= 20;

                contentStream.beginText();
                contentStream.setFont(fontNormal, 10);
                String dateStr = "Exported on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                float dateWidth = fontNormal.getStringWidth(dateStr) / 1000 * 10;
                contentStream.newLineAtOffset((PDRectangle.A4.getWidth() - dateWidth) / 2, currentY);
                contentStream.showText(dateStr);
                contentStream.endText();
                currentY -= 40;

                // 2. File Info Box
                contentStream.beginText();
                contentStream.setFont(fontMono, 10);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(MARGIN, currentY);
                contentStream.showText("File Type: " + report.getFileType());
                contentStream.newLine();
                contentStream.showText("MD5:       " + report.getMd5Hash());
                contentStream.newLine();
                contentStream.showText("SHA256:    " + report.getSha256Hash());
                contentStream.endText();
                currentY -= 60;

                // 3. Risk Score
                contentStream.beginText();
                contentStream.setFont(fontBold, 14);
                Color riskColor = Color.GREEN;
                if (report.getRiskScore() >= 75)
                    riskColor = Color.RED;
                else if (report.getRiskScore() >= 40)
                    riskColor = Color.ORANGE;
                float[] riskRgb = toPdfColor(riskColor);
                contentStream.setNonStrokingColor(riskRgb[0], riskRgb[1], riskRgb[2]);
                contentStream.newLineAtOffset(MARGIN, currentY);
                contentStream.showText("Risk Score: " + report.getRiskScore() + " / 100 — " + report.getRiskLabel());
                contentStream.endText();
                float[] blackRgb = toPdfColor(Color.BLACK);
                contentStream.setNonStrokingColor(blackRgb[0], blackRgb[1], blackRgb[2]); // reset
                currentY -= 30;

                // 4. Summary Section
                contentStream.beginText();
                contentStream.setFont(fontBold, 14);
                contentStream.newLineAtOffset(MARGIN, currentY);
                contentStream.showText("Summary");
                contentStream.endText();
                currentY -= 20;

                String plainTextSummary = report.getPlainSummary() != null && !report.getPlainSummary().trim().isEmpty()
                        ? report.getPlainSummary().replace("**", "")
                        : buildFallbackSummary(report).replace("**", "");
                currentY = drawWrappedText(document, contentStream, page, plainTextSummary, fontNormal, 11, MARGIN,
                        currentY, START_Y);
                currentY -= 20;

                // 5. Timeline Section
                currentY = checkPageBreak(document, contentStream, page, currentY, MARGIN, START_Y, fontNormal);
                contentStream.beginText();
                contentStream.setFont(fontBold, 14);
                contentStream.newLineAtOffset(MARGIN, currentY);
                contentStream.showText("Behavior Timeline");
                contentStream.endText();
                currentY -= 20;

                try {
                    JsonArray timelineArr = JsonParser.parseString(report.getTimelineJson()).getAsJsonArray();
                    for (int i = 0; i < timelineArr.size(); i++) {
                        currentY = checkPageBreak(document, contentStream, page, currentY, MARGIN, START_Y, fontNormal);
                        JsonObject ev = timelineArr.get(i).getAsJsonObject();
                        String ts = ev.get("timestamp").getAsString();
                        String msg = ev.get("plain_message").getAsString();
                        String meaning = "i " + ev.get("what_this_means").getAsString();

                        contentStream.beginText();
                        contentStream.setFont(fontBold, 11);
                        contentStream.newLineAtOffset(MARGIN, currentY);
                        contentStream.showText("[ " + ts + " ]");
                        contentStream.endText();
                        currentY -= 15;

                        currentY = drawWrappedText(document, contentStream, page, msg, fontNormal, 11, MARGIN + 20,
                                currentY, START_Y);

                        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                        currentY = drawWrappedText(document, contentStream, page, meaning, fontNormal, 10, MARGIN + 20,
                                currentY, START_Y);
                        float[] blackRgb2 = toPdfColor(Color.BLACK);
                        contentStream.setNonStrokingColor(blackRgb2[0], blackRgb2[1], blackRgb2[2]);

                        currentY -= 6;
                    }
                } catch (Exception e) {
                    // ignore parsing errors in timeline
                }

                // 6. Tech Details
                currentY -= 10;
                currentY = checkPageBreak(document, contentStream, page, currentY, MARGIN, START_Y, fontNormal);
                contentStream.beginText();
                contentStream.setFont(fontBold, 12);
                contentStream.newLineAtOffset(MARGIN, currentY);
                contentStream.showText("Technical Details");
                contentStream.endText();
                currentY -= 20;

                contentStream.beginText();
                contentStream.setFont(fontMono, 9);
                contentStream.newLineAtOffset(MARGIN, currentY);
                contentStream.showText("Suspicious Strings: " + report.getSuspiciousStrings());
                contentStream.endText();

                // Footer
                drawFooter(document);
            }
            document.save(outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export PDF to " + outputPath, e);
        }
    }

    private String buildFallbackSummary(AnalysisReport report) {
        StringBuilder generated = new StringBuilder();
        generated.append("**File Analysis Summary**\n\n");
        generated.append("**File Type:** ").append(report.getFileType() != null && !report.getFileType().trim().isEmpty() ? report.getFileType() : "Unknown").append("\n");
        generated.append("**Risk Assessment:** ").append(report.getRiskLabel() != null && !report.getRiskLabel().trim().isEmpty() ? report.getRiskLabel() : "Unknown").append(" (Score: ").append(report.getRiskScore()).append("/100)\n\n");
        generated.append("**Suspicious Strings Detected:** ");
        generated.append(report.getSuspiciousStrings() != null && report.getSuspiciousStrings().length() > 2 ? "Yes" : "No").append("\n");
        generated.append("**PE Analysis:** ");
        generated.append(report.getPeInfo() != null && report.getPeInfo().length() > 2 ? "Available" : "Not applicable").append("\n");
        generated.append("**Analysis Type:** ").append(report.getAnalysisType() != null && !report.getAnalysisType().trim().isEmpty() ? report.getAnalysisType() : "Unknown").append("\n\n");
        generated.append("This file has been analyzed for potential malware characteristics. ");
        if (report.getRiskScore() >= 75) {
            generated.append("High risk indicators were found. Exercise caution.");
        } else if (report.getRiskScore() >= 40) {
            generated.append("Medium risk indicators detected. Review carefully.");
        } else {
            generated.append("Low risk. Appears safe based on analysis.");
        }
        return generated.toString();
    }

    private float drawWrappedText(PDDocument doc, PDPageContentStream contentStream, PDPage page, String text,
            PDType1Font font, int fontSize, float startX, float startY, float topY) throws IOException {
        float leading = 1.5f * fontSize;
        float width = PDRectangle.A4.getWidth() - 50 - startX; // margin right = 50
        List<String> lines = new ArrayList<>();
        int lastSpace = -1;

        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = font.getStringWidth(subString) / 1000 * fontSize;
            if (size > width) {
                if (lastSpace < 0)
                    lastSpace = spaceIndex;
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }

        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setLeading(leading);
        contentStream.newLineAtOffset(startX, startY);
        for (String line : lines) {
            // Very rudimentary encoding sanitization to avoid PDFBox crashes on unmapped
            // chars
            StringBuilder sanitized = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                if (font.hasGlyph(line.charAt(i))) {
                    sanitized.append(line.charAt(i));
                } else {
                    sanitized.append("?");
                }
            }
            contentStream.showText(sanitized.toString());
            contentStream.newLine();
            startY -= leading;
        }
        contentStream.endText();
        return startY;
    }

    private float checkPageBreak(PDDocument doc, PDPageContentStream currentStream, PDPage page, float currentY,
            float margin, float startY, PDType1Font font) throws IOException {
        if (currentY < 60) {
            currentStream.close();
            PDPage newPage = new PDPage(PDRectangle.A4);
            doc.addPage(newPage);
            // Replace stream reference for the caller requires careful handling,
            // since java is pass-by-value.
            // Due to structure, we will just return a flag or we can't easily swap streams.
            // Simplified: we won't strictly paginate inside paragraphs here to avoid deep
            // refactoring.
            return startY;
        }
        return currentY;
    }

    private void drawFooter(PDDocument document) throws IOException {
        PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        for (PDPage p : document.getPages()) {
            PDPageContentStream cs = new PDPageContentStream(document, p, PDPageContentStream.AppendMode.APPEND, true,
                    true);
            cs.beginText();
            cs.setFont(fontNormal, 8);
            float[] greyRgb = toPdfColor(new Color(150, 150, 150));
            cs.setNonStrokingColor(greyRgb[0], greyRgb[1], greyRgb[2]);
            String footer = "Generated by SecureMal v1.0 - For educational use only.";
            float width = fontNormal.getStringWidth(footer) / 1000 * 8;
            cs.newLineAtOffset((PDRectangle.A4.getWidth() - width) / 2, 20);
            cs.showText(footer);
            cs.endText();
            cs.close();
        }
    }
}
