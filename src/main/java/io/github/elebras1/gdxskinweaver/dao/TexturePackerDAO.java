package io.github.elebras1.gdxskinweaver.dao;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class TexturePackerDAO {

    public void pack(File sourceDir, File outputRoot, Path assetsRoot, List<File> images) {
        Path sourcePath = sourceDir.toPath();
        String relative = assetsRoot.relativize(sourcePath).toString();
        File targetDir = outputRoot.toPath().resolve(relative).toFile();
        String atlasName = sourceDir.getName();
        if (atlasName.isEmpty()) {
            atlasName = "pack";
        }

        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.pot = true;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.edgePadding = true;
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.combineSubdirectories = false; //todo reflechir a l'ajout d'une option pour l'utilisateur

        TexturePacker packer = new TexturePacker(sourceDir, settings);
        for (File img : images) {
            packer.addImage(img);
        }
        packer.pack(targetDir, atlasName);
    }
}
