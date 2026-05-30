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

    private void saveToFile(JsonObject object, File target) {
        try (FileWriter writer = new FileWriter(target)) {
            object.writeTo(writer, WriterConfig.PRETTY_PRINT);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin file", e);
        }
    }
}