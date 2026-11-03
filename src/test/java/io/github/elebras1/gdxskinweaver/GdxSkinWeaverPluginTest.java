package io.github.elebras1.gdxskinweaver;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GdxSkinWeaverPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void weaveSkinsRefusesOutputDirectoryEqualToAssets() throws IOException {
        assertOutputDirGuard("weaveSkins");
    }

    @Test
    void cleanSkinsRefusesOutputDirectoryEqualToAssets() throws IOException {
        assertOutputDirGuard("cleanSkins");
    }

    private void assertOutputDirGuard(String task) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'skin-weaver-guard'\n");
        Files.createDirectories(projectDir.resolve("assets"));
        Files.writeString(projectDir.resolve("build.gradle"), """
                plugins { id 'io.github.elebras1.gdx-skin-weaver' }
                skinWeaver {
                    outputDir.set(layout.projectDirectory.dir("assets"))
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(task, "--stacktrace")
                .withPluginClasspath()
                .buildAndFail();

        assertTrue(result.getOutput().contains("must not be the assets directory"), result.getOutput());
    }
}
