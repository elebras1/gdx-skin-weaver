package io.github.elebras1.gdxskinweaver.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

public class ImageContrastAdjuster {
    public void apply(File source, File target, float percent) throws IOException {
        BufferedImage src = ImageIO.read(source);
        if (src == null) throw new IOException("Cannot read: " + source);
        float factor = 1f + percent / 100f;
        float offset = 0.5f * (1f - factor);
        BufferedImage dst = new RescaleOp(factor, offset, null).filter(src, null);
        target.getParentFile().mkdirs();

        if (isJpeg(target)) {
            dst = toRgb(dst);
            if (!ImageIO.write(dst, "jpg", target)) {
                throw new IOException("No JPEG writer available for: " + target);
            }
            return;
        }

        if (!ImageIO.write(dst, "png", target)) {
            throw new IOException("No PNG writer available for: " + target);
        }
    }

    private boolean isJpeg(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private BufferedImage toRgb(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }
}
