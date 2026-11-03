package io.github.elebras1.gdxskinweaver.staging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagingPreparerTest {

    @TempDir
    Path tempDir;

    @Test
    void prefersNinePatchVariantWithTheSameStem() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("ui");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "tiles_dialog.png");
        writePng(sourceDir, "tiles_dialog.9.png");

        StagingPreparer.StagingResult result = prepare(assetsRoot, sourceDir);

        assertEquals(List.of("tiles_dialog.9.png"), fileNames(result));
    }

    @Test
    void keepsManualButtonVariantsWithoutDuplicates() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("ui");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "foo_btn.png");
        writePng(sourceDir, "foo_btn_down.png");
        writePng(sourceDir, "foo_btn_over.png");

        StagingPreparer.StagingResult result = prepare(assetsRoot, sourceDir);

        assertEquals(Set.of("foo_btn.png", "foo_btn_down.png", "foo_btn_over.png"), fileNameSet(result));
        assertTrue(result.buttonSimpleToFull().containsKey("foo_btn"));
    }

    @Test
    void generatesMissingButtonVariants() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("ui");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "bar_btn.png");

        StagingPreparer.StagingResult result = prepare(assetsRoot, sourceDir);

        assertEquals(Set.of("bar_btn.png", "bar_btn_down.png", "bar_btn_over.png"), fileNameSet(result));
        assertTrue(result.buttonSimpleToFull().containsKey("bar_btn"));
    }

    @Test
    void generatesToggleStates() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("ui");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "baz_tgl.png");

        StagingPreparer.StagingResult result = prepare(assetsRoot, sourceDir);

        assertEquals(Set.of("baz_tgl_off.png", "baz_tgl_on.png"), fileNameSet(result));
        assertTrue(result.toggleSimpleToFull().containsKey("baz_tgl"));
    }

    @Test
    void preservesNinePatchWhenGeneratingButtonVariants() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("ui");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "nine_btn.9.png");

        StagingPreparer.StagingResult result = prepare(assetsRoot, sourceDir);

        assertEquals(Set.of("nine_btn.9.png", "nine_btn_down.9.png", "nine_btn_over.9.png"), fileNameSet(result));
    }

    @Test
    void usesManualNinePatchButtonVariants() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("ui");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "nine_btn.9.png");
        writePng(sourceDir, "nine_btn_down.9.png");

        StagingPreparer.StagingResult result = prepare(assetsRoot, sourceDir);

        assertEquals(Set.of("nine_btn.9.png", "nine_btn_down.9.png", "nine_btn_over.9.png"), fileNameSet(result));
    }

    @Test
    void excludesFontPagesFromPacking() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("fonts");
        Files.createDirectories(sourceDir);
        writePng(sourceDir, "arial_14.png");
        writePng(sourceDir, "other.png");

        StagingPreparer.StagingResult result = new StagingPreparer().prepare(
                sourceDir.toFile(), tempDir.resolve("staging").toFile(), assetsRoot,
                List.of(sourceDir.resolve("arial_14.png").toFile()));

        assertEquals(Set.of("other.png"), fileNameSet(result));
    }

    @Test
    void keepsRenamedFontPageWhenAnotherImageUsesItsRegionName() throws IOException {
        Path assetsRoot = tempDir.resolve("assets");
        Path sourceDir = assetsRoot.resolve("fonts");
        Files.createDirectories(sourceDir);
        Path page = writePng(sourceDir, "page.png", 8, Color.RED);
        writePng(sourceDir, "font_0.png", 16, Color.BLUE);
        Path stagingRoot = tempDir.resolve("staging");

        String err = captureErr(() -> new StagingPreparer().prepare(sourceDir.toFile(), stagingRoot.toFile(), assetsRoot,
                List.of(), Map.of(page.toFile(), List.of("font_0"))));

        Path staged = stagingRoot.resolve("fonts/font_0.png");
        assertTrue(Files.exists(staged));
        BufferedImage image = ImageIO.read(staged.toFile());
        assertEquals(8, image.getWidth());
        assertTrue(err.contains("collides with font page"));
    }

    private StagingPreparer.StagingResult prepare(Path assetsRoot, Path sourceDir) {
        return new StagingPreparer().prepare(sourceDir.toFile(), tempDir.resolve("staging").toFile(), assetsRoot, List.of());
    }

    private List<String> fileNames(StagingPreparer.StagingResult result) {
        return result.images().stream().map(file -> file.getName()).collect(Collectors.toList());
    }

    private Set<String> fileNameSet(StagingPreparer.StagingResult result) {
        return result.images().stream().map(file -> file.getName()).collect(Collectors.toSet());
    }

    private Path writePng(Path dir, String name) throws IOException {
        return writePng(dir, name, 8, Color.RED);
    }

    private Path writePng(Path dir, String name, int size, Color color) throws IOException {
        Path file = dir.resolve(name);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private String captureErr(ThrowingRunnable action) {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            System.setErr(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws IOException;
    }
}
