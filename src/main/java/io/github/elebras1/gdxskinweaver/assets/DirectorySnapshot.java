package io.github.elebras1.gdxskinweaver.assets;

import java.io.File;
import java.util.Collections;
import java.util.List;

public record DirectorySnapshot(List<File> subdirectories, List<File> fonts, List<File> fontPages,
                                List<FontPageResolver.FontPage> fontPageEntries, List<File> images, File existingSkin) {

    public DirectorySnapshot(List<File> subdirectories, List<File> fonts, List<File> fontPages,
                             List<FontPageResolver.FontPage> fontPageEntries, List<File> images, File existingSkin) {
        this.subdirectories = wrap(subdirectories);
        this.fonts = wrap(fonts);
        this.fontPages = wrap(fontPages);
        this.fontPageEntries = wrap(fontPageEntries);
        this.images = wrap(images);
        this.existingSkin = existingSkin;
    }

    public DirectorySnapshot(List<File> subdirectories, List<File> fonts, List<File> fontPages, List<File> images, File existingSkin) {
        this(subdirectories, fonts, fontPages, List.of(), images, existingSkin);
    }

    private static <T> List<T> wrap(List<T> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(input);
    }
}
