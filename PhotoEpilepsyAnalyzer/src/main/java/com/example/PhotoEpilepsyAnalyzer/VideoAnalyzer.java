package com.example.PhotoEpilepsyAnalyzer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class VideoAnalyzer {
    AnalysisReport report;


    public static void extractFrames(String videoPath, String outputDirPath) throws Exception {
        Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Initialize the grabber with the video source file
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();

            // Java2DFrameConverter is safely reusable within the loop thread
            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            int frameCounter = 0;

            System.out.println("Total frames expected: " + grabber.getLengthInFrames());

            // Loop through each frame in the video stream sequence
            while ((frame = grabber.grabImage()) != null) {
                // Ensure the frame contains actual layout/pixel image data
                if (frame.image != null) {
                    BufferedImage bufferedImage = converter.convert(frame);

                    String fileName = String.format("frame_%05d.png", frameCounter);
                    File outputFile = outputDir.resolve(fileName).toFile();

                    // Write to file system using standard ImageIO
                    ImageIO.write(bufferedImage, "png", outputFile);
                    frameCounter++;
                }
            }

            grabber.stop();
            System.out.println("Extraction finished. Extracted " + frameCounter + " frames.");
        }
    }
        
    public List<Double> extractLuminanceProfile(String videoPath) {
        return List.of();
    }

    public AnalysisReport evaluateLuminanceProfile(List<Double> unsafeHistory, double fps) {
        return null;
    }
}
