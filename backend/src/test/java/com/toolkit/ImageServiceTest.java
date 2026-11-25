package com.toolkit;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageServiceTest {

    private final ImageService service = new ImageService();

    private static byte[] alphaPng(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 120, 255, 180));
        g.fillRect(10, 10, w - 20, h - 20);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    @Test
    void convertSingleToJpgAndResize() throws IOException {
        MultipartFile src = new MockMultipartFile("files", "photo.png", null, alphaPng(400, 200));
        byte[] out = service.convert(List.of(src), new Job("i1", "converted.jpg"), "jpg", 80, 200);
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(out));
        assertNotNull(bi);
        assertEquals(200, bi.getWidth());
        assertEquals(100, bi.getHeight());
        assertFalse(bi.getColorModel().hasAlpha());
    }

    @Test
    void convertBatchProducesZipAndRejectsInvalid() throws IOException {
        byte[] png = alphaPng(100, 100);
        byte[] zip = service.convert(
                List.of(new MockMultipartFile("files", "a.png", null, png),
                        new MockMultipartFile("files", "b.png", null, png)),
                new Job("i2", "converted.zip"), "bmp", 85, null);

        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) names.add(e.getName());
        }
        assertEquals(List.of("a.bmp", "b.bmp"), names);

        MultipartFile bad = new MockMultipartFile("files", "bad.png", null, "not-an-image".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> service.convert(List.of(bad), new Job("i3", "c.jpg"), "jpg", 85, null));
    }
}
