package io.github.elebras1.gdxskinweaver.service;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import io.github.elebras1.gdxskinweaver.skin.SkinGenerationConfig;
import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkinJsonService {
    private static final String BITMAP_FONT_KEY = "com.badlogic.gdx.graphics.g2d.BitmapFont";
    private static final String COLOR_KEY = "com.badlogic.gdx.graphics.Color";
    private static final String BUTTON_STYLE_KEY = "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle";
    private static final String LABEL_STYLE_KEY = "com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle";

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
        write(targetSkin, fonts, SkinGenerationConfig.defaults());
    }

    public void write(File targetSkin, List<File> fonts, SkinGenerationConfig config) {
        JsonObject root = new JsonObject();
        if (!fonts.isEmpty()) {
            root.add(BITMAP_FONT_KEY, createBitmapFontObject(fonts));
        }
        addLabelStyles(root, fonts, config);
        saveToFile(root, targetSkin);
    }

    public void write(File targetSkin, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {
        write(targetSkin, buttonSimpleToFull, toggleSimpleToFull, SkinGenerationConfig.defaults());
    }

    public void write(File targetSkin, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull,
                      SkinGenerationConfig config) {
        SkinGenerationConfig effectiveConfig = config == null ? SkinGenerationConfig.defaults() : config;
        JsonObject root = new JsonObject();
        addButtonStyles(root, buttonSimpleToFull, effectiveConfig);
        addToggleStyles(root, toggleSimpleToFull, effectiveConfig);
        saveToFile(root, targetSkin);
    }

    public void write(File targetSkin, List<File> fonts, Map<String, String> buttonSimpleToFull,
                      Map<String, String> toggleSimpleToFull) {
        write(targetSkin, fonts, buttonSimpleToFull, toggleSimpleToFull, SkinGenerationConfig.defaults());
    }

    public void write(File targetSkin, List<File> fonts, Map<String, String> buttonSimpleToFull,
                      Map<String, String> toggleSimpleToFull, SkinGenerationConfig config) {
        SkinGenerationConfig effectiveConfig = config == null ? SkinGenerationConfig.defaults() : config;
        JsonObject root = new JsonObject();
        if (!fonts.isEmpty()) {
            root.add(BITMAP_FONT_KEY, createBitmapFontObject(fonts));
        }
        addButtonStyles(root, buttonSimpleToFull, effectiveConfig);
        addToggleStyles(root, toggleSimpleToFull, effectiveConfig);
        addLabelStyles(root, fonts, effectiveConfig);
        saveToFile(root, targetSkin);
    }

    public void merge(File targetSkin, File existingSkin, List<File> fonts, Map<String, String> buttonSimpleToFull,
                      Map<String, String> toggleSimpleToFull) {
        merge(targetSkin, existingSkin, fonts, buttonSimpleToFull, toggleSimpleToFull, SkinGenerationConfig.defaults());
    }

    public void merge(File targetSkin, File existingSkin, List<File> fonts, Map<String, String> buttonSimpleToFull,
                      Map<String, String> toggleSimpleToFull, SkinGenerationConfig config) {
        SkinGenerationConfig effectiveConfig = config == null ? SkinGenerationConfig.defaults() : config;
        JsonObject root = loadExisting(existingSkin);
        mergeFonts(root, fonts);
        addButtonStyles(root, buttonSimpleToFull, effectiveConfig);
        addToggleStyles(root, toggleSimpleToFull, effectiveConfig);
        addLabelStyles(root, fonts, effectiveConfig);
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
        if (fonts.isEmpty()) {
            return;
        }
        JsonObject bitmapFont = getOrCreateStyles(root, BITMAP_FONT_KEY);
        for (File font : fonts) {
            String baseName = AssetFileUtils.getBaseName(font);
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

    private JsonObject createBitmapFontObject(List<File> fonts) {
        JsonObject obj = new JsonObject();
        for (File font : fonts) {
            String baseName = AssetFileUtils.getBaseName(font);
            obj.add(baseName, new JsonObject()
                    .add("file", font.getName())
                    .add("scaledSize", -1)
                    .add("markupEnabled", false)
                    .add("flip", false));
        }
        return obj;
    }

    private void addButtonStyles(JsonObject root, Map<String, String> buttonSimpleToFull, SkinGenerationConfig config) {
        if (buttonSimpleToFull == null || buttonSimpleToFull.isEmpty()) {
            return;
        }
        JsonObject styles = getOrCreateStyles(root, BUTTON_STYLE_KEY);
        for (Map.Entry<String, String> entry : buttonSimpleToFull.entrySet()) {
            String simpleName = entry.getKey();
            String fullRegion = entry.getValue();
            if (styles.get(simpleName) != null) {
                continue;
            }
            String region = regionName(fullRegion, config);
            JsonObject style = new JsonObject()
                    .add("up", region)
                    .add("down", region + "_down")
                    .add("over", region + "_over");
            styles.add(simpleName, style);
        }
    }

    private void addToggleStyles(JsonObject root, Map<String, String> toggleSimpleToFull, SkinGenerationConfig config) {
        if (toggleSimpleToFull == null || toggleSimpleToFull.isEmpty()) {
            return;
        }
        JsonObject styles = getOrCreateStyles(root, BUTTON_STYLE_KEY);
        for (Map.Entry<String, String> entry : toggleSimpleToFull.entrySet()) {
            String simpleName = entry.getKey();
            String fullRegion = entry.getValue();
            if (styles.get(simpleName) != null) {
                continue;
            }
            String region = regionName(fullRegion, config);
            JsonObject style = new JsonObject()
                    .add("up", region + "_off")
                    .add("checked", region + "_on");
            styles.add(simpleName, style);
        }
    }

    private void addLabelStyles(JsonObject root, List<File> fonts, SkinGenerationConfig config) {
        SkinGenerationConfig effectiveConfig = config == null ? SkinGenerationConfig.defaults() : config;
        if (!effectiveConfig.generateLabelStyles() || fonts.isEmpty()) {
            return;
        }

        Set<String> availableColors = availableLabelColors(root);
        Set<String> colors = resolveLabelColors(availableColors, effectiveConfig);
        String defaultColor = resolveDefaultLabelColor(availableColors, effectiveConfig.defaultLabelColor());
        JsonObject styles = getOrCreateStyles(root, LABEL_STYLE_KEY);
        Set<String> fontNames = new LinkedHashSet<>();
        for (File font : fonts) {
            fontNames.add(AssetFileUtils.getBaseName(font));
        }

        for (File font : fonts) {
            String fontName = AssetFileUtils.getBaseName(font);
            if (styles.get(fontName) == null) {
                JsonObject style = new JsonObject().add("font", fontName);
                if (defaultColor != null) {
                    style.add("fontColor", defaultColor);
                }
                styles.add(fontName, style);
            }

            for (String color : colors) {
                String styleName = fontName + "_" + color;
                if (fontNames.contains(styleName)) {
                    warn("Label style '" + styleName + "' (font '" + fontName + "' in " + color
                            + ") is not generated: the font '" + styleName + "' already uses this name.");
                    continue;
                }
                if (styles.get(styleName) != null) {
                    continue;
                }
                styles.add(styleName, new JsonObject()
                        .add("font", fontName)
                        .add("fontColor", color));
            }
        }
    }

    private Set<String> availableLabelColors(JsonObject root) {
        Set<String> available = new LinkedHashSet<>();
        JsonValue colorValue = root.get(COLOR_KEY);
        if (colorValue != null && colorValue.isObject()) {
            for (String colorName : colorValue.asObject().names()) {
                available.add(colorName);
            }
        }
        return available;
    }

    private Set<String> resolveLabelColors(Set<String> available, SkinGenerationConfig config) {
        if (config.labelStyleColors().isEmpty()) {
            return available;
        }

        Set<String> selected = new LinkedHashSet<>();
        for (String color : config.labelStyleColors()) {
            if (available.contains(color)) {
                selected.add(color);
            } else {
                warn("Label color '" + color + "' is not defined in " + COLOR_KEY + "; ignoring it.");
            }
        }
        return selected;
    }

    private String resolveDefaultLabelColor(Set<String> colors, String defaultLabelColor) {
        if (defaultLabelColor == null || defaultLabelColor.isBlank()) {
            return null;
        }
        if (colors.contains(defaultLabelColor)) {
            return defaultLabelColor;
        }
        warn("Default label color '" + defaultLabelColor + "' is not defined in " + COLOR_KEY + "; ignoring it.");
        return null;
    }

    private JsonObject getOrCreateStyles(JsonObject root, String key) {
        JsonValue value = root.get(key);
        if (value == null || value.isNull()) {
            JsonObject styles = new JsonObject();
            root.add(key, styles);
            return styles;
        }
        return value.asObject();
    }

    private String regionName(String fullRegion, SkinGenerationConfig config) {
        return config.flattenPaths() ? stripPath(fullRegion) : fullRegion;
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

    private void warn(String message) {
        System.err.println("[gdx-skin-weaver] " + message);
    }
}
