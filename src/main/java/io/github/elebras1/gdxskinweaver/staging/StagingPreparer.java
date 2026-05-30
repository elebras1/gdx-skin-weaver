package io.github.elebras1.gdxskinweaver.staging;

import io.github.elebras1.gdxskinweaver.image.ContrastAdjuster;
import io.github.elebras1.gdxskinweaver.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StagingPreparer {
    private final ContrastAdjuster contrastAdjuster = new ContrastAdjuster();

    public StagingResult prepare(File sourceDir, File stagingRoot, Path assetsRoot) {
        List<File> stagedImages = new ArrayList<>();
        Map<String, String> buttonSimpleToFull = new HashMap<>();

        File[] images = sourceDir.listFiles(FileUtils::isImage);
        if (images == null) return new StagingResult(stagedImages, buttonSimpleToFull);

        for (File original : images) {
            Path relPath = assetsRoot.relativize(original.toPath());
            File stagedOriginal = new File(stagingRoot, relPath.toString());
            copyFile(original, stagedOriginal);
            stagedImages.add(stagedOriginal);

            String fullRegion = relPath.toString().replaceFirst("\\.[^.]*$", "");
            if (fullRegion.endsWith("_btn")) {
                String simpleName = relPath.getFileName().toString().replaceFirst("\\.[^.]*$", "");
                buttonSimpleToFull.put(simpleName, fullRegion);

                String prefix = fullRegion.substring(0, fullRegion.length() - 4);
                String parent = relPath.getParent() == null ? "" : relPath.getParent().toString();
                String ext = ".png";
                File down = new File(stagingRoot, parent + "/" + prefix + "_btn_down" + ext);
                generateVariant(original, down, -15f);
                stagedImages.add(down);
                File over = new File(stagingRoot, parent + "/" + prefix + "_btn_over" + ext);
                generateVariant(original, over, 15f);
                stagedImages.add(over);
            }
        }
        return new StagingResult(stagedImages, buttonSimpleToFull);
    }

    private void copyFile(File src, File dst) {
        try {
            dst.getParentFile().mkdirs();
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy " + src + " to " + dst, e);
        }
    }

    private void generateVariant(File src, File dst, float percent) {
        try {
            contrastAdjuster.apply(src, dst, percent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate " + dst, e);
        }
    }

    public record StagingResult(List<File> images, Map<String, String> buttonSimpleToFull) {}
}