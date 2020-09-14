package com.wetcoding.cardrecognizer.perceptual;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Класс для расчета перцептивного хэша (дифференицального)
 */
public class DHashProcessor implements PerceptualHashProcessor {
    private int hashSize;
    private final float minThreshold;

    public DHashProcessor(int hashSize, float minSimilarityThreshold) {
        this.hashSize = hashSize;
        this.minThreshold = minSimilarityThreshold;
    }

    @Override
    public String calculateHash(BufferedImage image) {
        BufferedImage resized = resizeImage(image);
        int[][] matrix = this.getMatrix(resized);
        return this.getHorizontalDifferences(matrix);
    }

    @Override
    public boolean areSimilar(String hash1, String hash2) {
        int distance = this.getDistance(hash1, hash2);
        int matrixSize = this.hashSize * this.hashSize;
        double similarity = 1.0 - ((double) distance / (double) matrixSize);
        return similarity > this.minThreshold;
    }

    private int[][] getMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[][] matrix = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                matrix[x][y] = image.getRGB(x, y);
            }
        }

        return matrix;
    }

    private String getHorizontalDifferences(int[][] matrix) {
        int width = matrix.length;
        int height = matrix[0].length;
        StringBuilder hash = new StringBuilder(this.hashSize * this.hashSize);

        for (int i = 0; i < width - 1; i++) {
            for (int j = 0; j < height; j++) {
                hash.append(matrix[i][j] > matrix[i + 1][j] ? "1" : "0");
            }
        }

        return hash.toString();
    }

    private BufferedImage resizeImage(BufferedImage image) {
        Image tmp = image.getScaledInstance(this.hashSize, this.hashSize, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(this.hashSize, this.hashSize, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private int getDistance(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) {
            return Integer.MAX_VALUE;
        }

        int length = hash1.length();
        int distance = 0;
        for (int i = 0; i < length; i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distance += 1;
            }
        }

        return distance;
    }
}
