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
public class RightOneRowMoved {
    public static void main(String[] args) throws Exception {
        PassFailJFrame.builder()
                      .instructions(INSTRUCTIONS)
                      .rows(15)
                      .columns(30)
                      .testUI(() -> WindowCreator.createTestWindows(3))
                      .positionTestUI(RightOneRowMoved::positionWindows)
                      .build()
                      .awaitAndCheck();
    }

    private static void positionWindows(List<Window> windows,
                                        PassFailJFrame.InstructionUI instructionUI) {
        int width = instructionUI.getSize().width
                    + WindowLayouts.getWindowListWidth(windows)
                    + PassFailJFrame.WINDOW_GAP;
        instructionUI.setLocation(WindowLayouts.getScreenCenter().x
                                  - width / 2,
                                  instructionUI.getLocation().y);
        WindowLayouts.rightOneRow(windows,
                                  instructionUI);
    }

    private static final String INSTRUCTIONS = """
            A simple demo with 3 test windows positioned to
            the right of the instruction frame in one row.
            The top of the test windows is aligned with
            the top of the instructions.
            
            The entire row is centered on the screen.
            
            Layout: WindowLayouts::rightOneColumn
                    plus custom action of moving the instruction
                    frame to the left.
            """;
}
