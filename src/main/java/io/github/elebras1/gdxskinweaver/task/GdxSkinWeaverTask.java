package io.github.elebras1.gdxskinweaver.task;

import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;
import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.assets.FontPageParser;
import io.github.elebras1.gdxskinweaver.core.WeaveContext;
import io.github.elebras1.gdxskinweaver.core.WeaveProcessor;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.service.SkinJsonService;
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

    private final TexturePackerService texturePackerDAO;
    private final SkinJsonService skinDAO;
    private final StagingPreparer stagingPreparer;
    private final WeaveProcessor weaveProcessor;

    public GdxSkinWeaverTask() {
        this.texturePackerDAO = new TexturePackerService();
        this.skinDAO = new SkinJsonService();
        this.stagingPreparer = new StagingPreparer();
        this.weaveProcessor = new WeaveProcessor(new DirectoryScanner(), new FontPageParser(), new FontPageCopier(), stagingPreparer, texturePackerDAO, new SkinFileHandler(skinDAO));
    }

    @TaskAction
    public void weave() {
        File assetsDir = this.getAssetsDir().get().getAsFile();
        File outputDir = this.getOutputDir().get().getAsFile();

        Path assetsRoot = assetsDir.toPath();
        String relativeOutput = assetsRoot.relativize(outputDir.toPath()).toString();

        Set<String> excludedDirsWithOutput = new HashSet<>(this.getExcludedDirs().get());
        excludedDirsWithOutput.add(relativeOutput);

        WeaveContext context = new WeaveContext(assetsRoot, outputDir, excludedDirsWithOutput, getTemporaryDir());
        weaveProcessor.process(assetsDir, context);
    }
}
