package com.wetcoding.cardrecognizer.perceptual;

import java.awt.image.BufferedImage;

public interface PerceptualHashProcessor {
    String calculateHash(BufferedImage image);

    boolean areSimilar(String hash1, String hash2);
}
