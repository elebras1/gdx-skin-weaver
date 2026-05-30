import io.github.elebras1.gdxskinweaver.SkinWeaverExtension

plugins {
    id("io.github.elebras1.gdxskinweaver")
}

group = "io.github.elebras1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

extensions.configure<SkinWeaverExtension>("skinWeaver") {
    assetsDir.set(layout.projectDirectory.dir("assets"))
    outputDir.set(layout.projectDirectory.dir("assets/skins"))
    excludedDirs.set(listOf("folder2/folder4"))
}
