package io.github.elebras1.gdxskinweaver.util;

import java.io.File;
import java.nio.file.Path;

public class AssetFileUtils {

    public static boolean isImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".ktx2");
    }

    public static boolean isFont(File file) {
        return file.getName().toLowerCase().endsWith(".fnt");
    }

    public static boolean isSkin(File file) {
        return file.getName().toLowerCase().endsWith("_skin.json");
    }

    public static File getOutputDirectory(File sourceDir, File outputRoot, Path assetsRoot) {
        Path sourcePath = sourceDir.toPath();
        String relative = assetsRoot.relativize(sourcePath).toString();
        File targetDir = outputRoot.toPath().resolve(relative).toFile();
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            System.err.println("Cannot create output directory: " + targetDir);
            return null;
        }

        return targetDir;
    }
}

