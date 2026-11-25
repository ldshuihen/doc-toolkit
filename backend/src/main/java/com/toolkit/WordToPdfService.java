package com.toolkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 通过 PowerShell 调用本机 Microsoft Word 将 doc/docx 批量转为 PDF（仅 Windows，需安装 Word）。 */
@Service
public class WordToPdfService {

    private static final String SCRIPT = """
        $ErrorActionPreference = 'Stop'
        [Console]::OutputEncoding = [Text.Encoding]::UTF8
        $list = Get-Content -LiteralPath $args[0] -Encoding UTF8
        $word = New-Object -ComObject Word.Application
        $word.Visible = $false
        $word.DisplayAlerts = 0
        try {
            foreach ($line in $list) {
                $p = $line -split '\\|'
                try {
                    $doc = $word.Documents.Open($p[0], $false, $true)
                    $doc.ExportAsFixedFormat($p[1], 17)
                    $doc.Close($false)
                    Write-Output ('OK|' + $p[2])
                } catch {
                    Write-Output ('FAIL|' + $p[2] + '|' + $_.Exception.Message)
                }
            }
        } finally {
            $word.Quit()
        }
        """;

    public byte[] convert(List<? extends MultipartFile> files, Job job) throws IOException, InterruptedException {
        Path dir = FileUtil.tempDir();
        try {
            Path in = Files.createDirectories(dir.resolve("in"));
            Path out = Files.createDirectories(dir.resolve("out"));
            List<String> lines = new ArrayList<>();
            Set<String> used = new HashSet<>();
            StringBuilder failed = new StringBuilder();
            int idx = 0;
            for (MultipartFile f : files) {
                String name = FileUtil.safeName(f.getOriginalFilename());
                String lower = name.toLowerCase();
                if (!lower.endsWith(".doc") && !lower.endsWith(".docx")) {
                    failed.append(name).append(" : 不是 doc/docx 文件\n");
                    job.step(name + "（已跳过）");
                    continue;
                }
                Path src = in.resolve((idx++) + "_" + name.replaceAll("[^A-Za-z0-9._-]", "_"));
                f.transferTo(src);
                String pdfName = FileUtil.unique(used, name.replaceAll("(?i)\\.docx?$", "") + ".pdf");
                lines.add(src.toAbsolutePath() + "|" + out.resolve(idx + ".pdf").toAbsolutePath() + "|" + pdfName);
            }
            Map<String, Path> result = new LinkedHashMap<>();
            if (!lines.isEmpty()) {
                job.phase("正在启动 Word...");
                // 脚本与列表均带 BOM 写入，保证 Windows PowerShell 5 正确识别中文
                Path script = dir.resolve("run.ps1");
                Path list = dir.resolve("list.txt");
                writeBom(script, SCRIPT);
                writeBom(list, String.join("\n", lines));
                Process pr = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-File", script.toString(), list.toString())
                        .redirectErrorStream(true).start();
                // 逐行读取 Word 的输出，每转换完一个文件就更新一次进度
                try (BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        if (l.startsWith("OK|")) {
                            job.step(l.substring(3));
                        } else if (l.startsWith("FAIL|")) {
                            String[] p = l.split("\\|", 3);
                            failed.append(p[1]).append(" : ").append(p.length > 2 ? p[2] : "").append('\n');
                            job.step(p[1] + "（失败）");
                        } else if (!l.isBlank()) {
                            failed.append("[Word] ").append(l).append('\n');
                        }
                    }
                }
                pr.waitFor();
                for (String l : lines) {
                    String[] p = l.split("\\|");
                    Path pdf = Paths.get(p[1]);
                    if (Files.exists(pdf)) result.put(p[2], pdf);
                }
            }
            job.phase("正在打包...");
            return FileUtil.zip(result, failed.toString());
        } finally {
            FileUtil.deleteQuietly(dir);
        }
    }

    private static void writeBom(Path p, String s) throws IOException {
        byte[] body = s.getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[body.length + 3];
        all[0] = (byte) 0xEF; all[1] = (byte) 0xBB; all[2] = (byte) 0xBF;
        System.arraycopy(body, 0, all, 3, body.length);
        Files.write(p, all);
    }
}
