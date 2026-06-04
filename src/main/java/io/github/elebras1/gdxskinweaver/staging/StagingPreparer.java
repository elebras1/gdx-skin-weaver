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

    public StagingResult prepare(File sourceDir, File stagingRoot, Path assetsRoot, List<File> excludedImages) {
        List<File> stagedImages = new ArrayList<>();
        Map<String, String> buttonStyles = new HashMap<>();
        Map<String, String> toggleStyles = new HashMap<>();

        Set<Path> excludedPaths = new HashSet<>();
        for (File file : excludedImages) {
            excludedPaths.add(file.toPath().toAbsolutePath().normalize());
        }

        File[] images = sourceDir.listFiles(AssetFileUtils::isImage);
        if (images == null) {
            return new StagingResult(stagedImages, buttonStyles, toggleStyles);
        }

        Map<String, Map<String, File>> groups = new LinkedHashMap<>();
        for (File file : images) {
            if (excludedPaths.contains(file.toPath().toAbsolutePath().normalize())) {
                continue;
            }
            String name = file.getName();
            String noExt = name.replaceFirst("\\.[^.]*$", "");
            int lastUnderscore = noExt.lastIndexOf('_');
            String prefix = lastUnderscore == -1 ? noExt : noExt.substring(0, lastUnderscore);
            String suffix = lastUnderscore == -1 ? "" : noExt.substring(lastUnderscore + 1);
            groups.computeIfAbsent(prefix, k -> new HashMap<>()).put(suffix, file);
        }

        for (Map.Entry<String, Map<String, File>> entry : groups.entrySet()) {
            String prefix = entry.getKey();
            Map<String, File> parts = entry.getValue();

            File firstFile = parts.values().iterator().next();
            Path rel = assetsRoot.relativize(firstFile.toPath());
            String parent = rel.getParent() == null ? "" : rel.getParent().toString();

            if (parts.containsKey("up") && parts.containsKey("down") && parts.containsKey("over")) {
                String ext = getExtension(parts.get("up"));
                copyPart(parts, "up", prefix, parent, ext, stagingRoot, stagedImages);
                copyPart(parts, "down", prefix, parent, ext, stagingRoot, stagedImages);
                copyPart(parts, "over", prefix, parent, ext, stagingRoot, stagedImages);
                buttonStyles.put(prefix, prefix);
            } else if (parts.containsKey("on") && parts.containsKey("off")) {
                String ext = getExtension(parts.get("on"));
                copyPart(parts, "on", prefix, parent, ext, stagingRoot, stagedImages);
                copyPart(parts, "off", prefix, parent, ext, stagingRoot, stagedImages);
                toggleStyles.put(prefix, prefix);
            } else if (parts.containsKey("btn")) {
                File baseFile = parts.get("btn");
                String ext = getExtension(baseFile);
                File stagedBase = new File(stagingRoot, parent + "/" + prefix + "_btn" + ext);
                copyFile(baseFile, stagedBase);
                stagedImages.add(stagedBase);
                buttonStyles.put(prefix + "_btn", prefix + "_btn");

                handleOptionalVariant(sourceDir, stagingRoot, baseFile, parent, prefix + "_btn_down", ext, -15f, stagedImages);
                handleOptionalVariant(sourceDir, stagingRoot, baseFile, parent, prefix + "_btn_over", ext, +15f, stagedImages);
            } else if (parts.containsKey("tgl")) {
                File baseFile = parts.get("tgl");
                String ext = getExtension(baseFile);
                handleOptionalVariant(sourceDir, stagingRoot, baseFile, parent, prefix + "_tgl_off", ext, 0, stagedImages);
                handleOptionalVariant(sourceDir, stagingRoot, baseFile, parent, prefix + "_tgl_on", ext, +15f, stagedImages);
                toggleStyles.put(prefix + "_tgl", prefix + "_tgl");
            } else {
                for (File f : parts.values()) {
                    Path fRel = assetsRoot.relativize(f.toPath());
                    File dst = new File(stagingRoot, fRel.toString());
                    copyFile(f, dst);
                    stagedImages.add(dst);
                }
            }
        }

        return new StagingResult(stagedImages, buttonStyles, toggleStyles);
    }

    private void copyPart(Map<String, File> parts, String suffix, String prefix, String parent, String ext, File stagingRoot, List<File> stagedImages) {
        File src = parts.get(suffix);
        File dst = new File(stagingRoot, parent + "/" + prefix + "_" + suffix + ext);
        copyFile(src, dst);
        stagedImages.add(dst);
    }

    private void handleOptionalVariant(File sourceDir, File stagingRoot, File baseImage, String parent, String variantName, String ext, float contrastPercent, List<File> stagedImages) {
        File manualFile = new File(sourceDir, variantName + ext);
        File stagedFile = new File(stagingRoot, parent + "/" + variantName + ext);
        if (manualFile.exists()) {
            copyFile(manualFile, stagedFile);
        } else if (contrastPercent == 0) {
            copyFile(baseImage, stagedFile);
        } else {
            generateVariant(baseImage, stagedFile, contrastPercent);
        }
        stagedImages.add(stagedFile);
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