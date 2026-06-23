package com.example.PhotoEpilepsyAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class AnalysisReport {
    private final boolean safe;
    private final List<Double> violationTimestamps;

    // Fixed constructor to accept clean, matching types
    public AnalysisReport(boolean safe, List<Double> violationTimestamps) {
        this.safe = safe;
        // Defensive copy: If a null list is accidentally passed, initialize an empty list to prevent NullPointerExceptions
        this.violationTimestamps = (violationTimestamps != null) ? violationTimestamps : new ArrayList<>();
    }

    public boolean isSafe() {
        return this.safe;
    }

    public List<Double> getViolationTimestamps() {
        return this.violationTimestamps;
    }
}