package com.toolkit;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class PdfServiceTest {

    private final PdfService service = new PdfService();

    private static byte[] pngBytes(int w, int h, Color c) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private static byte[] noisyPngBytes(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random rnd = new Random(42);
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int rgb = new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)).getRGB();
                img.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private static List<String> zipNames(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) names.add(e.getName());
        }
        return names;
    }

    private static MultipartFile mf(String name, byte[] data) {
        return new MockMultipartFile("files", name, null, data);
    }

    private byte[] makePdf(int pages) throws IOException {
        List<MultipartFile> imgs = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            imgs.add(mf("p" + i + ".png", pngBytes(300, 200, new Color(40 * (i + 1) % 255, 120, 200))));
        }
        return service.imagesToPdf(imgs, new Job("t", "a.pdf"), true);
    }

    @Test
    void parseRangesValidAndInvalid() {
        List<int[]> all = PdfService.parseRanges("", 3);
        assertEquals(3, all.size());
        assertArrayEquals(new int[]{2, 2}, all.get(1));

        List<int[]> custom = PdfService.parseRanges("1-2，4，3-2", 5);
        assertEquals(3, custom.size());
        assertArrayEquals(new int[]{1, 2}, custom.get(0));
        assertArrayEquals(new int[]{4, 4}, custom.get(1));
        assertArrayEquals(new int[]{2, 3}, custom.get(2));

        assertThrows(IllegalArgumentException.class, () -> PdfService.parseRanges("6", 5));
        assertThrows(IllegalArgumentException.class, () -> PdfService.parseRanges("abc", 5));
    }

    @Test
    void mergeAndSplitPdf() throws IOException {
        byte[] p2 = makePdf(2);
        byte[] p3 = makePdf(3);
        Job mergeJob = new Job("m", "merged.pdf");
        byte[] merged = service.merge(List.of(mf("a.pdf", p2), mf("b.pdf", p3)), mergeJob);
        try (PDDocument doc = Loader.loadPDF(merged)) {
            assertEquals(5, doc.getNumberOfPages());
        }

        Job splitJob = new Job("s", "split.zip");
        byte[] zip = service.split(mf("doc.pdf", merged), "1-2,5", splitJob);
        assertEquals(List.of("doc_1-2.pdf", "doc_5.pdf"), zipNames(zip));
    }

    @Test
    void pdfToImagesPerPageAndLongImage() throws IOException {
        byte[] pdf = makePdf(3);

        byte[] zip = service.pdfToImages(List.of(mf("sample.pdf", pdf)), new Job("i1", "pdf_images.zip"), "png", 72, false, 0);
        assertEquals(List.of("sample_01.png", "sample_02.png", "sample_03.png"), zipNames(zip));

        byte[] longImg = service.pdfToImages(List.of(mf("sample.pdf", pdf)), new Job("i2", "long.png"), "png", 72, true, 10);
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(longImg));
        assertNotNull(bi);
        assertTrue(bi.getHeight() > bi.getWidth(), "Long image should be taller than wide");
    }

    @Test
    void watermarkEncryptDecryptAndCompress() throws IOException {
        byte[] pdf = makePdf(2);

        // Watermark
        assertThrows(IllegalArgumentException.class,
                () -> service.watermark(List.of(mf("a.pdf", pdf)), new Job("w0", "w.pdf"), "  ", 30, 0.3f, 30, true, "#888888"));
        byte[] wm = service.watermark(List.of(mf("a.pdf", pdf)), new Job("w1", "w.pdf"), "CONFIDENTIAL", 30, 0.3f, 35, true, "#cc0000");
        try (PDDocument doc = Loader.loadPDF(wm)) {
            assertEquals(2, doc.getNumberOfPages());
        }

        // Encrypt & Decrypt
        byte[] enc = service.protect(List.of(mf("a.pdf", pdf)), new Job("e1", "e.pdf"), "encrypt", "secret", "", true, false);
        assertThrows(IllegalArgumentException.class,
                () -> service.protect(List.of(mf("e.pdf", enc)), new Job("d0", "d.pdf"), "decrypt", "wrong", "", true, true));
        byte[] dec = service.protect(List.of(mf("e.pdf", enc)), new Job("d1", "d.pdf"), "decrypt", "secret", "", true, true);
        try (PDDocument doc = Loader.loadPDF(dec)) {
            assertFalse(doc.isEncrypted());
            assertEquals(2, doc.getNumberOfPages());
        }

        // Compress noisy image PDF
        byte[] bigPdf = service.imagesToPdf(List.of(mf("big.png", noisyPngBytes(800, 600))), new Job("c0", "big.pdf"), false);
        byte[] comp = service.compress(List.of(mf("big.pdf", bigPdf)), new Job("c1", "comp.pdf"), "high");
        assertTrue(comp.length < bigPdf.length, "Compressed PDF should be smaller than noisy PNG PDF");
    }
}
