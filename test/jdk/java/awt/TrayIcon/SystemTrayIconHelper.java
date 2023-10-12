/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/*
 * @summary This is a helper class to find the location of a system tray icon,
 *          and skip some OS specific cases in tests.
 * @library /lib/client
 * @build ExtendedRobot SystemTrayIconHelper
 */
public class SystemTrayIconHelper {

    static Frame frame;

    private static ExtendedRobot robot;

    private static int imageCounter = 0;
    private static final String fileName = "trayIcon";

    private static class ScreenInfo {
        public final GraphicsDevice device;
        public final GraphicsConfiguration config;
        public final Rectangle bounds;
        public final Insets insets;

        private ScreenInfo() {
            device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                        .getDefaultScreenDevice();
            config = device.getDefaultConfiguration();
            bounds = config.getBounds();
            insets = Toolkit.getDefaultToolkit()
                            .getScreenInsets(config);
        }
    }

    private static final ScreenInfo screenInfo = new ScreenInfo();

    /**
     * Call this method if the tray icon need to be followed in an automated manner
     * This method will be called by automated testcases
     */
    static Point getTrayIconLocation(TrayIcon icon) throws Exception {
        if (icon == null) {
            return null;
        }

        //saveScreenshot();

        showFrameToHideMenus();

        //saveScreenshot();

        try {
            if (System.getProperty("os.name").startsWith("Win")) {
                return getWindowsTrayIconLocation((BufferedImage) icon.getImage());
            } else if (System.getProperty("os.name").startsWith("Mac")) {
                return getMacTrayIconLocation(icon);
            } else {
                return getLinuxTrayIconLocation(icon);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveScreenshot() throws AWTException {
        ExtendedRobot robot = createRobot();
        saveImage(robot.createMultiResolutionScreenCapture(screenInfo.bounds));
    }

    private static ExtendedRobot createRobot() throws AWTException {
        if (robot == null) {
            robot = new ExtendedRobot();
        }
        return robot;
    }

    private static void showFrameToHideMenus() throws Exception {
        //This is added just in case the tray's native menu is visible.
        //It has to be hidden if visible. For that, we are showing a Frame
        //and clicking on it - the assumption is, the menu will
        //be closed if another window is clicked
        final ExtendedRobot robot = createRobot();

        EventQueue.invokeAndWait(() -> {
            frame = new Frame();
            frame.setSize(100, 100);
            frame.setVisible(true);
        });
        robot.waitForIdle();

        final AtomicReference<Rectangle> frameBoundsRef = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            Point location = frame.getLocationOnScreen();
            Dimension size = frame.getSize();
            frameBoundsRef.set(new Rectangle(location, size));
        });
        final Rectangle frameBounds = frameBoundsRef.get();
        robot.mouseMove(frameBounds.x + frameBounds.width / 2,
                        frameBounds.y + frameBounds.height / 2);
        robot.waitForIdle();
        robot.click();
        robot.waitForIdle();
        EventQueue.invokeAndWait(frame::dispose);
    }

    private static Point getWindowsTrayIconLocation(BufferedImage iconImage) {
        // sun.awt.windows.WTrayIconPeer
        int width = iconImage.getWidth();
        int height = iconImage.getHeight();

        // Some previously created icons may not be removed
        // from tray until mouse move on it. So we glide
        // through the whole tray bar.
        robot.glide(screenInfo.bounds.width, screenInfo.bounds.height - screenInfo.insets.bottom / 2,
                    0, screenInfo.bounds.height - screenInfo.insets.bottom / 2,
                    1, 2);

        saveImage(robot.createMultiResolutionScreenCapture(screenInfo.bounds));
        BufferedImage screen = robot.createScreenCapture(screenInfo.bounds);

        for (int x = screenInfo.bounds.width - width; x > 0; x--) {
            for (int y = screenInfo.bounds.height - height;
                 y > (screenInfo.bounds.height - screenInfo.insets.bottom);
                 y--) {
                if (imagesEquals(iconImage, screen.getSubimage(x, y, width, height))) {
                    Point point = new Point(x + 5, y + 5);
                    System.out.println("Icon location: " + point);
                    return point;
                }
            }
        }
        return null;
    }

    private static Point getMacTrayIconLocation(final TrayIcon icon)
            throws Exception {
        Point2D point2d;
        // sun.lwawt.macosx.CTrayIcon
        Field f_peer = getField(java.awt.TrayIcon.class, "peer");
        Method m_addExports = Class.forName("java.awt.Helper")
                                   .getDeclaredMethod("addExports", String.class, java.lang.Module.class);
        m_addExports.invoke(null, "sun.lwawt.macosx", robot.getClass()
                                                                     .getModule());


        Object peer = f_peer.get(icon);
        Class<?> superclass = peer.getClass()
                                  .getSuperclass();
        System.out.println("superclass = " + superclass);
        Field m_getModel = superclass.getDeclaredField("ptr");
        m_getModel.setAccessible(true);
        long model = (Long) m_getModel.get(peer);
        Method m_getLocation = peer.getClass()
                                   .getDeclaredMethod(
                                           "nativeGetIconLocation", new Class[]{Long.TYPE});
        m_getLocation.setAccessible(true);
        point2d = (Point2D) m_getLocation.invoke(peer, new Object[]{model});
        Point po = new Point((int) (point2d.getX()), (int) (point2d.getY()));
        po.translate(10, -5);
        System.out.println("Icon location " + po);
        return po;
    }

    private static Point getLinuxTrayIconLocation(final TrayIcon icon)
            throws Exception {
        // sun.awt.X11.XTrayIconPeer
        Method m_addExports = Class.forName("java.awt.Helper")
                                   .getDeclaredMethod("addExports", String.class, java.lang.Module.class);
        m_addExports.invoke(null, "sun.awt.X11", robot.getClass().getModule());

        Field f_peer = getField(java.awt.TrayIcon.class, "peer");

        SystemTrayIconHelper.openTrayIfNeeded(robot);

        Object peer = f_peer.get(icon);
        Method m_getLOS = peer.getClass().getDeclaredMethod(
                "getLocationOnScreen", new Class[]{});
        m_getLOS.setAccessible(true);
        Point point = (Point)m_getLOS.invoke(peer, new Object[]{});
        point.translate(5, 5);
        System.out.println("Icon location " + point);
        return point;
    }

    private static void saveImage(MultiResolutionImage mri) {
        saveImage(mri, fileName);
    }

    private static void saveImage(MultiResolutionImage mri, String baseFileName) {
        int index = 0;
        for (Image image : mri.getResolutionVariants()) {
            saveImage((BufferedImage) image, baseFileName + "." + index);
            index++;
        }
    }

    private static void saveImage(BufferedImage image, String baseFileName) {
        try {
            ImageIO.write(image, "png",
                          new File(String.format("%02d-%s.png",
                                                 imageCounter++, baseFileName)));
        } catch (IOException ignored) {
        }
    }

    static Field getField(final Class clz, final String fieldName) {
        Field res = null;
        try {
            res = (Field)AccessController.doPrivileged((PrivilegedExceptionAction) () -> {
                Field f = clz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            });
        } catch (PrivilegedActionException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    private static boolean imagesEquals(BufferedImage img1,
                                        BufferedImage img2) {
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    static void doubleClick(Robot robot) {
        if (System.getProperty("os.name").startsWith("Mac")) {
            robot.mousePress(InputEvent.BUTTON3_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON3_MASK);
        } else {
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }
    }

    // Method for skipping some OS specific cases
    static boolean skip(int button) {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")){
            if (button == InputEvent.BUTTON1_MASK){
                // See JDK-6827035
                return true;
            }
        } else if (System.getProperty("os.name").toLowerCase().contains("os x")){
            // See JDK-7153700
            return true;
        }
        return false;
    }

    public static boolean openTrayIfNeeded(Robot robot) {
        String sysv = System.getProperty("os.version");
        System.out.println("System version is " + sysv);
        //Additional step to raise the system tray in Gnome 3 in OEL 7
        if (isOel7orLater()) {
            System.out.println("OEL 7 detected");
            if (screenInfo.insets.bottom > 0) {
                robot.mouseMove(screenInfo.bounds.width - screenInfo.insets.bottom / 2,
                                screenInfo.bounds.height - screenInfo.insets.bottom / 2);
                robot.delay(50);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.delay(50);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                robot.waitForIdle();
                robot.delay(1000);
                System.out.println("Tray is opened");
                return true;
            }
        }
        return false;
    }

    public static boolean isOel7orLater() {
        if (System.getProperty("os.name").toLowerCase().contains("linux") &&
            System.getProperty("os.version").toLowerCase().contains("el")) {
            Pattern p = Pattern.compile("el(\\d+)");
            Matcher m = p.matcher(System.getProperty("os.version"));
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1)) >= 7;
                } catch (NumberFormatException nfe) {}
            }
        }
        return false;
    }
}
