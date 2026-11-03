package io.github.elebras1.gdxskinweaver.service;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TexturePackerService {

    public void pack(File sourceDir, File targetDir, String atlasName, List<File> images) {
        pack(sourceDir, targetDir, atlasName, images, Map.of());
    }

    public void pack(File sourceDir, File targetDir, String atlasName, List<File> images, Map<String, String> options) {
        pack(sourceDir, targetDir, atlasName, images, options, Set.of());
    }

    /**
     * @param preservedFiles names of files in {@code targetDir} that must survive the cleanup even when they look
     *                       like atlas pages, such as font pages copied next to the atlas.
     */
    public void pack(File sourceDir, File targetDir, String atlasName, List<File> images, Map<String, String> options,
                     Set<String> preservedFiles) {
        TexturePacker.Settings settings = createSettings(options);
        cleanPreviousPack(targetDir, atlasName, settings, preservedFiles);
        TexturePacker packer = new TexturePacker(sourceDir, settings);
        for (File img : images) {
            packer.addImage(img);
        }
        packer.pack(targetDir, atlasName);
    }

    public void clean(File targetDir, String atlasName, Map<String, String> options) {
        clean(targetDir, atlasName, options, Set.of());
    }

    public void clean(File targetDir, String atlasName, Map<String, String> options, Set<String> preservedFiles) {
        cleanPreviousPack(targetDir, atlasName, createSettings(options), preservedFiles);
    }

    private void cleanPreviousPack(File targetDir, String atlasName, TexturePacker.Settings settings,
                                   Set<String> preservedFiles) {
        String atlasExtension = settings.atlasExtension == null || settings.atlasExtension.isBlank()
                ? ".atlas" : settings.atlasExtension;
        deleteQuietly(new File(targetDir, atlasName + atlasExtension));

        String pageSuffix = pageSuffixPattern(atlasName);
        for (String imageExtension : List.of(".png", ".jpg", ".jpeg", ".cbt")) {
            String pattern = Pattern.quote(atlasName) + pageSuffix + Pattern.quote(imageExtension);
            File[] pages = targetDir.listFiles((dir, name) -> name.matches(pattern) && !preservedFiles.contains(name));
            if (pages != null) {
                for (File page : pages) {
                    deleteQuietly(page);
                }
            }
        }
    }

    /**
     * Mirrors TexturePacker page naming: pages after the first are named {@code atlas2}, {@code atlas3}, ...,
     * with a dash only when the atlas name ends with a digit (or a digit followed by 'x'): {@code atlas1-2}.
     */
    private static String pageSuffixPattern(String atlasName) {
        int length = atlasName.length();
        char last = length == 0 ? ' ' : atlasName.charAt(length - 1);
        boolean dashed = Character.isDigit(last)
                || (length > 3 && last == 'x' && Character.isDigit(atlasName.charAt(length - 2)));
        return dashed ? "(-\\d+)?" : "(\\d+)?";
    }

    private void deleteQuietly(File file) {
        if (file.exists() && !file.delete()) {
            warn("Cannot delete previous pack file: " + file);
        }
    }

    private TexturePacker.Settings createSettings(Map<String, String> options) {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.pot = boolValue(options, "pot", true);
        settings.multipleOfFour = boolValue(options, "multipleOfFour", false);
        settings.paddingX = intValue(options, "paddingX", 2);
        settings.paddingY = intValue(options, "paddingY", 2);
        settings.edgePadding = boolValue(options, "edgePadding", true);
        settings.duplicatePadding = boolValue(options, "duplicatePadding", false);
        settings.rotation = boolValue(options, "rotation", false);
        settings.minWidth = intValue(options, "minWidth", 16);
        settings.minHeight = intValue(options, "minHeight", 16);
        settings.maxWidth = intValue(options, "maxWidth", 2048);
        settings.maxHeight = intValue(options, "maxHeight", 2048);
        settings.square = boolValue(options, "square", false);
        settings.stripWhitespaceX = boolValue(options, "stripWhitespaceX", false);
        settings.stripWhitespaceY = boolValue(options, "stripWhitespaceY", false);
        settings.alphaThreshold = intValue(options, "alphaThreshold", 1);
        settings.filterMin = filterValue(options, "filterMin", Texture.TextureFilter.Nearest);
        settings.filterMag = filterValue(options, "filterMag", Texture.TextureFilter.Nearest);
        settings.wrapX = wrapValue(options, "wrapX", Texture.TextureWrap.ClampToEdge);
        settings.wrapY = wrapValue(options, "wrapY", Texture.TextureWrap.ClampToEdge);
        settings.outputFormat = stringValue(options, "outputFormat", "png");
        settings.jpegQuality = floatValue(options, "jpegQuality", 0.9f);
        settings.ignoreBlankImages = boolValue(options, "ignoreBlankImages", true);
        settings.fast = boolValue(options, "fast", false);
        settings.debug = boolValue(options, "debug", false);
        settings.silent = boolValue(options, "silent", false);
        settings.combineSubdirectories = boolValue(options, "combineSubdirectories", false);
        settings.flattenPaths = boolValue(options, "flattenPaths", true);
        settings.premultiplyAlpha = boolValue(options, "premultiplyAlpha", false);
        settings.useIndexes = boolValue(options, "useIndexes", true);
        settings.bleed = boolValue(options, "bleed", false);
        settings.bleedIterations = intValue(options, "bleedIterations", 2);
        settings.prettyPrint = boolValue(options, "prettyPrint", false);
        settings.legacyOutput = boolValue(options, "legacyOutput", false);
        settings.atlasExtension = atlasExtension(stringValue(options, "atlasExtension", ".atlas"));
        return settings;
    }

    private int intValue(Map<String, String> options, String key, int defaultValue) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            warn("Invalid integer for packer option '" + key + "': " + value + ". Using " + defaultValue + ".");
            return defaultValue;
        }
    }

    private float floatValue(Map<String, String> options, String key, float defaultValue) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            warn("Invalid number for packer option '" + key + "': " + value + ". Using " + defaultValue + ".");
            return defaultValue;
        }
    }

    private boolean boolValue(Map<String, String> options, String key, boolean defaultValue) {
        String value = options.get(key);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private String stringValue(Map<String, String> options, String key, String defaultValue) {
        String value = options.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private Texture.TextureFilter filterValue(Map<String, String> options, String key, Texture.TextureFilter defaultValue) {
        String value = stringValue(options, key, defaultValue.name());
        try {
            return Texture.TextureFilter.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            try {
                return Texture.TextureFilter.valueOf(capitalize(value));
            } catch (IllegalArgumentException e) {
                warn("Invalid packer option '" + key + "': " + value + ". Using " + defaultValue.name() + ".");
                return defaultValue;
            }
        }
    }

    private Texture.TextureWrap wrapValue(Map<String, String> options, String key, Texture.TextureWrap defaultValue) {
        String value = stringValue(options, key, defaultValue.name());
        try {
            return Texture.TextureWrap.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            try {
                return Texture.TextureWrap.valueOf(capitalize(value));
            } catch (IllegalArgumentException e) {
                warn("Invalid packer option '" + key + "': " + value + ". Using " + defaultValue.name() + ".");
                return defaultValue;
            }
        }
    }

    private String atlasExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return ".atlas";
        }
        return extension.startsWith(".") ? extension : "." + extension;
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void warn(String message) {
        System.err.println("[gdx-skin-weaver] " + message);
    }
}
