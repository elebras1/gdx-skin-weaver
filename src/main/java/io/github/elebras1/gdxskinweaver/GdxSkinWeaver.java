package io.github.elebras1.gdxskinweaver;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GdxSkinWeaver implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register("weaveSkins", GdxSkinWeaverTask.class, task -> {
        });
    }
}
