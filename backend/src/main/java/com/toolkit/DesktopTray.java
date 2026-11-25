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

    private TrayIcon trayIcon;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!Boolean.getBoolean("toolkit.desktop")) return;
        ConfigurableApplicationContext ctx = event.getApplicationContext();
        int port = (ctx instanceof WebServerApplicationContext web) ? web.getWebServer().getPort() : 8080;
        String url = "http://localhost:" + port;

        SwingUtilities.invokeLater(() -> {
            installTray(ctx, url);
            openBrowser(url);
        });
    }

    private void installTray(ConfigurableApplicationContext ctx, String url) {
        if (!SystemTray.isSupported()) return;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        try {
            // 使用 Swing JPopupMenu 避免 Windows 原生 AWT PopupMenu 在 UTF-8 下的中文乱码问题
            JPopupMenu menu = new JPopupMenu();
            JMenuItem openItem = new JMenuItem("打开文档工具箱 (" + url.replace("http://", "") + ")");
            openItem.addActionListener(e -> openBrowser(url));
            JMenuItem exitItem = new JMenuItem("退出程序");
            exitItem.addActionListener(e -> {
                if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
                new Thread(() -> {
                    int code = SpringApplication.exit(ctx, () -> 0);
                    System.exit(code);
                }, "toolkit-shutdown").start();
            });
            menu.add(openItem);
            menu.addSeparator();
            menu.add(exitItem);

            // 隐藏对话框作为托盘菜单宿主，确保点击菜单外区域时自动收起
            JDialog invoker = new JDialog();
            invoker.setUndecorated(true);
            invoker.setSize(0, 0);

            trayIcon = new TrayIcon(createIcon(), "文档工具箱 (" + url + ")");
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> openBrowser(url));
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) showMenu(e);
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) showMenu(e);
                }
                private void showMenu(MouseEvent e) {
                    invoker.setLocation(e.getX(), e.getY());
                    invoker.setVisible(true);
                    menu.show(invoker, 0, 0);
                    invoker.toFront();
                }
            });
            menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
                @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { invoker.setVisible(false); }
                @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { invoker.setVisible(false); }
            });

            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage("文档工具箱已启动", "双击托盘图标可随时打开 " + url, TrayIcon.MessageType.INFO);
        } catch (Exception ignored) {
            // 托盘不可用时不影响服务运行
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

