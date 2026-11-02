# GdxSkinWeaver

Gradle plugin that packs libGDX texture atlases and generates skin JSON from asset folders.

## Features
- Scans asset folders recursively and creates a texture atlas per folder.
- Generates or merges `*_skin.json` files with bitmap fonts, button/toggle styles and label styles.
- Generates a `LabelStyle` for every bitmap font and a `<font>_<color>` style for every color declared in the folder skin.
- Copies `.fnt` files and their referenced pages to the output.
- Supports button (`*_btn`), toggle (`*_tgl`) and nine-patch (`.9.png`) conventions.
- Detects duplicate region names and prefers the nine-patch version of an image.
- Validates hand-written skin JSON and reports missing drawables, fonts, colors and duplicate atlas regions.
- Exposes the texture packer settings (page size, padding, filters, ...).
- Can keep font page images out of the folder atlas.
- Generates an HTML preview report of drawables, styles, fonts and colors.
- Keeps the output directory out of the task inputs so `weaveSkins` can be up-to-date.
- Supports the Gradle build cache for `weaveSkins`.
- Provides a `cleanSkins` task for the generated skins directory.

## Installation
In your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.elebras1.gdx-skin-weaver") version "x.y.z"
}
```

## Configuration
The extension is optional; defaults are `assets/` for input and `assets/skins/` for output.

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    assetsDir.set(layout.projectDirectory.dir("assets"))
    outputDir.set(layout.projectDirectory.dir("assets/skins"))
    excludedDirs.set(listOf("ui/legacy"))
}
```

Run the task:

```bash
./gradlew weaveSkins
```

## Tasks

| Task | Description |
| --- | --- |
| `weaveSkins` | Packs atlases, generates skin JSON, label styles and optional `SkinNames`. |
| `generateSkinReport` | Generates the HTML preview report. |
| `cleanSkins` | Deletes the generated skins directory. |

`weaveSkins` is cacheable and works with the Gradle build cache:

```bash
./gradlew weaveSkins --build-cache
```

`cleanSkins` only deletes the configured `outputDir`. Generated names under `build/` are removed by
the standard Gradle `clean` task.

### Label styles
Label style generation is enabled by default. Colors are read from the `com.badlogic.gdx.graphics.Color`
block of the folder's `*_skin.json`. Existing `LabelStyle` entries are never overwritten.

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    generateLabelStyles.set(true)
    labelStyleColors.set(listOf("red", "green", "blue", "yellow"))
    defaultLabelColor.set("blue")
}
```

For a font named `arial_14` and colors `red`/`blue`, the generated skin contains:

```json
"com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle": {
  "arial_14": { "font": "arial_14" },
  "arial_14_red": { "font": "arial_14", "fontColor": "red" },
  "arial_14_blue": { "font": "arial_14", "fontColor": "blue" }
}
```

This removes the common runtime pattern that builds `LabelStyle` instances by hand from a font skin:

```java
Label label = new Label("Population", skin.get("arial_14_blue", LabelStyle.class));
```

### Packer settings
All packer settings are optional and keep the historical defaults.

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    packer.maxWidth.set(4096)
    packer.maxHeight.set(4096)
    packer.paddingX.set(4)
    packer.paddingY.set(4)
    packer.pot.set(false)
    packer.filterMin.set("Linear")
    packer.filterMag.set("Linear")
}
```

Useful options:

| Option | Default | Description |
| --- | --- | --- |
| `maxWidth`, `maxHeight` | `2048` | Maximum atlas page size. |
| `minWidth`, `minHeight` | `16` | Minimum atlas page size. |
| `paddingX`, `paddingY` | `2` | Space between regions. |
| `pot` | `true` | Force power-of-two page sizes. |
| `edgePadding` | `true` | Repeat edge pixels to avoid bleeding. |
| `filterMin`, `filterMag` | `Nearest` | Atlas filter written to the `.atlas`. |
| `wrapX`, `wrapY` | `ClampToEdge` | Atlas wrap mode. |
| `flattenPaths` | `true` | Strip directories from region names. |
| `useIndexes` | `true` | Turn `frame_0`, `frame_1`, ... into indexed regions. |
| `outputFormat` | `png` | Page format (`png`, `jpg`, ...). |
| `atlasExtension` | `.atlas` | Atlas file extension. |
| `prettyPrint` | `false` | Pretty-print the atlas file. |

