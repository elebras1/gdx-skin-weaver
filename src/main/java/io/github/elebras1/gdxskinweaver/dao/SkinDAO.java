package io.github.elebras1.gdxskinweaver.dao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SkinDAO {

    public void write(File targetSkin) {
        try (FileWriter writer = new FileWriter(targetSkin)) {
            writer.write("{}");
        } catch (IOException e) {
            System.err.println("Failed to write empty skin: " + e.getMessage());
        }
    }

    public void write(File targetSkin, List<File> fonts) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"com.badlogic.gdx.graphics.g2d.BitmapFont\": {\n");

        for (int i = 0; i < fonts.size(); i++) {
            File font = fonts.get(i);
            String baseName = font.getName().replaceFirst("\\.fnt$", "");
            json.append("    \"").append(baseName).append("\": {\n");
            json.append("      \"file\": \"").append(font.getName()).append("\",\n");
            json.append("      \"scaledSize\": -1,\n");
            json.append("      \"markupEnabled\": false,\n");
            json.append("      \"flip\": false\n");
            json.append("    }");
            if (i < fonts.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  }\n");
        json.append("}\n");

        try (FileWriter writer = new FileWriter(targetSkin)) {
            writer.write(json.toString());
        } catch (IOException e) {
            System.err.println("Failed to write default skin: " + e.getMessage());
        }
    }
}
