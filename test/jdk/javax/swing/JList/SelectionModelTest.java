import java.util.stream.IntStream;

import javax.swing.DefaultListSelectionModel;

public class SelectionModelTest {
    public static void main(String[] args) {
        final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        //test01(selectionModel); // TODO must be empty
        test02(selectionModel);
        test03(selectionModel);
        test04(selectionModel);
        test05(selectionModel);
        test06(selectionModel);
        test07(selectionModel);
        test08(selectionModel);
        test09(selectionModel);
        //test10(selectionModel); // TODO bitIndex < 0: -2
        //test11(selectionModel); // TODO bitIndex < 0: -2
    }

    private static void test01(DefaultListSelectionModel selectionModel) {
        System.out.println("\n1st");
        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 0, 0, -1);
        //assertTrue(selectionModel.isSelectionEmpty());
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.of(/*0,*/ 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test02(DefaultListSelectionModel selectionModel) {
        System.out.println("\n2nd");
        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        selectionModel.removeIndexInterval(1, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 0, 0, 0);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.of(1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test03(DefaultListSelectionModel selectionModel) {
        System.out.println("\n3rd");
        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        selectionModel.removeIndexInterval(2, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 1, 0, 1);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.of(0, 1)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(2, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test04(DefaultListSelectionModel selectionModel) {
        System.out.println("\n4th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE - 1);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      -1, -1,
                      -1, -1);
        assertTrue(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test05(DefaultListSelectionModel selectionModel) {
        System.out.println("\n5th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE - 1);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      0, 0,
                      -1, 0);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.rangeClosed(1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test06(DefaultListSelectionModel selectionModel) {
        System.out.println("\n6th");
        selectionModel.setSelectionInterval(10, 20);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 10, 20, 10, 20);
        assertTrue(IntStream.rangeClosed(0, 9)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(10, 20)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(21, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));

        selectionModel.removeIndexInterval(0, 10);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 9, -1, 9);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 9)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(10, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test07(DefaultListSelectionModel selectionModel) {
        System.out.println("\n7th");
        selectionModel.setSelectionInterval(0, 0);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 0, 0, 0);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.rangeClosed(1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));

        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.rangeClosed(1, Integer.MAX_VALUE - 2)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        assertTrue(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        selectionModel.removeIndexInterval(0, 0);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE - 3)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 2));
        assertTrue(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        assertFalse(selectionModel.isSelectedIndex(Integer.MAX_VALUE));
    }

    private static void test08(DefaultListSelectionModel selectionModel) {
        System.out.println("\n8th");
        selectionModel.setSelectionInterval(0, 0);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 0, 0, 0);

        selectionModel.insertIndexInterval(0, Integer.MAX_VALUE - 1, true);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE - 1,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE - 1)
                            .allMatch(selectionModel::isSelectedIndex));
        assertFalse(selectionModel.isSelectedIndex(Integer.MAX_VALUE));
    }

    private static void test09(DefaultListSelectionModel selectionModel) {
        System.out.println("\n9th");
        selectionModel.setSelectionInterval(0, 0);
        printSelection(selectionModel);
        assertIndexes(selectionModel, 0, 0, 0, 0);

        selectionModel.insertIndexInterval(0, Integer.MAX_VALUE, true);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));
    }

    private static void test10(DefaultListSelectionModel selectionModel) {
        System.out.println("\n10th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE - 2)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));

        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, true);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));
    }

    private static void test11(DefaultListSelectionModel selectionModel) {
        System.out.println("\n11th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        printSelection(selectionModel);

        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE, true);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        System.out.println(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                                   .allMatch(selectionModel::isSelectedIndex));
    }

    private static void test12(DefaultListSelectionModel selectionModel) {
        System.out.println("\n12th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        printSelection(selectionModel);

        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 2, 2, true);
        printSelection(selectionModel);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        System.out.println(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                                   .allMatch(selectionModel::isSelectedIndex));
    }

    private static void printSelection(final DefaultListSelectionModel sel) {
        System.out.println(Integer.toHexString(sel.getAnchorSelectionIndex())
                           + ", "
                           + Integer.toHexString(sel.getLeadSelectionIndex())
                           + " - "
                           + Integer.toHexString(sel.getMinSelectionIndex())
                           + ", "
                           + Integer.toHexString(sel.getMaxSelectionIndex()));

    }

    public static void assertIndexes(DefaultListSelectionModel sel,
                                     int min, int max,
                                     int anchor, int lead) {
        assertMinMax(sel, min, max);
        assertAnchorLead(sel, anchor, lead);
    }

    public static void assertAnchorLead(DefaultListSelectionModel sel,
                                        int anchor, int lead) {
        assertEquals(anchor, sel.getAnchorSelectionIndex());
        assertEquals(lead, sel.getLeadSelectionIndex());
    }

    public static void assertMinMax(DefaultListSelectionModel sel,
                                    int min, int max) {
        assertEquals(min, sel.getMinSelectionIndex());
        assertEquals(max, sel.getMaxSelectionIndex());
    }

    public static void assertTrue(boolean value) {
        if (!value) {
            throw new RuntimeException("value is false");
        }
    }

    public static void assertFalse(boolean value) {
        if (value) {
            throw new RuntimeException("value is true");
        }
    }

    public static void assertEquals(int expected, int real) {
        if (expected != real) {
            throw new RuntimeException(String.format("Values are different:"
                    + " %1$d (0x%1$08x) vs %2$d (0x%2$08x)", real, expected));
        }
    }
}

