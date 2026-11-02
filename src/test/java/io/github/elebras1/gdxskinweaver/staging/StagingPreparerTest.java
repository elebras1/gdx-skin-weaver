package io.github.elebras1.gdxskinweaver.staging;

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

    private StagingPreparer.StagingResult prepare(Path assetsRoot, Path sourceDir) {
        return new StagingPreparer().prepare(sourceDir.toFile(), tempDir.resolve("staging").toFile(), assetsRoot, List.of());
    }

    private List<String> fileNames(StagingPreparer.StagingResult result) {
        return result.images().stream().map(file -> file.getName()).collect(Collectors.toList());
    }

    private Set<String> fileNameSet(StagingPreparer.StagingResult result) {
        return result.images().stream().map(file -> file.getName()).collect(Collectors.toSet());
    }

    private void writePng(Path dir, String name) throws IOException {
        Path file = dir.resolve(name);
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", file.toFile());
    }
}
