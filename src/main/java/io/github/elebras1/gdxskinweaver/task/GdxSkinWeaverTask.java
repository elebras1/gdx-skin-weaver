package io.github.elebras1.gdxskinweaver.task;

import io.github.elebras1.gdxskinweaver.util.FileUtils;
import io.github.elebras1.gdxskinweaver.dao.SkinDAO;
import io.github.elebras1.gdxskinweaver.dao.TexturePackerDAO;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public abstract class GdxSkinWeaverTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getAssetsDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract SetProperty<String> getExcludedDirs();

    private final TexturePackerDAO texturePackerDAO;

    private final SkinDAO skinDAO;

    public GdxSkinWeaverTask() {
        this.texturePackerDAO = new TexturePackerDAO();
        this.skinDAO = new SkinDAO();
    }

    @TaskAction
    public void weave() {
        File assetsDir = this.getAssetsDir().get().getAsFile();
        File outputDir = this.getOutputDir().get().getAsFile();

        Path assetsRoot = assetsDir.toPath();
        String relativeOutput = assetsRoot.relativize(outputDir.toPath()).toString();

        Set<String> excludedDirsWithOutput = new HashSet<>(this.getExcludedDirs().get());
        excludedDirsWithOutput.add(relativeOutput);

        this.processWeave(assetsDir, outputDir, excludedDirsWithOutput, assetsRoot);
    }

    private void processWeave(File dir, File outputDir, Set<String> excludedDirs, Path assetsRoot) {
        File[] files = dir.listFiles();
        if (files == null) return;

        List<File> images = new ArrayList<>();
        List<File> fonts = new ArrayList<>();
        File existingSkin = null;

        for (File file : files) {
            String relativePath = assetsRoot.relativize(file.toPath()).toString();

            if (file.isDirectory()) {
                if (!excludedDirs.contains(relativePath)) {
                    this.processWeave(file, outputDir, excludedDirs, assetsRoot);
                }
                continue;
            }

            if (FileUtils.isImage(file)) {
                images.add(file);
            } else if (FileUtils.isFont(file)) {
                fonts.add(file);
            } else if (FileUtils.isSkin(file)) {
                existingSkin = file;
            }
        }

        if (!images.isEmpty()) {
            this.texturePackerDAO.pack(dir, outputDir, assetsRoot, images);
        }

        if (!fonts.isEmpty()) {
            this.copyFontsToOutput(dir, outputDir, assetsRoot, fonts);
        }

        this.handleSkinFile(dir, outputDir, assetsRoot, existingSkin, fonts);
    }

    private void copyFontsToOutput(File sourceDir, File outputRoot, Path assetsRoot, List<File> fonts) {
        File targetDir = FileUtils.getOutputDirectory(sourceDir, outputRoot, assetsRoot);

        for (File font : fonts) {
            File targetFile = new File(targetDir, font.getName());
            try {
                Files.copy(font.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Failed to copy font " + font.getName() + ": " + e.getMessage());
            }
        }
    }

    private void handleSkinFile(File sourceDir, File outputRoot, Path assetsRoot, File existingSkin, List<File> fonts) {
        File targetDir = FileUtils.getOutputDirectory(sourceDir, outputRoot, assetsRoot);
        File targetSkin = new File(targetDir, sourceDir.getName() + "_skin.json");

        if (existingSkin != null && existingSkin.exists()) {
            if (fonts.isEmpty()) {
                this.skinDAO.write(existingSkin, targetSkin);
            } else {
                this.skinDAO.merge(targetSkin, existingSkin, fonts);
            }
        } else {
            if (fonts.isEmpty()) {
                this.skinDAO.write(targetSkin);
            } else {
                this.skinDAO.write(targetSkin, fonts);
            }
        }
    }
}