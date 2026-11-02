package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.DirectorySnapshot;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.skin.SkinValidator;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;
import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

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
        processDirectory(root, context);
    }

    private void processDirectory(File dir, WeaveContext context) {
        DirectorySnapshot snapshot = directoryScanner.scan(dir);
        processSubdirectories(snapshot, context);

        fontPageCopier.copy(dir, context.outputDir(), context.assetsRoot(), snapshot.fonts(), snapshot.fontPages());

        String atlasName = dir.getName().isEmpty() ? "pack" : dir.getName();
        File targetDir = AssetFileUtils.getOutputDirectory(dir, context.outputDir(), context.assetsRoot());

        if (!snapshot.images().isEmpty()) {
            List<File> excludedImages = context.packFontPages() ? Collections.emptyList() : snapshot.fontPages();
            StagingPreparer.StagingResult staging = stagingPreparer.prepare(dir, context.temporaryDir(), context.assetsRoot(), excludedImages);

            if (!staging.images().isEmpty() && targetDir != null) {
                texturePackerDAO.pack(context.temporaryDir(), targetDir, atlasName, staging.images(), context.packerOptions());
            } else if (targetDir != null) {
                texturePackerDAO.clean(targetDir, atlasName, context.packerOptions());
            }

            File targetSkin = skinFileHandler.handle(dir, context.outputDir(), context.assetsRoot(), snapshot.existingSkin(),
                    snapshot.fonts(), staging.buttonSimpleToFull(), staging.toggleSimpleToFull(), context.skinGeneration());
            validate(targetSkin, targetDir, atlasName, context);
            return;
        }

        if (targetDir != null) {
            texturePackerDAO.clean(targetDir, atlasName, context.packerOptions());
        }
        File targetSkin = skinFileHandler.handle(dir, context.outputDir(), context.assetsRoot(), snapshot.existingSkin(),
                snapshot.fonts(), Collections.emptyMap(), Collections.emptyMap(), context.skinGeneration());
        validate(targetSkin, targetDir, atlasName, context);
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
