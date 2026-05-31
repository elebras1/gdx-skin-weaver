package io.github.elebras1.gdxskinweaver.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

public record WeaveContext(Path assetsRoot, File outputDir, Set<String> excludedDirs, File temporaryDir) {
}
