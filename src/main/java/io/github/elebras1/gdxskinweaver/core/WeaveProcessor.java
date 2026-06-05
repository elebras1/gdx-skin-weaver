package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.DirectorySnapshot;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

public class WeaveProcessor {
    private final DirectoryScanner directoryScanner;
    private final StagingPreparer stagingPreparer;
    private final FontPageCopier fontPageCopier;
    private final TexturePackerService texturePackerDAO;
    private final SkinFileHandler skinFileHandler;

    public WeaveProcessor(DirectoryScanner directoryScanner, StagingPreparer stagingPreparer, FontPageCopier fontPageCopier, TexturePackerService texturePackerService, SkinFileHandler skinFileHandler) {
        this.directoryScanner = directoryScanner;
        this.stagingPreparer = stagingPreparer;
        this.fontPageCopier = fontPageCopier;
        this.texturePackerDAO = texturePackerService;
        this.skinFileHandler = skinFileHandler;
    }

    public void process(File root, WeaveContext context) {
        processDirectory(root, context);
    }

    private void processDirectory(File dir, WeaveContext context) {
        DirectorySnapshot snapshot = directoryScanner.scan(dir);
        processSubdirectories(snapshot, context);

        fontPageCopier.copy(dir, context.outputDir(), context.assetsRoot(), snapshot.fonts(), snapshot.fontPages());

        if (!snapshot.images().isEmpty()) {
            StagingPreparer.StagingResult staging = stagingPreparer.prepare(dir, context.temporaryDir(), context.assetsRoot(), Collections.emptyList());

            if (!staging.images().isEmpty()) {
                Path relativePath = context.assetsRoot().relativize(dir.toPath());
                File targetDir = context.outputDir().toPath().resolve(relativePath).toFile();
                String atlasName = dir.getName().isEmpty() ? "pack" : dir.getName();
                texturePackerDAO.pack(context.temporaryDir(), targetDir, atlasName, staging.images());
            }

            skinFileHandler.handle(dir, context.outputDir(), context.assetsRoot(), snapshot.existingSkin(), snapshot.fonts(), staging.buttonSimpleToFull(), staging.toggleSimpleToFull());
            return;
        }

        skinFileHandler.handle(dir, context.outputDir(), context.assetsRoot(), snapshot.existingSkin(), snapshot.fonts(), Collections.emptyMap(), Collections.emptyMap());
    }

    private void processSubdirectories(DirectorySnapshot snapshot, WeaveContext context) {
        for (File subdir : snapshot.subdirectories()) {
            String relativePath = context.assetsRoot().relativize(subdir.toPath()).toString();
            if (context.excludedDirs().contains(relativePath)) {
                continue;
            }
            processDirectory(subdir, context);
        }
    }
}
