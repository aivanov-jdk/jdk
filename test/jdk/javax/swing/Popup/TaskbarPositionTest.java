/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

// TODO remove the second asterisk so that IDEA doesn't complain about jtreg tags
// TODO not being valid javadoc tags
/*
 * @test
 * @bug 4245587 4474813 4425878 4767478 8015599
 * @key headful
 * @summary Tests the location of the heavy weight popup portion of JComboBox,
 * JMenu and JPopupMenu.
 * @library ../regtesthelpers
 * @build Util
 * @run main TaskbarPositionTest
 */
public class TaskbarPositionTest implements ActionListener {

    private static TaskbarPositionTest test;
    private static JFrame frame;
    private static JPopupMenu popupMenu;
    private static JPanel panel;
    private static JComboBox<String> combo1;
    private static JComboBox<String> combo2;
    private static JMenuBar menubar;
    private static JMenu menu1;
    private static JMenu menu2;
    private static JMenu submenu;
    private static Rectangle fullScreenBounds;
    // The usable desktop space: screen size - screen insets.
    private static Rectangle screenBounds;

    // TODO Add final modifier to numData, dayData, mnDayData
    private static final String[] numData = {
        "One", "Two", "Three", "Four", "Five", "Six", "Seven"
    };
    private static final String[] dayData = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private static final char[] mnDayData = {
        'M', 'T', 'W', 'R', 'F', 'S', 'U'
    };

