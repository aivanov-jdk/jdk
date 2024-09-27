import java.awt.Window;
import java.util.List;

/*
 * @test
 * @bug 8294156 8317116
 * @library /java/awt/regtesthelpers
 * @build PassFailJFrame
 * @summary Position test windows in a row to the right of the instructions
 * @run main/manual RightComplex2and1
 */
public class RightComplex2and1 {
    public static void main(String[] args) throws Exception {
        PassFailJFrame.builder()
                      .instructions(INSTRUCTIONS)
                      .rows(15)
                      .columns(30)
                      .testUI(() -> WindowCreator.createTestWindows(3))
                      .positionTestUI(RightComplex2and1::positionWindows)
                      .build()
                      .awaitAndCheck();
    }

    private static void positionWindows(List<Window> windows,
                                        PassFailJFrame.InstructionUI instructionUI) {
        WindowLayouts.rightOneRow(windows.subList(0, 2),
                                  instructionUI);
        WindowLayouts.rightOneColumn(List.of(windows.get(0),
                                             windows.get(2)),
                                     instructionUI);
    }

    private static final String INSTRUCTIONS = """
            A simple demo with 3 test windows positioned to
            the right of the instruction frame.
            The first two windows are displayed in a row
            where each window is aligned to the top the instructions.
            The third window is displayed below the first window.
            
            This test demonstrates combining default layouts to
            create more complex layouts:
                WindowLayouts::rightOneRow
                     for the first two windows
                WindowLayouts::rightOneColumn
                     for the first and third windows
            """;
}
