package io.github.elebras1.gdxskinweaver;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;
import java.util.Collections;

public abstract class SkinWeaverExtension {
    private final DirectoryProperty assetsDir;
    private final DirectoryProperty outputDir;
    private final SetProperty<String> excludedDirs;
    private final PackerSettings packer;
    private final SkinNamesSettings names;
    private final Property<Boolean> generateLabelStyles;
    private final ListProperty<String> labelStyleColors;
    private final Property<String> defaultLabelColor;
    private final Property<Boolean> validateSkins;
    private final Property<Boolean> packFontPages;
    private final Property<Boolean> copyPackedFontPages;

    @Inject
    public SkinWeaverExtension(ObjectFactory objects) {
        this.assetsDir = objects.directoryProperty();
        this.outputDir = objects.directoryProperty();
        this.excludedDirs = objects.setProperty(String.class);
        this.packer = objects.newInstance(PackerSettings.class);
        this.names = objects.newInstance(SkinNamesSettings.class);
        this.generateLabelStyles = objects.property(Boolean.class).convention(true);
        this.labelStyleColors = objects.listProperty(String.class).convention(Collections.emptyList());
        this.defaultLabelColor = objects.property(String.class).convention("");
        this.validateSkins = objects.property(Boolean.class).convention(true);
        this.packFontPages = objects.property(Boolean.class).convention(true);
        this.copyPackedFontPages = objects.property(Boolean.class).convention(true);
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

    public PackerSettings getPacker() {
        return packer;
    }

    public SkinNamesSettings getNames() {
        return names;
    }

    public Property<Boolean> getGenerateLabelStyles() {
        return generateLabelStyles;
    }

    public ListProperty<String> getLabelStyleColors() {
        return labelStyleColors;
    }

    public Property<String> getDefaultLabelColor() {
        return defaultLabelColor;
    }

    public Property<Boolean> getValidateSkins() {
        return validateSkins;
    }

    public Property<Boolean> getPackFontPages() {
        return packFontPages;
    }

    public Property<Boolean> getCopyPackedFontPages() {
        return copyPackedFontPages;
    }
}
