package io.github.elebras1.gdxskinweaver.task;

import io.github.elebras1.gdxskinweaver.assets.DirectoryScanner;
import io.github.elebras1.gdxskinweaver.assets.FontPageCopier;
import io.github.elebras1.gdxskinweaver.core.WeaveContext;
import io.github.elebras1.gdxskinweaver.core.WeaveProcessor;
import io.github.elebras1.gdxskinweaver.names.SkinNamesGenerator;
import io.github.elebras1.gdxskinweaver.service.SkinJsonService;
import io.github.elebras1.gdxskinweaver.service.TexturePackerService;
import io.github.elebras1.gdxskinweaver.skin.SkinFileHandler;
import io.github.elebras1.gdxskinweaver.skin.SkinGenerationConfig;
import io.github.elebras1.gdxskinweaver.skin.SkinValidator;
import io.github.elebras1.gdxskinweaver.staging.StagingPreparer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@CacheableTask
public abstract class GdxSkinWeaverTask extends DefaultTask {

    @Internal
    public abstract DirectoryProperty getAssetsDir();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileTree getAssetFiles() {
        File assetsDir = getAssetsDir().get().getAsFile();
        File outputDir = getOutputDir().get().getAsFile();
        Path assetsPath = assetsDir.toPath().toAbsolutePath().normalize();
        Path outputPath = outputDir.toPath().toAbsolutePath().normalize();

        if (outputPath.startsWith(assetsPath)) {
            String relativeOutput = assetsPath.relativize(outputPath).toString().replace('\\', '/');
            if (!relativeOutput.isEmpty()) {
                return getProject().fileTree(assetsDir).matching(pattern -> pattern.exclude(relativeOutput + "/**"));
            }
        }
        return getProject().fileTree(assetsDir);
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract SetProperty<String> getExcludedDirs();

    @Input
    public abstract MapProperty<String, String> getPackerOptions();

    @Input
    public abstract Property<Boolean> getGenerateLabelStyles();

    @Input
    public abstract ListProperty<String> getLabelStyleColors();

    @Input
    public abstract Property<String> getDefaultLabelColor();

    @Input
    public abstract Property<Boolean> getValidateSkins();

    @Input
    public abstract Property<Boolean> getPackFontPages();

    @Input
    public abstract Property<Boolean> getGenerateNames();

    @Input
    public abstract Property<String> getNamesPackage();

    @Input
    public abstract Property<String> getNamesClassName();

    @OutputDirectory
    public abstract DirectoryProperty getNamesOutputDir();

    private final WeaveProcessor weaveProcessor;
    private final SkinNamesGenerator skinNamesGenerator;

    public GdxSkinWeaverTask() {
        SkinJsonService skinService = new SkinJsonService();
        this.weaveProcessor = new WeaveProcessor(new DirectoryScanner(), new StagingPreparer(), new FontPageCopier(),
                new TexturePackerService(), new SkinFileHandler(skinService), new SkinValidator());
        this.skinNamesGenerator = new SkinNamesGenerator();

        getPackerOptions().convention(Collections.emptyMap());
        getGenerateLabelStyles().convention(true);
        getLabelStyleColors().convention(Collections.emptyList());
        getDefaultLabelColor().convention("");
        getValidateSkins().convention(true);
        getPackFontPages().convention(true);
        getGenerateNames().convention(true);
        getNamesPackage().convention("io.github.elebras1.gdxskinweaver.generated");
        getNamesClassName().convention("SkinNames");
        getNamesOutputDir().convention(getProject().getLayout().getBuildDirectory().dir("generated/sources/skinWeaver/java"));
    }

    @TaskAction
    public void weave() {
        File assetsDir = this.getAssetsDir().get().getAsFile();
        File outputDir = this.getOutputDir().get().getAsFile();

        Path assetsRoot = assetsDir.toPath();
        String relativeOutput = assetsRoot.relativize(outputDir.toPath()).toString().replace('\\', '/');

        Set<String> excludedDirsWithOutput = new HashSet<>();
        for (String excludedDir : this.getExcludedDirs().get()) {
            excludedDirsWithOutput.add(excludedDir.replace('\\', '/'));
        }
        excludedDirsWithOutput.add(relativeOutput);

        SkinGenerationConfig skinGenerationConfig = new SkinGenerationConfig(
                this.getGenerateLabelStyles().get(),
                this.getLabelStyleColors().get(),
                this.getDefaultLabelColor().getOrNull(),
                this.getValidateSkins().get(),
                Boolean.parseBoolean(this.getPackerOptions().get().getOrDefault("flattenPaths", "true"))
        );

        WeaveContext context = new WeaveContext(assetsRoot, outputDir, excludedDirsWithOutput, getTemporaryDir(),
                this.getPackerOptions().get(), this.getPackFontPages().get(), skinGenerationConfig);
        weaveProcessor.process(assetsDir, context);

        if (this.getGenerateNames().get()) {
            this.skinNamesGenerator.generate(outputDir, this.getNamesOutputDir().get().getAsFile(),
                    this.getNamesPackage().get(), this.getNamesClassName().get(),
                    this.getPackerOptions().get().getOrDefault("atlasExtension", ".atlas"));
        } else {
            this.skinNamesGenerator.clean(this.getNamesOutputDir().get().getAsFile(),
                    this.getNamesPackage().get(), this.getNamesClassName().get());
        }
    }
}
