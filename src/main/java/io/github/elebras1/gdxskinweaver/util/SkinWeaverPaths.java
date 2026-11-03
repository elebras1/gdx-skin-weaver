package io.github.elebras1.gdxskinweaver.util;

import org.gradle.api.InvalidUserDataException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SkinWeaverPaths {

    private SkinWeaverPaths() {
    }

    /**
     * Normalizes excluded directories relative to the assets directory: forward slashes, no leading {@code ./}
     * and no trailing slash. The assets root itself is written as an empty string ({@code ""} or {@code "."}).
     */
    public static Set<String> normalizeExcludedDirs(Collection<String> excludedDirs) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String excludedDir : excludedDirs) {
            normalized.add(normalizeExcludedDir(excludedDir));
        }
        return normalized;
    }

    public static String normalizeExcludedDir(String excludedDir) {
        String value = excludedDir == null ? "" : excludedDir.trim().replace('\\', '/');
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.equals(".") ? "" : value;
    }

    /**
     * Refuses an output directory that is the assets directory or one of its parents: generating or cleaning
     * skins there would overwrite or delete the source assets.
     */
    public static void requireSafeOutputDir(File assetsDir, File outputDir) {
        Path assets = realPath(assetsDir.toPath());
        Path output = realPath(outputDir.toPath());
        if (assets.startsWith(output)) {
            throw new InvalidUserDataException("skinWeaver.outputDir (" + output + ") must not be the assets directory ("
                    + assets + ") or one of its parents.");
        }
    }

    /**
     * Resolves symbolic links in the existing part of the path, then appends the missing segments. A plain
     * {@code normalize()} is not enough: an output directory can reach the assets through a symlink.
     */
    private static Path realPath(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        try {
            return absolute.toRealPath();
        } catch (IOException e) {
            Path parent = absolute.getParent();
            if (parent == null) {
                return absolute;
            }
            Path fileName = absolute.getFileName();
            return realPath(parent).resolve(fileName).normalize();
        }
    }
}
