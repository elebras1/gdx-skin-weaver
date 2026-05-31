plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "io.github.elebras1"
version = System.getenv("GITHUB_REF_NAME")?.removePrefix("v") ?: project.findProperty("version") as String? ?: "0.1-SNAPSHOT"

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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group as String?
            artifactId = "-java"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("GdxSkinWeaver")
                description.set("Gradle plugin that packs libGDX texture atlases and generates skin JSON from asset folders")
                url.set("https://github.com/elebras1/gdx-skin-weaver")

                withXml {
                    val node = asNode()
                    val dependenciesNodes = node.get("dependencies") as groovy.util.NodeList
                    if (dependenciesNodes.isNotEmpty()) {
                        node.remove(dependenciesNodes[0] as groovy.util.Node)
                    }
                }

                licenses {
                    license {
                        name.set("Apache License")
                        url.set("https://github.com/elebras1/gdx-skin-weaver/blob/main/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("elebras1")
                        name.set("elebras1")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/elebras1/gdx-skin-weaver.git")
                    developerConnection.set("scm:git:ssh://github.com/elebras1/gdx-skin-weaver.git")
                    url.set("https://github.com/elebras1/gdx-skin-weaver")
                }
            }
        }
    }
}

configure<SigningExtension> {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}