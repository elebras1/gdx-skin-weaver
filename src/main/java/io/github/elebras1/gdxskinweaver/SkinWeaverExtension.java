package io.github.elebras1.gdxskinweaver;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.file.DirectoryProperty;
import javax.inject.Inject;

public abstract class SkinWeaverExtension {
    private final DirectoryProperty assetsDir;
    private final DirectoryProperty outputDir;
    private final SetProperty<String> excludedDirs;

    @Inject
    public SkinWeaverExtension(ObjectFactory objects) {
        this.assetsDir = objects.directoryProperty();
        this.outputDir = objects.directoryProperty();
        this.excludedDirs = objects.setProperty(String.class);
    }

    public DirectoryProperty getAssetsDir() {
        return assetsDir;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public SetProperty<String> getExcludedDirs() {
        return excludedDirs;
    }
}