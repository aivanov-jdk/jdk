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
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

public class TestUndoInsertArabicText {

    private static JTextArea textArea;
    private static UndoManager manager;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
//        testEnd();
//        Thread.sleep(1000);
//        testMiddle();
//        Thread.sleep(1000);
        testBeginning();
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            textArea = new JTextArea(2, 20);
            manager = new UndoManager();
            frame = new JFrame("Undo - Redo Error");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JScrollPane scrollPane = new JScrollPane(textArea);

            textArea.getDocument().addUndoableEditListener(manager);

            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
//            frame.setSize(100, 100);
            frame.pack();
            frame.setVisible(true);
        });
    }

    private static void undoAndCheck() throws Exception {

        if (textArea.getText().contains("\u0633")) {
            System.out.println("\u0633 is present");
        }
        SwingUtilities.invokeAndWait(() -> {
            if (manager.canUndo()) {
                manager.undo();
                System.out.println("--- undone 1 ---");
                ((AbstractDocument) textArea.getDocument()).dump(System.out);
                manager.undo();
                System.out.println("--- undone 2 ---");
                ((AbstractDocument) textArea.getDocument()).dump(System.out);
            }
        });
        Thread.sleep(1000);
        if (textArea.getText().contains("\u0633")) {
            throw new RuntimeException("Undo-ed arabic character still present");
        }
    }

    private static void testEnd() throws Exception {
        try {
            createUI();
            Thread.sleep(1000);
            // insert at end of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                textArea.insert("start ", 0);
                textArea.insert("\u0631", textArea.getText().length());
                textArea.insert("\u0632", textArea.getText().length());
                textArea.insert("\u0633", textArea.getText().length());
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            });

            Thread.sleep(1000);
            undoAndCheck();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }

    }

    private static void testBeginning() throws Exception {
        try {
            createUI();
            Thread.sleep(1000);

            // insert at beginning of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                AbstractDocument doc = (AbstractDocument) textArea.getDocument();
                System.out.println("--- empty ---");
                doc.dump(System.out);

                String start = "start ";
                textArea.insert(start, 0);
                String digits = " 123 ";
                //textArea.insert(digits, start.length());
//                textArea.insert(" 678 ", doc.getLength());

                String end = " end";
                textArea.insert(end, doc.getLength());

                textArea.insert("\u0631\u0633", start.length());
                textArea.insert("\u0632", doc.getLength());
                textArea.insert("\u0633", doc.getLength());
//                textArea.insert("\u0632", textArea.getText().length());
//                textArea.setCaretPosition(0);
//                textArea.insert("\u0633", 0);
                System.out.println("--- one char ---");
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            SwingUtilities.invokeAndWait(() -> {
                AbstractDocument doc = (AbstractDocument) textArea.getDocument();
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                System.out.println("--- orientation changed ---");
                doc.dump(System.out);
                System.out.println("--- with digits ---");
                textArea.insert("123", 6 + 1);
                doc.dump(System.out);
            });
            Thread.sleep(2000);
            undoAndCheck();
            Thread.sleep(2000);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }

    }

    private static void testMiddle() throws Exception {
        try {
            createUI();
            Thread.sleep(1000);

            // insert at middle of existing text and undo
            SwingUtilities.invokeAndWait(() -> {
                textArea.insert("\u0631", textArea.getText().length());
                textArea.insert("\u0632", textArea.getText().length());
                textArea.insert("\u0634", textArea.getText().length());
                textArea.insert("\u0635", textArea.getText().length());
                textArea.setCaretPosition(2);
                textArea.insert("\u0633", 2);
                textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            });
            Thread.sleep(1000);
            undoAndCheck();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }

    }
}

