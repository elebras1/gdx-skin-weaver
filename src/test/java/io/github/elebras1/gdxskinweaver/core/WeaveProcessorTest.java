package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.service.SkinJsonService;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.skin.SkinGenerationConfig;
import io.github.elebras1.gdxskinweaver.skin.SkinValidator;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaveProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void warnsWhenFontHasTheSameNameAsItsFolderWithoutPackedFontPages() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path ui = Files.createDirectories(assets.resolve("ui"));
        writePng(ui.resolve("ui.png"));
        writePng(ui.resolve("button.png"));
        writeFont(ui.resolve("ui.fnt"), "ui.png");
        Path output = tempDir.resolve("out");

        String err = captureErr(() -> processor().process(assets.toFile(), context(assets, output, Set.of(), false)));

        assertTrue(err.contains("Font 'ui' has the same name as its folder atlas"));
        assertTrue(Files.exists(output.resolve("ui/ui.atlas")));
    }

    @Test
    void skipsRootFilesButProcessesSubdirectoriesWhenRootIsExcluded() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        writePng(assets.resolve("root.png"));
        Path ui = Files.createDirectories(assets.resolve("ui"));
        writePng(ui.resolve("button.png"));
        Path output = tempDir.resolve("out");

        processor().process(assets.toFile(), context(assets, output, Set.of(""), true));

        try (Stream<Path> entries = Files.list(output)) {
            assertTrue(entries.allMatch(Files::isDirectory), "The assets root must not generate a skin or an atlas");
        }
        assertTrue(Files.exists(output.resolve("ui/ui.atlas")));
        assertTrue(Files.exists(output.resolve("ui/ui.json")));
        assertFalse(Files.exists(output.resolve("assets.atlas")));
        assertFalse(Files.exists(output.resolve("assets.json")));
    }

    private WeaveProcessor processor() {
        return new WeaveProcessor(new DirectoryScanner(), new StagingPreparer(), new FontPageCopier(),
                new TexturePackerService(), new SkinFileHandler(new SkinJsonService()), new SkinValidator());
    }

    private WeaveContext context(Path assets, Path output, Set<String> excludedDirs, boolean packFontPages) {
        return new WeaveContext(assets, output.toFile(), excludedDirs, tempDir.resolve("staging").toFile(),
                Map.of(), packFontPages, SkinGenerationConfig.defaults());
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

    private void writeFont(Path file, String pageFile) throws IOException {
        Files.writeString(file, """
                info face="Test" size=8 bold=0 italic=0 charset="" unicode=0 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0
                common lineHeight=8 base=8 scaleW=8 scaleH=8 pages=1 packed=0
                page id=0 file="%s"
                chars count=0
                """.formatted(pageFile), StandardCharsets.UTF_8);
    }

    private void writePng(Path file) throws IOException {
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws IOException;
    }
}
