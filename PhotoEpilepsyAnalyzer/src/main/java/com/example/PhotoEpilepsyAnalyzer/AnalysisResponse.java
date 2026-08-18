package com.example.PhotoEpilepsyAnalyzer;

import java.util.List;

/**
 * Unified JSON envelope returned by every endpoint.
 *
 * Success shape:
 * {
 *   "success": true,
 *   "safe": true,
 *   "violationTimestamps": [],
 *   "message": null
 * }
 *
 * Error shape:
 * {
 *   "success": false,
 *   "safe": null,
 *   "violationTimestamps": null,
 *   "message": "Human-readable error string"
 * }
 *
 * When a video was unsafe and a corrected version was produced, downloadUrl
 * is populated with a path the client can GET to fetch it; otherwise null.
 */
public class AnalysisResponse {

    private final boolean success;
    private final Boolean safe;                       // null when success=false
    private final List<Double> violationTimestamps;   // null when success=false
    private final String message;                     // null when success=true
    private final String downloadUrl;                 // null unless an unsafe video was corrected

    public AnalysisResponse(boolean success, AnalysisReport report, String message) {
        this(success, report, message, null);

    }

    public AnalysisResponse(boolean success, AnalysisReport report, String message, String downloadUrl) {
        this.success = success;
        this.safe = (report != null) ? report.isSafe() : null;
        this.violationTimestamps = (report != null) ? report.getViolationTimestamps() : null;
        this.message = message;
        this.downloadUrl = downloadUrl;
    }

    public boolean isSuccess() { return success; }
    public Boolean getSafe() { return safe; }
    public List<Double> getViolationTimestamps() { return violationTimestamps; }
    public String getMessage() { return message; }
    public String getDownloadUrl() { return downloadUrl; }
}