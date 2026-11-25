package com.toolkit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 批量图片格式转换（支持 PNG / JPG / BMP / GIF，可选质量与最大宽度缩放） */
@Service
public class ImageService {

    public byte[] convert(List<? extends MultipartFile> files, Job job,
                          String format, int quality, Integer maxWidth) throws IOException {
        String ext = normalizeFormat(format);
        float q = Math.max(0.1f, Math.min(1.0f, quality / 100f));
        int limitW = (maxWidth != null && maxWidth > 0) ? maxWidth : 0;

        Path dir = FileUtil.tempDir();
        try {
            Map<String, Path> result = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            int idx = 0;

            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                BufferedImage src = ImageIO.read(new ByteArrayInputStream(f.getBytes()));
                if (src == null) throw new IllegalArgumentException("无法识别的图片: " + name);

                BufferedImage processed = resizeIfNeeded(src, limitW);
                if ("jpg".equals(ext) || "bmp".equals(ext)) {
                    processed = toOpaqueRgb(processed);
                }

                byte[] bytes = encode(processed, ext, q);
                String base = name.replaceFirst("\\.[^.]+$", "");
                job.step(name + " → " + base + "." + ext);

                if (files.size() == 1) {
                    return bytes;
                }
                Path tmp = dir.resolve((idx++) + "." + ext);
                Files.write(tmp, bytes);
                result.put(FileUtil.unique(used, base + "." + ext), tmp);
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            FileUtil.deleteQuietly(dir);
        }
    }

    static String normalizeFormat(String format) {
        if (format == null) return "png";
        return switch (format.toLowerCase()) {
            case "jpg", "jpeg" -> "jpg";
            case "bmp" -> "bmp";
            case "gif" -> "gif";
            default -> "png";
        };
    }

    private static BufferedImage resizeIfNeeded(BufferedImage src, int maxWidth) {
        if (maxWidth <= 0 || src.getWidth() <= maxWidth) return src;
        int w = maxWidth;
        int h = Math.max(1, (int) Math.round(src.getHeight() * (double) maxWidth / src.getWidth()));
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(w, h, type);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage toOpaqueRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static byte[] encode(BufferedImage img, String ext, float quality) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if ("jpg".equals(ext)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) throw new IOException("缺少 JPEG 编码器");
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                }
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(img, ext, bos);
        }
        return bos.toByteArray();
    }
}

