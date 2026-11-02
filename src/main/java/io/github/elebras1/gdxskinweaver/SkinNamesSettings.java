package io.github.elebras1.gdxskinweaver;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class SkinNamesSettings {

    private final Property<Boolean> enabled;
    private final DirectoryProperty outputDir;
    private final Property<String> packageName;
    private final Property<String> className;

    @Inject
    public SkinNamesSettings(ObjectFactory objects) {
        this.enabled = objects.property(Boolean.class).convention(true);
        this.outputDir = objects.directoryProperty();
        this.packageName = objects.property(String.class).convention("io.github.elebras1.gdxskinweaver.generated");
        this.className = objects.property(String.class).convention("SkinNames");
    }

    public Property<Boolean> getEnabled() {
        return enabled;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public Property<String> getPackageName() {
        return packageName;
    }

    public Property<String> getClassName() {
        return className;
    }
}
