/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.util.List;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.awt.Toolkit.getDefaultToolkit;

public  final class WindowLayouts {
    private WindowLayouts() {
    }

    private static final int WINDOW_GAP = PassFailJFrame.WINDOW_GAP;

    /**
     * {@return the center point of the main screen}
     */
    public static Point getScreenCenter() {
        GraphicsConfiguration gc = getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        Dimension size = gc.getBounds()
                           .getSize();
        Insets insets = getDefaultToolkit()
                .getScreenInsets(gc);

        return new Point((size.width - insets.left - insets.right) / 2,
                         (size.height - insets.top - insets.bottom) / 2);
    }

    public static void rightOneRow(List<? extends Window> windows,
                                   PassFailJFrame.InstructionUI instructionUI) {
        final int y = instructionUI.getLocation().y;

        int x = instructionUI.getLocation().x
                + instructionUI.getSize().width
                + WINDOW_GAP;
        for (Window w : windows) {
            w.setLocation(x, y);
            x += w.getWidth() + WINDOW_GAP;
        }
    }

    public static void rightOneColumn(List<? extends Window> windows,
                                      PassFailJFrame.InstructionUI instructionUI) {
        final int x = instructionUI.getLocation().x
                      + instructionUI.getSize().width
                      + WINDOW_GAP;

        int y = instructionUI.getLocation().y;
        for (Window w : windows) {
            w.setLocation(x, y);
            y += w.getHeight() + WINDOW_GAP;
        }
    }

    public static void rightOneColumnCentered(List<? extends Window> windows,
                                              PassFailJFrame.InstructionUI instructionUI) {
        final int x = instructionUI.getLocation().x
                      + instructionUI.getSize().width
                      + WINDOW_GAP;

        int y = getScreenCenter().y
                - getWindowListHeight(windows) / 2;
        for (Window w : windows) {
            w.setLocation(x, y);
            y += w.getHeight() + WINDOW_GAP;
        }
    }

    public static void bottomOneRow(List<? extends Window> windows,
                                    PassFailJFrame.InstructionUI instructionUI) {
        final int y = instructionUI.getLocation().y
                      + instructionUI.getSize().height
                      + WINDOW_GAP;

        int x = instructionUI.getLocation().x;
        for (Window w : windows) {
            w.setLocation(x, y);
            x += w.getWidth() + WINDOW_GAP;
        }
    }

    public static void bottomOneRowCentered(List<? extends Window> windows,
                                            PassFailJFrame.InstructionUI instructionUI) {
        final int y = instructionUI.getLocation().y
                      + instructionUI.getSize().height
                      + WINDOW_GAP;

        int x = getScreenCenter().x
                - getWindowListWidth(windows) / 2;
        for (Window w : windows) {
            w.setLocation(x, y);
            x += w.getWidth() + WINDOW_GAP;
        }
    }

    public static void bottomOneColumn(List<? extends Window> windows,
                                       PassFailJFrame.InstructionUI instructionUI) {
        final int x = instructionUI.getLocation().x;

        int y = instructionUI.getLocation().y
                + instructionUI.getSize().height
                + WINDOW_GAP;
        for (Window w : windows) {
            w.setLocation(x, y);
            y += w.getHeight() + WINDOW_GAP;
        }
    }

    public static int getWindowListWidth(List<? extends Window> windows) {
        return windows.stream()
                      .mapToInt(Component::getWidth)
                      .sum()
               + WINDOW_GAP * (windows.size() - 1);
    }

    public static int getWindowListHeight(List<? extends Window> windows) {
        return windows.stream()
                      .mapToInt(Component::getHeight)
                      .sum()
               + WINDOW_GAP * (windows.size() - 1);
    }
}
