package com.example.PhotoEpilepsyAnalyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoEpilepsyAnalyzerTest {

    private VideoAnalyzer videoAnalyzer;
    private VideoCleanser videoCleanser;

    @BeforeEach
    void setUp() {
        videoAnalyzer = new VideoAnalyzer();
        videoCleanser = new VideoCleanser();
    }

    // =========================================================================
    // STAGE 1: INPUT VALIDATION & FILE SYSTEM EDGE CASES
    // =========================================================================
    @Nested
    @DisplayName("1. Defensive Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Handle broken, missing, or corrupted video files gracefully")
        void testInvalidFilePathThrowsException() {
            String deadPath = "src/test/resources/ghost_file.mp4";

            assertThatThrownBy(() -> videoAnalyzer.extractLuminanceProfile(deadPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Video file not found or unreadable");
        }
    }

    // =========================================================================
    // STAGE 2: NATIVE FRAME SAMPLING & EXTRACTION
    // =========================================================================
    @Nested
    @DisplayName("2. Video Frame Extraction Tests")
    class FrameExtractionTests {

        @Test
        @DisplayName("Should successfully extract frames from a valid video file")
        void testFramesAreBeingExtracted() {
            String videoPath = "src/test/resources/flashingVideos/sample.mp4";
            File file = new File(videoPath);

            if (!file.exists()) {
                System.out.println("Skipping integration test: Put a real sample.mp4 in src/test/resources/");
                return;
            }

            List<Double> extractedLuminance = videoAnalyzer.extractLuminanceProfile(videoPath);

            assertThat(extractedLuminance).isNotEmpty();
            assertThat(extractedLuminance.size()).isGreaterThan(0);
        }
    }

    // =========================================================================
    // STAGE 3: BASELINE STABILITY VALIDATION (SAFE DATA CONTROL)
    // =========================================================================
    // =========================================================================
    // STAGE 3: BASELINE STABILITY VALIDATION (REAL SAFE DATA CONTROL)
    // =========================================================================
    @Nested
    @DisplayName("3. Control Video Baseline Tests")
    class ControlVideoTests {

        // Hardcoded fallback frame rate for evaluation if the mock file is short
        private final double DEFAULT_FPS = 30.0;

        @Test
        @DisplayName("Control 1: Should PASS when analyzing a real, solid white video file")
        void testPureWhiteVideoIsSafe() {
            // Given: A real video file that contains static white frames
            String videoPath = "src/test/resources/controlvideos/PlainWhiteVideo.mp4";
            File file = new File(videoPath);

            if (!file.exists()) {
                System.out.println("⚠️ SKIPPING TEST: Please place 'pure_white.mp4' in 'src/test/resources/controlVideos/'");
                return;
            }

            // When: Extracting the real profile and evaluating it
            List<Double> luminanceProfile = videoAnalyzer.extractLuminanceProfile(videoPath);
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(luminanceProfile, DEFAULT_FPS);

            // Then: High brightness is safe if it does not bounce dynamically
            assertThat(report.isSafe())
                    .as("A constant white video has no luminance deltas and must be marked safe")
                    .isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }

        @Test
        @DisplayName("Control 2: Should PASS when analyzing a real, solid black video file")
        void testPureBlackVideoIsSafe() {
            // Given: A real video file that contains static black frames
            String videoPath = "src/test/resources/controlvideos/PlainBlackVideo.mp4";
            File file = new File(videoPath);

            if (!file.exists()) {
                System.out.println("⚠️ SKIPPING TEST: Please place 'pure_black.mp4' in 'src/test/resources/controlVideos/'");
                return;
            }

            // When: Extracting the real profile and evaluating it
            List<Double> luminanceProfile = videoAnalyzer.extractLuminanceProfile(videoPath);
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(luminanceProfile, DEFAULT_FPS);

            // Then: Consistent black frames must easily pass
            assertThat(report.isSafe())
                    .as("A constant black video has no luminance shifts and must be marked safe")
                    .isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }

        @Test
        @DisplayName("Control 3: Should PASS when analyzing a real podcast video (low motion visual data)")
        void testPodcastVideoIsSafe() {
            // Given: A real sample clip of a podcast conversation
            String videoPath = "src/test/resources/controlvideos/podcastvid.mp4";
            File file = new File(videoPath);

            if (!file.exists()) {
                System.out.println("⚠️ SKIPPING TEST: Please place 'podcast_sample.mp4' in 'src/test/resources/controlVideos/'");
                return;
            }

            // When: Running the extraction and safety rule check
            List<Double> luminanceProfile = videoAnalyzer.extractLuminanceProfile(videoPath);
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(luminanceProfile, DEFAULT_FPS);

            // Then: Minor natural movements should register below your FLASH_THRESHOLD (20.0)
            assertThat(report.isSafe())
                    .as("A podcast contains small sub-threshold facial fluctuations and must pass seamlessly")
                    .isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }
    }

    // =========================================================================
    // STAGE 4: HIGH-FREQUENCY FLASH DETECTOR RULE MATH
    // =========================================================================
    @Nested
    @DisplayName("4. Flash Detection Threshold Tests")
    class FlashDetectionTests {

        private final double FPS = 30.0;

        @Test
        @DisplayName("Should FAIL check when video contains multiple flashes (>3) in 1 second")
        void testVideoWithMultipleFlashesInOneSecondWillFail() {
            List<Double> unsafeHistory = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                unsafeHistory.add(10.0);
                unsafeHistory.add(250.0);
            }
            while (unsafeHistory.size() < 30) {
                unsafeHistory.add(10.0);
            }

            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(unsafeHistory, FPS);

            assertThat(report.isSafe()).isFalse();
            assertThat(report.getViolationTimestamps()).isNotEmpty();
        }

        @Test
        @DisplayName("Should PASS check when video has consistent or smooth lighting")
        void testVideoWithoutFlashesWillPass() {
            List<Double> safeHistory = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                safeHistory.add(128.0);
            }

            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(safeHistory, FPS);

            assertThat(report.isSafe()).isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }

        @Test
        @DisplayName("Handle massive extreme inputs (Sustained Strobing over a long video) without locking")
        void testLongVideoWithHundredsOfFlashesDoesNotBufferOverflow() {
            double fps = 30.0;
            List<Double> massiveHistory = new ArrayList<>();
            for (int frame = 0; frame < 3600; frame++) {
                massiveHistory.add(frame % 2 == 0 ? 15.0 : 245.0);
            }

            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(massiveHistory, fps);

            assertThat(report.isSafe()).isFalse();
            assertThat(report.getViolationTimestamps().size()).isGreaterThan(0);
        }
    }

    // =========================================================================
    // STAGE 5: POST-PROCESSING MITIGATION PIPELINE
    // =========================================================================
    @Nested
    @DisplayName("5. Video Cleanser Pipeline Tests")
    class CleanserPipelineTests {

        private final double FPS = 30.0;

        @Test
        @DisplayName("Should PASS safety check after an unsafe profile has been cleaned")
        void testVideoPassesCheckOnceEditedByVideoCleanser() {
            List<Double> unsafeHistory = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                unsafeHistory.add(10.0);
                unsafeHistory.add(250.0);
            }

            List<Double> cleansedHistory = videoCleanser.cleanseLuminanceSpikes(unsafeHistory);
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(cleansedHistory, FPS);

            assertThat(report.isSafe()).isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }
    }
}