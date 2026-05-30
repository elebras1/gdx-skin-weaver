package io.github.elebras1.gdxskinweaver.dao;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkinDAO {

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
            JsonObject bitmapFont = new JsonObject();
            for (File font : fonts) {
                String baseName = font.getName().replaceFirst("\\.fnt$", "");
                JsonObject fontDef = new JsonObject()
                        .add("file", font.getName())
                        .add("scaledSize", -1)
                        .add("markupEnabled", false)
                        .add("flip", false);
                bitmapFont.add(baseName, fontDef);
            }
            root.add("com.badlogic.gdx.graphics.g2d.BitmapFont", bitmapFont);
        }
        this.saveToFile(root, targetSkin);
    }

    public void merge(File targetSkin, File existingSkin, List<File> fonts) {
        JsonObject root = new JsonObject();

        if (existingSkin != null && existingSkin.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(existingSkin), StandardCharsets.UTF_8)) {
                root = Json.parse(reader).asObject();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to merge skin file", e);
            }
        }

        JsonValue bitmapFontValue = root.get("com.badlogic.gdx.graphics.g2d.BitmapFont");
        JsonObject bitmapFont;
        if (bitmapFontValue == null || bitmapFontValue.isNull()) {
            bitmapFont = new JsonObject();
            root.add("com.badlogic.gdx.graphics.g2d.BitmapFont", bitmapFont);
        } else {
            bitmapFont = bitmapFontValue.asObject();
        }

        for (File font : fonts) {
            String baseName = font.getName().replaceFirst("\\.fnt$", "");
            JsonValue existingFont = bitmapFont.get(baseName);
            if (existingFont != null && !existingFont.isNull()) {
                continue;
            }
            JsonObject fontDef = new JsonObject()
                    .add("file", font.getName())
                    .add("scaledSize", -1)
                    .add("markupEnabled", false)
                    .add("flip", false);
            bitmapFont.add(baseName, fontDef);
        }

        this.saveToFile(root, targetSkin);
    }

    public void write(File targetSkin, Map<String, String> buttonSimpleToFull) {
        JsonObject root = new JsonObject();
        this.addButtonStyles(root, buttonSimpleToFull);
        this.saveToFile(root, targetSkin);
    }

    public void write(File targetSkin, List<File> fonts, Map<String, String> buttonSimpleToFull) {
        JsonObject root = new JsonObject();
        if (!fonts.isEmpty()) {
            root.add("com.badlogic.gdx.graphics.g2d.BitmapFont", this.createBitmapFontObject(fonts));
        }
        this.addButtonStyles(root, buttonSimpleToFull);
        this.saveToFile(root, targetSkin);
    }

    public void merge(File targetSkin, File existingSkin, List<File> fonts, Map<String, String> buttonSimpleToFull) {
        JsonObject root = this.loadExisting(existingSkin);
        this.mergeFonts(root, fonts);
        this.addButtonStyles(root, buttonSimpleToFull);
        this.saveToFile(root, targetSkin);
    }

    private JsonObject loadExisting(File file) {
        if (file == null || !file.exists()) return new JsonObject();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return Json.parse(reader).asObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void mergeFonts(JsonObject root, List<File> fonts) {
        JsonObject bitmapFont = getOrCreateBitmapFont(root);
        for (File font : fonts) {
            String baseName = font.getName().replaceFirst("\\.fnt$", "");
            if (bitmapFont.get(baseName) != null) continue;
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
        if (buttonSimpleToFull == null || buttonSimpleToFull.isEmpty()) return;
        String key = "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle";
        JsonObject styles = root.get(key) == null ? new JsonObject() : root.get(key).asObject();
        for (Map.Entry<String, String> entry : buttonSimpleToFull.entrySet()) {
            String simpleName = entry.getKey();
            String fullRegion = entry.getValue();
            if (styles.get(simpleName) != null) continue;
            JsonObject style = new JsonObject()
                    .add("up", fullRegion)
                    .add("down", fullRegion + "_down")
                    .add("over", fullRegion + "_over");
            styles.add(simpleName, style);
        }
        root.add(key, styles);
    }

    private void saveToFile(JsonObject object, File target) {
        try (FileWriter writer = new FileWriter(target)) {
            object.writeTo(writer, WriterConfig.PRETTY_PRINT);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin file", e);
        }
    }
}