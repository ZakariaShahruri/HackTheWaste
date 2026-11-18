package be.leuven.leuvengo.photo;

import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Re-encodes every uploaded photo as a JPEG of at most {@value #MAX_EDGE} px.
 * Besides saving space, this proves the bytes really are an image and drops
 * all metadata (EXIF GPS, device info) before anything is stored or shared.
 */
@Component
public class ImageNormalizer {

    public static final String JPEG = "image/jpeg";
    static final int MAX_EDGE = 1280;
    /** Refuse absurd dimensions before decoding, so a tiny file can't claim gigabytes of pixels. */
    static final long MAX_PIXELS = 50_000_000L;
    private static final float QUALITY = 0.85f;

    public record Normalized(byte[] bytes, int width, int height) {
        public String contentType() {
            return JPEG;
        }
    }

    public static class UnreadableImageException extends Exception {
        public UnreadableImageException(String message) {
            super(message);
        }
    }

    public Normalized normalize(byte[] input) throws UnreadableImageException {
        return normalize(decode(input));
    }

    /** For images drawn by the app itself, which need no decoding. */
    public Normalized normalize(BufferedImage image) {
        BufferedImage scaled = toRgbWithin(image, MAX_EDGE);
        return new Normalized(encodeJpeg(scaled), scaled.getWidth(), scaled.getHeight());
    }

    private static BufferedImage decode(byte[] input) throws UnreadableImageException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw new UnreadableImageException("Not a supported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw new UnreadableImageException("Image dimensions too large");
                }
                // Decode big photos at reduced resolution straight away (still >= 2x the target).
                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, Math.max(width, height) / (2 * MAX_EDGE));
                param.setSourceSubsampling(step, step, 0, 0);
                BufferedImage image = reader.read(0, param);
                if (image == null) throw new UnreadableImageException("Empty image");
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new UnreadableImageException("Could not decode image");
        }
    }

    /** Scales down to fit {@code maxEdge} and flattens onto white (JPEG has no alpha). */
    private static BufferedImage toRgbWithin(BufferedImage source, int maxEdge) {
        double factor = Math.min(1.0, maxEdge / (double) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(source.getHeight() * factor));

        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("JPEG encoding failed", e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
