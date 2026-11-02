package io.github.elebras1.gdxskinweaver.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinReportGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesHtmlReportWithThumbnailsAndStyles() throws IOException {
        Path skins = tempDir.resolve("skins");
        Path ui = skins.resolve("ui");
        Files.createDirectories(ui);
        Files.writeString(ui.resolve("ui.json"), """
                {
                  "com.badlogic.gdx.graphics.Color": {
                    "red": {"r": 1, "g": 0, "b": 0, "a": 1}
                  },
                  "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle": {
                    "ok_btn": {"up": "ok_btn"}
                  }
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(ui.resolve("ui.atlas"), """
                ui.png
                size:64,64
                format:RGBA8888
                filter:Nearest,Nearest
                repeat:none

                ok_btn
                bounds:0,0,10,10
                """, StandardCharsets.UTF_8);
        writePng(ui.resolve("ui.png"));

        Path report = tempDir.resolve("out/index.html");
        new SkinReportGenerator().generate(skins.toFile(), report.toFile(), ".atlas");

        String html = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(html.contains("Skin Weaver report"));
        assertTrue(html.contains("ui.json"));
        assertTrue(html.contains("ok_btn"));
        assertTrue(html.contains("background-image"));
        assertTrue(html.contains("file:"));
        assertTrue(html.contains("Button"));
        assertTrue(html.contains("rgba(255, 0, 0, 1.00)"));
        assertTrue(html.contains("id=\"filter\""));
        assertTrue(html.contains("addEventListener"));
    }

    private void writePng(Path file) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 64, 64);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", file.toFile());
    }
}
