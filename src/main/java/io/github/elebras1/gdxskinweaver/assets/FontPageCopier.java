package io.github.elebras1.gdxskinweaver.assets;

import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FontPageCopier {

    public void copy(File sourceDir, File outputRoot, Path assetsRoot, List<File> fonts, List<File> fontPages) {
        File targetDir = AssetFileUtils.getOutputDirectory(sourceDir, outputRoot, assetsRoot);

        for (File font : fonts) {
            File targetFile = new File(targetDir, font.getName());
            copyFile(font, targetFile, "font");
        }

        for (File page : fontPages) {
            copyFile(page, pageTarget(page, outputRoot, assetsRoot), "font page");
        }
    }

    /** Removes font page images copied by a previous run, when they are no longer copied. */
    public void removeCopies(File outputRoot, Path assetsRoot, List<File> fontPages) {
        for (File page : fontPages) {
            File target = pageTarget(page, outputRoot, assetsRoot);
            try {
                Files.deleteIfExists(target.toPath());
            } catch (IOException e) {
                System.err.println("Failed to delete font page " + target.getName() + ": " + e.getMessage());
            }
        }
    }

    private File pageTarget(File page, File outputRoot, Path assetsRoot) {
        Path pagePath = page.toPath().toAbsolutePath().normalize();
        Path assetsPath = assetsRoot.toAbsolutePath().normalize();
        Path relative = pagePath.startsWith(assetsPath) ? assetsPath.relativize(pagePath) : Path.of(page.getName());
        return outputRoot.toPath().resolve(relative).toFile();
    }

    private void copyFile(File source, File target, String label) {
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to copy " + label + " " + source.getName() + ": " + e.getMessage());
        }
    }
}
