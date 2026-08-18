package com.example.PhotoEpilepsyAnalyzer;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnalysisService {

    private final VideoAnalyzer analyzer;
    private final VideoCleanser cleanser;

    public AnalysisService(VideoAnalyzer analyzer, VideoCleanser cleanser) {
        this.analyzer = analyzer;
        this.cleanser = cleanser;
    }

    /**
     * Complete operational orchestration logic flow.
     */
    public List<Double> processAndCleanseWorkflow(String videoPath, double fps) {
        // Step 1: Turn physical file into a numbers profile
        List<Double> rawProfile = analyzer.extractLuminanceProfile(videoPath);

        // Step 2: Check for safety violations
        AnalysisReport report = analyzer.evaluateLuminanceProfile(rawProfile, fps);

        // Step 3: If unsafe, pass it through the freeze-frame engine
        if (!report.isSafe()) {
            return cleanser.cleanseLuminanceSpikes(rawProfile, report, fps);
        }

        return rawProfile; // Return untouched if safe
    }
}