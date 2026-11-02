package io.github.elebras1.gdxskinweaver.report;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class SkinReportTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getSkinsDir();

    @Input
    public abstract Property<String> getAtlasExtension();

    @OutputFile
    public abstract RegularFileProperty getReportFile();

    public SkinReportTask() {
        getAtlasExtension().convention(".atlas");
        getReportFile().convention(getProject().getLayout().getBuildDirectory().file("reports/skin-weaver/index.html"));
    }

    @TaskAction
    public void generate() {
        new SkinReportGenerator().generate(getSkinsDir().get().getAsFile(), getReportFile().get().getAsFile(),
                getAtlasExtension().get());
    }
}
