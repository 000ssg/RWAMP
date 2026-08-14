import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

group = "ssg"
version = "0.1.0-SNAPSHOT"

val javaRelease = "25"
val junitVersion = "5.11.4"
val junitPlatformVersion = "1.11.4"
val assertjVersion = "3.27.3"
val slf4jVersion = "2.0.16"
val legoFlowVersion = "0.2.0-SNAPSHOT"
val jacocoAgentVersion = "0.8.14"

// Modules excluded from JaCoCo coverage and Maven publishing (e.g., demos, examples)
val nonLibraryModules = setOf("rwamp-demos")

repositories {
    mavenLocal()
    mavenCentral()
    
    maven {
        name = "lego-flow-Packages"
        url = uri("https://maven.pkg.github.com/000ssg/lego-flow")
        content {
            includeGroup("ssg")
        }
        credentials {
            username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String?
                    ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

subprojects {
    apply(plugin = "java-library")

    group = "ssg"
    version = rootProject.version

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaRelease.toInt()))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaRelease.toInt())
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        maxParallelForks = 1
        dependsOn(tasks.named("jar"))
        jvmArgs("-XX:+UseG1GC")
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "lego-flow-Packages"
            url = uri("https://maven.pkg.github.com/000ssg/lego-flow")
            content {
                includeGroup("ssg")
            }
            credentials {
                username = project.findProperty("gpr.user") as String?
                        ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String?
                        ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    dependencies {
        "implementation"("ssg:lego-flow-wamp:$legoFlowVersion")
        "implementation"("org.slf4j:slf4j-api:$slf4jVersion")
        "testImplementation"("org.slf4j:slf4j-simple:$slf4jVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
        "testImplementation"("org.assertj:assertj-core:$assertjVersion")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    }
}

// ── JaCoCo Agent Configuration ─────────────────────────────────────
// Gradle 9.x removed the built-in jacoco plugin.
// Adds -javaagent to all test tasks so .exec files are generated during testing.

val jacocoAgent = configurations.create("jacocoAgent") {
    isTransitive = false
}
dependencies.add(jacocoAgent.name, "org.jacoco:org.jacoco.agent:$jacocoAgentVersion:runtime")

subprojects.filter { !nonLibraryModules.contains(it.name) }.forEach { subproject ->
    if (subproject.plugins.hasPlugin("java-library") || subproject.plugins.hasPlugin("java")) {
        subproject.tasks.withType<Test> {
            val agentJar = jacocoAgent.files.firstOrNull()
            if (agentJar != null) {
                val destFile = subproject.layout.buildDirectory.get().asFile
                    .resolve("jacoco/test.exec")
                jvmArgs("-javaagent:${agentJar.absolutePath}=includes=ssg.rwamp.**,output=file,destfile=${destFile}")
            }
        }
    }
}

// ── JaCoCo Aggregate Report ──────────────────────────────────────
// Produces HTML + XML reports from all subprojects' .exec files.
// Run: ./gradlew clean test jacocoAggregateReport --no-daemon

tasks.register("jacocoAggregateReport", JacocoAggregateReportTask::class) {
    val execDirPaths = subprojects
        .filter { !nonLibraryModules.contains(it.name) && (it.plugins.hasPlugin("java-library") || it.plugins.hasPlugin("java")) }
        .map { it.layout.buildDirectory.get().asFile.resolve("jacoco").absolutePath }
    
    setExecDirPaths(execDirPaths.toList())
    outputDir = layout.buildDirectory.dir("jacoco/aggregate").get().asFile
    
    outputs.upToDateWhen { false }
    
    subprojects.filter { !nonLibraryModules.contains(it.name) }.forEach { subproject ->
        if (subproject.plugins.hasPlugin("java-library") || subproject.plugins.hasPlugin("java")) {
            dependsOn(subproject.tasks.named("test"))
        }
    }
}

// ── JaCoCo Coverage Verification ──────────────────────────────────────
// Per-subproject: 50% minimum. Aggregate: 80% minimum.
// Run: ./gradlew jacocoTestCoverageVerification --no-daemon

subprojects.filter { !nonLibraryModules.contains(it.name) }.forEach { subproject ->
    if (subproject.plugins.hasPlugin("java-library") || subproject.plugins.hasPlugin("java")) {
        subproject.tasks.register("jacocoTestCoverageVerification", JacocoCoverageVerificationTask::class.java) {
            execFile = subproject.layout.buildDirectory.get().asFile.resolve("jacoco/test.exec")
            classesDir = subproject.layout.buildDirectory.get().asFile.resolve("classes/java/main")
            minCoverage = 0.50
            dependsOn(subproject.tasks.named("test"))
        }
    }
}

// Aggregate coverage verification across all modules
tasks.register("jacocoAggregateVerification", JacocoAggregateVerificationTask::class) {
    val execDirPaths = subprojects
        .filter { !nonLibraryModules.contains(it.name) && (it.plugins.hasPlugin("java-library") || it.plugins.hasPlugin("java")) }
        .map { it.layout.buildDirectory.get().asFile.resolve("jacoco").absolutePath }
    val classDirPaths = subprojects
        .filter { !nonLibraryModules.contains(it.name) && (it.plugins.hasPlugin("java-library") || it.plugins.hasPlugin("java")) }
        .map { it.layout.buildDirectory.get().asFile.resolve("classes/java/main").absolutePath }
    
    setExecDirPaths(execDirPaths.toList())
    setClassDirPaths(classDirPaths.toList())
    minCoverage = 0.80
    
    subprojects.filter { !nonLibraryModules.contains(it.name) }.forEach { subproject ->
        if (subproject.plugins.hasPlugin("java-library") || subproject.plugins.hasPlugin("java")) {
            dependsOn(subproject.tasks.named("test"))
        }
    }
}

tasks.register("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Verify JaCoCo coverage for all subprojects"
    subprojects.filter { !nonLibraryModules.contains(it.name) }.forEach { subproject ->
        if (subproject.plugins.hasPlugin("java-library") || subproject.plugins.hasPlugin("java")) {
            dependsOn(subproject.tasks.named("jacocoTestCoverageVerification"))
        }
    }
    dependsOn(tasks.named("jacocoAggregateVerification"))
}

// ── Maven Publish ──
subprojects.filter { !nonLibraryModules.contains(it.name) }.forEach { subproject ->
    subproject.plugins.apply("maven-publish")
    
    subproject.configure<PublishingExtension> {
        publications.create("maven", MavenPublication::class.java) {
            from(subproject.components.getByName("java"))
            
            groupId = subproject.group.toString()
            artifactId = subproject.name
            version = subproject.version.toString()
            
            pom {
                name.set(subproject.name)
                description.set("RWAMP ${subproject.name} module")
                url.set("https://github.com/000ssg/RWAMP")
                
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("000ssg")
                        name.set("Sergey Sidorov")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:000ssg/RWAMP.git")
                    developerConnection.set("scm:git:git@github.com:000ssg/RWAMP.git")
                    url.set("https://github.com/000ssg/RWAMP")
                }
            }
        }
        
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/000ssg/RWAMP")
                credentials {
                    username = project.findProperty("gpr.user") as String?
                            ?: System.getenv("GITHUB_ACTOR") ?: "000ssg"
                    password = project.findProperty("gpr.key") as String?
                            ?: System.getenv("PACKAGE_PAT") ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
