package io.github.elebras1.gdxskinweaver.service;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class SkinJsonService {

    public void write(File existingSkin, File targetSkin) {
        try {
            Files.copy(existingSkin.toPath(), targetSkin.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin file", e);
        }
    }

    public void write(File targetSkin) {
        try (FileWriter writer = new FileWriter(targetSkin)) {
            writer.write("{}");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin file", e);
        }
    }

    public void write(File targetSkin, List<File> fonts) {
        JsonObject root = new JsonObject();
        if (!fonts.isEmpty()) {
            root.add("com.badlogic.gdx.graphics.g2d.BitmapFont", createBitmapFontObject(fonts));
        }
        saveToFile(root, targetSkin);
    }

    public void write(File targetSkin, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {
        JsonObject root = new JsonObject();
        addButtonStyles(root, buttonSimpleToFull);
        addToggleStyles(root, toggleSimpleToFull);
        saveToFile(root, targetSkin);
    }

    public void write(File targetSkin, List<File> fonts, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {
        JsonObject root = new JsonObject();
        if (!fonts.isEmpty()) {
            root.add("com.badlogic.gdx.graphics.g2d.BitmapFont", createBitmapFontObject(fonts));
        }
        addButtonStyles(root, buttonSimpleToFull);
        addToggleStyles(root, toggleSimpleToFull);
        saveToFile(root, targetSkin);
    }

    public void merge(File targetSkin, File existingSkin, List<File> fonts, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {
        JsonObject root = loadExisting(existingSkin);
        mergeFonts(root, fonts);
        addButtonStyles(root, buttonSimpleToFull);
        addToggleStyles(root, toggleSimpleToFull);
        saveToFile(root, targetSkin);
    }

    private JsonObject loadExisting(File file) {
        if (file == null || !file.exists()) {
            return new JsonObject();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return Json.parse(reader).asObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void mergeFonts(JsonObject root, List<File> fonts) {
        if(fonts.isEmpty()) {
            return;
        }
        JsonObject bitmapFont = getOrCreateBitmapFont(root);
        for (File font : fonts) {
            String baseName = font.getName().replaceFirst("\\.fnt$", "");
            if (bitmapFont.get(baseName) != null) {
                continue;
            }
            bitmapFont.add(baseName, new JsonObject()
                    .add("file", font.getName())
                    .add("scaledSize", -1)
                    .add("markupEnabled", false)
                    .add("flip", false));
        }
    }

    private JsonObject getOrCreateBitmapFont(JsonObject root) {
        JsonValue val = root.get("com.badlogic.gdx.graphics.g2d.BitmapFont");
        if (val == null || val.isNull()) {
            JsonObject obj = new JsonObject();
            root.add("com.badlogic.gdx.graphics.g2d.BitmapFont", obj);
            return obj;
        }
        return val.asObject();
    }

    private JsonObject createBitmapFontObject(List<File> fonts) {
        JsonObject obj = new JsonObject();
        for (File font : fonts) {
            String baseName = font.getName().replaceFirst("\\.fnt$", "");
            obj.add(baseName, new JsonObject()
                    .add("file", font.getName())
                    .add("scaledSize", -1)
                    .add("markupEnabled", false)
                    .add("flip", false));
        }
        return obj;
    }

    private void addButtonStyles(JsonObject root, Map<String, String> buttonSimpleToFull) {
        if (buttonSimpleToFull == null || buttonSimpleToFull.isEmpty()) {
            return;
        }
        String key = "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle";
        JsonObject styles;
        if (root.get(key) == null) {
            styles = new JsonObject();
            root.add(key, styles);
        } else {
            styles = root.get(key).asObject();
        }
        for (Map.Entry<String, String> entry : buttonSimpleToFull.entrySet()) {
            String simpleName = entry.getKey();
            String fullRegion = entry.getValue();
            if (styles.get(simpleName) != null) continue;
            String region = stripPath(fullRegion);
            JsonObject style = new JsonObject()
                    .add("up", region)
                    .add("down", region + "_down")
                    .add("over", region + "_over");
            styles.add(simpleName, style);
        }
    }

    private void addToggleStyles(JsonObject root, Map<String, String> toggleSimpleToFull) {
        if (toggleSimpleToFull == null || toggleSimpleToFull.isEmpty()) {
            return;
        }
        String key = "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle";
        JsonObject styles;
        if (root.get(key) == null) {
            styles = new JsonObject();
            root.add(key, styles);
        } else {
            styles = root.get(key).asObject();
        }
        for (Map.Entry<String, String> entry : toggleSimpleToFull.entrySet()) {
            String simpleName = entry.getKey();
            String fullRegion = entry.getValue();
            if (styles.get(simpleName) != null) continue;
            String region = stripPath(fullRegion);
            JsonObject style = new JsonObject()
                    .add("up", region + "_off")
                    .add("checked", region + "_on");
            styles.add(simpleName, style);
        }
    }

    private String stripPath(String region) {
        int slash = region.lastIndexOf('/');
        int backslash = region.lastIndexOf('\\');
        int idx = Math.max(slash, backslash);
        return idx == -1 ? region : region.substring(idx + 1);
    }

    private void saveToFile(JsonObject object, File target) {
        try (FileWriter writer = new FileWriter(target)) {
            object.writeTo(writer, WriterConfig.PRETTY_PRINT);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin file", e);
        }
    }
}
