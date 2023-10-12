/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jtreg.SkippedException;

/*
 * @test
 * @key headful
 * @summary Check if a PopupMenu can be displayed when TrayIcon is
 *          right-clicked. It uses a Window as the parent of the PopupMenu
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @library /test/lib
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @build jtreg.SkippedException
 * @run main TrayIconPopupTest
 */

public class TrayIconPopupTest {

    TrayIcon icon;
    ExtendedRobot robot;

    private static final CountDownLatch popupShown = new CountDownLatch(1);
    private static final CountDownLatch actionCompleted = new CountDownLatch(1);

    PopupMenu popup;
    Dialog window;

    public static void main(String[] args) throws Exception {
        if (!SystemTray.isSupported()) {
            throw new SkippedException("SystemTray is not supported on the host");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("win"))
                System.err.println("Test can fail if the icon hides to a tray icons pool " +
                        "in Windows 7, which is behavior by default.\n" +
                        "Set \"Right mouse click\" -> \"Customize notification icons\" -> " +
                        "\"Always show all icons and notifications on the taskbar\" true " +
                        "to avoid this problem. Or change behavior only for Java SE " +
                        "tray icon.");
            new TrayIconPopupTest().doTest();
        }
    }

    TrayIconPopupTest() throws Exception {
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initializeGUI);
        robot.waitForIdle(1000);
        EventQueue.invokeAndWait(() ->  window.setLocation(100, 100));
        robot.waitForIdle(1000);
    }

    private void initializeGUI() {
        window = new Dialog((Frame) null);
        window.setSize(5, 5);
        window.setVisible(true);

        popup = new PopupMenu("");

        MenuItem item = new MenuItem("Sample");
        item.addActionListener(event -> actionCompleted.countDown());
        popup.add(item);
        popup.add(new MenuItem("Item2"));
        popup.add(new MenuItem("Item3"));

        window.add(popup);

        SystemTray tray = SystemTray.getSystemTray();
        Dimension iconSize = tray.getTrayIconSize();
        System.out.println("Icon size: " + iconSize);
        icon = new TrayIcon(createIcon(iconSize), "Sample Icon");
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                System.out.println("mousePressed " + event);
                showPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                System.out.println("mouseReleased " + event);
                showPopup(event);
            }

            private void showPopup(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    System.out.println("popup trigger - show menu");
                    popup.show(window, 0, 0);
                }
            }
        });

        try {
            tray.add(icon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage createIcon(Dimension size) {
        final BufferedImage image = new BufferedImage(size.width, size.height,
                                                      BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(new Color(0xF5BFF5));
            g2d.fillRect(0, 0, size.width, size.height);
        } finally {
            g2d.dispose();
        }
        return image;
    }

    void doTest() throws Exception {
        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null) {
            throw new RuntimeException("Unable to find the icon location!");
        }

        System.out.println("iconPosition: " + iconPosition);
        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        System.out.println("mousePos: " + mousePos);
        System.out.println("press / release mouse on the icon");
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        robot.delay(6000);

        if (!popupShown.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Tray icon popup menu isn't shown");
        }

        System.out.println("now move the mouse and click menu item");
        robot.mouseMove(window.getLocation().x + 10, window.getLocation().y + 10);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

        if (!actionCompleted.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("FAIL: ActionEvent not triggered when " +
                                       "PopupMenu shown and menu item clicked");
        }
    }
}
