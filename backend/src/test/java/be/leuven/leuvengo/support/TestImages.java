package be.leuven.leuvengo.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Small real images for tests that go through the photo pipeline. */
public final class TestImages {

    private TestImages() {}

    public static byte[] jpeg(int width, int height) {
        return encode(width, height, BufferedImage.TYPE_INT_RGB, "jpeg");
    }

    public static byte[] png(int width, int height) {
        return encode(width, height, BufferedImage.TYPE_INT_ARGB, "png");
    }

    private static byte[] encode(int width, int height, int type, String format) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(40, 120, 60));
        g.fillRect(0, 0, width, height / 2);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
