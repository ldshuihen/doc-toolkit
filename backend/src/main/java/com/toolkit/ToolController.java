package com.toolkit;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 所有处理接口均为异步：POST 返回 {jobId}；GET /jobs/{id} 查询进度；GET /jobs/{id}/result 下载结果。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ToolController {

    private final WordToPdfService wordToPdf;
    private final RenameService renameService;
    private final PdfService pdfService;
    private final ImageService imageService;
    private final JobService jobs;
    private final DesktopTray desktopTray;

    public ToolController(WordToPdfService w, RenameService r, PdfService p, ImageService i, JobService j, DesktopTray dt) {
        this.wordToPdf = w;
        this.renameService = r;
        this.pdfService = p;
        this.imageService = i;
        this.jobs = j;
        this.desktopTray = dt;
    }

    @PostMapping("/word2pdf")
    public Map<String, String> word2pdf(@RequestParam("files") List<MultipartFile> files) throws IOException {
        return start("pdf.zip", files, wordToPdf::convert);
    }

    @PostMapping("/rename")
    public Map<String, String> rename(@RequestParam("files") List<MultipartFile> files,
                                      @RequestParam(required = false) String prefix,
                                      @RequestParam(required = false) String suffix,
                                      @RequestParam(required = false) String find,
                                      @RequestParam(required = false) String replace,
                                      @RequestParam(required = false) Integer startNo,
                                      @RequestParam(defaultValue = "3") int digits) throws IOException {
        return start("renamed.zip", files,
                (fs, job) -> renameService.rename(fs, job, prefix, suffix, find, replace, startNo, digits));
    }

    @PostMapping("/merge")
    public Map<String, String> merge(@RequestParam("files") List<MultipartFile> files) throws IOException {
        return start("merged.pdf", files, pdfService::merge);
    }

    @PostMapping("/split")
    public Map<String, String> split(@RequestParam("files") List<MultipartFile> files,
                                     @RequestParam(required = false) String ranges) throws IOException {
        return start("split.zip", files.subList(0, 1), (fs, job) -> pdfService.split(fs.get(0), ranges, job));
    }

    @PostMapping("/img2pdf")
    public Map<String, String> img2pdf(@RequestParam("files") List<MultipartFile> files,
                                       @RequestParam(defaultValue = "true") boolean fit) throws IOException {
        return start("images.pdf", files, (fs, job) -> pdfService.imagesToPdf(fs, job, fit));
    }

    @PostMapping("/pdf2img")
    public Map<String, String> pdf2img(@RequestParam("files") List<MultipartFile> files,
                                       @RequestParam(defaultValue = "png") String format,
                                       @RequestParam(defaultValue = "150") int dpi,
                                       @RequestParam(defaultValue = "false") boolean longImage,
                                       @RequestParam(defaultValue = "0") int gap) throws IOException {
        String ext = "jpg".equalsIgnoreCase(format) ? "jpg" : "png";
        String outName = (files.size() == 1 && longImage) ? ("long." + ext) : "pdf_images.zip";
        return start(outName, files, (fs, job) -> pdfService.pdfToImages(fs, job, ext, dpi, longImage, gap));
    }

    @PostMapping("/watermark")
    public Map<String, String> watermark(@RequestParam("files") List<MultipartFile> files,
                                         @RequestParam String text,
                                         @RequestParam(defaultValue = "36") int fontSize,
                                         @RequestParam(defaultValue = "0.25") float opacity,
                                         @RequestParam(defaultValue = "35") int angle,
                                         @RequestParam(defaultValue = "true") boolean tile,
                                         @RequestParam(defaultValue = "#888888") String color) throws IOException {
        String outName = files.size() == 1 ? "watermarked.pdf" : "watermarked.zip";
        return start(outName, files, (fs, job) -> pdfService.watermark(fs, job, text, fontSize, opacity, angle, tile, color));
    }

    @PostMapping("/pdfsec")
    public Map<String, String> pdfsec(@RequestParam("files") List<MultipartFile> files,
                                      @RequestParam(defaultValue = "encrypt") String mode,
                                      @RequestParam(required = false) String userPwd,
                                      @RequestParam(required = false) String ownerPwd,
                                      @RequestParam(defaultValue = "true") boolean allowPrint,
                                      @RequestParam(defaultValue = "true") boolean allowCopy) throws IOException {
        boolean dec = "decrypt".equalsIgnoreCase(mode);
        String outName = files.size() == 1
                ? (dec ? "decrypted.pdf" : "encrypted.pdf")
                : (dec ? "decrypted.zip" : "encrypted.zip");
        return start(outName, files, (fs, job) -> pdfService.protect(fs, job, mode, userPwd, ownerPwd, allowPrint, allowCopy));
    }

    @PostMapping("/compress")
    public Map<String, String> compress(@RequestParam("files") List<MultipartFile> files,
                                        @RequestParam(defaultValue = "medium") String level) throws IOException {
        String outName = files.size() == 1 ? "compressed.pdf" : "compressed.zip";
        return start(outName, files, (fs, job) -> pdfService.compress(fs, job, level));
    }

    @PostMapping("/imgconv")
    public Map<String, String> imgconv(@RequestParam("files") List<MultipartFile> files,
                                       @RequestParam(defaultValue = "png") String format,
                                       @RequestParam(defaultValue = "85") int quality,
                                       @RequestParam(required = false) Integer maxWidth) throws IOException {
        String ext = ImageService.normalizeFormat(format);
        String outName = files.size() == 1 ? ("converted." + ext) : "converted.zip";
        return start(outName, files, (fs, job) -> imageService.convert(fs, job, ext, quality, maxWidth));
    }

    /** 健康检查：前端顶部栏据此显示服务状态、活跃任务数，以及是否为桌面 EXE 模式 */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "activeJobs", jobs.activeCount(),
                "desktop", Boolean.getBoolean("toolkit.desktop"));
    }

    /** 桌面 EXE 模式下的本机优雅退出接口（仅允许 desktop 模式且来自本机回环地址的请求） */
    @PostMapping("/shutdown")
    public ResponseEntity<Map<String, Object>> shutdown(HttpServletRequest req) {
        if (!Boolean.getBoolean("toolkit.desktop") || !isLocal(req.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前运行模式不支持网页退出"));
        }
        desktopTray.shutdown();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static boolean isLocal(String addr) {
        try {
            return addr != null && InetAddress.getByName(addr).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<Map<String, Object>> progress(@PathVariable String id) {
        Job j = jobs.get(id);
        return j == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(j.toMap());
    }

    @GetMapping("/jobs/{id}/result")
    public ResponseEntity<byte[]> result(@PathVariable String id) {
        Job j = jobs.get(id);
        if (j == null || j.result() == null) return ResponseEntity.notFound().build();
        byte[] data = j.result();
        jobs.remove(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(j.filename).build().toString())
                .body(data);
    }

    /** 把上传文件转存到本任务专属的临时目录（不读入内存），随后异步处理，处理完自动清理目录 */
    private Map<String, String> start(String outName, List<MultipartFile> files, JobService.Task task) throws IOException {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("toolkit-up-");
        List<DiskFile> copies = new ArrayList<>();
        try {
            int i = 0;
            for (MultipartFile f : files) {
                java.nio.file.Path p = dir.resolve((i++) + ".upload");
                f.transferTo(p);
                copies.add(new DiskFile(f.getOriginalFilename(), p));
            }
        } catch (IOException | RuntimeException e) {
            FileUtil.deleteQuietly(dir);
            throw e;
        }
        return Map.of("jobId", jobs.submit(outName, copies, dir, task).id);
    }
}
