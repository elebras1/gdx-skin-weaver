# GdxSkinWeaver

Gradle plugin that packs libGDX texture atlases and generates skin JSON from asset folders.

## Features
- Scans asset folders recursively and creates a texture atlas per folder.
- Generates or merges `*_skin.json` files with bitmap fonts and basic button/toggle styles.
- Copies `.fnt` files and their referenced pages to the output.
- Supports button (`*_btn`) and toggle (`*_tgl`) image conventions.

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

## Asset Conventions
- Images: `.png`, `.jpg`, `.jpeg`, `.ktx2`.
- Fonts: `.fnt` (page images referenced by the font are copied).
- Existing skin: `*_skin.json` in a folder is merged into the generated skin.
- Buttons: `*_btn` generates `_btn_down` and `_btn_over` variants and a `ButtonStyle` entry.
- Toggles: `*_tgl` generates `_tgl_off` and `_tgl_on` variants and a `ButtonStyle` entry.

## Output
For each scanned folder, the plugin writes:
- `<folder>.atlas` (atlas name matches the folder name, font pages excluded).
- `<folder>_skin.json` (generated or merged).
- Fonts and font pages copied next to the atlas.

## License
Apache License. See `LICENSE`.
