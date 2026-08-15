// RWAMP Demos — usage examples and scenario demonstrations
// Excluded from Maven publish lifecycle; for reference only.

import org.gradle.api.plugins.JavaPluginExtension

val javaRelease = "25"
val slf4jVersion = "2.0.16"

plugins { `java-library` }

group = "ssg"
version = rootProject.version

configure<JavaPluginExtension> {
    toolchain { languageVersion.set(JavaLanguageVersion.of(javaRelease.toInt())) }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(javaRelease.toInt())
}

// Demos are not published to Maven
afterEvaluate {
    plugins.withId("maven-publish") {
        // Remove the maven-publish plugin to exclude demos from publishing
    }
}

dependencies {
    // RWAMP feature modules (transitively brings in lego-flow-wamp)
    api(project(":rwamp-feature-session"))
    api(project(":rwamp-feature-statistics"))
    api(project(":rwamp-feature-testament"))
    api(project(":rwamp-feature-reflection"))
    api(project(":rwamp-feature-virtual"))
    api(project(":rwamp-feature-rerouting"))
    api(project(":rwamp-rest"))
    api(project(":rwamp-feature-registration"))

    // RWAMP API modules
    api(project(":rwamp-api-providers"))
    api(project(":rwamp-api-publishers"))
    api(project(":rwamp-api-web-services"))

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
}
