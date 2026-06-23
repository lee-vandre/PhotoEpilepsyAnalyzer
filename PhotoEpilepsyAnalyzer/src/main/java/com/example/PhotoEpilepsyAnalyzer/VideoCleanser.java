package com.example.PhotoEpilepsyAnalyzer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class VideoCleanser {

    /**
     * Cleanses a relative luminance profile by freezing flashing segments.
     * When a violation second is encountered, all frames within that second
     * are locked to the value of the frame right before the flashing sequence began.
     *
     * @param originalHistory the raw, unfiltered luminance profile containing flashes
     * @param report          the analysis report containing the identified violation timestamps
     * @param fps             the playback frame rate of the video
     * @return a new, modified List with all rapid flashing spikes flattened
     */
    public List<Double> cleanseLuminanceSpikes(List<Double> originalHistory, AnalysisReport report, double fps) {
        // Defensive Guardrail: If the video is already safe or has no data, return it untouched
        if (originalHistory == null || report == null || report.isSafe() || fps <= 0) {
            return originalHistory != null ? new ArrayList<>(originalHistory) : new ArrayList<>();
        }

        // Create a deep copy of the history array to avoid mutating original source parameters
        List<Double> cleansedHistory = new ArrayList<>(originalHistory);
        int framesPerSecond = (int) Math.round(fps);

        // Loop through each logged violation second
        for (double violationSecond : report.getViolationTimestamps()) {

            // 1. Calculate the exact frame index where the dangerous 1-second window starts
            int startFrameIndex = (int) (violationSecond * fps);

            // 2. Identify the Safe Anchor Frame (the frame immediately BEFORE the flashing begins)
            // If the violation happens at the absolute beginning (index 0), use index 0 as fallback.
            int anchorFrameIndex = Math.max(0, startFrameIndex - 1);
            double safeAnchorLuminance = cleansedHistory.get(anchorFrameIndex);

            // 3. Determine the end boundary of the 1-second flash block
            int endFrameIndex = Math.min(cleansedHistory.size(), startFrameIndex + framesPerSecond);

            // 4. Overwrite all volatile frames inside this second with our safe anchor value
            for (int i = startFrameIndex; i < endFrameIndex; i++) {
                cleansedHistory.set(i, safeAnchorLuminance);
            }
        }

        return cleansedHistory;
    }
}