package io.github.elebras1.gdxskinweaver.report;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import io.github.elebras1.gdxskinweaver.atlas.AtlasRegionReader;
import io.github.elebras1.gdxskinweaver.atlas.AtlasRegionReader.AtlasLayout;
import io.github.elebras1.gdxskinweaver.atlas.AtlasRegionReader.AtlasRegionData;
import io.github.elebras1.gdxskinweaver.atlas.AtlasRegionReader.PageData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public class SkinReportGenerator {

    private static final String BITMAP_FONT_KEY = "com.badlogic.gdx.graphics.g2d.BitmapFont";
    private static final String COLOR_KEY = "com.badlogic.gdx.graphics.Color";

    private static final Set<String> DRAWABLE_FIELDS = Set.of(
            "up", "down", "over", "checked", "checkedOver", "disabled",
            "background", "stageBackground", "knob", "knobBefore", "knobAfter",
            "hScroll", "hScrollKnob", "vScroll", "vScrollKnob", "corner",
            "imageUp", "imageDown", "imageOver", "imageChecked", "imageCheckedOver", "imageDisabled",
            "checkboxOn", "checkboxOff", "selection", "cursor", "scroll", "scrollKnob",
            "line", "textCursor", "textSelection", "handle", "plus", "minus", "plusOver", "minusOver",
            "icon"
    );

    private static final String CSS = """
            :root { color-scheme: dark; }
            * { box-sizing: border-box; }
            body { margin: 24px; background: #1b1b1f; color: #d8d8dc; font-family: system-ui, sans-serif; }
            h1, h2, h3, h4 { color: #ffffff; margin: 24px 0 12px; }
            h1 { margin-top: 0; }
            #filter { width: 100%; max-width: 480px; margin: 0 0 16px; padding: 10px 12px; border-radius: 6px; border: 1px solid #3a3a42; background: #26262c; color: #ffffff; }
            a { color: #7cc4ff; text-decoration: none; }
            a:hover { text-decoration: underline; }
            nav { display: flex; flex-wrap: wrap; gap: 8px 16px; padding: 12px 0 20px; border-bottom: 1px solid #3a3a42; }
            section { padding-top: 8px; margin-bottom: 40px; border-bottom: 1px solid #2c2c33; }
            .muted { color: #92929c; }
            .grid { display: flex; flex-wrap: wrap; gap: 10px; }
            .card { background: #26262c; border: 1px solid #3a3a42; border-radius: 6px; padding: 8px; min-width: 96px; text-align: center; }
            .thumb { background-repeat: no-repeat; image-rendering: pixelated; margin: 0 auto 6px; }
            .name { font-size: 11px; overflow-wrap: anywhere; }
            .style { background: #26262c; border: 1px solid #3a3a42; border-radius: 6px; padding: 10px; margin-bottom: 8px; }
            .style-name { font-weight: 700; color: #ffffff; margin-bottom: 8px; }
            .props { display: grid; grid-template-columns: max-content max-content max-content; gap: 6px 12px; align-items: center; }
            .prop { color: #9cdcfe; font-size: 12px; }
            code { color: #d7ba7d; font-size: 12px; overflow-wrap: anywhere; }
            .swatch { width: 48px; height: 24px; border-radius: 4px; border: 1px solid #55555f; margin: 0 auto 6px; }
            """;

    private final AtlasRegionReader atlasRegionReader = new AtlasRegionReader();

    public void generate(File skinsDir, File reportFile, String atlasExtension) {
        List<SkinData> skins = collect(skinsDir.toPath(), normalizeAtlasExtension(atlasExtension));
        String html = render(skins);

        try {
            Files.createDirectories(reportFile.toPath().getParent());
            Files.writeString(reportFile.toPath(), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write skin report", e);
        }
    }

    private List<SkinData> collect(Path skinsDir, String atlasExtension) {
        if (!Files.isDirectory(skinsDir)) {
            return List.of();
        }

        List<Path> skinFiles;
        try (Stream<Path> stream = Files.walk(skinsDir)) {
            skinFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan skins directory", e);
        }

        List<SkinData> skins = new ArrayList<>();
        int index = 1;
        for (Path skinFile : skinFiles) {
            JsonObject skin = readSkin(skinFile);
            if (skin == null) {
                continue;
            }

            String relativePath = skinsDir.relativize(skinFile).toString().replace('\\', '/');
            SkinData data = new SkinData("skin-" + index++, relativePath, skin);
            readAtlas(skinFile, atlasExtension, data);
            skins.add(data);
        }
        return skins;
    }

    private JsonObject readSkin(Path skinFile) {
        try (Reader reader = Files.newBufferedReader(skinFile, StandardCharsets.UTF_8)) {
            JsonValue value = Json.parse(reader);
            if (!value.isObject()) {
                return null;
            }
            return value.asObject();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read skin " + skinFile, e);
        }
    }

    private void readAtlas(Path skinFile, String atlasExtension, SkinData data) {
        Path atlasFile = skinFile.resolveSibling(fileStem(skinFile) + atlasExtension);
        if (!Files.isRegularFile(atlasFile)) {
            return;
        }

        try {
            AtlasLayout layout = atlasRegionReader.readLayout(atlasFile.toFile());
            data.atlasFile = atlasFile;
            data.layout = layout;
            for (PageData page : layout.pages()) {
                data.pages.put(page.name(), page);
                data.pageUrls.put(page.name(), atlasFile.getParent().resolve(page.name()).toUri().toString());
            }
            for (AtlasRegionData region : layout.regions()) {
                data.regions.putIfAbsent(region.skinName(), region);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read atlas " + atlasFile, e);
        }
    }

    private String render(List<SkinData> skins) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
        html.append("<title>Skin Weaver report</title>\n<style>\n").append(CSS).append("</style>\n</head>\n<body>\n");
        html.append("<h1>Skin Weaver report</h1>\n");
        html.append("<input id=\"filter\" type=\"search\" placeholder=\"Filter skins, drawables, styles, fonts, colors\">\n");
        html.append("<nav>\n");
        for (SkinData skin : skins) {
            html.append("<a href=\"#").append(skin.id).append("\">").append(escape(skin.relativePath)).append("</a>\n");
        }
        html.append("</nav>\n");

        for (SkinData skin : skins) {
            renderSkin(html, skin);
        }

        html.append("<script>\n");
        html.append("const filter = document.getElementById('filter');\n");
        html.append("filter.addEventListener('input', () => {\n");
        html.append("    const query = filter.value.toLowerCase();\n");
        html.append("    document.querySelectorAll('.card, .style').forEach(element => {\n");
        html.append("        element.style.display = element.textContent.toLowerCase().includes(query) ? '' : 'none';\n");
        html.append("    });\n");
        html.append("});\n");
        html.append("</script>\n");
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    private void renderSkin(StringBuilder html, SkinData skin) {
        html.append("<section id=\"").append(skin.id).append("\">\n");
        html.append("<h2>").append(escape(skin.relativePath)).append("</h2>\n");

        renderDrawables(html, skin);
        renderStyles(html, skin);
        renderFonts(html, skin);
        renderColors(html, skin);

        html.append("</section>\n");
    }

    private void renderDrawables(StringBuilder html, SkinData skin) {
        if (skin.layout == null || skin.layout.regions().isEmpty()) {
            return;
        }

        Map<String, List<AtlasRegionData>> grouped = new TreeMap<>();
        for (AtlasRegionData region : skin.layout.regions()) {
            grouped.computeIfAbsent(region.name(), key -> new ArrayList<>()).add(region);
        }

        html.append("<h3>Drawables (").append(grouped.size()).append(")</h3>\n<div class=\"grid\">\n");
        for (Map.Entry<String, List<AtlasRegionData>> entry : grouped.entrySet()) {
            AtlasRegionData first = entry.getValue().get(0);
            html.append("<div class=\"card\">\n");
            html.append(thumbnail(first, skin, 96));
            html.append("<div class=\"name\">").append(escape(entry.getKey())).append("</div>\n");
            if (first.index() >= 0) {
                html.append("<div class=\"muted\">").append(entry.getValue().size())
                        .append(entry.getValue().size() > 1 ? " frames" : " frame")
                        .append(", Skin names ").append(escape(entry.getKey())).append("_&lt;index&gt;</div>\n");
            }
            html.append("</div>\n");
        }
        html.append("</div>\n");
    }

    private void renderStyles(StringBuilder html, SkinData skin) {
        boolean hasStyles = false;
        for (String className : skin.skin.names()) {
            if (!className.endsWith("Style")) {
                continue;
            }
            JsonValue classValue = skin.skin.get(className);
            if (classValue == null || !classValue.isObject()) {
                continue;
            }

            if (!hasStyles) {
                html.append("<h3>Styles</h3>\n");
                hasStyles = true;
            }

            html.append("<h4>").append(escape(styleTypeName(className))).append("</h4>\n");
            JsonObject styles = classValue.asObject();
            for (String styleName : styles.names()) {
                JsonValue styleValue = styles.get(styleName);
                if (styleValue == null || !styleValue.isObject()) {
                    continue;
                }

                html.append("<div class=\"style\">\n<div class=\"style-name\">").append(escape(styleName)).append("</div>\n");
                html.append("<div class=\"props\">\n");
                JsonObject style = styleValue.asObject();
                for (String property : style.names()) {
                    JsonValue value = style.get(property);
                    String text = value.isString() ? value.asString() : value.toString();
                    html.append("<span class=\"prop\">").append(escape(property)).append("</span>\n");
                    if (value.isString() && DRAWABLE_FIELDS.contains(property)) {
                        AtlasRegionData region = skin.regions.get(text);
                        if (region != null) {
                            html.append(thumbnail(region, skin, 32));
                        } else {
                            html.append("<span></span>\n");
                        }
                    } else {
                        html.append("<span></span>\n");
                    }
                    html.append("<code>").append(escape(text)).append("</code>\n");
                }
                html.append("</div>\n</div>\n");
            }
        }

        if (!hasStyles) {
            html.append("<h3>Styles</h3>\n<p class=\"muted\">No styles.</p>\n");
        }
    }

    private void renderFonts(StringBuilder html, SkinData skin) {
        JsonValue fonts = skin.skin.get(BITMAP_FONT_KEY);
        if (fonts == null || !fonts.isObject() || fonts.asObject().names().isEmpty()) {
            return;
        }

        html.append("<h3>Fonts</h3>\n<div class=\"grid\">\n");
        for (String font : fonts.asObject().names()) {
            html.append("<div class=\"card\"><div class=\"name\">").append(escape(font)).append("</div></div>\n");
        }
        html.append("</div>\n");
    }

    private void renderColors(StringBuilder html, SkinData skin) {
        JsonValue colors = skin.skin.get(COLOR_KEY);
        if (colors == null || !colors.isObject() || colors.asObject().names().isEmpty()) {
            return;
        }

        html.append("<h3>Colors</h3>\n<div class=\"grid\">\n");
        for (String colorName : colors.asObject().names()) {
            JsonValue colorValue = colors.asObject().get(colorName);
            if (colorValue == null || !colorValue.isObject()) {
                continue;
            }
            JsonObject color = colorValue.asObject();
            double r = number(color, "r", 0);
            double g = number(color, "g", 0);
            double b = number(color, "b", 0);
            double a = number(color, "a", 1);
            String rgba = "rgba(" + Math.round(r * 255) + ", " + Math.round(g * 255) + ", "
                    + Math.round(b * 255) + ", " + format(a) + ")";
            html.append("<div class=\"card\">\n");
            html.append("<div class=\"swatch\" style=\"background: ").append(rgba).append(";\"></div>\n");
            html.append("<div class=\"name\">").append(escape(colorName)).append("</div>\n");
            html.append("<div class=\"muted\">").append(escape(rgba)).append("</div>\n");
            html.append("</div>\n");
        }
        html.append("</div>\n");
    }

    private String thumbnail(AtlasRegionData region, SkinData skin, int maxSize) {
        PageData page = skin.pages.get(region.pageName());
        String url = skin.pageUrls.get(region.pageName());
        if (page == null || url == null || region.width() <= 0 || region.height() <= 0) {
            return "";
        }

        int pageWidth = page.width();
        int pageHeight = page.height();
        if (pageWidth <= 0 || pageHeight <= 0) {
            BufferedImage image = readPageImage(skin, region.pageName());
            if (image == null) {
                return "";
            }
            pageWidth = image.getWidth();
            pageHeight = image.getHeight();
        }

        double scale = Math.min(1.0, (double) maxSize / Math.max(region.width(), region.height()));
        int width = Math.max(1, (int) Math.round(region.width() * scale));
        int height = Math.max(1, (int) Math.round(region.height() * scale));
        double backgroundWidth = pageWidth * scale;
        double backgroundHeight = pageHeight * scale;
        double backgroundX = -region.x() * scale;
        double backgroundY = -region.y() * scale;

        return "<div class=\"thumb\" title=\"" + escape(region.name()) + "\" style=\"width: " + width
                + "px; height: " + height
                + "px; background-image: url('" + escape(url)
                + "'); background-size: " + format(backgroundWidth) + "px " + format(backgroundHeight)
                + "px; background-position: " + format(backgroundX) + "px " + format(backgroundY) + "px;\"></div>\n";
    }

    private BufferedImage readPageImage(SkinData skin, String pageName) {
        if (skin.atlasFile == null) {
            return null;
        }
        File pageFile = skin.atlasFile.getParent().resolve(pageName).toFile();
        try {
            return ImageIO.read(pageFile);
        } catch (IOException e) {
            return null;
        }
    }

    private double number(JsonObject object, String name, double defaultValue) {
        JsonValue value = object.get(name);
        return value == null ? defaultValue : value.asDouble();
    }

    private String styleTypeName(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        int dollar = simpleName.indexOf('$');
        if (dollar >= 0) {
            return simpleName.substring(0, dollar);
        }
        if (simpleName.endsWith("Style")) {
            return simpleName.substring(0, simpleName.length() - 5);
        }
        return simpleName;
    }

    private String fileStem(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(0, dot);
    }

    private String normalizeAtlasExtension(String atlasExtension) {
        if (atlasExtension == null || atlasExtension.isBlank()) {
            return ".atlas";
        }
        return atlasExtension.startsWith(".") ? atlasExtension : "." + atlasExtension;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final class SkinData {
        private final String id;
        private final String relativePath;
        private final JsonObject skin;
        private final Map<String, PageData> pages = new HashMap<>();
        private final Map<String, String> pageUrls = new HashMap<>();
        private final Map<String, AtlasRegionData> regions = new HashMap<>();
        private Path atlasFile;
        private AtlasLayout layout;

        private SkinData(String id, String relativePath, JsonObject skin) {
            this.id = id;
            this.relativePath = relativePath;
            this.skin = skin;
        }
    }
}
