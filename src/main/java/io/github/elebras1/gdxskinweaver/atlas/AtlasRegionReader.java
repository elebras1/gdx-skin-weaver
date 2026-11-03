package io.github.elebras1.gdxskinweaver.atlas;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasRegionReader {

    /**
     * Reads the region names as a libGDX {@code Skin} registers them: an indexed region {@code load} with
     * {@code index:3} is reachable as {@code load_3}. The base names of indexed regions, used with
     * {@code TextureAtlas#findRegions}, are returned separately.
     */
    public AtlasRegions read(File atlasFile) throws IOException {
        AtlasLayout layout = readLayout(atlasFile);
        Set<String> names = new LinkedHashSet<>();
        Set<String> indexedNames = new LinkedHashSet<>();
        for (AtlasRegionData region : layout.regions()) {
            names.add(region.skinName());
            if (region.index() >= 0) {
                indexedNames.add(region.name());
            }
        }
        return new AtlasRegions(Set.copyOf(names), Set.copyOf(indexedNames), layout.duplicates());
    }

    public AtlasLayout readLayout(File atlasFile) throws IOException {
        List<String> lines = Files.readAllLines(atlasFile.toPath(), StandardCharsets.UTF_8);
        List<PageData> pages = new ArrayList<>();
        List<AtlasRegionData> regions = new ArrayList<>();
        PageData currentPage = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.contains(":")) {
                continue;
            }

            String next = i + 1 < lines.size() ? lines.get(i + 1).trim() : "";
            if (isPageHeader(next)) {
                int width = 0;
                int height = 0;
                if (next.startsWith("size:")) {
                    int[] size = parseInts(next.substring("size:".length()), 2);
                    width = size[0];
                    height = size[1];
                }
                currentPage = new PageData(line, width, height);
                pages.add(currentPage);
                i++;
                continue;
            }

            String name = line;
            int index = -1;
            int x = 0;
            int y = 0;
            int width = 0;
            int height = 0;
            boolean rotate = false;
            int[] splits = null;

            int j = i + 1;
            while (j < lines.size()) {
                String property = lines.get(j).trim();
                if (property.isEmpty()) {
                    j++;
                    continue;
                }
                if (!property.contains(":")) {
                    break;
                }

                int colon = property.indexOf(':');
                String key = property.substring(0, colon).trim();
                String value = property.substring(colon + 1).trim();
                switch (key) {
                    case "index" -> index = parseInt(value, -1);
                    case "bounds" -> {
                        int[] bounds = parseInts(value, 4);
                        x = bounds[0];
                        y = bounds[1];
                        width = bounds[2];
                        height = bounds[3];
                    }
                    case "rotate" -> rotate = Boolean.parseBoolean(value);
                    case "split" -> splits = parseInts(value, 4);
                    default -> {
                    }
                }
                j++;
            }

            regions.add(new AtlasRegionData(currentPage == null ? "" : currentPage.name(),
                    name, index, x, y, width, height, rotate, splits));
            i = j - 1;
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        Set<String> indexed = new HashSet<>();
        for (AtlasRegionData region : regions) {
            counts.merge(region.name(), 1, Integer::sum);
            if (region.index() >= 0) {
                indexed.add(region.name());
            }
        }

        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1 && !indexed.contains(entry.getKey())) {
                duplicates.add(entry.getKey());
            }
        }

        return new AtlasLayout(List.copyOf(pages), List.copyOf(regions), List.copyOf(duplicates));
    }

    private boolean isPageHeader(String nextLine) {
        return nextLine.startsWith("size:")
                || nextLine.startsWith("format:")
                || nextLine.startsWith("filter:")
                || nextLine.startsWith("repeat:");
    }

    private int[] parseInts(String value, int expected) {
        String[] parts = value.split(",");
        int[] result = new int[expected];
        for (int i = 0; i < expected && i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public record AtlasRegions(Set<String> names, Set<String> indexedNames, List<String> duplicates) {
    }

    public record AtlasLayout(List<PageData> pages, List<AtlasRegionData> regions, List<String> duplicates) {
    }

    public record PageData(String name, int width, int height) {
    }

    public record AtlasRegionData(String pageName, String name, int index, int x, int y, int width, int height,
                                  boolean rotate, int[] splits) {

        /** Name under which a libGDX {@code Skin} registers this region. */
        public String skinName() {
            return index >= 0 ? name + "_" + index : name;
        }
    }
}
