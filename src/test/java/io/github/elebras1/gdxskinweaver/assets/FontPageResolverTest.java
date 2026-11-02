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
