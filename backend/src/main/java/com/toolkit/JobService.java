package com.toolkit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

/** 异步任务调度：上传的文件先落到磁盘临时目录，再由后台线程处理，前端轮询进度 */
@Service
public class JobService {

    @FunctionalInterface
    public interface Task {
        byte[] run(List<DiskFile> files, Job job) throws Exception;
    }

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    // Word COM 不适合并发，统一用少量线程
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    /** @param uploadDir 本任务的上传临时目录，任务结束（无论成功失败）后会被删除 */
    public Job submit(String filename, List<DiskFile> files, Path uploadDir, Task task) {
        purgeOld();
        Job job = new Job(UUID.randomUUID().toString(), filename);
        job.setTotal(files.size());
        jobs.put(job.id, job);
        pool.submit(() -> {
            try {
                job.finish(task.run(files, job));
            } catch (IllegalArgumentException e) {
                job.fail(e.getMessage());
            } catch (OutOfMemoryError e) {
                job.fail("内存不足，请减少文件数量、降低 DPI 或取消长图模式后重试");
            } catch (Exception e) {
                job.fail("处理失败: " + e.getMessage());
            } finally {
                FileUtil.deleteQuietly(uploadDir);
            }
        });
        return job;
    }

    public Job get(String id) { return jobs.get(id); }

    public void remove(String id) { jobs.remove(id); }

    /** 正在处理中的任务数（供健康检查展示） */
    public long activeCount() { return jobs.values().stream().filter(Job::isRunning).count(); }

    private void purgeOld() {
        long limit = System.currentTimeMillis() - 30 * 60 * 1000L;
        jobs.values().removeIf(j -> j.created < limit);
    }
}
