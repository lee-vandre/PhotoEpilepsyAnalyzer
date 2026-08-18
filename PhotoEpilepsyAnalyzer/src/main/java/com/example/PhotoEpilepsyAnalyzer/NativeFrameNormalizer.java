package com.example.PhotoEpilepsyAnalyzer;

import java.awt.image.BufferedImage;

public class ImageNormalizer {

    private static final int MAX_CHANGE = 2;

    public static BufferedImage normalizeImage(
            BufferedImage image1,
            BufferedImage image2) {

        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());

        BufferedImage result =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int rgb1 = image1.getRGB(x, y);
                int rgb2 = image2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                int r = limitChange(r1, r2);
                int g = limitChange(g1, g2);
                int b = limitChange(b1, b2);

                int newRgb = (r << 16) | (g << 8) | b;

                result.setRGB(x, y, newRgb);
            }
        }

        return result;
    }

    private static int limitChange(int original, int changed) {
        int diff = changed - original;

        if (diff > MAX_CHANGE) {
            diff = MAX_CHANGE;
        } else if (diff < -MAX_CHANGE) {
            diff = -MAX_CHANGE;
        }

        return clamp(original + diff);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}