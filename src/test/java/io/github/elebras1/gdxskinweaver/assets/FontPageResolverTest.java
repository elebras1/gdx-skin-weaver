package io.github.elebras1.gdxskinweaver.assets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontPageResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesExistingFontPage() throws IOException {
        Path page = tempDir.resolve("arial.png");
        Files.writeString(page, "", StandardCharsets.UTF_8);
        File font = writeFont("page id=0 file=\"arial.png\"");

        Set<File> pages = new FontPageResolver().resolve(List.of(font));

        assertEquals(Set.of(page.toFile()), pages);
    }

    @Test
    void ignoresMissingFontPage() throws IOException {
        File font = writeFont("page id=0 file=\"missing.png\"");

        Set<File> pages = new FontPageResolver().resolve(List.of(font));

        assertTrue(pages.isEmpty());
    }

    @Test
    void namesMultiPageFontRegionsByPageId() throws IOException {
        Files.writeString(tempDir.resolve("big1.png"), "", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("big2.png"), "", StandardCharsets.UTF_8);
        File font = writeFont("page id=1 file=\"big2.png\"\npage id=0 file=\"big1.png\"");

        List<FontPageResolver.FontPage> pages = new FontPageResolver().resolvePages(List.of(font));

        assertEquals(List.of("font_0", "font_1"), pages.stream().map(FontPageResolver.FontPage::regionName).toList());
        assertEquals("big1.png", pages.get(0).file().getName());
    }

    @Test
    void namesSinglePageFontRegionByFontName() throws IOException {
        Files.writeString(tempDir.resolve("other.png"), "", StandardCharsets.UTF_8);
        File font = writeFont("page id=0 file=\"other.png\"");

        List<FontPageResolver.FontPage> pages = new FontPageResolver().resolvePages(List.of(font));

        assertEquals(List.of("font"), pages.stream().map(FontPageResolver.FontPage::regionName).toList());
    }

    @Test
    void sortsPageRegionsNumerically() throws IOException {
        List<String> pageLines = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            Files.writeString(tempDir.resolve("page" + i + ".png"), "", StandardCharsets.UTF_8);
            pageLines.add("page id=" + i + " file=\"page" + i + ".png\"");
        }
        File font = writeFont(String.join("\n", pageLines));

        List<FontPageResolver.FontPage> pages = new FontPageResolver().resolvePages(List.of(font));

        assertEquals(List.of("font_0", "font_1", "font_2", "font_3", "font_4", "font_5", "font_6", "font_7",
                "font_8", "font_9", "font_10"), pages.stream().map(FontPageResolver.FontPage::regionName).toList());
    }

    @Test
    void warnsWhenMultiPageFontHasNonContiguousPageIds() throws IOException {
        Files.writeString(tempDir.resolve("first.png"), "", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("third.png"), "", StandardCharsets.UTF_8);
        File font = writeFont("page id=0 file=\"first.png\"\npage id=2 file=\"third.png\"");

        String err = captureErr(() -> {
            List<FontPageResolver.FontPage> pages = new FontPageResolver().resolvePages(List.of(font));
            assertEquals(List.of("font_0", "font_2"), pages.stream().map(FontPageResolver.FontPage::regionName).toList());
        });

        assertTrue(err.contains("non-contiguous page ids"));
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

    private File writeFont(String pageLine) throws IOException {
        Path font = tempDir.resolve("font.fnt");
        String content = """
                info face="font" size=12
                common lineHeight=12 base=10 scaleW=64 scaleH=64 pages=1 packed=0
                %s
                chars count=0
                """.formatted(pageLine);
        Files.writeString(font, content, StandardCharsets.UTF_8);
        return font.toFile();
    }
}
