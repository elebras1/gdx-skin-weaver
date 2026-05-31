package io.github.elebras1.gdxskinweaver.image;

import javax.imageio.ImageIO;
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
        ImageIO.write(dst, "png", target);
    }
}

