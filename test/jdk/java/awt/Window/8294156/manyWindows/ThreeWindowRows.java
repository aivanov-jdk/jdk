import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/*
 * @test
 * @bug 8294156
 * @library /java/awt/regtesthelpers
 * @build PassFailJFrame
 * @summary Demonstrates adding and positioning several test windows
 *          vertically in multiple rows
 * @run main/manual ThreeWindowRows
 */
public class ThreeWindowRows {
    public static void main(String[] args) throws Exception {
        PassFailJFrame.builder()
                      .instructions(TwoWindowColumnsH.INSTRUCTIONS)
                      .rows(5)
                      .columns(30)
                      .testUI(ThreeWindowRows::createTestUI)
//                      .positionTestUI(TwoWindowColumnsH::positionTestUI)
                      .positionTestUI(ThreeWindowRows::positionTestUI)
                      .position(PassFailJFrame.Position.VERTICAL)
                      .build()
                      .awaitAndCheck();
    }

    private static List<? extends Window> createTestUI() {
        return TwoWindowColumnsH.createTestWindows(8);
    }

    private static final int COLUMNS = 3;

    public static void positionTestUI(List<Window> windows,
                                      PassFailJFrame.InstructionUI instructionUI) {
        final Point center = WindowLayouts.getScreenCenter();

        final List<List<Window>> windowRows =
                new ArrayList<>(windows.size() / COLUMNS + 1);
        int rowStart = 0;
        do {
            windowRows.add(windows.subList(rowStart,
                                           Math.min(rowStart + COLUMNS,
                                                    windows.size())));
            rowStart += COLUMNS;
        } while (rowStart < windows.size());

        List<Integer> rowHeights =
                windowRows.stream()
                          .mapToInt(wr -> wr.stream()
                                            .mapToInt(Window::getHeight)
                                            .max()
                                            .orElseThrow())
                          .boxed()
                          .toList();

        int y = center.y - (rowHeights.stream()
                                      .mapToInt(Integer::intValue)
                                      .sum()
                            - instructionUI.getSize().height) / 2;
        instructionUI.setLocation(instructionUI.getLocation().x,
                                  y - PassFailJFrame.WINDOW_GAP
                                  - instructionUI.getSize().height);

        for (int i = 0; i < windowRows.size(); i++) {
            List<Window> row = windowRows.get(i);
            int rowWidth = WindowLayouts.getWindowListWidth(row);

            int x = center.x - rowWidth / 2;
            for (Window w : row) {
                w.setLocation(x, y);
                x += w.getWidth() + PassFailJFrame.WINDOW_GAP;
            }

            y += rowHeights.get(i) + PassFailJFrame.WINDOW_GAP;
        }

    }
}
