package io.github.elebras1.gdxskinweaver.assets;

import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

public class FontPageCopier {

    public void copy(File sourceDir, File outputRoot, Path assetsRoot, List<File> fonts, Set<File> fontPageImages) {
        File targetDir = AssetFileUtils.getOutputDirectory(sourceDir, outputRoot, assetsRoot);

        for (File font : fonts) {
            File targetFile = new File(targetDir, font.getName());
            copyFile(font, targetFile, "font");
        }

        for (File page : fontPageImages) {
            File targetFile = new File(targetDir, page.getName());
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

