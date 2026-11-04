package io.github.elebras1.gdxskinweaver;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GdxSkinWeaverPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void weaveSkinsRefusesOutputDirectoryEqualToAssets() throws IOException {
        assertOutputDirGuard("weaveSkins", "layout.projectDirectory.dir(\"assets\")");
    }

    @Test
    void cleanSkinsRefusesOutputDirectoryEqualToAssets() throws IOException {
        assertOutputDirGuard("cleanSkins", "layout.projectDirectory.dir(\"assets\")");
    }

    @Test
    void cleanSkinsRefusesOutputDirectoryThatContainsAssets() throws IOException {
        assertOutputDirGuard("cleanSkins", "layout.projectDirectory");
    }

    @Test
    void cleanSkinsDeletesTheConfiguredOutputDirectory() throws IOException {
        writeBuild("layout.projectDirectory.dir(\"assets/skins\")");
        Path output = projectDir.resolve("assets/skins");
        Files.createDirectories(output);
        Files.writeString(output.resolve("stale.json"), "{}");

        GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("cleanSkins")
                .withPluginClasspath()
                .build();

        assertFalse(Files.exists(output));
    }

    @Test
    void generatesSkinCatalogWhenEnabled() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'skin-weaver-catalog'\n");
        Path skin = projectDir.resolve("assets/ui/ui_skin.json");
        Files.createDirectories(skin.getParent());
        Files.writeString(skin, """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok_btn": {"up": "ok_btn"}
                  }
                }
                """);
        Files.writeString(projectDir.resolve("build.gradle"), """
                plugins { id 'io.github.elebras1.gdx-skin-weaver' }
                skinWeaver {
                    names.skinCatalog.set(true)
                }
                """);

        GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("weaveSkins")
                .withPluginClasspath()
                .build();

        String source = Files.readString(projectDir.resolve(
                "build/generated/sources/skinWeaver/java/io/github/elebras1/gdxskinweaver/generated/SkinNames.java"));
        assertTrue(source.contains("public static final class Skins"));
        assertTrue(source.contains("public static final String UI = \"skins/ui/ui.json\";"));
    }

    @Test
    void usesConfiguredSkinCatalogPrefixForOutputOutsideAssets() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'skin-weaver-prefix'\n");
        Path skin = projectDir.resolve("assets/ui/ui_skin.json");
        Files.createDirectories(skin.getParent());
        Files.writeString(skin, """
                {
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok_btn": {"up": "ok_btn"}
                  }
                }
                """);
        Files.writeString(projectDir.resolve("build.gradle"), """
                plugins { id 'io.github.elebras1.gdx-skin-weaver' }
                skinWeaver {
                    outputDir.set(layout.buildDirectory.dir("skin-resources/generated-skins"))
                    names.skinCatalog.set(true)
                    names.skinCatalogPrefix.set("generated-skins")
                }
                """);

        GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("weaveSkins")
                .withPluginClasspath()
                .build();

        assertTrue(Files.exists(projectDir.resolve("build/skin-resources/generated-skins/ui/ui.json")));
        String source = Files.readString(projectDir.resolve(
                "build/generated/sources/skinWeaver/java/io/github/elebras1/gdxskinweaver/generated/SkinNames.java"));
        assertTrue(source.contains("public static final String UI = \"generated-skins/ui/ui.json\";"), source);
    }

    private void assertOutputDirGuard(String task, String outputExpression) throws IOException {
        writeBuild(outputExpression);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(task, "--stacktrace")
                .withPluginClasspath()
                .buildAndFail();

        assertTrue(result.getOutput().contains("must not be the assets directory"), result.getOutput());
    }

    private void writeBuild(String outputExpression) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'skin-weaver-guard'\n");
        Files.createDirectories(projectDir.resolve("assets"));
        Files.writeString(projectDir.resolve("build.gradle"), """
                plugins { id 'io.github.elebras1.gdx-skin-weaver' }
                skinWeaver {
                    outputDir.set(%s)
                }
                """.formatted(outputExpression));
    }
}
