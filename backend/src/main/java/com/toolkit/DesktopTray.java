package com.toolkit;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

import javax.swing.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 桌面模式（-Dtoolkit.desktop=true）启动完成后的集成逻辑：
 * 自动在默认浏览器打开应用页面，并在 Windows 右下角常驻系统托盘图标（支持打开页面与优雅退出）。
 */
@Component
public class DesktopTray implements ApplicationListener<ApplicationReadyEvent> {

    private volatile TrayIcon trayIcon;
    private volatile ConfigurableApplicationContext appCtx;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!Boolean.getBoolean("toolkit.desktop")) return;
        appCtx = event.getApplicationContext();
        int port = (appCtx instanceof WebServerApplicationContext web) ? web.getWebServer().getPort() : 8080;
        String url = "http://localhost:" + port;

        SwingUtilities.invokeLater(() -> {
            installTray(url);
            openBrowser(url);
        });
    }

    /** 供系统托盘与网页顶部“退出程序”按钮共同调用的优雅退出方法 */
    public void shutdown() {
        TrayIcon icon = this.trayIcon;
        this.trayIcon = null;
        if (icon != null && SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().remove(icon);
            } catch (Exception ignored) {}
        }
        ConfigurableApplicationContext ctx = this.appCtx;
        Thread t = new Thread(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            int code = ctx != null ? SpringApplication.exit(ctx, () -> 0) : 0;
            System.exit(code);
        }, "toolkit-shutdown");
        t.setDaemon(false);
        t.start();
    }

    private void installTray(String url) {
        if (!SystemTray.isSupported()) return;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        try {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem openItem = new JMenuItem("打开文档工具箱 (" + url.replace("http://", "") + ")");
            openItem.addActionListener(e -> openBrowser(url));
            JMenuItem exitItem = new JMenuItem("退出程序");
            exitItem.addActionListener(e -> shutdown());
            menu.add(openItem);
            menu.addSeparator();
            menu.add(exitItem);

            // 隐藏无边框工具窗作为菜单宿主，避免任务栏多出图标
            JDialog invoker = new JDialog();
            invoker.setUndecorated(true);
            invoker.setType(Window.Type.UTILITY);
            invoker.setAlwaysOnTop(true);
            invoker.setSize(1, 1);

            trayIcon = new TrayIcon(createIcon(), "文档工具箱 (" + url + ")");
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> openBrowser(url));
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) showMenu();
                }
                private void showMenu() {
                    // 使用 MouseInfo 获取经高 DPI 换算后的逻辑坐标，并保证菜单在任务栏上方完整显示
                    Point pt = MouseInfo.getPointerInfo().getLocation();
                    Dimension pref = menu.getPreferredSize();
                    Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    int x = Math.min(pt.x, bounds.x + bounds.width - pref.width - 4);
                    int y = pt.y - pref.height;
                    if (y < bounds.y) y = pt.y;
                    invoker.setLocation(Math.max(bounds.x, x), Math.max(bounds.y, y));
                    invoker.setVisible(true);
                    invoker.toFront();
                    menu.show(invoker, 0, 0);
                }
            });
            menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
                @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { invoker.setVisible(false); }
                @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { invoker.setVisible(false); }
            });

            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage("文档工具箱已启动", "可在网页右上角或托盘右键退出程序", TrayIcon.MessageType.INFO);
        } catch (Exception ignored) {
            // 托盘不可用时仍可通过网页右上角按钮退出
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {}
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (Exception ignored) {}
    }

    /** 绘制 32×32 蓝紫文档托盘图标 */
    private static Image createIcon() {
        int s = 32;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(79, 139, 255), s, s, new Color(106, 76, 245)));
        g.fillRoundRect(0, 0, s, s, 9, 9);
        g.setColor(Color.WHITE);
        g.fillRoundRect(8, 6, 16, 20, 3, 3);
        g.setColor(new Color(47, 111, 237));
        g.fillRect(11, 14, 10, 2);
        g.setColor(new Color(106, 76, 245));
        g.fillRect(11, 19, 7, 2);
        g.dispose();
        return img;
    }
}
