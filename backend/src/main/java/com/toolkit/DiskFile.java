package com.toolkit;

import java.io.*;
import java.nio.file.*;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 落在磁盘临时目录上的上传文件。
 * 与旧版把所有文件整个读进内存不同，这里只保存路径，真正需要时才按文件读取，
 * 任务结束后由 {@link JobService} 统一删除临时目录。
 */
public class DiskFile implements MultipartFile {
    private final String name;
    private final Path path;

    public DiskFile(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    public Path path() { return path; }

    @Override public String getName() { return "files"; }
    @Override public String getOriginalFilename() { return name; }
    @Override public String getContentType() { return null; }
    @Override public boolean isEmpty() { return getSize() == 0; }
    @Override public long getSize() {
        try { return Files.size(path); } catch (IOException e) { return 0; }
    }
    @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
    @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
    @Override public Resource getResource() { return new FileSystemResource(path); }
    @Override public void transferTo(File dest) throws IOException {
        Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    @Override public void transferTo(Path dest) throws IOException {
        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
    }
}

