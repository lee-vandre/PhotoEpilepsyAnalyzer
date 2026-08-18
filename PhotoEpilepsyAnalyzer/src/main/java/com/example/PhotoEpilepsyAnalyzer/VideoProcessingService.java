package com.example.PhotoEpilepsyAnalyzer;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VideoProcessingService {

    private final VideoAnalyzer analyzer;
    private final VideoReassembler reassembler;

    public VideoProcessingService(VideoAnalyzer analyzer, VideoReassembler reassembler) {
        this.analyzer = analyzer;
        this.reassembler = reassembler;
    }

    /**
     * Complete workflow orchestration: Analyzes an input video,
     * and if flashes are found, compiles a brand new safe video file.
     *
     * @param inputPath  Path to the source video file
     * @param outputPath Path where the safe processed video should be saved
     * @return The final AnalysisReport generated from the input video
     */
    public AnalysisReport processVideoPipeline(String inputPath, String outputPath) {
        // 1. Extract the numerical luminance numbers
        List<Double> rawProfile = analyzer.extractLuminanceProfile(inputPath);

        // 2. We need a temporary grabber just to peek at the video's FPS metadata
        double fps = 30.0; // default fallback
        try (org.bytedeco.javacv.FFmpegFrameGrabber grabber = new org.bytedeco.javacv.FFmpegFrameGrabber(inputPath)) {
            grabber.start();
            fps = grabber.getFrameRate();
            grabber.stop();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read frame rate metadata from video source", e);
        }

        // 3. Evaluate the numbers profile against safety rules
        AnalysisReport report = analyzer.evaluateLuminanceProfile(rawProfile, fps);

        // 4. If unsafe, trigger the frame-freeze video reassembler
        if (!report.isSafe()) {
            System.out.println(AnsiOutput.toString(AnsiColor.YELLOW,
                    "🚨 Video is UNSAFE. Violations found at seconds: " + report.getViolationTimestamps(),
                    AnsiColor.DEFAULT));
            System.out.println(AnsiOutput.toString(AnsiColor.GREEN,
                    "🎬 Initiating frame-freeze reassembly process...",
                    AnsiColor.DEFAULT));
            reassembler.reassembleSafeVideo(inputPath, outputPath, report);
            System.out.println(AnsiOutput.toString(AnsiColor.GREEN,
                    "✅ Safe video successfully written to: " + outputPath,
                    AnsiColor.DEFAULT));
        } else {
            System.out.println(AnsiOutput.toString(AnsiColor.GREEN,
                    "☀️ Video is safe. No reassembly modification required.",
                    AnsiColor.DEFAULT));
        }

        return report;
    }
}