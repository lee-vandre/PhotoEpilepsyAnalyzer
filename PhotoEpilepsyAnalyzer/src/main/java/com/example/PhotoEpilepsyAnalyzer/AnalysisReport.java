package com.example.PhotoEpilepsyAnalyzer;

import java.util.List;

public class AnalysisReport {
    private boolean safe;
    private List<Double> violationTimestamps; // Tracks seconds (e.g., 1.42, 15.8)

    public boolean isSafe() {
        return safe;
    }

    public List<Double> getViolationTimestamps() {
        return violationTimestamps;
    }
}