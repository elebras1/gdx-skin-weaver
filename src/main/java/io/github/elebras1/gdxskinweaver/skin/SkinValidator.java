package io.github.elebras1.gdxskinweaver.skin;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkinValidator {
    private static final String BITMAP_FONT_KEY = "com.badlogic.gdx.graphics.g2d.BitmapFont";
    private static final String COLOR_KEY = "com.badlogic.gdx.graphics.Color";

    private static final Set<String> DRAWABLE_FIELDS = Set.of(
            "up", "down", "over", "checked", "checkedOver", "disabled",
            "background", "stageBackground", "knob", "knobBefore", "knobAfter",
            "hScroll", "hScrollKnob", "vScroll", "vScrollKnob", "corner",
            "imageUp", "imageDown", "imageOver", "imageChecked", "imageCheckedOver", "imageDisabled",
            "checkboxOn", "checkboxOff", "selection", "cursor", "scroll", "scrollKnob",
            "line", "textCursor", "textSelection", "handle", "plus", "minus", "plusOver", "minusOver",
            "icon"
    );

    private static final Set<String> FONT_FIELDS = Set.of(
            "font", "titleFont", "messageFont", "downFont", "overFont", "checkedFont", "disabledFont"
    );

    private static final Set<String> COLOR_FIELDS = Set.of(
            "fontColor", "titleFontColor", "messageFontColor", "downFontColor", "overFontColor",
            "checkedFontColor", "disabledFontColor"
    );

    public List<String> validate(File skinFile, File atlasFile) {
        List<String> warnings = new ArrayList<>();
        if (skinFile == null || !skinFile.exists()) {
            warnings.add("Skin file not found: " + skinFile);
            return warnings;
        }

        JsonObject root;
        try (Reader reader = new InputStreamReader(new FileInputStream(skinFile), StandardCharsets.UTF_8)) {
            JsonValue value = Json.parse(reader);
            if (!value.isObject()) {
                warnings.add("Skin file is not a JSON object: " + skinFile.getName());
                return warnings;
            }
            root = value.asObject();
        } catch (IOException | RuntimeException e) {
            warnings.add("Cannot read skin " + skinFile.getName() + ": " + e.getMessage());
            return warnings;
        }

        AtlasInfo atlas = null;
        if (atlasFile != null) {
            if (!atlasFile.exists()) {
                warnings.add("Atlas file not found: " + atlasFile.getName());
            } else {
                try {
                    atlas = readAtlas(atlasFile);
                    for (String duplicate : atlas.duplicates()) {
                        warnings.add("Duplicate atlas region '" + duplicate + "' without an index; the first region wins.");
                    }
                } catch (IOException e) {
                    warnings.add("Cannot read atlas " + atlasFile.getName() + ": " + e.getMessage());
                }
            }
        }

        Set<String> fonts = names(root.get(BITMAP_FONT_KEY));
        Set<String> colors = names(root.get(COLOR_KEY));
        boolean hasDrawableStyles = false;

        for (String className : root.names()) {
            if (!isStyleClass(className)) {
                continue;
            }
            JsonValue classValue = root.get(className);
            if (classValue == null || !classValue.isObject()) {
                continue;
            }

            JsonObject styleClasses = classValue.asObject();
            for (String styleName : styleClasses.names()) {
                JsonValue styleValue = styleClasses.get(styleName);
                if (styleValue == null || !styleValue.isObject()) {
                    continue;
                }

                JsonObject style = styleValue.asObject();
                for (String field : style.names()) {
                    JsonValue fieldValue = style.get(field);
                    if (fieldValue == null || !fieldValue.isString()) {
                        continue;
                    }
                    String reference = fieldValue.asString();

                    if (DRAWABLE_FIELDS.contains(field)) {
                        hasDrawableStyles = true;
                        if (atlas != null && !atlas.regions().contains(reference)) {
                            warnings.add(styleName + " (" + className + "): missing drawable '" + reference + "'");
                        }
                    } else if (FONT_FIELDS.contains(field)) {
                        if (!fonts.isEmpty() && !fonts.contains(reference)) {
                            warnings.add(styleName + " (" + className + "): missing font '" + reference + "'");
                        }
                    } else if (COLOR_FIELDS.contains(field)) {
                        if (!colors.isEmpty() && !reference.startsWith("#") && !colors.contains(reference)) {
                            warnings.add(styleName + " (" + className + "): missing color '" + reference + "'");
                        }
                    }
                }
            }
        }

        if (hasDrawableStyles && atlas == null) {
            warnings.add("No atlas found next to " + skinFile.getName() + "; drawable references were not validated.");
        }

        return warnings;
    }

    private boolean isStyleClass(String className) {
        return className.endsWith("Style");
    }

    private Set<String> names(JsonValue value) {
        if (value == null || !value.isObject()) {
            return Set.of();
        }
        return new LinkedHashSet<>(value.asObject().names());
    }

    private AtlasInfo readAtlas(File atlasFile) throws IOException {
        List<String> lines = Files.readAllLines(atlasFile.toPath(), StandardCharsets.UTF_8);
        Set<String> regions = new LinkedHashSet<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Set<String> indexed = new LinkedHashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.contains(":")) {
                continue;
            }

            String next = i + 1 < lines.size() ? lines.get(i + 1).trim() : "";
            if (isPageHeader(next)) {
                continue;
            }

            regions.add(line);
            counts.merge(line, 1, Integer::sum);
            if (next.startsWith("index:")) {
                indexed.add(line);
            }
        }

        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1 && !indexed.contains(entry.getKey())) {
                duplicates.add(entry.getKey());
            }
        }

        return new AtlasInfo(regions, duplicates);
    }

    private boolean isPageHeader(String nextLine) {
        return nextLine.startsWith("size:")
                || nextLine.startsWith("format:")
                || nextLine.startsWith("filter:")
                || nextLine.startsWith("repeat:");
    }

    private record AtlasInfo(Set<String> regions, List<String> duplicates) {
        private AtlasInfo {
            regions = Set.copyOf(regions);
            duplicates = List.copyOf(duplicates);
        }
    }
}
