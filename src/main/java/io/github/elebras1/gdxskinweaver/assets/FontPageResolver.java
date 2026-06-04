package io.github.elebras1.gdxskinweaver.assets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FontPageResolver {
    private static final Pattern PAGE_FILE_PATTERN = Pattern.compile("file=\"([^\"]+)\"");

    public Set<File> resolve(List<File> fonts) {
        Set<File> pages = new LinkedHashSet<>();
        for (File font : fonts) {
            pages.addAll(resolveForFont(font));
        }
        return pages;
    }

    private Set<File> resolveForFont(File font) {
        Set<File> pages = new LinkedHashSet<>();
        if (font == null || !font.exists()) {
            return pages;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(font), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("page") || !line.contains("file=")) {
                    continue;
                }
                Matcher matcher = PAGE_FILE_PATTERN.matcher(line);
                if (!matcher.find()) {
                    continue;
                }
                String fileName = matcher.group(1);
                if (fileName == null || fileName.isEmpty()) {
                    continue;
                }
                File pageFile = resolvePageFile(font, fileName);
                if (pageFile.exists()) {
                    pages.add(pageFile);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read font file " + font.getName() + ": " + e.getMessage());
        }
        return pages;
    }

    private File resolvePageFile(File font, String fileName) {
        Path pagePath = Path.of(fileName);
        if (pagePath.isAbsolute()) {
            return pagePath.toFile();
        }
        return new File(font.getParentFile(), fileName);
    }
}

