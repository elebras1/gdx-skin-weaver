package io.github.elebras1.gdxskinweaver.skin;

import java.util.List;

public record SkinGenerationConfig(
        boolean generateLabelStyles,
        List<String> labelStyleColors,
        String defaultLabelColor,
        boolean validationEnabled,
        boolean flattenPaths
) {

    public SkinGenerationConfig {
        labelStyleColors = labelStyleColors == null ? List.of() : List.copyOf(labelStyleColors);
        defaultLabelColor = defaultLabelColor == null || defaultLabelColor.isBlank() ? null : defaultLabelColor;
    }

    public static SkinGenerationConfig defaults() {
        return new SkinGenerationConfig(true, List.of(), null, true, true);
    }
}
