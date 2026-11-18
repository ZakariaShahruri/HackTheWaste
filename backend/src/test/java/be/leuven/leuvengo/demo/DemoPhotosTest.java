package be.leuven.leuvengo.demo;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPhotosTest {

    @Test
    void dirtierSpotsGetMoreLitter() {
        assertThat(DemoPhotos.litterFor(100)).isZero();
        assertThat(DemoPhotos.litterFor(90)).isLessThan(DemoPhotos.litterFor(50));
        assertThat(DemoPhotos.litterFor(50)).isLessThan(DemoPhotos.litterFor(10));
    }

    @Test
    void theSameSeedPaintsTheSamePicture() {
        BufferedImage a = DemoPhotos.paint(30, new Random(3));
        BufferedImage b = DemoPhotos.paint(30, new Random(3));

        assertThat(a.getWidth()).isEqualTo(DemoPhotos.WIDTH);
        assertThat(a.getHeight()).isEqualTo(DemoPhotos.HEIGHT);
        assertThat(pixels(a)).isEqualTo(pixels(b));
        assertThat(pixels(a)).isNotEqualTo(pixels(DemoPhotos.paint(30, new Random(4))));
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }
}
