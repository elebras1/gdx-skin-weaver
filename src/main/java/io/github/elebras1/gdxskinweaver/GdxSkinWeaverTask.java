package io.github.elebras1.gdxskinweaver;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public abstract class GdxSkinWeaverTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getAssetsDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract SetProperty<String> getExcludedDirs();

    @TaskAction
    public void weave() {
        File assetsDir = this.getAssetsDir().get().getAsFile();
        File outputDir = this.getOutputDir().get().getAsFile();

        Path assetsRoot = assetsDir.toPath();
        String relativeOutput = assetsRoot.relativize(outputDir.toPath()).toString();

        Set<String> excludedDirsWithOutput = new HashSet<>(this.getExcludedDirs().get());
        excludedDirsWithOutput.add(relativeOutput);

        processWeave(assetsDir, outputDir, excludedDirsWithOutput, assetsRoot);
    }

    private void processWeave(File dir, File outputDir, Set<String> excludedDirs, Path assetsRoot) {
        System.out.println("Weaving directory: " + dir.getAbsolutePath());
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String relativePath = assetsRoot.relativize(file.toPath()).toString();

            if (file.isDirectory()) {
                if (excludedDirs.contains(relativePath)) {
                    System.out.println("Skipping excluded directory: " + relativePath);
                    continue;
                }
                this.processWeave(file, outputDir, excludedDirs, assetsRoot);
            } else if (this.isImage(file)) {
                this.processImage(file);
            }
        }
    }

    private void processImage(File file) {
    }

    private boolean isImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".ktx2");
    }
}