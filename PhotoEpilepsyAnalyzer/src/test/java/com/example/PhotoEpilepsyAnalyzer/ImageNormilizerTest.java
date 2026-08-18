//package com.example.PhotoEpilepsyAnalyzer;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//
//import java.awt.image.BufferedImage;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class ImageNormilizerTest {
//
//    // Helper to generate a solid 2x2 image of a single specific color
//    private BufferedImage createSolidImage(int width, int height, int r, int g, int b) {
//        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        int rgb = (r << 16) | (g << 8) | b;
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                img.setRGB(x, y, rgb);
//            }
//        }
//        return img;
//    }
//
//    @Nested
//    @DisplayName("Unit Tests: Pixel Correction Logic")
//    class PixelCorrectionTests {
//
//        @Test
//        @DisplayName("Should pass color through unchanged if delta is below MAX_CHANGE")
//        void testBelowThresholdStaysUnchanged() {
//            // Given: A minor step change of +1 unit per channel (within max limit of 2)
//            BufferedImage base = createSolidImage(2, 2, 100, 100, 100);
//            BufferedImage target = createSolidImage(2, 2, 101, 101, 101);
//
//            // When: Normalizing
//            BufferedImage normalized = ImageNormalizer.normalizeImage(base, target);
//
//            // Then: The final image should accurately match the target value
//            int pixelRgb = normalized.getRGB(0, 0);
//            int r = (pixelRgb >> 16) & 0xFF;
//            int g = (pixelRgb >> 8) & 0xFF;
//            int b = pixelRgb & 0xFF;
//
//            assertThat(r).isEqualTo(101);
//            assertThat(g).isEqualTo(101);
//            assertThat(b).isEqualTo(101);
//        }
//
//        @Test
//        @DisplayName("Should clamp massive positive shifts down to exactly +MAX_CHANGE")
//        void testMassivePositiveShiftClamped() {
//            // Given: An aggressive visual flash change (100 -> 250)
//            BufferedImage base = createSolidImage(2, 2, 100, 100, 100);
//            BufferedImage target = createSolidImage(2, 2, 250, 250, 250);
//
//            // When: Normalizing
//            BufferedImage normalized = ImageNormalizer.normalizeImage(base, target);
//
//            // Then: The change must be restricted to base + 2 = 102
//            int pixelRgb = normalized.getRGB(0, 0);
//            int r = (pixelRgb >> 16) & 0xFF;
//
//            assertThat(r).isEqualTo(102);
//        }
//
//        @Test
//        @DisplayName("Should clamp massive negative shifts down to exactly -MAX_CHANGE")
//        void testMassiveNegativeShiftClamped() {
//            // Given: A massive drop into sudden darkness (100 -> 10)
//            BufferedImage base = createSolidImage(2, 2, 100, 100, 100);
//            BufferedImage target = createSolidImage(2, 2, 10, 10, 10);
//
//            // When: Normalizing
//            BufferedImage normalized = ImageNormalizer.normalizeImage(base, target);
//
//            // Then: The change must be restricted to base - 2 = 98
//            int pixelRgb = normalized.getRGB(0, 0);
//            int r = (pixelRgb >> 16) & 0xFF;
//
//            assertThat(r).isEqualTo(98);
//        }
//    }
//
//    @Nested
//    @DisplayName("Integration Tests: Structural Image Transformation")
//    class ImageIntegrationTests {
//
//        @Test
//        @DisplayName("Should dynamically align to the smallest bounding dimensions when images mismatch")
//        void testMismatchedDimensionsHandling() {
//            // Given: An image of 4x4 matching an image of 2x6
//            BufferedImage image1 = createSolidImage(4, 4, 128, 128, 128);
//            BufferedImage image2 = createSolidImage(2, 6, 128, 128, 128);
//
//            // When: Processing the matrices
//            BufferedImage result = ImageNormalizer.normalizeImage(image1, image2);
//
//            // Then: Dimensions must shrink to the minimum common boundary (Width: 2, Height: 4)
//            assertThat(result.getWidth()).isEqualTo(2);
//            assertThat(result.getHeight()).isEqualTo(4);
//        }
//
//        @Test
//        @DisplayName("Should properly verify structural alterations across non-uniform pixels")
//        void testNonUniformIntegrationChanges() {
//            // Given: A base image and a frame with safe pixels on the left, unsafe on the right
//            BufferedImage base = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
//            base.setRGB(0, 0, (100 << 16)); // Pixel (0,0): Red = 100
//            base.setRGB(1, 0, (100 << 16)); // Pixel (1,0): Red = 100
//
//            BufferedImage target = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
//            target.setRGB(0, 0, (101 << 16)); // Pixel (0,0): Safe shift (+1) -> target 101
//            target.setRGB(1, 0, (200 << 16)); // Pixel (1,0): Flashing shift (+100) -> clamped to 102
//
//            // When: Normalizing the image frame combo
//            BufferedImage result = ImageNormalizer.normalizeImage(base, target);
//
//            // Then: Confirm changes were applied properly across discrete channels
//            int leftPixelR = (result.getRGB(0, 0) >> 16) & 0xFF;
//            int rightPixelR = (result.getRGB(1, 0) >> 16) & 0xFF;
//
//            assertThat(leftPixelR).isEqualTo(101);  // Kept intact
//            assertThat(rightPixelR).isEqualTo(102); // Successfully flattened/clamped
//        }
//    }
//}