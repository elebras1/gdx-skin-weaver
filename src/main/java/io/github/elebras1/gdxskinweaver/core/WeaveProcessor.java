package io.github.elebras1.gdxskinweaver.core;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.DirectorySnapshot;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.assets.FontPageParser;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WeaveProcessor {
    private final DirectoryScanner directoryScanner;
    private final FontPageParser fontPageParser;
    private final FontPageCopier fontPageCopier;
    private final StagingPreparer stagingPreparer;
    private final TexturePackerService texturePackerDAO;
    private final SkinFileHandler skinFileHandler;

    public WeaveProcessor(DirectoryScanner directoryScanner, FontPageParser fontPageParser, FontPageCopier fontPageCopier, StagingPreparer stagingPreparer, TexturePackerService texturePackerDAO, SkinFileHandler skinFileHandler) {
        this.directoryScanner = directoryScanner;
        this.fontPageParser = fontPageParser;
        this.fontPageCopier = fontPageCopier;
        this.stagingPreparer = stagingPreparer;
        this.texturePackerDAO = texturePackerDAO;
        this.skinFileHandler = skinFileHandler;
    }

    public void process(File root, WeaveContext context) {
        processDirectory(root, context);
    }

    private void processDirectory(File dir, WeaveContext context) {
        DirectorySnapshot snapshot = directoryScanner.scan(dir);
        processSubdirectories(snapshot, context);

        Set<File> fontPageImages = fontPageParser.parse(snapshot.fonts());
        List<File> nonFontImages = filterNonFontImages(snapshot.images(), fontPageImages);

        fontPageCopier.copy(dir, context.outputDir(), context.assetsRoot(), snapshot.fonts(), fontPageImages);

        if (!nonFontImages.isEmpty()) {
            StagingPreparer.StagingResult staging = stagingPreparer.prepare(dir, context.temporaryDir(), context.assetsRoot(), fontPageImages);

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

    private List<File> filterNonFontImages(List<File> images, Set<File> fontPageImages) {
        List<File> nonFontImages = new ArrayList<>();
        for (File img : images) {
            if (!fontPageImages.contains(img.getAbsoluteFile())) {
                nonFontImages.add(img);
            }
        }
        return nonFontImages;
    }
}
