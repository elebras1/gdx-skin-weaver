package io.github.elebras1.gdxskinweaver.skin;

import io.github.elebras1.gdxskinweaver.service.SkinJsonService;
import io.github.elebras1.gdxskinweaver.util.AssetFileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SkinFileHandler {
    private final SkinJsonService skinDAO;

    public SkinFileHandler(SkinJsonService skinService) {
        this.skinDAO = skinService;
    }

    public File handle(File sourceDir, File outputRoot, Path assetsRoot, File existingSkin, List<File> fonts,
                       Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull) {
        return handle(sourceDir, outputRoot, assetsRoot, existingSkin, fonts, buttonSimpleToFull, toggleSimpleToFull,
                SkinGenerationConfig.defaults());
    }

    public File handle(File sourceDir, File outputRoot, Path assetsRoot, File existingSkin, List<File> fonts,
                       Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull,
                       SkinGenerationConfig config) {
        File targetDir = AssetFileUtils.getOutputDirectory(sourceDir, outputRoot, assetsRoot);
        if (targetDir == null) {
            return null;
        }
        File targetSkin = new File(targetDir, sourceDir.getName() + ".json");

        if (existingSkin != null && existingSkin.exists()) {
            writeWithExisting(existingSkin, targetSkin, fonts, buttonSimpleToFull, toggleSimpleToFull, config);
            return targetSkin;
        }

        writeNew(targetSkin, fonts, buttonSimpleToFull, toggleSimpleToFull, config);
        return targetSkin;
    }

    private void writeWithExisting(File existingSkin, File targetSkin, List<File> fonts,
                                   Map<String, String> buttonSimpleToFull, Map<String, String> toggleSimpleToFull,
                                   SkinGenerationConfig config) {
        if (fonts.isEmpty() && buttonSimpleToFull.isEmpty() && toggleSimpleToFull.isEmpty()) {
            skinDAO.write(existingSkin, targetSkin);
            return;
        }
        skinDAO.merge(targetSkin, existingSkin, fonts, buttonSimpleToFull, toggleSimpleToFull, config);
    }

    private void writeNew(File targetSkin, List<File> fonts, Map<String, String> buttonSimpleToFull,
                          Map<String, String> toggleSimpleToFull, SkinGenerationConfig config) {
        if (fonts.isEmpty() && buttonSimpleToFull.isEmpty() && toggleSimpleToFull.isEmpty()) {
            skinDAO.write(targetSkin);
            return;
        }
        if (fonts.isEmpty()) {
            skinDAO.write(targetSkin, buttonSimpleToFull, toggleSimpleToFull, config);
            return;
        }
        if (buttonSimpleToFull.isEmpty() && toggleSimpleToFull.isEmpty()) {
            skinDAO.write(targetSkin, fonts, config);
            return;
        }
        skinDAO.write(targetSkin, fonts, buttonSimpleToFull, toggleSimpleToFull, config);
    }
}
