package io.github.elebras1.gdxskinweaver.service;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class TexturePackerService {

    public void pack(File sourceDir, File targetDir, String atlasName, List<File> images) {
        TexturePacker.Settings settings = createSettings();
        TexturePacker packer = new TexturePacker(sourceDir, settings);
        for (File img : images) {
            packer.addImage(img);
        }
        packer.pack(targetDir, atlasName);
    }

    private TexturePacker.Settings createSettings() {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.pot = true;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.edgePadding = true;
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.flattenPaths = true;
        settings.combineSubdirectories = false;
        settings.legacyOutput = false;
        return settings;
    }
}

