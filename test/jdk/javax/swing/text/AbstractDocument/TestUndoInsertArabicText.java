/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5080391
 * @summary  Verifies if AIOOBE is thrown when we do undo of text
 *           inserted in RTL
 * @run main TestUndoInsertArabicText
 */

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.undo.UndoManager;

public class TestUndoInsertArabicText {

    private static JTextArea textArea;
    private static AbstractDocument doc;
    private static UndoManager manager;
    private static JFrame frame;

    public static void main(String[] args) {
        try {
            testEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("\n");
            testMiddle();

            System.out.println("\n");
            testMiddleRTL();

            System.out.println("\n");
            testMiddlePara();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("\n");
            testComplex();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            textArea = new JTextArea(2, 10);
            textArea.setFont(textArea.getFont().deriveFont(50f));

            manager = new UndoManager();
            doc = (AbstractDocument) textArea.getDocument();
            doc.addUndoableEditListener(manager);

            frame = new JFrame("Undo - Redo Error");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new JScrollPane(textArea),
                                       BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void dispose() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    private static void testEnd() throws Exception {
        try {
            System.out.println("testEnd");
            createUI();
            Thread.sleep(1000);
            // Insert at end of the document and undo
            SwingUtilities.invokeAndWait(() -> {
                System.out.println("--- empty ---");
                doc.dump(System.out);

                textArea.insert("\u0631", textArea.getText().length());
                System.out.println("--- one char ---");
                doc.dump(System.out);

                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                System.out.println("--- orientation changed ---");
                doc.dump(System.out);
            });

            Thread.sleep(1000);
            undoEnd();
        } finally {
            dispose();
        }
    }

    private static void undoEnd() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (manager.canUndo()) {
                manager.undo();
                System.out.println("--- undone ---");
                doc.dump(System.out);
                assert "".equals(textArea.getText())
                       : "After undo the text must be empty";
            }
        });
        Thread.sleep(1000);
    }

    private static void testMiddle() throws Exception {
        try {
            System.out.println("Middle");
            createUI();
            Thread.sleep(1000);

            // insert at beginning of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                String start = "start ";
                textArea.insert(start, 0);
                textArea.insert("?! end", doc.getLength());

                textArea.insert("\u0631\u0632\u0633", start.length());
                System.out.println("--- text ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            SwingUtilities.invokeAndWait(() -> {
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                System.out.println("--- orientation changed ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            undoMiddle();
            Thread.sleep(2000);
        } finally {
            dispose();
        }
    }

    private static void testMiddleRTL() throws Exception {
        try {
            System.out.println("MiddleRTL");
            createUI();
            Thread.sleep(1000);

            // insert at beginning of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

                String start = "start ";
                textArea.insert(start, 0);
                textArea.insert("?! end", doc.getLength());

                textArea.insert("\u0631\u0632\u0633", start.length());
                System.out.println("--- text ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            undoMiddle();
            Thread.sleep(2000);
        } finally {
            dispose();
        }
    }

    private static void undoMiddle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (manager.canUndo()) {
                manager.undo();
                System.out.println("--- undone ---");
                doc.dump(System.out);
            }
        });
    }

    private static void testMiddlePara() throws Exception {
        try {
            System.out.println("testMiddlePara");
            createUI();
            Thread.sleep(1000);

            // insert at beginning of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                String start = "start ";
                textArea.insert(start + "\n", 0);
                textArea.insert("end", doc.getLength());

                textArea.insert("\u0631\u0632\u0633", start.length());
                System.out.println("--- text ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            SwingUtilities.invokeAndWait(() -> {
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                System.out.println("--- orientation changed ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            undoMiddlePara();
            Thread.sleep(2000);
        } finally {
            dispose();
        }
    }

    private static void undoMiddlePara() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (manager.canUndo()) {
                manager.undo();
                System.out.println("--- undone 1 ---");
                doc.dump(System.out);
            }
        });
        Thread.sleep(2000);
        SwingUtilities.invokeAndWait(() -> {
            if (manager.canUndo()) {
                manager.undo();
                System.out.println("--- undone 2 ---");
                doc.dump(System.out);
            }
        });
    }

    private static void testComplex() throws Exception {
        try {
            System.out.println("testComplex");
            createUI();
            Thread.sleep(1000);

            // insert at beginning of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                String start = "start ";
                textArea.insert(start, 0);

                String end = " end ";
                textArea.insert(end, doc.getLength());

                textArea.insert("\u0633", start.length());
                textArea.insert("\u0635", doc.getLength());
                System.out.println("--- text ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            SwingUtilities.invokeAndWait(() -> {
                AbstractDocument doc = (AbstractDocument) textArea.getDocument();
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                System.out.println("--- orientation changed ---");
                doc.dump(System.out);
                System.out.println("--- with digits ---");
                textArea.insert("1", "start ".length() + 1);
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            undoComplex();
            Thread.sleep(2000);
        } finally {
            dispose();
        }
    }

    private static void undoComplex() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (manager.canUndo()) {
                manager.undo();
                System.out.println("--- undone 1 ---");
                doc.dump(System.out);
                manager.undo();
                System.out.println("--- undone 2 ---");
                doc.dump(System.out);
            }
        });
    }
}
