package io.github.elebras1.gdxskinweaver.service;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import io.github.elebras1.gdxskinweaver.skin.SkinGenerationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinJsonServiceTest {
    private static final String LABEL_STYLE_KEY = "com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle";

    @TempDir
    Path tempDir;

    @Test
    void generatesBaseAndColorLabelStyles() throws IOException {
        Path existing = writeExistingSkin("""
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1},
                    "blue": {"r": 0, "g": 0, "b": 1, "a": 1}
                  }
                }
                """);
        List<File> fonts = List.of(font("arial_14.fnt"), font("impact.fnt"));
        Path target = tempDir.resolve("fonts.json");

        new SkinJsonService().merge(target.toFile(), existing.toFile(), fonts, Map.of(), Map.of(),
                new SkinGenerationConfig(true, List.of(), null, true, true));

        JsonObject labels = labels(target);
        assertNotNull(labels.get("arial_14"));
        assertNull(labels.get("arial_14").asObject().get("fontColor"));
        assertEquals("red", labels.get("arial_14_red").asObject().get("fontColor").asString());
        assertEquals("blue", labels.get("impact_blue").asObject().get("fontColor").asString());
    }

    @Test
    void honorsColorFilterAndDefaultColor() throws IOException {
        Path existing = writeExistingSkin("""
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1},
                    "blue": {"r": 0, "g": 0, "b": 1, "a": 1}
                  }
                }
                """);
        Path target = tempDir.resolve("fonts.json");

        new SkinJsonService().merge(target.toFile(), existing.toFile(), List.of(font("arial_14.fnt")),
                Map.of(), Map.of(), new SkinGenerationConfig(true, List.of("blue"), "red", true, true));

        JsonObject labels = labels(target);
        assertEquals("red", labels.get("arial_14").asObject().get("fontColor").asString());
        assertNotNull(labels.get("arial_14_blue"));
        assertNull(labels.get("arial_14_red"));
    }

    @Test
    void keepsExistingLabelStyles() throws IOException {
        Path existing = writeExistingSkin("""
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1}
                  },
                  "com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle": {
                    "arial_14_red": {"font": "custom_font", "fontColor": "red"}
                  }
                }
                """);
        Path target = tempDir.resolve("fonts.json");

        new SkinJsonService().merge(target.toFile(), existing.toFile(), List.of(font("arial_14.fnt")),
                Map.of(), Map.of(), new SkinGenerationConfig(true, List.of(), null, true, true));

        JsonObject labels = labels(target);
        assertEquals("custom_font", labels.get("arial_14_red").asObject().get("font").asString());
        assertNotNull(labels.get("arial_14"));
        assertNotNull(labels.get("arial_14_red"));
    }

    @Test
    void canDisableLabelStyles() throws IOException {
        Path existing = writeExistingSkin("""
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1}
                  }
                }
                """);
        Path target = tempDir.resolve("fonts.json");

        new SkinJsonService().merge(target.toFile(), existing.toFile(), List.of(font("arial_14.fnt")),
                Map.of(), Map.of(), new SkinGenerationConfig(false, List.of(), null, true, true));

        assertNull(read(target).get(LABEL_STYLE_KEY));
    }

    @Test
    void ignoresUnknownColors() throws IOException {
        Path existing = writeExistingSkin("""
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1}
                  }
                }
                """);
        Path target = tempDir.resolve("fonts.json");

        new SkinJsonService().merge(target.toFile(), existing.toFile(), List.of(font("arial_14.fnt")),
                Map.of(), Map.of(), new SkinGenerationConfig(true, List.of("purple"), null, true, true));

        JsonObject labels = labels(target);
        assertNotNull(labels.get("arial_14"));
        assertNull(labels.get("arial_14_purple"));
        assertFalse(labels.names().contains("arial_14_red"));
    }

    @Test
    void keepsDirectoryInRegionNamesWhenFlattenPathsIsDisabled() throws IOException {
        Path target = tempDir.resolve("ui.json");

        new SkinJsonService().write(target.toFile(), Map.of("nested_btn", "ui/nested_btn"), Map.of(),
                new SkinGenerationConfig(true, List.of(), null, true, false));

        JsonObject style = read(target)
                .get("com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle").asObject()
                .get("nested_btn").asObject();
        assertEquals("ui/nested_btn", style.get("up").asString());
        assertEquals("ui/nested_btn_down", style.get("down").asString());
    }

    private Path writeExistingSkin(String content) throws IOException {
        Path existing = tempDir.resolve("fonts_skin.json");
        Files.writeString(existing, content, StandardCharsets.UTF_8);
        return existing;
    }

    private File font(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, "", StandardCharsets.UTF_8);
        return file.toFile();
    }

    private JsonObject labels(Path target) throws IOException {
        JsonObject root = read(target);
        JsonValue labels = root.get(LABEL_STYLE_KEY);
        assertNotNull(labels);
        return labels.asObject();
    }

    private JsonObject read(Path target) throws IOException {
        try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            return Json.parse(reader).asObject();
        }
    }
}
