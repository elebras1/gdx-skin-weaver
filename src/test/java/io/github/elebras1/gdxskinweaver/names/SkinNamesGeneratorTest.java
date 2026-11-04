package io.github.elebras1.gdxskinweaver.names;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinNamesGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesNestedConstantsForSkins() throws IOException {
        Path skins = tempDir.resolve("skins");
        write(skins.resolve("ui/ui.json"), """
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1}
                  },
                  "com.badlogic.gdx.graphics.g2d.BitmapFont": {
                    "arial_14": {"file": "arial_14.fnt"}
                  },
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "settings_less_btn": {"up": "settings_less_btn"}
                  },
                  "com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle": {
                    "arial_14_red": {"font": "arial_14", "fontColor": "red"}
                  }
                }
                """);
        write(skins.resolve("ui/ui.atlas"), """
                ui.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                settings_less_btn
                bounds:0,0,10,10

                bg_province
                bounds:10,0,10,10
                """);
        write(skins.resolve("ui/economy/economy.json"), """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "eco_back_btn": {"up": "eco_back_btn"}
                  }
                }
                """);
        write(skins.resolve("fonts/fonts.json"), """
                {
                  "com.badlogic.gdx.graphics.g2d.BitmapFont": {
                    "impact": {"file": "impact.fnt"}
                  }
                }
                """);

        new SkinNamesGenerator().generate(skins.toFile(), tempDir.resolve("out").toFile(),
                "com.example.ui", "SkinNames", ".atlas");

        Path generated = tempDir.resolve("out/com/example/ui/SkinNames.java");
        String source = Files.readString(generated, StandardCharsets.UTF_8);

        assertTrue(source.contains("package com.example.ui;"));
        assertTrue(source.contains("public static final class UiSkin"));
        assertTrue(source.contains("public static final class UiEconomySkin"));
        assertTrue(source.contains("public static final class FontsSkin"));
        assertTrue(source.contains("public static final class Drawables"));
        assertTrue(source.contains("public static final String SETTINGS_LESS_BTN = \"settings_less_btn\";"));
        assertTrue(source.contains("public static final String BG_PROVINCE = \"bg_province\";"));
        assertTrue(source.contains("public static final class Styles"));
        assertTrue(source.contains("public static final class Button"));
        assertTrue(source.contains("public static final class Label"));
        assertTrue(source.contains("public static final String ARIAL_14_RED = \"arial_14_red\";"));
        assertTrue(source.contains("public static final class Fonts"));
        assertTrue(source.contains("public static final String ARIAL_14 = \"arial_14\";"));
        assertTrue(source.contains("public static final class Colors"));
        assertTrue(source.contains("public static final String RED = \"red\";"));
        assertTrue(source.contains("public static final class UiEconomySkin"));
        assertTrue(source.contains("ECO_BACK_BTN"));
        assertFalse(source.contains("AssetsSkin"));
        assertFalse(source.contains("public static final class Skins"));
    }

    @Test
    void generatesSkinCatalogWithRelativeJsonPaths() throws IOException {
        Path skins = tempDir.resolve("skins");
        write(skins.resolve("ui/ui.json"), """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok_btn": {"up": "ok_btn"}
                  }
                }
                """);
        write(skins.resolve("ui/economy/economy.json"), """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "back_btn": {"up": "back_btn"}
                  }
                }
                """);
        write(skins.resolve("fonts/fonts.json"), """
                {
                  "com.badlogic.gdx.graphics.g2d.BitmapFont": {
                    "impact": {"file": "impact.fnt"}
                  }
                }
                """);
        write(skins.resolve("settings.json"), """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "close_btn": {"up": "close_btn"}
                  }
                }
                """);
        write(skins.resolve("empty/empty.json"), "{}");
        Path out = tempDir.resolve("out");

        new SkinNamesGenerator().generate(skins.toFile(), out.toFile(),
                "com.example.ui", "SkinNames", ".atlas", true, "skins\\");

        String source = Files.readString(out.resolve("com/example/ui/SkinNames.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("public static final class Skins"));
        assertTrue(source.contains("public static final String FONTS = \"skins/fonts/fonts.json\";"));
        assertTrue(source.contains("public static final String UI = \"skins/ui/ui.json\";"));
        assertTrue(source.contains("public static final String UI_ECONOMY = \"skins/ui/economy/economy.json\";"));
        assertTrue(source.contains("public static final String SETTINGS = \"skins/settings.json\";"));
        assertFalse(source.contains("EMPTY"));
        assertFalse(source.contains(" ALL ="));
    }

    @Test
    void sanitizesAndDeduplicatesConstants() throws IOException {
        Path skins = tempDir.resolve("skins");
        write(skins.resolve("weird/weird.json"), """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "bg-province": {"up": "bg-province"},
                    "bg_province": {"up": "bg_province"},
                    "2d_icon": {"up": "2d_icon"}
                  }
                }
                """);

        new SkinNamesGenerator().generate(skins.toFile(), tempDir.resolve("out").toFile(),
                "com.example.ui", "SkinNames", ".atlas");

        String source = Files.readString(tempDir.resolve("out/com/example/ui/SkinNames.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("BG_PROVINCE"));
        assertTrue(source.contains("BG_PROVINCE_2"));
        assertTrue(source.contains("N_2D_ICON"));
    }

    @Test
    void removesGeneratedFileWhenDisabled() throws IOException {
        Path skins = tempDir.resolve("skins");
        write(skins.resolve("ui/ui.json"), """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok_btn": {"up": "ok_btn"}
                  }
                }
                """);
        Path out = tempDir.resolve("out");

        SkinNamesGenerator generator = new SkinNamesGenerator();
        generator.generate(skins.toFile(), out.toFile(), "com.example.ui", "SkinNames", ".atlas");
        assertTrue(Files.exists(out.resolve("com/example/ui/SkinNames.java")));

        generator.clean(out.toFile(), "com.example.ui", "SkinNames");
        assertFalse(Files.exists(out.resolve("com/example/ui/SkinNames.java")));
    }

    @Test
    void usesSkinNamesForIndexedRegions() throws IOException {
        Path skins = tempDir.resolve("skins");
        write(skins.resolve("screens/screens.json"), "{}");
        write(skins.resolve("screens/screens.atlas"), """
                screens.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                load
                index:1
                bounds:0,0,10,10

                load
                index:2
                bounds:10,0,10,10
                """);
        Path out = tempDir.resolve("out");

        new SkinNamesGenerator().generate(skins.toFile(), out.toFile(), "com.example.ui", "SkinNames", ".atlas");

        String source = Files.readString(out.resolve("com/example/ui/SkinNames.java"));
        assertTrue(source.contains("public static final String LOAD_1 = \"load_1\";"));
        assertTrue(source.contains("public static final String LOAD_2 = \"load_2\";"));
        assertTrue(source.contains("public static final class IndexedRegions"));
        assertTrue(source.contains("public static final String LOAD = \"load\";"));
    }

    @Test
    void listsFontPagesUnderFontsOnly() throws IOException {
        Path skins = tempDir.resolve("skins");
        write(skins.resolve("fonts/fonts.json"), """
                {
                  "com.badlogic.gdx.graphics.g2d.BitmapFont": {
                    "arial_17": {"file": "arial_17.fnt"},
                    "chinese_60": {"file": "chinese_60.fnt"}
                  }
                }
                """);
        write(skins.resolve("fonts/fonts.atlas"), """
                fonts.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                arial_17
                bounds:0,0,10,10

                chinese_60
                index:0
                bounds:10,0,10,10

                chinese_60
                index:1
                bounds:20,0,10,10

                icon
                bounds:30,0,10,10
                """);
        Path out = tempDir.resolve("out");

        new SkinNamesGenerator().generate(skins.toFile(), out.toFile(), "com.example.ui", "SkinNames", ".atlas");

        String source = Files.readString(out.resolve("com/example/ui/SkinNames.java"));
        assertTrue(source.contains("public static final String ICON = \"icon\";"));
        assertEquals(1, occurrences(source, "public static final String ARIAL_17 = "));
        assertEquals(1, occurrences(source, "public static final String CHINESE_60 = "));
        assertFalse(source.contains("CHINESE_60_0"));
        assertFalse(source.contains("IndexedRegions"));
    }

    private int occurrences(String source, String text) {
        return source.split(java.util.regex.Pattern.quote(text), -1).length - 1;
    }

    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
