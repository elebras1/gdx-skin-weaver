package io.github.elebras1.gdxskinweaver.util;

import org.gradle.api.InvalidUserDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinWeaverPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void normalizesExcludedDirectories() {
        Set<String> normalized = SkinWeaverPaths.normalizeExcludedDirs(
                List.of("./ui/", "common", "fonts\\legacy", "ui", ""));

        assertEquals(List.of("ui", "common", "fonts/legacy", ""), List.copyOf(normalized));
    }

    @Test
    void treatsDotAndEmptyAsTheAssetsRoot() {
        assertEquals(Set.of(""), SkinWeaverPaths.normalizeExcludedDirs(List.of("", ".", "./")));
    }

    @Test
    void refusesOutputDirectoryEqualToAssets() throws Exception {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> SkinWeaverPaths.requireSafeOutputDir(assets.toFile(), assets.toFile()));

        assertTrue(exception.getMessage().contains("must not be the assets directory"));
    }

    @Test
    void refusesOutputDirectoryThatContainsAssets() throws Exception {
        Path assets = Files.createDirectories(tempDir.resolve("project/assets"));

        assertThrows(InvalidUserDataException.class,
                () -> SkinWeaverPaths.requireSafeOutputDir(assets.toFile(), tempDir.resolve("project").toFile()));
    }

    @Test
    void acceptsOutputDirectoryInsideAssetsOrNextToIt() throws Exception {
        Path assets = Files.createDirectories(tempDir.resolve("assets"));

        assertDoesNotThrow(() -> SkinWeaverPaths.requireSafeOutputDir(assets.toFile(), assets.resolve("skins").toFile()));
        assertDoesNotThrow(() -> SkinWeaverPaths.requireSafeOutputDir(assets.toFile(), tempDir.resolve("generated").toFile()));
    }
}
