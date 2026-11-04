package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.skin.SkinGenerationConfig;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record WeaveContext(
        Path assetsRoot,
        File outputDir,
        Set<String> excludedDirs,
        File temporaryDir,
        Map<String, String> packerOptions,
        boolean packFontPages,
        boolean copyPackedFontPages,
        SkinGenerationConfig skinGeneration
) {

    public WeaveContext {
        packerOptions = packerOptions == null ? Map.of() : Map.copyOf(packerOptions);
        skinGeneration = skinGeneration == null ? SkinGenerationConfig.defaults() : skinGeneration;
    }

    public WeaveContext(Path assetsRoot, File outputDir, Set<String> excludedDirs, File temporaryDir,
                        Map<String, String> packerOptions, boolean packFontPages, SkinGenerationConfig skinGeneration) {
        this(assetsRoot, outputDir, excludedDirs, temporaryDir, packerOptions, packFontPages, true, skinGeneration);
    }

    /** Font page images are needed next to the {@code .fnt} files unless they are packed and their copy is disabled. */
    public boolean copyFontPages() {
        return !packFontPages || copyPackedFontPages;
    }
}
