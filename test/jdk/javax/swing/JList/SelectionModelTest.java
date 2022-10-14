import java.util.stream.IntStream;

import javax.swing.DefaultListSelectionModel;

public class SelectionModelTest {
    public static void main(String[] args) {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
//*
        System.out.println("\n2nd");
        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        System.out.println(selectionModel.isSelectionEmpty());
        System.out.println(selectionModel.isSelectedIndex(0));
        System.out.println(selectionModel.isSelectedIndex(1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        System.out.println("\n3rd");
        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        selectionModel.removeIndexInterval(1, Integer.MAX_VALUE);
        printSelection(selectionModel);
        System.out.println(selectionModel.isSelectionEmpty());
        System.out.println(selectionModel.isSelectedIndex(0));
        System.out.println(selectionModel.isSelectedIndex(1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        System.out.println("\n4th");
        selectionModel.setSelectionInterval(0, Integer.MAX_VALUE);
        printSelection(selectionModel);
        selectionModel.removeIndexInterval(2, Integer.MAX_VALUE);
        printSelection(selectionModel);
//*/
        System.out.println("\n5th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        printSelection(selectionModel);
        selectionModel.removeIndexInterval(0, Integer.MAX_VALUE - 1);
        printSelection(selectionModel);


        System.out.println("\n6th");
        selectionModel.setSelectionInterval(10, 20);
        printSelection(selectionModel);
        IntStream.rangeClosed(0, 30)
                 .map((i) -> selectionModel.isSelectedIndex(i) ? 1 : 0)
                 .forEach(System.out::print);
        System.out.println();

        selectionModel.removeIndexInterval(0, 10);
        printSelection(selectionModel);
        System.out.println(selectionModel.isSelectionEmpty());

        IntStream.rangeClosed(0, 30)
                 .map((i) -> selectionModel.isSelectedIndex(i) ? 1 : 0)
                 .forEach(System.out::print);
        System.out.println();
//*/
        System.out.println("\n7th");
        selectionModel.setSelectionInterval(0, 0);
        printSelection(selectionModel);
        System.out.println(selectionModel.isSelectionEmpty());
        System.out.println(selectionModel.isSelectedIndex(0));
        System.out.println(selectionModel.isSelectedIndex(1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        selectionModel.addSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        printSelection(selectionModel);
        System.out.println(selectionModel.isSelectionEmpty());
        System.out.println(selectionModel.isSelectedIndex(0));
        System.out.println(selectionModel.isSelectedIndex(1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        selectionModel.removeIndexInterval(0, 0);
        printSelection(selectionModel);
        System.out.println(selectionModel.isSelectionEmpty());
        System.out.println(selectionModel.isSelectedIndex(0));
        System.out.println(selectionModel.isSelectedIndex(1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 2));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE - 1));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        System.out.println("\n8th");
        selectionModel.setSelectionInterval(0, 0);
        printSelection(selectionModel);
        selectionModel.insertIndexInterval(0, Integer.MAX_VALUE - 1, true);
        printSelection(selectionModel);
        System.out.println(IntStream.rangeClosed(0, Integer.MAX_VALUE - 1)
                                    .allMatch(selectionModel::isSelectedIndex));
        System.out.println(selectionModel.isSelectedIndex(Integer.MAX_VALUE));

        System.out.println("\n9th");
        selectionModel.setSelectionInterval(0, 0);
        printSelection(selectionModel);
        selectionModel.insertIndexInterval(0, Integer.MAX_VALUE, true);
        printSelection(selectionModel);
        System.out.println(IntStream.rangeClosed(0, Integer.MAX_VALUE)
                                    .allMatch(selectionModel::isSelectedIndex));

        System.out.println("\n10th");
        selectionModel.setSelectionInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        printSelection(selectionModel);
        selectionModel.insertIndexInterval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, true);
        printSelection(selectionModel);
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
}
