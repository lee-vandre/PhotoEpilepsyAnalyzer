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


    @Nested
    @DisplayName("Video Frame Extraction Tests")
    class FrameExtractionTests {

        @Test
        @DisplayName("Should successfully extract frames from a valid video file")
        void testFramesAreBeingExtracted() {
            // Given: A small, valid test video file
            // Note: In a real project, place a 1-second sample video in src/test/resources/sample.mp4
            String videoPath = "src/test/resources/sample.mp4";
            File file = new File(videoPath);

            // Guard rail for the test execution environment
            if (!file.exists()) {
                System.out.println("Skipping integration test: Put a real sample.mp4 in src/test/resources/");
                return;
            }

            // When: Extracting frame brightness profiles
            List<Double> extractedLuminance = videoAnalyzer.extractLuminanceProfile(videoPath);

            // Then: The list should not be empty, proving FFmpeg successfully grabbed images
            assertThat(extractedLuminance).isNotEmpty();
            assertThat(extractedLuminance.size()).isGreaterThan(0);
        }
    }


    @Nested
    @DisplayName("Flash Detection Threshold Tests")
    class FlashDetectionTests {

        private final double FPS = 30.0;

        @Test
        @DisplayName("Should FAIL check when video contains multiple flashes (>3) in 1 second")
        void testVideoWithMultipleFlashesInOneSecondWillFail() {
            // Given: A 1-second frame sequence with 4 violent brightness spikes
            List<Double> unsafeHistory = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                unsafeHistory.add(10.0);  // Dark frame
                unsafeHistory.add(250.0); // Blindingly bright frame (Huge Delta)
            }
            while (unsafeHistory.size() < 30) {
                unsafeHistory.add(10.0); // Pad out the rest of the second
            }

            // When: Running safety check
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(unsafeHistory, FPS);

            // Then: It must be marked as unsafe
            assertThat(report.isSafe()).isFalse();
            assertThat(report.getViolationTimestamps()).isNotEmpty();
        }

        @Test
        @DisplayName("Should PASS check when video has consistent or smooth lighting")
        void testVideoWithoutFlashesWillPass() {
            // Given: A 1-second frame sequence with perfectly stable lighting
            List<Double> safeHistory = new ArrayList<>();

            for (int i = 0; i < 30; i++) {
                safeHistory.add(128.0); // Flat medium gray
            }

            // When: Running safety check
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(safeHistory, FPS);

            // Then: It must pass seamlessly
            assertThat(report.isSafe()).isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }
    }


    @Nested
    @DisplayName("Video Cleanser Pipeline Tests")
    class CleanserPipelineTests {

        private final double FPS = 30.0;

        @Test
        @DisplayName("Should PASS safety check after an unsafe profile has been cleaned")
        void testVideoPassesCheckOnceEditedByVideoCleanser() {
            // Given: A known highly flashing unsafe luminance profile
            List<Double> unsafeHistory = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                unsafeHistory.add(10.0);
                unsafeHistory.add(250.0);
            }

            // When: We run it through the Cleanser class to clamp extreme spikes
            List<Double> cleansedHistory = videoCleanser.cleanseLuminanceSpikes(unsafeHistory);

            // Analyze the modified output
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(cleansedHistory, FPS);

            // Then: The cleanser should have smoothed out the shifts, making it pass
            assertThat(report.isSafe()).isTrue();
            assertThat(report.getViolationTimestamps()).isEmpty();
        }
    }


    @Nested
    @DisplayName("Defensive Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Forgotten Case 1: Handle broken, missing, or corrupted video files gracefully")
        void testInvalidFilePathThrowsException() {
            // Given: A path to a non-existent file
            String deadPath = "src/test/resources/ghost_file.mp4";

            // When & Then: The system shouldn't crash with an obscure native error,
            // it should throw an explicit Java exception that your API can handle with a 400 Bad Request.
            assertThatThrownBy(() -> videoAnalyzer.extractLuminanceProfile(deadPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Video file not found or unreadable");
        }

        @Test
        @DisplayName("Forgotten Case 2: Handle massive extreme inputs (Sustained Strobing over a long video)")
        void testLongVideoWithHundredsOfFlashesDoesNotBufferOverflow() {
            // Given: A long 2-minute video profile containing non-stop continuous flashes
            double fps = 30.0;
            List<Double> massiveHistory = new ArrayList<>();
            for (int frame = 0; frame < 3600; frame++) { // 3600 frames = 2 minutes
                massiveHistory.add(frame % 2 == 0 ? 15.0 : 245.0);
            }

            // When: Evaluating huge arrays
            AnalysisReport report = videoAnalyzer.evaluateLuminanceProfile(massiveHistory, fps);

            // Then: It catches the violation, doesn't lock up memory, and correctly limits reported timestamps
            assertThat(report.isSafe()).isFalse();
            assertThat(report.getViolationTimestamps().size()).isGreaterThan(0);
        }
    }
}