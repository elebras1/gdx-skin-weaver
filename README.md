# GdxSkinWeaver

Gradle plugin that packs libGDX texture atlases and generates skin JSON from asset folders.

## Features
- Scans asset folders recursively and creates a texture atlas per folder.
- Generates or merges `*_skin.json` files with bitmap fonts, button/toggle styles and label styles.
- Generates a `LabelStyle` for every bitmap font and a `<font>_<color>` style for every color declared in the folder skin.
- Copies `.fnt` files and their referenced pages to the output.
- Packs multi-page fonts under the region names a libGDX `Skin` looks up (`<font>_0`, `<font>_1`, ...).
- Supports button (`*_btn`), toggle (`*_tgl`) and nine-patch (`.9.png`) conventions.
- Detects duplicate region names and prefers the nine-patch version of an image.
- Validates hand-written skin JSON and reports missing drawables, fonts, colors and duplicate atlas regions.
- Exposes the texture packer settings (page size, padding, filters, ...).
- Can keep font page images out of the folder atlas, or stop copying the page images it packs.
- Generates an HTML preview report of drawables, styles, fonts and colors.
- Can generate a catalog of all skin JSON paths (disabled by default), with a configurable path prefix.
- Writes generated styles in the same order on every run.
- Keeps the output directory out of the task inputs so `weaveSkins` can be up-to-date.
- Excludes the configured `excludedDirs` from the task inputs.
- Supports the Gradle build cache for `weaveSkins`.
- Provides a `cleanSkins` task for the generated skins directory.
- Refuses an output directory that would overwrite or delete the assets directory.

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

Directories listed in `excludedDirs` are removed from the `weaveSkins` inputs, so changing their files does not
invalidate the task. Use `""` or `"."` to exclude the assets root itself: its own files are ignored, while its
subdirectories are still processed.

`weaveSkins` and `cleanSkins` refuse an `outputDir` that is the assets directory or one of its parents. Symbolic
links are resolved before the check. An output directory inside the assets directory, such as the default
`assets/skins`, is allowed.

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
block of the folder's `*_skin.json`. Existing `LabelStyle` entries are never overwritten. A generated
`<font>_<color>` style is skipped with a warning when another font already uses that name.

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
    names.skinCatalog.set(true) // disabled by default
}
```

The generated structure follows the skin folders:

```java
SkinNames.UiSkin.Drawables.BG_PROVINCE
SkinNames.UiSkin.Drawables.LOAD_1
SkinNames.UiSkin.IndexedRegions.LOAD
SkinNames.UiSkin.Styles.Button.SETTINGS_LESS_BTN
SkinNames.FontsSkin.Fonts.ARIAL_14
SkinNames.FontsSkin.Colors.RED
```

A libGDX `Skin` registers an indexed atlas region such as `load` with `index:1` as `load_1`, so
`Drawables` contains the names to pass to `Skin#getDrawable`. `IndexedRegions` keeps the base name
(`load`) for `TextureAtlas#findRegions`.

Packed font pages are listed under `Fonts` only: the regions a `Skin` uses as font textures (`arial_14`,
`chinese_60_0`, `chinese_60_1`, ...) do not appear in `Drawables` or `IndexedRegions`.

When the plugin is applied to a project with the `java` plugin, the generated directory is added to
the `main` source set automatically. When it is applied to the root project, wire it from the Java
module:

```groovy
sourceSets.main.java.srcDir(rootProject.layout.buildDirectory.dir("generated/sources/skinWeaver/java"))
compileJava.dependsOn rootProject.tasks.named("weaveSkins")
```

Use `names.enabled.set(false)` to disable generation.

### Skin catalog
`names.skinCatalog` is disabled by default. When enabled, `SkinNames` also contains a `Skins` class with
the path of every generated skin JSON. Paths are relative to the assets directory when `outputDir` is
inside it, so they can be passed directly to `Gdx.files.internal`; otherwise they stay relative to the
skins output directory.

```java
SkinNames.Skins.FONTS      // "skins/fonts/fonts.json"
SkinNames.Skins.SETTINGS   // "skins/settings/settings.json"
SkinNames.Skins.UI         // "skins/ui/ui.json"
SkinNames.Skins.UI_ECONOMY // "skins/ui/economy/economy.json"
```

Skins without any generated resource are not listed. The catalog only contains plain `String` constants;
there is no generated `ALL` collection.

When the skins are generated outside the assets directory, for example under `build/` and added as a resources
directory, set the prefix the game uses to load them. It replaces the computed prefix; `""` removes it.

```groovy
skinWeaver {
    outputDir.set(layout.buildDirectory.dir('generated/skinWeaver/resources/skins'))
    names.skinCatalog.set(true)
    names.skinCatalogPrefix.set('skins')
}

sourceSets.main.resources.srcDir(layout.buildDirectory.dir('generated/skinWeaver/resources'))
processResources.dependsOn(tasks.named('weaveSkins'))
```

```java
SkinNames.Skins.UI // "skins/ui/ui.json", loaded from the classpath
```

### Font pages
By default, font page images are packed into the folder atlas. When the atlas is not used for
fonts, disable that packing and let the plugin only copy the `.fnt` files and their pages:

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    packFontPages.set(false)
}
```

A `Skin` loads packed pages from the atlas, so the page images copied next to the `.fnt` files are only needed
when a font is loaded without the skin, such as `new BitmapFont(file)`. Their copy can be disabled; copies left
by a previous run are then removed. Pages are always copied when `packFontPages` is disabled.

```kotlin
extensions.configure<io.github.elebras1.gdxskinweaver.SkinWeaverExtension>("skinWeaver") {
    copyPackedFontPages.set(false)
}
```

Packed pages use the names a libGDX `Skin` looks up: a single-page font keeps the font name as its
region, while a multi-page font uses `<font>_<page id>` for every page (`chinese_60_0`, `chinese_60_1`, ...).
Page ids that are not consecutive from `0` are reported, because `Skin#getRegions` stops at the first missing
index.

When `packFontPages` is disabled and a font has the same name as its folder, the atlas page
(`<folder>.png`) would be loaded as the font texture. The plugin reports a warning; rename the font or
the folder, or enable `packFontPages`.

If another image already uses the name of a packed font page region, the font page is kept and the plugin
reports a warning.

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

- a style that references a missing drawable (indexed regions are checked with their `Skin` name, such as `load_1`);
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
- Fonts and font pages copied next to the atlas (pages are skipped when `copyPackedFontPages` is disabled).

When the assets root is excluded with `""` or `"."`, no atlas or skin is generated for the root itself. Atlas
or skin files that an older plugin version left at the output root are not deleted automatically; remove them
once by hand.

## License
Apache License. See `LICENSE`.
