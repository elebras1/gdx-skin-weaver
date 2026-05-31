package io.github.elebras1.gdxskinweaver.staging;

import io.github.elebras1.gdxskinweaver.image.ImageContrastAdjuster;
import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StagingPreparer {
    private final ImageContrastAdjuster contrastAdjuster = new ImageContrastAdjuster();

    public StagingResult prepare(File sourceDir, File stagingRoot, Path assetsRoot, Set<File> excludeFiles) {
        List<File> stagedImages = new ArrayList<>();
        Map<String, String> buttonSimpleToFull = new HashMap<>();
        Map<String, String> toggleSimpleToFull = new HashMap<>();

        File[] images = sourceDir.listFiles(AssetFileUtils::isImage);
        if (images == null) return new StagingResult(stagedImages, buttonSimpleToFull, toggleSimpleToFull);

        for (File original : images) {
            if (excludeFiles != null && excludeFiles.contains(original.getAbsoluteFile())) {
                continue;
            }

            String fileName = original.getName();
            String simpleName = fileName.replaceFirst("\\.[^.]*$", "");

            Path relPath = assetsRoot.relativize(original.toPath());
            String parent = relPath.getParent() == null ? "" : relPath.getParent().toString();
            String fullRegion = relPath.toString().replaceFirst("\\.[^.]*$", "");

            if (simpleName.endsWith("_btn")) {
                File stagedOriginal = new File(stagingRoot, relPath.toString());
                copyFile(original, stagedOriginal);
                stagedImages.add(stagedOriginal);

                buttonSimpleToFull.put(simpleName, fullRegion);

                String prefix = fullRegion.substring(0, fullRegion.length() - 4);
                String ext = getExtension(original);
                File down = new File(stagingRoot, parent + "/" + prefix + "_btn_down" + ext);
                generateVariant(original, down, -15f);
                stagedImages.add(down);
                File over = new File(stagingRoot, parent + "/" + prefix + "_btn_over" + ext);
                generateVariant(original, over, 15f);
                stagedImages.add(over);

            } else if (simpleName.endsWith("_tgl")) {
                String prefix = fullRegion.substring(0, fullRegion.length() - 4);
                String ext = getExtension(original);

                File offFile = new File(stagingRoot, parent + "/" + prefix + "_tgl_off" + ext);
                copyFile(original, offFile);
                stagedImages.add(offFile);

                File onFile = new File(stagingRoot, parent + "/" + prefix + "_tgl_on.png");
                generateVariant(original, onFile, 15f);
                stagedImages.add(onFile);

                toggleSimpleToFull.put(simpleName, fullRegion);

            } else {
                File stagedOriginal = new File(stagingRoot, relPath.toString());
                copyFile(original, stagedOriginal);
                stagedImages.add(stagedOriginal);
            }
        }
        return new StagingResult(stagedImages, buttonSimpleToFull, toggleSimpleToFull);
    }

    private String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot);
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

    public record StagingResult(List<File> images, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {}
}