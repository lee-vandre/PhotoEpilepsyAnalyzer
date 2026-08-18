package com.example.PhotoEpilepsyAnalyzer;

import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Mat;

import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.springframework.stereotype.Component;

@Component
public class NativeFrameNormalizer {

    private static final int MAX_CHANGE = 2;
    // Thread-safe converter adapter to map JavaCV Frames to OpenCV Mats
    private final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

    /**
     * Natively alters target frame pixel data by clamping changes relative to a base frame.
     * Operates directly on the underlying off-heap memory using OpenCV structures.
     */
    public void normalizeFrameInPlace(Frame baseFrame, Frame targetFrame) {
        if (baseFrame == null || targetFrame == null || targetFrame.image == null) {
            return;
        }

        // 1. Convert native frame allocations into OpenCV Matrices (zero-copy wrapper)
        Mat baseMat = matConverter.convert(baseFrame);
        Mat targetMat = matConverter.convert(targetFrame);

        // Ensure both underlying matrices share the same frame configurations
        if (baseMat.rows() != targetMat.rows() || baseMat.cols() != targetMat.cols()) {
            return;
        }

        int rows = targetMat.rows();
        int cols = targetMat.cols();

        // 2. Obtain high-performance, direct pointer indexers for raw unsigned bytes (UByte)
        // BGR layout maps index values to: [row, col, channel] (0=Blue, 1=Green, 2=Red)
        try (UByteIndexer baseIdx = baseMat.createIndexer();
             UByteIndexer targetIdx = targetMat.createIndexer()) {

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    // Loop through the 3 color channels (Blue, Green, Red)
                    for (int c = 0; c < 3; c++) {
                        int baseVal = baseIdx.get(y, x, c);
                        int targetVal = targetIdx.get(y, x, c);

                        // Apply your custom delta threshold clamp logic
                        int diff = targetVal - baseVal;
                        if (diff > MAX_CHANGE) {
                            diff = MAX_CHANGE;
                        } else if (diff < -MAX_CHANGE) {
                            diff = -MAX_CHANGE;
                        }

                        int normalizedVal = Math.max(0, Math.min(255, baseVal + diff));

                        // 3. Structural mutation: Overwrite the target buffer byte directly in memory
                        targetIdx.put(y, x, c, normalizedVal);
                    }
                }
            }
        }
    }
}