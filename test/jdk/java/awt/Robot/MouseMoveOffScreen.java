/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;

/*
 * @test
 * @bug 8345538
 * @summary Tests mouseMove clamping to screen bounds when set to move offscreen
 * @key headful
 * @run main MouseMoveOffScreen
 */

public class MouseMoveOffScreen {
    private static final Point OFF_SCREEN_LOC = new Point(20000, 200);

    private static void moveMouseAndCheck(final Robot robot,
                                          final int x, final int y) {
        System.out.println(x + ", " + y);
        robot.mouseMove(x, y);
        robot.delay(1000);

        PointerInfo cursor = MouseInfo.getPointerInfo();
        if (cursor != null) {
            System.out.println(cursor.getLocation() + " - "
                               + cursor.getDevice().getIDstring());
        }
    }

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();

        GraphicsDevice[] screens = getLocalGraphicsEnvironment()
                                   .getScreenDevices();
        for (GraphicsDevice screen : screens) {
            GraphicsConfiguration gc = screen.getDefaultConfiguration();
            System.out.println(screen.getIDstring());
            System.out.println("\t" + gc.getBounds());
            System.out.println("\t" + gc.getDefaultTransform());
        }
        System.out.println("\n");

        for (GraphicsDevice screen : screens) {
            System.out.println(screen.getIDstring());
            Rectangle bounds = screen.getDefaultConfiguration()
                                     .getBounds();

            moveMouseAndCheck(robot, bounds.x, bounds.y);
            moveMouseAndCheck(robot, bounds.x + bounds.width - 1, bounds.y);
            moveMouseAndCheck(robot, bounds.x, bounds.y + bounds.height - 1);
            moveMouseAndCheck(robot, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
            moveMouseAndCheck(robot, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        }

        robot.mouseMove(200, 200);
        robot.delay(500);
        robot.mouseMove(OFF_SCREEN_LOC.x, OFF_SCREEN_LOC.y);
        robot.delay(500);

        if (MouseInfo.getPointerInfo().getLocation() == null) {
            throw new RuntimeException("Test Failed, getLocation returned null.");
        }
        Point currLoc = MouseInfo.getPointerInfo().getLocation();
        System.out.println("Current mouse location: " + currLoc);
        if (currLoc.equals(OFF_SCREEN_LOC)) {
            throw new RuntimeException("Test Failed, robot moved mouse off screen.");
        }
    }
}
