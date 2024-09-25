public class BottomOneColumn {
    public static void main(String[] args) throws Exception {
        PassFailJFrame.builder()
                      .instructions(INSTRUCTIONS)
                      .rows(7)
                      .columns(30)
                      .testUI(() -> WindowCreator.createTestWindows(3))
                      .positionTestUI(PassFailJFrame.WindowLayouts::bottomOneColumn)
                      .position(PassFailJFrame.Position.VERTICAL)
                      .build()
                      .awaitAndCheck();
    }

    private static final String INSTRUCTIONS = """
            A simple demo with 3 test windows positioned to
            the bottom of the instruction frame in one row.
            The row of the test windows is centered on the screen.
            
            Layout: WindowLayouts::bottomOneRowCentered
            """;
}
