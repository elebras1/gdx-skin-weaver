package io.github.elebras1.gdxskinweaver.staging;

import io.github.elebras1.gdxskinweaver.image.ImageContrastAdjuster;
import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StagingPreparer {
    private final ImageContrastAdjuster contrastAdjuster = new ImageContrastAdjuster();

    public StagingResult prepare(File sourceDir, File stagingRoot, Path assetsRoot, List<File> excludedImages) {
        StagedImages staged = new StagedImages();
        Map<String, String> buttonStyles = new LinkedHashMap<>();
        Map<String, String> toggleStyles = new LinkedHashMap<>();

        Set<Path> excludedPaths = new HashSet<>();
        for (File file : excludedImages) {
            excludedPaths.add(file.toPath().toAbsolutePath().normalize());
        }

        File[] listedImages = sourceDir.listFiles(AssetFileUtils::isImage);
        if (listedImages == null) {
            return new StagingResult(staged.list(), buttonStyles, toggleStyles);
        }

        List<File> images = new ArrayList<>(Arrays.asList(listedImages));
        images.sort(Comparator.comparing(File::getName));
        images.removeIf(file -> excludedPaths.contains(file.toPath().toAbsolutePath().normalize()));
        images = deduplicate(images);

        Map<String, Map<String, File>> groups = new LinkedHashMap<>();
        for (File file : images) {
            String stem = logicalStem(file);
            int lastUnderscore = stem.lastIndexOf('_');
            String prefix = lastUnderscore == -1 ? stem : stem.substring(0, lastUnderscore);
            String suffix = lastUnderscore == -1 ? "" : stem.substring(lastUnderscore + 1);
            groups.computeIfAbsent(prefix, key -> new LinkedHashMap<>()).put(suffix, file);
        }

        for (Map.Entry<String, Map<String, File>> entry : groups.entrySet()) {
            String prefix = entry.getKey();
            Map<String, File> parts = entry.getValue();

            if (parts.containsKey("up") && parts.containsKey("down") && parts.containsKey("over")) {
                File up = parts.get("up");
                copyPart(up, stagingRoot, assetsRoot, staged);
                copyPart(parts.get("down"), stagingRoot, assetsRoot, staged);
                copyPart(parts.get("over"), stagingRoot, assetsRoot, staged);
                buttonStyles.put(prefix, fullName(up, assetsRoot, prefix));
            } else if (parts.containsKey("on") && parts.containsKey("off")) {
                File on = parts.get("on");
                copyPart(on, stagingRoot, assetsRoot, staged);
                copyPart(parts.get("off"), stagingRoot, assetsRoot, staged);
                toggleStyles.put(prefix, fullName(on, assetsRoot, prefix));
            } else if (parts.containsKey("btn")) {
                File baseFile = parts.get("btn");
                stageFile(baseFile, stagingRoot, assetsRoot, staged);
                buttonStyles.put(prefix + "_btn", fullName(baseFile, assetsRoot, prefix + "_btn"));

                handleOptionalVariant(sourceDir, stagingRoot, assetsRoot, baseFile, prefix + "_btn_down", -15f, staged);
                handleOptionalVariant(sourceDir, stagingRoot, assetsRoot, baseFile, prefix + "_btn_over", 15f, staged);
                copyRemainingParts(parts, Set.of("btn"), stagingRoot, assetsRoot, staged);
            } else if (parts.containsKey("tgl")) {
                File baseFile = parts.get("tgl");
                handleOptionalVariant(sourceDir, stagingRoot, assetsRoot, baseFile, prefix + "_tgl_off", 0f, staged);
                handleOptionalVariant(sourceDir, stagingRoot, assetsRoot, baseFile, prefix + "_tgl_on", 15f, staged);
                toggleStyles.put(prefix + "_tgl", fullName(baseFile, assetsRoot, prefix + "_tgl"));
                copyRemainingParts(parts, Set.of("tgl"), stagingRoot, assetsRoot, staged);
            } else {
                copyRemainingParts(parts, Set.of(), stagingRoot, assetsRoot, staged);
            }
        }

        return new StagingResult(staged.list(), buttonStyles, toggleStyles);
    }

    private List<File> deduplicate(List<File> images) {
        Map<String, File> selected = new LinkedHashMap<>();
        for (File file : images) {
            String stem = logicalStem(file);
            File current = selected.get(stem);
            if (current == null) {
                selected.put(stem, file);
                continue;
            }

            File keep = priority(file) > priority(current) ? file : current;
            File drop = keep == file ? current : file;
            selected.put(stem, keep);
            warn("Duplicate image region '" + stem + "': keeping " + keep.getName() + ", ignoring " + drop.getName() + ".");
        }
        return new ArrayList<>(selected.values());
    }

    private int priority(File file) {
        int score = 0;
        if (isNinePatch(file)) {
            score += 100;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            score += 10;
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            score += 5;
        } else if (name.endsWith(".ktx2")) {
            score += 1;
        }
        return score;
    }

    private void handleOptionalVariant(File sourceDir, File stagingRoot, Path assetsRoot, File baseImage,
                                       String variantName, float contrastPercent, StagedImages staged) {
        File manualFile = findManualVariant(sourceDir, variantName);
        File stagedFile;

        if (manualFile != null) {
            stagedFile = stagingFile(stagingRoot, manualFile, assetsRoot);
            copyFile(manualFile, stagedFile);
        } else {
            stagedFile = stagingFileForVariant(baseImage, stagingRoot, assetsRoot, variantName);
            if (contrastPercent == 0) {
                copyFile(baseImage, stagedFile);
            } else {
                generateVariant(baseImage, stagedFile, contrastPercent);
            }
        }

        staged.add(stagedFile);
    }

    private File findManualVariant(File sourceDir, String variantName) {
        File[] matches = sourceDir.listFiles(file -> AssetFileUtils.isImage(file) && logicalStem(file).equals(variantName));
        if (matches == null || matches.length == 0) {
            return null;
        }

        Arrays.sort(matches, Comparator.comparing(File::getName));
        File best = matches[0];
        for (File file : matches) {
            if (priority(file) > priority(best)) {
                best = file;
            }
        }
        return best;
    }

    private void copyPart(File source, File stagingRoot, Path assetsRoot, StagedImages staged) {
        stageFile(source, stagingRoot, assetsRoot, staged);
    }

    private File stageFile(File source, File stagingRoot, Path assetsRoot, StagedImages staged) {
        File target = stagingFile(stagingRoot, source, assetsRoot);
        copyFile(source, target);
        staged.add(target);
        return target;
    }

    private void copyRemainingParts(Map<String, File> parts, Set<String> excludedSuffixes, File stagingRoot,
                                    Path assetsRoot, StagedImages staged) {
        for (Map.Entry<String, File> entry : parts.entrySet()) {
            if (excludedSuffixes.contains(entry.getKey())) {
                continue;
            }
            copyPart(entry.getValue(), stagingRoot, assetsRoot, staged);
        }
    }

    private File stagingFile(File stagingRoot, File source, Path assetsRoot) {
        return stagingRoot.toPath().resolve(assetsRoot.relativize(source.toPath())).toFile();
    }

    private File stagingFileForVariant(File baseImage, File stagingRoot, Path assetsRoot, String variantName) {
        Path relative = assetsRoot.relativize(baseImage.toPath());
        Path parent = relative.getParent();
        String fileName = variantName + (isNinePatch(baseImage) ? ".9" : "") + extension(baseImage);
        Path target = parent == null ? Path.of(fileName) : parent.resolve(fileName);
        return stagingRoot.toPath().resolve(target).toFile();
    }

    private String fullName(File file, Path assetsRoot, String name) {
        Path parent = assetsRoot.relativize(file.toPath()).getParent();
        return parent == null ? name : parent.toString().replace('\\', '/') + "/" + name;
    }

    private String logicalStem(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String stem = dot == -1 ? name : name.substring(0, dot);
        if (stem.toLowerCase(Locale.ROOT).endsWith(".9")) {
            stem = stem.substring(0, stem.length() - 2);
        }
        return stem;
    }

    private String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot);
    }

    private boolean isNinePatch(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".9.png") || name.endsWith(".9.jpg")
                || name.endsWith(".9.jpeg") || name.endsWith(".9.ktx2");
    }

    private void copyFile(File src, File dst) {
        try {
            File parent = dst.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy " + src + " to " + dst, e);
        }
    }

    private void generateVariant(File src, File dst, float percent) {
        try {
            contrastAdjuster.apply(src, dst, percent);
        } catch (IOException e) {
            warn("Cannot generate " + dst.getName() + " from " + src.getName() + ": " + e.getMessage()
                    + ". Falling back to a copy.");
            copyFile(src, dst);
        }
    }

    private void warn(String message) {
        System.err.println("[gdx-skin-weaver] " + message);
    }

    public record StagingResult(List<File> images, Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {
        public StagingResult {
            images = images == null ? List.of() : List.copyOf(images);
            buttonSimpleToFull = buttonSimpleToFull == null ? Map.of() : Map.copyOf(buttonSimpleToFull);
            toggleSimpleToFull = toggleSimpleToFull == null ? Map.of() : Map.copyOf(toggleSimpleToFull);
        }
    }

    private static final class StagedImages {
        private final Map<Path, File> files = new LinkedHashMap<>();

        void add(File file) {
            files.putIfAbsent(file.toPath().toAbsolutePath().normalize(), file);
        }

        List<File> list() {
            return new ArrayList<>(files.values());
        }
    }
}
