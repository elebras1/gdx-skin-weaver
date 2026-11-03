package io.github.elebras1.gdxskinweaver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TexturePackerServiceTest {

    private static final Map<String, String> SMALL_PAGES = Map.of(
            "maxWidth", "32",
            "maxHeight", "32",
            "silent", "true");

    @TempDir
    Path tempDir;

    @Test
    void repackDoesNotLeaveStalePages() throws IOException {
        Path sourceDir = Files.createDirectories(tempDir.resolve("source"));
        Path targetDir = Files.createDirectories(tempDir.resolve("target"));
        List<java.io.File> images = List.of(
                writePng(sourceDir, "a.png"),
                writePng(sourceDir, "b.png"),
                writePng(sourceDir, "c.png"));

        TexturePackerService service = new TexturePackerService();
        service.pack(sourceDir.toFile(), targetDir.toFile(), "fonts", images, SMALL_PAGES);
        service.pack(sourceDir.toFile(), targetDir.toFile(), "fonts", images, SMALL_PAGES);

        assertEquals(Set.of("fonts.atlas", "fonts.png", "fonts2.png", "fonts3.png"), fileNames(targetDir));
        assertEquals(Set.of("fonts.png", "fonts2.png", "fonts3.png"), atlasPages(targetDir.resolve("fonts.atlas")));
    }

    @Test
    void repackDoesNotLeaveStalePagesWhenAtlasNameEndsWithDigit() throws IOException {
        Path sourceDir = Files.createDirectories(tempDir.resolve("source"));
        Path targetDir = Files.createDirectories(tempDir.resolve("target"));
        List<java.io.File> images = List.of(writePng(sourceDir, "a.png"), writePng(sourceDir, "b.png"));

        TexturePackerService service = new TexturePackerService();
        service.pack(sourceDir.toFile(), targetDir.toFile(), "folder1", images, SMALL_PAGES);
        service.pack(sourceDir.toFile(), targetDir.toFile(), "folder1", images, SMALL_PAGES);

        assertEquals(Set.of("folder1.atlas", "folder1.png", "folder1-2.png"), fileNames(targetDir));
    }

    @Test
    void cleanRemovesOnlyPagesOfTheAtlas() throws IOException {
        Path targetDir = Files.createDirectories(tempDir.resolve("target"));
        for (String name : List.of("fonts.atlas", "fonts.png", "fonts2.png", "fonts7.png",
                "fonts_extra.png", "fontsx.png", "arial.png", "other.atlas")) {
            Files.writeString(targetDir.resolve(name), "");
        }

        new TexturePackerService().clean(targetDir.toFile(), "fonts", Map.of());

        assertEquals(Set.of("fonts_extra.png", "fontsx.png", "arial.png", "other.atlas"), fileNames(targetDir));
    }

    @Test
    void cleanKeepsUndashedNamesWhenAtlasNameEndsWithDigit() throws IOException {
        Path targetDir = Files.createDirectories(tempDir.resolve("target"));
        for (String name : List.of("folder1.atlas", "folder1.png", "folder1-2.png", "folder12.png")) {
            Files.writeString(targetDir.resolve(name), "");
        }

        new TexturePackerService().clean(targetDir.toFile(), "folder1", Map.of());

        assertEquals(Set.of("folder12.png"), fileNames(targetDir));
    }

    private static java.io.File writePng(Path dir, String name) throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(name.hashCode()));
        graphics.fillRect(0, 0, 20, 20);
        graphics.dispose();
        Path file = dir.resolve(name);
        ImageIO.write(image, "png", file.toFile());
        return file.toFile();
    }

    private static Set<String> fileNames(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(path -> path.getFileName().toString()).collect(Collectors.toSet());
        }
    }

    private static Set<String> atlasPages(Path atlas) throws IOException {
        return Files.readAllLines(atlas).stream()
                .map(String::trim)
                .filter(line -> line.endsWith(".png"))
                .collect(Collectors.toSet());
    }
}
