package io.github.elebras1.gdxskinweaver;

import io.github.elebras1.gdxskinweaver.report.SkinReportTask;
import io.github.elebras1.gdxskinweaver.task.GdxSkinWeaverTask;
import io.github.elebras1.gdxskinweaver.util.SkinWeaverPaths;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

public class GdxSkinWeaver implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        SkinWeaverExtension extension = project.getExtensions().create("skinWeaver", SkinWeaverExtension.class);

        TaskProvider<GdxSkinWeaverTask> taskProvider = project.getTasks().register("weaveSkins", GdxSkinWeaverTask.class);
        TaskProvider<SkinReportTask> reportTaskProvider = project.getTasks().register("generateSkinReport", SkinReportTask.class);
        TaskProvider<Delete> cleanTaskProvider = project.getTasks().register("cleanSkins", Delete.class);

        taskProvider.configure(task -> {
            task.setGroup("gdx");
            task.setDescription("Packs libGDX texture atlases and generates skin JSON files.");
        });
        reportTaskProvider.configure(task -> {
            task.setGroup("gdx");
            task.setDescription("Generates an HTML preview of the generated skins.");
        });
        cleanTaskProvider.configure(task -> {
            task.setGroup("gdx");
            task.setDescription("Deletes the generated skins directory.");
        });

        project.afterEvaluate(p -> {
            GdxSkinWeaverTask skinWeaverTask = taskProvider.get();
            skinWeaverTask.getAssetsDir().set(extension.getAssetsDir());
            skinWeaverTask.getOutputDir().set(extension.getOutputDir());
            skinWeaverTask.getExcludedDirs().set(extension.getExcludedDirs());
            skinWeaverTask.getPackerOptions().set(p.provider(() -> extension.getPacker().toOptions()));
            skinWeaverTask.getGenerateLabelStyles().set(extension.getGenerateLabelStyles());
            skinWeaverTask.getLabelStyleColors().set(extension.getLabelStyleColors());
            skinWeaverTask.getDefaultLabelColor().set(extension.getDefaultLabelColor());
            skinWeaverTask.getValidateSkins().set(extension.getValidateSkins());
            skinWeaverTask.getPackFontPages().set(extension.getPackFontPages());
            skinWeaverTask.getGenerateNames().set(extension.getNames().getEnabled());
            skinWeaverTask.getNamesPackage().set(extension.getNames().getPackageName());
            skinWeaverTask.getNamesClassName().set(extension.getNames().getClassName());
            skinWeaverTask.getSkinCatalog().set(extension.getNames().getSkinCatalog());

            if (!extension.getNames().getOutputDir().isPresent()) {
                extension.getNames().getOutputDir().set(p.getLayout().getBuildDirectory().dir("generated/sources/skinWeaver/java"));
            }
            skinWeaverTask.getNamesOutputDir().set(extension.getNames().getOutputDir());

            if (!skinWeaverTask.getAssetsDir().isPresent()) {
                skinWeaverTask.getAssetsDir().set(p.getLayout().getProjectDirectory().dir("assets"));
            }
            if (!skinWeaverTask.getOutputDir().isPresent()) {
                skinWeaverTask.getOutputDir().set(skinWeaverTask.getAssetsDir().get().dir("skins"));
            }

            SkinReportTask reportTask = reportTaskProvider.get();
            reportTask.getSkinsDir().set(skinWeaverTask.getOutputDir());
            reportTask.getAtlasExtension().set(extension.getPacker().getAtlasExtension());
            reportTask.dependsOn(taskProvider);

            File assetsDir = skinWeaverTask.getAssetsDir().get().getAsFile();
            File outputDir = skinWeaverTask.getOutputDir().get().getAsFile();
            Delete cleanTask = cleanTaskProvider.get();
            cleanTask.delete(outputDir);
            cleanTask.doFirst(task -> SkinWeaverPaths.requireSafeOutputDir(assetsDir, outputDir));

            if (extension.getNames().getEnabled().get() && p.getPluginManager().hasPlugin("java")) {
                SourceSetContainer sourceSets = p.getExtensions().findByType(SourceSetContainer.class);
                if (sourceSets != null) {
                    SourceSet main = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    if (main != null) {
                        main.getJava().srcDir(skinWeaverTask.getNamesOutputDir().get().getAsFile());
                        p.getTasks().named("compileJava").configure(task -> task.dependsOn(taskProvider));
                    }
                }
            }
        });
    }
}
