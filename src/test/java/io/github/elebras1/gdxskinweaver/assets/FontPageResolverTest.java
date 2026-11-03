package io.github.elebras1.gdxskinweaver.assets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
