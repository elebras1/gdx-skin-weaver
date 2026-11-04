package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.atlas.AtlasRegionReader;
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

    @Test
    void packsMultiPageFontUnderPageIds() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path fonts = Files.createDirectories(assets.resolve("fonts"));
        String[] pageFiles = new String[11];
        for (int i = 0; i < pageFiles.length; i++) {
            pageFiles[i] = "raw" + i + ".png";
            writePng(fonts.resolve(pageFiles[i]), 8 + i);
        }
        writeFont(fonts.resolve("chinese_60.fnt"), pageFiles);
        Path output = tempDir.resolve("out");

        processor().process(assets.toFile(), context(assets, output, Set.of(), true));

        AtlasRegionReader.AtlasRegions regions = new AtlasRegionReader()
                .read(output.resolve("fonts/fonts.atlas").toFile());
        for (int i = 0; i < pageFiles.length; i++) {
            int page = i;
            assertTrue(regions.names().contains("chinese_60_" + page), () -> "Missing page " + page);
        }
        assertTrue(regions.indexedNames().contains("chinese_60"));
    }

    @Test
    void packsSinglePageFontUnderFontName() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path fonts = Files.createDirectories(assets.resolve("fonts"));
        writePng(fonts.resolve("raw.png"));
        writeFont(fonts.resolve("arial_17.fnt"), "raw.png");
        Path output = tempDir.resolve("out");

        processor().process(assets.toFile(), context(assets, output, Set.of(), true));

        AtlasRegionReader.AtlasRegions regions = new AtlasRegionReader()
                .read(output.resolve("fonts/fonts.atlas").toFile());
        assertTrue(regions.names().contains("arial_17"));
        assertFalse(regions.names().contains("raw"));
    }

    @Test
    void packsSharedFontPageForEveryFont() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path fonts = Files.createDirectories(assets.resolve("fonts"));
        writePng(fonts.resolve("shared.png"));
        writeFont(fonts.resolve("first.fnt"), "shared.png");
        writeFont(fonts.resolve("second.fnt"), "shared.png");
        Path output = tempDir.resolve("out");

        processor().process(assets.toFile(), context(assets, output, Set.of(), true));

        AtlasRegionReader.AtlasRegions regions = new AtlasRegionReader()
                .read(output.resolve("fonts/fonts.atlas").toFile());
        assertTrue(regions.names().containsAll(Set.of("first", "second")));
        assertFalse(regions.names().contains("shared"));
    }

    @Test
    void removesPackedFontPageCopiesWhenTheirCopyIsDisabled() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path fonts = Files.createDirectories(assets.resolve("fonts"));
        writePng(fonts.resolve("raw.png"));
        writeFont(fonts.resolve("arial_17.fnt"), "raw.png");
        Path output = tempDir.resolve("out");
        Files.createDirectories(output.resolve("fonts"));
        writePng(output.resolve("fonts/raw.png"));

        processor().process(assets.toFile(), context(assets, output, true, false));

        assertTrue(Files.exists(output.resolve("fonts/arial_17.fnt")));
        assertFalse(Files.exists(output.resolve("fonts/raw.png")));
        assertTrue(new AtlasRegionReader().read(output.resolve("fonts/fonts.atlas").toFile()).names()
                .contains("arial_17"));
    }

    @Test
    void copiesFontPagesThatAreNotPackedEvenWhenPackedCopiesAreDisabled() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        Path fonts = Files.createDirectories(assets.resolve("fonts"));
        writePng(fonts.resolve("raw.png"));
        writeFont(fonts.resolve("arial_17.fnt"), "raw.png");
        Path output = tempDir.resolve("out");

        processor().process(assets.toFile(), context(assets, output, false, false));

        assertTrue(Files.exists(output.resolve("fonts/raw.png")));
    }

    private WeaveContext context(Path assets, Path output, boolean packFontPages, boolean copyPackedFontPages) {
        return new WeaveContext(assets, output.toFile(), Set.of(), tempDir.resolve("staging").toFile(), Map.of(),
                packFontPages, copyPackedFontPages, SkinGenerationConfig.defaults());
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

    private void writeFont(Path file, String... pageFiles) throws IOException {
        StringBuilder pageLines = new StringBuilder();
        for (int i = 0; i < pageFiles.length; i++) {
            if (i > 0) {
                pageLines.append('\n');
            }
            pageLines.append("page id=").append(i).append(" file=\"").append(pageFiles[i]).append('"');
        }
        Files.writeString(file, """
                info face="Test" size=8 bold=0 italic=0 charset="" unicode=0 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0
                common lineHeight=8 base=8 scaleW=8 scaleH=8 pages=%d packed=0
                %s
                chars count=0
                """.formatted(pageFiles.length, pageLines), StandardCharsets.UTF_8);
    }

    private void writePng(Path file) throws IOException {
        writePng(file, 8);
    }

    private void writePng(Path file, int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
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
