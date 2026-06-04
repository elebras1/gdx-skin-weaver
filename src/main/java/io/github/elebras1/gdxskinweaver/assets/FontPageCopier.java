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
            Path pagePath = page.toPath().toAbsolutePath().normalize();
            Path assetsPath = assetsRoot.toAbsolutePath().normalize();
            Path relative = pagePath.startsWith(assetsPath) ? assetsPath.relativize(pagePath) : Path.of(page.getName());
            File targetFile = outputRoot.toPath().resolve(relative).toFile();
            copyFile(page, targetFile, "font page");
        }
    }

    private void copyFile(File source, File target, String label) {
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to copy " + label + " " + source.getName() + ": " + e.getMessage());
        }
    }
}
