package com.toolkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

/** 工具方法：临时目录、打包 zip */
final class FileUtil {
    private FileUtil() {}

    static Path tempDir() throws IOException {
        return Files.createTempDirectory("toolkit-");
    }

    /** 取安全文件名（去掉路径部分） */
    static String safeName(String n) {
        if (n == null || n.isBlank()) return "file";
        return Paths.get(n.replace('\\', '/')).getFileName().toString();
    }

    static byte[] zip(Map<String, Path> entries, String extraNote) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            for (var e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                Files.copy(e.getValue(), zos);
                zos.closeEntry();
            }
            if (extraNote != null && !extraNote.isBlank()) {
                zos.putNextEntry(new ZipEntry("_failed.txt"));
                zos.write(extraNote.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    static void deleteQuietly(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {}
    }

    /** 文件名去重：a.pdf, a(1).pdf ... */
    static String unique(Set<String> used, String name) {
        String candidate = name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 1;
        while (!used.add(candidate.toLowerCase())) candidate = base + "(" + (i++) + ")" + ext;
        return candidate;
    }
}
