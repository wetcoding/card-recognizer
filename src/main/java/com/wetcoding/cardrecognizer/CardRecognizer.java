package com.wetcoding.cardrecognizer;

import com.wetcoding.cardrecognizer.perceptual.DHashProcessor;
import com.wetcoding.cardrecognizer.perceptual.PerceptualHashProcessor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CardRecognizer {
    private final Dimension expectedImageSize = new Dimension(636, 1166);
    private final Point valueOffset = new Point(147, 590);
    private final Point suitOffset = new Point(147, 617);
    private final Dimension valueSize = new Dimension(30, 27);
    private final Dimension suitSize = new Dimension(25, 20);
    private final int cardDistance = 72;
    private final List<Color> backgroundColors = Arrays.asList(new Color(255, 255, 255), new Color(120, 120, 120));

    private final Map<String, List<String>> templates = new HashMap<>();
    private final PerceptualHashProcessor hashProcessor = new DHashProcessor(16, 0.845f);

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Illegal arguments count");
            return;
        }

        CardRecognizer cardRecognizer = new CardRecognizer();
        cardRecognizer.execute(args[0]);
    }

    public CardRecognizer() throws IOException {
        loadTemplates();
    }

    private void loadTemplates() throws IOException {
        File directory = new File("./patterns");
        for (File pDir : directory.listFiles()) {
            if (Files.isDirectory(pDir.toPath())) {
                List<String> hashes = new ArrayList<>();
                for (File pattern : pDir.listFiles()) {
                    hashes.add(hashProcessor.calculateHash(ImageIO.read(pattern)));
                }
                templates.put(pDir.getName(), hashes);
            }
        }
    }

    public void execute(String directoryPath) throws IOException {

        File directory = new File(directoryPath);

        if (!directory.exists()) {
            System.out.println(String.format("Directory [%s] does not exist", directory.getCanonicalPath()));
            return;
        }

        System.out.println(String.format("Starting  cards recognition for directory [%s]", directory.getCanonicalPath()));
        AtomicInteger unrecognized = new AtomicInteger();

        for (File file : directory.listFiles()) {
            Optional<BufferedImage> optImg = readImage(file);
            if (optImg.isPresent()) {
                BufferedImage img = optImg.get();
                System.out.print(file.getName() + " - ");
                for (int i = 0; i < 5; i++) {
                    if (!isCard(img, valueOffset.x + cardDistance * i, valueOffset.y)) {
                        break;
                    }
                    recognizeImage(img, valueOffset, cardDistance * i, valueSize)
                            .ifPresentOrElse(System.out::print, unrecognized::getAndIncrement);
                    recognizeImage(img, suitOffset, cardDistance * i, suitSize)
                            .ifPresentOrElse(System.out::print, unrecognized::getAndIncrement);
                }
                System.out.println();
            }
        }

        System.out.println("Recognition complete, unrecognised: " + unrecognized);
    }

    private Optional<BufferedImage> readImage(File file) {
        if (file.isFile()) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img.getHeight() == expectedImageSize.height && img.getWidth() == expectedImageSize.width) {
                    return Optional.of(img);
                }
            } catch (IOException e) {
                System.out.println(String.format("Can`t read file: %s", file.getName()));
            }
        }

        return Optional.empty();
    }

    /**
     * true - если цвет точки совпадает с цветом карты
     */
    private boolean isCard(BufferedImage source, int x, int y) {
        Color dotColor = new Color(source.getRGB(x, y));
        return backgroundColors.contains(dotColor);
    }

    /**
     * Распознает символ или текст на участке изображения
     */
    private Optional<String> recognizeImage(BufferedImage source, Point start, int xOffset, Dimension size) throws IOException {
        BufferedImage cropped = cropImage(source.getSubimage(start.x + xOffset, start.y, size.width, size.height));

        String hash = hashProcessor.calculateHash(cropped);
        for (Map.Entry<String, List<String>> entry : templates.entrySet()) {
            for (String template : entry.getValue())
                if (hashProcessor.areSimilar(template, hash)) {
                    return Optional.of(entry.getKey());
                }
        }
        //Сохранем нераспознанный фрагмент
        writeUnrecognizedImage(cropped);
        return Optional.empty();
    }

    private void writeUnrecognizedImage(BufferedImage image) throws IOException {
        Path rootDir = Paths.get("unrecognized");
        if (Files.notExists(rootDir)) {
            Files.createDirectory(rootDir);
        }
        ImageIO.write(image, "png", rootDir.resolve(System.currentTimeMillis() + ".png").toFile());
    }

    private BufferedImage cropImage(BufferedImage image) {
        int minY = Integer.MAX_VALUE, maxY = 0, minX = Integer.MAX_VALUE, maxX = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            boolean isBlank = true;

            for (int x = 0; x < image.getWidth(); x++) {
                if (!backgroundColors.contains(new Color(image.getRGB(x, y)))) {
                    isBlank = false;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }

            if (!isBlank) {
                if (minY == Integer.MAX_VALUE) {
                    minY = y;
                } else if (y > maxY) {
                    maxY = y;
                }
            }
        }

        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
