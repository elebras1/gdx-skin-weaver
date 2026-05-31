package io.github.elebras1.gdxskinweaver.task;

import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;
import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
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

    private final WeaveProcessor weaveProcessor;

    public GdxSkinWeaverTask() {
        SkinJsonService skinService = new SkinJsonService();
        this.weaveProcessor = new WeaveProcessor(new DirectoryScanner(), new StagingPreparer(), new FontPageCopier(),  new TexturePackerService(), new SkinFileHandler(skinService));
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
