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
- Keeps the output directory out of the task inputs so `weaveSkins` can be up-to-date.

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

### Validation
When enabled (default), the plugin checks the merged skin against the generated atlas and prints
warnings for:

- a style that references a missing drawable;
- a style that references a missing bitmap font or color;
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
- `<folder>.atlas` (atlas name matches the folder name, font pages included).
- `<folder>.json` (generated or merged).
- Fonts and font pages copied next to the atlas.

## License
Apache License. See `LICENSE`.
