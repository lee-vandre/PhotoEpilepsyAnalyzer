package com.example.PhotoEpilepsyAnalyzer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Component responsible for analyzing video files to detect dangerous flashing patterns
 * that could trigger photosensitive epileptic seizures.
 * <p>
 * The analysis complies with the Web Content Accessibility Guidelines (WCAG 2.2)
 * general flash threshold standards.
 * </p>
 */
@Component
public class VideoAnalyzer {

    /**
     * The structural intensity delta required between adjacent frames
     * to count as an individual flash transition component.
     */
    private final double FLASH_THRESHOLD = 20.0;

    /**
     * Orchestrates the extraction pipeline by reading a video file frame-by-frame
     * and translating pixel profiles into sequential relative luminance scores.
     * <p>
     * Memory allocation is optimized by converting and disposing of heavy {@link BufferedImage}
     * profiles instantly within the sampling loop stream.
     * </p>
     *
     * @param videoPath the absolute or relative file system path to the target video file
     * @return a chronological {@link List} of average luminance weights ranging from 0.0 to 255.0
     * @throws IllegalArgumentException if the target file cannot be found or read securely
     * @throws RuntimeException         if native JavaCV/FFmpeg decoders fail to parse the container
     */
    public List<Double> extractLuminanceProfile(String videoPath) {
        validateVideoFile(videoPath);
        List<Double> luminanceProfile = new ArrayList<>();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();

            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                Frame frame;
                while ((frame = grabber.grabImage()) != null) {
                    if (frame.image != null) {
                        BufferedImage bufferedImage = converter.convert(frame);
                        if (bufferedImage != null) {
                            double avgLuminance = calculateFrameLuminance(bufferedImage);
                            luminanceProfile.add(avgLuminance);
                        }
                    }
                }
            }
            grabber.stop();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract luminance profile from video", e);
        }

        return luminanceProfile;
    }

    /**
     * Orchestrates the statistical logic pipeline to find high-frequency strobing clusters
     * using a dynamic, index-based 1-second sliding window.
     *
     * @param luminanceHistory the linear chronological array of frame brightness levels
     * @param fps              the native playback frame rate of the parsed video file
     * @return a structured {@link AnalysisReport} detailing the safety status and recorded failure offsets
     */
    public AnalysisReport evaluateLuminanceProfile(List<Double> luminanceHistory, double fps) {
        if (isHistoryInsufficient(luminanceHistory, fps)) {
            return new AnalysisReport(true, new ArrayList<>());
        }

        List<Double> violationTimestamps = new ArrayList<>();
        boolean isSafe = true;
        int windowFrameSize = (int) Math.round(fps);

        for (int i = 0; i <= luminanceHistory.size() - windowFrameSize; i++) {
            int shiftsInWindow = countShiftsInWindow(luminanceHistory, i, windowFrameSize);

            if (shiftsInWindow > 3) {
                isSafe = false;
                double violationSeconds = (double) i / fps;

                if (isNewViolationTimestamp(violationTimestamps, violationSeconds)) {
                    violationTimestamps.add(violationSeconds);
                }
            }
        }

        return new AnalysisReport(isSafe, violationTimestamps);
    }

    // ==========================================
    // PURIFIED HELPER METHODS (Single Responsibility)
    // ==========================================

    /**
     * Downsamples a single frame to calculate its global average relative luminance.
     * Uses ITU-R BT.709 spectral coefficients to account for human eye perception weights.
     *
     * @param image the uncompressed frame image to inspect
     * @return the calculated mean relative luminance score (0.0 - 255.0)
     */
    private double calculateFrameLuminance(BufferedImage image) {
        long totalLuminance = 0;
        int width = image.getWidth();
        int height = image.getHeight();

        int[] rgbData = image.getRGB(0, 0, width, height, null, 0, width);
        for (int rgb : rgbData) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            totalLuminance += (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
        }

        return (double) totalLuminance / (width * height);
    }

    /**
     * Determines the absolute mathematical variance distance between two brightness levels.
     *
     * @param frame1Luminance luminance profile score of the first frame
     * @param frame2Luminance luminance profile score of the second frame
     * @return the absolute numerical scalar difference
     */
    private double compareFrames(double frame1Luminance, double frame2Luminance) {
        return Math.abs(frame1Luminance - frame2Luminance);
    }

    /**
     * Loops through a localized subsection array window to sum the total number of violent transitions.
     *
     * @param history    the complete relative luminance collection
     * @param startIndex the current entry anchor point of the sliding window
     * @param windowSize the exact count of indexes that equal 1 second of runtime
     * @return the accumulated number of rapid shifts detected inside this window frame block
     */
    private int countShiftsInWindow(List<Double> history, int startIndex, int windowSize) {
        int shiftCount = 0;
        for (int j = startIndex + 1; j < startIndex + windowSize; j++) {
            double brightnessDelta = compareFrames(history.get(j - 1), history.get(j));
            if (brightnessDelta >= FLASH_THRESHOLD) {
                shiftCount++;
            }
        }
        return shiftCount;
    }

    // ==========================================
    // DEFENSIVE EDGE-CASE UTILITIES
    // ==========================================

    /**
     * Defensive validation structure to ensure target path locations are active and accessible.
     */
    private void validateVideoFile(String videoPath) {
        File videoFile = new File(videoPath);
        if (!videoFile.exists() || !videoFile.canRead()) {
            throw new IllegalArgumentException("Video file not found or unreadable: " + videoPath);
        }
    }

    /**
     * Basic array size validation to prevent empty matrices or division by zero runtime flags.
     */
    private boolean isHistoryInsufficient(List<Double> history, double fps) {
        return history == null || history.size() < 2 || fps <= 0;
    }

    /**
     * Deduplication constraint utility checking to verify if a logged violation second
     * is unique or falls into an already captured violation sequence window.
     */
    private boolean isNewViolationTimestamp(List<Double> timestamps, double currentViolationTime) {
        if (timestamps.isEmpty()) return true;
        double lastRecorded = timestamps.get(timestamps.size() - 1);
        return (currentViolationTime - lastRecorded) >= 1.0;
    }
}