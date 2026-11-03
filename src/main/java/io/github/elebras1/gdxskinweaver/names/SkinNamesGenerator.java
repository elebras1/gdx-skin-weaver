package io.github.elebras1.gdxskinweaver.names;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import io.github.elebras1.gdxskinweaver.atlas.AtlasRegionReader;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.stream.Stream;

public class SkinNamesGenerator {

    private static final String BITMAP_FONT_KEY = "com.badlogic.gdx.graphics.g2d.BitmapFont";
    private static final String COLOR_KEY = "com.badlogic.gdx.graphics.Color";

    private final AtlasRegionReader atlasRegionReader = new AtlasRegionReader();

    public void generate(File skinsDir, File outputDir, String packageName, String className, String atlasExtension) {
        generate(skinsDir, outputDir, packageName, className, atlasExtension, false, "");
    }

    public void generate(File skinsDir, File outputDir, String packageName, String className, String atlasExtension,
                         boolean skinCatalog) {
        generate(skinsDir, outputDir, packageName, className, atlasExtension, skinCatalog, "");
    }

    public void generate(File skinsDir, File outputDir, String packageName, String className, String atlasExtension,
                         boolean skinCatalog, String skinCatalogPrefix) {
        validatePackage(packageName);
        validateClassName(className);

        List<SkinModel> models = collect(skinsDir.toPath(), atlasExtension(atlasExtension));
        String source = render(packageName, className, models, skinCatalog, skinCatalogPrefix);
        Path target = outputDir.toPath()
                .resolve(packageName.replace('.', '/'))
                .resolve(className + ".java");

        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin names", e);
        }
    }

    public void clean(File outputDir, String packageName, String className) {
        Path target = outputDir.toPath()
                .resolve(packageName.replace('.', '/'))
                .resolve(className + ".java");
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete skin names", e);
        }
    }

    private List<SkinModel> collect(Path skinsDir, String atlasExtension) {
        if (!Files.isDirectory(skinsDir)) {
            return List.of();
        }

        List<Path> skinFiles;
        try (Stream<Path> stream = Files.walk(skinsDir)) {
            skinFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan skins directory", e);
        }

        List<SkinModel> models = new ArrayList<>();
        Set<String> usedClassNames = new HashSet<>();
        for (Path skinFile : skinFiles) {
            JsonObject root = readSkin(skinFile);
            if (root == null) {
                continue;
            }

            String baseName = uniqueName(skinClassName(skinsDir, skinFile), usedClassNames);
            String skinPath = skinsDir.relativize(skinFile).toString().replace('\\', '/');
            SkinModel model = new SkinModel(baseName, skinPath);
            readResources(root, model);
            readAtlas(skinFile, atlasExtension, model);

            if (!model.isEmpty()) {
                models.add(model);
            }
        }

        models.sort(Comparator.comparing(model -> model.className));
        return models;
    }

    private JsonObject readSkin(Path skinFile) {
        try (Reader reader = Files.newBufferedReader(skinFile, StandardCharsets.UTF_8)) {
            JsonValue value = Json.parse(reader);
            if (!value.isObject()) {
                return null;
            }
            return value.asObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read skin " + skinFile, e);
        }
    }

    private void readResources(JsonObject root, SkinModel model) {
        JsonValue fonts = root.get(BITMAP_FONT_KEY);
        if (fonts != null && fonts.isObject()) {
            model.fonts.addAll(fonts.asObject().names());
        }

        JsonValue colors = root.get(COLOR_KEY);
        if (colors != null && colors.isObject()) {
            model.colors.addAll(colors.asObject().names());
        }

        for (String className : root.names()) {
            if (!className.endsWith("Style")) {
                continue;
            }

            JsonValue classValue = root.get(className);
            if (classValue == null || !classValue.isObject()) {
                continue;
            }

            String typeName = uniqueStyleType(model, styleTypeName(className));
            model.styles.computeIfAbsent(typeName, key -> new TreeSet<>()).addAll(classValue.asObject().names());
        }
    }

    private void readAtlas(Path skinFile, String atlasExtension, SkinModel model) {
        Path atlasFile = skinFile.resolveSibling(fileStem(skinFile) + atlasExtension);
        if (!Files.isRegularFile(atlasFile)) {
            return;
        }

        try {
            AtlasRegionReader.AtlasRegions regions = atlasRegionReader.read(atlasFile.toFile());
            model.drawables.addAll(regions.names());
            model.indexedRegions.addAll(regions.indexedNames());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read atlas " + atlasFile, e);
        }
    }

    private String uniqueName(String baseName, Set<String> usedNames) {
        String name = baseName;
        int suffix = 2;
        while (!usedNames.add(name)) {
            name = baseName + suffix;
            suffix++;
        }
        return name;
    }

    private String uniqueStyleType(SkinModel model, String typeName) {
        String name = typeName;
        int suffix = 2;
        while (model.styles.containsKey(name)) {
            name = typeName + suffix;
            suffix++;
        }
        return name;
    }

    private String render(String packageName, String className, List<SkinModel> models, boolean skinCatalog,
                          String skinCatalogPrefix) {
        StringBuilder source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n");
        source.append("public final class ").append(className).append(" {\n");
        source.append("    private ").append(className).append("() {\n");
        source.append("    }\n");

        if (skinCatalog) {
            appendSkinCatalog(source, models, normalizePrefix(skinCatalogPrefix));
        }

        for (SkinModel model : models) {
            source.append("\n    public static final class ").append(model.className).append(" {\n");
            appendValues(source, "Drawables", model.drawables, 8);
            appendValues(source, "IndexedRegions", model.indexedRegions, 8);
            appendStyles(source, model.styles, 8);
            appendValues(source, "Fonts", model.fonts, 8);
            appendValues(source, "Colors", model.colors, 8);
            source.append("    }\n");
        }

        source.append("}\n");
        return source.toString();
    }

    private void appendSkinCatalog(StringBuilder source, List<SkinModel> models, String prefix) {
        if (models.isEmpty()) {
            return;
        }

        SortedMap<String, String> entries = new TreeMap<>();
        Set<String> usedConstants = new HashSet<>();
        for (SkinModel model : models) {
            String baseName = model.className.endsWith("Skin")
                    ? model.className.substring(0, model.className.length() - "Skin".length())
                    : model.className;
            if (baseName.isEmpty()) {
                baseName = model.className;
            }
            entries.put(uniqueConstant(baseName, usedConstants), prefix + model.skinPath);
        }

        source.append("\n    public static final class Skins {\n");
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            source.append("        public static final String ")
                    .append(entry.getKey())
                    .append(" = \"")
                    .append(escape(entry.getValue()))
                    .append("\";\n");
        }
        source.append("    }\n");
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String value = prefix.trim().replace('\\', '/');
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? "" : value + "/";
    }

    private void appendStyles(StringBuilder source, SortedMap<String, SortedSet<String>> styles, int indent) {
        if (styles.isEmpty()) {
            return;
        }

        String pad = " ".repeat(indent);
        source.append(pad).append("public static final class Styles {\n");
        for (Map.Entry<String, SortedSet<String>> entry : styles.entrySet()) {
            appendValues(source, entry.getKey(), entry.getValue(), indent + 4);
        }
        source.append(pad).append("}\n");
    }

    private void appendValues(StringBuilder source, String className, SortedSet<String> values, int indent) {
        if (values.isEmpty()) {
            return;
        }

        String pad = " ".repeat(indent);
        String inner = " ".repeat(indent + 4);
        Set<String> usedConstants = new HashSet<>();
        source.append(pad).append("public static final class ").append(className).append(" {\n");
        for (String value : values) {
            String constant = uniqueConstant(value, usedConstants);
            source.append(inner)
                    .append("public static final String ")
                    .append(constant)
                    .append(" = \"")
                    .append(escape(value))
                    .append("\";\n");
        }
        source.append(pad).append("}\n");
    }

    private String uniqueConstant(String value, Set<String> usedConstants) {
        String base = toConstantName(value);
        String name = base;
        int suffix = 2;
        while (!usedConstants.add(name)) {
            name = base + "_" + suffix;
            suffix++;
        }
        return name;
    }

    private String skinClassName(Path skinsDir, Path skinFile) {
        Path relative = skinsDir.relativize(skinFile);
        Path parent = relative.getParent();
        List<String> segments = new ArrayList<>();

        if (parent == null || parent.getNameCount() == 0) {
            segments.add(fileStem(skinFile));
        } else {
            for (Path part : parent) {
                segments.add(part.toString());
            }
        }

        String name = toClassName(String.join("_", segments), "Root");
        return name.endsWith("Skin") ? name : name + "Skin";
    }

    private String styleTypeName(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        int dollar = simpleName.indexOf('$');
        if (dollar >= 0) {
            simpleName = simpleName.substring(0, dollar);
        } else if (simpleName.endsWith("Style")) {
            simpleName = simpleName.substring(0, simpleName.length() - 5);
        }
        return toClassName(simpleName, "Style");
    }

    private String toClassName(String value, String fallback) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("[^A-Za-z0-9]+")) {
            if (part.isEmpty()) {
                continue;
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        if (result.isEmpty()) {
            return fallback;
        }
        if (Character.isDigit(result.charAt(0))) {
            result.insert(0, "N");
        }
        return result.toString();
    }

    private String toConstantName(String value) {
        String normalized = value.replaceAll("[^A-Za-z0-9]+", "_");
        normalized = normalized.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        normalized = normalized.toUpperCase(Locale.ROOT).replaceAll("_+", "_");
        if (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            normalized = "EMPTY";
        }
        if (Character.isDigit(normalized.charAt(0))) {
            normalized = "N_" + normalized;
        }
        return normalized;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String fileStem(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(0, dot);
    }

    private String atlasExtension(String atlasExtension) {
        if (atlasExtension == null || atlasExtension.isBlank()) {
            return ".atlas";
        }
        return atlasExtension.startsWith(".") ? atlasExtension : "." + atlasExtension;
    }

    private void validatePackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("Package name must not be empty");
        }
        for (String part : packageName.split("\\.")) {
            if (!part.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
                throw new IllegalArgumentException("Invalid package name: " + packageName);
            }
        }
    }

    private void validateClassName(String className) {
        if (className == null || !className.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("Invalid class name: " + className);
        }
    }

    private static final class SkinModel {
        private final String className;
        private final String skinPath;
        private final SortedSet<String> drawables = new TreeSet<>();
        private final SortedSet<String> indexedRegions = new TreeSet<>();
        private final SortedSet<String> fonts = new TreeSet<>();
        private final SortedSet<String> colors = new TreeSet<>();
        private final SortedMap<String, SortedSet<String>> styles = new TreeMap<>();

        private SkinModel(String className, String skinPath) {
            this.className = className;
            this.skinPath = skinPath;
        }

        private boolean isEmpty() {
            return drawables.isEmpty() && fonts.isEmpty() && colors.isEmpty() && styles.isEmpty();
        }
    }
}
