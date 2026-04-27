/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4476002
   @summary Tests the color of the bullet in HTML-formatted label
            is the same as the color of text
   @key headful
   @run main/othervm -Dawt.useSystemAAFontSettings=off -Dsun.java2d.dpiaware=true bug4476002
*/

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class bug4476002 extends WindowAdapter {
    private static final String HTML_LABEL =
            "<html><head><style>" +
                    "OL { list-style-type: disc; color: red }" +
                    "</style></head>" +
                    "<body><ol><li>wwww</li></ol></body></html>";

    private JFrame mainFrame;
    private JLabel htmlComponent;

    private final Robot robot;

    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        final bug4476002 testCase = new bug4476002();
        try {
            SwingUtilities.invokeLater(testCase::createUI);
            latch.await();
            testCase.robot.waitForIdle();
            Thread.sleep(500);
            SwingUtilities.invokeAndWait(testCase::runTest);
        } finally {
            SwingUtilities.invokeLater(testCase::closeFrame);
        }
    }

    private bug4476002() throws AWTException {
        robot = new Robot();
    }

    private void createUI() {
        mainFrame = new JFrame("bug4476002");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(this);

        htmlComponent = new JLabel(HTML_LABEL);
        mainFrame.getContentPane().add(htmlComponent);

        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    private void closeFrame() {
        mainFrame.dispose();
    }

    private void runTest() {
        Point p = htmlComponent.getLocationOnScreen();
        Dimension d = htmlComponent.getSize();

        final Color background = htmlComponent.getBackground();

        int x0 = p.x;
        int y = p.y + d.height / 2;

        for (int x = x0; x < x0 + d.width; x++) {
            Color pixel = robot.getPixelColor(x, y);
            if (!(pixel.equals(Color.red) || pixel.equals(background))) {
                throw new RuntimeException("Unexpected color: " + pixel);
            }
        }
    }

    @Override
    public void windowOpened(WindowEvent ev) {
        latch.countDown();
    }
}
