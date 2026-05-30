package io.github.elebras1.gdxskinweaver;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public class GdxSkinWeaver implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        SkinWeaverExtension extension = project.getExtensions().create("skinWeaver", SkinWeaverExtension.class);

        TaskProvider<GdxSkinWeaverTask> taskProvider = project.getTasks().register("weaveSkins", GdxSkinWeaverTask.class);

        project.afterEvaluate(p -> {
            GdxSkinWeaverTask skinWeaverTask = taskProvider.get();
            skinWeaverTask.getAssetsDir().set(extension.getAssetsDir());
            skinWeaverTask.getOutputDir().set(extension.getOutputDir());
            skinWeaverTask.getExcludedDirs().set(extension.getExcludedDirs());

            if (!skinWeaverTask.getAssetsDir().isPresent()) {
                skinWeaverTask.getAssetsDir().set(p.getLayout().getProjectDirectory().dir("assets"));
            }
            if (!skinWeaverTask.getOutputDir().isPresent()) {
                skinWeaverTask.getOutputDir().set(skinWeaverTask.getAssetsDir().get().dir("skins"));
            }
        });
    }
}