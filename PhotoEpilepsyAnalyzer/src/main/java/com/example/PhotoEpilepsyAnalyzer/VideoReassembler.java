package com.example.PhotoEpilepsyAnalyzer;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class VideoReassembler {

    public void reassembleSafeVideo(String inputPath, String outputPath, AnalysisReport report) {
        validateInputFile(inputPath);

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath)) {
            grabber.start();
            double fps = grabber.getFrameRate();
            int totalFrames = grabber.getLengthInFrames();

            Set<Integer> unsafeFrameIndexes = mapViolationsToFrameIndexes(report, fps, totalFrames);

            // Pass 1: Collect the first frame of every violation window as the anchor
            Map<Integer, Frame> violationAnchors = collectViolationAnchors(inputPath, report, fps, totalFrames);

            // Pass 2: Re-open the grabber and write the output, substituting anchors in unsafe zones
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    outputPath,
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels())) {

                configureRecorderEncoding(recorder, grabber);
                recorder.start();

                // Re-open a fresh grabber for the second pass
                try (FFmpegFrameGrabber grabber2 = new FFmpegFrameGrabber(inputPath)) {
                    grabber2.start();

                    Frame frame;
                    int currentFrameIndex = 0;

                    while ((frame = grabber2.grab()) != null) {
                        if (frame.image != null) {
                            if (unsafeFrameIndexes.contains(currentFrameIndex)) {
                                // Find which violation window owns this frame and use its anchor
                                Frame anchor = findAnchorForFrame(currentFrameIndex, fps, violationAnchors);
                                recorder.record(anchor != null ? anchor : frame);
                            } else {
                                recorder.record(frame);
                            }
                            currentFrameIndex++;
                        } else if (frame.samples != null) {
                            recorder.record(frame);
                        }
                    }

                    grabber2.stop();
                }

                recorder.stop();
            }

            grabber.stop();
        } catch (Exception e) {
            throw new RuntimeException("Failed to reassemble safe video", e);
        }
    }

    // ==========================================
    // PRIVATE PIPELINE HELPER METHODS
    // ==========================================

    /**
     * Pass 1 — Opens the video and deep-copies the very first frame of each violation window.
     * Key = the startFrame index of the violation, Value = deep copy of that frame.
     */
    private Map<Integer, Frame> collectViolationAnchors(String inputPath, AnalysisReport report, double fps, int totalFrames) throws Exception {
        Map<Integer, Frame> anchors = new HashMap<>();

        if (report.isSafe() || report.getViolationTimestamps() == null) {
            return anchors;
        }

        // Build a set of just the start frame indices we need to capture
        Set<Integer> startFrames = new HashSet<>();
        for (double violationSecond : report.getViolationTimestamps()) {
            startFrames.add((int)(violationSecond * fps));
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath)) {
            grabber.start();

            Frame frame;
            int currentFrameIndex = 0;

            while ((frame = grabber.grab()) != null && anchors.size() < startFrames.size()) {
                if (frame.image != null) {
                    if (startFrames.contains(currentFrameIndex)) {
                        // Deep copy this frame — it is the anchor for its entire violation window
                        anchors.put(currentFrameIndex, deepCopyFrame(frame));
                    }
                    currentFrameIndex++;
                }
            }

            grabber.stop();
        }

        return anchors;
    }

    /**
     * Given a frame index that sits inside an unsafe zone, finds the start frame
     * of the violation window it belongs to and returns the pre-captured anchor.
     */
    private Frame findAnchorForFrame(int frameIndex, double fps, Map<Integer, Frame> anchors) {
        int framesPerSecond = (int) Math.round(fps);

        for (Map.Entry<Integer, Frame> entry : anchors.entrySet()) {
            int windowStart = entry.getKey();
            int windowEnd = windowStart + framesPerSecond;
            if (frameIndex >= windowStart && frameIndex < windowEnd) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Set<Integer> mapViolationsToFrameIndexes(AnalysisReport report, double fps, int totalFrames) {
        Set<Integer> unsafeIndexes = new HashSet<>();
        if (report.isSafe() || report.getViolationTimestamps() == null) {
            return unsafeIndexes;
        }

        int framesPerSecond = (int) Math.round(fps);

        for (double violationSecond : report.getViolationTimestamps()) {
            int startFrame = (int)(violationSecond * fps);
            int endFrame = Math.min(totalFrames, startFrame + framesPerSecond);
            for (int i = startFrame; i < endFrame; i++) {
                unsafeIndexes.add(i);
            }
        }
        return unsafeIndexes;
    }

    private Frame deepCopyFrame(Frame source) {
        Frame copy = new Frame(source.imageWidth, source.imageHeight,
                source.imageDepth, source.imageChannels);
        for (int i = 0; i < source.image.length; i++) {
            java.nio.ByteBuffer src = (java.nio.ByteBuffer) source.image[i];
            java.nio.ByteBuffer dst = (java.nio.ByteBuffer) copy.image[i];
            dst.put(src.duplicate());
            dst.flip();
        }
        return copy;
    }

    private void configureRecorderEncoding(FFmpegFrameRecorder recorder, FFmpegFrameGrabber grabber) throws Exception {
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setVideoBitrate(grabber.getVideoBitrate() > 0 ? grabber.getVideoBitrate() : 2_000_000);
        recorder.setSampleRate(grabber.getSampleRate());
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioBitrate(grabber.getAudioBitrate() > 0 ? grabber.getAudioBitrate() : 128_000);
    }

    private void validateInputFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException("Input video target cannot be resolved: " + path);
        }
    }
}