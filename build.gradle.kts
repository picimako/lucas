import org.apache.tools.ant.filters.*
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    val luceneVersion = "9.11.1"

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

    //In case one wants to open in index using AssertingCodec in that index,
    // add implementation("org.apache.lucene:lucene-test-framework:$luceneVersion")
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with (it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Process UTF8 property files to unicode escapes.
    withType<ProcessResources>().forEach { task ->
        task.filesMatching("**/messages*.properties") {
            task.filteringCharset = "UTF-8"
            filter<EscapeUnicode>()
        }
    }
}