The plugin removes the previous atlas and its pages before packing, so changing the packer settings
or removing an asset does not leave stale regions behind.

### Generated name constants
`weaveSkins` also generates a Java constants file from the generated atlases and skin JSON files.

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    names.packageName.set("com.example.ui.generated")
    names.className.set("SkinNames")
    names.outputDir.set(layout.buildDirectory.dir("generated/sources/skinWeaver/java"))
}
```

The generated structure follows the skin folders:

```java
SkinNames.UiSkin.Drawables.BG_PROVINCE
SkinNames.UiSkin.Styles.Button.SETTINGS_LESS_BTN
SkinNames.FontsSkin.Fonts.ARIAL_14
SkinNames.FontsSkin.Colors.RED
```

When the plugin is applied to a project with the `java` plugin, the generated directory is added to
the `main` source set automatically. When it is applied to the root project, wire it from the Java
module:

```groovy
sourceSets.main.java.srcDir(rootProject.layout.buildDirectory.dir("generated/sources/skinWeaver/java"))
compileJava.dependsOn rootProject.tasks.named("weaveSkins")
```

Use `names.enabled.set(false)` to disable generation.

### Font pages
By default, font page images are packed into the folder atlas. When the atlas is not used for
fonts, disable that packing and let the plugin only copy the `.fnt` files and their pages:

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    packFontPages.set(false)
}
```

Missing page files referenced by a `.fnt` are reported as warnings.

### HTML preview
Generate a local report of every generated skin:

```bash
./gradlew generateSkinReport
```

The report is written to `build/reports/skin-weaver/index.html`. It lists drawables with thumbnails,
styles with their drawable previews, fonts and colors. It uses local `file://` image URLs, so open it
on the machine that produced the assets.

The output path can be changed on the task:

```groovy
tasks.named('generateSkinReport') {
    reportFile.set(layout.projectDirectory.file('build/skin-report.html'))
}
```

### Validation
When enabled (default), the plugin checks the merged skin against the generated atlas and prints
warnings for:

- a style that references a missing drawable;
- a style that references a missing bitmap font or color;
- a font that references a missing page file;
- duplicate atlas regions without an animation index;
- a skin with drawable styles but no atlas next to it.

Validation is non-fatal and can be disabled:

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    validateSkins.set(false)
}
```

## Asset Conventions
- Images: `.png`, `.jpg`, `.jpeg`, `.ktx2`.
- Fonts: `.fnt` (page images referenced by the font are copied).
- Existing skin: `*_skin.json` in a folder is merged into the generated skin. Hand-written styles take precedence.
- Buttons: `*_btn` generates `_btn_down` and `_btn_over` variants and a `ButtonStyle` entry.
- Buttons with explicit states: `*_up`, `*_down`, `*_over` generate a `ButtonStyle` named from the prefix.
- Toggles: `*_tgl` generates `_tgl_off` and `_tgl_on` variants and a `ButtonStyle` entry.
- Toggles with explicit states: `*_tgl_off`, `*_tgl_on` generate a `ButtonStyle` entry.
- Nine-patches: `*.9.png` regions expose their split values in the atlas. `Skin#getDrawable` then returns a
  `NinePatchDrawable` automatically.
- Duplicate stems: when both `name.png` and `name.9.png` exist, the nine-patch version wins. Among
  duplicates, any `.9.*` variant wins, then `.png`, `.jpg`/`.jpeg`, then `.ktx2`.
- Animations: numbered frames (`attack_move_0.png`, `attack_move_1.png`, ...) are packed as an indexed
  region (`attack_move`) and can be loaded with `atlas.findRegions("attack_move")`.

## Output
For each scanned folder, the plugin writes:
- `<folder>.atlas` (atlas name matches the folder name, font pages included unless `packFontPages` is disabled).
- `<folder>.json` (generated or merged).
- Fonts and font pages copied next to the atlas.

## License
Apache License. See `LICENSE`.
