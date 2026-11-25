package com.toolkit;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ToolKitApplication {

    public static void main(String[] args) {
        boolean desktop = Boolean.getBoolean("toolkit.desktop");
        SpringApplicationBuilder builder = new SpringApplicationBuilder(ToolKitApplication.class);
        if (desktop) {
            // 桌面模式启用 AWT/Swing（用于系统托盘）并自动寻找空闲端口
            builder.headless(false);
            Map<String, Object> props = new HashMap<>();
            props.put("server.port", findAvailablePort(8080, 8100));
            builder.properties(props);
        }
        builder.run(args);
    }

    /** 优先使用 8080，被占用则顺延寻找空闲端口；均被占用时回退为随机端口 */
    static int findAvailablePort(int start, int end) {
        for (int p = start; p <= end; p++) {
            try (ServerSocket ignored = new ServerSocket(p)) {
                return p;
            } catch (Exception ignored) {
                // 端口已被占用，继续尝试下一个
            }
        }
        return 0;
    }
}