    public TaskbarPositionTest() {
        frame = new JFrame("Use CTRL-down to show a JPopupMenu");
        frame.setContentPane(panel = createContentPane());
        frame.setJMenuBar(createMenuBar());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // CTRL-down will show the popup.
        panel.getInputMap().put(KeyStroke.getKeyStroke(
                KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "OPEN_POPUP");
        panel.getActionMap().put("OPEN_POPUP", new PopupHandler());

        frame.pack();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        fullScreenBounds = new Rectangle(new Point(), toolkit.getScreenSize());
        screenBounds = new Rectangle(new Point(), toolkit.getScreenSize());


        // Reduce the screen bounds by the insets.
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        if (gc != null) {
            Insets screenInsets = toolkit.getScreenInsets(gc);
            screenBounds = gc.getBounds();
            screenBounds.width -= (screenInsets.left + screenInsets.right);
            screenBounds.height -= (screenInsets.top + screenInsets.bottom);
            screenBounds.x += screenInsets.left;
            screenBounds.y += screenInsets.top;
        }

        // Place the frame near the bottom.
        // TODO x parameter should be screenBounds.x
        frame.setLocation(screenBounds.x, screenBounds.y + screenBounds.height - frame.getHeight());
        frame.setVisible(true);
    }

    public static class ComboPopupCheckListener implements PopupMenuListener {

        public void popupMenuCanceled(PopupMenuEvent ev) {
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
            // TODO Add generics <?> to avoid the warning
            JComboBox combo = (JComboBox)ev.getSource();
            if (combo != null) {
                Point cpos = combo.getLocation();
                SwingUtilities.convertPointToScreen(cpos, panel);

                JPopupMenu pm = (JPopupMenu) combo.getUI().getAccessibleChild(combo, 0);

                if (pm != null) {
                    Point p = pm.getLocation();
                    SwingUtilities.convertPointToScreen(p, pm);

				   boolean isPopupOutOfScreen = (p.x < 0 || p.y < 0) ? true : false ;//Popup should be within screen
				   boolean isComboOutOfScreen = (cpos.x < 0)? true : false;//When Frame moved to -ve postion popup should be moved.
                   if(isPopupOutOfScreen || (isComboOutOfScreen &&  p.y+1 < cpos.y )) {
                        System.out.println("p.x "+ p.x+ " cpos.x "+ cpos.x+" p.y " + p.y + " cpos.y " + cpos.y);
                        throw new RuntimeException("ComboBox popup is wrongly aligned");
                    } // check that popup was opened down
                }
            }
        }
    }

    // TODO Resolve the warnings for PopupHandler and PopupListener

    // TODO I suggest adding @Override annotations to the overridden methods

    private class PopupHandler extends AbstractAction {

        @Override public void actionPerformed(ActionEvent e) {
            if (!popupMenu.isVisible()) {
                popupMenu.show((Component) e.getSource(), 40, 40);
            }
            isPopupOnScreen(popupMenu, fullScreenBounds);
        }
    }

    // TODO Make it private
    class PopupListener extends MouseAdapter {

        private JPopupMenu popup;

        public PopupListener(JPopupMenu popup) {
            this.popup = popup;
        }

        @Override public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
                isPopupOnScreen(popup, fullScreenBounds);
            }
        }
    }

    /**
     * Tests if the popup is on the screen.
     */
    public static void isPopupOnScreen(JPopupMenu popup, Rectangle checkBounds) {
        Dimension dim = popup.getSize();
        Point pt = new Point();
        SwingUtilities.convertPointToScreen(pt, popup);
        Rectangle bounds = new Rectangle(pt, dim);

        if (!SwingUtilities.isRectangleContainingRectangle(checkBounds, bounds)) {
            throw new RuntimeException("We do not match! " + checkBounds + " / " + bounds);
        }

    }

    private JPanel createContentPane() {
        JPanel panel = new JPanel();

        combo1 = new JComboBox<>(numData);
        panel.add(combo1);
        combo2 = new JComboBox<>(dayData);
        combo2.setEditable(true);
        panel.add(combo2);
        panel.setSize(300, 200);
        combo1.addPopupMenuListener(new ComboPopupCheckListener());
        combo2.addPopupMenuListener(new ComboPopupCheckListener());
        popupMenu = new JPopupMenu();
        JMenuItem item;
        for (int i = 0; i < dayData.length; i++) {
            item = popupMenu.add(new JMenuItem(dayData[i], mnDayData[i]));
            item.addActionListener(this);
        }
        panel.addMouseListener(new PopupListener(popupMenu));

        JTextField field = new JTextField("CTRL+down for Popup");
        // CTRL-down will show the popup.
        field.getInputMap().put(KeyStroke.getKeyStroke(
                KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "OPEN_POPUP");
        field.getActionMap().put("OPEN_POPUP", new PopupHandler());

        panel.add(field);

        return panel;
    }

    private JMenuBar createMenuBar() {
        // TODO str and bFlag have the only value, remove the parameters, use the values directly
        menubar = new JMenuBar();

        menu1 = new JMenu("1 - First Menu");
        menu1.setMnemonic('1');

        menubar.add(menu1);
        for (int i = 0; i < 8; i++) {
            JMenuItem menuitem = new JMenuItem("1 JMenuItem" + i);
            menu1.add(menuitem);
        }
        // second menu
        menu2 = new JMenu("2 - Second Menu");
        menu2.setMnemonic('2');

        menubar.add(menu2);
        for (int i = 0; i < 5; i++) {
            JMenuItem menuitem = new JMenuItem("2 JMenuItem" + i);
            menu2.add(menuitem);
        }
        submenu = new JMenu("Sub Menu");
        submenu.setMnemonic('S');
        for (int i = 0; i < 5; i++) {
            JMenuItem menuitem = new JMenuItem("S JMenuItem" + i);
            submenu.add(menuitem);
        }
        menu2.add(new JSeparator());
        menu2.add(submenu);

        return menubar;
    }

    public void actionPerformed(ActionEvent evt) {
        Object obj = evt.getSource();
        if (obj instanceof JMenuItem) {
            // put the focus on the noneditable combo.
            combo1.requestFocus();
        }
    }

    public static void main(String[] args) throws Throwable {

        try {
            // Use Robot to automate the test
            Robot robot;
            robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test = new TaskbarPositionTest();
                }
            });

            robot.waitForIdle();
            robot.delay(1000);

            // 1 - menu
            Util.hitMnemonics(robot, KeyEvent.VK_1);

            robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> isPopupOnScreen(menu1.getPopupMenu(), screenBounds));

            // 2 menu with sub menu
            robot.keyPress(KeyEvent.VK_RIGHT);
            robot.keyRelease(KeyEvent.VK_RIGHT);
            // Open the submenu
            robot.keyPress(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_S);

            robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> isPopupOnScreen(menu2.getPopupMenu(), screenBounds));
            SwingUtilities.invokeAndWait(() -> isPopupOnScreen(submenu.getPopupMenu(), screenBounds));

            // Hit Enter to perform the action of
            // a selected menu item in the submenu
            // which requests focus on combo1, non-editable combo box
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);

            robot.waitForIdle();

            // Focus should go to combo1
            // Open combo1 popup
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);

			if(!combo1.isPopupVisible())
			{
				throw new RuntimeException("ComboBox1 popup not visible");
			}
            hidePopup(robot);

            // Move focus to combo2, editable combo box
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);

            robot.waitForIdle();

            // Open combo2 popup
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);

			if(!combo2.isPopupVisible())
			{
				throw new RuntimeException("ComboBox2 popup not visible");
			}
            hidePopup(robot);

            // Move focus to the text field
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);

            // Open its popup
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            // Ensure popupMenu is shown within screen bounds
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> isPopupOnScreen(popupMenu, screenBounds));

            hidePopup(robot);

            // Popup from a mouse click
            Point pt = new Point(4, 4);
            SwingUtilities.convertPointToScreen(pt, panel);
            robot.mouseMove(pt.x, pt.y);
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

            // Ensure popupMenu is shown within screen bounds
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(() -> isPopupOnScreen(popupMenu, screenBounds));

            hidePopup(robot);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setLocation(-30, 100);
                    combo1.requestFocus();
                }
            });

            robot.waitForIdle();

            // Open combo1 popup again
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);

            robot.waitForIdle();

			if(!combo1.isPopupVisible())
			{
				throw new RuntimeException("ComboBox1 popup not visible Post movement");
			}
            hidePopup(robot);
            robot.waitForIdle();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    private static void hidePopup(Robot robot) {
        robot.keyPress(KeyEvent.VK_ESCAPE);
        robot.keyRelease(KeyEvent.VK_ESCAPE);
    }
}