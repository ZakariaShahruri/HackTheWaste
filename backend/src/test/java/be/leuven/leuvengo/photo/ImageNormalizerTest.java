package be.leuven.leuvengo.photo;

import be.leuven.leuvengo.support.TestImages;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageNormalizerTest {

    private final ImageNormalizer normalizer = new ImageNormalizer();

    @Test
    void shrinksLargePhotosToTheMaxEdgeKeepingAspectRatio() throws Exception {
        ImageNormalizer.Normalized out = normalizer.normalize(TestImages.jpeg(4000, 3000));

        assertThat(out.width()).isEqualTo(1280);
        assertThat(out.height()).isEqualTo(960);
        assertThat(out.contentType()).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(out.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(1280);
    }

    @Test
    void leavesSmallPhotosAtTheirSize() throws Exception {
        ImageNormalizer.Normalized out = normalizer.normalize(TestImages.jpeg(640, 480));
        assertThat(out.width()).isEqualTo(640);
        assertThat(out.height()).isEqualTo(480);
    }

    @Test
    void convertsPngWithTransparencyToJpeg() throws Exception {
        ImageNormalizer.Normalized out = normalizer.normalize(TestImages.png(300, 200));
        assertThat(out.bytes()[0]).isEqualTo((byte) 0xFF);
        assertThat(out.bytes()[1]).isEqualTo((byte) 0xD8);
    }

    @Test
    void dropsExifMetadata() throws Exception {
        byte[] withExif = insertExif(TestImages.jpeg(200, 100), "GPS 50.8798N 4.7005E");

        byte[] out = normalizer.normalize(withExif).bytes();

        assertThat(new String(out, StandardCharsets.ISO_8859_1)).doesNotContain("Exif", "GPS");
    }

    @Test
    void rejectsThingsThatAreNotImages() {
        assertThatThrownBy(() -> normalizer.normalize("<html>hi</html>".getBytes()))
                .isInstanceOf(ImageNormalizer.UnreadableImageException.class);
        assertThatThrownBy(() -> normalizer.normalize(new byte[0]))
                .isInstanceOf(ImageNormalizer.UnreadableImageException.class);
    }

    @Test
    void rejectsTruncatedImages() {
        byte[] jpeg = TestImages.jpeg(400, 300);
        byte[] truncated = java.util.Arrays.copyOf(jpeg, 20);
        assertThatThrownBy(() -> normalizer.normalize(truncated))
                .isInstanceOf(ImageNormalizer.UnreadableImageException.class);
    }

    @Test
    void refusesDecompressionBombsBeforeDecoding() {
        assertThatThrownBy(() -> normalizer.normalize(pngHeaderClaiming(100_000, 100_000)))
                .isInstanceOf(ImageNormalizer.UnreadableImageException.class);
    }

    /** Puts an APP1 "Exif" segment right after the JPEG start marker. */
    private static byte[] insertExif(byte[] jpeg, String payload) {
        byte[] body = ("Exif\0\0" + payload).getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        int length = body.length + 2;
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.write(body, 0, body.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    /** A PNG whose header announces huge dimensions, with no real pixel data. */
    private static byte[] pngHeaderClaiming(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(17);
        ihdr.put("IHDR".getBytes(StandardCharsets.US_ASCII)).putInt(width).putInt(height)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        return ByteBuffer.allocate(8 + 4 + 17 + 4)
                .put(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})
                .putInt(13).put(ihdr.array()).putInt((int) crc.getValue())
                .array();
    }
}
