package io.github.elebras1.gdxskinweaver.assets;

import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryScanner {

    public DirectorySnapshot scan(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return new DirectorySnapshot(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null);
        }

        List<File> subdirectories = new ArrayList<>();
        List<File> fonts = new ArrayList<>();
        List<File> images = new ArrayList<>();
        File existingSkin = null;

        for (File file : files) {
            if (file.isDirectory()) {
                subdirectories.add(file);
                continue;
            }
            if (AssetFileUtils.isFont(file)) {
                fonts.add(file);
                continue;
            }
            if (AssetFileUtils.isSkin(file)) {
                existingSkin = file;
                continue;
            }
            if (AssetFileUtils.isImage(file)) {
                images.add(file);
            }
        }

        return new DirectorySnapshot(subdirectories, fonts, images, existingSkin);
    }
}

