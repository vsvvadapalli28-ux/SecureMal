package com.securemal.models;

import com.securemal.config.Config;
import java.awt.Color;
import java.sql.Timestamp;

public class AnalysisReport {
    private int id;
    private int fileId;
    private String md5Hash;
    private String sha256Hash;
    private String fileType;
    private int riskScore;
    private String riskLabel;
    private String plainSummary;
    private String timelineJson;
    private String suspiciousStrings;
    private String peInfo;
    private String analysisType;
    private String rawResult;
    private Timestamp createdAt;

    public AnalysisReport() {
    }

    public AnalysisReport(int id, int fileId, String md5Hash, String sha256Hash, String fileType, int riskScore, String riskLabel, String plainSummary, String timelineJson, String suspiciousStrings, String peInfo, String analysisType, String rawResult, Timestamp createdAt) {
        this.id = id;
        this.fileId = fileId;
        this.md5Hash = md5Hash;
        this.sha256Hash = sha256Hash;
        this.fileType = fileType;
        this.riskScore = riskScore;
        this.riskLabel = riskLabel;
        this.plainSummary = plainSummary;
        this.timelineJson = timelineJson;
        this.suspiciousStrings = suspiciousStrings;
        this.peInfo = peInfo;
        this.analysisType = analysisType;
        this.rawResult = rawResult;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLabel() {
        return riskLabel;
    }

    public void setRiskLabel(String riskLabel) {
        this.riskLabel = riskLabel;
    }

    public String getPlainSummary() {
        return plainSummary;
    }

    public void setPlainSummary(String plainSummary) {
        this.plainSummary = plainSummary;
    }

    public String getTimelineJson() {
        return timelineJson;
    }

    public void setTimelineJson(String timelineJson) {
        this.timelineJson = timelineJson;
    }

    public String getSuspiciousStrings() {
        return suspiciousStrings;
    }

    public void setSuspiciousStrings(String suspiciousStrings) {
        this.suspiciousStrings = suspiciousStrings;
    }

    public String getPeInfo() {
        return peInfo;
    }

    public void setPeInfo(String peInfo) {
        this.peInfo = peInfo;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getRawResult() {
        return rawResult;
    }

    public void setRawResult(String rawResult) {
        this.rawResult = rawResult;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Color getRiskColor() {
        if (riskScore >= 75) {
            return Config.COLOR_RISK_HIGH;
        } else if (riskScore >= 40) {
            return Config.COLOR_RISK_MEDIUM;
        } else {
            return Config.COLOR_RISK_LOW;
        }
    }

    public Color getRiskBarColor() {
        if (riskScore >= 75) {
            return Color.RED;
        } else if (riskScore >= 40) {
            return Color.ORANGE;
        } else {
            return Color.GREEN;
        }
    }
}
