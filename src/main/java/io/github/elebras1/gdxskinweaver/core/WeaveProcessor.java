package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.DirectorySnapshot;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.assets.FontPageResolver;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.skin.SkinValidator;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;
import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WeaveProcessor {
    private final DirectoryScanner directoryScanner;
    private final StagingPreparer stagingPreparer;
    private final FontPageCopier fontPageCopier;
    private final TexturePackerService texturePackerDAO;
    private final SkinFileHandler skinFileHandler;
    private final SkinValidator skinValidator;

    public WeaveProcessor(DirectoryScanner directoryScanner, StagingPreparer stagingPreparer, FontPageCopier fontPageCopier,
                          TexturePackerService texturePackerService, SkinFileHandler skinFileHandler) {
        this(directoryScanner, stagingPreparer, fontPageCopier, texturePackerService, skinFileHandler, new SkinValidator());
    }

    public WeaveProcessor(DirectoryScanner directoryScanner, StagingPreparer stagingPreparer, FontPageCopier fontPageCopier,
                          TexturePackerService texturePackerService, SkinFileHandler skinFileHandler, SkinValidator skinValidator) {
        this.directoryScanner = directoryScanner;
        this.stagingPreparer = stagingPreparer;
        this.fontPageCopier = fontPageCopier;
        this.texturePackerDAO = texturePackerService;
        this.skinFileHandler = skinFileHandler;
        this.skinValidator = skinValidator;
    }

    public void process(File root, WeaveContext context) {
        if (context.excludedDirs().contains("")) {
            // The assets root is excluded: skip its own files but still process its subdirectories.
            processSubdirectories(directoryScanner.scan(root), context);
            return;
        }
        processDirectory(root, context);
    }

    private void processDirectory(File dir, WeaveContext context) {
        DirectorySnapshot snapshot = directoryScanner.scan(dir);
        processSubdirectories(snapshot, context);

        List<File> copiedPages = context.copyFontPages() ? snapshot.fontPages() : List.of();
        fontPageCopier.copy(dir, context.outputDir(), context.assetsRoot(), snapshot.fonts(), copiedPages);
        if (!context.copyFontPages()) {
            fontPageCopier.removeCopies(context.outputDir(), context.assetsRoot(), snapshot.fontPages());
        }
        Set<String> copiedFontPages = copiedPages.stream().map(File::getName).collect(Collectors.toSet());

        String atlasName = dir.getName().isEmpty() ? "pack" : dir.getName();
        File targetDir = AssetFileUtils.getOutputDirectory(dir, context.outputDir(), context.assetsRoot());

        if (!snapshot.images().isEmpty()) {
            List<File> excludedImages = context.packFontPages() ? Collections.emptyList() : snapshot.fontPages();
            Map<File, List<String>> fontPageRegions = context.packFontPages() ? fontPageRegions(snapshot) : Map.of();
            StagingPreparer.StagingResult staging = stagingPreparer.prepare(dir, context.temporaryDir(), context.assetsRoot(),
                    excludedImages, fontPageRegions);

            if (!staging.images().isEmpty() && targetDir != null) {
                texturePackerDAO.pack(context.temporaryDir(), targetDir, atlasName, staging.images(), context.packerOptions(),
                        copiedFontPages);
                warnFontsShadowedByAtlas(snapshot, atlasName, context);
            } else if (targetDir != null) {
                texturePackerDAO.clean(targetDir, atlasName, context.packerOptions(), copiedFontPages);
            }

            File targetSkin = skinFileHandler.handle(dir, context.outputDir(), context.assetsRoot(), snapshot.existingSkin(),
                    snapshot.fonts(), staging.buttonSimpleToFull(), staging.toggleSimpleToFull(), context.skinGeneration());
            validate(targetSkin, targetDir, atlasName, context);
            return;
        }

        if (targetDir != null) {
            texturePackerDAO.clean(targetDir, atlasName, context.packerOptions(), copiedFontPages);
        }
        File targetSkin = skinFileHandler.handle(dir, context.outputDir(), context.assetsRoot(), snapshot.existingSkin(),
                snapshot.fonts(), Collections.emptyMap(), Collections.emptyMap(), context.skinGeneration());
        validate(targetSkin, targetDir, atlasName, context);
    }

    /**
     * Without packed font pages, a libGDX {@code Skin} falls back to {@code <font>.png} next to the {@code .fnt} file
     * as the font texture. When the font is named like its folder, that file is the first atlas page.
     */
    private void warnFontsShadowedByAtlas(DirectorySnapshot snapshot, String atlasName, WeaveContext context) {
        if (context.packFontPages()) {
            return;
        }
        for (File font : snapshot.fonts()) {
            String fontName = AssetFileUtils.getBaseName(font);
            if (!fontName.equals(atlasName)) {
                continue;
            }
            System.err.println("[gdx-skin-weaver] Font '" + fontName + "' has the same name as its folder atlas: a Skin "
                    + "would load the atlas page '" + fontName + ".png' as the font texture. Rename the font or the "
                    + "folder, or enable packFontPages.");
        }
    }

    /** Font pages are packed under the names a libGDX {@code Skin} looks up when it loads the font. */
    private Map<File, List<String>> fontPageRegions(DirectorySnapshot snapshot) {
        Map<File, List<String>> regions = new LinkedHashMap<>();
        for (FontPageResolver.FontPage page : snapshot.fontPageEntries()) {
            regions.computeIfAbsent(page.file(), key -> new ArrayList<>()).add(page.regionName());
        }
        return regions;
    }

    private void processSubdirectories(DirectorySnapshot snapshot, WeaveContext context) {
        for (File subdir : snapshot.subdirectories()) {
            String relativePath = context.assetsRoot().relativize(subdir.toPath()).toString().replace('\\', '/');
            if (context.excludedDirs().contains(relativePath)) {
                continue;
            }
            processDirectory(subdir, context);
        }
    }

    private void validate(File targetSkin, File targetDir, String atlasName, WeaveContext context) {
        if (targetSkin == null || !context.skinGeneration().validationEnabled()) {
            return;
        }

        String extension = context.packerOptions().getOrDefault("atlasExtension", ".atlas");
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        File atlasFile = targetDir == null ? null : new File(targetDir, atlasName + extension);
        List<String> warnings = skinValidator.validate(targetSkin, atlasFile != null && atlasFile.exists() ? atlasFile : null);
        for (String warning : warnings) {
            System.err.println("[gdx-skin-weaver] " + warning);
        }
    }
}
