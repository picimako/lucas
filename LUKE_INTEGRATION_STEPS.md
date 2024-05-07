# Integrating the Luke standalone application into an IntelliJ plugin

The aim for the first version was to do only the absolutely necessary changes compared to the original
sources of Luke, so that we have a working version without changing too much and potentially breaking
something.

## Setting up the repository

- Create a repository using the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Copy all project files from Lucene Luke to this new repository under the same package structure.

## Project configuration

NOTE: I would advise to have the plugin as a separate repository, so that the Gradle build and GitHub CI configurations
don't interfere with each other, and so it would require less potential hacking and less work overall.

- Copy all necessary `build.gradle` configuration. Anything that is related to building the standalone .jar file is not necessary.
This is probably all that is needed under `tasks`:
```kotlin
// Process UTF8 property files to unicode escapes.
withType<ProcessResources>().forEach { task ->
    task.filesMatching("**/messages*.properties") {
        task.filteringCharset = "UTF-8"
        filter<EscapeUnicode>()
    }
}
```
- Delete `module-info.java`. This makes it possible to properly use the IntelliJ Platform jars without potential hacking.
The IJ platform has a large amount of .jars released, and in order to access the classes in them, they would have to be added
to `module-info.java`, but I don't think adding all of them to prepare for every single JetBrains IDE for runtime class access is something feasible. 
- Add Lucene dependencies in `build.gradle.kts`: 
```kotlin
dependencies {
    val luceneVersion = "9.10.0"

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
}
```
- Add any other Apache and Lucene project specific configurations, dependencies, documents (like CONTRIBUTING.md).

## Integrating the UI, and opening Luke

- Remove anything that is related to themes in order to let the theme/LAF used by IntelliJ kick in.:
  - configuration from `LukeMain` and from preferences and anywhere where theme preferences were used (menus, dialogs, panel providers).
  - Remove no longer used `LukeWindowOperator`.
- Remove all occurrences of closing the application (`MenuBarProvider`, `System.exit()` calls) because it would close IntelliJ itself.
- `LukeMain`:
  - remove `--sanity-check` CLI argument and headless mode check.
  - Remove explicit usage of `UIManager` and `GraphicsEnvironment`
- Rework `LukeWindowProvider` and `LukeMain`, so that it creates and uses a `JPanel` instead of a `JFrame`
that can, in turn, be displayed inside an editor tab.
- Create a menu action to open Luke:
  - This can be e.g. in the Tools menu with a custom icon for better visuals.
    - NOTE: a 16x16 SVG version of the Lucene icon would be needed. See [Image Formats](https://plugins.jetbrains.com/docs/intellij/work-with-icons-and-images.html#image-formats)
    in the IntelliJ Plugin SDK docs.
  - The menu action would open a new editor tab showing the main panel of Luke (what is created by `LukeWindowProvider`).
  - In the first version of the plugin, this action would allow only one Luke tab to be open at a time, and would re-initialize
  the UI and underlying configuration on each open.
      - When Luke is already open, the action would just focus that tab, instead of opening a new one.
      - Later plugin versions could allow opening multiple Luke tabs, so that people could work with multiple indexes at the same time.
  - In order to achieve clean Luke instances the following changes are needed:
    - Replace the explicit singleton nature of `IndexHandler`, `DirectoryHandler`, `ComponentOperatorRegistry`, `MessageBroker`, `PreferencesImpl` and `TabSwitcherProxy`
    with application-level light services, so that they stay singletons but in a safer manner.
    - Register a `FileEditorManagerListener` that clear all data from the aforementioned light services every time the Luke tab is closed.
      - This makes it possible to properly re-instantiate and re-initialize the underlying configuration upon reopening Luke.
      - Make sure that there is only one `FileEditorManagerListener` registered.
- Migrate `DialogFactory` classes to implement IntelliJ's `DialogWrapper` class which is required for all dialogs used in IntelliJ.
See [DialogWrapper](https://plugins.jetbrains.com/docs/intellij/dialog-wrapper.html#dialogwrapper) docs.
  - Simultaneously, remove the singleton nature of the `DialogFactory` classes.
    - This is required for the `DialogWrapper` implementation because it uses its own, specific initialization and disposal logic,
    so that this would allow to create and initialize each dialog from scratch every time they are opened.
      - Later, this would also allow opening Luke on multiple tabs as multiple instances.
    - Additionally, the dialog specific parts could be extracted from `DialogFactory` classes to `Dialog` classes where feasible,
    e.g. as `CheckIndexDialogFactory implements DialogFactory<CheckIndexDialog> {}`, or the `DialogFactory` classes should be renamed to
    `Dialog`s, and should not be stored as fields in panel providers, rather instantiated at their locations of usages.
      - Either way, the dialogs have to instantiated at their locations of usages to allow proper initialization, and then disposal upon closing.
      - If dialog specific logic is not extracted, make sure that the `Observer` instances in the `DialogFactory` classes are registered only once in `IndexHandler`, etc.
    - Move necessary dialog factory properties from setter methods to constructor parameters, because `createCenterPanel()` is called within `init()`, so they must be initialized in the constructor.
  - Use `DialogWrapper`'s own OK and Cancel button (since they are provided) instead of the ones created by Luke. Rewire their logic as well.
      - Some dialog have only a Cancel or an 'OK' button, and some dialogs don't close upon clicking the 'OK' button. Make sure they have the same logic
      as in Luke standalone.
  - With these changes `DialogOpener` can be removed.
- Replace Swing components, colors and insets to JB specific ones, but only the ones that are absolutely necessary. Some can break the visuals of the panels and dialogs.
  - An IntelliJ inspection automatically helps to locate the types to replace.
- Replace HTML icons with actual SVG icons from the IntelliJ Platform.
  - Some icons don't have exact replacements, for those other icons have to be used.
  - This will make it possible to remove some methods from `FontUtils`.
- Remove custom font and button size configuration, so that the buttons and fonts blend more into the LAF of IntelliJ.
- Delete confirm dialog and replace with the IJ platform specific `Messages` yes-no dialog.
- Use `BoxLayout` for the whole main window panel to fix the layout of the components.
- Make sure that the `org.apache.lucene.luke.util.LoggerFactory` based logging on Luke's Logs tab works properly for log entries
coming from the plugin as well.
  - Here, a separate `com.picimako.org.apache.lucene.luke.util.LucasLoggerFactory` class was implemented. See class-level javadoc for details.

## Other changes
- Disable `assert` statements (at least certain ones) in `CustomAnalyzerPanelProvider` due to this:
  - By default, the Luke standalone application seems to run with `-disableassertions`, and this assertion fails there as well when assertions are enabled.
- The Help menu and the About dialog may also be removed.

## Steps for further versions of the plugin
- UI
  - Use JB specific file openers and directory choosers. They have better visuals, and they would blend more into the LAF.
  - Replace throwing of some `LukeException`s with error popups/notifications for a smoother user experience.
- Configuration and settings
  - Move messages from `messages.properties` to the IntelliJ Platform's own message bundle implementation and storage.
    - Also move hardcoded String literals to the bundle. (This applies to the standalone Luke as well.)
  - Replace the settings ini file with IntelliJ specific settings implementation.
- Others
  - "Copy to clipboard" logic should use IntelliJ's own utils and clipboard solution.
  - For when multiple Luke tabs are allowed:
    - The light service instances might not be a good solution to store `Observer`s and such because data
    would have to be stored separately for each instance of Luke.

## Further improvement ideas
- Add the ability to remove non-existent and no longer needed directory entries from the Open Index dialog's dropdown.
