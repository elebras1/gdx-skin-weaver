package io.github.elebras1.gdxskinweaver.skin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsMissingDrawablesAndFonts() throws IOException {
        Path atlas = writeAtlas("""
                ui.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                good
                bounds:0,0,10,10
                """);
        Path skin = writeSkin("""
                {
                  "com.badlogic.gdx.graphics.g2d.BitmapFont": {
                    "font_a": {"file": "font_a.fnt"}
                  },
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok": {"up": "good", "down": "missing_drawable"}
                  },
                  "com.badlogic.gdx.scenes.scene2d.ui.TextButton$TextButtonStyle": {
                    "bad": {"up": "good", "font": "missing_font"}
                  }
                }
                """);

        List<String> warnings = new SkinValidator().validate(skin.toFile(), atlas.toFile());

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing_drawable")));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("missing_font")));
        assertFalse(warnings.stream().anyMatch(warning -> warning.contains("'good'")));
    }

    @Test
    void reportsDuplicateAtlasRegionsWithoutIndexes() throws IOException {
        Path atlas = writeAtlas("""
                ui.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                panel
                bounds:0,0,10,10

                panel
                bounds:10,0,10,10
                """);
        Path skin = writeSkin("{}");

        List<String> warnings = new SkinValidator().validate(skin.toFile(), atlas.toFile());

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("Duplicate atlas region 'panel'")));
    }

    @Test
    void ignoresIndexedAnimationFrames() throws IOException {
        Path atlas = writeAtlas("""
                ui.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                anim
                index:0
                bounds:0,0,10,10

                anim
                index:1
                bounds:10,0,10,10
                """);
        Path skin = writeSkin("{}");

        List<String> warnings = new SkinValidator().validate(skin.toFile(), atlas.toFile());

        assertFalse(warnings.stream().anyMatch(warning -> warning.contains("Duplicate atlas region")));
    }

    @Test
    void warnsWhenStylesExistWithoutAnAtlas() throws IOException {
        Path skin = writeSkin("""
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok": {"up": "somewhere"}
                  }
                }
                """);

        List<String> warnings = new SkinValidator().validate(skin.toFile(), null);

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("No atlas found")));
    }

    private Path writeAtlas(String content) throws IOException {
        Path atlas = tempDir.resolve("ui.atlas");
        Files.writeString(atlas, content, StandardCharsets.UTF_8);
        return atlas;
    }

    private Path writeSkin(String content) throws IOException {
        Path skin = tempDir.resolve("ui.json");
        Files.writeString(skin, content, StandardCharsets.UTF_8);
        return skin;
    }
}
