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
 */
public class AnalysisResponse {

    private final boolean success;
    private final Boolean safe;                       // null when success=false
    private final List<Double> violationTimestamps;   // null when success=false
    private final String message;                     // null when success=true

    public AnalysisResponse(boolean success, AnalysisReport report, String message) {
        this.success = success;
        this.safe = (report != null) ? report.isSafe() : null;
        this.violationTimestamps = (report != null) ? report.getViolationTimestamps() : null;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public Boolean getSafe() { return safe; }
    public List<Double> getViolationTimestamps() { return violationTimestamps; }
    public String getMessage() { return message; }
}