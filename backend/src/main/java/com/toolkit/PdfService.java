package com.toolkit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** PDF 合并 / 拆分 / 图片互转 / 水印 / 加密解密 / 压缩（基于 Apache PDFBox） */
@Service
public class PdfService {

    /** 按上传顺序合并多个 PDF */
    public byte[] merge(List<? extends MultipartFile> files, Job job) throws IOException {
        List<PDDocument> sources = new ArrayList<>();
        try (PDDocument dest = new PDDocument()) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (MultipartFile f : files) {
                PDDocument src = openOrFail(f);
                sources.add(src);
                merger.appendDocument(dest, src);
                job.step(FileUtil.safeName(f.getOriginalFilename()));
            }
            job.phase("正在保存...");
            return toBytes(dest);
        } finally {
            for (PDDocument d : sources) d.close();
        }
    }

    /**
     * 拆分 PDF。ranges 形如 "1-3,5,7-9"，每段生成一个 PDF；为空则每页一个 PDF。
     * 返回 zip。
     */
    public byte[] split(MultipartFile file, String ranges, Job job) throws IOException {
        Path dir = FileUtil.tempDir();
        try (PDDocument src = openOrFail(file)) {
            int total = src.getNumberOfPages();
            List<int[]> parts = parseRanges(ranges, total);
            job.setTotal(parts.size());
            String base = FileUtil.safeName(file.getOriginalFilename()).replaceAll("(?i)\\.pdf$", "");
            Map<String, Path> result = new LinkedHashMap<>();
            for (int[] r : parts) {
                try (PDDocument out = new PDDocument()) {
                    for (int p = r[0]; p <= r[1]; p++) out.importPage(src.getPage(p - 1));
                    String label = r[0] == r[1] ? String.valueOf(r[0]) : r[0] + "-" + r[1];
                    Path tmp = dir.resolve("part_" + label + ".pdf");
                    out.save(tmp.toFile());
                    result.put(base + "_" + label + ".pdf", tmp);
                    job.step("第 " + label + " 页");
                }
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            FileUtil.deleteQuietly(dir);
        }
    }

    static List<int[]> parseRanges(String ranges, int total) {
        List<int[]> list = new ArrayList<>();
        if (ranges == null || ranges.isBlank()) {
            for (int i = 1; i <= total; i++) list.add(new int[]{i, i});
            return list;
        }
        for (String seg : ranges.replace('，', ',').split(",")) {
            seg = seg.trim();
            if (seg.isEmpty()) continue;
            try {
                int a, b;
                if (seg.contains("-")) {
                    String[] ab = seg.split("-", 2);
                    a = Integer.parseInt(ab[0].trim());
                    b = Integer.parseInt(ab[1].trim());
                } else {
                    a = b = Integer.parseInt(seg);
                }
                if (a > b) { int t = a; a = b; b = t; }
                if (a < 1 || b > total) throw new IllegalArgumentException("页码超出范围 (共 " + total + " 页): " + seg);
                list.add(new int[]{a, b});
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("页码格式错误: " + seg);
            }
        }
        if (list.isEmpty()) throw new IllegalArgumentException("未指定有效页码");
        return list;
    }

    /** 多张图片按顺序合成一个 PDF。fit=true 时统一 A4 并居中缩放；否则页面尺寸等于图片尺寸 */
    public byte[] imagesToPdf(List<? extends MultipartFile> files, Job job, boolean fit) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (MultipartFile f : files) {
                PDImageXObject img;
                try {
                    img = PDImageXObject.createFromByteArray(doc, f.getBytes(), FileUtil.safeName(f.getOriginalFilename()));
                } catch (IOException e) {
                    throw new IllegalArgumentException("无法识别的图片: " + f.getOriginalFilename());
                }
                float iw = img.getWidth(), ih = img.getHeight();
                PDRectangle size = fit ? PDRectangle.A4 : new PDRectangle(iw, ih);
                if (fit && iw > ih) size = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
                PDPage page = new PDPage(size);
                doc.addPage(page);
                float margin = fit ? 20 : 0;
                float scale = Math.min((size.getWidth() - 2 * margin) / iw, (size.getHeight() - 2 * margin) / ih);
                if (!fit) scale = 1;
                float w = iw * scale, h = ih * scale;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(img, (size.getWidth() - w) / 2, (size.getHeight() - h) / 2, w, h);
                }
                job.step(FileUtil.safeName(f.getOriginalFilename()));
            }
            job.phase("正在保存...");
            return toBytes(doc);
        }
    }

    /**
     * PDF 转图片，支持逐页导出（打包 zip）或竖向拼接为长图。
     * 单文件且 longImage=true 时直接返回图片字节；其余情况返回 zip。
     */
    public byte[] pdfToImages(List<? extends MultipartFile> files, Job job,
                              String format, int dpi, boolean longImage, int gap) throws IOException {
        String ext = "jpg".equalsIgnoreCase(format) ? "jpg" : "png";
        int safeDpi = Math.max(72, Math.min(300, dpi));
        int safeGap = Math.max(0, Math.min(100, gap));

        List<PDDocument> docs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        Path dir = FileUtil.tempDir();
        try {
            int totalPages = 0;
            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                PDDocument doc = openOrFail(f);
                docs.add(doc);
                bases.add(name.replaceAll("(?i)\\.pdf$", ""));
                totalPages += doc.getNumberOfPages();
            }
            if (totalPages == 0) throw new IllegalArgumentException("PDF 没有可转换的页面");
            job.setTotal(totalPages);

            Map<String, Path> result = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            int fileIdx = 0;

            for (int d = 0; d < docs.size(); d++) {
                PDDocument doc = docs.get(d);
                String base = bases.get(d);
                int n = doc.getNumberOfPages();
                PDFRenderer renderer = new PDFRenderer(doc);

                if (longImage) {
                    List<BufferedImage> pages = new ArrayList<>(n);
                    int maxW = 0;
                    long totalH = (long) safeGap * Math.max(0, n - 1);
                    for (int p = 0; p < n; p++) {
                        BufferedImage img = renderer.renderImageWithDPI(p, safeDpi, ImageType.RGB);
                        pages.add(img);
                        maxW = Math.max(maxW, img.getWidth());
                        totalH += img.getHeight();
                        job.step(base + " 第 " + (p + 1) + "/" + n + " 页");
                    }
                    if (totalH > 65000 || (long) maxW * totalH > 200_000_000L) {
                        throw new IllegalArgumentException(
                                "长图尺寸过大 (" + maxW + "×" + totalH + ")，请降低 DPI 或取消长图模式");
                    }
                    job.phase("正在拼接长图: " + base);
                    BufferedImage canvas = new BufferedImage(maxW, (int) totalH, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = canvas.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, maxW, (int) totalH);
                    int y = 0;
                    for (BufferedImage img : pages) {
                        g.drawImage(img, (maxW - img.getWidth()) / 2, y, null);
                        y += img.getHeight() + safeGap;
                    }
                    g.dispose();

                    if (docs.size() == 1) {
                        job.phase("正在编码图片...");
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ImageIO.write(canvas, ext, bos);
                        return bos.toByteArray();
                    }
                    Path tmp = dir.resolve((fileIdx++) + "." + ext);
                    ImageIO.write(canvas, ext, tmp.toFile());
                    result.put(FileUtil.unique(used, base + "_long." + ext), tmp);
                } else {
                    int digits = Math.max(2, String.valueOf(n).length());
                    for (int p = 0; p < n; p++) {
                        BufferedImage img = renderer.renderImageWithDPI(p, safeDpi, ImageType.RGB);
                        Path tmp = dir.resolve((fileIdx++) + "." + ext);
                        ImageIO.write(img, ext, tmp.toFile());
                        String entry = base + "_" + String.format("%0" + digits + "d", p + 1) + "." + ext;
                        result.put(FileUtil.unique(used, entry), tmp);
                        job.step(base + " 第 " + (p + 1) + "/" + n + " 页");
                    }
                }
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            for (PDDocument doc : docs) {
                try { doc.close(); } catch (IOException ignored) {}
            }
            FileUtil.deleteQuietly(dir);
        }
    }

    /** 批量给 PDF 添加文字水印（支持中文、透明度、旋转角度、居中或平铺） */
    public byte[] watermark(List<? extends MultipartFile> files, Job job,
                            String text, int fontSize, float opacity, int angle,
                            boolean tile, String colorHex) throws IOException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("请输入水印文字");
        int size = Math.max(10, Math.min(120, fontSize));
        float alpha = Math.max(0.05f, Math.min(1.0f, opacity));
        int deg = Math.max(-90, Math.min(90, angle));
        Color color = parseColor(colorHex);

        List<PDDocument> docs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        Path dir = FileUtil.tempDir();
        try {
            int totalPages = 0;
            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                PDDocument doc = openOrFail(f);
                docs.add(doc);
                bases.add(name.replaceAll("(?i)\\.pdf$", ""));
                totalPages += doc.getNumberOfPages();
            }
            if (totalPages == 0) throw new IllegalArgumentException("PDF 没有页面");
            job.setTotal(totalPages);

            Map<String, Path> result = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            for (int d = 0; d < docs.size(); d++) {
                PDDocument doc = docs.get(d);
                String base = bases.get(d);
                PDFont font = loadWatermarkFont(doc);
                float textWidth = font.getStringWidth(text) / 1000f * size;
                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(alpha);
                gs.setStrokingAlphaConstant(alpha);

                int n = doc.getNumberOfPages();
                for (int p = 0; p < n; p++) {
                    PDPage page = doc.getPage(p);
                    PDRectangle box = page.getMediaBox();
                    float pw = box.getWidth(), ph = box.getHeight();
                    double rad = Math.toRadians(deg);

                    try (PDPageContentStream cs = new PDPageContentStream(
                            doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        cs.setGraphicsStateParameters(gs);
                        cs.setNonStrokingColor(color);
                        cs.setFont(font, size);

                        if (tile) {
                            float stepX = Math.max(textWidth + 80, 180);
                            float stepY = Math.max(size * 3.5f, 130);
                            for (float y = -ph * 0.2f; y < ph * 1.2f; y += stepY) {
                                for (float x = -pw * 0.2f; x < pw * 1.2f; x += stepX) {
                                    cs.beginText();
                                    cs.setTextMatrix(Matrix.getRotateInstance(rad, x, y));
                                    cs.showText(text);
                                    cs.endText();
                                }
                            }
                        } else {
                            float cx = pw / 2f;
                            float cy = ph / 2f;
                            float ox = (float) (-textWidth / 2.0 * Math.cos(rad) + size / 3.0 * Math.sin(rad));
                            float oy = (float) (-textWidth / 2.0 * Math.sin(rad) - size / 3.0 * Math.cos(rad));
                            cs.beginText();
                            cs.setTextMatrix(Matrix.getRotateInstance(rad, cx + ox, cy + oy));
                            cs.showText(text);
                            cs.endText();
                        }
                    }
                    job.step(base + " 第 " + (p + 1) + "/" + n + " 页");
                }

                if (docs.size() == 1) {
                    job.phase("正在保存...");
                    return toBytes(doc);
                }
                Path tmp = dir.resolve(d + ".pdf");
                doc.save(tmp.toFile());
                result.put(FileUtil.unique(used, base + "_watermark.pdf"), tmp);
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            for (PDDocument doc : docs) {
                try { doc.close(); } catch (IOException ignored) {}
            }
            FileUtil.deleteQuietly(dir);
        }
    }

    /** 批量 PDF 加密或解密 */
    public byte[] protect(List<? extends MultipartFile> files, Job job,
                          String mode, String userPwd, String ownerPwd,
                          boolean allowPrint, boolean allowCopy) throws IOException {
        boolean decrypt = "decrypt".equalsIgnoreCase(mode);
        String uPwd = userPwd == null ? "" : userPwd;
        String oPwd = (ownerPwd == null || ownerPwd.isEmpty()) ? uPwd : ownerPwd;
        if (!decrypt && uPwd.isEmpty() && oPwd.isEmpty()) {
            throw new IllegalArgumentException("请至少设置打开密码或权限密码");
        }

        Path dir = FileUtil.tempDir();
        try {
            Map<String, Path> result = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            int idx = 0;

            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                String base = name.replaceAll("(?i)\\.pdf$", "");
                try (PDDocument doc = open(f, decrypt ? uPwd : "")) {
                    if (decrypt) {
                        doc.setAllSecurityToBeRemoved(true);
                    } else {
                        AccessPermission ap = new AccessPermission();
                        ap.setCanPrint(allowPrint);
                        ap.setCanPrintFaithful(allowPrint);
                        ap.setCanExtractContent(allowCopy);
                        ap.setCanExtractForAccessibility(allowCopy);
                        StandardProtectionPolicy spp = new StandardProtectionPolicy(oPwd, uPwd, ap);
                        spp.setEncryptionKeyLength(128);
                        doc.protect(spp);
                    }
                    job.step(name);
                    if (files.size() == 1) {
                        job.phase("正在保存...");
                        return toBytes(doc);
                    }
                    Path tmp = dir.resolve((idx++) + ".pdf");
                    doc.save(tmp.toFile());
                    String suffix = decrypt ? "_decrypted.pdf" : "_encrypted.pdf";
                    result.put(FileUtil.unique(used, base + suffix), tmp);
                } catch (InvalidPasswordException e) {
                    throw new IllegalArgumentException("密码错误，无法打开: " + name);
                } catch (IOException e) {
                    throw new IllegalArgumentException("无法处理 PDF: " + name);
                }
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            FileUtil.deleteQuietly(dir);
        }
    }

    /** 批量 PDF 压缩（重编码并按需缩放内嵌图片，仅在体积变小时替换） */
    public byte[] compress(List<? extends MultipartFile> files, Job job, String level) throws IOException {
        float quality;
        int maxDim;
        switch (level == null ? "medium" : level.toLowerCase()) {
            case "low" -> { quality = 0.82f; maxDim = 2400; }
            case "high" -> { quality = 0.45f; maxDim = 1200; }
            default -> { quality = 0.65f; maxDim = 1600; }
        }

        List<PDDocument> docs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        Path dir = FileUtil.tempDir();
        try {
            int totalPages = 0;
            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                PDDocument doc = openOrFail(f);
                docs.add(doc);
                bases.add(name.replaceAll("(?i)\\.pdf$", ""));
                totalPages += doc.getNumberOfPages();
            }
            if (totalPages == 0) throw new IllegalArgumentException("PDF 没有页面");
            job.setTotal(totalPages);

            Map<String, Path> result = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            for (int d = 0; d < docs.size(); d++) {
                PDDocument doc = docs.get(d);
                String base = bases.get(d);
                Set<COSBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                int n = doc.getNumberOfPages();
                for (int p = 0; p < n; p++) {
                    compressResources(doc, doc.getPage(p).getResources(), quality, maxDim, visited);
                    job.step(base + " 第 " + (p + 1) + "/" + n + " 页");
                }
                if (docs.size() == 1) {
                    job.phase("正在保存...");
                    return toBytes(doc);
                }
                Path tmp = dir.resolve(d + ".pdf");
                doc.save(tmp.toFile());
                result.put(FileUtil.unique(used, base + "_compressed.pdf"), tmp);
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            for (PDDocument doc : docs) {
                try { doc.close(); } catch (IOException ignored) {}
            }
            FileUtil.deleteQuietly(dir);
        }
    }

    private static void compressResources(PDDocument doc, PDResources res,
                                          float quality, int maxDim, Set<COSBase> visited) throws IOException {
        if (res == null) return;
        for (COSName name : res.getXObjectNames()) {
            PDXObject xobj;
            try {
                xobj = res.getXObject(name);
            } catch (IOException e) {
                continue;
            }
            if (xobj == null || !visited.add(xobj.getCOSObject())) continue;
            if (xobj instanceof PDFormXObject form) {
                compressResources(doc, form.getResources(), quality, maxDim, visited);
            } else if (xobj instanceof PDImageXObject img) {
                if (img.getWidth() < 64 && img.getHeight() < 64) continue;
                try {
                    BufferedImage bi = img.getImage();
                    if (bi == null) continue;
                    int w = bi.getWidth(), h = bi.getHeight();
                    int nw = w, nh = h;
                    if (Math.max(w, h) > maxDim) {
                        double scale = (double) maxDim / Math.max(w, h);
                        nw = Math.max(1, (int) Math.round(w * scale));
                        nh = Math.max(1, (int) Math.round(h * scale));
                    }
                    BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgb.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, nw, nh);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(bi, 0, 0, nw, nh, null);
                    g.dispose();
                    PDImageXObject compressed = JPEGFactory.createFromImage(doc, rgb, quality);
                    if (compressed.getCOSObject().getLength() < img.getCOSObject().getLength()) {
                        res.put(name, compressed);
                    }
                } catch (Exception ignored) {
                    // 跳过特殊色彩空间或遮罩图片
                }
            }
        }
    }

    private static PDFont loadWatermarkFont(PDDocument doc) throws IOException {
        String[] candidates = {
            "C:\\Windows\\Fonts\\simhei.ttf",
            "C:\\Windows\\Fonts\\simkai.ttf",
            "C:\\Windows\\Fonts\\simfang.ttf",
            "C:\\Windows\\Fonts\\arial.ttf"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.isFile()) return PDType0Font.load(doc, f);
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private static Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) return new Color(136, 136, 136);
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException e) {
            return new Color(136, 136, 136);
        }
    }

    /**
     * 打开 PDF。磁盘文件直接以随机访问方式读取，并使用临时文件作为流缓存，避免整份 PDF 常驻堆内存。
     */
    private static PDDocument open(MultipartFile f, String password) throws IOException {
        String pwd = password == null ? "" : password;
        if (f instanceof DiskFile df) {
            return Loader.loadPDF(df.path().toFile(), pwd, null, null, IOUtils.createTempFileOnlyStreamCache());
        }
        return Loader.loadPDF(f.getBytes(), pwd);
    }

    /** 打开 PDF，并把失败原因转换成面向用户的提示 */
    private static PDDocument openOrFail(MultipartFile f) {
        String name = FileUtil.safeName(f.getOriginalFilename());
        try {
            return open(f, "");
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("PDF 已加密，请先在“PDF 加密/解密”中解密: " + name);
        } catch (IOException e) {
            throw new IllegalArgumentException("不是有效的 PDF: " + name);
        }
    }

    private static byte[] toBytes(PDDocument doc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.save(bos);
        return bos.toByteArray();
    }
}

