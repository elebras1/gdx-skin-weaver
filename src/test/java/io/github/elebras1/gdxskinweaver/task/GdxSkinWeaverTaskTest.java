package io.github.elebras1.gdxskinweaver.task;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GdxSkinWeaverTaskTest {

    @TempDir
    Path tempDir;

    @Test
    void removesExcludedDirectoriesFromInputFiles() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        write(assets.resolve("root.png"));
        write(assets.resolve("ui/button.png"));
        write(assets.resolve("legacy/old.png"));
        GdxSkinWeaverTask task = createTask(assets, tempDir.resolve("out"));

        task.getExcludedDirs().set(Set.of("./legacy/"));

        assertEquals(Set.of("root.png", "ui/button.png"), inputFiles(task, assets));
    }

    @Test
    void removesNestedExcludedDirectoriesFromInputFiles() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        write(assets.resolve("ui/button.png"));
        write(assets.resolve("ui/legacy/old.png"));
        GdxSkinWeaverTask task = createTask(assets, tempDir.resolve("out"));

        task.getExcludedDirs().set(Set.of("ui/legacy"));

        assertEquals(Set.of("ui/button.png"), inputFiles(task, assets));
    }

    @Test
    void removesOutputDirectoryFromInputFiles() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        write(assets.resolve("ui/button.png"));
        write(assets.resolve("generated/ui.atlas"));
        GdxSkinWeaverTask task = createTask(assets, assets.resolve("generated"));

        assertEquals(Set.of("ui/button.png"), inputFiles(task, assets));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "."})
    void removesRootFilesButKeepsSubdirectoriesWhenRootIsExcluded(String excludedDir) throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        write(assets.resolve("root.png"));
        write(assets.resolve("ui/button.png"));
        GdxSkinWeaverTask task = createTask(assets, tempDir.resolve("out"));

        task.getExcludedDirs().set(Set.of(excludedDir));

        assertEquals(Set.of("ui/button.png"), inputFiles(task, assets));
    }

    @Test
    void weaveRejectsOutputDirectoryEqualToAssets() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        GdxSkinWeaverTask task = createTask(assets, assets);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class, task::weave);

        assertTrue(exception.getMessage().contains("must not be the assets directory"));
    }

    @Test
    void skinCatalogIsDisabledByDefault() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));
        GdxSkinWeaverTask task = createTask(assets, tempDir.resolve("out"));

        assertFalse(task.getSkinCatalog().get());
    }

    @Test
    void weaveRejectsOutputDirectoryThatContainsAssets() throws IOException {
        Path assets = Files.createDirectories(tempDir.resolve("project/assets"));
        GdxSkinWeaverTask task = createTask(assets, tempDir.resolve("project"));

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class, task::weave);

        assertTrue(exception.getMessage().contains("must not be the assets directory"));
    }

    private GdxSkinWeaverTask createTask(Path assets, Path output) {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        GdxSkinWeaverTask task = project.getTasks().register("weaveSkins", GdxSkinWeaverTask.class).get();
        task.getAssetsDir().set(assets.toFile());
        task.getOutputDir().set(output.toFile());
        return task;
    }

    private Set<String> inputFiles(GdxSkinWeaverTask task, Path assets) {
        return task.getAssetFiles().getFiles().stream()
                .map(file -> assets.relativize(file.toPath()).toString().replace('\\', '/'))
                .collect(Collectors.toSet());
    }

    private void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "");
    }
}
