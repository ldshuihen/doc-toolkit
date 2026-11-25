package com.toolkit;

import java.nio.file.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 批量重命名：前缀/后缀/查找替换/序号 */
@Service
public class RenameService {

    public byte[] rename(List<? extends MultipartFile> files, Job job, String prefix, String suffix,
                         String find, String replace, Integer startNo, int digits) throws Exception {
        Path dir = FileUtil.tempDir();
        try {
            Map<String, Path> result = new LinkedHashMap<>();
            Set<String> used = new HashSet<>();
            int no = startNo == null ? 0 : startNo;
            int i = 0;
            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                int dot = name.lastIndexOf('.');
                String base = dot > 0 ? name.substring(0, dot) : name;
                String ext = dot > 0 ? name.substring(dot) : "";
                if (find != null && !find.isEmpty()) base = base.replace(find, replace == null ? "" : replace);
                String seq = startNo == null ? "" : String.format("%0" + Math.max(digits, 1) + "d", no++);
                String newName = (prefix == null ? "" : prefix) + base + (suffix == null ? "" : suffix) + seq + ext;
                newName = newName.replaceAll("[\\\\/:*?\"<>|]", "_");
                Path tmp = dir.resolve((i++) + ".bin");
                f.transferTo(tmp);
                result.put(FileUtil.unique(used, newName), tmp);
                job.step(name + " → " + newName);
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, null);
        } finally {
            FileUtil.deleteQuietly(dir);
        }
    }
}
