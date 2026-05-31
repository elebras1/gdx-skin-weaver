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
    implementation("com.eclipsesource.minimal-json:minimal-json:0.9.5")
}

gradlePlugin {
    plugins {
        create("gdx-skin-weaver") {
            id = "io.github.elebras1.gdx-skin-weaver"
            implementationClass = "io.github.elebras1.gdxskinweaver.GdxSkinWeaver"
        }
    }
}