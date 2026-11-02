package io.github.elebras1.gdxskinweaver;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class PackerSettings {

    private final Property<Integer> maxWidth;
    private final Property<Integer> maxHeight;
    private final Property<Integer> minWidth;
    private final Property<Integer> minHeight;
    private final Property<Integer> paddingX;
    private final Property<Integer> paddingY;
    private final Property<Integer> alphaThreshold;
    private final Property<Integer> bleedIterations;
    private final Property<Boolean> pot;
    private final Property<Boolean> multipleOfFour;
    private final Property<Boolean> edgePadding;
    private final Property<Boolean> duplicatePadding;
    private final Property<Boolean> rotation;
    private final Property<Boolean> square;
    private final Property<Boolean> stripWhitespaceX;
    private final Property<Boolean> stripWhitespaceY;
    private final Property<Boolean> ignoreBlankImages;
    private final Property<Boolean> fast;
    private final Property<Boolean> debug;
    private final Property<Boolean> silent;
    private final Property<Boolean> combineSubdirectories;
    private final Property<Boolean> flattenPaths;
    private final Property<Boolean> useIndexes;
    private final Property<Boolean> premultiplyAlpha;
    private final Property<Boolean> bleed;
    private final Property<Boolean> prettyPrint;
    private final Property<Boolean> legacyOutput;
    private final Property<String> filterMin;
    private final Property<String> filterMag;
    private final Property<String> wrapX;
    private final Property<String> wrapY;
    private final Property<String> outputFormat;
    private final Property<String> atlasExtension;
    private final Property<Double> jpegQuality;

    @Inject
    public PackerSettings(ObjectFactory objects) {
        this.maxWidth = objects.property(Integer.class).convention(2048);
        this.maxHeight = objects.property(Integer.class).convention(2048);
        this.minWidth = objects.property(Integer.class).convention(16);
        this.minHeight = objects.property(Integer.class).convention(16);
        this.paddingX = objects.property(Integer.class).convention(2);
        this.paddingY = objects.property(Integer.class).convention(2);
        this.alphaThreshold = objects.property(Integer.class).convention(1);
        this.bleedIterations = objects.property(Integer.class).convention(2);
        this.pot = objects.property(Boolean.class).convention(true);
        this.multipleOfFour = objects.property(Boolean.class).convention(false);
        this.edgePadding = objects.property(Boolean.class).convention(true);
        this.duplicatePadding = objects.property(Boolean.class).convention(false);
        this.rotation = objects.property(Boolean.class).convention(false);
        this.square = objects.property(Boolean.class).convention(false);
        this.stripWhitespaceX = objects.property(Boolean.class).convention(false);
        this.stripWhitespaceY = objects.property(Boolean.class).convention(false);
        this.ignoreBlankImages = objects.property(Boolean.class).convention(true);
        this.fast = objects.property(Boolean.class).convention(false);
        this.debug = objects.property(Boolean.class).convention(false);
        this.silent = objects.property(Boolean.class).convention(false);
        this.combineSubdirectories = objects.property(Boolean.class).convention(false);
        this.flattenPaths = objects.property(Boolean.class).convention(true);
        this.useIndexes = objects.property(Boolean.class).convention(true);
        this.premultiplyAlpha = objects.property(Boolean.class).convention(false);
        this.bleed = objects.property(Boolean.class).convention(false);
        this.prettyPrint = objects.property(Boolean.class).convention(false);
        this.legacyOutput = objects.property(Boolean.class).convention(false);
        this.filterMin = objects.property(String.class).convention("Nearest");
        this.filterMag = objects.property(String.class).convention("Nearest");
        this.wrapX = objects.property(String.class).convention("ClampToEdge");
        this.wrapY = objects.property(String.class).convention("ClampToEdge");
        this.outputFormat = objects.property(String.class).convention("png");
        this.atlasExtension = objects.property(String.class).convention(".atlas");
        this.jpegQuality = objects.property(Double.class).convention(0.9d);
    }

    public Property<Integer> getMaxWidth() {
        return maxWidth;
    }

    public Property<Integer> getMaxHeight() {
        return maxHeight;
    }

    public Property<Integer> getMinWidth() {
        return minWidth;
    }

    public Property<Integer> getMinHeight() {
        return minHeight;
    }

    public Property<Integer> getPaddingX() {
        return paddingX;
    }

    public Property<Integer> getPaddingY() {
        return paddingY;
    }

    public Property<Integer> getAlphaThreshold() {
        return alphaThreshold;
    }

    public Property<Integer> getBleedIterations() {
        return bleedIterations;
    }

    public Property<Boolean> getPot() {
        return pot;
    }

    public Property<Boolean> getMultipleOfFour() {
        return multipleOfFour;
    }

    public Property<Boolean> getEdgePadding() {
        return edgePadding;
    }

    public Property<Boolean> getDuplicatePadding() {
        return duplicatePadding;
    }

    public Property<Boolean> getRotation() {
        return rotation;
    }

    public Property<Boolean> getSquare() {
        return square;
    }

    public Property<Boolean> getStripWhitespaceX() {
        return stripWhitespaceX;
    }

    public Property<Boolean> getStripWhitespaceY() {
        return stripWhitespaceY;
    }

    public Property<Boolean> getIgnoreBlankImages() {
        return ignoreBlankImages;
    }

    public Property<Boolean> getFast() {
        return fast;
    }

    public Property<Boolean> getDebug() {
        return debug;
    }

    public Property<Boolean> getSilent() {
        return silent;
    }

    public Property<Boolean> getCombineSubdirectories() {
        return combineSubdirectories;
    }

    public Property<Boolean> getFlattenPaths() {
        return flattenPaths;
    }

    public Property<Boolean> getUseIndexes() {
        return useIndexes;
    }

    public Property<Boolean> getPremultiplyAlpha() {
        return premultiplyAlpha;
    }

    public Property<Boolean> getBleed() {
        return bleed;
    }

    public Property<Boolean> getPrettyPrint() {
        return prettyPrint;
    }

    public Property<Boolean> getLegacyOutput() {
        return legacyOutput;
    }

    public Property<String> getFilterMin() {
        return filterMin;
    }

    public Property<String> getFilterMag() {
        return filterMag;
    }

    public Property<String> getWrapX() {
        return wrapX;
    }

    public Property<String> getWrapY() {
        return wrapY;
    }

    public Property<String> getOutputFormat() {
        return outputFormat;
    }

    public Property<String> getAtlasExtension() {
        return atlasExtension;
    }

    public Property<Double> getJpegQuality() {
        return jpegQuality;
    }

    public Map<String, String> toOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("maxWidth", String.valueOf(maxWidth.get()));
        options.put("maxHeight", String.valueOf(maxHeight.get()));
        options.put("minWidth", String.valueOf(minWidth.get()));
        options.put("minHeight", String.valueOf(minHeight.get()));
        options.put("paddingX", String.valueOf(paddingX.get()));
        options.put("paddingY", String.valueOf(paddingY.get()));
        options.put("alphaThreshold", String.valueOf(alphaThreshold.get()));
        options.put("bleedIterations", String.valueOf(bleedIterations.get()));
        options.put("pot", String.valueOf(pot.get()));
        options.put("multipleOfFour", String.valueOf(multipleOfFour.get()));
        options.put("edgePadding", String.valueOf(edgePadding.get()));
        options.put("duplicatePadding", String.valueOf(duplicatePadding.get()));
        options.put("rotation", String.valueOf(rotation.get()));
        options.put("square", String.valueOf(square.get()));
        options.put("stripWhitespaceX", String.valueOf(stripWhitespaceX.get()));
        options.put("stripWhitespaceY", String.valueOf(stripWhitespaceY.get()));
        options.put("ignoreBlankImages", String.valueOf(ignoreBlankImages.get()));
        options.put("fast", String.valueOf(fast.get()));
        options.put("debug", String.valueOf(debug.get()));
        options.put("silent", String.valueOf(silent.get()));
        options.put("combineSubdirectories", String.valueOf(combineSubdirectories.get()));
        options.put("flattenPaths", String.valueOf(flattenPaths.get()));
        options.put("useIndexes", String.valueOf(useIndexes.get()));
        options.put("premultiplyAlpha", String.valueOf(premultiplyAlpha.get()));
        options.put("bleed", String.valueOf(bleed.get()));
        options.put("prettyPrint", String.valueOf(prettyPrint.get()));
        options.put("legacyOutput", String.valueOf(legacyOutput.get()));
        options.put("filterMin", filterMin.get());
        options.put("filterMag", filterMag.get());
        options.put("wrapX", wrapX.get());
        options.put("wrapY", wrapY.get());
        options.put("outputFormat", outputFormat.get());
        options.put("atlasExtension", atlasExtension.get());
        options.put("jpegQuality", String.valueOf(jpegQuality.get()));
        return options;
    }
}
