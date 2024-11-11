import org.apache.tools.ant.filters.*
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies
repositories {
    mavenCentral()
    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    //Lucene

    //In case one wants to open in index using AssertingCodec in that index,
    // add implementation("org.apache.lucene:lucene-test-framework:$luceneVersion")

    val luceneVersion = "9.12.0"

    implementation("org.apache.lucene:lucene-core:$luceneVersion")

    implementation("org.apache.lucene:lucene-codecs:$luceneVersion")
    implementation("org.apache.lucene:lucene-backward-codecs:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-queries:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-misc:$luceneVersion")
    //This is so that files in Luke that don't have to be changed are not stored in this repository.
    implementation("org.apache.lucene:lucene-luke:$luceneVersion")

    runtimeOnly("org.apache.lucene:lucene-highlighter:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-icu:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-kuromoji:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-morfologik:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-nori:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-opennlp:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-phonetic:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-smartcn:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-analysis-stempel:$luceneVersion")
    runtimeOnly("org.apache.lucene:lucene-suggest:$luceneVersion")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html

    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

//Required for classes in 'org.apache.lucene.store' because they use JDK 21 Preview features.
// Without this option, finding those classes would fail during the IDE's run.
tasks.named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("--enable-preview", "--add-modules jdk.incubator.vector")
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    //Required for classes in 'org.apache.lucene.store' because they use JDK 21 Preview features.
    // Without this option, the build would fail.
    compileJava {
        //Since Lucene 9.11.x (and probably earlier) with JDK 21 / Lucas 0.4.0
        options.compilerArgs.add("--enable-preview")
        //Since Lucene 9.12.0 / Lucas 0.5.0
        options.compilerArgs.add("--add-modules")
        options.compilerArgs.add("jdk.incubator.vector")
    }

    // Process UTF8 property files to unicode escapes.
    withType<ProcessResources>().forEach { task ->
        task.filesMatching("**/messages*.properties") {
            task.filteringCharset = "UTF-8"
            filter<EscapeUnicode>()
        }
    }
}
