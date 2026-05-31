package io.github.elebras1.gdxskinweaver.assets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FontPageParser {

    public Set<File> parse(List<File> fonts) {
        Set<File> pages = new HashSet<>();
        for (File fnt : fonts) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fnt))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("page")) {
                        continue;
                    }
                    int fileStart = line.indexOf("file=\"");
                    if (fileStart == -1) {
                        continue;
                    }
                    int start = fileStart + 6;
                    int end = line.indexOf("\"", start);
                    if (end == -1) {
                        continue;
                    }
                    String pageName = line.substring(start, end);
                    File pageFile = new File(fnt.getParent(), pageName);
                    pages.add(pageFile.getAbsoluteFile());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to parse font file: " + fnt, e);
            }
        }
        return pages;
    }
}

