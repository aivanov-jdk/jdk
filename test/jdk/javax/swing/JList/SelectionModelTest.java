import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.swing.DefaultListSelectionModel;

public class SelectionModelTest {

    public static void main(String[] args) {
        Runnable[] tests = {
                SelectionModelTest::test01,
                SelectionModelTest::test02,
                SelectionModelTest::test03,
                SelectionModelTest::test04,
                SelectionModelTest::test05,
                SelectionModelTest::test06,
                SelectionModelTest::test07,
                SelectionModelTest::test08,
                SelectionModelTest::test09,
                SelectionModelTest::test10,
                SelectionModelTest::test11,
                SelectionModelTest::test12,
                SelectionModelTest::test13,
                SelectionModelTest::test14,
                SelectionModelTest::test15,
                SelectionModelTest::test16,
                SelectionModelTest::test17,
                SelectionModelTest::test18,
                SelectionModelTest::test19,
                SelectionModelTest::test20,
        };
        Collection<Exception> errors =
                Arrays.stream(tests)
                      .parallel()
                      .map(SelectionModelTest::runTest)
                      .filter(Objects::nonNull)
                      .toList();
        for (Exception error : errors) {
            error.printStackTrace();
            System.err.println();
        }
        if (errors.size() > 0) {
            throw new RuntimeException(errors.size() + " test(s) failed");
        }
    }

    private static void test01() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);

        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, 0, 0, -1);
        //assertTrue(selectionModel.isSelectionEmpty());
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.of(/*0,*/ 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test02() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);

        selectionModel.removeIndexInterval(1, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, 0, 0, 0);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.of(1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test03() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);

        selectionModel.removeIndexInterval(2, Integer.MAX_VALUE);
        assertIndexes(selectionModel, 0, 1, 0, 1);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.of(0, 1)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(2, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test04() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);

        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE - 1);
        assertIndexes(selectionModel,
                      -1, -1,
                      -1, -1);
        assertTrue(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test05() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 2, Integer.MAX_VALUE);

        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE - 1);
        assertIndexes(selectionModel,
                      0, 0,
                      -1, 0);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.rangeClosed(1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test06() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(10, 20);
        assertIndexes(selectionModel, 10, 20, 10, 20);
        assertTrue(IntStream.rangeClosed(0, 9)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(10, 20)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(21, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));

        selectionModel.removeIndexInterval(0, 10);
        assertIndexes(selectionModel, 0, 9, -1, 9);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 9)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(10, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test07() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(0, 0);
        assertIndexes(selectionModel, 0, 0, 0, 0);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(selectionModel.isSelectedIndex(0));
        assertTrue(IntStream.rangeClosed(1, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));

        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
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

    private static void test08() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(0, 0);
        assertIndexes(selectionModel, 0, 0, 0, 0);

        selectionModel.insertIndexInterval(0, Integer.MAX_VALUE - 1, true);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE - 1,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE - 1)
                            .allMatch(selectionModel::isSelectedIndex));
        assertFalse(selectionModel.isSelectedIndex(Integer.MAX_VALUE));
    }

    private static void test09() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(0, 0);
        assertIndexes(selectionModel, 0, 0, 0, 0);

        selectionModel.insertIndexInterval(0, Integer.MAX_VALUE, true);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));
    }

    private static void test10() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE - 2)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));

        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, true);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      -3, -2);
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE - 2)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));
    }

    private static void test11() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE, true);
        assertIndexes(selectionModel,
                      Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                      -3, -2);
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test12() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 2, 2, true);
        assertIndexes(selectionModel,
                      -1, -1,
                      Integer.MIN_VALUE, Integer.MIN_VALUE + 1);
        assertTrue(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test13() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(10, 20);

        selectionModel.insertIndexInterval(10, 10, true);
        assertIndexes(selectionModel,
                      10, 30,
                      20, 30);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 9)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(10, 30)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(31, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test14() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(10, 20);

        selectionModel.insertIndexInterval(10, 10, false);
        assertIndexes(selectionModel,
                      10, 30,
                      10, 30);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 9)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(10, 30)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(31, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test15() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(10, 20);

        selectionModel.insertIndexInterval(9, 10, true);
        assertIndexes(selectionModel,
                      20, 30,
                      20, 30);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 19)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(20, 30)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(31, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test16() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(10, 20);

        selectionModel.insertIndexInterval(9, 10, false);
        assertIndexes(selectionModel,
                      20, 30,
                      20, 30);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 19)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(20, 30)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(31, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test17() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(0, 10);
        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE - 11)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 10, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));

        selectionModel.insertIndexInterval(11, 10, true);
        assertIndexes(selectionModel,
                      0, 10,
                      0, 10);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test18() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(0, 10);
        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE - 11)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 10, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));

        selectionModel.insertIndexInterval(11, 10, false);
        assertIndexes(selectionModel,
                      0, 10,
                      0, 10);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));
    }

    private static void test19() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(0, 10);
        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 20, Integer.MAX_VALUE - 10);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE - 10,
                      Integer.MAX_VALUE - 20, Integer.MAX_VALUE - 10);
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE - 21)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 20, Integer.MAX_VALUE - 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 9, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));

        selectionModel.insertIndexInterval(11, 10, true);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE - 11)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 10, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));
    }

    private static void test20() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionInterval(0, 10);
        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 20, Integer.MAX_VALUE - 10);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE - 10,
                      Integer.MAX_VALUE - 20, Integer.MAX_VALUE - 10);
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE - 21)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 20, Integer.MAX_VALUE - 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 9, Integer.MAX_VALUE)
                            .noneMatch(selectionModel::isSelectedIndex));

        selectionModel.insertIndexInterval(11, 10, false);
        assertIndexes(selectionModel,
                      0, Integer.MAX_VALUE,
                      Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
        assertFalse(selectionModel.isSelectionEmpty());
        assertTrue(IntStream.rangeClosed(0, 10)
                            .allMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(11, Integer.MAX_VALUE - 11)
                            .noneMatch(selectionModel::isSelectedIndex));
        assertTrue(IntStream.rangeClosed(Integer.MAX_VALUE - 10, Integer.MAX_VALUE)
                            .allMatch(selectionModel::isSelectedIndex));
    }

    private static Exception runTest(Runnable test) {
        try {
            test.run();
        } catch (Exception e) {
            return e;
        }
        return null;
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

