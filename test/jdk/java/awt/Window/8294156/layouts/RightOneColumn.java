public class RightOneColumn {
    public static void main(String[] args) throws Exception {
        PassFailJFrame.builder()
                      .instructions(INSTRUCTIONS)
                      .rows(15)
                      .columns(30)
                      .testUI(() -> WindowCreator.createTestWindows(3))
                      .positionTestUI(PassFailJFrame.WindowLayouts::rightOneColumn)
                      .build()
                      .awaitAndCheck();
    }

    private static final String INSTRUCTIONS = """
            A simple demo with 3 test windows positioned to
            the right of the instruction frame in one column.
            The top of the first test window is aligned with
            the top of the instructions.
            
            Layout: WindowLayouts::rightOneColumn
            """;
}
