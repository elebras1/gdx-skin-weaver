package io.github.elebras1.gdxskinweaver.assets;

import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FontPageResolver {
    private static final Pattern PAGE_FILE_PATTERN = Pattern.compile("file=\"([^\"]+)\"");
    private static final Pattern PAGE_ID_PATTERN = Pattern.compile("\\bid=(\\d+)");

    public Set<File> resolve(List<File> fonts) {
        Set<File> pages = new LinkedHashSet<>();
        for (FontPage page : resolvePages(fonts)) {
            pages.add(page.file());
        }
        return pages;
    }

    /**
     * Resolves the existing pages of each font, ordered by page id. Missing pages are reported once.
     */
    public List<FontPage> resolvePages(List<File> fonts) {
        List<FontPage> pages = new ArrayList<>();
        for (File font : fonts) {
            pages.addAll(resolveForFont(font));
        }
        return List.copyOf(pages);
    }

    private List<FontPage> resolveForFont(File font) {
        List<FontPage> pages = new ArrayList<>();
        if (font == null || !font.exists()) {
            return pages;
        }

        int pageCount = 0;
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
                Matcher idMatcher = PAGE_ID_PATTERN.matcher(line);
                int id = idMatcher.find() ? Integer.parseInt(idMatcher.group(1)) : pageCount;
                pageCount++;

                File pageFile = resolvePageFile(font, fileName);
                if (pageFile.exists()) {
                    pages.add(new FontPage(font, id, pageFile, false));
                } else {
                    warn("Missing font page '" + fileName + "' referenced by " + font.getName() + ".");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read font file " + font.getName() + ": " + e.getMessage());
        }

        boolean multiPage = pageCount > 1;
        pages.sort(Comparator.comparingInt(FontPage::id));
        pages.replaceAll(page -> new FontPage(page.font(), page.id(), page.file(), multiPage));
        return pages;
    }

    private File resolvePageFile(File font, String fileName) {
        Path pagePath = Path.of(fileName);
        if (pagePath.isAbsolute()) {
            return pagePath.toFile();
        }
        return new File(font.getParentFile(), fileName);
    }

    private void warn(String message) {
        System.err.println("[gdx-skin-weaver] " + message);
    }

    /**
     * A page image of a bitmap font.
     *
     * @param multiPage whether the font declares more than one page
     */
    public record FontPage(File font, int id, File file, boolean multiPage) {

        /**
         * Region name a libGDX {@code Skin} looks up for this page: the font name for a single-page font and
         * {@code <font>_<page id>} for a multi-page font (read back with {@code Skin#getRegions}).
         */
        public String regionName() {
            String fontName = AssetFileUtils.getBaseName(font);
            return multiPage ? fontName + "_" + id : fontName;
        }
    }
}
