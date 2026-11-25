package com.toolkit;

import java.util.LinkedHashMap;
import java.util.Map;

/** 一个异步处理任务及其进度 */
public class Job {
    public final String id;
    public final String filename;
    public final long created = System.currentTimeMillis();
    private volatile String status = "RUNNING"; // RUNNING / DONE / FAILED
    private volatile int total;
    private volatile int done;
    private volatile String current = "";
    private volatile String error;
    private volatile byte[] result;

    public Job(String id, String filename) {
        this.id = id;
        this.filename = filename;
    }

    public void setTotal(int total) { this.total = total; }

    /** 完成了一个条目 */
    public synchronized void step(String name) {
        done++;
        current = name;
    }

    public void phase(String text) { current = text; }

    void finish(byte[] data) {
        done = total;
        result = data;
        status = "DONE";
    }

    void fail(String msg) {
        error = msg;
        status = "FAILED";
    }

    public byte[] result() { return result; }
    public void clearResult() { result = null; }
    public boolean isRunning() { return "RUNNING".equals(status); }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("total", total);
        m.put("done", done);
        m.put("current", current);
        m.put("error", error);
        return m;
    }
}

