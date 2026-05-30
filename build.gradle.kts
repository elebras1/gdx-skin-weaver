plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("java")
}

group = "io.github.elebras1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.tommyettinger:libgdx-texturepacker:1.14.0.0")
}

gradlePlugin {
    plugins {
        create("gdxskinweaver") {
            id = "io.github.elebras1.gdxskinweaver"
            implementationClass = "io.github.elebras1.gdxskinweaver.GdxSkinWeaver"
        }
    }
}