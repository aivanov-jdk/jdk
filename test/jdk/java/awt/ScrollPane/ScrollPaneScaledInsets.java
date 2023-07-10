/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.ScrollPane;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import jtreg.SkippedException;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;

/*
 * @test
 * @bug 8311689
 * @key headful
 * @requires os.family=="windows"
 * @summary Verifies ScrollPane insets are scaled down in High DPI env
 * @library /test/lib
 * @build jtreg.SkippedException
 * @run main ScrollPaneScaledInsets
 */
public final class ScrollPaneScaledInsets {
    private static final Color CANVAS_BACKGROUND = Color.GREEN;
    private static final Color SCROLL_PANE_BACKGROUND = Color.RED;

    private static final Dimension CANVAS_SIZE = new Dimension(900, 600);
    private static final Dimension SCROLL_PANE_SIZE =
            new Dimension(CANVAS_SIZE.width / 3, CANVAS_SIZE.height / 3);

    private static final int SCROLL_OFFSET = 100;
    public static final String SCREENSHOT_FILE_NAME = "scrollPane.png";

    public static void main(String[] args) throws Exception {
        GraphicsConfiguration mainScreen = getLocalGraphicsEnvironment()
                                           .getDefaultScreenDevice()
                                           .getDefaultConfiguration();
        final AffineTransform at = mainScreen.getDefaultTransform();
        if (at.getScaleX() < 1.25f || at.getScaleY() < 1.25f) {
            throw new SkippedException("This test is for High DPI env only");
        }

        Canvas canvas = new Canvas();
        canvas.setBackground(CANVAS_BACKGROUND);
        canvas.setSize(CANVAS_SIZE);

        ScrollPane scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        scrollPane.setBackground(SCROLL_PANE_BACKGROUND);
        scrollPane.add(canvas);
        scrollPane.setSize(SCROLL_PANE_SIZE);

        Frame frame = new Frame("ScrollPaneScrollEnd");
        frame.add(scrollPane, "Center");
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);

        scrollPane.setScrollPosition(CANVAS_SIZE.width + SCROLL_OFFSET,
                                     CANVAS_SIZE.height + SCROLL_OFFSET);

        final Robot robot = new Robot();
        robot.waitForIdle();
        robot.delay(500);

        final BufferedImage screenShot =
                createScreenCapture(robot,
                                    new Rectangle(scrollPane.getLocationOnScreen(),
                                                  SCROLL_PANE_SIZE));

        final List<Error> errors = Stream.of(checkHorizontalCenter(screenShot),
                                             checkVerticalCenter(screenShot))
                                         .filter(Objects::nonNull)
                                         .toList();
        try {
            if (errors.size() > 0) {
                errors.forEach(System.err::println);

                saveImage(screenShot);

                throw new Error(errors.size() + " failure(s) detected: "
                                + errors.get(0).getMessage());
            }
        } finally {
            //frame.dispose();
        }
    }

    private static BufferedImage createScreenCapture(final Robot robot,
                                                     final Rectangle area) {
        MultiResolutionImage screenShot =
                robot.createMultiResolutionScreenCapture(area);
        List<Image> variants = screenShot.getResolutionVariants();
        return (BufferedImage) variants.get(variants.size() - 1);
    }

    private static Error checkHorizontalCenter(final BufferedImage img) {
        return checkCenter(0, img.getHeight() / 2,
                           1, 0,
                           img);
    }

    private static Error checkVerticalCenter(final BufferedImage img) {
        return checkCenter(img.getHeight() / 2,  0,
                           0, 1,
                           img);
    }

    private static Error checkCenter(final int xStart, final int yStart,
                                    final int xStep, final int yStep,
                                    final BufferedImage img) {
        final int width = img.getWidth();
        final int height = img.getHeight();

        int x = xStart;
        int y = yStart;
        do {
            do {
                final int color = img.getRGB(x, y);
                if (color == SCROLL_PANE_BACKGROUND.getRGB()) {
                    return new Error(
                            String.format("Found invalid color at %d, %d: %08x",
                                          x, y, color));
                }
            } while (yStep > 0 && ((y += yStep) < height));
        } while (xStep > 0 && ((x += xStep) < width));

        return null;
    }

    private static void saveImage(BufferedImage img) {
        try {
            ImageIO.write(img,
                          "png",
                          new File(SCREENSHOT_FILE_NAME));
        } catch (IOException e) {
            // Don't propagate the exception
            e.printStackTrace();
        }
    }
}
